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
class LayoutAttributes implements ComponentLayout.Builder {

  // Indices for entries into the SparseArray.
  private static final int LAYOUT_DIRECTION = 0;
  private static final int ALIGN_SELF = 1;
  private static final int POSITION_TYPE = 2;
  private static final int FLEX = 3;
  private static final int FLEX_GROW = 4;
  private static final int FLEX_SHRINK = 5;
  private static final int FLEX_BASIS = 6;
  private static final int IMPORTANT_FOR_ACCESSIBILITY = 7;
  private static final int DUPLICATE_PARENT_STATE = 8;
  private static final int MARGIN = 9;
  private static final int PADDING = 10;
  private static final int POSITION = 11;
  private static final int POSITION_PERCENT = 12;
  private static final int WIDTH = 13;
  private static final int MIN_WIDTH = 14;
  private static final int MAX_WIDTH = 15;
  private static final int HEIGHT = 16;
  private static final int MIN_HEIGHT = 17;
  private static final int MAX_HEIGHT = 18;
  private static final int BACKGROUND = 19;
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
  private static final int TEST_KEY = 34;
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

  private final SparseArray<Object> mSparseArray = new SparseArray<>();

  private final ResourceResolver mResourceResolver = new ResourceResolver();

  private ComponentContext mComponentContext;
  private NodeInfo mNodeInfo;
  private ComponentLayout.Builder mNodeToCopyInto;

  void init(ComponentContext componentContext, ComponentLayout.Builder layoutToCopyInto) {
    mComponentContext = componentContext;
    mNodeToCopyInto = layoutToCopyInto;
    mResourceResolver.init(mComponentContext, componentContext.getResourceCache());
  }

  @Override
  public LayoutAttributes layoutDirection(YogaDirection direction) {
    mSparseArray.put(LAYOUT_DIRECTION, direction);
    return this;
  }

  @Override
  public LayoutAttributes alignSelf(YogaAlign alignSelf) {
    mSparseArray.put(ALIGN_SELF, alignSelf);
    return this;
  }

  @Override
  public LayoutAttributes positionType(YogaPositionType positionType) {
    mSparseArray.put(POSITION_TYPE, positionType);
    return this;
  }

  @Override
  public LayoutAttributes flex(float flex) {
    mSparseArray.put(FLEX, flex);
    return this;
  }

  @Override
  public LayoutAttributes flexGrow(float flexGrow) {
    mSparseArray.put(FLEX_GROW, flexGrow);
    return this;
  }

  @Override
  public LayoutAttributes flexShrink(float flexShrink) {
    mSparseArray.put(FLEX_SHRINK, flexShrink);
    return this;
  }

  @Override
  public LayoutAttributes flexBasisPx(@Px int flexBasis) {
    mSparseArray.put(FLEX_BASIS, flexBasis);
    return this;
  }

