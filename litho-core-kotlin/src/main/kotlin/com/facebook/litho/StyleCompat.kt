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
import com.facebook.litho.core.width
import com.facebook.litho.core.widthPercent
import com.facebook.litho.flexbox.flex
import com.facebook.litho.view.wrapInView
import com.facebook.yoga.YogaEdge
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

  /** @see [JavaStyle.marginDip] */
  @JvmStatic
  fun marginDip(yogaEdge: YogaEdge, value: Float): JavaStyle =
      JavaStyle().marginDip(yogaEdge, value)

  /** @see [JavaStyle.wrapInView] */
  @JvmStatic fun wrapInView(): JavaStyle = JavaStyle().wrapInView()

  /** @see [JavaStyle.accessibilityRole] */
  @JvmStatic
  fun accessibilityRole(@AccessibilityRoleType role: String): JavaStyle =
      JavaStyle().accessibilityRole(role)
}

class JavaStyle {

  private var style: Style = Style

  fun flexShrink(value: Float): JavaStyle {
    style = style.flex(shrink = value)
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

  fun wrapInView(): JavaStyle {
    style = style.wrapInView()
    return this
  }

  fun marginDip(yogaEdge: YogaEdge, value: Float): JavaStyle {
    when (yogaEdge) {
      YogaEdge.LEFT -> style = style.margin(left = value.dp)
      YogaEdge.VERTICAL -> style = style.margin(vertical = value.dp)
      YogaEdge.TOP -> style = style.margin(top = value.dp)
      YogaEdge.RIGHT -> style = style.margin(right = value.dp)
      YogaEdge.BOTTOM -> style = style.margin(bottom = value.dp)
      YogaEdge.START -> style = style.margin(start = value.dp)
      YogaEdge.END -> style = style.margin(end = value.dp)
      YogaEdge.HORIZONTAL -> style = style.margin(horizontal = value.dp)
      YogaEdge.ALL -> style = style.margin(all = value.dp)
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
