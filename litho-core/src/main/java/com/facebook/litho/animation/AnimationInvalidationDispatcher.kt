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
import androidx.collection.MutableScatterMap
import com.facebook.litho.StateUpdater

/**
 * The seam between animated values producing frame invalidations and the [StateUpdater]s that apply
 * them. [AnimatedValue] records here without knowing about [AnimationController], and
 * [AnimationController] scopes a frame via [batch] without knowing about [StateUpdater].
 *
 * Within a [batch], invalidations are collected and flushed once per [StateUpdater] so each tree
 * sees a single coalesced [StateUpdater.applyAnimationFrameUpdates] call regardless of how many
 * values changed. Records outside a batch (e.g. `snapTo`) are applied immediately. Main-thread
 * confined.
 */
@MainThread
internal object AnimationInvalidationDispatcher {

  private val pending = MutableScatterMap<StateUpdater, MutableList<AnimationFrameInvalidation>>()
  private var isBatching = false

  /**
   * Runs [block] as one batch: invalidations recorded during it are collected and then flushed once
   * per [StateUpdater] when [block] returns.
   */
  inline fun batch(block: () -> Unit) {
    check(!isBatching) {
      "AnimationInvalidationDispatcher.batch is not re-entrant"
    }
    isBatching = true
    try {
      block()
    } finally {
      isBatching = false
      flush()
    }
  }

  /**
   * Records a frame invalidation for [stateUpdater]. Inside a [batch] it is collected for the
   * end-of-batch flush; outside a batch it is applied immediately.
   */
  fun record(stateUpdater: StateUpdater, invalidation: AnimationFrameInvalidation) {
    if (isBatching) {
      pending.getOrPut(stateUpdater) { mutableListOf() }.add(invalidation)
    } else {
      stateUpdater.applyAnimationFrameUpdates(listOf(invalidation))
    }
  }

  private fun flush() {
    if (pending.isEmpty()) return
    // `isBatching` is already false here, so any re-entrant record() during apply is applied
    // immediately and won't mutate `pending` — safe to iterate then clear.
    pending.forEach { stateUpdater, invalidations ->
      stateUpdater.applyAnimationFrameUpdates(invalidations)
    }
    pending.clear()
  }

  /**
   * Resets all transient state. Test-only — invoked from [AnimationController]'s test frame-driver
   * reset to isolate this global singleton between tests. Must not be called during a [batch].
   */
  fun clear() {
    check(!isBatching) {
      "AnimationInvalidationDispatcher.clear() must not be called during a batch"
    }
    pending.clear()
  }
}
