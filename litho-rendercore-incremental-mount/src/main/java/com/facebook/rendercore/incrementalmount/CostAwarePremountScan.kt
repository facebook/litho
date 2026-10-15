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

import androidx.collection.MutableLongSet

/**
 * How far the cost-aware search has swept, and what it has already looked at this frame.
 *
 * The sweep runs outward from the incremental-mount cursors without disturbing them: it walks
 * forward through the outputs ordered by top bound and backward through those ordered by bottom
 * bound, inspecting at most [MAX_CANDIDATES_INSPECTED_PER_DIRECTION] in each direction per frame so
 * the search itself cannot consume the frame it is trying to fill.
 *
 * The indices, the per-frame inspection budget and the seen-set only mean anything together, so
 * they live behind one object that owns the transitions rather than as loose fields on the
 * extension state.
 */
internal class CostAwarePremountScan {

  /** Next output to inspect in the tops-ordered list. */
  var topIndex: Int = 0
    private set

  /** Next output to inspect in the bottoms-ordered list. */
  var bottomIndex: Int = -1
    private set

  /** Id of the mount currently in flight, so its duration can be attributed when it completes. */
  var mountInProgressId: Long = IncrementalMountOutput.NO_HOST_ID

  /** Content type of the in-flight mount, read once the visible-bounds bracket closes. */
  var pendingContentType: Any? = null

  /** When the in-flight mount started, so the search that selected it is not charged to it. */
  var pendingMountStartNs: Long = 0L

  private var frameSequence: Long = Long.MIN_VALUE
  /** Outputs evaluated this frame, per direction. */
  var inspectedTops: Int = 0
    private set

  var inspectedBottoms: Int = 0
    private set

  /**
   * Outputs stepped over this frame, evaluated or not.
   *
   * Instrumentation only. Walked minus inspected is how much of a frame went on outputs that needed
   * no work, which is the ratio that separates a sweep spinning from one making progress.
   */
  var walkedTops: Int = 0
    private set

  var walkedBottoms: Int = 0
    private set

  private var isInitialized: Boolean = false
  private val inspectedIds: MutableLongSet = MutableLongSet()

  val needsPositionReset: Boolean
    get() = !isInitialized

  /** Opens a new frame's inspection budget. A no-op when [frameSequence] is already current. */
  fun beginFrame(frameSequence: Long) {
    if (this.frameSequence == frameSequence) {
      return
    }
    this.frameSequence = frameSequence
    inspectedTops = 0
    inspectedBottoms = 0
    walkedTops = 0
    walkedBottoms = 0
    inspectedIds.clear()
  }

  /** Restarts the sweep from the official incremental-mount cursors. */
  fun resetPositions(previousTopsIndex: Int, previousBottomsIndex: Int) {
    topIndex = previousTopsIndex
    bottomIndex = previousBottomsIndex - 1
    isInitialized = true
  }

  /** Whether the sweep has anywhere left to go in this direction. */
  fun canAdvance(outputCount: Int, scanTop: Boolean): Boolean =
      if (scanTop) topIndex in 0 until outputCount else bottomIndex in 0 until outputCount

  /**
   * Whether another candidate may be *evaluated* in this direction within this frame's budget.
   *
   * Only evaluation is budgeted. Stepping over an output we already own is a set lookup, not search
   * work, and billing those was what let a growing run of premounted outputs in front of the cursor
   * consume a whole frame's allowance without mounting anything.
   */
  fun canInspect(outputCount: Int, scanTop: Boolean): Boolean =
      if (scanTop) {
        inspectedTops < MAX_CANDIDATES_INSPECTED_PER_DIRECTION
      } else {
        inspectedBottoms < MAX_CANDIDATES_INSPECTED_PER_DIRECTION
      }

  /** The index the sweep is pointing at, without advancing or spending anything. */
  fun currentIndex(scanTop: Boolean): Int = if (scanTop) topIndex else bottomIndex

  /** Moves past the current output without charging it to the inspection budget. */
  fun advance(scanTop: Boolean) {
    if (scanTop) {
      walkedTops++
      topIndex++
    } else {
      walkedBottoms++
      bottomIndex--
    }
  }

  /** Moves past the current output and charges it to this frame's inspection budget. */
  fun advanceAndBillInspection(scanTop: Boolean) {
    if (scanTop) {
      walkedTops++
      inspectedTops++
      topIndex++
    } else {
      walkedBottoms++
      inspectedBottoms++
      bottomIndex--
    }
  }

  /** Whether [id] has already been inspected this frame, without recording it. */
  fun isInspected(id: Long): Boolean = inspectedIds.contains(id)

  /** Records [id] as seen this frame; returns `false` if it had already been inspected. */
  fun markInspected(id: Long): Boolean = inspectedIds.add(id)

  /** Whether the sweep has run off both ends and needs to start over. */
  fun hasCompletedFullPass(outputCount: Int): Boolean =
      topIndex !in 0 until outputCount && bottomIndex !in 0 until outputCount

  /** Drops all scheduling progress, as when the input is replaced. */
  fun reset() {
    frameSequence = Long.MIN_VALUE
    topIndex = 0
    bottomIndex = -1
    inspectedTops = 0
    inspectedBottoms = 0
    walkedTops = 0
    walkedBottoms = 0
    inspectedIds.clear()
    isInitialized = false
    mountInProgressId = IncrementalMountOutput.NO_HOST_ID
    pendingContentType = null
    pendingMountStartNs = 0L
  }

  private companion object {
    /**
     * Bounds how many candidates one frame may inspect per direction. Without it a run of already
     * resolved or unaffordable outputs could spend the whole frame searching and mount nothing.
     */
    private const val MAX_CANDIDATES_INSPECTED_PER_DIRECTION = 8
  }
}
