/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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

import com.facebook.litho.reference.Reference;
import com.facebook.litho.reference.ResourceDrawableReference;

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

