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
import android.view.View
import com.facebook.litho.ClickEvent
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.FromEvent
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.Prop

@LayoutSpec
object LayoutSpecConditionalReParentingSpec {

  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop reParent: Boolean,
      @Prop firstComponent: Component
  ): Component =
      if (reParent) {
        Column.create(c)
            .child(
                Column.create(c)
                    .clickHandler(LayoutSpecConditionalReParenting.onClickEvent3(c)) // 3
                    .child(Text.create(c).widthPx(100).heightPx(100).text("test"))
                    .child(
                        Column.create(c)
                            .clickHandler(LayoutSpecConditionalReParenting.onClickEvent1(c)) // 1
                            .child(firstComponent)
                            .child(
                                SolidColor.create(c)
                                    .widthPx(100)
                                    .heightPx(100)
                                    .color(Color.GREEN))))
            .child(
                Column.create(c)
                    .clickHandler(LayoutSpecConditionalReParenting.onClickEvent2(c)) // 2
                    .child(Text.create(c).widthPx(100).heightPx(100).text("test2")))
            .build()
      } else {
        Column.create(c)
            .child(
                Column.create(c)
                    .clickHandler(LayoutSpecConditionalReParenting.onClickEvent3(c)) // 3
                    .child(Text.create(c).widthPx(100).heightPx(100).text("test")))
            .child(
                Column.create(c)
                    .clickHandler(LayoutSpecConditionalReParenting.onClickEvent2(c)) // 2
                    .child(Text.create(c).widthPx(100).heightPx(100).text("test2"))
                    .child(
                        Column.create(c)
                            .clickHandler(LayoutSpecConditionalReParenting.onClickEvent1(c)) // 1
                            .child(firstComponent)
                            .child(
                                SolidColor.create(c)
                                    .widthPx(100)
                                    .heightPx(100)
                                    .color(Color.GREEN))))
            .build()
      }

  @JvmStatic
  @OnEvent(ClickEvent::class)
  fun onClickEvent1(c: ComponentContext, @FromEvent view: View) = Unit

  @JvmStatic
  @OnEvent(ClickEvent::class)
  fun onClickEvent2(c: ComponentContext, @FromEvent view: View) = Unit

  @JvmStatic
  @OnEvent(ClickEvent::class)
  fun onClickEvent3(c: ComponentContext, @FromEvent view: View) = Unit
}
