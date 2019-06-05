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

import com.facebook.litho.StateContainer;
import com.facebook.litho.sections.Change;
import com.facebook.litho.sections.ChangeSet;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.Section;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.SectionLifecycleTestUtil;
import java.util.HashSet;
import java.util.Set;
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
      super("ChildrenSectionTest", initialCount, key, forceShouldUpdate);
      mChildren = children;
    }

    @Override
    protected Children createChildren(SectionContext c) {
      Children.Builder builder = Children.create();
      if (mChildren != null) {
        for (Section child : mChildren) {
          builder.child(child);
        }
      }

      return builder.build();
    }

    @Override
    protected void refresh(SectionContext listContext) {
      refreshCalled = true;
    }

    @Override
    protected void viewportChanged(
        SectionContext listContext,
        int firstVisibleItem,
        int lastVisibleItem,
        int totalItemsCount,
        int firstFullyVisibleItem,
        int lastFullyVisibleItem) {
      firstVisibleIndex = firstVisibleItem;
      lastVisibleIndex = lastVisibleItem;
      firstFullyVisibleIndex = firstFullyVisibleItem;
      lastFullyVisibleIndex = lastFullyVisibleItem;
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
      super("ChangeSetSection", initialCount, key, forceShouldUpdate);
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
    protected void refresh(SectionContext listContext) {
      refreshCalled = true;
    }

    @Override
    public boolean isDiffSectionSpec() {
      return true;
    }

    @Override
    protected void viewportChanged(
        SectionContext listContext,
        int firstVisibleItem,
        int lastVisibleItem,
        int totalItemsCount,
        int firstFullyVisibleItem,
        int lastFullyVisibleItem) {
      firstVisibleIndex = firstVisibleItem;
      lastVisibleIndex = lastVisibleItem;
      firstFullyVisibleIndex = firstFullyVisibleItem;
      lastFullyVisibleIndex = lastFullyVisibleItem;
    }
  }

  public static void createTree(Section section, SectionContext listContext) {
    if (!SectionLifecycleTestUtil.isDiffSectionSpec(section)) {
      section.setChildren(SectionLifecycleTestUtil.createChildren(section, listContext, section));
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

    private final StateContainer stateContainer = new TestStateContainer();

    protected TestSection(
        String simpleName, int initialCount, String key, boolean forceShouldUpdate) {
      super(simpleName);
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
    public boolean isEquivalentTo(Section other) {
      return this.equals(other);
    }

    @Override
    public Section makeShallowCopy() {
      return this;
    }

    @Nullable
    @Override
    protected StateContainer getStateContainer() {
      return stateContainer;
    }
  }

  public static class TestStateContainer extends StateContainer {
    public final Set<StateUpdate> appliedStateUpdate = new HashSet<>();

    @Override
    public void applyStateUpdate(StateUpdate stateUpdate) {
      appliedStateUpdate.add(stateUpdate);
    }
  }
}
