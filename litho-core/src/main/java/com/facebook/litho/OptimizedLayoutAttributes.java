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
class OptimizedLayoutAttributes implements ComponentLayout.Builder {

  // Flags used to indicate that a certain attribute was explicitly set on the node.
  private static final long PFLAG_POSITION_TYPE_IS_SET = 1L << 1;
  private static final long PFLAG_POSITION_IS_SET = 1L << 2;
  private static final long PFLAG_FLEX_SHRINK_IS_SET = 1L << 3;
  private static final long PFLAG_WIDTH_IS_SET = 1L << 4;
  private static final long PFLAG_HEIGHT_IS_SET = 1L << 5;
  private static final long PFLAG_BACKGROUND_IS_SET = 1L << 6;
  private static final long PFLAG_TEST_KEY_IS_SET = 1L << 7;

  // Indices for entries into the SparseArray.
  private static final int LAYOUT_DIRECTION = 0;
  private static final int ALIGN_SELF = 1;
  private static final int FLEX = 3;
  private static final int FLEX_GROW = 4;
  private static final int FLEX_BASIS = 6;
  private static final int IMPORTANT_FOR_ACCESSIBILITY = 7;
  private static final int DUPLICATE_PARENT_STATE = 8;
  private static final int MARGIN = 9;
  private static final int PADDING = 10;
  private static final int POSITION_PERCENT = 12;
  private static final int MIN_WIDTH = 14;
  private static final int MAX_WIDTH = 15;
  private static final int MIN_HEIGHT = 17;
  private static final int MAX_HEIGHT = 18;
  private static final int FOREGROUND = 20;
  private static final int VISIBLE_HANDLER = 21;
  private static final int FOCUSED_HANDLER = 22;
  private static final int FULL_IMPRESSION_HANDLER = 23;
  private static final int INVISIBLE_HANDLER = 24;
  private static final int UNFOCUSED_HANDLER = 25;
  private static final int TOUCH_EXPANSION = 26;
  private static final int BORDER_WIDTH = 27;
  private static final int ASPECT_RATIO = 28;
  private static final int TRANSITION_KEY = 29;
  private static final int BORDER_COLOR = 30;
  private static final int WRAP_IN_VIEW = 31;
  private static final int VISIBLE_HEIGHT_RATIO = 32;
  private static final int VISIBLE_WIDTH_RATIO = 33;
  private static final int FLEX_BASIS_PERCENT = 35;
  private static final int MARGIN_PERCENT = 36;
  private static final int MARGIN_AUTO = 37;
  private static final int PADDING_PERCENT = 38;
  private static final int WIDTH_PERCENT = 39;
  private static final int MIN_WIDTH_PERCENT = 40;
  private static final int MAX_WIDTH_PERCENT = 41;
  private static final int HEIGHT_PERCENT = 42;
  private static final int MIN_HEIGHT_PERCENT = 43;
  private static final int MAX_HEIGHT_PERCENT = 44;
  private static final int BORDER = 45;

  private SparseArray<Object> mSparseArray;

  private final ResourceResolver mResourceResolver = new ResourceResolver();

  private long mPrivateFlags;
  private NodeInfo mNodeInfo;
  private ComponentContext mComponentContext;
  private ComponentLayout.Builder mNodeToCopyInto;

  private YogaPositionType mPositionType;
  private YogaEdgesWithInts mPositions;
  private float mFlexShrink;
  private int mWidthPx;
  private int mHeightPx;
  private Reference<? extends Drawable> mBackground;
  private String mTestKey;

  void init(ComponentContext componentContext, ComponentLayout.Builder layoutToCopyInto) {
    mComponentContext = componentContext;
    mNodeToCopyInto = layoutToCopyInto;
    mResourceResolver.init(mComponentContext, componentContext.getResourceCache());
  }

  private SparseArray<Object> getOrCreateSparseArray() {
    if (mSparseArray == null) {
      mSparseArray = new SparseArray<>();
    }

    return mSparseArray;
  }

  @Override
  public OptimizedLayoutAttributes positionType(YogaPositionType positionType) {
    mPrivateFlags |= PFLAG_POSITION_TYPE_IS_SET;
    mPositionType = positionType;
    return this;
  }

