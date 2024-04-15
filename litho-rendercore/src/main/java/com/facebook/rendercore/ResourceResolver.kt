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

package com.facebook.rendercore

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.annotation.ArrayRes
import androidx.annotation.AttrRes
import androidx.annotation.BoolRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.IntegerRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.facebook.rendercore.FastMath.round

open class ResourceResolver(private val androidContext: Context, val resourceCache: ResourceCache) {
  private val resources: Resources = androidContext.resources
  private val theme: Resources.Theme = androidContext.theme

  open fun dipsToPixels(dips: Float): Int {
    val scale = resources.displayMetrics.density
    return round(dips * scale)
  }

  open fun sipsToPixels(sips: Float): Int {
    val scale = resources.displayMetrics.scaledDensity
    return round(sips * scale)
  }

  fun pixelsToDips(pixels: Int): Float {
    val scale = resources.displayMetrics.density
    return pixels / scale
  }

  fun pixelsToSips(pixels: Int): Float {
    val scale = resources.displayMetrics.scaledDensity
    return pixels / scale
  }

  fun resolveText(@StringRes resId: Int): CharSequence? {
    if (resId != 0) {
      val cached = resourceCache.get<CharSequence>(resId)
      if (cached != null) {
        return cached
      }
      val result = resources.getText(resId)
      resourceCache[resId] = result
      return result
    }
    return null
  }

  fun resolveStringRes(@StringRes resId: Int): String? {
    if (resId != 0) {
      val cached = resourceCache.get<String>(resId)
      if (cached != null) {
        return cached
      }
      val result = resources.getString(resId)
      resourceCache[resId] = result
      return result
    }
    return null
  }

  fun resolveStringRes(@StringRes resId: Int, vararg formatArgs: Any?): String? {
    return if (resId != 0) resources.getString(resId, *formatArgs) else null
  }

  fun resolveQuantityStringRes(@PluralsRes resId: Int, quantity: Int): String? {
    return if (resId != 0) resources.getQuantityString(resId, quantity) else null
  }

  fun resolveQuantityStringRes(
      @PluralsRes resId: Int,
      quantity: Int,
      vararg formatArgs: Any?
  ): String? {
    return if (resId != 0) resources.getQuantityString(resId, quantity, *formatArgs) else null
  }

  fun resolveStringArrayRes(@ArrayRes resId: Int): Array<String>? {
    if (resId != 0) {
      val cached = resourceCache.get<Array<String>>(resId)
      if (cached != null) {
        return cached
      }
      val result = resources.getStringArray(resId)
      resourceCache[resId] = result
      return result
    }
    return null
  }

  fun resolveIntRes(@IntegerRes resId: Int): Int {
    if (resId != 0) {
      val cached = resourceCache.get<Int>(resId)
      if (cached != null) {
        return cached
      }
      val result = resources.getInteger(resId)
      resourceCache[resId] = result
      return result
    }
    return 0
  }

  fun resolveIntArrayRes(@ArrayRes resId: Int): IntArray? {
    if (resId != 0) {
      val cached = resourceCache.get<IntArray>(resId)
      if (cached != null) {
        return cached
      }
      val result = resources.getIntArray(resId)
      resourceCache[resId] = result
      return result
    }
    return null
  }

  fun resolveIntegerArrayRes(@ArrayRes resId: Int): Array<Int?>? {
    val resIds = resolveIntArrayRes(resId) ?: return null
    val result = arrayOfNulls<Int>(resIds.size)
    for (i in resIds.indices) {
      result[i] = resIds[i]
    }
    return result
  }

  fun resolveBoolRes(@BoolRes resId: Int): Boolean {
    if (resId != 0) {
      val cached = resourceCache.get<Boolean>(resId)
      if (cached != null) {
        return cached
      }
      val result = resources.getBoolean(resId)
      resourceCache[resId] = result
      return result
    }
    return false
  }

  @ColorInt
  fun resolveColorRes(@ColorRes resId: Int): Int {
    if (resId != 0) {
      val cached = resourceCache.get<Int>(resId)
      if (cached != null) {
        return cached
      }
      val result = ContextCompat.getColor(androidContext, resId)
      resourceCache[resId] = result
      return result
    }
    return 0
  }

  fun resolveDimenSizeRes(@DimenRes resId: Int): Int {
    if (resId != 0) {
      val cached = resourceCache.get<Int>(resId)
      if (cached != null) {
        return cached
      }
      val result = resources.getDimensionPixelSize(resId)
      resourceCache[resId] = result
      return result
    }
    return 0
  }

