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

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ItemAnimator;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;
import androidx.recyclerview.widget.SnapHelper;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Diff;
import com.facebook.litho.EventHandler;
import com.facebook.litho.Output;
import com.facebook.litho.Size;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.FromPrepare;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.OnUnbind;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.ShouldAlwaysRemeasure;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.annotations.State;
import java.util.List;

/**
 * Components that renders a {@link RecyclerView}.
 *
 * @uidocs https://fburl.com/Recycler:a3f7
 * @prop binder Binder for RecyclerView.
 * @prop refreshHandler Event handler for refresh event.
 * @prop hasFixedSize If set, makes RecyclerView not affected by adapter changes.
 * @prop clipToPadding Clip RecyclerView to its padding.
 * @prop clipChildren Clip RecyclerView children to their bounds.
 * @prop nestedScrollingEnabled Enables nested scrolling on the RecyclerView.
 * @prop itemDecoration Item decoration for the RecyclerView.
 * @prop refreshProgressBarBackgroundColor Color for progress background.
 * @prop refreshProgressBarColor Color for progress animation.
 * @prop recyclerViewId View ID for the RecyclerView.
 * @prop recyclerEventsController Controller to pass events from outside the component.
 * @prop onScrollListener Listener for RecyclerView's scroll events.
 */
@MountSpec(
    hasChildLithoViews = true,
    isPureRender = true,
    events = {PTRRefreshEvent.class})
class RecyclerSpec {

  @PropDefault static final int scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY;
  @PropDefault static final boolean hasFixedSize = true;
  @PropDefault static final boolean nestedScrollingEnabled = true;
  @PropDefault static final ItemAnimator itemAnimator = new NoUpdateItemAnimator();
  @PropDefault static final int recyclerViewId = View.NO_ID;
  @PropDefault static final int overScrollMode = View.OVER_SCROLL_ALWAYS;
  @PropDefault static final int refreshProgressBarColor = Color.BLACK;
  @PropDefault static final boolean clipToPadding = true;
  @PropDefault static final boolean clipChildren = true;
  @PropDefault static final int leftPadding = 0;
  @PropDefault static final int rightPadding = 0;
  @PropDefault static final int topPadding = 0;
  @PropDefault static final int bottomPadding = 0;
  @PropDefault static final boolean pullToRefresh = true;

  // This is the default value for refresh spinner background from
  // SwipeRefreshLayout.CIRCLE_BG_LIGHT which is unfortunately private.
  static final int DEFAULT_REFRESH_SPINNER_BACKGROUND_COLOR = 0xFFFAFAFA;

  @OnMeasure
  static void onMeasure(
      ComponentContext c,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size measureOutput,
      @Prop Binder<RecyclerView> binder) {

    binder.measure(
        measureOutput,
        widthSpec,
        heightSpec,
        (binder.canMeasure() || binder.isWrapContent()) ? Recycler.onRemeasure(c) : null);
  }

  @OnBoundsDefined
  static void onBoundsDefined(
      ComponentContext context, ComponentLayout layout, @Prop Binder<RecyclerView> binder) {
    binder.setSize(layout.getWidth(), layout.getHeight());
  }

  @OnCreateMountContent
  static SectionsRecyclerView onCreateMountContent(Context c) {
    return new SectionsRecyclerView(c, new LithoRecylerView(c));
  }

