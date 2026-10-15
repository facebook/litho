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

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.litho.Component
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.TestLithoView
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class HorizontalScrollKComponentTest {

  @get:Rule val lithoTestRule = LithoTestRule()

  @Test
  fun render_placesAllThreeScrollSectionsInOrder() {
    val testLithoView = render(HorizontalScrollKComponent())
    val collection = requireNotNull(testLithoView.findCollectionComponent())

    assertThat(collection.itemCount).isEqualTo(3)
    assertThat(collection.items.map { it.component?.javaClass?.simpleName })
        .containsExactly(
            "FixedHeightHScroll",
            "WrapFirstItemHeightHScroll",
            "WrapDynamicHScroll",
        )
  }

  @Test
  fun fixedAndDynamicSections_includeZeroThroughTenInHorizontalLists() {
    for (component in listOf(FixedHeightHScroll(), WrapDynamicHScroll())) {
      val testLithoView = render(component)
      val collection = requireNotNull(testLithoView.findCollectionComponent())

      assertThat(collection.itemCount).isEqualTo(11)
      assertThat(collection.items.map { it.id }).containsExactlyElementsOf((0..10).toList())
      assertThat(findLayoutManager(testLithoView).orientation).isEqualTo(RecyclerView.HORIZONTAL)
    }
  }

  @Test
  fun firstItemHeightSection_includesOneThroughTenInAHorizontalList() {
    val testLithoView = render(WrapFirstItemHeightHScroll())
    val collection = requireNotNull(testLithoView.findCollectionComponent())

    assertThat(collection.itemCount).isEqualTo(10)
    assertThat(collection.items.map { it.id }).containsExactlyElementsOf((1..10).toList())
    assertThat(findLayoutManager(testLithoView).orientation).isEqualTo(RecyclerView.HORIZONTAL)
  }

  private fun render(component: Component): TestLithoView =
      lithoTestRule.render(widthPx = 500, heightPx = 500) { component }

  private fun findLayoutManager(testLithoView: TestLithoView): LinearLayoutManager {
    val recyclerView = requireNotNull(findRecyclerView(testLithoView.lithoView))
    return recyclerView.layoutManager as LinearLayoutManager
  }

  private fun findRecyclerView(view: View): RecyclerView? {
    if (view is RecyclerView) {
      return view
    }
    if (view !is ViewGroup) {
      return null
    }
    for (index in 0 until view.childCount) {
      findRecyclerView(view.getChildAt(index))?.let {
        return it
      }
    }
    return null
  }
}
