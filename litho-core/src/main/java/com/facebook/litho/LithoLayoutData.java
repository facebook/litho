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

package com.facebook.litho;

import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.LayoutOutput.UpdateState;
import com.facebook.rendercore.Node;
import com.facebook.rendercore.RenderState.LayoutContext;
import com.facebook.rendercore.RenderTreeNode;

/**
 * This object will host the data associated with the component which is generated during the
 * measure pass, for example: the {@link InterStagePropsContainer}, and the {@link UpdateState}. It
 * will be created in {@link Node.LayoutResult#calculateLayout(LayoutContext, int, int)}. This
 * object will be returned by {@link Node.LayoutResult#getLayoutData()}, then written to the layout
 * data in {@link RenderTreeNode} during reduce.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class LithoLayoutData {

  public final int width;
  public final int height;
  public final int currentLayoutStateId;
  public final int previousLayoutStateId;
  public final @Nullable Object mLayoutData;

  public LithoLayoutData(
      int width,
      int height,
      int currentLayoutStateId,
      int previousLayoutStateId,
      @Nullable Object mLayoutData) {
    this.width = width;
    this.height = height;
    this.currentLayoutStateId = currentLayoutStateId;
    this.previousLayoutStateId = previousLayoutStateId;
    this.mLayoutData = mLayoutData;
  }

  /**
   * Helper method to throw exception if a provided layout-data is null or not a LithoLayoutData
   * instance. Will return a casted, non-null instance of LithoLayoutData otherwise.
   */
  static LithoLayoutData verifyAndGetLithoLayoutData(@Nullable Object layoutData) {
    if (layoutData == null) {
      throw new RuntimeException("layout data must not be null.");
    }

    if (!(layoutData instanceof LithoLayoutData)) {
      throw new RuntimeException(
          "RenderTreeNode layout data for Litho should be LithoLayoutData but was <cls>"
              + layoutData.getClass().getName()
              + "</cls>");
    }

    return (LithoLayoutData) layoutData;
  }

  static @Nullable InterStagePropsContainer getInterStageProps(@Nullable Object data) {
    final LithoLayoutData layoutData = verifyAndGetLithoLayoutData(data);

    if (layoutData.mLayoutData == null) {
      return null;
    }

    if (!(layoutData.mLayoutData instanceof InterStagePropsContainer)) {
      throw new RuntimeException(
          "Layout data was not InterStagePropsContainer but was <cls>"
              + layoutData.mLayoutData.getClass().getName()
              + "</cls>");
    }

    return (InterStagePropsContainer) layoutData.mLayoutData;
  }
}
