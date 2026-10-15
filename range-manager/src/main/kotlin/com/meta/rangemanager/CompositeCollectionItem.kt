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

package com.meta.rangemanager

import androidx.collection.MutableScatterMap
import androidx.collection.ScatterMap

/**
 * A composite implementation of [PreparableCollectionItem] that manages multiple child items.
 *
 * This class allows grouping multiple [PreparableCollectionItem] instances together and treating
 * them as a single unit. When [prepare] or [unprepare] is called on this composite, it delegates
 * the operation to all child items in the collection.
 *
 * This is useful for scenarios where a single collection item conceptually consists of multiple
 * preparable components that need to be managed together, such as:
 * - A complex UI component with multiple independently preparable sections
 * - A collection item that needs both data and UI preparation
 * - Aggregating multiple preparation strategies for a single item
 *
 * ## Usage Examples
 *
 * ### Map-based construction
 *
 * ```kotlin
 * val items = mutableScatterMapOf<Any, PreparableCollectionItem>(
 *     SubKey.HEADER to headerItem,
 *     SubKey.CONTENT to contentItem,
 *     SubKey.FOOTER to footerItem
 * )
 * val composite = CompositeCollectionItem(items)
 * ```
 *
 * ### Vararg-based construction (declarative)
 *
 * ```kotlin
 * val composite = CompositeCollectionItem(
 *     SubKey.HEADER to headerItem,
 *     SubKey.CONTENT to contentItem,
 *     SubKey.FOOTER to footerItem
 * )
 * ```
 *
 * @property items A map of child items to be prepared/unprepared together. The keys can be any
 *   identifier useful for distinguishing the child items, while the values are the actual
 *   [PreparableCollectionItem] instances to manage.
 * @see PreparableCollectionItem
 */
open class CompositeCollectionItem(open val items: ScatterMap<Any, out PreparableCollectionItem>) :
    PreparableCollectionItem {

  /**
   * Creates a composite from vararg pairs of keys to items.
   *
   * This constructor provides a convenient, declarative way to create composite items:
   * ```kotlin
   * CompositeCollectionItem(
   *     SubKey.A to itemA,
   *     SubKey.B to itemB
   * )
   * ```
   *
   * @param spec Pairs of (key, item) to include in this composite
   */
  constructor(
      vararg spec: Pair<Any, PreparableCollectionItem>,
  ) : this(
      MutableScatterMap<Any, PreparableCollectionItem>(spec.size).apply {
        spec.forEach { (key, item) -> put(key, item) }
      },
  )

  /**
   * Prepares all child items in the composite.
   *
   * This method iterates through all items in the [items] map and calls [prepare] on each one,
   * delegating the preparation work to the individual child items. Each child item's state is
   * automatically scoped under a [CompoundKey] to prevent state collisions between children. The
   * order of preparation follows the iteration order of the map's values.
   */
  override fun PrepareScope.prepare() {
    items.forEach { subKey, item -> prepareChild(subKey, item) }
  }

  /**
   * This API will unprepare all child items in the composite.
   *
   * This method iterates through all items in the [items] map and calls [unprepare] on each one,
   * delegating the cleanup work to the individual child items. Each child item's state is
   * automatically scoped under a [CompoundKey] to prevent state collisions between children. The
   * order of unprepare follows the iteration order of the map's values.
   */
  override fun PrepareScope.unprepare() {
    items.forEach { subKey, item -> unprepareChild(subKey, item) }
  }
}
