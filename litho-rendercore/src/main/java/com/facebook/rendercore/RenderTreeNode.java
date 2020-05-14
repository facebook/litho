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
import java.util.ArrayList;
import java.util.List;

public class RenderTreeNode {

  private static final int DEFAULT_SIZE = 4;

  private final @Nullable RenderTreeNode mParent;
  private final RenderUnit mRenderUnit;
  private final @Nullable Object mLayoutData;
  private final Rect mBounds;
  private final int mHostTranslationX;
  private final int mHostTranslationY;
  private final @Nullable Rect mResolvedPadding;

  final int mPositionInParent;

  private List<RenderTreeNode> mChildren;

  public RenderTreeNode(
      @Nullable RenderTreeNode parent,
      RenderUnit renderUnit,
      @Nullable Object layoutData,
      Rect bounds,
      int hostTranslationX,
      int hostTranslationY,
      @Nullable Rect resolvedPadding,
      int positionInParent) {
    mParent = parent;
    mRenderUnit = renderUnit;
    mLayoutData = layoutData;
    mBounds = bounds;
    mHostTranslationX = hostTranslationX;
    mHostTranslationY = hostTranslationY;
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

  public int getHostTranslationX() {
    return mHostTranslationX;
  }

  public int getHostTranslationY() {
    return mHostTranslationY;
  }

  /**
   * Sets the relative bounds of this render tree node in {@param outRect}; i.e. returns the bounds
   * of this render tree node within its host view. This method should be used during mounting
   * because {@link #mBounds} can be the absolute bounds.
   *
   * @param outRect the calculated relative bounds.
   */
  public void getMountBounds(Rect outRect) {
    outRect.left = mBounds.left - mHostTranslationX;
    outRect.top = mBounds.top - mHostTranslationY;
    outRect.right = mBounds.right - mHostTranslationX;
    outRect.bottom = mBounds.bottom - mHostTranslationY;
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
