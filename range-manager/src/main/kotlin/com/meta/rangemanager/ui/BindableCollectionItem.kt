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

package com.meta.rangemanager.ui

import android.view.View
import com.meta.rangemanager.PreparableCollectionItem

/**
 * Extension of [PreparableCollectionItem] that adds view binding capabilities for UI components.
 *
 * This interface combines the asynchronous preparation capabilities of [PreparableCollectionItem]
 * with view binding lifecycle methods. It's designed for collection items that need both:
 * 1. Asynchronous preparation/unprepare operations (via [prepare] and [unprepare])
 * 2. Synchronous view binding/unbinding operations (via [bind] and [unbind])
 *
 * Typical lifecycle flow:
 * 1. [prepare] - Called when item enters prepare range (background thread)
 * 2. [bind] - Called when item needs to be bound to a view (main thread)
 * 3. [unbind] - Called when view is recycled (main thread)
 * 4. [unprepare] - Called when item exits prepare range (background thread)
 *
 * This separation allows expensive operations to happen asynchronously during [prepare], while
 * keeping the synchronous [bind] operation lightweight for smooth scrolling performance.
 */
interface BindableCollectionItem : PreparableCollectionItem {

  /**
   * Binds this item's data to the provided view.
   *
   * This method is called on the main thread when the item needs to be rendered. Implementations
   * should:
   * - Apply the item's data to the view's UI elements
   * - Set up any view-specific listeners or callbacks
   * - Use any pre-computed data from [prepare] to minimize work on the main thread
   *
   * This method should be lightweight and avoid expensive operations to maintain smooth scrolling.
   * Heavy operations should be performed in [prepare] instead.
   *
   * @param view The view to bind this item's data to
   */
  fun bind(view: View)

  /**
   * Unbinds this item from its view.
   *
   * This method is called on the main thread when the view is being recycled (typically typically
   * when the item is no longer visible but not necessarily). Implementations should:
   * - Clear any view-specific state or listeners
   * - Reset the view to a clean state for reuse
   * - Release any view-specific resources
   *
   * Note: This does not release the prepared data - use [unprepare] for that. This method only
   * handles view-level cleanup.
   */
  fun unbind()
}
