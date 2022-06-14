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

import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.RIGHT;
import static com.facebook.yoga.YogaEdge.TOP;

import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import com.facebook.rendercore.Node.LayoutResult;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.utils.MeasureSpecUtils;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaMeasureOutput;
import com.facebook.yoga.YogaNode;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the default implementation of a {@link LayoutResult} for Litho. This holds a reference to
 * the {@link LithoNode} which created it, its {@link YogaNode}, and a list of its children.
 */
public class LithoLayoutResult implements ComponentLayout, LayoutResult {

  protected final LayoutStateContext mLayoutContext;
  private final ComponentContext mContext;

  protected final LithoNode mNode;

  private final List<LithoLayoutResult> mChildren = new ArrayList<>();
  private final YogaNode mYogaNode;

  private @Nullable LithoLayoutResult mParent;

  private boolean mCachedMeasuresValid;
  private @Nullable DiffNode mDiffNode;

  private int mLastWidthSpec = DiffNode.UNSPECIFIED;
  private int mLastHeightSpec = DiffNode.UNSPECIFIED;
  private float mLastMeasuredWidth = DiffNode.UNSPECIFIED;
  private float mLastMeasuredHeight = DiffNode.UNSPECIFIED;

  private @Nullable LithoRenderUnit mContentRenderUnit;
  private @Nullable LithoRenderUnit mHostRenderUnit;
  private @Nullable LithoRenderUnit mBackgroundRenderUnit;
  private @Nullable LithoRenderUnit mForegroundRenderUnit;
  private @Nullable LithoRenderUnit mBorderRenderUnit;

  private @Nullable Object mLayoutData;

  public LithoLayoutResult(
      final LayoutStateContext layoutStateContext,
      final ComponentContext c,
      final LithoNode node,
      final YogaNode yogaNode,
      final @Nullable LithoLayoutResult parent) {
    mLayoutContext = layoutStateContext;
    mContext = c;
    mNode = node;
    mYogaNode = yogaNode;
    mParent = parent;

    /*

     Ideally the layout data should be created when measure is called on the mount spec or
     mountable component, but because of the current implementation of mount specs, and the way
     Yoga works it a possibility that measure may not be called, and a MountSpec [may] require
     inter stage props, then it is necessary to have a non-null InterStagePropsContainer even if
     the values are uninitialised. Otherwise it will lead to NPEs.

     This should get cleaned up once the implementation is general enough for MountableComponents.

    */
    final Component component = node.getTailComponent();
    if (!Component.isMountable(component)) {
      mLayoutData = component.createInterStagePropsContainer();
    }
  }

  public LayoutStateContext getLayoutStateContext() {
    return mLayoutContext;
  }

