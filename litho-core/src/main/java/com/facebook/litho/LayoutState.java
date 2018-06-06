/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.M;
import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO;
import static com.facebook.litho.Component.isLayoutSpecWithSizeSpec;
import static com.facebook.litho.Component.isMountDrawableSpec;
import static com.facebook.litho.Component.isMountSpec;
import static com.facebook.litho.Component.isMountViewSpec;
import static com.facebook.litho.ComponentContext.NULL_LAYOUT;
import static com.facebook.litho.ComponentLifecycle.MountType.NONE;
import static com.facebook.litho.ContextUtils.getValidActivityForContext;
import static com.facebook.litho.FrameworkLogEvents.EVENT_CALCULATE_LAYOUT_STATE;
import static com.facebook.litho.FrameworkLogEvents.EVENT_COLLECT_RESULTS;
import static com.facebook.litho.FrameworkLogEvents.EVENT_CREATE_LAYOUT;
import static com.facebook.litho.FrameworkLogEvents.EVENT_CSS_LAYOUT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_COMPONENT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_LAYOUT_STATE_SOURCE;
import static com.facebook.litho.FrameworkLogEvents.PARAM_LOG_TAG;
import static com.facebook.litho.FrameworkLogEvents.PARAM_TREE_DIFF_ENABLED;
import static com.facebook.litho.MountItem.LAYOUT_FLAG_DISABLE_TOUCHABLE;
import static com.facebook.litho.MountItem.LAYOUT_FLAG_DUPLICATE_PARENT_STATE;
import static com.facebook.litho.MountItem.LAYOUT_FLAG_MATCH_HOST_BOUNDS;
import static com.facebook.litho.MountState.ROOT_HOST_ID;
import static com.facebook.litho.NodeInfo.ENABLED_SET_FALSE;
import static com.facebook.litho.NodeInfo.ENABLED_UNSET;
import static com.facebook.litho.NodeInfo.FOCUS_SET_TRUE;
import static com.facebook.litho.SizeSpec.EXACTLY;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.LongSparseArray;
import android.support.v4.util.SimpleArrayMap;
import android.support.v4.view.accessibility.AccessibilityManagerCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.accessibility.AccessibilityManager;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.displaylist.DisplayList;
import com.facebook.litho.displaylist.DisplayListException;
import com.facebook.litho.reference.BorderColorDrawableReference;
import com.facebook.litho.reference.DrawableReference;
import com.facebook.litho.reference.Reference;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaNode;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.CheckReturnValue;

/**
 * The main role of {@link LayoutState} is to hold the output of layout calculation. This includes
 * mountable outputs and visibility outputs. A centerpiece of the class is {@link
 * #collectResults(InternalNode, LayoutState, DiffNode)} which prepares the before-mentioned outputs
 * based on the provided {@link InternalNode} for later use in {@link MountState}.
 */
class LayoutState {

  @IntDef({
    CalculateLayoutSource.TEST,
    CalculateLayoutSource.NONE,
    CalculateLayoutSource.SET_ROOT,
    CalculateLayoutSource.SET_SIZE_SPEC,
    CalculateLayoutSource.UPDATE_STATE,
    CalculateLayoutSource.MEASURE
  })
  @Retention(RetentionPolicy.SOURCE)
  public @interface CalculateLayoutSource {
    int TEST = -2;
    int NONE = -1;
    int SET_ROOT = 0;
    int SET_SIZE_SPEC = 1;
    int UPDATE_STATE = 2;
    int MEASURE = 3;
  }

  static final Comparator<LayoutOutput> sTopsComparator =
      new Comparator<LayoutOutput>() {
        @Override
        public int compare(LayoutOutput lhs, LayoutOutput rhs) {
          final int lhsTop = lhs.getBounds().top;
          final int rhsTop = rhs.getBounds().top;
          // Lower indices should be higher for tops so that they are mounted first if possible.
          return lhsTop == rhsTop ? lhs.getIndex() - rhs.getIndex() : lhsTop - rhsTop;
        }
      };

  static final Comparator<LayoutOutput> sBottomsComparator =
      new Comparator<LayoutOutput>() {
        @Override
        public int compare(LayoutOutput lhs, LayoutOutput rhs) {
          final int lhsBottom = lhs.getBounds().bottom;
          final int rhsBottom = rhs.getBounds().bottom;
          // Lower indices should be lower for bottoms so that they are mounted first if possible.
          return lhsBottom == rhsBottom ? rhs.getIndex() - lhs.getIndex() : lhsBottom - rhsBottom;
        }
      };

  private final Map<String, Rect> mComponentKeyToBounds = new HashMap<>();
  private final List<Component> mComponents = new ArrayList<>();

  @ThreadConfined(ThreadConfined.UI)
  private final Rect mDisplayListCreateRect = new Rect();

  @ThreadConfined(ThreadConfined.ANY)
  private final Rect mDisplayListQueueRect = new Rect();

  private static final int[] DRAWABLE_STATE_ENABLED = new int[]{android.R.attr.state_enabled};
  private static final int[] DRAWABLE_STATE_NOT_ENABLED = new int[]{};

  private volatile ComponentContext mContext;

  private Component mComponent;

  private int mWidthSpec;
  private int mHeightSpec;

  private final List<LayoutOutput> mMountableOutputs = new ArrayList<>(8);
  private final List<VisibilityOutput> mVisibilityOutputs = new ArrayList<>(8);
  private final LongSparseArray<Integer> mOutputsIdToPositionMap = new LongSparseArray<>(8);
  private final ArrayList<LayoutOutput> mMountableOutputTops = new ArrayList<>();
  private final ArrayList<LayoutOutput> mMountableOutputBottoms = new ArrayList<>();
  private final Queue<Integer> mDisplayListsToPrefetch = new LinkedList<>();

  @Nullable private LayoutStateOutputIdCalculator mLayoutStateOutputIdCalculator;

  private List<TestOutput> mTestOutputs;

  @Nullable InternalNode mLayoutRoot;
  @Nullable String mRootTransitionKey;

  private DiffNode mDiffTreeRoot;
  // Reference count will be initialized to 1 in init().
  private final AtomicInteger mReferenceCount = new AtomicInteger(-1);

  private int mWidth;
  private int mHeight;

  private int mCurrentX;
  private int mCurrentY;

  private int mCurrentLevel = 0;

  // Holds the current host marker in the layout tree.
  private long mCurrentHostMarker = -1L;
  private int mCurrentHostOutputPosition = -1;

  private boolean mShouldDuplicateParentState = true;
  @NodeInfo.EnabledState private short mParentEnabledState = ENABLED_UNSET;

  private boolean mShouldGenerateDiffTree = false;
  private int mComponentTreeId = -1;

  private AccessibilityManager mAccessibilityManager;
  private boolean mAccessibilityEnabled = false;

  private StateHandler mStateHandler;
  private boolean mCanPrefetchDisplayLists;
  private boolean mCanCacheDrawingDisplayLists;
  private boolean mClipChildren = true;
  private List<Component> mComponentsNeedingPreviousRenderData;
  @Nullable private OutputUnitsAffinityGroup<LayoutOutput> mCurrentLayoutOutputAffinityGroup;
  private final SimpleArrayMap<String, OutputUnitsAffinityGroup<LayoutOutput>>
      mTransitionKeyMapping = new SimpleArrayMap<>();
  private List<Transition> mTransitions;
  long mCalculateLayoutDuration;

  @Nullable WorkingRangeContainer mWorkingRangeContainer;

  LayoutState() {
    if (!ComponentsConfiguration.lazilyInitializeLayoutStateOutputIdCalculator) {
      mLayoutStateOutputIdCalculator = new LayoutStateOutputIdCalculator();
    }
  }

  void init(ComponentContext context) {
    mContext = context;
    mStateHandler = mContext.getStateHandler();
    mReferenceCount.set(1);
    mTestOutputs = ComponentsConfiguration.isEndToEndTestRun ? new ArrayList<TestOutput>(8) : null;
  }

  /**
   * Acquires a new layout output for the internal node and its associated component. It returns
   * null if there's no component associated with the node as the mount pass only cares about nodes
   * that will potentially mount content into the component host.
   */
  @Nullable
  private static LayoutOutput createGenericLayoutOutput(
      InternalNode node, LayoutState layoutState, boolean hasHostView) {
    final Component component = node.getRootComponent();

    // Skip empty nodes and layout specs because they don't mount anything.
    if (component == null || component.getMountType() == NONE) {
      return null;
    }

    return createLayoutOutput(
        component,
        layoutState,
        node,
        true /* useNodePadding */,
        node.getImportantForAccessibility(),
        layoutState.mShouldDuplicateParentState,
        hasHostView);
  }

