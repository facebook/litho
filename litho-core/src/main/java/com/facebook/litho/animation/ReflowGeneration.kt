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

import androidx.collection.ScatterMap
import com.facebook.litho.GlobalKey
import com.facebook.rendercore.IntSize

/**
 * The read-only reflow view the layout thread consumes via
 * `com.facebook.litho.LithoTree.reflowView`. Impl-agnostic on purpose: readers depend only on these
 * accessors, not on whether the backing is a live view or an immutable generation.
 */
internal interface ReflowReadView {
  /** True if the tree has no reflow values at all, so no key can resolve. */
  val isEmpty: Boolean

  /** The destination [key] is settled at or heading toward, or `null` if no value exists. */
  fun targetSize(key: GlobalKey): IntSize?

  /** The current on-screen size for [key], settled or mid-interpolation. */
  fun currentSize(key: GlobalKey): IntSize?

  /** The in-flight size to pin [key] to during a real, non-animation-frame relayout. */
  fun sizeToPin(key: GlobalKey): IntSize?

  /** The interpolated size for [key] on an active animation frame. */
  fun currentSizeIfRunning(key: GlobalKey): IntSize?
}

/**
 * One immutable generation of a tree's reflow values — [ReflowAnimationStore]'s single canonical
 * structure and, published into `LithoTree.reflowView`, the layout thread's read view. The key-set
 * spine is frozen; each [Entry] is shared with the store and carries `@Volatile` leaves (current
 * value, running state, target). A retarget or a per-frame tick mutates a leaf in place and is
 * visible through an already-published generation with no rebuild; only a key-set change (a node
 * minted or evicted) swaps in a new generation. A background walk therefore always reads a
 * structurally-stable generation.
 *
 * [entries] is nullable so the shared [EMPTY] generation carries no map at all — reads
 * short-circuit to "no value" instead of dispatching into an allocated empty [ScatterMap].
 */
internal class ReflowGeneration(private val entries: ScatterMap<GlobalKey, Entry>?) :
    ReflowReadView {

  /**
   * One per reflow node, running or idle. Shared by identity with the store: [value] and
   * [targetSize] are mutated in place on the main thread and read cross-thread, so both are
   * `@Volatile` (the [AnimatedValue] fields already are).
   */
  internal class Entry(val value: AnimatedValue<IntSize>, targetSize: IntSize) {
    // Seeded at mint, updated by retarget.
    @Volatile var targetSize: IntSize = targetSize

    val currentSize: IntSize
      get() = value.currentValueRaw

    val hasAnimator: Boolean
      get() = value.hasAnimator

    val isRunning: Boolean
      get() = value.isRunning
  }

  override val isEmpty: Boolean
    get() = entries?.isEmpty() ?: true

  override fun targetSize(key: GlobalKey): IntSize? = entries?.get(key)?.targetSize

  override fun currentSize(key: GlobalKey): IntSize? = entries?.get(key)?.currentSize

  override fun sizeToPin(key: GlobalKey): IntSize? =
      entries?.get(key)?.let { if (it.hasAnimator) it.currentSize else null }

  override fun currentSizeIfRunning(key: GlobalKey): IntSize? =
      entries?.get(key)?.let { if (it.isRunning) it.currentSize else null }

  // --- Store (main-thread) API for building the next generation ---

  internal fun entry(key: GlobalKey): Entry? = entries?.get(key)

  internal val size: Int
    get() = entries?.size ?: 0

  internal inline fun forEachEntry(block: (GlobalKey, Entry) -> Unit) {
    entries?.forEach(block)
  }

  companion object {
    internal val EMPTY: ReflowGeneration = ReflowGeneration(null)
  }
}
