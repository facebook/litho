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

import com.facebook.litho.annotations.Event;

/**
 * Components should implement an event of this type in order to receive Android touch events. The
 * method is equivalent to the Android method {@link View#onTouchEvent(MotionEvent)} -
 * implementations should return true if they consumed the event and wish to receive subsequent
 * events, and false otherwise. An example of the correct usage is:
 *
 * <pre>
 * {@code
 *
 * @OnEvent(TouchEvent.class)
 * static boolean onTouch(
 *     @FromEvent View view,
 *     @FromEvent MotionEvent motionEvent,
 *     @Param Param someParam
 *     @Prop Prop someProp) {
 *   if (shouldHandleEvent(someParam, someProp)) {
 *     handleEvent(view, motionEvent);
 *     return true;
 *   }
 *
 *   return false;
 * }
 * </pre>
 */
@Event(returnType = boolean.class)
public class TouchEvent {
  public View view;
  public MotionEvent motionEvent;
}
