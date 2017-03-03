// Copyright 2004-present Facebook. All Rights Reserved.

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
