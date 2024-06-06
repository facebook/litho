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

package com.facebook.litho.utils

import android.util.Log
import android.view.View.MeasureSpec
import com.facebook.litho.Size
import com.facebook.litho.SizeSpec
import com.facebook.litho.SizeSpec.getMode
import com.facebook.litho.SizeSpec.getSize
import com.facebook.litho.SizeSpec.makeSizeSpec
import com.facebook.litho.SizeSpec.toString
import com.facebook.litho.config.LithoDebugConfigurations
import com.facebook.rendercore.MeasureResult
import kotlin.math.ceil
import kotlin.math.min

object MeasureUtils {
  private const val TAG = "MeasureUtils"

  @JvmStatic
  fun getViewMeasureSpec(sizeSpec: Int): Int {
    return when (getMode(sizeSpec)) {
      SizeSpec.EXACTLY -> MeasureSpec.makeMeasureSpec(getSize(sizeSpec), MeasureSpec.EXACTLY)
      SizeSpec.AT_MOST -> MeasureSpec.makeMeasureSpec(getSize(sizeSpec), MeasureSpec.AT_MOST)
      SizeSpec.UNSPECIFIED ->
          MeasureSpec.makeMeasureSpec(getSize(sizeSpec), MeasureSpec.UNSPECIFIED)
      else -> throw IllegalStateException("Unexpected size spec mode")
    }
  }

  /**
   * Set the [outputSize] to respect both Specs and the desired width and height. The desired size
   * is usually the necessary pixels to render the inner content.
   */
  @JvmStatic
  fun measureWithDesiredPx(
      widthSpec: Int,
      heightSpec: Int,
      desiredWidthPx: Int,
      desiredHeightPx: Int,
      outputSize: Size
  ) {
    outputSize.width = getResultSizePxWithSpecAndDesiredPx(widthSpec, desiredWidthPx)
    outputSize.height = getResultSizePxWithSpecAndDesiredPx(heightSpec, desiredHeightPx)
  }

  private fun getResultSizePxWithSpecAndDesiredPx(spec: Int, desiredSize: Int): Int {
    return when (getMode(spec)) {
      SizeSpec.UNSPECIFIED -> desiredSize
      SizeSpec.AT_MOST -> min(getSize(spec), desiredSize)
      SizeSpec.EXACTLY -> getSize(spec)
      else -> throw IllegalStateException("Unexpected size spec mode")
    }
  }

  /**
   * Set the [outputSize] to respect both size specs and try to keep both width and height equal.
   * This will only not guarantee equal width and height if these specs use modes and sizes which
   * prevent it.
   */
  @JvmStatic
  fun measureWithEqualDimens(widthSpec: Int, heightSpec: Int, outputSize: Size) {
    val widthMode = getMode(widthSpec)
    val widthSize = getSize(widthSpec)
    val heightMode = getMode(heightSpec)
    val heightSize = getSize(heightSpec)
    if (widthMode == SizeSpec.UNSPECIFIED && heightMode == SizeSpec.UNSPECIFIED) {
      outputSize.width = 0
      outputSize.height = 0
      if (LithoDebugConfigurations.isDebugModeEnabled) {
        Log.d(TAG, "Default to size {0, 0} because both width and height are UNSPECIFIED")
      }
      return
    }
    if (widthMode == SizeSpec.EXACTLY) {
      outputSize.width = widthSize
      when (heightMode) {
        SizeSpec.EXACTLY -> {
          outputSize.height = heightSize
          return
        }
        SizeSpec.AT_MOST -> {
          outputSize.height = min(widthSize, heightSize)
          return
        }
        SizeSpec.UNSPECIFIED -> {
          outputSize.height = widthSize
          return
        }
      }
    } else if (widthMode == SizeSpec.AT_MOST) {
      when (heightMode) {
        SizeSpec.EXACTLY -> {
          outputSize.height = heightSize
          outputSize.width = min(widthSize, heightSize)
          return
        }
        SizeSpec.AT_MOST -> {
          // if both are AT_MOST, choose the smaller one to keep width and height equal
          val chosenSize = min(widthSize, heightSize)
          outputSize.width = chosenSize
          outputSize.height = chosenSize
          return
        }
        SizeSpec.UNSPECIFIED -> {
          outputSize.width = widthSize
          outputSize.height = widthSize
          return
        }
      }
    }

    // heightMode is either EXACTLY or AT_MOST, and widthMode is UNSPECIFIED
    outputSize.height = heightSize
    outputSize.width = heightSize
  }

  /**
   * Measure according to an aspect ratio an width and height constraints. This version of
   * measureWithAspectRatio will respect the intrinsic size of the component being measured.
   *
   * @param widthSpec A SizeSpec for the width
   * @param heightSpec A SizeSpec for the height
   * @param intrinsicWidth A pixel value for the intrinsic width of the measured component
   * @param intrinsicHeight A pixel value for the intrinsic height of the measured component
   * @param aspectRatio The aspect ratio size against
   * @param outputSize The output size of this measurement
   */
  @JvmStatic
  fun measureWithAspectRatio(
      widthSpec: Int,
      heightSpec: Int,
      intrinsicWidth: Int,
      intrinsicHeight: Int,
      aspectRatio: Float,
      outputSize: Size
  ) {
    var resolvedWidthSpec = widthSpec
    var resolvedHeightSpec = heightSpec
    if (getMode(resolvedWidthSpec) == SizeSpec.AT_MOST &&
        getSize(resolvedWidthSpec) > intrinsicWidth) {
      resolvedWidthSpec = makeSizeSpec(intrinsicWidth, SizeSpec.AT_MOST)
    }
    if (getMode(resolvedHeightSpec) == SizeSpec.AT_MOST &&
        getSize(resolvedHeightSpec) > intrinsicHeight) {
      resolvedHeightSpec = makeSizeSpec(intrinsicHeight, SizeSpec.AT_MOST)
    }
    measureWithAspectRatio(resolvedWidthSpec, resolvedHeightSpec, aspectRatio, outputSize)
  }

