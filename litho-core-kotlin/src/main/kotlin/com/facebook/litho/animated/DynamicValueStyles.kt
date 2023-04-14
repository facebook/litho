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

package com.facebook.litho.animated

import android.graphics.drawable.Drawable
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.DynamicPropsManager.KEY_ALPHA
import com.facebook.litho.DynamicPropsManager.KEY_BACKGROUND_COLOR
import com.facebook.litho.DynamicPropsManager.KEY_BACKGROUND_DRAWABLE
import com.facebook.litho.DynamicPropsManager.KEY_ELEVATION
import com.facebook.litho.DynamicPropsManager.KEY_FOREGROUND_COLOR
import com.facebook.litho.DynamicPropsManager.KEY_ROTATION
import com.facebook.litho.DynamicPropsManager.KEY_SCALE_X
import com.facebook.litho.DynamicPropsManager.KEY_SCALE_Y
import com.facebook.litho.DynamicPropsManager.KEY_TRANSLATION_X
import com.facebook.litho.DynamicPropsManager.KEY_TRANSLATION_Y
import com.facebook.litho.DynamicValue
import com.facebook.litho.Style
import com.facebook.litho.StyleItem
import com.facebook.litho.StyleItemField
import com.facebook.litho.getOrCreateCommonDynamicPropsHolder

/** Enums for [DynamicStyleItem]. */
@PublishedApi
internal enum class DynamicField : StyleItemField {
  ALPHA,
  BACKGROUND_COLOR,
  BACKGROUND_DRAWABLE,
  ELEVATION,
  FOREGROUND,
  ROTATION,
  SCALE_X,
  SCALE_Y,
  TRANSLATION_X,
  TRANSLATION_Y,
}

/**
 * Common style item for all dynamic value styles. See note on [DynamicField] about this pattern.
 */
@PublishedApi
@DataClassGenerate
internal data class DynamicStyleItem(
    override val field: DynamicField,
    override val value: DynamicValue<*>
) : StyleItem<DynamicValue<*>> {
  override fun applyToComponent(context: ComponentContext, component: Component) {
    val dynamicProps = component.getOrCreateCommonDynamicPropsHolder()
    when (field) {
      DynamicField.ALPHA -> dynamicProps.put(KEY_ALPHA, value)
      DynamicField.BACKGROUND_COLOR -> dynamicProps.put(KEY_BACKGROUND_COLOR, value)
      DynamicField.BACKGROUND_DRAWABLE -> dynamicProps.put(KEY_BACKGROUND_DRAWABLE, value)
      DynamicField.ELEVATION -> dynamicProps.put(KEY_ELEVATION, value)
      DynamicField.FOREGROUND -> dynamicProps.put(KEY_FOREGROUND_COLOR, value)
      DynamicField.ROTATION -> dynamicProps.put(KEY_ROTATION, value)
      DynamicField.SCALE_X -> dynamicProps.put(KEY_SCALE_X, value)
      DynamicField.SCALE_Y -> dynamicProps.put(KEY_SCALE_Y, value)
      DynamicField.TRANSLATION_X -> dynamicProps.put(KEY_TRANSLATION_X, value)
      DynamicField.TRANSLATION_Y -> dynamicProps.put(KEY_TRANSLATION_Y, value)
    }
  }
}

inline fun Style.alpha(alpha: DynamicValue<Float>): Style =
    this + DynamicStyleItem(DynamicField.ALPHA, alpha)

inline fun Style.background(background: DynamicValue<out Drawable>): Style =
    this + DynamicStyleItem(DynamicField.BACKGROUND_DRAWABLE, background)

inline fun Style.backgroundColor(background: DynamicValue<Int>): Style =
    this + DynamicStyleItem(DynamicField.BACKGROUND_COLOR, background)

inline fun Style.foregroundColor(foreground: DynamicValue<Int>): Style =
    this + DynamicStyleItem(DynamicField.FOREGROUND, foreground)

inline fun Style.elevation(elevation: DynamicValue<Float>): Style =
    this + DynamicStyleItem(DynamicField.ELEVATION, elevation)

inline fun Style.rotation(rotation: DynamicValue<Float>): Style =
    this + DynamicStyleItem(DynamicField.ROTATION, rotation)

inline fun Style.scaleX(scaleX: DynamicValue<Float>): Style =
    this + DynamicStyleItem(DynamicField.SCALE_X, scaleX)

inline fun Style.scaleY(scaleY: DynamicValue<Float>): Style =
    this + DynamicStyleItem(DynamicField.SCALE_Y, scaleY)

inline fun Style.translationX(translationX: DynamicValue<Float>): Style =
    this + DynamicStyleItem(DynamicField.TRANSLATION_X, translationX)

inline fun Style.translationY(translationY: DynamicValue<Float>): Style =
    this + DynamicStyleItem(DynamicField.TRANSLATION_Y, translationY)
