/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.sections;

import static com.facebook.litho.FrameworkLogEvents.PARAM_LOG_TAG;
import static com.facebook.litho.FrameworkLogEvents.PARAM_SECTION_CURRENT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_SECTION_NEXT;

import android.support.annotation.IntDef;
import com.facebook.litho.ComponentsLogger;
import com.facebook.litho.LogEvent;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Utility class for creating common logging events. */
public class SectionsLogEventUtils {

  @IntDef({
    ApplyNewChangeSet.NONE,
    ApplyNewChangeSet.SET_ROOT,
    ApplyNewChangeSet.SET_ROOT_ASYNC,
    ApplyNewChangeSet.UPDATE_STATE,
    ApplyNewChangeSet.UPDATE_STATE_ASYNC
  })
  @Retention(RetentionPolicy.SOURCE)
  public @interface ApplyNewChangeSet {
    int NONE = -1;
    int SET_ROOT = 0;
    int SET_ROOT_ASYNC = 1;
    int UPDATE_STATE = 2;
    int UPDATE_STATE_ASYNC = 3;
  }

  /**
   * Create a performance event that will add the names of the current and next section as params.
   */
  public static LogEvent getSectionsPerformanceEvent(
      ComponentsLogger logger,
      String logTag,
      int eventId,
      Section currentSection,
      Section nextSection) {
    final LogEvent logEvent = logger.newPerformanceEvent(eventId);
    logEvent.addParam(
        PARAM_SECTION_CURRENT, currentSection == null ? "null" : currentSection.getSimpleName());
    logEvent.addParam(
        PARAM_SECTION_NEXT, nextSection == null ? "null" : nextSection.getSimpleName());
    logEvent.addParam(PARAM_LOG_TAG, logTag);

    return logEvent;
  }

  static String applyNewChangeSetSourceToString(@ApplyNewChangeSet int source) {
    switch (source) {
      case ApplyNewChangeSet.NONE:
        return "none";
      case ApplyNewChangeSet.SET_ROOT:
        return "set_root";
      case ApplyNewChangeSet.SET_ROOT_ASYNC:
        return "set_root_async";
      case ApplyNewChangeSet.UPDATE_STATE:
        return "update_state";
      case ApplyNewChangeSet.UPDATE_STATE_ASYNC:
        return "update_state_async";
      default:
        throw new IllegalStateException("Unknown source");
    }
  }
}
