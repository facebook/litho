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

import com.meta.systrace.NoOpSystracer
import com.meta.systrace.Systracer
import com.meta.systrace.trace
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A synchronous worker implementation for preparing items in collections.
 *
 * This implementation of [RangeWorker] performs all preparation work synchronously on the calling
 * thread. Unlike [HandlerRangeWorker], it does not use background threads and completes all work
 * immediately when [schedule] is called.
 *
 * The work queue building strategy is provided via a [WorkQueueBuilder] to define how to prioritize
 * and order items for preparation based on their specific position and range types.
 *
 * ## Use Cases
 * This worker is suitable when:
 * - The calling code is responsible for managing threading during item preparation
 * - Preparation work is very lightweight and won't block the UI
 * - You need deterministic, immediate preparation (e.g., for testing)
 * - Thread synchronization is important for your use case
 *
 * @param Position The type representing the current position or viewport state
 * @param Range The type representing the range of items to prepare
 * @property workQueueBuilder Strategy for building the work queue that determines preparation order
 * @property scope The [PrepareScope] context passed to [PreparableCollectionItem.prepare] and
 *   [PreparableCollectionItem.unprepare] methods during work execution
 * @property tracer Optional tracer for debugging and performance monitoring. Defaults to
 *   [NoOpSystracer] if not provided.
 */
open class SynchronousRangeWorker<Position : Any, Range : Any>(
    private val workQueueBuilder: WorkQueueBuilder<Position, Range>,
    private val scope: PrepareScope,
    private val tracer: Systracer = NoOpSystracer(),
    private val observer: PreparationHealthObserver? = null,
) : RangeWorker<Position, Range> {

  protected var currentRange: Range? = null
  private val isRunning: AtomicBoolean = AtomicBoolean(false)

  /**
   * Prepares items within the specified range synchronously on the calling thread.
   *
   * This method builds a work queue using the [workQueueBuilder] and processes all items
   * immediately. Unlike asynchronous workers, all preparation work completes before this method
   * returns.
   *
   * ## Threading
   * This method must be called on the UI thread. All work is performed synchronously on the calling
   * thread.
   *
   * ## Behavior
   * - Builds a work queue by comparing the previous range with the new range
   * - Executes prepare/unprepare operations for each work item in order
   * - Updates the current range ONLY if all work completes successfully
   * - Can be interrupted mid-execution if [cancel] is called
   *
   * ## Cancellation Handling
   * If [cancel] is called during execution, the work loop exits early and [currentRange] is NOT
   * updated. This is critical to prevent data loss:
   *
   * **Without conditional update:**
   * 1. Schedule range A (completes successfully, currentRange = A)
   * 2. Schedule range B, but cancel mid-execution (only 50% of items processed)
   * 3. If currentRange was set to B, the next schedule(C) would diff from B → C
   * 4. Problem: buildWorkQueue thinks all items in B were prepared, but they weren't!
   * 5. Result: Some items that should be prepared/unprepared are skipped (data loss)
   *
   * **With conditional update:**
   * 1. Schedule range A (completes successfully, currentRange = A)
   * 2. Schedule range B, but cancel mid-execution (only 50% of items processed)
   * 3. currentRange remains A (reflects actual state)
   * 4. Next schedule(C) diffs from A → C (correct!)
   * 5. Result: buildWorkQueue generates the correct work based on actual previous state
   *
   * @param position The current visible position in the collection
   * @param range The range of item indices to prepare
   */
  override fun schedule(position: Position, range: Range) {
    isRunning.set(true)
    val hadPreviousRange = currentRange != null
    val workList =
        tracer.trace({ "range-worker:init" }) {
          workQueueBuilder.buildWorkQueue(
              previousRange = currentRange ?: range,
              newPosition = position,
              newRange = range,
          )
        }

    val prepareCount = workList.count { it.type == WorkType.Prepare }
    val unprepareCount = workList.count { it.type == WorkType.Unprepare }
    var workCompleted = false
    tracer.trace(
        {
          "range-worker:execute(prepare=$prepareCount,unprepare=$unprepareCount,hadPrev=$hadPreviousRange)"
        },
    ) {
      for (work in workList) {
        if (!isRunning.get()) {
          // Cancellation detected - exit early without completing all work
          break
        }
        tracer.trace({ "work:${work.type}" }) { work.execute(scope, work.item, work.key) }
        observer?.let { obs ->
          when (work.type) {
            WorkType.Prepare -> obs.onItemPrepared(work.key, PreparationSource.RANGE_SCHEDULE)
            WorkType.Unprepare -> obs.onItemUnprepared(work.key, UnprepareReason.RANGE_EVICTION)
          }
        }
      }
      // Only mark work as completed if we processed all items (didn't break early).
      // If cancel() was called, isRunning is false and workCompleted remains false.
      workCompleted = isRunning.get()
    }

    // Only update currentRange if all work completed successfully.
    // This ensures currentRange always reflects the actual state of prepared items.
    // If we updated currentRange even when cancelled, the next schedule() call would
    // generate an incorrect work queue based on a state that was never reached.
    if (workCompleted) {
      currentRange = range
    }
    isRunning.set(false)
  }

  /**
   * Cancels the currently executing work.
   *
   * Sets the running flag to false, which will cause the work loop in [schedule] to exit early.
   * This is a thread-safe operation that can be called from any thread.
   *
   * ## Behavior
   * When cancelled, the [schedule] method will:
   * 1. Exit the work loop after completing the current work item
   * 2. NOT update [currentRange] to preserve accurate state
   *
   * This ensures that the next [schedule] call will correctly diff from the actual previous state
   * (the range that was fully completed) rather than from a partially-completed range, preventing
   * data loss. See [schedule] documentation for detailed explanation.
   *
   * Note: Since work is performed synchronously, cancellation only takes effect between work items,
   * not during the execution of a single prepare/unprepare operation.
   */
  override fun cancel() {
    isRunning.set(false)
  }

  /**
   * Resets the worker to its initial state.
   *
   * This method cancels any ongoing work and clears the current range state, effectively returning
   * the worker to the same state as when it was first created. After calling reset(), the next
   * [schedule] call will treat it as if no previous range existed.
   *
   * This is useful when you need to:
   * - Clear all preparation state without destroying the worker
   * - Start fresh with a new collection or data set
   * - Recover from an error state
   *
   * Unlike [destroy], the worker remains usable after calling reset().
   */
  override fun reset() {
    cancel()
    currentRange = null
  }

  /**
   * Destroys the worker and releases all resources.
   *
   * This method:
   * - Cancels any ongoing work by calling [cancel]
   * - Clears the current range state
   *
   * Since this worker doesn't use background threads or other resources, cleanup is minimal. After
   * calling this method, the worker should not be used again.
   */
  override fun destroy() {
    cancel()
    currentRange = null
  }
}
