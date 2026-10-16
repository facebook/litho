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

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.view.View.MeasureSpec
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.ComponentTree
import com.facebook.litho.ComponentTreeRegistry
import com.facebook.litho.KComponent
import com.facebook.litho.LithoView
import com.facebook.litho.LithoVisibilityEventsController
import com.facebook.litho.LithoVisibilityEventsController.LithoVisibilityState
import com.facebook.litho.LithoVisibilityEventsControllerDelegate
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.collection.LazyList
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.px
import com.meta.rangemanager.CollectionItemProvider
import com.meta.rangemanager.KeyBasedRange
import com.meta.rangemanager.KeyBasedRangeWorker
import com.meta.rangemanager.PreparableCollectionItem
import com.meta.rangemanager.PrepareScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/**
 * A row holding a legacy-infra collection, whose item trees live until the controller they inherit
 * from the row is destroyed. Counts the trees still alive with [ComponentTreeRegistry].
 *
 * Releasing them takes both item options: a `visibilityEventsController`, so each row tree has a
 * controller the item destroys, and `keepStateAfterRelease = false`, so a row shown again builds a
 * new nested collection instead of restoring the old one, which is bound to the destroyed
 * controller. Each test passes both explicitly.
 */
@RunWith(LithoTestRunner::class)
class LithoCollectionItemNestedCollectionTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val window = FrameLayout(context)
  private val view = LithoView(context)
  private val parent = LithoVisibilityEventsControllerDelegate()
  private val treesBefore = mutableSetOf<Int>()

  @Before
  fun setUp() {
    ComponentTreeRegistry.enabled = true
    Robolectric.buildActivity(Activity::class.java).setup().get().setContentView(window)
    window.addView(view)
    idle()
    treesBefore += ComponentTreeRegistry.snapshot().map { it.id }
  }

  @After
  fun tearDown() {
    ComponentTreeRegistry.enabled = false
  }

  /** Why the item owns controllers at all: without them the nested items outlive the row. */
  @Test
  fun `with the parent controller set on the tree, the nested items outlive the row`() {
    val row = RowUnderTest(
        treeConfigurator = { builder -> builder.withLithoVisibilityEventsController(parent) },
    )
    row.showThenRelease()

    assertThat(liveTrees()).isEqualTo(NESTED_ITEMS)
  }

  @Test
  fun `with a visibilityEventsController, releasing the row releases its nested items`() {
    val row = RowUnderTest(visibilityEventsController = parent, keepStateAfterRelease = false)
    row.showThenRelease()

    assertThat(liveTrees()).isZero()
  }

  /**
   * The case that needs state dropped: a restored state would bring back the first tree's binder.
   */
  @Test
  fun `with keepStateAfterRelease off, a row shown again after a release releases its new nested items too`() {
    val row = RowUnderTest(visibilityEventsController = parent, keepStateAfterRelease = false)
    row.showThenRelease()

    row.showThenRelease()

    assertThat(liveTrees()).isZero()
  }

  /** The item's controller wins over the configurator's, so its nested items still go each time. */
  @Test
  fun `with a treeConfigurator controller too, every release releases its nested items`() {
    val row = RowUnderTest(
        visibilityEventsController = parent,
        keepStateAfterRelease = false,
        treeConfigurator = { builder -> builder.withLithoVisibilityEventsController(parent) },
    )

    val liveAfterEachRelease =
        (1..4).map {
          row.showThenRelease()
          liveTrees()
        }

    assertThat(liveAfterEachRelease).containsExactly(0, 0, 0, 0)
  }

  @Test
  fun `a row whose parent controller was destroyed first still releases its nested items`() {
    val row = RowUnderTest(visibilityEventsController = parent, keepStateAfterRelease = false)
    row.show()
    parent.moveToVisibilityState(LithoVisibilityState.DESTROYED)
    idle()

    row.hideThenRelease()

    assertThat(liveTrees()).isZero()
  }

  private fun idle() {
    shadowOf(Looper.getMainLooper()).idle()
  }

  private fun liveTrees(): Int {
    idle()
    return ComponentTreeRegistry.snapshot().count { it.id !in treesBefore && !it.isReleased }
  }

  private class NestedCollection : KComponent() {
    override fun ComponentScope.render(): Component =
        LazyList(style = Style.width(SIZE.px).height(SIZE.px), useLegacyInfra = true) {
          for (i in 0 until NESTED_ITEMS) {
            child(id = i, component = Row(style = Style.width(SIZE.px).height(20.px)))
          }
        }
  }

  /** A row whose item gets these [LithoCollectionItem] options; the rest are fixed. */
  private inner class RowUnderTest(
      visibilityEventsController: LithoVisibilityEventsController? = null,
      keepStateAfterRelease: Boolean = true,
      treeConfigurator: ((ComponentTree.Builder) -> Unit)? = null,
  ) {
    private val scope = PrepareScope()
    private val item = LithoCollectionItem(
        context = context,
        dataProvider = { 1 },
        component = { _ -> NestedCollection() },
        sizeConstraints = SizeConstraints.exact(SIZE, SIZE),
        traceTag = "test",
        treeConfigurator = treeConfigurator,
        visibilityEventsController = visibilityEventsController,
        keepStateAfterRelease = keepStateAfterRelease,
    )
    private val worker = KeyBasedRangeWorker(
        object : CollectionItemProvider<String>(scope = scope) {
          override fun create(key: String): PreparableCollectionItem = item

          override fun contains(key: String): Boolean = true
        },
        scope,
    )

    fun show() {
      worker.schedule(Unit, Keys(KEY))
      item.bind(view)
      val spec = MeasureSpec.makeMeasureSpec(SIZE, MeasureSpec.EXACTLY)
      view.measure(spec, spec)
      view.layout(0, 0, SIZE, SIZE)
      idle()
      check(liveTrees() == NESTED_ITEMS + 1) {
        "the row and its nested items should be alive while shown"
      }
    }

    fun hideThenRelease() {
      item.unbind()
      worker.schedule(Unit, Keys())
      idle()
    }

    fun showThenRelease() {
      show()
      hideThenRelease()
    }
  }

  private class Keys(override val keys: List<String>) : KeyBasedRange<String> {
    constructor(vararg keys: String) : this(keys.toList())
  }

  private companion object {
    const val KEY = "row"
    const val NESTED_ITEMS = 10
    const val SIZE = 300
  }
}
