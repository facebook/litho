// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.widget;

import android.support.v4.view.ViewCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;

import com.facebook.components.utils.IncrementalMountUtils;

// TODO 16248993 make this more generic.
public class IncrementalMountDefaultItemAnimator extends DefaultItemAnimator {

  private RecyclerView mRecyclerView;

  private final Runnable mIncrementalMountRunnable = new Runnable() {
    @Override
    public void run() {
      if (mRecyclerView != null && isRunning()) {
        IncrementalMountUtils.performIncrementalMount(mRecyclerView);
        ViewCompat.postOnAnimation(mRecyclerView, mIncrementalMountRunnable);
      } else {
        mRecyclerView = null;
      }
    }
  };

  @Override
  public void onMoveStarting(RecyclerView.ViewHolder item) {
    super.onMoveStarting(item);
    mRecyclerView = (RecyclerView) item.itemView.getParent();
    ViewCompat.postOnAnimation(mRecyclerView, mIncrementalMountRunnable);
  }
}
