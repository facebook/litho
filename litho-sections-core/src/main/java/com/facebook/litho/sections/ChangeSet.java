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

package com.facebook.litho.sections;

import static com.facebook.infer.annotation.ThreadConfined.ANY;
import static com.facebook.litho.sections.Change.DELETE;
import static com.facebook.litho.sections.Change.DELETE_RANGE;
import static com.facebook.litho.sections.Change.INSERT;
import static com.facebook.litho.sections.Change.INSERT_RANGE;
import static com.facebook.litho.sections.Change.MOVE;
import static com.facebook.litho.sections.Change.UPDATE;
import static com.facebook.litho.sections.Change.UPDATE_RANGE;

import android.support.annotation.VisibleForTesting;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.TreeProps;
import com.facebook.litho.sections.SectionTree.Target;
import com.facebook.litho.sections.annotations.DiffSectionSpec;
import com.facebook.litho.sections.annotations.OnDiff;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.litho.widget.TreePropsWrappedRenderInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * A ChangeSet represent a list of Change that has to be applied to a {@link Target} as the result
 * of an update of a {@link Section}. A ChangeSet is provided in the {@link OnDiff} of a {@link
 * DiffSectionSpec} to allow the ChangeSetSpec to define its changes based on old/new props and
 * state.
 */
@ThreadConfined(ANY)
public final class ChangeSet {

  private final List<Change> mChanges;
  private int mFinalCount;
  private Section mSection;

  private ChangeSet() {
    mChanges = new ArrayList<>();
    mFinalCount = 0;
  }

  /**
   * @return the {@link Change} at index.
   */
  public Change getChangeAt(int index) {
    return mChanges.get(index);
  }

  /**
   * @return the number of {@link Change}s in this ChangeSet.
   */
  public int getChangeCount() {
    return mChanges.size();
  }

  /**
   * Add a new Change to this ChangeSet. This is what a {@link DiffSectionSpec} would call in its
   * {@link OnDiff} method to append a {@link Change}.
   */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public void addChange(Change change) {
    int changeDelta = 0;
    switch (change.getType()) {
      case INSERT:
        changeDelta = 1;
        break;
      case INSERT_RANGE:
        changeDelta = change.getCount();
        break;
      case DELETE:
        changeDelta = -1;
        break;
      case DELETE_RANGE:
        changeDelta = -change.getCount();
        break;
      case UPDATE:
      case UPDATE_RANGE:
      case MOVE:
      default:
        break;
    }

    mFinalCount += changeDelta;
    mChanges.add(change);
  }

  public void insert(int index, RenderInfo renderInfo, TreeProps treeProps) {
    // Null check for tests only. This should never be the case otherwise.
    if (mSection != null) {
      renderInfo.addDebugInfo(SectionsDebugParams.SECTION_GLOBAL_KEY, mSection.getGlobalKey());
    }
    addChange(Change.insert(index, new TreePropsWrappedRenderInfo(renderInfo, treeProps)));
  }

  public void insertRange(int index, int count, List<RenderInfo> renderInfos, TreeProps treeProps) {
    // Null check for tests only. This should never be the case otherwise.
    if (mSection != null) {
      for (int i = 0, size = renderInfos.size(); i < size; i++) {
        renderInfos
            .get(i)
            .addDebugInfo(SectionsDebugParams.SECTION_GLOBAL_KEY, mSection.getGlobalKey());
      }
    }
    addChange(Change.insertRange(index, count, wrapTreePropRenderInfos(renderInfos, treeProps)));
  }

  public void update(int index, RenderInfo renderInfo, TreeProps treeProps) {
    addChange(Change.update(index, new TreePropsWrappedRenderInfo(renderInfo, treeProps)));
  }

  public void updateRange(int index, int count, List<RenderInfo> renderInfos, TreeProps treeProps) {
    addChange(Change.updateRange(index, count, wrapTreePropRenderInfos(renderInfos, treeProps)));
  }

  public void delete(int index) {
    addChange(Change.remove(index));
  }

  public void deleteRange(int index, int count) {
    addChange(Change.removeRange(index, count));
  }

  public void move(int fromIndex, int toIndex) {
    addChange(Change.move(fromIndex, toIndex));
  }

  /**
   * @return the total number of items in the {@link Target}
   * after this ChangeSet will be applied.
   */
  int getCount() {
    return mFinalCount;
  }

  /** @return an empty ChangeSet. */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public static ChangeSet acquireChangeSet(Section section) {
    return acquireChangeSet(0, section);
  }

  /** @return an empty ChangeSet starting from count startCount. */
  static ChangeSet acquireChangeSet(int startCount, Section section) {
    final ChangeSet changeSet = acquire();
    changeSet.mFinalCount = startCount;
    changeSet.mSection = section;

    return changeSet;
  }

  /** Wrap the given list of {@link RenderInfo} in a {@link TreePropsWrappedRenderInfo}. */
  private static List<RenderInfo> wrapTreePropRenderInfos(
      List<RenderInfo> renderInfos, TreeProps treeProps) {
    final List<RenderInfo> wrappedRenderInfos = new ArrayList<>(renderInfos.size());
    for (int i = 0; i < renderInfos.size(); i++) {
      wrappedRenderInfos.add(new TreePropsWrappedRenderInfo(renderInfos.get(i), treeProps));
    }

    return wrappedRenderInfos;
  }

  /**
   * Used internally by the framework to merge all the ChangeSet generated by all the leaf {@link
   * Section}. The merged ChangeSet will be passed to the {@link Target}.
   */
  static ChangeSet merge(ChangeSet first, ChangeSet second) {
    final ChangeSet mergedChangeSet = acquireChangeSet(null);
    final int firstCount = first != null ? first.mFinalCount : 0;
    final int secondCount = second != null ? second.mFinalCount : 0;

    final List<Change> mergedChanged = mergedChangeSet.mChanges;

    if (first != null) {
      for (Change change : first.mChanges) {
        mergedChanged.add(Change.copy(change));
      }
    }

    if (second != null) {
      for (Change change : second.mChanges) {
        mergedChanged.add(Change.offset(change, firstCount));
      }
    }

    mergedChangeSet.mFinalCount = firstCount + secondCount;

    return mergedChangeSet;
  }

  //TODO implement pools t11953296
  private static ChangeSet acquire() {
    return new ChangeSet();
  }

  //TODO implement pools t11953296
  void release() {
    for (Change change : mChanges) {
      change.release();
    }

    mChanges.clear();
    mFinalCount = 0;
  }
}
