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

package com.facebook.mountable.canvas

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint

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
