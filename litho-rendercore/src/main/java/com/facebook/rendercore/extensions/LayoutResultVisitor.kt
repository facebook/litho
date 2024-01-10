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

package com.facebook.rendercore.extensions

import android.graphics.Rect
import com.facebook.rendercore.LayoutResult
import com.facebook.rendercore.RenderTreeNode

/**
 * The [LayoutResultVisitor] API is used by RenderCore to allow a [RenderCoreExtension] to visit
 * every [LayoutResult] during every layout pass. The visitor can collect any data that it is
 * interested in from every [LayoutResult]. A [LayoutResultVisitor] must not mutate its
 * [RenderCoreExtension]; it should be functional. Consequently, a [LayoutResultVisitor] should only
 * be used for one layout pass.
 *
 * @param State The state represents the data collected by the visitor for a layout pass.
 */
fun interface LayoutResultVisitor<State> {
  /**
   * This API is called for every LayoutResult during a layout pass.
   *
   * @param parent The parent [RenderTreeNode].
   * @param result The [LayoutResult] being visited.
   * @param bounds The bounds of this [LayoutResult] relative to its parent.
   * @param x The absolute x position.
   * @param y The absolute y position.
   * @param position The position of the layout result.
   * @param state The state the visitor can write to.
   */
  fun visit(
      parent: RenderTreeNode?,
      result: LayoutResult,
      bounds: Rect,
      x: Int,
      y: Int,
      position: Int,
      state: State?
  )
}
