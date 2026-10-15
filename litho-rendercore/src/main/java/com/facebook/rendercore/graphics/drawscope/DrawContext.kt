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

package com.facebook.rendercore.graphics.drawscope

import android.graphics.ColorFilter
import android.graphics.MaskFilter
import android.graphics.Paint
import android.graphics.Paint.Cap
import android.graphics.PathEffect
import android.graphics.Shader
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.ResourceResolver
import com.facebook.rendercore.graphics.Color
import com.facebook.rendercore.px

class DrawContext(val resourceResolver: ResourceResolver) {

  private var fillPaint: Paint? = null
  private var strokePaint: Paint? = null

  private fun getOrCreateStrokePaint(width: Dimen): Paint {
    val paint =
        strokePaint
            ?: Paint().apply {
              style = Paint.Style.STROKE
              strokePaint = this
            }
    paint.strokeWidth = width.toFloatPixels(resourceResolver)
    return paint
  }

  private fun getOrCreateFillPaint(): Paint =
      fillPaint ?: Paint().apply { style = Paint.Style.FILL }.also { fillPaint = it }

  fun configurePaint(
      style: DrawStyle,
      color: Color,
      shader: Shader?,
      pathEffect: PathEffect? = null,
      maskFilter: MaskFilter? = null,
      colorFilter: ColorFilter? = null,
      strokeCap: Cap? = Cap.BUTT,
  ): Paint =
      when (style) {
        is Fill -> getOrCreateFillPaint()
        is Stroke -> getOrCreateStrokePaint(style.width)
      }.apply {
        val targetColor = color.toArgb()
        if (this.color != targetColor) this.color = targetColor
        if (this.shader != shader) this.shader = shader
        if (this.pathEffect != pathEffect) this.pathEffect = pathEffect
        if (this.maskFilter != maskFilter) this.maskFilter = maskFilter
        if (this.strokeCap != strokeCap) this.strokeCap = strokeCap
        if (this.colorFilter != colorFilter) this.colorFilter = colorFilter
      }

  fun configureTextPaint(
      style: DrawStyle,
      color: Color,
      shader: Shader?,
      pathEffect: PathEffect? = null,
      maskFilter: MaskFilter? = null,
      colorFilter: ColorFilter? = null,
      textAlign: Paint.Align = Paint.Align.LEFT,
      textScaleX: Float = 1.0f,
      textSize: Float? = null,
      textSkewX: Float = 0f,
      strokeCap: Cap? = Cap.BUTT,
  ): Paint {
    return configurePaint(style, color, shader, pathEffect, maskFilter, colorFilter, strokeCap)
        .apply {
          if (textSize != null) {
            this.textSize = textSize
          }
          if (this.textAlign != textAlign) {
            this.textAlign = textAlign
          }
          if (this.textScaleX != textScaleX) {
            this.textScaleX = textScaleX
          }
          if (this.textSkewX != textSkewX) {
            this.textSkewX = textSkewX
          }
        }
  }
}

/** Represents a style used for drawing shapes.* */
sealed class DrawStyle

/**
 * Default [DrawStyle] indicating shapes should be drawn completely filled in with the provided
 * color or pattern.
 */
object Fill : DrawStyle()

/**
 * [DrawStyle] indicating shapes should be drawn only with an outline (stroke) using the provided
 * color or pattern.
 */
class Stroke(
    val width: Dimen = 0.0.px,
) : DrawStyle()
