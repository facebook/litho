/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

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
