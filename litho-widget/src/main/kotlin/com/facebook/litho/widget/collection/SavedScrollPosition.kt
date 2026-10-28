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

import android.view.View
import androidx.annotation.UiThread
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.litho.Saver
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.widget.LithoRecyclerView.OnAfterLayoutListener

/**
 * The first visible child of a [LazyCollection] that saves its scroll position, and that child's
 * offset in pixels from the edge the list lays out from.
 */
internal class SavedScrollPosition
private constructor(
    /**
     * The child's index in the high 32 bits and its offset in the low 32 bits, or [NO_POSITION].
     * One value, so a save on another thread reads the index and offset of one scroll.
     */
    @Volatile private var position: Long,
    private var pending: Boolean,
) {

  constructor() : this(position = NO_POSITION, pending = false)

  /**
   * Records the list's first visible child on every scroll, including one that keeps the same
   * children visible, which the list's viewport callback doesn't report.
   */
  val scrollListener: RecyclerView.OnScrollListener =
      object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
          record(recyclerView)
        }
      }

  /**
   * Records the list's first visible child after the list lays out from its `onLayout`, as after a
   * jump by the list's controller that keeps the same children visible. A data change can move the
   * children without an `onLayout`; the list's viewport callback, which reports data changes,
   * records those through [record].
   */
  val layoutListener: OnAfterLayoutListener = OnAfterLayoutListener { record(it) }

  /**
   * Scrolls the list to a restored position, once, when it has children: to the saved index clamped
   * to [childrenSize].
   */
  @UiThread
  fun restore(controller: LazyCollectionController, childrenSize: Int) {
    if (!pending || childrenSize == 0) return
    pending = false
    val position = position
    if (position == NO_POSITION) return
    controller.scrollToIndex(indexOf(position).coerceAtMost(childrenSize - 1), offsetOf(position))
  }

  /** Records the list's first visible child, unless a restored position is still to be applied. */
  @UiThread
  fun record(recyclerView: RecyclerView) {
    if (pending) return
    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
    val index = layoutManager.findFirstVisibleItemPosition()
    if (index < 0) return
    val position = pack(index, offset(recyclerView, index))
    if (position == this.position) return
    this.position = position
  }

  companion object {
    /** Nothing recorded; a packed position is never negative, as its index isn't. */
    private const val NO_POSITION: Long = -1L

    @OptIn(ExperimentalLithoApi::class)
    val saver: Saver<SavedScrollPosition> = Saver(
        save = { holder ->
          val position = holder.position
          if (position == NO_POSITION) null else intArrayOf(indexOf(position), offsetOf(position))
        },
        restore = { value ->
          (value as? IntArray)?.let { SavedScrollPosition(pack(it[0], it[1]), pending = true) }
              ?: SavedScrollPosition()
        },
    )

    private fun pack(index: Int, offset: Int): Long =
        (index.toLong() shl 32) or (offset.toLong() and 0xFFFFFFFFL)

    private fun indexOf(position: Long): Int = (position shr 32).toInt()

    private fun offsetOf(position: Long): Int = position.toInt()

    /**
     * The offset of the child at [position] as `LinearLayoutManager.scrollToPositionWithOffset`
     * takes it: from the list's start edge, or from its end edge when the list lays out in reverse,
     * as a horizontal list in a right-to-left layout also does.
     */
    private fun offset(recyclerView: RecyclerView, position: Int): Int {
      val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return 0
      val view = layoutManager.findViewByPosition(position) ?: return 0
      val params = view.layoutParams as RecyclerView.LayoutParams
      val horizontal = layoutManager.orientation == RecyclerView.HORIZONTAL
      val reverse =
          layoutManager.reverseLayout !=
              (horizontal && layoutManager.layoutDirection == View.LAYOUT_DIRECTION_RTL)
      return with(layoutManager) {
        when {
          horizontal && reverse ->
              width - paddingRight - getDecoratedRight(view) - params.rightMargin
          horizontal -> getDecoratedLeft(view) - params.leftMargin - paddingLeft
          reverse -> height - paddingBottom - getDecoratedBottom(view) - params.bottomMargin
          else -> getDecoratedTop(view) - params.topMargin - paddingTop
        }
      }
    }
  }
}
