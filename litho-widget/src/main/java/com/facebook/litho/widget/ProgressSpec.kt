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

package com.facebook.litho.widget

import android.graphics.Color
import android.graphics.drawable.Drawable
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.ExcuseMySpec
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.PropDefault
import com.facebook.litho.annotations.Reason
import com.facebook.litho.annotations.ResType

/**
 * Renders an infinitely spinning progress bar.
 *
 * @uidocs
 * @prop indeterminateDrawable Drawable to be shown to show progress.
 * @prop color Tint color for the drawable.
 */
@ExcuseMySpec(reason = Reason.J2K_CONVERSION)
@LayoutSpec
object ProgressSpec {

  @PropDefault val color: Int = Color.TRANSPARENT

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop(optional = true, resType = ResType.COLOR) color: Int,
      @Prop(optional = true, resType = ResType.DRAWABLE) indeterminateDrawable: Drawable?,
  ): Component {
    return ProgressPrimitiveComponent(
        color = color,
        indeterminateDrawable = indeterminateDrawable,
    )
  }
}
