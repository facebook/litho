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

package com.facebook.litho.animation

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentHost
import com.facebook.litho.ComponentTree
import com.facebook.litho.DynamicValue
import com.facebook.litho.LithoView
import com.facebook.litho.Row
import com.facebook.litho.StateCaller
import com.facebook.litho.Style
import com.facebook.litho.Transition
import com.facebook.litho.animated.translationX
import com.facebook.litho.animated.translationY
import com.facebook.litho.config.TempComponentsConfigurations
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.core.widthPercent
import com.facebook.litho.flexbox.flex
import com.facebook.litho.flexbox.position
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.DEFAULT_HEIGHT_SPEC
import com.facebook.litho.testing.DEFAULT_WIDTH_SPEC
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.TransitionTestRule
import com.facebook.litho.testing.Whitebox
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.transition.transitionKey
import com.facebook.litho.view.alpha
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.rotation
import com.facebook.litho.view.rotationY
import com.facebook.litho.view.scale
import com.facebook.litho.view.testKey
import com.facebook.litho.view.viewTag
import com.facebook.litho.view.wrapInView
import com.facebook.litho.widget.TestAnimationMount
import com.facebook.litho.widget.TestAnimationsComponent
import com.facebook.rendercore.dp
import com.facebook.rendercore.px
import com.facebook.rendercore.sp
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.LooperMode

/**
 * This tests validate how different kind of animations modify the view. The values asserted here
 * are specific to the number of frames and the type of animation.
 *
 * All transitions are timed to 144 ms which translates to 9 frames (we actually step 10 because the
 * first one does not count as it only initializes the values in the [ ]) based on
 * [MockTimingSource].FRAME_TIME_MS) which is 16ms per frame.
 *
 * Formula for the specific values found in this tests:
 * - First we calculate the timing doing: timing = (frames*frame_time - frame_time) / ((frame_time +
 *   duration_ms) - frame_time)
 * - Then we run the AccelerateDecelerateInterpolator: fraction = cos((timing + 1) * PI / 2) + 0.5 f
 * - Finally the actual result is: result = initial_position + fraction * (final_position -
 *   initial_position)
 *
 * Example for X axis animation after 5 frames: (5*16 - 16) / ((16 + 144) - 16) = 0.44444445.
 *
 * AccelerateDecelareteInterpolator: cos((0.44444445 + 1) * PI / 2) + * 0.5 f = 0.4131759
 *
 * Final position: 160 + 0.4131759 * (-160) = 93.891856
 */
@LooperMode(LooperMode.Mode.LEGACY)
@SuppressLint("ColorConstantUsageIssue")
@RunWith(LithoTestRunner::class)
class AnimationTest {
  @JvmField @Rule val lithoViewRule = LithoViewRule()
  @JvmField @Rule val transitionTestRule = TransitionTestRule()

  private val stateCaller = StateCaller()
  private lateinit var activityController: ActivityController<Activity>

  @Before
  fun setUp() {
    activityController = Robolectric.buildActivity(Activity::class.java, Intent())
  }

  @Test
  fun animationProperties_animatingPropertyX_elementShouldAnimateInTheXAxis() {
    val testLithoView = lithoViewRule.render { animatingXPropertyComponent }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()
    val view = testLithoView.findViewWithTag(TRANSITION_KEY)

    // 160 is equal to height and width of 200 - 40 for the size of the row.
    Assertions.assertThat(view.x)
        .describedAs("view X axis should be at start position")
        .isEqualTo(160f)
    Assertions.assertThat(view.y)
        .describedAs("view Y axis should be at start position")
        .isEqualTo(160f)
    stateCaller.update()

    lithoViewRule.idle()

    // X after state update should be at 160 because is going to be animated.
    Assertions.assertThat(view.x).describedAs("view X axis after toggle").isEqualTo(160f)
    // Y moves without animating
    Assertions.assertThat(view.y).describedAs("view Y axis after toggle").isEqualTo(0f)
    transitionTestRule.step(5)

    // Check kdoc for how we calculate this value.
    Assertions.assertThat(view.x).describedAs("view X axis after 5 frames").isEqualTo(93.89186f)
    Assertions.assertThat(view.y).describedAs("view Y axis after 5 frames").isEqualTo(0f)
    transitionTestRule.step(5)
    Assertions.assertThat(view.x).describedAs("view X axis after 10 frames").isEqualTo(0f)
    Assertions.assertThat(view.y).describedAs("view Y axis after 10 frames").isEqualTo(0f)
  }

  @Test
  fun animationProperties_animatingPropertyY_elementShouldAnimateInTheYAxis() {
    val component =
        TestAnimationsComponent(
            stateCaller,
            Transition.create(TRANSITION_KEY)
                .animator(Transition.timing(144))
                .animate(AnimatedProperties.Y)) { state ->
              Row(
                  justifyContent = if (state) YogaJustify.FLEX_START else YogaJustify.FLEX_END,
                  alignItems = if (state) YogaAlign.FLEX_START else YogaAlign.FLEX_END,
                  style = Style.height(200.dp).width(200.dp)) {
                    child(
                        Row(
                            style =
                                Style.height(40.dp)
                                    .width(40.dp)
                                    .backgroundColor(Color.parseColor("#ee1111"))
                                    .transitionKey(context, TRANSITION_KEY)
                                    .viewTag(TRANSITION_KEY)))
                  }
            }

    val testLithoView = lithoViewRule.render { component }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()
    val view = testLithoView.findViewWithTag(TRANSITION_KEY)
    Assertions.assertThat(view.x)
        .describedAs("view X axis should be at start position")
        .isEqualTo(160f)
    Assertions.assertThat(view.y)
        .describedAs("view Y axis should be at start position")
        .isEqualTo(160f)
    stateCaller.update()

    lithoViewRule.idle()

    // X moves without animating
    Assertions.assertThat(view.x).describedAs("view X axis after toggle").isEqualTo(0f)
    // Y after state update should be at 160 because is going to be animated
    Assertions.assertThat(view.y).describedAs("view Y axis after toggle").isEqualTo(160f)
    transitionTestRule.step(5)
    Assertions.assertThat(view.x).describedAs("view X axis after 5 frames").isEqualTo(0f)
    // Check kdoc for how we calculate this value.
    Assertions.assertThat(view.y).describedAs("view Y axis after 5 frames").isEqualTo(93.89186f)
    transitionTestRule.step(5)
    Assertions.assertThat(view.x).describedAs("view X axis after 10 frames").isEqualTo(0f)
    Assertions.assertThat(view.y).describedAs("view Y axis after 10 frames").isEqualTo(0f)
  }

