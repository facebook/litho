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

package com.facebook.litho.transition

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.Transition
import com.facebook.litho.Transition.SPRING_WITH_OVERSHOOT
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.core.height
import com.facebook.litho.dp
import com.facebook.litho.view.alpha

private const val CMPONENT_A_KEY = "componentAKey"
private const val COMPONENT_B_KEY = "componentBKey"
private const val EXPAND_TO_REVEAL_KEY = "expandToReveal"

/**
 * Component implementing the Expand to Reveal behavior
 * @param isExpanded is the component currently expanded or hidden
 * @param component that will be revealed
 * @param transitionAnimator optional param for the custom [TransitionAnimator]
 */
class ExpandToReveal(
    private val isExpanded: Boolean,
    private val component: Component,
    private val transitionAnimator: Transition.TransitionAnimator = SPRING_WITH_OVERSHOOT
) : KComponent() {

  override fun ComponentScope.render(): Component? {
    val collapsedStyle = Style.height(0.dp).alpha(0f).transitionKey(context, EXPAND_TO_REVEAL_KEY)
    val normalStyle = Style.alpha(1f).transitionKey(context, EXPAND_TO_REVEAL_KEY)
    useTransition(
        Transition.parallel<Transition.BaseTransitionUnitsBuilder>(
            Transition.create(EXPAND_TO_REVEAL_KEY)
                .animate(AnimatedProperties.ALPHA)
                .animator(transitionAnimator),
            Transition.allLayout()))
    return Column(style = if (isExpanded) collapsedStyle else normalStyle) { child(component) }
  }
}

class CrossFade(
    private val showComponentB: Boolean,
    private val componentA: Component,
    private val componentB: Component,
    private val transitionType: TransitionType = TransitionType.FADE_IN_FADE_OUT
) : KComponent() {
  override fun ComponentScope.render(): Component? {
    useTransition(getAppearDisappearTransition(transitionType))

    return if (showComponentB) {
      Column(style = Style.transitionKey(context, COMPONENT_B_KEY)) { child(componentB) }
    } else {
      Column(style = Style.transitionKey(context, CMPONENT_A_KEY)) { child(componentA) }
    }
  }

  private fun getAppearDisappearTransition(transitionType: TransitionType): Transition =
      when (transitionType) {
        TransitionType.FADE_IN_FADE_OUT ->
            Transition.parallel<Transition.BaseTransitionUnitsBuilder>(
                Transition.create(CMPONENT_A_KEY)
                    .animate(AnimatedProperties.ALPHA)
                    .appearFrom(0f)
                    .disappearTo(0f),
                Transition.create(COMPONENT_B_KEY)
                    .animate(AnimatedProperties.ALPHA)
                    .appearFrom(0f)
                    .disappearTo(0f),
                Transition.allLayout())
      }
}

enum class TransitionType {
  FADE_IN_FADE_OUT,
}
