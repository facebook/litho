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

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.facebook.litho.BaseMountingView
import com.facebook.litho.Component
import com.facebook.litho.LithoView
import com.facebook.litho.sections.widget.SectionBinderTarget
import com.facebook.litho.widget.LithoRecyclerView
import com.facebook.litho.widget.Recycler
import com.facebook.litho.widget.RecyclerBinder
import com.facebook.litho.widget.SectionsRecyclerView
import com.facebook.litho.widget.collection.CollectionPrimitiveViewAdapter
import com.facebook.litho.widget.collection.CollectionRecyclerComponent
import kotlin.reflect.KClass

/**
 * A wrapper class that exposes methods specific to testing collection components.
 *
 * @property recyclerBinder The [RecyclerBinder] from the list being tested (legacy approach).
 * @property adapter The [CollectionPrimitiveViewAdapter] from the list being tested (new approach).
 */
class TestCollection {

  private var recyclerBinder: RecyclerBinder? = null
  private var adapter: CollectionPrimitiveViewAdapter? = null
  private var cachedRecyclerView: RecyclerView? = null
  private var lithoView: LithoView? = null

  constructor(recycler: Recycler) {
    val binder = recycler.binder as SectionBinderTarget
    this.recyclerBinder = binder.recyclerBinder
  }

  constructor(
      collectionRecyclerComponent: CollectionRecyclerComponent,
      lithoView: LithoView? = null,
  ) {
    this.recyclerBinder = null
    this.lithoView = lithoView
    // adapter is lazily retrieved from the mounted RecyclerView since
    // CollectionRecyclerComponent creates it inside render() via useState
  }

  private val SectionBinderTarget.recyclerBinder: RecyclerBinder
    get() = Whitebox.getInternalState(this, "mRecyclerBinder")

  /** Lazily resolve the adapter from the mounted RecyclerView for the new collection infra. */
  private fun resolveAdapter(): CollectionPrimitiveViewAdapter? {
    if (adapter != null) return adapter
    if (recyclerBinder != null) return null
    adapter =
        try {
          recyclerView.adapter as? CollectionPrimitiveViewAdapter
        } catch (e: Exception) {
          // The adapter may not be a CollectionPrimitiveViewAdapter (e.g., legacy infra),
          // so we catch and return null to fall back to other resolution paths.
          null
        }
    return adapter
  }

  /**
   * Get the number of items in the lazy collection. This is not the same as the number of items
   * that are displayed.
   */
  val itemCount: Int
    get() = recyclerBinder?.getItemCount() ?: resolveAdapter()?.itemCount ?: 0

  /** All items in the lazy collection */
  val items: List<TestCollectionItem>
    get() {
      val binder = recyclerBinder
      return if (binder != null) {
        // Legacy approach: use ComponentTreeHolder from RecyclerBinder
        (0 until itemCount).map {
          TestCollectionItem(this, binder.getComponentTreeHolderAt(it), it)
        }
      } else {
        // New collection infrastructure: get items from adapter
        // The adapter stores CollectionItem instances (e.g., LithoCollectionItem)
        // We create TestCollectionItem with null ComponentTreeHolder since the new
        // infrastructure doesn't use ComponentTreeHolder
        resolveAdapter()?.getItems()?.mapIndexed { index, item ->
          TestCollectionItem(this, null, index, item)
        } ?: emptyList()
      }
    }

  private enum class PositionType {
    FIRST_VISIBLE,
    FIRST_FULLY_VISIBLE,
    LAST_VISIBLE,
    LAST_FULLY_VISIBLE,
  }

  private fun findItemPosition(type: PositionType): Int {
    recyclerBinder?.let {
      return when (type) {
        PositionType.FIRST_VISIBLE -> it.findFirstVisibleItemPosition()
        PositionType.FIRST_FULLY_VISIBLE -> it.findFirstFullyVisibleItemPosition()
        PositionType.LAST_VISIBLE -> it.findLastVisibleItemPosition()
        PositionType.LAST_FULLY_VISIBLE -> it.findLastFullyVisibleItemPosition()
      }
    }

    return try {
      val lm = recyclerView.layoutManager
      when (lm) {
        is LinearLayoutManager -> {
          when (type) {
            PositionType.FIRST_VISIBLE -> lm.findFirstVisibleItemPosition()
            PositionType.FIRST_FULLY_VISIBLE -> lm.findFirstCompletelyVisibleItemPosition()
            PositionType.LAST_VISIBLE -> lm.findLastVisibleItemPosition()
            PositionType.LAST_FULLY_VISIBLE -> lm.findLastCompletelyVisibleItemPosition()
          }
        }
        is StaggeredGridLayoutManager -> {
          val positions = IntArray(lm.spanCount)
          when (type) {
            PositionType.FIRST_VISIBLE -> {
              lm.findFirstVisibleItemPositions(positions)
              positions.minOrNull() ?: RecyclerView.NO_POSITION
            }
            PositionType.FIRST_FULLY_VISIBLE -> {
              lm.findFirstCompletelyVisibleItemPositions(positions)
              positions.minOrNull() ?: RecyclerView.NO_POSITION
            }
            PositionType.LAST_VISIBLE -> {
              lm.findLastVisibleItemPositions(positions)
              positions.maxOrNull() ?: RecyclerView.NO_POSITION
            }
            PositionType.LAST_FULLY_VISIBLE -> {
              lm.findLastCompletelyVisibleItemPositions(positions)
              positions.maxOrNull() ?: RecyclerView.NO_POSITION
            }
          }
        }
        else -> RecyclerView.NO_POSITION
      }
    } catch (e: Exception) {
      RecyclerView.NO_POSITION
    }
  }

