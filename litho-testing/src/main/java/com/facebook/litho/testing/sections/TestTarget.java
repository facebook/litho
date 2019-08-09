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

import androidx.annotation.Nullable;
import com.facebook.litho.sections.SectionTree;
import com.facebook.litho.widget.ChangeSetCompleteCallback;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.litho.widget.SmoothScrollAlignmentType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A test target that keeps track of operations and changes. */
public class TestTarget implements SectionTree.Target {
  public static final int INSERT = 0;
  public static final int UPDATE = 1;
  public static final int DELETE = 2;
  public static final int MOVE = 3;
  public static final int INSERT_RANGE = 4;
  public static final int UPDATE_RANGE = 5;
  public static final int DELETE_RANGE = 6;

  public static class Operation {
    public final int mOp;
    public final int mIndex;
    public final int mToIndex;
    public final int mRangeCount;
    public @Nullable List<?> mPrevData;
    public @Nullable List<?> mNewData;

    Operation(int op, int index, @Nullable Object prevData, @Nullable Object newData) {
      this(op, index, -1, prevData, newData);
    }

    Operation(int op, int index, int toIndex, @Nullable Object prevData, @Nullable Object newData) {
      this(
          op,
          index,
          toIndex,
          1,
          prevData != null ? Collections.singletonList(prevData) : null,
          newData != null ? Collections.singletonList(newData) : null);
    }

    Operation(
        int op,
        int index,
        int toIndex,
        int rangeCount,
        @Nullable List<Object> prevData,
        @Nullable List<Object> newData) {
      mOp = op;
      mIndex = index;
      mToIndex = toIndex;
      mRangeCount = rangeCount;
      mPrevData = prevData;
      mNewData = newData;
    }
  }

  final List<Operation> mOperations = new ArrayList<>();
  final List<RenderInfo> mRenderInfos = new ArrayList<>();
  int mNumChanges = 0;
  int mFocusTo = -1;
  int mFocusToOffset = -1;
  boolean mWasNotifyChangeSetCompleteCalledWithChangedData = false;

  public List<Operation> getOperations() {
    return mOperations;
  }

  public int getNumChanges() {
    return mNumChanges;
  }

  public boolean wereChangesHandled() {
    return !mOperations.isEmpty();
  }

  public boolean wasNotifyChangeSetCompleteCalledWithChangedData() {
    return mWasNotifyChangeSetCompleteCalledWithChangedData;
  }

  @Override
  public void insert(int index, RenderInfo renderInfo) {
    final Object data = renderInfo.getCustomAttribute("model");
    mOperations.add(new Operation(INSERT, index, null, data));
    mNumChanges++;
    mRenderInfos.add(index, renderInfo);
  }

  @Override
  public void insertRange(int index, int count, List<RenderInfo> renderInfos) {
    final List<Object> data = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      data.add(renderInfos.get(i).getCustomAttribute("model"));
    }
    mOperations.add(new Operation(INSERT_RANGE, index, -1, count, null, data));
    mNumChanges += count;
    mRenderInfos.addAll(index, renderInfos);
  }

  @Override
  public void update(int index, RenderInfo renderInfo) {
    final Object oldData = mRenderInfos.get(index).getCustomAttribute("model");
    final Object newData = renderInfo.getCustomAttribute("model");
    mOperations.add(new Operation(UPDATE, index, oldData, newData));
    mNumChanges++;
    mRenderInfos.set(index, renderInfo);
  }

  @Override
  public void updateRange(int index, int count, List<RenderInfo> renderInfos) {
    final List<Object> oldData = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      oldData.add(mRenderInfos.get(index + i).getCustomAttribute("model"));
    }
    final List<Object> newData = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      newData.add(renderInfos.get(i).getCustomAttribute("model"));
    }
    mOperations.add(new Operation(UPDATE_RANGE, index, -1, count, oldData, newData));
    mNumChanges += count;
    for (int i = 0; i < count; i++) {
      mRenderInfos.set(index + i, renderInfos.get(i));
    }
  }

  @Override
  public void delete(int index) {
    final Object data = mRenderInfos.remove(index).getCustomAttribute("model");
    mOperations.add(new Operation(DELETE, index, data, null));
    mNumChanges++;
  }

  @Override
  public void deleteRange(int index, int count) {
    final List<RenderInfo> renderInfos = mRenderInfos.subList(index, index + count);
    final List<Object> data = new ArrayList<>();
    for (int i = 0, size = renderInfos.size(); i < size; i++) {
      data.add(renderInfos.get(i).getCustomAttribute("model"));
    }
    mOperations.add(new Operation(DELETE_RANGE, index, -1, count, data, null));
    mNumChanges += count;
  }

  @Override
  public void move(int fromPosition, int toPosition) {
    final RenderInfo renderInfo = mRenderInfos.remove(fromPosition);
    final Object data = renderInfo.getCustomAttribute("model");
    mOperations.add(new Operation(MOVE, fromPosition, toPosition, data, data));
    mNumChanges++;
    mRenderInfos.add(toPosition, renderInfo);
  }

  @Override
  public void notifyChangeSetComplete(
      boolean isDataChanged, ChangeSetCompleteCallback changeSetCompleteCallback) {
    mWasNotifyChangeSetCompleteCalledWithChangedData = isDataChanged;
    changeSetCompleteCallback.onDataBound();
    changeSetCompleteCallback.onDataRendered(false, 0);
  }

  @Override
  public void requestFocus(int index) {
    mFocusTo = index;
  }

  @Override
  public void requestSmoothFocus(int index, int offset, SmoothScrollAlignmentType type) {
    requestFocus(index);
  }

  @Override
  public void requestFocusWithOffset(int index, int offset) {
    mFocusTo = index;
    mFocusToOffset = offset;
  }

  @Override
  public boolean supportsBackgroundChangeSets() {
    return false;
  }

  @Override
  public void changeConfig(DynamicConfig dynamicConfig) {}

  public void clear() {
    mOperations.clear();
    mNumChanges = 0;
    mWasNotifyChangeSetCompleteCalledWithChangedData = false;
  }

  public int getFocusedTo() {
    return mFocusTo;
  }

  public int getFocusedToOffset() {
    return mFocusToOffset;
  }
}
