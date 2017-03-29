/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.Rect;

/**
 * Holds information about a VisibilityOutput (that is, about a component for which a visibility
 * event handler has been set). This class is justified by the fact that VisibilityOuput should
 * be immutable.
 */
class VisibilityItem {

  private static final int FLAG_LEFT_EDGE_VISIBLE = 1 << 1;
  private static final int FLAG_TOP_EDGE_VISIBLE = 1 << 2;
  private static final int FLAG_RIGHT_EDGE_VISIBLE = 1 << 3;
  private static final int FLAG_BOTTOM_EDGE_VISIBLE = 1 << 4;
  private static final int FLAG_FOCUSED_RANGE = 1 << 5;

  private int mFlags;
  // The invisible event handler is required to make it possible to dispatch the InvisibleEvent when
  // on unbind is called or when the MountState is reset.
  private EventHandler mInvisibleHandler;

  public VisibilityItem() {
    mFlags = 0;
    mInvisibleHandler = null;
  }

  /**
   * Sets the invisible event handler.
   */
  void setInvisibleHandler(EventHandler invisibleHandler) {
    mInvisibleHandler = invisibleHandler;
  }

  /**
   * Returns the invisible event handler.
   */
  EventHandler getInvisibleHandler() {
    return mInvisibleHandler;
  }

  boolean isInFocusedRange() {
    return (mFlags & FLAG_FOCUSED_RANGE) != 0;
  }

  void setIsInFocusedRange() {
    mFlags |= FLAG_FOCUSED_RANGE;
  }

  /**
   * Returns true if the component associated with this VisibilityItem is in the full impression
   * range.
   */
  boolean isInFullImpressionRange() {
    final int allEdgesVisible = FLAG_LEFT_EDGE_VISIBLE
        | FLAG_TOP_EDGE_VISIBLE
        | FLAG_RIGHT_EDGE_VISIBLE
        | FLAG_BOTTOM_EDGE_VISIBLE;

    return (mFlags & allEdgesVisible) == allEdgesVisible;
  }

  /**
   * Sets the flags corresponding to the edges of the component that are visible. Afterwards, it
   * checks if the component has entered the full impression visible range and, if so, it sets the
   * appropriate flag.
   */
  void setVisibleEdges(Rect componentBounds, Rect componentVisibleBounds) {
    if (componentBounds.top == componentVisibleBounds.top) {
      mFlags |= FLAG_TOP_EDGE_VISIBLE;
    }
    if (componentBounds.bottom == componentVisibleBounds.bottom) {
      mFlags |= FLAG_BOTTOM_EDGE_VISIBLE;
    }
    if (componentBounds.left == componentVisibleBounds.left) {
      mFlags |= FLAG_LEFT_EDGE_VISIBLE;
    }
    if (componentBounds.right == componentVisibleBounds.right) {
      mFlags |= FLAG_RIGHT_EDGE_VISIBLE;
    }
  }

  void release() {
    mFlags = 0;
    mInvisibleHandler = null;
  }
}
