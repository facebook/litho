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

package com.facebook.litho

import android.annotation.TargetApi
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Region
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.view.MotionEvent
import android.view.View
import com.facebook.rendercore.transitions.TransitionUtils.BoundsCallback
import kotlin.math.roundToInt

/** A Drawable that wraps another drawable. */
class MatrixDrawable<T : Drawable?> : Drawable(), Drawable.Callback, Touchable, BoundsCallback {

  var mountedDrawable: T? = null
    private set

  private var matrix: DrawableMatrix? = null
  private var shouldClipRect = false
  private var width = 0
  private var height = 0

  /**
   * Sets the necessary artifacts to display the given [drawable]. This method should be called in
   * your component's @OnMount method.
   */
  @JvmOverloads
  fun mount(drawable: T?, matrix: DrawableMatrix? = null) {
    if (mountedDrawable === drawable) {
      return
    }
    mountedDrawable?.let {
      setDrawableVisibilitySafe(visible = false, restart = false)
      it.callback = null
    }
    mountedDrawable =
        drawable?.also {
          setDrawableVisibilitySafe(isVisible, restart = false)
          it.callback = this
        }
    this.matrix = matrix

    // We should clip rect if either the transformation matrix needs so or
    // if a ColorDrawable in Gingerbread is being drawn because it doesn't
    // respect its bounds.
    shouldClipRect = (this.matrix?.shouldClipRect() == true || mountedDrawable is InsetDrawable)
    invalidateSelf()
  }

  /**
   * Applies the given dimensions to the drawable. This method should be called in your
   * component's @OnBind method.
   *
   * @param width The width of the drawable to be drawn.
   * @param height The height of the drawable to be drawn.
   */
  fun bind(width: Int, height: Int) {
    this.width = width
    this.height = height
    setInnerDrawableBounds(this.width, this.height)
  }

  private fun setInnerDrawableBounds(width: Int, height: Int) {
    mountedDrawable?.setBounds(0, 0, width, height)
  }

  fun unmount() {
    mountedDrawable?.let {
      setDrawableVisibilitySafe(visible = false, restart = false)
      it.callback = null
    }
    mountedDrawable = null
    matrix = null
    shouldClipRect = false
    height = 0
    width = 0
  }

  fun getVisualBounds(): Rect {
    val untranslatedBounds = copyBounds()
    val matrix = matrix ?: return untranslatedBounds
    val translatedRectF = RectF(untranslatedBounds)
    matrix.mapRect(translatedRectF)
    return Rect(
        translatedRectF.left.roundToInt(),
        translatedRectF.top.roundToInt(),
        translatedRectF.right.roundToInt(),
        translatedRectF.bottom.roundToInt())
  }

  private fun setDrawableVisibilitySafe(visible: Boolean, restart: Boolean) {
    val drawable = mountedDrawable ?: return
    if (drawable.isVisible != visible) {
      try {
        drawable.setVisible(visible, restart)
      } catch (e: NullPointerException) {
        // Swallow. LayerDrawable on KitKat sometimes causes this, if some of its children are null.
        // This should not cause any rendering bugs, since visibility is anyway a "hint".
      }
    }
  }

  override fun draw(canvas: Canvas) {
    val drawable = mountedDrawable ?: return
    val bounds = bounds
    val saveCount = canvas.save()
    canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
    if (shouldClipRect) {
      canvas.clipRect(0, 0, bounds.width(), bounds.height())
    }
    if (matrix != null) {
      canvas.concat(matrix)
    }
    drawable.draw(canvas)
    canvas.restoreToCount(saveCount)
  }

  override fun setChangingConfigurations(configs: Int) {
    mountedDrawable?.changingConfigurations = configs
  }

  override fun getChangingConfigurations(): Int = mountedDrawable?.changingConfigurations ?: UNSET

  override fun setDither(dither: Boolean) {
    mountedDrawable?.setDither(dither)
  }

  override fun setFilterBitmap(filter: Boolean) {
    mountedDrawable?.isFilterBitmap = filter
  }

  override fun setAlpha(alpha: Int) {
    mountedDrawable?.alpha = alpha
  }

  override fun setColorFilter(cf: ColorFilter?) {
    mountedDrawable?.colorFilter = cf
  }

  override fun isStateful(): Boolean = mountedDrawable?.isStateful == true

  override fun setState(stateSet: IntArray): Boolean = mountedDrawable?.setState(stateSet) == true

  override fun getState(): IntArray = mountedDrawable?.state ?: intArrayOf()

  override fun getCurrent(): Drawable = requireNotNull(mountedDrawable).current

  override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
    val changed = super.setVisible(visible, restart)
    setDrawableVisibilitySafe(visible, restart)
    return changed
  }

  override fun getOpacity(): Int = mountedDrawable?.opacity ?: UNSET

  override fun getTransparentRegion(): Region? = mountedDrawable?.transparentRegion

  override fun getIntrinsicWidth(): Int = mountedDrawable?.intrinsicWidth ?: UNSET

  override fun getIntrinsicHeight(): Int = mountedDrawable?.intrinsicHeight ?: UNSET

  override fun getMinimumWidth(): Int = mountedDrawable?.minimumWidth ?: UNSET

  override fun getMinimumHeight(): Int = mountedDrawable?.minimumHeight ?: UNSET

  override fun getPadding(padding: Rect): Boolean = mountedDrawable?.getPadding(padding) == true

  override fun onLevelChange(level: Int): Boolean = mountedDrawable?.setLevel(level) == true

  override fun invalidateDrawable(who: Drawable) = invalidateSelf()

  override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) =
      scheduleSelf(what, `when`)

  override fun unscheduleDrawable(who: Drawable, what: Runnable) {
    unscheduleSelf(what)
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  override fun onTouchEvent(event: MotionEvent, host: View): Boolean {
    val bounds = bounds
    val x = event.x.toInt() - bounds.left
    val y = event.y.toInt() - bounds.top
    requireNotNull(mountedDrawable).setHotspot(x.toFloat(), y.toFloat())
    return false
  }

  override fun shouldHandleTouchEvent(event: MotionEvent): Boolean =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
          mountedDrawable is RippleDrawable &&
          event.actionMasked == MotionEvent.ACTION_DOWN &&
          bounds.contains(event.x.toInt(), event.y.toInt())

  override fun onWidthHeightBoundsApplied(width: Int, height: Int) {
    bind(width, height)
  }

  override fun onXYBoundsApplied(x: Int, y: Int) = Unit

  companion object {
    const val UNSET = -1
  }
}
