/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.animation

import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.collection.MutableScatterMap
import androidx.collection.ScatterMap
import com.facebook.litho.GlobalKey
import com.facebook.litho.HookGlobalKey
import com.facebook.litho.LithoTree
import com.facebook.litho.LithoTreeLifecycleProvider
import com.facebook.litho.RenderScope
import com.facebook.litho.state.StateId
import com.facebook.rendercore.IntSize

/**
 * Tree-scoped owner of per-node animated reflow sizes for `Style.transition { bounds(reflow = true)
 * }`. A reflow node has no hook call site, so it cannot persist an [AnimatedValue] via
 * `getUpdatedState`; this store owns one per node instead, keyed by `transitionGlobalKey`.
 *
 * It is the sole [ReflowProvider]: `LithoTree` holds one eagerly, but it is cheap while empty and
 * mints its heavy state (the map, animators, and release backstop) lazily on the first
 * [rebuildKeySet]. Its state is a single immutable [ReflowGeneration] ([current]), read on the
 * background layout thread via [reflowView]; a key-set change swaps in a new generation, while a
 * retarget or per-frame tick mutates an entry's `@Volatile` leaves in place. All mutation is
 * main-thread confined.
 */
internal class ReflowAnimationStore(private val lithoTree: LithoTree) : ReflowProvider {

  // Read cross-thread through [reflowView], so @Volatile; the generation's leaves are already
  // @Volatile. Mutated only on the main thread.
  @Volatile private var current: ReflowGeneration = ReflowGeneration.EMPTY

  override val reflowView: ReflowReadView
    get() = current

  @get:MainThread
  override val isEmpty: Boolean
    get() = current.size == 0

  // Armed lazily on the first mint so an empty store never touches the tree's listener list. It
  // only clears — it must NOT remove itself, because tree release nulls the listener list
  // mid-iteration. [release] removes it on normal teardown so a later rebuild re-arms.
  private var onTreeRelease: LithoTreeLifecycleProvider.OnReleaseListener? = null

  @MainThread override operator fun contains(key: GlobalKey): Boolean = current.entry(key) != null

  @get:MainThread
  override val size: Int
    get() = current.size

  @MainThread override fun targetSizeOf(key: GlobalKey): IntSize? = current.entry(key)?.targetSize

  /**
   * Rebuilds the generation for the current key set: keep surviving entries by identity, mint
   * missing ones seeded at their committed size, stop departed ones. Targets are reconciled
   * separately via [retarget]. Call only on an actual key-set change — a same-set call needlessly
   * rebuilds (the extension preflights).
   */
  @MainThread
  override fun rebuildKeySet(liveSeeds: ScatterMap<GlobalKey, IntSize>) {
    val next = MutableScatterMap<GlobalKey, ReflowGeneration.Entry>(liveSeeds.size)
    liveSeeds.forEach { key, seed ->
      next[key] = current.entry(key) ?: ReflowGeneration.Entry(mint(key, seed), seed)
    }
    current.forEachEntry { key, entry ->
      if (key !in liveSeeds) {
        entry.value.stop()
        AnimationController.unregister(entry.value.frameAnimation)
      }
    }
    if (next.isNotEmpty()) ensureBackstop()
    current = if (next.isEmpty()) ReflowGeneration.EMPTY else ReflowGeneration(next)
  }

  /**
   * Retargets [key] toward [target], snapping an idle value to its [committedSize] start first, and
   * ensures it is receiving frames. A leaf write on the shared entry — no rebuild, no republish.
   * No-op if [key] is not minted.
   */
  @MainThread
  override fun retarget(
      key: GlobalKey,
      target: IntSize,
      committedSize: IntSize,
      spec: AnimationSpec<IntSize>,
  ) {
    val entry = current.entry(key) ?: return
    // snapTo re-seeds an idle value silently (notify = false avoids a stray relayout racing the
    // first
    // frame); a running value keeps its in-flight value, preserving velocity across an
    // interruption.
    if (!entry.value.isRunning) entry.value.snapTo(committedSize, notify = false)
    entry.value.retarget(target, spec)
    if (entry.value.hasAnimator && !entry.value.isActive) {
      AnimationController.register(entry.value.frameAnimation)
    }
    // Written last: targetSize is the entry's cross-thread publication marker, so a reader
    // observing
    // the new target is guaranteed to also observe the animator update above.
    entry.targetSize = target
  }

  /**
   * Normal teardown (extension onUnmount / tree swap): remove the release backstop so it can't
   * accumulate across mount cycles, then stop everything and clear the published view. Idempotent —
   * and reusable: a later [rebuildKeySet] re-arms the backstop, since this store lives on the tree.
   */
  @MainThread
  override fun release() {
    onTreeRelease?.let { listener ->
      lithoTree.treeLifecycle.removeOnReleaseListener(listener)
      onTreeRelease = null
    }
    stopAndClear()
  }

  /**
   * The current animated value for [key], or `null`. Test-only inspection; production reads go
   * through [ReflowProvider.reflowView].
   */
  @VisibleForTesting
  @MainThread
  fun peek(key: GlobalKey): AnimatedValue<IntSize>? = current.entry(key)?.value

  private fun ensureBackstop() {
    if (onTreeRelease == null) {
      val listener = LithoTreeLifecycleProvider.OnReleaseListener { stopAndClear() }
      onTreeRelease = listener
      lithoTree.treeLifecycle.addOnReleaseListener(listener)
    }
  }

  private fun stopAndClear() {
    current.forEachEntry { _, entry ->
      entry.value.stop()
      AnimationController.unregister(entry.value.frameAnimation)
    }
    current = ReflowGeneration.EMPTY
  }

  private fun mint(key: GlobalKey, seed: IntSize): AnimatedValue<IntSize> =
      // isLayoutContext=false keeps the per-frame no-op in the resolve handler, where it drains on
      // the resolve-skip branch; RenderScope.Resolve is the matching identity (similar to
      // useAnimatedValue). Neither flag drives relayout — the layout-phase read does.
      AnimatedValue(
          initialValue = seed,
          typeConverter = IntSizeTypeConverter,
          stateId = reflowStateId(lithoTree.id, key),
          isLayoutContext = false,
          stateUpdater = lithoTree.stateUpdater,
          label = REFLOW_LABEL,
      )
}

/**
 * The read-tracking [StateId] for a reflow node's animated size. A pure function of `(treeId, key)`
 * so the layout can record the read (subscribe) without the [AnimatedValue] existing yet — the
 * value is minted with this same id on the main-thread commit.
 */
internal fun reflowStateId(treeId: Int, key: GlobalKey): StateId =
    // RenderScope is only a StateId identity tag which matches the isLayoutContext that routes the
    // per-frame no-op to a state handler. Neither flag drives the relayout
    StateId(treeId, HookGlobalKey(key, REFLOW_HOOK_KEY, RenderScope.Resolve))

/** Whether this is a reflow node's animated-size id, as minted by [reflowStateId]. */
internal fun StateId.isReflowStateId(): Boolean = hookGlobalKey.hookKey == REFLOW_HOOK_KEY

private const val REFLOW_HOOK_KEY = "style-transition-reflow-size"
private const val REFLOW_LABEL = "reflow"
