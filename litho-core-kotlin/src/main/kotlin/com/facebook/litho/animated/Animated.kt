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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.facebook.litho.DynamicValue

object Animated {
  /**
   * Returns an [AnimatedAnimation] ready for running timing animation that calculate animated
   * values and set them on target param.
   *
   * By default it uses [AccelerateDecelerateInterpolator] and duration of 300 milliseconds.
   */
  fun timing(
      target: DynamicValue<Float>,
      to: Float,
      duration: Long = 300,
      easing: Interpolator = Easing.accelerateDecelerate(),
      onUpdate: ((Float) -> Unit)? = null,
      onFinish: (() -> Unit)? = null,
      onCancel: (() -> Unit)? = null
  ): AnimatedAnimation {

    val animator = ValueAnimator.ofFloat(target.get(), to)
    animator.duration = duration
    animator.interpolator = easing
    animator.addUpdateListener { animation ->
      val animatedValue = animation.animatedValue as Float
      target.set(animatedValue)
      onUpdate?.invoke(animatedValue)
    }
    if (onFinish != null || onCancel != null) {
      animator.addListener(
          object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
              onFinish?.invoke()
            }
            override fun onAnimationCancel(animation: Animator) {
              onCancel?.invoke()
            }
          })
    }
    return AnimatorAnimation(animator)
  }

  /**
   * Returns an [AnimatedAnimation] ready for running spring animation based on the [SpringConfig]
   * params
   *
   * By default it uses medium stiffness and damping.
   */
  fun spring(
      target: DynamicValue<Float>,
      to: Float,
      springConfig: SpringConfig = SpringConfig(),
      onUpdate: ((Float) -> Unit)? = null,
      onFinish: (() -> Unit)? = null,
      onCancel: (() -> Unit)? = null
  ): AnimatedAnimation {
    val springAnimation = SpringAnimation(DynamicFloatValueHolder(target))
    val springForce = SpringForce()
    springForce.finalPosition = to
    springForce.stiffness = springConfig.stiffness
    springForce.dampingRatio = springConfig.dampingRatio
    springAnimation.spring = springForce
    onUpdate?.let { springAnimation.addUpdateListener { _, value, _ -> onUpdate(value) } }
    onFinish?.let {
      springAnimation.addEndListener { _, canceled, _, _ ->
        if (!canceled) {
          onFinish()
        }
      }
    }
    onCancel?.let {
      springAnimation.addEndListener { _, canceled, _, _ ->
        if (canceled) {
          onCancel()
        }
      }
    }

    return AnimatedSpringAnimation(springAnimation)
  }

  /**
   * Returns [SequenceAnimation] ready for running collection of animations in sequence one after
   * another. The order of arguments is the order of the sequence
   */
  fun sequence(vararg animations: AnimatedAnimation): AnimatedAnimation =
      SequenceAnimation(animations)

  /** Returns [ParallelAnimation] ready for running collection of animations in parallel */
  fun parallel(vararg animations: AnimatedAnimation): AnimatedAnimation =
      ParallelAnimation(animations)

  /**
   * Returns [AnimatedAnimation] ready for running collection of animations in stagger with given
   * value in milliseconds
   */
  fun stagger(staggerMs: Long, vararg animations: AnimatedAnimation): AnimatedAnimation {
    val staggerAnimations: ArrayList<AnimatedAnimation> = ArrayList()
    animations.forEachIndexed { index, animation ->
      staggerAnimations.add(delay(staggerMs * index, animation))
    }
    return ParallelAnimation(staggerAnimations.toTypedArray())
  }

  /**
   * Returns [LoopAnimation] ready for running [AnimatedAnimation] object repeated @repeatCount
   * times. By default it runs infinite number of times.
   */
  fun loop(animation: AnimatedAnimation, repeatCount: Int = -1): AnimatedAnimation =
      LoopAnimation(repeatCount, animation)

  /**
   * Returns [AnimatedAnimation] ready for running single or a collection of animations after given
   * delay in milliseconds
   */
  fun delay(startDelay: Long, animation: AnimatedAnimation): AnimatedAnimation {
    val delayAnimator = ValueAnimator.ofFloat(0f, 1f)
    delayAnimator.startDelay = startDelay
    return sequence(AnimatorAnimation(delayAnimator), animation)
  }
}

/**
 * Interface representing single animation (like [Animated.timing] or [Animated.spring]) or a
 * collection of them
 */
interface AnimatedAnimation {
  fun start()
  fun stop()
  fun addListener(listener: AnimationListener)
}

/**
 * Listener that allows to listen for different state updates on animation or the whole collection
 * of them
 */
interface AnimationListener {
  /** triggers when animation or set of animations is completed */
  fun onFinish() = Unit

  /**
   * triggers when animation is cancelled. This callback is not invoked for animations with repeat
   * count set to INFINITE
   */
  fun onCancel() = Unit
}

private class AnimatedSpringAnimation(val springAnimation: SpringAnimation) : AnimatedAnimation {
  override fun start() {
    springAnimation.start()
  }
  override fun stop() {
    springAnimation.cancel()
  }

  override fun addListener(animationListener: AnimationListener) {
    springAnimation.addEndListener { _, canceled, _, _ ->
      if (canceled) {
        animationListener.onCancel()
      } else {
        animationListener.onFinish()
      }
    }
  }
}

internal class AnimatorAnimation(val animator: ValueAnimator) : AnimatedAnimation {
  override fun start() {
    animator.start()
  }
  override fun stop() {
    animator.cancel()
  }

  override fun addListener(animatorListener: AnimationListener) {
    animator.addListener(
        object : AnimatorListenerAdapter() {
          override fun onAnimationEnd(animation: Animator) {
            animatorListener.onFinish()
          }
          override fun onAnimationCancel(animation: Animator) {
            animatorListener.onCancel()
          }
        })
  }
}

/**
 * The stiffer the spring is, the harder it is to stretch it and the faster it undergoes dampening.
 * Spring damping ratio describes behavior after the disturbance, over-damp values (more than 1)
 * make the spring quickly return to the rest position. Under-damp values (less than 1) cause
 * overshooting. Damping equal to 0 will cause the spring to oscillate forever. (The lower the
 * damping ratio, the higher the bounciness)
 */
class SpringConfig(
    var stiffness: Float = SpringForce.STIFFNESS_LOW,
    var dampingRatio: Float = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
)

/** Bridge between Android [FloatValueHolder] and Litho DynamicValue<Float> */
private class DynamicFloatValueHolder(val dynamicValue: DynamicValue<Float>) : FloatValueHolder() {
  override fun getValue(): Float {
    return dynamicValue.get()
  }
  override fun setValue(value: Float) {
    dynamicValue.set(value)
  }
}
