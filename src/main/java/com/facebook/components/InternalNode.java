/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.Rect;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.DimenRes;
import android.support.annotation.Dimension;
import android.support.annotation.DrawableRes;
import android.support.annotation.Px;
import android.support.annotation.StringRes;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.SparseArray;

import com.facebook.R;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.reference.ColorDrawableReference;
import com.facebook.litho.reference.Reference;
import com.facebook.litho.reference.ResourceDrawableReference;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaBaselineFunction;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaPositionType;
import com.facebook.yoga.YogaWrap;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaNode;
import com.facebook.yoga.YogaNodeAPI;
import com.facebook.yoga.YogaOverflow;
import com.facebook.yoga.Spacing;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.support.annotation.Dimension.DP;
import static com.facebook.litho.ComponentContext.NULL_LAYOUT;
import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.END;
import static com.facebook.yoga.YogaEdge.HORIZONTAL;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.RIGHT;
import static com.facebook.yoga.YogaEdge.START;
import static com.facebook.yoga.YogaEdge.TOP;
import static com.facebook.yoga.YogaEdge.VERTICAL;

/**
 * Internal class representing both a {@link ComponentLayout} and a
 * {@link com.facebook.litho.ComponentLayout.ContainerBuilder}.
 */
@ThreadConfined(ThreadConfined.ANY)
class InternalNode implements ComponentLayout, ComponentLayout.ContainerBuilder {

  // Used to check whether or not the framework can use style IDs for
  // paddingStart/paddingEnd due to a bug in some Android devices.
  private static final boolean SUPPORTS_RTL = (SDK_INT >= JELLY_BEAN_MR1);

  // When this flag is set, layoutDirection style was explicitly set on this node.
  private static final long PFLAG_LAYOUT_DIRECTION_IS_SET = 1L << 0;
  // When this flag is set, alignSelf was explicitly set on this node.
  private static final long PFLAG_ALIGN_SELF_IS_SET = 1L << 1;
  // When this flag is set, position type was explicitly set on this node.
  private static final long PFLAG_POSITION_TYPE_IS_SET = 1L << 2;
  // When this flag is set, flex was explicitly set on this node.
  private static final long PFLAG_FLEX_IS_SET = 1L << 3;
  // When this flag is set, flex grow was explicitly set on this node.
  private static final long PFLAG_FLEX_GROW_IS_SET = 1L << 4;
  // When this flag is set, flex shrink was explicitly set on this node.
  private static final long PFLAG_FLEX_SHRINK_IS_SET = 1L << 5;
  // When this flag is set, flex basis was explicitly set on this node.
  private static final long PFLAG_FLEX_BASIS_IS_SET = 1L << 6;
  // When this flag is set, importantForAccessibility was explicitly set on this node.
  private static final long PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET = 1L << 7;
  // When this flag is set, duplicateParentState was explicitly set on this node.
  private static final long PFLAG_DUPLICATE_PARENT_STATE_IS_SET = 1L << 8;
  // When this flag is set, margin was explicitly set on this node.
  private static final long PFLAG_MARGIN_IS_SET = 1L << 9;
  // When this flag is set, padding was explicitly set on this node.
  private static final long PFLAG_PADDING_IS_SET = 1L << 10;
  // When this flag is set, position was explicitly set on this node.
  private static final long PFLAG_POSITION_IS_SET = 1L << 11;
  // When this flag is set, width was explicitly set on this node.
  private static final long PFLAG_WIDTH_IS_SET = 1L << 12;
  // When this flag is set, minWidth was explicitly set on this node.
  private static final long PFLAG_MIN_WIDTH_IS_SET = 1L << 13;
  // When this flag is set, maxWidth was explicitly set on this node.
  private static final long PFLAG_MAX_WIDTH_IS_SET = 1L << 14;
  // When this flag is set, height was explicitly set on this node.
  private static final long PFLAG_HEIGHT_IS_SET = 1L << 15;
  // When this flag is set, minHeight was explicitly set on this node.
  private static final long PFLAG_MIN_HEIGHT_IS_SET = 1L << 16;
  // When this flag is set, maxHeight was explicitly set on this node.
  private static final long PFLAG_MAX_HEIGHT_IS_SET = 1L << 17;
  // When this flag is set, background was explicitly set on this node.
  private static final long PFLAG_BACKGROUND_IS_SET = 1L << 18;
  // When this flag is set, foreground was explicitly set on this node.
  private static final long PFLAG_FOREGROUND_IS_SET = 1L << 19;
  // When this flag is set, visibleHandler was explicitly set on this node.
  private static final long PFLAG_VISIBLE_HANDLER_IS_SET = 1L << 20;
  // When this flag is set, focusedHandler was explicitly set on this node.
  private static final long PFLAG_FOCUSED_HANDLER_IS_SET = 1L << 21;
  // When this flag is set, fullImpressionHandler was explicitly set on this node.
  private static final long PFLAG_FULL_IMPRESSION_HANDLER_IS_SET = 1L << 22;
  // When this flag is set, invisibleHandler was explicitly set on this node.
  private static final long PFLAG_INVISIBLE_HANDLER_IS_SET = 1L << 23;
  // When this flag is set, touch expansion was explicitly set on this node.
  private static final long PFLAG_TOUCH_EXPANSION_IS_SET = 1L << 24;
  // When this flag is set, border width was explicitly set on this node.
  private static final long PFLAG_BORDER_WIDTH_IS_SET = 1L << 25;
  // When this flag is set, aspectRatio was explicitly set on this node.
  private static final long PFLAG_ASPECT_RATIO_IS_SET = 1L << 26;
  // When this flag is set, transitionKey was explicitly set on this node.
  private static final long PFLAG_TRANSITION_KEY_IS_SET = 1L << 27;
  // When this flag is set, border color was explicitly set on this node.
  private static final long PFLAG_BORDER_COLOR_IS_SET = 1L << 28;

