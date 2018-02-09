/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;

import android.graphics.Rect;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import com.facebook.litho.displaylist.DisplayList;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.atomic.AtomicInteger;

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

  private @Nullable NodeInfo mNodeInfo;
  private ViewNodeInfo mViewNodeInfo;
  private long mId;
  private Component mComponent;
  private final Rect mBounds = new Rect();
  private int mHostTranslationX;
  private int mHostTranslationY;
  private int mFlags;
  private long mHostMarker;
  private AtomicInteger mRefCount = new AtomicInteger(0);

  private int mUpdateState;
  private int mImportantForAccessibility;
  private @Nullable DisplayListContainer mDisplayListContainer;

  private @Nullable String mTransitionKey;

  public LayoutOutput() {
    mUpdateState = STATE_UNKNOWN;
    mImportantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_AUTO;
    mHostMarker = -1L;
  }

  Component getComponent() {
    return mComponent;
  }

  void setComponent(Component component) {
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

  @Override
  public float getScale() {
    return mNodeInfo != null ? mNodeInfo.getScale() : 1;
  }

  @Override
  public float getAlpha() {
    return mNodeInfo != null ? mNodeInfo.getAlpha() : 1;
  }

  @Override
  public boolean isScaleSet() {
    return mNodeInfo != null && mNodeInfo.isScaleSet();
  }

  @Override
  public boolean isAlphaSet() {
    return mNodeInfo != null && mNodeInfo.isAlphaSet();
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

  void initDisplayListContainer(String name, boolean canCacheDrawingDisplayList) {
    if (mDisplayListContainer != null) {
      throw new IllegalStateException("Trying to init displaylistcontainer but it already exists");
    }
    mDisplayListContainer = ComponentsPools.acquireDisplayListContainer();
    mDisplayListContainer.init(name, canCacheDrawingDisplayList);
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

  /*
   * Usually if displaylists are supported, it means we initialized the displaylist container.
   * However, there are cases when we need to query some displaylist data when this output has been
   * released. In such cases we need to perform this check.
   */
  boolean hasDisplayListContainer() {
    return mDisplayListContainer != null;
  }

  boolean hasValidDisplayList() {
    if (mDisplayListContainer == null) {
      throw new IllegalStateException(
          "Trying to check displaylist validity when generating displaylist is not supported " +
              "for this output");
    }
    return mDisplayListContainer.hasValidDisplayList();
  }

  @Nullable DisplayList getDisplayList() {
    if (mDisplayListContainer == null) {
      throw new IllegalStateException(
          "Trying to get displaylist when generating displaylist is not supported for this output");
    }
    return mDisplayListContainer.getDisplayList();
  }

  void setDisplayList(DisplayList displayList) {
    // We might have recycled the container already so we need to have this check.
    if (mDisplayListContainer != null) {
      mDisplayListContainer.setDisplayList(displayList);
    }
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

  void setTransitionKey(String transitionKey) {
    mTransitionKey = transitionKey;
  }

  @Nullable
  String getTransitionKey() {
    return mTransitionKey;
  }

  void acquire() {
    if (mRefCount.getAndSet(1) != 0) {
      throw new RuntimeException(
          "Tried to acquire a LayoutOutput that already had a non-zero ref count!");
    }
  }

  public LayoutOutput acquireRef() {
    if (mRefCount.getAndIncrement() < 1) {
      throw new RuntimeException("Tried to acquire a reference to a released LayoutOutput!");
    }

    return this;
  }

  void release() {
    final int count = mRefCount.decrementAndGet();
    if (count < 0) {
      throw new IllegalStateException("Trying to release a recycled LayoutOutput.");
    } else if (count > 0) {
      return;
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
    mTransitionKey = null;

    ComponentsPools.release(this);
  }
}
