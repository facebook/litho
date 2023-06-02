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

import com.facebook.rendercore.LogLevel
import com.facebook.rendercore.debug.DebugEvent.Companion.All
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DebugEventsTest {

  companion object {
    const val TestEvent = "TestEvent"
    const val TestRenderStateId = "1"
    const val TestAttr = "attr"
    const val TestAttrValue = 1
  }

  @Before
  fun before() {
    DebugEventDispatcher.minLogLevel = LogLevel.DEBUG
  }

  @After
  fun after() {
    DebugEventBus.unsubscribeAll()
  }

  @Test
  fun `test event should be dispatched to subscriber`() {
    var event: DebugEvent? = null
    DebugEventBus.subscribe(TestEventSubscriber { e -> event = e })

    val timestamp = System.currentTimeMillis()
    DebugEventDispatcher.dispatch(
        type = TestEvent,
        renderStateId = { TestRenderStateId },
        timestamp = timestamp,
    ) { map ->
      map[TestAttr] = TestAttrValue
    }

    assertThat(event).isNotNull
    assertThat(event?.type).isEqualTo(TestEvent)
    assertThat(event?.renderStateId).isEqualTo(TestRenderStateId)
    assertThat(event?.attribute<Long>(DebugEventAttribute.timestamp)).isEqualTo(timestamp)
    assertThat(event?.attribute<Int>(TestAttr)).isEqualTo(TestAttrValue)
  }

  @Test
  fun `test event should be dispatched to multiple subscribers`() {
    var eventToSub1: DebugEvent? = null
    var eventToSub2: DebugEvent? = null
    DebugEventBus.subscribe(TestEventSubscriber { e -> eventToSub1 = e })
    DebugEventBus.subscribe(TestEventSubscriber { e -> eventToSub2 = e })

    val timestamp = System.currentTimeMillis()
    DebugEventDispatcher.dispatch(
        type = TestEvent,
        renderStateId = { TestRenderStateId },
        timestamp = timestamp,
    ) { map ->
      map[TestAttr] = TestAttrValue
    }

    assertThat(eventToSub1).isNotNull
    assertThat(eventToSub1?.type).isEqualTo(TestEvent)
    assertThat(eventToSub1?.renderStateId).isEqualTo(TestRenderStateId)
    assertThat(eventToSub1?.attribute<Long>(DebugEventAttribute.timestamp)).isEqualTo(timestamp)
    assertThat(eventToSub1?.attribute<Int>(TestAttr)).isEqualTo(TestAttrValue)

    assertThat(eventToSub2).isNotNull
    assertThat(eventToSub2?.type).isEqualTo(TestEvent)
    assertThat(eventToSub2?.renderStateId).isEqualTo(TestRenderStateId)
    assertThat(eventToSub2?.attribute<Long>(DebugEventAttribute.timestamp)).isEqualTo(timestamp)
    assertThat(eventToSub2?.attribute<Int>(TestAttr)).isEqualTo(TestAttrValue)

    assertThat(eventToSub1).isSameAs(eventToSub2)
  }

  @Test
  fun `test event should not be created on dispatch if there are no subscribers`() {
    var createdDebugEvent = false

    fun dispatchDebugEvent() {
      DebugEventDispatcher.dispatch(
          type = TestEvent,
          renderStateId = {
            createdDebugEvent = true
            TestRenderStateId
          })
    }

    dispatchDebugEvent()
    assertThat(createdDebugEvent).isFalse

    DebugEventBus.subscribe(TestEventSubscriber { /* do nothing */})
    dispatchDebugEvent()
    assertThat(createdDebugEvent).isTrue
  }

  @Test
  fun `test event should not be created on trace if there are no subscribers`() {
    var createdDebugEvent = false
    var traceBlockRan = false

    fun runTrace() {
      DebugEventDispatcher.trace(
          type = TestEvent,
          renderStateId = {
            createdDebugEvent = true
            TestRenderStateId
          }) {
            traceBlockRan = true
          }
    }

    runTrace()
    assertThat(createdDebugEvent).isFalse
    assertThat(traceBlockRan).isTrue

    DebugEventBus.subscribe(TestEventSubscriber { /* do nothing */})
    runTrace()
    assertThat(createdDebugEvent).isTrue
    assertThat(traceBlockRan).isTrue
  }

  @Test
  fun `test event should not be dispatched to subscriber after unsubscribing`() {
    var event: DebugEvent? = null
    val subscriber = TestEventSubscriber { e -> event = e }
    DebugEventBus.subscribe(subscriber)

    DebugEventDispatcher.dispatch(type = TestEvent, renderStateId = { TestRenderStateId })

    assertThat(event).isNotNull
    event = null

    DebugEventBus.unsubscribe(subscriber)

    DebugEventDispatcher.dispatch(type = TestEvent, renderStateId = { TestRenderStateId })

    assertThat(event).isNull()
  }

  @Test
  fun `multiple test events should be dispatched to subscriber`() {
    val events = mutableListOf<DebugEvent>()

    DebugEventBus.subscribe(TestEventSubscriber { e -> events.add(e) })

    val timestamp = System.currentTimeMillis() - 1 // note: time in the past
    DebugEventDispatcher.dispatch(
        type = TestEvent,
        renderStateId = { TestRenderStateId },
        timestamp = timestamp,
    ) { map ->
      map[TestAttr] = TestAttrValue
    }
    DebugEventDispatcher.dispatch(
        type = TestEvent,
        renderStateId = { TestRenderStateId }, // note: no timestamp passed
    ) { map ->
      map[TestAttr] = "{$TestAttrValue}_1" // note: different attr value
    }

    assertThat(events.size).isEqualTo(2)
    assertThat(events[0].type).isEqualTo(TestEvent)
    assertThat(events[0].renderStateId).isEqualTo(TestRenderStateId)
    assertThat(events[0].attribute<Long>(DebugEventAttribute.timestamp)).isEqualTo(timestamp)
    assertThat(events[0].attribute<Int>(TestAttr)).isEqualTo(TestAttrValue)

    assertThat(events).isNotEmpty
    assertThat(events[1].type).isEqualTo(TestEvent)
    assertThat(events[1].renderStateId).isEqualTo(TestRenderStateId)
    assertThat(events[1].attribute<Long>(DebugEventAttribute.timestamp)).isGreaterThan(timestamp)
    assertThat(events[1].attribute<String>(TestAttr)).isEqualTo("{$TestAttrValue}_1")
  }

  @Test
  fun `test events should be dispatched to correct subscribers`() {
    var event: DebugEvent? = null
    DebugEventBus.subscribe(TestEventSubscriber { e -> event = e })

    val notTestEvent = "Not${TestEvent}"
    var otherEvent: DebugEvent? = null

    DebugEventBus.subscribe(TestEventsSubscriber(notTestEvent, listener = { e -> otherEvent = e }))

    DebugEventDispatcher.dispatch(type = TestEvent, renderStateId = { TestRenderStateId })

    DebugEventDispatcher.dispatch(type = notTestEvent, renderStateId = { TestRenderStateId })

    assertThat(event).isNotNull
    assertThat(otherEvent).isNotNull
    assertThat(event).isNotEqualTo(otherEvent)
  }

  @Test
  fun `test event should be dispatched to all event subscriber`() {
    val events = mutableListOf<DebugEvent>()

    DebugEventBus.subscribe(TestEventsSubscriber(All, listener = { e -> events.add(e) }))

    DebugEventDispatcher.dispatch(type = "a", renderStateId = { TestRenderStateId })
    DebugEventDispatcher.dispatch(type = "b", renderStateId = { TestRenderStateId })
    DebugEventDispatcher.dispatch(type = "c", renderStateId = { TestRenderStateId })
    DebugEventDispatcher.dispatch(type = "d", renderStateId = { TestRenderStateId })

    assertThat(events.size).isEqualTo(4)
  }

  @Test
  fun `trace event should be dispatched to subscriber`() {
    val traceTestAttr = "otherTestAttr"
    val traceTestAttrValue = "2"

    var event: DebugEvent? = null
    DebugEventBus.subscribe(TestEventSubscriber { e -> event = e })

    val timestamp = System.currentTimeMillis() - 1 // note: time in the past

    DebugEventDispatcher.trace(
        type = TestEvent,
        renderStateId = { TestRenderStateId },
        attributesAccumulator = { attributes -> attributes[TestAttr] = TestAttrValue },
    ) { scope ->
      scope?.attribute(traceTestAttr, "2")
    }

    assertThat(event).isNotNull
    assertThat(event?.type).isEqualTo(TestEvent)
    assertThat(event?.renderStateId).isEqualTo(TestRenderStateId)
    assertThat(event?.attribute<Long>(DebugEventAttribute.timestamp)).isGreaterThan(timestamp)
    assertThat(event?.attribute<Duration>(DebugEventAttribute.duration)?.value).isGreaterThan(0)
    assertThat(event?.attribute<Int>(TestAttr)).isEqualTo(TestAttrValue)
    assertThat(event?.attribute<String>(traceTestAttr)).isEqualTo(traceTestAttrValue)
  }

  @Test
  fun `test event should be dispatched only if min log level is smaller than event log level`() {
    var event: DebugEvent? = null
    DebugEventBus.subscribe(TestEventSubscriber { e -> event = e })

    DebugEventDispatcher.minLogLevel = LogLevel.VERBOSE
    DebugEventDispatcher.dispatch(
        type = TestEvent,
        renderStateId = { TestRenderStateId },
        timestamp = System.currentTimeMillis(),
        logLevel = LogLevel.VERBOSE) {
          mapOf<String, Any?>(TestAttr to TestAttrValue)
        }

    assertThat(event).isNotNull
    event = null

    DebugEventDispatcher.minLogLevel = LogLevel.VERBOSE
    DebugEventDispatcher.dispatch(
        type = TestEvent,
        renderStateId = { TestRenderStateId },
        timestamp = System.currentTimeMillis(),
        logLevel = LogLevel.DEBUG) {
          mapOf<String, Any?>(TestAttr to TestAttrValue)
        }

    assertThat(event).isNotNull
    event = null

    DebugEventDispatcher.minLogLevel = LogLevel.WARNING
    DebugEventDispatcher.dispatch(
        type = TestEvent,
        renderStateId = { TestRenderStateId },
        timestamp = System.currentTimeMillis(),
        logLevel = LogLevel.DEBUG) {
          mapOf<String, Any?>(TestAttr to TestAttrValue)
        }

    assertThat(event).isNull()
    event = null

    DebugEventDispatcher.minLogLevel = LogLevel.WARNING
    DebugEventDispatcher.dispatch(
        type = TestEvent,
        renderStateId = { TestRenderStateId },
        timestamp = System.currentTimeMillis(),
        logLevel = LogLevel.ERROR) {
          mapOf<String, Any?>(TestAttr to TestAttrValue)
        }

    assertThat(event).isNotNull
    event = null

    DebugEventDispatcher.minLogLevel = LogLevel.ERROR
    DebugEventDispatcher.dispatch(
        type = TestEvent,
        renderStateId = { TestRenderStateId },
        timestamp = System.currentTimeMillis(),
        logLevel = LogLevel.ERROR) {
          mapOf<String, Any?>(TestAttr to TestAttrValue)
        }

    assertThat(event).isNotNull
    event = null

    DebugEventDispatcher.minLogLevel = LogLevel.FATAL
    DebugEventDispatcher.dispatch(
        type = TestEvent,
        renderStateId = { TestRenderStateId },
        timestamp = System.currentTimeMillis(),
        logLevel = LogLevel.ERROR) {
          mapOf<String, Any?>(TestAttr to TestAttrValue)
        }

    assertThat(event).isNull()
    event = null
  }

  @Test
  fun `generateTraceIdentifier should generate a valid id if there is at least one subscriber listening to all events`() {
    assertThat(DebugEventDispatcher.generateTraceIdentifier(TestEvent)).isNull()
    DebugEventDispatcher.subscribe(TestEventsSubscriber(All, listener = { /* nothing */}))
    assertThat(DebugEventDispatcher.generateTraceIdentifier(TestEvent)).isNotNull()
  }

  @Test
  fun `generateTraceIdentifier should generate a valid id if there is at least one subscriber listens to that event`() {
    assertThat(DebugEventDispatcher.generateTraceIdentifier(TestEvent)).isNull()
    DebugEventDispatcher.subscribe(TestEventsSubscriber(TestEvent, listener = { /* nothing */}))
    assertThat(DebugEventDispatcher.generateTraceIdentifier(TestEvent)).isNotNull()
  }

  @Test
  fun `generateTraceIdentifier should generate different ids for each invocation as long as there are valid subscribers`() {
    assertThat(DebugEventDispatcher.generateTraceIdentifier(TestEvent)).isNull()
    DebugEventDispatcher.subscribe(TestEventsSubscriber(TestEvent, listener = { /* nothing */}))
    assertThat(DebugEventDispatcher.generateTraceIdentifier(TestEvent)).isEqualTo(1)
    assertThat(DebugEventDispatcher.generateTraceIdentifier(TestEvent)).isEqualTo(2)
  }

  @Test
  fun `java trace APIs work correctly`() {
    val events = mutableListOf<DebugEvent>()
    DebugEventDispatcher.subscribe(TestEventsSubscriber(All, listener = { events.add(it) }))

    val traceId = DebugEventDispatcher.generateTraceIdentifier(TestEvent)
    DebugEventDispatcher.beginTrace(
        traceId!!, TestEvent, TestRenderStateId, mutableMapOf(TestAttr to 1))
    assertThat(events).isEmpty()

    DebugEventDispatcher.endTrace(traceId)
    assertThat(events).hasSize(1)

    val event = events.last()
    assertThat(event.type).isEqualTo(TestEvent)
    assertThat(event.renderStateId).isEqualTo(TestRenderStateId)
    assertThat(event.attribute<Int>(TestAttr)).isEqualTo(1)
  }

  class TestEventSubscriber(val listener: (DebugEvent) -> Unit) : DebugEventSubscriber(TestEvent) {
    override fun onEvent(event: DebugEvent) {
      listener(event)
    }
  }

  class TestEventsSubscriber(
      vararg events: String,
      val listener: (DebugEvent) -> Unit,
  ) : DebugEventSubscriber(*events) {
    override fun onEvent(event: DebugEvent) {
      listener(event)
    }
  }
}
