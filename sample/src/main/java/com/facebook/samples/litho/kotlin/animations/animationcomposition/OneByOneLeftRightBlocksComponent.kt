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

package com.facebook.samples.litho.kotlin.animations.animationcomposition

import android.graphics.Color
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.Transition
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.dp
import com.facebook.litho.transition.transitionKey
import com.facebook.litho.transition.useTransition
import com.facebook.litho.useState
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.onClick
import com.facebook.yoga.YogaAlign

private const val TRANSITION_KEY_RED = "red"
private const val TRANSITION_KEY_BLUE = "blue"
private const val TRANSITION_KEY_GREEN = "green"
private val ALL_TRANSITION_KEYS =
    arrayOf(TRANSITION_KEY_RED, TRANSITION_KEY_BLUE, TRANSITION_KEY_GREEN)

class OneByOneLeftRightBlocksComponent : KComponent() {

  override fun ComponentScope.render(): Component? {
    val state = useState { 0 }

    val redLeft = state.value == 0 || state.value == 4 || state.value == 5
    val blueLeft = state.value == 0 || state.value == 1 || state.value == 5
    val greenLeft = state.value == 0 || state.value == 1 || state.value == 2

    useTransition(
        Transition.create(Transition.TransitionKeyType.GLOBAL, *ALL_TRANSITION_KEYS)
            .animate(AnimatedProperties.X, AnimatedProperties.Y, AnimatedProperties.ALPHA))

    return Column(style = Style.onClick { state.update { prevValue -> (prevValue + 1) % 6 } }) {
      child(
          Column(alignItems = (if (redLeft) YogaAlign.FLEX_START else YogaAlign.FLEX_END)) {
            child(
                Row(
                    style =
                        Style.height(40f.dp)
                            .width(40f.dp)
                            .backgroundColor(Color.parseColor("#ee1111"))
                            .transitionKey(
                                context, TRANSITION_KEY_RED, Transition.TransitionKeyType.GLOBAL)))
          })

      child(
          Column(alignItems = (if (blueLeft) YogaAlign.FLEX_START else YogaAlign.FLEX_END)) {
            child(
                Row(
                    style =
                        Style.height(40f.dp)
                            .width(40f.dp)
                            .backgroundColor(Color.parseColor("#1111ee"))
                            .transitionKey(
                                context, TRANSITION_KEY_BLUE, Transition.TransitionKeyType.GLOBAL)))
          })
      child(
          Column(alignItems = (if (greenLeft) YogaAlign.FLEX_START else YogaAlign.FLEX_END)) {
            child(
                Row(
                    style =
                        Style.height(40f.dp)
                            .width(40f.dp)
                            .backgroundColor(Color.parseColor("#11ee11"))
                            .transitionKey(
                                context,
                                TRANSITION_KEY_GREEN,
                                Transition.TransitionKeyType.GLOBAL)))
          })
    }
  }
}
