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

package com.facebook.litho

import android.animation.Animator
import android.view.animation.Interpolator
import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(LithoTestRunner::class)
class AnimationsTest {

  @Test
  fun animations_bind_changeInSourcePropagatesToBinding() {
    val source = DynamicValue(5f)
    val target = StateValue<DynamicValue<Float>>()
    Animations.bind(source).to(target)
    assertThat(target.get()!!.get()).isEqualTo(5f)
    source.set(2f)
    assertThat(target.get()!!.get()).isEqualTo(2f)
  }

  @Test
  fun animations_bindWithFromRange_rangeIsApplied() {
    val source = DynamicValue(0f)
    val target = StateValue<DynamicValue<Float>>()
    Animations.bind(source).inputRange(45f, 55f).to(target)
    assertThat(target.get()!!.get()).isEqualTo(0f)
    source.set(45f)
    assertThat(target.get()!!.get()).isEqualTo(0f)
    source.set(55f)
    assertThat(target.get()!!.get()).isEqualTo(1f)
    source.set(100f)
    assertThat(target.get()!!.get()).isEqualTo(1f)
  }

  @Test
  fun animations_bindWithToRange_rangeIsApplied() {
    val source = DynamicValue(0f)
    val target = StateValue<DynamicValue<Float>>()
    Animations.bind(source).outputRange(45f, 55f).to(target)
    assertThat(target.get()!!.get()).isEqualTo(45f)
    source.set(1f)
    assertThat(target.get()!!.get()).isEqualTo(55f)
    source.set(.5f)
    assertThat(target.get()!!.get()).isEqualTo(50f)
  }

  @Test
  fun animations_bindWithInterpolator_interpolatorIsApplied() {
    val mockInterpolator = mock<Interpolator>()
    whenever(mockInterpolator.getInterpolation(ArgumentMatchers.anyFloat())).thenReturn(1f)
    val source = DynamicValue(0f)
    val target = StateValue<DynamicValue<Float>>()
    Animations.bind(source).with(mockInterpolator).to(target)
    assertThat(target.get()!!.get()).isEqualTo(1f)
    verify(mockInterpolator).getInterpolation(eq(0f))
  }

  @Test
  fun animations_startAnimator_animatorStarted() {
    val `in` = DynamicValue(0f)
    val outState = StateValue<DynamicValue<Float>>()
    Animations.bind(`in`).outputRange(0f, 5f).to(outState)
    val animator = Animations.animate(`in`).to(1f).duration(2_000).start()
    assertThat(animator.duration).isEqualTo(2_000)
    animator.end()
    assertThat(outState.get()!!.get()).isEqualTo(5f)
  }

  @Test
  fun animations_startAnimatorAncCancelPrevious_previousIsCancelled() {
    val `in` = DynamicValue(0f)
    val outState = StateValue<DynamicValue<Float>>()
    Animations.bind(`in`).outputRange(0f, 5f).to(outState)
    val oldAnimator = mock<Animator>()
    val animatorRef = AtomicReference(oldAnimator)
    Animations.animate(`in`).to(1f).duration(2_000).startAndCancelPrevious(animatorRef)
    verify(oldAnimator).cancel()
    val newAnimator = animatorRef.get()
    assertThat(oldAnimator).isNotSameAs(newAnimator)
    assertThat(newAnimator.duration).isEqualTo(2_000)
    newAnimator.end()
    assertThat(outState.get()!!.get()).isEqualTo(5f)
  }

  @Test
  fun animations_bindFloatToInteger_propagates() {
    val source = DynamicValue(5f)
    val target = StateValue<DynamicValue<Int>>()
    Animations.bind(source).toInteger(target)
    assertThat(target.get()!!.get()).isEqualTo(5)
    source.set(2f)
    assertThat(target.get()!!.get()).isEqualTo(2)
  }
}
