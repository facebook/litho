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

package com.facebook.litho

import android.os.Build
import android.view.View
import androidx.annotation.FloatRange
import com.facebook.litho.utils.VersionedAndroidApis

/**
 * An interface for a View which supports setting and unsetting its transform pivot, even on pre-API
 * 28 (P) devices.
 *
 * This interface exists because View#resetPivot was only introduced in API 28 (P), and due to View
 * recycling in Litho, we must have a way to restore all Views to their vanilla, newly created state
 * after they are unmounted.
 */
interface SupportsPivotTransform {

  companion object {
    const val BadPivotClassErrorMessage: String =
        "Setting transform pivot is only supported on Views that implement SupportsPivotTransform. " +
            "If it isn't possible to add this interface to the View in question, wrap this " +
            "Component in a Row or Column and apply the transform and pivot there instead."

    fun resetPivot(view: View) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        VersionedAndroidApis.P.resetPivot(view)
      } else {
        check(view is SupportsPivotTransform) { BadPivotClassErrorMessage }
        view.setTransformPivot(50f, 50f)
      }
    }
  }

  /**
   * Sets the transform pivot of a View - used for scale and rotation transforms - to be centered at
   * the given point, expressed as a percentage of the View's width and height. The default pivot
   * point is (50f, 50f).
   *
   * @param pivotXPercent the percentage of the View's width to use as the pivot point's
   *   x-coordinate, expressed from 0-100f
   * @param pivotYPercent the percentage of the View's height to use as the pivot point's
   *   y-coordinate, expressed from 0-100f
   * @see View.setPivotX
   * @see View.setPivotY
   */
  fun setTransformPivot(
      @FloatRange(0.0, 100.0) pivotXPercent: Float,
      @FloatRange(0.0, 100.0) pivotYPercent: Float
  )
}
