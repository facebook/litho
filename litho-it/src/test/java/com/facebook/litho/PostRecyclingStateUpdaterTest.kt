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

import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.core.padding
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.assertj.LithoAssertions
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.onClick
import com.facebook.litho.view.viewTag
import com.facebook.litho.widget.collection.LazyCollectionController
import com.facebook.litho.widget.collection.LazyList
import com.facebook.rendercore.dp
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/**
 * Test that checks if state update is still working after the list item of [LazyList] has been
 * recycled
 */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class PostRecyclingStateUpdaterTest {

  private val configuration: ComponentsConfiguration =
      ComponentsConfiguration.defaultInstance.copy(enableFacadeStateUpdater = true)
  @Rule @JvmField val lithoViewRule = LithoViewRule(componentsConfiguration = configuration)
  lateinit var stateRef: AtomicReference<Int>

  @Test
  fun `StateUpdaterDelegator updates lazyListItem after it was recycled`() {
    val lazyCollectionController = LazyCollectionController()

    val testLithoView =
        lithoViewRule.render(widthPx = 100, heightPx = 100) {
          getTestComponent(lazyCollectionController)
        }

    assertThat(stateRef.get())
        .isEqualTo(1)
        .describedAs("ListItem state is initialised with initial value")

    LithoAssertions.assertThat(testLithoView)
        .hasVisibleText("1")
        .doesNotHaveVisibleText("100")
        .describedAs("ListItem with index 1 is visible, ListItem with index 100 is not visible")

    lithoViewRule.act(testLithoView) { clickOnTag("test_view_1") }
    assertThat(stateRef.get()).describedAs("ListItem state is updated when clicked").isEqualTo(2)

    lazyCollectionController.scrollBy(0, 20000) // scroll to the end of the list

    LithoAssertions.assertThat(testLithoView)
        .hasVisibleText("100")
        .doesNotHaveVisibleText("1")
        .describedAs(
            "After scrolling to the end of the list ListItem with index 100 is visible and ListItem with index 1 is not visible")

    lazyCollectionController.scrollBy(0, -20000) // scroll to the top of the list

    LithoAssertions.assertThat(testLithoView)
        .hasVisibleText("1")
        .doesNotHaveVisibleText("100")
        .describedAs(
            "After scrolling to the top of the list ListItem with index 1 is visible and ListItem with index 100 is not visible")

    lithoViewRule.act(testLithoView) { clickOnTag("test_view_1") }

    assertThat(stateRef.get())
        .describedAs("ListItem state is updated when clicked after the view was recycled")
        .isEqualTo(3)
  }

  private fun getTestComponent(controller: LazyCollectionController): KComponent {
    class ListItem(private val listValue: Int) : KComponent() {

      override fun ComponentScope.render(): Component {

        val state = useState { 1 }
        if (listValue == 1) {
          stateRef = AtomicReference(state.value)
        }
        val listener = useCached {
          object : ClickListener {
            override fun onClick() {
              state.update { it + 1 }
            }
          }
        }
        return Text(
            text = listValue.toString(),
            style =
                Style.onClick { listener.onClick() }
                    .viewTag("test_view_${listValue}")
                    .padding(all = 50.dp),
        )
      }
    }
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        val list = ArrayList<Int>()
        for (i in 1..100) {
          list.add(i)
        }
        return LazyList(
            lazyCollectionController = controller, style = Style.viewTag("collection_tag")) {
              children(list, id = { it }) { ListItem(it) }
            }
      }
    }
    return TestComponent()
  }
}

interface ClickListener {
  fun onClick()
}
