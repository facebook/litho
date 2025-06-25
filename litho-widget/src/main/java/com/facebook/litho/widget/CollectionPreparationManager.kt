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

import androidx.annotation.UiThread
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.litho.ThreadUtils
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.widget.ViewportInfo.ViewportChanged
import com.facebook.rendercore.Size
import com.facebook.rendercore.SizeConstraints
import kotlin.math.max

/**
 * This class is responsible for preparing the items in the collection. It will prepare the items
 * that are in the viewport and the items that are in the range of the viewport.
 */
@ExperimentalLithoApi
class CollectionPreparationManager(private val layoutInfo: LayoutInfo) {

  /**
   * The estimated item count in the viewport, which is used to determine the number of items that
   * should be rendered.
   */
  private var estimatedItemsInViewPort: Int = UNSET
  private var mountedView: RecyclerView? = null
  private var collectionSizeProvider: (() -> Size?)? = null
  private var rangeRatio: Float? = null
  private var onEnterRange: ((Int) -> Unit)? = null
  private var onExitRange: ((Int) -> Unit)? = null
  private var postUpdateViewportAttempts = 0

  private val viewportManager: ViewportManager =
      ViewportManager(
          currentFirstVisiblePosition = RecyclerView.NO_POSITION,
          currentLastVisiblePosition = RecyclerView.NO_POSITION,
          layoutInfo = layoutInfo)
  private val updateViewportRunnable =
      object : Runnable {
        override fun run() {
          val mountedView = mountedView
          if (mountedView == null || !mountedView.hasPendingAdapterUpdates()) {
            if (viewportManager.shouldUpdate()) {
              viewportManager.onViewportChanged(ViewportInfo.State.DATA_CHANGES)
            }
            postUpdateViewportAttempts = 0
            return
          }

          // If the view gets detached, we might still have pending updates.
          // If the view's visibility is GONE, layout won't happen until it becomes visible. We
          // have to exit here, otherwise we keep posting this runnable to the next frame until it
          // becomes visible.
          if (!mountedView.isAttachedToWindow || mountedView.isGone) {
            postUpdateViewportAttempts = 0
            return
          }

          if (postUpdateViewportAttempts >= POST_UPDATE_VIEWPORT_AND_COMPUTE_RANGE_MAX_ATTEMPTS) {
            postUpdateViewportAttempts = 0
            if (viewportManager.shouldUpdate()) {
              viewportManager.onViewportChanged(ViewportInfo.State.DATA_CHANGES)
            }
            return
          }

          // If we have pending updates, wait until the sync operations are finished and try again
          // in the next frame.
          postUpdateViewportAttempts++
          mountedView.postOnAnimation(this)
        }
      }
  private val viewportChangedListener: ViewportChanged =
      object : ViewportChanged {
        override fun viewportChanged(
            firstVisibleIndex: Int,
            lastVisibleIndex: Int,
            firstFullyVisibleIndex: Int,
            lastFullyVisibleIndex: Int,
            state: Int
        ) {
          viewportManager.resetShouldUpdate()
          maybePostUpdateViewportAndComputeRange(firstVisibleIndex, lastVisibleIndex)
        }
      }
  private val rangeTraverser: RecyclerRangeTraverser
  private val isBound
    get() =
        rangeRatio != null &&
            collectionSizeProvider != null &&
            onEnterRange != null &&
            onExitRange != null

  init {
    val layoutManager = layoutInfo.getLayoutManager()
    val stackFromEnd =
        if (layoutManager is LinearLayoutManager) {
          layoutManager.stackFromEnd
        } else {
          false
        }
    rangeTraverser =
        if (stackFromEnd) {
          RecyclerRangeTraverser.BACKWARD_TRAVERSER
        } else {
          RecyclerRangeTraverser.FORWARD_TRAVERSER
        }
  }

  @UiThread
  fun bind(
      view: RecyclerView,
      rangeRatio: Float,
      collectionSizeProvider: (() -> Size?),
      onEnterRange: (Int) -> Unit,
      onExitRange: (Int) -> Unit
  ) {
    ThreadUtils.assertMainThread()
    this.mountedView = view
    this.rangeRatio = rangeRatio
    this.collectionSizeProvider = collectionSizeProvider
    this.onEnterRange = onEnterRange
    this.onExitRange = onExitRange

    view.addOnScrollListener(viewportManager.scrollListener)
    viewportManager.addViewportChangedListener(viewportChangedListener)
  }

