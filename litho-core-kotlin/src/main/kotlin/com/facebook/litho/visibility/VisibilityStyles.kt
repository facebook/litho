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

package com.facebook.litho.visibility

import com.facebook.litho.Component
import com.facebook.litho.FocusedVisibleEvent
import com.facebook.litho.FullImpressionVisibleEvent
import com.facebook.litho.InvisibleEvent
import com.facebook.litho.ResourceResolver
import com.facebook.litho.Style
import com.facebook.litho.StyleItem
import com.facebook.litho.UnfocusedVisibleEvent
import com.facebook.litho.VisibleEvent
import com.facebook.litho.eventHandler
import com.facebook.litho.exhaustive
import com.facebook.litho.getCommonPropsHolder

/** Enums for [VisibilityStyleItem]. */
@PublishedApi
internal enum class VisibilityField {
  ON_VISIBLE,
  ON_INVISIBLE,
  ON_FOCUSED,
  ON_UNFOCUSED,
  ON_FULL_IMPRESSION,
}

@PublishedApi
internal class VisibilityStyleItem(val field: VisibilityField, val value: Any?) : StyleItem {
  override fun applyToComponent(resourceResolver: ResourceResolver, component: Component) {
    val commonProps = component.getCommonPropsHolder()
    when (field) {
      VisibilityField.ON_VISIBLE ->
          commonProps.visibleHandler(eventHandler(value as (VisibleEvent) -> Unit))
      VisibilityField.ON_INVISIBLE ->
          commonProps.invisibleHandler(eventHandler(value as (InvisibleEvent) -> Unit))
      VisibilityField.ON_FOCUSED ->
          commonProps.focusedHandler(eventHandler(value as (FocusedVisibleEvent) -> Unit))
      VisibilityField.ON_UNFOCUSED ->
          commonProps.unfocusedHandler(eventHandler(value as (UnfocusedVisibleEvent) -> Unit))
      VisibilityField.ON_FULL_IMPRESSION ->
          commonProps.fullImpressionHandler(
              eventHandler(value as (FullImpressionVisibleEvent) -> Unit))
    }.exhaustive
  }
}

/** Registers a callback to be called when any part of the Component becomes visible on screen. */
inline fun Style.onVisible(noinline onVisible: (VisibleEvent) -> Unit): Style =
    this + VisibilityStyleItem(VisibilityField.ON_VISIBLE, onVisible)

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
 * viewport once
 */
inline fun Style.onFullImpression(
    noinline onFullImpression: (FullImpressionVisibleEvent) -> Unit
): Style = this + VisibilityStyleItem(VisibilityField.ON_FULL_IMPRESSION, onFullImpression)
