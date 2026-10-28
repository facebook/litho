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
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Style
import com.facebook.litho.animation.FiniteAnimationSpec
import com.facebook.litho.animation.transition
import com.facebook.litho.animation.tween
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.alignSelf
import com.facebook.litho.flexbox.position
import com.facebook.litho.flexbox.positionType
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.useState
import com.facebook.litho.view.alpha
import com.facebook.litho.view.background
import com.facebook.litho.view.clipChildren
import com.facebook.litho.view.onClick
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.dp
import com.facebook.rendercore.ms
import com.facebook.samples.litho.kotlin.drawable.RoundedRect
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaPositionType

private val badgeMotion: FiniteAnimationSpec<Float> = tween(300.ms)

/**
 * Two comment badges that grow when tapped. The first reveals a "Top Follower" label; the second
 * widens and slides its avatars apart. Every node that moves animates its own `x` or `width` with
 * [Style.transition], so nothing relayouts while the badges animate. Tap to step through the four
 * combinations.
 */
@OptIn(ExperimentalLithoApi::class)
class AnimatedBadgeKotlin : KComponent() {

  override fun ComponentScope.render(): Component {
    val state = useState { 0 }
    val expanded1 = state.value == 1 || state.value == 2
    val expanded2 = state.value == 2 || state.value == 3

    return Column(style = Style.padding(all = 8.dp).onClick { state.update { (it + 1) % 4 } }) {
      child(Row(style = Style.margin(top = 8.dp)) { child(buildComment1(expanded1)) })
      child(Row(style = Style.margin(top = 16.dp)) { child(buildComment2(expanded2)) })
    }
  }

  private fun ResourcesScope.buildComment1(expanded: Boolean): Component =
      Column(style = bubbleStyle()) {
        child(
            Row(alignItems = YogaAlign.CENTER) {
              child(Text("Cristobal Castilla", textSize = 16.dp, textStyle = Typeface.BOLD))
              child(
                  Row(
                      style =
                          Style.margin(start = 8.dp)
                              .padding(all = 3.dp)
                              .background(RoundedRect(Color.WHITE, 12.dp))
                              .transition { width = badgeMotion },
                      alignItems = YogaAlign.CENTER,
                  ) {
                    child(
                        Column(
                            style =
                                Style.height(18.dp)
                                    .width(18.dp)
                                    .background(RoundedRect(0xffffb74b, 9.dp)),
                        ),
                    )
                    // While collapsed, the label is absolute so it keeps its one-line width and
                    // the zero-width wrapper clips it.
                    child(
                        Row(
                            alignItems = YogaAlign.CENTER,
                            style =
                                (if (expanded) Style.alpha(1f) else Style.width(0.dp).alpha(0f))
                                    .alignSelf(YogaAlign.STRETCH)
                                    .clipChildren(true)
                                    .transition {
                                      width = badgeMotion
                                      alpha = badgeMotion
                                    },
                        ) {
                          child(
                              Text(
                                  "Top Follower",
                                  style =
                                      Style.margin(left = 8.dp).plusIf(!expanded) {
                                        Style.positionType(YogaPositionType.ABSOLUTE)
                                      },
                                  textSize = 12.dp,
                              ),
                          )
                        },
                    )
                    child(
                        Text(
                            "+1",
                            style =
                                Style.margin(left = 8.dp, right = 4.dp).transition {
                                  x = badgeMotion
                                },
                            textSize = 12.dp,
                            textColor = Color.BLUE,
                        ),
                    )
                  },
              )
            },
        )
        child(Text("So awesome!", textSize = 18.dp))
      }

  private fun ResourcesScope.buildComment2(expanded: Boolean): Component =
      Column(style = bubbleStyle()) {
        child(
            Row(alignItems = YogaAlign.CENTER) {
              child(Text("Cristobal Castilla", textSize = 16.dp, textStyle = Typeface.BOLD))
              child(
                  Row(
                      style =
                          Style.width(if (expanded) 48.dp else 24.dp)
                              .margin(start = 8.dp)
                              .padding(all = 3.dp)
                              .background(RoundedRect(Color.WHITE, 12.dp))
                              .transition { width = badgeMotion },
                      alignItems = YogaAlign.CENTER,
                  ) {
                    child(avatar(start = if (expanded) 27.dp else 3.dp, color = 0xFFB2CFE5))
                    child(avatar(start = if (expanded) 15.dp else 3.dp, color = 0xFF4B8C61))
                    child(
                        Column(
                            style =
                                Style.height(18.dp)
                                    .width(18.dp)
                                    .background(RoundedRect(0xFFFFB74B, 9.dp)),
                        ),
                    )
                  },
              )
            },
        )
        child(Text("So awesome!", textSize = 18.dp))
      }

  private fun ResourcesScope.bubbleStyle(): Style =
      Style.padding(all = 8.dp).background(RoundedRect(0xFFDDDDDD, 20.dp)).transition {
        width = badgeMotion
      }

  private fun ResourcesScope.avatar(start: Dimen, color: Long): Component = Column(
      style =
          Style.positionType(YogaPositionType.ABSOLUTE)
              .position(start = start)
              .height(18.dp)
              .width(18.dp)
              .background(RoundedRect(color, 9.dp))
              .transition { x = badgeMotion },
  )
}
