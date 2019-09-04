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

package com.facebook.litho.sections.widget;

import static com.facebook.litho.widget.SnapUtil.SNAP_NONE;
import static com.facebook.litho.widget.SnapUtil.SNAP_TO_CENTER;
import static com.facebook.litho.widget.SnapUtil.SNAP_TO_CENTER_CHILD;
import static com.facebook.litho.widget.SnapUtil.SNAP_TO_END;
import static com.facebook.litho.widget.SnapUtil.SNAP_TO_START;

import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.facebook.litho.sections.SectionTree;
import com.facebook.litho.widget.RecyclerEventsController;
import com.facebook.litho.widget.SmoothScrollAlignmentType;
import com.facebook.litho.widget.SnapUtil;
import com.facebook.litho.widget.StaggeredGridLayoutHelper;
import javax.annotation.Nullable;

/**
 * An controller that can be passed as {@link com.facebook.litho.annotations.Prop} to a
 * RecyclerCollection component to trigger events from outside the component hierarchy.
 */
public class RecyclerCollectionEventsController extends RecyclerEventsController {

  @Nullable private SectionTree mSectionTree;
  private int mSnapMode = SNAP_NONE;
  private int mFirstCompletelyVisibleItemPosition = 0;
  private int mLastCompletelyVisibleItemPosition = 0;

  /** Sent the RecyclerCollection a request to refresh it's backing data. */
  public void requestRefresh() {
    requestRefresh(false);
  }

  /**
   * Sent the RecyclerCollection a request to refresh it's backing data. If showSpinner is true,
   * then refresh spinner is shown.
   *
   * @param showSpinner
   */
  public void requestRefresh(boolean showSpinner) {
    if (mSectionTree != null) {
      if (showSpinner) {
        showRefreshing();
      }

      mSectionTree.refresh();
    }
  }

  /**
   * Send the {@link RecyclerCollectionComponent} a request to scroll the content to the next item
   * in the binder.
   *
   * @param animated whether the scroll will happen with animation.
   */
  public void requestScrollToNextPosition(boolean animated) {
    final int defaultTarget = Math.max(0, mLastCompletelyVisibleItemPosition + 1);
    final int snapTarget = getSmoothScrollTarget(true, defaultTarget);
    requestScrollToPositionInner(animated, defaultTarget, snapTarget);
  }

  /**
   * Send the {@link RecyclerCollectionComponent} a request to scroll the content to the previous
   * item in the binder.
   *
   * @param animated whether the scroll will happen with animation.
   */
  public void requestScrollToPreviousPosition(boolean animated) {
    final int defaultTarget = Math.max(0, mFirstCompletelyVisibleItemPosition - 1);
    final int snapTarget = getSmoothScrollTarget(false, defaultTarget);
    requestScrollToPositionInner(animated, defaultTarget, snapTarget);
  }

  /**
   * Send the {@link RecyclerCollectionComponent} a request to scroll the content to the given
   * target position taking into account snapping behavior.
   *
   * <p>For example, if the content is center snapped, then the {@link RecyclerView} will scroll
   * contents until the target position is visible at the center (as opposed to the edge of {@link
   * RecyclerView} which is the case when using {@link #requestScrollToPosition(int, boolean)}).
   */
  public void requestScrollToPositionWithSnap(final int target) {
    requestScrollToPositionInner(true, target, target);
  }

  /**
   * Send the {@link RecyclerCollectionComponent} a request to scroll the content to the given
   * target position taking into account provided snapping behavior. The provided smoothScroller is
   * used to scroll to the target.
   */
  public void requestScrollToPositionWithSnap(
      final int target, final RecyclerView.SmoothScroller smoothScroller) {
    requestScrollToPositionInner(true, target, target, smoothScroller);
  }

  /**
   * Send the {@link RecyclerCollectionComponent} a request to scroll the content by the given
   * margins.
   */
  public void requestScrollBy(int dx, int dy) {
    final RecyclerView recyclerView = getRecyclerView();
    if (recyclerView == null) {
      return;
    }

    recyclerView.scrollBy(dx, dy);
  }

  /**
   * @param animated whether the scroll will happen with animation.
   * @param defaultTarget target to use as fallback.
   * @param snapTarget target that takes into account snapping behavior.
   */
  private void requestScrollToPositionInner(boolean animated, int defaultTarget, int snapTarget) {
    requestScrollToPositionInner(animated, defaultTarget, snapTarget, null);
  }

