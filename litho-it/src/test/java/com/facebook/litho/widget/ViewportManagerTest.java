/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.os.Handler;
import android.support.v7.widget.RecyclerView;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.ViewportManager.ViewportChangedWhileScrolling;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ViewportManager}
 */
@RunWith(ComponentsTestRunner.class)
public class ViewportManagerTest {

  private LayoutInfo mLayoutInfo;
  private ViewportChangedWhileScrolling mViewportChangedWhileScrolling;
  private ViewportInfo.ViewportChanged mViewportChangedListener;
  private Handler mMainThreadHandler;

  @Before
  public void setup() {
    mLayoutInfo = mock(LayoutInfo.class);
    mViewportChangedWhileScrolling = mock(ViewportChangedWhileScrolling.class);
    mViewportChangedListener = mock(ViewportInfo.ViewportChanged.class);
    mMainThreadHandler = mock(Handler.class);
  }

  @Test
  public void testOnViewportChangedWhileScrolling() {
    ViewportManager viewportManager = getViewportManager(RecyclerView.SCROLL_STATE_DRAGGING, 0, 0);

    setVisibleItemPositionInMockedLayoutManager(0, 5);
    setFullyVisibleItemPositionInMockedLayoutManager(1, 4);
    setTotalItemInMockedLayoutManager(20);

    viewportManager.onViewportChanged(mViewportChangedWhileScrolling);

    verify(mViewportChangedWhileScrolling).viewportChanged(0, 5);
    verify(mViewportChangedListener).viewportChanged(0, 5, 1, 4);
  }

  @Test
  public void testOnViewportChangedWithoutScrolling() {
    ViewportManager viewportManager = getViewportManager(RecyclerView.SCROLL_STATE_IDLE, 5, 20);

    setVisibleItemPositionInMockedLayoutManager(5, 20);
    setFullyVisibleItemPositionInMockedLayoutManager(7, 18);
    setTotalItemInMockedLayoutManager(20);

    viewportManager.onViewportChanged(null);

    verify(mViewportChangedListener).viewportChanged(5, 20, 7, 18);
  }

  @Test
  public void testNoViewportChangedWithScrolling() {
    setTotalItemInMockedLayoutManager(13);
    ViewportManager viewportManager = getViewportManager(RecyclerView.SCROLL_STATE_DRAGGING, 5, 10);

    setVisibleItemPositionInMockedLayoutManager(5, 10);
    setFullyVisibleItemPositionInMockedLayoutManager(7, 9);
    setTotalItemInMockedLayoutManager(13);

    viewportManager.onViewportChanged(mViewportChangedWhileScrolling);

    verifyZeroInteractions(mViewportChangedWhileScrolling);
    verifyZeroInteractions(mViewportChangedListener);
  }

  @Test
  public void testNoViewportChangedWithoutScrolling() {
    ViewportManager viewportManager = getViewportManager(RecyclerView.SCROLL_STATE_IDLE, 5, 10);

    setVisibleItemPositionInMockedLayoutManager(5, 10);
    setFullyVisibleItemPositionInMockedLayoutManager(7, 9);
    setTotalItemInMockedLayoutManager(12);

    viewportManager.onViewportChanged(mViewportChangedWhileScrolling);

    verifyZeroInteractions(mViewportChangedWhileScrolling);
    verify(mViewportChangedListener).viewportChanged(5, 10, 7, 9);
  }

  @Test
  public void testOnViewportChangedFromRemovalWithScrolling() {
    ViewportManager viewportManager = getViewportManager(RecyclerView.SCROLL_STATE_DRAGGING, -1, 0);
    viewportManager.onViewportChangedAfterViewRemoval(1);

    verifyZeroInteractions(mMainThreadHandler);
  }

  @Test
  public void testOnViewportChangedFromRemovalWithoutScrolling() {
    ViewportManager viewportManager = getViewportManager(RecyclerView.SCROLL_STATE_IDLE, 0, 1);
    viewportManager.onViewportChangedAfterViewRemoval(0);

    verify(mMainThreadHandler).post(any(Runnable.class));
  }

  @Test
  public void testTotalItemChangedWhileVisiblePositionsRemainTheSame() {
    setTotalItemInMockedLayoutManager(13);
    ViewportManager viewportManager = getViewportManager(RecyclerView.SCROLL_STATE_IDLE, 5, 10);

    setVisibleItemPositionInMockedLayoutManager(5, 10);
    setFullyVisibleItemPositionInMockedLayoutManager(7, 9);
    setTotalItemInMockedLayoutManager(12);

    viewportManager.onViewportChanged(mViewportChangedWhileScrolling);

    verifyZeroInteractions(mViewportChangedWhileScrolling);
    verify(mViewportChangedListener).viewportChanged(5, 10, 7, 9);
  }

  private void setVisibleItemPositionInMockedLayoutManager(
      int firstVisibleItemPosition,
      int lastVisibleItemPosition) {
    when(mLayoutInfo.findFirstVisibleItemPosition()).thenReturn(firstVisibleItemPosition);
    when(mLayoutInfo.findLastVisibleItemPosition()).thenReturn(lastVisibleItemPosition);
  }

  private void setFullyVisibleItemPositionInMockedLayoutManager(
      int firstFullyVisiblePosition,
      int lastFullyVisiblePosition) {
    when(mLayoutInfo.findFirstFullyVisibleItemPosition())
        .thenReturn(firstFullyVisiblePosition);
    when(mLayoutInfo.findLastFullyVisibleItemPosition())
        .thenReturn(lastFullyVisiblePosition);
  }

  private void setTotalItemInMockedLayoutManager(int totalItem) {
    when(mLayoutInfo.getItemCount()).thenReturn(totalItem);
  }

  private ViewportManager getViewportManager(
      int scrollState,
      int firstFullyVisiblePosition,
      int lastFullyVisiblePosition) {
    ViewportManager viewportManager = new ViewportManager(
        firstFullyVisiblePosition,
        lastFullyVisiblePosition,
        mLayoutInfo,
        mMainThreadHandler,
        scrollState,
        mViewportChangedWhileScrolling);
    viewportManager.setViewportChangedListener(mViewportChangedListener);

    return viewportManager;
  }
}
