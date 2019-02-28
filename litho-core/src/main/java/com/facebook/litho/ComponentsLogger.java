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

import androidx.annotation.Nullable;
import java.util.Map;
import java.util.Set;

/**
 * An interface for logging events and performance events in litho as well as in user defined
 * components. The ComponentsLogger is set on the {@link ComponentContext}. See {@link
 * FrameworkLogEvents} for a list of events and parameters logged internally by Litho.
 */
public interface ComponentsLogger {

  enum LogLevel {
    WARNING,
    ERROR,
    FATAL
  }

  /** Create a new performance event with the given event id and start counting the time. */
  PerfEvent newPerformanceEvent(@FrameworkLogEvents.LogEventId int eventId);

  /** Write a {@link PerfEvent} to storage. This also marks the end of the event. */
  void logPerfEvent(PerfEvent event);

  /** Release a previously obtained {@link PerfEvent} without logging it. */
  void cancelPerfEvent(PerfEvent event);

  /**
   * Emit a message that can be logged or escalated by the logger implementation.
   *
   * @param level
   * @param message Message to log
   */
  void emitMessage(LogLevel level, String message);

  /**
   * Emit a message that can be logged or escalated by the logger implementation.
   *
   * @param level
   * @param message Message to log
   * @param samplingFrequency sampling frequency to override default one
   */
  void emitMessage(LogLevel level, String message, int samplingFrequency);

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

  /**
   * Provide additional log metadata based on the tree props of the component hierarchy currently
   * being logged. This can be useful if information about the component hierarchy is needed.
   *
   * @param treeProps The treeprops available in the hierarchy.
   * @return Null for efficiency purposes when no data needs to be logged, associative map
   *     otherwise.
   */
  @Nullable
  Map<String, String> getExtraAnnotations(TreeProps treeProps);

  /** @return whether this event is being traced and getting logged. */
  boolean isTracing(PerfEvent logEvent);
}
