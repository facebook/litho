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
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.HasLithoViewChildren;
import com.facebook.litho.LithoView;
import com.facebook.litho.SizeSpec;
import java.util.List;

/**
 * Wrapper that encapsulates all the features {@link RecyclerSpec} provides such as sticky header
 * and pull-to-refresh
 */
public class SectionsRecyclerView extends SwipeRefreshLayout implements HasLithoViewChildren {

  private final LithoView mStickyHeader;
  private final RecyclerView mRecyclerView;
  /**
   * Indicates whether {@link RecyclerView} has been detached. In such case we need to make sure
   * to relayout its children eventually.
   */
  private boolean mHasBeenDetachedFromWindow = false;

  public SectionsRecyclerView(Context context, RecyclerView recyclerView) {
    super(context);

    mRecyclerView = recyclerView;

    // We need to draw first visible item on top of other children to support sticky headers
    mRecyclerView.setChildDrawingOrderCallback(new RecyclerView.ChildDrawingOrderCallback() {
      @Override
      public int onGetChildDrawingOrder(int childCount, int i) {
        return childCount - 1 - i;
      }
    });

    addView(mRecyclerView);
    mStickyHeader = new LithoView(new ComponentContext(getContext()), null);
    mStickyHeader.setLayoutParams(new ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));

    addView(mStickyHeader);
  }

  public RecyclerView getRecyclerView() {
    return mRecyclerView;
  }

  public void setStickyComponent(ComponentTree component) {
    if (component.getLithoView() != null) {
      component.getLithoView().startTemporaryDetach();
    }
    mStickyHeader.setComponentTree(component);
    measureStickyHeader(getWidth());
  }

  public LithoView getStickyHeader() {
    return mStickyHeader;
  }

  public void setStickyHeaderVerticalOffset(int verticalOffset) {
    mStickyHeader.setTranslationY(verticalOffset);
  }

  public void showStickyHeader() {
    mStickyHeader.setVisibility(View.VISIBLE);
  }

  public void hideStickyHeader() {
    mStickyHeader.setVisibility(View.GONE);
  }

  public boolean isStickyHeaderHidden() {
    return mStickyHeader.getVisibility() == View.GONE;
  }

  @Override
  public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    measureStickyHeader(MeasureSpec.getSize(widthMeasureSpec));
  }

  private void measureStickyHeader(int parentWidth) {
    measureChild(
        mStickyHeader,
        SizeSpec.makeSizeSpec(parentWidth, SizeSpec.EXACTLY),
        MeasureSpec.UNSPECIFIED);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);

    if (mStickyHeader.getVisibility() == View.GONE) {
      return;
    }

    final int stickyHeaderLeft = getPaddingLeft();
    final int stickyHeaderTop = getPaddingTop();
    mStickyHeader.layout(
        stickyHeaderLeft,
        stickyHeaderTop,
        stickyHeaderLeft + mStickyHeader.getMeasuredWidth(),
        stickyHeaderTop + mStickyHeader.getMeasuredHeight());
  }

  static SectionsRecyclerView getParentRecycler(RecyclerView recyclerView) {
    if (recyclerView.getParent() instanceof SectionsRecyclerView) {
      return (SectionsRecyclerView) recyclerView.getParent();
    }
    return null;
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    mHasBeenDetachedFromWindow = true;
  }

  /**
   * This is needed to solve a launch-blocker t14789523 and work around a framework bug t14809560.
   */
  @Override
  public boolean isLayoutRequested() {
    if (getParent() != null) {
      return getParent().isLayoutRequested() || super.isLayoutRequested();
    }
    return super.isLayoutRequested();
  }

  boolean hasBeenDetachedFromWindow() {
    return mHasBeenDetachedFromWindow;
  }

  void setHasBeenDetachedFromWindow(boolean hasBeenDetachedFromWindow) {
    mHasBeenDetachedFromWindow = hasBeenDetachedFromWindow;
  }

  @Override
  public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    super.requestDisallowInterceptTouchEvent(disallowIntercept);

    // SwipeRefreshLayout can ignore this request if nested scrolling is disabled on the child,
    // but it fails to delegate the request up to the parents.
    // This fixes a bug that can cause parents to improperly intercept scroll events from
    // nested recyclers.
    if (getParent() != null && !isNestedScrollingEnabled()) {
      getParent().requestDisallowInterceptTouchEvent(disallowIntercept);
    }
  }

  @Override
  public void setOnTouchListener(OnTouchListener listener) {
    // When setting touch handler for RecyclerSpec we want RecyclerView to handle it.
    mRecyclerView.setOnTouchListener(listener);
  }

  @Override
  public void obtainLithoViewChildren(List<LithoView> lithoViews) {
    for (int i = 0, size = mRecyclerView.getChildCount(); i < size; i++) {
      final View child = mRecyclerView.getChildAt(i);
      if (child instanceof LithoView) {
        lithoViews.add((LithoView) child);
      }
    }
  }
}
