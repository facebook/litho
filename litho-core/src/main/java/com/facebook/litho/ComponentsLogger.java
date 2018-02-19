/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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

  /**
   * Create a new event with the given event id.
   */
  LogEvent newEvent(int eventId);

  /**
   * Create a new performance event with the given event id and start counting the time.
   */
  LogEvent newPerformanceEvent(int eventId);

  /**
   * Log an event. Events are recycled and should not be used once logged. If the logged event is
   * a performance event it will stop counting the time.
   */
  void log(LogEvent event);

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
}
