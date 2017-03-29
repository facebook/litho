/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.LongSparseArray;
import android.support.v4.view.accessibility.AccessibilityManagerCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.displaylist.DisplayList;
import com.facebook.litho.displaylist.DisplayListException;
import com.facebook.litho.reference.BorderColorDrawableReference;
import com.facebook.litho.reference.Reference;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;

import static android.content.Context.ACCESSIBILITY_SERVICE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.M;
import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO;
import static com.facebook.litho.Component.isHostSpec;
import static com.facebook.litho.Component.isLayoutSpecWithSizeSpec;
import static com.facebook.litho.Component.isMountSpec;
import static com.facebook.litho.Component.isMountViewSpec;
import static com.facebook.litho.ComponentContext.NULL_LAYOUT;
import static com.facebook.litho.ComponentLifecycle.MountType.NONE;
import static com.facebook.litho.ComponentsLogger.ACTION_SUCCESS;
import static com.facebook.litho.ComponentsLogger.EVENT_COLLECT_RESULTS;
import static com.facebook.litho.ComponentsLogger.EVENT_CREATE_LAYOUT;
import static com.facebook.litho.ComponentsLogger.EVENT_CSS_LAYOUT;
import static com.facebook.litho.ComponentsLogger.PARAM_LOG_TAG;
import static com.facebook.litho.ComponentsLogger.PARAM_TREE_DIFF_ENABLED;
import static com.facebook.litho.MountItem.FLAG_DUPLICATE_PARENT_STATE;
import static com.facebook.litho.MountState.ROOT_HOST_ID;
import static com.facebook.litho.NodeInfo.FOCUS_SET_TRUE;
import static com.facebook.litho.SizeSpec.EXACTLY;

/**
 * The main role of {@link LayoutState} is to hold the output of layout calculation. This includes
 * mountable outputs and visibility outputs. A centerpiece of the class is {@link
 * #collectResults(InternalNode, LayoutState, DiffNode)} which prepares the before-mentioned outputs
 * based on the provided {@link InternalNode} for later use in {@link MountState}.
 */
class LayoutState {
  static final Comparator<LayoutOutput> sTopsComparator =
      new Comparator<LayoutOutput>() {
        @Override
        public int compare(LayoutOutput lhs, LayoutOutput rhs) {
          final int lhsTop = lhs.getBounds().top;
          final int rhsTop = rhs.getBounds().top;
          return lhsTop < rhsTop
              ? -1
              : lhsTop > rhsTop
              ? 1
              // Hosts should be higher for tops so that they are mounted first if possible.
              : isHostSpec(lhs.getComponent()) == isHostSpec(rhs.getComponent())
              ? 0
              : isHostSpec(lhs.getComponent()) ? -1 : 1;
        }
      };

  static final Comparator<LayoutOutput> sBottomsComparator =
      new Comparator<LayoutOutput>() {
        @Override
        public int compare(LayoutOutput lhs, LayoutOutput rhs) {
          final int lhsBottom = lhs.getBounds().bottom;
          final int rhsBottom = rhs.getBounds().bottom;
          return lhsBottom < rhsBottom
              ? -1
              : lhsBottom > rhsBottom
              ? 1
              // Hosts should be lower for bottoms so that they are mounted first if possible.
              : isHostSpec(lhs.getComponent()) == isHostSpec(rhs.getComponent())
              ? 0
              : isHostSpec(lhs.getComponent()) ? 1 : -1;
        }
      };

  private static final int[] DRAWABLE_STATE_ENABLED = new int[]{android.R.attr.state_enabled};
  private static final int[] DRAWABLE_STATE_NOT_ENABLED = new int[]{};

  private ComponentContext mContext;
  private TransitionContext mTransitionContext;

  private Component<?> mComponent;

  private int mWidthSpec;
  private int mHeightSpec;

  private final List<LayoutOutput> mMountableOutputs = new ArrayList<>(8);
  private final List<VisibilityOutput> mVisibilityOutputs = new ArrayList<>(8);
  private final LongSparseArray<Integer> mOutputsIdToPositionMap = new LongSparseArray<>(8);
  private final LayoutStateOutputIdCalculator mLayoutStateOutputIdCalculator;
  private final ArrayList<LayoutOutput> mMountableOutputTops = new ArrayList<>();
  private final ArrayList<LayoutOutput> mMountableOutputBottoms = new ArrayList<>();
  private final List<TestOutput> mTestOutputs;

