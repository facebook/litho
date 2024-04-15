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

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.core.accessibilityservice.AccessibilityServiceInfoCompat

object AccessibilityUtils {

  @Volatile private var isCachedIsAccessibilityEnabledSet = false
  @Volatile private var cachedIsAccessibilityEnabled = false

  /** @returns True if accessibility touch exploration is currently enabled in the framework. */
  @JvmStatic
  fun isAccessibilityEnabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    return isAccessibilityEnabled(manager)
  }

  @JvmStatic
  fun isAccessibilityEnabled(manager: AccessibilityManager?): Boolean {
    if (!isCachedIsAccessibilityEnabledSet) {
      updateCachedIsAccessibilityEnabled(manager)
    }
    return cachedIsAccessibilityEnabled
  }

  @Synchronized
  private fun updateCachedIsAccessibilityEnabled(manager: AccessibilityManager?) {
    cachedIsAccessibilityEnabled =
        (java.lang.Boolean.getBoolean("is_accessibility_enabled") ||
            isRunningApplicableAccessibilityService(manager))
    isCachedIsAccessibilityEnabledSet = true
  }

  @JvmStatic
  @Synchronized
  fun invalidateCachedIsAccessibilityEnabled() {
    isCachedIsAccessibilityEnabledSet = false
  }

  @JvmStatic
  fun isRunningApplicableAccessibilityService(manager: AccessibilityManager?): Boolean {
    if (manager?.isEnabled != true) {
      return false
    }
    return manager.isTouchExplorationEnabled ||
        enabledServiceCanFocusAndRetrieveWindowContent(manager)
  }

  @JvmStatic
  fun enabledServiceCanFocusAndRetrieveWindowContent(manager: AccessibilityManager): Boolean {
    val enabledServices =
        manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            ?: return false
    for (serviceInfo in enabledServices) {
      val eventTypes = serviceInfo.eventTypes
      if (eventTypes and AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED !=
          AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
        continue
      }
      val capabilities = AccessibilityServiceInfoCompat.getCapabilities(serviceInfo)
      if (capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT ==
          AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT) {
        return true
      }
    }
    return false
  }
}