  fun resolveDimenOffsetRes(@DimenRes resId: Int): Int {
    if (resId != 0) {
      val cached = resourceCache.get<Int>(resId)
      if (cached != null) {
        return cached
      }
      val result = resources.getDimensionPixelOffset(resId)
      resourceCache[resId] = result
      return result
    }
    return 0
  }

  fun resolveFloatRes(@DimenRes resId: Int): Float {
    if (resId != 0) {
      val cached = resourceCache.get<Float>(resId)
      if (cached != null) {
        return cached
      }
      val result = resources.getDimension(resId)
      resourceCache[resId] = result
      return result
    }
    return 0f
  }

  fun resolveDrawableRes(@DrawableRes resId: Int): Drawable? {
    return if (resId == 0) {
      null
    } else ContextCompat.getDrawable(androidContext, resId)
  }

  fun resolveStringAttr(@AttrRes attrResId: Int, @StringRes defResId: Int): String? {
    val a = theme.obtainStyledAttributes(intArrayOf(attrResId))
    return try {
      var result = a.getString(0)
      if (result == null) {
        result = resolveStringRes(defResId)
      }
      result
    } finally {
      a.recycle()
    }
  }

  fun resolveStringArrayAttr(@AttrRes attrResId: Int, @ArrayRes defResId: Int): Array<String>? {
    val a = theme.obtainStyledAttributes(intArrayOf(attrResId))
    return try {
      resolveStringArrayRes(a.getResourceId(0, defResId))
    } finally {
      a.recycle()
    }
  }

  fun resolveIntAttr(@AttrRes attrResId: Int, @IntegerRes defResId: Int): Int {
    val a = theme.obtainStyledAttributes(intArrayOf(attrResId))
    return try {
      a.getInt(0, resolveIntRes(defResId))
    } finally {
      a.recycle()
    }
  }

  fun resolveIntArrayAttr(@AttrRes attrResId: Int, @ArrayRes defResId: Int): IntArray? {
    val a = theme.obtainStyledAttributes(intArrayOf(attrResId))
    return try {
      resolveIntArrayRes(a.getResourceId(0, defResId))
    } finally {
      a.recycle()
    }
  }

  fun resolveIntegerArrayAttr(@AttrRes attrResId: Int, @ArrayRes defResId: Int): Array<Int?>? {
    val resIds = resolveIntArrayAttr(attrResId, defResId) ?: return null
    val result = arrayOfNulls<Int>(resIds.size)
    for (i in resIds.indices) {
      result[i] = resIds[i]
    }
    return result
  }

  fun resolveBoolAttr(@AttrRes attrResId: Int, @BoolRes defResId: Int): Boolean {
    val a = theme.obtainStyledAttributes(intArrayOf(attrResId))
    return try {
      a.getBoolean(0, resolveBoolRes(defResId))
    } finally {
      a.recycle()
    }
  }

  fun resolveColorAttr(@AttrRes attrResId: Int, @ColorRes defResId: Int): Int {
    val a = theme.obtainStyledAttributes(intArrayOf(attrResId))
    return try {
      a.getColor(0, resolveColorRes(defResId))
    } finally {
      a.recycle()
    }
  }

  fun resolveDimenSizeAttr(@AttrRes attrResId: Int, @DimenRes defResId: Int): Int {
    val a = theme.obtainStyledAttributes(intArrayOf(attrResId))
    return try {
      a.getDimensionPixelSize(0, resolveDimenSizeRes(defResId))
    } finally {
      a.recycle()
    }
  }

  fun resolveDimenOffsetAttr(@AttrRes attrResId: Int, @DimenRes defResId: Int): Int {
    val a = theme.obtainStyledAttributes(intArrayOf(attrResId))
    return try {
      a.getDimensionPixelOffset(0, resolveDimenOffsetRes(defResId))
    } finally {
      a.recycle()
    }
  }

  fun resolveFloatAttr(@AttrRes attrResId: Int, @DimenRes defResId: Int): Float {
    val a = theme.obtainStyledAttributes(intArrayOf(attrResId))
    return try {
      a.getDimension(0, resolveFloatRes(defResId))
    } finally {
      a.recycle()
    }
  }

  fun resolveDrawableAttr(@AttrRes attrResId: Int, @DrawableRes defResId: Int): Drawable? {
    if (attrResId == 0) {
      return null
    }
    val a = theme.obtainStyledAttributes(intArrayOf(attrResId))
    return try {
      resolveDrawableRes(a.getResourceId(0, defResId))
    } finally {
      a.recycle()
    }
  }

  fun resolveResIdAttr(@AttrRes attrResId: Int, defResId: Int): Int {
    val a = theme.obtainStyledAttributes(intArrayOf(attrResId))
    return try {
      a.getResourceId(0, defResId)
    } finally {
      a.recycle()
    }
  }
}
