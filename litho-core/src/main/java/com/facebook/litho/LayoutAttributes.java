/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static android.support.annotation.Dimension.DP;

import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.DimenRes;
import android.support.annotation.Dimension;
import android.support.annotation.DrawableRes;
import android.support.annotation.Px;
import android.support.annotation.StringRes;
import android.util.SparseArray;
import android.view.ViewOutlineProvider;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.reference.DrawableReference;
import com.facebook.litho.reference.Reference;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaPositionType;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal class that holds layout attributes and then copies them onto an {@link InternalNode}.
 */
@ThreadConfined(ThreadConfined.ANY)
class LayoutAttributes implements ComponentLayout.Builder {

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
  private static final long PFLAG_POSITION_PERCENT_IS_SET = 1L << 12;
  private static final long PFLAG_WIDTH_IS_SET = 1L << 13;
  private static final long PFLAG_MIN_WIDTH_IS_SET = 1L << 14;
  private static final long PFLAG_MAX_WIDTH_IS_SET = 1L << 15;
  private static final long PFLAG_HEIGHT_IS_SET = 1L << 16;
  private static final long PFLAG_MIN_HEIGHT_IS_SET = 1L << 17;
  private static final long PFLAG_MAX_HEIGHT_IS_SET = 1L << 18;
  private static final long PFLAG_BACKGROUND_IS_SET = 1L << 19;
  private static final long PFLAG_FOREGROUND_IS_SET = 1L << 20;
  private static final long PFLAG_VISIBLE_HANDLER_IS_SET = 1L << 21;
  private static final long PFLAG_FOCUSED_HANDLER_IS_SET = 1L << 22;
  private static final long PFLAG_FULL_IMPRESSION_HANDLER_IS_SET = 1L << 23;
  private static final long PFLAG_INVISIBLE_HANDLER_IS_SET = 1L << 24;
  private static final long PFLAG_UNFOCUSED_HANDLER_IS_SET = 1L << 25;
  private static final long PFLAG_TOUCH_EXPANSION_IS_SET = 1L << 26;
  private static final long PFLAG_BORDER_WIDTH_IS_SET = 1L << 27;
  private static final long PFLAG_ASPECT_RATIO_IS_SET = 1L << 28;
  private static final long PFLAG_TRANSITION_KEY_IS_SET = 1L << 29;
  private static final long PFLAG_BORDER_COLOR_IS_SET = 1L << 30;
  private static final long PFLAG_WRAP_IN_VIEW_IS_SET = 1L << 31;
  private static final long PFLAG_VISIBLE_HEIGHT_RATIO_IS_SET = 1L << 32;
  private static final long PFLAG_VISIBLE_WIDTH_RATIO_IS_SET = 1L << 33;
  private static final long PFLAG_TEST_KEY_IS_SET = 1L << 34;
  private static final long PFLAG_FLEX_BASIS_PERCENT_IS_SET = 1L << 35;
  private static final long PFLAG_MARGIN_PERCENT_IS_SET = 1L << 36;
  private static final long PFLAG_MARGIN_AUTO_IS_SET = 1L << 37;
  private static final long PFLAG_PADDING_PERCENT_IS_SET = 1L << 38;
  private static final long PFLAG_WIDTH_PERCENT_IS_SET = 1L << 39;
  private static final long PFLAG_MIN_WIDTH_PERCENT_IS_SET = 1L << 40;
  private static final long PFLAG_MAX_WIDTH_PERCENT_IS_SET = 1L << 41;
  private static final long PFLAG_HEIGHT_PERCENT_IS_SET = 1L << 42;
  private static final long PFLAG_MIN_HEIGHT_PERCENT_IS_SET = 1L << 43;
  private static final long PFLAG_MAX_HEIGHT_PERCENT_IS_SET = 1L << 44;

  private final ResourceResolver mResourceResolver = new ResourceResolver();

  private ComponentContext mComponentContext;
  private Resources mResources;
  private long mPrivateFlags;

  private NodeInfo mNodeInfo;
  private float mVisibleHeightRatio;
  private float mVisibleWidthRatio;
  private EventHandler<VisibleEvent> mVisibleHandler;
  private EventHandler<FocusedVisibleEvent> mFocusedHandler;
  private EventHandler<UnfocusedVisibleEvent> mUnfocusedHandler;
  private EventHandler<FullImpressionVisibleEvent> mFullImpressionHandler;
  private EventHandler<InvisibleEvent> mInvisibleHandler;
  private String mTestKey;
  private YogaDirection mLayoutDirection;
  private YogaAlign mAlignSelf;
  private YogaPositionType mPositionType;
  private float mFlex;
  private float mFlexGrow;
  private float mFlexShrink;
  @Px private int mFlexBasisPx;
  private float mFlexBasisPercent;
  private int mImportantForAccessibility;
  private boolean mDuplicateParentState;
  private YogaEdgesWithInts mMargins;
  private YogaEdgesWithFloats mMarginPercents;
  private List<YogaEdge> mMarginAutos;
  private YogaEdgesWithInts mPaddings;
  private YogaEdgesWithFloats mPaddingPercents;
  private int mBorderColor;
  private YogaEdgesWithInts mBorderWidths;
  private YogaEdgesWithInts mPositions;
  private YogaEdgesWithFloats mPositionPercents;
  private YogaEdgesWithInts mTouchExpansions;
  @Px private int mWidthPx;
  private float mWidthPercent;
  @Px private int mMinWidthPx;
  private float mMinWidthPercent;
  @Px private int mMaxWidthPx;
  private float mMaxWidthPercent;
  @Px private int mHeightPx;
  private float mHeightPercent;
  @Px private int mMinHeightPx;
  private float mMinHeightPercent;
  @Px private int mMaxHeightPx;
  private float mMaxHeightPercent;
  private float mAspectRatio;
  private Reference<? extends Drawable> mBackground;
  private Drawable mForeground;
  private String mTransitionKey;

