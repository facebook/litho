/*
 * Copyright 2019-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho.codelab.events

import android.graphics.Color
import com.facebook.litho.ClickEvent
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Row
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.Param
import com.facebook.litho.annotations.State
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaJustify
import kotlin.random.Random

@Suppress("MagicNumber")
@LayoutSpec
object RootComponentSpec {

  @OnCreateInitialState
  fun onCreateInitialState(
      c: ComponentContext,
      items: StateValue<IntArray>,
      statusText: StateValue<String>,
      statusColor: StateValue<Int>
  ) {
    items.set(IntArray(0))
    statusText.set("")
    statusColor.set(Color.TRANSPARENT)
  }

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @State items: IntArray,
      @State statusText: String,
      @State statusColor: Int
  ): Component {
    return Column.create(c)
        .paddingDip(YogaEdge.ALL, 20f)
        .child(
            Row.create(c)
                .justifyContent(YogaJustify.CENTER)
                .child(
                    Button.create(c)
                        .text("ADD")
                        .clickHandler(RootComponent.onClickEvent(c, true)))
                .child(
                    Button.create(c)
                        .marginDip(YogaEdge.LEFT, 20f)
                        .text("REMOVE")
                        .clickHandler(RootComponent.onClickEvent(c, false))))
        .child(
            Text.create(c)
                .alignSelf(YogaAlign.CENTER)
                .textSizeSp(18f)
                .text(statusText)
                .textColor(statusColor))
        .child(
            ColorBoxCollection.create(c)
                .items(items))
        .build()
  }

  @OnEvent(ClickEvent::class)
  fun onClickEvent(c: ComponentContext, @State items: IntArray, @Param add: Boolean) {
    if (add) {
      val newColor = Color.rgb(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
      RootComponent.updateItems(c, items.plus(newColor))
      RootComponent.updateTextStatus(c, newColor, "New item added")
    } else {
      if (items.isNotEmpty()) {
        RootComponent.updateItems(c, items.sliceArray(0..items.size - 2))
        RootComponent.updateTextStatus(c, items.last(), "Item removed")
      }
    }
  }

  @OnUpdateState
  fun updateItems(items: StateValue<IntArray>, @Param newItems: IntArray) {
    items.set(newItems)
  }

  @OnUpdateState
  fun updateTextStatus(
      statusText: StateValue<String>,
      statusColor: StateValue<Int>,
      @Param newColor: Int,
      @Param newStatus: String
  ) {
    statusText.set(newStatus)
    statusColor.set(newColor)
  }
}
