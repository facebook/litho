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

package com.facebook.rendercore;

import static com.facebook.rendercore.Node.LayoutResult;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.RIGHT;
import static com.facebook.yoga.YogaEdge.TOP;

import android.content.Context;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import com.facebook.rendercore.RenderState.LayoutContext;
import com.facebook.rendercore.utils.LayoutUtils;
import com.facebook.yoga.YogaConfig;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaDisplay;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaMeasureMode;
import com.facebook.yoga.YogaMeasureOutput;
import com.facebook.yoga.YogaNode;
import com.facebook.yoga.YogaNodeFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Layout function for nodes that layout their children via Flexbox.
 *
 * <p>**Notes on rounding**:
 *
 * <ul>
 *   <li>- By default, Yoga rounds width and height to a pixel grid (YGRoundToPixelGrid). This means
 *       that even though width/height are returned as floats, they can be cast to int without
 *       concerns about non-integer values.
 *   <li>- However, padding is **not** automatically rounded: we match the behavior of Litho by
 *       using simple float rounding but this is probably leaving us open to rounding errors in edge
 *       cases: e.g. imagine padding resolved to 1.7px on the left and right sides and content of
 *       7px. Yoga may round to a width of 1.7 + 1.7 + 7 ~= 10px, but we round padding to 2px per
 *       side, leaving only 6px for content, potentially causing a re-measure.
 * </ul>
 */
public class YogaLayoutFunction {

  public static final YogaConfig DEFAULT_YOGA_CONFIG = com.facebook.yoga.YogaConfigFactory.create();

  static {
    DEFAULT_YOGA_CONFIG.setUseWebDefaults(true);
  }

  private YogaLayoutFunction() {}

  public static LayoutResult calculateLayout(
      LayoutContext context,
      Node node,
      YogaLayoutDataProvider yogaLayoutDataProvider,
      int widthSpec,
      int heightSpec) {
    final Context androidContext = context.getAndroidContext();

    RenderCoreSystrace.beginSection("CreateYogaNodes");

    final List<FlexboxLayoutResult> pendingSubTrees = new ArrayList<>();
    final YogaConfig yogaConfig =
        yogaLayoutDataProvider.getYogaConfig() != null
            ? yogaLayoutDataProvider.getYogaConfig()
            : DEFAULT_YOGA_CONFIG;

    final FlexboxLayoutResult layoutResult =
        buildTree(context, node, pendingSubTrees, yogaConfig, yogaLayoutDataProvider);
    layoutResult.setMeasureSpecs(widthSpec, heightSpec);
    RenderCoreSystrace.endSection();

    RenderCoreSystrace.beginSection("YogaLayoutFunction");

    final int widthSize = View.MeasureSpec.getSize(widthSpec);
    final int heightSize = View.MeasureSpec.getSize(heightSpec);

    final int widthMode = View.MeasureSpec.getMode(widthSpec);
    final int heightMode = View.MeasureSpec.getMode(heightSpec);

    final float widthToLayoutAgainst =
        (widthMode == View.MeasureSpec.UNSPECIFIED || widthMode == View.MeasureSpec.AT_MOST)
            ? YogaConstants.UNDEFINED
            : widthSize;

    final float heightToLayoutAgainst =
        (heightMode == View.MeasureSpec.UNSPECIFIED || heightMode == View.MeasureSpec.AT_MOST)
            ? YogaConstants.UNDEFINED
            : heightSize;
    final YogaRootLayoutParams yogLayoutParams =
        yogaLayoutDataProvider.getYogaRootLayoutParams(node);

    if (widthMode == View.MeasureSpec.EXACTLY) {
      layoutResult.mYogaNode.setWidth(widthSize);
    } else if (widthMode == View.MeasureSpec.AT_MOST) {
      layoutResult.mYogaNode.setMaxWidth(widthSize);

      if (yogLayoutParams != null) {
        if (yogLayoutParams.usePercentDimensAtRoot() && yogLayoutParams.hasPercentWidth()) {
          layoutResult.mYogaNode.setWidth(widthSize * 0.01f * yogLayoutParams.getWidthPercent());
        }
      }
    }

    if (heightMode == View.MeasureSpec.EXACTLY) {
      layoutResult.mYogaNode.setHeight(heightSize);
    } else if (heightMode == View.MeasureSpec.AT_MOST) {
      layoutResult.mYogaNode.setMaxHeight(heightSize);

      if (yogLayoutParams != null) {
        if (yogLayoutParams.usePercentDimensAtRoot() && yogLayoutParams.hasPercentHeight()) {
          layoutResult.mYogaNode.setHeight(heightSize * 0.01f * yogLayoutParams.getHeightPercent());
        }
      }
    }

    if (layoutResult.mYogaNode.getLayoutDirection() == YogaDirection.INHERIT
        && LayoutUtils.isLayoutDirectionRTL(androidContext)) {
      layoutResult.mYogaNode.setDirection(YogaDirection.RTL);
    }
    RenderCoreSystrace.beginSection("YogaCalculate");
    layoutResult.mYogaNode.calculateLayout(widthToLayoutAgainst, heightToLayoutAgainst);
    RenderCoreSystrace.endSection();

    for (FlexboxLayoutResult flexboxLayoutResult : pendingSubTrees) {
      flexboxLayoutResult.measureIfNeeded();
    }
    RenderCoreSystrace.endSection(); // YogaLayoutFunction

    return layoutResult;
  }

