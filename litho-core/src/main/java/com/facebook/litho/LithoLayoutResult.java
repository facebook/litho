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

import static com.facebook.litho.Component.isMountSpec;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.RIGHT;
import static com.facebook.yoga.YogaEdge.TOP;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import com.facebook.litho.drawable.BorderColorDrawable;
import com.facebook.rendercore.FastMath;
import com.facebook.rendercore.LayoutContext;
import com.facebook.rendercore.LayoutResult;
import com.facebook.rendercore.MeasureResult;
import com.facebook.rendercore.Mountable;
import com.facebook.rendercore.primitives.Primitive;
import com.facebook.rendercore.primitives.utils.EquivalenceUtils;
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
  private final ComponentContext mContext;

  protected final LithoNode mNode;

  private @Nullable LayoutResult mDelegate = null;

  private final List<LithoLayoutResult> mChildren = new ArrayList<>();
  private final YogaNode mYogaNode;

  private boolean mCachedMeasuresValid;
  private boolean mIsCachedLayout;
  private @Nullable DiffNode mDiffNode;

  private int mWidthSpec = DiffNode.UNSPECIFIED;

  private int mHeightSpec = DiffNode.UNSPECIFIED;

  private long mLastMeasuredSize = Long.MIN_VALUE;

  private @Nullable LithoRenderUnit mContentRenderUnit;
  private @Nullable LithoRenderUnit mHostRenderUnit;
  private @Nullable LithoRenderUnit mBackgroundRenderUnit;
  private @Nullable LithoRenderUnit mForegroundRenderUnit;
  private @Nullable LithoRenderUnit mBorderRenderUnit;

  private @Nullable Object mLayoutData;

  private boolean mWasMeasured = false;
  private boolean mMeasureHadExceptions = false;
  private float mWidthFromStyle = YogaConstants.UNDEFINED;
  private float mHeightFromStyle = YogaConstants.UNDEFINED;

  public LithoLayoutResult(
      final ComponentContext c,
      final LithoNode node,
      final YogaNode yogaNode,
      final float widthFromStyle,
      final float heightFromStyle) {
    mContext = c;
    mNode = node;
    mYogaNode = yogaNode;
    mWidthFromStyle = widthFromStyle;
    mHeightFromStyle = heightFromStyle;

    /*

     Ideally the layout data should be created when measure is called on the mount spec or
     mountable component, but because of the current implementation of mount specs, and the way
     Yoga works it a possibility that measure may not be called, and a MountSpec [may] require
     inter stage props, then it is necessary to have a non-null InterStagePropsContainer even if
     the values are uninitialised. Otherwise it will lead to NPEs.

     This should get cleaned up once the implementation is general enough for MountableComponents.

    */
    final Component component = node.getTailComponent();
    if (component instanceof SpecGeneratedComponent) {
      mLayoutData = ((SpecGeneratedComponent) component).createInterStagePropsContainer();
    }
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
    return mWidthSpec;
  }

  @Override
  public int getHeightSpec() {
    return mHeightSpec;
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

  public float getContentWidth() {
    return YogaMeasureOutput.getWidth(mLastMeasuredSize);
  }

  public float getContentHeight() {
    return YogaMeasureOutput.getHeight(mLastMeasuredSize);
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

  @Nullable
  Rect getExpandedTouchBounds() {
    if (!getNode().hasTouchExpansion()) {
      return null;
    }

    final int left = getTouchExpansionLeft();
    final int top = getTouchExpansionTop();
    final int right = getTouchExpansionRight();
    final int bottom = getTouchExpansionBottom();
    if (left == 0 && top == 0 && right == 0 && bottom == 0) {
      return null;
    }

    return new Rect(left, top, right, bottom);
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

  public void setSizeSpec(int widthSpec, int heightSpec) {
    mWidthSpec = widthSpec;
    mHeightSpec = heightSpec;
  }

  public void setDiffNode(@Nullable DiffNode diffNode) {
    mDiffNode = diffNode;
  }

  public void setCachedMeasuresValid(boolean isValid) {
    mCachedMeasuresValid = isValid;
  }

  public boolean areCachedMeasuresValid() {
    return mCachedMeasuresValid;
  }

  public float getWidthFromStyle() {
    return mWidthFromStyle;
  }

  public float getHeightFromStyle() {
    return mHeightFromStyle;
  }

  public @Nullable DiffNode getDiffNode() {
    return mDiffNode;
  }

  public YogaDirection recursivelyResolveLayoutDirection() {
    final YogaDirection direction = mYogaNode.getLayoutDirection();
    if (direction == YogaDirection.INHERIT) {
      throw new IllegalStateException("Direction cannot be resolved before layout calculation");
    }
    return direction;
  }

  public @Nullable LayoutResult getDelegate() {
    return mDelegate;
  }

  @Override
  public @Nullable LithoRenderUnit getRenderUnit() {
    // Unimplemented.
    return null;
  }

  public @Nullable LithoRenderUnit getContentRenderUnit() {
    if (mContext.shouldReuseOutputs()) {
      return mContentRenderUnit;
    } else {
      return LithoNodeUtils.createContentRenderUnit(mNode, mCachedMeasuresValid, mDiffNode);
    }
  }

  public @Nullable LithoRenderUnit getHostRenderUnit() {
    if (mContext.shouldReuseOutputs()) {
      return mHostRenderUnit;
    } else {
      return LithoNodeUtils.createHostRenderUnit(getNode());
    }
  }

  public @Nullable LithoRenderUnit getBackgroundRenderUnit() {
    if (mContext.shouldReuseOutputs()) {
      return mBackgroundRenderUnit;
    } else {
      return LithoNodeUtils.createBackgroundRenderUnit(mNode, getWidth(), getHeight(), mDiffNode);
    }
  }

  public @Nullable LithoRenderUnit getForegroundRenderUnit() {
    if (mContext.shouldReuseOutputs()) {
      return mForegroundRenderUnit;
    } else {
      return LithoNodeUtils.createForegroundRenderUnit(mNode, getWidth(), getHeight(), mDiffNode);
    }
  }

  public @Nullable LithoRenderUnit getBorderRenderUnit() {
    if (mContext.shouldReuseOutputs()) {
      return mBorderRenderUnit;
    } else if (shouldDrawBorders()) {
      return LithoNodeUtils.createBorderRenderUnit(
          mNode, createBorderColorDrawable(this), getWidth(), getHeight(), mDiffNode);
    } else {
      return null;
    }
  }

  public LithoLayoutResult copyLayoutResult(LithoNode node, YogaNode yogaNode) {
    LithoLayoutResult copiedResult = node.createLayoutResult(yogaNode, null);
    copiedResult.mIsCachedLayout = true;
    copiedResult.mCachedMeasuresValid = true;
    copiedResult.mWidthSpec = mWidthSpec;
    copiedResult.mHeightSpec = mHeightSpec;
    copiedResult.mLastMeasuredSize = mLastMeasuredSize;
    copiedResult.mDelegate = mDelegate;
    copiedResult.mLayoutData = mLayoutData;
    copiedResult.mWidthFromStyle = mWidthFromStyle;
    copiedResult.mHeightFromStyle = mHeightFromStyle;
    if (mContext.shouldReuseOutputs()) {
      copiedResult.mContentRenderUnit = mContentRenderUnit;
      copiedResult.mHostRenderUnit = mHostRenderUnit;
      copiedResult.mBackgroundRenderUnit = mBackgroundRenderUnit;
      copiedResult.mForegroundRenderUnit = mForegroundRenderUnit;
      copiedResult.mBorderRenderUnit = mBorderRenderUnit;
    }
    return copiedResult;
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
    mChildren.add(child);
  }

  public int getChildCount() {
    return mChildren.size();
  }

  public YogaNode getYogaNode() {
    return mYogaNode;
  }

  public static LayoutContext getLayoutContextFromYogaNode(YogaNode yogaNode) {
    return ((Pair<LayoutContext, LithoLayoutResult>) yogaNode.getData()).first;
  }

  public static LithoLayoutResult getLayoutResultFromYogaNode(YogaNode yogaNode) {
    return ((Pair<LayoutContext, LithoLayoutResult>) yogaNode.getData()).second;
  }

  /**
   * Since layout data like the layout context and the diff node are not required after layout
   * calculation they can be released to free up memory.
   */
  void releaseLayoutPhaseData() {
    setDiffNode(null);
    getYogaNode().setData(null);
    for (int i = 0, count = getChildCount(); i < count; i++) {
      getChildAt(i).releaseLayoutPhaseData();
    }
  }

  boolean wasMeasured() {
    return mWasMeasured;
  }

  boolean measureHadExceptions() {
    return mMeasureHadExceptions;
  }

  void setMeasureHadExceptions(boolean hadException) {
    mMeasureHadExceptions = hadException;
  }

  MeasureResult measure(
      final LayoutContext<LithoRenderContext> context, final int widthSpec, final int heightSpec) {

    final boolean isTracing = ComponentsSystrace.isTracing();
    MeasureResult size;

    mWasMeasured = true;

    if (context.getRenderContext().lithoLayoutContext.isFutureReleased()) {

      // If layout is released then skip measurement
      size = MeasureResult.error();
    } else {

      final Component component = mNode.getTailComponent();

      if (isTracing) {
        ComponentsSystrace.beginSectionWithArgs("measure:" + component.getSimpleName())
            .arg("widthSpec", SizeSpec.toString(widthSpec))
            .arg("heightSpec", SizeSpec.toString(heightSpec))
            .arg("componentId", component.getId())
            .flush();
      }

      try {
        size = measureInternal(context, widthSpec, heightSpec);
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
      } catch (Exception e) {

        // Handle then exception
        ComponentUtils.handle(mNode.getTailComponentContext(), e);

        // If the exception is handled then return 0 size to continue layout.
        size = MeasureResult.error();
      }
    }

    // Record the last measured width, and height spec
    mWidthSpec = widthSpec;
    mHeightSpec = heightSpec;
    if (!mNode.getTailComponentContext().shouldCacheLayouts()) {
      mLastMeasuredSize = YogaMeasureOutput.make(size.width, size.height);
    }

    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    mMeasureHadExceptions = size.mHadExceptions;

    return size;
  }

  protected MeasureResult measureInternal(
      final LayoutContext<LithoRenderContext> context, final int widthSpec, final int heightSpec) {
    final boolean isTracing = ComponentsSystrace.isTracing();
    final LithoNode node = mNode;
    final Component component = node.getTailComponent();
    final ComponentContext componentScopedContext = node.getTailComponentContext();
    final DiffNode diffNode = areCachedMeasuresValid() ? getDiffNode() : null;

    final int width;
    final int height;
    final @Nullable LayoutResult delegate;
    final @Nullable Object layoutData;

    // If diff node is set check if measurements from the previous pass can be reused
    if (diffNode != null
        && diffNode.getLastWidthSpec() == widthSpec
        && diffNode.getLastHeightSpec() == heightSpec
        && !shouldAlwaysRemeasure(component)) {

      width = (int) diffNode.getLastMeasuredWidth();
      height = (int) diffNode.getLastMeasuredHeight();
      layoutData = diffNode.getLayoutData();
      delegate = diffNode.getDelegate();

      // Measure the component
    } else {

      if (isTracing) {
        ComponentsSystrace.beginSection("onMeasure:" + component.getSimpleName());
      }
      try {
        final @Nullable Mountable<?> mountable = node.getMountable();
        final @Nullable Primitive primitive = node.getPrimitive();
        // measure Mountable
        if (mountable != null) {
          context.setPreviousLayoutDataForCurrentNode(mLayoutData);
          context.setLayoutContextExtraData(new LithoLayoutContextExtraData(mYogaNode));
          delegate = mountable.calculateLayout(context, widthSpec, heightSpec);
          width = delegate.getWidth();
          height = delegate.getHeight();
          layoutData = delegate.getLayoutData();

        }
        // measure Primitive
        else if (primitive != null) {
          context.setPreviousLayoutDataForCurrentNode(mLayoutData);
          context.setLayoutContextExtraData(new LithoLayoutContextExtraData(mYogaNode));
          delegate = primitive.calculateLayout((LayoutContext) context, widthSpec, heightSpec);
          width = delegate.getWidth();
          height = delegate.getHeight();
          layoutData = delegate.getLayoutData();

        }
        // measure Mount Spec
        else {
          final Size size = new Size(Integer.MIN_VALUE, Integer.MIN_VALUE);
          // If the Layout Result was cached, but the size specs changed, then layout data
          // will be mutated. To avoid that create new (layout data) interstage props container
          // for mount specs to avoid mutating the currently mount layout data.
          layoutData = ((SpecGeneratedComponent) component).createInterStagePropsContainer();
          ((SpecGeneratedComponent) component)
              .onMeasure(
                  componentScopedContext,
                  this,
                  widthSpec,
                  heightSpec,
                  size,
                  (InterStagePropsContainer) layoutData);

          delegate = null;
          width = size.width;
          height = size.height;
        }

        // If layout data has changed then content render unit should be recreated
        if (!EquivalenceUtils.hasEquivalentFields(mLayoutData, layoutData)) {
          mContentRenderUnit = null;
        }

      } finally {
        if (isTracing) {
          ComponentsSystrace.endSection();
        }
      }
    }

    mDelegate = delegate;
    mLayoutData = layoutData;

    return new MeasureResult(width, height, layoutData);
  }

  public void onBoundsDefined() {
    final ComponentContext context = getNode().getTailComponentContext();
    final Component component = getNode().getTailComponent();

    if (!context.shouldCacheLayouts()) {
      if (mLastMeasuredSize == Long.MIN_VALUE) {
        mLastMeasuredSize = YogaMeasureOutput.make(getWidth(), getHeight());
      }
      return;
    }

    boolean hasSizeChanged =
        YogaMeasureOutput.getWidth(mLastMeasuredSize) != getWidth()
            || YogaMeasureOutput.getHeight(mLastMeasuredSize) != getHeight();

    if (isMountSpec(component)
        && (component instanceof SpecGeneratedComponent)
        && (!mIsCachedLayout || hasSizeChanged)) {

      // Invoke onBoundsDefined for all MountSpecs
      final SpecGeneratedComponent specGenComponent = (SpecGeneratedComponent) component;

      final boolean isTracing = ComponentsSystrace.isTracing();
      if (isTracing) {
        ComponentsSystrace.beginSection("onBoundsDefined:" + component.getSimpleName());
      }

      final @Nullable InterStagePropsContainer layoutData;

      // If the Layout Result was cached, but the size has changed, then interstage props container
      // (layout data) could be mutated when @OnBoundsDefined is invoked. To avoid that create new
      // interstage props container (layout data), and copy over the current values.
      if (mIsCachedLayout) {
        layoutData = specGenComponent.createInterStagePropsContainer();
        if (layoutData != null && mLayoutData != null) {
          specGenComponent.copyInterStageImpl(layoutData, (InterStagePropsContainer) mLayoutData);
        }
      } else {
        layoutData = (InterStagePropsContainer) mLayoutData;
      }

      try {
        specGenComponent.onBoundsDefined(context, this, layoutData);
      } catch (Exception e) {
        ComponentUtils.handleWithHierarchy(context, component, e);
        setMeasureHadExceptions(true);
      } finally {
        if (isTracing) {
          ComponentsSystrace.endSection();
        }
      }

      // If layout data has changed then content render unit should be recreated
      if (!EquivalenceUtils.hasEquivalentFields(mLayoutData, layoutData)) {
        mContentRenderUnit = null;
      }

      mLayoutData = layoutData;

      if (!wasMeasured()) {
        mWasMeasured = true;
        mWidthSpec = MeasureSpecUtils.exactly(getWidth());
        mHeightSpec = MeasureSpecUtils.exactly(getHeight());
      }

    } else if ((Component.isMountable(component) || Component.isPrimitive(component))
        && (mDelegate == null || (mIsCachedLayout && hasSizeChanged))) {

      // Check if we need to run measure for Mountable or Primitive that was skipped due to with
      // fixed size
      final int width =
          getWidth()
              - getPaddingRight()
              - getPaddingLeft()
              - getLayoutBorder(YogaEdge.RIGHT)
              - getLayoutBorder(YogaEdge.LEFT);
      final int height =
          getHeight()
              - getPaddingTop()
              - getPaddingBottom()
              - getLayoutBorder(YogaEdge.TOP)
              - getLayoutBorder(YogaEdge.BOTTOM);
      final LayoutContext layoutContext =
          LithoLayoutResult.getLayoutContextFromYogaNode(getYogaNode());
      measure(layoutContext, MeasureSpecUtils.exactly(width), MeasureSpecUtils.exactly(height));
    }

    mLastMeasuredSize = YogaMeasureOutput.make(getWidth(), getHeight());

    // Reuse or recreate additional outputs. Outputs are recreated if the size has changed
    if (mContext.shouldReuseOutputs()) {
      if (mContentRenderUnit == null) {
        mContentRenderUnit =
            LithoNodeUtils.createContentRenderUnit(mNode, mCachedMeasuresValid, mDiffNode);
      }
      if (mHostRenderUnit == null) {
        mHostRenderUnit = LithoNodeUtils.createHostRenderUnit(getNode());
      }
      if (hasSizeChanged || mBackgroundRenderUnit == null) {
        mBackgroundRenderUnit =
            LithoNodeUtils.createBackgroundRenderUnit(mNode, getWidth(), getHeight(), mDiffNode);
      }
      if (hasSizeChanged || mForegroundRenderUnit == null) {
        mForegroundRenderUnit =
            LithoNodeUtils.createForegroundRenderUnit(mNode, getWidth(), getHeight(), mDiffNode);
      }
      if (shouldDrawBorders() && (hasSizeChanged || mBorderRenderUnit == null)) {
        mBorderRenderUnit =
            LithoNodeUtils.createBorderRenderUnit(
                mNode, createBorderColorDrawable(this), getWidth(), getHeight(), mDiffNode);
      }
    }
  }

  private boolean shouldAlwaysRemeasure(Component component) {
    if (component instanceof SpecGeneratedComponent) {
      return ((SpecGeneratedComponent) component).shouldAlwaysRemeasure();
    } else {
      return false;
    }
  }

  private static BorderColorDrawable createBorderColorDrawable(LithoLayoutResult result) {
    final LithoNode node = result.getNode();
    final boolean isRtl = result.recursivelyResolveLayoutDirection() == YogaDirection.RTL;
    final float[] borderRadius = node.getBorderRadius();
    final int[] borderColors = node.getBorderColors();
    final YogaEdge leftEdge = isRtl ? YogaEdge.RIGHT : YogaEdge.LEFT;
    final YogaEdge rightEdge = isRtl ? YogaEdge.LEFT : YogaEdge.RIGHT;

    return new BorderColorDrawable.Builder()
        .pathEffect(node.getBorderPathEffect())
        .borderLeftColor(Border.getEdgeColor(borderColors, leftEdge))
        .borderTopColor(Border.getEdgeColor(borderColors, YogaEdge.TOP))
        .borderRightColor(Border.getEdgeColor(borderColors, rightEdge))
        .borderBottomColor(Border.getEdgeColor(borderColors, YogaEdge.BOTTOM))
        .borderLeftWidth(result.getLayoutBorder(leftEdge))
        .borderTopWidth(result.getLayoutBorder(YogaEdge.TOP))
        .borderRightWidth(result.getLayoutBorder(rightEdge))
        .borderBottomWidth(result.getLayoutBorder(YogaEdge.BOTTOM))
        .borderRadius(borderRadius)
        .build();
  }
}
