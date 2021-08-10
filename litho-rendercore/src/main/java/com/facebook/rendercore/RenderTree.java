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

import androidx.annotation.Nullable;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import com.facebook.rendercore.utils.MeasureSpecUtils;
import java.util.Locale;
import java.util.Map;

/** TODO add javadoc */
public class RenderTree {

  private final RenderTreeNode mRoot;
  private final RenderTreeNode[] mFlatList;
  private final int mWidthSpec;
  private final int mHeightSpec;
  private final @Nullable Map<RenderCoreExtension<?, ?>, Object> mResults;

  private @Nullable Object mRenderTreeData;

  public RenderTree(
      final RenderTreeNode root,
      final RenderTreeNode[] flatList,
      final int widthSpec,
      final int heightSpec,
      final @Nullable Map<RenderCoreExtension<?, ?>, Object> results) {
    mRoot = root;
    mFlatList = flatList;
    mWidthSpec = widthSpec;
    mHeightSpec = heightSpec;
    mResults = results;
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

  public @Nullable Map<RenderCoreExtension<?, ?>, Object> getExtensionResults() {
    return mResults;
  }

  @Nullable
  public Object getRenderTreeData() {
    return mRenderTreeData;
  }

  public void setRenderTreeData(Object renderTreeData) {
    mRenderTreeData = renderTreeData;
  }

  public RenderTreeNode getRoot() {
    return mRoot;
  }

  public String generateDebugString() {
    final StringBuilder stringBuilder = new StringBuilder();
    final Locale l = Locale.US;

    final String widthSpecDesc = MeasureSpecUtils.getMeasureSpecDescription(mWidthSpec);
    final String heightSpecDesc = MeasureSpecUtils.getMeasureSpecDescription(mHeightSpec);

    stringBuilder.append("RenderTree details:\n");
    stringBuilder.append(
        String.format(l, "WidthSpec=%s; HeightSpec=%s\n", widthSpecDesc, heightSpecDesc));

    stringBuilder.append(String.format(l, "Full child list (size = %d):\n", mFlatList.length));

    for (RenderTreeNode node : mFlatList) {
      stringBuilder.append(String.format(l, "%s\n", node.generateDebugString(this)));
    }

    return stringBuilder.toString();
  }
}
