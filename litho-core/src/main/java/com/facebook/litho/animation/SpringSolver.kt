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

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.text.toDouble

/** Analytical closed-form solver for the damped harmonic oscillator. */
internal class SpringSolver
private constructor(
    private val solution: Solution,
    /** Settling time, or [UNSUPPORTED_DURATION] if it cannot fit in Long nanoseconds. */
    val durationNanos: Long,
) {

  /** Returns the displacement from the rest position at the given elapsed time. */
  fun getDisplacement(timeNanos: Long): Float {
    val t = timeNanos / NANOS_PER_SECOND
    return solution.displacement(t).toFloat()
  }

  /** Returns the velocity at the given elapsed time, in float-space units per second. */
  fun getVelocity(timeNanos: Long): Float {
    val t = timeNanos / NANOS_PER_SECOND
    return solution.velocity(t).toFloat()
  }

  private sealed interface Solution {
    fun displacement(t: Double): Double

    fun velocity(t: Double): Double

    fun envelope(t: Double): Double

    fun durationNanos(threshold: Double): Long

    /**
     * After the last violating peak, the threshold predicate changes once. Search integer time with
     * the frame evaluator to include nanosecond rounding.
     */
    fun findDuration(
        threshold: Double,
        lastPeak: Double,
        upperBound: Double,
        useEnvelope: Boolean = false,
    ): Long {
      if (!lastPeak.isFinite() || lastPeak > MAX_SECONDS) return UNSUPPORTED_DURATION
      var low = nanosAfter(lastPeak)
      if (magnitude(low, useEnvelope) < threshold) return low
      var high = max(low, nanosAfter(upperBound))
      while (magnitude(high, useEnvelope) >= threshold) {
        if (high == Long.MAX_VALUE) return UNSUPPORTED_DURATION
        high += min(Long.MAX_VALUE - high, max(1L, high / 2))
      }
      while (high - low > 1) {
        val mid = low + (high - low) / 2
        if (magnitude(mid, useEnvelope) >= threshold) low = mid else high = mid
      }
      return high
    }

    private fun magnitude(nanos: Long, useEnvelope: Boolean): Double {
      val t = nanos / NANOS_PER_SECOND
      return if (useEnvelope) envelope(t) else abs(displacement(t))
    }
  }

  /**
   * x(t) = e^(-gamma*t) * (A*cos(omegaD*t) + B*sin(omegaD*t))
   *
   * v(t) = e^(-gamma*t) * ((B*omegaD - A*gamma)*cos(omegaD*t) - (A*omegaD + B*gamma)*sin(omegaD*t))
   */
  private class Underdamped(
      private val gamma: Double,
      private val omegaD: Double,
      private val a: Double,
      private val b: Double,
  ) : Solution {
    override fun displacement(t: Double): Double {
      val envelope = exp(-gamma * t)
      return envelope * (a * cos(omegaD * t) + b * sin(omegaD * t))
    }

    override fun velocity(t: Double): Double {
      val envelope = exp(-gamma * t)
      val cosT = cos(omegaD * t)
      val sinT = sin(omegaD * t)
      return envelope * ((b * omegaD - a * gamma) * cosT - (a * omegaD + b * gamma) * sinT)
    }

    override fun envelope(t: Double): Double = hypot(a, b) * exp(-gamma * t)

    override fun durationNanos(threshold: Double): Long {
      val amplitude = hypot(a, b)
      if (amplitude < threshold) return 0
      val upper = ln(amplitude / threshold) / gamma
      val phase = atan2(b, a)
      val angle = -atan2(gamma, omegaD)
      val peakAmplitude = amplitude * omegaD / hypot(gamma, omegaD)
      val peakLimit = ln(peakAmplitude / threshold) / gamma
      val index = floor((omegaD * peakLimit - phase - angle) / PI)
      // Very large phase indices lose the precision needed to locate individual extrema.
      if (!index.isFinite() || abs(index) > MAX_PHASE_INDEX) {
        return findDuration(threshold, 0.0, upper, useEnvelope = true)
      }
      var lastPeak = if (abs(a) >= threshold) 0.0 else -1.0
      for (neighbor in -1..1) {
        val peak = (phase + angle + (index + neighbor) * PI) / omegaD
        if (peak >= 0 && peakAmplitude * exp(-gamma * peak) >= threshold) {
          lastPeak = max(lastPeak, peak)
        }
      }
      return if (lastPeak < 0) 0 else findDuration(threshold, lastPeak, upper)
    }
  }

  /**
   * x(t) = e^(-omega0*t) * (A + B*t)
   *
   * v(t) = e^(-omega0*t) * (B - omega0 * (A + B*t))
   */
  private class CriticallyDamped(
      private val omega0: Double,
      private val a: Double,
      private val b: Double,
  ) : Solution {
    override fun displacement(t: Double): Double {
      val envelope = exp(-omega0 * t)
      return envelope * (a + b * t)
    }

    override fun velocity(t: Double): Double {
      val envelope = exp(-omega0 * t)
      return envelope * (b - omega0 * (a + b * t))
    }

    override fun envelope(t: Double): Double = exp(-omega0 * t) * (abs(a) + abs(b) * t)

    override fun durationNanos(threshold: Double): Long {
      var lastPeak = if (abs(a) >= threshold) 0.0 else -1.0
      if (b != 0.0) {
        val peak = 1 / omega0 - a / b
        if (peak >= 0 && abs(displacement(peak)) >= threshold) lastPeak = max(lastPeak, peak)
      }
      if (lastPeak < 0) return 0
      var upper = max(lastPeak, 1 / omega0)
      while (upper < MAX_SECONDS && envelope(upper) >= threshold) upper *= 2
      return findDuration(threshold, lastPeak, upper)
    }
  }

  /**
   * x(t) = A*e^(r1*t) + B*e^(r2*t)
   *
   * v(t) = A*r1*e^(r1*t) + B*r2*e^(r2*t)
   */
  private class Overdamped(
      private val r1: Double,
      private val r2: Double,
      private val a: Double,
      private val b: Double,
  ) : Solution {
    override fun displacement(t: Double): Double {
      return a * exp(r1 * t) + b * exp(r2 * t)
    }

    override fun velocity(t: Double): Double {
      return a * r1 * exp(r1 * t) + b * r2 * exp(r2 * t)
    }

    override fun envelope(t: Double): Double = abs(a) * exp(r1 * t) + abs(b) * exp(r2 * t)

    override fun durationNanos(threshold: Double): Long {
      var lastPeak = if (abs(a + b) >= threshold) 0.0 else -1.0
      if ((a > 0 && b < 0) || (a < 0 && b > 0)) {
        val peak = (ln(abs(a * r1)) - ln(abs(b * r2))) / (r2 - r1)
        if (peak >= 0 && abs(displacement(peak)) >= threshold) lastPeak = max(lastPeak, peak)
      }
      if (lastPeak < 0) return 0
      // Bounding each exponential by half the threshold also handles a negligible slow mode.
      val upper = max(0.0, max(ln(2 * abs(a) / threshold) / -r1, ln(2 * abs(b) / threshold) / -r2))
      return findDuration(threshold, lastPeak, upper)
    }
  }

  companion object {

    /**
     * Computes displacement and velocity at any point in time using the exact solution to the
     * spring ODE, rather than numerical integration (RK4, Euler, etc.). This enables O(1)
     * evaluation at arbitrary time points with no accumulated error.
     *
     * The three damping regimes are handled:
     * - **Underdamped** (damping ratio < 1): oscillatory decay
     * - **Critically damped** (damping ratio = 1): fastest non-oscillatory convergence
     * - **Overdamped** (damping ratio > 1): exponential decay without oscillation
     *
     * @param stiffness The spring stiffness. Must be finite and positive.
     * @param dampingRatio The damping ratio. Must be finite and positive.
     * @param initialDisplacement The initial displacement from the rest position (float-space
     *   units).
     * @param initialVelocity The initial velocity (float-space units per second).
     * @param displacementThreshold Positive, finite settling tolerance in float-space units.
     */
    fun create(
        stiffness: Float,
        dampingRatio: Float,
        initialDisplacement: Float,
        initialVelocity: Float,
        displacementThreshold: Float = defaultDisplacementThreshold(initialDisplacement),
    ): SpringSolver {
      require(stiffness.isFinite() && stiffness > 0) {
        "Spring stiffness must be finite and positive"
      }
      require(dampingRatio.isFinite() && dampingRatio > 0) {
        "Spring damping ratio must be finite and positive"
      }
      require(initialDisplacement.isFinite() && initialVelocity.isFinite()) {
        "Spring displacement and velocity must be finite"
      }
      require(displacementThreshold.isFinite() && displacementThreshold > 0f) {
        "Spring displacement threshold must be finite and positive"
      }
      val omega0 = sqrt(stiffness.toDouble()) // natural frequency
      val zeta = dampingRatio.toDouble()

      val solution =
          when {
            zeta < 1.0 - CRITICAL_EPSILON -> {
              // Underdamped: oscillatory decay
              val omegaD = omega0 * sqrt(1.0 - zeta * zeta) // damped frequency
              val gamma = zeta * omega0 // decay rate
              val x0 = initialDisplacement.toDouble()
              val v0 = initialVelocity.toDouble()
              val a = x0
              val b = (v0 + gamma * x0) / omegaD
              Underdamped(gamma, omegaD, a, b)
            }
            zeta > 1.0 + CRITICAL_EPSILON -> {
              // Overdamped: two exponential decays
              val sqrtTerm = sqrt(zeta * zeta - 1.0)
              val r1 = -omega0 / (zeta + sqrtTerm)
              val r2 = -omega0 * (zeta + sqrtTerm)
              val x0 = initialDisplacement.toDouble()
              val v0 = initialVelocity.toDouble()
              val denominator = r2 - r1
              val a = (x0 * r2 - v0) / denominator
              val b = (v0 - x0 * r1) / denominator
              Overdamped(r1, r2, a, b)
            }
            else -> {
              // Critically damped: fastest non-oscillatory convergence
              val x0 = initialDisplacement.toDouble()
              val v0 = initialVelocity.toDouble()
              val a = x0
              val b = v0 + omega0 * x0
              CriticallyDamped(omega0, a, b)
            }
          }
      return SpringSolver(solution, solution.durationNanos(displacementThreshold.toDouble()))
    }

    fun defaultDisplacementThreshold(initialDisplacement: Float): Float = min(
        DEFAULT_DISPLACEMENT_THRESHOLD,
        max(MIN_DISPLACEMENT_THRESHOLD, abs(initialDisplacement) * DISPLACEMENT_THRESHOLD_RATIO),
    )

    private fun nanosAfter(seconds: Double): Long {
      require(seconds >= 0 && !seconds.isNaN()) { "Invalid spring duration" }
      if (seconds == 0.0) return 0
      if (seconds >= MAX_SECONDS) return Long.MAX_VALUE
      // nextUp accounts for multiplication rounding at large elapsed times; no fixed time guard.
      return ceil(Math.nextUp(seconds * NANOS_PER_SECOND)).toLong()
    }

    const val UNSUPPORTED_DURATION = -1L

    private const val NANOS_PER_SECOND = 1_000_000_000.0
    private val MAX_SECONDS = Long.MAX_VALUE / NANOS_PER_SECOND

    /** Epsilon for deciding between critically damped and under/overdamped regimes. */
    private const val CRITICAL_EPSILON = 0.0001

    /**
     * Displacement convergence threshold for large float-space deltas. For small unitless values
     * like alpha and scale, the threshold scales down with the initial displacement.
     */
    private const val DEFAULT_DISPLACEMENT_THRESHOLD = 0.5f

    private const val MIN_DISPLACEMENT_THRESHOLD = 0.001f

    private const val DISPLACEMENT_THRESHOLD_RATIO = 0.01f

    private const val MAX_PHASE_INDEX = 1_099_511_627_776.0 // 2^40
  }
}