  private static LayoutOutput createHostLayoutOutput(LayoutState layoutState, InternalNode node) {
    final LayoutOutput hostOutput =
        createLayoutOutput(
            HostComponent.create(),
            layoutState,
            node,
            false /* useNodePadding */,
            node.getImportantForAccessibility(),
            node.isDuplicateParentStateEnabled(),
            false);

    ViewNodeInfo viewNodeInfo = hostOutput.getViewNodeInfo();
    if (node.hasStateListAnimatorResSet()) {
      viewNodeInfo.setStateListAnimatorRes(node.getStateListAnimatorRes());
    } else {
      viewNodeInfo.setStateListAnimator(node.getStateListAnimator());
    }

    return hostOutput;
  }

  private static LayoutOutput createDrawableLayoutOutput(
      Component component, LayoutState layoutState, InternalNode node, boolean hasHostView) {
    return createLayoutOutput(
        component,
        layoutState,
        node,
        false /* useNodePadding */,
        IMPORTANT_FOR_ACCESSIBILITY_NO,
        layoutState.mShouldDuplicateParentState,
        hasHostView);
  }

  private static LayoutOutput createLayoutOutput(
      Component component,
      LayoutState layoutState,
      InternalNode node,
      boolean useNodePadding,
      int importantForAccessibility,
      boolean duplicateParentState,
      boolean hasHostView) {
    final boolean isMountViewSpec = isMountViewSpec(component);

    final LayoutOutput layoutOutput = ComponentsPools.acquireLayoutOutput();
    layoutOutput.setComponent(component);
    layoutOutput.setImportantForAccessibility(importantForAccessibility);

    // The mount operation will need both the marker for the target host and its matching
    // parent host to ensure the correct hierarchy when nesting the host views.
    layoutOutput.setHostMarker(layoutState.mCurrentHostMarker);

    final int hostTranslationX;
    final int hostTranslationY;
    if (layoutState.mCurrentHostOutputPosition >= 0) {
      final LayoutOutput hostOutput =
          layoutState.mMountableOutputs.get(layoutState.mCurrentHostOutputPosition);

      final Rect hostBounds = hostOutput.getBounds();
      hostTranslationX = hostBounds.left;
      hostTranslationY = hostBounds.top;
      layoutOutput.setHostTranslationX(hostTranslationX);
      layoutOutput.setHostTranslationY(hostTranslationY);
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
    if (isMountViewSpec) {
      layoutOutput.setNodeInfo(node.getNodeInfo());
      // Acquire a ViewNodeInfo, set it up and release it after passing it to the LayoutOutput.
      final ViewNodeInfo viewNodeInfo = ViewNodeInfo.acquire();
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
      viewNodeInfo.setClipChildren(layoutState.mClipChildren);
      layoutOutput.setViewNodeInfo(viewNodeInfo);
      viewNodeInfo.release();
    } else {
      l += paddingLeft;
      t += paddingTop;
      r -= paddingRight;
      b -= paddingBottom;

      if (node.getNodeInfo() != null && node.getNodeInfo().getEnabledState() == ENABLED_SET_FALSE) {
        flags |= LAYOUT_FLAG_DISABLE_TOUCHABLE;
      }
    }

    layoutOutput.setBounds(l, t, r, b);

    if (duplicateParentState) {
      flags |= LAYOUT_FLAG_DUPLICATE_PARENT_STATE;
    }

    if (hasHostView) {
      flags |= LAYOUT_FLAG_MATCH_HOST_BOUNDS;
    } else {
      // If there is a host view, the transition key will be set on the view's layout output
      final String transitionKey = getTransitionKeyForNode(node);
      if (transitionKey != null) {
        layoutOutput.setTransitionKey(transitionKey);
      }
    }

    layoutOutput.setFlags(flags);

    final ComponentLifecycle lifecycle = component;
    if (isEligibleForCreatingDisplayLists() && lifecycle.shouldUseDisplayList()) {
      layoutOutput.initDisplayListContainer(
        lifecycle.getClass().getSimpleName(),
        layoutState.mCanCacheDrawingDisplayLists);
    }

    return layoutOutput;
  }

  /**
   * Acquires a {@link VisibilityOutput} object and computes the bounds for it using the information
   * stored in the {@link InternalNode}.
   */
  private static VisibilityOutput createVisibilityOutput(
      InternalNode node,
      LayoutState layoutState) {

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
    final VisibilityOutput visibilityOutput = ComponentsPools.acquireVisibilityOutput();

    visibilityOutput.setComponent(node.getRootComponent());
    visibilityOutput.setBounds(l, t, r, b);
    visibilityOutput.setVisibleHeightRatio(node.getVisibleHeightRatio());
    visibilityOutput.setVisibleWidthRatio(node.getVisibleWidthRatio());
    visibilityOutput.setVisibleEventHandler(visibleHandler);
    visibilityOutput.setFocusedEventHandler(focusedHandler);
    visibilityOutput.setUnfocusedEventHandler(unfocusedHandler);
    visibilityOutput.setFullImpressionEventHandler(fullImpressionHandler);
    visibilityOutput.setInvisibleEventHandler(invisibleHandler);

    return visibilityOutput;
  }

  private static TestOutput createTestOutput(
      InternalNode node,
      LayoutState layoutState,
      LayoutOutput layoutOutput) {
    final int l = layoutState.mCurrentX + node.getX();
    final int t = layoutState.mCurrentY + node.getY();
    final int r = l + node.getWidth();
    final int b = t + node.getHeight();

    final TestOutput output = ComponentsPools.acquireTestOutput();
    output.setTestKey(node.getTestKey());
    output.setBounds(l, t, r, b);
    output.setHostMarker(layoutState.mCurrentHostMarker);
    if (layoutOutput != null) {
      output.setLayoutOutputId(layoutOutput.getId());
    }

    return output;
  }

  private static boolean isLayoutDirectionRTL(Context context) {
    ApplicationInfo applicationInfo = context.getApplicationInfo();

    if ((SDK_INT >= JELLY_BEAN_MR1)
        && (applicationInfo.flags & ApplicationInfo.FLAG_SUPPORTS_RTL) != 0) {

      int layoutDirection = getLayoutDirection(context);
      return layoutDirection == View.LAYOUT_DIRECTION_RTL;
    }

    return false;
  }

  @TargetApi(JELLY_BEAN_MR1)
  private static int getLayoutDirection(Context context) {
    return context.getResources().getConfiguration().getLayoutDirection();
  }

  /**
   * Determine if a given {@link InternalNode} within the context of a given {@link LayoutState}
   * requires to be wrapped inside a view.
   *
   * @see #needsHostView(InternalNode, LayoutState)
   */
  private static boolean hasViewContent(InternalNode node, LayoutState layoutState) {
    final Component component = node.getRootComponent();
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
    final boolean hasAccessibilityContent = layoutState.mAccessibilityEnabled
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

    return hasFocusChangeHandler
        || hasEnabledTouchEventHandlers
        || hasViewTag
        || hasViewTags
        || hasShadowElevation
        || hasOutlineProvider
        || hasClipToOutline
        || hasAccessibilityContent
        || isFocusableSetTrue;
  }

  /**
   * Collects layout outputs and release the layout tree. The layout outputs hold necessary
   * information to be used by {@link MountState} to mount components into a {@link ComponentHost}.
   * <p/>
   * Whenever a component has view content (view tags, click handler, etc), a new host 'marker'
   * is added for it. The mount pass will use the markers to decide which host should be used
   * for each layout output. The root node unconditionally generates a layout output corresponding
   * to the root host.
   * <p/>
   * The order of layout outputs follows a depth-first traversal in the tree to ensure the hosts
   * will be created at the right order when mounting. The host markers will be define which host
   * each mounted artifacts will be attached to.
   * <p/>
   * At this stage all the {@link InternalNode} for which we have LayoutOutputs that can be recycled
   * will have a DiffNode associated. If the CachedMeasures are valid we'll try to recycle both the
   * host and the contents (including background/foreground). In all other cases instead we'll only
   * try to re-use the hosts. In some cases the host's structure might change between two updates
   * even if the component is of the same type. This can happen for example when a click listener is
   * added. To avoid trying to re-use the wrong host type we explicitly check that after all the
   * children for a subtree have been added (this is when the actual host type is resolved). If the
   * host type changed compared to the one in the DiffNode we need to refresh the ids for the whole
   * subtree in order to ensure that the MountState will unmount the subtree and mount it again on
   * the correct host.
   * <p/>
   *
   * @param node InternalNode to process.
   * @param layoutState the LayoutState currently operating.
   * @param parentDiffNode whether this method also populates the diff tree and assigns the root
   *                       to mDiffTreeRoot.
   */
  private static void collectResults(
      InternalNode node,
      LayoutState layoutState,
      DiffNode parentDiffNode) {
    if (node.hasNewLayout()) {
      node.markLayoutSeen();
    }
    final Component component = node.getRootComponent();

    // Early return if collecting results of a node holding a nested tree.
    if (node.isNestedTreeHolder()) {
      // If the nested tree is defined, it has been resolved during a measure call during
      // layout calculation.
      InternalNode nestedTree = resolveNestedTree(
          node,
          SizeSpec.makeSizeSpec(node.getWidth(), EXACTLY),
          SizeSpec.makeSizeSpec(node.getHeight(), EXACTLY));

      if (nestedTree == NULL_LAYOUT) {
        return;
      }

      // Account for position of the holder node.
      layoutState.mCurrentX += node.getX();
      layoutState.mCurrentY += node.getY();

      collectResults(nestedTree, layoutState, parentDiffNode);

      layoutState.mCurrentX -= node.getX();
      layoutState.mCurrentY -= node.getY();

      return;
    }

    final boolean shouldGenerateDiffTree = layoutState.mShouldGenerateDiffTree;
    final DiffNode currentDiffNode = node.getDiffNode();
    final boolean shouldUseCachedOutputs =
        isMountSpec(component) && currentDiffNode != null;
    final boolean isCachedOutputUpdated = shouldUseCachedOutputs && node.areCachedMeasuresValid();

    final DiffNode diffNode;
    if (shouldGenerateDiffTree) {
      diffNode = createDiffNode(node, parentDiffNode);
      if (parentDiffNode == null) {
        layoutState.mDiffTreeRoot = diffNode;
      }
    } else {
      diffNode = null;
    }

    // If the parent of this node is disabled, this node has to be disabled too.
    if (layoutState.mParentEnabledState == ENABLED_SET_FALSE) {
      node.enabled(false);
    }

    final boolean needsHostView = needsHostView(node, layoutState);

    final long currentHostMarker = layoutState.mCurrentHostMarker;
    final int currentHostOutputPosition = layoutState.mCurrentHostOutputPosition;

    final OutputUnitsAffinityGroup<LayoutOutput> currentLayoutOutputAffinityGroup =
        layoutState.mCurrentLayoutOutputAffinityGroup;

    final String transitionKey = getTransitionKeyForNode(node);
    layoutState.mCurrentLayoutOutputAffinityGroup =
        transitionKey != null ? new OutputUnitsAffinityGroup<LayoutOutput>() : null;

    int hostLayoutPosition = -1;

    // 1. Insert a host LayoutOutput if we have some interactive content to be attached to.
    if (needsHostView) {
      hostLayoutPosition = addHostLayoutOutput(node, layoutState, diffNode);
      addCurrentAffinityGroupToTransitionMapping(transitionKey, layoutState);

      layoutState.mCurrentLevel++;
      layoutState.mCurrentHostMarker =
          layoutState.mMountableOutputs.get(hostLayoutPosition).getId();
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
    final LayoutOutput layoutOutput = createGenericLayoutOutput(node, layoutState, needsHostView);
    if (layoutOutput != null) {
      final long previousId = shouldUseCachedOutputs ? currentDiffNode.getContent().getId() : -1;
      layoutState.calculateAndSetLayoutOutputIdAndUpdateState(
          layoutOutput,
          layoutState.mCurrentLevel,
          OutputUnitType.CONTENT,
          previousId,
          isCachedOutputUpdated);
    }

    // If we don't need to update this output we can safely re-use the display list from the
    // previous output.
    if (ThreadUtils.isMainThread() && isCachedOutputUpdated) {
      layoutOutput.setDisplayListContainer(currentDiffNode.getContent().getDisplayListContainer());
    }

    // 2. Add background if defined.
    final Reference<? extends Drawable> background = node.getBackground();
    if (background != null) {
      if (layoutOutput != null && layoutOutput.hasViewNodeInfo()) {
        layoutOutput.getViewNodeInfo().setBackground(background);
      } else {
        final LayoutOutput convertBackground = (currentDiffNode != null)
            ? currentDiffNode.getBackground()
            : null;

        final LayoutOutput backgroundOutput =
            addDrawableComponent(
                node,
                layoutState,
                convertBackground,
                background,
                OutputUnitType.BACKGROUND,
                needsHostView);

        if (diffNode != null) {
          diffNode.setBackground(backgroundOutput);
        }
      }
    }

    // 3. Now add the MountSpec (either View or Drawable) to the Outputs.
    if (isMountSpec(component)) {
      // Notify component about its final size.
      component.onBoundsDefined(layoutState.mContext, node);

      addMountableOutput(layoutState, layoutOutput);
      addLayoutOutputIdToPositionsMap(
          layoutState.mOutputsIdToPositionMap,
          layoutOutput,
          layoutState.mMountableOutputs.size() - 1);
      maybeAddLayoutOutputToAffinityGroup(
          layoutState.mCurrentLayoutOutputAffinityGroup, OutputUnitType.CONTENT, layoutOutput);

      if (diffNode != null) {
        diffNode.setContent(layoutOutput);
      }
    }

    // 4. Extract the Transitions.
    if (TransitionUtils.areTransitionsEnabled(component.getScopedContext())) {
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
    @NodeInfo.EnabledState final short parentEnabledState = layoutState.mParentEnabledState;
    layoutState.mParentEnabledState = (node.getNodeInfo() != null)
        ? node.getNodeInfo().getEnabledState()
        : ENABLED_UNSET;

    // We must process the nodes in order so that the layout state output order is correct.
    for (int i = 0, size = node.getChildCount(); i < size; i++) {
      collectResults(
          node.getChildAt(i),
          layoutState,
          diffNode);
    }

    layoutState.mParentEnabledState = parentEnabledState;
    layoutState.mCurrentX -= node.getX();
    layoutState.mCurrentY -= node.getY();

    // 5. Add border color if defined.
    if (node.shouldDrawBorders()) {
      final LayoutOutput convertBorder =
          (currentDiffNode != null) ? currentDiffNode.getBorder() : null;

      final LayoutOutput borderOutput =
          addDrawableComponent(
              node,
              layoutState,
              convertBorder,
              getBorderColorDrawable(node),
              OutputUnitType.BORDER,
              needsHostView);
      if (diffNode != null) {
        diffNode.setBorder(borderOutput);
      }
    }

    // 6. Add foreground if defined.
    final Drawable foreground = node.getForeground();
    if (foreground != null) {
      if (layoutOutput != null && layoutOutput.hasViewNodeInfo() && SDK_INT >= M) {
        layoutOutput.getViewNodeInfo().setForeground(foreground);
      } else {
        final LayoutOutput convertForeground = (currentDiffNode != null)
            ? currentDiffNode.getForeground()
            : null;

        final LayoutOutput foregroundOutput =
            addDrawableComponent(
                node,
                layoutState,
                convertForeground,
                DrawableReference.create().drawable(foreground).build(),
                OutputUnitType.FOREGROUND,
                needsHostView);

        if (diffNode != null) {
          diffNode.setForeground(foregroundOutput);
        }
      }
    }

    // 7. Add VisibilityOutputs if any visibility-related event handlers are present.
    if (node.hasVisibilityHandlers()) {
      final VisibilityOutput visibilityOutput = createVisibilityOutput(node, layoutState);
      final long previousId =
          shouldUseCachedOutputs && currentDiffNode.getVisibilityOutput() != null
              ? currentDiffNode.getVisibilityOutput().getId()
              : -1;

      layoutState.calculateAndSetVisibilityOutputId(
          visibilityOutput, layoutState.mCurrentLevel, previousId);
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

    // 9. Keep a list of the components we created during this layout calculation. If the layout is
    // valid, the ComponentTree will update the event handlers that have been created in the
    // previous ComponentTree with the new component dispatched, otherwise Section children might
    // not be accessing the correct props and state on the event handlers. The null checkers cover
    // tests, the scope and tree should not be null at this point of the layout calculation.
    if (component != null
        && component.getScopedContext() != null
        && component.getScopedContext().getComponentTree() != null) {
      layoutState.mComponents.add(component);
    }

    // 10. Extract the Working Range registrations.
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
      final Rect rect = ComponentsPools.acquireRect();
      if (layoutOutput != null) {
        rect.set(layoutOutput.getBounds());
      } else {
        rect.left = layoutState.mCurrentX + node.getX();
        rect.top = layoutState.mCurrentY + node.getY();
        rect.right = rect.left + node.getWidth();
        rect.bottom = rect.top + node.getHeight();
      }
      for (Component delegate : node.getComponents()) {
        final Rect copyRect = ComponentsPools.acquireRect();
        copyRect.set(rect);
        if (delegate.getGlobalKey() != null) {
          layoutState.mComponentKeyToBounds.put(delegate.getGlobalKey(), copyRect);
        }
      }
      ComponentsPools.release(rect);
    }

    // All children for the given host have been added, restore the previous
    // host, level, and duplicate parent state value in the recursive queue.
    if (layoutState.mCurrentHostMarker != currentHostMarker) {
      layoutState.mCurrentHostMarker = currentHostMarker;
      layoutState.mCurrentHostOutputPosition = currentHostOutputPosition;
      layoutState.mCurrentLevel--;
    }
    layoutState.mShouldDuplicateParentState = shouldDuplicateParentState;

    addCurrentAffinityGroupToTransitionMapping(transitionKey, layoutState);
    layoutState.mCurrentLayoutOutputAffinityGroup = currentLayoutOutputAffinityGroup;
  }

  Map<String, Rect> getComponentKeyToBounds() {
    return mComponentKeyToBounds;
  }

  List<Component> getComponents() {
    return mComponents;
  }

  void clearComponents() {
    mComponents.clear();
  }

  private static void calculateAndSetHostOutputIdAndUpdateState(
      InternalNode node,
      LayoutOutput hostOutput,
      LayoutState layoutState,
      boolean isCachedOutputUpdated) {
    if (layoutState.isLayoutRoot(node)) {
      // The root host (LithoView) always has ID 0 and is unconditionally
      // set as dirty i.e. no need to use shouldComponentUpdate().
      hostOutput.setId(ROOT_HOST_ID);

      // Special case where the host marker of the root host is pointing to itself.
      hostOutput.setHostMarker(ROOT_HOST_ID);
      hostOutput.setUpdateState(LayoutOutput.STATE_DIRTY);
    } else {
      layoutState.calculateAndSetLayoutOutputIdAndUpdateState(
          hostOutput, layoutState.mCurrentLevel, OutputUnitType.HOST, -1, isCachedOutputUpdated);
    }
  }

  private static LayoutOutput addDrawableComponent(
      InternalNode node,
      LayoutState layoutState,
      LayoutOutput recycle,
      Reference<? extends Drawable> reference,
      @OutputUnitType int type,
      boolean matchHostBoundsTransitions) {
    final Component drawableComponent = DrawableComponent.create(reference);
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
            drawableComponent,
            layoutState,
            node,
            type,
            previousId,
            isOutputUpdated,
            matchHostBoundsTransitions);

    maybeAddLayoutOutputToAffinityGroup(
        layoutState.mCurrentLayoutOutputAffinityGroup, type, output);

    return output;
  }

  private static Reference<? extends Drawable> getBorderColorDrawable(InternalNode node) {
    if (!node.shouldDrawBorders()) {
      throw new RuntimeException("This node does not support drawing border color");
    }

    final YogaNode yogaNode = node.mYogaNode;
    final boolean isRtl = resolveLayoutDirection(yogaNode) == YogaDirection.RTL;
    final float[] borderRadius = node.getBorderRadius();
    final int[] borderColors = node.getBorderColors();
    final YogaEdge leftEdge = isRtl ? YogaEdge.RIGHT : YogaEdge.LEFT;
    final YogaEdge rightEdge = isRtl ? YogaEdge.LEFT : YogaEdge.RIGHT;

    return BorderColorDrawableReference.create(node.getContext())
        .pathEffect(node.getBorderPathEffect())
        .borderLeftColor(Border.getEdgeColor(borderColors, leftEdge))
        .borderTopColor(Border.getEdgeColor(borderColors, YogaEdge.TOP))
        .borderRightColor(Border.getEdgeColor(borderColors, rightEdge))
        .borderBottomColor(Border.getEdgeColor(borderColors, YogaEdge.BOTTOM))
        .borderLeftWidth(FastMath.round(yogaNode.getLayoutBorder(leftEdge)))
        .borderTopWidth(FastMath.round(yogaNode.getLayoutBorder(YogaEdge.TOP)))
        .borderRightWidth(FastMath.round(yogaNode.getLayoutBorder(rightEdge)))
        .borderBottomWidth(FastMath.round(yogaNode.getLayoutBorder(YogaEdge.BOTTOM)))
        .borderRadius(borderRadius)
        .build();
  }

  /** Continually walks the node hierarchy until a node returns a non inherited layout direction */
  private static YogaDirection resolveLayoutDirection(YogaNode node) {
    while (node != null && node.getLayoutDirection() == YogaDirection.INHERIT) {
      node = node.getOwner();
    }
    return node == null ? YogaDirection.INHERIT : node.getLayoutDirection();
  }

  private static void addLayoutOutputIdToPositionsMap(
      LongSparseArray outputsIdToPositionMap,
      LayoutOutput layoutOutput,
      int position) {
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

  private static void addCurrentAffinityGroupToTransitionMapping(
      String transitionKey, LayoutState layoutState) {
    final OutputUnitsAffinityGroup<LayoutOutput> group =
        layoutState.mCurrentLayoutOutputAffinityGroup;
    if (group == null || group.isEmpty()) {
      return;
    }

    if (layoutState.mTransitionKeyMapping.put(transitionKey, group) != null) {
      throw new RuntimeException(
          "The transitionKey '"
              + transitionKey
              + "' is defined multiple times in the same layout. transitionKeys must be unique "
              + "identifiers per layout. If this is a reusable component that can appear in the "
              + "same layout multiple times, consider passing unique transitionKeys from above.");
    }

    layoutState.mCurrentLayoutOutputAffinityGroup = null;
  }

  private static LayoutOutput addDrawableLayoutOutput(
      Component drawableComponent,
      LayoutState layoutState,
      InternalNode node,
      @OutputUnitType int outputType,
      long previousId,
      boolean isCachedOutputUpdated,
      boolean matchHostBoundsTransitions) {

    drawableComponent.onBoundsDefined(layoutState.mContext, node);

    final LayoutOutput drawableLayoutOutput =
        createDrawableLayoutOutput(
            drawableComponent, layoutState, node, matchHostBoundsTransitions);
    layoutState.calculateAndSetLayoutOutputIdAndUpdateState(
        drawableLayoutOutput,
        layoutState.mCurrentLevel,
        outputType,
        previousId,
        isCachedOutputUpdated);

    addMountableOutput(layoutState, drawableLayoutOutput);
    addLayoutOutputIdToPositionsMap(
        layoutState.mOutputsIdToPositionMap,
        drawableLayoutOutput,
        layoutState.mMountableOutputs.size() - 1);

    return drawableLayoutOutput;
  }

  static void releaseNodeTree(InternalNode node, boolean isNestedTree) {
    if (node == NULL_LAYOUT) {
      throw new IllegalArgumentException("Cannot release a null node tree");
    }

    for (int i = node.getChildCount() - 1; i >= 0; i--) {
      final InternalNode child = node.getChildAt(i);

      if (isNestedTree && node.hasNewLayout()) {
        node.markLayoutSeen();
      }

      // A node must be detached from its parent *before* being released (otherwise the parent would
      // retain a reference to a node that may get re-used by another thread)
      node.removeChildAt(i);

      releaseNodeTree(child, isNestedTree);
    }

    if (node.hasNestedTree() && node.getNestedTree() != NULL_LAYOUT) {
      releaseNodeTree(node.getNestedTree(), true);
    }

    node.release();
  }

  /**
   * If we have an interactive LayoutSpec or a MountSpec Drawable, we need to insert an
   * HostComponent in the Outputs such as it will be used as a HostView at Mount time. View
   * MountSpec are not allowed.
   *
   * @return The position the HostLayoutOutput was inserted.
   */
  private static int addHostLayoutOutput(
      InternalNode node,
      LayoutState layoutState,
      DiffNode diffNode) {
    final Component component = node.getRootComponent();

    // Only the root host is allowed to wrap view mount specs as a layout output
    // is unconditionally added for it.
    if (isMountViewSpec(component) && !layoutState.isLayoutRoot(node)) {
      throw new IllegalArgumentException("We shouldn't insert a host as a parent of a View");
    }

    final LayoutOutput hostLayoutOutput = createHostLayoutOutput(layoutState, node);

    // The component of the hostLayoutOutput will be set later after all the
    // children got processed.
    addMountableOutput(layoutState, hostLayoutOutput);

    final int hostOutputPosition = layoutState.mMountableOutputs.size() - 1;

    if (diffNode != null) {
      diffNode.setHost(hostLayoutOutput);
    }

    calculateAndSetHostOutputIdAndUpdateState(
        node,
        hostLayoutOutput,
        layoutState,
        false);

    addLayoutOutputIdToPositionsMap(
        layoutState.mOutputsIdToPositionMap,
        hostLayoutOutput,
        hostOutputPosition);

    maybeAddLayoutOutputToAffinityGroup(
        layoutState.mCurrentLayoutOutputAffinityGroup, OutputUnitType.HOST, hostLayoutOutput);

    return hostOutputPosition;
  }

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
        componentTreeId,
        widthSpec,
        heightSpec,
        false /* shouldGenerateDiffTree */,
        null /* previousDiffTreeRoot */,
        false /* canPrefetchDisplayLists */,
        false /* canCacheDrawingDisplayLists */,
        true /* clipChildren */,
        false /* persistInternalNodeTree */,
        source,
        null);
  }

  static LayoutState calculate(
      ComponentContext c,
      Component component,
      int componentTreeId,
      int widthSpec,
      int heightSpec,
      boolean shouldGenerateDiffTree,
      DiffNode previousDiffTreeRoot,
      boolean canPrefetchDisplayLists,
      boolean canCacheDrawingDisplayLists,
      boolean clipChildren,
      boolean persistInternalNodeTree,
      @CalculateLayoutSource int source,
      @Nullable String extraAttribution) {

    final ComponentsLogger logger = c.getLogger();
    LogEvent logLayoutState = null;

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
          .arg("widthSpec", SizeSpec.toString(widthSpec))
          .arg("heightSpec", SizeSpec.toString(heightSpec))
          .flush();
    }

    final LayoutState layoutState;
    try {
      if (logger != null) {
        logLayoutState = logger.newPerformanceEvent(EVENT_CALCULATE_LAYOUT_STATE);
        logLayoutState.addParam(PARAM_LAYOUT_STATE_SOURCE, sourceToString(source));
      }

      // Detect errors internal to components
      component.markLayoutStarted();

      final long timestampStartLayout = System.nanoTime();
      layoutState = ComponentsPools.acquireLayoutState(c);
      layoutState.clearComponents();
      layoutState.mShouldGenerateDiffTree = shouldGenerateDiffTree;
      layoutState.mComponentTreeId = componentTreeId;
      layoutState.mAccessibilityManager =
          (AccessibilityManager) c.getSystemService(ACCESSIBILITY_SERVICE);
      layoutState.mAccessibilityEnabled = isAccessibilityEnabled(layoutState.mAccessibilityManager);
      layoutState.mComponent = component;
      layoutState.mWidthSpec = widthSpec;
      layoutState.mHeightSpec = heightSpec;
      layoutState.mCanPrefetchDisplayLists = canPrefetchDisplayLists;
      layoutState.mCanCacheDrawingDisplayLists = canCacheDrawingDisplayLists;
      layoutState.mClipChildren = clipChildren;

      final InternalNode root =
          component.mLayoutCreatedInWillRender == null
              ? createAndMeasureTreeForComponent(
                  c,
                  component,
                  null, // nestedTreeHolder is null because this is measuring the root component
                        // tree.
                  widthSpec,
                  heightSpec,
                  previousDiffTreeRoot)
              : component.mLayoutCreatedInWillRender;

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
        return layoutState;
      }

      layoutState.mLayoutRoot = root;
      layoutState.mRootTransitionKey = root.getTransitionKey();

      if (isTracing) {
        ComponentsSystrace.beginSection("collectResults:" + component.getSimpleName());
      }

      LogEvent collectResultsEvent = null;
      if (logger != null) {
        collectResultsEvent = logger.newPerformanceEvent(EVENT_COLLECT_RESULTS);
        collectResultsEvent.addParam(PARAM_LOG_TAG, c.getLogTag());
      }

      collectResults(root, layoutState, null);

      Collections.sort(layoutState.mMountableOutputTops, sTopsComparator);
      Collections.sort(layoutState.mMountableOutputBottoms, sBottomsComparator);

      if (logger != null) {
        logger.log(collectResultsEvent);
      }

      if (isTracing) {
        ComponentsSystrace.endSection();
      }

      if (!persistInternalNodeTree
          && !ComponentsConfiguration.isDebugModeEnabled
          && !ComponentsConfiguration.isEndToEndTestRun
          && layoutState.mLayoutRoot != null) {
        releaseNodeTree(layoutState.mLayoutRoot, false /* isNestedTree */);
        layoutState.mLayoutRoot = null;
      }

      final Activity activity = getValidActivityForContext(c);
      if (activity != null && isEligibleForCreatingDisplayLists()) {
        if (ThreadUtils.isMainThread()
            && !layoutState.mCanPrefetchDisplayLists
            && canCollectDisplayListsSync(activity)) {
          collectDisplayLists(layoutState);
        } else if (layoutState.mCanPrefetchDisplayLists) {
          queueDisplayListsForPrefetch(layoutState);
        }
      }

      layoutState.mCalculateLayoutDuration = System.nanoTime() - timestampStartLayout;
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
        if (extraAttribution != null) {
          ComponentsSystrace.endSection();
        }
      }
    }

    if (logger != null) {
      logger.log(logLayoutState);
    }

    return layoutState;
  }

  private static String sourceToString(@CalculateLayoutSource int source) {
    switch (source) {
      case CalculateLayoutSource.SET_ROOT:
        return "setRoot";
      case CalculateLayoutSource.SET_SIZE_SPEC:
        return "setSizeSpec";
      case CalculateLayoutSource.UPDATE_STATE:
        return "updateState";
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
  void preAllocateMountContent(boolean shouldPreallocatePerMountSpec) {
    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("preAllocateMountContent:" + mComponent.getSimpleName());
    }

    if (mMountableOutputs != null && !mMountableOutputs.isEmpty()) {
      for (int i = 0, size = mMountableOutputs.size(); i < size; i++) {
        final Component component = mMountableOutputs.get(i).getComponent();

        if (shouldPreallocatePerMountSpec && !component.canPreallocate()) {
          continue;
        }

        if (Component.isMountViewSpec(component)) {
          if (isTracing) {
            ComponentsSystrace.beginSection("preAllocateMountContent:" + component.getSimpleName());
          }

          ComponentsPools.maybePreallocateContent(mContext, component);

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

  private static void collectDisplayLists(LayoutState layoutState) {
    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection(
          "collectDisplayLists:" + layoutState.mComponent.getSimpleName());
    }

    final Rect rect = layoutState.mDisplayListCreateRect;

    for (int i = 0, count = layoutState.getMountableOutputCount(); i < count; i++) {
      final LayoutOutput output = layoutState.getMountableOutputAt(i);
      if (shouldCreateDisplayList(output, rect)) {
        layoutState.createDisplayList(output);
      }
    }
    if (isTracing) {
      ComponentsSystrace.endSection();
    }
  }

  private static boolean shouldCreateDisplayList(LayoutOutput output, Rect rect) {
    final Component component = output.getComponent();
    final ComponentLifecycle lifecycle = component;

    if (!lifecycle.shouldUseDisplayList()) {
      return false;
    }

    output.getMountBounds(rect);

    if (!output.hasValidDisplayList()) {
      return true;
    }

    // This output already has a valid DisplayList from diffing. No need to re-create it.
    // Just update its bounds.
    final DisplayList displayList = output.getDisplayList();
    try {
      displayList.setBounds(rect.left, rect.top, rect.right, rect.bottom);
      return false;
    } catch (DisplayListException e) {
      // Nothing to do here.
    }

    return true;
  }

  private static boolean canCollectDisplayListsSync(Activity activity) {
    // If we have no window or the hierarchy has never been drawn before we cannot guarantee that
    // a valid GL context exists. In this case just bail.
    final Window window = activity.getWindow();
    if (window == null) {
      return false;
    }

    final View decorView = window.getDecorView();
    if (decorView == null || decorView.getDrawingTime() == 0) {
      return false;
    }

    return true;
  }

  boolean isActivityValid() {
    return getValidActivityForContext(mContext) != null;
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
      boolean isCachedOutputUpdated) {
    if (mLayoutStateOutputIdCalculator == null) {
      mLayoutStateOutputIdCalculator = new LayoutStateOutputIdCalculator();
    }
    mLayoutStateOutputIdCalculator.calculateAndSetLayoutOutputIdAndUpdateState(
        layoutOutput, level, type, previousId, isCachedOutputUpdated);
  }

  private void calculateAndSetVisibilityOutputId(
      VisibilityOutput visibilityOutput, int level, long previousId) {
    if (mLayoutStateOutputIdCalculator == null) {
      mLayoutStateOutputIdCalculator = new LayoutStateOutputIdCalculator();
    }
    mLayoutStateOutputIdCalculator.calculateAndSetVisibilityOutputId(
        visibilityOutput, level, previousId);
  }

  void createDisplayList(LayoutOutput output) {
    ThreadUtils.assertMainThread();

    final ComponentContext context = mContext;
    if (context == null) {
      // This instance has been released.
      return;
    }

    final Component component = output.getComponent();
    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("createDisplayList: " + component.getSimpleName());
    }

    final ComponentLifecycle lifecycle = component;
    final DisplayList displayList = DisplayList.createDisplayList(
        lifecycle.getClass().getSimpleName());

    if (displayList == null) {
      ComponentsSystrace.endSection();
      return;
    }

    Drawable drawable = (Drawable) ComponentsPools.acquireMountContent(context, lifecycle);

    final LayoutOutput clickableOutput = findInteractiveRoot(this, output);
    boolean isStateEnabled = false;

    if (clickableOutput != null && clickableOutput.getNodeInfo() != null) {
      final NodeInfo nodeInfo = clickableOutput.getNodeInfo();

      if (nodeInfo.hasTouchEventHandlers() || nodeInfo.getFocusState() == FOCUS_SET_TRUE) {
        isStateEnabled = true;
      }
    }

    if (isStateEnabled) {
      drawable.setState(DRAWABLE_STATE_ENABLED);
    } else {
      drawable.setState(DRAWABLE_STATE_NOT_ENABLED);
    }

    lifecycle.mount(
        component.getScopedContext() != null ? component.getScopedContext() : context,
        drawable);
    lifecycle.bind(context, drawable);

    final Rect rect = mDisplayListCreateRect;

    output.getMountBounds(rect);
    drawable.setBounds(0, 0, rect.width(), rect.height());

    try {
      final Canvas canvas = displayList.start(rect.width(), rect.height());
      drawable.draw(canvas);

      displayList.end(canvas);
      displayList.setBounds(rect.left, rect.top, rect.right, rect.bottom);

      output.setDisplayList(displayList);
    } catch (DisplayListException e) {
      // Display list creation failed. Make sure the DisplayList for this output is set
      // to null.
      output.setDisplayList(null);
    }

    lifecycle.unbind(context, drawable);
    lifecycle.unmount(context, drawable);
    ComponentsPools.release(context, lifecycle, drawable);

    if (isTracing) {
      ComponentsSystrace.endSection();
    }
  }

  private static void queueDisplayListsForPrefetch(LayoutState layoutState) {
    final Rect rect = layoutState.mDisplayListQueueRect;

    for (int i = 0, count = layoutState.getMountableOutputCount(); i < count; i++) {
      final LayoutOutput output = layoutState.getMountableOutputAt(i);
      if (shouldCreateDisplayList(output, rect)) {
        layoutState.mDisplayListsToPrefetch.add(i);
      }
    }

    if (!layoutState.mDisplayListsToPrefetch.isEmpty()) {
      DisplayListPrefetcher.getInstance().addLayoutState(layoutState);
    }
  }

  public static boolean isEligibleForCreatingDisplayLists() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
  }

  private static LayoutOutput findInteractiveRoot(LayoutState layoutState, LayoutOutput output) {
    if (output.getId() == ROOT_HOST_ID) {
      return output;
    }

    if ((output.getFlags() & LAYOUT_FLAG_DUPLICATE_PARENT_STATE) != 0) {
      final int parentPosition = layoutState.getLayoutOutputPositionForId(output.getHostMarker());
      if (parentPosition >= 0) {
        final LayoutOutput parent = layoutState.mMountableOutputs.get(parentPosition);
        if (parent == null) {
          return null;
        }

        return findInteractiveRoot(layoutState, parent);
      }

      return null;
    }

    return output;
  }

  @VisibleForTesting
  static InternalNode createTree(
      Component component,
      ComponentContext context) {
    final ComponentsLogger logger = context.getLogger();

    final PerfEvent createLayoutPerfEvent =
        logger != null ? logger.newBetterPerformanceEvent(EVENT_CREATE_LAYOUT) : null;

    if (createLayoutPerfEvent != null) {
      createLayoutPerfEvent.markerAnnotate(PARAM_LOG_TAG, context.getLogTag());
      createLayoutPerfEvent.markerAnnotate(PARAM_COMPONENT, component.getSimpleName());
      LogTreePopulator.populatePerfEventFromLogger(context, logger, createLayoutPerfEvent);
    }

    final InternalNode root = component.createLayout(context, true /* resolveNestedTree */);

    if (createLayoutPerfEvent != null) {
      logger.betterLog(createLayoutPerfEvent);
    }

    return root;
  }

  @VisibleForTesting
  static void measureTree(
      InternalNode root,
      int widthSpec,
      int heightSpec,
      DiffNode previousDiffTreeRoot) {
    final ComponentContext context = root.getContext();
    final Component component = root.getRootComponent();
    final boolean isTracing = ComponentsSystrace.isTracing();

    if (isTracing) {
      ComponentsSystrace.beginSection("measureTree:" + component.getSimpleName());
    }

    if (YogaConstants.isUndefined(root.getStyleWidth())) {
      root.setStyleWidthFromSpec(widthSpec);
    }
    if (YogaConstants.isUndefined(root.getStyleHeight())) {
      root.setStyleHeightFromSpec(heightSpec);
    }

    if (previousDiffTreeRoot != null) {
      ComponentsSystrace.beginSection("applyDiffNode");
      applyDiffNodeToUnchangedNodes(root, previousDiffTreeRoot);
      ComponentsSystrace.endSection(/* applyDiffNode */);
    }

    final ComponentsLogger logger = context.getLogger();
    final PerfEvent layoutEvent =
        logger != null ? logger.newBetterPerformanceEvent(EVENT_CSS_LAYOUT) : null;

    if (layoutEvent != null) {
      layoutEvent.markerAnnotate(PARAM_LOG_TAG, context.getLogTag());
      layoutEvent.markerAnnotate(PARAM_TREE_DIFF_ENABLED, previousDiffTreeRoot != null);
    }

    root.calculateLayout(
        SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED
            ? YogaConstants.UNDEFINED
            : SizeSpec.getSize(widthSpec),
        SizeSpec.getMode(heightSpec) == SizeSpec.UNSPECIFIED
            ? YogaConstants.UNDEFINED
            : SizeSpec.getSize(heightSpec));

    if (layoutEvent != null) {
      LogTreePopulator.populatePerfEventFromLogger(context, logger, layoutEvent);
      logger.betterLog(layoutEvent);
    }

    if (isTracing) {
      ComponentsSystrace.endSection(/* measureTree */ );
    }
  }

  /**
   * Create and measure the nested tree or return the cached one for the same size specs.
   */
  static InternalNode resolveNestedTree(
      InternalNode nestedTreeHolder,
      int widthSpec,
      int heightSpec) {
    final ComponentContext context = nestedTreeHolder.getContext();
    final Component component = nestedTreeHolder.getRootComponent();

    InternalNode nestedTree =
        component.mLayoutCreatedInWillRender == null
            ? nestedTreeHolder.getNestedTree()
            : component.mLayoutCreatedInWillRender;

    if (nestedTree == null
        || !hasCompatibleSizeSpec(
        nestedTree.getLastWidthSpec(),
        nestedTree.getLastHeightSpec(),
        widthSpec,
        heightSpec,
        nestedTree.getLastMeasuredWidth(),
        nestedTree.getLastMeasuredHeight())) {
      if (nestedTree != null) {
        if (nestedTree != NULL_LAYOUT) {
          releaseNodeTree(nestedTree, true /* isNestedTree */);
        }

        nestedTree = null;
      }

      if (component.hasCachedLayout()) {
        final InternalNode cachedLayout = component.getCachedLayout();
        final boolean hasCompatibleLayoutDirection =
            InternalNode.hasValidLayoutDirectionInNestedTree(nestedTreeHolder, cachedLayout);

        // Transfer the cached layout to the node without releasing it if it's compatible.
        if (hasCompatibleLayoutDirection &&
            hasCompatibleSizeSpec(
                cachedLayout.getLastWidthSpec(),
                cachedLayout.getLastHeightSpec(),
                widthSpec,
                heightSpec,
                cachedLayout.getLastMeasuredWidth(),
                cachedLayout.getLastMeasuredHeight())) {
          nestedTree = cachedLayout;
          component.clearCachedLayout();
        } else {
          component.releaseCachedLayout();
        }
      }

      if (nestedTree == null) {
        nestedTree = createAndMeasureTreeForComponent(
            context,
            component,
            nestedTreeHolder,
            widthSpec,
            heightSpec,
            nestedTreeHolder.getDiffNode()); // Previously set while traversing the holder's tree.
        nestedTree.setLastWidthSpec(widthSpec);
        nestedTree.setLastHeightSpec(heightSpec);
        nestedTree.setLastMeasuredHeight(nestedTree.getHeight());
        nestedTree.setLastMeasuredWidth(nestedTree.getWidth());
      }

      nestedTreeHolder.setNestedTree(nestedTree);
    }

    // This is checking only nested tree roots however it will be moved to check all the tree roots.
    InternalNode.assertContextSpecificStyleNotSet(nestedTree);

    return nestedTree;
  }

  /**
   * Create and measure a component with the given size specs.
   */
  static InternalNode createAndMeasureTreeForComponent(
      ComponentContext c,
      Component component,
      int widthSpec,
      int heightSpec) {
    return createAndMeasureTreeForComponent(c, component, null, widthSpec, heightSpec, null);
  }

  private static InternalNode createAndMeasureTreeForComponent(
      ComponentContext c,
      Component component,
      InternalNode nestedTreeHolder, // This will be set only if we are resolving a nested tree.
      int widthSpec,
      int heightSpec,
      DiffNode diffTreeRoot) {

    component.updateInternalChildState(c);

    if (ComponentsConfiguration.isDebugModeEnabled) {
      DebugComponent.applyOverrides(c, component);
    }

    c = component.getScopedContext();

    final boolean isTest = "robolectric".equals(Build.FINGERPRINT);
    // Copy the context so that it can have its own set of tree props.
    // Robolectric tests keep the context so that tree props can be set externally.
    if (!isTest) {
      c = c.makeNewCopy();
    }

    final boolean hasNestedTreeHolder = nestedTreeHolder != null;
    if (hasNestedTreeHolder) {
      c.setTreeProps(nestedTreeHolder.getPendingTreeProps());
    } else if (!isTest) {
      c.setTreeProps(null);
    }

    // Account for the size specs in ComponentContext in case the tree is a NestedTree.
    final int previousWidthSpec = c.getWidthSpec();
    final int previousHeightSpec = c.getHeightSpec();

    c.setWidthSpec(widthSpec);
    c.setHeightSpec(heightSpec);

    final InternalNode root = createTree(
        component,
        c);

    if (hasNestedTreeHolder) {
      c.setTreeProps(null);
    }

    c.setWidthSpec(previousWidthSpec);
    c.setHeightSpec(previousHeightSpec);

    if (root == NULL_LAYOUT) {
      return root;
    }

    // If measuring a ComponentTree with a LayoutSpecWithSizeSpec at the root, the nested tree
    // holder argument will be null.
    if (hasNestedTreeHolder && isLayoutSpecWithSizeSpec(component)) {
      // Transfer information from the holder node to the nested tree root before measurement.
      nestedTreeHolder.copyInto(root);
      diffTreeRoot = nestedTreeHolder.getDiffNode();
    } else if (root.getStyleDirection() == com.facebook.yoga.YogaDirection.INHERIT
        && LayoutState.isLayoutDirectionRTL(c)) {
      root.layoutDirection(YogaDirection.RTL);
    }

    measureTree(
        root,
        widthSpec,
        heightSpec,
        diffTreeRoot);

    return root;
  }

  static DiffNode createDiffNode(InternalNode node, DiffNode parent) {
    ComponentsSystrace.beginSection("diff_node_creation");
    DiffNode diffNode = ComponentsPools.acquireDiffNode();

    diffNode.setLastWidthSpec(node.getLastWidthSpec());
    diffNode.setLastHeightSpec(node.getLastHeightSpec());
    diffNode.setLastMeasuredWidth(node.getLastMeasuredWidth());
    diffNode.setLastMeasuredHeight(node.getLastMeasuredHeight());
    diffNode.setComponent(node.getRootComponent());
    if (parent != null) {
      parent.addChild(diffNode);
    }

    ComponentsSystrace.endSection();

    return diffNode;
  }

  boolean isCompatibleSpec(int widthSpec, int heightSpec) {
    final boolean widthIsCompatible =
        MeasureComparisonUtils.isMeasureSpecCompatible(
            mWidthSpec,
            widthSpec,
            mWidth);

    final boolean heightIsCompatible =
        MeasureComparisonUtils.isMeasureSpecCompatible(
            mHeightSpec,
            heightSpec,
            mHeight);

    return widthIsCompatible && heightIsCompatible;
  }

  boolean isCompatibleAccessibility() {
    return isAccessibilityEnabled(mAccessibilityManager) == mAccessibilityEnabled;
  }

  private static boolean isAccessibilityEnabled(AccessibilityManager accessibilityManager) {
    return accessibilityManager.isEnabled() &&
        AccessibilityManagerCompat.isTouchExplorationEnabled(accessibilityManager);
  }

  /**
   * Traverses the layoutTree and the diffTree recursively. If a layoutNode has a compatible host
   * type {@link LayoutState#hostIsCompatible} it assigns the DiffNode to the layout node in order
   * to try to re-use the LayoutOutputs that will be generated by {@link
   * LayoutState#collectResults(InternalNode, LayoutState, DiffNode)}. If a layoutNode
   * component returns false when shouldComponentUpdate is called with the DiffNode Component it
   * also tries to re-use the old measurements and therefore marks as valid the cachedMeasures for
   * the whole component subtree.
   *
   * @param layoutNode the root of the LayoutTree
   * @param diffNode the root of the diffTree
   *
   * @return true if the layout node requires updating, false if it can re-use the measurements
   *              from the diff node.
   */
  static boolean applyDiffNodeToUnchangedNodes(InternalNode layoutNode, DiffNode diffNode) {
    // Root of the main tree or of a nested tree.
    final boolean isTreeRoot = layoutNode.getParent() == null;
    if (isLayoutSpecWithSizeSpec(layoutNode.getRootComponent()) && !isTreeRoot) {
      layoutNode.setDiffNode(diffNode);
      return true;
    }

    if (!hostIsCompatible(layoutNode, diffNode)) {
      return true;
    }

    layoutNode.setDiffNode(diffNode);

    final int layoutCount = layoutNode.getChildCount();
    final int diffCount = diffNode.getChildCount();

    // Layout node needs to be updated if:
    //   - it has a different number of children.
    //   - one of its children needs updating.
    //   - the node itself declares that it needs updating.
    boolean shouldUpdate = layoutCount != diffCount;
    for (int i = 0; i < layoutCount && i < diffCount; i++) {
      // ensure that we always run for all children.
      boolean shouldUpdateChild =
          applyDiffNodeToUnchangedNodes(
              layoutNode.getChildAt(i),
              diffNode.getChildAt(i));
      shouldUpdate |= shouldUpdateChild;
    }

    shouldUpdate |= shouldComponentUpdate(layoutNode, diffNode);

    if (!shouldUpdate) {
      applyDiffNodeToLayoutNode(layoutNode, diffNode);
    }

    return shouldUpdate;
  }

  /**
   * Copies the inter stage state (if any) from the DiffNode's component to the layout node's
   * component, and declares that the cached measures on the diff node are valid for the layout
   * node.
   */
  private static void applyDiffNodeToLayoutNode(InternalNode layoutNode, DiffNode diffNode) {
    final Component component = layoutNode.getRootComponent();
    if (component != null) {
      component.copyInterStageImpl(diffNode.getComponent());
    }

    layoutNode.setCachedMeasuresValid(true);
  }

  /**
   * Returns true either if the two nodes have the same Component type or if both don't have a
   * Component.
   */
  private static boolean hostIsCompatible(InternalNode node, DiffNode diffNode) {
    if (diffNode == null) {
      return false;
    }

    return isSameComponentType(node.getRootComponent(), diffNode.getComponent());
  }

  private static boolean isSameComponentType(Component a, Component b) {
    if (a == b) {
      return true;
    } else if (a == null || b == null) {
      return false;
    }
    return a.getClass().equals(b.getClass());
  }

  private static boolean shouldComponentUpdate(InternalNode layoutNode, DiffNode diffNode) {
    if (diffNode == null) {
      return true;
    }

    final Component component = layoutNode.getRootComponent();
    if (component != null) {
      return component.shouldComponentUpdate(component, diffNode.getComponent());
    }

    return true;
  }

  boolean isCompatibleComponentAndSpec(
      int componentId,
      int widthSpec,
      int heightSpec) {
    return mComponent.getId() == componentId && isCompatibleSpec(widthSpec, heightSpec);
  }

  boolean isCompatibleSize(int width, int height) {
    return mWidth == width && mHeight == height;
  }

  boolean isForComponentId(int componentId) {
    return mComponent.getId() == componentId;
  }

  int getMountableOutputCount() {
    return mMountableOutputs.size();
  }

  LayoutOutput getMountableOutputAt(int index) {
    return mMountableOutputs.get(index);
  }

  ArrayList<LayoutOutput> getMountableOutputTops() {
    return mMountableOutputTops;
  }

  ArrayList<LayoutOutput> getMountableOutputBottoms() {
    return mMountableOutputBottoms;
  }

  int getVisibilityOutputCount() {
    return mVisibilityOutputs.size();
  }

  VisibilityOutput getVisibilityOutputAt(int index) {
    return mVisibilityOutputs.get(index);
  }

  int getTestOutputCount() {
    return mTestOutputs == null ? 0 : mTestOutputs.size();
  }

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

  /**
   * @return The id of the {@link ComponentTree} that generated this {@link LayoutState}
   */
  int getComponentTreeId() {
    return mComponentTreeId;
  }

  /**
   * See {@link LayoutState#acquireRef} Call this when you are done using the reference to the
   * LayoutState.
   */
  @ThreadSafe(enableChecks = false)
  void releaseRef() {
    int count = mReferenceCount.decrementAndGet();
    if (count < 0) {
      throw new IllegalStateException("Trying to releaseRef a recycled LayoutState");
    }
    if (count == 0) {
      mContext = null;
      mComponent = null;

      mWidth = 0;
      mHeight = 0;

      mCurrentX = 0;
      mCurrentY = 0;
      mCurrentHostMarker = -1;
      mCurrentHostOutputPosition = -1;
      mComponentTreeId = -1;

      mShouldDuplicateParentState = true;
      mClipChildren = true;

      for (int i = 0, size = mMountableOutputs.size(); i < size; i++) {
        mMountableOutputs.get(i).release();
      }
      mMountableOutputs.clear();
      mMountableOutputTops.clear();
      mMountableOutputBottoms.clear();
      mOutputsIdToPositionMap.clear();
      mDisplayListsToPrefetch.clear();

      for (Rect rect : mComponentKeyToBounds.values()) {
        ComponentsPools.release(rect);
      }
      mComponentKeyToBounds.clear();

      for (int i = 0, size = mVisibilityOutputs.size(); i < size; i++) {
        ComponentsPools.release(mVisibilityOutputs.get(i));
      }
      mVisibilityOutputs.clear();

      if (mTestOutputs != null) {
        for (int i = 0, size = mTestOutputs.size(); i < size; i++) {
          ComponentsPools.release(mTestOutputs.get(i));
        }
        mTestOutputs.clear();
      }

      mShouldGenerateDiffTree = false;
      mAccessibilityManager = null;
      mAccessibilityEnabled = false;

      if (mDiffTreeRoot != null) {
        ComponentsPools.release(mDiffTreeRoot);
        mDiffTreeRoot = null;
      }
      clearLayoutStateOutputIdCalculator();

      if (mTransitions != null) {
        mTransitions.clear();
        mTransitions = null;
      }

      // This should only ever be true in non-release builds as we need this for Stetho integration
      // (or for as long as the ComponentsConfiguration.persistInternalNodeTree experiment runs).
      // Otherwise, in release builds the node tree is released in calculateLayout().
      if (mLayoutRoot != null) {
        releaseNodeTree(mLayoutRoot, false /* isNestedTree */);
        mLayoutRoot = null;
      }

      mRootTransitionKey = null;

      if (mComponentsNeedingPreviousRenderData != null) {
        mComponentsNeedingPreviousRenderData.clear();
      }

      mCurrentLayoutOutputAffinityGroup = null;
      mTransitionKeyMapping.clear();

      mWorkingRangeContainer = null;

      ComponentsPools.release(this);
    }
  }

  /**
   * The lifecycle of LayoutState is generally controlled by ComponentTree. Since sometimes we need
   * an old LayoutState to be passed to a new LayoutState to implement Tree diffing, We use
   * reference counting to avoid releasing a LayoutState that is not used by ComponentTree anymore
   * but could be used by another LayoutState. The rule is that whenever you need to pass the
   * LayoutState outside of ComponentTree, you acquire a reference and then you release it as soon
   * as you are done with it
   *
   * @return The same LayoutState instance with an higher reference count.
   */
  @CheckReturnValue
  LayoutState acquireRef() {
    if (mReferenceCount.getAndIncrement() == 0) {
      throw new IllegalStateException("Trying to use a released LayoutState");
    }

    return this;
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
   * Check if a cached nested tree has compatible SizeSpec to be reused as is or
   * if it needs to be recomputed.
   *
   * The conditions to be able to re-use previous measurements are:
   * 1) The measureSpec is the same
   * 2) The new measureSpec is EXACTLY and the last measured size matches the measureSpec size.
   * 3) The old measureSpec is UNSPECIFIED, the new one is AT_MOST and the old measured size is
   *    smaller that the maximum size the new measureSpec will allow.
   * 4) Both measure specs are AT_MOST. The old measure spec allows a bigger size than the new and
   *    the old measured size is smaller than the allowed max size for the new sizeSpec.
   */
  public static boolean hasCompatibleSizeSpec(
      int oldWidthSpec,
      int oldHeightSpec,
      int newWidthSpec,
      int newHeightSpec,
      float oldMeasuredWidth,
      float oldMeasuredHeight) {
    final boolean widthIsCompatible =
        MeasureComparisonUtils.isMeasureSpecCompatible(
            oldWidthSpec,
            newWidthSpec,
            (int) oldMeasuredWidth);

    final boolean heightIsCompatible =
        MeasureComparisonUtils.isMeasureSpecCompatible(
            oldHeightSpec,
            newHeightSpec,
            (int) oldMeasuredHeight);
    return  widthIsCompatible && heightIsCompatible;
  }


  /**
   * Returns true if this is the root node (which always generates a matching layout
   * output), if the node has view attributes e.g. tags, content description, etc, or if
   * the node has explicitly been forced to be wrapped in a view.
   */
  private static boolean needsHostView(InternalNode node, LayoutState layoutState) {
    return layoutState.isLayoutRoot(node)
        || (!isMountViewSpec(node.getRootComponent())
            && (hasViewContent(node, layoutState) || node.isForceViewWrapping()))
        || needsToBeWrappedForTransition(node);
  }

  private static boolean needsToBeWrappedForTransition(InternalNode node) {
    if (isMountViewSpec(node.getRootComponent()) || TextUtils.isEmpty(node.getTransitionKey())) {
      return false;
    }
    if (!ComponentsConfiguration.doNotForceWrappingInViewForAnimation) {
      return true;
    }
    return !isMountDrawableSpec(node.getRootComponent()) && node.getChildCount() > 0;
  }

  /**
   * @return the position of the {@link LayoutOutput} with id layoutOutputId in the
   * {@link LayoutState} list of outputs or -1 if no {@link LayoutOutput} with that id exists in
   * the {@link LayoutState}
   */
  int getLayoutOutputPositionForId(long layoutOutputId) {
    return mOutputsIdToPositionMap.get(layoutOutputId, -1);
  }

  @Nullable
  List<Transition> getTransitions() {
    return mTransitions;
  }

  /** Gets a mapping from transition key to a group of LayoutOutput. */
  SimpleArrayMap<String, OutputUnitsAffinityGroup<LayoutOutput>> getTransitionKeyMapping() {
    return mTransitionKeyMapping;
  }

  OutputUnitsAffinityGroup<LayoutOutput> getLayoutOutputsForTransitionKey(String key) {
    return mTransitionKeyMapping.get(key);
  }

  private static void addMountableOutput(LayoutState layoutState, LayoutOutput layoutOutput) {
    layoutOutput.setIndex(layoutState.mMountableOutputs.size());

    layoutState.mMountableOutputs.add(layoutOutput);
    layoutState.mMountableOutputTops.add(layoutOutput);
    layoutState.mMountableOutputBottoms.add(layoutOutput);
  }

  /** @return whether there are any items in the queue for Display Lists prefetching. */
  boolean hasItemsForDLPrefetch() {
    return !mDisplayListsToPrefetch.isEmpty();
  }

  /**
   * Remove items that have already valid displaylist. This item might have been already drawn on
   * the screen in which case we will have valid displaylist so we can skip them.
   */
  void trimDisplayListItemsQueue() {
    if (mMountableOutputs.isEmpty()) {
      // Item has been released, remove all pending items for displaylist prefetch.
      mDisplayListsToPrefetch.clear();
      return;
    }
    Integer currentIndex = mDisplayListsToPrefetch.peek();
    while (currentIndex != null) {
      final LayoutOutput layoutOutput = mMountableOutputs.get(currentIndex);
      if (!layoutOutput.hasDisplayListContainer() || layoutOutput.hasValidDisplayList()) {
        // Either this item has been released or we have already computed displaylist for this item.
        // In either case remove it from the queue.
        mDisplayListsToPrefetch.remove();
        currentIndex = mDisplayListsToPrefetch.peek();
      } else {
        break;
      }
    }
  }

  /**
   * Removes and returns next {@link LayoutOutput} from the queue for Display Lists.
   * Note that it is callers responsibility to make sure queue is not empty.
   */
  LayoutOutput getNextLayoutOutputForDLPrefetch() {
    final int layoutOutputIndex = mDisplayListsToPrefetch.poll();
    return getMountableOutputAt(layoutOutputIndex);
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
  String getRootTransitionKey() {
    return mRootTransitionKey;
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

  private static String getTransitionKeyForNode(InternalNode node) {
    if (node.hasTransitionKey()) {
      return node.getTransitionKey();
    }
    if (ComponentsConfiguration.assignTransitionKeysToAllOutputs) {
      return node.getRootComponent().getGlobalKey();
    }
    return null;
  }
}
