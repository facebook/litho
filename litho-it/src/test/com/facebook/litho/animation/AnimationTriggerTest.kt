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

import android.os.Looper.getMainLooper
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.EmptyComponent
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode

@OptIn(ExperimentalLithoApi::class)
@LooperMode(LooperMode.Mode.PAUSED)
@RunWith(LithoTestRunner::class)
class AnimationTriggerTest {

  @Rule
  @JvmField
  val lithoTestRule = LithoTestRule(
      ComponentsConfiguration.defaultInstance.copy(enableStateReadTracking = true),
  )

  private var capturedGroup: AnimationGroup<Boolean>? = null
  private var capturedStringGroup: AnimationGroup<String>? = null
  private var capturedLazyTrigger: AnimationTrigger.Lazy? = null

  @Before
  fun setUp() {
    capturedGroup = null
    capturedStringGroup = null
    capturedLazyTrigger = null
  }

  // --- AnimationTrigger.Immediate ---

  @Test
  fun eagerly_useAnimatedFloat_animatesOnTargetChange() {
    val testView = lithoTestRule.render {
      StartAlphaComponent(target = 0f, trigger = AnimationTrigger.Immediate)
    }
    shadowOf(getMainLooper()).idle()
    assertThat(testView.lithoView.alpha).isEqualTo(0f)

    testView.setRoot(StartAlphaComponent(target = 1f, trigger = AnimationTrigger.Immediate))
    shadowOf(getMainLooper()).idle()

    assertThat(testView.lithoView.alpha).isEqualTo(1f)
  }

  @Test
  fun eagerly_useAnimatedFloat_retargetAfterCompletion_animatesToNewTarget() {
    val testView = lithoTestRule.render {
      StartAlphaComponent(target = 0f, trigger = AnimationTrigger.Immediate)
    }
    shadowOf(getMainLooper()).idle()

    testView.setRoot(StartAlphaComponent(target = 1f, trigger = AnimationTrigger.Immediate))
    shadowOf(getMainLooper()).idle()
    assertThat(testView.lithoView.alpha).isEqualTo(1f)

    testView.setRoot(StartAlphaComponent(target = 0.5f, trigger = AnimationTrigger.Immediate))
    shadowOf(getMainLooper()).idle()

    assertThat(testView.lithoView.alpha).isEqualTo(0.5f)
  }

  @Test
  fun eagerly_useAnimationGroup_retargetAfterCompletion_restartsAllAnimations() {
    val testView = lithoTestRule.render {
      StartGroupComponent(expanded = false, trigger = AnimationTrigger.Immediate)
    }
    shadowOf(getMainLooper()).idle()
    assertThat(testView.lithoView.alpha).isEqualTo(0f)

    testView.setRoot(StartGroupComponent(expanded = true, trigger = AnimationTrigger.Immediate))
    shadowOf(getMainLooper()).idle()
    assertThat(testView.lithoView.alpha).isEqualTo(1f)
    assertThat(checkNotNull(capturedGroup).state.currentState).isTrue()

    testView.setRoot(StartGroupComponent(expanded = false, trigger = AnimationTrigger.Immediate))
    shadowOf(getMainLooper()).idle()

    assertThat(testView.lithoView.alpha).isEqualTo(0f)
    assertThat(checkNotNull(capturedGroup).state.currentState).isFalse()
  }

  // --- AnimationTrigger.Lazy + useAnimatedFloat ---

  @Test
  fun lazy_useAnimatedFloat_doesNotAnimateUntilStarted() {
    val testView = lithoTestRule.render { LazyStartAlphaComponent(target = 0f) }
    shadowOf(getMainLooper()).idle()
    assertThat(testView.lithoView.alpha).isEqualTo(0f)

    testView.setRoot(LazyStartAlphaComponent(target = 1f))
    shadowOf(getMainLooper()).idle()

    assertThat(testView.lithoView.alpha).isEqualTo(0f)
  }

  @Test
  fun lazy_useAnimatedFloat_animatesAfterStart() {
    val testView = lithoTestRule.render { LazyStartAlphaComponent(target = 0f) }
    shadowOf(getMainLooper()).idle()

    testView.setRoot(LazyStartAlphaComponent(target = 1f))
    shadowOf(getMainLooper()).idle()
    assertThat(testView.lithoView.alpha).isEqualTo(0f)

    checkNotNull(capturedLazyTrigger).start()
    shadowOf(getMainLooper()).idle()

    assertThat(testView.lithoView.alpha).isEqualTo(1f)
  }

  @Test
  fun lazy_useAnimatedFloat_startCalledMultipleTimes_isIdempotent() {
    val testView = lithoTestRule.render { LazyStartAlphaComponent(target = 0f) }
    shadowOf(getMainLooper()).idle()

    testView.setRoot(LazyStartAlphaComponent(target = 1f))
    shadowOf(getMainLooper()).idle()

    checkNotNull(capturedLazyTrigger).start()
    checkNotNull(capturedLazyTrigger).start()
    shadowOf(getMainLooper()).idle()

    assertThat(testView.lithoView.alpha).isEqualTo(1f)
  }

