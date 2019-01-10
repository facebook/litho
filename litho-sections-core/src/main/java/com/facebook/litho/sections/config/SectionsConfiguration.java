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

package com.facebook.litho.sections.config;

import com.facebook.litho.sections.logger.SectionsDebugLogger;
import java.util.List;

/** Configuration for the SectionComponents library */
public class SectionsConfiguration {

  /** Loggers for the core framework */
  public static List<SectionsDebugLogger> LOGGERS;

  /** Force all section component prop updates to be async */
  public static boolean sectionComponentsAsyncPropUpdates = false;

  /** Force all section component state updates to be async */
  public static boolean sectionComponentsAsyncStateUpdates = false;

  /**
   * If true, this will trim the items that pass the comparison check in the head and tail of the
   * DataDiffSection data before diffing.
   */
  public static boolean trimDataDiffSectionHeadAndTail = false;

  /**
   * If true, this will trim only the items that are the same instance in the head and tail of the
   * DataDiffSection data before diffing.
   */
  public static boolean trimSameInstancesOnly = false;

  /** Whether changesets can be applied from a background thread. */
  public static boolean useBackgroundChangeSets = false;
}
