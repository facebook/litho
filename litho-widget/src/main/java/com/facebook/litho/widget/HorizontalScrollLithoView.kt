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
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import android.widget.HorizontalScrollView
import androidx.core.view.doOnPreDraw
import com.facebook.litho.BaseMountingView
import com.facebook.litho.ComponentTree
import com.facebook.litho.HasLithoViewChildren
import com.facebook.litho.LayoutState
import com.facebook.litho.LithoRenderTreeView
import com.facebook.litho.LithoView
import com.facebook.litho.TreeState

/**
 * Extension of [HorizontalScrollView] that allows to add more features needed for
 * [HorizontalScrollSpec] and [ExperimentalHorizontalScroll] primitive component.
 */
class HorizontalScrollLithoView
@JvmOverloads
constructor(context: Context, val renderTreeView: BaseMountingView = LithoView(context)) :
    HorizontalScrollView(context), HasLithoViewChildren {

  private var componentWidth = 0
  private var componentHeight = 0

  private var scrollPosition: ScrollPosition? = null
  private var onScrollChangeListener: OnScrollChangeListener? = null
  private var scrollStateDetector: ScrollStateDetector? = null

  init {
    addView(renderTreeView)
  }

  override fun fling(velocityX: Int) {
    super.fling(velocityX)
    scrollStateDetector?.fling()
  }

  override fun draw(canvas: Canvas) {
    super.draw(canvas)
    scrollStateDetector?.onDraw()
  }

  override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
    super.onScrollChanged(l, t, oldl, oldt)

    // We need to notify LithoView about the visibility bounds that has changed when View is
    // scrolled so that correct visibility events are fired for the child components of
    // HorizontalScroll.
    renderTreeView.notifyVisibleBoundsChanged()

    scrollPosition?.let { position ->
      onScrollChangeListener?.onScrollChange(this, scrollX, position.x)
      position.x = scrollX
    }

    scrollStateDetector?.onScrollChanged()
  }

  override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
    val isConsumed = super.onTouchEvent(motionEvent)

    scrollStateDetector?.onTouchEvent(motionEvent)

    return isConsumed
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    // The hosting component view always matches the component size. This will
    // ensure that there will never be a size-mismatch between the view and the
    // component-based content, which would trigger a layout pass in the
    // UI thread.
    renderTreeView.measure(
        MeasureSpec.makeMeasureSpec(componentWidth, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(componentHeight, MeasureSpec.EXACTLY))

    // The mounted view always gets exact dimensions from the framework.
    setMeasuredDimension(
        MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
  }

  override fun obtainLithoViewChildren(lithoViews: MutableList<BaseMountingView>) {
    lithoViews.add(renderTreeView)
  }

  fun mount(layoutState: LayoutState?, state: TreeState?) {
    if (layoutState != null && state != null && renderTreeView is LithoRenderTreeView) {
      renderTreeView.setLayoutState(layoutState, state)
    }
  }

  fun mount(
      componentTree: ComponentTree,
      scrollPosition: ScrollPosition,
      width: Int,
      height: Int,
      scrollX: Int,
      onScrollChangeListener: OnScrollChangeListener?,
      scrollStateListener: ScrollStateListener?
  ) {
    if (renderTreeView !is LithoView) {
      throw UnsupportedOperationException("API can only be invoked from Horizontal Scroll Spec")
    }
    renderTreeView.componentTree = componentTree
    this.scrollPosition = scrollPosition
    this.onScrollChangeListener = onScrollChangeListener
    componentWidth = width
    componentHeight = height
    setScrollX(scrollX)
    if (scrollStateListener != null) {
      val detector =
          scrollStateDetector ?: ScrollStateDetector(this).also { scrollStateDetector = it }
      detector.setListener(scrollStateListener)
    }
  }

  fun unmount() {
    when (renderTreeView) {
      is LithoRenderTreeView -> renderTreeView.cleanup()
      is LithoView -> renderTreeView.setComponentTree(null, false)
    }
    componentWidth = 0
    componentHeight = 0
    scrollPosition = null
    onScrollChangeListener = null
    scrollX = 0
    scrollStateDetector?.setListener(null)
  }

  fun setScrollPosition(scrollPosition: ScrollPosition?) {
    this.scrollPosition = scrollPosition
    doOnPreDraw {
      val position = this.scrollPosition ?: return@doOnPreDraw
      if (position.x == LAST_SCROLL_POSITION_UNSET) {
        fullScroll(FOCUS_RIGHT)
        position.x = scrollX
      } else {
        scrollX = position.x
      }
    }
  }

  fun setOnScrollChangeListener(onScrollChangeListener: OnScrollChangeListener?) {
    this.onScrollChangeListener = onScrollChangeListener
  }

  fun setScrollStateListener(scrollStateListener: ScrollStateListener?) {
    if (scrollStateListener != null) {
      val detector =
          scrollStateDetector ?: ScrollStateDetector(this).also { scrollStateDetector = it }
      detector.setListener(scrollStateListener)
    }
  }

  class ScrollPosition(@JvmField var x: Int)

  /** Scroll change listener invoked when the scroll position changes. */
  fun interface OnScrollChangeListener {
    fun onScrollChange(v: View, scrollX: Int, oldScrollX: Int)
  }

  companion object {
    private const val LAST_SCROLL_POSITION_UNSET = -1
  }
}
