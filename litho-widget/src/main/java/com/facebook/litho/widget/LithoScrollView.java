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
import android.view.ViewTreeObserver;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.litho.BaseMountingView;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.HasLithoViewChildren;
import com.facebook.litho.LayoutState;
import com.facebook.litho.LithoMetadataExceptionWrapper;
import com.facebook.litho.LithoRenderTreeView;
import com.facebook.litho.LithoView;
import com.facebook.litho.TreeState;
import com.facebook.rendercore.ErrorReporter;
import com.facebook.rendercore.LogLevel;
import com.facebook.rendercore.utils.CommonUtils;
import java.util.List;

/**
 * Extension of {@link NestedScrollView} that allows to add more features needed for @{@link
 * VerticalScrollSpec}.
 */
public class LithoScrollView extends NestedScrollView implements HasLithoViewChildren {

  private final BaseMountingView mLithoView;

  @Nullable private ScrollPosition mScrollPosition;
  @Nullable private ViewTreeObserver.OnPreDrawListener mOnPreDrawListener;
  @Nullable private OnInterceptTouchListener mOnInterceptTouchListener;
  @Nullable private ScrollStateDetector mScrollStateDetector;

  private @Nullable String mCurrentRootComponent;
  private @Nullable String mCurrentLogTag;

  public LithoScrollView(Context context) {
    this(context, new LithoView(context));
  }

  public LithoScrollView(Context context, BaseMountingView view) {
    this(context, view, null, 0);
  }

  public LithoScrollView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public LithoScrollView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, new LithoView(context), attrs, defStyleAttr);
  }

  public LithoScrollView(
      final Context context,
      final BaseMountingView view,
      final @Nullable AttributeSet attrs,
      final int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    mLithoView = view;
    addView(mLithoView);
  }

  public BaseMountingView getRenderTreeView() {
    return mLithoView;
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
      ErrorReporter.getInstance()
          .report(
              LogLevel.ERROR,
              "LITHO:NPE:LITHO_SCROLL_VIEW_DRAW",
              "Root component: " + (mCurrentRootComponent != null ? mCurrentRootComponent : "null"),
              t,
              0,
              null);
      throw new LithoMetadataExceptionWrapper(null, mCurrentRootComponent, mCurrentLogTag, t);
    }
  }

  @Override
  protected void onScrollChanged(int l, int t, int oldl, int oldt) {
    super.onScrollChanged(l, t, oldl, oldt);

    mLithoView.notifyVisibleBoundsChanged();

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
  public void obtainLithoViewChildren(List<BaseMountingView> lithoViews) {
    lithoViews.add(mLithoView);
  }

  public void setScrollStateListener(final @Nullable ScrollStateListener scrollStateListener) {
    if (scrollStateListener != null) {
      if (mScrollStateDetector == null) {
        mScrollStateDetector = new ScrollStateDetector(this);
      }
      mScrollStateDetector.setListener(scrollStateListener);
    } else if (mScrollStateDetector != null) {
      mScrollStateDetector.setListener(null);
    }
  }

  public void setScrollPosition(final @Nullable ScrollPosition scrollPosition) {
    if (scrollPosition != null) {
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
    } else {
      setScrollY(0);
      getViewTreeObserver().removeOnPreDrawListener(mOnPreDrawListener);
      mOnPreDrawListener = null;
    }
  }

  public void mount(final @Nullable LayoutState layoutState, final @Nullable TreeState state) {
    if (layoutState != null && state != null && mLithoView instanceof LithoRenderTreeView) {
      mCurrentRootComponent = layoutState.getRootName();
      mCurrentLogTag = layoutState.getComponentContext().getLogTag();
      ((LithoRenderTreeView) mLithoView).setLayoutState(layoutState, state);
    }
  }

  public void mount(
      ComponentTree contentComponentTree,
      final ScrollPosition scrollPosition,
      ScrollStateListener scrollStateListener) {
    if (!(mLithoView instanceof LithoView)) {
      throw new UnsupportedOperationException("API can only be invoked from Vertical Scroll Spec");
    }

    if (contentComponentTree != null) {
      final @Nullable Component component = contentComponentTree.getRoot();
      mCurrentRootComponent = component != null ? component.getSimpleName() : "null";
      mCurrentLogTag = contentComponentTree.getLogTag();
    }
    ((LithoView) mLithoView).setComponentTree(contentComponentTree);

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

  public void unmount() {
    if (!(mLithoView instanceof LithoView)) {
      throw new UnsupportedOperationException("API can only be invoked from Vertical Scroll Spec");
    }

    ((LithoView) mLithoView).setComponentTree(null, false);

    mScrollPosition = null;
    getViewTreeObserver().removeOnPreDrawListener(mOnPreDrawListener);
    mOnPreDrawListener = null;
    if (mScrollStateDetector != null) {
      mScrollStateDetector.setListener(null);
    }
  }

  public void release() {
    if (mLithoView instanceof LithoRenderTreeView) {
      ((LithoRenderTreeView) mLithoView).clean();
    } else {
      throw new UnsupportedOperationException(
          "This operation is only support for LithoRenderTreeView but it was : "
              + CommonUtils.getSectionNameForTracing(mLithoView.getClass()));
    }
  }

  public static class ScrollPosition {
    public int y;

    public ScrollPosition() {
      this(0);
    }

    public ScrollPosition(int initialScrollOffsetPixels) {
      y = initialScrollOffsetPixels;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ScrollPosition that = (ScrollPosition) o;
      return y == that.y;
    }

    @Override
    public int hashCode() {
      return y;
    }
  }

  public interface OnInterceptTouchListener {
    boolean onInterceptTouch(NestedScrollView nestedScrollView, MotionEvent event);
  }
}
