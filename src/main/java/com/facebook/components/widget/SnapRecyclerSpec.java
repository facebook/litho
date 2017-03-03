// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.widget;

    import android.support.v7.widget.RecyclerView;
    import android.support.v7.widget.RecyclerView.ItemAnimator;
    import android.view.View;

    import com.facebook.components.ComponentContext;
    import com.facebook.components.ComponentLayout;
    import com.facebook.components.Diff;
    import com.facebook.components.Output;
    import com.facebook.components.Size;
    import com.facebook.components.annotations.FromBind;
    import com.facebook.components.annotations.MountSpec;
    import com.facebook.components.annotations.OnBind;
    import com.facebook.components.annotations.OnBoundsDefined;
    import com.facebook.components.annotations.OnCreateMountContent;
    import com.facebook.components.annotations.OnMeasure;
    import com.facebook.components.annotations.OnMount;
    import com.facebook.components.annotations.OnUnbind;
    import com.facebook.components.annotations.OnUnmount;
    import com.facebook.components.annotations.Prop;
    import com.facebook.components.annotations.PropDefault;
    import com.facebook.components.annotations.ShouldUpdate;
    import com.facebook.widget.snaprecyclerview.SnapRecyclerView;

/**
 * A Component that mounts a {@link SnapRecyclerView}.
 */
@MountSpec(canMountIncrementally = true, isPureRender = true)
class SnapRecyclerSpec {
  @PropDefault protected static final int scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY;
  @PropDefault protected static final boolean hasFixedSize = true;

  @OnMeasure
  protected static void onMeasure(
      ComponentContext context,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size measureOutput,
      @Prop Binder<RecyclerView> binder) {
    binder.measure(measureOutput, widthSpec, heightSpec);
  }

  @OnBoundsDefined
  protected static void onBoundsDefined(
      ComponentContext context,
      ComponentLayout layout,
      @Prop Binder<RecyclerView> binder) {
    binder.setSize(
        layout.getWidth(),
        layout.getHeight());
  }

  @OnCreateMountContent
  protected static SnapRecyclerView onCreateMountContent(ComponentContext c) {
    final SnapRecyclerView snapRecyclerView = new SnapRecyclerView(c);
    snapRecyclerView.setSnappingEnabled(true);

    return snapRecyclerView;
  }

  @OnMount
  protected static void onMount(
      ComponentContext c,
      SnapRecyclerView snapRecyclerView,
      @Prop Binder<RecyclerView> binder,
      @Prop(optional = true) boolean hasFixedSize,
      @Prop(optional = true) boolean clipToPadding,
      @Prop(optional = true) int scrollBarStyle,
      @Prop(optional = true) RecyclerView.ItemDecoration itemDecoration) {
    snapRecyclerView.setHasFixedSize(hasFixedSize);
    snapRecyclerView.setClipToPadding(clipToPadding);
    snapRecyclerView.setScrollBarStyle(scrollBarStyle);

    if (itemDecoration != null) {
      snapRecyclerView.addItemDecoration(itemDecoration);
    }

    binder.mount(snapRecyclerView);
  }

  @OnBind
  protected static void onBind(
      ComponentContext context,
      SnapRecyclerView snapRecyclerView,
      @Prop(optional = true) ItemAnimator itemAnimator,
      @Prop Binder<RecyclerView> binder,
      @Prop SnapRecyclerView.SnapDelegate snapDelegate,
      @Prop(optional =  true) final RecyclerEventsController recyclerEventsController,
      @Prop(optional = true) RecyclerView.OnScrollListener onScrollListener,
      Output<ItemAnimator> oldAnimator) {
    oldAnimator.set(snapRecyclerView.getItemAnimator());
    snapRecyclerView.setItemAnimator(itemAnimator);

    if (onScrollListener != null) {
      snapRecyclerView.addOnScrollListener(onScrollListener);
    }

    binder.bind(snapRecyclerView);

    if (recyclerEventsController != null) {
      recyclerEventsController.setRecyclerView(snapRecyclerView);
    }
    snapRecyclerView.setSnapDelegate(snapDelegate);
  }

  @OnUnbind
  protected static void onUnbind(
      ComponentContext context,
      SnapRecyclerView snapRecyclerView,
      @Prop Binder<RecyclerView> binder,
      @Prop(optional =  true) RecyclerEventsController recyclerEventsController,
      @Prop(optional = true) RecyclerView.OnScrollListener onScrollListener,
      @FromBind ItemAnimator oldAnimator) {
    snapRecyclerView.setItemAnimator(oldAnimator);

    binder.unbind(snapRecyclerView);

    if (recyclerEventsController != null) {
      recyclerEventsController.setRecyclerView(null);
    }

    if (onScrollListener != null) {
      snapRecyclerView.removeOnScrollListener(onScrollListener);
    }
    snapRecyclerView.setSnapDelegate(null);
  }

  @OnUnmount
  protected static void onUnmount(
      ComponentContext context,
      SnapRecyclerView snapRecyclerView,
      @Prop Binder<RecyclerView> binder,
      @Prop(optional = true) RecyclerView.ItemDecoration itemDecoration) {
    binder.unmount(snapRecyclerView);

    if (itemDecoration != null) {
      snapRecyclerView.removeItemDecoration(itemDecoration);
    }
  }

  @ShouldUpdate(onMount = true)
  protected static boolean shouldUpdate(
      Diff<Binder<RecyclerView>> binder,
      Diff<Boolean> hasFixedSize,
      Diff<Boolean> clipToPadding,
      Diff<Integer> scrollBarStyle,
      Diff<RecyclerView.ItemDecoration> itemDecoration) {
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

    final RecyclerView.ItemDecoration previous = itemDecoration.getPrevious();
    final RecyclerView.ItemDecoration next = itemDecoration.getNext();
    final boolean itemDecorationIsEqual =
        (previous == null) ? (next == null) : previous.equals(next);

    return !itemDecorationIsEqual;
  }
}
