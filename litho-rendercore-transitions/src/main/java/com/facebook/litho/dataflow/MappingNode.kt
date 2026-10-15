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
 * A [ValueNode] that will update its value from its "initial" input to its "end" input according to
 * the given input within expected range [0, 1]. For example [MappingNode] could accept values
 * emitted by [InterpolatorNode]. The node can also extrapolate beyond the output range if input
 * goes beyond the expected input range.
 */
class MappingNode : ValueNode() {

  override fun calculateValue(frameTimeNanos: Long): Float {
    val initialValue = getInput(INITIAL_INPUT).value
    val endValue = getInput(END_INPUT).value
    val fractionValue = getInput(DEFAULT_INPUT).value

    val valRange = endValue - initialValue
    return initialValue + fractionValue * valRange
  }

  companion object {
    const val INITIAL_INPUT: String = "initial"
    const val END_INPUT: String = "end"
  }
}
