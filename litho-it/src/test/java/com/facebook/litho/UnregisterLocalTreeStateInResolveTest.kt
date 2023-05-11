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
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.BackgroundLayoutLooperRule
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.TestLithoView
import com.facebook.litho.testing.assertj.LithoAssertions
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.onClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class UnregisterLocalTreeStateInResolveTest {

  @get:Rule val lithoViewRule: LithoViewRule = LithoViewRule()

  @get:Rule
  val backgroundLayoutLooperRule: BackgroundLayoutLooperRule = BackgroundLayoutLooperRule()

  /**
   * This test describes a bug we have found that led to a new [Component] to not re-calculate its
   * initial state and to use a previous redundant one.
   *
   * This happened because we were not unregistering pending state handlers whenever a "resolve" or
   * "layout" state handler was not completed.
   */
  @Test
  fun `the initial state container is cleaned on non-commited resolves`() {
    val lithoView = createEmptyLithoView()
    lithoViewRule.idle()

    val componentTree = lithoView.componentTree
    componentTree.setFutureExecutionListener(
        object : TreeFuture.FutureExecutionListener {
          override fun onPreExecution(
              version: Int,
              futureExecutionType: TreeFuture.FutureExecutionType?,
              attribution: String?
          ) {
            if (version == 1 && attribution == "resolve") {
              componentTree.setRootSync(TestCounterComponent(initialCounter = 2))
            }
          }

          override fun onPostExecution(version: Int, released: Boolean, attribution: String?) {}
        })

    componentTree.setRootAsync(TestCounterComponent(initialCounter = 1))
    lithoViewRule.idle()
    LithoAssertions.assertThat(lithoView).hasVisibleText("Counter: 2")

    componentTree.setRootSync(EmptyComponent())
    LithoAssertions.assertThat(lithoView).doesNotHaveVisibleText("Counter: 2")

    componentTree.setRootSync(TestCounterComponent(initialCounter = 3))
    LithoAssertions.assertThat(lithoView).hasVisibleText("Counter: 3")
  }

  private fun createEmptyLithoView(): TestLithoView {
    val context = getApplicationContext<Context>()
    val componentContext = ComponentContext(context)

    val componentTree = ComponentTree.create(componentContext).build()

    val lithoView =
        lithoViewRule.createTestLithoView(
            componentTree = componentTree, widthPx = 1080, heightPx = 840) {
              EmptyComponent()
            }
    lithoView.attachToWindow().measure().layout()
    return lithoView
  }
}

private class TestCounterComponent(val initialCounter: Int) : KComponent() {
  override fun ComponentScope.render(): Component {
    val counter = useState { initialCounter }

    return Row {
      child(Text("Counter: ${counter.value}", style = Style.onClick { counter.update { it + 1 } }))
    }
  }
}
