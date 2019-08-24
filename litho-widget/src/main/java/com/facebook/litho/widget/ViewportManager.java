/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.widget;

import androidx.annotation.AnyThread;
import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.facebook.litho.widget.ViewportInfo.ViewportChanged;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.concurrent.ThreadSafe;

/**
 * This class will handle all viewport changes due to both scrolling and {@link ViewHolder} removal
 * that is not related to scrolling.
 *
 * <p>Classes that are interested to have its viewport changes handled by {@link ViewportManager}
 * should set the {@link OnScrollListener} returned from {@link ViewportManager#getScrollListener()}
 * in the {@link RecyclerView}
 */
@ThreadSafe
final class ViewportManager {

  private int mCurrentFirstVisiblePosition;
  private int mCurrentLastVisiblePosition;
  private int mCurrentFirstFullyVisiblePosition;
  private int mCurrentLastFullyVisiblePosition;
  private int mTotalItemCount;
  private boolean mShouldUpdate;

  @GuardedBy("this")
  private final List<ViewportChanged> mViewportChangedListeners;

  private final LayoutInfo mLayoutInfo;
  private final ViewportScrollListener mViewportScrollListener = new ViewportScrollListener();

  ViewportManager(
      int currentFirstVisiblePosition, int currentLastVisiblePosition, LayoutInfo layoutInfo) {
    mCurrentFirstVisiblePosition = currentFirstVisiblePosition;
    mCurrentLastVisiblePosition = currentLastVisiblePosition;
    mCurrentFirstFullyVisiblePosition = layoutInfo.findFirstFullyVisibleItemPosition();
    mCurrentLastFullyVisiblePosition = layoutInfo.findLastFullyVisibleItemPosition();
    mTotalItemCount = layoutInfo.getItemCount();
    mLayoutInfo = layoutInfo;
    mViewportChangedListeners = new ArrayList<>(2);
  }

  /**
   * Handles a change in viewport. This method should not be called outside of the method {@link
   * OnScrollListener#onScrolled(RecyclerView, int, int)}
   */
  @UiThread
  void onViewportChanged(@ViewportInfo.State int state) {
    final int firstVisiblePosition = mLayoutInfo.findFirstVisibleItemPosition();
    final int lastVisiblePosition = mLayoutInfo.findLastVisibleItemPosition();
    final int firstFullyVisibleItemPosition = mLayoutInfo.findFirstFullyVisibleItemPosition();
    final int lastFullyVisibleItemPosition = mLayoutInfo.findLastFullyVisibleItemPosition();
    final int totalItemCount = mLayoutInfo.getItemCount();

    if (firstVisiblePosition < 0 || lastVisiblePosition < 0) {
      return;
    }

    if (firstVisiblePosition == mCurrentFirstVisiblePosition
        && lastVisiblePosition == mCurrentLastVisiblePosition
        && firstFullyVisibleItemPosition == mCurrentFirstFullyVisiblePosition
        && lastFullyVisibleItemPosition == mCurrentLastFullyVisiblePosition
        && totalItemCount == mTotalItemCount
        && state != ViewportInfo.State.DATA_CHANGES) {
      return;
    }

    mCurrentFirstVisiblePosition = firstVisiblePosition;
    mCurrentLastVisiblePosition = lastVisiblePosition;
    mCurrentFirstFullyVisiblePosition = firstFullyVisibleItemPosition;
    mCurrentLastFullyVisiblePosition = lastFullyVisibleItemPosition;
    mTotalItemCount = totalItemCount;
    mShouldUpdate = false;

    final List<ViewportChanged> viewportChangedListeners;
    synchronized (this) {
      if (mViewportChangedListeners.isEmpty()) {
        return;
      }
      viewportChangedListeners = new ArrayList<>(mViewportChangedListeners);
    }

    for (int i = 0, size = viewportChangedListeners.size(); i < size; i++) {
      final ViewportChanged viewportChangedListener = viewportChangedListeners.get(i);
      viewportChangedListener.viewportChanged(
          firstVisiblePosition,
          lastVisiblePosition,
          firstFullyVisibleItemPosition,
          lastFullyVisibleItemPosition,
          state);
    }
  }

  @UiThread
  void setShouldUpdate(boolean shouldUpdate) {
    mShouldUpdate = mShouldUpdate || shouldUpdate;
  }

  @UiThread
  void resetShouldUpdate() {
    mShouldUpdate = false;
  }

  @UiThread
  boolean insertAffectsVisibleRange(int position, int size, int viewportCount) {
    if (shouldUpdate() || viewportCount == RecyclerBinder.UNSET) {
      return true;
    }

    final int lastVisiblePosition =
        Math.max(mCurrentFirstVisiblePosition + viewportCount - 1, mCurrentLastVisiblePosition);

    return position <= lastVisiblePosition;
  }

  @UiThread
  boolean updateAffectsVisibleRange(int position, int size) {
    if (shouldUpdate()) {
      return true;
    }

    for (int index = position; index < position + size; ++index) {
      if (mCurrentFirstVisiblePosition <= index && index <= mCurrentLastVisiblePosition) {
        return true;
      }
    }

    return false;
  }

  @UiThread
  boolean moveAffectsVisibleRange(int fromPosition, int toPosition, int viewportCount) {
    if (shouldUpdate() || viewportCount == RecyclerBinder.UNSET) {
      return true;
    }

    final boolean isNewPositionInVisibleRange =
        toPosition >= mCurrentFirstVisiblePosition
            && toPosition <= mCurrentFirstVisiblePosition + viewportCount - 1;

    final boolean isOldPositionInVisibleRange =
        fromPosition >= mCurrentFirstVisiblePosition
            && fromPosition <= mCurrentFirstVisiblePosition + viewportCount - 1;

    return isNewPositionInVisibleRange || isOldPositionInVisibleRange;
  }

  @UiThread
  boolean removeAffectsVisibleRange(int position, int size) {
    if (shouldUpdate()) {
      return true;
    }

    return position <= mCurrentLastVisiblePosition;
  }

  /**
   * Whether first/last visible positions should be updated. If this returns true, we should not do
   * any computations based on current first/last visible positions until they are updated.
   */
  @UiThread
  boolean shouldUpdate() {
    return mCurrentFirstVisiblePosition < 0 || mCurrentLastVisiblePosition < 0 || mShouldUpdate;
  }

  @AnyThread
  void addViewportChangedListener(@Nullable ViewportChanged viewportChangedListener) {
    if (viewportChangedListener == null) {
      return;
    }

    synchronized (this) {
      mViewportChangedListeners.add(viewportChangedListener);
    }
  }

  @AnyThread
  void removeViewportChangedListener(@Nullable ViewportChanged viewportChangedListener) {
    if (viewportChangedListener == null) {
      return;
    }

    synchronized (this) {
      if (mViewportChangedListeners.isEmpty()) {
        return;
      }

      mViewportChangedListeners.remove(viewportChangedListener);
    }
  }

  @UiThread
  ViewportScrollListener getScrollListener() {
    return mViewportScrollListener;
  }

  private class ViewportScrollListener extends RecyclerView.OnScrollListener {

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
      onViewportChanged(ViewportInfo.State.SCROLLING);
    }
  }
}
