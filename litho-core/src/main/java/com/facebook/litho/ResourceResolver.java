/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.ArrayRes;
import android.support.annotation.AttrRes;
import android.support.annotation.BoolRes;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntegerRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

public class ResourceResolver {
  private Resources mResources;
  private Resources.Theme mTheme;
  private ResourceCache mResourceCache;

  // Use for *Attr methods to retrieve attributes without needing to
  // allocate a new int[] for each call.
  private final int[] mAttrs = new int[1];

  public final void init(ComponentContext context, ResourceCache resourceCache) {
    mResources = context.getResources();
    mTheme = context.getTheme();
    mResourceCache = resourceCache;
  }

  protected final int dipsToPixels(float dips) {
    final float scale = mResources.getDisplayMetrics().density;
    return FastMath.round(dips * scale);
  }

  protected final int sipsToPixels(float dips) {
    final float scale = mResources.getDisplayMetrics().scaledDensity;
    return FastMath.round(dips * scale);
  }

  protected final String resolveStringRes(@StringRes int resId) {
    if (resId != 0) {
      String cached = mResourceCache.get(resId);
      if (cached != null) {
        return cached;
      }

      String result = mResources.getString(resId);
      mResourceCache.put(resId, result);

      return result;
    }

    return null;
  }

  protected final String resolveStringRes(@StringRes int resId, Object... formatArgs) {
    return resId != 0 ? mResources.getString(resId, formatArgs) : null;
  }

  @Nullable
  protected final String[] resolveStringArrayRes(@ArrayRes int resId) {
    if (resId != 0) {
      String[] cached = mResourceCache.get(resId);
      if (cached != null) {
        return cached;
      }

      String[] result = mResources.getStringArray(resId);
      mResourceCache.put(resId, result);

      return result;
    }

    return null;
  }

  protected final int resolveIntRes(@IntegerRes int resId) {
    if (resId != 0) {
      Integer cached = mResourceCache.get(resId);
      if (cached != null) {
        return cached;
      }

      int result = mResources.getInteger(resId);
      mResourceCache.put(resId, result);

      return result;
    }

    return 0;
  }

  @Nullable
  private final int[] resolveIntArrayRes(@ArrayRes int resId) {
    if (resId != 0) {
      int[] cached = mResourceCache.get(resId);
      if (cached != null) {
        return cached;
      }

      int[] result = mResources.getIntArray(resId);
      mResourceCache.put(resId, result);

      return result;
    }

    return null;
  }

  @Nullable
  protected final Integer[] resolveIntegerArrayRes(@ArrayRes int resId) {
    int[] resIds = resolveIntArrayRes(resId);
    if (resIds == null) return null;
    Integer[] result = new Integer[resIds.length];
    for (int i = 0; i < resIds.length; i++) {
      result[i] = resIds[i];
    }
    return result;
  }

  protected final boolean resolveBoolRes(@BoolRes int resId) {
    if (resId != 0) {
      Boolean cached = mResourceCache.get(resId);
      if (cached != null) {
        return cached;
      }

      boolean result = mResources.getBoolean(resId);
      mResourceCache.put(resId, result);

      return result;
    }

    return false;
  }

  protected final int resolveColorRes(@ColorRes int resId) {
    if (resId != 0) {
      Integer cached = mResourceCache.get(resId);
      if (cached != null) {
        return cached;
      }

      int result = mResources.getColor(resId);
      mResourceCache.put(resId, result);

      return result;
    }

    return 0;
  }

  protected final int resolveDimenSizeRes(@DimenRes int resId) {
    if (resId != 0) {
      Integer cached = mResourceCache.get(resId);
      if (cached != null) {
        return cached;
      }

      int result = mResources.getDimensionPixelSize(resId);
      mResourceCache.put(resId, result);

      return result;
    }

    return 0;
  }

  protected final int resolveDimenOffsetRes(@DimenRes int resId) {
    if (resId != 0) {
      Integer cached = mResourceCache.get(resId);
      if (cached != null) {
        return cached;
      }

      int result = mResources.getDimensionPixelOffset(resId);
      mResourceCache.put(resId, result);

      return result;
    }

    return 0;
  }

  protected final float resolveFloatRes(@DimenRes int resId) {
    if (resId != 0) {
      Float cached = mResourceCache.get(resId);
      if (cached != null) {
        return cached;
      }

      float result = mResources.getDimension(resId);
      mResourceCache.put(resId, result);

      return result;
    }

    return 0;
  }

  @Nullable
  protected final Drawable resolveDrawableRes(@DrawableRes int resId) {
    if (resId == 0) {
      return null;
    }

    return mResources.getDrawable(resId);
  }

