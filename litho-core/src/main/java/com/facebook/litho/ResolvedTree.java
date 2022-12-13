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

import static com.facebook.litho.Component.hasCachedNode;
import static com.facebook.litho.Component.isLayoutSpec;
import static com.facebook.litho.Component.isLayoutSpecWithSizeSpec;
import static com.facebook.litho.Component.isMountSpec;
import static com.facebook.litho.Component.isMountable;
import static com.facebook.litho.Component.isNestedTree;
import static com.facebook.litho.Component.sMeasureFunction;
import static com.facebook.rendercore.utils.MeasureSpecUtils.unspecified;

import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.yoga.YogaFlexDirection;
import java.util.List;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class ResolvedTree {

  private static final String EVENT_START_CREATE_LAYOUT = "start_create_layout";
  private static final String EVENT_END_CREATE_LAYOUT = "end_create_layout";
  private static final String EVENT_START_RECONCILE = "start_reconcile_layout";
  private static final String EVENT_END_RECONCILE = "end_reconcile_layout";

  static @Nullable LithoNode createResolvedTree(
      final ResolveStateContext resolveStateContext,
      final ComponentContext c,
      final Component component) {

    final @Nullable LithoNode current = resolveStateContext.getCurrentRoot();
    final @Nullable PerfEvent layoutStatePerfEvent = resolveStateContext.getPerfEventLogger();

    final boolean isReconcilable =
        isReconcilable(
            c, component, Preconditions.checkNotNull(resolveStateContext.getTreeState()), current);

    try {
      resolveStateContext.getTreeState().applyStateUpdatesEarly(c, component, current, false);
    } catch (Exception ex) {
      ComponentUtils.handleWithHierarchy(c, component, ex);
      return null;
    }

    if (layoutStatePerfEvent != null) {
      final String event = isReconcilable ? EVENT_START_RECONCILE : EVENT_START_CREATE_LAYOUT;
      layoutStatePerfEvent.markerPoint(event);
    }

    final @Nullable LithoNode node;
    if (!isReconcilable) {
      node = resolve(resolveStateContext, c, component);

      // This needs to finish layout on the UI thread.
      if (node != null && resolveStateContext.isLayoutInterrupted()) {
        if (layoutStatePerfEvent != null) {
          layoutStatePerfEvent.markerPoint(EVENT_END_CREATE_LAYOUT);
        }

        return node;
      } else {
        // Layout is complete, disable interruption from this point on.
        resolveStateContext.markLayoutUninterruptible();
      }
    } else {
      final String globalKeyToReuse = Preconditions.checkNotNull(current).getHeadComponentKey();

      if (globalKeyToReuse == null) {
        throw new IllegalStateException("Cannot reuse a null global key");
      }

      final ComponentContext updatedScopedContext =
          createScopedContext(resolveStateContext, c, component, globalKeyToReuse);

      node =
          current.reconcile(
              resolveStateContext,
              c,
              component,
              updatedScopedContext.getScopedComponentInfo(),
              globalKeyToReuse);
    }

    if (layoutStatePerfEvent != null) {
      final String event = current == null ? EVENT_END_CREATE_LAYOUT : EVENT_END_RECONCILE;
      layoutStatePerfEvent.markerPoint(event);
    }

    return node;
  }

  public @Nullable static LithoNode resolve(
      final ResolveStateContext resolveStateContext,
      final ComponentContext parent,
      final Component component) {
    return resolveWithGlobalKey(resolveStateContext, parent, component, null);
  }

  static @Nullable LithoNode resolveWithGlobalKey(
      final ResolveStateContext resolveStateContext,
      final ComponentContext parent,
      final Component component,
      final @Nullable String globalKeyToReuse) {
    return resolveImpl(
        resolveStateContext,
        parent,
        unspecified(),
        unspecified(),
        component,
        false,
        globalKeyToReuse);
  }

  static @Nullable LithoNode resolveImpl(
      final ResolveStateContext resolveStateContext,
      final ComponentContext parent,
      final int parentWidthSpec,
      final int parentHeightSpec,
      Component component,
      final boolean resolveNestedTree,
      final @Nullable String globalKeyToReuse) {

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("createLayout:" + component.getSimpleName());
    }

    final LithoNode node;
    final ComponentContext c;
    final String globalKey;
    final boolean isNestedTree = isNestedTree(component);
    final boolean hasCachedNode = hasCachedNode(resolveStateContext, component);
    final ScopedComponentInfo scopedComponentInfo;

    try {

      // 1. Consume the layout created in `willrender`.
      final LithoNode cached =
          component.consumeLayoutCreatedInWillRender(resolveStateContext, parent);

      // 2. Return immediately if cached layout is available.
      if (cached != null) {
        return cached;
      }

      // 4. Update the component.
      // 5. Get the scoped context of the updated component.
      c = createScopedContext(resolveStateContext, parent, component, globalKeyToReuse);
      globalKey = c.getGlobalKey();

      scopedComponentInfo = c.getScopedComponentInfo();
      // 6. Resolve the component into an InternalNode tree.

      final boolean shouldDeferNestedTreeResolution =
          (isNestedTree || hasCachedNode) && !resolveNestedTree;

      // If nested tree resolution is deferred, then create an nested tree holder.
      if (shouldDeferNestedTreeResolution) {
        node =
            new NestedTreeHolder(
                c.getTreeProps(), resolveStateContext.getCache().getCachedNode(component));
      }

      // If the component can resolve itself resolve it.
      else if (component.canResolve()) {

        // Resolve the component into an InternalNode.
        node = component.resolve(resolveStateContext, c);
      }

      // If the component is a MountSpec (including MountableComponents).
      else if (isMountSpec(component)) {

        // Create a blank InternalNode for MountSpecs and set the default flex direction.
        node = new LithoNode();
        node.flexDirection(YogaFlexDirection.COLUMN);

        // Call onPrepare for MountSpecs or prepare for MountableComponents.
        PrepareResult prepareResult =
            component.prepare(resolveStateContext, scopedComponentInfo.getContext());

        if (isMountable(component) && prepareResult == null) {
          throw new RuntimeException(
              "PrepareResult is null for a MountableComponent in Layout.create()");
        }

        if (prepareResult != null) {
          node.setMountable(prepareResult.mountable);
          applyTransitionsAndUseEffectEntriesToNode(
              prepareResult.transitions, prepareResult.useEffectEntries, node);
        }
      }

      // If the component is a LayoutSpec.
      else if (isLayoutSpec(component)) {

        final RenderResult renderResult =
            component.render(resolveStateContext, c, parentWidthSpec, parentHeightSpec);
        final Component root = renderResult.component;

        if (root != null) {
          // TODO: (T57741374) this step is required because of a bug in redex.
          if (root == component) {
            node = root.resolve(resolveStateContext, c);
          } else {
            node = resolve(resolveStateContext, c, root);
          }
        } else {
          node = null;
        }

        if (renderResult != null && node != null) {
          applyTransitionsAndUseEffectEntriesToNode(
              renderResult.transitions, renderResult.useEffectEntries, node);
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
      if (isMountSpecWithMeasure || ((isNestedTree || hasCachedNode) && !resolveNestedTree)) {
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
    if (c.areTransitionsEnabled()) {
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
      ComponentsSystrace.endSection();
    }

    return node;
  }

  static LithoNode resumeResolvingTree(
      final ResolveStateContext resolveStateContext, final LithoNode root) {
    final List<Component> unresolved = root.getUnresolvedComponents();

    if (unresolved != null) {
      final ComponentContext context = root.getTailComponentContext();
      for (int i = 0, size = unresolved.size(); i < size; i++) {
        root.child(resolveStateContext, context, unresolved.get(i));
      }
      unresolved.clear();
    }

    for (int i = 0, size = root.getChildCount(); i < size; i++) {
      resumeResolvingTree(resolveStateContext, root.getChildAt(i));
    }
    return root;
  }

  static ComponentContext createScopedContext(
      final ResolveStateContext resolveStateContext,
      final ComponentContext parent,
      final Component component,
      @Nullable final String globalKeyToReuse) {
    final String globalKey =
        globalKeyToReuse == null
            ? ComponentKeyUtils.generateGlobalKey(parent, parent.getComponentScope(), component)
            : globalKeyToReuse;
    final ComponentContext c = ComponentContext.withComponentScope(parent, component, globalKey);

    // Set latest state and the TreeProps which will be passed to the descendants of the component.
    if (component instanceof SpecGeneratedComponent) {
      final SpecGeneratedComponent specComponent = (SpecGeneratedComponent) component;
      if (specComponent.hasState()) {
        c.getScopedComponentInfo()
            .setStateContainer(
                resolveStateContext
                    .getTreeState()
                    .createOrGetStateContainerForComponent(c, specComponent, globalKey));
      }

      // Note: state must be set (via ScopedComponentInfo.setStateContainer) before invoking
      // getTreePropsForChildren as @OnCreateTreeProps can depend on @State
      final TreeProps ancestor = parent.getTreeProps();
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

  static void applyTransitionsAndUseEffectEntriesToNode(
      @Nullable List<Transition> transitions,
      @Nullable List<Attachable> useEffectEntries,
      LithoNode node) {
    if (transitions != null) {
      for (Transition t : transitions) {
        node.addTransition(t);
      }
    }
    if (useEffectEntries != null) {
      for (Attachable attachable : useEffectEntries) {
        node.addAttachable(attachable);
      }
    }
  }

  static boolean isReconcilable(
      final ComponentContext c,
      final Component nextRootComponent,
      final TreeState treeState,
      final @Nullable LithoNode currentLayoutResult) {

    if (currentLayoutResult == null || !c.isReconciliationEnabled()) {
      return false;
    }

    if (!treeState.hasUncommittedUpdates()) {
      return false;
    }

    final Component currentRootComponent = currentLayoutResult.getHeadComponent();

    if (!nextRootComponent.getKey().equals(currentRootComponent.getKey())) {
      return false;
    }

    if (!ComponentUtils.isSameComponentType(currentRootComponent, nextRootComponent)) {
      return false;
    }

    return ComponentUtils.isEquivalent(currentRootComponent, nextRootComponent);
  }
}
