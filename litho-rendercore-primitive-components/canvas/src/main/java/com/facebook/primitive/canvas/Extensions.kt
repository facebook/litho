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

package com.facebook.primitive.canvas

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.facebook.primitive.utils.types.Point

/** Wrap the specified [block] in calls to [Canvas.saveLayer] and [Canvas.restoreToCount]. */
@Suppress("DEPRECATION")
@SuppressLint("DeprecatedMethod")
inline fun Canvas.withLayer(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    paint: Paint?,
    block: Canvas.() -> Unit
) {
  val checkpoint = saveLayer(x, y, x + width, y + height, paint, Canvas.ALL_SAVE_FLAG)
  try {
    block()
  } finally {
    restoreToCount(checkpoint)
  }
}

/**
 * This method adapts an arc's external API definition - (start degree, end degree, clockwise) - to
 * what Android's arc drawing methods expect - (start degree, sweep)
 *
 * These are the rules:
 * 1. 0 degrees is at the 3 o'clock position, 90 is at 6 o'clock, 180 is at 9 o'clock and so on.
 *    Similarly, -90 is at the 12 o-clock position, -180 is at 9 o'clock and so on.
 * 2. A start or end degree value of greater than 360 (or less than -360) is taken modulo 360. E.g.
 *    -715, -355, 5, 365, 725 all represent the same point on the circle, which is 5 degrees
 *    clockwise from the 0 degree position.
 * 3. An arc that sweeps more 360 degrees [ (end - start) >= 360 OR (end - start) <= -360] is drawn
 *    as a full circle, regardless of direction (clockwise or counter clockwise)
 */
fun standardizeArc(startDegrees: Float, endDegrees: Float, clockwise: Boolean): Pair<Float, Float> {
  var start = startDegrees
  var end = endDegrees
  var sweep = endDegrees - startDegrees
  if (sweep >= 360F || sweep <= -360F) {
    start = 0F
    sweep = 360F
  } else {
    end = endDegrees.mod(360F)
    start = startDegrees.mod(360F)
    sweep = sweep.mod(360F)
    if (!clockwise) {
      if (sweep > 0) {
        sweep -= 360F
      }
    }
  }

  return Pair(start, sweep)
}

/**
 * Adds the specified arc to the path as a new contour. The arc is defined by the rules described in
 * the [standardizeArc] method.
 */
fun Path.addArc(
    center: Point,
    radius: Float,
    startDegrees: Float,
    endDegrees: Float,
    clockwise: Boolean,
) {
  val (start, sweep) = standardizeArc(startDegrees, endDegrees, clockwise)
  arcTo(
      RectF(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
      start,
      sweep)
}

/**
 * Draws an arc onto the canvas. The arc is defined by the rules described in the [standardizeArc]
 * method.
 */
fun Canvas.drawArc(
    center: Point,
    radius: Float,
    startDegrees: Float,
    endDegrees: Float,
    clockwise: Boolean,
    paint: Paint
) {
  val (start, sweep) = standardizeArc(startDegrees, endDegrees, clockwise)
  drawArc(
      RectF(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
      start,
      sweep,
      false,
      paint)
}

/** Copies 9 values from the matrix into the array and returns the array. */
inline var Matrix.values: FloatArray
  get() {
    val values = FloatArray(9)
    getValues(values)
    return values
  }
  set(value) {
    setValues(value)
  }
