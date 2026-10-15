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

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Layout;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.style.URLSpan;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.facebook.rendercore.text.RCTextView;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Central editor class for text selection in RCTextView.
 *
 * <p>Ported from relevant parts of AOSP {@code Editor.java} for read-only selection support.
 * Manages selection state, word/paragraph selection, action mode (floating toolbar), and
 * coordinates handle views and selection controller.
 *
 * <p>Key differences from AOSP:
 *
 * <ul>
 *   <li>No editable text support (no InputConnection, no SpellChecker, no UndoManager)
 *   <li>No insertion cursor — only selection handles
 *   <li>No drag-and-drop of selected text
 *   <li>Simplified action mode with Copy, Select All, Share, and PROCESS_TEXT only
 * </ul>
 */
public class RCTextEditor {

  private static final int ID_COPY = android.R.id.copy;
  private static final int ID_SELECT_ALL = android.R.id.selectAll;
  private static final int ID_SHARE = View.generateViewId();

  // Ported from AOSP Editor.java — ordering for PROCESS_TEXT menu items in the floating toolbar.
  private static final int ACTION_MODE_MENU_ITEM_ORDER_PROCESS_TEXT_INTENT_ACTIONS_START = 100;

  // Ported from AOSP TextView.PROCESS_TEXT_REQUEST_CODE.
  private static final int PROCESS_TEXT_REQUEST_CODE = 100;

  // Maximum characters to include in intent extras to avoid TransactionTooLargeException.
  // Mirrors AOSP's TextUtils.trimToParcelableSize() which caps at
  // IBinder.getSuggestedMaxIpcSizeBytes() / 2. Using a conservative 260,000 chars (~520KB in
  // UTF-16) to stay under the Binder transaction limit.
  static final int MAX_PARCELABLE_TEXT_LENGTH = 260_000;

  private final RCTextView mTextView;
  private final RCEditorTouchState mTouchState;
  private final ProcessTextIntentActionsHandler mProcessTextIntentActionsHandler;

  @Nullable private RCSelectionController mSelectionController;
  @Nullable private ActionMode mTextActionMode;
  @Nullable private Spannable mSpannable;

  private boolean mDiscardNextActionUp;

  // Transient flag: when true, selection is preserved in onDestroyActionMode (not collapsed).
  // Ported from AOSP Editor.mPreserveSelection. Only true during the synchronous
  // mTextActionMode.finish() call inside stopTextActionModeWithPreservingSelection().
  private boolean mPreserveSelection;

  // When true, the action mode should be restarted on the next refreshTextActionMode() call.
  // Ported from AOSP Editor.mRestartActionModeOnNextRefresh.
  private boolean mRestartActionModeOnNextRefresh;

  // Handle drawables
  @Nullable private Drawable mSelectHandleLeft;
  @Nullable private Drawable mSelectHandleRight;

  // WordIterator for word boundary detection
  @Nullable private RCWordIterator mWordIterator;
  @Nullable private RCWordIterator mWordIteratorWithText;
  private boolean mUpdateWordIteratorText = true;

