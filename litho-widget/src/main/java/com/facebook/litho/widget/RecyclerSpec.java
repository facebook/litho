/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.graphics.Color;
import android.support.annotation.IdRes;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ItemAnimator;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.support.v7.widget.SnapHelper;
import android.view.View;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Diff;
import com.facebook.litho.EventHandler;
import com.facebook.litho.Output;
import com.facebook.litho.Size;
import com.facebook.litho.annotations.FromBind;
import com.facebook.litho.annotations.FromPrepare;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.OnUnbind;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.ShouldUpdate;
import java.util.List;

/**
 * Components that renders a {@link RecyclerView}.
 *
 * @uidocs
 * @prop binder Binder for RecyclerView.
 * @prop refreshHandler Event handler for refresh event.
 * @prop hasFixedSize If set, makes RecyclerView not affected by adapter changes.
 * @prop clipToPadding Clip RecyclerView to its padding.
 * @prop clipChildren Clip RecyclerView children to their bounds.
 * @prop nestedScrollingEnabled Enables nested scrolling on the RecyclerView.
 * @prop itemDecoration Item decoration for the RecyclerView.
 * @prop refreshProgressBarColor Color for progress animation.
 * @prop recyclerViewId View ID for the RecyclerView.
 * @prop recyclerEventsController Controller to pass events from outside the component.
 * @prop onScrollListener Listener for RecyclerView's scroll events.
 */
@MountSpec(canMountIncrementally = true, isPureRender = true, events = {PTRRefreshEvent.class})
class RecyclerSpec {
  @PropDefault static final int scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY;
  @PropDefault static final boolean hasFixedSize = true;
  @PropDefault static final boolean nestedScrollingEnabled = true;
  @PropDefault static final ItemAnimator itemAnimator = new NoUpdateItemAnimator();
  @PropDefault static final int recyclerViewId = View.NO_ID;
  @PropDefault static final int refreshProgressBarColor = Color.BLACK;
  @PropDefault static final boolean clipToPadding = true;
  @PropDefault static final boolean clipChildren = true;

  @OnMeasure
  static void onMeasure(
      ComponentContext c,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size measureOutput,
      @Prop Binder<RecyclerView> binder,
      @Prop(optional = true) boolean canMeasure) {
    binder.measure(
        measureOutput,
        widthSpec,
        heightSpec,
        canMeasure ? Recycler.onRemeasure(c) : null);
  }

  @OnBoundsDefined
  static void onBoundsDefined(
      ComponentContext context,
      ComponentLayout layout,
      @Prop Binder<RecyclerView> binder) {
    binder.setSize(
        layout.getWidth(),
        layout.getHeight());
  }

