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
import android.graphics.BlendMode
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import androidx.annotation.RequiresApi
import com.facebook.proguard.annotations.DoNotStrip

/** This class contains the list of alpha compositing and blending modes. */
@SuppressLint("NotAccessedPrivateField")
@JvmInline
value class BlendingMode internal constructor(@DoNotStrip private val value: Int) {

  companion object {
    /**
     * Destination pixels covered by the source are cleared to 0.
     *
     * ```
     * alpha_out = 0
     *
     * C_out = 0
     * ```
     */
    val Clear: BlendingMode = BlendingMode(0)

    /**
     * The source pixels replace the destination pixels.
     *
     * ```
     * alpha_out = alpha_src
     *
     * C_out = C_src
     * ```
     */
    val Src: BlendingMode = BlendingMode(1)

    /**
     * The source pixels are drawn over the destination pixels.
     *
     * ```
     * alpha_out = alpha_src + (1 - alpha_src) * alpha_dst
     *
     * C_out = C_src + (1 - alpha_src) * C_dst
     * ```
     */
    val SrcOver: BlendingMode = BlendingMode(3)

    /**
     * The source pixels are drawn behind the destination pixels.
     *
     * ```
     * alpha_out = alpha_dst + (1 - alpha_dst) * alpha_src
     *
     * C_out = C_dst + (1 - alpha_dst) * C_src
     * ```
     */
    val DstOver: BlendingMode = BlendingMode(4)

    /**
     * Keeps the source pixels that cover the destination pixels, discards the remaining source and
     * destination pixels.
     *
     * ```
     * alpha_out = alpha_src * alpha_dst
     *
     * C_out = C_src * alpha_dst
     * ```
     */
    val SrcIn: BlendingMode = BlendingMode(5)

    /**
     * Keeps the destination pixels that cover source pixels, discards the remaining source and
     * destination pixels.
     *
     * ```
     * alpha_out = alpha_src * alpha_dst
     *
     * C_out = C_dst * alpha_src
     * ```
     */
    val DstIn: BlendingMode = BlendingMode(6)

    /**
     * Keeps the source pixels that do not cover destination pixels. Discards source pixels that
     * cover destination pixels. Discards all destination pixels.
     *
     * ```
     * alpha_out = (1 - alpha_dst) * alpha_src
     *
     * C_out = (1 - alpha_dst) * C_src
     * ```
     */
    val SrcOut: BlendingMode = BlendingMode(7)

    /**
     * Keeps the destination pixels that are not covered by source pixels. Discards destination
     * pixels that are covered by source pixels. Discards all source pixels.
     *
     * ```
     * alpha_out = (1 - alpha_src) * alpha_dst
     *
     * C_out = (1 - alpha_src) * C_dst
     * ```
     */
    val DstOut: BlendingMode = BlendingMode(8)

    /**
     * Discards the source pixels that do not cover destination pixels. Draws remaining source
     * pixels over destination pixels.
     *
     * ```
     * alpha_out = alpha_dst
     *
     * C_out = alpha_dst * C_src + (1 - alpha_src) * C_dst
     * ```
     */
    val SrcAtop: BlendingMode = BlendingMode(9)

    /**
     * Discards the destination pixels that are not covered by source pixels. Draws remaining
     * destination pixels over source pixels.
     *
     * ```
     * alpha_out = alpha_src
     *
     * C_out = alpha_src * C_dst + (1 - alpha_dst) * C_src
     * ```
     */
    val DstAtop: BlendingMode = BlendingMode(10)

    /**
     * Discards the source and destination pixels where source pixels cover destination pixels.
     * Draws remaining source pixels.
     *
     * ```
     * alpha_out = (1 - alpha_dst) * alpha_src + (1 - alpha_src) * alpha_dst
     *
     * C_out = (1 - alpha_dst) * C_src + (1 - alpha_src) * C_dst
     * ```
     */
    val Xor: BlendingMode = BlendingMode(11)

    /**
     * Retains the smallest component of the source and destination pixels.
     *
     * ```
     * alpha_out = alpha_src + alpha_dst - alpha_src * alpha_dst
     *
     * C_out = (1 - alpha_dst) * C_src + (1 - alpha_src) * C_dst + min(C_src, C_dst)
     * ```
     */
    val Darken: BlendingMode = BlendingMode(16)

    /**
     * Retains the largest component of the source and destination pixel.
     *
     * ```
     * alpha_out = alpha_src + alpha_dst - alpha_src * alpha_dst
     *
     * C_out = (1 - alpha_dst) * C_src + (1 - alpha_src) * C_dst + max(C_src, C_dst)
     * ```
     */
    val Lighten: BlendingMode = BlendingMode(17)

    /**
     * Multiplies the source and destination pixels.
     *
     * ```
     * alpha_out = alpha_src + alpha_dst - alpha_src * alpha_dst
     *
     * C_out = C_src * (1 - alpha_dst) + C_dst * (1 - alpha_src) + (C_src * C_dst)
     * ```
     */
    val Multiply: BlendingMode = BlendingMode(13)

    /**
     * Adds the source and destination pixels, then subtracts the source pixels multiplied by the
     * destination.
     *
     * ```
     * alpha_out = alpha_src + alpha_dst - alpha_src * alpha_dst`
     *
     * C_out = C_src + C_dst - C_src * C_dst
     * ```
     */
    val Screen: BlendingMode = BlendingMode(14)

    /**
     * Multiplies or screens the source and destination depending on the destination color.
     *
     * ```
     * alpha_out = alpha_src + alpha_dst - alpha_src * alpha_dst
     *
     * C_out = if(2 * C_dst < alpha_dst) {
     *   2 * C_src * C_dst
     * } else {
     *   alpha_src * alpha_dst - 2 (alpha_dst - C_src) * (alpha_src - C_dst)
     * }
     * ```
     */
    val Overlay: BlendingMode = BlendingMode(15)

    /** Returns [BlendingMode] that matches the given [name]. */
    fun fromString(name: String?): BlendingMode {
      return when (name?.lowercase()) {
        "clear" -> Clear
        "src" -> Src
        "srcOver" -> SrcOver
        "dstOver" -> DstOver
        "srcIn" -> SrcIn
        "dstIn" -> DstIn
        "srcOut" -> SrcOut
        "dstOut" -> DstOut
        "srcAtop" -> SrcAtop
        "dstAtop" -> DstAtop
        "xor" -> Xor
        "screen" -> Screen
        "overlay" -> Overlay
        "darken" -> Darken
        "lighten" -> Lighten
        "multiply" -> Multiply
        else -> DEFAULT_BLENDING_MODE
      }
    }
  }

  override fun toString(): String =
      when (this) {
        Clear -> "Clear"
        Src -> "Src"
        SrcOver -> "SrcOver"
        DstOver -> "DstOver"
        SrcIn -> "SrcIn"
        DstIn -> "DstIn"
        SrcOut -> "SrcOut"
        DstOut -> "DstOut"
        SrcAtop -> "SrcAtop"
        DstAtop -> "DstAtop"
        Xor -> "Xor"
        Screen -> "Screen"
        Overlay -> "Overlay"
        Darken -> "Darken"
        Lighten -> "Lighten"
        Multiply -> "Multiply"
        else -> "Unknown" // Should not get here since we have an internal constructor
      }

  fun applyTo(paint: Paint) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val blendMode = this.toBlendMode()
      if (paint.blendMode != blendMode) {
        paint.blendMode = blendMode
      }
    } else {
      val xfermode = PorterDuffXfermode(toPorterDuff())
      if (paint.xfermode != xfermode) {
        paint.xfermode = xfermode
      }
    }
  }

  private fun toPorterDuff(): PorterDuff.Mode {
    return when (this) {
      Clear -> PorterDuff.Mode.CLEAR
      Src -> PorterDuff.Mode.SRC
      SrcOver -> PorterDuff.Mode.SRC_OVER
      DstOver -> PorterDuff.Mode.DST_OVER
      SrcIn -> PorterDuff.Mode.SRC_IN
      DstIn -> PorterDuff.Mode.DST_IN
      SrcOut -> PorterDuff.Mode.SRC_OUT
      DstOut -> PorterDuff.Mode.DST_OUT
      SrcAtop -> PorterDuff.Mode.SRC_ATOP
      DstAtop -> PorterDuff.Mode.DST_ATOP
      Xor -> PorterDuff.Mode.XOR
      Darken -> PorterDuff.Mode.DARKEN
      Lighten -> PorterDuff.Mode.LIGHTEN
      Multiply -> PorterDuff.Mode.MULTIPLY
      Screen -> PorterDuff.Mode.SCREEN
      Overlay -> PorterDuff.Mode.OVERLAY
      else -> PorterDuff.Mode.SRC_OVER
    }
  }

  @RequiresApi(Build.VERSION_CODES.Q)
  private fun toBlendMode(): BlendMode {
    return when (this) {
      Clear -> BlendMode.CLEAR
      Src -> BlendMode.SRC
      SrcOver -> BlendMode.SRC_OVER
      DstOver -> BlendMode.DST_OVER
      SrcIn -> BlendMode.SRC_IN
      DstIn -> BlendMode.DST_IN
      SrcOut -> BlendMode.SRC_OUT
      DstOut -> BlendMode.DST_OUT
      SrcAtop -> BlendMode.SRC_ATOP
      DstAtop -> BlendMode.DST_ATOP
      Xor -> BlendMode.XOR
      Darken -> BlendMode.DARKEN
      Lighten -> BlendMode.LIGHTEN
      Multiply -> BlendMode.MULTIPLY
      Screen -> BlendMode.SCREEN
      Overlay -> BlendMode.OVERLAY
      else -> BlendMode.SRC_OVER
    }
  }
}

val DEFAULT_BLENDING_MODE: BlendingMode = BlendingMode.SrcOver
