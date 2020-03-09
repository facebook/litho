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

import android.graphics.Rect;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import com.facebook.rendercore.Reducer;
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

  @IntDef({STATE_UPDATED, STATE_UNKNOWN, STATE_DIRTY})
  @Retention(RetentionPolicy.SOURCE)
  public @interface UpdateState {}

  private final @Nullable NodeInfo mNodeInfo;
  private final @Nullable ViewNodeInfo mViewNodeInfo;
  private @Nullable DebugHierarchy.Node mHierarchy;
  private final Component mComponent;
  private final Rect mBounds;
  private final int mHostTranslationX;
  private final int mHostTranslationY;
  private final int mFlags;

  private final int mImportantForAccessibility;
  private final int mOrientation;
  private final @Nullable TransitionId mTransitionId;
  private final long mHostMarker;

  private int mIndex;
  private long mId;
  private int mUpdateState = STATE_UNKNOWN;

  public LayoutOutput(
      @Nullable NodeInfo nodeInfo,
      @Nullable ViewNodeInfo viewNodeInfo,
      Component component,
      Rect bounds,
      int hostTranslationX,
      int hostTranslationY,
      int flags,
      long hostMarker,
      int importantForAccessibility,
      int orientation,
      @Nullable TransitionId transitionId) {

    if (component == null) {
      throw new RuntimeException("Trying to set a null Component on a LayoutOutput!");
    }

    mNodeInfo = nodeInfo;
    mViewNodeInfo = viewNodeInfo;
    mComponent = component;
    mBounds = bounds;
    mHostTranslationX = hostTranslationX;
    mHostTranslationY = hostTranslationY;
    mFlags = flags;
    mHostMarker = hostMarker;
    mImportantForAccessibility = importantForAccessibility;
    mOrientation = orientation;
    mTransitionId = transitionId;
  }

  Component getComponent() {
    return mComponent;
  }

  void getMountBounds(Rect outRect) {
    outRect.left = mBounds.left - mHostTranslationX;
    outRect.top = mBounds.top - mHostTranslationY;
    outRect.right = mBounds.right - mHostTranslationX;
    outRect.bottom = mBounds.bottom - mHostTranslationY;
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

  long getId() {
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

  public int getHostTranslationX() {
    return mHostTranslationX;
  }

  public int getHostTranslationY() {
    return mHostTranslationY;
  }

  public static LayoutOutput getLayoutOutput(RenderTreeNode node) {
    return (LayoutOutput) node.getLayoutData();
  }

  static RenderTreeNode create(final LayoutOutput output, final @Nullable RenderTreeNode parent) {
    return new RenderTreeNode(
        parent,
        parent != null ? new LithoRenderUnit(output) : Reducer.sRootHostRenderUnit,
        output,
        output.getBounds(),
        output.getViewNodeInfo() != null ? output.getViewNodeInfo().getPadding() : null,
        parent != null ? parent.getChildrenCount() : 0);
  }
}
