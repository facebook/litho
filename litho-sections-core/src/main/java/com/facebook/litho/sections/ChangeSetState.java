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

import static com.facebook.litho.FrameworkLogEvents.EVENT_SECTIONS_GENERATE_CHANGESET;
import static com.facebook.litho.FrameworkLogEvents.PARAM_CHANGESET_CHANGE_COUNT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_CHANGESET_DELETE_RANGE_COUNT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_CHANGESET_DELETE_SINGLE_COUNT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_CHANGESET_EFFECTIVE_COUNT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_CHANGESET_FINAL_COUNT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_CHANGESET_INSERT_RANGE_COUNT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_CHANGESET_INSERT_SINGLE_COUNT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_CHANGESET_MOVE_COUNT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_CHANGESET_UPDATE_RANGE_COUNT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_CHANGESET_UPDATE_SINGLE_COUNT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_CURRENT_ROOT_COUNT;
import static com.facebook.litho.sections.Section.acquireChildrenMap;
import static com.facebook.litho.sections.Section.releaseChildrenMap;

import android.util.SparseArray;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import com.facebook.litho.ComponentsLogger;
import com.facebook.litho.ComponentsSystrace;
import com.facebook.litho.PerfEvent;
import com.facebook.litho.sections.logger.SectionsDebugLogger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ChangeSetState is responsible to generate a global ChangeSet between two {@link Section}s trees.
 */
public class ChangeSetState {

  private static final List<Section> sEmptyList = new ArrayList<>();

  private Section mCurrentRoot;
  private Section mNewRoot;
  private ChangeSet mChangeSet;
  private List<Section> mRemovedComponents;

  private ChangeSetState() {
    mRemovedComponents = new ArrayList<>();
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
      Section newRoot,
      SectionsDebugLogger sectionsDebugLogger,
      String sectionTreeTag,
      String currentPrefix,
      String nextPrefix,
      boolean enableStats) {
    ChangeSetState changeSetState = acquireChangeSetState();
    changeSetState.mCurrentRoot = currentRoot;
    changeSetState.mNewRoot = newRoot;

    final ComponentsLogger logger = sectionContext.getLogger();
    final PerfEvent logEvent =
        SectionsLogEventUtils.getSectionsPerformanceEvent(
            sectionContext, EVENT_SECTIONS_GENERATE_CHANGESET, currentRoot, newRoot);

    if (currentRoot != null
        && newRoot != null
        && !currentRoot.getSimpleName().equals(newRoot.getSimpleName())) {
      ChangeSet remove =
          generateChangeSetRecursive(
              sectionContext,
              currentRoot,
              null,
              changeSetState.mRemovedComponents,
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
              changeSetState.mRemovedComponents,
              sectionsDebugLogger,
              sectionTreeTag,
              currentPrefix,
              nextPrefix,
              Thread.currentThread().getName(),
              enableStats);
      changeSetState.mChangeSet = ChangeSet.merge(remove, add);
    } else {
      changeSetState.mChangeSet =
          generateChangeSetRecursive(
              sectionContext,
              currentRoot,
              newRoot,
              changeSetState.mRemovedComponents,
              sectionsDebugLogger,
              sectionTreeTag,
              currentPrefix,
              nextPrefix,
              Thread.currentThread().getName(),
              enableStats);
    }

    if (logger != null && logEvent != null) {
      logEvent.markerAnnotate(
          PARAM_CURRENT_ROOT_COUNT, currentRoot == null ? -1 : currentRoot.getCount());
      logEvent.markerAnnotate(
          PARAM_CHANGESET_CHANGE_COUNT, changeSetState.mChangeSet.getChangeCount());
      logEvent.markerAnnotate(PARAM_CHANGESET_FINAL_COUNT, changeSetState.mChangeSet.getCount());

      final ChangeSet.ChangeSetStats changeSetStats = changeSetState.mChangeSet.getChangeSetStats();
      if (changeSetStats != null) {
        logEvent.markerAnnotate(
            PARAM_CHANGESET_EFFECTIVE_COUNT, changeSetStats.getEffectiveChangesCount());
        logEvent.markerAnnotate(
            PARAM_CHANGESET_INSERT_SINGLE_COUNT, changeSetStats.getInsertSingleCount());
        logEvent.markerAnnotate(
            PARAM_CHANGESET_INSERT_RANGE_COUNT, changeSetStats.getInsertRangeCount());
        logEvent.markerAnnotate(
            PARAM_CHANGESET_DELETE_SINGLE_COUNT, changeSetStats.getDeleteSingleCount());
        logEvent.markerAnnotate(
            PARAM_CHANGESET_DELETE_RANGE_COUNT, changeSetStats.getDeleteRangeCount());
        logEvent.markerAnnotate(
            PARAM_CHANGESET_UPDATE_SINGLE_COUNT, changeSetStats.getUpdateSingleCount());
        logEvent.markerAnnotate(
            PARAM_CHANGESET_UPDATE_RANGE_COUNT, changeSetStats.getUpdateRangeCount());
        logEvent.markerAnnotate(PARAM_CHANGESET_MOVE_COUNT, changeSetStats.getMoveCount());
      }

      logger.logPerfEvent(logEvent);
    }

    checkCount(currentRoot, newRoot, changeSetState);

    return changeSetState;
  }

