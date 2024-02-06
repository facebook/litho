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

package com.facebook.litho.animated

import android.os.Looper.getMainLooper
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.DynamicValue
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.PAUSED)
@RunWith(LithoTestRunner::class)
class AnimatedTest {

  // TODO(t112256774): Re-enable AnimatedTest tests. See https://fburl.com/h50b38s9 for more details

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  private lateinit var listener: AnimationFinishListener
  private lateinit var listener2: AnimationFinishListener
  private lateinit var listener3: AnimationFinishListener

  @Test(expected = IllegalStateException::class)
  fun startAnimation_called_twice_expect_IllegalStateException() {
    listener = mock()
    listener2 = mock()
    val alphaProgress = DynamicValue(0f)
    val animation =
        Animated.timing(
            target = alphaProgress, to = 1f, duration = 2000, animationFinishListener = listener)
    val animation2 =
        Animated.timing(
            target = alphaProgress, to = 1f, duration = 2000, animationFinishListener = listener2)
    val sequence = Animated.sequence(animation, animation2)
    sequence.start()
    sequence.start()
    shadowOf(getMainLooper()).idle()
  }

  @Test
  fun cancelAnimation_called_twice_expect_one_cancel_call_behaviour() {
    listener = mock()
    listener2 = mock()
    listener3 = mock()
    val alphaProgress = DynamicValue(0f)
    val animation =
        Animated.timing(
            target = alphaProgress, to = 1f, duration = 2000, animationFinishListener = listener)
    val animation2 =
        Animated.timing(
            target = alphaProgress, to = 1f, duration = 2000, animationFinishListener = listener2)
    val sequence = Animated.sequence(animation, animation2)
    sequence.addListener(listener3)
    sequence.start()
    sequence.cancel()
    sequence.cancel()
    shadowOf(getMainLooper()).idle()
    verify(
            listener,
            times(1).description("timing animation listener called onFinish(cancelled = true)"))
        .onFinish(true)
    verify(
            listener,
            never()
                .description(
                    "timing animation listener didn't call onFinish(cancelled = false) as it was cancelled"))
        .onFinish(false)
    verify(
            listener3,
            times(1)
                .description("main sequence animation listener called onFinish(cancelled = true)"))
        .onFinish(true)
    verify(listener2, never().description("timing2 animation listener not invoked")).onFinish(any())
  }

  @Test
  fun timingAnimation_whenAnimationFinish_alphaValueChange() {
    val alphaProgress = DynamicValue(0f)
    val animation = Animated.timing(target = alphaProgress, to = 1f, duration = 1000)
    val testLithoview = lithoViewRule.render { TestComponent(alphaProgress = alphaProgress) }

    val view = testLithoview.lithoView
    assertThat(view.alpha).isEqualTo(0f).describedAs("initial value")
    animation.start()
    shadowOf(getMainLooper()).idle()
    assertThat(view.alpha).isEqualTo(1f).describedAs("value after animation")
  }

  @Test
  fun timingAnimation_whenAnimationFinish_onFinishCallbackCalled() {
    listener = mock()
    listener2 = mock()
    val alphaProgress = DynamicValue(0f)
    val animation =
        Animated.timing(
            target = alphaProgress, to = 1f, duration = 1000, animationFinishListener = listener)
    animation.addListener(listener2)
    animation.start()
    shadowOf(getMainLooper()).idle()
    verify(listener, times(1).description("timing animation listener should finish"))
        .onFinish(false)
    verify(
            listener,
            never()
                .description("timing animation listener shouldnt call onFinish(cancelled = true)"))
        .onFinish(true)
    verify(listener2, times(1).description("main animation listener should finish")).onFinish(false)
    verify(
            listener2,
            never().description("main animation listener shouldnt call onFinish(cancelled = true)"))
        .onFinish(true)
  }

