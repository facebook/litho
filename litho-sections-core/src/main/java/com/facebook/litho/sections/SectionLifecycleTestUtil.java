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

import androidx.annotation.VisibleForTesting;
import com.facebook.litho.StateContainer;

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

  public static StateContainer getStateContainer(Section section) {
    return section.getStateContainer();
  }

  public static void setScopedContext(Section section, SectionContext c) {
    section.setScopedContext(c);
  }
}
