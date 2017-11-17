/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Extension of {@link RecyclerView} that allows to add more features needed for @{@link
 * RecyclerSpec}
 */
public class LithoRecylerView extends RecyclerView {

  private @Nullable TouchInterceptor mTouchInterceptor;

  public LithoRecylerView(Context context) {
    this(context, null);
  }

  public LithoRecylerView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public LithoRecylerView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  /**
   * Set TouchInterceptor that will be used in #onInterceptTouchEvent(android.view.MotionEvent) to
   * determine how touch events should be intercepted by this {@link RecyclerView}
   */
  public void setTouchInterceptor(@Nullable TouchInterceptor touchInterceptor) {
    mTouchInterceptor = touchInterceptor;
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    if (mTouchInterceptor == null) {
      return super.onInterceptTouchEvent(ev);
    }

    TouchInterceptor.Result result = mTouchInterceptor.onInterceptTouchEvent(this, ev);
    switch (result) {
      case INTERCEPT_TOUCH_EVENT:
        return true;
      case IGNORE_TOUCH_EVENT:
        return false;
      case CALL_SUPER:
        return super.onInterceptTouchEvent(ev);
      default:
        throw new IllegalArgumentException("Unknown TouchInterceptor.Result: " + result);
    }
  }

  /** Allows to override {@link #onInterceptTouchEvent(MotionEvent)} behavior */
  public interface TouchInterceptor {

    enum Result {
      /** Return true without calling super.onInterceptTouchEvent() */
      INTERCEPT_TOUCH_EVENT,
      /** Return false without calling super.onInterceptTouchEvent() */
      IGNORE_TOUCH_EVENT,
      /** Returns super.onInterceptTouchEvent() */
      CALL_SUPER,
    }

    /**
     * Called from {@link #onInterceptTouchEvent(MotionEvent)} to determine how touch events should
     * be intercepted
     */
    Result onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent ev);
  }
}
