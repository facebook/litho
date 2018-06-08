/*
 * Copyright 2018-present Facebook, Inc.
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

import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.EventHandler;
import com.facebook.litho.LayoutHandler;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.viewcompat.SimpleViewBinder;
import com.facebook.litho.viewcompat.ViewCreator;
import java.util.HashMap;
import java.util.Map;
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

/** Tests for viewport filling in {@link RecyclerBinder} */
@RunWith(ComponentsTestRunner.class)
public class RecyclerBinderFillViewportTest {

  private static final float RANGE_RATIO = 2.0f;
  private static final int RANGE_SIZE = 3;

  private final Map<Component, TestComponentTreeHolder> mHoldersForComponents = new HashMap<>();
  private RecyclerBinder mRecyclerBinder;
  private RecyclerBinder.Builder mRecyclerBinderBuilder;
  private LayoutInfo mLayoutInfo;
  private LayoutInfo mCircularLayoutInfo;
  private ComponentContext mComponentContext;
  private ShadowLooper mLayoutThreadShadowLooper;

  private static final ViewCreator VIEW_CREATOR_1 =
      new ViewCreator() {
        @Override
        public View createView(Context c, ViewGroup parent) {
          return mock(View.class);
        }
      };

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
              ComponentTreeHolder.ComponentTreeMeasureListenerFactory
                  componentTreeMeasureListenerFactory,
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
  public void testDoesNotFillViewportWithConfigurationOff() {
    final LayoutInfo layoutInfo = mock(LayoutInfo.class);
    setupBaseLayoutInfoMock(layoutInfo, OrientationHelper.VERTICAL);

    RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .rangeRatio(RANGE_RATIO)
            .layoutInfo(layoutInfo)
            .build(mComponentContext);

    fillRecyclerBinderWithComponents(recyclerBinder, 100, 100, 10);

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);

