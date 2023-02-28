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

import com.facebook.litho.FrameworkLogEvents.LogEventId

/**
 * An interface for logging events and performance events in litho as well as in user defined
 * components. The ComponentsLogger is set on the [ComponentContext]. See [FrameworkLogEvents] for a
 * list of events and parameters logged internally by Litho.
 */
interface ComponentsLogger {

  enum class LogLevel {
    WARNING,
    ERROR,
    FATAL
  }

  /**
   * Create a new performance event with the given event id and start counting the time. If the
   * logger doesn't care about this event id, it may return `null`.
   */
  fun newPerformanceEvent(c: ComponentContext, @LogEventId eventId: Int): PerfEvent?

  /** Write a [PerfEvent] to storage. This also marks the end of the event. */
  fun logPerfEvent(event: PerfEvent)

  /** Release a previously obtained [PerfEvent] without logging it. */
  fun cancelPerfEvent(event: PerfEvent)

  /**
   * Provide additional log metadata based on the tree props of the component hierarchy currently
   * being logged. This can be useful if information about the component hierarchy is needed.
   *
   * @param treeProps The treeprops available in the hierarchy.
   * @return Null for efficiency purposes when no data needs to be logged, associative map
   *   otherwise.
   */
  fun getExtraAnnotations(treeProps: TreeProps): Map<String, String>?

  /** @return whether this event is being traced and getting logged. */
  fun isTracing(logEvent: PerfEvent): Boolean
}
