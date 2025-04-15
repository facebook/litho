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
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import androidx.core.graphics.withSave
import kotlin.math.ceil

class CardShadowDrawable : Drawable() {
  private var shadowStartColor = 0
  private var shadowEndColor = 0

  private val edgeShadowPaint: Paint

  private val cornerShadowTopLeftPath = Path()
  private val cornerShadowBottomLeftPath = Path()
  private val cornerShadowTopRightPath = Path()
  private val cornerShadowBottomRightPath = Path()

  private val cornerShadowLeftPaint: Paint
  private val cornerShadowRightPaint: Paint

  private var cornerRadius = 0f
  private var shadowSize = 0f
  private var shadowLeftSizeOverride = UNDEFINED
  private var shadowRightSizeOverride = UNDEFINED
  private var shadowDx = UNDEFINED
  private var shadowDy = UNDEFINED

  private var hideTopShadow = false
  private var hideBottomShadow = false

  private var dirty = true

  init {
    cornerShadowLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    cornerShadowLeftPaint.style = Paint.Style.FILL

    cornerShadowRightPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    cornerShadowRightPaint.style = Paint.Style.FILL

    edgeShadowPaint = Paint(cornerShadowLeftPaint)
    edgeShadowPaint.isAntiAlias = false
  }

  override fun setAlpha(alpha: Int) {
    cornerShadowLeftPaint.alpha = alpha
    cornerShadowRightPaint.alpha = alpha
    edgeShadowPaint.alpha = alpha
  }

  override fun setColorFilter(cf: ColorFilter?) {
    cornerShadowLeftPaint.setColorFilter(cf)
    cornerShadowRightPaint.setColorFilter(cf)
    edgeShadowPaint.setColorFilter(cf)
  }

  override fun getOpacity(): Int {
    return PixelFormat.TRANSLUCENT
  }

  override fun draw(canvas: Canvas) {
    if (dirty) {
      buildShadow()
      dirty = false
    }

    val bounds = bounds

    drawShadowCorners(canvas, bounds)
    drawShadowEdges(canvas, bounds)
  }

  fun setShadowStartColor(shadowStartColor: Int) {
    if (this.shadowStartColor == shadowStartColor) {
      return
    }

    this.shadowStartColor = shadowStartColor

    dirty = true
    invalidateSelf()
  }

  fun setShadowEndColor(shadowEndColor: Int) {
    if (this.shadowEndColor == shadowEndColor) {
      return
    }

    this.shadowEndColor = shadowEndColor

    dirty = true
    invalidateSelf()
  }

  fun setCornerRadius(radius: Float) {
    var newRadius = radius
    newRadius = (newRadius + .5f).toInt().toFloat()
    if (cornerRadius == newRadius) {
      return
    }

    cornerRadius = newRadius

    dirty = true
    invalidateSelf()
  }

  fun setShadowSize(shadowSize: Float) {
    var newShadowSize = shadowSize
    if (newShadowSize < 0) {
      throw IllegalArgumentException("invalid shadow size")
    }

    newShadowSize = toEven(newShadowSize).toFloat()
    if (this.shadowSize == newShadowSize) {
      return
    }

    this.shadowSize = newShadowSize

    dirty = true
    invalidateSelf()
  }

  fun setShadowDx(shadowDx: Float) {
    if (shadowDx == this.shadowDx) {
      return
    }

    this.shadowDx = shadowDx

    dirty = true
    invalidateSelf()
  }

  fun setShadowDy(shadowDy: Float) {
    if (shadowDy == this.shadowDy) {
      return
    }

    this.shadowDy = shadowDy

    dirty = true
    invalidateSelf()
  }

  fun setHideTopShadow(hideTopShadow: Boolean) {
    this.hideTopShadow = hideTopShadow
  }

  fun setHideBottomShadow(hideBottomShadow: Boolean) {
    this.hideBottomShadow = hideBottomShadow
  }

