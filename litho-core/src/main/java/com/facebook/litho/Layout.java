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

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static com.facebook.litho.Component.isLayoutSpec;
import static com.facebook.litho.Component.isLayoutSpecWithSizeSpec;
import static com.facebook.litho.Component.isMountSpec;
import static com.facebook.litho.Component.isNestedTree;
import static com.facebook.litho.ComponentContext.NULL_LAYOUT;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaFlexDirection;
import java.util.List;

class Layout {

  static final boolean IS_TEST = "robolectric".equals(Build.FINGERPRINT);

  private static final String EVENT_START_CREATE_LAYOUT = "start_create_layout";
  private static final String EVENT_END_CREATE_LAYOUT = "end_create_layout";
  private static final String EVENT_START_RECONCILE = "start_reconcile_layout";
  private static final String EVENT_END_RECONCILE = "end_reconcile_layout";

  static InternalNode createAndMeasureComponent(
      final LayoutStateContext layoutStateContext,
      final ComponentContext c,
      final Component component,
      final int widthSpec,
      final int heightSpec) {
    return createAndMeasureComponent(
        layoutStateContext, c, component, widthSpec, heightSpec, null, null, null);
  }

  static InternalNode createAndMeasureComponent(
      final LayoutStateContext layoutStateContext,
      final ComponentContext c,
      final Component component,
      final int widthSpec,
      final int heightSpec,
      final @Nullable InternalNode current,
      final @Nullable DiffNode diff,
      final @Nullable PerfEvent layoutStatePerfEvent) {

    if (layoutStatePerfEvent != null) {
      final String event = current == null ? EVENT_START_CREATE_LAYOUT : EVENT_START_RECONCILE;
      layoutStatePerfEvent.markerPoint(event);
    }

    c.setWidthSpec(widthSpec);
    c.setHeightSpec(heightSpec);

    final InternalNode layout;
    if (current == null) {
      layout = create(c, component, true);

      // This needs to finish layout on the UI thread.
      if (c.wasLayoutInterrupted()) {
        if (layoutStatePerfEvent != null) {
          layoutStatePerfEvent.markerPoint(EVENT_END_CREATE_LAYOUT);
        }

        return layout;
      } else {
        // Layout is complete, disable interruption from this point on.
        c.markLayoutUninterruptible();
      }

    } else {
      ComponentContext updatedScopedContext = update(c, component, true);
      Component updated = updatedScopedContext.getComponentScope();
      layout = current.reconcile(layoutStateContext, c, updated, updated.getGlobalKey());
    }

    if (layoutStatePerfEvent != null) {
      final String event = current == null ? EVENT_END_CREATE_LAYOUT : EVENT_END_RECONCILE;
      layoutStatePerfEvent.markerPoint(event);
    }

    if (layoutStatePerfEvent != null) {
      layoutStatePerfEvent.markerPoint("start_measure");
    }

    measure(c, layout, widthSpec, heightSpec, diff);

    if (layoutStatePerfEvent != null) {
      layoutStatePerfEvent.markerPoint("end_measure");
    }

    return layout;
  }

  public static InternalNode create(final ComponentContext parent, final Component component) {
    return create(parent, component, false, false);
  }

  static InternalNode create(
      final ComponentContext parent, final Component component, final boolean resolveNestedTree) {
    return create(parent, component, resolveNestedTree, false);
  }

  static InternalNode create(
      final ComponentContext parent,
      Component component,
      final boolean resolveNestedTree,
      final boolean reuseGlobalKey) {

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("createLayout:" + component.getSimpleName());
    }

    final InternalNode node;
    final ComponentContext c;

