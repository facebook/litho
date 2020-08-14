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

package com.facebook.rendercore.visibility;

import android.graphics.Rect;
import androidx.annotation.Nullable;
import com.facebook.rendercore.Function;

/**
 * Holds information about a VisibilityOutput (that is, about an item for which a visibility event
 * handler have been set). This class is justified by the fact that VisibilityOutput should be
 * immutable.
 */
public class VisibilityItem {

  private static final int FLAG_LEFT_EDGE_VISIBLE = 1 << 1;
  private static final int FLAG_TOP_EDGE_VISIBLE = 1 << 2;
  private static final int FLAG_RIGHT_EDGE_VISIBLE = 1 << 3;
  private static final int FLAG_BOTTOM_EDGE_VISIBLE = 1 << 4;
  private static final int FLAG_FOCUSED_RANGE = 1 << 5;

  private final String mKey;
  private final @Nullable Function<Void> mVisibilityChangedHandler;

  // The invisible event and unfocused event handlers are required to make it possible to dispatch
  // the corresponding event when unbind is called or when the MountState is reset.
  private @Nullable Function<Void> mInvisibleHandler;
  private @Nullable Function<Void> mUnfocusedHandler;

  private boolean mDoNotClearInThisPass;
  private boolean mWasFullyVisible;

  private int mFlags;

  public VisibilityItem(
      final String key,
      final @Nullable Function<Void> invisibleHandler,
      final @Nullable Function<Void> unfocusedHandler,
      final @Nullable Function<Void> visibilityChangedHandler) {
    mKey = key;
    mInvisibleHandler = invisibleHandler;
    mUnfocusedHandler = unfocusedHandler;
    mVisibilityChangedHandler = visibilityChangedHandler;
  }

  public String getKey() {
    return mKey;
  }

  /** Sets the invisible event handler. */
  public void setInvisibleHandler(@Nullable Function<Void> invisibleHandler) {
    mInvisibleHandler = invisibleHandler;
  }

  /** Returns the invisible event handler. */
  public @Nullable Function<Void> getInvisibleHandler() {
    return mInvisibleHandler;
  }

  /** Sets the unfocused event handler. */
  public void setUnfocusedHandler(@Nullable Function<Void> unfocusedHandler) {
    mUnfocusedHandler = unfocusedHandler;
  }

  /** Returns the unfocused event handler. */
  public @Nullable Function<Void> getUnfocusedHandler() {
    return mUnfocusedHandler;
  }

  public @Nullable Function<Void> getVisibilityChangedHandler() {
    return mVisibilityChangedHandler;
  }

  public boolean isInFocusedRange() {
    return (mFlags & FLAG_FOCUSED_RANGE) != 0;
  }

  public void setFocusedRange(boolean isFocused) {
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
  public boolean isInFullImpressionRange() {
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
  public void setVisibleEdges(Rect componentBounds, Rect componentVisibleBounds) {
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

  public boolean doNotClearInThisPass() {
    return mDoNotClearInThisPass;
  }

  public void setDoNotClearInThisPass(boolean doNotClearInThisPass) {
    mDoNotClearInThisPass = doNotClearInThisPass;
  }

  public boolean wasFullyVisible() {
    return mWasFullyVisible;
  }

  public void setWasFullyVisible(boolean fullyVisible) {
    mWasFullyVisible = fullyVisible;
  }
}
