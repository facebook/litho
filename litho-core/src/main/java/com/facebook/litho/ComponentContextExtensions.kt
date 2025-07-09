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
import androidx.annotation.AttrRes
import androidx.annotation.BoolRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.IntegerRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.boolAttr
import com.facebook.rendercore.boolRes
import com.facebook.rendercore.colorAttr
import com.facebook.rendercore.colorRes
import com.facebook.rendercore.dimenAttr
import com.facebook.rendercore.dimenRes
import com.facebook.rendercore.drawableAttr
import com.facebook.rendercore.drawableRes
import com.facebook.rendercore.floatAttr
import com.facebook.rendercore.floatRes
import com.facebook.rendercore.intAttr
import com.facebook.rendercore.intRes
import com.facebook.rendercore.quantityStringRes
import com.facebook.rendercore.stringAttr
import com.facebook.rendercore.stringRes

/** Return a string for a resource ID. */
inline fun ComponentContext.stringRes(@StringRes id: Int): String =
    ComponentScope(this).stringRes(id)

/** Return a string for a resource ID, substituting the format arguments with [arg]. */
inline fun ComponentContext.stringRes(@StringRes id: Int, arg: Any): String =
    ComponentScope(this).stringRes(id, arg)

/** Return a string for a resource ID, substituting the format arguments with [formatArgs]. */
inline fun ComponentContext.stringRes(@StringRes id: Int, vararg formatArgs: Any): String =
    ComponentScope(this).stringRes(id, *formatArgs)

/** Return a string for an attribute resource ID. */
inline fun ComponentContext.stringAttr(
    @AttrRes attrResId: Int,
    @StringRes defResId: Int = 0
): String = ComponentScope(this).stringAttr(attrResId, defResId)

/** Return a boolean for a resource ID. */
inline fun ComponentContext.boolRes(@BoolRes id: Int): Boolean = ComponentScope(this).boolRes(id)

/** Return a boolean for an attribute resource ID. */
inline fun ComponentContext.boolAttr(@AttrRes attrResId: Int, @BoolRes defResId: Int = 0): Boolean =
    ComponentScope(this).boolAttr(attrResId, defResId)

/**
 * @return a string for a resource ID and quantity, substituting the format arguments with [arg].
 */
inline fun ComponentContext.quantityStringRes(
    @PluralsRes id: Int,
    quantity: Int,
    arg: Any
): String = ComponentScope(this).quantityStringRes(id, quantity, arg)

/**
 * @return a string for a resource ID and quantity, substituting the format arguments with
 *   [formatArgs].
 */
inline fun ComponentContext.quantityStringRes(@PluralsRes id: Int, quantity: Int): String =
    ComponentScope(this).quantityStringRes(id, quantity)

/** @return a string for a resource ID and quantity. */
inline fun ComponentContext.quantityStringRes(
    @PluralsRes id: Int,
    quantity: Int,
    vararg formatArgs: Any
): String = ComponentScope(this).quantityStringRes(id, quantity, *formatArgs)

/** Retrieve a [android.graphics.drawable.Drawable] for a resource ID as a [Drawable] instance. */
inline fun ComponentContext.drawableRes(@DrawableRes id: Int): Drawable =
    ComponentScope(this).drawableRes(id)

/**
 * Retrieve a [android.graphics.drawable.Drawable], corresponding to an attribute resource ID, as a
 * [Drawable] instance. If given attribute ID can not be found, default Drawable resource ID
 * [defResId] is used.
 */
inline fun ComponentContext.drawableAttr(
    @AttrRes id: Int,
    @DrawableRes defResId: Int = 0
): Drawable = ComponentScope(this).drawableAttr(id, defResId)

/** Return a [ColorInt] value for a color resource ID. */
@ColorInt
inline fun ComponentContext.colorRes(@ColorRes id: Int): Int = ComponentScope(this).colorRes(id)

/**
 * Return a [ColorInt] value, corresponding to an attribute resource ID. If given attribute ID can
 * not be found, default color resource ID [defResId] is used.
 */
@ColorInt
inline fun ComponentContext.colorAttr(@AttrRes id: Int, @ColorRes defResId: Int = 0): Int =
    ComponentScope(this).colorAttr(id, defResId)

inline fun ComponentContext.intRes(@IntegerRes id: Int): Int = ComponentScope(this).intRes(id)

inline fun ComponentContext.intAttr(@AttrRes id: Int, @IntegerRes defResId: Int = 0): Int =
    ComponentScope(this).intAttr(id, defResId)

inline fun ComponentContext.floatRes(@DimenRes id: Int): Float = ComponentScope(this).floatRes(id)

inline fun ComponentContext.floatAttr(@AttrRes id: Int, @DimenRes defResId: Int = 0): Float =
    ComponentScope(this).floatAttr(id, defResId)

/** Resolve a dimen resource ID as a [Dimen] value. */
inline fun ComponentContext.dimenRes(@DimenRes id: Int): Dimen = ComponentScope(this).dimenRes(id)

/** Return a dimen for an attribute resource ID. */
inline fun ComponentContext.dimenAttr(@AttrRes id: Int, @DimenRes defResId: Int = 0): Dimen =
    ComponentScope(this).dimenAttr(id, defResId)
