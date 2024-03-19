/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.widget;

import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.facebook.fbui.textlayoutbuilder.util.LayoutMeasureUtil;
import com.facebook.litho.TextContent;
import com.facebook.litho.Touchable;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A {@link Drawable} for mounting text content from a {@link Component}.
 *
 * @see Component
 * @see TextSpec
 */
public class TextDrawable extends Drawable implements Touchable, TextContent, Drawable.Callback {

  private @Nullable Layout mLayout;
  private float mLayoutTranslationX;
  private float mLayoutTranslationY;
  private boolean mClipToBounds;
  private boolean mShouldHandleTouch;
  private CharSequence mText;
  private ColorStateList mColorStateList;
  private int mUserColor;
  private int mHighlightColor;
  private float mOutlineWidth;
  private int mOutlineColor;
  private ClickableSpan[] mClickableSpans;
  private ImageSpan[] mImageSpans;

  private int mSelectionStart;
  private int mSelectionEnd;
  private Path mSelectionPath;
  private Path mTouchAreaPath;
  private boolean mSelectionPathNeedsUpdate;
  private Paint mHighlightPaint;
  private TextOffsetOnTouchListener mTextOffsetOnTouchListener;
  private float mClickableSpanExpandedOffset;
  private boolean mLongClickActivated;
  private @Nullable Handler mLongClickHandler;
  private @Nullable LongClickRunnable mLongClickRunnable;
  private @Nullable ClickableSpanListener mSpanListener;
  private @Nullable TouchableSpanListener mTouchableSpanListener;
  private @Nullable String mContextLogTag;

  /**
   * This should be lazy loaded so that it is only created whenever it is needed. In most cases we
   * won't need this data, so we can avoid this extra instance creation.
   */
  private @Nullable TextContent.Item mTextContentItem;

  @Override
  public void draw(Canvas canvas) {
    if (mLayout == null) {
      return;
    }

    final int saveCount = canvas.save();

    final Rect bounds = getBounds();
    if (mClipToBounds) {
      canvas.clipRect(bounds);
    }
    canvas.translate(bounds.left + mLayoutTranslationX, bounds.top + mLayoutTranslationY);
    try {
      maybeDrawOutline(canvas);
      mLayout.draw(canvas, getSelectionPath(), mHighlightPaint, 0);
    } catch (IndexOutOfBoundsException e) {
      RuntimeException withDebugInfo =
          new RuntimeException("Debug info for IOOB: " + getDebugInfo(), e);
      withDebugInfo.setStackTrace(new StackTraceElement[] {});
      throw withDebugInfo;
    }

    canvas.restoreToCount(saveCount);
  }

  /**
   * All texts drawn on top of images and videos need contrast outlines and shadows to be more
   * visible against busy backgrounds. Standard Android shadows do not produce the separation of
   * intensity needed, so the Litho library Text Component provides a special outline attribute that
   * draws contrast outlines usually combined with shadows. These outlines are drawn outside the
   * contours to avoid reducing the visible surface of character glyphs. However, since Android has
   * no mode for drawing outside strokes, they need to be drawn twice: the first pass draws strokes,
   * and the second pass draws inner filled shapes. This method performs the first outlining pass if
   * needed.
   *
   * @param canvas - A canvas to draw on.
   */
  private void maybeDrawOutline(Canvas canvas) {
    if (mOutlineWidth > 0f) {
      Paint p = mLayout.getPaint();
      int savedColor = p.getColor();
      Paint.Style savedStyle = p.getStyle();
      float savedStrokeWidth = p.getStrokeWidth();
      Paint.Join savedJoin = p.getStrokeJoin();
      p.setStrokeJoin(Paint.Join.ROUND);
      p.setColor(mOutlineColor != 0 ? mOutlineColor : p.getShadowLayerColor());
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(mOutlineWidth);
      mLayout.draw(canvas);
      p.setStrokeWidth(savedStrokeWidth);
      p.setStyle(savedStyle);
      p.setColor(savedColor);
      p.setStrokeJoin(savedJoin);
    }
  }

