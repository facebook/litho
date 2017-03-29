/**
 * Copyright (c) 2014-present, Facebook, Inc.
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
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.OnUnbind;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.ShouldUpdate;

@MountSpec(canMountIncrementally = true, isPureRender = true, events = {PTRRefreshEvent.class})
class RecyclerSpec {
  @PropDefault static final int scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY;
  @PropDefault static final boolean hasFixedSize = true;
  @PropDefault static final boolean nestedScrollingEnabled = true;
  @PropDefault static final ItemAnimator itemAnimator = new NoUpdateItemAnimator();
  @PropDefault static final int recyclerViewId = View.NO_ID;
  @PropDefault static final int refreshProgressBarColor = Color.BLACK;

  @OnMeasure
  static void onMeasure(
      ComponentContext context,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size measureOutput,
      @Prop Binder<RecyclerView> binder) {
    binder.measure(measureOutput, widthSpec, heightSpec);
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
      @Prop(optional = true) boolean nestedScrollingEnabled,
      @Prop(optional = true) int scrollBarStyle,
      @Prop(optional = true) RecyclerView.ItemDecoration itemDecoration,
      @Prop(optional = true, resType = ResType.COLOR) int refreshProgressBarColor,
      @Prop(optional = true) @IdRes int recyclerViewId) {
    final RecyclerView recyclerView = recyclerViewWrapper.getRecyclerView();

    if (recyclerView == null) {
      throw new IllegalStateException(
          "RecyclerView not found, it should not be removed from SwipeRefreshLayout");
    }

    recyclerViewWrapper.setColorSchemeColors(refreshProgressBarColor);
    recyclerView.setHasFixedSize(hasFixedSize);
    recyclerView.setClipToPadding(clipToPadding);
    recyclerView.setNestedScrollingEnabled(nestedScrollingEnabled);
    recyclerViewWrapper.setNestedScrollingEnabled(nestedScrollingEnabled);
    recyclerView.setScrollBarStyle(scrollBarStyle);
    // TODO (t14949498) determine if this is necessary
    recyclerView.setId(recyclerViewId);

    if (itemDecoration != null) {
      recyclerView.addItemDecoration(itemDecoration);
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
      @Prop(optional = true) RecyclerView.OnScrollListener onScrollListener,
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

    if (onScrollListener != null) {
      recyclerView.addOnScrollListener(onScrollListener);
    }

    binder.bind(recyclerView);

    if (recyclerEventsController != null) {
      recyclerEventsController.setRecyclerViewWrapper(recyclerViewWrapper);
    }

    if (recyclerViewWrapper.hasBeenDetachedFromWindow()) {
      recyclerView.requestLayout();
      recyclerViewWrapper.setHasBeenDetachedFromWindow(false);
    }
