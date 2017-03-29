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

  private static PropsBuilder newBuilder(ComponentContext context, State state) {
    PropsBuilder builder = mBuilderPool.acquire();
    if (builder == null) {
      builder = new PropsBuilder();
    }

    builder.init(context, state);

    return builder;
  }

  public static PropsBuilder create(ComponentContext c) {
    return newBuilder(c, new State());
  }

  @Override
  protected Drawable onAcquire(
      ComponentContext context,
      Reference reference) {
    ColorDrawable drawable = null;

    if (CAN_RECYCLE) {
      drawable = sPool.acquire();
    }

    if (drawable == null) {
      drawable = new ColorDrawable(((State) reference).mColor);
    } else {
      drawable.setColor(((State) reference).mColor);
    }

    drawable.setAlpha(((State) reference).mAlpha);

    return drawable;
  }

  @Override
  protected void onRelease(
      ComponentContext context,
      Drawable drawable,
      Reference reference) {
    if (CAN_RECYCLE) {
      sPool.release((ColorDrawable) drawable);
    }
  }

  private static class State extends Reference<Drawable> {

    int mColor;
    int mAlpha = DEFAULT_ALPHA;

    @Override
    public String getSimpleName() {
      return "ColorDrawableReference";
    }

    protected State() {
      super(get());
    }

    @Override
    public int hashCode() {
      int result = mColor;
      result = 31 * result + mAlpha;
      return result;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }

      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      State state = (State) o;

      if (mColor != state.mColor) {
        return false;
      }

      return mAlpha == state.mAlpha;
    }
  }

  public static class PropsBuilder extends Reference.Builder<Drawable> {

    private State mState;
    private ComponentContext mContext;

    protected void init(ComponentContext context, State state) {
      super.init(context, state);
      mState = state;
      mContext = context;
    }

    @Override
    protected void release() {
      super.release();

      mState = null;
      mContext = null;
      mBuilderPool.release(this);
    }

