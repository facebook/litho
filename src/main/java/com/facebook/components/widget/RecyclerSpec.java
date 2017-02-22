// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.widget;

import android.support.annotation.IdRes;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ItemAnimator;
import android.view.View;

import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentLayout;
import com.facebook.components.Diff;
import com.facebook.components.EventHandler;
import com.facebook.components.Output;
import com.facebook.components.Size;
import com.facebook.components.annotations.FromBind;
import com.facebook.components.annotations.FromPrepare;
import com.facebook.components.annotations.MountSpec;
import com.facebook.components.annotations.OnBind;
import com.facebook.components.annotations.OnBoundsDefined;
import com.facebook.components.annotations.OnCreateMountContent;
import com.facebook.components.annotations.OnMeasure;
import com.facebook.components.annotations.OnMount;
import com.facebook.components.annotations.OnPrepare;
import com.facebook.components.annotations.OnUnbind;
import com.facebook.components.annotations.OnUnmount;
import com.facebook.components.annotations.Prop;
import com.facebook.components.annotations.PropDefault;
import com.facebook.components.annotations.ShouldUpdate;

@MountSpec(canMountIncrementally = true, isPureRender = true, events = {PTRRefreshEvent.class})
class RecyclerSpec {
  @PropDefault static final int scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY;
  @PropDefault static final boolean hasFixedSize = true;
  @PropDefault static final ItemAnimator itemAnimator =
      new IncrementalMountDefaultItemAnimator();
  @PropDefault static final int recyclerViewId = View.NO_ID;

  @OnMeasure
  static void onMeasure(
      ComponentContext context,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size measureOutput) {
    throw new IllegalStateException("Recycler must have sizes spec set");
  }

  @OnBoundsDefined
  static void onBoundsDefined(
      ComponentContext context,
      ComponentLayout layout,
      @Prop RecyclerComponentBinder binder) {
    binder.setSize(
        layout.getWidth(),
        layout.getHeight());
  }

  @OnCreateMountContent
  static RecyclerViewWrapper onCreateMountContent(ComponentContext c) {
    return new RecyclerViewWrapper(c);
  }

  @OnPrepare
  static void onPrepare(
      ComponentContext c,
      @Prop(optional = true) final EventHandler refreshHandler,
      Output<OnRefreshListener> onRefreshListener) {
    if (refreshHandler != null) {
      onRefreshListener.set(new OnRefreshListener() {
        @Override
        public void onRefresh() {
          Recycler.dispatchPTRRefreshEvent(refreshHandler);
        }
      });
    }
  }

  @OnMount
  static void onMount(
      ComponentContext c,
      RecyclerViewWrapper recyclerViewWrapper,
      @Prop RecyclerComponentBinder binder,
      @Prop(optional = true) boolean hasFixedSize,
      @Prop(optional = true) boolean clipToPadding,
      @Prop(optional = true) int scrollBarStyle,
      @Prop(optional = true) @IdRes int recyclerViewId) {
    final RecyclerView recyclerView = recyclerViewWrapper.getRecyclerView();

    if (recyclerView == null) {
      throw new IllegalStateException(
          "RecyclerView not found, it should not be removed from SwipeRefreshLayout");
    }

    recyclerView.setHasFixedSize(hasFixedSize);
    recyclerView.setClipToPadding(clipToPadding);
    recyclerView.setScrollBarStyle(scrollBarStyle);
    // TODO (t14949498) determine if this is necessary
    recyclerView.setId(recyclerViewId);

    binder.mount(recyclerView);
  }

  @OnBind
  protected static void onBind(
      ComponentContext context,
      RecyclerViewWrapper recyclerViewWrapper,
      @Prop(optional = true) ItemAnimator itemAnimator,
      @Prop RecyclerComponentBinder binder,
      @Prop(optional = true) final RecyclerEventsController recyclerEventsController,
      @Prop(optional = true) RecyclerView.ItemDecoration itemDecoration,
      @Prop(optional = true) RecyclerView.OnScrollListener onScrollListener,
      @Prop(optional = true) boolean isRefreshing,
      @FromPrepare OnRefreshListener onRefreshListener,
      Output<ItemAnimator> oldAnimator) {

    recyclerViewWrapper.setRefreshing(isRefreshing);
    recyclerViewWrapper.setEnabled(onRefreshListener != null);
    recyclerViewWrapper.setOnRefreshListener(onRefreshListener);

    final RecyclerView recyclerView = recyclerViewWrapper.getRecyclerView();

    if (recyclerView == null) {
      throw new IllegalStateException(
          "RecyclerView not found, it should not be removed from SwipeRefreshLayout " +
              "before unmounting");
    }

    oldAnimator.set(recyclerView.getItemAnimator());
    if (itemAnimator != RecyclerSpec.itemAnimator) {
      recyclerView.setItemAnimator(itemAnimator);
    }

    if (itemDecoration != null) {
      recyclerView.addItemDecoration(itemDecoration);
    }

    if (onScrollListener != null) {
      recyclerView.addOnScrollListener(onScrollListener);
    }

    binder.bind(recyclerView);

    if (recyclerEventsController != null) {
      recyclerEventsController.setRecyclerView(recyclerView);
    }
  }

  @OnUnbind
  static void onUnbind(
      ComponentContext context,
      RecyclerViewWrapper recyclerViewWrapper,
      @Prop RecyclerComponentBinder binder,
      @Prop(optional =  true) RecyclerEventsController recyclerEventsController,
      @Prop(optional = true) RecyclerView.ItemDecoration itemDecoration,
      @Prop(optional = true) RecyclerView.OnScrollListener onScrollListener,
      @FromBind ItemAnimator oldAnimator) {
    final RecyclerView recyclerView = recyclerViewWrapper.getRecyclerView();

    if (recyclerView == null) {
      throw new IllegalStateException(
          "RecyclerView not found, it should not be removed from SwipeRefreshLayout " +
              "before unmounting");
    }

    recyclerView.setItemAnimator(oldAnimator);

    binder.unbind(recyclerView);

    if (recyclerEventsController != null) {
      recyclerEventsController.setRecyclerView(null);
    }

    if (itemDecoration != null) {
      recyclerView.removeItemDecoration(itemDecoration);
    }

    if (onScrollListener != null) {
      recyclerView.removeOnScrollListener(onScrollListener);
    }

    recyclerViewWrapper.setOnRefreshListener(null);
  }

  @OnUnmount
  static void onUnmount(
      ComponentContext context,
      RecyclerViewWrapper recyclerViewWrapper,
      @Prop RecyclerComponentBinder binder) {
    final RecyclerView recyclerView = recyclerViewWrapper.getRecyclerView();

    if (recyclerView == null) {
      throw new IllegalStateException(
          "RecyclerView not found, it should not be removed from SwipeRefreshLayout " +
              "before unmounting");
    }

    recyclerView.setId(RecyclerSpec.recyclerViewId);
    binder.unmount(recyclerView);
  }

  @ShouldUpdate(onMount = true)
  protected static boolean shouldUpdate(
      Diff<RecyclerComponentBinder> binder,
      Diff<Boolean> hasFixedSize,
      Diff<Boolean> clipToPadding,
      Diff<Integer> scrollBarStyle) {
    if (binder.getPrevious() != binder.getNext()) {
      return true;
    }

    if (!hasFixedSize.getPrevious().equals(hasFixedSize.getNext())) {
      return true;
    }

    if (!clipToPadding.getPrevious().equals(clipToPadding.getNext())) {
      return true;
    }

    if (!scrollBarStyle.getPrevious().equals(scrollBarStyle.getNext())) {
      return true;
    }

    return false;
  }
}
