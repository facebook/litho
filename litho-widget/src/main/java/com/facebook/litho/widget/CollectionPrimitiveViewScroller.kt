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

import android.content.Context
import androidx.annotation.UiThread
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import com.facebook.litho.annotations.ExperimentalLithoApi

/** A helper class to manage scrolling behavior in the [LithoRecyclerView]. */
@ExperimentalLithoApi
class CollectionPrimitiveViewScroller(private val context: Context) {

  private var currentTarget = RecyclerView.NO_POSITION
  private var currentOffset = 0
  private var smoothScrollAlignmentType: SmoothScrollAlignmentType? = null

  private var layoutInfo: LayoutInfo? = null
  private var adapter: CollectionPrimitiveViewAdapter? = null
  /** Indicates whether the scroller is bound to the RecyclerView. */
  private val isBound
    get() = layoutInfo != null && adapter != null

  fun bind(layoutInfo: LayoutInfo, adapter: CollectionPrimitiveViewAdapter) {
    this.layoutInfo = layoutInfo
    this.adapter = adapter
  }

  fun unbind() {
    this.layoutInfo = null
    this.adapter = null
  }

  @UiThread
  fun startSmoothScrollWithOffset(position: Int, offset: Int, type: SmoothScrollAlignmentType) {
    if (!isBound) {
      currentTarget = position
      currentOffset = offset
      smoothScrollAlignmentType = type
      return
    }

    val smoothScroller = SnapUtil.getSmoothScrollerWithOffset(context, offset, type)
    smoothScroller.targetPosition = position
    layoutInfo?.getLayoutManager()?.startSmoothScroll(smoothScroller)
  }

  @UiThread
  fun startSmoothScrollWithOffset(id: Any, offset: Int, type: SmoothScrollAlignmentType) {
    val index: Int = adapter?.findPositionById(id) ?: RecyclerView.NO_POSITION
    if (index >= 0) {
      // We might not be able to find the position for the id, skip to avoid polluting the state.
      startSmoothScrollWithOffset(index, offset, type)
    }
  }

  @UiThread
  fun scrollToPositionWithOffset(position: Int, offset: Int) {
    if (!isBound) {
      currentTarget = position
      currentOffset = offset
      return
    }

    layoutInfo?.scrollToPositionWithOffset(position, offset)
  }

  @UiThread
  fun scrollToPositionWithOffset(id: Any, offset: Int) {
    val index: Int = adapter?.findPositionById(id) ?: RecyclerView.NO_POSITION
    if (index >= 0) {
      // We might not be able to find the position for the id, skip to avoid polluting the state.
      scrollToPositionWithOffset(index, offset)
    }
  }

  @UiThread
  fun rememberScrollOffset(view: RecyclerView) {
    layoutInfo?.let { layoutInfo ->
      val layoutManager: RecyclerView.LayoutManager = layoutInfo.getLayoutManager()
      val firstView = layoutManager.findViewByPosition(layoutInfo.findFirstVisibleItemPosition())

      if (firstView != null) {
        val isReverseLayout: Boolean =
            if (layoutManager is LinearLayoutManager) {
              layoutManager.reverseLayout
            } else {
              false
            }

        currentOffset =
            if (layoutInfo.getScrollDirection() == OrientationHelper.HORIZONTAL) {
              if (isReverseLayout)
                  (view.width -
                      layoutManager.paddingRight -
                      layoutManager.getDecoratedRight(firstView))
              else layoutManager.getDecoratedLeft(firstView) - layoutManager.paddingLeft
            } else {
              if (isReverseLayout)
                  (view.height -
                      layoutManager.paddingBottom -
                      layoutManager.getDecoratedBottom(firstView))
              else layoutManager.getDecoratedTop(firstView) - layoutManager.paddingTop
            }
      } else {
        currentOffset = 0
      }
    }
  }

  @UiThread
  fun scrollToInitPosition(view: RecyclerView, position: Int = currentTarget) {
    // TODO: to support the circular list as a follow up task.
    val alignmentType = smoothScrollAlignmentType
    if (alignmentType != null) {
      startSmoothScrollWithOffset(position, currentOffset, alignmentType)
    } else {
      if (layoutInfo is StaggeredGridLayoutInfo) {
        // Run scrollToPositionWithOffset to restore positions for StaggeredGridLayout may cause a
        // layout issue. Posting it to the next UI update can solve this issue.
        view.post { scrollToPositionWithOffset(position, currentOffset) }
      } else {
        scrollToPositionWithOffset(position, currentOffset)
      }
    }
  }
}
