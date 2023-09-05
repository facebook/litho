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
import static com.facebook.rendercore.debug.DebugEventDispatcher.beginTrace;
import static com.facebook.rendercore.debug.DebugEventDispatcher.endTrace;
import static com.facebook.rendercore.debug.DebugEventDispatcher.generateTraceIdentifier;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.debug.LithoDebugEvent;
import com.facebook.litho.debug.LithoDebugEventAttributes;
import com.facebook.rendercore.utils.MeasureSpecUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
      final ResolveContext resolveContext, final ComponentContext c, final Component component) {

    final @Nullable LithoNode current = resolveContext.getCurrentRoot();
    final @Nullable PerfEvent layoutStatePerfEvent = resolveContext.getPerfEventLogger();

    final boolean isReconcilable =
        isReconcilable(
            c, component, Preconditions.checkNotNull(resolveContext.getTreeState()), current);

    try {
      resolveContext.getTreeState().applyStateUpdatesEarly(c, component, current, false);
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
      node = resolve(resolveContext, c, component);

      if (node != null && !resolveContext.isLayoutInterrupted()) {
        node.applyParentDependentCommonProps(resolveContext);
      }

      // This needs to finish layout on the UI thread.
      if (node != null && resolveContext.isLayoutInterrupted()) {
        if (layoutStatePerfEvent != null) {
          layoutStatePerfEvent.markerPoint(EVENT_END_CREATE_LAYOUT);
        }

        return node;
      } else {
        // Layout is complete, disable interruption from this point on.
        resolveContext.markLayoutUninterruptible();
      }
    } else {
      final String globalKeyToReuse = Preconditions.checkNotNull(current).getHeadComponentKey();

      if (globalKeyToReuse == null) {
        throw new IllegalStateException("Cannot reuse a null global key");
      }

      node = reconcile(resolveContext, c, current, component, globalKeyToReuse);
    }

    if (layoutStatePerfEvent != null) {
      final String event = current == null ? EVENT_END_CREATE_LAYOUT : EVENT_END_RECONCILE;
      layoutStatePerfEvent.markerPoint(event);
    }

    return node;
  }

  public @Nullable static LithoNode resolve(
      final ResolveContext resolveContext,
      final ComponentContext parent,
      final Component component) {
    return resolveImpl(
        resolveContext,
        parent,
        MEASURE_SPEC_UNSPECIFIED,
        MEASURE_SPEC_UNSPECIFIED,
        component,
        false,
        null,
        null);
  }

  static @Nullable LithoNode resolveWithGlobalKey(
      final ResolveContext resolveContext,
      final ComponentContext parent,
      final Component component,
      final @Nullable String globalKeyToReuse) {
    return resolveImpl(
        resolveContext,
        parent,
        MEASURE_SPEC_UNSPECIFIED,
        MEASURE_SPEC_UNSPECIFIED,
        component,
        false,
        globalKeyToReuse,
        null);
  }

  static @Nullable LithoNode resolveImpl(
      final ResolveContext resolveContext,
      final ComponentContext parent,
      final int parentWidthSpec,
      final int parentHeightSpec,
      Component component,
      final boolean resolveNestedTree,
      final @Nullable String globalKeyToReuse,
      final @Nullable TreeProps treePropsToReuse) {

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("resolve:" + component.getSimpleName());
      ComponentsSystrace.beginSection("create-node:" + component.getSimpleName());
    }

    Integer componentResolvedIdentifier =
        generateTraceIdentifier(LithoDebugEvent.ComponentResolved);
    if (componentResolvedIdentifier != null) {
      HashMap<String, Object> attributes = new HashMap<>();
      attributes.put(LithoDebugEventAttributes.RunsOnMainThread, ThreadUtils.isMainThread());
      attributes.put(LithoDebugEventAttributes.Component, component.getSimpleName());

      beginTrace(
          componentResolvedIdentifier,
          LithoDebugEvent.ComponentResolved,
          String.valueOf(resolveContext.getTreeId()),
          attributes);
    }

    ComponentsLogger componentsLogger = resolveContext.getComponentsLogger();
    PerfEvent resolveLayoutCreationEvent =
        createPerformanceEvent(
            component, componentsLogger, FrameworkLogEvents.EVENT_COMPONENT_RESOLVE);

    final LithoNode node;
    final ComponentContext c;
    final String globalKey;
    final boolean isNestedTree = Component.isNestedTree(component);
    final boolean hasCachedNode = Component.hasCachedNode(resolveContext, component);
    final ScopedComponentInfo scopedComponentInfo;
    @Nullable CommonProps commonProps = null;

    try {
      // 1. Consume the layout created in `willrender`.
      final LithoNode cached = component.consumeLayoutCreatedInWillRender(resolveContext, parent);

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

      final boolean shouldDeferNestedTreeResolution =
          (isNestedTree || hasCachedNode) && !resolveNestedTree;

      // 5. Get or create the scoped context component.
      if (hasCachedNode) {
        final MeasuredResultCache cache = resolveContext.getCache();
        c = Preconditions.checkNotNull(cache.getCachedNode(component)).getHeadComponentContext();
      } else {
        c =
            createScopedContext(
                resolveContext, parent, component, globalKeyToReuse, treePropsToReuse);
      }

      globalKey = c.getGlobalKey();
      scopedComponentInfo = c.getScopedComponentInfo();

      // 6. Resolve the component into an InternalNode tree.

      // If nested tree resolution is deferred, then create a nested tree holder.
      if (shouldDeferNestedTreeResolution) {
        node =
            new NestedTreeHolder(
                c.getTreeProps(), resolveContext.getCache().getCachedNode(component), parent);
      } else {
        // Resolve the component into an InternalNode.
        ComponentResolveResult resolveResult =
            component.resolve(
                resolveContext,
                scopedComponentInfo,
                parentWidthSpec,
                parentHeightSpec,
                componentsLogger);
        node = resolveResult.lithoNode;
        commonProps = resolveResult.commonProps;
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
    } finally {
      if (componentResolvedIdentifier != null) {
        endTrace(componentResolvedIdentifier);
      }
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
    if (commonProps == null && (component instanceof SpecGeneratedComponent)) {
      // this step is still needed to make OCLWSS case work
      commonProps = ((SpecGeneratedComponent) component).getCommonProps();
    }
    if (!(node instanceof NullNode)) { // only if NOT a NullNode
      if (commonProps != null
          && !(Component.isLayoutSpecWithSizeSpec(component) && resolveNestedTree)) {
        commonProps.copyInto(c, node);
      }
    }

    // 10. Add the component to the InternalNode.
    scopedComponentInfo.setCommonProps(commonProps);
    node.appendComponent(scopedComponentInfo);

    // 11. Create and add transition to this component's InternalNode.
    if (c.areTransitionsEnabled()) {
      if (component instanceof SpecGeneratedComponent
          && ((SpecGeneratedComponent) component).needsPreviousRenderData()) {
        node.addComponentNeedingPreviousRenderData(globalKey, scopedComponentInfo);
      } else {
        try {
          // Calls onCreateTransition on the Spec.
          final Transition transition =
              (component instanceof SpecGeneratedComponent)
                  ? ((SpecGeneratedComponent) component).createTransition(c)
                  : null;
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

    /* 14. Add custom binders - the custom binders should be added to the RenderUnit as soon as they are created. For Primitives and Mountables, this happens during "prepare". However,
    for MountSpecs the common props are only initialized later, and some needed LithoNode for tail components is also filled later, and this is why we are moving the addition a bit below. */
    if (commonProps != null) {
      node.addCustomBinders(commonProps.getViewBinders());
    }

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
  static PerfEvent createPerformanceEvent(
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

  static LithoNode resumeResolvingTree(final ResolveContext resolveContext, final LithoNode root) {
    final List<Component> unresolved = root.getUnresolvedComponents();

    if (unresolved != null) {
      final ComponentContext context = root.getTailComponentContext();
      for (int i = 0, size = unresolved.size(); i < size; i++) {
        root.child(resolveContext, context, unresolved.get(i));
      }
      unresolved.clear();
    }

    for (int i = 0, size = root.getChildCount(); i < size; i++) {
      resumeResolvingTree(resolveContext, root.getChildAt(i));
    }

    root.applyParentDependentCommonProps(resolveContext);

    return root;
  }

  static ComponentContext createScopedContext(
      final ResolveContext resolveContext,
      final ComponentContext parent,
      final Component component,
      @Nullable final String globalKeyToReuse,
      @Nullable final TreeProps treePropsToReuse) {
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
                resolveContext
                    .getTreeState()
                    .createOrGetStateContainerForComponent(c, specComponent, globalKey));
      }

      // Note: state must be set (via ScopedComponentInfo.setStateContainer) before invoking
      // getTreePropsForChildren as @OnCreateTreeProps can depend on @State
      final TreeProps ancestor = parent.getTreeProps();
      c.setParentTreeProps(ancestor);

      final TreeProps descendants;
      if (treePropsToReuse != null) {
        descendants = treePropsToReuse;
      } else {
        descendants = ((SpecGeneratedComponent) component).getTreePropsForChildren(c, ancestor);
      }
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

  public static @Nullable Outputs collectOutputs(@Nullable final LithoNode node) {
    if (node == null) {
      return null;
    }

    final List<Attachable> collected = new ArrayList<>();
    collectOutputs(node, collected);
    return collected.isEmpty() ? null : new Outputs(collected);
  }

  private static void collectOutputs(final LithoNode node, final List<Attachable> collected) {

    // TODO(T143986616): optimise traversal for reused nodes

    for (int i = 0; i < node.getChildCount(); i++) {
      collectOutputs(node.getChildAt(i), collected);
    }

    final @Nullable List<Attachable> list = node.getAttachables();
    if (list != null) {
      collected.addAll(list);
    }
  }

  private static @Nullable LithoNode reconcile(
      final ResolveContext resolveContext,
      final ComponentContext c,
      final LithoNode node,
      final Component next,
      final @Nullable String nextKey) {
    final TreeState treeState = resolveContext.getTreeState();
    final Set<String> keys;
    if (treeState == null) {
      keys = Collections.emptySet();
    } else {
      keys = treeState.getKeysForPendingStateUpdates();
    }

    return reconcile(resolveContext, c, node, next, nextKey, keys, null);
  }

  /**
   * Internal method to <b>try</b> and reconcile the {@param current} LithoNode with a new {@link
   * ComponentContext} and an updated head {@link Component}.
   *
   * @param resolveContext The RenderStateContext.
   * @param parentContext The ComponentContext.
   * @param current The current LithoNode which should be updated.
   * @param next The updated component to be used to reconcile this LithoNode.
   * @param keys The keys of mutated components.
   * @return A new updated LithoNode.
   */
  private static @Nullable LithoNode reconcile(
      final ResolveContext resolveContext,
      final ComponentContext parentContext,
      final LithoNode current,
      final Component next,
      final @Nullable String nextKey,
      final Set<String> keys,
      final @Nullable LithoNode parent) {
    final int mode = getReconciliationMode(current, keys);
    final LithoNode layout;
    switch (mode) {
      case ReconciliationMode.REUSE:
        commitToLayoutStateRecursively(resolveContext, current);
        layout = current;
        break;
      case ReconciliationMode.RECONCILE:
        layout = reconcile(resolveContext, current, next, keys);
        break;
      case ReconciliationMode.RECREATE:
        layout =
            Resolver.resolveWithGlobalKey(
                resolveContext, parentContext, next, Preconditions.checkNotNull(nextKey));
        if (layout != null) {
          if (parent == null) {
            layout.applyParentDependentCommonProps(resolveContext);
          } else {
            layout.applyParentDependentCommonProps(
                resolveContext,
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
      final ResolveContext resolveContext,
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
    commitToLayoutState(resolveContext, current);

    ComponentContext parentContext = layout.getTailComponentContext();

    // 3. Iterate over children.
    int count = current.getChildCount();
    for (int i = 0; i < count; i++) {
      final LithoNode child = current.getChildAt(i);

      // 3.1 Get the head component of the child layout.
      int index = Math.max(0, child.getComponentCount() - 1);
      final Component component = child.getComponentAt(index);
      final String key = child.getGlobalKeyAt(index);

      // 3.2 Reconcile child layout.
      final LithoNode copy =
          reconcile(resolveContext, parentContext, child, component, key, keys, current);

      // 3.3 Add the child to the cloned yoga node
      layout.child(copy);
    }

    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    return layout;
  }

  public static void commitToLayoutStateRecursively(ResolveContext c, LithoNode node) {
    final int count = node.getChildCount();
    commitToLayoutState(c, node);
    for (int i = 0; i < count; i++) {
      commitToLayoutStateRecursively(c, node.getChildAt(i));
    }
  }

  public static void commitToLayoutState(ResolveContext c, LithoNode node) {
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
      final LithoNode current, final Set<String> mutatedKeys) {
    final List<ScopedComponentInfo> components = current.getScopedComponentInfos();

    // 1.0 check early exit conditions
    if (current instanceof NestedTreeHolder) {
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

  public static class Outputs {
    final List<Attachable> attachables;

    Outputs(List<Attachable> attachables) {
      this.attachables = attachables;
    }
  }
}
