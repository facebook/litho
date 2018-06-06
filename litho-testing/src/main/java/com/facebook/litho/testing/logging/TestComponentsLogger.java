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

package com.facebook.litho.testing.logging;

import com.facebook.litho.BaseComponentsLogger;
import com.facebook.litho.FrameworkLogEvents;
import com.facebook.litho.LogEvent;
import com.facebook.litho.PerfEvent;
import com.facebook.litho.TestPerfEvent;
import java.util.LinkedList;
import java.util.List;

/**
 * In test environments we don't want to recycle the events as mockity hold on to them. We therefor
 * override log() to not call release.
 */
public class TestComponentsLogger extends BaseComponentsLogger {

  private final List<PerfEvent> mLoggedPerfEvents = new LinkedList<>();

  @Override
  public void log(LogEvent event) {
    if (event.isPerformanceEvent()) {
      onPerformanceEventEnded(event);
    } else {
      onEvent(event);
    }
  }

  @Override
  public boolean isTracing(LogEvent logEvent) {
    return true;
  }

  @Override
  public boolean isTracing(PerfEvent logEvent) {
    return true;
  }

  @Override
  public void onPerformanceEventStarted(LogEvent event) {}

  @Override
  public void onPerformanceEventEnded(LogEvent event) {}

  @Override
  public void onEvent(LogEvent event) {}

  @Override
  public PerfEvent newBetterPerformanceEvent(@FrameworkLogEvents.LogEventId int eventId) {
    return new TestPerfEvent(eventId);
  }

  @Override
  public void betterLog(PerfEvent event) {
    mLoggedPerfEvents.add(event);
  }

  @Override
  public void emitMessage(LogLevel level, String message) {}

  public List<PerfEvent> getLoggedPerfEvents() {
    return mLoggedPerfEvents;
  }

  public void reset() {
    mLoggedPerfEvents.clear();
  }
}