  @UiThread
  fun unbind(view: RecyclerView) {
    ThreadUtils.assertMainThread()
    view.removeOnScrollListener(viewportManager.scrollListener)
    viewportManager.removeViewportChangedListener(viewportChangedListener)
    mountedView = null
    collectionSizeProvider = null
    rangeRatio = null
    onEnterRange = null
    onExitRange = null
    postUpdateViewportAttempts = 0
  }

  fun addViewportChangedListener(viewportChangedListener: ViewportChanged?) {
    viewportManager.addViewportChangedListener(viewportChangedListener)
  }

  /**
   * Attempts to update the viewport and compute the range of items that should be prepared. This
   * method checks if the viewport needs updating and posts a runnable to handle the update. It also
   * triggers computation of which items should enter or exit the preparation range.
   *
   * @param firstVisibleIndex The index of the first visible item in the viewport
   * @param lastVisibleIndex The index of the last visible item in the viewport
   */
  @UiThread
  fun maybePostUpdateViewportAndComputeRange(
      firstVisibleIndex: Int = layoutInfo.findFirstVisibleItemPosition(),
      lastVisibleIndex: Int = layoutInfo.findLastVisibleItemPosition(),
  ) {
    mountedView?.let { recyclerView ->
      if (viewportManager.shouldUpdate()) {
        recyclerView.removeCallbacks(updateViewportRunnable)
        recyclerView.postOnAnimation(updateViewportRunnable)
      }
    }
    computeRange(firstVisibleIndex, lastVisibleIndex)
  }

  /**
   * Computes the range of items that should be prepared for rendering based on the currently
   * visible items. This method determines which items should enter or exit the preparation range
   * based on their position relative to the visible viewport.
   *
   * @param firstVisibleIndex The index of the first visible item in the viewport
   * @param lastVisibleIndex The index of the last visible item in the viewport
   * @param traverser The traverser that defines the order in which items are processed
   */
  @UiThread
  private fun computeRange(
      firstVisibleIndex: Int,
      lastVisibleIndex: Int,
      traverser: RecyclerRangeTraverser = rangeTraverser,
  ) {
    if (!isBound) return

    val collectionSize: Size? = requireNotNull(collectionSizeProvider).invoke()
    if (collectionSize == null || estimatedItemsInViewPort == UNSET) {
      return
    }

    val firstVisibleToUse: Int = max(firstVisibleIndex, 0)
    val lastVisibleToUse: Int = max(lastVisibleIndex, 0)
    val rangeSize: Int = max(estimatedItemsInViewPort, lastVisibleToUse - firstVisibleToUse)
    val rangeStart: Int = firstVisibleToUse - (rangeSize * requireNotNull(rangeRatio)).toInt()
    val rangeEnd: Int =
        firstVisibleToUse + rangeSize + (rangeSize * requireNotNull(rangeRatio)).toInt()
    val processor =
        object : RecyclerRangeTraverser.Processor {
          override fun process(index: Int): Boolean = computeAt(index, rangeStart, rangeEnd)
        }
    traverser.traverse(0, layoutInfo.getItemCount(), firstVisibleToUse, lastVisibleToUse, processor)
  }

  private fun computeAt(index: Int, rangeStart: Int, rangeEnd: Int): Boolean {
    if (!isBound) return false

    if (index >= rangeStart && index <= rangeEnd) {
      requireNotNull(onEnterRange).invoke(index)
    } else {
      requireNotNull(onExitRange).invoke(index)
    }
    return true
  }

  /**
   * Estimates the number of items that can fit in the viewport based on measuring a sample item.
   * This calculation is performed only once when estimatedItemsInViewPort is unset and helps
   * determine how many items should be prepared for rendering to optimize performance.
   *
   * @param item A sample CollectionItem used to estimate the size of items in the collection
   * @param sizeConstraintsProvider A function that provides size constraints for measuring the
   *   given item
   */
  fun estimateItemsInViewPort(
      item: CollectionItem<*>,
      sizeConstraintsProvider: (CollectionItem<*>) -> SizeConstraints,
  ) {
    if (!isBound) return

    val collectionSize: Size? = requireNotNull(collectionSizeProvider).invoke()
    if (estimatedItemsInViewPort == UNSET && collectionSize != null) {
      val output = IntArray(2)
      item.measure(sizeConstraintsProvider(item), output)
      estimatedItemsInViewPort =
          max(
              layoutInfo.approximateRangeSize(
                  output[0], output[1], collectionSize.width, collectionSize.height),
              1)
    }
  }

  companion object {
    private const val UNSET: Int = -1
    private const val POST_UPDATE_VIEWPORT_AND_COMPUTE_RANGE_MAX_ATTEMPTS: Int = 3
  }
}
