// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.rendercore;

import android.view.MotionEvent;
import android.view.View;

/** Any interface for mounted items that need to capture motion events from its {@link HostView}. */
public interface Touchable {
  boolean onTouchEvent(MotionEvent event, View host);

  boolean shouldHandleTouchEvent(MotionEvent event);
}
