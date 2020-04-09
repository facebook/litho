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
import static com.facebook.litho.Component.isMountSpec;
import static com.facebook.litho.Component.isMountViewSpec;
import static com.facebook.litho.ComponentContext.NULL_LAYOUT;
import static com.facebook.litho.ComponentLifecycle.MountType.NONE;
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
import static com.facebook.litho.LayoutOutput.LAYOUT_FLAG_DUPLICATE_PARENT_STATE;
import static com.facebook.litho.LayoutOutput.LAYOUT_FLAG_MATCH_HOST_BOUNDS;
import static com.facebook.litho.MountState.ROOT_HOST_ID;
import static com.facebook.litho.NodeInfo.CLICKABLE_SET_TRUE;
import static com.facebook.litho.NodeInfo.ENABLED_SET_FALSE;
import static com.facebook.litho.NodeInfo.ENABLED_UNSET;
import static com.facebook.litho.NodeInfo.FOCUS_SET_TRUE;
import static com.facebook.litho.SizeSpec.EXACTLY;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityManager;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.LongSparseArray;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.litho.ComponentTree.LayoutStateFuture;
import com.facebook.litho.IncrementalMountExtension.IncrementalMountExtensionInput;
import com.facebook.litho.TransitionsExtension.TransitionsExtensionInput;
import com.facebook.litho.VisibilityOutputsExtension.VisibilityOutputsExtensionInput;
import com.facebook.litho.annotations.ImportantForAccessibility;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.drawable.BorderColorDrawable;
import com.facebook.litho.stats.LithoStats;
import com.facebook.rendercore.RenderTree;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
 * #collectResults(RenderTreeNode, ComponentContext, DebugHierarchy.Node, InternalNode, LayoutState,
 * DiffNode)} which prepares the before-mentioned outputs based on the provided {@link InternalNode}
 * for later use in {@link MountState}.
 */
