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
import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO;
import static com.facebook.litho.Component.isLayoutSpecWithSizeSpec;
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
import static com.facebook.litho.FrameworkLogEvents.PARAM_ROOT_COMPONENT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_TREE_DIFF_ENABLED;
import static com.facebook.litho.MountItem.LAYOUT_FLAG_DISABLE_TOUCHABLE;
import static com.facebook.litho.MountItem.LAYOUT_FLAG_DUPLICATE_PARENT_STATE;
import static com.facebook.litho.MountItem.LAYOUT_FLAG_MATCH_HOST_BOUNDS;
import static com.facebook.litho.MountItem.LAYOUT_FLAG_PHANTOM;
import static com.facebook.litho.MountState.ROOT_HOST_ID;
import static com.facebook.litho.NodeInfo.CLICKABLE_SET_TRUE;
import static com.facebook.litho.NodeInfo.ENABLED_SET_FALSE;
import static com.facebook.litho.NodeInfo.ENABLED_UNSET;
import static com.facebook.litho.NodeInfo.FOCUS_SET_TRUE;
import static com.facebook.litho.SizeSpec.EXACTLY;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Rect;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.LongSparseArray;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.litho.annotations.ImportantForAccessibility;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.drawable.BorderColorDrawable;
import com.facebook.litho.drawable.ComparableDrawable;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
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
 * #collectResults(ComponentContext, InternalNode, LayoutState, DiffNode)} which prepares the
 * before-mentioned outputs based on the provided {@link InternalNode} for later use in {@link
 * MountState}.
 */
class LayoutState {

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
  @Retention(RetentionPolicy.SOURCE)
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

  private static final AtomicInteger sIdGenerator = new AtomicInteger(1);
  private static final int NO_PREVIOUS_LAYOUT_STATE_ID = -1;

  private final Map<String, Rect> mComponentKeyToBounds = new HashMap<>();
  private final List<Component> mComponents = new ArrayList<>();

  private final ComponentContext mContext;

  private Component mComponent;

  private int mWidthSpec;
  private int mHeightSpec;

  private final List<LayoutOutput> mMountableOutputs = new ArrayList<>(8);
  private final List<VisibilityOutput> mVisibilityOutputs = new ArrayList<>(8);
  private final LongSparseArray<Integer> mOutputsIdToPositionMap = new LongSparseArray<>(8);
  private final ArrayList<LayoutOutput> mMountableOutputTops = new ArrayList<>();
  private final ArrayList<LayoutOutput> mMountableOutputBottoms = new ArrayList<>();

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
  private final int mId;
  // Id of the layout state (if any) that was used in comparisons with this layout state.
  private int mPreviousLayoutStateId = NO_PREVIOUS_LAYOUT_STATE_ID;

  private AccessibilityManager mAccessibilityManager;
  private boolean mAccessibilityEnabled = false;

  private StateHandler mStateHandler;
  private boolean mClipChildren = true;
  private List<Component> mComponentsNeedingPreviousRenderData;
  @Nullable private TransitionId mCurrentTransitionId;
  @Nullable private OutputUnitsAffinityGroup<LayoutOutput> mCurrentLayoutOutputAffinityGroup;
  private final Map<TransitionId, OutputUnitsAffinityGroup<LayoutOutput>> mTransitionIdMapping =
      new LinkedHashMap<>();
  private final Set<TransitionId> mDuplicatedTransitionIds = new HashSet<>();
  private List<Transition> mTransitions;
  private final int mOrientation;

  private static final Object debugLock = new Object();
  @Nullable private static Map<Integer, List<Boolean>> layoutCalculationsOnMainThread;

  @Nullable WorkingRangeContainer mWorkingRangeContainer;

