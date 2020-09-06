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

package com.facebook.litho.animation;

import static org.assertj.core.api.Java6Assertions.assertThat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.LithoView;
import com.facebook.litho.Row;
import com.facebook.litho.StateCaller;
import com.facebook.litho.Transition;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.dataflow.MockTimingSource;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.TransitionTestRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.TestAnimationMount;
import com.facebook.litho.widget.TestAnimationsComponent;
import com.facebook.litho.widget.TestAnimationsComponentSpec;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaJustify;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;

/**
 * This tests validate how different kind of animations modify the view. The values asserted here
 * are specific to the number of frames and the type of animation.
 *
 * <p>All transitions are timed to 144 ms which translates to 9 frames (we actually step 10 because
 * the first one does not count as it only initializes the values in the {@link
 * com.facebook.litho.dataflow.TimingNode}) based on {@link MockTimingSource}.FRAME_TIME_MS) which
 * is 16ms per frame.
 *
 * <p>Formula for the specific values found in this tests:
 *
 * <p>- First we calculate the timing doing: timing = (frames*frame_time - frame_time) /
 * ((frame_time + duration_ms) - frame_time)
 *
 * <p>- Then we run the AccelerateDecelerateInterpolator: fraction = cos((timing + 1) * PI / 2) +
 * 0.5 f
 *
 * <p>- Finally the actual result is: result = initial_position + fraction * (final_position -
 * initial_position)
 *
 * <p>Example for X axis animation after 5 frames: (5*16 - 16) / ((16 + 144) - 16) = 0.44444445.
 *
 * <p>AccelerateDecelareteInterpolator: cos((0.44444445 + 1) * PI / 2) + * 0.5 f = 0.4131759
 *
 * <p>Final position: 160 + 0.4131759 * (-160) = 93.891856
 */
@SuppressLint("ColorConstantUsageIssue")
@RunWith(LithoTestRunner.class)
public class AnimationTest {
  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();
  public final @Rule TransitionTestRule mTransitionTestRule = new TransitionTestRule();
  private static final String TRANSITION_KEY = "TRANSITION_KEY";
  private final StateCaller mStateCaller = new StateCaller();
  private ActivityController<Activity> mActivityController;

  @Before
  public void setUp() {
    mActivityController = Robolectric.buildActivity(Activity.class, new Intent());
  }

