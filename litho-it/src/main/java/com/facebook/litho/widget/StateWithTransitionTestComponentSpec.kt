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
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnUpdateStateWithTransition
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State

@LayoutSpec
object StateWithTransitionTestComponentSpec {

  const val RED_TRANSITION_KEY = "red"
  const val GREEN_TRANSITION_KEY = "green"
  const val BLUE_TRANSITION_KEY = "blue"

  @JvmStatic
  @OnCreateInitialState
  fun onCreateInitialState(
      c: ComponentContext,
      state: StateValue<Boolean>,
      @Prop stateCaller: StateCaller
  ) {
    stateCaller.setStateUpdateListener {
      StateWithTransitionTestComponent.updateStateWithTransition(c)
    }
    state.set(false)
  }

  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop testComponent: TestAnimationsComponentSpec.TestComponent,
      @State state: Boolean
  ): Component? = testComponent.getComponent(c, state)

  @JvmStatic
  @OnUpdateStateWithTransition
  fun updateState(state: StateValue<Boolean>): Transition {
    state.set(!state.get()!!)
    return Transition.sequence(
        Transition.create(RED_TRANSITION_KEY)
            .animator(Transition.timing(144))
            .animate(AnimatedProperties.X),
        Transition.create(GREEN_TRANSITION_KEY)
            .animator(Transition.timing(144))
            .animate(AnimatedProperties.X),
        Transition.create(BLUE_TRANSITION_KEY)
            .animator(Transition.timing(144))
            .animate(AnimatedProperties.X))
  }
}