  private String getDebugInfo() {
    StringBuilder debugInfo = new StringBuilder();
    debugInfo.append(" [");
    debugInfo.append(mContextLogTag);
    debugInfo.append("] ");
    if (mText instanceof SpannableStringBuilder) {
      Object[] spans = ((SpannableStringBuilder) mText).getSpans(0, mText.length(), Object.class);
      debugInfo.append("spans: ");
      for (Object span : spans) {
        debugInfo.append(span.getClass().getSimpleName());
        debugInfo.append(", ");
      }
    }
    debugInfo.append("ellipsizedWidth: ");
    debugInfo.append(mLayout.getEllipsizedWidth());
    debugInfo.append(", lineCount: ");
    debugInfo.append(mLayout.getLineCount());
    return debugInfo.toString();
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
    if ((shouldHandleTouchForClickableSpan(event) || shouldHandleTouchForLongClickableSpan(event))
        && handleTouchForSpans(event, view)) {
      return true;
    }

    if (shouldHandleTextOffsetOnTouch(event)) {
      handleTextOffsetChange(event);
      // We will not consume touch events at this point because TextOffsetOnTouch event has
      // lower priority than click/longclick events.
    }

    return false;
  }

  private boolean handleTouchForSpans(MotionEvent event, View view) {
    final int action = event.getActionMasked();
    if (action == ACTION_CANCEL) {
      clearSelection();
      resetLongClick();
      if (mTouchableSpanListener != null) {
        mTouchableSpanListener.onTouch(null, event, view);
      }
      return false;
    }

    if (action == ACTION_MOVE && !mLongClickActivated && mLongClickRunnable != null) {
      trackLongClickBoundaryOnMove(event);
    }

    final boolean clickActivationAllowed = !mLongClickActivated;
    if (action == ACTION_UP) {
      resetLongClick();
    }

    final Rect bounds = getBounds();
    if (!isWithinBounds(bounds, event)) {
      return false;
    }

    final int x = (int) event.getX() - bounds.left;
    final int y = (int) event.getY() - bounds.top;

    ClickableSpan clickedSpan = getClickableSpanInCoords(x, y);

    if (clickedSpan == null && mClickableSpanExpandedOffset > 0) {
      clickedSpan = getClickableSpanInProximityToClick(x, y, mClickableSpanExpandedOffset);
    }

    if (clickedSpan == null) {
      clearSelection();
      return false;
    }

    if (action == ACTION_UP) {
      clearSelection();
      if (clickActivationAllowed
          && (mSpanListener == null || !mSpanListener.onClick(clickedSpan, view))) {
        clickedSpan.onClick(view);
      }
    } else if (action == ACTION_DOWN) {
      if (clickedSpan instanceof LongClickableSpan) {
        registerForLongClick((LongClickableSpan) clickedSpan, view);
      }
      setSelection(clickedSpan);
    }
    if (mTouchableSpanListener != null) {
      mTouchableSpanListener.onTouch(clickedSpan, event, view);
    }
    return true;
  }

  private void resetLongClick() {
    if (mLongClickHandler != null) {
      mLongClickHandler.removeCallbacks(mLongClickRunnable);
      mLongClickRunnable = null;
    }
    mLongClickActivated = false;
  }

  private void registerForLongClick(LongClickableSpan longClickableSpan, View view) {
    mLongClickRunnable = new LongClickRunnable(longClickableSpan, view);
    mLongClickHandler.postDelayed(mLongClickRunnable, ViewConfiguration.getLongPressTimeout());
  }

  private void handleTextOffsetChange(MotionEvent event) {
    final Rect bounds = getBounds();
    final int x = (int) event.getX() - bounds.left;
    final int y = (int) event.getY() - bounds.top;

    final int offset = getTextOffsetAt(x, y);
    if (offset >= 0 && offset <= mText.length()) {
      mTextOffsetOnTouchListener.textOffsetOnTouch(offset);
    }
  }

  @Override
  public boolean shouldHandleTouchEvent(MotionEvent event) {
    return shouldHandleTouchForClickableSpan(event)
        || shouldHandleTouchForLongClickableSpan(event)
        || shouldHandleTextOffsetOnTouch(event);
  }

  private boolean shouldHandleTouchForClickableSpan(MotionEvent event) {
    final int action = event.getActionMasked();
    final boolean isUpOrDown = action == ACTION_UP || action == ACTION_DOWN;
    return (mShouldHandleTouch && isWithinBounds(getBounds(), event) && isUpOrDown)
        || action == ACTION_CANCEL;
  }

  private boolean shouldHandleTouchForLongClickableSpan(MotionEvent event) {
    return mShouldHandleTouch && mLongClickHandler != null && event.getAction() != ACTION_DOWN;
  }

  private static boolean isWithinBounds(Rect bounds, MotionEvent event) {
    return bounds.contains((int) event.getX(), (int) event.getY());
  }

  private void trackLongClickBoundaryOnMove(MotionEvent event) {
    final Rect bounds = getBounds();
    if (!isWithinBounds(bounds, event)) {
      resetLongClick();
      return;
    }

    final ClickableSpan clickableSpan =
        getClickableSpanInCoords((int) event.getX() - bounds.left, (int) event.getY() - bounds.top);
    if (mLongClickRunnable.longClickableSpan != clickableSpan) {
      // we are out of span area, reset longpress
      resetLongClick();
    }
  }

