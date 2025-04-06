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
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import androidx.annotation.Nullable;
import androidx.core.view.OneShotPreDrawListener;
import com.facebook.litho.BaseMountingView;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.HasLithoViewChildren;
import com.facebook.litho.LayoutState;
import com.facebook.litho.LithoRenderTreeView;
import com.facebook.litho.LithoView;
import com.facebook.litho.TreeState;
import java.util.List;

/**
 * Extension of {@link HorizontalScrollView} that allows to add more features needed for @{@link
 * HorizontalScrollSpec}.
 */
public class HorizontalScrollLithoView extends HorizontalScrollView
    implements HasLithoViewChildren {

  private static final int LAST_SCROLL_POSITION_UNSET = -1;
  private final BaseMountingView mBaseMountingView;

  private int mComponentWidth;
  private int mComponentHeight;
  ;

  @Nullable private ScrollPosition mScrollPosition;
  @Nullable private OnScrollChangeListener mOnScrollChangeListener;
  @Nullable private ScrollStateDetector mScrollStateDetector;

  public HorizontalScrollLithoView(Context context) {
    this(context, new LithoView(context));
  }

  public HorizontalScrollLithoView(Context context, BaseMountingView view) {
    super(context);
    mBaseMountingView = view;
    addView(mBaseMountingView);
  }

  public BaseMountingView getRenderTreeView() {
    return mBaseMountingView;
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
    super.draw(canvas);
    if (mScrollStateDetector != null) {
      mScrollStateDetector.onDraw();
    }
  }

  @Override
  protected void onScrollChanged(int l, int t, int oldl, int oldt) {
    super.onScrollChanged(l, t, oldl, oldt);

    // We need to notify LithoView about the visibility bounds that has changed when View is
    // scrolled so that correct visibility events are fired for the child components of
    // HorizontalScroll.
    mBaseMountingView.notifyVisibleBoundsChanged();

    if (mScrollPosition != null) {
      if (mOnScrollChangeListener != null) {
        mOnScrollChangeListener.onScrollChange(this, getScrollX(), mScrollPosition.x);
      }
      mScrollPosition.x = getScrollX();
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

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // The hosting component view always matches the component size. This will
    // ensure that there will never be a size-mismatch between the view and the
    // component-based content, which would trigger a layout pass in the
    // UI thread.
    mBaseMountingView.measure(
        MeasureSpec.makeMeasureSpec(mComponentWidth, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(mComponentHeight, MeasureSpec.EXACTLY));

    // The mounted view always gets exact dimensions from the framework.
    setMeasuredDimension(
        MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
  }

  @Override
  public void obtainLithoViewChildren(List<BaseMountingView> lithoViews) {
    lithoViews.add(mBaseMountingView);
  }

  public void mount(final @Nullable LayoutState layoutState, final @Nullable TreeState state) {
    if (layoutState != null && state != null && mBaseMountingView instanceof LithoRenderTreeView) {
      ((LithoRenderTreeView) mBaseMountingView).setLayoutState(layoutState, state);
    }
  }

  public void mount(
      ComponentTree componentTree,
      ScrollPosition scrollPosition,
      int width,
      int height,
      int scrollX,
      @Nullable OnScrollChangeListener onScrollChangeListener,
      @Nullable ScrollStateListener scrollStateListener) {
    if (!(mBaseMountingView instanceof LithoView)) {
      throw new UnsupportedOperationException(
          "API can only be invoked from Horizontal Scroll Spec");
    }
    ((LithoView) mBaseMountingView).setComponentTree(componentTree);
    mScrollPosition = scrollPosition;
    mOnScrollChangeListener = onScrollChangeListener;
    mComponentWidth = width;
    mComponentHeight = height;
    setScrollX(scrollX);
    if (scrollStateListener != null) {
      if (mScrollStateDetector == null) {
        mScrollStateDetector = new ScrollStateDetector(this);
      }
      mScrollStateDetector.setListener(scrollStateListener);
    }
  }

  public void unmount() {
    if (mBaseMountingView instanceof LithoRenderTreeView) {
      ((LithoRenderTreeView) mBaseMountingView).clean();
    } else if (mBaseMountingView instanceof LithoView) {
      ((LithoView) mBaseMountingView).setComponentTree(null, false);
    }
    mComponentWidth = 0;
    mComponentHeight = 0;
    mScrollPosition = null;
    mOnScrollChangeListener = null;
    setScrollX(0);
    if (mScrollStateDetector != null) {
      mScrollStateDetector.setListener(null);
    }
  }

  public void setScrollPosition(ScrollPosition scrollPosition) {
    mScrollPosition = scrollPosition;
    OneShotPreDrawListener.add(
        this,
        () -> {
          if (mScrollPosition == null) {
            return;
          }
          if (mScrollPosition.x == LAST_SCROLL_POSITION_UNSET) {
            fullScroll(HorizontalScrollView.FOCUS_RIGHT);
            mScrollPosition.x = getScrollX();
          } else {
            setScrollX(mScrollPosition.x);
          }
        });
  }

  public void setOnScrollChangeListener(OnScrollChangeListener onScrollChangeListener) {
    mOnScrollChangeListener = onScrollChangeListener;
  }

  public void setScrollStateListener(ScrollStateListener scrollStateListener) {
    if (scrollStateListener != null) {
      if (mScrollStateDetector == null) {
        mScrollStateDetector = new ScrollStateDetector(this);
      }
      mScrollStateDetector.setListener(scrollStateListener);
    }
  }

  public static class ScrollPosition {
    public int x;

    public ScrollPosition(int initialX) {
      this.x = initialX;
    }
  }

  /** Scroll change listener invoked when the scroll position changes. */
  public interface OnScrollChangeListener {
    void onScrollChange(View v, int scrollX, int oldScrollX);
  }
}
