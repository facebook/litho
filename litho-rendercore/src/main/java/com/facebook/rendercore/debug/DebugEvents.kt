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

package com.facebook.rendercore.debug

import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean

/** Base class of all debug events */
sealed class DebugEvent(
    val type: String,
    val renderStateId: String,
    val threadName: String = Thread.currentThread().name,
    val attributes: Map<String, Any?> = emptyMap()
) {

  companion object {
    const val All = "*"
    const val RenderTreeCommitted = "RenderCore.RenderTreeCommitted"
    const val RenderTreeCalculated = "RenderCore.RenderTreeCalculated"
    const val RenderTreeMounted = "RenderCore.RenderTreeMounted"
  }

  fun attribute(name: String): Any? = attributes[name]

  override fun toString(): String {
    return """
      [DebugEvent]
        type='$type',
        renderStateId='$renderStateId',
        thread='$threadName',
        attributes='$attributes'
    """
        .trimIndent()
  }
}

/** Collection of attributes */
object DebugEventAttribute {
  const val timestamp = "timestamp"
  const val startTimestamp = "startTimestamp"
  const val endTimestamp = "endTimestamp"
  const val version = "version"
  const val width = "width"
  const val height = "height"
  const val source = "source"
}

/** Base class for marker events */
class DebugMarkerEvent(
    val timestamp: Long = SystemClock.uptimeMillis(),
    type: String,
    renderStateId: String,
    threadName: String = Thread.currentThread().name,
    attributes: Map<String, Any?> = emptyMap()
) :
    DebugEvent(
        type = type,
        renderStateId = renderStateId,
        threadName = threadName,
        attributes =
            buildMap {
              put(DebugEventAttribute.timestamp, timestamp)
              putAll(attributes)
            })

/** Base class for process events */
class DebugProcessEvent(
    val startTimestamp: Long,
    val endTimestamp: Long,
    type: String,
    renderStateId: String,
    threadName: String = Thread.currentThread().name,
    attributes: Map<String, Any?> = emptyMap()
) :
    DebugEvent(
        type = type,
        renderStateId = renderStateId,
        threadName = threadName,
        attributes =
            buildMap {
              put(DebugEventAttribute.startTimestamp, startTimestamp)
              put(DebugEventAttribute.endTimestamp, endTimestamp)
              putAll(attributes)
            })

/** Base class for event subscribers */
abstract class DebugEventSubscriber(vararg val events: String) {
  abstract fun onEvent(event: DebugEvent)
}

/** Object to dispatch debug events */
object DebugEventDispatcher {

  val _enabled: AtomicBoolean = AtomicBoolean(false)
  var enabled: Boolean
    get() = _enabled.get()
    set(value) {
      _enabled.set(value)
    }

  private val _mutableSubscribers: MutableSet<DebugEventSubscriber> = mutableSetOf()

  val subscribers: Set<DebugEventSubscriber>
    @Synchronized get() = _mutableSubscribers

  @JvmStatic
  inline fun dispatch(
      type: String,
      renderStateId: String,
      timestamp: Long = SystemClock.uptimeMillis(),
      attributes: () -> Map<String, Any?> = { emptyMap() },
  ) {
    if (enabled) {
      val event =
          DebugMarkerEvent(
              type = type,
              renderStateId = renderStateId,
              attributes = attributes(),
              timestamp = timestamp,
          )
      subscribers.forEach { subscriber ->
        if (subscriber.events.contains(type) || subscriber.events.contains(DebugEvent.All)) {
          subscriber.onEvent(event)
        }
      }
    }
  }

  @JvmStatic
  inline fun dispatch(
      type: String,
      renderStateId: String,
      attributes: () -> Map<String, Any?> = { emptyMap() },
  ) {
    dispatch(
        type = type,
        renderStateId = renderStateId,
        timestamp = SystemClock.uptimeMillis(),
        attributes = attributes,
    )
  }

  @JvmStatic
  inline fun trace(
      type: String,
      renderStateId: String,
      attributes: () -> Map<String, Any?> = { emptyMap() },
      block: (TraceScope?) -> Unit,
  ) {

    // run the block if the event bus is disabled
    if (!enabled) {
      block(null)
      return
    }

    // find the subscribers listening for this event
    val subscribersToNotify =
        subscribers.filter { subscriber ->
          subscriber.events.contains(type) || subscriber.events.contains(DebugEvent.All)
        }

    // run the block if there are no subscribers
    if (subscribersToNotify.isEmpty()) {
      block(null)
      return
    }

    val attrs = LinkedHashMap<String, Any?>(attributes())

    val startTime = SystemClock.uptimeMillis()
    block(TraceScope(attributes = attrs))
    val endTime = SystemClock.uptimeMillis()

    val event =
        DebugProcessEvent(
            type = type,
            renderStateId = renderStateId,
            startTimestamp = startTime,
            endTimestamp = endTime,
            attributes = attrs,
        )

    subscribersToNotify.forEach { it.onEvent(event) }
  }

  @Synchronized
  fun subscribe(subscriber: DebugEventSubscriber) {
    _mutableSubscribers.add(subscriber)
  }

  @Synchronized
  internal fun unsubscribe(subscriber: DebugEventSubscriber) {
    _mutableSubscribers.remove(subscriber)
  }

  @Synchronized
  fun unsubscribeAll() {
    _mutableSubscribers.clear()
  }

  class TraceScope(private val attributes: LinkedHashMap<String, Any?>) {
    fun attribute(name: String, value: Any?) {
      attributes[name] = value
    }
  }
}

/** Object to subscribe to debug events */
object DebugEventBus {

  @JvmStatic
  var enabled: Boolean
    @JvmStatic get() = DebugEventDispatcher.enabled
    @JvmStatic
    set(value) {
      DebugEventDispatcher.enabled = value
    }

  @JvmStatic
  inline fun subscribe(block: () -> DebugEventSubscriber) {
    if (enabled) {
      DebugEventDispatcher.subscribe(block())
    }
  }

  @JvmStatic
  inline fun subscribe(subscriber: DebugEventSubscriber) {
    if (enabled) {
      DebugEventDispatcher.subscribe(subscriber)
    }
  }

  @JvmStatic
  fun unsubscribe(subscriber: DebugEventSubscriber) {
    DebugEventDispatcher.unsubscribe(subscriber)
  }

  @JvmStatic
  fun unsubscribeAll() {
    DebugEventDispatcher.unsubscribeAll()
  }
}
