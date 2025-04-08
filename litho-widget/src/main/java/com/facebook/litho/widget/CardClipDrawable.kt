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

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.core.graphics.withSave

class CardClipDrawable : Drawable() {
  private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
  private val cornerPath = Path()

  private var disableClipCorners = NONE

  private var cornerRadius = 0f
  private var dirty = true

  override fun setAlpha(alpha: Int) {
    cornerPaint.alpha = alpha
  }

  override fun setColorFilter(cf: ColorFilter?) {
    cornerPaint.setColorFilter(cf)
  }

  override fun getOpacity(): Int {
    return PixelFormat.TRANSLUCENT
  }

  fun setDisableClip(edge: Int) {
    if ((disableClipCorners and edge) != 0) {
      return
    }
    disableClipCorners = edge
    dirty = true
    invalidateSelf()
  }

  override fun draw(canvas: Canvas) {
    if (dirty) {
      buildClippingCorners()
      dirty = false
    }

    val bounds = bounds

    if ((disableClipCorners and TOP_LEFT) == 0) {
      canvas.withSave {
        translate(bounds.left.toFloat(), bounds.top.toFloat())
        drawPath(cornerPath, cornerPaint)
      }
    }

    if ((disableClipCorners and BOTTOM_RIGHT) == 0) {
      canvas.withSave {
        translate(bounds.right.toFloat(), bounds.bottom.toFloat())
        rotate(180f)
        drawPath(cornerPath, cornerPaint)
      }
    }

    if ((disableClipCorners and BOTTOM_LEFT) == 0) {
      canvas.withSave {
        translate(bounds.left.toFloat(), bounds.bottom.toFloat())
        rotate(270f)
        drawPath(cornerPath, cornerPaint)
      }
    }

    if ((disableClipCorners and TOP_RIGHT) == 0) {
      canvas.withSave {
        translate(bounds.right.toFloat(), bounds.top.toFloat())
        rotate(90f)
        drawPath(cornerPath, cornerPaint)
      }
    }
  }

  fun setClippingColor(clippingColor: Int) {
    if (cornerPaint.color == clippingColor) {
      return
    }

    cornerPaint.color = clippingColor
    dirty = true
    invalidateSelf()
  }

  fun setCornerRadius(radius: Float) {
    var newRadius = (radius + .5f).toInt().toFloat()
    if (cornerRadius == newRadius) {
      return
    }

    cornerRadius = newRadius
    dirty = true
    invalidateSelf()
  }

  private fun buildClippingCorners() {
    cornerPath.reset()

    val oval = RectF(0f, 0f, cornerRadius * 2, cornerRadius * 2)

    cornerPath.fillType = Path.FillType.EVEN_ODD
    cornerPath.moveTo(0f, 0f)
    cornerPath.lineTo(0f, cornerRadius)
    cornerPath.arcTo(oval, 180f, 90f, true)
    cornerPath.lineTo(0f, 0f)

    cornerPath.close()
  }

  companion object {
    const val NONE: Int = 0
    const val TOP_LEFT: Int = 1 shl 0
    const val TOP_RIGHT: Int = 1 shl 1
    const val BOTTOM_LEFT: Int = 1 shl 2
    const val BOTTOM_RIGHT: Int = 1 shl 3
  }
}
