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

package com.facebook.rendercore.visibility

import android.graphics.Rect
import com.facebook.rendercore.Function

/**
 * Holds information about a VisibilityOutput (that is, about an item for which a visibility event
 * handler have been set). This class is justified by the fact that VisibilityOutput should be
 * immutable.
 */
class VisibilityItem(
    val key: String,
    // The invisible event and unfocused event handlers are required to make it possible to dispatch
    // the corresponding event when unbind is called or when the MountState is reset.
    var invisibleHandler: Function<Void>?,
    var unfocusedHandler: Function<Void>?,
    val visibilityChangedHandler: Function<Void>?,
    val componentName: String,
    val renderUnitId: Long,
    val bounds: Rect
) {

  private var _doNotClearInThisPass = false
  private var _wasFullyVisible = false
  private var flags = 0

  val isInFocusedRange: Boolean
    get() = flags and FLAG_FOCUSED_RANGE != 0

  fun setFocusedRange(isFocused: Boolean) {
    flags =
        if (isFocused) {
          flags or FLAG_FOCUSED_RANGE
        } else {
          flags and FLAG_FOCUSED_RANGE.inv()
        }
  }

  val isInFullImpressionRange: Boolean
    /**
     * Returns true if the component associated with this VisibilityItem is in the full impression
     * range.
     */
    get() {
      val allEdgesVisible =
          FLAG_LEFT_EDGE_VISIBLE or
              FLAG_TOP_EDGE_VISIBLE or
              FLAG_RIGHT_EDGE_VISIBLE or
              FLAG_BOTTOM_EDGE_VISIBLE
      return flags and allEdgesVisible == allEdgesVisible
    }

  /**
   * Sets the flags corresponding to the edges of the component that are visible. Afterwards, it
   * checks if the component has entered the full impression visible range and, if so, it sets the
   * appropriate flag.
   */
  fun setVisibleEdges(componentBounds: Rect, componentVisibleBounds: Rect) {
    if (componentBounds.top == componentVisibleBounds.top) {
      flags = flags or FLAG_TOP_EDGE_VISIBLE
    }
    if (componentBounds.bottom == componentVisibleBounds.bottom) {
      flags = flags or FLAG_BOTTOM_EDGE_VISIBLE
    }
    if (componentBounds.left == componentVisibleBounds.left) {
      flags = flags or FLAG_LEFT_EDGE_VISIBLE
    }
    if (componentBounds.right == componentVisibleBounds.right) {
      flags = flags or FLAG_RIGHT_EDGE_VISIBLE
    }
  }

  fun doNotClearInThisPass(): Boolean = _doNotClearInThisPass

  fun setDoNotClearInThisPass(doNotClearInThisPass: Boolean) {
    _doNotClearInThisPass = doNotClearInThisPass
  }

  fun wasFullyVisible(): Boolean = _wasFullyVisible

  fun setWasFullyVisible(fullyVisible: Boolean) {
    _wasFullyVisible = fullyVisible
  }

  companion object {
    private const val FLAG_LEFT_EDGE_VISIBLE = 1 shl 1
    private const val FLAG_TOP_EDGE_VISIBLE = 1 shl 2
    private const val FLAG_RIGHT_EDGE_VISIBLE = 1 shl 3
    private const val FLAG_BOTTOM_EDGE_VISIBLE = 1 shl 4
    private const val FLAG_FOCUSED_RANGE = 1 shl 5
  }
}
