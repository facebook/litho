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

package com.facebook.rendercore;

import androidx.annotation.Nullable;

public class MeasureResult implements Node.LayoutResult {

  private final @Nullable RenderUnit<?> mRenderUnit;
  private final int mMeasuredWidth;
  private final int mMeasuredHeight;
  private final int mWidthSpec;
  private final int mHeightSpec;
  private final Object mLayoutData;

  public MeasureResult(
      @Nullable RenderUnit<?> renderUnit,
      int widthSpec,
      int heightSpec,
      int measuredWidth,
      int measuredHeight) {
    this(renderUnit, widthSpec, heightSpec, measuredWidth, measuredHeight, null);
  }

  public MeasureResult(
      @Nullable RenderUnit<?> renderUnit,
      int widthSpec,
      int heightSpec,
      int measuredWidth,
      int measuredHeight,
      Object layoutData) {
    mRenderUnit = renderUnit;
    mMeasuredWidth = measuredWidth;
    mMeasuredHeight = measuredHeight;
    mWidthSpec = widthSpec;
    mHeightSpec = heightSpec;
    mLayoutData = layoutData;
  }

  @Nullable
  @Override
  public final RenderUnit getRenderUnit() {
    return mRenderUnit;
  }

  @Nullable
  @Override
  public final Object getLayoutData() {
    return mLayoutData;
  }

  @Override
  public final int getChildrenCount() {
    return 0;
  }

  @Override
  public final Node.LayoutResult getChildAt(int index) {
    throw new IllegalArgumentException("A MeasureResult has no children");
  }

  @Override
  public final int getXForChildAtIndex(int index) {
    throw new IllegalArgumentException("A MeasureResult has no children");
  }

  @Override
  public final int getYForChildAtIndex(int index) {
    throw new IllegalArgumentException("A MeasureResult has no children");
  }

  @Override
  public final int getWidth() {
    return mMeasuredWidth;
  }

  @Override
  public final int getHeight() {
    return mMeasuredHeight;
  }

  @Override
  public int getPaddingTop() {
    return 0;
  }

  @Override
  public int getPaddingRight() {
    return 0;
  }

  @Override
  public int getPaddingBottom() {
    return 0;
  }

  @Override
  public int getPaddingLeft() {
    return 0;
  }

  @Override
  public int getWidthSpec() {
    return mWidthSpec;
  }

  @Override
  public int getHeightSpec() {
    return mHeightSpec;
  }
}
