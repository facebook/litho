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

package com.facebook.litho

import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import java.util.WeakHashMap

/**
 * Thread-safe singleton registry that holds weak references to all [ComponentTree] instances that
 * are still in memory, including released ones that have not been garbage collected yet.
 *
 * Gated by [enabled] so it is zero-cost when disabled. The flag is set by
 * [com.facebook.litho.hotreload.LithoHotReloadHandler.install] and by Litho Observability in debug
 * and perf builds.
 */
object ComponentTreeRegistry {

  @JvmField @Volatile var enabled: Boolean = false

  private val lock = Any()

  // Keyed by identity rather than by ComponentTree.id: ids can be reused through
  // ComponentTree.Builder.overrideComponentTreeId.
  private val trees = WeakHashMap<ComponentTree, Entry>()

  /** Adds [tree] to the registry if [enabled] is true. */
  fun register(tree: ComponentTree) {
    if (!enabled) return
    synchronized(lock) { trees[tree] = Entry(createdAtMs = SystemClock.uptimeMillis()) }
  }

  /**
   * Marks [tree] as released. The tree stays in the registry until it is garbage collected, so a
   * released tree that is still reachable can be reported as a leak.
   */
  fun markReleased(tree: ComponentTree) {
    if (!enabled) return
    synchronized(lock) {
      val entry = trees[tree] ?: return
      if (entry.releasedAtMs == null) {
        entry.releasedAtMs = SystemClock.uptimeMillis()
      }
    }
  }

  /** Iterates over a snapshot of all unreleased [ComponentTree] instances. */
  fun forEach(action: (ComponentTree) -> Unit) {
    val snapshot: List<ComponentTree>
    synchronized(lock) {
      snapshot = trees.entries.filter { it.value.releasedAtMs == null }.map { it.key }
    }
    snapshot.forEach(action)
  }

  /** Counts registered trees by state. Takes no [ComponentTree] lock, so it is cheap to sample. */
  fun countByState(): StateCounts {
    var attached = 0
    var detached = 0
    var released = 0
    synchronized(lock) {
      for ((tree, entry) in trees) {
        when {
          entry.releasedAtMs != null -> released++
          tree.isAttached() -> attached++
          else -> detached++
        }
      }
    }
    return StateCounts(attached = attached, detached = detached, released = released)
  }

  /**
   * Returns the state of every registered tree. Reads root names under each tree's lock, so use
   * [countByState] for frequent sampling.
   */
  fun snapshot(): List<TreeInfo> {
    val entries: List<Triple<ComponentTree, Long, Long?>>
    synchronized(lock) {
      entries = trees.entries.map { Triple(it.key, it.value.createdAtMs, it.value.releasedAtMs) }
    }
    return entries.map { (tree, createdAtMs, releasedAtMs) ->
      TreeInfo(
          id = tree.id,
          rootName =
              if (releasedAtMs != null) tree.getReleasedComponent()
              else tree.getSimpleName().orEmpty(),
          isAttached = tree.isAttached(),
          createdAtMs = createdAtMs,
          releasedAtMs = releasedAtMs,
      )
    }
  }

  /**
   * Forces a re-resolve on every registered [ComponentTree] by setting a new root with a fresh
   * component ID via [Component.makeShallowCopyWithNewId]. This causes every component's render()
   * to be called again (picking up hot-swapped bytecode) while preserving hook state.
   *
   * Returns the number of trees invalidated.
   */
  fun forceResolveAll(): Int {
    var count = 0
    forEach { tree ->
      val root = tree.root
      if (root != null) {
        tree.setRootAsync(root.makeShallowCopyWithNewId())
        count++
      }
    }
    return count
  }

  @VisibleForTesting
  internal fun clear() {
    synchronized(lock) { trees.clear() }
  }

  class StateCounts(val attached: Int, val detached: Int, val released: Int)

  /**
   * State of one registered tree.
   *
   * @property rootName Simple name of the root component; for released trees, the root at release.
   *   Empty if unknown.
   * @property releasedAtMs [SystemClock.uptimeMillis] at release, or null if not released.
   */
  class TreeInfo(
      val id: Int,
      val rootName: String,
      val isAttached: Boolean,
      val createdAtMs: Long,
      val releasedAtMs: Long?,
  ) {
    val isReleased: Boolean
      get() = releasedAtMs != null
  }

  // Must not reference the tree, or the WeakHashMap would keep it alive.
  private class Entry(val createdAtMs: Long) {
    var releasedAtMs: Long? = null
  }
}
