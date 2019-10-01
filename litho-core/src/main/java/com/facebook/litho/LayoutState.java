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
import static com.facebook.litho.FrameworkLogEvents.EVENT_RESUME_CALCULATE_LAYOUT_STATE;
import static com.facebook.litho.FrameworkLogEvents.PARAM_COMPONENT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_LAYOUT_STATE_SOURCE;
import static com.facebook.litho.FrameworkLogEvents.PARAM_TREE_DIFF_ENABLED;
import static com.facebook.litho.MountItem.LAYOUT_FLAG_DISABLE_TOUCHABLE;
import static com.facebook.litho.MountItem.LAYOUT_FLAG_DUPLICATE_PARENT_STATE;
import static com.facebook.litho.MountItem.LAYOUT_FLAG_MATCH_HOST_BOUNDS;
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
// This needs to be accessible to statically mock the class in tests.
@VisibleForTesting
class LayoutState {

  private static final String DUPLICATE_TRANSITION_IDS = "LayoutState:DuplicateTransitionIds";

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

  /**
   * Wraps an instance of a LayoutState to access it in other classes such as ComponentContext
   * during layout state calculation. When the layout calculation finishes, the LayoutState
   * reference is nullified. Using a wrapper instead of accessing the LayoutState instance directly
   * helps with clearing out the reference from all objects that hold on to it, without needing the
   * LayoutState to know about them.
   */
  static final class LayoutStateReferenceWrapper {
    private @Nullable LayoutState mLayoutStateRef;
    private static @Nullable LayoutState sTestLayoutState;

    public static LayoutStateReferenceWrapper getTestInstance(ComponentContext c) {
      if (sTestLayoutState == null) {
        sTestLayoutState = new LayoutState(c);
      }
      return new LayoutStateReferenceWrapper(sTestLayoutState);
    }

    @VisibleForTesting
    LayoutStateReferenceWrapper(LayoutState layoutState) {
      mLayoutStateRef = layoutState;
    }

    private void releaseReference() {
      mLayoutStateRef = null;
    }

    /** Returns the LayoutState instance or null if the layout state has been released. */
    @Nullable
    LayoutState getLayoutState() {
      return mLayoutStateRef;
    }
  }

  private static final AtomicInteger sIdGenerator = new AtomicInteger(1);
  private static final int NO_PREVIOUS_LAYOUT_STATE_ID = -1;
  private static final boolean IS_TEST = "robolectric".equals(Build.FINGERPRINT);

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
  private final int mId;
  // Id of the layout state (if any) that was used in comparisons with this layout state.
  private int mPreviousLayoutStateId = NO_PREVIOUS_LAYOUT_STATE_ID;

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
  private final boolean mCacheInternalNodeOnLayoutState;

  private static final Object debugLock = new Object();
  @Nullable private static Map<Integer, List<Boolean>> layoutCalculationsOnMainThread;

  @Nullable WorkingRangeContainer mWorkingRangeContainer;

  /** A container stores components whose OnAttached delegate methods are about to be executed. */
  @Nullable private Map<String, Component> mAttachableContainer;

  LayoutState(ComponentContext context) {
    mContext = context;
    mId = sIdGenerator.getAndIncrement();
    mStateHandler = mContext.getStateHandler();
    mTestOutputs = ComponentsConfiguration.isEndToEndTestRun ? new ArrayList<TestOutput>(8) : null;
    mOrientation = context.getResources().getConfiguration().orientation;

    mCacheInternalNodeOnLayoutState = context.isLayoutStateCachingEnabled();
    mLastMeasuredLayouts =
        mCacheInternalNodeOnLayoutState ? new HashMap<Integer, InternalNode>() : null;
  }

  @VisibleForTesting
  Component getRootComponent() {
    return mComponent;
  }

  boolean isPartialLayoutState() {
    return mIsPartialLayoutState;
  }

