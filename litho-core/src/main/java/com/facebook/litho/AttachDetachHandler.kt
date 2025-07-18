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

package com.facebook.litho

import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.collection.ScatterSet
import androidx.collection.emptyScatterSet
import com.facebook.infer.annotation.ThreadSafe
import com.facebook.litho.annotations.OnAttached
import com.facebook.litho.annotations.OnDetached
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.debug.DebugInfoReporter
import com.facebook.litho.state.StateId
import com.facebook.litho.state.StateReadRecorder

/**
 * Manages dispatching attach/detach events to a set of [Attachable].
 *
 * For Spec Components, this will invoke [OnAttached] when a component is attached to the
 * [ComponentTree] and [OnDetached] when a component is detached from the tree.
 *
 * For Kotlin components, this will handle dispatching callbacks registered with the `useEffect`
 * hook.
 */
@ThreadSafe
class AttachDetachHandler {

  private var _attached: MutableMap<String, Attachable>? = null

  @get:VisibleForTesting
  val attached: Map<String, Attachable>?
    get() = _attached

  /**
   * Marks the given Attachables as attached, invoking attach if they weren't already attached. Any
   * Attachables that were attached and are no longer attached will be detached. Note that identity
   * is determined by [Attachable#getUniqueId()].
   */
  @UiThread
  fun onAttached(treeId: Int, attachables: List<Attachable>?) {
    ThreadUtils.assertMainThread()
    if (_attached == null && attachables == null) {
      return
    }

    // 1. if there is nothing to attach, then we must detach all current attachables
    if (attachables == null) {
      _attached?.let { detachAll(treeId, it) }
      _attached = null
      return
    }

    val newAttachablesMap = LinkedHashMap<String, Attachable>()
    attachables.associateByTo(newAttachablesMap) { it.uniqueId }

    // 2. if we have no current attachables, we can simply attach all the new ones.
    if (_attached.isNullOrEmpty()) {
      attachAll(treeId, newAttachablesMap)
      _attached = newAttachablesMap
      return
    }

    // 3.1 detach attachables no longer present
    val currentAttached = _attached ?: return

    for ((id, attachable) in currentAttached) {
      if (id !in newAttachablesMap) {
        attachable.detachWithTracking(treeId)
      }
    }

    // 3.2. attach or update attachables
    for (entry in newAttachablesMap) {
      val (id, newAttachable) = entry
      val existing = currentAttached[id]

      when {
        existing == null -> newAttachable.attachWithTracking(treeId)
        existing.shouldUpdate(newAttachable) -> {
          existing.detachWithTracking(treeId)
          newAttachable.attachWithTracking(treeId)
        }
        !existing.useLegacyUpdateBehavior() -> {
          // If the attachable already exists and it doesn't need to update, make sure to use the
          // existing one.
          entry.setValue(existing)
        }
      }
    }

    // 3. update the map reference
    _attached = newAttachablesMap
  }

  /** Detaches all Attachables currently attached. */
  @UiThread
  fun onDetached(treeId: Int) {
    ThreadUtils.assertMainThread()
    _attached?.let { detachAll(treeId, it) }
    _attached = null
  }

  private fun attachAll(treeId: Int, toAttach: Map<String, Attachable>) {
    for (entry in toAttach.values) {
      entry.attachWithTracking(treeId)
    }
  }

  private fun detachAll(treeId: Int, toDetach: Map<String, Attachable>) {
    for (entry in toDetach.values) {
      entry.detachWithTracking(treeId)
    }
  }

  private fun Attachable.attachWithTracking(treeId: Int) {
    val stateReads =
        executeInTrackingScope(
            treeId,
            debugInfo = {
              put("phase", "attachEffect")
              put("reader.owner", decodeComponentName())
            },
            func = ::attach)

    if (stateReads.isNotEmpty())
        DebugInfoReporter.report("StateReadTracking:runEffect", renderStateId = treeId) {
          put("component", decodeComponentName())
          put("stateReadCount", stateReads.size)
        }
  }

  private fun Attachable.detachWithTracking(treeId: Int) {
    val stateReads =
        executeInTrackingScope(
            treeId,
            debugInfo = {
              put("phase", "detachEffect")
              put("reader.owner", decodeComponentName())
            },
            func = ::detach)

    if (stateReads.isNotEmpty())
        DebugInfoReporter.report("StateReadTracking:disposeEffect", renderStateId = treeId) {
          put("component", decodeComponentName())
          put("stateReadCount", stateReads.size)
        }
  }

  private inline fun executeInTrackingScope(
      treeId: Int,
      noinline debugInfo: MutableMap<String, Any?>.() -> Unit,
      crossinline func: () -> Unit
  ): ScatterSet<StateId> {
    return if (ComponentsConfiguration.defaultInstance.enableStateReadTracking) {
      StateReadRecorder.record(treeId, debugInfo) { func() }
    } else {
      func()
      emptyScatterSet()
    }
  }

  private fun Attachable.decodeComponentName(): String {
    val componentKey = uniqueId.substringBeforeLast(':')
    return Component.generateHierarchy(componentKey).lastOrNull() ?: "null"
  }
}
