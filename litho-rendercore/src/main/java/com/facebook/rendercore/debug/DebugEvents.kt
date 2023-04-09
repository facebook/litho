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

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Base class of all debug events */
sealed class DebugEvent(
    val type: String,
    val renderStateId: String,
    val threadName: String = Thread.currentThread().name,
    private val attributes: Map<String, Any?> = emptyMap()
) {

  companion object {
    const val All = "*"
    const val RenderTreeCommitted = "RenderCore.RenderTreeCommitted"
    const val RenderTreeCalculated = "RenderCore.RenderTreeCalculated"
    const val RenderTreeMounted = "RenderCore.RenderTreeMounted"
    const val MountItemMount = "RenderCore.MountItem.Mount"
    const val RenderUnitMounted = "RenderCore.RenderUnit.Mounted"
    const val RenderUnitUnmounted = "RenderCore.RenderUnit.Unmounted"
  }

  /** Returns the value of attribute with [name]. */
  fun <T> attribute(name: String): T = attributes[name] as T

  /** Returns the value of attribute with [name] or `null` if not present. */
  fun <T> attributeOrNull(name: String): T? = attributes[name] as? T

  /**
   * Returns the value of the attribute with [name]. If there is no value present, then it will
   * return the [default].
   */
  fun <T> attribute(name: String, default: T): T = attributeOrNull(name) ?: default

  override fun toString(): String {
    return """
      |[DebugEvent]
      |  type = '$type',
      |  renderStateId = '$renderStateId',
      |  thread = '$threadName',
      |  attributes = ${attributes.entries.joinToString(
        prefix = "{\n",
        separator = ",\n",
        postfix = "\n|  }",
        transform = { e -> "|    ${e.key} = ${e.value}" }
    )}
    """
        .trimMargin()
  }
}

/** Collection of attributes */
object DebugEventAttribute {
  const val timestamp = "timestamp"
  const val duration = "duration"
  const val version = "version"
  const val width = "width"
  const val height = "height"
  const val source = "source"
  const val RenderUnitId = "renderUnitId"
  const val Description = "description"
  const val Bounds = "bounds"
}

/** Base class for marker events */
class DebugMarkerEvent(
    val timestamp: Long = System.currentTimeMillis(), // for calendar time
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
    val timestamp: Long = System.currentTimeMillis(), // for calendar time
    val duration: Duration,
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
              put(DebugEventAttribute.duration, duration)
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
      timestamp: Long = System.currentTimeMillis(), // for calender time
      attributesFiller: AttributesFiller = AttributesFiller {},
  ) {
    if (enabled) {
      val attributes = LinkedHashMap<String, Any?>()
      attributesFiller.fillAttributes(attributes)

      val event =
          DebugMarkerEvent(
              type = type,
              renderStateId = renderStateId,
              attributes = attributes,
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
      attributesFiller: AttributesFiller = AttributesFiller {}
  ) {
    dispatch(
        type = type,
        renderStateId = renderStateId,
        timestamp = System.currentTimeMillis(), // for calender time
        attributesFiller = attributesFiller,
    )
  }

  @JvmStatic
  inline fun <T> trace(
      type: String,
      renderStateId: String,
      attributesFiller: AttributesFiller = AttributesFiller {},
      block: (TraceScope?) -> T,
  ): T {

    // run the block if the event bus is disabled
    if (!enabled) {
      return block(null)
    }

    // find the subscribers listening for this event
    val subscribersToNotify =
        subscribers.filter { subscriber ->
          subscriber.events.contains(type) || subscriber.events.contains(DebugEvent.All)
        }

    // run the block if there are no subscribers
    if (subscribersToNotify.isEmpty()) {
      return block(null)
    }

    val attributes = LinkedHashMap<String, Any?>()
    attributesFiller.fillAttributes(attributes)

    val startTime = System.nanoTime()
    val res = block(TraceScope(attributes = attributes))
    val endTime = System.nanoTime()

    val event =
        DebugProcessEvent(
            type = type,
            renderStateId = renderStateId,
            duration = Duration(value = endTime - startTime),
            attributes = attributes,
        )

    subscribersToNotify.forEach { it.onEvent(event) }

    return res
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

/**
 * This consumer interface is used so that clients can fill the [DebugEvent] attributes map in a
 * cleaner way in both Java and Kotlin.
 *
 * By doing this (and not use a lambda directly), we can guarantee that Java clients are not forced
 * to return `Unit.INSTANCE` or `null`.
 */
fun interface AttributesFiller {

  fun fillAttributes(map: MutableMap<String, Any?>)
}

@JvmInline
value class Duration(val value: Long) {
  override fun toString(): String {
    return if (value < 1_000) {
      "$value ns"
    } else if (value < 1_000_0000) {
      "${TimeUnit.NANOSECONDS.toMicros(value)} µs"
    } else {
      "${TimeUnit.NANOSECONDS.toMillis(value)} ms"
    }
  }
}
