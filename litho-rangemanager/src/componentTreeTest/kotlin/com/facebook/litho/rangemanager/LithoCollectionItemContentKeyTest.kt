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

package com.facebook.litho.rangemanager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.Component
import com.facebook.litho.LithoView
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.SizeConstraints
import com.meta.rangemanager.CollectionItemProvider
import com.meta.rangemanager.KeyBasedRangeWorker
import com.meta.rangemanager.PreparableCollectionItem
import com.meta.rangemanager.PrepareScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A [LithoCollectionItem] rebuilds its component exactly when the [LithoCollectionItem.contentKey]
 * of the current data differs from the key it was built with.
 */
@RunWith(LithoTestRunner::class)
class LithoCollectionItemContentKeyTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val scope = PrepareScope()
  private val builtFrom = mutableListOf<Pair<Data, Component>>()
  private var data = Data(title = "first", unrendered = 0)
  private val item = LithoCollectionItem(
      context = context,
      dataProvider = { data },
      component = { data -> Row().also { builtFrom += data to it } },
      sizeConstraints = SizeConstraints.exact(100, 100),
      traceTag = "test",
      contentKey = { it.title },
  )
  private val worker = KeyBasedRangeWorker(
      object : CollectionItemProvider<String>(scope = scope) {
        override fun create(key: String): PreparableCollectionItem = item

        override fun contains(key: String): Boolean = true
      },
      scope,
  )

  @Test
  fun `a prepare with an equal key keeps the built component`() {
    prepare()
    val firstComponent = builtFrom.single().second

    data = Data(title = data.title, unrendered = 1)
    prepare()

    assertThat(builtFrom).hasSize(1)
    assertThat(rootAfterBind()).isSameAs(firstComponent)
  }

  @Test
  fun `a prepare with a changed key builds the component from the new data`() {
    prepare()

    data = Data(title = "second", unrendered = data.unrendered)
    prepare()

    assertThat(builtFrom.map { it.first.title }).containsExactly("first", "second")
    assertThat(rootAfterBind()).isSameAs(builtFrom.last().second)
  }

  @Test
  fun `a bind with a changed key builds the component from the new data`() {
    prepare()
    val view = LithoView(context)
    item.bind(view)

    data = Data(title = "second", unrendered = data.unrendered)
    item.bind(view)

    assertThat(builtFrom.map { it.first.title }).containsExactly("first", "second")
    assertThat(checkNotNull(view.componentTree).root).isSameAs(builtFrom.last().second)
  }

  private fun prepare() {
    worker.prepareKey(KEY)
  }

  private fun rootAfterBind(): Component? {
    val view = LithoView(context)
    item.bind(view)
    return checkNotNull(view.componentTree).root
  }

  private class Data(val title: String, val unrendered: Int)

  private companion object {
    const val KEY = "row"
  }
}
