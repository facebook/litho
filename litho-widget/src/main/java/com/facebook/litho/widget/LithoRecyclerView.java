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

package com.facebook.litho.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.litho.R;
import java.util.ArrayList;
import java.util.List;

/**
 * Extension of {@link RecyclerView} that allows to add more features needed for @{@link
 * com.facebook.litho.widget.Recycler}
 */
public class LithoRecyclerView extends RecyclerView implements HasPostDispatchDrawListener {

  private boolean mLeftFadingEnabled = true;
  private boolean mRightFadingEnabled = true;
  private boolean mTopFadingEnabled = true;
  private boolean mBottomFadingEnabled = true;
  private @Nullable TouchInterceptor mTouchInterceptor;
  private @Nullable List<PostDispatchDrawListener> mPostDispatchDrawListeners;

  public LithoRecyclerView(Context context) {
    this(context, null);
  }

  public LithoRecyclerView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public LithoRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    setTag(R.id.litho_pooling_container, true);
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

    if (mPostDispatchDrawListeners != null) {
      for (int i = 0, size = mPostDispatchDrawListeners.size(); i < size; i++) {
        mPostDispatchDrawListeners.get(i).postDispatchDraw(getChildCount());
      }
    }
  }

  @Override
  public void registerPostDispatchDrawListener(PostDispatchDrawListener listener) {
    if (mPostDispatchDrawListeners == null) {
      mPostDispatchDrawListeners = new ArrayList<>();
    }
    mPostDispatchDrawListeners.add(listener);
  }

  @Override
  public void unregisterPostDispatchDrawListener(PostDispatchDrawListener listener) {
    if (mPostDispatchDrawListeners != null) {
      mPostDispatchDrawListeners.remove(listener);
    }
  }

  public void setLeftFadingEnabled(boolean leftFadingEnabled) {
    this.mLeftFadingEnabled = leftFadingEnabled;
  }

  public void setRightFadingEnabled(boolean rightFadingEnabled) {
    this.mRightFadingEnabled = rightFadingEnabled;
  }

  public void setTopFadingEnabled(boolean topFadingEnabled) {
    this.mTopFadingEnabled = topFadingEnabled;
  }

  public void setBottomFadingEnabled(boolean bottomFadingEnabled) {
    this.mBottomFadingEnabled = bottomFadingEnabled;
  }

  @Override
  public float getLeftFadingEdgeStrength() {
    if (mLeftFadingEnabled) {
      return super.getLeftFadingEdgeStrength();
    } else {
      return 0f;
    }
  }

  @Override
  public float getRightFadingEdgeStrength() {
    if (mRightFadingEnabled) {
      return super.getRightFadingEdgeStrength();
    } else {
      return 0f;
    }
  }

  @Override
  public float getTopFadingEdgeStrength() {
    if (mTopFadingEnabled) {
      return super.getTopFadingEdgeStrength();
    } else {
      return 0f;
    }
  }

  @Override
  public float getBottomFadingEdgeStrength() {
    if (mBottomFadingEnabled) {
      return super.getBottomFadingEdgeStrength();
    } else {
      return 0f;
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
