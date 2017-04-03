/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.reference;

import android.graphics.drawable.Drawable;
import android.support.annotation.Px;
import android.support.v4.util.Pools;

import com.facebook.litho.BorderColorDrawable;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.ComponentsPools;

/**
 * A Reference for {@link com.facebook.litho.BorderColorDrawable}.
 */
public class BorderColorDrawableReference extends ReferenceLifecycle<Drawable> {

  private static final Pools.SynchronizedPool<BorderColorDrawableReference.PropsBuilder>
      mBuilderPool = new Pools.SynchronizedPool<BorderColorDrawableReference.PropsBuilder>(2);

  private static BorderColorDrawableReference sInstance;

  private BorderColorDrawableReference() {
  }

  public static synchronized BorderColorDrawableReference get() {
    if (sInstance == null) {
      sInstance = new BorderColorDrawableReference();
    }

    return sInstance;
  }

  private static BorderColorDrawableReference.PropsBuilder newBuilder(
      ComponentContext context,
      BorderColorDrawableReference.State state) {
    BorderColorDrawableReference.PropsBuilder builder = mBuilderPool.acquire();
    if (builder == null) {
      builder = new BorderColorDrawableReference.PropsBuilder();
    }

    builder.init(context, state);

    return builder;
  }

  public static BorderColorDrawableReference.PropsBuilder create(ComponentContext c) {
    return newBuilder(c, new BorderColorDrawableReference.State());
  }

  @Override
  protected Drawable onAcquire(
      ComponentContext context,
      Reference reference) {
    BorderColorDrawable drawable = ComponentsPools.acquireBorderColorDrawable();

    BorderColorDrawableReference.State state = (BorderColorDrawableReference.State) reference;

    drawable.init(
        state.mColor,
        state.mBorderLeft,
        state.mBorderTop,
        state.mBorderRight,
        state.mBorderBottom);

    return drawable;
  }

  @Override
  protected void onRelease(
      ComponentContext context,
      Drawable drawable,
      Reference reference) {
    ComponentsPools.release((BorderColorDrawable) drawable);
  }

  private static class State extends Reference<Drawable> {

    int mColor;
    int mBorderLeft;
    int mBorderTop;
    int mBorderRight;
    int mBorderBottom;

    @Override
    public String getSimpleName() {
      return "BorderColorDrawableReference";
    }

    protected State() {
      super(get());
    }

    @Override
    public int hashCode() {
      int result = mColor;
      result = 31 * result + mBorderLeft;
      result = 31 * result + mBorderTop;
      result = 31 * result + mBorderRight;
      result = 31 * result + mBorderBottom;
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

      BorderColorDrawableReference.State state = (BorderColorDrawableReference.State) o;

      if (mColor != state.mColor
          || mBorderLeft != state.mBorderLeft
          || mBorderTop != state.mBorderTop
          || mBorderRight != state.mBorderRight
          || mBorderBottom != state.mBorderBottom) {
        return false;
      }

      return true;
    }
  }

  public static class PropsBuilder extends Reference.Builder<Drawable> {

    private BorderColorDrawableReference.State mState;
    private ComponentContext mContext;

    protected void init(ComponentContext context, BorderColorDrawableReference.State state) {
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

    public BorderColorDrawableReference.PropsBuilder color(int color) {
      mState.mColor = color;
      return this;
    }

    public BorderColorDrawableReference.PropsBuilder borderLeft(@Px int borderLeft) {
      mState.mBorderLeft = borderLeft;
      return this;
    }

    public BorderColorDrawableReference.PropsBuilder borderTop(@Px int borderTop) {
      mState.mBorderTop = borderTop;
      return this;
    }

    public BorderColorDrawableReference.PropsBuilder borderRight(@Px int borderRight) {
      mState.mBorderRight = borderRight;
      return this;
    }

    public BorderColorDrawableReference.PropsBuilder borderBottom(@Px int borderBottom) {
      mState.mBorderBottom = borderBottom;
      return this;
    }

    @Override
    public Reference<Drawable> build() {
      BorderColorDrawableReference.State state = mState;

      release();

      return state;
    }
  }
}
