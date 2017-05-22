/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.view.View;

import static com.facebook.litho.EventDispatcherUtils.dispatchOnLongClick;

/**
 * Long click listener that triggers its underlying event handler.
 */
class ComponentLongClickListener implements View.OnLongClickListener {

  private EventHandler<LongClickEvent> mEventHandler;
  private boolean mShouldIgnoreMotionEventUp;

  @Override
  public boolean onLongClick(View view) {
    mShouldIgnoreMotionEventUp = true;
    if (mEventHandler != null) {
      return dispatchOnLongClick(mEventHandler, view);
    }

    return false;
  }

  EventHandler<LongClickEvent> getEventHandler() {
    return mEventHandler;
  }

  void setEventHandler(EventHandler<LongClickEvent> eventHandler) {
    mShouldIgnoreMotionEventUp = false;
    mEventHandler = eventHandler;
  }

  /**
   * Whether or not the next 'up' event should be ignored by the view this listener is set on. If
   * the previous touch event signified that there is a long click, this is true. Otherwise, false.
   *
   * @return Whether or not the next 'up' motion event should be ignored.
   */
  public boolean shouldIgnoreMotionEventUp() {
    return mShouldIgnoreMotionEventUp;
  }

  /**
   * This method should be called when the 'up' event that should have been cancelled was
   * cancelled.
   */
  public void resetShouldIgnoreMotionEventUp() {
    mShouldIgnoreMotionEventUp = false;
  }
}
