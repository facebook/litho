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

package com.facebook.samples.litho.kotlin.animations.animatedbadge

import android.graphics.Color
import android.graphics.Typeface
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.Transition
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.position
import com.facebook.litho.flexbox.positionType
import com.facebook.litho.key
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.transition.transitionKey
import com.facebook.litho.transition.useTransition
import com.facebook.litho.useState
import com.facebook.litho.view.background
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp
import com.facebook.samples.litho.kotlin.drawable.RoundedRect
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaPositionType

class AnimatedBadgeKotlin : KComponent() {
  private val ANIMATION_DURATION = 300
  private val ANIMATOR = Transition.timing(ANIMATION_DURATION)
  private val TRANSITION_KEY_TEXT = "text"

  override fun ComponentScope.render(): Component {
    val state = useState { 0 }
    val expanded1 = state.value == 1 || state.value == 2
    val expanded2 = state.value == 2 || state.value == 3

    // start_example
    useTransition(
        Transition.parallel<Transition.BaseTransitionUnitsBuilder>(
            Transition.allLayout().animator(ANIMATOR),
            Transition.create(Transition.TransitionKeyType.GLOBAL, TRANSITION_KEY_TEXT)
                .animate(AnimatedProperties.WIDTH)
                .appearFrom(0f)
                .disappearTo(0f)
                .animator(ANIMATOR)
                .animate(AnimatedProperties.ALPHA)
                .appearFrom(0f)
                .disappearTo(0f)
                .animator(ANIMATOR)))
    // end_example

    return Column(style = Style.padding(all = 8.dp).onClick { state.update { (it + 1) % 4 } }) {
      child(Row(style = Style.margin(top = 8.dp)) { child(buildComment1(expanded1)) })
      child(Row(style = Style.margin(top = 16.dp)) { child(buildComment2(expanded2)) })
    }
  }

  private fun ResourcesScope.buildComment1(expanded: Boolean): Component =
      Column(style = Style.padding(all = 8.dp).background(RoundedRect(0xFFDDDDDD, 20.dp))) {
        child(
            Row(alignItems = YogaAlign.CENTER) {
              child(Text("Cristobal Castilla", textSize = 16.dp, textStyle = Typeface.BOLD))
              child(
                  Row(
                      style =
                          Style.margin(start = 8.dp)
                              .padding(all = 3.dp)
                              .background(RoundedRect(Color.WHITE, 12.dp)),
                      alignItems = YogaAlign.CENTER) {
                        child(
                            Column(
                                style =
                                    Style.height(18.dp)
                                        .width(18.dp)
                                        .background(RoundedRect(0xffffb74b, 9.dp))))
                        if (expanded) {
                          child(
                              key("text") {
                                Text(
                                    "Top Follower",
                                    style =
                                        Style.margin(left = 8.dp)
                                            .transitionKey(
                                                context,
                                                TRANSITION_KEY_TEXT,
                                                Transition.TransitionKeyType.GLOBAL),
                                    textSize = 12.dp,
                                    clipToBounds = true)
                              })
                        }
                        child(
                            Text(
                                "+1",
                                style = Style.margin(left = 8.dp, right = 4.dp),
                                textSize = 12.dp,
                                textColor = Color.BLUE))
                      })
            })
        child(Text("So awesome!", textSize = 18.dp))
      }

  private fun ResourcesScope.buildComment2(expanded: Boolean): Component =
      Column(style = Style.padding(all = 8.dp).background(RoundedRect(0xFFDDDDDD, 20.dp))) {
        child(
            Row(alignItems = YogaAlign.CENTER) {
              child(Text("Cristobal Castilla", textSize = 16.dp, textStyle = Typeface.BOLD))
              child(
                  Row(
                      style =
                          Style.width(if (expanded) 48.dp else 24.dp)
                              .margin(start = 8.dp)
                              .padding(all = 3.dp)
                              .background(RoundedRect(Color.WHITE, 12.dp)),
                      alignItems = YogaAlign.CENTER) {
                        child(
                            Column(
                                style =
                                    Style.positionType(YogaPositionType.ABSOLUTE)
                                        .position(start = if (expanded) 27.dp else 3.dp)
                                        .positionType(YogaPositionType.ABSOLUTE)
                                        .height(18.dp)
                                        .width(18.dp)
                                        .background(RoundedRect(0xFFB2CFE5, 9.dp))))
                        child(
                            Column(
                                style =
                                    Style.positionType(YogaPositionType.ABSOLUTE)
                                        .position(start = if (expanded) 15.dp else 3.dp)
                                        .positionType(YogaPositionType.ABSOLUTE)
                                        .height(18.dp)
                                        .width(18.dp)
                                        .background(RoundedRect(0xFF4B8C61, 9.dp))))
                        child(
                            Column(
                                style =
                                    Style.height(18.dp)
                                        .width(18.dp)
                                        .background(RoundedRect(0xFFFFB74B, 9.dp))))
                      })
            })
        child(Text("So awesome!", textSize = 18.dp))
      }
}
