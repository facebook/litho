// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.View;

import com.facebook.components.annotations.Event;
/**
 * Components should implement an event of this type in order to receive callbacks to
 * {@link
 * android.support.v4.view.AccessibilityDelegateCompat#onInitializeAccessibilityNodeInfo(
 * View, AccessibilityNodeInfoCompat)}
 */
@Event
public class OnInitializeAccessibilityNodeInfoEvent {
  public View host;
  public AccessibilityNodeInfoCompat info;
  public AccessibilityDelegateCompat superDelegate;
}
