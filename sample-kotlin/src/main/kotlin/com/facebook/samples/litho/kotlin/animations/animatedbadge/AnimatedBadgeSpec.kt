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

package com.facebook.samples.litho.kotlin.animations.animatedbadge

import android.graphics.Color
import android.graphics.Typeface
import com.facebook.litho.ClickEvent
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Row
import com.facebook.litho.StateValue
import com.facebook.litho.Transition
import com.facebook.litho.Transition.BaseTransitionUnitsBuilder
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnCreateTransition
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.State
import com.facebook.litho.widget.Text
import com.facebook.samples.litho.kotlin.drawable.RoundedRect
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaPositionType

@LayoutSpec
object AnimatedBadgeSpec {

  private const val ANIMATION_DURATION = 300
  private val ANIMATOR = Transition.timing(ANIMATION_DURATION)

  private const val TRANSITION_KEY_TEXT = "text"

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext, @State state: Int): Component {
    val expanded1 = state == 1 || state == 2
    val expanded2 = state == 2 || state == 3
    return Column.create(c)
        .paddingDip(YogaEdge.ALL, 8f)
        .child(Row.create(c).marginDip(YogaEdge.TOP, 8f).child(buildComment1(c, expanded1)))
        .child(Row.create(c).marginDip(YogaEdge.TOP, 16f).child(buildComment2(c, expanded2)))
        .clickHandler(AnimatedBadge.onClick(c))
        .build()
  }

  private fun buildComment1(c: ComponentContext, expanded: Boolean): Component =
      Column.create(c)
          .paddingDip(YogaEdge.ALL, 8f)
          .child(
              Row.create(c)
                  .alignItems(YogaAlign.CENTER)
                  .child(
                      Text.create(c)
                          .textSizeSp(16f)
                          .textStyle(Typeface.BOLD)
                          .text("Cristobal Castilla"))
                  .child(
                      Row.create(c)
                          .marginDip(YogaEdge.LEFT, 8f)
                          .paddingDip(YogaEdge.ALL, 3f)
                          .alignItems(YogaAlign.CENTER)
                          .child(
                              Column.create(c)
                                  .heightDip(18f)
                                  .widthDip(18f)
                                  .background(RoundedRect.build(c, -0x48b5, 9)))
                          .child(
                              if (!expanded) {
                                null
                              } else {
                                Text.create(c)
                                    // still need transition keys for appear/disappear animations
                                    .transitionKey(TRANSITION_KEY_TEXT)
                                    .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                                    // need this to prevent the global key of "+1" Text from
                                    // changing
                                    .key("text")
                                    .marginDip(YogaEdge.LEFT, 8f)
                                    .clipToBounds(true)
                                    .textSizeDip(12f)
                                    .text("Top Follower")
                              })
                          .child(
                              Text.create(c)
                                  .marginDip(YogaEdge.LEFT, 8f)
                                  .marginDip(YogaEdge.RIGHT, 4f)
                                  .textSizeDip(12f)
                                  .textColor(Color.BLUE)
                                  .text("+1"))
                          .background(RoundedRect.build(c, Color.WHITE, 12))))
          .child(Text.create(c).textSizeSp(18f).text("So awesome!"))
          .background(RoundedRect.build(c, -0x222223, 20))
          .build()

  private fun buildComment2(c: ComponentContext, expanded: Boolean): Component =
      Column.create(c)
          .paddingDip(YogaEdge.ALL, 8f)
          .child(
              Row.create(c)
                  .alignItems(YogaAlign.CENTER)
                  .child(
                      Text.create(c)
                          .textSizeSp(16f)
                          .textStyle(Typeface.BOLD)
                          .text("Cristobal Castilla"))
                  .child(
                      Row.create(c)
                          .widthDip((if (expanded) 48 else 24).toFloat())
                          .marginDip(YogaEdge.LEFT, 8f)
                          .paddingDip(YogaEdge.ALL, 3f)
                          .alignItems(YogaAlign.CENTER)
                          .child(
                              Column.create(c)
                                  .positionType(YogaPositionType.ABSOLUTE)
                                  .positionDip(YogaEdge.LEFT, (if (expanded) 27 else 3).toFloat())
                                  .heightDip(18f)
                                  .widthDip(18f)
                                  .background(RoundedRect.build(c, 0xFFB2CFE5.toInt(), 9)))
                          .child(
                              Column.create(c)
                                  .positionType(YogaPositionType.ABSOLUTE)
                                  .positionDip(YogaEdge.LEFT, (if (expanded) 15 else 3).toFloat())
                                  .heightDip(18f)
                                  .widthDip(18f)
                                  .background(RoundedRect.build(c, 0xFF4B8C61.toInt(), 9)))
                          .child(
                              Column.create(c)
                                  .heightDip(18f)
                                  .widthDip(18f)
                                  .background(RoundedRect.build(c, 0xFFFFB74B.toInt(), 9)))
                          .background(RoundedRect.build(c, Color.WHITE, 12))))
          .child(Text.create(c).textSizeSp(18f).text("So awesome!"))
          .background(RoundedRect.build(c, 0xFFDDDDDD.toInt(), 20))
          .build()

  @OnEvent(ClickEvent::class)
  fun onClick(c: ComponentContext) {
    AnimatedBadge.updateState(c)
  }

  @OnUpdateState
  fun updateState(state: StateValue<Int>) {
    state.set((state.get()!! + 1) % 4)
  }

  @OnCreateTransition
  fun onCreateTransition(c: ComponentContext): Transition =
      Transition.parallel<BaseTransitionUnitsBuilder>(
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
              .animator(ANIMATOR))
}