  @Test
  fun springAnimation_whenAnimationFinish_onFinishCallbackCalled() {
    listener = mock()
    listener2 = mock()
    val alphaProgress = DynamicValue(0f)
    val animation =
        Animated.spring(
            target = alphaProgress,
            to = 1f,
            SpringConfig(stiffness = 50f, dampingRatio = 0.5f),
            animationFinishListener = listener)
    animation.addListener(listener2)
    animation.start()
    shadowOf(getMainLooper()).idle()
    verify(listener, times(1).description("timing animation listener should finish"))
        .onFinish(false)
    verify(
            listener,
            never()
                .description("timing animation listener shouldnt call onFinish(cancelled = true)"))
        .onFinish(true)
    verify(listener2, times(1).description("main animation listener should finish")).onFinish(false)
    verify(
            listener2,
            never().description("main animation listener shouldnt call onFinish(cancelled = true)"))
        .onFinish(true)
  }

  @Test
  fun sequenceAnimation_whenMainAnimationCancelledImmediately_onCancelCallbackCalled() {
    listener = mock()
    listener2 = mock()
    listener3 = mock()
    val alphaProgress = DynamicValue(0f)
    val animation =
        Animated.timing(
            target = alphaProgress, to = 1f, duration = 2000, animationFinishListener = listener)
    val animation2 =
        Animated.timing(
            target = alphaProgress, to = 1f, duration = 2000, animationFinishListener = listener2)
    val sequence = Animated.sequence(animation, animation2)
    sequence.addListener(listener3)
    sequence.start()
    sequence.cancel()
    shadowOf(getMainLooper()).idle()
    // verify that if we cancel main animation straight away the first animation and the main
    // animation onCancel listeners will be invoked
    verify(
            listener,
            times(1).description("timing animation listener called onFinish(cancelled = true)"))
        .onFinish(true)
    verify(
            listener,
            never()
                .description(
                    "timing animation listener didn't call onFinish(cancelled = false) as it was cancelled"))
        .onFinish(false)
    verify(
            listener3,
            times(1)
                .description("main sequence animation listener called onFinish(cancelled = true)"))
        .onFinish(true)
    verify(listener2, never().description("timing2 animation listener not invoked")).onFinish(any())
  }

  @Test
  fun sequenceAnimation_whenAnimationCancelledImmediately_onCancelCallbackCalled() {
    listener = mock()
    listener2 = mock()
    listener3 = mock()
    val alphaProgress = DynamicValue(0f)
    val animation =
        Animated.timing(
            target = alphaProgress, to = 1f, duration = 2000, animationFinishListener = listener)
    val animation2 =
        Animated.timing(
            target = alphaProgress, to = 1f, duration = 2000, animationFinishListener = listener2)
    val sequence = Animated.sequence(animation, animation2)
    sequence.addListener(listener3)
    sequence.start()
    animation.cancel()
    shadowOf(getMainLooper()).idle()
    // verify that if we cancel first animation straight away the first animation and the main
    // animation onCancel listeners will be invoked
    verify(
            listener,
            times(1).description("timing animation listener called onFinish(cancelled = true)"))
        .onFinish(true)
    verify(
            listener,
            never()
                .description(
                    "timing animation listener didn't onFinish(cancelled = false) as it was cancelled"))
        .onFinish(false)
    verify(
            listener3,
            times(1)
                .description("main sequence animation listener called onFinish(cancelled = true)"))
        .onFinish(true)
    verify(listener2, never().description("timing2 animation listener not invoked")).onFinish(any())
  }

  @Test
  fun sequenceAnimation_whenSpringAnimationCancelledImmediately_onCancelCallbackCalled() {
    listener = mock()
    listener2 = mock()
    listener3 = mock()
    val alphaProgress = DynamicValue(0f)
    val animation =
        Animated.spring(
            target = alphaProgress,
            to = 1f,
            SpringConfig(stiffness = 50f, dampingRatio = 0.5f),
            animationFinishListener = listener)
    val animation2 =
        Animated.spring(
            target = alphaProgress,
            to = 1f,
            SpringConfig(stiffness = 50f, dampingRatio = 0.5f),
            animationFinishListener = listener2)
    val sequence = Animated.sequence(animation, animation2)
    sequence.addListener(listener3)
    sequence.start()
    animation.cancel()
    shadowOf(getMainLooper()).idle()
    // verify that if we cancel first animation straight away the first animation and the main
    // animation onCancel listeners will be invoked
    verify(
            listener,
            times(1).description("spring animation listener called onFinish(cancelled = true)"))
        .onFinish(true)
    verify(
            listener,
            never()
                .description(
                    "spring animation listener didnt called onFinish(cancelled = false) as it was cancelled"))
        .onFinish(false)
    verify(
            listener3,
            times(1)
                .description("main sequence animation listener called onFinish(cancelled = true)"))
        .onFinish(true)
    verify(
            listener3,
            never()
                .description(
                    "main sequence animation listener didnt called onFinish(cancelled = false) as it was cancelled"))
        .onFinish(false)
    verify(listener2, never().description("spring2 animation listener not invoked")).onFinish(any())
  }

