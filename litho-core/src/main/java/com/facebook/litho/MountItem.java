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

  static final int FLAG_DUPLICATE_PARENT_STATE = 1 << 0;
  static final int FLAG_DISABLE_TOUCHABLE = 1 << 1;
  static final int FLAG_VIEW_CLICKABLE = 1 << 2;
  static final int FLAG_VIEW_LONG_CLICKABLE = 1 << 3;
  static final int FLAG_VIEW_FOCUSABLE = 1 << 4;
  static final int FLAG_VIEW_ENABLED = 1 << 5;
  static final int FLAG_MATCH_HOST_BOUNDS = 1 << 6;
  static final int FLAG_VIEW_SELECTED = 1 << 7;
  static final int FLAG_ONLY_SUPPORT_DISAPPEARING = 1 << 8;

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

  void init(LayoutOutput layoutOutput, MountItem mountItem) {
    init(
        layoutOutput.getComponent(),
        mountItem.getHost(),
        mountItem.getContent(),
        layoutOutput,
        mountItem.getDisplayListDrawable());
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
    mComponent = component;
    mContent = content;
    mHost = host;
    mLayoutFlags = layoutFlags;
    mImportantForAccessibility = importantForAccessibility;
    mDisplayListDrawable = displayListDrawable;
    mTransitionKey = transitionKey;

    if (mNodeInfo != null) {
      mNodeInfo.release();
      mNodeInfo = null;
    }

    if (nodeInfo != null) {
      mNodeInfo = nodeInfo.acquireRef();
    }

    if (mViewNodeInfo != null) {
      mViewNodeInfo.release();
      mViewNodeInfo = null;
    }

    if (viewNodeInfo != null) {
      mViewNodeInfo = viewNodeInfo.acquireRef();
    }

    if (mContent instanceof View) {
      final View view = (View) mContent;

      if (view.isClickable()) {
        mLayoutFlags |= FLAG_VIEW_CLICKABLE;
      }

      if (view.isLongClickable()) {
        mLayoutFlags |= FLAG_VIEW_LONG_CLICKABLE;
      }

      if (view.isFocusable()) {
        mLayoutFlags |= FLAG_VIEW_FOCUSABLE;
      }

      if (view.isEnabled()) {
        mLayoutFlags |= FLAG_VIEW_ENABLED;
      }

      if (view.isSelected()) {
        mLayoutFlags |= FLAG_VIEW_SELECTED;
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
      mLayoutFlags |= FLAG_ONLY_SUPPORT_DISAPPEARING;
    } else {
      mLayoutFlags &= (~FLAG_ONLY_SUPPORT_DISAPPEARING);
    }
  }

  boolean onlySupportsDisappearing() {
    return (mLayoutFlags & FLAG_ONLY_SUPPORT_DISAPPEARING) != 0;
  }

  void release(ComponentContext context) {
    ComponentsPools.release(context, mComponent, mContent);

    if (mDisplayListDrawable != null) {
      ComponentsPools.release(mDisplayListDrawable);
      mDisplayListDrawable = null;
    }

    if (mNodeInfo != null) {
      mNodeInfo.release();
      mNodeInfo = null;
    }

    if (mViewNodeInfo != null) {
      mViewNodeInfo.release();
      mViewNodeInfo = null;
    }

    mComponent = null;
    mHost = null;
    mContent = null;
    mLayoutFlags = 0;
    mIsBound = false;
    mImportantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_AUTO;
    mTransitionKey = null;
  }

  static boolean isDuplicateParentState(int flags) {
    return (flags & FLAG_DUPLICATE_PARENT_STATE) == FLAG_DUPLICATE_PARENT_STATE;
  }

  static boolean isTouchableDisabled(int flags) {
    return (flags & FLAG_DISABLE_TOUCHABLE) == FLAG_DISABLE_TOUCHABLE;
  }

  /**
   * @return Whether the view associated with this MountItem is clickable.
   */
  static boolean isViewClickable(int flags) {
    return (flags & FLAG_VIEW_CLICKABLE) == FLAG_VIEW_CLICKABLE;
  }

  /**
   * @return Whether the view associated with this MountItem is long clickable.
   */
  static boolean isViewLongClickable(int flags) {
    return (flags & FLAG_VIEW_LONG_CLICKABLE) == FLAG_VIEW_LONG_CLICKABLE;
  }

  /**
   * @return Whether the view associated with this MountItem is setFocusable.
   */
  static boolean isViewFocusable(int flags) {
    return (flags & FLAG_VIEW_FOCUSABLE) == FLAG_VIEW_FOCUSABLE;
  }

  /**
   * @return Whether the view associated with this MountItem is setEnabled.
   */
  static boolean isViewEnabled(int flags) {
    return (flags & FLAG_VIEW_ENABLED) == FLAG_VIEW_ENABLED;
  }

  /** @return Whether the view associated with this MountItem is setSelected. */
  static boolean isViewSelected(int flags) {
    return (flags & FLAG_VIEW_SELECTED) == FLAG_VIEW_SELECTED;
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
