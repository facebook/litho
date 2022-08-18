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

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.LithoView
import com.facebook.litho.Style
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.sections.SectionTree
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.TestLithoView
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.viewTag
import com.facebook.litho.widget.RecyclerEventsController
import com.facebook.litho.widget.SectionsRecyclerView
import com.facebook.litho.widget.SmoothScrollAlignmentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class LazyCollectionControllerTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  private fun getLazyCollectionRecyclerView(
      testLithoView: TestLithoView,
      lazyCollectionTag: String
  ): RecyclerView? =
      ((testLithoView.findViewWithTagOrNull(lazyCollectionTag) as LithoView?)?.getChildAt(0)
              as SectionsRecyclerView?)
          ?.recyclerView

  fun `test lazyCollectionController recyclerView reference is updated`() {
    val lazyCollectionController = LazyCollectionController()

    class Test : KComponent() {
      override fun ComponentScope.render(): Component {
        return LazyList(
            lazyCollectionController = lazyCollectionController,
            style = Style.viewTag("collection_tag"),
        ) {}
      }
    }

    assertThat(lazyCollectionController.recyclerView).isNull()

    val testLithoView = lithoViewRule.render(widthPx = 100, heightPx = 100) { Test() }
    lithoViewRule.idle()

    assertThat(lazyCollectionController.recyclerView)
        .isSameAs(getLazyCollectionRecyclerView(testLithoView, "collection_tag"))
  }

  @Test
  fun `test showRefreshing delegates to RecyclerEventsController`() {
    val lazyCollectionController = LazyCollectionController()

    val recyclerEventsController = mock<RecyclerEventsController>()
    lazyCollectionController.recyclerEventsController = recyclerEventsController

    lazyCollectionController.setRefreshing(true)
    verify(recyclerEventsController).showRefreshing()
  }

  @Test
  fun `test clearRefreshing delegates to RecyclerEventsController`() {
    val lazyCollectionController = LazyCollectionController()

    val recyclerEventsController = mock<RecyclerEventsController>()
    lazyCollectionController.recyclerEventsController = recyclerEventsController

    lazyCollectionController.setRefreshing(false)
    verify(recyclerEventsController).clearRefreshing()
  }

  @Test
  fun `test scrollBy delegates to RecyclerView`() {
    val lazyCollectionController = LazyCollectionController()

    val mockRecyclerView = mock<RecyclerView>()
    val recyclerEventsController = mock<RecyclerEventsController>()
    whenever(recyclerEventsController.recyclerView).thenReturn(mockRecyclerView)
    lazyCollectionController.recyclerEventsController = recyclerEventsController

    lazyCollectionController.scrollBy(10, 20)
    verify(mockRecyclerView).scrollBy(10, 20)

    lazyCollectionController.scrollBy(30, 40)
    verify(mockRecyclerView).scrollBy(30, 40)
  }

  @Test
  fun `test smoothScrollBy delegates to RecyclerView`() {
    val lazyCollectionController = LazyCollectionController()

    val mockRecyclerView = mock<RecyclerView>()
    val recyclerEventsController = mock<RecyclerEventsController>()
    whenever(recyclerEventsController.recyclerView).thenReturn(mockRecyclerView)
    lazyCollectionController.recyclerEventsController = recyclerEventsController

    lazyCollectionController.smoothScrollBy(10, 20)
    verify(mockRecyclerView).smoothScrollBy(10, 20)

    lazyCollectionController.smoothScrollBy(30, 40)
    verify(mockRecyclerView).smoothScrollBy(30, 40)
  }

  @Test
  fun `test scrollToIndex delegates to SectionTree`() {
    val lazyCollectionController = LazyCollectionController()

    val mockSectionTree = mock<SectionTree>()
    lazyCollectionController.sectionTree = mockSectionTree

    lazyCollectionController.scrollToIndex(5, 10)
    verify(mockSectionTree).requestFocusOnRoot(5, 10)
  }

  @Test
  fun `test smoothScrollToIndex delegates to SectionTree`() {
    val lazyCollectionController = LazyCollectionController()

    val mockSectionTree = mock<SectionTree>()
    lazyCollectionController.sectionTree = mockSectionTree

    lazyCollectionController.smoothScrollToIndex(5, 10, SmoothScrollAlignmentType.SNAP_TO_START)
    verify(mockSectionTree).requestSmoothFocusOnRoot(5, 10, SmoothScrollAlignmentType.SNAP_TO_START)
  }

  @Test
  fun `test scrollToId brings child with id into viewport`() {
    val lazyCollectionController = LazyCollectionController()

    class Test : KComponent() {
      override fun ComponentScope.render(): Component =
          LazyList(
              lazyCollectionController = lazyCollectionController,
              style = Style.viewTag("collection_tag"),
          ) {
            (0..10).forEach { child(id = it, component = Text("$it", Style.viewTag("$it"))) }
          }
    }

    val testLithoView = lithoViewRule.render(widthPx = 100, heightPx = 100) { Test() }
    lithoViewRule.idle()

    lithoViewRule.act(testLithoView) { lazyCollectionController.scrollToId(9) }

    // Additional `setRoot()` to kick the test infra into applying RecyclerView changes
    testLithoView.setRoot(Test())
    lithoViewRule.idle()

    val recyclerView = getLazyCollectionRecyclerView(testLithoView, "collection_tag")
    assertThat(recyclerView).isNotNull
    recyclerView ?: return

    val visibleRange =
        with(recyclerView.layoutManager as LinearLayoutManager) {
          (findFirstVisibleItemPosition()..findLastVisibleItemPosition())
        }
    assertThat(visibleRange).contains(9)
  }
}
