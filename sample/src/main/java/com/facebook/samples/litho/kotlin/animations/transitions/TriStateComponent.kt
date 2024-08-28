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

import android.graphics.Color
import android.view.animation.AccelerateDecelerateInterpolator
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.Transition
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.width
import com.facebook.litho.core.widthPercent
import com.facebook.litho.flexbox.alignSelf
import com.facebook.litho.transition.transitionKey
import com.facebook.litho.transition.useTransition
import com.facebook.litho.useState
import com.facebook.litho.view.alpha
import com.facebook.litho.view.background
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp
import com.facebook.samples.litho.kotlin.drawable.RoundedRect
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify

class TriStateComponent : KComponent() {

  @OptIn(ExperimentalLithoApi::class)
  override fun ComponentScope.render(): Component {
    // start_example
    val state = useState { TriState.HEIGHT }
    useTransition(state.value) {
      val diff = diffOf(state.value)
      val previous = diff.previous
      val next = diff.next
      val animator =
          when (if (previous == null || next == null) 0 else previous.ordinal + next.ordinal) {
            1 -> Transition.SPRING_WITH_OVERSHOOT
            2 -> Transition.timing(1_000, AccelerateDecelerateInterpolator())
            3 -> Transition.springWithConfig(250.0, 10.0)
            else -> Transition.timing(0)
          }
      Transition.create(Transition.TransitionKeyType.GLOBAL, "fancy-component")
          .animate(*AnimatedProperties.AUTO_LAYOUT_PROPERTIES)
          .animator(animator)
    }
    // end_example
    return Column {
      child(
          Row(
              style = Style.widthPercent(100f).margin(top = 10.dp),
              justifyContent = YogaJustify.SPACE_BETWEEN) {
                for (entry in TriState.entries) {
                  child(
                      Column(
                          style =
                              Style.width(50.dp)
                                  .height(50.dp)
                                  .margin(all = 5.dp)
                                  .alpha(if (state.value == entry) 1f else 0.3f)
                                  .background(RoundedRect(entry.color, 8.dp))
                                  .onClick { state.update(entry) }))
                }
              })
      child(
          Column(
              style =
                  Style.width(if (state.value == TriState.WIDTH) 150.dp else 50.dp)
                      .height(if (state.value == TriState.HEIGHT) 150.dp else 50.dp)
                      .transitionKey(
                          context, "fancy-component", Transition.TransitionKeyType.GLOBAL)
                      .alignSelf(
                          if (state.value == TriState.ALIGN) YogaAlign.FLEX_END
                          else YogaAlign.FLEX_START)
                      .margin(all = 5.dp)
                      .background(RoundedRect(Color.RED, 8.dp))))
    }
  }
}

private enum class TriState(val color: Long) {
  HEIGHT(0xffbf678d),
  WIDTH(0xff6ab071),
  ALIGN(0xff678db0)
}
