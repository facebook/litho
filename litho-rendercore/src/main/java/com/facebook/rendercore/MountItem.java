/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import android.content.Context;
import androidx.annotation.Nullable;

public class MountItem {
  private Object mContent;
  private @Nullable Host mHost = null;
  private boolean mBound;
  private RenderTreeNode mRenderTreeNode;
  private Object mMountData;
  private final BindData mBindData = new BindData();

  public MountItem(RenderTreeNode renderTreeNode, Object content) {
    mRenderTreeNode = renderTreeNode;
    mContent = content;
  }

  public Object getContent() {
    return mContent;
  }

  public boolean isBound() {
    return mBound;
  }

  RenderUnit getRenderUnit() {
    return mRenderTreeNode.getRenderUnit();
  }

  public void setIsBound(boolean bound) {
    mBound = bound;
  }

  public @Nullable Host getHost() {
    return mHost;
  }

  public void setHost(@Nullable Host host) {
    this.mHost = host;
  }

  public void update(RenderTreeNode renderTreeNode) {
    mRenderTreeNode = renderTreeNode;
  }

  public void releaseMountContent(Context context) {
    MountItemsPool.release(context, getRenderUnit().getContentAllocator(), mContent);
  }

  public RenderTreeNode getRenderTreeNode() {
    return mRenderTreeNode;
  }

  /** @deprecated Use BindData API instead. */
  @Deprecated
  public Object getMountData() {
    return mMountData;
  }

  public BindData getBindData() {
    return mBindData;
  }

  /** @deprecated Use BindData API instead. */
  @Deprecated
  public void setMountData(Object mountData) {
    mMountData = mountData;
  }

  public static long getId(MountItem item) {
    return item.mRenderTreeNode.getRenderUnit().getId();
  }
}
