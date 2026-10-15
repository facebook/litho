/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import static com.facebook.litho.sections.Section.acquireChildrenMap;

import android.util.Pair;
import android.util.SparseArray;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.ComponentsSystrace;
import com.facebook.litho.sections.logger.SectionsDebugLogger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ChangeSetState is responsible to generate a global ChangeSet between two {@link Section}s trees.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class ChangeSetState {

  private static final List<Section> sEmptyList = new ArrayList<>();

  private final @Nullable Section mCurrentRoot;
  private final @Nullable Section mNewRoot;
  private final ChangeSet mChangeSet;
  private final List<Section> mRemovedComponents;

  private ChangeSetState(
      @Nullable Section currentRoot,
      @Nullable Section newRoot,
      ChangeSet changeSet,
      List<Section> removedComponents) {
    mCurrentRoot = currentRoot;
    mNewRoot = newRoot;
    mChangeSet = changeSet;
    mRemovedComponents = removedComponents;
  }

  /**
   * Calculate the {@link ChangeSet} for this ChangeSetState. The returned ChangeSet will be the
   * result of merging all the changeSets for all the leafs of the tree. As a result of calculating
   * the {@link ChangeSet} all the nodes in the new tree will be populated with the number of items
   * in their subtree.
   */
  static ChangeSetState generateChangeSet(
      SectionContext sectionContext,
      @Nullable Section currentRoot,
      @Nullable Section newRoot,
      SectionsDebugLogger sectionsDebugLogger,
      String sectionTreeTag,
      String currentPrefix,
      String nextPrefix,
      boolean enableStats) {
    final ArrayList<Section> removedComponents = new ArrayList<>();
    final ChangeSet changeSet;
    if (currentRoot != null
        && newRoot != null
        && !currentRoot.getSimpleName().equals(newRoot.getSimpleName())) {
      ChangeSet remove =
          generateChangeSetRecursive(
              sectionContext,
              currentRoot,
              null,
              removedComponents,
              sectionsDebugLogger,
              sectionTreeTag,
              currentPrefix,
              nextPrefix,
              Thread.currentThread().getName(),
              enableStats);

      ChangeSet add =
          generateChangeSetRecursive(
              sectionContext,
              null,
              newRoot,
              removedComponents,
              sectionsDebugLogger,
              sectionTreeTag,
              currentPrefix,
              nextPrefix,
              Thread.currentThread().getName(),
              enableStats);
      changeSet = ChangeSet.merge(remove, add);
    } else {
      changeSet =
          generateChangeSetRecursive(
              sectionContext,
              currentRoot,
              newRoot,
              removedComponents,
              sectionsDebugLogger,
              sectionTreeTag,
              currentPrefix,
              nextPrefix,
              Thread.currentThread().getName(),
              enableStats);
    }

    final ChangeSetState changeSetState =
        new ChangeSetState(currentRoot, newRoot, changeSet, removedComponents);
    checkCount(currentRoot, newRoot, changeSetState);
    return changeSetState;
  }

  private static ChangeSet generateChangeSetRecursive(
      SectionContext sectionContext,
      @Nullable Section currentRoot,
      @Nullable Section newRoot,
      List<Section> removedComponents,
      SectionsDebugLogger sectionsDebugLogger,
      String sectionTreeTag,
      String currentPrefix,
      String newPrefix,
      String thread,
      boolean enableStats) {

    if (currentRoot == null && newRoot == null) {
      throw new IllegalStateException("Both currentRoot and newRoot are null.");
    }

    if (newRoot == null) {
      if (currentRoot == null) {
        throw new IllegalStateException("currentRoot is null when newRoot is null.");
      }

      final Section currentRootSection = currentRoot;
      // The new tree doesn't have this component. We only need to remove all its children from
      // the list.
      final int currentItemsCount = currentRootSection.getCount();
      removedComponents.add(currentRootSection);
      final ChangeSet changeSet =
          ChangeSet.acquireChangeSet(currentRootSection.getCount(), newRoot, enableStats);

      for (int i = 0; i < currentItemsCount; i++) {
        changeSet.addChange(Change.remove(0));
      }

      return changeSet;
    }

    final Section newRootSection = newRoot;
    final SectionLifecycle lifecycle = newRootSection;
    final String updateCurrentPrefix = updatePrefix(currentRoot, currentPrefix);
    final String updateNewPrefix = updatePrefix(newRootSection, newPrefix);

    // Check the count of children to prevent from developers updating section based on
    // some variables other than props and state, which could lead us to generate a bad diff
    // later because a section is considered unchanged if the old one and the new one have
    // the same props and state.
    final boolean isChildrenOfSectionConsistent =
        currentRoot != null && (currentRoot.getCount() == newRootSection.getCount());
    // Components both exist and don't need to update.
    if (isChildrenOfSectionConsistent
        && currentRoot != null
        && !lifecycle.shouldComponentUpdate(currentRoot, newRootSection)) {
      final ChangeSet changeSet =
          ChangeSet.acquireChangeSet(currentRoot.getCount(), newRootSection, enableStats);
      newRootSection.setCount(changeSet.getCount());
      sectionsDebugLogger.logShouldUpdate(
          sectionTreeTag,
          currentRoot,
          newRootSection,
          updateCurrentPrefix,
          updateNewPrefix,
          false,
          thread);
      return changeSet;
    }

    sectionsDebugLogger.logShouldUpdate(
        sectionTreeTag,
        // NULLSAFE_FIXME[Parameter Not Nullable]
        currentRoot,
        newRootSection,
        updateCurrentPrefix,
        updateNewPrefix,
        true,
        thread);

    // Component(s) can generate changeSets and will generate the changeset.
    // Add the startCount to the changeSet.
    if (lifecycle.isDiffSectionSpec()) {
      final boolean isTracing = ComponentsSystrace.isTracing();
      if (isTracing) {
        final String currentRootKey = currentRoot == null ? "<null>" : currentRoot.getKey();
        ComponentsSystrace.beginSectionWithArgs("sections.generateChangeSet")
            .arg("current_root", currentRootKey)
            .arg("update_prefix", updateCurrentPrefix)
            .flush();
      }

      final ChangeSet changeSet =
          ChangeSet.acquireChangeSet(
              currentRoot == null ? 0 : currentRoot.getCount(), newRootSection, enableStats);
      final SectionContext newRootScopedContext = newRootSection.getScopedContext();
      final SectionContext currentRootScopedContext =
          currentRoot == null ? null : currentRoot.getScopedContext();
      lifecycle.generateChangeSet(
          newRootScopedContext,
          changeSet,
          // NULLSAFE_FIXME[Parameter Not Nullable]
          currentRootScopedContext,
          // NULLSAFE_FIXME[Parameter Not Nullable]
          currentRoot,
          newRootScopedContext,
          newRootSection);
      newRootSection.setCount(changeSet.getCount());

      if (isTracing) {
        ComponentsSystrace.endSection();
      }

      return changeSet;
    }

    ChangeSet resultChangeSet = ChangeSet.acquireChangeSet(newRootSection, enableStats);

    final Map<String, Pair<Section, Integer>> currentChildren = acquireChildrenMap(currentRoot);
    final Map<String, Pair<Section, Integer>> newChildren = acquireChildrenMap(newRootSection);

    final List<Section> currentChildrenList;
    if (currentRoot == null || currentRoot.getChildren() == null) {
      currentChildrenList = sEmptyList;
    } else {
      currentChildrenList = new ArrayList<>(currentRoot.getChildren());
    }

    final List<Section> newChildrenList;
    final List<Section> newRootChildren = newRootSection.getChildren();
    if (newRootChildren == null) {
      newChildrenList = sEmptyList;
    } else {
      newChildrenList = newRootChildren;
    }

    // Determine Move Changes.
    // Index of a section that was detected as moved.
    // Components that have swapped order with this one in the new list will be moved.
    int sectionToSwapIndex = -1;
    int swapToIndex = -1;

    for (int i = 0; i < newChildrenList.size(); i++) {
      final String key = newChildrenList.get(i).getGlobalKey();

      if (currentChildren.containsKey(key)) {
        final Pair<Section, Integer> valueAndPosition = currentChildren.get(key);
        final Section current = valueAndPosition.first;
        final int currentIndex = valueAndPosition.second;

        // We found something that swapped order with the moved section.
        if (sectionToSwapIndex > currentIndex) {

          for (int c = 0; c < current.getCount(); c++) {
            resultChangeSet.addChange(
                Change.move(getPreviousChildrenCount(currentChildrenList, key), swapToIndex));
          }

          // Place this section in the correct order in the current children list.
          currentChildrenList.remove(currentIndex);
          currentChildrenList.add(sectionToSwapIndex, current);
          for (int j = 0, size = currentChildrenList.size(); j < size; j++) {
            final Section section = currentChildrenList.get(j);
            final Pair<Section, Integer> valueAndIndex =
                currentChildren.get(section.getGlobalKey());

            if (valueAndIndex != null && valueAndIndex.second != j) {
              currentChildren.put(section.getGlobalKey(), new Pair<>(valueAndIndex.first, j));
            }
          }
        } else if (currentIndex > sectionToSwapIndex) { // We found something that was moved.
          sectionToSwapIndex = currentIndex;
          swapToIndex =
              getPreviousChildrenCount(currentChildrenList, key)
                  + currentChildrenList.get(sectionToSwapIndex).getCount()
                  - 1;
        }
      }
    }

    final SparseArray<ChangeSet> changeSets =
        generateChildrenChangeSets(
            sectionContext,
            currentChildren,
            newChildren,
            currentChildrenList,
            newChildrenList,
            removedComponents,
            sectionsDebugLogger,
            sectionTreeTag,
            updateCurrentPrefix,
            updateNewPrefix,
            thread,
            enableStats);

    for (int i = 0, size = changeSets.size(); i < size; i++) {
      ChangeSet changeSet = changeSets.valueAt(i);
      resultChangeSet = ChangeSet.merge(resultChangeSet, changeSet);
    }

    newRootSection.setCount(resultChangeSet.getCount());

    return resultChangeSet;
  }

  /**
   * Generates a list of {@link ChangeSet} for the children of newRoot and currentRoot. The
   * generated SparseArray will contain an element for each children of currentRoot. {@link
   * ChangeSet}s for new items in newRoot will me merged in place with the appropriate {@link
   * ChangeSet}. If for example a new child is added in position 2, its {@link ChangeSet} will be
   * merged with the {@link ChangeSet} generated for the child of currentRoot in position 1. This
   * still guarantees a correct ordering while preserving the validity of indexes in the children of
   * currentRoot. Re-ordering a child is not supported and will trigger an {@link
   * IllegalStateException}.
   */
  private static SparseArray<ChangeSet> generateChildrenChangeSets(
      SectionContext sectionContext,
      Map<String, Pair<Section, Integer>> currentChildren,
      Map<String, Pair<Section, Integer>> newChildren,
      List<Section> currentChildrenList,
      List<Section> newChildrenList,
      List<Section> removedComponents,
      SectionsDebugLogger sectionsDebugLogger,
      String sectionTreeTag,
      String currentPrefix,
      String newPrefix,
      String thread,
      boolean enableStats) {
    final SparseArray<ChangeSet> changeSets = new SparseArray<>();

    // Find removed current children.
    for (int i = 0; i < currentChildrenList.size(); i++) {
      final String key = currentChildrenList.get(i).getGlobalKey();
      final Section currentChild = currentChildrenList.get(i);

      if (newChildren.get(key) == null) {
        changeSets.put(
            i,
            generateChangeSetRecursive(
                sectionContext,
                currentChild,
                null,
                removedComponents,
                sectionsDebugLogger,
                sectionTreeTag,
                currentPrefix,
                newPrefix,
                thread,
                enableStats));
      }
    }

    int activeChildIndex = 0;
    for (int i = 0; i < newChildrenList.size(); i++) {
      final Section newChild = newChildrenList.get(i);
      final Pair<Section, Integer> valueAndPosition = currentChildren.get(newChild.getGlobalKey());
      final int currentChildIndex = valueAndPosition != null ? valueAndPosition.second : -1;

      // New child was added.
      if (currentChildIndex < 0) {
        final ChangeSet currentChangeSet = changeSets.get(activeChildIndex);
        final ChangeSet changeSet =
            generateChangeSetRecursive(
                sectionContext,
                null,
                newChild,
                removedComponents,
                sectionsDebugLogger,
                sectionTreeTag,
                currentPrefix,
                newPrefix,
                thread,
                enableStats);

        changeSets.put(activeChildIndex, ChangeSet.merge(currentChangeSet, changeSet));
      } else {
        activeChildIndex = currentChildIndex;

        final ChangeSet currentChangeSet = changeSets.get(activeChildIndex);
        final ChangeSet changeSet =
            generateChangeSetRecursive(
                sectionContext,
                currentChildrenList.get(currentChildIndex),
                newChild,
                removedComponents,
                sectionsDebugLogger,
                sectionTreeTag,
                currentPrefix,
                newPrefix,
                thread,
                enableStats);

        changeSets.put(activeChildIndex, ChangeSet.merge(currentChangeSet, changeSet));
      }
    }

    return changeSets;
  }

  private static int getPreviousChildrenCount(List<Section> sections, String key) {
    int count = 0;
    for (Section s : sections) {
      if (s.getGlobalKey().equals(key)) {
        return count;
      }

      count += s.getCount();
    }

    return count;
  }

  private static String updatePrefix(@Nullable Section root, String prefix) {
    if (root != null && root.getParent() == null) {
      return root.getClass().getSimpleName();
    } else if (root != null) {
      return prefix + "->" + root.getClass().getSimpleName();
    }
    return "";
  }

  private static void checkCount(
      @Nullable Section currentRoot, @Nullable Section newRoot, ChangeSetState changeSetState) {
    final boolean hasNegativeCount =
        (currentRoot != null && currentRoot.getCount() < 0)
            || (newRoot != null && newRoot.getCount() < 0);

    if (!hasNegativeCount) {
      return;
    }

    final StringBuilder message = new StringBuilder();
    message.append("ChangeSet count is below 0! ");

    message.append("Current section: ");
    if (currentRoot == null) {
      message.append("null; ");
    } else {
      final List<Section> currentRootChildren = currentRoot.getChildren();
      message.append(
          currentRoot.getSimpleName()
              + " , key="
              + currentRoot.getGlobalKey()
              + ", count="
              + currentRoot.getCount()
              + ", childrenSize="
              + (currentRootChildren == null ? "null" : currentRootChildren.size())
              + "; ");
    }

    message.append("Next section: ");
    if (newRoot == null) {
      message.append("null; ");
    } else {
      final List<Section> newRootChildren = newRoot.getChildren();
      message.append(
          newRoot.getSimpleName()
              + " , key="
              + newRoot.getGlobalKey()
              + ", count="
              + newRoot.getCount()
              + ", childrenSize="
              + (newRootChildren == null ? "null" : newRootChildren.size())
              + "; ");
    }

    message.append("Changes (finalCount=");
    final ChangeSet changeSet = changeSetState.mChangeSet;
    message.append(changeSet.getCount());
    message.append("): [");

    for (int i = 0; i < changeSet.getChangeCount(); i++) {
      final Change change = changeSet.getChangeAt(i);
      message.append(
          Change.changeTypeToString(change.getType())
              + " idx="
              + change.getIndex()
              + " toIdx="
              + change.getToIndex()
              + " cnt="
              + change.getCount());
      if (i < changeSet.getChangeCount() - 1) {
        message.append(", ");
      }
    }

    message.append("]");

    throw new IllegalStateException(message.toString());
  }

  /**
   * @return the ChangeSet that needs to be applied when transitioning from currentRoot to newRoot.
   */
  ChangeSet getChangeSet() {
    return mChangeSet;
  }

  /**
   * @return the {@link Section} that was used as current root for this ChangeSet computation.
   */
  @Nullable
  Section getCurrentRoot() {
    return mCurrentRoot;
  }

  /**
   * @return the {@link Section} that was used as new root for this ChangeSet computation.
   */
  @Nullable
  Section getNewRoot() {
    return mNewRoot;
  }

  /**
   * @return the {@link Section} that were removed from the tree as result of this ChangeSet
   *     computation.
   */
  List<Section> getRemovedComponents() {
    return mRemovedComponents;
  }
}
