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
import com.facebook.litho.KComponent
import com.facebook.litho.LithoView
import com.facebook.litho.LithoVisibilityEventsController
import com.facebook.litho.LithoVisibilityEventsController.LithoVisibilityState
import com.facebook.litho.LithoVisibilityEventsControllerDelegate
import com.facebook.litho.LithoVisibilityEventsListener
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.onCleanup
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.useEffect
import com.facebook.rendercore.SizeConstraints
import com.meta.rangemanager.CollectionItemProvider
import com.meta.rangemanager.KeyBasedRange
import com.meta.rangemanager.KeyBasedRangeWorker
import com.meta.rangemanager.PreparableCollectionItem
import com.meta.rangemanager.PrepareScope
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/**
 * A [LithoCollectionItem] with a [LithoCollectionItem.visibilityEventsController] gives each tree
 * it builds a controller of its own, and destroys it when it releases that tree. A listener on the
 * tree's controller stands in for what is nested in the tree, such as a collection's item trees.
 */
@RunWith(LithoTestRunner::class)
class LithoCollectionItemVisibilityControllerTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val window = FrameLayout(context)
  private val view = LithoView(context)
  private val parent = LithoVisibilityEventsControllerDelegate()
  private val nestedReceived = mutableListOf<LithoVisibilityState>()
  private val nested = LithoVisibilityEventsListener { state -> nestedReceived.add(state) }

  @Before
  fun setUp() {
    Robolectric.buildActivity(Activity::class.java).setup().get().setContentView(window)
    window.addView(view)
  }

  @Test
  fun `each tree gets its own controller that forwards the parent's events`() {
    val row = RowUnderTest(keepTreeWhileShown = false)
    row.worker.schedule(Unit, Keys(KEY))
    row.item.bind(view)
    val controller = treeControllerOf(view)
    controller.addListener(nested)

    parent.moveToVisibilityState(LithoVisibilityState.HINT_INVISIBLE)

    assertThat(controller).isNotSameAs(parent)
    assertThat(nestedReceived).containsExactly(LithoVisibilityState.HINT_INVISIBLE)
  }

  @Test
  fun `releasing a tree destroys its controller`() {
    val row = RowUnderTest(keepTreeWhileShown = false)
    row.worker.schedule(Unit, Keys(KEY))
    row.item.bind(view)
    val tree = checkNotNull(view.componentTree)
    val controller = treeControllerOf(view)
    controller.addListener(nested)
    row.item.unbind()

    row.worker.schedule(Unit, Keys())

    assertThat(tree.isReleased).isTrue()
    assertThat(controller.visibilityState).isEqualTo(LithoVisibilityState.DESTROYED)
    assertThat(nestedReceived).contains(LithoVisibilityState.DESTROYED)
  }

  /** Destroying it under a shown tree would release what the user is looking at. */
  @Test
  fun `a tree kept while shown keeps its controller until unbind`() {
    val row = RowUnderTest(keepTreeWhileShown = true)
    row.worker.schedule(Unit, Keys(KEY))
    row.item.bind(view)
    val controller = treeControllerOf(view)
    controller.addListener(nested)

    val releasing = Thread { row.worker.schedule(Unit, Keys()) }
    releasing.start()
    releasing.join()
    shadowOf(Looper.getMainLooper()).idle()

    assertThat(nestedReceived).doesNotContain(LithoVisibilityState.DESTROYED)

    row.item.unbind()

    assertThat(nestedReceived).contains(LithoVisibilityState.DESTROYED)
  }

  @Test
  fun `without a visibilityEventsController, the tree keeps the one treeConfigurator sets`() {
    val scope = PrepareScope()
    val item = LithoCollectionItem(
        context = context,
        dataProvider = { 1 },
        component = { _ -> Row() },
        sizeConstraints = SizeConstraints.exact(100, 100),
        traceTag = "test",
        treeConfigurator = { builder -> builder.withLithoVisibilityEventsController(parent) },
    )
    KeyBasedRangeWorker(providerOf(item, scope), scope).schedule(Unit, Keys(KEY))

    item.bind(view)

    assertThat(checkNotNull(view.componentTree).lithoVisibilityEventsController).isSameAs(parent)
  }

  /** A restored state would bring back nested collections bound to a destroyed controller. */
  @Test
  fun `a visibilityEventsController requires keepStateAfterRelease off`() {
    assertThatThrownBy {
          LithoCollectionItem(
              context = context,
              dataProvider = { 1 },
              component = { _ -> Row() },
              sizeConstraints = SizeConstraints.exact(100, 100),
              traceTag = "test",
              visibilityEventsController = parent,
          )
        }
        .isInstanceOf(IllegalArgumentException::class.java)
  }

  /** With the configurator's controller instead, nested items would outlive every release. */
  @Test
  fun `the item's controller wins over one treeConfigurator sets`() {
    val recordingParent = RecordingController()
    val other = LithoVisibilityEventsControllerDelegate()
    val item =
        itemWith(recordingParent) { builder -> builder.withLithoVisibilityEventsController(other) }

    item.bind(view)

    assertThat(checkNotNull(view.componentTree).lithoVisibilityEventsController)
        .isNotSameAs(other)
        .isInstanceOf(TreeVisibilityEventsController::class.java)
    assertThat(recordingParent.listeners).hasSize(1)
  }

  @Test
  fun `a build that throws leaves nothing on the parent`() {
    val recordingParent = RecordingController()
    val item = itemWith(recordingParent) { throw IllegalStateException("configurator failed") }

    assertThatThrownBy { item.bind(view) }.isInstanceOf(IllegalStateException::class.java)

    assertThat(recordingParent.listeners).isEmpty()
  }

  /** The item destroys the controller it created, not whatever the tree still points at. */
  @Test
  fun `a tree released before the item releases it still gets its controller destroyed`() {
    val row = RowUnderTest(keepTreeWhileShown = false)
    row.worker.schedule(Unit, Keys(KEY))
    row.item.bind(view)
    val tree = checkNotNull(view.componentTree)
    treeControllerOf(view).addListener(nested)
    row.item.unbind()
    tree.release()

    row.worker.schedule(Unit, Keys())

    assertThat(nestedReceived).contains(LithoVisibilityState.DESTROYED)
  }

  /** An effect cleanup that throws comes out of `ComponentTree.release()`. */
  @Test
  fun `a tree whose release throws still gets its controller destroyed`() {
    val scope = PrepareScope()
    val item = LithoCollectionItem(
        context = context,
        dataProvider = { 1 },
        component = { _ -> ThrowsOnCleanup() },
        sizeConstraints = SizeConstraints.exact(100, 100),
        traceTag = "test",
        visibilityEventsController = parent,
        keepStateAfterRelease = false,
    )
    val worker = KeyBasedRangeWorker(providerOf(item, scope), scope)
    worker.schedule(Unit, Keys(KEY))
    item.bind(view)
    val spec = MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY)
    view.measure(spec, spec)
    view.layout(0, 0, 100, 100)
    shadowOf(Looper.getMainLooper()).idle()
    treeControllerOf(view).addListener(nested)
    item.unbind()

    runCatching {
      worker.schedule(Unit, Keys())
      shadowOf(Looper.getMainLooper()).idle()
    }

    check(cleanupThrew) { "the release should have run the throwing cleanup" }
    assertThat(nestedReceived).contains(LithoVisibilityState.DESTROYED)
  }

  private var cleanupThrew = false

  private inner class ThrowsOnCleanup : KComponent() {
    override fun ComponentScope.render(): Component? {
      useEffect(Unit) {
        onCleanup {
          cleanupThrew = true
          throw IllegalStateException("cleanup failed")
        }
      }
      return null
    }
  }

  private fun itemWith(
      parent: LithoVisibilityEventsController,
      treeConfigurator: (ComponentTree.Builder) -> Unit,
  ): LithoCollectionItem<Int> = LithoCollectionItem(
      context = context,
      dataProvider = { 1 },
      component = { _ -> Row() },
      sizeConstraints = SizeConstraints.exact(100, 100),
      traceTag = "test",
      treeConfigurator = treeConfigurator,
      visibilityEventsController = parent,
      keepStateAfterRelease = false,
  )

  private class RecordingController : LithoVisibilityEventsController {
    val listeners = mutableSetOf<LithoVisibilityEventsListener>()
    override val visibilityState: LithoVisibilityState = LithoVisibilityState.HINT_VISIBLE

    override fun moveToVisibilityState(newVisibilityState: LithoVisibilityState): Unit = Unit

    override fun addListener(listener: LithoVisibilityEventsListener) {
      listeners.add(listener)
    }

    override fun removeListener(listener: LithoVisibilityEventsListener) {
      listeners.remove(listener)
    }
  }

  private fun treeControllerOf(view: LithoView): LithoVisibilityEventsController =
      checkNotNull(checkNotNull(view.componentTree).lithoVisibilityEventsController)

  private inner class RowUnderTest(keepTreeWhileShown: Boolean) {
    private val scope = PrepareScope()
    val item = LithoCollectionItem(
        context = context,
        dataProvider = { 1 },
        component = { _ -> Row() },
        sizeConstraints = SizeConstraints.exact(100, 100),
        traceTag = "test",
        keepTreeWhileShown = keepTreeWhileShown,
        visibilityEventsController = parent,
        keepStateAfterRelease = false,
    )
    val worker = KeyBasedRangeWorker(providerOf(item, scope), scope)
  }

  private fun providerOf(item: PreparableCollectionItem, scope: PrepareScope) =
      object : CollectionItemProvider<String>(scope = scope) {
        override fun create(key: String): PreparableCollectionItem = item

        override fun contains(key: String): Boolean = true
      }

  private class Keys(override val keys: List<String>) : KeyBasedRange<String> {
    constructor(vararg keys: String) : this(keys.toList())
  }

  private companion object {
    const val KEY = "row"
  }
}
