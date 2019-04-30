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

package com.facebook.widget.accessibility.delegates;

import android.text.style.ClickableSpan;
import com.facebook.litho.AccessibilityRole;
import javax.annotation.Nullable;

/**
 * Extends the ClickableSpan class to include a dedicated field for the
 * accessibility label. This is useful in cases where we know what the span
 * object will represent and its description is not easily obtainable from
 * its actual contents. For example, the number of likers for a story might
 * want to set the accessibility label to the corresponding plurals resource.
 */
public abstract class AccessibleClickableSpan extends ClickableSpan {
  private @Nullable String mAccessibilityDescription;
  @AccessibilityRole.AccessibilityRoleType @Nullable private String mAccessibilityRole;

  public @Nullable String getAccessibilityDescription() {
    return mAccessibilityDescription;
  }

  public void setAccessibilityDescription(String accessibilityDescription) {
    mAccessibilityDescription = accessibilityDescription;
  }

  @AccessibilityRole.AccessibilityRoleType
  public @Nullable String getAccessibilityRole() {
    return mAccessibilityRole;
  }

  public void setAccessibilityRole(
      @AccessibilityRole.AccessibilityRoleType String accessibilityRole) {
    mAccessibilityRole = accessibilityRole;
  }

  public AccessibleClickableSpan(@Nullable String accessibilityDescription) {
    this(accessibilityDescription, null);
  }

  public AccessibleClickableSpan(
      @Nullable String accessibilityDescription,
      @Nullable @AccessibilityRole.AccessibilityRoleType String accessibilityRole) {
    super();
    mAccessibilityDescription = accessibilityDescription;
    mAccessibilityRole = accessibilityRole;
  }
}
