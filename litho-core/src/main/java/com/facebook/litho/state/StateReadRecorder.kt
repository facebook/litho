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

import androidx.collection.MutableScatterSet
import androidx.collection.ScatterSet
import androidx.collection.emptyScatterSet
import androidx.collection.mutableScatterSetOf
import com.facebook.infer.annotation.ThreadSafe
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.debug.DebugInfoReporter
import com.facebook.litho.utils.LithoThreadLocal
import com.facebook.litho.utils.getOrSet

@ThreadSafe
internal class StateReadRecorder private constructor() {

  private var readSet: MutableScatterSet<StateId>? = null
  private var currentId: Int = NO_ID
  private var debugInfo: (MutableMap<String, Any?>.() -> Unit)? = null

  /**
   * Executes [scope] in the context of the current recorder to enable tracking of all state reads
   * during that execution which are then returned as a set of [StateId]s.
   *
   * @param treeId The id of the current tree
   * @param scope The scope to execute
   * @return A set of all state ids that were read during the execution of the scope.
   */
  private fun record(
      treeId: Int,
      debugInfo: MutableMap<String, Any?>.() -> Unit,
      scope: () -> Unit
  ): ScatterSet<StateId> {
    return if (treeId == currentId) {
      // Re-entrant call. Simply run the scope and return an empty set
      // The full read set will be delivered to the parent recorder at the end.
      check(readSet != null)
      scope()
      emptyScatterSet()
    } else {
      mutableScatterSetOf<StateId>().also { newSet ->
        val previousReadSet = readSet
        val previousTreeId = currentId
        val previousDebugInfo = this.debugInfo
        try {
          readSet = newSet
          currentId = treeId
          this.debugInfo = debugInfo
          scope()
        } finally {
          currentId = previousTreeId
          readSet = previousReadSet
          this.debugInfo = previousDebugInfo
        }
      }
    }
  }

  /**
   * Records a [state] that is being read
   *
   * State may be read within the context of an active recorder as long as the the recorder is for
   * the same tree where the state was created. This restriction may be lifted in future.
   *
   * While it's possible to read state without an active recorder (for example during a post-bind
   * operation), such reads will not be tracked.
   *
   * @throws IllegalStateException If state is read from a different tree than where it was created
   */
  private fun read(state: StateId): Boolean {
    val isStateReadFromDifferentTree = currentId != NO_ID && currentId != state.treeId
    val violationPolicy = ComponentsConfiguration.defaultInstance.stateReadViolationPolicy
    if (violationPolicy == ComponentsConfiguration.LogicViolationPolicy.CRASH) {
      check(!isStateReadFromDifferentTree) {
        "State can only be read in the same tree where it was created. " +
            "State tree: ${state.treeId}, Current tree: $currentId"
      }
    } else if (isStateReadFromDifferentTree) {
      DebugInfoReporter.report(
          "StateReadTracking:ReadFromDifferentTree", renderStateId = state.treeId) {
            put("state", state)
            put("state.owner", state.ownerName?.invoke())
            put("reader.treeId", currentId)
            debugInfo?.invoke(this)
          }
      return false
    }
    readSet?.add(state)
    return true
  }

  companion object {
    private const val NO_ID = Int.MIN_VALUE
    private val recorder = LithoThreadLocal<StateReadRecorder>()

    private val current: StateReadRecorder
      get() = recorder.getOrSet { StateReadRecorder() }

    fun record(
        treeId: Int,
        debugInfo: MutableMap<String, Any?>.() -> Unit = {},
        scope: () -> Unit
    ): ScatterSet<StateId> {
      return current.record(treeId, debugInfo, scope)
    }

    fun read(state: StateId): Boolean {
      return current.read(state)
    }
  }
}
