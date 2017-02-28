// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.os.Bundle;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.view.View;

import com.facebook.components.annotations.Event;

/**
 * Components should implement an event of this type in order to receive callbacks to
 * {@link
 * android.view.View.AccessibilityDelegate#performAccessibilityAction(View, int, Bundle)}
 */
@Event(returnType = boolean.class)
public class PerformAccessibilityActionEvent {
  public View host;
  public int action;
  public Bundle args;
  public AccessibilityDelegateCompat superDelegate;
}