  protected final String resolveStringAttr(@AttrRes int attrResId, @StringRes int defResId) {
    mAttrs[0] = attrResId;
    TypedArray a = mTheme.obtainStyledAttributes(mAttrs);

    try {
      String result = a.getString(0);
      if (result == null) {
        result = resolveStringRes(defResId);
      }

      return result;
    } finally {
      a.recycle();
    }
  }

  @Nullable
  public final String[] resolveStringArrayAttr(@AttrRes int attrResId, @ArrayRes int defResId) {
    mAttrs[0] = attrResId;
    TypedArray a = mTheme.obtainStyledAttributes(mAttrs);

    try {
      return resolveStringArrayRes(a.getResourceId(0, defResId));
    } finally {
      a.recycle();
    }
  }

  protected final int resolveIntAttr(@AttrRes int attrResId, @IntegerRes int defResId) {
    mAttrs[0] = attrResId;
    TypedArray a = mTheme.obtainStyledAttributes(mAttrs);

    try {
      return a.getInt(0, resolveIntRes(defResId));
    } finally {
      a.recycle();
    }
  }

  @Nullable
  public final int[] resolveIntArrayAttr(@AttrRes int attrResId, @ArrayRes int defResId) {
    mAttrs[0] = attrResId;
    TypedArray a = mTheme.obtainStyledAttributes(mAttrs);

    try {
      return resolveIntArrayRes(a.getResourceId(0, defResId));
    } finally {
      a.recycle();
    }
  }

  @Nullable
  protected final Integer[] resolveIntegerArrayAttr(
      @AttrRes int attrResId, @ArrayRes int defResId) {
    int[] resIds = resolveIntArrayAttr(attrResId, defResId);
    if (resIds == null) return null;
    Integer[] result = new Integer[resIds.length];
    for (int i = 0; i < resIds.length; i++) {
      result[i] = resIds[i];
    }
    return result;
  }

  protected final boolean resolveBoolAttr(@AttrRes int attrResId, @BoolRes int defResId) {
    mAttrs[0] = attrResId;
    TypedArray a = mTheme.obtainStyledAttributes(mAttrs);

    try {
      return a.getBoolean(0, resolveBoolRes(defResId));
    } finally {
      a.recycle();
    }
  }

  protected final int resolveColorAttr(@AttrRes int attrResId, @ColorRes int defResId) {
    mAttrs[0] = attrResId;
    TypedArray a = mTheme.obtainStyledAttributes(mAttrs);

    try {
      return a.getColor(0, resolveColorRes(defResId));
    } finally {
      a.recycle();
    }
  }

  protected final int resolveDimenSizeAttr(@AttrRes int attrResId, @DimenRes int defResId) {
    mAttrs[0] = attrResId;
    TypedArray a = mTheme.obtainStyledAttributes(mAttrs);

    try {
      return a.getDimensionPixelSize(0, resolveDimenSizeRes(defResId));
    } finally {
      a.recycle();
    }
  }

  protected final int resolveDimenOffsetAttr(@AttrRes int attrResId, @DimenRes int defResId) {
    mAttrs[0] = attrResId;
    TypedArray a = mTheme.obtainStyledAttributes(mAttrs);

    try {
      return a.getDimensionPixelOffset(0, resolveDimenOffsetRes(defResId));
    } finally {
      a.recycle();
    }
  }

  protected final float resolveFloatAttr(@AttrRes int attrResId, @DimenRes int defResId) {
    mAttrs[0] = attrResId;
    TypedArray a = mTheme.obtainStyledAttributes(mAttrs);

    try {
      return a.getDimension(0, resolveFloatRes(defResId));
    } finally {
      a.recycle();
    }
  }

  @Nullable
  protected final Drawable resolveDrawableAttr(
      @AttrRes int attrResId,
      @DrawableRes int defResId) {
    if (attrResId == 0) {
      return null;
    }

    mAttrs[0] = attrResId;
    TypedArray a = mTheme.obtainStyledAttributes(mAttrs);

    try {
      return resolveDrawableRes(a.getResourceId(0, defResId));
    } finally {
      a.recycle();
    }
  }

  final int resolveResIdAttr(@AttrRes int attrResId, int defResId) {
    mAttrs[0] = attrResId;
    TypedArray a = mTheme.obtainStyledAttributes(mAttrs);

    try {
      return a.getResourceId(0, defResId);
    } finally {
      a.recycle();
    }
  }

  protected void release() {
    internalRelease();
  }

  void internalRelease() {
    mResources = null;
    mTheme = null;
    mResourceCache = null;
  }
}
