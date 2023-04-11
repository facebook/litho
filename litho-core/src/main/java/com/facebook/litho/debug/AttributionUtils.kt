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

@file:JvmName("AttributionUtils")

package com.facebook.litho.debug

import com.facebook.rendercore.LogLevel
import com.facebook.rendercore.debug.DebugEventDispatcher

fun getAttribution(attribution: String?): String? {
  if (attribution?.isNotEmpty() == true || DebugEventDispatcher.minLogLevel <= LogLevel.VERBOSE) {
    return attribution
  }

  val stack = RuntimeException().stackTrace
  val frame =
      stack.find { frame ->
        !frame.className.contains("com.facebook.litho.ComponentTree") &&
            !frame.className.contains("com.facebook.litho.debug.") &&
            !frame.className.contains("com.facebook.rendercore.debug")
      }

  return frame?.toString() ?: attribution
}
