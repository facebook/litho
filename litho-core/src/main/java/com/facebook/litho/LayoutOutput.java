/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;

import android.graphics.Rect;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The output of a layout pass for a given {@link Component}. It's used by
 * {@link MountState} to mount a component.
 */
class LayoutOutput implements Cloneable, AnimatableItem {
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
  private long mHostMarker = -1L;
  private int mIndex;

  private int mUpdateState = STATE_UNKNOWN;
  private int mImportantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_AUTO;
  private int mOrientation;

  private @Nullable TransitionId mTransitionId;

  Component getComponent() {
    return mComponent;
  }

  void setComponent(Component component) {
    if (component == null) {
      throw new RuntimeException("Trying to set a null Component on a LayoutOutput!");
    }

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
  public float getRotation() {
    return mNodeInfo != null ? mNodeInfo.getRotation() : 0;
  }

  @Override
  public float getRotationX() {
    return mNodeInfo != null ? mNodeInfo.getRotationX() : 0;
  }

  @Override
  public float getRotationY() {
    return mNodeInfo != null ? mNodeInfo.getRotationY() : 0;
  }

  @Override
  public boolean isScaleSet() {
    return mNodeInfo != null && mNodeInfo.isScaleSet();
  }

  @Override
  public boolean isAlphaSet() {
    return mNodeInfo != null && mNodeInfo.isAlphaSet();
  }

  @Override
  public boolean isRotationSet() {
    return mNodeInfo != null && mNodeInfo.isRotationSet();
  }

  @Override
  public boolean isRotationXSet() {
    return mNodeInfo != null && mNodeInfo.isRotationXSet();
  }

  @Override
  public boolean isRotationYSet() {
    return mNodeInfo != null && mNodeInfo.isRotationYSet();
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
   * Returns the id of the LayoutOutput that represents the host of this LayoutOutput. This host may
   * be phantom, meaning that the mount content that represents this LayoutOutput may be hosted
   * inside one of higher level hosts {@see MountState#getActualComponentHost()}
   */
  long getHostMarker() {
    return mHostMarker;
  }

  /**
   * hostMarker is the id of the LayoutOutput that represents the host of this LayoutOutput. This
   * host may be phantom, meaning that the mount content that represents this LayoutOutput may be
   * hosted inside one of higher level hosts {@see MountState#getActualComponentHost()}
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

  int getIndex() {
    return mIndex;
  }

  void setIndex(int index) {
    mIndex = index;
  }

  void setNodeInfo(NodeInfo nodeInfo) {
    if (mNodeInfo != null) {
      throw new IllegalStateException("NodeInfo set more than once on the same LayoutOutput.");
    } else if (nodeInfo != null) {
      mNodeInfo = nodeInfo;
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

  int getOrientation() {
    return mOrientation;
  }

  void setOrientation(int orientation) {
    mOrientation = orientation;
  }

  void setViewNodeInfo(ViewNodeInfo viewNodeInfo) {
    if (mViewNodeInfo != null) {
      throw new IllegalStateException("Try to set a new ViewNodeInfo in a LayoutOutput that" +
          " is already initialized with one.");
    }
    mViewNodeInfo = viewNodeInfo;
  }

  boolean hasViewNodeInfo() {
    return (mViewNodeInfo != null);
  }

  ViewNodeInfo getViewNodeInfo() {
    return mViewNodeInfo;
  }

  public void setTransitionId(@Nullable TransitionId transitionId) {
    this.mTransitionId = transitionId;
  }

  @Nullable
  public TransitionId getTransitionId() {
    return mTransitionId;
  }
}
