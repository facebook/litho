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
import android.graphics.Path
import com.facebook.proguard.annotations.DoNotStrip

/**
 * This class contains the list of the ways a path may be filled.
 *
 * See [Path.FillType].
 */
@SuppressLint("NotAccessedPrivateField")
@JvmInline
value class FillRule internal constructor(@DoNotStrip private val value: Int) {
  companion object {
    /**
     * Also known as WINDING. Specifies that "inside" is computed by a non-zero sum of signed edge
     * crossings.
     */
    val NonZero: FillRule = FillRule(0)

    /** Specifies that "inside" is computed by an odd number of edge crossings. */
    val EvenOdd: FillRule = FillRule(1)

    /** Returns [FillRule] that matches the given [name]. */
    fun fromString(name: String?): FillRule {
      return when (name?.lowercase()) {
        "nonzero" -> NonZero
        "evenodd" -> EvenOdd
        else -> DEFAULT_FILL_RULE
      }
    }
  }

  override fun toString(): String =
      when (this) {
        NonZero -> "NonZero"
        EvenOdd -> "EvenOdd"
        else -> "Unknown"
      }

  fun applyTo(path: Path) {
    val fillType = this.toFillType()
    if (path.fillType != fillType) {
      path.fillType = fillType
    }
  }

  private fun toFillType(): Path.FillType =
      when (this) {
        NonZero -> Path.FillType.WINDING
        EvenOdd -> Path.FillType.EVEN_ODD
        else -> Path.FillType.WINDING
      }
}

val DEFAULT_FILL_RULE: FillRule = FillRule.NonZero
