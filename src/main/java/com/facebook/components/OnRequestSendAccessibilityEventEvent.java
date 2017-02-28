// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.support.v4.view.AccessibilityDelegateCompat;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import com.facebook.components.annotations.Event;

/**
 * Components should implement an event of this type in order to receive callbacks to
 * {@link
 * android.support.v4.view.AccessibilityDelegateCompat#onRequestSendAccessibilityEvent(
 * ViewGroup, View, AccessibilityEvent)}
 */
@Event(returnType = boolean.class)
public class OnRequestSendAccessibilityEventEvent {
  public ViewGroup host;
  public View child;
  public AccessibilityEvent event;
  public AccessibilityDelegateCompat superDelegate;
}
