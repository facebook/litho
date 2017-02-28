// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.graphics.Rect;

/**
 * Stores information about a {@link Component} which is only available when tests are run.
 * TestOutputs are calculated in {@link LayoutState} and transformed into {@link TestItem}s in
 * {@link MountState}.
 */
class TestOutput {
  private String mTestKey;
  private long mHostMarker = -1;
  private long mLayoutOutputId = -1;
  private final Rect mBounds = new Rect();

  String getTestKey() {
    return mTestKey;
  }

  void setTestKey(String testKey) {
    mTestKey = testKey;
  }

  Rect getBounds() {
    return mBounds;
  }

  void setBounds(Rect bounds) {
    mBounds.set(bounds);
  }

  void setBounds(int left, int top, int right, int bottom) {
    mBounds.set(left, top, right, bottom);
  }

  void setHostMarker(long hostMarker) {
    mHostMarker = hostMarker;
  }

  long getHostMarker() {
    return mHostMarker;
  }

  long getLayoutOutputId() {
    return mLayoutOutputId;
  }

  void setLayoutOutputId(long layoutOutputId) {
    mLayoutOutputId = layoutOutputId;
  }

  void release() {
    mTestKey = null;
    mLayoutOutputId = -1;
    mHostMarker = -1;
    mBounds.setEmpty();
  }
}
