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
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaEdge

@Suppress("MagicNumber")
@LayoutSpec
object RootComponentSpec {

  @OnCreateInitialState
  fun onCreateInitialState(c: ComponentContext, toggleState: StateValue<Boolean>) {
    toggleState.set(true)
  }

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop labelText: String,
      @State toggleState: Boolean
  ): Component {
    return Column.create(c)
        .child(Text.create(c).textSizeSp(20f).text(labelText))
        .child(
            Row.create(c)
                .child(
                    Text.create(c)
                        .textSizeSp(20f)
                        .text("Toggle state: ")
                        .marginPx(YogaEdge.RIGHT, 30)
                        .clickHandler(RootComponent.onClick(c)))
                .child(Text.create(c).textSizeSp(20f).text(toggleState.toString())))
        .build()
  }

  @OnEvent(ClickEvent::class)
  fun onClick(c: ComponentContext) {
    RootComponent.updateToggle(c)
  }

  @OnUpdateState
  fun updateToggle(toggleState: StateValue<Boolean>) {
    val toggleStateVal: Boolean? = toggleState.get()
    if (toggleStateVal == true) {
      toggleState.set(false)
    } else {
      toggleState.set(true)
    }
  }
}
