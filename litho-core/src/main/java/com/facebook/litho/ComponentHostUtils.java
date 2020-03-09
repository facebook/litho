/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.collection.SparseArrayCompat;
import com.facebook.rendercore.MountItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ComponentHostUtils {

  /**
   * Moves an item from oldIndex to newIndex. The item is taken from scrapitems if an item exists in
   * scrapItems at oldPosition. Otherwise the item is taken from items. This assumes that there is
   * no item at newIndex for the items array. If that's the case {@link
   * ComponentHostUtils#scrapItemAt(int, SparseArrayCompat, SparseArrayCompat)} has to be called
   * before invoking this.
   */
  static <T> void moveItem(
      int oldIndex, int newIndex, SparseArrayCompat<T> items, SparseArrayCompat<T> scrapItems) {
    T itemToMove;

    if (existsScrapItemAt(oldIndex, scrapItems)) {
      // Before moving the item from items we need to check whether an old item has been put in
      // the scrapItems array. If there is an item at oldIndex there, it means that in
      // items at position oldIndex there's now something else and the correct item to move to
      // newIndex is instead in the scrapItems SparseArray.
      itemToMove = scrapItems.get(oldIndex);
      scrapItems.remove(oldIndex);
    } else {
      itemToMove = items.get(oldIndex);
      items.remove(oldIndex);
    }

    items.put(newIndex, itemToMove);
  }

  /**
   * Takes the item at position index from items and puts it into scrapItems. If no such item exists
   * the invocation of this method will have no effect.
   */
  static <T> void scrapItemAt(
      int index, SparseArrayCompat<T> items, SparseArrayCompat<T> scrapItems) {
    if (items == null || scrapItems == null) {
      return;
    }
    final T value = items.get(index);
    if (value != null) {
      scrapItems.put(index, value);
    }
  }

  /** Returns true if scrapItems is not null and contains an item with key index. */
  static <T> boolean existsScrapItemAt(int index, SparseArrayCompat<T> scrapItems) {
    return scrapItems != null && scrapItems.get(index) != null;
  }

  /** Sets the state on a drawable if it is clickable or should duplicate its parent's state. */
  static void maybeSetDrawableState(View view, Drawable drawable, int flags, NodeInfo nodeInfo) {
    final boolean shouldSetState =
        (nodeInfo != null && nodeInfo.hasTouchEventHandlers())
            || LithoMountData.isDuplicateParentState(flags);

    if (shouldSetState && drawable.isStateful()) {
      drawable.setState(view.getDrawableState());
    }
  }

  /**
   * Remove the item at given {@param index}. The item is removed from {@param scrapItems} if the
   * item exists there at given index, otherwise it is removed from {@param items}.
   */
  static <T> void removeItem(
      int index, SparseArrayCompat<T> items, SparseArrayCompat<T> scrapItems) {
    if (existsScrapItemAt(index, scrapItems)) {
      scrapItems.remove(index);
    } else {
      items.remove(index);
    }
  }

  /**
   * Mounts a drawable into a view.
   *
   * @param view view into which the drawable should be mounted
   * @param drawable drawable to be mounted
   * @param bounds bounds of the drawable being mounted
   * @param flags flags that determine whether the drawable obtains state from the view
   * @param nodeInfo nodeInfo associated to the drawable node
   */
  static void mountDrawable(
      View view, Drawable drawable, Rect bounds, int flags, NodeInfo nodeInfo) {
    drawable.setVisible(view.getVisibility() == View.VISIBLE, false);
    drawable.setCallback(view);
    maybeSetDrawableState(view, drawable, flags, nodeInfo);
    view.invalidate(bounds);
  }

  static List<?> extractContent(SparseArrayCompat<MountItem> items) {
    final int size = items.size();
    if (size == 1) {
      return Collections.singletonList(items.valueAt(0).getContent());
    }

    final List<Object> content = new ArrayList<>(size);

    for (int i = 0; i < size; i++) {
      content.add(items.valueAt(i).getContent());
    }

    return content;
  }

  static TextContent extractTextContent(List<?> items) {
    final int size = items.size();
    if (size == 1) {
      Object item = items.get(0);
      return item instanceof TextContent ? (TextContent) item : TextContent.EMPTY;
    }

    final List<CharSequence> textContent = new ArrayList<>();

    for (int i = 0; i < size; ++i) {
      final Object item = items.get(i);
      if (item instanceof TextContent) {
        textContent.addAll(((TextContent) item).getTextItems());
      }
    }

    return new TextContent() {
      @Override
      public List<CharSequence> getTextItems() {
        return textContent;
      }
    };
  }

  static ImageContent extractImageContent(List<?> items) {
    final int size = items.size();
    if (size == 1) {
      Object item = items.get(0);
      return item instanceof ImageContent ? (ImageContent) item : ImageContent.EMPTY;
    }

    final List<Drawable> imageContent = new ArrayList<>();

    for (int i = 0; i < size; ++i) {
      final Object item = items.get(i);
      if (item instanceof ImageContent) {
        imageContent.addAll(((ImageContent) item).getImageItems());
      }
    }

    return new ImageContent() {
      @Override
      public List<Drawable> getImageItems() {
        return imageContent;
      }
    };
  }

  static void maybeInvalidateAccessibilityState(MountItem mountItem) {
    final LithoMountData data = (LithoMountData) mountItem.getMountData();
    if (data.isAccessible()) {
      ((ComponentHost) mountItem.getHost()).invalidateAccessibilityState();
    }
  }
}
