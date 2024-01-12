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

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import com.facebook.litho.config.ComponentsConfiguration

/** Draw operations used in developer options. */
internal object DebugDraw {

  private const val INTERACTIVE_VIEW_COLOR = 0x66C29BFF
  private const val TOUCH_DELEGATE_COLOR = 0x44D3FFCE
  private const val MOUNT_BORDER_COLOR = -0x66010000
  private const val MOUNT_BORDER_COLOR_HOST = -0x6600ff01
  private const val MOUNT_CORNER_COLOR = -0xffff01
  private const val MOUNT_CORNER_COLOR_HOST = -0xff0001

  private val interactiveViewPaint: Paint by lazy {
    Paint().apply { color = INTERACTIVE_VIEW_COLOR }
  }
  private val touchDelegatePaint: Paint by lazy { Paint().apply { color = TOUCH_DELEGATE_COLOR } }

  private val mountBoundsRect: Rect by lazy { Rect() }
  private val mountBoundsBorderPaint: Paint by lazy {
    Paint().apply {
      style = Paint.Style.STROKE
      strokeWidth = 1f
    }
  }

  private val mountBoundsCornerPaint: Paint by lazy {
    Paint().apply {
      style = Paint.Style.FILL
      strokeWidth = 2f
    }
  }

  @JvmStatic
  fun draw(host: ComponentHost, canvas: Canvas) {
    if (ComponentsConfiguration.debugHighlightInteractiveBounds) {
      highlightInteractiveBounds(host, canvas)
    }
    if (ComponentsConfiguration.debugHighlightMountBounds) {
      highlightMountBounds(host, canvas)
    }
  }

  private fun highlightInteractiveBounds(host: ComponentHost, canvas: Canvas) {
    // 1. Highlight root, if applicable.
    if (isInteractive(host)) {
      canvas.drawRect(0f, 0f, host.width.toFloat(), host.height.toFloat(), interactiveViewPaint)
    }

    // 2. Highlight non-host interactive mounted views.
    for (i in host.mountItemCount - 1 downTo 0) {
      val item = host.getMountItemAt(i)
      val component = LithoRenderUnit.getRenderUnit(item).component
      if (!LithoRenderUnit.isMountableView(item.renderTreeNode.renderUnit) ||
          Component.isHostSpec(component)) {
        continue
      }
      val view = item.content as View
      if (!isInteractive(view)) {
        continue
      }
      canvas.drawRect(
          view.left.toFloat(),
          view.top.toFloat(),
          view.right.toFloat(),
          view.bottom.toFloat(),
          touchDelegatePaint)
    }

    // 3. Highlight expanded touch bounds.
    val touchDelegate = host.touchExpansionDelegate
    touchDelegate?.draw(canvas, touchDelegatePaint)
  }

  private fun highlightMountBounds(host: ComponentHost, canvas: Canvas) {
    val resources = host.resources
    for (i in host.mountItemCount - 1 downTo 0) {
      val item = host.getMountItemAt(i)
      val component = LithoRenderUnit.getRenderUnit(item).component
      val content = item.content
      if (!shouldHighlight(component)) {
        continue
      }
      if (content is View) {
        mountBoundsRect.set(content.left, content.top, content.right, content.bottom)
      } else if (content is Drawable) {
        mountBoundsRect.set(content.bounds)
      }

      mountBoundsBorderPaint.color = getBorderColor(component)
      drawMountBoundsBorder(canvas, mountBoundsBorderPaint, mountBoundsRect)
      mountBoundsCornerPaint.color = getCornerColor(component)
      drawMountBoundsCorners(
          canvas,
          mountBoundsCornerPaint,
          mountBoundsRect,
          mountBoundsCornerPaint.strokeWidth.toInt(),
          Math.min(
              Math.min(mountBoundsRect.width(), mountBoundsRect.height()) / 3,
              dipToPixels(resources, 12)))
    }
  }

  private fun drawMountBoundsBorder(canvas: Canvas, paint: Paint, bounds: Rect) {
    val inset = paint.strokeWidth.toInt() / 2
    bounds.inset(inset, inset)
    canvas.drawRect(bounds, paint)
  }

  private fun drawMountBoundsCorners(
      canvas: Canvas,
      paint: Paint,
      bounds: Rect,
      cornerLength: Int,
      cornerWidth: Int
  ) {
    drawCorner(canvas, paint, bounds.left, bounds.top, cornerLength, cornerLength, cornerWidth)
    drawCorner(canvas, paint, bounds.left, bounds.bottom, cornerLength, -cornerLength, cornerWidth)
    drawCorner(canvas, paint, bounds.right, bounds.top, -cornerLength, cornerLength, cornerWidth)
    drawCorner(
        canvas, paint, bounds.right, bounds.bottom, -cornerLength, -cornerLength, cornerWidth)
  }

  private fun shouldHighlight(component: Component): Boolean =
      // Don't highlight bounds of background/foreground components.
      component !is DrawableComponent<*>

  private fun dipToPixels(res: Resources, dips: Int): Int {
    val scale = res.displayMetrics.density
    return (dips * scale + 0.5f).toInt()
  }

  private fun getBorderColor(component: Component): Int =
      if (Component.isHostSpec(component)) MOUNT_BORDER_COLOR_HOST else MOUNT_BORDER_COLOR

  private fun getCornerColor(component: Component): Int =
      if (Component.isHostSpec(component)) MOUNT_CORNER_COLOR_HOST else MOUNT_CORNER_COLOR

  private fun sign(x: Float): Int = if (x >= 0) 1 else -1

  private fun drawCorner(
      c: Canvas,
      paint: Paint,
      x: Int,
      y: Int,
      dx: Int,
      dy: Int,
      cornerWidth: Int
  ) {
    drawCornerLine(c, paint, x, y, x + dx, y + cornerWidth * sign(dy.toFloat()))
    drawCornerLine(c, paint, x, y, x + cornerWidth * sign(dx.toFloat()), y + dy)
  }

  private fun drawCornerLine(
      canvas: Canvas,
      paint: Paint,
      left: Int,
      top: Int,
      right: Int,
      bottom: Int
  ) {
    var left = left
    var top = top
    var right = right
    var bottom = bottom
    if (left > right) {
      val tmp = left
      left = right
      right = tmp
    }
    if (top > bottom) {
      val tmp = top
      top = bottom
      bottom = tmp
    }
    canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
  }

  private fun isInteractive(view: View): Boolean =
      view.hasOnClickListeners() ||
          LithoViewAttributesExtension.getComponentLongClickListener(view) != null ||
          LithoViewAttributesExtension.getComponentTouchListener(view) != null
}
