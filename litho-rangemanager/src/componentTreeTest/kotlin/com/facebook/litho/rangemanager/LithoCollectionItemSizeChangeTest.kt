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
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.LithoView
import com.facebook.litho.State
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.useState
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.toHeightSpec
import com.facebook.rendercore.toWidthSpec
import com.meta.rangemanager.CollectionItemProvider
import com.meta.rangemanager.KeyBasedRange
import com.meta.rangemanager.KeyBasedRangeWorker
import com.meta.rangemanager.PreparableCollectionItem
import com.meta.rangemanager.PrepareScope
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(LithoTestRunner::class)
class LithoCollectionItemSizeChangeTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Test
  fun `unbound item lays out the same component tree at new constraints on bind`() {
    var constraints = SizeConstraints.exact(100, 100)
    val componentBuilds = AtomicInteger()
    val scope = PrepareScope()
    val item = LithoCollectionItem(
        context = context,
        dataProvider = { 1 },
        component = { _ ->
          componentBuilds.incrementAndGet()
          Row()
        },
        sizeConstraints = constraints,
        // Unlike the fixed fallback above, the provider reads the latest value after a resize.
        sizeConstraintsProvider = { constraints },
        traceTag = "test",
    )
    val worker = KeyBasedRangeWorker(providerOf(item, scope), scope)
    val view = LithoView(context)
    worker.schedule(Unit, Keys(KEY))
    item.bind(view)
    val originalTree = checkNotNull(view.componentTree)
    item.unbind()

    constraints = SizeConstraints.exact(200, 100)
    item.bind(view)

    assertThat(view.componentTree).isSameAs(originalTree)
    assertThat(componentBuilds.get()).isEqualTo(1)
    assertThat(
        originalTree.hasCompatibleLayout(
            constraints.toWidthSpec(),
            constraints.toHeightSpec(),
        ),
    )
        .isTrue()
  }

  @Test
  fun `bound item leaves changed constraints to LithoView measurement`() {
    var data = 1
    var constraints = SizeConstraints.exact(100, 100)
    val componentBuilds = AtomicInteger()
    val scope = PrepareScope()
    val item = LithoCollectionItem(
        context = context,
        dataProvider = { data },
        component = { _ ->
          componentBuilds.incrementAndGet()
          Row()
        },
        sizeConstraints = constraints,
        sizeConstraintsProvider = { constraints },
        traceTag = "test",
    )
    val worker = KeyBasedRangeWorker(providerOf(item, scope), scope)
    val view = LithoView(context)
    view.onAttachedToWindowForTest()
    worker.schedule(Unit, Keys(KEY))
    item.bind(view)
    val originalTree = checkNotNull(view.componentTree)

    constraints = SizeConstraints.exact(200, 100)
    item.bind(view)

    assertThat(view.componentTree).isSameAs(originalTree)
    assertThat(componentBuilds.get()).isEqualTo(1)
    assertThat(
        originalTree.hasCompatibleLayout(
            constraints.toWidthSpec(),
            constraints.toHeightSpec(),
        ),
    )
        .isFalse()

    view.measure(constraints.toWidthSpec(), constraints.toHeightSpec())
    view.layout(0, 0, view.measuredWidth, view.measuredHeight)

    assertThat(
        originalTree.hasCompatibleLayout(
            constraints.toWidthSpec(),
            constraints.toHeightSpec(),
        ),
    )
        .isTrue()

    data = 2
    item.bind(view)

    assertThat(componentBuilds.get()).isEqualTo(2)
  }

  @Test
  fun `bound item with a changed key replaces its content at the view's specs`() {
    var data = 1
    val measured = SizeConstraints.exact(100, 100)
    var constraints = measured
    val renderedData = AtomicInteger()
    val scope = PrepareScope()
    val item = LithoCollectionItem(
        context = context,
        dataProvider = { data },
        component = { d -> DataRow(d, renderedData) },
        sizeConstraints = constraints,
        sizeConstraintsProvider = { constraints },
        traceTag = "test",
    )
    val worker = KeyBasedRangeWorker(providerOf(item, scope), scope)
    val view = LithoView(context)
    view.onAttachedToWindowForTest()
    worker.schedule(Unit, Keys(KEY))
    item.bind(view)
    val originalTree = checkNotNull(view.componentTree)
    view.measure(measured.toWidthSpec(), measured.toHeightSpec())
    view.layout(0, 0, view.measuredWidth, view.measuredHeight)

    data = 2
    constraints = SizeConstraints.exact(200, 100)
    item.bind(view)

    assertThat(view.componentTree).isSameAs(originalTree)
    assertThat(renderedData.get()).isEqualTo(2)
    assertThat(originalTree.hasCompatibleLayout(measured.toWidthSpec(), measured.toHeightSpec()))
        .isTrue()
    assertThat(view.isLayoutRequested).isFalse()
  }

  @Test
  fun `item without a provider keeps the size its view measured`() {
    val fixed = SizeConstraints.exact(100, 100)
    val measured = SizeConstraints.exact(80, 100)
    val scope = PrepareScope()
    val item = LithoCollectionItem(
        context = context,
        dataProvider = { 1 },
        component = { _ -> Row() },
        sizeConstraints = fixed,
        traceTag = "test",
    )
    val worker = KeyBasedRangeWorker(providerOf(item, scope), scope)
    val view = LithoView(context)
    view.onAttachedToWindowForTest()
    worker.schedule(Unit, Keys(KEY))
    item.bind(view)
    val originalTree = checkNotNull(view.componentTree)
    view.measure(measured.toWidthSpec(), measured.toHeightSpec())
    view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    item.unbind()

    worker.prepareKey(KEY)
    item.bind(view)

    assertThat(originalTree.hasCompatibleLayout(measured.toWidthSpec(), measured.toHeightSpec()))
        .isTrue()
  }

  @Test
  fun `key that ignores a layout input still lays out at the newest constraints`() {
    var snapshot = Snapshot(id = 1, width = 100)
    val componentBuilds = AtomicInteger()
    val scope = PrepareScope()
    val item = LithoCollectionItem(
        context = context,
        dataProvider = { snapshot },
        component = { _ ->
          componentBuilds.incrementAndGet()
          Row()
        },
        sizeConstraints = SizeConstraints.exact(100, 100),
        contentKey = { it.id },
        sizeConstraintsProvider = { SizeConstraints.exact(it.width, 100) },
        traceTag = "test",
    )
    val worker = KeyBasedRangeWorker(providerOf(item, scope), scope)
    val view = LithoView(context)
    worker.schedule(Unit, Keys(KEY))
    item.bind(view)
    val originalTree = checkNotNull(view.componentTree)
    item.unbind()

    snapshot = Snapshot(id = 1, width = 200)
    item.bind(view)

    assertThat(view.componentTree).isSameAs(originalTree)
    assertThat(componentBuilds.get()).isEqualTo(1)
    assertThat(
        originalTree.hasCompatibleLayout(
            SizeConstraints.exact(200, 100).toWidthSpec(),
            SizeConstraints.exact(200, 100).toHeightSpec(),
        ),
    )
        .isTrue()
  }

  @Test
  fun `tree re-measured by its view is laid out at the current constraints on the next bind`() {
    val prepared = SizeConstraints.exact(100, 100)
    val measured = SizeConstraints.exact(200, 100)
    var constraints = prepared
    val scope = PrepareScope()
    val item = LithoCollectionItem(
        context = context,
        dataProvider = { 1 },
        component = { _ -> Row() },
        sizeConstraints = constraints,
        sizeConstraintsProvider = { constraints },
        traceTag = "test",
    )
    val worker = KeyBasedRangeWorker(providerOf(item, scope), scope)
    val view = LithoView(context)
    view.onAttachedToWindowForTest()
    worker.schedule(Unit, Keys(KEY))
    item.bind(view)
    val originalTree = checkNotNull(view.componentTree)

    constraints = measured
    view.measure(measured.toWidthSpec(), measured.toHeightSpec())
    view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    item.unbind()
    constraints = prepared
    item.bind(view)

    assertThat(originalTree.hasCompatibleLayout(prepared.toWidthSpec(), prepared.toHeightSpec()))
        .isTrue()
  }

  @Test
  fun `size-only layout after a state update does not render the row again`() {
    var constraints = SizeConstraints.exact(100, 100)
    val renders = AtomicInteger()
    val state = AtomicReference<State<Int>>()
    val renderedValue = AtomicInteger()
    val scope = PrepareScope()
    val item = LithoCollectionItem(
        context = context,
        dataProvider = { 1 },
        component = { _ -> StatefulRow(renders, state, renderedValue) },
        sizeConstraints = constraints,
        sizeConstraintsProvider = { constraints },
        traceTag = "test",
    )
    val worker = KeyBasedRangeWorker(providerOf(item, scope), scope)
    val view = LithoView(context)
    worker.schedule(Unit, Keys(KEY))
    item.bind(view)
    val originalTree = checkNotNull(view.componentTree)
    state.get().updateSync(1)
    shadowOf(Looper.getMainLooper()).idle()
    assertThat(renderedValue.get()).isEqualTo(1)
    val rendersAfterStateUpdate = renders.get()
    item.unbind()

    constraints = SizeConstraints.exact(200, 100)
    item.bind(view)

    assertThat(renders.get()).isEqualTo(rendersAfterStateUpdate)
    assertThat(
        originalTree.hasCompatibleLayout(constraints.toWidthSpec(), constraints.toHeightSpec()),
    )
        .isTrue()
  }

  private fun providerOf(item: PreparableCollectionItem, scope: PrepareScope) =
      object : CollectionItemProvider<String>(scope = scope) {
        override fun create(key: String): PreparableCollectionItem = item

        override fun contains(key: String): Boolean = true
      }

  private class Snapshot(val id: Int, val width: Int)

  /** Counts its renders and records the state value each one read. */
  private class StatefulRow(
      private val renders: AtomicInteger,
      private val state: AtomicReference<State<Int>>,
      private val renderedValue: AtomicInteger,
  ) : KComponent() {
    override fun ComponentScope.render(): Component {
      renders.incrementAndGet()
      val counter = useState { 0 }
      state.set(counter)
      renderedValue.set(counter.value)
      return Row()
    }
  }

  /** Records the data of its latest render. */
  private class DataRow(private val data: Int, private val renderedData: AtomicInteger) :
      KComponent() {
    override fun ComponentScope.render(): Component {
      renderedData.set(data)
      return Row()
    }
  }

  private class Keys(override val keys: List<String>) : KeyBasedRange<String> {
    constructor(vararg keys: String) : this(keys.toList())
  }

  private companion object {
    const val KEY = "row"
  }
}
