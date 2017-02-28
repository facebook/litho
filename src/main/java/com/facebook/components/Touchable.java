// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.view.MotionEvent;
import android.view.View;

/**
 * Any interface for mounted items that need to capture motion events from its
 * {@link ComponentHost}.
 */
public interface Touchable {
  boolean onTouchEvent(MotionEvent event, View host);
  boolean shouldHandleTouchEvent(MotionEvent event);
}
