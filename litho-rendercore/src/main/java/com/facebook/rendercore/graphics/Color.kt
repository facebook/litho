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

package com.facebook.rendercore.graphics

import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import com.facebook.rendercore.ResourceResolver

@JvmInline
value class Color(val value: ULong) {

  val red: Int
    get() = ((value shr 16) and 0xFFUL).toInt()

  val green: Int
    get() = ((value shr 8) and 0xFFUL).toInt()

  val blue: Int
    get() = (value and 0xFFUL).toInt()

  val alpha: Float
    get() = ((value shr 24) and 0xFFUL).toFloat() / 255f

  fun copy(@FloatRange(from = 0.0, to = 1.0) alpha: Float = this.alpha): Color =
      fromRgba(red = red, green = green, blue = blue, alpha = alpha)

  fun copy(
      @FloatRange(from = 0.0, to = 1.0) alpha: Float = this.alpha,
      @IntRange(from = 0, to = 255) red: Int = this.red,
      @IntRange(from = 0, to = 255) green: Int = this.green,
      @IntRange(from = 0, to = 255) blue: Int = this.blue,
  ): Color = fromRgba(red = red, green = green, blue = blue, alpha = alpha)

  fun toArgb(): Int = value.toInt()

  companion object {
    fun fromRgba(
        @IntRange(from = 0, to = 255) red: Int,
        @IntRange(from = 0, to = 255) green: Int,
        @IntRange(from = 0, to = 255) blue: Int,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
    ): Color {
      val a = (alpha * 255.0f + 0.5f).toInt().coerceIn(0, 255)
      val r = red.coerceIn(0, 255)
      val g = green.coerceIn(0, 255)
      val b = blue.coerceIn(0, 255)
      return Color(
          ((a.toULong() shl 24) or (r.toULong() shl 16) or (g.toULong() shl 8) or b.toULong()),
      )
    }

    fun fromInt(color: Int): Color = Color(color.toULong() and 0xFFFFFFFFUL)

    fun fromRes(resourceResolver: ResourceResolver, @ColorRes resId: Int): Color {
      return fromInt(resourceResolver.resolveColorRes(resId))
    }

    fun fromAttr(
        resourceResolver: ResourceResolver,
        @AttrRes attrResId: Int,
        @ColorRes defResId: Int = 0,
    ): Color {
      return fromInt(resourceResolver.resolveColorAttr(attrResId, defResId))
    }
  }
}
