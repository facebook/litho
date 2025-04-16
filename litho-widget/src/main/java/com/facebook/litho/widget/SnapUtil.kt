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
import androidx.annotation.IntDef
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper

/** Utility class containing snapping related behavior of [RecyclerView]. */
object SnapUtil {

  /* No snap helper is required */
  const val SNAP_NONE: Int = Int.MIN_VALUE
  const val SNAP_TO_END: Int = LinearSmoothScroller.SNAP_TO_END
  /* This snap mode will cause a StartSnapHelper to be used */
  const val SNAP_TO_START: Int = LinearSmoothScroller.SNAP_TO_START
  /* This snap mode will cause a PagerSnapHelper to be used */
  const val SNAP_TO_CENTER: Int = Int.MAX_VALUE
  /* This snap mode will cause a LinearSnapHelper to be used */
  const val SNAP_TO_CENTER_CHILD: Int = Int.MAX_VALUE - 1

  /* This snap mode will cause a custom LinearSnapHelper to be used */
  const val SNAP_TO_CENTER_CHILD_WITH_CUSTOM_SPEED: Int = Int.MAX_VALUE - 2

  /* The default fling offset for StartSnapHelper */
  const val SNAP_TO_START_DEFAULT_FLING_OFFSET: Int = 1

  @JvmStatic
  fun getSnapHelper(
      @SnapMode snapMode: Int,
      deltaJumpThreshold: Int,
      snapToStartFlingOffset: Int,
      snapToStartOffset: Int,
      useExactScrollPosition: Boolean
  ): SnapHelper? =
      when (snapMode) {
        SNAP_TO_CENTER -> PagerSnapHelper()
        SNAP_TO_START -> StartSnapHelper(snapToStartFlingOffset, snapToStartOffset)

        SNAP_TO_CENTER_CHILD -> LinearSnapHelper()
        SNAP_TO_CENTER_CHILD_WITH_CUSTOM_SPEED ->
            CustomSpeedLinearSnapHelper(deltaJumpThreshold, useExactScrollPosition)

        SNAP_TO_END,
        SNAP_NONE -> null
        else -> null
      }

  /**
   * @return [androidx.recyclerview.widget.RecyclerView.SmoothScroller] that takes snapping into
   *   account.
   */
  @JvmStatic
  fun getSmoothScrollerWithOffset(
      context: Context,
      offset: Int,
      type: SmoothScrollAlignmentType
  ): RecyclerView.SmoothScroller =
      when (type) {
        SmoothScrollAlignmentType.SNAP_TO_ANY,
        SmoothScrollAlignmentType.SNAP_TO_START,
        SmoothScrollAlignmentType.SNAP_TO_END ->
            EdgeSnappingSmoothScroller(context, type.value, offset)

        SmoothScrollAlignmentType.SNAP_TO_CENTER -> CenterSnappingSmoothScroller(context, offset)
        else -> LinearSmoothScroller(context)
      }

  @JvmStatic
  fun getSnapModeFromString(snapString: String?): Int =
      when (snapString) {
        null -> SNAP_NONE
        "SNAP_TO_END" -> SNAP_TO_END
        "SNAP_TO_START" -> SNAP_TO_START
        "SNAP_TO_CENTER" -> SNAP_TO_CENTER
        "SNAP_TO_CENTER_CHILD" -> SNAP_TO_CENTER_CHILD
        "SNAP_TO_CENTER_CHILD_WITH_CUSTOM_SPEED" -> SNAP_TO_CENTER_CHILD_WITH_CUSTOM_SPEED
        "SNAP_NONE" -> SNAP_NONE
        else -> SNAP_NONE
      }

  @IntDef(
      SNAP_NONE,
      SNAP_TO_END,
      SNAP_TO_START,
      SNAP_TO_CENTER,
      SNAP_TO_CENTER_CHILD,
      SNAP_TO_CENTER_CHILD_WITH_CUSTOM_SPEED)
  @Retention(AnnotationRetention.SOURCE)
  annotation class SnapMode
}