  /**
   * Measure according to an aspect ratio an width and height constraints. This version of
   * measureWithAspectRatio will respect the intrinsic size of the component being measured.
   *
   * @param widthSpec A SizeSpec for the width
   * @param heightSpec A SizeSpec for the height
   * @param intrinsicWidth A pixel value for the intrinsic width of the measured component
   * @param intrinsicHeight A pixel value for the intrinsic height of the measured component
   * @param aspectRatio The aspect ratio size against
   */
  @JvmStatic
  fun measureResultUsingAspectRatio(
      widthSpec: Int,
      heightSpec: Int,
      intrinsicWidth: Int,
      intrinsicHeight: Int,
      aspectRatio: Float,
      layoutData: Any?
  ): MeasureResult {
    val size = Size()
    measureWithAspectRatio(
        widthSpec, heightSpec, intrinsicWidth, intrinsicHeight, aspectRatio, size)
    return MeasureResult(size.width, size.height, layoutData)
  }

  /**
   * Measure according to an aspect ratio an width and height constraints.
   *
   * @param widthSpec A SizeSpec for the width
   * @param heightSpec A SizeSpec for the height
   * @param aspectRatio The aspect ratio size against
   * @param outputSize The output size of this measurement
   */
  @JvmStatic
  fun measureWithAspectRatio(
      widthSpec: Int,
      heightSpec: Int,
      aspectRatio: Float,
      outputSize: Size
  ) {
    // To avoid instant crashes, we need to identify all bad usages and log them, so we ignore them
    // for now.
    val isInvalidAspectRatio = aspectRatio.isNaN() || aspectRatio.isInfinite() || aspectRatio == 0f
    require(isInvalidAspectRatio || aspectRatio > 0) {
      "The aspect ratio must be a positive number"
    }

    val widthMode = getMode(widthSpec)
    val widthSize = getSize(widthSpec)
    val heightMode = getMode(heightSpec)
    val heightSize = getSize(heightSpec)
    val widthBasedHeight = ceil(widthSize / aspectRatio).toInt()
    val heightBasedWidth = ceil(heightSize * aspectRatio).toInt()
    if (widthMode == SizeSpec.UNSPECIFIED && heightMode == SizeSpec.UNSPECIFIED) {
      outputSize.width = 0
      outputSize.height = 0
      if (LithoDebugConfigurations.isDebugModeEnabled) {
        Log.d(TAG, "Default to size {0, 0} because both width and height are UNSPECIFIED")
      }
      return
    }

    // Both modes are AT_MOST, find the largest possible size which respects both constraints.
    if (widthMode == SizeSpec.AT_MOST && heightMode == SizeSpec.AT_MOST) {
      if (widthBasedHeight > heightSize) {
        outputSize.width = heightBasedWidth
        outputSize.height = heightSize
      } else {
        outputSize.width = widthSize
        outputSize.height = widthBasedHeight
      }
    } else if (widthMode == SizeSpec.EXACTLY) {
      // Width is set to exact measurement and the height is either unspecified or is allowed to be
      // large enough to accommodate the given aspect ratio.
      outputSize.width = widthSize
      if (heightMode == SizeSpec.UNSPECIFIED || widthBasedHeight <= heightSize) {
        outputSize.height = widthBasedHeight
      } else {
        outputSize.height = heightSize
        if (LithoDebugConfigurations.isDebugModeEnabled) {
          Log.d(
              TAG,
              String.format(
                  "Ratio makes height larger than allowed. w:%s h:%s aspectRatio:%f",
                  toString(widthSpec),
                  toString(heightSpec),
                  aspectRatio))
        }
      }
    } else if (heightMode == SizeSpec.EXACTLY) {
      // Height is set to exact measurement and the width is either unspecified or is allowed to be
      // large enough to accommodate the given aspect ratio.
      outputSize.height = heightSize
      if (widthMode == SizeSpec.UNSPECIFIED || heightBasedWidth <= widthSize) {
        outputSize.width = heightBasedWidth
      } else {
        outputSize.width = widthSize
        if (LithoDebugConfigurations.isDebugModeEnabled) {
          Log.d(
              TAG,
              String.format(
                  "Ratio makes width larger than allowed. w:%s h:%s aspectRatio:%f",
                  toString(widthSpec),
                  toString(heightSpec),
                  aspectRatio))
        }
      }
    } else if (widthMode == SizeSpec.AT_MOST) {
      // Width is set to at most measurement. If that is the case heightMode must be unspecified.
      outputSize.width = widthSize
      outputSize.height = widthBasedHeight
    } else if (heightMode == SizeSpec.AT_MOST) {
      // Height is set to at most measurement. If that is the case widthMode must be unspecified.
      outputSize.width = heightBasedWidth
      outputSize.height = heightSize
    }
  }
}