  fun setShadowLeftSizeOverride(shadowLeftSize: Float) {
    shadowLeftSizeOverride = shadowLeftSize
  }

  fun setShadowRightSizeOverride(shadowRightSizeOverride: Float) {
    this.shadowRightSizeOverride = shadowRightSizeOverride
  }

  private fun buildShadow() {
    val shadowLeftSideUnadjusted =
        if (shadowLeftSizeOverride == UNDEFINED) shadowSize else shadowLeftSizeOverride
    val shadowRightSideUnadjusted =
        if (shadowRightSizeOverride == UNDEFINED) shadowSize else shadowRightSizeOverride

    val shadowCornerLeftRadius = shadowLeftSideUnadjusted + cornerRadius
    val shadowCornerRightRadius = shadowRightSideUnadjusted + cornerRadius

    cornerShadowLeftPaint.setShader(
        RadialGradient(
            shadowCornerLeftRadius,
            shadowCornerLeftRadius,
            shadowCornerLeftRadius,
            intArrayOf(shadowStartColor, shadowStartColor, shadowEndColor),
            floatArrayOf(0f, .2f, 1f),
            Shader.TileMode.CLAMP))

    cornerShadowRightPaint.setShader(
        RadialGradient(
            shadowCornerRightRadius,
            shadowCornerRightRadius,
            shadowCornerRightRadius,
            intArrayOf(shadowStartColor, shadowStartColor, shadowEndColor),
            floatArrayOf(0f, .2f, 1f),
            Shader.TileMode.CLAMP))

    val shadowDx = if (shadowDx == UNDEFINED) 0f else shadowDx
    val shadowDy = if (shadowDy == UNDEFINED) getDefaultShadowDy(shadowSize) else shadowDy

    val shadowLeft = getShadowLeft(shadowLeftSideUnadjusted, shadowDx)
    val shadowRight = getShadowRight(shadowRightSideUnadjusted, shadowDx)
    val shadowTop = getShadowTop(shadowSize, shadowDy)
    val shadowBottom = getShadowBottom(shadowSize, shadowDy)

    setPath(cornerShadowTopLeftPath, shadowLeft, shadowTop, cornerRadius)
    setPath(cornerShadowTopRightPath, shadowRight, shadowTop, cornerRadius)
    setPath(cornerShadowBottomLeftPath, shadowLeft, shadowBottom, cornerRadius)
    setPath(cornerShadowBottomRightPath, shadowRight, shadowBottom, cornerRadius)

    // We offset the content (shadowSize / 2) pixels up to make it more realistic.
    // This is why edge shadow shader has some extra space. When drawing bottom edge
    // shadow, we use that extra space.
    edgeShadowPaint.setShader(
        LinearGradient(
            0f,
            shadowCornerLeftRadius,
            0f,
            0f,
            intArrayOf(shadowStartColor, shadowStartColor, shadowEndColor),
            floatArrayOf(0f, .2f, 1f),
            Shader.TileMode.CLAMP))

    edgeShadowPaint.isAntiAlias = false
  }

  private fun drawShadowCorners(canvas: Canvas, bounds: Rect) {
    if (!hideTopShadow) {
      // left-top
      canvas.withSave {
        translate(bounds.left.toFloat(), bounds.top.toFloat())
        drawPath(cornerShadowTopLeftPath, cornerShadowLeftPaint)
      }

      // right-top
      canvas.withSave {
        translate(bounds.right.toFloat(), bounds.top.toFloat())
        scale(-1f, 1f)
        drawPath(cornerShadowTopRightPath, cornerShadowLeftPaint)
      }
    }

    if (!hideBottomShadow) {
      // right-bottom
      canvas.withSave {
        translate(bounds.right.toFloat(), bounds.bottom.toFloat())
        scale(-1f, -1f)
        drawPath(cornerShadowBottomRightPath, cornerShadowRightPaint)
      }

      // left-bottom
      canvas.withSave {
        translate(bounds.left.toFloat(), bounds.bottom.toFloat())
        scale(1f, -1f)
        drawPath(cornerShadowBottomLeftPath, cornerShadowRightPaint)
      }
    }
  }

