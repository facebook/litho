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
import com.facebook.litho.Transition.TransitionUnitsBuilder
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

class LeftRightBlocksSequenceComponent : KComponent() {

  override fun ComponentScope.render(): Component? {
    val left = useState { false }

    useTransition(
        Transition.sequence<TransitionUnitsBuilder>(
            Transition.create(Transition.TransitionKeyType.GLOBAL, "red")
                .animate(AnimatedProperties.X),
            Transition.create(Transition.TransitionKeyType.GLOBAL, "blue")
                .animate(AnimatedProperties.X),
            Transition.create(Transition.TransitionKeyType.GLOBAL, "green")
                .animate(AnimatedProperties.X)))
    return Column(
        alignItems = (if (left.value) YogaAlign.FLEX_START else YogaAlign.FLEX_END),
        style = Style.onClick { e -> left.update(!left.value) }) {
      child(
          Row(
              style =
                  Style.height(40f.dp)
                      .width(40f.dp)
                      .backgroundColor(Color.parseColor("#ee1111"))
                      .transitionKey(context, "red", Transition.TransitionKeyType.GLOBAL)))
      child(
          Row(
              style =
                  Style.height(40f.dp)
                      .width(40f.dp)
                      .backgroundColor(Color.parseColor("#1111ee"))
                      .transitionKey(context, "blue", Transition.TransitionKeyType.GLOBAL)))
      child(
          Row(
              style =
                  Style.height(40f.dp)
                      .width(40f.dp)
                      .backgroundColor(Color.parseColor("#11ee11"))
                      .transitionKey(context, "green", Transition.TransitionKeyType.GLOBAL)))
    }
  }
}
