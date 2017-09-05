/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections;

import com.facebook.litho.sections.logger.SectionComponentLogger;
import com.facebook.litho.widget.RenderInfo;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Logger implements SectionComponentLogger {

  protected Set<SectionComponentLogger> mSectionComponentLoggers;

  public Logger(Collection<SectionComponentLogger> sectionComponentLoggers) {
    mSectionComponentLoggers = new HashSet<>();
    if (sectionComponentLoggers != null) {
      for (SectionComponentLogger sectionComponentLogger: sectionComponentLoggers) {
        if (sectionComponentLogger != null) {
          mSectionComponentLoggers.add(sectionComponentLogger);
        }
      }
    }
  }

  public void logInsert(String tag, int index, RenderInfo renderInfo, String thread) {
    for (SectionComponentLogger sectionComponentLogger : mSectionComponentLoggers) {
      sectionComponentLogger.logInsert(tag, index, renderInfo, thread);
    }
  }

  public void logUpdate(String tag, int index, RenderInfo renderInfo, String thread) {
    for (SectionComponentLogger sectionComponentLogger : mSectionComponentLoggers) {
      sectionComponentLogger.logUpdate(tag, index, renderInfo, thread);
    }
  }

  public void logDelete(String tag, int index, String thread) {
    for (SectionComponentLogger sectionComponentLogger : mSectionComponentLoggers) {
      sectionComponentLogger.logDelete(tag, index, thread);
    }
  }

  public void logRequestFocus(String tag, int index, RenderInfo renderInfo, String thread) {
    for (SectionComponentLogger sectionComponentLogger : mSectionComponentLoggers) {
      sectionComponentLogger.logRequestFocus(tag, index, renderInfo, thread);
    }
  }

  @Override
  public void logRequestFocusWithOffset(
      String tag, int index, int offset, RenderInfo renderInfo, String thread) {
    for (SectionComponentLogger sectionComponentLogger : mSectionComponentLoggers) {
      sectionComponentLogger.logRequestFocusWithOffset(tag, index, offset, renderInfo, thread);
    }
  }

  public void logMove(String tag, int fromPosition, int toPosition, String thread) {
    for (SectionComponentLogger sectionComponentLogger : mSectionComponentLoggers) {
      sectionComponentLogger.logMove(tag, fromPosition, toPosition, thread);
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
    for (SectionComponentLogger sectionComponentLogger : mSectionComponentLoggers) {
      sectionComponentLogger.logShouldUpdate(
          tag, previous, next, previousPrefix, nextPrefix, shouldUpdate, thread);
    }
  }
}
