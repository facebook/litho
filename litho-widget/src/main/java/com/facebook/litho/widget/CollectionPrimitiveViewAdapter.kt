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

import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.recyclerview.widget.RecyclerView
import com.facebook.litho.LithoRenderTreeView

/**
 * RecyclerView adapter that can be used to render a list of [CollectionItem]s. The adapter will
 * create a [PrimitiveRecyclerViewHolder] for each item and will call [CollectionItem.onBindView]
 * and [CollectionItem.onViewRecycled] on the holder when the item is bound and recycle
 * respectively.
 */
class CollectionPrimitiveViewAdapter(
    private val viewHolderCreator:
        (parent: ViewGroup, viewType: Int) -> PrimitiveRecyclerViewHolder,
) : RecyclerView.Adapter<PrimitiveRecyclerViewHolder>() {

  private val items: MutableList<CollectionItem<*>> = ArrayList()

  init {
    setHasStableIds(true)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrimitiveRecyclerViewHolder {
    return viewHolderCreator(parent, viewType)
  }

  override fun onBindViewHolder(viewHolder: PrimitiveRecyclerViewHolder, position: Int) {
    // TODO: handle OOB exception
    val item = items[position]
    when (viewHolder.itemView) {
      is LithoRenderTreeView -> {
        (item as LithoCollectionItem).onBindView(viewHolder.itemView as LithoRenderTreeView)
      }
      else -> {
        throw IllegalStateException("Not supported collection item")
      }
    }
    viewHolder.data = item
  }

  override fun onViewRecycled(viewHolder: PrimitiveRecyclerViewHolder) {
    when (viewHolder.itemView) {
      is LithoRenderTreeView -> {
        (viewHolder.data as LithoCollectionItem).onViewRecycled(
            viewHolder.itemView as LithoRenderTreeView)
      }
      else -> {
        throw IllegalStateException("Not supported collection item")
      }
    }
    viewHolder.data = null
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

  // endregion
}
