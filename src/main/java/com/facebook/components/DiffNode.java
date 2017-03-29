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
import java.util.List;

/**
 * A lightweight representation of a layout node, used to cache measurements between two Layout tree
 * calculations.
 */
class DiffNode implements Cloneable {

  static final int UNSPECIFIED = -1;

  private LayoutOutput mContent;
  private LayoutOutput mBackground;
  private LayoutOutput mForeground;
  private LayoutOutput mBorder;
  private LayoutOutput mHost;
  private VisibilityOutput mVisibilityOutput;
  private Component mComponent;
  private float mLastMeasuredWidth;
  private float mLastMeasuredHeight;
  private int mLastWidthSpec;
  private int mLastHeightSpec;
  private List<DiffNode> mChildren;

  DiffNode() {
    mChildren = new ArrayList<>(4);
  }

  int getChildCount() {
    return mChildren == null ? 0 : mChildren.size();
  }

  DiffNode getChildAt(int i) {
    return mChildren.get(i);
  }

  Component getComponent() {
    return mComponent;
  }

  void setComponent(Component component) {
    mComponent = component;
  }

  float getLastMeasuredWidth() {
    return mLastMeasuredWidth;
  }

  void setLastMeasuredWidth(float lastMeasuredWidth) {
