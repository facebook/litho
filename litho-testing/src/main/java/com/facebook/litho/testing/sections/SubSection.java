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

package com.facebook.litho.testing.sections;

import com.facebook.litho.sections.Section;
import com.facebook.litho.sections.SectionLifecycle;

/**
 * Allows convenient type matching comparison for instances of {@link SectionLifecycle}s. Useful for
 * verifying the existence of sub-sections that are part of a group.
 */
public class SubSection {

  public static SubSection of(Class<? extends SectionLifecycle> sectionType) {
    return new SubSection(sectionType, null);
  }

  public static SubSection of(Section section) {
    return new SubSection(section.getClass(), section);
  }

  private final Class<? extends SectionLifecycle> mSectionType;
  private final Section mSection;

  private SubSection(Class<? extends SectionLifecycle> sectionType, Section section) {
    mSectionType = sectionType;
    mSection = section;
  }

  public Section getSection() {
    return mSection;
  }

  public Class<? extends SectionLifecycle> getSectionType() {
    return mSectionType;
  }

  @Override
  public boolean equals(Object o) {

    if (!(o instanceof SubSection)) {
      return false;
    }

    SubSection that = (SubSection) o;
    return that.mSectionType.equals(mSectionType) && arePropsEqual(that.mSection, mSection);
  }

  @Override
  public int hashCode() {
    return mSectionType.hashCode();
  }

  @Override
  public String toString() {
    return mSectionType.toString() + " [" + super.toString() + "]";
  }

  /**
   * For testing purposes, props are only compared if both subSections supply them. Otherwise, just
   * ignore them.
   */
  private static boolean arePropsEqual(Section thatSection, Section thisSection) {
    return thatSection == null || thisSection == null || thatSection.isEquivalentTo(thisSection);
  }
}
