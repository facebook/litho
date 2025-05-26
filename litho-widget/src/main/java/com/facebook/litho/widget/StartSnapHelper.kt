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

import android.graphics.PointF
import android.util.DisplayMetrics
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.SmoothScroller.ScrollVectorProvider
import androidx.recyclerview.widget.SnapHelper
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

open class StartSnapHelper : SnapHelper {

  // Orientation helpers are lazily created per LayoutManager.
  private var verticalHelper: OrientationHelper? = null
  private var horizontalHelper: OrientationHelper? = null

  private var verticalHelperLayoutManager: LayoutManager? = null
  private var horizontalHelperLayoutManager: LayoutManager? = null
  private var recyclerView: RecyclerView? = null
  private val flingOffset: Int
  private val offset: Int

  constructor(flingOffset: Int) {
    this.flingOffset = flingOffset
    this.offset = 0
  }

  constructor(flingOffset: Int, offset: Int) {
    this.flingOffset = flingOffset
    this.offset = offset
  }

  override fun calculateDistanceToFinalSnap(
      layoutManager: LayoutManager,
      targetView: View
  ): IntArray? {
    val out = IntArray(2)
    if (layoutManager.canScrollHorizontally()) {
      out[0] = distanceToStart(targetView, getHorizontalHelper(layoutManager))
    } else {
      out[0] = 0
    }

    if (layoutManager.canScrollVertically()) {
      out[1] = distanceToStart(targetView, getVerticalHelper(layoutManager))
    } else {
      out[1] = 0
    }
    return out
  }

  @Throws(IllegalStateException::class)
  override fun attachToRecyclerView(recyclerView: RecyclerView?) {
    this.recyclerView = recyclerView
    super.attachToRecyclerView(recyclerView)
  }

  override fun findSnapView(layoutManager: LayoutManager): View? {
    if (layoutManager.canScrollVertically()) {
      return findViewClosestToStart(layoutManager, getVerticalHelper(layoutManager))
    } else if (layoutManager.canScrollHorizontally()) {
      return findViewClosestToStart(layoutManager, getHorizontalHelper(layoutManager))
    }
    return null
  }

  override fun findTargetSnapPosition(
      layoutManager: LayoutManager?,
      velocityX: Int,
      velocityY: Int
  ): Int {
    val itemCount: Int = layoutManager?.getItemCount() ?: 0
    if (itemCount == 0 || layoutManager == null) {
      return RecyclerView.NO_POSITION
    }

    val isHorizontal: Boolean = layoutManager.canScrollHorizontally()
    val orientationHelper: OrientationHelper =
        if (isHorizontal) getHorizontalHelper(layoutManager) else getVerticalHelper(layoutManager)
    val firstBeforeStartChild =
        findFirstViewBeforeStart(layoutManager, orientationHelper)
            ?: return RecyclerView.NO_POSITION

    val firstBeforeStartPosition: Int = layoutManager.getPosition(firstBeforeStartChild)
    if (firstBeforeStartPosition == RecyclerView.NO_POSITION) {
      return RecyclerView.NO_POSITION
    }

    val forwardDirection = if (isHorizontal) velocityX > 0 else velocityY > 0

    var reverseLayout = false
    if ((layoutManager is ScrollVectorProvider)) {
      val vectorProvider: ScrollVectorProvider = layoutManager
      val vectorForEnd: PointF? = vectorProvider.computeScrollVectorForPosition(itemCount - 1)
      if (vectorForEnd != null) {
        reverseLayout = vectorForEnd.x < 0 || vectorForEnd.y < 0
      }
    }

    var targetPos =
        if (forwardDirection)
            getForwardTargetPosition(layoutManager, reverseLayout, firstBeforeStartPosition)
        else firstBeforeStartPosition
    if (targetPos < 0) {
      targetPos = 0
    }
    if (targetPos >= itemCount) {
      targetPos = itemCount - 1
    }
    return targetPos
  }

  private fun getForwardTargetPosition(
      layoutManager: LayoutManager,
      reverseLayout: Boolean,
      firstBeforeStartPosition: Int
  ): Int {
    if (layoutManager is GridLayoutManager) {
      return if (reverseLayout) ((firstBeforeStartPosition - layoutManager.spanCount) / flingOffset)
      else ((firstBeforeStartPosition + layoutManager.spanCount) * flingOffset)
    }
    return if (reverseLayout) firstBeforeStartPosition - flingOffset
    else firstBeforeStartPosition + flingOffset
  }

