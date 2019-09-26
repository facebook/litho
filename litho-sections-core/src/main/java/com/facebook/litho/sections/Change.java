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

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.litho.Component;
import com.facebook.litho.sections.annotations.DiffSectionSpec;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A Change represent a single operation in a section's {@link ChangeSet}. A Change can be one of
 * Insert, Update or Delete. When creating a Change an index at which the Change will be applied has
 * to be specified. The index is local in the {@link DiffSectionSpec} coordinates. So to insert a
 * new item represented by a given Component in any give ChangeSetSpec at the top, a change would
 * have to be created with <code>
 * Change.insert(0, component);
 * </code>
 */
public final class Change {

  public static final int INSERT = 1; // INSERT(index, component)
  public static final int INSERT_RANGE = -1; // INSERT_RANGE(index, count, [components])
  public static final int UPDATE = 2; // UPDATE(index, component)
  public static final int UPDATE_RANGE = -2; // UPDATE_RANGE(index, count, [components])
  public static final int DELETE = 3; // DELETE(index)
  public static final int DELETE_RANGE = -3; // DELETE_RANGE(index, count)
  public static final int MOVE = 0; // MOVE(index, toIndex, component)

  /** Describes how a {@link Section} count will change once the Change is applied. */
  @IntDef({INSERT, UPDATE, DELETE, MOVE, INSERT_RANGE, UPDATE_RANGE, DELETE_RANGE})
  @Retention(RetentionPolicy.SOURCE)
  @interface Type {}

  private static final List<RenderInfo> EMPTY = new ArrayList<>();

  private @Type int mType;
  private int mIndex;
  private int mToIndex;
  private int mCount;
  private RenderInfo mRenderInfo;
  private List<RenderInfo> mRenderInfos;
  private @Nullable List<?> mPrevData;
  private @Nullable List<?> mNextData;

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  public Change(
      @Type int ct,
      int index,
      int toIndex,
      int count,
      @Nullable RenderInfo renderInfo,
      @Nullable List<RenderInfo> renderInfos,
      @Nullable List<?> prevData,
      @Nullable List<?> nextData) {
    mType = ct;
    mIndex = index;
    mToIndex = toIndex;
    mCount = count;
    mRenderInfo = renderInfo == null ? ComponentRenderInfo.createEmpty() : renderInfo;

    if (renderInfos == null) {
      mRenderInfos = EMPTY;
    } else {
      int size = renderInfos.size();
      mRenderInfos = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        final RenderInfo renderInfoTemp = renderInfos.get(i);
        mRenderInfos.add(
            renderInfoTemp == null ? ComponentRenderInfo.createEmpty() : renderInfoTemp);
      }
    }

