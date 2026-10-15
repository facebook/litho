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

package com.facebook.litho.dataflow

import com.facebook.litho.choreographercompat.ChoreographerCompat

/** TimingSource and Choreographer implementation that allows manual stepping by frame in tests. */
class MockTimingSource : TimingSource, ChoreographerCompat {

  private val choreographerCallbacksToStartTimes:
      ArrayList<Pair<ChoreographerCompat.FrameCallback, Long>> =
      ArrayList()
  private var dataFlowGraph: DataFlowGraph? = null
  private var isRunning: Boolean = false
  private var currentTimeNanos: Long = 0

  override fun setDataFlowGraph(dataFlowGraph: DataFlowGraph) {
    this.dataFlowGraph = dataFlowGraph
  }

  override fun start() {
    isRunning = true
  }

  override fun stop() {
    isRunning = false
  }

  fun step(numFrames: Int) {
    val dataFlowGraph = dataFlowGraph ?: return
    for (i in 0 until numFrames) {
      if (!isRunning) {
        return
      }
      currentTimeNanos += FRAME_TIME_NANOS
      dataFlowGraph.doFrame(currentTimeNanos)
      fireChoreographerCallbacks()
    }
  }

  private fun fireChoreographerCallbacks() {
    var size = choreographerCallbacksToStartTimes.size
    var i = 0
    while (i < size) {
      val entry = choreographerCallbacksToStartTimes[i]
      if (entry.second <= currentTimeNanos) {
        entry.first.doFrame(currentTimeNanos)
        choreographerCallbacksToStartTimes.removeAt(i)
        size--
      } else {
        i++
      }
    }
  }

  override fun postFrameCallback(callbackWrapper: ChoreographerCompat.FrameCallback) {
    postFrameCallbackDelayed(callbackWrapper, 0)
  }

  override fun postFrameCallbackDelayed(
      callbackWrapper: ChoreographerCompat.FrameCallback,
      delayMillis: Long,
  ) {
    choreographerCallbacksToStartTimes.add(
        Pair(callbackWrapper, (currentTimeNanos + delayMillis * 1e6).toLong()),
    )
  }

  override fun removeFrameCallback(callbackWrapper: ChoreographerCompat.FrameCallback) {
    for (i in choreographerCallbacksToStartTimes.indices.reversed()) {
      val entry = choreographerCallbacksToStartTimes[i]
      if (entry.first === callbackWrapper) {
        choreographerCallbacksToStartTimes.removeAt(i)
      }
    }
  }

  companion object {
    @JvmField var FRAME_TIME_MS: Int = 16

    private val FRAME_TIME_NANOS: Long = (FRAME_TIME_MS * 1e6).toLong()
  }
}