  override fun createScroller(layoutManager: LayoutManager): RecyclerView.SmoothScroller? {
    if (layoutManager !is ScrollVectorProvider) {
      return null
    }
    val recyclerView = checkNotNull(recyclerView)
    return object : LinearSmoothScroller(recyclerView.context) {
      override fun onTargetFound(targetView: View, state: RecyclerView.State, action: Action) {
        calculateDistanceToFinalSnap(checkNotNull(recyclerView.layoutManager), targetView)?.let {
            snapDistances ->
          val dx = snapDistances[0]
          val dy = snapDistances[1]
          val time: Int = calculateTimeForDeceleration(max(abs(dx), abs(dy)))
          if (time > 0) {
            action.update(dx, dy, time, mDecelerateInterpolator)
          }
        }
      }

      override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
        return MILLISECONDS_PER_INCH / displayMetrics.densityDpi
      }

      override fun calculateTimeForScrolling(dx: Int): Int {
        return min(MAX_SCROLL_ON_FLING_DURATION, super.calculateTimeForScrolling(dx))
      }
    }
  }

  private fun distanceToStart(targetView: View, helper: OrientationHelper): Int {
    val childStart: Int = helper.getDecoratedStart(targetView)
    val containerStart: Int = helper.getStartAfterPadding()
    return childStart - containerStart - offset
  }

  private fun getVerticalHelper(layoutManager: LayoutManager): OrientationHelper {
    if (verticalHelper == null || verticalHelperLayoutManager !== layoutManager) {
      verticalHelper = OrientationHelper.createVerticalHelper(layoutManager)
      verticalHelperLayoutManager = layoutManager
    }

    return checkNotNull(verticalHelper)
  }

  private fun getHorizontalHelper(layoutManager: LayoutManager): OrientationHelper {
    if (horizontalHelper == null || horizontalHelperLayoutManager !== layoutManager) {
      horizontalHelper = OrientationHelper.createHorizontalHelper(layoutManager)
      horizontalHelperLayoutManager = layoutManager
    }

    return checkNotNull(horizontalHelper)
  }

  companion object {
    private const val MAX_SCROLL_ON_FLING_DURATION = 100 // ms
    private const val MILLISECONDS_PER_INCH = 100f

    /**
     * Return the child view that is currently closest to the start of this parent.
     *
     * @param layoutManager The [LayoutManager] associated with the attached [ ].
     * @param helper The relevant [OrientationHelper] for the attached [RecyclerView].
     * @return the child view that is currently closest to the start of this parent.
     */
    private fun findViewClosestToStart(
        layoutManager: LayoutManager,
        helper: OrientationHelper
    ): View? {
      val childCount: Int = layoutManager.childCount
      if (childCount == 0) {
        return null
      }

      var closestChild: View? = null
      val start: Int = helper.startAfterPadding
      var absClosest = Int.MAX_VALUE

      for (i in 0 until childCount) {
        val child = layoutManager.getChildAt(i)
        val childStart: Int = helper.getDecoratedStart(child)
        val absDistance = abs(childStart - start)

        /** if child start is closer than previous closest, set it as closest * */
        if (absDistance < absClosest) {
          absClosest = absDistance
          closestChild = child
        }
      }

      return closestChild
    }

    /** @return the first View whose start is before the start of this recycler view */
    private fun findFirstViewBeforeStart(
        layoutManager: LayoutManager,
        helper: OrientationHelper
    ): View? {
      val childCount: Int = layoutManager.childCount
      if (childCount == 0) {
        return null
      }

      var closestChild: View? = null
      val start: Int = helper.startAfterPadding
      var absClosest = Int.MAX_VALUE

      for (i in 0 until childCount) {
        val child: View? = layoutManager.getChildAt(i)
        val childStart: Int = helper.getDecoratedStart(child)
        val absDistance = abs((childStart - start).toDouble()).toInt()

        if (childStart < start && absDistance < absClosest) {
          absClosest = absDistance
          closestChild = child
        }
      }

      return closestChild
    }
  }
}
