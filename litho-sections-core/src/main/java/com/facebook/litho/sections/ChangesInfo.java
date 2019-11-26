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

import com.facebook.litho.sections.SectionTree.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A ChangesInfo represent a list of Change that has to be applied to a {@link Target} as the result
 * of an update of a {@link Section}.
 */
public class ChangesInfo {

  private final List<Change> mChanges;

  public ChangesInfo(List<Change> changes) {
    mChanges = Collections.unmodifiableList(changes);
  }

  /** @return a list of change in the visible range. */
  public List<Change> getVisibleChanges(
      int firstVisibleIndex, int lastVisibleIndex, int globalOffset) {
    int globalFirstVisibleIndex = globalOffset + (firstVisibleIndex >= 0 ? firstVisibleIndex : 0);
    int globalLastVisibleIndex = globalOffset + (lastVisibleIndex >= 0 ? lastVisibleIndex : 0);
    final List<Change> result = new ArrayList<>();
    for (int i = 0, size = mChanges.size(); i < size; i++) {
      final Change change = mChanges.get(i);
      if (isRangeOverlap(
          globalFirstVisibleIndex,
          globalLastVisibleIndex,
          change.getIndex(),
          change.getIndex() + change.getCount() - 1)) {
        result.add(change);
      }
    }
    return result;
  }

  public List<Change> getAllChanges() {
    return mChanges;
  }

  /**
   * @param s1 Starting point from Range 1
   * @param e1 Ending point from Range 1
   * @param s2 Starting point from Range 2
   * @param e2 Ending point from Range 2
   * @return True if range 1 overlaps with range 2
   */
  private static boolean isRangeOverlap(int s1, int e1, int s2, int e2) {
    return Math.max(s1, s2) <= Math.min(e1, e2);
  }
}
