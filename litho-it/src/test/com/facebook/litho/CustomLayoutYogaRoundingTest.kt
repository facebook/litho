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

import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.paddingPercent
import com.facebook.litho.core.width
import com.facebook.litho.layout.LayoutFunctionN
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A `Layout` in a Yoga tree is rounded by Yoga as its builder rounds it. */
@OptIn(ExperimentalLithoApi::class)
@RunWith(LithoTestRunner::class)
class CustomLayoutYogaRoundingTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  /**
   * Its percent padding comes to 2.1 px an edge, and the height it declares to 10 plus 4.2: rounded
   * up, as Yoga rounds a node with a measure function, it would be 15, and its function would run
   * again for content 1 px taller than it measured.
   */
  @Test
  fun `sized to a fraction, it's built under the constraints it was measured under`() {
    val runs = ArrayList<SizeConstraints>()
    val layout = LayoutFunctionN { _, constraints ->
      runs += constraints
      layout(constraints.maxWidth, 10) {}
    }

    lithoTestRule.render(widthPx = 400, heightPx = 400) {
      Column(style = Style.width(300.px)) {
        child(Layout(emptyList(), style = Style.paddingPercent(all = 0.7f), layout = layout))
      }
    }

    assertThat(runs).describedAs("its function's runs").hasSize(1)
  }
}
