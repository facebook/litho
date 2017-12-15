/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections;

import android.support.annotation.VisibleForTesting;

/**
 * A utility elevating some of the visibility constraints internal classes, including {@link
 * SectionLifecycle}, {@link SectionContext} and {@link Section} to ease testing.
 */
@VisibleForTesting
public final class SectionLifecycleTestUtil {
  private SectionLifecycleTestUtil() {}

  public static boolean isDiffSectionSpec(SectionLifecycle sectionLifecycle) {
    return sectionLifecycle.isDiffSectionSpec();
  }

  public static Children createChildren(
      SectionLifecycle sectionLifecycle, SectionContext c, Section component) {
    return sectionLifecycle.createChildren(c);
  }

  public static void createInitialState(SectionLifecycle lifecycle, SectionContext c, Section s) {
    lifecycle.createInitialState(c);
  }

  public static SectionLifecycle.StateContainer getStateContainer(Section section) {
    return section.getStateContainer();
  }

  public static void setScopedContext(Section section, SectionContext c) {
    section.setScopedContext(c);
  }
}
