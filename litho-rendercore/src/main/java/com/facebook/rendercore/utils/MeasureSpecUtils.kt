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

package com.facebook.rendercore.utils

import android.view.View.MeasureSpec
import java.util.Locale

/**
 * An utility class to verify that an old measured size is still compatible to be used with a new
 * measureSpec.
 */
object MeasureSpecUtils {

  private const val DELTA = 0.5f
  private val UNSPECIFIED = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)

  @JvmStatic fun unspecified(): Int = UNSPECIFIED

  @JvmStatic fun atMost(px: Int): Int = MeasureSpec.makeMeasureSpec(px, MeasureSpec.AT_MOST)

  @JvmStatic fun exactly(px: Int): Int = MeasureSpec.makeMeasureSpec(px, MeasureSpec.EXACTLY)

  @JvmStatic fun getMode(spec: Int): Int = MeasureSpec.getMode(spec)

  @JvmStatic fun getSize(spec: Int): Int = MeasureSpec.getSize(spec)

  @JvmStatic
  fun areMeasureSpecsEquivalent(specA: Int, specB: Int): Boolean =
      specA == specB ||
          (MeasureSpec.getMode(specA) == UNSPECIFIED && MeasureSpec.getMode(specB) == UNSPECIFIED)

  @JvmStatic
  fun isMeasureSpecCompatible(oldSizeSpec: Int, sizeSpec: Int, oldMeasuredSize: Int): Boolean {
    val newSpecMode = MeasureSpec.getMode(sizeSpec)
    val newSpecSize = MeasureSpec.getSize(sizeSpec)
    val oldSpecMode = MeasureSpec.getMode(oldSizeSpec)
    val oldSpecSize = MeasureSpec.getSize(oldSizeSpec)
    return (oldSizeSpec == sizeSpec) ||
        (oldSpecMode == UNSPECIFIED && newSpecMode == UNSPECIFIED) ||
        newSizeIsExactAndMatchesOldMeasuredSize(
            newSpecMode, newSpecSize, oldMeasuredSize.toFloat()) ||
        oldSizeIsUnspecifiedAndStillFits(
            oldSpecMode, newSpecMode, newSpecSize, oldMeasuredSize.toFloat()) ||
        newMeasureSizeIsStricterAndStillValid(
            oldSpecMode, newSpecMode, oldSpecSize, newSpecSize, oldMeasuredSize.toFloat())
  }

  private fun newSizeIsExactAndMatchesOldMeasuredSize(
      newSizeSpecMode: Int,
      newSizeSpecSize: Int,
      oldMeasuredSize: Float
  ): Boolean =
      newSizeSpecMode == MeasureSpec.EXACTLY && Math.abs(newSizeSpecSize - oldMeasuredSize) < DELTA

  private fun oldSizeIsUnspecifiedAndStillFits(
      oldSizeSpecMode: Int,
      newSizeSpecMode: Int,
      newSizeSpecSize: Int,
      oldMeasuredSize: Float
  ): Boolean =
      newSizeSpecMode == MeasureSpec.AT_MOST &&
          oldSizeSpecMode == UNSPECIFIED &&
          newSizeSpecSize >= oldMeasuredSize

  private fun newMeasureSizeIsStricterAndStillValid(
      oldSizeSpecMode: Int,
      newSizeSpecMode: Int,
      oldSizeSpecSize: Int,
      newSizeSpecSize: Int,
      oldMeasuredSize: Float
  ): Boolean =
      oldSizeSpecMode == MeasureSpec.AT_MOST &&
          newSizeSpecMode == MeasureSpec.AT_MOST &&
          oldSizeSpecSize > newSizeSpecSize &&
          oldMeasuredSize <= newSizeSpecSize

  @JvmStatic
  fun getMeasureSpecDescription(measureSpec: Int): String {
    val value = getSize(measureSpec)
    val mode = getModeDescription(getMode(measureSpec))
    return String.format(Locale.US, "[%d, %s]", value, mode)
  }

  @JvmStatic
  fun getModeDescription(mode: Int): String =
      when (mode) {
        MeasureSpec.AT_MOST -> "AT_MOST"
        MeasureSpec.EXACTLY -> "EXACTLY"
        UNSPECIFIED -> "UNSPECIFIED"
        else -> "INVALID"
      }
}
