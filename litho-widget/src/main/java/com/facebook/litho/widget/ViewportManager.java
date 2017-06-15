/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import javax.annotation.concurrent.ThreadSafe;

import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.support.v7.widget.RecyclerView.ViewHolder;

/**
 * This class will handle all viewport changes due to both scrolling and
 * {@link ViewHolder} removal that is not related to scrolling.
 *
 * Classes that are interested to have its viewport changes handled by {@link ViewportManager}
 * should set the {@link OnScrollListener} returned from {@link ViewportManager#getScrollListener()}
 * in the {@link RecyclerView}
 */
@ThreadSafe
final class ViewportManager implements ViewportInfo {

  private volatile int mCurrentFirstVisiblePosition;
  private volatile int mCurrentLastVisiblePosition;
  private int mScrollingState;
  private @Nullable ViewportChanged mViewportChangedListener;

  private final @Nullable ViewportChangedWhileScrolling mViewportChangedWhileScrolling;
  private final LayoutInfo mLayoutInfo;
  private final Handler mMainThreadHandler;
  private final ViewportScrollListener mViewportScrollListener = new ViewportScrollListener();
  private final Runnable mViewportChangedRunnable = new Runnable() {
    @Override
    public void run() {
      onViewportChanged(null);
    }
  };

  /**
   * Implement this interface if you are interested to process your own work due to a
   * viewport change.
   */
  interface ViewportChangedWhileScrolling {
    void viewportChanged(int firstVisiblePosition, int lastVisiblePosition);
  }

  ViewportManager(
      int currentFirstVisiblePosition,
      int currentLastVisiblePosition,
      LayoutInfo layoutInfo,
      Handler mainThreadHandler,
      int initialScrollingState,
      @Nullable ViewportChangedWhileScrolling viewportChangedWhileScrolling) {
    mCurrentFirstVisiblePosition = currentFirstVisiblePosition;
    mCurrentLastVisiblePosition = currentLastVisiblePosition;
    mScrollingState = initialScrollingState;
    mLayoutInfo = layoutInfo;
    mMainThreadHandler = mainThreadHandler;
    mViewportChangedWhileScrolling = viewportChangedWhileScrolling;
  }

  /**
   * Handles a change in viewport. This method should not be called outside of the method
   * {@link OnScrollListener#onScrolled(RecyclerView, int, int)}
   *
   * @param handler
   */
  @UiThread
  void onViewportChanged(@Nullable ViewportChangedWhileScrolling handler) {
    final int firstVisiblePosition = mLayoutInfo.findFirstVisibleItemPosition();
    final int lastVisiblePosition = mLayoutInfo.findLastVisibleItemPosition();

    if (firstVisiblePosition < 0 || lastVisiblePosition < 0) {
      return;
    }

    if (firstVisiblePosition == mCurrentFirstVisiblePosition
        && lastVisiblePosition == mCurrentLastVisiblePosition
        && isScrolling()) {
      // firstVisiblePosition and lastVisiblePosition will remove unchanged if the viewport
      // change is due to a view removal without scrolling, and we need to notify a viewport
      // change for this case
      return;
    }

    setCurrentFirstVisiblePositionTo(firstVisiblePosition);
    setCurrentLastVisiblePosition(lastVisiblePosition);

    if (handler != null) {
      handler.viewportChanged(firstVisiblePosition, lastVisiblePosition);
    }

    if (mViewportChangedListener == null) {
      return;
    }

    mViewportChangedListener.viewportChanged(
        firstVisiblePosition,
        lastVisiblePosition,
        mLayoutInfo.findFirstFullyVisibleItemPosition(),
        mLayoutInfo.findLastFullyVisibleItemPosition());
  }

  /**
   * Handles a change in viewport when a View is removed from the RecyclerView.
   * This method does nothing if the removal is due to a scrolling event
   *
   * @param removedPosition
   */
  @UiThread
  void onViewportChangedAfterViewRemoval(int removedPosition) {
    if (isScrolling()) {
      // If the RecyclerView is scrolling we do not have to handle viewport changed notification
      return;
    }

    final boolean isRangeNotInitialised = mCurrentFirstVisiblePosition <= 0
        && mCurrentLastVisiblePosition <= 0;

    final boolean isPositionRemovedWithinRange = mCurrentFirstVisiblePosition <= removedPosition
        && removedPosition <= mCurrentLastVisiblePosition;

    if (isRangeNotInitialised || isPositionRemovedWithinRange) {
      putViewportChangedRunnableToEndOfUIThreadQueue();
    }
  }

  private void putViewportChangedRunnableToEndOfUIThreadQueue() {
    mMainThreadHandler.removeCallbacks(mViewportChangedRunnable);
    mMainThreadHandler.post(mViewportChangedRunnable);
  }

  @UiThread
  void setViewportChangedListener(@Nullable ViewportChanged viewportChangedListener) {
    mViewportChangedListener = viewportChangedListener;
  }

  @UiThread
  ViewportScrollListener getScrollListener() {
    return mViewportScrollListener;
  }

  @UiThread
  void setCurrentFirstVisiblePositionTo(int currentFirstVisiblePosition) {
    mCurrentFirstVisiblePosition = currentFirstVisiblePosition;
  }

  @UiThread
  void setCurrentLastVisiblePosition(int currentLastVisiblePosition) {
    mCurrentLastVisiblePosition = currentLastVisiblePosition;
  }

  @Override
  public int findFirstVisibleItemPosition() {
    return mCurrentFirstVisiblePosition;
  }

  @Override
  public int findLastVisibleItemPosition() {
    return mCurrentLastVisiblePosition;
  }

  @Override
  public int findFirstFullyVisibleItemPosition() {
    return mLayoutInfo.findFirstFullyVisibleItemPosition();
  }

  @Override
  public int findLastFullyVisibleItemPosition() {
    return mLayoutInfo.findLastFullyVisibleItemPosition();
  }

  @Override
  public int getItemCount() {
    return mLayoutInfo.getItemCount();
  }

  private boolean isScrolling() {
    return mScrollingState != RecyclerView.SCROLL_STATE_IDLE;
  }

  private class ViewportScrollListener extends RecyclerView.OnScrollListener {

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
      onViewportChanged(mViewportChangedWhileScrolling);
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
      mScrollingState = newState;
    }
  }
}
