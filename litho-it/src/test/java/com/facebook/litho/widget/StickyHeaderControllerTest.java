/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.support.v7.widget.RecyclerView;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.LithoView;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

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
    SectionsRecyclerView recycler = mock(SectionsRecyclerView.class);
    RecyclerView recyclerView = mock(RecyclerView.class);
    when(recycler.getRecyclerView()).thenReturn(recyclerView);

    thrown.expect(RuntimeException.class);
    thrown.expectMessage(StickyHeaderController.LAYOUTMANAGER_NOT_INITIALIZED);
    mStickyHeaderController.init(recycler);
  }

  @Test
  public void testInitTwiceWithoutReset() {
    SectionsRecyclerView recycler1 = mock(SectionsRecyclerView.class);
    RecyclerView recyclerView1 = mock(RecyclerView.class);
    when(recycler1.getRecyclerView()).thenReturn(recyclerView1);
    when(recyclerView1.getLayoutManager()).thenReturn(mock(RecyclerView.LayoutManager.class));
    mStickyHeaderController.init(recycler1);

    SectionsRecyclerView recycler2 = mock(SectionsRecyclerView.class);
    RecyclerView recyclerView2 = mock(RecyclerView.class);
    when(recyclerView2.getLayoutManager()).thenReturn(mock(RecyclerView.LayoutManager.class));
    when(recycler2.getRecyclerView()).thenReturn(recyclerView2);

    thrown.expect(RuntimeException.class);
    thrown.expectMessage(StickyHeaderController.RECYCLER_ALREADY_INITIALIZED);
    mStickyHeaderController.init(recycler2);
  }

  @Test
  public void testResetBeforeInit() {
    thrown.expect(RuntimeException.class);
    thrown.expectMessage(StickyHeaderController.RECYCLER_NOT_INITIALIZED);

    mStickyHeaderController.reset();
  }

  @Test
  public void testTranslateRecyclerViewChild() {
    SectionsRecyclerView recycler = mock(SectionsRecyclerView.class);
    RecyclerView recyclerView = mock(RecyclerView.class);
    when(recycler.getRecyclerView()).thenReturn(recyclerView);
    when(recyclerView.getLayoutManager()).thenReturn(mock(RecyclerView.LayoutManager.class));
    mStickyHeaderController.init(recycler);

    when(mHasStickyHeader.findFirstVisibleItemPosition()).thenReturn(2);
    when(mHasStickyHeader.isSticky(2)).thenReturn(true);

    ComponentTree componentTree = mock(ComponentTree.class);
    when(mHasStickyHeader.getComponentForStickyHeaderAt(2)).thenReturn(componentTree);
    LithoView lithoView = mock(LithoView.class);
    when(componentTree.getLithoView()).thenReturn(lithoView);

    mStickyHeaderController.onScrolled(null, 0, 0);

    verify(lithoView).setTranslationY(anyFloat());
    verify(recycler, times(2)).hideStickyHeader();
  }

  @Test
  public void testTranslaterecyclerChild() {
    SectionsRecyclerView recycler = mock(SectionsRecyclerView.class);
    RecyclerView recyclerView = mock(RecyclerView.class);
    when(recycler.getRecyclerView()).thenReturn(recyclerView);
    when(recyclerView.getLayoutManager()).thenReturn(mock(RecyclerView.LayoutManager.class));
    mStickyHeaderController.init(recycler);

    when(mHasStickyHeader.findFirstVisibleItemPosition()).thenReturn(6);
    when(mHasStickyHeader.isSticky(2)).thenReturn(true);

    when(mHasStickyHeader.getComponentForStickyHeaderAt(2)).thenReturn(mock(ComponentTree.class));
    when(mHasStickyHeader.getComponentForStickyHeaderAt(6)).thenReturn(mock(ComponentTree.class));

    mStickyHeaderController.onScrolled(null, 0, 0);

    verify(recycler).setStickyHeaderVerticalOffset(any(Integer.class));
  }

  @Test
  public void testTranslateStackedStickyHeaders() {
    SectionsRecyclerView recycler = mock(SectionsRecyclerView.class);
    RecyclerView recyclerView = mock(RecyclerView.class);
    when(recycler.getRecyclerView()).thenReturn(recyclerView);
    when(recyclerView.getLayoutManager()).thenReturn(mock(RecyclerView.LayoutManager.class));
    mStickyHeaderController.init(recycler);

    when(mHasStickyHeader.findFirstVisibleItemPosition()).thenReturn(2);
    when(mHasStickyHeader.isSticky(2)).thenReturn(true);
    when(mHasStickyHeader.isSticky(3)).thenReturn(true);
    when(mHasStickyHeader.isValidPosition(3)).thenReturn(true);

    ComponentTree componentTree = mock(ComponentTree.class);
    when(mHasStickyHeader.getComponentForStickyHeaderAt(2)).thenReturn(componentTree);
    LithoView lithoView = mock(LithoView.class);
    when(componentTree.getLithoView()).thenReturn(lithoView);

    mStickyHeaderController.onScrolled(null, 0, 0);

    verify(lithoView, never()).setTranslationY(any(Integer.class));
    verify(recycler, times(2)).hideStickyHeader();
  }
}
