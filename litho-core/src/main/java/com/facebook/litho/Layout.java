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
import static com.facebook.litho.Component.sMeasureFunction;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.LithoLayoutResult.NestedTreeHolderResult;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.rendercore.RenderState.LayoutContext;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaNode;
import java.util.List;

@Nullsafe(Nullsafe.Mode.LOCAL)
class Layout {

  static final boolean IS_TEST = "robolectric".equals(Build.FINGERPRINT);

  private static final String EVENT_START_CREATE_LAYOUT = "start_create_layout";
  private static final String EVENT_END_CREATE_LAYOUT = "end_create_layout";
  private static final String EVENT_START_RECONCILE = "start_reconcile_layout";
  private static final String EVENT_END_RECONCILE = "end_reconcile_layout";

  static LayoutResultHolder createAndMeasureComponent(
      final LayoutStateContext layoutStateContext,
      final ComponentContext c,
      final Component component,
      final int widthSpec,
      final int heightSpec) {
    return createAndMeasureComponent(
        layoutStateContext, c, component, null, widthSpec, heightSpec, null, null, null, null);
  }

  /* TODO: (T81557408) Fix @Nullable issue */
  static LayoutResultHolder createAndMeasureComponent(
      final LayoutStateContext layoutStateContext,
      final ComponentContext c,
      final Component component,
      final @Nullable String globalKeyToReuse,
      final int widthSpec,
      final int heightSpec,
      final @Nullable LithoLayoutResult current,
      final @Nullable LayoutStateContext prevLayoutStateContext,
      final @Nullable DiffNode diff,
      final @Nullable PerfEvent layoutStatePerfEvent) {

    if (layoutStatePerfEvent != null) {
      final String event = current == null ? EVENT_START_CREATE_LAYOUT : EVENT_START_RECONCILE;
      layoutStatePerfEvent.markerPoint(event);
    }

    c.setWidthSpec(widthSpec);
    c.setHeightSpec(heightSpec);

    final @Nullable InternalNode layout;
    if (current == null) {
      layout = create(layoutStateContext, c, component, true);

      // This needs to finish layout on the UI thread.
      if (layout != null
          && layoutStateContext != null
          && layoutStateContext.isLayoutInterrupted()) {
        if (layoutStatePerfEvent != null) {
          layoutStatePerfEvent.markerPoint(EVENT_END_CREATE_LAYOUT);
        }

        return LayoutResultHolder.interrupted(layout);
      } else {
        // Layout is complete, disable interruption from this point on.
        if (layoutStateContext != null) {
          layoutStateContext.markLayoutUninterruptible();
        }
      }
    } else {
      final ComponentContext updatedScopedContext =
          update(layoutStateContext, c, component, true, globalKeyToReuse);
      final Component updated = updatedScopedContext.getComponentScope();

      layout =
          current
              .getInternalNode()
              .reconcile(
                  layoutStateContext,
                  c,
                  updated,
                  updatedScopedContext.useStatelessComponent()
                      ? updatedScopedContext.getScopedComponentInfo()
                      : null,
                  globalKeyToReuse);
    }

    if (layoutStatePerfEvent != null) {
      final String event = current == null ? EVENT_END_CREATE_LAYOUT : EVENT_END_RECONCILE;
      layoutStatePerfEvent.markerPoint(event);
    }

    if (layoutStatePerfEvent != null) {
      layoutStatePerfEvent.markerPoint("start_measure");
    }

    LithoLayoutResult result =
        layout != null
            ? measure(
                layoutStateContext,
                c,
                layout,
                widthSpec,
                heightSpec,
                current,
                prevLayoutStateContext,
                diff)
            : null;

    if (layoutStatePerfEvent != null) {
      layoutStatePerfEvent.markerPoint("end_measure");
    }

    return new LayoutResultHolder(result);
  }

  public @Nullable static InternalNode create(
      final LayoutStateContext layoutStateContext,
      final ComponentContext parent,
      final Component component) {
    return create(layoutStateContext, parent, component, false, false, null);
  }

  static @Nullable InternalNode create(
      final LayoutStateContext layoutStateContext,
      final ComponentContext parent,
      final Component component,
      final boolean resolveNestedTree) {
    return create(layoutStateContext, parent, component, resolveNestedTree, false, null);
  }

