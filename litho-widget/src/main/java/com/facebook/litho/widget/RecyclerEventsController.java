/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.support.v7.widget.RecyclerView;

/**
 * An controller that can be passed as {@link com.facebook.litho.annotations.Prop} to a
 * Recycler component to trigger events from outside the component hierarchy.
 */
public class RecyclerEventsController {

  private RecyclerViewWrapper mRecyclerViewWrapper;

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
    if (mRecyclerViewWrapper != null) {
      mRecyclerViewWrapper.setRefreshing(false);
    }
  }

  void setRecyclerViewWrapper(RecyclerViewWrapper recyclerViewWrapper) {
    mRecyclerViewWrapper = recyclerViewWrapper;
  }
}
