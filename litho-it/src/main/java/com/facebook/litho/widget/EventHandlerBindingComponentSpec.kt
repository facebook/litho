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

import androidx.annotation.ColorInt
import com.facebook.litho.ClickEvent
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.StateValue
import com.facebook.litho.Wrapper
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.Param
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State

@LayoutSpec
object EventHandlerBindingComponentSpec {

  class StateUpdater {
    internal var componentContext: ComponentContext? = null

    fun updateCounterSync(counter: Int) {
      componentContext?.let { EventHandlerBindingComponent.updateCounterSync(it, counter) }
    }
  }

  fun interface OnButtonClickListener {
    fun onClick(counter: Int)
  }

  fun interface ButtonCreator {
    fun createButton(c: ComponentContext): Component
  }

  @OnCreateInitialState
  fun onCreateInitialState(
      c: ComponentContext,
      counter: StateValue<Int>,
  ) {
    counter.set(0)
  }

  @OnUpdateState
  fun updateCounter(counter: StateValue<Int>, @Param newCounter: Int) {
    counter.set(newCounter)
  }

  @OnCreateLayout
  fun onCreateChildren(
      c: ComponentContext,
      @Prop stateUpdater: StateUpdater,
      @Prop buttonCreator: ButtonCreator,
      @State counter: Int,
      @State @ColorInt buttonBgColor: Int,
  ): Component {
    stateUpdater.componentContext = c

    return Column.create(c)
        .child(Text.create(c).text("Counter: $counter").build())
        .child(
            SimpleNestedTreeComponent(
                Wrapper.create(c)
                    .delegate(buttonCreator.createButton(c))
                    .clickHandler(EventHandlerBindingComponent.onRowClick(c))
                    .build()))
        .build()
  }

  @OnEvent(ClickEvent::class)
  fun onRowClick(
      c: ComponentContext,
      @Prop onButtonClickListener: OnButtonClickListener,
      @State counter: Int
  ) {
    onButtonClickListener.onClick(counter)
  }
}
