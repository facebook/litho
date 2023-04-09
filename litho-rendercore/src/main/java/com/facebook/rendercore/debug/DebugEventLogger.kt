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

package com.facebook.rendercore.debug

import android.util.Log
import com.facebook.rendercore.LogLevel

/** This debug event subscriber listens to all events and prints them to logcat */
class DebugEventLogger : DebugEventSubscriber(DebugEvent.All) {
  override fun onEvent(event: DebugEvent) {
    when (event.logLevel) {
      LogLevel.VERBOSE -> Log.v("rc-debug-events", event.toString())
      LogLevel.DEBUG -> Log.d("rc-debug-events", event.toString())
      LogLevel.WARNING -> Log.w("rc-debug-events", event.toString())
      LogLevel.ERROR -> Log.e("rc-debug-events", event.toString())
      LogLevel.FATAL -> Log.e("rc-debug-events", event.toString())
    }
  }
}
