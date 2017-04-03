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
    mLastMeasuredWidth = lastMeasuredWidth;
  }

  float getLastMeasuredHeight() {
    return mLastMeasuredHeight;
  }

  void setLastMeasuredHeight(float lastMeasuredHeight) {
    mLastMeasuredHeight = lastMeasuredHeight;
  }

  int getLastWidthSpec() {
    return mLastWidthSpec;
  }

  int getLastHeightSpec() {
    return mLastHeightSpec;
  }

  void setLastWidthSpec(int widthSpec) {
    mLastWidthSpec = widthSpec;
  }

  void setLastHeightSpec(int heightSpec) {
    mLastHeightSpec = heightSpec;
  }

  List<DiffNode> getChildren() {
    return mChildren;
  }

  void addChild(DiffNode node) {
    mChildren.add(node);
  }

  LayoutOutput getContent() {
    return mContent;
  }

  void setContent(LayoutOutput content) {
    mContent = content;
  }

  VisibilityOutput getVisibilityOutput() {
    return mVisibilityOutput;
  }

  void setVisibilityOutput(VisibilityOutput visibilityOutput) {
    mVisibilityOutput = visibilityOutput;
  }

  LayoutOutput getBackground() {
    return mBackground;
  }

  void setBackground(LayoutOutput background) {
    mBackground = background;
  }

  LayoutOutput getForeground() {
    return mForeground;
  }

  void setForeground(LayoutOutput foreground) {
    mForeground = foreground;
  }

  LayoutOutput getBorder() {
    return mBorder;
  }

  void setBorder(LayoutOutput border) {
    mBorder = border;
  }

  LayoutOutput getHost() {
    return mHost;
  }

  void setHost(LayoutOutput host) {
    mHost = host;
  }

  void release() {
    mComponent = null;

    mContent = null;
    mBackground = null;
    mForeground = null;
    mHost = null;
    mVisibilityOutput = null;

    mLastMeasuredWidth = UNSPECIFIED;
    mLastMeasuredHeight = UNSPECIFIED;
    mLastWidthSpec = UNSPECIFIED;
    mLastHeightSpec = UNSPECIFIED;

    for (int i = 0, size = mChildren.size(); i < size; i++) {
      ComponentsPools.release(mChildren.get(i));
    }
    mChildren.clear();
  }
}
