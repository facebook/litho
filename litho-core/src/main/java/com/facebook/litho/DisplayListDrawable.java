/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.annotation.TargetApi;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import com.facebook.litho.displaylist.DisplayList;
import com.facebook.litho.displaylist.DisplayListException;

/**
 * Drawable which supports {@link DisplayList} for drawing and delegates all other calls to it's
 * wrapped {@link android.graphics.drawable.Drawable}.
 */
class DisplayListDrawable extends Drawable implements Drawable.Callback {

  private Drawable mDrawable;
  private @Nullable DisplayListContainer mDisplayListContainer;
  private boolean mIgnoreInvalidations;
  private boolean mInvalidated;

  DisplayListDrawable(Drawable drawable, DisplayListContainer displayListContainer) {
    setWrappedDrawable(drawable, displayListContainer);
  }

  @Override
  public void draw(Canvas canvas) {
    if (mDisplayListContainer == null) {
      mDrawable.draw(canvas);
      return;
    }

    DisplayList displayList = mDisplayListContainer.getDisplayList();
    if (displayList == null && mDisplayListContainer.canCacheDrawingDisplayLists()) {
      displayList = DisplayList.createDisplayList(mDisplayListContainer.getName());
      mDisplayListContainer.setDisplayList(displayList);
      mInvalidated = true;
    }

    if (displayList == null) {
      mDrawable.draw(canvas);
      return;
    }

    try {
      if (mInvalidated || !displayList.isValid()) {
        drawContentIntoDisplayList();
        mInvalidated = false;
      }

      if (displayList.isValid()) {
        displayList.draw(canvas);
      } else {
        mDrawable.draw(canvas);
      }
    } catch (DisplayListException e) {
      // Let's make sure next draw calls will just bail the DisplayList part.
      mDisplayListContainer = null;
      mDrawable.draw(canvas);
    }
  }

  boolean willDrawDisplayList() {
    return mDisplayListContainer != null &&
        (mDisplayListContainer.hasValidDisplayList()
            || mDisplayListContainer.canCacheDrawingDisplayLists());
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    mDrawable.setBounds(bounds);
  }

  @Override
  public void setChangingConfigurations(int configs) {
    mDrawable.setChangingConfigurations(configs);
  }

  @Override
  public int getChangingConfigurations() {
    return mDrawable.getChangingConfigurations();
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
  public void setAlpha(int alpha) {
    mDrawable.setAlpha(alpha);
  }

  @Override
  public void setColorFilter(ColorFilter cf) {
    mDrawable.setColorFilter(cf);
  }

  @Override
  public boolean isStateful() {
    return mDrawable.isStateful();
  }

  @Override
  public boolean setState(final int[] stateSet) {
    return mDrawable.setState(stateSet);
  }

  @Override
  public int[] getState() {
    return mDrawable.getState();
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
  public int getOpacity() {
    return mDrawable.getOpacity();
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
  public Drawable mutate() {
    Drawable wrapped = mDrawable;
    Drawable mutated = wrapped.mutate();
    if (mutated != wrapped) {
      // If mutate() returned a new instance, update our reference
      setWrappedDrawable(mutated, mDisplayListContainer);
    }
    // We return ourselves, since only the wrapped drawable needs to mutate
    return this;
  }

  @Override
  public void invalidateDrawable(Drawable who) {
    invalidateSelf();
    if (mIgnoreInvalidations) {
      return;
    }

    // We need to make sure at this point that the bounds of the  {@link DisplayListDrawable} are
    // equals to the bounds of its content to make sure that the view invalidation works as
    // expected.
    setBounds(mDrawable.getBounds());
    mInvalidated = true;
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
  protected boolean onLevelChange(int level) {
    return mDrawable.setLevel(level);
  }

  @Override
  public void setTint(int tint) {
    setTintList(ColorStateList.valueOf(tint));
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  @Override
  public void setTintList(ColorStateList tint) {
    mDrawable.setTintList(tint);
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  @Override
  public void setTintMode(PorterDuff.Mode tintMode) {
    mDrawable.setTintMode(tintMode);
  }

  /**
   * Sets the current wrapped {@link Drawable}
   */
  void setWrappedDrawable(Drawable drawable, DisplayListContainer displayListContainer) {
    if (mDrawable != null) {
      mDrawable.setCallback(null);
    }

    mDrawable = drawable;

    if (drawable != null) {
      drawable.setCallback(this);
    }

    mDisplayListContainer = displayListContainer;
    invalidateSelf();
  }

  public void suppressInvalidations(boolean suppress) {
    mIgnoreInvalidations = suppress;
  }

  public void release() {
    setCallback(null);
    mIgnoreInvalidations = false;
    // Do not release DisplayListContainer yet, just dereference the object,
    // as it might be still referenced by LayoutOutput.
    mDisplayListContainer = null;
    mDrawable.setCallback(null);
    mDrawable = null;
  }

  /**
   * Draw original drawable to {@link DisplayList}'s canvas.
   */
  @UiThread
  private void drawContentIntoDisplayList() {
    final DisplayList displayList = mDisplayListContainer.getDisplayList();
    if (displayList == null) {
      return;
    }

    try {
      final Rect bounds = mDrawable.getBounds();
      final Canvas displayListCanvas = displayList.start(bounds.width(), bounds.height());

      displayListCanvas.translate(-bounds.left, -bounds.top);
      mDrawable.draw(displayListCanvas);
      displayListCanvas.translate(bounds.left, bounds.top);

      displayList.end(displayListCanvas);
      displayList.setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
    } catch (DisplayListException e) {
      // Nothing to do.
    }
  }
}
