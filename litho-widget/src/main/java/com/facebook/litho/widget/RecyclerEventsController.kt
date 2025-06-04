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

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.facebook.litho.ThreadUtils.assertMainThread
import com.facebook.litho.ThreadUtils.isMainThread

/**
 * An controller that can be passed as [com.facebook.litho.annotations.Prop] to a Recycler component
 * to trigger events from outside the component hierarchy.
 */
open class RecyclerEventsController {

  var snapHelper: SnapHelper? = null

  val isRefreshing: Boolean
    get() {
      val sectionsRecyclerView = sectionsRecyclerView ?: return false
      return sectionsRecyclerView.isRefreshing
    }

  open val recyclerView: RecyclerView?
    get() = sectionsRecyclerView?.recyclerView

  private var sectionsRecyclerView: SectionsRecyclerView? = null

  private var onRecyclerUpdateListener: OnRecyclerUpdateListener? = null

  private val clearRefreshRunnable = Runnable {
    val sectionsRecyclerView = sectionsRecyclerView
    if (sectionsRecyclerView != null && sectionsRecyclerView.isRefreshing) {
      sectionsRecyclerView.isRefreshing = false
    }
  }

  /**
   * Send the Recycler a request to scroll the content to the first item in the binder.
   *
   * @param animated if animated is set to true the scroll will happen with an animation.
   */
  fun requestScrollToTop(animated: Boolean) {
    requestScrollToPosition(0, animated)
  }

  /**
   * Send the Recycler a request to scroll the content to a specific item in the binder.
   *
   * @param animated if animated is set to true the scroll will happen with an animation.
   */
  fun requestScrollToPosition(position: Int, animated: Boolean) {
    val sectionsRecyclerView = sectionsRecyclerView ?: return

    if (animated) {
      sectionsRecyclerView.recyclerView.smoothScrollToPosition(position)
      return
    }

    sectionsRecyclerView.recyclerView.scrollToPosition(position)
  }

  /**
   * Send the Recycler a request to scroll the content to a specific item in the binder with the
   * given offset from resolved layout start. Animation will not be performed.
   *
   * If you are just trying to make a position visible, use [.requestScrollToPosition].
   *
   * Note: This offset is valid for LinearLayout only!
   *
   * @param position Index (starting at 0) of the reference item.
   * @param offset The distance (in pixels) between the start edge of the item view and start edge
   *   of the RecyclerView.
   */
  fun requestScrollToPositionWithOffset(position: Int, offset: Int) {
    val sectionsRecyclerView = sectionsRecyclerView ?: return

    val layoutManager = sectionsRecyclerView.recyclerView.layoutManager
    if (layoutManager !is LinearLayoutManager) {
      requestScrollToPosition(position, /* animated */ false)
      return
    }

    layoutManager.scrollToPositionWithOffset(position, offset)
  }

  fun clearRefreshing() {
    val sectionsRecyclerView = sectionsRecyclerView
    if (sectionsRecyclerView == null || !sectionsRecyclerView.isRefreshing) {
      return
    }

    if (isMainThread) {
      sectionsRecyclerView.isRefreshing = false
      return
    }

    sectionsRecyclerView.removeCallbacks(clearRefreshRunnable)
    sectionsRecyclerView.post(clearRefreshRunnable)
  }

  fun showRefreshing() {
    val sectionsRecyclerView = sectionsRecyclerView
    if (sectionsRecyclerView == null || sectionsRecyclerView.isRefreshing) {
      return
    }
    assertMainThread()
    sectionsRecyclerView.isRefreshing = true
  }

  fun setSectionsRecyclerView(sectionsRecyclerView: SectionsRecyclerView?) {
    this.sectionsRecyclerView = sectionsRecyclerView
    onRecyclerUpdateListener?.onUpdate(sectionsRecyclerView?.recyclerView)
  }

  fun setOnRecyclerUpdateListener(onRecyclerUpdateListener: OnRecyclerUpdateListener?) {
    this.onRecyclerUpdateListener = onRecyclerUpdateListener
  }

  fun interface OnRecyclerUpdateListener {
    fun onUpdate(recyclerView: RecyclerView?)
  }
}