  private ComponentLayout.Builder mNodeToCopyInto;

  void init(ComponentContext componentContext, ComponentLayout.Builder layoutToCopyInto) {
    mComponentContext = componentContext;
    mNodeToCopyInto = layoutToCopyInto;
    mResources = componentContext.getResources();
    mResourceResolver.init(mComponentContext, componentContext.getResourceCache());
  }

  @Override
  public LayoutAttributes layoutDirection(YogaDirection direction) {
    mPrivateFlags |= PFLAG_LAYOUT_DIRECTION_IS_SET;
    mLayoutDirection = direction;
    return this;
  }

  @Override
  public LayoutAttributes alignSelf(YogaAlign alignSelf) {
    mPrivateFlags |= PFLAG_ALIGN_SELF_IS_SET;
    mAlignSelf = alignSelf;
    return this;
  }

  @Override
  public LayoutAttributes positionType(YogaPositionType positionType) {
    mPrivateFlags |= PFLAG_POSITION_TYPE_IS_SET;
    mPositionType = positionType;
    return this;
  }

  @Override
  public LayoutAttributes flex(float flex) {
    mPrivateFlags |= PFLAG_FLEX_IS_SET;
    mFlex = flex;
    return this;
  }

  @Override
  public LayoutAttributes flexGrow(float flexGrow) {
    mPrivateFlags |= PFLAG_FLEX_GROW_IS_SET;
    mFlexGrow = flexGrow;
    return this;
  }

  @Override
  public LayoutAttributes flexShrink(float flexShrink) {
    mPrivateFlags |= PFLAG_FLEX_SHRINK_IS_SET;
    mFlexShrink = flexShrink;
    return this;
  }

  @Override
  public LayoutAttributes flexBasisPx(@Px int flexBasis) {
    mPrivateFlags |= PFLAG_FLEX_BASIS_IS_SET;
    mFlexBasisPx = flexBasis;
    return this;
  }

  @Override
  public LayoutAttributes flexBasisPercent(float percent) {
    mPrivateFlags |= PFLAG_FLEX_BASIS_PERCENT_IS_SET;
    mFlexBasisPercent = percent;
    return this;
  }

  @Override
  public LayoutAttributes flexBasisAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return flexBasisPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public LayoutAttributes flexBasisAttr(@AttrRes int resId) {
    return flexBasisAttr(resId, 0);
  }

