/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Base implementation of {@link ComponentsLogger} which handles pooling event objects.
 */
public abstract class BaseComponentsLogger implements ComponentsLogger {

  /** Filenames that match these keywords will be added to the stack trace. */
  private static final Set<String> sStackTraceKeywords = new HashSet<>();

  /** Filenames that match these blacklisted items will be excluded from the stack trace. */
  private static final Set<String> sStackTraceBlacklist = new HashSet<>();

  static {
    sStackTraceKeywords.add("Spec.java");
    sStackTraceKeywords.add("Activity.java");
  }

  @Override
  public LogEvent newEvent(int eventId) {
    return ComponentsPools.acquireLogEvent(eventId);
  }

  @Override
  public LogEvent newPerformanceEvent(int eventId) {
    final LogEvent event = newEvent(eventId);
    event.setIsPerformanceEvent(true);
    onPerformanceEventStarted(event);
    return event;
  }

  @Override
  public void log(LogEvent event) {
    if (event.isPerformanceEvent()) {
      onPerformanceEventEnded(event);
    } else {
      onEvent(event);
    }

    ComponentsPools.release(event);
  }

  @Override
  public Set<String> getKeyCollisionStackTraceKeywords() {
    return Collections.unmodifiableSet(sStackTraceKeywords);
  }

  @Override
  public Set<String> getKeyCollisionStackTraceBlacklist() {
    return Collections.unmodifiableSet(sStackTraceBlacklist);
  }

  /**
   * Log the start of a performance event. This is where you would start a timer and associate it
   * with the given event object.
   */
  public abstract void onPerformanceEventStarted(LogEvent event);

  /**
   * Log the end of a performance event. This is where you would end a timer and associate it
   * with the given event object and log the event.
   */
  public abstract void onPerformanceEventEnded(LogEvent event);

  /**
   * Log a non-performance event.
   */
  public abstract void onEvent(LogEvent event);
}
