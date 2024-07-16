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

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.facebook.rendercore.utils.ThreadUtils

abstract class ItemDecorationWithMeasureFunction() : ItemDecoration() {

  internal var measure: (View.() -> Unit)? = null
    @JvmName("setMeasure")
    set(value) {
      ThreadUtils.assertMainThread()
      field = value
    }
    get() {
      ThreadUtils.assertMainThread()
      return field
    }

  override fun getItemOffsets(
      outRect: Rect,
      view: View,
      parent: RecyclerView,
      state: RecyclerView.State
  ) {
    getItemOffsets(
        outRect,
        view,
        parent,
        state,
        checkNotNull(measure) { "measure function is null" },
    )
  }

  abstract fun getItemOffsets(
      outRect: Rect,
      view: View,
      parent: RecyclerView,
      state: RecyclerView.State,
      measure: View.() -> Unit
  )
}