  private InternalNode mLayoutRoot;
  private DiffNode mDiffTreeRoot;
  // Reference count will be initialized to 1 in init().
  private final AtomicInteger mReferenceCount = new AtomicInteger(-1);

  private int mWidth;
  private int mHeight;

  private int mCurrentX;
  private int mCurrentY;

  private int mCurrentLevel = 0;

  // Holds the current host marker in the layout tree.
  private long mCurrentHostMarker = -1;
  private int mCurrentHostOutputPosition = -1;

  private boolean mShouldDuplicateParentState = true;

  private boolean mShouldGenerateDiffTree = false;
  private int mComponentTreeId = -1;

  private AccessibilityManager mAccessibilityManager;
  private boolean mAccessibilityEnabled = false;

  private StateHandler mStateHandler;

  LayoutState() {
    mLayoutStateOutputIdCalculator = new LayoutStateOutputIdCalculator();
    mTestOutputs = ComponentsConfiguration.isEndToEndTestRun ? new ArrayList<TestOutput>(8) : null;
  }

  /**
   * Acquires a new layout output for the internal node and its associated component. It returns
   * null if there's no component associated with the node as the mount pass only cares about nodes
   * that will potentially mount content into the component host.
   */
  @Nullable
  private static LayoutOutput createGenericLayoutOutput(
      InternalNode node,
      LayoutState layoutState) {
    final Component<?> component = node.getComponent();

    // Skip empty nodes and layout specs because they don't mount anything.
    if (component == null || component.getLifecycle().getMountType() == NONE) {
      return null;
    }

    return createLayoutOutput(
        component,
        layoutState,
        node,
        true /* useNodePadding */,
        node.getImportantForAccessibility(),
        layoutState.mShouldDuplicateParentState);
  }

  private static LayoutOutput createHostLayoutOutput(LayoutState layoutState, InternalNode node) {
    final LayoutOutput hostOutput = createLayoutOutput(
        HostComponent.create(),
        layoutState,
        node,
        false /* useNodePadding */,
        node.getImportantForAccessibility(),
        node.isDuplicateParentStateEnabled());

    hostOutput.getViewNodeInfo().setTransitionKey(node.getTransitionKey());

    return hostOutput;
  }

  private static LayoutOutput createDrawableLayoutOutput(
      Component<?> component,
      LayoutState layoutState,
      InternalNode node) {
    return createLayoutOutput(
        component,
        layoutState,
        node,
        false /* useNodePadding */,
        IMPORTANT_FOR_ACCESSIBILITY_NO,
        layoutState.mShouldDuplicateParentState);
  }

  private static LayoutOutput createLayoutOutput(
      Component<?> component,
      LayoutState layoutState,
      InternalNode node,
      boolean useNodePadding,
      int importantForAccessibility,
      boolean duplicateParentState) {
    final boolean isMountViewSpec = isMountViewSpec(component);

    final LayoutOutput layoutOutput = ComponentsPools.acquireLayoutOutput();
    layoutOutput.setComponent(component);
    layoutOutput.setImportantForAccessibility(importantForAccessibility);

    // The mount operation will need both the marker for the target host and its matching
    // parent host to ensure the correct hierarchy when nesting the host views.
    layoutOutput.setHostMarker(layoutState.mCurrentHostMarker);

    if (layoutState.mCurrentHostOutputPosition >= 0) {
      final LayoutOutput hostOutput =
          layoutState.mMountableOutputs.get(layoutState.mCurrentHostOutputPosition);

      final Rect hostBounds = hostOutput.getBounds();
      layoutOutput.setHostTranslationX(hostBounds.left);
      layoutOutput.setHostTranslationY(hostBounds.top);
    }

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
      viewNodeInfo.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
      viewNodeInfo.setLayoutDirection(node.getResolvedLayoutDirection());
      viewNodeInfo.setExpandedTouchBounds(node, l, t, r, b);
      layoutOutput.setViewNodeInfo(viewNodeInfo);
      viewNodeInfo.release();
    } else {
      l += paddingLeft;
      t += paddingTop;
      r -= paddingRight;
      b -= paddingBottom;
    }

    layoutOutput.setBounds(l, t, r, b);

    int flags = 0;
    if (duplicateParentState) {
      flags |= FLAG_DUPLICATE_PARENT_STATE;
    }