  /**
   * Acquires a new layout output for the internal node and its associated component. It returns
   * null if there's no component associated with the node as the mount pass only cares about nodes
   * that will potentially mount content into the component host.
   */
  @Nullable
  private static LayoutOutput createGenericLayoutOutput(
      InternalNode node, LayoutState layoutState, boolean hasHostView) {
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

  private static LayoutOutput createHostLayoutOutput(LayoutState layoutState, InternalNode node) {

    final HostComponent hostComponent = HostComponent.create();

    // We need to pass common dynamic props to the host component, as they only could be applied to
    // views, so we'll need to set them up, when binding HostComponent to ComponentHost. At the same
    // time, we don't remove them from the current component, as we may calculate multiple
    // LayoutStates using same Components
    Component tailComponent = node.getTailComponent();
    if (tailComponent != null) {
      hostComponent.setCommonDynamicProps(tailComponent.getCommonDynamicProps());
    }

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
      final LayoutOutput hostOutput =
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

  @VisibleForTesting
  static boolean isLayoutDirectionRTL(Context context) {
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
    final Component component = node.getTailComponent();
    final boolean isTracing = ComponentsSystrace.isTracing();

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
          resolveNestedTree(
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

      collectResults(parentContext, nestedTree, layoutState, parentDiffNode);

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
      diffNode = createDiffNode(node, parentDiffNode);
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
      final int hostLayoutPosition = addHostLayoutOutput(node, layoutState, diffNode);
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

    // 2. Add background if defined.
    final ComparableDrawable background = node.getBackground();
    if (background != null) {
      if (layoutOutput != null && layoutOutput.getViewNodeInfo() != null) {
        layoutOutput.getViewNodeInfo().setBackground(background);
      } else {
        final LayoutOutput convertBackground =
            (currentDiffNode != null) ? currentDiffNode.getBackground() : null;

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
    if (areTransitionsEnabled(component != null ? component.getScopedContext() : null)) {
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
      collectResults(node.getContext(), node.getChildAt(i), layoutState, diffNode);
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
    final ComparableDrawable foreground = node.getForeground();
    if (foreground != null) {
      if (layoutOutput != null && layoutOutput.getViewNodeInfo() != null && SDK_INT >= M) {
        layoutOutput.getViewNodeInfo().setForeground(foreground);
      } else {
        final LayoutOutput convertForeground =
            (currentDiffNode != null) ? currentDiffNode.getForeground() : null;

        final LayoutOutput foregroundOutput =
            addDrawableComponent(
                node,
                layoutState,
                convertForeground,
                foreground,
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
          layoutState.mComponents.add(delegate);
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

  /**
   * This method determine if transitions are enabled for the user. If the experiment is enabled for
   * the user then they will get cached value else it will be determined using the utility method.
   *
   * @param context Component context.
   * @return true if transitions are enabled.
   */
  static boolean areTransitionsEnabled(@Nullable ComponentContext context) {
    if (context == null) {
      return TransitionUtils.areTransitionsEnabled(null);
    }
    // Experiment of caching the transition check is enabled
    if (ComponentsConfiguration.isTransitionCheckCached && context.getComponentTree() != null) {
      return context.getComponentTree().areTransitionsEnabled();
    }
    // Fall back to the old flow when the experiment is not enabled.
    return TransitionUtils.areTransitionsEnabled(context.getAndroidContext());
  }

  @Nullable
  Map<String, Component> consumeAttachables() {
    @Nullable Map<String, Component> tmp = mAttachableContainer;
    mAttachableContainer = null;
    return tmp;
  }

  private static void calculateAndSetHostOutputIdAndUpdateState(
      InternalNode node, LayoutOutput hostOutput, LayoutState layoutState) {
    if (layoutState.isLayoutRoot(node)) {
      // The root host (LithoView) always has ID 0 and is unconditionally
      // set as dirty i.e. no need to use shouldComponentUpdate().
      hostOutput.setId(ROOT_HOST_ID);

      hostOutput.setUpdateState(LayoutOutput.STATE_DIRTY);
    } else {
      layoutState.calculateAndSetLayoutOutputIdAndUpdateState(
          hostOutput, layoutState.mCurrentLevel, OutputUnitType.HOST, -1, false);
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
      InternalNode node, LayoutState layoutState, DiffNode diffNode) {
    final Component component = node.getTailComponent();

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

    calculateAndSetHostOutputIdAndUpdateState(node, hostLayoutOutput, layoutState);

    addLayoutOutputIdToPositionsMap(
        layoutState.mOutputsIdToPositionMap, hostLayoutOutput, hostOutputPosition);

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
    LayoutStateReferenceWrapper layoutStateWrapper = null;

    try {
      final PerfEvent logLayoutState =
          logger != null
              ? LogTreePopulator.populatePerfEventFromLogger(
                  c, logger, logger.newPerformanceEvent(c, EVENT_CALCULATE_LAYOUT_STATE))
              : null;
      if (logLayoutState != null) {
        logLayoutState.markerAnnotate(PARAM_COMPONENT, component.getSimpleName());
        logLayoutState.markerAnnotate(PARAM_LAYOUT_STATE_SOURCE, sourceToString(source));
        logLayoutState.markerAnnotate(PARAM_TREE_DIFF_ENABLED, diffTreeRoot != null);
      }

      // Detect errors internal to components
      component.markLayoutStarted();

      layoutState = new LayoutState(c);
      layoutStateWrapper = new LayoutStateReferenceWrapper(layoutState);
      c.setLayoutStateReferenceWrapper(layoutStateWrapper);

      layoutState.mShouldGenerateDiffTree = shouldGenerateDiffTree;
      layoutState.mComponentTreeId = componentTreeId;
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

      final InternalNode layoutCreatedInWillRender = component.consumeLayoutCreatedInWillRender();

      final boolean isReconcilable = isReconcilable(c, component, currentLayoutState);

      final InternalNode root =
          layoutCreatedInWillRender == null
              ? createAndMeasureTreeForComponent(
                  c,
                  component,
                  widthSpec,
                  heightSpec,
                  null, // nestedTreeHolder is null as this is measuring the root component tree.
                  isReconcilable ? currentLayoutState.mLayoutRoot : null,
                  diffTreeRoot,
                  logLayoutState)
              : layoutCreatedInWillRender;
      // Null check for tests.
      if (root.getContext() != null) {
        root.getContext().setLayoutStateReferenceWrapper(layoutStateWrapper);
      }

      layoutState.mLayoutRoot = root;
      layoutState.mRootTransitionId = getTransitionIdForNode(root);

      if (c.wasLayoutInterrupted()) {
        layoutState.mIsPartialLayoutState = true;
        return layoutState;
      }

      if (logLayoutState != null) {
        logLayoutState.markerPoint("start_collect_results");
      }

      setSizeAfterMeasureAndCollectResults(c, layoutState);

      if (layoutStateWrapper != null) {
        layoutStateWrapper.releaseReference();
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

    return layoutState;
  }

  static LayoutState resumeCalculate(
      ComponentContext c,
      @CalculateLayoutSource int source,
      @Nullable String extraAttribution,
      LayoutState layoutState) {

    if (!layoutState.mIsPartialLayoutState) {
      throw new IllegalStateException("Can not resume a finished LayoutState calculation");
    }

    final LayoutStateReferenceWrapper layoutStateWrapper =
        new LayoutStateReferenceWrapper(layoutState);
    c.setLayoutStateReferenceWrapper(layoutStateWrapper);

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
      resumeCreateAndMeasureTreeForComponent(
          c,
          component,
          widthSpec,
          heightSpec,
          null, // nestedTreeHolder is null as this is measuring the root component tree.)
          layoutState.mLayoutRoot,
          layoutState.mDiffTreeRoot,
          logLayoutState);

      setSizeAfterMeasureAndCollectResults(c, layoutState);

      if (layoutStateWrapper != null) {
        layoutStateWrapper.releaseReference();
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

  private static void setSizeAfterMeasureAndCollectResults(
      ComponentContext c, LayoutState layoutState) {
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
    collectResults(c, root, layoutState, null);
    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    if (isTracing) {
      ComponentsSystrace.beginSection("sortMountableOutputs");
    }
    Collections.sort(layoutState.mMountableOutputTops, sTopsComparator);
    Collections.sort(layoutState.mMountableOutputBottoms, sBottomsComparator);
    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    if (!c.isReconciliationEnabled()
        && !ComponentsConfiguration.isDebugModeEnabled
        && !ComponentsConfiguration.isEndToEndTestRun
        && layoutState.mLayoutRoot != null) {
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
    if (!isSameComponentType(previous, nextRootComponent)) {
      return false;
    }

    if (!nextRootComponent.isEquivalentTo(previous)) {
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
      Component component, ComponentContext context, @Nullable InternalNode current) {
    if (current != null) {
      return current.reconcile(context, component);
    }

    return createLayout(context, component, true /* resolveNestedTree */);
  }

  @VisibleForTesting
  static void measureTree(
      InternalNode root, int widthSpec, int heightSpec, DiffNode previousDiffTreeRoot) {
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
      ComponentsSystrace.endSection(/* applyDiffNode */ );
    }

    root.calculateLayout(
        SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED
            ? YogaConstants.UNDEFINED
            : SizeSpec.getSize(widthSpec),
        SizeSpec.getMode(heightSpec) == SizeSpec.UNSPECIFIED
            ? YogaConstants.UNDEFINED
            : SizeSpec.getSize(heightSpec));

    if (isTracing) {
      ComponentsSystrace.endSection(/* measureTree */ );
    }
  }

  /** Create and measure the nested tree or return the cached one for the same size specs. */
  static InternalNode resolveNestedTree(
      ComponentContext context, InternalNode holder, int widthSpec, int heightSpec) {

    final Component component = holder.getTailComponent();
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
          consumeCachedLayout(context, component, holder, widthSpec, heightSpec);

      if (cachedLayout != null) {

        // Use the cached layout.
        resolvedLayout = cachedLayout;
      } else {
        // Check if previous layout can be remeasured and used.
        if (nestedTree != null && component.canUsePreviousLayout(context)) {
          remeasureTree(nestedTree, widthSpec, heightSpec);
          resolvedLayout = nestedTree;
        } else {

          // We need to create a shallow copy of this component to clear
          // the child counters as all the children may be created again.
          final Component root = component.makeShallowCopy();

          // We have to set the current global key to avoid generating
          // a new global key; in addition that new global key would be
          // incorrect because it will be de-duplicated by parent as the
          // parent is still maintaining its child counters.
          root.setGlobalKey(component.getGlobalKey());

          // Create a new layout.
          resolvedLayout =
              createAndMeasureTreeForComponent(
                  context,
                  root,
                  widthSpec,
                  heightSpec,
                  holder,
                  null,
                  holder.getDiffNode(), // Was set while traversing the holder's tree.
                  null);
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

  /** Create and measure a component with the given size specs. */
  static InternalNode createAndMeasureTreeForComponent(
      ComponentContext c, Component component, int widthSpec, int heightSpec) {
    return createAndMeasureTreeForComponent(
        c, component, widthSpec, heightSpec, null, null, null, null);
  }

  @VisibleForTesting
  static InternalNode createAndMeasureTreeForComponent(
      ComponentContext c,
      Component component,
      int widthSpec,
      int heightSpec,
      @Nullable InternalNode nestedTreeHolder, // Will be set iff resolving a nested tree.
      @Nullable InternalNode current,
      @Nullable DiffNode diffTreeRoot,
      @Nullable PerfEvent layoutStatePerfEvent) {

    if (layoutStatePerfEvent != null) {
      layoutStatePerfEvent.markerPoint("start_create_layout");
    }

    component.updateInternalChildState(c);

    if (ComponentsConfiguration.isDebugModeEnabled) {
      DebugComponent.applyOverrides(c, component);
    }

    c = component.getScopedContext();
    // Copy the context so that it can have its own set of tree props.
    // Robolectric tests keep the context so that tree props can be set externally.
    if (!IS_TEST) {
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

    final InternalNode root = createTree(component, c, current);

    c.setTreeProps(null);
    c.setWidthSpec(previousWidthSpec);
    c.setHeightSpec(previousHeightSpec);

    if (layoutStatePerfEvent != null) {
      layoutStatePerfEvent.markerPoint("end_create_layout");
    }

    if (root == NULL_LAYOUT || c.wasLayoutInterrupted()) {
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

    if (layoutStatePerfEvent != null) {
      layoutStatePerfEvent.markerPoint("start_measure");
    }

    measureTree(root, widthSpec, heightSpec, diffTreeRoot);

    if (layoutStatePerfEvent != null) {
      layoutStatePerfEvent.markerPoint("end_measure");
    }

    return root;
  }

  private static InternalNode resumeCreateAndMeasureTreeForComponent(
      ComponentContext c,
      Component component,
      int widthSpec,
      int heightSpec,
      @Nullable InternalNode nestedTreeHolder, // Will be set iff resolving a nested tree.
      InternalNode root,
      @Nullable DiffNode diffTreeRoot,
      @Nullable PerfEvent logLayoutState) {
    resumeCreateTree(c, root);

    if (root == NULL_LAYOUT) {
      return root;
    }

    // If measuring a ComponentTree with a LayoutSpecWithSizeSpec at the root, the nested tree
    // holder argument will be null.
    if (nestedTreeHolder != null && isLayoutSpecWithSizeSpec(component)) {
      // Transfer information from the holder node to the nested tree root before measurement.
      nestedTreeHolder.copyInto(root);
      diffTreeRoot = nestedTreeHolder.getDiffNode();
    } else if (root.getStyleDirection() == com.facebook.yoga.YogaDirection.INHERIT
        && LayoutState.isLayoutDirectionRTL(c.getAndroidContext())) {
      root.layoutDirection(YogaDirection.RTL);
    }

    if (logLayoutState != null) {
      logLayoutState.markerPoint("start_measure");
    }

    measureTree(root, widthSpec, heightSpec, diffTreeRoot);

    if (logLayoutState != null) {
      logLayoutState.markerPoint("end_measure");
    }

    return root;
  }

  private static void resumeCreateTree(ComponentContext c, InternalNode root) {
    final List<Component> unresolved = root.getUnresolvedComponents();

    if (unresolved != null) {
      for (int i = 0, size = unresolved.size(); i < size; i++) {
        root.child(unresolved.get(i));
      }
      root.getUnresolvedComponents().clear();
    }

    for (int i = 0, size = root.getChildCount(); i < size; i++) {
      resumeCreateTree(c, root.getChildAt(i));
    }
  }

  boolean shouldCacheInternalNodeOnLayoutState() {
    return mCacheInternalNodeOnLayoutState;
  }

  @Nullable
  static InternalNode consumeCachedLayout(
      ComponentContext c, Component component, InternalNode holder, int widthSpec, int heightSpec) {
    final InternalNode cachedLayout = component.getCachedLayout(c);

    if (cachedLayout != null) {
      component.clearCachedLayout(c);

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
    DiffNode diffNode = new DiffNode();

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
      if (isLayoutSpecWithSizeSpec(layoutNode.getTailComponent()) && !isTreeRoot) {
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
      final Component c = layoutNode.getTailComponent();
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
    final Component component = layoutNode.getTailComponent();
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

    return isSameComponentType(node.getTailComponent(), diffNode.getComponent());
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

    final Component component = layoutNode.getTailComponent();
    if (component != null) {
      return component.shouldComponentUpdate(component, diffNode.getComponent());
    }

    return true;
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
      int oldWidthSpec,
      int oldHeightSpec,
      int newWidthSpec,
      int newHeightSpec,
      float oldMeasuredWidth,
      float oldMeasuredHeight) {
    final boolean widthIsCompatible =
        MeasureComparisonUtils.isMeasureSpecCompatible(
            oldWidthSpec, newWidthSpec, (int) oldMeasuredWidth);

    final boolean heightIsCompatible =
        MeasureComparisonUtils.isMeasureSpecCompatible(
            oldHeightSpec, newHeightSpec, (int) oldMeasuredHeight);
    return widthIsCompatible && heightIsCompatible;
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

    if (component != null && component.hasCommonDynamicProps()) {
      // Need a host View to apply the dynamic props to
      return true;
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
      final LayoutOutput layoutOutput = getMountableOutputAt(i);
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
    final Component component = node.getTailComponent();

    @TransitionId.Type int type;
    String reference;
    String extraData = null;

    if (node.hasTransitionKey()) {
      final Transition.TransitionKeyType transitionKeyType = node.getTransitionKeyType();
      if (transitionKeyType == Transition.TransitionKeyType.GLOBAL) {
        type = TransitionId.Type.GLOBAL;
      } else if (transitionKeyType == Transition.TransitionKeyType.LOCAL) {
        type = TransitionId.Type.SCOPED;
        extraData = component != null ? component.getOwnerGlobalKey() : null;
      } else {
        throw new RuntimeException("Unhandled transition key type " + transitionKeyType);
      }
      reference = node.getTransitionKey();
    } else {
      type = TransitionId.Type.AUTOGENERATED;
      reference = component != null ? component.getGlobalKey() : null;
    }

    return reference != null ? new TransitionId(type, reference, extraData) : null;
  }

  /**
   * Create a layout from the given component.
   *
   * @param component the root component.
   * @param shouldResolveNestedTree if the layout of the component should be immediately resolved.
   * @return New InternalNode associated with the given component.
   */
  static InternalNode createLayout(
      final ComponentContext c, final Component component, final boolean shouldResolveNestedTree) {
    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("createLayout:" + component.getSimpleName());
    }

    final boolean shouldDeferNestedTreeResolution =
        Component.isNestedTree(c, component) && !shouldResolveNestedTree;
    final InternalNode node;

    try {

      // 1. Consume the layout created in will render.
      final InternalNode layoutCreatedInWillRender = component.consumeLayoutCreatedInWillRender();

      // 2. Return immediately if will render returned a layout.
      if (layoutCreatedInWillRender != null) {
        return layoutCreatedInWillRender;
      }

      // 3. Add this component's tree props to the current context.
      final TreeProps treeProps = c.getTreeProps();
      c.setTreeProps(component.getTreePropsForChildren(c, treeProps));

      // 4. Resolve the Component into an InternalNode.

      // 4.1 If nested tree resolution should be deferred.
      if (shouldDeferNestedTreeResolution) {

        // Create a blank InternalNode for the nested tree holder.
        node = InternalNodeUtils.create(c);
        node.markIsNestedTreeHolder(c.getTreeProps());

        // 4.2 If the Component can resolve its own InternalNode.
      } else if (component.canResolve()) {

        // Copy the tree props and set it again.
        c.setTreeProps(c.getTreePropsCopy());

        // Resolve the component into an InternalNode.
        node = (InternalNode) component.resolve(c);

        // 4.3 If the Component is a MountSpec
      } else if (ComponentsConfiguration.isConsistentComponentHierarchyExperimentEnabled
          && isMountSpec(component)) {

        // Create a blank InternalNode for MountSpecs.
        node = c.newLayoutBuilder(0, 0);

        // 4.4 Create and resolve the LayoutSpec.
      } else {

        // Create the component's layout.
        final Component root = component.createComponentLayout(c);

        // Resolve the layout into an InternalNode.
        if (root == null || root.getId() <= 0) {
          node = null;
        } else {

          // Resolve the root component's layout.
          node = resolve(c, root);

          // If the root is a layout spec which can resolve itself, add it to the InternalNode.
          if (ComponentsConfiguration.isConsistentComponentHierarchyExperimentEnabled
              && Component.isLayoutSpec(root)
              && root.canResolve()) {
            node.appendComponent(root);
          }
        }
      }

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

    // 5. Copy common props of this Component into its InternalNode.
    // If this is a layout spec with size spec, and we're not deferring the nested tree resolution,
    // then we already added the props earlier on (when we did defer resolution), and
    // therefore we shouldn't add them again here.
    final CommonPropsCopyable commonProps = component.getCommonPropsCopyable();
    if (commonProps != null
        && (shouldDeferNestedTreeResolution || !isLayoutSpecWithSizeSpec(component))) {
      commonProps.copyInto(c, node);
    }

    // 6. Set the measure function.
    // Set measure func on the root node of the generated tree so that the mount calls use
    // those (see Controller.mountNodeTree()). Handle the case where the component simply
    // delegates its layout creation to another component, i.e. the root node belongs to
    // another component.
    if (node.getTailComponent() == null) {
      final boolean isMountSpecWithMeasure = component.canMeasure() && isMountSpec(component);
      if (isMountSpecWithMeasure || shouldDeferNestedTreeResolution) {
        node.setMeasureFunction(ComponentLifecycle.sMeasureFunction);
      }
    }

    // 7. Add the component to its InternalNode.
    node.appendComponent(component);

    // 8. Create and add transition to this component's InternalNode.
    if (areTransitionsEnabled(c)) {
      if (component.needsPreviousRenderData()) {
        node.addComponentNeedingPreviousRenderData(component);
      } else {
        final Transition transition = component.createTransition(c);
        if (transition != null) {
          node.addTransition(transition);
        }
      }
    }

    // 9. Call onPrepare for MountSpecs.
    if (!shouldDeferNestedTreeResolution) {
      component.onPrepare(c);
    }

    // 10. Add working ranges to the InternalNode.
    if (component.mWorkingRangeRegistrations != null
        && !component.mWorkingRangeRegistrations.isEmpty()) {
      node.addWorkingRanges(component.mWorkingRangeRegistrations);
    }

    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    return node;
  }

  static InternalNode resolve(final ComponentContext parentContext, Component component) {

    // 1. Consume the layout created in will render.
    final InternalNode layoutCreatedInWillRender = component.consumeLayoutCreatedInWillRender();

    // 2. Return immediately if will render returned a layout.
    if (layoutCreatedInWillRender != null) {
      return layoutCreatedInWillRender;
    }

    // 3. Create a shallow copy of this component for thread safety.
    component = component.getThreadSafeInstance();

    // 4. Update this component with its current parent context.
    component.updateInternalChildState(parentContext);

    if (ComponentsConfiguration.isDebugModeEnabled) {
      DebugComponent.applyOverrides(parentContext, component);
    }

    final ComponentContext c = component.getScopedContext();

    // 5. Resolve the Component into an InternalNode.
    final InternalNode node = (InternalNode) component.resolve(c);

    // Explicitly copy the common props into the InternalNode if it did resolve itself.
    if (component.canResolve()) {
      final CommonPropsCopyable props = component.getCommonPropsCopyable();
      if (props != null) {
        props.copyInto(c, node);
      }
    }

    return node;
  }
}