  @OnPrepare
  static void onPrepare(
      ComponentContext c,
      @Nullable @Prop(optional = true) final EventHandler refreshHandler,
      Output<OnRefreshListener> onRefreshListener) {
    if (refreshHandler != null) {
      onRefreshListener.set(
          new OnRefreshListener() {
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
      SectionsRecyclerView sectionsRecycler,
      @Prop Binder<RecyclerView> binder,
      @Prop(optional = true) boolean hasFixedSize,
      @Prop(optional = true) boolean clipToPadding,
      @Prop(optional = true) int leftPadding,
      @Prop(optional = true) int rightPadding,
      @Prop(optional = true) int topPadding,
      @Prop(optional = true) int bottomPadding,
      @Prop(optional = true, resType = ResType.COLOR) @Nullable
          Integer refreshProgressBarBackgroundColor,
      @Prop(optional = true, resType = ResType.COLOR) int refreshProgressBarColor,
      @Prop(optional = true) boolean clipChildren,
      @Prop(optional = true) boolean nestedScrollingEnabled,
      @Prop(optional = true) int scrollBarStyle,
      @Prop(optional = true) RecyclerView.ItemDecoration itemDecoration,
      @Prop(optional = true) boolean horizontalFadingEdgeEnabled,
      @Prop(optional = true) boolean verticalFadingEdgeEnabled,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) int fadingEdgeLength,
      @Prop(optional = true) @IdRes int recyclerViewId,
      @Prop(optional = true) int overScrollMode,
      @Prop(optional = true, isCommonProp = true) CharSequence contentDescription,
      @Prop(optional = true) ItemAnimator itemAnimator) {
    final RecyclerView recyclerView = sectionsRecycler.getRecyclerView();

    if (recyclerView == null) {
      throw new IllegalStateException(
          "RecyclerView not found, it should not be removed from SwipeRefreshLayout");
    }
    recyclerView.setContentDescription(contentDescription);
    recyclerView.setHasFixedSize(hasFixedSize);
    recyclerView.setClipToPadding(clipToPadding);
    sectionsRecycler.setClipToPadding(clipToPadding);
    ViewCompat.setPaddingRelative(
        recyclerView, leftPadding, topPadding, rightPadding, bottomPadding);
    recyclerView.setClipChildren(clipChildren);
    sectionsRecycler.setClipChildren(clipChildren);
    recyclerView.setNestedScrollingEnabled(nestedScrollingEnabled);
    sectionsRecycler.setNestedScrollingEnabled(nestedScrollingEnabled);
    recyclerView.setScrollBarStyle(scrollBarStyle);
    recyclerView.setHorizontalFadingEdgeEnabled(horizontalFadingEdgeEnabled);
    recyclerView.setVerticalFadingEdgeEnabled(verticalFadingEdgeEnabled);
    recyclerView.setFadingEdgeLength(fadingEdgeLength);
    // TODO (t14949498) determine if this is necessary
    recyclerView.setId(recyclerViewId);
    recyclerView.setOverScrollMode(overScrollMode);
    if (refreshProgressBarBackgroundColor != null) {
      sectionsRecycler.setProgressBackgroundColorSchemeColor(refreshProgressBarBackgroundColor);
    }
    sectionsRecycler.setColorSchemeColors(refreshProgressBarColor);

    if (itemDecoration != null) {
      recyclerView.addItemDecoration(itemDecoration);
    }

    sectionsRecycler.setItemAnimator(
        itemAnimator != RecyclerSpec.itemAnimator ? itemAnimator : new NoUpdateItemAnimator());

    binder.mount(recyclerView);
  }

  @OnBind
  protected static void onBind(
      ComponentContext context,
      SectionsRecyclerView sectionsRecycler,
      @Prop Binder<RecyclerView> binder,
      @Prop(optional = true) final RecyclerEventsController recyclerEventsController,
      @Prop(optional = true, varArg = "onScrollListener") List<OnScrollListener> onScrollListeners,
      @Prop(optional = true) SnapHelper snapHelper,
      @Prop(optional = true) boolean pullToRefresh,
      @Prop(optional = true) LithoRecylerView.TouchInterceptor touchInterceptor,
      @FromPrepare OnRefreshListener onRefreshListener) {

    // contentDescription should be set on the recyclerView itself, and not the sectionsRecycler.
    sectionsRecycler.setContentDescription(null);

    sectionsRecycler.setEnabled(pullToRefresh && onRefreshListener != null);
    sectionsRecycler.setOnRefreshListener(onRefreshListener);

    final LithoRecylerView recyclerView = (LithoRecylerView) sectionsRecycler.getRecyclerView();

    if (recyclerView == null) {
      throw new IllegalStateException(
          "RecyclerView not found, it should not be removed from SwipeRefreshLayout "
              + "before unmounting");
    }

    if (onScrollListeners != null) {
      for (OnScrollListener onScrollListener : onScrollListeners) {
        recyclerView.addOnScrollListener(onScrollListener);
      }
    }

    if (touchInterceptor != null) {
      recyclerView.setTouchInterceptor(touchInterceptor);
    }

    // We cannot detach the snap helper in unbind, so it may be possible for it to get
    // attached twice which causes SnapHelper to raise an exception.
    if (snapHelper != null && recyclerView.getOnFlingListener() == null) {
      snapHelper.attachToRecyclerView(recyclerView);
    }

    binder.bind(recyclerView);

    if (recyclerEventsController != null) {
      recyclerEventsController.setSectionsRecyclerView(sectionsRecycler);
    }

    if (sectionsRecycler.hasBeenDetachedFromWindow()) {
      recyclerView.requestLayout();
      sectionsRecycler.setHasBeenDetachedFromWindow(false);
    }
  }

  @OnUnbind
  static void onUnbind(
      ComponentContext context,
      SectionsRecyclerView sectionsRecycler,
      @Prop Binder<RecyclerView> binder,
      @Prop(optional = true) RecyclerEventsController recyclerEventsController,
      @Prop(optional = true, varArg = "onScrollListener")
          List<OnScrollListener> onScrollListeners) {
    final LithoRecylerView recyclerView = (LithoRecylerView) sectionsRecycler.getRecyclerView();

    if (recyclerView == null) {
      throw new IllegalStateException(
          "RecyclerView not found, it should not be removed from SwipeRefreshLayout "
              + "before unmounting");
    }

    binder.unbind(recyclerView);

    if (recyclerEventsController != null) {
      recyclerEventsController.setSectionsRecyclerView(null);
    }

    if (onScrollListeners != null) {
      for (OnScrollListener onScrollListener : onScrollListeners) {
        recyclerView.removeOnScrollListener(onScrollListener);
      }
    }

    recyclerView.setTouchInterceptor(null);

    sectionsRecycler.setOnRefreshListener(null);
  }

  @OnUnmount
  static void onUnmount(
      ComponentContext context,
      SectionsRecyclerView sectionsRecycler,
      @Prop Binder<RecyclerView> binder,
      @Prop(optional = true) RecyclerView.ItemDecoration itemDecoration,
      @Prop(optional = true, resType = ResType.COLOR) @Nullable
          Integer refreshProgressBarBackgroundColor,
      @Prop(optional = true) SnapHelper snapHelper) {
    final RecyclerView recyclerView = sectionsRecycler.getRecyclerView();

    if (recyclerView == null) {
      throw new IllegalStateException(
          "RecyclerView not found, it should not be removed from SwipeRefreshLayout "
              + "before unmounting");
    }

    recyclerView.setId(RecyclerSpec.recyclerViewId);

    if (refreshProgressBarBackgroundColor != null) {
      sectionsRecycler.setProgressBackgroundColorSchemeColor(
          DEFAULT_REFRESH_SPINNER_BACKGROUND_COLOR);
    }

    if (itemDecoration != null) {
      recyclerView.removeItemDecoration(itemDecoration);
    }

    binder.unmount(recyclerView);

    if (snapHelper != null) {
      snapHelper.attachToRecyclerView(null);
    }

    sectionsRecycler.resetItemAnimator();
  }

  @ShouldUpdate(onMount = true)
  protected static boolean shouldUpdate(
      @Prop Diff<Binder<RecyclerView>> binder,
      @Prop(optional = true) Diff<Boolean> hasFixedSize,
      @Prop(optional = true) Diff<Boolean> clipToPadding,
      @Prop(optional = true) Diff<Integer> leftPadding,
      @Prop(optional = true) Diff<Integer> rightPadding,
      @Prop(optional = true) Diff<Integer> topPadding,
      @Prop(optional = true) Diff<Integer> bottomPadding,
      @Prop(optional = true, resType = ResType.COLOR)
          Diff<Integer> refreshProgressBarBackgroundColor,
      @Prop(optional = true, resType = ResType.COLOR) Diff<Integer> refreshProgressBarColor,
      @Prop(optional = true) Diff<Boolean> clipChildren,
      @Prop(optional = true) Diff<Integer> scrollBarStyle,
      @Prop(optional = true) Diff<RecyclerView.ItemDecoration> itemDecoration,
      @Prop(optional = true) Diff<Boolean> horizontalFadingEdgeEnabled,
      @Prop(optional = true) Diff<Boolean> verticalFadingEdgeEnabled,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) Diff<Integer> fadingEdgeLength,
      @Prop(optional = true) Diff<ItemAnimator> itemAnimator,
      @State Diff<Integer> measureVersion) {

    if (measureVersion.getPrevious().intValue() != measureVersion.getNext().intValue()) {
      return true;
    }

    if (binder.getPrevious() != binder.getNext()) {
      return true;
    }

    if (!hasFixedSize.getPrevious().equals(hasFixedSize.getNext())) {
      return true;
    }

    if (!clipToPadding.getPrevious().equals(clipToPadding.getNext())) {
      return true;
    }

    if (!leftPadding.getPrevious().equals(leftPadding.getNext())) {
      return true;
    }

    if (!rightPadding.getPrevious().equals(rightPadding.getNext())) {
      return true;
    }

    if (!topPadding.getPrevious().equals(topPadding.getNext())) {
      return true;
    }

    if (!bottomPadding.getPrevious().equals(bottomPadding.getNext())) {
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

    Integer previousRefreshBgColor = refreshProgressBarBackgroundColor.getPrevious();
    Integer nextRefreshBgColor = refreshProgressBarBackgroundColor.getNext();
    if (previousRefreshBgColor == null
        ? nextRefreshBgColor != null
        : !previousRefreshBgColor.equals(nextRefreshBgColor)) {
      return true;
    }

    if (!refreshProgressBarColor.getPrevious().equals(refreshProgressBarColor.getNext())) {
      return true;
    }

    final ItemAnimator previousItemAnimator = itemAnimator.getPrevious();
    final ItemAnimator nextItemAnimator = itemAnimator.getNext();

    if (previousItemAnimator == null
        ? nextItemAnimator != null
        : !previousItemAnimator.getClass().equals(nextItemAnimator.getClass())) {
      return true;
    }

    final RecyclerView.ItemDecoration previous = itemDecoration.getPrevious();
    final RecyclerView.ItemDecoration next = itemDecoration.getNext();
    final boolean itemDecorationIsEqual =
        (previous == null) ? (next == null) : previous.equals(next);

    return !itemDecorationIsEqual;
  }

  @OnEvent(ReMeasureEvent.class)
  protected static void onRemeasure(ComponentContext c, @State int measureVersion) {
    Recycler.onUpdateMeasureAsync(c, measureVersion + 1);
  }

  @OnCreateInitialState
  protected static void onCreateInitialState(
      ComponentContext c, StateValue<Integer> measureVersion) {
    measureVersion.set(0);
  }

  @OnUpdateState
  protected static void onUpdateMeasure(@Param int measureVer, StateValue<Integer> measureVersion) {
    // We don't really need to update a state here. This state update is only really used to force
    // a re-layout on the tree containing this Recycler.
    measureVersion.set(measureVer);
  }

  @ShouldAlwaysRemeasure
  protected static boolean shouldAlwaysRemeasure(@Prop Binder<RecyclerView> binder) {
    return binder.isWrapContent();
  }

  public static class NoUpdateItemAnimator extends DefaultItemAnimator {

    public NoUpdateItemAnimator() {
      super();
      setSupportsChangeAnimations(false);
    }
  }
}
