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

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.Mode
import com.meta.rangemanager.PositionChangeEmitter
import com.meta.rangemanager.PrepareScope
import com.meta.rangemanager.RangeManager
import com.meta.rangemanager.WorkerMode
import com.meta.rangemanager.rangeWorkDispatcher

/**
 * Creates a [RangeManager] for linear list layouts.
 *
 * This extension function creates a pre-configured [RangeManager] specifically designed for
 * collections with linear layouts (such as RecyclerView with LinearLayoutManager). It internally
 * creates and wires together:
 * - [ListRangeCalculator] for calculating which items to prepare based on visible position
 * - Either [AsynchronousListRangeWorker] or [SynchronousListRangeWorker] for performing the actual
 *   preparation work, depending on the specified [workerMode]
 *
 * The resulting range manager will prepare items ahead of and behind the currently visible items
 * based on the configuration provided.
 *
 * @param itemProvider Provider for items by index. Must extend [ListCollectionItemProvider].
 * @param config Configuration specifying how many items to prepare behind (before first visible)
 *   and ahead (after last visible) of the visible range. Behind items are at lower indices, ahead
 *   items are at higher indices.
 * @param scope Optional [PrepareScope] context passed to [PreparableCollectionItem.prepare] and
 *   [PreparableCollectionItem.unprepare] methods. Defaults to [PrepareScope].
 * @param workerMode Specifies how preparation work should be executed. Defaults to
 *   [WorkerMode.Sync] which executes on the calling thread. Use [WorkerMode.Async] for background
 *   thread execution.
 * @return A [RangeManager] configured for list-based range preparation with [ListPosition] and
 *   [IntRange] types
 *
 * Example usage:
 * ```
 * // Define your list provider
 * class MyListProvider(private val items: List<Data>) : ListCollectionItemProvider() {
 *     override val size: Int get() = items.size
 *     override fun create(key: Int): PreparableCollectionItem = createItem(items[key])
 * }
 *
 * // Create a range manager that prepares 10 items behind and 20 items ahead
 * val rangeManager = RangeManager.ListBased(
 *     itemProvider = MyListProvider(items),
 *     config = ListRangeConfig(behind = 10, ahead = 20),
 * )
 *
 * // Use it to manage range preparation
 * rangeManager.recalculateRange(ListPosition(currentFirstVisible = 5, currentLastVisible = 15))
 *
 * // Clean up when done
 * rangeManager.destroy()
 * ```
 */
fun RangeManager.Companion.ListBased(
    itemProvider: ListCollectionItemProvider,
    config: ListRangeConfig,
    scope: PrepareScope = PrepareScope(),
    workerMode: WorkerMode = WorkerMode.Sync,
): RangeManager<ListPosition, IntRange> {
  val worker =
      when (workerMode) {
        is WorkerMode.Async ->
            AsynchronousListRangeWorker(
                itemProvider = itemProvider,
                scope = scope,
                dispatcher = workerMode.rangeWorkDispatcher("AsynchronousListRangeWorker"),
            )
        WorkerMode.Sync -> SynchronousListRangeWorker(itemProvider = itemProvider, scope = scope)
      }
  return RangeManager(
      ListRangeCalculator(config, itemProvider),
      worker,
      itemProvider,
      scope,
  )
}

