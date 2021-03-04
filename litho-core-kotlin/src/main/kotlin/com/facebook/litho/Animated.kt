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
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import main.kotlin.com.facebook.litho.Easing

object Animated {
  /**
   * Returns an [Animator] ready for running timing animation that calculate animated values and set
   * them on target param.
   *
   * By default it uses [AccelerateDecelerateInterpolator] and duration of 300 milliseconds.
   */
  fun timing(
      target: DynamicValue<Float>,
      to: Float,
      duration: Long = 300,
      easing: Interpolator = Easing.accelerateDecelerate(),
      onUpdate: ((Float) -> Unit)? = null,
      onFinish: (() -> Unit)? = null
  ): Animator {

    val animator = ValueAnimator.ofFloat(target.get(), to)
    animator.duration = duration
    animator.interpolator = easing
    animator.addUpdateListener { animation ->
      val animatedValue = animation.animatedValue as Float
      target.set(animatedValue)
      onUpdate?.invoke(animatedValue)
    }
    onFinish?.let {
      animator.addListener(
          object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
              onFinish()
            }
          })
    }
    return animator
  }
}
