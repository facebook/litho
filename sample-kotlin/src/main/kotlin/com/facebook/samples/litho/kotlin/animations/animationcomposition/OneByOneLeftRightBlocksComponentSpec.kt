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

package com.facebook.samples.litho.kotlin.animations.animationcomposition

import android.graphics.Color
import com.facebook.litho.ClickEvent
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Row
import com.facebook.litho.StateValue
import com.facebook.litho.Transition
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnCreateTransition
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.State
import com.facebook.yoga.YogaAlign

@LayoutSpec
object OneByOneLeftRightBlocksComponentSpec {

  private const val TRANSITION_KEY_RED = "red"
  private const val TRANSITION_KEY_BLUE = "blue"
  private const val TRANSITION_KEY_GREEN = "green"
  private val ALL_TRANSITION_KEYS = arrayOf(
      TRANSITION_KEY_RED,
      TRANSITION_KEY_BLUE,
      TRANSITION_KEY_GREEN
  )

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext, @State state: Int): Component {
    val redLeft = state == 0 || state == 4 || state == 5
    val blueLeft = state == 0 || state == 1 || state == 5
    val greenLeft = state == 0 || state == 1 || state == 2

    return Column.create(c)
        .child(
            Column.create(c)
                .alignItems(if (redLeft) YogaAlign.FLEX_START else YogaAlign.FLEX_END)
                .child(
                    Row.create(c)
                        .heightDip(40f)
                        .widthDip(40f)
                        .backgroundColor(Color.parseColor("#ee1111"))
                        .transitionKey(TRANSITION_KEY_RED)
                        .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                        .build()))
        .child(
            Column.create(c)
                .alignItems(if (blueLeft) YogaAlign.FLEX_START else YogaAlign.FLEX_END)
                .child(
                    Row.create(c)
                        .heightDip(40f)
                        .widthDip(40f)
                        .backgroundColor(Color.parseColor("#1111ee"))
                        .transitionKey(TRANSITION_KEY_BLUE)
                        .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                        .build()))
        .child(
            Column.create(c)
                .alignItems(if (greenLeft) YogaAlign.FLEX_START else YogaAlign.FLEX_END)
                .child(
                    Row.create(c)
                        .heightDip(40f)
                        .widthDip(40f)
                        .backgroundColor(Color.parseColor("#11ee11"))
                        .transitionKey(TRANSITION_KEY_GREEN)
                        .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                        .build()))
        .clickHandler(OneByOneLeftRightBlocksComponent.onClick(c))
        .build()
  }

  @OnEvent(ClickEvent::class)
  fun onClick(c: ComponentContext) {
    OneByOneLeftRightBlocksComponent.updateState(c)
  }

  @OnUpdateState
  fun updateState(state: StateValue<Int>) {
    state.set((state.get()!! + 1) % 6)
  }

  @OnCreateTransition
  fun onCreateTransition(c: ComponentContext): Transition =
      Transition.create(Transition.TransitionKeyType.GLOBAL, *ALL_TRANSITION_KEYS)
          .animate(AnimatedProperties.X, AnimatedProperties.Y, AnimatedProperties.ALPHA)
}
