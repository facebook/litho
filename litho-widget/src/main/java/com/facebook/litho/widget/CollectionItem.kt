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
import com.facebook.rendercore.Size
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
    var renderInfo: RenderInfo
) {

  /**
   * Returns the size of this collection item.
   *
   * @return Size object containing the width and height dimensions of the item
   */
  abstract val size: Size?

  /**
   * Returns the size constraints that define the measurement boundaries for this collection item.
   * These constraints specify the minimum and maximum width and height values that should be used
   * when measuring and laying out the item.
   *
   * @return SizeConstraints object containing the measurement boundaries, or null if no specific
   *   constraints are defined for this item
   */
  abstract val sizeConstraints: SizeConstraints?

  /**
   * Measures the item according to the given size constraints.
   *
   * @param sizeConstraints The constraints that define the minimum and maximum dimensions for
   *   measurement
   * @param result Optional array to store the measured width and height values (index 0 for width,
   *   index 1 for height)
   */
  abstract fun measure(sizeConstraints: SizeConstraints, result: IntArray?)

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
   */
  abstract fun prepareSync(sizeConstraints: SizeConstraints, result: IntArray?)

  /**
   * Binds data and properties to the view when it's being displayed in the collection. This method
   * is called when the RecyclerView needs to display the item at a specific position.
   *
   * @param view The view instance of type V that will display this collection item
   * @param sizeConstraints The size constraints that define the available space for rendering the
   *   view
   */
  abstract fun onBindView(view: V, sizeConstraints: SizeConstraints)

  /**
   * Called when a view has been recycled and is no longer being displayed in the collection. This
   * method should clean up any resources, listeners, or bindings associated with the view to
   * prevent memory leaks and ensure proper recycling behavior.
   *
   * @param view The view instance of type V that has been recycled and needs cleanup
   */
  abstract fun onViewRecycled(view: V)

  /**
   * To unprepare the item, which is called after the item is being released by the range
   * preparation.
   */
  abstract fun unprepare()

  companion object {
    /** The key of unique identifier to find the position in the RecyclerView.Adapter. */
    const val ID_CUSTOM_ATTR_KEY: String = "id"
  }
}
