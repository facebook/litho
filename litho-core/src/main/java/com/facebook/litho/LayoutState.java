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

import static android.content.Context.ACCESSIBILITY_SERVICE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.M;
import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO;
import static com.facebook.litho.Component.MountType.NONE;
import static com.facebook.litho.Component.isHostSpec;
import static com.facebook.litho.Component.isMountSpec;
import static com.facebook.litho.Component.isMountViewSpec;
import static com.facebook.litho.ContextUtils.getValidActivityForContext;
import static com.facebook.litho.FrameworkLogEvents.EVENT_CALCULATE_LAYOUT_STATE;
import static com.facebook.litho.FrameworkLogEvents.EVENT_RESUME_CALCULATE_LAYOUT_STATE;
import static com.facebook.litho.FrameworkLogEvents.PARAM_ATTRIBUTION;
import static com.facebook.litho.FrameworkLogEvents.PARAM_COMPONENT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_IS_BACKGROUND_LAYOUT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_LAYOUT_STATE_SOURCE;
import static com.facebook.litho.FrameworkLogEvents.PARAM_TREE_DIFF_ENABLED;
import static com.facebook.litho.LayoutOutput.LAYOUT_FLAG_DISABLE_TOUCHABLE;
import static com.facebook.litho.LayoutOutput.LAYOUT_FLAG_DRAWABLE_OUTPUTS_DISABLED;
import static com.facebook.litho.LayoutOutput.LAYOUT_FLAG_DUPLICATE_CHILDREN_STATES;
import static com.facebook.litho.LayoutOutput.LAYOUT_FLAG_DUPLICATE_PARENT_STATE;
import static com.facebook.litho.LayoutOutput.LAYOUT_FLAG_MATCH_HOST_BOUNDS;
import static com.facebook.litho.NodeInfo.CLICKABLE_SET_TRUE;
import static com.facebook.litho.NodeInfo.ENABLED_SET_FALSE;
import static com.facebook.litho.NodeInfo.FOCUS_SET_TRUE;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.rendercore.MountState.ROOT_HOST_ID;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityManager;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.ArraySet;
import androidx.collection.LongSparseArray;
import androidx.core.util.Preconditions;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.litho.ComponentTree.LayoutStateFuture;
import com.facebook.litho.EndToEndTestingExtension.EndToEndTestingExtensionInput;
import com.facebook.litho.LithoLayoutResult.NestedTreeHolderResult;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.drawable.BorderColorDrawable;
import com.facebook.litho.stats.LithoStats;
import com.facebook.rendercore.MountItemsPool;
import com.facebook.rendercore.RenderTree;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.incrementalmount.IncrementalMountExtensionInput;
import com.facebook.rendercore.incrementalmount.IncrementalMountOutput;
import com.facebook.rendercore.incrementalmount.IncrementalMountRenderCoreExtension;
import com.facebook.rendercore.transitions.TransitionUtils;
import com.facebook.rendercore.transitions.TransitionsExtensionInput;
import com.facebook.rendercore.visibility.VisibilityExtensionInput;
import com.facebook.rendercore.visibility.VisibilityOutput;
import com.facebook.yoga.YogaDirection;
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
 * #collectResults(ComponentContext, LithoLayoutResult, InternalNode, LayoutState, RenderTreeNode,
 * DiffNode, DebugHierarchy.Node)} which prepares the before-mentioned outputs based on the provided
 * {@link InternalNode} for later use in {@link MountState}.
 */
