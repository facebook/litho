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

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.NestedScrollingChild3
import androidx.core.view.NestedScrollingChildHelper
import com.facebook.litho.LithoRenderTreeView
import com.facebook.rendercore.simplelist.RCRecyclerView.RCRecyclerViewNestedScrollingChild

class LithoZoomableView(context: Context) :
    FrameLayout(context), NestedScrollingChild3, RCRecyclerViewNestedScrollingChild {

  val renderTreeView: LithoRenderTreeView = LithoRenderTreeView(context)

  val nestedScrollingChildHelper: NestedScrollingChildHelper = NestedScrollingChildHelper(this)

  init {
    addView(renderTreeView)
    renderTreeView.clipChildren = false
    renderTreeView.clipToPadding = false
    clipChildren = false
    clipToPadding = false
  }

  public override fun attachViewToParent(
      child: View?,
      index: Int,
      params: ViewGroup.LayoutParams?
  ) {
    super.attachViewToParent(child, index, params)
  }

  public override fun detachViewFromParent(child: View?) {
    super.detachViewFromParent(child)
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    nestedScrollingChildHelper.isNestedScrollingEnabled = true
  }

  override fun onDetachedFromWindow() {
    nestedScrollingChildHelper.isNestedScrollingEnabled = false
    super.onDetachedFromWindow()
  }

  override fun startNestedScroll(axes: Int, type: Int): Boolean {
    return nestedScrollingChildHelper.startNestedScroll(axes, type)
  }

  override fun stopNestedScroll(type: Int) {
    nestedScrollingChildHelper.stopNestedScroll(type)
  }

  override fun hasNestedScrollingParent(type: Int): Boolean {
    return nestedScrollingChildHelper.hasNestedScrollingParent(type)
  }

  override fun dispatchNestedScroll(
      dxConsumed: Int,
      dyConsumed: Int,
      dxUnconsumed: Int,
      dyUnconsumed: Int,
      offsetInWindow: IntArray?,
      type: Int,
      consumed: IntArray
  ) {
    return nestedScrollingChildHelper.dispatchNestedScroll(
        dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type, consumed)
  }

  override fun dispatchNestedScroll(
      dxConsumed: Int,
      dyConsumed: Int,
      dxUnconsumed: Int,
      dyUnconsumed: Int,
      offsetInWindow: IntArray?,
      type: Int
  ): Boolean {
    return nestedScrollingChildHelper.dispatchNestedScroll(
        dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type)
  }

  override fun dispatchNestedPreScroll(
      dx: Int,
      dy: Int,
      consumed: IntArray?,
      offsetInWindow: IntArray?,
      type: Int
  ): Boolean {
    return nestedScrollingChildHelper.dispatchNestedPreScroll(
        dx, dy, consumed, offsetInWindow, type)
  }

  override fun dispatchNestedFling(velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
    return nestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed)
  }

  override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
    return nestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY)
  }
}