  private static <RenderContext> FlexboxLayoutResult buildTree(
      final LayoutContext<RenderContext> context,
      final Node node,
      List<FlexboxLayoutResult> pendingSubtrees,
      YogaConfig yogaConfig,
      YogaLayoutDataProvider yogaLayoutDataProvider) {

    final LayoutResult cachedResult = context.getLayoutCache().get(node);
    if (cachedResult != null && cachedResult instanceof FlexboxLayoutResult) {
      return buildTreeFromCache((FlexboxLayoutResult) cachedResult, context, pendingSubtrees);
    }

    final FlexboxLayoutResult layoutResult;
    final YogaNode yogaNode = YogaNodeFactory.create(yogaConfig);

    yogaLayoutDataProvider.applyYogaPropsFromNode(node, context, yogaNode);
    yogaLayoutDataProvider.applyYogaPropsFromLayoutParams(node, context, yogaNode);

    if (yogaNode.getDisplay() == YogaDisplay.NONE) {
      final YogaNode emptyYogaNode = YogaNodeFactory.create(yogaConfig);
      emptyYogaNode.setWidth(0f);
      emptyYogaNode.setHeight(0f);
      return new FlexboxLayoutResult(context, node, emptyYogaNode, null, null);
    }

    if (yogaLayoutDataProvider.nodeCanMeasure(node)) {
      layoutResult =
          new FlexboxLayoutResult(
              context,
              node,
              yogaNode,
              new MeasureImpl() {
                @Override
                public LayoutResult measure(
                    LayoutContext layoutContext, int widthSpec, int heightSpec) {
                  return node.calculateLayout(layoutContext, widthSpec, heightSpec);
                }
              },
              null);
      pendingSubtrees.add(layoutResult);
      yogaNode.setMeasureFunction(layoutResult);
    } else {
      layoutResult =
          new FlexboxLayoutResult(
              context,
              node,
              yogaNode,
              null,
              yogaLayoutDataProvider.getRenderUnitForNode(node, context));
      context.getLayoutCache().put(node, layoutResult);

      final List<? extends Node> children = yogaLayoutDataProvider.getYogaChildren(node);
      for (int i = 0; i < children.size(); i++) {
        final Node child = children.get(i);
        final FlexboxLayoutResult childLayoutResult =
            buildTree(context, child, pendingSubtrees, yogaConfig, yogaLayoutDataProvider);
        // We already checked if this node was NONE when we created it but we want to also check
        // here so that we don't add it to the hierarchy at all and it doesn't participate in
        // grow/shrink behaviours.
        if (childLayoutResult.mYogaNode.getDisplay() != YogaDisplay.NONE) {
          yogaNode.addChildAt(childLayoutResult.mYogaNode, layoutResult.mYogaNode.getChildCount());
          layoutResult.addChild(childLayoutResult);
        }
      }
    }

    return layoutResult;
  }

  private static FlexboxLayoutResult buildTreeFromCache(
      FlexboxLayoutResult cachedResult,
      LayoutContext layoutContext,
      List<FlexboxLayoutResult> pendingSubtrees) {
    RenderCoreSystrace.beginSection("CloneYogaTree");
    YogaNode clonedYogaNode = cachedResult.mYogaNode.cloneWithChildren();
    RenderCoreSystrace.endSection();
    return registerClonedNodesRecursively(
        cachedResult, clonedYogaNode, layoutContext, pendingSubtrees);
  }

