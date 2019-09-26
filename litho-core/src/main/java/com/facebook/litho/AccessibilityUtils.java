/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static android.content.Context.ACCESSIBILITY_SERVICE;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import androidx.core.accessibilityservice.AccessibilityServiceInfoCompat;
import java.util.List;

public class AccessibilityUtils {

  private static volatile boolean isCachedIsAccessibilityEnabledSet = false;
  private static volatile boolean cachedIsAccessibilityEnabled;

  /** @returns True if accessibility touch exploration is currently enabled in the framework. */
  public static boolean isAccessibilityEnabled(Context context) {
    if (!isCachedIsAccessibilityEnabledSet) {
      final AccessibilityManager manager =
          (AccessibilityManager) context.getSystemService(ACCESSIBILITY_SERVICE);
      updateCachedIsAccessibilityEnabled(manager);
    }
    return cachedIsAccessibilityEnabled;
  }

  public static boolean isAccessibilityEnabled(AccessibilityManager manager) {
    if (!isCachedIsAccessibilityEnabledSet) {
      updateCachedIsAccessibilityEnabled(manager);
    }
    return cachedIsAccessibilityEnabled;
  }

  private static synchronized void updateCachedIsAccessibilityEnabled(
      AccessibilityManager manager) {
    cachedIsAccessibilityEnabled =
        Boolean.getBoolean("is_accessibility_enabled")
            || isRunningApplicableAccessibilityService(manager);
    isCachedIsAccessibilityEnabledSet = true;
  }

  public static synchronized void invalidateCachedIsAccessibilityEnabled() {
    isCachedIsAccessibilityEnabledSet = false;
  }

  public static boolean isRunningApplicableAccessibilityService(AccessibilityManager manager) {
    if (manager == null || !manager.isEnabled()) {
      return false;
    }

    return manager.isTouchExplorationEnabled()
        || enabledServiceCanFocusAndRetrieveWindowContent(manager);
  }

  public static boolean enabledServiceCanFocusAndRetrieveWindowContent(
      AccessibilityManager manager) {
    List<AccessibilityServiceInfo> enabledServices =
        manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

    if (enabledServices == null) {
      return false;
    }

    for (AccessibilityServiceInfo serviceInfo : enabledServices) {
      int eventTypes = serviceInfo.eventTypes;
      if ((eventTypes & AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED)
          != AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
        continue;
      }
      int capabilities = AccessibilityServiceInfoCompat.getCapabilities(serviceInfo);
      if ((capabilities & AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT)
          == AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT) {
        return true;
      }
    }

    return false;
  }
}
