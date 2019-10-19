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
