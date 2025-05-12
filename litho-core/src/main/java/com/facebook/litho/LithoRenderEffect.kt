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

import android.graphics.Rect
import android.graphics.RenderEffect
import android.os.Build
import androidx.annotation.RequiresApi
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate

/**
 * A Litho abstraction of [RenderEffect]. Android [RenderEffect] is a wrapper for native code, which
 * means that the instances are never equal and if we pass such instances through Litho, it may
 * break some of our optimizations which rely on object equivalence.
 *
 * See [RenderEffect]
 */
sealed class LithoRenderEffect {

  /** Creates a [RenderEffect] instance from this [LithoRenderEffect]. */
  abstract fun toRenderEffect(): RenderEffect

  /**
   * A [LithoRenderEffect] instance that will offset the drawing content by the provided x and y
   * offset.
   *
   * @param offsetX offset along the x axis in pixels
   * @param offsetY offset along the y axis in pixels
   * @param input target [LithoRenderEffect] used to render in the offset coordinates.
   *
   * See [RenderEffect.createOffsetEffect]
   */
  @DataClassGenerate
  @RequiresApi(Build.VERSION_CODES.S)
  data class Offset(val offsetX: Float, val offsetY: Float, val input: LithoRenderEffect? = null) :
      LithoRenderEffect() {
    override fun toRenderEffect(): RenderEffect {
      return if (input != null) {
        RenderEffect.createOffsetEffect(offsetX, offsetY, input.toRenderEffect())
      } else {
        RenderEffect.createOffsetEffect(offsetX, offsetY)
      }
    }
  }

  /**
   * A [LithoRenderEffect] that blurs the contents of the optional input RenderEffect with the
   * specified radius along the x and y axis. If no input [LithoRenderEffect] is provided then all
   * drawing commands issued with a [android.graphics.RenderNode] that this [LithoRenderEffect] is
   * installed in will be blurred
   *
   * @param radiusX Radius of blur along the X axis
   * @param radiusY Radius of blur along the Y axis
   * @param edgeTreatment Policy for how to blur content near edges of the blur kernel
   * @param inputEffect Input [LithoRenderEffect] that provides the content to be blurred, can be
   *   null to indicate that the drawing commands on the RenderNode are to be blurred instead of the
   *   input [LithoRenderEffect]
   *
   * See [RenderEffect.createBlurEffect]
   */
  @DataClassGenerate
  @RequiresApi(Build.VERSION_CODES.S)
  data class Blur(
      val radiusX: Float,
      val radiusY: Float,
      val edgeTreatment: android.graphics.Shader.TileMode,
      val inputEffect: LithoRenderEffect? = null
  ) : LithoRenderEffect() {
    override fun toRenderEffect(): RenderEffect {
      return if (inputEffect != null) {
        RenderEffect.createBlurEffect(radiusX, radiusY, inputEffect.toRenderEffect(), edgeTreatment)
      } else {
        RenderEffect.createBlurEffect(radiusX, radiusY, edgeTreatment)
      }
    }
  }

  /**
   * A [LithoRenderEffect] that renders the contents of the input [android.graphics.Bitmap]. This is
   * useful to create an input for other [LithoRenderEffect] types such as [Blur] or [ColorFilter].
   *
   * @param bitmap The source bitmap to be rendered by the created [LithoRenderEffect]
   * @param src Optional subset of the bitmap to be part of the rendered output. If null is
   *   provided, the entire bitmap bounds are used.
   * @param dst Bounds of the destination which the bitmap is translated and scaled to be drawn into
   *   within the bounds of the [android.graphics.RenderNode] this [LithoRenderEffect] is installed
   *   on
   *
   * See [RenderEffect.createBitmapEffect]
   */
  @DataClassGenerate
  @RequiresApi(Build.VERSION_CODES.S)
  data class Bitmap(
      val bitmap: android.graphics.Bitmap,
      val src: Rect? = null,
      val dst: Rect? = null,
  ) : LithoRenderEffect() {
    override fun toRenderEffect(): RenderEffect {
      val destination = dst
      return if (destination != null) {
        RenderEffect.createBitmapEffect(bitmap, src, destination)
      } else {
        RenderEffect.createBitmapEffect(bitmap)
      }
    }
  }