  @Test
  fun animationProperties_animatingPropertyScale_elementShouldAnimateXandYScale() {
    val component =
        TestAnimationsComponent(
            stateCaller,
            Transition.create(TRANSITION_KEY)
                .animator(Transition.timing(144))
                .animate(AnimatedProperties.SCALE)) { state ->
              Row {
                child(
                    Row(
                        style =
                            Style.height(40.dp)
                                .width(40.dp)
                                .scale(if (state) 1f else 2f)
                                .backgroundColor(Color.parseColor("#ee1111"))
                                .transitionKey(context, TRANSITION_KEY)
                                .viewTag(TRANSITION_KEY)))
              }
            }

    val testLithoView = lithoViewRule.render { component }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()
    val view = testLithoView.findViewWithTag(TRANSITION_KEY)
    Assertions.assertThat(view.scaleX).describedAs("view scale X initial position").isEqualTo(2f)
    Assertions.assertThat(view.scaleY).describedAs("view scale Y initial position").isEqualTo(2f)
    stateCaller.update()
    lithoViewRule.idle()
    Assertions.assertThat(view.scaleX).describedAs("view X axis after toggle").isEqualTo(2f)
    Assertions.assertThat(view.scaleY).describedAs("view Y axis after toggle").isEqualTo(2f)
    transitionTestRule.step(5)

    // Check kdoc for how we calculate this value.
    Assertions.assertThat(view.scaleX)
        .describedAs("view X axis after 5 frames")
        .isEqualTo(1.5868242f)
    Assertions.assertThat(view.scaleY)
        .describedAs("view Y axis after 5 frames")
        .isEqualTo(1.5868242f)
    transitionTestRule.step(5)
    Assertions.assertThat(view.scaleX).describedAs("view X axis after 10 frames").isEqualTo(1f)
    Assertions.assertThat(view.scaleY).describedAs("view Y axis after 10 frames").isEqualTo(1f)
  }

  @Test
  fun animationProperties_animatingPropertyAlpha_elementShouldAnimateAlpha() {
    val component =
        TestAnimationsComponent(
            stateCaller,
            Transition.create(TRANSITION_KEY)
                .animator(Transition.timing(144))
                .animate(AnimatedProperties.ALPHA)) { state ->
              Row {
                child(
                    Row(
                        style =
                            Style.height(40.dp)
                                .width(40.dp)
                                .alpha(if (state) 1f else 0.5f)
                                .backgroundColor(Color.parseColor("#ee1111"))
                                .transitionKey(context, TRANSITION_KEY)
                                .viewTag(TRANSITION_KEY)))
              }
            }
    val testLithoView = lithoViewRule.render { component }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()
    val view = testLithoView.findViewWithTag(TRANSITION_KEY)
    Assertions.assertThat(view.alpha).describedAs("view alpha initial state").isEqualTo(0.5f)
    stateCaller.update()
    lithoViewRule.idle()
    Assertions.assertThat(view.alpha).describedAs("view alpha after toggle").isEqualTo(0.5f)
    transitionTestRule.step(5)

    // Check kdoc for how we calculate this value.
    Assertions.assertThat(view.alpha).describedAs("view alpha after 5 frames").isEqualTo(0.7065879f)
    transitionTestRule.step(5)
    Assertions.assertThat(view.alpha).describedAs("view alpha after 10 frames").isEqualTo(1f)
  }

  @Test
  fun animationProperties_animatingPropertyRotation_elementShouldAnimateRotation() {
    val component =
        TestAnimationsComponent(
            stateCaller,
            Transition.create(TRANSITION_KEY)
                .animator(Transition.timing(144))
                .animate(AnimatedProperties.ROTATION)) { state ->
              Row {
                child(
                    Row(
                        style =
                            Style.height(40.dp)
                                .width(40.dp)
                                .rotation(if (state) 45f else 0f)
                                .backgroundColor(Color.parseColor("#ee1111"))
                                .transitionKey(context, TRANSITION_KEY)
                                .viewTag(TRANSITION_KEY)))
              }
            }
    val testLithoView = lithoViewRule.render { component }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()
    val view = testLithoView.findViewWithTag(TRANSITION_KEY)
    Assertions.assertThat(view.rotation).describedAs("view rotation initial state").isEqualTo(0f)
    stateCaller.update()
    lithoViewRule.idle()
    Assertions.assertThat(view.rotation).describedAs("view rotation after toggle").isEqualTo(0f)
    transitionTestRule.step(5)

    // Check kdoc for how we calculate this value.
    Assertions.assertThat(view.rotation)
        .describedAs("view rotation after 5 frames")
        .isEqualTo(18.592915f)
    transitionTestRule.step(5)
    Assertions.assertThat(view.rotation).describedAs("view rotation after 10 frames").isEqualTo(45f)
  }

  @Test
  fun animationProperties_animatingPropertyRotationY_elementShouldAnimateRotationY() {
    val component =
        TestAnimationsComponent(
            stateCaller,
            Transition.create(TRANSITION_KEY)
                .animator(Transition.timing(144))
                .animate(AnimatedProperties.ROTATION_Y)) { state ->
              Row {
                child(
                    Row(
                        style =
                            Style.height(40.dp)
                                .width(40.dp)
                                .rotationY(if (state) 180f else 0f)
                                .backgroundColor(Color.parseColor("#ee1111"))
                                .transitionKey(context, TRANSITION_KEY)
                                .viewTag(TRANSITION_KEY)))
              }
            }
    val testLithoView = lithoViewRule.render { component }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()
    val view = testLithoView.findViewWithTag(TRANSITION_KEY)
    Assertions.assertThat(view.rotationY).describedAs("view rotationY initial state").isEqualTo(0f)
    stateCaller.update()
    lithoViewRule.idle()
    Assertions.assertThat(view.rotationY).describedAs("view rotationY after toggle").isEqualTo(0f)
    transitionTestRule.step(5)

    // Check kdoc for how we calculate this value.
    Assertions.assertThat(view.rotationY)
        .describedAs("view rotationY after 5 frames")
        .isEqualTo(74.37166f)
    transitionTestRule.step(5)
    Assertions.assertThat(view.rotationY)
        .describedAs("view rotationY after 10 frames")
        .isEqualTo(180f)
  }

  @Test
  fun animationProperties_animatingPropertyHeight_elementShouldAnimateHeight() {
    val component =
        TestAnimationsComponent(
            stateCaller,
            Transition.create(TRANSITION_KEY)
                .animator(Transition.timing(144))
                .animate(AnimatedProperties.HEIGHT)) { state ->
              Row(style = Style.height(200.dp).width(200.dp)) {
                child(
                    Row(
                        style =
                            Style.height(if (state) 80.dp else 40.dp)
                                .width(if (state) 80.dp else 40.dp)
                                .backgroundColor(Color.parseColor("#ee1111"))
                                .transitionKey(context, TRANSITION_KEY)
                                .viewTag(TRANSITION_KEY)))
              }
            }
    val testLithoView = lithoViewRule.render { component }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()
    val view = testLithoView.findViewWithTag(TRANSITION_KEY)
    Assertions.assertThat(view.height).describedAs("view height initial state").isEqualTo(40)
    Assertions.assertThat(view.width).describedAs("view width initial state").isEqualTo(40)
    stateCaller.update()
    lithoViewRule.idle()
    Assertions.assertThat(view.height).describedAs("view height after toggle").isEqualTo(40)
    Assertions.assertThat(view.width).describedAs("view width after toggle").isEqualTo(80)
    transitionTestRule.step(5)

    // Check kdoc for how we calculate this value.
    Assertions.assertThat(view.height).describedAs("view height after 5 frames").isEqualTo(56)
    transitionTestRule.step(5)
    Assertions.assertThat(view.height).describedAs("view height after 10 frames").isEqualTo(80)
    Assertions.assertThat(view.width).describedAs("view width after 10 frames").isEqualTo(80)
  }