  private fun drawShadowEdges(canvas: Canvas, bounds: Rect) {
    val shadowDx = if (shadowDx == UNDEFINED) 0f else shadowDx
    val shadowDy = if (shadowDy == UNDEFINED) getDefaultShadowDy(shadowSize) else shadowDy

    val shadowLeftSideNonAdjusted =
        if (shadowLeftSizeOverride == UNDEFINED) shadowSize else shadowLeftSizeOverride
    val shadowRightSideNonAdjusted =
        if (shadowRightSizeOverride == UNDEFINED) shadowSize else shadowRightSizeOverride
    val paddingLeft = getShadowLeft(shadowLeftSideNonAdjusted, shadowDx)
    val paddingRight = getShadowRight(shadowRightSideNonAdjusted, shadowDx)

    val paddingTop = getShadowTop(shadowSize, shadowDy)
    val paddingBottom = getShadowBottom(shadowSize, shadowDy)

    if (!hideTopShadow) {
      // top
      canvas.withSave {
        translate(bounds.left.toFloat(), bounds.top.toFloat())
        drawRect(
            paddingLeft + cornerRadius,
            0f,
            bounds.width() - cornerRadius - paddingRight,
            paddingTop.toFloat(),
            edgeShadowPaint)
      }
    }

    if (!hideBottomShadow) {
      // bottom
      canvas.withSave {
        translate(bounds.right.toFloat(), bounds.bottom.toFloat())
        rotate(180f)
        drawRect(
            paddingRight + cornerRadius,
            0f,
            bounds.width() - cornerRadius - paddingLeft,
            paddingBottom.toFloat(),
            edgeShadowPaint)
      }
    }

    // left
    canvas.withSave {
      translate(bounds.left.toFloat(), bounds.bottom.toFloat())
      rotate(270f)
      drawRect(
          if (hideBottomShadow) 0f else (paddingBottom + cornerRadius),
          0f,
          bounds.height() - (if (hideTopShadow) 0f else cornerRadius + paddingTop),
          paddingLeft.toFloat(),
          edgeShadowPaint)
    }

    // right
    canvas.withSave {
      translate(bounds.right.toFloat(), bounds.top.toFloat())
      rotate(90f)
      drawRect(
          if (hideTopShadow) 0f else (paddingTop + cornerRadius),
          0f,
          bounds.height() - (if (hideBottomShadow) 0f else cornerRadius + paddingBottom),
          paddingRight.toFloat(),
          edgeShadowPaint)
    }
  }

