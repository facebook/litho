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
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.StateValue
import com.facebook.litho.Wrapper
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.Param
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State
import com.facebook.litho.sections.Children
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.annotations.GroupSectionSpec
import com.facebook.litho.sections.annotations.OnCreateChildren
import com.facebook.litho.sections.common.SingleComponentSection

@GroupSectionSpec
object EventHandlerBindingSectionSpec {

  class StateUpdater {
    internal var sectionContext: SectionContext? = null

    fun updateCounterSync(counter: Int) {
      sectionContext?.let { EventHandlerBindingSection.updateCounterSync(it, counter) }
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
      c: SectionContext,
      counter: StateValue<Int>,
  ) {
    counter.set(0)
  }

  @OnUpdateState
  fun updateCounter(counter: StateValue<Int>, @Param newCounter: Int) {
    counter.set(newCounter)
  }

  @OnCreateChildren
  fun onCreateChildren(
      c: SectionContext,
      @Prop stateUpdater: StateUpdater,
      @Prop buttonCreator: ButtonCreator,
      @State counter: Int,
      @State @ColorInt buttonBgColor: Int,
  ): Children {
    stateUpdater.sectionContext = c

    return Children.create()
        .child(
            SingleComponentSection.create(c)
                .component(Text.create(c).text("Counter: $counter").build()))
        .child(
            SingleComponentSection.create(c)
                .component(
                    Wrapper.create(c)
                        .delegate(buttonCreator.createButton(c))
                        .clickHandler(EventHandlerBindingSection.onRowClick(c))
                        .build()))
        .build()
  }

  @OnEvent(ClickEvent::class)
  fun onRowClick(
      c: SectionContext,
      @Prop onButtonClickListener: OnButtonClickListener,
      @State counter: Int
  ) {
    onButtonClickListener.onClick(counter)
  }
}