  @Test
  fun animationProperties_animatingPropertyWidth_elementShouldAnimateWidth() {
    val component =
        TestAnimationsComponent(
            stateCaller,
            Transition.create(TRANSITION_KEY)
                .animator(Transition.timing(144))
                .animate(AnimatedProperties.WIDTH)) { state ->
              Row(style = Style.height(200.dp).width(200.dp)) {
                child(
                    Row(
                        style =
                            Style.height(if (state) 80.dp else 40.dp)
                                .width(if (state) 80.dp else 40.dp)
                                .backgroundColor(Color.parseColor("#ee1111"))
                                .transitionKey(context, TRANSITION_KEY)
                                .viewTag(TRANSITION_KEY)))
              }
            }
    val testLithoView = lithoViewRule.render { component }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()
    val view = testLithoView.findViewWithTag(TRANSITION_KEY)
    Assertions.assertThat(view.height).describedAs("view height initial state").isEqualTo(40)
    Assertions.assertThat(view.width).describedAs("view width initial state").isEqualTo(40)
    stateCaller.update()
    lithoViewRule.idle()
    Assertions.assertThat(view.height).describedAs("view height after toggle").isEqualTo(80)
    Assertions.assertThat(view.width).describedAs("view width after toggle").isEqualTo(40)
    transitionTestRule.step(5)

    // Check kdoc for how we calculate this value.
    Assertions.assertThat(view.width).describedAs("view width after 5 frames").isEqualTo(56)
    transitionTestRule.step(5)
    Assertions.assertThat(view.height).describedAs("view height after 10 frames").isEqualTo(80)
    Assertions.assertThat(view.width).describedAs("view width after 10 frames").isEqualTo(80)
  }

  @Test
  fun animation_appearAnimation_elementShouldAppearAnimatingAlpha() {
    val component =
        TestAnimationsComponent(
            stateCaller,
            Transition.create(TRANSITION_KEY)
                .animator(Transition.timing(144))
                .animate(AnimatedProperties.ALPHA)
                .appearFrom(0f)
                .disappearTo(0f)) { state ->
              Column {
                child(Row(style = Style.height(50.dp).width(50.dp).backgroundColor(Color.YELLOW)))
                child(
                    if (state)
                        Row(
                            style =
                                Style.height(50.dp)
                                    .width(50.dp)
                                    .backgroundColor(Color.RED)
                                    .viewTag(TRANSITION_KEY)
                                    .transitionKey(context, TRANSITION_KEY))
                    else null)
              }
            }
    val testLithoView = lithoViewRule.render { component }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()
    var view = testLithoView.findViewWithTagOrNull(TRANSITION_KEY)

    // View should be null as state is null
    Assertions.assertThat(view).describedAs("view before appearing").isNull()
    stateCaller.update()
    lithoViewRule.idle()

    // After state update we should have the view added but with alpha equal to 0
    view = testLithoView.findViewWithTag(TRANSITION_KEY)
    Assertions.assertThat(view).describedAs("view after toggle").isNotNull
    Assertions.assertThat(view.alpha).describedAs("view after toggle").isEqualTo(0f)
    transitionTestRule.step(5)

    // Check kdoc for how we calculate this value.
    Assertions.assertThat(view.alpha).describedAs("view after 5 frames").isEqualTo(0.41317588f)
    transitionTestRule.step(5)
    Assertions.assertThat(view.alpha).describedAs("view after 10 frames").isEqualTo(1f)
  }

  @Test
  fun animation_disappearAnimation_elementShouldDisappearAnimatingAlpha() {
    val component =
        TestAnimationsComponent(
            stateCaller,
            Transition.create(TRANSITION_KEY)
                .animator(Transition.timing(144))
                .animate(AnimatedProperties.ALPHA)
                .appearFrom(0f)
                .disappearTo(0f)) { state ->
              Column {
                child(Row(style = Style.height(50.dp).width(50.dp).backgroundColor(Color.YELLOW)))
                child(
                    if (!state)
                        Row(
                            style =
                                Style.height(50.dp)
                                    .width(50.dp)
                                    .backgroundColor(Color.RED)
                                    .viewTag(TRANSITION_KEY)
                                    .transitionKey(context, TRANSITION_KEY))
                    else null)
              }
            }
    val testLithoView = lithoViewRule.render { component }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()

    // We move 10 frames to account for the appear animation.
    transitionTestRule.step(10)
    var view = testLithoView.findViewWithTag(TRANSITION_KEY)
    // The view is not null
    Assertions.assertThat(view).describedAs("view before disappearing").isNotNull
    Assertions.assertThat(view.alpha).describedAs("view before disappearing").isEqualTo(1f)
    stateCaller.update()
    lithoViewRule.idle()
    view = testLithoView.findViewWithTag(TRANSITION_KEY)

    // After state update, even if the row was removed from the component, it still not null as we
    // are going to animate it. Alpha stays at 1 before advancing frames.
    Assertions.assertThat(view).describedAs("view after toggle").isNotNull
    Assertions.assertThat(view.alpha).describedAs("view after toggle").isEqualTo(1f)
    transitionTestRule.step(5)
    // Check kdoc for how we calculate this value.
    Assertions.assertThat(view.alpha).describedAs("view after 5 frames").isEqualTo(0.5868241f)

    // We move only 4 more frames because after 5 the view should be removed from the hierarchy.
    transitionTestRule.step(4)
    // Check kdoc for how we calculate this value.
    Assertions.assertThat(view.alpha).describedAs("view after 10 frames").isEqualTo(0.030153751f)
    transitionTestRule.step(1)

    val viewOrNull = testLithoView.findViewWithTagOrNull(TRANSITION_KEY)
    Assertions.assertThat(viewOrNull)
        .describedAs("view after last re-measure and re-layout")
        .isNull()
  }

