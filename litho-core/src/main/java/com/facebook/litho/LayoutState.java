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

import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static com.facebook.litho.Component.isLayoutSpec;
import static com.facebook.litho.Component.isMountSpec;
import static com.facebook.litho.Component.isMountable;
import static com.facebook.litho.Component.isPrimitive;
import static com.facebook.litho.ContextUtils.getValidActivityForContext;
import static com.facebook.litho.LithoRenderUnit.getRenderUnit;
import static com.facebook.litho.LithoRenderUnit.isMountableView;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.rendercore.MountState.ROOT_HOST_ID;

import android.graphics.Rect;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.LongSparseArray;
import androidx.core.util.Preconditions;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.litho.EndToEndTestingExtension.EndToEndTestingExtensionInput;
import com.facebook.litho.LithoViewAttributesExtension.ViewAttributesInput;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.rendercore.LayoutCache;
import com.facebook.rendercore.LayoutContext;
import com.facebook.rendercore.MeasureResult;
import com.facebook.rendercore.MountItemsPool;
import com.facebook.rendercore.MountState;
import com.facebook.rendercore.RenderTree;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.Systracer;
import com.facebook.rendercore.incrementalmount.ExcludeFromIncrementalMountBinder;
import com.facebook.rendercore.incrementalmount.IncrementalMountExtensionInput;
import com.facebook.rendercore.incrementalmount.IncrementalMountOutput;
import com.facebook.rendercore.incrementalmount.IncrementalMountRenderCoreExtension;
import com.facebook.rendercore.transitions.TransitionUtils;
import com.facebook.rendercore.transitions.TransitionsExtensionInput;
import com.facebook.rendercore.utils.MeasureSpecUtils;
import com.facebook.rendercore.visibility.VisibilityBoundsTransformer;
import com.facebook.rendercore.visibility.VisibilityExtensionInput;
import com.facebook.rendercore.visibility.VisibilityOutput;
import com.facebook.yoga.YogaEdge;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.CheckReturnValue;

