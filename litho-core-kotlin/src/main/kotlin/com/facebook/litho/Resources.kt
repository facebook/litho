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

import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.facebook.litho.drawable.ComparableColorDrawable
import com.facebook.litho.drawable.ComparableDrawable
import com.facebook.litho.drawable.ComparableResDrawable

/**
 * Retrieve a dimensional for a resource ID as a [Dp] value.
 */
fun ComponentContext.dpRes(@DimenRes id: Int): Dp =
    resourceResolver.pixelsToDips(resourceResolver.resolveDimenSizeRes(id)).dp

/**
 * Retrieve a dimensional for a resource ID as a [Sp] value.
 */
// TODO Internally this will be consumed as a pixel value, i.e. converted back. Think about better
//  approach.
fun ComponentContext.spRes(@DimenRes id: Int): Sp =
    resourceResolver.pixelsToSips(resourceResolver.resolveDimenSizeRes(id)).sp

/**
 * Return a string for a resource ID.
 */
fun ComponentContext.stringRes(@StringRes id: Int): String =
    requireNotNull(resourceResolver.resolveStringRes(id)) {
      "String resource not found for ID #0x${Integer.toHexString(id)}"
    }

/**
 * Return a string for a resource ID, substituting the format arguments with [formatArgs].
 */
fun ComponentContext.stringRes(@StringRes id: Int, vararg formatArgs: Any): String =
    requireNotNull(resourceResolver.resolveStringRes(id, formatArgs)) {
      "String resource not found for ID #0x${Integer.toHexString(id)}"
    }

/**
 * Retrieve a [android.graphics.drawable.Drawable] for a resource ID as a [ComparableDrawable]
 * instance.
 */
fun ComponentContext.drawableRes(@DrawableRes id: Int): ComparableDrawable =
    ComparableResDrawable.create(androidContext, id)

/**
 * Retrieve a [android.graphics.drawable.Drawable], corresponding to an attribute resource ID, as
 * a [ComparableDrawable] instance. If given attribute ID can not be found, default Drawable
 * resource ID [defResId] is used.
 */
fun ComponentContext.drawableAttr(@AttrRes id: Int, @DrawableRes defResId: Int = 0): ComparableDrawable =
    drawableRes(resourceResolver.resolveResIdAttr(id, defResId))

/**
 * Return a [android.graphics.drawable.Drawable] for a [ColorInt] value as a [ComparableDrawable]
 * instance.
 */
fun ComponentContext.drawableColor(@ColorInt color: Int): ComparableDrawable =
    ComparableColorDrawable.create(color)

/**
 * Return a [android.graphics.drawable.Drawable] for a [ColorInt] value as a [ComparableDrawable]
 * instance.
 */
fun ComponentContext.drawableColor(@ColorInt color: Long): ComparableDrawable =
    ComparableColorDrawable.create(color.toInt())

/**
 * Return a [ColorInt] value for a color resource ID.
 */
@ColorInt fun ComponentContext.colorRes(@ColorRes id: Int): Int =
    resourceResolver.resolveColorRes(id)

/**
 * Return a [ColorInt] value, corresponding to an attribute resource ID. If given attribute ID can
 * not be found, default color resource ID [defResId] is used.
 */
@ColorInt fun ComponentContext.colorAttr(@AttrRes id: Int, @ColorRes defResId: Int = 0): Int =
    resourceResolver.resolveColorAttr(id, defResId)
