/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.content.res.Resources;
import android.content.res.TypedArray;
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
import com.facebook.yoga.YogaBaselineFunction;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaPositionType;
import com.facebook.yoga.YogaWrap;
import com.facebook.yoga.YogaEdge;

import com.facebook.litho.reference.Reference;
import com.facebook.yoga.YogaNodeAPI;

import static android.support.annotation.Dimension.DP;

/**
 * Class representing an empty InternalNode with a null ComponentLayout. All methods
 * have been overridden so no actions are performed, and no exceptions are thrown.
 */
class NoOpInternalNode extends InternalNode {

  @Override
  void init(YogaNodeAPI cssNode, ComponentContext componentContext, Resources resources) {}

  @Override
  void setComponent(Component component) {

  }

  @Px
  @Override
  public int getX() {
    return 0;
  }

  @Px
  @Override
  public int getY() {
    return 0;
  }

  @Px
  @Override
  public int getWidth() {
    return 0;
  }

  @Px
  @Override
  public int getHeight() {
    return 0;
  }

  @Px
  @Override
  public int getPaddingLeft() {
    return 0;
  }

  @Px
  @Override
  public int getPaddingTop() {
    return 0;
  }

  @Px
  @Override
  public int getPaddingRight() {
    return 0;
  }

  @Px
  @Override
  public int getPaddingBottom() {
    return 0;
  }

  @Override
  public void setCachedMeasuresValid(boolean valid) {}

  @Override
  public int getLastWidthSpec() {
    return 0;
  }

  @Override
  public void setLastWidthSpec(int widthSpec) {}

  @Override
  public int getLastHeightSpec() {
    return 0;
  }

  @Override
  public void setLastHeightSpec(int heightSpec) {}

  @Override
  void setLastMeasuredWidth(float lastMeasuredWidth) {}

  @Override
  void setLastMeasuredHeight(float lastMeasuredHeight) {}

  @Override
  void setDiffNode(DiffNode diffNode) {}

  @Override
  public InternalNode layoutDirection(YogaDirection direction) {
    return this;
  }

  @Override
  public InternalNode flexDirection(YogaFlexDirection direction) {
    return this;
  }

  @Override
  public InternalNode wrap(YogaWrap wrap) {
    return this;
  }

  @Override
  public InternalNode justifyContent(YogaJustify justifyContent) {
    return this;
  }

  @Override
  public InternalNode alignItems(YogaAlign alignItems) {
    return this;
  }

  @Override
  public InternalNode alignContent(YogaAlign alignContent) {
    return this;
  }

  @Override
  public InternalNode alignSelf(YogaAlign alignSelf) {
    return this;
  }

  @Override
  public InternalNode positionType(YogaPositionType positionType) {
    return this;
  }

  @Override
  public InternalNode flex(float flex) {
    return this;
  }

  @Override
  public InternalNode flexGrow(float flexGrow) {
    return this;
  }

  @Override
  public InternalNode flexShrink(float flexShrink) {
    return this;
  }

  @Override
  public InternalNode flexBasisPx(@Px int flexBasis) {
    return this;
  }