    layoutOutput.setFlags(flags);

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

    final EventHandler visibleHandler = node.getVisibleHandler();
    final EventHandler focusedHandler = node.getFocusedHandler();
    final EventHandler fullImpressionHandler = node.getFullImpressionHandler();
    final EventHandler invisibleHandler = node.getInvisibleHandler();
    final VisibilityOutput visibilityOutput = ComponentsPools.acquireVisibilityOutput();
    final Component<?> handlerComponent;

    // Get the component from the handler that is not null. If more than one is not null, then
    // getting the component from any of them works.
    if (visibleHandler != null) {
      handlerComponent = (Component<?>) visibleHandler.mHasEventDispatcher;
    } else if (focusedHandler != null) {
      handlerComponent = (Component<?>) focusedHandler.mHasEventDispatcher;
    } else if (fullImpressionHandler != null) {
      handlerComponent = (Component<?>) fullImpressionHandler.mHasEventDispatcher;
    } else {
      handlerComponent = (Component<?>) invisibleHandler.mHasEventDispatcher;
    }

    visibilityOutput.setComponent(handlerComponent);

    visibilityOutput.setBounds(l, t, r, b);
    visibilityOutput.setVisibleEventHandler(visibleHandler);
    visibilityOutput.setFocusedEventHandler(focusedHandler);
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
    final Component<?> component = node.getComponent();
    final NodeInfo nodeInfo = node.getNodeInfo();

    final boolean implementsAccessibility =
        (nodeInfo != null && nodeInfo.hasAccessibilityHandlers())
        || (component != null && component.getLifecycle().implementsAccessibility());

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

    final boolean hasTouchEventHandlers = (nodeInfo != null && nodeInfo.hasTouchEventHandlers());
    final boolean hasViewTag = (nodeInfo != null && nodeInfo.getViewTag() != null);
    final boolean hasViewTags = (nodeInfo != null && nodeInfo.getViewTags() != null);
    final boolean isFocusableSetTrue =
        (nodeInfo != null && nodeInfo.getFocusState() == FOCUS_SET_TRUE);

    return hasTouchEventHandlers
        || hasViewTag
        || hasViewTags
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
    final Component<?> component = node.getComponent();

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

    final boolean needsHostView = needsHostView(node, layoutState);

    final long currentHostMarker = layoutState.mCurrentHostMarker;
    final int currentHostOutputPosition = layoutState.mCurrentHostOutputPosition;

    int hostLayoutPosition = -1;

    // 1. Insert a host LayoutOutput if we have some interactive content to be attached to.
    if (needsHostView) {
      hostLayoutPosition = addHostLayoutOutput(node, layoutState, diffNode);

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
    final LayoutOutput layoutOutput = createGenericLayoutOutput(node, layoutState);
    if (layoutOutput != null) {
      final long previousId = shouldUseCachedOutputs ? currentDiffNode.getContent().getId() : -1;
      layoutState.mLayoutStateOutputIdCalculator.calculateAndSetLayoutOutputIdAndUpdateState(
          layoutOutput,
          layoutState.mCurrentLevel,
          LayoutOutput.TYPE_CONTENT,
          previousId,
          isCachedOutputUpdated);
    }

    // If we don't need to update this output we can safely re-use the display list from the
    // previous output.
    if (isCachedOutputUpdated) {
      layoutOutput.setDisplayList(currentDiffNode.getContent().getDisplayList());
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

        final LayoutOutput backgroundOutput = addDrawableComponent(
            node,
            layoutState,
            convertBackground,
            background,
            LayoutOutput.TYPE_BACKGROUND);

        if (diffNode != null) {
          diffNode.setBackground(backgroundOutput);
        }
      }
    }

    // 3. Now add the MountSpec (either View or Drawable) to the Outputs.
    if (isMountSpec(component)) {
      // Notify component about its final size.
      component.getLifecycle().onBoundsDefined(layoutState.mContext, node, component);

      addMountableOutput(layoutState, layoutOutput);
      addLayoutOutputIdToPositionsMap(
          layoutState.mOutputsIdToPositionMap,
          layoutOutput,
          layoutState.mMountableOutputs.size() - 1);

      if (diffNode != null) {
        diffNode.setContent(layoutOutput);
      }
    }

