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

package com.facebook.litho.widget.canvas

import android.graphics.Shader
import androidx.annotation.ColorInt
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.primitive.canvas.model.CanvasGradient
import com.facebook.primitive.canvas.model.CanvasGradientShading
import com.facebook.primitive.canvas.model.CanvasLinearGradient
import com.facebook.primitive.canvas.model.CanvasRadialGradient
import com.facebook.primitive.canvas.model.CanvasShadingModel
import com.facebook.primitive.canvas.model.CanvasSolidColorShading
import com.facebook.primitive.utils.types.Point

@JvmInline
value class Shading
private constructor(@PublishedApi internal val shadingModel: CanvasShadingModel) {
  companion object {
    /**
     * Solid color shading.
     *
     * @param color The color (including alpha)
     */
    fun solidColor(@ColorInt color: Int): Shading {
      return Shading(CanvasSolidColorShading(color))
    }

    /**
     * A linear gradient that varies along the line defined by the provided starting and ending
     * points.
     *
     * @param gradient The colors and positions of the gradient
     * @param startPoint The start of the gradient line
     * @param endPoint The end of the gradient line
     * @param tileMode The tiling mode
     */
    fun linearGradient(
        gradient: Gradient,
        startPoint: Point,
        endPoint: Point,
        tileMode: Shader.TileMode = Shader.TileMode.CLAMP
    ): Shading {
      return Shading(
          CanvasGradientShading(
              CanvasLinearGradient(gradient.canvasGradient, startPoint, endPoint, tileMode)))
    }

    /**
     * A radial gradient given the center and radius.
     *
     * @param gradient The colors and positions of the gradient
     * @param center The center of the radius
     * @param radius Must be positive. The radius of the circle for this gradient
     * @param tileMode The tiling mode
     */
    fun radialGradient(
        gradient: Gradient,
        center: Point,
        radius: Float,
        tileMode: Shader.TileMode = Shader.TileMode.CLAMP
    ): Shading {
      return Shading(
          CanvasGradientShading(
              CanvasRadialGradient(gradient.canvasGradient, center, radius, tileMode)))
    }
  }
}

@JvmInline
value class Gradient
internal constructor(@PublishedApi internal val canvasGradient: CanvasGradient)

/**
 * A smooth transition between colors for drawing gradients.
 *
 * @param colors The sRGB colors (including alpha) to be evenly distributed along the gradient
 */
fun Gradient(@ColorInt vararg colors: Int): Gradient {
  return Gradient(CanvasGradient(colors, null))
}

/**
 * A smooth transition between colors for drawing gradients.
 *
 * @param colorsAndPositions The colors and positions that should be used to create a gradient.
 */
fun Gradient(vararg colorsAndPositions: GradientColorAndPosition): Gradient {
  val colors = colorsAndPositions.map { it.color }.toIntArray()
  val positions = colorsAndPositions.map { it.position }.toFloatArray()
  return Gradient(CanvasGradient(colors, positions))
}

/**
 * A definition for a position and its corresponding color in a gradient.
 *
 * @property color The sRGB colors (including alpha) to be distributed along the gradient
 * @property position The relative positions [0..1] of each corresponding color in the colors array.
 *   If this is null, the the colors are distributed evenly along the gradient
 */
@DataClassGenerate
data class GradientColorAndPosition(@ColorInt val color: Int, val position: Float)

infix fun @receiver:ColorInt Int.at(position: Float): GradientColorAndPosition {
  return GradientColorAndPosition(this, position)
}
