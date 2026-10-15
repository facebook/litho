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

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.text.toDouble

/** Analytical closed-form solver for the damped harmonic oscillator. */
internal class SpringSolver
private constructor(
    private val solution: Solution,
    private val displacementThreshold: Double,
    private val velocityThreshold: Double,
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

  /** Returns true if both displacement and velocity are below convergence thresholds. */
  fun isFinished(timeNanos: Long): Boolean {
    val t = timeNanos / NANOS_PER_SECOND
    return abs(solution.displacement(t)) < displacementThreshold &&
        abs(solution.velocity(t)) < velocityThreshold
  }

  private sealed interface Solution {
    fun displacement(t: Double): Double

    fun velocity(t: Double): Double
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
     * @param stiffness The spring stiffness. Must be positive.
     * @param dampingRatio The damping ratio. Must be non-negative.
     * @param initialDisplacement The initial displacement from the rest position (float-space
     *   units).
     * @param initialVelocity The initial velocity (float-space units per second).
     */
    fun create(
        stiffness: Float,
        dampingRatio: Float,
        initialDisplacement: Float,
        initialVelocity: Float,
    ): SpringSolver {
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
              val r1 = -omega0 * (zeta - sqrtTerm)
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
      val displacementScale = abs(initialDisplacement)
      val displacementThreshold = min(
          DEFAULT_DISPLACEMENT_THRESHOLD,
          max(MIN_DISPLACEMENT_THRESHOLD, displacementScale * DISPLACEMENT_THRESHOLD_RATIO),
      )
      val velocityThreshold = min(
          DEFAULT_VELOCITY_THRESHOLD,
          max(MIN_VELOCITY_THRESHOLD, displacementScale * VELOCITY_THRESHOLD_RATIO),
      )
      return SpringSolver(
          solution,
          displacementThreshold.toDouble(),
          velocityThreshold.toDouble(),
      )
    }

    private const val NANOS_PER_SECOND = 1_000_000_000.0

    /** Epsilon for deciding between critically damped and under/overdamped regimes. */
    private const val CRITICAL_EPSILON = 0.0001

    /**
     * Displacement convergence threshold for large float-space deltas. For small unitless values
     * like alpha and scale, the threshold scales down with the initial displacement.
     */
    private const val DEFAULT_DISPLACEMENT_THRESHOLD = 0.5f

    private const val MIN_DISPLACEMENT_THRESHOLD = 0.001f

    private const val DISPLACEMENT_THRESHOLD_RATIO = 0.01f

    /** Velocity convergence threshold for large float-space deltas. */
    private const val DEFAULT_VELOCITY_THRESHOLD = 0.1f

    private const val MIN_VELOCITY_THRESHOLD = 0.001f

    private const val VELOCITY_THRESHOLD_RATIO = 0.1f
  }
}