  private boolean shouldHandleTextOffsetOnTouch(MotionEvent event) {
    return mTextOffsetOnTouchListener != null
        && event.getActionMasked() == ACTION_DOWN
        && getBounds().contains((int) event.getX(), (int) event.getY());
  }

  public void mount(
      CharSequence text, Layout layout, int userColor, ClickableSpan[] clickableSpans) {
    mount(
        text,
        layout,
        0,
        0,
        false,
        null,
        userColor,
        0,
        0f,
        0,
        clickableSpans,
        null,
        null,
        null,
        null,
        -1,
        -1,
        0f,
        null);
  }

  public void mount(CharSequence text, Layout layout, int userColor, int highlightColor) {
    mount(
        text,
        layout,
        0,
        0,
        false,
        null,
        userColor,
        highlightColor,
        0f,
        0,
        null,
        null,
        null,
        null,
        null,
        -1,
        -1,
        0f,
        null);
  }

  public void mount(
      CharSequence text,
      Layout layout,
      float layoutTranslationY,
      ColorStateList colorStateList,
      int userColor,
      int highlightColor,
      ClickableSpan[] clickableSpans) {
    mount(
        text,
        layout,
        0,
        0,
        false,
        null,
        userColor,
        highlightColor,
        0f,
        0,
        clickableSpans,
        null,
        null,
        null,
        null,
        -1,
        -1,
        0f,
        null);
  }

  public void mount(
      CharSequence text,
      Layout layout,
      float layoutTranslationX,
      float layoutTranslationY,
      boolean clipToBounds,
      ColorStateList colorStateList,
      int userColor,
      int highlightColor,
      float outlineWidth,
      int outlineColor,
      @Nullable ClickableSpan[] clickableSpans,
      @Nullable ImageSpan[] imageSpans,
      @Nullable ClickableSpanListener spanListener,
      @Nullable TouchableSpanListener touchableSpanListener,
      @Nullable TextOffsetOnTouchListener textOffsetOnTouchListener,
      int highlightStartOffset,
      int highlightEndOffset,
      float clickableSpanExpandedOffset,
      String contextLogTag) {
    mLayout = layout;
    mLayoutTranslationX = layoutTranslationX;
    mLayoutTranslationY = layoutTranslationY;
    mClipToBounds = clipToBounds;
    mText = text;
    mClickableSpans = clickableSpans;
    mTouchableSpanListener = touchableSpanListener;
    if (mLongClickHandler == null && containsLongClickableSpan(clickableSpans)) {
      mLongClickHandler = new Handler();
    }
    mSpanListener = spanListener;
    mTextOffsetOnTouchListener = textOffsetOnTouchListener;
    mShouldHandleTouch = (clickableSpans != null && clickableSpans.length > 0);
    mHighlightColor = highlightColor;
    setOutline(outlineWidth, outlineColor);
    mClickableSpanExpandedOffset = clickableSpanExpandedOffset;
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

    if (highlightOffsetsValid(text, highlightStartOffset, highlightEndOffset)) {
      setSelection(highlightStartOffset, highlightEndOffset);
    } else {
      clearSelection();
    }

    if (imageSpans != null) {
      for (int i = 0, size = imageSpans.length; i < size; i++) {
        Drawable drawable = imageSpans[i].getDrawable();
        drawable.setCallback(this);
        drawable.setVisible(true, false);
      }
    }
    mImageSpans = imageSpans;
    mContextLogTag = contextLogTag;

    invalidateSelf();
  }

  public void setTextColor(@ColorInt int textColor) {
    mUserColor = textColor;
    if (mLayout != null) {
      mLayout.getPaint().setColor(textColor);
    }
    invalidateSelf();
  }

