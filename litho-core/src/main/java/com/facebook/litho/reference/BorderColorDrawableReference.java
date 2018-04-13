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

package com.facebook.litho.reference;

import android.graphics.PathEffect;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.Px;
import android.support.v4.util.Pools;
import com.facebook.litho.BorderColorDrawable;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentsPools;
import java.util.Arrays;
import javax.annotation.Nullable;

/** A Reference for {@link com.facebook.litho.BorderColorDrawable}. */
public class BorderColorDrawableReference extends ReferenceLifecycle<Drawable> {

  private static final Pools.SynchronizedPool<BorderColorDrawableReference.PropsBuilder>
      sBuilderPool = new Pools.SynchronizedPool<>(2);

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
    BorderColorDrawableReference.PropsBuilder builder = sBuilderPool.acquire();
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
        state.mPathEffect,
        state.mBorderLeftWidth,
        state.mBorderTopWidth,
        state.mBorderRightWidth,
        state.mBorderBottomWidth,
        state.mBorderLeftColor,
        state.mBorderTopColor,
        state.mBorderRightColor,
        state.mBorderBottomColor,
        state.mBorderRadius);

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

    @Nullable PathEffect mPathEffect;
    @ColorInt int mBorderLeftColor;
    @ColorInt int mBorderTopColor;
    @ColorInt int mBorderRightColor;
    @ColorInt int mBorderBottomColor;
    int mBorderLeftWidth;
    int mBorderTopWidth;
    int mBorderRightWidth;
    int mBorderBottomWidth;

    float[] mBorderRadius;

    @Override
    public String getSimpleName() {
      return "BorderColorDrawableReference";
    }

    protected State() {
      super(get());
    }

    @Override
    public int hashCode() {
      int result = mBorderLeftColor;
      result = 31 * result + mBorderTopColor;
      result = 31 * result + mBorderRightColor;
      result = 31 * result + mBorderBottomColor;
      result = 31 * result + mBorderLeftWidth;
      result = 31 * result + mBorderTopWidth;
      result = 31 * result + mBorderRightWidth;
      result = 31 * result + mBorderBottomWidth;
      result = 31 * result + Arrays.hashCode(mBorderRadius);
      result = 31 * result + (mPathEffect != null ? mPathEffect.hashCode() : 0);
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

      return mBorderLeftColor == state.mBorderLeftColor
          && mBorderTopColor == state.mBorderTopColor
          && mBorderRightColor == state.mBorderRightColor
          && mBorderBottomColor == state.mBorderBottomColor
          && mBorderLeftWidth == state.mBorderLeftWidth
          && mBorderTopWidth == state.mBorderTopWidth
          && mBorderRightWidth == state.mBorderRightWidth
          && mBorderBottomWidth == state.mBorderBottomWidth
          && Arrays.equals(mBorderRadius, state.mBorderRadius)
          && (mPathEffect == state.mPathEffect
              || (mPathEffect != null && mPathEffect.equals(state.mPathEffect)));
    }
  }

  public static class PropsBuilder extends Reference.Builder<Drawable> {

    private BorderColorDrawableReference.State mState;

    protected void init(ComponentContext context, BorderColorDrawableReference.State state) {
      super.init(context, state);
      mState = state;
    }

    @Override
    protected void release() {
      super.release();

      mState = null;
      sBuilderPool.release(this);
    }

    public BorderColorDrawableReference.PropsBuilder pathEffect(@Nullable PathEffect pathEffect) {
      mState.mPathEffect = pathEffect;
      return this;
    }

    public BorderColorDrawableReference.PropsBuilder borderLeftColor(@ColorInt int color) {
      mState.mBorderLeftColor = color;
      return this;
    }

    public BorderColorDrawableReference.PropsBuilder borderTopColor(@ColorInt int color) {
      mState.mBorderTopColor = color;
      return this;
    }

    public BorderColorDrawableReference.PropsBuilder borderRightColor(@ColorInt int color) {
      mState.mBorderRightColor = color;
      return this;
    }

    public BorderColorDrawableReference.PropsBuilder borderBottomColor(@ColorInt int color) {
      mState.mBorderBottomColor = color;
      return this;
    }

    public BorderColorDrawableReference.PropsBuilder borderLeftWidth(@Px int borderLeft) {
      mState.mBorderLeftWidth = borderLeft;
      return this;
    }

    public BorderColorDrawableReference.PropsBuilder borderTopWidth(@Px int borderTop) {
      mState.mBorderTopWidth = borderTop;
      return this;
    }

    public BorderColorDrawableReference.PropsBuilder borderRightWidth(@Px int borderRight) {
      mState.mBorderRightWidth = borderRight;
      return this;
    }

    public BorderColorDrawableReference.PropsBuilder borderBottomWidth(@Px int borderBottom) {
      mState.mBorderBottomWidth = borderBottom;
      return this;
    }

    public BorderColorDrawableReference.PropsBuilder borderRadius(float[] radius) {
      mState.mBorderRadius = Arrays.copyOf(radius, radius.length);
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
