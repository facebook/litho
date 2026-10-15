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

import android.view.MotionEvent;
import android.view.ViewConfiguration;

/**
 * Tracks touch gesture state for double-tap and triple-click detection.
 *
 * <p>Ported from AOSP {@code android.widget.EditorTouchState} (framework-internal class, not part
 * of the public SDK). The entire class is a near-verbatim copy with naming adapted for RCTextView.
 */
public class RCEditorTouchState {

  private float mLastDownX;
  private float mLastDownY;
  private long mLastDownMillis;
  private float mLastUpX;
  private float mLastUpY;
  private long mLastUpMillis;
  private boolean mIsOnHandle;

  private int mMultiTapStatus = MultiTapStatus.NONE;
  private boolean mMultiTapInSameArea;
  private boolean mMovedEnoughForDrag;

  // Ported from AOSP EditorTouchState.MultiTapStatus constants.
  private static class MultiTapStatus {
    static final int NONE = 0;
    static final int FIRST_TAP = 1;
    static final int DOUBLE_TAP = 2;
    static final int TRIPLE_CLICK = 3;
  }

  public float getLastDownX() {
    return mLastDownX;
  }

  public float getLastDownY() {
    return mLastDownY;
  }

  public float getLastUpX() {
    return mLastUpX;
  }

  public float getLastUpY() {
    return mLastUpY;
  }

  public boolean isDoubleTap() {
    return mMultiTapStatus == MultiTapStatus.DOUBLE_TAP;
  }

  public boolean isTripleClick() {
    return mMultiTapStatus == MultiTapStatus.TRIPLE_CLICK;
  }

  public boolean isMultiTap() {
    return mMultiTapStatus == MultiTapStatus.DOUBLE_TAP
        || mMultiTapStatus == MultiTapStatus.TRIPLE_CLICK;
  }

  public boolean isMultiTapInSameArea() {
    return mMultiTapInSameArea;
  }

  public boolean isMovedEnoughForDrag() {
    return mMovedEnoughForDrag;
  }

  public boolean isOnHandle() {
    return mIsOnHandle;
  }

  public void setIsOnHandle(boolean onHandle) {
    mIsOnHandle = onHandle;
  }

  /**
   * Updates the touch state based on the given motion event. Should be called for every touch event
   * received by the view.
   *
   * <p>Ported from AOSP EditorTouchState.update(MotionEvent, ViewConfiguration).
   */
  public void update(MotionEvent event, ViewConfiguration viewConfiguration) {
    final int action = event.getActionMasked();
    if (action == MotionEvent.ACTION_DOWN) {
      final boolean isDoubleTap =
          (event.getEventTime() - mLastUpMillis) <= ViewConfiguration.getDoubleTapTimeout()
              && isDistanceWithin(
                  mLastUpX,
                  mLastUpY,
                  event.getX(),
                  event.getY(),
                  viewConfiguration.getScaledDoubleTapSlop());

      if (isDoubleTap) {
        if (mMultiTapStatus == MultiTapStatus.FIRST_TAP) {
          mMultiTapStatus = MultiTapStatus.DOUBLE_TAP;
        } else if (mMultiTapStatus == MultiTapStatus.DOUBLE_TAP) {
          mMultiTapStatus = MultiTapStatus.TRIPLE_CLICK;
        }
      } else {
        mMultiTapStatus = MultiTapStatus.FIRST_TAP;
      }

      mMultiTapInSameArea =
          isDistanceWithin(
              mLastDownX,
              mLastDownY,
              event.getX(),
              event.getY(),
              viewConfiguration.getScaledTouchSlop());

      mLastDownX = event.getX();
      mLastDownY = event.getY();
      mLastDownMillis = event.getEventTime();
      mMovedEnoughForDrag = false;
    } else if (action == MotionEvent.ACTION_UP) {
      mLastUpX = event.getX();
      mLastUpY = event.getY();
      mLastUpMillis = event.getEventTime();
    } else if (action == MotionEvent.ACTION_MOVE) {
      if (!mMovedEnoughForDrag) {
        mMovedEnoughForDrag =
            !isDistanceWithin(
                mLastDownX,
                mLastDownY,
                event.getX(),
                event.getY(),
                viewConfiguration.getScaledTouchSlop());
      }
    } else if (action == MotionEvent.ACTION_CANCEL) {
      // Reset drag tracking on cancel. Ported from AOSP EditorTouchState which
      // handles all action types.
      mMovedEnoughForDrag = false;
      mIsOnHandle = false;
    }
  }

  public void resetMultiTap() {
    mMultiTapStatus = MultiTapStatus.NONE;
    mMultiTapInSameArea = false;
  }

  /**
   * Returns true if the distance between (x1, y1) and (x2, y2) is less than or equal to the given
   * distance.
   *
   * <p>Ported from AOSP EditorTouchState.isDistanceWithin().
   */
  public static boolean isDistanceWithin(float x1, float y1, float x2, float y2, int distance) {
    float deltaX = x2 - x1;
    float deltaY = y2 - y1;
    return (deltaX * deltaX + deltaY * deltaY) <= (distance * distance);
  }
}