  @OnCreateMountContent
  static RecyclerViewWrapper onCreateMountContent(ComponentContext c) {
    return new RecyclerViewWrapper(c, new RecyclerView(c));
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
      @Prop Binder<RecyclerView> binder,
      @Prop(optional = true) boolean hasFixedSize,
      @Prop(optional = true) boolean clipToPadding,
      @Prop(optional = true) boolean clipChildren,
      @Prop(optional = true) boolean nestedScrollingEnabled,
      @Prop(optional = true) int scrollBarStyle,
      @Prop(optional = true) RecyclerView.ItemDecoration itemDecoration,
      @Prop(optional = true, resType = ResType.COLOR) int refreshProgressBarColor,
      @Prop(optional = true) boolean horizontalFadingEdgeEnabled,
      @Prop(optional = true) boolean verticalFadingEdgeEnabled,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) int fadingEdgeLength,
      @Prop(optional = true) @IdRes int recyclerViewId,
      @Prop(optional = true) SnapHelper snapHelper) {
    final RecyclerView recyclerView = recyclerViewWrapper.getRecyclerView();

    if (recyclerView == null) {
      throw new IllegalStateException(
          "RecyclerView not found, it should not be removed from SwipeRefreshLayout");
    }

    recyclerViewWrapper.setColorSchemeColors(refreshProgressBarColor);
    recyclerView.setHasFixedSize(hasFixedSize);
    recyclerView.setClipToPadding(clipToPadding);
    recyclerViewWrapper.setClipToPadding(clipToPadding);
    recyclerView.setClipChildren(clipChildren);
    recyclerViewWrapper.setClipChildren(clipChildren);
    recyclerView.setNestedScrollingEnabled(nestedScrollingEnabled);
    recyclerViewWrapper.setNestedScrollingEnabled(nestedScrollingEnabled);
    recyclerView.setScrollBarStyle(scrollBarStyle);
    recyclerView.setHorizontalFadingEdgeEnabled(horizontalFadingEdgeEnabled);
    recyclerView.setVerticalFadingEdgeEnabled(verticalFadingEdgeEnabled);
    recyclerView.setFadingEdgeLength(fadingEdgeLength);
    // TODO (t14949498) determine if this is necessary
    recyclerView.setId(recyclerViewId);

    if (itemDecoration != null) {
      recyclerView.addItemDecoration(itemDecoration);
    }

    if (snapHelper != null) {
      snapHelper.attachToRecyclerView(recyclerView);
    }

    binder.mount(recyclerView);
  }

  @OnBind
  protected static void onBind(
      ComponentContext context,
      RecyclerViewWrapper recyclerViewWrapper,
      @Prop(optional = true) ItemAnimator itemAnimator,
      @Prop Binder<RecyclerView> binder,
      @Prop(optional = true) final RecyclerEventsController recyclerEventsController,
      @Prop(optional = true, varArg = "onScrollListener") List<OnScrollListener> onScrollListeners,
      @FromPrepare OnRefreshListener onRefreshListener,
      Output<ItemAnimator> oldAnimator) {

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
    } else {
      recyclerView.setItemAnimator(new NoUpdateItemAnimator());
    }

    if (onScrollListeners != null) {
      for (OnScrollListener onScrollListener : onScrollListeners) {
        recyclerView.addOnScrollListener(onScrollListener);
      }
    }

    binder.bind(recyclerView);

    if (recyclerEventsController != null) {
      recyclerEventsController.setRecyclerViewWrapper(recyclerViewWrapper);
    }

    if (recyclerViewWrapper.hasBeenDetachedFromWindow()) {
      recyclerView.requestLayout();
      recyclerViewWrapper.setHasBeenDetachedFromWindow(false);
    }
  }

  @OnUnbind
  static void onUnbind(
      ComponentContext context,
      RecyclerViewWrapper recyclerViewWrapper,
      @Prop Binder<RecyclerView> binder,
      @Prop(optional =  true) RecyclerEventsController recyclerEventsController,
      @Prop(optional = true, varArg = "onScrollListener") List<OnScrollListener> onScrollListeners,
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
      recyclerEventsController.setRecyclerViewWrapper(null);
    }

    if (onScrollListeners != null) {
      for (OnScrollListener onScrollListener : onScrollListeners) {
        recyclerView.removeOnScrollListener(onScrollListener);
      }
    }

    recyclerViewWrapper.setOnRefreshListener(null);
  }

  @OnUnmount
  static void onUnmount(
      ComponentContext context,
      RecyclerViewWrapper recyclerViewWrapper,
      @Prop Binder<RecyclerView> binder,
      @Prop(optional = true) RecyclerView.ItemDecoration itemDecoration,
      @Prop(optional = true) SnapHelper snapHelper) {
    final RecyclerView recyclerView = recyclerViewWrapper.getRecyclerView();

    if (recyclerView == null) {
      throw new IllegalStateException(
          "RecyclerView not found, it should not be removed from SwipeRefreshLayout " +
              "before unmounting");
    }

    recyclerView.setId(RecyclerSpec.recyclerViewId);

    if (itemDecoration != null) {
      recyclerView.removeItemDecoration(itemDecoration);
    }

    if (snapHelper != null) {
      snapHelper.attachToRecyclerView(null);
    }

    binder.unmount(recyclerView);
  }

  @ShouldUpdate(onMount = true)
  protected static boolean shouldUpdate(
      Diff<Binder<RecyclerView>> binder,
      Diff<Boolean> hasFixedSize,
      Diff<Boolean> clipToPadding,
      Diff<Boolean> clipChildren,
      Diff<Integer> scrollBarStyle,
      Diff<RecyclerView.ItemDecoration> itemDecoration,
      Diff<Boolean> horizontalFadingEdgeEnabled,
      Diff<Boolean> verticalFadingEdgeEnabled,
      Diff<Integer> fadingEdgeLength) {
    if (binder.getPrevious() != binder.getNext()) {
      return true;
    }

    if (!hasFixedSize.getPrevious().equals(hasFixedSize.getNext())) {
      return true;
    }

    if (!clipToPadding.getPrevious().equals(clipToPadding.getNext())) {
      return true;
    }

    if (!clipChildren.getPrevious().equals(clipChildren.getNext())) {
      return true;
    }

    if (!scrollBarStyle.getPrevious().equals(scrollBarStyle.getNext())) {
      return true;
    }

    if (!horizontalFadingEdgeEnabled.getPrevious().equals(horizontalFadingEdgeEnabled.getNext())) {
      return true;
    }

    if (!verticalFadingEdgeEnabled.getPrevious().equals(verticalFadingEdgeEnabled.getNext())) {
      return true;
    }

    if (!fadingEdgeLength.getPrevious().equals(fadingEdgeLength.getNext())) {
      return true;
    }

    final RecyclerView.ItemDecoration previous = itemDecoration.getPrevious();
    final RecyclerView.ItemDecoration next = itemDecoration.getNext();
    final boolean itemDecorationIsEqual =
        (previous == null) ? (next == null) : previous.equals(next);

    return !itemDecorationIsEqual;
  }

  @OnEvent(ReMeasureEvent.class)
  protected static void onRemeasure(ComponentContext c) {
    Recycler.onUpdateMeasure(c);
  }

  @OnUpdateState
  protected static void onUpdateMeasure() {
    // We don't really need to update a state here. This state update is only really used to force
    // a re-layout on the tree containing this Recycler.
  }

  public static class NoUpdateItemAnimator extends DefaultItemAnimator {
    public NoUpdateItemAnimator() {
      super();
      setSupportsChangeAnimations(false);
    }
  }
}
