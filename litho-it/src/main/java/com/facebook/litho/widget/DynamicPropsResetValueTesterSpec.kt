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

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.DynamicValue
import com.facebook.litho.Row
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State
import kotlin.jvm.JvmField

@LayoutSpec
object DynamicPropsResetValueTesterSpec {

  const val ALPHA_OPAQUE = 1f
  const val ALPHA_TRANSPARENT = 0f

  @JvmStatic
  @OnCreateInitialState
  fun onCreateInitialState(
      c: ComponentContext,
      setDynamicAlpha: StateValue<Boolean>,
      dynamicAlpha: StateValue<DynamicValue<Float>>
  ) {
    setDynamicAlpha.set(true)
    dynamicAlpha.set(DynamicValue(ALPHA_TRANSPARENT))
  }

  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop caller: Caller,
      @State setDynamicAlpha: Boolean,
      @State dynamicAlpha: DynamicValue<Float>
  ): Component {
    caller.set(c)
    val textChild = Text.create(c).text("Child 1").viewTag("vt1")
    if (setDynamicAlpha) {
      textChild.alpha(dynamicAlpha)
    }
    return Row.create(c)
        .child(Column.create(c).child(textChild.build()))
        .child(Row.create(c).child(Text.create(c).text("Child 2").viewTag("vt2")))
        .build()
  }

  @JvmStatic
  @OnUpdateState
  fun toggleSetDynamicAlpha(setDynamicAlpha: StateValue<Boolean>) {
    setDynamicAlpha.set(!setDynamicAlpha.get()!!)
  }

  class Caller {
    @JvmField var c: ComponentContext? = null

    fun set(c: ComponentContext) {
      this.c = c
    }

    fun toggleShowChild() {
      DynamicPropsResetValueTester.toggleSetDynamicAlphaSync(c)
    }
  }
}
