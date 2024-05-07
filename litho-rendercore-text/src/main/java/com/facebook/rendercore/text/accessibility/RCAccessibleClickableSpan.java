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

package com.facebook.rendercore.text.accessibility;

import android.text.style.ClickableSpan;
import androidx.annotation.Nullable;

/**
 * Extends the ClickableSpan class to include a dedicated field for the accessibility label. This is
 * useful in cases where we know what the span object will represent and its description is not
 * easily obtainable from its actual contents. For example, the number of likers for a story might
 * want to set the accessibility label to the corresponding plurals resource.
 */
public abstract class RCAccessibleClickableSpan extends ClickableSpan {

  private @Nullable String mAccessibilityLabel;
  private @Nullable String mAccessibilityRole;
  private boolean mIsKeyboardFocused = false;

  public RCAccessibleClickableSpan(@Nullable String accessibilityLabel) {
    this(accessibilityLabel, null);
  }

  public RCAccessibleClickableSpan(
      @Nullable String accessibilityLabel, @Nullable String accessibilityRole) {
    super();
    mAccessibilityLabel = accessibilityLabel;
    mAccessibilityRole = accessibilityRole;
  }

  @Nullable
  public String getAccessibilityLabel() {
    return mAccessibilityLabel;
  }

  public void setAccessibilityLabel(@Nullable String accessibilityLabel) {
    mAccessibilityLabel = accessibilityLabel;
  }

  @Nullable
  public String getAccessibilityRole() {
    return mAccessibilityRole;
  }

  public void setAccessibilityRole(@Nullable String accessibilityRole) {
    mAccessibilityRole = accessibilityRole;
  }

  public boolean isKeyboardFocused() {
    return mIsKeyboardFocused;
  }

  public void setKeyboardFocused(boolean isKeyboardFocused) {
    mIsKeyboardFocused = isKeyboardFocused;
  }
}
