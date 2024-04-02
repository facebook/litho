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

package com.facebook.rendercore.utils

import android.content.Context
import android.view.WindowManager
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

object VSyncUtils {
  private const val DEFAULT_REFRESH_RATE = 60.0
  private const val REFRESH_RATE_MIN = 30.0
  private const val REFRESH_RATE_MAX = 240.0
  private val ONE_SECOND_IN_NS = TimeUnit.SECONDS.toNanos(1)

  private val vsyncTimeNs: AtomicInteger = AtomicInteger(-1)

  @JvmStatic
  fun getNormalVsyncTime(context: Context): Int {
    var value = vsyncTimeNs.get()
    if (value != -1) {
      return value
    }

    // Initialize the value.
    val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    var refreshRate = display.refreshRate.toDouble()

    // If refresh rate is lower than 0, it means the OS is not reporting a correct value. We
    // will
    // assume it is 60.
    refreshRate =
        if (refreshRate < 0) {
          DEFAULT_REFRESH_RATE
        } else {
          // Cap refresh rates between 30 and 240. Anything else is unreasonable.
          refreshRate.coerceIn(REFRESH_RATE_MIN, REFRESH_RATE_MAX)
        }

    value = (ONE_SECOND_IN_NS / refreshRate).roundToInt()
    vsyncTimeNs.compareAndSet(-1, value)

    return value
  }
}
