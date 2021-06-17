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
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.HasLithoViewChildren;
import com.facebook.litho.LithoMetadataExceptionWrapper;
import com.facebook.litho.LithoView;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.widget.VerticalScrollSpec.OnInterceptTouchListener;
import com.facebook.litho.widget.VerticalScrollSpec.ScrollPosition;
import com.facebook.rendercore.ErrorReporter;
import com.facebook.rendercore.LogLevel;
import java.util.List;

/**
 * Extension of {@link NestedScrollView} that allows to add more features needed for @{@link
 * VerticalScrollSpec}.
 */
public class LithoScrollView extends NestedScrollView implements HasLithoViewChildren {

  private final LithoView mLithoView;

  @Nullable private ScrollPosition mScrollPosition;
  @Nullable private ViewTreeObserver.OnPreDrawListener mOnPreDrawListener;
  @Nullable private OnInterceptTouchListener mOnInterceptTouchListener;
  @Nullable private ScrollStateDetector mScrollStateDetector;
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
  public void fling(int velocityX) {
    super.fling(velocityX);
    if (mScrollStateDetector != null) {
      mScrollStateDetector.fling();
    }
  }

  @Override
  public void draw(Canvas canvas) {
    try {
      super.draw(canvas);
      if (mScrollStateDetector != null) {
        mScrollStateDetector.onDraw();
      }
    } catch (Throwable t) {
      final ComponentTree ct = mLithoView.getComponentTree();
      if (ct != null) {
        ErrorReporter.getInstance()
            .report(
                LogLevel.ERROR,
                "LITHO:NPE:LITHO_SCROLL_VIEW_DRAW",
                "Root component: " + ct.getSimpleName(),
                t);
        throw new LithoMetadataExceptionWrapper(ct, t);
      } else {
        throw t;
      }
    }
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

    if (mScrollStateDetector != null) {
      mScrollStateDetector.onScrollChanged();
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent motionEvent) {
    boolean isConsumed = super.onTouchEvent(motionEvent);

    if (mScrollStateDetector != null) {
      mScrollStateDetector.onTouchEvent(motionEvent);
    }

    return isConsumed;
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

  @Override
  public void obtainLithoViewChildren(List<LithoView> lithoViews) {
    lithoViews.add(mLithoView);
  }

  void mount(
      ComponentTree contentComponentTree,
      final ScrollPosition scrollPosition,
      @Nullable ScrollStateListener scrollStateListener,
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
    if (scrollStateListener != null) {
      if (mScrollStateDetector == null) {
        mScrollStateDetector = new ScrollStateDetector(this);
      }
      mScrollStateDetector.setListener(scrollStateListener);
    }
  }

  void unmount() {
    mLithoView.unbind();
    if (!ComponentsConfiguration.unmountAllWhenComponentTreeSetToNull) {
      mLithoView.setComponentTree(null);
    }

    mScrollPosition = null;
    getViewTreeObserver().removeOnPreDrawListener(mOnPreDrawListener);
    mOnPreDrawListener = null;
    if (mScrollStateDetector != null) {
      mScrollStateDetector.setListener(null);
    }
  }
}