  /**
   * A [LithoRenderEffect] that applies the color filter to the provided [LithoRenderEffect]
   *
   * @param colorFilter ColorFilter applied to the content in the input [LithoRenderEffect]
   * @param renderEffect Source to be transformed by the specified [android.graphics.ColorFilter]
   *
   * See [RenderEffect.createColorFilterEffect]
   */
  @DataClassGenerate
  @RequiresApi(Build.VERSION_CODES.S)
  data class ColorFilter(
      val colorFilter: android.graphics.ColorFilter,
      val renderEffect: LithoRenderEffect? = null
  ) : LithoRenderEffect() {
    override fun toRenderEffect(): RenderEffect {
      val input = renderEffect
      return if (input != null) {
        RenderEffect.createColorFilterEffect(colorFilter, input.toRenderEffect())
      } else {
        RenderEffect.createColorFilterEffect(colorFilter)
      }
    }
  }

  /**
   * A [LithoRenderEffect] that is a composition of 2 other [LithoRenderEffect] instances combined
   * by the specified [android.graphics.BlendMode]
   *
   * @param dst The Dst pixels used in blending
   * @param src The Src pixels used in blending
   * @param blendMode The [android.graphics.BlendMode] to be used to combine colors from the two
   *   [LithoRenderEffect]s
   *
   * See [RenderEffect.createBlendModeEffect]
   */
  @DataClassGenerate
  @RequiresApi(Build.VERSION_CODES.S)
  data class BlendMode(
      val dst: LithoRenderEffect,
      val src: LithoRenderEffect,
      val blendMode: android.graphics.BlendMode,
  ) : LithoRenderEffect() {
    override fun toRenderEffect(): RenderEffect {
      return RenderEffect.createBlendModeEffect(
          src.toRenderEffect(), dst.toRenderEffect(), blendMode)
    }
  }

  /**
   * A [LithoRenderEffect] that composes `inner` with `outer`, such that the results of `inner` are
   * treated as the source bitmap passed to `outer`, i.e.
   *
   * ```
   * result = outer(inner(source))
   * ```
   *
   * Consumers should favor explicit chaining of [LithoRenderEffect] instances at creation time
   * rather than using chain effect. Chain effects are useful for situations where the input or
   * output are provided from elsewhere and the input or output [LithoRenderEffect] need to be
   * changed.
   *
   * @param outer [LithoRenderEffect] that consumes the output of `inner` as its input
   * @param inner [LithoRenderEffect] that is consumed as input by `outer`
   *
   * See [RenderEffect.createChainEffect]
   */
  @DataClassGenerate
  @RequiresApi(Build.VERSION_CODES.S)
  data class Chain(val outer: LithoRenderEffect, val inner: LithoRenderEffect) :
      LithoRenderEffect() {
    override fun toRenderEffect(): RenderEffect {
      return RenderEffect.createChainEffect(outer.toRenderEffect(), inner.toRenderEffect())
    }
  }

  /**
   * A [LithoRenderEffect] that renders the contents of the input [ android.graphics.Shader]. This
   * is useful to create an input for other [LithoRenderEffect] types such as [Blur] or
   * [ColorFilter].
   *
   * See [RenderEffect.createShaderEffect]
   */
  @DataClassGenerate
  @RequiresApi(Build.VERSION_CODES.S)
  data class Shader(val shader: android.graphics.Shader) : LithoRenderEffect() {
    override fun toRenderEffect(): RenderEffect {
      return RenderEffect.createShaderEffect(shader)
    }
  }

  /**
   * A [LithoRenderEffect] that executes the provided [android.graphics.RuntimeShader] and passes
   * the contents of the [android.graphics.RenderNode] that this [LithoRenderEffect] is installed on
   * as an input to the shader.
   *
   * @param shader the runtime shader that will bind the inputShaderName to the [LithoRenderEffect]
   *   input
   * @param uniformShaderName the uniform name defined in the RuntimeShader's program to which the
   *   contents of the RenderNode will be bound
   *
   * See [RenderEffect.createRuntimeShaderEffect]
   */
  @DataClassGenerate
  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  data class RuntimeShader(
      val shader: android.graphics.RuntimeShader,
      val uniformShaderName: String,
  ) : LithoRenderEffect() {
    override fun toRenderEffect(): RenderEffect {
      return RenderEffect.createRuntimeShaderEffect(shader, uniformShaderName)
    }
  }
}
