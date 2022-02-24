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

import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.LithoView
import com.facebook.litho.Style
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.TestLithoView
import com.facebook.litho.view.viewTag
import com.facebook.litho.widget.SectionsRecyclerView
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(AndroidJUnit4::class)
class LazyCollectionControllerTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  private fun getLazyCollectionRecyclerView(
      testLithoView: TestLithoView,
      lazyCollectionTag: String
  ): RecyclerView? =
      ((testLithoView.findViewWithTagOrNull(lazyCollectionTag) as LithoView?)?.getChildAt(0) as
              SectionsRecyclerView?)
          ?.recyclerView

  @Test
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
}
