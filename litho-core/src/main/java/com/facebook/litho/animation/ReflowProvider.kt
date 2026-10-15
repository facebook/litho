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
import androidx.collection.ScatterMap
import com.facebook.litho.GlobalKey
import com.facebook.rendercore.IntSize

/**
 * The per-tree reflow surface exposed by `LithoTree`, mirroring how `StateProvider` /`StateUpdater`
 * hang off the tree without it knowing `TreeState`'s internals. The background layout thread reads
 * [reflowView]; the mount extension drives the rest. `LithoTree` holds one immutable `val` of this
 * type, so it depends only on this abstraction, not on the concrete store or any animation type.
 * The sole implementation is [ReflowAnimationStore].
 */
internal interface ReflowProvider {

  /** The current published view, read on the background layout thread. EMPTY until a node mints. */
  val reflowView: ReflowReadView

  /**
   * True while no node is tracked — lets the extension skip work allocation-free (and, for an empty
   * store, without minting its heavy state).
   */
  @get:MainThread val isEmpty: Boolean

  /** Number of tracked reflow nodes (0 while empty). */
  @get:MainThread val size: Int

  /** True if a value has been minted for [key]. */
  @MainThread operator fun contains(key: GlobalKey): Boolean

  /** The live destination [key] is settled at or heading toward, or `null`. */
  @MainThread fun targetSizeOf(key: GlobalKey): IntSize?

  /** Batched structural update; mints the heavy state on first use. See [ReflowAnimationStore]. */
  @MainThread fun rebuildKeySet(liveSeeds: ScatterMap<GlobalKey, IntSize>)

  /** Retargets [key]; a no-op if no node is minted. See [ReflowAnimationStore.retarget]. */
  @MainThread
  fun retarget(
      key: GlobalKey,
      target: IntSize,
      committedSize: IntSize,
      spec: AnimationSpec<IntSize>,
  )

  /** Stops and clears everything, releasing the backstop so a later rebuild re-arms. Idempotent. */
  @MainThread fun release()
}