  static @Nullable InternalNode create(
      final LayoutStateContext layoutStateContext,
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
    final @Nullable ScopedComponentInfo scopedComponentInfo;

    try {

      // 1. Consume the layout created in `willrender`.
      final InternalNode cached =
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

      scopedComponentInfo = c.useStatelessComponent() ? c.getScopedComponentInfo() : null;
      // 6. Resolve the component into an InternalNode tree.

      final boolean shouldDeferNestedTreeResolution =
          isNestedTree(layoutStateContext, component) && !resolveNestedTree;

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
        node = InternalNodeUtils.create(c).flexDirection(YogaFlexDirection.COLUMN);
      }

      // If the component is a LayoutSpec.
      else if (isLayoutSpec(component)) {

        final RenderResult renderResult = component.render(c);
        final Component root = renderResult.component;

        if (root != null) {
          // TODO: (T57741374) this step is required because of a bug in redex.
          if (root == component) {
            node = root.resolve(layoutStateContext, c);
          } else {
            node = create(layoutStateContext, c, root, false);
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
    if (node.getTailComponent() == null) {
      final boolean isMountSpecWithMeasure = component.canMeasure() && isMountSpec(component);
      if (isMountSpecWithMeasure
          || (isNestedTree(layoutStateContext, component) && !resolveNestedTree)) {
        node.setMeasureFunction(sMeasureFunction);
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
    node.appendComponent(component, globalKey, scopedComponentInfo);

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

  static @Nullable LithoLayoutResult create(
      final LayoutStateContext layoutStateContext,
      ComponentContext parentContext,
      final NestedTreeHolderResult holder,
      final int widthSpec,
      final int heightSpec,
      final @Nullable LayoutStateContext prevLayoutStateContext) {

    final InternalNode node = holder.getInternalNode();
    final Component component = node.getTailComponent();
    if (component == null) {
      throw new IllegalArgumentException("A component is required to resolve a nested tree.");
    }

    final String globalKey = Preconditions.checkNotNull(node.getTailComponentKey());
    final LithoLayoutResult currentLayout = holder.getNestedResult();

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
          consumeCachedLayout(
              layoutStateContext, parentContext, component, holder, widthSpec, heightSpec);

      if (cachedLayout != null) {

        // Use the cached layout.
        layout = cachedLayout;
      } else {

        // Check if previous layout can be remeasured and used.
        if (currentLayout != null
            && Preconditions.checkNotNull(currentLayout.getInternalNode().getHeadComponent())
                .canUsePreviousLayout(layoutStateContext, parentContext, globalKey)) {
          remeasure(
              layoutStateContext,
              currentLayout,
              widthSpec,
              heightSpec,
              currentLayout,
              prevLayoutStateContext);
          layout = currentLayout;
        } else {

          final int prevWidthSpec = parentContext.getWidthSpec();
          final int prevHeightSpec = parentContext.getHeightSpec();

          if (!parentContext.useStatelessComponent()) {
            parentContext.setTreeProps(holder.getInternalNode().getPendingTreeProps());
          }

          // Set the size specs in ComponentContext for the nested tree
          parentContext.setWidthSpec(widthSpec);
          parentContext.setHeightSpec(heightSpec);

          // Create a new layout.
          final @Nullable InternalNode newNode =
              create(layoutStateContext, parentContext, component, true, true, globalKey);

          if (parentContext.useStatelessComponent()) {
            parentContext.setWidthSpec(prevWidthSpec);
            parentContext.setHeightSpec(prevHeightSpec);
          }

          if (newNode != null) {
            holder.getInternalNode().copyInto(newNode);

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
                    null,
                    prevLayoutStateContext,
                    holder.getDiffNode());
          } else {
            layout = null;
          }
        }

        if (layout != null) {
          layout.setLastWidthSpec(widthSpec);
          layout.setLastHeightSpec(heightSpec);
          layout.setLastMeasuredHeight(layout.getHeight());
          layout.setLastMeasuredWidth(layout.getWidth());
        }
      }

      holder.setNestedResult(layout);
    } else {

      // Use the previous layout.
      layout = currentLayout;
    }

    if (layout != null) {
      // This is checking only nested tree roots however should be moved to check all the tree
      // roots.
      layout.getInternalNode().assertContextSpecificStyleNotSet();
    }

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
      final LayoutStateContext layoutStateContext,
      final ComponentContext parent,
      final Component original,
      final boolean reuseGlobalKey,
      @Nullable final String globalKeyToReuse) {

    final Component component =
        parent.useStatelessComponent() ? original : original.getThreadSafeInstance();

    if (reuseGlobalKey) {
      if (globalKeyToReuse == null) {
        throw new IllegalStateException("Cannot reuse a null global key");
      }
      component.setGlobalKey(globalKeyToReuse);
    }

    final TreeProps ancestor = parent.getTreeProps();

    // 1. Update the internal state of the component wrt the parent.
    // 2. Get the scoped context from the updated component.
    final ComponentContext c =
        component.updateInternalChildState(layoutStateContext, parent, globalKeyToReuse);

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
      final InternalNode root,
      final int widthSpec,
      final int heightSpec,
      final @Nullable LithoLayoutResult current,
      final @Nullable LayoutStateContext prevLayoutStateContext,
      final @Nullable DiffNode diff) {

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("measureTree:" + root.getSimpleName());
    }

    if (diff != null && root.implementsLayoutDiffing()) {
      ComponentsSystrace.beginSection("applyDiffNode");
      applyDiffNodeToUnchangedNodes(
          layoutStateContext,
          (LithoLayoutResult) root, // Only for DefaultInternalNode
          true,
          prevLayoutStateContext,
          diff);
      ComponentsSystrace.endSection(/* applyDiffNode */ );
    }

    final LayoutContext<LithoRenderContext> context =
        new LayoutContext<>(
            c.getAndroidContext(),
            new LithoRenderContext(layoutStateContext, current, diff),
            0,
            null,
            null);

    LithoLayoutResult result = root.calculateLayout(context, widthSpec, heightSpec);

    if (isTracing) {
      ComponentsSystrace.endSection(/* measureTree */ );
    }

    return result;
  }

  static @Nullable LithoLayoutResult resumeCreateAndMeasureComponent(
      final LayoutStateContext layoutStateContext,
      final ComponentContext c,
      final @Nullable InternalNode root,
      final int widthSpec,
      final int heightSpec,
      final @Nullable LayoutStateContext prevLayoutStateContext,
      final @Nullable DiffNode diff,
      final @Nullable PerfEvent logLayoutState) {
    if (root == null) {
      return null;
    }

    resume(layoutStateContext, root);

    if (logLayoutState != null) {
      logLayoutState.markerPoint("start_measure");
    }

    final LithoLayoutResult result =
        measure(
            layoutStateContext,
            c,
            root,
            widthSpec,
            heightSpec,
            null, // TODO(T94662963): Pass the current LayoutResult from LayoutState.
            prevLayoutStateContext,
            diff);

    if (logLayoutState != null) {
      logLayoutState.markerPoint("end_measure");
    }

    return result;
  }

  static void resume(final LayoutStateContext c, final InternalNode root) {
    final List<Component> unresolved = root.getUnresolvedComponents();

    if (unresolved != null) {
      final Component tailComponent = Preconditions.checkNotNull(root.getTailComponent());
      final String tailKey = Preconditions.checkNotNull(root.getTailComponentKey());
      final ComponentContext context = tailComponent.getScopedContext(c, tailKey);
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
  static void remeasure(
      final LayoutStateContext layoutStateContext,
      final @Nullable LithoLayoutResult layout,
      final int widthSpec,
      final int heightSpec,
      final @Nullable LithoLayoutResult current,
      final @Nullable LayoutStateContext prevLayoutStateContext) {
    if (layout == null) {
      return;
    }

    measure(
        layoutStateContext,
        layout.getContext(),
        layout.getInternalNode(),
        widthSpec,
        heightSpec,
        current,
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
   * @param result the root of the LayoutTree
   * @param diffNode the root of the diffTree
   */
  static void applyDiffNodeToUnchangedNodes(
      final LayoutStateContext layoutStateContext,
      final LithoLayoutResult result,
      final boolean isTreeRoot,
      final @Nullable LayoutStateContext prevLayoutStateContext,
      final @Nullable DiffNode diffNode) {

    final InternalNode layoutNode = result.getInternalNode();

    try {
      // Root of the main tree or of a nested tree.
      if (isLayoutSpecWithSizeSpec(layoutNode.getTailComponent()) && !isTreeRoot) {
        result.setDiffNode(diffNode);
        return;
      }

      if (diffNode == null || !hostIsCompatible(layoutNode, diffNode)) {
        return;
      }

      result.setDiffNode(diffNode);

      final int layoutCount = layoutNode.getChildCount();
      final int diffCount = diffNode.getChildCount();

      if (layoutCount != 0 && diffCount != 0) {
        for (int i = 0; i < layoutCount && i < diffCount; i++) {
          applyDiffNodeToUnchangedNodes(
              layoutStateContext,
              result.getChildAt(i),
              false,
              prevLayoutStateContext,
              diffNode.getChildAt(i));
        }

        // Apply the DiffNode to a leaf node (i.e. MountSpec) only if it should NOT update.
      } else if (!shouldComponentUpdate(
          layoutStateContext, layoutNode, prevLayoutStateContext, diffNode)) {
        applyDiffNodeToLayoutNode(layoutStateContext, result, prevLayoutStateContext, diffNode);
      }
    } catch (Throwable t) {
      final Component c = layoutNode.getTailComponent();
      if (c != null) {
        final ComponentContext ct =
            c.getScopedContext(
                layoutStateContext, Preconditions.checkNotNull(layoutNode.getTailComponentKey()));
        final LithoMetadataExceptionWrapper e = new LithoMetadataExceptionWrapper(ct, t);
        e.addComponentForLayoutStack(c);
        throw e;
      } else {
        throw t;
      }
    }
  }

  /**
   * Copies the inter stage state (if any) from the DiffNode's component to the layout node's
   * component, and declares that the cached measures on the diff node are valid for the layout
   * node.
   */
  private static void applyDiffNodeToLayoutNode(
      final LayoutStateContext nextLayoutStateContext,
      final LithoLayoutResult result,
      final @Nullable LayoutStateContext diffNodeLayoutStateContext,
      final DiffNode diffNode) {
    final InternalNode layoutNode = result.getInternalNode();
    final Component component = layoutNode.getTailComponent();
    final String componentKey = layoutNode.getTailComponentKey();
    if (component != null) {
      component.copyInterStageImpl(
          component.getInterStagePropsContainer(
              nextLayoutStateContext, Preconditions.checkNotNull(componentKey)),
          Preconditions.checkNotNull(diffNode.getComponent())
              .getInterStagePropsContainer(
                  Preconditions.checkNotNull(diffNodeLayoutStateContext),
                  Preconditions.checkNotNull(diffNode.getComponentGlobalKey())));
    }

    result.setCachedMeasuresValid(true);
  }

  @Nullable
  static LithoLayoutResult consumeCachedLayout(
      final LayoutStateContext layoutStateContext,
      final ComponentContext c,
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

    final LithoLayoutResult cachedLayout = layoutState.getCachedLayout(component);

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
  static boolean hostIsCompatible(final InternalNode node, final DiffNode diffNode) {
    return ComponentUtils.isSameComponentType(node.getTailComponent(), diffNode.getComponent());
  }

  static boolean shouldComponentUpdate(
      final LayoutStateContext layoutStateContext,
      final InternalNode layoutNode,
      final @Nullable LayoutStateContext prevLayoutStateContext,
      final @Nullable DiffNode diffNode) {
    if (diffNode == null) {
      return true;
    }

    final Component component = layoutNode.getTailComponent();

    if (component != null) {
      final String globalKey = Preconditions.checkNotNull(layoutNode.getTailComponentKey());
      final ComponentContext scopedContext =
          component.getScopedContext(layoutStateContext, globalKey);

      try {
        return component.shouldComponentUpdate(
            getDiffNodeScopedContext(layoutStateContext, prevLayoutStateContext, diffNode),
            diffNode.getComponent(),
            scopedContext,
            component);
      } catch (Exception e) {
        ComponentUtils.handleWithHierarchy(Preconditions.checkNotNull(scopedContext), component, e);
      }
    }

    return true;
  }

  /** DiffNode state should be retrieved from the committed LayoutState. */
  private static @Nullable ComponentContext getDiffNodeScopedContext(
      LayoutStateContext currentLayoutStateContext,
      final @Nullable LayoutStateContext prevLayoutStateContext,
      DiffNode diffNode) {
    final Component diffNodeComponent = diffNode.getComponent();
    if (diffNodeComponent == null) {
      return null;
    }

    final LayoutStateContext committedContext;
    if (currentLayoutStateContext.getComponentTree() != null
        && currentLayoutStateContext.getComponentTree().useStatelessComponent()) {
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
        committedContext, Preconditions.checkNotNull(diffNode.getComponentGlobalKey()));
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
