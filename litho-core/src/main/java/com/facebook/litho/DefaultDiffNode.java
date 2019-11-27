/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import java.util.ArrayList;
import java.util.List;

public class DefaultDiffNode implements DiffNode {

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
  private final List<DiffNode> mChildren;

  DefaultDiffNode() {
    mChildren = new ArrayList<>(4);
  }

  @Override
  public int getChildCount() {
    return mChildren.size();
  }

  @Override
  public DiffNode getChildAt(int i) {
    return mChildren.get(i);
  }

  @Override
  public Component getComponent() {
    return mComponent;
  }

  @Override
  public void setComponent(Component component) {
    mComponent = component;
  }

  @Override
  public float getLastMeasuredWidth() {
    return mLastMeasuredWidth;
  }

  @Override
  public void setLastMeasuredWidth(float lastMeasuredWidth) {
    mLastMeasuredWidth = lastMeasuredWidth;
  }

  @Override
  public float getLastMeasuredHeight() {
    return mLastMeasuredHeight;
  }

  @Override
  public void setLastMeasuredHeight(float lastMeasuredHeight) {
    mLastMeasuredHeight = lastMeasuredHeight;
  }

  @Override
  public int getLastWidthSpec() {
    return mLastWidthSpec;
  }

  @Override
  public int getLastHeightSpec() {
    return mLastHeightSpec;
  }

  @Override
  public void setLastWidthSpec(int widthSpec) {
    mLastWidthSpec = widthSpec;
  }

  @Override
  public void setLastHeightSpec(int heightSpec) {
    mLastHeightSpec = heightSpec;
  }

  @Override
  public List<DiffNode> getChildren() {
    return mChildren;
  }

  @Override
  public void addChild(DiffNode node) {
    mChildren.add(node);
  }

  @Override
  public LayoutOutput getContent() {
    return mContent;
  }

  @Override
  public void setContent(LayoutOutput content) {
    mContent = content;
  }

  @Override
  public VisibilityOutput getVisibilityOutput() {
    return mVisibilityOutput;
  }

  @Override
  public void setVisibilityOutput(VisibilityOutput visibilityOutput) {
    mVisibilityOutput = visibilityOutput;
  }

  @Override
  public LayoutOutput getBackground() {
    return mBackground;
  }

  @Override
  public void setBackground(LayoutOutput background) {
    mBackground = background;
  }

  @Override
  public LayoutOutput getForeground() {
    return mForeground;
  }

  @Override
  public void setForeground(LayoutOutput foreground) {
    mForeground = foreground;
  }

  @Override
  public LayoutOutput getBorder() {
    return mBorder;
  }

  @Override
  public void setBorder(LayoutOutput border) {
    mBorder = border;
  }

  @Override
  public LayoutOutput getHost() {
    return mHost;
  }

  @Override
  public void setHost(LayoutOutput host) {
    mHost = host;
  }
}
