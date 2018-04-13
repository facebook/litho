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

import static com.facebook.litho.widget.ViewportInfo.State.DATA_CHANGES;
import static com.facebook.litho.widget.ViewportInfo.State.SCROLLING;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.os.Handler;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link ViewportManager}
 */
@RunWith(ComponentsTestRunner.class)
public class ViewportManagerTest {

  private LayoutInfo mLayoutInfo;
  private ViewportInfo.ViewportChanged mViewportChangedListener;
  private Handler mMainThreadHandler;

  @Before
  public void setup() {
    mLayoutInfo = mock(LayoutInfo.class);
    mViewportChangedListener = mock(ViewportInfo.ViewportChanged.class);
    mMainThreadHandler = mock(Handler.class);
  }

  @Test
  public void testOnViewportChangedWhileScrolling() {
    ViewportManager viewportManager = getViewportManager(0, 0);

    setVisibleItemPositionInMockedLayoutManager(0, 5);
    setFullyVisibleItemPositionInMockedLayoutManager(1, 4);
    setTotalItemInMockedLayoutManager(20);

    viewportManager.setDataChangedIsVisible(false);
    viewportManager.onViewportChanged(SCROLLING);

    verify(mViewportChangedListener).viewportChanged(0, 5, 1, 4, SCROLLING);
  }

  @Test
  public void testOnViewportChangedWhileScrollingWithNoItemsFullyVisible() {
    ViewportManager viewportManager = getViewportManager(0, 0);

    // The second and third items are visible partially but neither is fully visible
    setVisibleItemPositionInMockedLayoutManager(1, 2);
    setFullyVisibleItemPositionInMockedLayoutManager(-1, -1);
    setTotalItemInMockedLayoutManager(20);

    viewportManager.setDataChangedIsVisible(true);
    viewportManager.onViewportChanged(SCROLLING);

    verify(mViewportChangedListener).viewportChanged(1, 2, -1, -1, SCROLLING);
  }

  @Test
  public void testOnViewportChangedWithoutScrolling() {
    ViewportManager viewportManager = getViewportManager(5, 20);

    setVisibleItemPositionInMockedLayoutManager(5, 20);
    setFullyVisibleItemPositionInMockedLayoutManager(7, 18);
    setTotalItemInMockedLayoutManager(20);

    viewportManager.setDataChangedIsVisible(true);
    viewportManager.onViewportChanged(DATA_CHANGES);

    verify(mViewportChangedListener).viewportChanged(5, 20, 7, 18, DATA_CHANGES);
  }

  @Test
  public void testNoViewportChangedWithScrolling() {
    setFullyVisibleItemPositionInMockedLayoutManager(7, 9);
    setTotalItemInMockedLayoutManager(13);

    ViewportManager viewportManager = getViewportManager(5, 10);

    setVisibleItemPositionInMockedLayoutManager(5, 10);
    setFullyVisibleItemPositionInMockedLayoutManager(7, 9);
    setTotalItemInMockedLayoutManager(13);

    viewportManager.setDataChangedIsVisible(false);
    viewportManager.onViewportChanged(SCROLLING);

    verifyZeroInteractions(mViewportChangedListener);
  }

  @Test
  public void testTotalItemChangedWhileVisiblePositionsRemainTheSame() {
    setTotalItemInMockedLayoutManager(13);
    ViewportManager viewportManager = getViewportManager(5, 10);

    setVisibleItemPositionInMockedLayoutManager(5, 10);
    setFullyVisibleItemPositionInMockedLayoutManager(7, 9);
    setTotalItemInMockedLayoutManager(12);

    viewportManager.setDataChangedIsVisible(true);
    viewportManager.onViewportChanged(DATA_CHANGES);

    verify(mViewportChangedListener).viewportChanged(5, 10, 7, 9, DATA_CHANGES);
  }

