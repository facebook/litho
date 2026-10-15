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

package com.facebook.litho.animation

import android.content.Context

/** A [RuntimeValue] for dimension properties like x/y/width/height. */
class DimensionValue private constructor(private val type: Type, private val value: Float) :
    RuntimeValue {

  private enum class Type {
    ABSOLUTE,
    OFFSET,
    OFFSET_WIDTH_PERCENTAGE,
    OFFSET_HEIGHT_PERCENTAGE,
  }

  override fun resolve(resolver: Resolver, propertyHandle: PropertyHandle): Float {
    val currentValue = resolver.getCurrentState(propertyHandle)
    return when (type) {
      Type.ABSOLUTE -> value
      Type.OFFSET -> value + currentValue
      Type.OFFSET_WIDTH_PERCENTAGE -> {
        val width =
            resolver.getCurrentState(
                PropertyHandle(propertyHandle.transitionId, AnimatedProperties.WIDTH),
            )
        value / 100 * width + currentValue
      }
      Type.OFFSET_HEIGHT_PERCENTAGE -> {
        val height =
            resolver.getCurrentState(
                PropertyHandle(propertyHandle.transitionId, AnimatedProperties.HEIGHT),
            )
        value / 100 * height + currentValue
      }
    }
  }

  companion object {
    /** Returns a RuntimeValue that resolves to an absolute value. */
    @JvmStatic fun absolute(value: Float): DimensionValue = DimensionValue(Type.ABSOLUTE, value)

    /**
     * Returns a RuntimeValue that resolves to an offset relative to the current value of some mount
     * content property.
     */
    @JvmStatic fun offsetPx(value: Float): DimensionValue = DimensionValue(Type.OFFSET, value)

    /**
     * Returns a RuntimeValue that resolves to an offset relative to the current value of some mount
     * content property.
     */
    @JvmStatic
    fun offsetDip(context: Context, valueDp: Int): DimensionValue {
      val displayMetrics = context.resources.displayMetrics
      val valuePx = displayMetrics.density * valueDp
      return offsetPx(valuePx)
    }

    /**
     * Like [offsetPx], but the relative offset is based on a percentage of the mount content width.
     */
    @JvmStatic
    fun widthPercentageOffset(value: Float): DimensionValue =
        DimensionValue(Type.OFFSET_WIDTH_PERCENTAGE, value)

    /**
     * Like [offsetPx], but the relative offset is based on a percentage of the mount content
     * height.
     */
    @JvmStatic
    fun heightPercentageOffset(value: Float): DimensionValue =
        DimensionValue(Type.OFFSET_HEIGHT_PERCENTAGE, value)
  }
}
