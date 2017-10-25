/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.widget;

import static com.facebook.litho.Column.create;
import static com.facebook.litho.sections.LoadingEvent.LoadingState.FAILED;
import static com.facebook.litho.sections.LoadingEvent.LoadingState.INITIAL_LOAD;
import static com.facebook.litho.sections.LoadingEvent.LoadingState.LOADING;
import static com.facebook.litho.sections.LoadingEvent.LoadingState.SUCCEEDED;
import static com.facebook.yoga.YogaAlign.FLEX_START;
import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.yoga.YogaPositionType.ABSOLUTE;

import android.support.annotation.IdRes;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ItemAnimator;
import android.support.v7.widget.RecyclerView.ItemDecoration;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.support.v7.widget.SnapHelper;
import android.view.View;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.ComponentLayout.ContainerBuilder;
import com.facebook.litho.EventHandler;
import com.facebook.litho.Layout;
import com.facebook.litho.StateValue;
import com.facebook.litho.TouchEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.State;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.sections.BaseLoadEventsHandler;
import com.facebook.litho.sections.LoadEventsHandler;
import com.facebook.litho.sections.LoadingEvent;
import com.facebook.litho.sections.LoadingEvent.LoadingState;
import com.facebook.litho.sections.Section;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.SectionTree;
import com.facebook.litho.sections.SectionTree.Target;
import com.facebook.litho.sections.config.SectionsConfiguration;
import com.facebook.litho.widget.Binder;
import com.facebook.litho.widget.PTRRefreshEvent;
import com.facebook.litho.widget.Recycler;
import com.facebook.litho.widget.RecyclerEventsController;
import com.facebook.litho.widget.ViewportInfo;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A {@link Component} that renders a {@link Recycler} backed by a {@link Section} tree.
 *
 * This {@link Component} handles the loading events from the {@link Section} hierarchy and shows
 * the appropriate error,loading or empty {@link Component} passed in as props.
 * If either the empty or the error components are not passed in and the
 * {@link RecyclerCollectionComponent} is in one of these states it will simply not render
 * anything.
 *
 * The {@link RecyclerCollectionComponent} also exposes a {@link LoadEventsHandler} and a
 * {@link OnScrollListener} as {@link Prop}s so its users can receive events about the state
 * of the loading and about the state of the {@link Recycler} scrolling.
 *
 * clipToPadding, clipChildren, itemDecoration, scrollBarStyle, horizontalPadding, verticalPadding
 * and recyclerViewId {@link Prop}s will be directly applied to the {@link Recycler} component.
 *
 * The {@link RecyclerCollectionEventsController} {@link Prop} is a way to sent commands to the
 * {@link RecyclerCollectionComponentSpec}, such as scrollTo(position) and refresh().
 */
@LayoutSpec
public class RecyclerCollectionComponentSpec {

  @PropDefault protected static final RecyclerConfiguration recyclerConfiguration =
      new ListRecyclerConfiguration();
  @PropDefault protected static final boolean nestedScrollingEnabled = true;
  @PropDefault protected static final int scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY;
  @PropDefault protected static final int recyclerViewId = View.NO_ID;
  @PropDefault
  protected static final boolean asyncStateUpdates =
      SectionsConfiguration.sectionComponentsAsyncStateUpdates;

  @PropDefault
  protected static final boolean asyncPropUpdates =
      SectionsConfiguration.sectionComponentsAsyncPropUpdates;

  @PropDefault static final boolean clipToPadding = true;
  @PropDefault static final boolean clipChildren = true;
  @PropDefault static final int refreshProgressBarColor = 0XFF4267B2; // blue
  private static final int MIN_SCROLL_FOR_PAGE = 20;

  /**
   * A configuration object the {@link RecyclerCollectionComponent} will use to determine which
   * layout manager should be used for the {@link RecyclerView}
   */
  public interface RecyclerConfiguration {

    <E extends Binder<RecyclerView> & Target> E buildTarget(ComponentContext c);

