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

import java.text.BreakIterator;
import java.util.Locale;

/**
 * Simplified port of AOSP {@code android.text.method.WordIterator} for use with RCTextEditor. Since
 * {@code android.text.method.WordIterator} is {@code @hide} in the Android SDK, we provide our own
 * implementation with word-level and punctuation boundary support.
 *
 * <p>Uses {@code java.text.BreakIterator} instead of {@code android.icu.text.BreakIterator} (which
 * requires API 24). On Android, {@code java.text.BreakIterator} delegates to ICU internally,
 * providing identical Unicode, CJK, and emoji word boundary detection on all API levels (including
 * minSdk 21+). The two have the same API surface ({@code getWordInstance}, {@code setText}, {@code
 * isBoundary}, {@code preceding}, {@code following}, {@code DONE}), so this is a transparent
 * substitution with no behavioral difference.
 *
 * <p>Ported from AOSP WordIterator.java (/tmp/textview/WordIterator.java). Key differences from the
 * AOSP original:
 *
 * <ul>
 *   <li>Uses {@code charSequence.subSequence().toString()} instead of AOSP's internal {@code
 *       CharSequenceCharacterIterator} (which is not public API)
 *   <li>Punctuation detection uses {@code Character.getType()} instead of AOSP's internal {@code
 *       TextUtils.isPunctuation()} (which is {@code @hide})
 * </ul>
 */
public class RCWordIterator {

  // Copied from AOSP WordIterator.java:42
  private static final int WINDOW_WIDTH = 50;

  private final BreakIterator mIterator;
  private CharSequence mCharSeq;
  private int mStart;
  private int mEnd;

  public RCWordIterator(Locale locale) {
    mIterator = BreakIterator.getWordInstance(locale);
  }

  // Ported from AOSP WordIterator.java:73-82. Modified to use String instead of
  // CharSequenceCharacterIterator (which is @hide).
  public void setCharSequence(CharSequence charSequence, int start, int end) {
    if (start < 0 || end > charSequence.length()) {
      throw new IndexOutOfBoundsException(
          "Invalid range: ["
              + start
              + ", "
              + end
              + ") in sequence of length "
              + charSequence.length());
    }
    mCharSeq = charSequence;
    mStart = Math.max(0, start - WINDOW_WIDTH);
    mEnd = Math.min(charSequence.length(), end + WINDOW_WIDTH);
    mIterator.setText(charSequence.subSequence(mStart, mEnd).toString());
  }

  /**
   * Finds the start of the word at or before the given offset.
   *
   * <p>Ported from AOSP WordIterator.java:154-158 (getBeginning, delegates to overload with
   * getPrevWordBeginningOnTwoWordsBoundary=false).
   */
  public int getBeginning(int offset) {
    return getBeginning(offset, false);
  }

  /**
   * Finds the start of the word at or before the given offset.
   *
   * <p>Ported from AOSP WordIterator.java:234-251. When getPrevWordBeginningOnTwoWordsBoundary is
   * true and the offset is at a boundary between two words (both isOnLetterOrDigit and
   * isAfterLetterOrDigit), it returns the beginning of the previous word instead of the current
   * offset.
   */
  private int getBeginning(int offset, boolean getPrevWordBeginningOnTwoWordsBoundary) {
    final int shiftedOffset = offset - mStart;
    checkOffsetIsValid(shiftedOffset);

    if (isOnLetterOrDigit(shiftedOffset)) {
      if (mIterator.isBoundary(shiftedOffset)
          && (!isAfterLetterOrDigit(shiftedOffset) || !getPrevWordBeginningOnTwoWordsBoundary)) {
        return shiftedOffset + mStart;
      }
      int result = mIterator.preceding(shiftedOffset);
      return result == BreakIterator.DONE ? BreakIterator.DONE : result + mStart;
    }
    if (isAfterLetterOrDigit(shiftedOffset)) {
      int result = mIterator.preceding(shiftedOffset);
      return result == BreakIterator.DONE ? BreakIterator.DONE : result + mStart;
    }
    return BreakIterator.DONE;
  }

  /**
   * Finds the end of the word at or after the given offset.
   *
   * <p>Ported from AOSP WordIterator.java:174-178 (getEnd, delegates to overload with
   * getNextWordEndOnTwoWordBoundary=false).
   */
  public int getEnd(int offset) {
    return getEnd(offset, false);
  }

  /**
   * Finds the end of the word at or after the given offset.
   *
   * <p>Ported from AOSP WordIterator.java:269-285. When getNextWordEndOnTwoWordBoundary is true and
   * the offset is at a boundary between two words (both isAfterLetterOrDigit and
   * isOnLetterOrDigit), it returns the end of the next word instead of the current offset.
   */
  private int getEnd(int offset, boolean getNextWordEndOnTwoWordBoundary) {
    final int shiftedOffset = offset - mStart;
    checkOffsetIsValid(shiftedOffset);

    if (isAfterLetterOrDigit(shiftedOffset)) {
      if (mIterator.isBoundary(shiftedOffset)
          && (!isOnLetterOrDigit(shiftedOffset) || !getNextWordEndOnTwoWordBoundary)) {
        return shiftedOffset + mStart;
      }
      int result = mIterator.following(shiftedOffset);
      return result == BreakIterator.DONE ? BreakIterator.DONE : result + mStart;
    }
    if (isOnLetterOrDigit(shiftedOffset)) {
      int result = mIterator.following(shiftedOffset);
      return result == BreakIterator.DONE ? BreakIterator.DONE : result + mStart;
    }
    return BreakIterator.DONE;
  }

