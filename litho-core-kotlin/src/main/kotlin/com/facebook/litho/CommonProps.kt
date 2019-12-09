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

inline fun <reified T> eventHandler(crossinline onEvent: () -> Unit): EventHandler<T> =
    EventHandler({ EventDispatcher { _, _ -> onEvent() } }, 0, null)

/**
 * Builder for setting an [onClick] event handler for component.
 *
 * TODO Currently lambda captures possibly old props. Find a better option.
 *  This will work for core Litho, but may break Sections.
 */
inline fun <C : Component.Builder<C>> DslScope.Clickable(
    crossinline onClick: () -> Unit,
    content: DslScope.() -> C
): C =
    content().clickHandler(eventHandler(onClick))

/**
 * Builder for decorating a child component with [background] or [foreground].
 */
inline fun <C : Component.Builder<C>> DslScope.Decoration(
    foreground: Drawable? = null,
    background: Drawable? = null,
    content: DslScope.() -> C
): C =
    content()
        .apply {
          foreground?.let { foreground(it) }
          background?.let { background(it) }
        }