  @Test
  fun sequenceAnimation_whenAnimationCancelledFromSecondAnimation_onCancelCallbackCalled() {
    listener = mock()
    listener2 = mock()
    listener3 = mock()
    val alphaProgress = DynamicValue(0f)
    var animation2: AnimatedAnimation? = null
    val animation =
        Animated.timing(
            target = alphaProgress, to = 1f, duration = 2000, animationFinishListener = listener)
    animation2 =
        Animated.timing(
            target = alphaProgress,
            to = 1f,
            duration = 2000,
            animationFinishListener = listener2,
            onUpdate = { animation2?.cancel() })
    val sequence = Animated.sequence(animation, animation2)
    sequence.addListener(listener3)
    sequence.start()
    shadowOf(getMainLooper()).idle()
    // verify that if we cancel second animation after it started the first animation onFinish
    // listener will be invoked, the second animation OnCancel listener will be
    // invoked and the main animation onCancel listener will be invoked
    verify(
            listener,
            times(1)
                .description("timing animation listener called onFinish(false) as it was finished"))
        .onFinish(false)
    verify(
            listener2,
            times(1)
                .description(
                    "timing 2 animation listener called onFinish(true) as it was cancelled"))
        .onFinish(true)
    verify(
            listener2,
            never()
                .description(
                    "timing 2 animation listener didnt called onFinish(cancelled = false) as it was cancelled"))
        .onFinish(false)
    verify(
            listener3,
            times(1)
                .description(
                    "main sequence animation listener called onFinish(true) as it was cancelled"))
        .onFinish(true)
  }

  @Test
  fun parralelAnimation_whenAnimationCancelledFromSecondAnimation_onCancelCallbackCalled() {
    listener = mock()
    listener2 = mock()
    listener3 = mock()
    val alphaProgress = DynamicValue(0f)
    var animation2: AnimatedAnimation? = null
    val animation =
        Animated.timing(
            target = alphaProgress, to = 1f, duration = 2000, animationFinishListener = listener)
    animation2 =
        Animated.timing(
            target = alphaProgress,
            to = 1f,
            duration = 2000,
            animationFinishListener = listener2,
            onUpdate = { animation2?.cancel() })
    val parallel = Animated.parallel(animation, animation2)
    parallel.addListener(listener3)
    parallel.start()
    shadowOf(getMainLooper()).idle()
    // verify that if we cancel second animation after it started the first animation onFinish
    // listener will be invoked, the second animation OnCancel and OnFInish listeners will be
    // invoked and the main animation onCancel listener will be invoked
    verify(
            listener,
            times(1)
                .description("timing animation listener called onFinish(true) as it was cancelled"))
        .onFinish(true)
    verify(
            listener,
            never()
                .description(
                    "timing animation listener didnt called onFinish(cancelled = false) as it was cancelled"))
        .onFinish(false)
    verify(
            listener2,
            times(1)
                .description(
                    "timing 2 animation listener called onFinish(true) as it was cancelled"))
        .onFinish(true)
    verify(
            listener2,
            never()
                .description(
                    "timing 2 animation listenerdidnt called onFinish(cancelled = false) as it was cancelled"))
        .onFinish(false)
    verify(
            listener3,
            times(1)
                .description(
                    "main sequence animation listener called onFinish(true) as it was cancelled"))
        .onFinish(true)
  }

