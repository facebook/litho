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

package com.facebook.rendercore.incrementalmount

/**
 * Remembers one output the cost-aware search keeps passing over because it does not fit the frame.
 *
 * Without this, an output that is always too expensive would be passed over forever: every frame
 * the search would step over it in favour of something cheaper. Deferring it lets the search count
 * how many frames it has waited and eventually mount it anyway, accepting one overrun rather than
 * never premounting the content at all.
 *
 * The deferred output is identified by id *and* host id: trees are rebuilt between frames, so a
 * matching id whose host changed is a different output and the tracker starts over.
 *
 * Every transition is a named method here rather than a flag the caller sets, so the rules live
 * next to the state they govern instead of being spread across the extension.
 */
internal class StarvationTracker {

  /** The deferred output, or [IncrementalMountOutput.NO_HOST_ID] when nothing is deferred. */
  var deferredId: Long = IncrementalMountOutput.NO_HOST_ID
    private set

  /** Its host when it was deferred, used to detect that the tree moved underneath it. */
  var deferredHostId: Long = IncrementalMountOutput.NO_HOST_ID
    private set

  private var awaitingFullPass: Boolean = false
  private var passedOverCount: Int = 0
  private var lastPassedOverFrameSequence: Long = Long.MIN_VALUE
  private var lastRetriedFrameSequence: Long = Long.MIN_VALUE

  val isDeferring: Boolean
    get() = deferredId != IncrementalMountOutput.NO_HOST_ID

  /**
   * Whether the search may pick something to defer right now.
   *
   * False while one is already deferred, and false after [requireFullPass] until the sweep has come
   * full circle, so a replacement is chosen from a complete pass rather than from wherever the last
   * one happened to stop.
   */
  val canDefer: Boolean
    get() = !isDeferring && !awaitingFullPass

  fun isDeferred(id: Long): Boolean = isDeferring && id == deferredId

  fun defer(id: Long, hostId: Long) {
    deferredId = id
    deferredHostId = hostId
  }

  /** Whether the deferred output has already been reconsidered in this frame. */
  fun wasRetriedIn(frameSequence: Long): Boolean = lastRetriedFrameSequence == frameSequence

  fun markRetriedIn(frameSequence: Long) {
    lastRetriedFrameSequence = frameSequence
  }

  /**
   * Counts at most one pass-over per frame and reports whether the deferred output has now waited
   * long enough to be mounted regardless of cost.
   */
  fun recordPassedOver(frameSequence: Long): Boolean {
    if (lastPassedOverFrameSequence != frameSequence) {
      lastPassedOverFrameSequence = frameSequence
      passedOverCount++
    }
    return passedOverCount >= STARVATION_FRAME_LIMIT
  }

  /** Stops deferring, leaving the search free to pick something else immediately. */
  fun stopDeferring() {
    deferredId = IncrementalMountOutput.NO_HOST_ID
    deferredHostId = IncrementalMountOutput.NO_HOST_ID
    passedOverCount = 0
    lastPassedOverFrameSequence = Long.MIN_VALUE
    lastRetriedFrameSequence = Long.MIN_VALUE
  }

  /**
   * Stops deferring and holds off picking a replacement until the sweep wraps.
   *
   * Used when the output stopped being a candidate part-way through a pass. Picking the next one
   * from where the sweep happens to be would bias it toward whatever sits just after the abandoned
   * output, so the choice waits for a complete pass.
   */
  fun stopDeferringUntilFullPass() {
    stopDeferring()
    requireFullPass()
  }

  fun requireFullPass() {
    awaitingFullPass = true
  }

  fun onFullPassComplete() {
    awaitingFullPass = false
  }

  fun reset() {
    stopDeferring()
    awaitingFullPass = false
  }

  private companion object {
    /** Frames an output may be passed over before it is mounted regardless of cost. */
    private const val STARVATION_FRAME_LIMIT = 8
  }
}
