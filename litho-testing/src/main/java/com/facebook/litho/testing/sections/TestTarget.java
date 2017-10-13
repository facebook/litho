/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.sections;

import com.facebook.litho.sections.SectionTree;
import com.facebook.litho.widget.RenderInfo;
import java.util.ArrayList;
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

    Operation(int op, int index) {
      this(op, index, -1);
    }

    Operation(int op, int index, int toIndex) {
      this(op, index, toIndex, 1);
    }

    Operation(int op, int index, int toIndex,  int rangeCount) {
      mOp = op;
      mIndex = index;
      mToIndex = toIndex;
      mRangeCount = rangeCount;
    }
  }

  final List<Operation> mOperations = new ArrayList<>();
  int mNumChanges = 0;
  int mFocusTo = -1;
  int mFocusToOffset = -1;

  public List<Operation> getOperations() {
    return mOperations;
  }

  public int getNumChanges() {
    return mNumChanges;
  }

  public boolean wereChangesHandled() {
    return !mOperations.isEmpty();
  }

  @Override
  public void insert(int index, RenderInfo renderInfo) {
    mOperations.add(new Operation(INSERT, index));
    mNumChanges++;
  }

  @Override
  public void insertRange(
      int index, int count, List<RenderInfo> renderInfos) {
    mOperations.add(new Operation(INSERT_RANGE, index, -1, count));
    mNumChanges += count;
  }

  @Override
  public void update(int index, RenderInfo renderInfo) {
    mOperations.add(new Operation(UPDATE, index));
    mNumChanges++;
  }

  @Override
  public void updateRange(
      int index, int count, List<RenderInfo> renderInfos) {
    mOperations.add(new Operation(UPDATE_RANGE, index, -1, count));
    mNumChanges += count;
  }

  @Override
  public void delete(int index) {
    mOperations.add(new Operation(DELETE, index));
    mNumChanges++;
  }

  @Override
  public void deleteRange(int index, int count) {
    mOperations.add(new Operation(DELETE_RANGE, index, -1, count));
    mNumChanges += count;
  }

  @Override
  public void move(int fromPosition, int toPosition) {
    mOperations.add(new Operation(MOVE, fromPosition, toPosition));
    mNumChanges++;
  }

  @Override
  public void requestFocus(int index) {
    mFocusTo = index;
  }

  @Override
  public void requestFocusWithOffset(int index, int offset) {
    mFocusTo = index;
    mFocusToOffset = offset;
  }

  public void clear() {
    mOperations.clear();
    mNumChanges = 0;
  }

  public int getFocusedTo() {
    return mFocusTo;
  }

  public int getFocusedToOffset() {
    return mFocusToOffset;
  }
}
