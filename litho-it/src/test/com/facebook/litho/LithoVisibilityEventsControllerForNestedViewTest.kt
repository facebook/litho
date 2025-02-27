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
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.visibility.onInvisible
import com.facebook.litho.visibility.onVisible
import com.facebook.litho.widget.collection.LazyList
import com.facebook.rendercore.sp
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class LithoVisibilityEventsControllerForNestedViewTest {
  @Rule
  @JvmField
  val mLithoTestRule: LithoTestRule =
      LithoTestRule(lithoVisibilityEventsController = { lithoVisibilityEventsControllerDelegate })
  private val lithoVisibilityEventsControllerDelegate: LithoVisibilityEventsController =
      LithoVisibilityEventsControllerDelegate()
  private val invisibleTags: MutableSet<Int> = mutableSetOf()

  @Test
  fun `test visibility events for nested LithoView`() {
    val testLithoView =
        mLithoTestRule.render {
          LazyList(style = Style.height(100.sp).width(100.sp)) {
            for (i in 0 until 10) {
              child(
                  component =
                      VisibilityTrackingComponent(tag = i, invisibleTracking = invisibleTags))
            }
          }
        }
    testLithoView.lithoView.subscribeComponentTreeToVisibilityEventsController(
        lithoVisibilityEventsControllerDelegate)

    // testing initial state
    assertThat(
            testLithoView.lithoView.componentTree?.lithoVisibilityEventsController?.visibilityState)
        .isEqualTo(LithoVisibilityEventsController.LithoVisibilityState.HINT_VISIBLE)
    assertThat(invisibleTags).isEmpty()

    // move to invisible
    lithoVisibilityEventsControllerDelegate.moveToVisibilityState(
        LithoVisibilityEventsController.LithoVisibilityState.HINT_INVISIBLE)
    assertThat(invisibleTags).isEqualTo((0 until 10).toSet())

    // move to visible
    lithoVisibilityEventsControllerDelegate.moveToVisibilityState(
        LithoVisibilityEventsController.LithoVisibilityState.HINT_VISIBLE)
    assertThat(invisibleTags).isEmpty()
  }
}

class VisibilityTrackingComponent(val invisibleTracking: MutableSet<Int>, val tag: Int) :
    KComponent() {
  override fun ComponentScope.render(): Component {

    return Text(
        text = "test",
        style =
            Style.height(5.sp)
                .onVisible { invisibleTracking.remove(tag) }
                .onInvisible { invisibleTracking.add(tag) })
  }
}
