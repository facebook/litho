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
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.ViewTreeObserver
import androidx.annotation.ColorInt
import androidx.core.view.OneShotPreDrawListener
import androidx.core.widget.NestedScrollView
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.litho.BaseMountingView
import com.facebook.litho.ComponentTree
import com.facebook.litho.HasLithoViewChildren
import com.facebook.litho.LayoutState
import com.facebook.litho.LithoMetadataExceptionWrapper
import com.facebook.litho.LithoRenderTreeView
import com.facebook.litho.LithoView
import com.facebook.litho.TreeState
import com.facebook.rendercore.ErrorReporter
import com.facebook.rendercore.LogLevel
import com.facebook.rendercore.utils.CommonUtils

/**
 * Extension of [NestedScrollView] that allows to add more features needed for [VerticalScrollSpec].
 */
class LithoScrollView
@JvmOverloads
constructor(
    context: Context,
    val renderTreeView: BaseMountingView = LithoView(context),
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    NestedScrollView(ContextThemeWrapper(context, R.style.LithoScrollView), attrs, defStyleAttr),
    HasLithoViewChildren {

  private var scrollPosition: ScrollPosition? = null
  private var onPreDrawListener: ViewTreeObserver.OnPreDrawListener? = null
  private var onInterceptTouchListener: OnInterceptTouchListener? = null
  private var scrollStateDetector: ScrollStateDetector? = null

  private var currentRootComponent: String? = null
  private var currentLogTag: String? = null
  @ColorInt private var fadingEdgeColor: Int? = null

  init {
    addView(renderTreeView)
  }

  @JvmOverloads
  constructor(
      context: Context,
      attrs: AttributeSet?,
      defStyleAttr: Int = 0
  ) : this(context, LithoView(context), attrs, defStyleAttr)

  override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
    val result = onInterceptTouchListener?.onInterceptTouch(this, ev) ?: false
    return result || super.onInterceptTouchEvent(ev)
  }

  override fun fling(velocityX: Int) {
    super.fling(velocityX)
    scrollStateDetector?.fling()
  }

  override fun draw(canvas: Canvas) {
    try {
      super.draw(canvas)
      scrollStateDetector?.onDraw()
    } catch (t: Throwable) {
      ErrorReporter.report(
          LogLevel.ERROR,
          "LITHO:NPE:LITHO_SCROLL_VIEW_DRAW",
          "Root component: $currentRootComponent",
          t,
          0,
          null)
      throw LithoMetadataExceptionWrapper(null, currentRootComponent, currentLogTag, t)
    }
  }

  override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
    super.onScrollChanged(l, t, oldl, oldt)

    renderTreeView.notifyVisibleBoundsChanged()

    scrollPosition?.y = scrollY
    scrollStateDetector?.onScrollChanged()
  }

  override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
    val isConsumed = super.onTouchEvent(motionEvent)

    scrollStateDetector?.onTouchEvent(motionEvent)

    return isConsumed
  }

  /**
   * NestedScrollView does not automatically consume the fling event. However, RecyclerView consumes
   * this event if it's either vertically or horizontally scrolling. [RecyclerView.fling] Since this
   * view is specifically made for vertically scrolling components, we always consume the nested
   * fling event just like recycler view.
   */
  override fun dispatchNestedFling(velocityX: Float, velocityY: Float, consumed: Boolean): Boolean =
      super.dispatchNestedFling(velocityX, velocityY, true)

  fun setOnInterceptTouchListener(onInterceptTouchListener: OnInterceptTouchListener?) {
    this.onInterceptTouchListener = onInterceptTouchListener
  }

  override fun obtainLithoViewChildren(lithoViews: MutableList<BaseMountingView>) {
    lithoViews.add(renderTreeView)
  }

  fun setScrollStateListener(scrollStateListener: ScrollStateListener?) {
    if (scrollStateListener != null) {
      val detector =
          scrollStateDetector ?: ScrollStateDetector(this).also { scrollStateDetector = it }
      detector.setListener(scrollStateListener)
    } else {
      scrollStateDetector?.setListener(null)
    }
  }

  fun setScrollPosition(scrollPosition: ScrollPosition?) {
    if (scrollPosition != null) {
      onPreDrawListener = OneShotPreDrawListener.add(this) { scrollY = scrollPosition.y }
    } else {
      scrollY = 0
      viewTreeObserver.removeOnPreDrawListener(onPreDrawListener)
      onPreDrawListener = null
    }
  }

  fun mount(layoutState: LayoutState?, state: TreeState?) {
    if (layoutState != null && state != null && renderTreeView is LithoRenderTreeView) {
      currentRootComponent = layoutState.rootName
      currentLogTag = layoutState.componentContext.logTag
      renderTreeView.setLayoutState(layoutState, state)
    }
  }

  fun mount(
      contentComponentTree: ComponentTree?,
      scrollPosition: ScrollPosition,
      scrollStateListener: ScrollStateListener?
  ) {
    if (renderTreeView !is LithoView) {
      throw UnsupportedOperationException("API can only be invoked from Vertical Scroll Spec")
    }

    if (contentComponentTree != null) {
      val component = contentComponentTree.root
      currentRootComponent = component?.simpleName ?: "null"
      currentLogTag = contentComponentTree.logTag
    }
    renderTreeView.componentTree = contentComponentTree

    this.scrollPosition = scrollPosition
    onPreDrawListener = OneShotPreDrawListener.add(this) { scrollY = scrollPosition.y }
    if (scrollStateListener != null) {
      val detector =
          scrollStateDetector ?: ScrollStateDetector(this).also { scrollStateDetector = it }
      detector.setListener(scrollStateListener)
    }
  }

  fun unmount() {
    if (renderTreeView !is LithoView) {
      throw UnsupportedOperationException("API can only be invoked from Vertical Scroll Spec")
    }

    renderTreeView.setComponentTree(null, false)

    scrollPosition = null
    viewTreeObserver.removeOnPreDrawListener(onPreDrawListener)
    onPreDrawListener = null
    scrollStateDetector?.setListener(null)
  }

  fun release() {
    if (renderTreeView is LithoRenderTreeView) {
      renderTreeView.cleanup()
    } else {
      throw UnsupportedOperationException(
          "This operation is only support for LithoRenderTreeView but it was : ${CommonUtils.getSectionNameForTracing(renderTreeView.javaClass)}")
    }
  }

  fun setFadingEdgeColor(@ColorInt edgeColor: Int?) {
    fadingEdgeColor = edgeColor
  }

  override fun getSolidColor(): Int = fadingEdgeColor ?: super.getSolidColor()

  @DataClassGenerate data class ScrollPosition(@JvmField var y: Int = 0)

  fun interface OnInterceptTouchListener {
    fun onInterceptTouch(nestedScrollView: NestedScrollView, event: MotionEvent): Boolean
  }
}