// This needs to be accessible to statically mock the class in tests.
@VisibleForTesting
class LayoutState
    implements IncrementalMountExtensionInput,
        VisibilityOutputsExtensionInput,
        TransitionsExtensionInput {

  private static final String DUPLICATE_TRANSITION_IDS = "LayoutState:DuplicateTransitionIds";
  private static final String DUPLICATE_MANUAL_KEY = "LayoutState:DuplicateManualKey";
  private static final String NULL_PARENT_KEY = "LayoutState:NullParentKey";
  private final boolean mIncrementalVisibility;

  @IntDef({
    CalculateLayoutSource.TEST,
    CalculateLayoutSource.NONE,
    CalculateLayoutSource.SET_ROOT_SYNC,
    CalculateLayoutSource.SET_ROOT_ASYNC,
    CalculateLayoutSource.SET_SIZE_SPEC_SYNC,
    CalculateLayoutSource.SET_SIZE_SPEC_ASYNC,
    CalculateLayoutSource.UPDATE_STATE_SYNC,
    CalculateLayoutSource.UPDATE_STATE_ASYNC,
    CalculateLayoutSource.MEASURE
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
    int MEASURE = 6;
  }

  static final Comparator<RenderTreeNode> sTopsComparator =
      new Comparator<RenderTreeNode>() {
        @Override
        public int compare(RenderTreeNode l, RenderTreeNode r) {
          LayoutOutput lhs = LayoutOutput.getLayoutOutput(l);
          LayoutOutput rhs = LayoutOutput.getLayoutOutput(r);
          final int lhsTop = lhs.getBounds().top;
          final int rhsTop = rhs.getBounds().top;
          // Lower indices should be higher for tops so that they are mounted first if possible.
          return lhsTop == rhsTop ? lhs.getIndex() - rhs.getIndex() : lhsTop - rhsTop;
        }
      };

  static final Comparator<RenderTreeNode> sBottomsComparator =
      new Comparator<RenderTreeNode>() {
        @Override
        public int compare(RenderTreeNode l, RenderTreeNode r) {
          LayoutOutput lhs = LayoutOutput.getLayoutOutput(l);
          LayoutOutput rhs = LayoutOutput.getLayoutOutput(r);
          final int lhsBottom = lhs.getBounds().bottom;
          final int rhsBottom = rhs.getBounds().bottom;
          // Lower indices should be lower for bottoms so that they are mounted first if possible.
          return lhsBottom == rhsBottom ? rhs.getIndex() - lhs.getIndex() : lhsBottom - rhsBottom;
        }
      };

  /**
   * Wraps objects which should only be available for the duration of a LayoutState, to access them
   * in other classes such as ComponentContext during layout state calculation. When the layout
   * calculation finishes, all references are nullified. Using a wrapper instead of passing the
   * instances directly helps with clearing out the reference from all objects that hold on to it,
   * without having to keep track of all these objects to clear out the references.
   */
  static final class LayoutStateContext {
    private @Nullable LayoutState mLayoutStateRef;
    private @Nullable LayoutStateFuture mLayoutStateFuture;

    private static @Nullable LayoutState sTestLayoutState;

    public static LayoutStateContext getTestInstance(ComponentContext c) {
      if (sTestLayoutState == null) {
        sTestLayoutState = new LayoutState(c);
      }

      return new LayoutStateContext(sTestLayoutState, null);
    }

    @VisibleForTesting
    LayoutStateContext(LayoutState layoutState) {
      this(layoutState, null);
    }

    @VisibleForTesting
    LayoutStateContext(LayoutState layoutState, @Nullable LayoutStateFuture layoutStateFuture) {
      mLayoutStateRef = layoutState;
      mLayoutStateFuture = layoutStateFuture;
    }

    private void releaseReference() {
      mLayoutStateRef = null;
      mLayoutStateFuture = null;
    }

    /** Returns the LayoutState instance or null if the layout state has been released. */
    @Nullable
    LayoutState getLayoutState() {
      return mLayoutStateRef;
    }

    public @Nullable LayoutStateFuture getLayoutStateFuture() {
      return mLayoutStateFuture;
    }

    boolean isLayoutInterrupted() {
      boolean isInterruptRequested =
          mLayoutStateFuture == null ? false : mLayoutStateFuture.isInterruptRequested();
      boolean isInterruptible = mLayoutStateRef == null ? false : mLayoutStateRef.mIsInterruptible;

      return isInterruptible && isInterruptRequested;
    }

    boolean isLayoutReleased() {
      return mLayoutStateFuture == null ? false : mLayoutStateFuture.isReleased();
    }

    public void markLayoutUninterruptible() {
      if (mLayoutStateRef != null) {
        mLayoutStateRef.mIsInterruptible = false;
      }
    }
  }

  private static final AtomicInteger sIdGenerator = new AtomicInteger(1);
  private static final int NO_PREVIOUS_LAYOUT_STATE_ID = -1;

  private final Map<String, Rect> mComponentKeyToBounds = new HashMap<>();
  private final Map<Handle, Rect> mComponentHandleToBounds = new HashMap<>();
  @Nullable private List<Component> mComponents;

  /**
   * Holds onto how many clashed global keys exist in the tree. Used for automatically generating
   * unique global keys for all sibling components of the same type.
   */
  private @Nullable Map<String, Integer> mGlobalKeysCounter;

  private @Nullable Map<String, Integer> mGlobalManualKeysCounter;

  private final ComponentContext mContext;

  private Component mComponent;

  private int mWidthSpec;
  private int mHeightSpec;

  private final List<RenderTreeNode> mMountableOutputs = new ArrayList<>(8);
  private List<VisibilityOutput> mVisibilityOutputs;
  private final LongSparseArray<Integer> mOutputsIdToPositionMap = new LongSparseArray<>(8);
  private final ArrayList<RenderTreeNode> mMountableOutputTops = new ArrayList<>();
  private final ArrayList<RenderTreeNode> mMountableOutputBottoms = new ArrayList<>();
  private final @Nullable VisibilityModuleInput mVisibilityModuleInput;

  private final @Nullable Map<Integer, InternalNode> mLastMeasuredLayouts;

  @Nullable private LayoutStateOutputIdCalculator mLayoutStateOutputIdCalculator;

  private final List<TestOutput> mTestOutputs;

  @Nullable InternalNode mLayoutRoot;
  @Nullable TransitionId mRootTransitionId;
  @Nullable String mRootComponentName;

  private DiffNode mDiffTreeRoot;

  private int mWidth;
  private int mHeight;

  private int mCurrentX;
  private int mCurrentY;

  private int mCurrentLevel = 0;

  // Holds the current host marker in the layout tree.
  private long mCurrentHostMarker = -1L;
  private int mCurrentHostOutputPosition = -1;

  private boolean mShouldDuplicateParentState = true;
  @NodeInfo.EnabledState private int mParentEnabledState = ENABLED_UNSET;

  private boolean mShouldGenerateDiffTree = false;
  private int mComponentTreeId = -1;
  int mLayoutVersion;
  private final int mId;
  // Id of the layout state (if any) that was used in comparisons with this layout state.
  private int mPreviousLayoutStateId = NO_PREVIOUS_LAYOUT_STATE_ID;
  private boolean mIsCreateLayoutInProgress;

  private AccessibilityManager mAccessibilityManager;
  private boolean mAccessibilityEnabled = false;

  private StateHandler mStateHandler;
  private List<Component> mComponentsNeedingPreviousRenderData;
  @Nullable private TransitionId mCurrentTransitionId;
  @Nullable private OutputUnitsAffinityGroup<LayoutOutput> mCurrentLayoutOutputAffinityGroup;
  private final Map<TransitionId, OutputUnitsAffinityGroup<LayoutOutput>> mTransitionIdMapping =
      new LinkedHashMap<>();
  private final Set<TransitionId> mDuplicatedTransitionIds = new HashSet<>();
  private List<Transition> mTransitions;
  private final int mOrientation;
  // If true, the LayoutState calculate call was interrupted and will need to be resumed to finish
  // creating and measuring the InternalNode of the LayoutState.
  private volatile boolean mIsPartialLayoutState;
  private volatile boolean mIsInterruptible = true;

  private static final Object debugLock = new Object();
  @Nullable private static Map<Integer, List<Boolean>> layoutCalculationsOnMainThread;

  @Nullable WorkingRangeContainer mWorkingRangeContainer;

  /** A container stores components whose OnAttached delegate methods are about to be executed. */
  @Nullable private Map<String, Component> mAttachableContainer;

  final boolean mShouldDisableDrawableOutputs =
      ComponentsConfiguration.shouldDisableDrawableOutputs;

  LayoutState(ComponentContext context) {
    mContext = context;
    mId = sIdGenerator.getAndIncrement();
    mStateHandler = mContext.getStateHandler();
    mTestOutputs = ComponentsConfiguration.isEndToEndTestRun ? new ArrayList<TestOutput>(8) : null;
    mOrientation = context.getResources().getConfiguration().orientation;
    mLastMeasuredLayouts = new HashMap<>();
    mComponents = new ArrayList<>();

    if (context.getComponentTree() != null) {
      mIncrementalVisibility = context.getComponentTree().hasIncrementalVisibility();
    } else {
      mIncrementalVisibility = false;
    }

    mVisibilityModuleInput = mIncrementalVisibility ? new VisibilityModuleInput() : null;
    mVisibilityOutputs = new ArrayList<>(8);
  }

  @Override
  public boolean isIncrementalVisibilityEnabled() {
    return mIncrementalVisibility;
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

  /**
   * Acquires a new layout output for the internal node and its associated component. It returns
   * null if there's no component associated with the node as the mount pass only cares about nodes
   * that will potentially mount content into the component host.
   */
  @Nullable
  private static LayoutOutput createGenericLayoutOutput(
      InternalNode node,
      LayoutState layoutState,
      @Nullable DebugHierarchy.Node hierarchy,
      boolean hasHostView) {
    final Component component = node.getTailComponent();

    // Skip empty nodes and layout specs because they don't mount anything.
    if (component == null || component.getMountType() == NONE) {
      return null;
    }

    // The mount operation will need both the marker for the target host and its matching
    // parent host to ensure the correct hierarchy when nesting the host views.
    long hostMarker = layoutState.mCurrentHostMarker;

    return createLayoutOutput(
        component,
        hostMarker,
        layoutState,
        node,
        true /* useNodePadding */,
        node.getImportantForAccessibility(),
        layoutState.mShouldDuplicateParentState,
        hasHostView);
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

  private static LayoutOutput createHostLayoutOutput(LayoutState layoutState, InternalNode node) {

    final HostComponent hostComponent = HostComponent.create(node.getContext());

    // We need to pass common dynamic props to the host component, as they only could be applied to
    // views, so we'll need to set them up, when binding HostComponent to ComponentHost. At the same
    // time, we don't remove them from the current component, as we may calculate multiple
    // LayoutStates using same Components
    hostComponent.setCommonDynamicProps(mergeCommonDynamicProps(node.getComponents()));

    long hostMarker =
        layoutState.isLayoutRoot(node) ? ROOT_HOST_ID : layoutState.mCurrentHostMarker;

    final LayoutOutput hostOutput =
        createLayoutOutput(
            hostComponent,
            hostMarker,
            layoutState,
            node,
            false /* useNodePadding */,
            node.getImportantForAccessibility(),
            node.isDuplicateParentStateEnabled(),
            false);

    ViewNodeInfo viewNodeInfo = hostOutput.getViewNodeInfo();
    if (viewNodeInfo != null) {
      if (node.hasStateListAnimatorResSet()) {
        viewNodeInfo.setStateListAnimatorRes(node.getStateListAnimatorRes());
      } else {
        viewNodeInfo.setStateListAnimator(node.getStateListAnimator());
      }
    }

    return hostOutput;
  }

  private static LayoutOutput createDrawableLayoutOutput(
      Component component, LayoutState layoutState, InternalNode node, boolean hasHostView) {
    // The mount operation will need both the marker for the target host and its matching
    // parent host to ensure the correct hierarchy when nesting the host views.
    long hostMarker = layoutState.mCurrentHostMarker;

    return createLayoutOutput(
        component,
        hostMarker,
        layoutState,
        node,
        false /* useNodePadding */,
        IMPORTANT_FOR_ACCESSIBILITY_NO,
        layoutState.mShouldDuplicateParentState,
        hasHostView);
  }

  private static LayoutOutput createLayoutOutput(
      Component component,
      long hostMarker,
      LayoutState layoutState,
      InternalNode node,
      boolean useNodePadding,
      int importantForAccessibility,
      boolean duplicateParentState,
      boolean hasHostView) {
    final boolean isMountViewSpec = isMountViewSpec(component);

    final int hostTranslationX;
    final int hostTranslationY;
    if (layoutState.mCurrentHostOutputPosition >= 0) {
      final RenderTreeNode hostOutput =
          layoutState.mMountableOutputs.get(layoutState.mCurrentHostOutputPosition);

      final Rect hostBounds = hostOutput.getBounds();
      hostTranslationX = hostBounds.left;
      hostTranslationY = hostBounds.top;
    } else {
      hostTranslationX = 0;
      hostTranslationY = 0;
    }

    int flags = 0;

    int l = layoutState.mCurrentX + node.getX();
    int t = layoutState.mCurrentY + node.getY();
    int r = l + node.getWidth();
    int b = t + node.getHeight();

    final int paddingLeft = useNodePadding ? node.getPaddingLeft() : 0;
    final int paddingTop = useNodePadding ? node.getPaddingTop() : 0;
    final int paddingRight = useNodePadding ? node.getPaddingRight() : 0;
    final int paddingBottom = useNodePadding ? node.getPaddingBottom() : 0;

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
      if (layoutState.mShouldDisableDrawableOutputs) {
        viewNodeInfo.setBackground(node.getBackground());
        if (SDK_INT >= M) {
          viewNodeInfo.setForeground(node.getForeground());
        }
      }
      if (useNodePadding && node.isPaddingSet()) {
        viewNodeInfo.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
      }
      viewNodeInfo.setLayoutDirection(node.getResolvedLayoutDirection());
      viewNodeInfo.setExpandedTouchBounds(
          node,
          l - hostTranslationX,
          t - hostTranslationY,
          r - hostTranslationX,
          b - hostTranslationY);
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

    return new LayoutOutput(
        layoutOutputNodeInfo,
        layoutOutputViewNodeInfo,
        component,
        bounds,
        hostTranslationX,
        hostTranslationY,
        flags,
        hostMarker,
        importantForAccessibility,
        layoutState.mOrientation,
        transitionId);
  }

  /**
   * Acquires a {@link VisibilityOutput} object and computes the bounds for it using the information
   * stored in the {@link InternalNode}.
   */
  private static VisibilityOutput createVisibilityOutput(
      InternalNode node, LayoutState layoutState) {

    final int l = layoutState.mCurrentX + node.getX();
    final int t = layoutState.mCurrentY + node.getY();
    final int r = l + node.getWidth();
    final int b = t + node.getHeight();

    final EventHandler<VisibleEvent> visibleHandler = node.getVisibleHandler();
    final EventHandler<FocusedVisibleEvent> focusedHandler = node.getFocusedHandler();
    final EventHandler<UnfocusedVisibleEvent> unfocusedHandler = node.getUnfocusedHandler();
    final EventHandler<FullImpressionVisibleEvent> fullImpressionHandler =
        node.getFullImpressionHandler();
    final EventHandler<InvisibleEvent> invisibleHandler = node.getInvisibleHandler();
    final EventHandler<VisibilityChangedEvent> visibleRectChangedEventHandler =
        node.getVisibilityChangedHandler();
    final VisibilityOutput visibilityOutput = new VisibilityOutput();

    visibilityOutput.setComponent(node.getTailComponent());
    visibilityOutput.setBounds(l, t, r, b);
    visibilityOutput.setVisibleHeightRatio(node.getVisibleHeightRatio());
    visibilityOutput.setVisibleWidthRatio(node.getVisibleWidthRatio());
    visibilityOutput.setVisibleEventHandler(visibleHandler);
    visibilityOutput.setFocusedEventHandler(focusedHandler);
    visibilityOutput.setUnfocusedEventHandler(unfocusedHandler);
    visibilityOutput.setFullImpressionEventHandler(fullImpressionHandler);
    visibilityOutput.setInvisibleEventHandler(invisibleHandler);
    visibilityOutput.setVisibilityChangedEventHandler(visibleRectChangedEventHandler);

    return visibilityOutput;
  }

  private static TestOutput createTestOutput(
      InternalNode node, LayoutState layoutState, LayoutOutput layoutOutput) {
    final int l = layoutState.mCurrentX + node.getX();
    final int t = layoutState.mCurrentY + node.getY();
    final int r = l + node.getWidth();
    final int b = t + node.getHeight();

    final TestOutput output = new TestOutput();
    output.setTestKey(node.getTestKey());
    output.setBounds(l, t, r, b);
    output.setHostMarker(layoutState.mCurrentHostMarker);
    if (layoutOutput != null) {
      output.setLayoutOutputId(layoutOutput.getId());
    }

    return output;
  }

  /**
   * Determine if a given {@link InternalNode} within the context of a given {@link LayoutState}
   * requires to be wrapped inside a view.
   *
   * @see #needsHostView(InternalNode, LayoutState)
   */
  private static boolean hasViewContent(InternalNode node, LayoutState layoutState) {
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

    final boolean hasFocusChangeHandler = (nodeInfo != null && nodeInfo.hasFocusChangeHandler());
    final boolean hasEnabledTouchEventHandlers =
        nodeInfo != null
            && nodeInfo.hasTouchEventHandlers()
            && nodeInfo.getEnabledState() != ENABLED_SET_FALSE;
    final boolean hasViewTag = (nodeInfo != null && nodeInfo.getViewTag() != null);
    final boolean hasViewTags = (nodeInfo != null && nodeInfo.getViewTags() != null);
    final boolean hasShadowElevation = (nodeInfo != null && nodeInfo.getShadowElevation() != 0);
    final boolean hasOutlineProvider = (nodeInfo != null && nodeInfo.getOutlineProvider() != null);
    final boolean hasClipToOutline = (nodeInfo != null && nodeInfo.getClipToOutline());
    final boolean isFocusableSetTrue =
        (nodeInfo != null && nodeInfo.getFocusState() == FOCUS_SET_TRUE);
    final boolean isClickableSetTrue =
        (nodeInfo != null && nodeInfo.getClickableState() == CLICKABLE_SET_TRUE);
    final boolean hasClipChildrenSet = (nodeInfo != null && nodeInfo.isClipChildrenSet());

    return hasBackgroundOrForeground
        || hasFocusChangeHandler
        || hasEnabledTouchEventHandlers
        || hasViewTag
        || hasViewTags
        || hasShadowElevation
        || hasOutlineProvider
        || hasClipToOutline
        || hasClipChildrenSet
        || hasAccessibilityContent
        || isFocusableSetTrue
        || isClickableSetTrue;
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
   * @param parent
   * @param parentContext the parent component context
   * @param parentHierarchy The parent hierarchy linked list or null.
   * @param node InternalNode to process.
   * @param layoutState the LayoutState currently operating.
   * @param parentDiffNode whether this method also populates the diff tree and assigns the root
   */
  private static void collectResults(
      @Nullable RenderTreeNode parent,
      ComponentContext parentContext,
      @Nullable DebugHierarchy.Node parentHierarchy,
      InternalNode node,
      LayoutState layoutState,
      DiffNode parentDiffNode) {
    if (parentContext.wasLayoutCanceled()) {
      return;
    }

    if (node.hasNewLayout()) {
      node.markLayoutSeen();
    }
    final Component component = node.getTailComponent();
    final boolean isTracing = ComponentsSystrace.isTracing();

    final DebugHierarchy.Node hierarchy;
    // Update the hierarchy if we are tracking it.
    if (ComponentsConfiguration.isDebugHierarchyEnabled) {
      hierarchy = DebugHierarchy.newNode(parentHierarchy, component, node.getComponents());
    } else {
      hierarchy = null;
    }

    // Early return if collecting results of a node holding a nested tree.
    if (node.isNestedTreeHolder()) {
      // If the nested tree is defined, it has been resolved during a measure call during
      // layout calculation.
      if (isTracing) {
        ComponentsSystrace.beginSectionWithArgs("resolveNestedTree:" + node.getSimpleName())
            .arg("widthSpec", "EXACTLY " + node.getWidth())
            .arg("heightSpec", "EXACTLY " + node.getHeight())
            .arg("rootComponentId", node.getTailComponent().getId())
            .flush();
      }
      InternalNode nestedTree =
          Layout.create(
              parentContext,
              node,
              SizeSpec.makeSizeSpec(node.getWidth(), EXACTLY),
              SizeSpec.makeSizeSpec(node.getHeight(), EXACTLY));
      if (isTracing) {
        ComponentsSystrace.endSection();
      }

      if (nestedTree == NULL_LAYOUT) {
        return;
      }

      // Account for position of the holder node.
      layoutState.mCurrentX += node.getX();
      layoutState.mCurrentY += node.getY();

      collectResults(parent, parentContext, hierarchy, nestedTree, layoutState, parentDiffNode);

      layoutState.mCurrentX -= node.getX();
      layoutState.mCurrentY -= node.getY();

      return;
    }

    // IMPORTANT_FOR_ACCESSIBILITY_YES_HIDE_DESCENDANTS sets node to YES and children to
    // NO_HIDE_DESCENDANTS
    if (node.getImportantForAccessibility()
        == ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_YES_HIDE_DESCENDANTS) {
      node.importantForAccessibility(ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_YES);
      for (int i = 0, size = node.getChildCount(); i < size; i++) {
        node.getChildAt(i)
            .importantForAccessibility(
                ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
      }
    }

    final boolean shouldGenerateDiffTree = layoutState.mShouldGenerateDiffTree;
    final DiffNode currentDiffNode = node.getDiffNode();
    final boolean shouldUseCachedOutputs = isMountSpec(component) && currentDiffNode != null;
    final boolean isCachedOutputUpdated = shouldUseCachedOutputs && node.areCachedMeasuresValid();

    final DiffNode diffNode;
    if (shouldGenerateDiffTree) {
      if (ComponentsConfiguration.useInternalNodesForLayoutDiffing) {
        diffNode = node;
      } else {
        diffNode = createDiffNode(node, parentDiffNode);
      }
      if (parentDiffNode == null) {
        layoutState.mDiffTreeRoot = diffNode;
      }
    } else {
      diffNode = null;
    }

    // If the parent of this node is disabled, this node has to be disabled too.
    if (layoutState.mParentEnabledState == ENABLED_SET_FALSE) {
      node.getOrCreateNodeInfo().setEnabled(false);
    }

    final boolean needsHostView = needsHostView(node, layoutState);

    final long currentHostMarker = layoutState.mCurrentHostMarker;
    final int currentHostOutputPosition = layoutState.mCurrentHostOutputPosition;

    final TransitionId currentTransitionId = layoutState.mCurrentTransitionId;
    final OutputUnitsAffinityGroup<LayoutOutput> currentLayoutOutputAffinityGroup =
        layoutState.mCurrentLayoutOutputAffinityGroup;

    layoutState.mCurrentTransitionId = getTransitionIdForNode(node);
    layoutState.mCurrentLayoutOutputAffinityGroup =
        layoutState.mCurrentTransitionId != null
            ? new OutputUnitsAffinityGroup<LayoutOutput>()
            : null;

    // 1. Insert a host LayoutOutput if we have some interactive content to be attached to.
    if (needsHostView) {
      final int hostLayoutPosition =
          addHostLayoutOutput(parent, node, layoutState, diffNode, hierarchy);
      addCurrentAffinityGroupToTransitionMapping(layoutState);

      parent = layoutState.mMountableOutputs.get(hostLayoutPosition);
      final LayoutOutput output = LayoutOutput.getLayoutOutput(parent);

      layoutState.mCurrentLevel++;
      layoutState.mCurrentHostMarker = output.getId();
      layoutState.mCurrentHostOutputPosition = hostLayoutPosition;
    }

    // We need to take into account flattening when setting duplicate parent state. The parent after
    // flattening may no longer exist. Therefore the value of duplicate parent state should only be
    // true if the path between us (inclusive) and our inner/root host (exclusive) all are
    // duplicate parent state.
    final boolean shouldDuplicateParentState = layoutState.mShouldDuplicateParentState;
    layoutState.mShouldDuplicateParentState =
        needsHostView || (shouldDuplicateParentState && node.isDuplicateParentStateEnabled());

    // Generate the layoutOutput for the given node.
    final LayoutOutput layoutOutput =
        createGenericLayoutOutput(node, layoutState, hierarchy, needsHostView);

    if (layoutOutput != null) {
      final long previousId =
          shouldUseCachedOutputs && currentDiffNode.getContentOutput() != null
              ? currentDiffNode.getContentOutput().getId()
              : -1;
      layoutState.calculateAndSetLayoutOutputIdAndUpdateState(
          layoutOutput,
          layoutState.mCurrentLevel,
          OutputUnitType.CONTENT,
          previousId,
          isCachedOutputUpdated,
          hierarchy);
    }

    // 2. Add background if defined.
    if (!layoutState.mShouldDisableDrawableOutputs) {
      final Drawable background = node.getBackground();
      if (background != null) {
        if (layoutOutput != null && layoutOutput.getViewNodeInfo() != null) {
          layoutOutput.getViewNodeInfo().setBackground(background);
        } else {
          final LayoutOutput convertBackground =
              (currentDiffNode != null) ? currentDiffNode.getBackgroundOutput() : null;

          final LayoutOutput backgroundOutput =
              addDrawableComponent(
                  parent,
                  node,
                  layoutState,
                  convertBackground,
                  hierarchy,
                  background,
                  OutputUnitType.BACKGROUND,
                  needsHostView);

          if (diffNode != null) {
            diffNode.setBackgroundOutput(backgroundOutput);
          }
        }
      }
    }

    // 3. Now add the MountSpec (either View or Drawable) to the Outputs.
    if (isMountSpec(component)) {
      // Notify component about its final size.
      if (isTracing) {
        ComponentsSystrace.beginSection("onBoundsDefined:" + node.getSimpleName());
      }
      component.onBoundsDefined(component.getScopedContext(), node);
      if (isTracing) {
        ComponentsSystrace.endSection();
      }

      addMountableOutput(layoutState, layoutOutput, parent);
      addLayoutOutputIdToPositionsMap(
          layoutState.mOutputsIdToPositionMap,
          layoutOutput,
          layoutState.mMountableOutputs.size() - 1);
      maybeAddLayoutOutputToAffinityGroup(
          layoutState.mCurrentLayoutOutputAffinityGroup, OutputUnitType.CONTENT, layoutOutput);

      if (diffNode != null) {
        diffNode.setContentOutput(layoutOutput);
      }
    }

    // 4. Extract the Transitions.
    if (Layout.areTransitionsEnabled(component != null ? component.getScopedContext() : null)) {
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

      final ArrayList<Component> componentsNeedingPreviousRenderData =
          node.getComponentsNeedingPreviousRenderData();
      if (componentsNeedingPreviousRenderData != null) {
        if (layoutState.mComponentsNeedingPreviousRenderData == null) {
          layoutState.mComponentsNeedingPreviousRenderData = new ArrayList<>();
        }
        // We'll check for animations in mount
        layoutState.mComponentsNeedingPreviousRenderData.addAll(
            componentsNeedingPreviousRenderData);
      }
    }

    layoutState.mCurrentX += node.getX();
    layoutState.mCurrentY += node.getY();
    @NodeInfo.EnabledState final int parentEnabledState = layoutState.mParentEnabledState;
    layoutState.mParentEnabledState =
        (node.getNodeInfo() != null) ? node.getNodeInfo().getEnabledState() : ENABLED_UNSET;

    // We must process the nodes in order so that the layout state output order is correct.
    for (int i = 0, size = node.getChildCount(); i < size; i++) {
      collectResults(
          parent, node.getContext(), hierarchy, node.getChildAt(i), layoutState, diffNode);
    }

    layoutState.mParentEnabledState = parentEnabledState;
    layoutState.mCurrentX -= node.getX();
    layoutState.mCurrentY -= node.getY();

    // 5. Add border color if defined.
    if (node.shouldDrawBorders()) {
      final LayoutOutput convertBorder =
          (currentDiffNode != null) ? currentDiffNode.getBorderOutput() : null;
      final LayoutOutput borderOutput =
          addDrawableComponent(
              parent,
              node,
              layoutState,
              convertBorder,
              hierarchy,
              getBorderColorDrawable(node),
              OutputUnitType.BORDER,
              needsHostView);
      if (diffNode != null) {
        diffNode.setBorderOutput(borderOutput);
      }
    }

    // 6. Add foreground if defined.
    if (!layoutState.mShouldDisableDrawableOutputs) {
      final Drawable foreground = node.getForeground();
      if (foreground != null) {
        if (layoutOutput != null && layoutOutput.getViewNodeInfo() != null && SDK_INT >= M) {
          layoutOutput.getViewNodeInfo().setForeground(foreground);
        } else {
          final LayoutOutput convertForeground =
              (currentDiffNode != null) ? currentDiffNode.getForegroundOutput() : null;

          final LayoutOutput foregroundOutput =
              addDrawableComponent(
                  parent,
                  node,
                  layoutState,
                  convertForeground,
                  hierarchy,
                  foreground,
                  OutputUnitType.FOREGROUND,
                  needsHostView);

          if (diffNode != null) {
            diffNode.setForegroundOutput(foregroundOutput);
          }
        }
      }
    }

    // 7. Add VisibilityOutputs if any visibility-related event handlers are present.
    if (node.hasVisibilityHandlers()) {
      final VisibilityOutput visibilityOutput = createVisibilityOutput(node, layoutState);

      layoutState.mVisibilityOutputs.add(visibilityOutput);

      if (diffNode != null) {
        diffNode.setVisibilityOutput(visibilityOutput);
      }
    }

    // 8. If we're in a testing environment, maintain an additional data structure with
    // information about nodes that we can query later.
    if (layoutState.mTestOutputs != null && !TextUtils.isEmpty(node.getTestKey())) {
      final TestOutput testOutput = createTestOutput(node, layoutState, layoutOutput);
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
            registration.mName, registration.mWorkingRange, registration.mComponent);
      }
    }

    if (component != null) {
      final Rect rect = new Rect();
      if (layoutOutput != null) {
        rect.set(layoutOutput.getBounds());
      } else {
        rect.left = layoutState.mCurrentX + node.getX();
        rect.top = layoutState.mCurrentY + node.getY();
        rect.right = rect.left + node.getWidth();
        rect.bottom = rect.top + node.getHeight();
      }

      for (Component delegate : node.getComponents()) {
        final Rect copyRect = new Rect();
        copyRect.set(rect);

        // Keep a list of the components we created during this layout calculation. If the layout is
        // valid, the ComponentTree will update the event handlers that have been created in the
        // previous ComponentTree with the new component dispatched, otherwise Section children
        // might not be accessing the correct props and state on the event handlers. The null
        // checkers cover tests, the scope and tree should not be null at this point of the layout
        // calculation.
        if (delegate.getScopedContext() != null
            && delegate.getScopedContext().getComponentTree() != null) {
          if (layoutState.mComponents != null) {
            layoutState.mComponents.add(delegate);
          }
          if (delegate.hasAttachDetachCallback()) {
            if (layoutState.mAttachableContainer == null) {
              layoutState.mAttachableContainer = new HashMap<>();
            }
            layoutState.mAttachableContainer.put(delegate.getGlobalKey(), delegate);
          }
        }
        if (delegate.getGlobalKey() != null) {
          layoutState.mComponentKeyToBounds.put(delegate.getGlobalKey(), copyRect);
        }
        if (delegate.hasHandle()) {
          layoutState.mComponentHandleToBounds.put(delegate.getHandle(), copyRect);
        }
      }
    }

    // 10. If enabled, show a debug foreground layer covering the whole LithoView showing which
    // thread the LayoutState was calculated into and number of calculations for given node.
    if (ComponentsConfiguration.enableLithoViewDebugOverlay) {
      if (layoutState.isLayoutRoot(node)) {
        ArrayList<Boolean> mainThreadCalculations;
        int layoutId = layoutState.getComponentTreeId();

        synchronized (debugLock) {
          if (layoutCalculationsOnMainThread == null) {
            layoutCalculationsOnMainThread = new HashMap<>();
          }
          List<Boolean> calculationsOnMainThread = layoutCalculationsOnMainThread.get(layoutId);
          if (calculationsOnMainThread == null) {
            calculationsOnMainThread = new ArrayList<>();
          }
          calculationsOnMainThread.add(ThreadUtils.isMainThread());
          layoutCalculationsOnMainThread.put(layoutId, calculationsOnMainThread);
          mainThreadCalculations = new ArrayList<>(calculationsOnMainThread);
        }

        addDrawableComponent(
            parent,
            node,
            layoutState,
            null,
            hierarchy,
            new DebugOverlayDrawable(mainThreadCalculations),
            OutputUnitType.FOREGROUND,
            needsHostView);
      }
    } else if (layoutCalculationsOnMainThread != null) {
      synchronized (debugLock) {
        layoutCalculationsOnMainThread = null;
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
  Map<String, Component> consumeAttachables() {
    @Nullable Map<String, Component> tmp = mAttachableContainer;
    mAttachableContainer = null;
    return tmp;
  }

  private static void calculateAndSetHostOutputIdAndUpdateState(
      InternalNode node,
      LayoutOutput hostOutput,
      LayoutState layoutState,
      @Nullable DebugHierarchy.Node hierarchy) {
    if (layoutState.isLayoutRoot(node)) {
      // The root host (LithoView) always has ID 0 and is unconditionally
      // set as dirty i.e. no need to use shouldComponentUpdate().
      hostOutput.setId(ROOT_HOST_ID);
      if (hierarchy != null) {
        hostOutput.setHierarchy(hierarchy.mutateType(OutputUnitType.HOST));
      }

      hostOutput.setUpdateState(LayoutOutput.STATE_DIRTY);
    } else {
      layoutState.calculateAndSetLayoutOutputIdAndUpdateState(
          hostOutput, layoutState.mCurrentLevel, OutputUnitType.HOST, -1, false, hierarchy);
    }
  }

  private static LayoutOutput addDrawableComponent(
      final @Nullable RenderTreeNode parent,
      InternalNode node,
      LayoutState layoutState,
      @Nullable LayoutOutput recycle,
      @Nullable DebugHierarchy.Node hierarchy,
      Drawable drawable,
      @OutputUnitType int type,
      boolean matchHostBoundsTransitions) {
    final Component drawableComponent = DrawableComponent.create(drawable);
    drawableComponent.setScopedContext(
        ComponentContext.withComponentScope(node.getContext(), drawableComponent));
    final boolean isOutputUpdated;
    if (recycle != null) {
      isOutputUpdated =
          !drawableComponent.shouldComponentUpdate(recycle.getComponent(), drawableComponent);
    } else {
      isOutputUpdated = false;
    }

    final long previousId = recycle != null ? recycle.getId() : -1;
    final LayoutOutput output =
        addDrawableLayoutOutput(
            parent,
            drawableComponent,
            layoutState,
            hierarchy,
            node,
            type,
            previousId,
            isOutputUpdated,
            matchHostBoundsTransitions);

    maybeAddLayoutOutputToAffinityGroup(
        layoutState.mCurrentLayoutOutputAffinityGroup, type, output);

    return output;
  }

  private static Drawable getBorderColorDrawable(InternalNode node) {
    if (!node.shouldDrawBorders()) {
      throw new RuntimeException("This node does not support drawing border color");
    }

    final boolean isRtl = node.recursivelyResolveLayoutDirection() == YogaDirection.RTL;
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
        .borderLeftWidth(node.getLayoutBorder(leftEdge))
        .borderTopWidth(node.getLayoutBorder(YogaEdge.TOP))
        .borderRightWidth(node.getLayoutBorder(rightEdge))
        .borderBottomWidth(node.getLayoutBorder(YogaEdge.BOTTOM))
        .borderRadius(borderRadius)
        .build();
  }

  private static void addLayoutOutputIdToPositionsMap(
      LongSparseArray outputsIdToPositionMap, LayoutOutput layoutOutput, int position) {
    if (outputsIdToPositionMap != null) {
      outputsIdToPositionMap.put(layoutOutput.getId(), position);
    }
  }

  private static void maybeAddLayoutOutputToAffinityGroup(
      OutputUnitsAffinityGroup<LayoutOutput> group,
      @OutputUnitType int outputType,
      LayoutOutput layoutOutput) {
    if (group != null) {
      group.add(outputType, layoutOutput);
    }
  }

  private static void addCurrentAffinityGroupToTransitionMapping(LayoutState layoutState) {
    final OutputUnitsAffinityGroup<LayoutOutput> group =
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

  private static LayoutOutput addDrawableLayoutOutput(
      final @Nullable RenderTreeNode parent,
      Component drawableComponent,
      LayoutState layoutState,
      @Nullable DebugHierarchy.Node hierarchy,
      InternalNode node,
      @OutputUnitType int outputType,
      long previousId,
      boolean isCachedOutputUpdated,
      boolean matchHostBoundsTransitions) {

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("onBoundsDefined:" + node.getSimpleName());
    }
    drawableComponent.onBoundsDefined(layoutState.mContext, node);
    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    final LayoutOutput drawableLayoutOutput =
        createDrawableLayoutOutput(
            drawableComponent, layoutState, node, matchHostBoundsTransitions);

    layoutState.calculateAndSetLayoutOutputIdAndUpdateState(
        drawableLayoutOutput,
        layoutState.mCurrentLevel,
        outputType,
        previousId,
        isCachedOutputUpdated,
        hierarchy);

    addMountableOutput(layoutState, drawableLayoutOutput, parent);
    addLayoutOutputIdToPositionsMap(
        layoutState.mOutputsIdToPositionMap,
        drawableLayoutOutput,
        layoutState.mMountableOutputs.size() - 1);

    return drawableLayoutOutput;
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
      InternalNode node,
      LayoutState layoutState,
      DiffNode diffNode,
      @Nullable DebugHierarchy.Node hierarchy) {
    final Component component = node.getTailComponent();

    // Only the root host is allowed to wrap view mount specs as a layout output
    // is unconditionally added for it.
    if (isMountViewSpec(component) && !layoutState.isLayoutRoot(node)) {
      throw new IllegalArgumentException("We shouldn't insert a host as a parent of a View");
    }

    final LayoutOutput hostLayoutOutput = createHostLayoutOutput(layoutState, node);

    if (diffNode != null) {
      diffNode.setHostOutput(hostLayoutOutput);
    }

    calculateAndSetHostOutputIdAndUpdateState(node, hostLayoutOutput, layoutState, hierarchy);

    // The component of the hostLayoutOutput will be set later after all the
    // children got processed.
    addMountableOutput(layoutState, hostLayoutOutput, parent);

    final int hostOutputPosition = layoutState.mMountableOutputs.size() - 1;

    addLayoutOutputIdToPositionsMap(
        layoutState.mOutputsIdToPositionMap, hostLayoutOutput, hostOutputPosition);

    maybeAddLayoutOutputToAffinityGroup(
        layoutState.mCurrentLayoutOutputAffinityGroup, OutputUnitType.HOST, hostLayoutOutput);

    return hostOutputPosition;
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
                  .append(sourceToString(source))
                  .toString())
          .arg("treeId", componentTreeId)
          .arg("rootId", component.getId())
          .arg("widthSpec", SizeSpec.toString(widthSpec))
          .arg("heightSpec", SizeSpec.toString(heightSpec))
          .flush();
    }

    final DiffNode diffTreeRoot =
        currentLayoutState != null ? currentLayoutState.mDiffTreeRoot : null;
    final LayoutState layoutState;
    LayoutStateContext layoutStateContext = null;

    try {
      final PerfEvent logLayoutState =
          logger != null
              ? LogTreePopulator.populatePerfEventFromLogger(
                  c, logger, logger.newPerformanceEvent(c, EVENT_CALCULATE_LAYOUT_STATE))
              : null;
      if (logLayoutState != null) {
        logLayoutState.markerAnnotate(PARAM_COMPONENT, component.getSimpleName());
        logLayoutState.markerAnnotate(PARAM_LAYOUT_STATE_SOURCE, sourceToString(source));
        logLayoutState.markerAnnotate(PARAM_IS_BACKGROUND_LAYOUT, !ThreadUtils.isMainThread());
        logLayoutState.markerAnnotate(PARAM_TREE_DIFF_ENABLED, diffTreeRoot != null);
        logLayoutState.markerAnnotate(PARAM_ATTRIBUTION, extraAttribution);
      }

      // Detect errors internal to components
      component.markLayoutStarted();

      layoutState = new LayoutState(c);
      layoutStateContext = new LayoutStateContext(layoutState, layoutStateFuture);
      c.setLayoutStateContext(layoutStateContext);

      layoutState.mShouldGenerateDiffTree = shouldGenerateDiffTree;
      layoutState.mComponentTreeId = componentTreeId;
      layoutState.mLayoutVersion = layoutVersion;
      layoutState.mPreviousLayoutStateId =
          currentLayoutState != null ? currentLayoutState.mId : NO_PREVIOUS_LAYOUT_STATE_ID;
      layoutState.mAccessibilityManager =
          (AccessibilityManager) c.getAndroidContext().getSystemService(ACCESSIBILITY_SERVICE);
      layoutState.mAccessibilityEnabled =
          AccessibilityUtils.isAccessibilityEnabled(layoutState.mAccessibilityManager);
      layoutState.mComponent = component;
      layoutState.mWidthSpec = widthSpec;
      layoutState.mHeightSpec = heightSpec;
      layoutState.mRootComponentName = component.getSimpleName();
      layoutState.mIsCreateLayoutInProgress = true;

      final InternalNode layoutCreatedInWillRender = component.consumeLayoutCreatedInWillRender();

      final boolean isReconcilable = isReconcilable(c, component, currentLayoutState);

      // Release the current InternalNode tree if it is not reconcilable.
      if (!isReconcilable && currentLayoutState != null) {
        currentLayoutState.mLayoutRoot = null;
      }

      final InternalNode root =
          layoutCreatedInWillRender == null
              ? Layout.createAndMeasureComponent(
                  c,
                  component,
                  widthSpec,
                  heightSpec,
                  isReconcilable ? currentLayoutState.mLayoutRoot : null,
                  diffTreeRoot,
                  logLayoutState)
              : layoutCreatedInWillRender;
      // Null check for tests.
      if (root.getContext() != null) {
        root.getContext().setLayoutStateContext(layoutStateContext);
      }

      layoutState.mLayoutRoot = root;
      layoutState.mRootTransitionId = getTransitionIdForNode(root);
      layoutState.mIsCreateLayoutInProgress = false;

      if (layoutStateContext.isLayoutInterrupted()) {
        layoutState.mIsPartialLayoutState = true;
        if (logLayoutState != null) {
          logger.logPerfEvent(logLayoutState);
        }
        return layoutState;
      }

      if (logLayoutState != null) {
        logLayoutState.markerPoint("start_collect_results");
      }

      setSizeAfterMeasureAndCollectResults(c, layoutState);

      if (layoutStateContext != null) {
        layoutStateContext.releaseReference();
      }

      if (logLayoutState != null) {
        logLayoutState.markerPoint("end_collect_results");
        logger.logPerfEvent(logLayoutState);
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

    final LayoutStateContext layoutStateContext = new LayoutStateContext(layoutState, null);
    c.setLayoutStateContext(layoutStateContext);

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
                  .append(sourceToString(source))
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
        logLayoutState.markerAnnotate(PARAM_LAYOUT_STATE_SOURCE, sourceToString(source));
      }

      // If we already have a LayoutState but the InternalNode is only partially resolved,
      // resume resolving the InternalNode and measure it.
      Layout.resumeCreateAndMeasureComponent(
          c,
          layoutState.mLayoutRoot,
          widthSpec,
          heightSpec,
          layoutState.mDiffTreeRoot,
          logLayoutState);

      setSizeAfterMeasureAndCollectResults(c, layoutState);

      layoutStateContext.releaseReference();

      if (logLayoutState != null) {
        logger.logPerfEvent(logLayoutState);
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
    RenderTreeNode root = mMountableOutputs.get(0);
    RenderTreeNode[] flatList = new RenderTreeNode[mMountableOutputs.size()];
    for (int i = 0, size = mMountableOutputs.size(); i < size; i++) {
      flatList[i] = mMountableOutputs.get(i);
    }

    return new RenderTree(root, flatList, mWidthSpec, mHeightSpec);
  }

  private static void setSizeAfterMeasureAndCollectResults(
      ComponentContext c, LayoutState layoutState) {
    if (c.wasLayoutCanceled()) {
      return;
    }

    final boolean isTracing = ComponentsSystrace.isTracing();
    final int widthSpec = layoutState.mWidthSpec;
    final int heightSpec = layoutState.mHeightSpec;
    final InternalNode root = layoutState.mLayoutRoot;

    switch (SizeSpec.getMode(widthSpec)) {
      case SizeSpec.EXACTLY:
        layoutState.mWidth = SizeSpec.getSize(widthSpec);
        break;
      case SizeSpec.AT_MOST:
        layoutState.mWidth = Math.min(root.getWidth(), SizeSpec.getSize(widthSpec));
        break;
      case SizeSpec.UNSPECIFIED:
        layoutState.mWidth = root.getWidth();
        break;
    }

    switch (SizeSpec.getMode(heightSpec)) {
      case SizeSpec.EXACTLY:
        layoutState.mHeight = SizeSpec.getSize(heightSpec);
        break;
      case SizeSpec.AT_MOST:
        layoutState.mHeight = Math.min(root.getHeight(), SizeSpec.getSize(heightSpec));
        break;
      case SizeSpec.UNSPECIFIED:
        layoutState.mHeight = root.getHeight();
        break;
    }

    layoutState.clearLayoutStateOutputIdCalculator();

    // Reset markers before collecting layout outputs.
    layoutState.mCurrentHostMarker = -1;

    if (root == NULL_LAYOUT) {
      return;
    }

    if (isTracing) {
      ComponentsSystrace.beginSection("collectResults");
    }
    collectResults(null, c, null, root, layoutState, null);
    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    if (isTracing) {
      ComponentsSystrace.beginSection("sortMountableOutputs");
    }
    Collections.sort(layoutState.mMountableOutputTops, sTopsComparator);
    Collections.sort(layoutState.mMountableOutputBottoms, sBottomsComparator);

    if (layoutState.mIncrementalVisibility) {
      layoutState.mVisibilityModuleInput.setIncrementalModuleItems(layoutState.mVisibilityOutputs);
      layoutState.mVisibilityOutputs.clear();
    }

    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    if (!c.isReconciliationEnabled()
        && !ComponentsConfiguration.useInternalNodesForLayoutDiffing
        && !ComponentsConfiguration.isDebugModeEnabled
        && !ComponentsConfiguration.isEndToEndTestRun) {
      layoutState.mLayoutRoot = null;
    }
  }

  private static boolean isReconcilable(
      final ComponentContext c,
      final Component nextRootComponent,
      final @Nullable LayoutState currentLayoutState) {

    if (currentLayoutState == null
        || currentLayoutState.mLayoutRoot == null
        || !c.isReconciliationEnabled()) {
      return false;
    }

    StateHandler stateHandler = c.getStateHandler();
    if (stateHandler == null || !stateHandler.hasPendingUpdates()) {
      return false;
    }

    final Component previous = currentLayoutState.mComponent;
    if (!ComponentUtils.isSameComponentType(previous, nextRootComponent)) {
      return false;
    }

    if (isMountSpec(nextRootComponent) && !nextRootComponent.isEquivalentTo(previous)) {
      return false;
    }

    if (!ComponentUtils.isEquivalentToIgnoringState(previous, nextRootComponent)) {
      return false;
    }

    return true;
  }

  private static String sourceToString(@CalculateLayoutSource int source) {
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
      case CalculateLayoutSource.MEASURE:
        return "measure";
      case CalculateLayoutSource.TEST:
        return "test";
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

        if (Component.isMountViewSpec(component)) {
          if (isTracing) {
            ComponentsSystrace.beginSection("preAllocateMountContent:" + component.getSimpleName());
          }

          ComponentsPools.maybePreallocateContent(
              mContext.getAndroidContext(), component, recyclingMode);

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

  private void calculateAndSetLayoutOutputIdAndUpdateState(
      LayoutOutput layoutOutput,
      int level,
      @OutputUnitType int type,
      long previousId,
      boolean isCachedOutputUpdated,
      @Nullable DebugHierarchy.Node hierarchy) {
    if (mLayoutStateOutputIdCalculator == null) {
      mLayoutStateOutputIdCalculator = new LayoutStateOutputIdCalculator();
    }
    mLayoutStateOutputIdCalculator.calculateAndSetLayoutOutputIdAndUpdateState(
        layoutOutput, level, type, previousId, isCachedOutputUpdated, hierarchy);
  }

  /**
   * Generate a global key for the given components that is unique among all of the components in
   * the layout.
   */
  static String generateGlobalKey(ComponentContext parentContext, Component component) {
    final LayoutState layoutState = parentContext.getLayoutState();
    if (layoutState == null) {
      throw new IllegalStateException(
          component.getSimpleName()
              + ": Trying to generate global key of component outside of a LayoutState calculation.");
    }

    final Component parentScope = parentContext.getComponentScope();
    final String globalKey;

    if (parentScope == null) {
      globalKey = component.getKey();
    } else {
      if (parentScope.getGlobalKey() == null) {
        ComponentsReporter.emitMessage(
            ComponentsReporter.LogLevel.ERROR,
            NULL_PARENT_KEY,
            "Trying to generate parent-based key for component "
                + component.getSimpleName()
                + " , but parent "
                + parentScope.getSimpleName()
                + " has a null global key \"."
                + " This is most likely a configuration mistake,"
                + " check the value of ComponentsConfiguration.useGlobalKeys.");
      }
      globalKey =
          generateUniqueGlobalKeyForChild(layoutState, parentScope.getGlobalKey(), component);
    }

    return globalKey;
  }

  /**
   * Generate a global key for the given components that is unique among all of the components in
   * the layout.
   *
   * @param layoutState the LayoutState currently operating
   * @param parentGlobalKey the global key of the parent component
   * @param component the child component for which the unique global key will be generated
   * @return a unique global key for given component
   */
  private static String generateUniqueGlobalKeyForChild(
      LayoutState layoutState, @Nullable String parentGlobalKey, Component component) {

    if (parentGlobalKey == null) {
      parentGlobalKey = "null";
    }

    final String childKey =
        ComponentKeyUtils.getKeyWithSeparator(parentGlobalKey, component.getKey());

    if (component.hasManualKey()) {
      final int manualKeyIndex = layoutState.getGlobalManualKeyCountAndIncrement(childKey);
      if (manualKeyIndex != 0) {
        ComponentsReporter.emitMessage(
            ComponentsReporter.LogLevel.WARNING,
            DUPLICATE_MANUAL_KEY,
            "The manual key "
                + component.getKey()
                + " you are setting on this "
                + component.getSimpleName()
                + " is a duplicate and will be changed into a unique one. "
                + "This will result in unexpected behavior if you don't change it.");
      }
      return ComponentKeyUtils.getKeyForChildPosition(childKey, manualKeyIndex);
    }

    final int childIndex = layoutState.getGlobalKeyCountAndIncrement(childKey);
    return ComponentKeyUtils.getKeyForChildPosition(childKey, childIndex);
  }

  @Nullable
  InternalNode getCachedLayout(Component component) {
    return mLastMeasuredLayouts.get(component.getId());
  }

  boolean hasCachedLayout(Component component) {
    return mLastMeasuredLayouts.containsKey(component.getId());
  }

  @VisibleForTesting
  protected void clearCachedLayout(Component component) {
    mLastMeasuredLayouts.remove(component.getId());
  }

  void addLastMeasuredLayout(Component component, InternalNode lastMeasuredLayout) {
    mLastMeasuredLayouts.put(component.getId(), lastMeasuredLayout);
  }

  static DiffNode createDiffNode(InternalNode node, DiffNode parent) {
    DiffNode diffNode = new DefaultDiffNode();

    diffNode.setLastWidthSpec(node.getLastWidthSpec());
    diffNode.setLastHeightSpec(node.getLastHeightSpec());
    diffNode.setLastMeasuredWidth(node.getLastMeasuredWidth());
    diffNode.setLastMeasuredHeight(node.getLastMeasuredHeight());
    diffNode.setComponent(node.getTailComponent());
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
  public RenderTreeNode getMountableOutputAt(int index) {
    return mMountableOutputs.get(index);
  }

  @Override
  public ArrayList<RenderTreeNode> getMountableOutputTops() {
    return mMountableOutputTops;
  }

  @Override
  public ArrayList<RenderTreeNode> getMountableOutputBottoms() {
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
  public VisibilityModuleInput getVisibilityModuleInput() {
    return mVisibilityModuleInput;
  }

  int getTestOutputCount() {
    return mTestOutputs == null ? 0 : mTestOutputs.size();
  }

  @Nullable
  TestOutput getTestOutputAt(int index) {
    return mTestOutputs == null ? null : mTestOutputs.get(index);
  }

  public DiffNode getDiffTree() {
    return mDiffTreeRoot;
  }

  int getWidth() {
    return mWidth;
  }

  int getHeight() {
    return mHeight;
  }

  /** @return The id of the {@link ComponentTree} that generated this {@link LayoutState} */
  int getComponentTreeId() {
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

  /**
   * Returns the state handler instance currently held by LayoutState and nulls it afterwards.
   *
   * @return the state handler
   */
  @CheckReturnValue
  StateHandler consumeStateHandler() {
    final StateHandler stateHandler = mStateHandler;
    mStateHandler = null;
    return stateHandler;
  }

  InternalNode getLayoutRoot() {
    return mLayoutRoot;
  }

  // If the layout root is a nested tree holder node, it gets skipped immediately while
  // collecting the LayoutOutputs. The nested tree itself effectively becomes the layout
  // root in this case.
  private boolean isLayoutRoot(InternalNode node) {
    return mLayoutRoot.isNestedTreeHolder()
        ? node == mLayoutRoot.getNestedTree()
        : node == mLayoutRoot;
  }

  /**
   * Returns true if this is the root node (which always generates a matching layout output), if the
   * node has view attributes e.g. tags, content description, etc, or if the node has explicitly
   * been forced to be wrapped in a view.
   */
  private static boolean needsHostView(InternalNode node, LayoutState layoutState) {
    if (layoutState.isLayoutRoot(node)) {
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

    final List<Component> components = node.getComponents();
    for (Component comp : components) {
      if (comp != null && comp.hasCommonDynamicProps()) {
        // Need a host View to apply the dynamic props to
        return true;
      }
    }

    if (needsHostViewForTransition(node)) {
      return true;
    }

    return false;
  }

  private static boolean needsHostViewForTransition(InternalNode node) {
    return !TextUtils.isEmpty(node.getTransitionKey()) && !isMountViewSpec(node.getTailComponent());
  }

  /**
   * @return the position of the {@link LayoutOutput} with id layoutOutputId in the {@link
   *     LayoutState} list of outputs or -1 if no {@link LayoutOutput} with that id exists in the
   *     {@link LayoutState}
   */
  @Override
  public int getLayoutOutputPositionForId(long layoutOutputId) {
    return mOutputsIdToPositionMap.get(layoutOutputId, -1);
  }

  /** @return a {@link LayoutOutput} for a given {@param layoutOutputId} */
  @Nullable
  LayoutOutput getLayoutOutput(long layoutOutputId) {
    final int position = getLayoutOutputPositionForId(layoutOutputId);
    return position < 0 ? null : LayoutOutput.getLayoutOutput(getMountableOutputAt(position));
  }

  @Nullable
  List<Transition> getTransitions() {
    return mTransitions;
  }

  /** Gets a mapping from transition ids to a group of LayoutOutput. */
  Map<TransitionId, OutputUnitsAffinityGroup<LayoutOutput>> getTransitionIdMapping() {
    return mTransitionIdMapping;
  }

  /** Gets a group of LayoutOutput given transition key */
  @Nullable
  OutputUnitsAffinityGroup<LayoutOutput> getLayoutOutputsForTransitionId(
      TransitionId transitionId) {
    return mTransitionIdMapping.get(transitionId);
  }

  private static void addMountableOutput(
      final LayoutState layoutState,
      final LayoutOutput layoutOutput,
      final @Nullable RenderTreeNode parent) {

    layoutOutput.setIndex(layoutState.mMountableOutputs.size());

    final RenderTreeNode node = LayoutOutput.create(layoutOutput, parent);

    if (parent != null) {
      parent.child(node);
    }

    layoutState.mMountableOutputs.add(node);
    layoutState.mMountableOutputTops.add(node);
    layoutState.mMountableOutputBottoms.add(node);
  }

  /**
   * @return the list of Components in this LayoutState that care about the previously mounted
   *     versions of their @Prop/@State params.
   */
  @Nullable
  List<Component> getComponentsNeedingPreviousRenderData() {
    return mComponentsNeedingPreviousRenderData;
  }

  @Nullable
  TransitionId getRootTransitionId() {
    return mRootTransitionId;
  }

  private int getGlobalKeyCountAndIncrement(String key) {
    if (mGlobalKeysCounter == null) {
      mGlobalKeysCounter = new HashMap<>();
    }

    Integer count = mGlobalKeysCounter.get(key);
    if (count == null) {
      count = 0;
    }

    mGlobalKeysCounter.put(key, count + 1);
    return count;
  }

  private int getGlobalManualKeyCountAndIncrement(String manualKey) {
    if (mGlobalManualKeysCounter == null) {
      mGlobalManualKeysCounter = new HashMap<>();
    }

    Integer count = mGlobalManualKeysCounter.get(manualKey);
    if (count == null) {
      count = 0;
    }

    mGlobalManualKeysCounter.put(manualKey, count + 1);
    return count;
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
      final LayoutOutput layoutOutput = LayoutOutput.getLayoutOutput(getMountableOutputAt(i));
      res +=
          "  ["
              + i
              + "] id: "
              + layoutOutput.getId()
              + ", host: "
              + layoutOutput.getHostMarker()
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

  private static @Nullable TransitionId getTransitionIdForNode(InternalNode node) {
    return TransitionUtils.createTransitionId(node);
  }

  @Override
  public boolean hasMounted() {
    return mContext.getComponentTree().hasMounted();
  }
}
