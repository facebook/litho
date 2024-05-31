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
import android.graphics.PointF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import android.view.ViewParent
import android.widget.OverScroller
import androidx.core.view.ViewCompat
import com.facebook.common.sdk34workaround.SimpleOnGestureListenerWorkaround
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringConfig
import com.facebook.rebound.SpringListener
import com.facebook.rebound.SpringSystem
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sign

/** Most logic is from existing rendercore class [ZoomableViewController] */
class ZoomableController(
    private val context: Context,
    private val isDoubleTapEnabled: Boolean = false
) : SpringListener {

  private enum class State {
    IDLE,
    ZOOMING,
    PANNING,
    INVALID_PANNING,
    FLINGING,
    ANIMATING
  }

  companion object {
    const val DEFAULT_MAX_SCALE_FACTOR = 4f
    const val DEFAULT_MIN_SCALE_FACTOR = 1f
    private const val SNAPPING_DISTANCE_IN_POINTS = 4f
    private const val SPRING_TENSION = 90.0
    private const val SPRING_FRICTION = 10.0
  }

  internal var interceptingTouch = false

  private var currentScaleFactor = 1f
  private var state = State.IDLE
  private var dragOffsetX = 0f
  private var dragOffsetY = 0f

  private var accumulatedXDeltaForTouchInteraction = 0f
  private var accumulatedYDeltaForTouchInteraction = 0f

  private var rootView: LithoZoomableView? = null

  private val scroller = OverScroller(context)
  private val snappingDelta = SNAPPING_DISTANCE_IN_POINTS.dpToPx()
  private val pivotPoint: PointF = PointF()
  private val zoomSpring: Spring
  private val panXSpring: Spring
  private val panYSpring: Spring
  private val minFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
  private val maxScaleFactor: Float = DEFAULT_MAX_SCALE_FACTOR
  private val doubleTapScaleFactor: Float = maxScaleFactor
  private val tapAction: Runnable? = null

  init {
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

  private val gestureListener: SimpleOnGestureListenerWorkaround by lazy {
    object : SimpleOnGestureListenerWorkaround() {
      override fun onDoubleTap(event: MotionEvent): Boolean {
        if (!isDoubleTapEnabled) {
          return false
        }
        val contentView = requireRootView().renderTreeView
        if (state == State.IDLE) {
          if (currentScaleFactor - DEFAULT_MIN_SCALE_FACTOR <
              doubleTapScaleFactor - currentScaleFactor) {
            dragOffsetX = ((contentView.width) / 2 - (event.x)) * (doubleTapScaleFactor - 1)
            dragOffsetY = ((contentView.height) / 2 - (event.y)) * (doubleTapScaleFactor - 1)
            val (panX, panY) = maxOffsetXYForScale(doubleTapScaleFactor)
            animateTo(doubleTapScaleFactor.toDouble(), panX.toDouble(), panY.toDouble())
          } else {
            val (panX, panY) = maxOffsetXYForScale(DEFAULT_MIN_SCALE_FACTOR)
            animateTo(DEFAULT_MIN_SCALE_FACTOR.toDouble(), panX.toDouble(), panY.toDouble())
          }
        }
        return true
      }

      override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
        val tapAction = this@ZoomableController.tapAction
        if (tapAction != null) {
          tapAction.run()
          return true
        }
        return false
      }

      override fun onScroll(
          e1: MotionEvent?,
          e2: MotionEvent,
          distanceX: Float,
          distanceY: Float
      ): Boolean {
        if (e1 == null) {
          return false
        }
        val dx = (e1.x - e2.x)
        accumulatedXDeltaForTouchInteraction -= dx
        val dy = (e1.y - e2.y)
        accumulatedYDeltaForTouchInteraction -= dy
        if (state != State.INVALID_PANNING &&
            (state == State.PANNING || canScroll(distanceX, distanceY))) {
          state = State.PANNING
          dragOffsetX -= distanceX
          dragOffsetY -= distanceY
          panXSpring.setCurrentValue(dragOffsetX.toDouble(), true)
          panYSpring.setCurrentValue(dragOffsetY.toDouble(), true)
          updateTranslation(dragOffsetX, dragOffsetY)
          requireRootView()
              .nestedScrollingChildHelper
              .dispatchNestedScroll(dx.roundToInt(), dy.roundToInt(), 0, 0, null)
          return true
        } else {
          state = State.INVALID_PANNING
          requireRootView()
              .nestedScrollingChildHelper
              .dispatchNestedScroll(0, 0, dx.roundToInt(), dy.roundToInt(), null)
          return true
        }
      }

      override fun onFling(
          e1: MotionEvent?,
          e2: MotionEvent,
          velocityX: Float,
          velocityY: Float
      ): Boolean {
        if (e1 == null) {
          return false
        }
        val dx = (e1.x - e2.x)
        val dy = (e1.y - e2.y)
        val flingIsVertical = velocityX.absoluteValue <= velocityY.absoluteValue
        if ((flingIsVertical && velocityY.absoluteValue < minFlingVelocity) ||
            (!flingIsVertical && velocityX.absoluteValue < minFlingVelocity)) {
          return false
        }
        val rootView = requireRootView()
        val contentView = rootView.renderTreeView

        val maxAllowedOffsetX =
            ((currentScaleFactor * contentView.width - contentView.width) / 2f).roundToInt()
        val maxAllowedOffsetY =
            ((currentScaleFactor * contentView.height - contentView.height) / 2f).roundToInt()

        if ((flingIsVertical && canScrollY(dy)) || (!flingIsVertical && canScrollX(dx))) {
          state = State.FLINGING

          scroller.fling(
              dragOffsetX.roundToInt(),
              dragOffsetY.roundToInt(),
              if (flingIsVertical) 0 else velocityX.roundToInt(),
              if (flingIsVertical) velocityY.roundToInt() else 0,
              -maxAllowedOffsetX,
              maxAllowedOffsetX,
              -maxAllowedOffsetY,
              maxAllowedOffsetY)

          val runnable: Runnable =
              object : Runnable {
                override fun run() {
                  // Lambdas don't allow  you to refer to this
                  scroller.computeScrollOffset()
                  dragOffsetX = scroller.currX.toFloat()
                  dragOffsetY = scroller.currY.toFloat()
                  updateTranslation(scroller.currX.toFloat(), scroller.currY.toFloat())
                  if (!scroller.isFinished) {
                    rootView.postOnAnimation(this)
                  } else {
                    finishMovement()
                  }
                }
              }

          rootView.postOnAnimation(runnable)
          return true
        }

        if (state == State.IDLE || state == State.INVALID_PANNING) {
          // For some reason sometime the sign of the velocity returned by the GestureDetector
          // is
          // wrong. Let's adjust it manually
          var adjustedVelX = velocityX
          var adjustedVelY = velocityY

          if (velocityX.sign != accumulatedXDeltaForTouchInteraction.sign) {
            adjustedVelX *= -1
          }
          if (velocityY.sign != accumulatedYDeltaForTouchInteraction.sign) {
            adjustedVelY *= -1
          }
          rootView.dispatchNestedPreFling(adjustedVelX, velocityY)
          return rootView.dispatchNestedFling(adjustedVelX, adjustedVelY, false)
        }

        return true
      }
    }
  }

  private val gestureDetector: GestureDetector by lazy { GestureDetector(context, gestureListener) }

  private val scaleListener =
      object : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
          if (state == State.ZOOMING) {
            val focusX = detector.focusX
            val focusY = detector.focusY
            val offsetX: Float = focusX - pivotPoint.x
            val offsetY: Float = focusY - pivotPoint.y
            dragOffsetX += offsetX
            dragOffsetY += offsetY
            panXSpring.setCurrentValue(dragOffsetX.toDouble(), true)
            panYSpring.setCurrentValue(dragOffsetY.toDouble(), true)

            updateTranslation(dragOffsetX, dragOffsetY)
            updatePivotPoint(focusX, focusY)

            var accumulatedScaleFactor: Float = (currentScaleFactor * detector.getScaleFactor())

            if ((accumulatedScaleFactor > maxScaleFactor &&
                accumulatedScaleFactor > currentScaleFactor) ||
                (accumulatedScaleFactor < DEFAULT_MIN_SCALE_FACTOR &&
                    accumulatedScaleFactor < currentScaleFactor)) {
              // Constrain the scale if it's going over the limits
              accumulatedScaleFactor =
                  (currentScaleFactor + (accumulatedScaleFactor - currentScaleFactor) * .5f)
            }

            currentScaleFactor = accumulatedScaleFactor

            zoomSpring.currentValue = accumulatedScaleFactor.toDouble()
            updateScaleFactor(currentScaleFactor)
            return true
          }

          return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
          if (state == State.IDLE ||
              state == State.PANNING) { // We want to be able to transform pan events into zoom
            startZoom()
            pivotPoint.x = detector.focusX
            pivotPoint.y = detector.focusY
            return true
          }

          return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) = Unit
      }

  private val zoomGestureDetector =
      ScaleGestureDetector(context, scaleListener).apply { isQuickScaleEnabled = false }

  fun bindTo(view: LithoZoomableView?) {
    this.rootView = view
  }

  fun unbind() {
    this.rootView = null
  }

  private fun requireRootView(): LithoZoomableView = checkNotNull(rootView)

  private fun canScroll(distanceX: Float, distanceY: Float): Boolean {
    return canScrollX(distanceX) || canScrollY(distanceY)
  }

  private fun canScrollX(distanceX: Float): Boolean {
    val contentView = requireRootView().renderTreeView
    val maxAllowedOffsetX = ((currentScaleFactor * contentView.width - contentView.width) / 2f)

    return (dragOffsetX - distanceX.sign * snappingDelta in
        -maxAllowedOffsetX..maxAllowedOffsetX) && distanceX.absoluteValue > snappingDelta
  }

  private fun canScrollY(distanceY: Float): Boolean {
    val contentView = requireRootView().renderTreeView
    val maxAllowedOffsetY = ((currentScaleFactor * contentView.height - contentView.height) / 2f)

    return (dragOffsetY - distanceY.sign * snappingDelta) in
        -maxAllowedOffsetY..maxAllowedOffsetY && distanceY.absoluteValue > snappingDelta
  }

  private fun startZoom() {
    state = State.ZOOMING
    interceptingTouch = true
    val renderTreeView = requireRootView().renderTreeView
    renderTreeView.parent.requestDisallowInterceptTouchEvent(true)
    renderTreeView.setHasTransientState(true)
  }

  private fun finishMovement() {
    interceptingTouch = false
    // We can settle
    val contentView = requireRootView().renderTreeView
    updatePivotPoint(contentView.width / 2f, contentView.height / 2f)
    zoomSpring.setCurrentValue(currentScaleFactor.toDouble(), true)
    panXSpring.setCurrentValue(dragOffsetX.toDouble(), true)
    panYSpring.setCurrentValue(dragOffsetY.toDouble(), true)
    zoomSpring.removeListener(this)
    panXSpring.removeListener(this)
    panYSpring.removeListener(this)
    contentView.setHasTransientState(false)
    state = State.IDLE
  }

  private fun animateToLimits() {
    if (state == State.ANIMATING || state == State.FLINGING) {
      // We are already doing an animation. No need to do anything here
      return
    }
    val targetScaleFactor = currentScaleFactor.coerceIn(DEFAULT_MIN_SCALE_FACTOR..maxScaleFactor)
    val (panX, panY) = maxOffsetXYForScale(targetScaleFactor)
    animateTo(targetScaleFactor.toDouble(), panX.toDouble(), panY.toDouble())
  }

  private fun animateTo(scaleFactor: Double, panX: Double, panY: Double) {
    state = State.ANIMATING
    interceptingTouch = false
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
    val contentView = requireRootView().renderTreeView
    contentView.translationX = translationX
    contentView.translationY = translationY
  }

  private fun updateScaleFactor(scaleFactor: Float) {
    val contentView = requireRootView().renderTreeView
    var scaleFactorToApply = scaleFactor
    if (java.lang.Float.isNaN(scaleFactorToApply)) {
      scaleFactorToApply = 1.0f
    }
    contentView.scaleX = scaleFactorToApply
    contentView.scaleY = scaleFactorToApply
  }

  private fun maxOffsetXYForScale(scale: Float): Pair<Float, Float> {
    val contentView = requireRootView().renderTreeView

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
      currentScaleFactor = zoomSpring.currentValue.toFloat()
      updateScaleFactor(currentScaleFactor)
      dragOffsetX = panXSpring.currentValue.toFloat()
      dragOffsetY = panYSpring.currentValue.toFloat()
      updateTranslation(dragOffsetX, dragOffsetY)
    }
  }

  override fun onSpringAtRest(spring: Spring) {
    finishMovement()
  }

  override fun onSpringActivate(spring: Spring) = Unit

  override fun onSpringEndStateChange(spring: Spring?) = Unit

  /** Touch Listener */
  fun onTouch(event: MotionEvent, parent: ViewParent?): Boolean {
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        requireRootView().startNestedScroll(ViewCompat.SCROLL_AXIS_NONE, ViewCompat.TYPE_TOUCH)
        if (state == State.ANIMATING) {
          zoomSpring.endValue = zoomSpring.currentValue
          panXSpring.endValue = panXSpring.currentValue
          panYSpring.endValue = panYSpring.currentValue

          zoomSpring.setAtRest()
          panYSpring.setAtRest()
          panXSpring.setAtRest()
          finishMovement()
        }
        if (state == State.FLINGING) {
          state = State.IDLE
        }
        scroller.forceFinished(true)
      }
    }

    zoomGestureDetector.onTouchEvent(event)

    if (state != State.ZOOMING && event.pointerCount == 1) {
      gestureDetector.onTouchEvent(event)
    }

    when (event.actionMasked) {
      MotionEvent.ACTION_UP,
      MotionEvent.ACTION_CANCEL -> {
        if (state != State.FLINGING) {
          animateToLimits()
        }
        parent?.requestDisallowInterceptTouchEvent(false)
        interceptingTouch = false
        requireRootView().stopNestedScroll(ViewCompat.TYPE_TOUCH)
        accumulatedXDeltaForTouchInteraction = 0f
        accumulatedYDeltaForTouchInteraction = 0f
      }
    }

    return true
  }

  private fun Float.dpToPx(): Float = (this * context.resources.displayMetrics.density)
}
