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
import static com.facebook.litho.Component.hasCachedLayout;
import static com.facebook.litho.Component.isLayoutSpec;
import static com.facebook.litho.Component.isLayoutSpecWithSizeSpec;
import static com.facebook.litho.Component.isMountSpec;
import static com.facebook.litho.Component.isNestedTree;
import static com.facebook.litho.Component.sMeasureFunction;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.rendercore.RenderState.LayoutContext;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaNode;
import java.util.List;

@Nullsafe(Nullsafe.Mode.LOCAL)
class Layout {

  private static final String EVENT_START_CREATE_LAYOUT = "start_create_layout";
  private static final String EVENT_END_CREATE_LAYOUT = "end_create_layout";
  private static final String EVENT_START_RECONCILE = "start_reconcile_layout";
  private static final String EVENT_END_RECONCILE = "end_reconcile_layout";

  static @Nullable LithoNode render(
      final LayoutStateContext layoutStateContext,
      final ComponentContext c,
      final Component component,
      final @Nullable String globalKeyToReuse,
      final int widthSpec,
      final int heightSpec,
      final @Nullable LithoNode current,
      final @Nullable PerfEvent layoutStatePerfEvent) {
    if (layoutStatePerfEvent != null) {
      final String event = current == null ? EVENT_START_CREATE_LAYOUT : EVENT_START_RECONCILE;
      layoutStatePerfEvent.markerPoint(event);
    }

    final @Nullable LithoNode node;
    if (current == null) {
      node = create(layoutStateContext, c, widthSpec, heightSpec, component, true, false, null);

      // This needs to finish layout on the UI thread.
      if (node != null && layoutStateContext.isLayoutInterrupted()) {
        if (layoutStatePerfEvent != null) {
          layoutStatePerfEvent.markerPoint(EVENT_END_CREATE_LAYOUT);
        }

        return node;
      } else {
        // Layout is complete, disable interruption from this point on.
        layoutStateContext.markLayoutUninterruptible();
      }
    } else {
      final ComponentContext updatedScopedContext =
          update(layoutStateContext, c, component, true, globalKeyToReuse);
      final Component updated = updatedScopedContext.getComponentScope();

      node =
          current.reconcile(
              layoutStateContext,
              c,
              updated,
              updatedScopedContext.getScopedComponentInfo(),
              globalKeyToReuse);
    }

    if (layoutStatePerfEvent != null) {
      final String event = current == null ? EVENT_END_CREATE_LAYOUT : EVENT_END_RECONCILE;
      layoutStatePerfEvent.markerPoint(event);
    }

    return node;
  }

  static @Nullable LithoLayoutResult layout(
      final LayoutStateContext layoutStateContext,
      final ComponentContext c,
      final @Nullable LithoNode node,
      final int widthSpec,
      final int heightSpec,
      final @Nullable DiffNode diff,
      final @Nullable PerfEvent layoutStatePerfEvent) {
    if (layoutStatePerfEvent != null) {
      layoutStatePerfEvent.markerPoint("start_measure");
    }

    final @Nullable LithoLayoutResult result =
        node != null ? measure(layoutStateContext, c, node, widthSpec, heightSpec, diff) : null;

    if (layoutStatePerfEvent != null) {
      layoutStatePerfEvent.markerPoint("end_measure");
    }

    return result;
  }

  static LayoutResultHolder createAndMeasureComponent(
      final LayoutStateContext layoutStateContext,
      final ComponentContext c,
      final Component component,
      final int widthSpec,
      final int heightSpec) {
    return createAndMeasureComponent(
        layoutStateContext, c, component, null, widthSpec, heightSpec, null, null, null);
  }

  static LayoutResultHolder createAndMeasureComponent(
      final LayoutStateContext layoutStateContext,
      final ComponentContext c,
      final Component component,
      final @Nullable String globalKeyToReuse,
      final int widthSpec,
      final int heightSpec,
      final @Nullable LithoNode current,
      final @Nullable DiffNode diff,
      final @Nullable PerfEvent layoutStatePerfEvent) {

    final @Nullable LithoNode node =
        render(
            layoutStateContext,
            c,
            component,
            globalKeyToReuse,
            widthSpec,
            heightSpec,
            current,
            layoutStatePerfEvent);

    if (node != null && layoutStateContext.isLayoutInterrupted()) {
      return LayoutResultHolder.interrupted(node);
    }

    final @Nullable LithoLayoutResult result =
        layout(layoutStateContext, c, node, widthSpec, heightSpec, diff, layoutStatePerfEvent);

    return new LayoutResultHolder(result);
  }

