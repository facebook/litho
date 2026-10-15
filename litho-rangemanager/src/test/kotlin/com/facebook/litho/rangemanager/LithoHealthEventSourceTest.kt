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

package com.facebook.litho.rangemanager

import android.os.Looper
import com.facebook.litho.debug.LithoDebugEvent
import com.facebook.litho.debug.LithoDebugEventAttributes
import com.facebook.litho.rangemanager.LithoPreparationHealthEvent.MainThreadRender
import com.facebook.litho.rangemanager.LithoPreparationHealthEvent.MainThreadRender.Phase
import com.facebook.rendercore.debug.DebugEventAttribute
import com.facebook.rendercore.debug.DebugEventBus
import com.facebook.rendercore.debug.DebugEventDispatcher
import com.facebook.rendercore.debug.DebugMarkerEvent
import com.meta.rangemanager.PreparationFailureType
import com.meta.rangemanager.PreparationHealthEvent
import com.meta.rangemanager.PreparationHealthObserver
import com.meta.rangemanager.PreparationSource
import com.meta.rangemanager.UnprepareReason
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LithoHealthEventSourceTest {

  private val reportedEvents = mutableListOf<Pair<Any, PreparationHealthEvent>>()

  private val observer =
      object : PreparationHealthObserver {
        override fun onItemRegistered(key: Any) {}

        override fun onItemPrepared(key: Any, source: PreparationSource) {}

        override fun onItemBound(key: Any, wasPrepared: Boolean) {}

        override fun onItemUnprepared(key: Any, reason: UnprepareReason) {}

        override fun onFirstScroll() {}

        override fun reportEvent(key: Any, event: PreparationHealthEvent) {
          reportedEvents.add(key to event)
        }
      }

  @After
  fun tearDown() {
    DebugEventBus.unsubscribeAll()
  }

  @Test
  fun `reports Late when hasMainThreadLayoutState is false`() {
    val source = LithoHealthEventSource(observer) { it }
    source.onEvent(
        makeRenderEvent(
            breadcrumb = "binder/key1",
            hasMainThreadLayoutState = false,
        ),
    )

    assertThat(reportedEvents).hasSize(1)
    assertThat(reportedEvents[0].first).isEqualTo("binder/key1")
    assertThat(reportedEvents[0].second.failureType).isEqualTo(PreparationFailureType.LATE)
  }

  @Test
  fun `reports Wrong when idMatch is false`() {
    val source = LithoHealthEventSource(observer) { it }
    source.onEvent(
        makeRenderEvent(
            breadcrumb = "binder/key2",
            hasMainThreadLayoutState = true,
            idMatch = false,
        ),
    )

    assertThat(reportedEvents).hasSize(1)
    assertThat(reportedEvents[0].second.failureType).isEqualTo(PreparationFailureType.WRONG)
  }

  @Test
  fun `reports Wrong when sizeSpecsMatch is false`() {
    val source = LithoHealthEventSource(observer) { it }
    source.onEvent(
        makeRenderEvent(
            breadcrumb = "binder/key3",
            hasMainThreadLayoutState = true,
            sizeSpecsMatch = false,
        ),
    )

    assertThat(reportedEvents).hasSize(1)
    assertThat(reportedEvents[0].second.failureType).isEqualTo(PreparationFailureType.WRONG)
  }

  @Test
  fun `skips event when no failure detected`() {
    val source = LithoHealthEventSource(observer) { it }
    source.onEvent(
        makeRenderEvent(
            breadcrumb = "binder/key4",
            hasMainThreadLayoutState = true,
            sizeSpecsMatch = true,
            idMatch = true,
        ),
    )

    assertThat(reportedEvents).isEmpty()
  }

  @Test
  fun `skips event when keyExtractor returns null`() {
    val source = LithoHealthEventSource(observer) { null }
    source.onEvent(
        makeRenderEvent(
            breadcrumb = "binder/key5",
            hasMainThreadLayoutState = false,
        ),
    )

    assertThat(reportedEvents).isEmpty()
  }

  @Test
  fun `skips event when breadcrumb is missing`() {
    val source = LithoHealthEventSource(observer) { it }
    source.onEvent(
        DebugMarkerEvent(
            timestamp = TIMESTAMP_MS,
            type = LithoDebugEvent.RenderOnMainThreadStarted,
            renderStateId = "1",
            attributes = emptyMap(),
        ),
    )

    assertThat(reportedEvents).isEmpty()
  }

  @Test
  fun `skips event with wrong type`() {
    val source = LithoHealthEventSource(observer) { it }
    source.onEvent(
        DebugMarkerEvent(
            timestamp = TIMESTAMP_MS,
            type = "SomeOtherEvent",
            renderStateId = "1",
            attributes =
                mapOf(
                    LithoDebugEventAttributes.Breadcrumb to "binder/key",
                    LithoDebugEventAttributes.HasMainThreadLayoutState to false,
                ),
        ),
    )

    assertThat(reportedEvents).isEmpty()
  }

  @Test
  fun `uses key from keyExtractor`() {
    val source = LithoHealthEventSource(observer) { breadcrumb -> breadcrumb.substringAfter("/") }
    source.onEvent(
        makeRenderEvent(
            breadcrumb = "binder/myKey",
            hasMainThreadLayoutState = false,
        ),
    )

    assertThat(reportedEvents).hasSize(1)
    assertThat(reportedEvents[0].first).isEqualTo("myKey")
  }

  @Test
  fun `start subscribes to DebugEventBus`() {
    val source = LithoHealthEventSource(observer) { it }
    source.start()

    dispatchRenderEvent(
        breadcrumb = "binder/busKey",
        hasMainThreadLayoutState = false,
    )

    assertThat(reportedEvents).hasSize(1)
    assertThat(reportedEvents[0].first).isEqualTo("binder/busKey")
  }

  @Test
  fun `stop unsubscribes from DebugEventBus`() {
    val source = LithoHealthEventSource(observer) { it }
    source.start()
    source.stop()

    dispatchRenderEvent(
        breadcrumb = "binder/stoppedKey",
        hasMainThreadLayoutState = false,
    )

    assertThat(reportedEvents).isEmpty()
  }

  @Test
  fun `start is idempotent`() {
    val source = LithoHealthEventSource(observer) { it }
    source.start()
    source.start()

    dispatchRenderEvent(
        breadcrumb = "binder/idempotent",
        hasMainThreadLayoutState = false,
    )

    // Should only receive one event, not two
    assertThat(reportedEvents).hasSize(1)
  }

  @Test
  fun `ignores a render off the main thread`() {
    val source = LithoHealthEventSource(observer) { it }

    source.onEvent(makeResolveEvent(threadName = "LithoLayoutThread"))

    assertThat(reportedEvents).isEmpty()
  }

  @Test
  fun `ignores measure's own render`() {
    val source = LithoHealthEventSource(observer) { it }

    source.onEvent(makeResolveEvent(renderSource = "measure_setSizeSpecSync"))

    assertThat(reportedEvents).isEmpty()
  }

  @Test
  fun `reports main finishing an interrupted resolve`() {
    val source = LithoHealthEventSource(observer) { it }

    source.onEvent(
        makeResolveEvent(type = LithoDebugEvent.ComponentTreeResume, renderSource = "setRootAsync"),
    )

    assertThat(reportedEvents)
        .containsExactly(
            "binder/key" to
                LithoPreparationHealthEvent.MainThreadRender("setRootAsync", Phase.RESUME),
        )
  }

  @Test
  fun `names the component behind a synchronous state update`() {
    val source = LithoHealthEventSource(observer) { it }

    source.onEvent(makeResolveEvent(renderSource = "updateStateSync", attribution = "DateWrapper"))

    assertThat(reportedEvents)
        .containsExactly(
            "binder/key" to MainThreadRender("updateStateSync", requestedBy = "DateWrapper"),
        )
  }

  /** Other sources carry their caller's attribution, which is not a component asking to update. */
  @Test
  fun `names no component for a render that is not a synchronous state update`() {
    val source = LithoHealthEventSource(observer) { it }

    source.onEvent(makeResolveEvent(renderSource = "setRootSync", attribution = "SomeCaller"))

    assertThat(reportedEvents).containsExactly("binder/key" to MainThreadRender("setRootSync"))
  }

  @Test
  fun `names no component when the state update carries none`() {
    val source = LithoHealthEventSource(observer) { it }

    source.onEvent(makeResolveEvent(renderSource = "updateStateSync"))

    assertThat(reportedEvents).containsExactly("binder/key" to MainThreadRender("updateStateSync"))
  }

  private fun makeRenderEvent(
      breadcrumb: String,
      hasMainThreadLayoutState: Boolean = true,
      sizeSpecsMatch: Boolean = true,
      idMatch: Boolean = true,
  ): DebugMarkerEvent {
    return DebugMarkerEvent(
        timestamp = TIMESTAMP_MS,
        type = LithoDebugEvent.RenderOnMainThreadStarted,
        renderStateId = "1",
        attributes =
            mapOf(
                LithoDebugEventAttributes.Breadcrumb to breadcrumb,
                LithoDebugEventAttributes.HasMainThreadLayoutState to hasMainThreadLayoutState,
                LithoDebugEventAttributes.SizeSpecsMatch to sizeSpecsMatch,
                LithoDebugEventAttributes.IdMatch to idMatch,
            ),
    )
  }

  private fun makeResolveEvent(
      type: String = LithoDebugEvent.ComponentTreeResolve,
      renderSource: String = "setRootSync",
      threadName: String = Looper.getMainLooper().thread.name,
      attribution: String? = null,
  ): DebugMarkerEvent = DebugMarkerEvent(
      timestamp = TIMESTAMP_MS,
      type = type,
      renderStateId = "1",
      threadName = threadName,
      attributes =
          mapOf(
              LithoDebugEventAttributes.Breadcrumb to "binder/key",
              DebugEventAttribute.Source to renderSource,
              LithoDebugEventAttributes.Attribution to attribution,
          ),
  )

  private fun dispatchRenderEvent(
      breadcrumb: String,
      hasMainThreadLayoutState: Boolean = true,
      sizeSpecsMatch: Boolean = true,
      idMatch: Boolean = true,
  ) {
    DebugEventDispatcher.dispatch(
        type = LithoDebugEvent.RenderOnMainThreadStarted,
        renderStateId = { "1" },
        timestamp = TIMESTAMP_MS,
    ) { attrs ->
      attrs[LithoDebugEventAttributes.Breadcrumb] = breadcrumb
      attrs[LithoDebugEventAttributes.HasMainThreadLayoutState] = hasMainThreadLayoutState
      attrs[LithoDebugEventAttributes.SizeSpecsMatch] = sizeSpecsMatch
      attrs[LithoDebugEventAttributes.IdMatch] = idMatch
    }
  }

  private companion object {
    const val TIMESTAMP_MS = 0L
  }
}
