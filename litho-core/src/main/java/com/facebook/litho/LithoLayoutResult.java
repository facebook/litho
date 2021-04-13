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
import com.facebook.litho.InternalNode.NestedTreeHolder;
import com.facebook.rendercore.Node.LayoutResult;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaNode;

/** The {@link LayoutResult} class for Litho */
public interface LithoLayoutResult extends ComponentLayout {

  InternalNode getInternalNode();

  int getChildCount();

  LithoLayoutResult getChildAt(int i);

  @Nullable
  LithoLayoutResult getParent();

  void setParent(LithoLayoutResult parent);

  YogaNode getYogaNode();

  boolean shouldDrawBorders();

  int getLayoutBorder(YogaEdge edge);

  int getTouchExpansionBottom();

  int getTouchExpansionLeft();

  int getTouchExpansionRight();

  int getTouchExpansionTop();

  /** Continually walks the node hierarchy until a node returns a non inherited layout direction */
  YogaDirection recursivelyResolveLayoutDirection();

  /**
   * The last value the measure funcion associated with this node {@link Component} returned for the
   * height. This is used together with {@link LithoLayoutResult#getLastHeightSpec()} to implement
   * measure caching.
   */
  float getLastMeasuredHeight();

  /**
   * The last value the measure funcion associated with this node {@link Component} returned for the
   * width. This is used together with {@link LithoLayoutResult#getLastWidthSpec()} to implement
   * measure caching.
   */
  float getLastMeasuredWidth();

  int getLastHeightSpec();

  int getLastWidthSpec();

  /* Measurement related APIs for mutating the result */

  void setLastWidthSpec(int widthSpec);

  void setLastHeightSpec(int heightSpec);

  /**
   * Sets the last value the measure funcion associated with this node {@link Component} returned
   * for the height.
   */
  void setLastMeasuredHeight(float lastMeasuredHeight);

  /**
   * Sets the last value the measure funcion associated with this node {@link Component} returned
   * for the width.
   */
  void setLastMeasuredWidth(float lastMeasuredWidth);

  /** Holds the {@link LithoLayoutResult} for {@link NestedTreeHolder} */
  interface NestedTreeHolderResult extends LithoLayoutResult {

    NestedTreeHolder getInternalNode();

    @Nullable
    LithoLayoutResult getNestedResult();

    void setNestedResult(@Nullable LithoLayoutResult tree);
  }
}
