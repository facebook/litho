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

/** TODO add javadoc */
public class RenderTree {

  private final RenderTreeNode mRoot;
  private final RenderTreeNode[] mFlatList;
  private final int mWidthSpec;
  private final int mHeightSpec;

  public RenderTree(RenderTreeNode root, RenderTreeNode[] flatList, int widthSpec, int heightSpec) {
    mRoot = root;
    mFlatList = flatList;
    mWidthSpec = widthSpec;
    mHeightSpec = heightSpec;
  }

  public int getWidth() {
    return mRoot.getBounds().width();
  }

  public int getHeight() {
    return mRoot.getBounds().height();
  }

  public int getWidthSpec() {
    return mWidthSpec;
  }

  public int getHeightSpec() {
    return mHeightSpec;
  }

  public int getRenderTreeNodeIndex(long renderUnitId) {
    for (int i = 0; i < mFlatList.length; i++) {
      if (mFlatList[i].getRenderUnit().getId() == renderUnitId) {
        return i;
      }
    }

    return -1;
  }

  public RenderTreeNode getRenderTreeNodeAtIndex(int index) {
    return mFlatList[index];
  }

  public int getMountableOutputCount() {
    return mFlatList.length;
  }
}
