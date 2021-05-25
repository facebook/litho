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

import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.RIGHT;
import static com.facebook.yoga.YogaEdge.TOP;

import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaNode;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the default implementation of a {@link LithoLayoutResult}. This holds a reference to the
 * {@link InternalNode} which created it, its {@link YogaNode}, and a list of its children.
 */
public class DefaultLayoutResult implements LithoLayoutResult, ComponentLayout {

  private final ComponentContext mContext;

  private final InputOnlyInternalNode mInternalNode;

  private final List<LithoLayoutResult> mChildren = new ArrayList<>();
  private final YogaNode mYogaNode;

  private @Nullable LithoLayoutResult mParent;

  private int mLastWidthSpec = DiffNode.UNSPECIFIED;
  private int mLastHeightSpec = DiffNode.UNSPECIFIED;
  private float mLastMeasuredWidth = DiffNode.UNSPECIFIED;
  private float mLastMeasuredHeight = DiffNode.UNSPECIFIED;

  public DefaultLayoutResult(
      final ComponentContext c,
      final InputOnlyInternalNode internalNode,
      final YogaNode yogaNode,
      final @Nullable LithoLayoutResult parent) {
    mContext = c;
    mInternalNode = internalNode;
    mYogaNode = yogaNode;
    mParent = parent;
  }

  @Px
  @Override
  public int getX() {
    return (int) mYogaNode.getLayoutX();
  }

  @Px
  @Override
  public int getY() {
    return (int) mYogaNode.getLayoutY();
  }

  @Px
  @Override
  public int getWidth() {
    return (int) mYogaNode.getLayoutWidth();
  }

  @Px
  @Override
  public int getHeight() {
    return (int) mYogaNode.getLayoutHeight();
  }

  @Px
  @Override
  public int getPaddingTop() {
    return FastMath.round(mYogaNode.getLayoutPadding(TOP));
  }

  @Px
  @Override
  public int getPaddingRight() {
    return FastMath.round(mYogaNode.getLayoutPadding(RIGHT));
  }

  @Px
  @Override
  public int getPaddingBottom() {
    return FastMath.round(mYogaNode.getLayoutPadding(BOTTOM));
  }

  @Px
  @Override
  public int getPaddingLeft() {
    return FastMath.round(mYogaNode.getLayoutPadding(LEFT));
  }

  @Override
  public boolean isPaddingSet() {
    return mInternalNode.isPaddingSet();
  }

  @Override
  public @Nullable Drawable getBackground() {
    return mInternalNode.getBackground();
  }

  @Override
  public YogaDirection getResolvedLayoutDirection() {
    return mYogaNode.getLayoutDirection();
  }

  @Override
  public InternalNode getInternalNode() {
    return mInternalNode;
  }

  @Override
  public boolean shouldDrawBorders() {
    return mInternalNode.hasBorderColor()
        && (mYogaNode.getLayoutBorder(LEFT) != 0
            || mYogaNode.getLayoutBorder(TOP) != 0
            || mYogaNode.getLayoutBorder(RIGHT) != 0
            || mYogaNode.getLayoutBorder(BOTTOM) != 0);
  }

  @Override
  public int getLayoutBorder(YogaEdge edge) {
    return FastMath.round(mYogaNode.getLayoutBorder(edge));
  }

  @Override
  public int getTouchExpansionBottom() {
    if (!shouldApplyTouchExpansion()) {
      return 0;
    }

    return FastMath.round(mInternalNode.getTouchExpansion().get(YogaEdge.BOTTOM));
  }

  @Override
  public int getTouchExpansionLeft() {
    if (!shouldApplyTouchExpansion()) {
      return 0;
    }

    return FastMath.round(resolveHorizontalEdges(mInternalNode.getTouchExpansion(), YogaEdge.LEFT));
  }

