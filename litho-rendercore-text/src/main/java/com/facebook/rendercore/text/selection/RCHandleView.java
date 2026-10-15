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

package com.facebook.rendercore.text.selection;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.text.Layout;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.Magnifier;
import android.widget.PopupWindow;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.facebook.rendercore.text.RCTextView;
import java.util.Collections;

/**
 * Abstract base class for selection handle views (start and end handles).
 *
 * <p>Ported from AOSP {@code Editor.HandleView} (Editor.java:5049-5814). Each handle is rendered in
 * a PopupWindow positioned relative to the parent RCTextView. The handle supports touch-based
 * dragging with magnifier integration and vertical hysteresis for stable line tracking.
 *
 * <p>Key differences from AOSP:
 *
 * <ul>
 *   <li>Uses a simple OnPreDrawListener instead of AOSP's TextViewPositionListener/PositionListener
 *       inner classes
 *   <li>No InsertionHandleView (we only support selection, not cursor insertion)
 * </ul>
 */
public abstract class RCHandleView extends View {

  // Copied from AOSP Editor.java:189,188
  private static final int UNSET_LINE = -1;
  private static final float UNSET_X_VALUE = -1f;

  // Touch-up filter constants — copied from AOSP Editor.HandleView (Editor.java:5171-5173)
  private static final int HISTORY_SIZE = 5;
  private static final int TOUCH_UP_FILTER_DELAY_AFTER = 150;
  private static final int TOUCH_UP_FILTER_DELAY_BEFORE = 350;

  protected final RCTextView mTextView;
  protected final RCTextEditor mEditor;

  protected Drawable mDrawable;
  protected Drawable mDrawableLtr;
  protected Drawable mDrawableRtl;
  private final PopupWindow mContainer;

  // Position with respect to the parent RCTextView
  private int mPositionX;
  private int mPositionY;
  private boolean mIsDragging;
  // Offset from touch position to mPosition
  private float mTouchToWindowOffsetX;
  private float mTouchToWindowOffsetY;
  protected int mHotspotX;
  protected int mHorizontalGravity;
  // Offsets the hotspot point up, so cursor is not hidden by finger when moving up
  private float mTouchOffsetY;
  // Where the touch position should be on the handle to ensure maximum cursor visibility.
  private final float mIdealVerticalOffset;
  // Parent's previous position in window
  private int mLastParentX;
  private int mLastParentY;
  // Parent's previous position on screen
  private int mLastParentXOnScreen;
  private int mLastParentYOnScreen;
  // Previous text character offset
  protected int mPreviousOffset = -1;
  // Previous text character offset
  private boolean mPositionHasChanged = true;
  // Minimum touch target size for handles
  private int mMinSize;
  // Indicates the line of text that the handle is on.
  protected int mPrevLine = UNSET_LINE;
  // Indicates the line of text that the user was touching.
  protected int mPreviousLineTouched = UNSET_LINE;
  // The raw x coordinate of the motion down event which started the current dragging session.
  private float mCurrentDragInitialTouchRawX = UNSET_X_VALUE;
  // The scale transform applied by containers to the view.
  private float mTextViewScaleX;
  private float mTextViewScaleY;

  // Touch-up filter
  private final long[] mPreviousOffsetsTimes = new long[HISTORY_SIZE];
  private final int[] mPreviousOffsets = new int[HISTORY_SIZE];
  private int mPreviousOffsetIndex = 0;
  private int mNumberPreviousOffsets = 0;

  // Magnifier support
  @Nullable private MagnifierMotionAnimator mMagnifierAnimator;

  // Pre-draw listener for handle repositioning during scroll/animation
  @Nullable private ViewTreeObserver.OnPreDrawListener mPositionListener;

