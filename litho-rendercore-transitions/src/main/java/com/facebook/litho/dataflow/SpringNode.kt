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

import com.facebook.litho.dataflow.springs.Spring
import com.facebook.litho.dataflow.springs.SpringConfig

/**
 * A node that implements spring physics: it takes an initial value ("initial" input) and end value
 * ("end" input) and animates that value on each frame, outputting the progress over time.
 */
class SpringNode @JvmOverloads constructor(springConfig: SpringConfig? = null) :
    ValueNode(), NodeCanFinish {

  private val spring: Spring =
      Spring().apply {
        if (springConfig != null) {
          setSpringConfig(springConfig)
        }
      }
  private var lastFrameTimeNs: Long = Long.MIN_VALUE

  override fun calculateValue(frameTimeNanos: Long): Float {
    if (lastFrameTimeNs == Long.MIN_VALUE) {
      lastFrameTimeNs = frameTimeNanos
      val initialValue = getInput(INITIAL_INPUT).value
      val endValue = getInput(END_INPUT).value
      spring.setCurrentValue(initialValue.toDouble())
      spring.setEndValue(endValue.toDouble())
      return initialValue
    }

    val endValue = getInput(END_INPUT).value
    spring.setEndValue(endValue.toDouble())
    if (isFinished()) {
      return endValue
    }

    val timeDeltaSec = (frameTimeNanos - lastFrameTimeNs) / NS_PER_SECOND
    spring.advance(timeDeltaSec)
    lastFrameTimeNs = frameTimeNanos

    return spring.getCurrentValue().toFloat()
  }

  override fun isFinished(): Boolean = spring.isAtRest()

  companion object {
    const val NS_PER_SECOND: Double = 1_000_000_000.0
    const val INITIAL_INPUT: String = "initial"
    const val END_INPUT: String = "end"
  }
}
