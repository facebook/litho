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

import static com.facebook.litho.Component.isLayoutSpec;
import static com.facebook.litho.Component.isLayoutSpecWithSizeSpec;
import static com.facebook.litho.Component.isMountSpec;
import static com.facebook.litho.Component.isNestedTree;
import static com.facebook.litho.ComponentContext.NULL_LAYOUT;
import static com.facebook.litho.LayoutState.IS_TEST;
import static com.facebook.litho.LayoutState.areTransitionsEnabled;
import static com.facebook.litho.LayoutState.consumeCachedLayout;
import static com.facebook.litho.LayoutState.hasCompatibleSizeSpec;
import static com.facebook.litho.LayoutState.remeasureTree;

import androidx.annotation.Nullable;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaFlexDirection;

class Layout {

  private static final String EVENT_START_CREATE_LAYOUT = "start_create_layout";
  private static final String EVENT_END_CREATE_LAYOUT = "end_create_layout";
  private static final String EVENT_START_RECONCILE = "start_reconcile_layout";
  private static final String EVENT_END_RECONCILE = "end_reconcile_layout";

  static InternalNode createAndMeasureComponent(
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
    } else {
      Component updated = update(c, component, true);
      layout = current.reconcile(c, updated);
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

  static InternalNode create(
      final ComponentContext parent, Component component, final boolean resolveNestedTree) {
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

    // 1. Consume the layout created in `willrender`.
    final InternalNode cached = component.consumeLayoutCreatedInWillRender();

    // 2. Return immediately if cached layout is available.
    if (cached != null) {
      return cached;
    }

    // 4. Update the component.
    component = update(parent, component, reuseGlobalKey);

    // 5. Get the scoped context of the updated component.
    final ComponentContext c = component.getScopedContext();

    // 6. Resolve the component into an InternalNode tree.
    final InternalNode node;

    final boolean shouldDeferNestedTreeResolution =
        isNestedTree(c, component) && !resolveNestedTree;

    try {

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

    } catch (Throwable t) {
      throw new ComponentsChainException(component, t);
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
        node.setMeasureFunction(ComponentLifecycle.sMeasureFunction);
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
    node.appendComponent(component);

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

  static @Nullable Component onCreateLayout(final ComponentContext c, final Component component) {
    final Component root = component.createComponentLayout(c);
    return root != null && root.getId() > 0 ? root : null;
  }

  /** Replaces {@link LayoutState#resolveNestedTree(ComponentContext, InternalNode, int, int)} */
  static InternalNode create(
      final ComponentContext parentContext,
      final InternalNode holder,
      final int widthSpec,
      final int heightSpec) {

    final Component component = holder.getTailComponent();
    final InternalNode currentLayout = holder.getNestedTree();

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
          remeasureTree(currentLayout, widthSpec, heightSpec);
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

  static void measure(
      ComponentContext c,
      final InternalNode root,
      final int widthSpec,
      final int heightSpec,
      final @Nullable DiffNode diff) {
    if (root.getStyleDirection() == com.facebook.yoga.YogaDirection.INHERIT
        && LayoutState.isLayoutDirectionRTL(c.getAndroidContext())) {
      root.layoutDirection(YogaDirection.RTL);
    }

    LayoutState.measureTree(root, widthSpec, heightSpec, diff);
  }

  /** TODO: This should be done in {@link Component#updateInternalChildState(ComponentContext)}. */
  static Component update(
      final ComponentContext parent, final Component original, final boolean reuseGlobalKey) {

    final Component component = original.getThreadSafeInstance();

    if (reuseGlobalKey) {
      component.setGlobalKey(original.getGlobalKey());
    }

    final TreeProps ancestor = parent.getTreeProps();

    // 1. Populate the TreeProps for component.
    component.populateTreeProps(ancestor);

    // 2. Update the internal state of the component wrt the parent.
    component.updateInternalChildState(parent);

    // 3. Get the scoped context from the updated component.
    final ComponentContext c = component.getScopedContext();

    // 4. Set the TreeProps which will be passed to the descendants of the component.
    final TreeProps descendants = component.getTreePropsForChildren(c, ancestor);
    c.setTreeProps(descendants);

    if (ComponentsConfiguration.isDebugModeEnabled) {
      DebugComponent.applyOverrides(c, component);
    }

    return component;
  }
}
