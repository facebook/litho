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

import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static com.facebook.litho.LayoutOutput.getLayoutOutput;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.yoga.YogaDirection;

/**
 * Represents a mounted UI element in a {@link MountState}. It holds a key and a content instance
 * which might be any type of UI element supported by the framework e.g. {@link Drawable}.
 */
class MountItem {

  static final int LAYOUT_FLAG_DUPLICATE_PARENT_STATE = 1 << 0;
  static final int LAYOUT_FLAG_DISABLE_TOUCHABLE = 1 << 1;
  static final int LAYOUT_FLAG_MATCH_HOST_BOUNDS = 1 << 2;
  static final int LAYOUT_FLAG_DRAWABLE_OUTPUTS_DISABLED = 1 << 3;

  private static final int FLAG_VIEW_CLICKABLE = 1 << 0;
  private static final int FLAG_VIEW_LONG_CLICKABLE = 1 << 1;
  private static final int FLAG_VIEW_FOCUSABLE = 1 << 2;
  private static final int FLAG_VIEW_ENABLED = 1 << 3;
  private static final int FLAG_VIEW_SELECTED = 1 << 4;

  private final Object mContent;

  private ComponentHost mHost;
  private RenderTreeNode mRenderTreeNode;
  private boolean mIsBound;

  private boolean mIsReleased;
  private String mReleaseCause;

  // Flags that track view-related behaviour of mounted view content.
  private int mMountViewFlags;

  /** This mountItem represents the top-level root host (LithoView) which is always mounted. */
  static MountItem createRootHostMountItem(LithoView lithoView) {
    final ViewNodeInfo viewNodeInfo = new ViewNodeInfo();
    viewNodeInfo.setLayoutDirection(YogaDirection.INHERIT);
    LayoutOutput output =
        new LayoutOutput(
            null,
            viewNodeInfo,
            HostComponent.create(),
            lithoView.getPreviousMountBounds(),
            0,
            0,
            0,
            0,
            IMPORTANT_FOR_ACCESSIBILITY_AUTO,
            lithoView.getContext().getResources().getConfiguration().orientation,
            null);
    return new MountItem(lithoView, lithoView, LayoutOutput.create(output, null));
  }

  MountItem(ComponentHost host, Object content, RenderTreeNode node) {
    mContent = content;
    mHost = host;
    mRenderTreeNode = node;

    if (content instanceof View) {
      final View view = (View) content;

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
   * Call this method when assigning a new {@link RenderTreeNode} to an existing MountItem. In this
   * case we don't want to update mMountViewFlags since those flags are only used to determine the
   * initial state of the view content, which we will have already done in init(). If it is done
   * again now some of the values may be wrong (e.g. the Litho framework may add a click listener to
   * a view that was not originally clickable.
   */
  void update(RenderTreeNode node) {
    mRenderTreeNode = node;
  }

  void setHost(ComponentHost host) {
    mHost = host;
  }

  ComponentHost getHost() {
    return mHost;
  }

  /** @return Mount content created by the component. */
  Object getContent() {
    if (mIsReleased) {
      throw new RuntimeException("Trying to access released mount content!");
    }
    return mContent;
  }

  public RenderTreeNode getRenderTreeNode() {
    return mRenderTreeNode;
  }

  void releaseMountContent(Context context, String releaseCause, int recyclingMode) {
    final LayoutOutput output = getLayoutOutput(mRenderTreeNode);
    final Component mComponent = output.getComponent();
    if (mIsReleased) {
      final String componentName = mComponent != null ? mComponent.getSimpleName() : "<null>";
      final String globalKey = mComponent != null ? mComponent.getGlobalKey() : "<null>";
      throw new ReleasingReleasedMountContentException(
          "Releasing released mount content! component: "
              + componentName
              + ", globalKey: "
              + globalKey
              + ", transitionId: "
              + output.getTransitionId()
              + ", previousReleaseCause: "
              + mReleaseCause);
    }
    ComponentsPools.release(context, mComponent, mContent, recyclingMode);
    mIsReleased = true;
    mReleaseCause = releaseCause;
  }

  static boolean isDuplicateParentState(int flags) {
    return (flags & LAYOUT_FLAG_DUPLICATE_PARENT_STATE) == LAYOUT_FLAG_DUPLICATE_PARENT_STATE;
  }

  static boolean isTouchableDisabled(int flags) {
    return (flags & LAYOUT_FLAG_DISABLE_TOUCHABLE) == LAYOUT_FLAG_DISABLE_TOUCHABLE;
  }

  public boolean areDrawableOutputsDisabled() {
    final LayoutOutput output = getLayoutOutput(mRenderTreeNode);
    return (output.getFlags() & LAYOUT_FLAG_DRAWABLE_OUTPUTS_DISABLED) != 0;
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

  /** Sets whether this MountItem is currently bound. */
  void setIsBound(boolean bound) {
    mIsBound = bound;
  }

  public static class ReleasingReleasedMountContentException extends RuntimeException {

    public ReleasingReleasedMountContentException(String message) {
      super(message);
    }
  }
}