  @Test
  fun animation_disappearAnimationWithRemountToRoot_elementShouldDisappearWithoutCrashing() {
    val component =
        TestAnimationsComponent(
            stateCaller,
            Transition.parallel(
                Transition.create("comment_editText")
                    .animate(AnimatedProperties.ALPHA)
                    .appearFrom(0f)
                    .disappearTo(0f)
                    .animate(AnimatedProperties.X)
                    .appearFrom(DimensionValue.widthPercentageOffset(-50f))
                    .disappearTo(DimensionValue.widthPercentageOffset(-50f)),
                Transition.create("cont_comment")
                    .animate(AnimatedProperties.ALPHA)
                    .appearFrom(0f)
                    .disappearTo(0f),
                Transition.create("icon_like", "icon_share").animate(AnimatedProperties.X),
                Transition.create("text_like", "text_share")
                    .animate(AnimatedProperties.ALPHA)
                    .appearFrom(0f)
                    .disappearTo(0f)
                    .animate(AnimatedProperties.X)
                    .appearFrom(DimensionValue.widthPercentageOffset(50f))
                    .disappearTo(DimensionValue.widthPercentageOffset(50f)))) { state ->
              if (!state)
                  Row(style = Style.backgroundColor(Color.WHITE).height(56.dp)) {
                    child(
                        Row(
                            alignItems = YogaAlign.CENTER,
                            justifyContent = YogaJustify.CENTER,
                            style = Style.widthPercent(33.3f).wrapInView().testKey("like_button")) {
                              child(
                                  Column(
                                      style =
                                          Style.height(24.dp)
                                              .width(24.dp)
                                              .backgroundColor(Color.RED)
                                              .transitionKey(context, "icon_like")))
                              child(
                                  Text(
                                      textSize = 16.sp,
                                      text = "Like",
                                      style =
                                          Style.transitionKey(context, "text_like")
                                              .margin(left = 8.dp)))
                            })
                    child(
                        Row(
                            alignItems = YogaAlign.CENTER,
                            justifyContent = YogaJustify.CENTER,
                            style =
                                Style.transitionKey(context, "cont_comment").widthPercent(33.3f)) {
                              child(
                                  Column(
                                      style =
                                          Style.height(24.dp)
                                              .width(24.dp)
                                              .backgroundColor(Color.RED)))
                              child(
                                  Text(
                                      textSize = 16.sp,
                                      text = "Comment",
                                      style = Style.margin(left = 8.dp)))
                            })
                    child(
                        Row(
                            alignItems = YogaAlign.CENTER,
                            justifyContent = YogaJustify.CENTER,
                            style = Style.widthPercent(33.3f)) {
                              child(
                                  Column(
                                      style =
                                          Style.transitionKey(context, "icon_share")
                                              .height(24.dp)
                                              .width(24.dp)
                                              .backgroundColor(Color.RED)))
                              child(
                                  Text(
                                      textSize = 16.sp,
                                      text = "Share",
                                      style =
                                          Style.transitionKey(context, "text_share")
                                              .margin(left = 8.dp)))
                            })
                  }
              else
                  Row(style = Style.backgroundColor(Color.WHITE).height(56.dp)) {
                    child(
                        Row(
                            alignItems = YogaAlign.CENTER,
                            justifyContent = YogaJustify.CENTER,
                            style =
                                Style.wrapInView()
                                    .padding(horizontal = 16.dp)
                                    .testKey("like_button")) {
                              child(
                                  Column(
                                      style =
                                          Style.transitionKey(context, "icon_like")
                                              .height(24.dp)
                                              .width(24.dp)
                                              .backgroundColor(Color.RED)))
                            })
                    child(
                        Column(
                            style =
                                Style.flex(grow = 1f).transitionKey(context, "comment_editText")) {
                              child(Text(text = "Input here", textSize = 16.sp))
                            })
                    child(
                        Row(
                            style =
                                Style.transitionKey(context, "cont_share")
                                    .wrapInView()
                                    .padding(all = 16.dp)
                                    .backgroundColor(-0xffff01),
                            alignItems = YogaAlign.CENTER) {
                              child(
                                  Column(
                                      style =
                                          Style.transitionKey(context, "icon_share")
                                              .height(24.dp)
                                              .width(24.dp)
                                              .backgroundColor(Color.RED)))
                            })
                  }
            }
    val testLithoView = lithoViewRule.render { component }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()

    // We move 100 frames to be sure any appearing animation finished.
    transitionTestRule.step(100)
    stateCaller.update()
    lithoViewRule.idle()
    // We move an other 100 frames to be sure disappearing animations are done.
    transitionTestRule.step(100)
    transitionTestRule.step(100)

    // Do not crash.
  }

  @Test
  fun animationProperties_differentInterpolator_elementShouldAnimateInTheXAxis() {
    val component =
        TestAnimationsComponent(
            stateCaller,
            Transition.create(TRANSITION_KEY)
                .animator(Transition.timing(144, AccelerateInterpolator()))
                .animate(AnimatedProperties.X)) { state ->
              Row(
                  style = Style.height(200.dp).width(200.dp),
                  justifyContent = if (state) YogaJustify.FLEX_START else YogaJustify.FLEX_END,
                  alignItems = if (state) YogaAlign.FLEX_START else YogaAlign.FLEX_END) {
                    child(
                        Row(
                            style =
                                Style.height(40.dp)
                                    .width(40.dp)
                                    .backgroundColor(Color.parseColor("#ee1111"))
                                    .transitionKey(context, TRANSITION_KEY)
                                    .viewTag(TRANSITION_KEY)))
                  }
            }

    val testLithoView = lithoViewRule.render { component }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()
    val view = testLithoView.findViewWithTag(TRANSITION_KEY)

    // 160 is equal to height and width of 200 - 40 for the size of the row.
    Assertions.assertThat(view.x)
        .describedAs("view X axis should be at start position")
        .isEqualTo(160f)
    Assertions.assertThat(view.y)
        .describedAs("view Y axis should be at start position")
        .isEqualTo(160f)
    stateCaller.update()
    lithoViewRule.idle()

    // X after state update should be at 160 because is going to be animated.
    Assertions.assertThat(view.x).describedAs("view X axis after toggle").isEqualTo(160f)
    // Y moves without animating
    Assertions.assertThat(view.y).describedAs("view Y axis after toggle").isEqualTo(0f)
    transitionTestRule.step(5)

    // Check kdoc for how we calculate this value. NOTE: this is using a different interpolator
    // so the fraction is different, check AccelerateInterpolator class
    Assertions.assertThat(view.x).describedAs("view X axis after 5 frames").isEqualTo(128.39507f)
    Assertions.assertThat(view.y).describedAs("view Y axis after 5 frames").isEqualTo(0f)
    transitionTestRule.step(5)
    Assertions.assertThat(view.x).describedAs("view X axis after 10 frames").isEqualTo(0f)
    Assertions.assertThat(view.y).describedAs("view Y axis after 10 frames").isEqualTo(0f)
  }

  @Test
  fun animationProperties_nullInterpolator_elementShouldAnimateInTheXAxis() {
    val component =
        TestAnimationsComponent(
            stateCaller,
            Transition.create(TRANSITION_KEY)
                .animator(Transition.timing(144, null))
                .animate(AnimatedProperties.X)) { state ->
              Row(
                  style = Style.height(200.dp).width(200.dp),
                  justifyContent = if (state) YogaJustify.FLEX_START else YogaJustify.FLEX_END,
                  alignItems = if (state) YogaAlign.FLEX_START else YogaAlign.FLEX_END) {
                    child(
                        Row(
                            style =
                                Style.height(40.dp)
                                    .width(40.dp)
                                    .backgroundColor(Color.parseColor("#ee1111"))
                                    .transitionKey(context, TRANSITION_KEY)
                                    .viewTag(TRANSITION_KEY)))
                  }
            }
    val testLithoView = lithoViewRule.render { component }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()
    val view = testLithoView.findViewWithTag(TRANSITION_KEY)

    // 160 is equal to height and width of 200 - 40 for the size of the row.
    Assertions.assertThat(view.x)
        .describedAs("view X axis should be at start position")
        .isEqualTo(160f)
    Assertions.assertThat(view.y)
        .describedAs("view Y axis should be at start position")
        .isEqualTo(160f)
    stateCaller.update()
    lithoViewRule.idle()

    // X after state update should be at 160 because is going to be animated.
    Assertions.assertThat(view.x).describedAs("view X axis after toggle").isEqualTo(160f)
    // Y moves without animating
    Assertions.assertThat(view.y).describedAs("view Y axis after toggle").isEqualTo(0f)
    transitionTestRule.step(5)

    // This is not using any interpolator so after 5 frames
    // 160(movement)/144(time_frame)*5(frames)*16(frame_time)
    Assertions.assertThat(view.x).describedAs("view X axis after 5 frames").isEqualTo(88.888885f)
    Assertions.assertThat(view.y).describedAs("view Y axis after 5 frames").isEqualTo(0f)
    transitionTestRule.step(5)
    Assertions.assertThat(view.x).describedAs("view X axis after 10 frames").isEqualTo(0f)
    Assertions.assertThat(view.y).describedAs("view Y axis after 10 frames").isEqualTo(0f)
  }

