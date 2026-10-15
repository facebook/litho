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

import android.animation.TimeInterpolator

/**
 * A [ValueNode] that will update its value by mapping an input value between 0 to 1.0 representing
 * elapsed fraction of animation to a value that represents interpolated fraction.
 */
class InterpolatorNode(private val interpolator: TimeInterpolator) : ValueNode() {

  override fun calculateValue(frameTimeNanos: Long): Float {
    val timingValue = getInput(DEFAULT_INPUT).value
    return interpolator.getInterpolation(timingValue)
  }
}
