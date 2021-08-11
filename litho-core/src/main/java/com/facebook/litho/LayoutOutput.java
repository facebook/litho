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

import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO;
import static com.facebook.litho.annotations.ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_YES;
import static com.facebook.litho.annotations.ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_YES_HIDE_DESCENDANTS;

import android.graphics.Rect;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import com.facebook.rendercore.MountItem;
import com.facebook.rendercore.RenderTreeNode;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The output of a layout pass for a given {@link Component}. It's used by {@link MountState} to
 * mount a component.
 */
class LayoutOutput implements Cloneable, AnimatableItem {
  public static final int STATE_UNKNOWN = 0;
  public static final int STATE_UPDATED = 1;
  public static final int STATE_DIRTY = 2;

  static final int LAYOUT_FLAG_DUPLICATE_PARENT_STATE = 1 << 0;
  static final int LAYOUT_FLAG_DISABLE_TOUCHABLE = 1 << 1;
  static final int LAYOUT_FLAG_MATCH_HOST_BOUNDS = 1 << 2;
  static final int LAYOUT_FLAG_DRAWABLE_OUTPUTS_DISABLED = 1 << 3;
  static final int LAYOUT_FLAG_DUPLICATE_CHILDREN_STATES = 1 << 4;
  private final String mKey;

  @IntDef({STATE_UPDATED, STATE_UNKNOWN, STATE_DIRTY})
  @Retention(RetentionPolicy.SOURCE)
  public @interface UpdateState {}

  private final @Nullable NodeInfo mNodeInfo;
  private final @Nullable ViewNodeInfo mViewNodeInfo;
  private @Nullable DebugHierarchy.Node mHierarchy;
  private final Component mComponent;
  private final Rect mBounds;
  final int mHostTranslationX;
  final int mHostTranslationY;
  private final int mFlags;

  private final int mImportantForAccessibility;
  private final int mOrientation;
  private final @Nullable TransitionId mTransitionId;
  private final long mHostMarker;

  private int mIndex;
  private long mId;
  private int mUpdateState = STATE_UNKNOWN;

  /* TODO: (T81557408) Fix @Nullable issue */
  public LayoutOutput(
      final Component component,
      final @Nullable NodeInfo nodeInfo,
      final @Nullable ViewNodeInfo viewNodeInfo,
      final @Nullable String key,
      final Rect bounds,
      final int hostTranslationX,
      final int hostTranslationY,
      final int flags,
      final long hostMarker,
      final int importantForAccessibility,
      final int orientation,
      final @Nullable TransitionId transitionId) {

    if (component == null) {
      throw new RuntimeException("Trying to set a null Component on a LayoutOutput!");
    }

    mNodeInfo = nodeInfo;
    mViewNodeInfo = viewNodeInfo;
    mComponent = component;
    mKey = key;
    mBounds = bounds;
    mHostTranslationX = hostTranslationX;
    mHostTranslationY = hostTranslationY;
    mFlags = flags;
    mHostMarker = hostMarker;
    mImportantForAccessibility =
        importantForAccessibility == IMPORTANT_FOR_ACCESSIBILITY_YES_HIDE_DESCENDANTS
            ? IMPORTANT_FOR_ACCESSIBILITY_YES // the A11Y prop for descendants has been corrected
            : importantForAccessibility;
    mOrientation = orientation;
    mTransitionId = transitionId;
  }

  Component getComponent() {
    return mComponent;
  }

  String getKey() {
    return mKey;
  }

  @Override
  public Rect getBounds() {
    return mBounds;
  }

  @Override
  public float getScale() {
    return mNodeInfo != null ? mNodeInfo.getScale() : 1;
  }

  @Override
  public float getAlpha() {
    return mNodeInfo != null ? mNodeInfo.getAlpha() : 1;
  }

  @Override
  public float getRotation() {
    return mNodeInfo != null ? mNodeInfo.getRotation() : 0;
  }

