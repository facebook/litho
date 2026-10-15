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
import com.meta.systrace.NoOpSystracer
import com.meta.systrace.Systracer

/**
 * Coordinates range calculation and preparation work for collection items.
 *
 * RangeManager connects a [RangeCalculator] (which determines what range to prepare) with a
 * [RangeWorker] (which performs the actual preparation work). Use the [KeyBased] factory method to
 * create instances for key-based collections.
 *
 * ## Flow
 * 1. Position change occurs (e.g., user scrolls)
 * 2. Calculator determines the new range to prepare
 * 3. Worker prepares items within that range (and unprepares items outside)
 *
 * ## Item Provider
 *
 * RangeManager requires a [CollectionItemProvider] for item access. Create one by extending the
 * abstract class:
 * ```kotlin
 * class MyProvider(private val repo: Repository) : CollectionItemProvider<MyKey>() {
 *     override fun create(key: MyKey) = createItem(key)
 *     override fun contains(key: MyKey) = repo.contains(key)
 * }
 * ```
 *
 * ## Usage
 *
 * ```kotlin
 * // Create your item provider (extends CollectionItemProvider)
 * val itemProvider = MyProvider(repo)
 *
 * // Create range manager for key-based collections
 * val rangeManager = RangeManager.KeyBased(
 *     itemProvider = itemProvider,
 *     calculator = myCalculator,
 * )
 *
 * // Update range when position changes (e.g., scroll)
 * rangeManager.recalculateRange(newPosition)
 *
 * // Notify when data changes (e.g., items added/removed)
 * rangeManager.onDataSetChanged(currentPosition)
 *
 * // Access items through the provider
 * val item = itemProvider[key]
 *
 * // Clean up when done
 * rangeManager.destroy()
 * itemProvider.invalidateAll()
 * ```
 *
 * @param Position The type representing the current position or viewport state
 * @param Range The type representing the range of items to prepare
 * @property calculator Determines which range to prepare based on position
 * @property worker Performs preparation work on items in the range
 * @see CollectionItemProvider
 * @see RangeCalculator
 * @see KeyBased
 */
