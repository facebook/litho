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

package com.facebook.litho.widget

import android.content.Context
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearSmoothScroller

/** LinearSmoothScroller subclass that snaps the target position to center. */
class CenterSnappingSmoothScroller(context: Context, private val offset: Int) :
    LinearSmoothScroller(context) {

  override fun calculateDtToFit(
      viewStart: Int,
      viewEnd: Int,
      boxStart: Int,
      boxEnd: Int,
      snapPreference: Int
  ): Int {
    val result = (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2)
    return result + offset
  }

  override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
    return MILLISECONDS_PER_INCH / displayMetrics.densityDpi
  }

  companion object {
    private const val MILLISECONDS_PER_INCH = 100f
  }
}
