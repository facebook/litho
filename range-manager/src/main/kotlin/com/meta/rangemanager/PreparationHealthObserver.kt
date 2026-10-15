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
 * Observer for preparation health lifecycle events.
 *
 * Passed as an optional parameter to [RangeManager] and worker constructors. All methods use [Any]
 * for keys to avoid generics complexity across infrastructure layers — `key.toString()` is
 * sufficient for logging and correlation.
 *
 * Debug-only: when the observer is null, zero overhead (caller null-checks before invoking).
 * Callbacks run on the thread that performs each lifecycle action, which may be a background worker
 * thread. Implementations that retain state must be thread-safe.
 */
interface PreparationHealthObserver {

  /** Called when an item is registered in the collection. */
  fun onItemRegistered(key: Any)

  /**
   * Called after an item has been prepared by the worker.
   *
   * @param key Stable item identifier.
   * @param source What triggered preparation.
   */
  fun onItemPrepared(key: Any, source: PreparationSource)

  /**
   * Called when an item is bound to a view.
   *
   * @param key Stable item identifier.
   * @param wasPrepared true if the item had been prepared before bind.
   */
  fun onItemBound(key: Any, wasPrepared: Boolean)

  /**
   * Called after an item has been unprepared by the worker.
   *
   * @param key Stable item identifier.
   * @param reason Why the item was unprepared.
   */
  fun onItemUnprepared(key: Any, reason: UnprepareReason)

  /**
   * Called when a framework-specific event occurs (e.g., Litho LATE/WRONG failures).
   *
   * Framework modules define typed [PreparationHealthEvent] implementations and report them through
   * this method. The observer stores them as-is and delegates formatting to `toString()`.
   *
   * @param key Stable item identifier.
   * @param event A typed [PreparationHealthEvent] from the framework module.
   */
  fun reportEvent(key: Any, event: PreparationHealthEvent)

  /** Called when the user performs their first scroll. */
  fun onFirstScroll()
}
