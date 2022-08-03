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
import static com.facebook.litho.Component.isMountable;
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
import com.facebook.rendercore.Mountable;
import com.facebook.rendercore.RenderCoreSystrace;
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

  static @Nullable ResolvedTree resolveTree(
      final LayoutStateContext layoutStateContext,
      final ComponentContext c,
      final Component component,
      final int widthSpec,
      final int heightSpec,
      final @Nullable LithoNode current,
      final @Nullable PerfEvent layoutStatePerfEvent) {

    if (layoutStatePerfEvent != null) {
      final String event = current == null ? EVENT_START_CREATE_LAYOUT : EVENT_START_RECONCILE;
      layoutStatePerfEvent.markerPoint(event);
    }

    final RenderStateContext renderStateContext = layoutStateContext.getRenderStateContext();

    final @Nullable LithoNode node;
    if (current == null) {
      node =
          create(
              layoutStateContext,
              c,
              widthSpec,
              heightSpec,
              component,
              !c.shouldAlwaysResolveNestedTreeInMeasure(),
              null);

      // This needs to finish layout on the UI thread.
      if (node != null && renderStateContext.isLayoutInterrupted()) {
        if (layoutStatePerfEvent != null) {
          layoutStatePerfEvent.markerPoint(EVENT_END_CREATE_LAYOUT);
        }

        return new ResolvedTree(node);
      } else {
        // Layout is complete, disable interruption from this point on.
        renderStateContext.markLayoutUninterruptible();
      }
    } else {
      final String globalKeyToReuse = current.getHeadComponentKey();

      if (globalKeyToReuse == null) {
        throw new IllegalStateException("Cannot reuse a null global key");
      }

      final ComponentContext updatedScopedContext =
          update(layoutStateContext, c, component, globalKeyToReuse);
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

    return node == null ? null : new ResolvedTree(node);
  }

  static @Nullable LithoLayoutResult layout(
      final LayoutStateContext layoutStateContext,
      final Context androidContext,
      final @Nullable LithoNode node,
      final int widthSpec,
      final int heightSpec,
      final @Nullable PerfEvent layoutStatePerfEvent) {
    if (layoutStatePerfEvent != null) {
      layoutStatePerfEvent.markerPoint("start_measure");
    }

    final @Nullable LithoLayoutResult result;

    if (node != null) {
      final boolean isTracing = RenderCoreSystrace.isEnabled();
      if (isTracing) {
        RenderCoreSystrace.beginSection("measureTree:" + node.getHeadComponent().getSimpleName());
      }

      final LayoutContext<LithoRenderContext> context =
          new LayoutContext<>(
              androidContext, new LithoRenderContext(layoutStateContext), 0, null, null);

      result = node.calculateLayout(context, widthSpec, heightSpec);

      if (isTracing) {
        RenderCoreSystrace.endSection(/* measureTree */ );
      }
    } else {
      result = null;
    }

    if (layoutStatePerfEvent != null) {
      layoutStatePerfEvent.markerPoint("end_measure");
    }

    return result;
  }

  static @Nullable ResolvedTree createResolvedTree(
      final LayoutStateContext layoutStateContext,
      final ComponentContext c,
      final Component component,
      final int widthSpec,
      final int heightSpec) {
    return createResolvedTree(
        layoutStateContext, c, component, widthSpec, heightSpec, false, null, null);
  }

  static @Nullable ResolvedTree createResolvedTree(
      final LayoutStateContext layoutStateContext,
      final ComponentContext c,
      final Component component,
      final int widthSpec,
      final int heightSpec,
      final boolean isReconcilable,
      final @Nullable LithoNode current,
      final @Nullable PerfEvent layoutStatePerfEvent) {
    try {
      applyStateUpdateEarly(layoutStateContext, c, component, current);
    } catch (Exception ex) {
      ComponentUtils.handleWithHierarchy(c, component, ex);
      return null;
    }

    return resolveTree(
        layoutStateContext,
        c,
        component,
        widthSpec,
        heightSpec,
        isReconcilable ? current : null,
        layoutStatePerfEvent);
  }

  static LayoutResultHolder measureTree(
      final LayoutStateContext layoutStateContext,
      final @Nullable LithoNode node,
      final ComponentContext c,
      final int widthSpec,
      final int heightSpec,
      final @Nullable PerfEvent layoutStatePerfEvent) {
    if (node == null) {
      return new LayoutResultHolder(null);
    }

    final @Nullable LithoLayoutResult result =
        layout(
            layoutStateContext,
            c.getAndroidContext(),
            node,
            widthSpec,
            heightSpec,
            layoutStatePerfEvent);

    return new LayoutResultHolder(result);
  }

  private static void applyStateUpdateEarly(
      final LayoutStateContext layoutStateContext,
      final ComponentContext c,
      final Component component,
      final @Nullable LithoNode current) {
    if (c.isApplyStateUpdateEarlyEnabled() && c.getComponentTree() != null) {
      layoutStateContext.getTreeState().applyStateUpdatesEarly(c, component, current, false);
    }
  }

  public @Nullable static LithoNode create(
      final LayoutStateContext layoutStateContext,
      final ComponentContext parent,
      final Component component) {
    return create(layoutStateContext, parent, component, null);
  }

  static @Nullable LithoNode create(
      final LayoutStateContext layoutStateContext,
      final ComponentContext parent,
      Component component,
      final @Nullable String globalKeyToReuse) {
    return create(
        layoutStateContext,
        parent,
        SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED),
        SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED),
        component,
        false,
        globalKeyToReuse);
  }

  static @Nullable LithoNode create(
      final LayoutStateContext layoutStateContext,
      final ComponentContext parent,
      final int parentWidthSpec,
      final int parentHeightSpec,
      Component component,
      final boolean resolveNestedTree,
      final @Nullable String globalKeyToReuse) {

    final boolean isTracing = RenderCoreSystrace.isEnabled();
    if (isTracing) {
      RenderCoreSystrace.beginSection("createLayout:" + component.getSimpleName());
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
          component.consumeLayoutCreatedInWillRender(
              layoutStateContext.getRenderStateContext(), parent);

      // 2. Return immediately if cached layout is available.
      if (cached != null) {
        return cached;
      }

      // 4. Update the component.
      // 5. Get the scoped context of the updated component.
      c = update(layoutStateContext, parent, component, globalKeyToReuse);
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

      // If the component is a MountSpec (including MountableComponents).
      else if (isMountSpec(component)) {

        // Create a blank InternalNode for MountSpecs and set the default flex direction.
        node = InternalNodeUtils.create(c);
        node.flexDirection(YogaFlexDirection.COLUMN);

        // Call onPrepare for MountSpecs or prepare for MountableComponents.
        PrepareResult prepareResult = component.prepare(scopedComponentInfo.getContext());
        if (prepareResult != null) {
          Mountable<?> mountable = prepareResult.mountable;
          final String componentKey = scopedComponentInfo.getContext().getGlobalKey();
          mountable.setId(
              LayoutStateContext.calculateNextId(layoutStateContext, component, componentKey));
          node.setMountable(mountable);
        }
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
        RenderCoreSystrace.endSection();
      }
    }

    if (isTracing) {
      RenderCoreSystrace.beginSection("afterCreateLayout:" + component.getSimpleName());
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
      if (component instanceof SpecGeneratedComponent
          && ((SpecGeneratedComponent) component).needsPreviousRenderData()) {
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
    if (component instanceof SpecGeneratedComponent
        && ((SpecGeneratedComponent) component).hasAttachDetachCallback()) {
      // needs ComponentUtils.getGlobalKey?
      node.addAttachable(
          new LayoutSpecAttachable(
              globalKey, (SpecGeneratedComponent) component, scopedComponentInfo));
    }

    // 13. Add working ranges to the InternalNode.
    scopedComponentInfo.addWorkingRangeToNode(node);

    if (isTracing) {
      RenderCoreSystrace.endSection();
    }

    return node;
  }

  static @Nullable LithoNode resolveNestedTreeAndAddToCache(
      final LayoutStateContext layoutStateContext,
      final ComponentContext parentContext,
      final Component component,
      final String globalKey,
      final int widthSpec,
      final int heightSpec) {

    if (parentContext.isApplyStateUpdateEarlyEnabled()) {
      layoutStateContext
          .getTreeState()
          .applyStateUpdatesEarly(parentContext, component, null, true);
    }

    // Create a new layout.
    final @Nullable LithoNode newNode =
        create(
            layoutStateContext,
            parentContext,
            widthSpec,
            heightSpec,
            component,
            true,
            Preconditions.checkNotNull(globalKey));

    return newNode;
  }

  private static @Nullable LithoLayoutResult measureNestedTree(
      final LayoutStateContext layoutStateContext,
      ComponentContext parentContext,
      final NestedTreeHolderResult holder,
      final int widthSpec,
      final int heightSpec) {

    // 1. Check if current layout result is compatible with size spec and can be reused or not
    final @Nullable LithoLayoutResult currentLayout = holder.getNestedResult();
    final LithoNode node = holder.getNode();
    final Component component = node.getTailComponent();

    if (currentLayout != null
        && hasCompatibleSizeSpec(
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
        consumeCachedLayout(layoutStateContext, component, holder, widthSpec, heightSpec);

    if (cachedLayout != null) {
      return cachedLayout;
    }

    // 3. If component is not using OnCreateLayoutWithSizeSpec, we don't have to resolve it again
    // and we can simply re-measure the tree. This is for cases where component was measured with
    // Component.measure API but we could not find the cached layout result or cached layout result
    // was not compatible with given size spec.
    if (currentLayout != null && !isLayoutSpecWithSizeSpec(component)) {
      return remeasure(layoutStateContext, currentLayout, widthSpec, heightSpec);
    }

    // 4. If current layout result is not available or component uses OnCreateLayoutWithSizeSpec
    // then resolve the tree and measure it. At this point we know that current layout result and
    // cached layout result are not available or are not compatible with given size spec.
    final String globalKey = node.getTailComponentKey();

    // 4.a Create a new layout
    // This step will eventually go away in the desired end state as we will have LithoNode
    // resolved for nested tree in the beginning with different size specs.
    final @Nullable LithoNode newNode =
        resolveNestedTreeAndAddToCache(
            layoutStateContext,
            parentContext,
            component,
            node.getTailComponentKey(),
            widthSpec,
            heightSpec);

    if (newNode == null) {
      return null;
    }

    holder.getNode().copyInto(newNode);

    // If the resolved tree inherits the layout direction, then set it now.
    if (newNode.isLayoutDirectionInherit()) {
      newNode.layoutDirection(holder.getResolvedLayoutDirection());
    }

    // Set the DiffNode for the nested tree's result to consume during measurement.
    layoutStateContext.setNestedTreeDiffNode(holder.getDiffNode());

    // 4.b Measure the tree
    return layout(
        layoutStateContext,
        parentContext.getAndroidContext(),
        newNode,
        widthSpec,
        heightSpec,
        null);
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
      @Nullable final String globalKeyToReuse) {

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
    c.getScopedComponentInfo().applyStateUpdates(layoutStateContext.getTreeState());

    // 3. Set the TreeProps which will be passed to the descendants of the component.
    if (component instanceof SpecGeneratedComponent) {
      final TreeProps descendants =
          ((SpecGeneratedComponent) component).getTreePropsForChildren(c, ancestor);
      c.setParentTreeProps(ancestor);
      c.setTreeProps(descendants);
    }

    if (ComponentsConfiguration.isDebugModeEnabled) {
      DebugComponent.applyOverrides(c, component, c.getGlobalKey());
    }

    return c;
  }

  static @Nullable LithoLayoutResult resumeCreateAndMeasureComponent(
      final LayoutStateContext layoutStateContext,
      final ComponentContext c,
      final @Nullable LithoNode root,
      final int widthSpec,
      final int heightSpec,
      final @Nullable PerfEvent logLayoutState) {
    final RenderStateContext renderStateContext = layoutStateContext.getRenderStateContext();

    if (renderStateContext.isLayoutReleased()) {
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.ERROR,
          "ReleasedLayoutResumed",
          layoutStateContext.getLifecycleDebugString());
    }

    if (root == null || renderStateContext.isLayoutReleased()) {
      return null;
    }

    final boolean isTracing = RenderCoreSystrace.isEnabled();

    if (isTracing) {
      RenderCoreSystrace.beginSection("resume:" + root.getHeadComponent().getSimpleName());
    }

    resume(layoutStateContext, root);

    final LithoLayoutResult result =
        layout(
            layoutStateContext, c.getAndroidContext(), root, widthSpec, heightSpec, logLayoutState);

    if (isTracing) {
      RenderCoreSystrace.endSection();
    }

    return result;
  }

  static void resume(final LayoutStateContext c, final LithoNode root) {
    final List<Component> unresolved = root.getUnresolvedComponents();

    if (unresolved != null) {
      final ComponentContext context = root.getTailComponentContext();
      for (int i = 0, size = unresolved.size(); i < size; i++) {
        root.child(c, context, unresolved.get(i));
      }
      unresolved.clear();
    }

    for (int i = 0, size = root.getChildCount(); i < size; i++) {
      resume(c, root.getChildAt(i));
    }
  }

  @VisibleForTesting
  static @Nullable LithoLayoutResult remeasure(
      final LayoutStateContext layoutStateContext,
      final LithoLayoutResult layout,
      final int widthSpec,
      final int heightSpec) {
    return layout(
        layoutStateContext,
        layout.getContext().getAndroidContext(),
        layout.getNode(),
        widthSpec,
        heightSpec,
        null);
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
