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

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static com.facebook.litho.Component.isLayoutSpecWithSizeSpec;
import static com.facebook.litho.Component.isMountable;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.LayoutContext;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaNode;

@Nullsafe(Nullsafe.Mode.LOCAL)
class Layout {

  static @Nullable LithoLayoutResult measureTree(
      final LayoutStateContext layoutStateContext,
      final Context androidContext,
      final @Nullable LithoNode node,
      final int widthSpec,
      final int heightSpec,
      final @Nullable PerfEvent layoutStatePerfEvent) {
    if (node == null) {
      return null;
    }

    if (layoutStatePerfEvent != null) {
      layoutStatePerfEvent.markerPoint("start_measure");
    }

    final LithoLayoutResult result;

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("measureTree:" + node.getHeadComponent().getSimpleName());
    }

    final LayoutContext<LithoRenderContext> context =
        new LayoutContext<>(
            androidContext, new LithoRenderContext(layoutStateContext), 0, null, null);

    result = node.calculateLayout(context, widthSpec, heightSpec);

    if (isTracing) {
      ComponentsSystrace.endSection(/* measureTree */ );
    }

    if (layoutStatePerfEvent != null) {
      layoutStatePerfEvent.markerPoint("end_measure");
    }

    return result;
  }

  private static @Nullable LithoLayoutResult measureNestedTree(
      final LayoutStateContext layoutStateContext,
      ComponentContext parentContext,
      final NestedTreeHolderResult holderResult,
      final int widthSpec,
      final int heightSpec) {

    // 1. Check if current layout result is compatible with size spec and can be reused or not
    final @Nullable LithoLayoutResult currentLayout = holderResult.getNestedResult();
    final NestedTreeHolder node = holderResult.getNode();
    final Component component = node.getTailComponent();

    if (currentLayout != null
        && MeasureComparisonUtils.hasCompatibleSizeSpec(
            currentLayout.getLastWidthSpec(),
            currentLayout.getLastHeightSpec(),
            widthSpec,
            heightSpec,
            currentLayout.getLastMeasuredWidth(),
            currentLayout.getLastMeasuredHeight())) {
      return currentLayout;
    }

    // 2. Check if cached layout result is compatible and can be reused or not.
    final @Nullable LithoLayoutResult cachedLayout =
        consumeCachedLayout(layoutStateContext, node, holderResult, widthSpec, heightSpec);

    if (cachedLayout != null) {
      return cachedLayout;
    }

    // 3. If component is not using OnCreateLayoutWithSizeSpec, we don't have to resolve it again
    // and we can simply re-measure the tree. This is for cases where component was measured with
    // Component.measure API but we could not find the cached layout result or cached layout result
    // was not compatible with given size spec.
    if (currentLayout != null && !isLayoutSpecWithSizeSpec(component)) {
      return measureTree(
          layoutStateContext,
          currentLayout.getContext().getAndroidContext(),
          currentLayout.getNode(),
          widthSpec,
          heightSpec,
          null);
    }

    // 4. If current layout result is not available or component uses OnCreateLayoutWithSizeSpec
    // then resolve the tree and measure it. At this point we know that current layout result and
    // cached layout result are not available or are not compatible with given size spec.

    // NestedTree is used for two purposes i.e for components measured using Component.measure API
    // and for components which are OnCreateLayoutWithSizeSpec.
    // For components measured with measure API, we want to reuse the same global key calculated
    // during measure API call and for that we are using the cached node and accessing the global
    // key from it since NestedTreeHolder will have incorrect global key for it.
    final String globalKeyToReuse =
        isLayoutSpecWithSizeSpec(component)
            ? node.getTailComponentKey()
            : Preconditions.checkNotNull(node.getCachedNode()).getTailComponentKey();

    // 4.a Apply state updates early for layout phase
    layoutStateContext.getTreeState().applyStateUpdatesEarly(parentContext, component, null, true);

    final CalculationStateContext prevContext = parentContext.getCalculationStateContext();

    try {
      final RenderStateContext nestedRsc =
          new RenderStateContext(
              layoutStateContext.getCache(),
              layoutStateContext.getTreeState(),
              layoutStateContext.getLayoutVersion(),
              null,
              null,
              null);

      parentContext.setRenderStateContext(nestedRsc);

      // 4.b Create a new layout.
      final @Nullable LithoNode newNode =
          ResolvedTree.resolveImpl(
              nestedRsc,
              parentContext,
              widthSpec,
              heightSpec,
              component,
              true,
              Preconditions.checkNotNull(globalKeyToReuse));

      if (newNode == null) {
        return null;
      }

      holderResult.getNode().copyInto(newNode);

      // If the resolved tree inherits the layout direction, then set it now.
      if (newNode.isLayoutDirectionInherit()) {
        newNode.layoutDirection(holderResult.getResolvedLayoutDirection());
      }

      final LayoutStateContext nestedLsc =
          new LayoutStateContext(
              nestedRsc.getCache(),
              parentContext,
              nestedRsc.getTreeState(),
              parentContext.getComponentTree(),
              nestedRsc.getLayoutVersion(),
              layoutStateContext.getCurrentDiffTree(),
              null);

      // Set the DiffNode for the nested tree's result to consume during measurement.
      nestedLsc.setNestedTreeDiffNode(holderResult.getDiffNode());

      parentContext.setLayoutStateContext(nestedLsc);

      // 4.b Measure the tree
      return measureTree(
          nestedLsc, parentContext.getAndroidContext(), newNode, widthSpec, heightSpec, null);
    } finally {
      parentContext.setCalculationStateContext(prevContext);
    }
  }

  static @Nullable LithoLayoutResult measure(
      final LayoutStateContext layoutStateContext,
      ComponentContext parentContext,
      final NestedTreeHolderResult holder,
      final int widthSpec,
      final int heightSpec) {

    final LithoLayoutResult layout =
        measureNestedTree(layoutStateContext, parentContext, holder, widthSpec, heightSpec);

    final @Nullable LithoLayoutResult currentLayout = holder.getNestedResult();

    if (layout != null && layout != currentLayout) {
      // If layout created is not same as previous layout then set last width / heihgt, measdured
      // width and height specs
      layout.setLastWidthSpec(widthSpec);
      layout.setLastHeightSpec(heightSpec);
      layout.setLastMeasuredHeight(layout.getHeight());
      layout.setLastMeasuredWidth(layout.getWidth());

      // Set new created LayoutResult for future access
      holder.setNestedResult(layout);
    }

    return layout;
  }

  @Nullable
  static LithoLayoutResult consumeCachedLayout(
      final LayoutStateContext layoutStateContext,
      final NestedTreeHolder holder,
      final NestedTreeHolderResult holderResult,
      final int widthSpec,
      final int heightSpec) {
    if (holder.getCachedNode() == null) {
      return null;
    }

    final MeasuredResultCache resultCache = layoutStateContext.getCache();
    final Component component = holder.getTailComponent();

    final @Nullable LithoLayoutResult cachedLayout =
        resultCache.getCachedResult(holder.getCachedNode());

    if (cachedLayout != null) {

      // Consume the cached result
      resultCache.removeCachedResult(holder.getCachedNode());

      final boolean hasValidDirection =
          hasValidLayoutDirectionInNestedTree(holderResult, cachedLayout);
      final boolean hasCompatibleSizeSpec =
          MeasureComparisonUtils.hasCompatibleSizeSpec(
              cachedLayout.getLastWidthSpec(),
              cachedLayout.getLastHeightSpec(),
              widthSpec,
              heightSpec,
              cachedLayout.getLastMeasuredWidth(),
              cachedLayout.getLastMeasuredHeight());

      // Transfer the cached layout to the node it if it's compatible.
      if (hasValidDirection) {
        if (hasCompatibleSizeSpec) {
          return cachedLayout;
        } else if (!isLayoutSpecWithSizeSpec(component)) {
          return measureTree(
              layoutStateContext,
              cachedLayout.getContext().getAndroidContext(),
              cachedLayout.getNode(),
              widthSpec,
              heightSpec,
              null);
        }
      }
    }

    return null;
  }

  /**
   * Check that the root of the nested tree we are going to use, has valid layout directions with
   * its main tree holder node.
   */
  private static boolean hasValidLayoutDirectionInNestedTree(
      NestedTreeHolderResult holder, LithoLayoutResult nestedTree) {
    return nestedTree.getNode().isLayoutDirectionInherit()
        || (nestedTree.getResolvedLayoutDirection() == holder.getResolvedLayoutDirection());
  }

  static boolean shouldComponentUpdate(
      final LithoNode layoutNode, final @Nullable DiffNode diffNode) {
    if (diffNode == null) {
      return true;
    }

    final Component component = layoutNode.getTailComponent();
    final ComponentContext scopedContext = layoutNode.getTailComponentContext();

    // return true for mountables to exit early
    if (isMountable(component)) {
      return true;
    }

    try {
      return component.shouldComponentUpdate(
          getDiffNodeScopedContext(diffNode), diffNode.getComponent(), scopedContext, component);
    } catch (Exception e) {
      ComponentUtils.handleWithHierarchy(scopedContext, component, e);
    }

    return true;
  }

  /** DiffNode state should be retrieved from the committed LayoutState. */
  private static @Nullable ComponentContext getDiffNodeScopedContext(DiffNode diffNode) {
    final @Nullable ScopedComponentInfo scopedComponentInfo = diffNode.getScopedComponentInfo();

    if (scopedComponentInfo == null) {
      return null;
    }

    return scopedComponentInfo.getContext();
  }

  static boolean isLayoutDirectionRTL(final Context context) {
    ApplicationInfo applicationInfo = context.getApplicationInfo();

    if ((SDK_INT >= JELLY_BEAN_MR1)
        && (applicationInfo.flags & ApplicationInfo.FLAG_SUPPORTS_RTL) != 0) {

      int layoutDirection = getLayoutDirection(context);
      return layoutDirection == View.LAYOUT_DIRECTION_RTL;
    }

    return false;
  }

  @TargetApi(JELLY_BEAN_MR1)
  private static int getLayoutDirection(final Context context) {
    return context.getResources().getConfiguration().getLayoutDirection();
  }

  static void setStyleWidthFromSpec(YogaNode node, int widthSpec) {
    switch (SizeSpec.getMode(widthSpec)) {
      case SizeSpec.UNSPECIFIED:
        node.setWidth(YogaConstants.UNDEFINED);
        break;
      case SizeSpec.AT_MOST:
        node.setMaxWidth(SizeSpec.getSize(widthSpec));
        break;
      case SizeSpec.EXACTLY:
        node.setWidth(SizeSpec.getSize(widthSpec));
        break;
    }
  }

  static void setStyleHeightFromSpec(YogaNode node, int heightSpec) {
    switch (SizeSpec.getMode(heightSpec)) {
      case SizeSpec.UNSPECIFIED:
        node.setHeight(YogaConstants.UNDEFINED);
        break;
      case SizeSpec.AT_MOST:
        node.setMaxHeight(SizeSpec.getSize(heightSpec));
        break;
      case SizeSpec.EXACTLY:
        node.setHeight(SizeSpec.getSize(heightSpec));
        break;
    }
  }
}
