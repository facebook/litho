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

import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO;

import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.view.View;
import com.facebook.litho.displaylist.DisplayList;

/**
 * Represents a mounted UI element in a {@link MountState}. It holds a
 * key and a content instance which might be any type of UI element
 * supported by the framework e.g. {@link Drawable}.
 */
class MountItem {

  static final int LAYOUT_FLAG_DUPLICATE_PARENT_STATE = 1 << 0;
  static final int LAYOUT_FLAG_DISABLE_TOUCHABLE = 1 << 1;
  static final int LAYOUT_FLAG_MATCH_HOST_BOUNDS = 1 << 2;
  static final int LAYOUT_FLAG_ONLY_SUPPORT_DISAPPEARING = 1 << 3;

  private static final int FLAG_VIEW_CLICKABLE = 1 << 0;
  private static final int FLAG_VIEW_LONG_CLICKABLE = 1 << 1;
  private static final int FLAG_VIEW_FOCUSABLE = 1 << 2;
  private static final int FLAG_VIEW_ENABLED = 1 << 3;
  private static final int FLAG_VIEW_SELECTED = 1 << 4;

  private NodeInfo mNodeInfo;
  private ViewNodeInfo mViewNodeInfo;
  private Component mComponent;
  private Object mContent;
  private ComponentHost mHost;
  private boolean mIsBound;
  private int mImportantForAccessibility;
  private @Nullable DisplayListDrawable mDisplayListDrawable;
  private @Nullable String mTransitionKey;

  // ComponentHost flags defined in the LayoutOutput specifying
  // the behaviour of this item when mounted.
  private int mLayoutFlags;

  // Flags that track view-related behaviour of mounted view content.
  private int mMountViewFlags;

  /**
   * Call this method when assigning a new {@link LayoutOutput} to an existing MountItem. In this
   * case we don't want to update mMountViewFlags since those flags are only used to determine the
   * initial state of the view content, which we will have already done in init(). If it is done
   * again now some of the values may be wrong (e.g. the Litho framework may add a click listener to
   * a view that was not originally clickable.
   */
  void update(LayoutOutput layoutOutput) {
    mComponent = layoutOutput.getComponent();
    mLayoutFlags = layoutOutput.getFlags();
    mImportantForAccessibility = layoutOutput.getImportantForAccessibility();
    mDisplayListDrawable =
        acquireDisplayListDrawableIfNeeded(
            mContent, layoutOutput.getDisplayListContainer(), mDisplayListDrawable);
    mTransitionKey = layoutOutput.getTransitionKey();

    releaseNodeInfos();

    if (layoutOutput.getNodeInfo() != null) {
      mNodeInfo = layoutOutput.getNodeInfo().acquireRef();
    }

    if (layoutOutput.getViewNodeInfo() != null) {
      mViewNodeInfo = layoutOutput.getViewNodeInfo().acquireRef();
    }
  }

  void init(
      Component component,
      ComponentHost host,
      Object content,
      LayoutOutput layoutOutput,
      @Nullable DisplayListDrawable displayListDrawable) {
    displayListDrawable =
        acquireDisplayListDrawableIfNeeded(
            content, layoutOutput.getDisplayListContainer(), displayListDrawable);
    init(
        component,
        host,
        content,
        layoutOutput.getNodeInfo(),
        layoutOutput.getViewNodeInfo(),
        displayListDrawable,
        layoutOutput.getFlags(),
        layoutOutput.getImportantForAccessibility(),
        layoutOutput.getTransitionKey());
  }

  void init(
      Component component,
      ComponentHost host,
      Object content,
      NodeInfo nodeInfo,
      ViewNodeInfo viewNodeInfo,
      @Nullable DisplayListDrawable displayListDrawable,
      int layoutFlags,
      int importantForAccessibility,
      String transitionKey) {
    if (mHost != null) {
      throw new RuntimeException("Calling init() on a MountItem that has not been released!");
    }

    mComponent = component;
    mContent = content;
    mHost = host;
    mLayoutFlags = layoutFlags;
    mImportantForAccessibility = importantForAccessibility;
    mDisplayListDrawable = displayListDrawable;
    mTransitionKey = transitionKey;

    if (nodeInfo != null) {
      mNodeInfo = nodeInfo.acquireRef();
    }

    if (viewNodeInfo != null) {
      mViewNodeInfo = viewNodeInfo.acquireRef();
    }

    if (mContent instanceof View) {
      final View view = (View) mContent;

      if (view.isClickable()) {
        mMountViewFlags |= FLAG_VIEW_CLICKABLE;
      }

      if (view.isLongClickable()) {
        mMountViewFlags |= FLAG_VIEW_LONG_CLICKABLE;
      }

      if (view.isFocusable()) {
        mMountViewFlags |= FLAG_VIEW_FOCUSABLE;
      }

      if (view.isEnabled()) {
        mMountViewFlags |= FLAG_VIEW_ENABLED;
      }

      if (view.isSelected()) {
        mMountViewFlags |= FLAG_VIEW_SELECTED;
      }
    }
  }

