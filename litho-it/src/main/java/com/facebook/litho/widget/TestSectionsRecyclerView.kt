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
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener

/** A test [SectionsRecyclerView] class used for unit testing. */
class TestSectionsRecyclerView(context: Context, view: RecyclerView?) :
    SectionsRecyclerView(context, view) {

  private var listener: OnRefreshListener? = null
  var removeCallbackCount: Int = 0
    private set

  var postCount: Int = 0
    private set

  private var lastRefreshingValue: Boolean = false
  var setRefreshingValuesCount: Int = 0
    private set

  override fun setRefreshing(refreshing: Boolean) {
    super.setRefreshing(refreshing)
    setRefreshingValuesCount++
    lastRefreshingValue = refreshing
  }

  override fun removeCallbacks(action: Runnable): Boolean {
    removeCallbackCount++
    return true
  }

  override fun post(action: Runnable): Boolean {
    postCount++
    return true
  }

  override fun isRefreshing(): Boolean = lastRefreshingValue

  override fun setOnRefreshListener(listener: OnRefreshListener?) {
    this.listener = listener
    super.setOnRefreshListener(listener)
  }

  fun getOnRefreshListener(): OnRefreshListener? = listener

  /** Used for resetting the fields of [TestSectionsRecyclerView] */
  fun reset() {
    setRefreshingValuesCount = 0
    removeCallbackCount = 0
    postCount = 0
  }
}
