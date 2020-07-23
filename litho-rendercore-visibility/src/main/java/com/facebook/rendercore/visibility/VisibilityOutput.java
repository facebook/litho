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
 * Stores information about a node which has registered visibility handlers for. The information is
 * passed to the visibility extension which then dispatches the appropriate events.
 */
public class VisibilityOutput {

  private final String mId;
  private final String mKey;
  private final Rect mBounds;

  private final float mVisibleHeightRatio;
  private final float mVisibleWidthRatio;

  private final @Nullable Function mVisibleEventHandler;
  private final @Nullable Function mFocusedEventHandler;
  private final @Nullable Function mUnfocusedEventHandler;
  private final @Nullable Function mFullImpressionEventHandler;
  private final @Nullable Function mInvisibleEventHandler;
  private final @Nullable Function mVisibilityChangedEventHandler;

  private float mFocusedRatio;

  public VisibilityOutput(
      final String id,
      final String key,
      final Rect bounds,
      final float visibleHeightRatio,
      final float visibleWidthRatio,
      final @Nullable Function visibleEventHandler,
      final @Nullable Function focusedEventHandler,
      final @Nullable Function unfocusedEventHandler,
      final @Nullable Function fullImpressionEventHandler,
      final @Nullable Function invisibleEventHandler,
      final @Nullable Function visibilityChangedEventHandler) {
    mId = id;
    mKey = key;
    mBounds = bounds;
    mVisibleHeightRatio = visibleHeightRatio;
    mVisibleWidthRatio = visibleWidthRatio;
    mVisibleEventHandler = visibleEventHandler;
    mFocusedEventHandler = focusedEventHandler;
    mUnfocusedEventHandler = unfocusedEventHandler;
    mFullImpressionEventHandler = fullImpressionEventHandler;
    mInvisibleEventHandler = invisibleEventHandler;
    mVisibilityChangedEventHandler = visibilityChangedEventHandler;
  }

  /**
   * An identifier unique to this visibility output. This needs to be unique for every output in a
   * given {@link com.facebook.rendercore.RenderTree}.
   */
  public String getId() {
    return mId;
  }

  /** A pretty name of this visibility output; does not need to be unique. */
  public String getKey() {
    return mKey;
  }

  public Rect getBounds() {
    return mBounds;
  }

  public float getVisibilityTop() {
    if (getVisibleHeightRatio() == 0) {
      return mBounds.top;
    }

    return mBounds.top + getVisibleHeightRatio() * (mBounds.bottom - mBounds.top);
  }

  public float getVisibilityBottom() {
    if (getVisibleHeightRatio() == 0) {
      return mBounds.bottom;
    }

    return mBounds.bottom - getVisibleHeightRatio() * (mBounds.bottom - mBounds.top);
  }

  public float getVisibilityLeft() {
    return mBounds.left + getVisibleWidthRatio() * (mBounds.right - mBounds.left);
  }

  public float getVisibilityRight() {
    return mBounds.right - getVisibleHeightRatio() * (mBounds.right - mBounds.left);
  }

  public float getFullImpressionTop() {
    return mBounds.bottom;
  }

  public float getFullImpressionBottom() {
    return mBounds.top;
  }

  public float getFullImpressionLeft() {
    return mBounds.right;
  }

  public float getFullImpressionRight() {
    return mBounds.left;
  }

  public float getFocusedTop() {
    return mBounds.top + mFocusedRatio * (mBounds.bottom - mBounds.top);
  }

  public float getFocusedBottom() {
    return mBounds.bottom - mFocusedRatio * (mBounds.bottom - mBounds.top);
  }

  public float getFocusedLeft() {
    return mBounds.left + mFocusedRatio * (mBounds.right - mBounds.left);
  }

  public float getFocusedRight() {
    return mBounds.right - mFocusedRatio * (mBounds.right - mBounds.left);
  }

  public float getVisibleHeightRatio() {
    return mVisibleHeightRatio;
  }

  public float getVisibleWidthRatio() {
    return mVisibleWidthRatio;
  }

  public void setFocusedRatio(float focusedRatio) {
    mFocusedRatio = focusedRatio;
  }

  public @Nullable Function getVisibleEventHandler() {
    return mVisibleEventHandler;
  }

  public int getComponentArea() {
    final Rect rect = getBounds();
    return rect.isEmpty() ? 0 : (rect.width() * rect.height());
  }

  public @Nullable Function getFocusedEventHandler() {
    return mFocusedEventHandler;
  }

  public @Nullable Function getUnfocusedEventHandler() {
    return mUnfocusedEventHandler;
  }

  public @Nullable Function getFullImpressionEventHandler() {
    return mFullImpressionEventHandler;
  }

  public @Nullable Function getInvisibleEventHandler() {
    return mInvisibleEventHandler;
  }

  public @Nullable Function getVisibilityChangedEventHandler() {
    return mVisibilityChangedEventHandler;
  }
}
