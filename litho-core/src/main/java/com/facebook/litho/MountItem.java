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
import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.Nullable;
import com.facebook.yoga.YogaDirection;

/**
 * Represents a mounted UI element in a {@link MountState}. It holds a
 * key and a content instance which might be any type of UI element
 * supported by the framework e.g. {@link Drawable}.
 */
class MountItem {

  static final int LAYOUT_FLAG_DUPLICATE_PARENT_STATE = 1 << 0;
  static final int LAYOUT_FLAG_DISABLE_TOUCHABLE = 1 << 1;
  static final int LAYOUT_FLAG_MATCH_HOST_BOUNDS = 1 << 2;

  private static final int FLAG_VIEW_CLICKABLE = 1 << 0;
  private static final int FLAG_VIEW_LONG_CLICKABLE = 1 << 1;
  private static final int FLAG_VIEW_FOCUSABLE = 1 << 2;
  private static final int FLAG_VIEW_ENABLED = 1 << 3;
  private static final int FLAG_VIEW_SELECTED = 1 << 4;

  private final Object mContent;

  private NodeInfo mNodeInfo;
  private ViewNodeInfo mViewNodeInfo;
  private Component mComponent;
  private ComponentHost mHost;
  private boolean mIsBound;
  private int mImportantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_AUTO;
  private @Nullable TransitionId mTransitionId;
  private int mOrientation;
  private boolean mIsReleased;
  private String mReleaseCause;

  // ComponentHost flags defined in the LayoutOutput specifying
  // the behaviour of this item when mounted.
  private int mLayoutFlags;

  // Flags that track view-related behaviour of mounted view content.
  private int mMountViewFlags;

  /** This mountItem represents the top-level root host (LithoView) which is always mounted. */
  static MountItem createRootHostMountItem(LithoView lithoView) {
    final ViewNodeInfo viewNodeInfo = new ViewNodeInfo();
    viewNodeInfo.setLayoutDirection(YogaDirection.INHERIT);
    MountItem item =
        new MountItem(
            HostComponent.create(),
            lithoView,
            lithoView,
            null,
            viewNodeInfo,
            0,
            IMPORTANT_FOR_ACCESSIBILITY_AUTO,
            lithoView.getContext().getResources().getConfiguration().orientation,
            null);
    return item;
  }

  MountItem(Component component, ComponentHost host, Object content, LayoutOutput layoutOutput) {
    this(
        component,
        host,
        content,
        layoutOutput.getNodeInfo(),
        layoutOutput.getViewNodeInfo(),
        layoutOutput.getFlags(),
        layoutOutput.getImportantForAccessibility(),
        layoutOutput.getOrientation(),
        layoutOutput.getTransitionId());
  }

  MountItem(
      Component component,
      ComponentHost host,
      Object content,
      NodeInfo nodeInfo,
      ViewNodeInfo viewNodeInfo,
      int layoutFlags,
      int importantForAccessibility,
      int orientation,
      TransitionId transitionId) {
    if (component == null) {
      throw new RuntimeException("Calling init() on a MountItem with a null Component!");
    }

    mComponent = component;
    mContent = content;
    mHost = host;
    mLayoutFlags = layoutFlags;
    mImportantForAccessibility = importantForAccessibility;
    mOrientation = orientation;
    mTransitionId = transitionId;

    if (nodeInfo != null) {
      mNodeInfo = nodeInfo;
    }

    if (viewNodeInfo != null) {
      mViewNodeInfo = viewNodeInfo;
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

  /**
   * Call this method when assigning a new {@link LayoutOutput} to an existing MountItem. In this
   * case we don't want to update mMountViewFlags since those flags are only used to determine the
   * initial state of the view content, which we will have already done in init(). If it is done
   * again now some of the values may be wrong (e.g. the Litho framework may add a click listener to
   * a view that was not originally clickable.
   */
  void update(LayoutOutput layoutOutput) {
    mComponent = layoutOutput.getComponent();
    if (mComponent == null) {
      throw new RuntimeException("Trying to update a MountItem with a null Component!");
    }
    mLayoutFlags = layoutOutput.getFlags();
    mImportantForAccessibility = layoutOutput.getImportantForAccessibility();
    mOrientation = layoutOutput.getOrientation();
    mTransitionId = layoutOutput.getTransitionId();
    mNodeInfo = null;
    mViewNodeInfo = null;

    if (layoutOutput.getNodeInfo() != null) {
      mNodeInfo = layoutOutput.getNodeInfo();
    }

    if (layoutOutput.getViewNodeInfo() != null) {
      mViewNodeInfo = layoutOutput.getViewNodeInfo();
    }
  }

  @Nullable
  Component getComponent() {
    return mComponent;
  }

  void setHost(ComponentHost host) {
    mHost = host;
  }

  ComponentHost getHost() {
    return mHost;
  }

  /** @return Mount content created by the component. */
  Object getContent() {
    // TODO(t46457238): Re-enable mIsReleased assertion
    // if (mIsReleased) {
    //   throw new RuntimeException("Trying to access released mount content!");
    // }
    return mContent;
  }

  int getLayoutFlags() {
    return mLayoutFlags;
  }

  int getImportantForAccessibility() {
    return mImportantForAccessibility;
  }

  int getOrientation() {
    return mOrientation;
  }

  NodeInfo getNodeInfo() {
    return mNodeInfo;
  }

  ViewNodeInfo getViewNodeInfo() {
    return mViewNodeInfo;
  }

  @Nullable
  TransitionId getTransitionId() {
    return mTransitionId;
  }

  boolean hasTransitionId() {
    return mTransitionId != null;
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

  void releaseMountContent(Context context, String releaseCause) {
    if (mIsReleased) {
      final String componentName = mComponent != null ? mComponent.getSimpleName() : "<null>";
      final String globalKey = mComponent != null ? mComponent.getGlobalKey() : "<null>";
      throw new ReleasingReleasedMountContentException(
          "Releasing released mount content! component: "
              + componentName
              + ", globalKey: "
              + globalKey
              + ", transitionId: "
              + getTransitionId()
              + ", previousReleaseCause: "
              + mReleaseCause);
    }
    ComponentsPools.release(context, mComponent, mContent);
    mIsReleased = true;
    mReleaseCause = releaseCause;
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

  public static class ReleasingReleasedMountContentException extends RuntimeException {

    public ReleasingReleasedMountContentException(String message) {
      super(message);
    }
  }
}
