/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
   * If this is False, then a Service created from SectionLifecycle#createService(SectionContext,
   * Section) is registered and kept in ServiceRegistry.
   *
   * <p>SectionLifecycle#destroyService(SectionContext, Object) will not be called if this is True
   */
  public static boolean noServiceRegistration = false;
}
