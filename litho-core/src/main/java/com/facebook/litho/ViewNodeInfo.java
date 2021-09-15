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

import android.animation.StateListAnimator;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.DrawableRes;
import com.facebook.litho.drawable.DrawableUtils;
import com.facebook.yoga.YogaDirection;
import javax.annotation.Nullable;

/**
 * Additional information passed between {@link LayoutState} and {@link MountState} used on a {@link
 * View}.
 */
class ViewNodeInfo {

  private @Nullable Drawable mBackground;
  private @Nullable Drawable mForeground;
  private Rect mPadding;
  private Rect mTouchBoundsExpansion;
  private YogaDirection mLayoutDirection;
  private @Nullable StateListAnimator mStateListAnimator;
  private @DrawableRes int mStateListAnimatorRes;
  private int mLayoutType = LayerType.LAYER_TYPE_NOT_SET;
  private @Nullable Paint mLayerPaint;

  void setBackground(@Nullable Drawable background) {
    mBackground = background;
  }

  @Nullable
  Drawable getBackground() {
    return mBackground;
  }

  void setForeground(@Nullable Drawable foreground) {
    mForeground = foreground;
  }

  @Nullable
  Drawable getForeground() {
    return mForeground;
  }

  @Nullable
  Rect getPadding() {
    return mPadding;
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
      throw new IllegalStateException("Padding already initialized for this " + "ViewNodeInfo.");
    }

    mPadding = new Rect();
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

  void setExpandedTouchBounds(final LithoLayoutResult result) {

    final int left = result.getTouchExpansionLeft();
    final int top = result.getTouchExpansionTop();
    final int right = result.getTouchExpansionRight();
    final int bottom = result.getTouchExpansionBottom();
    if (left == 0 && top == 0 && right == 0 && bottom == 0) {
      return;
    }

    if (mTouchBoundsExpansion != null) {
      throw new IllegalStateException(
          "ExpandedTouchBounds already initialized for this " + "ViewNodeInfo.");
    }

    mTouchBoundsExpansion = new Rect(left, top, right, bottom);
  }

  @Nullable
  Rect getTouchBoundsExpansion() {
    if (mTouchBoundsExpansion == null) {
      return null;
    }

    return mTouchBoundsExpansion;
  }

  @Nullable
  StateListAnimator getStateListAnimator() {
    return mStateListAnimator;
  }

  void setStateListAnimator(@Nullable StateListAnimator stateListAnimator) {
    mStateListAnimator = stateListAnimator;
  }

  @DrawableRes
  int getStateListAnimatorRes() {
    return mStateListAnimatorRes;
  }

  void setStateListAnimatorRes(@DrawableRes int resId) {
    mStateListAnimatorRes = resId;
  }

  void setLayerType(@LayerType int type, @Nullable Paint paint) {
    mLayoutType = type;
    mLayerPaint = paint;
  }

  @LayerType
  int getLayerType() {
    return mLayoutType;
  }

  public @Nullable Paint getLayoutPaint() {
    return mLayerPaint;
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

    if (!DrawableUtils.isEquivalentTo(mBackground, other.mBackground)) {
      return false;
    }

    if (!DrawableUtils.isEquivalentTo(mForeground, other.mForeground)) {
      return false;
    }

    if (!CommonUtils.equals(mPadding, other.mPadding)) {
      return false;
    }

    if (!CommonUtils.equals(mTouchBoundsExpansion, other.mTouchBoundsExpansion)) {
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
}
