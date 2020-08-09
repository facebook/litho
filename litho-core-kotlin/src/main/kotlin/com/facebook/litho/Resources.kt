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

import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.facebook.litho.drawable.ComparableColorDrawable

/**
 * Retrieve a dimensional for a resource ID as a [Dp] value.
 */
fun DslScope.dpRes(@DimenRes id: Int): Dp =
    resourceResolver.pixelsToDips(resourceResolver.resolveDimenSizeRes(id)).dp

/**
 * Retrieve a dimensional for a resource ID as a [Sp] value.
 */
// TODO Internally this will be consumed as a pixel value, i.e. converted back. Think about better
//  approach.
fun DslScope.spRes(@DimenRes id: Int): Sp =
    resourceResolver.pixelsToSips(resourceResolver.resolveDimenSizeRes(id)).sp

/**
 * Return a string for a resource ID.
 */
fun DslScope.stringRes(@StringRes id: Int): String =
    requireNotNull(resourceResolver.resolveStringRes(id)) {
      "String resource not found for ID #0x${Integer.toHexString(id)}"
    }

/**
 * Return a string for a resource ID, substituting the format arguments with [formatArgs].
 */
fun DslScope.stringRes(@StringRes id: Int, vararg formatArgs: Any): String =
    requireNotNull(resourceResolver.resolveStringRes(id, formatArgs)) {
      "String resource not found for ID #0x${Integer.toHexString(id)}"
    }

/**
 * Retrieve a [android.graphics.drawable.Drawable] for a resource ID as a [Drawable]
 * instance.
 */
fun DslScope.drawableRes(@DrawableRes id: Int): Drawable =
    requireNotNull(resourceResolver.resolveDrawableRes(id)) {
      "Drawable resource not found for ID #0x${Integer.toHexString(id)}"
    }

/**
 * Retrieve a [android.graphics.drawable.Drawable], corresponding to an attribute resource ID, as
 * a [Drawable] instance. If given attribute ID can not be found, default Drawable
 * resource ID [defResId] is used.
 */
fun DslScope.drawableAttr(@AttrRes id: Int, @DrawableRes defResId: Int = 0): Drawable =
    drawableRes(resourceResolver.resolveResIdAttr(id, defResId))

/**
 * Return a [android.graphics.drawable.Drawable] for a [String] hex color value as a [Drawable]
 * instance.
 */
fun DslScope.drawableColor(hexColor: String): Drawable =
        ComparableColorDrawable.create(Color.parseColor("#$hexColor"))

/**
 * Return a [android.graphics.drawable.Drawable] for a [String] hex color value as a [Drawable]
 * instance.
 */
fun DslScope.drawableColor(hexColor: String, opacity: Opacity): Drawable =
        ComparableColorDrawable.create(Color.parseColor("#${opacity.percent}$hexColor"))

/**
 * Return a [android.graphics.drawable.Drawable] for a [ColorInt] value as a [Drawable]
 * instance.
 */
fun DslScope.drawableColor(@ColorInt color: Long): Drawable =
    ComparableColorDrawable.create(color.toInt())

/**
 * Return a [ColorInt] value for a color resource ID.
 */
@ColorInt fun DslScope.colorRes(@ColorRes id: Int): Int =
    resourceResolver.resolveColorRes(id)

/**
 * Return a [ColorInt] value, corresponding to an attribute resource ID. If given attribute ID can
 * not be found, default color resource ID [defResId] is used.
 */
@ColorInt fun DslScope.colorAttr(@AttrRes id: Int, @ColorRes defResId: Int = 0): Int =
    resourceResolver.resolveColorAttr(id, defResId)
