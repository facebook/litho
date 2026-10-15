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

import com.facebook.litho.core.height
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.visibility.onInvisible
import com.facebook.litho.visibility.onVisible
import com.facebook.rendercore.extensions.VisibilityChangeReason
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class TransientStateVisibilityPassesTest {

  @get:Rule val lithoTestRule = LithoTestRule()

  private val log = mutableListOf<String>()

  /** The log since the last call. */
  private fun takeLog(): List<String> = log.toList().also { log.clear() }

  @Test
  fun visibleBoundsPassUnderTransientState_callsTheListenerOncePerPass_andLogsNothing() {
    val lithoView = lithoTestRule.render { Rows("a") }.lithoView
    val skipped = mutableListOf<BaseMountingView>()
    lithoView.setOnPassUnderTransientStateListener { view -> skipped.add(view) }
    lithoView.setHasTransientState(true)
    lithoView.setVisibilityHint(false)
    takeLog()

    lithoView.setVisibilityHint(true)
    lithoView.notifyVisibleBoundsChanged(VisibilityChangeReason.SCROLL)

    assertThat(skipped).containsExactly(lithoView, lithoView)
    assertThat(takeLog()).isEmpty()
  }

  @Test
  fun dirtyMountUnderTransientState_callsTheListenerOnce_andLogsNothing() {
    val testLithoView = lithoTestRule.render { Rows("a") }
    val lithoView = testLithoView.lithoView
    val skipped = mutableListOf<BaseMountingView>()
    val dirtyAtCall = mutableListOf<Boolean>()
    lithoView.setOnPassUnderTransientStateListener { view ->
      skipped.add(view)
      dirtyAtCall.add(view.isMountStateDirty)
    }
    lithoView.setHasTransientState(true)
    takeLog()

    testLithoView.setRoot(Rows("a", "b"))

    assertThat(skipped).containsExactly(lithoView)
    // The call comes after the mount, so an owner that reruns visibility sees the new content.
    assertThat(dirtyAtCall).containsExactly(false)
    assertThat(takeLog()).isEmpty()
  }

  @Test
  fun passWithoutTransientState_orPaused_doesNotCallTheListener() {
    val lithoView = lithoTestRule.render { Rows("a") }.lithoView
    val skipped = mutableListOf<BaseMountingView>()
    lithoView.setOnPassUnderTransientStateListener { view -> skipped.add(view) }

    lithoView.notifyVisibleBoundsChanged(VisibilityChangeReason.SCROLL)
    lithoView.setHasTransientState(true)
    lithoView.setVisibilityHint(false)
    lithoView.notifyVisibleBoundsChanged(VisibilityChangeReason.SCROLL)

    assertThat(skipped).isEmpty()
  }

  @Test
  fun processVisibilityOutputsForDirtyMount_logsWhatAMountUnderTransientStateRemoved() {
    val testLithoView = lithoTestRule.render { Rows("kept", "removed") }
    val lithoView = testLithoView.lithoView
    takeLog()

    lithoView.setHasTransientState(true)
    testLithoView.setRoot(Rows("kept"))
    lithoView.processVisibilityOutputsForDirtyMount()
    val underTransientState = takeLog()
    lithoView.setHasTransientState(false)
    val afterTransientStateEnds = takeLog()
    lithoView.processVisibilityOutputsForDirtyMount()
    val afterDirtyMountPass = takeLog()
    testLithoView.setRoot(Rows("kept", "removed"))

    assertThat(underTransientState).isEmpty()
    // Ending transient state runs a visible-bounds pass, which does not log what a mount removed.
    assertThat(afterTransientStateEnds).isEmpty()
    assertThat(afterDirtyMountPass).containsExactly("removed-")
    assertThat(takeLog()).containsExactly("removed+")
  }

  @Test
  fun processVisibilityOutputsForDirtyMount_whileMountingIsPaused_logsNothing() {
    val lithoView = lithoTestRule.render { Rows("a") }.lithoView
    lithoView.setVisibilityHint(false)
    takeLog()

    lithoView.processVisibilityOutputsForDirtyMount()

    assertThat(takeLog()).isEmpty()
  }

  /** Rows that log `label+` and `label-`, keyed by position. */
  private inner class Rows(private vararg val labels: String) : KComponent() {
    override fun ComponentScope.render(): Component = Column {
      for (label in labels) {
        child(
            Row(
                style =
                    Style.height(10.px)
                        .onVisible { log.add("$label+") }
                        .onInvisible { log.add("$label-") },
            ),
        )
      }
    }
  }
}