  private static @Nullable DisplayListDrawable acquireDisplayListDrawableIfNeeded(
      Object content,
      @Nullable DisplayListContainer layoutOutputDisplayListContainer,
      @Nullable DisplayListDrawable mountItemDisplayListDrawable) {

    if (layoutOutputDisplayListContainer == null) {
      // If we do not have DisplayListContainer it would mean that we do not support generating
      // displaylists, hence this mount item should not have DisplayListDrawable.
      if (mountItemDisplayListDrawable != null) {
        ComponentsPools.release(mountItemDisplayListDrawable);
      }
      return null;
    }

    final DisplayList displayList = layoutOutputDisplayListContainer.getDisplayList();
    if (mountItemDisplayListDrawable == null
        && (layoutOutputDisplayListContainer.canCacheDrawingDisplayLists()
            || displayList != null)) {
      mountItemDisplayListDrawable =
          ComponentsPools.acquireDisplayListDrawable(
              (Drawable) content, layoutOutputDisplayListContainer);
    } else if (mountItemDisplayListDrawable != null) {
      mountItemDisplayListDrawable.setWrappedDrawable(
          (Drawable) content, layoutOutputDisplayListContainer);
    }

    if (displayList != null && mountItemDisplayListDrawable != null) {
      mountItemDisplayListDrawable.suppressInvalidations(true);
    }

    return mountItemDisplayListDrawable;
  }

  @Nullable
  Component getComponent() {
    return mComponent;
  }

  ComponentHost getHost() {
    return mHost;
  }

  Object getContent() {
    return mContent;
  }

  int getLayoutFlags() {
    return mLayoutFlags;
  }

  int getImportantForAccessibility() {
    return mImportantForAccessibility;
  }

  NodeInfo getNodeInfo() {
    return mNodeInfo;
  }

  ViewNodeInfo getViewNodeInfo() {
    return mViewNodeInfo;
  }

  @Nullable
  String getTransitionKey() {
    return mTransitionKey;
  }

  boolean hasTransitionKey() {
    return mTransitionKey != null;
  }

  boolean isAccessible() {
    if (mComponent == null) {
      return false;
    }

    if (mImportantForAccessibility == IMPORTANT_FOR_ACCESSIBILITY_NO) {
      return false;
    }

    return (mNodeInfo != null && mNodeInfo.needsAccessibilityDelegate())
        || mComponent.implementsAccessibility();
  }

  void setOnlySupportsDisappearing(boolean onlySupportsDisappearing) {
    if (onlySupportsDisappearing) {
      mLayoutFlags |= LAYOUT_FLAG_ONLY_SUPPORT_DISAPPEARING;
    } else {
      mLayoutFlags &= (~LAYOUT_FLAG_ONLY_SUPPORT_DISAPPEARING);
    }
  }

  boolean onlySupportsDisappearing() {
    return (mLayoutFlags & LAYOUT_FLAG_ONLY_SUPPORT_DISAPPEARING) != 0;
  }

  void release(ComponentContext context) {
    ComponentsPools.release(context, mComponent, mContent);

    if (mDisplayListDrawable != null) {
      ComponentsPools.release(mDisplayListDrawable);
      mDisplayListDrawable = null;
    }

    releaseNodeInfos();

    mComponent = null;
    mHost = null;
    mContent = null;
    mLayoutFlags = 0;
    mMountViewFlags = 0;
    mIsBound = false;
    mImportantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_AUTO;
    mTransitionKey = null;
  }

  private void releaseNodeInfos() {
    if (mNodeInfo != null) {
      mNodeInfo.release();
      mNodeInfo = null;
    }

    if (mViewNodeInfo != null) {
      mViewNodeInfo.release();
      mViewNodeInfo = null;
    }
  }

  static boolean isDuplicateParentState(int flags) {
    return (flags & LAYOUT_FLAG_DUPLICATE_PARENT_STATE) == LAYOUT_FLAG_DUPLICATE_PARENT_STATE;
  }

  static boolean isTouchableDisabled(int flags) {
    return (flags & LAYOUT_FLAG_DISABLE_TOUCHABLE) == LAYOUT_FLAG_DISABLE_TOUCHABLE;
  }

  /** @return Whether the view associated with this MountItem is clickable. */
  boolean isViewClickable() {
    return (mMountViewFlags & FLAG_VIEW_CLICKABLE) == FLAG_VIEW_CLICKABLE;
  }

  /** @return Whether the view associated with this MountItem is long clickable. */
  boolean isViewLongClickable() {
    return (mMountViewFlags & FLAG_VIEW_LONG_CLICKABLE) == FLAG_VIEW_LONG_CLICKABLE;
  }

  /** @return Whether the view associated with this MountItem is setFocusable. */
  boolean isViewFocusable() {
    return (mMountViewFlags & FLAG_VIEW_FOCUSABLE) == FLAG_VIEW_FOCUSABLE;
  }

  /** @return Whether the view associated with this MountItem is setEnabled. */
  boolean isViewEnabled() {
    return (mMountViewFlags & FLAG_VIEW_ENABLED) == FLAG_VIEW_ENABLED;
  }

  /** @return Whether the view associated with this MountItem is setSelected. */
  boolean isViewSelected() {
    return (mMountViewFlags & FLAG_VIEW_SELECTED) == FLAG_VIEW_SELECTED;
  }

  /**
   * @return Whether this MountItem is currently bound. A bound mount item is a Mount item that has
   *     been mounted and is currently active on screen.
   */
  boolean isBound() {
    return mIsBound;
  }

  /**
   * Sets whether this MountItem is currently bound.
   */
  void setIsBound(boolean bound) {
    mIsBound = bound;
  }

  @Nullable
  DisplayListDrawable getDisplayListDrawable() {
    return mDisplayListDrawable;
  }
}
