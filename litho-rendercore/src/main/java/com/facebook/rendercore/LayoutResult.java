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

import androidx.annotation.Nullable;
import androidx.annotation.Px;

/**
 * Represents the result of a Layout pass. A LayoutResult has a reference to its originating Node
 * and all the layout information needed to position the content of such Node.
 */
public interface LayoutResult {

  /** @return the RenderUnit that should be rendered by this layout result. */
  @Nullable
  RenderUnit<?> getRenderUnit();

  /**
   * @return layout specific data that was generated during the layout pass that created this
   *     LayoutResult.
   */
  @Nullable
  Object getLayoutData();

  /** @return the number of children of this LayoutResult. */
  int getChildrenCount();

  /** @return the LayoutResult for the given child index */
  LayoutResult getChildAt(int index);

  /** @return the resolved X position for the Node */
  @Px
  int getXForChildAtIndex(int index);

  /** @return the resolved Y position for the Node */
  @Px
  int getYForChildAtIndex(int index);

  /** @return the resolved width for the Node */
  @Px
  int getWidth();

  /** @return the resolved height for the Node */
  @Px
  int getHeight();

  /** @return the resolved top padding for the Node */
  @Px
  int getPaddingTop();

  /** @return the resolved right padding for the Node */
  @Px
  int getPaddingRight();

  /** @return the resolved bottom padding for the Node */
  @Px
  int getPaddingBottom();

  /** @return the resolved left padding for the Node */
  @Px
  int getPaddingLeft();

  /** @return the width measurement that generated this LayoutResult */
  int getWidthSpec();

  /** @return the height measurement that generated this LayoutResult */
  int getHeightSpec();
}