    // 4. Add border color if defined.
    if (node.shouldDrawBorders()) {
      final LayoutOutput convertBorder = (currentDiffNode != null)
          ? currentDiffNode.getBorder()
          : null;

      final LayoutOutput borderOutput = addDrawableComponent(
          node,
          layoutState,
          convertBorder,
          getBorderColorDrawable(node),
          LayoutOutput.TYPE_BORDER);
      if (diffNode != null) {
        diffNode.setBorder(borderOutput);
      }
    }

    // 5. Extract the Transitions.
    if (SDK_INT >= ICE_CREAM_SANDWICH) {
      if (node.getTransitionKey() != null) {
        layoutState
            .getOrCreateTransitionContext()
            .addTransitionKey(node.getTransitionKey());
      }
      if (component != null) {
        Transition transition = component.getLifecycle().onLayoutTransition(
            layoutState.mContext,
            component);

        if (transition != null) {
          layoutState.getOrCreateTransitionContext().add(transition);
        }
      }
    }

    layoutState.mCurrentX += node.getX();
    layoutState.mCurrentY += node.getY();

    // We must process the nodes in order so that the layout state output order is correct.
    for (int i = 0, size = node.getChildCount(); i < size; i++) {
      collectResults(
          node.getChildAt(i),
          layoutState,
          diffNode);
    }

    layoutState.mCurrentX -= node.getX();
    layoutState.mCurrentY -= node.getY();

    // 6. Add foreground if defined.
    final Reference<? extends Drawable> foreground = node.getForeground();
    if (foreground != null) {
      if (layoutOutput != null && layoutOutput.hasViewNodeInfo() && SDK_INT >= M) {
        layoutOutput.getViewNodeInfo().setForeground(foreground);
      } else {
        final LayoutOutput convertForeground = (currentDiffNode != null)
            ? currentDiffNode.getForeground()
            : null;

        final LayoutOutput foregroundOutput = addDrawableComponent(
            node,
            layoutState,
            convertForeground,
            foreground,
            LayoutOutput.TYPE_FOREGROUND);

        if (diffNode != null) {
          diffNode.setForeground(foregroundOutput);
        }
      }
    }

    // 7. Add VisibilityOutputs if any visibility-related event handlers are present.
    if (node.hasVisibilityHandlers()) {
      final VisibilityOutput visibilityOutput = createVisibilityOutput(node, layoutState);
      final long previousId =
          shouldUseCachedOutputs ? currentDiffNode.getVisibilityOutput().getId() : -1;

      layoutState.mLayoutStateOutputIdCalculator.calculateAndSetVisibilityOutputId(
          visibilityOutput,
          layoutState.mCurrentLevel,
          previousId);
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

    // All children for the given host have been added, restore the previous
    // host, level, and duplicate parent state value in the recursive queue.
    if (layoutState.mCurrentHostMarker != currentHostMarker) {
      layoutState.mCurrentHostMarker = currentHostMarker;
      layoutState.mCurrentHostOutputPosition = currentHostOutputPosition;
      layoutState.mCurrentLevel--;
    }
    layoutState.mShouldDuplicateParentState = shouldDuplicateParentState;

    Collections.sort(layoutState.mMountableOutputTops, sTopsComparator);
    Collections.sort(layoutState.mMountableOutputBottoms, sBottomsComparator);
  }

  private static void calculateAndSetHostOutputIdAndUpdateState(
      InternalNode node,
      LayoutOutput hostOutput,
      LayoutState layoutState,
      boolean isCachedOutputUpdated) {
    if (layoutState.isLayoutRoot(node)) {
      // The root host (ComponentView) always has ID 0 and is unconditionally
      // set as dirty i.e. no need to use shouldComponentUpdate().
      hostOutput.setId(ROOT_HOST_ID);

      // Special case where the host marker of the root host is pointing to itself.
      hostOutput.setHostMarker(ROOT_HOST_ID);
      hostOutput.setUpdateState(LayoutOutput.STATE_DIRTY);
    } else {
      layoutState.mLayoutStateOutputIdCalculator.calculateAndSetLayoutOutputIdAndUpdateState(
          hostOutput,
          layoutState.mCurrentLevel,
          LayoutOutput.TYPE_HOST,
          -1,
          isCachedOutputUpdated);
    }
  }