  @Test
  fun lazy_useAnimatedFloat_retargetBeforeStart_animatesToLatestTarget() {
    val testView = lithoTestRule.render { LazyStartAlphaComponent(target = 0f) }
    shadowOf(getMainLooper()).idle()

    testView.setRoot(LazyStartAlphaComponent(target = 0.5f))
    shadowOf(getMainLooper()).idle()
    assertThat(testView.lithoView.alpha).isEqualTo(0f)

    testView.setRoot(LazyStartAlphaComponent(target = 1f))
    shadowOf(getMainLooper()).idle()
    assertThat(testView.lithoView.alpha).isEqualTo(0f)

    checkNotNull(capturedLazyTrigger).start()
    shadowOf(getMainLooper()).idle()

    assertThat(testView.lithoView.alpha).isEqualTo(1f)
  }

  @Test
  fun lazy_useAnimatedFloat_retargetAfterSettle_animatesWithoutSecondStart() {
    val testView = lithoTestRule.render { LazyStartAlphaComponent(target = 0f) }
    shadowOf(getMainLooper()).idle()

    testView.setRoot(LazyStartAlphaComponent(target = 1f))
    shadowOf(getMainLooper()).idle()
    checkNotNull(capturedLazyTrigger).start()
    shadowOf(getMainLooper()).idle()
    assertThat(testView.lithoView.alpha).isEqualTo(1f)

    // start() latches: a retarget after the animation settled gets a fresh handle, which the
    // already-started trigger must start on arrival rather than re-gate.
    testView.setRoot(LazyStartAlphaComponent(target = 0.5f))
    shadowOf(getMainLooper()).idle()

    assertThat(testView.lithoView.alpha).isEqualTo(0.5f)
  }

  @Test
  fun lazy_startBeforeFirstHandle_armsTheTrigger() {
    val testView = lithoTestRule.render { LazyStartAlphaComponent(target = 0f) }
    shadowOf(getMainLooper()).idle()

    // Already at the target, so no animation is pending and no handle has been offered yet.
    checkNotNull(capturedLazyTrigger).start()

    testView.setRoot(LazyStartAlphaComponent(target = 1f))
    shadowOf(getMainLooper()).idle()

    assertThat(testView.lithoView.alpha).isEqualTo(1f)
  }

  @Test
  fun lazy_useAnimatedFloat_unmountBeforeStart_startIsNoOp() {
    val testView = lithoTestRule.render { LazyStartAlphaComponent(target = 0f) }
    shadowOf(getMainLooper()).idle()

    testView.setRoot(LazyStartAlphaComponent(target = 1f))
    shadowOf(getMainLooper()).idle()

    // Unmount the animated component by replacing with a plain component
    testView.setRoot(EmptyComponent())
    shadowOf(getMainLooper()).idle()

    // start() after unmount should be a safe no-op
    checkNotNull(capturedLazyTrigger).start()
    shadowOf(getMainLooper()).idle()
  }

  // --- AnimationTrigger.Lazy + useAnimationGroup ---

  @Test
  fun lazy_useAnimationGroup_doesNotAnimateUntilStarted() {
    val testView = lithoTestRule.render { LazyStartGroupComponent(expanded = false) }
    shadowOf(getMainLooper()).idle()
    assertThat(testView.lithoView.alpha).isEqualTo(0f)

    testView.setRoot(LazyStartGroupComponent(expanded = true))
    shadowOf(getMainLooper()).idle()

    assertThat(testView.lithoView.alpha).isEqualTo(0f)
  }

  @Test
  fun lazy_useAnimationGroup_animatesAfterStart() {
    val testView = lithoTestRule.render { LazyStartGroupComponent(expanded = false) }
    shadowOf(getMainLooper()).idle()

    testView.setRoot(LazyStartGroupComponent(expanded = true))
    shadowOf(getMainLooper()).idle()
    assertThat(testView.lithoView.alpha).isEqualTo(0f)

    checkNotNull(capturedLazyTrigger).start()
    shadowOf(getMainLooper()).idle()

    assertThat(testView.lithoView.alpha).isEqualTo(1f)
  }

  @Test
  fun lazy_useAnimationGroup_stateIsConsistentBeforeStart() {
    lithoTestRule.render { LazyStartGroupComponent(expanded = false) }
    shadowOf(getMainLooper()).idle()

    val capturedGroup = checkNotNull(capturedGroup)
    assertThat(capturedGroup.state.currentState).isFalse()
    assertThat(capturedGroup.targetState).isFalse()
    assertThat(capturedGroup.state.isRunning).isFalse()
  }

