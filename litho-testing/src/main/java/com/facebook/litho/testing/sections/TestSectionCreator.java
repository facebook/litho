/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.sections;

import com.facebook.litho.sections.Change;
import com.facebook.litho.sections.ChangeSet;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.Section;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.SectionLifecycle;
import com.facebook.litho.sections.SectionLifecycleTestUtil;
import javax.annotation.Nullable;

/**
 * Test support class to easily create static Section hierarchies.
 */
public class TestSectionCreator {

  public static Section createSection(
      int initialCount,
      String key,
      SectionLifecycle sectionLifecycle) {
    return createSection(initialCount, key, sectionLifecycle, false);
  }

  public static Section createSection(
      int initialCount,
      String key,
      SectionLifecycle sectionLifecycle,
      final boolean forceShouldUpdate) {
    final Section section = new TestSection(sectionLifecycle) {
      @Override
      public boolean equals(Object o) {
        if (forceShouldUpdate) {
          return false;
        }

        return ((Section) o).getLifecycle().equals(getLifecycle())
            && ((Section) o).getGlobalKey().equals(getGlobalKey());
      }
    };
    section.setCount(initialCount);
    section.setKey(key);
    section.setGlobalKey(key);

    return section;
  }

  public static Section createSectionComponent(
      String key,
      @Nullable Section... children) {
    return createSection(0, key, createSectionLifecycle(children));
  }

  public static Section createChangeSetComponent(
      String key,
      @Nullable final Change... changes) {
    return createSection(0, key, createChangeSetLifecycle(changes));
  }

  /**
   * @return a Lifecycle for a non ChangeSetSpec Section that statically returns a list of
   * {@link Section}s as children.
   */
  public static SectionLifecycle createSectionLifecycle(
      @Nullable final Section... children) {

    final SectionLifecycle sectionLifecycle =
        new SectionLifecycle() {
          @Override
          protected Children createChildren(SectionContext c, Section component) {
            Children.Builder builder = Children.create();
            if (children != null) {
              for (Section child : children) {
                builder.child(child);
              }
            }

            return builder.build();
          }

          @Override
          protected void refresh(SectionContext listContext, Section section) {
            ((TestSection) section).refreshCalled = true;
          }

          @Override
          protected void viewportChanged(
              SectionContext listContext,
              int firstVisibleItem,
              int lastVisibleItem,
              int totalItemsCount,
              int firstFullyVisibleItem,
              int lastFullyVisibleItem,
              Section section) {
            ((TestSection) section).firstVisibleIndex = firstVisibleItem;
            ((TestSection) section).lastVisibleIndex = lastVisibleItem;
            ((TestSection) section).firstFullyVisibleIndex = firstFullyVisibleItem;
            ((TestSection) section).lastFullyVisibleIndex = lastFullyVisibleItem;
          }
        };

    return sectionLifecycle;
  }

  /**
   * @return a Lifecycle for a ChangeSetSpec Section that statically populates the
   * {@link ChangeSet} with a list of {@link Change}s.
   */
  public static SectionLifecycle createChangeSetLifecycle(@Nullable final Change... changes) {
    final SectionLifecycle sectionLifecycle =
        new SectionLifecycle() {
          @Override
          protected void generateChangeSet(
              SectionContext c, ChangeSet changeSet, Section previous, Section next) {
            if (changes != null) {
              for (int i = 0, size = changes.length; i < size; i++) {
                changeSet.addChange(changes[i]);
              }
            }
          }

          @Override
          protected boolean isDiffSectionSpec() {
            return true;
          }

          @Override
          protected void refresh(SectionContext listContext, Section section) {
            ((TestSection) section).refreshCalled = true;
          }

          @Override
          protected void viewportChanged(
              SectionContext listContext,
              int firstVisibleItem,
              int lastVisibleItem,
              int totalItemsCount,
              int firstFullyVisibleItem,
              int lastFullyVisibleItem,
              Section section) {
            ((TestSection) section).firstVisibleIndex = firstVisibleItem;
            ((TestSection) section).lastVisibleIndex = lastVisibleItem;
            ((TestSection) section).firstFullyVisibleIndex = firstFullyVisibleItem;
            ((TestSection) section).lastFullyVisibleIndex = lastFullyVisibleItem;
          }
        };

    return sectionLifecycle;
  }

  public static void createTree(Section section, SectionContext listContext) {
    if (!SectionLifecycleTestUtil.isDiffSectionSpec(section.getLifecycle())) {
      section.setChildren(
          SectionLifecycleTestUtil.createChildren(section.getLifecycle(), listContext, section));
      for (int i = 0, size = section.getChildren().size(); i < size; i++) {
        createTree((Section) section.getChildren().get(i), listContext);
      }
    }
  }

  public static class TestSection extends Section implements Cloneable {

    public boolean refreshCalled;
    public int firstVisibleIndex;
    public int lastVisibleIndex;
    public int firstFullyVisibleIndex;
    public int lastFullyVisibleIndex;

    TestSection(SectionLifecycle lifecycle) {
      super(lifecycle);
    }

    @Override
    public String getSimpleName() {
      return "TestSection";
    }
  }
}