  private static ChangeSet generateChangeSetRecursive(
      SectionContext sectionContext,
      Section currentRoot,
      Section newRoot,
      List<Section> removedComponents,
      SectionsDebugLogger sectionsDebugLogger,
      String sectionTreeTag,
      String currentPrefix,
      String newPrefix,
      String thread,
      boolean enableStats) {

    boolean currentRootIsNull = currentRoot == null;
    boolean newRootIsNull = newRoot == null;

    if (currentRootIsNull && newRootIsNull) {
      throw new IllegalStateException("Both currentRoot and newRoot are null.");
    }

    if (newRootIsNull) {
      // The new tree doesn't have this component. We only need to remove all its children from
      // the list.
      final int currentItemsCount = currentRoot.getCount();
      removedComponents.add(currentRoot);
      final ChangeSet changeSet =
          ChangeSet.acquireChangeSet(currentRoot.getCount(), newRoot, enableStats);

      for (int i = 0; i < currentItemsCount; i++) {
        changeSet.addChange(Change.remove(0));
      }

      return changeSet;
    }

    final SectionLifecycle lifecycle = newRoot;
    final String updateCurrentPrefix = updatePrefix(currentRoot, currentPrefix);
    final String updateNewPrefix = updatePrefix(newRoot, newPrefix);

    // Components both exist and don't need to update.
    if (!currentRootIsNull && !lifecycle.shouldComponentUpdate(currentRoot, newRoot)) {
      final ChangeSet changeSet =
          ChangeSet.acquireChangeSet(currentRoot.getCount(), newRoot, enableStats);
      newRoot.setCount(changeSet.getCount());
      sectionsDebugLogger.logShouldUpdate(
          sectionTreeTag,
          currentRoot,
          newRoot,
          updateCurrentPrefix,
          updateNewPrefix,
          false,
          thread);
      return changeSet;
    }

    sectionsDebugLogger.logShouldUpdate(
        sectionTreeTag, currentRoot, newRoot, updateCurrentPrefix, updateNewPrefix, true, thread);

    // Component(s) can generate changeSets and will generate the changeset.
    // Add the startCount to the changeSet.
    if (lifecycle.isDiffSectionSpec()) {
      final boolean isTracing = ComponentsSystrace.isTracing();
      if (isTracing) {
        ComponentsSystrace.beginSectionWithArgs("generateChangeSet")
            .arg("current_root", currentRootIsNull ? "<null>" : currentRoot.getKey())
            .arg("update_prefix", updateCurrentPrefix)
            .flush();
      }

      final ChangeSet changeSet =
          ChangeSet.acquireChangeSet(
              currentRootIsNull ? 0 : currentRoot.getCount(), newRoot, enableStats);
      lifecycle.generateChangeSet(newRoot.getScopedContext(), changeSet, currentRoot, newRoot);
      newRoot.setCount(changeSet.getCount());

      if (isTracing) {
        ComponentsSystrace.endSection();
      }

      return changeSet;
    }

    ChangeSet resultChangeSet = ChangeSet.acquireChangeSet(newRoot, enableStats);

    final Map<String, Pair<Section, Integer>> currentChildren = acquireChildrenMap(currentRoot);
    final Map<String, Pair<Section, Integer>> newChildren = acquireChildrenMap(newRoot);

    List<Section> currentChildrenList;
    if (currentRoot == null) {
      currentChildrenList = sEmptyList;
    } else {
      currentChildrenList = new ArrayList<>(currentRoot.getChildren());
    }

    final List<Section> newChildrenList = newRoot.getChildren();

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

            if (valueAndIndex.second != j) {
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

      if (changeSet != null) {
        changeSet.release();
      }
    }

    releaseChangeSetSparseArray(changeSets);
    newRoot.setCount(resultChangeSet.getCount());

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
    final SparseArray<ChangeSet> changeSets = acquireChangeSetSparseArray();

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

        if (currentChangeSet != null) {
          currentChangeSet.release();
        }

        changeSet.release();
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

        if (currentChangeSet != null) {
          currentChangeSet.release();
        }

        changeSet.release();
      }
    }

    releaseChildrenMap(currentChildren);
    releaseChildrenMap(newChildren);

    return changeSets;
  }

  private static SparseArray<ChangeSet> acquireChangeSetSparseArray() {
    // TODO use pools instead t11953296
    return new SparseArray<>();
  }

  private static void releaseChangeSetSparseArray(SparseArray<ChangeSet> changeSets) {
    // TODO use pools t11953296
  }

  private static ChangeSetState acquireChangeSetState() {
    // TODO use pools t11953296
    return new ChangeSetState();
  }

  private static final int getPreviousChildrenCount(List<Section> sections, String key) {
    int count = 0;
    for (Section s : sections) {
      if (s.getGlobalKey().equals(key)) {
        return count;
      }

      count += s.getCount();
    }

    return count;
  }

  private static final String updatePrefix(Section root, String prefix) {
    if (root != null && root.getParent() == null) {
      return root.getClass().getSimpleName();
    } else if (root != null) {
      return prefix + "->" + root.getClass().getSimpleName();
    }
    return "";
  }

  private static void checkCount(
      Section currentRoot, Section newRoot, ChangeSetState changeSetState) {
    final boolean hasNegativeCount =
        (currentRoot != null && currentRoot.getCount() < 0)
            || (newRoot != null && newRoot.getCount() < 0);

    if (!hasNegativeCount) {
      return;
    }

    final StringBuilder message = new StringBuilder();
    message.append("Changet count is below 0! ");

    message.append("Current section: ");
    if (currentRoot == null) {
      message.append("null; ");
    } else {
      message.append(
          currentRoot.getSimpleName()
              + " , key="
              + currentRoot.getGlobalKey()
              + ", count="
              + currentRoot.getCount()
              + ", childrenSize="
              + currentRoot.getChildren().size()
              + "; ");
    }

    message.append("Next section: ");
    if (newRoot == null) {
      message.append("null; ");
    } else {
      message.append(
          newRoot.getSimpleName()
              + " , key="
              + newRoot.getGlobalKey()
              + ", count="
              + newRoot.getCount()
              + ", childrenSize="
              + newRoot.getChildren().size()
              + "; ");
    }

    message.append("Changes: [");

    final ChangeSet changeSet = changeSetState.mChangeSet;
    for (int i = 0; i < changeSet.getCount(); i++) {
      final Change change = changeSet.getChangeAt(i);
      message.append(change.getType() + " " + change.getIndex() + " " + change.getToIndex());
      message.append(", ");
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

  /** @return the {@link Section} that was used as current root for this ChangeSet computation. */
  Section getCurrentRoot() {
    return mCurrentRoot;
  }

  /** @return the {@link Section} that was used as new root for this ChangeSet computation. */
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

  void release() {
    mRemovedComponents.clear();
    // TODO use pools t11953296
  }
}
