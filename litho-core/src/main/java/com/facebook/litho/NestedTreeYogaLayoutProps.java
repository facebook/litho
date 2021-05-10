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

import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaNode;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class NestedTreeYogaLayoutProps extends YogaLayoutProps {

  private @Nullable int[] mBorderEdges;
  private @Nullable Edges mPaddingEdges;
  private @Nullable boolean[] mIsPaddingPercentage;

  public NestedTreeYogaLayoutProps(YogaNode node) {
    super(node);
  }

  @Override
  public void paddingPx(YogaEdge edge, int padding) {
    setPadding(edge, padding);
    setIsPaddingPercentage(edge, false);
  }

  @Override
  public void paddingPercent(YogaEdge edge, float percent) {
    setPadding(edge, percent);
    setIsPaddingPercentage(edge, true);
  }

  @Override
  public void setBorderWidth(YogaEdge edge, float borderWidth) {
    if (mBorderEdges == null) {
      mBorderEdges = new int[Border.EDGE_COUNT];
    }
    Border.setEdgeValue(mBorderEdges, edge, (int) borderWidth);
  }

  public @Nullable int[] getBorderWidth() {
    return mBorderEdges;
  }

  public @Nullable Edges getPadding() {
    return mPaddingEdges;
  }

  public @Nullable boolean[] getIsPaddingPercentage() {
    return mIsPaddingPercentage;
  }

  private void setPadding(YogaEdge edge, float width) {
    if (mPaddingEdges == null) {
      mPaddingEdges = new Edges();
    }
    mPaddingEdges.set(edge, width);
  }

  private void setIsPaddingPercentage(YogaEdge edge, boolean isPercentage) {
    if (mIsPaddingPercentage == null && isPercentage) {
      mIsPaddingPercentage = new boolean[YogaEdge.ALL.intValue() + 1];
    }
    if (mIsPaddingPercentage != null) {
      mIsPaddingPercentage[edge.intValue()] = isPercentage;
    }
  }
}