/**
 * Creates a [RangeManager] for linear list layouts, wired to a [RecyclerView]'s scroll events.
 *
 * This convenience function automatically subscribes to the [recyclerView]'s scroll callbacks via
 * [listPositionEmitter] and feeds position changes into [RangeManager.recalculateRange]. The caller
 * no longer needs to set up an [RecyclerView.OnScrollListener] manually.
 *
 * When [lifecycleOwner] is provided, [RangeManager.destroy] is called automatically on
 * [Lifecycle.Event.ON_DESTROY], preventing leaks. When `null`, the caller is responsible for
 * calling [RangeManager.destroy].
 *
 * ```kotlin
 * // Activity / Fragment with auto-cleanup
 * val rangeManager = RangeManager.bindToRecyclerView(
 *     itemProvider = myProvider,
 *     recyclerView = recyclerView,
 *     lifecycleOwner = this,
 * )
 * ```
 *
 * @param itemProvider Provider for items by index. Must extend [ListCollectionItemProvider].
 * @param recyclerView The RecyclerView whose scroll events drive range recalculation. Its
 *   [RecyclerView.getLayoutManager] must be a [LinearLayoutManager]; an [IllegalStateException] is
 *   thrown on scroll if it is not.
 * @param config Configuration specifying how many items to prepare behind and ahead of the visible
 *   range. Defaults to 5 items behind and 10 items ahead.
 * @param lifecycleOwner Optional owner whose [Lifecycle.Event.ON_DESTROY] triggers
 *   [RangeManager.destroy]. Pass `null` to manage cleanup manually.
 * @param scope Optional [PrepareScope] for item preparation. Defaults to [PrepareScope].
 * @param workerMode Execution strategy for preparation work. Defaults to [WorkerMode.Sync].
 * @return A [RangeManager] configured for list-based range preparation
 * @see listPositionEmitter
 */
fun RangeManager.Companion.bindToRecyclerView(
    itemProvider: ListCollectionItemProvider,
    recyclerView: RecyclerView,
    config: ListRangeConfig = ListRangeConfig(behind = 5, ahead = 10),
    lifecycleOwner: LifecycleOwner? = null,
    scope: PrepareScope = PrepareScope(),
    workerMode: WorkerMode = WorkerMode.Sync,
): RangeManager<ListPosition, IntRange> {
  return bindToRecyclerView(
      itemProvider,
      recyclerView.listPositionEmitter(),
      config,
      lifecycleOwner,
      scope,
      workerMode,
  )
}

/**
 * Creates a [RangeManager] for linear list layouts, wired to a custom [PositionChangeEmitter].
 *
 * Use this overload when position events come from a source other than a standard RecyclerView
 * scroll listener — for example, a custom scrolling widget or a test harness. The [RangeManager]
 * subscribes to [positionChangeEmitter] once and calls [RangeManager.recalculateRange] on every
 * emitted position.
 *
 * When [lifecycleOwner] is provided, [RangeManager.destroy] is called automatically on
 * [Lifecycle.Event.ON_DESTROY], preventing leaks. When `null`, the caller is responsible for
 * calling [RangeManager.destroy].
 *
 * ```kotlin
 * // Custom position source with auto-cleanup
 * val rangeManager = RangeManager.bindToRecyclerView(
 *     itemProvider = myProvider,
 *     positionChangeEmitter = { onChange -> myScrollSource.listen { onChange(it) } },
 *     lifecycleOwner = this,
 * )
 * ```
 *
 * @param itemProvider Provider for items by index. Must extend [ListCollectionItemProvider].
 * @param positionChangeEmitter A [PositionChangeEmitter] that delivers [ListPosition] updates.
 *   [PositionChangeEmitter.subscribe] is called exactly once during construction.
 * @param config Configuration specifying how many items to prepare behind and ahead of the visible
 *   range. Defaults to 5 items behind and 10 items ahead.
 * @param lifecycleOwner Optional owner whose [Lifecycle.Event.ON_DESTROY] triggers
 *   [RangeManager.destroy]. Pass `null` to manage cleanup manually.
 * @param scope Optional [PrepareScope] for item preparation. Defaults to [PrepareScope].
 * @param workerMode Execution strategy for preparation work. Defaults to [WorkerMode.Sync].
 * @return A [RangeManager] configured for list-based range preparation
 */
