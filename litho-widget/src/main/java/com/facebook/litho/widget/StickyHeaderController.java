/*
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
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.facebook.litho.ComponentTree;
import com.facebook.litho.LithoView;

/**
 * Controller that handles sticky header logic. Depending on where the sticky item is located in the
 * list, we might either use first child as sticky header or use {@link RecyclerViewWrapper}'s
 * sticky header.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class StickyHeaderController extends RecyclerView.OnScrollListener {

  static final String WRAPPER_ARGUMENT_NULL = "Cannot initialize with null RecyclerViewWrapper.";
  static final String WRAPPER_ALREADY_INITIALIZED =
          "RecyclerViewWrapper has already been initialized but never reset.";
  static final String WRAPPER_NOT_INITIALIZED = "RecyclerViewWrapper has not been set yet.";
  static final String LAYOUTMANAGER_NOT_INITIALIZED =
      "LayoutManager of RecyclerView is not initialized yet.";

  private final HasStickyHeader mHasStickyHeader;

  private RecyclerViewWrapper mRecyclerViewWrapper;
  private RecyclerView.LayoutManager mLayoutManager;
  private View lastTranslatedView;
  private int previousStickyHeaderPosition = RecyclerView.NO_POSITION;

  StickyHeaderController(HasStickyHeader hasStickyHeader) {
    mHasStickyHeader = hasStickyHeader;
  }

  void init(RecyclerViewWrapper recyclerViewWrapper) {
    if (recyclerViewWrapper == null) {
      throw new RuntimeException(WRAPPER_ARGUMENT_NULL);
    }

    if (mRecyclerViewWrapper != null) {
      throw new RuntimeException(WRAPPER_ALREADY_INITIALIZED);
    }

    mRecyclerViewWrapper = recyclerViewWrapper;
    mRecyclerViewWrapper.hideStickyHeader();
    mLayoutManager = recyclerViewWrapper.getRecyclerView().getLayoutManager();
    if (mLayoutManager == null) {
      throw new RuntimeException(LAYOUTMANAGER_NOT_INITIALIZED);
    }

    mRecyclerViewWrapper.getRecyclerView().addOnScrollListener(this);
  }

  void reset() {
    if (mRecyclerViewWrapper == null) {
      throw new IllegalStateException(WRAPPER_NOT_INITIALIZED);
    }

    mRecyclerViewWrapper.getRecyclerView().removeOnScrollListener(this);
    mLayoutManager = null;
    mRecyclerViewWrapper = null;
  }

  @Override
  public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
    final int firstVisiblePosition = mHasStickyHeader.findFirstVisibleItemPosition();

    if (firstVisiblePosition == RecyclerView.NO_POSITION) {
      return;
    }

    final int stickyHeaderPosition = findStickyHeaderPosition(firstVisiblePosition);
    final ComponentTree firstVisibleItemComponentTree =
        mHasStickyHeader.getComponentAt(firstVisiblePosition);

    if (lastTranslatedView != null
        && firstVisibleItemComponentTree != null
        && lastTranslatedView != firstVisibleItemComponentTree.getLithoView()) {
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

    if (firstVisiblePosition == stickyHeaderPosition) {

      final LithoView firstVisibleView = firstVisibleItemComponentTree.getLithoView();

      // Translate first child, no need for sticky header.
      //
      // NOTE: Translate only if the next item is not also sticky header. If two sticky items are
      // stacked we don't want to translate the first one, as it would hide the second one under
      // the first one which is undesirable.
      if (!mHasStickyHeader.isValidPosition(stickyHeaderPosition + 1) ||
          !mHasStickyHeader.isSticky(stickyHeaderPosition + 1)) {
        firstVisibleView.setTranslationY(-firstVisibleView.getTop());
      }

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
      final int lastVisiblePosition = mHasStickyHeader.findLastVisibleItemPosition();
      int translationY = 0;
      for (int i = firstVisiblePosition; i <= lastVisiblePosition; i++) {
        if (mHasStickyHeader.isSticky(i)) {
          final View nextStickyHeader = mLayoutManager.findViewByPosition(i);
          final int offsetBetweenStickyHeaders = nextStickyHeader.getTop()
              - mRecyclerViewWrapper.getStickyHeader().getBottom()
              + mRecyclerViewWrapper.getPaddingTop();
          translationY = Math.min(offsetBetweenStickyHeaders, 0);
          break;
        }
      }
      mRecyclerViewWrapper.setStickyHeaderVerticalOffset(translationY);
      previousStickyHeaderPosition = stickyHeaderPosition;
    }
  }

  private void initStickyHeader(int stickyHeaderPosition) {
    final ComponentTree componentTree = mHasStickyHeader.getComponentAt(stickyHeaderPosition);
    // RecyclerView might not have yet detached the view that this componentTree bound to,
    // so detach it if that is the case.
    detachLithoViewIfNeeded(componentTree.getLithoView());
    mRecyclerViewWrapper.setStickyComponent(componentTree);
  }

  private static void detachLithoViewIfNeeded(LithoView view) {
    if (view == null) {
      return;
    }
    // This is equivalent of calling view.isAttachedToWindow(),
    // however, that method is available only from API19
    final boolean isAttachedToWindow = view.getWindowToken() != null;
    if (isAttachedToWindow) {
        view.onStartTemporaryDetach();
    }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  int findStickyHeaderPosition(int currentFirstVisiblePosition) {
    for (int i = currentFirstVisiblePosition; i >= 0; i--) {
      if (mHasStickyHeader.isSticky(i)) {
        return i;
      }
    }
    return RecyclerView.NO_POSITION;
  }
}
