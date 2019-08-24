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

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
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
  private Section mSection;

  @Nullable private ChangeSetStats mChangeSetStats;
  private int mFinalCount;

  private ChangeSet() {
    mChanges = new ArrayList<>();
  }

  /** @return the {@link Change} at index. */
  public Change getChangeAt(int index) {
    return mChanges.get(index);
  }

  /** @return the number of {@link Change}s in this ChangeSet. */
  public int getChangeCount() {
    return mChanges.size();
  }

  List<Change> getChanges() {
    return mChanges;
  }

  /**
   * Add a new Change to this ChangeSet. This is what a {@link DiffSectionSpec} would call in its
   * {@link OnDiff} method to append a {@link Change}.
   */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public void addChange(Change change) {
    mChanges.add(change);

    final int changeDelta = getChangeDelta(change);
    mFinalCount += changeDelta;

    if (mChangeSetStats != null) {
      mChangeSetStats = mChangeSetStats.merge(ChangeSetStats.fromChange(change, changeDelta));
    }
  }

  private static int getChangeDelta(Change change) {
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

    return changeDelta;
  }

  public void insert(int index, RenderInfo renderInfo, @Nullable TreeProps treeProps) {
    insert(index, renderInfo, treeProps, null);
  }

  public void insert(
      int index, RenderInfo renderInfo, @Nullable TreeProps treeProps, @Nullable Object data) {
    // Null check for tests only. This should never be the case otherwise.
    if (mSection != null) {
      renderInfo.addDebugInfo(SectionsDebugParams.SECTION_GLOBAL_KEY, mSection.getGlobalKey());
    }
    addChange(Change.insert(index, new TreePropsWrappedRenderInfo(renderInfo, treeProps), data));
  }

  public void insertRange(
      int index, int count, List<RenderInfo> renderInfos, @Nullable TreeProps treeProps) {
    insertRange(index, count, renderInfos, treeProps, null);
  }

  public void insertRange(
      int index,
      int count,
      List<RenderInfo> renderInfos,
      @Nullable TreeProps treeProps,
      @Nullable List<?> data) {
    // Null check for tests only. This should never be the case otherwise.
    if (mSection != null) {
      for (int i = 0, size = renderInfos.size(); i < size; i++) {
        renderInfos
            .get(i)
            .addDebugInfo(SectionsDebugParams.SECTION_GLOBAL_KEY, mSection.getGlobalKey());
      }
    }
    addChange(
        Change.insertRange(index, count, wrapTreePropRenderInfos(renderInfos, treeProps), data));
  }

  public void update(int index, RenderInfo renderInfo, @Nullable TreeProps treeProps) {
    update(index, renderInfo, treeProps, null, null);
  }

  public void update(
      int index,
      RenderInfo renderInfo,
      @Nullable TreeProps treeProps,
      @Nullable Object prevData,
      @Nullable Object nextData) {
    addChange(
        Change.update(
            index, new TreePropsWrappedRenderInfo(renderInfo, treeProps), prevData, nextData));
  }

  public void updateRange(
      int index, int count, List<RenderInfo> renderInfos, @Nullable TreeProps treeProps) {
    updateRange(index, count, renderInfos, treeProps, null, null);
  }

  public void updateRange(
      int index,
      int count,
      List<RenderInfo> renderInfos,
      @Nullable TreeProps treeProps,
      @Nullable List<?> prevData,
      @Nullable List<?> nextData) {
    addChange(
        Change.updateRange(
            index, count, wrapTreePropRenderInfos(renderInfos, treeProps), prevData, nextData));
  }

  public void delete(int index) {
    delete(index, null);
  }

  public void delete(int index, @Nullable Object data) {
    addChange(Change.remove(index, data));
  }

  public void deleteRange(int index, int count) {
    deleteRange(index, count, null);
  }

  public void deleteRange(int index, int count, @Nullable List<?> data) {
    addChange(Change.removeRange(index, count, data));
  }

  public void move(int fromIndex, int toIndex) {
    move(fromIndex, toIndex, null);
  }

  public void move(int fromIndex, int toIndex, @Nullable Object data) {
    addChange(Change.move(fromIndex, toIndex, data));
  }

  /**
   * @return the total number of items in the {@link Target} after this ChangeSet will be applied.
   */
  int getCount() {
    return mFinalCount;
  }

  @Nullable
  public ChangeSetStats getChangeSetStats() {
    return mChangeSetStats;
  }

  /** @return an empty ChangeSet. */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public static ChangeSet acquireChangeSet(Section section, boolean enableStats) {
    return acquireChangeSet(0, section, enableStats);
  }

  /** @return an empty ChangeSet starting from count startCount. */
  static ChangeSet acquireChangeSet(int startCount, Section section, boolean enableStats) {
    final ChangeSet changeSet = acquire();
    changeSet.mFinalCount = startCount;
    changeSet.mSection = section;
    changeSet.mChangeSetStats = enableStats ? new ChangeSetStats() : null;

    return changeSet;
  }

  /** Wrap the given list of {@link RenderInfo} in a {@link TreePropsWrappedRenderInfo}. */
  private static List<RenderInfo> wrapTreePropRenderInfos(
      List<RenderInfo> renderInfos, @Nullable TreeProps treeProps) {
    if (treeProps == null) {
      return renderInfos;
    }

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
    final ChangeSet mergedChangeSet = acquireChangeSet(null, false);
    final int firstCount = first != null ? first.mFinalCount : 0;
    final int secondCount = second != null ? second.mFinalCount : 0;

    final List<Change> mergedChanged = mergedChangeSet.mChanges;
    final ChangeSetStats firstStats = first != null ? first.getChangeSetStats() : null;
    final ChangeSetStats secondStats = second != null ? second.getChangeSetStats() : null;

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
    mergedChangeSet.mChangeSetStats = ChangeSetStats.merge(firstStats, secondStats);

    return mergedChangeSet;
  }

  // TODO implement pools t11953296
  private static ChangeSet acquire() {
    return new ChangeSet();
  }

  // TODO implement pools t11953296
  void release() {
    for (Change change : mChanges) {
      change.release();
    }

    mChanges.clear();
    mChangeSetStats = null;
    mFinalCount = 0;
  }

  /** Keep track of internal statistics useful for performance analyses. */
  static class ChangeSetStats {

    private final int mEffectiveChangesCount;

    private final int mInsertSingleCount;
    private final int mInsertRangeCount;

    private final int mDeleteSingleCount;
    private final int mDeleteRangeCount;

    private final int mUpdateSingleCount;
    private final int mUpdateRangeCount;

    private final int mMoveCount;

    ChangeSetStats(
        int effectiveChangesCount,
        int insertSingleCount,
        int insertRangeCount,
        int deleteSingleCount,
        int deleteRangeCount,
        int updateSingleCount,
        int updateRangeCount,
        int moveCount) {
      mEffectiveChangesCount = effectiveChangesCount;
      mInsertSingleCount = insertSingleCount;
      mInsertRangeCount = insertRangeCount;
      mDeleteSingleCount = deleteSingleCount;
      mDeleteRangeCount = deleteRangeCount;
      mUpdateSingleCount = updateSingleCount;
      mUpdateRangeCount = updateRangeCount;
      mMoveCount = moveCount;
    }

    ChangeSetStats() {
      mEffectiveChangesCount = 0;
      mInsertSingleCount = 0;
      mInsertRangeCount = 0;
      mDeleteSingleCount = 0;
      mDeleteRangeCount = 0;
      mUpdateSingleCount = 0;
      mUpdateRangeCount = 0;
      mMoveCount = 0;
    }

    @Nullable
    ChangeSetStats merge(@Nullable ChangeSetStats other) {
      if (other == null) {
        return null;
      }

      return new ChangeSetStats(
          other.mEffectiveChangesCount + mEffectiveChangesCount,
          other.mInsertSingleCount + mInsertSingleCount,
          other.mInsertRangeCount + mInsertRangeCount,
          other.mDeleteSingleCount + mDeleteSingleCount,
          other.mDeleteRangeCount + mDeleteRangeCount,
          other.mUpdateSingleCount + mUpdateSingleCount,
          other.mUpdateRangeCount + mUpdateRangeCount,
          other.mMoveCount + mMoveCount);
    }

    @Nullable
    static ChangeSetStats merge(@Nullable ChangeSetStats a, @Nullable ChangeSetStats b) {
      if (a == null) {
        return b;
      }

      if (b == null) {
        return a;
      }

      return a.merge(b);
    }

    static ChangeSetStats fromChange(Change change, int changeDelta) {
      int insertSingleCount = 0,
          insertRangeCount = 0,
          deleteSingleCount = 0,
          deleteRangeCount = 0,
          updateSingleCount = 0,
          updateRangeCount = 0,
          moveCount = 0;

      switch (change.getType()) {
        case INSERT:
          insertSingleCount += 1;
          break;
        case INSERT_RANGE:
          insertRangeCount += change.getCount();
          break;
        case DELETE:
          deleteSingleCount += 1;
          break;
        case DELETE_RANGE:
          deleteRangeCount += change.getCount();
          break;
        case UPDATE:
          updateSingleCount += 1;
          break;
        case UPDATE_RANGE:
          updateRangeCount += change.getCount();
          break;
        case MOVE:
          moveCount += change.getCount();
          break;
      }

      return new ChangeSetStats(
          changeDelta,
          insertSingleCount,
          insertRangeCount,
          deleteSingleCount,
          deleteRangeCount,
          updateSingleCount,
          updateRangeCount,
          moveCount);
    }

    /**
     * The effective number of changes this changeset causes. E.g. one delete + two updates + one
     * insert = 0 (delete and insert cancel each other out, updates don't effect the list size).
     */
    public int getEffectiveChangesCount() {
      return mEffectiveChangesCount;
    }

    public int getInsertSingleCount() {
      return mInsertSingleCount;
    }

    public int getInsertRangeCount() {
      return mInsertRangeCount;
    }

    public int getDeleteSingleCount() {
      return mDeleteSingleCount;
    }

    public int getDeleteRangeCount() {
      return mDeleteRangeCount;
    }

    public int getUpdateSingleCount() {
      return mUpdateSingleCount;
    }

    public int getUpdateRangeCount() {
      return mUpdateRangeCount;
    }

    public int getMoveCount() {
      return mMoveCount;
    }

    @Override
    public String toString() {
      return "ChangeSetStats{"
          + "mEffectiveChangesCount="
          + mEffectiveChangesCount
          + ", mInsertSingleCount="
          + mInsertSingleCount
          + ", mInsertRangeCount="
          + mInsertRangeCount
          + ", mDeleteSingleCount="
          + mDeleteSingleCount
          + ", mDeleteRangeCount="
          + mDeleteRangeCount
          + ", mUpdateSingleCount="
          + mUpdateSingleCount
          + ", mUpdateRangeCount="
          + mUpdateRangeCount
          + ", mMoveCount="
          + mMoveCount
          + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ChangeSetStats that = (ChangeSetStats) o;

      if (mEffectiveChangesCount != that.mEffectiveChangesCount) return false;
      if (mInsertSingleCount != that.mInsertSingleCount) return false;
      if (mInsertRangeCount != that.mInsertRangeCount) return false;
      if (mDeleteSingleCount != that.mDeleteSingleCount) return false;
      if (mDeleteRangeCount != that.mDeleteRangeCount) return false;
      if (mUpdateSingleCount != that.mUpdateSingleCount) return false;
      if (mUpdateRangeCount != that.mUpdateRangeCount) return false;
      return mMoveCount == that.mMoveCount;
    }

    @Override
    public int hashCode() {
      int result = mEffectiveChangesCount;
      result = 31 * result + mInsertSingleCount;
      result = 31 * result + mInsertRangeCount;
      result = 31 * result + mDeleteSingleCount;
      result = 31 * result + mDeleteRangeCount;
      result = 31 * result + mUpdateSingleCount;
      result = 31 * result + mUpdateRangeCount;
      result = 31 * result + mMoveCount;
      return result;
    }
  }
}
