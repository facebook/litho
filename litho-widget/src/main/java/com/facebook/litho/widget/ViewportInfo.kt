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

import androidx.annotation.IntDef

/**
 * An implementation of this interface will provide both the
 * [androidx.recyclerview.widget.RecyclerView]'s current visible views position and the total number
 * of items in its [androidx.recyclerview.widget.RecyclerView.Adapter].
 */
interface ViewportInfo {

  /** @return the adapter position of the first visible view. */
  fun findFirstVisibleItemPosition(): Int

  /** @return the adapter position of the last visible view. */
  fun findLastVisibleItemPosition(): Int

  /** @return the adapter position of the first fully visible view. */
  fun findFirstFullyVisibleItemPosition(): Int

  /** @return the adapter position of the last fully visible view. */
  fun findLastFullyVisibleItemPosition(): Int

  /** @return total number of items in the adapter */
  fun getItemCount(): Int

  /** Implement this interface to be notified of Viewport changes from the [Binder] */
  fun interface ViewportChanged {
    fun viewportChanged(
        firstVisibleIndex: Int,
        lastVisibleIndex: Int,
        firstFullyVisibleIndex: Int,
        lastFullyVisibleIndex: Int,
        @State state: Int
    )
  }

  @IntDef(State.SCROLLING, State.DATA_CHANGES)
  annotation class State {
    companion object {
      const val SCROLLING: Int = 0
      const val DATA_CHANGES: Int = 1
    }
  }
}
