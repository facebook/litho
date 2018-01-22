/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.sections;

import static com.facebook.litho.FrameworkLogEvents.PARAM_SECTION_CURRENT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_SECTION_NEXT;

import com.facebook.litho.ComponentsLogger;
import com.facebook.litho.LogEvent;

/** Utility class for creating common logging events. */
public class SectionsLogEventUtils {

  /**
   * Create a performance event that will add the names of the current and next section as params.
   */
  public static LogEvent getSectionsPerformanceEvent(
      ComponentsLogger logger, int eventId, Section currentSection, Section nextSection) {
    final LogEvent logEvent = logger.newPerformanceEvent(eventId);
    logEvent.addParam(
        PARAM_SECTION_CURRENT, currentSection == null ? "null" : currentSection.getSimpleName());
    logEvent.addParam(
        PARAM_SECTION_NEXT, nextSection == null ? "null" : nextSection.getSimpleName());

    return logEvent;
  }
}
