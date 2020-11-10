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

inline class Px(val value: Int)

inline class Dp(val value: Float) {

  inline operator fun plus(other: Dp) = Dp(value = this.value + other.value)

  companion object {
    /**
     * Used to represent dimension of an element that should be drawn with 1px size (typically,
     * dividers). Is respected only for sizing and not spacing.
     */
    val Hairline = Dp(0f)
  }
}

inline class Sp(val value: Float)

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
