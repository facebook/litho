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
package com.facebook.litho.codelab

import android.graphics.Color
import android.text.Layout
import com.facebook.litho.ClickEvent
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Row
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge

@Suppress("MagicNumber")
@LayoutSpec
object RootComponentSpec {

  private val COLOR_BG_DECREMENT = Color.parseColor("#ff7733")
  private val COLOR_TXT_DECREMENT = Color.parseColor("#aa4411")
  private val COLOR_BG_INCREMENT = Color.parseColor("#33ff77")
  private val COLOR_TXT_INCREMENT = Color.parseColor("#11aa77")
  private val COLOR_BG_COUNT = Color.parseColor("#efefef")
  private val COLOR_TXT_COUNT = Color.parseColor("#232323")

  @OnCreateInitialState
  fun onCreateInitialState(
      c: ComponentContext,
      @Prop(optional = true) startCount: Int,
      count: StateValue<Int>
  ) {
    count.set(startCount) // set the initial value of count.
  }

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop(optional = true) startCount: Int,
      @State count: Int
  ): Component {
    return Row.create(c)
      .alignItems(YogaAlign.FLEX_START)
      .child(
        Text.create(c)
          .backgroundColor(COLOR_BG_DECREMENT)
          .paddingDip(YogaEdge.LEFT, 16F)
          .paddingDip(YogaEdge.RIGHT, 16F)
          .paddingDip(YogaEdge.TOP, 4F)
          .paddingDip(YogaEdge.BOTTOM, 4F)
          .marginDip(YogaEdge.ALL, 8F)
          .text("-")
          .textSizeSp(24F)
          .textColor(COLOR_TXT_DECREMENT)
          /* Setting the click handler for the decrement button */
          .clickHandler(RootComponent.onDecrement(c))
      )
      .child(
        Text.create(c)
          .backgroundColor(COLOR_BG_COUNT)
          .paddingDip(YogaEdge.LEFT, 16F)
          .paddingDip(YogaEdge.RIGHT, 16F)
          .paddingDip(YogaEdge.TOP, 4F)
          .paddingDip(YogaEdge.BOTTOM, 4F)
          .marginDip(YogaEdge.ALL, 8F)
          .minWidthDip(64f)
          .text(count.toString())
          .textSizeSp(24F)
          .textColor(COLOR_TXT_COUNT)
          .textAlignment(Layout.Alignment.ALIGN_CENTER)
      )
      .child(
        Text.create(c)
          .backgroundColor(COLOR_BG_INCREMENT)
          .paddingDip(YogaEdge.LEFT, 16F)
          .paddingDip(YogaEdge.RIGHT, 16F)
          .paddingDip(YogaEdge.TOP, 4F)
          .paddingDip(YogaEdge.BOTTOM, 4F)
          .marginDip(YogaEdge.ALL, 8F)
          .text("+")
          .textSizeSp(24F)
          .textColor(COLOR_TXT_INCREMENT)
          /* Setting the click handler for the increment button */
          .clickHandler(RootComponent.onIncrement(c))
      )
      .build()
  }

  /**
   * The event handler for the decrement button.
   */
  @OnEvent(ClickEvent::class)
  fun onDecrement(c: ComponentContext) {
    RootComponent.decrement(c) // Call the decrement state update method.
  }

  /**
   * Update method for decrement which receives the state value container for `count`.
   */
  @OnUpdateState
  fun decrement(count: StateValue<Int>) {
    count.set(count.get()?.minus(1)) // Actually decrement the value of count.
  }

  /**
   * The event handler for the increment button.
   */
  @OnEvent(ClickEvent::class)
  fun onIncrement(c: ComponentContext) {
    RootComponent.increment(c) // Call the increment state update method.
  }

  /**
   * Update method for increment which receives the state value container of for `count`.
   */
  @OnUpdateState
  fun increment(count: StateValue<Int>) {
    count.set(count.get()?.plus(1)) // Actually increment the value of count.
  }
}
