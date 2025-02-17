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
import com.facebook.litho.debug.LithoDebugEvent
import com.facebook.rendercore.LogLevel
import com.facebook.rendercore.debug.DebugEvent
import com.facebook.rendercore.debug.DebugEventAttribute
import com.facebook.rendercore.debug.DebugEventDispatcher
import com.facebook.rendercore.thread.utils.ThreadUtils

/**
 * Creates an [ItemDecoration] that provides the developer a function to measure the [View] if the
 * insets required depend on the measured size of the [View]. The [measure] function will use the
 * size constraints recorded by [Binder] of the Recycler Component. This approach is required
 * because the [RecyclerView] gets the insets from the [ItemDecoration] before the [View] is
 * measured, so the measured size of the view is 0, and the insets would not be correct.
 */
abstract class ItemDecorationWithMeasureFunction : ItemDecoration() {

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

  final override fun getItemOffsets(
      outRect: Rect,
      view: View,
      parent: RecyclerView,
      state: RecyclerView.State
  ) {

    val measureFunctionToUse: View.() -> Unit =
        measure
            ?: NoOpMeasure.also {
              DebugEventDispatcher.dispatch(
                  type = LithoDebugEvent.DebugInfo,
                  renderStateId = DebugEvent.NoId,
                  logLevel = LogLevel.ERROR,
              ) {
                it[DebugEventAttribute.Name] = "ItemDecorationWithNullMeasureFunction"
              }
            }

    getItemOffsets(
        outRect,
        view,
        parent,
        state,
        measureFunctionToUse,
    )
  }

  abstract fun getItemOffsets(
      outRect: Rect,
      view: View,
      parent: RecyclerView,
      state: RecyclerView.State,
      measure: View.() -> Unit
  )

  companion object {
    val NoOpMeasure: View.() -> Unit = {}
  }
}
