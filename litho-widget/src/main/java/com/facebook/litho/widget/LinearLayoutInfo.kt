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
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import com.facebook.litho.ComponentContext
import com.facebook.litho.SizeSpec
import com.facebook.litho.SizeSpec.makeSizeSpec
import com.facebook.litho.widget.LayoutInfo.RenderInfoCollection
import kotlin.math.ceil

/** An implementation for [LayoutInfo] to implement linear lists with a [LinearLayoutManager]. */
open class LinearLayoutInfo(private val linearLayoutManager: LinearLayoutManager) : LayoutInfo {

  @JvmOverloads
  constructor(
      context: ComponentContext?,
      orientation: Int,
      reverseLayout: Boolean,
      stackFromEnd: Boolean = false
  ) : this(context?.androidContext, orientation, reverseLayout, stackFromEnd)

  @JvmOverloads
  constructor(
      context: Context?,
      orientation: Int,
      reverseLayout: Boolean,
      stackFromEnd: Boolean = false
  ) : this(InternalLinearLayoutManager(context, orientation, reverseLayout, stackFromEnd)) {
    linearLayoutManager.isMeasurementCacheEnabled = false
  }

  override fun getScrollDirection(): Int = linearLayoutManager.orientation

  override fun findFirstVisibleItemPosition(): Int =
      linearLayoutManager.findFirstVisibleItemPosition()

  override fun findLastVisibleItemPosition(): Int =
      linearLayoutManager.findLastVisibleItemPosition()

  override fun findFirstFullyVisibleItemPosition(): Int =
      linearLayoutManager.findFirstCompletelyVisibleItemPosition()

  override fun findLastFullyVisibleItemPosition(): Int =
      linearLayoutManager.findLastCompletelyVisibleItemPosition()

  override fun getItemCount(): Int = linearLayoutManager.getItemCount()

  override fun getLayoutManager(): RecyclerView.LayoutManager = linearLayoutManager

  override fun setRenderInfoCollection(renderInfoCollection: RenderInfoCollection?) {
    // Do nothing
  }

  override fun scrollToPositionWithOffset(position: Int, offset: Int) {
    linearLayoutManager.scrollToPositionWithOffset(position, offset)
  }

  override fun approximateRangeSize(
      firstMeasuredItemWidth: Int,
      firstMeasuredItemHeight: Int,
      recyclerMeasuredWidth: Int,
      recyclerMeasuredHeight: Int
  ): Int {
    var approximateRange: Int

    approximateRange =
        when (linearLayoutManager.orientation) {
          LinearLayoutManager.HORIZONTAL ->
              ceil((recyclerMeasuredWidth.toFloat() / firstMeasuredItemWidth.toFloat()).toDouble())
                  .toInt()

          else ->
              ceil(
                      (recyclerMeasuredHeight.toFloat() / firstMeasuredItemHeight.toFloat())
                          .toDouble())
                  .toInt()
        }

    if (approximateRange < MIN_SANE_RANGE) {
      approximateRange = MIN_SANE_RANGE
    } else if (approximateRange > MAX_SANE_RANGE) {
      approximateRange = MAX_SANE_RANGE
    }

    return approximateRange
  }

  override fun getChildHeightSpec(heightSpec: Int, renderInfo: RenderInfo): Int {
    return when (linearLayoutManager.orientation) {
      LinearLayoutManager.HORIZONTAL -> heightSpec
      else -> makeSizeSpec(0, SizeSpec.UNSPECIFIED)
    }
  }

  override fun getChildWidthSpec(widthSpec: Int, renderInfo: RenderInfo): Int {
    return when (linearLayoutManager.orientation) {
      LinearLayoutManager.HORIZONTAL -> makeSizeSpec(0, SizeSpec.UNSPECIFIED)
      else -> widthSpec
    }
  }

  override fun createViewportFiller(measuredWidth: Int, measuredHeight: Int): ViewportFiller? =
      ViewportFiller(measuredWidth, measuredHeight, getScrollDirection())

  override fun computeWrappedHeight(
      maxHeight: Int,
      componentTreeHolders: MutableList<ComponentTreeHolder>
  ): Int =
      LayoutInfoUtils.computeLinearLayoutWrappedHeight(
          linearLayoutManager, maxHeight, componentTreeHolders)

  private class InternalLinearLayoutManager(
      context: Context?,
      orientation: Int,
      reverseLayout: Boolean,
      stackFromEnd: Boolean
  ) : LinearLayoutManager(context, orientation, reverseLayout) {
    init {
      setStackFromEnd(stackFromEnd)
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams =
        if (orientation == OrientationHelper.VERTICAL)
            RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        else
            RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)

    override fun supportsPredictiveItemAnimations(): Boolean =
        // Predictive animation in RecyclerView has some bugs that surface occasionally when one
        // RecyclerView is used as a child of another RecyclerView and it is very hard to repro
        // (https://issuetracker.google.com/issues/37007605). This one disables that optional
        // feature for Horizontal one which is used as inner RecyclerView most of the cases.
        // Disabling predictive animations lets RecyclerView use the simple animations instead.
        if (orientation == OrientationHelper.HORIZONTAL) false
        else super.supportsPredictiveItemAnimations()
  }

  /**
   * Simple implementation of [LayoutInfo.ViewportFiller] which fills the viewport by laying out
   * items sequentially.
   */
  class ViewportFiller(
      private val width: Int,
      private val height: Int,
      private val orientation: Int
  ) : LayoutInfo.ViewportFiller {
    private var _fill = 0

    override fun wantsMore(): Boolean {
      val target = if (orientation == OrientationHelper.VERTICAL) height else width
      return _fill < target
    }

    override fun add(renderInfo: RenderInfo, width: Int, height: Int) {
      _fill += if (orientation == OrientationHelper.VERTICAL) height else width
    }

    override fun getFill(): Int = _fill
  }

  companion object {
    private const val MAX_SANE_RANGE = 10
    private const val MIN_SANE_RANGE = 2
  }
}