  /** Get the first visible item index, returns -1 when there are no components */
  val firstVisibleIndex: Int
    get() = findItemPosition(PositionType.FIRST_VISIBLE)

  /** Get the first fully visible item index, returns -1 when there are no components */
  val firstFullyVisibleIndex: Int
    get() = findItemPosition(PositionType.FIRST_FULLY_VISIBLE)

  /** Get the last visible item index, returns -1 when there are no components */
  val lastVisibleIndex: Int
    get() = findItemPosition(PositionType.LAST_VISIBLE)

  /** Get the last Fully visible item index, returns -1 when there are no components */
  val lastFullyVisibleIndex: Int
    get() = findItemPosition(PositionType.LAST_FULLY_VISIBLE)

  /** Get all visible items */
  val visibleItems: List<TestCollectionItem>
    get() =
        if (
            firstVisibleIndex != RecyclerView.NO_POSITION &&
                lastVisibleIndex != RecyclerView.NO_POSITION
        ) {
          items.slice(firstVisibleIndex..lastVisibleIndex)
        } else {
          listOf()
        }

  /** Get all fully visible items */
  val fullyVisibleItems: List<TestCollectionItem>
    get() =
        if (
            firstFullyVisibleIndex != RecyclerView.NO_POSITION &&
                lastFullyVisibleIndex != RecyclerView.NO_POSITION
        ) {
          items.slice(firstFullyVisibleIndex..lastFullyVisibleIndex)
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
  fun getItemAtIndex(index: Int): TestCollectionItem = items[index]

  /**
   * Performs a shallow search to Find the first item, with the given component type; in the
   * LazyCollection.
   */
  fun findFirstItem(clazz: Class<out Component>): TestCollectionItem? = items.find {
    it.component?.let { c -> c::class.java == clazz } ?: false
  }

  /** @see findFirstItem */
  fun findFirstItem(clazz: KClass<out Component>): TestCollectionItem? = findFirstItem(clazz.java)

  /**
   * Performs a shallow search for items, with the given component type; from the LazyCollection.
   */
  internal fun findItems(classes: List<Class<out Component>>): List<TestCollectionItem> =
      items.filter {
        it.component?.let { c -> classes.contains(c::class.java) } ?: false
      }

  /** @see findItems */
  fun findItems(vararg clazz: Class<out Component>): List<TestCollectionItem> =
      findItems(clazz.toList())

  /** @see findItems */
  fun findItems(vararg clazz: KClass<out Component>): List<TestCollectionItem> =
      findItems(clazz.map { it.java })

  /** Retrieve the visible mounted children. */
  fun getLithoViews(): List<BaseMountingView> {
    return (firstVisibleIndex..lastVisibleIndex).mapNotNull {
      recyclerView.layoutManager?.findViewByPosition(it) as? BaseMountingView
    }
  }

  val measuredHeight: Int
    get() =
        recyclerBinder?.measuredHeight
            ?: (recyclerView.measuredHeight - recyclerView.paddingTop - recyclerView.paddingBottom)

  val measuredWidth: Int
    get() =
        recyclerBinder?.measuredWidth
            ?: (recyclerView.measuredWidth - recyclerView.paddingLeft - recyclerView.paddingRight)

  internal val recyclerView: RecyclerView
    get() {
      cachedRecyclerView?.let {
        return it
      }

      val rv: RecyclerView? =
          if (recyclerBinder != null) {
            Whitebox.getInternalState(recyclerBinder, "mountedView")
          } else {
            // For new collection infrastructure, find the RecyclerView from the view hierarchy
            lithoView?.let { lv -> findRecyclerViewInHierarchy(lv) }
          }

      cachedRecyclerView =
          requireNotNull(rv) {
            """
            We could not find a mounted recycler view.  This normally happens if you did not provide an
            explicit size when rendering your component for test.  To fix this, add a width and height
            when rendering the component: `lithoViewRule.render(widthPx = 100, heightPx = 100) { ... }`.
            """
                .trimIndent()
          }

      return checkNotNull(cachedRecyclerView)
    }

  private fun findRecyclerViewInHierarchy(view: android.view.View): RecyclerView? {
    // Search for SectionsRecyclerView in the view hierarchy
    if (view is SectionsRecyclerView) {
      return view.recyclerView as? LithoRecyclerView
    }
    if (view is android.view.ViewGroup) {
      for (i in 0 until view.childCount) {
        val child = view.getChildAt(i)
        val found = findRecyclerViewInHierarchy(child)
        if (found != null) {
          return found
        }
      }
    }
    return null
  }
}
