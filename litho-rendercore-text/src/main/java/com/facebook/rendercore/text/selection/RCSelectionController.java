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

import android.graphics.drawable.Drawable;
import android.text.Selection;
import android.text.Spannable;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import com.facebook.rendercore.text.RCTextView;

/**
 * Controller for selection gesture lifecycle.
 *
 * <p>Ported from AOSP {@code Editor.SelectionModifierCursorController} (Editor.java:6871-7295).
 * Manages start/end selection handles, drag accelerator modes (word/character/paragraph), and
 * double-tap/triple-click word/paragraph selection.
 *
 * <p>Key differences from AOSP:
 *
 * <ul>
 *   <li>Removed insertion cursor controller logic (we only support selection)
 *   <li>No cursor drag direction tracking (read-only text doesn't need it)
 * </ul>
 */
public class RCSelectionController implements ViewTreeObserver.OnTouchModeChangeListener {

  // Drag accelerator modes — copied from AOSP Editor.java:6890-6898.
  private static final int DRAG_ACCELERATOR_MODE_INACTIVE = 0;
  private static final int DRAG_ACCELERATOR_MODE_CHARACTER = 1;
  private static final int DRAG_ACCELERATOR_MODE_WORD = 2;
  private static final int DRAG_ACCELERATOR_MODE_PARAGRAPH = 3;

  private final RCTextView mTextView;
  private final RCTextEditor mEditor;

  // The cursor controller handles, lazily created when shown.
  public RCSelectionHandleView mStartHandle;
  public RCSelectionHandleView mEndHandle;

  // The offsets of that last touch down event. Remembered to start selection there.
  private int mMinTouchOffset;
  private int mMaxTouchOffset;

  private boolean mGestureStayedInTapRegion;

  // Where the user first starts the drag motion.
  private int mStartOffset = -1;

  private boolean mHaventMovedEnoughToStartDrag;
  // The line that a selection happened most recently with the drag accelerator.
  private int mLineSelectionIsOn = -1;
  // Whether the drag accelerator has selected past the initial line.
  private boolean mSwitchedLines = false;

  // Indicates the drag accelerator mode that the user is currently using.
  private int mDragAcceleratorMode = DRAG_ACCELERATOR_MODE_INACTIVE;

  @SuppressWarnings("this-escape")
  public RCSelectionController(RCTextView textView, RCTextEditor editor) {
    mTextView = textView;
    mEditor = editor;
    resetTouchOffsets();
  }

  public void show() {
    initHandles();
    // Register touch mode listener so handles are hidden when switching to non-touch mode
    // (e.g. keyboard/trackball). Ported from AOSP SelectionModifierCursorController.show().
    mTextView.getViewTreeObserver().addOnTouchModeChangeListener(this);
  }

  private void initHandles() {
    final Drawable selectHandleLeft = mEditor.getSelectHandleLeft();
    final Drawable selectHandleRight = mEditor.getSelectHandleRight();
    if (selectHandleLeft == null || selectHandleRight == null) {
      return;
    }

    // Lazy object creation has to be done before updatePosition() is called.
    if (mStartHandle == null) {
      mStartHandle =
          new RCSelectionHandleView(
              mTextView,
              mEditor,
              selectHandleLeft,
              selectHandleRight,
              RCSelectionHandleView.HANDLE_TYPE_SELECTION_START);
    }
    if (mEndHandle == null) {
      mEndHandle =
          new RCSelectionHandleView(
              mTextView,
              mEditor,
              selectHandleRight,
              selectHandleLeft,
              RCSelectionHandleView.HANDLE_TYPE_SELECTION_END);
    }

    mStartHandle.show();
    mEndHandle.show();
  }

  public void hide() {
    if (mStartHandle != null) mStartHandle.hide();
    if (mEndHandle != null) mEndHandle.hide();
  }

