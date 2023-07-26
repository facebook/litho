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

import android.view.View
import androidx.annotation.IntDef
import com.facebook.rendercore.FastMath
import com.facebook.yoga.YogaMeasureMode
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import kotlin.math.min

/**
 * A SizeSpec encapsulates the layout requirements passed from parent to child. Each SizeSpec
 * represents a requirement for either the width or the height. A SizeSpec is comprised of a size
 * and a mode. There are two possible modes:
 *
 * UNSPECIFIED : The parent has not imposed any constraint on the child. It can be whatever size it
 * wants.
 *
 * EXACTLY : The parent has determined an exact size for the child. The child is going to be given
 * those bounds regardless of how big it wants to be. SizeSpecs are implemented as ints to reduce
 * object allocation. This class is provided to pack and unpack the tuple into the int.
 */
object SizeSpec {

  /**
   * Size specification mode: The parent has not imposed any constraint on the child. It can be
   * whatever size it wants.
   */
  const val UNSPECIFIED = View.MeasureSpec.UNSPECIFIED

  /**
   * Size specification mode: The parent has determined an exact size for the child. The child is
   * going to be given those bounds regardless of how big it wants to be.
   */
  const val EXACTLY = View.MeasureSpec.EXACTLY

  /** Size specification mode: The child can be as large as it wants up to the specified size. */
  const val AT_MOST = View.MeasureSpec.AT_MOST

  /**
   * Creates a size specification based on the supplied size and mode.
   *
   * The mode must always be one of the following:
   * * [com.facebook.litho.SizeSpec#UNSPECIFIED]
   * * [com.facebook.litho.SizeSpec#EXACTLY]
   *
   * **Note:** On API level 17 and lower, makeMeasureSpec's implementation was such that the order
   * of arguments did not matter and overflow in either value could impact the resulting
   * MeasureSpec. [android.widget.RelativeLayout] was affected by this bug. Apps targeting API
   * levels greater than 17 will get the fixed, more strict behavior.
   *
   * @param size the size of the size specification
   * @param mode the mode of the size specification
   * @return the size specification based on size and mode
   */
  @JvmStatic
  fun makeSizeSpec(size: Int, @MeasureSpecMode mode: Int): Int =
      View.MeasureSpec.makeMeasureSpec(size, mode)

  /**
   * Extracts the mode from the supplied size specification.
   *
   * @param sizeSpec the size specification to extract the mode from
   * @return [com.facebook.litho.SizeSpec#UNSPECIFIED] or [com.facebook.litho.SizeSpec#EXACTLY]
   */
  @JvmStatic fun getMode(sizeSpec: Int): Int = View.MeasureSpec.getMode(sizeSpec)

  /**
   * Extracts the size from the supplied size specification.
   *
   * @param sizeSpec the size specification to extract the size from
   * @return the size in pixels defined in the supplied size specification
   */
  @JvmStatic fun getSize(sizeSpec: Int): Int = View.MeasureSpec.getSize(sizeSpec)

  /**
   * Returns a String representation of the specified measure specification.
   *
   * @param sizeSpec the size specification to convert to a String
   * @return a String with the following format: "MeasureSpec: MODE SIZE"
   */
  @JvmStatic fun toString(sizeSpec: Int): String = View.MeasureSpec.toString(sizeSpec)

  /**
   * Returns a simple String representation of the specified measure specification.
   *
   * @param sizeSpec the size specification to convert to a String
   * @return a String with the following format: "MODE SIZE"
   */
  @JvmStatic
  fun toSimpleString(sizeSpec: Int): String {
    val mode = getMode(sizeSpec)
    val size = getSize(sizeSpec)
    return buildString {
      when (mode) {
        UNSPECIFIED -> append("UNSPECIFIED ")
        EXACTLY -> append("EXACTLY ")
        AT_MOST -> append("AT_MOST ")
        else -> append(mode).append(" ")
      }
      append(size)
    }
  }

  /**
   * Resolve a size spec given a preferred size.
   *
   * @param sizeSpec The spec to resolve.
   * @param preferredSize The preferred size.
   * @return The resolved size.
   */
  @JvmStatic
  fun resolveSize(sizeSpec: Int, preferredSize: Int): Int =
      when (getMode(sizeSpec)) {
        EXACTLY -> getSize(sizeSpec)
        AT_MOST -> min(getSize(sizeSpec), preferredSize)
        UNSPECIFIED -> preferredSize
        else -> throw IllegalStateException("Unexpected size mode: ${getMode(sizeSpec)}")
      }

  @JvmStatic
  fun makeSizeSpecFromCssSpec(cssSize: Float, cssMode: YogaMeasureMode): Int =
      when (cssMode) {
        YogaMeasureMode.EXACTLY -> makeSizeSpec(FastMath.round(cssSize), EXACTLY)
        YogaMeasureMode.UNDEFINED -> makeSizeSpec(0, UNSPECIFIED)
        YogaMeasureMode.AT_MOST -> makeSizeSpec(FastMath.round(cssSize), AT_MOST)
        else -> throw IllegalArgumentException("Unexpected YogaMeasureMode: $cssMode")
      }

  @IntDef(UNSPECIFIED, EXACTLY, AT_MOST)
  @Retention(AnnotationRetention.SOURCE)
  annotation class MeasureSpecMode
}
