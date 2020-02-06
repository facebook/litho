// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.rendercore;

import androidx.annotation.Nullable;

public class MeasureResult implements Node.LayoutResult {

  private final RenderUnit mRenderUnit;
  private final int mMeasuredWidth;
  private final int mMeasuredHeight;
  private final int mWidthSpec;
  private final int mHeightSpec;

  public MeasureResult(
      RenderUnit renderUnit, int widthSpec, int heightSpec, int measuredWidth, int measuredHeight) {
    mRenderUnit = renderUnit;
    mMeasuredWidth = measuredWidth;
    mMeasuredHeight = measuredHeight;
    mWidthSpec = widthSpec;
    mHeightSpec = heightSpec;
  }

  @Nullable
  @Override
  public RenderUnit getRenderUnit() {
    return mRenderUnit;
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
  public int getWidth() {
    return mMeasuredWidth;
  }

  @Override
  public int getHeight() {
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
