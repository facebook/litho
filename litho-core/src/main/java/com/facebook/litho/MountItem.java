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

package com.facebook.litho;

import android.graphics.drawable.Drawable;
import com.facebook.rendercore.RenderTreeNode;

/**
 * Represents a mounted UI element in a {@link MountState}. It holds a key and a content instance
 * which might be any type of UI element supported by the framework e.g. {@link Drawable}.
 */
class MountItem {

  private final Object mContent;

  private ComponentHost mHost;
  private RenderTreeNode mRenderTreeNode;
  private boolean mIsBound;

  private final LithoMountData mMountData;

  MountItem(ComponentHost host, Object content, RenderTreeNode node) {
    mContent = content;
    mHost = host;
    mRenderTreeNode = node;
    mMountData = new LithoMountData(content);
  }

  /**
   * Call this method when assigning a new {@link RenderTreeNode} to an existing MountItem. In this
   * case we don't want to update mMountViewFlags since those flags are only used to determine the
   * initial state of the view content, which we will have already done in init(). If it is done
   * again now some of the values may be wrong (e.g. the Litho framework may add a click listener to
   * a view that was not originally clickable.
   */
  void update(RenderTreeNode node) {
    mRenderTreeNode = node;
  }

  void setHost(ComponentHost host) {
    mHost = host;
  }

  ComponentHost getHost() {
    return mHost;
  }

  /** @return Mount content created by the component. */
  Object getContent() {
    if (mMountData.mIsReleased) {
      throw new RuntimeException("Trying to access released mount content!");
    }
    return mContent;
  }

  public RenderTreeNode getRenderTreeNode() {
    return mRenderTreeNode;
  }

  /**
   * @return Whether this MountItem is currently bound. A bound mount item is a Mount item that has
   *     been mounted and is currently active on screen.
   */
  boolean isBound() {
    return mIsBound;
  }

  /** Sets whether this MountItem is currently bound. */
  void setIsBound(boolean bound) {
    mIsBound = bound;
  }

  Object getMountData() {
    return mMountData;
  }
}
