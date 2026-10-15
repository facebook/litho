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

import android.content.Context
import android.view.accessibility.AccessibilityManager
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.accessibility.contentDescription
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.position
import com.facebook.litho.flexbox.positionType
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.testing.AccessibilityProbe
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.accessibilityProbe
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.onClick
import com.facebook.rendercore.px
import com.facebook.yoga.YogaPositionType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowAccessibilityManager

/** What [AccessibilityProbe] reports, and in which order. */
@RunWith(LithoTestRunner::class)
class AccessibilityProbeTest {

  @get:Rule val lithoTestRule = LithoTestRule(attachToRealWindow = true)

  private lateinit var shadowManager: ShadowAccessibilityManager

  @Before
  fun setUp() {
    val manager =
        ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    shadowManager = Shadows.shadowOf(manager)
    shadowManager.setEnabled(true)
    shadowManager.setTouchExplorationEnabled(true)
    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
  }

  @After
  fun tearDown() {
    shadowManager.setEnabled(false)
    shadowManager.setTouchExplorationEnabled(false)
    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
  }

  @Test
  fun `nodes are listed by position on screen, not in mount order`() {
    // The lower child is declared, so mounted, first. The platform sorts siblings top to bottom.
    val probe =
        lithoTestRule
            .render {
              Column(style = Style.width(SIZE.px).height((SIZE * 2).px)) {
                child(labelledBox(LOWER, top = SIZE))
                child(labelledBox(UPPER, top = 0))
              }
            }
            .accessibilityProbe()

    val labels = probe.nodes().mapNotNull { it.info().contentDescription?.toString() }
    assertThat(labels).containsExactly(UPPER, LOWER)
  }

  @Test
  fun `a tree that is not attached to a window is refused`() {
    // Detached, the platform lists no children, so a "not found" assertion would pass vacuously.
    val detached = FrameLayout(ApplicationProvider.getApplicationContext<Context>())

    assertThatThrownBy { AccessibilityProbe(detached).nodes() }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("attachToRealWindow")
  }

  private companion object {
    const val SIZE = 100
    const val UPPER = "upper"
    const val LOWER = "lower"
  }
}

private fun ResourcesScope.labelledBox(label: String, top: Int): Component = Column(
    style =
        Style.width(100.px)
            .height(100.px)
            .positionType(YogaPositionType.ABSOLUTE)
            .position(top = top.px, left = 0.px)
            .contentDescription(label)
            .onClick {},
)
