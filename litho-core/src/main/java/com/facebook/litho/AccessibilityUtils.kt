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
import com.facebook.litho.config.LithoDebugConfigurations

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
    val enabledServices =
        manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    return manager.isTouchExplorationEnabled ||
        enabledServiceCanFocusAndRetrieveWindowContent(enabledServices) ||
        (LithoDebugConfigurations.enableFrameworkA11yClientDetection &&
            isFrameworkAccessibilityClientConnected(enabledServices))
  }

  /**
   * Detects framework-level accessibility clients like UiAutomation (used by uiautomator). These
   * clients set [AccessibilityManager.isEnabled] to true but do not appear in
   * [AccessibilityManager.getEnabledAccessibilityServiceList] because they are privileged framework
   * clients, not user-installed services.
   *
   * When such a client is connected, Litho enables accessibility support (host views, delegates,
   * virtual nodes) so that tools like uiautomator can see the full accessibility tree. The existing
   * [LithoView.AccessibilityStateChangeListener] handles the lifecycle — it triggers a relayout
   * when the client connects and another when it disconnects, so hosts are only created while the
   * client is active.
   *
   * Heuristic: if accessibility is enabled but no services are in the list, a framework-level
   * client (UiAutomation) is connected. UiAutomation is the only known trigger for this state. If
   * this ever fires without a framework client, the only cost is unnecessary host views
   * (performance, not correctness).
   */
  private fun isFrameworkAccessibilityClientConnected(
      enabledServices: List<AccessibilityServiceInfo>?,
  ): Boolean {
    return enabledServices.isNullOrEmpty()
  }

  @JvmStatic
  fun enabledServiceCanFocusAndRetrieveWindowContent(
      enabledServices: List<AccessibilityServiceInfo>?,
  ): Boolean {
    if (enabledServices == null) return false
    for (serviceInfo in enabledServices) {
      val eventTypes = serviceInfo.eventTypes
      if (
          eventTypes and AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED !=
              AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
      ) {
        continue
      }
      val capabilities = AccessibilityServiceInfoCompat.getCapabilities(serviceInfo)
      if (
          capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT ==
              AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT
      ) {
        return true
      }
    }
    return false
  }
}
