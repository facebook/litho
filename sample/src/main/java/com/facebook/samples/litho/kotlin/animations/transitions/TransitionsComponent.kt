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

package com.facebook.samples.litho.kotlin.animations.transitions

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.Transition
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.width
import com.facebook.litho.dp
import com.facebook.litho.flexbox.alignSelf
import com.facebook.litho.transition.transitionKey
import com.facebook.litho.transition.useTransition
import com.facebook.litho.useState
import com.facebook.litho.view.alpha
import com.facebook.litho.view.background
import com.facebook.litho.view.onClick
import com.facebook.samples.litho.kotlin.drawable.RoundedRect
import com.facebook.yoga.YogaAlign

private const val TRANSITION_KEY_TEXT = "key"
private const val TRANSITION_KEY2_TEXT = "key2"

class TransitionsComponent : KComponent() {

  override fun ComponentScope.render(): Component? {
    useTransition(
        Transition.parallel<Transition.BaseTransitionUnitsBuilder>(
            Transition.create(Transition.TransitionKeyType.GLOBAL, TRANSITION_KEY_TEXT)
                .animate(AnimatedProperties.X),
            Transition.create(TRANSITION_KEY_TEXT).animate(AnimatedProperties.ALPHA),
            Transition.create(Transition.TransitionKeyType.GLOBAL, TRANSITION_KEY2_TEXT)
                .animate(AnimatedProperties.HEIGHT, AnimatedProperties.WIDTH)))

    val alphaValue = useState { 1f }
    val shouldExpand = useState { false }
    val toRight = useState { true }
    return Column(
        style =
            Style.width(200.dp).alpha(alphaValue.value).onClick {
              toRight.update(!toRight.value)
              alphaValue.update { prevValue -> if (prevValue == 1f) 0.5f else 1f }
              shouldExpand.update(!shouldExpand.value)
            }) {
      child(
          Column(
              style =
                  Style.width(50.dp)
                      .height(50.dp)
                      .margin(all = 5.dp)
                      .transitionKey(
                          context, TRANSITION_KEY_TEXT, Transition.TransitionKeyType.GLOBAL)
                      .alignSelf(if (toRight.value) YogaAlign.FLEX_END else YogaAlign.FLEX_START)
                      .background(RoundedRect(0xff666699, 8.dp))))
      child(
          Column(
              style =
                  Style.width(if (shouldExpand.value) 75.dp else 50.dp)
                      .height(if (shouldExpand.value) 75.dp else 50.dp)
                      .margin(all = 5.dp)
                      .transitionKey(
                          context, TRANSITION_KEY2_TEXT, Transition.TransitionKeyType.GLOBAL)
                      .background(RoundedRect(0xffba7bb5, 8.dp))))
      child(ContainersComponent())
    }
  }
}
