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
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;
import com.facebook.proguard.annotations.DoNotStrip;
import java.util.List;

/** A pared-down TextView that only displays text. */
@DoNotStrip
public class RCTextView extends View {

  private static final String ACCESSIBILITY_BUTTON_CLASS = "android.widget.Button";
  private CharSequence mText;
  private ClickableSpan[] mClickableSpans;
  private Layout mLayout;
  private float mLayoutTranslationX;
  private float mLayoutTranslationY;
  private boolean mClipToBounds;
  private ColorStateList mColorStateList;
  private int mLinkColor;
  private int mHighlightColor;
  private int mHighlightCornerRadius;
  private ImageSpan[] mImageSpans;

  private int mSelectionStart;
  private int mSelectionEnd;
  private Path mSelectionPath;
  private boolean mSelectionPathNeedsUpdate;
  private Paint mHighlightPaint;
  @Nullable private final RCTextAccessibilityDelegate mRCTextAccessibilityDelegate;

  public RCTextView(Context context) {
    super(context);

    if (getImportantForAccessibility() == IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
      mRCTextAccessibilityDelegate = new RCTextAccessibilityDelegate();
      ViewCompat.setAccessibilityDelegate(this, mRCTextAccessibilityDelegate);
    } else {
      mRCTextAccessibilityDelegate = null;
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
    if (mLayoutTranslationX != 0
        || mLayoutTranslationY != 0
        || getPaddingTop() != 0
        || getPaddingLeft() != 0) {
      saveCount = canvas.save();
      canvas.translate(mLayoutTranslationX, mLayoutTranslationY);
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
      float layoutTranslationX,
      float layoutTranslationY,
      boolean clipToBounds,
      ColorStateList colorStateList,
      int linkColor,
      int highlightColor,
      ImageSpan[] imageSpans,
      ClickableSpan[] clickableSpans,
      int highlightStartOffset,
      int highlightEndOffset,
      int highlightCornerRadius) {
    mText = text;
    mLayout = layout;
    mLayoutTranslationX = layoutTranslationX;
    mLayoutTranslationY = layoutTranslationY;
    mClipToBounds = clipToBounds;
    mHighlightColor = highlightColor;
    mHighlightCornerRadius = highlightCornerRadius;
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
    mClickableSpans = clickableSpans;
    invalidate();
  }

  private static boolean highlightOffsetsValid(
      CharSequence text, int highlightStart, int highlightEnd) {
    return highlightStart >= 0 && highlightEnd <= text.length() && highlightStart < highlightEnd;
  }

  public void unmount() {
    mText = null;
    mLayout = null;
    mLayoutTranslationX = 0;
    mLayoutTranslationY = 0;
    mHighlightColor = 0;
    mHighlightCornerRadius = 0;
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

  // Note: if renaming this method, we have use reflection to access this in
  // EndToEndDumpsysHelper.java
  @DoNotStrip
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

    if (mHighlightCornerRadius != 0) {
      mHighlightPaint.setPathEffect(new CornerPathEffect(mHighlightCornerRadius));
    } else {
      mHighlightPaint.setPathEffect(null);
    }

    mSelectionPathNeedsUpdate = true;
    invalidate();
  }

  private void clearSelection() {
    setSelection(0, 0);
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

  public float getLayoutTranslationX() {
    return mLayoutTranslationX;
  }

  public float getLayoutTranslationY() {
    return mLayoutTranslationY;
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
    // Adjust for any canvas translations
    y -= mLayoutTranslationY;
    x -= mLayoutTranslationX;
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

  @Override
  public void setAccessibilityDelegate(@Nullable View.AccessibilityDelegate delegate) {
    super.setAccessibilityDelegate(delegate);
    // We need to do this like this to get the AccessibilityDelegateCompat with the ViewCompat
    // helper since the compatibility class/methods are protected.
    final AccessibilityDelegateCompat accessibilityDelegateCompat =
        ViewCompat.getAccessibilityDelegate(this);
    if (mRCTextAccessibilityDelegate != null
        && accessibilityDelegateCompat != mRCTextAccessibilityDelegate) {
      mRCTextAccessibilityDelegate.setWrappedAccessibilityDelegate(accessibilityDelegateCompat);
      ViewCompat.setAccessibilityDelegate(this, mRCTextAccessibilityDelegate);
    }
  }

  @Override
  public boolean dispatchHoverEvent(MotionEvent event) {
    return mRCTextAccessibilityDelegate.dispatchHoverEvent(event)
        || super.dispatchHoverEvent(event);
  }

  private class RCTextAccessibilityDelegate extends ExploreByTouchHelper {

    @Nullable private AccessibilityDelegateCompat mWrappedAccessibilityDelegate;

    public RCTextAccessibilityDelegate() {
      super(RCTextView.this);

      RCTextView.this.setFocusable(false);
      setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void setWrappedAccessibilityDelegate(
        @Nullable AccessibilityDelegateCompat wrappedAccessibilityDelegate) {
      mWrappedAccessibilityDelegate = wrappedAccessibilityDelegate;
    }

    @Override
    protected int getVirtualViewAt(float x, float y) {
      if (!(mText instanceof Spanned)) {
        return INVALID_ID;
      }
      final Spanned spanned = (Spanned) mText;

      for (int i = 0; i < mClickableSpans.length; i++) {
        final ClickableSpan span = mClickableSpans[i];
        final int start = spanned.getSpanStart(span);
        final int end = spanned.getSpanEnd(span);
        int textOffset = RCTextView.this.getTextOffsetAt((int) x, (int) y);
        if (textOffset >= start && textOffset <= end) {
          return i;
        }
      }

      return INVALID_ID;
    }

    @Override
    protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
      final int virtualViewsCount = mClickableSpans != null ? mClickableSpans.length : 0;
      for (int i = 0; i < virtualViewsCount; i++) {
        virtualViewIds.add(i);
      }
    }

    @Override
    protected void onPopulateNodeForVirtualView(
        int virtualViewId, AccessibilityNodeInfoCompat node) {
      final Spanned spanned = (Spanned) mText;
      final ClickableSpan span = mClickableSpans[virtualViewId];
      final int start = spanned.getSpanStart(span);
      final int end = spanned.getSpanEnd(span);
      final int startLine = mLayout.getLineForOffset(start);
      final int endLine = mLayout.getLineForOffset(end);
      final Path sTempPath = new Path();
      final Rect sTempRect = new Rect();
      final RectF sTempRectF = new RectF();

      // The bounds for multi-line strings should *only* include the first line.  This is because
      // TalkBack triggers its click at the center point of these bounds, and if that center point
      // is outside the spannable, it will click on something else.  There is no harm in not
      // outlining
      // the wrapped part of the string, as the text for the whole string will be read regardless of
      // the bounding box.
      final int selectionPathEnd =
          startLine == endLine ? end : mLayout.getLineVisibleEnd(startLine);

      mLayout.getSelectionPath(start, selectionPathEnd, sTempPath);
      sTempPath.computeBounds(sTempRectF, /* unused */ true);

      sTempRectF.round(sTempRect);

      node.setBoundsInParent(sTempRect);

      node.setClickable(true);
      node.setFocusable(true);
      node.setEnabled(true);
      node.setVisibleToUser(true);
      node.setText(spanned.subSequence(start, end));
      // This is needed to get the swipe action
      node.setClassName(ACCESSIBILITY_BUTTON_CLASS);
    }

    @Override
    protected boolean onPerformActionForVirtualView(
        int virtualViewId, int action, @Nullable Bundle arguments) {
      return false;
    }

    @Override
    public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
      super.onPopulateAccessibilityEvent(host, event);
      if (!TextUtils.isEmpty(mText)) {
        event.getText().add(getTextForAccessibility());
      }
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
      super.onInitializeAccessibilityNodeInfo(host, info);
      final CharSequence textForAccessibility = ((RCTextView) host).getTextForAccessibility();
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
      // We call the wrapped delegate to override any configuration done here.
      if (mWrappedAccessibilityDelegate != null) {
        mWrappedAccessibilityDelegate.onInitializeAccessibilityNodeInfo(host, info);
      }
    }
  }

  public Layout getLayout() {
    return mLayout;
  }
}
