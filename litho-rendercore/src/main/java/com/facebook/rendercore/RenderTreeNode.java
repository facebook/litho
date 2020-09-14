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

import android.graphics.Rect;
import androidx.annotation.Nullable;
import com.facebook.proguard.annotations.DoNotStrip;
import java.util.ArrayList;
import java.util.List;

@DoNotStrip
public class RenderTreeNode {

  private static final int DEFAULT_SIZE = 4;

  private final @Nullable RenderTreeNode mParent;
  private final RenderUnit mRenderUnit;
  private final @Nullable Object mLayoutData;
  private final Rect mBounds;
  private final int mAbsoluteX;
  private final int mAbsoluteY;
  private final @Nullable Rect mResolvedPadding;

  final int mPositionInParent;

  private List<RenderTreeNode> mChildren;

  public RenderTreeNode(
      final @Nullable RenderTreeNode parent,
      final RenderUnit renderUnit,
      final @Nullable Object layoutData,
      final Rect bounds,
      final @Nullable Rect resolvedPadding,
      final int positionInParent) {
    mParent = parent;
    mRenderUnit = renderUnit;
    mLayoutData = layoutData;
    mBounds = bounds;
    mAbsoluteX = parent != null ? parent.getAbsoluteX() + bounds.left : 0;
    mAbsoluteY = parent != null ? parent.getAbsoluteY() + bounds.top : 0;
    mResolvedPadding = resolvedPadding;
    mPositionInParent = positionInParent;
  }

  public void child(RenderTreeNode renderTreeNode) {
    if (mChildren == null) {
      mChildren = new ArrayList<>(DEFAULT_SIZE);
    }

    mChildren.add(renderTreeNode);
  }

  public Rect getBounds() {
    return mBounds;
  }

  int getAbsoluteX() {
    return mAbsoluteX;
  }

  int getAbsoluteY() {
    return mAbsoluteY;
  }

  /**
   * Sets the absolutes bounds of this render tree node in {@param outRect}; i.e. returns the bounds
   * of this render tree node within its {@link RootHost}.
   *
   * @param outRect the calculated absolute bounds.
   */
  public Rect getAbsoluteBounds(Rect outRect) {
    outRect.left = mAbsoluteX;
    outRect.top = mAbsoluteY;
    outRect.right = mAbsoluteX + mBounds.width();
    outRect.bottom = mAbsoluteY + mBounds.height();

    return outRect;
  }

  @Nullable
  public Rect getResolvedPadding() {
    return mResolvedPadding;
  }

  public RenderUnit getRenderUnit() {
    return mRenderUnit;
  }

  @Nullable
  public RenderTreeNode getParent() {
    return mParent;
  }

  public int getChildrenCount() {
    return mChildren != null ? mChildren.size() : 0;
  }

  public RenderTreeNode getChildAt(int idx) {
    return mChildren.get(idx);
  }

  public int getPositionInParent() {
    return mPositionInParent;
  }

  @Nullable
  public Object getLayoutData() {
    return mLayoutData;
  }
}