  @Override
  public InternalNode flexBasisAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return this;
  }

  @Override
  public InternalNode flexBasisAttr(@AttrRes int resId) {
    return this;
  }

  @Override
  public InternalNode flexBasisRes(@DimenRes int resId) {
    return this;
  }

  @Override
  public InternalNode flexBasisDip(@Dimension(unit = DP) int flexBasis) {
    return this;
  }

  @Override
  public InternalNode flexBasisPercent(float percent) {
    return this;
  }

  @Override
  public InternalNode importantForAccessibility(int importantForAccessibility) {
    return this;
  }

  @Override
  public InternalNode duplicateParentState(boolean duplicateParentState) {
    return this;
  }

  @Override
  public InternalNode marginPx(YogaEdge edge, @Px int margin) {
    return this;
  }

  @Override
  public InternalNode marginAttr(
      YogaEdge edge,
      @AttrRes int resId,
      @DimenRes int defaultResId) {
    return this;
  }

  @Override
  public InternalNode marginAttr(YogaEdge edge, @AttrRes int resId) {
    return this;
  }

  @Override
  public InternalNode marginRes(YogaEdge edge, @DimenRes int resId) {
    return this;
  }

  @Override
  public InternalNode marginDip(YogaEdge edge, @Dimension(unit = DP) int margin) {
    return this;
  }

  @Override
  public InternalNode marginPercent(YogaEdge edge, float percent) {
    return this;
  }

  @Override
  public InternalNode marginAuto(YogaEdge edge) {
    return this;
  }

  @Override
  public InternalNode paddingPx(YogaEdge edge, @Px int padding) {
    return this;
  }

  @Override
  public InternalNode paddingAttr(
      YogaEdge edge,
      @AttrRes int resId,
      @DimenRes int defaultResId) {
    return this;
  }

  @Override
  public InternalNode paddingAttr(YogaEdge edge, @AttrRes int resId) {
    return this;
  }

  @Override
  public InternalNode paddingRes(YogaEdge edge,@DimenRes int resId) {
    return this;
  }

  @Override
  public InternalNode paddingDip(YogaEdge edge, @Dimension(unit = DP) int padding) {
    return this;
  }

  @Override
  public InternalNode paddingPercent(YogaEdge edge, float percent) {
    return this;
  }

  @Override
  public InternalNode borderWidthPx(YogaEdge edge, @Px int borderWidth) {
    return this;
  }

  @Override
  public InternalNode borderWidthAttr(
      YogaEdge edge,
      @AttrRes int resId,
      @DimenRes int defaultResId) {
    return this;
  }

  @Override
  public InternalNode borderWidthAttr(YogaEdge edge, @AttrRes int resId) {
    return this;
  }

  @Override
  public InternalNode borderWidthRes(YogaEdge edge,@DimenRes int resId) {
    return this;
  }

  @Override
  public InternalNode borderWidthDip(
      YogaEdge edge,
      @Dimension(unit = DP) int borderWidth) {
    return this;
  }

  @Override
  public InternalNode positionPx(YogaEdge edge, @Px int position) {
    return this;
  }

  @Override
  public InternalNode positionAttr(
      YogaEdge edge,
      @AttrRes int resId,
      @DimenRes int defaultResId) {
    return this;
  }

  @Override
  public InternalNode positionRes(YogaEdge edge, @DimenRes int resId) {
    return this;
  }

  @Override
  public InternalNode positionDip(
      YogaEdge edge,
      @Dimension(unit = DP) int position) {
    return this;
  }

  @Override
  public InternalNode positionPercent(YogaEdge edge, float percent) {
    return this;
  }

  @Override
  public InternalNode widthPx(@Px int width) {
    return this;
  }

  @Override
  public InternalNode widthRes(@DimenRes int resId) {
    return this;
  }

  @Override
  public InternalNode widthAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return this;
  }

  @Override
  public InternalNode widthAttr(@AttrRes int resId) {
    return this;
  }

  @Override
  public InternalNode widthDip(@Dimension(unit = DP) int width) {
    return this;
  }

  @Override
  public InternalNode widthPercent(float percent) {
    return this;
  }

  @Override
  public InternalNode minWidthPx(@Px int minWidth) {
    return this;
  }

  @Override
  public InternalNode minWidthAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return this;
  }

  @Override
  public InternalNode minWidthAttr(@AttrRes int resId) {
    return this;
  }

  @Override
  public InternalNode minWidthRes(@DimenRes int resId) {
    return this;
  }

  @Override
  public InternalNode minWidthDip(@Dimension(unit = DP) int minWidth) {
    return this;
  }

  @Override
  public InternalNode minWidthPercent(float percent) {
    return this;
  }

  @Override
  public InternalNode maxWidthPx(@Px int maxWidth) {
    return this;
  }

  @Override
  public InternalNode maxWidthAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return this;
  }

  @Override
  public InternalNode maxWidthAttr(@AttrRes int resId) {
    return this;
  }

  @Override
  public InternalNode maxWidthRes(@DimenRes int resId) {
    return this;
  }

  @Override
  public InternalNode maxWidthDip(@Dimension(unit = DP) int maxWidth) {
    return this;
  }

  @Override
  public InternalNode maxWidthPercent(float percent) {
    return this;
  }

  @Override
  public InternalNode heightPx(@Px int height) {