  @Override
  public OptimizedLayoutAttributes positionPx(YogaEdge edge, @Px int position) {
    mPrivateFlags |= PFLAG_POSITION_IS_SET;
    if (mPositions == null) {
      mPositions = new YogaEdgesWithInts();
    }

    mPositions.add(edge, position);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes flexShrink(float flexShrink) {
    mPrivateFlags |= PFLAG_FLEX_SHRINK_IS_SET;
    mFlexShrink = flexShrink;
    return this;
  }

  @Override
  public OptimizedLayoutAttributes widthPx(@Px int width) {
    mPrivateFlags |= PFLAG_WIDTH_IS_SET;
    mWidthPx = width;
    return this;
  }

  @Override
  public OptimizedLayoutAttributes heightPx(@Px int height) {
    mPrivateFlags |= PFLAG_HEIGHT_IS_SET;
    mHeightPx = height;
    return this;
  }

  @Override
  public OptimizedLayoutAttributes background(Reference<? extends Drawable> background) {
    mPrivateFlags |= PFLAG_BACKGROUND_IS_SET;
    mBackground = background;
    return this;
  }

  @Override
  public OptimizedLayoutAttributes testKey(String testKey) {
    mPrivateFlags |= PFLAG_TEST_KEY_IS_SET;
    mTestKey = testKey;
    return this;
  }

  @Override
  public OptimizedLayoutAttributes layoutDirection(YogaDirection direction) {
    getOrCreateSparseArray().put(LAYOUT_DIRECTION, direction);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes alignSelf(YogaAlign alignSelf) {
    getOrCreateSparseArray().put(ALIGN_SELF, alignSelf);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes flex(float flex) {
    getOrCreateSparseArray().put(FLEX, flex);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes flexGrow(float flexGrow) {
    getOrCreateSparseArray().put(FLEX_GROW, flexGrow);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes flexBasisPx(@Px int flexBasis) {
    getOrCreateSparseArray().put(FLEX_BASIS, flexBasis);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes flexBasisPercent(float percent) {
    getOrCreateSparseArray().put(FLEX_BASIS_PERCENT, percent);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes flexBasisAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return flexBasisPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public OptimizedLayoutAttributes flexBasisAttr(@AttrRes int resId) {
    return flexBasisAttr(resId, 0);
  }

  @Override
  public OptimizedLayoutAttributes flexBasisRes(@DimenRes int resId) {
    return flexBasisPx(mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public OptimizedLayoutAttributes flexBasisDip(@Dimension(unit = DP) float flexBasis) {
    return flexBasisPx(mResourceResolver.dipsToPixels(flexBasis));
  }

  @Override
  public OptimizedLayoutAttributes importantForAccessibility(int importantForAccessibility) {
    getOrCreateSparseArray().put(IMPORTANT_FOR_ACCESSIBILITY, importantForAccessibility);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes duplicateParentState(boolean duplicateParentState) {
    getOrCreateSparseArray().put(DUPLICATE_PARENT_STATE, duplicateParentState);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes marginPx(YogaEdge edge, @Px int margin) {
    YogaEdgesWithInts margins = (YogaEdgesWithInts) getOrCreateSparseArray().get(MARGIN);

    if (margins == null) {
      margins = new YogaEdgesWithInts();
      getOrCreateSparseArray().put(MARGIN, margins);
    }

    margins.add(edge, margin);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes marginPercent(YogaEdge edge, float percent) {
    YogaEdgesWithFloats marginPercents =
        (YogaEdgesWithFloats) getOrCreateSparseArray().get(MARGIN_PERCENT);

    if (marginPercents == null) {
      marginPercents = new YogaEdgesWithFloats();
      getOrCreateSparseArray().put(MARGIN_PERCENT, marginPercents);
    }

    marginPercents.add(edge, percent);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes marginAuto(YogaEdge edge) {
    List<YogaEdge> marginAutos = (List<YogaEdge>) getOrCreateSparseArray().get(MARGIN_AUTO);

    if (marginAutos == null) {
      marginAutos = new ArrayList<>(2);
      getOrCreateSparseArray().put(MARGIN_AUTO, marginAutos);
    }

    marginAutos.add(edge);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes marginAttr(
      YogaEdge edge, @AttrRes int resId, @DimenRes int defaultResId) {
    return marginPx(edge, mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public OptimizedLayoutAttributes marginAttr(YogaEdge edge, @AttrRes int resId) {
    return marginAttr(edge, resId, 0);
  }

  @Override
  public OptimizedLayoutAttributes marginRes(YogaEdge edge, @DimenRes int resId) {
    return marginPx(edge, mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public OptimizedLayoutAttributes marginDip(YogaEdge edge, @Dimension(unit = DP) float margin) {
    return marginPx(edge, mResourceResolver.dipsToPixels(margin));
  }

  @Override
  public OptimizedLayoutAttributes paddingPx(YogaEdge edge, @Px int padding) {
    YogaEdgesWithInts paddings = (YogaEdgesWithInts) getOrCreateSparseArray().get(PADDING);

    if (paddings == null) {
      paddings = new YogaEdgesWithInts();
      getOrCreateSparseArray().put(PADDING, paddings);
    }

    paddings.add(edge, padding);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes paddingPercent(YogaEdge edge, float percent) {
    YogaEdgesWithFloats paddingPercents =
        (YogaEdgesWithFloats) getOrCreateSparseArray().get(PADDING_PERCENT);

    if (paddingPercents == null) {
      paddingPercents = new YogaEdgesWithFloats();
      getOrCreateSparseArray().put(PADDING_PERCENT, paddingPercents);
    }

    paddingPercents.add(edge, percent);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes paddingAttr(
      YogaEdge edge, @AttrRes int resId, @DimenRes int defaultResId) {
    return paddingPx(edge, mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public OptimizedLayoutAttributes paddingAttr(YogaEdge edge, @AttrRes int resId) {
    return paddingAttr(edge, resId, 0);
  }

  @Override
  public OptimizedLayoutAttributes paddingRes(YogaEdge edge, @DimenRes int resId) {
    return paddingPx(edge, mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public OptimizedLayoutAttributes paddingDip(YogaEdge edge, @Dimension(unit = DP) float padding) {
    return paddingPx(edge, mResourceResolver.dipsToPixels(padding));
  }

  @Override
  public OptimizedLayoutAttributes border(Border border) {
    getOrCreateSparseArray().put(BORDER, border);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes borderWidthPx(YogaEdge edge, @Px int borderWidth) {
    YogaEdgesWithInts borderWidths = (YogaEdgesWithInts) getOrCreateSparseArray().get(BORDER_WIDTH);

    if (borderWidths == null) {
      borderWidths = new YogaEdgesWithInts();
      getOrCreateSparseArray().put(BORDER_WIDTH, borderWidths);
    }

    borderWidths.add(edge, borderWidth);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes borderWidthAttr(
      YogaEdge edge, @AttrRes int resId, @DimenRes int defaultResId) {
    return borderWidthPx(edge, mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public OptimizedLayoutAttributes borderWidthAttr(YogaEdge edge, @AttrRes int resId) {
    return borderWidthAttr(edge, resId, 0);
  }

  @Override
  public OptimizedLayoutAttributes borderWidthRes(YogaEdge edge, @DimenRes int resId) {
    return borderWidthPx(edge, mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public OptimizedLayoutAttributes borderWidthDip(
      YogaEdge edge, @Dimension(unit = DP) float borderWidth) {
    return borderWidthPx(edge, mResourceResolver.dipsToPixels(borderWidth));
  }

  @Override
  public OptimizedLayoutAttributes borderColor(@ColorInt int borderColor) {
    getOrCreateSparseArray().put(BORDER_COLOR, borderColor);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes positionPercent(YogaEdge edge, float percent) {
    YogaEdgesWithFloats positionPercents =
        (YogaEdgesWithFloats) getOrCreateSparseArray().get(POSITION_PERCENT);

    if (positionPercents == null) {
      positionPercents = new YogaEdgesWithFloats();
      getOrCreateSparseArray().put(POSITION_PERCENT, positionPercents);
    }

    positionPercents.add(edge, percent);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes positionAttr(
      YogaEdge edge, @AttrRes int resId, @DimenRes int defaultResId) {
    return positionPx(edge, mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public OptimizedLayoutAttributes positionAttr(YogaEdge edge, @AttrRes int resId) {
    return positionAttr(edge, resId, 0);
  }

  @Override
  public OptimizedLayoutAttributes positionRes(YogaEdge edge, @DimenRes int resId) {
    return positionPx(edge, mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public OptimizedLayoutAttributes positionDip(
      YogaEdge edge, @Dimension(unit = DP) float position) {
    return positionPx(edge, mResourceResolver.dipsToPixels(position));
  }

  @Override
  public OptimizedLayoutAttributes widthPercent(float percent) {
    getOrCreateSparseArray().put(WIDTH_PERCENT, percent);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes widthRes(@DimenRes int resId) {
    return widthPx(mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public OptimizedLayoutAttributes widthAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return widthPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public OptimizedLayoutAttributes widthAttr(@AttrRes int resId) {
    return widthAttr(resId, 0);
  }

  @Override
  public OptimizedLayoutAttributes widthDip(@Dimension(unit = DP) float width) {
    return widthPx(mResourceResolver.dipsToPixels(width));
  }

  @Override
  public OptimizedLayoutAttributes minWidthPx(@Px int minWidth) {
    getOrCreateSparseArray().put(MIN_WIDTH, minWidth);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes minWidthPercent(float percent) {
    getOrCreateSparseArray().put(MIN_WIDTH_PERCENT, percent);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes minWidthAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return minWidthPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public OptimizedLayoutAttributes minWidthAttr(@AttrRes int resId) {
    return minWidthAttr(resId, 0);
  }

  @Override
  public OptimizedLayoutAttributes minWidthRes(@DimenRes int resId) {
    return minWidthPx(mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public OptimizedLayoutAttributes minWidthDip(@Dimension(unit = DP) float minWidth) {
    return minWidthPx(mResourceResolver.dipsToPixels(minWidth));
  }

  @Override
  public OptimizedLayoutAttributes maxWidthPx(@Px int maxWidth) {
    getOrCreateSparseArray().put(MAX_WIDTH, maxWidth);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes maxWidthPercent(float percent) {
    getOrCreateSparseArray().put(MAX_WIDTH_PERCENT, percent);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes maxWidthAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return maxWidthPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public OptimizedLayoutAttributes maxWidthAttr(@AttrRes int resId) {
    return maxWidthAttr(resId, 0);
  }

  @Override
  public OptimizedLayoutAttributes maxWidthRes(@DimenRes int resId) {
    return maxWidthPx(mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public OptimizedLayoutAttributes maxWidthDip(@Dimension(unit = DP) float maxWidth) {
    return maxWidthPx(mResourceResolver.dipsToPixels(maxWidth));
  }

  @Override
  public OptimizedLayoutAttributes heightPercent(float percent) {
    getOrCreateSparseArray().put(HEIGHT_PERCENT, percent);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes heightRes(@DimenRes int resId) {
    return heightPx(mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public OptimizedLayoutAttributes heightAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return heightPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public OptimizedLayoutAttributes heightAttr(@AttrRes int resId) {
    return heightAttr(resId, 0);
  }

  @Override
  public OptimizedLayoutAttributes heightDip(@Dimension(unit = DP) float height) {
    return heightPx(mResourceResolver.dipsToPixels(height));
  }

  @Override
  public OptimizedLayoutAttributes minHeightPx(@Px int minHeight) {
    getOrCreateSparseArray().put(MIN_HEIGHT, minHeight);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes minHeightPercent(float percent) {
    getOrCreateSparseArray().put(MIN_HEIGHT_PERCENT, percent);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes minHeightAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return minHeightPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public OptimizedLayoutAttributes minHeightAttr(@AttrRes int resId) {
    return minHeightAttr(resId, 0);
  }

  @Override
  public OptimizedLayoutAttributes minHeightRes(@DimenRes int resId) {
    return minHeightPx(mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public OptimizedLayoutAttributes minHeightDip(@Dimension(unit = DP) float minHeight) {
    return minHeightPx(mResourceResolver.dipsToPixels(minHeight));
  }

  @Override
  public OptimizedLayoutAttributes maxHeightPx(@Px int maxHeight) {
    getOrCreateSparseArray().put(MAX_HEIGHT, maxHeight);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes maxHeightPercent(float percent) {
    getOrCreateSparseArray().put(MAX_HEIGHT_PERCENT, percent);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes maxHeightAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return maxHeightPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public OptimizedLayoutAttributes maxHeightAttr(@AttrRes int resId) {
    return maxHeightAttr(resId, 0);
  }

  @Override
  public OptimizedLayoutAttributes maxHeightRes(@DimenRes int resId) {
    return maxHeightPx(mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public OptimizedLayoutAttributes maxHeightDip(@Dimension(unit = DP) float maxHeight) {
    return maxHeightPx(mResourceResolver.dipsToPixels(maxHeight));
  }

  @Override
  public OptimizedLayoutAttributes aspectRatio(float aspectRatio) {
    getOrCreateSparseArray().put(ASPECT_RATIO, aspectRatio);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes touchExpansionPx(YogaEdge edge, @Px int touchExpansion) {
    YogaEdgesWithInts touchExpansions =
        (YogaEdgesWithInts) getOrCreateSparseArray().get(TOUCH_EXPANSION);

    if (touchExpansions == null) {
      touchExpansions = new YogaEdgesWithInts();
      getOrCreateSparseArray().put(TOUCH_EXPANSION, touchExpansions);
    }

    touchExpansions.add(edge, touchExpansion);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes touchExpansionAttr(
      YogaEdge edge, @AttrRes int resId, @DimenRes int defaultResId) {
    return touchExpansionPx(edge, mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public OptimizedLayoutAttributes touchExpansionAttr(YogaEdge edge, @AttrRes int resId) {
    return touchExpansionAttr(edge, resId, 0);
  }

  @Override
  public OptimizedLayoutAttributes touchExpansionRes(YogaEdge edge, @DimenRes int resId) {
    return touchExpansionPx(edge, mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public OptimizedLayoutAttributes touchExpansionDip(
      YogaEdge edge, @Dimension(unit = DP) float touchExpansion) {
    return touchExpansionPx(edge, mResourceResolver.dipsToPixels(touchExpansion));
  }

  @Override
  public OptimizedLayoutAttributes background(Reference.Builder<? extends Drawable> builder) {
    return background(builder.build());
  }

  @Override
  public OptimizedLayoutAttributes background(Drawable background) {
    return background(DrawableReference.create().drawable(background));
  }

  @Override
  public OptimizedLayoutAttributes backgroundAttr(
      @AttrRes int resId, @DrawableRes int defaultResId) {
    return backgroundRes(mResourceResolver.resolveResIdAttr(resId, defaultResId));
  }

  @Override
  public OptimizedLayoutAttributes backgroundAttr(@AttrRes int resId) {
    return backgroundAttr(resId, 0);
  }

  @Override
  public OptimizedLayoutAttributes backgroundRes(@DrawableRes int resId) {
    if (resId == 0) {
      return background((Reference<? extends Drawable>) null);
    }

    return background(mComponentContext.getResources().getDrawable(resId));
  }

  @Override
  public OptimizedLayoutAttributes backgroundColor(@ColorInt int backgroundColor) {
    return background(new ColorDrawable(backgroundColor));
  }

  @Override
  public OptimizedLayoutAttributes foreground(Drawable foreground) {
    getOrCreateSparseArray().put(FOREGROUND, foreground);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes foregroundAttr(
      @AttrRes int resId, @DrawableRes int defaultResId) {
    return foregroundRes(mResourceResolver.resolveResIdAttr(resId, defaultResId));
  }

  @Override
  public OptimizedLayoutAttributes foregroundAttr(@AttrRes int resId) {
    return foregroundAttr(resId, 0);
  }

  @Override
  public OptimizedLayoutAttributes foregroundRes(@DrawableRes int resId) {
    if (resId == 0) {
      return foreground(null);
    }

    return foreground(mComponentContext.getResources().getDrawable(resId));
  }

  @Override
  public OptimizedLayoutAttributes foregroundColor(@ColorInt int foregroundColor) {
    return foreground(new ColorDrawable(foregroundColor));
  }

  @Override
  public OptimizedLayoutAttributes wrapInView() {
    getOrCreateSparseArray().put(WRAP_IN_VIEW, true);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes clickHandler(EventHandler<ClickEvent> clickHandler) {
    getOrCreateNodeInfo().setClickHandler(clickHandler);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes longClickHandler(EventHandler<LongClickEvent> longClickHandler) {
    getOrCreateNodeInfo().setLongClickHandler(longClickHandler);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes focusChangeHandler(
      EventHandler<FocusChangedEvent> focusChangeHandler) {
    getOrCreateNodeInfo().setFocusChangeHandler(focusChangeHandler);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes touchHandler(EventHandler<TouchEvent> touchHandler) {
    getOrCreateNodeInfo().setTouchHandler(touchHandler);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes interceptTouchHandler(
      EventHandler<InterceptTouchEvent> interceptTouchHandler) {
    getOrCreateNodeInfo().setInterceptTouchHandler(interceptTouchHandler);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes focusable(boolean isFocusable) {
    getOrCreateNodeInfo().setFocusable(isFocusable);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes enabled(boolean isEnabled) {
    getOrCreateNodeInfo().setEnabled(isEnabled);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes visibleHeightRatio(float visibleHeightRatio) {
    getOrCreateSparseArray().put(VISIBLE_HEIGHT_RATIO, visibleHeightRatio);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes visibleWidthRatio(float visibleWidthRatio) {
    getOrCreateSparseArray().put(VISIBLE_WIDTH_RATIO, visibleWidthRatio);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes visibleHandler(EventHandler<VisibleEvent> visibleHandler) {
    getOrCreateSparseArray().put(VISIBLE_HANDLER, visibleHandler);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes focusedHandler(
      EventHandler<FocusedVisibleEvent> focusedHandler) {
    getOrCreateSparseArray().put(FOCUSED_HANDLER, focusedHandler);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes unfocusedHandler(
      EventHandler<UnfocusedVisibleEvent> unfocusedHandler) {
    getOrCreateSparseArray().put(UNFOCUSED_HANDLER, unfocusedHandler);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes fullImpressionHandler(
      EventHandler<FullImpressionVisibleEvent> fullImpressionHandler) {
    getOrCreateSparseArray().put(FULL_IMPRESSION_HANDLER, fullImpressionHandler);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes invisibleHandler(EventHandler<InvisibleEvent> invisibleHandler) {
    getOrCreateSparseArray().put(INVISIBLE_HANDLER, invisibleHandler);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes contentDescription(CharSequence contentDescription) {
    getOrCreateNodeInfo().setContentDescription(contentDescription);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes contentDescription(@StringRes int stringId) {
    return contentDescription(mComponentContext.getResources().getString(stringId));
  }

  @Override
  public OptimizedLayoutAttributes contentDescription(
      @StringRes int stringId, Object... formatArgs) {
    return contentDescription(mComponentContext.getResources().getString(stringId, formatArgs));
  }

  @Override
  public OptimizedLayoutAttributes viewTag(Object viewTag) {
    getOrCreateNodeInfo().setViewTag(viewTag);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes viewTags(SparseArray<Object> viewTags) {
    getOrCreateNodeInfo().setViewTags(viewTags);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes shadowElevationPx(float shadowElevation) {
    getOrCreateNodeInfo().setShadowElevation(shadowElevation);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes shadowElevationAttr(
      @AttrRes int resId, @DimenRes int defaultResId) {
    return shadowElevationPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public OptimizedLayoutAttributes shadowElevationAttr(@AttrRes int resId) {
    return shadowElevationAttr(resId, 0);
  }

  @Override
  public OptimizedLayoutAttributes shadowElevationRes(@DimenRes int resId) {
    return shadowElevationPx(mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public OptimizedLayoutAttributes shadowElevationDip(@Dimension(unit = DP) float shadowElevation) {
    return shadowElevationPx(mResourceResolver.dipsToPixels(shadowElevation));
  }

  @Override
  public OptimizedLayoutAttributes outlineProvider(ViewOutlineProvider outlineProvider) {
    getOrCreateNodeInfo().setOutlineProvider(outlineProvider);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes clipToOutline(boolean clipToOutline) {
    getOrCreateNodeInfo().setClipToOutline(clipToOutline);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes dispatchPopulateAccessibilityEventHandler(
      EventHandler<DispatchPopulateAccessibilityEventEvent>
          dispatchPopulateAccessibilityEventHandler) {
    getOrCreateNodeInfo()
        .setDispatchPopulateAccessibilityEventHandler(dispatchPopulateAccessibilityEventHandler);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes onInitializeAccessibilityEventHandler(
      EventHandler<OnInitializeAccessibilityEventEvent> onInitializeAccessibilityEventHandler) {
    getOrCreateNodeInfo()
        .setOnInitializeAccessibilityEventHandler(onInitializeAccessibilityEventHandler);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes onInitializeAccessibilityNodeInfoHandler(
      EventHandler<OnInitializeAccessibilityNodeInfoEvent>
          onInitializeAccessibilityNodeInfoHandler) {
    getOrCreateNodeInfo()
        .setOnInitializeAccessibilityNodeInfoHandler(onInitializeAccessibilityNodeInfoHandler);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes onPopulateAccessibilityEventHandler(
      EventHandler<OnPopulateAccessibilityEventEvent> onPopulateAccessibilityEventHandler) {
    getOrCreateNodeInfo()
        .setOnPopulateAccessibilityEventHandler(onPopulateAccessibilityEventHandler);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes onRequestSendAccessibilityEventHandler(
      EventHandler<OnRequestSendAccessibilityEventEvent> onRequestSendAccessibilityEventHandler) {
    getOrCreateNodeInfo()
        .setOnRequestSendAccessibilityEventHandler(onRequestSendAccessibilityEventHandler);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes performAccessibilityActionHandler(
      EventHandler<PerformAccessibilityActionEvent> performAccessibilityActionHandler) {
    getOrCreateNodeInfo().setPerformAccessibilityActionHandler(performAccessibilityActionHandler);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes sendAccessibilityEventHandler(
      EventHandler<SendAccessibilityEventEvent> sendAccessibilityEventHandler) {
    getOrCreateNodeInfo().setSendAccessibilityEventHandler(sendAccessibilityEventHandler);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes sendAccessibilityEventUncheckedHandler(
      EventHandler<SendAccessibilityEventUncheckedEvent> sendAccessibilityEventUncheckedHandler) {
    getOrCreateNodeInfo()
        .setSendAccessibilityEventUncheckedHandler(sendAccessibilityEventUncheckedHandler);
    return this;
  }

  @Override
  public ComponentLayout.Builder scale(float scale) {
    getOrCreateNodeInfo().setScale(scale);
    return this;
  }

  @Override
  public ComponentLayout.Builder alpha(float alpha) {
    getOrCreateNodeInfo().setAlpha(alpha);
    return this;
  }

  @Override
  public OptimizedLayoutAttributes transitionKey(String key) {
    getOrCreateSparseArray().put(TRANSITION_KEY, key);
    return this;
  }

  @Override
  public ComponentLayout build() {
    final ComponentLayout.Builder nodeToCopyInto = mNodeToCopyInto;
    mNodeToCopyInto = null;
    copyInto(nodeToCopyInto);

    return nodeToCopyInto.build();
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

    if ((mPrivateFlags & PFLAG_BACKGROUND_IS_SET) != 0L) {
      node.background(mBackground);
    }
    if ((mPrivateFlags & PFLAG_TEST_KEY_IS_SET) != 0L) {
      node.testKey(mTestKey);
    }
    if ((mPrivateFlags & PFLAG_POSITION_TYPE_IS_SET) != 0L) {
      node.positionType(mPositionType);
    }
    if ((mPrivateFlags & PFLAG_POSITION_IS_SET) != 0L) {
      for (int i = 0; i < mPositions.mNumEntries; i++) {
        node.positionPx(mPositions.mEdges[i], mPositions.mValues[i]);
      }
    }
    if ((mPrivateFlags & PFLAG_FLEX_SHRINK_IS_SET) != 0L) {
      node.flexShrink(mFlexShrink);
    }
    if ((mPrivateFlags & PFLAG_WIDTH_IS_SET) != 0L) {
      node.widthPx(mWidthPx);
    }
    if ((mPrivateFlags & PFLAG_HEIGHT_IS_SET) != 0L) {
      node.heightPx(mHeightPx);
    }

    if (mSparseArray == null) {
      return;
    }

    for (int i = 0, size = mSparseArray.size(); i < size; i++) {
      int key = getOrCreateSparseArray().keyAt(i);

      switch (key) {
        case LAYOUT_DIRECTION:
          node.layoutDirection((YogaDirection) getOrCreateSparseArray().get(key));
          break;

        case IMPORTANT_FOR_ACCESSIBILITY:
          node.importantForAccessibility((Integer) getOrCreateSparseArray().get(key));
          break;

        case DUPLICATE_PARENT_STATE:
          node.duplicateParentState((Boolean) getOrCreateSparseArray().get(key));
          break;

        case FOREGROUND:
          node.foreground((Drawable) getOrCreateSparseArray().get(key));
          break;

        case WRAP_IN_VIEW:
          node.wrapInView();
          break;

        case VISIBLE_HANDLER:
          node.visibleHandler((EventHandler<VisibleEvent>) getOrCreateSparseArray().get(key));
          break;

        case FOCUSED_HANDLER:
          node.focusedHandler(
              (EventHandler<FocusedVisibleEvent>) getOrCreateSparseArray().get(key));
          break;

        case FULL_IMPRESSION_HANDLER:
          node.fullImpressionHandler(
              (EventHandler<FullImpressionVisibleEvent>) getOrCreateSparseArray().get(key));
          break;

        case INVISIBLE_HANDLER:
          node.invisibleHandler((EventHandler<InvisibleEvent>) getOrCreateSparseArray().get(key));
          break;

        case UNFOCUSED_HANDLER:
          node.unfocusedHandler(
              (EventHandler<UnfocusedVisibleEvent>) getOrCreateSparseArray().get(key));
          break;

        case TRANSITION_KEY:
          node.transitionKey((String) getOrCreateSparseArray().get(key));
          break;

        case VISIBLE_HEIGHT_RATIO:
          node.visibleHeightRatio((Float) getOrCreateSparseArray().get(key));
          break;

        case VISIBLE_WIDTH_RATIO:
          node.visibleWidthRatio((Float) getOrCreateSparseArray().get(key));
          break;

        case ALIGN_SELF:
          node.alignSelf((YogaAlign) getOrCreateSparseArray().get(key));
          break;

        case FLEX:
          node.flex((Float) getOrCreateSparseArray().get(key));
          break;

        case FLEX_GROW:
          node.flexGrow((Float) getOrCreateSparseArray().get(key));
          break;

        case FLEX_BASIS:
          node.flexBasisPx((Integer) getOrCreateSparseArray().get(key));
          break;

        case FLEX_BASIS_PERCENT:
          node.flexBasisPercent((Float) getOrCreateSparseArray().get(key));
          break;

        case WIDTH_PERCENT:
          node.widthPercent((Float) getOrCreateSparseArray().get(key));
          break;

        case MIN_WIDTH:
          node.minWidthPx((Integer) getOrCreateSparseArray().get(key));
          break;

        case MIN_WIDTH_PERCENT:
          node.minWidthPercent((Float) getOrCreateSparseArray().get(key));
          break;

        case MAX_WIDTH:
          node.maxWidthPx((Integer) getOrCreateSparseArray().get(key));
          break;

        case MAX_WIDTH_PERCENT:
          node.maxWidthPercent((Float) getOrCreateSparseArray().get(key));
          break;

        case HEIGHT_PERCENT:
          node.heightPercent((Float) getOrCreateSparseArray().get(key));
          break;

        case MIN_HEIGHT:
          node.minHeightPx((Integer) getOrCreateSparseArray().get(key));
          break;

        case MIN_HEIGHT_PERCENT:
          node.minHeightPercent((Float) getOrCreateSparseArray().get(key));
          break;

        case MAX_HEIGHT:
          node.maxHeightPx((Integer) getOrCreateSparseArray().get(key));
          break;

        case MAX_HEIGHT_PERCENT:
          node.maxHeightPercent((Float) getOrCreateSparseArray().get(key));
          break;

        case ASPECT_RATIO:
          node.aspectRatio((Float) getOrCreateSparseArray().get(key));
          break;

        case BORDER:
          node.border((Border) getOrCreateSparseArray().get(key));
          break;

        case BORDER_COLOR:
          node.borderColor((Integer) getOrCreateSparseArray().get(key));
          break;

        case BORDER_WIDTH:
          YogaEdgesWithInts borderWidths = (YogaEdgesWithInts) getOrCreateSparseArray().get(key);
          for (int j = 0; j < borderWidths.mNumEntries; j++) {
            node.borderWidthPx(borderWidths.mEdges[j], borderWidths.mValues[j]);
          }
          break;

        case POSITION_PERCENT:
          YogaEdgesWithFloats positionPercents =
              (YogaEdgesWithFloats) getOrCreateSparseArray().get(key);
          for (int j = 0; j < positionPercents.mNumEntries; j++) {
            node.positionPercent(positionPercents.mEdges[j], positionPercents.mValues[j]);
          }
          break;

        case MARGIN:
          YogaEdgesWithInts margins = (YogaEdgesWithInts) getOrCreateSparseArray().get(key);
          for (int j = 0; j < margins.mNumEntries; j++) {
            node.marginPx(margins.mEdges[j], margins.mValues[j]);
          }
          break;

        case MARGIN_PERCENT:
          YogaEdgesWithFloats marginPercents =
              (YogaEdgesWithFloats) getOrCreateSparseArray().get(key);
          for (int j = 0; j < marginPercents.mNumEntries; j++) {
            node.marginPercent(marginPercents.mEdges[j], marginPercents.mValues[j]);
          }
          break;

        case MARGIN_AUTO:
          List<YogaEdge> marginAutos = (List<YogaEdge>) getOrCreateSparseArray().get(key);
          for (YogaEdge edge : marginAutos) {
            node.marginAuto(edge);
          }
          break;

        case PADDING:
          YogaEdgesWithInts paddings = (YogaEdgesWithInts) getOrCreateSparseArray().get(key);
          for (int j = 0; j < paddings.mNumEntries; j++) {
            node.paddingPx(paddings.mEdges[j], paddings.mValues[j]);
          }
          break;

        case PADDING_PERCENT:
          YogaEdgesWithFloats paddingPercents =
              (YogaEdgesWithFloats) getOrCreateSparseArray().get(key);
          for (int j = 0; j < paddingPercents.mNumEntries; j++) {
            node.paddingPercent(paddingPercents.mEdges[j], paddingPercents.mValues[j]);
          }
          break;

        case TOUCH_EXPANSION:
          YogaEdgesWithInts mTouchExpansions =
              (YogaEdgesWithInts) getOrCreateSparseArray().get(key);
          for (int j = 0; j < mTouchExpansions.mNumEntries; j++) {
            node.touchExpansionPx(mTouchExpansions.mEdges[j], mTouchExpansions.mValues[j]);
          }
          break;
      }
    }
  }

  static class YogaEdgesWithInts {
    YogaEdge[] mEdges = new YogaEdge[2];
    int[] mValues = new int[2];
    int mNumEntries;
    int mSize = 2;

    void add(YogaEdge yogaEdge, int value) {
      if (mNumEntries == mSize) {
        increaseSize();
      }

      mEdges[mNumEntries] = yogaEdge;
      mValues[mNumEntries] = value;
      mNumEntries++;
    }

    private void increaseSize() {
      YogaEdge[] oldEdges = mEdges;
      int[] oldValues = mValues;

      mSize *= 2;
      mEdges = new YogaEdge[mSize];
      mValues = new int[mSize];

      System.arraycopy(oldEdges, 0, mEdges, 0, mNumEntries);
      System.arraycopy(oldValues, 0, mValues, 0, mNumEntries);
    }
  }

  private static class YogaEdgesWithFloats {
    private YogaEdge[] mEdges = new YogaEdge[2];
    private float[] mValues = new float[2];
    private int mNumEntries;
    private int mSize = 2;

    private void add(YogaEdge yogaEdge, float value) {
      if (mNumEntries == mSize) {
        increaseSize();
      }

      mEdges[mNumEntries] = yogaEdge;
      mValues[mNumEntries] = value;
      mNumEntries++;
    }

    private void increaseSize() {
      YogaEdge[] oldEdges = mEdges;
      float[] oldValues = mValues;

      mSize *= 2;
      mEdges = new YogaEdge[mSize];
      mValues = new float[mSize];

      System.arraycopy(oldEdges, 0, mEdges, 0, mNumEntries);
      System.arraycopy(oldValues, 0, mValues, 0, mNumEntries);
    }
  }
}
