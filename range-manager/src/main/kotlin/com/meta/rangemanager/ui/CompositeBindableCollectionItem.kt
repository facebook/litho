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

package com.meta.rangemanager.ui

import android.view.View
import androidx.collection.MutableScatterMap
import androidx.collection.ScatterMap
import com.meta.rangemanager.CompositeCollectionItem

/**
 * A composite implementation of [BindableCollectionItem] that manages multiple child items with
 * view binding capabilities.
 *
 * This class extends [CompositeCollectionItem] to add view binding support, allowing multiple
 * [BindableCollectionItem] instances to be grouped together and bound to different views within a
 * container. Each child item is responsible for finding its own view within the container and
 * binding to it.
 *
 * This is useful for complex UI components where:
 * - A single collection item contains multiple independently bindable sub-components
 * - Each sub-component needs its own view within a container view
 * - Both preparation and binding need to be coordinated across multiple items
 *
 * ## Usage Examples
 *
 * ### Map-based construction
 *
 * ```kotlin
 * val items = mutableScatterMapOf<Any, BindableCollectionItem>(
 *     SubKey.VIDEO to videoItem,
 *     SubKey.OVERLAY to overlayItem,
 *     SubKey.CONTROLS to controlsItem
 * )
 * val composite = CompositeBindableCollectionItem(items)
 * ```
 *
 * ### Vararg-based construction (declarative)
 *
 * ```kotlin
 * val composite = CompositeBindableCollectionItem(
 *     SubKey.VIDEO to videoItem,
 *     SubKey.OVERLAY to overlayItem,
 *     SubKey.CONTROLS to controlsItem
 * )
 * ```
 *
 * Example use case: A story item that contains separate bindable components for the video player,
 * overlay UI, and interactive elements, each with their own view and preparation logic.
 *
 * @property items A [ScatterMap] of child items to be prepared/unprepared and bound/unbound
 *   together. Uses [ScatterMap] for efficient key-based lookups.
 * @see BindableCollectionItem
 * @see CompositeCollectionItem
 */
class CompositeBindableCollectionItem(
    override val items: ScatterMap<Any, BindableCollectionItem>,
) : CompositeCollectionItem(items), BindableCollectionItem {

  /**
   * Creates a composite from vararg pairs of keys to bindable items.
   *
   * This constructor provides a convenient, declarative way to create composite bindable items:
   * ```kotlin
   * CompositeBindableCollectionItem(
   *     SubKey.A to itemA,
   *     SubKey.B to itemB
   * )
   * ```
   *
   * @param spec Pairs of (key, item) to include in this composite
   */
  constructor(
      vararg spec: Pair<Any, BindableCollectionItem>,
  ) : this(
      MutableScatterMap<Any, BindableCollectionItem>(spec.size).apply {
        spec.forEach { (key, item) -> put(key, item) }
      },
  )

  /**
   * Binds all child items to their corresponding views within the container.
   *
   * This method iterates through all items in the [items] map and calls [bind] on each child item
   * with the container view. Each child item is responsible for locating its specific view within
   * the container and performing its own binding logic.
   *
   * The binding order follows the iteration order of the [ScatterMap].
   *
   * @param view The parent view containing all the child views for the items
   */
  override fun bind(view: View) {
    items.forEachValue { item -> item.bind(view) }
  }

  /**
   * Unbinds all child items from their views.
   *
   * This method iterates through all items in the [items] map and calls [unbind] on each one,
   * delegating the view cleanup work to the individual child items. The unbinding order follows the
   * iteration order of the [ScatterMap].
   */
  override fun unbind() {
    items.forEachValue { item -> item.unbind() }
  }
}