    if (prevData != null) {
      mPrevData = Collections.unmodifiableList(prevData);
    }
    if (nextData != null) {
      mNextData = Collections.unmodifiableList(nextData);
    }
  }

  /**
   * @return a new Change with an index equal to its current index plus an offset. This is used
   *     internally by the framework when generating the final {@link ChangeSet} for the {@link
   *     com.facebook.litho.sections.SectionTree.Target}.
   */
  static Change offset(Change change, int offset) {
    final int toIndex = change.mToIndex >= 0 ? change.mToIndex + offset : -1;
    return acquire(
        change.mType,
        change.mIndex + offset,
        toIndex,
        change.mCount,
        change.mRenderInfo,
        change.mRenderInfos,
        change.mPrevData,
        change.mNextData);
  }

  /** @return a new Change that is a copy of a given Change. */
  static Change copy(Change change) {
    return acquire(
        change.mType,
        change.mIndex,
        change.mToIndex,
        change.mCount,
        change.mRenderInfo,
        change.mRenderInfos,
        change.mPrevData,
        change.mNextData);
  }

  static Change insert(int index, RenderInfo renderInfo) {
    return insert(index, renderInfo, null);
  }

  /**
   * Creates a Change of type INSERT. As a result of this Change the {@link Component} c will be
   * rendered at index in the context of the {@link DiffSectionSpec} creating this Change.
   */
  static Change insert(int index, RenderInfo renderInfo, @Nullable Object data) {
    return acquireSingularChange(INSERT, index, renderInfo, null, data);
  }

  static Change insertRange(int index, int count, List<RenderInfo> renderInfos) {
    return insertRange(index, count, renderInfos, null);
  }

  /**
   * Creates a Change of type INSERT_RANGE. As a result of this Change {@param count} number of
   * components from {@param renderInfos} will be inserted starting at index {@param index} in the
   * context of the {@link DiffSectionSpec} creating this Change.
   */
  static Change insertRange(
      int index, int count, List<RenderInfo> renderInfos, @Nullable List<?> nextData) {
    return acquireRangedChange(INSERT_RANGE, index, count, renderInfos, null, nextData);
  }

  static Change update(int index, RenderInfo renderInfo) {
    return update(index, renderInfo, null, null);
  }

  /**
   * Creates a Change of type UPDATE. As a result of this Change the {@link Component} c substitute
   * the current Component rendered at index in the context of the {@link DiffSectionSpec} creating
   * this Change.
   */
  static Change update(
      int index, RenderInfo renderInfo, @Nullable Object prevData, @Nullable Object nextData) {
    return acquireSingularChange(UPDATE, index, renderInfo, prevData, nextData);
  }

  static Change updateRange(int index, int count, List<RenderInfo> renderInfos) {
    return updateRange(index, count, renderInfos, null, null);
  }

  /**
   * Creates a Change of type UPDATE_RANGE. As a result of this Change {@param count} number of
   * components starting at {@param index} (in the context of the {@link DiffSectionSpec} creating
   * this change) will be replaces by components from {@param renderInfos}.
   */
  static Change updateRange(
      int index,
      int count,
      List<RenderInfo> renderInfos,
      @Nullable List<?> prevData,
      @Nullable List<?> nextData) {
    return acquireRangedChange(UPDATE_RANGE, index, count, renderInfos, prevData, nextData);
  }

  static Change remove(int index) {
    return remove(index, null);
  }

  /**
   * Creates a Change of type DELETE. As a result of this Change item at index in the context of the
   * {@link DiffSectionSpec} creating this Change will be removed.
   */
  static Change remove(int index, @Nullable Object data) {
    return acquireSingularChange(DELETE, index, ComponentRenderInfo.createEmpty(), data, null);
  }

  static Change removeRange(int index, int count) {
    return removeRange(index, count, null);
  }

  /**
   * Creates a Change of type DELETE_RANGE. As a result of this Change {@param count} items starting
   * at {@param index} in the context of the {@link DiffSectionSpec} creating this Change will be
   * removed.
   */
  static Change removeRange(int index, int count, @Nullable List<?> prevData) {
    return acquireRangedChange(DELETE_RANGE, index, count, EMPTY, prevData, null);
  }

  static Change move(int fromIndex, int toIndex) {
    return move(fromIndex, toIndex, null);
  }

  /**
   * Creates a Change of type MOVE. As a result of this Change item at fromIndex in the context of
   * the {@link DiffSectionSpec} creating this Change will be moved to toIndex.
   */
  static Change move(int fromIndex, int toIndex, @Nullable Object data) {
    return acquireMoveChange(fromIndex, toIndex, data);
  }

  /** @return the type of this Change. */
  @Type
  public int getType() {
    return mType;
  }

  /** @return the index at which this change will be applied. */
  public int getIndex() {
    return mIndex;
  }

  /**
   * @return the index to which this change will move its item. This is only valid if type is MOVE.
   */
  public int getToIndex() {
    return mToIndex;
  }

  /** @return the number of changes to be made. This is only valid if type is *_RANGE. */
  public int getCount() {
    return mCount;
  }

  /**
   * @return the Component that will render this Change (if this Change is either an INSERT or an
   *     UPDATE).
   */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public RenderInfo getRenderInfo() {
    return mRenderInfo;
  }

  public List<RenderInfo> getRenderInfos() {
    return mRenderInfos;
  }

  public @Nullable List<?> getPrevData() {
    return mPrevData;
  }

  public @Nullable List<?> getNextData() {
    return mNextData;
  }

  // TODO t11953296
  private static Change acquireMoveChange(int index, int toIndex, @Nullable Object data) {
    final List<?> singleDataList = data != null ? Collections.singletonList(data) : null;
    return acquire(MOVE, index, toIndex, 1, null, null, singleDataList, singleDataList);
  }

  // TODO t11953296
  private static Change acquireSingularChange(
      @Type int ct,
      int index,
      RenderInfo renderInfo,
      @Nullable Object prevData,
      @Nullable Object nextData) {
    return acquire(
        ct,
        index,
        -1,
        1,
        renderInfo,
        null,
        prevData != null ? Collections.singletonList(prevData) : null,
        nextData != null ? Collections.singletonList(nextData) : null);
  }

  // TODO t11953296
  private static Change acquireRangedChange(
      @Type int ct,
      int index,
      int count,
      List<RenderInfo> renderInfos,
      @Nullable List<?> prevData,
      @Nullable List<?> nextData) {
    return acquire(ct, index, -1, count, null, renderInfos, prevData, nextData);
  }

  // TODO t11953296
  private static Change acquire(
      @Type int ct,
      int index,
      int toIndex,
      int count,
      @Nullable RenderInfo renderInfo,
      @Nullable List<RenderInfo> renderInfos,
      @Nullable List<?> prevData,
      @Nullable List<?> nextData) {
    return new Change(ct, index, toIndex, count, renderInfo, renderInfos, prevData, nextData);
  }

  // TODO t11953296
  void release() {
    mRenderInfo = null;
    mRenderInfos = null;
    mPrevData = null;
    mNextData = null;
  }

  public static String changeTypeToString(@Type int type) {
    switch (type) {
      case INSERT:
        return "INSERT";
      case UPDATE:
        return "UPDATE";
      case DELETE:
        return "DELETE";
      case MOVE:
        return "MOVE";
      case INSERT_RANGE:
        return "INSERT_RANGE";
      case UPDATE_RANGE:
        return "UPDATE_RANGE";
      case DELETE_RANGE:
        return "DELETE_RANGE";
      default:
        return "UNKNOW TYPE";
    }
  }
}
