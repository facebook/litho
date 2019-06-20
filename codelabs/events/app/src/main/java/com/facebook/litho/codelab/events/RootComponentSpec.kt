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
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.Param
import com.facebook.litho.annotations.State
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import kotlin.random.Random

@Suppress("MagicNumber")
@LayoutSpec
object RootComponentSpec {

  @OnCreateInitialState
  fun onCreateInitialState(c: ComponentContext, items: StateValue<IntArray>) {
    items.set(IntArray(0))
  }

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext, @State items: IntArray): Component {
    return Column.create(c)
        .paddingDip(YogaEdge.ALL, 20f)
        .child(
            Button.create(c)
                .alignSelf(YogaAlign.CENTER)
                .text("ADD")
                .clickHandler(RootComponent.onClickEvent(c)))
        .child(
            ColorBoxCollection.create(c).items(items))
        .build()
  }

  @OnEvent(ClickEvent::class)
  fun onClickEvent(c: ComponentContext, @State items: IntArray) {
    val newColor = Color.rgb(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
    RootComponent.updateItems(c, items.plus(newColor))
  }

  @OnUpdateState
  fun updateItems(items: StateValue<IntArray>, @Param newItems: IntArray) {
    items.set(newItems)
  }
}
