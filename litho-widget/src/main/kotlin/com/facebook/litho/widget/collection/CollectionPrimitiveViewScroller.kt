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

import android.content.Context
import android.view.View
import androidx.annotation.UiThread
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionInfoCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerViewAccessibilityDelegate
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.widget.LayoutInfo
import com.facebook.litho.widget.SmoothScrollAlignmentType
import com.facebook.litho.widget.SnapUtil
import com.facebook.litho.widget.StaggeredGridLayoutInfo
import kotlin.math.max

/**
 * A helper class to manage scrolling behavior in the [com.facebook.litho.widget.LithoRecyclerView].
 */
@ExperimentalLithoApi
class CollectionPrimitiveViewScroller(private val context: Context) {

  private var currentTarget = RecyclerView.NO_POSITION
  private var currentOffset = 0
  private var smoothScrollAlignmentType: SmoothScrollAlignmentType? = null

  private var layoutInfo: LayoutInfo? = null
  private var adapter: CollectionPrimitiveViewAdapter? = null
  private var recyclerView: RecyclerView? = null

  /** Indicates whether the scroller is bound to the RecyclerView. */
  private val isBound
    get() = layoutInfo != null && adapter != null && recyclerView != null

  fun bind(
      layoutInfo: LayoutInfo,
      recyclerView: RecyclerView,
      adapter: CollectionPrimitiveViewAdapter,
  ) {
    this.layoutInfo = layoutInfo
    this.adapter = adapter
    this.recyclerView = recyclerView
  }

  fun unbind() {
    this.layoutInfo = null
    this.adapter = null
    this.recyclerView = null
  }

  @UiThread
  fun startSmoothScrollWithOffset(position: Int, offset: Int, type: SmoothScrollAlignmentType) {
    if (!isBound) {
      currentTarget = position
      currentOffset = offset
      smoothScrollAlignmentType = type
      return
    }

    val targetPosition =
        if (checkNotNull(adapter).isCircular) {
          getClosestIndexInCircularList(checkNotNull(recyclerView), position)
        } else {
          position
        }
    val smoothScroller = SnapUtil.getSmoothScrollerWithOffset(context, offset, type)
    smoothScroller.targetPosition = targetPosition
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

    val targetPosition =
        if (checkNotNull(adapter).isCircular) {
          getClosestIndexInCircularList(checkNotNull(recyclerView), position)
        } else {
          position
        }
    layoutInfo?.scrollToPositionWithOffset(targetPosition, offset)
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
  fun rememberScrollPosition(view: RecyclerView) {
    layoutInfo?.let { layoutInfo ->
      currentTarget = layoutInfo.findFirstVisibleItemPosition()
      val layoutManager: RecyclerView.LayoutManager = layoutInfo.getLayoutManager()
      val firstView = layoutManager.findViewByPosition(currentTarget)

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
    val adapter = checkNotNull(adapter)
    val layoutManager = checkNotNull(layoutInfo).getLayoutManager()

    var targetPosition = position
    if (adapter.isCircular) {
      // The first time we mount with a circular collection, offset its first visible position
      // by Integer.MAX_VALUE / 2. We only want to do this once if the position is invalid or hasn't
      // been mounted before. This is so that the value saved in unmount doesn't keep getting the
      // offset added to it.
      if (targetPosition < 0) {
        val jumpToMiddle = Int.MAX_VALUE / 2
        val totalSize = adapter.getItems().size
        val offsetFirstItem =
            if (totalSize > 0) {
              jumpToMiddle % totalSize
            } else {
              0
            }
        targetPosition = max(0, targetPosition) + jumpToMiddle - offsetFirstItem
      }

      // Initialize circular RecyclerView position
      view.scrollToPosition(targetPosition)
      // Circular RecyclerViews report their size as Integer.MAX_VALUE, which makes Talkback
      // actually announce "In List, 2147483674 items". This overrides the row/column count on the
      // AccessibilityNodeInfo to accurately reflect the real number of items in the list.
      view.setAccessibilityDelegateCompat(
          object : RecyclerViewAccessibilityDelegate(view) {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat,
            ) {
              super.onInitializeAccessibilityNodeInfo(host, info)

              val itemCount = adapter.getItems().size
              val rowCount = if (layoutManager.canScrollVertically()) itemCount else 1
              val colCount = if (layoutManager.canScrollHorizontally()) itemCount else 1

              val collectionInfo =
                  CollectionInfoCompat.obtain(
                      rowCount,
                      colCount,
                      false,
                      CollectionInfoCompat.SELECTION_MODE_NONE,
                  )
              info.setCollectionInfo(collectionInfo)
            }
          },
      )
    } else if (targetPosition >= 0) {
      val alignmentType = smoothScrollAlignmentType
      if (alignmentType != null) {
        startSmoothScrollWithOffset(targetPosition, currentOffset, alignmentType)
      } else {
        if (layoutInfo is StaggeredGridLayoutInfo) {
          // Run scrollToPositionWithOffset to restore positions for StaggeredGridLayout may cause a
          // layout issue. Posting it to the next UI update can solve this issue.
          view.post { scrollToPositionWithOffset(targetPosition, currentOffset) }
        } else {
          scrollToPositionWithOffset(targetPosition, currentOffset)
        }
      }
    }
  }

  @UiThread
  private fun getClosestIndexInCircularList(recyclerView: RecyclerView, target: Int): Int {
    // Since circular lists position us in the middle of 0->MAX_INT, scrolling to a specific
    // index interacts poorly. We either jump immediately to the beginning of the list (and ruin the
    // "circular illusion") or send the RV on a mission to do an animated scroll from index
    // MAX_INT/2 to some small number, animating forever effectively. This logic instead tries to
    // find the closest item corresponding to the given index within a circular list.
    val firstChild = recyclerView.getChildAt(0)
    val layoutManager = recyclerView.layoutManager
    val adapter = adapter
    if (firstChild == null || layoutManager == null || adapter == null) {
      return target
    }
    val numChildren = adapter.getItems().size
    val firstVisibleIndex = layoutManager.getPosition(firstChild)
    return firstVisibleIndex +
        (target - firstVisibleIndex % numChildren + numChildren) % numChildren
  }
}