  private static FlexboxLayoutResult registerClonedNodesRecursively(
      FlexboxLayoutResult cachedResult,
      YogaNode clonedYogaNode,
      LayoutContext layoutContext,
      List<FlexboxLayoutResult> pendingSubtrees) {
    final Node node = cachedResult.mNode;
    final FlexboxLayoutResult result =
        new FlexboxLayoutResult(
            layoutContext, node, clonedYogaNode, cachedResult.mMeasure, cachedResult.mRenderUnit);

    // Re-cache this only if it's not a leaf. We want the leaves to rely on the delegate behavior
    // for caching.
    if (cachedResult.mMeasure == null) {
      layoutContext.getLayoutCache().put(node, result);
      for (int i = 0; i < cachedResult.getChildrenCount(); i++) {
        LayoutResult childResult = cachedResult.getChildAt(i);
        FlexboxLayoutResult child =
            registerClonedNodesRecursively(
                (FlexboxLayoutResult) childResult,
                clonedYogaNode.getChildAt(i),
                layoutContext,
                pendingSubtrees);

        result.addChild(child);
      }
    }

    if (result.mMeasure != null) {
      pendingSubtrees.add(result);
      clonedYogaNode.setMeasureFunction(result);
    }

    return result;
  }

  private static int round(float val) {
    if (val > 0) {
      return (int) (val + 0.5);
    } else {
      return (int) (val - 0.5);
    }
  }

  private static int makeSizeSpecFromYogaSpec(float yogaSize, YogaMeasureMode yogaMode) {
    switch (yogaMode) {
      case EXACTLY:
        return View.MeasureSpec.makeMeasureSpec(round(yogaSize), View.MeasureSpec.EXACTLY);
      case UNDEFINED:
        return View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
      case AT_MOST:
        return View.MeasureSpec.makeMeasureSpec(round(yogaSize), View.MeasureSpec.AT_MOST);
      default:
        throw new IllegalArgumentException("Unexpected YogaMeasureMode: " + yogaMode);
    }
  }

  private static int getContentLayoutWidth(YogaNode yogaNode) {
    // See class-level javadocs for notes about rounding
    return round(yogaNode.getLayoutWidth())
        - round(yogaNode.getLayoutPadding(LEFT))
        - round(yogaNode.getLayoutPadding(RIGHT));
  }

  private static int getContentLayoutHeight(YogaNode yogaNode) {
    return round(yogaNode.getLayoutHeight())
        - round(yogaNode.getLayoutPadding(TOP))
        - round(yogaNode.getLayoutPadding(BOTTOM));
  }

  private interface MeasureImpl {
    LayoutResult measure(LayoutContext layoutContext, int widthSpec, int heightSpec);
  }

  private static class FlexboxLayoutResult implements LayoutResult, YogaMeasureFunction {

    private final Node mNode;
    @Nullable private final RenderUnit mRenderUnit;
    private final YogaNode mYogaNode;
    private final List<LayoutResult> mChildren;
    private int mWidthSpec;
    private int mHeightSpec;
    private boolean mIsRoot;
    @Nullable private LayoutResult mDelegate;
    @Nullable private final MeasureImpl mMeasure;
    private long mLastMeasuredDimensions = Long.MIN_VALUE;
    private final LayoutContext mLayoutContext;

    FlexboxLayoutResult(
        LayoutContext layoutContext,
        Node node,
        YogaNode result,
        @Nullable MeasureImpl measure,
        @Nullable RenderUnit renderUnit) {
      mNode = node;
      mRenderUnit = renderUnit;
      mYogaNode = result;
      mLayoutContext = layoutContext;
      mChildren = new ArrayList<>();
      mIsRoot = false;
      mMeasure = measure;
    }

    @Nullable
    @Override
    public RenderUnit getRenderUnit() {
      if (mDelegate != null) {
        return mDelegate.getRenderUnit();
      }

      return mRenderUnit;
    }

    @Nullable
    @Override
    public Object getLayoutData() {
      if (mDelegate != null) {
        return mDelegate.getLayoutData();
      }
      return mYogaNode;
    }

    @Override
    public int getChildrenCount() {
      if (mDelegate != null) {
        return mDelegate.getChildrenCount();
      }
      return mChildren.size();
    }

    @Override
    public Node.LayoutResult getChildAt(int index) {
      if (mDelegate != null) {
        return mDelegate.getChildAt(index);
      }
      return mChildren.get(index);
    }

