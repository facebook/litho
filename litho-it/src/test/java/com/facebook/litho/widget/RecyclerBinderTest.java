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

import static com.facebook.litho.SizeSpec.AT_MOST;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.litho.widget.ComponentRenderInfo.create;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.EventHandler;
import com.facebook.litho.LayoutHandler;
import com.facebook.litho.LithoView;
import com.facebook.litho.RenderCompleteEvent;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import com.facebook.litho.viewcompat.SimpleViewBinder;
import com.facebook.litho.viewcompat.ViewBinder;
import com.facebook.litho.viewcompat.ViewCreator;
import com.facebook.litho.widget.ComponentTreeHolder.ComponentTreeMeasureListenerFactory;
import com.facebook.litho.widget.RecyclerBinder.RenderCompleteRunnable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

/**
 * Tests for {@link RecyclerBinder}
 */
@RunWith(ComponentsTestRunner.class)
public class RecyclerBinderTest {

  public static final OnDataBoundListener NO_OP_ON_DATA_BOUND_LISTENER =
      new NoOpOnDataBoundListener();

  private static final float RANGE_RATIO = 2.0f;
  private static final int RANGE_SIZE = 3;

  private static final ViewCreator VIEW_CREATOR_1 =
      new ViewCreator() {
        @Override
        public View createView(Context c, ViewGroup parent) {
          return mock(View.class);
        }
      };

  private static final ViewCreator VIEW_CREATOR_2 =
      new ViewCreator() {
        @Override
        public View createView(Context c, ViewGroup parent) {
          return mock(View.class);
        }
      };

  private static final ViewCreator VIEW_CREATOR_3 =
      new ViewCreator() {
        @Override
        public View createView(Context c, ViewGroup parent) {
          return mock(View.class);
        }
      };

  private static final int SCROLL_RESTORATION_VIEW_POSITION = 1;
  private static final int SCROLL_RESTORATION_RECYCLER_VIEW_SIZE = 30;
  private static final int SCROLL_RESTORATION_PADDING_EDGE = 20;
  private static final int SCROLL_RESTORATION_ITEM_EDGE = 10;
  
  private interface ViewCreatorProvider {
    ViewCreator get();
  }

  private final Map<Component, TestComponentTreeHolder> mHoldersForComponents = new HashMap<>();
  private RecyclerBinder mRecyclerBinder;
  private RecyclerBinder.Builder mRecyclerBinderBuilder;
  private RecyclerBinder mCircularRecyclerBinder;
  private LayoutInfo mLayoutInfo;
  private LayoutInfo mCircularLayoutInfo;
  private ComponentContext mComponentContext;
  private ShadowLooper mLayoutThreadShadowLooper;

  @Before
  public void setup() throws Exception {
    mHoldersForComponents.clear();

    mComponentContext = new ComponentContext(RuntimeEnvironment.application);

    final RecyclerBinder.ComponentTreeHolderFactory componentTreeHolderFactory =
        new RecyclerBinder.ComponentTreeHolderFactory() {
          @Override
          public ComponentTreeHolder create(
              RenderInfo renderInfo,
              LayoutHandler layoutHandler,
              boolean canPrefetchDisplayLists,
              boolean canCacheDrawingDisplayLists,
              ComponentTreeMeasureListenerFactory componentTreeMeasureListenerFactory,
              String splitLayoutTag) {
            final TestComponentTreeHolder holder = new TestComponentTreeHolder(renderInfo);
            if (renderInfo.rendersComponent()) {
              mHoldersForComponents.put(renderInfo.getComponent(), holder);
            }

            return holder;
          }
        };

    mLayoutInfo = mock(LayoutInfo.class);
    mCircularLayoutInfo = mock(LayoutInfo.class);

    setupBaseLayoutInfoMock(mLayoutInfo, OrientationHelper.VERTICAL);
    setupBaseLayoutInfoMock(mCircularLayoutInfo, OrientationHelper.HORIZONTAL);

    mRecyclerBinderBuilder =
        new RecyclerBinder.Builder()
            .rangeRatio(RANGE_RATIO)
            .layoutInfo(mLayoutInfo)
            .componentTreeHolderFactory(componentTreeHolderFactory);

    mRecyclerBinder = mRecyclerBinderBuilder.build(mComponentContext);

    mCircularRecyclerBinder =
        new RecyclerBinder.Builder()
            .rangeRatio(RANGE_RATIO)
            .layoutInfo(mCircularLayoutInfo)
            .componentTreeHolderFactory(componentTreeHolderFactory)
            .isCircular(true)
            .build(mComponentContext);

    mLayoutThreadShadowLooper =
        Shadows.shadowOf(
            (Looper) Whitebox.invokeMethod(ComponentTree.class, "getDefaultLayoutThreadLooper"));
  }

  @After
  public void tearDown() {
    ComponentsConfiguration.fillListViewport = false;
    ComponentsConfiguration.fillListViewportHScrollOnly = false;
    mLayoutThreadShadowLooper.runToEndOfTasks();
  }