    @Nullable
    SnapHelper getSnapHelper();
  }

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      final ComponentContext c,
      @Prop Section<?> section,
      @Prop(optional = true) Component<?> loadingComponent,
      @Prop(optional = true) Component<?> emptyComponent,
      @Prop(optional = true) Component<?> errorComponent,
      @Prop(optional = true, varArg = "onScrollListener") List<OnScrollListener> onScrollListeners,
      @Prop(optional = true) final LoadEventsHandler loadEventsHandler,
      @Prop(optional = true) boolean clipToPadding,
      @Prop(optional = true) boolean clipChildren,
      @Prop(optional = true) boolean nestedScrollingEnabled,
      @Prop(optional = true) int scrollBarStyle,
      @Prop(optional = true) ItemDecoration itemDecoration,
      @Prop(optional = true) ItemAnimator itemAnimator,
      @Prop(optional = true) boolean disablePTR,
      @Prop(optional = true) @IdRes int recyclerViewId,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) int leftPadding,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) int rightPadding,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) int topPadding,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) int bottomPadding,
      @Prop(optional = true) EventHandler<TouchEvent> recyclerTouchEventHandler,
      @Prop(optional = true) boolean canMeasureRecycler,
      @Prop(optional = true) boolean horizontalFadingEdgeEnabled,
      @Prop(optional = true) boolean verticalFadingEdgeEnabled,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) int fadingEdgeLength,
      @Prop(optional = true, resType = ResType.COLOR) int refreshProgressBarColor,
      @State(canUpdateLazily = true) boolean hasSetSectionTreeRoot,
      @State RecyclerCollectionEventsController internalEventsController,
      @State(canUpdateLazily = true) LoadingEvent.LoadingState loadingState,
      @State boolean isEmpty,
      @State Binder<RecyclerView> binder,
      @State SectionTree sectionTree,
      @State SnapHelper snapHelper) {
    sectionTree.setLoadEventsHandler(
        new RecyclerCollectionLoadEventsHandler(
            loadEventsHandler,
            c,
            internalEventsController,
            isEmpty));

    if (hasSetSectionTreeRoot && ComponentsConfiguration.setRootAsyncRecyclerCollectionComponent) {
      sectionTree.setRootAsync(section);
    } else {
      RecyclerCollectionComponent.lazyUpdateHasSetSectionTreeRoot(c, true);
      sectionTree.setRoot(section);
    }

    if (internalEventsController != null) {
      internalEventsController.setSectionTree(sectionTree);
    }

    final boolean shouldDisplayLoading = shouldDisplayLoading(loadingState, isEmpty);
    final boolean shouldDisplayEmpty = shouldDisplayEmpty(loadingState, isEmpty);
    final boolean shouldDisplayError = shouldDisplayError(loadingState, isEmpty);
    final boolean isErrorButNoErrorComponent = shouldDisplayError && (errorComponent == null);
    final boolean isEmptyButNoEmptyComponent = shouldDisplayEmpty && (emptyComponent == null);
    final boolean shouldHideComponent = isEmptyButNoEmptyComponent || isErrorButNoErrorComponent;

    if (shouldHideComponent) {
      return null;
    }

    final Component.Builder recyclerBuilder;

    final Recycler.Builder recycler =
        Recycler.create(c)
            .clipToPadding(clipToPadding)
            .leftPadding(leftPadding)
            .rightPadding(rightPadding)
            .topPadding(topPadding)
            .bottomPadding(bottomPadding)
            .clipChildren(clipChildren)
            .nestedScrollingEnabled(nestedScrollingEnabled)
            .scrollBarStyle(scrollBarStyle)
            .recyclerViewId(recyclerViewId)
            .recyclerEventsController(internalEventsController)
            .refreshHandler(
                disablePTR ? null : RecyclerCollectionComponent.onRefresh(c, sectionTree))
            .itemDecoration(itemDecoration)
            .canMeasure(canMeasureRecycler)
            .horizontalFadingEdgeEnabled(horizontalFadingEdgeEnabled)
            .verticalFadingEdgeEnabled(verticalFadingEdgeEnabled)
            .fadingEdgeLengthDip(fadingEdgeLength)
            .onScrollListener(new RecyclerCollectionOnScrollListener(internalEventsController))
            .onScrollListeners(onScrollListeners)
            .refreshProgressBarColor(refreshProgressBarColor)
            .snapHelper(snapHelper)
            .binder(binder);

    if (itemAnimator != null) {
      recycler.itemAnimator(itemAnimator);
    }
    recyclerBuilder = recycler;

    ComponentLayout.Builder recyclerLayoutBuilder = recyclerBuilder
        .withLayout()
        .flexShrink(0)
        .touchHandler(recyclerTouchEventHandler);

    if (!canMeasureRecycler) {
      recyclerLayoutBuilder = recyclerLayoutBuilder
          .positionType(ABSOLUTE)
          .positionPx(ALL, 0);
    }

    final ContainerBuilder containerBuilder = create(c)
        .flexShrink(0)
        .alignContent(FLEX_START)
        .child(recyclerLayoutBuilder);

    if (shouldDisplayLoading && loadingComponent != null) {
      containerBuilder.child(
          Layout.create(c, loadingComponent).flexShrink(0)
              .positionType(ABSOLUTE)
              .positionPx(ALL, 0));
    } else if (shouldDisplayEmpty) {
      containerBuilder.child(
          Layout.create(c, emptyComponent).flexShrink(0)
              .positionType(ABSOLUTE)
              .positionPx(ALL, 0));
    } else if (shouldDisplayError) {
      containerBuilder.child(
          Layout.create(c, errorComponent).flexShrink(0)
              .positionType(ABSOLUTE)
              .positionPx(ALL, 0));
    }

    return containerBuilder.build();
  }

  private static boolean shouldDisplayLoading(LoadingState loadingState, boolean isEmpty) {
    return isEmpty && (loadingState == INITIAL_LOAD || loadingState == LOADING);
  }

  private static boolean shouldDisplayEmpty(LoadingState loadingState, boolean isEmpty) {
    return isEmpty && loadingState == SUCCEEDED;
  }

  private static boolean shouldDisplayError(LoadingState loadingState, boolean isEmpty) {
    return isEmpty && loadingState == FAILED;
  }

  @OnCreateInitialState
  static <E extends Binder<RecyclerView> & Target> void createInitialState(
      final ComponentContext c,
      @Prop(optional = true) RecyclerConfiguration recyclerConfiguration,
      @Prop(optional = true) RecyclerCollectionEventsController eventsController,
      @Prop(optional = true) boolean asyncPropUpdates,
      @Prop(optional = true) boolean asyncStateUpdates,
      @Prop(optional = true) String sectionTreeTag,
      StateValue<SnapHelper> snapHelper,
      StateValue<SectionTree> sectionTree,
      StateValue<Binder<RecyclerView>> binder,
      StateValue<LoadingEvent.LoadingState> loadingState,
      StateValue<Boolean> isEmpty,
      StateValue<RecyclerCollectionEventsController> internalEventsController) {

    E targetBinder = recyclerConfiguration.buildTarget(c);

    final SectionContext sectionContext = new SectionContext(c);
    binder.set(targetBinder);
    snapHelper.set(recyclerConfiguration.getSnapHelper());

    final SectionTree sectionTreeInstance =
        SectionTree.create(sectionContext, targetBinder)
            .tag(sectionTreeTag)
            .asyncPropUpdates(asyncPropUpdates)
            .asyncStateUpdates(asyncStateUpdates)
            .build();
    sectionTree.set(sectionTreeInstance);

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

    isEmpty.set(true);
    loadingState.set(LoadingEvent.LoadingState.INITIAL_LOAD);

    internalEventsController.set(eventsController != null
        ? eventsController
        : new RecyclerCollectionEventsController());
  }

  @OnUpdateState
  static void updateLoadingState(
      StateValue<LoadingState> loadingState,
      @Param LoadingState currentLoadingState) {
    loadingState.set(currentLoadingState);
  }

  @OnUpdateState
  static void updateLoadingAndEmpty(
      StateValue<LoadingState> loadingState,
      StateValue<Boolean> isEmpty,
      @Param LoadingState currentLoadingState,
      @Param boolean empty) {
    isEmpty.set(empty);
    loadingState.set(currentLoadingState);
  }

  @OnEvent(PTRRefreshEvent.class)
  protected static void onRefresh(ComponentContext c, @Param SectionTree sectionTree) {
    sectionTree.refresh();
  }

  private static class RecyclerCollectionOnScrollListener extends OnScrollListener {

    private final RecyclerCollectionEventsController mEventsController;

    private RecyclerCollectionOnScrollListener(
        RecyclerCollectionEventsController eventsController) {
      mEventsController = eventsController;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
      super.onScrolled(recyclerView, dx, dy);

      final LinearLayoutManager linearLayoutManager =
          (LinearLayoutManager) recyclerView.getLayoutManager();
      final int firstCompletelyVisibleItemPosition =
          linearLayoutManager.findFirstCompletelyVisibleItemPosition();

      if (firstCompletelyVisibleItemPosition != -1) {
        // firstCompletelyVisibleItemPosition can be -1 in middle of the scroll, so
        // wait until it finishes to set the state.
        mEventsController.setFirstCompletelyVisibleItemPosition(firstCompletelyVisibleItemPosition);
      }
    }
  }

  private static class RecyclerCollectionLoadEventsHandler extends BaseLoadEventsHandler {

    private final LoadEventsHandler mDelegate;
    private final ComponentContext mComponentContext;
    private final RecyclerEventsController mRecyclerEventsController;
    private final boolean mIsEmpty;

    private RecyclerCollectionLoadEventsHandler(
        LoadEventsHandler delegate,
        ComponentContext c,
        RecyclerEventsController recyclerEventsController,
        boolean isEmpty) {
      mDelegate = delegate;
      mComponentContext = c;
      mRecyclerEventsController = recyclerEventsController;
      mIsEmpty = isEmpty;
    }

    @Override
    public void onLoadStarted(boolean empty) {
      if (mIsEmpty || empty) {
        RecyclerCollectionComponent.updateLoadingAndEmptyAsync(mComponentContext, LOADING, empty);
      } else {
        RecyclerCollectionComponent.lazyUpdateLoadingState(mComponentContext, LOADING);
      }

      if (mDelegate != null) {
        mDelegate.onLoadStarted(empty);
      }
    }

    @Override
    public void onLoadSucceeded(boolean empty) {
      if (mIsEmpty || empty) {
        RecyclerCollectionComponent.updateLoadingAndEmptyAsync(mComponentContext, SUCCEEDED, empty);
      } else {
        RecyclerCollectionComponent.lazyUpdateLoadingState(mComponentContext, SUCCEEDED);
      }

      mRecyclerEventsController.clearRefreshing();
      if (mDelegate != null) {
        mDelegate.onLoadSucceeded(empty);
      }
    }

    @Override
    public void onLoadFailed(boolean empty) {
      if (mIsEmpty || empty) {
        RecyclerCollectionComponent.updateLoadingAndEmptyAsync(mComponentContext, FAILED, empty);
      } else {
        RecyclerCollectionComponent.lazyUpdateLoadingState(mComponentContext, FAILED);
      }

      mRecyclerEventsController.clearRefreshing();
      if (mDelegate != null) {
        mDelegate.onLoadFailed(empty);
      }
    }

    @Override
    public void onInitialLoad() {
      if (mDelegate != null) {
        mDelegate.onInitialLoad();
      }
    }
  }
}
