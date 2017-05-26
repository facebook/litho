/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.support.v7.widget.RecyclerView;

import com.facebook.litho.ComponentTree;
import com.facebook.litho.LithoView;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link StickyHeaderController}
 */
@RunWith(ComponentsTestRunner.class)
public class StickyHeaderControllerTest {

  private HasStickyHeader mHasStickyHeader;
  private StickyHeaderController mStickyHeaderController;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() {
    mHasStickyHeader = mock(HasStickyHeader.class);
    mStickyHeaderController = new StickyHeaderController(mHasStickyHeader);
  }

  @Test
  public void testInitNoLayoutManager() {
    RecyclerViewWrapper wrapper = mock(RecyclerViewWrapper.class);
    RecyclerView recyclerView = mock(RecyclerView.class);
    when(wrapper.getRecyclerView()).thenReturn(recyclerView);

    thrown.expect(RuntimeException.class);
    thrown.expectMessage(StickyHeaderController.LAYOUTMANAGER_NOT_INITIALIZED);
    mStickyHeaderController.init(wrapper);
  }

  @Test
  public void testInitTwiceWithoutReset() {
    RecyclerViewWrapper wrapper1 = mock(RecyclerViewWrapper.class);
    RecyclerView recyclerView1 = mock(RecyclerView.class);
    when(wrapper1.getRecyclerView()).thenReturn(recyclerView1);
    when(recyclerView1.getLayoutManager()).thenReturn(mock(RecyclerView.LayoutManager.class));
    mStickyHeaderController.init(wrapper1);

    RecyclerViewWrapper wrapper2 = mock(RecyclerViewWrapper.class);
    RecyclerView recyclerView2 = mock(RecyclerView.class);
    when(recyclerView2.getLayoutManager()).thenReturn(mock(RecyclerView.LayoutManager.class));
    when(wrapper2.getRecyclerView()).thenReturn(recyclerView2);

    thrown.expect(RuntimeException.class);
    thrown.expectMessage(StickyHeaderController.WRAPPER_ALREADY_INITIALIZED);
    mStickyHeaderController.init(wrapper2);
  }

  @Test
  public void testResetBeforeInit() {
    thrown.expect(RuntimeException.class);
    thrown.expectMessage(StickyHeaderController.WRAPPER_NOT_INITIALIZED);

    mStickyHeaderController.reset();
  }

  @Test
  public void testTranslateRecyclerViewChild() {
    RecyclerViewWrapper wrapper = mock(RecyclerViewWrapper.class);
    RecyclerView recyclerView = mock(RecyclerView.class);
    when(wrapper.getRecyclerView()).thenReturn(recyclerView);
    when(recyclerView.getLayoutManager()).thenReturn(mock(RecyclerView.LayoutManager.class));
    mStickyHeaderController.init(wrapper);

    when(mHasStickyHeader.findFirstVisibleItemPosition()).thenReturn(2);
    when(mHasStickyHeader.isSticky(2)).thenReturn(true);

    ComponentTree componentTree = mock(ComponentTree.class);
    when(mHasStickyHeader.getComponentAt(2)).thenReturn(componentTree);
    LithoView lithoView = mock(LithoView.class);
    when(componentTree.getLithoView()).thenReturn(lithoView);

    mStickyHeaderController.onScrolled(null, 0, 0);

    verify(lithoView).setTranslationY(any(Integer.class));
    verify(wrapper, times(2)).hideStickyHeader();
  }

  @Test
  public void testTranslateWrapperChild() {
    RecyclerViewWrapper wrapper = mock(RecyclerViewWrapper.class);
    RecyclerView recyclerView = mock(RecyclerView.class);
    when(wrapper.getRecyclerView()).thenReturn(recyclerView);
    when(recyclerView.getLayoutManager()).thenReturn(mock(RecyclerView.LayoutManager.class));
    mStickyHeaderController.init(wrapper);

    when(mHasStickyHeader.findFirstVisibleItemPosition()).thenReturn(6);
    when(mHasStickyHeader.isSticky(2)).thenReturn(true);

    ComponentTree componentTree = mock(ComponentTree.class);
    when(mHasStickyHeader.getComponentAt(6)).thenReturn(componentTree);

    mStickyHeaderController.onScrolled(null, 0, 0);

    verify(wrapper).setStickyHeaderVerticalOffset(any(Integer.class));
  }

  @Test
  public void testTranslateStackedStickyHeaders() {
    RecyclerViewWrapper wrapper = mock(RecyclerViewWrapper.class);
    RecyclerView recyclerView = mock(RecyclerView.class);
    when(wrapper.getRecyclerView()).thenReturn(recyclerView);
    when(recyclerView.getLayoutManager()).thenReturn(mock(RecyclerView.LayoutManager.class));
    mStickyHeaderController.init(wrapper);

    when(mHasStickyHeader.findFirstVisibleItemPosition()).thenReturn(2);
    when(mHasStickyHeader.isSticky(2)).thenReturn(true);
    when(mHasStickyHeader.isSticky(3)).thenReturn(true);
    when(mHasStickyHeader.isValidPosition(3)).thenReturn(true);

    ComponentTree componentTree = mock(ComponentTree.class);
    when(mHasStickyHeader.getComponentAt(2)).thenReturn(componentTree);
    LithoView lithoView = mock(LithoView.class);
    when(componentTree.getLithoView()).thenReturn(lithoView);

    mStickyHeaderController.onScrolled(null, 0, 0);

    verify(lithoView, never()).setTranslationY(any(Integer.class));
    verify(wrapper, times(1)).hideStickyHeader();
  }
}
