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

import androidx.annotation.UiThread

/**
 * Worker component responsible for preparing items within a calculated range.
 *
 * This interface is part of the range management system and works in conjunction with
 * [RangeCalculator] and [RangeManager]. The worker receives a position and range from the
 * RangeManager and is responsible for performing the actual preparation work (e.g., prefetching
 * content, warming caches, or pre-rendering UI components) for items within that range.
 *
 * ## Lifecycle
 * The typical lifecycle of a RangeWorker is:
 * 1. [schedule] is called when items need to be prepared within a range
 * 2. [cancel] is called to cancel scheduled or ongoing preparation work
 * 3. [destroy] is called to clean up and release all resources
 *
 * @param Position The type representing the current position or viewport state (e.g., scroll
 *   position, visible item indices)
 * @param Range The type representing the range of items to prepare (e.g., IntRange for list
 *   indices)
 */
interface RangeWorker<in Position : Any, in Range : Any> {

  /**
   * Schedules preparation work for items within the specified range at the given position.
   *
   * This method is called by [RangeManager] when the range needs to be prepared. Implementations
   * should schedule any necessary work to prepare items within the range by calling
   * [PreparableCollectionItem.prepare] on items that need preparation and
   * [PreparableCollectionItem.unprepare] on items that are no longer in the range.
   *
   * The actual execution of preparation work may be delayed based on lifecycle state or other
   * conditions. Implementations should respect lifecycle constraints and only execute work when
   * appropriate.
   *
   * ## Threading
   * This method must be called on the UI thread and should return quickly. Heavy preparation work
   * should be dispatched to background threads.
   *
   * ## Incremental Updates
   * This method may be called multiple times as the position changes. Implementations should handle
   * incremental updates efficiently, only preparing/unpreparing items that have changed between
   * calls rather than redoing all work.
   *
   * @param position The current position or viewport state in the collection
   * @param range The range of items to prepare
   */
  @UiThread fun schedule(position: Position, range: Range)

  /**
   * Cancels any scheduled or ongoing preparation work.
   *
   * This method is called by [RangeManager] when preparation work should be cancelled. It
   * invalidates work that has not started, but cannot interrupt a lifecycle callback already in
   * progress. An asynchronous implementation may finish or compensate that callback before it
   * becomes idle.
   */
  fun cancel()

  /**
   * Resets the worker's internal range state.
   *
   * This invalidates the current target after the underlying data source changes. Implementations
   * may retain exact prepared items still owned by the provider, or items whose lifecycle is owned
   * by a non-retaining provider, so the next [schedule] can reconcile them. Entries already evicted
   * and released by the provider are forgotten without a second
   * [PreparableCollectionItem.unprepare].
   */
  fun reset()

  /**
   * Destroys the worker and releases all associated resources.
   *
   * This method is called when the worker is no longer needed. After it returns, the worker rejects
   * new work and no longer exposes owned items through its provider cache. Asynchronous
   * implementations may finish an active callback and drain final unprepare work later on their
   * dispatcher; callers must not assume all lifecycle callbacks completed before this method
   * returns.
   */
  fun destroy()
}

/**
 * Represents a single unit of work for preparing or releasing an item.
 *
 * This data class encapsulates all information needed to perform a single prepare or unprepare
 * operation on a collection item. The item is fetched on the main thread and stored here to avoid
 * accessing the potentially-changing collection on the background thread.
 *
 * This generic design allows the same infrastructure to support both index-based and key-based
 * collections without adapters or additional overhead.
 *
 * @property key The key identifying this item in the collection. Used for cache eviction when the
 *   item moves out of range.
 * @property item The preparable item to work on
 * @property type The type of work to perform (prepare or unprepare)
 * @property execute The lambda that performs the actual work. Called by workers with the
 *   [PrepareScope], [item], and [key] as parameters. Can include custom logic like cache eviction
 *   after unprepare.
 */
class ItemWork(
    val key: Any,
    val item: PreparableCollectionItem,
    val type: WorkType,
    val execute: (scope: PrepareScope, item: PreparableCollectionItem, key: Any) -> Unit,
)

/**
 * Type of work to perform on a collection item.
 *
 * @property Prepare Indicates the item should be prepared (made ready for display)
 * @property Unprepare Indicates the item should be unprepared (free resources)
 */
enum class WorkType {
  Prepare,
  Unprepare,
}
