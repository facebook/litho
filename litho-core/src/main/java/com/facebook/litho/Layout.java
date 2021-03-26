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
import com.facebook.litho.LithoLayoutResult.NestedTreeHolderResult;
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

  static LayoutResultHolder createAndMeasureComponent(
      final ComponentContext c,
      final Component component,
      final int widthSpec,
      final int heightSpec) {
    return createAndMeasureComponent(
        c, component, null, widthSpec, heightSpec, null, null, null, null);
  }

  /* TODO: (T81557408) Fix @Nullable issue */
  static LayoutResultHolder createAndMeasureComponent(
      final ComponentContext c,
      final Component component,
      @Nullable final String globalKeyToReuse,
      final int widthSpec,
      final int heightSpec,
      final @Nullable InternalNode current,
      final @Nullable LayoutStateContext prevLayoutStateContext,
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

        return LayoutResultHolder.interrupted(layout);
      } else {
        // Layout is complete, disable interruption from this point on.
        c.markLayoutUninterruptible();
      }

    } else {
      ComponentContext updatedScopedContext = update(c, component, true, globalKeyToReuse);
      Component updated = updatedScopedContext.getComponentScope();
      layout =
          current.reconcile(c, updated, ComponentUtils.getGlobalKey(updated, globalKeyToReuse));
    }

    if (layoutStatePerfEvent != null) {
      final String event = current == null ? EVENT_END_CREATE_LAYOUT : EVENT_END_RECONCILE;
      layoutStatePerfEvent.markerPoint(event);
    }

    if (layoutStatePerfEvent != null) {
      layoutStatePerfEvent.markerPoint("start_measure");
    }

    measure(c, layout, widthSpec, heightSpec, prevLayoutStateContext, diff);

    if (layoutStatePerfEvent != null) {
      layoutStatePerfEvent.markerPoint("end_measure");
    }

    return new LayoutResultHolder(layout);
  }

  public static InternalNode create(final ComponentContext parent, final Component component) {
    return create(parent, component, false, false, null);
  }

  static InternalNode create(
      final ComponentContext parent, final Component component, final boolean resolveNestedTree) {
    return create(parent, component, resolveNestedTree, false, null);
  }

  static InternalNode create(
      final ComponentContext parent,
      Component component,
      final boolean resolveNestedTree,
      final boolean reuseGlobalKey,
      final @Nullable String globalKeyToReuse) {

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("createLayout:" + component.getSimpleName());
    }

    final InternalNode node;
    final ComponentContext c;
    final String globalKey;

    try {

      // 1. Consume the layout created in `willrender`.
      final InternalNode cached = component.consumeLayoutCreatedInWillRender(parent);

      // 2. Return immediately if cached layout is available.
      if (cached != null) {
        return cached;
      }

      // 4. Update the component.
      // 5. Get the scoped context of the updated component.
      c = update(parent, component, reuseGlobalKey, globalKeyToReuse);
      globalKey = c.getGlobalKey();
      component = c.getComponentScope();

      // 6. Resolve the component into an InternalNode tree.

      final boolean shouldDeferNestedTreeResolution =
          isNestedTree(c, component) && !resolveNestedTree;

      // If nested tree resolution is deferred, then create an nested tree holder.
      if (shouldDeferNestedTreeResolution) {
        node = InternalNodeUtils.createNestedTreeHolder(c, c.getTreeProps());
      }

      // If the component can resolve itself resolve it.
      else if (component.canResolve()) {

        // Resolve the component into an InternalNode.
        node = component.resolve(c);
      }

      // If the component is a MountSpec.
      else if (isMountSpec(component)) {

        // Create a blank InternalNode for MountSpecs and set the default flex direction.
        node = InternalNodeUtils.create(c).flexDirection(YogaFlexDirection.COLUMN);
      }

      // If the component is a LayoutSpec.
      else if (isLayoutSpec(component)) {

        final RenderResult renderResult = component.render(c);
        final Component root = renderResult.component;

        // TODO: (T57741374) this step is required because of a bug in redex.
        if (root == component) {
          node = root.resolve(c);
        } else if (root != null) {
          node = create(c, root, false);
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
      if (node == null || node == NULL_LAYOUT) {
        return NULL_LAYOUT;
      }

    } catch (Exception e) {
      ComponentUtils.handleWithHierarchy(parent, component, e);
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
            ComponentLifecycle.getYogaMeasureFunction(component, c.getLayoutStateContext()));
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
    node.appendComponent(component, ComponentUtils.getGlobalKey(component, globalKey));

    // 11. Create and add transition to this component's InternalNode.
    if (areTransitionsEnabled(c)) {
      if (component.needsPreviousRenderData()) {
        node.addComponentNeedingPreviousRenderData(globalKey, component);
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
      node.addAttachable(new LayoutSpecAttachable(globalKey, component));
    }

    // 13. Call onPrepare for MountSpecs.
    if (isMountSpec(component)) {
      try {
        component.onPrepare(c);
      } catch (Exception e) {
        ComponentUtils.handleWithHierarchy(parent, component, e);
      }
    }

    // 14. Add working ranges to the InternalNode.
    Component.addWorkingRangeToNode(node, c, component);

    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    return node;
  }

  static LithoLayoutResult create(
      ComponentContext parentContext,
      final NestedTreeHolderResult holder,
      final int widthSpec,
      final int heightSpec,
      final @Nullable LayoutStateContext prevLayoutStateContext) {

    final Component component = holder.getTailComponent();
    final String componentGlobalKey = holder.getTailComponentKey();
    final LithoLayoutResult currentLayout = holder.getNestedResult();

    // Find the immediate parent context
    List<Component> components = holder.getComponents();
    List<String> componentKeys = holder.getComponentKeys();
    if (components.size() > 1) {
      int index = components.size() - 2;
      final Component parent = components.get(index);
      final String parentGlobalKey =
          ComponentUtils.getGlobalKey(
              parent, componentKeys == null ? null : componentKeys.get(index));

      parentContext =
          parent.getScopedContext(parentContext.getLayoutStateContext(), parentGlobalKey);
    }

    if (component == null) {
      throw new IllegalArgumentException("A component is required to resolve a nested tree.");
    }

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

      // Check if cached layout can be used.
      final LithoLayoutResult cachedLayout =
          consumeCachedLayout(parentContext, component, holder, widthSpec, heightSpec);

      if (cachedLayout != null) {

        // Use the cached layout.
        layout = cachedLayout;
      } else {

        // Check if previous layout can be remeasured and used.
        if (currentLayout != null && component.canUsePreviousLayout(parentContext)) {
          // TODO: Avoid this hard cast, after splitting is complete.
          remeasure((InternalNode) currentLayout, widthSpec, heightSpec, prevLayoutStateContext);
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
          layout = create(context, component, true, true, componentGlobalKey);

          // TODO: Avoid this hard cast, after splitting is complete.
          holder.copyInto((InternalNode) layout);

          measure(
              parentContext,
              (InternalNode) layout, // TODO: Avoid this hard cast, after splitting is complete.
              widthSpec,
              heightSpec,
              prevLayoutStateContext,
              holder.getDiffNode());
        }

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

    // This is checking only nested tree roots however should be moved to check all the tree roots.
    layout.assertContextSpecificStyleNotSet();

    return layout;
  }

  static void applyRenderResultToNode(RenderResult renderResult, InternalNode node) {
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

  /**
   * TODO: This should be done in {@link Component#updateInternalChildState(ComponentContext)}.
   * TODO: (T81557408) Fix @Nullable issue
   */
  static ComponentContext update(
      final ComponentContext parent,
      final Component original,
      final boolean reuseGlobalKey,
      @Nullable final String globalKeyToReuse) {

    final Component component = original.getThreadSafeInstance();

    if (reuseGlobalKey) {
      component.setGlobalKey(ComponentUtils.getGlobalKey(original, globalKeyToReuse));
    }

    final TreeProps ancestor = parent.getTreeProps();

    // 1. Populate the TreeProps for component.
    component.populateTreeProps(ancestor);

    // 2. Update the internal state of the component wrt the parent.
    // 3. Get the scoped context from the updated component.
    final ComponentContext c = component.updateInternalChildState(parent, globalKeyToReuse);

    // 4. Set the TreeProps which will be passed to the descendants of the component.
    final TreeProps descendants = component.getTreePropsForChildren(c, ancestor);
    c.setTreeProps(descendants);

    if (ComponentsConfiguration.isDebugModeEnabled) {
      DebugComponent.applyOverrides(c, component, c.getGlobalKey());
    }

    return c;
  }

  static void measure(
      final ComponentContext c,
      final InternalNode root,
      final int widthSpec,
      final int heightSpec,
      final @Nullable LayoutStateContext prevLayoutStateContext,
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
      applyDiffNodeToUnchangedNodes(c.getLayoutStateContext(), root, prevLayoutStateContext, diff);
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
      final @Nullable LayoutStateContext prevLayoutStateContext,
      final @Nullable DiffNode diff,
      final @Nullable PerfEvent logLayoutState) {

    if (root == NULL_LAYOUT) {
      return;
    }

    resume(root);

    if (logLayoutState != null) {
      logLayoutState.markerPoint("start_measure");
    }

    measure(c, root, widthSpec, heightSpec, prevLayoutStateContext, diff);

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
  static void remeasure(
      final InternalNode layout,
      final int widthSpec,
      final int heightSpec,
      final @Nullable LayoutStateContext prevLayoutStateContext) {
    if (layout == NULL_LAYOUT) { // If NULL LAYOUT return immediately.
      return;
    }

    layout.resetResolvedLayoutProperties(); // Reset all resolved props to force-remeasure.
    measure(
        layout.getContext(),
        layout,
        widthSpec,
        heightSpec,
        prevLayoutStateContext,
        layout.getDiffNode());
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
      final LayoutStateContext layoutStateContext,
      final InternalNode layoutNode,
      final @Nullable LayoutStateContext prevLayoutStateContext,
      final @Nullable DiffNode diffNode) {
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
          applyDiffNodeToUnchangedNodes(
              layoutStateContext,
              layoutNode.getChildAt(i),
              prevLayoutStateContext,
              diffNode.getChildAt(i));
        }

        // Apply the DiffNode to a leaf node (i.e. MountSpec) only if it should NOT update.
      } else if (!shouldComponentUpdate(
          layoutStateContext, layoutNode, prevLayoutStateContext, diffNode)) {
        applyDiffNodeToLayoutNode(layoutStateContext, layoutNode, prevLayoutStateContext, diffNode);
      }
    } catch (Throwable t) {
      final LithoMetadataExceptionWrapper e =
          new LithoMetadataExceptionWrapper(layoutNode.getContext(), t);
      final Component c = layoutNode.getTailComponent();
      if (c != null) {
        e.addComponentForLayoutStack(c);
      }
      throw e;
    }
  }

  /**
   * Copies the inter stage state (if any) from the DiffNode's component to the layout node's
   * component, and declares that the cached measures on the diff node are valid for the layout
   * node.
   */
  private static void applyDiffNodeToLayoutNode(
      final LayoutStateContext nextLayoutStateContext,
      final InternalNode layoutNode,
      final LayoutStateContext diffNodeLayoutStateContext,
      final DiffNode diffNode) {
    final Component component = layoutNode.getTailComponent();
    final String componentKey =
        ComponentUtils.getGlobalKey(component, layoutNode.getTailComponentKey());
    if (component != null) {
      component.copyInterStageImpl(
          component.getInterStagePropsContainer(nextLayoutStateContext, componentKey),
          diffNode
              .getComponent()
              .getInterStagePropsContainer(
                  diffNodeLayoutStateContext, diffNode.getComponentGlobalKey()));
    }

    layoutNode.setCachedMeasuresValid(true);
  }

  @Nullable
  static LithoLayoutResult consumeCachedLayout(
      final ComponentContext c,
      final Component component,
      final NestedTreeHolderResult holder,
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
      return AnimationsDebug.areTransitionsEnabled(null);
    }
    return context.getComponentTree().areTransitionsEnabled();
  }

  /**
   * Returns true either if the two nodes have the same Component type or if both don't have a
   * Component.
   */
  private static boolean hostIsCompatible(
      final InternalNode node, final @Nullable DiffNode diffNode) {
    if (diffNode == null) {
      return false;
    }

    return ComponentUtils.isSameComponentType(node.getTailComponent(), diffNode.getComponent());
  }

  private static boolean shouldComponentUpdate(
      final LayoutStateContext layoutStateContext,
      final InternalNode layoutNode,
      final @Nullable LayoutStateContext prevLayoutStateContext,
      final @Nullable DiffNode diffNode) {
    if (diffNode == null) {
      return true;
    }

    final Component component = layoutNode.getTailComponent();

    if (component != null) {
      final String globalKey =
          ComponentUtils.getGlobalKey(component, layoutNode.getTailComponentKey());
      final ComponentContext scopedContext =
          component.getScopedContext(layoutStateContext, globalKey);

      try {
        if (component.isStateless()) {
          Component diffNodeComponent = diffNode.getComponent();

          return component.shouldUpdate(
              diffNodeComponent,
              diffNodeComponent == null || prevLayoutStateContext == null
                  ? null
                  : diffNodeComponent.getStateContainer(
                      prevLayoutStateContext,
                      ComponentUtils.getGlobalKey(
                          diffNodeComponent, diffNode.getComponentGlobalKey())),
              component,
              component.getStateContainer(layoutStateContext, globalKey));
        } else {
          return component.shouldUpdate(
              getDiffNodeScopedContext(layoutStateContext, prevLayoutStateContext, diffNode),
              diffNode.getComponent(),
              scopedContext,
              component);
        }
      } catch (Exception e) {
        ComponentUtils.handleWithHierarchy(scopedContext, component, e);
      }
    }

    return true;
  }

  /** DiffNode state should be retrieved from the committed LayoutState. */
  private static ComponentContext getDiffNodeScopedContext(
      LayoutStateContext currentLayoutStateContext,
      final @Nullable LayoutStateContext prevLayoutStateContext,
      DiffNode diffNode) {
    final Component diffNodeComponent = diffNode.getComponent();
    if (diffNodeComponent == null) {
      return null;
    }

    final LayoutStateContext committedContext;
    if (diffNodeComponent.isStateless()) {
      committedContext = prevLayoutStateContext;
    } else {
      if (currentLayoutStateContext == null) {
        return null;
      }

      final ComponentTree componentTree = currentLayoutStateContext.getComponentTree();
      if (componentTree == null) {
        return null;
      }

      committedContext = componentTree.getLayoutStateContext();
    }

    if (committedContext == null) {
      return null;
    }

    return diffNodeComponent.getScopedContext(
        committedContext,
        ComponentUtils.getGlobalKey(diffNodeComponent, diffNode.getComponentGlobalKey()));
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
}