  companion object {
    const val UNDEFINED: Float = -1f

    /**
     * @param shadowSize
     * @param shadowDx Light source offset applied horizontally, i.e. if shadowDx is 0, the shadows
     *   on the left and right sides are both equal to shadowSize. If shadowDx is positive, the
     *   light source moves to the left side, hence left shadow is decreased by shadowDx while right
     *   shadow is increased by shadowDx. If shadowDx is negative, light source moves to the right
     *   side and left and right shadows are adjusted accordingly.
     * @return Shadow applied to the left side of the element.
     */
    @JvmStatic
    fun getShadowLeft(shadowSize: Float, shadowDx: Float): Int {
      return ceil((toEven(shadowSize) - shadowDx).toDouble()).toInt()
    }

    /**
     * @param shadowSize
     * @return Shadow applied to the left side of the element. Shadow offset is 0.
     */
    @JvmStatic
    fun getShadowLeft(shadowSize: Float): Int {
      return getShadowLeft(shadowSize, 0f)
    }

    /**
     * @param shadowSize
     * @param shadowDx Light source offset applied horizontally, i.e. if shadowDx is 0, the shadows
     *   on the left and right sides are both equal to shadowSize. If shadowDx is positive, the
     *   light source moves to the left side, hence left shadow is decreased by shadowDx while right
     *   shadow is increased by shadowDx. If shadowDx is negative, light source moves to the right
     *   side and left and right shadows are adjusted accordingly.
     * @return Shadow applied to the right side of the element.
     */
    @JvmStatic
    fun getShadowRight(shadowSize: Float, shadowDx: Float): Int {
      return ceil((toEven(shadowSize) + shadowDx).toDouble()).toInt()
    }

    /**
     * @param shadowSize
     * @return Shadow applied to the right side of the element. Shadow offset is 0.
     */
    @JvmStatic
    fun getShadowRight(shadowSize: Float): Int {
      return getShadowRight(shadowSize, 0f)
    }

    /**
     * @param shadowSize
     * @param shadowDy Light source offset applied vertically, i.e. if shadowDy is 0, the shadows on
     *   the top and bottom sides are both equal to shadowSize. If shadowDy is positive, the light
     *   source moves to the top side, hence top shadow is decreased by shadowDy while bottom shadow
     *   is increased by shadowDy. If shadowDy is negative, light source moves to the bottom side
     *   and top and bottom shadows are adjusted accordingly.
     * @return Shadow applied to the top side of the element.
     */
    @JvmStatic
    fun getShadowTop(shadowSize: Float, shadowDy: Float): Int {
      return ceil((toEven(shadowSize) - shadowDy).toDouble()).toInt()
    }

    /**
     * @param shadowSize
     * @return Shadow applied to the top side of the element. Shadow offset equals to half of
     *   shadowSize.
     */
    @JvmStatic
    fun getShadowTop(shadowSize: Float): Int {
      val shadowDy = getDefaultShadowDy(shadowSize)
      return getShadowTop(shadowSize, shadowDy)
    }

    private fun getDefaultShadowDy(shadowSize: Float): Float {
      return toEven(shadowSize) * 0.5f
    }

    /**
     * @param shadowSize
     * @param shadowDy Light source offset applied vertically, i.e. if shadowDy is 0, the shadows on
     *   the top and bottom sides are both equal to shadowSize. If shadowDy is positive, the light
     *   source moves to the top side, hence top shadow is decreased by shadowDy while bottom shadow
     *   is increased by shadowDy. If shadowDy is negative, light source moves to the bottom side
     *   and top and bottom shadows are adjusted accordingly.
     * @return Shadow applied to the bottom side of the element.
     */
    @JvmStatic
    fun getShadowBottom(shadowSize: Float, shadowDy: Float): Int {
      return ceil((toEven(shadowSize) + shadowDy).toDouble()).toInt()
    }

    /**
     * @param shadowSize
     * @return Shadow applied to the bottom side of the element. Shadow offset equals to half of
     *   shadowSize.
     */
    @JvmStatic
    fun getShadowBottom(shadowSize: Float): Int {
      val shadowDy = getDefaultShadowDy(shadowSize)
      return getShadowBottom(shadowSize, shadowDy)
    }

    private fun setPath(path: Path, shadowX: Int, shadowY: Int, cornerRadius: Float) {
      val innerBounds =
          RectF(
              shadowX.toFloat(),
              shadowY.toFloat(),
              shadowX + 2 * cornerRadius,
              shadowY + 2 * cornerRadius)

      val outerBounds = RectF(0f, 0f, 2 * cornerRadius, 2 * cornerRadius)

      path.reset()
      path.fillType = Path.FillType.EVEN_ODD
      path.moveTo(shadowX + cornerRadius, shadowY.toFloat())
      path.arcTo(innerBounds, 270f, -90f, true)
      path.rLineTo(-shadowX.toFloat(), 0f)
      path.lineTo(0f, cornerRadius)
      path.arcTo(outerBounds, 180f, 90f, true)
      path.lineTo(shadowX + cornerRadius, 0f)
      path.rLineTo(0f, shadowY.toFloat())
      path.close()
    }

    private fun toEven(value: Float): Int {
      val i = (value + .5f).toInt()
      if (i % 2 == 1) {
        return i - 1
      }

      return i
    }
  }
}
