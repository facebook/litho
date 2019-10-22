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
  inline fun toPx(c: ComponentContext): Px = this
}

inline class Dp(val dp: Int) {
  inline fun toPx(c: ComponentContext): Px =
      Px(c.resourceResolver.dipsToPixels(dp.toFloat()))
}

inline class Sp(val sp: Int) {
  inline fun toPx(c: ComponentContext): Px =
      Px(c.resourceResolver.sipsToPixels(sp.toFloat()))
}

inline val Int.dp: Dp get() = Dp(this)

inline val Int.sp: Sp get() = Sp(this)

inline val Int.px: Px get() = Px(this)