  LayoutState(ComponentContext context) {
    mContext = context;
    mId = sIdGenerator.getAndIncrement();
    mStateHandler = mContext.getStateHandler();
    mTestOutputs = ComponentsConfiguration.isEndToEndTestRun ? new ArrayList<TestOutput>(8) : null;
    mOrientation = context.getResources().getConfiguration().orientation;

    if (!ComponentsConfiguration.lazilyInitializeLayoutStateOutputIdCalculator) {
      mLayoutStateOutputIdCalculator = new LayoutStateOutputIdCalculator();
    }
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
        hasHostView,
        false);
  }

  private static LayoutOutput createHostLayoutOutput(
      LayoutState layoutState, InternalNode node, boolean isPhantom) {
    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection(
          "createHostLayoutOutput:" + node.getSimpleName());
    }
    final LayoutOutput hostOutput =
        createLayoutOutput(
            HostComponent.create(),
            layoutState,
            node,
            false /* useNodePadding */,
            node.getImportantForAccessibility(),
            node.isDuplicateParentStateEnabled(),
            false,
            isPhantom);

    ViewNodeInfo viewNodeInfo = hostOutput.getViewNodeInfo();
    if (node.hasStateListAnimatorResSet()) {
      viewNodeInfo.setStateListAnimatorRes(node.getStateListAnimatorRes());
    } else {
      viewNodeInfo.setStateListAnimator(node.getStateListAnimator());
    }

    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    return hostOutput;
  }

  private static LayoutOutput createDrawableLayoutOutput(
      Component component, LayoutState layoutState, InternalNode node, boolean hasHostView) {
    final boolean isTracing = ComponentsSystrace.isTracing();
    try {
      if (isTracing) {
        ComponentsSystrace.beginSection("createDrawableLayoutOutput:" + node.getSimpleName());
      }
      return createLayoutOutput(
          component,
          layoutState,
          node,
          false /* useNodePadding */,
          IMPORTANT_FOR_ACCESSIBILITY_NO,
          layoutState.mShouldDuplicateParentState,
          hasHostView,
          false);
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }
  }

  private static LayoutOutput createLayoutOutput(
      Component component,
      LayoutState layoutState,
      InternalNode node,
      boolean useNodePadding,
      int importantForAccessibility,
      boolean duplicateParentState,
      boolean hasHostView,
      boolean isPhantom) {
    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("createLayoutOutput:" + node.getSimpleName());
    }
    final boolean isMountViewSpec = isMountViewSpec(component);

    final LayoutOutput layoutOutput = new LayoutOutput();
    layoutOutput.setComponent(component);
    layoutOutput.setImportantForAccessibility(importantForAccessibility);
    layoutOutput.setOrientation(layoutState.mOrientation);

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
    NodeInfo nodeInfo = node.getNodeInfo();
    if (isMountViewSpec) {
      layoutOutput.setNodeInfo(nodeInfo);
      // Acquire a ViewNodeInfo, set it up and release it after passing it to the LayoutOutput.
      final ViewNodeInfo viewNodeInfo = new ViewNodeInfo();
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
      layoutOutput.setViewNodeInfo(viewNodeInfo);
    } else {
      l += paddingLeft;
      t += paddingTop;
      r -= paddingRight;
      b -= paddingBottom;

      if (nodeInfo != null && nodeInfo.getEnabledState() == ENABLED_SET_FALSE) {
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
      layoutOutput.setTransitionId(layoutState.mCurrentTransitionId);
    }

    if (isPhantom) {
      flags |= LAYOUT_FLAG_PHANTOM;
    }

    layoutOutput.setFlags(flags);

    if (isTracing) {
      ComponentsSystrace.endSection();
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
    final EventHandler<VisibilityChangedEvent> visibleRectChangedEventHandler =
        node.getVisibilityChangedHandler();
    final VisibilityOutput visibilityOutput = new VisibilityOutput();

    visibilityOutput.setComponent(node.getRootComponent());
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
      InternalNode node,
      LayoutState layoutState,
      LayoutOutput layoutOutput) {
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
    final boolean isClickableSetTrue =
        (nodeInfo != null && nodeInfo.getClickableState() == CLICKABLE_SET_TRUE);
    final boolean hasClipChildrenSet = (nodeInfo != null && nodeInfo.isClipChildrenSet());

    return hasFocusChangeHandler
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
   * @param parentContext the parent component context
   * @param node InternalNode to process.
   * @param layoutState the LayoutState currently operating.
   * @param parentDiffNode whether this method also populates the diff tree and assigns the root
   */
  private static void collectResults(
      ComponentContext parentContext,
      InternalNode node,
      LayoutState layoutState,
      DiffNode parentDiffNode) {
    if (node.hasNewLayout()) {
      node.markLayoutSeen();
    }
    final Component component = node.getRootComponent();
    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("collectResults:" + node.getSimpleName());
    }

    // Early return if collecting results of a node holding a nested tree.
    if (node.isNestedTreeHolder()) {
      // If the nested tree is defined, it has been resolved during a measure call during
      // layout calculation.
      if (isTracing) {
        ComponentsSystrace.beginSectionWithArgs("resolveNestedTree:" + node.getSimpleName())
            .arg("widthSpec", "EXACTLY " + node.getWidth())
            .arg("heightSpec", "EXACTLY " + node.getHeight())
            .arg("rootComponentId", node.getRootComponent().getId())
            .flush();
      }
      InternalNode nestedTree =
          resolveNestedTree(
              parentContext.isNestedTreeResolutionExperimentEnabled()
                  ? parentContext
                  : node.getContext(),
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

      collectResults(parentContext, nestedTree, layoutState, parentDiffNode);

      layoutState.mCurrentX -= node.getX();
      layoutState.mCurrentY -= node.getY();

      if (isTracing) {
        ComponentsSystrace.endSection();
      }
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
    final boolean shouldUseCachedOutputs =
        isMountSpec(component) && currentDiffNode != null;
    final boolean isCachedOutputUpdated = shouldUseCachedOutputs && node.areCachedMeasuresValid();

    final DiffNode diffNode;
    if (shouldGenerateDiffTree) {
      if (isTracing) {
        ComponentsSystrace.beginSection("createDiffNode:" + node.getSimpleName());
      }
      diffNode = createDiffNode(node, parentDiffNode);
      if (isTracing) {
        ComponentsSystrace.endSection();
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
    final boolean needsPhantomLayoutOutput = !needsHostView && needsHostViewForTransition(node);
    final boolean shouldAddHostLayoutOutput = needsHostView || needsPhantomLayoutOutput;

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

    int hostLayoutPosition = -1;

    // 1. Insert a host LayoutOutput if we have some interactive content to be attached to.
    if (shouldAddHostLayoutOutput) {
      hostLayoutPosition =
          addHostLayoutOutput(node, layoutState, diffNode, needsPhantomLayoutOutput);
      addCurrentAffinityGroupToTransitionMapping(layoutState);

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
        shouldAddHostLayoutOutput
            || (shouldDuplicateParentState && node.isDuplicateParentStateEnabled());

    // Generate the layoutOutput for the given node.
    final LayoutOutput layoutOutput =
        createGenericLayoutOutput(node, layoutState, shouldAddHostLayoutOutput);
    if (layoutOutput != null) {
      final long previousId = shouldUseCachedOutputs ? currentDiffNode.getContent().getId() : -1;
      layoutState.calculateAndSetLayoutOutputIdAndUpdateState(
          layoutOutput,
          layoutState.mCurrentLevel,
          OutputUnitType.CONTENT,
          previousId,
          isCachedOutputUpdated);
    }

    // 2. Add background if defined.
    final ComparableDrawable background = node.getBackground();
    if (background != null) {
      if (layoutOutput != null && layoutOutput.hasViewNodeInfo()) {
        layoutOutput.getViewNodeInfo().setBackground(background);
      } else {
        final LayoutOutput convertBackground = (currentDiffNode != null)
            ? currentDiffNode.getBackground()
            : null;

        if (isTracing) {
          ComponentsSystrace.beginSection("addBgDrawableComponent:" + node.getSimpleName());
        }
        final LayoutOutput backgroundOutput =
            addDrawableComponent(
                node,
                layoutState,
                convertBackground,
                background,
                OutputUnitType.BACKGROUND,
                shouldAddHostLayoutOutput);

        if (diffNode != null) {
          diffNode.setBackground(backgroundOutput);
        }
        if (isTracing) {
          ComponentsSystrace.endSection();
        }
      }
    }

    // 3. Now add the MountSpec (either View or Drawable) to the Outputs.
    if (isMountSpec(component)) {
      // Notify component about its final size.
      if (isTracing) {
        ComponentsSystrace.beginSection("onBoundsDefined:" + node.getSimpleName());
      }
      component.onBoundsDefined(layoutState.mContext, node);
      if (isTracing) {
        ComponentsSystrace.endSection();
      }

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
    final Context scopedAndroidContext;
    if (component == null || component.getScopedContext() == null) {
      scopedAndroidContext = null;
    } else {
      scopedAndroidContext = component.getScopedContext().getAndroidContext();
    }

    if (TransitionUtils.areTransitionsEnabled(scopedAndroidContext)) {
      if (isTracing) {
        ComponentsSystrace.beginSection("extractTransitions:" + node.getSimpleName());
      }
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
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }

    layoutState.mCurrentX += node.getX();
    layoutState.mCurrentY += node.getY();
    @NodeInfo.EnabledState final int parentEnabledState = layoutState.mParentEnabledState;
    layoutState.mParentEnabledState = (node.getNodeInfo() != null)
        ? node.getNodeInfo().getEnabledState()
        : ENABLED_UNSET;

    // We must process the nodes in order so that the layout state output order is correct.
    for (int i = 0, size = node.getChildCount(); i < size; i++) {
      collectResults(node.getContext(), node.getChildAt(i), layoutState, diffNode);
    }

    layoutState.mParentEnabledState = parentEnabledState;
    layoutState.mCurrentX -= node.getX();
    layoutState.mCurrentY -= node.getY();

    // 5. Add border color if defined.
    if (node.shouldDrawBorders()) {
      final LayoutOutput convertBorder =
          (currentDiffNode != null) ? currentDiffNode.getBorder() : null;
      if (isTracing) {
        ComponentsSystrace.beginSection("addBorderDrawableComponent:" + node.getSimpleName());
      }
      final LayoutOutput borderOutput =
          addDrawableComponent(
              node,
              layoutState,
              convertBorder,
              getBorderColorDrawable(node),
              OutputUnitType.BORDER,
              shouldAddHostLayoutOutput);
      if (diffNode != null) {
        diffNode.setBorder(borderOutput);
      }
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }

    // 6. Add foreground if defined.
    final ComparableDrawable foreground = node.getForeground();
    if (foreground != null) {
      if (layoutOutput != null && layoutOutput.hasViewNodeInfo() && SDK_INT >= M) {
        layoutOutput.getViewNodeInfo().setForeground(foreground);
      } else {
        final LayoutOutput convertForeground = (currentDiffNode != null)
            ? currentDiffNode.getForeground()
            : null;

        if (isTracing) {
          ComponentsSystrace.beginSection("addFgDrawableComponent:" + node.getSimpleName());
        }
        final LayoutOutput foregroundOutput =
            addDrawableComponent(
                node,
                layoutState,
                convertForeground,
                foreground,
                OutputUnitType.FOREGROUND,
                shouldAddHostLayoutOutput);

        if (diffNode != null) {
          diffNode.setForeground(foregroundOutput);
        }
        if (isTracing) {
          ComponentsSystrace.endSection();
        }
      }
    }

    // 7. Add VisibilityOutputs if any visibility-related event handlers are present.
    if (node.hasVisibilityHandlers()) {
      if (isTracing) {
        ComponentsSystrace.beginSection("addVisibilityHandlers:" + node.getSimpleName());
      }
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
      if (isTracing) {
        ComponentsSystrace.endSection();
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
      if (isTracing) {
        ComponentsSystrace.beginSection("extractWorkingRanges:" + node.getSimpleName());
      }
      if (layoutState.mWorkingRangeContainer == null) {
        layoutState.mWorkingRangeContainer = new WorkingRangeContainer();
      }

      for (WorkingRangeContainer.Registration registration : registrations) {
        layoutState.mWorkingRangeContainer.registerWorkingRange(
            registration.mName, registration.mWorkingRange, registration.mComponent);
      }
      if (isTracing) {
        ComponentsSystrace.endSection();
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

      if (isTracing) {
        ComponentsSystrace.beginSection("keepComponentDelegates:" + node.getSimpleName());
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
          layoutState.mComponents.add(delegate);
        }
        if (delegate.getGlobalKey() != null) {
          layoutState.mComponentKeyToBounds.put(delegate.getGlobalKey(), copyRect);
        }
      }
      if (isTracing) {
        ComponentsSystrace.endSection();
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
            node,
            layoutState,
            null,
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

    if (isTracing) {
      ComponentsSystrace.endSection();
    }
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
      @Nullable LayoutOutput recycle,
      ComparableDrawable drawable,
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

  private static ComparableDrawable getBorderColorDrawable(InternalNode node) {
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
      Component drawableComponent,
      LayoutState layoutState,
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
        isCachedOutputUpdated);

    addMountableOutput(layoutState, drawableLayoutOutput);
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
      InternalNode node, LayoutState layoutState, DiffNode diffNode, boolean isPhantom) {
    final Component component = node.getRootComponent();
    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("addHostLayoutOutput:" + node.getSimpleName());
    }

    // Only the root host is allowed to wrap view mount specs as a layout output
    // is unconditionally added for it.
    if (isMountViewSpec(component) && !layoutState.isLayoutRoot(node)) {
      throw new IllegalArgumentException("We shouldn't insert a host as a parent of a View");
    }

    final LayoutOutput hostLayoutOutput = createHostLayoutOutput(layoutState, node, isPhantom);

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

    if (isTracing) {
      ComponentsSystrace.endSection();
    }
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
        source,
        null,
        false /* isPersistenceEnabled */);
  }

  static LayoutState calculate(
      ComponentContext c,
      Component component,
      int componentTreeId,
      int widthSpec,
      int heightSpec,
      boolean shouldGenerateDiffTree,
      @Nullable LayoutState previousLayoutState,
      @CalculateLayoutSource int source,
      @Nullable String extraAttribution,
      boolean isPersistenceEnabled) {

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

    final LayoutState layoutState;
    try {
      final PerfEvent logLayoutState =
          logger != null
              ? LogTreePopulator.populatePerfEventFromLogger(
                  c, logger, logger.newPerformanceEvent(c, EVENT_CALCULATE_LAYOUT_STATE))
              : null;
      if (logLayoutState != null) {
        logLayoutState.markerAnnotate(PARAM_COMPONENT, component.getSimpleName());
        logLayoutState.markerAnnotate(PARAM_LAYOUT_STATE_SOURCE, sourceToString(source));
      }

      // Detect errors internal to components
      component.markLayoutStarted();

      layoutState = new LayoutState(c);
      layoutState.mShouldGenerateDiffTree = shouldGenerateDiffTree;
      layoutState.mComponentTreeId = componentTreeId;
      layoutState.mPreviousLayoutStateId =
          previousLayoutState != null ? previousLayoutState.mId : NO_PREVIOUS_LAYOUT_STATE_ID;
      layoutState.mAccessibilityManager =
          (AccessibilityManager) c.getAndroidContext().getSystemService(ACCESSIBILITY_SERVICE);
      layoutState.mAccessibilityEnabled =
          AccessibilityUtils.isAccessibilityEnabled(layoutState.mAccessibilityManager);
      layoutState.mComponent = component;
      layoutState.mWidthSpec = widthSpec;
      layoutState.mHeightSpec = heightSpec;
      layoutState.mRootComponentName = component.getSimpleName();

      final InternalNode layoutCreatedInWillRender = component.consumeLayoutCreatedInWillRender();
      final InternalNode root =
          layoutCreatedInWillRender == null
              ? createAndMeasureTreeForComponent(
                  c,
                  component,
                  null, // nestedTreeHolder is null because this is measuring the root component
                  // tree.
                  widthSpec,
                  heightSpec,
                  previousLayoutState != null ? previousLayoutState.mDiffTreeRoot : null)
              : layoutCreatedInWillRender;

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
      layoutState.mRootTransitionId = getTransitionIdForNode(root);

      final PerfEvent collectResultsEvent =
          logger != null
              ? LogTreePopulator.populatePerfEventFromLogger(
                  c, logger, logger.newPerformanceEvent(c, EVENT_COLLECT_RESULTS))
              : null;

      collectResults(c, root, layoutState, null);

      if (isTracing) {
        ComponentsSystrace.beginSection("sortMountableOutputs");
      }
      Collections.sort(layoutState.mMountableOutputTops, sTopsComparator);
      Collections.sort(layoutState.mMountableOutputBottoms, sBottomsComparator);
      if (isTracing) {
        ComponentsSystrace.endSection();
      }

      if (collectResultsEvent != null) {
        collectResultsEvent.markerAnnotate(
            FrameworkLogEvents.PARAM_ROOT_COMPONENT, root.getRootComponent().getSimpleName());
        logger.logPerfEvent(collectResultsEvent);
      }

      if (!isPersistenceEnabled
          && !ComponentsConfiguration.isDebugModeEnabled
          && !ComponentsConfiguration.isEndToEndTestRun
          && layoutState.mLayoutRoot != null) {
        layoutState.mLayoutRoot = null;
      }

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

          ComponentsPools.maybePreallocateContent(mContext.getAndroidContext(), component);

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

  @VisibleForTesting
  static InternalNode createTree(
      Component component,
      ComponentContext context) {
    final ComponentsLogger logger = context.getLogger();

    final PerfEvent createLayoutPerfEvent =
        logger != null
            ? LogTreePopulator.populatePerfEventFromLogger(
                context, logger, logger.newPerformanceEvent(context, EVENT_CREATE_LAYOUT))
            : null;

    if (createLayoutPerfEvent != null) {
      createLayoutPerfEvent.markerAnnotate(PARAM_COMPONENT, component.getSimpleName());
    }

    final InternalNode root = component.createLayout(context, true /* resolveNestedTree */);

    if (createLayoutPerfEvent != null) {
      logger.logPerfEvent(createLayoutPerfEvent);
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
      ComponentsSystrace.beginSection("measureTree:" + root.getSimpleName());
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
        logger != null
            ? LogTreePopulator.populatePerfEventFromLogger(
                context, logger, logger.newPerformanceEvent(context, EVENT_CSS_LAYOUT))
            : null;

    if (layoutEvent != null) {
      layoutEvent.markerAnnotate(PARAM_TREE_DIFF_ENABLED, previousDiffTreeRoot != null);
      layoutEvent.markerAnnotate(PARAM_ROOT_COMPONENT, root.getRootComponent().getSimpleName());
    }

    root.calculateLayout(
        SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED
            ? YogaConstants.UNDEFINED
            : SizeSpec.getSize(widthSpec),
        SizeSpec.getMode(heightSpec) == SizeSpec.UNSPECIFIED
            ? YogaConstants.UNDEFINED
            : SizeSpec.getSize(heightSpec));

    if (layoutEvent != null) {
      logger.logPerfEvent(layoutEvent);
    }

    if (isTracing) {
      ComponentsSystrace.endSection(/* measureTree */ );
    }
  }

  /** Create and measure the nested tree or return the cached one for the same size specs. */
  static InternalNode resolveNestedTree(
      ComponentContext context, InternalNode holder, int widthSpec, int heightSpec) {

    final Component component = holder.getRootComponent();
    final InternalNode layoutFromWillRender = component.consumeLayoutCreatedInWillRender();
    final InternalNode nestedTree =
        layoutFromWillRender == null ? holder.getNestedTree() : layoutFromWillRender;

    // The resolved layout to return.
    final InternalNode resolvedLayout;

    if (nestedTree == null
        || !hasCompatibleSizeSpec(
            nestedTree.getLastWidthSpec(),
            nestedTree.getLastHeightSpec(),
            widthSpec,
            heightSpec,
            nestedTree.getLastMeasuredWidth(),
            nestedTree.getLastMeasuredHeight())) {

      // Check if cached layout can be used.
      final InternalNode cachedLayout =
          consumeCachedLayout(component, holder, widthSpec, heightSpec);

      if (cachedLayout != null) {

        // Use the cached layout.
        resolvedLayout = cachedLayout;
      } else {

        final Component root = holder.getRootComponent();
        if (context.isNestedTreeResolutionExperimentEnabled() && root != null) {
          /*
           * We create a shallow copy of the component to ensure that component is resolved
           * without any side effects caused by it's current internal state. In this case the
           * global key is reset so that a new global key is not generated for the the root
           * component; which would also change the global keys of it's descendants. This would
           * break state updates.
           */

          // Create a shallow copy for measure.
          final Component copy = root.makeShallowCopy();

          // Set the original global key so that state update work.
          copy.setGlobalKey(root.getGlobalKey());

          // Set this component as the root.
          holder.setRootComponent(copy);
        }

        // Check if previous layout can be remeasured and used.
        if (nestedTree != null && component.canUsePreviousLayout(context)) {
          remeasureTree(nestedTree, widthSpec, heightSpec);
          resolvedLayout = nestedTree;
        } else {
          // Create a new layout.
          resolvedLayout =
              createAndMeasureTreeForComponent(
                  context,
                  component,
                  holder,
                  widthSpec,
                  heightSpec,
                  holder.getDiffNode()); // Was set while traversing the holder's tree.
        }

        resolvedLayout.setLastWidthSpec(widthSpec);
        resolvedLayout.setLastHeightSpec(heightSpec);
        resolvedLayout.setLastMeasuredHeight(resolvedLayout.getHeight());
        resolvedLayout.setLastMeasuredWidth(resolvedLayout.getWidth());
      }

      holder.setNestedTree(resolvedLayout);
    } else {

      // Use the previous layout.
      resolvedLayout = nestedTree;
    }

    // This is checking only nested tree roots however should be moved to check all the tree roots.
    resolvedLayout.assertContextSpecificStyleNotSet();

    return resolvedLayout;
  }

  @VisibleForTesting
  static void remeasureTree(InternalNode layout, int widthSpec, int heightSpec) {
    if (layout == NULL_LAYOUT) { // If NULL LAYOUT return immediately.
      return;
    }

    layout.resetResolvedLayoutProperties(); // Reset all resolved props to force-remeasure.
    measureTree(layout, widthSpec, heightSpec, layout.getDiffNode());
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

  @VisibleForTesting
  static InternalNode createAndMeasureTreeForComponent(
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
    }

    // Account for the size specs in ComponentContext in case the tree is a NestedTree.
    final int previousWidthSpec = c.getWidthSpec();
    final int previousHeightSpec = c.getHeightSpec();

    c.setWidthSpec(widthSpec);
    c.setHeightSpec(heightSpec);

    final InternalNode root = createTree(
        component,
        c);

    c.setTreeProps(null);
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
        && LayoutState.isLayoutDirectionRTL(c.getAndroidContext())) {
      root.layoutDirection(YogaDirection.RTL);
    }

    measureTree(
        root,
        widthSpec,
        heightSpec,
        diffTreeRoot);

    return root;
  }

  @Nullable
  static InternalNode consumeCachedLayout(
      Component component, InternalNode holder, int widthSpec, int heightSpec) {
    final InternalNode cachedLayout = component.getCachedLayout();
    if (cachedLayout != null) {
      component.clearCachedLayout();

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

  static DiffNode createDiffNode(InternalNode node, DiffNode parent) {
    DiffNode diffNode = new DiffNode();

    diffNode.setLastWidthSpec(node.getLastWidthSpec());
    diffNode.setLastHeightSpec(node.getLastHeightSpec());
    diffNode.setLastMeasuredWidth(node.getLastMeasuredWidth());
    diffNode.setLastMeasuredHeight(node.getLastMeasuredHeight());
    diffNode.setComponent(node.getRootComponent());
    if (parent != null) {
      parent.addChild(diffNode);
    }

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
    return AccessibilityUtils.isAccessibilityEnabled(mAccessibilityManager)
        == mAccessibilityEnabled;
  }

  /**
   * Traverses the layoutTree and the diffTree recursively. If a layoutNode has a compatible host
   * type {@link LayoutState#hostIsCompatible} it assigns the DiffNode to the layout node in order
   * to try to re-use the LayoutOutputs that will be generated by {@link
   * LayoutState#collectResults(ComponentContext, InternalNode, LayoutState, DiffNode)}. If a
   * layoutNode component returns false when shouldComponentUpdate is called with the DiffNode
   * Component it also tries to re-use the old measurements and therefore marks as valid the
   * cachedMeasures for the whole component subtree.
   *
   * @param layoutNode the root of the LayoutTree
   * @param diffNode the root of the diffTree
   * @return true if the layout node requires updating, false if it can re-use the measurements from
   *     the diff node.
   */
  static void applyDiffNodeToUnchangedNodes(InternalNode layoutNode, DiffNode diffNode) {
    try {
      // Root of the main tree or of a nested tree.
      final boolean isTreeRoot = layoutNode.getParent() == null;
      if (isLayoutSpecWithSizeSpec(layoutNode.getRootComponent()) && !isTreeRoot) {
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
          applyDiffNodeToUnchangedNodes(layoutNode.getChildAt(i), diffNode.getChildAt(i));
        }

        // Apply the DiffNode to a leaf node (i.e. MountSpec) only if it should NOT update.
      } else if (!shouldComponentUpdate(layoutNode, diffNode)) {
        applyDiffNodeToLayoutNode(layoutNode, diffNode);
      }
    } catch (Throwable t) {
      final Component c = layoutNode.getRootComponent();
      if (c != null) {
        throw new ComponentsChainException(c, t);
      }

      throw t;
    }
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

  /**
   * @return The id of the {@link ComponentTree} that generated this {@link LayoutState}
   */
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
        || (!ComponentsConfiguration.createPhantomLayoutOutputsForTransitions
            && needsHostViewForTransition(node));
  }

  private static boolean needsHostViewForTransition(InternalNode node) {
    return !TextUtils.isEmpty(node.getTransitionKey()) && !isMountViewSpec(node.getRootComponent());
  }

  /**
   * @return the position of the {@link LayoutOutput} with id layoutOutputId in the
   * {@link LayoutState} list of outputs or -1 if no {@link LayoutOutput} with that id exists in
   * the {@link LayoutState}
   */
  int getLayoutOutputPositionForId(long layoutOutputId) {
    return mOutputsIdToPositionMap.get(layoutOutputId, -1);
  }

  /** @return a {@link LayoutOutput} for a given {@param layoutOutputId} */
  @Nullable
  LayoutOutput getLayoutOutput(long layoutOutputId) {
    final int position = getLayoutOutputPositionForId(layoutOutputId);
    return position < 0 ? null : getMountableOutputAt(position);
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

  private static void addMountableOutput(LayoutState layoutState, LayoutOutput layoutOutput) {
    layoutOutput.setIndex(layoutState.mMountableOutputs.size());

    layoutState.mMountableOutputs.add(layoutOutput);
    layoutState.mMountableOutputTops.add(layoutOutput);
    layoutState.mMountableOutputBottoms.add(layoutOutput);
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
    @TransitionId.Type int type;
    String reference;
    String extraData = null;
    if (node.hasTransitionKey()) {
      Transition.TransitionKeyType transitionKeyType = node.getTransitionKeyType();
      if (transitionKeyType == Transition.TransitionKeyType.GLOBAL) {
        type = TransitionId.Type.GLOBAL;
      } else if (transitionKeyType == Transition.TransitionKeyType.LOCAL) {
        type = TransitionId.Type.SCOPED;
        extraData = node.getRootComponent().getOwnerGlobalKey();
      } else {
        throw new RuntimeException("Unhandled transition key type " + transitionKeyType);
      }
      reference = node.getTransitionKey();
    } else if (ComponentsConfiguration.assignTransitionKeysToAllOutputs) {
      type = TransitionId.Type.AUTOGENERATED;
      if (node.getRootComponent() != null) {
        reference = node.getRootComponent().getGlobalKey();
      } else {
        reference = null;
      }
    } else {
      return null;
    }
    return reference != null ? new TransitionId(type, reference, extraData) : null;
  }
}
