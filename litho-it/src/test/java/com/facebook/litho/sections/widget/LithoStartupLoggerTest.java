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

package com.facebook.litho.sections.widget;

import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static org.assertj.core.api.Java6Assertions.assertThat;

import androidx.recyclerview.widget.RecyclerView;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoStartupLogger;
import com.facebook.litho.LithoView;
import com.facebook.litho.Size;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.ChangeSetCompleteCallback;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RecyclerBinder;
import com.facebook.litho.widget.RenderInfo;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/** Tests {@link com.facebook.litho.LithoStartupLogger} */
@RunWith(ComponentsTestRunner.class)
public class LithoStartupLoggerTest {

  public static final NoOpChangeSetCompleteCallback NO_OP_CHANGE_SET_COMPLETE_CALLBACK =
      new NoOpChangeSetCompleteCallback();

  private TestLithoStartupLogger mTestLithoStartupLogger;
  private RecyclerBinder mRecyclerBinder;
  private ComponentContext mComponentContext;

  @Before
  public void setup() {
    mTestLithoStartupLogger = new TestLithoStartupLogger();

    mComponentContext = new ComponentContext(RuntimeEnvironment.application);
    mRecyclerBinder =
        new RecyclerBinder.Builder()
            .startupLogger(mTestLithoStartupLogger)
            .build(mComponentContext);
  }

  @Test
  public void markPoint_pointsTraced() {
    mTestLithoStartupLogger.markPoint("_event1", "_stage1");
    mTestLithoStartupLogger.markPoint("_event2", "_stage2");
    mTestLithoStartupLogger.markPoint("_event3", "_stage4", "attr1");

    assertThat(mTestLithoStartupLogger.tracePointCount()).isEqualTo(3);
    assertThat(mTestLithoStartupLogger.getTracedPointAt(0)).isEqualTo("litho_event1_stage1");
    assertThat(mTestLithoStartupLogger.getTracedPointAt(1)).isEqualTo("litho_event2_stage2");
    assertThat(mTestLithoStartupLogger.getTracedPointAt(2)).isEqualTo("litho_attr1_event3_stage4");
  }

  @Test
  public void markPoint_noDuplicatePoints() {
    mTestLithoStartupLogger.markPoint("_event1", "_stage1");
    mTestLithoStartupLogger.markPoint("_event1", "_stage1");

    assertThat(mTestLithoStartupLogger.tracePointCount()).isEqualTo(1);
    assertThat(mTestLithoStartupLogger.getTracedPointAt(0)).isEqualTo("litho_event1_stage1");
  }

  @Test
  public void markPoint_endStageWithoutStartNotTraced() {
    mTestLithoStartupLogger.markPoint("_event1", LithoStartupLogger.END);

    assertThat(mTestLithoStartupLogger.tracePointCount()).isEqualTo(0);

    mTestLithoStartupLogger.markPoint("_event1", LithoStartupLogger.START);
    mTestLithoStartupLogger.markPoint("_event1", LithoStartupLogger.END);

    assertThat(mTestLithoStartupLogger.tracePointCount()).isEqualTo(2);
    assertThat(mTestLithoStartupLogger.getTracedPointAt(0)).isEqualTo("litho_event1_start");
    assertThat(mTestLithoStartupLogger.getTracedPointAt(1)).isEqualTo("litho_event1_end");
  }

