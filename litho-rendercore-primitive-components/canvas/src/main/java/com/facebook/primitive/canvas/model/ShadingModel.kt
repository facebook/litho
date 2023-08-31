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

package com.facebook.primitive.canvas.model

import android.graphics.LinearGradient
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.annotation.ColorInt
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.primitive.utils.types.Point

sealed interface CanvasShadingModel

/**
 * A definition for a solid color shading.
 *
 * @property color The color (including alpha)
 */
@DataClassGenerate
data class CanvasSolidColorShading(@ColorInt val color: Int) : CanvasShadingModel

/**
 * A definition for a gradient shading.
 *
 * @property gradient The gradient definition
 */
@DataClassGenerate
data class CanvasGradientShading(val gradient: CanvasGradientModel) : CanvasShadingModel

sealed interface CanvasGradientModel {
  fun toShader(): Shader
}

/**
 * A definition for a linear gradient that varies along the line defined by the provided starting
 * and ending points.
 *
 * @property gradient The colors and positions of the gradient
 * @property startPoint The start of the gradient line
 * @property endPoint The end of the gradient line
 * @property tileMode The tiling mode
 */
@DataClassGenerate
data class CanvasLinearGradient(
    val gradient: CanvasGradient,
    val startPoint: Point,
    val endPoint: Point,
    val tileMode: Shader.TileMode
) : CanvasGradientModel {
  override fun toShader(): Shader {
    return LinearGradient(
        startPoint.x,
        startPoint.y,
        endPoint.x,
        endPoint.y,
        gradient.colors,
        gradient.positions,
        tileMode)
  }
}

/**
 * A definition for a radial gradient given the center and radius.
 *
 * @property gradient The colors and positions of the gradient
 * @property center The center of the radius
 * @property radius Must be positive. The radius of the circle for this gradient
 * @property tileMode The tiling mode
 */
@DataClassGenerate
data class CanvasRadialGradient(
    val gradient: CanvasGradient,
    val center: Point,
    val radius: Float,
    val tileMode: Shader.TileMode
) : CanvasGradientModel {
  override fun toShader(): Shader {
    return RadialGradient(center.x, center.y, radius, gradient.colors, gradient.positions, tileMode)
  }
}

/**
 * A definition for a smooth transition between colors for drawing gradients.
 *
 * @property colors The sRGB colors (including alpha) to be distributed along the gradient
 * @property positions The relative positions [0..1] of each corresponding color in the colors
 *   array. If this is null, the the colors are distributed evenly along the gradient
 */
@DataClassGenerate
data class CanvasGradient(@ColorInt val colors: IntArray, val positions: FloatArray?) {
  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (javaClass != other?.javaClass) {
      return false
    }

    other as CanvasGradient

    if (!colors.contentEquals(other.colors)) {
      return false
    }
    if (positions != null) {
      if (other.positions == null) {
        return false
      }
      if (!positions.contentEquals(other.positions)) {
        return false
      }
    } else if (other.positions != null) {
      return false
    }

    return true
  }

  override fun hashCode(): Int {
    var result = colors.contentHashCode()
    result = 31 * result + (positions?.contentHashCode() ?: 0)
    return result
  }
}