  @Test
  public void testTotalItemChangedWhileNoItemsFullyVisible() {
    setTotalItemInMockedLayoutManager(13);
    ViewportManager viewportManager = getViewportManager(5, 6);

    // The seventh and eighth items are visible partially but neither is fully visible
    setVisibleItemPositionInMockedLayoutManager(6, 7);
    setFullyVisibleItemPositionInMockedLayoutManager(-1, -1);
    setTotalItemInMockedLayoutManager(12);

    viewportManager.setDataChangedIsVisible(false);
    viewportManager.onViewportChanged(SCROLLING);

    verify(mViewportChangedListener).viewportChanged(6, 7, -1, -1, SCROLLING);
  }

  @Test
  public void testInsertInVisibleRange() {
    ViewportManager viewportManager = getViewportManager(1, 6);
    boolean isInRange = viewportManager.isInsertInVisibleRange(7, 2, 7);
    assertThat(isInRange).isTrue();
  }

  @Test
  public void testInsertNotInVisibleRange() {
    ViewportManager viewportManager = getViewportManager(1, 6);
    boolean isInRange = viewportManager.isInsertInVisibleRange(7, 2, 6);
    assertThat(isInRange).isFalse();
  }

  @Test
  public void testUpdateInVisibleRange() {
    ViewportManager viewportManager = getViewportManager(1, 6);
    boolean isInRange = viewportManager.isUpdateInVisibleRange(5, 2);
    assertThat(isInRange).isTrue();
  }

  @Test
  public void testUpdateNotInVisibleRange() {
    ViewportManager viewportManager = getViewportManager(1, 6);
    boolean isInRange = viewportManager.isUpdateInVisibleRange(7, 2);
    assertThat(isInRange).isFalse();
  }

  @Test
  public void testMoveInVisibleRange() {
    ViewportManager viewportManager = getViewportManager(1, 6);
    boolean isInRange = viewportManager.isMoveInVisibleRange(8, 5, 6);
    assertThat(isInRange).isTrue();

    viewportManager = getViewportManager(1, 6);
    isInRange = viewportManager.isMoveInVisibleRange(5, 8, 6);
    assertThat(isInRange).isTrue();
  }

  @Test
  public void testMoveNotInVisibleRange() {
    ViewportManager viewportManager = getViewportManager(1, 6);
    boolean isInRange = viewportManager.isMoveInVisibleRange(7, 9, 6);
    assertThat(isInRange).isFalse();
  }

  @Test
  public void testRemoveInVisibleRange() {
    ViewportManager viewportManager = getViewportManager(1, 6);
    boolean isInRange = viewportManager.isRemoveInVisibleRange(6, 2);
    assertThat(isInRange).isTrue();

    getViewportManager(3, 6);
    isInRange = viewportManager.isRemoveInVisibleRange(2, 2);
    assertThat(isInRange).isTrue();
  }

  @Test
  public void testRemoveNotInVisibleRange() {
    ViewportManager viewportManager = getViewportManager(3, 6);
    boolean isInRange = viewportManager.isRemoveInVisibleRange(2, 1);
    assertThat(isInRange).isFalse();
  }

  @Test
  public void testChangeSetIsVisibleForInitialisation() {
    ViewportManager viewportManager = getViewportManager(-1, 2);

    assertThat(viewportManager.isInsertInVisibleRange(6, 2, -1)).isTrue();
    assertThat(viewportManager.isUpdateInVisibleRange(6, 2)).isTrue();
    assertThat(viewportManager.isMoveInVisibleRange(6, 2, 10)).isTrue();
    assertThat(viewportManager.isRemoveInVisibleRange(6, 2)).isTrue();

    viewportManager = getViewportManager(1, -1);

    assertThat(viewportManager.isInsertInVisibleRange(6, 2, 10)).isTrue();
    assertThat(viewportManager.isUpdateInVisibleRange(6, 2)).isTrue();
    assertThat(viewportManager.isMoveInVisibleRange(6, 2, -1)).isTrue();
    assertThat(viewportManager.isRemoveInVisibleRange(6, 2)).isTrue();
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
      int firstFullyVisiblePosition, int lastFullyVisiblePosition) {
    ViewportManager viewportManager =
        new ViewportManager(
            firstFullyVisiblePosition, lastFullyVisiblePosition, mLayoutInfo, mMainThreadHandler);
    viewportManager.addViewportChangedListener(mViewportChangedListener);

    return viewportManager;
  }
}
