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

  /**
   * Create a new performance event with the given event id and start counting the time. If the
   * logger doesn't care about this event id, it may return null.
   */
  @Nullable
  PerfEvent newPerformanceEvent(ComponentContext c, @FrameworkLogEvents.LogEventId int eventId);

  /** Write a {@link PerfEvent} to storage. This also marks the end of the event. */
  void logPerfEvent(PerfEvent event);

  /** Release a previously obtained {@link PerfEvent} without logging it. */
  void cancelPerfEvent(PerfEvent event);

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
