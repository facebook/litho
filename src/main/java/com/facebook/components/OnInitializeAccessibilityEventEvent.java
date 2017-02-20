// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.support.v4.view.AccessibilityDelegateCompat;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import com.facebook.components.annotations.Event;

/**
 * Components should implement an event of this type in order to receive callbacks to
 * {@link android.support.v4.view.AccessibilityDelegateCompat#onInitializeAccessibilityEvent(
 * View, AccessibilityEvent)}
 */
@Event
public class OnInitializeAccessibilityEventEvent {
  public View host;
  public AccessibilityEvent event;
  public AccessibilityDelegateCompat superDelegate;
}
