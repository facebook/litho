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

import android.util.SparseArray
import androidx.annotation.GuardedBy

/** Keeps all valid instances of [EventTrigger] from the hierarchy when the layout is completed */
class EventTriggersContainer {

  @GuardedBy("this") private var eventTriggers: MutableMap<Any, EventTrigger<*>>? = null

  @GuardedBy("this")
  private var handleEventTriggers: MutableMap<Handle, SparseArray<EventTrigger<*>>>? = null

  /**
   * Record an [EventTrigger] according to its key.
   *
   * @param trigger
   */
  fun recordEventTrigger(trigger: EventTrigger<*>?) {
    if (trigger == null) {
      return
    }

    synchronized(this) {
      if (eventTriggers == null) {
        eventTriggers = HashMap()
      }

      if (handleEventTriggers == null) {
        handleEventTriggers = HashMap()
      }

      eventTriggers?.put(trigger.key, trigger)

      if (trigger.handle != null) {
        handleEventTriggers?.getOrPut(trigger.handle) { SparseArray() }?.put(trigger.id, trigger)
      }
    }
  }

  @Synchronized
  private fun getEventTriggerInternal(triggerKeyOrHandle: Any): EventTrigger<*>? {
    return eventTriggers?.get(triggerKeyOrHandle)
  }

  /**
   * Retrieve and return an [EventTrigger] based on the given key.
   *
   * @param triggerKey
   * @return EventTrigger with the triggerKey given.
   */
  fun getEventTrigger(triggerKey: String): EventTrigger<*>? {
    return getEventTriggerInternal(triggerKey)
  }

  /**
   * Retrieve and return an [EventTrigger] based on the given handle.
   *
   * @param handle
   * @return EventTrigger with the handle given.
   */
  @Synchronized
  fun getEventTrigger(handle: Handle, methodId: Int): EventTrigger<*>? {
    val handleTriggers = handleEventTriggers ?: return null
    val triggers = handleTriggers[handle] ?: return null
    return triggers[methodId]
  }

  @Synchronized
  fun clear() {
    eventTriggers?.clear()
    handleEventTriggers?.clear()
  }
}
