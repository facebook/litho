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

package com.fblitho.lithoktsample.animations.bounds

import android.graphics.Color
import android.graphics.Typeface
import com.facebook.litho.ClickEvent
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Row
import com.facebook.litho.StateValue
import com.facebook.litho.Transition
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnCreateTransition
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.State
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaJustify

@LayoutSpec
object BoundsAnimationComponentSpec {

  private const val TRANSITION_KEY_CONTAINER_1 = "container_1"
  private const val TRANSITION_KEY_CHILD_1_1 = "child_1_1"
  private const val TRANSITION_KEY_CHILD_1_2 = "child_1_2"
  private const val TRANSITION_KEY_CHILD_1_3 = "child_1_3"
  private const val TRANSITION_KEY_CONTAINER_2 = "container_2"
  private const val TRANSITION_KEY_CHILD_2_1 = "child_2_1"
  private const val TRANSITION_KEY_CHILD_2_2 = "child_2_2"
  private const val TRANSITION_KEY_CONTAINER_3 = "container_3"
  private const val TRANSITION_KEY_CHILD_3_1 = "child_3_1"
  private const val TRANSITION_KEY_CONTAINER_4 = "container_4"
  private const val TRANSITION_KEY_CONTAINER_4_1 = "container_4_1"
  private const val TRANSITION_KEY_CONTAINER_4_2 = "container_4_2"
  private const val TRANSITION_KEY_CHILD_4_1_1 = "child_4_1_1"
  private const val TRANSITION_KEY_CHILD_4_1_2 = "child_4_1_2"
  private const val TRANSITION_KEY_CHILD_4_2_1 = "child_4_2_1"
  private const val TRANSITION_KEY_CHILD_4_2_2 = "child_4_2_2"

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @State autoBoundsTransitionEnabled: Boolean,
      @State flag1: Boolean,
      @State flag2: Boolean,
      @State flag3: Boolean,
      @State flag4: Boolean
  ): Component = Column.create(c)
      .backgroundColor(Color.WHITE)
      .alignItems(YogaAlign.CENTER)
      .paddingDip(YogaEdge.VERTICAL, 8f)
      .child(
          Text.create(c)
              .text("ABT " + if (autoBoundsTransitionEnabled) "enabled" else "disabled")
              .textSizeSp(20f)
              .textStyle(Typeface.BOLD)
              .clickHandler(BoundsAnimationComponent.onABTClick(c)))
      .child(
          Text.create(c).marginDip(YogaEdge.VERTICAL, 8f).text("Affected Children").textSizeSp(20f))
      .child(affectedChildren(c, flag1))
      .child(
          Text.create(c).marginDip(YogaEdge.VERTICAL, 8f).text("Affected Siblings").textSizeSp(20f))
      .child(affectedSiblings(c, flag2))
      .child(
          Text.create(c).marginDip(YogaEdge.VERTICAL, 8f).text("Affected Parent").textSizeSp(20f))
      .child(affectedParent(c, flag3))
      .child(altogether(c, flag4))
      .build()

  @OnEvent(ClickEvent::class)
  fun onABTClick(c: ComponentContext) {
    BoundsAnimationComponent.toggleABT(c)
  }

  @OnUpdateState
  fun toggleABT(autoBoundsTransitionEnabled: StateValue<Boolean>) {
    autoBoundsTransitionEnabled.set(!(autoBoundsTransitionEnabled.get()!!))
  }

  private fun affectedChildren(c: ComponentContext, flag1: Boolean): Component =
      Row.create(c)
          .transitionKey(TRANSITION_KEY_CONTAINER_1)
          .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
          .heightDip((60 + 2 * 8).toFloat())
          .widthDip(3 * 60 * (if (flag1) 0.5f else 1f) + 4 * 8)
          .paddingDip(YogaEdge.ALL, 8f)
          .backgroundColor(Color.YELLOW)
          .child(
              Column.create(c)
                  .transitionKey(TRANSITION_KEY_CHILD_1_1)
                  .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                  .flex(1f)
                  .backgroundColor(Color.RED))
          .child(
              Column.create(c)
                  .transitionKey(TRANSITION_KEY_CHILD_1_2)
                  .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                  .flex(1f)
                  .backgroundColor(Color.RED)
                  .marginDip(YogaEdge.HORIZONTAL, 8f))
          .child(
              Column.create(c)
                  .flex(1f)
                  .transitionKey(TRANSITION_KEY_CHILD_1_3)
                  .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                  .backgroundColor(Color.RED))
          .clickHandler(BoundsAnimationComponent.onFirstComponentClick(c))
          .build()

  @OnEvent(ClickEvent::class)
  fun onFirstComponentClick(c: ComponentContext) {
    BoundsAnimationComponent.toggleFlag1(c)
  }

  @OnUpdateState
  fun toggleFlag1(flag1: StateValue<Boolean>) {
    flag1.set(!(flag1.get()!!))
  }

  private fun affectedSiblings(c: ComponentContext, flag2: Boolean): Component =
      Row.create(c)
          .transitionKey(TRANSITION_KEY_CONTAINER_2)
          .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
          .heightDip((60 + 2 * 8).toFloat())
          .widthDip((3 * 60 + 3 * 8).toFloat())
          .paddingDip(YogaEdge.ALL, 8f)
          .backgroundColor(Color.LTGRAY)
          .child(
              Column.create(c)
                  .transitionKey(TRANSITION_KEY_CHILD_2_1)
                  .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                  .flex(1f)
                  .backgroundColor(Color.RED))
          .child(
              Column.create(c)
                  .transitionKey(TRANSITION_KEY_CHILD_2_2)
                  .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                  .flex((if (flag2) 1 else 2).toFloat())
                  .backgroundColor(Color.YELLOW)
                  .marginDip(YogaEdge.LEFT, 8f))
          .clickHandler(BoundsAnimationComponent.onSecondComponentClick(c))
          .build()

  @OnEvent(ClickEvent::class)
  fun onSecondComponentClick(c: ComponentContext) {
    BoundsAnimationComponent.toggleFlag2(c)
  }

  @OnUpdateState
  fun toggleFlag2(flag2: StateValue<Boolean>) {
    flag2.set(!(flag2.get()!!))
  }

  private fun affectedParent(c: ComponentContext, flag3: Boolean): Component =
      Row.create(c)
          .justifyContent(YogaJustify.CENTER)
          .child(
              Row.create(c)
                  .transitionKey(TRANSITION_KEY_CONTAINER_3)
                  .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                  .heightDip((60 + 2 * 8).toFloat())
                  .paddingDip(YogaEdge.ALL, 8f)
                  .backgroundColor(Color.LTGRAY)
                  .child(
                      Column.create(c)
                          .transitionKey(TRANSITION_KEY_CHILD_3_1)
                          .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                          .widthDip((60 * if (flag3) 1 else 2).toFloat())
                          .backgroundColor(Color.YELLOW))
                  .clickHandler(BoundsAnimationComponent.onThirdComponentClick(c)))
          .build()

  @OnEvent(ClickEvent::class)
  fun onThirdComponentClick(c: ComponentContext) {
    BoundsAnimationComponent.toggleFlag3(c)
  }

  @OnUpdateState
  fun toggleFlag3(flag3: StateValue<Boolean>) {
    flag3.set(!(flag3.get()!!))
  }

  private fun altogether(c: ComponentContext, flag4: Boolean): Component =
      Column.create(c)
          .transitionKey(TRANSITION_KEY_CONTAINER_4)
          .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
          .marginDip(YogaEdge.TOP, 24f)
          .heightDip((60 * 2 + 3 * 8).toFloat())
          .paddingDip(YogaEdge.ALL, 8f)
          .backgroundColor(Color.LTGRAY)
          .child(
              Row.create(c)
                  .transitionKey(TRANSITION_KEY_CONTAINER_4_1)
                  .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                  .heightDip(60f)
                  .paddingDip(YogaEdge.ALL, 6f)
                  .backgroundColor(Color.GRAY)
                  .child(
                      Column.create(c)
                          .transitionKey(TRANSITION_KEY_CHILD_4_1_1)
                          .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                          .marginDip(YogaEdge.RIGHT, 6f)
                          .flex(1f)
                          .backgroundColor(Color.RED))
                  .child(
                      Column.create(c)
                          .transitionKey(TRANSITION_KEY_CHILD_4_1_2)
                          .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                          .flex(1f)
                          .backgroundColor(Color.RED)))
          .child(
              Row.create(c)
                  .transitionKey(TRANSITION_KEY_CONTAINER_4_2)
                  .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                  .heightDip(60f)
                  .marginDip(YogaEdge.TOP, 8f)
                  .paddingDip(YogaEdge.ALL, 6f)
                  .backgroundColor(Color.GRAY)
                  .child(
                      Column.create(c)
                          .transitionKey(TRANSITION_KEY_CHILD_4_2_1)
                          .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                          .marginDip(YogaEdge.RIGHT, 6f)
                          .widthDip(100f)
                          .backgroundColor(Color.RED))
                  .child(
                      Column.create(c)
                          .transitionKey(TRANSITION_KEY_CHILD_4_2_2)
                          .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                          .widthDip((if (flag4) 200 else 100).toFloat())
                          .backgroundColor(Color.YELLOW)))
          .clickHandler(BoundsAnimationComponent.onFourthComponentClick(c))
          .build()

  @OnEvent(ClickEvent::class)
  fun onFourthComponentClick(c: ComponentContext) {
    BoundsAnimationComponent.toggleFlag4(c)
  }

  @OnUpdateState
  fun toggleFlag4(flag4: StateValue<Boolean>) {
    flag4.set(!(flag4.get()!!))
  }

  @OnCreateTransition
  fun animate(c: ComponentContext, @State autoBoundsTransitionEnabled: Boolean): Transition {
    val transitionKeys = if (autoBoundsTransitionEnabled) {
      arrayOf(
          TRANSITION_KEY_CONTAINER_1,
          TRANSITION_KEY_CHILD_1_1,
          TRANSITION_KEY_CHILD_1_2,
          TRANSITION_KEY_CHILD_1_3,
          TRANSITION_KEY_CONTAINER_2,
          TRANSITION_KEY_CHILD_2_1,
          TRANSITION_KEY_CHILD_2_2,
          TRANSITION_KEY_CONTAINER_3,
          TRANSITION_KEY_CHILD_3_1,
          TRANSITION_KEY_CONTAINER_4,
          TRANSITION_KEY_CONTAINER_4_1,
          TRANSITION_KEY_CONTAINER_4_2,
          TRANSITION_KEY_CHILD_4_1_1,
          TRANSITION_KEY_CHILD_4_1_2,
          TRANSITION_KEY_CHILD_4_2_1,
          TRANSITION_KEY_CHILD_4_2_2)
    } else {
      arrayOf(
          TRANSITION_KEY_CONTAINER_1,
          TRANSITION_KEY_CHILD_2_2,
          TRANSITION_KEY_CHILD_3_1,
          TRANSITION_KEY_CHILD_4_2_2)
    }

    return Transition.create(Transition.TransitionKeyType.GLOBAL, *transitionKeys)
        .animate(AnimatedProperties.WIDTH, AnimatedProperties.X)
        .animator(Transition.timing(1000))
  }
}
