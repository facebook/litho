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

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Implementation of the [LinearSnapHelper] supporting hscroll custom target child view
 *
 * The implementation will snap the center of the custom target child view to the center of the
 * attached [RecyclerView]. If you intend to change this behavior then override
 * [SnapHelper.findTargetSnapPosition].
 */
internal class CustomSpeedLinearSnapHelper
@JvmOverloads
constructor(private val deltaJumpThreshold: Int, private val isStrictMode: Boolean = false) :
    LinearSnapHelper() {

  override fun findTargetSnapPosition(
      layoutManager: RecyclerView.LayoutManager,
      velocityX: Int,
      velocityY: Int
  ): Int {
    if (layoutManager !is RecyclerView.SmoothScroller.ScrollVectorProvider) {
      return RecyclerView.NO_POSITION
    }

    val itemCount = layoutManager.itemCount
    if (itemCount == 0) {
      return RecyclerView.NO_POSITION
    }

    val currentView = findSnapView(layoutManager) ?: return RecyclerView.NO_POSITION
    val currentPosition =
        if (isStrictMode) {
          if (velocityX > 0 || velocityY > 0) {
            (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
          } else {
            (layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
          }
        } else {
          layoutManager.getPosition(currentView)
        }

    if (currentPosition == RecyclerView.NO_POSITION) {
      return RecyclerView.NO_POSITION
    }

    val vectorProvider = layoutManager as RecyclerView.SmoothScroller.ScrollVectorProvider
    // deltaJumps sign comes from the velocity which may not match the order of children in
    // the LayoutManager. To overcome this, we ask for a vector from the LayoutManager to
    // get the direction.
    val vectorForEnd =
        vectorProvider.computeScrollVectorForPosition(itemCount - 1)
            ?: // cannot get a vector for the given position.
            return RecyclerView.NO_POSITION

    var vDeltaJump: Int
    var hDeltaJump: Int
    if (layoutManager.canScrollHorizontally()) {
      hDeltaJump =
          estimateNextPositionDiffForFling(
              layoutManager, OrientationHelper.createHorizontalHelper(layoutManager), velocityX, 0)
      // set a threshold to the jump
      if (hDeltaJump > deltaJumpThreshold) {
        hDeltaJump = deltaJumpThreshold
      }
      if (hDeltaJump < -deltaJumpThreshold) {
        hDeltaJump = -deltaJumpThreshold
      }
      if (vectorForEnd.x < 0) {
        hDeltaJump = -hDeltaJump
      }
    } else {
      hDeltaJump = 0
    }
    if (layoutManager.canScrollVertically()) {
      vDeltaJump =
          estimateNextPositionDiffForFling(
              layoutManager, OrientationHelper.createVerticalHelper(layoutManager), 0, velocityY)
      if (vectorForEnd.y < 0) {
        vDeltaJump = -vDeltaJump
      }
    } else {
      vDeltaJump = 0
    }

    val deltaJump = if (layoutManager.canScrollVertically()) vDeltaJump else hDeltaJump
    if (deltaJump == 0) {
      return RecyclerView.NO_POSITION
    }

    var targetPos = currentPosition + deltaJump
    if (targetPos < 0) {
      targetPos = 0
    }
    if (targetPos >= itemCount) {
      targetPos = itemCount - 1
    }
    return targetPos
  }

  private fun estimateNextPositionDiffForFling(
      layoutManager: RecyclerView.LayoutManager,
      helper: OrientationHelper,
      velocityX: Int,
      velocityY: Int
  ): Int {
    val distances = calculateScrollDistance(velocityX, velocityY)
    val distancePerChild = computeDistancePerChild(layoutManager, helper)
    if (distancePerChild <= 0) {
      return 0
    }
    val distance =
        if (abs(distances[0].toDouble()) > abs(distances[1].toDouble())) distances[0]
        else distances[1]
    return Math.round(distance / distancePerChild)
  }

  companion object {
    private const val INVALID_DISTANCE = 1f

    private fun computeDistancePerChild(
        layoutManager: RecyclerView.LayoutManager,
        helper: OrientationHelper
    ): Float {
      var minPosView: View? = null
      var maxPosView: View? = null
      var minPos = Int.MAX_VALUE
      var maxPos = Int.MIN_VALUE
      val childCount = layoutManager.childCount
      if (childCount == 0) {
        return INVALID_DISTANCE
      }

      for (i in 0 until childCount) {
        val child = layoutManager.getChildAt(i)
        val pos =
            if (child != null) {
              layoutManager.getPosition(child)
            } else {
              RecyclerView.NO_POSITION
            }
        if (pos == RecyclerView.NO_POSITION) {
          continue
        }
        if (pos < minPos) {
          minPos = pos
          minPosView = child
        }
        if (pos > maxPos) {
          maxPos = pos
          maxPosView = child
        }
      }
      if (minPosView == null || maxPosView == null) {
        return INVALID_DISTANCE
      }
      val start =
          min(
                  helper.getDecoratedStart(minPosView).toDouble(),
                  helper.getDecoratedStart(maxPosView).toDouble())
              .toInt()
      val end =
          max(
                  helper.getDecoratedEnd(minPosView).toDouble(),
                  helper.getDecoratedEnd(maxPosView).toDouble())
              .toInt()
      val distance = end - start
      if (distance == 0) {
        return INVALID_DISTANCE
      }
      return 1f * distance / ((maxPos - minPos) + 1)
    }
  }
}
