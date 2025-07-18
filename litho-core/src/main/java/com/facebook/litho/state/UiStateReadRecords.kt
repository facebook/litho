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
import androidx.collection.MutableLongSet
import androidx.collection.MutableScatterSet
import androidx.collection.ScatterSet
import androidx.collection.mutableLongSetOf
import androidx.collection.mutableScatterMapOf
import androidx.collection.mutableScatterSetOf
import com.facebook.rendercore.BinderId
import com.facebook.rendercore.BinderObserver

/**
 * A [BinderObserver] responsible for tracking and recording state reads in binders.
 *
 * The [UiStateReadRecords] keeps a live record of which state is read and by who. These records are
 * exclusively those that are read during the UI phases (namely Mount and Draw). These records may
 * be used to determine which binders or draw hosts need to be re-bound or invalidated respectively,
 * when a state that they read is updated, which can help to significantly optimize execution in the
 * respective phases.
 *
 * @param config Supplies the read tracking status and current treeId to the observer.
 */
@MainThread
internal class UiStateReadRecords(private val config: Config) : BinderObserver() {

  private val onBindRecords = mutableScatterMapOf<StateId, MutableScatterSet<BinderId>>()
  private val onDrawRecords = mutableScatterMapOf<StateId, MutableLongSet>()

  override fun onBind(binderId: BinderId, func: () -> Unit) {
    if (!config.isReadTrackingEnabled) return func()

    val treeId = config.currentTreeId()
    val reads =
        StateReadRecorder.record(
            treeId,
            debugInfo = {
              put("phase", "bind")
              put("reader.binder", binderId)
              put("reader.owner", binderId.renderUnitDebugDescription?.invoke())
            },
            func)
    synchronized(this) {
      reads.forEach { stateId ->
        val binderIds = onBindRecords.getOrPut(stateId) { mutableScatterSetOf() }
        binderIds.add(binderId)
      }
    }
  }

  override fun onUnbind(binderId: BinderId, func: () -> Unit) {
    func()
    if (!config.isReadTrackingEnabled) return

    synchronized(this) {
      val stateIdsToRemove = mutableSetOf<StateId>()
      onBindRecords.forEach { stateId, binderIds ->
        val removed = binderIds.remove(binderId)
        if (removed && binderIds.isEmpty()) stateIdsToRemove.add(stateId)
      }
      stateIdsToRemove.forEach { onBindRecords.remove(it) }
    }
  }

  fun recordOnDraw(hostId: Long, description: (() -> String?)?, func: () -> Unit) {
    if (!config.isReadTrackingEnabled) return func()

    val treeId = config.currentTreeId()
    val reads =
        StateReadRecorder.record(
            treeId,
            debugInfo = {
              put("phase", "draw")
              put("reader.host", hostId)
              put("reader.owner", description?.invoke())
            },
            func)
    reads.forEach { stateId ->
      val hostIds = onDrawRecords.getOrPut(stateId) { mutableLongSetOf() }
      hostIds.add(hostId)
    }
  }

  fun removeDrawScope(hostId: Long) {
    if (!config.isReadTrackingEnabled) return

    synchronized(this) {
      val stateIdsToRemove = mutableSetOf<StateId>()
      onDrawRecords.forEach { stateId, hostIds ->
        val removed = hostIds.remove(hostId)
        if (removed && hostIds.isEmpty()) stateIdsToRemove.add(stateId)
      }
      stateIdsToRemove.forEach { onDrawRecords.remove(it) }
    }
  }

  fun hasBindersForState(dirtyStates: Set<StateId>): Boolean {
    return synchronized(this) {
      dirtyStates.any { stateId -> onBindRecords[stateId]?.isNotEmpty() == true }
    }
  }

  fun takeBinderSnapshotForState(dirtyStates: Set<StateId>): ScatterSet<BinderId> {
    val result = mutableScatterSetOf<BinderId>()
    synchronized(this) {
      dirtyStates.forEach { stateId ->
        val binderIds = onBindRecords[stateId]
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
