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

import com.meta.rangemanager.HandlerThreadRangeWorkDispatcher
import com.meta.rangemanager.PrepareScope
import com.meta.rangemanager.PreparedStateRangeWorker
import com.meta.rangemanager.RangeWorkDispatcher
import com.meta.rangemanager.RangeWorker
import com.meta.systrace.NoOpSystracer
import com.meta.systrace.Systracer

/**
 * Asynchronously reconciles a list range against the exact items that completed preparation.
 *
 * Items are resolved on the scheduling thread and prepared one at a time on a dedicated background
 * thread. Successfully prepared items remain known across overlapping schedules, so they are not
 * prepared again until they are released or the worker is reset.
 */
class AsynchronousListRangeWorker
private constructor(
    private val worker: PreparedStateRangeWorker<ListPosition, IntRange, Int>,
) : RangeWorker<ListPosition, IntRange> {

  constructor(
      itemProvider: ListCollectionItemProvider,
      scope: PrepareScope,
      tracer: Systracer = NoOpSystracer(),
  ) : this(
      PreparedStateRangeWorker(
          adapter = ListRangeAdapter(itemProvider),
          scope = scope,
          dispatcher = HandlerThreadRangeWorkDispatcher("AsynchronousListRangeWorker"),
          tracer = tracer,
      ),
  )

  internal constructor(
      itemProvider: ListCollectionItemProvider,
      scope: PrepareScope,
      tracer: Systracer = NoOpSystracer(),
      dispatcher: RangeWorkDispatcher,
  ) : this(
      PreparedStateRangeWorker(
          adapter = ListRangeAdapter(itemProvider),
          scope = scope,
          dispatcher = dispatcher,
          tracer = tracer,
      ),
  )

  override fun schedule(position: ListPosition, range: IntRange) {
    worker.schedule(position, range)
  }

  override fun cancel() {
    worker.cancel()
  }

  override fun reset() {
    worker.reset()
  }

  override fun destroy() {
    worker.destroy()
  }
}
