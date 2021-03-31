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

package com.facebook.litho.animated

import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.view.animation.PathInterpolatorCompat

object Easing {

  /** quadratic Bezier curve */
  fun bezier(x: Float, y: Float) = PathInterpolatorCompat.create(x, y)

  /** cubic Bezier curve */
  fun bezier(x1: Float, y1: Float, x2: Float, y2: Float) =
      PathInterpolatorCompat.create(x1, y1, x2, y2)

  fun linear() = LinearInterpolator()

  fun accelerate(factor: Float = 1.0f) = AccelerateInterpolator(factor)

  fun decelerate(factor: Float = 1.0f) = DecelerateInterpolator(factor)

  /**
   * An interpolator where the rate of change starts and ends slowly but accelerates through the
   * middle.
   */
  fun accelerateDecelerate() = AccelerateDecelerateInterpolator()

  fun bounce() = BounceInterpolator()

  /**
   * An interpolator where the change flings forward and overshoots the last value then comes back.
   */
  fun overshoot(tension: Float = 2.0f) = OvershootInterpolator(tension)
}
