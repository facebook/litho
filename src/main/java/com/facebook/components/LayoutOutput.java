/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import android.graphics.Rect;
import android.support.annotation.IntDef;

import com.facebook.litho.displaylist.DisplayList;

import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;

/**
 * The output of a layout pass for a given {@link Component}. It's used by
 * {@link MountState} to mount a component.
 */
class LayoutOutput implements Cloneable {
  public static final int TYPE_CONTENT = 0;
  public static final int TYPE_BACKGROUND = 1;
  public static final int TYPE_FOREGROUND = 2;
  public static final int TYPE_HOST = 3;
  public static final int TYPE_BORDER = 4;

  @IntDef({TYPE_CONTENT, TYPE_BACKGROUND, TYPE_FOREGROUND, TYPE_HOST, TYPE_BORDER})
  @Retention(RetentionPolicy.SOURCE)
  @interface LayoutOutputType {}

  public static final int STATE_UNKNOWN = 0;
  public static final int STATE_UPDATED = 1;
  public static final int STATE_DIRTY = 2;

  @IntDef({STATE_UPDATED, STATE_UNKNOWN, STATE_DIRTY})
  @Retention(RetentionPolicy.SOURCE)
  @interface UpdateState {}

  private NodeInfo mNodeInfo;
  private ViewNodeInfo mViewNodeInfo;
  private long mId;
  private Component<?> mComponent;
  private final Rect mBounds = new Rect();
  private int mHostTranslationX;
  private int mHostTranslationY;
  private int mFlags;
  private long mHostMarker;

  private int mUpdateState;
  private int mImportantForAccessibility;
  private DisplayList mDisplayList;

  public LayoutOutput() {
    mUpdateState = STATE_UNKNOWN;
    mImportantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_AUTO;
    mHostMarker = -1L;
  }

  Component<?> getComponent() {
    return mComponent;
  }

  void setComponent(Component<?> component) {
    mComponent = component;
  }

  void getMountBounds(Rect outRect) {
    outRect.left = mBounds.left - mHostTranslationX;
    outRect.top = mBounds.top - mHostTranslationY;
    outRect.right = mBounds.right - mHostTranslationX;
    outRect.bottom = mBounds.bottom - mHostTranslationY;
  }

  Rect getBounds() {
    return mBounds;
  }

  void setBounds(int l, int t, int r, int b) {
    mBounds.set(l, t, r, b);
  }

  void setHostTranslationX(int hostTranslationX) {
    mHostTranslationX = hostTranslationX;
  }

  void setHostTranslationY(int hostTranslationY) {
    mHostTranslationY = hostTranslationY;
  }

  int getFlags() {
    return mFlags;
  }

  void setFlags(int flags) {
    mFlags = flags;
  }

  /**
   * Returns the id of the LayoutOutput that represents the host of this LayoutOutput.
   */
  long getHostMarker() {
    return mHostMarker;
  }

  /**
   * hostMarker is the id of the LayoutOutput that represents the host of this LayoutOutput.
   */
  void setHostMarker(long hostMarker) {
    mHostMarker = hostMarker;
  }

  long getId() {
    return mId;
  }

  void setId(long id) {
    mId = id;
  }

  void setNodeInfo(NodeInfo nodeInfo) {
    if (mNodeInfo != null) {
      throw new IllegalStateException("NodeInfo set more than once on the same LayoutOutput.");
    } else if (nodeInfo != null) {
      mNodeInfo = nodeInfo.acquireRef();
    }
  }

  NodeInfo getNodeInfo() {
    return mNodeInfo;
  }

  public void setUpdateState(@UpdateState int state) {
    mUpdateState = state;
  }

  public @UpdateState int getUpdateState() {
    return mUpdateState;
  }

  public int getImportantForAccessibility() {
    return mImportantForAccessibility;
  }

  public void setImportantForAccessibility(int importantForAccessibility) {
    mImportantForAccessibility = importantForAccessibility;
  }

  public DisplayList getDisplayList() {
    return mDisplayList;
  }

  public void setDisplayList(DisplayList displayList) {
    mDisplayList = displayList;
  }

  void setViewNodeInfo(ViewNodeInfo viewNodeInfo) {
    if (mViewNodeInfo != null) {
      throw new IllegalStateException("Try to set a new ViewNodeInfo in a LayoutOutput that" +
          " is already initialized with one.");
    }
    mViewNodeInfo = viewNodeInfo.acquireRef();
  }

  boolean hasViewNodeInfo() {
    return (mViewNodeInfo != null);
  }

  ViewNodeInfo getViewNodeInfo() {
    return mViewNodeInfo;
  }

  void release() {
    if (mComponent != null) {
      mComponent.release();
      mComponent = null;
    }
