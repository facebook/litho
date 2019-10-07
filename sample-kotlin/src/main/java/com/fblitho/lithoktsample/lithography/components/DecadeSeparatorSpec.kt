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

package com.fblitho.lithoktsample.lithography.components

import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Row
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Prop
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaAlign.CENTER
import com.facebook.yoga.YogaEdge.ALL
import com.facebook.yoga.YogaEdge.HORIZONTAL
import com.fblitho.lithoktsample.lithography.data.Decade

@LayoutSpec
object DecadeSeparatorSpec {

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop decade: Decade): Component =
      Row.create(c)
          .alignItems(CENTER)
          .paddingDip(ALL, 16f)
          .child(
              Row.create(c)
                  .heightPx(1)
                  .backgroundColor(0xFFAAAAAA.toInt())
                  .flex(1f))
          .child(
              Text.create(c)
                  .text(decade.year.toString())
                  .textSizeDip(14f)
                  .textColor(0xFFAAAAAA.toInt())
                  .marginDip(HORIZONTAL, 10f)
                  .flex(0f))
          .child(
              Row.create(c)
                  .heightPx(1)
                  .backgroundColor(0xFFAAAAAA.toInt())
                  .flex(1f))
          .backgroundColor(0xFFFAFAFA.toInt())
          .build()
}
