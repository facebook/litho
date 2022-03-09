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
class TestListComponent {

  private var recyclerBinder: RecyclerBinder

  constructor(recycler: Recycler) {
    val binder =
        Whitebox.getInternalState<Binder<RecyclerView>>(recycler, "binder") as SectionBinderTarget

    this.recyclerBinder = Whitebox.getInternalState(binder, "mRecyclerBinder")
  }

  /**
   * Get the number of items in the recycler binder
   *
   * This is not the same as the number of items that are displayed.
   */
  val itemCount: Int
    get() = recyclerBinder.itemCount

  /** All Components managed by the lazy collection */
  val components: List<Component>
    get() = (0 until itemCount).map { recyclerBinder.getRenderInfoAt(it).component }

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
  val visibleComponents: List<Component>
    get() =
        if (recyclerBinder.findFirstVisibleItemPosition() != RecyclerView.NO_POSITION) {
          components.slice(
              recyclerBinder.findFirstVisibleItemPosition()..recyclerBinder
                      .findLastVisibleItemPosition())
        } else {
          listOf()
        }

  /** Get all fully visible items */
  val fullyVisibleComponents: List<Component>
    get() =
        if (recyclerBinder.findFirstVisibleItemPosition() != RecyclerView.NO_POSITION) {
          components.slice(
              recyclerBinder.findFirstFullyVisibleItemPosition()..recyclerBinder
                      .findLastFullyVisibleItemPosition())
        } else {
          listOf()
        }

  /** Convenience function for getting the component at an index */
  inline fun getItemAtIndex(index: Int): Component = components[index]

  /**
   * Performs a shallow search to Find the first component, of the given type; in the
   * LazyCollection.
   */
  inline fun findFirstComponent(clazz: Class<out Component>): Component? =
      components.find { it::class.java == clazz }

  /** @see findComponent */
  inline fun findFirstComponent(clazz: KClass<out Component>): Component? =
      findFirstComponent(clazz.java)
}
