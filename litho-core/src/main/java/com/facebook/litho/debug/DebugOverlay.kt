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

package com.facebook.litho.debug

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import com.facebook.litho.ThreadUtils
import kotlin.math.min

/**
 * Draws a row of rectangles starting from the top left corner; the number of rectangles represents
 * the number resolves. If the rectangle is red is color then that resolved results was calculated
 * on the main thread. If the overlay is red in color then the most recent layout was on the main
 * thread. The debug overlay tracks layout history, but doesn't show it on the overlay.
 */
class DebugOverlay(private val isMainThreadLayouts: List<Boolean>) : Drawable() {

  companion object {

    @JvmField var isEnabled: Boolean = false

    private const val COLOR_RED_SEMITRANSPARENT = 0x22FF0000
    private const val COLOR_GREEN_SEMITRANSPARENT = 0x2200FF00
    private const val COLOR_RED_OPAQUE = 0xCCFF0000.toInt()
    private const val COLOR_GREEN_OPAQUE = 0xCC00FF00.toInt()
    private const val BOX_WIDTH_PX = 8
    private const val BOX_HEIGHT_PX = 10
    private const val BOX_MARGIN_BETWEEN_PX = 4
    private const val BOX_MARGIN_EDGE_PX = 4
    private const val TEXT_SIZE_PX = 20f

    private val layoutThreadHistory: MutableMap<Int, MutableList<Boolean>> by lazy {
      mutableMapOf()
    }

    private val resolveThreadHistory: MutableMap<Int, MutableList<Boolean>> by lazy {
      mutableMapOf()
    }

    @JvmStatic
    @Synchronized
    fun updateLayoutHistory(id: Int) {
      val history: MutableList<Boolean> = layoutThreadHistory.getOrPut(id) { mutableListOf() }
      history.add(ThreadUtils.isMainThread())
    }

    @JvmStatic
    @Synchronized
    fun updateResolveHistory(id: Int) {
      val history: MutableList<Boolean> = resolveThreadHistory.getOrPut(id) { mutableListOf() }
      history.add(ThreadUtils.isMainThread())
    }

    @JvmStatic
    @Synchronized
    fun getDebugOverlay(id: Int): DebugOverlay {
      return DebugOverlay(resolveThreadHistory.getOrPut(id) { mutableListOf() })
    }
  }

  private val text: String
  private val textPaint: Paint = Paint()
  private val colorPaint: Paint = Paint()
  private val overlayColor: Int

  init {
    textPaint.color = Color.BLACK
    textPaint.style = Paint.Style.FILL
    textPaint.textSize = TEXT_SIZE_PX
    textPaint.textAlign = Paint.Align.LEFT
    textPaint.isAntiAlias = true
    val size = isMainThreadLayouts.size
    if (size > 0) {
      text = "${isMainThreadLayouts.size}x"
      overlayColor =
          if (isMainThreadLayouts[size - 1]) {
            COLOR_RED_SEMITRANSPARENT
          } else {
            COLOR_GREEN_SEMITRANSPARENT
          }
    } else {
      text = ""
      overlayColor = Color.TRANSPARENT
    }
  }

  override fun draw(canvas: Canvas) {
    val bounds = bounds

    // draw overlay background
    colorPaint.color = overlayColor
    canvas.drawRect(bounds, colorPaint)

    val count = isMainThreadLayouts.size
    val leftEnd = bounds.left + BOX_MARGIN_EDGE_PX
    val rightEnd = bounds.right
    val top = bounds.top + BOX_MARGIN_EDGE_PX
    val bottom = min(top + BOX_HEIGHT_PX, bounds.bottom)
    var i = 0
    var left: Int
    var right: Int

    while (i < count) {

      // calculate position for next render history rect
      left = leftEnd + i * (BOX_WIDTH_PX + BOX_MARGIN_BETWEEN_PX)
      right = left + BOX_WIDTH_PX

      // if position outside bounds then exit
      if (right >= rightEnd) {
        break
      }

      if (isMainThreadLayouts[i]) {
        colorPaint.color = COLOR_RED_OPAQUE
      } else {
        colorPaint.color = COLOR_GREEN_OPAQUE
      }

      // draw render history rect
      canvas.drawRect(
          left.toFloat(),
          top.toFloat(),
          right.toFloat(),
          bottom.toFloat(),
          colorPaint,
      )

      i++
    }

    // draw render count text
    canvas.drawText(
        text,
        leftEnd + BOX_MARGIN_EDGE_PX.toFloat(),
        top + BOX_HEIGHT_PX + TEXT_SIZE_PX + 2,
        textPaint,
    )
  }

  override fun setAlpha(alpha: Int) = Unit

  override fun setColorFilter(cf: ColorFilter?) = Unit

  override fun getOpacity(): Int {
    return PixelFormat.TRANSLUCENT
  }
}
