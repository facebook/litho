// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.content.Context;
import android.support.v4.view.accessibility.AccessibilityManagerCompat;
import android.view.accessibility.AccessibilityManager;

import static android.content.Context.ACCESSIBILITY_SERVICE;

class AccessibilityUtils {
  private static final boolean ACCESSIBILITY_ENABLED =
      Boolean.getBoolean("is_accessibility_enabled");

  /**
   * @returns True if accessibility touch exploration is currently enabled
   * in the framework.
   */
  public static boolean isAccessibilityEnabled(Context context) {
    final AccessibilityManager manager =
        (AccessibilityManager) context.getSystemService(ACCESSIBILITY_SERVICE);
    return ACCESSIBILITY_ENABLED
        || (manager.isEnabled() && AccessibilityManagerCompat.isTouchExplorationEnabled(manager));
  }
}
