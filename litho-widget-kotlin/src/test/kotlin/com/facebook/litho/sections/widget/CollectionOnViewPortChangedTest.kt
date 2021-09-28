/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho.sections.widget

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentScope
import com.facebook.litho.Handle
import com.facebook.litho.KComponent
import com.facebook.litho.LithoView
import com.facebook.litho.Style
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.view.onClick
import com.facebook.litho.view.viewTag
import com.facebook.litho.widget.SectionsRecyclerView
import com.facebook.litho.widget.Text
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/** Tests for [Collection]'s pagination prop */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(AndroidJUnit4::class)
class CollectionOnViewPortChangedTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  @Test
  fun `test Collection pagination callback receives correct updates`() {
    val firstVisibleIndexValue = AtomicInteger()
    val lastVisibleIndexValue = AtomicInteger()
    val totalCountValue = AtomicInteger()
    val firstFullyVisibleIndexValue = AtomicInteger()
    val lastFullyVisibleIndexValue = AtomicInteger()

    class Test : KComponent() {
      override fun ComponentScope.render(): Component? {

        val onViewportChanged =
            {
            c: ComponentContext,
            firstVisibleIndex: Int,
            lastVisibleIndex: Int,
            totalCount: Int,
            firstFullyVisibleIndex: Int,
            lastFullyVisibleIndex: Int ->
          firstVisibleIndexValue.set(firstVisibleIndex)
          lastVisibleIndexValue.set(lastVisibleIndex)
          totalCountValue.set(totalCount)
          firstFullyVisibleIndexValue.set(firstFullyVisibleIndex)
          lastFullyVisibleIndexValue.set(lastFullyVisibleIndex)
        }

        val handle = Handle()
        return Collection(
            handle = handle,
            onViewportChanged = onViewportChanged,
            style =
                Style.viewTag("collection_tag").onClick {
                  Collection.scrollTo(context, handle, 4)
                }) { (0..4).forEach { child { Text("Child $it") } } }
      }
    }
    lithoViewRule.setSizePx(100, 100)
    lithoViewRule.render { Test() }

    assertThat(firstVisibleIndexValue.get()).isEqualTo(0)
    assertThat(lastVisibleIndexValue.get()).isEqualTo(2)
    assertThat(totalCountValue.get()).isEqualTo(5)
    assertThat(firstFullyVisibleIndexValue.get()).isEqualTo(0)
    assertThat(lastFullyVisibleIndexValue.get()).isEqualTo(1)

    lithoViewRule.act {
      val recyclerView =
          ((findViewWithTag("collection_tag") as LithoView).getChildAt(0) as SectionsRecyclerView)
              .recyclerView

      // Scroll by a distance less than the item height so the first and last items overlap the
      // edges
      recyclerView?.scrollBy(0, 50)
    }

    assertThat(firstVisibleIndexValue.get()).isEqualTo(1)
    assertThat(lastVisibleIndexValue.get()).isEqualTo(3)
    assertThat(totalCountValue.get()).isEqualTo(5)
    assertThat(firstFullyVisibleIndexValue.get()).isEqualTo(2)
    assertThat(lastFullyVisibleIndexValue.get()).isEqualTo(2)
  }
}
