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

package com.facebook.samples.litho.kotlin.animations.docs

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.Transition
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.core.heightPercent
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.transition.transitionKey
import com.facebook.litho.transition.useTransition
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.samples.litho.kotlin.bordereffects.NiceColor
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify

private const val YellowKey = "YellowKey"

// start_example
class StaggerTransitionSameComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    val shown = useState { false }

    // Animate different properties of the same component with staggered timing
    useTransition(
        Transition.stagger(
            50,
            Transition.create(YellowKey).animate(AnimatedProperties.Y),
            Transition.create(YellowKey).animate(AnimatedProperties.X),
        ),
    )

    return Row(
        alignItems = if (shown.value) YogaAlign.FLEX_END else YogaAlign.FLEX_START,
        justifyContent = if (shown.value) YogaJustify.FLEX_END else YogaJustify.FLEX_START,
        style = Style.heightPercent(100f).onClick { shown.update { !it } },
    ) {
      child(ColorCell(color = NiceColor.Yellow, style = Style.transitionKey(context, YellowKey)))
    }
  }
}
// end_example
