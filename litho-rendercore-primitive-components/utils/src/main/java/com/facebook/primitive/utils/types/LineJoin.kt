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
value class LineJoin internal constructor(@DoNotStrip private val value: Int) {
  companion object {
    /** The outer edges of a join meet at a sharp angle */
    val Miter: LineJoin = LineJoin(0)

    /** The outer edges of a join meet in a circular arc. */
    val Round: LineJoin = LineJoin(1)

    /** The outer edges of a join meet with a straight line. */
    val Bevel: LineJoin = LineJoin(2)

    /** Returns [LineJoin] that matches the given [name]. */
    fun fromString(name: String?): LineJoin {
      return when (name?.lowercase()) {
        "miter" -> Miter
        "round" -> Round
        "bevel" -> Bevel
        else -> DEFAULT_LINE_JOIN
      }
    }
  }

  override fun toString(): String =
      when (this) {
        Miter -> "Miter"
        Round -> "Round"
        Bevel -> "Bevel"
        else -> "Unknown"
      }

  fun applyTo(paint: Paint) {
    val paintJoin = this.toPaintJoin()
    if (paint.strokeJoin != paintJoin) {
      paint.strokeJoin = paintJoin
    }
  }

  private fun toPaintJoin(): Paint.Join =
      when (this) {
        Miter -> Paint.Join.MITER
        Round -> Paint.Join.ROUND
        Bevel -> Paint.Join.BEVEL
        else -> Paint.Join.MITER
      }
}

val DEFAULT_LINE_JOIN: LineJoin = LineJoin.Miter
