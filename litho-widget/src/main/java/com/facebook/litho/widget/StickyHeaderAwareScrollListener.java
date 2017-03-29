/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.ComponentView;

/**
 * Scroll listener that handles sticky header logic, such as using list item vs. list wrapper's item
 * as sticky header, visibility changes between them and calculation of translation amount
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class StickyHeaderAwareScrollListener extends RecyclerView.OnScrollListener {

  private final RecyclerComponentBinder mRecyclerComponentBinder;
  private final RecyclerView.LayoutManager mLayoutManager;
  private final RecyclerViewWrapper mRecyclerViewWrapper;
  private final ComponentContext mComponentContext;

  private View lastTranslatedView;
  private int previousStickyHeaderPosition = RecyclerView.NO_POSITION;

  StickyHeaderAwareScrollListener(
      ComponentContext componentContext,
      RecyclerComponentBinder recyclerComponentBinder,
      RecyclerViewWrapper recyclerViewWrapper) {
    mComponentContext = componentContext;
    mRecyclerComponentBinder = recyclerComponentBinder;
    mLayoutManager = recyclerComponentBinder.getLayoutManager();
    mRecyclerViewWrapper = recyclerViewWrapper;
    mRecyclerViewWrapper.hideStickyHeader();
  }

  @Override
  public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
    final int firstVisiblePosition =
        ((HasStickyHeader) mRecyclerComponentBinder).findFirstVisibleItemPosition();
    final int stickyHeaderPosition = findStickyHeaderPosition(firstVisiblePosition);

    final ComponentTree firstVisibleItemComponentTree =
        mRecyclerComponentBinder.getComponentAt(firstVisiblePosition);

    if (lastTranslatedView != null
        && firstVisibleItemComponentTree != null
        && lastTranslatedView != firstVisibleItemComponentTree.getComponentView()) {
      // Reset previously modified view
      lastTranslatedView.setTranslationY(0);
      lastTranslatedView = null;
    }

    if (stickyHeaderPosition == RecyclerView.NO_POSITION || firstVisibleItemComponentTree == null) {
      // no sticky header above first visible position, reset the state
      mRecyclerViewWrapper.hideStickyHeader();
      previousStickyHeaderPosition = RecyclerView.NO_POSITION;
      return;
    }

    final ComponentView firstVisibleView = firstVisibleItemComponentTree.getComponentView();

    if (firstVisiblePosition == stickyHeaderPosition) {
      // Translate first child, no need for sticky header
      firstVisibleView.setTranslationY(-firstVisibleView.getTop());
      lastTranslatedView = firstVisibleView;
      mRecyclerViewWrapper.hideStickyHeader();
      previousStickyHeaderPosition = RecyclerView.NO_POSITION;
    } else {

      if (mRecyclerViewWrapper.isStickyHeaderHidden()
          || stickyHeaderPosition != previousStickyHeaderPosition) {
        initStickyHeader(stickyHeaderPosition);
        mRecyclerViewWrapper.showStickyHeader();
      }

      // Translate sticky header
      final int lastVisiblePosition =
          ((HasStickyHeader) mRecyclerComponentBinder).findLastVisibleItemPosition();
      int translationY = 0;
      for (int i = firstVisiblePosition; i <= lastVisiblePosition; i++) {
        if (((HasStickyHeader) mRecyclerComponentBinder).isSticky(i)) {
          final View nextStickyHeader = mLayoutManager.findViewByPosition(i);
          final int offsetBetweenStickyHeaders = nextStickyHeader.getTop()
              - mRecyclerViewWrapper.getStickyHeader().getBottom()
              + mRecyclerViewWrapper.getPaddingTop();
          translationY = Math.min(offsetBetweenStickyHeaders, 0);
          break;
        }
      }
      mRecyclerViewWrapper.setVerticalOffset(translationY);
      previousStickyHeaderPosition = stickyHeaderPosition;
    }
  }

  private void initStickyHeader(int stickyHeaderPosition) {
    mRecyclerViewWrapper.setStickyComponent(
        mRecyclerComponentBinder.getComponentAt(stickyHeaderPosition));
  }

  private int findStickyHeaderPosition(int currentFirstVisiblePosition) {
    for (int i = currentFirstVisiblePosition; i >= 0; i--) {
      if (((HasStickyHeader) mRecyclerComponentBinder).isSticky(i)) {
        return i;
      }
    }
    return RecyclerView.NO_POSITION;
  }
}