class RangeManager<Position : Any, Range : Any>(
    private val calculator: RangeCalculator<Position, Range>,
    private val worker: RangeWorker<Position, Range>,
    private val itemProvider: CollectionItemProvider<*>,
    private val scope: PrepareScope = PrepareScope(),
    private val observer: PreparationHealthObserver? = null,
) {

  private var lastRange: Range? = null
  private var lastPosition: Position? = null

  /**
   * Recalculates the range and restarts preparation work from scratch.
   *
   * This method should be called when a full recalculation is needed, such as when the collection
   * data changes, configuration changes, or when you need to ensure all previous preparation work
   * is cancelled before starting new work.
   *
   * The method:
   * 1. Calculates a new range via [RangeCalculator.calculateRange]
   * 2. Skips work if the range is unchanged from the last calculation
   * 3. Cancels any ongoing preparation work via [RangeWorker.cancel]
   * 4. Schedules preparation work for the new range via [RangeWorker.schedule]
   *
   * ## Threading
   * This method must be called on the UI thread as it invokes [RangeWorker.schedule] which requires
   * UI thread execution.
   */
  @UiThread
  fun recalculateRange(newPosition: Position) {
    val newRange = calculator.calculateRange(newPosition)
    if (newRange == lastRange) {
      return
    }
    lastRange = newRange
    lastPosition = newPosition
    worker.cancel()
    worker.schedule(newPosition, newRange)
  }

  /**
   * Notifies that the underlying data source has changed and triggers range recalculation.
   *
   * Call this when items are added, removed, or reordered in the data source. This method
   * coordinates three operations that must happen together:
   * 1. **Evict stale items**: Removes cached items where [CollectionItemProvider.contains] returns
   *    false and cleans up associated state (e.g., cached TreeState via [EvictionAwareScope])
   * 2. **Reset worker state**: Clears the worker's tracked range so it doesn't try to unprepare
   *    already-evicted items on the next schedule
   * 3. **Recalculate range**: Computes a fresh range and prepares items from scratch
   *
   * ## Why not just call [recalculateRange]?
   *
   * After data changes, the worker's internal `currentRange` contains stale keys for items that no
   * longer exist. Without resetting, the worker would diff against this stale range and attempt to
   * access removed items through the provider, triggering [create] for non-existent data. This
   * method ensures the worker starts fresh.
   *
   * ## Threading
   * This method must be called on the UI thread.
   *
   * @param newPosition The current position to recalculate the range from. If null, the last
   *   position passed to [recalculateRange] is used. If no position is available, this is a no-op.
   */
  @UiThread
  fun onDataSetChanged(newPosition: Position? = null) {
    // 1. Evict items that no longer exist in the data source
    itemProvider.onDataSetChanged()
    // 2. Clear the worker's stale range state to prevent accessing evicted items
    worker.reset()
    // 3. Reset range cache so recalculateRange doesn't skip (data changed → same range may
    //    contain different items)
    lastRange = null
    // 4. Recalculate and prepare with a fresh range
    val positionToUse = newPosition ?: lastPosition
    positionToUse?.let { recalculateRange(it) }
  }

  /**
   * Releases resources and cancels all ongoing preparation work.
   *
   * This method should be called when the RangeManager is no longer needed, such as when the
   * component using it is being destroyed or disposed. It ensures that any background preparation
   * work is properly cancelled to prevent memory leaks and unnecessary processing.
   */
  fun destroy() {
    worker.cancel()
    worker.destroy()
    scope.clearAll()
    lastRange = null
    lastPosition = null
  }

  companion object {
    /**
     * Creates a RangeManager for key-based collections.
     *
     * This factory creates a RangeManager configured with a [KeyBasedRangeWorker] that uses keys
     * (rather than indices) to identify items. This is the recommended way to create a RangeManager
     * for most use cases.
     *
     * ## Item Provider
     *
     * The [itemProvider] must extend [CollectionItemProvider]:
     * ```kotlin
     * class MyProvider(private val repo: Repository) : CollectionItemProvider<MyKey>() {
     *     override fun create(key: MyKey) = createItem(key)
     *     override fun contains(key: MyKey) = repo.contains(key)
     * }
     *
     * val itemProvider = MyProvider(repo)
     * val rangeManager = RangeManager.KeyBased(
     *     itemProvider = itemProvider,
     *     calculator = myCalculator,
     * )
     *
     * // Access items through itemProvider
     * val item = itemProvider[key]
     * ```
     *
     * @param itemProvider A [CollectionItemProvider] for items by key
     * @param calculator Range calculator for determining preparation range
     * @param scope Optional prepare scope for item preparation. Defaults to [PrepareScope].
     * @param workerMode Whether preparation runs on the calling thread ([WorkerMode.Sync], the
     *   default) or in the background ([WorkerMode.Async], optionally on a caller's executor).
     * @return A RangeManager configured for key-based collections
     * @see CollectionItemProvider
     */
    fun <K : Any, P : Any, R : KeyBasedRange<K>> Companion.KeyBased(
        itemProvider: CollectionItemProvider<K>,
        calculator: RangeCalculator<P, R>,
        scope: PrepareScope = PrepareScope(),
        observer: PreparationHealthObserver? = null,
        workerMode: WorkerMode = WorkerMode.Sync,
    ): RangeManager<P, R> {
      return RangeManager(
          calculator = calculator,
          worker =
              keyBasedWorker(
                  itemProvider = itemProvider,
                  scope = scope,
                  observer = observer,
                  workerMode = workerMode,
              ),
          itemProvider = itemProvider,
          scope = scope,
          observer = observer,
      )
    }

    /**
     * Creates a key-based worker that callers can retain for [KeyPreparingRangeWorker.prepareKey].
     *
     * Pass the returned worker to the public [RangeManager] constructor so direct key requests and
     * range schedules use the same instance. [WorkerMode.Sync] remains the default; select
     * [WorkerMode.Async] explicitly when delayed, latest-per-key reconciliation is required. Use
     * [KeyBased] when direct worker access is unnecessary.
     */
    fun <K : Any> keyBasedWorker(
        itemProvider: CollectionItemProvider<K>,
        scope: PrepareScope = PrepareScope(),
        observer: PreparationHealthObserver? = null,
        workerMode: WorkerMode = WorkerMode.Sync,
        tracer: Systracer = NoOpSystracer(),
        // Sync only; the async worker always releases them (see KeyBasedRangeWorker).
        releaseOutOfRangeOnReset: Boolean = false,
    ): KeyPreparingRangeWorker<K> =
        when (workerMode) {
          WorkerMode.Sync ->
              KeyBasedRangeWorker(
                  itemProvider = itemProvider,
                  scope = scope,
                  tracer = tracer,
                  observer = observer,
                  releaseOutOfRangeOnReset = releaseOutOfRangeOnReset,
              )
          is WorkerMode.Async ->
              AsynchronousKeyBasedRangeWorker(
                  itemProvider = itemProvider,
                  scope = scope,
                  tracer = tracer,
                  observer = observer,
                  dispatcher = workerMode.rangeWorkDispatcher("AsynchronousKeyBasedRangeWorker"),
              )
        }
  }
}
