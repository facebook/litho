/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.rendercore.AuditableMountContent;
import java.util.ArrayList;
import java.util.List;

/**
 * Extension of {@link RecyclerView} that allows to add more features needed for @{@link
 * RecyclerSpec}
 */
public class LithoRecylerView extends RecyclerView
    implements HasPostDispatchDrawListener, AuditableMountContent {

  private @Nullable TouchInterceptor mTouchInterceptor;
  private @Nullable PostDispatchDrawListener mPostDispatchDrawListener;

  private boolean mIsBound = false;
  private final StringBuilder mUsageStringBuilder = new StringBuilder();

  public LithoRecylerView(Context context) {
    this(context, null);
  }

  public LithoRecylerView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public LithoRecylerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
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
  public void registerPostDispatchDrawListener(PostDispatchDrawListener listener) {
    mPostDispatchDrawListener = listener;
  }

  @Override
  public void unregisterPostDispatchDrawListener(PostDispatchDrawListener listener) {
    mPostDispatchDrawListener = null;
  }

  @Override
  public void auditAfterOnBind() {
    if (mIsBound) {
      throw new RuntimeException(
          "LithoRecyclerView is already bound and is being bound again!"
              + "\nUsage Log:\n"
              + mUsageStringBuilder.toString());
    }

    mIsBound = true;
  }

  @Override
  public void auditAfterOnUnbind() {
    // Don't crash if already unbound here, since this might happen.
    mIsBound = false;
  }

  @Override
  public void logError(String message, Exception e) {
    // ERROR: <message>: <exception message>
    // <stack-trace line 1>
    // <stack-trace line 2>
    // ...
    // <stack-trace line n>
    mUsageStringBuilder.append("ERROR: ");
    mUsageStringBuilder.append(message);
    mUsageStringBuilder.append(": ");
    mUsageStringBuilder.append(e.getMessage());
    mUsageStringBuilder.append("\n");
    mUsageStringBuilder.append(Log.getStackTraceString(e));
    mUsageStringBuilder.append("\n");
  }

  @Override
  public void logUsage(String usageDescription) {
    mUsageStringBuilder.append(usageDescription);
    mUsageStringBuilder.append("\n");
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
