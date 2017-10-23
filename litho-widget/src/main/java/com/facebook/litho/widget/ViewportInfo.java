/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.support.annotation.IntDef;

/**
 * An implementation of this interface will provide both the
 * {@link android.support.v7.widget.RecyclerView}'s current visible views position and
 * the total number of items in its {@link android.support.v7.widget.RecyclerView.Adapter}.
 */
public interface ViewportInfo {

  /**
   * @return the adapter position of the first visible view.
   */
  int findFirstVisibleItemPosition();

  /**
   * @return the adapter position of the last visible view.
   */
  int findLastVisibleItemPosition();

  /**
   * @return the adapter position of the first fully visible view.
   */
  int findFirstFullyVisibleItemPosition();

  /**
   * @return the adapter position of the last fully visible view.
   */
  int findLastFullyVisibleItemPosition();

  /**
   * @return total number of items in the adapter
   */
  int getItemCount();

  /**
   * Implement this interface to be notified of Viewport changes from the {@link Binder}
   */
  interface ViewportChanged {
    void viewportChanged(
        int firstVisibleIndex,
        int lastVisibleIndex,
        int firstFullyVisibleIndex,
        int lastFullyVisibleIndex,
        @State int state);
  }

  @IntDef({State.SCROLLING, State.DATA_CHANGES})
  @interface State {
    int SCROLLING = 0;
    int DATA_CHANGES = 1;
  }
}
