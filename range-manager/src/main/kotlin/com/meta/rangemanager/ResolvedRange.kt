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

/** One key and the exact item instance resolved for it on the scheduling thread. */
internal class ResolvedItem<K : Any>(
    val key: K,
    val item: PreparableCollectionItem,
)

/**
 * An immutable target range with stable preparation order and constant-time key membership.
 * Duplicate keys are discarded after their first occurrence.
 */
internal class ResolvedRange<K : Any>
private constructor(
    val items: List<ResolvedItem<K>>,
    private val itemsByKey: Map<K, ResolvedItem<K>>,
    private val releaseComparator: Comparator<K>?,
) {
  operator fun contains(key: K): Boolean = itemsByKey.containsKey(key)

  fun captures(key: K, item: PreparableCollectionItem): Boolean = itemsByKey[key]?.item === item

  fun orderedReleaseKeys(preparedKeys: Set<K>): List<K> {
    val releaseKeys = preparedKeys.filterNot { key -> key in this }
    return releaseComparator?.let { comparator -> releaseKeys.sortedWith(comparator) }
        ?: releaseKeys
  }

  internal companion object {
    fun <K : Any> fromKeys(
        orderedKeys: Iterable<K>,
        releaseComparator: Comparator<K>? = null,
        itemForKey: (K) -> PreparableCollectionItem,
    ): ResolvedRange<K> {
      val itemsByKey = LinkedHashMap<K, ResolvedItem<K>>()
      orderedKeys.forEach { key ->
        if (!itemsByKey.containsKey(key)) {
          itemsByKey[key] = ResolvedItem(key, itemForKey(key))
        }
      }
      return ResolvedRange(
          items = itemsByKey.values.toList(),
          itemsByKey = itemsByKey.toMap(),
          releaseComparator = releaseComparator,
      )
    }
  }
}

/** Resolves a domain range and owns identity-safe provider eviction for its items. */
internal interface RangeAdapter<in Position : Any, in Range : Any, K : Any> {
  /** Resolves every target item while [schedule][RangeWorker.schedule] is still on its caller. */
  fun resolve(position: Position, range: Range): ResolvedRange<K>

  /** Whether exact eviction is atomic, or unnecessary because the provider retains no items. */
  val supportsExactEviction: Boolean

  /**
   * Returns whether reset should carry this exact item into its replacement ledger.
   *
   * This is a continuity check, not a cleanup-ownership check. Cleanup ownership is transferred
   * only through [claimReleaseIfSame].
   */
  fun isCachedSame(key: K, item: PreparableCollectionItem): Boolean

  /**
   * Atomically transfers responsibility for unpreparing [item] from the provider to the worker.
   * Providers without a cache return `true` because that responsibility is already worker-owned.
   */
  fun claimReleaseIfSame(key: K, item: PreparableCollectionItem): Boolean

  /**
   * Evicts [item] only if that exact instance remains owned by the provider for [key], and returns
   * whether it did.
   *
   * The identity check and removal must complete atomically before invoking any external callback.
   */
  fun evictIfSame(key: K, item: PreparableCollectionItem): Boolean

  /**
   * Tells the provider an item the worker removed ([claimReleaseIfSame] then unprepare, or
   * [evictIfSame]) is gone. Called after the release completes, outside the worker's locks.
   */
  fun notifyRemoved(key: K, item: PreparableCollectionItem): Unit = Unit
}
