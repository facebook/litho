/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

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

import com.facebook.components.reference.Reference;
import com.facebook.components.reference.ResourceDrawableReference;

public class ResourceResolver {
  private ComponentContext mContext;
  private Resources mResources;
  private Resources.Theme mTheme;
  private ResourceCache mResourceCache;

  // Use for *Attr methods to retrieve attributes without needing to
  // allocate a new int[] for each call.
  private final int[] mAttrs = new int[1];

  public final void init(ComponentContext context, ResourceCache resourceCache) {
    mContext = context;
    mResources = context.getResources();
    mTheme = context.getTheme();
    mResourceCache = resourceCache;
  }

  protected final int dipsToPixels(float dips) {
    final float scale = mResources.getDisplayMetrics().density;
    return FastMath.round(dips * scale);
  }

  final int dipsToPixels(int dips) {
    return dipsToPixels((float) dips);
  }

  protected final int sipsToPixels(float dips) {
    final float scale = mResources.getDisplayMetrics().scaledDensity;
    return FastMath.round(dips * scale);
  }

  public final int sipsToPixels(int dips) {
    return dipsToPixels((float) dips);
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

  private final String[] resolveStringArrayRes(@ArrayRes int resId) {
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
  protected final Reference<Drawable> resolveDrawableRes(@DrawableRes int resId) {
    if (resId == 0) {
      return null;
    }

    return ResourceDrawableReference.create(mContext)
        .resId(resId)
        .build();
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

  public final String[] resolveStringArrayAttr(@AttrRes int attrResId, @ArrayRes int defResId) {
    mAttrs[0] = attrResId;
    TypedArray a = mContext.getTheme().obtainStyledAttributes(mAttrs);

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

  public final int[] resolveIntArrayAttr(@AttrRes int attrResId, @ArrayRes int defResId) {
    mAttrs[0] = attrResId;
    TypedArray a = mContext.getTheme().obtainStyledAttributes(mAttrs);

    try {
      return resolveIntArrayRes(a.getResourceId(0, defResId));
    } finally {
      a.recycle();
    }
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