    try {

      // 1. Consume the layout created in `willrender`.
      final InternalNode cached = component.consumeLayoutCreatedInWillRender();

      // 2. Return immediately if cached layout is available.
      if (cached != null) {
        return cached;
      }

      // 4. Update the component.
      // 5. Get the scoped context of the updated component.
      c = update(parent, component, reuseGlobalKey);
      component = c.getComponentScope();

      // 6. Resolve the component into an InternalNode tree.

      final boolean shouldDeferNestedTreeResolution =
          isNestedTree(c, component) && !resolveNestedTree;

      // If nested tree resolution is deferred, then create an nested tree holder.
      if (shouldDeferNestedTreeResolution) {
        node = InternalNodeUtils.create(c);
        node.markIsNestedTreeHolder(c.getTreeProps());
      }

      // If the component can resolve itself resolve it.
      else if (component.canResolve()) {

        // Resolve the component into an InternalNode.
        node = (InternalNode) component.resolve(c);
      }

      // If the component is a MountSpec.
      else if (isMountSpec(component)) {

        // Create a blank InternalNode for MountSpecs and set the default flex direction.
        node = InternalNodeUtils.create(c).flexDirection(YogaFlexDirection.COLUMN);
      }

      // If the component is a LayoutSpec.
      else if (isLayoutSpec(component)) {

        // Calls the onCreateLayout or onCreateLayoutWithSizeSpec on the Spec.
        final Component root = onCreateLayout(c, component);

        // TODO: (T57741374) this step is required because of a bug in redex.
        if (root == component) {
          node = (InternalNode) root.resolve(c);
        } else if (root != null) {
          node = create(c, root, false);
        } else {
          node = null;
        }
      }

      // What even is this?
      else {
        throw new IllegalArgumentException("component:" + component.getSimpleName());
      }

      // 7. If the layout is null then return immediately.
      if (node == null || node == NULL_LAYOUT) {
        return NULL_LAYOUT;
      }

    } catch (Exception e) {
      handle(parent, component, e);
      return NULL_LAYOUT;
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
    if (node.getTailComponent() == null) {
      final boolean isMountSpecWithMeasure = component.canMeasure() && isMountSpec(component);
      if (isMountSpecWithMeasure || (isNestedTree(c, component) && !resolveNestedTree)) {
        node.setMeasureFunction(
            ComponentLifecycle.getYogaMeasureFunction(c.getLayoutStateContext()));
      }
    }

    // 9. Copy the common props
    // Skip if resolving a layout with size spec because common props were copied in the previous
    // layout pass.
    final CommonPropsCopyable commonProps = component.getCommonPropsCopyable();
    if (commonProps != null && !(isLayoutSpecWithSizeSpec(component) && resolveNestedTree)) {
      commonProps.copyInto(c, node);
    }

    // 10. Add the component to the InternalNode.
    node.appendComponent(component, component.getGlobalKey());

    // 11. Create and add transition to this component's InternalNode.
    if (areTransitionsEnabled(c)) {
      if (component.needsPreviousRenderData()) {
        node.addComponentNeedingPreviousRenderData(component);
      } else {

        // Calls onCreateTransition on the Spec.
        final Transition transition = component.createTransition(c);
        if (transition != null) {
          node.addTransition(transition);
        }
      }
    }

    // 12. Call onPrepare for MountSpecs.
    if (isMountSpec(component)) {
      component.onPrepare(c);
    }

    // 13. Add working ranges to the InternalNode.
    // TODO: (T56146478) Create getters for working ranges.
    if (component.mWorkingRangeRegistrations != null
        && !component.mWorkingRangeRegistrations.isEmpty()) {
      node.addWorkingRanges(component.mWorkingRangeRegistrations);
    }

    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    return node;
  }

