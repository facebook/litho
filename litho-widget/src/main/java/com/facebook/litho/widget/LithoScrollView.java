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
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.LithoView;
import com.facebook.litho.widget.VerticalScrollSpec.OnInterceptTouchListener;
import com.facebook.litho.widget.VerticalScrollSpec.ScrollPosition;

/**
 * Extension of {@link NestedScrollView} that allows to add more features needed for @{@link
 * VerticalScrollSpec}.
 */
public class LithoScrollView extends NestedScrollView {

  private final LithoView mLithoView;

  @Nullable private ScrollPosition mScrollPosition;
  @Nullable private ViewTreeObserver.OnPreDrawListener mOnPreDrawListener;
  @Nullable private OnInterceptTouchListener mOnInterceptTouchListener;
  private boolean mIsIncrementalMountEnabled;

  public LithoScrollView(Context context) {
    this(context, null);
  }

  public LithoScrollView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public LithoScrollView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    mLithoView = new LithoView(context);
    addView(mLithoView);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    boolean result = false;
    if (mOnInterceptTouchListener != null) {
      result = mOnInterceptTouchListener.onInterceptTouch(this, ev);
    }
    if (!result && super.onInterceptTouchEvent(ev)) {
      result = true;
    }
    return result;
  }

  @Override
  protected void onScrollChanged(int l, int t, int oldl, int oldt) {
    super.onScrollChanged(l, t, oldl, oldt);

    if (mIsIncrementalMountEnabled) {
      mLithoView.notifyVisibleBoundsChanged();
    }

    if (mScrollPosition != null) {
      mScrollPosition.y = getScrollY();
    }
  }

  /**
   * NestedScrollView does not automatically consume the fling event. However, RecyclerView consumes
   * this event if it's either vertically or horizontally scrolling. {@link RecyclerView#fling}
   * Since this view is specifically made for vertically scrolling components, we always consume the
   * nested fling event just like recycler view.
   */
  @Override
  public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
    return super.dispatchNestedFling(velocityX, velocityY, true);
  }

  public void setOnInterceptTouchListener(
      @Nullable OnInterceptTouchListener onInterceptTouchListener) {
    mOnInterceptTouchListener = onInterceptTouchListener;
  }

  void mount(
      ComponentTree contentComponentTree,
      final ScrollPosition scrollPosition,
      boolean isIncrementalMountEnabled) {
    mLithoView.setComponentTree(contentComponentTree);

    mIsIncrementalMountEnabled = isIncrementalMountEnabled;
    mScrollPosition = scrollPosition;
    final ViewTreeObserver.OnPreDrawListener onPreDrawListener =
        new ViewTreeObserver.OnPreDrawListener() {
          @Override
          public boolean onPreDraw() {
            setScrollY(scrollPosition.y);
            ViewTreeObserver currentViewTreeObserver = getViewTreeObserver();
            if (currentViewTreeObserver.isAlive()) {
              currentViewTreeObserver.removeOnPreDrawListener(this);
            }
            return true;
          }
        };
    getViewTreeObserver().addOnPreDrawListener(onPreDrawListener);

    mOnPreDrawListener = onPreDrawListener;
  }

  void unmount() {
    mLithoView.setComponentTree(null);

    mScrollPosition = null;
    getViewTreeObserver().removeOnPreDrawListener(mOnPreDrawListener);
    mOnPreDrawListener = null;
  }
}
