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
import static com.facebook.litho.Component.isPrimitive;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.LayoutContext;
import com.facebook.rendercore.utils.MeasureSpecUtils;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaNode;
import java.util.ArrayList;

@Nullsafe(Nullsafe.Mode.LOCAL)
class Layout {

  static @Nullable LithoLayoutResult measureTree(
      final LithoLayoutContext lithoLayoutContext,
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

    final LayoutContext<LithoRenderContext> context =
        new LayoutContext<>(
            androidContext,
            new LithoRenderContext(lithoLayoutContext),
            0,
            lithoLayoutContext.getLayoutCache(),
            null);

    final LithoLayoutResult result = node.calculateLayout(context, widthSpec, heightSpec);

    if (layoutStatePerfEvent != null) {
      layoutStatePerfEvent.markerPoint("end_measure");
    }

    return result;
  }

  private static @Nullable LithoLayoutResult measureNestedTree(
      final LithoLayoutContext lithoLayoutContext,
      ComponentContext parentContext,
      final NestedTreeHolderResult holderResult,
      final int widthSpec,
      final int heightSpec) {

    // 1. Check if current layout result is compatible with size spec and can be reused or not
    final @Nullable LithoLayoutResult currentLayout = holderResult.getNestedResult();
    if (currentLayout != null
        && MeasureComparisonUtils.hasCompatibleSizeSpec(
            currentLayout.getWidthSpec(),
            currentLayout.getHeightSpec(),
            widthSpec,
            heightSpec,
            currentLayout.getWidth(),
            currentLayout.getHeight())) {
      return currentLayout;
    }

    // 2. Check if cached layout result is compatible and can be reused or not.
    final NestedTreeHolder node = holderResult.getNode();
    final @Nullable LithoLayoutResult cachedLayout =
        consumeCachedLayout(lithoLayoutContext, node, holderResult, widthSpec, heightSpec);

    if (cachedLayout != null) {
      return cachedLayout;
    }

    // 3. If component is not using OnCreateLayoutWithSizeSpec, we don't have to resolve it again
    // and we can simply re-measure the tree. This is for cases where component was measured with
    // Component.measure API but we could not find the cached layout result or cached layout result
    // was not compatible with given size spec.
    final Component component = node.getTailComponent();
    if (currentLayout != null && !isLayoutSpecWithSizeSpec(component)) {
      return measureTree(
          lithoLayoutContext,
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
    final String globalKeyToReuse;
    final @Nullable TreeProps treePropsToReuse;

    if (isLayoutSpecWithSizeSpec(component)) {
      globalKeyToReuse = node.getTailComponentKey();
      treePropsToReuse = node.getTailComponentContext().getTreeProps();
    } else {
      globalKeyToReuse = Preconditions.checkNotNull(node.getCachedNode()).getTailComponentKey();
      treePropsToReuse =
          Preconditions.checkNotNull(node.getCachedNode()).getTailComponentContext().getTreeProps();
    }

    // 4.a Apply state updates early for layout phase
    lithoLayoutContext.getTreeState().applyStateUpdatesEarly(parentContext, component, null, true);

    final CalculationContext prevContext = parentContext.getCalculationStateContext();

    try {
      final ResolveContext nestedRsc =
          new ResolveContext(
              lithoLayoutContext.getTreeId(),
              lithoLayoutContext.getCache(),
              lithoLayoutContext.getTreeState(),
              lithoLayoutContext.getLayoutVersion(),
              lithoLayoutContext.getRootComponentId(),
              lithoLayoutContext.isAccessibilityEnabled(),
              null,
              null,
              null,
              null);

      parentContext.setRenderStateContext(nestedRsc);

      // 4.b Create a new layout.
      final @Nullable LithoNode newNode =
          Resolver.resolveImpl(
              nestedRsc,
              parentContext,
              widthSpec,
              heightSpec,
              component,
              true,
              Preconditions.checkNotNull(globalKeyToReuse),
              treePropsToReuse);

      if (newNode == null) {
        if (parentContext.shouldCacheLayouts()) {
          // mark as error to prevent from resolving it again.
          holderResult.setMeasureHadExceptions(true);
        }
        return null;
      }

      // TODO (T151239896): Revaluate copy into and freeze after common props are refactored
      holderResult.getNode().copyInto(newNode);
      newNode.applyParentDependentCommonProps(lithoLayoutContext);

      // If the resolved tree inherits the layout direction, then set it now.
      if (newNode.isLayoutDirectionInherit()) {
        newNode.layoutDirection(holderResult.getResolvedLayoutDirection());
      }

      final LithoLayoutContext nestedLsc =
          new LithoLayoutContext(
              nestedRsc.getTreeId(),
              nestedRsc.getCache(),
              parentContext,
              nestedRsc.getTreeState(),
              nestedRsc.getLayoutVersion(),
              nestedRsc.getRootComponentId(),
              lithoLayoutContext.isAccessibilityEnabled(),
              lithoLayoutContext.getLayoutCache(),
              lithoLayoutContext.getCurrentDiffTree(),
              null);

      // Set the DiffNode for the nested tree's result to consume during measurement.
      nestedLsc.setNestedTreeDiffNode(holderResult.getDiffNode());

      parentContext.setLithoLayoutContext(nestedLsc);

      // 4.b Measure the tree
      return measureTree(
          nestedLsc, parentContext.getAndroidContext(), newNode, widthSpec, heightSpec, null);
    } finally {
      parentContext.setCalculationStateContext(prevContext);
    }
  }

  static @Nullable LithoLayoutResult measure(
      final LithoLayoutContext lithoLayoutContext,
      ComponentContext parentContext,
      final NestedTreeHolderResult holder,
      final int widthSpec,
      final int heightSpec) {

    final LithoLayoutResult layout =
        measureNestedTree(lithoLayoutContext, parentContext, holder, widthSpec, heightSpec);

    final @Nullable LithoLayoutResult currentLayout = holder.getNestedResult();
    // Set new created LayoutResult for future access
    if (layout != null && layout != currentLayout) {
      holder.setNestedResult(layout);
    }

    return layout;
  }

  @Nullable
  static LithoLayoutResult consumeCachedLayout(
      final LithoLayoutContext lithoLayoutContext,
      final NestedTreeHolder holder,
      final NestedTreeHolderResult holderResult,
      final int widthSpec,
      final int heightSpec) {
    if (holder.getCachedNode() == null) {
      return null;
    }

    final MeasuredResultCache resultCache = lithoLayoutContext.getCache();
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
              cachedLayout.getWidthSpec(),
              cachedLayout.getHeightSpec(),
              widthSpec,
              heightSpec,
              cachedLayout.getWidth(),
              cachedLayout.getHeight());

      // Transfer the cached layout to the node it if it's compatible.
      if (hasValidDirection) {
        if (hasCompatibleSizeSpec) {
          return cachedLayout;
        } else if (!isLayoutSpecWithSizeSpec(component)) {
          return measureTree(
              lithoLayoutContext,
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
   * In order to reuse render unit, we have to make sure layout data which render unit relies on is
   * determined before collecting layout results. So we're doing three things here:<br>
   * 1. Resolve NestedTree.<br>
   * 2. Measure Mountable and Primitive that were skipped due to fixed size.<br>
   * 3. Invoke OnBoundsDefined for all MountSpecs.<br>
   */
  static void measurePendingSubtrees(
      final ComponentContext parentContext,
      final LithoLayoutResult result,
      final LithoNode node,
      final LayoutState layoutState,
      final LithoLayoutContext lithoLayoutContext) {

    if (lithoLayoutContext.isFutureReleased() || result.measureHadExceptions()) {
      // Exit early if the layout future as been released or if this result had exceptions.
      return;
    }

    final Component component = node.getTailComponent();
    final boolean isTracing = ComponentsSystrace.isTracing();

    if (result instanceof NestedTreeHolderResult) {
      // If the nested tree is defined, it has been resolved during a measure call during
      // layout calculation.
      if (isTracing) {
        ComponentsSystrace.beginSectionWithArgs("resolveNestedTree:" + component.getSimpleName())
            .arg("widthSpec", "EXACTLY " + result.getWidth())
            .arg("heightSpec", "EXACTLY " + result.getHeight())
            .arg("rootComponentId", node.getTailComponent().getId())
            .flush();
      }

      final int size = node.getComponentCount();
      final ComponentContext immediateParentContext;
      if (size == 1) {
        immediateParentContext = parentContext;
      } else {
        immediateParentContext = node.getComponentContextAt(1);
      }

      LithoLayoutResult nestedTree =
          Layout.measure(
              lithoLayoutContext,
              Preconditions.checkNotNull(immediateParentContext),
              (NestedTreeHolderResult) result,
              MeasureSpecUtils.exactly(result.getWidth()),
              MeasureSpecUtils.exactly(result.getHeight()));

      if (isTracing) {
        ComponentsSystrace.endSection();
      }

      if (nestedTree == null) {
        return;
      }

      final @Nullable Resolver.Outputs outputs = Resolver.collectOutputs(nestedTree.mNode);
      if (outputs != null) {
        if (layoutState.mAttachables == null) {
          layoutState.mAttachables = new ArrayList<>(outputs.attachables.size());
        }
        layoutState.mAttachables.addAll(outputs.attachables);
      }

      measurePendingSubtrees(
          parentContext, nestedTree, nestedTree.getNode(), layoutState, lithoLayoutContext);
      return;
    } else if (result.getChildrenCount() > 0) {
      final ComponentContext context = result.getNode().getTailComponentContext();
      for (int i = 0, count = result.getChildrenCount(); i < count; i++) {
        LithoLayoutResult child = result.getChildAt(i);
        measurePendingSubtrees(context, child, child.getNode(), layoutState, lithoLayoutContext);
      }
    }

    result.onBoundsDefined();
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

    // return true for mountables and primitives to exit early
    if (isMountable(component) || isPrimitive(component)) {
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