  @Override
  public int getTouchExpansionRight() {
    if (!shouldApplyTouchExpansion()) {
      return 0;
    }

    return FastMath.round(
        resolveHorizontalEdges(mInternalNode.getTouchExpansion(), YogaEdge.RIGHT));
  }

  @Override
  public int getTouchExpansionTop() {
    if (!shouldApplyTouchExpansion()) {
      return 0;
    }

    return FastMath.round(mInternalNode.getTouchExpansion().get(YogaEdge.TOP));
  }

  private boolean shouldApplyTouchExpansion() {
    return mInternalNode.getTouchExpansion() != null
        && mInternalNode.getNodeInfo() != null
        && mInternalNode.getNodeInfo().hasTouchEventHandlers();
  }

  private float resolveHorizontalEdges(Edges spacing, YogaEdge edge) {
    final boolean isRtl = (mYogaNode.getLayoutDirection() == YogaDirection.RTL);

    final YogaEdge resolvedEdge;
    switch (edge) {
      case LEFT:
        resolvedEdge = (isRtl ? YogaEdge.END : YogaEdge.START);
        break;

      case RIGHT:
        resolvedEdge = (isRtl ? YogaEdge.START : YogaEdge.END);
        break;

      default:
        throw new IllegalArgumentException("Not an horizontal padding edge: " + edge);
    }

    float result = spacing.getRaw(resolvedEdge);
    if (YogaConstants.isUndefined(result)) {
      result = spacing.get(edge);
    }

    return result;
  }

  @Override
  public int getLastHeightSpec() {
    return mLastHeightSpec;
  }

  @Override
  public void setLastHeightSpec(int heightSpec) {
    mLastHeightSpec = heightSpec;
  }

  /**
   * The last value the measure funcion associated with this node {@link Component} returned for the
   * height. This is used together with {@link LithoLayoutResult#getLastHeightSpec()} to implement
   * measure caching.
   */
  @Override
  public float getLastMeasuredHeight() {
    return mLastMeasuredHeight;
  }

  /**
   * Sets the last value the measure funcion associated with this node {@link Component} returned
   * for the height.
   */
  @Override
  public void setLastMeasuredHeight(float lastMeasuredHeight) {
    mLastMeasuredHeight = lastMeasuredHeight;
  }

  /**
   * The last value the measure funcion associated with this node {@link Component} returned for the
   * width. This is used together with {@link LithoLayoutResult#getLastWidthSpec()} to implement
   * measure caching.
   */
  @Override
  public float getLastMeasuredWidth() {
    return mLastMeasuredWidth;
  }

  /**
   * Sets the last value the measure funcion associated with this node {@link Component} returned
   * for the width.
   */
  @Override
  public void setLastMeasuredWidth(float lastMeasuredWidth) {
    mLastMeasuredWidth = lastMeasuredWidth;
  }

  @Override
  public int getLastWidthSpec() {
    return mLastWidthSpec;
  }

  @Override
  public void setLastWidthSpec(int widthSpec) {
    mLastWidthSpec = widthSpec;
  }

  @Override
  public YogaDirection recursivelyResolveLayoutDirection() {
    final YogaDirection direction = mYogaNode.getLayoutDirection();
    if (direction == YogaDirection.INHERIT) {
      throw new IllegalStateException("Direction cannot be resolved before layout calculation");
    }
    return direction;
  }

  @Override
  public LithoLayoutResult getChildAt(int i) {
    return mChildren.get(i);
  }

  @Override
  public void addChild(LithoLayoutResult child) {
    child.setParent(this);
    mChildren.add(child);
  }

  @Override
  public int getChildCount() {
    return mChildren.size();
  }

  @Override
  public @Nullable LithoLayoutResult getParent() {
    return mParent;
  }

  @Override
  public void setParent(@Nullable LithoLayoutResult parent) {
    mParent = parent;
  }

  @Override
  public YogaNode getYogaNode() {
    return mYogaNode;
  }
}
