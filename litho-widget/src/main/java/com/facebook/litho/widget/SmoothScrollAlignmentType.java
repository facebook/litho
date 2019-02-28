/*
 * Copyright 2018-present Facebook, Inc.
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
package com.facebook.litho.widget;

import androidx.recyclerview.widget.LinearSmoothScroller;

public enum SmoothScrollAlignmentType {
  /**
   * Use the layout manager's default smooth scrolling, needed for backwards compatibility because
   * certain layout managers implement their own smooth scrolling
   */
  DEFAULT(-5),

  /**
   * Default alias to LinearSmoothScroller.SNAP_TO_ANY, align child top edge to viewport top or
   * bottom edge based on which one is closer
   */
  SNAP_TO_ANY(LinearSmoothScroller.SNAP_TO_ANY),

  /**
   * Default alias to LinearSmoothScroller.SNAP_TO_START, align child top edge to viewport top edge
   */
  SNAP_TO_START(LinearSmoothScroller.SNAP_TO_START),

  /**
   * Default alias to LinearSmoothScroller.SNAP_TO_END, align child top edge to viewport bottom edge
   */
  SNAP_TO_END(LinearSmoothScroller.SNAP_TO_END),

  /** Scroll the selected position to the center of the RecyclerView. */
  SNAP_TO_CENTER(-6);

  private int value;

  public int getValue() {
    return this.value;
  }

  // enum constructor - cannot be public or protected
  private SmoothScrollAlignmentType(int value) {
    this.value = value;
  }
}
