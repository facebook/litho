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
 * Builder for decorating a child component with [background] or [foreground].
 */
inline fun <C : Component.Builder<C>> ComponentContext.Decoration(
    foreground: Drawable? = null,
    background: Drawable? = null,
    content: ComponentContext.() -> C
): C =
    @Suppress("DEPRECATION")
    content()
        .apply {
          foreground?.let { foreground(it) }
          background?.let { background(it) }
        }
