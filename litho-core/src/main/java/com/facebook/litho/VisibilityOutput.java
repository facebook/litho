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

  private Component mComponent;
  private final Rect mBounds = new Rect();
  private float mVisibleHeightRatio;
  private float mVisibleWidthRatio;
  private EventHandler<VisibleEvent> mVisibleEventHandler;
  private EventHandler<FocusedVisibleEvent> mFocusedEventHandler;
  private EventHandler<UnfocusedVisibleEvent> mUnfocusedEventHandler;
  private EventHandler<FullImpressionVisibleEvent> mFullImpressionEventHandler;
  private EventHandler<InvisibleEvent> mInvisibleEventHandler;
  private @Nullable EventHandler<VisibilityChangedEvent> mVisibilityChangedEventHandler;

  @Nullable
  String getId() {
    return mComponent == null ? null : mComponent.getGlobalKey();
  }

  Component getComponent() {
    return mComponent;
  }

  void setComponent(Component component) {
    mComponent = component;
  }

  Rect getBounds() {
    return mBounds;
  }

  void setBounds(int l, int t, int r, int b) {
    mBounds.set(l, t, r, b);
  }

  void setBounds(Rect bounds) {
    mBounds.set(bounds);
  }

  void setVisibleHeightRatio(float visibleHeightRatio) {
    mVisibleHeightRatio = visibleHeightRatio;
  }

  float getVisibleHeightRatio() {
    return mVisibleHeightRatio;
  }

  void setVisibleWidthRatio(float visibleWidthRatio) {
    mVisibleWidthRatio = visibleWidthRatio;
  }

  float getVisibleWidthRatio() {
    return mVisibleWidthRatio;
  }

  void setVisibleEventHandler(EventHandler<VisibleEvent> visibleEventHandler) {
    mVisibleEventHandler = visibleEventHandler;
  }

  EventHandler<VisibleEvent> getVisibleEventHandler() {
    return mVisibleEventHandler;
  }

  void setFocusedEventHandler(EventHandler<FocusedVisibleEvent> focusedEventHandler) {
    mFocusedEventHandler = focusedEventHandler;
  }

  EventHandler<FocusedVisibleEvent> getFocusedEventHandler() {
    return mFocusedEventHandler;
  }

  void setUnfocusedEventHandler(EventHandler<UnfocusedVisibleEvent> unfocusedEventHandler) {
    mUnfocusedEventHandler = unfocusedEventHandler;
  }

  EventHandler<UnfocusedVisibleEvent> getUnfocusedEventHandler() {
    return mUnfocusedEventHandler;
  }

  void setFullImpressionEventHandler(
      EventHandler<FullImpressionVisibleEvent> fullImpressionEventHandler) {
    mFullImpressionEventHandler = fullImpressionEventHandler;
  }

  EventHandler<FullImpressionVisibleEvent> getFullImpressionEventHandler() {
    return mFullImpressionEventHandler;
  }

  void setInvisibleEventHandler(EventHandler<InvisibleEvent> invisibleEventHandler) {
    mInvisibleEventHandler = invisibleEventHandler;
  }

  EventHandler<InvisibleEvent> getInvisibleEventHandler() {
    return mInvisibleEventHandler;
  }

  void setVisibilityChangedEventHandler(
      @Nullable EventHandler<VisibilityChangedEvent> visibilityChangedEventHandler) {
    mVisibilityChangedEventHandler = visibilityChangedEventHandler;
  }

  @Nullable
  EventHandler<VisibilityChangedEvent> getVisibilityChangedEventHandler() {
    return mVisibilityChangedEventHandler;
  }
}
