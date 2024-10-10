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

package com.facebook.litho.widget.zoomable

import android.view.MotionEvent

/**
 * Gesture Listener class that holds all of the listeners from [LithoZoomableController]s that will
 * react to the onTouch [MotionEvent]s.
 */
class ZoomableTouchGestureListener {

  private val listeners: MutableList<ZoomableTouchListener> = mutableListOf()

  val isCurrentlyInZoom: Boolean
    get() = listeners.any { it.isCurrentlyInZoom }

  fun addZoomableTouchListener(listener: ZoomableTouchListener) {
    listeners.add(listener)
  }

  fun removeZoomableTouchListener(listener: ZoomableTouchListener) {
    listeners.remove(listener)
  }

  open fun onTouch(event: MotionEvent): Boolean {
    return listeners.any { it.onTouchEvent(event) }
  }
}
