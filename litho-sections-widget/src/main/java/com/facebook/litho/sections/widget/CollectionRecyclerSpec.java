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

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ItemAnimator;
import androidx.recyclerview.widget.RecyclerView.ItemDecoration;
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.EventHandler;
import com.facebook.litho.Handle;
import com.facebook.litho.LithoStartupLogger;
import com.facebook.litho.StateValue;
import com.facebook.litho.TouchEvent;
import com.facebook.litho.annotations.FromTrigger;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnDetached;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnTrigger;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.State;
import com.facebook.litho.sections.Section;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.SectionTree;
import com.facebook.litho.widget.Binder;
import com.facebook.litho.widget.LithoRecyclerView;
import com.facebook.litho.widget.PTRRefreshEvent;
import com.facebook.litho.widget.Recycler;
import com.facebook.litho.widget.RecyclerBinder;
import com.facebook.litho.widget.RecyclerEventsController;
import com.facebook.litho.widget.SectionsRecyclerView;
import com.facebook.litho.widget.SmoothScrollAlignmentType;
import com.facebook.litho.widget.ViewportInfo;
import java.util.List;

@LayoutSpec(events = PTRRefreshEvent.class)
public class CollectionRecyclerSpec {

  @PropDefault
  public static final RecyclerConfiguration recyclerConfiguration =
      ListRecyclerConfiguration.create().build();

