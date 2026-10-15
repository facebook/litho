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

package com.facebook.litho.layout

import android.view.View
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.State
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.useState
import com.facebook.litho.view.viewTag
import com.facebook.rendercore.px
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class RenderWithConstraintsReadTrackingTest {

  @Rule @JvmField val lithoTestRule = LithoTestRule()

  @Test
  fun `wrap-content RenderWithConstraints grows when state read inside it changes`() {
    assertContentWidthFollowsState(initialWidth = 50, updatedWidth = 200)
  }

  @Test
  fun `wrap-content RenderWithConstraints grows from zero when state read inside it changes`() {
    assertContentWidthFollowsState(initialWidth = 0, updatedWidth = 200)
  }

  @Test
  fun `wrap-content RenderWithConstraints shrinks when state read inside it changes`() {
    assertContentWidthFollowsState(initialWidth = 200, updatedWidth = 50)
  }

  @Test
  fun `wrap-content RenderWithConstraints shrinks to zero when state read inside it changes`() {
    assertContentWidthFollowsState(initialWidth = 200, updatedWidth = 0)
  }

  private fun assertContentWidthFollowsState(initialWidth: Int, updatedWidth: Int) {
    val width = AtomicReference<State<Int>>()

    val testLithoView =
        lithoTestRule.render(widthPx = 1000, heightPx = 1000) {
          Row {
            child(
                Column(style = Style.viewTag(WRAPPER_TAG).padding(horizontal = PADDING.px)) {
                  child(RenderWithConstraints { SizedChild(initialWidth, width) })
                },
            )
          }
        }
    assertThat(contentWidth(testLithoView.lithoView)).isEqualTo(initialWidth)

    lithoTestRule.act(testLithoView) { width.get().update(updatedWidth) }

    assertThat(contentWidth(testLithoView.lithoView)).isEqualTo(updatedWidth)
  }

  private class SizedChild(
      private val initialWidth: Int,
      private val width: AtomicReference<State<Int>>,
  ) : KComponent() {
    override fun ComponentScope.render(): Component {
      val widthState = useState { initialWidth }
      width.set(widthState)
      return Column(style = Style.width(widthState.value.px).height(10.px))
    }
  }

  // The padding keeps the wrapper mounted when the content is zero wide.
  private fun contentWidth(lithoView: View): Int =
      checkNotNull(lithoView.findViewWithTag<View>(WRAPPER_TAG)).width - 2 * PADDING

  private companion object {
    const val WRAPPER_TAG = "wrapper"
    const val PADDING = 10
  }
}
