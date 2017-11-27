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
  static final int FLAG_IS_TRANSITION_KEY_SET = 1 << 6;

  private NodeInfo mNodeInfo;
  private ViewNodeInfo mViewNodeInfo;
  private Component<?> mComponent;
  private Object mContent;
  private ComponentHost mHost;
  private boolean mIsBound;
  private int mImportantForAccessibility;
  private DisplayListContainer mDisplayListContainer;
  private DisplayListDrawable mDisplayListDrawable;

  // ComponentHost flags defined in the LayoutOutput specifying
  // the behaviour of this item when mounted.
  private int mFlags;

  void init(
      Component<?> component,
      MountItem mountItem,
      LayoutOutput layoutOutput) {
    init(
        component,
        mountItem.getHost(),
        mountItem.getContent(),
        layoutOutput,
        mountItem.getDisplayListDrawable());
  }

  void init(
      Component<?> component,
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
        layoutOutput.getDisplayListContainer(),
        displayListDrawable,
        layoutOutput.getFlags(),
        layoutOutput.getImportantForAccessibility());
  }

  void init(
      Component<?> component,
      ComponentHost host,
      Object content,
      NodeInfo nodeInfo,
      ViewNodeInfo viewNodeInfo,
      DisplayListContainer displayListContainer,
      DisplayListDrawable displayListDrawable,
      int flags,
      int importantForAccessibility) {
    mComponent = component;
    mContent = content;
    mHost = host;
    mFlags = flags;
    mImportantForAccessibility = importantForAccessibility;
    mDisplayListContainer = displayListContainer;
    mDisplayListDrawable = displayListDrawable;

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
        mFlags |= FLAG_VIEW_CLICKABLE;
      }

      if (view.isLongClickable()) {
        mFlags |= FLAG_VIEW_LONG_CLICKABLE;
      }

      if (view.isFocusable()) {
        mFlags |= FLAG_VIEW_FOCUSABLE;
      }

      if (view.isEnabled()) {
        mFlags |= FLAG_VIEW_ENABLED;
      }
    }
  }

  private @Nullable DisplayListDrawable acquireDisplayListDrawableIfNeeded(
      Object content,
      DisplayListContainer layoutOutputDisplayListContainer,
      DisplayListDrawable mountItemDisplayListDrawable) {

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

    if (displayList != null) {
      mountItemDisplayListDrawable.suppressInvalidations(true);
    }

    return mountItemDisplayListDrawable;
  }

  @Nullable
  Component<?> getComponent() {
    return mComponent;
  }

  ComponentHost getHost() {
    return mHost;
  }

  Object getContent() {
    return mContent;
  }

  int getFlags() {
    return mFlags;
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

  boolean isAccessible() {
    if (mComponent == null) {
      return false;
    }

    if (mImportantForAccessibility == IMPORTANT_FOR_ACCESSIBILITY_NO) {
      return false;
    }

    return (mNodeInfo != null && mNodeInfo.hasAccessibilityHandlers())
        || mComponent.getLifecycle().implementsAccessibility();
  }

  void release(ComponentContext context) {
    // Component hosts are recycled within other hosts instead of the global pool.
    if (!(mContent instanceof ComponentHost)) {
      ComponentsPools.release(context, mComponent.getLifecycle(), mContent);
    }

    if (mDisplayListDrawable != null) {
      ComponentsPools.release(mDisplayListDrawable);
      mDisplayListDrawable = null;
    }
    // Do not release DisplayListContainer yet, just dereference the object,
    // as it might be still referenced by LayoutOutput.
    mDisplayListContainer = null;

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
    mFlags = 0;
    mIsBound = false;
    mImportantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_AUTO;
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

  /**
   * @return Whether this MountItem is currently bound. A bound mount item is a Mount item that has
   * been mounted and is currently active on screen.
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

  DisplayListDrawable getDisplayListDrawable() {
    return mDisplayListDrawable;
  }

  void setDisplayList(DisplayList displayList) {
    mDisplayListContainer.setDisplayList(displayList);
  }

  @Nullable DisplayList getDisplayList() {
    return mDisplayListContainer.getDisplayList();
  }
}
