/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO;
import static com.facebook.litho.annotations.ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_YES;
import static com.facebook.litho.annotations.ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_YES_HIDE_DESCENDANTS;

import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.MountItem;
import com.facebook.rendercore.RenderTreeNode;

/** The output of a layout pass for a given {@link Component}. */
@Nullsafe(Nullsafe.Mode.LOCAL)
class LayoutOutput implements Cloneable {
  public static final int STATE_UNKNOWN = LithoRenderUnit.STATE_UNKNOWN;
  public static final int STATE_UPDATED = LithoRenderUnit.STATE_UPDATED;
  public static final int STATE_DIRTY = LithoRenderUnit.STATE_DIRTY;

  static final int LAYOUT_FLAG_DUPLICATE_PARENT_STATE =
      LithoRenderUnit.LAYOUT_FLAG_DUPLICATE_PARENT_STATE;
  static final int LAYOUT_FLAG_DISABLE_TOUCHABLE = LithoRenderUnit.LAYOUT_FLAG_DISABLE_TOUCHABLE;
  static final int LAYOUT_FLAG_MATCH_HOST_BOUNDS = LithoRenderUnit.LAYOUT_FLAG_MATCH_HOST_BOUNDS;
  static final int LAYOUT_FLAG_DRAWABLE_OUTPUTS_DISABLED =
      LithoRenderUnit.LAYOUT_FLAG_DRAWABLE_OUTPUTS_DISABLED;
  static final int LAYOUT_FLAG_DUPLICATE_CHILDREN_STATES =
      LithoRenderUnit.LAYOUT_FLAG_DUPLICATE_CHILDREN_STATES;

  private final @Nullable NodeInfo mNodeInfo;
  private final @Nullable ViewNodeInfo mViewNodeInfo;
  private @Nullable DebugHierarchy.Node mHierarchy; // TODO: remove
  private final Component mComponent;
  private final int mFlags;

  private final int mImportantForAccessibility;

  private final int mUpdateState;

  public LayoutOutput(
      final Component component,
      final @Nullable NodeInfo nodeInfo,
      final @Nullable ViewNodeInfo viewNodeInfo,
      final int flags,
      final int importantForAccessibility,
      final @LithoRenderUnit.UpdateState int updateState) {

    mNodeInfo = nodeInfo;
    mViewNodeInfo = viewNodeInfo;
    mComponent = component;
    mFlags = flags;
    mImportantForAccessibility =
        importantForAccessibility == IMPORTANT_FOR_ACCESSIBILITY_YES_HIDE_DESCENDANTS
            ? IMPORTANT_FOR_ACCESSIBILITY_YES // the A11Y prop for descendants has been corrected
            : importantForAccessibility;
    mUpdateState = updateState;
  }

  Component getComponent() {
    return mComponent;
  }

  int getFlags() {
    return mFlags;
  }

  @Nullable
  DebugHierarchy.Node getHierarchy() {
    return mHierarchy;
  }

  void setHierarchy(@Nullable DebugHierarchy.Node node) {
    mHierarchy = node;
  }

  @Nullable
  NodeInfo getNodeInfo() {
    return mNodeInfo;
  }

  @LithoRenderUnit.UpdateState
  public int getUpdateState() {
    return mUpdateState;
  }

  public int getImportantForAccessibility() {
    return mImportantForAccessibility;
  }

  boolean isAccessible() {
    if (mImportantForAccessibility == IMPORTANT_FOR_ACCESSIBILITY_NO) {
      return false;
    }

    return (mNodeInfo != null && mNodeInfo.needsAccessibilityDelegate())
        || (mComponent instanceof SpecGeneratedComponent
            && ((SpecGeneratedComponent) mComponent).implementsAccessibility());
  }

  @Nullable
  ViewNodeInfo getViewNodeInfo() {
    return mViewNodeInfo;
  }

  static LayoutOutput getLayoutOutput(RenderTreeNode node) {
    return ((LithoRenderUnit) node.getRenderUnit()).getLayoutOutput();
  }

  static LayoutOutput getLayoutOutput(MountItem item) {
    return getLayoutOutput(item.getRenderTreeNode());
  }

  static boolean isDuplicateParentState(int flags) {
    return (flags & LAYOUT_FLAG_DUPLICATE_PARENT_STATE) == LAYOUT_FLAG_DUPLICATE_PARENT_STATE;
  }

  static boolean isDuplicateChildrenStates(int flags) {
    return (flags & LAYOUT_FLAG_DUPLICATE_CHILDREN_STATES) == LAYOUT_FLAG_DUPLICATE_CHILDREN_STATES;
  }

  static boolean isTouchableDisabled(int flags) {
    return (flags & LAYOUT_FLAG_DISABLE_TOUCHABLE) == LAYOUT_FLAG_DISABLE_TOUCHABLE;
  }

  static boolean areDrawableOutputsDisabled(int flags) {
    return (flags & LAYOUT_FLAG_DRAWABLE_OUTPUTS_DISABLED) != 0;
  }
}
