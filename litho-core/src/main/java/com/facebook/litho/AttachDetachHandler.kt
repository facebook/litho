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
import com.facebook.infer.annotation.ThreadSafe
import com.facebook.litho.annotations.OnAttached
import com.facebook.litho.annotations.OnDetached
import java.util.LinkedHashMap

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
  fun onAttached(attachables: List<Attachable>?) {
    ThreadUtils.assertMainThread()
    if (_attached == null && attachables == null) {
      return
    }

    // 1. if there is nothing to attach, then we must detach all current attachables
    if (attachables == null) {
      _attached?.let { detachAll(it) }
      _attached = null
      return
    }

    val newAttachablesMap = LinkedHashMap<String, Attachable>()
    attachables.associateByTo(newAttachablesMap) { it.uniqueId }

    // 2. if we have no current attachables, we can simply attach all the new ones.
    if (_attached.isNullOrEmpty()) {
      attachAll(newAttachablesMap)
      _attached = newAttachablesMap
      return
    }

    // 3.1 detach attachables no longer present
    val currentAttached = _attached ?: return

    for ((id, attachable) in currentAttached) {
      if (id !in newAttachablesMap) {
        attachable.detach()
      }
    }

    // 3.2. attach or update attachables
    for (entry in newAttachablesMap) {
      val (id, newAttachable) = entry
      val existing = currentAttached[id]

      when {
        existing == null -> newAttachable.attach()
        existing.shouldUpdate(newAttachable) -> {
          existing.detach()
          newAttachable.attach()
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
  fun onDetached() {
    ThreadUtils.assertMainThread()
    _attached?.let { detachAll(it) }
    _attached = null
  }

  private fun attachAll(toAttach: Map<String, Attachable>) {
    for (entry in toAttach.values) {
      entry.attach()
    }
  }

  private fun detachAll(toDetach: Map<String, Attachable>) {
    for (entry in toDetach.values) {
      entry.detach()
    }
  }
}
