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
import com.facebook.litho.sections.SectionLifecycleTestUtil;
import javax.annotation.Nullable;

/**
 * Test support class to easily create static Section hierarchies.
 */
public class TestSectionCreator {

  public static Section createChangeSetSection(
      int initialCount,
      String key,
      final boolean forceShouldUpdate,
      @Nullable final Change... changes) {
    return new ChangeSetSection(
        initialCount,
        key,
        forceShouldUpdate,
        changes);
  }

  public static Section createSectionComponent(
      String key,
      @Nullable Section... children) {
    return createSectionComponent(key, false, children);
  }

  public static Section createSectionComponent(
      String key,
      boolean forceShouldUpdate,
      @Nullable Section... children) {
    return new ChildrenSectionTest(0, key, forceShouldUpdate, children);
  }

  public static Section createChangeSetComponent(
      String key,
      @Nullable final Change... changes) {
    return createChangeSetSection(0, key, false, changes);
  }

  public static Section createChangeSetComponent(
      String key,
      boolean forceShouldUpdate,
      @Nullable final Change... changes) {
    return createChangeSetSection(0, key, forceShouldUpdate, changes);
  }

  /**
   * @return a Lifecycle for a non ChangeSetSpec Section that statically returns a list of
   * {@link Section}s as children.
   */
  private static class ChildrenSectionTest extends TestSection {
    private final Section[] mChildren;

    ChildrenSectionTest(
        int initialCount,
        String key,
        boolean forceShouldUpdate,
        @Nullable final Section... children) {
      super(initialCount, key, forceShouldUpdate);
      mChildren = children;
    }

    @Override
    protected Children createChildren(
        SectionContext c, Section component) {
      Children.Builder builder = Children.create();
      if (mChildren != null) {
        for (Section child : mChildren) {
          builder.child(child);
        }
      }

      return builder.build();
    }

    @Override
    protected void refresh(
        SectionContext listContext, Section section) {
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
  }

  /**
   * @return a Lifecycle for a ChangeSetSpec Section that statically populates the
   * {@link ChangeSet} with a list of {@link Change}s.
   */
  private static class ChangeSetSection extends TestSection {
    private final Change[] mChanges;

    ChangeSetSection(
        int initialCount,
        String key,
        boolean forceShouldUpdate,
        @Nullable final Change... changes) {
      super(initialCount, key, forceShouldUpdate);
      this.mChanges = changes;
    }

    @Override
    protected void generateChangeSet(
        SectionContext c, ChangeSet changeSet, Section previous, Section next) {
      if (mChanges != null) {
        for (int i = 0, size = mChanges.length; i < size; i++) {
          changeSet.addChange(mChanges[i]);
        }
      }
    }

    @Override
    protected void refresh(
        SectionContext listContext, Section section) {
      ((TestSection) section).refreshCalled = true;
    }

    @Override
    protected boolean isDiffSectionSpec() {
      return true;
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

  public static class TestSection extends Section {
    private final boolean forceShouldUpdate;

    public boolean refreshCalled;
    public int firstVisibleIndex;
    public int lastVisibleIndex;
    public int firstFullyVisibleIndex;
    public int lastFullyVisibleIndex;

    protected TestSection(
        int initialCount,
        String key,
        boolean forceShouldUpdate) {
      super();
      this.forceShouldUpdate = forceShouldUpdate;
      setCount(initialCount);
      setKey(key);
      setGlobalKey(key);
    }

    @Override
    public boolean equals(Object o) {
      if (forceShouldUpdate) {
        return false;
      }

      return o.getClass().equals(getClass())
          && ((Section) o).getGlobalKey().equals(getGlobalKey());
    }

    @Override
    public String getSimpleName() {
      return "TestSection";
    }
  }
}
