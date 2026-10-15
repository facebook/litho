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

package com.facebook.litho.visibility

import android.graphics.Rect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.facebook.litho.AOSPLithoVisibilityEventsController
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.px
import com.facebook.rendercore.visibility.VisibilityChangeReason
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for [VisibilityChangeReason] propagation through visibility events.
 *
 * These tests verify that visibility events include the correct reason indicating WHY the
 * visibility change occurred.
 */
@RunWith(LithoTestRunner::class)
class VisibilityChangeReasonTest {

  private val testLifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

  @Rule
  @JvmField
  val lithoTestRule = LithoTestRule(
      lithoVisibilityEventsController = {
        AOSPLithoVisibilityEventsController(testLifecycleOwner)
      },
      lifecycleOwner = { testLifecycleOwner },
  )

  // ==========================================================================
  // MOUNT Reason Tests
  // ==========================================================================

  @Test
  fun onVisible_receivesMountReason_onInitialMount() {
    val receivedReason = AtomicReference<VisibilityChangeReason>(null)

    lithoTestRule.render {
      Row(
          style =
              Style.width(200.px).height(200.px).onVisible { event ->
                receivedReason.set(event.reason)
              },
      )
    }

    assertThat(receivedReason.get()).isEqualTo(VisibilityChangeReason.MOUNT)
  }

  @Test
  fun onVisibilityChanged_receivesMountReason_onInitialMount() {
    val receivedReasons = mutableListOf<VisibilityChangeReason>()

    lithoTestRule.render {
      Row(
          style =
              Style.width(200.px).height(200.px).onVisibilityChanged { event ->
                receivedReasons.add(event.reason)
              },
      )
    }

    // First event should be MOUNT
    assertThat(receivedReasons).isNotEmpty
    assertThat(receivedReasons[0]).isEqualTo(VisibilityChangeReason.MOUNT)
  }

  @Test
  fun onFullImpression_receivesMountReason_onInitialMount() {
    val receivedReason = AtomicReference<VisibilityChangeReason>(null)

    lithoTestRule.render {
      Row(
          style =
              Style.width(200.px).height(200.px).onFullImpression { event ->
                receivedReason.set(event.reason)
              },
      )
    }

    assertThat(receivedReason.get()).isEqualTo(VisibilityChangeReason.MOUNT)
  }

  // ==========================================================================
  // SCROLL Reason Tests
  // ==========================================================================

  @Test
  fun onInvisible_receivesScrollReason_whenScrolledOutOfView() {
    val receivedReasons = mutableListOf<VisibilityChangeReason>()

    val testLithoView =
        lithoTestRule.render(widthPx = 100, heightPx = 100) {
          Row(
              style =
                  Style.width(100.px).height(50.px).onInvisible { event ->
                    receivedReasons.add(event.reason)
                  },
          )
        }

    // Simulate scrolling component out of view using processVisibilityOutputs
    // (which is @VisibleForTesting) to pass the SCROLL reason directly, since the
    // public notifyVisibleBoundsChanged defaults reason to UNKNOWN.
    testLithoView.lithoView.processVisibilityOutputs(
        Rect(0, 100, 100, 200),
        VisibilityChangeReason.SCROLL,
    )

    // Should receive SCROLL reason
    assertThat(receivedReasons).isNotEmpty
    assertThat(receivedReasons[0]).isEqualTo(VisibilityChangeReason.SCROLL)
  }

  // ==========================================================================
  // LIFECYCLE Reason Tests
  // ==========================================================================

  @Test
  fun onInvisible_receivesLifecycleReason_whenLifecyclePaused() {
    val receivedReasons = mutableListOf<VisibilityChangeReason>()

    lithoTestRule.render {
      Row(
          style =
              Style.width(200.px).height(200.px).onInvisible { event ->
                receivedReasons.add(event.reason)
              },
      )
    }

    // Trigger lifecycle pause - this should propagate LIFECYCLE reason
    // through AOSPLithoVisibilityEventsController -> ComponentTree.onMovedToState()
    testLifecycleOwner.currentState = Lifecycle.State.CREATED

    // Should receive LIFECYCLE reason (set by ComponentTree's onMoveToStateHintInvisible)
    assertThat(receivedReasons).isNotEmpty
    assertThat(receivedReasons[0]).isEqualTo(VisibilityChangeReason.LIFECYCLE)
  }

  @Test
  fun onVisible_receivesLifecycleReason_whenLifecycleResumed() {
    val receivedReasons = mutableListOf<VisibilityChangeReason>()

    lithoTestRule.render {
      Row(
          style =
              Style.width(200.px).height(200.px).onVisible { event ->
                receivedReasons.add(event.reason)
              },
      )
    }

    // First pause the lifecycle to make component invisible
    testLifecycleOwner.currentState = Lifecycle.State.CREATED

    // Clear previous reasons
    receivedReasons.clear()

    // Resume lifecycle - this should propagate LIFECYCLE reason
    testLifecycleOwner.currentState = Lifecycle.State.RESUMED

    // Should receive LIFECYCLE reason
    assertThat(receivedReasons).isNotEmpty
    assertThat(receivedReasons[0]).isEqualTo(VisibilityChangeReason.LIFECYCLE)
  }
}
