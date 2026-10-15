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
 * A generic handler-based worker implementation for preparing items in collections.
 *
 * This implementation of [RangeWorker] provides the common threading and task management
 * infrastructure for preparing collection items on a background thread. Production work is
 * dispatched through a dedicated Android HandlerThread.
 *
 * The work queue building strategy is provided via a [WorkQueueBuilder] to define how to prioritize
 * and order items for preparation based on their specific position and range types.
 *
 * Key features:
 * - Asynchronous preparation: All item preparation work happens on a dedicated background thread
 * - Automatic resource management: Creates and manages its own HandlerThread
 * - Task cancellation: Can cancel and restart work when the range changes
 *
 * ## Threading
 * - [schedule] must be called on the UI thread (enforced by @UiThread annotation on [RangeWorker])
 * - Actual preparation work is dispatched to a dedicated background HandlerThread
 * - [destroy] safely shuts down the background thread
 *
 * @param Position The type representing the current position or viewport state
 * @param Range The type representing the range of items to prepare
 * @property workQueueBuilder Strategy for building the work queue that determines preparation order
 * @property scope The [PrepareScope] context passed to [PreparableCollectionItem.prepare] and
 *   [PreparableCollectionItem.unprepare] methods during work execution
 * @property tracer Optional tracer for debugging and performance monitoring. Defaults to
 *   [NoOpSystracer] if not provided.
 *
 * TODO: Support multiple RangeWorkers (handle thread and handler creation)
 * TODO: Support priority based on last scroll direction
 * TODO: Support weighted interleaving of prepare and unprepare
 * TODO: Support Android Lifecycle awareness
 * TODO: Add error for using destroyed worker
 * TODO: Define task recovery after a preparation callback fails
 * TODO: Add thread priority inheritance logic
 */
