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

/**
 * Key-based range worker that resolves items on the caller thread and reconciles their lifecycle on
 * a dedicated worker thread. Pending same-key intents coalesce; later range or lifecycle updates
 * may invalidate accepted work before it starts.
 */
class AsynchronousKeyBasedRangeWorker<K : Any>
private constructor(
    private val itemProvider: CollectionItemProvider<K>,
    private val delegate: PreparedStateRangeWorker<Any, KeyBasedRange<K>, K>,
) : KeyPreparingRangeWorker<K> {

  constructor(
      itemProvider: CollectionItemProvider<K>,
      scope: PrepareScope = PrepareScope(),
      tracer: Systracer = NoOpSystracer(),
      observer: PreparationHealthObserver? = null,
  ) : this(
      itemProvider = itemProvider,
      scope = scope,
      tracer = tracer,
      observer = observer,
      dispatcher = HandlerThreadRangeWorkDispatcher("AsynchronousKeyBasedRangeWorker"),
  )

  internal constructor(
      itemProvider: CollectionItemProvider<K>,
      scope: PrepareScope = PrepareScope(),
      tracer: Systracer = NoOpSystracer(),
      observer: PreparationHealthObserver? = null,
      dispatcher: RangeWorkDispatcher,
  ) : this(
      itemProvider = itemProvider,
      delegate =
          PreparedStateRangeWorker(
              adapter = KeyBasedRangeAdapter(itemProvider),
              scope = scope,
              dispatcher = dispatcher,
              tracer = tracer,
              observer = observer,
          ),
  )

  override fun schedule(position: Any, range: KeyBasedRange<K>) {
    delegate.schedule(position, range)
  }

  override fun prepareKey(key: K) {
    delegate.prepareKey(
        key = key,
        source = PreparationSource.PREPARE_KEY,
        requireCurrentRange = false,
        itemForKey = { itemKey -> itemProvider[itemKey] },
    )
  }

  override fun prepareKeyIfInRange(key: K): Boolean =
      delegate.prepareKey(
          key = key,
          source = PreparationSource.PREPARE_KEY_IN_RANGE,
          requireCurrentRange = true,
          itemForKey = { itemKey -> itemProvider[itemKey] },
      )

  override fun cancel() {
    delegate.cancel()
  }

  override fun reset() {
    delegate.reset()
  }

  override fun destroy() {
    delegate.destroy()
  }
}
