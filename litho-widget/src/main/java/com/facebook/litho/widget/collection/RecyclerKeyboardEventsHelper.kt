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

import android.view.KeyEvent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max
import kotlin.math.min

object RecyclerKeyboardEventsHelper {

  /** Handle the key event and return true if the event is handled. Otherwise, return false. */
  @JvmStatic
  fun dispatchKeyEvent(recyclerView: RecyclerView, event: KeyEvent): Boolean {

    val linearLayoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return false
    val adapter = recyclerView.adapter ?: return false

    // only support horizontal scrolling for now
    if (linearLayoutManager.canScrollHorizontally() && event.action == KeyEvent.ACTION_UP) {
      val childView: View? = linearLayoutManager.getFocusedChild()
      if (childView != null) {
        // respect the focusable state of the child view
        return false
      }

      val keyCode = event.keyCode
      when (keyCode) {
        KeyEvent.KEYCODE_DPAD_LEFT -> {
          // scroll to the previous item of the first visible item
          val firstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition()
          val prevPosition = firstVisibleItemPosition - 1
          val targetPosition = max(0, prevPosition)
          if (prevPosition == targetPosition) {
            // we found a valid target position
            recyclerView.smoothScrollToPosition(targetPosition)
            return true
          }
        }
        KeyEvent.KEYCODE_DPAD_RIGHT -> {
          // scroll to the next item of the last visible item
          val lastVisibleItemPosition = linearLayoutManager.findLastVisibleItemPosition()
          val nextPosition = lastVisibleItemPosition + 1
          val targetPosition = min((adapter.itemCount - 1), nextPosition)
          if (nextPosition == targetPosition) {
            // we found a valid target position
            recyclerView.smoothScrollToPosition(targetPosition)
            return true
          }
        }
      }
    }
    return false
  }
}
