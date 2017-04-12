/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import android.support.annotation.IntDef;

@IntDef({
    ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_AUTO,
    ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_YES,
    ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_NO,
    ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
})
@Retention(RetentionPolicy.SOURCE)
public @interface ImportantForAccessibility {
  /**
   * Automatically determine whether a view is important for accessibility.
   */
  int IMPORTANT_FOR_ACCESSIBILITY_AUTO = 0x00000000;

  /**
   * The view is important for accessibility.
   */
  int IMPORTANT_FOR_ACCESSIBILITY_YES = 0x00000001;

  /**
   * The view is not important for accessibility.
   */
  int IMPORTANT_FOR_ACCESSIBILITY_NO = 0x00000002;

  /**
   * The view is not important for accessibility, nor are any of its
   * descendant views.
   */
  int IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS = 0x00000004;
}
