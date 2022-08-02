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

package com.facebook.litho.widget.collection

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.onClick
import com.facebook.litho.view.viewTag
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/** Tests for [Collection]'s pagination prop */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class CollectionPaginationTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  @Test
  fun `test Collection pagination callback receives correct updates`() {
    val onNearEndCallbackCount = AtomicInteger()

    class Test : KComponent() {
      override fun ComponentScope.render(): Component {
        val controller = LazyCollectionController()
        return LazyList(
            lazyCollectionController = controller,
            onNearEnd = OnNearCallback { onNearEndCallbackCount.incrementAndGet() },
            style = Style.viewTag("collection_tag").onClick { controller.scrollToIndex(4) }) {
              (0..4).forEach { child(Text("Child $it")) }
            }
      }
    }

    val testLithoView = lithoViewRule.render(widthPx = 100, heightPx = 100) { Test() }
    lithoViewRule.idle()

    assertThat(onNearEndCallbackCount.get()).isEqualTo(0)

    lithoViewRule.act(testLithoView) { clickOnTag("collection_tag") }

    lithoViewRule.render(lithoView = testLithoView.lithoView, widthPx = 100, heightPx = 100) {
      Test()
    }

    assertThat(onNearEndCallbackCount.get()).isEqualTo(1)
  }
}
