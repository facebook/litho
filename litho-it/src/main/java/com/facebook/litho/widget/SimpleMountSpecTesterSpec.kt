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

import android.content.Context
import android.graphics.drawable.ColorDrawable
import androidx.annotation.UiThread
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentLayout
import com.facebook.litho.Size
import com.facebook.litho.SizeSpec
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnMeasure
import com.facebook.litho.annotations.OnMount
import com.facebook.litho.annotations.Prop

@MountSpec
internal object SimpleMountSpecTesterSpec {

  @JvmStatic
  @OnMeasure
  fun onMeasure(
      context: ComponentContext,
      layout: ComponentLayout,
      widthSpec: Int,
      heightSpec: Int,
      size: Size,
      @Prop(optional = true) measuredWidth: Int?,
      @Prop(optional = true) measuredHeight: Int?
  ) {
    if (measuredWidth == null && measuredHeight == null) {
      size.width = SizeSpec.getSize(widthSpec)
      size.height = SizeSpec.getSize(heightSpec)
    } else {
      val width = measuredWidth ?: SizeSpec.UNSPECIFIED
      val height = measuredHeight ?: SizeSpec.UNSPECIFIED
      size.width = SizeSpec.resolveSize(widthSpec, width)
      size.height = SizeSpec.resolveSize(heightSpec, height)
    }
  }

  @JvmStatic
  @UiThread
  @OnCreateMountContent
  fun onCreateMountContent(c: Context): ColorDrawable = ColorDrawable()

  @JvmStatic
  @OnMount
  fun onMount(c: ComponentContext, drawable: ColorDrawable, @Prop(optional = true) color: Int) {
    drawable.color = color
  }
}
