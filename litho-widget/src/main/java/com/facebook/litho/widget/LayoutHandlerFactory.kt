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

package com.facebook.litho.widget

import com.facebook.litho.ComponentTree
import com.facebook.rendercore.RunnableHandler

/** A Factory used to create [RunnableHandler]s in [RecyclerBinder]. */
interface LayoutHandlerFactory {
  /**
   * @return a new [RunnableHandler] that will be used to compute the layouts of the children of the
   *   [Recycler].
   */
  fun createLayoutCalculationHandler(renderInfo: RenderInfo): RunnableHandler?

  /**
   * @return If true, [RunnableHandler] of [ComponentTree] that's being updated by update operation
   *   of [RecyclerBinder] will be replaced by new [RunnableHandler] returned from
   *   [createLayoutCalculationHandler], otherwise keep using existing [RunnableHandler] created
   *   during item insertion.
   */
  fun shouldUpdateLayoutHandler(previousRenderInfo: RenderInfo, newRenderInfo: RenderInfo): Boolean
}