  private static LayoutOutput addDrawableComponent(
      InternalNode node,
      LayoutState layoutState,
      LayoutOutput recycle,
      Reference<? extends Drawable> reference,
      @LayoutOutput.LayoutOutputType int type) {
    final Component<DrawableComponent> drawableComponent = DrawableComponent.create(reference);
    drawableComponent.setScopedContext(
        ComponentContext.withComponentScope(node.getContext(), drawableComponent));
    final boolean isOutputUpdated;
    if (recycle != null) {
      isOutputUpdated = !drawableComponent.getLifecycle().shouldComponentUpdate(
          recycle.getComponent(),
          drawableComponent);
    } else {
      isOutputUpdated = false;
    }

    final long previousId = recycle != null ? recycle.getId() : -1;
    final LayoutOutput output = addDrawableLayoutOutput(
        drawableComponent,
        layoutState,
        node,
        type,
        previousId,
        isOutputUpdated);

    return output;
  }

  private static Reference<? extends Drawable> getBorderColorDrawable(InternalNode node) {
    if (!node.shouldDrawBorders()) {
      throw new RuntimeException("This node does not support drawing border color");
    }

    return BorderColorDrawableReference.create(node.getContext())
                .color(node.getBorderColor())
                .borderLeft(FastMath.round(node.mYogaNode.getLayoutBorder(YogaEdge.LEFT)))
                .borderTop(FastMath.round(node.mYogaNode.getLayoutBorder(YogaEdge.TOP)))
                .borderRight(FastMath.round(node.mYogaNode.getLayoutBorder(YogaEdge.RIGHT)))
                .borderBottom(FastMath.round(node.mYogaNode.getLayoutBorder(YogaEdge.BOTTOM)))
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

  private static LayoutOutput addDrawableLayoutOutput(
      Component<DrawableComponent> drawableComponent,
      LayoutState layoutState,
      InternalNode node,
      @LayoutOutput.LayoutOutputType int layoutOutputType,
      long previousId,
      boolean isCachedOutputUpdated) {

    drawableComponent.getLifecycle().onBoundsDefined(
        layoutState.mContext,
        node,
        drawableComponent);

    final LayoutOutput drawableLayoutOutput = createDrawableLayoutOutput(
        drawableComponent,
        layoutState,
        node);
    layoutState.mLayoutStateOutputIdCalculator.calculateAndSetLayoutOutputIdAndUpdateState(
        drawableLayoutOutput,
        layoutState.mCurrentLevel,
        layoutOutputType,
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

    ComponentsPools.release(node);
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
    final Component<?> component = node.getComponent();

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

    return hostOutputPosition;
  }

  static <T extends ComponentLifecycle> LayoutState calculate(
      ComponentContext c,
      Component<T> component,
      int componentTreeId,
      int widthSpec,
      int heightSpec,
      boolean shouldGenerateDiffTree,
      DiffNode previousDiffTreeRoot) {

    // Detect errors internal to components
    component.markLayoutStarted();

    LayoutState layoutState = ComponentsPools.acquireLayoutState(c);
    layoutState.mShouldGenerateDiffTree = shouldGenerateDiffTree;
    layoutState.mComponentTreeId = componentTreeId;
    layoutState.mAccessibilityManager =
        (AccessibilityManager) c.getSystemService(ACCESSIBILITY_SERVICE);
    layoutState.mAccessibilityEnabled = isAccessibilityEnabled(layoutState.mAccessibilityManager);
    layoutState.mComponent = component;
    layoutState.mWidthSpec = widthSpec;
    layoutState.mHeightSpec = heightSpec;

    component.applyStateUpdates(c);

    final InternalNode root = createAndMeasureTreeForComponent(
        component.getScopedContext(),
        component,
        null, // nestedTreeHolder is null because this is measuring the root component tree.
        widthSpec,
        heightSpec,
        previousDiffTreeRoot);

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

    layoutState.mLayoutStateOutputIdCalculator.clear();

    // Reset markers before collecting layout outputs.
    layoutState.mCurrentHostMarker = -1;

    final ComponentsLogger logger = c.getLogger();

    if (root == NULL_LAYOUT) {
      return layoutState;
    }

    layoutState.mLayoutRoot = root;

    ComponentsSystrace.beginSection("collectResults:" + component.getSimpleName());
    if (logger != null) {
      logger.eventStart(EVENT_COLLECT_RESULTS, component, PARAM_LOG_TAG, c.getLogTag());
    }

    collectResults(root, layoutState, null);

    if (logger != null) {
      logger.eventEnd(EVENT_COLLECT_RESULTS, component, ACTION_SUCCESS);
    }
    ComponentsSystrace.endSection();

    if (!ComponentsConfiguration.IS_INTERNAL_BUILD && layoutState.mLayoutRoot != null) {
      releaseNodeTree(layoutState.mLayoutRoot, false /* isNestedTree */);
      layoutState.mLayoutRoot = null;
    }

    if (ThreadUtils.isMainThread() && ComponentsConfiguration.shouldGenerateDisplayLists) {
      collectDisplayLists(layoutState);
    }

    return layoutState;
  }

  void preAllocateMountContent() {
    if (mMountableOutputs != null && !mMountableOutputs.isEmpty()) {
      for (int i = 0, size = mMountableOutputs.size(); i < size; i++) {
        final Component component = mMountableOutputs.get(i).getComponent();

        if (Component.isMountViewSpec(component)) {
          final ComponentLifecycle lifecycle = component.getLifecycle();

          if (!lifecycle.hasBeenPreallocated()) {
            final int poolSize = lifecycle.poolSize();

            int insertedCount = 0;
            while (insertedCount < poolSize &&
                ComponentsPools.canAddMountContentToPool(mContext, lifecycle)) {
              ComponentsPools.release(
                  mContext,
                  lifecycle,
                  lifecycle.createMountContent(mContext));
              insertedCount++;
            }

            lifecycle.setWasPreallocated();
          }
        }
      }
    }
  }

  private static void collectDisplayLists(LayoutState layoutState) {
    final Rect rect = new Rect();
    final ComponentContext context = layoutState.mContext;
    final Activity activity = findActivityInContext(context);

    if (activity == null || activity.isFinishing() || isActivityDestroyed(activity)) {
      return;
    }

    for (int i = 0, count = layoutState.getMountableOutputCount(); i < count; i++) {
      final LayoutOutput output = layoutState.getMountableOutputAt(i);
      final Component component = output.getComponent();
      final ComponentLifecycle lifecycle = component.getLifecycle();

      if (lifecycle.shouldUseDisplayList()) {
        output.getMountBounds(rect);

        if (output.getDisplayList() != null && output.getDisplayList().isValid()) {
          // This output already has a valid DisplayList from diffing. No need to re-create it.
          // Just update its bounds.
          try {
            output.getDisplayList().setBounds(rect.left, rect.top, rect.right, rect.bottom);
            continue;
          } catch (DisplayListException e) {
            // Nothing to do here.
          }
        }

        final DisplayList displayList = DisplayList.createDisplayList(
            lifecycle.getClass().getSimpleName());

        if (displayList != null) {
          Drawable drawable =
              (Drawable) ComponentsPools.acquireMountContent(context, lifecycle.getId());
          if (drawable == null) {
            drawable = (Drawable) lifecycle.createMountContent(context);
          }

          final LayoutOutput clickableOutput = findInteractiveRoot(layoutState, output);
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
              context,
              drawable,
              component);
          lifecycle.bind(context, drawable, component);

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

          lifecycle.unbind(context, drawable, component);
          lifecycle.unmount(context, drawable, component);
          ComponentsPools.release(context, lifecycle, drawable);
        }
      }
    }
  }

  private static LayoutOutput findInteractiveRoot(LayoutState layoutState, LayoutOutput output) {
    if (output.getId() == ROOT_HOST_ID) {
      return output;
    }

    if ((output.getFlags() & FLAG_DUPLICATE_PARENT_STATE) != 0) {
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

  private static boolean isActivityDestroyed(Activity activity) {
    if (SDK_INT >= JELLY_BEAN_MR1) {
      return activity.isDestroyed();
    }

    return false;
  }

  private static Activity findActivityInContext(Context context) {
    if (context instanceof Activity) {
      return (Activity) context;
    } else if (context instanceof ContextWrapper) {
      return findActivityInContext(((ContextWrapper) context).getBaseContext());
    }

    return null;
  }

  @VisibleForTesting
  static <T extends ComponentLifecycle> InternalNode createTree(
      Component<T> component,
      ComponentContext context) {
    final ComponentsLogger logger = context.getLogger();

    if (logger != null) {
      logger.eventStart(EVENT_CREATE_LAYOUT, context, PARAM_LOG_TAG, context.getLogTag());
      logger.eventAddTag(EVENT_CREATE_LAYOUT, context, component.getSimpleName());
    }

    final InternalNode root = (InternalNode) component.getLifecycle().createLayout(
        context,
        component,
        true /* resolveNestedTree */);

    if (logger != null) {
      logger.eventEnd(EVENT_CREATE_LAYOUT, context, ACTION_SUCCESS);
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
    final Component component = root.getComponent();
    ComponentsSystrace.beginSection("measureTree:" + component.getSimpleName());

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
    if (logger != null) {
      logger.eventStart(EVENT_CSS_LAYOUT, component, PARAM_LOG_TAG, context.getLogTag());
      logger.eventAddParam(
          EVENT_CSS_LAYOUT,
          component,
          PARAM_TREE_DIFF_ENABLED,
          String.valueOf(previousDiffTreeRoot != null));
    }

    root.calculateLayout(
        SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED
            ? YogaConstants.UNDEFINED
            : SizeSpec.getSize(widthSpec),
        SizeSpec.getMode(heightSpec) == SizeSpec.UNSPECIFIED
            ? YogaConstants.UNDEFINED
            : SizeSpec.getSize(heightSpec));

    if (logger != null) {
      logger.eventEnd(EVENT_CSS_LAYOUT, component, ACTION_SUCCESS);
    }
    ComponentsSystrace.endSection(/* measureTree */);
  }

  /**
   * Create and measure the nested tree or return the cached one for the same size specs.
   */
  static InternalNode resolveNestedTree(
      InternalNode nestedTreeHolder,
      int widthSpec,
      int heightSpec) {
    final ComponentContext context = nestedTreeHolder.getContext();
    final Component<?> component = nestedTreeHolder.getComponent();

    InternalNode nestedTree = nestedTreeHolder.getNestedTree();

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
    // Account for the size specs in ComponentContext in case the tree is a NestedTree.
    final int previousWidthSpec = c.getWidthSpec();
    final int previousHeightSpec = c.getHeightSpec();

    final boolean hasNestedTreeHolder = nestedTreeHolder != null;

    c.setWidthSpec(widthSpec);
    c.setHeightSpec(heightSpec);

    if (hasNestedTreeHolder) {
      c.setTreeProps(nestedTreeHolder.getPendingTreeProps());
    }

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
    diffNode.setComponent(node.getComponent());
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
    if (isLayoutSpecWithSizeSpec(layoutNode.getComponent()) && !isTreeRoot) {
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
    final Component component = layoutNode.getComponent();
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

    return isSameComponentType(node.getComponent(), diffNode.getComponent());
  }

  private static boolean isSameComponentType(Component a, Component b) {
    if (a == b) {
      return true;
    } else if (a == null || b == null) {
      return false;
    }
    return a.getLifecycle().getClass().equals(b.getLifecycle().getClass());
  }

  private static boolean shouldComponentUpdate(InternalNode layoutNode, DiffNode diffNode) {
    if (diffNode == null) {
      return true;
    }

    final Component component = layoutNode.getComponent();
    if (component != null) {
      return component.getLifecycle().shouldComponentUpdate(component, diffNode.getComponent());
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

  boolean isComponentId(int componentId) {
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

      for (int i = 0, size = mMountableOutputs.size(); i < size; i++) {
        ComponentsPools.release(mMountableOutputs.get(i));
      }
      mMountableOutputs.clear();
      mMountableOutputTops.clear();
      mMountableOutputBottoms.clear();
      mOutputsIdToPositionMap.clear();

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
      mLayoutStateOutputIdCalculator.clear();

      if (mTransitionContext != null) {
        ComponentsPools.release(mTransitionContext);
        mTransitionContext = null;
      }

      // This should only ever be true in non-release builds as we need this for Stetho integration.
      // In release builds the node tree is released in calculateLayout().
      if (mLayoutRoot != null) {
        releaseNodeTree(mLayoutRoot, false /* isNestedTree */);
        mLayoutRoot = null;
      }

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
  LayoutState acquireRef() {
    if (mReferenceCount.getAndIncrement() == 0) {
      throw new IllegalStateException("Trying to use a released LayoutState");
    }

    return this;
  }

  void init(ComponentContext context) {
    mContext = context;
    mStateHandler = mContext.getStateHandler();
    mReferenceCount.set(1);
  }

  /**
   * Returns the state handler instance currently held by LayoutState and nulls it afterwards.
   * @return the state handler
   */
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