  @Override
  public LayoutAttributes flexBasisRes(@DimenRes int resId) {
    return flexBasisPx(mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public LayoutAttributes flexBasisDip(@Dimension(unit = DP) int flexBasis) {
    return flexBasisPx(mResourceResolver.dipsToPixels(flexBasis));
  }

  @Override
  public LayoutAttributes importantForAccessibility(int importantForAccessibility) {
    mPrivateFlags |= PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET;
    mImportantForAccessibility = importantForAccessibility;
    return this;
  }

  @Override
  public LayoutAttributes duplicateParentState(boolean duplicateParentState) {
    mPrivateFlags |= PFLAG_DUPLICATE_PARENT_STATE_IS_SET;
    mDuplicateParentState = duplicateParentState;
    return this;
  }

  @Override
  public LayoutAttributes marginPx(YogaEdge edge, @Px int margin) {
    mPrivateFlags |= PFLAG_MARGIN_IS_SET;

    if (mMargins == null) {
      mMargins = new YogaEdgesWithInts();
    }
    mMargins.add(edge, margin);
    return this;
  }

  @Override
  public LayoutAttributes marginPercent(YogaEdge edge, float percent) {
    mPrivateFlags |= PFLAG_MARGIN_PERCENT_IS_SET;
    if (mMarginPercents == null) {
      mMarginPercents = new YogaEdgesWithFloats();
    }
    mMarginPercents.add(edge, percent);
    return this;
  }

  @Override
  public LayoutAttributes marginAuto(YogaEdge edge) {
    mPrivateFlags |= PFLAG_MARGIN_AUTO_IS_SET;
    if (mMarginAutos == null) {
      mMarginAutos = new ArrayList<>(2);
    }
    mMarginAutos.add(edge);
    return this;
  }

  @Override
  public LayoutAttributes marginAttr(
      YogaEdge edge, @AttrRes int resId, @DimenRes int defaultResId) {
    return marginPx(edge, mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public LayoutAttributes marginAttr(YogaEdge edge, @AttrRes int resId) {
    return marginAttr(edge, resId, 0);
  }

  @Override
  public LayoutAttributes marginRes(YogaEdge edge, @DimenRes int resId) {
    return marginPx(edge, mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public LayoutAttributes marginDip(YogaEdge edge, @Dimension(unit = DP) int margin) {
    return marginPx(edge, mResourceResolver.dipsToPixels(margin));
  }

  @Override
  public LayoutAttributes paddingPx(YogaEdge edge, @Px int padding) {
    mPrivateFlags |= PFLAG_PADDING_IS_SET;
    if (mPaddings == null) {
      mPaddings = new YogaEdgesWithInts();
    }
    mPaddings.add(edge, padding);
    return this;
  }

  @Override
  public LayoutAttributes paddingPercent(YogaEdge edge, float percent) {
    mPrivateFlags |= PFLAG_PADDING_PERCENT_IS_SET;
    if (mPaddingPercents == null) {
      mPaddingPercents = new YogaEdgesWithFloats();
    }
    mPaddingPercents.add(edge, percent);
    return this;
  }

  @Override
  public LayoutAttributes paddingAttr(
      YogaEdge edge, @AttrRes int resId, @DimenRes int defaultResId) {
    return paddingPx(edge, mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public LayoutAttributes paddingAttr(YogaEdge edge, @AttrRes int resId) {
    return paddingAttr(edge, resId, 0);
  }

  @Override
  public LayoutAttributes paddingRes(YogaEdge edge, @DimenRes int resId) {
    return paddingPx(edge, mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public LayoutAttributes paddingDip(YogaEdge edge, @Dimension(unit = DP) int padding) {
    return paddingPx(edge, mResourceResolver.dipsToPixels(padding));
  }

  @Override
  public LayoutAttributes borderWidthPx(YogaEdge edge, @Px int borderWidth) {
    mPrivateFlags |= PFLAG_BORDER_WIDTH_IS_SET;
    if (mBorderWidths == null) {
      mBorderWidths = new YogaEdgesWithInts();
    }
    mBorderWidths.add(edge, borderWidth);
    return this;
  }

  @Override
  public LayoutAttributes borderWidthAttr(
      YogaEdge edge, @AttrRes int resId, @DimenRes int defaultResId) {
    return borderWidthPx(edge, mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public LayoutAttributes borderWidthAttr(YogaEdge edge, @AttrRes int resId) {
    return borderWidthAttr(edge, resId, 0);
  }

  @Override
  public LayoutAttributes borderWidthRes(YogaEdge edge, @DimenRes int resId) {
    return borderWidthPx(edge, mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public LayoutAttributes borderWidthDip(YogaEdge edge, @Dimension(unit = DP) int borderWidth) {
    return borderWidthPx(edge, mResourceResolver.dipsToPixels(borderWidth));
  }

  @Override
  public LayoutAttributes borderColor(@ColorInt int borderColor) {
    mPrivateFlags |= PFLAG_BORDER_COLOR_IS_SET;
    mBorderColor = borderColor;
    return this;
  }

  @Override
  public LayoutAttributes positionPx(YogaEdge edge, @Px int position) {
    mPrivateFlags |= PFLAG_POSITION_IS_SET;
    if (mPositions == null) {
      mPositions = new YogaEdgesWithInts();
    }
    mPositions.add(edge, position);
    return this;
  }

  @Override
  public LayoutAttributes positionPercent(YogaEdge edge, float percent) {
    mPrivateFlags |= PFLAG_POSITION_PERCENT_IS_SET;
    if (mPositionPercents == null) {
      mPositionPercents = new YogaEdgesWithFloats();
    }
    mPositionPercents.add(edge, percent);
    return this;
  }

  @Override
  public LayoutAttributes positionAttr(
      YogaEdge edge, @AttrRes int resId, @DimenRes int defaultResId) {
    return positionPx(edge, mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public LayoutAttributes positionAttr(YogaEdge edge, @AttrRes int resId) {
    return positionAttr(edge, resId, 0);
  }

  @Override
  public LayoutAttributes positionRes(YogaEdge edge, @DimenRes int resId) {
    return positionPx(edge, mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public LayoutAttributes positionDip(YogaEdge edge, @Dimension(unit = DP) int position) {
    return positionPx(edge, mResourceResolver.dipsToPixels(position));
  }

  @Override
  public LayoutAttributes widthPx(@Px int width) {
    mPrivateFlags |= PFLAG_WIDTH_IS_SET;
    mWidthPx = width;
    return this;
  }

  @Override
  public LayoutAttributes widthPercent(float percent) {
    mPrivateFlags |= PFLAG_WIDTH_PERCENT_IS_SET;
    mWidthPercent = percent;
    return this;
  }

  @Override
  public LayoutAttributes widthRes(@DimenRes int resId) {
    return widthPx(mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public LayoutAttributes widthAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return widthPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public LayoutAttributes widthAttr(@AttrRes int resId) {
    return widthAttr(resId, 0);
  }

  @Override
  public LayoutAttributes widthDip(@Dimension(unit = DP) int width) {
    return widthPx(mResourceResolver.dipsToPixels(width));
  }

  @Override
  public LayoutAttributes minWidthPx(@Px int minWidth) {
    mPrivateFlags |= PFLAG_MIN_WIDTH_IS_SET;
    mMinWidthPx = minWidth;
    return this;
  }

  @Override
  public LayoutAttributes minWidthPercent(float percent) {
    mPrivateFlags |= PFLAG_MIN_WIDTH_PERCENT_IS_SET;
    mMinWidthPercent = percent;
    return this;
  }

  @Override
  public LayoutAttributes minWidthAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return minWidthPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public LayoutAttributes minWidthAttr(@AttrRes int resId) {
    return minWidthAttr(resId, 0);
  }

  @Override
  public LayoutAttributes minWidthRes(@DimenRes int resId) {
    return minWidthPx(mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public LayoutAttributes minWidthDip(@Dimension(unit = DP) int minWidth) {
    return minWidthPx(mResourceResolver.dipsToPixels(minWidth));
  }

  @Override
  public LayoutAttributes maxWidthPx(@Px int maxWidth) {
    mPrivateFlags |= PFLAG_MAX_WIDTH_IS_SET;
    mMaxWidthPx = maxWidth;
    return this;
  }

  @Override
  public LayoutAttributes maxWidthPercent(float percent) {
    mPrivateFlags |= PFLAG_MAX_WIDTH_PERCENT_IS_SET;
    mMaxWidthPercent = percent;
    return this;
  }

  @Override
  public LayoutAttributes maxWidthAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return maxWidthPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public LayoutAttributes maxWidthAttr(@AttrRes int resId) {
    return maxWidthAttr(resId, 0);
  }

  @Override
  public LayoutAttributes maxWidthRes(@DimenRes int resId) {
    return maxWidthPx(mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public LayoutAttributes maxWidthDip(@Dimension(unit = DP) int maxWidth) {
    return maxWidthPx(mResourceResolver.dipsToPixels(maxWidth));
  }

  @Override
  public LayoutAttributes heightPx(@Px int height) {
    mPrivateFlags |= PFLAG_HEIGHT_IS_SET;
    mHeightPx = height;
    return this;
  }

  @Override
  public LayoutAttributes heightPercent(float percent) {
    mPrivateFlags |= PFLAG_HEIGHT_PERCENT_IS_SET;
    mHeightPercent = percent;
    return this;
  }

  @Override
  public LayoutAttributes heightRes(@DimenRes int resId) {
    return heightPx(mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public LayoutAttributes heightAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return heightPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public LayoutAttributes heightAttr(@AttrRes int resId) {
    return heightAttr(resId, 0);
  }

  @Override
  public LayoutAttributes heightDip(@Dimension(unit = DP) int height) {
    return heightPx(mResourceResolver.dipsToPixels(height));
  }

  @Override
  public LayoutAttributes minHeightPx(@Px int minHeight) {
    mPrivateFlags |= PFLAG_MIN_HEIGHT_IS_SET;
    mMinHeightPx = minHeight;
    return this;
  }

  @Override
  public LayoutAttributes minHeightPercent(float percent) {
    mPrivateFlags |= PFLAG_MIN_HEIGHT_PERCENT_IS_SET;
    mMinHeightPercent = percent;
    return this;
  }

  @Override
  public LayoutAttributes minHeightAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return minHeightPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public LayoutAttributes minHeightAttr(@AttrRes int resId) {
    return minHeightAttr(resId, 0);
  }

  @Override
  public LayoutAttributes minHeightRes(@DimenRes int resId) {
    return minHeightPx(mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public LayoutAttributes minHeightDip(@Dimension(unit = DP) int minHeight) {
    return minHeightPx(mResourceResolver.dipsToPixels(minHeight));
  }

  @Override
  public LayoutAttributes maxHeightPx(@Px int maxHeight) {
    mPrivateFlags |= PFLAG_MAX_HEIGHT_IS_SET;
    mMaxHeightPx = maxHeight;
    return this;
  }

  @Override
  public LayoutAttributes maxHeightPercent(float percent) {
    mPrivateFlags |= PFLAG_MAX_HEIGHT_PERCENT_IS_SET;
    mMaxHeightPercent = percent;
    return this;
  }

  @Override
  public LayoutAttributes maxHeightAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return maxHeightPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public LayoutAttributes maxHeightAttr(@AttrRes int resId) {
    return maxHeightAttr(resId, 0);
  }

  @Override
  public LayoutAttributes maxHeightRes(@DimenRes int resId) {
    return maxHeightPx(mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public LayoutAttributes maxHeightDip(@Dimension(unit = DP) int maxHeight) {
    return maxHeightPx(mResourceResolver.dipsToPixels(maxHeight));
  }

  @Override
  public LayoutAttributes aspectRatio(float aspectRatio) {
    mPrivateFlags |= PFLAG_ASPECT_RATIO_IS_SET;
    mAspectRatio = aspectRatio;
    return this;
  }

  @Override
  public LayoutAttributes touchExpansionPx(YogaEdge edge, @Px int touchExpansion) {
    mPrivateFlags |= PFLAG_TOUCH_EXPANSION_IS_SET;
    if (mTouchExpansions == null) {
      mTouchExpansions = new YogaEdgesWithInts();
    }
    mTouchExpansions.add(edge, touchExpansion);

    return this;
  }

  @Override
  public LayoutAttributes touchExpansionAttr(
      YogaEdge edge, @AttrRes int resId, @DimenRes int defaultResId) {
    return touchExpansionPx(edge, mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public LayoutAttributes touchExpansionAttr(YogaEdge edge, @AttrRes int resId) {
    return touchExpansionAttr(edge, resId, 0);
  }

  @Override
  public LayoutAttributes touchExpansionRes(YogaEdge edge, @DimenRes int resId) {
    return touchExpansionPx(edge, mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public LayoutAttributes touchExpansionDip(
      YogaEdge edge, @Dimension(unit = DP) int touchExpansion) {
    return touchExpansionPx(edge, mResourceResolver.dipsToPixels(touchExpansion));
  }

  @Override
  public LayoutAttributes background(Reference<? extends Drawable> background) {
    mPrivateFlags |= PFLAG_BACKGROUND_IS_SET;
    mBackground = background;
    return this;
  }

  @Override
  public LayoutAttributes background(Reference.Builder<? extends Drawable> builder) {
    return background(builder.build());
  }

  @Override
  public LayoutAttributes background(Drawable background) {
    return background(DrawableReference.create().drawable(background));
  }

  @Override
  public LayoutAttributes backgroundAttr(@AttrRes int resId, @DrawableRes int defaultResId) {
    return backgroundRes(mResourceResolver.resolveResIdAttr(resId, defaultResId));
  }

  @Override
  public LayoutAttributes backgroundAttr(@AttrRes int resId) {
    return backgroundAttr(resId, 0);
  }

  @Override
  public LayoutAttributes backgroundRes(@DrawableRes int resId) {
    if (resId == 0) {
      return background((Reference<? extends Drawable>) null);
    }

    return background(mComponentContext.getResources().getDrawable(resId));
  }

  @Override
  public LayoutAttributes backgroundColor(@ColorInt int backgroundColor) {
    return background(new ColorDrawable(backgroundColor));
  }

  @Override
  public LayoutAttributes foreground(Drawable foreground) {
    mPrivateFlags |= PFLAG_FOREGROUND_IS_SET;
    mForeground = foreground;
    return this;
  }

  @Override
  public LayoutAttributes foregroundAttr(@AttrRes int resId, @DrawableRes int defaultResId) {
    return foregroundRes(mResourceResolver.resolveResIdAttr(resId, defaultResId));
  }

  @Override
  public LayoutAttributes foregroundAttr(@AttrRes int resId) {
    return foregroundAttr(resId, 0);
  }

  @Override
  public LayoutAttributes foregroundRes(@DrawableRes int resId) {
    if (resId == 0) {
      return foreground(null);
    }

    return foreground(mResources.getDrawable(resId));
  }

  @Override
  public LayoutAttributes foregroundColor(@ColorInt int foregroundColor) {
    return foreground(new ColorDrawable(foregroundColor));
  }

  @Override
  public LayoutAttributes wrapInView() {
    mPrivateFlags |= PFLAG_WRAP_IN_VIEW_IS_SET;
    return this;
  }

  @Override
  public LayoutAttributes clickHandler(EventHandler<ClickEvent> clickHandler) {
    getOrCreateNodeInfo().setClickHandler(clickHandler);
    return this;
  }

  @Override
  public LayoutAttributes longClickHandler(EventHandler<LongClickEvent> longClickHandler) {
    getOrCreateNodeInfo().setLongClickHandler(longClickHandler);
    return this;
  }

  @Override
  public LayoutAttributes focusChangeHandler(EventHandler<FocusChangedEvent> focusChangeHandler) {
    getOrCreateNodeInfo().setFocusChangeHandler(focusChangeHandler);
    return this;
  }

  @Override
  public LayoutAttributes touchHandler(EventHandler<TouchEvent> touchHandler) {
    getOrCreateNodeInfo().setTouchHandler(touchHandler);
    return this;
  }

  @Override
  public LayoutAttributes interceptTouchHandler(
      EventHandler<InterceptTouchEvent> interceptTouchHandler) {
    getOrCreateNodeInfo().setInterceptTouchHandler(interceptTouchHandler);
    return this;
  }

  @Override
  public LayoutAttributes focusable(boolean isFocusable) {
    getOrCreateNodeInfo().setFocusable(isFocusable);
    return this;
  }

  @Override
  public LayoutAttributes enabled(boolean isEnabled) {
    getOrCreateNodeInfo().setEnabled(isEnabled);
    return this;
  }

  @Override
  public LayoutAttributes visibleHeightRatio(float visibleHeightRatio) {
    mPrivateFlags |= PFLAG_VISIBLE_HEIGHT_RATIO_IS_SET;
    mVisibleHeightRatio = visibleHeightRatio;
    return this;
  }

  @Override
  public LayoutAttributes visibleWidthRatio(float visibleWidthRatio) {
    mPrivateFlags |= PFLAG_VISIBLE_WIDTH_RATIO_IS_SET;
    mVisibleWidthRatio = visibleWidthRatio;
    return this;
  }

  @Override
  public LayoutAttributes visibleHandler(EventHandler<VisibleEvent> visibleHandler) {
    mPrivateFlags |= PFLAG_VISIBLE_HANDLER_IS_SET;
    mVisibleHandler = visibleHandler;
    return this;
  }

  @Override
  public LayoutAttributes focusedHandler(EventHandler<FocusedVisibleEvent> focusedHandler) {
    mPrivateFlags |= PFLAG_FOCUSED_HANDLER_IS_SET;
    mFocusedHandler = focusedHandler;
    return this;
  }

  @Override
  public LayoutAttributes unfocusedHandler(EventHandler<UnfocusedVisibleEvent> unfocusedHandler) {
    mPrivateFlags |= PFLAG_UNFOCUSED_HANDLER_IS_SET;
    mUnfocusedHandler = unfocusedHandler;
    return this;
  }

  @Override
  public LayoutAttributes fullImpressionHandler(
      EventHandler<FullImpressionVisibleEvent> fullImpressionHandler) {
    mPrivateFlags |= PFLAG_FULL_IMPRESSION_HANDLER_IS_SET;
    mFullImpressionHandler = fullImpressionHandler;
    return this;
  }

  @Override
  public LayoutAttributes invisibleHandler(EventHandler<InvisibleEvent> invisibleHandler) {
    mPrivateFlags |= PFLAG_INVISIBLE_HANDLER_IS_SET;
    mInvisibleHandler = invisibleHandler;
    return this;
  }

  @Override
  public LayoutAttributes contentDescription(CharSequence contentDescription) {
    getOrCreateNodeInfo().setContentDescription(contentDescription);
    return this;
  }

  @Override
  public LayoutAttributes contentDescription(@StringRes int stringId) {
    return contentDescription(mResources.getString(stringId));
  }

  @Override
  public LayoutAttributes contentDescription(@StringRes int stringId, Object... formatArgs) {
    return contentDescription(mResources.getString(stringId, formatArgs));
  }

  @Override
  public LayoutAttributes viewTag(Object viewTag) {
    getOrCreateNodeInfo().setViewTag(viewTag);
    return this;
  }

  @Override
  public LayoutAttributes viewTags(SparseArray<Object> viewTags) {
    getOrCreateNodeInfo().setViewTags(viewTags);
    return this;
  }

  @Override
  public LayoutAttributes shadowElevationPx(float shadowElevation) {
    getOrCreateNodeInfo().setShadowElevation(shadowElevation);
    return this;
  }

  @Override
  public LayoutAttributes shadowElevationAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return shadowElevationPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public LayoutAttributes shadowElevationAttr(@AttrRes int resId) {
    return shadowElevationAttr(resId, 0);
  }

  @Override
  public LayoutAttributes shadowElevationRes(@DimenRes int resId) {
    return shadowElevationPx(mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public LayoutAttributes shadowElevationDip(@Dimension(unit = DP) int shadowElevation) {
    return shadowElevationPx(mResourceResolver.dipsToPixels(shadowElevation));
  }

  @Override
  public LayoutAttributes outlineProvider(ViewOutlineProvider outlineProvider) {
    getOrCreateNodeInfo().setOutlineProvider(outlineProvider);
    return this;
  }

  @Override
  public LayoutAttributes clipToOutline(boolean clipToOutline) {
    getOrCreateNodeInfo().setClipToOutline(clipToOutline);
    return this;
  }

  @Override
  public LayoutAttributes testKey(String testKey) {
    mPrivateFlags |= PFLAG_TEST_KEY_IS_SET;
    mTestKey = testKey;
    return this;
  }

  @Override
  public LayoutAttributes dispatchPopulateAccessibilityEventHandler(
      EventHandler<DispatchPopulateAccessibilityEventEvent>
          dispatchPopulateAccessibilityEventHandler) {
    getOrCreateNodeInfo()
        .setDispatchPopulateAccessibilityEventHandler(dispatchPopulateAccessibilityEventHandler);
    return this;
  }

  @Override
  public LayoutAttributes onInitializeAccessibilityEventHandler(
      EventHandler<OnInitializeAccessibilityEventEvent> onInitializeAccessibilityEventHandler) {
    getOrCreateNodeInfo()
        .setOnInitializeAccessibilityEventHandler(onInitializeAccessibilityEventHandler);
    return this;
  }

  @Override
  public LayoutAttributes onInitializeAccessibilityNodeInfoHandler(
      EventHandler<OnInitializeAccessibilityNodeInfoEvent>
          onInitializeAccessibilityNodeInfoHandler) {
    getOrCreateNodeInfo()
        .setOnInitializeAccessibilityNodeInfoHandler(onInitializeAccessibilityNodeInfoHandler);
    return this;
  }

  @Override
  public LayoutAttributes onPopulateAccessibilityEventHandler(
      EventHandler<OnPopulateAccessibilityEventEvent> onPopulateAccessibilityEventHandler) {
    getOrCreateNodeInfo()
        .setOnPopulateAccessibilityEventHandler(onPopulateAccessibilityEventHandler);
    return this;
  }

  @Override
  public LayoutAttributes onRequestSendAccessibilityEventHandler(
      EventHandler<OnRequestSendAccessibilityEventEvent> onRequestSendAccessibilityEventHandler) {
    getOrCreateNodeInfo()
        .setOnRequestSendAccessibilityEventHandler(onRequestSendAccessibilityEventHandler);
    return this;
  }

  @Override
  public LayoutAttributes performAccessibilityActionHandler(
      EventHandler<PerformAccessibilityActionEvent> performAccessibilityActionHandler) {
    getOrCreateNodeInfo().setPerformAccessibilityActionHandler(performAccessibilityActionHandler);
    return this;
  }

  @Override
  public LayoutAttributes sendAccessibilityEventHandler(
      EventHandler<SendAccessibilityEventEvent> sendAccessibilityEventHandler) {
    getOrCreateNodeInfo().setSendAccessibilityEventHandler(sendAccessibilityEventHandler);
    return this;
  }

  @Override
  public LayoutAttributes sendAccessibilityEventUncheckedHandler(
      EventHandler<SendAccessibilityEventUncheckedEvent> sendAccessibilityEventUncheckedHandler) {
    getOrCreateNodeInfo()
        .setSendAccessibilityEventUncheckedHandler(sendAccessibilityEventUncheckedHandler);
    return this;
  }

  @Override
  public LayoutAttributes transitionKey(String key) {
    mPrivateFlags |= PFLAG_TRANSITION_KEY_IS_SET;
    mTransitionKey = key;
    return this;
  }

  @Override
  public ComponentLayout build() {
    copyInto(mNodeToCopyInto);

    return mNodeToCopyInto.build();
  }

  private NodeInfo getOrCreateNodeInfo() {
    if (mNodeInfo == null) {
      mNodeInfo = NodeInfo.acquire();
    }

    return mNodeInfo;
  }

  void copyInto(ComponentLayout.Builder node) {
    if (mNodeInfo != null) {
      mNodeInfo.copyInto(node);
    }

    if ((mPrivateFlags & PFLAG_LAYOUT_DIRECTION_IS_SET) != 0L) {
      node.layoutDirection(mLayoutDirection);
    }
    if ((mPrivateFlags & PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET) != 0L) {
      node.importantForAccessibility(mImportantForAccessibility);
    }
    if ((mPrivateFlags & PFLAG_DUPLICATE_PARENT_STATE_IS_SET) != 0L) {
      node.duplicateParentState(mDuplicateParentState);
    }
    if ((mPrivateFlags & PFLAG_BACKGROUND_IS_SET) != 0L) {
      node.background(mBackground);
    }
    if ((mPrivateFlags & PFLAG_FOREGROUND_IS_SET) != 0L) {
      node.foreground(mForeground);
    }
    if ((mPrivateFlags & PFLAG_WRAP_IN_VIEW_IS_SET) != 0L) {
      node.wrapInView();
    }
    if ((mPrivateFlags & PFLAG_VISIBLE_HANDLER_IS_SET) != 0L) {
      node.visibleHandler(mVisibleHandler);
    }
    if ((mPrivateFlags & PFLAG_FOCUSED_HANDLER_IS_SET) != 0L) {
      node.focusedHandler(mFocusedHandler);
    }
    if ((mPrivateFlags & PFLAG_FULL_IMPRESSION_HANDLER_IS_SET) != 0L) {
      node.fullImpressionHandler(mFullImpressionHandler);
    }
    if ((mPrivateFlags & PFLAG_INVISIBLE_HANDLER_IS_SET) != 0L) {
      node.invisibleHandler(mInvisibleHandler);
    }
    if ((mPrivateFlags & PFLAG_UNFOCUSED_HANDLER_IS_SET) != 0L) {
      node.unfocusedHandler(mUnfocusedHandler);
    }
    if ((mPrivateFlags & PFLAG_TEST_KEY_IS_SET) != 0L) {
      node.testKey(mTestKey);
    }
    if ((mPrivateFlags & PFLAG_TRANSITION_KEY_IS_SET) != 0L) {
      node.transitionKey(mTransitionKey);
    }
    if ((mPrivateFlags & PFLAG_BORDER_COLOR_IS_SET) != 0L) {
      node.borderColor(mBorderColor);
    }
    if ((mPrivateFlags & PFLAG_BORDER_WIDTH_IS_SET) != 0L) {
      for (int i = 0; i < mBorderWidths.mNumEntries; i++) {
        node.borderWidthPx(mBorderWidths.mEdges[i], mBorderWidths.mValues[i]);
      }
    }
    if ((mPrivateFlags & PFLAG_VISIBLE_HEIGHT_RATIO_IS_SET) != 0L) {
      node.visibleHeightRatio(mVisibleHeightRatio);
    }
    if ((mPrivateFlags & PFLAG_VISIBLE_WIDTH_RATIO_IS_SET) != 0L) {
      node.visibleWidthRatio(mVisibleWidthRatio);
    }
    if ((mPrivateFlags & PFLAG_ALIGN_SELF_IS_SET) != 0L) {
      node.alignSelf(mAlignSelf);
    }
    if ((mPrivateFlags & PFLAG_POSITION_TYPE_IS_SET) != 0L) {
      node.positionType(mPositionType);
    }
    if ((mPrivateFlags & PFLAG_POSITION_IS_SET) != 0L) {
      for (int i = 0; i < mPositions.mNumEntries; i++) {
        node.positionPx(mPositions.mEdges[i], mPositions.mValues[i]);
      }
    }
    if ((mPrivateFlags & PFLAG_POSITION_PERCENT_IS_SET) != 0L) {
      for (int i = 0; i < mPositionPercents.mNumEntries; i++) {
        node.positionPercent(mPositionPercents.mEdges[i], mPositionPercents.mValues[i]);
      }
    }
    if ((mPrivateFlags & PFLAG_FLEX_IS_SET) != 0L) {
      node.flex(mFlex);
    }
    if ((mPrivateFlags & PFLAG_FLEX_GROW_IS_SET) != 0L) {
      node.flexGrow(mFlexGrow);
    }
    if ((mPrivateFlags & PFLAG_FLEX_SHRINK_IS_SET) != 0L) {
      node.flexShrink(mFlexShrink);
    }
    if ((mPrivateFlags & PFLAG_FLEX_BASIS_IS_SET) != 0L) {
      node.flexBasisPx(mFlexBasisPx);
    }
    if ((mPrivateFlags & PFLAG_FLEX_BASIS_PERCENT_IS_SET) != 0L) {
      node.flexBasisPercent(mFlexBasisPercent);
    }
    if ((mPrivateFlags & PFLAG_WIDTH_IS_SET) != 0L) {
      node.widthPx(mWidthPx);
    }
    if ((mPrivateFlags & PFLAG_WIDTH_PERCENT_IS_SET) != 0L) {
      node.widthPercent(mWidthPercent);
    }
    if ((mPrivateFlags & PFLAG_MIN_WIDTH_IS_SET) != 0L) {
      node.minWidthPx(mMinWidthPx);
    }
    if ((mPrivateFlags & PFLAG_MIN_WIDTH_PERCENT_IS_SET) != 0L) {
      node.minWidthPercent(mMinWidthPercent);
    }
    if ((mPrivateFlags & PFLAG_MAX_WIDTH_IS_SET) != 0L) {
      node.maxWidthPx(mMaxWidthPx);
    }
    if ((mPrivateFlags & PFLAG_MAX_WIDTH_PERCENT_IS_SET) != 0L) {
      node.maxWidthPercent(mMaxWidthPercent);
    }
    if ((mPrivateFlags & PFLAG_HEIGHT_IS_SET) != 0L) {
      node.heightPx(mHeightPx);
    }
    if ((mPrivateFlags & PFLAG_HEIGHT_PERCENT_IS_SET) != 0L) {
      node.heightPercent(mHeightPercent);
    }
    if ((mPrivateFlags & PFLAG_MIN_HEIGHT_IS_SET) != 0L) {
      node.minHeightPx(mMinHeightPx);
    }
    if ((mPrivateFlags & PFLAG_MIN_HEIGHT_PERCENT_IS_SET) != 0L) {
      node.minHeightPercent(mMinHeightPercent);
    }
    if ((mPrivateFlags & PFLAG_MAX_HEIGHT_IS_SET) != 0L) {
      node.maxHeightPx(mMaxHeightPx);
    }
    if ((mPrivateFlags & PFLAG_MAX_HEIGHT_PERCENT_IS_SET) != 0L) {
      node.maxHeightPercent(mMaxHeightPercent);
    }
    if ((mPrivateFlags & PFLAG_ASPECT_RATIO_IS_SET) != 0L) {
      node.aspectRatio(mAspectRatio);
    }
    if ((mPrivateFlags & PFLAG_MARGIN_IS_SET) != 0L) {
      for (int i = 0; i < mMargins.mNumEntries; i++) {
        node.marginPx(mMargins.mEdges[i], mMargins.mValues[i]);
      }
    }
    if ((mPrivateFlags & PFLAG_MARGIN_PERCENT_IS_SET) != 0L) {
      for (int i = 0; i < mMarginPercents.mNumEntries; i++) {
        node.marginPercent(mMarginPercents.mEdges[i], mMarginPercents.mValues[i]);
      }
    }
    if ((mPrivateFlags & PFLAG_MARGIN_AUTO_IS_SET) != 0L) {
      for (YogaEdge edge : mMarginAutos) {
        node.marginAuto(edge);
      }
    }
    if ((mPrivateFlags & PFLAG_PADDING_IS_SET) != 0L) {
      for (int i = 0; i < mPaddings.mNumEntries; i++) {
        node.paddingPx(mPaddings.mEdges[i], mPaddings.mValues[i]);
      }
    }
    if ((mPrivateFlags & PFLAG_PADDING_PERCENT_IS_SET) != 0L) {
      for (int i = 0; i < mPaddingPercents.mNumEntries; i++) {
        node.paddingPercent(mPaddingPercents.mEdges[i], mPaddingPercents.mValues[i]);
      }
    }
    if ((mPrivateFlags & PFLAG_TOUCH_EXPANSION_IS_SET) != 0L) {
      for (int i = 0; i < mTouchExpansions.mNumEntries; i++) {
        node.touchExpansionPx(mTouchExpansions.mEdges[i], mTouchExpansions.mValues[i]);
      }
    }
  }

  private class YogaEdgesWithInts {
    private YogaEdge[] mEdges = new YogaEdge[8];
    private int[] mValues = new int[8];
    private int mNumEntries;

    private void add(YogaEdge yogaEdge, int value) {
      mEdges[mNumEntries] = yogaEdge;
      mValues[mNumEntries] = value;
      mNumEntries++;
    }
  }

  private class YogaEdgesWithFloats {
    private YogaEdge[] mEdges = new YogaEdge[8];
    private float[] mValues = new float[8];
    private int mNumEntries;

    private void add(YogaEdge yogaEdge, float value) {
      mEdges[mNumEntries] = yogaEdge;
      mValues[mNumEntries] = value;
      mNumEntries++;
    }
  }
}
