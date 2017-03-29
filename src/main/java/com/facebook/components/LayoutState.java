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
