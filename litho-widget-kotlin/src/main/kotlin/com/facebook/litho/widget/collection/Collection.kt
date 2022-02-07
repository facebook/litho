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

package com.facebook.litho.widget.collection

import androidx.annotation.Px
import com.facebook.litho.ComponentContext
import com.facebook.litho.Handle
import com.facebook.litho.widget.SmoothScrollAlignmentType

object Collection {

  fun scrollTo(c: ComponentContext, handle: Handle, position: Int): Unit =
      CollectionRecycler.onScroll(c, handle, position)

  fun scrollToHandle(
      c: ComponentContext,
      handle: Handle,
      target: Handle,
      @Px offset: Int = 0,
  ): Unit = CollectionRecycler.onScrollToHandle(c, handle, target, offset)

  fun smoothScrollTo(
      c: ComponentContext,
      handle: Handle,
      index: Int,
      @Px offset: Int = 0,
      smoothScrollAlignmentType: SmoothScrollAlignmentType? = SmoothScrollAlignmentType.DEFAULT,
  ): Unit = CollectionRecycler.onSmoothScroll(c, handle, index, offset, smoothScrollAlignmentType)

  fun smoothScrollToHandle(
      c: ComponentContext,
      handle: Handle,
      target: Handle,
      @Px offset: Int = 0,
      smoothScrollAlignmentType: SmoothScrollAlignmentType? = SmoothScrollAlignmentType.DEFAULT,
  ): Unit =
      CollectionRecycler.onSmoothScrollToHandle(
          c, handle, target, offset, smoothScrollAlignmentType)

  fun clearRefreshing(c: ComponentContext, handle: Handle): Unit =
      CollectionRecycler.onClearRefreshing(c, handle)

  /**
   * Create a manager for tail pagination, i.e. fetch more data when a [Collection] is scrolled near
   * to the end. Should be applied to [Collection]'s pagination prop.
   * @param offsetBeforeTailFetch trigger a fetch at some offset before the end of the list
   * @param fetchNextPage lambda to perform the data fetch
   */
  fun tailPagination(
      offsetBeforeTailFetch: Int = 0,
      fetchNextPage: () -> Unit
  ): (Int, Int) -> Unit {
    return { lastVisibleIndex: Int, totalCount: Int ->
      if (lastVisibleIndex >= totalCount - 1 - offsetBeforeTailFetch) {
        fetchNextPage()
      }
    }
  }
}
