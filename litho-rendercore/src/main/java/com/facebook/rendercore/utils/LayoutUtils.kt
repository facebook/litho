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
import android.content.pm.ApplicationInfo
import android.view.View

object LayoutUtils {

  /** @return whether the current Activity has RTL layout direction. */
  @JvmStatic
  fun isLayoutDirectionRTL(context: Context): Boolean {
    val applicationInfo = context.applicationInfo
    if (applicationInfo.flags and ApplicationInfo.FLAG_SUPPORTS_RTL != 0) {
      val layoutDirection = API17LayoutUtils.getLayoutDirection(context)
      return layoutDirection == View.LAYOUT_DIRECTION_RTL
    }
    return false
  }

  /** Separate class to limit scope of pre-verification failures on older devices. */
  private object API17LayoutUtils {
    @JvmStatic
    fun getLayoutDirection(context: Context): Int {
      return context.resources.configuration.layoutDirection
    }
  }
}
