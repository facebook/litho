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

import static com.facebook.litho.annotations.ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_NO;
import static com.facebook.litho.annotations.ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_YES;
import static com.facebook.litho.annotations.ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_YES_HIDE_DESCENDANTS;

import android.graphics.Rect;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.MountItem;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.transitions.TransitionRenderUnit;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Nullsafe(Nullsafe.Mode.LOCAL)
public abstract class LithoRenderUnit extends RenderUnit<Object> implements TransitionRenderUnit {

  public static final int STATE_UNKNOWN = 0;
  public static final int STATE_UPDATED = 1;
  public static final int STATE_DIRTY = 2;

  static final int LAYOUT_FLAG_DUPLICATE_PARENT_STATE = 1 << 0;
  static final int LAYOUT_FLAG_DISABLE_TOUCHABLE = 1 << 1;
  static final int LAYOUT_FLAG_MATCH_HOST_BOUNDS = 1 << 2;
  static final int LAYOUT_FLAG_DRAWABLE_OUTPUTS_DISABLED = 1 << 3;
  static final int LAYOUT_FLAG_DUPLICATE_CHILDREN_STATES = 1 << 4;

  @IntDef({STATE_UPDATED, STATE_UNKNOWN, STATE_DIRTY})
  @Retention(RetentionPolicy.SOURCE)
  public @interface UpdateState {}

  private final @Nullable NodeInfo mNodeInfo;
  private final @Nullable ViewNodeInfo mViewNodeInfo;
  private final @Nullable Rect mTouchBoundsExpansion;
  private @Nullable DebugHierarchy.Node mHierarchy; // TODO: remove
  private final Component mComponent;
  private final int mFlags;

  private final int mImportantForAccessibility;

  private final int mUpdateState;

  protected final long mId;
  protected final @Nullable ComponentContext mContext;

  protected LithoRenderUnit(
      long id,
      final Component component,
      final @Nullable NodeInfo nodeInfo,
      final @Nullable ViewNodeInfo viewNodeInfo,
      final @Nullable Rect touchBoundsExpansion,
      final int flags,
      final int importantForAccessibility,
      final @UpdateState int updateState,
      RenderType renderType,
      @Nullable ComponentContext context) {
    super(renderType);
    this.mContext = context;
    mNodeInfo = nodeInfo;
    mViewNodeInfo = viewNodeInfo;
    mTouchBoundsExpansion = touchBoundsExpansion;
    mComponent = component;
    mFlags = flags;
    mImportantForAccessibility =
        importantForAccessibility == IMPORTANT_FOR_ACCESSIBILITY_YES_HIDE_DESCENDANTS
            ? IMPORTANT_FOR_ACCESSIBILITY_YES // the A11Y prop for descendants has been corrected
            : importantForAccessibility;
    mUpdateState = updateState;
    this.mId = id;
  }

  public @Nullable ComponentContext getComponentContext() {
    return mContext;
  }

  @Override
  public long getId() {
    return mId;
  }

  @Override
  public boolean getMatchHostBounds() {
    return (getFlags() & LAYOUT_FLAG_MATCH_HOST_BOUNDS) != 0;
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

  @UpdateState
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

  @Nullable
  public Rect getTouchBoundsExpansion() {
    return mTouchBoundsExpansion;
  }

  static @Nullable ComponentContext getComponentContext(MountItem item) {
    return ((LithoRenderUnit) item.getRenderTreeNode().getRenderUnit()).getComponentContext();
  }

  static @Nullable ComponentContext getComponentContext(RenderTreeNode node) {
    return ((LithoRenderUnit) node.getRenderUnit()).getComponentContext();
  }

  static @Nullable ComponentContext getComponentContext(LithoRenderUnit unit) {
    return unit.getComponentContext();
  }

  static LithoRenderUnit getRenderUnit(MountItem item) {
    return getRenderUnit(item.getRenderTreeNode());
  }

  static LithoRenderUnit getRenderUnit(RenderTreeNode node) {
    return (LithoRenderUnit) node.getRenderUnit();
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

  public static boolean isMountableView(RenderUnit unit) {
    return unit.getRenderType() == RenderType.VIEW;
  }
}
