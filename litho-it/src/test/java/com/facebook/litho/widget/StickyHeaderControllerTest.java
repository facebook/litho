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

import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.recyclerview.widget.RecyclerView;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.LithoView;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

/** Tests for {@link StickyHeaderController} */
@RunWith(LithoTestRunner.class)
public class StickyHeaderControllerTest {

  private HasStickyHeader mHasStickyHeader;
  private StickyHeaderController mStickyHeaderController;

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() {
    mHasStickyHeader = mock(HasStickyHeader.class);
    mStickyHeaderController = new StickyHeaderControllerImpl(mHasStickyHeader);
  }

  @Test
  public void testInitNoLayoutManager() {
    SectionsRecyclerView recycler = mock(SectionsRecyclerView.class);
    RecyclerView recyclerView = mock(RecyclerView.class);
    when(recycler.getRecyclerView()).thenReturn(recyclerView);

    thrown.expect(RuntimeException.class);
    thrown.expectMessage(StickyHeaderControllerImpl.LAYOUTMANAGER_NOT_INITIALIZED);
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
    thrown.expectMessage(StickyHeaderControllerImpl.RECYCLER_ALREADY_INITIALIZED);
    mStickyHeaderController.init(recycler2);
  }

  @Test
  public void testResetBeforeInit() {
    thrown.expect(RuntimeException.class);
    thrown.expectMessage(StickyHeaderControllerImpl.RECYCLER_NOT_INITIALIZED);

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

    verify(recycler).setStickyHeaderVerticalOffset(nullable(Integer.class));
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

    verify(lithoView, never()).setTranslationY(nullable(Integer.class));
    verify(recycler, times(2)).hideStickyHeader();
  }
}
