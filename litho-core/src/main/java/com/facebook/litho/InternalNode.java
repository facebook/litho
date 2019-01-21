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

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static com.facebook.litho.ComponentContext.NULL_LAYOUT;
import static com.facebook.litho.ComponentsLogger.LogLevel.WARNING;
import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.END;
import static com.facebook.yoga.YogaEdge.HORIZONTAL;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.RIGHT;
import static com.facebook.yoga.YogaEdge.START;
import static com.facebook.yoga.YogaEdge.TOP;
import static com.facebook.yoga.YogaEdge.VERTICAL;

import android.animation.StateListAnimator;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PathEffect;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.Px;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.ViewOutlineProvider;
import com.facebook.infer.annotation.ReturnsOwnership;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.drawable.ComparableColorDrawable;
import com.facebook.litho.drawable.ComparableDrawable;
import com.facebook.litho.drawable.ComparableResDrawable;
import com.facebook.litho.drawable.DefaultComparableDrawable;
import com.facebook.litho.reference.DrawableReference;
import com.facebook.litho.reference.Reference;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaBaselineFunction;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaNode;
import com.facebook.yoga.YogaPositionType;
import com.facebook.yoga.YogaWrap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

/** Internal class representing a {@link ComponentLayout}. */
@ThreadConfined(ThreadConfined.ANY)
class InternalNode implements ComponentLayout {

  // Used to check whether or not the framework can use style IDs for
  // paddingStart/paddingEnd due to a bug in some Android devices.
  private static final boolean SUPPORTS_RTL = (SDK_INT >= JELLY_BEAN_MR1);

  // Flags used to indicate that a certain attribute was explicitly set on the node.
  private static final long PFLAG_LAYOUT_DIRECTION_IS_SET = 1L << 0;
  private static final long PFLAG_ALIGN_SELF_IS_SET = 1L << 1;
  private static final long PFLAG_POSITION_TYPE_IS_SET = 1L << 2;
  private static final long PFLAG_FLEX_IS_SET = 1L << 3;
  private static final long PFLAG_FLEX_GROW_IS_SET = 1L << 4;
  private static final long PFLAG_FLEX_SHRINK_IS_SET = 1L << 5;
  private static final long PFLAG_FLEX_BASIS_IS_SET = 1L << 6;
  private static final long PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET = 1L << 7;
  private static final long PFLAG_DUPLICATE_PARENT_STATE_IS_SET = 1L << 8;
  private static final long PFLAG_MARGIN_IS_SET = 1L << 9;
  private static final long PFLAG_PADDING_IS_SET = 1L << 10;
  private static final long PFLAG_POSITION_IS_SET = 1L << 11;
  private static final long PFLAG_WIDTH_IS_SET = 1L << 12;
  private static final long PFLAG_MIN_WIDTH_IS_SET = 1L << 13;
  private static final long PFLAG_MAX_WIDTH_IS_SET = 1L << 14;
  private static final long PFLAG_HEIGHT_IS_SET = 1L << 15;
  private static final long PFLAG_MIN_HEIGHT_IS_SET = 1L << 16;
  private static final long PFLAG_MAX_HEIGHT_IS_SET = 1L << 17;
  private static final long PFLAG_BACKGROUND_IS_SET = 1L << 18;
  private static final long PFLAG_FOREGROUND_IS_SET = 1L << 19;
  private static final long PFLAG_VISIBLE_HANDLER_IS_SET = 1L << 20;
  private static final long PFLAG_FOCUSED_HANDLER_IS_SET = 1L << 21;
  private static final long PFLAG_FULL_IMPRESSION_HANDLER_IS_SET = 1L << 22;
  private static final long PFLAG_INVISIBLE_HANDLER_IS_SET = 1L << 23;
  private static final long PFLAG_UNFOCUSED_HANDLER_IS_SET = 1L << 24;
  private static final long PFLAG_TOUCH_EXPANSION_IS_SET = 1L << 25;
  private static final long PFLAG_ASPECT_RATIO_IS_SET = 1L << 26;
  private static final long PFLAG_TRANSITION_KEY_IS_SET = 1L << 27;
  private static final long PFLAG_BORDER_IS_SET = 1L << 28;
  private static final long PFLAG_STATE_LIST_ANIMATOR_SET = 1L << 29;
  private static final long PFLAG_STATE_LIST_ANIMATOR_RES_SET = 1L << 30;
  private static final long PFLAG_VISIBLE_RECT_CHANGED_HANDLER_IS_SET = 1L << 31;
  private static final long PFLAG_TRANSITION_KEY_TYPE_IS_SET = 1L << 32;

  YogaNode mYogaNode;
  private ComponentContext mComponentContext;
  @ThreadConfined(ThreadConfined.ANY)
  private final List<Component> mComponents = new ArrayList<>(1);
  private int mImportantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
  private boolean mDuplicateParentState;
  private boolean mIsNestedTreeHolder;
  private InternalNode mNestedTree;
  private InternalNode mNestedTreeHolder;
  private long mPrivateFlags;

  private @Nullable Reference<? extends Drawable> mBackground;
  private @Nullable ComparableDrawable mForeground;
  private final int[] mBorderColors = new int[Border.EDGE_COUNT];
  private final float[] mBorderRadius = new float[Border.RADIUS_COUNT];
  private @Nullable PathEffect mBorderPathEffect;
  private @Nullable StateListAnimator mStateListAnimator;
  private @DrawableRes int mStateListAnimatorRes;

  private NodeInfo mNodeInfo;
  private boolean mForceViewWrapping;
  private String mTransitionKey;
  private @Nullable Transition.TransitionKeyType mTransitionKeyType;
  private float mVisibleHeightRatio;
  private float mVisibleWidthRatio;
  @Nullable private EventHandler<VisibleEvent> mVisibleHandler;
  @Nullable private EventHandler<FocusedVisibleEvent> mFocusedHandler;
  @Nullable private EventHandler<UnfocusedVisibleEvent> mUnfocusedHandler;
  @Nullable private EventHandler<FullImpressionVisibleEvent> mFullImpressionHandler;
  @Nullable private EventHandler<InvisibleEvent> mInvisibleHandler;
  private @Nullable EventHandler<VisibilityChangedEvent> mVisibilityChangedHandler;
  private String mTestKey;
  private Edges mTouchExpansion;
  private Edges mNestedTreePadding;
  private Edges mNestedTreeBorderWidth;
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
  private @Nullable ArrayList<Transition> mTransitions;
  private @Nullable ArrayList<Component> mComponentsNeedingPreviousRenderData;
  private @Nullable ArrayList<WorkingRangeContainer.Registration> mWorkingRangeRegistrations;

  private boolean mCachedMeasuresValid;
  private TreeProps mPendingTreeProps;

  // Hold onto DebugComponents which reference InternalNode to tie there Vm lifecycles together.
  // DebugComponents are supposed to be help onto as weak references and have we want to ensure they
  // live exactly as long as InternalNodes.
  @Nullable private Set<DebugComponent> mDebugComponents = null;

  @Nullable private int[] mExtraMemory;

  public InternalNode() {
    this(true);
  }

  protected InternalNode(boolean createDebugComponentsInCtor) {
    if (createDebugComponentsInCtor) {
      mDebugComponents = new HashSet<>();
    }
  }

  void init(YogaNode yogaNode, ComponentContext componentContext) {
    if (yogaNode != null) {
      yogaNode.setData(this);
    }
    mYogaNode = yogaNode;
    mComponentContext = componentContext;
    if (mComponentContext.getExtraMemorySize() > 0) {
      mExtraMemory = new int[mComponentContext.getExtraMemorySize()];
    }
  }

