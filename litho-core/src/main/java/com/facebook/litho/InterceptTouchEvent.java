/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
