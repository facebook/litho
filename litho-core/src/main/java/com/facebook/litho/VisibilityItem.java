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

import android.graphics.Rect;
import androidx.annotation.Nullable;
import com.facebook.rendercore.Function;

/**
 * Holds information about a VisibilityOutput (that is, about a component for which a visibility
 * event handler has been set). This class is justified by the fact that VisibilityOutput should be
 * immutable.
 */
class VisibilityItem {

  private static final int FLAG_LEFT_EDGE_VISIBLE = 1 << 1;
  private static final int FLAG_TOP_EDGE_VISIBLE = 1 << 2;
  private static final int FLAG_RIGHT_EDGE_VISIBLE = 1 << 3;
  private static final int FLAG_BOTTOM_EDGE_VISIBLE = 1 << 4;
  private static final int FLAG_FOCUSED_RANGE = 1 << 5;

  private final String mKey;
  private final @Nullable Function mVisibilityChangedHandler;

  // The invisible event and unfocused event handlers are required to make it possible to dispatch
  // the corresponding event when unbind is called or when the MountState is reset.
  private @Nullable Function mInvisibleHandler;
  private @Nullable Function mUnfocusedHandler;

  private boolean mDoNotClearInThisPass;
  private boolean mWasFullyVisible;

  private int mFlags;

  public VisibilityItem(
      final String key,
      final @Nullable Function invisibleHandler,
      final @Nullable Function unfocusedHandler,
      final @Nullable Function visibilityChangedHandler) {
    mKey = key;
    mInvisibleHandler = invisibleHandler;
    mUnfocusedHandler = unfocusedHandler;
    mVisibilityChangedHandler = visibilityChangedHandler;
  }

  String getKey() {
    return mKey;
  }

  /** Sets the invisible event handler. */
  void setInvisibleHandler(@Nullable Function invisibleHandler) {
    mInvisibleHandler = invisibleHandler;
  }

  /** Returns the invisible event handler. */
  public @Nullable Function getInvisibleHandler() {
    return mInvisibleHandler;
  }

  /** Sets the unfocused event handler. */
  void setUnfocusedHandler(@Nullable Function unfocusedHandler) {
    mUnfocusedHandler = unfocusedHandler;
  }

  /** Returns the unfocused event handler. */
  public @Nullable Function getUnfocusedHandler() {
    return mUnfocusedHandler;
  }

  public @Nullable Function getVisibilityChangedHandler() {
    return mVisibilityChangedHandler;
  }

  boolean isInFocusedRange() {
    return (mFlags & FLAG_FOCUSED_RANGE) != 0;
  }

  void setFocusedRange(boolean isFocused) {
    if (isFocused) {
      mFlags |= FLAG_FOCUSED_RANGE;
    } else {
      mFlags &= ~FLAG_FOCUSED_RANGE;
    }
  }

  /**
   * Returns true if the component associated with this VisibilityItem is in the full impression
   * range.
   */
  boolean isInFullImpressionRange() {
    final int allEdgesVisible =
        FLAG_LEFT_EDGE_VISIBLE
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

  boolean doNotClearInThisPass() {
    return mDoNotClearInThisPass;
  }

  void setDoNotClearInThisPass(boolean doNotClearInThisPass) {
    mDoNotClearInThisPass = doNotClearInThisPass;
  }

  boolean wasFullyVisible() {
    return mWasFullyVisible;
  }

  void setWasFullyVisible(boolean fullyVisible) {
    mWasFullyVisible = fullyVisible;
  }
}
