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
  private final @Nullable Rect mResolvedPadding;

  final int mPositionInParent;

  private List<RenderTreeNode> mChildren;

  public RenderTreeNode(
      @Nullable RenderTreeNode parent,
      RenderUnit renderUnit,
      @Nullable Object layoutData,
      Rect bounds,
      @Nullable Rect resolvedPadding,
      int positionInParent) {
    mParent = parent;
    mRenderUnit = renderUnit;
    mLayoutData = layoutData;
    mBounds = bounds;
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