  public void setOutline(float outlineWidth, @ColorInt int outlineColor) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      mOutlineWidth = outlineWidth;
      mOutlineColor = outlineColor;
      invalidateSelf();
    }
  }

  private static boolean containsLongClickableSpan(@Nullable ClickableSpan[] clickableSpans) {
    if (clickableSpans == null) {
      return false;
    }

    for (ClickableSpan span : clickableSpans) {
      if (span instanceof LongClickableSpan) {
        return true;
      }
    }

    return false;
  }

  private boolean highlightOffsetsValid(CharSequence text, int highlightStart, int highlightEnd) {
    return highlightStart >= 0 && highlightEnd <= text.length() && highlightStart < highlightEnd;
  }

  public void unmount() {
    mTextContentItem = null;
    mLayout = null;
    mLayoutTranslationY = 0;
    mText = null;
    mClickableSpans = null;
    mShouldHandleTouch = false;
    mHighlightColor = 0;
    mSpanListener = null;
    mTextOffsetOnTouchListener = null;
    mColorStateList = null;
    mUserColor = 0;
    if (mImageSpans != null) {
      for (int i = 0, size = mImageSpans.length; i < size; i++) {
        Drawable drawable = mImageSpans[i].getDrawable();
        drawable.setCallback(null);
        drawable.setVisible(false, false);
      }
      mImageSpans = null;
    }
  }

  public ClickableSpan[] getClickableSpans() {
    return mClickableSpans;
  }

  @Override
  public void setAlpha(int alpha) {}

  @Override
  public void setColorFilter(ColorFilter cf) {}

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

  public float getTextSize() {
    return mLayout.getPaint().getTextSize();
  }

  public Layout getLayout() {
    return mLayout;
  }

  /**
   * Get the clickable span that is at the exact coordinates
   *
   * @param x x-position of the click
   * @param y y-position of the click
   * @return a clickable span that's located where the click occurred, or: {@code null} if no
   *     clickable span was located there
   */
  @Nullable
  private ClickableSpan getClickableSpanInCoords(int x, int y) {
    final int offset = getTextOffsetAt(x, y);
    if (offset < 0) {
      return null;
    }
    final ClickableSpan[] clickableSpans =
        ((Spanned) mText).getSpans(offset, offset, ClickableSpan.class);

    if (clickableSpans != null && clickableSpans.length > 0) {
      return clickableSpans[0];
    }

    return null;
  }

  private int getTextOffsetAt(int x, int y) {
    final int line = mLayout.getLineForVertical(y);

    final float left;
    final float right;

    if (mLayout.getAlignment() == Layout.Alignment.ALIGN_CENTER) {
      /**
       * {@link Layout#getLineLeft} and {@link Layout#getLineRight} properly account for paragraph
       * margins on centered text.
       */
      left = mLayout.getLineLeft(line);
      right = mLayout.getLineRight(line);
    } else {
      /**
       * {@link Layout#getLineLeft} and {@link Layout#getLineRight} do NOT properly account for
       * paragraph margins on non-centered text, so we need an alternative.
       *
       * <p>To determine the actual bounds of the line, we need the line's direction and alignment,
       * leading margin, and extent, but only the first is available directly. The margin is given
       * by either {@link Layout#getParagraphLeft} or {@link Layout#getParagraphRight} depending on
       * line direction, and {@link Layout#getLineMax} gives the extent *plus* the leading margin,
       * so we can figure out the rest from there.
       */
      final int direction = mLayout.getParagraphDirection(line);
      final Layout.Alignment alignment = mLayout.getParagraphAlignment(line);
      final boolean rightAligned =
          (direction == Layout.DIR_RIGHT_TO_LEFT && alignment == Layout.Alignment.ALIGN_NORMAL)
              || (direction == Layout.DIR_LEFT_TO_RIGHT
                  && alignment == Layout.Alignment.ALIGN_OPPOSITE);
      left =
          rightAligned
              ? mLayout.getWidth() - mLayout.getLineMax(line)
              : mLayout.getParagraphLeft(line);
      right = rightAligned ? mLayout.getParagraphRight(line) : mLayout.getLineMax(line);
    }

    if (x < left || x > right) {
      return -1;
    }

    try {
      return mLayout.getOffsetForHorizontal(line, x);
    } catch (ArrayIndexOutOfBoundsException e) {
      // This happens for bidi text on Android 7-8.
      // See
      // https://android.googlesource.com/platform/frameworks/base/+/821e9bd5cc2be4b3210cb0226e40ba0f42b51aed
      return -1;
    }
  }

  @VisibleForTesting
  @Nullable
  Layout.Alignment getLayoutAlignment() {
    return mLayout == null ? null : mLayout.getAlignment();
  }

  /**
   * Get the clickable span that's close to where the view was clicked.
   *
   * @param x x-position of the click
   * @param y y-position of the click
   * @return a clickable span that's close the click position, or: {@code null} if no clickable span
   *     was close to the click, or if a link was directly clicked or if more than one clickable
   *     span was in proximity to the click.
   */
  @Nullable
  private ClickableSpan getClickableSpanInProximityToClick(float x, float y, float tapRadius) {
    final Region touchAreaRegion = new Region();
    final Region clipBoundsRegion = new Region();

    if (mTouchAreaPath == null) {
      mTouchAreaPath = new Path();
    }

    clipBoundsRegion.set(
        0, 0, LayoutMeasureUtil.getWidth(mLayout), LayoutMeasureUtil.getHeight(mLayout));
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

  private @Nullable Path getSelectionPath() {
    if (mSelectionStart == mSelectionEnd) {
      return null;
    }

    if (Color.alpha(mHighlightColor) == 0) {
      return null;
    }

    if (mSelectionPathNeedsUpdate) {
      if (mSelectionPath == null) {
        mSelectionPath = new Path();
      }

      mLayout.getSelectionPath(mSelectionStart, mSelectionEnd, mSelectionPath);
      mSelectionPathNeedsUpdate = false;
    }

    return mSelectionPath;
  }

  private void setSelection(ClickableSpan span) {
    final Spanned text = (Spanned) mText;
    setSelection(text.getSpanStart(span), text.getSpanEnd(span));
  }

  /**
   * Updates selection to [selectionStart, selectionEnd] range.
   *
   * @param selectionStart
   * @param selectionEnd
   */
  private void setSelection(int selectionStart, int selectionEnd) {
    if (Color.alpha(mHighlightColor) == 0
        || (mSelectionStart == selectionStart && mSelectionEnd == selectionEnd)) {
      return;
    }

    mSelectionStart = selectionStart;
    mSelectionEnd = selectionEnd;

    if (mHighlightPaint == null) {
      mHighlightPaint = new Paint();
      mHighlightPaint.setColor(mHighlightColor);
    } else {
      mHighlightPaint.setColor(mHighlightColor);
    }

    mSelectionPathNeedsUpdate = true;
    invalidateSelf();
  }

  private void clearSelection() {
    setSelection(0, 0);
  }

  private boolean isClickCloseToSpan(
      ClickableSpan span,
      Spanned buffer,
      Layout layout,
      Region touchAreaRegion,
      Region clipBoundsRegion) {
    final Region clickableSpanAreaRegion = new Region();
    final Path clickableSpanAreaPath = new Path();

    layout.getSelectionPath(
        buffer.getSpanStart(span), buffer.getSpanEnd(span), clickableSpanAreaPath);
    clickableSpanAreaRegion.setPath(clickableSpanAreaPath, clipBoundsRegion);

    return clickableSpanAreaRegion.op(touchAreaRegion, Region.Op.INTERSECT);
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

  @NonNull
  @Override
  public List<Item> getItems() {
    TextContent.Item item = getOrCreateTextItem();
    if (item == null) {
      return Collections.emptyList();
    } else {
      return Collections.singletonList(item);
    }
  }

  @NonNull
  @Override
  public List<CharSequence> getTextList() {
    TextContent.Item item = getOrCreateTextItem();
    if (item == null) {
      return Collections.emptyList();
    } else {
      return Collections.singletonList(item.getText());
    }
  }

  @Nullable
  private TextContent.Item getOrCreateTextItem() {
    Layout layout = mLayout;
    if (layout == null) {
      return null;
    }

    if (mTextContentItem == null) {
      /* we get a reference to the values of these properties when we need it. this potentially avoids
       * situations where the `mLayout` could be set to null after we got access to the `TextContent.Item` */
      CharSequence text = getText();
      float textSize = getTextSize();
      Typeface typeface = layout.getPaint().getTypeface();
      int color = getColor();
      float fontLineHeight =
          (layout.getPaint().getFontMetricsInt(null) * layout.getSpacingMultiplier())
              + layout.getSpacingAdd();
      int linesCount = layout.getLineCount();

      mTextContentItem =
          new TextContent.Item() {
            @NonNull
            @Override
            public CharSequence getText() {
              return text;
            }

            @Override
            public float getTextSize() {
              return textSize;
            }

            @NonNull
            @Override
            public Typeface getTypeface() {
              return typeface;
            }

            @Override
            public int getColor() {
              return color;
            }

            @Override
            public float getFontLineHeight() {
              return fontLineHeight;
            }

            @Override
            public int getLinesCount() {
              return linesCount;
            }
          };
    }

    return mTextContentItem;
  }

  interface TextOffsetOnTouchListener {

    void textOffsetOnTouch(int textOffset);
  }

  private class LongClickRunnable implements Runnable {

    private LongClickableSpan longClickableSpan;
    private View longClickableSpanView;

    LongClickRunnable(LongClickableSpan span, View view) {
      longClickableSpan = span;
      longClickableSpanView = view;
    }

    @Override
    public void run() {
      mLongClickActivated =
          (mSpanListener != null
                  && mSpanListener.onLongClick(longClickableSpan, longClickableSpanView))
              || longClickableSpan.onLongClick(longClickableSpanView);
    }
  }
}
