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

package com.facebook.litho.codelab

import android.graphics.Color
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Prop
import com.facebook.litho.widget.Text

@LayoutSpec
object RootComponentSpec {

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext, @Prop colors: List<Int>): Component {

    return Column.create(c)
        .backgroundColor(0xF0F8FF)
        .child(Text.create(c).text("H-Scroll with fixed height").textSizeDip(15f))
        .child(FixedHeightHscrollComponent.create(c).backgroundColor(Color.GRAY).colors(colors))
        .child(
            Text.create(c)
                .text("H-Scroll height based on height of first item")
                .textSizeDip(15f))
        .child(
            MeasureFirstItemForHeightHscrollComponent.create(c).backgroundColor(Color.GRAY).colors(
                colors))
        .child(
            Text.create(c)
                .text("H-Scroll takes the height of the tallest item")
                .textSizeDip(15f))
        .child(DynamicHeightHscrollComponent.create(c).backgroundColor(Color.GRAY).colors(colors))
        .build()
  }
}