  @Test
  fun lazy_useAnimationGroup_completionUpdatesCurrentStateAfterStart() {
    val testView = lithoTestRule.render { LazyStartGroupComponent(expanded = false) }
    shadowOf(getMainLooper()).idle()

    testView.setRoot(LazyStartGroupComponent(expanded = true))
    shadowOf(getMainLooper()).idle()
    assertThat(checkNotNull(capturedGroup).state.currentState).isFalse()

    checkNotNull(capturedLazyTrigger).start()
    shadowOf(getMainLooper()).idle()

    val capturedGroup = checkNotNull(capturedGroup)
    assertThat(capturedGroup.state.currentState).isTrue()
    assertThat(capturedGroup.targetState).isTrue()
    assertThat(capturedGroup.state.isRunning).isFalse()
  }

  @Test
  fun lazy_useAnimationGroup_retargetBeforeStart_animatesToLatestState() {
    val testView = lithoTestRule.render { LazyStartThreeStateComponent(state = "A") }
    shadowOf(getMainLooper()).idle()
    assertThat(testView.lithoView.alpha).isEqualTo(0f)

    testView.setRoot(LazyStartThreeStateComponent(state = "B"))
    shadowOf(getMainLooper()).idle()
    assertThat(testView.lithoView.alpha).isEqualTo(0f)

    testView.setRoot(LazyStartThreeStateComponent(state = "C"))
    shadowOf(getMainLooper()).idle()
    assertThat(testView.lithoView.alpha).isEqualTo(0f)

    checkNotNull(capturedLazyTrigger).start()
    shadowOf(getMainLooper()).idle()

    assertThat(testView.lithoView.alpha).isEqualTo(1f)
    assertThat(checkNotNull(capturedStringGroup).state.currentState).isEqualTo("C")
  }

  // --- Test components ---

  private class StartAlphaComponent(
      private val target: Float,
      private val trigger: AnimationTrigger,
  ) : KComponent() {
    override fun ComponentScope.render(): Component? {
      val animatedAlpha = useAnimatedFloat(target, spec = snap(), trigger = trigger)
      return Row(style = Style.width(100.px).height(100.px).alpha(animatedAlpha))
    }
  }

  private inner class LazyStartAlphaComponent(
      private val target: Float,
  ) : KComponent() {
    override fun ComponentScope.render(): Component? {
      val trigger = useLazyAnimationTrigger()
      capturedLazyTrigger = trigger
      val animatedAlpha = useAnimatedFloat(target, spec = snap(), trigger = trigger)
      return Row(style = Style.width(100.px).height(100.px).alpha(animatedAlpha))
    }
  }

  private inner class StartGroupComponent(
      private val expanded: Boolean,
      private val trigger: AnimationTrigger,
  ) : KComponent() {
    override fun ComponentScope.render(): Component? {
      val group = useAnimationGroup(expanded, trigger = trigger)
      capturedGroup = group
      val alpha =
          group.animateFloat(
              transitionSpec = { snap() },
          ) { state ->
            if (state) 1f else 0f
          }
      return Row(style = Style.width(100.px).height(100.px).alpha(alpha))
    }
  }

  private inner class LazyStartGroupComponent(
      private val expanded: Boolean,
  ) : KComponent() {
    override fun ComponentScope.render(): Component? {
      val trigger = useLazyAnimationTrigger()
      capturedLazyTrigger = trigger
      val group = useAnimationGroup(expanded, trigger = trigger)
      capturedGroup = group
      val alpha =
          group.animateFloat(
              transitionSpec = { snap() },
          ) { state ->
            if (state) 1f else 0f
          }
      return Row(style = Style.width(100.px).height(100.px).alpha(alpha))
    }
  }

  private inner class StartThreeStateComponent(
      private val state: String,
      private val trigger: AnimationTrigger,
  ) : KComponent() {
    override fun ComponentScope.render(): Component? {
      val group = useAnimationGroup(state, trigger = trigger)
      capturedStringGroup = group
      val alpha =
          group.animateFloat(
              transitionSpec = { snap() },
          ) { s ->
            when (s) {
              "A" -> 0f
              "B" -> 0.5f
              else -> 1f
            }
          }
      return Row(style = Style.width(100.px).height(100.px).alpha(alpha))
    }
  }

  private inner class LazyStartThreeStateComponent(
      private val state: String,
  ) : KComponent() {
    override fun ComponentScope.render(): Component? {
      val trigger = useLazyAnimationTrigger()
      capturedLazyTrigger = trigger
      val group = useAnimationGroup(state, trigger = trigger)
      capturedStringGroup = group
      val alpha =
          group.animateFloat(
              transitionSpec = { snap() },
          ) { s ->
            when (s) {
              "A" -> 0f
              "B" -> 0.5f
              else -> 1f
            }
          }
      return Row(style = Style.width(100.px).height(100.px).alpha(alpha))
    }
  }
}
