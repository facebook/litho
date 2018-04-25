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

import java.util.Set;

/**
 * An interface for logging events and performance events in litho as well as in user defined
 * components. The ComponentsLogger is set on the {@link ComponentContext}.
 * See {@link FrameworkLogEvents} for a list of events and parameters
 * logged internally by litho.
 */
public interface ComponentsLogger {

  /** Create a new event with the given event id. */
  LogEvent newEvent(@FrameworkLogEvents.LogEventId int eventId);

  /** Create a new performance event with the given event id and start counting the time. */
  LogEvent newPerformanceEvent(@FrameworkLogEvents.LogEventId int eventId);

  /**
   * Create a new performance event with the given event id and start counting the time. This will
   * be called instead of {@link #newPerformanceEvent(int)} if {@link
   * com.facebook.litho.config.ComponentsConfiguration#useBatchArrayAllocator} is enabled.
   */
  PerfEvent newBetterPerformanceEvent(@FrameworkLogEvents.LogEventId int eventId);

  /**
   * Log an event. Events are recycled and should not be used once logged. If the logged event is
   * a performance event it will stop counting the time.
   */
  void log(LogEvent event);

  /** Write a {@link PerfEvent} to storage. This also marks the end of the event. */
  void betterLog(PerfEvent event);

  /**
   * When a component key collision occurs, filenames that contain keywords contained in the
   * returned set will be added to the error stack trace.
   */
  Set<String> getKeyCollisionStackTraceKeywords();

  /**
   * When a component key collision occurs, filenames that match the names contained in the returned
   * set will be added to the error stack trace even if they match keywords in the whitelist.
   *
   * @see #getKeyCollisionStackTraceKeywords()
   */
  Set<String> getKeyCollisionStackTraceBlacklist();

  /** @return whether this event is being traced and getting logged. */
  boolean isTracing(LogEvent logEvent);

  /** @return whether this event is being traced and getting logged. */
  boolean isTracing(PerfEvent logEvent);
}