  /**
   * For testing and debugging purposes only where initialization may have not occurred. For
   * any production use, this should never be necessary.
   */
  boolean isInitialized() {
    return mYogaNode != null && mComponentContext != null;
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

  @Override
  public boolean isPaddingSet() {
    return (mPrivateFlags & PFLAG_PADDING_IS_SET) != 0L;
  }

  @Override
  public @Nullable Reference<? extends Drawable> getBackground() {
    return mBackground;
  }

  public @Nullable ComparableDrawable getForeground() {
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
  }

  public void setLastHeightSpec(int heightSpec) {
    mLastHeightSpec = heightSpec;
  }

  public boolean hasVisibilityHandlers() {
    return mVisibleHandler != null
        || mFocusedHandler != null
        || mUnfocusedHandler != null
        || mFullImpressionHandler != null
        || mInvisibleHandler != null
        || mVisibilityChangedHandler != null;
  }

  /**
   * The last value the measure funcion associated with this node {@link Component} returned for the
   * width. This is used together with {@link InternalNode#getLastWidthSpec()} to implement measure
   * caching.
   */
  float getLastMeasuredWidth() {
    return mLastMeasuredWidth;
  }

  /**
   * Sets the last value the measure funcion associated with this node {@link Component} returned
   * for the width.
   */
  void setLastMeasuredWidth(float lastMeasuredWidth) {
    mLastMeasuredWidth = lastMeasuredWidth;
  }

  /**
   * The last value the measure funcion associated with this node {@link Component} returned for the
   * height. This is used together with {@link InternalNode#getLastHeightSpec()} to implement
   * measure caching.
   */
  float getLastMeasuredHeight() {
    return mLastMeasuredHeight;
  }

  /**
   * Sets the last value the measure funcion associated with this node {@link Component} returned
   * for the height.
   */
  void setLastMeasuredHeight(float lastMeasuredHeight) {
    mLastMeasuredHeight = lastMeasuredHeight;
  }

  DiffNode getDiffNode() {
    return mDiffNode;
  }

  boolean areCachedMeasuresValid() {
    return mCachedMeasuresValid;
  }

  void setDiffNode(DiffNode diffNode) {
    mDiffNode = diffNode;
  }

  /**
   * Mark this node as a nested tree root holder.
   */
  void markIsNestedTreeHolder(TreeProps currentTreeProps) {
    mIsNestedTreeHolder = true;
    mPendingTreeProps = TreeProps.copy(currentTreeProps);
  }

  /**
   * @return Whether this node is holding a nested tree or not. The decision was made during tree
   *     creation {@link ComponentLifecycle#createLayout(ComponentContext, boolean)}.
   */
  boolean isNestedTreeHolder() {
    return mIsNestedTreeHolder;
  }

  @Override
  public YogaDirection getResolvedLayoutDirection() {
    return mYogaNode.getLayoutDirection();
  }

  /** Continually walks the node hierarchy until a node returns a non inherited layout direction */
  YogaDirection recursivelyResolveLayoutDirection() {
    YogaNode yogaNode = mYogaNode;
    while (yogaNode != null && yogaNode.getLayoutDirection() == YogaDirection.INHERIT) {
      yogaNode = yogaNode.getOwner();
    }
    return yogaNode == null ? YogaDirection.INHERIT : yogaNode.getLayoutDirection();
  }

  InternalNode layoutDirection(YogaDirection direction) {
    mPrivateFlags |= PFLAG_LAYOUT_DIRECTION_IS_SET;
    mYogaNode.setDirection(direction);
    return this;
  }

  InternalNode flexDirection(YogaFlexDirection direction) {
    mYogaNode.setFlexDirection(direction);
    return this;
  }

  InternalNode wrap(YogaWrap wrap) {
    mYogaNode.setWrap(wrap);
    return this;
  }

  InternalNode justifyContent(YogaJustify justifyContent) {
    mYogaNode.setJustifyContent(justifyContent);
    return this;
  }

  InternalNode alignItems(YogaAlign alignItems) {
    mYogaNode.setAlignItems(alignItems);
    return this;
  }

  InternalNode alignContent(YogaAlign alignContent) {
    mYogaNode.setAlignContent(alignContent);
    return this;
  }

  InternalNode alignSelf(YogaAlign alignSelf) {
    mPrivateFlags |= PFLAG_ALIGN_SELF_IS_SET;
    mYogaNode.setAlignSelf(alignSelf);
    return this;
  }

  InternalNode positionType(YogaPositionType positionType) {
    mPrivateFlags |= PFLAG_POSITION_TYPE_IS_SET;
    mYogaNode.setPositionType(positionType);
    return this;
  }

  InternalNode flex(float flex) {
    mPrivateFlags |= PFLAG_FLEX_IS_SET;
    mYogaNode.setFlex(flex);
    return this;
  }

  InternalNode flexGrow(float flexGrow) {
    mPrivateFlags |= PFLAG_FLEX_GROW_IS_SET;
    mYogaNode.setFlexGrow(flexGrow);
    return this;
  }

  InternalNode flexShrink(float flexShrink) {
    mPrivateFlags |= PFLAG_FLEX_SHRINK_IS_SET;
    mYogaNode.setFlexShrink(flexShrink);
    return this;
  }

  InternalNode flexBasisPx(@Px int flexBasis) {
    mPrivateFlags |= PFLAG_FLEX_BASIS_IS_SET;
    mYogaNode.setFlexBasis(flexBasis);
    return this;
  }

  // Used by stetho to re-set auto value
  InternalNode flexBasisAuto() {
    mYogaNode.setFlexBasisAuto();
    return this;
  }

  InternalNode flexBasisPercent(float percent) {
    mPrivateFlags |= PFLAG_FLEX_BASIS_IS_SET;
    mYogaNode.setFlexBasisPercent(percent);
    return this;
  }

  InternalNode importantForAccessibility(int importantForAccessibility) {
    mPrivateFlags |= PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET;
    mImportantForAccessibility = importantForAccessibility;
    return this;
  }

  InternalNode duplicateParentState(boolean duplicateParentState) {
    mPrivateFlags |= PFLAG_DUPLICATE_PARENT_STATE_IS_SET;
    mDuplicateParentState = duplicateParentState;
    return this;
  }

  InternalNode marginPx(YogaEdge edge, @Px int margin) {
    mPrivateFlags |= PFLAG_MARGIN_IS_SET;
    mYogaNode.setMargin(edge, margin);
    return this;
  }

  InternalNode marginPercent(YogaEdge edge, float percent) {
    mPrivateFlags |= PFLAG_MARGIN_IS_SET;
    mYogaNode.setMarginPercent(edge, percent);
    return this;
  }

  InternalNode marginAuto(YogaEdge edge) {
    mPrivateFlags |= PFLAG_MARGIN_IS_SET;
    mYogaNode.setMarginAuto(edge);
    return this;
  }

  @ReturnsOwnership
  private Edges getNestedTreePadding() {
    if (mNestedTreePadding == null) {
      mNestedTreePadding = ComponentsPools.acquireEdges();
    }
    return mNestedTreePadding;
  }

  InternalNode paddingPx(YogaEdge edge, @Px int padding) {
    mPrivateFlags |= PFLAG_PADDING_IS_SET;

    if (mIsNestedTreeHolder) {
      getNestedTreePadding().set(edge, padding);
      setIsPaddingPercent(edge, false);
    } else {
      mYogaNode.setPadding(edge, padding);
    }

    return this;
  }

  InternalNode paddingPercent(YogaEdge edge, float percent) {
    mPrivateFlags |= PFLAG_PADDING_IS_SET;

    if (mIsNestedTreeHolder) {
      getNestedTreePadding().set(edge, percent);
      setIsPaddingPercent(edge, true);
    } else {
      mYogaNode.setPaddingPercent(edge, percent);
    }

    return this;
  }

  InternalNode border(Border border) {
    mPrivateFlags |= PFLAG_BORDER_IS_SET;
    for (int i = 0, length = border.mEdgeWidths.length; i < length; ++i) {
      setBorderWidth(Border.edgeFromIndex(i), border.mEdgeWidths[i]);
    }
    System.arraycopy(border.mEdgeColors, 0, mBorderColors, 0, mBorderColors.length);
    System.arraycopy(border.mRadius, 0, mBorderRadius, 0, mBorderRadius.length);
    mBorderPathEffect = border.mPathEffect;
    return this;
  }

  void setBorderWidth(YogaEdge edge, @Px int borderWidth) {
    if (mIsNestedTreeHolder) {
      if (mNestedTreeBorderWidth == null) {
        mNestedTreeBorderWidth = ComponentsPools.acquireEdges();
      }

      mNestedTreeBorderWidth.set(edge, borderWidth);
    } else {
      mYogaNode.setBorder(edge, borderWidth);
    }
  }

  int getLayoutBorder(YogaEdge edge) {
    return FastMath.round(mYogaNode.getLayoutBorder(edge));
  }

  InternalNode stateListAnimator(StateListAnimator stateListAnimator) {
    mPrivateFlags |= PFLAG_STATE_LIST_ANIMATOR_SET;
    mStateListAnimator = stateListAnimator;
    wrapInView();
    return this;
  }

  @Nullable
  StateListAnimator getStateListAnimator() {
    return mStateListAnimator;
  }

  InternalNode stateListAnimatorRes(@DrawableRes int resId) {
    mPrivateFlags |= PFLAG_STATE_LIST_ANIMATOR_RES_SET;
    mStateListAnimatorRes = resId;
    wrapInView();
    return this;
  }

  boolean hasStateListAnimatorResSet() {
    return (mPrivateFlags & PFLAG_STATE_LIST_ANIMATOR_RES_SET) != 0;
  }

  @DrawableRes
  int getStateListAnimatorRes() {
    return mStateListAnimatorRes;
  }

  InternalNode positionPx(YogaEdge edge, @Px int position) {
    mPrivateFlags |= PFLAG_POSITION_IS_SET;
    mYogaNode.setPosition(edge, position);
    return this;
  }

  InternalNode positionPercent(YogaEdge edge, float percent) {
    mPrivateFlags |= PFLAG_POSITION_IS_SET;
    mYogaNode.setPositionPercent(edge, percent);
    return this;
  }

  InternalNode widthPx(@Px int width) {
    mPrivateFlags |= PFLAG_WIDTH_IS_SET;
    mYogaNode.setWidth(width);
    return this;
  }

  // Used by stetho to re-set auto value
  InternalNode widthAuto() {
    mYogaNode.setWidthAuto();
    return this;
  }

  InternalNode widthPercent(float percent) {
    mPrivateFlags |= PFLAG_WIDTH_IS_SET;
    mYogaNode.setWidthPercent(percent);
    return this;
  }

  InternalNode minWidthPx(@Px int minWidth) {
    mPrivateFlags |= PFLAG_MIN_WIDTH_IS_SET;
    mYogaNode.setMinWidth(minWidth);
    return this;
  }

  InternalNode minWidthPercent(float percent) {
    mPrivateFlags |= PFLAG_MIN_WIDTH_IS_SET;
    mYogaNode.setMinWidthPercent(percent);
    return this;
  }

  InternalNode maxWidthPx(@Px int maxWidth) {
    mPrivateFlags |= PFLAG_MAX_WIDTH_IS_SET;
    mYogaNode.setMaxWidth(maxWidth);
    return this;
  }

  InternalNode maxWidthPercent(float percent) {
    mPrivateFlags |= PFLAG_MAX_WIDTH_IS_SET;
    mYogaNode.setMaxWidthPercent(percent);
    return this;
  }

  InternalNode heightPx(@Px int height) {
    mPrivateFlags |= PFLAG_HEIGHT_IS_SET;
    mYogaNode.setHeight(height);
    return this;
  }

  // Used by stetho to re-set auto value
  InternalNode heightAuto() {
    mYogaNode.setHeightAuto();
    return this;
  }

  InternalNode heightPercent(float percent) {
    mPrivateFlags |= PFLAG_HEIGHT_IS_SET;
    mYogaNode.setHeightPercent(percent);
    return this;
  }

  InternalNode minHeightPx(@Px int minHeight) {
    mPrivateFlags |= PFLAG_MIN_HEIGHT_IS_SET;
    mYogaNode.setMinHeight(minHeight);
    return this;
  }

  InternalNode minHeightPercent(float percent) {
    mPrivateFlags |= PFLAG_MIN_HEIGHT_IS_SET;
    mYogaNode.setMinHeightPercent(percent);
    return this;
  }

  InternalNode maxHeightPx(@Px int maxHeight) {
    mPrivateFlags |= PFLAG_MAX_HEIGHT_IS_SET;
    mYogaNode.setMaxHeight(maxHeight);
    return this;
  }

  InternalNode maxHeightPercent(float percent) {
    mPrivateFlags |= PFLAG_MAX_HEIGHT_IS_SET;
    mYogaNode.setMaxHeightPercent(percent);
    return this;
  }

  InternalNode aspectRatio(float aspectRatio) {
    mPrivateFlags |= PFLAG_ASPECT_RATIO_IS_SET;
    mYogaNode.setAspectRatio(aspectRatio);
    return this;
  }

  private boolean shouldApplyTouchExpansion() {
    return mTouchExpansion != null && mNodeInfo != null && mNodeInfo.hasTouchEventHandlers();
  }

  boolean hasTouchExpansion() {
    return ((mPrivateFlags & PFLAG_TOUCH_EXPANSION_IS_SET) != 0L);
  }

  Edges getTouchExpansion() {
    return mTouchExpansion;
  }

  int getTouchExpansionLeft() {
    if (!shouldApplyTouchExpansion()) {
      return 0;
    }

    if (YogaConstants.isUndefined(mResolvedTouchExpansionLeft)) {
      mResolvedTouchExpansionLeft = resolveHorizontalEdges(mTouchExpansion, YogaEdge.LEFT);
    }

    return FastMath.round(mResolvedTouchExpansionLeft);
  }

  int getTouchExpansionTop() {
    if (!shouldApplyTouchExpansion()) {
      return 0;
    }

    return FastMath.round(mTouchExpansion.get(YogaEdge.TOP));
  }

  int getTouchExpansionRight() {
    if (!shouldApplyTouchExpansion()) {
      return 0;
    }

    if (YogaConstants.isUndefined(mResolvedTouchExpansionRight)) {
      mResolvedTouchExpansionRight = resolveHorizontalEdges(mTouchExpansion, YogaEdge.RIGHT);
    }

    return FastMath.round(mResolvedTouchExpansionRight);
  }

  int getTouchExpansionBottom() {
    if (!shouldApplyTouchExpansion()) {
      return 0;
    }

    return FastMath.round(mTouchExpansion.get(YogaEdge.BOTTOM));
  }

  InternalNode touchExpansionPx(YogaEdge edge, @Px int touchExpansion) {
    if (mTouchExpansion == null) {
      mTouchExpansion = ComponentsPools.acquireEdges();
    }

    mPrivateFlags |= PFLAG_TOUCH_EXPANSION_IS_SET;
    mTouchExpansion.set(edge, touchExpansion);

    return this;
  }

  InternalNode child(Component child) {
    if (child != null) {
      return child(Layout.create(mComponentContext, child));
    }

    return this;
  }

  InternalNode child(Component.Builder<?> child) {
    if (child != null) {
      child(child.build());
    }
    return this;
  }

  InternalNode child(InternalNode child) {
    if (child != null && child != NULL_LAYOUT) {
      addChildAt(child, mYogaNode.getChildCount());
    }

    return this;
  }

  /**
   * @deprecated use {@link #background(ComparableDrawable)} more efficient diffing of drawables.
   */
  @Deprecated
  InternalNode background(@Nullable Reference<? extends Drawable> background) {
    mPrivateFlags |= PFLAG_BACKGROUND_IS_SET;
    mBackground = background;
    setPaddingFromDrawableReference(background);
    return this;
  }

  InternalNode background(@Nullable ComparableDrawable background) {
    return background(background != null ? DrawableReference.create(background) : null);
  }

  /**
   * @deprecated use {@link #background(ComparableDrawable)} more efficient diffing of drawables.
   */
  @Deprecated
  InternalNode background(@Nullable Drawable background) {
    if (background instanceof ComparableDrawable) {
      return background((ComparableDrawable) background);
    }
    return background(background != null ? DefaultComparableDrawable.create(background) : null);
  }

  InternalNode backgroundRes(@DrawableRes int resId) {
    if (resId == 0) {
      return background((ComparableDrawable) null);
    }

    return background(ComparableResDrawable.create(mComponentContext.getAndroidContext(), resId));
  }

  InternalNode backgroundColor(@ColorInt int backgroundColor) {
    return background(ComparableColorDrawable.create(backgroundColor));
  }

  /**
   * @deprecated use {@link #foreground(ComparableDrawable)} more efficient diffing of drawables.
   */
  @Deprecated
  InternalNode foreground(@Nullable Drawable foreground) {
    return foreground(foreground != null ? DefaultComparableDrawable.create(foreground) : null);
  }

  InternalNode foreground(@Nullable ComparableDrawable foreground) {
    mPrivateFlags |= PFLAG_FOREGROUND_IS_SET;
    mForeground = foreground;
    return this;
  }

  InternalNode foregroundRes(@DrawableRes int resId) {
    if (resId == 0) {
      return foreground(null);
    }

    return foreground(ComparableResDrawable.create(mComponentContext.getAndroidContext(), resId));
  }

  InternalNode foregroundColor(@ColorInt int foregroundColor) {
    return foreground(ComparableColorDrawable.create(foregroundColor));
  }

  InternalNode wrapInView() {
    mForceViewWrapping = true;
    return this;
  }

  boolean isForceViewWrapping() {
    return mForceViewWrapping;
  }

  EventHandler<ClickEvent> getClickHandler() {
    return getOrCreateNodeInfo().getClickHandler();
  }

  InternalNode clickHandler(EventHandler<ClickEvent> clickHandler) {
    getOrCreateNodeInfo().setClickHandler(clickHandler);
    return this;
  }

  InternalNode longClickHandler(EventHandler<LongClickEvent> longClickHandler) {
    getOrCreateNodeInfo().setLongClickHandler(longClickHandler);
    return this;
  }

  InternalNode focusChangeHandler(EventHandler<FocusChangedEvent> focusChangeHandler) {
    getOrCreateNodeInfo().setFocusChangeHandler(focusChangeHandler);
    return this;
  }

  InternalNode touchHandler(EventHandler<TouchEvent> touchHandler) {
    getOrCreateNodeInfo().setTouchHandler(touchHandler);
    return this;
  }

  InternalNode interceptTouchHandler(EventHandler interceptTouchHandler) {
    getOrCreateNodeInfo().setInterceptTouchHandler(interceptTouchHandler);
    return this;
  }

  InternalNode focusable(boolean isFocusable) {
    getOrCreateNodeInfo().setFocusable(isFocusable);
    return this;
  }

  InternalNode enabled(boolean isEnabled) {
    getOrCreateNodeInfo().setEnabled(isEnabled);
    return this;
  }

  InternalNode selected(boolean isSelected) {
    getOrCreateNodeInfo().setSelected(isSelected);
    return this;
  }

  InternalNode visibleHeightRatio(float visibleHeightRatio) {
    mVisibleHeightRatio = visibleHeightRatio;
    return this;
  }

  float getVisibleHeightRatio() {
    return mVisibleHeightRatio;
  }

  InternalNode visibleWidthRatio(float visibleWidthRatio) {
    mVisibleWidthRatio = visibleWidthRatio;
    return this;
  }

  float getVisibleWidthRatio() {
    return mVisibleWidthRatio;
  }

  InternalNode visibleHandler(@Nullable EventHandler<VisibleEvent> visibleHandler) {
    mPrivateFlags |= PFLAG_VISIBLE_HANDLER_IS_SET;
    mVisibleHandler = addVisibilityHandler(mVisibleHandler, visibleHandler);
    return this;
  }

  @Nullable
  EventHandler<VisibleEvent> getVisibleHandler() {
    return mVisibleHandler;
  }

  InternalNode focusedHandler(@Nullable EventHandler<FocusedVisibleEvent> focusedHandler) {
    mPrivateFlags |= PFLAG_FOCUSED_HANDLER_IS_SET;
    mFocusedHandler = addVisibilityHandler(mFocusedHandler, focusedHandler);
    return this;
  }

  @Nullable
  EventHandler<FocusedVisibleEvent> getFocusedHandler() {
    return mFocusedHandler;
  }

  InternalNode unfocusedHandler(@Nullable EventHandler<UnfocusedVisibleEvent> unfocusedHandler) {
    mPrivateFlags |= PFLAG_UNFOCUSED_HANDLER_IS_SET;
    mUnfocusedHandler = addVisibilityHandler(mUnfocusedHandler, unfocusedHandler);
    return this;
  }

  @Nullable
  EventHandler<UnfocusedVisibleEvent> getUnfocusedHandler() {
    return mUnfocusedHandler;
  }

  InternalNode fullImpressionHandler(
      @Nullable EventHandler<FullImpressionVisibleEvent> fullImpressionHandler) {
    mPrivateFlags |= PFLAG_FULL_IMPRESSION_HANDLER_IS_SET;
    mFullImpressionHandler = addVisibilityHandler(mFullImpressionHandler, fullImpressionHandler);
    return this;
  }

  @Nullable
  EventHandler<FullImpressionVisibleEvent> getFullImpressionHandler() {
    return mFullImpressionHandler;
  }

  InternalNode invisibleHandler(@Nullable EventHandler<InvisibleEvent> invisibleHandler) {
    mPrivateFlags |= PFLAG_INVISIBLE_HANDLER_IS_SET;
    mInvisibleHandler = addVisibilityHandler(mInvisibleHandler, invisibleHandler);
    return this;
  }

  @Nullable
  EventHandler<InvisibleEvent> getInvisibleHandler() {
    return mInvisibleHandler;
  }

  InternalNode visibilityChangedHandler(
      @Nullable EventHandler<VisibilityChangedEvent> visibilityChangedHandler) {
    mPrivateFlags |= PFLAG_VISIBLE_RECT_CHANGED_HANDLER_IS_SET;
    mVisibilityChangedHandler =
        addVisibilityHandler(mVisibilityChangedHandler, visibilityChangedHandler);
    return this;
  }

  @Nullable
  EventHandler<VisibilityChangedEvent> getVisibilityChangedHandler() {
    return mVisibilityChangedHandler;
  }

  @Nullable
  private static <T> EventHandler<T> addVisibilityHandler(
      @Nullable EventHandler<T> existingEventHandler, @Nullable EventHandler<T> newEventHandler) {
    if (existingEventHandler == null) {
      return newEventHandler;
    }
    if (newEventHandler == null) {
      return existingEventHandler;
    }
    return new DelegatingEventHandler<>(existingEventHandler, newEventHandler);
  }

  InternalNode contentDescription(CharSequence contentDescription) {
    getOrCreateNodeInfo().setContentDescription(contentDescription);
    return this;
  }

  InternalNode viewTag(Object viewTag) {
    getOrCreateNodeInfo().setViewTag(viewTag);
    return this;
  }

  InternalNode viewTags(SparseArray<Object> viewTags) {
    getOrCreateNodeInfo().setViewTags(viewTags);
    return this;
  }

  InternalNode shadowElevationPx(float shadowElevation) {
    getOrCreateNodeInfo().setShadowElevation(shadowElevation);
    return this;
  }

  InternalNode outlineProvider(ViewOutlineProvider outlineProvider) {
    getOrCreateNodeInfo().setOutlineProvider(outlineProvider);
    return this;
  }

  InternalNode clipToOutline(boolean clipToOutline) {
    getOrCreateNodeInfo().setClipToOutline(clipToOutline);
    return this;
  }

  InternalNode clipChildren(boolean clipChildren) {
    getOrCreateNodeInfo().setClipChildren(clipChildren);
    return this;
  }

  InternalNode testKey(String testKey) {
    mTestKey = testKey;
    return this;
  }

  InternalNode scale(float scale) {
    wrapInView();

    getOrCreateNodeInfo().setScale(scale);
    return this;
  }

  InternalNode alpha(float alpha) {
    wrapInView();

    getOrCreateNodeInfo().setAlpha(alpha);
    return this;
  }

  InternalNode rotation(float rotation) {
    wrapInView();

    getOrCreateNodeInfo().setRotation(rotation);
    return this;
  }

  InternalNode accessibilityRole(@AccessibilityRole.AccessibilityRoleType String role) {
    getOrCreateNodeInfo().setAccessibilityRole(role);
    return this;
  }

  InternalNode dispatchPopulateAccessibilityEventHandler(
      EventHandler<DispatchPopulateAccessibilityEventEvent>
          dispatchPopulateAccessibilityEventHandler) {
    getOrCreateNodeInfo().setDispatchPopulateAccessibilityEventHandler(
        dispatchPopulateAccessibilityEventHandler);
    return this;
  }

  InternalNode onInitializeAccessibilityEventHandler(
      EventHandler<OnInitializeAccessibilityEventEvent> onInitializeAccessibilityEventHandler) {
    getOrCreateNodeInfo().setOnInitializeAccessibilityEventHandler(
        onInitializeAccessibilityEventHandler);
    return this;
  }

  InternalNode onInitializeAccessibilityNodeInfoHandler(
      EventHandler<OnInitializeAccessibilityNodeInfoEvent>
          onInitializeAccessibilityNodeInfoHandler) {
    getOrCreateNodeInfo().setOnInitializeAccessibilityNodeInfoHandler(
        onInitializeAccessibilityNodeInfoHandler);
    return this;
  }

  InternalNode onPopulateAccessibilityEventHandler(
      EventHandler<OnPopulateAccessibilityEventEvent> onPopulateAccessibilityEventHandler) {
    getOrCreateNodeInfo().setOnPopulateAccessibilityEventHandler(
        onPopulateAccessibilityEventHandler);
    return this;
  }

  InternalNode onRequestSendAccessibilityEventHandler(
      EventHandler<OnRequestSendAccessibilityEventEvent> onRequestSendAccessibilityEventHandler) {
    getOrCreateNodeInfo().setOnRequestSendAccessibilityEventHandler(
        onRequestSendAccessibilityEventHandler);
    return this;
  }

  InternalNode performAccessibilityActionHandler(
      EventHandler<PerformAccessibilityActionEvent> performAccessibilityActionHandler) {
    getOrCreateNodeInfo().setPerformAccessibilityActionHandler(performAccessibilityActionHandler);
    return this;
  }

  InternalNode sendAccessibilityEventHandler(
      EventHandler<SendAccessibilityEventEvent> sendAccessibilityEventHandler) {
    getOrCreateNodeInfo().setSendAccessibilityEventHandler(sendAccessibilityEventHandler);
    return this;
  }

  InternalNode sendAccessibilityEventUncheckedHandler(
      EventHandler<SendAccessibilityEventUncheckedEvent> sendAccessibilityEventUncheckedHandler) {
    getOrCreateNodeInfo().setSendAccessibilityEventUncheckedHandler(
        sendAccessibilityEventUncheckedHandler);
    return this;
  }

  InternalNode transitionKey(String key) {
    if (SDK_INT >= ICE_CREAM_SANDWICH && !TextUtils.isEmpty(key)) {
      mPrivateFlags |= PFLAG_TRANSITION_KEY_IS_SET;
      mTransitionKey = key;
    }

    return this;
  }

  String getTransitionKey() {
    return mTransitionKey;
  }

  boolean hasTransitionKey() {
    return !TextUtils.isEmpty(mTransitionKey);
  }

  InternalNode transitionKeyType(Transition.TransitionKeyType type) {
    mPrivateFlags |= PFLAG_TRANSITION_KEY_TYPE_IS_SET;
    mTransitionKeyType = type;
    return this;
  }

  @Nullable
  Transition.TransitionKeyType getTransitionKeyType() {
    return mTransitionKeyType;
  }

  @Nullable
  ArrayList<Transition> getTransitions() {
    return mTransitions;
  }

  @Nullable
  ArrayList<Component> getComponentsNeedingPreviousRenderData() {
    return mComponentsNeedingPreviousRenderData;
  }

  @Nullable
  ArrayList<WorkingRangeContainer.Registration> getWorkingRangeRegistrations() {
    return mWorkingRangeRegistrations;
  }

  /**
   * A unique identifier which may be set for retrieving a component and its bounds when testing.
   */
  String getTestKey() {
    return mTestKey;
  }

  void setMeasureFunction(YogaMeasureFunction measureFunction) {
    mYogaNode.setMeasureFunction(measureFunction);
  }

  void setBaselineFunction(YogaBaselineFunction baselineFunction) {
    mYogaNode.setBaselineFunction(baselineFunction);
  }

  boolean hasNewLayout() {
    return mYogaNode.hasNewLayout();
  }

  void markLayoutSeen() {
    mYogaNode.markLayoutSeen();
  }

  float getStyleWidth() {
    return mYogaNode.getWidth().value;
  }

  float getMinWidth() {
    return mYogaNode.getMinWidth().value;
  }

  float getMaxWidth() {
    return mYogaNode.getMaxWidth().value;
  }

  float getStyleHeight() {
    return mYogaNode.getHeight().value;
  }

  float getMinHeight() {
    return mYogaNode.getMinHeight().value;
  }

  float getMaxHeight() {
    return mYogaNode.getMaxHeight().value;
  }

  void calculateLayout(float width, float height) {
    applyOverridesRecursive(this);
    mYogaNode.calculateLayout(width, height);
  }

  void calculateLayout() {
    calculateLayout(YogaConstants.UNDEFINED, YogaConstants.UNDEFINED);
  }

  private void applyOverridesRecursive(InternalNode node) {
    if (ComponentsConfiguration.isDebugModeEnabled) {
      DebugComponent.applyOverrides(mComponentContext, node);

      for (int i = 0, count = node.getChildCount(); i < count; i++) {
        applyOverridesRecursive(node.getChildAt(i));
      }

      if (node.hasNestedTree()) {
        applyOverridesRecursive(node.getNestedTree());
      }
    }
  }

  int getChildCount() {
    return mYogaNode.getChildCount();
  }

  void registerDebugComponent(DebugComponent debugComponent) {
    if (mDebugComponents == null) {
      mDebugComponents = new HashSet<>();
    }
    mDebugComponents.add(debugComponent);
  }

  com.facebook.yoga.YogaDirection getStyleDirection() {
    return mYogaNode.getStyleDirection();
  }

  @Nullable
  InternalNode getChildAt(int index) {
    if (mYogaNode.getChildAt(index) == null) {
      return null;
    }
    return (InternalNode) mYogaNode.getChildAt(index).getData();
  }

  int getChildIndex(InternalNode child) {
    for (int i = 0, count = mYogaNode.getChildCount(); i < count; i++) {
      if (mYogaNode.getChildAt(i) == child.mYogaNode) {
        return i;
      }
    }
    return -1;
  }

  @Nullable
  InternalNode getParent() {
    if (mYogaNode == null || mYogaNode.getOwner() == null) {
      return null;
    }
    return (InternalNode) mYogaNode.getOwner().getData();
  }

  void addChildAt(InternalNode child, int index) {
    mYogaNode.addChildAt(child.mYogaNode, index);
  }

  InternalNode removeChildAt(int index) {
    return (InternalNode) mYogaNode.removeChildAt(index).getData();
  }

  private float resolveHorizontalEdges(Edges spacing, YogaEdge edge) {
    final boolean isRtl =
        (mYogaNode.getLayoutDirection() == YogaDirection.RTL);

    final YogaEdge resolvedEdge;
    switch (edge) {
      case LEFT:
        resolvedEdge = (isRtl ? YogaEdge.END : YogaEdge.START);
        break;

      case RIGHT:
        resolvedEdge = (isRtl ? YogaEdge.START : YogaEdge.END);
        break;

      default:
        throw new IllegalArgumentException("Not an horizontal padding edge: " + edge);
    }

    float result = spacing.getRaw(resolvedEdge);
    if (YogaConstants.isUndefined(result)) {
      result = spacing.get(edge);
    }

    return result;
  }

  ComponentContext getContext() {
    return mComponentContext;
  }

  /**
   * Return the list of components contributing to this InternalNode. We have no need for this in
   * production but it is useful information to have while debugging. Therefor this list will only
   * contain the root component if running in production mode.
   */
  List<Component> getComponents() {
    return mComponents;
  }

  @Nullable
  Component getRootComponent() {
    return mComponents.size() == 0 ? null : mComponents.get(0);
  }

  void setRootComponent(Component component) {
    mComponents.clear();
    mComponents.add(component);
  }

  int[] getBorderColors() {
    return mBorderColors;
  }

  float[] getBorderRadius() {
    return mBorderRadius;
  }

  @Nullable
  PathEffect getBorderPathEffect() {
    return mBorderPathEffect;
  }

  protected boolean hasBorderColor() {
    for (int color : mBorderColors) {
      if (color != Color.TRANSPARENT) {
        return true;
      }
    }

    return false;
  }

  boolean shouldDrawBorders() {
    return hasBorderColor()
        && (mYogaNode.getLayoutBorder(LEFT) != 0
            || mYogaNode.getLayoutBorder(TOP) != 0
            || mYogaNode.getLayoutBorder(RIGHT) != 0
            || mYogaNode.getLayoutBorder(BOTTOM) != 0);
  }

  void appendComponent(Component component) {
    mComponents.add(component);
  }

  void addTransition(Transition transition) {
    if (mTransitions == null) {
      mTransitions = new ArrayList<>(1);
    }
    mTransitions.add(transition);
  }

  void addComponentNeedingPreviousRenderData(Component component) {
    if (mComponentsNeedingPreviousRenderData == null) {
      mComponentsNeedingPreviousRenderData = new ArrayList<>(1);
    }
    mComponentsNeedingPreviousRenderData.add(component);
  }

  void addWorkingRanges(List<WorkingRangeContainer.Registration> registrations) {
    if (mWorkingRangeRegistrations == null) {
      mWorkingRangeRegistrations = new ArrayList<>(registrations.size());
    }
    mWorkingRangeRegistrations.addAll(registrations);
  }

  boolean hasNestedTree() {
    return mNestedTree != null;
  }

  @Nullable
  InternalNode getNestedTree() {
    return mNestedTree;
  }

  InternalNode getNestedTreeHolder() {
    return mNestedTreeHolder;
  }

  /**
   * Set the nested tree before measuring it in order to transfer over important information such as
   * layout direction needed during measurement.
   */
  void setNestedTree(InternalNode nestedTree) {
    nestedTree.mNestedTreeHolder = this;
    mNestedTree = nestedTree;
  }

  NodeInfo getNodeInfo() {
    return mNodeInfo;
  }

  void copyInto(InternalNode node) {
    if (mNodeInfo != null) {
      if (node.mNodeInfo == null) {
        node.mNodeInfo = mNodeInfo.acquireRef();
      } else {
        node.mNodeInfo.updateWith(mNodeInfo);
      }
    }
    if ((node.mPrivateFlags & PFLAG_LAYOUT_DIRECTION_IS_SET) == 0L
        || node.getResolvedLayoutDirection() == YogaDirection.INHERIT) {
      node.layoutDirection(getResolvedLayoutDirection());
    }
    if ((node.mPrivateFlags & PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET) == 0L
        || node.mImportantForAccessibility == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
      node.mImportantForAccessibility = mImportantForAccessibility;
    }
    if ((mPrivateFlags & PFLAG_DUPLICATE_PARENT_STATE_IS_SET) != 0L) {
      node.mDuplicateParentState = mDuplicateParentState;
    }
    if ((mPrivateFlags & PFLAG_BACKGROUND_IS_SET) != 0L) {
      node.mBackground = mBackground;
    }
    if ((mPrivateFlags & PFLAG_FOREGROUND_IS_SET) != 0L) {
      node.mForeground = mForeground;
    }
    if (mForceViewWrapping) {
      node.mForceViewWrapping = true;
    }
    if ((mPrivateFlags & PFLAG_VISIBLE_HANDLER_IS_SET) != 0L) {
      node.mVisibleHandler = mVisibleHandler;
    }
    if ((mPrivateFlags & PFLAG_FOCUSED_HANDLER_IS_SET) != 0L) {
      node.mFocusedHandler = mFocusedHandler;
    }
    if ((mPrivateFlags & PFLAG_FULL_IMPRESSION_HANDLER_IS_SET) != 0L) {
      node.mFullImpressionHandler = mFullImpressionHandler;
    }
    if ((mPrivateFlags & PFLAG_INVISIBLE_HANDLER_IS_SET) != 0L) {
      node.mInvisibleHandler = mInvisibleHandler;
    }
    if ((mPrivateFlags & PFLAG_UNFOCUSED_HANDLER_IS_SET) != 0L) {
      node.mUnfocusedHandler = mUnfocusedHandler;
    }
    if ((mPrivateFlags & PFLAG_VISIBLE_RECT_CHANGED_HANDLER_IS_SET) != 0L) {
      node.mVisibilityChangedHandler = mVisibilityChangedHandler;
    }
    if (mTestKey != null) {
      node.mTestKey = mTestKey;
    }
    if ((mPrivateFlags & PFLAG_PADDING_IS_SET) != 0L) {
      if (mNestedTreePadding == null) {
        throw new IllegalStateException("copyInto() must be used when resolving a nestedTree. " +
            "If padding was set on the holder node, we must have a mNestedTreePadding instance");
      }

      final YogaNode yogaNode = node.mYogaNode;

      node.mPrivateFlags |= PFLAG_PADDING_IS_SET;
      if (isPaddingPercent(LEFT)) {
        yogaNode.setPaddingPercent(LEFT, mNestedTreePadding.getRaw(YogaEdge.LEFT));
      } else {
        yogaNode.setPadding(LEFT, mNestedTreePadding.getRaw(YogaEdge.LEFT));
      }

      if (isPaddingPercent(TOP)) {
        yogaNode.setPaddingPercent(TOP, mNestedTreePadding.getRaw(YogaEdge.TOP));
      } else {
        yogaNode.setPadding(TOP, mNestedTreePadding.getRaw(YogaEdge.TOP));
      }

      if (isPaddingPercent(RIGHT)) {
        yogaNode.setPaddingPercent(RIGHT, mNestedTreePadding.getRaw(YogaEdge.RIGHT));
      } else {
        yogaNode.setPadding(RIGHT, mNestedTreePadding.getRaw(YogaEdge.RIGHT));
      }

      if (isPaddingPercent(BOTTOM)) {
        yogaNode.setPaddingPercent(BOTTOM, mNestedTreePadding.getRaw(YogaEdge.BOTTOM));
      } else {
        yogaNode.setPadding(BOTTOM, mNestedTreePadding.getRaw(YogaEdge.BOTTOM));
      }

      if (isPaddingPercent(VERTICAL)) {
        yogaNode.setPaddingPercent(VERTICAL, mNestedTreePadding.getRaw(YogaEdge.VERTICAL));
      } else {
        yogaNode.setPadding(VERTICAL, mNestedTreePadding.getRaw(YogaEdge.VERTICAL));
      }

      if (isPaddingPercent(HORIZONTAL)) {
        yogaNode.setPaddingPercent(HORIZONTAL, mNestedTreePadding.getRaw(YogaEdge.HORIZONTAL));
      } else {
        yogaNode.setPadding(HORIZONTAL, mNestedTreePadding.getRaw(YogaEdge.HORIZONTAL));
      }

      if (isPaddingPercent(START)) {
        yogaNode.setPaddingPercent(START, mNestedTreePadding.getRaw(YogaEdge.START));
      } else {
        yogaNode.setPadding(START, mNestedTreePadding.getRaw(YogaEdge.START));
      }

      if (isPaddingPercent(END)) {
        yogaNode.setPaddingPercent(END, mNestedTreePadding.getRaw(YogaEdge.END));
      } else {
        yogaNode.setPadding(END, mNestedTreePadding.getRaw(YogaEdge.END));
      }

      if (isPaddingPercent(ALL)) {
        yogaNode.setPaddingPercent(ALL, mNestedTreePadding.getRaw(YogaEdge.ALL));
      } else {
        yogaNode.setPadding(ALL, mNestedTreePadding.getRaw(YogaEdge.ALL));
      }
    }

    if ((mPrivateFlags & PFLAG_BORDER_IS_SET) != 0L) {
      if (mNestedTreeBorderWidth == null) {
        throw new IllegalStateException("copyInto() must be used when resolving a nestedTree. " +
            "If border width was set on the holder node, we must have a mNestedTreeBorderWidth " +
            "instance");
      }

      final YogaNode yogaNode = node.mYogaNode;

      node.mPrivateFlags |= PFLAG_BORDER_IS_SET;
      yogaNode.setBorder(LEFT, mNestedTreeBorderWidth.getRaw(YogaEdge.LEFT));
      yogaNode.setBorder(TOP, mNestedTreeBorderWidth.getRaw(YogaEdge.TOP));
      yogaNode.setBorder(RIGHT, mNestedTreeBorderWidth.getRaw(YogaEdge.RIGHT));
      yogaNode.setBorder(BOTTOM, mNestedTreeBorderWidth.getRaw(YogaEdge.BOTTOM));
      yogaNode.setBorder(VERTICAL, mNestedTreeBorderWidth.getRaw(YogaEdge.VERTICAL));
      yogaNode.setBorder(HORIZONTAL, mNestedTreeBorderWidth.getRaw(YogaEdge.HORIZONTAL));
      yogaNode.setBorder(START, mNestedTreeBorderWidth.getRaw(YogaEdge.START));
      yogaNode.setBorder(END, mNestedTreeBorderWidth.getRaw(YogaEdge.END));
      yogaNode.setBorder(ALL, mNestedTreeBorderWidth.getRaw(YogaEdge.ALL));
      System.arraycopy(mBorderColors, 0, node.mBorderColors, 0, mBorderColors.length);
      System.arraycopy(mBorderRadius, 0, node.mBorderRadius, 0, mBorderRadius.length);
    }
    if ((mPrivateFlags & PFLAG_TRANSITION_KEY_IS_SET) != 0L) {
      node.mTransitionKey = mTransitionKey;
    }
    if ((mPrivateFlags & PFLAG_TRANSITION_KEY_TYPE_IS_SET) != 0L) {
      node.mTransitionKeyType = mTransitionKeyType;
    }
    if (mVisibleHeightRatio != 0) {
      node.mVisibleHeightRatio = mVisibleHeightRatio;
    }
    if (mVisibleWidthRatio != 0) {
      node.mVisibleWidthRatio = mVisibleWidthRatio;
    }
    if ((mPrivateFlags & PFLAG_STATE_LIST_ANIMATOR_SET) != 0L) {
      node.mStateListAnimator = mStateListAnimator;
    }
    if ((mPrivateFlags & PFLAG_STATE_LIST_ANIMATOR_RES_SET) != 0L) {
      node.mStateListAnimatorRes = mStateListAnimatorRes;
    }
  }

  void setStyleWidthFromSpec(int widthSpec) {
    switch (SizeSpec.getMode(widthSpec)) {
      case SizeSpec.UNSPECIFIED:
        mYogaNode.setWidth(YogaConstants.UNDEFINED);
        break;
      case SizeSpec.AT_MOST:
        mYogaNode.setMaxWidth(SizeSpec.getSize(widthSpec));
        break;
      case SizeSpec.EXACTLY:
        mYogaNode.setWidth(SizeSpec.getSize(widthSpec));
        break;
    }
  }

  void setStyleHeightFromSpec(int heightSpec) {
    switch (SizeSpec.getMode(heightSpec)) {
      case SizeSpec.UNSPECIFIED:
        mYogaNode.setHeight(YogaConstants.UNDEFINED);
        break;
      case SizeSpec.AT_MOST:
        mYogaNode.setMaxHeight(SizeSpec.getSize(heightSpec));
        break;
      case SizeSpec.EXACTLY:
        mYogaNode.setHeight(SizeSpec.getSize(heightSpec));
        break;
    }
  }

  int getImportantForAccessibility() {
    return mImportantForAccessibility;
  }

  boolean isDuplicateParentStateEnabled() {
    return mDuplicateParentState;
  }

  void applyAttributes(TypedArray a) {
    for (int i = 0, size = a.getIndexCount(); i < size; i++) {
      final int attr = a.getIndex(i);

      if (attr == R.styleable.ComponentLayout_android_layout_width) {
        int width = a.getLayoutDimension(attr, -1);
        // We don't support WRAP_CONTENT or MATCH_PARENT so no-op for them
        if (width >= 0) {
          widthPx(width);
        }
      } else if (attr == R.styleable.ComponentLayout_android_layout_height) {
        int height = a.getLayoutDimension(attr, -1);
        // We don't support WRAP_CONTENT or MATCH_PARENT so no-op for them
        if (height >= 0) {
          heightPx(height);
        }
      } else if (attr == R.styleable.ComponentLayout_android_minHeight) {
        minHeightPx(a.getDimensionPixelSize(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_minWidth) {
        minWidthPx(a.getDimensionPixelSize(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_paddingLeft) {
        paddingPx(LEFT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_paddingTop) {
        paddingPx(TOP, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_paddingRight) {
        paddingPx(RIGHT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_paddingBottom) {
        paddingPx(BOTTOM, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_paddingStart && SUPPORTS_RTL) {
        paddingPx(START, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_paddingEnd && SUPPORTS_RTL) {
        paddingPx(END, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_padding) {
        paddingPx(ALL, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_layout_marginLeft) {
        marginPx(LEFT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_layout_marginTop) {
        marginPx(TOP, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_layout_marginRight) {
        marginPx(RIGHT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_layout_marginBottom) {
        marginPx(BOTTOM, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_layout_marginStart && SUPPORTS_RTL) {
        marginPx(START, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_layout_marginEnd && SUPPORTS_RTL) {
        marginPx(END, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_layout_margin) {
        marginPx(ALL, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_importantForAccessibility &&
          SDK_INT >= JELLY_BEAN) {
        importantForAccessibility(a.getInt(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_duplicateParentState) {
        duplicateParentState(a.getBoolean(attr, false));
      } else if (attr == R.styleable.ComponentLayout_android_background) {
        if (TypedArrayUtils.isColorAttribute(a, R.styleable.ComponentLayout_android_background)) {
          backgroundColor(a.getColor(attr, 0));
        } else {
          backgroundRes(a.getResourceId(attr, -1));
        }
      } else if (attr == R.styleable.ComponentLayout_android_foreground) {
        if (TypedArrayUtils.isColorAttribute(a, R.styleable.ComponentLayout_android_foreground)) {
          foregroundColor(a.getColor(attr, 0));
        } else {
          foregroundRes(a.getResourceId(attr, -1));
        }
      } else if (attr == R.styleable.ComponentLayout_android_contentDescription) {
        contentDescription(a.getString(attr));
      } else if (attr == R.styleable.ComponentLayout_flex_direction) {
        flexDirection(YogaFlexDirection.fromInt(a.getInteger(attr, 0)));
      } else if (attr == R.styleable.ComponentLayout_flex_wrap) {
        wrap(YogaWrap.fromInt(a.getInteger(attr, 0)));
      } else if (attr == R.styleable.ComponentLayout_flex_justifyContent) {
        justifyContent(YogaJustify.fromInt(a.getInteger(attr, 0)));
      } else if (attr == R.styleable.ComponentLayout_flex_alignItems) {
        alignItems(YogaAlign.fromInt(a.getInteger(attr, 0)));
      } else if (attr == R.styleable.ComponentLayout_flex_alignSelf) {
        alignSelf(YogaAlign.fromInt(a.getInteger(attr, 0)));
      } else if (attr == R.styleable.ComponentLayout_flex_positionType) {
        positionType(YogaPositionType.fromInt(a.getInteger(attr, 0)));
      } else if (attr == R.styleable.ComponentLayout_flex) {
        final float flex = a.getFloat(attr, -1);
        if (flex >= 0f) {
          flex(flex);
        }
      } else if (attr == R.styleable.ComponentLayout_flex_left) {
        positionPx(LEFT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_flex_top) {
        positionPx(TOP, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_flex_right) {
        positionPx(RIGHT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_flex_bottom) {
        positionPx(BOTTOM, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_flex_layoutDirection) {
        final int layoutDirection = a.getInteger(attr, -1);
        layoutDirection(YogaDirection.fromInt(layoutDirection));
      }
    }
  }

  /**
   * Reset all attributes to default values and release the YogaNode if present. Intended to
   * facilitate recycling.
   */
  void release() {
    if (ComponentsConfiguration.disablePools) {
      return;
    }

    if (mYogaNode != null) {
      if (mYogaNode.getOwner() != null || mYogaNode.getChildCount() > 0) {
        throw new IllegalStateException("You should not free an attached Internalnode");
      }

      ComponentsPools.release(mYogaNode);
      mYogaNode = null;
    }

    if (mDebugComponents != null) {
      mDebugComponents.clear();
    }

    mResolvedTouchExpansionLeft = YogaConstants.UNDEFINED;
    mResolvedTouchExpansionRight = YogaConstants.UNDEFINED;
    mResolvedX = YogaConstants.UNDEFINED;
    mResolvedY = YogaConstants.UNDEFINED;
    mResolvedWidth = YogaConstants.UNDEFINED;
    mResolvedHeight = YogaConstants.UNDEFINED;

    mComponentContext = null;
    mComponents.clear();
    mNestedTree = null;
    mNestedTreeHolder = null;

    if (mNodeInfo != null) {
      mNodeInfo.release();
      mNodeInfo = null;
    }
    mImportantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
    mDuplicateParentState = false;
    mBackground = null;
    mForeground = null;
    mForceViewWrapping = false;
    mVisibleHeightRatio = 0;
    mVisibleWidthRatio = 0;
    mVisibleHandler = null;
    mFocusedHandler = null;
    mUnfocusedHandler = null;
    mFullImpressionHandler = null;
    mInvisibleHandler = null;
    mPrivateFlags = 0L;
    mTransitionKey = null;
    mTransitionKeyType = null;
    Arrays.fill(mBorderColors, Color.TRANSPARENT);
    Arrays.fill(mBorderRadius, 0f);
    mIsPaddingPercent = null;

    if (mTouchExpansion != null) {
      ComponentsPools.release(mTouchExpansion);
      mTouchExpansion = null;
    }
    if (mNestedTreePadding != null) {
      ComponentsPools.release(mNestedTreePadding);
      mNestedTreePadding = null;
    }
    if (mNestedTreeBorderWidth != null) {
      ComponentsPools.release(mNestedTreeBorderWidth);
      mNestedTreeBorderWidth = null;
    }

    mLastWidthSpec = DiffNode.UNSPECIFIED;
    mLastHeightSpec = DiffNode.UNSPECIFIED;
    mLastMeasuredHeight = DiffNode.UNSPECIFIED;
    mLastMeasuredWidth = DiffNode.UNSPECIFIED;
    mDiffNode = null;
    mCachedMeasuresValid = false;
    mIsNestedTreeHolder = false;
    mTestKey = null;
    mPendingTreeProps = null;

    mTransitions = null;
    mComponentsNeedingPreviousRenderData = null;
    mWorkingRangeRegistrations = null;

    mStateListAnimator = null;
    mStateListAnimatorRes = 0;

    if (mExtraMemory != null && mExtraMemory.length > 0 && mExtraMemory[0] == 0) {
      mExtraMemory = null;
    }

    ComponentsPools.release(this);
  }

  private NodeInfo getOrCreateNodeInfo() {
    if (mNodeInfo == null) {
      mNodeInfo = NodeInfo.acquire();
    }

    return mNodeInfo;
  }

  /**
   * Check that the root of the nested tree we are going to use, has valid layout directions with
   * its main tree holder node.
   */
  static boolean hasValidLayoutDirectionInNestedTree(
      InternalNode nestedTreeHolder, InternalNode nestedTree) {
    final boolean nestedTreeHasExplicitDirection =
        ((nestedTree.mPrivateFlags & PFLAG_LAYOUT_DIRECTION_IS_SET) != 0L);
    final boolean hasSameLayoutDirection =
        (nestedTree.getResolvedLayoutDirection() == nestedTreeHolder.getResolvedLayoutDirection());

    return nestedTreeHasExplicitDirection || hasSameLayoutDirection;
  }

  /**
   * Adds an item to a possibly nulled list to defer the allocation as long as possible.
   */
  private static <A> List<A> addOrCreateList(@Nullable List<A> list, A item) {
    if (list == null) {
      list = new LinkedList<>();
    }

    list.add(item);

    return list;
  }

  private void setIsPaddingPercent(YogaEdge edge, boolean isPaddingPercent) {
    if (mIsPaddingPercent == null && isPaddingPercent) {
      mIsPaddingPercent = new boolean[YogaEdge.ALL.intValue() + 1];
    }
    if (mIsPaddingPercent != null) {
      mIsPaddingPercent[edge.intValue()] = isPaddingPercent;
    }
  }

  private boolean isPaddingPercent(YogaEdge edge) {
    return mIsPaddingPercent != null && mIsPaddingPercent[edge.intValue()];
  }

  /**
   * Crash if the given node has context specific style set.
   */
  static void assertContextSpecificStyleNotSet(InternalNode node) {
    List<CharSequence> errorTypes = null;

    if ((node.mPrivateFlags & PFLAG_ALIGN_SELF_IS_SET) != 0L) {
      errorTypes = addOrCreateList(errorTypes, "alignSelf");
    }
    if ((node.mPrivateFlags & PFLAG_POSITION_TYPE_IS_SET) != 0L) {
      errorTypes = addOrCreateList(errorTypes, "positionType");
    }
    if ((node.mPrivateFlags & PFLAG_FLEX_IS_SET) != 0L) {
      errorTypes = addOrCreateList(errorTypes, "flex");
    }
    if ((node.mPrivateFlags & PFLAG_FLEX_GROW_IS_SET) != 0L) {
      errorTypes = addOrCreateList(errorTypes, "flexGrow");
    }
    if ((node.mPrivateFlags & PFLAG_MARGIN_IS_SET) != 0L) {
      errorTypes = addOrCreateList(errorTypes, "margin");
    }

    if (errorTypes != null) {
      final CharSequence errorStr = TextUtils.join(", ", errorTypes);
      final ComponentsLogger logger = node.getContext().getLogger();
      if (logger != null) {
        logger.emitMessage(
            WARNING,
            "You should not set "
                + errorStr
                + " to a root layout in "
                + node.getRootComponent().getClass().getSimpleName());
      }
    }
  }

  public TreeProps getPendingTreeProps() {
    return mPendingTreeProps;
  }

  private <T extends Drawable> void setPaddingFromDrawableReference(@Nullable Reference<T> ref) {
    if (ref == null) {
      return;
    }
    final T drawable = Reference.acquire(mComponentContext.getAndroidContext(), ref);
    if (drawable != null) {
      final Rect backgroundPadding = ComponentsPools.acquireRect();
      if (getDrawablePadding(drawable, backgroundPadding)) {
        paddingPx(LEFT, backgroundPadding.left);
        paddingPx(TOP, backgroundPadding.top);
        paddingPx(RIGHT, backgroundPadding.right);
        paddingPx(BOTTOM, backgroundPadding.bottom);
      }

      Reference.release(mComponentContext.getAndroidContext(), drawable, ref);
      ComponentsPools.release(backgroundPadding);
    }
  }

  /**
   * This is a wrapper on top of built in {@link Drawable#getPadding(Rect)} which overrides
   * default return value. The reason why we need this - is because on pre-L devices LayerDrawable
   * always returns "true" even if drawable doesn't have padding (see https://goo.gl/gExcMQ).
   * Since we heavily rely on correctness of this information, we need to check padding manually
   */
  private static boolean getDrawablePadding(Drawable drawable, Rect outRect) {
    drawable.getPadding(outRect);
    return outRect.bottom != 0 || outRect.top != 0 || outRect.left != 0 || outRect.right != 0;
  }

  /** This method marks all resolved layout property values to undefined. */
  void resetResolvedLayoutProperties() {
    mResolvedTouchExpansionLeft = YogaConstants.UNDEFINED;
    mResolvedTouchExpansionRight = YogaConstants.UNDEFINED;
    mResolvedX = YogaConstants.UNDEFINED;
    mResolvedY = YogaConstants.UNDEFINED;
    mResolvedWidth = YogaConstants.UNDEFINED;
    mResolvedHeight = YogaConstants.UNDEFINED;
  }
}