    @Override
    @Px
    public int getXForChildAtIndex(int index) {
      if (mDelegate != null) {
        return mDelegate.getXForChildAtIndex(index);
      }
      return (int) mYogaNode.getChildAt(index).getLayoutX();
    }

    @Override
    @Px
    public int getYForChildAtIndex(int index) {
      if (mDelegate != null) {
        return mDelegate.getYForChildAtIndex(index);
      }
      return (int) mYogaNode.getChildAt(index).getLayoutY();
    }

    @Px
    @Override
    public int getWidth() {
      if (mDelegate != null) {
        return mDelegate.getWidth()
            + round(mYogaNode.getLayoutPadding(LEFT))
            + round(mYogaNode.getLayoutPadding(RIGHT));
      }

      return (int) mYogaNode.getLayoutWidth();
    }

    @Px
    @Override
    public int getHeight() {
      if (mDelegate != null) {
        return mDelegate.getHeight()
            + round(mYogaNode.getLayoutPadding(TOP))
            + round(mYogaNode.getLayoutPadding(BOTTOM));
      }

      return (int) mYogaNode.getLayoutHeight();
    }

    @Px
    @Override
    public int getPaddingTop() {
      if (mDelegate != null) {
        return mDelegate.getPaddingTop();
      }
      return round(mYogaNode.getLayoutPadding(TOP));
    }

    @Px
    @Override
    public int getPaddingRight() {
      if (mDelegate != null) {
        return mDelegate.getPaddingRight();
      }
      return round(mYogaNode.getLayoutPadding(RIGHT));
    }

    @Px
    @Override
    public int getPaddingBottom() {
      if (mDelegate != null) {
        return mDelegate.getPaddingBottom();
      }
      return round(mYogaNode.getLayoutPadding(BOTTOM));
    }

    @Px
    @Override
    public int getPaddingLeft() {
      if (mDelegate != null) {
        return mDelegate.getPaddingLeft();
      }
      return round(mYogaNode.getLayoutPadding(LEFT));
    }

    public void addChild(LayoutResult layoutResult) {
      mChildren.add(layoutResult);
    }

    public void setMeasureSpecs(int widthSpec, int heightSpec) {
      mIsRoot = true;
      mWidthSpec = widthSpec;
      mHeightSpec = heightSpec;
    }

    @Override
    public int getWidthSpec() {
      return mIsRoot
          ? mWidthSpec
          : View.MeasureSpec.makeMeasureSpec(
              (int) mYogaNode.getLayoutWidth(), View.MeasureSpec.EXACTLY);
    }

    @Override
    public int getHeightSpec() {
      return mIsRoot
          ? mHeightSpec
          : View.MeasureSpec.makeMeasureSpec(
              (int) mYogaNode.getLayoutHeight(), View.MeasureSpec.EXACTLY);
    }

    public void measureIfNeeded() {

      if (mMeasure != null) {
        final int measuredContentWidth = getContentLayoutWidth(mYogaNode);
        final int measuredContentHeight = getContentLayoutHeight(mYogaNode);
        if (mLastMeasuredDimensions == Long.MIN_VALUE
            || (int) YogaMeasureOutput.getWidth(mLastMeasuredDimensions) != measuredContentWidth
            || (int) YogaMeasureOutput.getHeight(mLastMeasuredDimensions) != measuredContentHeight
            || mDelegate == null) {
          mDelegate =
              mMeasure.measure(
                  mLayoutContext,
                  View.MeasureSpec.makeMeasureSpec(measuredContentWidth, View.MeasureSpec.EXACTLY),
                  View.MeasureSpec.makeMeasureSpec(
                      measuredContentHeight, View.MeasureSpec.EXACTLY));
          mLastMeasuredDimensions =
              YogaMeasureOutput.make(mDelegate.getWidth(), mDelegate.getHeight());
        }
      }
    }

    @Override
    public long measure(
        YogaNode node,
        float width,
        YogaMeasureMode widthMode,
        float height,
        YogaMeasureMode heightMode) {
      final int widthSpec = makeSizeSpecFromYogaSpec(width, widthMode);
      final int heightSpec = makeSizeSpecFromYogaSpec(height, heightMode);
      mDelegate = mMeasure.measure(mLayoutContext, widthSpec, heightSpec);
      mLastMeasuredDimensions = YogaMeasureOutput.make(mDelegate.getWidth(), mDelegate.getHeight());

      return mLastMeasuredDimensions;
    }
  }
}
