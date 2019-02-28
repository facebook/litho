/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Extension of {@link RecyclerView} that allows to add more features needed for @{@link
 * RecyclerSpec}
 */
public class LithoRecylerView extends RecyclerView implements HasPostDispatchDrawListener {

  private @Nullable TouchInterceptor mTouchInterceptor;
  private @Nullable PostDispatchDrawListener mPostDispatchDrawListener;

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

  @Override
  protected void dispatchDraw(Canvas canvas) {
    super.dispatchDraw(canvas);

    if (mPostDispatchDrawListener != null) {
      mPostDispatchDrawListener.postDispatchDraw();
    }
  }

  @Override
  public void setPostDispatchDrawListener(@Nullable PostDispatchDrawListener listener) {
    mPostDispatchDrawListener = listener;
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
