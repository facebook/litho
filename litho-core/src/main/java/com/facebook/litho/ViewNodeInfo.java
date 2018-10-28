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

import android.animation.StateListAnimator;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.view.View;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.reference.Reference;
import com.facebook.yoga.YogaDirection;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

/**
 * Additional information passed between {@link LayoutState} and {@link MountState}
 * used on a {@link View}.
 */
class ViewNodeInfo {

  private final AtomicInteger mReferenceCount = new AtomicInteger(0);

  private Reference<Drawable> mBackground;
  private Drawable mForeground;
  private Rect mPadding;
  private Rect mExpandedTouchBounds;
  private YogaDirection mLayoutDirection;
  private @Nullable StateListAnimator mStateListAnimator;
  private @DrawableRes int mStateListAnimatorRes;

  void setBackground(Reference<? extends Drawable> background) {
    mBackground = (Reference<Drawable>) background;
  }

  Reference<Drawable> getBackground() {
    return mBackground;
  }

  void setForeground(Drawable foreground) {
    mForeground = foreground;
  }

  Drawable getForeground() {
    return mForeground;
  }

  int getPaddingLeft() {
    return (mPadding != null) ? mPadding.left : 0;
  }

  int getPaddingTop() {
    return (mPadding != null) ? mPadding.top : 0;
  }

  int getPaddingRight() {
    return (mPadding != null) ? mPadding.right : 0;
  }

  int getPaddingBottom() {
    return (mPadding != null) ? mPadding.bottom : 0;
  }

  void setPadding(int l, int t, int r, int b) {
    if (mPadding != null) {
      throw new IllegalStateException("Padding already initialized for this " +
          "ViewNodeInfo.");
    }

    mPadding = ComponentsPools.acquireRect();
    mPadding.set(l, t, r, b);
  }

  boolean hasPadding() {
    return (mPadding != null);
  }

  void setLayoutDirection(YogaDirection layoutDirection) {
    mLayoutDirection = layoutDirection;
  }

  YogaDirection getLayoutDirection() {
    return mLayoutDirection;
  }

  void setExpandedTouchBounds(InternalNode node, int l, int t, int r, int b) {
    if (!node.hasTouchExpansion()) {
      return;
    }

    final int touchExpansionLeft = node.getTouchExpansionLeft();
    final int touchExpansionTop = node.getTouchExpansionTop();
    final int touchExpansionRight = node.getTouchExpansionRight();
    final int touchExpansionBottom = node.getTouchExpansionBottom();
    if (touchExpansionLeft == 0
        && touchExpansionTop == 0
        && touchExpansionRight == 0
        && touchExpansionBottom == 0) {
      return;
    }

    if (mExpandedTouchBounds != null) {
      throw new IllegalStateException("ExpandedTouchBounds already initialized for this " +
          "ViewNodeInfo.");
    }

    mExpandedTouchBounds = ComponentsPools.acquireRect();
    mExpandedTouchBounds.set(
        l - touchExpansionLeft,
        t - touchExpansionTop,
        r + touchExpansionRight,
        b + touchExpansionBottom);
  }

  @Nullable
  Rect getExpandedTouchBounds() {
    if (mExpandedTouchBounds == null || mExpandedTouchBounds.isEmpty()) {
      return null;
    }

    return mExpandedTouchBounds;
  }

  @Nullable
  StateListAnimator getStateListAnimator() {
    return mStateListAnimator;
  }

  void setStateListAnimator(StateListAnimator stateListAnimator) {
    mStateListAnimator = stateListAnimator;
  }

  @DrawableRes
  int getStateListAnimatorRes() {
    return mStateListAnimatorRes;
  }

  void setStateListAnimatorRes(@DrawableRes int resId) {
    mStateListAnimatorRes = resId;
  }

  /**
   * Checks if this ViewNodeInfo is equal to the {@param other}
   *
   * @param other the other ViewNodeInfo
   * @return {@code true} iff this NodeInfo is equal to the {@param other}.
   */
  public boolean isEquivalentTo(ViewNodeInfo other) {
    if (this == other) {
      return true;
    }

    if (other == null) {
      return false;
    }

    // TODO: (T33421916) We need compare Drawables more accurately
    if (!CommonUtils.equals(mBackground, other.mBackground)) {
      return false;
    }

    if (!CommonUtils.equals(mForeground, other.mForeground)) {
      return false;
    }

    if (!CommonUtils.equals(mPadding, other.mPadding)) {
      return false;
    }

    if (!CommonUtils.equals(mExpandedTouchBounds, other.mExpandedTouchBounds)) {
      return false;
    }

    if (!CommonUtils.equals(mLayoutDirection, other.mLayoutDirection)) {
      return false;
    }

    if (mStateListAnimatorRes != other.mStateListAnimatorRes) {
      return false;
    }

    // TODO: (T33421916) We need compare StateListAnimators more accurately
    if (!CommonUtils.equals(mStateListAnimator, other.mStateListAnimator)) {
      return false;
    }

    return true;
  }

  static ViewNodeInfo acquire() {
    final ViewNodeInfo viewNodeInfo = ComponentsPools.acquireViewNodeInfo();

    if (viewNodeInfo.mReferenceCount.getAndSet(1) != 0) {
      throw new IllegalStateException("The ViewNodeInfo reference acquired from the pool " +
          " wasn't correctly released.");
    }

    return viewNodeInfo;
  }

  ViewNodeInfo acquireRef() {
    if (mReferenceCount.getAndIncrement() < 1) {
      throw new IllegalStateException("The ViewNodeInfo being acquired" +
          " wasn't correctly initialized.");
    }

    return this;
  }

  void release() {
    final int count = mReferenceCount.decrementAndGet();
    if (count < 0) {
      throw new IllegalStateException("Trying to release a recycled ViewNodeInfo.");
    } else if (count > 0) {
      return;
    }

    if (ComponentsConfiguration.disablePools) {
      return;
    }

    mBackground = null;
    mForeground = null;
    mLayoutDirection = YogaDirection.INHERIT;
    mStateListAnimator = null;

    if (mPadding != null) {
      ComponentsPools.release(mPadding);
      mPadding = null;
    }

    if (mExpandedTouchBounds != null) {
      ComponentsPools.release(mExpandedTouchBounds);
      mExpandedTouchBounds = null;
    }

    ComponentsPools.release(this);
  }
}
