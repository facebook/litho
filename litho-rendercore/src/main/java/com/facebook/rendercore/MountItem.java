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