  public ComponentContext getContext() {
    return mContext;
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
  public int getWidthSpec() {
    return mLastWidthSpec;
  }

  @Override
  public int getHeightSpec() {
    return mLastHeightSpec;
  }

  @Override
  public boolean isPaddingSet() {
    return mNode.isPaddingSet();
  }

  @Override
  public @Nullable Drawable getBackground() {
    return mNode.getBackground();
  }

  @Override
  public YogaDirection getResolvedLayoutDirection() {
    return mYogaNode.getLayoutDirection();
  }

  public LithoNode getNode() {
    return mNode;
  }

  public boolean shouldDrawBorders() {
    return mNode.hasBorderColor()
        && (mYogaNode.getLayoutBorder(LEFT) != 0
            || mYogaNode.getLayoutBorder(TOP) != 0
            || mYogaNode.getLayoutBorder(RIGHT) != 0
            || mYogaNode.getLayoutBorder(BOTTOM) != 0);
  }

  public int getLayoutBorder(YogaEdge edge) {
    return FastMath.round(mYogaNode.getLayoutBorder(edge));
  }

  public int getTouchExpansionBottom() {
    if (!shouldApplyTouchExpansion()) {
      return 0;
    }

    return FastMath.round(mNode.getTouchExpansion().get(YogaEdge.BOTTOM));
  }

  public int getTouchExpansionLeft() {
    if (!shouldApplyTouchExpansion()) {
      return 0;
    }

    return FastMath.round(resolveHorizontalEdges(mNode.getTouchExpansion(), YogaEdge.LEFT));
  }

  public int getTouchExpansionRight() {
    if (!shouldApplyTouchExpansion()) {
      return 0;
    }

    return FastMath.round(resolveHorizontalEdges(mNode.getTouchExpansion(), YogaEdge.RIGHT));
  }

  public int getTouchExpansionTop() {
    if (!shouldApplyTouchExpansion()) {
      return 0;
    }

    return FastMath.round(mNode.getTouchExpansion().get(YogaEdge.TOP));
  }

  private boolean shouldApplyTouchExpansion() {
    return mNode.getTouchExpansion() != null
        && mNode.getNodeInfo() != null
        && mNode.getNodeInfo().hasTouchEventHandlers();
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

  public int getLastHeightSpec() {
    return mLastHeightSpec;
  }

  public void setLastHeightSpec(int heightSpec) {
    mLastHeightSpec = heightSpec;
  }

  /**
   * The last value the measure funcion associated with this node {@link Component} returned for the
   * height. This is used together with {@link LithoLayoutResult#getLastHeightSpec()} to implement
   * measure caching.
   */
  public float getLastMeasuredHeight() {
    return mLastMeasuredHeight;
  }

  /**
   * Sets the last value the measure funcion associated with this node {@link Component} returned
   * for the height.
   */
  public void setLastMeasuredHeight(float lastMeasuredHeight) {
    mLastMeasuredHeight = lastMeasuredHeight;
  }

  /**
   * The last value the measure funcion associated with this node {@link Component} returned for the
   * width. This is used together with {@link LithoLayoutResult#getLastWidthSpec()} to implement
   * measure caching.
   */
  public float getLastMeasuredWidth() {
    return mLastMeasuredWidth;
  }

  /**
   * Sets the last value the measure funcion associated with this node {@link Component} returned
   * for the width.
   */
  public void setLastMeasuredWidth(float lastMeasuredWidth) {
    mLastMeasuredWidth = lastMeasuredWidth;
  }

  public void setDiffNode(@Nullable DiffNode diffNode) {
    mDiffNode = diffNode;
  }

  public void setCachedMeasuresValid(boolean isValid) {
    mCachedMeasuresValid = isValid;
  }

  public int getLastWidthSpec() {
    return mLastWidthSpec;
  }

  public boolean areCachedMeasuresValid() {
    return mCachedMeasuresValid;
  }

  public @Nullable DiffNode getDiffNode() {
    return mDiffNode;
  }

  public void setLastWidthSpec(int widthSpec) {
    mLastWidthSpec = widthSpec;
  }

  public YogaDirection recursivelyResolveLayoutDirection() {
    final YogaDirection direction = mYogaNode.getLayoutDirection();
    if (direction == YogaDirection.INHERIT) {
      throw new IllegalStateException("Direction cannot be resolved before layout calculation");
    }
    return direction;
  }

  @Override
  public @Nullable LithoRenderUnit getRenderUnit() {
    if (mContext.shouldReuseOutputs()) {
      if (mContentRenderUnit == null) {
        mContentRenderUnit = InternalNodeUtils.createContentRenderUnit(this);
      }
      return mContentRenderUnit;
    } else {
      return InternalNodeUtils.createContentRenderUnit(this);
    }
  }

  public @Nullable LithoRenderUnit getHostRenderUnit() {
    if (mContext.shouldReuseOutputs()) {
      if (mHostRenderUnit == null) {
        mHostRenderUnit = InternalNodeUtils.createHostRenderUnit(this);
      }
      return mHostRenderUnit;
    } else {
      return InternalNodeUtils.createHostRenderUnit(this);
    }
  }

  public @Nullable LithoRenderUnit getBackgroundRenderUnit() {
    if (mContext.shouldReuseOutputs()) {
      if (mBackgroundRenderUnit == null) {
        mBackgroundRenderUnit = InternalNodeUtils.createBackgroundRenderUnit(this);
      }
      return mBackgroundRenderUnit;
    } else {
      return InternalNodeUtils.createBackgroundRenderUnit(this);
    }
  }

  public @Nullable LithoRenderUnit getForegroundRenderUnit() {
    if (mContext.shouldReuseOutputs()) {
      if (mForegroundRenderUnit == null) {
        mForegroundRenderUnit = InternalNodeUtils.createForegroundRenderUnit(this);
      }
      return mForegroundRenderUnit;
    } else {
      return InternalNodeUtils.createForegroundRenderUnit(this);
    }
  }

  public @Nullable LithoRenderUnit getBorderRenderUnit() {
    if (mContext.shouldReuseOutputs()) {
      if (mBorderRenderUnit == null) {
        mBorderRenderUnit = InternalNodeUtils.createBorderRenderUnit(this);
      }
      return mBorderRenderUnit;
    } else {
      return InternalNodeUtils.createBorderRenderUnit(this);
    }
  }

  @Override
  public @Nullable Object getLayoutData() {
    return mLayoutData;
  }

  public void setLayoutData(@Nullable Object layoutData) {
    mLayoutData = layoutData;
  }

  @Override
  public int getChildrenCount() {
    return mChildren.size();
  }

  @Override
  public LithoLayoutResult getChildAt(int i) {
    return mChildren.get(i);
  }

  @Override
  public int getXForChildAtIndex(int index) {
    return mChildren.get(index).getX();
  }

  @Override
  public int getYForChildAtIndex(int index) {
    return mChildren.get(index).getY();
  }

  public void addChild(LithoLayoutResult child) {
    child.setParent(this);
    mChildren.add(child);
  }

  public int getChildCount() {
    return mChildren.size();
  }

  public @Nullable LithoLayoutResult getParent() {
    return mParent;
  }

  public void setParent(@Nullable LithoLayoutResult parent) {
    mParent = parent;
  }

  public YogaNode getYogaNode() {
    return mYogaNode;
  }

  boolean wasMeasured() {
    return mLastMeasuredWidth != DiffNode.UNSPECIFIED;
  }

  long measure(final int widthSpec, final int heightSpec) {

    if (mLayoutContext.isLayoutReleased()) {
      return 0;
    }

    final Component component = mNode.getTailComponent();

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSectionWithArgs("measure:" + component.getSimpleName())
          .arg("widthSpec", SizeSpec.toString(widthSpec))
          .arg("heightSpec", SizeSpec.toString(heightSpec))
          .arg("componentId", component.getId())
          .flush();
    }

    final Size size = new Size(Integer.MIN_VALUE, Integer.MIN_VALUE);

    try {

      measureInternal(widthSpec, heightSpec, size);

      if (size.width < 0 || size.height < 0) {
        throw new IllegalStateException(
            "MeasureOutput not set, Component is: "
                + component
                + " WidthSpec: "
                + MeasureSpecUtils.getMeasureSpecDescription(widthSpec)
                + " HeightSpec: "
                + MeasureSpecUtils.getMeasureSpecDescription(heightSpec)
                + " Measured width : "
                + size.width
                + " Measured Height: "
                + size.height);
      }

      if (getDiffNode() != null) {
        getDiffNode().setLastWidthSpec(widthSpec);
        getDiffNode().setLastHeightSpec(heightSpec);
        getDiffNode().setLastMeasuredWidth(size.width);
        getDiffNode().setLastMeasuredHeight(size.height);
      }

      return YogaMeasureOutput.make(size.width, size.height);

    } catch (Exception e) {
      size.width = 0;
      size.height = 0;
      ComponentUtils.handle(mNode.getTailComponentContext(), e);

      // If the exception is handled then return 0 size to continue layout.
      return YogaMeasureOutput.make(0, 0);
    } finally {

      // Record the last measured width, and height spec
      setLastMeasuredWidth(size.width);
      setLastMeasuredHeight(size.height);
      setLastWidthSpec(widthSpec);
      setLastHeightSpec(heightSpec);

      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }
  }

  protected void measureInternal(final int widthSpec, final int heightSpec, final Size size) {
    final boolean isTracing = ComponentsSystrace.isTracing();
    final LithoNode node = mNode;
    final Component component = node.getTailComponent();
    final ComponentContext componentScopedContext = node.getTailComponentContext();
    final DiffNode diffNode = areCachedMeasuresValid() ? getDiffNode() : null;

    // If diff node is set check if measurements from the previous pass can be reused
    if (diffNode != null
        && diffNode.getLastWidthSpec() == widthSpec
        && diffNode.getLastHeightSpec() == heightSpec
        && !component.shouldAlwaysRemeasure()) {

      size.width = (int) diffNode.getLastMeasuredWidth();
      size.height = (int) diffNode.getLastMeasuredHeight();

      // Measure the component
    } else {

      if (isTracing) {
        ComponentsSystrace.beginSection("onMeasure:" + component.getSimpleName());
      }
      try {
        final @Nullable Mountable<?> mountable = node.getMountable();
        if (mountable != null) {
          MeasureResult measureResult =
              mountable.measure(getContext(), widthSpec, heightSpec, mLayoutData);
          mLayoutData = measureResult.layoutData;
          size.width = measureResult.width;
          size.height = measureResult.height;
        } else {
          component.onMeasure(
              componentScopedContext,
              this,
              widthSpec,
              heightSpec,
              size,
              (InterStagePropsContainer) getLayoutData());
        }

      } finally {
        if (isTracing) {
          ComponentsSystrace.endSection();
        }
      }
    }
  }

  /**
   * This utility method checks if the {@param result} will mount a {@link View}. It returns true if
   * and only if the {@param result} will mount a {@link View}. If it returns {@code false} then the
   * result will either mount a {@link Drawable} or it is {@link NestedTreeHolderResult}, which will
   * not mount anything.
   *
   * @return {@code true} iff the result will mount a view.
   */
  public static boolean willMountView(LithoLayoutResult result) {
    if (result.mNode.getMountable() != null) {
      return result.mNode.getMountable().getRenderType() == RenderUnit.RenderType.VIEW;
    } else {
      final Component component = result.getNode().getTailComponent();
      return (component != null && component.getMountType() == Component.MountType.VIEW);
    }
  }
}
