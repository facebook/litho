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
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.facebook.litho.BaseMountingView
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringConfig
import com.facebook.rebound.SpringListener
import com.facebook.rebound.SpringSystem
import com.facebook.rendercore.zoomable.ZoomableViewBaseController

/**
 * Pinch to zoom for content that stays where it is in its Litho tree.
 *
 * While the user zooms, the content's host is hidden from its parent and a scaled copy of it is
 * drawn in the overlay of the window's root view, above everything else. Nothing is moved between
 * parents, so the content keeps its mount state. When the fingers lift, the copy springs back and
 * the host is shown again.
 *
 * The copy is recorded from the host whenever the gesture or the spring moves it, and whenever a
 * view inside the host changes, such as a video showing its next frame. Drawables mounted on the
 * host itself show changes at the next move.
 */
@ExperimentalLithoApi
class OverlayZoomController(
    private val context: Context,
    backgroundDrawable: Drawable = ColorDrawable(ZoomableViewBaseController.BLACK_TRANSPARENT_80),
    private val maxScale: Float = ZoomableViewBaseController.DEFAULT_MAX_SCALE_FACTOR,
) {

  private enum class State {
    IDLE,
    // At least one finger is down after the zoom started: the fingers scale and move the copy.
    ZOOMING,
    // The fingers lifted and the copy springs back.
    ANIMATING,
  }

  /** Told when a zoom starts and when it has finished, after the copy springs back. */
  var zoomableEventListener: ZoomableViewBaseController.Listener? = null

  /**
   * Whether the host keeps a gesture its content doesn't handle, so its second finger reaches the
   * host. When false, such a gesture goes on to the host's ancestors, which then have to pass a
   * pinch to [zoomableTouchListener] themselves.
   */
  var keepsUnhandledTouches: Boolean = true

  val zoomableTouchListener: ZoomableTouchListener =
      object : ZoomableTouchListener {
        override fun onTouchEvent(event: MotionEvent): Boolean = onTouch(event)

        override val isCurrentlyInZoom: Boolean
          get() = state != State.IDLE
      }

  // Its alpha fades with the spring, and the caller's drawable may be shared.
  private val backgroundDrawable: Drawable = backgroundDrawable.mutate()
  private var state = State.IDLE
  // Whether the host took the current gesture from its content, at the second finger, to zoom it.
  private var claimed = false
  private var host: View? = null
  private var overlay: ZoomOverlay? = null
  private var overlayRoot: ViewGroup? = null
  private var mountingView: View? = null
  private var hostClipBounds: Rect? = null
  private var hostVisibility = View.VISIBLE
  // The observer of the window the host was hidden in, to redraw the copy when the host changes.
  private var hiddenHostObserver: ViewTreeObserver? = null
  // The host the copy is drawn from, hidden until the zoom finishes.
  private var zoomedHost: View? = null
  private val visibleRect = Rect()
  private val mainHandler = Handler(Looper.getMainLooper())
  private val rootOnScreen = IntArray(2)

  // The copy is scaled by `scale` around `pivot` (in the host's coordinates), then moved by
  // `drag`, the distance the fingers' center moved on the screen since the zoom started.
  private var scale = 1f
  // The fingers' scale before the resistance past the allowed range.
  private var fingerScale = 1f
  private var pivotX = 0f
  private var pivotY = 0f
  private var dragX = 0f
  private var dragY = 0f
  // The center of the fingers down, on the screen; set again whenever a finger lands or lifts.
  private var focusX = 0f
  private var focusY = 0f
  // The transform the copy was last drawn with.
  private var drawnScale = 1f
  private var drawnDragX = 0f
  private var drawnDragY = 0f
  private val hostOnScreen = IntArray(2)

  private var springSystem: SpringSystem? = null
  private var backSpring: Spring? = null
  private var animateFromScale = 1f
  private var animateFromDragX = 0f
  private var animateFromDragY = 0f

  private val scaleDetector: ScaleGestureDetector by
      lazy(LazyThreadSafetyMode.NONE) {
        ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
              override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                if (state == State.IDLE) {
                  start()
                }
                return true
              }

              override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (state == State.ZOOMING) {
                  fingerScale *= detector.scaleFactor
                  scale = resist(fingerScale)
                }
                return true
              }
            },
        )
            .apply { isQuickScaleEnabled = false }
      }

  fun bindTo(host: View) {
    if (host !== this.host) {
      if (state != State.IDLE) {
        finish(releaseTransientStateLater = true)
      }
      claimed = false
    }
    this.host = host
  }

  fun unbind() {
    if (state != State.IDLE) {
      // Unbinding is part of an unmount, which mustn't remount the tree as it goes.
      finish(releaseTransientStateLater = true)
    }
    host = null
    claimed = false
    backSpring?.destroy()
    backSpring = null
    springSystem = null
  }

  /**
   * Whether the host should take the gesture from its content: from the second finger landing on it
   * until the fingers lift, so a pinch on content that handles touch still zooms.
   */
  fun onInterceptTouchEvent(event: MotionEvent): Boolean {
    if (state != State.IDLE) {
      return state == State.ZOOMING
    }
    val host = host ?: return false
    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
      // A new gesture: one the host claimed before may never have ended here.
      claimed = false
    }
    if (
        event.actionMasked == MotionEvent.ACTION_POINTER_DOWN &&
            event.pointerCount == 2 &&
            host.canZoom() &&
            host.containsRaw(event)
    ) {
      // The host's onTouch sees only the events after this one.
      detect(event)
      claimed = true
      host.parent?.requestDisallowInterceptTouchEvent(true)
      return true
    }
    return false
  }

  private fun onTouch(event: MotionEvent): Boolean {
    val host = host ?: return false
    when (state) {
      State.IDLE -> {
        if (!host.canZoom()) {
          return false
        }
        if (!claimed && !(event.pointerCount == 2 && host.containsRaw(event))) {
          // Keeps the gesture when the content doesn't, so its second finger reaches the host.
          return keepsUnhandledTouches && event.actionMasked == MotionEvent.ACTION_DOWN
        }
      }
      State.ANIMATING -> return false
      State.ZOOMING -> Unit
    }
    if (detect(event)) {
      return true
    }
    if (state != State.ZOOMING) {
      return true
    }
    when (event.actionMasked) {
      MotionEvent.ACTION_POINTER_DOWN -> setFocus(event, lifting = -1)
      MotionEvent.ACTION_POINTER_UP -> setFocus(event, lifting = event.actionIndex)
      MotionEvent.ACTION_MOVE -> {
        val lastX = focusX
        val lastY = focusY
        setFocus(event, lifting = -1)
        dragX += focusX - lastX
        dragY += focusY - lastY
        redrawIfMoved()
      }
      MotionEvent.ACTION_UP,
      MotionEvent.ACTION_CANCEL -> {
        claimed = false
        animateBack()
      }
    }
    return true
  }

  // Fingers holding still keep sending moves: the copy is redrawn only when they move it.
  private fun redrawIfMoved() {
    if (scale != drawnScale || dragX != drawnDragX || dragY != drawnDragY) {
      drawnScale = scale
      drawnDragX = dragX
      drawnDragY = dragY
      overlay?.invalidateSelf()
    }
  }

  /** Feeds the scale detector; returns whether this event started the zoom. */
  private fun detect(event: MotionEvent): Boolean {
    val wasIdle = state == State.IDLE
    scaleDetector.onTouchEvent(event)
    if (wasIdle && state == State.ZOOMING) {
      setFocus(event, lifting = -1)
      // From the screen, as a listener shared with another view may pass events in its coordinates.
      zoomedHost?.getLocationOnScreen(hostOnScreen)
      pivotX = focusX - hostOnScreen[0]
      pivotY = focusY - hostOnScreen[1]
      return true
    }
    return false
  }

  /** Sets the center of the fingers that stay down: every finger but the one [lifting], if any. */
  private fun setFocus(event: MotionEvent, lifting: Int) {
    var sumX = 0f
    var sumY = 0f
    var count = 0
    for (i in 0 until event.pointerCount) {
      if (i != lifting) {
        sumX += event.rawXOf(i)
        sumY += event.rawYOf(i)
        count++
      }
    }
    if (count > 0) {
      focusX = sumX / count
      focusY = sumY / count
    }
  }

  // The copy is drawn in the overlay of the host's root, which must not be the host itself.
  private fun View.canZoom(): Boolean = rootView.let { root -> root is ViewGroup && root !== this }

  private fun start() {
    val host = host ?: return
    if (!host.canZoom()) {
      return
    }
    val root = host.rootView as ViewGroup
    val hostLocation = IntArray(2).also { host.getLocationInWindow(it) }
    val rootLocation = IntArray(2).also { root.getLocationInWindow(it) }

    scale = 1f
    fingerScale = 1f
    dragX = 0f
    dragY = 0f
    drawnScale = 1f
    drawnDragX = 0f
    drawnDragY = 0f

    val overlay = ZoomOverlay(
        host = host,
        originX = (hostLocation[0] - rootLocation[0]).toFloat(),
        originY = (hostLocation[1] - rootLocation[1]).toFloat(),
    )
    // The whole window: the dim covers it, so each redraw of the copy damages the whole window.
    overlay.setBounds(0, 0, root.width, root.height)
    backgroundDrawable.alpha = OPAQUE
    root.overlay.add(overlay)
    this.overlay = overlay
    this.overlayRoot = root

    hide(host)
    zoomedHost = host
    host.addOnAttachStateChangeListener(detachListener)
    // Mounts everything in the tree for the zoom and keeps a list from recycling the item.
    mountingView = host.mountingViewOrSelf().also { it.setHasTransientState(true) }
    host.parent?.requestDisallowInterceptTouchEvent(true)

    state = State.ZOOMING
    zoomableEventListener?.onZoomStarted()
  }

  private fun animateBack() {
    state = State.ANIMATING
    animateFromScale = scale
    animateFromDragX = dragX
    animateFromDragY = dragY
    val spring =
        backSpring
            ?: (springSystem ?: SpringSystem.create().also { springSystem = it })
                .createSpring()
                .setSpringConfig(SpringConfig.fromOrigamiTensionAndFriction(40.0, 5.0))
                .setOvershootClampingEnabled(true)
                .also { backSpring = it }
    spring.removeListener(springListener)
    spring.addListener(springListener)
    spring.setCurrentValue(1.0, true)
    spring.endValue = 0.0
  }

  // A host detached mid-zoom may never see the spring settle: finish there instead.
  private val detachListener: View.OnAttachStateChangeListener =
      object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) = Unit

        override fun onViewDetachedFromWindow(view: View) {
          if (state != State.IDLE) {
            // Releasing the transient state remounts the tree, which must not remove views while
            // the detach is still going down it.
            finish(releaseTransientStateLater = true)
          }
        }
      }

  private val springListener: SpringListener =
      object : SpringListener {
        override fun onSpringUpdate(spring: Spring) {
          if (state != State.ANIMATING) {
            return
          }
          val progress = spring.currentValue.toFloat()
          scale = 1f + (animateFromScale - 1f) * progress
          dragX = animateFromDragX * progress
          dragY = animateFromDragY * progress
          // The background fades with the copy, so it doesn't vanish in one frame at the end.
          backgroundDrawable.alpha = (OPAQUE * progress).toInt().coerceIn(0, OPAQUE)
          overlay?.invalidateSelf()
        }

        override fun onSpringAtRest(spring: Spring) {
          spring.removeListener(this)
          if (state == State.ANIMATING) {
            finish()
          }
        }

        override fun onSpringActivate(spring: Spring) = Unit

        override fun onSpringEndStateChange(spring: Spring) = Unit
      }

  private fun finish(releaseTransientStateLater: Boolean = false) {
    overlay?.let { overlayRoot?.overlay?.remove(it) }
    overlay = null
    overlayRoot = null
    backSpring?.removeListener(springListener)
    zoomedHost?.let { host ->
      host.removeOnAttachStateChangeListener(detachListener)
      show(host)
    }
    zoomedHost = null
    mountingView?.let { view ->
      if (releaseTransientStateLater) {
        // On the main thread's handler, which runs it whether or not the view is still attached.
        mainHandler.post { view.setHasTransientState(false) }
      } else {
        view.setHasTransientState(false)
      }
    }
    mountingView = null
    claimed = false
    state = State.IDLE
    scale = 1f
    dragX = 0f
    dragY = 0f
    zoomableEventListener?.onZoomFinished()
  }

  /**
   * Hides the host from its parent, which then stops drawing it, while the copy still draws it.
   *
   * From Android 10 it is marked invisible without telling the views inside it, which would stop a
   * `TextureView` or an animation. The parent then leaves out the host's views, so a `SurfaceView`
   * inside is positioned once per frame, by the copy. Before, an empty clip hides the host, which
   * its parent still draws.
   */
  private fun hide(host: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      hostVisibility = host.visibility
      host.setTransitionVisibility(View.INVISIBLE)
      (host.parent as? View)?.invalidate()
      hiddenHostObserver =
          host.viewTreeObserver.also { it.addOnPreDrawListener(hiddenHostChangeListener) }
    } else {
      hostClipBounds = host.clipBounds
      host.clipBounds = Rect()
    }
  }

  // A view invalidated inside the hidden host isn't drawn again where the host's parent draws it,
  // so
  // its change, such as a TextureView's next frame, shows only when the copy is drawn again.
  private val hiddenHostChangeListener = ViewTreeObserver.OnPreDrawListener {
    if (zoomedHost?.isDirty == true) {
      overlay?.invalidateSelf()
    }
    true
  }

  private fun show(host: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      host.setTransitionVisibility(hostVisibility)
      (host.parent as? View)?.invalidate()
      hiddenHostObserver?.takeIf { it.isAlive }?.removeOnPreDrawListener(hiddenHostChangeListener)
      hiddenHostObserver = null
    } else {
      host.clipBounds = hostClipBounds
      hostClipBounds = null
    }
  }

  /** Beyond the allowed range the zoom gives at half the rate, so it can be pulled past it. */
  private fun resist(target: Float): Float =
      when {
        target > maxScale -> maxScale + (target - maxScale) * 0.5f
        target < 1f -> 1f - (1f - target) * 0.5f
        else -> target
      }

  private fun View.containsRaw(event: MotionEvent): Boolean {
    if (!getGlobalVisibleRect(visibleRect)) {
      return false
    }
    // The visible rect is in the window's coordinates and raw ones are on the screen: they differ
    // when the window isn't at the screen's origin, as with a dialog or split screen.
    rootView.getLocationOnScreen(rootOnScreen)
    visibleRect.offset(rootOnScreen[0], rootOnScreen[1])
    for (i in 0 until event.pointerCount) {
      if (!visibleRect.contains(event.rawXOf(i).toInt(), event.rawYOf(i).toInt())) {
        return false
      }
    }
    return true
  }

  // MotionEvent.getRawX(Int) is API 29: every pointer is offset from the screen as the first is.
  private fun MotionEvent.rawXOf(pointerIndex: Int): Float = getX(pointerIndex) + rawX - x

  private fun MotionEvent.rawYOf(pointerIndex: Int): Float = getY(pointerIndex) + rawY - y

  private fun View.mountingViewOrSelf(): View {
    var view: View? = this
    while (view != null) {
      if (view is BaseMountingView) {
        return view
      }
      view = view.parent as? View
    }
    return this
  }

  /** Draws the hidden host, scaled and moved, over a dimmed window. */
  private inner class ZoomOverlay(
      private val host: View,
      private val originX: Float,
      private val originY: Float,
  ) : Drawable() {

    override fun draw(canvas: Canvas) {
      backgroundDrawable.bounds = bounds
      backgroundDrawable.draw(canvas)
      val save = canvas.save()
      canvas.translate(originX + dragX, originY + dragY)
      canvas.scale(scale, scale, pivotX, pivotY)
      // The parent's checks of visibility and clipBounds don't apply here, so the hidden host
      // draws.
      host.draw(canvas)
      canvas.restoreToCount(save)
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Deprecated in Java") override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
  }

  private companion object {
    const val OPAQUE = 255
  }
}