  @Test
  public void initRange_noDataAttribution() {
    final List<RenderInfo> components = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      components.add(
          ComponentRenderInfo.create()
              .component(
                  TestDrawableComponent.create(mComponentContext)
                      .widthPx(100)
                      .heightPx(100)
                      .build())
              .build());
    }
    mRecyclerBinder.insertRangeAt(0, components);
    mRecyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);

    assertThat(mTestLithoStartupLogger.tracePointCount()).isEqualTo(2);
    assertThat(mTestLithoStartupLogger.getTracedPointAt(0)).isEqualTo("litho_ui_initrange_start");
    assertThat(mTestLithoStartupLogger.getTracedPointAt(1)).isEqualTo("litho_ui_initrange_end");
  }

  @Test
  public void initRange_withDataAttribution() {
    mTestLithoStartupLogger.setDataAttribution("myquery");

    final List<RenderInfo> components = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      components.add(
          ComponentRenderInfo.create()
              .component(
                  TestDrawableComponent.create(mComponentContext)
                      .widthPx(100)
                      .heightPx(100)
                      .build())
              .build());
    }
    mRecyclerBinder.insertRangeAt(0, components);
    mRecyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);
    mRecyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY), null);

    assertThat(mTestLithoStartupLogger.tracePointCount()).isEqualTo(2);
    assertThat(mTestLithoStartupLogger.getTracedPointAt(0))
        .isEqualTo("litho_ui_myquery_initrange_start");
    assertThat(mTestLithoStartupLogger.getTracedPointAt(1))
        .isEqualTo("litho_ui_myquery_initrange_end");
  }

  @Test
  public void firstmount_noDataAttribution() {
    final RecyclerView recyclerView = new RecyclerView(mComponentContext.getAndroidContext());

    final List<RenderInfo> components = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      components.add(
          ComponentRenderInfo.create()
              .component(
                  TestDrawableComponent.create(mComponentContext)
                      .widthPx(100)
                      .heightPx(100)
                      .build())
              .build());
    }
    mRecyclerBinder.mount(recyclerView);
    mRecyclerBinder.insertRangeAt(0, components);
    mRecyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    createBindAndMountLithoView(recyclerView, 0);

    assertThat(mTestLithoStartupLogger.tracePointCount()).isEqualTo(0);
  }

  @Test
  public void firstmount_withDataAttribution() {
    mTestLithoStartupLogger.setDataAttribution("myquery");
    final RecyclerView recyclerView = new RecyclerView(mComponentContext.getAndroidContext());

    final List<RenderInfo> components = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      components.add(
          ComponentRenderInfo.create()
              .component(
                  TestDrawableComponent.create(mComponentContext)
                      .widthPx(100)
                      .heightPx(100)
                      .build())
              .build());
    }
    mRecyclerBinder.mount(recyclerView);
    mRecyclerBinder.insertRangeAt(0, components);
    mRecyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    createBindAndMountLithoView(recyclerView, 0);

    assertThat(mTestLithoStartupLogger.tracePointCount()).isEqualTo(2);
    assertThat(mTestLithoStartupLogger.getTracedPointAt(0))
        .isEqualTo("litho_myquery_firstmount_start");
    assertThat(mTestLithoStartupLogger.getTracedPointAt(1))
        .isEqualTo("litho_myquery_firstmount_end");
  }

  @Test
  public void lastmount_withDataAttributionLastAdapterItem() {
    mTestLithoStartupLogger.setDataAttribution("myquery");
    final RecyclerView recyclerView = new RecyclerView(mComponentContext.getAndroidContext());

    final List<RenderInfo> components = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      components.add(
          ComponentRenderInfo.create()
              .component(
                  TestDrawableComponent.create(mComponentContext)
                      .widthPx(100)
                      .heightPx(100)
                      .build())
              .build());
    }
    mRecyclerBinder.mount(recyclerView);
    mRecyclerBinder.insertRangeAt(0, components);
    mRecyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    createBindAndMountLithoView(recyclerView, 0);

    assertThat(mTestLithoStartupLogger.tracePointCount()).isEqualTo(2); // first_mount points

    createBindAndMountLithoView(recyclerView, 2);

    assertThat(mTestLithoStartupLogger.tracePointCount()).isEqualTo(4);
    assertThat(mTestLithoStartupLogger.getTracedPointAt(2))
        .isEqualTo("litho_myquery_lastmount_start");
    assertThat(mTestLithoStartupLogger.getTracedPointAt(3))
        .isEqualTo("litho_myquery_lastmount_end");
  }

  @Test
  public void lastmount_withDataAttributionNonLastAdapterItemLastVisibleItem() {
    mTestLithoStartupLogger.setDataAttribution("myquery");
    final RecyclerView recyclerView = new RecyclerView(mComponentContext.getAndroidContext());

    final List<RenderInfo> components = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      components.add(
          ComponentRenderInfo.create()
              .component(
                  TestDrawableComponent.create(mComponentContext)
                      .widthPx(100)
                      .heightPx(100)
                      .build())
              .build());
    }
    mRecyclerBinder.mount(recyclerView);
    mRecyclerBinder.insertRangeAt(0, components);
    mRecyclerBinder.measure(
        new Size(), makeSizeSpec(1000, EXACTLY), makeSizeSpec(150, EXACTLY), null);

    assertThat(mTestLithoStartupLogger.tracePointCount()).isEqualTo(2); // init_range points

    mRecyclerBinder.notifyChangeSetComplete(true, NO_OP_CHANGE_SET_COMPLETE_CALLBACK);

    createBindAndMountLithoView(recyclerView, 0);

    assertThat(mTestLithoStartupLogger.tracePointCount()).isEqualTo(4); // first_mount points

    createBindAndMountLithoView(recyclerView, 1);

    assertThat(mTestLithoStartupLogger.tracePointCount()).isEqualTo(6);
    assertThat(mTestLithoStartupLogger.getTracedPointAt(4))
        .isEqualTo("litho_myquery_lastmount_start");
    assertThat(mTestLithoStartupLogger.getTracedPointAt(5))
        .isEqualTo("litho_myquery_lastmount_end");
  }

  void createBindAndMountLithoView(RecyclerView recyclerView, int position) {
    final RecyclerView.ViewHolder vh = recyclerView.getAdapter().onCreateViewHolder(null, 0);
    recyclerView.getAdapter().onBindViewHolder(vh, position);
    ComponentTestHelper.mountComponent(
        (LithoView) vh.itemView, mRecyclerBinder.getComponentAt(position));
  }

  private static class NoOpChangeSetCompleteCallback implements ChangeSetCompleteCallback {

    @Override
    public void onDataBound() {}

    @Override
    public void onDataRendered(boolean isMounted, long uptimeMillis) {}
  }
}