  @OnCreateLayout
  static @Nullable Component onCreateLayout(
      final ComponentContext c,
      @Prop Section section,
      @Prop(optional = true, varArg = "onScrollListener") @Nullable
          List<OnScrollListener> onScrollListeners,
      @Prop(optional = true) Boolean clipToPadding,
      @Prop(optional = true) Boolean clipChildren,
      @Prop(optional = true) Boolean nestedScrollingEnabled,
      @Prop(optional = true) Integer scrollBarStyle,
      @Prop(optional = true) ItemDecoration itemDecoration,
      @Prop(optional = true) @Nullable ItemAnimator itemAnimator,
      @Prop(optional = true) @IdRes Integer recyclerViewId,
      @Prop(optional = true) Integer overScrollMode,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) int startPadding,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) int endPadding,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) int topPadding,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) int bottomPadding,
      @Prop(optional = true) EventHandler<TouchEvent> recyclerTouchEventHandler,
      @Prop(optional = true) boolean horizontalFadingEdgeEnabled,
      @Prop(optional = true) boolean verticalFadingEdgeEnabled,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) int fadingEdgeLength,
      @Prop(optional = true, resType = ResType.COLOR) @Nullable
          Integer refreshProgressBarBackgroundColor,
      @Prop(optional = true, resType = ResType.COLOR) Integer refreshProgressBarColor,
      @Prop(optional = true) @Nullable LithoRecyclerView.TouchInterceptor touchInterceptor,
      @Prop(optional = true) OnItemTouchListener itemTouchListener,
      @Prop(optional = true) boolean pullToRefreshEnabled,
      @Prop(optional = true) RecyclerConfiguration recyclerConfiguration,
      @Prop(optional = true) SectionsRecyclerView.SectionsRecylerViewLogger sectionsViewLogger,
      @State RecyclerEventsController recyclerEventsController,
      @State Binder<RecyclerView> binder,
      @State SectionTree sectionTree) {

    sectionTree.setRoot(section);

    final boolean internalPullToRefreshEnabled =
        recyclerConfiguration.getOrientation() != OrientationHelper.HORIZONTAL
            && pullToRefreshEnabled;

    final Recycler.Builder recycler =
        Recycler.create(c)
            .leftPadding(startPadding)
            .rightPadding(endPadding)
            .topPadding(topPadding)
            .bottomPadding(bottomPadding)
            .recyclerEventsController(recyclerEventsController)
            .refreshHandler(!pullToRefreshEnabled ? null : CollectionRecycler.onRefresh(c))
            .pullToRefresh(internalPullToRefreshEnabled)
            .itemDecoration(itemDecoration)
            .horizontalFadingEdgeEnabled(horizontalFadingEdgeEnabled)
            .verticalFadingEdgeEnabled(verticalFadingEdgeEnabled)
            .fadingEdgeLengthDip(fadingEdgeLength)
            .onScrollListeners(onScrollListeners)
            .refreshProgressBarBackgroundColor(refreshProgressBarBackgroundColor)
            .snapHelper(recyclerConfiguration.getSnapHelper())
            .touchInterceptor(touchInterceptor)
            .onItemTouchListener(itemTouchListener)
            .binder(binder)
            .touchHandler(recyclerTouchEventHandler)
            .sectionsViewLogger(sectionsViewLogger);

    if (clipToPadding != null) {
      recycler.clipToPadding(clipToPadding);
    }
    if (clipChildren != null) {
      recycler.clipChildren(clipChildren);
    }
    if (nestedScrollingEnabled != null) {
      recycler.nestedScrollingEnabled(nestedScrollingEnabled);
    }
    if (scrollBarStyle != null) {
      recycler.scrollBarStyle(scrollBarStyle);
    }
    if (recyclerViewId != null) {
      recycler.recyclerViewId(recyclerViewId);
    }
    if (overScrollMode != null) {
      recycler.overScrollMode(overScrollMode);
    }
    if (refreshProgressBarColor != null) {
      recycler.refreshProgressBarColor(refreshProgressBarColor);
    }
    if (itemAnimator != null) {
      recycler.itemAnimator(itemAnimator);
    }

    return recycler.build();
  }

  @OnCreateInitialState
  static void createInitialState(
      final ComponentContext c,
      StateValue<SectionTree> sectionTree,
      StateValue<Binder<RecyclerView>> binder,
      StateValue<RecyclerEventsController> recyclerEventsController,
      @Prop Section section,
      @Prop(optional = true) RecyclerConfiguration recyclerConfiguration,
      @Prop(optional = true) String sectionTreeTag,
      @Prop(optional = true) boolean canMeasureRecycler,
      @Prop(optional = true) @Nullable LithoStartupLogger startupLogger) {

    final RecyclerBinderConfiguration binderConfiguration =
        recyclerConfiguration.getRecyclerBinderConfiguration();

    final RecyclerBinder.Builder recyclerBinderBuilder =
        new RecyclerBinder.Builder()
            .layoutInfo(recyclerConfiguration.getLayoutInfo(c))
            .rangeRatio(binderConfiguration.getRangeRatio())
            .layoutHandlerFactory(binderConfiguration.getLayoutHandlerFactory())
            .wrapContent(binderConfiguration.isWrapContent())
            .enableStableIds(binderConfiguration.getEnableStableIds())
            .invalidStateLogParamsList(binderConfiguration.getInvalidStateLogParamsList())
            .threadPoolConfig(binderConfiguration.getThreadPoolConfiguration())
            .hscrollAsyncMode(binderConfiguration.getHScrollAsyncMode())
            .isCircular(binderConfiguration.isCircular())
            .hasDynamicItemHeight(binderConfiguration.hasDynamicItemHeight())
            .enableDetach(binderConfiguration.getEnableDetach())
            .componentsConfiguration(binderConfiguration.getComponentsConfiguration())
            .canInterruptAndMoveLayoutsBetweenThreads(
                binderConfiguration.moveLayoutsBetweenThreads())
            .isReconciliationEnabled(binderConfiguration.isReconciliationEnabled())
            .recyclingMode(binderConfiguration.getRecyclingMode())
            .isLayoutDiffingEnabled(binderConfiguration.isLayoutDiffingEnabled())
            .componentWarmer(binderConfiguration.getComponentWarmer())
            .lithoViewFactory(binderConfiguration.getLithoViewFactory())
            .errorEventHandler(binderConfiguration.getErrorEventHandler())
            .startupLogger(startupLogger);

    if (binderConfiguration.getEstimatedViewportCount()
        != RecyclerBinderConfiguration.Builder.UNSET) {
      recyclerBinderBuilder.estimatedViewportCount(binderConfiguration.getEstimatedViewportCount());
    }
    final RecyclerBinder recyclerBinder = recyclerBinderBuilder.build(c);

    final SectionBinderTarget targetBinder =
        new SectionBinderTarget(recyclerBinder, binderConfiguration.getUseBackgroundChangeSets());
    binder.set(targetBinder);

    final SectionTree sectionTreeInstance =
        SectionTree.create(new SectionContext(c), targetBinder)
            .tag(
                sectionTreeTag == null || sectionTreeTag.equals("")
                    ? section.getSimpleName()
                    : sectionTreeTag)
            .changeSetThreadHandler(binderConfiguration.getChangeSetThreadHandler())
            .postToFrontOfQueueForFirstChangeset(
                binderConfiguration.isPostToFrontOfQueueForFirstChangeset())
            .build();
    sectionTree.set(sectionTreeInstance);

    recyclerEventsController.set(new RecyclerEventsController());

    final ViewportInfo.ViewportChanged viewPortChanged =
        new ViewportInfo.ViewportChanged() {
          @Override
          public void viewportChanged(
              int firstVisibleIndex,
              int lastVisibleIndex,
              int firstFullyVisibleIndex,
              int lastFullyVisibleIndex,
              int state) {
            sectionTreeInstance.viewPortChanged(
                firstVisibleIndex,
                lastVisibleIndex,
                firstFullyVisibleIndex,
                lastFullyVisibleIndex,
                state);
          }
        };

    targetBinder.setViewportChangedListener(viewPortChanged);
    targetBinder.setCanMeasure(canMeasureRecycler);
  }

  @OnEvent(PTRRefreshEvent.class)
  protected static boolean onRefresh(ComponentContext c, @State SectionTree sectionTree) {
    EventHandler<PTRRefreshEvent> ptrEventHandler = CollectionRecycler.getPTRRefreshEventHandler(c);

    if (ptrEventHandler == null) {
      sectionTree.refresh();
      return true;
    }

    final boolean isHandled = CollectionRecycler.dispatchPTRRefreshEvent(ptrEventHandler);
    if (!isHandled) {
      sectionTree.refresh();
    }

    return true;
  }

  @OnTrigger(ScrollEvent.class)
  static void onScroll(
      ComponentContext c, @FromTrigger int position, @State SectionTree sectionTree) {
    sectionTree.requestFocusOnRoot(position);
  }

  @OnTrigger(ScrollToHandle.class)
  static void onScrollToHandle(
      ComponentContext c,
      @FromTrigger Handle target,
      @FromTrigger int offset,
      @State SectionTree sectionTree) {
    sectionTree.requestFocusOnRoot(target, offset);
  }

  @OnTrigger(SmoothScrollEvent.class)
  static void onSmoothScroll(
      ComponentContext c,
      @FromTrigger int index,
      @FromTrigger int offset,
      @FromTrigger SmoothScrollAlignmentType type,
      @State SectionTree sectionTree) {
    sectionTree.requestSmoothFocusOnRoot(index, offset, type);
  }

  @OnTrigger(SmoothScrollToHandleEvent.class)
  static void onSmoothScrollToHandle(
      ComponentContext c,
      @FromTrigger Handle target,
      @FromTrigger int offset,
      @FromTrigger SmoothScrollAlignmentType type,
      @State SectionTree sectionTree) {
    sectionTree.requestSmoothFocusOnRoot(target, offset, type);
  }

  @OnTrigger(ClearRefreshingEvent.class)
  static void onClearRefreshing(
      ComponentContext c, @State RecyclerEventsController recyclerEventsController) {
    recyclerEventsController.clearRefreshing();
  }

  @OnDetached
  static void onDetached(ComponentContext c, @State Binder<RecyclerView> binder) {
    binder.detach();
  }
}
