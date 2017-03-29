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
