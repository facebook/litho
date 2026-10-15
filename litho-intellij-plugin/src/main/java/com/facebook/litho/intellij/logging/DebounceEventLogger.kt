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

package com.facebook.litho.intellij.logging

import com.facebook.litho.intellij.extensions.EventLogger
import com.google.common.annotations.VisibleForTesting

/** Only logs an event if a particular timespanMillis has passed without it logging another event */
class DebounceEventLogger
@VisibleForTesting
internal constructor(private val timespanMillis: Long, private val eventLogger: EventLogger) :
    EventLogger {

  private var lastTimeMillis: Long = 0

  constructor(timespanMillis: Long) : this(timespanMillis, LithoLoggerProvider.getEventLogger())

  override fun log(event: String?, metadata: Map<String, String>) {
    var log = false
    synchronized(this) {
      val currentTimeMillis = System.currentTimeMillis()
      if (currentTimeMillis - lastTimeMillis > timespanMillis) {
        lastTimeMillis = currentTimeMillis
        log = true
      }
    }
    if (log) {
      eventLogger.log(event, metadata)
    }
  }
}