  @Test
  fun animation_unmountingLithoViewMidAnimation_shouldNotCrash() {
    val testLithoView = lithoViewRule.render { animatingXPropertyComponent }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()
    val view = testLithoView.findViewWithTag(TRANSITION_KEY)

    // 160 is equal to height and width of 200 - 40 for the size of the row.
    Assertions.assertThat(view.x)
        .describedAs("view X axis should be at start position")
        .isEqualTo(160f)
    Assertions.assertThat(view.y)
        .describedAs("view Y axis should be at start position")
        .isEqualTo(160f)
    stateCaller.update()
    lithoViewRule.idle()

    // X after state update should be at 160 because is going to be animated.
    Assertions.assertThat(view.x).describedAs("view X axis after toggle").isEqualTo(160f)
    // Y moves without animating
    Assertions.assertThat(view.y).describedAs("view Y axis after toggle").isEqualTo(0f)
    transitionTestRule.step(5)

    // Check kdoc for how we calculate this value.
    Assertions.assertThat(view.x).describedAs("view X axis after 5 frames").isEqualTo(93.89186f)
    Assertions.assertThat(view.y).describedAs("view Y axis after 5 frames").isEqualTo(0f)

    // This line would unmount the animating mountitem and the framework should stop the animation.
    testLithoView.lithoView.unmountAllItems()

    // After unmounting all items it should not crash.
    transitionTestRule.step(5)
  }

  @Test
  fun animation_reUsingLithoViewWithDifferentComponentTrees_shouldNotCrash() {
    val componentContext = ComponentContext(ApplicationProvider.getApplicationContext<Context>())

    // We measure and layout this non animating component to initialize the transition extension.
    val testLithoView = lithoViewRule.render { nonAnimatingComponent }

    // We need an other litho view where we are going to measure and layout an other similar tree
    // (the real difference here is that the root components are not animating)
    val lithoView = LithoView(componentContext)
    val nonAnimatingComponentTree = ComponentTree.create(componentContext).build()
    nonAnimatingComponentTree.root = nonAnimatingComponent
    lithoView.componentTree = nonAnimatingComponentTree
    lithoView.measure(DEFAULT_WIDTH_SPEC, DEFAULT_HEIGHT_SPEC)
    lithoView.layout(0, 0, lithoView.measuredWidth, lithoView.measuredHeight)

    // Now we need a new component tree that will hold a component tree that holds an animating root
    // component.
    val animatingComponentTree = ComponentTree.create(componentContext).build()
    animatingComponentTree.root = animatingXPropertyComponent
    testLithoView.useComponentTree(animatingComponentTree)
    // We measure this component tree so we initialize the mRootTransition in the extension, but we
    // end up not running a layout here.
    testLithoView.measure()

    // Finally we set a new animating component tree to the initial litho view and run measure and
    // layout.
    testLithoView.useComponentTree(nonAnimatingComponentTree)
    testLithoView.measure().layout()
    // Should not crash.
  }

  @Test
  fun animation_unmountParentBeforeChildDisappearAnimation_shouldNotCrash() {
    // Disabling drawable outputs to ensure a nest heirachy rather than a list of drawables.
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(true)
    val component =
        TestAnimationsComponent(
            stateCaller,
            Transition.create(TRANSITION_KEY)
                .animator(Transition.timing(144))
                .animate(AnimatedProperties.ALPHA)
                .disappearTo(0f)) { state ->
              Column {
                child(
                    Row(
                        style =
                            Style.height(50.dp)
                                .width(50.dp)
                                .backgroundColor(Color.YELLOW)
                                .viewTag("parent_of_parent")) {
                          child(
                              Row(
                                  style =
                                      Style.height(25.dp)
                                          .width(25.dp)
                                          .backgroundColor(Color.RED)
                                          .viewTag(
                                              "parent")) { // This is the parent that will unmount
                                    child(
                                        if (!state) // Disappearing child
                                            Row(
                                                style =
                                                    Style.height(10.dp)
                                                        .width(10.dp)
                                                        .backgroundColor(Color.BLUE)
                                                        .transitionKey(context, TRANSITION_KEY)
                                                        .viewTag(TRANSITION_KEY))
                                        else null)
                                  })
                        })
              }
            }
    val testLithoView = lithoViewRule.render { component }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()
    transitionTestRule.step(1)

    // Grab the parent of the parent
    val parentOfParent = testLithoView.findViewWithTagOrNull("parent_of_parent") as ComponentHost?

    // Grab the id of the 1st child - this is the parent we will unmount
    val id = parentOfParent!!.getMountItemAt(0).renderTreeNode.renderUnit.id

    // Manually unmount the parent of the disappearing item
    testLithoView.lithoView.mountDelegateTarget.notifyUnmount(id)

    // Update so the disappearing item triggers a disappear animation.
    // If there's a problem, a crash will occur here.
    stateCaller.update()
    lithoViewRule.idle()

    // Restoring disable drawable outputs configuration
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent()
  }

