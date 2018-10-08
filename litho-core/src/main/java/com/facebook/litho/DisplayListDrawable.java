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
import android.support.annotation.VisibleForTesting;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.displaylist.DisplayList;
import com.facebook.litho.displaylist.DisplayListException;

/**
 * Drawable which supports {@link DisplayList} for drawing and delegates all other calls to it's
 * wrapped {@link android.graphics.drawable.Drawable}.
 */
class DisplayListDrawable extends Drawable implements Drawable.Callback {

  private Drawable mDrawable;
  private @Nullable String mName;
  private @Nullable DisplayList mDisplayList;
  private boolean mIgnoreInvalidations;
  private boolean mInvalidated; // If the DL is up-to-date
  private boolean mDoNotAttemptDLDrawing = false;

  DisplayListDrawable(Drawable drawable) {
    if (ComponentsConfiguration.forceNotToCacheDisplayLists) {
      throw new RuntimeException(
          "DisplayLists are not supposed to be used, this should never be called");
    }

    setWrappedDrawable(drawable);
  }

  @Override
  public void draw(Canvas canvas) {
    if (mDrawable == null) {
      throw new IllegalStateException("The wrapped drawable hasn't been set yet");
    }

    if (mDoNotAttemptDLDrawing) {
      // We tried before, it didn't go well, we are not doing it again
      mDrawable.draw(canvas);
      return;
    }

    if (mDisplayList == null) {
      final DisplayList displayList = DisplayList.createDisplayList(mName);

      if (displayList == null) {
        // DL was not created, just draw the drawable itself and return
        mDrawable.draw(canvas);
        return;
      }

      setDisplayList(displayList);
    }
    // At this point we have a DL (non-null), but it isn't necessarily up-to-date
    try {
      if (mInvalidated || !mDisplayList.isValid()) {
        drawContentIntoDisplayList();
        mInvalidated = false;
      }

      if (!mDisplayList.isValid()) {
        // DL still isn't valid, just draw the drawable itself and return
        mDrawable.draw(canvas);
        return;
      }
      // At this point we have a valid DL, that we can draw
      mDisplayList.draw(canvas);
    } catch (DisplayListException e) {
      // Let's make sure next draw calls will just bail the DisplayList part.
      mDoNotAttemptDLDrawing = true;
      mDisplayList = null;
      mDrawable.draw(canvas);
    }
  }

  /** The only reason this method exists is so we can mock it in tests. */
  @VisibleForTesting
  void setDisplayList(DisplayList displayList) {
    mDisplayList = displayList;
    // DL needs to be re-drawn
    mInvalidated = true;
  }

  /** Draw original drawable to {@link DisplayList}'s canvas. */
  @UiThread
  private void drawContentIntoDisplayList() throws DisplayListException {
    final Rect bounds = mDrawable.getBounds();
    final Canvas displayListCanvas = mDisplayList.start(bounds.width(), bounds.height());

    displayListCanvas.translate(-bounds.left, -bounds.top);
    mDrawable.draw(displayListCanvas);
    displayListCanvas.translate(bounds.left, bounds.top);

    mDisplayList.end(displayListCanvas);
    mDisplayList.setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
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
      setWrappedDrawable(mutated);
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
    mInvalidated = true; // DL needs to be re-created
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

  /** Sets the current wrapped {@link Drawable} */
  void setWrappedDrawable(Drawable drawable) {
    if (drawable == null) {
      throw new IllegalArgumentException("The wrapped drawable must not be null");
    }

    if (mDrawable != null) {
      mDrawable.setCallback(null);
    }

    mDrawable = drawable;
    mDrawable.setCallback(this);

    // DL needs to be re-created
    mInvalidated = true;

    // Notify callback about invalidation
    invalidateSelf();
  }

  public void suppressInvalidations(boolean suppress) {
    mIgnoreInvalidations = suppress;
  }

  public void release() {
    setCallback(null);
    mIgnoreInvalidations = false;
    mInvalidated = false;
    mDoNotAttemptDLDrawing = false;
    mDrawable.setCallback(null);
    mDrawable = null;
    mName = null;
    mDisplayList = null;
  }
}