/**
 * The main role of {@link LayoutState} is to hold the output of layout calculation. This includes
 * mountable outputs and visibility outputs. A centerpiece of the class is {@link
 * #collectResults(ComponentContext, LithoLayoutResult, LithoNode, LayoutState, LithoLayoutContext,
 * RenderTreeNode, DiffNode, DebugHierarchy.Node)} which prepares the before-mentioned outputs based
 * on the provided {@link LithoNode} for later use in {@link MountState}.
 *
 * <p>This needs to be accessible to statically mock the class in tests.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class LayoutState
    implements IncrementalMountExtensionInput,
        VisibilityExtensionInput,
        TransitionsExtensionInput,
        EndToEndTestingExtensionInput,
        PotentiallyPartialResult,
        ViewAttributesInput {

  private static final String DUPLICATE_TRANSITION_IDS = "LayoutState:DuplicateTransitionIds";
  @Nullable private Transition.RootBoundsTransition mRootWidthAnimation;
  @Nullable private Transition.RootBoundsTransition mRootHeightAnimation;

  public static boolean isFromSyncLayout(@RenderSource int source) {
    switch (source) {
      case RenderSource.MEASURE_SET_SIZE_SPEC:
      case RenderSource.SET_ROOT_SYNC:
      case RenderSource.UPDATE_STATE_SYNC:
      case RenderSource.SET_SIZE_SPEC_SYNC:
      case RenderSource.RELOAD_PREVIOUS_STATE:
        return true;
      default:
        return false;
    }
  }

  private static final AtomicInteger sIdGenerator = new AtomicInteger(1);
  private static final int NO_PREVIOUS_LAYOUT_STATE_ID = -1;

  private final Map<String, Rect> mComponentKeyToBounds = new HashMap<>();
  private final Map<Handle, Rect> mComponentHandleToBounds = new HashMap<>();
  private @Nullable List<ScopedComponentInfo> mScopedSpecComponentInfos;
  private @Nullable List<Pair<String, EventHandler<?>>> mCreatedEventHandlers;

  private final ComponentContext mContext;

  private final Component mComponent;

  private int mWidthSpec;
  private int mHeightSpec;

  private final List<RenderTreeNode> mMountableOutputs = new ArrayList<>(8);
  private List<VisibilityOutput> mVisibilityOutputs;
  private final LongSparseArray<Integer> mOutputsIdToPositionMap = new LongSparseArray<>(8);
  private final Map<Long, ViewAttributes> mRenderUnitsWithViewAttributes = new HashMap<>(8);
  private final Map<Long, IncrementalMountOutput> mIncrementalMountOutputs = new LinkedHashMap<>(8);
  private final ArrayList<IncrementalMountOutput> mMountableOutputTops = new ArrayList<>();
  private final ArrayList<IncrementalMountOutput> mMountableOutputBottoms = new ArrayList<>();
  private final LongSparseArray<AnimatableItem> mAnimatableItems = new LongSparseArray<>(8);
  private final Set<Long> mRenderUnitIdsWhichHostRenderTrees = new HashSet<>(4);
  private final Systracer mTracer = ComponentsSystrace.getSystrace();
  private final @Nullable List<TestOutput> mTestOutputs;

  @Nullable LithoNode mRoot;
  @Nullable LithoLayoutResult mLayoutResult;
  @Nullable TransitionId mRootTransitionId;
  @Nullable String mRootComponentName;
  @Nullable LayoutCache.CachedData mLayoutCacheData;

  private @Nullable DiffNode mDiffTreeRoot;

  private int mWidth;
  private int mHeight;

  private int mCurrentX;
  private int mCurrentY;

  // Holds the current host marker in the layout tree.
  private long mCurrentHostMarker = -1L;
  private int mCurrentHostOutputPosition = -1;

  private final boolean mShouldGenerateDiffTree;
  private int mComponentTreeId = -1;
  private final int mId;
  // Id of the layout state (if any) that was used in comparisons with this layout state.
  private final int mPreviousLayoutStateId;

  private final boolean mIsAccessibilityEnabled;

  private final TreeState mTreeState;
  private @Nullable List<ScopedComponentInfo> mScopedComponentInfosNeedingPreviousRenderData;
  private @Nullable TransitionId mCurrentTransitionId;
  private @Nullable OutputUnitsAffinityGroup<AnimatableItem> mCurrentLayoutOutputAffinityGroup;
  private final Map<TransitionId, OutputUnitsAffinityGroup<AnimatableItem>> mTransitionIdMapping =
      new LinkedHashMap<>();
  private final Set<TransitionId> mDuplicatedTransitionIds = new HashSet<>();
  private @Nullable List<Transition> mTransitions;
  private @Nullable RenderTree mCachedRenderTree = null;

  @Nullable WorkingRangeContainer mWorkingRangeContainer;

  @Nullable List<Attachable> mAttachables;

  // If there is any component marked with 'ExcludeFromIncrementalMountComponent'
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public boolean mHasComponentsExcludedFromIncrementalMount;

  // TODO(t66287929): Remove mIsCommitted from LayoutState by matching RenderState logic around
  // Futures.
  private boolean mIsCommitted;

  private boolean mShouldProcessVisibilityOutputs;

  LayoutState(
      ComponentContext context,
      Component rootComponent,
      TreeState treeState,
      @Nullable List<Attachable> attachables,
      @Nullable LayoutState current,
      @Nullable LithoNode root,
      int widthSpec,
      int heightSpec,
      int componentTreeId,
      boolean isLayoutDiffingEnabled,
      boolean isAccessibilityEnabled) {
    mContext = context;
    mComponent = rootComponent;
    mId = sIdGenerator.getAndIncrement();
    mPreviousLayoutStateId = current != null ? current.mId : NO_PREVIOUS_LAYOUT_STATE_ID;
    mLayoutCacheData = current != null ? current.mLayoutCacheData : null;
    mTestOutputs = ComponentsConfiguration.isEndToEndTestRun ? new ArrayList<TestOutput>(8) : null;
    mScopedSpecComponentInfos = new ArrayList<>();
    mVisibilityOutputs = new ArrayList<>(8);

    mTreeState = treeState;
    mAttachables = attachables != null ? new ArrayList<>(attachables) : null;
    mWidthSpec = widthSpec;
    mHeightSpec = heightSpec;
    mComponentTreeId = componentTreeId;
    mRootComponentName = rootComponent.getSimpleName();
    mShouldGenerateDiffTree = isLayoutDiffingEnabled;
    mRoot = root;
    mRootTransitionId = getTransitionIdForNode(root);
    mIsAccessibilityEnabled = isAccessibilityEnabled;
  }

  @VisibleForTesting
  Component getRootComponent() {
    return mComponent;
  }

  @Override
  public boolean isPartialResult() {
    return false;
  }

  /**
   * Acquires a new layout output for the internal node and its associated component. It returns
   * null if there's no component associated with the node as the mount pass only cares about nodes
   * that will potentially mount content into the component host.
   */
  @Nullable
  private static RenderTreeNode createContentRenderTreeNode(
      LithoLayoutResult result,
      LithoNode node,
      LayoutState layoutState,
      @Nullable RenderTreeNode parent) {

    if (isLayoutSpec(node.getTailComponent()) || result.measureHadExceptions()) {
      // back out when dealing with Layout Specs or if there was an error during measure
      return null;
    }

    final @Nullable MeasureResult measure;
    final boolean hasExactSize = !result.wasMeasured();
    if (!node.getTailComponentContext().shouldCacheLayouts()) {
      if ((isMountable(node.getTailComponent()) || isPrimitive(node.getTailComponent()))
          && hasExactSize) {
        final int width =
            result.getWidth()
                - result.getPaddingRight()
                - result.getPaddingLeft()
                - result.getLayoutBorder(YogaEdge.RIGHT)
                - result.getLayoutBorder(YogaEdge.LEFT);
        final int height =
            result.getHeight()
                - result.getPaddingTop()
                - result.getPaddingBottom()
                - result.getLayoutBorder(YogaEdge.TOP)
                - result.getLayoutBorder(YogaEdge.BOTTOM);

        final LayoutContext layoutContext =
            LithoLayoutResult.getLayoutContextFromYogaNode(result.getYogaNode());
        measure =
            result.measure(
                layoutContext, MeasureSpecUtils.exactly(width), MeasureSpecUtils.exactly(height));
      } else {
        measure = null;
      }

      if (measure != null && measure.mHadExceptions) {
        return null;
      }
    }

    final @Nullable LithoRenderUnit unit = result.getContentRenderUnit();
    if (unit == null) {
      return null;
    }

    final @Nullable Object layoutData = result.getLayoutData();

    return createRenderTreeNode(unit, layoutState, result, true, layoutData, parent, hasExactSize);
  }

  private static void addRootHostRenderTreeNode(
      final LayoutState layoutState,
      final @Nullable LithoLayoutResult result,
      final @Nullable DebugHierarchy.Node hierarchy) {
    final int width = result != null ? result.getWidth() : 0;
    final int height = result != null ? result.getHeight() : 0;

    final LithoRenderUnit unit =
        MountSpecLithoRenderUnit.create(
            ROOT_HOST_ID,
            HostComponent.create(),
            null,
            null,
            null,
            0,
            IMPORTANT_FOR_ACCESSIBILITY_AUTO,
            MountSpecLithoRenderUnit.STATE_DIRTY,
            LithoNodeUtils.getDebugKey(
                layoutState.getComponentContext().mGlobalKey, OutputUnitType.HOST));

    final RenderTreeNode node =
        RenderTreeNodeUtils.create(
            unit,
            new Rect(0, 0, width, height),
            new LithoLayoutData(
                width, height, layoutState.mId, layoutState.mPreviousLayoutStateId, null, null),
            null);

    if (hierarchy != null) {
      unit.setHierarchy(hierarchy.mutateType(OutputUnitType.HOST));
    }

    addRenderTreeNode(layoutState, node, result, unit, OutputUnitType.HOST, null, null);
  }

  private static RenderTreeNode createHostRenderTreeNode(
      final LithoRenderUnit unit,
      LayoutState layoutState,
      LithoLayoutResult result,
      @Nullable RenderTreeNode parent,
      @Nullable DebugHierarchy.Node hierarchy) {

    final RenderTreeNode renderTreeNode =
        createRenderTreeNode(unit, layoutState, result, false, null, parent, false);

    if (hierarchy != null) {
      unit.setHierarchy(hierarchy.mutateType(OutputUnitType.HOST));
    }

    return renderTreeNode;
  }

  private static RenderTreeNode createRenderTreeNode(
      final LithoRenderUnit unit,
      final LayoutState layoutState,
      final LithoLayoutResult result,
      final boolean useNodePadding,
      final @Nullable Object layoutData,
      final @Nullable RenderTreeNode parent,
      final boolean hasExactSize) {

    final int hostTranslationX;
    final int hostTranslationY;
    if (parent != null) {
      hostTranslationX = parent.getAbsoluteX();
      hostTranslationY = parent.getAbsoluteY();
    } else {
      hostTranslationX = 0;
      hostTranslationY = 0;
    }

    int l = layoutState.mCurrentX - hostTranslationX + result.getX();
    int t = layoutState.mCurrentY - hostTranslationY + result.getY();
    int r = l + result.getWidth();
    int b = t + result.getHeight();

    if (useNodePadding) {
      if (isMountable(unit.getComponent()) || isPrimitive(unit.getComponent())) {
        if (!isMountableView(unit)) {
          if (!hasExactSize) {
            l += result.getPaddingLeft() + result.getLayoutBorder(YogaEdge.LEFT);
            t += result.getPaddingTop() + result.getLayoutBorder(YogaEdge.TOP);
            r -= (result.getPaddingRight() + result.getLayoutBorder(YogaEdge.RIGHT));
            b -= (result.getPaddingBottom() + result.getLayoutBorder(YogaEdge.BOTTOM));
          } else {
            // for exact size the border doesn't need to be adjusted since it's inside the bounds of
            // the content
            l += result.getPaddingLeft();
            t += result.getPaddingTop();
            r -= result.getPaddingRight();
            b -= result.getPaddingBottom();
          }
        }
      } else if (!isMountableView(unit)) {
        l += result.getPaddingLeft();
        t += result.getPaddingTop();
        r -= result.getPaddingRight();
        b -= result.getPaddingBottom();
      }
    }

    final Rect bounds = new Rect(l, t, r, b);

    return RenderTreeNodeUtils.create(
        unit,
        bounds,
        new LithoLayoutData(
            bounds.width(),
            bounds.height(),
            layoutState.mId,
            layoutState.mPreviousLayoutStateId,
            result.getExpandedTouchBounds(),
            layoutData),
        parent);
  }

  static AnimatableItem createAnimatableItem(
      final LithoRenderUnit unit,
      final Rect absoluteBounds,
      final @OutputUnitType int outputType,
      final @Nullable TransitionId transitionId) {
    return new LithoAnimtableItem(
        unit.getId(), absoluteBounds, outputType, unit.getNodeInfo(), transitionId);
  }

  /**
   * Acquires a {@link VisibilityOutput} object and computes the bounds for it using the information
   * stored in the {@link LithoNode}.
   */
  private static VisibilityOutput createVisibilityOutput(
      final LithoLayoutResult result,
      final LithoNode node,
      final LayoutState layoutState,
      final @Nullable RenderTreeNode renderTreeNode) {

    final int l = layoutState.mCurrentX + result.getX();
    final int t = layoutState.mCurrentY + result.getY();
    final int r = l + result.getWidth();
    final int b = t + result.getHeight();

    final EventHandler<VisibleEvent> visibleHandler = node.getVisibleHandler();
    final EventHandler<FocusedVisibleEvent> focusedHandler = node.getFocusedHandler();
    final EventHandler<UnfocusedVisibleEvent> unfocusedHandler = node.getUnfocusedHandler();
    final EventHandler<FullImpressionVisibleEvent> fullImpressionHandler =
        node.getFullImpressionHandler();
    final EventHandler<InvisibleEvent> invisibleHandler = node.getInvisibleHandler();
    final EventHandler<VisibilityChangedEvent> visibleRectChangedEventHandler =
        node.getVisibilityChangedHandler();
    final Component component = node.getTailComponent();
    final String componentGlobalKey = node.getTailComponentKey();

    return new VisibilityOutput(
        componentGlobalKey,
        component != null ? component.getSimpleName() : "Unknown",
        new Rect(l, t, r, b),
        renderTreeNode != null,
        renderTreeNode != null ? renderTreeNode.getRenderUnit().getId() : 0,
        node.getVisibleHeightRatio(),
        node.getVisibleWidthRatio(),
        visibleHandler,
        invisibleHandler,
        focusedHandler,
        unfocusedHandler,
        fullImpressionHandler,
        visibleRectChangedEventHandler);
  }

  private static TestOutput createTestOutput(
      final LithoLayoutResult result,
      final LithoNode node,
      final LayoutState layoutState,
      final @Nullable LithoRenderUnit renderUnit) {
    final int l = layoutState.mCurrentX + result.getX();
    final int t = layoutState.mCurrentY + result.getY();
    final int r = l + result.getWidth();
    final int b = t + result.getHeight();

    final TestOutput output = new TestOutput();
    output.setTestKey(Preconditions.checkNotNull(node.getTestKey()));
    output.setBounds(l, t, r, b);
    output.setHostMarker(layoutState.mCurrentHostMarker);
    if (renderUnit != null) {
      output.setLayoutOutputId(renderUnit.getId());
    }

    return output;
  }

  @Nullable
  private static DebugHierarchy.Node getDebugHierarchy(
      @Nullable DebugHierarchy.Node parentHierarchy, final LithoNode node) {
    if (!ComponentsConfiguration.isDebugHierarchyEnabled) {
      return null;
    }
    List<ScopedComponentInfo> infos = node.getScopedComponentInfos();
    List<Component> components = new ArrayList<>(infos.size());
    for (ScopedComponentInfo info : infos) {
      components.add(info.getComponent());
    }
    return DebugHierarchy.newNode(parentHierarchy, node.getTailComponent(), components);
  }

  /**
   * Collects layout outputs and release the layout tree. The layout outputs hold necessary
   * information to be used by {@link MountState} to mount components into a {@link ComponentHost}.
   *
   * <p>Whenever a component has view content (view tags, click handler, etc), a new host 'marker'
   * is added for it. The mount pass will use the markers to decide which host should be used for
   * each layout output. The root node unconditionally generates a layout output corresponding to
   * the root host.
   *
   * <p>The order of layout outputs follows a depth-first traversal in the tree to ensure the hosts
   * will be created at the right order when mounting. The host markers will be define which host
   * each mounted artifacts will be attached to.
   *
   * <p>At this stage all the {@link LithoNode} for which we have LayoutOutputs that can be recycled
   * will have a DiffNode associated. If the CachedMeasures are valid we'll try to recycle both the
   * host and the contents (including background/foreground). In all other cases instead we'll only
   * try to re-use the hosts. In some cases the host's structure might change between two updates
   * even if the component is of the same type. This can happen for example when a click listener is
   * added. To avoid trying to re-use the wrong host type we explicitly check that after all the
   * children for a subtree have been added (this is when the actual host type is resolved). If the
   * host type changed compared to the one in the DiffNode we need to refresh the ids for the whole
   * subtree in order to ensure that the MountState will unmount the subtree and mount it again on
   * the correct host.
   *
   * <p>
   *
   * @param parentContext the parent component context
   * @param result InternalNode to process.
   * @param layoutState the LayoutState currently operating.
   * @param parent
   * @param parentDiffNode whether this method also populates the diff tree and assigns the root
   * @param parentHierarchy The parent hierarchy linked list or null.
   */
  private static void collectResults(
      final ComponentContext parentContext,
      final LithoLayoutResult result,
      final LithoNode node,
      final LayoutState layoutState,
      final LithoLayoutContext lithoLayoutContext,
      @Nullable RenderTreeNode parent,
      final @Nullable DiffNode parentDiffNode,
      final @Nullable DebugHierarchy.Node parentHierarchy) {
    if (lithoLayoutContext.isFutureReleased() || result.measureHadExceptions()) {
      // Exit early if the layout future as been released or if this result had exceptions.
      return;
    }

    final Component component = node.getTailComponent();
    final boolean isTracing = ComponentsSystrace.isTracing();

    final DebugHierarchy.Node hierarchy = getDebugHierarchy(parentHierarchy, node);

    if (result instanceof NestedTreeHolderResult) {

      final LithoLayoutResult nestedTree;
      final int size = node.getComponentCount();
      final ComponentContext immediateParentContext;
      if (size == 1) {
        immediateParentContext = parentContext;
      } else {
        immediateParentContext = node.getComponentContextAt(1);
      }

      if (parentContext.shouldCacheLayouts()) {
        nestedTree = ((NestedTreeHolderResult) result).getNestedResult();
      } else {
        // If the nested tree is defined, it has been resolved during a measure call during
        // layout calculation.
        if (isTracing) {
          ComponentsSystrace.beginSectionWithArgs("resolveNestedTree:" + component.getSimpleName())
              .arg("widthSpec", "EXACTLY " + result.getWidth())
              .arg("heightSpec", "EXACTLY " + result.getHeight())
              .arg("rootComponentId", node.getTailComponent().getId())
              .flush();
        }

        nestedTree =
            Layout.measure(
                lithoLayoutContext,
                Preconditions.checkNotNull(immediateParentContext),
                (NestedTreeHolderResult) result,
                SizeSpec.makeSizeSpec(result.getWidth(), EXACTLY),
                SizeSpec.makeSizeSpec(result.getHeight(), EXACTLY));

        if (isTracing) {
          ComponentsSystrace.endSection();
        }
      }

      if (nestedTree == null) {
        return;
      }

      if (!parentContext.shouldCacheLayouts()) {
        final @Nullable Resolver.Outputs outputs = Resolver.collectOutputs(nestedTree.mNode);
        if (outputs != null) {
          if (layoutState.mAttachables == null) {
            layoutState.mAttachables = new ArrayList<>(outputs.attachables.size());
          }
          layoutState.mAttachables.addAll(outputs.attachables);
        }
      }

      // Account for position of the holder node.
      layoutState.mCurrentX += result.getX();
      layoutState.mCurrentY += result.getY();

      collectResults(
          immediateParentContext,
          nestedTree,
          nestedTree.getNode(),
          layoutState,
          lithoLayoutContext,
          parent,
          parentDiffNode,
          hierarchy);

      if (parentContext.shouldCacheLayouts()
          && !parentContext.getComponentsConfiguration().shouldCacheNestedLayouts()) {
        ((NestedTreeHolderResult) result).setNestedResult(null);
      }

      layoutState.mCurrentX -= result.getX();
      layoutState.mCurrentY -= result.getY();

      return;
    }

    final ScopedComponentInfo tail = node.getTailScopedComponentInfo();
    final ComponentContext context = tail.getContext();
    final boolean shouldGenerateDiffTree = layoutState.mShouldGenerateDiffTree;
    final DiffNode diffNode;

    if (shouldGenerateDiffTree) {
      diffNode = createDiffNode(tail, parentDiffNode);
      if (parentDiffNode == null) {
        layoutState.mDiffTreeRoot = diffNode;
      }
    } else {
      diffNode = null;
    }

    final @Nullable LithoRenderUnit hostRenderUnit;
    if (parent == null /* isRoot */) {
      hostRenderUnit = LithoNodeUtils.createRootHostRenderUnit(result.getNode());
    } else {
      hostRenderUnit = result.getHostRenderUnit();
    }

    final boolean needsHostView = hostRenderUnit != null;

    final long currentHostMarker = layoutState.mCurrentHostMarker;
    final int currentHostOutputPosition = layoutState.mCurrentHostOutputPosition;

    final TransitionId currentTransitionId = layoutState.mCurrentTransitionId;
    final OutputUnitsAffinityGroup<AnimatableItem> currentLayoutOutputAffinityGroup =
        layoutState.mCurrentLayoutOutputAffinityGroup;

    layoutState.mCurrentTransitionId = getTransitionIdForNode(node);
    layoutState.mCurrentLayoutOutputAffinityGroup =
        layoutState.mCurrentTransitionId != null
            ? new OutputUnitsAffinityGroup<AnimatableItem>()
            : null;

    // 1. Insert a host LayoutOutput if we have some interactive content to be attached to.
    if (hostRenderUnit != null) {
      final int hostLayoutPosition =
          addHostRenderTreeNode(
              hostRenderUnit, parent, result, node, layoutState, diffNode, hierarchy);
      addCurrentAffinityGroupToTransitionMapping(layoutState);

      parent = layoutState.mMountableOutputs.get(hostLayoutPosition);

      layoutState.mCurrentHostMarker = parent.getRenderUnit().getId();
      layoutState.mCurrentHostOutputPosition = hostLayoutPosition;
    }

    // 2. Add background if defined.
    if (!context.mLithoConfiguration.mComponentsConfiguration.isShouldDisableBgFgOutputs()) {
      final LithoRenderUnit backgroundRenderUnit = result.getBackgroundRenderUnit();
      if (backgroundRenderUnit != null) {
        final RenderTreeNode backgroundRenderTreeNode =
            addDrawableRenderTreeNode(
                backgroundRenderUnit,
                parent,
                result,
                layoutState,
                hierarchy,
                OutputUnitType.BACKGROUND,
                needsHostView);

        if (diffNode != null) {
          diffNode.setBackgroundOutput((LithoRenderUnit) backgroundRenderTreeNode.getRenderUnit());
        }
      }
    }

    // Generate the RenderTreeNode for the given node.
    final @Nullable RenderTreeNode contentRenderTreeNode =
        createContentRenderTreeNode(result, node, layoutState, parent);

    // 3. Now add the MountSpec (either View or Drawable) to the outputs.
    if (contentRenderTreeNode != null) {
      final LithoRenderUnit contentRenderUnit =
          (LithoRenderUnit) contentRenderTreeNode.getRenderUnit();

      if (!context.shouldCacheLayouts()) {
        final LithoLayoutData layoutData =
            (LithoLayoutData) Preconditions.checkNotNull(contentRenderTreeNode.getLayoutData());

        // Notify component about its final size.
        if (isTracing) {
          ComponentsSystrace.beginSection("onBoundsDefined:" + component.getSimpleName());
        }

        try {
          if (isMountSpec(component) && component instanceof SpecGeneratedComponent) {
            ((SpecGeneratedComponent) component)
                .onBoundsDefined(context, result, (InterStagePropsContainer) layoutData.layoutData);
          }
        } catch (Exception e) {
          ComponentUtils.handleWithHierarchy(context, component, e);
          return;
        } finally {
          if (isTracing) {
            ComponentsSystrace.endSection();
          }
        }
      }

      addRenderTreeNode(
          layoutState,
          contentRenderTreeNode,
          result,
          contentRenderUnit,
          OutputUnitType.CONTENT,
          !needsHostView ? layoutState.mCurrentTransitionId : null,
          parent);

      if (diffNode != null) {
        diffNode.setContentOutput(contentRenderUnit);
      }

      if (hierarchy != null) {
        contentRenderUnit.setHierarchy(hierarchy.mutateType(OutputUnitType.CONTENT));
      }
    }

    // Set the measurements, and the layout data on the diff node
    if (diffNode != null) {
      diffNode.setLastWidthSpec(result.getWidthSpec());
      diffNode.setLastHeightSpec(result.getHeightSpec());
      diffNode.setLastMeasuredWidth(result.getContentWidth());
      diffNode.setLastMeasuredHeight(result.getContentHeight());
      diffNode.setLayoutData(result.getLayoutData());
      diffNode.setMountable(result.getNode().getMountable());
      diffNode.setPrimitive(result.getNode().getPrimitive());
      diffNode.setDelegate(result.getDelegate());
    }

    // 4. Extract the Transitions.
    if (context.areTransitionsEnabled()) {
      final ArrayList<Transition> transitions = node.getTransitions();
      if (transitions != null) {
        for (int i = 0, size = transitions.size(); i < size; i++) {
          final Transition transition = transitions.get(i);
          if (layoutState.mTransitions == null) {
            layoutState.mTransitions = new ArrayList<>();
          }
          TransitionUtils.addTransitions(transition, layoutState.mTransitions);
        }
      }

      final @Nullable Map<String, ScopedComponentInfo>
          scopedComponentInfosNeedingPreviousRenderData =
              node.getScopedComponentInfosNeedingPreviousRenderData();

      if (scopedComponentInfosNeedingPreviousRenderData != null) {

        if (layoutState.mScopedComponentInfosNeedingPreviousRenderData == null) {
          layoutState.mScopedComponentInfosNeedingPreviousRenderData = new ArrayList<>();
        }

        for (Map.Entry<String, ScopedComponentInfo> entry :
            scopedComponentInfosNeedingPreviousRenderData.entrySet()) {
          layoutState.mScopedComponentInfosNeedingPreviousRenderData.add(entry.getValue());
        }
      }
    }

    layoutState.mCurrentX += result.getX();
    layoutState.mCurrentY += result.getY();

    // We must process the nodes in order so that the layout state output order is correct.
    for (int i = 0, size = result.getChildCount(); i < size; i++) {
      final LithoLayoutResult child = result.getChildAt(i);
      collectResults(
          context,
          child,
          child.getNode(),
          layoutState,
          lithoLayoutContext,
          parent,
          diffNode,
          hierarchy);
    }

    layoutState.mCurrentX -= result.getX();
    layoutState.mCurrentY -= result.getY();

    // 5. Add border color if defined.
    final LithoRenderUnit borderRenderUnit = result.getBorderRenderUnit();
    if (borderRenderUnit != null) {
      final RenderTreeNode borderRenderTreeNode =
          addDrawableRenderTreeNode(
              borderRenderUnit,
              parent,
              result,
              layoutState,
              hierarchy,
              OutputUnitType.BORDER,
              needsHostView);

      if (diffNode != null) {
        diffNode.setBorderOutput((LithoRenderUnit) borderRenderTreeNode.getRenderUnit());
      }
    }

    // 6. Add foreground if defined.
    if (!context.mLithoConfiguration.mComponentsConfiguration.isShouldDisableBgFgOutputs()) {
      final @Nullable LithoRenderUnit foregroundRenderUnit = result.getForegroundRenderUnit();
      if (foregroundRenderUnit != null) {
        final RenderTreeNode foregroundRenderTreeNode =
            addDrawableRenderTreeNode(
                foregroundRenderUnit,
                parent,
                result,
                layoutState,
                hierarchy,
                OutputUnitType.FOREGROUND,
                needsHostView);

        if (diffNode != null) {
          diffNode.setForegroundOutput((LithoRenderUnit) foregroundRenderTreeNode.getRenderUnit());
        }
      }
    }

    // 7. Add VisibilityOutputs if any visibility-related event handlers are present.
    if (node.hasVisibilityHandlers()) {
      final VisibilityOutput visibilityOutput =
          createVisibilityOutput(
              result,
              node,
              layoutState,
              contentRenderTreeNode != null
                  ? contentRenderTreeNode
                  : needsHostView ? parent : null);

      layoutState.mVisibilityOutputs.add(visibilityOutput);

      if (diffNode != null) {
        diffNode.setVisibilityOutput(visibilityOutput);
      }
    }

    // 8. If we're in a testing environment, maintain an additional data structure with
    // information about nodes that we can query later.
    if (layoutState.mTestOutputs != null && !TextUtils.isEmpty(node.getTestKey())) {
      final TestOutput testOutput =
          createTestOutput(
              result,
              node,
              layoutState,
              contentRenderTreeNode != null
                  ? (LithoRenderUnit) contentRenderTreeNode.getRenderUnit()
                  : null);
      layoutState.mTestOutputs.add(testOutput);
    }

    // 9. Extract the Working Range registrations.
    List<WorkingRangeContainer.Registration> registrations = node.getWorkingRangeRegistrations();
    if (CollectionsUtils.isNotNullOrEmpty(registrations)) {
      if (layoutState.mWorkingRangeContainer == null) {
        layoutState.mWorkingRangeContainer = new WorkingRangeContainer();
      }

      for (WorkingRangeContainer.Registration registration : registrations) {
        if (component instanceof SpecGeneratedComponent) {
          layoutState.mWorkingRangeContainer.registerWorkingRange(
              registration.name,
              registration.workingRange,
              registration.scopedComponentInfo,
              (InterStagePropsContainer) result.getLayoutData());
        } else {
          layoutState.mWorkingRangeContainer.registerWorkingRange(
              registration.name, registration.workingRange, registration.scopedComponentInfo, null);
        }
      }
    }

    final Rect rect;
    if (contentRenderTreeNode != null) {
      rect = contentRenderTreeNode.getAbsoluteBounds(new Rect());
    } else {
      rect = new Rect();
      rect.left = layoutState.mCurrentX + result.getX();
      rect.top = layoutState.mCurrentY + result.getY();
      rect.right = rect.left + result.getWidth();
      rect.bottom = rect.top + result.getHeight();
    }

    for (int i = 0, size = node.getComponentCount(); i < size; i++) {
      final Component delegate = node.getComponentAt(i);
      final String delegateKey = node.getGlobalKeyAt(i);
      // Keep a list of the components we created during this layout calculation. If the layout is
      // valid, the ComponentTree will update the event handlers that have been created in the
      // previous ComponentTree with the new component dispatched, otherwise Section children
      // might not be accessing the correct props and state on the event handlers. The null
      // checkers cover tests, the scope and tree should not be null at this point of the layout
      // calculation.
      final ComponentContext delegateScopedContext = node.getComponentContextAt(i);
      if (delegateScopedContext != null) {
        if (layoutState.mScopedSpecComponentInfos != null
            && delegate instanceof SpecGeneratedComponent) {
          layoutState.mScopedSpecComponentInfos.add(delegateScopedContext.getScopedComponentInfo());
        }
      }
      if (delegateKey != null || delegate.hasHandle()) {
        Rect copyRect = new Rect(rect);
        if (delegateKey != null) {
          layoutState.mComponentKeyToBounds.put(delegateKey, copyRect);
        }
        if (delegate.hasHandle()) {
          layoutState.mComponentHandleToBounds.put(delegate.getHandle(), copyRect);
        }
      }
    }

    // All children for the given host have been added, restore the previous
    // host, level, and duplicate parent state value in the recursive queue.
    if (layoutState.mCurrentHostMarker != currentHostMarker) {
      layoutState.mCurrentHostMarker = currentHostMarker;
      layoutState.mCurrentHostOutputPosition = currentHostOutputPosition;
    }

    addCurrentAffinityGroupToTransitionMapping(layoutState);
    layoutState.mCurrentTransitionId = currentTransitionId;
    layoutState.mCurrentLayoutOutputAffinityGroup = currentLayoutOutputAffinityGroup;
  }

  Map<String, Rect> getComponentKeyToBounds() {
    return mComponentKeyToBounds;
  }

  Map<Handle, Rect> getComponentHandleToBounds() {
    return mComponentHandleToBounds;
  }

  Set<Handle> getComponentHandles() {
    return mComponentHandleToBounds.keySet();
  }

  @Nullable
  List<ScopedComponentInfo> consumeScopedSpecComponentInfos() {
    final List<ScopedComponentInfo> scopedSpecComponentInfos = mScopedSpecComponentInfos;
    mScopedSpecComponentInfos = null;

    return scopedSpecComponentInfos;
  }

  void setCreatedEventHandlers(@Nullable List<Pair<String, EventHandler<?>>> createdEventHandlers) {
    mCreatedEventHandlers = createdEventHandlers;
  }

  @Nullable
  List<Pair<String, EventHandler<?>>> consumeCreatedEventHandlers() {
    final List<Pair<String, EventHandler<?>>> createdEventHandlers = mCreatedEventHandlers;
    mCreatedEventHandlers = null;

    return createdEventHandlers;
  }

  @Nullable
  List<Attachable> getAttachables() {
    return mAttachables;
  }

  /** @return true, means there are components marked as 'ExcludeFromIncrementalMount'. */
  boolean hasComponentsExcludedFromIncrementalMount() {
    return mHasComponentsExcludedFromIncrementalMount;
  }

  private static RenderTreeNode addDrawableRenderTreeNode(
      LithoRenderUnit unit,
      final @Nullable RenderTreeNode parent,
      LithoLayoutResult result,
      LayoutState layoutState,
      @Nullable DebugHierarchy.Node hierarchy,
      @OutputUnitType int type,
      boolean matchHostBoundsTransitions) {

    final RenderTreeNode renderTreeNode =
        createRenderTreeNode(unit, layoutState, result, false, null, parent, false);

    final LithoRenderUnit drawableRenderUnit = (LithoRenderUnit) renderTreeNode.getRenderUnit();

    addRenderTreeNode(
        layoutState,
        renderTreeNode,
        result,
        drawableRenderUnit,
        type,
        !matchHostBoundsTransitions ? layoutState.mCurrentTransitionId : null,
        parent);

    if (hierarchy != null) {
      drawableRenderUnit.setHierarchy(hierarchy.mutateType(type));
    }

    return renderTreeNode;
  }

  private static void addLayoutOutputIdToPositionsMap(
      final LongSparseArray<Integer> outputsIdToPositionMap,
      final LithoRenderUnit unit,
      final int position) {
    outputsIdToPositionMap.put(unit.getId(), position);
  }

  private static void maybeAddLayoutOutputToAffinityGroup(
      @Nullable OutputUnitsAffinityGroup<AnimatableItem> group,
      @OutputUnitType int outputType,
      AnimatableItem animatableItem) {
    if (group != null) {
      group.add(outputType, animatableItem);
    }
  }

  private static void addCurrentAffinityGroupToTransitionMapping(LayoutState layoutState) {
    final OutputUnitsAffinityGroup<AnimatableItem> group =
        layoutState.mCurrentLayoutOutputAffinityGroup;
    if (group == null || group.isEmpty()) {
      return;
    }

    final TransitionId transitionId = layoutState.mCurrentTransitionId;
    if (transitionId == null) {
      return;
    }

    if (transitionId.mType == TransitionId.Type.AUTOGENERATED) {
      // Check if the duplications of this key has been found before, if so, just ignore it
      if (!layoutState.mDuplicatedTransitionIds.contains(transitionId)) {
        if (layoutState.mTransitionIdMapping.put(transitionId, group) != null) {
          // Already seen component with the same generated transition key, remove it from the
          // mapping and ignore in the future
          layoutState.mTransitionIdMapping.remove(transitionId);
          layoutState.mDuplicatedTransitionIds.add(transitionId);
        }
      }
    } else {
      if (layoutState.mTransitionIdMapping.put(transitionId, group) != null) {
        // Already seen component with the same manually set transition key
        ComponentsReporter.emitMessage(
            ComponentsReporter.LogLevel.FATAL,
            DUPLICATE_TRANSITION_IDS,
            "The transitionId '"
                + transitionId
                + "' is defined multiple times in the same layout. TransitionIDs must be unique.\n"
                + "Tree:\n"
                + ComponentUtils.treeToString(layoutState.mRoot));
      }
    }

    layoutState.mCurrentLayoutOutputAffinityGroup = null;
    layoutState.mCurrentTransitionId = null;
  }

  /**
   * If we have an interactive LayoutSpec or a MountSpec Drawable, we need to insert an
   * HostComponent in the Outputs such as it will be used as a HostView at Mount time. View
   * MountSpec are not allowed.
   *
   * @return The position the HostLayoutOutput was inserted.
   */
  private static int addHostRenderTreeNode(
      final LithoRenderUnit hostRenderUnit,
      final @Nullable RenderTreeNode parent,
      LithoLayoutResult result,
      LithoNode node,
      LayoutState layoutState,
      @Nullable DiffNode diffNode,
      @Nullable DebugHierarchy.Node hierarchy) {

    // Only the root host is allowed to wrap view mount specs as a layout output
    // is unconditionally added for it.
    if (node.willMountView() && !layoutState.isLayoutRoot(result)) {
      throw new IllegalArgumentException("We shouldn't insert a host as a parent of a View");
    }

    final RenderTreeNode hostRenderTreeNode =
        createHostRenderTreeNode(hostRenderUnit, layoutState, result, parent, hierarchy);

    if (diffNode != null) {
      diffNode.setHostOutput(hostRenderUnit);
    }

    // The component of the hostLayoutOutput will be set later after all the
    // children got processed.
    addRenderTreeNode(
        layoutState,
        hostRenderTreeNode,
        result,
        hostRenderUnit,
        OutputUnitType.HOST,
        layoutState.mCurrentTransitionId,
        parent);

    return layoutState.mMountableOutputs.size() - 1;
  }

  RenderTree toRenderTree() {
    if (mCachedRenderTree != null) {
      return mCachedRenderTree;
    }

    final RenderTreeNode root;

    if (mMountableOutputs.isEmpty()) {
      addRootHostRenderTreeNode(this, null, null);
    }

    root = mMountableOutputs.get(0);

    if (root.getRenderUnit().getId() != ROOT_HOST_ID) {
      throw new IllegalStateException(
          "Root render unit has invalid id " + root.getRenderUnit().getId());
    }

    RenderTreeNode[] flatList = new RenderTreeNode[mMountableOutputs.size()];
    for (int i = 0, size = mMountableOutputs.size(); i < size; i++) {
      flatList[i] = mMountableOutputs.get(i);
    }

    final RenderTree renderTree =
        new RenderTree(root, flatList, mWidthSpec, mHeightSpec, mComponentTreeId, null, null);
    mCachedRenderTree = renderTree;

    return renderTree;
  }

  static void setSizeAfterMeasureAndCollectResults(
      ComponentContext c, LithoLayoutContext lithoLayoutContext, LayoutState layoutState) {
    if (lithoLayoutContext.isFutureReleased()) {
      return;
    }

    if (!layoutState.mMountableOutputs.isEmpty()) {
      throw new IllegalStateException(
          "Attempting to collect results on an already populated LayoutState."
              + "\n Root: "
              + layoutState.mRootComponentName);
    }

    final boolean isTracing = ComponentsSystrace.isTracing();
    final int widthSpec = layoutState.mWidthSpec;
    final int heightSpec = layoutState.mHeightSpec;
    final @Nullable LithoLayoutResult root = layoutState.mLayoutResult;
    final @Nullable LithoNode node = root != null ? root.getNode() : null;

    final int rootWidth = root != null ? root.getWidth() : 0;
    final int rootHeight = root != null ? root.getHeight() : 0;
    switch (SizeSpec.getMode(widthSpec)) {
      case SizeSpec.EXACTLY:
        layoutState.mWidth = SizeSpec.getSize(widthSpec);
        break;
      case SizeSpec.AT_MOST:
        layoutState.mWidth = Math.max(0, Math.min(rootWidth, SizeSpec.getSize(widthSpec)));
        break;
      case SizeSpec.UNSPECIFIED:
        layoutState.mWidth = rootWidth;
        break;
    }

    switch (SizeSpec.getMode(heightSpec)) {
      case SizeSpec.EXACTLY:
        layoutState.mHeight = SizeSpec.getSize(heightSpec);
        break;
      case SizeSpec.AT_MOST:
        layoutState.mHeight = Math.max(0, Math.min(rootHeight, SizeSpec.getSize(heightSpec)));
        break;
      case SizeSpec.UNSPECIFIED:
        layoutState.mHeight = rootHeight;
        break;
    }

    // Reset markers before collecting layout outputs.
    layoutState.mCurrentHostMarker = -1;

    if (root == null) {
      return;
    }

    RenderTreeNode parent = null;
    DebugHierarchy.Node hierarchy = null;
    if (c.mLithoConfiguration.mComponentsConfiguration.isShouldAddHostViewForRootComponent()) {
      hierarchy = node != null ? getDebugHierarchy(null, node) : null;
      addRootHostRenderTreeNode(layoutState, root, hierarchy);
      parent = layoutState.mMountableOutputs.get(0);
      layoutState.mCurrentHostMarker = parent.getRenderUnit().getId();
      layoutState.mCurrentHostOutputPosition = 0;
    }

    if (isTracing) {
      ComponentsSystrace.beginSection("collectResults");
    }
    collectResults(
        c,
        root,
        Preconditions.checkNotNull(node),
        layoutState,
        lithoLayoutContext,
        parent,
        null,
        hierarchy);
    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    if (isTracing) {
      ComponentsSystrace.beginSection("sortMountableOutputs");
    }

    sortTops(layoutState);
    sortBottoms(layoutState);

    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    final LithoNode nodeForSaving = layoutState.mRoot;
    final LithoLayoutResult layoutResultForSaving = layoutState.mLayoutResult;

    // clean it up for sanity
    layoutState.mRoot = null;
    layoutState.mLayoutResult = null;

    // enabled for debugging and end to end tests
    if (ComponentsConfiguration.isDebugModeEnabled || ComponentsConfiguration.isEndToEndTestRun) {
      layoutState.mRoot = nodeForSaving;
      layoutState.mLayoutResult = layoutResultForSaving;
      return;
    }

    // override used by analytics teams
    if (ComponentsConfiguration.keepLayoutResults) {
      layoutState.mLayoutResult = layoutResultForSaving;
    }
  }

  private static void sortTops(LayoutState layoutState) {
    final List<IncrementalMountOutput> unsorted = new ArrayList<>(layoutState.mMountableOutputTops);
    try {
      Collections.sort(
          layoutState.mMountableOutputTops, IncrementalMountRenderCoreExtension.sTopsComparator);
    } catch (IllegalArgumentException e) {
      final StringBuilder errorMessage = new StringBuilder();
      errorMessage.append(e.getMessage()).append("\n");
      final int size = unsorted.size();
      errorMessage.append("Error while sorting LayoutState tops. Size: " + size).append("\n");
      final Rect rect = new Rect();
      for (int i = 0; i < size; i++) {
        final RenderTreeNode node = layoutState.getMountableOutputAt(i);
        errorMessage
            .append("   Index " + i + " top: " + node.getAbsoluteBounds(rect).top)
            .append("\n");
      }

      throw new IllegalStateException(errorMessage.toString());
    }
  }

  private static void sortBottoms(LayoutState layoutState) {
    final List<IncrementalMountOutput> unsorted =
        new ArrayList<>(layoutState.mMountableOutputBottoms);
    try {
      Collections.sort(
          layoutState.mMountableOutputBottoms,
          IncrementalMountRenderCoreExtension.sBottomsComparator);
    } catch (IllegalArgumentException e) {
      final StringBuilder errorMessage = new StringBuilder();
      errorMessage.append(e.getMessage()).append("\n");
      final int size = unsorted.size();
      errorMessage.append("Error while sorting LayoutState bottoms. Size: " + size).append("\n");
      final Rect rect = new Rect();
      for (int i = 0; i < size; i++) {
        final RenderTreeNode node = layoutState.getMountableOutputAt(i);
        errorMessage
            .append("   Index " + i + " bottom: " + node.getAbsoluteBounds(rect).bottom)
            .append("\n");
      }

      throw new IllegalStateException(errorMessage.toString());
    }
  }

  static String layoutSourceToString(@RenderSource int source) {
    switch (source) {
      case RenderSource.SET_ROOT_SYNC:
        return "setRootSync";
      case RenderSource.SET_SIZE_SPEC_SYNC:
        return "setSizeSpecSync";
      case RenderSource.UPDATE_STATE_SYNC:
        return "updateStateSync";
      case RenderSource.SET_ROOT_ASYNC:
        return "setRootAsync";
      case RenderSource.SET_SIZE_SPEC_ASYNC:
        return "setSizeSpecAsync";
      case RenderSource.UPDATE_STATE_ASYNC:
        return "updateStateAsync";
      case RenderSource.MEASURE_SET_SIZE_SPEC:
        return "measure_setSizeSpecSync";
      case RenderSource.MEASURE_SET_SIZE_SPEC_ASYNC:
        return "measure_setSizeSpecAsync";
      case RenderSource.TEST:
        return "test";
      case RenderSource.RELOAD_PREVIOUS_STATE:
        return "reloadState";
      case RenderSource.NONE:
        return "none";
      default:
        throw new RuntimeException("Unknown calculate layout source: " + source);
    }
  }

  @ThreadSafe(enableChecks = false)
  void preAllocateMountContent(boolean shouldPreallocatePerMountSpec) {
    if (!shouldPreallocatePerMountSpec) {
      return;
    }

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("preAllocateMountContent:" + mComponent.getSimpleName());
    }

    if (!mMountableOutputs.isEmpty()) {
      for (int i = 0, size = mMountableOutputs.size(); i < size; i++) {
        final RenderTreeNode treeNode = mMountableOutputs.get(i);
        final Component component = getRenderUnit(treeNode).getComponent();

        if (!isSpecGeneratedComponentThatCanPreallocate(component)
            && !isMountableThatCanPreallocate(treeNode)
            && !isPrimitiveThatCanPreallocate(treeNode)) {
          continue;
        }

        if (ComponentsConfiguration.componentPreallocationBlocklist != null
            && ComponentsConfiguration.componentPreallocationBlocklist.contains(
                component.getSimpleName())) {
          continue;
        }

        if (ComponentsConfiguration.enableDrawablePreAllocation
            || isMountableView(treeNode.getRenderUnit())) {
          if (isTracing) {
            ComponentsSystrace.beginSection("preAllocateMountContent:" + component.getSimpleName());
          }

          boolean preallocated =
              MountItemsPool.maybePreallocateContent(
                  mContext.getAndroidContext(), treeNode.getRenderUnit().getContentAllocator());

          Log.d(
              "LayoutState",
              "Preallocation of"
                  + component.getSimpleName()
                  + (preallocated ? " succeeded" : " failed"));

          if (isTracing) {
            ComponentsSystrace.endSection();
          }
        }
      }
    }

    if (isTracing) {
      ComponentsSystrace.endSection();
    }
  }

  private boolean isPrimitiveThatCanPreallocate(RenderTreeNode treeNode) {
    return treeNode.getRenderUnit() instanceof PrimitiveLithoRenderUnit
        && ((PrimitiveLithoRenderUnit) treeNode.getRenderUnit())
            .getPrimitiveRenderUnit()
            .getContentAllocator()
            .canPreallocate();
  }

  private boolean isMountableThatCanPreallocate(RenderTreeNode treeNode) {
    return treeNode.getRenderUnit() instanceof MountableLithoRenderUnit
        && ((MountableLithoRenderUnit) treeNode.getRenderUnit())
            .getMountable()
            .getContentAllocator()
            .canPreallocate();
  }

  private boolean isSpecGeneratedComponentThatCanPreallocate(Component component) {
    return component instanceof SpecGeneratedComponent
        && ((SpecGeneratedComponent) component).canPreallocate();
  }

  boolean isActivityValid() {
    return getValidActivityForContext(mContext.getAndroidContext()) != null;
  }

  @VisibleForTesting
  static @OutputUnitType int getTypeFromId(long id) {
    long masked = id & 0x00000000_00000000_FFFFFFFF_00000000L;
    return (int) (masked >> 32);
  }

  static DiffNode createDiffNode(final ScopedComponentInfo tail, final @Nullable DiffNode parent) {
    final DiffNode diffNode =
        new DefaultDiffNode(tail.getComponent(), tail.getContext().getGlobalKey(), tail);
    if (parent != null) {
      parent.addChild(diffNode);
    }

    return diffNode;
  }

  boolean isCompatibleSpec(int widthSpec, int heightSpec) {
    final boolean widthIsCompatible =
        MeasureComparisonUtils.isMeasureSpecCompatible(mWidthSpec, widthSpec, mWidth);

    final boolean heightIsCompatible =
        MeasureComparisonUtils.isMeasureSpecCompatible(mHeightSpec, heightSpec, mHeight);

    return widthIsCompatible && heightIsCompatible;
  }

  boolean isCompatibleComponentAndSpec(int componentId, int widthSpec, int heightSpec) {
    return mComponent.getId() == componentId && isCompatibleSpec(widthSpec, heightSpec);
  }

  boolean isCompatibleSize(int width, int height) {
    return mWidth == width && mHeight == height;
  }

  boolean isForComponentId(int componentId) {
    return mComponent.getId() == componentId;
  }

  @Override
  public int getMountableOutputCount() {
    return mMountableOutputs.size();
  }

  @Override
  public int getIncrementalMountOutputCount() {
    return mIncrementalMountOutputs.size();
  }

  @Override
  public RenderTreeNode getMountableOutputAt(int index) {
    return mMountableOutputs.get(index);
  }

  @Override
  public Map<Long, ViewAttributes> getViewAttributes() {
    return mRenderUnitsWithViewAttributes;
  }

  @Override
  public @Nullable IncrementalMountOutput getIncrementalMountOutputForId(long id) {
    return mIncrementalMountOutputs.get(id);
  }

  @Override
  public Collection<IncrementalMountOutput> getIncrementalMountOutputs() {
    return mIncrementalMountOutputs.values();
  }

  @Override
  public @Nullable AnimatableItem getAnimatableRootItem() {
    return mAnimatableItems.get(ROOT_HOST_ID);
  }

  @Override
  public @Nullable AnimatableItem getAnimatableItem(long id) {
    return mAnimatableItems.get(id);
  }

  public ArrayList<IncrementalMountOutput> getOutputsOrderedByTopBounds() {
    return mMountableOutputTops;
  }

  public ArrayList<IncrementalMountOutput> getOutputsOrderedByBottomBounds() {
    return mMountableOutputBottoms;
  }

  int getVisibilityOutputCount() {
    return mVisibilityOutputs.size();
  }

  VisibilityOutput getVisibilityOutputAt(int index) {
    return mVisibilityOutputs.get(index);
  }

  @Override
  public List<VisibilityOutput> getVisibilityOutputs() {
    return mVisibilityOutputs;
  }

  @Override
  public int getTestOutputCount() {
    return mTestOutputs == null ? 0 : mTestOutputs.size();
  }

  @Nullable
  @Override
  public TestOutput getTestOutputAt(int index) {
    return mTestOutputs == null ? null : mTestOutputs.get(index);
  }

  public synchronized @Nullable DiffNode getDiffTree() {
    return mDiffTreeRoot;
  }

  int getWidth() {
    return mWidth;
  }

  int getHeight() {
    return mHeight;
  }

  int getWidthSpec() {
    return mWidthSpec;
  }

  int getHeightSpec() {
    return mHeightSpec;
  }

  @Override
  public int getTreeId() {
    return getComponentTreeId();
  }

  /** @return The id of the {@link ComponentTree} that generated this {@link LayoutState} */
  public int getComponentTreeId() {
    return mComponentTreeId;
  }

  /** Id of this {@link LayoutState}. */
  int getId() {
    return mId;
  }

  /**
   * Id of the {@link LayoutState} that was compared to when calculating this {@link LayoutState}.
   */
  int getPreviousLayoutStateId() {
    return mPreviousLayoutStateId;
  }

  public ComponentContext getComponentContext() {
    return mContext;
  }

  boolean isAccessibilityEnabled() {
    return mIsAccessibilityEnabled;
  }

  /**
   * Returns the state handler instance currently held by LayoutState.
   *
   * @return the state handler
   */
  @CheckReturnValue
  TreeState getTreeState() {
    return mTreeState;
  }

  @Nullable
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public LithoNode getLayoutRoot() {
    return mRoot;
  }

  public @Nullable LithoLayoutResult getRootLayoutResult() {
    return mLayoutResult;
  }

  // If the layout root is a nested tree holder node, it gets skipped immediately while
  // collecting the LayoutOutputs. The nested tree itself effectively becomes the layout
  // root in this case.
  private boolean isLayoutRoot(LithoLayoutResult result) {
    return mLayoutResult instanceof NestedTreeHolderResult
        ? result == ((NestedTreeHolderResult) mLayoutResult).getNestedResult()
        : result == mLayoutResult;
  }

  /**
   * @return the position of the {@link LithoRenderUnit} with id layoutOutputId in the {@link
   *     LayoutState} list of outputs or -1 if no {@link LithoRenderUnit} with that id exists in the
   *     {@link LayoutState}
   */
  @Override
  public int getPositionForId(long layoutOutputId) {
    return Preconditions.checkNotNull(mOutputsIdToPositionMap.get(layoutOutputId, -1));
  }

  @Override
  public boolean renderUnitWithIdHostsRenderTrees(long id) {
    return mRenderUnitIdsWhichHostRenderTrees.contains(id);
  }

  @Override
  public Set<Long> getRenderUnitIdsWhichHostRenderTrees() {
    return mRenderUnitIdsWhichHostRenderTrees;
  }

  @Override
  @Nullable
  public List<Transition> getTransitions() {
    return mTransitions;
  }

  /** Gets a mapping from transition ids to a group of LayoutOutput. */
  @Override
  public Map<TransitionId, OutputUnitsAffinityGroup<AnimatableItem>> getTransitionIdMapping() {
    return mTransitionIdMapping;
  }

  /** Gets a group of LayoutOutput given transition key */
  @Override
  @Nullable
  public OutputUnitsAffinityGroup<AnimatableItem> getAnimatableItemForTransitionId(
      TransitionId transitionId) {
    return mTransitionIdMapping.get(transitionId);
  }

  private static void addRenderTreeNode(
      final LayoutState layoutState,
      final RenderTreeNode node,
      final @Nullable LithoLayoutResult result,
      final LithoRenderUnit unit,
      final @OutputUnitType int type,
      final @Nullable TransitionId transitionId,
      final @Nullable RenderTreeNode parent) {

    if (parent != null) {
      parent.child(node);
    }

    final Component component = unit.getComponent();
    if (component instanceof SpecGeneratedComponent
        && ((SpecGeneratedComponent) component).implementsExtraAccessibilityNodes()
        && unit.isAccessible()
        && parent != null) {
      final LithoRenderUnit parentUnit = getRenderUnit(parent);
      ((HostComponent) parentUnit.getComponent()).setImplementsVirtualViews();
    }

    final int position = layoutState.mMountableOutputs.size();
    final Rect absoluteBounds = node.getAbsoluteBounds(new Rect());
    final boolean shouldExcludeMountableFromIncrementalMount =
        unit.findAttachBinderByClass(ExcludeFromIncrementalMountBinder.class) != null;

    final boolean shouldExcludeSpecGeneratedComponentFromIncrementalMount =
        component instanceof SpecGeneratedComponent
            && ((SpecGeneratedComponent) component).excludeFromIncrementalMount();

    final IncrementalMountOutput incrementalMountOutput =
        new IncrementalMountOutput(
            node.getRenderUnit().getId(),
            position,
            absoluteBounds,
            shouldExcludeSpecGeneratedComponentFromIncrementalMount
                || shouldExcludeMountableFromIncrementalMount,
            parent != null
                ? layoutState.mIncrementalMountOutputs.get(parent.getRenderUnit().getId())
                : null);

    if (shouldExcludeSpecGeneratedComponentFromIncrementalMount
        || shouldExcludeMountableFromIncrementalMount) {
      layoutState.mHasComponentsExcludedFromIncrementalMount = true;
    }

    final long id = node.getRenderUnit().getId();
    layoutState.mMountableOutputs.add(node);
    layoutState.mIncrementalMountOutputs.put(id, incrementalMountOutput);
    layoutState.mMountableOutputTops.add(incrementalMountOutput);
    layoutState.mMountableOutputBottoms.add(incrementalMountOutput);

    if ((component instanceof SpecGeneratedComponent
            && ((SpecGeneratedComponent) component).hasChildLithoViews())
        || node.getRenderUnit().doesMountRenderTreeHosts()) {

      layoutState.mRenderUnitIdsWhichHostRenderTrees.add(id);
    }
    boolean willMountView;
    if (type == OutputUnitType.HOST) {
      willMountView = true;
    } else if (type == OutputUnitType.CONTENT) {
      willMountView = result != null && result.getNode().willMountView();
    } else {
      willMountView = false;
    }

    if (unit.getNodeInfo() != null || willMountView) {
      final ViewAttributes attrs = new ViewAttributes();
      final boolean disableBgFgOutputs =
          layoutState.mContext.mLithoConfiguration.mComponentsConfiguration
              .isShouldDisableBgFgOutputs();
      attrs.setHostSpec(Component.isHostSpec(component));
      attrs.setComponentName(component.getSimpleName());
      attrs.setImportantForAccessibility(unit.getImportantForAccessibility());
      attrs.setDisableDrawableOutputs(disableBgFgOutputs);

      if (unit.getNodeInfo() != null) {
        unit.getNodeInfo().copyInto(attrs);
      }
      if (result != null) {
        LithoNode lithoNode = result.getNode();
        // The following only applies if bg/fg outputs are NOT disabled:
        // backgrounds and foregrounds should not be set for HostComponents
        // because those will either be set on the content output or explicit outputs
        // will be created for backgrounds and foreground.
        if (disableBgFgOutputs || !attrs.isHostSpec()) {
          attrs.setBackground(result.getBackground());
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            attrs.setForeground(lithoNode.getForeground());
          }
        }
        if (result.isPaddingSet()) {
          attrs.setPadding(
              new Rect(
                  result.getPaddingLeft(),
                  result.getPaddingTop(),
                  result.getPaddingRight(),
                  result.getPaddingBottom()));
        }
        attrs.setLayoutDirection(result.getResolvedLayoutDirection());
        attrs.setLayerType(lithoNode.getLayerType());
        attrs.setLayoutPaint(lithoNode.getLayerPaint());
        if (attrs.isHostSpec()) {
          if (lithoNode.hasStateListAnimatorResSet()) {
            attrs.setStateListAnimatorRes(lithoNode.getStateListAnimatorRes());
          } else {
            attrs.setStateListAnimator(lithoNode.getStateListAnimator());
          }
        }
      }
      layoutState.mRenderUnitsWithViewAttributes.put(id, attrs);
    }

    final AnimatableItem animatableItem =
        createAnimatableItem(unit, absoluteBounds, type, transitionId);

    layoutState.mAnimatableItems.put(node.getRenderUnit().getId(), animatableItem);

    addLayoutOutputIdToPositionsMap(layoutState.mOutputsIdToPositionMap, unit, position);
    maybeAddLayoutOutputToAffinityGroup(
        layoutState.mCurrentLayoutOutputAffinityGroup, type, animatableItem);
  }

  @Nullable
  public List<ScopedComponentInfo> getScopedComponentInfosNeedingPreviousRenderData() {
    return mScopedComponentInfosNeedingPreviousRenderData;
  }

  @Override
  public void setInitialRootBoundsForAnimation(
      @Nullable Transition.RootBoundsTransition rootWidth,
      @Nullable Transition.RootBoundsTransition rootHeight) {
    mRootWidthAnimation = rootWidth;
    mRootHeightAnimation = rootHeight;
  }

  @Nullable
  @Override
  public List<Transition> getMountTimeTransitions() {
    if (mTreeState == null) {
      return null;
    }
    mTreeState.applyPreviousRenderData(this);

    List<Transition> mountTimeTransitions = null;

    if (mScopedComponentInfosNeedingPreviousRenderData != null) {
      mountTimeTransitions = new ArrayList<>();
      for (int i = 0, size = mScopedComponentInfosNeedingPreviousRenderData.size(); i < size; i++) {
        final ScopedComponentInfo scopedComponentInfo =
            mScopedComponentInfosNeedingPreviousRenderData.get(i);
        final ComponentContext scopedContext = scopedComponentInfo.getContext();
        final Component component = scopedComponentInfo.getComponent();
        try {
          final Transition transition =
              (component instanceof SpecGeneratedComponent)
                  ? ((SpecGeneratedComponent) component).createTransition(scopedContext)
                  : null;
          if (transition != null) {
            mountTimeTransitions.add(transition);
          }
        } catch (Exception e) {
          ComponentUtils.handleWithHierarchy(scopedContext, component, e);
        }
      }
    }

    final List<Transition> updateStateTransitions = mTreeState.getPendingStateUpdateTransitions();
    if (updateStateTransitions != null) {
      if (mountTimeTransitions == null) {
        mountTimeTransitions = new ArrayList<>();
      }
      mountTimeTransitions.addAll(updateStateTransitions);
    }

    return mountTimeTransitions;
  }

  @Override
  public boolean isIncrementalMountEnabled() {
    return mContext != null && ComponentContext.isIncrementalMountEnabled(mContext);
  }

  @Override
  public Systracer getTracer() {
    return mTracer;
  }

  @Override
  @Nullable
  public TransitionId getRootTransitionId() {
    return mRootTransitionId;
  }

  /** Debug-only: return a string representation of this LayoutState and its LayoutOutputs. */
  String dumpAsString() {
    if (!ComponentsConfiguration.isDebugModeEnabled && !ComponentsConfiguration.isEndToEndTestRun) {
      throw new RuntimeException(
          "LayoutState#dumpAsString() should only be called in debug mode or from e2e tests!");
    }

    String res =
        "LayoutState w/ "
            + getMountableOutputCount()
            + " mountable outputs, root: "
            + mRootComponentName
            + "\n";

    for (int i = 0; i < getMountableOutputCount(); i++) {
      final RenderTreeNode node = getMountableOutputAt(i);
      final LithoRenderUnit renderUnit = getRenderUnit(node);
      res +=
          "  ["
              + i
              + "] id: "
              + node.getRenderUnit().getId()
              + ", host: "
              + (node.getParent() != null ? node.getParent().getRenderUnit().getId() : -1)
              + ", component: "
              + renderUnit.getComponent().getSimpleName()
              + "\n";
    }

    return res;
  }

  void checkWorkingRangeAndDispatch(
      int position,
      int firstVisibleIndex,
      int lastVisibleIndex,
      int firstFullyVisibleIndex,
      int lastFullyVisibleIndex,
      WorkingRangeStatusHandler stateHandler) {
    if (mWorkingRangeContainer == null) {
      return;
    }

    mWorkingRangeContainer.checkWorkingRangeAndDispatch(
        position,
        firstVisibleIndex,
        lastVisibleIndex,
        firstFullyVisibleIndex,
        lastFullyVisibleIndex,
        stateHandler);
  }

  void dispatchOnExitRangeIfNeeded(WorkingRangeStatusHandler stateHandler) {
    if (mWorkingRangeContainer == null) {
      return;
    }

    mWorkingRangeContainer.dispatchOnExitedRangeIfNeeded(stateHandler);
  }

  private static @Nullable TransitionId getTransitionIdForNode(@Nullable LithoNode result) {
    if (result == null) {
      return null;
    }
    return TransitionUtils.createTransitionId(
        result.getTransitionKey(),
        result.getTransitionKeyType(),
        result.getTransitionOwnerKey(),
        result.getTransitionGlobalKey());
  }

  @Override
  public boolean needsToRerunTransitions() {
    return mContext.getStateUpdater().isFirstMount();
  }

  @Override
  public void setNeedsToRerunTransitions(boolean needsToRerunTransitions) {
    mContext.getStateUpdater().setFirstMount(needsToRerunTransitions);
  }

  boolean isCommitted() {
    return mIsCommitted;
  }

  void markCommitted() {
    mIsCommitted = true;
  }

  @Override
  public boolean isProcessingVisibilityOutputsEnabled() {
    return mShouldProcessVisibilityOutputs;
  }

  public void setShouldProcessVisibilityOutputs(boolean value) {
    mShouldProcessVisibilityOutputs = value;
  }

  @Override
  public @Nullable String getRootName() {
    return mRootComponentName;
  }

  RenderTreeNode getRenderTreeNode(IncrementalMountOutput output) {
    return getMountableOutputAt(output.getIndex());
  }

  @Nullable
  public Transition.RootBoundsTransition getRootHeightAnimation() {
    return mRootHeightAnimation;
  }

  @Nullable
  public Transition.RootBoundsTransition getRootWidthAnimation() {
    return mRootWidthAnimation;
  }

  @Override
  @Nullable
  public VisibilityBoundsTransformer getVisibilityBoundsTransformer() {
    return getComponentContext().getVisibilityBoundsTransformer();
  }
}
