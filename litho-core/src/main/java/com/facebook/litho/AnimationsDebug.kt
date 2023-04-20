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

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.facebook.litho.config.ComponentsConfiguration
import kotlin.jvm.JvmField

/** Utilities for animations debug. */
object AnimationsDebug {
  @JvmField val ENABLED = ComponentsConfiguration.isEndToEndTestRun
  const val TAG = "LithoAnimationDebug"

  @JvmStatic
  fun debugPrintLayoutState(layoutState: LayoutState) {
    if (!ENABLED) {
      return
    }
    Log.d(TAG, layoutState.dumpAsString())
  }

  @JvmStatic
  fun areTransitionsEnabled(context: Context?): Boolean {
    if (ComponentsConfiguration.isAnimationDisabled) {
      // mostly for unit tests
      return false
    }
    if (!ComponentsConfiguration.isEndToEndTestRun) {
      return true
    }
    if (!ComponentsConfiguration.CAN_CHECK_GLOBAL_ANIMATOR_SETTINGS) {
      return false
    }
    if (context == null) {
      return false
    }
    val animatorDurationScale =
        Settings.Global.getFloat(
            context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    return (ComponentsConfiguration.forceEnableTransitionsForInstrumentationTests ||
        animatorDurationScale != 0f)
  }
}