  // Ported from AOSP WordIterator.java:136-139 (prevBoundary).
  public int prevBoundary(int offset) {
    final int shiftedOffset = offset - mStart;
    if (shiftedOffset <= 0) {
      return BreakIterator.DONE;
    }
    if (shiftedOffset > (mEnd - mStart)) {
      return BreakIterator.DONE;
    }
    int result = mIterator.preceding(shiftedOffset);
    return result == BreakIterator.DONE ? BreakIterator.DONE : result + mStart;
  }

  // Ported from AOSP WordIterator.java:123-126 (nextBoundary).
  public int nextBoundary(int offset) {
    final int shiftedOffset = offset - mStart;
    if (shiftedOffset < 0) {
      return BreakIterator.DONE;
    }
    if (shiftedOffset >= (mEnd - mStart)) {
      return BreakIterator.DONE;
    }
    int result = mIterator.following(shiftedOffset);
    return result == BreakIterator.DONE ? BreakIterator.DONE : result + mStart;
  }

  // Ported from AOSP WordIterator.java:110-113.
  public boolean isBoundary(int offset) {
    final int shiftedOffset = offset - mStart;
    return mIterator.isBoundary(shiftedOffset);
  }

  // Ported from AOSP WordIterator.java:345-351. Modified to use Character.getType() instead
  // of AOSP's @hide TextUtils.isPunctuation(codePoint). Bounds-checks against the windowed
  // range [mStart, mEnd) matching AOSP.
  public boolean isOnPunctuation(int offset) {
    if (mStart > offset || offset >= mEnd) return false;
    final int codePoint = Character.codePointAt(mCharSeq, offset);
    return isPunctuation(Character.getType(codePoint));
  }

  // Ported from AOSP WordIterator.java:329-335. Modified to use Character.getType().
  // Bounds-checks against (mStart, mEnd] matching AOSP.
  public boolean isAfterPunctuation(int offset) {
    if (mStart >= offset || offset > mEnd) return false;
    final int codePoint = Character.codePointBefore(mCharSeq, offset);
    return isPunctuation(Character.getType(codePoint));
  }

  // Ported from AOSP WordIterator.java:374-376.
  private boolean isPunctuationStartBoundary(int offset) {
    return isOnPunctuation(offset) && !isAfterPunctuation(offset);
  }

  // Ported from AOSP WordIterator.java:378-380.
  private boolean isPunctuationEndBoundary(int offset) {
    return !isOnPunctuation(offset) && isAfterPunctuation(offset);
  }

  // Ported from AOSP WordIterator.java:295-302. Uses prevBoundary() in a loop to find
  // the start of the punctuation group, matching AOSP behavior.
  public int getPunctuationBeginning(int offset) {
    checkOffsetIsValid(offset - mStart);
    while (offset != BreakIterator.DONE && !isPunctuationStartBoundary(offset)) {
      offset = prevBoundary(offset);
    }
    return offset;
  }

  // Ported from AOSP WordIterator.java:312-319. Uses nextBoundary() in a loop to find
  // the end of the punctuation group, matching AOSP behavior.
  public int getPunctuationEnd(int offset) {
    checkOffsetIsValid(offset - mStart);
    while (offset != BreakIterator.DONE && !isPunctuationEndBoundary(offset)) {
      offset = nextBoundary(offset);
    }
    return offset;
  }

  // Ported from AOSP WordIterator.java:195-197. Delegates to getBeginning(offset, true).
  public int getPrevWordBeginningOnTwoWordsBoundary(int offset) {
    return getBeginning(offset, true);
  }

  // Ported from AOSP WordIterator.java:214-216. Delegates to getEnd(offset, true).
  public int getNextWordEndOnTwoWordBoundary(int offset) {
    return getEnd(offset, true);
  }

  // Ported from AOSP WordIterator.java:390-396.
  private boolean isOnLetterOrDigit(int shiftedOffset) {
    int index = shiftedOffset + mStart;
    if (index < mStart || index >= mEnd) return false;
    return Character.isLetterOrDigit(Character.codePointAt(mCharSeq, index));
  }

  // Ported from AOSP WordIterator.java:382-388.
  private boolean isAfterLetterOrDigit(int shiftedOffset) {
    int index = shiftedOffset + mStart;
    if (index <= mStart || index > mEnd) return false;
    return Character.isLetterOrDigit(Character.codePointBefore(mCharSeq, index));
  }

  // Ported from AOSP WordIterator.java:398-403.
  private void checkOffsetIsValid(int shiftedOffset) {
    if (shiftedOffset < 0 || shiftedOffset > (mEnd - mStart)) {
      throw new IllegalArgumentException("Invalid offset: " + (shiftedOffset + mStart));
    }
  }

  // Adapted from AOSP TextUtils.isPunctuation() which is @hide. Uses Character.getType()
  // to check all Unicode punctuation categories, matching the AOSP behavior.
  private static boolean isPunctuation(int type) {
    return type == Character.CONNECTOR_PUNCTUATION
        || type == Character.DASH_PUNCTUATION
        || type == Character.END_PUNCTUATION
        || type == Character.FINAL_QUOTE_PUNCTUATION
        || type == Character.INITIAL_QUOTE_PUNCTUATION
        || type == Character.OTHER_PUNCTUATION
        || type == Character.START_PUNCTUATION;
  }
}
