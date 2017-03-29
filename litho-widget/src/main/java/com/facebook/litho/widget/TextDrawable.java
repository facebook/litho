/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import javax.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.facebook.litho.TextContent;
import com.facebook.litho.Touchable;
import com.facebook.fbui.textlayoutbuilder.util.LayoutMeasureUtil;

import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_UP;

/**
 * A {@link Drawable} for mounting text content from a
 * {@link Component}.
 *
 * @see Component
 * @see TextSpec
 */
public class TextDrawable extends Drawable implements Touchable, TextContent {

  private static final float DEFAULT_TOUCH_RADIUS_IN_SP = 18f;

  private Layout mLayout;
  private float mLayoutTranslationY;
  private boolean mShouldHandleTouch;
  private CharSequence mText;
  private ColorStateList mColorStateList;
  private int mUserColor;
  private int mHighlightColor;
  private ClickableSpan[] mClickableSpans;

  private int mSelectionStart;
  private int mSelectionEnd;
  private Path mSelectionPath;
  private Path mTouchAreaPath;
  private boolean mSelectionPathNeedsUpdate;
  private Paint mHighlightPaint;

  @Override
  public void draw(Canvas canvas) {
    if (mLayout == null) {
      return;
    }

    final Rect bounds = getBounds();

    canvas.translate(bounds.left, bounds.top + mLayoutTranslationY);
    mLayout.draw(canvas, getSelectionPath(), mHighlightPaint, 0);
    canvas.translate(-bounds.left, -bounds.top - mLayoutTranslationY);
  }

  @Override
  public boolean isStateful() {
    return mColorStateList != null;
  }

  @Override
  protected boolean onStateChange(int[] states) {
    if (mColorStateList != null && mLayout != null) {
      final int previousColor = mLayout.getPaint().getColor();
      final int currentColor = mColorStateList.getColorForState(states, mUserColor);

      if (currentColor != previousColor) {
        mLayout.getPaint().setColor(currentColor);
        invalidateSelf();
      }
    }

    return super.onStateChange(states);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event, View view) {
    final int action = event.getActionMasked();
    if (action == ACTION_CANCEL) {
      clearSelection();
      return false;
    }

    final Rect bounds = getBounds();
    final int x = (int) event.getX() - bounds.left;
    final int y = (int) event.getY() - bounds.top;

    float touchRadius = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        DEFAULT_TOUCH_RADIUS_IN_SP,
        view.getResources().getDisplayMetrics());

    ClickableSpan clickedSpan = getClickableSpanInCoords(x, y);

    if (clickedSpan == null) {
      clickedSpan = getClickableSpanInProximityToClick(x, y, touchRadius);
    }

    if (clickedSpan != null) {
      if (action == ACTION_UP) {
        clearSelection();
        clickedSpan.onClick(view);
      } else if (action == ACTION_DOWN) {
        setSelection(clickedSpan);
      }

      return true;
    }