fun RangeManager.Companion.bindToRecyclerView(
    itemProvider: ListCollectionItemProvider,
    positionChangeEmitter: PositionChangeEmitter<ListPosition>,
    config: ListRangeConfig = ListRangeConfig(behind = 5, ahead = 10),
    lifecycleOwner: LifecycleOwner? = null,
    scope: PrepareScope = PrepareScope(),
    workerMode: WorkerMode = WorkerMode.Sync,
): RangeManager<ListPosition, IntRange> {
  val rangeManager = ListBased(itemProvider, config, scope, workerMode)
  positionChangeEmitter.subscribe(rangeManager::recalculateRange)
  lifecycleOwner
      ?.lifecycle
      ?.addObserver(
          object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
              if (event == Lifecycle.Event.ON_DESTROY) {
                rangeManager.destroy()
                source.lifecycle.removeObserver(this)
              }
            }
          },
      )
  return rangeManager
}

/**
 * Creates a [PositionChangeEmitter] backed by this RecyclerView's scroll events.
 *
 * Each scroll callback reads the first and last visible item positions from the attached
 * [LinearLayoutManager] and emits a [ListPosition]. The listener's lifetime is tied to the
 * RecyclerView itself.
 *
 * @return A [PositionChangeEmitter] that emits [ListPosition] on every scroll event
 * @throws IllegalStateException if the RecyclerView's layout manager is not a [LinearLayoutManager]
 */
fun RecyclerView.listPositionEmitter(): PositionChangeEmitter<ListPosition> =
    PositionChangeEmitter { onPositionChange ->
      addOnScrollListener(
          object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
              val layoutManager =
                  recyclerView.layoutManager as? LinearLayoutManager
                      ?: error("listPositionEmitter() requires a LinearLayoutManager")
              val firstVisible = layoutManager.findFirstVisibleItemPosition()
              if (firstVisible == RecyclerView.NO_POSITION) {
                return
              }
              onPositionChange(
                  ListPosition(
                      firstVisible,
                      layoutManager.findLastVisibleItemPosition(),
                  ),
              )
            }
          },
      )
    }

/**
 * Represents the current visible position in a linear list.
 *
 * This data class encapsulates the viewport state for list-based collections, defining which items
 * are currently visible to the user. It is used by [ListRangeCalculator] to determine which
 * additional items should be prepared.
 *
 * @property currentFirstVisible The index of the first fully or partially visible item in the list
 * @property currentLastVisible The index of the last fully or partially visible item in the list
 *
 * Example:
 * ```
 * // If items 5-15 are visible on screen
 * val position = ListPosition(
 *   currentFirstVisible = 5,
 *   currentLastVisible = 15
 * )
 * ```
 */
@DataClassGenerate(toString = Mode.OMIT, equalsHashCode = Mode.KEEP)
data class ListPosition(val currentFirstVisible: Int, val currentLastVisible: Int)

/**
 * Configuration for list range preparation behavior.
 *
 * This data class defines how many items to prepare ahead of and behind the currently visible
 * range. These buffer sizes help ensure smooth scrolling by preparing content before it becomes
 * visible.
 *
 * @property behind Number of items to prepare before the first visible item (lower indices). For
 *   example, if behind=10 and item 20 is first visible, items 10-19 will be prepared.
 * @property ahead Number of items to prepare after the last visible item (higher indices). For
 *   example, if ahead=5 and item 30 is last visible, items 31-35 will be prepared.
 *
 * Example:
 * ```
 * // Prepare 10 items behind (before) and 20 items ahead (after) visible range
 * val config = ListRangeConfig(behind = 10, ahead = 20)
 *
 * // More aggressive prefetching for fast scrolling
 * val aggressiveConfig = ListRangeConfig(behind = 50, ahead = 30)
 *
 * // Minimal prefetching to conserve resources
 * val minimalConfig = ListRangeConfig(behind = 5, ahead = 5)
 * ```
 */
@DataClassGenerate(toString = Mode.OMIT, equalsHashCode = Mode.KEEP)
data class ListRangeConfig(val behind: Int, val ahead: Int)
