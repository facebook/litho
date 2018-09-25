/*
 * Copyright 2018-present Facebook, Inc.
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
package com.facebook.litho.drawable;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.v4.util.Pools;
import javax.annotation.Nullable;

/** ComparableColorDrawable */
public class ComparableColorDrawable implements ComparableDrawable<ColorDrawable> {

  private static final Pools.SynchronizedPool<ColorDrawable> sDrawablePool =
      new Pools.SynchronizedPool<>(8);

  @ColorInt private final int mColor;
  private @Nullable ColorDrawable mDrawable;

  private ComparableColorDrawable(@ColorInt int color) {
    this.mColor = color;
  }

  @Override
  public ColorDrawable acquire(Context context) {
    if (mDrawable == null) {
      mDrawable = acquire(mColor);
    }

    return mDrawable;
  }

  @Override
  public void release(Context context) {
    if (mDrawable != null) {
      sDrawablePool.release(mDrawable);
    }
    mDrawable = null;
  }

  @Override
  public boolean isEquivalentTo(@Nullable ComparableDrawable other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof ComparableColorDrawable)) {
      return false;
    }

    return this.mColor == ((ComparableColorDrawable) other).mColor;
  }

  private static ColorDrawable acquire(@ColorInt int color) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
      return new ColorDrawable(color);
    }

    ColorDrawable drawable = sDrawablePool.acquire();
    if (drawable == null) {
      drawable = new ColorDrawable(color);
    } else {
      drawable.setColor(color);
    }

    return drawable;
  }

  public static ComparableColorDrawable create(@ColorInt int color) {
    return new ComparableColorDrawable(color);
  }
}