  private void setupBaseLayoutInfoMock(LayoutInfo layoutInfo, int orientation) {
    when(layoutInfo.getScrollDirection()).thenReturn(orientation);

    when(layoutInfo.getLayoutManager())
        .thenReturn(new LinearLayoutManager(mComponentContext, orientation, false));

    when(layoutInfo.approximateRangeSize(anyInt(), anyInt(), anyInt(), anyInt()))
        .thenReturn(RANGE_SIZE);

    when(layoutInfo.getChildHeightSpec(anyInt(), any(RenderInfo.class)))
        .thenReturn(SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));
    when(layoutInfo.getChildWidthSpec(anyInt(), any(RenderInfo.class)))
        .thenReturn(SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));
  }

  @Test
  public void testComponentTreeHolderCreation() {
    final List<ComponentRenderInfo> components = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      components.add(ComponentRenderInfo.create().component(mock(Component.class)).build());
      mRecyclerBinder.insertItemAt(0, components.get(i));
    }
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    for (int i = 0; i < 100; i++) {
      Assert.assertNotNull(mHoldersForComponents.get(components.get(i).getComponent()));
    }
  }

  @Test
  public void testAppendItems() {
    final List<ComponentRenderInfo> components = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      components.add(create().component(mock(Component.class)).build());
      mRecyclerBinder.appendItem(components.get(i));
    }
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    for (int i = 0; i < 100; i++) {
      assertThat(mHoldersForComponents.get(components.get(i).getComponent())).isNotNull();
      assertThat(components.get(i)).isEqualTo(mRecyclerBinder.getRenderInfoAt(i));
    }
  }

  @Test
  public void testOnMeasureAfterAddingItems() {
    final List<ComponentRenderInfo> components = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      components.add(create().component(mock(Component.class)).build());
      mRecyclerBinder.insertItemAt(i, components.get(i));
    }
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    for (int i = 0; i < 100; i++) {
      assertThat(mHoldersForComponents.get(components.get(i).getComponent())).isNotNull();
    }

    final Size size = new Size();
    final int widthSpec = makeSizeSpec(200, AT_MOST);
    final int heightSpec = makeSizeSpec(200, EXACTLY);

    mRecyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));

    TestComponentTreeHolder componentTreeHolder =
        mHoldersForComponents.get(components.get(0).getComponent());

    assertThat(componentTreeHolder.isTreeValid()).isTrue();
    assertThat(componentTreeHolder.mLayoutSyncCalled).isTrue();

    int rangeTotal = RANGE_SIZE + (int) (RANGE_SIZE * RANGE_RATIO);

    for (int i = 1; i <= rangeTotal; i++) {
      componentTreeHolder = mHoldersForComponents.get(components.get(i).getComponent());

      assertThat(componentTreeHolder.isTreeValid()).isTrue();
      assertThat(componentTreeHolder.mLayoutAsyncCalled).isTrue();
      assertThat(componentTreeHolder.mLayoutSyncCalled).isFalse();
    }

    for (int k = rangeTotal + 1; k < components.size(); k++) {
      componentTreeHolder = mHoldersForComponents.get(components.get(k).getComponent());

      assertThat(componentTreeHolder.isTreeValid()).isFalse();
      assertThat(componentTreeHolder.mLayoutAsyncCalled).isFalse();
      assertThat(componentTreeHolder.mLayoutSyncCalled).isFalse();
    }

    assertThat(100).isEqualTo(size.width);
  }

  @Test
  public void onBoundsDefined() {
    final List<ComponentRenderInfo> components = prepareLoadedBinder();
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
      assertThat(holder.mLayoutAsyncCalled).isFalse();
      assertThat(holder.mLayoutSyncCalled).isFalse();
    }
  }

  @Test
  public void onBoundsDefinedWithDifferentSize() {
    final List<ComponentRenderInfo> components = prepareLoadedBinder();
    for (int i = 0; i < components.size(); i++) {
      final TestComponentTreeHolder holder =
          mHoldersForComponents.get(components.get(i).getComponent());
      holder.mLayoutAsyncCalled = false;
      holder.mLayoutSyncCalled = false;
    }

    mRecyclerBinder.setSize(300, 200);

    final int rangeTotal = (int) (RANGE_SIZE + (RANGE_RATIO * RANGE_SIZE));

    TestComponentTreeHolder holder = mHoldersForComponents.get(components.get(0).getComponent());
    assertThat(holder.isTreeValid()).isTrue();
    assertThat(holder.mLayoutSyncCalled).isTrue();

    for (int i = 1; i <= rangeTotal; i++) {
      holder = mHoldersForComponents.get(components.get(i).getComponent());
      assertThat(holder.isTreeValid()).isTrue();
      assertThat(holder.mLayoutAsyncCalled).isTrue();
    }

    for (int i = rangeTotal + 1; i < components.size(); i++) {
      holder = mHoldersForComponents.get(components.get(i).getComponent());
      assertThat(holder.isTreeValid()).isFalse();
      assertThat(holder.mLayoutAsyncCalled).isFalse();
      assertThat(holder.mLayoutSyncCalled).isFalse();
    }
  }

  @Test
  public void testMount() {
    RecyclerView recyclerView = mock(RecyclerView.class);
    mRecyclerBinder.mount(recyclerView);

    verify(recyclerView).setLayoutManager(mLayoutInfo.getLayoutManager());
    verify(recyclerView).setAdapter(any(RecyclerView.Adapter.class));
    verify(mLayoutInfo).setRenderInfoCollection(mRecyclerBinder);
    verify(recyclerView, times(2)).addOnScrollListener(any(OnScrollListener.class));
  }

  @Test
  public void testMountWithStaleView() {
    RecyclerView recyclerView = mock(RecyclerView.class);
    mRecyclerBinder.mount(recyclerView);

    verify(recyclerView).setLayoutManager(mLayoutInfo.getLayoutManager());
    verify(recyclerView).setAdapter(any(RecyclerView.Adapter.class));
    verify(recyclerView, times(2)).addOnScrollListener(any(OnScrollListener.class));

    RecyclerView secondRecyclerView = mock(RecyclerView.class);
    mRecyclerBinder.mount(secondRecyclerView);

    verify(recyclerView).setLayoutManager(null);
    verify(recyclerView).setAdapter(null);
    verify(recyclerView, times(2)).removeOnScrollListener(any(OnScrollListener.class));

    verify(secondRecyclerView).setLayoutManager(mLayoutInfo.getLayoutManager());
    verify(secondRecyclerView).setAdapter(any(RecyclerView.Adapter.class));
    verify(secondRecyclerView, times(2)).addOnScrollListener(any(OnScrollListener.class));
  }

  @Test
  public void testUnmount() {
    RecyclerView recyclerView = mock(RecyclerView.class);
    mRecyclerBinder.mount(recyclerView);

    verify(recyclerView).setLayoutManager(mLayoutInfo.getLayoutManager());
    verify(recyclerView).setAdapter(any(RecyclerView.Adapter.class));
    verify(recyclerView, times(2)).addOnScrollListener(any(OnScrollListener.class));

    mRecyclerBinder.unmount(recyclerView);

    verify(recyclerView).setLayoutManager(null);
    verify(recyclerView).setAdapter(null);
    verify(mLayoutInfo).setRenderInfoCollection(null);
    verify(recyclerView, times(2)).removeOnScrollListener(any(OnScrollListener.class));
  }

  @Test
  public void testScrollRestorationVertical() {
    testScrollRestoration(true /* verticalScroll */, false /* reverseLayout */);
  }

  @Test
  public void testScrollRestorationVerticalReversed() {
    testScrollRestoration(true /* verticalScroll */, true /* reverseLayout */);
  }

  @Test
  public void testScrollRestorationHorizontal() {
    testScrollRestoration(false /* verticalScroll */, false /* reverseLayout */);
  }

  @Test
  public void testScrollRestorationHorizontalReversed() {
    testScrollRestoration(false /* verticalScroll */, true /* reverseLayout */);
  }

  private void testScrollRestoration(boolean verticalScroll, boolean reverseLayout) {
    View firstView = mock(View.class);

    LinearLayoutManager layoutManager = mock(LinearLayoutManager.class);
    when(layoutManager.findViewByPosition(SCROLL_RESTORATION_VIEW_POSITION)).thenReturn(firstView);
    when(layoutManager.getReverseLayout()).thenReturn(reverseLayout);

    RecyclerView recyclerView = mock(RecyclerView.class);
    when(recyclerView.getLayoutManager()).thenReturn(layoutManager);

    final LayoutInfo layoutInfo = mock(LayoutInfo.class);
    when(layoutInfo.getScrollDirection())
        .thenReturn(verticalScroll ? OrientationHelper.VERTICAL : OrientationHelper.HORIZONTAL);
    when(layoutInfo.getLayoutManager()).thenReturn(layoutManager);

    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .rangeRatio(RANGE_RATIO)
            .layoutInfo(layoutInfo)
            .build(mComponentContext);

    // Arbitrary relevant dimensions for the RecyclerView, its padding, and the item view.
    final int recyclerViewSize = SCROLL_RESTORATION_RECYCLER_VIEW_SIZE;
    final int paddingEdge = SCROLL_RESTORATION_PADDING_EDGE;
    final int itemEdge = SCROLL_RESTORATION_ITEM_EDGE;

    final int paddingSize = reverseLayout ? recyclerViewSize - paddingEdge : paddingEdge;
    final int trueOffset = reverseLayout ? paddingEdge - itemEdge : itemEdge - paddingEdge;

    if (verticalScroll) {
      if (reverseLayout) {
        when(recyclerView.getHeight()).thenReturn(recyclerViewSize);
        when(layoutManager.getPaddingBottom()).thenReturn(paddingSize);
        when(layoutManager.getDecoratedBottom(firstView)).thenReturn(itemEdge);
      } else {
        when(layoutManager.getPaddingTop()).thenReturn(paddingSize);
        when(layoutManager.getDecoratedTop(firstView)).thenReturn(itemEdge);
      }
    } else {
      if (reverseLayout) {
        when(recyclerView.getWidth()).thenReturn(recyclerViewSize);
        when(layoutManager.getPaddingRight()).thenReturn(paddingSize);
        when(layoutManager.getDecoratedRight(firstView)).thenReturn(itemEdge);
      } else {
        when(layoutManager.getPaddingLeft()).thenReturn(paddingSize);
        when(layoutManager.getDecoratedLeft(firstView)).thenReturn(itemEdge);
      }
    }

    recyclerBinder.mount(recyclerView);
    // Tell the RecyclerBinder to use our arbitrary position for scroll restoration.
    recyclerBinder.onNewVisibleRange(
        SCROLL_RESTORATION_VIEW_POSITION, SCROLL_RESTORATION_VIEW_POSITION);
    // Unmount the RecyclerView, causing the current scroll offset to be stored.
    recyclerBinder.unmount(recyclerView);
    // Remount the RecyclerView, causing it to scroll using the stored scroll offset.
    recyclerBinder.mount(recyclerView);

    verify(layoutManager).scrollToPositionWithOffset(SCROLL_RESTORATION_VIEW_POSITION, trueOffset);
  }

  @Test
  public void testAddStickyHeaderIfSectionsRecyclerViewExists() throws Exception {
    RecyclerView recyclerView = mock(RecyclerView.class);
    SectionsRecyclerView recycler = mock(SectionsRecyclerView.class);

    when(recyclerView.getParent()).thenReturn(recycler);
    when(recyclerView.getLayoutManager()).thenReturn(mock(RecyclerView.LayoutManager.class));
    when(recycler.getRecyclerView()).thenReturn(recyclerView);

    mRecyclerBinder.mount(recyclerView);

    verify(recyclerView).setAdapter(any(RecyclerView.Adapter.class));
    verify(recyclerView, times(3)).addOnScrollListener(any(OnScrollListener.class));
  }

  @Test
  public void onRemeasureWithDifferentSize() {
    final List<ComponentRenderInfo> components = prepareLoadedBinder();
    for (int i = 0; i < components.size(); i++) {
      final TestComponentTreeHolder holder =
          mHoldersForComponents.get(components.get(i).getComponent());
      holder.mLayoutAsyncCalled = false;
      holder.mLayoutSyncCalled = false;
    }

    int widthSpec = makeSizeSpec(100, EXACTLY);
    int heightSpec = makeSizeSpec(200, EXACTLY);

    mRecyclerBinder.measure(new Size(), widthSpec, heightSpec, null);
    final int rangeTotal = (int) (RANGE_SIZE + (RANGE_RATIO * RANGE_SIZE));

    TestComponentTreeHolder holder = mHoldersForComponents.get(components.get(0).getComponent());
    assertThat(holder.isTreeValid()).isTrue();
    assertThat(holder.mLayoutSyncCalled).isTrue();

    for (int i = 1; i <= rangeTotal; i++) {
      holder = mHoldersForComponents.get(components.get(i).getComponent());
      assertThat(holder.isTreeValid()).isTrue();
      assertThat(holder.mLayoutAsyncCalled).isTrue();
    }

    for (int i = rangeTotal + 1; i < components.size(); i++) {
      holder = mHoldersForComponents.get(components.get(i).getComponent());
      assertThat(holder.isTreeValid()).isFalse();
      assertThat(holder.mLayoutAsyncCalled).isFalse();
      assertThat(holder.mLayoutSyncCalled).isFalse();
    }
  }

  @Test
  public void testComponentWithDifferentSpanSize() {
    final List<ComponentRenderInfo> components = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      components.add(create()
          .component(mock(Component.class))
          .spanSize((i == 0 || i % 3 == 0) ? 2 : 1)
          .build());

      mRecyclerBinder.insertItemAt(i, components.get(i));
    }
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    when(mLayoutInfo.getChildWidthSpec(anyInt(), any(RenderInfo.class)))
        .thenAnswer(new Answer<Integer>() {
          @Override
          public Integer answer(InvocationOnMock invocation) throws Throwable {
            final RenderInfo renderInfo = (RenderInfo) invocation.getArguments()[1];
            final int spanSize = renderInfo.getSpanSize();

            return makeSizeSpec(100 * spanSize, EXACTLY);
          }
        });

    for (int i = 0; i < 100; i++) {
      assertThat(mHoldersForComponents.get(components.get(i).getComponent())).isNotNull();
    }

    Size size = new Size();
    int widthSpec = makeSizeSpec(200, EXACTLY);
    int heightSpec = makeSizeSpec(200, EXACTLY);

    mRecyclerBinder.measure(size, widthSpec, heightSpec, null);

    TestComponentTreeHolder componentTreeHolder =
        mHoldersForComponents.get(components.get(0).getComponent());

    assertThat(componentTreeHolder.isTreeValid()).isTrue();
    assertThat(componentTreeHolder.mLayoutSyncCalled).isTrue();
    assertThat(200).isEqualTo(size.width);

    final int rangeTotal = (int) (RANGE_SIZE + (RANGE_RATIO * RANGE_SIZE));

    for (int i = 1; i <= rangeTotal; i++) {
      componentTreeHolder = mHoldersForComponents.get(components.get(i).getComponent());

      assertThat(componentTreeHolder.isTreeValid()).isTrue();
      assertThat(componentTreeHolder.mLayoutAsyncCalled).isTrue();

      final int expectedWidth = i % 3 == 0 ? 200 : 100;
      assertThat(expectedWidth).isEqualTo(componentTreeHolder.mChildWidth);
      assertThat(100).isEqualTo(componentTreeHolder.mChildHeight);
    }
  }

  @Test
  public void testAddItemsAfterMeasuring() {
    final Size size = new Size();
    final int widthSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);
    final int heightSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);

    mRecyclerBinder.measure(size, widthSpec, heightSpec, null);

    assertThat(200).isEqualTo(size.width);

    final List<ComponentRenderInfo> components = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      components.add(ComponentRenderInfo.create().component(mock(Component.class)).build());
      mRecyclerBinder.insertItemAt(i, components.get(i));
    }
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    for (int i = 0; i < 100; i++) {
      Assert.assertNotNull(mHoldersForComponents.get(components.get(i).getComponent()));
    }

    TestComponentTreeHolder componentTreeHolder;

    int rangeTotal = RANGE_SIZE + (int) (RANGE_SIZE * RANGE_RATIO);

    // The first component is used to calculate the range
    TestComponentTreeHolder firstHolder =
        mHoldersForComponents.get(components.get(0).getComponent());
    assertThat(firstHolder.isTreeValid()).isTrue();
    assertThat(firstHolder.mLayoutSyncCalled).isTrue();

    for (int i = 1; i < RANGE_SIZE; i++) {
      componentTreeHolder = mHoldersForComponents.get(components.get(i).getComponent());

      assertThat(componentTreeHolder.isTreeValid()).isTrue();
      assertThat(componentTreeHolder.mLayoutAsyncCalled).isTrue();
      assertThat(componentTreeHolder.mLayoutSyncCalled).isFalse();
    }

    for (int i = RANGE_SIZE; i <= rangeTotal; i++) {
      componentTreeHolder = mHoldersForComponents.get(components.get(i).getComponent());

      assertThat(componentTreeHolder.isTreeValid()).isTrue();
      assertThat(componentTreeHolder.mLayoutAsyncCalled).isTrue();
      assertThat(componentTreeHolder.mLayoutSyncCalled).isFalse();
    }

    for (int i = rangeTotal + 1; i < components.size(); i++) {
      componentTreeHolder = mHoldersForComponents.get(components.get(i).getComponent());

      assertThat(componentTreeHolder.isTreeValid()).isFalse();
      assertThat(componentTreeHolder.mLayoutAsyncCalled).isFalse();
      assertThat(componentTreeHolder.mLayoutSyncCalled).isFalse();
    }
  }

  @Test
  public void testRequestRemeasure() {
    final Size size = new Size();
    final int widthSpec = SizeSpec.makeSizeSpec(200, SizeSpec.AT_MOST);
    final int heightSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);
    final RecyclerView recyclerView = mock(RecyclerView.class);

    mRecyclerBinder.mount(recyclerView);
    mRecyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));

    assertThat(0).isEqualTo(size.width);

    final List<ComponentRenderInfo> components = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      components.add(ComponentRenderInfo.create().component(mock(Component.class)).build());
      mRecyclerBinder.insertItemAt(i, components.get(i));
    }
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    verify(recyclerView).removeCallbacks(mRecyclerBinder.mRemeasureRunnable);
    verify(recyclerView).postOnAnimation(mRecyclerBinder.mRemeasureRunnable);
  }

  @Test
  public void testRequestRemeasureInsertRange() {
    final Size size = new Size();
    final int widthSpec = SizeSpec.makeSizeSpec(200, SizeSpec.AT_MOST);
    final int heightSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);
    final RecyclerView recyclerView = mock(RecyclerView.class);

    mRecyclerBinder.mount(recyclerView);
    mRecyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));

    assertThat(0).isEqualTo(size.width);

    final List<RenderInfo> components = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      components.add(ComponentRenderInfo.create().component(mock(Component.class)).build());
    }
    mRecyclerBinder.insertRangeAt(0, components);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    verify(recyclerView).removeCallbacks(mRecyclerBinder.mRemeasureRunnable);
    verify(recyclerView).postOnAnimation(mRecyclerBinder.mRemeasureRunnable);
  }

  @Test
  public void testRequestRemeasureUpdateAt() {
    final Size size = new Size();
    final int widthSpec = SizeSpec.makeSizeSpec(200, SizeSpec.AT_MOST);
    final int heightSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);
    final RecyclerView recyclerView = mock(RecyclerView.class);

    mRecyclerBinder.mount(recyclerView);
    mRecyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));

    assertThat(0).isEqualTo(size.width);

    final List<RenderInfo> components = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      components.add(ComponentRenderInfo.create().component(mock(Component.class)).build());
    }
    mRecyclerBinder.insertRangeAt(0, components);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    reset(recyclerView);

    for (int i = 0; i < 50; i++) {
      mRecyclerBinder.updateItemAt(
          i, ComponentRenderInfo.create().component(mock(Component.class)).build());
    }
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    verify(recyclerView).removeCallbacks(mRecyclerBinder.mRemeasureRunnable);
    verify(recyclerView).postOnAnimation(mRecyclerBinder.mRemeasureRunnable);
  }

  @Test
  public void testRequestRemeasureUpdateRange() {
    final Size size = new Size();
    final int widthSpec = SizeSpec.makeSizeSpec(200, SizeSpec.AT_MOST);
    final int heightSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);
    final RecyclerView recyclerView = mock(RecyclerView.class);

    mRecyclerBinder.mount(recyclerView);
    mRecyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));

    assertThat(0).isEqualTo(size.width);

    final List<RenderInfo> components = new ArrayList<>();
    final List<RenderInfo> updatedComponents = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      components.add(ComponentRenderInfo.create().component(mock(Component.class)).build());
    }
    for (int i = 0; i < 50; i++) {
      updatedComponents.add(ComponentRenderInfo.create().component(mock(Component.class)).build());
    }
    mRecyclerBinder.insertRangeAt(0, components);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    reset(recyclerView);

    mRecyclerBinder.updateRangeAt(0, updatedComponents);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    verify(recyclerView).removeCallbacks(mRecyclerBinder.mRemeasureRunnable);
    verify(recyclerView).postOnAnimation(mRecyclerBinder.mRemeasureRunnable);
  }

    @Test
    public void testDoesntRequestRemeasure() {
      final Size size = new Size();
      final int widthSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);
      final int heightSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);
      final RecyclerView recyclerView = mock(RecyclerView.class);

      mRecyclerBinder.mount(recyclerView);
      mRecyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));

    assertThat(200).isEqualTo(size.width);

      final List<ComponentRenderInfo> components = new ArrayList<>();
      for (int i = 0; i < 100; i++) {
        components.add(ComponentRenderInfo.create().component(mock(Component.class)).build());
        mRecyclerBinder.insertItemAt(i, components.get(i));
      }
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    verify(recyclerView, never()).removeCallbacks(mRecyclerBinder.mRemeasureRunnable);
    verify(recyclerView, never()).postOnAnimation(mRecyclerBinder.mRemeasureRunnable);
    }

  @Test
  public void testRangeBiggerThanContent() {
    final List<ComponentRenderInfo> components = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      components.add(create().component(mock(Component.class)).build());
      mRecyclerBinder.insertItemAt(i, components.get(i));
    }
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    for (int i = 0; i < 2; i++) {
      assertThat(mHoldersForComponents.get(components.get(i).getComponent())).isNotNull();
    }

    Size size = new Size();
    int widthSpec = makeSizeSpec(200, AT_MOST);
    int heightSpec = makeSizeSpec(200, EXACTLY);

    mRecyclerBinder.measure(size, widthSpec, heightSpec, mock(EventHandler.class));

    TestComponentTreeHolder componentTreeHolder =
        mHoldersForComponents.get(components.get(0).getComponent());

    assertThat(componentTreeHolder.isTreeValid()).isTrue();
    assertThat(componentTreeHolder.mLayoutSyncCalled).isTrue();

    for (int i = 1; i < 2; i++) {
      componentTreeHolder = mHoldersForComponents.get(components.get(i).getComponent());

      assertThat(componentTreeHolder.isTreeValid()).isTrue();
      assertThat(componentTreeHolder.mLayoutAsyncCalled).isTrue();
      assertThat(componentTreeHolder.mLayoutSyncCalled).isFalse();
    }

    assertThat(100).isEqualTo(size.width);
  }

  @Test
  public void testMoveRange() {
    final List<ComponentRenderInfo> components = prepareLoadedBinder();
    final int newRangeStart = 40;
    final int newRangeEnd = 42;
    final int rangeTotal = (int) (RANGE_SIZE + (RANGE_RATIO * RANGE_SIZE));

    mRecyclerBinder.onNewVisibleRange(newRangeStart, newRangeEnd);

    TestComponentTreeHolder componentTreeHolder;
    for (int i = 0; i < components.size(); i++) {
      componentTreeHolder = mHoldersForComponents.get(components.get(i).getComponent());

      if (i >= newRangeStart - (RANGE_RATIO * RANGE_SIZE) && i <= newRangeStart + rangeTotal) {
        assertThat(componentTreeHolder.isTreeValid()).isTrue();
        assertThat(componentTreeHolder.mLayoutAsyncCalled).isTrue();
        assertThat(componentTreeHolder.mLayoutSyncCalled).isFalse();
      } else {
        assertThat(componentTreeHolder.isTreeValid()).isFalse();
        assertThat(componentTreeHolder.mLayoutAsyncCalled).isFalse();
        assertThat(componentTreeHolder.mLayoutSyncCalled).isFalse();
        if (i <= rangeTotal) {
          assertThat(componentTreeHolder.mDidAcquireStateHandler).isTrue();
        } else {
          assertThat(componentTreeHolder.mDidAcquireStateHandler).isFalse();
        }
      }
    }
  }

  @Test
  public void testRealRangeOverridesEstimatedRange() {
    final List<ComponentRenderInfo> components = prepareLoadedBinder();
    final int newRangeStart = 40;
    final int newRangeEnd = 50;
    int rangeSize = newRangeEnd - newRangeStart;
    final int rangeTotal = (int) (rangeSize + (RANGE_RATIO * rangeSize));

    mRecyclerBinder.onNewVisibleRange(newRangeStart, newRangeEnd);

    TestComponentTreeHolder componentTreeHolder;
    for (int i = 0; i < components.size(); i++) {
      componentTreeHolder = mHoldersForComponents.get(components.get(i).getComponent());

      if (i >= newRangeStart - (RANGE_RATIO * rangeSize) && i <= newRangeStart + rangeTotal) {
        assertThat(componentTreeHolder.isTreeValid()).isTrue();
        assertThat(componentTreeHolder.mLayoutAsyncCalled).isTrue();
        assertThat(componentTreeHolder.mLayoutSyncCalled).isFalse();
      } else {
        assertThat(componentTreeHolder.isTreeValid()).isFalse();
        assertThat(componentTreeHolder.mLayoutAsyncCalled).isFalse();
        assertThat(componentTreeHolder.mLayoutSyncCalled).isFalse();
      }
    }
  }

  @Test
  public void testStickyComponentsStayValidOutsideRange() {
    final List<ComponentRenderInfo> components = prepareLoadedBinder();
    makeIndexSticky(components, 5);
    makeIndexSticky(components, 40);
    makeIndexSticky(components, 80);

    Size size = new Size();
    int widthSpec = makeSizeSpec(200, EXACTLY);
    int heightSpec = makeSizeSpec(200, EXACTLY);

    mRecyclerBinder.measure(size, widthSpec, heightSpec, null);

    assertThat(mHoldersForComponents.get(components.get(5).getComponent()).isTreeValid()).isTrue();

    final int newRangeStart = 40;
    final int newRangeEnd = 50;
    int rangeSize = newRangeEnd - newRangeStart;
    final int rangeTotal = (int) (rangeSize + (RANGE_RATIO * rangeSize));

    mRecyclerBinder.onNewVisibleRange(newRangeStart, newRangeEnd);

    TestComponentTreeHolder componentTreeHolder;
    for (int i = 0; i < components.size(); i++) {
      componentTreeHolder = mHoldersForComponents.get(components.get(i).getComponent());
      boolean isIndexInRange =
          i >= newRangeStart - (RANGE_RATIO * rangeSize) && i <= newRangeStart + rangeTotal;
      boolean isPreviouslyComputedTreeAndSticky =
          i <= newRangeStart + rangeTotal && componentTreeHolder.getRenderInfo().isSticky();

      if (isIndexInRange || isPreviouslyComputedTreeAndSticky) {
        assertThat(componentTreeHolder.isTreeValid()).isTrue();
        assertThat(componentTreeHolder.mLayoutAsyncCalled).isTrue();
        assertThat(componentTreeHolder.mLayoutSyncCalled).isFalse();
      } else {
        assertThat(componentTreeHolder.isTreeValid()).isFalse();
        assertThat(componentTreeHolder.mLayoutAsyncCalled).isFalse();
        assertThat(componentTreeHolder.mLayoutSyncCalled).isFalse();
      }
    }
  }

  @Test
  public void testMoveRangeToEnd() {
    final List<ComponentRenderInfo> components = prepareLoadedBinder();
    final int newRangeStart = 99;
    final int newRangeEnd = 99;
    final int rangeTotal = (int) (RANGE_SIZE + (RANGE_RATIO * RANGE_SIZE));

    mRecyclerBinder.onNewVisibleRange(newRangeStart, newRangeEnd);

    TestComponentTreeHolder componentTreeHolder;

    for (int i = 0; i < components.size(); i++) {
      componentTreeHolder = mHoldersForComponents.get(components.get(i).getComponent());

      if (i >= newRangeStart - (RANGE_RATIO * RANGE_SIZE) && i <= newRangeStart + rangeTotal) {
        assertThat(componentTreeHolder.isTreeValid()).isTrue();
        assertThat(componentTreeHolder.mLayoutAsyncCalled).isTrue();
        assertThat(componentTreeHolder.mLayoutSyncCalled).isFalse();
      } else {
        assertThat(componentTreeHolder.isTreeValid()).isFalse();
        assertThat(componentTreeHolder.mLayoutAsyncCalled).isFalse();
        assertThat(componentTreeHolder.mLayoutSyncCalled).isFalse();
        if (i <= rangeTotal) {
          assertThat(componentTreeHolder.mDidAcquireStateHandler).isTrue();
        } else {
          assertThat(componentTreeHolder.mDidAcquireStateHandler).isFalse();
        }
      }
    }
  }

  @Test
  public void testMoveItemOutsideFromRange() {
    final List<ComponentRenderInfo> components = prepareLoadedBinder();
    mRecyclerBinder.moveItem(0, 99);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    final TestComponentTreeHolder movedHolder =
        mHoldersForComponents.get(components.get(0).getComponent());
    assertThat(movedHolder.isTreeValid()).isFalse();
    assertThat(movedHolder.mLayoutAsyncCalled).isFalse();
    assertThat(movedHolder.mLayoutSyncCalled).isFalse();
    assertThat(movedHolder.mDidAcquireStateHandler).isTrue();

    final int rangeTotal = (int) (RANGE_SIZE + (RANGE_RATIO * RANGE_SIZE));
    final TestComponentTreeHolder holderMovedIntoRange =
        mHoldersForComponents.get(components.get(rangeTotal + 1).getComponent());

    assertThat(holderMovedIntoRange.isTreeValid()).isTrue();
    assertThat(holderMovedIntoRange.mLayoutAsyncCalled).isTrue();
    assertThat(holderMovedIntoRange.mLayoutSyncCalled).isFalse();
  }

  @Test
  public void testMoveItemInsideRange() {
    final List<ComponentRenderInfo> components = prepareLoadedBinder();
    mRecyclerBinder.moveItem(99, 4);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    TestComponentTreeHolder movedHolder =
        mHoldersForComponents.get(components.get(99).getComponent());
    assertThat(movedHolder.isTreeValid()).isTrue();
    assertThat(movedHolder.mLayoutAsyncCalled).isTrue();
    assertThat(movedHolder.mLayoutSyncCalled).isFalse();
    assertThat(movedHolder.mDidAcquireStateHandler).isFalse();
    final int rangeTotal = (int) (RANGE_SIZE + (RANGE_RATIO * RANGE_SIZE));

    TestComponentTreeHolder holderMovedOutsideRange =
        mHoldersForComponents.get(components.get(rangeTotal).getComponent());

    assertThat(holderMovedOutsideRange.isTreeValid()).isFalse();
    assertThat(holderMovedOutsideRange.mLayoutAsyncCalled).isFalse();
    assertThat(holderMovedOutsideRange.mLayoutSyncCalled).isFalse();
  }

  @Test
  public void testMoveItemInsideVisibleRange() {
    final List<ComponentRenderInfo> components = prepareLoadedBinder();
    mRecyclerBinder.moveItem(99, 2);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    final TestComponentTreeHolder movedHolder =
        mHoldersForComponents.get(components.get(99).getComponent());

    assertThat(movedHolder.isTreeValid()).isTrue();
    assertThat(movedHolder.mLayoutAsyncCalled).isTrue();
    assertThat(movedHolder.mLayoutSyncCalled).isFalse();
    assertThat(movedHolder.mDidAcquireStateHandler).isFalse();
    final int rangeTotal = (int) (RANGE_SIZE + (RANGE_RATIO * RANGE_SIZE));

    final TestComponentTreeHolder holderMovedOutsideRange =
        mHoldersForComponents.get(components.get(rangeTotal).getComponent());

    assertThat(holderMovedOutsideRange.isTreeValid()).isFalse();
    assertThat(holderMovedOutsideRange.mLayoutAsyncCalled).isFalse();
    assertThat(holderMovedOutsideRange.mLayoutSyncCalled).isFalse();
    assertThat(holderMovedOutsideRange.mDidAcquireStateHandler).isTrue();
  }

  @Test
  public void testMoveItemOutsideRange() {
    final List<ComponentRenderInfo> components = prepareLoadedBinder();
    mRecyclerBinder.moveItem(2, 99);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    final TestComponentTreeHolder movedHolder =
        mHoldersForComponents.get(components.get(2).getComponent());
    assertThat(movedHolder.isTreeValid()).isFalse();
    assertThat(movedHolder.mLayoutAsyncCalled).isFalse();
    assertThat(movedHolder.mLayoutSyncCalled).isFalse();
    assertThat(movedHolder.mDidAcquireStateHandler).isTrue();

    final int rangeTotal = (int) (RANGE_SIZE + (RANGE_RATIO * RANGE_SIZE));
    final TestComponentTreeHolder holderMovedInsideRange =
        mHoldersForComponents.get(components.get(rangeTotal + 1).getComponent());

    assertThat(holderMovedInsideRange.isTreeValid()).isTrue();
    assertThat(holderMovedInsideRange.mLayoutAsyncCalled).isTrue();
    assertThat(holderMovedInsideRange.mLayoutSyncCalled).isFalse();
    assertThat(holderMovedInsideRange.mDidAcquireStateHandler).isFalse();
  }

  @Test
  public void testMoveWithinRange() {
    final List<ComponentRenderInfo> components = prepareLoadedBinder();

    final TestComponentTreeHolder movedHolderOne =
        mHoldersForComponents.get(components.get(0).getComponent());
    final TestComponentTreeHolder movedHolderTwo =
        mHoldersForComponents.get(components.get(1).getComponent());

    movedHolderOne.mLayoutSyncCalled = false;
    movedHolderOne.mLayoutAsyncCalled = false;
    movedHolderOne.mDidAcquireStateHandler = false;

    movedHolderTwo.mLayoutSyncCalled = false;
    movedHolderTwo.mLayoutAsyncCalled = false;
    movedHolderTwo.mDidAcquireStateHandler = false;

    mRecyclerBinder.moveItem(0, 1);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(movedHolderOne.isTreeValid()).isTrue();
    assertThat(movedHolderOne.mLayoutAsyncCalled).isFalse();
    assertThat(movedHolderOne.mLayoutSyncCalled).isFalse();
    assertThat(movedHolderOne.mDidAcquireStateHandler).isFalse();

    assertThat(movedHolderTwo.isTreeValid()).isTrue();
    assertThat(movedHolderTwo.mLayoutAsyncCalled).isFalse();
    assertThat(movedHolderTwo.mLayoutSyncCalled).isFalse();
    assertThat(movedHolderTwo.mDidAcquireStateHandler).isFalse();
  }

  @Test
  public void testInsertInVisibleRange() {
    final List<ComponentRenderInfo> components = prepareLoadedBinder();
    final ComponentRenderInfo newRenderInfo =
        create().component(mock(Component.class)).build();

    mRecyclerBinder.insertItemAt(1, newRenderInfo);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
    final TestComponentTreeHolder holder =
        mHoldersForComponents.get(newRenderInfo.getComponent());

    assertThat(holder.isTreeValid()).isTrue();
    assertThat(holder.mLayoutSyncCalled).isFalse();
    assertThat(holder.mLayoutAsyncCalled).isTrue();
    assertThat(holder.mDidAcquireStateHandler).isFalse();

    final int rangeTotal = (int) (RANGE_SIZE + (RANGE_RATIO * RANGE_SIZE));
    final TestComponentTreeHolder holderMovedOutsideRange =
        mHoldersForComponents.get(components.get(rangeTotal).getComponent());

    assertThat(holderMovedOutsideRange.isTreeValid()).isFalse();
    assertThat(holderMovedOutsideRange.mLayoutSyncCalled).isFalse();
    assertThat(holderMovedOutsideRange.mLayoutAsyncCalled).isFalse();
    assertThat(holderMovedOutsideRange.mDidAcquireStateHandler).isTrue();
  }

  void changeViewportTo(int firstVisiblePosition, int lastVisiblePosition) {
    when(mLayoutInfo.findFirstVisibleItemPosition()).thenReturn(firstVisiblePosition);
    when(mLayoutInfo.findLastVisibleItemPosition()).thenReturn(lastVisiblePosition);

    mRecyclerBinder.mViewportManager.onViewportChanged(ViewportInfo.State.DATA_CHANGES);
  }

  @Test
  public void testRemoveRangeAboveTheViewport() {
    final List<ComponentRenderInfo> components = prepareLoadedBinder();
    RecyclerView recyclerView = new RecyclerView(RuntimeEnvironment.application);
    mRecyclerBinder.mount(recyclerView);

    final int firstVisible = 40;
    final int lastVisible = 50;
    int rangeSize = lastVisible - firstVisible;

    changeViewportTo(firstVisible, lastVisible);

    assertThat(mRecyclerBinder.mViewportManager.shouldUpdate()).isFalse();

    int removeRangeSize = rangeSize;
    // Remove above the visible range
    mRecyclerBinder.removeRangeAt(0, removeRangeSize);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(mRecyclerBinder.mViewportManager.shouldUpdate()).isTrue();

    // compute range not yet updated, range will be updated in next frame
    assertThat(mRecyclerBinder.mCurrentFirstVisiblePosition).isEqualTo(firstVisible);
    assertThat(mRecyclerBinder.mCurrentLastVisiblePosition).isEqualTo(lastVisible);
  }

  @Test
  public void testRemoveRangeBelowTheViewport() {
    final List<ComponentRenderInfo> components = prepareLoadedBinder();
    RecyclerView recyclerView = new RecyclerView(RuntimeEnvironment.application);
    mRecyclerBinder.mount(recyclerView);

    final int firstVisible = 40;
    final int lastVisible = 50;
    int rangeSize = lastVisible - firstVisible;

    changeViewportTo(firstVisible, lastVisible);

    assertThat(mRecyclerBinder.mViewportManager.shouldUpdate()).isFalse();

    int removeRangeSize = rangeSize;
    // Remove below the visible range
    mRecyclerBinder.removeRangeAt(lastVisible + 1, removeRangeSize);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(mRecyclerBinder.mViewportManager.shouldUpdate()).isFalse();

    // compute range has been updated and range did not change
    assertThat(mRecyclerBinder.mCurrentFirstVisiblePosition).isEqualTo(firstVisible);
    assertThat(mRecyclerBinder.mCurrentLastVisiblePosition).isEqualTo(lastVisible);
  }

  @Test
  public void testInsertInRange() {
    final List<ComponentRenderInfo> components = prepareLoadedBinder();
    final ComponentRenderInfo newRenderInfo =
        create().component(mock(Component.class)).build();

    mRecyclerBinder.insertItemAt(RANGE_SIZE + 1, newRenderInfo);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
    final TestComponentTreeHolder holder =
        mHoldersForComponents.get(newRenderInfo.getComponent());

    assertThat(holder.isTreeValid()).isTrue();
    assertThat(holder.mLayoutSyncCalled).isFalse();
    assertThat(holder.mLayoutAsyncCalled).isTrue();
    assertThat(holder.mDidAcquireStateHandler).isFalse();

    final int rangeTotal = (int) (RANGE_SIZE + (RANGE_RATIO * RANGE_SIZE));
    final TestComponentTreeHolder holderMovedOutsideRange =
        mHoldersForComponents.get(components.get(rangeTotal).getComponent());

    assertThat(holderMovedOutsideRange.isTreeValid()).isFalse();
    assertThat(holderMovedOutsideRange.mLayoutSyncCalled).isFalse();
    assertThat(holderMovedOutsideRange.mLayoutAsyncCalled).isFalse();
    assertThat(holderMovedOutsideRange.mDidAcquireStateHandler).isTrue();
  }

  @Test
  public void testInsertRange() {
    final List<ComponentRenderInfo> components = prepareLoadedBinder();
    final List<RenderInfo> newComponents = new ArrayList<>();

    for (int i = 0; i < 3; i++) {
      newComponents.add(
          ComponentRenderInfo.create().component(mock(Component.class)).build());
    }

    mRecyclerBinder.insertRangeAt(0, newComponents);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    // The new elements were scheduled for layout.
    for (int i = 0; i < 3; i++) {
      final TestComponentTreeHolder holder =
          mHoldersForComponents.get(((ComponentRenderInfo) newComponents.get(i)).getComponent());
      assertThat(holder.isTreeValid()).isTrue();
      assertThat(holder.mLayoutSyncCalled).isFalse();
      assertThat(holder.mLayoutAsyncCalled).isTrue();
      assertThat(holder.mDidAcquireStateHandler).isFalse();
    }

    final int rangeTotal = (int) (RANGE_SIZE + (RANGE_RATIO * RANGE_SIZE));

    // The elements that went outside the layout range have been released.
    for (int i = rangeTotal - 2; i <= rangeTotal; i++) {
      final TestComponentTreeHolder holder =
          mHoldersForComponents.get(components.get(i).getComponent());
      assertThat(holder.isTreeValid()).isFalse();
      assertThat(holder.mLayoutSyncCalled).isFalse();
      assertThat(holder.mLayoutAsyncCalled).isFalse();
      assertThat(holder.mDidAcquireStateHandler).isTrue();
    }
  }

  @Test
  public void testInsertOusideRange() {
    prepareLoadedBinder();
    final ComponentRenderInfo newRenderInfo =
        create().component(mock(Component.class)).build();
    final int rangeTotal = (int) (RANGE_SIZE + (RANGE_RATIO * RANGE_SIZE));

    mRecyclerBinder.insertItemAt(rangeTotal + 1, newRenderInfo);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    final TestComponentTreeHolder holder =
        mHoldersForComponents.get(newRenderInfo.getComponent());

    assertThat(holder.isTreeValid()).isFalse();
    assertThat(holder.mLayoutSyncCalled).isFalse();
    assertThat(holder.mLayoutAsyncCalled).isFalse();
    assertThat(holder.mDidAcquireStateHandler).isFalse();
  }

  @Test
  public void testRemoveItem() {
    final List<ComponentRenderInfo> components = prepareLoadedBinder();
    final int rangeTotal = (int) (RANGE_SIZE + (RANGE_RATIO * RANGE_SIZE));

    mRecyclerBinder.removeItemAt(rangeTotal + 1);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    final TestComponentTreeHolder holder =
        mHoldersForComponents.get(components.get(rangeTotal + 1).getComponent());
    assertThat(holder.mReleased).isTrue();
  }

  @Test
  public void testRemoveFromRange() {
    final List<ComponentRenderInfo> components = prepareLoadedBinder();
    final int rangeTotal = (int) (RANGE_SIZE + (RANGE_RATIO * RANGE_SIZE));

    mRecyclerBinder.removeItemAt(rangeTotal);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    final TestComponentTreeHolder holder =
        mHoldersForComponents.get(components.get(rangeTotal).getComponent());
    assertThat(holder.mReleased).isTrue();

    final TestComponentTreeHolder holderMovedInRange =
        mHoldersForComponents.get(components.get(rangeTotal + 1).getComponent());
    assertThat(holderMovedInRange.isTreeValid()).isTrue();
    assertThat(holderMovedInRange.mLayoutSyncCalled).isFalse();
    assertThat(holderMovedInRange.mLayoutAsyncCalled).isTrue();
    assertThat(holderMovedInRange.mDidAcquireStateHandler).isFalse();
  }

  @Test
  public void testRemoveRange() {
    final List<ComponentRenderInfo> components = prepareLoadedBinder();
    final int rangeTotal = (int) (RANGE_SIZE + (RANGE_RATIO * RANGE_SIZE));

    mRecyclerBinder.removeRangeAt(0, RANGE_SIZE);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    // The elements that were removed have been released.
    for (int i = 0; i < RANGE_SIZE; i++) {
      final TestComponentTreeHolder holder =
          mHoldersForComponents.get(components.get(i).getComponent());
      assertThat(holder.mReleased).isTrue();
    }

    // The elements that are now in the range get their layout computed
    for (int i = rangeTotal + 1; i <= rangeTotal + RANGE_SIZE; i++) {
      final TestComponentTreeHolder holder =
          mHoldersForComponents.get(components.get(i).getComponent());
      assertThat(holder.isTreeValid()).isTrue();
      assertThat(holder.mLayoutSyncCalled).isFalse();
      assertThat(holder.mLayoutAsyncCalled).isTrue();
      assertThat(holder.mDidAcquireStateHandler).isFalse();
    }
  }

  @Test
  public void testUpdate() {
    final List<ComponentRenderInfo> components = prepareLoadedBinder();

    final TestComponentTreeHolder holder =
        mHoldersForComponents.get(components.get(0).getComponent());
    assertThat(holder.isTreeValid()).isTrue();
    holder.mTreeValid = false;
    assertThat(holder.isTreeValid()).isFalse();

    final ComponentRenderInfo newRenderInfo =
        create().component(mock(Component.class)).build();
    mRecyclerBinder.updateItemAt(0, newRenderInfo);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(newRenderInfo).isEqualTo(holder.getRenderInfo());
    assertThat(holder.isTreeValid()).isTrue();
  }

  @Test
  public void testUpdateRange() {
    final List<ComponentRenderInfo> components = prepareLoadedBinder();

    for (int i = 0; i < RANGE_SIZE; i++) {
      final TestComponentTreeHolder holder =
          mHoldersForComponents.get(components.get(i).getComponent());
      assertThat(holder.isTreeValid()).isTrue();
      holder.mTreeValid = false;
      assertThat(holder.isTreeValid()).isFalse();
    }

    final List<RenderInfo> newInfos = new ArrayList<>();
    for (int i = 0; i < RANGE_SIZE; i++) {
      newInfos.add(
          ComponentRenderInfo.create().component(mock(Component.class)).build());
    }
    mRecyclerBinder.updateRangeAt(0, newInfos);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    for (int i = 0; i < RANGE_SIZE; i++) {
      final TestComponentTreeHolder holder =
          mHoldersForComponents.get(components.get(i).getComponent());
      assertThat(newInfos.get(i)).isEqualTo(holder.getRenderInfo());
      assertThat(holder.isTreeValid()).isTrue();
    }
  }

  @Test
  public void testInsertMixedContentWithSingleViewCreator() {
    List<Integer> viewItems = Arrays.asList(3, 6, 7, 11);
    prepareMixedLoadedBinder(
        30,
        new HashSet<>(viewItems),
        new ViewCreatorProvider() {
          @Override
          public ViewCreator get() {
            return VIEW_CREATOR_1;
          }
        });

    assertThat(mRecyclerBinder.mRenderInfoViewCreatorController.mViewTypeToViewCreator.size())
        .isEqualTo(1);
    assertThat(mRecyclerBinder.mRenderInfoViewCreatorController.mViewCreatorToViewType.size())
        .isEqualTo(1);

    ViewCreator obtainedViewCreator =
        mRecyclerBinder.mRenderInfoViewCreatorController.mViewCreatorToViewType.keyAt(0);
    assertThat(obtainedViewCreator).isEqualTo(VIEW_CREATOR_1);
    assertThat(
            mRecyclerBinder.mRenderInfoViewCreatorController.mViewTypeToViewCreator.indexOfValue(
                obtainedViewCreator))
        .isGreaterThanOrEqualTo(0);
  }

  @Test
  public void testInsertMixedContentWithMultiViewCreator() {
    final List<Integer> viewItems = Arrays.asList(3, 6, 7, 11);
    prepareMixedLoadedBinder(
        30,
        new HashSet<>(viewItems),
        new ViewCreatorProvider() {
          @Override
          public ViewCreator get() {
            // Different ViewCreator instances for each view item.
            return new ViewCreator() {
              @Override
              public View createView(Context c, ViewGroup parent) {
                return mock(View.class);
              }
            };
          }
        });

    assertThat(mRecyclerBinder.mRenderInfoViewCreatorController.mViewTypeToViewCreator.size())
        .isEqualTo(4);
    assertThat(mRecyclerBinder.mRenderInfoViewCreatorController.mViewCreatorToViewType.size())
        .isEqualTo(4);

    for (int i = 0,
            size = mRecyclerBinder.mRenderInfoViewCreatorController.mViewCreatorToViewType.size();
        i < size;
        i++) {
      final ViewCreator obtainedViewCreator =
          mRecyclerBinder.mRenderInfoViewCreatorController.mViewCreatorToViewType.keyAt(i);
      assertThat(
              mRecyclerBinder.mRenderInfoViewCreatorController.mViewTypeToViewCreator.indexOfValue(
                  obtainedViewCreator))
          .isGreaterThanOrEqualTo(0);
    }
  }

  @Test
  public void testInsertMixedContentFollowedByDelete() {
    mRecyclerBinder.insertItemAt(
        0, ComponentRenderInfo.create().component(mock(Component.class)).build());

    mRecyclerBinder.insertItemAt(
        1,
        ViewRenderInfo.create()
            .viewBinder(new SimpleViewBinder())
            .viewCreator(VIEW_CREATOR_1)
            .build());
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(mRecyclerBinder.mRenderInfoViewCreatorController.mViewCreatorToViewType.size())
        .isEqualTo(1);
    assertThat(mRecyclerBinder.mRenderInfoViewCreatorController.mViewTypeToViewCreator.size())
        .isEqualTo(1);

    mRecyclerBinder.insertItemAt(
        2,
        ViewRenderInfo.create()
            .viewBinder(new SimpleViewBinder())
            .viewCreator(VIEW_CREATOR_2)
            .build());
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(mRecyclerBinder.mRenderInfoViewCreatorController.mViewCreatorToViewType.size())
        .isEqualTo(2);
    assertThat(mRecyclerBinder.mRenderInfoViewCreatorController.mViewTypeToViewCreator.size())
        .isEqualTo(2);

    mRecyclerBinder.removeItemAt(1);
    mRecyclerBinder.removeItemAt(1);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(mRecyclerBinder.mRenderInfoViewCreatorController.mViewCreatorToViewType.size())
        .isEqualTo(2);
    assertThat(mRecyclerBinder.mRenderInfoViewCreatorController.mViewTypeToViewCreator.size())
        .isEqualTo(2);
  }

  @Test
  public void testInsertMixedContentFollowedByUpdate() {
    mRecyclerBinder.insertItemAt(
        0, ComponentRenderInfo.create().component(mock(Component.class)).build());
    mRecyclerBinder.insertItemAt(
        1,
        ViewRenderInfo.create()
            .viewBinder(new SimpleViewBinder())
            .viewCreator(VIEW_CREATOR_1)
            .build());
    mRecyclerBinder.insertItemAt(
        2,
        ViewRenderInfo.create()
            .viewBinder(new SimpleViewBinder())
            .viewCreator(VIEW_CREATOR_2)
            .build());
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(mRecyclerBinder.mRenderInfoViewCreatorController.mViewCreatorToViewType.size())
        .isEqualTo(2);
    assertThat(mRecyclerBinder.mRenderInfoViewCreatorController.mViewTypeToViewCreator.size())
        .isEqualTo(2);

    mRecyclerBinder.updateItemAt(
        1,
        ViewRenderInfo.create()
            .viewBinder(new SimpleViewBinder())
            .viewCreator(VIEW_CREATOR_2)
            .build());
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(mRecyclerBinder.mRenderInfoViewCreatorController.mViewCreatorToViewType.size())
        .isEqualTo(2);
    assertThat(mRecyclerBinder.mRenderInfoViewCreatorController.mViewTypeToViewCreator.size())
        .isEqualTo(2);

    mRecyclerBinder.updateItemAt(
        2,
        ViewRenderInfo.create()
            .viewCreator(VIEW_CREATOR_3)
            .viewBinder(new SimpleViewBinder())
            .build());
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(mRecyclerBinder.mRenderInfoViewCreatorController.mViewCreatorToViewType.size())
        .isEqualTo(3);
    assertThat(mRecyclerBinder.mRenderInfoViewCreatorController.mViewTypeToViewCreator.size())
        .isEqualTo(3);
  }

  @Test
  public void testCustomViewTypeEnabledViewTypeProvided() {
    final RecyclerBinder recyclerBinder =
        mRecyclerBinderBuilder.enableCustomViewType(5).build(mComponentContext);

    recyclerBinder.insertItemAt(
        0, ComponentRenderInfo.create().component(mock(Component.class)).build());
    recyclerBinder.insertItemAt(
        1,
        ViewRenderInfo.create()
            .viewBinder(new SimpleViewBinder())
            .viewCreator(VIEW_CREATOR_1)
            .customViewType(10)
            .build());
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(recyclerBinder.mRenderInfoViewCreatorController.mViewCreatorToViewType.size())
        .isEqualTo(1);
  }

  @Test(expected = IllegalStateException.class)
  public void testCustomViewTypeEnabledViewTypeNotProvided() {
    final RecyclerBinder recyclerBinder =
        mRecyclerBinderBuilder.enableCustomViewType(4).build(mComponentContext);

    recyclerBinder.insertItemAt(
        0, ComponentRenderInfo.create().component(mock(Component.class)).build());
    recyclerBinder.insertItemAt(
        1,
        ViewRenderInfo.create()
            .viewBinder(new SimpleViewBinder())
            .viewCreator(VIEW_CREATOR_1)
            .build());
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
  }

  @Test(expected = IllegalStateException.class)
  public void testCustomViewTypeNotEnabledViewTypeProvided() {
    final RecyclerBinder recyclerBinder = mRecyclerBinderBuilder.build(mComponentContext);

    recyclerBinder.insertItemAt(
        0, ComponentRenderInfo.create().component(mock(Component.class)).build());
    recyclerBinder.insertItemAt(
        1,
        ViewRenderInfo.create()
            .viewBinder(new SimpleViewBinder())
            .viewCreator(VIEW_CREATOR_1)
            .customViewType(10)
            .build());
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
  }

  @Test(expected = IllegalStateException.class)
  public void testCustomViewTypeEnabledComponentViewTypeSameAsCustomViewType() {
    final RecyclerBinder recyclerBinder =
        mRecyclerBinderBuilder.enableCustomViewType(2).build(mComponentContext);

    recyclerBinder.insertItemAt(
        0, ComponentRenderInfo.create().component(mock(Component.class)).build());
    recyclerBinder.insertItemAt(
        1,
        ViewRenderInfo.create()
            .viewBinder(new SimpleViewBinder())
            .viewCreator(VIEW_CREATOR_1)
            .customViewType(2)
            .build());
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
  }

  @Test
  public void testViewBinderBindAndUnbind() {
    final View view = mock(View.class);
    final RecyclerView recyclerView = new RecyclerView(mComponentContext);
    ViewBinder viewBinder = mock(ViewBinder.class);
    final ViewCreator<View> viewCreator =
        new ViewCreator<View>() {
          @Override
          public View createView(Context c, ViewGroup parent) {
            return view;
          }
        };

    mRecyclerBinder.insertItemAt(
        0, ViewRenderInfo.create().viewBinder(viewBinder).viewCreator(viewCreator).build());
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    mRecyclerBinder.mount(recyclerView);

    final ViewHolder vh =
        recyclerView
            .getAdapter()
            .onCreateViewHolder(
                new FrameLayout(mComponentContext),
                RenderInfoViewCreatorController.DEFAULT_COMPONENT_VIEW_TYPE + 1);

    recyclerView.getAdapter().onBindViewHolder(vh, 0);
    verify(viewBinder).bind(view);
    verify(viewBinder, never()).unbind(view);

    recyclerView.getAdapter().onViewRecycled(vh);
    verify(viewBinder, times(1)).bind(view);
    verify(viewBinder).unbind(view);
  }

  @Test
  public void testGetItemCount() {
    for (int i = 0; i < 100; i++) {
      assertThat(mRecyclerBinder.getItemCount()).isEqualTo(i);
      mRecyclerBinder.insertItemAt(
          i,
          create().component(mock(Component.class)).build());
    }
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
  }

  @Test
  public void testAllComponentsRangeInitialized() {
    prepareLoadedBinder();
    assertThat(mRecyclerBinder.getRangeCalculationResult()).isNotNull();
  }

  @Test
  public void testMixedContentFirstItemIsViewRangeInitialized() {
    prepareMixedLoadedBinder(
        3,
        new HashSet<>(Arrays.asList(0)),
        new ViewCreatorProvider() {
          @Override
          public ViewCreator get() {
            return VIEW_CREATOR_1;
          }
        });
    assertThat(mRecyclerBinder.getRangeCalculationResult()).isNotNull();
  }

  @Test
  public void testAllViewsInsertItemRangeInitialized() {
    prepareMixedLoadedBinder(
        3,
        new HashSet<>(Arrays.asList(0, 1, 2)),
        new ViewCreatorProvider() {
          @Override
          public ViewCreator get() {
            return VIEW_CREATOR_1;
          }
        });
    assertThat(mRecyclerBinder.getRangeCalculationResult()).isNull();

    mRecyclerBinder.insertItemAt(
        1, ComponentRenderInfo.create().component(mock(Component.class)).build());
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(mRecyclerBinder.getRangeCalculationResult()).isNotNull();
  }

  @Test
  public void testAllViewsUpdateItemRangeInitialized() {
    prepareMixedLoadedBinder(
        3,
        new HashSet<>(Arrays.asList(0, 1, 2)),
        new ViewCreatorProvider() {
          @Override
          public ViewCreator get() {
            return VIEW_CREATOR_1;
          }
        });
    assertThat(mRecyclerBinder.getRangeCalculationResult()).isNull();

    mRecyclerBinder.updateItemAt(
        1, ComponentRenderInfo.create().component(mock(Component.class)).build());
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(mRecyclerBinder.getRangeCalculationResult()).isNotNull();
  }

  @Test
  public void testAllViewsInsertMultiItemRangeNotInitialized() {
    prepareMixedLoadedBinder(
        3,
        new HashSet<>(Arrays.asList(0, 1, 2)),
        new ViewCreatorProvider() {
          @Override
          public ViewCreator get() {
            return VIEW_CREATOR_1;
          }
        });
    assertThat(mRecyclerBinder.getRangeCalculationResult()).isNull();

    ArrayList<RenderInfo> renderInfos = new ArrayList<>();
    renderInfos.add(ComponentRenderInfo.create().component(mock(Component.class)).build());
    renderInfos.add(ComponentRenderInfo.create().component(mock(Component.class)).build());
    mRecyclerBinder.insertRangeAt(0, renderInfos);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(mRecyclerBinder.getRangeCalculationResult()).isNotNull();
  }

  @Test
  public void testAllViewsUpdateMultiItemRangeNotInitialized() {
    prepareMixedLoadedBinder(
        3,
        new HashSet<>(Arrays.asList(0, 1, 2)),
        new ViewCreatorProvider() {
          @Override
          public ViewCreator get() {
            return VIEW_CREATOR_1;
          }
        });
    assertThat(mRecyclerBinder.getRangeCalculationResult()).isNull();

    ArrayList<RenderInfo> renderInfos = new ArrayList<>();
    renderInfos.add(ComponentRenderInfo.create().component(mock(Component.class)).build());
    renderInfos.add(ComponentRenderInfo.create().component(mock(Component.class)).build());
    mRecyclerBinder.updateRangeAt(0, renderInfos);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(mRecyclerBinder.getRangeCalculationResult()).isNotNull();
  }

  @Test
  public void testAllViewsInsertComponentMeasureAfterRangeInitialized() {
    for (int i = 0; i < 3; i++) {
      mRecyclerBinder.insertItemAt(
          i,
          ViewRenderInfo.create()
              .viewBinder(new SimpleViewBinder())
              .viewCreator(VIEW_CREATOR_1)
              .build());
    }
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(mRecyclerBinder.getRangeCalculationResult()).isNull();

    mRecyclerBinder.insertItemAt(
        1, ComponentRenderInfo.create().component(mock(Component.class)).build());
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(mRecyclerBinder.getRangeCalculationResult()).isNull();

    Size size = new Size();
    int widthSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);
    int heightSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);

    mRecyclerBinder.measure(size, widthSpec, heightSpec, null);
    assertThat(mRecyclerBinder.getRangeCalculationResult()).isNotNull();
  }

  private List<RenderInfo> prepareMixedLoadedBinder(
      int adapterSize, Set<Integer> viewItems, ViewCreatorProvider viewCreatorProvider) {
    final List<RenderInfo> renderInfos = new ArrayList<>();

    for (int i = 0; i < adapterSize; i++) {
      final RenderInfo renderInfo;
      if (viewItems.contains(i)) {
        renderInfo =
            ViewRenderInfo.create()
                .viewBinder(new SimpleViewBinder())
                .viewCreator(viewCreatorProvider.get())
                .build();
      } else {
        renderInfo = ComponentRenderInfo.create().component(mock(Component.class)).build();
      }
      renderInfos.add(renderInfo);
      mRecyclerBinder.insertItemAt(i, renderInfos.get(i));
    }
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    Size size = new Size();
    int widthSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);
    int heightSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);

    mRecyclerBinder.measure(size, widthSpec, heightSpec, null);

    return renderInfos;
  }

  @Test
  public void testCircularRecyclerItemCount() {
    List<RenderInfo> renderInfos = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      renderInfos.add(ComponentRenderInfo.create().component(mock(Component.class)).build());
    }

    mCircularRecyclerBinder.insertRangeAt(0, renderInfos);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    Assert.assertEquals(Integer.MAX_VALUE, mCircularRecyclerBinder.getItemCount());
  }

  @Test
  public void testCircularRecyclerItemCountWithOneItem() {
    mCircularRecyclerBinder.insertItemAt(
        0, ComponentRenderInfo.create().component(mock(Component.class)).build());
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    Assert.assertEquals(Integer.MAX_VALUE, mCircularRecyclerBinder.getItemCount());
  }

  @Test
  public void testCircularRecyclerItemFirstVisible() {
    RecyclerView recyclerView = mock(RecyclerView.class);
    when(mCircularLayoutInfo.getLayoutManager()).thenReturn(mock(RecyclerView.LayoutManager.class));

    mCircularRecyclerBinder.mount(recyclerView);

    verify(recyclerView).scrollToPosition(Integer.MAX_VALUE / 2);
  }

  @Test
  public void testCircularRecyclerInitRange() {
    final List<ComponentRenderInfo> components = prepareLoadedBinder(mCircularRecyclerBinder, 10);
    TestComponentTreeHolder holder = mHoldersForComponents.get(components.get(0).getComponent());
    assertThat(holder.isTreeValid()).isTrue();
    assertThat(holder.mLayoutSyncCalled).isTrue();
  }

  @Test
  public void testCircularRecyclerMeasure() {
    final List<ComponentRenderInfo> components = prepareLoadedBinder(mCircularRecyclerBinder, 10);

    int widthSpec = makeSizeSpec(100, EXACTLY);
    int heightSpec = makeSizeSpec(200, EXACTLY);

    mCircularRecyclerBinder.measure(new Size(), widthSpec, heightSpec, null);

    TestComponentTreeHolder holder = mHoldersForComponents.get(components.get(0).getComponent());
    assertThat(holder.isTreeValid()).isTrue();
    assertThat(holder.mLayoutSyncCalled).isTrue();

    for (int i = 1; i < 10; i++) {
      holder = mHoldersForComponents.get(components.get(i).getComponent());
      assertThat(holder.isTreeValid()).isTrue();
      assertThat(holder.mLayoutAsyncCalled).isTrue();
    }
  }

  @Test
  public void testCircularRecyclerMeasureExact() {
    RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .rangeRatio(RANGE_RATIO)
            .layoutInfo(
                new LinearLayoutInfo(mComponentContext, OrientationHelper.HORIZONTAL, false))
            .build(mComponentContext);

    final List<RenderInfo> components = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      final Component component =
          new InlineLayoutSpec() {
            @Override
            protected Component onCreateLayout(ComponentContext c) {
              return TestDrawableComponent.create(c).widthPx(100).heightPx(100).build();
            }
          };

      components.add(ComponentRenderInfo.create().component(component).build());
    }
    recyclerBinder.insertRangeAt(0, components);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    RecyclerView rv = new RecyclerView(RuntimeEnvironment.application);
    recyclerBinder.mount(rv);
    rv.measure(makeSizeSpec(200, EXACTLY), makeSizeSpec(200, EXACTLY));
    recyclerBinder.setSize(200, 200);
    rv.layout(0, 0, 200, 200);

    assertThat(rv.getChildCount()).isGreaterThan(0);
    for (int i = 0; i < rv.getChildCount(); i++) {
      LithoView lv = (LithoView) rv.getChildAt(i);
      assertThat(lv.getMeasuredWidth()).isEqualTo(100);
      assertThat(lv.getMeasuredHeight()).isEqualTo(200);
    }
  }

  @Test
  public void testCircularRecyclerMeasureAtMost() {
    RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .rangeRatio(RANGE_RATIO)
            .layoutInfo(
                new LinearLayoutInfo(mComponentContext, OrientationHelper.HORIZONTAL, false) {
                  @Override
                  public int getChildWidthSpec(int widthSpec, RenderInfo renderInfo) {
                    return SizeSpec.makeSizeSpec(SizeSpec.getSize(widthSpec), AT_MOST);
                  }
                })
            .build(mComponentContext);

    final List<RenderInfo> components = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      final Component component =
          new InlineLayoutSpec() {
            @Override
            protected Component onCreateLayout(ComponentContext c) {
              return TestDrawableComponent.create(c).widthPercent(50).heightPercent(25).build();
            }
          };

      components.add(ComponentRenderInfo.create().component(component).build());
    }
    recyclerBinder.insertRangeAt(0, components);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    RecyclerView rv = new RecyclerView(RuntimeEnvironment.application);
    recyclerBinder.mount(rv);
    rv.measure(makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));
    recyclerBinder.setSize(100, 100);
    rv.layout(0, 0, 100, 100);

    assertThat(rv.getChildCount()).isGreaterThan(0);
    for (int i = 0; i < rv.getChildCount(); i++) {
      LithoView lv = (LithoView) rv.getChildAt(i);
      assertThat(lv.getMeasuredWidth()).isEqualTo(50);
      assertThat(lv.getMeasuredHeight()).isEqualTo(100);
    }
  }

  @Test
  public void testUpdateItemAtDoesNotNotifyItemChangedExceptWhenUpdatingViews() {
    final RecyclerView.Adapter adapter = mock(RecyclerView.Adapter.class);
    final RecyclerBinder recyclerBinder = createRecyclerBinderWithMockAdapter(adapter);

    final List<RenderInfo> components = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      final Component component =
          new InlineLayoutSpec() {
            @Override
            protected Component onCreateLayout(ComponentContext c) {
              return TestDrawableComponent.create(c).widthPx(100).heightPx(100).build();
            }
          };

      components.add(ComponentRenderInfo.create().component(component).build());
    }
    recyclerBinder.insertRangeAt(0, components);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    RecyclerView rv = mock(RecyclerView.class);
    recyclerBinder.mount(rv);

    recyclerBinder.updateItemAt(
        0, TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build());
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    verify(adapter, never()).notifyItemChanged(anyInt());
    verify(adapter, never()).notifyItemRangeChanged(anyInt(), anyInt());

    recyclerBinder.updateItemAt(
        0,
        ViewRenderInfo.create()
            .viewCreator(VIEW_CREATOR_1)
            .viewBinder(new SimpleViewBinder())
            .build());
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
    verify(adapter, times(1)).notifyItemChanged(0);
  }

  @Test
  public void testUpdateRangeAtDoesNotNotifyItemChangedExceptWhenUpdatingViews() {
    final RecyclerView.Adapter adapter = mock(RecyclerView.Adapter.class);
    final RecyclerBinder recyclerBinder = createRecyclerBinderWithMockAdapter(adapter);

    final int NUM_ITEMS = 10;
    final List<RenderInfo> components = new ArrayList<>();
    for (int i = 0; i < NUM_ITEMS; i++) {
      final Component component =
          new InlineLayoutSpec() {
            @Override
            protected Component onCreateLayout(ComponentContext c) {
              return TestDrawableComponent.create(c).widthPx(100).heightPx(100).build();
            }
          };

      components.add(ComponentRenderInfo.create().component(component).build());
    }
    recyclerBinder.insertRangeAt(0, components);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    RecyclerView rv = mock(RecyclerView.class);
    recyclerBinder.mount(rv);

    final List<RenderInfo> newComponents = new ArrayList<>();
    for (int i = 0; i < NUM_ITEMS; i++) {
      final Component component =
          new InlineLayoutSpec() {
            @Override
            protected Component onCreateLayout(ComponentContext c) {
              return TestDrawableComponent.create(c).widthPx(100).heightPx(100).build();
            }
          };
      newComponents.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.updateRangeAt(0, newComponents);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    verify(adapter, never()).notifyItemChanged(anyInt());
    verify(adapter, never()).notifyItemRangeChanged(anyInt(), anyInt());

    final List<RenderInfo> newViews = new ArrayList<>();
    for (int i = 0; i < NUM_ITEMS; i++) {
      newViews.add(
          ViewRenderInfo.create()
              .viewCreator(VIEW_CREATOR_1)
              .viewBinder(new SimpleViewBinder())
              .build());
    }

    recyclerBinder.updateRangeAt(0, newViews);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    for (int i = 0; i < NUM_ITEMS; i++) {
      verify(adapter, times(1)).notifyItemChanged(i);
    }
  }

  @Test
  public void testOnNewWorkingRange() {
    final List<ComponentRenderInfo> components = prepareLoadedBinder();
    final int firstVisibleIndex = 40;
    final int lastVisibleIndex = 82;
    final int rangeSize = Math.max(RANGE_SIZE, lastVisibleIndex - firstVisibleIndex);
    final int layoutRangeSize = (int) (rangeSize * RANGE_RATIO);
    final int rangeTotal = rangeSize + layoutRangeSize;

    mRecyclerBinder.onNewWorkingRange(
        firstVisibleIndex, lastVisibleIndex, firstVisibleIndex + 1, lastVisibleIndex - 1);

    TestComponentTreeHolder componentTreeHolder;
    for (int i = 0; i < components.size(); i++) {
      componentTreeHolder = mHoldersForComponents.get(components.get(i).getComponent());
      assertThat(componentTreeHolder).isNotNull();

      if (i >= firstVisibleIndex - layoutRangeSize && i <= firstVisibleIndex + rangeTotal) {
        assertThat(componentTreeHolder.mCheckWorkingRangeCalled).isTrue();
      } else {
        assertThat(componentTreeHolder.mCheckWorkingRangeCalled).isFalse();
      }
    }
  }

  @Test
  public void testInsertAsync() {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder().rangeRatio(RANGE_RATIO).build(mComponentContext);
    final Component component =
        TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
    final ComponentRenderInfo renderInfo =
        ComponentRenderInfo.create().component(component).build();

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);
    recyclerBinder.insertItemAtAsync(0, renderInfo);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(recyclerBinder.getItemCount()).isEqualTo(0);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNull();

    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(1);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNotNull();

    final ComponentTreeHolder holder = recyclerBinder.getComponentTreeHolderAt(0);
    assertThat(holder.getRenderInfo().getComponent()).isEqualTo(component);
    assertThat(holder.hasCompletedLatestLayout()).isTrue();
    assertThat(holder.isTreeValid()).isTrue();
  }

  @Test
  public void testMultipleInsertAsyncs() {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder().rangeRatio(RANGE_RATIO).build(mComponentContext);
    final Component component =
        TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
    final ComponentRenderInfo renderInfo =
        ComponentRenderInfo.create().component(component).build();

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);
    recyclerBinder.insertItemAtAsync(0, renderInfo);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(recyclerBinder.getItemCount()).isEqualTo(0);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNull();

    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(1);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNotNull();

    final Component component2 =
        TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
    final ComponentRenderInfo renderInfo2 =
        ComponentRenderInfo.create().component(component2).build();

    recyclerBinder.insertItemAtAsync(0, renderInfo2);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(recyclerBinder.getItemCount()).isEqualTo(1);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNotNull();

    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(2);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNotNull();

    final ComponentTreeHolder holder = recyclerBinder.getComponentTreeHolderAt(0);
    assertThat(holder.getRenderInfo().getComponent()).isEqualTo(component);
    assertThat(holder.hasCompletedLatestLayout()).isTrue();
    assertThat(holder.isTreeValid()).isTrue();

    final ComponentTreeHolder holder2 = recyclerBinder.getComponentTreeHolderAt(1);
    assertThat(holder2.getRenderInfo().getComponent()).isEqualTo(component2);
    assertThat(holder2.hasCompletedLatestLayout()).isTrue();
    assertThat(holder2.isTreeValid()).isTrue();
  }

  @Test
  public void testInsertAsyncBeforeInitialMeasure() {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder().rangeRatio(RANGE_RATIO).build(mComponentContext);
    final Component component =
        TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
    final ComponentRenderInfo renderInfo =
        ComponentRenderInfo.create().component(component).build();

    recyclerBinder.insertItemAtAsync(0, renderInfo);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(recyclerBinder.getItemCount()).isEqualTo(0);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNull();

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);

    assertThat(recyclerBinder.getItemCount()).isEqualTo(1);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNotNull();

    final ComponentTreeHolder holder = recyclerBinder.getComponentTreeHolderAt(0);
    assertThat(holder.getRenderInfo().getComponent()).isEqualTo(component);
    assertThat(holder.hasCompletedLatestLayout()).isTrue();
    assertThat(holder.isTreeValid()).isTrue();
  }

  @Test
  public void testInsertAsyncAfterInitialMeasureButNeedsRemeasure() {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .rangeRatio(RANGE_RATIO)
            .layoutInfo(
                new LinearLayoutInfo(mComponentContext, OrientationHelper.HORIZONTAL, false))
            .build(mComponentContext);
    final Component component =
        TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
    final ComponentRenderInfo renderInfo =
        ComponentRenderInfo.create().component(component).build();
    final RecyclerView recyclerView = mock(RecyclerView.class);

    recyclerBinder.mount(recyclerView);
    recyclerBinder.measure(
        new Size(),
        makeSizeSpec(1000, EXACTLY),
        makeSizeSpec(0, UNSPECIFIED),
        mock(EventHandler.class));

    recyclerBinder.insertItemAtAsync(0, renderInfo);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(recyclerBinder.getItemCount()).isEqualTo(0);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNull();

    verify(recyclerView).postOnAnimation(recyclerBinder.mRemeasureRunnable);

    // Manually invoke the remeasure
    recyclerBinder.measure(
        new Size(),
        makeSizeSpec(1000, EXACTLY),
        makeSizeSpec(0, UNSPECIFIED),
        mock(EventHandler.class));

    assertThat(recyclerBinder.getItemCount()).isEqualTo(1);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNotNull();

    final ComponentTreeHolder holder = recyclerBinder.getComponentTreeHolderAt(0);
    assertThat(holder.getRenderInfo().getComponent()).isEqualTo(component);
    assertThat(holder.hasCompletedLatestLayout()).isTrue();
    assertThat(holder.isTreeValid()).isTrue();
  }

  @Test
  public void testInsertAsyncWithSizeChangeBeforeCompletion() {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder().rangeRatio(RANGE_RATIO).build(mComponentContext);
    final Component component =
        TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
    final ComponentRenderInfo renderInfo =
        ComponentRenderInfo.create().component(component).build();

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);
    recyclerBinder.insertItemAtAsync(0, renderInfo);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    recyclerBinder.measure(
        new Size(), makeSizeSpec(500, EXACTLY), makeSizeSpec(1000, EXACTLY), null);

    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(1);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNotNull();

    final ComponentTreeHolder holder = recyclerBinder.getComponentTreeHolderAt(0);
    assertThat(holder.getRenderInfo().getComponent()).isEqualTo(component);
    assertThat(holder.hasCompletedLatestLayout()).isTrue();
    assertThat(holder.isTreeValid()).isTrue();
    assertHasCompatibleLayout(
        recyclerBinder, 0, makeSizeSpec(500, EXACTLY), makeSizeSpec(0, UNSPECIFIED));
  }

  @Test
  public void testInsertAsyncWithSizeChangeBeforeBatchClosed() {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder().rangeRatio(RANGE_RATIO).build(mComponentContext);
    final Component component =
        TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
    final ComponentRenderInfo renderInfo =
        ComponentRenderInfo.create().component(component).build();

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);
    recyclerBinder.insertItemAtAsync(0, renderInfo);

    recyclerBinder.measure(
        new Size(), makeSizeSpec(500, EXACTLY), makeSizeSpec(1000, EXACTLY), null);

    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(1);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNotNull();

    final ComponentTreeHolder holder = recyclerBinder.getComponentTreeHolderAt(0);
    assertThat(holder.getRenderInfo().getComponent()).isEqualTo(component);
    assertThat(holder.hasCompletedLatestLayout()).isTrue();
    assertThat(holder.isTreeValid()).isTrue();
    assertHasCompatibleLayout(
        recyclerBinder, 0, makeSizeSpec(500, EXACTLY), makeSizeSpec(0, UNSPECIFIED));
  }

  @Test
  public void testInsertRangeAsync() {
    final int NUM_TO_INSERT = 5;
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder().rangeRatio(RANGE_RATIO).build(mComponentContext);
    final ArrayList<Component> components = new ArrayList<>();
    final ArrayList<RenderInfo> renderInfos = new ArrayList<>();
    for (int i = 0; i < NUM_TO_INSERT; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
      components.add(component);
      renderInfos.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);
    recyclerBinder.insertRangeAtAsync(0, renderInfos);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(recyclerBinder.getItemCount()).isEqualTo(0);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNull();

    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(NUM_TO_INSERT);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNotNull();

    for (int i = 0; i < NUM_TO_INSERT; i++) {
      final ComponentTreeHolder holder = recyclerBinder.getComponentTreeHolderAt(i);
      assertThat(holder.getRenderInfo().getComponent()).isEqualTo(components.get(i));
      assertThat(holder.hasCompletedLatestLayout()).isTrue();
      assertThat(holder.isTreeValid()).isTrue();
    }
  }

  @Test
  public void testInsertRangeAsyncBeforeInitialMeasure() {
    final int NUM_TO_INSERT = 5;
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder().rangeRatio(RANGE_RATIO).build(mComponentContext);
    final ArrayList<Component> components = new ArrayList<>();
    final ArrayList<RenderInfo> renderInfos = new ArrayList<>();
    for (int i = 0; i < NUM_TO_INSERT; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
      components.add(component);
      renderInfos.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.insertRangeAtAsync(0, renderInfos);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(recyclerBinder.getItemCount()).isEqualTo(0);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNull();

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);

    assertThat(recyclerBinder.getItemCount()).isEqualTo(NUM_TO_INSERT);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNotNull();

    for (int i = 0; i < NUM_TO_INSERT; i++) {
      final ComponentTreeHolder holder = recyclerBinder.getComponentTreeHolderAt(i);
      assertThat(holder.getRenderInfo().getComponent()).isEqualTo(components.get(i));
      assertThat(holder.hasCompletedLatestLayout()).isTrue();
      assertThat(holder.isTreeValid()).isTrue();
    }
  }

  @Test
  public void testInsertRangeAsyncBeforeInitialMeasureRangeIsLargerThanMeasure() {
    final int NUM_TO_INSERT = 5;
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder().rangeRatio(RANGE_RATIO).build(mComponentContext);
    final ArrayList<Component> components = new ArrayList<>();
    final ArrayList<RenderInfo> renderInfos = new ArrayList<>();
    for (int i = 0; i < NUM_TO_INSERT; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).widthPx(600).heightPx(600).build();
      components.add(component);
      renderInfos.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.insertRangeAtAsync(0, renderInfos);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(recyclerBinder.getItemCount()).isEqualTo(0);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNull();

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);

    assertThat(recyclerBinder.getItemCount()).isEqualTo(2);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNotNull();

    for (int i = 0; i < 2; i++) {
      final ComponentTreeHolder holder = recyclerBinder.getComponentTreeHolderAt(i);
      assertThat(holder.getRenderInfo().getComponent()).isEqualTo(components.get(i));
      assertThat(holder.hasCompletedLatestLayout()).isTrue();
      assertThat(holder.isTreeValid()).isTrue();
    }

    // compute one layout to ensure batching behavior remains
    mLayoutThreadShadowLooper.runOneTask();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(2);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNotNull();

    // finish computing all layouts - batch should now be applied
    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(NUM_TO_INSERT);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNotNull();

    for (int i = 0; i < NUM_TO_INSERT; i++) {
      final ComponentTreeHolder holder = recyclerBinder.getComponentTreeHolderAt(i);
      assertThat(holder.getRenderInfo().getComponent()).isEqualTo(components.get(i));
      assertThat(holder.hasCompletedLatestLayout()).isTrue();
      assertThat(holder.isTreeValid()).isTrue();
    }
  }

  @Test(expected = RuntimeException.class)
  public void testThrowsIfSyncInsertAfterAsyncInsert() {
    mRecyclerBinder.insertItemAtAsync(0, createTestComponentRenderInfo());
    mRecyclerBinder.insertItemAt(1, createTestComponentRenderInfo());
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
  }

  @Test(expected = RuntimeException.class)
  public void testThrowsIfSyncInsertAfterAsyncInsertRange() {
    final ArrayList<RenderInfo> firstInsert = new ArrayList<>();
    final ArrayList<RenderInfo> secondInsert = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      firstInsert.add(createTestComponentRenderInfo());
      secondInsert.add(createTestComponentRenderInfo());
    }

    mRecyclerBinder.insertRangeAtAsync(0, firstInsert);
    mRecyclerBinder.insertRangeAt(firstInsert.size(), secondInsert);
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
  }

  @Test
  public void testInsertsDispatchedInBatch() {
    final int NUM_TO_INSERT = 5;
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder().rangeRatio(RANGE_RATIO).build(mComponentContext);
    final ArrayList<Component> components = new ArrayList<>();
    final ArrayList<RenderInfo> renderInfos = new ArrayList<>();
    for (int i = 0; i < NUM_TO_INSERT; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
      components.add(component);
      renderInfos.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);
    recyclerBinder.insertRangeAtAsync(0, renderInfos);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(recyclerBinder.getItemCount()).isEqualTo(0);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNull();

    // complete first layout
    mLayoutThreadShadowLooper.runOneTask();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(0);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNull();

    // complete second layout
    mLayoutThreadShadowLooper.runOneTask();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(0);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNull();

    // complete the rest of the layouts
    for (int i = 2; i < NUM_TO_INSERT; i++) {
      mLayoutThreadShadowLooper.runToEndOfTasks();
    }

    assertThat(recyclerBinder.getItemCount()).isEqualTo(NUM_TO_INSERT);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNotNull();
  }

  @Test
  public void testRemoveAsync() {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder().rangeRatio(RANGE_RATIO).build(mComponentContext);
    final Component component =
        TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
    final ComponentRenderInfo renderInfo =
        ComponentRenderInfo.create().component(component).build();

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);
    recyclerBinder.insertItemAtAsync(0, renderInfo);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(1);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNotNull();

    recyclerBinder.removeItemAtAsync(0);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(0);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNotNull();
  }

  @Test
  public void testRemoveAsyncMixedWithInserts() {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder().rangeRatio(RANGE_RATIO).build(mComponentContext);
    final ArrayList<Component> components = new ArrayList<>();
    final ArrayList<RenderInfo> renderInfos = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
      components.add(component);
      renderInfos.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);
    recyclerBinder.insertItemAtAsync(0, renderInfos.get(0));
    recyclerBinder.insertItemAtAsync(1, renderInfos.get(1));
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(2);

    recyclerBinder.insertItemAtAsync(0, renderInfos.get(2));
    recyclerBinder.removeItemAtAsync(0);
    recyclerBinder.insertItemAtAsync(2, renderInfos.get(3));
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(3);
    assertComponentAtEquals(recyclerBinder, 0, components.get(0));
    assertComponentAtEquals(recyclerBinder, 1, components.get(1));
    assertComponentAtEquals(recyclerBinder, 2, components.get(3));
  }

  @Test
  public void testMoveAsync() {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder().rangeRatio(RANGE_RATIO).build(mComponentContext);
    final ArrayList<Component> components = new ArrayList<>();
    final ArrayList<RenderInfo> renderInfos = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
      components.add(component);
      renderInfos.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);
    recyclerBinder.insertRangeAtAsync(0, renderInfos);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(4);

    recyclerBinder.moveItemAsync(0, 2);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(4);
    assertComponentAtEquals(recyclerBinder, 0, components.get(1));
    assertComponentAtEquals(recyclerBinder, 1, components.get(2));
    assertComponentAtEquals(recyclerBinder, 2, components.get(0));
    assertComponentAtEquals(recyclerBinder, 3, components.get(3));
  }

  @Test
  public void testMoveAsyncMixedWithInserts() {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder().rangeRatio(RANGE_RATIO).build(mComponentContext);
    final ArrayList<Component> components = new ArrayList<>();
    final ArrayList<RenderInfo> renderInfos = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
      components.add(component);
      renderInfos.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);
    recyclerBinder.insertItemAtAsync(0, renderInfos.get(0));
    recyclerBinder.insertItemAtAsync(1, renderInfos.get(1));
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(2);

    recyclerBinder.insertItemAtAsync(0, renderInfos.get(2));
    recyclerBinder.moveItemAsync(0, 1);
    recyclerBinder.insertItemAtAsync(2, renderInfos.get(3));
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(4);
    assertComponentAtEquals(recyclerBinder, 0, components.get(0));
    assertComponentAtEquals(recyclerBinder, 1, components.get(2));
    assertComponentAtEquals(recyclerBinder, 2, components.get(3));
    assertComponentAtEquals(recyclerBinder, 3, components.get(1));
  }

  @Test
  public void testUpdateAsyncOnInsertedItem() {
    final RecyclerView.Adapter adapter = mock(RecyclerView.Adapter.class);
    final RecyclerBinder recyclerBinder = createRecyclerBinderWithMockAdapter(adapter);
    final Component component =
        TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
    final RenderInfo renderInfo = ComponentRenderInfo.create().component(component).build();

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);
    recyclerBinder.insertItemAtAsync(0, renderInfo);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    final ComponentRenderInfo newRenderInfo =
        ComponentRenderInfo.create()
            .component(
                TestDrawableComponent.create(mComponentContext).widthPx(50).heightPx(50).build())
            .build();
    recyclerBinder.updateItemAtAsync(0, newRenderInfo);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    verify(adapter, never()).notifyItemChanged(anyInt());

    final ComponentTreeHolder holder = recyclerBinder.getComponentTreeHolderAt(0);
    assertThat(holder.getRenderInfo()).isEqualTo(newRenderInfo);
    assertThat(holder.isTreeValid()).isTrue();

    mLayoutThreadShadowLooper.runToEndOfTasks();
  }

  @Test
  public void testUpdateAsyncOnNonInsertedItem() {
    final RecyclerView.Adapter adapter = mock(RecyclerView.Adapter.class);
    final RecyclerBinder recyclerBinder = createRecyclerBinderWithMockAdapter(adapter);
    final Component component =
        TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
    final RenderInfo renderInfo = ComponentRenderInfo.create().component(component).build();

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);
    recyclerBinder.insertItemAtAsync(0, renderInfo);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(recyclerBinder.getItemCount()).isEqualTo(0);

    final ComponentRenderInfo newRenderInfo =
        ComponentRenderInfo.create()
            .component(
                TestDrawableComponent.create(mComponentContext).widthPx(50).heightPx(50).build())
            .build();
    recyclerBinder.updateItemAtAsync(0, newRenderInfo);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(recyclerBinder.getItemCount()).isEqualTo(0);
    verify(adapter, never()).notifyItemChanged(anyInt());

    mLayoutThreadShadowLooper.runToEndOfTasks();

    final ComponentTreeHolder holder = recyclerBinder.getComponentTreeHolderAt(0);
    assertThat(holder.getRenderInfo()).isEqualTo(newRenderInfo);
    assertThat(holder.isTreeValid()).isTrue();
    assertThat(holder.hasCompletedLatestLayout()).isTrue();
  }

  @Test
  public void testUpdateAsyncMixedWithInserts() {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder().rangeRatio(RANGE_RATIO).build(mComponentContext);
    final ArrayList<Component> components = new ArrayList<>();
    final ArrayList<RenderInfo> renderInfos = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
      components.add(component);
      renderInfos.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);
    recyclerBinder.insertItemAtAsync(0, renderInfos.get(0));
    recyclerBinder.insertItemAtAsync(1, renderInfos.get(1));
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(2);

    final ComponentRenderInfo newRenderInfo0 =
        ComponentRenderInfo.create()
            .component(
                TestDrawableComponent.create(mComponentContext).widthPx(50).heightPx(50).build())
            .build();
    final ComponentRenderInfo newRenderInfo1 =
        ComponentRenderInfo.create()
            .component(
                TestDrawableComponent.create(mComponentContext).widthPx(50).heightPx(50).build())
            .build();

    recyclerBinder.insertItemAtAsync(0, renderInfos.get(2));
    recyclerBinder.updateItemAtAsync(0, newRenderInfo0);
    recyclerBinder.insertItemAtAsync(2, renderInfos.get(3));
    recyclerBinder.updateItemAtAsync(1, newRenderInfo1);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(4);
    assertComponentAtEquals(recyclerBinder, 0, newRenderInfo0.getComponent());
    assertComponentAtEquals(recyclerBinder, 1, newRenderInfo1.getComponent());
    assertComponentAtEquals(recyclerBinder, 2, components.get(3));
    assertComponentAtEquals(recyclerBinder, 3, components.get(1));
  }

  @Test
  public void testUpdateAsyncMixedWithOtherOperations() {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder().rangeRatio(RANGE_RATIO).build(mComponentContext);
    final ArrayList<Component> components = new ArrayList<>();
    final ArrayList<RenderInfo> renderInfos = new ArrayList<>();
    for (int i = 0; i < 7; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
      components.add(component);
      renderInfos.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);
    recyclerBinder.insertItemAtAsync(0, renderInfos.get(0));
    recyclerBinder.insertItemAtAsync(1, renderInfos.get(1));
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(2);

    final ComponentRenderInfo newRenderInfo0 =
        ComponentRenderInfo.create()
            .component(
                TestDrawableComponent.create(mComponentContext).widthPx(50).heightPx(50).build())
            .build();
    final ComponentRenderInfo newRenderInfo1 =
        ComponentRenderInfo.create()
            .component(
                TestDrawableComponent.create(mComponentContext).widthPx(50).heightPx(50).build())
            .build();
    final ComponentRenderInfo newRenderInfo2 =
        ComponentRenderInfo.create()
            .component(
                TestDrawableComponent.create(mComponentContext).widthPx(50).heightPx(50).build())
            .build();

    recyclerBinder.insertItemAtAsync(0, renderInfos.get(2));
    recyclerBinder.updateItemAtAsync(0, newRenderInfo0);
    recyclerBinder.removeItemAtAsync(2);
    recyclerBinder.insertItemAtAsync(1, renderInfos.get(3));
    recyclerBinder.moveItemAsync(1, 2);
    recyclerBinder.updateItemAtAsync(1, newRenderInfo1);
    recyclerBinder.insertRangeAtAsync(0, renderInfos.subList(4, renderInfos.size()));
    recyclerBinder.updateItemAtAsync(2, newRenderInfo2);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(6);
    assertComponentAtEquals(recyclerBinder, 0, components.get(4));
    assertComponentAtEquals(recyclerBinder, 1, components.get(5));
    assertComponentAtEquals(recyclerBinder, 2, newRenderInfo2.getComponent());
    assertComponentAtEquals(recyclerBinder, 3, newRenderInfo0.getComponent());
    assertComponentAtEquals(recyclerBinder, 4, newRenderInfo1.getComponent());
    assertComponentAtEquals(recyclerBinder, 5, components.get(3));
  }

  @Test
  public void testUpdateAsyncOnInsertedViewFromComponent() {
    final RecyclerView.Adapter adapter = mock(RecyclerView.Adapter.class);
    final RecyclerBinder recyclerBinder = createRecyclerBinderWithMockAdapter(adapter);
    final Component component =
        TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
    final RenderInfo renderInfo = ComponentRenderInfo.create().component(component).build();

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);
    recyclerBinder.insertItemAtAsync(0, renderInfo);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    final RenderInfo newRenderInfo =
        ViewRenderInfo.create()
            .viewBinder(new SimpleViewBinder())
            .viewCreator(VIEW_CREATOR_1)
            .build();
    recyclerBinder.updateItemAtAsync(0, newRenderInfo);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    verify(adapter).notifyItemChanged(0);

    final ComponentTreeHolder holder = recyclerBinder.getComponentTreeHolderAt(0);
    assertThat(holder.getRenderInfo()).isEqualTo(newRenderInfo);
    mLayoutThreadShadowLooper.runToEndOfTasks();
  }

  @Test
  public void testUpdateAsyncOnInsertedViewToComponent() {
    final RecyclerView.Adapter adapter = mock(RecyclerView.Adapter.class);
    final RecyclerBinder recyclerBinder = createRecyclerBinderWithMockAdapter(adapter);
    final ViewRenderInfo renderInfo =
        ViewRenderInfo.create()
            .viewBinder(new SimpleViewBinder())
            .viewCreator(VIEW_CREATOR_1)
            .build();

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);
    recyclerBinder.insertItemAtAsync(0, renderInfo);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    final ComponentRenderInfo newRenderInfo =
        ComponentRenderInfo.create()
            .component(
                TestDrawableComponent.create(mComponentContext).widthPx(50).heightPx(50).build())
            .build();
    recyclerBinder.updateItemAtAsync(0, newRenderInfo);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    verify(adapter).notifyItemChanged(0);

    final ComponentTreeHolder holder = recyclerBinder.getComponentTreeHolderAt(0);
    assertThat(holder.getRenderInfo()).isEqualTo(newRenderInfo);
    assertThat(holder.isTreeValid()).isTrue();

    mLayoutThreadShadowLooper.runToEndOfTasks();
  }

  @Test
  public void testUpdateAsyncOnNonInsertedView() {
    final RecyclerView.Adapter adapter = mock(RecyclerView.Adapter.class);
    final RecyclerBinder recyclerBinder = createRecyclerBinderWithMockAdapter(adapter);
    final Component component =
        TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
    final RenderInfo renderInfo = ComponentRenderInfo.create().component(component).build();

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);
    recyclerBinder.insertItemAtAsync(0, renderInfo);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    final RenderInfo newRenderInfo =
        ViewRenderInfo.create()
            .viewBinder(new SimpleViewBinder())
            .viewCreator(VIEW_CREATOR_1)
            .build();
    recyclerBinder.updateItemAtAsync(0, newRenderInfo);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    assertThat(recyclerBinder.getItemCount()).isEqualTo(0);

    mLayoutThreadShadowLooper.runToEndOfTasks();

    verify(adapter, never()).notifyItemChanged(anyInt());

    final ComponentTreeHolder holder = recyclerBinder.getComponentTreeHolderAt(0);
    assertThat(holder.getRenderInfo()).isEqualTo(newRenderInfo);
    assertThat(holder.hasCompletedLatestLayout()).isTrue();
  }

  @Test
  public void testUpdateRangeAtAsync() {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder().rangeRatio(RANGE_RATIO).build(mComponentContext);
    final ArrayList<Component> components = new ArrayList<>();
    final ArrayList<RenderInfo> renderInfos = new ArrayList<>();
    for (int i = 0; i < 7; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
      components.add(component);
      renderInfos.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);
    recyclerBinder.insertRangeAtAsync(0, renderInfos.subList(0, 5));
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(5);

    final ArrayList<Component> newComponents = new ArrayList<>();
    final ArrayList<RenderInfo> newRenderInfos = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
      newComponents.add(component);
      newRenderInfos.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.insertItemAtAsync(0, renderInfos.get(5));
    recyclerBinder.updateRangeAtAsync(0, newRenderInfos);
    recyclerBinder.insertItemAtAsync(0, renderInfos.get(6));
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(7);
    assertComponentAtEquals(recyclerBinder, 0, components.get(6));
    assertComponentAtEquals(recyclerBinder, 1, newComponents.get(0));
    assertComponentAtEquals(recyclerBinder, 2, newComponents.get(1));
    assertComponentAtEquals(recyclerBinder, 3, newComponents.get(2));
    assertComponentAtEquals(recyclerBinder, 4, newComponents.get(3));
    assertComponentAtEquals(recyclerBinder, 5, components.get(3));
    assertComponentAtEquals(recyclerBinder, 6, components.get(4));
  }

  @Test
  public void testRemoveRangeAtAsync() {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder().rangeRatio(RANGE_RATIO).build(mComponentContext);
    final ArrayList<Component> components = new ArrayList<>();
    final ArrayList<RenderInfo> renderInfos = new ArrayList<>();
    for (int i = 0; i < 7; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
      components.add(component);
      renderInfos.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);
    recyclerBinder.insertRangeAtAsync(0, renderInfos.subList(0, 5));
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(5);

    recyclerBinder.insertItemAtAsync(0, renderInfos.get(5));
    recyclerBinder.removeRangeAtAsync(0, 3);
    recyclerBinder.insertItemAtAsync(0, renderInfos.get(6));
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(4);
    assertComponentAtEquals(recyclerBinder, 0, components.get(6));
    assertComponentAtEquals(recyclerBinder, 1, components.get(2));
    assertComponentAtEquals(recyclerBinder, 2, components.get(3));
    assertComponentAtEquals(recyclerBinder, 3, components.get(4));
  }

  @Test
  public void testInsertAsyncOutOfOrderBeforeMeasure() {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder().rangeRatio(RANGE_RATIO).build(mComponentContext);
    final ArrayList<Component> components = new ArrayList<>();
    final ArrayList<RenderInfo> renderInfos = new ArrayList<>();
    for (int i = 0; i < 7; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
      components.add(component);
      renderInfos.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.insertRangeAtAsync(0, renderInfos.subList(0, 3));
    recyclerBinder.insertRangeAtAsync(0, renderInfos.subList(3, renderInfos.size()));
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);

    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(7);

    assertComponentAtEquals(recyclerBinder, 0, components.get(3));
    assertComponentAtEquals(recyclerBinder, 1, components.get(4));
    assertComponentAtEquals(recyclerBinder, 2, components.get(5));
    assertComponentAtEquals(recyclerBinder, 3, components.get(6));
    assertComponentAtEquals(recyclerBinder, 4, components.get(0));
    assertComponentAtEquals(recyclerBinder, 5, components.get(1));
    assertComponentAtEquals(recyclerBinder, 6, components.get(2));
  }

  @Test
  public void testInsertAsyncAndMutationsBeforeMeasure() {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder().rangeRatio(RANGE_RATIO).build(mComponentContext);
    final ArrayList<Component> components = new ArrayList<>();
    final ArrayList<RenderInfo> renderInfos = new ArrayList<>();
    for (int i = 0; i < 7; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
      components.add(component);
      renderInfos.add(ComponentRenderInfo.create().component(component).build());
    }

    final Component updatedIndex3 =
        TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();

    recyclerBinder.insertRangeAtAsync(0, renderInfos);
    recyclerBinder.removeRangeAtAsync(1, 3);
    recyclerBinder.updateItemAtAsync(
        3, ComponentRenderInfo.create().component(updatedIndex3).build());
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);

    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(recyclerBinder.getItemCount()).isEqualTo(4);

    assertComponentAtEquals(recyclerBinder, 0, components.get(0));
    assertComponentAtEquals(recyclerBinder, 1, components.get(4));
    assertComponentAtEquals(recyclerBinder, 2, components.get(5));
    assertComponentAtEquals(recyclerBinder, 3, updatedIndex3);
  }

  @Test
  public void testRenderStateWithNotifyItemRenderCompleteAt() {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder().rangeRatio(RANGE_RATIO).build(mComponentContext);
    final RecyclerView recyclerView = mock(RecyclerView.class);
    recyclerBinder.mount(recyclerView);

    final Component component = TestDrawableComponent.create(mComponentContext).build();
    final EventHandler<RenderCompleteEvent> renderCompleteEventHandler =
        (EventHandler<RenderCompleteEvent>) mock(EventHandler.class);
    final ComponentRenderInfo renderInfo =
        ComponentRenderInfo.create()
            .component(component)
            .renderCompleteHandler(renderCompleteEventHandler)
            .build();

    recyclerBinder.insertItemAt(0, renderInfo);
    recyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    final ComponentTreeHolder holder = recyclerBinder.getComponentTreeHolderAt(0);
    assertThat(holder.getRenderState()).isEqualTo(ComponentTreeHolder.RENDER_UNINITIALIZED);

    recyclerBinder.notifyItemRenderCompleteAt(0, 0);
    verify(recyclerView).postOnAnimation(any(RenderCompleteRunnable.class));

    assertThat(holder.getRenderState()).isEqualTo(ComponentTreeHolder.RENDER_DRAWN);
  }

  @Test
  public void testOnDataBound() {
    final OnDataBoundListener onDataBoundListener1 = mock(OnDataBoundListener.class);
    final OnDataBoundListener onDataBoundListener2 = mock(OnDataBoundListener.class);
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder().rangeRatio(RANGE_RATIO).build(mComponentContext);
    final ArrayList<RenderInfo> renderInfos1 = new ArrayList<>();
    final ArrayList<RenderInfo> renderInfos2 = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
      renderInfos1.add(ComponentRenderInfo.create().component(component).build());
      renderInfos2.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.insertRangeAt(0, renderInfos1);
    recyclerBinder.notifyChangeSetComplete(onDataBoundListener1);

    verify(onDataBoundListener1).onDataBound();
    reset(onDataBoundListener1);

    recyclerBinder.measure(
        new Size(), makeSizeSpec(200, EXACTLY), makeSizeSpec(200, EXACTLY), null);

    recyclerBinder.insertRangeAt(renderInfos1.size(), renderInfos2);
    recyclerBinder.notifyChangeSetComplete(onDataBoundListener2);

    verify(onDataBoundListener1, never()).onDataBound();
    verify(onDataBoundListener2).onDataBound();
  }

  @Test
  public void testOnDataBoundInsertAsync() {
    final OnDataBoundListener onDataBoundListener1 = mock(OnDataBoundListener.class);
    final OnDataBoundListener onDataBoundListener2 = mock(OnDataBoundListener.class);
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder().rangeRatio(RANGE_RATIO).build(mComponentContext);
    final ArrayList<RenderInfo> renderInfos1 = new ArrayList<>();
    final ArrayList<RenderInfo> renderInfos2 = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
      renderInfos1.add(ComponentRenderInfo.create().component(component).build());
      renderInfos2.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.insertRangeAtAsync(0, renderInfos1);
    recyclerBinder.notifyChangeSetComplete(onDataBoundListener1);

    assertThat(recyclerBinder.getItemCount()).isEqualTo(0);
    assertThat(recyclerBinder.getRangeCalculationResult()).isNull();
    verify(onDataBoundListener1, never()).onDataBound();

    recyclerBinder.measure(
        new Size(), makeSizeSpec(200, EXACTLY), makeSizeSpec(200, EXACTLY), null);

    verify(onDataBoundListener1, never()).onDataBound();

    mLayoutThreadShadowLooper.runToEndOfTasks();

    verify(onDataBoundListener1).onDataBound();
    reset(onDataBoundListener1);

    recyclerBinder.insertRangeAtAsync(renderInfos1.size(), renderInfos2);
    recyclerBinder.notifyChangeSetComplete(onDataBoundListener2);

    verify(onDataBoundListener2, never()).onDataBound();

    mLayoutThreadShadowLooper.runToEndOfTasks();

    verify(onDataBoundListener1, never()).onDataBound();
    verify(onDataBoundListener2).onDataBound();
  }

  @Test
  public void testOnDataBoundInsertAsyncLessThanViewport() {
    final OnDataBoundListener onDataBoundListener = mock(OnDataBoundListener.class);
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder().rangeRatio(RANGE_RATIO).build(mComponentContext);
    final ArrayList<RenderInfo> renderInfos = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      final Component component =
          TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build();
      renderInfos.add(ComponentRenderInfo.create().component(component).build());
    }

    recyclerBinder.insertRangeAtAsync(0, renderInfos);
    recyclerBinder.notifyChangeSetComplete(onDataBoundListener);

    verify(onDataBoundListener, never()).onDataBound();

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);

    verify(onDataBoundListener).onDataBound();
  }

  private RecyclerBinder createRecyclerBinderWithMockAdapter(RecyclerView.Adapter adapterMock) {
    return new RecyclerBinder.Builder()
        .rangeRatio(RANGE_RATIO)
        .layoutInfo(new LinearLayoutInfo(mComponentContext, OrientationHelper.HORIZONTAL, false))
        .overrideInternalAdapter(adapterMock)
        .build(mComponentContext);
  }

  private List<ComponentRenderInfo> prepareLoadedBinder() {
    return prepareLoadedBinder(mRecyclerBinder, 100);
  }

  private List<ComponentRenderInfo> prepareLoadedBinder(RecyclerBinder binder, int count) {
    final List<ComponentRenderInfo> components = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      components.add(ComponentRenderInfo.create().component(mock(Component.class)).build());
    }
    binder.insertRangeAt(0, (List) components);
    binder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);

    for (int i = 0; i < count; i++) {
      Assert.assertNotNull(mHoldersForComponents.get(components.get(i).getComponent()));
    }

    Size size = new Size();
    int widthSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);
    int heightSpec = SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY);

    binder.measure(size, widthSpec, heightSpec, null);

    return components;
  }

  private void makeIndexSticky(List<ComponentRenderInfo> components, int i) {
    components.set(
        i,
        ComponentRenderInfo.create().component(mock(Component.class)).isSticky(true).build());
    mRecyclerBinder.removeItemAt(i);
    mRecyclerBinder.insertItemAt(i, components.get(i));
    mRecyclerBinder.notifyChangeSetComplete(NO_OP_ON_DATA_BOUND_LISTENER);
  }

  private RenderInfo createTestComponentRenderInfo() {
    return ComponentRenderInfo.create()
        .component(
            TestDrawableComponent.create(mComponentContext).widthPx(100).heightPx(100).build())
        .build();
  }

  private void assertHasCompatibleLayout(
      RecyclerBinder recyclerBinder, int position, int widthSpec, int heightSpec) {
    final ComponentTree tree = recyclerBinder.getComponentAt(position);
    assertThat(tree).isNotNull();
    assertThat(tree.hasCompatibleLayout(widthSpec, heightSpec)).isTrue();
  }

  private static void assertComponentAtEquals(
      RecyclerBinder recyclerBinder, int position, Component component) {
    final ComponentTreeHolder holder = recyclerBinder.getComponentTreeHolderAt(position);
    assertThat(holder).isNotNull();
    assertThat(holder.getRenderInfo().rendersComponent()).isTrue();
    assertThat(holder.getRenderInfo().getComponent()).isSameAs(component);
  }

  private static class NoOpOnDataBoundListener implements OnDataBoundListener {

    @Override
    public void onDataBound() {}
  }
}
