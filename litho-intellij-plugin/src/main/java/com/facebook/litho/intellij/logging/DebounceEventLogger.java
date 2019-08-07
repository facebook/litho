/*
 * Copyright 2019-present Facebook, Inc.
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
package com.facebook.litho.intellij.logging;

import com.facebook.litho.intellij.extensions.EventLogger;
import com.google.common.annotations.VisibleForTesting;
import java.util.Map;

/** Only logs an event if a particular timespanMillis has passed without it logging another event */
public class DebounceEventLogger implements EventLogger {
  private final EventLogger eventLogger;
  private final long timespanMillis;
  private long lastTimeMillis = 0;

  public DebounceEventLogger(long timespanMillis) {
    this(timespanMillis, LithoLoggerProvider.getEventLogger());
  }

  @VisibleForTesting
  DebounceEventLogger(long timespanMillis, EventLogger logger) {
    this.timespanMillis = timespanMillis;
    this.eventLogger = logger;
  }

  @Override
  public void log(String event, Map<String, String> metadata) {
    boolean log = false;
    synchronized (this) {
      long currentTimeMillis = System.currentTimeMillis();
      if (currentTimeMillis - lastTimeMillis > timespanMillis) {
        lastTimeMillis = currentTimeMillis;
        log = true;
      }
    }
    if (log) {
      eventLogger.log(event, metadata);
    }
  }
}
