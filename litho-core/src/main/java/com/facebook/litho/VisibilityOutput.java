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

/**
 * Stores information about a {@link Component} which has registered handlers for {@link
 * VisibleEvent} or {@link InvisibleEvent}. The information is passed to {@link MountState} which
 * then dispatches the appropriate events.
 */
class VisibilityOutput {

  private final Rect mBounds;

  private final Component mComponent;

  private final float mVisibleHeightRatio;
  private final float mVisibleWidthRatio;

  private final @Nullable EventHandler<VisibleEvent> mVisibleEventHandler;
  private final @Nullable EventHandler<FocusedVisibleEvent> mFocusedEventHandler;
  private final @Nullable EventHandler<UnfocusedVisibleEvent> mUnfocusedEventHandler;
  private final @Nullable EventHandler<FullImpressionVisibleEvent> mFullImpressionEventHandler;
  private final @Nullable EventHandler<InvisibleEvent> mInvisibleEventHandler;
  private final @Nullable EventHandler<VisibilityChangedEvent> mVisibilityChangedEventHandler;

  private float mFocusedRatio;

  public VisibilityOutput(
      final Component component,
      final Rect bounds,
      final float visibleHeightRatio,
      final float visibleWidthRatio,
      final @Nullable EventHandler<VisibleEvent> visibleEventHandler,
      final @Nullable EventHandler<FocusedVisibleEvent> focusedEventHandler,
      final @Nullable EventHandler<UnfocusedVisibleEvent> unfocusedEventHandler,
      final @Nullable EventHandler<FullImpressionVisibleEvent> fullImpressionEventHandler,
      final @Nullable EventHandler<InvisibleEvent> invisibleEventHandler,
      final @Nullable EventHandler<VisibilityChangedEvent> visibilityChangedEventHandler) {
    mComponent = component;
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

  @Nullable
  String getId() {
    return mComponent == null ? null : mComponent.getGlobalKey();
  }

  Component getComponent() {
    return mComponent;
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

  EventHandler<VisibleEvent> getVisibleEventHandler() {
    return mVisibleEventHandler;
  }

  int getComponentArea() {
    final Rect rect = getBounds();
    return rect.isEmpty() ? 0 : (rect.width() * rect.height());
  }

  EventHandler<FocusedVisibleEvent> getFocusedEventHandler() {
    return mFocusedEventHandler;
  }

  EventHandler<UnfocusedVisibleEvent> getUnfocusedEventHandler() {
    return mUnfocusedEventHandler;
  }

  EventHandler<FullImpressionVisibleEvent> getFullImpressionEventHandler() {
    return mFullImpressionEventHandler;
  }

  EventHandler<InvisibleEvent> getInvisibleEventHandler() {
    return mInvisibleEventHandler;
  }

  @Nullable
  EventHandler<VisibilityChangedEvent> getVisibilityChangedEventHandler() {
    return mVisibilityChangedEventHandler;
  }
}
