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

package com.facebook.litho.drawable

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Region
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.graphics.drawable.DrawableCompat

/**
 * Comparable Drawable Wrapper delegates all calls to its wrapped [Drawable].
 *
 * The wrapped [Drawable] *must* be fully released from any [View] before wrapping, otherwise
 * internal [Drawable.Callback] may be dropped.
 */
abstract class ComparableDrawableWrapper protected constructor(wrappedDrawable: Drawable) :
    Drawable(), ComparableDrawable, Drawable.Callback {

  var wrappedDrawable: Drawable = wrappedDrawable
    set(value) {
      require(value !is ComparableDrawable) { "drawable is already a ComparableDrawable" }
      field.callback = null
      field = value
      field.callback = this
    }

  override fun onBoundsChange(bounds: Rect) {
    wrappedDrawable.bounds = bounds
  }

  override fun getChangingConfigurations(): Int = wrappedDrawable.changingConfigurations

  override fun setChangingConfigurations(configs: Int) {
    wrappedDrawable.changingConfigurations = configs
  }

  @Deprecated("Deprecated API")
  override fun setDither(dither: Boolean) {
    wrappedDrawable.setDither(dither)
  }

  override fun setFilterBitmap(filter: Boolean) {
    wrappedDrawable.isFilterBitmap = filter
  }

  override fun isStateful(): Boolean = wrappedDrawable.isStateful

  override fun setState(state: IntArray): Boolean = wrappedDrawable.setState(state)

  override fun getState(): IntArray = wrappedDrawable.state

  override fun jumpToCurrentState() {
    wrappedDrawable.jumpToCurrentState()
  }

  override fun getCurrent(): Drawable = wrappedDrawable.current

  override fun setVisible(visible: Boolean, restart: Boolean): Boolean =
      super.setVisible(visible, restart) || wrappedDrawable.setVisible(visible, restart)

  override fun getTransparentRegion(): Region? = wrappedDrawable.transparentRegion

  override fun getIntrinsicWidth(): Int = wrappedDrawable.intrinsicWidth

  override fun getIntrinsicHeight(): Int = wrappedDrawable.intrinsicHeight

  override fun getMinimumWidth(): Int = wrappedDrawable.minimumWidth

  override fun getMinimumHeight(): Int = wrappedDrawable.minimumHeight

  override fun getPadding(padding: Rect): Boolean = wrappedDrawable.getPadding(padding)

  override fun onLevelChange(level: Int): Boolean = wrappedDrawable.setLevel(level)

  override fun isAutoMirrored(): Boolean = DrawableCompat.isAutoMirrored(wrappedDrawable)

  override fun setAutoMirrored(mirrored: Boolean) {
    DrawableCompat.setAutoMirrored(wrappedDrawable, mirrored)
  }

  override fun setTint(tint: Int) {
    DrawableCompat.setTint(wrappedDrawable, tint)
  }

  override fun setTintList(tint: ColorStateList?) {
    DrawableCompat.setTintList(wrappedDrawable, tint)
  }

  override fun setTintMode(mode: PorterDuff.Mode?) {
    DrawableCompat.setTintMode(wrappedDrawable, mode)
  }

  override fun setHotspot(x: Float, y: Float) {
    DrawableCompat.setHotspot(wrappedDrawable, x, y)
  }

  override fun setHotspotBounds(left: Int, top: Int, right: Int, bottom: Int) {
    DrawableCompat.setHotspotBounds(wrappedDrawable, left, top, right, bottom)
  }

  override fun draw(canvas: Canvas) {
    wrappedDrawable.draw(canvas)
  }

  override fun setAlpha(alpha: Int) {
    wrappedDrawable.alpha = alpha
  }

  override fun setColorFilter(filter: ColorFilter?) {
    wrappedDrawable.colorFilter = filter
  }

  @Deprecated("Deprecated API") override fun getOpacity(): Int = wrappedDrawable.opacity

  override fun invalidateDrawable(drawable: Drawable) {
    invalidateSelf()
  }

  override fun scheduleDrawable(drawable: Drawable, runnable: Runnable, l: Long) {
    scheduleSelf(runnable, l)
  }

  override fun unscheduleDrawable(drawable: Drawable, runnable: Runnable) {
    unscheduleSelf(runnable)
  }
}
