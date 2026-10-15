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

/**
 * Calculates the range of items to prepare based on the current position.
 *
 * This interface is responsible for determining which items in a collection should be prepared
 * (pre-loaded) based on the current position (e.g., scroll position, visible area). The range
 * manager uses this to efficiently prepare items ahead of time and release items that are no longer
 * needed.
 *
 * @param Position The type representing the current position in the collection (e.g., indices of
 *   visible items, scroll offset)
 * @param Range The type representing the range of items to prepare (e.g., an IntRange of indices, a
 *   custom range type)
 */
fun interface RangeCalculator<in Position : Any, out Range : Any> {

  /**
   * Calculates the range of items that should be prepared based on the current position.
   *
   * This method is called by the range manager whenever the position changes (e.g., user scrolls).
   * Implementations should analyze the current position and determine which items should be
   * prepared to ensure smooth scrolling and optimal performance.
   *
   * For example, in a list implementation, this might calculate an IntRange that includes:
   * - Currently visible items
   * - Items just above the visible area (for upward scrolling)
   * - Items just below the visible area (for downward scrolling)
   *
   * @param position The current position in the collection
   * @return The range of items to prepare
   */
  fun calculateRange(position: Position): Range
}
