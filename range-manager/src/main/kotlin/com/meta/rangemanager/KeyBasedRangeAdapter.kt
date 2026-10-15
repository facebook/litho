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

/** Resolves key ranges on the caller thread and evicts only the exact captured item. */
internal class KeyBasedRangeAdapter<K : Any>(
    private val itemProvider: CollectionItemProvider<K>,
) : RangeAdapter<Any, KeyBasedRange<K>, K> {

  override fun resolve(position: Any, range: KeyBasedRange<K>): ResolvedRange<K> =
      ResolvedRange.fromKeys(
          orderedKeys = range.keys,
          itemForKey = { key -> itemProvider[key] },
      )

  override val supportsExactEviction: Boolean
    get() = itemProvider.supportsExactEviction

  override fun isCachedSame(key: K, item: PreparableCollectionItem): Boolean =
      itemProvider.isCachedSame(key, item)

  override fun claimReleaseIfSame(key: K, item: PreparableCollectionItem): Boolean =
      itemProvider.claimReleaseIfSame(key, item)

  override fun evictIfSame(key: K, item: PreparableCollectionItem): Boolean =
      itemProvider.evictIfSame(key, item)

  override fun notifyRemoved(key: K, item: PreparableCollectionItem) {
    itemProvider.notifyRemoved(key, item)
  }
}
