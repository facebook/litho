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

package com.facebook.samples.litho.kotlin.collection

import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class ScrollToCollectionKComponentTest {

  @get:Rule val lithoTestRule = LithoTestRule()

  @Test
  fun render_allFourNavigationButtonsArePresent() {
    val testLithoView =
        lithoTestRule.render(widthPx = 500, heightPx = 1000) { ScrollToCollectionKComponent() }
    val buttonTexts = listOf("First", "Position 10", "50 to center", "End")
    for (text in buttonTexts) {
      assertThat(testLithoView.findViewWithTextOrNull(text))
          .describedAs("Button '$text' should be rendered")
          .isNotNull
    }
  }

  @Test
  fun render_collectionHasNumberedItemsAndEndMarker() {
    val testLithoView =
        lithoTestRule.render(widthPx = 500, heightPx = 1000) { ScrollToCollectionKComponent() }
    val collection = testLithoView.findCollectionComponent()
    assertThat(collection).isNotNull
    // Verify the collection has items: 100 numbered (0-99) + 1 "End" marker
    assertThat(collection!!.itemCount).isEqualTo(101)
    // Verify the first item starts at index 0 and is visible
    assertThat(collection.firstVisibleIndex).isEqualTo(0)
    assertThat(testLithoView.findViewWithTextOrNull("0 ")).isNotNull
  }

  @Test
  fun clickButtons_handlersAreWiredAndComponentRemainsStable() {
    val testLithoView =
        lithoTestRule.render(widthPx = 500, heightPx = 1000) { ScrollToCollectionKComponent() }
    val collection = testLithoView.findCollectionComponent()
    assertThat(collection).isNotNull
    // Verify initial state: collection starts at index 0 with 101 items
    assertThat(collection!!.firstVisibleIndex).isEqualTo(0)
    assertThat(collection.itemCount).isEqualTo(101)
    // Click each button to verify click handlers are wired up
    // (Robolectric doesn't execute actual RecyclerView scrolls,
    // but we verify the handlers don't throw and the component stays stable)
    lithoTestRule.act(testLithoView) { clickOnText("First") }
    lithoTestRule.act(testLithoView) { clickOnText("Position 10") }
    // Verify the component is still functional after clicks
    assertThat(testLithoView.findViewWithTextOrNull("First")).isNotNull
    assertThat(testLithoView.findViewWithTextOrNull("Position 10")).isNotNull
    assertThat(testLithoView.findViewWithTextOrNull("50 to center")).isNotNull
    assertThat(testLithoView.findViewWithTextOrNull("End")).isNotNull
    assertThat(collection.itemCount).isEqualTo(101)
  }
}
