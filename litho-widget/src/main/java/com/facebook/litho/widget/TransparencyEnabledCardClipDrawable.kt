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
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.core.graphics.withSave
import java.util.Arrays

class TransparencyEnabledCardClipDrawable : Drawable() {

  private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
  private val cornerPath = Path()
  private var cornerPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
  private var dirty = true
  private var disableClipCorners = NONE
  private var cornerRadius = 0f
  private var backgroundDrawable: Drawable? = null
  private val drawableCornerRadii = FloatArray(CORNER_RADII_LIST_SIZE)
  private val rectF = RectF()

  override fun setAlpha(alpha: Int) {
    backgroundPaint.alpha = alpha
  }

  override fun setColorFilter(cf: ColorFilter?) {
    backgroundPaint.setColorFilter(cf)
  }

  override fun getOpacity(): Int {
    return PixelFormat.TRANSLUCENT
  }

  override fun draw(canvas: Canvas) {
    val bounds = bounds
    rectF.set(bounds)
    if (areAllDisableClipFalse()) {
      canvas.drawRect(bounds, cornerPaint)
      drawWithRoundRectImplementation(canvas, bounds)
    } else {
      drawWithClipPathImplementation(canvas, bounds)
    }
  }

  private fun drawWithClipPathImplementation(canvas: Canvas, bounds: Rect) {
    if (dirty) {
      buildClippingCorners()
      dirty = false
    }

    if (backgroundDrawable != null) {
      drawWithClipPathImplementationOnDrawable(canvas, bounds)
      return
    }

    canvas.drawRect(bounds, backgroundPaint)
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

  private fun drawWithClipPathImplementationOnDrawable(canvas: Canvas, bounds: Rect) {
    val topLeftRadius = if ((disableClipCorners and TOP_LEFT) == 0) cornerRadius else 0f
    val topRightRadius = if ((disableClipCorners and TOP_RIGHT) == 0) cornerRadius else 0f
    val bottomLeftRadius = if ((disableClipCorners and BOTTOM_LEFT) == 0) cornerRadius else 0f
    val bottomRightRadius = if ((disableClipCorners and BOTTOM_RIGHT) == 0) cornerRadius else 0f

    drawableCornerRadii[0] = topLeftRadius
    drawableCornerRadii[1] = topLeftRadius
    drawableCornerRadii[2] = topRightRadius
    drawableCornerRadii[3] = topRightRadius
    drawableCornerRadii[4] = bottomRightRadius
    drawableCornerRadii[5] = bottomRightRadius
    drawableCornerRadii[6] = bottomLeftRadius
    drawableCornerRadii[7] = bottomLeftRadius

    drawOnBackgroundDrawable(canvas, bounds)
  }

  private fun drawOnBackgroundDrawable(canvas: Canvas, bounds: Rect) {
    backgroundDrawable?.let { drawable ->
      canvas.withSave {
        cornerPath.reset()
        cornerPath.addRoundRect(rectF, drawableCornerRadii, Path.Direction.CW)
        clipPath(cornerPath)
        drawable.bounds = bounds
        drawable.draw(this)
      }

      // Reset the path and rectF
      cornerPath.reset()
      rectF.setEmpty()
    }
  }

  private fun drawWithRoundRectImplementation(canvas: Canvas, bounds: Rect) {
    if (backgroundDrawable != null) {
      drawOnBackgroundDrawable(canvas, bounds)
    } else {
      canvas.drawRoundRect(
          bounds.left.toFloat(),
          bounds.top.toFloat(),
          bounds.right.toFloat(),
          bounds.bottom.toFloat(),
          cornerRadius,
          cornerRadius,
          backgroundPaint)
    }
  }

  fun setBackgroundColor(backgroundColor: Int) {
    if (backgroundPaint.color == backgroundColor) {
      return
    }

    backgroundPaint.color = backgroundColor
    invalidateSelf()
  }

  fun setClippingColor(clippingColor: Int) {
    if (cornerPaint.color == clippingColor) {
      return
    }

    if (clippingColor == Color.TRANSPARENT && !areAllDisableClipFalse()) {
      setAlphaAndXferModeForTransparentClipping()
    } else {
      resetCornerPaint()
    }

    cornerPaint.color = clippingColor
    dirty = true
    invalidateSelf()
  }

  private fun setAlphaAndXferModeForTransparentClipping() {
    cornerPaint.alpha = 0xff
    cornerPaint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.CLEAR))
  }

  fun setBackgroundDrawable(backgroundDrawable: Drawable?) {
    this.backgroundDrawable = backgroundDrawable
    invalidateSelf()
  }

  fun setCornerRadius(radius: Float) {
    var newRadius = radius
    newRadius = (newRadius + .5f).toInt().toFloat()
    if (cornerRadius == newRadius) {
      return
    }

    dirty = true
    cornerRadius = newRadius
    Arrays.fill(drawableCornerRadii, newRadius)
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

  fun setDisableClip(edge: Int) {
    if (disableClipCorners == edge) {
      return
    }
    disableClipCorners = edge

    // This condition ensures that the required alpha and xfermode is set in case setClippingColor
    // is called before setDisableClip
    if (cornerPaint.color == Color.TRANSPARENT && !areAllDisableClipFalse()) {
      setAlphaAndXferModeForTransparentClipping()
    }
    dirty = true
    invalidateSelf()
  }

  fun resetCornerPaint() {
    cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
  }

  private fun areAllDisableClipFalse(): Boolean {
    return disableClipCorners == NONE
  }

  companion object {
    private const val CORNER_RADII_LIST_SIZE = 8
    const val NONE: Int = 0
    const val TOP_LEFT: Int = 1 shl 0
    const val TOP_RIGHT: Int = 1 shl 1
    const val BOTTOM_LEFT: Int = 1 shl 2
    const val BOTTOM_RIGHT: Int = 1 shl 3
  }
}
