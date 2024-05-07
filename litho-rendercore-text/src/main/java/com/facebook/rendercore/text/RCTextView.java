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
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;
import com.facebook.proguard.annotations.DoNotStrip;
import com.facebook.rendercore.text.TextMeasurementUtils.TextLayout;
import com.facebook.rendercore.text.accessibility.AccessibilityUtils;
import com.facebook.rendercore.text.accessibility.RCAccessibleClickableSpan;
import java.util.List;

/**
 * A custom Android View that behaves like a TextView providing support for spans, custom drawing
 * and truncations.
 */
@DoNotStrip
public class RCTextView extends View {
  private static final String ACCESSIBILITY_CLASS_BUTTON = "android.widget.Button";
  // See TextView#getTextForAccessibility()
  private static final int SAFE_PARCELABLE_SIZE = 1000000;
  @Nullable private final RCTextAccessibilityDelegate mRCTextAccessibilityDelegate;
  private CharSequence mText;
  private ClickableSpan[] mClickableSpans;
  private Layout mLayout;
  private float mLayoutTranslationX;
  private float mLayoutTranslationY;
  @Nullable private ColorStateList mColorStateList;
  private int mLinkColor;
  private int mHighlightColor;
  private int mHighlightCornerRadius;
  private boolean mIsExplicitlyTruncated;
  private ImageSpan[] mImageSpans;
  private int mSelectionStart;
  private int mSelectionEnd;
  private Path mSelectionPath;
  private boolean mSelectionPathNeedsUpdate;
  private Paint mHighlightPaint;
  private boolean mIsSettingDefaultAccessibilityDelegate = false;

  public RCTextView(Context context) {
    super(context);

    if (getImportantForAccessibility() == IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
      mRCTextAccessibilityDelegate = new RCTextAccessibilityDelegate();
      setDefaultAccessibilityDelegate();
    } else {
      mRCTextAccessibilityDelegate = null;
    }
  }

  private static boolean highlightOffsetsValid(
      CharSequence text, int highlightStart, int highlightEnd) {
    return highlightStart >= 0 && highlightEnd <= text.length() && highlightStart < highlightEnd;
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

    final OnPrePostDrawSpan[] onPrePostDrawSpans = getOnPrePostDrawableSpans();
    if (onPrePostDrawSpans.length == 0) {
      drawLayout(canvas);
    } else {
      drawInternal(onPrePostDrawSpans, canvas);
    }

    if (shouldRestore) {
      canvas.restoreToCount(saveCount);
    }
  }

  private void drawInternal(final OnPrePostDrawSpan[] onOnPrePostDrawSpans, final Canvas canvas) {
    OnPrePostDrawSpan.Command currentDrawAction =
        new OnPrePostDrawSpan.Command() {
          @Override
          public void draw(@NonNull Canvas canvas) {
            drawLayout(canvas);
          }
        };
    final Spanned text = (Spanned) mText;
    // OnPrePostDraw spans are retrieved in the same order in which they are applied (i.e. in order
    // of pre-order traversal of the composable span tree).  We want to have their onDraw calls
    // invoked in the same order.  In order to chain them in that order, we need to walk the spans
    // list backward.
    int length = onOnPrePostDrawSpans.length;
    for (int i = length - 1; i >= 0; i--) {
      OnPrePostDrawSpan onDraw = onOnPrePostDrawSpans[i];
      OnPrePostDrawSpan.Command previousAction = currentDrawAction;
      int spanStart = text.getSpanStart(onDraw);
      int spanEnd = text.getSpanEnd(onDraw);
      currentDrawAction =
          new OnPrePostDrawSpan.Command() {
            @Override
            public void draw(@NonNull Canvas canvas) {
              onDraw.draw(canvas, mText, spanStart, spanEnd, mLayout, previousAction);
            }
          };
    }
    currentDrawAction.draw(canvas);
  }

  private void drawLayout(Canvas canvas) {
    mLayout.draw(canvas, getSelectionPath(), mHighlightPaint, 0);
  }

  private OnPrePostDrawSpan[] getOnPrePostDrawableSpans() {
    if (!(mText instanceof Spanned)) {
      return new OnPrePostDrawSpan[0];
    }
    return ((Spanned) mText).getSpans(0, mText.length(), OnPrePostDrawSpan.class);
  }

  public void mount(TextLayout textLayout) {
    final ColorStateList colorStateList = textLayout.textStyle.textColorStateList;
    mText = textLayout.processedText;
    mLayout = textLayout.layout;
    mLayoutTranslationX = textLayout.textLayoutTranslationX;
    mLayoutTranslationY = textLayout.textLayoutTranslationY;
    mHighlightColor = textLayout.textStyle.highlightColor;
    mHighlightCornerRadius = textLayout.textStyle.highlightCornerRadius;
    mIsExplicitlyTruncated = textLayout.isExplicitlyTruncated;
    if (textLayout.textStyle.textColor != 0) {
      mColorStateList = null;
      mLinkColor = textLayout.textStyle.textColor;
    } else {
      mColorStateList = colorStateList;
      mLinkColor = mColorStateList.getDefaultColor();
      if (mLayout != null) {
        mLayout
            .getPaint()
            .setColor(mColorStateList.getColorForState(getDrawableState(), mLinkColor));
      }
    }

    if (highlightOffsetsValid(
        mText,
        textLayout.textStyle.highlightStartOffset,
        textLayout.textStyle.highlightEndOffset)) {
      setSelection(
          textLayout.textStyle.highlightStartOffset, textLayout.textStyle.highlightEndOffset);
    } else {
      clearSelection();
    }

    if (textLayout.imageSpans != null) {
      mImageSpans = textLayout.imageSpans;

      for (int i = 0, size = mImageSpans.length; i < size; i++) {
        Drawable drawable = mImageSpans[i].getDrawable();
        drawable.setCallback(this);
        drawable.setVisible(true, false);
      }
    }
    mClickableSpans = textLayout.clickableSpans;
    if (textLayout.textStyle.accessibilityLabel != null) {
      setContentDescription(textLayout.textStyle.accessibilityLabel);
    }
    invalidate();
  }

