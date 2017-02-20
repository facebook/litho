// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.graphics.Rect;

/**
 * Stores information about a {@link Component} which has registered handlers for
 * {@link VisibleEvent} or {@link InvisibleEvent}. The information is passed to {@link MountState}
 * which then dispatches the appropriate events.
 */
class VisibilityOutput {

  private long mId;
  private Component<?> mComponent;
  private final Rect mBounds = new Rect();
  private EventHandler mVisibleEventHandler;
  private EventHandler mFocusedEventHandler;
  private EventHandler mFullImpressionEventHandler;
  private EventHandler mInvisibleEventHandler;

  long getId() {
    return mId;
  }

  void setId(long id) {
    mId = id;
  }

  Component<?> getComponent() {
    return mComponent;
  }

  void setComponent(Component<?> component) {
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

  void setVisibleEventHandler(EventHandler visibleEventHandler) {
    mVisibleEventHandler = visibleEventHandler;
  }

  EventHandler getVisibleEventHandler() {
    return mVisibleEventHandler;
  }

  void setFocusedEventHandler(EventHandler focusedEventHandler) {
    mFocusedEventHandler = focusedEventHandler;
  }

  EventHandler getFocusedEventHandler() {
    return mFocusedEventHandler;
  }

  void setFullImpressionEventHandler(EventHandler visibleEventHandler) {
    mFullImpressionEventHandler = visibleEventHandler;
  }

  EventHandler getFullImpressionEventHandler() {
    return mFullImpressionEventHandler;
  }

  void setInvisibleEventHandler(EventHandler invisibleEventHandler) {
    mInvisibleEventHandler = invisibleEventHandler;
  }

  EventHandler getInvisibleEventHandler() {
    return mInvisibleEventHandler;
  }

  void release() {
    mComponent = null;
    mVisibleEventHandler = null;
    mFocusedEventHandler = null;
    mFullImpressionEventHandler = null;
    mInvisibleEventHandler = null;

    mBounds.setEmpty();
  }
}
