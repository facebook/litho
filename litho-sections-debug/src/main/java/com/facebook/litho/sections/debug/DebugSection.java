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

package com.facebook.litho.sections.debug;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.Nullable;
import com.facebook.litho.StateContainer;
import com.facebook.litho.sections.Section;
import com.facebook.litho.sections.SectionDebugUtil;
import com.facebook.litho.widget.RenderInfoDebugInfoRegistry;
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
        final DebugSection debugSection = new DebugSection(child, mViews);
        if (debugSection.getSectionChildren().size() > 0) {
          childrenDebugSections.add(debugSection);
        }
      }

      return childrenDebugSections;
    }
  }

  public boolean isDiffSectionSpec() {
    return mSectionDebugNode.isDiffSectionSpec();
  }

  public String getGlobalKey() {
    return mSectionDebugNode.getGlobalKey();
  }

  public String getName() {
    return mSectionDebugNode.getSimpleName();
  }

  /**
   * @return a Rect containing the bounds of the views inside theSection represented by this node.
   */
  public Rect getBounds() {
    List<?> children = getSectionChildren();
    int count = children.size();

    if (count == 0) {
      return null;
    }

    Rect rect = new Rect();

    if (isDiffSectionSpec()) {
      View firstView = (View) children.get(0);
      View lastView = (View) children.get(count - 1);
      rect.left = firstView.getLeft();
      rect.top = firstView.getTop();
      rect.right = lastView.getRight();
      rect.bottom = lastView.getBottom();
    } else {
      DebugSection firstSection = (DebugSection) children.get(0);
      DebugSection lastSection = (DebugSection) children.get(count - 1);
      Rect first = firstSection.getBounds();
      Rect last = lastSection.getBounds();
      rect.left = first.left;
      rect.top = first.top;
      rect.right = last.right;
      rect.bottom = last.bottom;
    }

    return rect;
  }

  public Section getSection() {
    return mSectionDebugNode;
  }

  public @Nullable StateContainer getStateContainer() {
    return SectionDebugUtil.getStateContainerDebug(mSectionDebugNode);
  }
}
