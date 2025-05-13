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
import kotlin.math.max

/** Utility class for calculating the wrapped height of given holders. */
object LayoutInfoUtils {

  @JvmStatic
  fun getTopDecorationHeight(layoutManager: RecyclerView.LayoutManager, position: Int): Int {
    val itemView = layoutManager.getChildAt(position)
    return if ((itemView != null)) layoutManager.getTopDecorationHeight(itemView) else 0
  }

  @JvmStatic
  fun getBottomDecorationHeight(layoutManager: RecyclerView.LayoutManager, position: Int): Int {
    val itemView = layoutManager.getChildAt(position)
    return if ((itemView != null)) layoutManager.getBottomDecorationHeight(itemView) else 0
  }

  /**
   * Return the accumulated height of ComponentTreeHolders, or return the [maxHeight] if the
   * accumulated height is higher than the [maxHeight].
   */
  @JvmStatic
  fun computeLinearLayoutWrappedHeight(
      linearLayoutManager: LinearLayoutManager,
      maxHeight: Int,
      componentTreeHolders: List<ComponentTreeHolder>
  ): Int {
    val itemCount = componentTreeHolders.size
    var measuredHeight = 0

    when (linearLayoutManager.orientation) {
      LinearLayoutManager.VERTICAL -> {
        var i = 0
        while (i < itemCount) {
          val holder = componentTreeHolders[i]

          measuredHeight += holder.measuredHeight
          measuredHeight += getTopDecorationHeight(linearLayoutManager, i)
          measuredHeight += getBottomDecorationHeight(linearLayoutManager, i)

          if (measuredHeight > maxHeight) {
            measuredHeight = maxHeight
            break
          }
          i++
        }
        return measuredHeight
      }
      LinearLayoutManager.HORIZONTAL ->
          throw IllegalStateException(
              "This method should only be called when orientation is vertical")
      else ->
          throw IllegalStateException(
              "This method should only be called when orientation is vertical")
    }
  }

  /**
   * Return the max height in the [componentTreeHolders], the range is from position [start] to
   * position [end] (excluded).
   */
  @JvmStatic
  fun getMaxHeightInRow(
      start: Int,
      end: Int,
      componentTreeHolders: List<ComponentTreeHolder>
  ): Int {
    val itemCount = componentTreeHolders.size

    var measuredHeight = 0
    var i = start
    while (i < end && i < itemCount) {
      val holder = componentTreeHolders[i]
      measuredHeight = max(measuredHeight.toDouble(), holder.measuredHeight.toDouble()).toInt()
      i++
    }
    return measuredHeight
  }
}
