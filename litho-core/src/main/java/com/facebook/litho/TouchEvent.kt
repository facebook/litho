/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho

import android.view.MotionEvent
import android.view.View
import com.facebook.litho.annotations.Event

/**
 * Components should implement an event of this type in order to receive Android touch events. The
 * method is equivalent to the Android method [View.onTouchEvent] - implementations should return
 * true if they consumed the event and wish to receive subsequent events, and false otherwise. An
 * example of the correct usage is:
 * ```
 * @OnEvent(TouchEvent.class)
 * static boolean onTouch(
 *   @FromEvent View view,
 *   @FromEvent MotionEvent motionEvent,
 *   @Param Param someParam,
 *   @Prop Prop someProp) {
 *
 *   if (shouldHandleEvent(someParam, someProp)) {
 *     handleEvent(view, motionEvent);
 *     return true;
 *   }
 *
 *   return false;
 * }
 * ```
 */
@Event(returnType = Boolean::class)
class TouchEvent() {
  lateinit var view: View
  lateinit var motionEvent: MotionEvent
}
