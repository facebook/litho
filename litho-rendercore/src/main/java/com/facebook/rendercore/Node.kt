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

package com.facebook.rendercore

/**
 * Represents a single node in a RenderCore Tree. A Node has children, base layout information, and
 * whether it needs to be rendered.
 */
interface Node<RenderContext> {
  /**
   * Implementations of Node are responsible to calculate a layout based on the width/height
   * constraints provided. A Node could decide to implement its own layout function or to delegate
   * to its RenderUnit measure.
   *
   * The general contract is:
   * * - A Node must call calculateLayout on each child (if it has any) at least once, even if the
   *   Node is going to assign that child an exact size -- this gives a chance for children to lay
   *   out their own children and produce artifacts like text layouts.
   * * - If a Node calls layout on a child with unspecified or bounded size constraints to get
   *   sizing information, but ultimately decides to assign that child a different size than the
   *   child returned, the Node must call calculateLayout again on that child with the exact size
   *   constraints.
   *
   * The default implementation exists for compatibility with existing [Node] implementations until
   * they are refactored to use this API.
   *
   * @param context The LayoutContext associated with this layout calculation [LayoutContext]
   * @param sizeConstraints The size constraints for this layout pass
   */
  fun calculateLayout(
      context: LayoutContext<@UnsafeVariance RenderContext>,
      sizeConstraints: SizeConstraints
  ): LayoutResult {
    return calculateLayout(context, sizeConstraints.toWidthSpec(), sizeConstraints.toHeightSpec())
  }

  /**
   * Implementations of Node are responsible to calculate a layout based on the width/height
   * constraints provided. A Node could decide to implement its own layout function or to delegate
   * to its RenderUnit measure.
   *
   * The general contract is:
   * * - A Node must call calculateLayout on each child (if it has any) at least once, even if the
   *   Node is going to assign that child an exact size -- this gives a chance for children to lay
   *   out their own children and produce artifacts like text layouts.
   * * - If a Node calls layout on a child with flexible specs (UNSPECIFIED or AT_MOST) to get
   *   sizing information, but ultimately decides to assign that child a different size than the
   *   child returned, the Node must call calculateLayout again on that child with a mode of EXACTLY
   *   to enforce the assigned size.
   *
   * @param context The LayoutContext associated with this layout calculation [LayoutContext]
   * @param widthSpec a measure spec for the width in the format of [View.MeasureSpec]
   * @param heightSpec a measure spec for the height in the format of [View.MeasureSpec]
   */
  @Deprecated(message = "Invoke the calculate layout with SizeConstraint API")
  fun calculateLayout(
      context: LayoutContext<@UnsafeVariance RenderContext>,
      widthSpec: Int,
      heightSpec: Int,
  ): LayoutResult {
    throw UnsupportedOperationException("This API must be implemented to be invoked.")
  }
}
