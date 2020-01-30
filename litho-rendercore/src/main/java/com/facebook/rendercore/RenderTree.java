// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.rendercore;

import java.util.Map;

/** TODO add javadoc */
public class RenderTree {

  private final RenderTreeNode mRoot;
  private final RenderTreeNode[] mFlatList;
  private final Map<?, ?> mLayoutContexts;
  private final int mWidthSpec;
  private final int mHeightSpec;

  public RenderTree(
      RenderTreeNode root,
      RenderTreeNode[] flatList,
      Map<?, ?> layoutContexts,
      int widthSpec,
      int heightSpec) {
    mRoot = root;
    mFlatList = flatList;
    mLayoutContexts = layoutContexts;
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

  public Map getLayoutContexts() {
    return mLayoutContexts;
  }
}
