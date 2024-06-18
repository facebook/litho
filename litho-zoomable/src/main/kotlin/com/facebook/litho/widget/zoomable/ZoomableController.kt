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
import android.graphics.Color
import android.graphics.PointF
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import com.facebook.litho.findComponentActivity
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringConfig
import com.facebook.rebound.SpringListener
import com.facebook.rebound.SpringSystem

/** Most logic is from existing rendercore class [ZoomableViewController] */
class ZoomableController(
    private val context: Context,
    private val backgroundColor: Int = BLACK_TRANSPARENT_80,
    private val backgroundDrawable: Drawable? = null
) : SpringListener {

  private enum class State {
    IDLE,
    ANIMATING,
    REMOTE_ZOOMING
  }

  companion object {
    const val DEFAULT_MAX_SCALE_FACTOR = 4f
    const val DEFAULT_MIN_SCALE_FACTOR = 1f
    private const val SNAPPING_DISTANCE_IN_POINTS = 4f
    private const val SPRING_TENSION = 90.0
    private const val SPRING_FRICTION = 10.0
    private val BLACK_TRANSPARENT_80 = Color.parseColor("#CC000000")
    private val LAYOUT_PARAMS: FrameLayout.LayoutParams =
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
  }

  internal var interceptingTouch = false

  // As we're reusing the same ScaleGestureDetector from when the zoom initiated, the initial pivot
  // points will have values respective to the source view's coordinate system (left corner of
  // source view is the origin). Then as we intercept touches, the pivot points will be respective
  // to the zoom view container's coordinate system (left corner of the screen is the origin,
  // including the status bar). Hence, we need to adjust the pivot points' y-values by the
  // difference so that we avoid a sudden jump of the view in the y-axis.
  private var touchAdjustment = 0
  private var initialTranslationY = 0f
  private var contentViewIndex = 0
  private var contentViewLayoutParams: ViewGroup.LayoutParams? = null

  private var currentScaleFactor = 1f
  private var state = State.IDLE
  private var dragOffsetX = 0f
  private var dragOffsetY = 0f

  private var rootView: LithoZoomableView? = null
  private var remoteContainer: LithoRemoteContainerView? = null
  private var contentView: View? = null

  private var zoomSpring: Spring? = null
  private var panXSpring: Spring? = null
  private var panYSpring: Spring? = null

  private val decorView: ViewGroup =
      checkNotNull(context.findComponentActivity()).window.decorView as ViewGroup
  private val snappingDelta = SNAPPING_DISTANCE_IN_POINTS.dpToPx()
  private val pivotPoint: PointF = PointF()
  private val maxScaleFactor: Float = DEFAULT_MAX_SCALE_FACTOR

  private val scaleListener =
      object : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScale(detector: ScaleGestureDetector): Boolean {

          if (state == State.REMOTE_ZOOMING) {
            val focusX = detector.focusX
            val focusY = detector.focusY
            val offsetX: Float = focusX - pivotPoint.x
            val offsetY: Float = focusY - pivotPoint.y
            dragOffsetX += offsetX
            dragOffsetY += offsetY
            requirePanXSpring().setCurrentValue(dragOffsetX.toDouble(), true)
            requirePanYSpring().setCurrentValue(dragOffsetY.toDouble(), true)

            updateTranslation(dragOffsetX, dragOffsetY)
            updatePivotPoint(focusX, focusY)

            var accumulatedScaleFactor: Float = (currentScaleFactor * detector.scaleFactor)

            if ((accumulatedScaleFactor > maxScaleFactor &&
                accumulatedScaleFactor > currentScaleFactor) ||
                (accumulatedScaleFactor < DEFAULT_MIN_SCALE_FACTOR &&
                    accumulatedScaleFactor < currentScaleFactor)) {
              // Constrain the scale if it's going over the limits
              accumulatedScaleFactor =
                  (currentScaleFactor + (accumulatedScaleFactor - currentScaleFactor) * .5f)
            }

            currentScaleFactor = accumulatedScaleFactor

            requireZoomSpring().currentValue = accumulatedScaleFactor.toDouble()
            updateScaleFactor(currentScaleFactor)
            return true
          }
          return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
          if (state == State.IDLE) {
            startZoom()
            pivotPoint.x = detector.focusX
            pivotPoint.y =
                if (interceptingTouch) detector.focusY - touchAdjustment else detector.focusY
            return true
          }
          return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) = Unit
      }

  private val zoomGestureDetector =
      ScaleGestureDetector(context, scaleListener).apply { isQuickScaleEnabled = false }

  fun init() {
    val springSystem = SpringSystem.create()
    zoomSpring =
        springSystem
            .createSpring()
            .setSpringConfig(
                SpringConfig.fromOrigamiTensionAndFriction(SPRING_TENSION, SPRING_FRICTION))
            .apply { currentValue = DEFAULT_MIN_SCALE_FACTOR.toDouble() }
    panXSpring =
        springSystem
            .createSpring()
            .setSpringConfig(
                SpringConfig.fromOrigamiTensionAndFriction(SPRING_TENSION, SPRING_FRICTION))
    panYSpring =
        springSystem
            .createSpring()
            .setSpringConfig(
                SpringConfig.fromOrigamiTensionAndFriction(SPRING_TENSION, SPRING_FRICTION))
  }

  fun bindTo(view: LithoZoomableView?) {
    rootView = view
  }

  fun unbind() {
    rootView = null
    remoteContainer = null
    requireZoomSpring().removeListener(this)
    requirePanXSpring().removeListener(this)
    requirePanYSpring().removeListener(this)
    state = State.IDLE
  }

  private fun createRemoteContainer() {
    initialTranslationY = requireRootView().renderTreeView.translationY

    val location = IntArray(2)
    requireRootView().renderTreeView.getLocationInWindow(location)
    touchAdjustment = location[1]

    val remoteContainer = LithoRemoteContainerView(context)
    remoteContainer.layoutParams = LAYOUT_PARAMS
    if (backgroundDrawable != null) {
      remoteContainer.background = backgroundDrawable
    } else {
      ViewCompat.setBackground(remoteContainer, ColorDrawable(backgroundColor))
    }
    remoteContainer.visibility = View.GONE
    decorView.addView(remoteContainer)
    this.remoteContainer = remoteContainer
  }

  private fun requireRootView(): LithoZoomableView = checkNotNull(rootView)

  private fun requireRemoteContainer(): LithoRemoteContainerView = checkNotNull(remoteContainer)

  private fun requireContentView(): View =
      if (state == State.REMOTE_ZOOMING) {
        checkNotNull(contentView)
      } else {
        requireRootView().renderTreeView
      }

  private fun requireZoomSpring(): Spring = checkNotNull(zoomSpring)

  private fun requirePanXSpring(): Spring = checkNotNull(panXSpring)

  private fun requirePanYSpring(): Spring = checkNotNull(panYSpring)

  private fun startZoom() {
    createRemoteContainer()
    state = State.REMOTE_ZOOMING
    interceptingTouch = true

    val rootView = requireRootView()
    val remoteContainer = requireRemoteContainer()
    val contentView = rootView.renderTreeView

    contentViewIndex = rootView.indexOfChild(contentView)
    contentViewLayoutParams = contentView.layoutParams

    rootView.detachViewFromParent(contentView)
    remoteContainer.attachViewToParent(contentView, 0, LAYOUT_PARAMS)
    remoteContainer.bringToFront()
    rootView.requestLayout()
    rootView.invalidate()

    remoteContainer.visibility = View.VISIBLE
    this.contentView = contentView

    rootView.parent.requestDisallowInterceptTouchEvent(true)
    rootView.requestDisallowInterceptTouchEvent(true)
    remoteContainer.parent.requestDisallowInterceptTouchEvent(true)
    requireContentView().setHasTransientState(true)
  }

  private fun finishMovement() {
    interceptingTouch = false
    // We can settle
    val contentView = requireContentView()
    val zoomSpring = requireZoomSpring()
    val panXSpring = requirePanXSpring()
    val panYSpring = requirePanYSpring()
    updatePivotPoint(contentView.width / 2f, contentView.height / 2f)
    zoomSpring.setCurrentValue(currentScaleFactor.toDouble(), true)
    panXSpring.setCurrentValue(dragOffsetX.toDouble(), true)
    panYSpring.setCurrentValue(dragOffsetY.toDouble(), true)
    zoomSpring.removeListener(this)
    panXSpring.removeListener(this)
    panYSpring.removeListener(this)
    contentView.setHasTransientState(false)
    updateTranslation(0f, initialTranslationY)
    dragOffsetX = 0f
    dragOffsetY = 0f
    contentViewIndex = 0
    contentViewLayoutParams = null
    state = State.IDLE
  }

  private fun animateTo(scaleFactor: Double, panX: Double, panY: Double) {
    state = State.ANIMATING
    interceptingTouch = false
    val zoomSpring = requireZoomSpring()
    val panXSpring = requirePanXSpring()
    val panYSpring = requirePanYSpring()
    zoomSpring.addListener(this)
    panXSpring.addListener(this)
    panYSpring.addListener(this)
    zoomSpring.endValue = scaleFactor
    panXSpring.endValue = panX
    panYSpring.endValue = panY
    if (zoomSpring.isAtRest && panXSpring.isAtRest && panYSpring.isAtRest) {
      finishMovement()
    }
  }

  public fun resetZoom() {
    val (panX, panY) = maxOffsetXYForScale(DEFAULT_MIN_SCALE_FACTOR)
    animateTo(DEFAULT_MIN_SCALE_FACTOR.toDouble(), panX.toDouble(), panY.toDouble())
  }

  private fun updatePivotPoint(pivotX: Float, pivotY: Float) {
    pivotPoint.x = pivotX
    pivotPoint.y = pivotY
  }

  private fun updateTranslation(translationX: Float, translationY: Float) {
    val contentView = requireContentView()
    contentView.translationX = translationX
    contentView.translationY = translationY
  }

  private fun updateScaleFactor(scaleFactor: Float) {
    val contentView = requireContentView()
    var scaleFactorToApply = scaleFactor
    if (java.lang.Float.isNaN(scaleFactorToApply)) {
      scaleFactorToApply = 1.0f
    }
    contentView.scaleX = scaleFactorToApply
    contentView.scaleY = scaleFactorToApply
  }

  private fun maxOffsetXYForScale(scale: Float): Pair<Float, Float> {
    val contentView = requireContentView()

    val maxAllowedOffsetX = (scale * contentView.width - contentView.width) / 2f
    val maxAllowedOffsetY = (scale * contentView.height - contentView.height) / 2f
    var dragX = dragOffsetX.coerceIn(-maxAllowedOffsetX..maxAllowedOffsetX)
    var dragY = dragOffsetY.coerceIn(-maxAllowedOffsetY..maxAllowedOffsetY)

    if (dragX in -maxAllowedOffsetX..(-maxAllowedOffsetX + snappingDelta)) {
      dragX = -maxAllowedOffsetX
    }
    if (dragX in (maxAllowedOffsetX - snappingDelta)..maxAllowedOffsetX) {
      dragX = maxAllowedOffsetX
    }

    if (dragY in -maxAllowedOffsetY..(-maxAllowedOffsetY + snappingDelta)) {
      dragY = -maxAllowedOffsetY
    }
    if (dragY in (maxAllowedOffsetY - snappingDelta)..maxAllowedOffsetY) {
      dragY = maxAllowedOffsetY
    }
    return Pair(dragX, dragY)
  }

  /** Spring Listener */
  override fun onSpringUpdate(spring: Spring) {
    if (state == State.ANIMATING) {
      currentScaleFactor = requireZoomSpring().currentValue.toFloat()
      updateScaleFactor(currentScaleFactor)
      dragOffsetX = requirePanXSpring().currentValue.toFloat()
      dragOffsetY = requirePanYSpring().currentValue.toFloat()
      updateTranslation(dragOffsetX, dragOffsetY)
    }
  }

  override fun onSpringAtRest(spring: Spring) {
    if (state == State.REMOTE_ZOOMING || state == State.ANIMATING) {
      interceptingTouch = false

      val rootView = requireRootView()
      val remoteContainer = requireRemoteContainer()
      val contentView = requireContentView()

      rootView.parent.requestDisallowInterceptTouchEvent(false)
      rootView.requestDisallowInterceptTouchEvent(false)
      remoteContainer.parent.requestDisallowInterceptTouchEvent(false)
      contentView.setHasTransientState(false)
      rootView.stopNestedScroll(ViewCompat.TYPE_TOUCH)

      remoteContainer.detachViewFromParent(contentView)
      rootView.attachViewToParent(contentView, contentViewIndex, contentViewLayoutParams)
      contentView.requestLayout()

      decorView.removeView(this.remoteContainer)
      this.contentView = contentView
      this.remoteContainer = null
      state == State.IDLE
    }
    finishMovement()
  }

  override fun onSpringActivate(spring: Spring) = Unit

  override fun onSpringEndStateChange(spring: Spring?) = Unit

  /** Touch Listener */
  fun onTouch(event: MotionEvent, parent: ViewParent?): Boolean {
    zoomGestureDetector.onTouchEvent(event)

    when (event.actionMasked) {
      MotionEvent.ACTION_UP,
      MotionEvent.ACTION_CANCEL -> {
        if (state == State.REMOTE_ZOOMING) {
          val (panX, panY) = maxOffsetXYForScale(DEFAULT_MIN_SCALE_FACTOR)
          animateTo(
              DEFAULT_MIN_SCALE_FACTOR.toDouble(),
              panX.toDouble(),
              panY.toDouble() + touchAdjustment)
        }
      }
    }

    return true
  }

  private fun Float.dpToPx(): Float = (this * context.resources.displayMetrics.density)
}
