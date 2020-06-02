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
import com.facebook.rendercore.Node;
import com.facebook.rendercore.RenderUnit;
import java.util.ArrayList;
import java.util.List;

public class SimpleLayoutResult implements Node.LayoutResult {

  private final @Nullable RenderUnit mRenderUnit;
  private final int mX;
  private final int mY;
  private final int mWidth;
  private final int mHeight;
  private final List<Node.LayoutResult> mChildren;
  private final Object mLayoutData;

  public SimpleLayoutResult(
      @Nullable RenderUnit renderUnit, Object layoutData, int x, int y, int width, int height) {
    mRenderUnit = renderUnit;
    mX = x;
    mY = y;
    mWidth = width;
    mHeight = height;
    mChildren = new ArrayList<>();
    mLayoutData = layoutData;
  }

  public SimpleLayoutResult(RenderUnit renderUnit, int x, int y, int width, int height) {
    this(renderUnit, null, x, y, width, height);
  }

  @Override
  public @Nullable RenderUnit getRenderUnit() {
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
  public Node.LayoutResult getChildAt(int index) {
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
    return View.MeasureSpec.makeMeasureSpec(mWidth, View.MeasureSpec.EXACTLY);
  }

  @Override
  public int getHeightSpec() {
    return View.MeasureSpec.makeMeasureSpec(mHeight, View.MeasureSpec.EXACTLY);
  }

  public List<Node.LayoutResult> getChildren() {
    return mChildren;
  }
}
