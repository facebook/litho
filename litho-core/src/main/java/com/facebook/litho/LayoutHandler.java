/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

/**
 * the Layout handler is responsible for scheduling layout computations on a {@link ComponentTree}.
 * The default implementation uses a {@link android.os.Handler} with a {@link android.os.Looper}.
 */
public interface LayoutHandler {
  boolean post(Runnable runnable);
  void removeCallbacks(Runnable runnable);
  void removeCallbacksAndMessages(Object token);
}
