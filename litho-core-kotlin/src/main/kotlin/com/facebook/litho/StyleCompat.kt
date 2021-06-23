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

package com.facebook.litho

import com.facebook.litho.AccessibilityRole.AccessibilityRoleType
import com.facebook.litho.accessibility.accessibilityRole
import com.facebook.litho.core.height
import com.facebook.litho.core.heightPercent
import com.facebook.litho.core.margin
import com.facebook.litho.core.maxHeight
import com.facebook.litho.core.maxWidth
import com.facebook.litho.core.minHeight
import com.facebook.litho.core.minWidth
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.core.widthPercent
import com.facebook.litho.flexbox.alignSelf
import com.facebook.litho.flexbox.border
import com.facebook.litho.flexbox.flex
import com.facebook.litho.flexbox.position
import com.facebook.litho.flexbox.positionType
import com.facebook.litho.view.wrapInView
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaPositionType
import com.facebook.yoga.YogaWrap

/**
 * Backwards compatibility for a Kotlin Component to accept a [Style] and apply it to a Java Spec
 * Component. Using this builder method is the equivalent of setting all common props the Style
 * defines.
 */
fun <T : Component.Builder<T>> Component.Builder<T>.kotlinStyle(style: Style?): T {
  style?.applyToComponent(context!!.resourceResolver, component)
  return getThis()
}

/**
 * Backwards compatibility to allow Java code to define Styles used by Kotlin components
 *
 * Usage:
 * ```
 * new MyKotlinComponent(
 *     prop1,
 *     prop2,
 *     StyleCompat.flexShrink(0f)
 *         .marginDip(YogaEdge.ALL, 4f)
 *         .build())
 * ```
 */
object StyleCompat {

  /** @see [JavaStyle.flexShrink] */
  @JvmStatic fun flexShrink(value: Float): JavaStyle = JavaStyle().flexShrink(value)

  /** @see [JavaStyle.flexGrow] */
  @JvmStatic fun flexGrow(value: Float): JavaStyle = JavaStyle().flexGrow(value)

  /** @see [JavaStyle.flexBasisDip]] */
  @JvmStatic fun flexBasisPx(value: Float): JavaStyle = JavaStyle().flexBasisDip(value)

  /** @see [JavaStyle.flexBasisPx] */
  @JvmStatic fun flexBasisPx(value: Int): JavaStyle = JavaStyle().flexBasisPx(value)

  /** @see [JavaStyle.widthDip] */
  @JvmStatic fun widthDip(value: Float): JavaStyle = JavaStyle().widthDip(value)

  /** @see [JavaStyle.heightDip] */
  @JvmStatic fun heightDip(value: Float): JavaStyle = JavaStyle().heightDip(value)

  /** @see [JavaStyle.widthPx] */
  @JvmStatic fun widthPx(value: Int): JavaStyle = JavaStyle().widthPx(value)

  /** @see [JavaStyle.heightPx] */
  @JvmStatic fun heightPx(value: Int): JavaStyle = JavaStyle().heightPx(value)

  /** @see [JavaStyle.widthPercent] */
  @JvmStatic fun widthPercent(value: Float): JavaStyle = JavaStyle().widthPercent(value)

  /** @see [JavaStyle.heightPercent] */
  @JvmStatic fun heightPercent(value: Float): JavaStyle = JavaStyle().heightPercent(value)

  /** @see [JavaStyle.maxWidthDip] */
  @JvmStatic fun maxWidthDip(value: Float): JavaStyle = JavaStyle().maxWidthDip(value)

  /** @see [JavaStyle.maxHeightDip] */
  @JvmStatic fun maxHeightDip(value: Float): JavaStyle = JavaStyle().maxHeightDip(value)

  /** @see [JavaStyle.maxWidthPx] */
  @JvmStatic fun maxWidthPx(value: Int): JavaStyle = JavaStyle().maxWidthPx(value)

  /** @see [JavaStyle.maxHeightPx] */
  @JvmStatic fun maxHeightPx(value: Int): JavaStyle = JavaStyle().maxHeightPx(value)

  /** @see [JavaStyle.minWidthDip] */
  @JvmStatic fun minWidthDip(value: Float): JavaStyle = JavaStyle().minWidthDip(value)

  /** @see [JavaStyle.minHeightDip] */
  @JvmStatic fun minHeightDip(value: Float): JavaStyle = JavaStyle().minHeightDip(value)

  /** @see [JavaStyle.minWidthPx] */
  @JvmStatic fun minWidthPx(value: Int): JavaStyle = JavaStyle().minWidthPx(value)

  /** @see [JavaStyle.minHeightPx] */
  @JvmStatic fun minHeightPx(value: Int): JavaStyle = JavaStyle().minHeightPx(value)

  /** @see [JavaStyle.marginDip] */
  @JvmStatic
  fun marginDip(yogaEdge: YogaEdge, value: Float): JavaStyle =
      JavaStyle().marginDip(yogaEdge, value)

