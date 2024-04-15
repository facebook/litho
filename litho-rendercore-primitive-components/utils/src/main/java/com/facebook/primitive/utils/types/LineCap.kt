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

package com.facebook.primitive.utils.types

import android.annotation.SuppressLint
import android.graphics.Paint
import com.facebook.proguard.annotations.DoNotStrip

/**
 * Styles to use for line endings.
 *
 * See [Paint.strokeCap].
 */
@SuppressLint("NotAccessedPrivateField")
@JvmInline
value class LineCap internal constructor(@DoNotStrip private val value: Int) {
  companion object {
    /** The stroke ends with the path, and does not project beyond it. */
    val Butt: LineCap = LineCap(0)

    /** The stroke projects out as a semicircle, with the center at the end of the path. */
    val Round: LineCap = LineCap(1)

    /** The stroke projects out as a square, with the center at the end of the path. */
    val Square: LineCap = LineCap(2)

    /** Returns [LineCap] that matches the given [name]. */
    fun fromString(name: String?): LineCap {
      return when (name?.lowercase()) {
        "butt" -> Butt
        "round" -> Round
        "square" -> Square
        else -> DEFAULT_LINE_CAP
      }
    }
  }

  override fun toString(): String =
      when (this) {
        Butt -> "Butt"
        Round -> "Round"
        Square -> "Square"
        else -> "Unknown"
      }

  fun applyTo(paint: Paint) {
    val paintCap = this.toPaintCap()
    if (paint.strokeCap != paintCap) {
      paint.strokeCap = paintCap
    }
  }

  private fun toPaintCap(): Paint.Cap =
      when (this) {
        Butt -> Paint.Cap.BUTT
        Round -> Paint.Cap.ROUND
        Square -> Paint.Cap.SQUARE
        else -> Paint.Cap.BUTT
      }
}

val DEFAULT_LINE_CAP: LineCap = LineCap.Butt
