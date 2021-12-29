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

package com.facebook.samples.litho.kotlin.animations.dynamicprops

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.TypedValue
import android.view.View
import androidx.annotation.Dimension
import androidx.annotation.Dimension.DP
import androidx.annotation.Px
import java.util.concurrent.TimeUnit
import kotlin.math.min

class SimpleClockView(context: Context) : View(context) {
  private val oneHourMs = TimeUnit.HOURS.toMillis(1)
  private val twelveHours = TimeUnit.HOURS.toMillis(12)
  @Px
  private val stroke: Float =
      TypedValue.applyDimension(
          TypedValue.COMPLEX_UNIT_DIP, STROKE_WIDTH_DP, resources.displayMetrics)
  private val paint: Paint =
      Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = stroke
      }
  private var center = PointF(0f, 0f)
  @Px private var radius = 0f
  @Px private var shortHandLength = 0f
  @Px private var longHandLength = 0f
  private var shortHandAngle = 0f
  private var longHandAngle = 0f

  var time: Long = 0
    set(time) {
      if (field == time) {
        return
      }
      field = time % twelveHours
      val inHourProgress = time.toFloat() % oneHourMs / oneHourMs
      longHandAngle = 360 * inHourProgress
      val inTwelveHoursProgress = time.toFloat() / twelveHours
      shortHandAngle = 360 * inTwelveHoursProgress
      invalidate()
    }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    center.x = w / 2f
    center.y = h / 2f
    val size = min(w, h)
    radius = (size - stroke) / 2f
    shortHandLength = radius * 0.4f
    longHandLength = radius * 0.7f
  }

  override fun onDraw(canvas: Canvas) {
    canvas.drawCircle(center.x, center.y, radius, paint)
    drawHand(canvas, longHandAngle, longHandLength)
    drawHand(canvas, shortHandAngle, shortHandLength)
  }

  private fun drawHand(canvas: Canvas, angle: Float, length: Float) =
      with(canvas) {
        save()
        rotate(angle, center.x, center.y)
        drawLine(center.x, center.y, center.x, center.y - length, paint)
        restore()
      }

  companion object {
    @Dimension(unit = DP) private const val STROKE_WIDTH_DP = 6f
  }
}
