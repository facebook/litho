/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.Rect;

/**
 * Stores information about a {@link Component} which has registered handlers for
 * {@link VisibleEvent} or {@link InvisibleEvent}. The information is passed to {@link MountState}
 * which then dispatches the appropriate events.
 */
class VisibilityOutput {

  private long mId;
  private Component mComponent;
  private final Rect mBounds = new Rect();
  private float mVisibleHeightRatio;
  private float mVisibleWidthRatio;
  private EventHandler<VisibleEvent> mVisibleEventHandler;
  private EventHandler<FocusedVisibleEvent> mFocusedEventHandler;
  private EventHandler<UnfocusedVisibleEvent> mUnfocusedEventHandler;
  private EventHandler<FullImpressionVisibleEvent> mFullImpressionEventHandler;
  private EventHandler<InvisibleEvent> mInvisibleEventHandler;

  long getId() {
    return mId;
  }

  void setId(long id) {
    mId = id;
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

  void release() {
    mVisibleHeightRatio = 0;
    mVisibleWidthRatio = 0;
    mComponent = null;
    mVisibleEventHandler = null;
    mFocusedEventHandler = null;
    mUnfocusedEventHandler = null;
    mFullImpressionEventHandler = null;
    mInvisibleEventHandler = null;

    mBounds.setEmpty();
  }
}
