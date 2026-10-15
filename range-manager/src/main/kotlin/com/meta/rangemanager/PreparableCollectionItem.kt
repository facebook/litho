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
 * General interface for preparing and unpreparing collection items.
 *
 * This interface defines the contract for items that can be asynchronously prepared ahead of time
 * (e.g., before they become visible) and cleaned up when no longer needed. The range manager uses
 * this interface to optimize performance by pre-loading items that are about to become visible and
 * releasing resources for items that have scrolled out of view.
 *
 * Both methods use [PrepareScope] as a receiver to provide access to shared context during
 * preparation operations.
 */
interface PreparableCollectionItem {

  /**
   * Prepares the item for display.
   *
   * This method is called by the range manager when an item is about to become visible or needs to
   * be pre-loaded. Implementations should perform any expensive operations needed before the item
   * is displayed, such as:
   * - Pre-computing view hierarchies
   * - Loading or decoding assets
   * - Initializing expensive components
   *
   * This method is called on a background thread and should be idempotent - calling it multiple
   * times should be safe. If preparation throws, the implementation must release any resources it
   * acquired before throwing; the range manager does not call [unprepare] for a failed preparation.
   */
  fun PrepareScope.prepare()

  /**
   * Releases resources associated with this item.
   *
   * This method is called by the range manager when an item is no longer visible or needed.
   * Implementations should clean up any resources allocated during [prepare], such as:
   * - Releasing pre-computed view hierarchies
   * - Freeing loaded assets
   * - Disposing temporary resources
   *
   * This method is called on a background thread and should be idempotent - calling it multiple
   * times should be safe.
   */
  fun PrepareScope.unprepare()
}