  // Runnable to re-show the floating toolbar after a short delay, matching AOSP behavior.
  // Calls hide(0) to cancel any pending hide duration and make the toolbar visible again.
  // API behavior:
  //   API 23+ (M): Uses ActionMode.hide(0) to re-show the floating toolbar.
  //   Pre-23: No-op. ActionMode.hide() is not available, so the toolbar remains in its
  //           default state (always visible as a standard action bar).
  private final Runnable mShowFloatingToolbar =
      () -> {
        if (mTextActionMode != null) {
          // ActionMode.hide() requires API 23 (M).
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mTextActionMode.hide(0); // hide(0) cancels previous hide and re-shows toolbar
          }
        }
      };

  // Line slop for stable line tracking during handle drag — ported from AOSP
  // Editor.java:506-507 (WidgetFlags.LINE_SLOP_RATIO_DEFAULT).
  private static final float LINE_SLOP_RATIO = 0.5f;
  private static final int LINE_CHANGE_SLOP_MIN_DP = 8;
  private static final int LINE_CHANGE_SLOP_MAX_DP = 45;

  // Span watcher for selection change notifications
  @Nullable private SelectionSpanWatcher mSelectionSpanWatcher;

  @SuppressWarnings("this-escape")
  public RCTextEditor(RCTextView textView) {
    mTextView = textView;
    mTouchState = new RCEditorTouchState();
    mProcessTextIntentActionsHandler = new ProcessTextIntentActionsHandler(this);
  }

  public void setSpannable(@Nullable Spannable spannable) {
    // Remove watcher from old spannable
    if (mSpannable != null && mSelectionSpanWatcher != null) {
      mSpannable.removeSpan(mSelectionSpanWatcher);
    }
    mSpannable = spannable;
    mUpdateWordIteratorText = true;
    // Add watcher to new spannable
    if (mSpannable != null) {
      if (mSelectionSpanWatcher == null) {
        mSelectionSpanWatcher = new SelectionSpanWatcher();
      }
      mSpannable.setSpan(
          mSelectionSpanWatcher, 0, mSpannable.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
    }
  }

  @Nullable
  public Spannable getSpannable() {
    return mSpannable;
  }

  public RCEditorTouchState getTouchState() {
    return mTouchState;
  }

  public int getSelectionStart() {
    if (mSpannable == null) return -1;
    return Selection.getSelectionStart(mSpannable);
  }

  public int getSelectionEnd() {
    if (mSpannable == null) return -1;
    return Selection.getSelectionEnd(mSpannable);
  }

  public boolean hasSelection() {
    final int selStart = getSelectionStart();
    final int selEnd = getSelectionEnd();
    return selStart >= 0 && selEnd >= 0 && selStart != selEnd;
  }

  // ---- Touch event handling ----

  public void onTouchEvent(MotionEvent event) {
    ViewConfiguration viewConfiguration = ViewConfiguration.get(mTextView.getContext());
    mTouchState.update(event, viewConfiguration);
    updateFloatingToolbarVisibility(event);

    final RCSelectionController selectionController = getSelectionController();
    if (selectionController != null) {
      selectionController.onTouchEvent(event);
    }
    // Note: onTouchUpEvent is NOT called here. It is called by RCTextView.onTouchEvent()
    // at the appropriate point after ClickableSpan handling (matching AOSP's flow where
    // touch-up is processed after the existing clickable span logic).
  }

  public void onTouchUpEvent(MotionEvent event) {
    if (mDiscardNextActionUp) {
      mDiscardNextActionUp = false;
      return;
    }

    // On touch-up with existing selection and no drag, collapse selection.
    if (hasSelection() && !isCursorBeingModified()) {
      final int offset = getOffsetForPosition(event.getX(), event.getY());
      Selection.setSelection(mSpannable, offset);
      stopTextActionMode();
    }
  }

  public boolean isCursorBeingModified() {
    return mSelectionController != null && mSelectionController.isCursorBeingModified();
  }

  public void setDiscardNextActionUp(boolean discard) {
    mDiscardNextActionUp = discard;
  }

  /** Returns true if the editor's discard-next-action-up flag is set. */
  public boolean isDiscardNextActionUp() {
    return mDiscardNextActionUp;
  }

  /** Clears the discard-next-action-up flag. */
  public void clearDiscardNextActionUp() {
    mDiscardNextActionUp = false;
  }

  /**
   * Returns true if a selection drag is currently active (the user is dragging to extend
   * selection). Used by RCTextView to decide whether touch events should be consumed.
   */
  public boolean isSelectionDragActive() {
    return mSelectionController != null && mSelectionController.isDragAcceleratorActive();
  }

  // ---- performLongClick: select word and show handles ----
  // Ported from AOSP Editor.performLongClick(boolean handled) (Editor.java:1483-1537).
  // Simplified: removed insertion-cursor and drag-and-drop branches (read-only text only).

  public boolean performLongClick(boolean handled) {
    if (mSpannable == null) return false;

    // For read-only selectable text, we skip the insertion-cursor and drag-and-drop
    // branches from AOSP (they're for editable text). We focus on the selection path.

    // If there's an existing action mode with selection, and the touch is within
    // the selection, we could start drag-and-drop. Since DnD is out of scope, we
    // instead just re-select the word at the new position.
    if (!handled && mTextActionMode != null) {
      stopTextActionMode();
      if (mSelectionController == null) {
        mSelectionController = new RCSelectionController(mTextView, this);
      }
      handled = mSelectionController.selectCurrentWordAndStartDrag();
    }

    // Start a new selection
    if (!handled) {
      if (!mTextView.requestFocus()) return false;
      if (mSelectionController == null) {
        mSelectionController = new RCSelectionController(mTextView, this);
      }
      handled = mSelectionController.selectCurrentWordAndStartDrag();
    }

    return handled;
  }

  // ---- Word/paragraph selection ----

  // Ported from AOSP Editor.selectCurrentWord() (Editor.java:1226-1272).
  public boolean selectCurrentWord() {
    if (mSpannable == null) return false;

    long lastTouchOffsets = getLastTouchOffsets();
    final int minOffset = (int) (lastTouchOffsets >>> 32);
    final int maxOffset = (int) (lastTouchOffsets & 0xFFFFFFFFL);

    if (minOffset < 0 || minOffset > mSpannable.length()) return false;
    if (maxOffset < 0 || maxOffset > mSpannable.length()) return false;

    int selectionStart;
    int selectionEnd;

    // If a URLSpan is found at that position, select it.
    URLSpan[] urlSpans = mSpannable.getSpans(minOffset, maxOffset, URLSpan.class);
    if (urlSpans.length >= 1) {
      URLSpan urlSpan = urlSpans[0];
      selectionStart = mSpannable.getSpanStart(urlSpan);
      selectionEnd = mSpannable.getSpanEnd(urlSpan);
    } else {
      final RCWordIterator wordIterator = getWordIterator();
      wordIterator.setCharSequence(mSpannable, minOffset, maxOffset);

      selectionStart = wordIterator.getBeginning(minOffset);
      selectionEnd = wordIterator.getEnd(maxOffset);

      if (selectionStart == BreakIterator.DONE
          || selectionEnd == BreakIterator.DONE
          || selectionStart == selectionEnd) {
        // Fallback to char cluster range.
        long range = getCharClusterRange(minOffset);
        selectionStart = (int) (range >>> 32);
        selectionEnd = (int) (range & 0xFFFFFFFFL);
      }
    }

    Selection.setSelection(mSpannable, selectionStart, selectionEnd);
    return selectionEnd > selectionStart;
  }

  // Ported from AOSP Editor.selectCurrentParagraph() (Editor.java:1278-1299).
  public boolean selectCurrentParagraph() {
    if (mSpannable == null) return false;

    long lastTouchOffsets = getLastTouchOffsets();
    final int minLastTouchOffset = (int) (lastTouchOffsets >>> 32);
    final int maxLastTouchOffset = (int) (lastTouchOffsets & 0xFFFFFFFFL);

    final long paragraphsRange = getParagraphsRange(minLastTouchOffset, maxLastTouchOffset);
    final int start = (int) (paragraphsRange >>> 32);
    final int end = (int) (paragraphsRange & 0xFFFFFFFFL);
    if (start < end) {
      Selection.setSelection(mSpannable, start, end);
      return true;
    }
    return false;
  }

  // Ported from AOSP Editor.getParagraphsRange() (Editor.java:1304-1336).
  public long getParagraphsRange(int startOffset, int endOffset) {
    final Layout layout = mTextView.getLayout();
    if (layout == null) {
      return packRangeInLong(-1, -1);
    }
    final CharSequence text = layout.getText();
    int minLine = layout.getLineForOffset(startOffset);
    // Search paragraph start.
    while (minLine > 0) {
      final int prevLineEndOffset = layout.getLineEnd(minLine - 1);
      if (text.charAt(prevLineEndOffset - 1) == '\n') {
        break;
      }
      minLine--;
    }
    int maxLine = layout.getLineForOffset(endOffset);
    // Search paragraph end.
    while (maxLine < layout.getLineCount() - 1) {
      final int lineEndOffset = layout.getLineEnd(maxLine);
      if (text.charAt(lineEndOffset - 1) == '\n') {
        break;
      }
      maxLine++;
    }
    return packRangeInLong(layout.getLineStart(minLine), layout.getLineEnd(maxLine));
  }

  // Ported from AOSP Editor.getWordStart() (Editor.java:1165-1180).
  public int getWordStart(int offset) {
    int retOffset = getWordIteratorWithText().prevBoundary(offset);
    if (getWordIteratorWithText().isOnPunctuation(retOffset)) {
      retOffset = getWordIteratorWithText().getPunctuationBeginning(offset);
    } else {
      retOffset = getWordIteratorWithText().getPrevWordBeginningOnTwoWordsBoundary(offset);
    }
    if (retOffset == BreakIterator.DONE) {
      return offset;
    }
    return retOffset;
  }

  // Ported from AOSP Editor.getWordEnd() (Editor.java:1182-1195).
  public int getWordEnd(int offset) {
    int retOffset = getWordIteratorWithText().nextBoundary(offset);
    if (getWordIteratorWithText().isAfterPunctuation(retOffset)) {
      retOffset = getWordIteratorWithText().getPunctuationEnd(offset);
    } else {
      retOffset = getWordIteratorWithText().getNextWordEndOnTwoWordBoundary(offset);
    }
    if (retOffset == BreakIterator.DONE) {
      return offset;
    }
    return retOffset;
  }

  public boolean isWordBoundary(int offset) {
    return getWordIteratorWithText().isBoundary(offset);
  }

  // Ported from AOSP Editor.getNextCursorOffset() (Editor.java:1370-1383).
  public int getNextCursorOffset(int offset, boolean findAfterGivenOffset) {
    final Layout layout = mTextView.getLayout();
    if (layout == null) return offset;
    if (findAfterGivenOffset == layout.isRtlCharAt(offset)) {
      return layout.getOffsetToLeftOf(offset);
    } else {
      return layout.getOffsetToRightOf(offset);
    }
  }

  // Ported from AOSP Editor.getCharClusterRange() (Editor.java:1385-1397).
  private long getCharClusterRange(int offset) {
    if (mSpannable == null) return packRangeInLong(offset, offset);
    final int textLength = mSpannable.length();
    if (offset < textLength) {
      final int clusterEndOffset = getNextCursorOffset(offset, true);
      return packRangeInLong(getNextCursorOffset(clusterEndOffset, false), clusterEndOffset);
    }
    if (offset - 1 >= 0) {
      final int clusterStartOffset = getNextCursorOffset(offset, false);
      return packRangeInLong(clusterStartOffset, getNextCursorOffset(clusterStartOffset, true));
    }
    return packRangeInLong(offset, offset);
  }

  // Ported from AOSP Editor.getLastTouchOffsets() (Editor.java:1556-1561).
  private long getLastTouchOffsets() {
    if (mSelectionController != null) {
      int minOffset = mSelectionController.getMinTouchOffset();
      int maxOffset = mSelectionController.getMaxTouchOffset();
      if (minOffset >= 0 && maxOffset >= 0) {
        return packRangeInLong(minOffset, maxOffset);
      }
    }
    return packRangeInLong(0, 0);
  }

  // ---- Position/offset helpers ----

  // Ported from AOSP TextView.getOffsetForPosition() (TextView.java:15766-15770).
  public int getOffsetForPosition(float x, float y) {
    final Layout layout = mTextView.getLayout();
    if (layout == null) return -1;
    final int line = getLineAtCoordinate(y);
    return getOffsetAtCoordinate(line, x);
  }

  // Ported from AOSP TextView.getLineAtCoordinate() (TextView.java:15784-15791).
  public int getLineAtCoordinate(float y) {
    y -= mTextView.getTotalPaddingTop();
    // Clamp the position to inside of the view.
    y = Math.max(0.0f, y);
    y = Math.min(mTextView.getHeight() - mTextView.getTotalPaddingBottom() - 1, y);
    y += mTextView.getScrollY();
    return mTextView.getLayout().getLineForVertical((int) y);
  }

  // Ported from AOSP TextView.getOffsetAtCoordinate() (TextView.java:15799-15803).
  public int getOffsetAtCoordinate(int line, float x) {
    x = convertToLocalHorizontalCoordinate(x);
    return mTextView.getLayout().getOffsetForHorizontal(line, x);
  }

  // Ported from AOSP TextView.convertToLocalHorizontalCoordinate() (TextView.java:15773-15781).
  private float convertToLocalHorizontalCoordinate(float x) {
    x -= mTextView.getTotalPaddingLeft();
    x = Math.max(0.0f, x);
    x = Math.min(mTextView.getWidth() - mTextView.getTotalPaddingRight() - 1, x);
    x += mTextView.getScrollX();
    return x;
  }

  // Ported from AOSP Editor.isPositionOnText() (Editor.java:1451-1460).
  public boolean isPositionOnText(float x, float y) {
    final Layout layout = mTextView.getLayout();
    if (layout == null) return false;

    final int line = getLineAtCoordinate(y);
    x = convertToLocalHorizontalCoordinate(x);
    return x >= layout.getLineLeft(line) && x <= layout.getLineRight(line);
  }

  // Ported from AOSP Editor.getCurrentLineAdjustedForSlop() (Editor.java:6564-6591).
  // Uses LINE_SLOP_RATIO matching AOSP's WidgetFlags.LINE_SLOP_RATIO_DEFAULT.
  public int getCurrentLineAdjustedForSlop(Layout layout, int prevLine, float y) {
    final int trueLine = getLineAtCoordinate(y);
    if (layout == null
        || prevLine >= layout.getLineCount()
        || layout.getLineCount() <= 0
        || prevLine < 0) {
      return trueLine;
    }

    if (Math.abs(trueLine - prevLine) >= 2) {
      return trueLine;
    }

    final float lineHeight = mTextView.getLineHeight();
    final float density = mTextView.getResources().getDisplayMetrics().density;
    final int slopMin = (int) (LINE_CHANGE_SLOP_MIN_DP * density);
    final int slopMax = (int) (LINE_CHANGE_SLOP_MAX_DP * density);
    int slop = (int) (LINE_SLOP_RATIO * lineHeight);
    slop = Math.max(slopMin, Math.min(slopMax, (int) lineHeight + slop)) - (int) lineHeight;
    slop = Math.max(0, slop);

    final float verticalOffset = viewportToContentVerticalOffset();
    if (trueLine > prevLine && y >= layout.getLineBottom(prevLine) + slop + verticalOffset) {
      return trueLine;
    }
    if (trueLine < prevLine && y <= layout.getLineTop(prevLine) - slop + verticalOffset) {
      return trueLine;
    }
    return prevLine;
  }

  private float viewportToContentVerticalOffset() {
    return mTextView.getTotalPaddingTop() - mTextView.getScrollY();
  }

  // ---- Action mode ----

  public void startSelectionActionMode(boolean adjustSelection) {
    if (mTextActionMode != null) {
      invalidateActionMode();
      return;
    }
    if (!hasSelection()) return;

    // API behavior:
    //   API 23+ (M): Uses ActionMode.Callback2 with TYPE_FLOATING for a floating context menu
    //     that tracks the selection bounds via onGetContentRect(). This is the standard modern
    //     text selection experience.
    //   Pre-23: Uses ActionMode.Callback (no Callback2) with the default action mode type,
    //     which shows a standard contextual action bar at the top of the screen. The action bar
    //     provides the same menu items (Copy, Select All, Share, Process Text) but does not
    //     float near the selection.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      ActionMode.Callback actionModeCallback = new SelectionActionModeCallback();
      mTextActionMode = mTextView.startActionMode(actionModeCallback, ActionMode.TYPE_FLOATING);
    } else {
      ActionMode.Callback actionModeCallback = new SelectionActionModeCallbackCompat();
      mTextActionMode = mTextView.startActionMode(actionModeCallback);
    }

    // Show selection handles after starting action mode.
    // Ported from AOSP: after startActionModeInternal, the selection controller's handles
    // are shown so the user can adjust the selection.
    final RCSelectionController selectionController = getSelectionController();
    if (selectionController != null && hasSelection()) {
      selectionController.show();
    }
  }

  public void stopTextActionMode() {
    if (mTextActionMode != null) {
      mTextActionMode.finish();
    }
  }

  // Ported from AOSP Editor.stopTextActionModeWithPreservingSelection() (Editor.java:2802-2809).
  // Wraps stopTextActionMode() with mPreserveSelection so that onDestroyActionMode does not
  // collapse the selection. Sets mRestartActionModeOnNextRefresh so the action mode can be
  // restarted later (e.g., when the window regains focus).
  public void stopTextActionModeWithPreservingSelection() {
    if (mTextActionMode != null) {
      mRestartActionModeOnNextRefresh = true;
    }
    mPreserveSelection = true;
    stopTextActionMode();
    mPreserveSelection = false;
  }

  public void invalidateActionMode() {
    if (mTextActionMode != null) {
      mTextActionMode.invalidate();
    }
  }

  @Nullable
  public ActionMode getTextActionMode() {
    return mTextActionMode;
  }

  // Ported from AOSP Editor.hideFloatingToolbar() (Editor.java:1823-1828).
  // API behavior:
  //   API 23+ (M): Hides the floating toolbar for the given duration (ms). The toolbar
  //     re-appears when mShowFloatingToolbar is posted after the delay.
  //   Pre-23: Only removes the pending mShowFloatingToolbar callback. The standard action bar
  //     cannot be temporarily hidden, so the toolbar stays visible during handle drags.
  public void hideFloatingToolbar(int duration) {
    if (mTextActionMode != null) {
      mTextView.removeCallbacks(mShowFloatingToolbar);
      // ActionMode.hide() requires API 23 (M).
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        mTextActionMode.hide(duration);
      }
    }
  }

  // Ported from AOSP Editor.showFloatingToolbar() (Editor.java:1830-1842).
  public void showFloatingToolbar() {
    if (mTextActionMode != null) {
      // Delay re-show so it doesn't interfere with click confirmations or double-clicks
      // that could dismiss the toolbar. Matches AOSP Editor.showFloatingToolbar().
      int delay = ViewConfiguration.getDoubleTapTimeout();
      mTextView.postDelayed(mShowFloatingToolbar, delay);
    }
  }

  // Ported from AOSP Editor.updateFloatingToolbarVisibility() (Editor.java:1810-1820).
  private void updateFloatingToolbarVisibility(MotionEvent event) {
    if (mTextActionMode != null) {
      switch (event.getActionMasked()) {
        case MotionEvent.ACTION_MOVE:
          hideFloatingToolbar(ActionMode.DEFAULT_HIDE_DURATION);
          break;
        case MotionEvent.ACTION_UP: // fall through
        case MotionEvent.ACTION_CANCEL:
          showFloatingToolbar();
      }
    }
  }

  // ---- Handle drawables ----

  public void loadHandleDrawables() {
    if (mSelectHandleLeft == null) {
      mSelectHandleLeft = loadHandleDrawableFromAttribute(android.R.attr.textSelectHandleLeft);
    }
    if (mSelectHandleRight == null) {
      mSelectHandleRight = loadHandleDrawableFromAttribute(android.R.attr.textSelectHandleRight);
    }
    final int handleColor = mTextView.getSelectionHandleColor();
    if (handleColor != Color.TRANSPARENT) {
      if (mSelectHandleLeft != null) {
        mSelectHandleLeft.mutate().setTint(handleColor);
      }
      if (mSelectHandleRight != null) {
        mSelectHandleRight.mutate().setTint(handleColor);
      }
    }
  }

  @Nullable
  private Drawable loadHandleDrawableFromAttribute(int attrId) {
    TypedValue typedValue = new TypedValue();
    if (mTextView.getContext().getTheme().resolveAttribute(attrId, typedValue, true)) {
      return mTextView.getContext().getDrawable(typedValue.resourceId);
    }
    return null;
  }

  @Nullable
  public Drawable getSelectHandleLeft() {
    loadHandleDrawables();
    return mSelectHandleLeft;
  }

  @Nullable
  public Drawable getSelectHandleRight() {
    loadHandleDrawables();
    return mSelectHandleRight;
  }

  // ---- Selection controller ----

  @Nullable
  public RCSelectionController getSelectionController() {
    if (mSelectionController == null) {
      mSelectionController = new RCSelectionController(mTextView, this);
    }
    return mSelectionController;
  }

  /** Returns the process text handler for accessibility integration. */
  public ProcessTextIntentActionsHandler getProcessTextIntentActionsHandler() {
    return mProcessTextIntentActionsHandler;
  }

  // ---- WordIterator ----

  private RCWordIterator getWordIterator() {
    if (mWordIterator == null) {
      mWordIterator = new RCWordIterator(mTextView.getTextServicesLocale());
    }
    return mWordIterator;
  }

  private RCWordIterator getWordIteratorWithText() {
    if (mWordIteratorWithText == null) {
      mWordIteratorWithText = new RCWordIterator(mTextView.getTextServicesLocale());
      mUpdateWordIteratorText = true;
    }
    if (mUpdateWordIteratorText) {
      if (mSpannable != null) {
        mWordIteratorWithText.setCharSequence(mSpannable, 0, mSpannable.length());
      }
      mUpdateWordIteratorText = false;
    }
    return mWordIteratorWithText;
  }

  // ---- Selection highlight for drawing ----

  private final Path mInteractiveSelectionPath = new Path();
  private final Paint mInteractiveSelectionPaint = new Paint();
  private boolean mInteractiveSelectionPathNeedsUpdate = true;

  /**
   * Returns the Path for the current interactive selection highlight. Used by RCTextView in
   * drawLayout() to render the selection background.
   */
  @Nullable
  public Path getSelectionHighlightPath() {
    if (!hasSelection()) return null;
    final Layout layout = mTextView.getLayout();
    if (layout == null) return null;

    if (mInteractiveSelectionPathNeedsUpdate) {
      mInteractiveSelectionPath.reset();
      final int start = getSelectionStart();
      final int end = getSelectionEnd();
      if (start >= 0 && end >= 0 && start != end) {
        layout.getSelectionPath(
            Math.min(start, end), Math.max(start, end), mInteractiveSelectionPath);
      }
      mInteractiveSelectionPathNeedsUpdate = false;
    }
    return mInteractiveSelectionPath;
  }

  /** Returns the Paint for the interactive selection highlight. */
  public Paint getSelectionHighlightPaint() {
    mInteractiveSelectionPaint.setColor(mTextView.getHighlightColor());
    return mInteractiveSelectionPaint;
  }

  /** Called when the selection changes to invalidate the cached selection path. */
  public void invalidateSelection() {
    mInteractiveSelectionPathNeedsUpdate = true;
    mTextView.invalidate();
  }

  /** Called during draw to ensure handles are properly positioned. */
  public void drawHandles(Canvas canvas) {
    // Handles are drawn via PopupWindow, not directly on the canvas.
    // Handle positioning is managed by the selection controller and SpanWatcher callbacks.
    // No action needed here since PopupWindow draws independently.
  }

  /**
   * Called from RCTextView.unmount() to clean up editor state. Unlike onDetachedFromWindow() which
   * is called by the system, this is called during the component lifecycle.
   */
  public void onDetach() {
    stopTextActionMode();
    mTextView.removeCallbacks(mShowFloatingToolbar);
    if (mSelectionController != null) {
      mSelectionController.hide();
      mSelectionController.onDetached();
    }
    if (mSpannable != null) {
      Selection.removeSelection(mSpannable);
    }
    mSpannable = null;
    mTextView.setHasTransientState(false);
  }

  // ---- Cleanup ----

  // Ported from AOSP Editor.onDetachedFromWindow() (Editor.java:760-785).
  // Hides handles and stops action mode while preserving selection.
  public void onDetachedFromWindow() {
    mTextView.removeCallbacks(mShowFloatingToolbar);
    if (mSelectionController != null) {
      mSelectionController.hide();
      mSelectionController.onDetached();
    }
    stopTextActionModeWithPreservingSelection();
  }

  // Ported from AOSP Editor.onWindowFocusChanged() (Editor.java:1729-1751).
  /**
   * Called when the window containing RCTextView gains or loses focus. When the window loses focus,
   * hides handles and stops the action mode while preserving the selection. When the window regains
   * focus and there is a selection with mRestartActionModeOnNextRefresh set, restarts the selection
   * action mode.
   */
  public void onWindowFocusChanged(boolean hasWindowFocus) {
    if (hasWindowFocus) {
      // Ported from AOSP Editor.onWindowFocusChanged(true) → refreshTextActionMode()
      // (Editor.java:1732-1734, 2446-2484). Simplified for read-only text: only handles
      // the selection restart path.
      if (hasSelection()) {
        if (mTextActionMode == null && mRestartActionModeOnNextRefresh) {
          startSelectionActionMode(false);
        }
      }
      mRestartActionModeOnNextRefresh = false;
    } else {
      // Ported from AOSP Editor.onWindowFocusChanged(false) (Editor.java:1740-1742).
      // hideCursorAndSpanControllers() hides the selection controller.
      if (mSelectionController != null) {
        mSelectionController.hide();
      }
      stopTextActionModeWithPreservingSelection();
    }
  }

  // ---- Select all (keyboard shortcut) ----

  public boolean selectAll() {
    if (mSpannable == null) {
      return false;
    }
    Selection.setSelection(mSpannable, 0, mSpannable.length());
    startSelectionActionMode(false);
    return true;
  }

  // ---- Copy ----

  public void copyToClipboard() {
    if (mSpannable == null) {
      return;
    }
    int start = getSelectionStart();
    int end = getSelectionEnd();
    if (start > end) {
      int tmp = start;
      start = end;
      end = tmp;
    }
    if (start < 0 || end < 0 || start == end) {
      return;
    }

    CharSequence selectedText = mSpannable.subSequence(start, end);
    ClipboardManager clipboard =
        (ClipboardManager) mTextView.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
    if (clipboard != null) {
      clipboard.setPrimaryClip(ClipData.newPlainText(null, selectedText));
    }

    // Collapse selection after copy
    Selection.setSelection(mSpannable, end);
    stopTextActionMode();
  }

  // ---- Share ----

  /**
   * Shares the currently selected text using the system share intent. Ported from AOSP
   * TextView.onTextContextMenuItem(ID_SHARE).
   */
  public void shareSelectedText() {
    if (mSpannable == null) return;
    int start = getSelectionStart();
    int end = getSelectionEnd();
    if (start > end) {
      int tmp = start;
      start = end;
      end = tmp;
    }
    if (start < 0 || end < 0 || start == end) return;

    String selectedText = mSpannable.subSequence(start, end).toString();
    // Trim to avoid TransactionTooLargeException, matching AOSP's
    // TextUtils.trimToParcelableSize() behavior in shareSelectedText().
    if (selectedText.length() > MAX_PARCELABLE_TEXT_LENGTH) {
      selectedText = selectedText.substring(0, MAX_PARCELABLE_TEXT_LENGTH);
    }
    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
    sharingIntent.setType("text/plain");
    sharingIntent.removeExtra(Intent.EXTRA_TEXT);
    sharingIntent.putExtra(Intent.EXTRA_TEXT, selectedText);
    mTextView.getContext().startActivity(Intent.createChooser(sharingIntent, null));
  }

  // ---- Get selected text ----

  /**
   * Returns the currently selected text, or null if there is no selection. Ported from AOSP
   * TextView.getSelectedText().
   */
  @Nullable
  public String getSelectedText() {
    if (!hasSelection() || mSpannable == null) {
      return null;
    }
    final int start = getSelectionStart();
    final int end = getSelectionEnd();
    return String.valueOf(
        start > end ? mSpannable.subSequence(end, start) : mSpannable.subSequence(start, end));
  }

  // ---- Helper ----

  // Ported from AOSP TextUtils.packRangeInLong() — packs two int offsets into a single long.
  private static long packRangeInLong(int start, int end) {
    return (((long) start) << 32) | (end & 0xFFFFFFFFL);
  }

  // ---- Action mode callback ----

  // Shared callback logic extracted for use by both API 23+ (Callback2) and pre-API 23 (Callback)
  // implementations. Ported from AOSP Editor.TextActionModeCallback (Editor.java:4683-4920).
  // Simplified for read-only text: only Copy, Select All, Share, and PROCESS_TEXT items.
  //
  // API behavior:
  //   API 23+ (M): SelectionActionModeCallback extends ActionMode.Callback2, which provides
  //     onGetContentRect() for positioning the floating toolbar near the selection. The toolbar
  //     floats and repositions dynamically as the selection changes.
  //   Pre-23: SelectionActionModeCallbackCompat implements ActionMode.Callback (the standard
  //     interface available since API 11). The same menu items are shown in a contextual action
  //     bar pinned to the top of the screen. onGetContentRect() is not available, so the bar
  //     does not track the selection position.

  private boolean handleCreateActionMode(ActionMode mode, Menu menu) {
    mode.setTitle(null);
    mode.setSubtitle(null);
    mode.setTitleOptionalHint(true);

    // Copy
    menu.add(Menu.NONE, ID_COPY, 0, android.R.string.copy)
        .setAlphabeticShortcut('c')
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

    // Select All
    if (mSpannable != null) {
      final int selStart = getSelectionStart();
      final int selEnd = getSelectionEnd();
      if (selStart > 0 || selEnd < mSpannable.length()) {
        menu.add(Menu.NONE, ID_SELECT_ALL, 1, android.R.string.selectAll)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
      }
    }

    // Share — ported from AOSP TextActionModeCallback.populateMenuWithItems()
    // com.android.internal.R.string.share is @hide, so look up by resource name.
    String shareLabel = "Share";
    int shareResId =
        mTextView.getContext().getResources().getIdentifier("share", "string", "android");
    if (shareResId != 0) {
      shareLabel = mTextView.getContext().getString(shareResId);
    }
    menu.add(Menu.NONE, ID_SHARE, 2, shareLabel).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

    // Process text intent actions — ported from AOSP Editor.TextActionModeCallback.
    // Adds menu items for installed apps that handle ACTION_PROCESS_TEXT (e.g., Translate,
    // Search).
    mProcessTextIntentActionsHandler.onInitializeMenu(menu);

    if (hasSelection() && !mTextView.hasTransientState()) {
      mTextView.setHasTransientState(true);
    }
    return true;
  }

  private boolean handlePrepareActionMode(ActionMode mode, Menu menu) {
    // Update select all availability
    final MenuItem selectAllItem = menu.findItem(ID_SELECT_ALL);
    if (mSpannable != null && selectAllItem != null) {
      final int selStart = getSelectionStart();
      final int selEnd = getSelectionEnd();
      selectAllItem.setVisible(selStart > 0 || selEnd < mSpannable.length());
    }
    return true;
  }

  private boolean handleActionItemClicked(ActionMode mode, MenuItem item) {
    // Check for PROCESS_TEXT intent actions first — ported from AOSP
    // Editor.TextActionModeCallback.
    if (mProcessTextIntentActionsHandler.performMenuItemAction(item)) {
      return true;
    }

    final int itemId = item.getItemId();
    if (itemId == ID_COPY) {
      copyToClipboard();
      mode.finish();
      return true;
    } else if (itemId == ID_SELECT_ALL) {
      selectAll();
      return true;
    } else if (itemId == ID_SHARE) {
      shareSelectedText();
      mode.finish();
      return true;
    }
    return false;
  }

  // Ported from AOSP Editor.TextActionModeCallback.onDestroyActionMode() (Editor.java:4858-4882).
  private void handleDestroyActionMode(ActionMode mode) {
    mTextActionMode = null;

    // When mPreserveSelection is true (set transiently by
    // stopTextActionModeWithPreservingSelection), keep the current selection so it's
    // still visible. Otherwise, collapse the selection to its end.
    if (!mPreserveSelection) {
      if (mSpannable != null) {
        final int selEnd = getSelectionEnd();
        if (selEnd >= 0) {
          Selection.setSelection(mSpannable, selEnd);
        }
      }
    }
    if (mSelectionController != null) {
      mSelectionController.hide();
    }

    mTextView.setHasTransientState(false);
  }

  // API 23+ callback with floating toolbar support via ActionMode.Callback2.
  @RequiresApi(Build.VERSION_CODES.M)
  private class SelectionActionModeCallback extends ActionMode.Callback2 {
    private final Path mSelectionPath = new Path();
    private final RectF mSelectionBounds = new RectF();

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
      return handleCreateActionMode(mode, menu);
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
      return handlePrepareActionMode(mode, menu);
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
      return handleActionItemClicked(mode, item);
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
      handleDestroyActionMode(mode);
    }

    // Ported from AOSP Editor.TextActionModeCallback.onGetContentRect() (Editor.java:4886-4919).
    @Override
    public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
      if (!view.equals(mTextView) || mTextView.getLayout() == null) {
        super.onGetContentRect(mode, view, outRect);
        return;
      }
      final int selectionStart = getSelectionStart();
      final int selectionEnd = getSelectionEnd();
      if (selectionStart < 0 || selectionEnd < 0) {
        super.onGetContentRect(mode, view, outRect);
        return;
      }
      final Layout layout = mTextView.getLayout();
      mSelectionPath.reset();
      layout.getSelectionPath(selectionStart, selectionEnd, mSelectionPath);
      mSelectionPath.computeBounds(mSelectionBounds, true);

      int textHorizontalOffset = mTextView.getTotalPaddingLeft() - mTextView.getScrollX();
      int textVerticalOffset = mTextView.getTotalPaddingTop() - mTextView.getScrollY();
      outRect.set(
          (int) Math.floor(mSelectionBounds.left + textHorizontalOffset),
          (int) Math.floor(mSelectionBounds.top + textVerticalOffset),
          (int) Math.ceil(mSelectionBounds.right + textHorizontalOffset),
          (int) Math.ceil(mSelectionBounds.bottom + textVerticalOffset));

      Drawable handleLeft = getSelectHandleLeft();
      Drawable handleRight = getSelectHandleRight();
      if (handleLeft != null && handleRight != null) {
        int handleHeight = Math.max(handleLeft.getMinimumHeight(), handleRight.getMinimumHeight());
        outRect.bottom += handleHeight;
      }
    }
  }

  // Pre-API 23 callback using standard ActionMode.Callback (no floating toolbar support).
  private class SelectionActionModeCallbackCompat implements ActionMode.Callback {
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
      return handleCreateActionMode(mode, menu);
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
      return handlePrepareActionMode(mode, menu);
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
      return handleActionItemClicked(mode, item);
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
      handleDestroyActionMode(mode);
    }
  }

  /**
   * SpanWatcher that detects when selection spans change and triggers invalidation of the selection
   * highlight path and view repaint. Only reacts to changes in the SELECTION_START and
   * SELECTION_END spans, matching AOSP's behavior and avoiding unnecessary work when other spans
   * are modified (e.g., during mount).
   */
  private class SelectionSpanWatcher implements SpanWatcher {
    private int mPrevSelStart = -1;
    private int mPrevSelEnd = -1;

    @Override
    public void onSpanAdded(Spannable text, Object what, int start, int end) {
      if (isSelectionSpan(what)) {
        checkSelectionChanged();
      }
    }

    @Override
    public void onSpanRemoved(Spannable text, Object what, int start, int end) {
      if (isSelectionSpan(what)) {
        checkSelectionChanged();
      }
    }

    @Override
    public void onSpanChanged(
        Spannable text, Object what, int ostart, int oend, int nstart, int nend) {
      if (isSelectionSpan(what)) {
        checkSelectionChanged();
      }
    }

    private boolean isSelectionSpan(Object what) {
      return what == Selection.SELECTION_START || what == Selection.SELECTION_END;
    }

    private void checkSelectionChanged() {
      final int selStart = getSelectionStart();
      final int selEnd = getSelectionEnd();
      if (selStart != mPrevSelStart || selEnd != mPrevSelEnd) {
        mPrevSelStart = selStart;
        mPrevSelEnd = selEnd;
        invalidateSelection();
      }
    }
  }

  /**
   * A helper for enabling and handling "PROCESS_TEXT" menu actions. These allow external
   * applications to plug into currently selected text.
   *
   * <p>Ported from AOSP {@code Editor.ProcessTextIntentActionsHandler} (Editor.java:8029-8163).
   * Nearly verbatim copy with naming adapted for RCTextEditor.
   */
  public static final class ProcessTextIntentActionsHandler {

    // Ported from AOSP TextView.ACCESSIBILITY_ACTION_PROCESS_TEXT_START_ID.
    private static final int ACCESSIBILITY_ACTION_PROCESS_TEXT_START_ID = 0x10000100;

    private final RCTextEditor mEditor;
    private final RCTextView mTextView;
    private final Context mContext;
    private final PackageManager mPackageManager;
    private final String mPackageName;
    private final List<ResolveInfo> mSupportedActivities = new ArrayList<>();
    private final SparseArray<Intent> mAccessibilityIntents = new SparseArray<>();
    private final SparseArray<AccessibilityNodeInfo.AccessibilityAction> mAccessibilityActions =
        new SparseArray<>();

    private ProcessTextIntentActionsHandler(RCTextEditor editor) {
      mEditor = Objects.requireNonNull(editor);
      mTextView = Objects.requireNonNull(mEditor.mTextView);
      mContext = Objects.requireNonNull(mTextView.getContext());
      mPackageManager = Objects.requireNonNull(mContext.getPackageManager());
      mPackageName = Objects.requireNonNull(mContext.getPackageName());
    }

    /** Adds "PROCESS_TEXT" menu items to the specified menu. */
    public void onInitializeMenu(Menu menu) {
      loadSupportedActivities();
      final int size = mSupportedActivities.size();
      for (int i = 0; i < size; i++) {
        final ResolveInfo resolveInfo = mSupportedActivities.get(i);
        menu.add(
                Menu.NONE,
                Menu.NONE,
                ACTION_MODE_MENU_ITEM_ORDER_PROCESS_TEXT_INTENT_ACTIONS_START + i,
                getLabel(resolveInfo))
            .setIntent(createProcessTextIntentForResolveInfo(resolveInfo))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
      }
    }

    /**
     * Performs a "PROCESS_TEXT" action if there is one associated with the specified menu item.
     *
     * @return True if the action was performed, false otherwise.
     */
    public boolean performMenuItemAction(MenuItem item) {
      return fireIntent(item.getIntent());
    }

    private boolean fireIntent(Intent intent) {
      if (intent != null && Intent.ACTION_PROCESS_TEXT.equals(intent.getAction())) {
        String selectedText = mEditor.getSelectedText();
        if (selectedText == null) {
          return false;
        }
        // Trim to avoid TransactionTooLargeException, matching AOSP's
        // TextUtils.trimToParcelableSize() behavior.
        if (selectedText.length() > MAX_PARCELABLE_TEXT_LENGTH) {
          selectedText = selectedText.substring(0, MAX_PARCELABLE_TEXT_LENGTH);
        }
        intent.putExtra(Intent.EXTRA_PROCESS_TEXT, selectedText);
        // Selection is preserved via the onWindowFocusChanged(false) →
        // stopTextActionModeWithPreservingSelection() path, matching AOSP behavior.
        if (mContext instanceof Activity) {
          ((Activity) mContext).startActivityForResult(intent, PROCESS_TEXT_REQUEST_CODE);
        } else {
          mContext.startActivity(intent);
        }
        return true;
      }
      return false;
    }

    private void loadSupportedActivities() {
      mSupportedActivities.clear();
      // Guard matching AOSP's canStartActivityForResult() check (which is @hide).
      // Only populate PROCESS_TEXT items when the context is an Activity, since
      // startActivityForResult is needed for the full round-trip.
      if (!(mContext instanceof Activity)) {
        return;
      }
      List<ResolveInfo> unfiltered =
          mPackageManager.queryIntentActivities(createProcessTextIntent(), 0);
      for (ResolveInfo info : unfiltered) {
        if (isSupportedActivity(info)) {
          mSupportedActivities.add(info);
        }
      }
    }

    private boolean isSupportedActivity(ResolveInfo info) {
      if (mPackageName.equals(info.activityInfo.packageName)) {
        return true;
      }
      if (!info.activityInfo.exported) {
        return false;
      }
      if (info.activityInfo.permission == null) {
        return true;
      }
      // Check if we hold the required permission for this activity.
      // API behavior:
      //   API 23+ (M): Uses Context.checkSelfPermission(), which accounts for runtime
      //     permissions and the app's own identity (matching AOSP's original code).
      //   Pre-23: Falls back to PackageManager.checkPermission(), which checks if the
      //     permission is granted to our package. Before the runtime permissions model
      //     (API 23), all declared permissions are granted at install time, so this
      //     produces equivalent results.
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        return mContext.checkSelfPermission(info.activityInfo.permission)
            == PackageManager.PERMISSION_GRANTED;
      }
      return mPackageManager.checkPermission(info.activityInfo.permission, mPackageName)
          == PackageManager.PERMISSION_GRANTED;
    }

    private Intent createProcessTextIntentForResolveInfo(ResolveInfo info) {
      // RCTextView is always read-only, so EXTRA_PROCESS_TEXT_READONLY is always true.
      return createProcessTextIntent()
          .putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
          .setClassName(info.activityInfo.packageName, info.activityInfo.name);
    }

    private Intent createProcessTextIntent() {
      return new Intent().setAction(Intent.ACTION_PROCESS_TEXT).setType("text/plain");
    }

    private CharSequence getLabel(ResolveInfo resolveInfo) {
      return resolveInfo.loadLabel(mPackageManager);
    }

    /**
     * Initializes and caches "PROCESS_TEXT" accessibility actions. Ported from AOSP
     * Editor.ProcessTextIntentActionsHandler.initializeAccessibilityActions().
     */
    public void initializeAccessibilityActions() {
      mAccessibilityIntents.clear();
      mAccessibilityActions.clear();
      int i = 0;
      loadSupportedActivities();
      for (ResolveInfo resolveInfo : mSupportedActivities) {
        int actionId = ACCESSIBILITY_ACTION_PROCESS_TEXT_START_ID + i++;
        mAccessibilityActions.put(
            actionId,
            new AccessibilityNodeInfo.AccessibilityAction(actionId, getLabel(resolveInfo)));
        mAccessibilityIntents.put(actionId, createProcessTextIntentForResolveInfo(resolveInfo));
      }
    }

    /**
     * Adds "PROCESS_TEXT" accessibility actions to the specified accessibility node info. NOTE:
     * This needs a prior call to {@link #initializeAccessibilityActions()} to make the latest
     * accessibility actions available for this call. Ported from AOSP
     * Editor.ProcessTextIntentActionsHandler.onInitializeAccessibilityNodeInfo().
     */
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo nodeInfo) {
      for (int i = 0; i < mAccessibilityActions.size(); i++) {
        nodeInfo.addAction(mAccessibilityActions.valueAt(i));
      }
    }

    /**
     * Performs a "PROCESS_TEXT" action if there is one associated with the specified accessibility
     * action id.
     *
     * @return True if the action was performed, false otherwise.
     */
    public boolean performAccessibilityAction(int actionId) {
      return fireIntent(mAccessibilityIntents.get(actionId));
    }
  }
}
