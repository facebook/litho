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

package com.facebook.litho.state

import androidx.annotation.VisibleForTesting
import com.facebook.rendercore.BinderId
import com.facebook.rendercore.BinderType
import com.facebook.rendercore.DepChange

/**
 * A [UiStateReadRecords.BinderEventListener] that tracks per-binder shouldUpdate results and bind
 * durations for observability.
 *
 * Data is accumulated in a thread-local map keyed by renderUnitId. This data is drained by the
 * BinderUpdateDetector when it receives the corresponding RenderUnitUpdated debug event (which
 * fires on the same thread, immediately after all binder operations complete).
 *
 * When [isEnabled] is false (default), callbacks are no-ops with negligible overhead (one static
 * field read per call).
 */
class BinderUpdateTracer : UiStateReadRecords.BinderEventListener {

  override fun onShouldUpdate(binderId: BinderId, result: Boolean) {
    if (isEnabled) {
      addEntry(binderId, shouldUpdate = result, isStateReadRebind = false)
    }
  }

  override fun onStateReadRebind(binderId: BinderId) {
    if (isEnabled) {
      addEntry(binderId, shouldUpdate = true, isStateReadRebind = true)
    }
  }

  override fun onDepsChanged(binderId: BinderId, changes: List<DepChange>) {
    if (isEnabled) {
      updateLastEntry(binderId) { it.copy(depChanges = changes) }
    }
  }

  override fun onDepsNotCollected(binderId: BinderId) {
    if (isEnabled) {
      updateLastEntry(binderId) { it.copy(depsCollected = false) }
    }
  }

  override fun onBindComplete(binderId: BinderId, durationNs: Long) {
    if (isEnabled) {
      // Never creates an entry: initial mount dispatches no RenderUnitUpdated to drain it.
      updateLastEntry(binderId) { it.copy(bindDurationNs = durationNs) }
    }
  }

  private fun addEntry(binderId: BinderId, shouldUpdate: Boolean, isStateReadRebind: Boolean) {
    val map = ensureMap()
    val list = map.getOrPut(binderId.renderUnitId) { mutableListOf() }
    list.add(
        BinderUpdateInfo(
            binderKey = binderName(binderId),
            binderType = binderId.type,
            shouldUpdate = shouldUpdate,
            isStateReadRebind = isStateReadRebind,
        ),
    )
  }

  private fun updateLastEntry(
      binderId: BinderId,
      transform: (BinderUpdateInfo) -> BinderUpdateInfo,
  ) {
    val map = threadLocalData.get() ?: return
    val list = map[binderId.renderUnitId] ?: return
    val key = binderName(binderId)
    val type = binderId.type
    // Target the oldest entry still awaiting a duration, and match the binder type as well as the
    // name. A render unit resolves its attach and mount binders in separate passes before any of
    // them bind, so two entries can be waiting at once - and an unnamed binder is keyed
    // positionally, which makes an attach `binder:0` and a mount `binder:0` collide on name alone.
    val index = list.indexOfFirst {
      it.binderKey == key && it.binderType == type && it.bindDurationNs == null
    }
    if (index >= 0) {
      list[index] = transform(list[index])
    }
  }

  private fun binderName(binderId: BinderId): String {
    return binderId.binderDebugDescription ?: binderId.key.toString()
  }

  private fun ensureMap(): MutableMap<Long, MutableList<BinderUpdateInfo>> {
    var map = threadLocalData.get()
    if (map == null) {
      map = mutableMapOf()
      threadLocalData.set(map)
    }
    return map
  }

  data class BinderUpdateInfo(
      val binderKey: String,
      val binderType: BinderType,
      val shouldUpdate: Boolean,
      val bindDurationNs: Long? = null,
      /** Deps that differed. `null` if custom `shouldUpdate`, empty if layout data. */
      val depChanges: List<DepChange>? = null,
      val isStateReadRebind: Boolean = false,
      /**
       * `collectBindReasons` was on when this binder ran, so a `null` [depChanges] means the binder
       * has no dep model rather than that nobody looked.
       */
      val depsCollected: Boolean = true,
  ) {
    /** Human-readable reason this binder was rebound. */
    fun describeReason(): String =
        when {
          !shouldUpdate -> "skipped"
          isStateReadRebind -> "state read"
          // Without this, every KBinder reads as "custom shouldUpdate" whenever the flag is off,
          // which is the opposite of true.
          !depsCollected -> "reason not collected"
          depChanges == null -> "custom shouldUpdate"
          depChanges.isEmpty() -> "layout data"
          else -> depChanges.joinToString(separator = ", ") { it.describe() }
        }
  }

  companion object {
    @JvmField var isEnabled: Boolean = false

    private val threadLocalData = ThreadLocal<MutableMap<Long, MutableList<BinderUpdateInfo>>>()

    /**
     * Drain accumulated binder data for the given renderUnitId. Returns null if no data was
     * collected (e.g., observability is disabled or no binder operations occurred).
     *
     * This is called by the BinderUpdateDetector when processing a RenderUnitUpdated event.
     */
    @JvmStatic
    fun drainData(renderUnitId: Long): List<BinderUpdateInfo>? {
      return threadLocalData.get()?.remove(renderUnitId)
    }

    @VisibleForTesting
    @JvmStatic
    fun seedTestData(renderUnitId: Long, data: List<BinderUpdateInfo>) {
      var map = threadLocalData.get()
      if (map == null) {
        map = mutableMapOf()
        threadLocalData.set(map)
      }
      map[renderUnitId] = data.toMutableList()
    }
  }
}
