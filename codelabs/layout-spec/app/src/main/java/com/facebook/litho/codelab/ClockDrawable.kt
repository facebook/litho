/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho.codelab

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

class ClockDrawable : Drawable() {
  companion object {
    const val ONE_MINUTE = 60L * 1000
    const val ONE_HOUR = 60 * ONE_MINUTE
    const val TWELVE_HOURS = 12 * ONE_HOUR
  }

  private val paintThin = buildPaint(6)
  private val paintMedium = buildPaint(9)
  private val paintWide = buildPaint(12)

  private var longHandAngle = 0
  private var shortHandAngle = 0

  var radius = 0

  private fun buildPaint(strokeWidthPx: Int): Paint {
    return Paint().apply {
      color = Color.BLACK
      isAntiAlias = true
      style = Paint.Style.STROKE
      strokeWidth = strokeWidthPx.toFloat()
    }
  }

  private fun drawHand(
      canvas: Canvas, degrees: Float, length: Int, paint: Paint, centerX: Float, centerY: Float
  ) {
    // Save and then rotate canvas
    canvas.run {
      val savedCount = save()
      rotate(degrees, centerX, centerY)

      // Draw a hand
      drawLine(centerX, centerY, centerX, centerY - length, paint)

      // Restore canvas
      restoreToCount(savedCount)
    }
  }

  fun setTime(timeMillis: Long) {
    val inHourProgress = timeMillis.toFloat() % ONE_HOUR / ONE_HOUR
    longHandAngle = (360 * inHourProgress).toInt()

    val inTwelveHoursProgress = timeMillis.toFloat() / TWELVE_HOURS
    shortHandAngle = (360 * inTwelveHoursProgress).toInt()
  }

  override fun draw(canvas: Canvas) {
    val bounds = bounds

    canvas.run {
      val saveCount = save()
      translate(bounds.left.toFloat(), bounds.top.toFloat())

      drawCircle(radius.toFloat(), radius.toFloat(), radius.toFloat(), paintWide)
      drawHand(
          this,
          longHandAngle.toFloat(),
          radius * 2 / 3,
          paintThin,
          radius.toFloat(),
          radius.toFloat())
      drawHand(
          this,
          shortHandAngle.toFloat(),
          radius / 3,
          paintMedium,
          radius.toFloat(),
          radius.toFloat())

      restoreToCount(saveCount)
    }
  }

  override fun setAlpha(alpha: Int) {}

  override fun setColorFilter(colorFilter: ColorFilter?) {}

  override fun getOpacity(): Int {
    return PixelFormat.OPAQUE
  }
}
