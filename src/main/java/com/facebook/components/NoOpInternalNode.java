/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

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

import com.facebook.components.reference.Reference;
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
