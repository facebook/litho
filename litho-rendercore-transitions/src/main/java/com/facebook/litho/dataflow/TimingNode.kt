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

/**
 * A [ValueNode] that will linearly update its value from 0 to 1.0 over the course of the given
 * duration.
 */
class TimingNode(private val durationMs: Int) : ValueNode(), NodeCanFinish {

  private var startTimeNs: Long = Long.MIN_VALUE
  private var expectedEndTimeNs: Long = Long.MIN_VALUE
  private var lastValueTimeNs: Long = Long.MIN_VALUE

  override fun calculateValue(frameTimeNanos: Long): Float {
    if (lastValueTimeNs == Long.MIN_VALUE) {
      startTimeNs = frameTimeNanos
      lastValueTimeNs = frameTimeNanos
      expectedEndTimeNs = startTimeNs + durationMs.toLong() * MS_IN_NANOS
      return INITIAL_VALUE
    }

    if (frameTimeNanos >= expectedEndTimeNs) {
      lastValueTimeNs = frameTimeNanos
      return END_VALUE
    }

    lastValueTimeNs = frameTimeNanos
    return (frameTimeNanos - startTimeNs).toFloat() / (expectedEndTimeNs - startTimeNs).toFloat()
  }

  override fun isFinished(): Boolean = lastValueTimeNs >= expectedEndTimeNs

  companion object {
    private const val MS_IN_NANOS: Int = 1_000_000
    private const val INITIAL_VALUE: Float = 0.0f
    private const val END_VALUE: Float = 1.0f
  }
}
