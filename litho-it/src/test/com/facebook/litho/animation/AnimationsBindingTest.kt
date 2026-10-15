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

import android.animation.Animator
import android.animation.TimeInterpolator
import android.view.animation.Interpolator
import com.facebook.litho.Animations
import com.facebook.litho.DynamicValue
import com.facebook.litho.StateValue
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnimationsBindingTest {

  @Test
  fun bindStateValue_changeInSourcePropagatesToBinding() {
    val source = DynamicValue(3f)
    val sourceState = StateValue<DynamicValue<Float>>()
    sourceState.set(source)
    val target = StateValue<DynamicValue<Float>>()

    Animations.bind(sourceState).to(target)

    assertThat(target.get()!!.get()).isEqualTo(3f)
    source.set(7f)
    assertThat(target.get()!!.get()).isEqualTo(7f)
  }

  @Test
  fun bindNullStateValue_throwsIllegalArgumentException() {
    val sourceState = StateValue<DynamicValue<Float>>()

    assertThatThrownBy { Animations.bind(sourceState) }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("The input must not be null.")
  }

  @Test
  fun bindWithInputRangeInterpolatorAndOutputRange_appliesModifiersInOrder() {
    val squareInterpolator = Interpolator { input -> input * input }
    val source = DynamicValue(15f)
    val target = StateValue<DynamicValue<Float>>()

    Animations.bind(source)
        .inputRange(10f, 20f)
        .with(squareInterpolator)
        .outputRange(100f, 200f)
        .to(target)

    assertThat(target.get()!!.get()).isEqualTo(125f)
    source.set(25f)
    assertThat(target.get()!!.get()).isEqualTo(200f)
  }

  @Test
  fun bindFloatToInteger_withOutputRange_convertsModifiedValue() {
    val source = DynamicValue(0.74f)
    val target = StateValue<DynamicValue<Int>>()

    Animations.bind(source).outputRange(10f, 20f).toInteger(target)

    assertThat(target.get()!!.get()).isEqualTo(17)
    source.set(1f)
    assertThat(target.get()!!.get()).isEqualTo(20)
  }

  @Test
  fun startAnimator_withInterpolator_setsAnimatorInterpolator() {
    val source = DynamicValue(0f)
    val interpolator = Interpolator { input -> input }

    val animator = Animations.animate(source).to(1f).interpolator(interpolator).start()

    assertThat(animator.interpolator).isSameAs(interpolator)
    animator.end()
    assertThat(source.get()).isEqualTo(1f)
  }

  @Test
  fun startAnimatorAndCancelPrevious_withPrevious_cancelsOldAnimatorAndSetsNewAnimator() {
    val source = DynamicValue(0f)
    val oldAnimator = CancelRecordingAnimator()
    val animatorRef = AtomicReference<Animator?>(oldAnimator)

    Animations.animate(source).to(3f).startAndCancelPrevious(animatorRef)

    val newAnimator = animatorRef.get()
    assertThat(oldAnimator.cancelCount).isEqualTo(1)
    assertThat(newAnimator).isNotSameAs(oldAnimator)
    newAnimator!!.end()
    assertThat(source.get()).isEqualTo(3f)
  }

  @Test
  fun startAnimatorAndCancelPrevious_withoutPrevious_setsNewAnimator() {
    val source = DynamicValue(0f)
    val animatorRef = AtomicReference<Animator?>(null)

    Animations.animate(source).to(3f).startAndCancelPrevious(animatorRef)

    val newAnimator = animatorRef.get()
    assertThat(newAnimator).isNotNull
    newAnimator!!.end()
    assertThat(source.get()).isEqualTo(3f)
  }

  private class CancelRecordingAnimator : Animator() {
    var cancelCount = 0

    override fun cancel() {
      cancelCount++
    }

    override fun start() = Unit

    override fun end() = Unit

    override fun setStartDelay(startDelay: Long) = Unit

    override fun getStartDelay(): Long = 0L

    override fun setDuration(duration: Long): Animator = this

    override fun getDuration(): Long = 0L

    override fun setInterpolator(value: TimeInterpolator?) = Unit

    override fun isRunning(): Boolean = false
  }
}
