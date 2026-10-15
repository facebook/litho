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

package com.facebook.samples.litho.kotlin.animations.animationcomposition

import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class LeftRightBlocksComponentTest {

  @Rule @JvmField val lithoTestRule = LithoTestRule()

  private val containerWidth = 500
  private val containerHeight = 500

  @Test
  fun `initial state right-aligns all children at container edge`() {
    val testLithoView =
        lithoTestRule.render(widthPx = containerWidth, heightPx = containerHeight) {
          LeftRightBlocksComponent()
        }
    val rootResult = testLithoView.currentRootNode!!
    // With FLEX_END alignment (left=false), each child should be at x = containerWidth - childWidth
    for (i in 0 until rootResult.childrenCount) {
      val childWidth = rootResult.getChildAt(i).width
      val childX = rootResult.getXForChildAtIndex(i)
      assertThat(childX)
          .describedAs("Child $i should be right-aligned")
          .isEqualTo(containerWidth - childWidth)
    }
  }

  @Test
  fun `click toggles all children from right edge to left edge`() {
    val testLithoView =
        lithoTestRule.render(widthPx = containerWidth, heightPx = containerHeight) {
          LeftRightBlocksComponent()
        }
    val rootResult = testLithoView.currentRootNode!!

    // Verify initial right-alignment (FLEX_END)
    for (i in 0 until rootResult.childrenCount) {
      assertThat(rootResult.getXForChildAtIndex(i))
          .describedAs("Child $i initial X")
          .isGreaterThan(0)
    }

    // Click to toggle state (FLEX_END -> FLEX_START)
    lithoTestRule.act(testLithoView) { clickOnRootView() }

    // After click, all children should be at the left edge (FLEX_START: x = 0)
    val updatedRoot = testLithoView.currentRootNode!!
    for (i in 0 until updatedRoot.childrenCount) {
      assertThat(updatedRoot.getXForChildAtIndex(i))
          .describedAs("Child $i X after click")
          .isEqualTo(0)
    }
  }

  @Test
  fun `double click restores original right-aligned positions`() {
    val testLithoView =
        lithoTestRule.render(widthPx = containerWidth, heightPx = containerHeight) {
          LeftRightBlocksComponent()
        }

    // Record initial child X positions (FLEX_END)
    val rootResult = testLithoView.currentRootNode!!
    val initialPositions =
        (0 until rootResult.childrenCount).map { rootResult.getXForChildAtIndex(it) }

    // Click twice to toggle back to original state
    lithoTestRule.act(testLithoView) { clickOnRootView() }
    lithoTestRule.act(testLithoView) { clickOnRootView() }

    // After double click, all children should be back at their original positions
    val restoredRoot = testLithoView.currentRootNode!!
    for (i in 0 until restoredRoot.childrenCount) {
      assertThat(restoredRoot.getXForChildAtIndex(i))
          .describedAs("Child $i X after double click")
          .isEqualTo(initialPositions[i])
    }
  }
}
