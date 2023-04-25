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

import static com.facebook.litho.NodeInfo.ENABLED_UNSET;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.rendercore.utils.MeasureSpecUtils;
import com.facebook.yoga.YogaFlexDirection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class Resolver {

  private static final String EVENT_START_CREATE_LAYOUT = "start_create_layout";
  private static final String EVENT_END_CREATE_LAYOUT = "end_create_layout";
  private static final String EVENT_START_RECONCILE = "start_reconcile_layout";
  private static final String EVENT_END_RECONCILE = "end_reconcile_layout";
  private static final int MEASURE_SPEC_UNSPECIFIED = MeasureSpecUtils.unspecified();

  static @Nullable LithoNode resolveTree(
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

      if (node != null && !resolveStateContext.isLayoutInterrupted()) {
        node.applyParentDependentCommonProps(resolveStateContext);
      }

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
          reconcile(
              resolveStateContext,
              c,
              current,
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
    return resolveImpl(
        resolveStateContext,
        parent,
        MEASURE_SPEC_UNSPECIFIED,
        MEASURE_SPEC_UNSPECIFIED,
        component,
        false,
        null);
  }

  static @Nullable LithoNode resolveWithGlobalKey(
      final ResolveStateContext resolveStateContext,
      final ComponentContext parent,
      final Component component,
      final @Nullable String globalKeyToReuse) {
    return resolveImpl(
        resolveStateContext,
        parent,
        MEASURE_SPEC_UNSPECIFIED,
        MEASURE_SPEC_UNSPECIFIED,
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
      ComponentsSystrace.beginSection("resolve:" + component.getSimpleName());
      ComponentsSystrace.beginSection("create-node:" + component.getSimpleName());
    }

    ComponentsLogger componentsLogger = resolveStateContext.getComponentsLogger();
    PerfEvent resolveLayoutCreationEvent =
        createPerformanceEvent(
            component, componentsLogger, FrameworkLogEvents.EVENT_COMPONENT_RESOLVE);

    final LithoNode node;
    final ComponentContext c;
    final String globalKey;
    final boolean isNestedTree = Component.isNestedTree(component);
    final boolean hasCachedNode = Component.hasCachedNode(resolveStateContext, component);
    final ScopedComponentInfo scopedComponentInfo;

    try {
      // 1. Consume the layout created in `willrender`.
      final LithoNode cached =
          component.consumeLayoutCreatedInWillRender(resolveStateContext, parent);

      // 2. Return immediately if cached layout is available.
      if (cached != null) {
        if (isTracing) {
          // end create-node
          ComponentsSystrace.endSection();
          // end-resolve
          ComponentsSystrace.endSection();
        }
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
                c.getTreeProps(), resolveStateContext.getCache().getCachedNode(component), parent);
      }

      // If the component can resolve itself resolve it.
      else if (component.canResolve()) {

        // Resolve the component into an InternalNode.
        node = component.resolve(resolveStateContext, c);
      }

      // If the component is a MountSpec (including MountableComponents and PrimitiveComponents).
      else if (Component.isMountSpec(component)) {

        // Create a blank InternalNode for MountSpecs and set the default flex direction.
        node = new LithoNode();
        node.flexDirection(YogaFlexDirection.COLUMN);

        // Call onPrepare for MountSpecs or prepare for MountableComponents.
        PerfEvent prepareEvent =
            createPerformanceEvent(
                component, componentsLogger, FrameworkLogEvents.EVENT_COMPONENT_PREPARE);

        if (isTracing) {
          ComponentsSystrace.beginSection("prepare:" + component.getSimpleName());
        }

        PrepareResult prepareResult =
            component.prepare(resolveStateContext, scopedComponentInfo.getContext());

        if (prepareEvent != null && componentsLogger != null) {
          componentsLogger.logPerfEvent(prepareEvent);
        }

        if (prepareResult != null) {
          if (Component.isMountable(component) && prepareResult.mountable != null) {
            node.setMountable(prepareResult.mountable);
          } else if (Component.isPrimitive(component) && prepareResult.primitive != null) {
            node.setPrimitive(prepareResult.primitive);
          }
          applyTransitionsAndUseEffectEntriesToNode(
              prepareResult.transitions, prepareResult.useEffectEntries, node);
        }
        if (isTracing) {
          // end of prepare
          ComponentsSystrace.endSection();
        }
      }

      // If the component is a LayoutSpec.
      else if (Component.isLayoutSpec(component)) {

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
          node = c.isNullNodeEnabled() ? new NullNode() : null;
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
        if (isTracing) {
          // end create-node
          ComponentsSystrace.endSection();
          // end-resolve
          ComponentsSystrace.endSection();
        }
        return null;
      }

      if (isTracing) {
        // end create-node
        ComponentsSystrace.endSection();
      }
    } catch (Exception e) {
      ComponentUtils.handleWithHierarchy(parent, component, e);
      if (isTracing) {
        // end create-node
        ComponentsSystrace.endSection();
        // end resolve
        ComponentsSystrace.endSection();
      }
      return null;
    }

    if (resolveLayoutCreationEvent != null && componentsLogger != null) {
      componentsLogger.logPerfEvent(resolveLayoutCreationEvent);
    }

    if (isTracing) {
      ComponentsSystrace.beginSection("after-create-node:" + component.getSimpleName());
    }

    // 8. Set the measure function
    // Set measure func on the root node of the generated tree so that the mount calls use
    // those (see Controller.mountNodeTree()). Handle the case where the component simply
    // delegates its layout creation to another component, i.e. the root node belongs to
    // another component.
    if (node.getComponentCount() == 0) {
      final boolean isMountSpecWithMeasure =
          component.canMeasure() && Component.isMountSpec(component);
      if (isMountSpecWithMeasure || ((isNestedTree || hasCachedNode) && !resolveNestedTree)) {
        node.setMeasureFunction(Component.sMeasureFunction);
      }
    }

    // 9. Copy the common props
    // Skip if resolving a layout with size spec because common props were copied in the previous
    // layout pass.
    final CommonProps commonProps = component.getCommonProps();
    if (commonProps != null
        && !(Component.isLayoutSpecWithSizeSpec(component) && resolveNestedTree)) {
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
      // end of after-create-node
      ComponentsSystrace.endSection();
    }

    if (isTracing) {
      // end of resolve
      ComponentsSystrace.endSection();
    }

    return node;
  }

  /**
   * Creates a {@link PerfEvent} for the given {@param eventId}. If the used {@link
   * ComponentsLogger} is not interested in that event, it will return <code>null</code>.
   */
  @Nullable
  private static PerfEvent createPerformanceEvent(
      Component component,
      @Nullable ComponentsLogger componentsLogger,
      @FrameworkLogEvents.LogEventId int eventId) {
    PerfEvent event = null;
    if (componentsLogger != null) {
      event = componentsLogger.newPerformanceEvent(eventId);
      if (event != null) {
        event.markerAnnotate(FrameworkLogEvents.PARAM_COMPONENT, component.getSimpleName());
        event.markerAnnotate(FrameworkLogEvents.PARAM_IS_MAIN_THREAD, ThreadUtils.isMainThread());
      }
    }
    return event;
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

    root.applyParentDependentCommonProps(resolveStateContext);

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

  public static @Nullable List<Attachable> collectAttachables(@Nullable final LithoNode node) {
    if (node == null) {
      return null;
    }

    final List<Attachable> collected = new ArrayList<>();
    collectAttachables(node, collected);
    return collected.isEmpty() ? null : collected;
  }

  private static void collectAttachables(final LithoNode node, final List<Attachable> collected) {

    // TODO(T143986616): optimise traversal for reused nodes

    for (int i = 0; i < node.getChildCount(); i++) {
      collectAttachables(node.getChildAt(i), collected);
    }

    final @Nullable List<Attachable> list = node.getAttachables();
    if (list != null) {
      collected.addAll(list);
    }
  }

  private static @Nullable LithoNode reconcile(
      final ResolveStateContext resolveStateContext,
      final ComponentContext c,
      final LithoNode node,
      final Component next,
      final ScopedComponentInfo nextScopedComponentInfo,
      final @Nullable String nextKey) {
    final TreeState treeState = resolveStateContext.getTreeState();
    final Set<String> keys;
    if (treeState == null) {
      keys = Collections.emptySet();
    } else {
      keys = treeState.getKeysForPendingStateUpdates();
    }

    return reconcile(
        resolveStateContext, c, node, next, nextScopedComponentInfo, nextKey, keys, null);
  }

  /**
   * Internal method to <b>try</b> and reconcile the {@param current} LithoNode with a new {@link
   * ComponentContext} and an updated head {@link Component}.
   *
   * @param resolveStateContext The RenderStateContext.
   * @param parentContext The ComponentContext.
   * @param current The current LithoNode which should be updated.
   * @param next The updated component to be used to reconcile this LithoNode.
   * @param keys The keys of mutated components.
   * @return A new updated LithoNode.
   */
  private static @Nullable LithoNode reconcile(
      final ResolveStateContext resolveStateContext,
      final ComponentContext parentContext,
      final LithoNode current,
      final Component next,
      final ScopedComponentInfo nextScopedComponentInfo,
      final @Nullable String nextKey,
      final Set<String> keys,
      final @Nullable LithoNode parent) {
    final int mode = getReconciliationMode(nextScopedComponentInfo.getContext(), current, keys);
    final LithoNode layout;
    switch (mode) {
      case ReconciliationMode.REUSE:
        commitToLayoutStateRecursively(resolveStateContext, current);
        layout = current;
        break;
      case ReconciliationMode.RECONCILE:
        layout = reconcile(resolveStateContext, current, next, keys);
        break;
      case ReconciliationMode.RECREATE:
        layout =
            Resolver.resolveWithGlobalKey(
                resolveStateContext, parentContext, next, Preconditions.checkNotNull(nextKey));
        if (layout != null) {
          if (parent == null) {
            layout.applyParentDependentCommonProps(resolveStateContext);
          } else {
            layout.applyParentDependentCommonProps(
                resolveStateContext,
                parent.getImportantForAccessibility(),
                parent.getNodeInfo() != null
                    ? parent.getNodeInfo().getEnabledState()
                    : ENABLED_UNSET,
                parent.isDuplicateParentStateEnabled());
          }
        }
        break;
      default:
        throw new IllegalArgumentException(mode + " is not a valid ReconciliationMode");
    }

    return layout;
  }

  /**
   * Internal method to reconcile the {@param current} LithoNode with a new {@link ComponentContext}
   * and an updated head {@link Component} and a {@link ReconciliationMode}.
   *
   * @param current The current LithoNode which should be updated.
   * @param next The updated component to be used to reconcile this LithoNode.
   * @param keys The keys of mutated components.
   * @return A new updated LithoNode.
   */
  private static LithoNode reconcile(
      final ResolveStateContext resolveStateContext,
      final LithoNode current,
      final Component next,
      final Set<String> keys) {

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("reconcile:" + next.getSimpleName());
    }

    // 2. Shallow copy this layout.
    final LithoNode layout;

    layout = current.clone();
    layout.setChildren(new ArrayList<>(current.getChildCount()));
    layout.resetDebugInfo();
    commitToLayoutState(resolveStateContext, current);

    ComponentContext parentContext = layout.getTailComponentContext();

    // 3. Iterate over children.
    int count = current.getChildCount();
    for (int i = 0; i < count; i++) {
      final LithoNode child = current.getChildAt(i);

      // 3.1 Get the head component of the child layout.
      int index = Math.max(0, child.getComponentCount() - 1);
      final Component component = child.getComponentAt(index);
      final String key = child.getGlobalKeyAt(index);
      final ScopedComponentInfo scopedComponentInfo = child.getComponentInfoAt(index);

      // 3.2 Reconcile child layout.
      final LithoNode copy =
          reconcile(
              resolveStateContext,
              parentContext,
              child,
              component,
              scopedComponentInfo,
              key,
              keys,
              current);

      // 3.3 Add the child to the cloned yoga node
      layout.child(copy);
    }

    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    return layout;
  }

  public static void commitToLayoutStateRecursively(ResolveStateContext c, LithoNode node) {
    final int count = node.getChildCount();
    commitToLayoutState(c, node);
    for (int i = 0; i < count; i++) {
      commitToLayoutStateRecursively(c, node.getChildAt(i));
    }
  }

  public static void commitToLayoutState(ResolveStateContext c, LithoNode node) {
    final List<ScopedComponentInfo> scopedComponentInfos = node.getScopedComponentInfos();

    for (ScopedComponentInfo info : scopedComponentInfos) {
      info.commitToLayoutState(c.getTreeState());
    }
  }

  /**
   * Returns the a {@link ReconciliationMode} mode which directs the reconciling process to branch
   * to either recreate the entire subtree, copy the entire subtree or continue to recursively
   * reconcile the subtree.
   */
  @VisibleForTesting
  static @ReconciliationMode int getReconciliationMode(
      final ComponentContext c, final LithoNode current, final Set<String> mutatedKeys) {
    final List<ScopedComponentInfo> components = current.getScopedComponentInfos();

    // 1.0 check early exit conditions
    if (c == null || current instanceof NestedTreeHolder) {
      return ReconciliationMode.RECREATE;
    }

    // 1.1 Check if any component has mutations
    for (int i = 0, size = components.size(); i < size; i++) {
      final String key = components.get(i).getContext().getGlobalKey();
      if (mutatedKeys.contains(key)) {
        return ReconciliationMode.RECREATE;
      }
    }

    // 2.0 Check if any descendants have mutations
    final String rootKey = current.getHeadComponentKey();
    for (String key : mutatedKeys) {
      if (key.startsWith(rootKey)) {
        return ReconciliationMode.RECONCILE;
      }
    }

    return ReconciliationMode.REUSE;
  }

  @IntDef({ReconciliationMode.REUSE, ReconciliationMode.RECONCILE, ReconciliationMode.RECREATE})
  @interface ReconciliationMode {
    int RECONCILE = 1;
    int RECREATE = 2;
    int REUSE = 3;
  }
}
