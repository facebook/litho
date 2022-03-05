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
import com.facebook.litho.dp
import com.facebook.litho.flexbox.position
import com.facebook.litho.flexbox.positionType
import com.facebook.litho.key
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.transition.transitionKey
import com.facebook.litho.transition.useTransition
import com.facebook.litho.useState
import com.facebook.litho.view.background
import com.facebook.litho.view.onClick
import com.facebook.samples.litho.kotlin.drawable.RoundedRect
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaPositionType

class AnimatedBadgeKotlin : KComponent() {
  private val ANIMATION_DURATION = 300
  private val ANIMATOR = Transition.timing(ANIMATION_DURATION)
  private val TRANSITION_KEY_TEXT = "text"

  override fun ComponentScope.render(): Component? {
    val state = useState { 0 }
    val expanded1 = state.value == 1 || state.value == 2
    val expanded2 = state.value == 2 || state.value == 3

    useTransition(
        Transition.parallel<Transition.BaseTransitionUnitsBuilder>(
            Transition.allLayout().animator(ANIMATOR),
            Transition.create(Transition.TransitionKeyType.GLOBAL, TRANSITION_KEY_TEXT)
                .animate(AnimatedProperties.WIDTH)
                .appearFrom(0f)
                .disappearTo(0f)
                .animator(ANIMATOR),
            Transition.create(Transition.TransitionKeyType.GLOBAL, TRANSITION_KEY_TEXT)
                .animate(AnimatedProperties.ALPHA)
                .appearFrom(0f)
                .disappearTo(0f)
                .animator(ANIMATOR)))

    return Column(
        style = Style.padding(all = 8f.dp).onClick { state.update(state.value.plus(1).rem(4)) }) {
      child(Row(style = Style.margin(top = 8f.dp)) { child(buildComment1(expanded1)) })
      child(Row(style = Style.margin(top = 16f.dp)) { child(buildComment2(expanded2)) })
    }
  }

  private fun ResourcesScope.buildComment1(expanded: Boolean): Component =
      Column(style = Style.padding(all = 8f.dp).background(RoundedRect(0xFFDDDDDD, 20.dp))) {
        child(
            Row(alignItems = YogaAlign.CENTER) {
              child(Text("Cristobal Castilla", textSize = 16f.dp, textStyle = Typeface.BOLD))
              child(
                  Row(
                      style =
                          Style.margin(start = 8f.dp)
                              .padding(all = 3f.dp)
                              .background(RoundedRect(Color.WHITE, 12.dp)),
                      alignItems = YogaAlign.CENTER) {
                    child(
                        Column(
                            style =
                                Style.height(18f.dp)
                                    .width(18f.dp)
                                    .background(RoundedRect(0xffffb74b, 9.dp))))
                    if (expanded) {
                      child(
                          key("text") {
                            Text(
                                "Top Follower",
                                style =
                                    Style.margin(left = 8f.dp)
                                        .transitionKey(
                                            context,
                                            TRANSITION_KEY_TEXT,
                                            Transition.TransitionKeyType.GLOBAL),
                                textSize = 12f.dp,
                                clipToBounds = true)
                          })
                    }
                    child(
                        Text(
                            "+1",
                            style = Style.margin(left = 8f.dp, right = 4f.dp),
                            textSize = 12f.dp,
                            textColor = Color.BLUE))
                  })
            })
        child(Text("So awesome!", textSize = 18f.dp))
      }

  private fun ResourcesScope.buildComment2(expanded: Boolean): Component =
      Column(style = Style.padding(all = 8f.dp).background(RoundedRect(0xFFDDDDDD, 20.dp))) {
        child(
            Row(alignItems = YogaAlign.CENTER) {
              child(Text("Cristobal Castilla", textSize = 16f.dp, textStyle = Typeface.BOLD))
              child(
                  Row(
                      style =
                          Style.width((if (expanded) 48f else 24f).dp)
                              .margin(start = 8f.dp)
                              .padding(all = 3f.dp)
                              .background(RoundedRect(Color.WHITE, 12.dp)),
                      alignItems = YogaAlign.CENTER) {
                    child(
                        Column(
                            style =
                                Style.positionType(YogaPositionType.ABSOLUTE)
                                    .position(start = (if (expanded) 27 else 3).dp)
                                    .positionType(YogaPositionType.ABSOLUTE)
                                    .height(18f.dp)
                                    .width(18f.dp)
                                    .background(RoundedRect(0xFFB2CFE5, 9.dp))))
                    child(
                        Column(
                            style =
                                Style.positionType(YogaPositionType.ABSOLUTE)
                                    .position(start = (if (expanded) 15 else 3).dp)
                                    .positionType(YogaPositionType.ABSOLUTE)
                                    .height(18f.dp)
                                    .width(18f.dp)
                                    .background(RoundedRect(0xFF4B8C61, 9.dp))))
                    child(
                        Column(
                            style =
                                Style.height(18f.dp)
                                    .width(18f.dp)
                                    .background(RoundedRect(0xFFFFB74B, 9.dp))))
                  })
            })
        child(Text("So awesome!", textSize = 18f.dp))
      }
}
