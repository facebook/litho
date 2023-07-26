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

import android.graphics.Rect
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.config.ComponentsConfiguration.isIncrementalMountGloballyDisabled
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.visibility.onVisible
import com.facebook.rendercore.px
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class LithoViewVisibilityProcessingTest {

  @get:Rule
  val lithoViewRule =
      LithoViewRule(
          componentsConfiguration =
              ComponentsConfiguration.getDefaultComponentsConfiguration().apply {
                isIncrementalMountGloballyDisabled = true
              })

  @Test
  fun `should process visibility outputs with the current visible rect when mount state is dirty and incremental mount disabled`() {
    val firstComponentOnVisibleCounter = AtomicInteger()
    val secondComponentOnVisibleCounter = AtomicInteger()

    val firstComponent = MyTestComponent(firstComponentOnVisibleCounter)
    val secondComponent = MyTestComponent(secondComponentOnVisibleCounter)

    /* We start out by rendering two different components in two different litho views */
    val firstLithoView = lithoViewRule.render { firstComponent }.lithoView
    val secondLithoView = lithoViewRule.render { secondComponent }.lithoView

    /* we verify that both components on visible callback was triggered */
    assertThat(firstComponentOnVisibleCounter.get()).isEqualTo(1)
    assertThat(secondComponentOnVisibleCounter.get()).isEqualTo(1)

    /* we re-use the second litho view component tree, and set it in the first litho view. */
    firstLithoView.componentTree = secondLithoView.componentTree

    /* immediately after, we "fake" translate down the component out of the viewport, so that it is not visible; calling translate
     * is not possible in this test because it requires a wrapping view of the LithoView */
    firstLithoView.notifyVisibleBoundsChanged(Rect(0, 200, 200, 600), true)

    /* during the translation, the used rect should be the visible rect (and not the last stored one in the visibility extensions),
    and therefore no extra visible callback should be called. */
    assertThat(firstComponentOnVisibleCounter.get()).isEqualTo(1)
    assertThat(secondComponentOnVisibleCounter.get()).isEqualTo(1)
  }

  class MyTestComponent(private val atomicCounter: AtomicInteger) : KComponent() {

    override fun ComponentScope.render(): Component {

      return Row {
        child(
            Text(
                "Hello",
                style =
                    Style.height(200.px).width(200.px).onVisible {
                      atomicCounter.incrementAndGet()
                    }))
      }
    }
  }
}
