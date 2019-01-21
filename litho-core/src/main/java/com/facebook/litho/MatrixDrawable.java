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

import static android.os.Build.VERSION_CODES.HONEYCOMB;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import javax.annotation.Nullable;

/**
 * A Drawable that wraps another drawable.
 */
public class MatrixDrawable<T extends Drawable> extends Drawable
    implements Drawable.Callback, Touchable {

  public static final int UNSET = -1;

  private T mDrawable;
  private DrawableMatrix mMatrix;
  private boolean mShouldClipRect;
  private int mWidth;
  private int mHeight;

  public MatrixDrawable() {
    super();
  }

  public void mount(T drawable, DrawableMatrix matrix) {
    if (mDrawable == drawable) {
      return;
    }

    if (mDrawable != null) {
      setDrawableVisibilitySafe(false, false);
      mDrawable.setCallback(null);
    }

    mDrawable = drawable;

    if (mDrawable != null) {
      setDrawableVisibilitySafe(isVisible(), false);
      mDrawable.setCallback(this);
    }

    mMatrix = matrix;

    // We should clip rect if either the transformation matrix needs so or
    // if a ColorDrawable in Gingerbread is being drawn because it doesn't
    // respect its bounds.
    mShouldClipRect = (mMatrix != null && mMatrix.shouldClipRect()) ||
        (Build.VERSION.SDK_INT < HONEYCOMB && mDrawable instanceof ColorDrawable) ||
        (mDrawable instanceof InsetDrawable);

    invalidateSelf();
  }

  /**
   * Sets the necessary artifacts to display the given drawable.
   * This method should be called in your component's @OnMount method.
   *
   * @param drawable The drawable to be drawn.
   */
  public void mount(T drawable) {
    mount(drawable, null);
  }

  /**
   * Applies the given dimensions to the drawable.
   * This method should be called in your component's @OnBind method.
   *
   * @param width The width of the drawable to be drawn.
   * @param height The height of the drawable to be drawn.
   */
  public void bind(int width, int height) {
    mWidth = width;
    mHeight = height;
    setInnerDrawableBounds(mWidth, mHeight);
  }

  @Override
  public void setBounds(int left, int top, int right, int bottom) {
    super.setBounds(left, top, right, bottom);
  }

  @Override
  public void setBounds(Rect bounds) {
    super.setBounds(bounds);
  }

  private void setInnerDrawableBounds(int width, int height) {
    if (mDrawable == null) {
      return;
    }

    mDrawable.setBounds(0, 0, width, height);
  }

  public void unmount() {
    if (mDrawable != null) {
      setDrawableVisibilitySafe(false, false);
      mDrawable.setCallback(null);
    }

    mDrawable = null;
    mMatrix = null;
    mShouldClipRect = false;
    mWidth = mHeight = 0;
  }

  public T getMountedDrawable() {
    return mDrawable;
  }

  private void setDrawableVisibilitySafe(boolean visible, boolean restart) {
    if (mDrawable != null && mDrawable.isVisible() != visible) {
      try {
        mDrawable.setVisible(visible, restart);
      } catch (NullPointerException e) {
        // Swallow. LayerDrawable on KitKat sometimes causes this, if some of its children are null.
        // This should not cause any rendering bugs, since visibility is anyway a "hint".
      }
    }
  }

  @Override
  public void draw(Canvas canvas) {
    if (mDrawable == null) {
      return;
    }

    final Rect bounds = getBounds();

    final int saveCount = canvas.save();
    canvas.translate(bounds.left, bounds.top);

    if (mShouldClipRect) {
      canvas.clipRect(0, 0, bounds.width(), bounds.height());
    }

    if (mMatrix != null) {
      canvas.concat(mMatrix);
    }

    mDrawable.draw(canvas);
    canvas.restoreToCount(saveCount);
  }

  @Override
  public void setChangingConfigurations(int configs) {
    if (mDrawable == null) {
      return;
    }
    mDrawable.setChangingConfigurations(configs);
  }

  @Override
  public int getChangingConfigurations() {
    return mDrawable == null ? UNSET : mDrawable.getChangingConfigurations();
  }

  @Override
  public void setDither(boolean dither) {
    if (mDrawable == null) {
      return;
    }
    mDrawable.setDither(dither);
  }

  @Override
  public void setFilterBitmap(boolean filter) {
    if (mDrawable == null) {
      return;
    }
    mDrawable.setFilterBitmap(filter);
  }

  @Override
  public void setAlpha(int alpha) {
    if (mDrawable == null) {
      return;
    }
    mDrawable.setAlpha(alpha);
  }

  @Override
  public void setColorFilter(ColorFilter cf) {
    if (mDrawable == null) {
      return;
    }
    mDrawable.setColorFilter(cf);
  }

  @Override
  public boolean isStateful() {
    return mDrawable != null && mDrawable.isStateful();
  }

  @Override
  public boolean setState(final int[] stateSet) {
    return mDrawable != null && mDrawable.setState(stateSet);
  }

  @Override
  public @Nullable int[] getState() {
    return mDrawable == null ? null : mDrawable.getState();
  }

  @Override
  public @Nullable Drawable getCurrent() {
    return mDrawable == null ? null : mDrawable.getCurrent();
  }

  @Override
  public boolean setVisible(boolean visible, boolean restart) {
    final boolean changed = super.setVisible(visible, restart);
    setDrawableVisibilitySafe(visible, restart);
    return changed;
  }

  @Override
  public int getOpacity() {
    return mDrawable == null ? UNSET : mDrawable.getOpacity();
  }

  @Override
  public @Nullable Region getTransparentRegion() {
    return mDrawable == null ? null : mDrawable.getTransparentRegion();
  }

  @Override
  public int getIntrinsicWidth() {
    return mDrawable == null ? UNSET : mDrawable.getIntrinsicWidth();
  }

  @Override
  public int getIntrinsicHeight() {
    return mDrawable == null ? UNSET : mDrawable.getIntrinsicHeight();
  }

  @Override
  public int getMinimumWidth() {
    return mDrawable == null ? UNSET : mDrawable.getMinimumWidth();
  }

  @Override
  public int getMinimumHeight() {
    return mDrawable == null ? UNSET : mDrawable.getMinimumHeight();
  }

  @Override
  public boolean getPadding(Rect padding) {
    return mDrawable != null && mDrawable.getPadding(padding);
  }

  @Override
  protected boolean onLevelChange(int level) {
    return mDrawable != null && mDrawable.setLevel(level);
  }

  @Override
  public void invalidateDrawable(Drawable who) {
    invalidateSelf();
  }

  @Override
  public void scheduleDrawable(Drawable who, Runnable what, long when) {
    scheduleSelf(what, when);
  }

  @Override
  public void unscheduleDrawable(Drawable who, Runnable what) {
    unscheduleSelf(what);
  }

  @Override
  @TargetApi(LOLLIPOP)
  public boolean onTouchEvent(MotionEvent event, View host) {
    final Rect bounds = getBounds();
    final int x = (int) event.getX() - bounds.left;
    final int y = (int) event.getY() - bounds.top;

    mDrawable.setHotspot(x, y);

    return false;
  }

  @Override
  public boolean shouldHandleTouchEvent(MotionEvent event) {
    return Build.VERSION.SDK_INT >= LOLLIPOP &&
        mDrawable != null &&
        mDrawable instanceof RippleDrawable &&
        event.getActionMasked() == MotionEvent.ACTION_DOWN &&
        getBounds().contains((int) event.getX(), (int) event.getY());
  }
}
