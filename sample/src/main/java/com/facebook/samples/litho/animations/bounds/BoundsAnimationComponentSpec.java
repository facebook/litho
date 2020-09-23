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

package com.facebook.samples.litho.animations.bounds;

import android.graphics.Color;
import android.graphics.Typeface;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.Transition;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateTransition;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaJustify;

@LayoutSpec
public class BoundsAnimationComponentSpec {

  private static final String TRANSITION_KEY_CONTAINER_1 = "container_1";
  private static final String TRANSITION_KEY_CHILD_1_1 = "child_1_1";
  private static final String TRANSITION_KEY_CHILD_1_2 = "child_1_2";
  private static final String TRANSITION_KEY_CHILD_1_3 = "child_1_3";
  private static final String TRANSITION_KEY_CONTAINER_2 = "container_2";
  private static final String TRANSITION_KEY_CHILD_2_1 = "child_2_1";
  private static final String TRANSITION_KEY_CHILD_2_2 = "child_2_2";
  private static final String TRANSITION_KEY_CONTAINER_3 = "container_3";
  private static final String TRANSITION_KEY_CHILD_3_1 = "child_3_1";
  private static final String TRANSITION_KEY_CONTAINER_4 = "container_4";
  private static final String TRANSITION_KEY_CONTAINER_4_1 = "container_4_1";
  private static final String TRANSITION_KEY_CONTAINER_4_2 = "container_4_2";
  private static final String TRANSITION_KEY_CHILD_4_1_1 = "child_4_1_1";
  private static final String TRANSITION_KEY_CHILD_4_1_2 = "child_4_1_2";
  private static final String TRANSITION_KEY_CHILD_4_2_1 = "child_4_2_1";
  private static final String TRANSITION_KEY_CHILD_4_2_2 = "child_4_2_2";

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @State boolean autoBoundsTransitionEnabled,
      @State boolean flag1,
      @State boolean flag2,
      @State boolean flag3,
      @State boolean flag4) {
    return Column.create(c)
        .backgroundColor(Color.WHITE)
        .alignItems(YogaAlign.CENTER)
        .paddingDip(YogaEdge.VERTICAL, 8)
        .child(
            Text.create(c)
                .text("ABT " + (autoBoundsTransitionEnabled ? "enabled" : "disabled"))
                .textSizeSp(20)
                .textStyle(Typeface.BOLD)
                .clickHandler(BoundsAnimationComponent.onABTClick(c)))
        .child(
            Text.create(c).marginDip(YogaEdge.VERTICAL, 8).text("Affected Children").textSizeSp(20))
        .child(affectedChildren(c, flag1))
        .child(
            Text.create(c).marginDip(YogaEdge.VERTICAL, 8).text("Affected Siblings").textSizeSp(20))
        .child(affectedSiblings(c, flag2))
        .child(
            Text.create(c).marginDip(YogaEdge.VERTICAL, 8).text("Affected Parent").textSizeSp(20))
        .child(affectedParent(c, flag3))
        .child(altogether(c, flag4))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onABTClick(ComponentContext c) {
    BoundsAnimationComponent.toggleABTSync(c);
  }

  @OnUpdateState
  static void toggleABT(StateValue<Boolean> autoBoundsTransitionEnabled) {
    autoBoundsTransitionEnabled.set(!autoBoundsTransitionEnabled.get());
  }

  private static Component affectedChildren(ComponentContext c, boolean flag1) {
    return Row.create(c)
        .transitionKey(TRANSITION_KEY_CONTAINER_1)
        .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
        .heightDip(60 + 2 * 8)
        .widthDip((3 * 60) * (flag1 ? 0.5f : 1) + 4 * 8)
        .paddingDip(YogaEdge.ALL, 8)
        .backgroundColor(Color.YELLOW)
        .child(
            Column.create(c)
                .transitionKey(TRANSITION_KEY_CHILD_1_1)
                .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                .flex(1)
                .backgroundColor(Color.RED))
        .child(
            Column.create(c)
                .transitionKey(TRANSITION_KEY_CHILD_1_2)
                .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                .flex(1)
                .backgroundColor(Color.RED)
                .marginDip(YogaEdge.HORIZONTAL, 8))
        .child(
            Column.create(c)
                .flex(1)
                .transitionKey(TRANSITION_KEY_CHILD_1_3)
                .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                .backgroundColor(Color.RED))
        .clickHandler(BoundsAnimationComponent.onFirstComponentClick(c))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onFirstComponentClick(ComponentContext c) {
    BoundsAnimationComponent.toggleFlag1Sync(c);
  }

  @OnUpdateState
  static void toggleFlag1(StateValue<Boolean> flag1) {
    flag1.set(!flag1.get());
  }

  private static Component affectedSiblings(ComponentContext c, boolean flag2) {
    return Row.create(c)
        .transitionKey(TRANSITION_KEY_CONTAINER_2)
        .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
        .heightDip(60 + 2 * 8)
        .widthDip(3 * 60 + 3 * 8)
        .paddingDip(YogaEdge.ALL, 8)
        .backgroundColor(Color.LTGRAY)
        .child(
            Column.create(c)
                .transitionKey(TRANSITION_KEY_CHILD_2_1)
                .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                .flex(1)
                .backgroundColor(Color.RED))
        .child(
            Column.create(c)
                .transitionKey(TRANSITION_KEY_CHILD_2_2)
                .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                .flex(flag2 ? 1 : 2)
                .backgroundColor(Color.YELLOW)
                .marginDip(YogaEdge.LEFT, 8))
        .clickHandler(BoundsAnimationComponent.onSecondComponentClick(c))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onSecondComponentClick(ComponentContext c) {
    BoundsAnimationComponent.toggleFlag2Sync(c);
  }

  @OnUpdateState
  static void toggleFlag2(StateValue<Boolean> flag2) {
    flag2.set(!flag2.get());
  }

  private static Component affectedParent(ComponentContext c, boolean flag3) {
    return Row.create(c)
        .justifyContent(YogaJustify.CENTER)
        .child(
            Row.create(c)
                .transitionKey(TRANSITION_KEY_CONTAINER_3)
                .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                .heightDip(60 + 2 * 8)
                .paddingDip(YogaEdge.ALL, 8)
                .backgroundColor(Color.LTGRAY)
                .child(
                    Column.create(c)
                        .transitionKey(TRANSITION_KEY_CHILD_3_1)
                        .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                        .widthDip(60 * (flag3 ? 1 : 2))
                        .backgroundColor(Color.YELLOW))
                .clickHandler(BoundsAnimationComponent.onThirdComponentClick(c)))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onThirdComponentClick(ComponentContext c) {
    BoundsAnimationComponent.toggleFlag3Sync(c);
  }

  @OnUpdateState
  static void toggleFlag3(StateValue<Boolean> flag3) {
    flag3.set(!flag3.get());
  }

  private static Component altogether(ComponentContext c, boolean flag4) {
    return Column.create(c)
        .transitionKey(TRANSITION_KEY_CONTAINER_4)
        .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
        .marginDip(YogaEdge.TOP, 24)
        .heightDip(60 * 2 + 3 * 8)
        .paddingDip(YogaEdge.ALL, 8)
        .backgroundColor(Color.LTGRAY)
        .child(
            Row.create(c)
                .transitionKey(TRANSITION_KEY_CONTAINER_4_1)
                .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                .heightDip(60)
                .paddingDip(YogaEdge.ALL, 6)
                .backgroundColor(Color.GRAY)
                .child(
                    Column.create(c)
                        .transitionKey(TRANSITION_KEY_CHILD_4_1_1)
                        .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                        .marginDip(YogaEdge.RIGHT, 6)
                        .flex(1)
                        .backgroundColor(Color.RED))
                .child(
                    Column.create(c)
                        .transitionKey(TRANSITION_KEY_CHILD_4_1_2)
                        .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                        .flex(1)
                        .backgroundColor(Color.RED)))
        .child(
            Row.create(c)
                .transitionKey(TRANSITION_KEY_CONTAINER_4_2)
                .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                .heightDip(60)
                .marginDip(YogaEdge.TOP, 8)
                .paddingDip(YogaEdge.ALL, 6)
                .backgroundColor(Color.GRAY)
                .child(
                    Column.create(c)
                        .transitionKey(TRANSITION_KEY_CHILD_4_2_1)
                        .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                        .marginDip(YogaEdge.RIGHT, 6)
                        .widthDip(100)
                        .backgroundColor(Color.RED))
                .child(
                    Column.create(c)
                        .transitionKey(TRANSITION_KEY_CHILD_4_2_2)
                        .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                        .widthDip(flag4 ? 200 : 100)
                        .backgroundColor(Color.YELLOW)))
        .clickHandler(BoundsAnimationComponent.onFourthComponentClick(c))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onFourthComponentClick(ComponentContext c) {
    BoundsAnimationComponent.toggleFlag4Sync(c);
  }

  @OnUpdateState
  static void toggleFlag4(StateValue<Boolean> flag4) {
    flag4.set(!flag4.get());
  }

  @OnCreateTransition
  static Transition createTransition(ComponentContext c, @State boolean autoBoundsTransitionEnabled) {
    String[] transitionKeys =
        autoBoundsTransitionEnabled
            ? new String[] {
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
              TRANSITION_KEY_CHILD_4_2_2,
            }
            : new String[] {
              TRANSITION_KEY_CONTAINER_1,
              TRANSITION_KEY_CHILD_2_2,
              TRANSITION_KEY_CHILD_3_1,
              TRANSITION_KEY_CHILD_4_2_2
            };
    return Transition.create(Transition.TransitionKeyType.GLOBAL, transitionKeys)
        .animate(AnimatedProperties.WIDTH, AnimatedProperties.X)
        .animator(Transition.timing(1000));
  }
}
