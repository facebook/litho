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
import com.facebook.rendercore.visibility.VisibilityOutput;
import java.util.List;

/**
 * A lightweight representation of a layout node, used to cache measurements between two Layout tree
 * calculations.
 */
public interface DiffNode extends Cloneable {

  int UNSPECIFIED = -1;

  int getChildCount();

  @Nullable
  DiffNode getChildAt(int i);

  @Nullable
  Component getComponent();

  @Nullable
  String getComponentGlobalKey();

  @Nullable
  ScopedComponentInfo getScopedComponentInfo();

  void setComponent(
      @Nullable Component component,
      @Nullable String globalKey,
      @Nullable ScopedComponentInfo scopedComponentInfo);

  /**
   * The last value the measure funcion associated with this node {@link Component} returned for the
   * width. This is used together with {@link InternalNode#getLastWidthSpec()} to implement measure
   * caching.
   */
  float getLastMeasuredWidth();

  /**
   * Sets the last value the measure funcion associated with this node {@link Component} returned
   * for the width.
   */
  void setLastMeasuredWidth(float lastMeasuredWidth);

  /**
   * The last value the measure funcion associated with this node {@link Component} returned for the
   * height. This is used together with {@link InternalNode#getLastHeightSpec()} to implement
   * measure caching.
   */
  float getLastMeasuredHeight();

  /**
   * Sets the last value the measure funcion associated with this node {@link Component} returned
   * for the height.
   */
  void setLastMeasuredHeight(float lastMeasuredHeight);

  int getLastWidthSpec();

  void setLastWidthSpec(int widthSpec);

  int getLastHeightSpec();

  void setLastHeightSpec(int heightSpec);

  List<DiffNode> getChildren();

  void addChild(DiffNode node);

  @Nullable
  LithoRenderUnit getContentOutput();

  void setContentOutput(@Nullable LithoRenderUnit content);

  @Nullable
  VisibilityOutput getVisibilityOutput();

  void setVisibilityOutput(@Nullable VisibilityOutput visibilityOutput);

  @Nullable
  LithoRenderUnit getBackgroundOutput();

  void setBackgroundOutput(@Nullable LithoRenderUnit background);

  @Nullable
  LithoRenderUnit getForegroundOutput();

  void setForegroundOutput(@Nullable LithoRenderUnit foreground);

  @Nullable
  LithoRenderUnit getBorderOutput();

  void setBorderOutput(@Nullable LithoRenderUnit border);

  @Nullable
  LithoRenderUnit getHostOutput();

  void setHostOutput(@Nullable LithoRenderUnit host);
}
