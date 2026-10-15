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

package com.facebook.litho.dataflow.springs

import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.abs

/**
 * Classical spring implementing Hooke's law with configurable friction and tension.
 *
 * This code was forked from the facebook/rebound repository.
 */
class Spring {

  private var springConfig: SpringConfig = SpringConfig.defaultConfig
  private var overshootClampingEnabled: Boolean = false

  // storage for the current and prior physics state while integration is occurring
  private class PhysicsState {
    var position: Double = 0.0
    var velocity: Double = 0.0
  }

  // unique id for the spring in the system
  private val id: String = "spring:${nextId++}"
  // all physics simulation objects are final and reused in each processing pass
  private val currentState: PhysicsState = PhysicsState()
  private val previousState: PhysicsState = PhysicsState()
  private val tempState: PhysicsState = PhysicsState()
  private var startValue: Double = 0.0
  private var endValue: Double = 0.0
  private var wasAtRest: Boolean = true
  // thresholds for determining when the spring is at rest
  private var restSpeedThreshold: Double = 0.005
  private var displacementFromRestThreshold: Double = 0.005
  private var timeAccumulator: Double = 0.0
  private val listeners: CopyOnWriteArraySet<SpringListener> = CopyOnWriteArraySet()

  /**
   * Destroys this Spring, meaning that it will be deregistered from its BaseSpringSystem so it
   * won't be iterated anymore and will clear its set of listeners. Do not use the Spring after
   * calling this, doing so may just cause an exception to be thrown.
   */
  fun destroy() {
    listeners.clear()
  }

  /**
   * get the unique id for this spring
   *
   * @return the unique id
   */
  fun getId(): String = id

  /**
   * set the config class
   *
   * @param springConfig config class for the spring
   * @return this Spring instance for chaining
   */
  fun setSpringConfig(springConfig: SpringConfig): Spring {
    this.springConfig = springConfig
    return this
  }

  /**
   * retrieve the spring config for this spring
   *
   * @return the SpringConfig applied to this spring
   */
  fun getSpringConfig(): SpringConfig = springConfig

  /**
   * Set the displaced value to determine the displacement for the spring from the rest value. This
   * value is retained and used to calculate the displacement ratio. The default signature also sets
   * the Spring at rest to facilitate the common behavior of moving a spring to a new position.
   *
   * @param currentValue the new start and current value for the spring
   * @return the spring for chaining
   */
  fun setCurrentValue(currentValue: Double): Spring {
    return setCurrentValue(currentValue, true)
  }

  /**
   * The full signature for setCurrentValue includes the option of not setting the spring at rest
   * after updating its currentValue. Passing setAtRest false means that if the endValue of the
   * spring is not equal to the currentValue, the physics system will start iterating to resolve the
   * spring to the end value. This is almost never the behavior that you want, so the default
   * setCurrentValue signature passes true.
   *
   * @param currentValue the new start and current value for the spring
   * @param setAtRest optionally set the spring at rest after updating its current value.
   * @return the spring for chaining
   */
  fun setCurrentValue(currentValue: Double, setAtRest: Boolean): Spring {
    startValue = currentValue
    currentState.position = currentValue
    for (listener in listeners) {
      listener.onSpringUpdate(this)
    }
    if (setAtRest) {
      this.setAtRest()
    }
    return this
  }

  /**
   * Get the displacement value from the last time setCurrentValue was called.
   *
   * @return displacement value
   */
  fun getStartValue(): Double = startValue

  /**
   * Get the current
   *
   * @return current value
   */
  fun getCurrentValue(): Double = currentState.position

  /**
   * get the displacement of the springs current value from its rest value.
   *
   * @return the distance displaced by
   */
  fun getCurrentDisplacementDistance(): Double = getDisplacementDistanceForState(currentState)

  /**
   * get the displacement from rest for a given physics state
   *
   * @param state the state to measure from
   * @return the distance displaced by
   */
  private fun getDisplacementDistanceForState(state: PhysicsState): Double {
    return abs(endValue - state.position)
  }

