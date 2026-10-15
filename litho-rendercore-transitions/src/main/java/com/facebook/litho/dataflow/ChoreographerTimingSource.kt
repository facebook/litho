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
import com.facebook.litho.choreographercompat.ChoreographerCompatImpl

/** The default [TimingSource] hooked into Android's Choreographer. */
class ChoreographerTimingSource : TimingSource {

  private val choreographerCompat: ChoreographerCompat = ChoreographerCompatImpl.getInstance()
  private val frameCallback: ChoreographerCompat.FrameCallback =
      object : ChoreographerCompat.FrameCallback() {
        override fun doFrame(frameTimeNanos: Long) {
          this@ChoreographerTimingSource.doFrame(frameTimeNanos)
        }
      }
  private var dataFlowGraph: DataFlowGraph? = null
  private var isRunning: Boolean = false
  private var hasPostedFrameCallback: Boolean = false
  private var lastFrameTime: Long = Long.MIN_VALUE

  override fun setDataFlowGraph(dataFlowGraph: DataFlowGraph) {
    this.dataFlowGraph = dataFlowGraph
  }

  override fun start() {
    dataFlowGraph ?: throw RuntimeException("Must set a binding graph first.")
    if (isRunning) {
      throw RuntimeException("Tried to start but was already running.")
    }
    isRunning = true
    postFrameCallback()
  }

  override fun stop() {
    if (!isRunning) {
      throw RuntimeException("Tried to stop but wasn't running.")
    }
    isRunning = false
    stopFrameCallback()
  }

  private fun postFrameCallback() {
    if (hasPostedFrameCallback) {
      return
    }
    choreographerCompat.postFrameCallback(frameCallback)
    hasPostedFrameCallback = true
  }

  private fun stopFrameCallback() {
    choreographerCompat.removeFrameCallback(frameCallback)
    hasPostedFrameCallback = false
  }

  private fun doFrame(frameTimeNanos: Long) {
    hasPostedFrameCallback = false
    if (!isRunning) {
      return
    }

    val dataFlowGraph = dataFlowGraph ?: return

    // Sometimes Choreographer can call doFrame multiple times with the same frame time, especially
    // in the case of skipped frames. De-bounce it here.
    if (lastFrameTime != frameTimeNanos) {
      dataFlowGraph.doFrame(frameTimeNanos)
      lastFrameTime = frameTimeNanos
    }

    if (isRunning) {
      postFrameCallback()
    }
  }
}