  @Test
  fun animation_unmountElementMidAppearAnimation_elementShouldBeUnmounted() {
    val component =
        TestAnimationsComponent(
            stateCaller,
            Transition.stagger(
                144,
                Transition.create(TRANSITION_KEY + 0)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.ALPHA)
                    .appearFrom(0f),
                Transition.create(TRANSITION_KEY + 1)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.ALPHA)
                    .appearFrom(0f))) { state ->
              Column {
                child(Row(style = Style.height(50.dp).width(50.dp).backgroundColor(Color.YELLOW)))

                if (state) {
                  for (i in 0..1) {
                    child(
                        Row(
                            style =
                                Style.height(50.dp)
                                    .width(50.dp)
                                    .backgroundColor(Color.RED)
                                    .viewTag(TRANSITION_KEY + i)
                                    .transitionKey(context, TRANSITION_KEY + i)))
                  }
                }
              }
            }
    val testLithoView = lithoViewRule.render { component }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()

    // The bug only happens if you start the animation in the middle twice.
    var view = testLithoView.findViewWithTagOrNull(TRANSITION_KEY + 1)

    // View should be null as state is null
    Assertions.assertThat(view).describedAs("view before appearing").isNull()
    stateCaller.update()
    lithoViewRule.idle()
    view = testLithoView.findViewWithTag(TRANSITION_KEY + 1)
    Assertions.assertThat(view).describedAs("view after toggle").isNotNull
    // After state update we should have the view added but with alpha equal to 0
    Assertions.assertThat(view.getAlpha()).describedAs("view after toggle").isEqualTo(0f)
    transitionTestRule.step(11)
    // Update state again the element should not be there.
    stateCaller.update()
    lithoViewRule.idle()
    view = testLithoView.findViewWithTagOrNull(TRANSITION_KEY + 1)
    Assertions.assertThat(view).describedAs("view unmount mid animation").isNull()
    transitionTestRule.step(1)
    // Now if we do this again we expect the appearing items to the same thing.
    stateCaller.update()
    lithoViewRule.idle()
    transitionTestRule.step(1)
    view = testLithoView.findViewWithTag(TRANSITION_KEY + 1)
    Assertions.assertThat(view).describedAs("view after toggle").isNotNull
    // After state update we should have the view added but with alpha equal to 0
    Assertions.assertThat(view.alpha).describedAs("view after toggle").isEqualTo(0f)
    stateCaller.update()
    lithoViewRule.idle()
    view = testLithoView.findViewWithTagOrNull(TRANSITION_KEY + 1)
    Assertions.assertThat(view).describedAs("view unmount mid animation").isNull()
  }

  @Test
  fun animation_disappearAnimationMovingAnItemToTheSameIndex_elementShouldDisappearWithoutCrashing() {
    val component =
        TestAnimationsComponent(
            stateCaller,
            Transition.create("text_like")
                .animate(AnimatedProperties.ALPHA)
                .appearFrom(0f)
                .disappearTo(0f)
                .animate(AnimatedProperties.X)
                .appearFrom(DimensionValue.widthPercentageOffset(50f))
                .disappearTo(DimensionValue.widthPercentageOffset(50f))) { state ->
              if (!state)
                  Row(style = Style.backgroundColor(Color.WHITE).height(56.dp)) {
                    child(
                        Row(
                            style = Style.widthPercent(33.3f),
                            alignItems = YogaAlign.CENTER,
                            justifyContent = YogaJustify.CENTER) {
                              child(
                                  Text(
                                      textSize = 16.sp,
                                      text = "Comment",
                                      style = Style.margin(left = 8.dp)))
                            })
                    child(
                        Row(
                            style = Style.widthPercent(33.3f),
                            alignItems = YogaAlign.CENTER,
                            justifyContent = YogaJustify.CENTER) {
                              child(
                                  Column(
                                      style =
                                          Style.height(24.dp)
                                              .width(24.dp)
                                              .backgroundColor(Color.RED)))
                              child(
                                  Text(
                                      textSize = 16.sp,
                                      text = "Like",
                                      style =
                                          Style.transitionKey(context, "text_like")
                                              .margin(left = 8.dp)))
                            })
                  }
              else
                  Row(style = Style.backgroundColor(Color.WHITE).height(56.dp)) {
                    child(
                        Row(
                            alignItems = YogaAlign.CENTER,
                            justifyContent = YogaJustify.CENTER,
                            style = Style.padding(horizontal = 16.dp)) {
                              child(
                                  Column(
                                      style =
                                          Style.transitionKey(context, "icon_like")
                                              .height(24.dp)
                                              .width(24.dp)
                                              .backgroundColor(Color.RED)))
                            })
                    child(
                        Row(
                            style = Style.widthPercent(33.3f),
                            alignItems = YogaAlign.CENTER,
                            justifyContent = YogaJustify.CENTER) {
                              child(
                                  Text(
                                      textSize = 16.sp,
                                      text = "Comment",
                                      style = Style.margin(left = 8.dp)))
                            })
                  }
            }
    val testLithoView = lithoViewRule.render { component }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()

    // We move 100 frames to be sure any appearing animation finished.
    transitionTestRule.step(100)
    stateCaller.update()
    lithoViewRule.idle()
    // We move an other 100 frames to be sure disappearing animations are done.
    transitionTestRule.step(100)
    transitionTestRule.step(100)

    // Do not crash.
  }

  @Test
  fun animationTransitionsExtension_reUsingLithoViewWithSameComponentTrees_shouldNotCrash() {
    val componentContext = lithoViewRule.context
    val testLithoView = lithoViewRule.createTestLithoView()
    val animatingComponentTree = ComponentTree.create(componentContext).build()
    animatingComponentTree.root = animatingXPropertyComponent
    testLithoView.useComponentTree(animatingComponentTree)
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()
    stateCaller.update()
    lithoViewRule.idle()
    transitionTestRule.step(5)
    val lithoView1 = testLithoView.lithoView
    lithoView1.componentTree = null
    val lithoView2 = LithoView(componentContext)
    lithoView2.componentTree = animatingComponentTree
    testLithoView.useLithoView(lithoView2).attachToWindow().measure().layout()
    animatingComponentTree.root = nonAnimatingComponent
    stateCaller.update()
    lithoViewRule.idle()
    transitionTestRule.step(1)
    lithoView2.componentTree = null
    lithoView1.componentTree = animatingComponentTree
    testLithoView.useLithoView(lithoView1)
    transitionTestRule.step(1000)
  }

  @Test
  fun animationProperties_animatingPropertyOnRootComponent_elementShouldAnimateInTheXAxis() {
    val component =
        TestAnimationsComponent(
            stateCaller,
            Transition.create(TRANSITION_KEY)
                .animator(Transition.timing(144))
                .animate(AnimatedProperties.X)
                .animate(AnimatedProperties.Y)
                .animate(AnimatedProperties.WIDTH)
                .animate(AnimatedProperties.HEIGHT)) { state ->
              Row(
                  style =
                      Style.height(if (state) 200.dp else 100.dp)
                          .width(if (state) 200.dp else 100.dp)
                          .backgroundColor(Color.RED)
                          .position(left = if (!state) 0.px else 100.px)
                          .position(top = if (!state) 0.px else 100.px)
                          .transitionKey(context, TRANSITION_KEY))
            }
    val testLithoView = lithoViewRule.render { component }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()
    val lithoView: View = testLithoView.lithoView

    // 160 is equal to height and width of 200 - 40 for the size of the row.
    Assertions.assertThat(lithoView.x)
        .describedAs("view X axis should be at start position")
        .isEqualTo(0f)
    Assertions.assertThat(lithoView.y)
        .describedAs("view Y axis should be at start position")
        .isEqualTo(0f)
    Assertions.assertThat(lithoView.width)
        .describedAs("view Width should be at start position")
        .isEqualTo(320)
    Assertions.assertThat(lithoView.height)
        .describedAs("view Height should be at start position")
        .isEqualTo(414)
    stateCaller.update()
    lithoViewRule.idle()

    // X after state update should be at 0 because is going to be animated.
    Assertions.assertThat(lithoView.x).describedAs("view X axis after toggle").isEqualTo(0f)
    // Y after state update should be at 0 because is going to be animated.
    Assertions.assertThat(lithoView.y).describedAs("view Y axis after toggle").isEqualTo(0f)
    // Width after state update should be same as original because is going to be animated.
    Assertions.assertThat(lithoView.width).describedAs("view Width after toggle").isEqualTo(320)
    // Height after state update should be same as original because is going to be animated.
    Assertions.assertThat(lithoView.height).describedAs("view Height after toggle").isEqualTo(414)
    transitionTestRule.step(5)

    // Check kdoc for how we calculate this value.
    Assertions.assertThat(lithoView.x)
        .describedAs("view X axis after 5 frames")
        .isEqualTo(41.31759f)
    Assertions.assertThat(lithoView.y)
        .describedAs("view Y axis after 5 frames")
        .isEqualTo(28.238356f)
    Assertions.assertThat(lithoView.width)
        .describedAs("view Width axis after 5 frames")
        .isEqualTo(128)
    Assertions.assertThat(lithoView.height)
        .describedAs("view Height axis after 5 frames")
        .isEqualTo(128)

    // Enough frames to finish all animations
    transitionTestRule.step(500)
    Assertions.assertThat(lithoView.x)
        .describedAs("view X axis after animation finishes")
        .isEqualTo(100f)
    Assertions.assertThat(lithoView.y)
        .describedAs("view Y axis after animation finishes")
        .isEqualTo(100f)
    Assertions.assertThat(lithoView.width)
        .describedAs("view Width after animation finishes")
        .isEqualTo(200)
    Assertions.assertThat(lithoView.height)
        .describedAs("view Height after animation finishes")
        .isEqualTo(200)
  }

  private val animatingXPropertyComponent: TestAnimationsComponent
    get() =
        TestAnimationsComponent(
            stateCaller,
            Transition.create(TRANSITION_KEY)
                .animator(Transition.timing(144))
                .animate(AnimatedProperties.X)) { state ->
              Row(
                  justifyContent = if (state) YogaJustify.FLEX_START else YogaJustify.FLEX_END,
                  alignItems = if (state) YogaAlign.FLEX_START else YogaAlign.FLEX_END,
                  style = Style.height(200.dp).width(200.dp)) {
                    child(
                        Row(
                            style =
                                Style.height(40.dp)
                                    .width(40.dp)
                                    .backgroundColor(Color.parseColor("#ee1111"))
                                    .transitionKey(context, TRANSITION_KEY)
                                    .viewTag(TRANSITION_KEY)))
                  }
            }

  private val nonAnimatingComponent: Component
    get() =
        TestAnimationsComponent(stateCaller, null) { state ->
          Row(
              style = Style.height(200.dp).width(200.dp),
              justifyContent = if (state) YogaJustify.FLEX_START else YogaJustify.FLEX_END,
              alignItems = if (state) YogaAlign.FLEX_START else YogaAlign.FLEX_END) {
                child(
                    Row(
                        style =
                            Style.height(40.dp)
                                .width(40.dp)
                                .backgroundColor(Color.parseColor("#ee1111"))))
              }
        }

  @Test
  fun transitionAnimation_interruption_overridesCurrentTransition() {
    val component =
        TestAnimationsComponent(
            stateCaller,
            Transition.create(TRANSITION_KEY)
                .animator(Transition.timing(160, null))
                .animate(AnimatedProperties.X)) { state ->
              Row(
                  style = Style.width(200.dp),
                  justifyContent = if (state) YogaJustify.FLEX_START else YogaJustify.FLEX_END) {
                    child(
                        Row(
                            style =
                                Style.height(40.dp)
                                    .width(40.dp)
                                    .transitionKey(context, TRANSITION_KEY)
                                    .viewTag(TRANSITION_KEY)))
                  }
            }
    val testLithoView = lithoViewRule.render { component }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()
    val view = testLithoView.findViewWithTag(TRANSITION_KEY)
    Assertions.assertThat(view.x).describedAs("x pos before transition").isEqualTo(160f)

    // Start the transition by changing the state
    stateCaller.update()
    lithoViewRule.idle()

    // Advance to the mid point of the transition
    transitionTestRule.step(6)
    Assertions.assertThat(view.x).describedAs("x pos at transition midpoint").isEqualTo(80f)

    // Trigger a new transition that interrupts the current transition and  returns the component to
    // its original position. NB: The Transition is fixed time, so it will take longer to return
    stateCaller.update()
    lithoViewRule.idle()

    // Advance to the mid point of the return transition
    transitionTestRule.step(6)
    Assertions.assertThat(view.x).describedAs("x pos at return transition midpoint").isEqualTo(120f)

    // Advance to the end of the return transition
    transitionTestRule.step(5)
    Assertions.assertThat(view.x).describedAs("x pos after return transition").isEqualTo(160f)
  }

  @Test
  fun animation_disappearAnimationOnNestedLithoViews_elementShouldDisappearAnimatingAlpha() {
    val innerStateCaller = StateCaller()
    val component =
        TestAnimationsComponent(
            stateCaller,
            Transition.create(TRANSITION_KEY)
                .animator(Transition.timing(144))
                .animate(AnimatedProperties.ALPHA)
                .appearFrom(0f)
                .disappearTo(0f)) { state ->
              Column {
                child(Row(style = Style.height(50.dp).width(50.dp).backgroundColor(Color.YELLOW)))
                child(TestAnimationMount(innerStateCaller))
                child(
                    if (!state)
                        Row(
                            style =
                                Style.height(50.dp)
                                    .width(50.dp)
                                    .backgroundColor(Color.RED)
                                    .viewTag(TRANSITION_KEY)
                                    .transitionKey(context, TRANSITION_KEY))
                    else null)
              }
            }

    val testLithoView = lithoViewRule.render { component }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()

    // We move 10 frames to account for the appear animation.
    transitionTestRule.step(10)
    val view = testLithoView.findViewWithTag(TRANSITION_KEY)
    var innerView = testLithoView.findViewWithTag("TestAnimationMount")
    // Here we get inner LithoView
    val innerLithoView = innerView.parent as LithoView

    // Update state on both
    stateCaller.update()
    innerStateCaller.update()
    lithoViewRule.idle()
    // We look for the same view
    innerView = testLithoView.findViewWithTag("TestAnimationMount")
    Assertions.assertThat(innerLithoView)
        .describedAs("We mantain the same LithoView")
        .isEqualTo(innerView.parent)
  }

  @Test
  fun animation_animatingComponentAndChangingHost_elementShouldAnimateOnlyOnce() {
    val secondLithoView = LithoView(activityController.get())
    secondLithoView.componentTree = null
    val component = animatingXPropertyComponent
    val testLithoView = lithoViewRule.render { component }
    val fl = FrameLayout(activityController.get())
    val componentTree = testLithoView.componentTree
    fl.addView(testLithoView.lithoView)
    fl.addView(secondLithoView)
    activityController.get().setContentView(fl)
    activityController.resume().visible()
    var view = testLithoView.findViewWithTag(TRANSITION_KEY)

    // 160 is equal to height and width of 200 - 40 for the size of the row.
    Assertions.assertThat(view.x)
        .describedAs("view X axis should be at start position")
        .isEqualTo(160f)
    Assertions.assertThat(view.y)
        .describedAs("view Y axis should be at start position")
        .isEqualTo(160f)
    testLithoView.useComponentTree(null)
    secondLithoView.componentTree = componentTree
    view = secondLithoView.findViewWithTag(TRANSITION_KEY)
    stateCaller.update()
    lithoViewRule.idle()

    // X after state update should be at 160 because is going to be animated.
    Assertions.assertThat(view.x).describedAs("view X axis after toggle").isEqualTo(160f)
    // Y moves without animating
    Assertions.assertThat(view.y).describedAs("view Y axis after toggle").isEqualTo(0f)
    transitionTestRule.step(10)
    Assertions.assertThat(view.x).describedAs("view X axis after 10 frames").isEqualTo(0f)
    Assertions.assertThat(view.y).describedAs("view Y axis after 10 frames").isEqualTo(0f)
    secondLithoView.componentTree = null
    testLithoView.useComponentTree(componentTree)
    view = testLithoView.findViewWithTag(TRANSITION_KEY)
    Assertions.assertThat(view.x).describedAs("view X axis after 10 frames").isEqualTo(0f)
    Assertions.assertThat(view.y).describedAs("view Y axis after 10 frames").isEqualTo(0f)
  }

  @Test
  fun animation_disappearAnimationWhichRemountsToRoot_shouldPreserveInitialXandY() {
    val innerStateCaller = StateCaller()
    val component =
        TestAnimationsComponent(
            stateCaller,
            Transition.create(TRANSITION_KEY)
                .animator(Transition.timing(144))
                .animate(AnimatedProperties.ALPHA)
                .disappearTo(0f)) { state ->
              Column(style = Style.wrapInView()) {
                child(Row(style = Style.height(50.dp).width(50.dp).backgroundColor(Color.YELLOW)))
                child(TestAnimationMount(innerStateCaller))
                child(
                    Row(style = Style.wrapInView()) {
                      child(
                          if (!state)
                              Row(
                                  style =
                                      Style.height(50.dp)
                                          .width(50.dp)
                                          .backgroundColor(Color.RED)
                                          .viewTag(TRANSITION_KEY)
                                          .transitionKey(context, TRANSITION_KEY))
                          else null)
                    })
              }
            }
    val testLithoView = lithoViewRule.render { component }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()
    val location = IntArray(2)
    var animatingView = testLithoView.findViewWithTag(TRANSITION_KEY)
    animatingView.getLocationOnScreen(location)
    val initialX = location[0]
    val initialY = location[1]

    // Update state on both
    stateCaller.update()
    innerStateCaller.update()
    lithoViewRule.idle()

    // We look for the same view
    animatingView = testLithoView.findViewWithTag(TRANSITION_KEY)
    animatingView.getLocationOnScreen(location)
    Assertions.assertThat(animatingView.alpha).isEqualTo(1f)
    Assertions.assertThat(location[0])
        .describedAs("initial x should be the same")
        .isEqualTo(initialX)
    Assertions.assertThat(location[1])
        .describedAs("initial y should be the same")
        .isEqualTo(initialY)
    transitionTestRule.step(2)
    animatingView.getLocationOnScreen(location)
    Assertions.assertThat(animatingView.alpha).isGreaterThan(0f).isLessThan(1f)
    Assertions.assertThat(location[0])
        .describedAs("x during animation should be the same")
        .isEqualTo(initialX)
    Assertions.assertThat(location[1])
        .describedAs("y during animation should be the same")
        .isEqualTo(initialY)
    transitionTestRule.step(100)
    Assertions.assertThat(testLithoView.findViewWithTagOrNull(TRANSITION_KEY)).isNull()
  }

  /**
   * Same as animation_disappearAnimationWhichRemountsToRoot_shouldPreserveInitialXandY but with
   * parents with translation set.
   */
  @Test
  fun animation_disappearAnimationWhichRemountsToRoot_shouldPreserveInitialXandYWithParentTranslation() {
    val translation = DynamicValue(10f)
    val innerStateCaller = StateCaller()
    val component =
        TestAnimationsComponent(
            stateCaller,
            Transition.create(TRANSITION_KEY)
                .animator(Transition.timing(144))
                .animate(AnimatedProperties.ALPHA)
                .disappearTo(0f)) { state ->
              Column(
                  style = Style.translationX(translation).translationY(translation).wrapInView()) {
                    child(
                        Row(style = Style.height(50.dp).width(50.dp).backgroundColor(Color.YELLOW)))
                    child(TestAnimationMount(innerStateCaller))
                    child(
                        Row(
                            style =
                                Style.translationX(translation)
                                    .translationY(translation)
                                    .wrapInView()) {
                              child(
                                  if (!state)
                                      Row(
                                          style =
                                              Style.height(50.dp)
                                                  .width(50.dp)
                                                  .backgroundColor(Color.RED)
                                                  .viewTag(TRANSITION_KEY)
                                                  .transitionKey(context, TRANSITION_KEY))
                                  else null)
                            })
                  }
            }
    val testLithoView = lithoViewRule.render { component }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()
    val location = IntArray(2)
    var animatingView = testLithoView.findViewWithTag(TRANSITION_KEY)
    animatingView.getLocationOnScreen(location)
    val initialX = location[0]
    val initialY = location[1]

    // Update state on both
    stateCaller.update()
    innerStateCaller.update()
    lithoViewRule.idle()

    // We look for the same view
    animatingView = testLithoView.findViewWithTag(TRANSITION_KEY)
    animatingView.getLocationOnScreen(location)
    Assertions.assertThat(animatingView.alpha).isEqualTo(1f)
    Assertions.assertThat(location[0])
        .describedAs("initial x should be the same")
        .isEqualTo(initialX)
    Assertions.assertThat(location[1])
        .describedAs("initial y should be the same")
        .isEqualTo(initialY)
    transitionTestRule.step(2)
    animatingView.getLocationOnScreen(location)
    Assertions.assertThat(animatingView.alpha).isGreaterThan(0f).isLessThan(1f)
    Assertions.assertThat(location[0])
        .describedAs("x during animation should be the same")
        .isEqualTo(initialX)
    Assertions.assertThat(location[1])
        .describedAs("y during animation should be the same")
        .isEqualTo(initialY)
    transitionTestRule.step(100)
    Assertions.assertThat(testLithoView.findViewWithTagOrNull(TRANSITION_KEY)).isNull()
  }

  @Test
  fun animation_disappearAnimationNewComponentTree_disappearingElementsContentsRemoved() {
    val component =
        TestAnimationsComponent(
            stateCaller,
            Transition.create(TRANSITION_KEY)
                .animator(Transition.timing(144))
                .animate(AnimatedProperties.ALPHA)
                .appearFrom(0f)
                .disappearTo(0f)) { state ->
              Column {
                child(Row(style = Style.height(50.dp).width(50.dp).backgroundColor(Color.YELLOW)))
                child(
                    if (!state)
                        Row(
                            style =
                                Style.height(50.dp)
                                    .width(50.dp)
                                    .backgroundColor(Color.RED)
                                    .viewTag(TRANSITION_KEY)
                                    .transitionKey(context, TRANSITION_KEY))
                    else null)
              }
            }

    val nonAnimatingComponent = nonAnimatingComponent
    val newComponentTree = ComponentTree.create(lithoViewRule.context).build()
    newComponentTree.root = nonAnimatingComponent
    val testLithoView = lithoViewRule.render { component }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()

    // We move 10 frames to account for the appear animation.
    transitionTestRule.step(10)
    var view = testLithoView.findViewWithTag(TRANSITION_KEY)
    // The view is not null
    Assertions.assertThat(view).describedAs("view before disappearing").isNotNull
    Assertions.assertThat(view.alpha).describedAs("view before disappearing").isEqualTo(1f)
    stateCaller.update()
    lithoViewRule.idle()
    view = testLithoView.findViewWithTag(TRANSITION_KEY)

    // After state update, even if the row was removed from the component, it still not null as we
    // are going to animate it. Alpha stays at 1 before advancing frames.
    Assertions.assertThat(view).describedAs("view after toggle").isNotNull
    Assertions.assertThat(view.alpha).describedAs("view after toggle").isEqualTo(1f)
    transitionTestRule.step(5)
    // Check kdoc for how we calculate this value.
    Assertions.assertThat(view.alpha).describedAs("view after 5 frames").isEqualTo(0.5868241f)
    Assertions.assertThat(
            Whitebox.invokeMethod<Any>(testLithoView.lithoView, "hasDisappearingItems") as Boolean)
        .describedAs("root host has disappearing items before updating the tree")
        .isTrue

    // Change component tree mid animation.
    testLithoView.useComponentTree(newComponentTree)
    Assertions.assertThat(
            Whitebox.invokeMethod<Any>(testLithoView.lithoView, "hasDisappearingItems") as Boolean)
        .describedAs("root host does not have disappearing items after setting tree")
        .isFalse
    val nullableView = testLithoView.findViewWithTagOrNull(TRANSITION_KEY)
    Assertions.assertThat(nullableView).describedAs("view after setting new tree").isNull()
  }

  @Test
  fun animation_clipChildren_shouldBeFalseDuringAnimation() {
    val component = animatingXPropertyComponent
    val testLithoView = lithoViewRule.render { component }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()
    Assertions.assertThat(testLithoView.lithoView.clipChildren)
        .describedAs("before animation, clip children is set to true")
        .isTrue
    stateCaller.update()
    lithoViewRule.idle()
    transitionTestRule.step(5)
    Assertions.assertThat(testLithoView.lithoView.clipChildren)
        .describedAs("during animation, clip children is set to false")
        .isFalse
    transitionTestRule.step(5)
    Assertions.assertThat(testLithoView.lithoView.clipChildren)
        .describedAs("after animation, clip children is set to true")
        .isTrue
  }

  companion object {
    private const val TRANSITION_KEY = "TRANSITION_KEY"
  }
}