  /** @see [JavaStyle.marginPx] */
  @JvmStatic
  fun marginPx(yogaEdge: YogaEdge, value: Int): JavaStyle = JavaStyle().marginPx(yogaEdge, value)

  /** @see [JavaStyle.paddingDip] */
  @JvmStatic
  fun paddingDip(yogaEdge: YogaEdge, value: Float): JavaStyle =
      JavaStyle().paddingDip(yogaEdge, value)

  /** @see [JavaStyle.paddingPx] */
  @JvmStatic
  fun paddingPx(yogaEdge: YogaEdge, value: Int): JavaStyle = JavaStyle().paddingPx(yogaEdge, value)

  /** @see [JavaStyle.positionDip] */
  @JvmStatic
  fun positionDip(yogaEdge: YogaEdge, value: Float): JavaStyle =
      JavaStyle().positionDip(yogaEdge, value)

  /** @see [JavaStyle.positionPx] */
  @JvmStatic
  fun positionPx(yogaEdge: YogaEdge, value: Int): JavaStyle =
      JavaStyle().positionPx(yogaEdge, value)

  /** @see [JavaStyle.wrapInView] */
  @JvmStatic fun wrapInView(): JavaStyle = JavaStyle().wrapInView()

  /** @see [JavaStyle.accessibilityRole] */
  @JvmStatic
  fun accessibilityRole(@AccessibilityRoleType role: String): JavaStyle =
      JavaStyle().accessibilityRole(role)

  /** @see [JavaStyle.alignSelf] */
  @JvmStatic fun alignSelf(value: YogaAlign): JavaStyle = JavaStyle().alignSelf(value)

  /** @see [JavaStyle.positionType] */
  @JvmStatic fun positionType(value: YogaPositionType): JavaStyle = JavaStyle().positionType(value)

  /** @see [JavaStyle.border] */
  @JvmStatic fun border(border: Border): JavaStyle = JavaStyle().border(border)
}

class JavaStyle {

  private var style: Style = Style

  fun flexShrink(value: Float): JavaStyle {
    style = style.flex(shrink = value)
    return this
  }

  fun flexGrow(value: Float): JavaStyle {
    style = style.flex(grow = value)
    return this
  }

  fun flexBasisDip(value: Float): JavaStyle {
    style = style.flex(basis = value.dp)
    return this
  }

  fun flexBasisPx(value: Int): JavaStyle {
    style = style.flex(basis = value.px)
    return this
  }

  fun widthDip(value: Float): JavaStyle {
    style = style.width(value.dp)
    return this
  }

  fun heightDip(value: Float): JavaStyle {
    style = style.height(value.dp)
    return this
  }

  fun widthPx(value: Int): JavaStyle {
    style = style.width(value.px)
    return this
  }

  fun heightPx(value: Int): JavaStyle {
    style = style.height(value.px)
    return this
  }

  fun widthPercent(value: Float): JavaStyle {
    style = style.widthPercent(value)
    return this
  }

  fun heightPercent(value: Float): JavaStyle {
    style = style.heightPercent(value)
    return this
  }

  fun maxWidthDip(value: Float): JavaStyle {
    style = style.maxWidth(value.dp)
    return this
  }

  fun maxHeightDip(value: Float): JavaStyle {
    style = style.maxHeight(value.dp)
    return this
  }

  fun maxWidthPx(value: Int): JavaStyle {
    style = style.maxWidth(value.px)
    return this
  }

  fun maxHeightPx(value: Int): JavaStyle {
    style = style.maxHeight(value.px)
    return this
  }

  fun minWidthDip(value: Float): JavaStyle {
    style = style.minWidth(value.dp)
    return this
  }

  fun minHeightDip(value: Float): JavaStyle {
    style = style.minHeight(value.dp)
    return this
  }

  fun minWidthPx(value: Int): JavaStyle {
    style = style.minWidth(value.px)
    return this
  }

  fun minHeightPx(value: Int): JavaStyle {
    style = style.minHeight(value.px)
    return this
  }

  fun wrapInView(): JavaStyle {
    style = style.wrapInView()
    return this
  }

  fun alignSelf(value: YogaAlign): JavaStyle {
    style = style.alignSelf(value)
    return this
  }

  fun positionType(value: YogaPositionType): JavaStyle {
    style = style.positionType(value)
    return this
  }

  fun border(border: Border): JavaStyle {
    style = style.border(border)
    return this
  }

  fun marginDip(yogaEdge: YogaEdge, value: Float): JavaStyle {
    val valueDip = value.dp
    when (yogaEdge) {
      YogaEdge.LEFT -> style = style.margin(left = valueDip)
      YogaEdge.VERTICAL -> style = style.margin(vertical = valueDip)
      YogaEdge.TOP -> style = style.margin(top = valueDip)
      YogaEdge.RIGHT -> style = style.margin(right = valueDip)
      YogaEdge.BOTTOM -> style = style.margin(bottom = valueDip)
      YogaEdge.START -> style = style.margin(start = valueDip)
      YogaEdge.END -> style = style.margin(end = valueDip)
      YogaEdge.HORIZONTAL -> style = style.margin(horizontal = valueDip)
      YogaEdge.ALL -> style = style.margin(all = valueDip)
    }.exhaustive
    return this
  }