  @Override
  public LayoutAttributes flexBasisPercent(float percent) {
    mSparseArray.put(FLEX_BASIS_PERCENT, percent);
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
  public LayoutAttributes flexBasisDip(@Dimension(unit = DP) float flexBasis) {
    return flexBasisPx(mResourceResolver.dipsToPixels(flexBasis));
  }

  @Override
  public LayoutAttributes importantForAccessibility(int importantForAccessibility) {
    mSparseArray.put(IMPORTANT_FOR_ACCESSIBILITY, importantForAccessibility);
    return this;
  }

  @Override
  public LayoutAttributes duplicateParentState(boolean duplicateParentState) {
    mSparseArray.put(DUPLICATE_PARENT_STATE, duplicateParentState);
    return this;
  }

  @Override
  public LayoutAttributes marginPx(YogaEdge edge, @Px int margin) {
    YogaEdgesWithInts margins = (YogaEdgesWithInts) mSparseArray.get(MARGIN);

    if (margins == null) {
      margins = new YogaEdgesWithInts();
      mSparseArray.put(MARGIN, margins);
    }

    margins.add(edge, margin);
    return this;
  }

  @Override
  public LayoutAttributes marginPercent(YogaEdge edge, float percent) {
    YogaEdgesWithFloats marginPercents = (YogaEdgesWithFloats) mSparseArray.get(MARGIN_PERCENT);

    if (marginPercents == null) {
      marginPercents = new YogaEdgesWithFloats();
      mSparseArray.put(MARGIN_PERCENT, marginPercents);
    }

    marginPercents.add(edge, percent);
    return this;
  }

  @Override
  public LayoutAttributes marginAuto(YogaEdge edge) {
    List<YogaEdge> marginAutos = (List<YogaEdge>) mSparseArray.get(MARGIN_AUTO);

    if (marginAutos == null) {
      marginAutos = new ArrayList<>(2);
      mSparseArray.put(MARGIN_AUTO, marginAutos);
    }

    marginAutos.add(edge);
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
  public LayoutAttributes marginDip(YogaEdge edge, @Dimension(unit = DP) float margin) {
    return marginPx(edge, mResourceResolver.dipsToPixels(margin));
  }

  @Override
  public LayoutAttributes paddingPx(YogaEdge edge, @Px int padding) {
    YogaEdgesWithInts paddings = (YogaEdgesWithInts) mSparseArray.get(PADDING);

    if (paddings == null) {
      paddings = new YogaEdgesWithInts();
      mSparseArray.put(PADDING, paddings);
    }

    paddings.add(edge, padding);
    return this;
  }

  @Override
  public LayoutAttributes paddingPercent(YogaEdge edge, float percent) {
    YogaEdgesWithFloats paddingPercents = (YogaEdgesWithFloats) mSparseArray.get(PADDING_PERCENT);

    if (paddingPercents == null) {
      paddingPercents = new YogaEdgesWithFloats();
      mSparseArray.put(PADDING_PERCENT, paddingPercents);
    }

    paddingPercents.add(edge, percent);
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
  public LayoutAttributes paddingDip(YogaEdge edge, @Dimension(unit = DP) float padding) {
    return paddingPx(edge, mResourceResolver.dipsToPixels(padding));
  }

  @Override
  public LayoutAttributes border(Border border) {
    mSparseArray.put(BORDER, border);
    return this;
  }

  @Override
  public LayoutAttributes borderWidthPx(YogaEdge edge, @Px int borderWidth) {
    YogaEdgesWithInts borderWidths = (YogaEdgesWithInts) mSparseArray.get(BORDER_WIDTH);

    if (borderWidths == null) {
      borderWidths = new YogaEdgesWithInts();
      mSparseArray.put(BORDER_WIDTH, borderWidths);
    }

    borderWidths.add(edge, borderWidth);
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
  public LayoutAttributes borderWidthDip(YogaEdge edge, @Dimension(unit = DP) float borderWidth) {
    return borderWidthPx(edge, mResourceResolver.dipsToPixels(borderWidth));
  }

  @Override
  public LayoutAttributes borderColor(@ColorInt int borderColor) {
    mSparseArray.put(BORDER_COLOR, borderColor);
    return this;
  }

  @Override
  public LayoutAttributes positionPx(YogaEdge edge, @Px int position) {
    YogaEdgesWithInts positions = (YogaEdgesWithInts) mSparseArray.get(POSITION);

    if (positions == null) {
      positions = new YogaEdgesWithInts();
      mSparseArray.put(POSITION, positions);
    }

    positions.add(edge, position);
    return this;
  }

  @Override
  public LayoutAttributes positionPercent(YogaEdge edge, float percent) {
    YogaEdgesWithFloats positionPercents = (YogaEdgesWithFloats) mSparseArray.get(POSITION_PERCENT);

    if (positionPercents == null) {
      positionPercents = new YogaEdgesWithFloats();
      mSparseArray.put(POSITION_PERCENT, positionPercents);
    }

    positionPercents.add(edge, percent);
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
  public LayoutAttributes positionDip(YogaEdge edge, @Dimension(unit = DP) float position) {
    return positionPx(edge, mResourceResolver.dipsToPixels(position));
  }

  @Override
  public LayoutAttributes widthPx(@Px int width) {
    mSparseArray.put(WIDTH, width);
    return this;
  }

  @Override
  public LayoutAttributes widthPercent(float percent) {
    mSparseArray.put(WIDTH_PERCENT, percent);
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
  public LayoutAttributes widthDip(@Dimension(unit = DP) float width) {
    return widthPx(mResourceResolver.dipsToPixels(width));
  }

  @Override
  public LayoutAttributes minWidthPx(@Px int minWidth) {
    mSparseArray.put(MIN_WIDTH, minWidth);
    return this;
  }

  @Override
  public LayoutAttributes minWidthPercent(float percent) {
    mSparseArray.put(MIN_WIDTH_PERCENT, percent);
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
  public LayoutAttributes minWidthDip(@Dimension(unit = DP) float minWidth) {
    return minWidthPx(mResourceResolver.dipsToPixels(minWidth));
  }

  @Override
  public LayoutAttributes maxWidthPx(@Px int maxWidth) {
    mSparseArray.put(MAX_WIDTH, maxWidth);
    return this;
  }

  @Override
  public LayoutAttributes maxWidthPercent(float percent) {
    mSparseArray.put(MAX_WIDTH_PERCENT, percent);
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
  public LayoutAttributes maxWidthDip(@Dimension(unit = DP) float maxWidth) {
    return maxWidthPx(mResourceResolver.dipsToPixels(maxWidth));
  }

  @Override
  public LayoutAttributes heightPx(@Px int height) {
    mSparseArray.put(HEIGHT, height);
    return this;
  }

  @Override
  public LayoutAttributes heightPercent(float percent) {
    mSparseArray.put(HEIGHT_PERCENT, percent);
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
  public LayoutAttributes heightDip(@Dimension(unit = DP) float height) {
    return heightPx(mResourceResolver.dipsToPixels(height));
  }

  @Override
  public LayoutAttributes minHeightPx(@Px int minHeight) {
    mSparseArray.put(MIN_HEIGHT, minHeight);
    return this;
  }

  @Override
  public LayoutAttributes minHeightPercent(float percent) {
    mSparseArray.put(MIN_HEIGHT_PERCENT, percent);
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
  public LayoutAttributes minHeightDip(@Dimension(unit = DP) float minHeight) {
    return minHeightPx(mResourceResolver.dipsToPixels(minHeight));
  }

  @Override
  public LayoutAttributes maxHeightPx(@Px int maxHeight) {
    mSparseArray.put(MAX_HEIGHT, maxHeight);
    return this;
  }

  @Override
  public LayoutAttributes maxHeightPercent(float percent) {
    mSparseArray.put(MAX_HEIGHT_PERCENT, percent);
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
  public LayoutAttributes maxHeightDip(@Dimension(unit = DP) float maxHeight) {
    return maxHeightPx(mResourceResolver.dipsToPixels(maxHeight));
  }

  @Override
  public LayoutAttributes aspectRatio(float aspectRatio) {
    mSparseArray.put(ASPECT_RATIO, aspectRatio);
    return this;
  }

  @Override
  public LayoutAttributes touchExpansionPx(YogaEdge edge, @Px int touchExpansion) {
    YogaEdgesWithInts touchExpansions = (YogaEdgesWithInts) mSparseArray.get(TOUCH_EXPANSION);

    if (touchExpansions == null) {
      touchExpansions = new YogaEdgesWithInts();
      mSparseArray.put(TOUCH_EXPANSION, touchExpansions);
    }

    touchExpansions.add(edge, touchExpansion);
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
      YogaEdge edge, @Dimension(unit = DP) float touchExpansion) {
    return touchExpansionPx(edge, mResourceResolver.dipsToPixels(touchExpansion));
  }

  @Override
  public LayoutAttributes background(Reference<? extends Drawable> background) {
    mSparseArray.put(BACKGROUND, background);
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
    mSparseArray.put(FOREGROUND, foreground);
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

    return foreground(mComponentContext.getResources().getDrawable(resId));
  }

  @Override
  public LayoutAttributes foregroundColor(@ColorInt int foregroundColor) {
    return foreground(new ColorDrawable(foregroundColor));
  }

  @Override
  public LayoutAttributes wrapInView() {
    mSparseArray.put(WRAP_IN_VIEW, true);
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
    mSparseArray.put(VISIBLE_HEIGHT_RATIO, visibleHeightRatio);
    return this;
  }

  @Override
  public LayoutAttributes visibleWidthRatio(float visibleWidthRatio) {
    mSparseArray.put(VISIBLE_WIDTH_RATIO, visibleWidthRatio);
    return this;
  }

  @Override
  public LayoutAttributes visibleHandler(EventHandler<VisibleEvent> visibleHandler) {
    mSparseArray.put(VISIBLE_HANDLER, visibleHandler);
    return this;
  }

  @Override
  public LayoutAttributes focusedHandler(EventHandler<FocusedVisibleEvent> focusedHandler) {
    mSparseArray.put(FOCUSED_HANDLER, focusedHandler);
    return this;
  }

  @Override
  public LayoutAttributes unfocusedHandler(EventHandler<UnfocusedVisibleEvent> unfocusedHandler) {
    mSparseArray.put(UNFOCUSED_HANDLER, unfocusedHandler);
    return this;
  }

  @Override
  public LayoutAttributes fullImpressionHandler(
      EventHandler<FullImpressionVisibleEvent> fullImpressionHandler) {
    mSparseArray.put(FULL_IMPRESSION_HANDLER, fullImpressionHandler);
    return this;
  }

  @Override
  public LayoutAttributes invisibleHandler(EventHandler<InvisibleEvent> invisibleHandler) {
    mSparseArray.put(INVISIBLE_HANDLER, invisibleHandler);
    return this;
  }

  @Override
  public LayoutAttributes contentDescription(CharSequence contentDescription) {
    getOrCreateNodeInfo().setContentDescription(contentDescription);
    return this;
  }

  @Override
  public LayoutAttributes contentDescription(@StringRes int stringId) {
    return contentDescription(mComponentContext.getResources().getString(stringId));
  }

  @Override
  public LayoutAttributes contentDescription(@StringRes int stringId, Object... formatArgs) {
    return contentDescription(mComponentContext.getResources().getString(stringId, formatArgs));
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
  public LayoutAttributes shadowElevationDip(@Dimension(unit = DP) float shadowElevation) {
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
    mSparseArray.put(TEST_KEY, testKey);
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
  public ComponentLayout.Builder scale(float scale) {
    getOrCreateNodeInfo().setScale(scale);
    return this;
  }

  @Override
  public LayoutAttributes transitionKey(String key) {
    mSparseArray.put(TRANSITION_KEY, key);
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

    for (int i = 0, size = mSparseArray.size(); i < size; i++) {
      int key = mSparseArray.keyAt(i);

      switch (key) {
        case LAYOUT_DIRECTION:
          node.layoutDirection((YogaDirection) mSparseArray.get(key));
          break;

        case IMPORTANT_FOR_ACCESSIBILITY:
          node.importantForAccessibility((Integer) mSparseArray.get(key));
          break;

        case DUPLICATE_PARENT_STATE:
          node.duplicateParentState((Boolean) mSparseArray.get(key));
          break;

        case BACKGROUND:
          node.background((Reference<? extends Drawable>) mSparseArray.get(key));
          break;

        case FOREGROUND:
          node.foreground((Drawable) mSparseArray.get(key));
          break;

        case WRAP_IN_VIEW:
          node.wrapInView();
          break;

        case VISIBLE_HANDLER:
          node.visibleHandler((EventHandler<VisibleEvent>) mSparseArray.get(key));
          break;

        case FOCUSED_HANDLER:
          node.focusedHandler((EventHandler<FocusedVisibleEvent>) mSparseArray.get(key));
          break;

        case FULL_IMPRESSION_HANDLER:
          node.fullImpressionHandler(
              (EventHandler<FullImpressionVisibleEvent>) mSparseArray.get(key));
          break;

        case INVISIBLE_HANDLER:
          node.invisibleHandler((EventHandler<InvisibleEvent>) mSparseArray.get(key));
          break;

        case UNFOCUSED_HANDLER:
          node.unfocusedHandler((EventHandler<UnfocusedVisibleEvent>) mSparseArray.get(key));
          break;

        case TEST_KEY:
          node.testKey((String) mSparseArray.get(key));
          break;

        case TRANSITION_KEY:
          node.transitionKey((String) mSparseArray.get(key));
          break;

        case VISIBLE_HEIGHT_RATIO:
          node.visibleHeightRatio((Float) mSparseArray.get(key));
          break;

        case VISIBLE_WIDTH_RATIO:
          node.visibleWidthRatio((Float) mSparseArray.get(key));
          break;

        case ALIGN_SELF:
          node.alignSelf((YogaAlign) mSparseArray.get(key));
          break;

        case POSITION_TYPE:
          node.positionType((YogaPositionType) mSparseArray.get(key));
          break;

        case FLEX:
          node.flex((Float) mSparseArray.get(key));
          break;

        case FLEX_GROW:
          node.flexGrow((Float) mSparseArray.get(key));
          break;

        case FLEX_SHRINK:
          node.flexShrink((Float) mSparseArray.get(key));
          break;

        case FLEX_BASIS:
          node.flexBasisPx((Integer) mSparseArray.get(key));
          break;

        case FLEX_BASIS_PERCENT:
          node.flexBasisPercent((Float) mSparseArray.get(key));
          break;

        case WIDTH:
          node.widthPx((Integer) mSparseArray.get(key));
          break;

        case WIDTH_PERCENT:
          node.widthPercent((Float) mSparseArray.get(key));
          break;

        case MIN_WIDTH:
          node.minWidthPx((Integer) mSparseArray.get(key));
          break;

        case MIN_WIDTH_PERCENT:
          node.minWidthPercent((Float) mSparseArray.get(key));
          break;

        case MAX_WIDTH:
          node.maxWidthPx((Integer) mSparseArray.get(key));
          break;

        case MAX_WIDTH_PERCENT:
          node.maxWidthPercent((Float) mSparseArray.get(key));
          break;

        case HEIGHT:
          node.heightPx((Integer) mSparseArray.get(key));
          break;

        case HEIGHT_PERCENT:
          node.heightPercent((Float) mSparseArray.get(key));
          break;

        case MIN_HEIGHT:
          node.minHeightPx((Integer) mSparseArray.get(key));
          break;

        case MIN_HEIGHT_PERCENT:
          node.minHeightPercent((Float) mSparseArray.get(key));
          break;

        case MAX_HEIGHT:
          node.maxHeightPx((Integer) mSparseArray.get(key));
          break;

        case MAX_HEIGHT_PERCENT:
          node.maxHeightPercent((Float) mSparseArray.get(key));
          break;

        case ASPECT_RATIO:
          node.aspectRatio((Float) mSparseArray.get(key));
          break;

        case BORDER:
          node.border((Border) mSparseArray.get(key));
          break;

        case BORDER_COLOR:
          node.borderColor((Integer) mSparseArray.get(key));
          break;

        case BORDER_WIDTH:
          YogaEdgesWithInts borderWidths = (YogaEdgesWithInts) mSparseArray.get(key);
          for (int j = 0; j < borderWidths.mNumEntries; j++) {
            node.borderWidthPx(borderWidths.mEdges[j], borderWidths.mValues[j]);
          }
          break;

        case POSITION:
          YogaEdgesWithInts positions = (YogaEdgesWithInts) mSparseArray.get(key);
          for (int j = 0; j < positions.mNumEntries; j++) {
            node.positionPx(positions.mEdges[j], positions.mValues[j]);
          }
          break;

        case POSITION_PERCENT:
          YogaEdgesWithFloats positionPercents = (YogaEdgesWithFloats) mSparseArray.get(key);
          for (int j = 0; j < positionPercents.mNumEntries; j++) {
            node.positionPercent(positionPercents.mEdges[j], positionPercents.mValues[j]);
          }
          break;

        case MARGIN:
          YogaEdgesWithInts margins = (YogaEdgesWithInts) mSparseArray.get(key);
          for (int j = 0; j < margins.mNumEntries; j++) {
            node.marginPx(margins.mEdges[j], margins.mValues[j]);
          }
          break;

        case MARGIN_PERCENT:
          YogaEdgesWithFloats marginPercents = (YogaEdgesWithFloats) mSparseArray.get(key);
          for (int j = 0; j < marginPercents.mNumEntries; j++) {
            node.marginPercent(marginPercents.mEdges[j], marginPercents.mValues[j]);
          }
          break;

        case MARGIN_AUTO:
          List<YogaEdge> marginAutos = (List<YogaEdge>) mSparseArray.get(key);
          for (YogaEdge edge : marginAutos) {
            node.marginAuto(edge);
          }
          break;

        case PADDING:
          YogaEdgesWithInts paddings = (YogaEdgesWithInts) mSparseArray.get(key);
          for (int j = 0; j < paddings.mNumEntries; j++) {
            node.paddingPx(paddings.mEdges[j], paddings.mValues[j]);
          }
          break;

        case PADDING_PERCENT:
          YogaEdgesWithFloats paddingPercents = (YogaEdgesWithFloats) mSparseArray.get(key);
          for (int j = 0; j < paddingPercents.mNumEntries; j++) {
            node.paddingPercent(paddingPercents.mEdges[j], paddingPercents.mValues[j]);
          }
          break;

        case TOUCH_EXPANSION:
          YogaEdgesWithInts mTouchExpansions = (YogaEdgesWithInts) mSparseArray.get(key);
          for (int j = 0; j < mTouchExpansions.mNumEntries; j++) {
            node.touchExpansionPx(mTouchExpansions.mEdges[j], mTouchExpansions.mValues[j]);
          }
          break;
      }
    }
  }

  private static class YogaEdgesWithInts {
    private YogaEdge[] mEdges = new YogaEdge[2];
    private int[] mValues = new int[2];
    private int mNumEntries;
    private int mSize = 2;

    private void add(YogaEdge yogaEdge, int value) {
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
