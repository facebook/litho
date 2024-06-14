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

import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.widget.ImageView.ScaleType
import com.facebook.rendercore.FastMath
import kotlin.math.min

/** Static class containing a factory method for creating a matrix to apply to a drawable. */
class DrawableMatrix private constructor() : Matrix() {

  private var shouldClipRect = false

  /** @return True if this Matrix requires a clipRect() on the bounds of the drawable. */
  fun shouldClipRect(): Boolean {
    return shouldClipRect
  }

  companion object {

    /**
     * Create a matrix to be applied to a drawable which scales the drawable according to its scale
     * type.
     *
     * @param d The drawable to create a matrix for
     * @param scaleType A scale type describing how to scale the drawable.
     * @param width The width of the drawable's container.
     * @param height The height of the drawable's container.
     * @return The scale matrix or null if the drawable does not need to be scaled.
     */
    @JvmStatic
    fun create(d: Drawable?, scaleType: ScaleType?, width: Int, height: Int): DrawableMatrix? {
      if (d == null) {
        return null
      }

      val scaleType = scaleType ?: ScaleType.FIT_CENTER

      val intrinsicWidth = d.intrinsicWidth
      val intrinsicHeight = d.intrinsicHeight

      if (intrinsicWidth <= 0 ||
          intrinsicHeight <= 0 ||
          ScaleType.FIT_XY == scaleType ||
          ScaleType.MATRIX == scaleType) {
        return null
      }

      if (width == intrinsicWidth && height == intrinsicHeight) {
        // The bitmap fits exactly, no transform needed.
        return null
      }

      val result = DrawableMatrix()

      if (ScaleType.CENTER == scaleType) {
        // Center bitmap in view, no scaling.
        result.setTranslate(
            FastMath.round((width - intrinsicWidth) * 0.5f).toFloat(),
            FastMath.round((height - intrinsicHeight) * 0.5f).toFloat())

        result.shouldClipRect = intrinsicWidth > width || intrinsicHeight > height
      } else if (ScaleType.CENTER_CROP == scaleType) {
        val scale: Float
        var dx = 0f
        var dy = 0f

        if (intrinsicWidth * height > width * intrinsicHeight) {
          scale = height.toFloat() / intrinsicHeight.toFloat()
          dx = (width - intrinsicWidth * scale) * 0.5f
        } else {
          scale = width.toFloat() / intrinsicWidth.toFloat()
          dy = (height - intrinsicHeight * scale) * 0.5f
        }

        result.setScale(scale, scale)
        result.postTranslate(FastMath.round(dx).toFloat(), FastMath.round(dy).toFloat())

        result.shouldClipRect = true
      } else if (ScaleType.CENTER_INSIDE == scaleType) {
        val scale: Float =
            if (intrinsicWidth <= width && intrinsicHeight <= height) {
              1.0f
            } else {
              min(
                  width.toFloat() / intrinsicWidth.toFloat(),
                  height.toFloat() / intrinsicHeight.toFloat())
            }

        val dx = FastMath.round((width - intrinsicWidth * scale) * 0.5f).toFloat()
        val dy = FastMath.round((height - intrinsicHeight * scale) * 0.5f).toFloat()

        result.setScale(scale, scale)
        result.postTranslate(dx, dy)
      } else {
        val src = RectF()
        val dest = RectF()

        // Generate the required transform.
        src[0f, 0f, intrinsicWidth.toFloat()] = intrinsicHeight.toFloat()
        dest[0f, 0f, width.toFloat()] = height.toFloat()

        result.setRectToRect(src, dest, scaleTypeToScaleToFit(scaleType))
      }

      return result
    }

    private fun scaleTypeToScaleToFit(st: ScaleType): ScaleToFit {
      // ScaleToFit enum to their corresponding Matrix.ScaleToFit values
      return when (st) {
        ScaleType.FIT_XY -> ScaleToFit.FILL
        ScaleType.FIT_START -> ScaleToFit.START
        ScaleType.FIT_CENTER -> ScaleToFit.CENTER
        ScaleType.FIT_END -> ScaleToFit.END
        else -> throw IllegalArgumentException("Only FIT_... values allowed")
      }
    }
  }
}
