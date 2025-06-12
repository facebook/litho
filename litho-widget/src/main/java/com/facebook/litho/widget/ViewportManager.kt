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

import androidx.annotation.AnyThread
import androidx.annotation.GuardedBy
import androidx.annotation.UiThread
import androidx.recyclerview.widget.RecyclerView
import com.facebook.litho.widget.ViewportInfo.ViewportChanged
import javax.annotation.concurrent.ThreadSafe
import kotlin.math.max

/**
 * This class will handle all viewport changes due to both scrolling and [ViewHolder] removal that
 * is not related to scrolling.
 *
 * Classes that are interested to have its viewport changes handled by [ViewportManager] should set
 * the [OnScrollListener] returned from [ViewportManager.getScrollListener] in the [RecyclerView]
 */
@ThreadSafe
class ViewportManager(
    private var currentFirstVisiblePosition: Int,
    private var currentLastVisiblePosition: Int,
    private val layoutInfo: LayoutInfo
) {

  private var currentFirstFullyVisiblePosition: Int = layoutInfo.findFirstFullyVisibleItemPosition()
  private var currentLastFullyVisiblePosition: Int = layoutInfo.findLastFullyVisibleItemPosition()
  private var totalItemCount: Int = layoutInfo.getItemCount()
  private var shouldUpdate = false

  @GuardedBy("this")
  private val viewportChangedListeners: MutableList<ViewportChanged> = ArrayList(2)

  @get:UiThread val scrollListener: ViewportScrollListener = ViewportScrollListener()

  /**
   * Handles a change in viewport. This method should not be called outside of the method
   * [OnScrollListener.onScrolled]
   */
  @UiThread
  fun onViewportChanged(@ViewportInfo.State state: Int) {
    val firstVisiblePosition = layoutInfo.findFirstVisibleItemPosition()
    val lastVisiblePosition = layoutInfo.findLastVisibleItemPosition()
    val firstFullyVisibleItemPosition = layoutInfo.findFirstFullyVisibleItemPosition()
    val lastFullyVisibleItemPosition = layoutInfo.findLastFullyVisibleItemPosition()
    val totalItemCount = layoutInfo.getItemCount()

    if (firstVisiblePosition < 0 || lastVisiblePosition < 0) {
      return
    }

    if (firstVisiblePosition == currentFirstVisiblePosition &&
        lastVisiblePosition == currentLastVisiblePosition &&
        firstFullyVisibleItemPosition == currentFirstFullyVisiblePosition &&
        lastFullyVisibleItemPosition == currentLastFullyVisiblePosition &&
        totalItemCount == this.totalItemCount &&
        state != ViewportInfo.State.DATA_CHANGES) {
      return
    }

    currentFirstVisiblePosition = firstVisiblePosition
    currentLastVisiblePosition = lastVisiblePosition
    currentFirstFullyVisiblePosition = firstFullyVisibleItemPosition
    currentLastFullyVisiblePosition = lastFullyVisibleItemPosition
    this.totalItemCount = totalItemCount
    shouldUpdate = false

    val viewportChangedListeners: List<ViewportChanged>
    synchronized(this) {
      if (this.viewportChangedListeners.isEmpty()) {
        return
      }
      viewportChangedListeners = ArrayList(this.viewportChangedListeners)
    }

    val size = viewportChangedListeners.size
    for (i in 0 until size) {
      val viewportChangedListener = viewportChangedListeners[i]
      viewportChangedListener.viewportChanged(
          firstVisiblePosition,
          lastVisiblePosition,
          firstFullyVisibleItemPosition,
          lastFullyVisibleItemPosition,
          state)
    }
  }

  @UiThread
  fun setShouldUpdate(shouldUpdate: Boolean) {
    this.shouldUpdate = this.shouldUpdate || shouldUpdate
  }

  @UiThread
  fun resetShouldUpdate() {
    shouldUpdate = false
  }

  @UiThread
  fun insertAffectsVisibleRange(position: Int, size: Int, viewportCount: Int): Boolean {
    if (shouldUpdate() || viewportCount == RecyclerBinder.UNSET) {
      return true
    }

    val lastVisiblePosition =
        max((currentFirstVisiblePosition + viewportCount - 1), currentLastVisiblePosition)

    return position <= lastVisiblePosition
  }

  @UiThread
  fun updateAffectsVisibleRange(position: Int, size: Int): Boolean {
    if (shouldUpdate()) {
      return true
    }

    val visibleRange = currentFirstVisiblePosition..currentLastVisiblePosition
    for (index in position until position + size) {
      if (index in visibleRange) {
        return true
      }
    }

    return false
  }

  @UiThread
  fun moveAffectsVisibleRange(fromPosition: Int, toPosition: Int, viewportCount: Int): Boolean {
    if (shouldUpdate() || viewportCount == RecyclerBinder.UNSET) {
      return true
    }

    val isNewPositionInVisibleRange =
        (toPosition >= currentFirstVisiblePosition &&
            toPosition <= currentFirstVisiblePosition + viewportCount - 1)

    val isOldPositionInVisibleRange =
        (fromPosition >= currentFirstVisiblePosition &&
            fromPosition <= currentFirstVisiblePosition + viewportCount - 1)

    return isNewPositionInVisibleRange || isOldPositionInVisibleRange
  }

  @UiThread
  fun removeAffectsVisibleRange(position: Int, size: Int): Boolean {
    if (shouldUpdate()) {
      return true
    }

    return position <= currentLastVisiblePosition
  }

  /**
   * Whether first/last visible positions should be updated. If this returns true, we should not do
   * any computations based on current first/last visible positions until they are updated.
   */
  @UiThread
  fun shouldUpdate(): Boolean {
    return currentFirstVisiblePosition < 0 || currentLastVisiblePosition < 0 || shouldUpdate
  }

  @AnyThread
  fun addViewportChangedListener(viewportChangedListener: ViewportChanged?) {
    if (viewportChangedListener == null) {
      return
    }

    synchronized(this) { viewportChangedListeners.add(viewportChangedListener) }
  }

  @AnyThread
  fun removeViewportChangedListener(viewportChangedListener: ViewportChanged?) {
    if (viewportChangedListener == null) {
      return
    }

    synchronized(this) {
      if (viewportChangedListeners.isEmpty()) {
        return
      }
      viewportChangedListeners.remove(viewportChangedListener)
    }
  }

  inner class ViewportScrollListener : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
      onViewportChanged(ViewportInfo.State.SCROLLING)
    }
  }
}
