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
import android.animation.ValueAnimator
import android.view.animation.Interpolator
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.math.min

object Animations {

  @JvmStatic
  fun bind(value: DynamicValue<Float>): DynamicValueBindingBuilder =
      DynamicValueBindingBuilder(value)

  @JvmStatic
  fun bind(value: StateValue<DynamicValue<Float>>): DynamicValueBindingBuilder {
    val dynamicValue = value.get() ?: throw IllegalArgumentException("The input must not be null.")
    return bind(dynamicValue)
  }

  /** Create a new AnimationBuilder to manage the creation of a DynamicProps animation. */
  @JvmStatic fun animate(value: DynamicValue<Float>): AnimationBuilder = AnimationBuilder(value)

  class DynamicValueBindingBuilder internal constructor(private val source: DynamicValue<Float>) {
    private var hasInputRange = false
    private var inputRangeStart = 0f
    private var inputRangeEnd = 1f
    private var hasOutputRange = false
    private var outputRangeStart = 0f
    private var outputRangeEnd = 1f
    private var interpolator: Interpolator? = null

    fun inputRange(start: Float, end: Float): DynamicValueBindingBuilder {
      inputRangeStart = start
      inputRangeEnd = end
      hasInputRange = true
      return this
    }

    fun outputRange(start: Float, end: Float): DynamicValueBindingBuilder {
      outputRangeStart = start
      outputRangeEnd = end
      hasOutputRange = true
      return this
    }

    fun with(interpolator: Interpolator?): DynamicValueBindingBuilder {
      this.interpolator = interpolator
      return this
    }

    /** Bind modified flow source to a dynamic value */
    fun to(dynamicValueState: StateValue<DynamicValue<Float>>) {
      val dynamicValue = create()
      dynamicValueState.set(dynamicValue)
    }

    /** Special case method to bind to a DynamicValue<Integer> to animate color */
    fun toInteger(dynamicValueState: StateValue<DynamicValue<Int>>) {
      val dynamicValue = createInteger()
      dynamicValueState.set(dynamicValue)
    }

    private fun modify(value: Float): Float {
      var result = value
      if (hasInputRange) {
        result = (result - inputRangeStart) / (inputRangeEnd - inputRangeStart)
        result = min(result, 1f)
        result = max(result, 0f)
      }
      interpolator?.let { result = it.getInterpolation(result) }
      if (hasOutputRange) {
        val range = outputRangeEnd - outputRangeStart
        result = outputRangeStart + result * range
      }
      return result
    }

    fun create(): DynamicValue<Float> {
      val modifier =
          DerivedDynamicValue.Modifier<Float, Float> { input ->
            this@DynamicValueBindingBuilder.modify(input)
          }
      return DerivedDynamicValue(source, modifier)
    }

    fun createInteger(): DynamicValue<Int> {
      val modifier =
          DerivedDynamicValue.Modifier<Float, Int> { input ->
            this@DynamicValueBindingBuilder.modify(input).toInt()
          }
      return DerivedDynamicValue(source, modifier)
    }
  }

  class AnimationBuilder internal constructor(private val valueToAnimate: DynamicValue<Float>) {
    private var _duration: Long = -1
    private var _from: Float = valueToAnimate.get()
    private var _to = 0f
    private var _interpolator: Interpolator? = null

    /** Specify that value that the DynamicValue will be animated to. */
    fun to(to: Float): AnimationBuilder {
      _to = to
      return this
    }

    /** Specify that value that the DynamicValue will be animated from. */
    fun from(from: Float): AnimationBuilder {
      _from = from
      return this
    }

    /** Specify that duration of the animation. */
    fun duration(duration: Long): AnimationBuilder {
      _duration = duration
      return this
    }

    /** Specify that duration of the animation. */
    fun interpolator(interpolator: Interpolator?): AnimationBuilder {
      _interpolator = interpolator
      return this
    }

    /** Start the animation and return a reference to the Animator */
    fun start(): Animator {
      val animator = ValueAnimator.ofFloat(_from, _to)
      if (_duration > -1) {
        animator.duration = _duration
      }
      if (_interpolator != null) {
        animator.interpolator = _interpolator
      }
      animator.addUpdateListener { animation ->
        val animatedValue = animation.animatedValue as Float
        valueToAnimate.set(animatedValue)
      }
      animator.start()
      return animator
    }

    /**
     * Stop the previous animation and start a new one.
     *
     * @param animatorRef A reference to the previous animation. This will be changed to the new
     *   animation.
     */
    fun startAndCancelPrevious(animatorRef: AtomicReference<Animator?>) {
      val oldAnimator = animatorRef.get()
      oldAnimator?.cancel()
      val newAnimator = start()
      animatorRef.set(newAnimator)
    }
  }
}
