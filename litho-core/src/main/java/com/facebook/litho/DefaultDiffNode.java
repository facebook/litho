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

import androidx.annotation.Nullable;
import com.facebook.rendercore.visibility.VisibilityOutput;
import java.util.ArrayList;
import java.util.List;

public class DefaultDiffNode implements DiffNode {

  private @Nullable LithoRenderUnit mContent;
  private @Nullable LithoRenderUnit mBackground;
  private @Nullable LithoRenderUnit mForeground;
  private @Nullable LithoRenderUnit mBorder;
  private @Nullable LithoRenderUnit mHost;
  private @Nullable VisibilityOutput mVisibilityOutput;
  private @Nullable Component mComponent;
  private float mLastMeasuredWidth;
  private float mLastMeasuredHeight;
  private int mLastWidthSpec;
  private int mLastHeightSpec;
  private final List<DiffNode> mChildren = new ArrayList<>(4);
  private String mGlobalKey;

  /** package private constructor */
  DefaultDiffNode() {}

  @Override
  public int getChildCount() {
    return mChildren.size();
  }

  @Override
  public @Nullable DiffNode getChildAt(int i) {
    return mChildren.get(i);
  }

  @Override
  public @Nullable Component getComponent() {
    return mComponent;
  }

  @Override
  public @Nullable String getComponentGlobalKey() {
    return mGlobalKey;
  }

  @Override
  public void setComponent(@Nullable Component component, @Nullable String globalKey) {
    mComponent = component;
    mGlobalKey = globalKey;
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
  public void setLastWidthSpec(int widthSpec) {
    mLastWidthSpec = widthSpec;
  }

  @Override
  public int getLastHeightSpec() {
    return mLastHeightSpec;
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
  public @Nullable LithoRenderUnit getContentOutput() {
    return mContent;
  }

  @Override
  public void setContentOutput(@Nullable LithoRenderUnit content) {
    mContent = content;
  }

  @Override
  public @Nullable VisibilityOutput getVisibilityOutput() {
    return mVisibilityOutput;
  }

  @Override
  public void setVisibilityOutput(@Nullable VisibilityOutput visibilityOutput) {
    mVisibilityOutput = visibilityOutput;
  }

  @Override
  public @Nullable LithoRenderUnit getBackgroundOutput() {
    return mBackground;
  }

  @Override
  public void setBackgroundOutput(@Nullable LithoRenderUnit background) {
    mBackground = background;
  }

  @Override
  public @Nullable LithoRenderUnit getForegroundOutput() {
    return mForeground;
  }

  @Override
  public void setForegroundOutput(@Nullable LithoRenderUnit foreground) {
    mForeground = foreground;
  }

  @Override
  public @Nullable LithoRenderUnit getBorderOutput() {
    return mBorder;
  }

  @Override
  public void setBorderOutput(@Nullable LithoRenderUnit border) {
    mBorder = border;
  }

  @Override
  public @Nullable LithoRenderUnit getHostOutput() {
    return mHost;
  }

  @Override
  public void setHostOutput(@Nullable LithoRenderUnit host) {
    mHost = host;
  }
}
