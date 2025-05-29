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
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;
import static com.facebook.rendercore.text.accessibility.AccessibilityUtils.ACCESSIBILITY_ROLE_LINK;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.proguard.annotations.DoNotStrip;
import com.facebook.rendercore.text.TextMeasurementUtils.TextLayout;
import com.facebook.rendercore.text.accessibility.AccessibilityUtils;
import com.facebook.rendercore.text.accessibility.RCAccessibleClickableSpan;
import java.util.ArrayList;
import java.util.List;

/**
 * A custom Android View that behaves like a TextView providing support for spans, custom drawing
 * and truncations.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
@DoNotStrip
public class RCTextView extends View {
  private static final String ACCESSIBILITY_CLASS_BUTTON = "android.widget.Button";
  // See TextView#getTextForAccessibility()
  private static final int SAFE_PARCELABLE_SIZE = 1000000;
  @Nullable private final RCTextAccessibilityDelegate mRCTextAccessibilityDelegate;
  @Nullable private final AccessibilityManager mAccessibilityManager;
  private final Region mClickableSpanAreaRegion = new Region();
  private final Path mClickableSpanAreaPath = new Path();
  // NULLSAFE_FIXME[Field Not Initialized]
  private CharSequence mText;
  // NULLSAFE_FIXME[Field Not Initialized]
  private ClickableSpan[] mClickableSpans;
  // NULLSAFE_FIXME[Field Not Initialized]
  private Layout mLayout;
  private float mLayoutTranslationX;
  private float mLayoutTranslationY;
  @Nullable private ColorStateList mColorStateList;
  private int mLinkColor;
  private int mHighlightColor;
  private int mKeyboardHighlightColor;
  private int mHighlightCornerRadius;
  private boolean mIsExplicitlyTruncated;
  // NULLSAFE_FIXME[Field Not Initialized]
  private ImageSpan[] mImageSpans;
  private int mSelectionStart;
  private int mSelectionEnd;
  private int mSelectionColor;
  // NULLSAFE_FIXME[Field Not Initialized]
  private Path mSelectionPath;
  private boolean mSelectionPathNeedsUpdate;
  // NULLSAFE_FIXME[Field Not Initialized]
  private Paint mHighlightPaint;
  private boolean mIsSettingDefaultAccessibilityDelegate = false;
  private Rect mBounds = new Rect();
  private boolean mShouldHandleTouch;
  private boolean mShouldHandleKeyEvents;
  private boolean mLongClickActivated;
  private float mClickableSpanExpandedOffset;
  private float mOutlineWidth;
  private int mOutlineColor;
  @Nullable private Path mTouchAreaPath;
  @Nullable private Integer mWasFocusable;
  @Nullable private TouchableSpanListener mTouchableSpanListener;
  @Nullable private ClickableSpan mCurrentlyTouchedSpan;
  @Nullable private ClickableSpanListener mClickableSpanListener;
  @Nullable private Handler mLongClickHandler;
  @Nullable private LongClickRunnable mLongClickRunnable;

  public RCTextView(Context context) {
    super(context);

    if (getImportantForAccessibility() == IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
      mRCTextAccessibilityDelegate = new RCTextAccessibilityDelegate();
      setDefaultAccessibilityDelegate();
      mAccessibilityManager =
          (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
    } else {
      mRCTextAccessibilityDelegate = null;
      mAccessibilityManager = null;
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

    final OnPrePostDrawSpan[] onPrePostDrawSpans = getOnPrePostDrawSpans();
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
    if (!(mText instanceof Spanned)) {
      return;
    }
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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      maybeDrawOutline(canvas);
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      Api34Utils.draw(mLayout, canvas, getSelectionPath(), mHighlightPaint);
    } else {
      // NULLSAFE_FIXME[Parameter Not Nullable]
      mLayout.draw(canvas, getSelectionPath(), mHighlightPaint, 0);
    }
  }

  /**
   * All texts drawn on top of images and videos need contrast outlines and shadows to be more
   * visible against busy backgrounds. Standard Android shadows do not produce the separation of
   * intensity needed, so the RCTextView provides a special outline attribute that draws contrast
   * outlines usually combined with shadows. These outlines are drawn outside the contours to avoid
   * reducing the visible surface of character glyphs. However, since Android has no mode for
   * drawing outside strokes, they need to be drawn twice: the first pass draws strokes, and the
   * second pass draws inner filled shapes. This method performs the first outlining pass if needed.
   *
   * @param canvas - A canvas to draw on.
   */
  @RequiresApi(Build.VERSION_CODES.Q)
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

  private OnPrePostDrawSpan[] getOnPrePostDrawSpans() {
    if (!(mText instanceof Spanned)) {
      return new OnPrePostDrawSpan[0];
    }
    return ((Spanned) mText).getSpans(0, mText.length(), OnPrePostDrawSpan.class);
  }

  private MountableSpan[] getMountableSpans() {
    if (!(mText instanceof Spanned)) {
      return new MountableSpan[0];
    }
    return ((Spanned) mText).getSpans(0, mText.length(), MountableSpan.class);
  }

  public void mount(TextLayout textLayout) {
    final ColorStateList colorStateList = textLayout.textStyle.textColorStateList;
    mText = textLayout.processedText;
    mLayout = textLayout.layout;
    mLayoutTranslationX = textLayout.textLayoutTranslationX;
    mLayoutTranslationY = textLayout.textLayoutTranslationY;
    mHighlightColor = textLayout.textStyle.highlightColor;
    mKeyboardHighlightColor = textLayout.textStyle.keyboardHighlightColor;
    mHighlightCornerRadius = textLayout.textStyle.highlightCornerRadius;
    mIsExplicitlyTruncated = textLayout.isExplicitlyTruncated;
    mClickableSpanExpandedOffset = textLayout.textStyle.clickableSpanExpandedOffset;
    if (textLayout.textStyle.textColor != 0) {
      mColorStateList = null;
      mLinkColor = textLayout.textStyle.textColor;
    } else {
      mColorStateList = colorStateList;
      // NULLSAFE_FIXME[Nullable Dereference]
      mLinkColor = mColorStateList.getDefaultColor();
      if (mLayout != null) {
        mLayout
            .getPaint()
            // NULLSAFE_FIXME[Nullable Dereference]
            .setColor(mColorStateList.getColorForState(getDrawableState(), mLinkColor));
      }
    }

    if (highlightOffsetsValid(
        mText,
        textLayout.textStyle.highlightStartOffset,
        textLayout.textStyle.highlightEndOffset)) {
      setSelection(
          textLayout.textStyle.highlightStartOffset,
          textLayout.textStyle.highlightEndOffset,
          mHighlightColor);
    } else {
      clearSelection();
    }
    if (textLayout.textStyle.outlineWidth > 0f) {
      mOutlineWidth = textLayout.textStyle.outlineWidth;
      mOutlineColor = textLayout.textStyle.outlineColor;
    }

    if (textLayout.imageSpans != null) {
      mImageSpans = textLayout.imageSpans;

      for (int i = 0, size = mImageSpans.length; i < size; i++) {
        Drawable drawable = mImageSpans[i].getDrawable();
        // NULLSAFE_FIXME[Nullable Dereference]
        drawable.setCallback(this);
        // NULLSAFE_FIXME[Nullable Dereference]
        drawable.setVisible(true, false);
      }
    }
    mClickableSpans = textLayout.clickableSpans;
    mShouldHandleTouch = mClickableSpans != null && mClickableSpans.length > 0;
    mShouldHandleKeyEvents =
        mClickableSpans != null
            && mClickableSpans.length > 0
            && Color.alpha(mKeyboardHighlightColor) != 0;
    if (mShouldHandleKeyEvents) {
      // View needs to be focusable in order to receive key events
      // Capture focusable state to restore it on unmount
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        mWasFocusable = Api26Utils.getFocusable(this);
      } else {
        mWasFocusable = isFocusable() ? 1 : 0;
      }
      setFocusable(true);
    }
    if (textLayout.textStyle.accessibilityLabel != null) {
      setContentDescription(textLayout.textStyle.accessibilityLabel);
    }

    if (mLongClickHandler == null && containsLongClickableSpan(mClickableSpans)) {
      mLongClickHandler = new Handler(Looper.getMainLooper());
    }

    final MountableSpan[] mountableSpans = getMountableSpans();
    for (MountableSpan mountableSpan : mountableSpans) {
      mountableSpan.onMount(this);
    }
    invalidate();
  }

  public void setTouchableSpanListener(@Nullable TouchableSpanListener touchableSpanListener) {
    mTouchableSpanListener = touchableSpanListener;
  }

  public void setClickableSpanListener(@Nullable ClickableSpanListener clickableSpanListener) {
    mClickableSpanListener = clickableSpanListener;
  }

  public boolean isTextTruncated() {
    return mIsExplicitlyTruncated || TextMeasurementUtils.getEllipsizedLineNumber(mLayout) != -1;
  }

  public void unmount() {
    final MountableSpan[] mountableSpans = getMountableSpans();
    for (MountableSpan mountableSpan : mountableSpans) {
      mountableSpan.onUnmount(this);
    }
    // NULLSAFE_FIXME[Field Not Nullable]
    mText = null;
    // NULLSAFE_FIXME[Field Not Nullable]
    mLayout = null;
    mLayoutTranslationX = 0;
    mLayoutTranslationY = 0;
    mHighlightColor = 0;
    mKeyboardHighlightColor = 0;
    mHighlightCornerRadius = 0;
    mColorStateList = null;
    mLinkColor = 0;
    if (mImageSpans != null) {
      for (int i = 0, size = mImageSpans.length; i < size; i++) {
        Drawable drawable = mImageSpans[i].getDrawable();
        // NULLSAFE_FIXME[Nullable Dereference]
        drawable.setCallback(null);
        // NULLSAFE_FIXME[Nullable Dereference]
        drawable.setVisible(false, false);
      }
      // NULLSAFE_FIXME[Field Not Nullable]
      mImageSpans = null;
    }
    // NULLSAFE_FIXME[Field Not Nullable]
    mClickableSpans = null;
    mShouldHandleTouch = false;
    mShouldHandleKeyEvents = false;
    mTouchableSpanListener = null;
    mCurrentlyTouchedSpan = null;
    mClickableSpanListener = null;
    mBounds.setEmpty();
    mClickableSpanAreaRegion.setEmpty();
    mClickableSpanAreaPath.reset();
    resetLongClick();
    // Restore original focusable state if it was overridden
    if (mWasFocusable != null) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Api26Utils.setFocusable(this, mWasFocusable);
      } else {
        setFocusable(mWasFocusable == 1);
      }
      mWasFocusable = null;
    }
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

    if (Color.alpha(mSelectionColor) == 0) {
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

  private void setSelection(ClickableSpan span, boolean isTouch) {
    final Spanned text = (Spanned) mText;
    setSelection(
        text.getSpanStart(span),
        text.getSpanEnd(span),
        isTouch ? mHighlightColor : mKeyboardHighlightColor);
  }

  /** Updates selection to [selectionStart, selectionEnd] range. */
  private void setSelection(int selectionStart, int selectionEnd, int color) {
    if (Color.alpha(color) == 0
        || (mSelectionStart == selectionStart && mSelectionEnd == selectionEnd)) {
      return;
    }

    mSelectionStart = selectionStart;
    mSelectionEnd = selectionEnd;
    mSelectionColor = color;

    if (mHighlightPaint == null) {
      mHighlightPaint = new Paint();
    }
    mHighlightPaint.setColor(mSelectionColor);

    if (mHighlightCornerRadius != 0) {
      mHighlightPaint.setPathEffect(new CornerPathEffect(mHighlightCornerRadius));
    } else {
      mHighlightPaint.setPathEffect(null);
    }

    mSelectionPathNeedsUpdate = true;
    invalidate();
  }

  private void clearSelection() {
    setSelection(0, 0, mSelectionColor);
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
    if (!mShouldHandleTouch) {
      return super.onTouchEvent(event);
    }

    final int action = event.getActionMasked();

    if (action == ACTION_MOVE && !mLongClickActivated && mLongClickRunnable != null) {
      trackLongClickBoundaryOnMove(event);
    }

    final boolean clickActivationAllowed = !mLongClickActivated;

    ClickableSpan currentSpan = mCurrentlyTouchedSpan;
    if (action == ACTION_UP) {
      resetLongClick();
      clearSelection();
      if (clickActivationAllowed && currentSpan != null) {
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
        if (mClickableSpanListener == null || !mClickableSpanListener.onClick(currentSpan, this)) {
          currentSpan.onClick(this);
        }
      }
      mCurrentlyTouchedSpan = null;
    } else if (action == ACTION_DOWN) {
      final int x = (int) event.getX();
      final int y = (int) event.getY();
      mCurrentlyTouchedSpan = getClickableSpanInCoords(x, y);
      if (mCurrentlyTouchedSpan == null) {
        return super.onTouchEvent(event);
      }
      if (mCurrentlyTouchedSpan instanceof LongClickableSpan) {
        registerForLongClick((LongClickableSpan) mCurrentlyTouchedSpan);
      }
      setSelection(mCurrentlyTouchedSpan, true);
      currentSpan = mCurrentlyTouchedSpan;
    } else if (action == ACTION_CANCEL) {
      clearSelection();
      resetLongClick();
      mCurrentlyTouchedSpan = null;
    }
    if (mTouchableSpanListener != null) {
      return mTouchableSpanListener.onTouch(currentSpan, event, this);
    }
    if (currentSpan == null) {
      return super.onTouchEvent(event);
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

    if (mClickableSpanExpandedOffset > 0) {
      return getClickableSpanInProximityToClick(x, y);
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
  private ClickableSpan getClickableSpanInProximityToClick(float x, float y) {
    final Region touchAreaRegion = new Region();
    final Region clipBoundsRegion = new Region();

    if (mTouchAreaPath == null) {
      mTouchAreaPath = new Path();
    }

    clipBoundsRegion.set(
        0, 0, TextMeasurementUtils.getWidth(mLayout), TextMeasurementUtils.getHeight(mLayout));
    mTouchAreaPath.reset();
    mTouchAreaPath.addCircle(x, y, mClickableSpanExpandedOffset, Path.Direction.CW);
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

  private boolean isClickCloseToSpan(
      ClickableSpan span,
      Spanned buffer,
      Layout layout,
      Region touchAreaRegion,
      Region clipBoundsRegion) {
    mClickableSpanAreaRegion.setEmpty();
    mClickableSpanAreaPath.reset();

    layout.getSelectionPath(
        buffer.getSpanStart(span), buffer.getSpanEnd(span), mClickableSpanAreaPath);
    mClickableSpanAreaRegion.setPath(mClickableSpanAreaPath, clipBoundsRegion);

    return mClickableSpanAreaRegion.op(touchAreaRegion, Region.Op.INTERSECT);
  }

  private void resetLongClick() {
    if (mLongClickHandler != null && mLongClickRunnable != null) {
      mLongClickHandler.removeCallbacks(mLongClickRunnable);
      mLongClickRunnable = null;
    }
    mLongClickActivated = false;
  }

  private void registerForLongClick(@Nullable LongClickableSpan longClickableSpan) {
    if (longClickableSpan == null || mLongClickHandler == null) {
      return;
    }
    mLongClickRunnable = new LongClickRunnable(longClickableSpan, this);
    mLongClickHandler.postDelayed(mLongClickRunnable, ViewConfiguration.getLongPressTimeout());
  }

  private void trackLongClickBoundaryOnMove(MotionEvent event) {
    mBounds.setEmpty();
    this.getHitRect(mBounds);
    if (!isWithinBounds(mBounds, event)) {
      resetLongClick();
      return;
    }

    final ClickableSpan clickableSpan =
        getClickableSpanInCoords(
            (int) event.getX() - mBounds.left, (int) event.getY() - mBounds.top);
    if (mLongClickRunnable != null && mLongClickRunnable.longClickableSpan != clickableSpan) {
      // we are out of span area, reset longClick
      resetLongClick();
    }
  }

  private static boolean isWithinBounds(Rect bounds, MotionEvent event) {
    return bounds.contains((int) event.getX(), (int) event.getY());
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
    if (mShouldHandleKeyEvents && !gainFocus) {
      final int selectedSpanIndex = getSelectedSpanIndex();
      if (selectedSpanIndex != -1) {
        final ClickableSpan selectedSpan = mClickableSpans[selectedSpanIndex];
        if (selectedSpan instanceof RCAccessibleClickableSpan) {
          ((RCAccessibleClickableSpan) selectedSpan).setKeyboardSelected(false);
        }
        clearSelection();
      }
    }
    super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    if (mRCTextAccessibilityDelegate != null && mClickableSpans.length > 0) {
      mRCTextAccessibilityDelegate.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    return (mRCTextAccessibilityDelegate != null
            && mClickableSpans.length > 0
            // ExploreByTouchHelper does the same check internally for all other methods, expect
            // dispatchKeyEvent, this seems like a bug. Let's do the same check ourselves, otherwise
            // ExploreByTouchHelper would consume keyDown events even when the screen reader is
            // disabled.
            && isScreenReaderEnabled()
            && mRCTextAccessibilityDelegate.dispatchKeyEvent(event))
        || super.dispatchKeyEvent(event);
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    if (mShouldHandleKeyEvents
        && mSelectionStart == 0
        && mSelectionEnd == 0
        && (isDirectionKey(keyCode) || keyCode == KeyEvent.KEYCODE_TAB)) {
      // View just received focus due to keyboard navigation. Nothing is currently selected,
      // let's select first span according to the navigation direction.
      ClickableSpan targetSpan = null;
      if (isDirectionKey(keyCode) && event.hasNoModifiers()) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
          targetSpan = mClickableSpans[0];
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_UP) {
          targetSpan = mClickableSpans[mClickableSpans.length - 1];
        }
      }

      if (keyCode == KeyEvent.KEYCODE_TAB) {
        if (event.hasNoModifiers()) {
          targetSpan = mClickableSpans[0];
        } else if (event.hasModifiers(KeyEvent.META_SHIFT_ON)) {
          targetSpan = mClickableSpans[mClickableSpans.length - 1];
        }
      }

      if (targetSpan != null) {
        if (targetSpan instanceof RCAccessibleClickableSpan) {
          ((RCAccessibleClickableSpan) targetSpan).setKeyboardSelected(true);
        }
        setSelection(targetSpan, false);
        return true;
      }
    }

    return super.onKeyUp(keyCode, event);
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (mShouldHandleKeyEvents
        && (isDirectionKey(keyCode) || isConfirmKey(keyCode))
        && event.hasNoModifiers()) {
      final int selectedSpanIndex = getSelectedSpanIndex();
      if (selectedSpanIndex == -1) {
        return super.onKeyDown(keyCode, event);
      }
      final ClickableSpan selectedSpan = mClickableSpans[selectedSpanIndex];

      if (isDirectionKey(keyCode)) {
        final int direction;
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
          direction = 1;
        } else {
          // keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_UP
          direction = -1;
        }
        final int repeatCount = 1 + event.getRepeatCount();
        final int targetIndex = selectedSpanIndex + direction * repeatCount;
        if (targetIndex >= 0 && targetIndex < mClickableSpans.length) {
          final ClickableSpan targetSpan = mClickableSpans[targetIndex];
          if (selectedSpan instanceof RCAccessibleClickableSpan) {
            ((RCAccessibleClickableSpan) selectedSpan).setKeyboardSelected(false);
          }
          if (targetSpan instanceof RCAccessibleClickableSpan) {
            ((RCAccessibleClickableSpan) targetSpan).setKeyboardSelected(true);
          }
          setSelection(targetSpan, false);
          return true;
        }
      }

      if (isConfirmKey(keyCode) && event.getRepeatCount() == 0) {
        if (selectedSpan instanceof RCAccessibleClickableSpan) {
          ((RCAccessibleClickableSpan) selectedSpan).setKeyboardSelected(false);
        }
        clearSelection();
        selectedSpan.onClick(this);
        return true;
      }
    }

    return super.onKeyDown(keyCode, event);
  }

  public Layout getLayout() {
    return mLayout;
  }

  private static boolean isDirectionKey(int keyCode) {
    return keyCode == KeyEvent.KEYCODE_DPAD_LEFT
        || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
        || keyCode == KeyEvent.KEYCODE_DPAD_UP
        || keyCode == KeyEvent.KEYCODE_DPAD_DOWN;
  }

  private static boolean isConfirmKey(int keyCode) {
    return keyCode == KeyEvent.KEYCODE_DPAD_CENTER
        || keyCode == KeyEvent.KEYCODE_ENTER
        || keyCode == KeyEvent.KEYCODE_SPACE
        || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER;
  }

  private int getSelectedSpanIndex() {
    if (mClickableSpans == null
        || mClickableSpans.length == 0
        || (mSelectionStart == 0 && mSelectionEnd == 0)
        || !(mText instanceof Spanned)) {
      return -1;
    }
    final Spanned spanned = (Spanned) mText;
    for (int i = 0; i < mClickableSpans.length; i++) {
      final ClickableSpan span = mClickableSpans[i];
      final int spanStart = spanned.getSpanStart(span);
      final int spanEnd = spanned.getSpanEnd(span);
      if (spanStart == mSelectionStart && spanEnd == mSelectionEnd) {
        return i;
      }
    }
    return -1;
  }

  private boolean isScreenReaderEnabled() {
    // this system boolean is used by litho and internal tooling to "force" the accessibility state
    // copypaste of AccessibilityEnabledUtil, can't use this dependency directly because of WA
    if (Boolean.getBoolean("is_accessibility_enabled")) {
      return true;
    }

    return mAccessibilityManager != null
        && mAccessibilityManager.isEnabled()
        && mAccessibilityManager.isTouchExplorationEnabled();
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
      if (!(mText instanceof Spanned)) {
        return;
      }
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
      // Default to link if no role is specified to align with iOS –
      // https://fburl.com/code/eubqa9re.
      final String role =
          span.getAccessibilityRole() == null
              ? ACCESSIBILITY_ROLE_LINK
              : span.getAccessibilityRole();
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

  @RequiresApi(api = Build.VERSION_CODES.O)
  private static class Api26Utils {

    @DoNotInline
    public static int getFocusable(View view) {
      return view.getFocusable();
    }

    @DoNotInline
    public static void setFocusable(View view, int focusable) {
      view.setFocusable(focusable);
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
  private static class Api34Utils {

    private static List<Path> highlightPaths;
    private static List<Paint> highlightPaints;

    @DoNotInline
    public static void draw(
        Layout layout,
        Canvas canvas,
        @Nullable Path selectionPath,
        @Nullable Paint selectionPaint) {
      if (selectionPath != null) {
        // Layout#drawHighlights noops when highlightPaths and highlightPaints are nulls
        // Passing empty lists to fix that
        if (highlightPaths == null) {
          highlightPaths = new ArrayList<>();
        }
        if (highlightPaints == null) {
          highlightPaints = new ArrayList<>();
        }
      }
      layout.draw(canvas, highlightPaths, highlightPaints, selectionPath, selectionPaint, 0);
    }
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
          (mClickableSpanListener != null
                  && mClickableSpanListener.onLongClick(longClickableSpan, longClickableSpanView))
              || longClickableSpan.onLongClick(longClickableSpanView);
      sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED);
    }
  }
}
