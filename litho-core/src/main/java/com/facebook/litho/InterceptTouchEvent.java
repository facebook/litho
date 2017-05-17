/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.view.MotionEvent;
import android.view.ViewGroup;

import com.facebook.litho.annotations.Event;

/**
 * Components should implement an event of this type in order to intercept Android touch events.
 * The method is equivalent to the Android method
 * {@link ViewGroup#onInterceptTouchEvent(MotionEvent)} - implementations should return true if
 * they intercepted the event and wish to receive subsequent events, and false otherwise. An
 * example of the correct usage is:
 *
 * <pre>
 * {@code
 *
 * @OnEvent(InterceptTouchEvent.class)
 * static boolean onInterceptTouchEvent(
 *     @FromEvent MotionEvent motionEvent,
 *     @Param Param someParam
 *     @Prop Prop someProp) {
 *   return shouldInterceptEvent(someParam, someProp);
 * }
 * </pre>
 */
@Event(returnType = boolean.class)
public class InterceptTouchEvent {
  public MotionEvent motionEvent;
}
