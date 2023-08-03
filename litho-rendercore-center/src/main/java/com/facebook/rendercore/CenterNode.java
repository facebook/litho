// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.rendercore;

import android.view.View;
import androidx.annotation.Nullable;

public class CenterNode implements Node {

  private Node mChildNode;

  @Override
  public LayoutResult calculateLayout(LayoutContext context, int widthSpec, int heightSpec) {

    final LayoutResult child = mChildNode.calculateLayout(context, widthSpec, heightSpec);

    final int childWidht = child.getWidth();
    final int childHeight = child.getHeight();

    int width = findSizeForChild(widthSpec, childWidht);
    int height = findSizeForChild(heightSpec, childHeight);

    return new CenterLayoutResult(child, widthSpec, heightSpec, width, height, this);
  }

  private static int findSizeForChild(int sizeSpec, int childSize) {
    switch (View.MeasureSpec.getMode(sizeSpec)) {
      case View.MeasureSpec.UNSPECIFIED:
        return childSize;
      default:
        return View.MeasureSpec.getSize(sizeSpec);
    }
  }

  public void setChildNode(Node childNode) {
    mChildNode = childNode;
  }

  private class CenterLayoutResult implements LayoutResult {

    final int mWidthSpec;
    final int mHeightSpec;
    final int mWidth;
    final int mHeight;
    final Node mNode;
    final LayoutResult mChildLayoutResult;
    private final int mChildX;
    private final int mChildY;

    public CenterLayoutResult(
        LayoutResult child, int widthSpec, int heightSpec, int width, int height, Node node) {
      mWidthSpec = widthSpec;
      mHeightSpec = heightSpec;
      mHeight = height;
      mWidth = width;
      mNode = node;
      mChildX = (width - child.getWidth()) / 2;
      mChildY = (height - child.getHeight()) / 2;
      mChildLayoutResult = child;
    }

    @Override
    public RenderUnit getRenderUnit() {
      return null;
    }

    @Nullable
    @Override
    public Object getLayoutData() {
      return null;
    }

    @Override
    public int getChildrenCount() {
      return 1;
    }

    @Override
    public LayoutResult getChildAt(int index) {
      if (index > 0) {
        throw new IndexOutOfBoundsException(index + " ");
      }

      return mChildLayoutResult;
    }

    @Override
    public int getXForChildAtIndex(int index) {
      if (index > 0) {
        throw new IndexOutOfBoundsException(index + " ");
      }

      return mChildX;
    }

    @Override
    public int getYForChildAtIndex(int index) {
      if (index > 0) {
        throw new IndexOutOfBoundsException(index + " ");
      }

      return mChildY;
    }

    @Override
    public int getWidth() {
      return mWidth;
    }

    @Override
    public int getHeight() {
      return mHeight;
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
}
