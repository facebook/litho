/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections;

import com.facebook.litho.sections.logger.SectionsDebugLogger;
import com.facebook.litho.widget.RenderInfo;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Logger implements SectionsDebugLogger {

  protected Set<SectionsDebugLogger> mSectionsDebugLoggers;

  public Logger(Collection<SectionsDebugLogger> sectionsDebugLoggers) {
    mSectionsDebugLoggers = new HashSet<>();
    if (sectionsDebugLoggers != null) {
      for (SectionsDebugLogger sectionsDebugLogger : sectionsDebugLoggers) {
        if (sectionsDebugLogger != null) {
          mSectionsDebugLoggers.add(sectionsDebugLogger);
        }
      }
    }
  }

  public void logInsert(String tag, int index, RenderInfo renderInfo, String thread) {
    for (SectionsDebugLogger sectionsDebugLogger : mSectionsDebugLoggers) {
      sectionsDebugLogger.logInsert(tag, index, renderInfo, thread);
    }
  }

  public void logUpdate(String tag, int index, RenderInfo renderInfo, String thread) {
    for (SectionsDebugLogger sectionsDebugLogger : mSectionsDebugLoggers) {
      sectionsDebugLogger.logUpdate(tag, index, renderInfo, thread);
    }
  }

  public void logDelete(String tag, int index, String thread) {
    for (SectionsDebugLogger sectionsDebugLogger : mSectionsDebugLoggers) {
      sectionsDebugLogger.logDelete(tag, index, thread);
    }
  }

  public void logRequestFocus(String tag, int index, RenderInfo renderInfo, String thread) {
    for (SectionsDebugLogger sectionsDebugLogger : mSectionsDebugLoggers) {
      sectionsDebugLogger.logRequestFocus(tag, index, renderInfo, thread);
    }
  }

  @Override
  public void logRequestFocusWithOffset(
      String tag, int index, int offset, RenderInfo renderInfo, String thread) {
    for (SectionsDebugLogger sectionsDebugLogger : mSectionsDebugLoggers) {
      sectionsDebugLogger.logRequestFocusWithOffset(tag, index, offset, renderInfo, thread);
    }
  }

  public void logMove(String tag, int fromPosition, int toPosition, String thread) {
    for (SectionsDebugLogger sectionsDebugLogger : mSectionsDebugLoggers) {
      sectionsDebugLogger.logMove(tag, fromPosition, toPosition, thread);
    }
  }

  public void logShouldUpdate(
      String tag,
      Object previous,
      Object next,
      String previousPrefix,
      String nextPrefix,
      Boolean shouldUpdate,
      String thread) {
    for (SectionsDebugLogger sectionsDebugLogger : mSectionsDebugLoggers) {
      sectionsDebugLogger.logShouldUpdate(
          tag, previous, next, previousPrefix, nextPrefix, shouldUpdate, thread);
    }
  }
}
