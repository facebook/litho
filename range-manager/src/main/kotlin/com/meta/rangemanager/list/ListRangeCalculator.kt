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

package com.meta.rangemanager.list

import com.meta.rangemanager.RangeCalculator

/**
 * Calculator for determining the range of items to prepare in a linear list layout.
 *
 * This implementation of [RangeCalculator] is designed for collections with linear layouts (such as
 * RecyclerView with LinearLayoutManager). It calculates a range of item indices to prepare based on
 * the currently visible items and configurable behind/ahead buffer sizes.
 *
 * The calculator extends the visible range in both directions:
 * - Behind items: Items before the first visible item (lower indices)
 * - Ahead items: Items after the last visible item (higher indices)
 *
 * This ensures items are prepared ahead of time as the user scrolls, improving perceived
 * performance.
 *
 * @property config Configuration specifying how many items to prepare behind and ahead of the
 *   visible range
 *
 * Example usage:
 * ```
 * val config = ListRangeConfig(behind = 10, ahead = 5)
 * val calculator = ListRangeCalculator(config)
 *
 * // If items 10-20 are visible:
 * // - With behind=10: prepares items 0-20
 * // - With ahead=5: prepares items 10-25
 * // - Total range: 0..25
 * ```
 */
class ListRangeCalculator(
    val config: ListRangeConfig,
    val itemProvider: ListCollectionItemProvider,
) : RangeCalculator<ListPosition, IntRange> {

  /**
   * Calculates the range of item indices to prepare based on current position.
   *
   * The calculation:
   * 1. Takes the currently visible range
   *    [position.currentFirstVisible, position.currentLastVisible]
   * 2. Extends backward by [ListRangeConfig.behind] items (clamped to 0)
   * 3. Extends forward by [ListRangeConfig.ahead] items (clamped to collection size)
   * 4. Returns an IntRange representing the indices to prepare
   *
   * @return An IntRange from start (first visible - behind) to end (last visible + ahead), clamped
   *   to valid collection bounds [0, lastIndex]
   */
  override fun calculateRange(position: ListPosition): IntRange {
    val first = position.currentFirstVisible
    val last = position.currentLastVisible
    val start = maxOf(0, first - config.behind)
    val end = minOf(itemProvider.lastIndex, last + config.ahead)
    return start..end
  }
}
