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

package com.facebook.litho.tooling

import com.facebook.litho.debug.LithoDebugEvent
import com.facebook.litho.debug.LithoDebugEventAttributes
import com.facebook.litho.tooling.MainThreadRenderSourcesDebugger.MainThreadRenderSource
import com.facebook.litho.tooling.MainThreadRenderSourcesDebugger.OnMainThreadRenderDetected
import com.facebook.rendercore.debug.DebugMarkerEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MainThreadRenderSourcesDebuggerTest {

  private val detected = mutableListOf<Pair<String, MainThreadRenderSource>>()

  private val callback = OnMainThreadRenderDetected { breadcrumb, source ->
    detected.add(breadcrumb to source)
  }

  @Test
  fun `reports MainThreadLayoutStateMismatch with all attributes when hasMainThreadLayoutState is true`() {
    val debugger = MainThreadRenderSourcesDebugger(callback)

    debugger.onEvent(
        makeRenderEvent(
            breadcrumb = "surface/key1",
            hasMainThreadLayoutState = true,
            root = "MyRootComponent",
            currentSizeConstraint = "current-size",
            currentRootId = 42,
            sizeConstraint = "measure-size",
            rootId = 99,
        ),
    )

    assertThat(detected).hasSize(1)
    val (breadcrumb, source) = detected[0]
    assertThat(breadcrumb).isEqualTo("surface/key1")
    assertThat(source)
        .isEqualTo(
            MainThreadRenderSource.MainThreadLayoutStateMismatch(
                root = "MyRootComponent",
                mainThreadLayoutStateSizeSpecs = "current-size",
                mainThreadLayoutRootId = 42,
                measureSizeSpecs = "measure-size",
                rootId = 99,
                breadcrumb = "surface/key1",
            ),
        )
  }

  @Test
  fun `reports NoMainThreadLayoutState when hasMainThreadLayoutState is false`() {
    val debugger = MainThreadRenderSourcesDebugger(callback)

    debugger.onEvent(
        makeRenderEvent(
            breadcrumb = "surface/key2",
            hasMainThreadLayoutState = false,
            root = "AnotherRoot",
        ),
    )

    assertThat(detected).hasSize(1)
    val (breadcrumb, source) = detected[0]
    assertThat(breadcrumb).isEqualTo("surface/key2")
    assertThat(source)
        .isEqualTo(
            MainThreadRenderSource.NoMainThreadLayoutState(
                root = "AnotherRoot",
                breadcrumb = "surface/key2",
            ),
        )
  }

  @Test
  fun `reports NoMainThreadLayoutState when hasMainThreadLayoutState attribute is absent`() {
    val debugger = MainThreadRenderSourcesDebugger(callback)

    debugger.onEvent(
        DebugMarkerEvent(
            type = LithoDebugEvent.RenderOnMainThreadStarted,
            renderStateId = "1",
            attributes =
                mapOf(
                    LithoDebugEventAttributes.Breadcrumb to "surface/key3",
                    LithoDebugEventAttributes.Root to "DefaultedRoot",
                ),
        ),
    )

    assertThat(detected).hasSize(1)
    assertThat(detected[0].second)
        .isEqualTo(
            MainThreadRenderSource.NoMainThreadLayoutState(
                root = "DefaultedRoot",
                breadcrumb = "surface/key3",
            ),
        )
  }

  @Test
  fun `does not report when breadcrumb attribute is missing`() {
    val debugger = MainThreadRenderSourcesDebugger(callback)

    debugger.onEvent(
        DebugMarkerEvent(
            type = LithoDebugEvent.RenderOnMainThreadStarted,
            renderStateId = "1",
            attributes =
                mapOf(
                    LithoDebugEventAttributes.HasMainThreadLayoutState to true,
                    LithoDebugEventAttributes.Root to "R",
                ),
        ),
    )

    assertThat(detected).isEmpty()
  }

  @Test
  fun `does not report when event type does not match RenderOnMainThreadStarted`() {
    val debugger = MainThreadRenderSourcesDebugger(callback)

    debugger.onEvent(
        DebugMarkerEvent(
            type = "SomeOtherEventType",
            renderStateId = "1",
            attributes =
                mapOf(
                    LithoDebugEventAttributes.Breadcrumb to "surface/key4",
                    LithoDebugEventAttributes.HasMainThreadLayoutState to false,
                    LithoDebugEventAttributes.Root to "R",
                ),
        ),
    )

    assertThat(detected).isEmpty()
  }

  @Test
  fun `emits one callback per event`() {
    val debugger = MainThreadRenderSourcesDebugger(callback)

    debugger.onEvent(
        makeRenderEvent(
            breadcrumb = "surface/keyA",
            hasMainThreadLayoutState = true,
            root = "RootA",
            currentRootId = 1,
            rootId = 2,
        ),
    )
    debugger.onEvent(
        makeRenderEvent(
            breadcrumb = "surface/keyB",
            hasMainThreadLayoutState = false,
            root = "RootB",
        ),
    )

    assertThat(detected).hasSize(2)
    assertThat(detected[0].first).isEqualTo("surface/keyA")
    assertThat(detected[0].second)
        .isInstanceOf(MainThreadRenderSource.MainThreadLayoutStateMismatch::class.java)
    assertThat(detected[1].first).isEqualTo("surface/keyB")
    assertThat(detected[1].second)
        .isInstanceOf(MainThreadRenderSource.NoMainThreadLayoutState::class.java)
  }

  private fun makeRenderEvent(
      breadcrumb: String,
      hasMainThreadLayoutState: Boolean,
      root: String = "Root",
      currentSizeConstraint: String = "current-size-constraint",
      currentRootId: Int = 0,
      sizeConstraint: String = "size-constraint",
      rootId: Int = 0,
  ): DebugMarkerEvent {
    return DebugMarkerEvent(
        type = LithoDebugEvent.RenderOnMainThreadStarted,
        renderStateId = "1",
        attributes =
            mapOf(
                LithoDebugEventAttributes.Breadcrumb to breadcrumb,
                LithoDebugEventAttributes.HasMainThreadLayoutState to hasMainThreadLayoutState,
                LithoDebugEventAttributes.Root to root,
                LithoDebugEventAttributes.CurrentSizeConstraint to currentSizeConstraint,
                LithoDebugEventAttributes.CurrentRootId to currentRootId,
                LithoDebugEventAttributes.SizeConstraint to sizeConstraint,
                LithoDebugEventAttributes.RootId to rootId,
            ),
    )
  }
}
