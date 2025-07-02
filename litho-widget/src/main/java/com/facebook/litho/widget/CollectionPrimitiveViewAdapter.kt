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

package com.facebook.litho.widget

import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.recyclerview.widget.RecyclerView
import com.facebook.rendercore.thread.utils.ThreadUtils

/**
 * RecyclerView adapter that can be used to render a list of [CollectionItem]s. The adapter will
 * create a [PrimitiveRecyclerViewHolder] for each item and will call [CollectionItem.onBindView]
 * and [CollectionItem.onViewRecycled] on the holder when the item is bound and recycle
 * respectively.
 */
class CollectionPrimitiveViewAdapter : RecyclerView.Adapter<PrimitiveRecyclerViewHolder>() {

  private val items: MutableList<CollectionItem<*>> = ArrayList()

  /**
   * Factory function for creating CollectionItemRootHostHolder instances. This function is called
   * by the adapter when a new view holder needs to be created. Must be set before the adapter
   * starts creating view holders.
   *
   * @param parent The ViewGroup into which the new View will be added after it is bound to an
   *   adapter position
   * @param viewType The view type of the new View, used to distinguish different item types
   * @return A new CollectionItemRootHostHolder instance
   */
  var viewHolderCreator:
      ((parent: ViewGroup, viewType: Int) -> CollectionItemRootHostHolder<
              out View, out CollectionItem<out View>>)? =
      null

  /**
   * Optional layout data that provides additional configuration and metadata for the collection's
   * layout behavior. This can be used to store layout-specific information such as span counts,
   * orientation, or other layout parameters that influence how items are arranged in the
   * RecyclerView.
   */
  var layoutData: CollectionLayoutData? = null
    @UiThread
    set(value) {
      ThreadUtils.assertMainThread()
      field = value
    }

  init {
    setHasStableIds(true)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrimitiveRecyclerViewHolder {
    val viewHolderDelegate =
        checkNotNull(viewHolderCreator) {
              "viewHolderCreator must be set before creating the view holders"
            }
            .invoke(parent, viewType)
    @Suppress("UNCHECKED_CAST")
    return PrimitiveRecyclerViewHolder(
        viewHolderDelegate as CollectionItemRootHostHolder<View, CollectionItem<View>>)
  }

  override fun onBindViewHolder(viewHolder: PrimitiveRecyclerViewHolder, position: Int) {
    // TODO: handle OOB exception
    val item = items[position]
    @Suppress("UNCHECKED_CAST")
    viewHolder.delegate.data = item as CollectionItem<View>
    layoutData?.let { scope ->
      item.onBindView(viewHolder.delegate.view, scope.getChildSizeConstraints(item))
    }
  }

  override fun onViewRecycled(viewHolder: PrimitiveRecyclerViewHolder) {
    viewHolder.delegate.data?.onViewRecycled(viewHolder.delegate.view)
    viewHolder.delegate.data = null
  }

  override fun getItemCount(): Int = items.size

  override fun getItemId(position: Int): Long = items[position].id.toLong()

  override fun getItemViewType(position: Int): Int = items[position].viewType

  /** Returns the position of the first item with the given id, otherwise, returns -1. */
  @UiThread
  fun findPositionById(id: Any): Int {
    return items.indexOfFirst { item ->
      item.renderInfo.getCustomAttribute(CollectionItem.ID_CUSTOM_ATTR_KEY) == id
    }
  }

  /**
   * Finds and returns a CollectionItem at the specified position in the adapter's data set.
   *
   * @param position The position of the item to retrieve
   * @return The CollectionItem at the specified position, or null if position is invalid
   */
  @UiThread
  fun findItemByPosition(position: Int): CollectionItem<*>? {
    ThreadUtils.assertMainThread()
    return items.getOrNull(position)
  }

  // region update operations

  fun insertAt(position: Int, holders: List<CollectionItem<*>>) {
    for (index in holders.indices) {
      val holder = holders[index]
      items.add(position + index, holder)
    }
    if (holders.size > 1) {
      notifyItemRangeInserted(position, holders.size)
    } else {
      notifyItemInserted(position)
    }
  }

  fun updateAt(position: Int, holders: List<CollectionItem<*>>) {
    for (index in holders.indices) {
      val holder = holders[index]
      items.removeAt(position + index)
      items.add(position + index, holder)
    }
    if (holders.size > 1) {
      notifyItemRangeChanged(position, holders.size)
    } else {
      notifyItemChanged(position)
    }
  }

  fun deleteAt(position: Int, count: Int) {
    repeat(count) { items.removeAt(position) }
    if (count > 1) {
      notifyItemRangeRemoved(position, count)
    } else {
      notifyItemRemoved(position)
    }
  }

  fun move(from: Int, to: Int) {
    items.add(to, items.removeAt(from))
    notifyItemMoved(from, to)
  }

  fun setItems(newData: List<CollectionItem<*>>) {
    items.clear()
    items.addAll(newData)
  }

  fun getItems(): List<CollectionItem<*>> {
    return items
  }

  // endregion
}