  /**
   * set the rest value to determine the displacement for the spring
   *
   * @param endValue the endValue for the spring
   * @return the spring for chaining
   */
  fun setEndValue(endValue: Double): Spring {
    if (this.endValue == endValue && isAtRest()) {
      return this
    }
    startValue = getCurrentValue()
    this.endValue = endValue
    for (listener in listeners) {
      listener.onSpringEndStateChange(this)
    }
    return this
  }

  /**
   * get the rest value used for determining the displacement of the spring
   *
   * @return the rest value for the spring
   */
  fun getEndValue(): Double = endValue

  /**
   * set the velocity on the spring in pixels per second
   *
   * @param velocity velocity value
   * @return the spring for chaining
   */
  fun setVelocity(velocity: Double): Spring {
    if (velocity == currentState.velocity) {
      return this
    }
    currentState.velocity = velocity
    return this
  }

  /**
   * get the velocity of the spring
   *
   * @return the current velocity
   */
  fun getVelocity(): Double = currentState.velocity

  /**
   * Sets the speed at which the spring should be considered at rest.
   *
   * @param restSpeedThreshold speed pixels per second
   * @return the spring for chaining
   */
  fun setRestSpeedThreshold(restSpeedThreshold: Double): Spring {
    this.restSpeedThreshold = restSpeedThreshold
    return this
  }

  /**
   * Returns the speed at which the spring should be considered at rest in pixels per second
   *
   * @return speed in pixels per second
   */
  fun getRestSpeedThreshold(): Double = restSpeedThreshold

  /**
   * set the threshold of displacement from rest below which the spring should be considered at rest
   *
   * @param displacementFromRestThreshold displacement to consider resting below
   * @return the spring for chaining
   */
  fun setRestDisplacementThreshold(displacementFromRestThreshold: Double): Spring {
    this.displacementFromRestThreshold = displacementFromRestThreshold
    return this
  }

  /**
   * get the threshold of displacement from rest below which the spring should be considered at rest
   *
   * @return displacement to consider resting below
   */
  fun getRestDisplacementThreshold(): Double = displacementFromRestThreshold

  /**
   * Force the spring to clamp at its end value to avoid overshooting the target value.
   *
   * @param overshootClampingEnabled whether or not to enable overshoot clamping
   * @return the spring for chaining
   */
  fun setOvershootClampingEnabled(overshootClampingEnabled: Boolean): Spring {
    this.overshootClampingEnabled = overshootClampingEnabled
    return this
  }

  /**
   * Check if overshoot clamping is enabled.
   *
   * @return is overshoot clamping enabled
   */
  fun isOvershootClampingEnabled(): Boolean = overshootClampingEnabled

  /**
   * Check if the spring is overshooting beyond its target.
   *
   * @return true if the spring is overshooting its target
   */
  fun isOvershooting(): Boolean {
    return springConfig.tension > 0 &&
        ((startValue < endValue && getCurrentValue() > endValue) ||
            (startValue > endValue && getCurrentValue() < endValue))
  }

