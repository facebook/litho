/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

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
