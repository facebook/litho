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

package com.facebook.litho.animated

import android.os.Looper.getMainLooper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.DynamicValue
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.px
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.setRoot
import com.facebook.litho.testing.unspecified
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class AnimatedTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  private lateinit var listener: AnimationListener
  private lateinit var listener2: AnimationListener
  private lateinit var listener3: AnimationListener

  @Test
  fun timingAnimation_whenAnimationFinish_alphaValueChange() {
    val alphaProgress = DynamicValue(0f)
    val animation = Animated.timing(target = alphaProgress, to = 1f, duration = 1000)
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot(TestComponent(alphaProgress = alphaProgress))
        .measure()
        .layout()
        .attachToWindow()

    val view = lithoViewRule.lithoView
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
            target = alphaProgress, to = 1f, duration = 1000, onFinish = { listener.onFinish() })
    animation.addListener(listener2)
    animation.start()
    shadowOf(getMainLooper()).idle()
    verify(listener, times(1).description("timing animation listener should finish")).onFinish()
    verify(listener2, times(1).description("main animation listener should finish")).onFinish()
  }

  @Test
  fun sequenceAnimation_whenAnimationFinish_alphaAndXValueChange() {
    val alphaProgress = DynamicValue(0f)
    val xProgress = DynamicValue(100f)
    val animation1 = Animated.timing(target = alphaProgress, to = 1f, duration = 1000)
    val animation2 = Animated.timing(target = xProgress, to = 200f, duration = 1000)
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot(TestComponent(alphaProgress = alphaProgress, xProgress = xProgress))
        .measure()
        .layout()
        .attachToWindow()

    val view = lithoViewRule.lithoView
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
            target = alphaProgress, to = 1f, duration = 1000, onFinish = { listener2.onFinish() })
    val animation2 =
        Animated.timing(
            target = xProgress, to = 200f, duration = 1000, onFinish = { listener3.onFinish() })

    val sequence = Animated.sequence(animation1, animation2)
    sequence.addListener(listener)
    sequence.start()
    shadowOf(getMainLooper()).idle()

    verify(listener, times(1).description("sequence animation listener should finish")).onFinish()
    verify(listener2, times(1).description("first timing animation listener should finish"))
        .onFinish()
    verify(listener3, times(1).description("second timing animation listener should finish"))
        .onFinish()
  }

  @Test
  fun loopAnimation_whenAnimationFinish_alphaAndXValueChange() {
    val alphaProgress = DynamicValue(0f)
    val xProgress = DynamicValue(100f)
    val animation1 = Animated.timing(target = alphaProgress, to = 1f, duration = 1000)
    val animation2 = Animated.timing(target = xProgress, to = 200f, duration = 1000)
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot(TestComponent(alphaProgress = alphaProgress, xProgress = xProgress))
        .measure()
        .layout()
        .attachToWindow()

    val view = lithoViewRule.lithoView
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
            target = alphaProgress, to = 1f, duration = 1000, onFinish = { listener2.onFinish() })
    val animation2 =
        Animated.timing(
            target = xProgress, to = 200f, duration = 1000, onFinish = { listener3.onFinish() })

    val loop = Animated.loop(Animated.sequence(animation1, animation2), 2)
    loop.addListener(listener)
    loop.start()
    shadowOf(getMainLooper()).idle()
    verify(listener, times(1).description("loop animation listener should finish once")).onFinish()
    verify(listener2, times(2).description("first timing animation listener should finish twice"))
        .onFinish()
    verify(listener3, times(2).description("second timing animation listener should finish twice"))
        .onFinish()
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
