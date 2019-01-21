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

/**
 * Extends the ClickableSpan class to include a dedicated field for the
 * accessibility label. This is useful in cases where we know what the span
 * object will represent and its description is not easily obtainable from
 * its actual contents. For example, the number of likers for a story might
 * want to set the accessibility label to the corresponding plurals resource.
 */
public abstract class AccessibleClickableSpan extends ClickableSpan {
  private String mAccessibilityDescription;

  public String getAccessibilityDescription() {
    return mAccessibilityDescription;
  }

  public void setAccessibilityDescription(String accessibilityDescription) {
    mAccessibilityDescription = accessibilityDescription;
  }

  public AccessibleClickableSpan(String accessibilityDescription) {
    super();
    mAccessibilityDescription = accessibilityDescription;
  }
}
