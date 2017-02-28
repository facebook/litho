// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.support.v4.view.AccessibilityDelegateCompat;
import android.view.View;

import com.facebook.components.annotations.Event;

/**
 * Components should implement an event of this type in order to receive callbacks to
 * {@link android.support.v4.view.AccessibilityDelegateCompat#sendAccessibilityEvent(View, int)}
 */
@Event
public class SendAccessibilityEventEvent {
  public View host;
  public int eventType;
  public AccessibilityDelegateCompat superDelegate;
}
