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

package com.facebook.litho.testing

import androidx.recyclerview.widget.RecyclerView
import com.facebook.litho.Component
import com.facebook.litho.LithoView
import com.facebook.litho.sections.widget.SectionBinderTarget
import com.facebook.litho.widget.Binder
import com.facebook.litho.widget.Recycler
import com.facebook.litho.widget.RecyclerBinder
import kotlin.reflect.KClass

/**
 * A wrapper class that exposes methods specific to testing collection components.
 *
 * @property recyclerBinder The [RecyclerBinder] from the list being tested.
 */
class TestCollection {

  private var recyclerBinder: RecyclerBinder

  constructor(recycler: Recycler) {
    val binder =
        Whitebox.getInternalState<Binder<RecyclerView>>(recycler, "binder") as SectionBinderTarget

    this.recyclerBinder = Whitebox.getInternalState(binder, "mRecyclerBinder")
  }

  /**
   * Get the number of items in the lazy collection. This is not the same as the number of items
   * that are displayed.
   */
  val itemCount: Int
    get() = recyclerBinder.itemCount

  /** All items in the lazy collection */
  val items: List<TestCollectionItem>
    get() =
        (0 until itemCount).map {
          TestCollectionItem(this, recyclerBinder.getComponentTreeHolderAt(it), it)
        }

  /** Get the first visible item index, returns -1 when there are no components */
  val firstVisibleIndex: Int
    get() = recyclerBinder.findFirstVisibleItemPosition()

  /** Get the first fully visible item index, returns -1 when there are no components */
  val firstFullyVisibleIndex: Int
    get() = recyclerBinder.findFirstFullyVisibleItemPosition()

  /** Get the last visible item index, returns -1 when there are no components */
  val lastVisibleIndex: Int
    get() = recyclerBinder.findLastVisibleItemPosition()

  /** Get the last Fully visible item index, returns -1 when there are no components */
  val lastFullyVisibleIndex: Int
    get() = recyclerBinder.findLastFullyVisibleItemPosition()

  /** Get all visible items */
  val visibleItems: List<TestCollectionItem>
    get() =
        if (recyclerBinder.findFirstVisibleItemPosition() != RecyclerView.NO_POSITION) {
          items.slice(
              recyclerBinder.findFirstVisibleItemPosition()..recyclerBinder
                      .findLastVisibleItemPosition())
        } else {
          listOf()
        }

  /** Get all fully visible items */
  val fullyVisibleItems: List<TestCollectionItem>
    get() =
        if (recyclerBinder.findFirstVisibleItemPosition() != RecyclerView.NO_POSITION) {
          items.slice(
              recyclerBinder.findFirstFullyVisibleItemPosition()..recyclerBinder
                      .findLastFullyVisibleItemPosition())
        } else {
          listOf()
        }

  /** Convenience property for getting the first item */
  val firstItem: TestCollectionItem
    get() = items.first()

  /** Convenience property for getting the first visible item */
  val firstVisibleItem: TestCollectionItem
    get() = visibleItems.first()

  /** Convenience property for getting the last item */
  val lastItem: TestCollectionItem
    get() = items.last()

  /** Convenience property for getting the last visible item */
  val lastVisibleItem: TestCollectionItem
    get() = visibleItems.last()

  /** Convenience function for getting the item at an index */
  inline fun getItemAtIndex(index: Int): TestCollectionItem = items[index]

  /**
   * Performs a shallow search to Find the first item, with the given component type; in the
   * LazyCollection.
   */
  inline fun findFirstItem(clazz: Class<out Component>): TestCollectionItem? =
      items.find { it.component::class.java == clazz }

  /** @see findFirstItem */
  inline fun findFirstItem(clazz: KClass<out Component>): TestCollectionItem? =
      findFirstItem(clazz.java)

  /**
   * Performs a shallow search for items, with the given component type; from the LazyCollection.
   */
  @PublishedApi
  internal inline fun findItems(classes: List<Class<out Component>>): List<TestCollectionItem> =
      items.filter { classes.contains(it.component::class.java) }

  /** @see findItems */
  inline fun findItems(vararg clazz: Class<out Component>): List<TestCollectionItem> =
      findItems(clazz.toList())

  /** @see findItems */
  inline fun findItems(vararg clazz: KClass<out Component>): List<TestCollectionItem> =
      findItems(clazz.map { it.java })

  /** Retrieve the visible mounted children. */
  fun getLithoViews(): List<LithoView> {
    return (firstVisibleIndex..lastVisibleIndex).mapNotNull {
      recyclerView.layoutManager?.findViewByPosition(it) as? LithoView
    }
  }

  internal val recyclerView: RecyclerView
    get() {
      val recyclerView: RecyclerView? = Whitebox.getInternalState(recyclerBinder, "mMountedView")

      return requireNotNull(recyclerView) {
        """
        We could not find a mounted recycler view.  This normally happens if you did not provide an 
        explicit size when rendering your component for test.  To fix this, add a width and height 
        when rendering the component: `lithoViewRule.render(widthPx = 100, heightPx = 100) { ... }`.
      """
            .trimIndent()
      }
    }
}