  private final ResourceResolver mResourceResolver = new ResourceResolver();

  YogaNodeAPI mYogaNode;
  private ComponentContext mComponentContext;
  private Resources mResources;
  private Component mComponent;
  private int mImportantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
  private boolean mDuplicateParentState;
  private boolean mIsNestedTreeHolder;
  private InternalNode mNestedTree;
  private InternalNode mNestedTreeHolder;
  private long mPrivateFlags;

  private Reference<? extends Drawable> mBackground;
  private Reference<? extends Drawable> mForeground;
  private int mBorderColor = Color.TRANSPARENT;

  private NodeInfo mNodeInfo;
  private boolean mForceViewWrapping;
  private String mTransitionKey;
  private EventHandler mVisibleHandler;
  private EventHandler mFocusedHandler;
  private EventHandler mFullImpressionHandler;
  private EventHandler mInvisibleHandler;
  private String mTestKey;
  private Spacing mTouchExpansion;
  private Spacing mNestedTreePadding;
  private Spacing mNestedTreeBorderWidth;
  private boolean[] mIsPaddingPercent;

  private float mResolvedTouchExpansionLeft = YogaConstants.UNDEFINED;
  private float mResolvedTouchExpansionRight = YogaConstants.UNDEFINED;
  private float mResolvedX = YogaConstants.UNDEFINED;
  private float mResolvedY = YogaConstants.UNDEFINED;
  private float mResolvedWidth = YogaConstants.UNDEFINED;
  private float mResolvedHeight = YogaConstants.UNDEFINED;

  private int mLastWidthSpec = DiffNode.UNSPECIFIED;
  private int mLastHeightSpec = DiffNode.UNSPECIFIED;
  private float mLastMeasuredWidth = DiffNode.UNSPECIFIED;
  private float mLastMeasuredHeight = DiffNode.UNSPECIFIED;
  private DiffNode mDiffNode;

  private boolean mCachedMeasuresValid;
  private TreeProps mPendingTreeProps;

  void init(YogaNodeAPI yogaNode, ComponentContext componentContext, Resources resources) {
    yogaNode.setData(this);
    yogaNode.setOverflow(YogaOverflow.HIDDEN);
    yogaNode.setMeasureFunction(null);

    // YogaNode is the only version of YogaNodeAPI with this support;
    if (yogaNode instanceof YogaNode) {
      yogaNode.setBaselineFunction(null);
    }

    mYogaNode = yogaNode;

    mComponentContext = componentContext;
    mResources = resources;
    mResourceResolver.init(
        mComponentContext,
        componentContext.getResourceCache());
  }

  @Px
  @Override
  public int getX() {
    if (YogaConstants.isUndefined(mResolvedX)) {
      mResolvedX = mYogaNode.getLayoutX();
    }

    return (int) mResolvedX;
  }

  @Px
  @Override
  public int getY() {
    if (YogaConstants.isUndefined(mResolvedY)) {
      mResolvedY = mYogaNode.getLayoutY();
    }

    return (int) mResolvedY;
  }

  @Px
  @Override
  public int getWidth() {
    if (YogaConstants.isUndefined(mResolvedWidth)) {
      mResolvedWidth = mYogaNode.getLayoutWidth();
    }

    return (int) mResolvedWidth;
  }

  @Px
  @Override
  public int getHeight() {
    if (YogaConstants.isUndefined(mResolvedHeight)) {
      mResolvedHeight = mYogaNode.getLayoutHeight();
    }

    return (int) mResolvedHeight;
  }

  @Px
  @Override
  public int getPaddingLeft() {
    return FastMath.round(mYogaNode.getLayoutPadding(LEFT));
  }

  @Px
  @Override
  public int getPaddingTop() {
    return FastMath.round(mYogaNode.getLayoutPadding(TOP));
  }

  @Px
  @Override
  public int getPaddingRight() {
    return FastMath.round(mYogaNode.getLayoutPadding(RIGHT));
  }

  @Px
  @Override
  public int getPaddingBottom() {
    return FastMath.round(mYogaNode.getLayoutPadding(BOTTOM));
  }

  public Reference<? extends Drawable> getBackground() {
    return mBackground;
  }

  public Reference<? extends Drawable> getForeground() {
    return mForeground;
  }

  public void setCachedMeasuresValid(boolean valid) {
    mCachedMeasuresValid = valid;
  }

  public int getLastWidthSpec() {
    return mLastWidthSpec;
  }

  public void setLastWidthSpec(int widthSpec) {
    mLastWidthSpec = widthSpec;
  }

  public int getLastHeightSpec() {
    return mLastHeightSpec;