  static InternalNode create(
      ComponentContext parentContext,
      final InternalNode holder,
      final int widthSpec,
      final int heightSpec) {

    final Component component = holder.getTailComponent();
    final InternalNode currentLayout = holder.getNestedTree();

    // Find the immediate parent context
    List<Component> components = holder.getComponents();
    if (components.size() > 1) {
      parentContext =
          components
              .get(components.size() - 2)
              .getScopedContext(parentContext.getLayoutStateContext());
    }

    if (component == null) {
      throw new IllegalArgumentException("A component is required to resolve a nested tree.");
    }

    // The resolved layout to return.
    final InternalNode layout;

    if (currentLayout == null
        || !hasCompatibleSizeSpec(
            currentLayout.getLastWidthSpec(),
            currentLayout.getLastHeightSpec(),
            widthSpec,
            heightSpec,
            currentLayout.getLastMeasuredWidth(),
            currentLayout.getLastMeasuredHeight())) {

      // Check if cached layout can be used.
      final InternalNode cachedLayout =
          consumeCachedLayout(parentContext, component, holder, widthSpec, heightSpec);

      if (cachedLayout != null) {

        // Use the cached layout.
        layout = cachedLayout;
      } else {

        // Check if previous layout can be remeasured and used.
        if (currentLayout != null && component.canUsePreviousLayout(parentContext)) {
          remeasure(currentLayout, widthSpec, heightSpec);
          layout = currentLayout;
        } else {

          /* INCLUDES LEGACY LOGIC */

          /*
           Copy the context so that it can have its own set of tree props. Robolectric tests
           need to original context of that tree props can be set externally.
           TODO: (T56146833) Do not copy the component context.
          */
          final ComponentContext context;
          if (!IS_TEST) {
            context = parentContext;
          } else {
            context = parentContext.makeNewCopy();
          }

          context.setTreeProps(holder.getPendingTreeProps());

          // Set the size specs in ComponentContext for the nested tree
          // TODO: (T48229905) size specs should be passed in as arguments.
          context.setWidthSpec(widthSpec);
          context.setHeightSpec(heightSpec);

          // Create a new layout.
          layout = create(context, component, true, true);

          holder.copyInto(layout);

          measure(parentContext, layout, widthSpec, heightSpec, holder.getDiffNode());
        }

        layout.setLastWidthSpec(widthSpec);
        layout.setLastHeightSpec(heightSpec);
        layout.setLastMeasuredHeight(layout.getHeight());
        layout.setLastMeasuredWidth(layout.getWidth());
      }

      holder.setNestedTree(layout);
    } else {

      // Use the previous layout.
      layout = currentLayout;
    }

    // This is checking only nested tree roots however should be moved to check all the tree roots.
    layout.assertContextSpecificStyleNotSet();

    return layout;
  }

  static @Nullable Component onCreateLayout(final ComponentContext c, final Component component) {
    final Component root = component.createComponentLayout(c);
    return root != null && root.getId() > 0 ? root : null;
  }

  /** TODO: This should be done in {@link Component#updateInternalChildState(ComponentContext)}. */
  static ComponentContext update(
      final ComponentContext parent, final Component original, final boolean reuseGlobalKey) {

    final Component component = original.getThreadSafeInstance();

    if (reuseGlobalKey) {
      component.setGlobalKey(original.getGlobalKey());
    }

    final TreeProps ancestor = parent.getTreeProps();

    // 1. Populate the TreeProps for component.
    component.populateTreeProps(ancestor);

    // 2. Update the internal state of the component wrt the parent.
    // 3. Get the scoped context from the updated component.
    final ComponentContext c = component.updateInternalChildState(parent);

    // 4. Set the TreeProps which will be passed to the descendants of the component.
    final TreeProps descendants = component.getTreePropsForChildren(c, ancestor);
    c.setTreeProps(descendants);

    if (ComponentsConfiguration.isDebugModeEnabled) {
      DebugComponent.applyOverrides(c, component);
    }

    return c;
  }

  static void measure(
      final ComponentContext c,
      final InternalNode root,
      final int widthSpec,
      final int heightSpec,
      final @Nullable DiffNode diff) {

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("measureTree:" + root.getSimpleName());
    }

    if (root.getStyleDirection() == com.facebook.yoga.YogaDirection.INHERIT
        && isLayoutDirectionRTL(c.getAndroidContext())) {
      root.layoutDirection(YogaDirection.RTL);
    }

    if (YogaConstants.isUndefined(root.getStyleWidth())) {
      root.setStyleWidthFromSpec(widthSpec);
    }
    if (YogaConstants.isUndefined(root.getStyleHeight())) {
      root.setStyleHeightFromSpec(heightSpec);
    }

