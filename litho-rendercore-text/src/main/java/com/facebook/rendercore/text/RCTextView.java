/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.rendercore.text;

import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_UP;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.Nullable;

/** A pared-down TextView that only displays text. */
public class RCTextView extends View {

  private CharSequence mText;
  private Layout mLayout;
  private float mLayoutTranslationY;
  private boolean mClipToBounds;
  private ColorStateList mColorStateList;
  private int mLinkColor;
  private int mHighlightColor;
  private ImageSpan[] mImageSpans;

  private int mSelectionStart;
  private int mSelectionEnd;
  private Path mSelectionPath;
  private boolean mSelectionPathNeedsUpdate;
  private Paint mHighlightPaint;

  public RCTextView(Context context) {
    super(context);

    if (getImportantForAccessibility() == IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
      setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);

    if (mLayout == null) {
      return;
    }

    int saveCount = 0;
    boolean shouldRestore = false;
    if (mLayoutTranslationY != 0 || getPaddingTop() != 0 || getPaddingLeft() != 0) {
      saveCount = canvas.save();
      canvas.translate(0, mLayoutTranslationY);
      canvas.translate(getPaddingLeft(), getPaddingTop());
      shouldRestore = true;
    }

    mLayout.draw(canvas, getSelectionPath(), mHighlightPaint, 0);

    if (shouldRestore) {
      canvas.restoreToCount(saveCount);
    }
  }

  public void mount(
      CharSequence text,
      Layout layout,
      float layoutTranslationY,
      boolean clipToBounds,
      ColorStateList colorStateList,
      int linkColor,
      int highlightColor,
      ImageSpan[] imageSpans,
      int highlightStartOffset,
      int highlightEndOffset) {
    mText = text;
    mLayout = layout;
    mLayoutTranslationY = layoutTranslationY;
    mClipToBounds = clipToBounds;
    mHighlightColor = highlightColor;
    if (linkColor != 0) {
      mColorStateList = null;
      mLinkColor = linkColor;
    } else {
      mColorStateList = colorStateList;
      mLinkColor = mColorStateList.getDefaultColor();
      if (mLayout != null) {
        mLayout
            .getPaint()
            .setColor(mColorStateList.getColorForState(getDrawableState(), mLinkColor));
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

    invalidate();
  }

  private static boolean highlightOffsetsValid(
      CharSequence text, int highlightStart, int highlightEnd) {
    return highlightStart >= 0 && highlightEnd <= text.length() && highlightStart < highlightEnd;
  }

  public void unmount() {
    mText = null;
    mLayout = null;
    mLayoutTranslationY = 0;
    mHighlightColor = 0;
    mColorStateList = null;
    mLinkColor = 0;
    if (mImageSpans != null) {
      for (int i = 0, size = mImageSpans.length; i < size; i++) {
        Drawable drawable = mImageSpans[i].getDrawable();
        drawable.setCallback(null);
        drawable.setVisible(false, false);
      }
      mImageSpans = null;
    }
  }

  public CharSequence getText() {
    return mText;
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

  /** Updates selection to [selectionStart, selectionEnd] range. */
  private void setSelection(int selectionStart, int selectionEnd) {
    if (Color.alpha(mHighlightColor) == 0
        || (mSelectionStart == selectionStart && mSelectionEnd == selectionEnd)) {
      return;
    }

    mSelectionStart = selectionStart;
    mSelectionEnd = selectionEnd;

    if (mHighlightPaint == null) {
      mHighlightPaint = new Paint();
    }
    mHighlightPaint.setColor(mHighlightColor);

    mSelectionPathNeedsUpdate = true;
    invalidate();
  }

  private void clearSelection() {
    setSelection(0, 0);
  }

  @Override
  public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
    super.onPopulateAccessibilityEvent(event);
    if (!TextUtils.isEmpty(mText)) {
      event.getText().add(getTextForAccessibility());
    }
  }

  @Override
  public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
    super.onInitializeAccessibilityNodeInfo(info);

    final CharSequence textForAccessibility = getTextForAccessibility();
    if (!TextUtils.isEmpty(textForAccessibility)) {
      info.setText(textForAccessibility);

      info.addAction(AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY);
      info.addAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY);
      info.setMovementGranularities(
          AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER
              | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD
              | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE
              | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH
              | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
      info.addAction(AccessibilityNodeInfo.ACTION_SET_SELECTION);
    }
  }

  // See TextView#getTextForAccessibility()
  private static final int SAFE_PARCELABLE_SIZE = 1000000;

  private CharSequence getTextForAccessibility() {
    if (mText == null || mText.length() < SAFE_PARCELABLE_SIZE) {
      return mText;
    }

    if (Character.isHighSurrogate(mText.charAt(SAFE_PARCELABLE_SIZE - 1))
        && Character.isLowSurrogate(mText.charAt(SAFE_PARCELABLE_SIZE))) {
      return mText.subSequence(0, SAFE_PARCELABLE_SIZE - 1);
    }
    return mText.subSequence(0, SAFE_PARCELABLE_SIZE);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    final int action = event.getActionMasked();
    if (action == ACTION_CANCEL) {
      clearSelection();
      return false;
    }

    final int x = (int) event.getX();
    final int y = (int) event.getY();

    ClickableSpan clickedSpan = getClickableSpanInCoords(x, y);

    if (clickedSpan == null) {
      clearSelection();
      return super.onTouchEvent(event);
    }

    if (action == ACTION_UP) {
      clearSelection();
      clickedSpan.onClick(this);
    } else if (action == ACTION_DOWN) {
      setSelection(clickedSpan);
    }

    return true;
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
    if (!(mText instanceof Spanned) || offset < 0) {
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
       * <p>To determine the actual bounds of the line, we need the line's direction, leading
       * margin, and extent, but only the first is available directly. The margin is given by either
       * {@link Layout#getParagraphLeft} or {@link Layout#getParagraphRight} depending on line
       * direction, and {@link Layout#getLineMax} gives the extent *plus* the leading margin, so we
       * can figure out the rest from there.
       */
      final boolean rtl = mLayout.getParagraphDirection(line) == Layout.DIR_RIGHT_TO_LEFT;
      left = rtl ? mLayout.getWidth() - mLayout.getLineMax(line) : mLayout.getParagraphLeft(line);
      right = rtl ? mLayout.getParagraphRight(line) : mLayout.getLineMax(line);
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
}
