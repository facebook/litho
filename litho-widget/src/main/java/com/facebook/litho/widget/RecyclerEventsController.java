/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.litho.ThreadUtils;

/**
 * An controller that can be passed as {@link com.facebook.litho.annotations.Prop} to a Recycler
 * component to trigger events from outside the component hierarchy.
 */
public class RecyclerEventsController {

  public interface OnRecyclerUpdateListener {

    void onUpdate(@Nullable RecyclerView recyclerView);
  }

  @Nullable private SectionsRecyclerView mSectionsRecyclerView;

  @Nullable private OnRecyclerUpdateListener mOnRecyclerUpdateListener;

  private final Runnable mClearRefreshRunnable =
      new Runnable() {
        @Override
        public void run() {
          SectionsRecyclerView sectionsRecyclerView = mSectionsRecyclerView;
          if (sectionsRecyclerView != null && sectionsRecyclerView.isRefreshing()) {
            sectionsRecyclerView.setRefreshing(false);
          }
        }
      };

  /**
   * Send the Recycler a request to scroll the content to the first item in the binder.
   *
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
    SectionsRecyclerView sectionsRecyclerView = mSectionsRecyclerView;
    if (sectionsRecyclerView == null) {
      return;
    }

    if (animated) {
      sectionsRecyclerView.getRecyclerView().smoothScrollToPosition(position);
      return;
    }

    sectionsRecyclerView.getRecyclerView().scrollToPosition(position);
  }

  /**
   * Send the Recycler a request to scroll the content to a specific item in the binder with the
   * given offset from resolved layout start. Animation will not be performed.
   *
   * <p>If you are just trying to make a position visible, use {@link #requestScrollToPosition(int,
   * boolean)}.
   *
   * <p>Note: This offset is valid for LinearLayout only!
   *
   * @param position Index (starting at 0) of the reference item.
   * @param offset The distance (in pixels) between the start edge of the item view and start edge
   *     of the RecyclerView.
   */
  public void requestScrollToPositionWithOffset(final int position, final int offset) {
    SectionsRecyclerView sectionsRecyclerView = mSectionsRecyclerView;
    if (sectionsRecyclerView == null) {
      return;
    }

    RecyclerView.LayoutManager layoutManager =
        sectionsRecyclerView.getRecyclerView().getLayoutManager();
    if (!(layoutManager instanceof LinearLayoutManager)) {
      requestScrollToPosition(position, /* animated */ false);
      return;
    }

    ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(position, offset);
  }

  public void clearRefreshing() {
    SectionsRecyclerView sectionsRecyclerView = mSectionsRecyclerView;
    if (sectionsRecyclerView == null || !sectionsRecyclerView.isRefreshing()) {
      return;
    }

    if (ThreadUtils.isMainThread()) {
      sectionsRecyclerView.setRefreshing(false);
      return;
    }

    sectionsRecyclerView.removeCallbacks(mClearRefreshRunnable);
    sectionsRecyclerView.post(mClearRefreshRunnable);
  }

  public void showRefreshing() {
    SectionsRecyclerView sectionsRecyclerView = mSectionsRecyclerView;
    if (sectionsRecyclerView == null || sectionsRecyclerView.isRefreshing()) {
      return;
    }
    ThreadUtils.assertMainThread();
    sectionsRecyclerView.setRefreshing(true);
  }

  void setSectionsRecyclerView(@Nullable SectionsRecyclerView sectionsRecyclerView) {
    mSectionsRecyclerView = sectionsRecyclerView;

    if (mOnRecyclerUpdateListener != null) {
      mOnRecyclerUpdateListener.onUpdate(
          sectionsRecyclerView == null ? null : sectionsRecyclerView.getRecyclerView());
    }
  }

  public @Nullable RecyclerView getRecyclerView() {
    SectionsRecyclerView sectionsRecyclerView = mSectionsRecyclerView;
    return sectionsRecyclerView == null ? null : sectionsRecyclerView.getRecyclerView();
  }

  public void setOnRecyclerUpdateListener(
      @Nullable OnRecyclerUpdateListener onRecyclerUpdateListener) {
    mOnRecyclerUpdateListener = onRecyclerUpdateListener;
  }
}
