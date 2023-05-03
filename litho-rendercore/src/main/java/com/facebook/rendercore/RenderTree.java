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

import android.util.LongSparseArray;
import android.util.Pair;
import androidx.annotation.Nullable;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import com.facebook.rendercore.utils.MeasureSpecUtils;
import java.util.List;
import java.util.Locale;

/** TODO add javadoc */
public class RenderTree {

  private final int mRenderStateId;
  private final RenderTreeNode mRoot;
  private final RenderTreeNode[] mFlatList;
  private final int mWidthSpec;
  private final int mHeightSpec;
  private final @Nullable List<Pair<RenderCoreExtension<?, ?>, Object>> mResults;
  private final LongSparseArray<Integer> mIdToIndexMap = new LongSparseArray<>();
  private @Nullable Object mDebugData;

  public RenderTree(
      final RenderTreeNode root,
      final RenderTreeNode[] flatList,
      final int widthSpec,
      final int heightSpec,
      final int renderStateId,
      final @Nullable List<Pair<RenderCoreExtension<?, ?>, Object>> results,
      final @Nullable Object debugData) {
    mRoot = root;
    mFlatList = flatList;
    mWidthSpec = widthSpec;
    mHeightSpec = heightSpec;
    mResults = results;
    mRenderStateId = renderStateId;
    mDebugData = debugData;

    for (int i = 0; i < mFlatList.length; i++) {
      assertNoDuplicateRenderUnits(i);
      mIdToIndexMap.put(mFlatList[i].getRenderUnit().getId(), i);
    }
  }

  /**
   * Throws an exception if this RenderTree already has a RenderUnit with the same ID as the one at
   * the given index.
   */
  private void assertNoDuplicateRenderUnits(int newNodeIndex) {
    final RenderTreeNode newNode = mFlatList[newNodeIndex];
    if (mIdToIndexMap.get(newNode.getRenderUnit().getId()) == null) {
      return;
    }

    final int existingNodeIndex = mIdToIndexMap.get(newNode.getRenderUnit().getId());
    final RenderTreeNode existingNode = mFlatList[existingNodeIndex];

    throw new IllegalStateException(
        String.format(
            Locale.US,
            "RenderTrees must not have RenderUnits with the same ID:\n"
                + "Attempted to add item with existing ID at index %d: %s\n"
                + "Existing item at index %d: %s\n"
                + "Full RenderTree: %s",
            newNodeIndex,
            newNode.generateDebugString(null),
            existingNodeIndex,
            existingNode.generateDebugString(null),
            generateDebugString()));
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

  public int getRenderStateId() {
    return mRenderStateId;
  }

  public int getRenderTreeNodeIndex(long renderUnitId) {
    return mIdToIndexMap.get(renderUnitId, -1);
  }

  public RenderTreeNode getRenderTreeNodeAtIndex(int index) {
    return mFlatList[index];
  }

  public int getMountableOutputCount() {
    return mFlatList.length;
  }

  public @Nullable List<Pair<RenderCoreExtension<?, ?>, Object>> getExtensionResults() {
    return mResults;
  }

  // This will vary by framework and will be null outside of debug builds
  @Nullable
  public Object getDebugData() {
    return mDebugData;
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