  @Test
  public void animationProperties_animatingPropertyX_elementShouldAnimateInTheXAxis() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.X))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Row.create(componentContext)
                        .heightDip(200)
                        .widthDip(200)
                        .justifyContent(state ? YogaJustify.FLEX_START : YogaJustify.FLEX_END)
                        .alignItems(state ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
                        .child(
                            Row.create(componentContext)
                                .heightDip(40)
                                .widthDip(40)
                                .backgroundColor(Color.parseColor("#ee1111"))
                                .transitionKey(TRANSITION_KEY)
                                .viewTag(TRANSITION_KEY)
                                .build())
                        .build();
                  }
                })
            .build();
    mLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    View view = mLithoViewRule.findViewWithTag(TRANSITION_KEY);

    // 160 is equal to height and width of 200 - 40 for the size of the row.
    assertThat(view.getX()).describedAs("view X axis should be at start position").isEqualTo(160);
    assertThat(view.getY()).describedAs("view Y axis should be at start position").isEqualTo(160);

    mStateCaller.update();

    // X after state update should be at 160 because is going to be animated.
    assertThat(view.getX()).describedAs("view X axis after toggle").isEqualTo(160);
    // Y moves without animating
    assertThat(view.getY()).describedAs("view Y axis after toggle").isEqualTo(0);

    mTransitionTestRule.step(5);

    // Check java doc for how we calculate this value.
    assertThat(view.getX()).describedAs("view X axis after 5 frames").isEqualTo(93.89186f);
    assertThat(view.getY()).describedAs("view Y axis after 5 frames").isEqualTo(0);

    mTransitionTestRule.step(5);

    assertThat(view.getX()).describedAs("view X axis after 10 frames").isEqualTo(0);
    assertThat(view.getY()).describedAs("view Y axis after 10 frames").isEqualTo(0);
  }

  @Test
  public void animationProperties_animatingPropertyY_elementShouldAnimateInTheYAxis() {

    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.Y))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Row.create(componentContext)
                        .heightDip(200)
                        .widthDip(200)
                        .justifyContent(state ? YogaJustify.FLEX_START : YogaJustify.FLEX_END)
                        .alignItems(state ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
                        .child(
                            Row.create(componentContext)
                                .heightDip(40)
                                .widthDip(40)
                                .backgroundColor(Color.parseColor("#ee1111"))
                                .transitionKey(TRANSITION_KEY)
                                .viewTag(TRANSITION_KEY)
                                .build())
                        .build();
                  }
                })
            .build();
    mLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    View view = mLithoViewRule.findViewWithTag(TRANSITION_KEY);

    assertThat(view.getX()).describedAs("view X axis should be at start position").isEqualTo(160);
    assertThat(view.getY()).describedAs("view Y axis should be at start position").isEqualTo(160);

    mStateCaller.update();

    // X moves without animating
    assertThat(view.getX()).describedAs("view X axis after toggle").isEqualTo(0);
    // Y after state update should be at 160 because is going to be animated
    assertThat(view.getY()).describedAs("view Y axis after toggle").isEqualTo(160);

    mTransitionTestRule.step(5);

    assertThat(view.getX()).describedAs("view X axis after 5 frames").isEqualTo(0);
    // Check java doc for how we calculate this value.
    assertThat(view.getY()).describedAs("view Y axis after 5 frames").isEqualTo(93.89186f);

    mTransitionTestRule.step(5);

    assertThat(view.getX()).describedAs("view X axis after 10 frames").isEqualTo(0);
    assertThat(view.getY()).describedAs("view Y axis after 10 frames").isEqualTo(0);
  }

  @Test
  public void animationProperties_animatingPropertyScale_elementShouldAnimateXandYScale() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.SCALE))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Row.create(componentContext)
                        .child(
                            Row.create(componentContext)
                                .heightDip(40)
                                .widthDip(40)
                                .scale(state ? 1 : 2)
                                .backgroundColor(Color.parseColor("#ee1111"))
                                .transitionKey(TRANSITION_KEY)
                                .viewTag(TRANSITION_KEY)
                                .build())
                        .build();
                  }
                })
            .build();
    mLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    View view = mLithoViewRule.findViewWithTag(TRANSITION_KEY);

    assertThat(view.getScaleX()).describedAs("view scale X initial position").isEqualTo(2);
    assertThat(view.getScaleY()).describedAs("view scale Y initial position").isEqualTo(2);

    mStateCaller.update();

    assertThat(view.getScaleX()).describedAs("view X axis after toggle").isEqualTo(2);
    assertThat(view.getScaleY()).describedAs("view Y axis after toggle").isEqualTo(2);

    mTransitionTestRule.step(5);

    // Check java doc for how we calculate this value.
    assertThat(view.getScaleX()).describedAs("view X axis after 5 frames").isEqualTo(1.5868242f);
    assertThat(view.getScaleY()).describedAs("view Y axis after 5 frames").isEqualTo(1.5868242f);

    mTransitionTestRule.step(5);

    assertThat(view.getScaleX()).describedAs("view X axis after 10 frames").isEqualTo(1);
    assertThat(view.getScaleY()).describedAs("view Y axis after 10 frames").isEqualTo(1);
  }

  @Test
  public void animationProperties_animatingPropertyAlpha_elementShouldAnimateAlpha() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.ALPHA))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Row.create(componentContext)
                        .child(
                            Row.create(componentContext)
                                .heightDip(40)
                                .widthDip(40)
                                .alpha(state ? 1 : 0.5f)
                                .backgroundColor(Color.parseColor("#ee1111"))
                                .transitionKey(TRANSITION_KEY)
                                .viewTag(TRANSITION_KEY)
                                .build())
                        .build();
                  }
                })
            .build();
    mLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    View view = mLithoViewRule.findViewWithTag(TRANSITION_KEY);

    assertThat(view.getAlpha()).describedAs("view alpha initial state").isEqualTo(0.5f);

    mStateCaller.update();

    assertThat(view.getAlpha()).describedAs("view alpha after toggle").isEqualTo(0.5f);

    mTransitionTestRule.step(5);

    // Check java doc for how we calculate this value.
    assertThat(view.getAlpha()).describedAs("view alpha after 5 frames").isEqualTo(0.7065879f);

    mTransitionTestRule.step(5);

    assertThat(view.getAlpha()).describedAs("view alpha after 10 frames").isEqualTo(1);
  }

  @Test
  public void animationProperties_animatingPropertyRotation_elementShouldAnimateRotation() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.ROTATION))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Row.create(componentContext)
                        .child(
                            Row.create(componentContext)
                                .heightDip(40)
                                .widthDip(40)
                                .rotation(state ? 45 : 0)
                                .backgroundColor(Color.parseColor("#ee1111"))
                                .transitionKey(TRANSITION_KEY)
                                .viewTag(TRANSITION_KEY)
                                .build())
                        .build();
                  }
                })
            .build();
    mLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    View view = mLithoViewRule.findViewWithTag(TRANSITION_KEY);

    assertThat(view.getRotation()).describedAs("view rotation initial state").isEqualTo(0);

    mStateCaller.update();

    assertThat(view.getRotation()).describedAs("view rotation after toggle").isEqualTo(0);

    mTransitionTestRule.step(5);

    // Check java doc for how we calculate this value.
    assertThat(view.getRotation())
        .describedAs("view rotation after 5 frames")
        .isEqualTo(18.592915f);

    mTransitionTestRule.step(5);

    assertThat(view.getRotation()).describedAs("view rotation after 10 frames").isEqualTo(45);
  }

  @Test
  public void animationProperties_animatingPropertyHeight_elementShouldAnimateHeight() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.HEIGHT))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Row.create(componentContext)
                        .heightDip(200)
                        .widthDip(200)
                        .child(
                            Row.create(componentContext)
                                .heightDip(state ? 80 : 40)
                                .widthDip(state ? 80 : 40)
                                .backgroundColor(Color.parseColor("#ee1111"))
                                .transitionKey(TRANSITION_KEY)
                                .viewTag(TRANSITION_KEY)
                                .build())
                        .build();
                  }
                })
            .build();
    mLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    View view = mLithoViewRule.findViewWithTag(TRANSITION_KEY);
    assertThat(view.getHeight()).describedAs("view height initial state").isEqualTo(40);
    assertThat(view.getWidth()).describedAs("view width initial state").isEqualTo(40);

    mStateCaller.update();

    assertThat(view.getHeight()).describedAs("view height after toggle").isEqualTo(40);
    assertThat(view.getWidth()).describedAs("view width after toggle").isEqualTo(80);

    mTransitionTestRule.step(5);

    // Check java doc for how we calculate this value.
    assertThat(view.getHeight()).describedAs("view height after 5 frames").isEqualTo(56);

    mTransitionTestRule.step(5);

    assertThat(view.getHeight()).describedAs("view height after 10 frames").isEqualTo(80);
    assertThat(view.getWidth()).describedAs("view width after 10 frames").isEqualTo(80);
  }

  @Test
  public void animationProperties_animatingPropertyWidth_elementShouldAnimateWidth() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.WIDTH))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Row.create(componentContext)
                        .heightDip(200)
                        .widthDip(200)
                        .child(
                            Row.create(componentContext)
                                .heightDip(state ? 80 : 40)
                                .widthDip(state ? 80 : 40)
                                .backgroundColor(Color.parseColor("#ee1111"))
                                .transitionKey(TRANSITION_KEY)
                                .viewTag(TRANSITION_KEY)
                                .build())
                        .build();
                  }
                })
            .build();
    mLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    View view = mLithoViewRule.findViewWithTag(TRANSITION_KEY);
    assertThat(view.getHeight()).describedAs("view height initial state").isEqualTo(40);
    assertThat(view.getWidth()).describedAs("view width initial state").isEqualTo(40);

    mStateCaller.update();

    assertThat(view.getHeight()).describedAs("view height after toggle").isEqualTo(80);
    assertThat(view.getWidth()).describedAs("view width after toggle").isEqualTo(40);

    mTransitionTestRule.step(5);

    // Check java doc for how we calculate this value.
    assertThat(view.getWidth()).describedAs("view width after 5 frames").isEqualTo(56);

    mTransitionTestRule.step(5);

    assertThat(view.getHeight()).describedAs("view height after 10 frames").isEqualTo(80);
    assertThat(view.getWidth()).describedAs("view width after 10 frames").isEqualTo(80);
  }

  @Test
  public void animation_appearAnimation_elementShouldAppearAnimatingAlpha() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.ALPHA)
                    .appearFrom(0)
                    .disappearTo(0))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Column.create(componentContext)
                        .child(
                            Row.create(componentContext)
                                .heightDip(50)
                                .widthDip(50)
                                .backgroundColor(Color.YELLOW))
                        .child(
                            state
                                ? Row.create(componentContext)
                                    .heightDip(50)
                                    .widthDip(50)
                                    .backgroundColor(Color.RED)
                                    .viewTag(TRANSITION_KEY)
                                    .transitionKey(TRANSITION_KEY)
                                    .key(TRANSITION_KEY)
                                : null)
                        .build();
                  }
                })
            .build();
    mLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    View view = mLithoViewRule.findViewWithTag(TRANSITION_KEY);

    // View should be null as state is null
    assertThat(view).describedAs("view before appearing").isNull();
    mStateCaller.update();

    // After state update we should have the view added but with alpha equal to 0
    view = mLithoViewRule.findViewWithTag(TRANSITION_KEY);
    assertThat(view).describedAs("view after toggle").isNotNull();
    assertThat(view.getAlpha()).describedAs("view after toggle").isEqualTo(0);

    mTransitionTestRule.step(5);

    // Check java doc for how we calculate this value.
    assertThat(view.getAlpha()).describedAs("view after 5 frames").isEqualTo(0.41317588f);

    mTransitionTestRule.step(5);
    assertThat(view.getAlpha()).describedAs("view after 10 frames").isEqualTo(1);
  }

  @Test
  public void animation_disappearAnimation_elementShouldDisappearAnimatingAlpha() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.ALPHA)
                    .appearFrom(0)
                    .disappearTo(0))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Column.create(componentContext)
                        .child(
                            Row.create(componentContext)
                                .heightDip(50)
                                .widthDip(50)
                                .backgroundColor(Color.YELLOW))
                        .child(
                            !state
                                ? Row.create(componentContext)
                                    .heightDip(50)
                                    .widthDip(50)
                                    .backgroundColor(Color.RED)
                                    .viewTag(TRANSITION_KEY)
                                    .transitionKey(TRANSITION_KEY)
                                    .key(TRANSITION_KEY)
                                : null)
                        .build();
                  }
                })
            .build();
    mLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    // We move 10 frames to account for the appear animation.
    mTransitionTestRule.step(10);
    View view = mLithoViewRule.findViewWithTag(TRANSITION_KEY);
    // The view is not null
    assertThat(view).describedAs("view before disappearing").isNotNull();
    assertThat(view.getAlpha()).describedAs("view before disappearing").isEqualTo(1);
    mStateCaller.update();
    view = mLithoViewRule.findViewWithTag(TRANSITION_KEY);

    // After state update, even if the row was removed from the component, it still not null as we
    // are going to animate it. Alpha stays at 1 before advancing frames.
    assertThat(view).describedAs("view after toggle").isNotNull();
    assertThat(view.getAlpha()).describedAs("view after toggle").isEqualTo(1);

    mTransitionTestRule.step(5);
    // Check java doc for how we calculate this value.
    assertThat(view.getAlpha()).describedAs("view after 5 frames").isEqualTo(0.5868241f);

    // We move only 4 more frames because after 5 the view should be removed from the hierarchy.
    mTransitionTestRule.step(4);
    // Check java doc for how we calculate this value.
    assertThat(view.getAlpha()).describedAs("view after 10 frames").isEqualTo(0.030153751f);
    mTransitionTestRule.step(1);

    view = mLithoViewRule.findViewWithTag(TRANSITION_KEY);
    assertThat(view).describedAs("view after last re-measure and re-layout").isNull();
  }

  @Test
  public void
      animation_disappearAnimationWithRemountToRoot_elementShouldDisappearWithoutCrashing() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.parallel(
                    Transition.create("comment_editText")
                        .animate(AnimatedProperties.ALPHA)
                        .appearFrom(0)
                        .disappearTo(0)
                        .animate(AnimatedProperties.X)
                        .appearFrom(DimensionValue.widthPercentageOffset(-50))
                        .disappearTo(DimensionValue.widthPercentageOffset(-50)),
                    Transition.create("cont_comment")
                        .animate(AnimatedProperties.ALPHA)
                        .appearFrom(0)
                        .disappearTo(0),
                    Transition.create("icon_like", "icon_share").animate(AnimatedProperties.X),
                    Transition.create("text_like", "text_share")
                        .animate(AnimatedProperties.ALPHA)
                        .appearFrom(0)
                        .disappearTo(0)
                        .animate(AnimatedProperties.X)
                        .appearFrom(DimensionValue.widthPercentageOffset(50))
                        .disappearTo(DimensionValue.widthPercentageOffset(50))))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext c, boolean state) {
                    return !state
                        ? Row.create(c)
                            .backgroundColor(Color.WHITE)
                            .heightDip(56)
                            .child(
                                Row.create(c)
                                    .widthPercent(33.3f)
                                    .alignItems(YogaAlign.CENTER)
                                    .justifyContent(YogaJustify.CENTER)
                                    .wrapInView()
                                    .testKey("like_button")
                                    .child(
                                        Column.create(c)
                                            .heightDip(24)
                                            .widthDip(24)
                                            .backgroundColor(Color.RED)
                                            .transitionKey("icon_like"))
                                    .child(
                                        Text.create(c)
                                            .textSizeSp(16)
                                            .text("Like")
                                            .transitionKey("text_like")
                                            .marginDip(YogaEdge.LEFT, 8)))
                            .child(
                                Row.create(c)
                                    .transitionKey("cont_comment")
                                    .widthPercent(33.3f)
                                    .alignItems(YogaAlign.CENTER)
                                    .justifyContent(YogaJustify.CENTER)
                                    .child(
                                        Column.create(c)
                                            .heightDip(24)
                                            .widthDip(24)
                                            .backgroundColor(Color.RED))
                                    .child(
                                        Text.create(c)
                                            .textSizeSp(16)
                                            .text("Comment")
                                            .marginDip(YogaEdge.LEFT, 8)))
                            .child(
                                Row.create(c)
                                    .widthPercent(33.3f)
                                    .alignItems(YogaAlign.CENTER)
                                    .justifyContent(YogaJustify.CENTER)
                                    .child(
                                        Column.create(c)
                                            .transitionKey("icon_share")
                                            .heightDip(24)
                                            .widthDip(24)
                                            .backgroundColor(Color.RED))
                                    .child(
                                        Text.create(c)
                                            .textSizeSp(16)
                                            .text("Share")
                                            .transitionKey("text_share")
                                            .marginDip(YogaEdge.LEFT, 8)))
                            .build()
                        : Row.create(c)
                            .backgroundColor(Color.WHITE)
                            .heightDip(56)
                            .child(
                                Row.create(c)
                                    .alignItems(YogaAlign.CENTER)
                                    .justifyContent(YogaJustify.CENTER)
                                    .wrapInView()
                                    .paddingDip(YogaEdge.HORIZONTAL, 16)
                                    .testKey("like_button")
                                    .child(
                                        Column.create(c)
                                            .transitionKey("icon_like")
                                            .heightDip(24)
                                            .widthDip(24)
                                            .backgroundColor(Color.RED)))
                            .child(
                                Column.create(c)
                                    .flexGrow(1)
                                    .transitionKey("comment_editText")
                                    .child(Text.create(c).text("Input here").textSizeSp(16)))
                            .child(
                                Row.create(c)
                                    .transitionKey("cont_share")
                                    .alignItems(YogaAlign.CENTER)
                                    .wrapInView()
                                    .paddingDip(YogaEdge.ALL, 16)
                                    .backgroundColor(0xff0000ff)
                                    .child(
                                        Column.create(c)
                                            .transitionKey("icon_share")
                                            .heightDip(24)
                                            .widthDip(24)
                                            .backgroundColor(Color.RED)))
                            .build();
                  }
                })
            .build();
    mLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    // We move 100 frames to be sure any appearing animation finished.
    mTransitionTestRule.step(100);

    mStateCaller.update();
    // We move an other 100 frames to be sure disappearing animations are done.
    mTransitionTestRule.step(100);

    mTransitionTestRule.step(100);

    // Do not crash.
  }

  @Test
  public void animationProperties_differentInterpolator_elementShouldAnimateInTheXAxis() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144, new AccelerateInterpolator()))
                    .animate(AnimatedProperties.X))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Row.create(componentContext)
                        .heightDip(200)
                        .widthDip(200)
                        .justifyContent(state ? YogaJustify.FLEX_START : YogaJustify.FLEX_END)
                        .alignItems(state ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
                        .child(
                            Row.create(componentContext)
                                .heightDip(40)
                                .widthDip(40)
                                .backgroundColor(Color.parseColor("#ee1111"))
                                .transitionKey(TRANSITION_KEY)
                                .viewTag(TRANSITION_KEY)
                                .build())
                        .build();
                  }
                })
            .build();
    mLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    View view = mLithoViewRule.findViewWithTag(TRANSITION_KEY);

    // 160 is equal to height and width of 200 - 40 for the size of the row.
    assertThat(view.getX()).describedAs("view X axis should be at start position").isEqualTo(160);
    assertThat(view.getY()).describedAs("view Y axis should be at start position").isEqualTo(160);

    mStateCaller.update();

    // X after state update should be at 160 because is going to be animated.
    assertThat(view.getX()).describedAs("view X axis after toggle").isEqualTo(160);
    // Y moves without animating
    assertThat(view.getY()).describedAs("view Y axis after toggle").isEqualTo(0);

    mTransitionTestRule.step(5);

    // Check java doc for how we calculate this value. NOTE: this is using a different interpolator
    // so the fraction is different, check AccelerateInterpolator class
    assertThat(view.getX()).describedAs("view X axis after 5 frames").isEqualTo(128.39507f);
    assertThat(view.getY()).describedAs("view Y axis after 5 frames").isEqualTo(0);

    mTransitionTestRule.step(5);

    assertThat(view.getX()).describedAs("view X axis after 10 frames").isEqualTo(0);
    assertThat(view.getY()).describedAs("view Y axis after 10 frames").isEqualTo(0);
  }

  @Test
  public void animationProperties_nullInterpolator_elementShouldAnimateInTheXAxis() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144, null))
                    .animate(AnimatedProperties.X))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Row.create(componentContext)
                        .heightDip(200)
                        .widthDip(200)
                        .justifyContent(state ? YogaJustify.FLEX_START : YogaJustify.FLEX_END)
                        .alignItems(state ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
                        .child(
                            Row.create(componentContext)
                                .heightDip(40)
                                .widthDip(40)
                                .backgroundColor(Color.parseColor("#ee1111"))
                                .transitionKey(TRANSITION_KEY)
                                .viewTag(TRANSITION_KEY)
                                .build())
                        .build();
                  }
                })
            .build();
    mLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    View view = mLithoViewRule.findViewWithTag(TRANSITION_KEY);

    // 160 is equal to height and width of 200 - 40 for the size of the row.
    assertThat(view.getX()).describedAs("view X axis should be at start position").isEqualTo(160);
    assertThat(view.getY()).describedAs("view Y axis should be at start position").isEqualTo(160);

    mStateCaller.update();

    // X after state update should be at 160 because is going to be animated.
    assertThat(view.getX()).describedAs("view X axis after toggle").isEqualTo(160);
    // Y moves without animating
    assertThat(view.getY()).describedAs("view Y axis after toggle").isEqualTo(0);

    mTransitionTestRule.step(5);

    // This is not using any interpolator so after 5 frames
    // 160(movement)/144(time_frame)*5(frames)*16(frame_time)
    assertThat(view.getX()).describedAs("view X axis after 5 frames").isEqualTo(88.888885f);
    assertThat(view.getY()).describedAs("view Y axis after 5 frames").isEqualTo(0);

    mTransitionTestRule.step(5);

    assertThat(view.getX()).describedAs("view X axis after 10 frames").isEqualTo(0);
    assertThat(view.getY()).describedAs("view Y axis after 10 frames").isEqualTo(0);
  }

  @Test
  public void animationRenderCore_unmountingLithoViewMidAnimation_shouldNotCrash() {
    final boolean useExtensionsWithMountDelegate =
        ComponentsConfiguration.useExtensionsWithMountDelegate;
    final boolean delegateToRenderCoreMount = ComponentsConfiguration.delegateToRenderCoreMount;
    ComponentsConfiguration.useExtensionsWithMountDelegate = true;
    ComponentsConfiguration.delegateToRenderCoreMount = true;

    mLithoViewRule.setRoot(getAnimatingXPropertyComponent());
    mActivityController.get().setContentView(mLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    View view = mLithoViewRule.findViewWithTag(TRANSITION_KEY);

    // 160 is equal to height and width of 200 - 40 for the size of the row.
    assertThat(view.getX()).describedAs("view X axis should be at start position").isEqualTo(160);
    assertThat(view.getY()).describedAs("view Y axis should be at start position").isEqualTo(160);

    mStateCaller.update();

    // X after state update should be at 160 because is going to be animated.
    assertThat(view.getX()).describedAs("view X axis after toggle").isEqualTo(160);
    // Y moves without animating
    assertThat(view.getY()).describedAs("view Y axis after toggle").isEqualTo(0);

    mTransitionTestRule.step(5);

    // Check java doc for how we calculate this value.
    assertThat(view.getX()).describedAs("view X axis after 5 frames").isEqualTo(93.89186f);
    assertThat(view.getY()).describedAs("view Y axis after 5 frames").isEqualTo(0);

    // This line would unmount the animating mountitem and the framework should stop the animation.
    mLithoViewRule.getLithoView().unmountAllItems();

    // After unmounting all items it should not crash.
    mTransitionTestRule.step(5);

    ComponentsConfiguration.useExtensionsWithMountDelegate = useExtensionsWithMountDelegate;
    ComponentsConfiguration.delegateToRenderCoreMount = delegateToRenderCoreMount;
  }

  @Test
  public void animationTransitionExtension_unmountingLithoViewMidAnimation_shouldNotCrash() {
    final boolean useTransitionsExtension = ComponentsConfiguration.useTransitionsExtension;
    ComponentsConfiguration.useTransitionsExtension = true;

    mLithoViewRule.setRoot(getAnimatingXPropertyComponent());
    mActivityController.get().setContentView(mLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    View view = mLithoViewRule.findViewWithTag(TRANSITION_KEY);

    // 160 is equal to height and width of 200 - 40 for the size of the row.
    assertThat(view.getX()).describedAs("view X axis should be at start position").isEqualTo(160);
    assertThat(view.getY()).describedAs("view Y axis should be at start position").isEqualTo(160);

    mStateCaller.update();

    // X after state update should be at 160 because is going to be animated.
    assertThat(view.getX()).describedAs("view X axis after toggle").isEqualTo(160);
    // Y moves without animating
    assertThat(view.getY()).describedAs("view Y axis after toggle").isEqualTo(0);

    mTransitionTestRule.step(5);

    // Check java doc for how we calculate this value.
    assertThat(view.getX()).describedAs("view X axis after 5 frames").isEqualTo(93.89186f);
    assertThat(view.getY()).describedAs("view Y axis after 5 frames").isEqualTo(0);

    // This line would unmount the animating mountitem and the framework should stop the animation.
    mLithoViewRule.getLithoView().unmountAllItems();

    // After unmounting all items it should not crash.
    mTransitionTestRule.step(5);

    ComponentsConfiguration.useTransitionsExtension = useTransitionsExtension;
  }

  @Test
  public void
      animationTransitionsExtension_reUsingLithoViewWithDifferentComponentTrees_shouldNotCrash() {
    final boolean useExtensionsWithMountDelegate =
        ComponentsConfiguration.useExtensionsWithMountDelegate;
    ComponentsConfiguration.useExtensionsWithMountDelegate = true;

    ComponentContext componentContext = new ComponentContext(RuntimeEnvironment.application);

    mLithoViewRule.setRoot(getNonAnimatingComponent());
    // We measure and layout this non animating component to initialize the transition extension.
    mLithoViewRule.measure().layout();

    // We need an other litho view where we are going to measure and layout an other similar tree
    // (the real difference here is that the root components are not animating)
    LithoView lithoView = new LithoView(componentContext);
    ComponentTree nonAnimatingComponentTree = ComponentTree.create(componentContext).build();
    nonAnimatingComponentTree.setRoot(getNonAnimatingComponent());
    lithoView.setComponentTree(nonAnimatingComponentTree);
    lithoView.measure(LithoViewRule.DEFAULT_WIDTH_SPEC, LithoViewRule.DEFAULT_HEIGHT_SPEC);
    lithoView.layout(0, 0, lithoView.getMeasuredWidth(), lithoView.getMeasuredHeight());

    // Now we need a new component tree that will hold a component tree that holds an animating root
    // component.
    ComponentTree animatingComponentTree = ComponentTree.create(componentContext).build();
    animatingComponentTree.setRoot(getAnimatingXPropertyComponent());

    mLithoViewRule.useComponentTree(animatingComponentTree);
    // We measure this component tree so we initialize the mRootTransition in the extension, but we
    // end up not running a layout here.
    mLithoViewRule.measure();

    // Finally we set a new animating component tree to the initial litho view and run measure and
    // layout.
    mLithoViewRule.useComponentTree(nonAnimatingComponentTree);
    mLithoViewRule.measure().layout();
    // Should not crash.
    ComponentsConfiguration.useExtensionsWithMountDelegate = useExtensionsWithMountDelegate;
  }

  @Test
  public void animation_unmountElementMidAppearAnimation_elementShouldBeUnmounted() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.stagger(
                    144,
                    Transition.create(TRANSITION_KEY + 0)
                        .animator(Transition.timing(144))
                        .animate(AnimatedProperties.ALPHA)
                        .appearFrom(0),
                    Transition.create(TRANSITION_KEY + 1)
                        .animator(Transition.timing(144))
                        .animate(AnimatedProperties.ALPHA)
                        .appearFrom(0)))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    Column.Builder builder =
                        Column.create(componentContext)
                            .child(
                                Row.create(componentContext)
                                    .heightDip(50)
                                    .widthDip(50)
                                    .backgroundColor(Color.YELLOW));
                    if (state) {
                      for (int i = 0; i < 2; i++) {
                        builder.child(
                            Row.create(componentContext)
                                .heightDip(50)
                                .widthDip(50)
                                .backgroundColor(Color.RED)
                                .viewTag(TRANSITION_KEY + i)
                                .transitionKey(TRANSITION_KEY + i));
                      }
                    }
                    return builder.build();
                  }
                })
            .build();
    mLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    // The bug only happens if you start the animation in the middle twice.
    View view = mLithoViewRule.findViewWithTag(TRANSITION_KEY + 1);

    // View should be null as state is null
    assertThat(view).describedAs("view before appearing").isNull();
    mStateCaller.update();

    view = mLithoViewRule.findViewWithTag(TRANSITION_KEY + 1);
    assertThat(view).describedAs("view after toggle").isNotNull();
    // After state update we should have the view added but with alpha equal to 0
    assertThat(view.getAlpha()).describedAs("view after toggle").isEqualTo(0);

    mTransitionTestRule.step(11);
    // Update state again the element should not be there.
    mStateCaller.update();

    view = mLithoViewRule.findViewWithTag(TRANSITION_KEY + 1);
    assertThat(view).describedAs("view unmount mid animation").isNull();

    mTransitionTestRule.step(1);
    // Now if we do this again we expect the appearing items to the same thing.
    mStateCaller.update();
    mTransitionTestRule.step(1);
    view = mLithoViewRule.findViewWithTag(TRANSITION_KEY + 1);
    assertThat(view).describedAs("view after toggle").isNotNull();
    // After state update we should have the view added but with alpha equal to 0
    assertThat(view.getAlpha()).describedAs("view after toggle").isEqualTo(0);
    mStateCaller.update();
    view = mLithoViewRule.findViewWithTag(TRANSITION_KEY + 1);
    assertThat(view).describedAs("view unmount mid animation").isNull();
  }

  @Test
  public void
      animation_disappearAnimationMovingAnItemToTheSameIndex_elementShouldDisappearWithoutCrashing() {
    final boolean useTransitionsExtension = ComponentsConfiguration.useTransitionsExtension;
    ComponentsConfiguration.useTransitionsExtension = true;
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create("text_like")
                    .animate(AnimatedProperties.ALPHA)
                    .appearFrom(0)
                    .disappearTo(0)
                    .animate(AnimatedProperties.X)
                    .appearFrom(DimensionValue.widthPercentageOffset(50))
                    .disappearTo(DimensionValue.widthPercentageOffset(50)))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext c, boolean state) {
                    return !state
                        ? Row.create(c)
                            .backgroundColor(Color.WHITE)
                            .heightDip(56)
                            .child(
                                Row.create(c)
                                    .widthPercent(33.3f)
                                    .alignItems(YogaAlign.CENTER)
                                    .justifyContent(YogaJustify.CENTER)
                                    .child(
                                        Text.create(c)
                                            .textSizeSp(16)
                                            .text("Comment")
                                            .marginDip(YogaEdge.LEFT, 8)))
                            .child(
                                Row.create(c)
                                    .widthPercent(33.3f)
                                    .alignItems(YogaAlign.CENTER)
                                    .justifyContent(YogaJustify.CENTER)
                                    .child(
                                        Column.create(c)
                                            .heightDip(24)
                                            .widthDip(24)
                                            .backgroundColor(Color.RED))
                                    .child(
                                        Text.create(c)
                                            .textSizeSp(16)
                                            .text("Like")
                                            .transitionKey("text_like")
                                            .marginDip(YogaEdge.LEFT, 8)))
                            .build()
                        : Row.create(c)
                            .backgroundColor(Color.WHITE)
                            .heightDip(56)
                            .child(
                                Row.create(c)
                                    .alignItems(YogaAlign.CENTER)
                                    .justifyContent(YogaJustify.CENTER)
                                    .paddingDip(YogaEdge.HORIZONTAL, 16)
                                    .child(
                                        Column.create(c)
                                            .transitionKey("icon_like")
                                            .heightDip(24)
                                            .widthDip(24)
                                            .backgroundColor(Color.RED)))
                            .child(
                                Row.create(c)
                                    .widthPercent(33.3f)
                                    .alignItems(YogaAlign.CENTER)
                                    .justifyContent(YogaJustify.CENTER)
                                    .child(
                                        Text.create(c)
                                            .textSizeSp(16)
                                            .text("Comment")
                                            .marginDip(YogaEdge.LEFT, 8)))
                            .build();
                  }
                })
            .build();
    mLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    // We move 100 frames to be sure any appearing animation finished.
    mTransitionTestRule.step(100);

    mStateCaller.update();
    // We move an other 100 frames to be sure disappearing animations are done.
    mTransitionTestRule.step(100);

    mTransitionTestRule.step(100);

    // Do not crash.
    ComponentsConfiguration.useTransitionsExtension = useTransitionsExtension;
  }

  @Test
  public void
      animationTransitionsExtension_reUsingLithoViewWithSameComponentTrees_shouldNotCrash() {
    ComponentContext componentContext = mLithoViewRule.getContext();

    ComponentTree animatingComponentTree = ComponentTree.create(componentContext).build();
    animatingComponentTree.setRoot(getAnimatingXPropertyComponent());

    mLithoViewRule.useComponentTree(animatingComponentTree);
    mActivityController.get().setContentView(mLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    mStateCaller.update();

    mTransitionTestRule.step(5);

    LithoView lithoView1 = mLithoViewRule.getLithoView();
    lithoView1.setComponentTree(null);
    LithoView lithoView2 = new LithoView(componentContext);
    lithoView2.setComponentTree(animatingComponentTree);
    mLithoViewRule.useLithoView(lithoView2);
    animatingComponentTree.setRoot(getNonAnimatingComponent());
    mStateCaller.update();
    mTransitionTestRule.step(1);

    lithoView2.setComponentTree(null);
    lithoView1.setComponentTree(animatingComponentTree);
    mLithoViewRule.useLithoView(lithoView1);

    mTransitionTestRule.step(1000);
  }

  private Component getAnimatingXPropertyComponent() {
    return TestAnimationsComponent.create(mLithoViewRule.getContext())
        .stateCaller(mStateCaller)
        .transition(
            Transition.create(TRANSITION_KEY)
                .animator(Transition.timing(144))
                .animate(AnimatedProperties.X))
        .testComponent(
            new TestAnimationsComponentSpec
                .TestComponent() { // This could be a lambda but it fails ci.
              @Override
              public Component getComponent(ComponentContext componentContext, boolean state) {
                return Row.create(componentContext)
                    .heightDip(200)
                    .widthDip(200)
                    .justifyContent(state ? YogaJustify.FLEX_START : YogaJustify.FLEX_END)
                    .alignItems(state ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
                    .child(
                        Row.create(componentContext)
                            .heightDip(40)
                            .widthDip(40)
                            .backgroundColor(Color.parseColor("#ee1111"))
                            .transitionKey(TRANSITION_KEY)
                            .viewTag(TRANSITION_KEY)
                            .build())
                    .build();
              }
            })
        .build();
  }

  private Component getNonAnimatingComponent() {
    return TestAnimationsComponent.create(mLithoViewRule.getContext())
        .stateCaller(mStateCaller)
        .transition(null)
        .testComponent(
            new TestAnimationsComponentSpec
                .TestComponent() { // This could be a lambda but it fails ci.
              @Override
              public Component getComponent(ComponentContext componentContext, boolean state) {
                return Row.create(componentContext)
                    .heightDip(200)
                    .widthDip(200)
                    .justifyContent(state ? YogaJustify.FLEX_START : YogaJustify.FLEX_END)
                    .alignItems(state ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
                    .child(
                        Row.create(componentContext)
                            .heightDip(40)
                            .widthDip(40)
                            .backgroundColor(Color.parseColor("#ee1111"))
                            .build())
                    .build();
              }
            })
        .build();
  }

  @Test
  public void transitionAnimation_interruption_overridesCurrentTransition() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(160, null))
                    .animate(AnimatedProperties.X))
            .testComponent(
                new TestAnimationsComponentSpec.TestComponent() {
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Row.create(componentContext)
                        .widthDip(200)
                        .justifyContent(state ? YogaJustify.FLEX_START : YogaJustify.FLEX_END)
                        .child(
                            Row.create(componentContext)
                                .heightDip(40)
                                .widthDip(40)
                                .transitionKey(TRANSITION_KEY)
                                .viewTag(TRANSITION_KEY)
                                .build())
                        .build();
                  }
                })
            .build();
    mLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    final View view = mLithoViewRule.findViewWithTag(TRANSITION_KEY);

    assertThat(view.getX()).describedAs("x pos before transition").isEqualTo(160);

    // Start the transition by changing the state
    mStateCaller.update();

    // Advance to the mid point of the transition
    mTransitionTestRule.step(6);
    assertThat(view.getX()).describedAs("x pos at transition midpoint").isEqualTo(80f);

    // Trigger a new transition that interrupts the current transition and  returns the component to
    // its original position. NB: The Transition is fixed time, so it will take longer to return
    mStateCaller.update();

    // Advance to the mid point of the return transition
    mTransitionTestRule.step(6);
    assertThat(view.getX()).describedAs("x pos at return transition midpoint").isEqualTo(120);

    // Advance to the end of the return transition
    mTransitionTestRule.step(5);
    assertThat(view.getX()).describedAs("x pos after return transition").isEqualTo(160);
  }

  @Test
  public void
      animation_disappearAnimationOnNestedLithoViews_elementShouldDisappearAnimatingAlpha() {
    final boolean useTransitionsExtension = ComponentsConfiguration.useTransitionsExtension;
    ComponentsConfiguration.useTransitionsExtension = true;
    final StateCaller innerStateCaller = new StateCaller();
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.ALPHA)
                    .appearFrom(0)
                    .disappearTo(0))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Column.create(componentContext)
                        .child(
                            Row.create(componentContext)
                                .heightDip(50)
                                .widthDip(50)
                                .backgroundColor(Color.YELLOW))
                        .child(
                            TestAnimationMount.create(componentContext)
                                .stateCaller(innerStateCaller))
                        .child(
                            !state
                                ? Row.create(componentContext)
                                    .heightDip(50)
                                    .widthDip(50)
                                    .backgroundColor(Color.RED)
                                    .viewTag(TRANSITION_KEY)
                                    .transitionKey(TRANSITION_KEY)
                                    .key(TRANSITION_KEY)
                                : null)
                        .build();
                  }
                })
            .build();
    mLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    // We move 10 frames to account for the appear animation.
    mTransitionTestRule.step(10);

    View view = mLithoViewRule.findViewWithTag(TRANSITION_KEY);
    View innerView = mLithoViewRule.findViewWithTag("TestAnimationMount");
    // Here we get inner LithoView
    LithoView innerLithoView = (LithoView) innerView.getParent();

    // Update state on both
    mStateCaller.update();
    innerStateCaller.update();
    // We look for the same view
    innerView = mLithoViewRule.findViewWithTag("TestAnimationMount");

    assertThat(innerLithoView)
        .describedAs("We mantain the same LithoView")
        .isEqualTo(innerView.getParent());

    ComponentsConfiguration.useTransitionsExtension = useTransitionsExtension;
  }
}
