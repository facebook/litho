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

package com.facebook.litho

import android.graphics.drawable.Drawable
import android.view.View
import androidx.collection.SparseArrayCompat
import com.facebook.rendercore.MountItem
import java.util.ArrayList

internal object ComponentHostUtils {

  /**
   * Moves an item from oldIndex to newIndex. The item is taken from scrapitems if an item exists in
   * scrapItems at oldPosition. Otherwise the item is taken from items. This assumes that there is
   * no item at newIndex for the items array. If that's the case [ComponentHostUtils.scrapItemAt]
   * has to be called before invoking this.
   */
  @JvmStatic
  fun <T> moveItem(
      oldIndex: Int,
      newIndex: Int,
      items: SparseArrayCompat<T?>,
      scrapItems: SparseArrayCompat<T>?
  ) {
    val itemToMove: T?
    if (existsScrapItemAt(oldIndex, scrapItems)) {
      // Before moving the item from items we need to check whether an old item has been put in
      // the scrapItems array. If there is an item at oldIndex there, it means that in
      // items at position oldIndex there's now something else and the correct item to move to
      // newIndex is instead in the scrapItems SparseArray.
      itemToMove = scrapItems?.get(oldIndex)
      scrapItems?.remove(oldIndex)
    } else {
      itemToMove = items[oldIndex]
      items.remove(oldIndex)
    }
    items.put(newIndex, itemToMove)
  }

  /**
   * Takes the item at position index from items and puts it into scrapItems. If no such item exists
   * the invocation of this method will have no effect.
   */
  @JvmStatic
  fun <T> scrapItemAt(index: Int, items: SparseArrayCompat<T>?, scrapItems: SparseArrayCompat<T>?) {
    if (items == null || scrapItems == null) {
      return
    }
    val value = items[index]
    if (value != null) {
      scrapItems.put(index, value)
    }
  }

  /** Returns true if scrapItems is not null and contains an item with key index. */
  private fun <T> existsScrapItemAt(index: Int, scrapItems: SparseArrayCompat<T>?): Boolean =
      scrapItems?.get(index) != null

  /** Sets the state on a drawable if it is clickable or should duplicate its parent's state. */
  @JvmStatic
  fun maybeSetDrawableState(view: View, drawable: Drawable, flags: Int) {
    val shouldSetState =
        LithoRenderUnit.hasTouchEventHandlers(flags) ||
            LithoRenderUnit.isDuplicateParentState(flags)
    if (shouldSetState && drawable.isStateful) {
      drawable.state = view.drawableState
    }
  }

  /**
   * Remove the item at given {@param index}. The item is removed from {@param scrapItems} if the
   * item exists there at given index, otherwise it is removed from {@param items}.
   */
  @JvmStatic
  fun <T> removeItem(index: Int, items: SparseArrayCompat<T>, scrapItems: SparseArrayCompat<T>?) {
    if (existsScrapItemAt(index, scrapItems)) {
      scrapItems?.remove(index)
    } else {
      items.remove(index)
    }
  }

  @JvmStatic
  fun extractContent(items: SparseArrayCompat<MountItem>): List<*> {
    val size = items.size()
    val content: MutableList<Any> = ArrayList(size)
    for (i in 0 until size) {
      content.add(items.valueAt(i).content)
    }
    return content
  }

  /**
   * This is a helper method that makes Java code to get this data in an easier way. Once we
   * finished the Kotlin migration we should remove this method and replace it the code that it
   * calls.
   */
  @JvmStatic
  fun extractTextContent(items: List<*>): List<TextContent> = items.filterIsInstance<TextContent>()

  @JvmStatic
  fun extractImageContent(items: List<*>): ImageContent =
      object : ImageContent {
        override val imageItems: List<Drawable> =
            items.filterIsInstance<ImageContent>().flatMap { it.imageItems }
      }
}