open class HandlerRangeWorker<in Position : Any, in Range : Any>(
    private val workQueueBuilder: WorkQueueBuilder<Position, Range>,
    private val scope: PrepareScope,
    private val tracer: Systracer = NoOpSystracer(),
    private val observer: PreparationHealthObserver? = null,
) : RangeWorker<Position, Range> {

  private var dispatcher: RangeWorkDispatcher =
      HandlerThreadRangeWorkDispatcher(this::class.simpleName ?: "HandlerRangeWorker")

  internal constructor(
      workQueueBuilder: WorkQueueBuilder<Position, Range>,
      scope: PrepareScope,
      tracer: Systracer,
      observer: PreparationHealthObserver?,
      dispatcher: RangeWorkDispatcher,
  ) : this(
      workQueueBuilder = workQueueBuilder,
      scope = scope,
      tracer = tracer,
      observer = observer,
  ) {
    this.dispatcher = dispatcher
  }

  /** The current range being prepared, used to calculate incremental changes. */
  private var currentRange: Range? = null

  /** The current task performing preparation work, if any. */
  private var currentTask: Task? = null

  /**
   * Prepares items within the specified range.
   *
   * This method starts a new task to prepare items in the given range. The new task will prepare
   * items based on the work queue built by [workQueueBuilder].
   *
   * All work is performed asynchronously on the background thread.
   *
   * ## Threading
   * This method must be called on the UI thread as required by the [RangeWorker] interface. The
   * method returns quickly after scheduling the work on the background thread.
   *
   * @param position The current visible position in the collection
   * @param range The range of item indices to prepare
   */
  override fun schedule(position: Position, range: Range) {
    val workQueue =
        tracer.trace({ "range-worker:init" }) {
          workQueueBuilder.buildWorkQueue(
              previousRange = currentRange ?: range,
              newPosition = position,
              newRange = range,
          )
        }

    currentTask =
        Task(
            dispatcher = dispatcher,
            workQueue = workQueue,
            scope = scope,
            tracer = tracer,
            onFinish = { currentTask = null },
        )
            .also { it.start() }

    currentRange = range
  }

  /**
   * Stops all preparation work.
   *
   * This method cancels any ongoing preparation task. The background thread continues running and
   * the worker can be reused by calling [schedule] again.
   *
   * To completely shut down the worker and free all resources, call [destroy] instead.
   */
  override fun cancel() {
    currentTask?.cancel()
  }

  /**
   * Resets the worker to its initial state.
   *
   * This method cancels any ongoing preparation work and clears the current range state,
   * effectively resetting the worker as if no range has been scheduled yet. The background thread
   * continues running and the worker can be reused by calling [schedule] again.
   *
   * This is useful when you want to clear the worker's state without destroying it, such as when
   * the underlying data source changes or when you want to start fresh with a new range.
   *
   * To completely shut down the worker and free all resources, call [destroy] instead.
   */
  override fun reset() {
    cancel()
    currentRange = null
  }

  /**
   * Destroys the worker and releases all resources.
   *
   * This method:
   * 1. Cancels any ongoing preparation work
   * 2. Clears the current task and range state
   * 3. Safely shuts down the background HandlerThread
   *
   * After calling this method, the worker should not be used again. This method should be called
   * when the worker is no longer needed to free up resources and prevent memory leaks.
   */
  override fun destroy() {
    currentTask?.cancel()
    currentTask = null
    currentRange = null
    dispatcher.quit()
  }

  /**
   * Internal task that executes preparation and release work for items in a range.
   *
   * This class manages the actual work of preparing and releasing items. It processes one work item
   * per dispatcher callback and can be cancelled if the range changes before completion.
   *
   * @property dispatcher Dispatcher used to post each unit of work
   * @property workQueue Read-only list of work items to prepare/release, ordered by priority
   * @property scope Scope for calling prepare/unprepare on items
   * @property tracer Tracer for performance monitoring
   * @property onFinish Callback invoked when the task completes or is cancelled
   */
  private inner class Task(
      private val dispatcher: RangeWorkDispatcher,
      private val workQueue: List<ItemWork>,
      private val scope: PrepareScope,
      private val tracer: Systracer,
      private val onFinish: () -> Unit,
  ) {

    /** Current index in the work list being processed. */
    private var currentIndex: Int = 0

    /** Flag indicating whether the task is currently running. */
    private val isRunning: AtomicBoolean = AtomicBoolean(false)

    /** Returns true if the task has been cancelled after being started. */
    val isCancelled: Boolean
      get() = !isRunning.get()

    /** Starts the task by marking it as running and scheduling the first work item. */
    fun start() {
      isRunning.set(true)
      prepareNextItem()
    }

    /** Schedules the next item in the queue to be processed by the dispatcher. */
    private fun prepareNextItem() {
      dispatcher.post { run() }
    }

    /**
     * Processes the next work item in the list.
     *
     * This method is called on the background thread. It gets the next item at the current index,
     * performs the prepare or release operation, and schedules the next item. If all items have
     * been processed or the task has been cancelled, it stops processing.
     */
    private fun run() {
      if (isCancelled) {
        return
      }

      if (currentIndex >= workQueue.size) {
        markAsFinished()
        return
      }

      val work = workQueue[currentIndex]
      currentIndex++

      tracer.trace({ "work:${work.type})" }) { work.execute(scope, work.item, work.key) }
      observer?.let { obs ->
        when (work.type) {
          WorkType.Prepare -> obs.onItemPrepared(work.key, PreparationSource.RANGE_SCHEDULE)
          WorkType.Unprepare -> obs.onItemUnprepared(work.key, UnprepareReason.RANGE_EVICTION)
        }
      }
      prepareNextItem()
    }

    /** Marks the task as finished and invokes the completion callback. */
    private fun markAsFinished() {
      isRunning.set(false)
      onFinish()
    }

    /** Cancels the task, preventing further work items from being processed. */
    fun cancel() {
      tracer.trace({ "range-worker:cancel" }) { isRunning.set(false) }
    }
  }
}