    verify(layoutInfo, never()).createViewportFiller(anyInt(), anyInt());
  }

  @Test
  public void testDoesNotFillViewportHScrollOnly() {
    ComponentsConfiguration.fillListViewportHScrollOnly = true;

    final LayoutInfo layoutInfo = mock(LayoutInfo.class);
    setupBaseLayoutInfoMock(layoutInfo, OrientationHelper.VERTICAL);

    RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .rangeRatio(RANGE_RATIO)
            .layoutInfo(layoutInfo)
            .build(mComponentContext);

    fillRecyclerBinderWithComponents(recyclerBinder, 100, 100, 10);

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);

    verify(layoutInfo, never()).createViewportFiller(anyInt(), anyInt());
  }

  @Test
  public void testFillsViewport() {
    ComponentsConfiguration.fillListViewport = true;

    final LayoutInfo layoutInfo =
        new LinearLayoutInfo(mComponentContext, OrientationHelper.VERTICAL, false);

    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .rangeRatio(RANGE_RATIO)
            .layoutInfo(layoutInfo)
            .build(mComponentContext);

    fillRecyclerBinderWithComponents(recyclerBinder, 100, 100, 10);

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(250, EXACTLY), null);

    final int expectedWidthSpec = makeSizeSpec(1000, EXACTLY);
    final int expectedHeightSpec = makeSizeSpec(0, UNSPECIFIED);
    assertThat(
            recyclerBinder
                .getComponentAt(0)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isTrue();
    assertThat(
            recyclerBinder
                .getComponentAt(1)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isTrue();
    assertThat(
            recyclerBinder
                .getComponentAt(2)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isTrue();
    assertThat(
            recyclerBinder
                .getComponentAt(3)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isFalse();
  }

  @Test
  public void testFillsViewportHScroll() {
    ComponentsConfiguration.fillListViewportHScrollOnly = true;

    final LayoutInfo layoutInfo =
        new LinearLayoutInfo(mComponentContext, OrientationHelper.HORIZONTAL, false);

    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .rangeRatio(RANGE_RATIO)
            .layoutInfo(layoutInfo)
            .build(mComponentContext);

    fillRecyclerBinderWithComponents(recyclerBinder, 100, 100, 10);

    recyclerBinder.measure(
        new Size(), makeSizeSpec(250, EXACTLY), makeSizeSpec(1000, EXACTLY), null);

    final int expectedWidthSpec = makeSizeSpec(0, UNSPECIFIED);
    final int expectedHeightSpec = makeSizeSpec(1000, SizeSpec.EXACTLY);
    assertThat(
            recyclerBinder
                .getComponentAt(0)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isTrue();
    assertThat(
            recyclerBinder
                .getComponentAt(1)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isTrue();
    assertThat(
            recyclerBinder
                .getComponentAt(2)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isTrue();
    assertThat(
            recyclerBinder
                .getComponentAt(3)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isFalse();
  }

  @Test
  public void testFillsViewportWithMeasureBeforeData() {
    ComponentsConfiguration.fillListViewport = true;

    final LayoutInfo layoutInfo =
        new LinearLayoutInfo(mComponentContext, OrientationHelper.VERTICAL, false);

    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .rangeRatio(RANGE_RATIO)
            .layoutInfo(layoutInfo)
            .build(mComponentContext);

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(250, EXACTLY), null);

    fillRecyclerBinderWithComponents(recyclerBinder, 100, 100, 10);

    final int expectedWidthSpec = makeSizeSpec(1000, EXACTLY);
    final int expectedHeightSpec = makeSizeSpec(0, UNSPECIFIED);
    assertThat(
            recyclerBinder
                .getComponentAt(0)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isTrue();
    assertThat(
            recyclerBinder
                .getComponentAt(1)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isTrue();
    assertThat(
            recyclerBinder
                .getComponentAt(2)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isTrue();
    assertThat(
            recyclerBinder
                .getComponentAt(3)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isFalse();
  }

  @Test
  public void testDoesNotFillViewportOnRemeasure() {
    ComponentsConfiguration.fillListViewport = true;

    final LayoutInfo layoutInfo =
        new LinearLayoutInfo(mComponentContext, OrientationHelper.HORIZONTAL, false);

    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .rangeRatio(RANGE_RATIO)
            .layoutInfo(layoutInfo)
            .build(mComponentContext);

    fillRecyclerBinderWithComponents(recyclerBinder, 100, 100, 10);

    recyclerBinder.measure(
        new Size(), makeSizeSpec(250, EXACTLY), makeSizeSpec(1000, EXACTLY), null);

    final int expectedWidthSpec = makeSizeSpec(0, UNSPECIFIED);
    final int expectedHeightSpec = makeSizeSpec(1000, SizeSpec.EXACTLY);
    assertThat(
            recyclerBinder
                .getComponentAt(0)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isTrue();
    assertThat(
            recyclerBinder
                .getComponentAt(1)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isTrue();
    assertThat(
            recyclerBinder
                .getComponentAt(2)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isTrue();
    assertThat(
            recyclerBinder
                .getComponentAt(3)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isFalse();

    recyclerBinder.measure(
        new Size(), makeSizeSpec(250, EXACTLY), makeSizeSpec(900, EXACTLY), null);

    final int newExpectedHeightSpec = makeSizeSpec(900, SizeSpec.EXACTLY);
    assertThat(
            recyclerBinder
                .getComponentAt(0)
                .hasCompatibleLayout(expectedWidthSpec, newExpectedHeightSpec))
        .isTrue();
    assertThat(
            recyclerBinder
                .getComponentAt(1)
                .hasCompatibleLayout(expectedWidthSpec, newExpectedHeightSpec))
        .isFalse();
  }

  @Test
  public void testDoesNotFillViewportOnCompatibleMeasure() {
    ComponentsConfiguration.fillListViewport = true;

    final LayoutInfo layoutInfo = mock(LayoutInfo.class);
    setupBaseLayoutInfoMock(layoutInfo, OrientationHelper.HORIZONTAL);

    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .rangeRatio(RANGE_RATIO)
            .layoutInfo(layoutInfo)
            .build(mComponentContext);

    when(layoutInfo.findFirstFullyVisibleItemPosition()).thenReturn(0);

    fillRecyclerBinderWithComponents(recyclerBinder, 100, 100, 10);

    recyclerBinder.measure(
        new Size(), makeSizeSpec(250, EXACTLY), makeSizeSpec(1000, EXACTLY), null);

    verify(layoutInfo).createViewportFiller(anyInt(), anyInt());
    reset(layoutInfo);

    recyclerBinder.measure(
        new Size(), makeSizeSpec(250, EXACTLY), makeSizeSpec(1000, EXACTLY), null);

    verify(layoutInfo, never()).createViewportFiller(anyInt(), anyInt());
  }

  @Test
  public void testFillsViewportFromFirstVisibleItem() {
    ComponentsConfiguration.fillListViewport = true;

    final LayoutInfo layoutInfo =
        spy(new LinearLayoutInfo(mComponentContext, OrientationHelper.HORIZONTAL, false));

    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .rangeRatio(RANGE_RATIO)
            .layoutInfo(layoutInfo)
            .build(mComponentContext);

    fillRecyclerBinderWithComponents(recyclerBinder, 100, 100, 10);

    when(layoutInfo.findFirstVisibleItemPosition()).thenReturn(5);
    recyclerBinder.measure(
        new Size(), makeSizeSpec(250, EXACTLY), makeSizeSpec(1000, EXACTLY), null);

    final int expectedWidthSpec = makeSizeSpec(0, UNSPECIFIED);
    final int expectedHeightSpec = makeSizeSpec(1000, SizeSpec.EXACTLY);
    assertThat(
            recyclerBinder
                .getComponentAt(5)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isTrue();
    assertThat(
            recyclerBinder
                .getComponentAt(6)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isTrue();
    assertThat(
            recyclerBinder
                .getComponentAt(7)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isTrue();
    assertThat(
            recyclerBinder
                .getComponentAt(8)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isFalse();

    assertThat(
            recyclerBinder
                .getComponentAt(1)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isFalse();
    assertThat(
            recyclerBinder
                .getComponentAt(4)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isFalse();
  }

  @Test
  public void testFillsViewportWithSomeViews() {
    ComponentsConfiguration.fillListViewport = true;

    final LayoutInfo layoutInfo =
        new LinearLayoutInfo(mComponentContext, OrientationHelper.HORIZONTAL, false);

    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .rangeRatio(RANGE_RATIO)
            .layoutInfo(layoutInfo)
            .build(mComponentContext);

    fillRecyclerBinderWithComponents(recyclerBinder, 100, 100, 3);

    recyclerBinder.insertItemAt(
        3,
        ViewRenderInfo.create()
            .viewBinder(new SimpleViewBinder())
            .viewCreator(VIEW_CREATOR_1)
            .build());

    for (int i = 4; i < 7; i++) {
      recyclerBinder.insertItemAt(
          i,
          ComponentRenderInfo.create()
              .component(
                  TestDrawableComponent.create(mComponentContext)
                      .widthPx(100)
                      .heightPx(100)
                      .build())
              .build());
    }
    mRecyclerBinder.notifyChangeSetComplete();

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);

    final int expectedWidthSpec = makeSizeSpec(0, UNSPECIFIED);
    final int expectedHeightSpec = makeSizeSpec(1000, SizeSpec.EXACTLY);
    assertThat(
            recyclerBinder
                .getComponentAt(0)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isTrue();
    assertThat(
            recyclerBinder
                .getComponentAt(1)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isTrue();
    assertThat(
            recyclerBinder
                .getComponentAt(2)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isTrue();
    assertThat(
            recyclerBinder
                .getComponentAt(4)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isFalse();
    assertThat(
            recyclerBinder
                .getComponentAt(5)
                .hasCompatibleLayout(expectedWidthSpec, expectedHeightSpec))
        .isFalse();
  }

  @Test
  public void testFillViewportWithAllViews() {
    ComponentsConfiguration.fillListViewport = true;

    final LayoutInfo layoutInfo =
        new LinearLayoutInfo(mComponentContext, OrientationHelper.HORIZONTAL, false);

    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .rangeRatio(RANGE_RATIO)
            .layoutInfo(layoutInfo)
            .build(mComponentContext);

    recyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);

    // Just make sure we don't crash
  }

  @Test
  public void testRemeasureAfterInsertFills() {
    ComponentsConfiguration.fillListViewport = true;

    final LayoutInfo layoutInfo = mock(LayoutInfo.class);
    setupBaseLayoutInfoMock(layoutInfo, OrientationHelper.HORIZONTAL);

    final RecyclerView recyclerView = mock(RecyclerView.class);

    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .rangeRatio(RANGE_RATIO)
            .layoutInfo(layoutInfo)
            .build(mComponentContext);

    recyclerBinder.mount(recyclerView);

    // Simulate the remeasure runnable
    doAnswer(
            new Answer() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                recyclerBinder.measure(
                    new Size(),
                    makeSizeSpec(250, EXACTLY),
                    makeSizeSpec(0, UNSPECIFIED),
                    mock(EventHandler.class));
                return null;
              }
            })
        .when(recyclerView)
        .postOnAnimation(recyclerBinder.mRemeasureRunnable);

    recyclerBinder.measure(
        new Size(),
        makeSizeSpec(250, EXACTLY),
        makeSizeSpec(0, UNSPECIFIED),
        mock(EventHandler.class));

    verify(layoutInfo, never()).createViewportFiller(anyInt(), anyInt());

    fillRecyclerBinderWithComponents(recyclerBinder, 100, 100, 10);

    verify(layoutInfo).createViewportFiller(anyInt(), anyInt());
  }

  private void fillRecyclerBinderWithComponents(
      RecyclerBinder recyclerBinder,
      int componentWidthPx,
      int componentHeightPx,
      int numComponents) {
    for (int i = 0; i < numComponents; i++) {
      recyclerBinder.insertItemAt(
          i,
          ComponentRenderInfo.create()
              .component(
                  TestDrawableComponent.create(mComponentContext)
                      .widthPx(componentWidthPx)
                      .heightPx(componentHeightPx)
                      .build())
              .build());
    }
    recyclerBinder.notifyChangeSetComplete();
  }
}
