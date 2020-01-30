// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.rendercore;

import android.graphics.Rect;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class RenderTreeNode {

  private static final int DEFAULT_SIZE = 4;

  private List<RenderTreeNode> mChildren;
  @Nullable private final RenderTreeNode mParent;
  private final RenderUnit mRenderUnit;
  private final Rect mBounds;
  @Nullable private final Rect mResolvedPadding;
  final int mPositionInParent;

  public RenderTreeNode(
      @Nullable RenderTreeNode parent,
      RenderUnit renderUnit,
      Rect bounds,
      @Nullable Rect resolvedPadding,
      int positionInParent) {
    mParent = parent;
    mRenderUnit = renderUnit;
    mBounds = bounds;
    mResolvedPadding = resolvedPadding;
    mPositionInParent = positionInParent;
  }

  void child(RenderTreeNode renderTreeNode) {
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

  public int getPositionInParent() {
    return mPositionInParent;
  }
}
