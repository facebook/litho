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

import android.view.View
import com.facebook.rendercore.SizeConstraints

/**
 * Abstract class representing an item in a collection that can be rendered as a child view of a
 * collection. This class provides the foundation for managing collection items with their rendering
 * lifecycle.
 *
 * @param V The type of View this collection item represents
 * @property id The unique identifier for this item in the RecyclerView.Adapter
 * @property viewType The type identifier used for this item in the RecyclerView.Adapter
 * @property renderInfo Contains information about how to render this item
 */
abstract class CollectionItem<V : View>(
    val id: Int,
    val viewType: Int,
    var renderInfo: RenderInfo,
) {

  /**
   * To prepare the item for rendering in async way. This is going to be used in the range
   * preparation.
   *
   * @param sizeConstraints The size constraints of the item.
   */
  abstract fun prepare(sizeConstraints: SizeConstraints)

  /**
   * To prepare the item for rendering in sync way.
   *
   * @param sizeConstraints The size constraints of the item.
   * @param result The output of the rendering.
   * @param shouldCommit Whether the result should be committed.
   */
  abstract fun prepareSync(
      sizeConstraints: SizeConstraints,
      result: IntArray?,
      shouldCommit: Boolean = true
  )

  /** To bind properties for the underline view. */
  abstract fun onBindView(view: V)

  /** To unbind properties for the underline view. */
  abstract fun onViewRecycled(view: V)

  /**
   * To unprepare the item, which is called after the item is being released by the range
   * preparation.
   */
  abstract fun unprepare()
}
