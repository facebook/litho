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
 * Stores information about a {@link Component} which has registered handlers for {@link
 * VisibleEvent} or {@link InvisibleEvent}. The information is passed to {@link MountState} which
 * then dispatches the appropriate events.
 */
class VisibilityOutput {

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

  String getId() {
    return mId;
  }

  String getKey() {
    return mKey;
  }

  Rect getBounds() {
    return mBounds;
  }

  float getVisibilityTop() {
    if (getVisibleHeightRatio() == 0) {
      return mBounds.top;
    }

    return mBounds.top + getVisibleHeightRatio() * (mBounds.bottom - mBounds.top);
  }

  float getVisibilityBottom() {
    if (getVisibleHeightRatio() == 0) {
      return mBounds.bottom;
    }

    return mBounds.bottom - getVisibleHeightRatio() * (mBounds.bottom - mBounds.top);
  }

  float getVisibilityLeft() {
    return mBounds.left + getVisibleWidthRatio() * (mBounds.right - mBounds.left);
  }

  float getVisibilityRight() {
    return mBounds.right - getVisibleHeightRatio() * (mBounds.right - mBounds.left);
  }

  float getFullImpressionTop() {
    return mBounds.bottom;
  }

  float getFullImpressionBottom() {
    return mBounds.top;
  }

  float getFullImpressionLeft() {
    return mBounds.right;
  }

  float getFullImpressionRight() {
    return mBounds.left;
  }

  float getFocusedTop() {
    return mBounds.top + mFocusedRatio * (mBounds.bottom - mBounds.top);
  }

  float getFocusedBottom() {
    return mBounds.bottom - mFocusedRatio * (mBounds.bottom - mBounds.top);
  }

  float getFocusedLeft() {
    return mBounds.left + mFocusedRatio * (mBounds.right - mBounds.left);
  }

  float getFocusedRight() {
    return mBounds.right - mFocusedRatio * (mBounds.right - mBounds.left);
  }

  float getVisibleHeightRatio() {
    return mVisibleHeightRatio;
  }

  float getVisibleWidthRatio() {
    return mVisibleWidthRatio;
  }

  void setFocusedRatio(float focusedRatio) {
    mFocusedRatio = focusedRatio;
  }

  public @Nullable Function getVisibleEventHandler() {
    return mVisibleEventHandler;
  }

  int getComponentArea() {
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