  @Override
  public float getRotationX() {
    return mNodeInfo != null ? mNodeInfo.getRotationX() : 0;
  }

  @Override
  public float getRotationY() {
    return mNodeInfo != null ? mNodeInfo.getRotationY() : 0;
  }

  @Override
  public boolean isScaleSet() {
    return mNodeInfo != null && mNodeInfo.isScaleSet();
  }

  @Override
  public boolean isAlphaSet() {
    return mNodeInfo != null && mNodeInfo.isAlphaSet();
  }

  @Override
  public boolean isRotationSet() {
    return mNodeInfo != null && mNodeInfo.isRotationSet();
  }

  @Override
  public boolean isRotationXSet() {
    return mNodeInfo != null && mNodeInfo.isRotationXSet();
  }

  @Override
  public boolean isRotationYSet() {
    return mNodeInfo != null && mNodeInfo.isRotationYSet();
  }

  int getFlags() {
    return mFlags;
  }

  /**
   * Returns the id of the LayoutOutput that represents the host of this LayoutOutput. This host may
   * be phantom, meaning that the mount content that represents this LayoutOutput may be hosted
   * inside one of higher level hosts {@see MountState#getActualComponentHost()}
   */
  long getHostMarker() {
    return mHostMarker;
  }

  @Override
  public long getId() {
    return mId;
  }

  void setId(long id) {
    mId = id;
  }

  int getIndex() {
    return mIndex;
  }

  void setIndex(int index) {
    mIndex = index;
  }

  @Nullable
  DebugHierarchy.Node getHierarchy() {
    return mHierarchy;
  }

  void setHierarchy(DebugHierarchy.Node node) {
    mHierarchy = node;
  }

  @Nullable
  NodeInfo getNodeInfo() {
    return mNodeInfo;
  }

  public void setUpdateState(@UpdateState int state) {
    mUpdateState = state;
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
        || mComponent.implementsAccessibility();
  }

  int getOrientation() {
    return mOrientation;
  }

  @Nullable
  ViewNodeInfo getViewNodeInfo() {
    return mViewNodeInfo;
  }

  @Nullable
  public TransitionId getTransitionId() {
    return mTransitionId;
  }

  @Override
  public int getOutputType() {
    return LayoutStateOutputIdCalculator.getTypeFromId(getId());
  }

  static RenderTreeNode create(
      final LayoutOutput output,
      final Rect bounds,
      final @Nullable ComponentContext context,
      final @Nullable LithoRenderUnitFactory lithoRenderUnitFactory,
      final @Nullable RenderTreeNode parent) {

    final LithoRenderUnit lithoRenderUnit =
        lithoRenderUnitFactory == null
            ? new LithoRenderUnit(output)
            : lithoRenderUnitFactory.getRenderUnit(output);

    return new RenderTreeNode(
        parent,
        lithoRenderUnit,
        context != null
            ? context.useStatelessComponent() ? context.getScopedComponentInfo() : context
            : null,
        bounds,
        output.getViewNodeInfo() != null ? output.getViewNodeInfo().getPadding() : null,
        parent != null ? parent.getChildrenCount() : 0);
  }

  static LayoutOutput getLayoutOutput(RenderTreeNode node) {
    return ((LithoRenderUnit) node.getRenderUnit()).output;
  }

  static LayoutOutput getLayoutOutput(MountItem item) {
    return getLayoutOutput(item.getRenderTreeNode());
  }

  static @Nullable ComponentContext getComponentContext(RenderTreeNode node) {
    return LithoRenderUnit.getContext(node.getLayoutData());
  }

  static ComponentContext getComponentContext(MountItem item) {
    return getComponentContext(item.getRenderTreeNode());
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

  static Rect getMountBounds(Rect outRect, Rect bounds, int x, int y) {
    outRect.left = bounds.left - x;
    outRect.top = bounds.top - y;
    outRect.right = bounds.right - x;
    outRect.bottom = bounds.bottom - y;
    return outRect;
  }
}
