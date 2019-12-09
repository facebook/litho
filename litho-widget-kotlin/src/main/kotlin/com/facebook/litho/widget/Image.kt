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

@file:Suppress("NOTHING_TO_INLINE", "FunctionName")

package com.facebook.litho.widget

import android.graphics.drawable.Drawable
import android.widget.ImageView.ScaleType
import androidx.annotation.DrawableRes
import com.facebook.litho.DslScope

/**
 * Builder function for creating [ImageSpec] components.
 */
inline fun DslScope.Image(
    drawable: Drawable,
    scaleType: ScaleType = ScaleType.FIT_CENTER
): Image.Builder =
    Image.create(this)
        .drawable(drawable)
        .scaleType(scaleType)

/**
 * Builder function for creating [ImageSpec] components.
 */
inline fun DslScope.Image(
    @DrawableRes drawableRes: Int,
    scaleType: ScaleType = ScaleType.FIT_CENTER
): Image.Builder =
    Image.create(this)
        .drawableRes(drawableRes)
        .scaleType(scaleType)
