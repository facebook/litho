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
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.view.Gravity;
import android.view.MotionEvent;
import com.facebook.rendercore.text.RCTextView;

/**
 * Concrete selection handle view for start or end of selection.
 *
 * <p>Ported from AOSP {@code Editor.SelectionHandleView} (Editor.java:6164-6555). Uses
 * character-level selection during handle drag, with handle crossing prevention and magnifier
 * integration. Note that native AOSP does word-boundary snapping during handle drag, but this port
 * intentionally simplifies to character-level positioning only. Word-level selection is handled
 * during the initial long-press drag by {@link RCSelectionController}.
 *
 * <p>Key differences from AOSP:
 *
 * <ul>
 *   <li>Removed flags for cursor start float (not needed for read-only text)
 * </ul>
 */
public class RCSelectionHandleView extends RCHandleView {

  static final int HANDLE_TYPE_SELECTION_START = 0;
  static final int HANDLE_TYPE_SELECTION_END = 1;

  private static final int UNSET_LINE = -1;

  private final int mHandleType;

  RCSelectionHandleView(
      RCTextView textView,
      RCTextEditor editor,
      Drawable drawableLtr,
      Drawable drawableRtl,
      int handleType) {
    super(textView, editor, drawableLtr, drawableRtl);
    mHandleType = handleType;
  }

  boolean isStartHandle() {
    return mHandleType == HANDLE_TYPE_SELECTION_START;
  }

  // Ported from AOSP Editor.SelectionHandleView.getHotspotX() (Editor.java:6196-6203).
  @Override
  protected int getHotspotX(Drawable drawable, boolean isRtlRun) {
    if (isRtlRun == isStartHandle()) {
      return drawable.getIntrinsicWidth() / 4;
    } else {
      return (drawable.getIntrinsicWidth() * 3) / 4;
    }
  }

  // Ported from AOSP Editor.SelectionHandleView.getHorizontalGravity() (Editor.java:6205-6207).
  @Override
  protected int getHorizontalGravity(boolean isRtlRun) {
    return (isRtlRun == isStartHandle()) ? Gravity.LEFT : Gravity.RIGHT;
  }

  // Ported from AOSP Editor.SelectionHandleView.getCurrentCursorOffset() (Editor.java:6210-6212).
  @Override
  int getCurrentCursorOffset() {
    return isStartHandle() ? mEditor.getSelectionStart() : mEditor.getSelectionEnd();
  }

  // Ported from AOSP Editor.SelectionHandleView.updateSelection() (Editor.java:6215-6227).
  @Override
  protected void updateSelection(int offset) {
    final Spannable spannable = mEditor.getSpannable();
    if (spannable == null) return;
    if (isStartHandle()) {
      Selection.setSelection(spannable, offset, mEditor.getSelectionEnd());
    } else {
      Selection.setSelection(spannable, mEditor.getSelectionStart(), offset);
    }
    updateDrawable(false /* updateDrawableWhenDragging */);
    mEditor.invalidateActionMode();
  }

  // Ported from AOSP Editor.SelectionHandleView.updatePosition() (Editor.java:6230-6403).
  // Uses getCurrentLineAdjustedForSlop() for vertical hysteresis matching AOSP behavior.
  //
  // NOTE: This is intentionally simplified compared to AOSP. The original AOSP implementation
  // (Editor.java:6230-6403, ~170 lines) includes word-boundary snapping during handle drag,
  // BiDi/RTL run handling, horizontal scroll edge detection, and expand/shrink hysteresis.
  // This port uses character-level positioning only, which is simpler but means dragging
  // selection handles does not snap to word boundaries like stock Android.
  // Word-level selection is handled during the initial long-press drag by RCSelectionController.
  // We may revisit this to add word-snapping during handle drag if needed in the future.
  @Override
  protected void updatePosition(float x, float y, boolean fromTouchScreen) {
    final Layout layout = mTextView.getLayout();
    if (layout == null) {
      positionAndAdjustForCrossingHandles(mEditor.getOffsetForPosition(x, y), fromTouchScreen);
      return;
    }

    if (mPreviousLineTouched == UNSET_LINE) {
      mPreviousLineTouched = mEditor.getLineAtCoordinate(y);
    }

    final int anotherHandleOffset =
        isStartHandle() ? mEditor.getSelectionEnd() : mEditor.getSelectionStart();
    int currLine = mEditor.getCurrentLineAdjustedForSlop(layout, mPreviousLineTouched, y);
    int offset = getOffsetAtCoordinate(layout, currLine, x);

    if ((isStartHandle() && offset >= anotherHandleOffset)
        || (!isStartHandle() && offset <= anotherHandleOffset)) {
      // Handles have crossed, bound it to the first selected line and
      // adjust by char as normal.
      currLine = getLineForOffset(layout, anotherHandleOffset);
      offset = getOffsetAtCoordinate(layout, currLine, x);
    }

    mPreviousLineTouched = currLine;
    positionAndAdjustForCrossingHandles(offset, fromTouchScreen);
  }

  // Ported from AOSP Editor.SelectionHandleView.onTouchEvent() (Editor.java:6413-6439).
  // Adds magnifier show/dismiss around the base class touch handling.
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    boolean superResult = super.onTouchEvent(event);

    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        updateMagnifier(event);
        break;

      case MotionEvent.ACTION_MOVE:
        updateMagnifier(event);
        break;

      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        dismissMagnifier();
        break;
    }

    return superResult;
  }

  // Ported from AOSP Editor.SelectionHandleView.positionAndAdjustForCrossingHandles()
  // (Editor.java:6441-6478). Prevents handles from crossing each other.
  //
  // TODO: AOSP has additional BiDi run boundary detection here (Editor.java:6448-6472).
  // When handles cross a direction boundary in mixed LTR/RTL text, AOSP uses
  // layout.getRunRange() to keep the handle at the run boundary instead of
  // collapsing the selection. This port currently does simple snapping to one
  // character past the other handle, which may cause unexpected behavior in BiDi text.
  private void positionAndAdjustForCrossingHandles(int offset, boolean fromTouchScreen) {
    final int anotherHandleOffset =
        isStartHandle() ? mEditor.getSelectionEnd() : mEditor.getSelectionStart();
    if ((isStartHandle() && offset >= anotherHandleOffset)
        || (!isStartHandle() && offset <= anotherHandleOffset)) {
      // Handles can not cross and selection is at least one character.
      offset = mEditor.getNextCursorOffset(anotherHandleOffset, !isStartHandle());
    }
    positionAtCursorOffset(offset, false, fromTouchScreen);
  }

  @Override
  protected int getMagnifierHandleTrigger() {
    return isStartHandle()
        ? MagnifierHandleTrigger.SELECTION_START
        : MagnifierHandleTrigger.SELECTION_END;
  }
}
