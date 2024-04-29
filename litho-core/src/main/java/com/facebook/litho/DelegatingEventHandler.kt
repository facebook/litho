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

import androidx.annotation.VisibleForTesting
import com.facebook.litho.annotations.EventHandlerRebindMode
import java.lang.reflect.Modifier.PROTECTED

/**
 * An event handler that takes two event handlers and calls both of them when an event is
 * dispatched.
 */
class DelegatingEventHandler<E>
@VisibleForTesting(otherwise = PROTECTED)
constructor(eventHandler1: EventHandler<E>, eventHandler2: EventHandler<E>) :
    EventHandler<E>(-1, EventHandlerRebindMode.NONE, EventDispatchInfo(null, null), null) {
  private val eventHandlers: MutableList<EventHandler<E>> =
      arrayListOf(eventHandler1, eventHandler2)

  override fun dispatchEvent(event: E): Any? {
    for (i in 0 until eventHandlers.size) {
      eventHandlers[i].dispatchEvent(event)
    }
    return null
  }

  override fun isEquivalentTo(other: EventHandler<*>?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null) {
      return false
    }
    if (other.javaClass != javaClass) {
      return false
    }
    val list: MutableList<out EventHandler<out Any?>>? =
        (other as? DelegatingEventHandler<*>)?.eventHandlers
    val size = eventHandlers.size
    if (size != list?.size) {
      return false
    }
    return (0 until size).none { i -> !eventHandlers[i].isEquivalentTo(list[i]) }
  }

  fun addEventHandler(eventHandler: EventHandler<E>): DelegatingEventHandler<E> {
    eventHandlers.add(eventHandler)
    return this
  }
}
