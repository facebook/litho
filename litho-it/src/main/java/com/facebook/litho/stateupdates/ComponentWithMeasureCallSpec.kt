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

package com.facebook.litho.stateupdates

import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Size
import com.facebook.litho.SizeSpec
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Prop

@LayoutSpec
object ComponentWithMeasureCallSpec {
  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop component: Component,
      @Prop shouldCacheResult: Boolean,
      @Prop(optional = true) widthSpec: Int,
      @Prop(optional = true) heightSpec: Int
  ): Component {
    component.measure(
        c,
        if (widthSpec != 0) widthSpec else SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
        if (heightSpec != 0) heightSpec else SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
        Size(),
        shouldCacheResult)
    return component
  }
}
