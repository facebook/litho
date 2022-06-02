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

package com.facebook.litho.sections;

import androidx.annotation.UiThread;
import com.facebook.litho.widget.SmoothScrollAlignmentType;
import javax.annotation.Nullable;

/**
 * Registers a focus request created in the {@link SectionTree} and dispatches the focus once
 * requested.
 *
 * <p>You can register a pending focus to a position {@link #registerPendingFocus(int, int,
 * SmoothScrollAlignmentType)}, or via an id, using {@link #registerPendingFocus(Object, int,
 * SmoothScrollAlignmentType)}.
 *
 * <p>This orchestrator will update the pending scroll position according to the structural updates
 * it receives via: - {@link #registerInsert(int, int)} - {@link #registerDelete(int, int)} - {@link
 * #registerMove(int, int)}
 *
 * <p>This mechanism will guarantee that when the pending scroll is always in sync with any updates
 * that occur from the moment the focus is requested, until the moment it is executed.
 *
 * <p>To perform the pending focus, you should call {@link #requestFocus()} on the {@link UiThread}.
 */
class PendingFocusOrchestrator {

  private static final int NO_POSITION = 1;

  private final FocusDispatcher mDispatcher;

  PendingFocusOrchestrator(FocusDispatcher dispatcher) {
    this.mDispatcher = dispatcher;
  }

  @Nullable private Object mPendingFocusId = null;
  private int mPendingFocusPosition = NO_POSITION;
  private int mPendingFocusOffset = 0;
  @Nullable private SmoothScrollAlignmentType mPendingFocusScrollAlignmentType = null;

  void registerPendingFocus(
      Object id, int offset, @Nullable SmoothScrollAlignmentType smoothScrollAlignmentType) {
    mPendingFocusId = id;
    mPendingFocusOffset = offset;
    mPendingFocusScrollAlignmentType = smoothScrollAlignmentType;
  }

  void registerPendingFocus(
      int position, int offset, @Nullable SmoothScrollAlignmentType smoothScrollAlignmentType) {
    mPendingFocusPosition = position;
    mPendingFocusOffset = offset;
    mPendingFocusScrollAlignmentType = smoothScrollAlignmentType;
  }

  void registerInsert(int position, int numItems) {
    if (mPendingFocusPosition != NO_POSITION && position <= mPendingFocusPosition) {
      mPendingFocusPosition += numItems;
    }
  }

  void registerDelete(int position, int numItems) {
    if (mPendingFocusPosition != NO_POSITION) {
      // when removed items include the focus target, then we reset it
      if (position <= mPendingFocusPosition && position + numItems > mPendingFocusPosition) {
        resetPendingFocus();
      } else if (position <= mPendingFocusPosition
          && position + numItems <= mPendingFocusPosition) {
        mPendingFocusPosition -= numItems;
      }
    }
  }

  void registerMove(int fromPosition, int toPosition) {
    if (mPendingFocusPosition != NO_POSITION) {
      if (mPendingFocusPosition == fromPosition) {
        mPendingFocusPosition = toPosition;
      } else if (mPendingFocusPosition == toPosition) {
        mPendingFocusPosition = fromPosition;
      }
    }
  }

  @UiThread
  void requestFocus() {
    if (mPendingFocusId != null) {
      if (mPendingFocusScrollAlignmentType == null) {
        mDispatcher.requestFocusWithOffset(mPendingFocusId, mPendingFocusOffset);
      } else {
        mDispatcher.requestSmoothFocus(
            mPendingFocusId, mPendingFocusOffset, mPendingFocusScrollAlignmentType);
      }
    } else if (mPendingFocusPosition != NO_POSITION) {
      if (mPendingFocusScrollAlignmentType == null) {
        mDispatcher.requestFocusWithOffset(mPendingFocusPosition, mPendingFocusOffset);
      } else {
        mDispatcher.requestSmoothFocus(
            mPendingFocusPosition, mPendingFocusOffset, mPendingFocusScrollAlignmentType);
      }
    }

    resetPendingFocus();
  }

  private void resetPendingFocus() {
    mPendingFocusId = null;
    mPendingFocusPosition = NO_POSITION;
    mPendingFocusOffset = 0;
    mPendingFocusScrollAlignmentType = null;
  }
}
