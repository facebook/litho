// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.rendercore;

import android.content.Context;
import androidx.annotation.Nullable;
import java.util.Map;

class MountItem {

  private Object mContent;
  private @Nullable Host mHost;
  private boolean mBound;
  private RenderTreeNode mRenderTreeNode;
  private Map<?, ?> mLayoutContexts;

  MountItem(
      RenderTreeNode renderTreeNode,
      @Nullable Host host,
      Object content,
      Map<?, ?> layoutContexts) {
    mRenderTreeNode = renderTreeNode;
    mHost = host;
    mContent = content;
    mLayoutContexts = layoutContexts;
  }

  Object getContent() {
    return mContent;
  }

  boolean isBound() {
    return mBound;
  }

  @Nullable
  RenderUnit getRenderUnit() {
    return mRenderTreeNode.getRenderUnit();
  }

  void setContent(Object content) {
    mContent = content;
  }

  void setIsBound(boolean bound) {
    mBound = bound;
  }

  public @Nullable Host getHost() {
    return mHost;
  }

  void update(RenderTreeNode renderTreeNode, Map layoutContexts) {
    mRenderTreeNode = renderTreeNode;
    mLayoutContexts = layoutContexts;
  }

  public void releaseMountContent(Context context) {
    MountItemsPool.release(context, getRenderUnit(), mContent);
  }

  public @Nullable RenderTreeNode getRenderTreeNode() {
    return mRenderTreeNode;
  }

  public Map getLayoutContexts() {
    return mLayoutContexts;
  }
}
