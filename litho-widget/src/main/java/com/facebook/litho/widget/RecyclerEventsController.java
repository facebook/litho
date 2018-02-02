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

  private SectionsRecyclerView mSectionsRecyclerView;

  private final Runnable mClearRefreshRunnable = new Runnable() {
    @Override
    public void run() {
      if (mSectionsRecyclerView != null && mSectionsRecyclerView.isRefreshing()) {
        mSectionsRecyclerView.setRefreshing(false);
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
   *
   * @param animated if animated is set to true the scroll will happen with an animation.
   */
  public void requestScrollToPosition(final int position, final boolean animated) {
    if (mSectionsRecyclerView == null) {
      return;
    }

    if (animated) {
      mSectionsRecyclerView.getRecyclerView().smoothScrollToPosition(position);
      return;
    }

    mSectionsRecyclerView.getRecyclerView().scrollToPosition(position);
  }

  public void clearRefreshing() {
    if (mSectionsRecyclerView == null || !mSectionsRecyclerView.isRefreshing()) {
      return;
    }

    if (ThreadUtils.isMainThread()) {
      mSectionsRecyclerView.setRefreshing(false);
      return;
    }

    mSectionsRecyclerView.removeCallbacks(mClearRefreshRunnable);
    mSectionsRecyclerView.post(mClearRefreshRunnable);
  }

  public void showRefreshing() {
    if (mSectionsRecyclerView == null || mSectionsRecyclerView.isRefreshing()) {
      return;
    }
    ThreadUtils.assertMainThread();
    mSectionsRecyclerView.setRefreshing(true);
  }

  void setSectionsRecyclerView(SectionsRecyclerView SectionsrecyclerView) {
    mSectionsRecyclerView = SectionsrecyclerView;
  }

  protected @Nullable RecyclerView getRecyclerView() {
    return mSectionsRecyclerView == null ? null : mSectionsRecyclerView.getRecyclerView();
  }
}
