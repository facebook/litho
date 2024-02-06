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

package com.facebook.litho.visibility

import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.litho.CommonProps
import com.facebook.litho.ComponentContext
import com.facebook.litho.FocusedVisibleEvent
import com.facebook.litho.FullImpressionVisibleEvent
import com.facebook.litho.InvisibleEvent
import com.facebook.litho.Style
import com.facebook.litho.StyleItem
import com.facebook.litho.StyleItemField
import com.facebook.litho.UnfocusedVisibleEvent
import com.facebook.litho.VisibilityChangedEvent
import com.facebook.litho.VisibleEvent
import com.facebook.litho.eventHandler

/** Enums for [VisibilityStyleItem]. */
@PublishedApi
internal enum class VisibilityField : StyleItemField {
  ON_VISIBLE,
  ON_INVISIBLE,
  ON_FOCUSED,
  ON_UNFOCUSED,
  ON_FULL_IMPRESSION,
  ON_VISIBILITY_CHANGED,
}

/** Enums for [VisibilityFloatStyleItem]. */
@PublishedApi
internal enum class VisibilityFloatField : StyleItemField {
  VISIBLE_HEIGHT_RATIO,
  VISIBLE_WIDTH_RATIO,
}

@PublishedApi
@DataClassGenerate
internal data class VisibilityStyleItem(
    override val field: VisibilityField,
    override val value: Any?,
    val tag: String? = null
) : StyleItem<Any?> {
  override fun applyCommonProps(context: ComponentContext, commonProps: CommonProps) {
    when (field) {
      VisibilityField.ON_VISIBLE ->
          commonProps.visibleHandler(eventHandler(value as (VisibleEvent) -> Unit, tag))
      VisibilityField.ON_INVISIBLE ->
          commonProps.invisibleHandler(eventHandler(value as (InvisibleEvent) -> Unit, tag))
      VisibilityField.ON_FOCUSED ->
          commonProps.focusedHandler(eventHandler(value as (FocusedVisibleEvent) -> Unit, tag))
      VisibilityField.ON_UNFOCUSED ->
          commonProps.unfocusedHandler(eventHandler(value as (UnfocusedVisibleEvent) -> Unit, tag))
      VisibilityField.ON_FULL_IMPRESSION ->
          commonProps.fullImpressionHandler(
              eventHandler(value as (FullImpressionVisibleEvent) -> Unit, tag))
      VisibilityField.ON_VISIBILITY_CHANGED, ->
          commonProps.visibilityChangedHandler(
              eventHandler(value as (VisibilityChangedEvent) -> Unit, tag))
    }
  }
}

@PublishedApi
internal class VisibilityFloatStyleItem(
    override val field: VisibilityFloatField,
    override val value: Float
) : StyleItem<Float> {
  override fun applyCommonProps(context: ComponentContext, commonProps: CommonProps) {
    when (field) {
      VisibilityFloatField.VISIBLE_HEIGHT_RATIO -> commonProps.visibleHeightRatio(value)
      VisibilityFloatField.VISIBLE_WIDTH_RATIO -> commonProps.visibleWidthRatio(value)
    }
  }
}

/** Registers a callback to be called when any part of the Component becomes visible on screen. */
inline fun Style.onVisible(noinline onVisible: (VisibleEvent) -> Unit): Style =
    this + VisibilityStyleItem(VisibilityField.ON_VISIBLE, onVisible)

@Deprecated("This will be removed soon")
inline fun Style.onVisibleWithTag(tag: String?, noinline onVisible: (VisibleEvent) -> Unit): Style =
    this + VisibilityStyleItem(VisibilityField.ON_VISIBLE, onVisible, tag)

/**
 * Registers a callback to be called when a Component becomes fully invisible (e.g. scrolled off or
 * unmounted)
 */
inline fun Style.onInvisible(noinline onInvisible: (InvisibleEvent) -> Unit): Style =
    this + VisibilityStyleItem(VisibilityField.ON_INVISIBLE, onInvisible)

/**
 * Registers a callback to be called when either the Component occupies at least half of the
 * viewport, or, if the Component is smaller than half the viewport, when it is fully visible.
 */
inline fun Style.onFocusedVisible(noinline onFocused: (FocusedVisibleEvent) -> Unit): Style =
    this + VisibilityStyleItem(VisibilityField.ON_FOCUSED, onFocused)

/**
 * Registers a callback to be called when the Component is no longer focused, i.e. it is not fully
 * visible and does not occupy at least half the viewport.
 */
inline fun Style.onUnfocusedVisible(noinline onUnfocused: (UnfocusedVisibleEvent) -> Unit): Style =
    this + VisibilityStyleItem(VisibilityField.ON_UNFOCUSED, onUnfocused)

/**
 * Registers a callback to be called when all parts of a Component have been made visible at some
 * point, termed a "full impression". A full impression is defined as:
 * - if the Component is smaller than the viewport, when the entire Component is visible
 * - if the Component is bigger than the viewport, when all the edges have passed through the
 *   viewport once
 */
inline fun Style.onFullImpression(
    noinline onFullImpression: (FullImpressionVisibleEvent) -> Unit
): Style = this + VisibilityStyleItem(VisibilityField.ON_FULL_IMPRESSION, onFullImpression)

/** Registers a callback to be called when the visible rect of a Component changes. */
inline fun Style.onVisibilityChanged(
    noinline onVisibilityChanged: (VisibilityChangedEvent) -> Unit
): Style = this + VisibilityStyleItem(VisibilityField.ON_VISIBILITY_CHANGED, onVisibilityChanged)
/**
 * Defines a ratio of the Component height for the visibility callback to be dispatched use together
 * with [onVisible] and/or with [onInvisible].
 */
inline fun Style.visibleHeightRatio(visibleHeightRatio: Float): Style =
    this + VisibilityFloatStyleItem(VisibilityFloatField.VISIBLE_HEIGHT_RATIO, visibleHeightRatio)

/**
 * Defines a ratio of the Component width for the visibility callback to be dispatched use together
 * with [onVisible] and/or with [onInvisible].
 */
inline fun Style.visibleWidthRatio(visibleWidthRatio: Float): Style =
    this + VisibilityFloatStyleItem(VisibilityFloatField.VISIBLE_WIDTH_RATIO, visibleWidthRatio)