  fun marginPx(yogaEdge: YogaEdge, value: Int): JavaStyle {
    val valuePx = value.px
    when (yogaEdge) {
      YogaEdge.LEFT -> style = style.margin(left = valuePx)
      YogaEdge.VERTICAL -> style = style.margin(vertical = valuePx)
      YogaEdge.TOP -> style = style.margin(top = valuePx)
      YogaEdge.RIGHT -> style = style.margin(right = valuePx)
      YogaEdge.BOTTOM -> style = style.margin(bottom = valuePx)
      YogaEdge.START -> style = style.margin(start = valuePx)
      YogaEdge.END -> style = style.margin(end = valuePx)
      YogaEdge.HORIZONTAL -> style = style.margin(horizontal = valuePx)
      YogaEdge.ALL -> style = style.margin(all = valuePx)
    }.exhaustive
    return this
  }

  fun paddingDip(yogaEdge: YogaEdge, value: Float): JavaStyle {
    val valueDip = value.dp
    when (yogaEdge) {
      YogaEdge.LEFT -> style = style.padding(left = valueDip)
      YogaEdge.VERTICAL -> style = style.padding(vertical = valueDip)
      YogaEdge.TOP -> style = style.padding(top = valueDip)
      YogaEdge.RIGHT -> style = style.padding(right = valueDip)
      YogaEdge.BOTTOM -> style = style.padding(bottom = valueDip)
      YogaEdge.START -> style = style.padding(start = valueDip)
      YogaEdge.END -> style = style.padding(end = valueDip)
      YogaEdge.HORIZONTAL -> style = style.padding(horizontal = valueDip)
      YogaEdge.ALL -> style = style.padding(all = valueDip)
    }.exhaustive
    return this
  }

  fun paddingPx(yogaEdge: YogaEdge, value: Int): JavaStyle {
    val valuePx = value.px
    when (yogaEdge) {
      YogaEdge.LEFT -> style = style.padding(left = valuePx)
      YogaEdge.VERTICAL -> style = style.padding(vertical = valuePx)
      YogaEdge.TOP -> style = style.padding(top = valuePx)
      YogaEdge.RIGHT -> style = style.padding(right = valuePx)
      YogaEdge.BOTTOM -> style = style.padding(bottom = valuePx)
      YogaEdge.START -> style = style.padding(start = valuePx)
      YogaEdge.END -> style = style.padding(end = valuePx)
      YogaEdge.HORIZONTAL -> style = style.padding(horizontal = valuePx)
      YogaEdge.ALL -> style = style.padding(all = valuePx)
    }.exhaustive
    return this
  }

  fun positionDip(yogaEdge: YogaEdge, value: Float): JavaStyle {
    val valueDip = value.dp
    when (yogaEdge) {
      YogaEdge.LEFT -> style = style.position(left = valueDip)
      YogaEdge.VERTICAL -> TODO("No matching functionality on Style yet")
      YogaEdge.TOP -> style = style.position(top = valueDip)
      YogaEdge.RIGHT -> style = style.position(right = valueDip)
      YogaEdge.BOTTOM -> style = style.position(bottom = valueDip)
      YogaEdge.START -> style = style.position(start = valueDip)
      YogaEdge.END -> style = style.position(end = valueDip)
      YogaEdge.HORIZONTAL -> TODO("No matching functionality on Style yet")
      YogaEdge.ALL -> TODO("No matching functionality on Style yet")
    }.exhaustive
    return this
  }

  fun positionPx(yogaEdge: YogaEdge, value: Int): JavaStyle {
    val valuePx = value.px
    when (yogaEdge) {
      YogaEdge.LEFT -> style = style.position(left = valuePx)
      YogaEdge.VERTICAL -> style = TODO("No matching functionality on Style yet")
      YogaEdge.TOP -> style = style.position(top = valuePx)
      YogaEdge.RIGHT -> style = style.position(right = valuePx)
      YogaEdge.BOTTOM -> style = style.position(bottom = valuePx)
      YogaEdge.START -> style = style.position(start = valuePx)
      YogaEdge.END -> style = style.position(end = valuePx)
      YogaEdge.HORIZONTAL -> style = TODO("No matching functionality on Style yet")
      YogaEdge.ALL -> style = TODO("No matching functionality on Style yet")
    }.exhaustive
    return this
  }

  fun accessibilityRole(@AccessibilityRoleType role: String): JavaStyle {
    style = style.accessibilityRole(role)
    return this
  }

  fun build(): Style {
    return style
  }
}
