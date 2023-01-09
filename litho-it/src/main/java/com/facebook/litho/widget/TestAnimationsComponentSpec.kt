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

import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.StateCaller
import com.facebook.litho.StateValue
import com.facebook.litho.Transition
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnCreateTransition
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State

@LayoutSpec
object TestAnimationsComponentSpec {

  @JvmStatic
  @OnCreateInitialState
  fun onCreateInitialState(
      c: ComponentContext,
      state: StateValue<Boolean>,
      @Prop stateCaller: StateCaller
  ) {
    stateCaller.setStateUpdateListener { TestAnimationsComponent.updateStateSync(c) }
    state.set(false)
  }

  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop testComponent: TestComponent,
      @State state: Boolean
  ): Component? = testComponent.getComponent(c, state)

  @JvmStatic
  @OnUpdateState
  fun updateState(state: StateValue<Boolean>) {
    state.set(!state.get()!!)
  }

  @JvmStatic
  @OnCreateTransition
  fun onCreateTransition(c: ComponentContext, @Prop transition: Transition?): Transition? =
      transition

  fun interface TestComponent {
    fun getComponent(componentContext: ComponentContext, state: Boolean): Component?
  }
}
