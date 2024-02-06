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
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.onClick
import com.facebook.litho.view.viewTag
import com.facebook.litho.widget.collection.LazyList
import com.facebook.rendercore.px
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/** Tests for [useCallback] and capturing of props/state within collections lambdas. */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class UseCallbackTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  @Test
  fun useCallbackWithCollection_whenUseCallbackCapturesState_stateInCallbackIsUpToDate() {
    val renderCount = AtomicInteger(0)
    class CollectionWithSelectedRows(
        private val data: List<Int>,
        private val onSelectedChanged: (List<Int>) -> Unit,
        private val renderCount: AtomicInteger,
    ) : KComponent() {
      override fun ComponentScope.render(): Component? {
        val selected = useState { listOf<Int>() }

        useEffect(selected) {
          onSelectedChanged(selected.value)
          null
        }

        val clickCallback = useCallback { selectedItem: Int ->
          selected.updateSync(selected.value + listOf(selectedItem))
        }

        return LazyList {
          data.forEach { item ->
            child(
                id = item,
                component =
                    CollectionRow(
                        style = Style.viewTag("item_$item"),
                        item = item,
                        onRowClick = clickCallback,
                        renderCount = renderCount))
          }
        }
      }
    }

    val selectionEvents = mutableListOf<List<Int>>()
    val lithoView =
        lithoViewRule.render(widthPx = 1000, heightPx = 1000) {
          CollectionWithSelectedRows(
              data = listOf(1, 2, 3, 4, 5),
              onSelectedChanged = { list -> selectionEvents.add(list) },
              renderCount = renderCount)
        }

    assertThat(renderCount.get()).isEqualTo(5)

    lithoViewRule.act(lithoView) { clickOnTag("item_1") }

    assertThat(renderCount.get()).isEqualTo(5)

    lithoViewRule.act(lithoView) { clickOnTag("item_2") }

    assertThat(selectionEvents).containsExactly(listOf(), listOf(1), listOf(1, 2))
    assertThat(renderCount.get()).isEqualTo(5)
  }

  @Test
  fun useCallbackWithCollection_whenUseCallbackCapturesProps_propsAreUpToDate() {
    val renderCount = AtomicInteger(0)
    class CollectionWithUseCallback(
        private val data: List<Int>,
        private val rowClickedTag: String,
        private val onRowClick: (String) -> Unit,
        private val renderCount: AtomicInteger,
    ) : KComponent() {
      override fun ComponentScope.render(): Component? {
        val clickCallback = useCallback { _: Int -> onRowClick(rowClickedTag) }
        return LazyList {
          data.forEach { item ->
            child(
                id = item,
                component =
                    CollectionRow(
                        style = Style.viewTag("item_$item"),
                        item = item,
                        onRowClick = clickCallback,
                        renderCount = renderCount))
          }
        }
      }
    }

    val rowClickedEvents = mutableListOf<String>()
    val lithoView =
        lithoViewRule.render(widthPx = 1000, heightPx = 1000) {
          CollectionWithUseCallback(
              data = listOf(1, 2, 3, 4, 5),
              rowClickedTag = "setRoot1",
              onRowClick = { data -> rowClickedEvents.add(data) },
              renderCount = renderCount)
        }

    assertThat(renderCount.get()).isEqualTo(5)

    lithoViewRule.act(lithoView) { clickOnTag("item_1") }
    lithoViewRule.act(lithoView) { clickOnTag("item_2") }

    assertThat(renderCount.get()).isEqualTo(5)

    lithoView.setRoot(
        CollectionWithUseCallback(
            data = listOf(1, 2, 3, 4, 5),
            rowClickedTag = "setRoot2",
            onRowClick = { data -> rowClickedEvents.add(data) },
            renderCount = renderCount))
    lithoViewRule.idle()

    assertThat(renderCount.get()).isEqualTo(5)

    lithoViewRule.act(lithoView) { clickOnTag("item_1") }
    lithoViewRule.act(lithoView) { clickOnTag("item_2") }

    assertThat(renderCount.get()).isEqualTo(5)
    assertThat(rowClickedEvents).containsExactly("setRoot1", "setRoot1", "setRoot2", "setRoot2")
  }

  /**
   * This ends up showing correct behavior since the updateSync lambda will receive the latest state
   * value at the time its invoked (i.e. it does not use captured state).
   */
  @Test
  fun collection_whenCallbackUsesLambdaStateUpdate_stateInCallbackIsUpToDate() {
    val renderCount = AtomicInteger(0)
    class CollectionWithSelectedRows(
        private val data: List<Int>,
        private val onSelectedChanged: (List<Int>) -> Unit,
        private val renderCount: AtomicInteger,
    ) : KComponent() {
      override fun ComponentScope.render(): Component? {
        val selected = useState { listOf<Int>() }

        useEffect(selected) {
          onSelectedChanged(selected.value)
          null
        }

        val onClick = useCallback { item: Int ->
          selected.updateSync { oldList -> oldList + listOf(item) }
        }

        return LazyList {
          data.forEach { item ->
            child(
                id = item,
                component =
                    CollectionRow(
                        style = Style.viewTag("item_$item"),
                        item = item,
                        onRowClick = onClick,
                        renderCount = renderCount))
          }
        }
      }
    }

    val selectionEvents = mutableListOf<List<Int>>()
    val lithoView =
        lithoViewRule.render(widthPx = 1000, heightPx = 1000) {
          CollectionWithSelectedRows(
              data = listOf(1, 2, 3, 4, 5),
              onSelectedChanged = { list -> selectionEvents.add(list) },
              renderCount = renderCount)
        }

    assertThat(renderCount.get()).isEqualTo(5)

    lithoViewRule.act(lithoView) { clickOnTag("item_1") }
    lithoViewRule.act(lithoView) { clickOnTag("item_2") }

    assertThat(renderCount.get()).isEqualTo(5)
    assertThat(selectionEvents).containsExactly(listOf(), listOf(1), listOf(1, 2))
  }
}

private class CollectionRow(
    val style: Style,
    val item: Int,
    val onRowClick: ((Int) -> Unit)? = null,
    val renderCount: AtomicInteger
) : KComponent() {
  override fun ComponentScope.render(): Component? {
    renderCount.incrementAndGet()
    return Row(
        style = Style.onClick { onRowClick?.invoke(item) }.width(100.px).height(100.px) + style)
  }
}