  public @Nullable static LithoNode create(
      final LayoutStateContext layoutStateContext,
      final ComponentContext parent,
      final Component component) {
    return create(layoutStateContext, parent, component, false, null);
  }

  static @Nullable LithoNode create(
      final LayoutStateContext layoutStateContext,
      final ComponentContext parent,
      Component component,
      final boolean reuseGlobalKey,
      final @Nullable String globalKeyToReuse) {
    return create(
        layoutStateContext,
        parent,
        SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED),
        SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED),
        component,
        false,
        reuseGlobalKey,
        globalKeyToReuse);
  }

  static @Nullable LithoNode create(
      final LayoutStateContext layoutStateContext,
      final ComponentContext parent,
      final int parentWidthSpec,
      final int parentHeightSpec,
      Component component,
      final boolean resolveNestedTree,
      final boolean reuseGlobalKey,
      final @Nullable String globalKeyToReuse) {

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("createLayout:" + component.getSimpleName());
    }

    final LithoNode node;
    final ComponentContext c;
    final String globalKey;
    final boolean isNestedTree = isNestedTree(component);
    final boolean hasCachedLayout = hasCachedLayout(layoutStateContext, component);
    final ScopedComponentInfo scopedComponentInfo;

    try {

      // 1. Consume the layout created in `willrender`.
      final LithoNode cached =
          component.consumeLayoutCreatedInWillRender(layoutStateContext, parent);

      // 2. Return immediately if cached layout is available.
      if (cached != null) {
        return cached;
      }

      // 4. Update the component.
      // 5. Get the scoped context of the updated component.
      c = update(layoutStateContext, parent, component, reuseGlobalKey, globalKeyToReuse);
      globalKey = c.getGlobalKey();

      component = c.getComponentScope();

      scopedComponentInfo = c.getScopedComponentInfo();
      // 6. Resolve the component into an InternalNode tree.

      final boolean shouldDeferNestedTreeResolution =
          (isNestedTree || hasCachedLayout) && !resolveNestedTree;

      // If nested tree resolution is deferred, then create an nested tree holder.
      if (shouldDeferNestedTreeResolution) {
        node = InternalNodeUtils.createNestedTreeHolder(c, c.getTreeProps());
      }

      // If the component can resolve itself resolve it.
      else if (component.canResolve()) {

        // Resolve the component into an InternalNode.
        node = component.resolve(layoutStateContext, c);
      }

      // If the component is a MountSpec.
      else if (isMountSpec(component)) {

        // Create a blank InternalNode for MountSpecs and set the default flex direction.
        node = InternalNodeUtils.create(c);
        node.flexDirection(YogaFlexDirection.COLUMN);
      }

      // If the component is a LayoutSpec.
      else if (isLayoutSpec(component)) {

        final RenderResult renderResult = component.render(c, parentWidthSpec, parentHeightSpec);
        final Component root = renderResult.component;

        if (root != null) {
          // TODO: (T57741374) this step is required because of a bug in redex.
          if (root == component) {
            node = root.resolve(layoutStateContext, c);
          } else {
            node = create(layoutStateContext, c, root);
          }
        } else {
          node = null;
        }

        if (renderResult != null && node != null) {
          applyRenderResultToNode(renderResult, node);
        }
      }

      // What even is this?
      else {
        throw new IllegalArgumentException("component:" + component.getSimpleName());
      }

      // 7. If the layout is null then return immediately.
      if (node == null) {
        return null;
      }

    } catch (Exception e) {
      ComponentUtils.handleWithHierarchy(parent, component, e);
      return null;
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }

    if (isTracing) {
      ComponentsSystrace.beginSection("afterCreateLayout:" + component.getSimpleName());
    }

    // 8. Set the measure function
    // Set measure func on the root node of the generated tree so that the mount calls use
    // those (see Controller.mountNodeTree()). Handle the case where the component simply
    // delegates its layout creation to another component, i.e. the root node belongs to
    // another component.
    if (node.getComponentCount() == 0) {
      final boolean isMountSpecWithMeasure = component.canMeasure() && isMountSpec(component);
      if (isMountSpecWithMeasure || ((isNestedTree || hasCachedLayout) && !resolveNestedTree)) {
        node.setMeasureFunction(sMeasureFunction);
      }
    }

    // 9. Copy the common props
    // Skip if resolving a layout with size spec because common props were copied in the previous
    // layout pass.
    final CommonProps commonProps = component.getCommonProps();
    if (commonProps != null && !(isLayoutSpecWithSizeSpec(component) && resolveNestedTree)) {
      commonProps.copyInto(c, node);
    }

    // 10. Add the component to the InternalNode.
    node.appendComponent(scopedComponentInfo);

    // 11. Create and add transition to this component's InternalNode.
    if (areTransitionsEnabled(c)) {
      if (component.needsPreviousRenderData()) {
        node.addComponentNeedingPreviousRenderData(globalKey, scopedComponentInfo);
      } else {
        try {
          // Calls onCreateTransition on the Spec.
          final Transition transition = component.createTransition(c);
          if (transition != null) {
            node.addTransition(transition);
          }
        } catch (Exception e) {
          ComponentUtils.handleWithHierarchy(parent, component, e);
        }
      }
    }

    // 12. Add attachable components
    if (component.hasAttachDetachCallback()) {
      // needs ComponentUtils.getGlobalKey?
      node.addAttachable(new LayoutSpecAttachable(globalKey, component, scopedComponentInfo));
    }

    // 13. Call onPrepare for MountSpecs or prepare for MountableComponents.
    if (isMountSpec(component)) {
      try {
        if (isMountSpec(component)) {
          PrepareResult prepareResult = component.prepare(scopedComponentInfo.getContext());
          if (prepareResult != null) {
            node.setMountable(prepareResult.mountable);
          }
        }
      } catch (Exception e) {
        ComponentUtils.handleWithHierarchy(parent, component, e);
      }
    }

    // 14. Add working ranges to the InternalNode.
    scopedComponentInfo.addWorkingRangeToNode(node);

    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    return node;
  }

  static @Nullable LithoLayoutResult create(
      final LayoutStateContext layoutStateContext,
      ComponentContext parentContext,
      final NestedTreeHolderResult holder,
      final int widthSpec,
      final int heightSpec) {

    final LithoNode node = holder.getNode();
    final Component component = node.getTailComponent();
    if (component == null) {
      throw new IllegalArgumentException("A component is required to resolve a nested tree.");
    }

    final String globalKey = Preconditions.checkNotNull(node.getTailComponentKey());
    final @Nullable LithoLayoutResult currentLayout = holder.getNestedResult();

    // The resolved layout to return.
    final LithoLayoutResult layout;

    if (currentLayout == null
        || !hasCompatibleSizeSpec(
            currentLayout.getLastWidthSpec(),
            currentLayout.getLastHeightSpec(),
            widthSpec,
            heightSpec,
            currentLayout.getLastMeasuredWidth(),
            currentLayout.getLastMeasuredHeight())) {

      if (currentLayout != null && !isLayoutSpecWithSizeSpec(component)) {
        layout = remeasure(layoutStateContext, currentLayout, widthSpec, heightSpec);
      } else {

        // Check if cached layout can be used.
        final @Nullable LithoLayoutResult cachedLayout =
            consumeCachedLayout(layoutStateContext, component, holder, widthSpec, heightSpec);

        if (cachedLayout != null) {
          // Use the cached layout.
          layout = cachedLayout;
        } else {
          // Create a new layout.
          final @Nullable LithoNode newNode =
              create(
                  layoutStateContext,
                  parentContext,
                  widthSpec,
                  heightSpec,
                  component,
                  true,
                  true,
                  globalKey);

          if (newNode != null) {
            holder.getNode().copyInto(newNode);

            // If the resolved tree inherits the layout direction, then set it now.
            if (newNode.isLayoutDirectionInherit()) {
              newNode.layoutDirection(holder.getResolvedLayoutDirection());
            }

            // Set the DiffNode for the nested tree's result to consume during measurement.
            layoutStateContext.setNestedTreeDiffNode(holder.getDiffNode());

            layout =
                measure(
                    layoutStateContext,
                    parentContext,
                    newNode,
                    widthSpec,
                    heightSpec,
                    holder.getDiffNode());
          } else {
            layout = null;
          }
        }
      }

      if (layout != null) {
        layout.setLastWidthSpec(widthSpec);
        layout.setLastHeightSpec(heightSpec);
        layout.setLastMeasuredHeight(layout.getHeight());
        layout.setLastMeasuredWidth(layout.getWidth());
      }

      holder.setNestedResult(layout);
    } else {

      // Use the previous layout.
      layout = currentLayout;
    }

    return layout;
  }

  static void applyRenderResultToNode(RenderResult renderResult, LithoNode node) {
    if (renderResult.transitions != null) {
      for (Transition t : renderResult.transitions) {
        node.addTransition(t);
      }
    }
    if (renderResult.useEffectEntries != null) {
      for (Attachable attachable : renderResult.useEffectEntries) {
        node.addAttachable(attachable);
      }
    }
  }

  static ComponentContext update(
      final LayoutStateContext layoutStateContext,
      final ComponentContext parent,
      final Component component,
      final boolean reuseGlobalKey,
      @Nullable final String globalKeyToReuse) {

    if (reuseGlobalKey) {
      if (globalKeyToReuse == null) {
        throw new IllegalStateException("Cannot reuse a null global key");
      }
    }

    final TreeProps ancestor = parent.getTreeProps();

    // 1. Update the internal state of the component wrt the parent.
    // 2. Get the scoped context from the updated component.
    final ComponentContext c =
        ComponentContext.withComponentScope(
            layoutStateContext,
            parent,
            component,
            globalKeyToReuse == null
                ? ComponentKeyUtils.generateGlobalKey(parent, parent.getComponentScope(), component)
                : globalKeyToReuse);
    c.getScopedComponentInfo().applyStateUpdates(layoutStateContext.getStateHandler());

    // 3. Set the TreeProps which will be passed to the descendants of the component.
    final TreeProps descendants = component.getTreePropsForChildren(c, ancestor);
    c.setParentTreeProps(ancestor);
    c.setTreeProps(descendants);

    if (ComponentsConfiguration.isDebugModeEnabled) {
      DebugComponent.applyOverrides(c, component, c.getGlobalKey());
    }

    return c;
  }

  static LithoLayoutResult measure(
      final LayoutStateContext layoutStateContext,
      final ComponentContext c,
      final LithoNode root,
      final int widthSpec,
      final int heightSpec,
      final @Nullable DiffNode diff) {

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection(
          "measureTree:" + Preconditions.checkNotNull(root.getHeadComponent()).getSimpleName());
    }

    final LayoutContext<LithoRenderContext> context =
        new LayoutContext<>(
            c.getAndroidContext(), new LithoRenderContext(layoutStateContext, diff), 0, null, null);

    LithoLayoutResult result = root.calculateLayout(context, widthSpec, heightSpec);

    if (isTracing) {
      ComponentsSystrace.endSection(/* measureTree */ );
    }

    return result;
  }

  static @Nullable LithoLayoutResult resumeCreateAndMeasureComponent(
      final LayoutStateContext layoutStateContext,
      final ComponentContext c,
      final @Nullable LithoNode root,
      final int widthSpec,
      final int heightSpec,
      final @Nullable DiffNode diff,
      final @Nullable PerfEvent logLayoutState) {
    if (root == null) {
      return null;
    }

    final boolean isTracing = ComponentsSystrace.isTracing();

    if (isTracing) {
      ComponentsSystrace.beginSection(
          "resume:" + Preconditions.checkNotNull(root.getHeadComponent()).getSimpleName());
    }

    resume(layoutStateContext, root);

    if (logLayoutState != null) {
      logLayoutState.markerPoint("start_measure");
    }

    final LithoLayoutResult result =
        measure(layoutStateContext, c, root, widthSpec, heightSpec, diff);

    if (logLayoutState != null) {
      logLayoutState.markerPoint("end_measure");
    }

    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    return result;
  }

  static void resume(final LayoutStateContext c, final LithoNode root) {
    final List<Component> unresolved = root.getUnresolvedComponents();

    if (unresolved != null) {
      final ComponentContext context = Preconditions.checkNotNull(root.getTailComponentContext());
      for (int i = 0, size = unresolved.size(); i < size; i++) {
        root.child(c, Preconditions.checkNotNull(context), unresolved.get(i));
      }
      unresolved.clear();
    }

    for (int i = 0, size = root.getChildCount(); i < size; i++) {
      resume(c, root.getChildAt(i));
    }
  }

  @VisibleForTesting
  static LithoLayoutResult remeasure(
      final LayoutStateContext layoutStateContext,
      final LithoLayoutResult layout,
      final int widthSpec,
      final int heightSpec) {
    return measure(
        layoutStateContext,
        layout.getContext(),
        layout.getNode(),
        widthSpec,
        heightSpec,
        layout.getDiffNode());
  }

  @Nullable
  static LithoLayoutResult consumeCachedLayout(
      final LayoutStateContext layoutStateContext,
      final Component component,
      final NestedTreeHolderResult holder,
      final int widthSpec,
      final int heightSpec) {
    final LayoutState layoutState = layoutStateContext.getLayoutState();
    if (layoutState == null) {
      throw new IllegalStateException(
          component.getSimpleName()
              + ": Trying to access the cached InternalNode for a component outside of a"
              + " LayoutState calculation. If that is what you must do, see"
              + " Component#measureMightNotCacheInternalNode.");
    }

    final @Nullable LithoLayoutResult cachedLayout = layoutState.getCachedLayout(component);

    if (cachedLayout != null) {

      layoutState.clearCachedLayout(component);

      final boolean isFromCurrentLayout =
          cachedLayout.getLayoutStateContext() == layoutStateContext;
      final boolean hasValidDirection =
          InternalNodeUtils.hasValidLayoutDirectionInNestedTree(holder, cachedLayout);
      final boolean hasCompatibleSizeSpec =
          hasCompatibleSizeSpec(
              cachedLayout.getLastWidthSpec(),
              cachedLayout.getLastHeightSpec(),
              widthSpec,
              heightSpec,
              cachedLayout.getLastMeasuredWidth(),
              cachedLayout.getLastMeasuredHeight());

      // Transfer the cached layout to the node it if it's compatible.
      if (isFromCurrentLayout && hasValidDirection) {
        if (hasCompatibleSizeSpec) {
          return cachedLayout;
        } else if (!isLayoutSpecWithSizeSpec(component)) {
          return remeasure(layoutStateContext, cachedLayout, widthSpec, heightSpec);
        }
      }
    }

    return null;
  }

  /**
   * Check if a cached nested tree has compatible SizeSpec to be reused as is or if it needs to be
   * recomputed.
   *
   * <p>The conditions to be able to re-use previous measurements are: 1) The measureSpec is the
   * same 2) The new measureSpec is EXACTLY and the last measured size matches the measureSpec size.
   * 3) The old measureSpec is UNSPECIFIED, the new one is AT_MOST and the old measured size is
   * smaller that the maximum size the new measureSpec will allow. 4) Both measure specs are
   * AT_MOST. The old measure spec allows a bigger size than the new and the old measured size is
   * smaller than the allowed max size for the new sizeSpec.
   */
  public static boolean hasCompatibleSizeSpec(
      final int oldWidthSpec,
      final int oldHeightSpec,
      final int newWidthSpec,
      final int newHeightSpec,
      final float oldMeasuredWidth,
      final float oldMeasuredHeight) {
    final boolean widthIsCompatible =
        MeasureComparisonUtils.isMeasureSpecCompatible(
            oldWidthSpec, newWidthSpec, (int) oldMeasuredWidth);

    final boolean heightIsCompatible =
        MeasureComparisonUtils.isMeasureSpecCompatible(
            oldHeightSpec, newHeightSpec, (int) oldMeasuredHeight);
    return widthIsCompatible && heightIsCompatible;
  }

  /**
   * This method determine if transitions are enabled for the user. If the experiment is enabled for
   * the user then they will get cached value else it will be determined using the utility method.
   *
   * @param context Component context.
   * @return true if transitions are enabled.
   */
  static boolean areTransitionsEnabled(final @Nullable ComponentContext context) {
    if (context == null || context.getComponentTree() == null) {
      return AnimationsDebug.areTransitionsEnabled(null);
    }
    return context.getComponentTree().areTransitionsEnabled();
  }

  /**
   * Returns true either if the two nodes have the same Component type or if both don't have a
   * Component.
   */
  static boolean hostIsCompatible(final LithoNode node, final DiffNode diffNode) {
    return ComponentUtils.isSameComponentType(node.getTailComponent(), diffNode.getComponent());
  }

  static boolean shouldComponentUpdate(
      final LithoNode layoutNode, final @Nullable DiffNode diffNode) {
    if (diffNode == null) {
      return true;
    }

    final Component component = layoutNode.getTailComponent();

    if (component != null) {
      final ComponentContext scopedContext =
          Preconditions.checkNotNull(layoutNode.getTailComponentContext());

      try {
        return component.shouldComponentUpdate(
            getDiffNodeScopedContext(diffNode), diffNode.getComponent(), scopedContext, component);
      } catch (Exception e) {
        ComponentUtils.handleWithHierarchy(Preconditions.checkNotNull(scopedContext), component, e);
      }
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
