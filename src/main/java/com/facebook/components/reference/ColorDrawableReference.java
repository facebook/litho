/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.reference;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorRes;
import android.support.v4.util.Pools;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.config.ComponentsConfiguration;

/**
 * A Reference for {@link ColorDrawable}. This keeps a {@link Pools.Pool} of up to 10 ColorDrawable
 * and allows to specify color and alpha as create.
 */
public final class ColorDrawableReference extends ReferenceLifecycle<Drawable> {

  private static final int DEFAULT_ALPHA = 255;
  private static final int INITIAL_POOL_SIZE = 50;
  // setColor() was introduced only in Honeycomb.
  private static final boolean CAN_RECYCLE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

  private static ColorDrawableReference sInstance;

  private static final Pools.SynchronizedPool<PropsBuilder> mBuilderPool =
      new Pools.SynchronizedPool<PropsBuilder>(2);

  private static final Pools.Pool<ColorDrawable> sPool = CAN_RECYCLE
      ? new Pools.SynchronizedPool<ColorDrawable>(INITIAL_POOL_SIZE)
      : null;

  private ColorDrawableReference() {
  }

  public static synchronized ColorDrawableReference get() {
    if (sInstance == null) {
      sInstance = new ColorDrawableReference();
    }

    return sInstance;
  }

