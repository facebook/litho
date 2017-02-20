// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

/**
 * the Layout handler is responsible for scheduling layout computations on a {@link ComponentTree}.
 * The default implementation uses a {@link android.os.Handler} with a {@link android.os.Looper}.
 */
public interface LayoutHandler {
  boolean post(Runnable runnable);
  void removeCallbacks(Runnable runnable);
  void removeCallbacksAndMessages(Object token);
}
