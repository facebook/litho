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

package com.facebook.rendercore.zoomable

import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringConfig
import com.facebook.rebound.SpringListener
import com.facebook.rebound.SpringSystem
import com.facebook.rebound.SpringUtil
import com.facebook.rendercore.thread.utils.ThreadUtils
import kotlin.LazyThreadSafetyMode

abstract class ZoomableViewBaseController<Content : ZoomableContainerView>(
    private val context: Context,
    private val backgroundDrawable: Drawable,
    private val isRemoteZoomEnabled: Boolean,
) : SpringListener {

  enum class State {
    IDLE,
    ANIMATING,
    REMOTE_ZOOMING,
    ZOOMING,
    PANNING,
    INVALID_PANNING,
    FLINGING,
  }

  interface Listener {
    fun onZoomStarted()

    fun onZoomFinished()
  }

  companion object {
    val BLACK_TRANSPARENT_80: Int = Color.parseColor("#CC000000")
    const val DEFAULT_MAX_SCALE_FACTOR = 4f
    const val DEFAULT_MIN_SCALE_FACTOR = 1f
    private const val SNAPPING_DISTANCE_IN_POINTS = 4f
    private const val ZOOM_SPRING_TENSION = 90.0
    private const val ZOOM_SPRING_FRICTION = 10.0
    private const val ANIMATING_BACK_SPRING_TENSION = 40.0
    private const val ANIMATING_BACK_SPRING_FRICTION = 5.0
    private val LAYOUT_PARAMS: FrameLayout.LayoutParams =
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
  }

  abstract val decorView: ViewGroup

  private var isInitialized = false
  private var isBound = false

  var interceptingTouch = false
  var currentScaleFactor = 1f
  var state: State = State.IDLE
  var dragOffsetX = 0f
  var dragOffsetY = 0f
  var panOffsetX = 0f
  var panOffsetY = 0f
  var lastFocusX = 0f
  var lastFocusY = 0f
  var maxScaleFactor: Float = DEFAULT_MAX_SCALE_FACTOR
  var animateToYValue: Double = 0.0
  var animateToXValue: Double = 0.0

  private var touchXAdjustment = 0
  private var touchYAdjustment = 0
  private var containerMarginX = 0
  private var containerMarginY = 0
  private var initialTranslationY = 0f
  private var contentViewIndex = 0
  private var contentViewLayoutParams: ViewGroup.LayoutParams? = null
  private var rootView: Content? = null
  private var remoteContainer: ZoomableContainerView? = null
  private var contentView: View? = null
  private var zoomSpring: Spring? = null
  private var animatingBackSpring: Spring? = null
  private var isTearingDown = false

  private val zoomableEventListeners: MutableSet<Listener> by lazy { mutableSetOf() }

  val snappingDelta: Float = SNAPPING_DISTANCE_IN_POINTS.dpToPx()
  private val pivotPoint: PointF = PointF()
  private val rect = Rect()

  private fun interestedInTouchEvent(event: MotionEvent): Boolean {
    // TODO: T199256988 temporarily fix for root view sometimes being null in carousel
    if (rootView == null) {
      return false
    }

    return when (state) {
      State.IDLE -> event.pointerCount == 2 && contentViewContainsEvent(event)
      State.REMOTE_ZOOMING,
      State.PANNING,
      State.ZOOMING -> event.pointerCount <= 2
      else -> event.pointerCount <= 2 && contentViewContainsEvent(event)
    }
  }

  private fun contentViewContainsEvent(event: MotionEvent): Boolean {
    requireContentView().getGlobalVisibleRect(rect)
    return rect.contains(event.rawX.toInt(), event.rawY.toInt())
  }

  private val scaleListener =
      object : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
          if (state == State.REMOTE_ZOOMING || state == State.ZOOMING) {
            accumulateDragAndPivot(adjustedFocusX(detector.focusX), adjustedFocusY(detector.focusY))
            var accumulatedScaleFactor: Float = (currentScaleFactor * detector.scaleFactor)

            if (
                (accumulatedScaleFactor > maxScaleFactor &&
                    accumulatedScaleFactor > currentScaleFactor) ||
                    (accumulatedScaleFactor < DEFAULT_MIN_SCALE_FACTOR &&
                        accumulatedScaleFactor < currentScaleFactor)
            ) {
              accumulatedScaleFactor =
                  (currentScaleFactor + (accumulatedScaleFactor - currentScaleFactor) * .5f)
            }

            val contentView = requireContentView()
            updateScaleFactor(contentView, accumulatedScaleFactor)
            updatePivotPoint(contentView, pivotPoint.x, pivotPoint.y)
            updateTranslation(
                contentView,
                dragOffsetX * contentView.scaleX,
                dragOffsetY * contentView.scaleY,
            )
            return true
          }
          return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
          if (state == State.IDLE) {
            startZoom()
            pivotPoint.x = adjustedFocusX(detector.focusX)
            pivotPoint.y = adjustedFocusY(detector.focusY)

            zoomableEventListeners.forEach { it.onZoomStarted() }

            return true
          }
          return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) = Unit
      }

  // Lazy: ScaleGestureDetector allocates VelocityTracker and internal buffers.
  // ~99% of controllers never see a zoom gesture, so defer allocation to first touch.
  // NONE is safe: only accessed from onTouch() on the main thread.
  protected val zoomGestureDetector: ScaleGestureDetector by
      lazy(LazyThreadSafetyMode.NONE) {
        ScaleGestureDetector(context, scaleListener).apply { isQuickScaleEnabled = false }
      }

  abstract fun getRenderTreeView(): View

  protected open fun stopNestedScroll(rootView: Content, typeTouch: Int): Unit = Unit

  protected fun onTouch(event: MotionEvent): Boolean {
    if (!interestedInTouchEvent(event)) {
      return false
    }
    zoomGestureDetector.onTouchEvent(event)

    when (event.actionMasked) {
      MotionEvent.ACTION_MOVE -> {
        if (state == State.PANNING) {
          updateDragAndPivot(
              adjustedFocusX(zoomGestureDetector.focusX) - panOffsetX,
              adjustedFocusY(zoomGestureDetector.focusY) - panOffsetY,
          )
        } else if (state == State.REMOTE_ZOOMING || state == State.ZOOMING) {
          val focusX = adjustedFocusX(zoomGestureDetector.focusX)
          val focusY = adjustedFocusY(zoomGestureDetector.focusY)
          if (!zoomGestureDetector.isInProgress) {
            accumulateDragAndPivot(focusX, focusY)
            val contentView = requireContentView()
            updatePivotPoint(contentView, pivotPoint.x, pivotPoint.y)
            updateTranslation(
                contentView,
                dragOffsetX * contentView.scaleX,
                dragOffsetY * contentView.scaleY,
            )
          }
          lastFocusX = focusX
          lastFocusY = focusY
        } else {
          lastFocusX = adjustedFocusX(zoomGestureDetector.focusX)
          lastFocusY = adjustedFocusY(zoomGestureDetector.focusY)
        }
      }
      MotionEvent.ACTION_POINTER_UP -> {
        if (state != State.REMOTE_ZOOMING) {
          return false
        }
        state = State.PANNING
        if (event.pointerCount == 2) {
          val remainingPointerIndex = event.pointerCount - event.actionIndex - 1
          panOffsetX = adjustedFocusX(event.getX(remainingPointerIndex)) - lastFocusX
          panOffsetY = adjustedFocusY(event.getY(remainingPointerIndex)) - lastFocusY
        } else {
          panOffsetX = adjustedFocusX(zoomGestureDetector.focusX) - lastFocusX
          panOffsetY = adjustedFocusY(zoomGestureDetector.focusY) - lastFocusY
        }
        return true
      }
      MotionEvent.ACTION_UP,
      MotionEvent.ACTION_CANCEL -> {
        if (state == State.REMOTE_ZOOMING || state == State.PANNING) {
          if (contentView != null && rootView != null) {
            val (panX, panY) = maxOffsetXYForScale(DEFAULT_MIN_SCALE_FACTOR)
            animateTo(
                DEFAULT_MIN_SCALE_FACTOR.toDouble(),
                panX.toDouble(),
                panY.toDouble(),
            )
          } else {
            resetValues()
          }
        }
      }
    }

    return true
  }

  /**
   * Lifecycle marker — must be called before bindTo(). Springs are lazily allocated on first zoom
   * gesture via ensureSpringsCreated(), not here.
   */
  fun init() {
    ThreadUtils.assertMainThread(
        "ZoomableViewBaseController.init() must be called on the main thread",
    )
    if (isInitialized) {
      return
    }
    isInitialized = true
  }

  /** Lazily creates springs on first zoom gesture. Only called from startZoom() on main thread. */
  private fun ensureSpringsCreated() {
    if (zoomSpring != null && animatingBackSpring != null) return
    val springSystem = SpringSystem.create()
    zoomSpring =
        springSystem
            .createSpring()
            .setSpringConfig(
                SpringConfig.fromOrigamiTensionAndFriction(
                    ZOOM_SPRING_TENSION,
                    ZOOM_SPRING_FRICTION,
                ),
            )
            .apply { currentValue = DEFAULT_MIN_SCALE_FACTOR.toDouble() }

    animatingBackSpring =
        springSystem
            .createSpring()
            .setSpringConfig(
                SpringConfig.fromOrigamiTensionAndFriction(
                    ANIMATING_BACK_SPRING_TENSION,
                    ANIMATING_BACK_SPRING_FRICTION,
                ),
            )
            .setOvershootClampingEnabled(true)
  }

  fun bindTo(view: Content?) {
    if (!isBound) {
      isBound = true
    }
    rootView = view
  }

  fun unbind() {
    if (isBound) {
      isBound = false
    }
    // Remove spring listeners BEFORE teardown to prevent onSpringAtRest re-entrancy
    zoomSpring?.removeListener(this)
    animatingBackSpring?.removeListener(this)
    val wasZooming = state != State.IDLE
    if (wasZooming) {
      teardownRemoteZoom()
    }
    rootView = null
    remoteContainer = null
    resetValues()
    if (wasZooming) {
      zoomableEventListeners.forEach { it.onZoomFinished() }
    }
    zoomableEventListeners.clear()
  }

  /**
   * Removes [child] from its parent ViewGroup, deferring via [View.post] if the parent is currently
   * in a layout pass to avoid FrameLayout.layoutChildren NPE.
   */
  private fun removeViewWhenSafe(child: View) {
    val parent = child.parent as? ViewGroup ?: return
    if (parent.isInLayout) {
      parent.post { (child.parent as? ViewGroup)?.removeView(child) }
      return
    }
    parent.removeView(child)
  }

  private fun teardownRemoteZoom() {
    if (isTearingDown) return
    isTearingDown = true
    try {
      teardownRemoteZoomInternal()
    } finally {
      isTearingDown = false
    }
  }

  private fun teardownRemoteZoomInternal() {
    val contentView = this.contentView
    val remoteContainer = this.remoteContainer
    val rootView = this.rootView

    if (contentView != null && remoteContainer != null && rootView != null) {
      // Defer reparenting if in layout to avoid FrameLayout.layoutChildren NPE.
      val activeDecorView = decorView
      if (rootView.isInLayout || remoteContainer.isInLayout || activeDecorView.isInLayout) {
        val capturedContentView = contentView
        val capturedRemoteContainer = remoteContainer
        val capturedRootView = rootView
        val capturedIndex = contentViewIndex
        val capturedLayoutParams = contentViewLayoutParams
        val capturedTranslationY = initialTranslationY
        activeDecorView.post {
          if (capturedContentView.parent === capturedRemoteContainer) {
            capturedRootView.parent?.requestDisallowInterceptTouchEvent(false)
            capturedRootView.requestDisallowInterceptTouchEvent(false)
            capturedRemoteContainer.parent?.requestDisallowInterceptTouchEvent(false)
            capturedContentView.setHasTransientState(false)
            resetContentViewTransforms(capturedContentView, capturedTranslationY)
            capturedRemoteContainer.detachViewFromParent(capturedContentView)
            val safeIdx = capturedIndex.coerceIn(0, capturedRootView.childCount)
            capturedRootView.attachViewToParent(capturedContentView, safeIdx, capturedLayoutParams)
            capturedContentView.requestLayout()
          }
          removeViewWhenSafe(capturedRemoteContainer)
        }
        this.contentView = null
        this.remoteContainer = null
        interceptingTouch = false
        state = State.IDLE
        return
      }

      rootView.parent?.requestDisallowInterceptTouchEvent(false)
      rootView.requestDisallowInterceptTouchEvent(false)
      remoteContainer.parent?.requestDisallowInterceptTouchEvent(false)
      stopNestedScroll(rootView, ViewCompat.TYPE_TOUCH)

      contentView.setHasTransientState(false)
      resetContentViewTransforms(contentView)

      remoteContainer.detachViewFromParent(contentView)
      val safeIndex = contentViewIndex.coerceIn(0, rootView.childCount)
      rootView.attachViewToParent(contentView, safeIndex, contentViewLayoutParams)
      contentView.requestLayout()
    }

    remoteContainer?.let { removeViewWhenSafe(it) }

    this.contentView = null
    this.remoteContainer = null
    interceptingTouch = false
    state = State.IDLE
  }

  fun destroy() {
    if (!isInitialized) {
      cleanupSprings()
      return
    }
    isInitialized = false
    unbind()
    cleanupSprings()
  }

  private fun cleanupSprings() {
    zoomSpring?.destroy()
    animatingBackSpring?.destroy()
    zoomSpring = null
    animatingBackSpring = null
  }

  private fun createRemoteContainer() {
    initialTranslationY = getRenderTreeView().translationY

    val contentLocation = IntArray(2)
    getRenderTreeView().getLocationInWindow(contentLocation)
    val rootLocation = IntArray(2)
    requireRootView().getLocationInWindow(rootLocation)
    touchXAdjustment = contentLocation[0] - rootLocation[0]
    touchYAdjustment = contentLocation[1] - rootLocation[1]

    val activeDecorView = decorView
    val decorViewLocation = IntArray(2)
    activeDecorView.getLocationInWindow(decorViewLocation)
    containerMarginX = contentLocation[0] - decorViewLocation[0] - activeDecorView.paddingLeft
    containerMarginY = contentLocation[1] - decorViewLocation[1] - activeDecorView.paddingTop

    val remoteContainer = ZoomableContainerView(context)
    remoteContainer.layoutParams = LAYOUT_PARAMS
    remoteContainer.background = backgroundDrawable
    remoteContainer.visibility = View.INVISIBLE
    activeDecorView.addView(remoteContainer)
    this.remoteContainer = remoteContainer
  }

  protected fun requireRootView(): Content = checkNotNull(rootView)

  private fun requireRemoteContainer(): ZoomableContainerView = checkNotNull(remoteContainer)

  private fun requireContentView(): View =
      if (state == State.REMOTE_ZOOMING) {
        checkNotNull(contentView)
      } else {
        getRenderTreeView()
      }

  protected fun requireZoomSpring(): Spring = checkNotNull(zoomSpring)

  protected fun requireAnimatingBackSpring(): Spring = checkNotNull(animatingBackSpring)

  private fun startZoom() {
    ensureSpringsCreated()
    if (isRemoteZoomEnabled) {
      startRemoteZoom()
    } else {
      state = State.ZOOMING
    }
    requireContentView().setHasTransientState(true)
    val zoomSpring = requireZoomSpring()
    zoomSpring.currentValue = 1.0
    zoomSpring.addListener(this)
  }

  private fun startRemoteZoom() {
    val rootView = requireRootView()

    // Defer reparenting if in layout to avoid FrameLayout.layoutChildren NPE.
    if (rootView.isInLayout || decorView.isInLayout) {
      rootView.post { if (isBound && state != State.REMOTE_ZOOMING) startRemoteZoom() }
      return
    }

    val contentView = getRenderTreeView()

    // Recover contentView from a stale remoteContainer left by a deferred teardown.
    // Must run before createRemoteContainer() so initialTranslationY captures the
    // resting value, not a zoom-modified one.
    val currentParent = contentView.parent
    if (currentParent !== rootView && currentParent is ZoomableContainerView) {
      currentParent.detachViewFromParent(contentView)
      removeViewWhenSafe(currentParent)
      resetContentViewTransforms(contentView)
      val reattachIndex = contentViewIndex.coerceIn(0, rootView.childCount)
      rootView.attachViewToParent(
          contentView,
          reattachIndex,
          contentViewLayoutParams ?: LAYOUT_PARAMS,
      )
    }

    createRemoteContainer()
    state = State.REMOTE_ZOOMING
    interceptingTouch = true

    val remoteContainer = requireRemoteContainer()

    contentViewIndex = rootView.indexOfChild(contentView)
    contentViewLayoutParams = contentView.layoutParams

    rootView.detachViewFromParent(contentView)
    val preservedParams =
        FrameLayout.LayoutParams(
                contentView.width.coerceAtLeast(1),
                contentView.height.coerceAtLeast(1),
            )
            .apply {
              leftMargin = containerMarginX
              topMargin = containerMarginY
            }
    remoteContainer.attachViewToParent(contentView, 0, preservedParams)
    remoteContainer.visibility = View.VISIBLE
    remoteContainer.bringToFront()
    rootView.requestLayout()
    rootView.invalidate()
    this.contentView = contentView

    rootView.parent.requestDisallowInterceptTouchEvent(true)
    rootView.requestDisallowInterceptTouchEvent(true)
    remoteContainer.parent.requestDisallowInterceptTouchEvent(true)
  }

  fun finishMovement() {
    interceptingTouch = false
    // Null-safe: fields may be null if unbind()/destroy() ran during animation
    val zoomSpring = this.zoomSpring
    val animatingBackSpring = this.animatingBackSpring
    zoomSpring?.removeListener(this)
    animatingBackSpring?.removeListener(this)

    val contentView = this.contentView ?: rootView?.let { getRenderTreeView() }
    if (contentView != null) {
      updatePivotPoint(contentView, contentView.width / 2f, contentView.height / 2f)
      contentView.setHasTransientState(false)
      if (isRemoteZoomEnabled) {
        updateTranslation(contentView, 0f, initialTranslationY)
        updateScaleFactor(contentView, 1f)
      }
    }
    resetValues()
    cleanupSprings() // Release springs; re-created lazily on next zoom
  }

  private fun resetValues() {
    val wasZooming = state != State.IDLE
    dragOffsetX = 0f
    dragOffsetY = 0f
    panOffsetX = 0f
    panOffsetY = 0f
    lastFocusX = 0f
    lastFocusY = 0f
    animateToYValue = 0.0
    animateToXValue = 0.0
    state = State.IDLE
    if (wasZooming) {
      zoomableEventListeners.forEach { it.onZoomFinished() }
    }
  }

  private fun animateTo(scaleFactor: Double, panX: Double, panY: Double) {
    animateToXValue = panX
    animateToYValue = panY

    val animatingBackSpring = requireAnimatingBackSpring()
    val zoomSpring = requireZoomSpring()
    zoomSpring.currentValue = currentScaleFactor.toDouble()
    animatingBackSpring.currentValue = 1.0
    animatingBackSpring.addListener(this)
    animatingBackSpring.endValue = 0.0
    state = State.ANIMATING
    interceptingTouch = false
    if (zoomSpring.isAtRest && animatingBackSpring.isAtRest) {
      finishMovement()
    }
  }

  private fun updatePivotPoint(contentView: View, pivotX: Float, pivotY: Float) {
    pivotPoint.x = pivotX
    pivotPoint.y = pivotY
    contentView.pivotX = pivotX
    contentView.pivotY = pivotY
  }

  private fun updateTranslation(contentView: View, translationX: Float, translationY: Float) {
    contentView.translationX = translationX
    contentView.translationY = translationY
  }

  /** Resets scale and translation on [contentView] to its resting state. */
  private fun resetContentViewTransforms(
      contentView: View,
      translationY: Float = initialTranslationY,
  ) {
    contentView.scaleX = 1f
    contentView.scaleY = 1f
    contentView.translationX = 0f
    contentView.translationY = translationY
  }

  private fun accumulateDragAndPivot(x: Float, y: Float) {
    dragOffsetX += x - pivotPoint.x
    dragOffsetY += y - pivotPoint.y
    pivotPoint.x = x
    pivotPoint.y = y
  }

  private fun updateDragAndPivot(x: Float, y: Float) {
    val contentView = requireContentView()
    val offsetX = x - pivotPoint.x
    val offsetY = y - pivotPoint.y
    updateDragOffsets(contentView, offsetX, offsetY)
    updatePivotPoint(contentView, x, y)
  }

  private fun updateDragOffsets(contentView: View, offsetX: Float, offsetY: Float) {
    dragOffsetX += offsetX
    dragOffsetY += offsetY
    updateTranslation(
        contentView,
        dragOffsetX * contentView.scaleX,
        dragOffsetY * contentView.scaleY,
    )
  }

  /** Adjusts raw focus X from window coordinates to the content view's local coordinate space. */
  private fun adjustedFocusX(rawFocusX: Float): Float =
      if (interceptingTouch) rawFocusX - touchXAdjustment else rawFocusX

  /** Adjusts raw focus Y from window coordinates to the content view's local coordinate space. */
  private fun adjustedFocusY(rawFocusY: Float): Float =
      if (interceptingTouch) rawFocusY - touchYAdjustment else rawFocusY

  private fun updateScaleFactor(contentView: View, scaleFactor: Float) {
    val scaleFactorToApply =
        if (java.lang.Float.isNaN(scaleFactor)) {
          1.0f
        } else {
          scaleFactor
        }
    currentScaleFactor = scaleFactorToApply
    contentView.scaleX = scaleFactorToApply
    contentView.scaleY = scaleFactorToApply
  }

  protected fun maxOffsetXYForScale(scale: Float): Pair<Float, Float> {
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
      // Null-safe: content view or springs may be null if unbind() ran during animation
      val contentView = this.contentView ?: rootView?.let { getRenderTreeView() }
      val zoomSpring = this.zoomSpring
      if (contentView == null || zoomSpring == null) {
        return
      }
      val currentValue = spring.currentValue
      updateScaleFactor(
          contentView,
          SpringUtil.mapValueFromRangeToRange(
                  currentValue,
                  0.0,
                  1.0,
                  1.0,
                  zoomSpring.currentValue,
              )
              .toFloat(),
      )
      updateTranslation(
          contentView,
          SpringUtil.mapValueFromRangeToRange(
                  currentValue,
                  0.0,
                  1.0,
                  animateToXValue,
                  (dragOffsetX * currentValue),
              )
              .toFloat(),
          SpringUtil.mapValueFromRangeToRange(
                  currentValue,
                  0.0,
                  1.0,
                  animateToYValue,
                  (dragOffsetY * currentValue),
              )
              .toFloat(),
      )
    }
  }

  override fun onSpringAtRest(spring: Spring) {
    if (isRemoteZoomEnabled && (state == State.REMOTE_ZOOMING || state == State.ANIMATING)) {
      teardownRemoteZoom()
    }
    finishMovement()
  }

  fun addZoomableEventListener(zoomableEventListener: Listener) {
    ThreadUtils.assertMainThread()
    this.zoomableEventListeners.add(zoomableEventListener)
  }

  fun removeZoomableEventListener(zoomableEventListener: Listener) {
    ThreadUtils.assertMainThread()
    this.zoomableEventListeners.remove(zoomableEventListener)
  }

  override fun onSpringActivate(spring: Spring) = Unit

  override fun onSpringEndStateChange(spring: Spring?) = Unit

  private fun Float.dpToPx(): Float = (this * context.resources.displayMetrics.density)
}