  public boolean isTextTruncated() {
    return mIsExplicitlyTruncated || TextMeasurementUtils.getEllipsizedLineNumber(mLayout) != -1;
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
    mClickableSpans = null;
    setContentDescription("");

    if (mRCTextAccessibilityDelegate != null) {
      mRCTextAccessibilityDelegate.invalidateRoot();
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

  private void setDefaultAccessibilityDelegate() {
    if (mRCTextAccessibilityDelegate != null) {
      mIsSettingDefaultAccessibilityDelegate = true;
      ViewCompat.setAccessibilityDelegate(this, mRCTextAccessibilityDelegate);
      mIsSettingDefaultAccessibilityDelegate = false;
    }
  }

  @Override
  public void setAccessibilityDelegate(@Nullable View.AccessibilityDelegate delegate) {
    super.setAccessibilityDelegate(delegate);
    if (mRCTextAccessibilityDelegate != null && !mIsSettingDefaultAccessibilityDelegate) {
      // We need to do this like this to get the AccessibilityDelegateCompat with the ViewCompat
      // helper since the compatibility class/methods are protected.
      final AccessibilityDelegateCompat accessibilityDelegateCompat =
          ViewCompat.getAccessibilityDelegate(this);
      if (accessibilityDelegateCompat != mRCTextAccessibilityDelegate) {
        mRCTextAccessibilityDelegate.setWrappedAccessibilityDelegate(accessibilityDelegateCompat);
        setDefaultAccessibilityDelegate();
      }
    }
  }

  @Override
  public boolean dispatchHoverEvent(MotionEvent event) {
    return (mRCTextAccessibilityDelegate != null
            && mRCTextAccessibilityDelegate.dispatchHoverEvent(event))
        || super.dispatchHoverEvent(event);
  }

  @Override
  public final void onFocusChanged(
      boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
    super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    if (mRCTextAccessibilityDelegate != null && mClickableSpans.length > 0) {
      mRCTextAccessibilityDelegate.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    return (mRCTextAccessibilityDelegate != null
            && mClickableSpans.length > 0
            && mRCTextAccessibilityDelegate.dispatchKeyEvent(event))
        || super.dispatchKeyEvent(event);
  }

  public Layout getLayout() {
    return mLayout;
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
      final Rect sTempRect = new Rect();
      // it can happen as part of an unmount/mount cycle that the accessibility framework will
      // request the bounds of a virtual view even if it no longer exists.
      if (mClickableSpans == null || virtualViewId >= mClickableSpans.length) {
        node.setText("");
        node.setBoundsInParent(sTempRect);
        return;
      }
      final ClickableSpan span = mClickableSpans[virtualViewId];
      final int start = spanned.getSpanStart(span);
      final int end = spanned.getSpanEnd(span);
      final int startLine = mLayout.getLineForOffset(start);
      final int endLine = mLayout.getLineForOffset(end);
      final Path sTempPath = new Path();
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
      sTempRectF.offset(mLayoutTranslationX, mLayoutTranslationY);
      sTempRectF.round(sTempRect);

      node.setBoundsInParent(sTempRect);

      node.setClickable(true);
      node.setFocusable(true);
      node.setEnabled(true);
      node.setVisibleToUser(true);
      node.setText(spanned.subSequence(start, end));
      // This is needed to get the swipe action
      node.setClassName(ACCESSIBILITY_CLASS_BUTTON);

      if (span instanceof RCAccessibleClickableSpan) {
        populateNodeAccessibilityInfo((RCAccessibleClickableSpan) span, node);
      }
    }

    private void populateNodeAccessibilityInfo(
        RCAccessibleClickableSpan span, AccessibilityNodeInfoCompat node) {
      final String label = span.getAccessibilityLabel();
      final String role = span.getAccessibilityRole();
      AccessibilityUtils.initializeAccessibilityLabel(label, node);
      AccessibilityUtils.initializeAccessibilityRole(getContext(), role, null, node);
    }

    @Override
    protected boolean onPerformActionForVirtualView(
        int virtualViewId, int action, @Nullable Bundle arguments) {
      if (action == AccessibilityNodeInfoCompat.ACTION_CLICK) {
        mClickableSpans[virtualViewId].onClick(RCTextView.this);
        return true;
      }
      return false;
    }

    @Override
    protected void onVirtualViewKeyboardFocusChanged(int virtualViewId, boolean hasFocus) {

      if (mClickableSpans[virtualViewId] instanceof RCAccessibleClickableSpan) {
        ((RCAccessibleClickableSpan) mClickableSpans[virtualViewId]).setKeyboardFocused(hasFocus);

        // force redraw when focus changes, so that any visual changes get applied.
        RCTextView.this.invalidate();
      }
      super.onVirtualViewKeyboardFocusChanged(virtualViewId, hasFocus);
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
}
