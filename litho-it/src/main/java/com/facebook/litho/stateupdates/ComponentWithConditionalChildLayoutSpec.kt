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

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.Param
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State
import com.facebook.litho.widget.Text

@LayoutSpec
object ComponentWithConditionalChildLayoutSpec {

  const val INITIAL_COUNT_VALUE = 0

  @JvmStatic
  @OnCreateInitialState
  fun OnCreateInitialState(c: ComponentContext, showChild: StateValue<Boolean?>) {
    showChild.set(true)
  }

  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop(optional = true) caller: Caller?,
      @Prop(optional = true) childCaller: ComponentWithCounterStateLayoutSpec.Caller?,
      @State showChild: Boolean
  ): Component {
    caller?.set(c)
    return Column.create(c)
        .child(
            if (showChild)
                ComponentWithCounterStateLayout.create(c).caller(childCaller).key("testKey")
            else Text.create(c).text("Empty").key("testKey"))
        .build()
  }

  @JvmStatic
  @OnUpdateState
  fun updateChild(showChild: StateValue<Boolean?>, @Param showChildValue: Boolean) {
    showChild.set(showChildValue)
  }

  class Caller {
    private var c: ComponentContext? = null

    fun set(c: ComponentContext) {
      this.c = c
    }

    fun hideChildComponent() {
      ComponentWithConditionalChildLayout.updateChildSync(c, false)
    }

    fun showChildComponent() {
      ComponentWithConditionalChildLayout.updateChildSync(c, true)
    }
  }
}
