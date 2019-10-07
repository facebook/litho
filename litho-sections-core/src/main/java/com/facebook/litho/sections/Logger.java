/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

  @Override
  public void logInsert(String tag, int index, RenderInfo renderInfo, String thread) {
    for (SectionsDebugLogger sectionsDebugLogger : mSectionsDebugLoggers) {
      sectionsDebugLogger.logInsert(tag, index, renderInfo, thread);
    }
  }

  @Override
  public void logUpdate(String tag, int index, RenderInfo renderInfo, String thread) {
    for (SectionsDebugLogger sectionsDebugLogger : mSectionsDebugLoggers) {
      sectionsDebugLogger.logUpdate(tag, index, renderInfo, thread);
    }
  }

  @Override
  public void logDelete(String tag, int index, String thread) {
    for (SectionsDebugLogger sectionsDebugLogger : mSectionsDebugLoggers) {
      sectionsDebugLogger.logDelete(tag, index, thread);
    }
  }

  @Override
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

  @Override
  public void logMove(String tag, int fromPosition, int toPosition, String thread) {
    for (SectionsDebugLogger sectionsDebugLogger : mSectionsDebugLoggers) {
      sectionsDebugLogger.logMove(tag, fromPosition, toPosition, thread);
    }
  }

  @Override
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
