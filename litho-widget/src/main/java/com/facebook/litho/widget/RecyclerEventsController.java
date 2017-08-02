/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import com.facebook.litho.ThreadUtils;

/**
 * An controller that can be passed as {@link com.facebook.litho.annotations.Prop} to a
 * Recycler component to trigger events from outside the component hierarchy.
 */
public class RecyclerEventsController {

  private RecyclerViewWrapper mRecyclerViewWrapper;

  private final Runnable mClearRefreshRunnable = new Runnable() {
    @Override
    public void run() {
      if (mRecyclerViewWrapper != null && mRecyclerViewWrapper.isRefreshing()) {
        mRecyclerViewWrapper.setRefreshing(false);
      }
    }
  };

  /**
   * Send the Recycler a request to scroll the content to the first item in the binder.
   * @param animated if animated is set to true the scroll will happen with an animation.
   */
  public void requestScrollToTop(boolean animated) {
    requestScrollToPosition(0, animated);
  }

  /**
   * Send the Recycler a request to scroll the content to a specific item in the binder.
   * @param animated if animated is set to true the scroll will happen with an animation.
   */
  public void requestScrollToPosition(int position, boolean animated) {
    if (mRecyclerViewWrapper != null) {
      if (animated) {
        mRecyclerViewWrapper.getRecyclerView().smoothScrollToPosition(position);
      } else {
        mRecyclerViewWrapper.getRecyclerView().scrollToPosition(position);
      }
    }
  }

  public void clearRefreshing() {
    if (mRecyclerViewWrapper == null || !mRecyclerViewWrapper.isRefreshing()) {
      return;
    }

    if (ThreadUtils.isMainThread()) {
      mRecyclerViewWrapper.setRefreshing(false);
      return;
    }

    mRecyclerViewWrapper.removeCallbacks(mClearRefreshRunnable);
    mRecyclerViewWrapper.post(mClearRefreshRunnable);
  }

  public void showRefreshing() {
    if (mRecyclerViewWrapper == null || mRecyclerViewWrapper.isRefreshing()) {
      return;
    }
    ThreadUtils.assertMainThread();
    mRecyclerViewWrapper.setRefreshing(true);
  }

  void setRecyclerViewWrapper(RecyclerViewWrapper recyclerViewWrapper) {
    mRecyclerViewWrapper = recyclerViewWrapper;
  }

  protected @Nullable RecyclerView getRecyclerView() {
    return mRecyclerViewWrapper == null ? null : mRecyclerViewWrapper.getRecyclerView();
  }
}
