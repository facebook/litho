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

package com.facebook.litho

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import com.facebook.litho.drawable.ComparableColorDrawable
import com.facebook.rendercore.BaseResourcesScope

/**
 * Return a [android.graphics.drawable.Drawable] for a [ColorInt] value as a [Drawable] instance.
 */
fun BaseResourcesScope.drawableColor(@ColorInt color: Int): Drawable =
    ComparableColorDrawable.create(color)

/**
 * Return a [android.graphics.drawable.Drawable] for a [ColorInt] value as a [Drawable] instance.
 */
fun BaseResourcesScope.drawableColor(@ColorInt color: Long): Drawable =
    ComparableColorDrawable.create(color.toInt())
