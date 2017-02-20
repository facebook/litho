// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.widget;

import android.support.v7.widget.RecyclerView;

/**
 * An controller that can be passed as {@link com.facebook.components.annotations.Prop} to a
 * Recycler component to trigger events from outside the component hierarchy.
 */
public class RecyclerEventsController {

  private RecyclerView mRecyclerView;

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
    if (mRecyclerView != null) {
      if (animated) {
        mRecyclerView.smoothScrollToPosition(position);
      } else {
        mRecyclerView.scrollToPosition(position);
      }
    }
  }

  void setRecyclerView(RecyclerView recyclerView) {
    mRecyclerView = recyclerView;
  }
}
