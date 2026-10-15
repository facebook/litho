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
 * Interface for building work queues that determine how items should be prepared and released.
 *
 * This interface defines the strategy for creating an ordered list of preparation and release work
 * items based on the current position, previous range, and new range. Implementations can define
 * custom prioritization logic to optimize for their specific use case (e.g., list-based
 * collections, grid layouts, etc.).
 *
 * The return type is a read-only [List] to ensure immutability - implementations should return
 * immutable lists or use Kotlin's list builders (e.g., `listOf()`, `buildList {}`) to prevent
 * external modification of the work queue.
 *
 * @param Position The type representing the current position or viewport state
 * @param Range The type representing the range of items to prepare
 */
fun interface WorkQueueBuilder<in Position : Any, in Range : Any> {

  /**
   * Builds a work queue for preparing and releasing items.
   *
   * This method determines which items should be prepared or released and in what order. The
   * implementation should:
   * 1. Determine which items in the new range need to be prepared
   * 2. Determine which items from the previous range should be released (unprepared)
   * 3. Order the work items by priority (e.g., items closest to the visible viewport first)
   *
   * The returned list should be ordered such that the first item has the highest priority and
   * should be processed first.
   *
   * **Important:** Return an immutable list to prevent external modification. Use `listOf()`,
   * `buildList {}`, or convert mutable lists using `toList()`.
   *
   * @param previousRange The previously prepared range, or the new range if this is the first call
   * @param newPosition The current position in the collection
   * @param newRange The new range to prepare
   * @return A read-only list of work items ordered by priority (first item has highest priority)
   */
  fun buildWorkQueue(
      previousRange: Range,
      newPosition: Position,
      newRange: Range,
  ): List<ItemWork>
}
