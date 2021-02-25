/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho

import android.animation.Animator
import android.animation.ValueAnimator

class Animated {
  companion object {
    fun timing(
        target: DynamicValue<Float>,
        to: Float,
        duration: Long = 300,
    ): Animator {

      val animator = ValueAnimator.ofFloat(target.get(), to)
      animator.duration = duration
      animator.addUpdateListener { animation ->
        val animatedValue = animation.animatedValue as Float
        target.set(animatedValue)
      }
      return animator
    }
  }
}
