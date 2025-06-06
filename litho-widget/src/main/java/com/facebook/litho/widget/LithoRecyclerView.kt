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
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.widget.collection.RecyclerKeyboardEventsHelper.dispatchKeyEvent

/** Extension of [RecyclerView] that allows to add more features needed for [Recycler] */
open class LithoRecyclerView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    RecyclerView(context, attrs, defStyle), HasPostDispatchDrawListener {

  /**
   * Listener that will be called before RecyclerView and LayoutManager layout is called.
   *
   * This is generally not recommended to be used unless you really know what you're doing because
   * it will be called on every layout pass. Other callbacks like onDataBound and onDataRendered may
   * be more appropriate.
   */
  fun interface OnBeforeLayoutListener {
    fun onBeforeLayout(recyclerView: RecyclerView)
  }

  /**
   * Listener that will be called after RecyclerView and LayoutManager layout is called.
   *
   * This is generally not recommended to be used unless you really know what you're doing because
   * it will be called on every layout pass. Other callbacks like onDataBound and onDataRendered may
   * be more appropriate. It is a good time to invoke code which needs to respond to child sizing
   * changes.
   */
  fun interface OnAfterLayoutListener {
    fun onAfterLayout(recyclerView: RecyclerView)
  }

  private var leftFadingEnabled = true
  private var rightFadingEnabled = true
  private var topFadingEnabled = true
  private var bottomFadingEnabled = true
  private var touchInterceptor: TouchInterceptor? = null
  private var postDispatchDrawListeners: MutableList<PostDispatchDrawListener>? = null
  private var onBeforeLayoutListener: OnBeforeLayoutListener? = null
  private var onAfterLayoutListener: OnAfterLayoutListener? = null

  init {
    setTag(com.facebook.rendercore.R.id.rc_pooling_container, true)
  }

  /**
   * Set TouchInterceptor that will be used in [onInterceptTouchEvent] to determine how touch events
   * should be intercepted by this [RecyclerView]
   */
  open fun setTouchInterceptor(touchInterceptor: TouchInterceptor?) {
    this.touchInterceptor = touchInterceptor
  }

  fun setOnBeforeLayoutListener(onBeforeLayoutListener: OnBeforeLayoutListener?) {
    this.onBeforeLayoutListener = onBeforeLayoutListener
  }

  fun setOnAfterLayoutListener(onAfterLayoutListener: OnAfterLayoutListener?) {
    this.onAfterLayoutListener = onAfterLayoutListener
  }

  override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
    val touchInterceptor = touchInterceptor ?: return super.onInterceptTouchEvent(ev)
    val result = touchInterceptor.onInterceptTouchEvent(this, ev)
    return when (result) {
      TouchInterceptor.Result.INTERCEPT_TOUCH_EVENT -> true
      TouchInterceptor.Result.IGNORE_TOUCH_EVENT -> false
      TouchInterceptor.Result.CALL_SUPER -> super.onInterceptTouchEvent(ev)
    }
  }

  override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
    if (!ComponentsConfiguration.enableKeyboardNavigationForHScroll) {
      return super.dispatchKeyEvent(event)
    }

    // Let child to dispatch first, then handle ours if child didn't do it.
    if (super.dispatchKeyEvent(event)) {
      return true
    }

    return event != null && dispatchKeyEvent(this, event)
  }

  public override fun dispatchDraw(canvas: Canvas) {
    super.dispatchDraw(canvas)
    postDispatchDrawListeners?.forEach { it.postDispatchDraw(childCount) }
  }

  override fun registerPostDispatchDrawListener(listener: PostDispatchDrawListener) {
    val listeners =
        postDispatchDrawListeners
            ?: ArrayList<PostDispatchDrawListener>().also { postDispatchDrawListeners = it }
    listeners.add(listener)
  }

  override fun unregisterPostDispatchDrawListener(listener: PostDispatchDrawListener) {
    postDispatchDrawListeners?.remove(listener)
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    onBeforeLayoutListener?.onBeforeLayout(this)
    super.onLayout(changed, l, t, r, b)
    onAfterLayoutListener?.onAfterLayout(this)
  }

  fun setLeftFadingEnabled(leftFadingEnabled: Boolean) {
    this.leftFadingEnabled = leftFadingEnabled
  }

  fun setRightFadingEnabled(rightFadingEnabled: Boolean) {
    this.rightFadingEnabled = rightFadingEnabled
  }

  fun setTopFadingEnabled(topFadingEnabled: Boolean) {
    this.topFadingEnabled = topFadingEnabled
  }

  fun setBottomFadingEnabled(bottomFadingEnabled: Boolean) {
    this.bottomFadingEnabled = bottomFadingEnabled
  }

  public override fun getLeftFadingEdgeStrength(): Float {
    return if (leftFadingEnabled) {
      super.getLeftFadingEdgeStrength()
    } else {
      0f
    }
  }

  public override fun getRightFadingEdgeStrength(): Float {
    return if (rightFadingEnabled) {
      super.getRightFadingEdgeStrength()
    } else {
      0f
    }
  }

  public override fun getTopFadingEdgeStrength(): Float {
    return if (topFadingEnabled) {
      super.getTopFadingEdgeStrength()
    } else {
      0f
    }
  }

  public override fun getBottomFadingEdgeStrength(): Float {
    return if (bottomFadingEnabled) {
      super.getBottomFadingEdgeStrength()
    } else {
      0f
    }
  }

  /** Allows to override [onInterceptTouchEvent] behavior */
  fun interface TouchInterceptor {
    enum class Result {
      /** Return true without calling `super.onInterceptTouchEvent()` */
      INTERCEPT_TOUCH_EVENT,
      /** Return false without calling `super.onInterceptTouchEvent()` */
      IGNORE_TOUCH_EVENT,
      /** Returns `super.onInterceptTouchEvent()` */
      CALL_SUPER,
    }

    /** Called from [.onInterceptTouchEvent] to determine how touch events should be intercepted */
    fun onInterceptTouchEvent(recyclerView: RecyclerView, ev: MotionEvent): Result
  }
}
