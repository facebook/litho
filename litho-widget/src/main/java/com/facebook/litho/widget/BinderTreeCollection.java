/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import java.util.List;

import android.support.v4.util.SparseArrayCompat;

import com.facebook.litho.ComponentTree;

/**
 * BinderTreeCollection hide the structure used to operate on ComponentTrees used by the Binder.
 * Right now we are operating on a SparseArray.
 * Shifting the SparseArray left and right is potentially bad. Each call uses a System.arraycopy()
 * this means shiftSparseArrayRight/Left is O(n*m), where n is (sparseArray.size() - fromPosition)
 * and m is shiftByAmount. However we don't expect this to happen often and with the elastic ranges,
 * the items to shift should always be quite small.
 */
class BinderTreeCollection {

  private final SparseArrayCompat<ComponentTree> mItems;

  BinderTreeCollection() {
    mItems = new SparseArrayCompat<>();
  }

  /**
   * Get the first stored position or -1 if it's empty.
   */
  int getFirstPosition() {
    return mItems.size() == 0
        ? -1
        : mItems.keyAt(0);
  }

  /**
   * Replace the tree at the given position.
   */
  void put(int position, ComponentTree item) {
    mItems.put(position, item);
  }

  /**
   * While {@link #put(int, ComponentTree)} only replace the element at "position", this method
   * shift all the elements on the right of "position" by 1.
   */
  void insert(int position, ComponentTree item) {
    shiftRangeRight(position, getFirstPosition() + size() - position, 1);
    mItems.put(position, item);
  }

  /**
   * Move an ComponentTree from a position to another position.
   */
  void move(int fromPosition, int toPosition) {
    ComponentTree movingComponentTree = mItems.get(fromPosition);

    if (toPosition < fromPosition) {
      shiftRangeRight(toPosition, fromPosition - toPosition, 1);
    } else {
      shiftRangeLeft(fromPosition + 1, toPosition - fromPosition, 1);
    }

    put(toPosition, movingComponentTree);
  }

  /**
   * Simply remove the ComponentTree at the given position without shifting the rest.
   */
  void remove(int position) {
    mItems.remove(position);
  }

  /**
   * Remove the ComponentTree at position and shift left all the successive ones.
   */
  void removeShiftingLeft(int position) {
    removeShiftingLeft(position, 1);
  }

  /**
   * Remove a range of ComponentTrees and shift left all the successive ones. This will remove
   * itemsToRemoveCount items starting from positionStart.
   */
  void removeShiftingLeft(int positionStart, int itemsToRemoveCount) {
    if (positionStart + itemsToRemoveCount < getFirstPosition() + size()) {
      final int start = Math.max(positionStart + itemsToRemoveCount, getFirstPosition());
      shiftRangeLeft(start, getFirstPosition() + size() - start, itemsToRemoveCount);
    } else {
      // E.g. mItems.keys = [3, 4, 5], positionStart = 4 and itemsToRemoveCount = 3. The expected
      // result should be mItems.keys = [3]. In this case, we will not shift anything, but rather
      // just remove the items at the end of the collection.
      final int positionEnd = getFirstPosition() + size() - 1;
      for (int position = positionStart; position <= positionEnd; position++) {
        mItems.remove(position);
      }
    }
  }

  /**
   * Inserts a new item in the list at a given position. All the items preceding that position are
   * shifted left.
   * @param position the position at which the new item is inserted
   * @param item the new item
   */
  void insertShiftingLeft(int position, ComponentTree item) {
    final int firstPosition = getFirstPosition();
    shiftRangeLeft(firstPosition, position - firstPosition + 1, 1);
    mItems.put(position, item);
