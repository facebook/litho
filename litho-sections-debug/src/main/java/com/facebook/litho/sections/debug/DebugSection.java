/*
 * Copyright 2019-present Facebook, Inc.
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

package com.facebook.litho.sections.debug;

import android.support.annotation.Nullable;
import android.view.View;
import com.facebook.litho.sections.Section;
import com.facebook.litho.sections.debug.widget.RenderInfoDebugInfoRegistry;
import java.util.ArrayList;
import java.util.List;

/**
 * A DebugSection represents a section node that created part of Litho's component hierarchy. The
 * purpose of this class is for tools such as Stetho's UI inspector to be able to insert Section
 * information when visualising a component hierarchy.
 */
public final class DebugSection {

  private final List<View> mViews;
  private Section mSectionDebugNode;

  private DebugSection(Section section, List<View> views) {
    mViews = views;
    mSectionDebugNode = section;
  }

  /** The root represents a DebugSection with information about the root of the SectionTree. */
  public static @Nullable DebugSection getRootInstance(List<View> lithoViews) {
    if (lithoViews == null || lithoViews.isEmpty()) {
      return null;
    }

    // We can create the root DebugSection from any of the Sections provided by the Litho views.
    final Section renderInfoSection =
        (Section) RenderInfoDebugInfoRegistry.getRenderInfoSectionDebugInfo(lithoViews.get(0));

    return getRootDebugSection(renderInfoSection, lithoViews);
  }

  /** Creates a DebugSection that holds the root Section of the SectionTree. */
  private static DebugSection getRootDebugSection(Section section, List<View> views) {
    while (section.getParent() != null) {
      section = section.getParent();
    }

    return new DebugSection(section, views);
  }

  /**
   * Children of a GroupSectionSpec section will be other DebugSections. Children of a
   * DiffSectionSpec will be views.
   */
  public List<?> getSectionChildren() {
    if (mSectionDebugNode.isDiffSectionSpec()) {
      final List<View> childViews = new ArrayList<>();

      for (int i = 0; i < mViews.size(); i++) {
        final View childView = mViews.get(i);
        final Section renderInfoSection =
            (Section) RenderInfoDebugInfoRegistry.getRenderInfoSectionDebugInfo(childView);

        if (renderInfoSection != null
            && renderInfoSection.getGlobalKey().equals(mSectionDebugNode.getGlobalKey())) {
          childViews.add(childView);
        }
      }

      return childViews;
    } else {
      final List<DebugSection> childrenDebugSections = new ArrayList<>();
      for (Section child : mSectionDebugNode.getChildren()) {
        childrenDebugSections.add(new DebugSection(child, mViews));
      }

      return childrenDebugSections;
    }
  }

  public String getGlobalKey() {
    return mSectionDebugNode.getGlobalKey();
  }

  public String getName() {
    return mSectionDebugNode.getSimpleName();
  }
}
