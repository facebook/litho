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
import com.facebook.litho.KComponent
import com.facebook.litho.LithoView
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.useState
import com.facebook.rendercore.SizeConstraints
import com.meta.rangemanager.CollectionItemProvider
import com.meta.rangemanager.KeyBasedRange
import com.meta.rangemanager.KeyBasedRangeWorker
import com.meta.rangemanager.PreparableCollectionItem
import com.meta.rangemanager.PrepareScope
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/**
 * Whether the tree a [LithoCollectionItem] builds after a release gets the released tree's state.
 */
@RunWith(LithoTestRunner::class)
class LithoCollectionItemStateAfterReleaseTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val stateInits = AtomicInteger(0)

  @Test
  fun `a tree built after a release restores the released tree's state`() {
    bindReleaseAndBindAgain(keepStateAfterRelease = true)

    assertThat(stateInits.get()).isEqualTo(1)
  }

  @Test
  fun `without keepStateAfterRelease, a tree built after a release starts from fresh state`() {
    bindReleaseAndBindAgain(keepStateAfterRelease = false)

    assertThat(stateInits.get()).isEqualTo(2)
  }

  /** The provider builds a new item for the row once the last one is released. */
  @Test
  fun `a new item for the row restores the state the last item saved`() {
    assertThat(showItemsInTurn(true, true)).containsExactly(1, 1)
  }

  @Test
  fun `a new item without keepStateAfterRelease ignores the state the last item saved`() {
    assertThat(showItemsInTurn(true, false)).containsExactly(1, 2)
  }

  /** A row whose retention changes between items must not get state from before the change. */
  @Test
  fun `an item without keepStateAfterRelease clears the state saved before it`() {
    assertThat(showItemsInTurn(true, false, true)).containsExactly(1, 2, 3)
  }

  /**
   * A second release of an item, as a task left over from an earlier range can make, comes after a
   * newer item for the row saved its state and must leave that state alone.
   */
  @Test
  fun `a repeat release of an item without keepStateAfterRelease leaves a newer item's state`() {
    val scope = PrepareScope()
    val first = itemWith(keepStateAfterRelease = false)
    val later =
        listOf(itemWith(keepStateAfterRelease = true), itemWith(keepStateAfterRelease = true))
    val items = (listOf(first) + later).iterator()
    val provider = providerOf(scope) { items.next() }
    val worker = KeyBasedRangeWorker(provider, scope)
    // A second owner of the first item, still holding it after the row moves on.
    val staleWorker = KeyBasedRangeWorker(providerOf(scope) { first }, scope)
    val view = attachedView()

    worker.schedule(Unit, Keys(KEY))
    staleWorker.schedule(Unit, Keys(KEY))
    val inits = mutableListOf(show(provider, view))
    worker.schedule(Unit, Keys())
    worker.schedule(Unit, Keys(KEY))
    inits += show(provider, view)
    worker.schedule(Unit, Keys())
    staleWorker.schedule(Unit, Keys())
    worker.schedule(Unit, Keys(KEY))
    inits += show(provider, view)

    assertThat(inits).containsExactly(1, 2, 2)
  }

  /**
   * Shows and releases one new item per entry, each with that [keepStateAfterRelease], all for the
   * same row. Returns the state initializations counted after each item was shown.
   */
  private fun showItemsInTurn(vararg keepStateAfterRelease: Boolean): List<Int> {
    val scope = PrepareScope()
    val items = keepStateAfterRelease.map(::itemWith).iterator()
    val provider = providerOf(scope) { items.next() }
    val worker = KeyBasedRangeWorker(provider, scope)
    val view = attachedView()
    return keepStateAfterRelease.map {
      worker.schedule(Unit, Keys(KEY))
      val inits = show(provider, view)
      worker.schedule(Unit, Keys())
      shadowOf(Looper.getMainLooper()).idle()
      inits
    }
  }

  private fun itemWith(
      keepStateAfterRelease: Boolean,
  ): LithoCollectionItem<Int> = LithoCollectionItem(
      context = context,
      dataProvider = { 1 },
      component = { _ -> WithState() },
      sizeConstraints = SizeConstraints.exact(SIZE, SIZE),
      traceTag = "test",
      keepStateAfterRelease = keepStateAfterRelease,
  )

  private fun providerOf(scope: PrepareScope, create: () -> PreparableCollectionItem) =
      object : CollectionItemProvider<String>(scope = scope) {
        override fun create(key: String): PreparableCollectionItem = create()

        override fun contains(key: String): Boolean = true
      }

  private fun attachedView(): LithoView {
    val window = FrameLayout(context)
    Robolectric.buildActivity(Activity::class.java).setup().get().setContentView(window)
    return LithoView(context).also(window::addView)
  }

  /**
   * Binds the row's current item, lays it out, unbinds it; returns state initializations so far.
   */
  private fun show(provider: CollectionItemProvider<String>, view: LithoView): Int {
    val item = provider[KEY] as LithoCollectionItem<*>
    item.bind(view)
    val spec = MeasureSpec.makeMeasureSpec(SIZE, MeasureSpec.EXACTLY)
    view.measure(spec, spec)
    view.layout(0, 0, SIZE, SIZE)
    shadowOf(Looper.getMainLooper()).idle()
    item.unbind()
    return stateInits.get()
  }

  /** Binds builds and resolves the tree on the spot, so its state exists before the release. */
  private fun bindReleaseAndBindAgain(keepStateAfterRelease: Boolean) {
    val scope = PrepareScope()
    val item = LithoCollectionItem(
        context = context,
        dataProvider = { 1 },
        component = { _ -> WithState() },
        sizeConstraints = SizeConstraints.exact(100, 100),
        traceTag = "test",
        keepStateAfterRelease = keepStateAfterRelease,
    )
    val worker = KeyBasedRangeWorker(
        object : CollectionItemProvider<String>(scope = scope) {
          override fun create(key: String): PreparableCollectionItem = item

          override fun contains(key: String): Boolean = true
        },
        scope,
    )
    val view = LithoView(context)
    item.bind(view)
    worker.schedule(Unit, Keys(KEY))
    item.unbind()
    worker.schedule(Unit, Keys())
    check(stateInits.get() == 1)

    item.bind(view)
  }

  private inner class WithState : KComponent() {
    override fun ComponentScope.render(): Component? {
      useState { stateInits.incrementAndGet() }
      return null
    }
  }

  private class Keys(override val keys: List<String>) : KeyBasedRange<String> {
    constructor(vararg keys: String) : this(keys.toList())
  }

  private companion object {
    const val KEY = "row"
    const val SIZE = 100
  }
}
