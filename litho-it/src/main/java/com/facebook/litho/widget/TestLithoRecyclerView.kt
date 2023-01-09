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
import java.util.ArrayList

/** A test [LithoRecyclerView] class used for unit testing. */
class TestLithoRecyclerView(context: Context) : LithoRecyclerView(context) {

  private var itemAnimator: ItemAnimator? = null
  private var touchInterceptor: TouchInterceptor? = null
  private val removeOnScrollListeners: MutableList<OnScrollListener> = ArrayList()
  private val addOnScrollListeners: MutableList<OnScrollListener> = ArrayList()
  private var layoutRequested = false

  override fun getItemAnimator(): ItemAnimator? = itemAnimator

  override fun setItemAnimator(animator: ItemAnimator?) {
    itemAnimator = animator
  }

  override fun removeOnScrollListener(onScrollListener: OnScrollListener) {
    removeOnScrollListeners.add(onScrollListener)
    super.removeOnScrollListener(onScrollListener)
  }

  val removeOnScrollListenersCount: Int
    get() = removeOnScrollListeners.size

  override fun addOnScrollListener(onScrollListener: OnScrollListener) {
    addOnScrollListeners.add(onScrollListener)
    super.addOnScrollListener(onScrollListener)
  }

  val addOnScrollListenersCount: Int
    get() = addOnScrollListeners.size

  override fun setTouchInterceptor(touchInterceptor: TouchInterceptor?) {
    super.setTouchInterceptor(touchInterceptor)
    this.touchInterceptor = touchInterceptor
  }

  fun getTouchInterceptor(): TouchInterceptor? = touchInterceptor

  override fun requestLayout() {
    super.requestLayout()
    layoutRequested = true
  }

  override fun isLayoutRequested(): Boolean = layoutRequested
}
