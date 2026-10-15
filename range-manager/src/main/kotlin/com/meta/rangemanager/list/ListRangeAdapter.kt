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

import com.meta.rangemanager.PreparableCollectionItem
import com.meta.rangemanager.RangeAdapter
import com.meta.rangemanager.ResolvedRange

internal class ListRangeAdapter(
    private val itemProvider: ListCollectionItemProvider,
) : RangeAdapter<ListPosition, IntRange, Int> {

  override fun resolve(position: ListPosition, range: IntRange): ResolvedRange<Int> =
      ResolvedRange.fromKeys(
          orderedKeys = orderedKeys(position.currentFirstVisible, range),
          releaseComparator = releaseComparator(range),
          itemForKey = { key -> itemProvider[key] },
      )

  override val supportsExactEviction: Boolean
    get() = itemProvider.supportsExactEviction

  override fun isCachedSame(key: Int, item: PreparableCollectionItem): Boolean =
      itemProvider.isCachedSame(key, item)

  override fun claimReleaseIfSame(key: Int, item: PreparableCollectionItem): Boolean =
      itemProvider.claimReleaseIfSame(key, item)

  override fun evictIfSame(key: Int, item: PreparableCollectionItem): Boolean =
      itemProvider.evictIfSame(key, item)

  override fun notifyRemoved(key: Int, item: PreparableCollectionItem) {
    itemProvider.notifyRemoved(key, item)
  }

  /**
   * [firstVisible] must belong to a non-empty [range]. This is the existing list-worker contract:
   * [ListRangeCalculator] produces ranges around valid visible positions, and the RecyclerView
   * emitter suppresses [androidx.recyclerview.widget.RecyclerView.NO_POSITION].
   */
  private fun orderedKeys(firstVisible: Int, range: IntRange): List<Int> {
    if (range.isEmpty()) {
      return emptyList()
    }
    return buildList(range.last - range.first + 1) {
      for (index in firstVisible..range.last) {
        add(index)
      }
      for (index in firstVisible - 1 downTo range.first) {
        add(index)
      }
    }
  }

  private fun releaseComparator(range: IntRange): Comparator<Int> {
    if (range.isEmpty()) {
      return compareBy { key -> key }
    }
    return Comparator { left, right ->
      val leftGroup = releaseGroup(left, range)
      val rightGroup = releaseGroup(right, range)
      if (leftGroup != rightGroup) {
        leftGroup.compareTo(rightGroup)
      } else if (leftGroup == ABOVE_RANGE) {
        left.compareTo(right)
      } else {
        right.compareTo(left)
      }
    }
  }

  private fun releaseGroup(key: Int, range: IntRange): Int =
      if (key > range.last) ABOVE_RANGE else BELOW_RANGE

  private companion object {
    private const val ABOVE_RANGE = 0
    private const val BELOW_RANGE = 1
  }
}
