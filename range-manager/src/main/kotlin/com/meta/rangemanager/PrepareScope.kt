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

import androidx.annotation.GuardedBy

/**
 * Provides context for preparing and releasing collection items.
 *
 * This class is used as a receiver in [PreparableCollectionItem.prepare] and
 * [PreparableCollectionItem.unprepare] methods, providing state save/restore capabilities needed
 * during item preparation and cleanup operations. It is passed to [RangeWorker] implementations to
 * coordinate preparation lifecycle across collection items.
 *
 * ## State Persistence
 *
 * PrepareScope provides state save/restore methods that allow items to persist state across
 * recreation. The key is managed by the infrastructure via [prepare]/[unprepare] — items simply
 * call [saveState] and [getSavedState] without needing to know or provide their own key. This
 * prevents items from saving state under arbitrary keys.
 *
 * ## Composite Items
 *
 * For [CompositeCollectionItem], each child item needs its own state namespace despite sharing the
 * same parent key. [CompositeCollectionItem] uses [prepareChild]/[unprepareChild] which
 * automatically scope each child under a [CompoundKey], so child items get isolated state without
 * any action from item developers.
 *
 * ## Thread Safety
 *
 * All state operations are synchronized for safe access from multiple threads. The current key is
 * stored per-thread via [ThreadLocal] because [HandlerRangeWorker] runs prepare/unprepare on a
 * background HandlerThread while [CollectionItemProvider.invalidate] or
 * [CollectionItemProvider.onDataSetChanged] may call prepare/unprepare from the UI thread
 * concurrently on the same scope instance. Without ThreadLocal, one thread's key assignment would
 * clobber the other's, causing [saveState]/[getSavedState] to read or write the wrong slot.
 *
 * @see PreparableCollectionItem
 * @see RangeWorker
 */
open class PrepareScope {

  @GuardedBy("this") private val savedState = mutableMapOf<Any, Any?>()

  /**
   * Per-thread current key.
   *
   * ThreadLocal is required because [HandlerRangeWorker] calls [prepare]/[unprepare] on a
   * background HandlerThread while [CollectionItemProvider] may call them from the UI thread
   * simultaneously — a plain field would let one thread's key clobber the other's.
   */
  private val threadLocalKey = ThreadLocal<Any?>()

  private var currentKey: Any?
    get() = threadLocalKey.get()
    set(value) {
      threadLocalKey.set(value)
    }

  /**
   * Sets the current key, executes [block], then restores the previous key. The key is restored
   * even if [block] throws.
   */
  private fun <T> withKey(key: Any, block: () -> T): T {
    val previousKey = currentKey
    currentKey = key
    try {
      return block()
    } finally {
      currentKey = previousKey
    }
  }

  /**
   * Prepares [item] with [key] as the current key context.
   *
   * Sets [currentKey] so that [saveState]/[getSavedState] calls inside [item]'s prepare use the
   * correct key, then delegates to [PreparableCollectionItem.prepare].
   *
   * @param key Stable identifier for the item.
   * @param item The item to prepare.
   */
  internal fun prepare(key: Any, item: PreparableCollectionItem) {
    withKey(key) { with(item) { prepare() } }
  }

  /**
   * Unprepares [item] with [key] as the current key context.
   *
   * Sets [currentKey] so that [saveState]/[getSavedState] calls inside [item]'s unprepare use the
   * correct key, then delegates to [PreparableCollectionItem.unprepare].
   *
   * @param key Stable identifier for the item.
   * @param item The item to unprepare.
   */
  internal fun unprepare(key: Any, item: PreparableCollectionItem) {
    withKey(key) { with(item) { unprepare() } }
  }

  /**
   * Prepares a child [item] within a composite, scoped under a [CompoundKey].
   *
   * Combines the current parent key with [subKey] to give the child its own state namespace. Used
   * by [CompositeCollectionItem] so each child gets isolated state automatically.
   *
   * @param subKey Identifier for the child item within the composite.
   * @param item The child item to prepare.
   */
  internal fun prepareChild(subKey: Any, item: PreparableCollectionItem) {
    val parentKey = currentKey
    if (parentKey != null) {
      prepare(CompoundKey(parentKey, subKey), item)
    } else {
      with(item) { prepare() }
    }
  }

  /**
   * Unprepares a child [item] within a composite, scoped under a [CompoundKey].
   *
   * Combines the current parent key with [subKey] to give the child its own state namespace. Used
   * by [CompositeCollectionItem] so each child gets isolated state automatically.
   *
   * @param subKey Identifier for the child item within the composite.
   * @param item The child item to unprepare.
   */
  internal fun unprepareChild(subKey: Any, item: PreparableCollectionItem) {
    val parentKey = currentKey
    if (parentKey != null) {
      unprepare(CompoundKey(parentKey, subKey), item)
    } else {
      with(item) { unprepare() }
    }
  }

  /**
   * Saves state for the current item.
   *
   * Call this during [PreparableCollectionItem.unprepare] to persist state that should survive item
   * recreation.
   *
   * @param state The state to save, or null to clear saved state for this item.
   * @throws IllegalStateException if called outside of prepare/unprepare.
   */
  @Synchronized
  fun saveState(state: Any?) {
    val key = checkNotNull(currentKey) { "saveState must be called within prepare/unprepare" }
    if (state != null) {
      savedState[key] = state
    } else {
      savedState.remove(key)
    }
  }

  /**
   * Retrieves saved state for the current item.
   *
   * Call this during [PreparableCollectionItem.prepare] to restore state that was saved during a
   * previous [PreparableCollectionItem.unprepare] call.
   *
   * @return The saved state, or null if no state was saved for this item.
   * @throws IllegalStateException if called outside of prepare/unprepare.
   */
  @Synchronized
  fun getSavedState(): Any? {
    val key = checkNotNull(currentKey) { "getSavedState must be called within prepare/unprepare" }
    return savedState[key]
  }

  /**
   * Clears saved state for the given key and any compound keys derived from it.
   *
   * Called by the infrastructure when an item is permanently evicted and will not be recreated.
   * This also clears state for any child items in a [CompositeCollectionItem] whose [CompoundKey]s
   * have this key as their parent.
   *
   * @param key Stable identifier for the item.
   */
  @Synchronized
  internal fun clearSavedState(key: Any) {
    savedState.remove(key)
    // Also remove any compound keys that have this as their parent
    savedState.keys.removeAll { it is CompoundKey && it.parent == key }
  }

  /** Clears all saved state. Call this when the entire collection is being destroyed. */
  @Synchronized
  fun clearAll() {
    savedState.clear()
  }
}

/**
 * A compound key that scopes a child item's state under a parent key.
 *
 * Used internally by [PrepareScope.prepareChild]/[PrepareScope.unprepareChild] to give each child
 * in a [CompositeCollectionItem] its own state namespace. For example, a composite with parent key
 * "item-5" and sub-keys "header"/"content" produces CompoundKey("item-5", "header") and
 * CompoundKey("item-5", "content").
 */
private data class CompoundKey(val parent: Any, val sub: Any)