  RCHandleView(
      RCTextView textView, RCTextEditor editor, Drawable drawableLtr, Drawable drawableRtl) {
    super(textView.getContext());

    mTextView = textView;
    mEditor = editor;
    mContainer =
        new PopupWindow(textView.getContext(), null, android.R.attr.textSelectHandleWindowStyle);
    mContainer.setSplitTouchEnabled(true);
    mContainer.setClippingEnabled(false);
    // PopupWindow.setWindowLayoutType() requires API 23 (M).
    // API behavior:
    //   API 23+ (M): Sets the window type to TYPE_APPLICATION_SUB_PANEL, ensuring the handle
    //     popup renders above other application windows (matching AOSP behavior).
    //   Pre-23: Uses the PopupWindow's default window type (TYPE_APPLICATION_PANEL). The handle
    //     still displays correctly in typical scenarios but may render below certain overlays.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      mContainer.setWindowLayoutType(WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL);
    }
    mContainer.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
    mContainer.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
    mContainer.setContentView(this);

    setDrawables(drawableLtr, drawableRtl);

    mMinSize = (int) (40 * textView.getContext().getResources().getDisplayMetrics().density);

    final int handleHeight = getPreferredHeight();
    mTouchOffsetY = -0.3f * handleHeight;
    mIdealVerticalOffset = 0.7f * handleHeight;
  }

  public float getIdealVerticalOffset() {
    return mIdealVerticalOffset;
  }

  void setDrawables(final Drawable drawableLtr, final Drawable drawableRtl) {
    mDrawableLtr = drawableLtr;
    mDrawableRtl = drawableRtl;
    updateDrawable(true /* updateDrawableWhenDragging */);
  }

  // Ported from AOSP Editor.HandleView.updateDrawable() (Editor.java:5142-5165).
  protected void updateDrawable(final boolean updateDrawableWhenDragging) {
    if (!updateDrawableWhenDragging && mIsDragging) {
      return;
    }
    final Layout layout = mTextView.getLayout();
    if (layout == null) {
      return;
    }
    final int offset = getCurrentCursorOffset();
    final boolean isRtlCharAtOffset = isAtRtlRun(layout, offset);
    final Drawable oldDrawable = mDrawable;
    mDrawable = isRtlCharAtOffset ? mDrawableRtl : mDrawableLtr;
    mHotspotX = getHotspotX(mDrawable, isRtlCharAtOffset);
    mHorizontalGravity = getHorizontalGravity(isRtlCharAtOffset);
    if (oldDrawable != mDrawable && isShowing()) {
      // Update popup window position.
      mPositionX =
          getCursorHorizontalPosition(layout, offset)
              - mHotspotX
              - getHorizontalOffset()
              + getCursorOffset();
      mPositionX += viewportToContentHorizontalOffset();
      mPositionHasChanged = true;
      updatePosition(mLastParentX, mLastParentY, false, false);
      postInvalidate();
    }
  }

  protected abstract int getHotspotX(Drawable drawable, boolean isRtlRun);

  protected abstract int getHorizontalGravity(boolean isRtlRun);

  // Ported from AOSP Editor.HandleView.isAtRtlRun() (Editor.java:5295-5300).
  protected boolean isAtRtlRun(@NonNull Layout layout, int offset) {
    return layout.isRtlCharAt(offset);
  }

  // Ported from AOSP Editor.HandleView.getHorizontal() (Editor.java:5302-5306).
  float getHorizontal(@NonNull Layout layout, int offset) {
    return layout.getPrimaryHorizontal(offset);
  }

  int getLineForOffset(@NonNull Layout layout, int offset) {
    return layout.getLineForOffset(offset);
  }

  // Ported from AOSP Editor.HandleView.getOffsetAtCoordinate() (Editor.java:5320-5322).
  protected int getOffsetAtCoordinate(@NonNull Layout layout, int line, float x) {
    final float localX = convertToLocalHorizontalCoordinate(x);
    return layout.getOffsetForHorizontal(line, localX);
  }

  // Ported from AOSP TextView.convertToLocalHorizontalCoordinate() (TextView.java:15773-15781).
  private float convertToLocalHorizontalCoordinate(float x) {
    x -= mTextView.getTotalPaddingLeft();
    // Clamp the position to inside of the view.
    x = Math.max(0.0f, x);
    x = Math.min(mTextView.getWidth() - mTextView.getTotalPaddingRight() - 1, x);
    x += mTextView.getScrollX();
    return x;
  }

  // Ported from AOSP Editor.HandleView.startTouchUpFilter() (Editor.java:5179-5182).
  private void startTouchUpFilter(int offset) {
    mNumberPreviousOffsets = 0;
    addPositionToTouchUpFilter(offset);
  }

  // Ported from AOSP Editor.HandleView.addPositionToTouchUpFilter() (Editor.java:5184-5189).
  private void addPositionToTouchUpFilter(int offset) {
    mPreviousOffsetIndex = (mPreviousOffsetIndex + 1) % HISTORY_SIZE;
    mPreviousOffsets[mPreviousOffsetIndex] = offset;
    mPreviousOffsetsTimes[mPreviousOffsetIndex] = SystemClock.uptimeMillis();
    mNumberPreviousOffsets++;
  }

  // Ported from AOSP Editor.HandleView.filterOnTouchUp() (Editor.java:5191-5210).
  private void filterOnTouchUp(boolean fromTouchScreen) {
    final long now = SystemClock.uptimeMillis();
    int i = 0;
    int index = mPreviousOffsetIndex;
    final int iMax = Math.min(mNumberPreviousOffsets, HISTORY_SIZE);
    while (i < iMax && (now - mPreviousOffsetsTimes[index]) < TOUCH_UP_FILTER_DELAY_AFTER) {
      i++;
      index = (mPreviousOffsetIndex - i + HISTORY_SIZE) % HISTORY_SIZE;
    }

    if (i > 0 && i < iMax && (now - mPreviousOffsetsTimes[index]) > TOUCH_UP_FILTER_DELAY_BEFORE) {
      positionAtCursorOffset(mPreviousOffsets[index], false, fromTouchScreen);
    }
  }

  boolean offsetHasBeenChanged() {
    return mNumberPreviousOffsets > 1;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    setMeasuredDimension(getPreferredWidth(), getPreferredHeight());
  }

  @Override
  public void invalidate() {
    super.invalidate();
    if (isShowing()) {
      positionAtCursorOffset(getCurrentCursorOffset(), true, false);
    }
  }

  final int getPreferredWidth() {
    return Math.max(mDrawable.getIntrinsicWidth(), mMinSize);
  }

  final int getPreferredHeight() {
    return Math.max(mDrawable.getIntrinsicHeight(), mMinSize);
  }

  public void show() {
    if (isShowing()) return;

    // Make sure the offset is always considered new, even when focusing at same position
    mPreviousOffset = -1;
    positionAtCursorOffset(getCurrentCursorOffset(), false, false);

    // Register a pre-draw listener so handle positions follow the view during scroll/animation.
    registerPositionListener();
  }

  // Ported from AOSP Editor.HandleView.dismiss() (Editor.java:5247-5251).
  // Dismisses the popup but keeps the position listener active so that
  // updatePosition() continues to be called on each frame. This allows
  // the handle to re-appear when it scrolls back into view (shouldShow()
  // returns true again and showAtLocation is called).
  void dismiss() {
    mIsDragging = false;
    mContainer.dismiss();
    onDetached();
  }

  // Ported from AOSP Editor.HandleView.hide() (Editor.java:5253-5262).
  // Full hide: dismisses the popup AND unregisters the position listener.
  // After hide(), the handle will not re-appear until show() is called again.
  public void hide() {
    dismiss();
    unregisterPositionListener();
  }

  public boolean isShowing() {
    return mContainer.isShowing();
  }

  // Ported from AOSP Editor.HandleView.shouldShow() (Editor.java:5268-5280).
  private boolean shouldShow() {
    // A dragging handle should always be shown.
    if (mIsDragging) {
      return true;
    }
    return mTextView.isPositionVisible(mPositionX + mHotspotX + getHorizontalOffset(), mPositionY);
  }

  public void setVisible(final boolean visible) {
    mContainer.getContentView().setVisibility(visible ? VISIBLE : INVISIBLE);
  }

  abstract int getCurrentCursorOffset();

  protected abstract void updateSelection(int offset);

  protected abstract void updatePosition(float x, float y, boolean fromTouchScreen);

  // Ported from AOSP Editor.HandleView.positionAtCursorOffset() (Editor.java:5331-5370).
  /**
   * @param offset Cursor offset. Must be in [-1, length].
   * @param forceUpdatePosition whether to force update the position.
   * @param fromTouchScreen true if the cursor is moved with motion events from the touch screen.
   */
  void positionAtCursorOffset(int offset, boolean forceUpdatePosition, boolean fromTouchScreen) {
    final Layout layout = mTextView.getLayout();
    if (layout == null) {
      return;
    }

    boolean offsetChanged = offset != mPreviousOffset;
    if (offsetChanged || forceUpdatePosition) {
      if (offsetChanged) {
        updateSelection(offset);
        addPositionToTouchUpFilter(offset);
      }
      final int line = getLineForOffset(layout, offset);
      mPrevLine = line;

      mPositionX =
          getCursorHorizontalPosition(layout, offset)
              - mHotspotX
              - getHorizontalOffset()
              + getCursorOffset();
      // Layout.getLineBottom(int, boolean) requires API 34 (UPSIDE_DOWN_CAKE).
      // API behavior:
      //   API 34+: Uses getLineBottom(line, false) to exclude line spacing from the bottom
      //     coordinate, giving a tighter handle position against the text baseline (AOSP behavior).
      //   Pre-34: Uses getLineBottom(line) which includes line spacing. The handle may sit
      //     slightly lower when line spacing is set, but this is a minor visual difference.
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        mPositionY = layout.getLineBottom(line, /* includeLineSpacing= */ false);
      } else {
        mPositionY = layout.getLineBottom(line);
      }

      // Take padding and scroll into account.
      mPositionX += viewportToContentHorizontalOffset();
      mPositionY += viewportToContentVerticalOffset();

      // Ported from AOSP: provide haptic feedback when cursor position changes via touch.
      if (offsetChanged && fromTouchScreen) {
        mTextView.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE);
      }

      mPreviousOffset = offset;
      mPositionHasChanged = true;
    }
  }

  // Ported from AOSP Editor.HandleView.getCursorHorizontalPosition() (Editor.java:5373-5375).
  int getCursorHorizontalPosition(Layout layout, int offset) {
    return (int) (getHorizontal(layout, offset) - 0.5f);
  }

  // Ported from AOSP TextView.viewportToContentHorizontalOffset() (TextView.java:12191-12194).
  private int viewportToContentHorizontalOffset() {
    return mTextView.getTotalPaddingLeft() - mTextView.getScrollX();
  }

  // Ported from AOSP TextView.viewportToContentVerticalOffset() (TextView.java:12196-12200).
  private int viewportToContentVerticalOffset() {
    return mTextView.getTotalPaddingTop() - mTextView.getScrollY();
  }

  // Ported from AOSP Editor.HandleView.updatePosition() (Editor.java:5378-5412).
  // Modified: uses getLocationInWindow() for coordinate transformation instead of AOSP's
  // PositionListener which tracks parent position via ViewTreeObserver.
  void updatePosition(
      int parentPositionX,
      int parentPositionY,
      boolean parentPositionChanged,
      boolean parentScrolled) {
    positionAtCursorOffset(getCurrentCursorOffset(), parentScrolled, false);
    if (parentPositionChanged || mPositionHasChanged) {
      if (mIsDragging) {
        // Update touchToWindow offset in case of parent scrolling while dragging
        if (parentPositionX != mLastParentX || parentPositionY != mLastParentY) {
          mTouchToWindowOffsetX += parentPositionX - mLastParentX;
          mTouchToWindowOffsetY += parentPositionY - mLastParentY;
          mLastParentX = parentPositionX;
          mLastParentY = parentPositionY;
        }
        onHandleMoved();
      }

      if (shouldShow()) {
        // Transform from view-local coordinates to window coordinates.
        // PopupWindow.showAtLocation with Gravity.NO_GRAVITY expects window coordinates.
        // Start with the cursor position in view space (mPositionX includes hotspot/offset
        // adjustments, so we add them back to get the raw cursor x for transformation).
        final int[] pts = {mPositionX + mHotspotX + getHorizontalOffset(), mPositionY};
        final int[] viewLocation = new int[2];
        mTextView.getLocationInWindow(viewLocation);
        pts[0] += viewLocation[0];
        pts[1] += viewLocation[1];
        // Subtract the hotspot and horizontal offset so the popup is positioned such that
        // the handle's hotspot aligns with the cursor position (matching AOSP).
        pts[0] -= mHotspotX + getHorizontalOffset();

        if (isShowing()) {
          mContainer.update(pts[0], pts[1], -1, -1);
        } else {
          mContainer.showAtLocation(mTextView, Gravity.NO_GRAVITY, pts[0], pts[1]);
        }
      } else {
        if (isShowing()) {
          dismiss();
        }
      }

      mPositionHasChanged = false;
    }
  }

  @Override
  protected void onDraw(Canvas c) {
    final int drawWidth = mDrawable.getIntrinsicWidth();
    final int left = getHorizontalOffset();

    mDrawable.setBounds(left, 0, left + drawWidth, mDrawable.getIntrinsicHeight());
    mDrawable.draw(c);
  }

  // Ported from AOSP Editor.HandleView.getHorizontalOffset() (Editor.java:5424-5441) — computes
  // left offset based on gravity to center/align the handle drawable within the touch target area.
  private int getHorizontalOffset() {
    final int width = getPreferredWidth();
    final int drawWidth = mDrawable.getIntrinsicWidth();
    final int left;
    switch (mHorizontalGravity) {
      case Gravity.LEFT:
        left = 0;
        break;
      case Gravity.RIGHT:
        left = width - drawWidth;
        break;
      default:
      case Gravity.CENTER:
        left = (width - drawWidth) / 2;
        break;
    }
    return left;
  }

  protected int getCursorOffset() {
    return 0;
  }

  // Ported from AOSP Editor.HandleView.tooLargeTextForMagnifier() (Editor.java:5447-5467).
  private boolean tooLargeTextForMagnifier() {
    if (mMagnifierAnimator == null) return true;
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return true;
    final float magnifierContentHeight =
        Math.round(
            mMagnifierAnimator.mMagnifier.getHeight() / mMagnifierAnimator.mMagnifier.getZoom());
    final float glyphHeight =
        mTextView.getPaint().getFontMetrics().descent
            - mTextView.getPaint().getFontMetrics().ascent;
    return glyphHeight * mTextViewScaleY > magnifierContentHeight;
  }

  // Ported from AOSP Editor.HandleView.checkForTransforms() (Editor.java:5470-5503).
  private boolean checkForTransforms() {
    if (mMagnifierAnimator != null
        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        && mMagnifierAnimator.mMagnifierIsShowing) {
      return true;
    }

    if (mTextView.getRotation() != 0f
        || mTextView.getRotationX() != 0f
        || mTextView.getRotationY() != 0f) {
      return false;
    }
    mTextViewScaleX = mTextView.getScaleX();
    mTextViewScaleY = mTextView.getScaleY();

    ViewParent viewParent = mTextView.getParent();
    while (viewParent != null) {
      if (viewParent instanceof View) {
        final View view = (View) viewParent;
        if (view.getRotation() != 0f || view.getRotationX() != 0f || view.getRotationY() != 0f) {
          return false;
        }
        mTextViewScaleX *= view.getScaleX();
        mTextViewScaleY *= view.getScaleY();
      }
      viewParent = viewParent.getParent();
    }
    return true;
  }

  // Ported from AOSP Editor.HandleView.obtainMagnifierShowCoordinates() (Editor.java:5505-5625).
  // Simplified: removed same-line selection boundary clamping since it requires accessing the
  // other handle's offset, which AOSP does via a different abstraction.
  private boolean obtainMagnifierShowCoordinates(
      @NonNull final MotionEvent event, final PointF showPosInView) {
    final int trigger = getMagnifierHandleTrigger();
    final int offset;
    switch (trigger) {
      case MagnifierHandleTrigger.SELECTION_START:
        offset = mEditor.getSelectionStart();
        break;
      case MagnifierHandleTrigger.SELECTION_END:
        offset = mEditor.getSelectionEnd();
        break;
      default:
        offset = -1;
        break;
    }

    if (offset == -1) {
      return false;
    }

    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
      mCurrentDragInitialTouchRawX = event.getRawX();
    } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
      mCurrentDragInitialTouchRawX = UNSET_X_VALUE;
    }

    final Layout layout = mTextView.getLayout();
    final int lineNumber = getLineForOffset(layout, offset);

    final int[] textViewLocationOnScreen = new int[2];
    mTextView.getLocationOnScreen(textViewLocationOnScreen);
    final float touchXInView = event.getRawX() - textViewLocationOnScreen[0];

    float leftBound = 0;
    float rightBound = mTextView.getWidth();
    if (touchXInView < leftBound || touchXInView > rightBound) {
      return false;
    }

    showPosInView.x = Math.max(leftBound, Math.min(rightBound, touchXInView));

    // Vertically snap to middle of current line.
    // Layout.getLineBottom(int, boolean) requires API 34 (UPSIDE_DOWN_CAKE).
    // API behavior:
    //   API 34+: Uses getLineBottom(line, false) to exclude line spacing, giving a tighter
    //     vertical center for magnifier positioning (same as AOSP Editor.java:5606-5609).
    //   Pre-34: Uses getLineBottom(line) which includes line spacing. The magnifier center
    //     may be slightly lower when line spacing is set.
    final float lineBottom;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      lineBottom = layout.getLineBottom(lineNumber, /* includeLineSpacing= */ false);
    } else {
      lineBottom = layout.getLineBottom(lineNumber);
    }
    showPosInView.y =
        ((layout.getLineTop(lineNumber) + lineBottom) / 2.0f
                + mTextView.getTotalPaddingTop()
                - mTextView.getScrollY())
            * mTextViewScaleY;
    return true;
  }

  // Ported from AOSP Editor.HandleView.updateMagnifier() (Editor.java:5653-5705).
  public void updateMagnifier(@NonNull final MotionEvent event) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
      return;
    }
    if (mMagnifierAnimator == null) {
      mMagnifierAnimator = new MagnifierMotionAnimator(new Magnifier(mTextView));
    }

    final PointF showPosInView = new PointF();
    final boolean shouldShow =
        checkForTransforms()
            && !tooLargeTextForMagnifier()
            && obtainMagnifierShowCoordinates(event, showPosInView);
    if (shouldShow) {
      mMagnifierAnimator.show(showPosInView.x, showPosInView.y);
    } else {
      dismissMagnifier();
    }
  }

  // Ported from AOSP Editor.HandleView.dismissMagnifier() (Editor.java:5709-5723).
  public void dismissMagnifier() {
    if (mMagnifierAnimator != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      mMagnifierAnimator.dismiss();
      setVisible(true);
      final RCHandleView otherHandle = getOtherSelectionHandle();
      if (otherHandle != null) {
        otherHandle.setVisible(true);
      }
    }
  }

  // Ported from AOSP Editor.HandleView.getOtherSelectionHandle() (Editor.java:5627-5635).
  @Nullable
  private RCHandleView getOtherSelectionHandle() {
    final RCSelectionController controller = mEditor.getSelectionController();
    if (controller == null || !controller.isActive()) {
      return null;
    }
    return controller.mStartHandle != this ? controller.mStartHandle : controller.mEndHandle;
  }

  // Ported from AOSP Editor.HandleView.onTouchEvent() (Editor.java:5728-5800). Simplified to
  // remove insertion handle logic and focus on selection handles only.
  @Override
  @SuppressWarnings("fallthrough")
  public boolean onTouchEvent(MotionEvent ev) {
    updateFloatingToolbarVisibility(ev);

    switch (ev.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        {
          startTouchUpFilter(getCurrentCursorOffset());

          // Mark that the touch is on a handle (for double-tap detection in touch state)
          mEditor.getTouchState().setIsOnHandle(true);

          final int[] parentLocation = new int[2];
          mTextView.getLocationInWindow(parentLocation);
          mLastParentX = parentLocation[0];
          mLastParentY = parentLocation[1];
          final int[] screenLocation = new int[2];
          mTextView.getLocationOnScreen(screenLocation);
          mLastParentXOnScreen = screenLocation[0];
          mLastParentYOnScreen = screenLocation[1];

          final float xInWindow = ev.getRawX() - mLastParentXOnScreen + mLastParentX;
          final float yInWindow = ev.getRawY() - mLastParentYOnScreen + mLastParentY;
          mTouchToWindowOffsetX = xInWindow - mPositionX;
          mTouchToWindowOffsetY = yInWindow - mPositionY;

          mIsDragging = true;
          mPreviousLineTouched = UNSET_LINE;
          break;
        }

      case MotionEvent.ACTION_MOVE:
        {
          final float xInWindow = ev.getRawX() - mLastParentXOnScreen + mLastParentX;
          final float yInWindow = ev.getRawY() - mLastParentYOnScreen + mLastParentY;

          // Vertical hysteresis: vertical down movement tends to snap to ideal offset
          final float previousVerticalOffset = mTouchToWindowOffsetY - mLastParentY;
          final float currentVerticalOffset = yInWindow - mPositionY - mLastParentY;
          float newVerticalOffset;
          if (previousVerticalOffset < mIdealVerticalOffset) {
            newVerticalOffset = Math.min(currentVerticalOffset, mIdealVerticalOffset);
            newVerticalOffset = Math.max(newVerticalOffset, previousVerticalOffset);
          } else {
            newVerticalOffset = Math.max(currentVerticalOffset, mIdealVerticalOffset);
            newVerticalOffset = Math.min(newVerticalOffset, previousVerticalOffset);
          }
          mTouchToWindowOffsetY = newVerticalOffset + mLastParentY;

          final float newPosX =
              xInWindow - mTouchToWindowOffsetX + mHotspotX + getHorizontalOffset();
          final float newPosY = yInWindow - mTouchToWindowOffsetY + mTouchOffsetY;

          updatePosition(newPosX, newPosY, ev.isFromSource(InputDevice.SOURCE_TOUCHSCREEN));
          break;
        }

      case MotionEvent.ACTION_UP:
        filterOnTouchUp(ev.isFromSource(InputDevice.SOURCE_TOUCHSCREEN));
      // fallthrough
      case MotionEvent.ACTION_CANCEL:
        mIsDragging = false;
        mEditor.getTouchState().setIsOnHandle(false);
        updateDrawable(false /* updateDrawableWhenDragging */);
        break;
    }
    return true;
  }

  // Ported from AOSP Editor.updateFloatingToolbarVisibility() (Editor.java:1810-1820).
  private void updateFloatingToolbarVisibility(MotionEvent event) {
    if (mEditor.getTextActionMode() != null) {
      switch (event.getActionMasked()) {
        case MotionEvent.ACTION_MOVE:
          mEditor.hideFloatingToolbar(ActionMode.DEFAULT_HIDE_DURATION);
          break;
        case MotionEvent.ACTION_UP: // fall through
        case MotionEvent.ACTION_CANCEL:
          mEditor.showFloatingToolbar();
      }
    }
  }

  public boolean isDragging() {
    return mIsDragging;
  }

  void onHandleMoved() {}

  // Ported from AOSP Editor.HandleView.onDetached() (Editor.java:5805-5807).
  // Only dismisses the magnifier. Does NOT unregister the position listener —
  // that is done separately in hide() so that dismiss() (used when the handle
  // scrolls out of view) can keep the position listener active for re-showing.
  public void onDetached() {
    dismissMagnifier();
  }

  /**
   * Registers an OnPreDrawListener to reposition the handle on every draw frame. This ensures
   * handles follow the text view during scrolling, animation, or layout changes. Matches AOSP's
   * TextViewPositionListener behavior where the PositionListener calls
   * HandleView.updatePosition(parentX, parentY, changed, scrolled) on each frame.
   */
  private void registerPositionListener() {
    if (mPositionListener != null) {
      return;
    }
    mPositionListener =
        () -> {
          // Compute parent position in window (matches AOSP PositionListener behavior)
          final int[] parentPos = new int[2];
          mTextView.getLocationInWindow(parentPos);
          boolean parentPositionChanged =
              parentPos[0] != mLastParentX || parentPos[1] != mLastParentY;
          // Call the full updatePosition which handles showing/hiding/updating the popup
          updatePosition(parentPos[0], parentPos[1], parentPositionChanged, false);
          return true;
        };
    mTextView.getViewTreeObserver().addOnPreDrawListener(mPositionListener);
  }

  private void unregisterPositionListener() {
    if (mPositionListener != null) {
      mTextView.getViewTreeObserver().removeOnPreDrawListener(mPositionListener);
      mPositionListener = null;
    }
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      setSystemGestureExclusionRects(Collections.singletonList(new Rect(0, 0, w, h)));
    }
  }

  protected abstract int getMagnifierHandleTrigger();

  @Nullable
  MagnifierMotionAnimator getMagnifierAnimator() {
    return mMagnifierAnimator;
  }

  /** Constants for magnifier handle triggers. Copied from AOSP Editor.java:215-222. */
  static class MagnifierHandleTrigger {
    static final int INSERTION = 0;
    static final int SELECTION_START = 1;
    static final int SELECTION_END = 2;
  }

  /**
   * Wraps a Magnifier with smooth animation on line jumps.
   *
   * <p>Ported from AOSP {@code Editor.MagnifierMotionAnimator} (Editor.java:4966-5046).
   *
   * <p>Requires API 28 (P) because {@link Magnifier} was introduced in that version. This class is
   * only instantiated within {@code Build.VERSION.SDK_INT >= Build.VERSION_CODES.P} guards in
   * {@link RCHandleView}, so it is never loaded on older devices. On pre-28 devices, handle
   * dragging works normally but without the magnifier loupe effect.
   */
  @RequiresApi(Build.VERSION_CODES.P)
  static class MagnifierMotionAnimator {
    private static final long DURATION = 100;

    final Magnifier mMagnifier;
    private final ValueAnimator mAnimator;

    boolean mMagnifierIsShowing;
    private float mAnimationStartX;
    private float mAnimationStartY;
    private float mAnimationCurrentX;
    private float mAnimationCurrentY;
    private float mLastX;
    private float mLastY;

    MagnifierMotionAnimator(final Magnifier magnifier) {
      mMagnifier = magnifier;
      mAnimator = ValueAnimator.ofFloat(0, 1);
      mAnimator.setDuration(DURATION);
      mAnimator.setInterpolator(new LinearInterpolator());
      mAnimator.addUpdateListener(
          (animation) -> {
            mAnimationCurrentX =
                mAnimationStartX + (mLastX - mAnimationStartX) * animation.getAnimatedFraction();
            mAnimationCurrentY =
                mAnimationStartY + (mLastY - mAnimationStartY) * animation.getAnimatedFraction();
            mMagnifier.show(mAnimationCurrentX, mAnimationCurrentY);
          });
    }

    void show(final float x, final float y) {
      final boolean startNewAnimation = mMagnifierIsShowing && y != mLastY;

      if (startNewAnimation) {
        if (mAnimator.isRunning()) {
          mAnimator.cancel();
          mAnimationStartX = mAnimationCurrentX;
          mAnimationStartY = mAnimationCurrentY;
        } else {
          mAnimationStartX = mLastX;
          mAnimationStartY = mLastY;
        }
        mAnimator.start();
      } else {
        if (!mAnimator.isRunning()) {
          mMagnifier.show(x, y);
        }
      }
      mLastX = x;
      mLastY = y;
      mMagnifierIsShowing = true;
    }

    void dismiss() {
      mMagnifier.dismiss();
      mAnimator.cancel();
      mMagnifierIsShowing = false;
    }
  }
}
