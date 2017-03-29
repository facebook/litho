// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;

import com.facebook.components.Component;
import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentInfo;
import com.facebook.components.ComponentTree;
import com.facebook.components.LayoutHandler;
import com.facebook.components.Size;
import com.facebook.components.SizeSpec;
import com.facebook.components.testing.testrunner.ComponentsTestRunner;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RecyclerBinder}
 */
@RunWith(ComponentsTestRunner.class)
@PrepareForTest(ComponentTreeHolder.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
public class RecyclerBinderTest {

  @Rule
  public PowerMockRule mPowerMockRule = new PowerMockRule();

  private static final float RANGE_RATIO = 2.0f;
  private static final int RANGE_SIZE = 3;
  private final Map<Component, TestComponentTreeHolder> mHoldersForComponents = new HashMap<>();
  private RecyclerBinder mRecyclerBinder;
  private LayoutInfo mLayoutInfo;
  private ComponentContext mComponentContext;

  private final Answer<ComponentTreeHolder> mComponentTreeHolderAnswer =
      new Answer<ComponentTreeHolder>() {
        @Override
        public ComponentTreeHolder answer(InvocationOnMock invocation) throws Throwable {
          final ComponentInfo componentInfo = (ComponentInfo) invocation.getArguments()[0];
          final TestComponentTreeHolder holder = new TestComponentTreeHolder(componentInfo);
          mHoldersForComponents.put(componentInfo.getComponent(), holder);

          return holder;
        }
      };

  @Before
  public void setup() {
    mComponentContext = new ComponentContext(RuntimeEnvironment.application);
    PowerMockito.mockStatic(ComponentTreeHolder.class);
    PowerMockito.when(ComponentTreeHolder.acquire(
        any(ComponentInfo.class),
        any(LayoutHandler.class)))
        .thenAnswer(mComponentTreeHolderAnswer);
    mLayoutInfo = mock(LayoutInfo.class);
    setupBaseLayoutInfoMock();

    mRecyclerBinder = new RecyclerBinder(mComponentContext, RANGE_RATIO, mLayoutInfo);
  }

  private void setupBaseLayoutInfoMock() {
    Mockito.when(mLayoutInfo.getScrollDirection()).thenReturn(OrientationHelper.VERTICAL);

    Mockito.when(mLayoutInfo.getLayoutManager())
        .thenReturn(new LinearLayoutManager(mComponentContext));

    Mockito.when(mLayoutInfo.approximateRangeSize(anyInt(), anyInt(), anyInt(), anyInt()))
        .thenReturn(RANGE_SIZE);

    Mockito.when(mLayoutInfo.getChildHeightSpec(anyInt()))
        .thenReturn(SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));
    Mockito.when(mLayoutInfo.getChildWidthSpec(anyInt()))
        .thenReturn(SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));
  }

  @Test
  public void testComponentTreeHolderCreation() {
    final List<ComponentInfo> components = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      components.add(ComponentInfo.create().component(mock(Component.class)).build());
      mRecyclerBinder.insertItemAt(0, components.get(i));
    }

    for (int i = 0; i < 100; i++) {
      Assert.assertNotNull(mHoldersForComponents.get(components.get(i).getComponent()));
    }
  }

  @Test
  public void testOnMeasureAfterAddingItems() {
    final List<ComponentInfo> components = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      components.add(ComponentInfo.create().component(mock(Component.class)).build());
      mRecyclerBinder.insertItemAt(i, components.get(i));
    }

    for (int i = 0; i < 100; i++) {
      Assert.assertNotNull(mHoldersForComponents.get(components.get(i).getComponent()));
    }

    final Size size = new Size();
    final int widthSpec = SizeSpec.makeSizeSpec(200, SizeSpec.AT_MOST);
    final int heightSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);

    mRecyclerBinder.measure(size, widthSpec, heightSpec);

    TestComponentTreeHolder componentTreeHolder =
        mHoldersForComponents.get(components.get(0).getComponent());

    assertTrue(componentTreeHolder.isTreeValid());
    assertTrue(componentTreeHolder.mLayoutSyncCalled);

    int rangeTotal = RANGE_SIZE + (int) (RANGE_SIZE * RANGE_RATIO);

    for (int i = 1; i <= rangeTotal; i++) {
      componentTreeHolder = mHoldersForComponents.get(components.get(i).getComponent());

      assertTrue(componentTreeHolder.isTreeValid());
      assertTrue(componentTreeHolder.mLayoutAsyncCalled);
      assertFalse(componentTreeHolder.mLayoutSyncCalled);
    }

    for (int k = rangeTotal + 1; k < components.size(); k++) {
      componentTreeHolder = mHoldersForComponents.get(components.get(k).getComponent());

      assertFalse(componentTreeHolder.isTreeValid());
      assertFalse(componentTreeHolder.mLayoutAsyncCalled);
      assertFalse(componentTreeHolder.mLayoutSyncCalled);
    }

    Assert.assertEquals(100, size.width);
  }

  @Test
  public void onBoundsDefined() {
    final List<ComponentInfo> components = prepareLoadedBinder();
    for (int i = 0; i < components.size(); i++) {
      final TestComponentTreeHolder holder =
          mHoldersForComponents.get(components.get(i).getComponent());
      holder.mLayoutAsyncCalled = false;
      holder.mLayoutSyncCalled = false;
    }

    mRecyclerBinder.setSize(200, 200);

    for (int i = 0; i < components.size(); i++) {
      final TestComponentTreeHolder holder =
          mHoldersForComponents.get(components.get(i).getComponent());
      assertFalse(holder.mLayoutAsyncCalled);
      assertFalse(holder.mLayoutSyncCalled);
    }
  }

  @Test
  public void onBoundsDefinedWithDifferentSize() {
    final List<ComponentInfo> components = prepareLoadedBinder();
    for (int i = 0; i < components.size(); i++) {
      final TestComponentTreeHolder holder =
          mHoldersForComponents.get(components.get(i).getComponent());
      holder.mLayoutAsyncCalled = false;
      holder.mLayoutSyncCalled = false;
    }

    mRecyclerBinder.setSize(300, 200);

    final int rangeTotal = (int) (RANGE_SIZE + (RANGE_RATIO * RANGE_SIZE));

    TestComponentTreeHolder holder =  mHoldersForComponents.get(components.get(0).getComponent());
    assertTrue(holder.isTreeValid());
    assertTrue(holder.mLayoutSyncCalled);

    for (int i = 1; i <= rangeTotal; i++) {
      holder = mHoldersForComponents.get(components.get(i).getComponent());
      assertTrue(holder.isTreeValid());
      assertTrue(holder.mLayoutAsyncCalled);
    }

    for (int i = rangeTotal + 1; i < components.size(); i++) {
      holder = mHoldersForComponents.get(components.get(i).getComponent());
      assertFalse(holder.isTreeValid());
      assertFalse(holder.mLayoutAsyncCalled);
      assertFalse(holder.mLayoutSyncCalled);
    }
  }

  @Test
  public void testMount() {
    RecyclerView recyclerView = mock(RecyclerView.class);
    mRecyclerBinder.mount(recyclerView);

    verify(recyclerView).setLayoutManager(mLayoutInfo.getLayoutManager());
    verify(recyclerView).setAdapter(any(RecyclerView.Adapter.class));
    verify(recyclerView).addOnScrollListener(any(OnScrollListener.class));
  }

  @Test
  public void testMountGrid() {
    when(mLayoutInfo.getSpanCount()).thenReturn(2);
    GridLayoutManager gridLayoutManager = mock(GridLayoutManager.class);
    when(mLayoutInfo.getLayoutManager()).thenReturn(gridLayoutManager);

    RecyclerView recyclerView = mock(RecyclerView.class);
    mRecyclerBinder.mount(recyclerView);

    verify(recyclerView).setLayoutManager(mLayoutInfo.getLayoutManager());
    verify(gridLayoutManager).setSpanSizeLookup(any(GridLayoutManager.SpanSizeLookup.class));
    verify(recyclerView).setAdapter(any(RecyclerView.Adapter.class));
    verify(recyclerView).addOnScrollListener(any(OnScrollListener.class));
  }

  @Test
  public void testMountWithStaleView() {
    RecyclerView recyclerView = mock(RecyclerView.class);
    mRecyclerBinder.mount(recyclerView);

    verify(recyclerView).setLayoutManager(mLayoutInfo.getLayoutManager());
    verify(recyclerView).setAdapter(any(RecyclerView.Adapter.class));
    verify(recyclerView).addOnScrollListener(any(OnScrollListener.class));

    RecyclerView secondRecyclerView = mock(RecyclerView.class);
    mRecyclerBinder.mount(secondRecyclerView);

    verify(recyclerView).setLayoutManager(null);
    verify(recyclerView).setAdapter(null);
    verify(recyclerView).removeOnScrollListener(any(OnScrollListener.class));

    verify(secondRecyclerView).setLayoutManager(mLayoutInfo.getLayoutManager());
    verify(secondRecyclerView).setAdapter(any(RecyclerView.Adapter.class));
    verify(secondRecyclerView).addOnScrollListener(any(OnScrollListener.class));
  }

  @Test
  public void testUnmount() {
    RecyclerView recyclerView = mock(RecyclerView.class);
    mRecyclerBinder.mount(recyclerView);

    verify(recyclerView).setLayoutManager(mLayoutInfo.getLayoutManager());
    verify(recyclerView).setAdapter(any(RecyclerView.Adapter.class));
    verify(recyclerView).addOnScrollListener(any(OnScrollListener.class));

    mRecyclerBinder.unmount(recyclerView);

    verify(recyclerView).setLayoutManager(null);
    verify(recyclerView).setAdapter(null);
    verify(recyclerView).removeOnScrollListener(any(OnScrollListener.class));
  }

  @Test
  public void testUnmountGrid() {
    when(mLayoutInfo.getSpanCount()).thenReturn(2);
    GridLayoutManager gridLayoutManager = mock(GridLayoutManager.class);
    when(mLayoutInfo.getLayoutManager()).thenReturn(gridLayoutManager);

    RecyclerView recyclerView = mock(RecyclerView.class);
    mRecyclerBinder.mount(recyclerView);

    verify(recyclerView).setLayoutManager(mLayoutInfo.getLayoutManager());
    verify(gridLayoutManager).setSpanSizeLookup(any(GridLayoutManager.SpanSizeLookup.class));
    verify(recyclerView).setAdapter(any(RecyclerView.Adapter.class));
    verify(recyclerView).addOnScrollListener(any(OnScrollListener.class));

    mRecyclerBinder.unmount(recyclerView);
    verify(recyclerView).setLayoutManager(null);
    verify(gridLayoutManager).setSpanSizeLookup(null);
    verify(recyclerView).setAdapter(null);
    verify(recyclerView).removeOnScrollListener(any(OnScrollListener.class));
  }

  @Test
  public void onRemeasureWithDifferentSize() {
    final List<ComponentInfo> components = prepareLoadedBinder();
    for (int i = 0; i < components.size(); i++) {
      final TestComponentTreeHolder holder =
          mHoldersForComponents.get(components.get(i).getComponent());
      holder.mLayoutAsyncCalled = false;
      holder.mLayoutSyncCalled = false;
    }

    int widthSpec = SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY);
    int heightSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);

    mRecyclerBinder.measure(new Size(), widthSpec, heightSpec);
    final int rangeTotal = (int) (RANGE_SIZE + (RANGE_RATIO * RANGE_SIZE));

    TestComponentTreeHolder holder =  mHoldersForComponents.get(components.get(0).getComponent());
    assertTrue(holder.isTreeValid());
    assertTrue(holder.mLayoutSyncCalled);

    for (int i = 1; i <= rangeTotal; i++) {
      holder = mHoldersForComponents.get(components.get(i).getComponent());
      assertTrue(holder.isTreeValid());
      assertTrue(holder.mLayoutAsyncCalled);
    }

    for (int i = rangeTotal + 1; i < components.size(); i++) {
      holder = mHoldersForComponents.get(components.get(i).getComponent());
      assertFalse(holder.isTreeValid());
      assertFalse(holder.mLayoutAsyncCalled);
      assertFalse(holder.mLayoutSyncCalled);
    }
  }

  @Test
  public void testComponentWithDifferentSpanSize() {
    Mockito.when(mLayoutInfo.getLayoutManager())
        .thenReturn(new GridLayoutManager(mComponentContext, 2));
    final List<ComponentInfo> components = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      components.add(ComponentInfo.create()
          .component(mock(Component.class))
          .spanSize((i == 0 || i % 3 == 0) ? 2 : 1)
          .build());
      mRecyclerBinder.insertItemAt(i, components.get(i));
    }

    for (int i = 0; i < 100; i++) {
      Assert.assertNotNull(mHoldersForComponents.get(components.get(i).getComponent()));
    }

    Size size = new Size();
    int widthSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);
    int heightSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);

    mRecyclerBinder.measure(size, widthSpec, heightSpec);

    TestComponentTreeHolder componentTreeHolder =
        mHoldersForComponents.get(components.get(0).getComponent());

    assertTrue(componentTreeHolder.isTreeValid());
    assertTrue(componentTreeHolder.mLayoutSyncCalled);
    Assert.assertEquals(200, size.width);

    final int rangeTotal = (int) (RANGE_SIZE + (RANGE_RATIO * RANGE_SIZE));

    for (int i = 1; i <= rangeTotal; i++) {
      componentTreeHolder = mHoldersForComponents.get(components.get(i).getComponent());

      assertTrue(componentTreeHolder.isTreeValid());
      assertTrue(componentTreeHolder.mLayoutAsyncCalled);

      final int expectedWidth = i % 3 == 0 ? 200 : 100;
      Assert.assertEquals(expectedWidth, componentTreeHolder.mChildWidth);
      Assert.assertEquals(100, componentTreeHolder.mChildHeight);
    }
  }

  @Test
  public void testAddItemsAfterMeasuring() {
    final Size size = new Size();
    final int widthSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);
    final int heightSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);

    mRecyclerBinder.measure(size, widthSpec, heightSpec);

    Assert.assertEquals(200, size.width);

    final List<ComponentInfo> components = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      components.add(ComponentInfo.create().component(mock(Component.class)).build());
      mRecyclerBinder.insertItemAt(i, components.get(i));
    }

    for (int i = 0; i < 100; i++) {
      Assert.assertNotNull(mHoldersForComponents.get(components.get(i).getComponent()));
    }

    TestComponentTreeHolder componentTreeHolder;

    int rangeTotal = RANGE_SIZE + (int) (RANGE_SIZE * RANGE_RATIO);

    for (int i = 0; i < RANGE_SIZE; i++) {
      componentTreeHolder = mHoldersForComponents.get(components.get(i).getComponent());

      assertTrue(componentTreeHolder.isTreeValid());
      assertTrue(componentTreeHolder.mLayoutSyncCalled);
    }

    for (int i = RANGE_SIZE ; i <= rangeTotal; i++) {
      componentTreeHolder = mHoldersForComponents.get(components.get(i).getComponent());

      assertTrue(componentTreeHolder.isTreeValid());
      assertTrue(componentTreeHolder.mLayoutAsyncCalled);
      assertFalse(componentTreeHolder.mLayoutSyncCalled);
    }

    for (int i = rangeTotal + 1; i < components.size(); i++) {
      componentTreeHolder = mHoldersForComponents.get(components.get(i).getComponent());

      assertFalse(componentTreeHolder.isTreeValid());
      assertFalse(componentTreeHolder.mLayoutAsyncCalled);
      assertFalse(componentTreeHolder.mLayoutSyncCalled);
    }
  }

  @Test
  public void testRangeBiggerThanContent() {
    final List<ComponentInfo> components = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      components.add(ComponentInfo.create().component(mock(Component.class)).build());
      mRecyclerBinder.insertItemAt(i, components.get(i));
    }

    for (int i = 0; i < 2; i++) {
      Assert.assertNotNull(mHoldersForComponents.get(components.get(i).getComponent()));
    }

    Size size = new Size();
    int widthSpec = SizeSpec.makeSizeSpec(200, SizeSpec.AT_MOST);
    int heightSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);

    mRecyclerBinder.measure(size, widthSpec, heightSpec);

    TestComponentTreeHolder componentTreeHolder =
        mHoldersForComponents.get(components.get(0).getComponent());

    assertTrue(componentTreeHolder.isTreeValid());
    assertTrue(componentTreeHolder.mLayoutSyncCalled);

    for (int i = 1; i < 2; i++) {
      componentTreeHolder = mHoldersForComponents.get(components.get(i).getComponent());

      assertTrue(componentTreeHolder.isTreeValid());
      assertTrue(componentTreeHolder.mLayoutAsyncCalled);
      assertFalse(componentTreeHolder.mLayoutSyncCalled);
    }

    Assert.assertEquals(100, size.width);
  }

  @Test
  public void testMoveRange() {
    final List<ComponentInfo> components = prepareLoadedBinder();
    final int newRangeStart = 40;
    final int newRangeEnd = 42;
    final int rangeTotal = (int) (RANGE_SIZE + (RANGE_RATIO * RANGE_SIZE));

    mRecyclerBinder.onNewVisibleRange(newRangeStart, newRangeEnd);

    TestComponentTreeHolder componentTreeHolder;
    for (int i = 0; i < components.size(); i++) {
      componentTreeHolder = mHoldersForComponents.get(components.get(i).getComponent());

      if (i >= newRangeStart - (RANGE_RATIO * RANGE_SIZE) && i <= newRangeStart + rangeTotal) {
        assertTrue(componentTreeHolder.isTreeValid());
        assertTrue(componentTreeHolder.mLayoutAsyncCalled);
        assertFalse(componentTreeHolder.mLayoutSyncCalled);
      } else {
        assertFalse(componentTreeHolder.isTreeValid());
        assertFalse(componentTreeHolder.mLayoutAsyncCalled);
        assertFalse(componentTreeHolder.mLayoutSyncCalled);
        if (i <= rangeTotal) {
          assertTrue(componentTreeHolder.mDidAcquireStateHandler);
        } else {
          assertFalse(componentTreeHolder.mDidAcquireStateHandler);
        }
      }
    }
  }

  @Test
  public void testRealRangeOverridesEstimatedRange() {
    final List<ComponentInfo> components = prepareLoadedBinder();
    final int newRangeStart = 40;
    final int newRangeEnd = 50;
    int rangeSize = newRangeEnd - newRangeStart;
    final int rangeTotal = (int) (rangeSize + (RANGE_RATIO * rangeSize));

    mRecyclerBinder.onNewVisibleRange(newRangeStart, newRangeEnd);

    TestComponentTreeHolder componentTreeHolder;
    for (int i = 0; i < components.size(); i++) {
      componentTreeHolder = mHoldersForComponents.get(components.get(i).getComponent());

      if (i >= newRangeStart - (RANGE_RATIO * rangeSize) && i <= newRangeStart + rangeTotal) {
        assertTrue(componentTreeHolder.isTreeValid());
        assertTrue(componentTreeHolder.mLayoutAsyncCalled);
        assertFalse(componentTreeHolder.mLayoutSyncCalled);
      } else {
        assertFalse(componentTreeHolder.isTreeValid());
        assertFalse(componentTreeHolder.mLayoutAsyncCalled);
        assertFalse(componentTreeHolder.mLayoutSyncCalled);
      }
    }
  }

  @Test
  public void testMoveRangeToEnd() {
    final List<ComponentInfo> components = prepareLoadedBinder();
    final int newRangeStart = 99;
    final int newRangeEnd = 99;
    final int rangeTotal = (int) (RANGE_SIZE + (RANGE_RATIO * RANGE_SIZE));

    mRecyclerBinder.onNewVisibleRange(newRangeStart, newRangeEnd);

    TestComponentTreeHolder componentTreeHolder;

    for (int i = 0; i < components.size(); i++) {
      componentTreeHolder = mHoldersForComponents.get(components.get(i).getComponent());

      if (i >= newRangeStart - (RANGE_RATIO * RANGE_SIZE) && i <= newRangeStart + rangeTotal) {
        assertTrue(componentTreeHolder.isTreeValid());
        assertTrue(componentTreeHolder.mLayoutAsyncCalled);
        assertFalse(componentTreeHolder.mLayoutSyncCalled);
      } else {
        assertFalse(componentTreeHolder.isTreeValid());
        assertFalse(componentTreeHolder.mLayoutAsyncCalled);
        assertFalse(componentTreeHolder.mLayoutSyncCalled);
        if (i <= rangeTotal) {
          assertTrue(componentTreeHolder.mDidAcquireStateHandler);
        } else {
          assertFalse(componentTreeHolder.mDidAcquireStateHandler);
        }
      }
    }
  }

  @Test
  public void testMoveItemOutsideFromRange() {
    final List<ComponentInfo> components = prepareLoadedBinder();
    mRecyclerBinder.moveItem(0, 99);

    final TestComponentTreeHolder movedHolder =
        mHoldersForComponents.get(components.get(0).getComponent());
    assertFalse(movedHolder.isTreeValid());
    assertFalse(movedHolder.mLayoutAsyncCalled);
    assertFalse(movedHolder.mLayoutSyncCalled);
    assertTrue(movedHolder.mDidAcquireStateHandler);

    final int rangeTotal = (int) (RANGE_SIZE + (RANGE_RATIO * RANGE_SIZE));
    final TestComponentTreeHolder holderMovedIntoRange =
        mHoldersForComponents.get(components.get(rangeTotal + 1).getComponent());

    assertTrue(holderMovedIntoRange.isTreeValid());
    assertTrue(holderMovedIntoRange.mLayoutAsyncCalled);
    assertFalse(holderMovedIntoRange.mLayoutSyncCalled);
  }

  @Test
  public void testMoveItemInsideRange() {
    final List<ComponentInfo> components = prepareLoadedBinder();
    mRecyclerBinder.moveItem(99, 4);

    TestComponentTreeHolder movedHolder =
        mHoldersForComponents.get(components.get(99).getComponent());
    assertTrue(movedHolder.isTreeValid());
    assertTrue(movedHolder.mLayoutAsyncCalled);
    assertFalse(movedHolder.mLayoutSyncCalled);
    assertFalse(movedHolder.mDidAcquireStateHandler);
    final int rangeTotal = (int) (RANGE_SIZE + (RANGE_RATIO * RANGE_SIZE));

    TestComponentTreeHolder holderMovedOutsideRange =
        mHoldersForComponents.get(components.get(rangeTotal).getComponent());

    assertFalse(holderMovedOutsideRange.isTreeValid());
    assertFalse(holderMovedOutsideRange.mLayoutAsyncCalled);
    assertFalse(holderMovedOutsideRange.mLayoutSyncCalled);
  }

  @Test
  public void testMoveItemInsideVisibleRange() {
    final List<ComponentInfo> components = prepareLoadedBinder();
    mRecyclerBinder.moveItem(99, 2);

    final TestComponentTreeHolder movedHolder =
        mHoldersForComponents.get(components.get(99).getComponent());

    assertTrue(movedHolder.isTreeValid());
    assertFalse(movedHolder.mLayoutAsyncCalled);
    assertTrue(movedHolder.mLayoutSyncCalled);
    assertFalse(movedHolder.mDidAcquireStateHandler);
    final int rangeTotal = (int) (RANGE_SIZE + (RANGE_RATIO * RANGE_SIZE));

    final TestComponentTreeHolder holderMovedOutsideRange =
        mHoldersForComponents.get(components.get(rangeTotal).getComponent());

    assertFalse(holderMovedOutsideRange.isTreeValid());
    assertFalse(holderMovedOutsideRange.mLayoutAsyncCalled);
    assertFalse(holderMovedOutsideRange.mLayoutSyncCalled);
    assertTrue(holderMovedOutsideRange.mDidAcquireStateHandler);
  }

  @Test
  public void testMoveItemOutsideRange() {
    final List<ComponentInfo> components = prepareLoadedBinder();
