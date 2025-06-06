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

import androidx.annotation.MainThread
import androidx.collection.MutableScatterSet
import androidx.collection.ScatterSet
import androidx.collection.mutableScatterMapOf
import androidx.collection.mutableScatterSetOf
import com.facebook.rendercore.BinderId
import com.facebook.rendercore.BinderObserver

/**
 * A [BinderObserver] responsible for tracking and recording state reads in binders.
 *
 * The [UiStateReadRecords] keeps a live [records] of which state is read and by who. This may be
 * used to determine which binders need to be re-bound when a state that they read is updated, which
 * can help to significantly optimize binder execution in the mount phase.
 *
 * @param config Supplies the read tracking status and current treeId to the observer.
 */
@MainThread
internal class UiStateReadRecords(private val config: Config) : BinderObserver() {

  private val records = mutableScatterMapOf<StateId, MutableScatterSet<BinderId>>()

  override fun onBind(binderId: BinderId, func: () -> Unit) {
    if (!config.isReadTrackingEnabled) return func()

    val treeId = config.currentTreeId()
    val reads = StateReadRecorder.record(treeId, func)
    synchronized(this) {
      reads.forEach { stateId ->
        val binderIds = records.getOrPut(stateId) { mutableScatterSetOf() }
        binderIds.add(binderId)
      }
    }
  }

  override fun onUnbind(binderId: BinderId, func: () -> Unit) {
    func()
    if (!config.isReadTrackingEnabled) return

    synchronized(this) {
      val stateIdsToRemove = mutableSetOf<StateId>()
      records.forEach { stateId, binderIds ->
        val removed = binderIds.remove(binderId)
        if (removed && binderIds.isEmpty()) stateIdsToRemove.add(stateId)
      }
      stateIdsToRemove.forEach { records.remove(it) }
    }
  }

  fun hasBindersForState(dirtyStates: Set<StateId>): Boolean {
    return synchronized(this) {
      dirtyStates.any { stateId -> records[stateId]?.isNotEmpty() == true }
    }
  }

  fun takeSnapshotForState(dirtyStates: Set<StateId>): ScatterSet<BinderId> {
    val result = mutableScatterSetOf<BinderId>()
    synchronized(this) {
      dirtyStates.forEach { stateId ->
        val binderIds = records[stateId]
        if (binderIds != null) result.addAll(binderIds)
      }
    }
    return result
  }

  interface Config {
    val isReadTrackingEnabled: Boolean

    fun currentTreeId(): Int
  }
}
