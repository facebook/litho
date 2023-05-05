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

import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.rendercore.LogLevel
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Base class of all debug events */
sealed class DebugEvent(
    val type: String,
    val renderStateId: String,
    val threadName: String = Thread.currentThread().name,
    val logLevel: LogLevel = LogLevel.DEBUG,
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
    const val RenderUnitUpdated = "RenderCore.RenderUnit.Updated"
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
  const val widthSpec = "widthSpec"
  const val heightSpec = "heightSpec"
  const val source = "source"
  const val RenderUnitId = "renderUnitId"
  const val Description = "description"
  const val Bounds = "bounds"
  const val RootHostHashCode = "rootHostHashCode"
}

/** Base class for marker events */
class DebugMarkerEvent(
    val timestamp: Long = System.currentTimeMillis(), // for calendar time
    type: String,
    renderStateId: String,
    threadName: String = Thread.currentThread().name,
    logLevel: LogLevel = LogLevel.DEBUG,
    attributes: Map<String, Any?> = emptyMap()
) :
    DebugEvent(
        type = type,
        renderStateId = renderStateId,
        threadName = threadName,
        logLevel = logLevel,
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
    logLevel: LogLevel = LogLevel.DEBUG,
    attributes: Map<String, Any?> = emptyMap()
) :
    DebugEvent(
        type = type,
        renderStateId = renderStateId,
        threadName = threadName,
        logLevel = logLevel,
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

  val enabledRef: AtomicBoolean = AtomicBoolean(false)

  @JvmStatic
  var enabled: Boolean
    @JvmStatic get() = enabledRef.get()
    @JvmStatic
    set(value) {
      enabledRef.set(value)
    }

  private val minLogLevelRef = AtomicReference(LogLevel.DEBUG)

  @JvmStatic
  var minLogLevel: LogLevel
    @JvmStatic get() = minLogLevelRef.get()
    @JvmStatic
    set(value) {
      minLogLevelRef.set(value)
    }

  private val mutableSubscribers: MutableSet<DebugEventSubscriber> = mutableSetOf()

  val subscribers: Set<DebugEventSubscriber>
    @Synchronized get() = mutableSubscribers

  @JvmStatic
  inline fun dispatch(
      type: String,
      renderStateId: () -> String,
      timestamp: Long = System.currentTimeMillis(), // for calender time
      logLevel: LogLevel = LogLevel.DEBUG,
      attributesAccumulator: (MutableMap<String, Any?>) -> Unit = {},
  ) {
    if (enabled && logLevel >= this.minLogLevel) {
      val attributes = LinkedHashMap<String, Any?>()
      attributesAccumulator(attributes)

      val event =
          DebugMarkerEvent(
              type = type,
              renderStateId = renderStateId(),
              attributes = attributes,
              timestamp = timestamp,
              logLevel = logLevel,
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
      renderStateId: () -> String,
      attributesAccumulator: (MutableMap<String, Any?>) -> Unit = {}
  ) {
    dispatch(
        type = type,
        renderStateId = renderStateId,
        timestamp = System.currentTimeMillis(), // for calender time
        attributesAccumulator = attributesAccumulator,
    )
  }

  @JvmStatic
  inline fun dispatch(
      type: String,
      renderStateId: () -> String,
      logLevel: LogLevel = LogLevel.DEBUG,
      attributesAccumulator: (MutableMap<String, Any?>) -> Unit = {}
  ) {
    dispatch(
        type = type,
        renderStateId = renderStateId,
        timestamp = System.currentTimeMillis(), // for calender time
        logLevel = logLevel,
        attributesAccumulator = attributesAccumulator,
    )
  }

  private val lastTraceIdentifier = AtomicInteger(0)
  private val traceIdsToEvents: MutableMap<Int, EventData> = HashMap()

  /**
   * Returns an identifier for a trace for the given [type]. This identifier will always be unique
   * across a session, and if `null` it means this event is not to be traced.
   *
   * This is to be used whenever the [DebugEvent] API is called from Java.
   */
  @JvmStatic
  fun generateTraceIdentifier(type: String): Int? =
      if (enabled && subscribers.any { type in it.events || DebugEvent.All in it.events }) {
        lastTraceIdentifier.getAndIncrement()
      } else {
        null
      }

  /**
   * Starts recording a trace block for the given [type].
   *
   * This is intended to be used with Java clients due to performance reasons. The recommended usage
   * is the following:
   * ```
   * Integer traceIdentifier = DebugEvents.getTraceIdentifier(DebugEvents.MyEvent);
   * if(traceIdentifier != null) {
   *    DebugEvents.beginTrace(traceIdentifier, ...);
   * }
   *
   * // code for the process
   *
   * if(traceIdentifier != null) {
   *   DebugEvents.endTrace(traceIdentifier);
   * }
   * ```
   */
  @JvmStatic
  fun beginTrace(
      traceIdentifier: Int,
      type: String,
      renderStateId: String,
      attributes: Map<String, Any?>,
  ) {
    traceIdsToEvents[traceIdentifier] =
        EventData(
            type = type,
            renderStateId = renderStateId,
            attributes = attributes,
            startTimestamp = System.nanoTime())
  }

  @JvmStatic
  fun endTrace(traceIdentifier: Int) {
    val endTime = System.nanoTime()
    val last = traceIdsToEvents.remove(traceIdentifier) ?: return
    val type = last.type

    // find the subscribers listening for this event
    val subscribersToNotify =
        subscribers.filter { subscriber ->
          subscriber.events.contains(type) || subscriber.events.contains(DebugEvent.All)
        }

    if (subscribersToNotify.isEmpty()) {
      return
    }

    val event =
        DebugProcessEvent(
            type = type,
            timestamp = last.startTimestamp, // for calender time
            renderStateId = last.renderStateId,
            duration = Duration(value = endTime - last.startTimestamp),
            attributes = last.attributes,
        )

    subscribersToNotify.forEach { it.onEvent(event) }
  }

  inline fun <T> trace(
      type: String,
      renderStateId: () -> String,
      attributesAccumulator: (MutableMap<String, Any?>) -> Unit = {},
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
    attributesAccumulator(attributes)

    val startTime = System.nanoTime()
    val res = block(TraceScope(attributes = attributes))
    val endTime = System.nanoTime()

    val event =
        DebugProcessEvent(
            type = type,
            timestamp = startTime,
            renderStateId = renderStateId(),
            duration = Duration(value = endTime - startTime),
            attributes = attributes,
        )

    subscribersToNotify.forEach { it.onEvent(event) }

    return res
  }

  @Synchronized
  fun subscribe(subscriber: DebugEventSubscriber) {
    mutableSubscribers.add(subscriber)
  }

  @Synchronized
  internal fun unsubscribe(subscriber: DebugEventSubscriber) {
    mutableSubscribers.remove(subscriber)
  }

  @Synchronized
  fun unsubscribeAll() {
    mutableSubscribers.clear()
  }

  class TraceScope(private val attributes: LinkedHashMap<String, Any?>) {
    fun attribute(name: String, value: Any?) {
      attributes[name] = value
    }
  }

  @DataClassGenerate
  private data class EventData(
      val type: String,
      val renderStateId: String,
      val attributes: Map<String, Any?>,
      val startTimestamp: Long
  )
}

/** Object to subscribe to debug events */
object DebugEventBus {
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
