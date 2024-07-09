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

package com.facebook.litho.drawable

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathEffect
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.Px
import com.facebook.rendercore.utils.equals
import java.util.Arrays
import kotlin.math.max
import kotlin.math.min

/** Drawable that draws border lines with given color, widths and path effect. */
class BorderColorDrawable private constructor(private val state: State) :
    Drawable(), ComparableDrawable {
  private val paint: Paint = Paint()
  private val path: Path = Path()
  private val clipPath: Path = Path()
  private var drawBorderWithPath = false

  init {
    var hasRadius = false
    var lastRadius = 0f
    state.borderRadius?.let { borderRadius ->
      for (i in 0 until borderRadius.size) {
        val radius = borderRadius[i]
        if (radius > 0f) {
          hasRadius = true
        }
        if (i == 0) {
          lastRadius = radius
        } else if (lastRadius != radius) {
          drawBorderWithPath = true
          break
        }
      }

      if (drawBorderWithPath && borderRadius.size != 8) {
        // Need to duplicate values because Android expects X / Y radii specified separately
        val radii = FloatArray(8)
        for (i in 0..3) {
          radii[i * 2] = borderRadius[i]
          radii[i * 2 + 1] = borderRadius[i]
        }
        state.borderRadius = radii
      }
    }

    paint.setPathEffect(state.pathEffect)
    paint.isAntiAlias = state.pathEffect != null || hasRadius
    paint.style = Paint.Style.STROKE
  }

  override fun draw(canvas: Canvas) {
    val equalBorderColors =
        state.borderLeftColor == state.borderTopColor &&
            state.borderTopColor == state.borderRightColor &&
            state.borderRightColor == state.borderBottomColor
    val equalBorderWidths =
        state.borderLeftWidth == state.borderTopWidth &&
            state.borderTopWidth == state.borderRightWidth &&
            state.borderRightWidth == state.borderBottomWidth
    if (equalBorderWidths && state.borderLeftWidth == 0f) {
      // No border widths, nothing to draw
      return
    }
    if (equalBorderWidths && equalBorderColors) {
      drawAllBorders(canvas, state.borderLeftWidth, state.borderLeftColor)
    } else if (equalBorderWidths) {
      drawMultiColoredBorders(canvas)
    } else {
      drawIndividualBorders(canvas)
    }
  }

  /** Best case possible, all colors are the same and all widths are the same */
  private fun drawAllBorders(canvas: Canvas, strokeWidth: Float, @ColorInt color: Int) {
    val inset = strokeWidth / 2f
    DRAW_BOUNDS.set(bounds)
    DRAW_BOUNDS.inset(inset, inset)
    paint.strokeWidth = strokeWidth
    paint.color = color
    drawBorder(canvas, DRAW_BOUNDS, path(), state.borderRadius, paint)
  }

  /** Special, special case, support for multi color with same widths for API 28 */
  private fun drawMultiColoredBorders(canvas: Canvas) {
    paint.strokeWidth = state.borderLeftWidth
    val inset = state.borderLeftWidth / 2f
    DRAW_BOUNDS.set(bounds)
    val translateSaveCount = canvas.save()
    canvas.translate(DRAW_BOUNDS.left, DRAW_BOUNDS.top)
    DRAW_BOUNDS.offsetTo(0.0f, 0.0f)
    DRAW_BOUNDS.inset(inset, inset)
    INNER_DRAW_BOUNDS.set(DRAW_BOUNDS)
    val third = min(DRAW_BOUNDS.width(), DRAW_BOUNDS.height()) / 3f
    INNER_DRAW_BOUNDS.inset(third, third)
    var saveCount: Int

    // Left
    var color = state.borderLeftColor
    if (color != QUICK_REJECT_COLOR) {
      saveCount = canvas.save()
      paint.color = color
      clipPath.reset()
      clipPath.moveTo(DRAW_BOUNDS.left - inset, DRAW_BOUNDS.top - inset)
      clipPath.lineTo(INNER_DRAW_BOUNDS.left, INNER_DRAW_BOUNDS.top)
      clipPath.lineTo(INNER_DRAW_BOUNDS.left, INNER_DRAW_BOUNDS.bottom)
      clipPath.lineTo(DRAW_BOUNDS.left - inset, DRAW_BOUNDS.bottom + inset)
      clipPath.close()
      canvas.clipPath(clipPath)
      drawBorder(canvas, DRAW_BOUNDS, path(), state.borderRadius, paint)
      canvas.restoreToCount(saveCount)
    }

    // Top
    color = state.borderTopColor
    if (color != QUICK_REJECT_COLOR) {
      saveCount = canvas.save()
      paint.color = color
      clipPath.reset()
      clipPath.moveTo(DRAW_BOUNDS.left - inset, DRAW_BOUNDS.top - inset)
      clipPath.lineTo(INNER_DRAW_BOUNDS.left, INNER_DRAW_BOUNDS.top)
      clipPath.lineTo(INNER_DRAW_BOUNDS.right, INNER_DRAW_BOUNDS.top)
      clipPath.lineTo(DRAW_BOUNDS.right + inset, DRAW_BOUNDS.top - inset)
      clipPath.close()
      canvas.clipPath(clipPath)
      drawBorder(canvas, DRAW_BOUNDS, path(), state.borderRadius, paint)
      canvas.restoreToCount(saveCount)
    }

    // Right
    color = state.borderRightColor
    if (color != QUICK_REJECT_COLOR) {
      saveCount = canvas.save()
      paint.color = color
      clipPath.reset()
      clipPath.moveTo(DRAW_BOUNDS.right + inset, DRAW_BOUNDS.top - inset)
      clipPath.lineTo(INNER_DRAW_BOUNDS.right, INNER_DRAW_BOUNDS.top)
      clipPath.lineTo(INNER_DRAW_BOUNDS.right, INNER_DRAW_BOUNDS.bottom)
      clipPath.lineTo(DRAW_BOUNDS.right + inset, DRAW_BOUNDS.bottom + inset)
      clipPath.close()
      canvas.clipPath(clipPath)
      drawBorder(canvas, DRAW_BOUNDS, path(), state.borderRadius, paint)
      canvas.restoreToCount(saveCount)
    }

    // Bottom
    color = state.borderBottomColor
    if (color != QUICK_REJECT_COLOR) {
      saveCount = canvas.save()
      paint.color = color
      clipPath.reset()
      clipPath.moveTo(DRAW_BOUNDS.left - inset, DRAW_BOUNDS.bottom + inset)
      clipPath.lineTo(INNER_DRAW_BOUNDS.left, INNER_DRAW_BOUNDS.bottom)
      clipPath.lineTo(INNER_DRAW_BOUNDS.right, INNER_DRAW_BOUNDS.bottom)
      clipPath.lineTo(DRAW_BOUNDS.right + inset, DRAW_BOUNDS.bottom + inset)
      clipPath.close()
      canvas.clipPath(clipPath)
      drawBorder(canvas, DRAW_BOUNDS, path(), state.borderRadius, paint)
      canvas.restoreToCount(saveCount)
    }
    canvas.restoreToCount(translateSaveCount)
  }

  /** Worst case, we have different widths _and_ colors specified */
  private fun drawIndividualBorders(canvas: Canvas) {
    val bounds = bounds
    // Draw left border.
    if (state.borderLeftWidth > 0 && state.borderLeftColor != QUICK_REJECT_COLOR) {
      drawBorder(
          canvas,
          state.borderLeftColor,
          state.borderLeftWidth,
          bounds.left.toFloat(),
          bounds.top.toFloat(),
          min(bounds.left + state.borderLeftWidth, bounds.right.toFloat()),
          bounds.bottom.toFloat(),
          true)
    }

    // Draw right border.
    if (state.borderRightWidth > 0 && state.borderRightColor != QUICK_REJECT_COLOR) {
      drawBorder(
          canvas,
          state.borderRightColor,
          state.borderRightWidth,
          max(bounds.right - state.borderRightWidth, bounds.left.toFloat()),
          bounds.top.toFloat(),
          bounds.right.toFloat(),
          bounds.bottom.toFloat(),
          true)
    }

    // Draw top border.
    if (state.borderTopWidth > 0 && state.borderTopColor != QUICK_REJECT_COLOR) {
      drawBorder(
          canvas,
          state.borderTopColor,
          state.borderTopWidth,
          bounds.left.toFloat(),
          bounds.top.toFloat(),
          bounds.right.toFloat(),
          min(bounds.top + state.borderTopWidth, bounds.bottom.toFloat()),
          false)
    }

    // Draw bottom border.
    if (state.borderBottomWidth > 0 && state.borderBottomColor != QUICK_REJECT_COLOR) {
      drawBorder(
          canvas,
          state.borderBottomColor,
          state.borderBottomWidth,
          bounds.left.toFloat(),
          max(bounds.bottom - state.borderBottomWidth, bounds.top.toFloat()),
          bounds.right.toFloat(),
          bounds.bottom.toFloat(),
          false)
    }
  }

  private fun drawBorder(
      canvas: Canvas,
      @ColorInt color: Int,
      strokeWidth: Float,
      left: Float,
      top: Float,
      right: Float,
      bottom: Float,
      insetHorizontal: Boolean
  ) {
    paint.strokeWidth = strokeWidth
    paint.color = color
    CLIP_BOUNDS[left, top, right] = bottom
    DRAW_BOUNDS.set(bounds)
    if (insetHorizontal) {
      DRAW_BOUNDS.inset(CLIP_BOUNDS.centerX() - CLIP_BOUNDS.left, 0f)
    } else {
      DRAW_BOUNDS.inset(0f, CLIP_BOUNDS.centerY() - CLIP_BOUNDS.top)
    }
    val saveCount = canvas.save()
    canvas.clipRect(CLIP_BOUNDS)
    drawBorder(canvas, DRAW_BOUNDS, path(), state.borderRadius, paint)
    canvas.restoreToCount(saveCount)
  }

  private fun path(): Path? {
    return if (drawBorderWithPath) path else null
  }

  override fun setAlpha(alpha: Int) {
    paint.alpha = alpha
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    paint.setColorFilter(colorFilter)
  }

  override fun getColorFilter(): ColorFilter? {
    return paint.colorFilter
  }

  @Deprecated("This method is no longer used in graphics optimizations")
  override fun getOpacity(): Int {
    return PixelFormat.OPAQUE
  }

  override fun isEquivalentTo(other: ComparableDrawable): Boolean {
    return equals(other)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is BorderColorDrawable) {
      return false
    }
    return equals(state, other.state)
  }

  override fun hashCode(): Int {
    return state.hashCode()
  }

  internal class State {
    var borderLeftWidth: Float = 0f
    var borderTopWidth: Float = 0f
    var borderRightWidth: Float = 0f
    var borderBottomWidth: Float = 0f
    @ColorInt var borderLeftColor: Int = 0
    @ColorInt var borderTopColor: Int = 0
    @ColorInt var borderRightColor: Int = 0
    @ColorInt var borderBottomColor: Int = 0
    var pathEffect: PathEffect? = null
    var borderRadius: FloatArray? = null

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as State

      if (borderLeftWidth != other.borderLeftWidth) return false
      if (borderTopWidth != other.borderTopWidth) return false
      if (borderRightWidth != other.borderRightWidth) return false
      if (borderBottomWidth != other.borderBottomWidth) return false
      if (borderLeftColor != other.borderLeftColor) return false
      if (borderTopColor != other.borderTopColor) return false
      if (borderRightColor != other.borderRightColor) return false
      if (borderBottomColor != other.borderBottomColor) return false
      if (!equals(pathEffect, other.pathEffect)) return false
      if (Arrays.equals(borderRadius, other.borderRadius)) return false

      return true
    }

    override fun hashCode(): Int {
      var result = 0
      result = 31 * result + borderLeftWidth.toInt()
      result = 31 * result + borderTopWidth.toInt()
      result = 31 * result + borderRightWidth.toInt()
      result = 31 * result + borderBottomWidth.toInt()
      result = 31 * result + borderLeftColor
      result = 31 * result + borderTopColor
      result = 31 * result + borderRightColor
      result = 31 * result + borderBottomColor
      result = 31 * result + (pathEffect?.hashCode() ?: 0)
      result = 31 * result + (borderRadius?.contentHashCode() ?: 0)
      return result
    }
  }

  class Builder {
    private val state: State = State()

    fun pathEffect(pathEffect: PathEffect?): Builder {
      state.pathEffect = pathEffect
      return this
    }

    fun borderColor(@ColorInt color: Int): Builder {
      state.borderLeftColor = color
      state.borderTopColor = color
      state.borderRightColor = color
      state.borderBottomColor = color
      return this
    }

    fun borderLeftColor(@ColorInt color: Int): Builder {
      state.borderLeftColor = color
      return this
    }

    fun borderTopColor(@ColorInt color: Int): Builder {
      state.borderTopColor = color
      return this
    }

    fun borderRightColor(@ColorInt color: Int): Builder {
      state.borderRightColor = color
      return this
    }

    fun borderBottomColor(@ColorInt color: Int): Builder {
      state.borderBottomColor = color
      return this
    }

    fun borderWidth(@Px border: Int): Builder {
      state.borderLeftWidth = border.toFloat()
      state.borderTopWidth = border.toFloat()
      state.borderRightWidth = border.toFloat()
      state.borderBottomWidth = border.toFloat()
      return this
    }

    fun borderLeftWidth(@Px borderLeft: Int): Builder {
      state.borderLeftWidth = borderLeft.toFloat()
      return this
    }

    fun borderTopWidth(@Px borderTop: Int): Builder {
      state.borderTopWidth = borderTop.toFloat()
      return this
    }

    fun borderRightWidth(@Px borderRight: Int): Builder {
      state.borderRightWidth = borderRight.toFloat()
      return this
    }

    fun borderBottomWidth(@Px borderBottom: Int): Builder {
      state.borderBottomWidth = borderBottom.toFloat()
      return this
    }

    fun borderRadius(radius: FloatArray): Builder {
      state.borderRadius = radius.copyOf(radius.size)
      return this
    }

    fun build(): BorderColorDrawable {
      return BorderColorDrawable(state)
    }
  }

  companion object {
    private const val QUICK_REJECT_COLOR: Int = Color.TRANSPARENT
    private val CLIP_BOUNDS: RectF = RectF()
    private val DRAW_BOUNDS: RectF = RectF()
    private val INNER_DRAW_BOUNDS: RectF = RectF()

    private fun drawBorder(
        canvas: Canvas,
        bounds: RectF,
        path: Path?,
        radii: FloatArray?,
        paint: Paint
    ) {
      if (radii == null) {
        return
      }
      val maxRadii = min(bounds.width(), bounds.height()) / 2f
      if (path == null) {
        // All radii are the same
        val radius = min(maxRadii, radii[0])
        canvas.drawRoundRect(bounds, radius, radius, paint)
      } else {
        if (path.isEmpty) {
          path.addRoundRect(bounds, radii, Path.Direction.CW)
        }
        canvas.drawPath(path, paint)
      }
    }
  }
}
