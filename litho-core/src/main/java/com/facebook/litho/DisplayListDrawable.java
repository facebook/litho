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
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.facebook.litho.displaylist.DisplayList;
import com.facebook.litho.displaylist.DisplayListException;

/**
 * Drawable which supports {@link DisplayList} for drawing and delegates all other calls to it's
 * wrapped {@link android.graphics.drawable.Drawable}.
 */
class DisplayListDrawable extends Drawable implements Drawable.Callback, Touchable {

  private static final boolean DEBUG = false;
  private static final String LOG_TAG = "DisplayListDrawable";
  private static Paint sDebugBorderPaint;

  private Drawable mDrawable;
  private boolean mTouchable;
  private @Nullable String mName;
  private @Nullable DisplayList mDisplayList;
  private boolean mIgnoreInvalidations;
  private boolean mInvalidated; // If the DL is up-to-date
  private boolean mDoNotAttemptDLDrawing = false;

  DisplayListDrawable(Drawable drawable) {
    setWrappedDrawable(drawable);
  }

  @Override
  public void draw(Canvas canvas) {
    if (mDrawable == null) {
      throw new IllegalStateException("The wrapped drawable hasn't been set yet");
    }

    logDebug("Drawing", false);

    if (mDoNotAttemptDLDrawing) {
      // We tried before, it didn't go well, we are not doing it again
      logDebug("\t> origin (not attempting DL drawing)", false);

      mDrawable.draw(canvas);
      return;
    }

    if (mDisplayList == null) {
      final DisplayList displayList = DisplayList.createDisplayList(mName);

      if (displayList == null) {
        // DL was not created, just draw the drawable itself and return
        logDebug("\t> origin (DL wasn't created)", false);

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
        logDebug("\t> origin (DL isn't valid)", false);

        mDrawable.draw(canvas);
        return;
      }
      // At this point we have a valid DL, that we can draw
      logDebug("\t> DL", false);

      mDisplayList.draw(canvas);
    } catch (DisplayListException e) {
      // Let's make sure next draw calls will just bail the DisplayList part.
      logDebug("\t> origin (exception)\n\t" + e, false);

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
    invalidateDL();
  }

  /** Draw original drawable to {@link DisplayList}'s canvas. */
  @UiThread
  private void drawContentIntoDisplayList() throws DisplayListException {
    logDebug("Drawing content into DL", false);

    final Rect bounds = mDrawable.getBounds();
    final Canvas displayListCanvas = mDisplayList.start(bounds.width(), bounds.height());

    displayListCanvas.translate(-bounds.left, -bounds.top);
    mDrawable.draw(displayListCanvas);
    displayListCanvas.translate(bounds.left, bounds.top);

    drawDebugBorder(bounds, displayListCanvas);

    mDisplayList.end(displayListCanvas);
    mDisplayList.setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
    // Also need to clean up translation we had set to the DL in order to adjust positioning without
    // changing bounds, as now drawable bounds and DL bounds are same
    mDisplayList.setTranslationX(0);
    mDisplayList.setTranslationY(0);
  }

  @Override
  @UiThread
  protected void onBoundsChange(Rect bounds) {
    logDebug("On bounds change, bounds=" + bounds, false);

    // We want to pass bounds to the underlying Drawable, but it's gonna call invalidateSelf() in
    // setBounds(), we don't really need that callback, but what makes it even worse is the fact
    // that it invalidates *before* it applies the new bounds, however, when we receive the
    // invalidation callback we sync the bounds, thus applying the old bounds back here. To avoid
    // all this we are removing callback before setting bounds, and setting it right back after
    mDrawable.setCallback(null);
    mDrawable.setBounds(bounds);
    // TODO(t22432769): Remove this after D5965597 lands
    if (mDrawable instanceof MatrixDrawable) {
      ((MatrixDrawable) mDrawable).bind(bounds.width(), bounds.height());
    }
    mDrawable.setCallback(this);

    if (mDisplayList == null || !mDisplayList.isValid() || mInvalidated) {
      logDebug("\t> no valid DL", false);
      return;
    }

    final Rect dlBounds = mDisplayList.getBounds();
    if (bounds.height() == dlBounds.height() && bounds.width() == dlBounds.width()) {
      try {
        final int dx = bounds.left - dlBounds.left;
        final int dy = bounds.top - dlBounds.top;

        logDebug("\t> size didn't change, translating, dx=" + dx + ", dy=" + dy, false);

        mDisplayList.setTranslationX(dx);
        mDisplayList.setTranslationY(dy);
      } catch (DisplayListException e) {
        // We'll re-create DL in draw()
        logDebug("\t> couldn't translate\n\t" + e, false);

        mDisplayList = null;
      }
    } else {
      // We'll need to re-draw into DL
      logDebug("\t> size changed, invalidating", false);

      invalidateDL();
    }
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
    logDebug("Invalidating drawable", true);

    invalidateSelf();
    if (mIgnoreInvalidations) {
      return;
    }

    // We need to make sure at this point that the bounds of the  {@link DisplayListDrawable} are
    // equals to the bounds of its content to make sure that the view invalidation works as
    // expected.
    setBounds(mDrawable.getBounds());
    invalidateDL(); // DL needs to be re-created
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

    mTouchable = mDrawable instanceof Touchable;

    // DL needs to be re-created
    invalidateDL();

    // Notify callback about invalidation
    invalidateSelf();
  }

  public void suppressInvalidations(boolean suppress) {
    mIgnoreInvalidations = suppress;
  }

  private void invalidateDL() {
    logDebug("invalidateDL", true);
    mInvalidated = true;
  }

  @Override
  public boolean shouldHandleTouchEvent(MotionEvent event) {
    return (mDrawable instanceof Touchable)
        && ((Touchable) mDrawable).shouldHandleTouchEvent(event);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event, View host) {
    return mTouchable && ((Touchable) mDrawable).onTouchEvent(event, host);
  }

  public void release() {
    setCallback(null);
    mIgnoreInvalidations = false;
    mInvalidated = false;
    mDoNotAttemptDLDrawing = false;
    mDrawable.setCallback(null);
    mDrawable = null;
    mTouchable = false;
    mName = null;
    mDisplayList = null;
  }

  @Override
  public String toString() {
    return "DisplayListDrawable("
        + hashCode()
        + "){"
        + "\n\tbounds="
        + getBounds()
        + "\n\torigin="
        + mDrawable
        + " bounds="
        + mDrawable.getBounds()
        + "\n\tDL="
        + mDisplayList
        + "\n\tinvalidated="
        + mInvalidated
        + "\n\tskipping DL="
        + mDoNotAttemptDLDrawing
        + '}';
  }

  private static void logDebug(String message, boolean withStackTrace) {
    if (DEBUG) {
      Log.d(LOG_TAG, message, withStackTrace ? new RuntimeException() : null);
    }
  }

  private static void drawDebugBorder(Rect bounds, Canvas canvas) {
    if (!DEBUG) {
      return;
    }

    if (sDebugBorderPaint == null) {
      sDebugBorderPaint = new Paint();
      sDebugBorderPaint.setColor(Color.GREEN);
      sDebugBorderPaint.setStrokeWidth(2);
    }

    final float w = bounds.width();
    final float h = bounds.height();
    final float[] points = {
      0, 0, w, 0, // top line
      w, 0, w, h, // right line
      w, h, 0, h, // bottom line
      0, h, 0, 0, // left line
    };

    canvas.drawLines(points, sDebugBorderPaint);
  }
}
