/*
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

package com.facebook.rendercore;

import android.graphics.drawable.Drawable;
import android.view.View;

class MountUtils {

  /**
   * Moves an item from oldIndex to newIndex. The item is taken from scrapitems if an item exists in
   * scrapItems at oldPosition. Otherwise the item is taken from items. This assumes that there is
   * no item at newIndex for the items array. If that's the case {@link MountUtils#scrapItemAt(int,
   * T[], T[])} has to be called before invoking this.
   */
  static <T> void moveItem(int oldIndex, int newIndex, T[] items, T[] scrapItems) {
    T itemToMove;

    if (existsScrapItemAt(oldIndex, scrapItems)) {
      // Before moving the item from items we need to check whether an old item has been put in
      // the scrapItems array. If there is an item at oldIndex there, it means that in
      // items at position oldIndex there's now something else and the correct item to move to
      // newIndex is instead in the scrapItems SparseArray.
      itemToMove = scrapItems[oldIndex];
      scrapItems[oldIndex] = null;
    } else {
      itemToMove = items[oldIndex];
      items[oldIndex] = null;
    }

    items[newIndex] = itemToMove;
  }

  /**
   * Takes the item at position index from items and puts it into scrapItems. If no such item exists
   * the invocation of this method will have no effect.
   */
  static <T> void scrapItemAt(int index, T[] items, T[] scrapItems) {
    if (items == null || scrapItems == null) {
      return;
    }
    final T value = items[index];
    if (value != null) {
      scrapItems[index] = value;
    }
  }

  /** Returns true if scrapItems is not null and contains an item with key index. */
  static <T> boolean existsScrapItemAt(int index, T[] scrapItems) {
    return scrapItems != null && scrapItems[index] != null;
  }

  /** Sets the state on a drawable if it is clickable or should duplicate its parent's state. */
  static void maybeSetDrawableState(View view, Drawable drawable) {
    if (drawable.isStateful()) {
      drawable.setState(view.getDrawableState());
    }
  }

  /**
   * Remove the item at given {@param index}. The item is removed from {@param scrapItems} if the
   * item exists there at given index, otherwise it is removed from {@param items}.
   */
  static <T> void removeItem(int index, T[] items, T[] scrapItems) {
    if (existsScrapItemAt(index, scrapItems)) {
      scrapItems[index] = null;
    } else {
      items[index] = null;
    }
  }

  /**
   * Mounts a drawable into a view.
   *
   * @param view view into which the drawable should be mounted
   * @param drawable drawable to be mounted
   */
  static void mountDrawable(View view, Drawable drawable) {
    drawable.setVisible(view.getVisibility() == View.VISIBLE, false);
    drawable.setCallback(view);
    maybeSetDrawableState(view, drawable);
  }
}