  /**
   * advance the physics simulation in SOLVER_TIMESTEP_SEC sized chunks to fulfill the required
   * realTimeDelta. The math is inlined inside the loop since it made a huge performance impact when
   * there are several springs being advanced.
   *
   * @param realDeltaTime clock drift
   */
  fun advance(realDeltaTime: Double) {

    var isAtRest = isAtRest()

    if (isAtRest && wasAtRest) {
      /* begin debug
      Log.d(TAG, "bailing out because we are at rest:" + getName());
      end debug */
      return
    }

    // clamp the amount of realTime to simulate to avoid stuttering in the UI. We should be able
    // to catch up in a subsequent advance if necessary.
    var adjustedDeltaTime = realDeltaTime
    if (realDeltaTime > MAX_DELTA_TIME_SEC) {
      adjustedDeltaTime = MAX_DELTA_TIME_SEC
    }

    /* begin debug
    long startTime = System.currentTimeMillis();
    int iterations = 0;
    end debug */

    timeAccumulator += adjustedDeltaTime

    val tension = springConfig.tension
    val friction = springConfig.friction

    var position = currentState.position
    var velocity = currentState.velocity
    var tempPosition = tempState.position
    var tempVelocity = tempState.velocity

    var aVelocity: Double
    var aAcceleration: Double
    var bVelocity: Double
    var bAcceleration: Double
    var cVelocity: Double
    var cAcceleration: Double
    var dVelocity: Double
    var dAcceleration: Double

    var dxdt: Double
    var dvdt: Double

    // iterate over the true time
    while (timeAccumulator >= SOLVER_TIMESTEP_SEC) {
      /* begin debug
      iterations++;
      end debug */
      timeAccumulator -= SOLVER_TIMESTEP_SEC

      if (timeAccumulator < SOLVER_TIMESTEP_SEC) {
        // This will be the last iteration. Remember the previous state in case we need to
        // interpolate
        previousState.position = position
        previousState.velocity = velocity
      }

      // Perform an RK4 integration to provide better detection of the acceleration curve via
      // sampling of Euler integrations at 4 intervals feeding each derivative into the calculation
      // of the next and taking a weighted sum of the 4 derivatives as the final output.

      // This math was inlined since it made for big performance improvements when advancing several
      // springs in one pass of the BaseSpringSystem.

      // The initial derivative is based on the current velocity and the calculated acceleration
      aVelocity = velocity
      aAcceleration = (tension * (endValue - tempPosition)) - friction * velocity

      // Calculate the next derivatives starting with the last derivative and integrating over the
      // timestep
      tempPosition = position + aVelocity * SOLVER_TIMESTEP_SEC * 0.5
      tempVelocity = velocity + aAcceleration * SOLVER_TIMESTEP_SEC * 0.5
      bVelocity = tempVelocity
      bAcceleration = (tension * (endValue - tempPosition)) - friction * tempVelocity

      tempPosition = position + bVelocity * SOLVER_TIMESTEP_SEC * 0.5
      tempVelocity = velocity + bAcceleration * SOLVER_TIMESTEP_SEC * 0.5
      cVelocity = tempVelocity
      cAcceleration = (tension * (endValue - tempPosition)) - friction * tempVelocity

      tempPosition = position + cVelocity * SOLVER_TIMESTEP_SEC
      tempVelocity = velocity + cAcceleration * SOLVER_TIMESTEP_SEC
      dVelocity = tempVelocity
      dAcceleration = (tension * (endValue - tempPosition)) - friction * tempVelocity

      // Take the weighted sum of the 4 derivatives as the final output.
      dxdt = 1.0 / 6.0 * (aVelocity + 2.0 * (bVelocity + cVelocity) + dVelocity)
      dvdt = 1.0 / 6.0 * (aAcceleration + 2.0 * (bAcceleration + cAcceleration) + dAcceleration)

      position += dxdt * SOLVER_TIMESTEP_SEC
      velocity += dvdt * SOLVER_TIMESTEP_SEC
    }

    tempState.position = tempPosition
    tempState.velocity = tempVelocity

    currentState.position = position
    currentState.velocity = velocity

    if (timeAccumulator > 0) {
      interpolate(timeAccumulator / SOLVER_TIMESTEP_SEC)
    }

    // End the spring immediately if it is overshooting and overshoot clamping is enabled.
    // Also make sure that if the spring was considered within a resting threshold that it's now
    // snapped to its end value.
    if (this.isAtRest() || (overshootClampingEnabled && isOvershooting())) {
      // Don't call setCurrentValue because that forces a call to onSpringUpdate
      if (tension > 0) {
        startValue = endValue
        currentState.position = endValue
      } else {
        endValue = currentState.position
        startValue = endValue
      }
      setVelocity(0.0)
      isAtRest = true
    }

    /* begin debug
    long endTime = System.currentTimeMillis();
    long elapsedMillis = endTime - startTime;
    Log.d(TAG,
        "iterations:" + iterations +
            " iterationTime:" + elapsedMillis +
            " position:" + currentState.position +
            " velocity:" + currentState.velocity +
            " realDeltaTime:" + realDeltaTime +
            " adjustedDeltaTime:" + adjustedDeltaTime +
            " isAtRest:" + isAtRest +
            " wasAtRest:" + wasAtRest);
    end debug */

    // NB: do these checks outside the loop so all listeners are properly notified of the state
    //     transition
    var notifyActivate = false
    if (wasAtRest) {
      wasAtRest = false
      notifyActivate = true
    }
    var notifyAtRest = false
    if (isAtRest) {
      wasAtRest = true
      notifyAtRest = true
    }
    for (listener in listeners) {
      // starting to move
      if (notifyActivate) {
        listener.onSpringActivate(this)
      }

      // updated
      listener.onSpringUpdate(this)

      // coming to rest
      if (notifyAtRest) {
        listener.onSpringAtRest(this)
      }
    }
  }

