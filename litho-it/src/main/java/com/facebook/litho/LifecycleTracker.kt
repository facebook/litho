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

import android.graphics.Rect
import com.facebook.litho.LifecycleStep.StepInfo

/**
 * Can be used to track the sequence of MountSpec callbacks and optionally additional info related
 * to each callback.
 */
class LifecycleTracker {
  var isMounted: Boolean = false
    private set

  var isBound: Boolean = false
    private set

  var isAttached: Boolean = false
    private set

  var isMeasured: Boolean = false
    private set

  val intrinsicSize: Size
    get() = getInfo(LifecycleStep.ON_MEASURE).args?.get(0) as? Size ?: Size()

  val width: Int
    get() = (getInfo(LifecycleStep.ON_BOUNDS_DEFINED).args?.get(0) as? Rect)?.width() ?: 0

  val height: Int
    get() = (getInfo(LifecycleStep.ON_BOUNDS_DEFINED).args?.get(0) as? Rect)?.height() ?: 0

  val steps: List<LifecycleStep>
    get() = LifecycleStep.getSteps(_steps)

  private val _steps = mutableListOf<StepInfo>()

  fun addStep(step: LifecycleStep?, vararg args: Any?) {
    _steps.add(StepInfo(step, *args))
    when (step) {
      LifecycleStep.ON_MEASURE -> isMeasured = true
      LifecycleStep.ON_MOUNT -> isMounted = true
      LifecycleStep.ON_UNMOUNT -> isMounted = false
      LifecycleStep.ON_BIND -> isBound = true
      LifecycleStep.ON_UNBIND -> isBound = false
      LifecycleStep.ON_ATTACHED -> isAttached = true
      LifecycleStep.ON_DETACHED -> isAttached = false
      else -> {}
    }
  }

  fun reset() {
    _steps.clear()
    isMounted = false
    isBound = false
    isAttached = false
    isMeasured = false
  }

  private fun getInfo(step: LifecycleStep): StepInfo {
    for (stepInfo in _steps) {
      if (stepInfo.step == step) {
        return stepInfo
      }
    }
    throw IllegalStateException("'$step' was not called or lifecycle steps were reset.")
  }
}
