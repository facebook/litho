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

inline class Px(val px: Int) {
  inline fun toPx(scope: DslScope): Px = this
}

inline class Dp(val dp: Float) {
  inline fun toPx(scope: DslScope): Px =
      Px(scope.resourceResolver.dipsToPixels(dp))
}

inline class Sp(val sp: Float) {
  inline fun toPx(scope: DslScope): Px =
      Px(scope.resourceResolver.sipsToPixels(sp))
}

inline val Int.dp: Dp get() = Dp(this.toFloat())
inline val Float.dp: Dp get() = Dp(this)
inline val Double.dp: Dp get() = Dp(this.toFloat())

inline val Int.sp: Sp get() = Sp(this.toFloat())
inline val Float.sp: Sp get() = Sp(this)
inline val Double.sp: Sp get() = Sp(this.toFloat())

inline val Int.px: Px get() = Px(this)
inline val Float.px: Px get() = Px(this.toInt())
inline val Double.px: Px get() = Px(this.toInt())
