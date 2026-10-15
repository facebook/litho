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

package com.facebook.samples.litho.kotlin.debug

import android.graphics.Rect
import android.util.Log
import com.facebook.litho.debug.LithoTouchEventDebugger.ATTRIBUTE_BOUNDS
import com.facebook.litho.debug.LithoTouchEventDebugger.ATTRIBUTE_EVENT
import com.facebook.litho.debug.LithoTouchEventDebugger.ATTRIBUTE_HANDLER
import com.facebook.litho.debug.TouchEventType.Companion.ALL_TOUCH_EVENT_NAMES
import com.facebook.rendercore.debug.DebugEvent
import com.facebook.rendercore.debug.DebugEventSubscriber

class ExampleTouchEventSubscriber : DebugEventSubscriber(events = ALL_TOUCH_EVENT_NAMES) {
  override fun onEvent(event: DebugEvent) {
    Log.d(
        "LithoTouchEventDebugger",
        "LithoTouchEventDebugger : ${event.type}\n" +
            "handler: ${event.attribute<String>(ATTRIBUTE_HANDLER)}\n" +
            "bounds: ${event.attribute<Rect>(ATTRIBUTE_BOUNDS)}\n" +
            "event: ${event.attribute<Any?>(ATTRIBUTE_EVENT)}\n",
    )
  }
}