  @Test
  fun sequenceAnimation_whenAnimationFinish_alphaAndXValueChange() {
    val alphaProgress = DynamicValue(0f)
    val xProgress = DynamicValue(100f)
    val animation1 = Animated.timing(target = alphaProgress, to = 1f, duration = 1000)
    val animation2 = Animated.timing(target = xProgress, to = 200f, duration = 1000)
    val testLithoview =
        lithoViewRule.render { TestComponent(alphaProgress = alphaProgress, xProgress = xProgress) }

    val view = testLithoview.lithoView
    assertThat(view.alpha).isEqualTo(0f).describedAs("alpha initial value")
    assertThat(view.translationX).isEqualTo(100f).describedAs("translationX initial value")
    val sequence = Animated.sequence(animation1, animation2)
    sequence.start()
    shadowOf(getMainLooper()).idle()
    assertThat(view.alpha).isEqualTo(1f).describedAs("alpha value after animation")
    assertThat(view.translationX).isEqualTo(200f).describedAs("translationX value after animation")
  }

  @Test
  fun sequenceAnimation_whenAnimationFinish_onFinishCallbackCalled() {
    listener = mock()
    listener2 = mock()
    listener3 = mock()

    val alphaProgress = DynamicValue(0f)
    val xProgress = DynamicValue(100f)
    val animation1 =
        Animated.timing(
            target = alphaProgress, to = 1f, duration = 1000, animationFinishListener = listener2)
    val animation2 =
        Animated.timing(
            target = xProgress, to = 200f, duration = 1000, animationFinishListener = listener3)

    val sequence = Animated.sequence(animation1, animation2)
    sequence.addListener(listener)
    sequence.start()
    shadowOf(getMainLooper()).idle()

    verify(listener, times(1).description("sequence animation listener should finish"))
        .onFinish(false)
    verify(listener2, times(1).description("first timing animation listener should finish"))
        .onFinish(false)
    verify(listener3, times(1).description("second timing animation listener should finish"))
        .onFinish(false)
  }

  @Test
  fun loopAnimation_whenAnimationFinish_alphaAndXValueChange() {
    val alphaProgress = DynamicValue(0f)
    val xProgress = DynamicValue(100f)
    val animation1 = Animated.timing(target = alphaProgress, to = 1f, duration = 1000)
    val animation2 = Animated.timing(target = xProgress, to = 200f, duration = 1000)
    val testLithoview =
        lithoViewRule.render { TestComponent(alphaProgress = alphaProgress, xProgress = xProgress) }

    val view = testLithoview.lithoView
    assertThat(view.alpha).isEqualTo(0f).describedAs("alpha initial value")
    assertThat(view.translationX).isEqualTo(100f).describedAs("translationX initial value")
    val loop = Animated.loop(Animated.sequence(animation1, animation2), 2)
    loop.start()
    shadowOf(getMainLooper()).idle()
    assertThat(view.alpha).isEqualTo(1f).describedAs("alpha value after animation")
    assertThat(view.translationX).isEqualTo(200f).describedAs("translationX value after animation")
  }

  @Test
  fun loopAnimation_whenAnimationFinish_onFinishCallbackCalled() {
    listener = mock()
    listener2 = mock()
    listener3 = mock()
    val alphaProgress = DynamicValue(0f)
    val xProgress = DynamicValue(100f)
    val animation1 =
        Animated.timing(
            target = alphaProgress, to = 1f, duration = 1000, animationFinishListener = listener2)
    val animation2 =
        Animated.timing(
            target = xProgress, to = 200f, duration = 1000, animationFinishListener = listener3)

    val loop = Animated.loop(Animated.sequence(animation1, animation2), 2)
    loop.addListener(listener)
    loop.start()
    shadowOf(getMainLooper()).idle()
    verify(listener, times(1).description("loop animation listener should finish once"))
        .onFinish(false)
    verify(listener2, times(2).description("first timing animation listener should finish twice"))
        .onFinish(false)
    verify(listener3, times(2).description("second timing animation listener should finish twice"))
        .onFinish(false)
  }

  private class TestComponent(
      private val alphaProgress: DynamicValue<Float> = DynamicValue<Float>(0f),
      private val xProgress: DynamicValue<Float> = DynamicValue<Float>(100f)
  ) : KComponent() {
    override fun ComponentScope.render(): Component? {
      return Row(
          style = Style.width(100.px).height(100.px).alpha(alphaProgress).translationX(xProgress))
    }
  }
}
