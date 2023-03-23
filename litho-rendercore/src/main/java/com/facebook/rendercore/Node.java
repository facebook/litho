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

import android.view.View;

/**
 * Represents a single node in a RenderCore Tree. A Node has children, base layout information, and
 * whether it needs to be rendered.
 */
public interface Node<RenderContext> {

  /**
   * Implementations of Node are responsible to calculate a layout based on the width/height
   * constraints provided. A Node could decide to implement its own layout function or to delegate
   * to its RenderUnit measure.
   *
   * <ul>
   *   The general contract is:
   *   <li>- A Node must call calculateLayout on each child (if it has any) at least once, even if
   *       the Node is going to assign that child an exact size -- this gives a chance for children
   *       to lay out their own children and produce artifacts like text layouts.
   *   <li>- If a Node calls layout on a child with flexible specs (UNSPECIFIED or AT_MOST) to get
   *       sizing information, but ultimately decides to assign that child a different size than the
   *       child returned, the Node must call calculateLayout again on that child with a mode of
   *       EXACTLY to enforce the assigned size.
   * </ul>
   *
   * @param context The LayoutContext associated with this layout calculation {@link LayoutContext}
   * @param widthSpec a measure spec for the width in the format of {@link View.MeasureSpec}
   * @param heightSpec a measure spec for the height in the format of {@link View.MeasureSpec}
   */
  LayoutResult calculateLayout(LayoutContext<RenderContext> context, int widthSpec, int heightSpec);
}