// This needs to be accessible to statically mock the class in tests.
@VisibleForTesting
@Nullsafe(Nullsafe.Mode.LOCAL)
public class LayoutState
    implements IncrementalMountExtensionInput,
        VisibilityExtensionInput,
        TransitionsExtensionInput,
        EndToEndTestingExtensionInput {

  private static final String DUPLICATE_TRANSITION_IDS = "LayoutState:DuplicateTransitionIds";
  private static final String DUPLICATE_MANUAL_KEY = "LayoutState:DuplicateManualKey";
  private static final String NULL_PARENT_KEY = "LayoutState:NullParentKey";

  static final String KEY_LAYOUT_STATE_ID = "layoutId";
  static final String KEY_PREVIOUS_LAYOUT_STATE_ID = "previousLayoutId";

  @IntDef({
    CalculateLayoutSource.TEST,
    CalculateLayoutSource.NONE,
    CalculateLayoutSource.SET_ROOT_SYNC,
    CalculateLayoutSource.SET_ROOT_ASYNC,
    CalculateLayoutSource.SET_SIZE_SPEC_SYNC,
    CalculateLayoutSource.SET_SIZE_SPEC_ASYNC,
    CalculateLayoutSource.UPDATE_STATE_SYNC,
    CalculateLayoutSource.UPDATE_STATE_ASYNC,
    CalculateLayoutSource.MEASURE_SET_SIZE_SPEC,
    CalculateLayoutSource.MEASURE_SET_SIZE_SPEC_ASYNC,
    CalculateLayoutSource.RELOAD_PREVIOUS_STATE,
  })
  public @interface CalculateLayoutSource {
    int TEST = -2;
    int NONE = -1;
    int SET_ROOT_SYNC = 0;
    int SET_ROOT_ASYNC = 1;
    int SET_SIZE_SPEC_SYNC = 2;
    int SET_SIZE_SPEC_ASYNC = 3;
    int UPDATE_STATE_SYNC = 4;
    int UPDATE_STATE_ASYNC = 5;
    int MEASURE_SET_SIZE_SPEC = 6;
    int MEASURE_SET_SIZE_SPEC_ASYNC = 7;
    int RELOAD_PREVIOUS_STATE = 8;
  }

  private static final AtomicInteger sIdGenerator = new AtomicInteger(1);
  private static final int NO_PREVIOUS_LAYOUT_STATE_ID = -1;

  private final Map<String, Rect> mComponentKeyToBounds = new HashMap<>();
  private final Map<Handle, Rect> mComponentHandleToBounds = new HashMap<>();
  private @Nullable List<Component> mComponents;
  private @Nullable List<String> mComponentKeys;

  private final ComponentContext mContext;

  private final Component mComponent;

  private int mWidthSpec;
  private int mHeightSpec;

  private @Nullable LayoutStateContext mLayoutStateContext;
  private @Nullable LayoutStateContext mPrevLayoutStateContext;

  private final List<RenderTreeNode> mMountableOutputs = new ArrayList<>(8);
  private List<VisibilityOutput> mVisibilityOutputs;
  private final LongSparseArray<Integer> mOutputsIdToPositionMap = new LongSparseArray<>(8);
  private final Map<Long, IncrementalMountOutput> mIncrementalMountOutputs = new LinkedHashMap<>(8);
  private final ArrayList<IncrementalMountOutput> mMountableOutputTops = new ArrayList<>();
  private final ArrayList<IncrementalMountOutput> mMountableOutputBottoms = new ArrayList<>();
  private final LongSparseArray<AnimatableItem> mAnimatableItems = new LongSparseArray<>(8);
  private final Set<Long> mRenderUnitIdsWhichHostRenderTrees = new ArraySet<>(4);

  private final Map<Integer, LithoLayoutResult> mLastMeasuredLayouts;

  private @Nullable LayoutStateOutputIdCalculator mLayoutStateOutputIdCalculator;

  private final @Nullable List<TestOutput> mTestOutputs;

  @Nullable LithoLayoutResult mLayoutRoot;
  @Nullable InternalNode mPartiallyResolvedLayoutRoot;
  @Nullable TransitionId mRootTransitionId;
  @Nullable String mRootComponentName;

  private @Nullable DiffNode mDiffTreeRoot;

  private int mWidth;
  private int mHeight;

  private int mCurrentX;
  private int mCurrentY;

  private int mCurrentLevel = 0;

  // Holds the current host marker in the layout tree.
  private long mCurrentHostMarker = -1L;
  private int mCurrentHostOutputPosition = -1;

  private boolean mShouldDuplicateParentState = true;

  private boolean mShouldGenerateDiffTree = false;
  private int mComponentTreeId = -1;
  int mLayoutVersion;
  private final int mId;
  // Id of the layout state (if any) that was used in comparisons with this layout state.
  private final int mPreviousLayoutStateId;
  private boolean mIsCreateLayoutInProgress;

  private @Nullable AccessibilityManager mAccessibilityManager;
  private boolean mAccessibilityEnabled = false;

  private @Nullable StateHandler mStateHandler;
  private @Nullable List<Component> mComponentsNeedingPreviousRenderData;
  private @Nullable List<String> mComponentKeysNeedingPreviousRenderData;
  private @Nullable TransitionId mCurrentTransitionId;
  private @Nullable OutputUnitsAffinityGroup<AnimatableItem> mCurrentLayoutOutputAffinityGroup;
  private final Map<TransitionId, OutputUnitsAffinityGroup<AnimatableItem>> mTransitionIdMapping =
      new LinkedHashMap<>();
  private final Set<TransitionId> mDuplicatedTransitionIds = new HashSet<>();
  private @Nullable List<Transition> mTransitions;
  // If true, the LayoutState calculate call was interrupted and will need to be resumed to finish
  // creating and measuring the InternalNode of the LayoutState.
  private volatile boolean mIsPartialLayoutState;
  private volatile boolean mIsInterruptible = true;

  @Nullable WorkingRangeContainer mWorkingRangeContainer;

  private @Nullable List<Attachable> mAttachables;

  final boolean mShouldAddHostViewForRootComponent =
      ComponentsConfiguration.shouldAddHostViewForRootComponent;

  final boolean mShouldDisableDrawableOutputs =
      mShouldAddHostViewForRootComponent || ComponentsConfiguration.shouldDisableBgFgOutputs;

  final boolean mDelegateToRenderCoreMount = ComponentsConfiguration.delegateToRenderCoreMount;

  final Map<String, Object> mLayoutData = new HashMap<>();

  // TODO(t66287929): Remove mIsCommitted from LayoutState by matching RenderState logic around
  // Futures.
  private boolean mIsCommitted;

  /** @deprecated create a real instance with `calculate` instead */
  @Deprecated
  LayoutState(ComponentContext context) {
    this(context, Column.create(context).build(), null);
  }

  LayoutState(
      ComponentContext context, Component rootComponent, final @Nullable LayoutState current) {
    mContext = context;
    mComponent = rootComponent;
    mId = sIdGenerator.getAndIncrement();
    mPreviousLayoutStateId = current != null ? current.mId : NO_PREVIOUS_LAYOUT_STATE_ID;
    mStateHandler = mContext.getStateHandler();
    mTestOutputs = ComponentsConfiguration.isEndToEndTestRun ? new ArrayList<TestOutput>(8) : null;
    mLastMeasuredLayouts = new HashMap<>();
    mComponents = new ArrayList<>();
    mComponentKeys = new ArrayList<>();
    mVisibilityOutputs = new ArrayList<>(8);
    mLayoutData.put(KEY_LAYOUT_STATE_ID, mId);
    mLayoutData.put(KEY_PREVIOUS_LAYOUT_STATE_ID, mPreviousLayoutStateId);
  }

  @VisibleForTesting
  Component getRootComponent() {
    return mComponent;
  }

  boolean isPartialLayoutState() {
    return mIsPartialLayoutState;
  }

  boolean isCreateLayoutInProgress() {
    return mIsCreateLayoutInProgress;
  }

  boolean isInterruptible() {
    return mIsInterruptible;
  }

  void setInterruptible(boolean isInterruptible) {
    mIsInterruptible = isInterruptible;
  }

  LayoutStateContext getLayoutStateContext() {
    return Preconditions.checkNotNull(mLayoutStateContext);
  }

  @Nullable
  LayoutStateContext getPrevLayoutStateContext() {
    return mPrevLayoutStateContext;
  }

  /**
   * Acquires a new layout output for the internal node and its associated component. It returns
   * null if there's no component associated with the node as the mount pass only cares about nodes
   * that will potentially mount content into the component host.
   */
  @Nullable
  private static RenderTreeNode createGenericLayoutOutput(
      LithoLayoutResult result,
      InternalNode node,
      LayoutState layoutState,
      boolean hasHostView,
      boolean shouldUseCachedOutputs,
      @Nullable RenderTreeNode parent,
      @Nullable DiffNode diffNode) {
    final Component component = node.getTailComponent();
    final String componentKey = node.getTailComponentKey();

    // Skip empty nodes and layout specs because they don't mount anything.
    if (component == null || component.getMountType() == NONE) {
      return null;
    }

    final boolean isCachedOutputUpdated = shouldUseCachedOutputs && result.areCachedMeasuresValid();
    long previousId = -1;
    if (shouldUseCachedOutputs) {
      final LithoRenderUnit contentOutput = Preconditions.checkNotNull(diffNode).getContentOutput();
      if (contentOutput != null) {
        previousId = contentOutput.getId();
      }
    }

    final long newId =
        layoutState.calculateLayoutOutputId(
            component, componentKey, layoutState.mCurrentLevel, OutputUnitType.CONTENT, previousId);

    return createRenderTreeNode(
        newId,
        component,
        result.getContext(),
        layoutState,
        result,
        node,
        true /* useNodePadding */,
        node.getImportantForAccessibility(),
        previousId != newId
            ? LayoutOutput.STATE_UNKNOWN
            : isCachedOutputUpdated ? LayoutOutput.STATE_UPDATED : LayoutOutput.STATE_DIRTY,
        layoutState.mShouldDuplicateParentState,
        false,
        hasHostView,
        parent);
  }

  private static SparseArray<DynamicValue<?>> mergeCommonDynamicProps(List<Component> components) {
    final SparseArray<DynamicValue<?>> mergedDynamicProps = new SparseArray<>();
    for (Component component : components) {
      final SparseArray<DynamicValue<?>> commonDynamicProps = component.getCommonDynamicProps();
      if (commonDynamicProps == null) {
        continue;
      }
      for (int i = 0; i < commonDynamicProps.size(); i++) {
        final int key = commonDynamicProps.keyAt(i);
        final DynamicValue<?> commonDynamicProp = commonDynamicProps.get(key);
        if (commonDynamicProp != null) {
          mergedDynamicProps.append(key, commonDynamicProp);
        }
      }
    }

    return mergedDynamicProps;
  }

  private static void addRootHostLayoutOutput(
      final LayoutState layoutState,
      final @Nullable LithoLayoutResult result,
      final @Nullable DebugHierarchy.Node hierarchy) {
    final int width = result != null ? result.getWidth() : 0;
    final int height = result != null ? result.getHeight() : 0;
    final Rect bounds = new Rect(0, 0, width, height);

    final LithoRenderUnit unit =
        LithoRenderUnit.create(
            ROOT_HOST_ID,
            HostComponent.create(),
            null,
            null,
            null,
            bounds,
            0,
            0,
            IMPORTANT_FOR_ACCESSIBILITY_AUTO,
            LayoutOutput.STATE_DIRTY,
            null);

    final RenderTreeNode node = LithoRenderUnit.create(unit, new Rect(0, 0, width, height), null);

    final LayoutOutput hostOutput = unit.output;

    if (hierarchy != null) {
      hostOutput.setHierarchy(hierarchy.mutateType(OutputUnitType.HOST));
    }

    addMountableOutput(layoutState, node, unit, hostOutput, OutputUnitType.HOST, null, null);
  }

  private static RenderTreeNode createHostLayoutOutput(
      LayoutState layoutState,
      LithoLayoutResult result,
      InternalNode node,
      @Nullable RenderTreeNode parent,
      @Nullable DebugHierarchy.Node hierarchy) {

    final HostComponent hostComponent = HostComponent.create();

    // We need to pass common dynamic props to the host component, as they only could be applied to
    // views, so we'll need to set them up, when binding HostComponent to ComponentHost. At the same
    // time, we don't remove them from the current component, as we may calculate multiple
    // LayoutStates using same Components
    hostComponent.setCommonDynamicProps(mergeCommonDynamicProps(node.getComponents()));

    final long id;
    final @LayoutOutput.UpdateState int updateState;
    if (isLayoutRootThatRequiresHost(layoutState, result) || parent == null) {
      // The root host (LithoView) always has ID 0 and is unconditionally
      // set as dirty i.e. no need to use shouldComponentUpdate().
      id = ROOT_HOST_ID;
      updateState = LayoutOutput.STATE_DIRTY;
    } else {
      id =
          layoutState.calculateLayoutOutputId(
              hostComponent,
              node.getTailComponentKey(),
              layoutState.mCurrentLevel,
              OutputUnitType.HOST,
              -1);
      updateState = LayoutOutput.STATE_UNKNOWN;
    }

    final RenderTreeNode renderTreeNode =
        createRenderTreeNode(
            id,
            hostComponent,
            null,
            layoutState,
            result,
            node,
            false /* useNodePadding */,
            node.getImportantForAccessibility(),
            updateState,
            node.isDuplicateParentStateEnabled(),
            node.isDuplicateChildrenStatesEnabled(),
            false,
            parent);

    final LayoutOutput hostOutput = ((LithoRenderUnit) renderTreeNode.getRenderUnit()).output;

    if (hierarchy != null) {
      hostOutput.setHierarchy(hierarchy.mutateType(OutputUnitType.HOST));
    }

    ViewNodeInfo viewNodeInfo = hostOutput.getViewNodeInfo();
    if (viewNodeInfo != null) {
      if (node.hasStateListAnimatorResSet()) {
        viewNodeInfo.setStateListAnimatorRes(node.getStateListAnimatorRes());
      } else {
        viewNodeInfo.setStateListAnimator(node.getStateListAnimator());
      }
    }

    return renderTreeNode;
  }

  /* TODO: (T81557408) Fix @Nullable issue */
  private static RenderTreeNode createDrawableLayoutOutput(
      Component component,
      String componentKey,
      LayoutState layoutState,
      LithoLayoutResult result,
      InternalNode node,
      boolean hasHostView,
      @OutputUnitType int outputType,
      long previousId,
      boolean isCachedOutputUpdated,
      @Nullable RenderTreeNode parent) {

    final long id =
        layoutState.calculateLayoutOutputId(
            component, componentKey, layoutState.mCurrentLevel, outputType, previousId);

    return createRenderTreeNode(
        id,
        component,
        null,
        layoutState,
        result,
        node,
        false /* useNodePadding */,
        IMPORTANT_FOR_ACCESSIBILITY_NO,
        previousId != id
            ? LayoutOutput.STATE_UNKNOWN
            : isCachedOutputUpdated ? LayoutOutput.STATE_UPDATED : LayoutOutput.STATE_DIRTY,
        layoutState.mShouldDuplicateParentState,
        false,
        hasHostView,
        parent);
  }

  private static RenderTreeNode createRenderTreeNode(
      long id,
      Component component,
      @Nullable ComponentContext context,
      LayoutState layoutState,
      LithoLayoutResult result,
      InternalNode node,
      boolean useNodePadding,
      int importantForAccessibility,
      @LayoutOutput.UpdateState int updateState,
      boolean duplicateParentState,
      boolean duplicateChildrenStates,
      boolean hasHostView,
      @Nullable RenderTreeNode parent) {
    final boolean isMountViewSpec = isMountViewSpec(component);

    final int hostTranslationX;
    final int hostTranslationY;
    if (parent != null) {
      hostTranslationX = parent.getAbsoluteX();
      hostTranslationY = parent.getAbsoluteY();
    } else {
      hostTranslationX = 0;
      hostTranslationY = 0;
    }

    int flags = 0;

    int l = layoutState.mCurrentX + result.getX();
    int t = layoutState.mCurrentY + result.getY();
    int r = l + result.getWidth();
    int b = t + result.getHeight();

    final int paddingLeft = useNodePadding ? result.getPaddingLeft() : 0;
    final int paddingTop = useNodePadding ? result.getPaddingTop() : 0;
    final int paddingRight = useNodePadding ? result.getPaddingRight() : 0;
    final int paddingBottom = useNodePadding ? result.getPaddingBottom() : 0;

    // View mount specs are able to set their own attributes when they're mounted.
    // Non-view specs (drawable and layout) always transfer their view attributes
    // to their respective hosts.
    // Moreover, if the component mounts a view, then we apply padding to the view itself later on.
    // Otherwise, apply the padding to the bounds of the layout output.

    final NodeInfo layoutOutputNodeInfo;
    final ViewNodeInfo layoutOutputViewNodeInfo;

    final NodeInfo nodeInfo = node.getNodeInfo();

    if (isMountViewSpec) {
      layoutOutputNodeInfo = nodeInfo;
      // Acquire a ViewNodeInfo, set it up and release it after passing it to the LayoutOutput.
      final ViewNodeInfo viewNodeInfo = new ViewNodeInfo();

      // The following only applies if bg/fg outputs are NOT disabled:
      // backgrounds and foregrounds should not be set for HostComponents
      // because those will either be set on the content output or explicit outputs
      // will be created for backgrounds and foreground.
      if (layoutState.mShouldDisableDrawableOutputs || !isHostSpec(component)) {
        viewNodeInfo.setBackground(result.getBackground());
        if (SDK_INT >= M) {
          viewNodeInfo.setForeground(node.getForeground());
        }
      }
      if (useNodePadding && result.isPaddingSet()) {
        viewNodeInfo.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
      }
      viewNodeInfo.setLayoutDirection(result.getResolvedLayoutDirection());
      viewNodeInfo.setExpandedTouchBounds(
          result,
          node,
          l - hostTranslationX,
          t - hostTranslationY,
          r - hostTranslationX,
          b - hostTranslationY);
      viewNodeInfo.setLayerType(node.getLayerType(), node.getLayerPaint());
      layoutOutputViewNodeInfo = viewNodeInfo;
    } else {
      l += paddingLeft;
      t += paddingTop;
      r -= paddingRight;
      b -= paddingBottom;

      if (nodeInfo != null && nodeInfo.getEnabledState() == ENABLED_SET_FALSE) {
        flags |= LAYOUT_FLAG_DISABLE_TOUCHABLE;
      }
      layoutOutputNodeInfo = null;
      layoutOutputViewNodeInfo = null;
    }

    final Rect bounds = new Rect(l, t, r, b);

    if (duplicateParentState) {
      flags |= LAYOUT_FLAG_DUPLICATE_PARENT_STATE;
    }

    if (duplicateChildrenStates) {
      flags |= LAYOUT_FLAG_DUPLICATE_CHILDREN_STATES;
    }

    final TransitionId transitionId;
    if (hasHostView) {
      flags |= LAYOUT_FLAG_MATCH_HOST_BOUNDS;
      transitionId = null;
    } else {
      // If there is a host view, the transition key will be set on the view's layout output
      transitionId = layoutState.mCurrentTransitionId;
    }

    if (layoutState.mShouldDisableDrawableOutputs) {
      flags |= LAYOUT_FLAG_DRAWABLE_OUTPUTS_DISABLED;
    }

    final LithoRenderUnit unit =
        LithoRenderUnit.create(
            id,
            component,
            context,
            layoutOutputNodeInfo,
            layoutOutputViewNodeInfo,
            bounds,
            layoutState.mMountableOutputs.size(),
            flags,
            importantForAccessibility,
            updateState,
            transitionId);

    return LithoRenderUnit.create(
        unit,
        LithoRenderUnit.getMountBounds(new Rect(), bounds, hostTranslationX, hostTranslationY),
        parent);
  }

  static AnimatableItem createAnimatableItem(
      final LithoRenderUnit unit,
      final Rect absoluteBounds,
      final @OutputUnitType int outputType,
      final @Nullable TransitionId transitionId) {
    return new LithoAnimtableItem(
        unit.getId(), absoluteBounds, outputType, unit.output.getNodeInfo(), transitionId);
  }

  /**
   * Acquires a {@link VisibilityOutput} object and computes the bounds for it using the information
   * stored in the {@link InternalNode}.
   */
  private static VisibilityOutput createVisibilityOutput(
      final LithoLayoutResult result,
      final InternalNode node,
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
        component != null ? Preconditions.checkNotNull(componentGlobalKey) : "null",
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
      final InternalNode node,
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

  /**
   * Determine if a given {@link InternalNode} within the context of a given {@link LayoutState}
   * requires to be wrapped inside a view.
   *
   * @see #needsHostView(LithoLayoutResult, InternalNode, LayoutState)
   */
  private static boolean hasViewContent(final InternalNode node, final LayoutState layoutState) {
    final Component component = node.getTailComponent();
    final NodeInfo nodeInfo = node.getNodeInfo();

    final boolean implementsAccessibility =
        (nodeInfo != null && nodeInfo.needsAccessibilityDelegate())
            || (component != null && component.implementsAccessibility());

    final int importantForAccessibility = node.getImportantForAccessibility();

    // A component has accessibility content if:
    //   1. Accessibility is currently enabled.
    //   2. Accessibility hasn't been explicitly disabled on it
    //      i.e. IMPORTANT_FOR_ACCESSIBILITY_NO.
    //   3. Any of these conditions are true:
    //      - It implements accessibility support.
    //      - It has a content description.
    //      - It has importantForAccessibility set as either IMPORTANT_FOR_ACCESSIBILITY_YES
    //        or IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS.
    // IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS should trigger an inner host
    // so that such flag is applied in the resulting view hierarchy after the component
    // tree is mounted. Click handling is also considered accessibility content but
    // this is already covered separately i.e. click handler is not null.
    final boolean hasBackgroundOrForeground =
        layoutState.mShouldDisableDrawableOutputs
            && (node.getBackground() != null || node.getForeground() != null);
    final boolean hasAccessibilityContent =
        layoutState.mAccessibilityEnabled
            && importantForAccessibility != IMPORTANT_FOR_ACCESSIBILITY_NO
            && (implementsAccessibility
                || (nodeInfo != null && !TextUtils.isEmpty(nodeInfo.getContentDescription()))
                || importantForAccessibility != IMPORTANT_FOR_ACCESSIBILITY_AUTO);

    return hasBackgroundOrForeground
        || hasAccessibilityContent
        || node.isDuplicateChildrenStatesEnabled()
        || hasViewAttributes(nodeInfo)
        || node.getLayerType() != LayerType.LAYER_TYPE_NOT_SET;
  }

  private static boolean hasViewAttributes(@Nullable NodeInfo nodeInfo) {
    if (nodeInfo == null) {
      return false;
    }

    final boolean hasFocusChangeHandler = nodeInfo.hasFocusChangeHandler();
    final boolean hasEnabledTouchEventHandlers =
        nodeInfo.hasTouchEventHandlers() && nodeInfo.getEnabledState() != ENABLED_SET_FALSE;
    final boolean hasViewTag = nodeInfo.getViewTag() != null;
    final boolean hasViewTags = nodeInfo.getViewTags() != null;
    final boolean hasShadowElevation = nodeInfo.getShadowElevation() != 0;
    final boolean hasOutlineProvider = nodeInfo.getOutlineProvider() != null;
    final boolean hasClipToOutline = nodeInfo.getClipToOutline();
    final boolean isFocusableSetTrue = nodeInfo.getFocusState() == FOCUS_SET_TRUE;
    final boolean isClickableSetTrue = nodeInfo.getClickableState() == CLICKABLE_SET_TRUE;
    final boolean hasClipChildrenSet = nodeInfo.isClipChildrenSet();
    final boolean hasTransitionName = nodeInfo.getTransitionName() != null;

    return hasFocusChangeHandler
        || hasEnabledTouchEventHandlers
        || hasViewTag
        || hasViewTags
        || hasShadowElevation
        || hasOutlineProvider
        || hasClipToOutline
        || hasClipChildrenSet
        || isFocusableSetTrue
        || isClickableSetTrue
        || hasTransitionName;
  }

  @Nullable
  private static DebugHierarchy.Node getDebugHierarchy(
      @Nullable DebugHierarchy.Node parentHierarchy, final InternalNode node) {
    return ComponentsConfiguration.isDebugHierarchyEnabled
        ? DebugHierarchy.newNode(parentHierarchy, node.getTailComponent(), node.getComponents())
        : null;
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
   * <p>At this stage all the {@link InternalNode} for which we have LayoutOutputs that can be
   * recycled will have a DiffNode associated. If the CachedMeasures are valid we'll try to recycle
   * both the host and the contents (including background/foreground). In all other cases instead
   * we'll only try to re-use the hosts. In some cases the host's structure might change between two
   * updates even if the component is of the same type. This can happen for example when a click
   * listener is added. To avoid trying to re-use the wrong host type we explicitly check that after
   * all the children for a subtree have been added (this is when the actual host type is resolved).
   * If the host type changed compared to the one in the DiffNode we need to refresh the ids for the
   * whole subtree in order to ensure that the MountState will unmount the subtree and mount it
   * again on the correct host.
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
      final InternalNode node,
      final LayoutState layoutState,
      @Nullable RenderTreeNode parent,
      final @Nullable DiffNode parentDiffNode,
      final @Nullable DebugHierarchy.Node parentHierarchy) {
    final LayoutStateContext layoutStateContext = layoutState.getLayoutStateContext();

    if (layoutStateContext.isLayoutReleased()) {
      return;
    }

    final Component component = Preconditions.checkNotNull(node.getTailComponent());
    final String componentGlobalKey = Preconditions.checkNotNull(node.getTailComponentKey());
    final boolean isTracing = ComponentsSystrace.isTracing();

    final DebugHierarchy.Node hierarchy = getDebugHierarchy(parentHierarchy, node);

    // Early return if collecting results of a node holding a nested tree.
    if (result instanceof NestedTreeHolderResult) {
      // If the nested tree is defined, it has been resolved during a measure call during
      // layout calculation.
      if (isTracing) {
        ComponentsSystrace.beginSectionWithArgs("resolveNestedTree:" + node.getSimpleName())
            .arg("widthSpec", "EXACTLY " + result.getWidth())
            .arg("heightSpec", "EXACTLY " + result.getHeight())
            .arg("rootComponentId", node.getTailComponent().getId())
            .flush();
      }

      final int size = node.getComponents().size();
      final ComponentContext immediateParentContext;
      if (size == 1) {
        immediateParentContext = parentContext;
      } else {
        immediateParentContext =
            node.getComponents()
                .get(1)
                .getScopedContext(layoutStateContext, node.getComponentKeys().get(1));
      }

      LithoLayoutResult nestedTree =
          Layout.create(
              layoutStateContext,
              Preconditions.checkNotNull(immediateParentContext),
              (NestedTreeHolderResult) result,
              SizeSpec.makeSizeSpec(result.getWidth(), EXACTLY),
              SizeSpec.makeSizeSpec(result.getHeight(), EXACTLY),
              layoutState.mPrevLayoutStateContext);

      if (isTracing) {
        ComponentsSystrace.endSection();
      }

      if (nestedTree == null) {
        return;
      }

      // Account for position of the holder node.
      layoutState.mCurrentX += result.getX();
      layoutState.mCurrentY += result.getY();

      collectResults(
          parentContext,
          nestedTree,
          nestedTree.getInternalNode(),
          layoutState,
          parent,
          parentDiffNode,
          hierarchy);

      layoutState.mCurrentX -= result.getX();
      layoutState.mCurrentY -= result.getY();

      return;
    }

    final boolean shouldGenerateDiffTree = layoutState.mShouldGenerateDiffTree;
    final DiffNode currentDiffNode = result.getDiffNode();
    final boolean shouldUseCachedOutputs = isMountSpec(component) && currentDiffNode != null;

    final DiffNode diffNode;
    if (shouldGenerateDiffTree) {
      diffNode = createDiffNode(result, node, parentDiffNode);
      if (parentDiffNode == null) {
        layoutState.mDiffTreeRoot = diffNode;
      }
    } else {
      diffNode = null;
    }

    final boolean needsHostView = needsHostView(result, node, layoutState);

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
    if (needsHostView) {
      final int hostLayoutPosition =
          addHostLayoutOutput(parent, result, node, layoutState, diffNode, hierarchy);
      addCurrentAffinityGroupToTransitionMapping(layoutState);

      parent = layoutState.mMountableOutputs.get(hostLayoutPosition);

      layoutState.mCurrentLevel++;
      layoutState.mCurrentHostMarker = parent.getRenderUnit().getId();
      layoutState.mCurrentHostOutputPosition = hostLayoutPosition;
    }

    // We need to take into account flattening when setting duplicate parent state. The parent after
    // flattening may no longer exist. Therefore the value of duplicate parent state should only be
    // true if the path between us (inclusive) and our inner/root host (exclusive) all are
    // duplicate parent state.
    final boolean shouldDuplicateParentState = layoutState.mShouldDuplicateParentState;
    layoutState.mShouldDuplicateParentState =
        needsHostView
            || layoutState.isLayoutRoot(result)
            || (shouldDuplicateParentState && node.isDuplicateParentStateEnabled());

    // 2. Add background if defined.
    if (!layoutState.mShouldDisableDrawableOutputs) {
      final Drawable background = result.getBackground();

      // Only create a background output when the component does not mount a View because
      // the background will get set in the output of the component.
      if (background != null && !isMountViewSpec(component)) {
        final LithoRenderUnit convertBackground =
            (currentDiffNode != null) ? currentDiffNode.getBackgroundOutput() : null;

        final RenderTreeNode backgroundRenderTreeNode =
            addDrawableComponent(
                parent,
                result,
                node,
                layoutState,
                convertBackground,
                hierarchy,
                background,
                OutputUnitType.BACKGROUND,
                needsHostView);

        if (diffNode != null) {
          diffNode.setBackgroundOutput((LithoRenderUnit) backgroundRenderTreeNode.getRenderUnit());
        }
      }
    }

    final @Nullable RenderTreeNode renderTreeNode;

    // Generate the layoutOutput for the given node.
    final @Nullable RenderTreeNode contentRenderTreeNode =
        createGenericLayoutOutput(
            result,
            node,
            layoutState,
            needsHostView,
            shouldUseCachedOutputs,
            parent,
            currentDiffNode);
    final @Nullable LithoRenderUnit contentRenderUnit;
    final @Nullable LayoutOutput contentLayoutOutput;

    if (contentRenderTreeNode != null) {
      contentRenderUnit = (LithoRenderUnit) contentRenderTreeNode.getRenderUnit();
      contentLayoutOutput = contentRenderUnit.output;
    } else {
      contentRenderUnit = null;
      contentLayoutOutput = null;
    }

    if (contentLayoutOutput != null && hierarchy != null) {
      contentLayoutOutput.setHierarchy(hierarchy.mutateType(OutputUnitType.CONTENT));
    }

    // 3. Now add the MountSpec (either View or Drawable) to the Outputs.
    final ComponentContext scopedContext =
        Preconditions.checkNotNull(
            component.getScopedContext(layoutState.getLayoutStateContext(), componentGlobalKey));
    if (isMountSpec(component)) {

      renderTreeNode = contentRenderTreeNode;

      // Notify component about its final size.
      if (isTracing) {
        ComponentsSystrace.beginSection("onBoundsDefined:" + node.getSimpleName());
      }

      try {
        component.onBoundsDefined(scopedContext, result);
      } catch (Exception e) {
        ComponentUtils.handleWithHierarchy(scopedContext, component, e);
      } finally {
        if (isTracing) {
          ComponentsSystrace.endSection();
        }
      }

      addMountableOutput(
          layoutState,
          Preconditions.checkNotNull(contentRenderTreeNode),
          Preconditions.checkNotNull(contentRenderUnit),
          Preconditions.checkNotNull(contentLayoutOutput),
          OutputUnitType.CONTENT,
          !needsHostView ? layoutState.mCurrentTransitionId : null,
          parent);

      if (diffNode != null) {
        diffNode.setContentOutput(contentRenderUnit);
      }
    } else {
      renderTreeNode = needsHostView ? parent : null;
    }

    // 4. Extract the Transitions.
    if (Layout.areTransitionsEnabled(scopedContext)) {
      final ArrayList<Transition> transitions = node.getTransitions();
      if (transitions != null) {
        for (int i = 0, size = transitions.size(); i < size; i++) {
          final Transition transition = transitions.get(i);
          if (layoutState.mTransitions == null) {
            layoutState.mTransitions = new ArrayList<>();
          }
          TransitionUtils.addTransitions(
              transition, layoutState.mTransitions, layoutState.mRootComponentName);
        }
      }

      final Map<String, Component> componentsNeedingPreviousRenderData =
          node.getComponentsNeedingPreviousRenderData();
      if (componentsNeedingPreviousRenderData != null) {
        if (layoutState.mComponentsNeedingPreviousRenderData == null) {
          layoutState.mComponentsNeedingPreviousRenderData = new ArrayList<>();
        }

        if (layoutState.mComponentKeysNeedingPreviousRenderData == null) {
          layoutState.mComponentKeysNeedingPreviousRenderData = new ArrayList<>();
        }
        // We'll check for animations in mount

        for (Map.Entry<String, Component> entry : componentsNeedingPreviousRenderData.entrySet()) {
          layoutState.mComponentKeysNeedingPreviousRenderData.add(entry.getKey());
          layoutState.mComponentsNeedingPreviousRenderData.add(entry.getValue());
        }
      }
    }

    layoutState.mCurrentX += result.getX();
    layoutState.mCurrentY += result.getY();

    // We must process the nodes in order so that the layout state output order is correct.
    for (int i = 0, size = result.getChildCount(); i < size; i++) {
      final LithoLayoutResult child = result.getChildAt(i);
      collectResults(
          scopedContext, child, child.getInternalNode(), layoutState, parent, diffNode, hierarchy);
    }

    layoutState.mCurrentX -= result.getX();
    layoutState.mCurrentY -= result.getY();

    // 5. Add border color if defined.
    if (result.shouldDrawBorders()) {
      final LithoRenderUnit convertBorder =
          (currentDiffNode != null) ? currentDiffNode.getBorderOutput() : null;
      final RenderTreeNode borderRenderTreeNode =
          addDrawableComponent(
              parent,
              result,
              node,
              layoutState,
              convertBorder,
              hierarchy,
              getBorderColorDrawable(result, node),
              OutputUnitType.BORDER,
              needsHostView);

      if (diffNode != null) {
        diffNode.setBorderOutput((LithoRenderUnit) borderRenderTreeNode.getRenderUnit());
      }
    }

    // 6. Add foreground if defined.
    if (!layoutState.mShouldDisableDrawableOutputs) {
      final Drawable foreground = node.getForeground();
      // Only create a foreground output when the component does not mount a View because
      // the foreground has already been set in the output of the component.
      if (foreground != null && (!isMountViewSpec(component) || SDK_INT < M)) {

        final LithoRenderUnit convertForeground =
            (currentDiffNode != null) ? currentDiffNode.getForegroundOutput() : null;

        final RenderTreeNode foregroundRenderTreeNode =
            addDrawableComponent(
                parent,
                result,
                node,
                layoutState,
                convertForeground,
                hierarchy,
                foreground,
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
          createVisibilityOutput(result, node, layoutState, renderTreeNode);

      layoutState.mVisibilityOutputs.add(visibilityOutput);

      if (diffNode != null) {
        diffNode.setVisibilityOutput(visibilityOutput);
      }
    }

    // 8. If we're in a testing environment, maintain an additional data structure with
    // information about nodes that we can query later.
    if (layoutState.mTestOutputs != null && !TextUtils.isEmpty(node.getTestKey())) {
      final TestOutput testOutput = createTestOutput(result, node, layoutState, contentRenderUnit);
      layoutState.mTestOutputs.add(testOutput);
    }

    // 9. Extract the Working Range registrations.
    List<WorkingRangeContainer.Registration> registrations = node.getWorkingRangeRegistrations();
    if (registrations != null && !registrations.isEmpty()) {
      if (layoutState.mWorkingRangeContainer == null) {
        layoutState.mWorkingRangeContainer = new WorkingRangeContainer();
      }

      for (WorkingRangeContainer.Registration registration : registrations) {
        layoutState.mWorkingRangeContainer.registerWorkingRange(
            registration.mName,
            registration.mWorkingRange,
            registration.mComponent,
            registration.mKey);
      }
    }

    final List<Attachable> attachables = result.getInternalNode().getAttachables();
    if (attachables != null) {
      if (layoutState.mAttachables == null) {
        layoutState.mAttachables = new ArrayList<>();
      }
      layoutState.mAttachables.addAll(attachables);
    }

    if (component != null) {
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

      final List<String> componentKeys = node.getComponentKeys();
      for (int i = 0, size = node.getComponents().size(); i < size; i++) {
        final Component delegate = node.getComponents().get(i);
        final String delegateKey = componentKeys.get(i);
        // Keep a list of the components we created during this layout calculation. If the layout is
        // valid, the ComponentTree will update the event handlers that have been created in the
        // previous ComponentTree with the new component dispatched, otherwise Section children
        // might not be accessing the correct props and state on the event handlers. The null
        // checkers cover tests, the scope and tree should not be null at this point of the layout
        // calculation.
        final ComponentContext delegateScopedContext =
            delegate.getScopedContext(layoutStateContext, delegateKey);
        if (delegateScopedContext != null && delegateScopedContext.getComponentTree() != null) {
          if (layoutState.mComponents != null) {
            layoutState.mComponents.add(delegate);
            Preconditions.checkNotNull(layoutState.mComponentKeys).add(delegateKey);
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
    }

    // All children for the given host have been added, restore the previous
    // host, level, and duplicate parent state value in the recursive queue.
    if (layoutState.mCurrentHostMarker != currentHostMarker) {
      layoutState.mCurrentHostMarker = currentHostMarker;
      layoutState.mCurrentHostOutputPosition = currentHostOutputPosition;
      layoutState.mCurrentLevel--;
    }
    layoutState.mShouldDuplicateParentState = shouldDuplicateParentState;

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

  @Nullable
  List<Component> consumeComponents() {
    final List<Component> components = mComponents;
    mComponents = null;

    return components;
  }

  @Nullable
  List<String> consumeComponentKeys() {
    final List<String> componentKeys = mComponentKeys;
    mComponentKeys = null;

    return componentKeys;
  }

  @Nullable
  List<Attachable> getAttachables() {
    return mAttachables;
  }

  /**
   * Returns true when the layout state is the root, and shouldDisableDrawableOutputs flag is false
   * When this flag is set to true, layout roots don't need their own host unless some other
   * conditions apply, in which case they will need their own dedicated host other than the
   * LithoView.
   *
   * @see #needsHostView(LithoLayoutResult, InternalNode, LayoutState)
   */
  private static boolean isLayoutRootThatRequiresHost(
      LayoutState layoutState, LithoLayoutResult result) {
    return !layoutState.mShouldAddHostViewForRootComponent && layoutState.isLayoutRoot(result);
  }

  private static RenderTreeNode addDrawableComponent(
      final @Nullable RenderTreeNode parent,
      LithoLayoutResult result,
      InternalNode node,
      LayoutState layoutState,
      @Nullable LithoRenderUnit recycle,
      @Nullable DebugHierarchy.Node hierarchy,
      Drawable drawable,
      @OutputUnitType int type,
      boolean matchHostBoundsTransitions) {
    final Component drawableComponent = DrawableComponent.create(drawable);
    boolean isOutputUpdated;
    if (recycle != null) {
      try {
        isOutputUpdated =
            !drawableComponent.shouldComponentUpdate(
                null, recycle.output.getComponent(), null, drawableComponent);
      } catch (Exception e) {
        ComponentUtils.handleWithHierarchy(result.getContext(), drawableComponent, e);
        isOutputUpdated = false;
      }
    } else {
      isOutputUpdated = false;
    }

    final long previousId = recycle != null ? recycle.getId() : -1;
    final RenderTreeNode renderTreeNode =
        createDrawableLayoutOutput(
            parent,
            drawableComponent,
            Preconditions.checkNotNull(node.getTailComponentKey()),
            layoutState,
            hierarchy,
            result,
            node,
            type,
            previousId,
            isOutputUpdated,
            matchHostBoundsTransitions);

    final LithoRenderUnit drawableRenderUnit = (LithoRenderUnit) renderTreeNode.getRenderUnit();
    final LayoutOutput output = drawableRenderUnit.output;

    addMountableOutput(
        layoutState,
        renderTreeNode,
        drawableRenderUnit,
        drawableRenderUnit.output,
        type,
        !matchHostBoundsTransitions ? layoutState.mCurrentTransitionId : null,
        parent);

    if (hierarchy != null) {
      output.setHierarchy(hierarchy.mutateType(type));
    }

    return renderTreeNode;
  }

  private static Drawable getBorderColorDrawable(LithoLayoutResult result, InternalNode node) {
    if (!result.shouldDrawBorders()) {
      throw new RuntimeException("This result does not support drawing border color");
    }

    final boolean isRtl = result.recursivelyResolveLayoutDirection() == YogaDirection.RTL;
    final float[] borderRadius = node.getBorderRadius();
    final int[] borderColors = node.getBorderColors();
    final YogaEdge leftEdge = isRtl ? YogaEdge.RIGHT : YogaEdge.LEFT;
    final YogaEdge rightEdge = isRtl ? YogaEdge.LEFT : YogaEdge.RIGHT;

    return new BorderColorDrawable.Builder()
        .pathEffect(node.getBorderPathEffect())
        .borderLeftColor(Border.getEdgeColor(borderColors, leftEdge))
        .borderTopColor(Border.getEdgeColor(borderColors, YogaEdge.TOP))
        .borderRightColor(Border.getEdgeColor(borderColors, rightEdge))
        .borderBottomColor(Border.getEdgeColor(borderColors, YogaEdge.BOTTOM))
        .borderLeftWidth(result.getLayoutBorder(leftEdge))
        .borderTopWidth(result.getLayoutBorder(YogaEdge.TOP))
        .borderRightWidth(result.getLayoutBorder(rightEdge))
        .borderBottomWidth(result.getLayoutBorder(YogaEdge.BOTTOM))
        .borderRadius(borderRadius)
        .build();
  }

  private static void addLayoutOutputIdToPositionsMap(
      LongSparseArray outputsIdToPositionMap, LithoRenderUnit unit, int position) {
    if (outputsIdToPositionMap != null) {
      outputsIdToPositionMap.put(unit.getId(), position);
    }
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
                + ComponentUtils.treeToString(layoutState.mLayoutRoot));
      }
    }

    layoutState.mCurrentLayoutOutputAffinityGroup = null;
    layoutState.mCurrentTransitionId = null;
  }
  /* TODO: (T81557408) Fix @Nullable issue */
  private static RenderTreeNode createDrawableLayoutOutput(
      final @Nullable RenderTreeNode parent,
      Component drawableComponent,
      String ownerComponentKey, // the key of the component that owns this bg/fg/etc
      LayoutState layoutState,
      @Nullable DebugHierarchy.Node hierarchy,
      LithoLayoutResult result,
      InternalNode node,
      @OutputUnitType int outputType,
      long previousId,
      boolean isCachedOutputUpdated,
      boolean matchHostBoundsTransitions) {

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("onBoundsDefined:" + node.getSimpleName());
    }

    try {
      drawableComponent.onBoundsDefined(layoutState.mContext, result);
    } catch (Exception e) {
      ComponentUtils.handleWithHierarchy(layoutState.mContext, drawableComponent, e);
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }

    final RenderTreeNode renderTreeNode =
        createDrawableLayoutOutput(
            drawableComponent,
            ownerComponentKey,
            layoutState,
            result,
            node,
            matchHostBoundsTransitions,
            outputType,
            previousId,
            isCachedOutputUpdated,
            parent);

    return renderTreeNode;
  }

  /**
   * If we have an interactive LayoutSpec or a MountSpec Drawable, we need to insert an
   * HostComponent in the Outputs such as it will be used as a HostView at Mount time. View
   * MountSpec are not allowed.
   *
   * @return The position the HostLayoutOutput was inserted.
   */
  private static int addHostLayoutOutput(
      final @Nullable RenderTreeNode parent,
      LithoLayoutResult result,
      InternalNode node,
      LayoutState layoutState,
      @Nullable DiffNode diffNode,
      @Nullable DebugHierarchy.Node hierarchy) {
    final Component component = node.getTailComponent();

    // Only the root host is allowed to wrap view mount specs as a layout output
    // is unconditionally added for it.
    if (isMountViewSpec(component) && !layoutState.isLayoutRoot(result)) {
      throw new IllegalArgumentException("We shouldn't insert a host as a parent of a View");
    }

    final RenderTreeNode hostRenderTreeNode =
        createHostLayoutOutput(layoutState, result, node, parent, hierarchy);
    final LithoRenderUnit hostRenderUnit = (LithoRenderUnit) hostRenderTreeNode.getRenderUnit();
    final LayoutOutput hostLayoutOutput = hostRenderUnit.output;

    if (diffNode != null) {
      diffNode.setHostOutput(hostRenderUnit);
    }

    // The component of the hostLayoutOutput will be set later after all the
    // children got processed.
    addMountableOutput(
        layoutState,
        hostRenderTreeNode,
        hostRenderUnit,
        hostLayoutOutput,
        OutputUnitType.HOST,
        layoutState.mCurrentTransitionId,
        parent);

    return layoutState.mMountableOutputs.size() - 1;
  }

  @VisibleForTesting
  static LayoutState calculate(
      ComponentContext c,
      Component component,
      int componentTreeId,
      int widthSpec,
      int heightSpec,
      @CalculateLayoutSource int source) {
    return calculate(
        c,
        component,
        null,
        componentTreeId,
        widthSpec,
        heightSpec,
        -1,
        false /* shouldGenerateDiffTree */,
        null /* previousDiffTreeRoot */,
        source,
        null);
  }

  static LayoutState calculate(
      ComponentContext c,
      Component component,
      @Nullable LayoutStateFuture layoutStateFuture,
      int componentTreeId,
      int widthSpec,
      int heightSpec,
      int layoutVersion,
      boolean shouldGenerateDiffTree,
      @Nullable LayoutState currentLayoutState,
      @CalculateLayoutSource int source,
      @Nullable String extraAttribution) {

    final ComponentsLogger logger = c.getLogger();

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      if (extraAttribution != null) {
        ComponentsSystrace.beginSection("extra:" + extraAttribution);
      }
      ComponentsSystrace.beginSectionWithArgs(
              new StringBuilder("LayoutState.calculate_")
                  .append(component.getSimpleName())
                  .append("_")
                  .append(layoutSourceToString(source))
                  .toString())
          .arg("treeId", componentTreeId)
          .arg("rootId", component.getId())
          .arg("widthSpec", SizeSpec.toString(widthSpec))
          .arg("heightSpec", SizeSpec.toString(heightSpec))
          .flush();
    }

    final @Nullable DiffNode diffTreeRoot;
    final @Nullable LithoLayoutResult currentLayoutRoot;
    final @Nullable LayoutStateContext currentLayoutStateContext;

    final boolean isReconcilable;

    if (currentLayoutState != null) {
      synchronized (currentLayoutState) {
        diffTreeRoot = currentLayoutState.mDiffTreeRoot;
        currentLayoutRoot = currentLayoutState.mLayoutRoot;
        currentLayoutStateContext = currentLayoutState.getLayoutStateContext();
        isReconcilable = isReconcilable(c, component, currentLayoutRoot);
        if (!isReconcilable) { // Release the current InternalNode tree if it is not reconcilable.
          currentLayoutState.mLayoutRoot = null;
        }
      }
    } else {
      diffTreeRoot = null;
      currentLayoutRoot = null;
      currentLayoutStateContext = null;
      isReconcilable = false;
    }

    final LayoutState layoutState;
    final LayoutStateContext layoutStateContext;

    try {
      final PerfEvent logLayoutState =
          logger != null
              ? LogTreePopulator.populatePerfEventFromLogger(
                  c, logger, logger.newPerformanceEvent(c, EVENT_CALCULATE_LAYOUT_STATE))
              : null;
      if (logLayoutState != null) {
        logLayoutState.markerAnnotate(PARAM_COMPONENT, component.getSimpleName());
        logLayoutState.markerAnnotate(PARAM_LAYOUT_STATE_SOURCE, layoutSourceToString(source));
        logLayoutState.markerAnnotate(PARAM_IS_BACKGROUND_LAYOUT, !ThreadUtils.isMainThread());
        logLayoutState.markerAnnotate(PARAM_TREE_DIFF_ENABLED, diffTreeRoot != null);
        logLayoutState.markerAnnotate(PARAM_ATTRIBUTION, extraAttribution);
      }

      layoutState = new LayoutState(c, component, currentLayoutState);

      layoutState.mPrevLayoutStateContext = currentLayoutStateContext;

      layoutStateContext =
          new LayoutStateContext(
              layoutState, c.getComponentTree(), layoutStateFuture, diffTreeRoot);

      // Detect errors internal to components
      Component.markLayoutStarted(component, layoutStateContext);

      if (isReconcilable) {
        layoutStateContext.copyScopedInfoFrom(
            Preconditions.checkNotNull(currentLayoutStateContext),
            Preconditions.checkNotNull(c.getStateHandler()));
      }

      final InternalNode layoutCreatedInWillRender =
          component.consumeLayoutCreatedInWillRender(currentLayoutStateContext, c);

      layoutState.mLayoutStateContext = layoutStateContext;
      c.setLayoutStateContext(layoutStateContext);

      layoutState.mShouldGenerateDiffTree = shouldGenerateDiffTree;
      layoutState.mComponentTreeId = componentTreeId;
      layoutState.mLayoutVersion = layoutVersion;
      layoutState.mAccessibilityManager =
          (AccessibilityManager) c.getAndroidContext().getSystemService(ACCESSIBILITY_SERVICE);
      layoutState.mAccessibilityEnabled =
          AccessibilityUtils.isAccessibilityEnabled(layoutState.mAccessibilityManager);
      layoutState.mWidthSpec = widthSpec;
      layoutState.mHeightSpec = heightSpec;
      layoutState.mRootComponentName = component.getSimpleName();
      layoutState.mIsCreateLayoutInProgress = true;

      final @Nullable LithoLayoutResult root;
      if (layoutCreatedInWillRender == null) {

        final LayoutResultHolder holder =
            Layout.createAndMeasureComponent(
                layoutStateContext,
                c,
                component,
                isReconcilable
                    ? Preconditions.checkNotNull(currentLayoutRoot)
                        .getInternalNode()
                        .getHeadComponentKey()
                    : null,
                widthSpec,
                heightSpec,
                isReconcilable ? currentLayoutRoot : null,
                layoutState.mPrevLayoutStateContext,
                diffTreeRoot,
                logLayoutState);

        // Check if layout was interrupted.
        if (holder.wasLayoutInterrupted()) {
          layoutState.mPartiallyResolvedLayoutRoot =
              Preconditions.checkNotNull(holder.mPartiallyResolvedLayout);
          layoutState.mRootTransitionId = getTransitionIdForNode(holder.mPartiallyResolvedLayout);
          layoutState.mIsCreateLayoutInProgress = false;
          layoutState.mIsPartialLayoutState = true;
          if (logLayoutState != null) {
            Preconditions.checkNotNull(logger).logPerfEvent(logLayoutState);
          }

          return layoutState;
        } else {
          root = holder.mResult;
        }

      } else {
        root =
            Layout.measure(
                layoutStateContext,
                c,
                layoutCreatedInWillRender,
                widthSpec,
                heightSpec,
                currentLayoutRoot,
                layoutState.mPrevLayoutStateContext,
                diffTreeRoot);
      }

      final @Nullable InternalNode node = root != null ? root.getInternalNode() : null;

      layoutState.mLayoutRoot = root;
      layoutState.mRootTransitionId = getTransitionIdForNode(node);
      layoutState.mIsCreateLayoutInProgress = false;

      if (logLayoutState != null) {
        logLayoutState.markerPoint("start_collect_results");
      }

      setSizeAfterMeasureAndCollectResults(c, layoutState);

      layoutStateContext.releaseReference();

      if (logLayoutState != null) {
        logLayoutState.markerPoint("end_collect_results");
        Preconditions.checkNotNull(logger).logPerfEvent(logLayoutState);
      }
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
        if (extraAttribution != null) {
          ComponentsSystrace.endSection();
        }
      }
    }
    LithoStats.incrementComponentCalculateLayoutCount();
    if (ThreadUtils.isMainThread()) {
      LithoStats.incrementComponentCalculateLayoutOnUICount();
    }

    return layoutState;
  }

  static LayoutState resumeCalculate(
      @CalculateLayoutSource int source,
      @Nullable String extraAttribution,
      LayoutState layoutState) {
    final ComponentContext c = layoutState.mContext;

    if (!layoutState.mIsPartialLayoutState) {
      throw new IllegalStateException("Can not resume a finished LayoutState calculation");
    }

    final Component component = layoutState.mComponent;
    final int componentTreeId = layoutState.mComponentTreeId;
    final int widthSpec = layoutState.mWidthSpec;
    final int heightSpec = layoutState.mHeightSpec;

    final ComponentsLogger logger = c.getLogger();

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      if (extraAttribution != null) {
        ComponentsSystrace.beginSection("extra:" + extraAttribution);
      }
      ComponentsSystrace.beginSectionWithArgs(
              new StringBuilder("LayoutState.resumeCalculate_")
                  .append(component.getSimpleName())
                  .append("_")
                  .append(layoutSourceToString(source))
                  .toString())
          .arg("treeId", componentTreeId)
          .arg("rootId", component.getId())
          .arg("widthSpec", SizeSpec.toString(widthSpec))
          .arg("heightSpec", SizeSpec.toString(heightSpec))
          .flush();
    }

    try {
      final PerfEvent logLayoutState =
          logger != null
              ? LogTreePopulator.populatePerfEventFromLogger(
                  c, logger, logger.newPerformanceEvent(c, EVENT_RESUME_CALCULATE_LAYOUT_STATE))
              : null;
      if (logLayoutState != null) {
        logLayoutState.markerAnnotate(PARAM_COMPONENT, component.getSimpleName());
        logLayoutState.markerAnnotate(PARAM_LAYOUT_STATE_SOURCE, layoutSourceToString(source));
      }

      // If we already have a LayoutState but the InternalNode is only partially resolved,
      // resume resolving the InternalNode and measure it.

      layoutState.mLayoutRoot =
          Layout.resumeCreateAndMeasureComponent(
              layoutState.getLayoutStateContext(),
              c,
              Preconditions.checkNotNull(layoutState.mPartiallyResolvedLayoutRoot),
              widthSpec,
              heightSpec,
              layoutState.mPrevLayoutStateContext,
              layoutState.mDiffTreeRoot,
              logLayoutState);

      setSizeAfterMeasureAndCollectResults(c, layoutState);

      layoutState.getLayoutStateContext().releaseReference();

      if (logLayoutState != null) {
        Preconditions.checkNotNull(logger).logPerfEvent(logLayoutState);
      }
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
        if (extraAttribution != null) {
          ComponentsSystrace.endSection();
        }
      }
    }

    return layoutState;
  }

  RenderTree toRenderTree() {
    final RenderTreeNode root;

    if (mMountableOutputs.isEmpty()) {
      addRootHostLayoutOutput(this, null, null);
    }

    root = mMountableOutputs.get(0);

    RenderTreeNode[] flatList = new RenderTreeNode[mMountableOutputs.size()];
    for (int i = 0, size = mMountableOutputs.size(); i < size; i++) {
      flatList[i] = mMountableOutputs.get(i);
    }

    final RenderTree renderTree = new RenderTree(root, flatList, mWidthSpec, mHeightSpec, null);
    renderTree.setRenderTreeData(this);
    return renderTree;
  }

  private static void setSizeAfterMeasureAndCollectResults(
      ComponentContext c, LayoutState layoutState) {
    if (layoutState.getLayoutStateContext().isLayoutReleased()) {
      return;
    }

    final boolean isTracing = ComponentsSystrace.isTracing();
    final int widthSpec = layoutState.mWidthSpec;
    final int heightSpec = layoutState.mHeightSpec;
    final @Nullable LithoLayoutResult root = layoutState.mLayoutRoot;
    final @Nullable InternalNode node = root != null ? root.getInternalNode() : null;

    final int rootWidth = root != null ? root.getWidth() : 0;
    final int rootHeight = root != null ? root.getHeight() : 0;
    switch (SizeSpec.getMode(widthSpec)) {
      case SizeSpec.EXACTLY:
        layoutState.mWidth = SizeSpec.getSize(widthSpec);
        break;
      case SizeSpec.AT_MOST:
        layoutState.mWidth = Math.min(rootWidth, SizeSpec.getSize(widthSpec));
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
        layoutState.mHeight = Math.min(rootHeight, SizeSpec.getSize(heightSpec));
        break;
      case SizeSpec.UNSPECIFIED:
        layoutState.mHeight = rootHeight;
        break;
    }

    layoutState.clearLayoutStateOutputIdCalculator();

    // Reset markers before collecting layout outputs.
    layoutState.mCurrentHostMarker = -1;

    if (root == null) {
      return;
    }

    RenderTreeNode parent = null;
    DebugHierarchy.Node hierarchy = null;
    if (layoutState.mShouldAddHostViewForRootComponent) {
      hierarchy = node != null ? getDebugHierarchy(null, node) : null;
      addRootHostLayoutOutput(layoutState, root, hierarchy);
      parent = layoutState.mMountableOutputs.get(0);
      layoutState.mCurrentLevel++;
      layoutState.mCurrentHostMarker = parent.getRenderUnit().getId();
      layoutState.mCurrentHostOutputPosition = 0;
    }

    if (isTracing) {
      ComponentsSystrace.beginSection("collectResults");
    }
    collectResults(c, root, Preconditions.checkNotNull(node), layoutState, parent, null, hierarchy);
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

    if (!c.isReconciliationEnabled()
        && !ComponentsConfiguration.isDebugModeEnabled
        && !ComponentsConfiguration.isEndToEndTestRun
        && !ComponentsConfiguration.keepInternalNodes) {
      layoutState.mLayoutRoot = null;
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

  private static boolean isReconcilable(
      final ComponentContext c,
      final Component nextRootComponent,
      final @Nullable LithoLayoutResult currentLayoutResult) {

    if (currentLayoutResult == null || !c.isReconciliationEnabled()) {
      return false;
    }

    StateHandler stateHandler = c.getStateHandler();
    if (stateHandler == null || !stateHandler.hasUncommittedUpdates()) {
      return false;
    }

    final Component currentRootComponent =
        Preconditions.checkNotNull(currentLayoutResult.getInternalNode().getHeadComponent());

    if (!nextRootComponent.getKey().equals(currentRootComponent.getKey())) {
      return false;
    }

    if (!ComponentUtils.isSameComponentType(currentRootComponent, nextRootComponent)) {
      return false;
    }

    if (!ComponentUtils.isEquivalent(currentRootComponent, nextRootComponent)) {
      return false;
    }

    return true;
  }

  static String layoutSourceToString(@CalculateLayoutSource int source) {
    switch (source) {
      case CalculateLayoutSource.SET_ROOT_SYNC:
        return "setRoot";
      case CalculateLayoutSource.SET_SIZE_SPEC_SYNC:
        return "setSizeSpec";
      case CalculateLayoutSource.UPDATE_STATE_SYNC:
        return "updateStateSync";
      case CalculateLayoutSource.SET_ROOT_ASYNC:
        return "setRootAsync";
      case CalculateLayoutSource.SET_SIZE_SPEC_ASYNC:
        return "setSizeSpecAsync";
      case CalculateLayoutSource.UPDATE_STATE_ASYNC:
        return "updateStateAsync";
      case CalculateLayoutSource.MEASURE_SET_SIZE_SPEC:
        return "measure_setSizeSpec";
      case CalculateLayoutSource.MEASURE_SET_SIZE_SPEC_ASYNC:
        return "measure_setSizeSpecAsync";
      case CalculateLayoutSource.TEST:
        return "test";
      case CalculateLayoutSource.RELOAD_PREVIOUS_STATE:
        return "reloadState";
      case CalculateLayoutSource.NONE:
        return "none";
      default:
        throw new RuntimeException("Unknown calculate layout source: " + source);
    }
  }

  @ThreadSafe(enableChecks = false)
  void preAllocateMountContent(boolean shouldPreallocatePerMountSpec, int recyclingMode) {
    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("preAllocateMountContent:" + mComponent.getSimpleName());
    }

    if (mMountableOutputs != null && !mMountableOutputs.isEmpty()) {
      for (int i = 0, size = mMountableOutputs.size(); i < size; i++) {
        final RenderTreeNode treeNode = mMountableOutputs.get(i);
        final LayoutOutput output = LayoutOutput.getLayoutOutput(treeNode);
        final Component component = output.getComponent();

        if (shouldPreallocatePerMountSpec && !component.canPreallocate()) {
          continue;
        }

        if (ComponentsConfiguration.componentPreallocationBlocklist != null
            && ComponentsConfiguration.componentPreallocationBlocklist.contains(
                component.getSimpleName())) {
          continue;
        }

        if (Component.isMountViewSpec(component)) {
          if (isTracing) {
            ComponentsSystrace.beginSection("preAllocateMountContent:" + component.getSimpleName());
          }

          if (mDelegateToRenderCoreMount) {
            MountItemsPool.maybePreallocateContent(
                mContext.getAndroidContext(), treeNode.getRenderUnit());
          } else {
            ComponentsPools.maybePreallocateContent(
                mContext.getAndroidContext(), component, recyclingMode);
          }

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

  boolean isActivityValid() {
    return getValidActivityForContext(mContext.getAndroidContext()) != null;
  }

  private void clearLayoutStateOutputIdCalculator() {
    if (mLayoutStateOutputIdCalculator != null) {
      mLayoutStateOutputIdCalculator.clear();
    }
  }

  private long calculateLayoutOutputId(
      Component component,
      @Nullable String componentKey,
      int level,
      @OutputUnitType int type,
      long previousId) {
    final ComponentTree componentTree =
        Preconditions.checkNotNull(
            Preconditions.checkNotNull(mLayoutStateContext).getComponentTree());
    if (componentTree.useRenderUnitIdMap()) {
      return addTypeAndComponentTreeToId(
          componentTree.getRenderUnitIdMap().getId(Preconditions.checkNotNull(componentKey)),
          type,
          componentTree.mId);
    } else {
      if (mLayoutStateOutputIdCalculator == null) {
        mLayoutStateOutputIdCalculator = new LayoutStateOutputIdCalculator();
      }

      return mLayoutStateOutputIdCalculator.calculateLayoutOutputId(
          component, level, type, previousId);
    }
  }

  private static long addTypeAndComponentTreeToId(
      int id, @OutputUnitType int type, int componentTreeId) {
    return (long) id | ((long) type) << 32 | ((long) componentTreeId) << 35;
  }

  @Nullable
  LithoLayoutResult getCachedLayout(Component component) {
    return mLastMeasuredLayouts.get(component.getId());
  }

  boolean hasCachedLayout(Component component) {
    return mLastMeasuredLayouts.containsKey(component.getId());
  }

  @VisibleForTesting
  protected void clearCachedLayout(Component component) {
    mLastMeasuredLayouts.remove(component.getId());
  }

  void addLastMeasuredLayout(Component component, LithoLayoutResult lastMeasuredLayout) {
    mLastMeasuredLayouts.put(component.getId(), lastMeasuredLayout);
  }

  static DiffNode createDiffNode(
      final LithoLayoutResult result, final InternalNode node, final @Nullable DiffNode parent) {
    final DiffNode diffNode = new DefaultDiffNode();
    final Component tail = node.getTailComponent();
    final String key = node.getTailComponentKey();
    diffNode.setLastWidthSpec(result.getLastWidthSpec());
    diffNode.setLastHeightSpec(result.getLastHeightSpec());
    diffNode.setLastMeasuredWidth(result.getLastMeasuredWidth());
    diffNode.setLastMeasuredHeight(result.getLastMeasuredHeight());
    diffNode.setComponent(tail, key);
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

  boolean isCompatibleAccessibility() {
    return AccessibilityUtils.isAccessibilityEnabled(mAccessibilityManager)
        == mAccessibilityEnabled;
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

  public @Nullable DiffNode getDiffTree() {
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

  /**
   * Returns the state handler instance currently held by LayoutState and nulls it afterwards.
   *
   * @return the state handler
   */
  @Nullable
  @CheckReturnValue
  StateHandler consumeStateHandler() {
    final StateHandler stateHandler = mStateHandler;
    mStateHandler = null;
    return stateHandler;
  }

  @Nullable
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public LithoLayoutResult getLayoutRoot() {
    return mLayoutRoot;
  }

  // If the layout root is a nested tree holder node, it gets skipped immediately while
  // collecting the LayoutOutputs. The nested tree itself effectively becomes the layout
  // root in this case.
  private boolean isLayoutRoot(LithoLayoutResult result) {
    return mLayoutRoot instanceof NestedTreeHolderResult
        ? result == ((NestedTreeHolderResult) mLayoutRoot).getNestedResult()
        : result == mLayoutRoot;
  }

  /**
   * Returns true if this is the root node (which always generates a matching layout output), if the
   * node has view attributes e.g. tags, content description, etc, or if the node has explicitly
   * been forced to be wrapped in a view.
   */
  private static boolean needsHostView(
      final LithoLayoutResult result, final InternalNode node, final LayoutState layoutState) {
    if (isLayoutRootThatRequiresHost(layoutState, result)) {
      // Need a View for the Root component.
      return true;
    }

    final Component component = node.getTailComponent();
    if (isMountViewSpec(component)) {
      // Component already represents a View.
      return false;
    }

    if (node.isForceViewWrapping()) {
      // Wrapping into a View requested.
      return true;
    }

    if (hasViewContent(node, layoutState)) {
      // Has View content (e.g. Accessibility content, Focus change listener, shadow, view tag etc)
      // thus needs a host View.
      return true;
    }

    if (needsHostViewForCommonDynamicProps(node)) {
      return true;
    }

    if (needsHostViewForTransition(node)) {
      return true;
    }

    if (hasSelectedStateWhenDisablingDrawableOutputs(layoutState, node)) {
      return true;
    }

    return false;
  }

  private static boolean hasSelectedStateWhenDisablingDrawableOutputs(
      final LayoutState layoutState, final InternalNode node) {
    return layoutState.mShouldAddHostViewForRootComponent
        && !isMountViewSpec(node.getTailComponent())
        && node.getNodeInfo() != null
        && node.getNodeInfo().getSelectedState() != NodeInfo.SELECTED_UNSET;
  }

  private static boolean needsHostViewForCommonDynamicProps(final InternalNode node) {
    final List<Component> components = node.getComponents();
    for (Component comp : components) {
      if (comp != null && comp.hasCommonDynamicProps()) {
        // Need a host View to apply the dynamic props to
        return true;
      }
    }
    return false;
  }

  private static boolean needsHostViewForTransition(final InternalNode node) {
    return !TextUtils.isEmpty(node.getTransitionKey()) && !isMountViewSpec(node.getTailComponent());
  }

  /**
   * Similar to {@link #needsHostView(LithoLayoutResult, InternalNode, LayoutState)} but without
   * dependency to {@link LayoutState} instance. This will be used for debugging tools to indicate
   * whether the mountable output is a wrapped View or View MountSpec. Unlike {@link
   * #needsHostView(LithoLayoutResult, InternalNode, LayoutState)} this does not consider
   * accessibility also does not consider root component, but this approximation is good enough for
   * debugging purposes.
   */
  static boolean hasViewOutput(InternalNode node) {
    return node.isForceViewWrapping()
        || isMountViewSpec(node.getTailComponent())
        || hasViewAttributes(node.getNodeInfo())
        || needsHostViewForCommonDynamicProps(node)
        || needsHostViewForTransition(node);
  }

  /**
   * @return the position of the {@link LayoutOutput} with id layoutOutputId in the {@link
   *     LayoutState} list of outputs or -1 if no {@link LayoutOutput} with that id exists in the
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

  /** @return a {@link LayoutOutput} for a given {@param layoutOutputId} */
  @Nullable
  LayoutOutput getLayoutOutput(long layoutOutputId) {
    final int position = getPositionForId(layoutOutputId);
    return position < 0 ? null : LayoutOutput.getLayoutOutput(getMountableOutputAt(position));
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

  private static void addMountableOutput(
      final LayoutState layoutState,
      final RenderTreeNode node,
      final LithoRenderUnit unit,
      final LayoutOutput layoutOutput,
      final @OutputUnitType int type,
      final @Nullable TransitionId transitionId,
      final @Nullable RenderTreeNode parent) {

    if (parent != null) {
      parent.child(node);
    }

    if (layoutOutput.getComponent().implementsExtraAccessibilityNodes()
        && layoutOutput.isAccessible()
        && parent != null) {
      final LayoutOutput output = LayoutOutput.getLayoutOutput(parent);
      ((HostComponent) output.getComponent()).setImplementsVirtualViews();
    }

    final int position = layoutState.mMountableOutputs.size();
    final Rect absoluteBounds = node.getAbsoluteBounds(new Rect());

    final IncrementalMountOutput incrementalMountOutput =
        new IncrementalMountOutput(
            node.getRenderUnit().getId(),
            position,
            absoluteBounds,
            parent != null
                ? layoutState.mIncrementalMountOutputs.get(parent.getRenderUnit().getId())
                : null);

    final long id = node.getRenderUnit().getId();
    layoutState.mMountableOutputs.add(node);
    layoutState.mIncrementalMountOutputs.put(id, incrementalMountOutput);
    layoutState.mMountableOutputTops.add(incrementalMountOutput);
    layoutState.mMountableOutputBottoms.add(incrementalMountOutput);
    if (layoutOutput.getComponent().hasChildLithoViews()) {
      layoutState.mRenderUnitIdsWhichHostRenderTrees.add(id);
    }

    final AnimatableItem animatableItem =
        createAnimatableItem(unit, absoluteBounds, type, transitionId);

    layoutState.mAnimatableItems.put(node.getRenderUnit().getId(), animatableItem);

    addLayoutOutputIdToPositionsMap(layoutState.mOutputsIdToPositionMap, unit, position);
    maybeAddLayoutOutputToAffinityGroup(
        layoutState.mCurrentLayoutOutputAffinityGroup, type, animatableItem);
  }

  /**
   * @return the list of Components in this LayoutState that care about the previously mounted
   *     versions of their @Prop/@State params.
   */
  @Nullable
  public List<Component> getComponentsNeedingPreviousRenderData() {
    return mComponentsNeedingPreviousRenderData;
  }

  @Nullable
  public List<String> getComponentKeysNeedingPreviousRenderData() {
    return mComponentKeysNeedingPreviousRenderData;
  }

  @Override
  public void setInitialRootBoundsForAnimation(
      @Nullable Transition.RootBoundsTransition rootWidth,
      @Nullable Transition.RootBoundsTransition rootHeight) {
    final ComponentTree componentTree = mContext.getComponentTree();
    if (componentTree != null) {
      componentTree.setRootWidthAnimation(rootWidth);
      componentTree.setRootHeightAnimation(rootHeight);
    }
  }

  @Nullable
  @Override
  public List<Transition> getMountTimeTransitions() {
    final ComponentTree componentTree = mContext.getComponentTree();
    if (componentTree == null) {
      return null;
    }
    componentTree.applyPreviousRenderData(this);

    List<Transition> mountTimeTransitions = null;

    if (mComponentsNeedingPreviousRenderData != null) {
      mountTimeTransitions = new ArrayList<>();
      for (int i = 0, size = mComponentsNeedingPreviousRenderData.size(); i < size; i++) {
        final Component component = mComponentsNeedingPreviousRenderData.get(i);
        final String globalKey =
            Preconditions.checkNotNull(mComponentKeysNeedingPreviousRenderData).get(i);
        final ComponentContext scopedContext =
            Preconditions.checkNotNull(
                component.getScopedContext(getLayoutStateContext(), globalKey));
        try {
          final Transition transition = component.createTransition(scopedContext);
          if (transition != null) {
            mountTimeTransitions.add(transition);
          }
        } catch (Exception e) {
          ComponentUtils.handleWithHierarchy(scopedContext, component, e);
        }
      }
    }

    final List<Transition> updateStateTransitions = componentTree.getStateUpdateTransitions();
    if (updateStateTransitions != null) {
      if (mountTimeTransitions == null) {
        mountTimeTransitions = new ArrayList<>();
      }
      mountTimeTransitions.addAll(updateStateTransitions);
    }

    return mountTimeTransitions;
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
      final LayoutOutput layoutOutput = LayoutOutput.getLayoutOutput(getMountableOutputAt(i));
      res +=
          "  ["
              + i
              + "] id: "
              + node.getRenderUnit().getId()
              + ", host: "
              + (node.getParent() != null ? node.getParent().getRenderUnit().getId() : -1)
              + ", component: "
              + layoutOutput.getComponent().getSimpleName()
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
        getLayoutStateContext(),
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

    mWorkingRangeContainer.dispatchOnExitedRangeIfNeeded(getLayoutStateContext(), stateHandler);
  }

  private static @Nullable TransitionId getTransitionIdForNode(@Nullable InternalNode result) {
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
    return mContext.getComponentTree().isFirstMount();
  }

  @Override
  public void setNeedsToRerunTransitions(boolean needsToRerunTransitions) {
    mContext.getComponentTree().setIsFirstMount(needsToRerunTransitions);
  }

  boolean isCommitted() {
    return mIsCommitted;
  }

  void markCommitted() {
    mIsCommitted = true;
  }

  @Override
  public @Nullable String getRootName() {
    return mRootComponentName;
  }

  RenderTreeNode getRenderTreeNode(IncrementalMountOutput output) {
    return getMountableOutputAt(output.getIndex());
  }

  LayoutOutput getLayoutOutput(IncrementalMountOutput output) {
    return LayoutOutput.getLayoutOutput(getRenderTreeNode(output));
  }

  @VisibleForTesting
  void setLayoutStateContextForTest(LayoutStateContext layoutStateContext) {
    mLayoutStateContext = layoutStateContext;
  }

  static int getId(ComponentContext c) {
    final LayoutState state = c.getLayoutState();
    if (state != null) {
      return state.getId();
    }

    return 0;
  }

  static int getPreviousId(ComponentContext c) {
    final LayoutState state = c.getLayoutState();
    if (state != null) {
      return state.getPreviousLayoutStateId();
    }

    return 0;
  }

  /**
   * @deprecated kept around for old code, you should create a real instance instead with
   *     `calculate`
   */
  @Deprecated
  public static LayoutState createTestInstance(ComponentContext c) {
    return new LayoutState(c);
  }
}
