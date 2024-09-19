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

package com.facebook.litho.drawable

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Build
import androidx.annotation.ColorInt
import com.facebook.rendercore.utils.equals
import java.util.Arrays

/** A comparable gradient drawable. */
open class ComparableGradientDrawable(
    orientation: Orientation = Orientation.TOP_BOTTOM, // default orientation of GradientDrawable
    @JvmField protected var colors: IntArray? = null
) : GradientDrawable(orientation, colors), ComparableDrawable {

  @JvmField protected var color: Int = 0

  @JvmField protected var colorStateList: ColorStateList? = null

  @JvmField protected var cornerRadius: Float = 0f

  @JvmField protected var cornerRadii: FloatArray? = null

  @JvmField protected var gradientType: Int = LINEAR_GRADIENT

  @JvmField protected var gradientRadius: Float = 0f

  @JvmField protected var shape: Int = RECTANGLE

  @JvmField protected var width: Int = -1

  @JvmField protected var height: Int = -1

  @JvmField protected var strokeWidth: Int = -1

  @JvmField protected var strokeDashWidth: Float = 0.0f

  @JvmField protected var strokeDashGap: Float = 0.0f

  @JvmField protected var strokeColor: Int = 0

  @JvmField protected var strokeColorStateList: ColorStateList? = null

  override fun equals(o: Any?): Boolean {
    if (this === o) return true
    if (o !is ComparableGradientDrawable) return false
    val that: ComparableGradientDrawable = o
    return color == that.color &&
        equals(colorStateList, that.colorStateList) &&
        cornerRadius == that.cornerRadius &&
        gradientType == that.gradientType &&
        gradientRadius == that.gradientRadius &&
        shape == that.shape &&
        width == that.width &&
        height == that.height &&
        strokeWidth == that.strokeWidth &&
        strokeDashWidth == that.strokeDashWidth &&
        strokeDashGap == that.strokeDashGap &&
        strokeColor == that.strokeColor &&
        orientationOrNullOnAPI15 == that.orientationOrNullOnAPI15 &&
        Arrays.equals(colors, that.colors) &&
        Arrays.equals(cornerRadii, that.cornerRadii) &&
        equals(strokeColorStateList, that.strokeColorStateList)
  }

  override fun hashCode(): Int {
    var result =
        arrayOf(
                orientationOrNullOnAPI15,
                color,
                colorStateList,
                cornerRadius,
                gradientType,
                gradientRadius,
                shape,
                width,
                height,
                strokeWidth,
                strokeDashWidth,
                strokeDashGap,
                strokeColor,
                strokeColorStateList)
            .contentHashCode()
    result = 31 * result + colors.contentHashCode()
    result = 31 * result + cornerRadii.contentHashCode()
    return result
  }

  private val orientationOrNullOnAPI15: Orientation?
    /**
     * On API 15, get/setOrientation didn't exist so we need to not call it. It also wasn't possible
     * to change the orientation so we don't need to compare it anyway.
     */
    get() =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
          null
        } else orientation

  override fun isEquivalentTo(other: ComparableDrawable): Boolean {
    return equals(other)
  }

  override fun setColor(color: Int) {
    super.setColor(color)
    this.color = color
  }

  override fun setColor(color: ColorStateList?) {
    super.setColor(color)
    colorStateList = color
  }

  override fun setColors(colors: IntArray?) {
    super.setColors(colors)
    this.colors = colors
  }

  override fun setCornerRadius(cornerRadius: Float) {
    super.setCornerRadius(cornerRadius)
    this.cornerRadius = cornerRadius
  }

  override fun setCornerRadii(cornerRadii: FloatArray?) {
    super.setCornerRadii(cornerRadii)
    this.cornerRadii = cornerRadii
  }

  override fun setGradientType(gradientType: Int) {
    super.setGradientType(gradientType)
    this.gradientType = gradientType
  }

  override fun setGradientRadius(gradientRadius: Float) {
    super.setGradientRadius(gradientRadius)
    this.gradientRadius = gradientRadius
  }

  override fun setShape(shape: Int) {
    super.setShape(shape)
    this.shape = shape
  }

  override fun setSize(width: Int, height: Int) {
    super.setSize(width, height)
    this.width = width
    this.height = height
  }

  override fun setStroke(width: Int, @ColorInt color: Int, dashWidth: Float, dashGap: Float) {
    super.setStroke(width, color, dashWidth, dashGap)
    strokeWidth = width
    strokeDashWidth = dashWidth
    strokeDashGap = dashGap
    strokeColor = color
  }

  override fun setStroke(
      width: Int,
      colorStateList: ColorStateList,
      dashWidth: Float,
      dashGap: Float
  ) {
    super.setStroke(width, colorStateList, dashWidth, dashGap)
    strokeWidth = width
    strokeDashWidth = dashWidth
    strokeDashGap = dashGap
    strokeColorStateList = colorStateList
  }

  companion object {

    @JvmStatic
    fun create(): ComparableGradientDrawable {
      return ComparableGradientDrawable()
    }
  }
}
