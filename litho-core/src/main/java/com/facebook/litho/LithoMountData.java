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
import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO;

import android.content.Context;
import android.view.View;
import androidx.annotation.Nullable;
import com.facebook.rendercore.MountItem;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.yoga.YogaDirection;

/** Contains the addition data of a mountable item. */
class LithoMountData {

  static final int LAYOUT_FLAG_DUPLICATE_PARENT_STATE = 1 << 0;
  static final int LAYOUT_FLAG_DISABLE_TOUCHABLE = 1 << 1;
  static final int LAYOUT_FLAG_MATCH_HOST_BOUNDS = 1 << 2;

  private static final int FLAG_VIEW_CLICKABLE = 1 << 0;
  private static final int FLAG_VIEW_LONG_CLICKABLE = 1 << 1;
  private static final int FLAG_VIEW_FOCUSABLE = 1 << 2;
  private static final int FLAG_VIEW_ENABLED = 1 << 3;
  private static final int FLAG_VIEW_SELECTED = 1 << 4;

  private LayoutOutput mOutput;

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
    MountItem item = new MountItem(LayoutOutput.create(output, null), lithoView, lithoView);
    item.setMountData(new LithoMountData(output, lithoView));
    return item;
  }

  LithoMountData(LayoutOutput output, Object content) {
    mOutput = output;

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
   * Call this method when assigning a new {@link LayoutOutput} to an existing MountItem. In this
   * case we don't want to update mMountViewFlags since those flags are only used to determine the
   * initial state of the view content, which we will have already done in init(). If it is done
   * again now some of the values may be wrong (e.g. the Litho framework may add a click listener to
   * a view that was not originally clickable.
   */
  void update(RenderTreeNode node) {
    LayoutOutput output = (LayoutOutput) node.getLayoutData();
    if (output != null) {
      final NodeInfo nodeInfo;
      if (output.getNodeInfo() != null) {
        nodeInfo = output.getNodeInfo();
      } else {
        nodeInfo = null;
      }

      final ViewNodeInfo viewNodeInfo;
      if (output.getViewNodeInfo() != null) {
        viewNodeInfo = output.getViewNodeInfo();
      } else {
        viewNodeInfo = null;
      }

      mOutput =
          new LayoutOutput(
              nodeInfo,
              viewNodeInfo,
              output.getComponent(),
              output.getBounds(),
              output.getHostTranslationX(),
              output.getHostTranslationY(),
              output.getFlags(),
              output.getHostMarker(),
              output.getImportantForAccessibility(),
              output.getOrientation(),
              output.getTransitionId());
    }
  }

  Component getComponent() {
    return mOutput.getComponent();
  }

  int getLayoutFlags() {
    return mOutput.getFlags();
  }

  int getImportantForAccessibility() {
    return mOutput.getImportantForAccessibility();
  }

  int getOrientation() {
    return mOutput.getOrientation();
  }

  @Nullable
  DebugHierarchy.Node getHierarchy() {
    return mOutput.getHierarchy();
  }

  @Nullable
  NodeInfo getNodeInfo() {
    return mOutput.getNodeInfo();
  }

  @Nullable
  ViewNodeInfo getViewNodeInfo() {
    return mOutput.getViewNodeInfo();
  }

  @Nullable
  TransitionId getTransitionId() {
    return mOutput.getTransitionId();
  }

  boolean hasTransitionId() {
    return getTransitionId() != null;
  }

  boolean isAccessible() {
    if (mOutput.getComponent() == null) {
      return false;
    }

    if (getImportantForAccessibility() == IMPORTANT_FOR_ACCESSIBILITY_NO) {
      return false;
    }

    return (getNodeInfo() != null && getNodeInfo().needsAccessibilityDelegate())
        || getComponent().implementsAccessibility();
  }

  void releaseMountContent(Context context, String releaseCause) {
    if (mIsReleased) {
      Component component = getComponent();
      final String componentName = component != null ? component.getSimpleName() : "<null>";
      final String globalKey = component != null ? component.getGlobalKey() : "<null>";
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

  public static LithoMountData getMountData(MountItem item) {
    return (LithoMountData) item.getMountData();
  }

  public static class ReleasingReleasedMountContentException extends RuntimeException {

    public ReleasingReleasedMountContentException(String message) {
      super(message);
    }
  }
}