    if (diff != null) {
      ComponentsSystrace.beginSection("applyDiffNode");
      applyDiffNodeToUnchangedNodes(root, diff);
      ComponentsSystrace.endSection(/* applyDiffNode */ );
    }

    root.calculateLayout(
        SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED
            ? YogaConstants.UNDEFINED
            : SizeSpec.getSize(widthSpec),
        SizeSpec.getMode(heightSpec) == SizeSpec.UNSPECIFIED
            ? YogaConstants.UNDEFINED
            : SizeSpec.getSize(heightSpec));

    if (isTracing) {
      ComponentsSystrace.endSection(/* measureTree */ );
    }
  }

  static void resumeCreateAndMeasureComponent(
      final ComponentContext c,
      final InternalNode root,
      final int widthSpec,
      final int heightSpec,
      final @Nullable DiffNode diff,
      final @Nullable PerfEvent logLayoutState) {

    if (root == NULL_LAYOUT) {
      return;
    }

    resume(root);

    if (logLayoutState != null) {
      logLayoutState.markerPoint("start_measure");
    }

    measure(c, root, widthSpec, heightSpec, diff);

    if (logLayoutState != null) {
      logLayoutState.markerPoint("end_measure");
    }
  }

  static void resume(final InternalNode root) {
    final List<Component> unresolved = root.getUnresolvedComponents();

    if (unresolved != null) {
      for (int i = 0, size = unresolved.size(); i < size; i++) {
        root.child(unresolved.get(i));
      }
      root.getUnresolvedComponents().clear();
    }

    for (int i = 0, size = root.getChildCount(); i < size; i++) {
      resume(root.getChildAt(i));
    }
  }

  @VisibleForTesting
  static void remeasure(final InternalNode layout, final int widthSpec, final int heightSpec) {
    if (layout == NULL_LAYOUT) { // If NULL LAYOUT return immediately.
      return;
    }

    layout.resetResolvedLayoutProperties(); // Reset all resolved props to force-remeasure.
    measure(layout.getContext(), layout, widthSpec, heightSpec, layout.getDiffNode());
  }

  /**
   * Traverses the layoutTree and the diffTree recursively. If a layoutNode has a compatible host
   * type {@link Layout#hostIsCompatible} it assigns the DiffNode to the layout node in order to try
   * to re-use the LayoutOutputs that will be generated during result collection. If a layout node
   * component returns false when shouldComponentUpdate is called with the DiffNode Component it
   * also tries to re-use the old measurements and therefore marks as valid the cachedMeasures for
   * the whole component subtree.
   *
   * @param layoutNode the root of the LayoutTree
   * @param diffNode the root of the diffTree
   */
  static void applyDiffNodeToUnchangedNodes(
      final InternalNode layoutNode, final DiffNode diffNode) {
    try {
      // Root of the main tree or of a nested tree.
      final boolean isTreeRoot = layoutNode.getParent() == null;
      if (isLayoutSpecWithSizeSpec(layoutNode.getTailComponent()) && !isTreeRoot) {
        layoutNode.setDiffNode(diffNode);
        return;
      }

      if (!hostIsCompatible(layoutNode, diffNode)) {
        return;
      }

      layoutNode.setDiffNode(diffNode);

      final int layoutCount = layoutNode.getChildCount();
      final int diffCount = diffNode.getChildCount();

      if (layoutCount != 0 && diffCount != 0) {
        for (int i = 0; i < layoutCount && i < diffCount; i++) {
          applyDiffNodeToUnchangedNodes(layoutNode.getChildAt(i), diffNode.getChildAt(i));
        }

        // Apply the DiffNode to a leaf node (i.e. MountSpec) only if it should NOT update.
      } else if (!shouldComponentUpdate(layoutNode, diffNode)) {
        applyDiffNodeToLayoutNode(layoutNode, diffNode);
      }
    } catch (Throwable t) {
      final Component c = layoutNode.getTailComponent();
      if (c != null) {
        throw new ComponentsChainException(c, t);
      }

      throw t;
    }
  }

  /**
   * Copies the inter stage state (if any) from the DiffNode's component to the layout node's
   * component, and declares that the cached measures on the diff node are valid for the layout
   * node.
   */
  private static void applyDiffNodeToLayoutNode(
      final InternalNode layoutNode, final DiffNode diffNode) {
    final Component component = layoutNode.getTailComponent();
    if (component != null) {
      component.copyInterStageImpl(diffNode.getComponent());
    }

    layoutNode.setCachedMeasuresValid(true);
  }

  @Nullable
  static InternalNode consumeCachedLayout(
      final ComponentContext c,
      final Component component,
      final InternalNode holder,
      final int widthSpec,
      final int heightSpec) {
    final LayoutState layoutState = c.getLayoutState();
    if (layoutState == null) {
      throw new IllegalStateException(
          component.getSimpleName()
              + ": Trying to access the cached InternalNode for a component outside of a"
              + " LayoutState calculation. If that is what you must do, see"
              + " Component#measureMightNotCacheInternalNode.");
    }

    final InternalNode cachedLayout = layoutState.getCachedLayout(component);

    if (cachedLayout != null) {
      layoutState.clearCachedLayout(component);

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
      if (hasValidDirection && hasCompatibleSizeSpec) {
        return cachedLayout;
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
      return TransitionUtils.areTransitionsEnabled(null);
    }
    return context.getComponentTree().areTransitionsEnabled();
  }

  /**
   * Returns true either if the two nodes have the same Component type or if both don't have a
   * Component.
   */
  private static boolean hostIsCompatible(final InternalNode node, final DiffNode diffNode) {
    if (diffNode == null) {
      return false;
    }

    return ComponentUtils.isSameComponentType(node.getTailComponent(), diffNode.getComponent());
  }

  private static boolean shouldComponentUpdate(
      final InternalNode layoutNode, final DiffNode diffNode) {
    if (diffNode == null) {
      return true;
    }

    final Component component = layoutNode.getTailComponent();
    if (component != null) {
      return component.shouldComponentUpdate(diffNode.getComponent(), component);
    }

    return true;
  }

  @VisibleForTesting
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

  private static void handle(ComponentContext parent, Component component, Exception exception) {
    final EventHandler<ErrorEvent> nextHandler = parent.getErrorEventHandler();
    final EventHandler<ErrorEvent> handler;
    final Exception original;
    final boolean rethrown;

    if (exception instanceof ReThrownException) {
      original = ((ReThrownException) exception).original;
      handler = ((ReThrownException) exception).handler;
      rethrown = true;
    } else if (exception instanceof ComponentsChainException) {
      if (((ComponentsChainException) exception).handler != null) {
        original = ((ComponentsChainException) exception).original;
        handler = ((ComponentsChainException) exception).handler;
        rethrown = false;
      } else {
        original = null;
        handler = null;
        rethrown = false;
      }
    } else {
      original = exception;
      handler = null;
      rethrown = false;
    }

    final Exception link;
    if (rethrown) {
      link = original;
    } else {
      link = exception;
    }

    if (handler == nextHandler) { // was and handled
      // propagate with updated component chain exception, handler, and original cause
      final ComponentsChainException chain = new ComponentsChainException(component, link);
      chain.original = original;
      chain.handler = handler;
      throw chain;
    } else if (nextHandler instanceof ErrorEventHandler) { // at the root
      // update component chain exception and call error handler
      final ComponentsChainException chain = new ComponentsChainException(component, link);
      ((ErrorEventHandler) nextHandler).onError(chain);
    } else { // Handle again with new handler
      try {
        ComponentLifecycle.dispatchErrorEvent(parent, link);
      } catch (ReThrownException ex) { // exception was raised again
        // propagate with updated component chain, latest handler, and original cause
        final ComponentsChainException chain = new ComponentsChainException(component, link);
        chain.original = original;
        chain.handler = nextHandler;
        throw chain;
      }
    }
  }
}
