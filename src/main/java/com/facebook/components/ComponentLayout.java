/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.DimenRes;
import android.support.annotation.Dimension;
import android.support.annotation.DrawableRes;
import android.support.annotation.Px;
import android.support.annotation.StringRes;
import android.util.SparseArray;

import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaPositionType;
import com.facebook.yoga.YogaWrap;
import com.facebook.yoga.YogaEdge;

import com.facebook.litho.annotations.ImportantForAccessibility;
import com.facebook.litho.reference.Reference;

import static android.support.annotation.Dimension.DP;

/**
 * Represents a {@link Component}'s computed layout state. The computed bounds will be
 * used by the framework to define the size and position of the component's mounted
 * {@link android.view.View}s and {@link android.graphics.drawable.Drawable}s returned.
 * by {@link ComponentLifecycle#mount(ComponentContext, Object, Component)}.
 *
 * @see ComponentLifecycle#createLayout(ComponentContext, Component, boolean)
 * @see ComponentLifecycle#mount(ComponentContext, Object, Component)
 */
public interface ComponentLayout {

  @Px
  int getX();
  @Px
  int getY();

  @Px
  int getWidth();
  @Px
  int getHeight();

  @Px
  int getPaddingTop();
  @Px
  int getPaddingRight();
  @Px
  int getPaddingBottom();
  @Px
  int getPaddingLeft();

  YogaDirection getResolvedLayoutDirection();

  interface Builder {
    Builder layoutDirection(YogaDirection direction);
    Builder alignSelf(YogaAlign alignSelf);
    Builder positionType(YogaPositionType positionType);
    Builder flex(float flex);
    Builder flexGrow(float flexGrow);
    Builder flexShrink(float flexShrink);
    Builder flexBasisPx(@Px int flexBasis);
    Builder flexBasisAttr(@AttrRes int resId, @DimenRes int defaultResId);
    Builder flexBasisAttr(@AttrRes int resId);
    Builder flexBasisRes(@DimenRes int resId);
    Builder flexBasisDip(@Dimension(unit = DP) int flexBasis);
    Builder flexBasisPercent(float percent);

    Builder importantForAccessibility(@ImportantForAccessibility int importantForAccessibility);
    Builder duplicateParentState(boolean duplicateParentState);

    Builder marginPx(YogaEdge edge, @Px int margin);
    Builder marginAttr(
        YogaEdge edge,
        @AttrRes int resId,
        @DimenRes int defaultResId);
    Builder marginAttr(
        YogaEdge edge,
        @AttrRes int resId);
    Builder marginRes(YogaEdge edge, @DimenRes int resId);
    Builder marginDip(YogaEdge edge, @Dimension(unit = DP) int margin);
    Builder marginPercent(YogaEdge edge, float percent);
    Builder marginAuto(YogaEdge edge);

    Builder paddingPx(YogaEdge edge, @Px int padding);
    Builder paddingAttr(
        YogaEdge edge,
        @AttrRes int resId,
        @DimenRes int defaultResId);
    Builder paddingAttr(
        YogaEdge edge,
        @AttrRes int resId);
    Builder paddingRes(YogaEdge edge, @DimenRes int resId);
    Builder paddingDip(YogaEdge edge, @Dimension(unit = DP) int padding);
    Builder paddingPercent(YogaEdge edge, float percent);

    Builder borderWidthPx(YogaEdge edge, @Px int borderWidth);
    Builder borderWidthAttr(
        YogaEdge edge,
        @AttrRes int resId,
        @DimenRes int defaultResId);
    Builder borderWidthAttr(
        YogaEdge edge,
        @AttrRes int resId);
    Builder borderWidthRes(YogaEdge edge, @DimenRes int resId);
    Builder borderWidthDip(YogaEdge edge, @Dimension(unit = DP) int borderWidth);
    Builder borderColor(@ColorInt int borderColor);

    Builder positionPx(YogaEdge edge, @Px int value);
    Builder positionAttr(YogaEdge edge, @AttrRes int resId, @DimenRes int defaultResId);
    Builder positionAttr(YogaEdge edge, @AttrRes int resId);
    Builder positionRes(YogaEdge edge, @DimenRes int resId);
    Builder positionDip(YogaEdge edge, @Dimension(unit = DP) int value);
    Builder positionPercent(YogaEdge edge, float percent);

    Builder widthPx(@Px int width);
    Builder widthAttr(@AttrRes int resId, @DimenRes int defaultResId);
    Builder widthAttr(@AttrRes int resId);
    Builder widthRes(@DimenRes int resId);
    Builder widthDip(@Dimension(unit = DP) int width);
    Builder widthPercent(float percent);

    Builder minWidthPx(@Px int minWidth);
    Builder minWidthAttr(@AttrRes int resId, @DimenRes int defaultResId);
    Builder minWidthAttr(@AttrRes int resId);
    Builder minWidthRes(@DimenRes int resId);
    Builder minWidthDip(@Dimension(unit = DP) int minWidth);
    Builder minWidthPercent(float percent);

    Builder maxWidthPx(@Px int maxWidth);
    Builder maxWidthAttr(@AttrRes int resId, @DimenRes int defaultResId);
    Builder maxWidthAttr(@AttrRes int resId);
    Builder maxWidthRes(@DimenRes int resId);
    Builder maxWidthDip(@Dimension(unit = DP) int maxWidth);
    Builder maxWidthPercent(float percent);

    Builder heightPx(@Px int height);
    Builder heightAttr(@AttrRes int resId, @DimenRes int defaultResId);
    Builder heightAttr(@AttrRes int resId);
    Builder heightRes(@DimenRes int resId);
    Builder heightDip(@Dimension(unit = DP) int height);
    Builder heightPercent(float percent);

    Builder minHeightPx(@Px int minHeight);
    Builder minHeightAttr(@AttrRes int resId, @DimenRes int defaultResId);
    Builder minHeightAttr(@AttrRes int resId);
    Builder minHeightRes(@DimenRes int resId);
    Builder minHeightDip(@Dimension(unit = DP) int minHeight);
    Builder minHeightPercent(float percent);

    Builder maxHeightPx(@Px int maxHeight);
    Builder maxHeightAttr(@AttrRes int resId, @DimenRes int defaultResId);
    Builder maxHeightAttr(@AttrRes int resId);
    Builder maxHeightRes(@DimenRes int resId);
    Builder maxHeightDip(@Dimension(unit = DP) int maxHeight);
    Builder maxHeightPercent(float percent);

    Builder aspectRatio(float aspectRatio);

    Builder touchExpansionPx(YogaEdge edge, @Px int value);
    Builder touchExpansionAttr(
        YogaEdge edge,
        @AttrRes int resId,
        @DimenRes int defaultResId);
    Builder touchExpansionAttr(YogaEdge edge, @AttrRes int resId);
    Builder touchExpansionRes(YogaEdge edge, @DimenRes int resId);
    Builder touchExpansionDip(YogaEdge edge, @Dimension(unit = DP) int value);

    Builder background(Reference<? extends Drawable> background);
    Builder background(Reference.Builder<? extends Drawable> backgroundBuilder);
    Builder backgroundAttr(@AttrRes int resId, @DrawableRes int defaultResId);
    Builder backgroundAttr(@AttrRes int resId);
    Builder backgroundRes(@DrawableRes int resId);
    Builder backgroundColor(@ColorInt int backgroundColor);