  /**
   * Check if this spring should be advanced by the system. The rule is if the spring is currently
   * at rest and it was at rest in the previous advance, the system can skip this spring
   *
   * @return should the system process this spring
   */
  fun systemShouldAdvance(): Boolean = !isAtRest() || !wasAtRest()

  /**
   * Check if the spring was at rest in the prior iteration. This is used for ensuring the ending
   * callbacks are fired as the spring comes to a rest.
   *
   * @return true if the spring was at rest in the prior iteration
   */
  fun wasAtRest(): Boolean = wasAtRest

  /**
   * check if the current state is at rest
   *
   * @return is the spring at rest
   */
  fun isAtRest(): Boolean {
    return abs(currentState.velocity) <= restSpeedThreshold &&
        (getDisplacementDistanceForState(currentState) <= displacementFromRestThreshold ||
            springConfig.tension == 0.0)
  }

  /**
   * Set the spring to be at rest by making its end value equal to its current value and setting
   * velocity to 0.
   *
   * @return this object
   */
  fun setAtRest(): Spring {
    endValue = currentState.position
    tempState.position = currentState.position
    currentState.velocity = 0.0
    return this
  }

  /**
   * linear interpolation between the previous and current physics state based on the amount of
   * timestep remaining after processing the rendering delta time in timestep sized chunks.
   *
   * @param alpha from 0 to 1, where 0 is the previous state, 1 is the current state
   */
  private fun interpolate(alpha: Double) {
    currentState.position = currentState.position * alpha + previousState.position * (1 - alpha)
    currentState.velocity = currentState.velocity * alpha + previousState.velocity * (1 - alpha)
  }

  /** listeners */

  /**
   * add a listener
   *
   * @param newListener to add
   * @return the spring for chaining
   */
  fun addListener(newListener: SpringListener): Spring {
    listeners.add(newListener)
    return this
  }

  /**
   * remove a listener
   *
   * @param listenerToRemove to remove
   * @return the spring for chaining
   */
  fun removeListener(listenerToRemove: SpringListener): Spring {
    listeners.remove(listenerToRemove)
    return this
  }

  /**
   * remove all of the listeners
   *
   * @return the spring for chaining
   */
  fun removeAllListeners(): Spring {
    listeners.clear()
    return this
  }

  /**
   * This method checks to see that the current spring displacement value is equal to the input,
   * accounting for the spring's rest displacement threshold.
   *
   * @param value The value to compare the spring value to
   * @return Whether the displacement value from the spring is within the bounds of the compare
   *   value, accounting for threshold
   */
  fun currentValueIsApproximately(value: Double): Boolean {
    return abs(getCurrentValue() - value) <= getRestDisplacementThreshold()
  }

  private companion object {
    // maximum amount of time to simulate per physics iteration in seconds (4 frames at 60 FPS)
    private const val MAX_DELTA_TIME_SEC: Double = 0.064
    // fixed timestep to use in the physics solver in seconds
    private const val SOLVER_TIMESTEP_SEC: Double = 0.001
    // unique incrementer id for springs
    private var nextId: Int = 0
  }
}