    clearSelection();
    return false;
  }

  @Override
  public boolean shouldHandleTouchEvent(MotionEvent event) {
    final int action = event.getActionMasked();

    boolean isWithinBounds = getBounds().contains((int) event.getX(), (int) event.getY());
    boolean isUpOrDown = action == ACTION_UP || action == ACTION_DOWN;
    return (mShouldHandleTouch && isWithinBounds && isUpOrDown) || action == ACTION_CANCEL;
  }

  public void mount(
      CharSequence text,
      Layout layout,
      int userColor,
      ClickableSpan[] clickableSpans) {
    mount(text, layout, 0, null, userColor, 0, clickableSpans);
  }

  public void mount(CharSequence text, Layout layout, int userColor, int highlightColor) {
    mount(text, layout, 0, null, userColor, highlightColor, null);
  }

  public void mount(
      CharSequence text,
      Layout layout,
      float layoutTranslationY,
      ColorStateList colorStateList,
      int userColor,
      int highlightColor,
      ClickableSpan[] clickableSpans) {
    mLayout = layout;
    mLayoutTranslationY = layoutTranslationY;
    mText = text;
    mClickableSpans = clickableSpans;
    mShouldHandleTouch = (clickableSpans != null && clickableSpans.length > 0);
    mHighlightColor = highlightColor;
    if (userColor != 0) {
      mColorStateList = null;
      mUserColor = userColor;
    } else {
      mColorStateList = colorStateList != null ? colorStateList : TextSpec.textColorStateList;
      mUserColor = mColorStateList.getDefaultColor();
      if (mLayout != null) {
        mLayout.getPaint().setColor(mColorStateList.getColorForState(getState(), mUserColor));
      }
    }

    invalidateSelf();
  }

  public void unmount() {
    mLayout = null;
    mLayoutTranslationY = 0;
    mText = null;
    mClickableSpans = null;
    mShouldHandleTouch = false;
    mHighlightColor = 0;
    mColorStateList = null;
    mUserColor = 0;
  }

  public ClickableSpan[] getClickableSpans() {
    return mClickableSpans;
  }

  @Override
  public void setAlpha(int alpha) {
  }

  @Override
  public void setColorFilter(ColorFilter cf) {
  }

  @Override
  public int getOpacity() {
    return 0;
  }

  public CharSequence getText() {
    return mText;
  }

  public int getColor() {
    return mLayout.getPaint().getColor();
  }

  @Override
  public List<CharSequence> getTextItems() {
    return mText != null ? Collections.singletonList(mText) : Collections.<CharSequence>emptyList();
  }

  /**
   * Get the clickable span that is at the exact coordinates
   * @param x x-position of the click
   * @param y y-position of the click
   * @return a clickable span that's located where the click occurred,
   *   or: {@code null} if no clickable span was located there
   */
  @Nullable
  private ClickableSpan getClickableSpanInCoords(int x, int y) {
    final int line = mLayout.getLineForVertical(y);
    float start = mLayout.getPrimaryHorizontal(mLayout.getLineStart(line));
    float end = mLayout.getPrimaryHorizontal(mLayout.getLineVisibleEnd(line));
    if (start > end) {
      // In RTL scenario
      float temp = start;
      start = end;
      end = temp;
    }

    if (x >= start && x <= end) {
      final int offset = mLayout.getOffsetForHorizontal(line, x);

      final ClickableSpan[] clickableSpans = ((Spanned) mText).getSpans(
          offset,
          offset,
          ClickableSpan.class);

      if (clickableSpans != null && clickableSpans.length > 0) {
        return clickableSpans[0];
      }
    }

    return null;
  }

  /**
   * Get the clickable span that's close to where the view was clicked.
   * @param x x-position of the click
   * @param y y-position of the click
   * @return a clickable span that's close the click position,
   *   or: {@code null} if no clickable span was close to the click,
   *   or if a link was directly clicked or if more than one clickable
   *   span was in proximity to the click.
   */
  @Nullable
  private ClickableSpan getClickableSpanInProximityToClick(
      float x,
      float y,
      float tapRadius) {
    final Region touchAreaRegion= new Region();
    final Region clipBoundsRegion = new Region();

    if (mTouchAreaPath == null) {
      mTouchAreaPath = new Path();
    }

    clipBoundsRegion.set(
        0,
        0,
        LayoutMeasureUtil.getWidth(mLayout),
        LayoutMeasureUtil.getHeight(mLayout));
    mTouchAreaPath.reset();
    mTouchAreaPath.addCircle(x, y, tapRadius, Path.Direction.CW);
    touchAreaRegion.setPath(mTouchAreaPath, clipBoundsRegion);

    ClickableSpan result = null;
    for (ClickableSpan span : mClickableSpans) {
      if (!isClickCloseToSpan(span, (Spanned) mText, mLayout, touchAreaRegion, clipBoundsRegion)) {
        continue;
      }

      if (result != null) {
        // This is the second span that's close to the tap, so we don't have a definitive answer
        return null;
      }

      result = span;
    }

    return result;
  }

