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

package com.facebook.rendercore.testing;

import android.view.View;
import androidx.annotation.Nullable;
import com.facebook.rendercore.Node.LayoutResult;
import com.facebook.rendercore.RenderUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Create simple layout result hierarchies for writing tests. The layout resutl can we used with to
 * render with {@link RendercoreTestDriver}.
 *
 * <pre>
 *   SimpleLayoutResult.create()
 *     .x(0).y(0)
 *     .width(1920).height(1080)
 *     .child(
 *       SimpleLayoutResult.create()
 *         .x(10).y(10)
 *         .width(100).height(100)
 *         .renderUnit(new SimpleViewUnit(new View(), 42)
 *         .build()
 *     )
 *     .build()
 * </pre>
 */
public class SimpleLayoutResult implements LayoutResult {

  private final @Nullable RenderUnit<?> mRenderUnit;
  private final int mX;
  private final int mY;
  private final int mWidth;
  private final int mHeight;
  private final int paddingTop;
  private final int paddingRight;
  private final int paddingBottom;
  private final int paddingLeft;
  private final List<LayoutResult> mChildren;
  private final Object mLayoutData;

  public SimpleLayoutResult(
      int x,
      int y,
      int width,
      int height,
      int paddingTop,
      int paddingRight,
      int paddingBottom,
      int paddingLeft,
      List<LayoutResult> children,
      @Nullable RenderUnit<?> renderUnit,
      @Nullable Object layoutData) {
    mX = x;
    mY = y;
    mWidth = width;
    mHeight = height;
    this.paddingTop = paddingTop;
    this.paddingRight = paddingRight;
    this.paddingBottom = paddingBottom;
    this.paddingLeft = paddingLeft;
    mChildren = children;
    mRenderUnit = renderUnit;
    mLayoutData = layoutData;
  }

  public SimpleLayoutResult(
      @Nullable RenderUnit<?> renderUnit, Object layoutData, int x, int y, int width, int height) {
    this(x, y, width, height, 0, 0, 0, 0, new ArrayList<LayoutResult>(), renderUnit, layoutData);
  }

  public SimpleLayoutResult(RenderUnit<?> renderUnit, int x, int y, int width, int height) {
    this(renderUnit, null, x, y, width, height);
  }

  @Override
  public @Nullable RenderUnit<?> getRenderUnit() {
    return mRenderUnit;
  }

  @Nullable
  @Override
  public Object getLayoutData() {
    return mLayoutData;
  }

  @Override
  public int getChildrenCount() {
    return mChildren.size();
  }

  @Override
  public LayoutResult getChildAt(int index) {
    return mChildren.get(index);
  }

  @Override
  public int getXForChildAtIndex(int index) {
    return ((SimpleLayoutResult) mChildren.get(index)).mX;
  }

  @Override
  public int getYForChildAtIndex(int index) {
    return ((SimpleLayoutResult) mChildren.get(index)).mY;
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
    return paddingTop;
  }

  @Override
  public int getPaddingRight() {
    return paddingRight;
  }

  @Override
  public int getPaddingBottom() {
    return paddingBottom;
  }

  @Override
  public int getPaddingLeft() {
    return paddingLeft;
  }

  @Override
  public int getWidthSpec() {
    return View.MeasureSpec.makeMeasureSpec(mWidth, View.MeasureSpec.EXACTLY);
  }

  @Override
  public int getHeightSpec() {
    return View.MeasureSpec.makeMeasureSpec(mHeight, View.MeasureSpec.EXACTLY);
  }

  public static SimpleLayoutResult.Builder create() {
    return new Builder();
  }

  public List<LayoutResult> getChildren() {
    return mChildren;
  }

  public static class Builder {

    private int x;
    private int y;
    private int width;
    private int height;
    private int paddingTop;
    private int paddingRight;
    private int paddingBottom;
    private int paddingLeft;

    private List<LayoutResult> children = new ArrayList<>();

    private @Nullable RenderUnit<?> renderUnit;
    private @Nullable Object layoutData;

    public Builder x(int x) {
      this.x = x;
      return this;
    }

    public Builder y(int y) {
      this.y = y;
      return this;
    }

    public Builder width(int width) {
      this.width = width;
      return this;
    }

    public Builder height(int height) {
      this.height = height;
      return this;
    }

    public Builder padding(int t, int r, int b, int l) {
      this.paddingTop = t;
      this.paddingRight = r;
      this.paddingBottom = b;
      this.paddingLeft = l;
      return this;
    }

    public Builder child(LayoutResult child) {
      this.children.add(child);
      return this;
    }

    public Builder child(Builder builder) {
      this.children.add(builder.build());
      return this;
    }

    public Builder renderUnit(@Nullable RenderUnit<?> renderUnit) {
      this.renderUnit = renderUnit;
      return this;
    }

    public Builder layoutData(@Nullable Object layoutData) {
      this.layoutData = layoutData;
      return this;
    }

    public SimpleLayoutResult build() {
      return new SimpleLayoutResult(
          x,
          y,
          width,
          height,
          paddingTop,
          paddingRight,
          paddingBottom,
          paddingLeft,
          children,
          renderUnit,
          layoutData);
    }
  }
}
