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

import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FrameCallbackScheduler
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator

/**
 * The default item animator of Lazy Collections, modelled on Compose's `Modifier.animateItem`:
 * inserted items fade in, removed items fade out in place, and moved items slide, all driven by
 * springs and started together. Updated items rebind in place.
 *
 * Each item has at most one animation in flight. A new animation for an item finishes the previous
 * one and continues from the item's current alpha, offset and velocity, so interrupted animations
 * never jump. Springs write through the view's alpha and translation setters, which also keep
 * incremental mount and visibility up to date.
 *
 * An animator holds per-RecyclerView state and must never be shared. Lazy Collections pass
 * [Default] as a marker, and [SectionsRecyclerView.setItemAnimator] replaces it with a new instance
 * every time a RecyclerView is bound.
 */
class LazyCollectionItemAnimator
internal constructor(
    /** Drives the springs' frames in tests; null uses the main thread's Choreographer. */
    private val frameScheduler: FrameCallbackScheduler?,
) : SimpleItemAnimator() {

  constructor() : this(null)

  private val pending = LinkedHashMap<RecyclerView.ViewHolder, Pending>()
  private val running = HashMap<RecyclerView.ViewHolder, Running>()
  private var skipFadeIns = false
  private var skipFadeInsEnd: Pair<RecyclerView, View.OnLayoutChangeListener>? = null

  init {
    supportsChangeAnimations = false
  }

  /**
   * Rows added in the next layout of [recyclerView] show up at full opacity instead of fading in.
   * The skip ends as soon as that layout finishes, or when animations are ended, which includes the
   * RecyclerView being detached before it lays out again.
   *
   * Used when the adapter is reattached to a RecyclerView that stays on screen: RecyclerView then
   * treats every visible row as newly added, and without the skip they would all fade in.
   */
  internal fun skipFadeInsInNextLayout(recyclerView: RecyclerView) {
    skipFadeIns = true
    if (skipFadeInsEnd != null) {
      return
    }
    val listener =
        object : View.OnLayoutChangeListener {
          override fun onLayoutChange(
              view: View,
              left: Int,
              top: Int,
              right: Int,
              bottom: Int,
              oldLeft: Int,
              oldTop: Int,
              oldRight: Int,
              oldBottom: Int,
          ) {
            // A suppressed RecyclerView skips laying out its rows, so the reattach layout is still
            // to come.
            if (!recyclerView.isLayoutSuppressed) {
              stopSkippingFadeIns()
            }
          }
        }
    skipFadeInsEnd = recyclerView to listener
    recyclerView.addOnLayoutChangeListener(listener)
  }

  private fun stopSkippingFadeIns() {
    skipFadeIns = false
    skipFadeInsEnd?.let { (recyclerView, listener) ->
      recyclerView.removeOnLayoutChangeListener(listener)
    }
    skipFadeInsEnd = null
  }

  override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
    val previous = finishPrevious(holder)
    if (previous == null && !skipFadeIns) {
      holder.itemView.alpha = 0f
    }
    pending[holder] = Pending(Kind.ADD, previous ?: Velocity.ZERO)
    return true
  }

  override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
    val previous = finishPrevious(holder)
    pending[holder] = Pending(Kind.REMOVE, previous ?: Velocity.ZERO)
    return true
  }

  override fun animateMove(
      holder: RecyclerView.ViewHolder,
      fromX: Int,
      fromY: Int,
      toX: Int,
      toY: Int,
  ): Boolean {
    val view = holder.itemView
    // The item is laid out at (toX, toY) but still drawn where it was, including any offset left by
    // an animation it is being taken over from.
    val offsetX = fromX + view.translationX - toX
    val offsetY = fromY + view.translationY - toY
    val previous = finishPrevious(holder)
    view.translationX = offsetX
    view.translationY = offsetY
    if (isAtRest(view, Kind.MOVE)) {
      dispatchMoveFinished(holder)
      dispatchFinishedWhenDone()
      return false
    }
    pending[holder] = Pending(Kind.MOVE, previous ?: Velocity.ZERO)
    return true
  }

  override fun animateChange(
      oldHolder: RecyclerView.ViewHolder,
      newHolder: RecyclerView.ViewHolder?,
      fromLeft: Int,
      fromTop: Int,
      toLeft: Int,
      toTop: Int,
  ): Boolean {
    if (oldHolder === newHolder) {
      return animateMove(oldHolder, fromLeft, fromTop, toLeft, toTop)
    }
    // Change animations are off, so RecyclerView only uses two holders when the view type changed.
    // Swap them without animating.
    endAnimation(oldHolder)
    dispatchChangeFinished(oldHolder, true)
    if (newHolder != null) {
      endAnimation(newHolder)
      dispatchChangeFinished(newHolder, false)
    }
    return false
  }

  override fun runPendingAnimations() {
    // Each holder leaves `pending` only as it starts, so isRunning() and endAnimations() still see
    // the ones not started yet when an earlier holder finishes synchronously.
    for ((holder, animation) in ArrayList(pending.entries)) {
      if (pending[holder] !== animation) {
        continue // Ended or replaced from an earlier holder's callback.
      }
      pending.remove(holder)
      start(holder, animation)
    }
  }

  override fun endAnimation(item: RecyclerView.ViewHolder) {
    val kind = pending.remove(item)?.kind ?: running.remove(item)?.also { it.cancel() }?.kind
    if (kind != null) {
      resetView(item.itemView)
      dispatchFinished(item, kind)
    }
    dispatchFinishedWhenDone()
  }

  override fun endAnimations() {
    stopSkippingFadeIns()
    for (holder in pending.keys.toList() + running.keys.toList()) {
      endAnimation(holder)
    }
  }

  override fun isRunning(): Boolean = pending.isNotEmpty() || running.isNotEmpty()

  /**
   * Finishes the animation [holder] already has, pending or running, leaving the view where it is
   * drawn. Returns the velocity to continue from, or null when there was nothing in flight.
   */
  private fun finishPrevious(holder: RecyclerView.ViewHolder): Velocity? {
    val previousPending = pending.remove(holder)
    if (previousPending != null) {
      dispatchFinished(holder, previousPending.kind)
      return previousPending.velocity
    }
    val previousRunning = running.remove(holder) ?: return null
    previousRunning.cancel()
    dispatchFinished(holder, previousRunning.kind)
    return previousRunning.velocity()
  }

  private fun start(holder: RecyclerView.ViewHolder, animation: Pending) {
    val view = holder.itemView
    if (isAtRest(view, animation.kind)) {
      // Nothing left to animate, for example when an offset was reset before the animation started.
      dispatchStarting(holder, animation.kind)
      complete(holder, animation.kind)
      return
    }
    // Registered before its callbacks, so isRunning() and endAnimations() see it from them.
    val run = Running(animation.kind, frameScheduler)
    running[holder] = run
    dispatchStarting(holder, animation.kind)
    if (running[holder] !== run) {
      return
    }
    val targetAlpha = if (animation.kind == Kind.REMOVE) 0f else 1f
    if (view.alpha != targetAlpha) {
      run.alpha =
          run.spring(
              view,
              DynamicAnimation.ALPHA,
              targetAlpha,
              animation.velocity.alpha,
              DynamicAnimation.MIN_VISIBLE_CHANGE_ALPHA,
          ) {
            onSpringEnd(holder, run)
          }
    }
    // A removed item fades out where it is; only moves settle their offset.
    if (animation.kind != Kind.REMOVE && view.translationX != 0f) {
      run.x =
          run.spring(
              view,
              DynamicAnimation.TRANSLATION_X,
              0f,
              animation.velocity.x,
              DynamicAnimation.MIN_VISIBLE_CHANGE_PIXELS,
          ) {
            onSpringEnd(holder, run)
          }
    }
    if (animation.kind != Kind.REMOVE && view.translationY != 0f) {
      run.y =
          run.spring(
              view,
              DynamicAnimation.TRANSLATION_Y,
              0f,
              animation.velocity.y,
              DynamicAnimation.MIN_VISIBLE_CHANGE_PIXELS,
          ) {
            onSpringEnd(holder, run)
          }
    }
    run.start()
  }

  private fun onSpringEnd(holder: RecyclerView.ViewHolder, run: Running) {
    if (running[holder] !== run || --run.remaining > 0) {
      return
    }
    running.remove(holder)
    complete(holder, run.kind)
  }

  private fun complete(holder: RecyclerView.ViewHolder, kind: Kind) {
    // A removed item is invisible here, and RecyclerView takes it out of the hierarchy as part of
    // the dispatch below, so restoring it for reuse is never drawn.
    resetView(holder.itemView)
    dispatchFinished(holder, kind)
    dispatchFinishedWhenDone()
  }

  private fun dispatchStarting(holder: RecyclerView.ViewHolder, kind: Kind) {
    when (kind) {
      Kind.ADD -> dispatchAddStarting(holder)
      Kind.REMOVE -> dispatchRemoveStarting(holder)
      Kind.MOVE -> dispatchMoveStarting(holder)
    }
  }

  private fun dispatchFinished(holder: RecyclerView.ViewHolder, kind: Kind) {
    when (kind) {
      Kind.ADD -> dispatchAddFinished(holder)
      Kind.REMOVE -> dispatchRemoveFinished(holder)
      Kind.MOVE -> dispatchMoveFinished(holder)
    }
  }

  private fun dispatchFinishedWhenDone() {
    if (!isRunning) {
      dispatchAnimationsFinished()
    }
  }

  private enum class Kind {
    ADD,
    REMOVE,
    MOVE,
  }

  private class Velocity(val alpha: Float, val x: Float, val y: Float) {
    companion object {
      val ZERO = Velocity(0f, 0f, 0f)
    }
  }

  private class Pending(val kind: Kind, val velocity: Velocity)

  private class Running(val kind: Kind, private val frameScheduler: FrameCallbackScheduler?) {
    var alpha: SpringAnimation? = null
    var x: SpringAnimation? = null
    var y: SpringAnimation? = null
    var remaining = 0
    private var alphaVelocity = 0f
    private var xVelocity = 0f
    private var yVelocity = 0f

    fun spring(
        view: View,
        property: DynamicAnimation.ViewProperty,
        target: Float,
        startVelocity: Float,
        minimumVisibleChange: Float,
        onEnd: () -> Unit,
    ): SpringAnimation {
      remaining++
      return SpringAnimation(view, property, target).apply {
        frameScheduler?.let { setScheduler(it) }
        spring.stiffness = STIFFNESS
        spring.dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
        this.minimumVisibleChange = minimumVisibleChange
        setStartVelocity(startVelocity)
        addUpdateListener { _, _, velocity ->
          when (property) {
            DynamicAnimation.ALPHA -> alphaVelocity = velocity
            DynamicAnimation.TRANSLATION_X -> xVelocity = velocity
            else -> yVelocity = velocity
          }
        }
        addEndListener { _, _, _, _ -> onEnd() }
      }
    }

    fun start() {
      alpha?.start()
      x?.start()
      y?.start()
    }

    fun velocity(): Velocity = Velocity(alphaVelocity, xVelocity, yVelocity)

    fun cancel() {
      // The animator has already dropped this run, so the end listeners cancel() triggers are
      // no-ops.
      alpha?.cancel()
      x?.cancel()
      y?.cancel()
    }
  }

  companion object {
    /** Compose's `Spring.StiffnessMediumLow`, the default of `Modifier.animateItem`. */
    private const val STIFFNESS = 400f

    /** Marker that [resolve] turns into a new instance on every bind. Never attach it to a view. */
    @JvmField val Default: RecyclerView.ItemAnimator = LazyCollectionItemAnimator()

    @JvmStatic
    fun resolve(itemAnimator: RecyclerView.ItemAnimator?): RecyclerView.ItemAnimator? =
        if (itemAnimator === Default) LazyCollectionItemAnimator() else itemAnimator

    private fun isAtRest(view: View, kind: Kind): Boolean =
        if (kind == Kind.REMOVE) {
          view.alpha == 0f
        } else {
          view.translationX == 0f && view.translationY == 0f && view.alpha == 1f
        }

    private fun resetView(view: View) {
      view.alpha = 1f
      view.translationX = 0f
      view.translationY = 0f
    }
  }
}
