/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

/**
 * Interface for dimension values in different units ( [Px], [Dp], [Sp]). All can be converted to
 * pixels with a [ResourceResolver] which wraps a [Resources].
 */
interface Dimen {
  fun toPixels(resourceResolver: ResourceResolver): Int

  companion object {
    /**
     * Used to represent dimension of an element that should be drawn with 1px size (typically,
     * dividers). Is respected only for sizing and not spacing.
     */
    val Hairline = Dp(0f)
  }
}

/** Unit-type for pixels. */
inline class Px(val value: Int) : Dimen {
  override fun toPixels(resourceResolver: ResourceResolver) = value
  inline operator fun plus(other: Px) = Px(value = this.value + other.value)
}

/** Unit-type for dips. */
inline class Dp(val value: Float) : Dimen {
  override fun toPixels(resourceResolver: ResourceResolver) = resourceResolver.dipsToPixels(value)
  inline operator fun plus(other: Dp) = Dp(value = this.value + other.value)
}

/** Unit-type for sips. */
inline class Sp(val value: Float) : Dimen {
  override fun toPixels(resourceResolver: ResourceResolver) = resourceResolver.sipsToPixels(value)
  inline operator fun plus(other: Sp) = Sp(value = this.value + other.value)
}

inline val Int.dp: Dp
  get() = Dp(this.toFloat())

inline val Float.dp: Dp
  get() = Dp(this)

inline val Double.dp: Dp
  get() = Dp(this.toFloat())

inline val Int.sp: Sp
  get() = Sp(this.toFloat())

inline val Float.sp: Sp
  get() = Sp(this)

inline val Double.sp: Sp
  get() = Sp(this.toFloat())

inline val Int.px: Px
  get() = Px(this)

inline val Float.px: Px
  get() = Px(this.toInt())

inline val Double.px: Px
  get() = Px(this.toInt())
