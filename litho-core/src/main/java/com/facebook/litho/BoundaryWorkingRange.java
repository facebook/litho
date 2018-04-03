/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

/**
 * An implementation of working range that uses offset to define the upper bound and the lower
 * boundary. If the position is inside the boundary, it's in the range, otherwise it's out of the
 * range.
 */
public class BoundaryWorkingRange implements WorkingRange {

  private static final int OFFSET = 1;

  private final int mOffset;

  public BoundaryWorkingRange() {
    this(OFFSET);
  }

  public BoundaryWorkingRange(int offset) {
    mOffset = offset;
  }

  @Override
  public boolean shouldEnterRange(
      int position,
      int firstVisibleIndex,
      int lastVisibleIndex,
      int firstFullyVisibleIndex,
      int lastFullyVisibleIndex) {
    return isInRange(position, firstVisibleIndex, lastVisibleIndex, mOffset);
  }

  @Override
  public boolean shouldExitRange(
      int position,
      int firstVisibleIndex,
      int lastVisibleIndex,
      int firstFullyVisibleIndex,
      int lastFullyVisibleIndex) {
    return !isInRange(position, firstVisibleIndex, lastVisibleIndex, mOffset);
  }

  private static boolean isInRange(
      int position, int firstVisiblePosition, int lastVisiblePosition, int offset) {
    int lowerBound = firstVisiblePosition - offset;
    int upperBound = lastVisiblePosition + offset;

    return (position >= lowerBound && position <= upperBound);
  }
}
