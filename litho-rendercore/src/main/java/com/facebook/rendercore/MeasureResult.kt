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

package com.facebook.rendercore

import android.view.View.MeasureSpec
import com.facebook.rendercore.utils.MeasureSpecUtils
import kotlin.math.ceil
import kotlin.math.min

/** Encapsulates the measured size of a Mountable, and any layout data */
class MeasureResult {

  val width: Int
  val height: Int
  val layoutData: Any?
  val hadExceptions: Boolean

  @JvmOverloads
  constructor(width: Int, height: Int, layoutData: Any? = null) {
    this.width = width
    this.height = height
    this.layoutData = layoutData
    hadExceptions = false
  }

  /** This constructor should only be used if there were exceptions during measurement. */
  private constructor() {
    width = 0
    height = 0
    layoutData = null
    hadExceptions = true
  }

  override fun toString(): String =
      "MeasureResult:[width $width height $height layoutData $layoutData hadExceptions $hadExceptions]"

  companion object {

    /**
     * Returns a [MeasureResult] with sizes set based on the provided [widthSpec] and [heightSpec].
     *
     * This method should only be used for Mountable Components which do not measure themselves -
     * it's the parent that has determined the exact size for this child.
     *
     * @throws IllegalArgumentException if the [widthSpec] or [heightSpec] is not exact
     */
    @JvmStatic
    fun fromSpecs(widthSpec: Int, heightSpec: Int): MeasureResult {
      check(
          (MeasureSpecUtils.getMode(widthSpec) != MeasureSpec.EXACTLY ||
              MeasureSpecUtils.getMode(heightSpec) != MeasureSpec.EXACTLY)) {
            ("The sizes must be exact, but width is ${MeasureSpecUtils.getMeasureSpecDescription(widthSpec)} and height is ${MeasureSpecUtils.getMeasureSpecDescription(heightSpec)}")
          }
      return MeasureResult(
          MeasureSpecUtils.getSize(widthSpec), MeasureSpecUtils.getSize(heightSpec))
    }

    /**
     * Returns a [MeasureResult] to respect both size specs and try to keep both width and height
     * equal. This will only not guarantee equal width and height if these specs use modes and sizes
     * which prevent it.
     */
    @JvmStatic
    fun withEqualDimensions(widthSpec: Int, heightSpec: Int, layoutData: Any?): MeasureResult {
      val widthMode = MeasureSpec.getMode(widthSpec)
      val widthSize = MeasureSpec.getSize(widthSpec)
      val heightMode = MeasureSpec.getMode(heightSpec)
      val heightSize = MeasureSpec.getSize(heightSpec)
      if (widthMode == MeasureSpec.UNSPECIFIED && heightMode == MeasureSpec.UNSPECIFIED) {
        return MeasureResult(0, 0, layoutData)
      }
      val width: Int
      val height: Int
      when (widthMode) {
        MeasureSpec.EXACTLY -> {
          width = widthSize
          height =
              when (heightMode) {
                MeasureSpec.EXACTLY -> heightSize
                MeasureSpec.AT_MOST -> min(widthSize, heightSize)
                MeasureSpec.UNSPECIFIED -> widthSize
                else -> widthSize
              }
        }
        MeasureSpec.AT_MOST -> {
          when (heightMode) {
            MeasureSpec.EXACTLY -> {
              height = heightSize
              width = min(widthSize, heightSize)
            }
            MeasureSpec.AT_MOST -> {
              // if both are AT_MOST, choose the smaller one to keep width and height equal
              val chosenSize = min(widthSize, heightSize)
              width = chosenSize
              height = chosenSize
            }
            MeasureSpec.UNSPECIFIED -> {
              width = widthSize
              height = widthSize
            }
            else -> {
              width = widthSize
              height = widthSize
            }
          }
        }
        else -> {
          width = 0
          height = 0
        }
      }
      return MeasureResult(width, height)
    }

    /**
     * Returns a [MeasureResult] that respects both specs and the desired width and height. The
     * desired size is usually the necessary pixels to render the inner content.
     */
    @JvmStatic
    @JvmOverloads
    fun withDesiredPx(
        widthSpec: Int,
        heightSpec: Int,
        desiredWidthPx: Int,
        desiredHeightPx: Int,
        layoutData: Any? = null
    ): MeasureResult =
        MeasureResult(
            getResultSizePxWithSpecAndDesiredPx(widthSpec, desiredWidthPx),
            getResultSizePxWithSpecAndDesiredPx(heightSpec, desiredHeightPx),
            layoutData)

    private fun getResultSizePxWithSpecAndDesiredPx(spec: Int, desiredSize: Int): Int {
      return when (MeasureSpecUtils.getMode(spec)) {
        MeasureSpec.UNSPECIFIED -> desiredSize
        MeasureSpec.AT_MOST -> min(MeasureSpecUtils.getSize(spec), desiredSize)
        MeasureSpec.EXACTLY -> MeasureSpecUtils.getSize(spec)
        else -> throw IllegalStateException("Unexpected size spec mode")
      }
    }

    /**
     * Measure according to an aspect ratio an width and height constraints. This version of
     * forAspectRatio will respect the intrinsic size of the component being measured.
     *
     * @param widthSpec A SizeSpec for the width
     * @param heightSpec A SizeSpec for the height
     * @param intrinsicWidth A pixel value for the intrinsic width of the measured component
     * @param intrinsicHeight A pixel value for the intrinsic height of the measured component
     * @param aspectRatio The aspect ration size against
     */
    @JvmStatic
    fun forAspectRatio(
        widthSpec: Int,
        heightSpec: Int,
        intrinsicWidth: Int,
        intrinsicHeight: Int,
        aspectRatio: Float
    ): MeasureResult {
      var newWidthSpec = widthSpec
      if (MeasureSpecUtils.getMode(widthSpec) == MeasureSpec.AT_MOST &&
          MeasureSpecUtils.getSize(widthSpec) > intrinsicWidth) {
        newWidthSpec = MeasureSpecUtils.atMost(intrinsicWidth)
      }
      if (MeasureSpecUtils.getMode(heightSpec) == MeasureSpec.AT_MOST &&
          MeasureSpecUtils.getSize(heightSpec) > intrinsicHeight) {
        newWidthSpec = MeasureSpecUtils.atMost(intrinsicHeight)
      }
      return forAspectRatio(newWidthSpec, heightSpec, aspectRatio)
    }

    /**
     * Measure according to an aspect ratio an width and height constraints.
     *
     * @param widthSpec A SizeSpec for the width
     * @param heightSpec A SizeSpec for the height
     * @param aspectRatio The aspect ration size against
     */
    @JvmStatic
    fun forAspectRatio(widthSpec: Int, heightSpec: Int, aspectRatio: Float): MeasureResult {
      require(aspectRatio >= 0) { "The aspect ratio must be a positive number" }
      val widthMode = MeasureSpecUtils.getMode(widthSpec)
      val widthSize = MeasureSpecUtils.getSize(widthSpec)
      val heightMode = MeasureSpecUtils.getMode(heightSpec)
      val heightSize = MeasureSpecUtils.getSize(heightSpec)
      val widthBasedHeight = ceil((widthSize / aspectRatio)).toInt()
      val heightBasedWidth = ceil((heightSize * aspectRatio)).toInt()
      var outputWidth = 0
      var outputHeight = 0
      if (widthMode == MeasureSpec.UNSPECIFIED && heightMode == MeasureSpec.UNSPECIFIED) {
        // default to size {0, 0} because both width and height are UNSPECIFIED
        return MeasureResult(0, 0)
      }

      // Both modes are AT_MOST, find the largest possible size which respects both constraints.
      when {
        widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.AT_MOST -> {
          if (widthBasedHeight > heightSize) {
            outputWidth = heightBasedWidth
            outputHeight = heightSize
          } else {
            outputWidth = widthSize
            outputHeight = widthBasedHeight
          }
        }
        widthMode == MeasureSpec.EXACTLY -> {
          outputWidth = widthSize
          outputHeight =
              if (heightMode == MeasureSpec.UNSPECIFIED || widthBasedHeight <= heightSize) {
                widthBasedHeight
              } else {
                heightSize
              }
        }
        heightMode == MeasureSpec.EXACTLY -> {
          outputHeight = heightSize
          outputWidth =
              if (widthMode == MeasureSpec.UNSPECIFIED || heightBasedWidth <= widthSize) {
                heightBasedWidth
              } else {
                widthSize
              }
        }
        widthMode == MeasureSpec.AT_MOST -> {
          outputWidth = widthSize
          outputHeight = widthBasedHeight
        }
        heightMode == MeasureSpec.AT_MOST -> {
          outputWidth = heightBasedWidth
          outputHeight = heightSize
        }
      }
      return MeasureResult(outputWidth, outputHeight)
    }

    /**
     * Returns a [MeasureResult] with sizes set based on the provided [widthSpec] and [heightSpec]
     * if the spec mode is [MeasureSpec.EXACTLY] or [MeasureSpec.AT_MOST], otherwise uses fallback
     * value.
     *
     * @param widthSpec A SizeSpec for the width
     * @param heightSpec A SizeSpec for the height
     */
    @JvmStatic
    fun fillSpaceOrGone(widthSpec: Int, heightSpec: Int, layoutData: Any?): MeasureResult =
        fillSpace(widthSpec, heightSpec, 0, 0, layoutData)

    /**
     * Returns a [MeasureResult] with sizes set based on the provided [widthSpec] and [heightSpec]
     * if the spec mode is [MeasureSpec.EXACTLY] or [MeasureSpec.AT_MOST], otherwise uses fallback
     * value.
     *
     * @param widthSpec A SizeSpec for the width
     * @param heightSpec A SizeSpec for the height
     * @param widthFallback The width value for the [MeasureSpec.UNSPECIFIED] mode
     * @param heightFallback The height value for the [MeasureSpec.UNSPECIFIED] mode
     */
    @JvmStatic
    fun fillSpace(
        widthSpec: Int,
        heightSpec: Int,
        widthFallback: Int,
        heightFallback: Int,
        layoutData: Any?
    ): MeasureResult {
      val widthMode = MeasureSpec.getMode(widthSpec)
      val widthSize = MeasureSpec.getSize(widthSpec)
      val heightMode = MeasureSpec.getMode(heightSpec)
      val heightSize = MeasureSpec.getSize(heightSpec)
      if (widthMode == MeasureSpec.UNSPECIFIED && heightMode == MeasureSpec.UNSPECIFIED) {
        return MeasureResult(widthFallback, heightFallback, layoutData)
      }
      val width =
          when (widthMode) {
            MeasureSpec.EXACTLY,
            MeasureSpec.AT_MOST -> widthSize
            MeasureSpec.UNSPECIFIED -> widthFallback
            else -> widthFallback
          }
      val height =
          when (heightMode) {
            MeasureSpec.EXACTLY,
            MeasureSpec.AT_MOST -> heightSize
            MeasureSpec.UNSPECIFIED -> heightFallback
            else -> heightFallback
          }
      return MeasureResult(width, height, layoutData)
    }

    @JvmStatic fun error(): MeasureResult = MeasureResult()
  }
}
