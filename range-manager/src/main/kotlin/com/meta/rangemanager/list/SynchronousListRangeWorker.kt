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

import com.meta.rangemanager.ItemWork
import com.meta.rangemanager.PrepareScope
import com.meta.rangemanager.SynchronousRangeWorker
import com.meta.rangemanager.WorkQueueBuilder
import com.meta.rangemanager.WorkType
import com.meta.systrace.NoOpSystracer
import com.meta.systrace.Systracer

/**
 * Synchronous worker implementation for preparing items in collections with linear layouts.
 *
 * This implementation of [SynchronousRangeWorker] is designed for list-based collections (such as
 * RecyclerView with LinearLayoutManager). It performs preparation and release work synchronously on
 * the calling thread.
 *
 * Key features:
 * - Synchronous preparation: All item preparation work happens on the calling thread
 * - Intelligent work ordering: Prioritizes items closest to the visible viewport first
 * - Incremental updates: Can cancel and restart work when the range changes
 *
 * Work ordering strategy:
 * 1. Start from the currently visible items
 * 2. Interleave preparation of leading items (above visible) and trailing items (below visible)
 * 3. Release items that are no longer in range
 *
 * ## Use Cases
 * This worker is suitable when:
 * - The calling code is responsible for managing threading during item preparation
 * - Preparation work is very lightweight and won't block the UI
 * - You need deterministic, immediate preparation (e.g., for testing)
 * - Thread synchronization is important for your use case
 *
 * ## Threading
 * - [schedule] must be called on the UI thread (enforced by @UiThread annotation on [RangeWorker])
 * - All work is performed synchronously on the calling thread
 *
 * @property itemProvider Provides access to collection items for preparation and release operations
 * @property scope The [PrepareScope] context passed to [PreparableCollectionItem.prepare] and
 *   [PreparableCollectionItem.unprepare] methods during work execution
 * @property tracer Optional tracer for debugging and performance monitoring. Defaults to
 *   [NoOpSystracer] if not provided.
 */
class SynchronousListRangeWorker(
    itemProvider: ListCollectionItemProvider,
    scope: PrepareScope,
    tracer: Systracer = NoOpSystracer(),
) :
    SynchronousRangeWorker<ListPosition, IntRange>(
        workQueueBuilder = ListWorkQueueBuilder(itemProvider),
        scope = scope,
        tracer = tracer,
    )

/**
 * Work queue builder implementation for list-based collections.
 *
 * This implementation creates a work queue that:
 * 1. Starts from the currently visible items
 * 2. Moves ahead (forward) to higher indices, then behind (backward) to lower indices
 * 3. Releases items from the previous range that are no longer in the new range
 *
 * Special cases:
 * - If the new range is empty, all items in the previous range are unprepared
 *
 * @property itemProvider Provides access to collection items
 */
internal class ListWorkQueueBuilder(
    private val itemProvider: ListCollectionItemProvider,
) : WorkQueueBuilder<ListPosition, IntRange> {

  /**
   * Builds a work queue for preparing and releasing items in a list-based collection.
   *
   * @param previousRange The previously prepared range
   * @param newPosition The current visible position in the list
   * @param newRange The new range to prepare
   * @return A read-only list of work items ordered by priority (items closest to visible position
   *   first)
   */
  override fun buildWorkQueue(
      previousRange: IntRange,
      newPosition: ListPosition,
      newRange: IntRange,
  ): List<ItemWork> {
    // Special case: if newRange is empty, only unprepare items in previousRange
    if (newRange.isEmpty()) {
      val size =
          if (previousRange.isEmpty()) {
            0
          } else {
            previousRange.last - previousRange.first + 1
          }
      return buildList(size) {
        // Unprepare all items in previous range
        for (index in previousRange) {
          add(
              ItemWork(
                  key = index,
                  item = itemProvider[index],
                  type = WorkType.Unprepare,
                  execute = { scope, workItem, workKey ->
                    scope.unprepare(workKey, workItem)
                    itemProvider.evict(workKey as Int)
                  },
              ),
          )
        }
      }
    }

    val start = minOf(previousRange.first, newRange.first)
    val end = maxOf(previousRange.last, newRange.last)
    val size = end - start + 1

    return buildList(size) {
      // Start at first visible and move forward (ahead) to higher indices
      var currentAheadIndex = newPosition.currentFirstVisible
      // Start just before first visible and move backward (behind) to lower indices
      var currentBehindIndex = newPosition.currentFirstVisible - 1

      while (true) {
        when {
          // Prepare items ahead (forward from first visible toward higher indices)
          currentAheadIndex <= newRange.last -> {
            add(
                ItemWork(
                    key = currentAheadIndex,
                    item = itemProvider[currentAheadIndex],
                    type = WorkType.Prepare,
                    execute = { scope, workItem, workKey -> scope.prepare(workKey, workItem) },
                ),
            )
            currentAheadIndex++
          }
          // Prepare items behind (backward from first visible toward lower indices)
          currentBehindIndex >= newRange.first -> {
            add(
                ItemWork(
                    key = currentBehindIndex,
                    item = itemProvider[currentBehindIndex],
                    type = WorkType.Prepare,
                    execute = { scope, workItem, workKey -> scope.prepare(workKey, workItem) },
                ),
            )
            currentBehindIndex--
          }
          // Release items ahead (forward beyond new range)
          currentAheadIndex <= end -> {
            add(
                ItemWork(
                    key = currentAheadIndex,
                    item = itemProvider[currentAheadIndex],
                    type = WorkType.Unprepare,
                    execute = { scope, workItem, workKey ->
                      scope.unprepare(workKey, workItem)
                      itemProvider.evict(workKey as Int)
                    },
                ),
            )
            currentAheadIndex++
          }
          // Release items behind (backward beyond new range)
          currentBehindIndex >= start -> {
            add(
                ItemWork(
                    key = currentBehindIndex,
                    item = itemProvider[currentBehindIndex],
                    type = WorkType.Unprepare,
                    execute = { scope, workItem, workKey ->
                      scope.unprepare(workKey, workItem)
                      itemProvider.evict(workKey as Int)
                    },
                ),
            )
            currentBehindIndex--
          }
          else -> break
        }
      }
    }
  }
}
