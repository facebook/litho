/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.atomic.AtomicInteger;

import android.graphics.Rect;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;

import com.facebook.litho.displaylist.DisplayList;

import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;

/**
 * The output of a layout pass for a given {@link Component}. It's used by
 * {@link MountState} to mount a component.
 */
class LayoutOutput implements Cloneable, AnimatableItem {
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
  public @interface UpdateState {}

  private NodeInfo mNodeInfo;
  private ViewNodeInfo mViewNodeInfo;
  private long mId;
  private Component<?> mComponent;
  private final Rect mBounds = new Rect();
  private int mHostTranslationX;
  private int mHostTranslationY;
  private int mFlags;
  private long mHostMarker;
  private AtomicInteger mRefCount = new AtomicInteger(0);

  private int mUpdateState;
  private int mImportantForAccessibility;
  private @Nullable DisplayListContainer mDisplayListContainer;

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

  @Override
  public Rect getBounds() {
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

  @UpdateState
  public int getUpdateState() {
    return mUpdateState;
  }

  public int getImportantForAccessibility() {
    return mImportantForAccessibility;
  }

  public void setImportantForAccessibility(int importantForAccessibility) {
    mImportantForAccessibility = importantForAccessibility;
  }

  void initDisplayListContainer(boolean canCacheDrawingDisplayList) {
    if (mDisplayListContainer != null) {
      throw new IllegalStateException("Trying to init displaylistcontainer but it already exists");
    }
    mDisplayListContainer = ComponentsPools.acquireDisplayListContainer();
    mDisplayListContainer.setCacheDrawingDisplayListsEnabled(canCacheDrawingDisplayList);
  }

  void setDisplayListContainer(DisplayListContainer displayListContainer) {
    if (mDisplayListContainer != null) {
      mDisplayListContainer.release();
    }
    mDisplayListContainer = displayListContainer;
  }

  @Nullable DisplayListContainer getDisplayListContainer() {
    return mDisplayListContainer;
  }

  boolean hasValidDisplayList() {
    if (mDisplayListContainer == null) {
      throw new IllegalStateException(
          "Trying to check displaylist validity when generating displaylist is not supported " +
              "for this output");
    }
    return mDisplayListContainer.hasDisplayList()
        && mDisplayListContainer.getDisplayList().isValid();
  }

  @Nullable DisplayList getDisplayList() {
    if (mDisplayListContainer == null) {
      throw new IllegalStateException(
          "Trying to get displaylist when generating displaylist is not supported for this output");
    }
    return mDisplayListContainer.getDisplayList();
  }

  void setDisplayList(DisplayList displayList) {
    if (mDisplayListContainer == null) {
      throw new IllegalStateException(
          "Trying to set displaylist when generating displaylist is not supported for this output");
    }
    mDisplayListContainer.setDisplayList(displayList);
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

  String getTransitionKey() {
    if (mViewNodeInfo == null) {
      return null;
    }

    final String transitionKey = mViewNodeInfo.getTransitionKey();
    if (transitionKey == null || transitionKey.length() == 0) {
      return null;
    }

    return transitionKey;
  }

  void acquire() {
    if (mRefCount.getAndSet(1) != 0) {
      throw new RuntimeException(
          "Tried to acquire LayoutOutput that already had a non-zero ref count!");
    }
  }

  public void incrementRefCount() {
    if (mRefCount.getAndIncrement() < 1) {
      throw new RuntimeException(
          "Tried to increment ref count of a released LayoutOutput!");
    }
  }

  public void decrementRefCount() {
    if (mRefCount.decrementAndGet() < 1) {
      throw new RuntimeException("Decremented external ref count below 1!");
    }
  }

  void release() {
    if (mRefCount.getAndSet(0) != 1) {
      throw new RuntimeException(
          "Trying to release LayoutOutput that still has external references!");
    }
    if (mComponent != null) {
      mComponent.release();
      mComponent = null;
    }
    if (mNodeInfo != null) {
      mNodeInfo.release();
      mNodeInfo = null;
    }
    if (mViewNodeInfo != null) {
      mViewNodeInfo.release();
      mViewNodeInfo = null;
    }
    if (mDisplayListContainer != null) {
      mDisplayListContainer.release();
      mDisplayListContainer = null;
    }
    mBounds.setEmpty();
    mHostTranslationX = 0;
    mHostTranslationY = 0;
    mFlags = 0;
    mHostMarker = -1L;
    mUpdateState = STATE_UNKNOWN;
    mImportantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_AUTO;
  }
}