  // Ported from AOSP Editor.SelectionModifierCursorController.enterDrag() (Editor.java:6946-6967).
  void enterDrag(int dragAcceleratorMode) {
    // Just need to init the handles.
    show();
    mDragAcceleratorMode = dragAcceleratorMode;
    // Start location of selection.
    mStartOffset =
        mEditor.getOffsetForPosition(
            mEditor.getTouchState().getLastDownX(), mEditor.getTouchState().getLastDownY());
    mLineSelectionIsOn = mEditor.getLineAtCoordinate(mEditor.getTouchState().getLastDownY());
    // Don't show the handles until user has lifted finger.
    hide();

    // This stops scrolling parents from intercepting the touch event.
    final ViewParent parent = mTextView.getParent();
    if (parent != null) {
      parent.requestDisallowInterceptTouchEvent(true);
    }
    mTextView.cancelLongPress();
  }

  // Ported from AOSP Editor.SelectionModifierCursorController.onTouchEvent()
  // (Editor.java:6969-7085). Simplified: removed insertion-cursor and cursor-drag logic.
  public void onTouchEvent(MotionEvent event) {
    final float eventX = event.getX();
    final float eventY = event.getY();
    final boolean isMouse = event.isFromSource(InputDevice.SOURCE_MOUSE);
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        // Remember finger down position, to be able to start selection from there.
        mMinTouchOffset = mMaxTouchOffset = mEditor.getOffsetForPosition(eventX, eventY);

        // Double tap detection
        if (mGestureStayedInTapRegion
            && mEditor.getTouchState().isMultiTapInSameArea()
            && (isMouse
                || mEditor.isPositionOnText(eventX, eventY)
                || mEditor.getTouchState().isOnHandle())) {
          if (mEditor.getTouchState().isDoubleTap()) {
            selectCurrentWordAndStartDrag();
          } else if (mEditor.getTouchState().isTripleClick()) {
            selectCurrentParagraphAndStartDrag();
          }
          mEditor.setDiscardNextActionUp(true);
        }
        mGestureStayedInTapRegion = true;
        mHaventMovedEnoughToStartDrag = true;
        break;

      case MotionEvent.ACTION_POINTER_DOWN:
      case MotionEvent.ACTION_POINTER_UP:
        updateMinAndMaxOffsets(event);
        break;

      case MotionEvent.ACTION_MOVE:
        if (mGestureStayedInTapRegion) {
          final ViewConfiguration viewConfig = ViewConfiguration.get(mTextView.getContext());
          mGestureStayedInTapRegion =
              RCEditorTouchState.isDistanceWithin(
                  mEditor.getTouchState().getLastDownX(),
                  mEditor.getTouchState().getLastDownY(),
                  eventX,
                  eventY,
                  viewConfig.getScaledDoubleTapSlop());
        }

        if (mHaventMovedEnoughToStartDrag) {
          mHaventMovedEnoughToStartDrag = !mEditor.getTouchState().isMovedEnoughForDrag();
        }

        if (isMouse && !isDragAcceleratorActive()) {
          final int offset = mEditor.getOffsetForPosition(eventX, eventY);
          if (mStartOffset != offset) {
            // Start character based drag accelerator.
            mEditor.stopTextActionMode();
            enterDrag(DRAG_ACCELERATOR_MODE_CHARACTER);
            mEditor.setDiscardNextActionUp(true);
            mHaventMovedEnoughToStartDrag = false;
          }
        }

        if (mStartHandle != null && mStartHandle.isShowing()) {
          // Don't do the drag if the handles are showing already.
          break;
        }

        updateSelection(event);
        if (mEditor.hasSelection() && mEndHandle != null && isDragAcceleratorActive()) {
          mEndHandle.updateMagnifier(event);
        }
        break;

      case MotionEvent.ACTION_UP:
        if (mEndHandle != null) {
          mEndHandle.dismissMagnifier();
        }
        if (!isDragAcceleratorActive()) {
          break;
        }
        updateSelection(event);

        // No longer dragging to select text, let the parent intercept events.
        final ViewParent parentOnUp = mTextView.getParent();
        if (parentOnUp != null) {
          parentOnUp.requestDisallowInterceptTouchEvent(false);
        }

        // No longer the first dragging motion, reset.
        resetDragAcceleratorState();

        if (mEditor.hasSelection()) {
          mEditor.startSelectionActionMode(mHaventMovedEnoughToStartDrag);
        }
        break;
    }
  }

  // Ported from AOSP Editor.SelectionModifierCursorController — dispatches to character/word/
  // paragraph update methods based on drag accelerator mode (Editor.java:7087-7101).
  private void updateSelection(MotionEvent event) {
    if (mTextView.getLayout() != null) {
      switch (mDragAcceleratorMode) {
        case DRAG_ACCELERATOR_MODE_CHARACTER:
          updateCharacterBasedSelection(event);
          break;
        case DRAG_ACCELERATOR_MODE_WORD:
          updateWordBasedSelection(event);
          break;
        case DRAG_ACCELERATOR_MODE_PARAGRAPH:
          updateParagraphBasedSelection(event);
          break;
      }
    }
  }

  /**
   * Selects the current word and starts drag for word-based selection. Called on long-press or
   * double-tap.
   */
  // Ported from AOSP Editor.selectCurrentWordAndStartDrag() (Editor.java:2568-2586).
  public boolean selectCurrentWordAndStartDrag() {
    mEditor.stopTextActionModeWithPreservingSelection();
    if (!mEditor.hasSelection() && !mEditor.selectCurrentWord()) {
      return false;
    }
    enterDrag(DRAG_ACCELERATOR_MODE_WORD);
    return true;
  }

  /** Selects the current paragraph and starts drag. Called on triple-click (mouse). */
  // Ported from AOSP Editor.SelectionModifierCursorController.selectCurrentParagraphAndStartDrag()
  // (Editor.java:7108-7118).
  private boolean selectCurrentParagraphAndStartDrag() {
    mEditor.stopTextActionMode();
    if (!mEditor.selectCurrentParagraph()) {
      return false;
    }
    enterDrag(DRAG_ACCELERATOR_MODE_PARAGRAPH);
    return true;
  }

  // Ported from AOSP Editor.SelectionModifierCursorController — DRAG_ACCELERATOR_MODE_CHARACTER
  // case (Editor.java:7119-7123).
  private void updateCharacterBasedSelection(MotionEvent event) {
    final int offset = mEditor.getOffsetForPosition(event.getX(), event.getY());
    updateSelectionInternal(
        mStartOffset, offset, event.isFromSource(InputDevice.SOURCE_TOUCHSCREEN));
  }

  // Ported from AOSP Editor.SelectionModifierCursorController — DRAG_ACCELERATOR_MODE_WORD
  // case (Editor.java:7125-7183). Includes line-switching finger offset logic.
  private void updateWordBasedSelection(MotionEvent event) {
    if (mHaventMovedEnoughToStartDrag) {
      return;
    }
    final boolean isMouse = event.isFromSource(InputDevice.SOURCE_MOUSE);
    final ViewConfiguration viewConfig = ViewConfiguration.get(mTextView.getContext());
    final float eventX = event.getX();
    final float eventY = event.getY();
    final int currLine;
    if (isMouse) {
      currLine = mEditor.getLineAtCoordinate(eventY);
    } else {
      float y = eventY;
      if (mSwitchedLines) {
        // Offset the finger by the same vertical offset as the handles.
        final int touchSlop = viewConfig.getScaledTouchSlop();
        final float fingerOffset =
            (mStartHandle != null) ? mStartHandle.getIdealVerticalOffset() : touchSlop;
        y = eventY - fingerOffset;
      }

      currLine =
          mEditor.getCurrentLineAdjustedForSlop(mTextView.getLayout(), mLineSelectionIsOn, y);
      if (!mSwitchedLines && currLine != mLineSelectionIsOn) {
        mSwitchedLines = true;
        return;
      }
    }

    int startOffset;
    int offset = mEditor.getOffsetAtCoordinate(currLine, eventX);
    // Snap to word boundaries.
    if (mStartOffset < offset) {
      // Expanding with end handle.
      offset = mEditor.getWordEnd(offset);
      startOffset = mEditor.getWordStart(mStartOffset);
    } else {
      // Expanding with start handle.
      offset = mEditor.getWordStart(offset);
      startOffset = mEditor.getWordEnd(mStartOffset);
      if (startOffset == offset) {
        offset = mEditor.getNextCursorOffset(offset, false);
      }
    }
    mLineSelectionIsOn = currLine;
    updateSelectionInternal(
        startOffset, offset, event.isFromSource(InputDevice.SOURCE_TOUCHSCREEN));
  }

  // Ported from AOSP Editor.SelectionModifierCursorController — DRAG_ACCELERATOR_MODE_PARAGRAPH
  // case (Editor.java:7186-7195). Delegates to getParagraphsRange().
  private void updateParagraphBasedSelection(MotionEvent event) {
    final int offset = mEditor.getOffsetForPosition(event.getX(), event.getY());

    final int start = Math.min(offset, mStartOffset);
    final int end = Math.max(offset, mStartOffset);
    final long paragraphsRange = mEditor.getParagraphsRange(start, end);
    final int selectionStart = (int) (paragraphsRange >>> 32);
    final int selectionEnd = (int) (paragraphsRange & 0xFFFFFFFFL);
    updateSelectionInternal(
        selectionStart, selectionEnd, event.isFromSource(InputDevice.SOURCE_TOUCHSCREEN));
  }

  private void updateSelectionInternal(
      int selectionStart, int selectionEnd, boolean fromTouchScreen) {
    final Spannable spannable = mEditor.getSpannable();
    if (spannable == null) return;
    final int oldSelStart = mEditor.getSelectionStart();
    final int oldSelEnd = mEditor.getSelectionEnd();
    Selection.setSelection(spannable, selectionStart, selectionEnd);
    if (fromTouchScreen && (oldSelStart != selectionStart || oldSelEnd != selectionEnd)) {
      mTextView.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE);
    }
  }

  // Ported from AOSP Editor.SelectionModifierCursorController.updateMinAndMaxOffsets()
  // (Editor.java:7211-7218).
  private void updateMinAndMaxOffsets(MotionEvent event) {
    int pointerCount = event.getPointerCount();
    for (int index = 0; index < pointerCount; index++) {
      int offset = mEditor.getOffsetForPosition(event.getX(index), event.getY(index));
      if (offset < mMinTouchOffset) mMinTouchOffset = offset;
      if (offset > mMaxTouchOffset) mMaxTouchOffset = offset;
    }
  }

  public int getMinTouchOffset() {
    return mMinTouchOffset;
  }

  public int getMaxTouchOffset() {
    return mMaxTouchOffset;
  }

  public void resetTouchOffsets() {
    mMinTouchOffset = mMaxTouchOffset = -1;
    resetDragAcceleratorState();
  }

  // Ported from AOSP Editor.SelectionModifierCursorController.resetDragAcceleratorState()
  // (Editor.java:7233-7244).
  private void resetDragAcceleratorState() {
    mStartOffset = -1;
    mDragAcceleratorMode = DRAG_ACCELERATOR_MODE_INACTIVE;
    mSwitchedLines = false;
    final Spannable spannable = mEditor.getSpannable();
    if (spannable == null) return;
    final int selectionStart = mEditor.getSelectionStart();
    final int selectionEnd = mEditor.getSelectionEnd();
    if (selectionStart < 0 || selectionEnd < 0) {
      Selection.removeSelection(spannable);
    } else if (selectionStart > selectionEnd) {
      Selection.setSelection(spannable, selectionEnd, selectionStart);
    }
  }

  public boolean isSelectionStartDragged() {
    return mStartHandle != null && mStartHandle.isDragging();
  }

  public boolean isCursorBeingModified() {
    return isDragAcceleratorActive()
        || isSelectionStartDragged()
        || (mEndHandle != null && mEndHandle.isDragging());
  }

  public boolean isDragAcceleratorActive() {
    return mDragAcceleratorMode != DRAG_ACCELERATOR_MODE_INACTIVE;
  }

  @Override
  public void onTouchModeChanged(boolean isInTouchMode) {
    if (!isInTouchMode) {
      hide();
    }
  }

  public void onDetached() {
    final ViewTreeObserver observer = mTextView.getViewTreeObserver();
    observer.removeOnTouchModeChangeListener(this);

    if (mStartHandle != null) mStartHandle.onDetached();
    if (mEndHandle != null) mEndHandle.onDetached();
  }

  public boolean isActive() {
    return mStartHandle != null && mStartHandle.isShowing();
  }

  public void invalidateHandles() {
    if (mStartHandle != null) {
      mStartHandle.invalidate();
    }
    if (mEndHandle != null) {
      mEndHandle.invalidate();
    }
  }
}