  /**
   * @param animated whether the scroll will happen with animation.
   * @param defaultTarget target to use as fallback.
   * @param snapTarget target that takes into account snapping behavior.
   * @param smoothScroller custom smooth scroller
   */
  private void requestScrollToPositionInner(
      boolean animated,
      int defaultTarget,
      int snapTarget,
      RecyclerView.SmoothScroller smoothScroller) {
    final RecyclerView recyclerView = getRecyclerView();
    if (recyclerView == null) {
      return;
    }

    final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
    if (layoutManager == null || recyclerView.isLayoutFrozen()) {
      return;
    }

    if (!animated) {
      requestScrollToPosition(defaultTarget, false);
      return;
    }

    if (smoothScroller == null && mSnapMode == SNAP_NONE) {
      requestScrollToPosition(defaultTarget, true);
      return;
    }

    if (smoothScroller == null) {
      smoothScroller =
          SnapUtil.getSmoothScrollerWithOffset(
              recyclerView.getContext(), 0, getSmoothScrollAlignmentTypeFrom(mSnapMode));
    }

    smoothScroller.setTargetPosition(snapTarget);
    layoutManager.startSmoothScroll(smoothScroller);
  }

  private int getSmoothScrollTarget(boolean forward, int defaultTarget) {
    switch (mSnapMode) {
      case SNAP_NONE:
        return defaultTarget;
      case SNAP_TO_START:
        return Math.max(
            0,
            forward
                ? mFirstCompletelyVisibleItemPosition + 1
                : mFirstCompletelyVisibleItemPosition - 1);
      case SNAP_TO_END: // SNAP_TO_END not yet implemented
        return Math.max(
            0,
            forward
                ? mLastCompletelyVisibleItemPosition + 1
                : mLastCompletelyVisibleItemPosition - 1);
      case SNAP_TO_CENTER:
      case SNAP_TO_CENTER_CHILD:
        final RecyclerView recyclerView = getRecyclerView();
        if (recyclerView == null) {
          return defaultTarget;
        }
        final int centerPositionX = recyclerView.getWidth() / 2;
        final int centerPositionY = recyclerView.getHeight() / 2;
        final View centerView = recyclerView.findChildViewUnder(centerPositionX, centerPositionY);
        if (centerView == null) {
          return defaultTarget;
        }
        final int centerViewPosition = recyclerView.getChildAdapterPosition(centerView);
        return Math.max(0, forward ? centerViewPosition + 1 : centerViewPosition - 1);
      default:
        return defaultTarget;
    }
  }

  private static SmoothScrollAlignmentType getSmoothScrollAlignmentTypeFrom(int snapMode) {
    switch (snapMode) {
      case SNAP_TO_START:
        return SmoothScrollAlignmentType.SNAP_TO_START;
      case SNAP_TO_END: // SNAP_TO_END not yet implemented
        return SmoothScrollAlignmentType.SNAP_TO_END;
      case SNAP_TO_CENTER:
      case SNAP_TO_CENTER_CHILD:
        return SmoothScrollAlignmentType.SNAP_TO_CENTER;
      default:
        return SmoothScrollAlignmentType.DEFAULT;
    }
  }

  void setSectionTree(SectionTree sectionTree) {
    mSectionTree = sectionTree;
  }

  void setSnapMode(int snapMode) {
    mSnapMode = snapMode;
  }

  private static int getFirstCompletelyVisibleItemPosition(
      RecyclerView.LayoutManager layoutManager) {
    if (layoutManager instanceof StaggeredGridLayoutManager) {
      return StaggeredGridLayoutHelper.findFirstFullyVisibleItemPosition(
          (StaggeredGridLayoutManager) layoutManager);
    } else {
      return ((LinearLayoutManager) layoutManager).findFirstCompletelyVisibleItemPosition();
    }
  }

  private static int getLastCompletelyVisibleItemPosition(
      RecyclerView.LayoutManager layoutManager) {
    if (layoutManager instanceof StaggeredGridLayoutManager) {
      return StaggeredGridLayoutHelper.findLastFullyVisibleItemPosition(
          (StaggeredGridLayoutManager) layoutManager);
    } else {
      return ((LinearLayoutManager) layoutManager).findLastCompletelyVisibleItemPosition();
    }
  }

  public void updateFirstLastFullyVisibleItemPositions(RecyclerView.LayoutManager layoutManager) {
    final int firstCompletelyVisibleItemPosition =
        getFirstCompletelyVisibleItemPosition(layoutManager);
    if (firstCompletelyVisibleItemPosition != -1) {
      // firstCompletelyVisibleItemPosition can be -1 in middle of the scroll, so
      // wait until it finishes to set the state.
      mFirstCompletelyVisibleItemPosition = firstCompletelyVisibleItemPosition;
    }

    final int lastCompletelyVisibleItemPosition =
        getLastCompletelyVisibleItemPosition(layoutManager);
    if (lastCompletelyVisibleItemPosition != -1) {
      mLastCompletelyVisibleItemPosition = lastCompletelyVisibleItemPosition;
    }
  }
}
