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

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;
import com.facebook.litho.config.ComponentsConfiguration;
import javax.annotation.Nullable;

/**
 * Default Comparable Drawable delegates all calls to its wrapped {@link Drawable}.
 *
 * <p>The wrapped {@link Drawable} <em>must</em> be fully released from any {@link View} before
 * wrapping, otherwise internal {@link Drawable.Callback} may be dropped.
 *
 * <p>Using this is not the recommended as semantically equal drawables will not be considered
 * equivalent because they will have different references, which could make diffing inefficient.
 */
public class DefaultComparableDrawable extends ComparableDrawable implements Drawable.Callback {

  private Drawable mDrawable;

  protected DefaultComparableDrawable(Drawable drawable) {
    setWrappedDrawable(drawable);
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    mDrawable.setBounds(bounds);
  }

  @Override
  public int getChangingConfigurations() {
    return mDrawable.getChangingConfigurations();
  }

  @Override
  public void setChangingConfigurations(int configs) {
    mDrawable.setChangingConfigurations(configs);
  }

  @Override
  public void setDither(boolean dither) {
    mDrawable.setDither(dither);
  }

  @Override
  public void setFilterBitmap(boolean filter) {
    mDrawable.setFilterBitmap(filter);
  }

  @Override
  public boolean isStateful() {
    return mDrawable.isStateful();
  }

  @Override
  public boolean setState(final int[] state) {
    return mDrawable.setState(state);
  }

  @Override
  public int[] getState() {
    return mDrawable.getState();
  }

  @Override
  public void jumpToCurrentState() {
    mDrawable.jumpToCurrentState();
  }

  @Override
  public Drawable getCurrent() {
    return mDrawable.getCurrent();
  }

  @Override
  public boolean setVisible(boolean visible, boolean restart) {
    return super.setVisible(visible, restart) || mDrawable.setVisible(visible, restart);
  }

  @Override
  public Region getTransparentRegion() {
    return mDrawable.getTransparentRegion();
  }

  @Override
  public int getIntrinsicWidth() {
    return mDrawable.getIntrinsicWidth();
  }

  @Override
  public int getIntrinsicHeight() {
    return mDrawable.getIntrinsicHeight();
  }

  @Override
  public int getMinimumWidth() {
    return mDrawable.getMinimumWidth();
  }

  @Override
  public int getMinimumHeight() {
    return mDrawable.getMinimumHeight();
  }

  @Override
  public boolean getPadding(Rect padding) {
    return mDrawable.getPadding(padding);
  }

  @Override
  protected boolean onLevelChange(int level) {
    return mDrawable.setLevel(level);
  }

  @Override
  public boolean isAutoMirrored() {
    return DrawableCompat.isAutoMirrored(mDrawable);
  }

  @Override
  public void setAutoMirrored(boolean mirrored) {
    DrawableCompat.setAutoMirrored(mDrawable, mirrored);
  }

  @Override
  public void setTint(int tint) {
    DrawableCompat.setTint(mDrawable, tint);
  }

  @Override
  public void setTintList(ColorStateList tint) {
    DrawableCompat.setTintList(mDrawable, tint);
  }

  @Override
  public void setTintMode(PorterDuff.Mode mode) {
    DrawableCompat.setTintMode(mDrawable, mode);
  }

  @Override
  public void setHotspot(float x, float y) {
    DrawableCompat.setHotspot(mDrawable, x, y);
  }

  @Override
  public void setHotspotBounds(int left, int top, int right, int bottom) {
    DrawableCompat.setHotspotBounds(mDrawable, left, top, right, bottom);
  }

  @Override
  public void draw(Canvas canvas) {
    mDrawable.draw(canvas);
  }

  @Override
  public void setAlpha(int alpha) {
    mDrawable.setAlpha(alpha);
  }

  @Override
  public void setColorFilter(ColorFilter filter) {
    mDrawable.setColorFilter(filter);
  }

  @Override
  public int getOpacity() {
    return mDrawable.getOpacity();
  }

  @Override
  public void invalidateDrawable(Drawable drawable) {
    invalidateSelf();
  }

  @Override
  public void scheduleDrawable(Drawable drawable, Runnable runnable, long l) {
    scheduleSelf(runnable, l);
  }

  @Override
  public void unscheduleDrawable(Drawable drawable, Runnable runnable) {
    unscheduleSelf(runnable);
  }

  @Override
  public boolean isEquivalentTo(ComparableDrawable other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof DefaultComparableDrawable)) {
      return false;
    }

    return mDrawable.equals(((DefaultComparableDrawable) other).mDrawable);
  }

  public Drawable getWrappedDrawable() {
    return mDrawable;
  }

  public void setWrappedDrawable(Drawable drawable) {
    if (drawable instanceof ComparableDrawable) {
      throw new IllegalArgumentException("drawable is already a ComparableDrawable");
    }

    if (mDrawable != null) {
      mDrawable.setCallback(null);
    }

    mDrawable = drawable;

    if (drawable != null) {
      drawable.setCallback(this);
    }
  }

  /**
   * Use to create a wrapper for a drawable which does <em>not</em> extend comparable drawable for
   * backwards compatibility. Use this method only when the value of the property is going to be
   * constant (i.e. does not depend on state)
   *
   * <p>If used with stateful values litho may not update the property during updates. This is
   * because every {@link DefaultComparableDrawable} will be considered equivalent to improve mount
   * performance.
   *
   * @see ComparableColorDrawable
   * @see ComparableResDrawable
   * @see ComparableDrawable
   */
  public static DefaultComparableDrawable create(Drawable drawable) {
    return new DefaultComparableDrawable(drawable);
  }

  public static boolean isDefaultComparableDrawable(ComparableDrawable comparable) {
    return (comparable instanceof DefaultComparableDrawable);
  }

  public static boolean isEquivalentToWithExperiment(
      @Nullable ComparableDrawable x, @Nullable ComparableDrawable y) {
    if (x == null) {
      return y == null;
    } else if (y == null) {
      return false;
    }

    if (ComponentsConfiguration.areDefaultComparableDrawablesAlwaysEquivalent
        && isDefaultComparableDrawable(x)
        && isDefaultComparableDrawable(y)) {
      return true;
    }

    return x.isEquivalentTo(y);
  }
}
