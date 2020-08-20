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

@file:Suppress("FunctionName")

package com.facebook.litho

import android.graphics.drawable.Drawable

/**
 * Builder for setting an [onClick] event handler for component.
 *
 * TODO Currently lambda captures possibly old props. Find a better option.
 *  This will work for core Litho, but may break Sections.
 */
inline fun DslScope.Clickable(
    noinline onClick: ((ClickEvent) -> Unit)? = null,
    noinline onLongClick: ((LongClickEvent) -> Unit)? = null,
    content: DslScope.() -> Component
): Component =
    content().apply {
      getOrCreateCommonProps.apply {
        onClick?.let { clickHandler(eventHandler(it)) }
        onLongClick?.let { longClickHandler(eventHandler(it)) }
      }
    }

/**
 * Builder for setting a [ClickEvent] [EventHandler] for a component. Can be used to provide an
 * [EventHandler] generated from a Spec.
 */
inline fun DslScope.Clickable(
    clickHandler: EventHandler<ClickEvent>? = null,
    longClickHandler: EventHandler<LongClickEvent>? = null,
    content: DslScope.() -> Component
): Component =
    content().apply {
      getOrCreateCommonProps.apply {
        clickHandler?.let { clickHandler(it) }
        longClickHandler?.let { longClickHandler(it) }
      }
    }

/**
 * Builder for setting [onVisible], [onFocused], and [onFullImpression] event handlers for
 * component.
 *
 * TODO Currently lambda captures possibly old props. Find a better option.
 *  This will work for core Litho, but may break Sections.
 */
inline fun DslScope.VisibilityHandler(
    noinline onVisible: ((VisibleEvent) -> Unit)? = null,
    noinline onFocused: ((FocusedVisibleEvent) -> Unit)? = null,
    noinline onFullImpression: ((FullImpressionVisibleEvent) -> Unit)? = null,
    content: DslScope.() -> Component
): Component =
    content().apply {
      getOrCreateCommonProps.apply {
        onVisible?.let { visibleHandler(eventHandler(it)) }
        onFocused?.let { focusedHandler(eventHandler(it)) }
        onFullImpression?.let { fullImpressionHandler(eventHandler(it)) }
      }
    }

/**
 * Builder for decorating a child component with [background] or [foreground].
 */
@Deprecated(message = "Use Style.background and Style.foreground instead")
inline fun DslScope.Decoration(
    foreground: Drawable? = null,
    background: Drawable? = null,
    content: DslScope.() -> Component
): Component =
    content().apply {
      getOrCreateCommonProps.apply {
        foreground?.let { foreground(it) }
        background?.let { background(it) }
      }
    }

@PublishedApi
internal val Component.getOrCreateCommonProps: CommonProps
  get() = this.getOrCreateCommonProps()
