// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.util.Pools.SynchronizedPool;
import android.widget.HorizontalScrollView;

import com.facebook.R;
import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentTree;
import com.facebook.components.Component;
import com.facebook.components.ComponentLayout;
import com.facebook.components.ComponentView;
import com.facebook.components.Output;
import com.facebook.components.SizeSpec;
import com.facebook.components.annotations.OnCreateMountContent;
import com.facebook.components.annotations.OnLoadStyle;
import com.facebook.components.annotations.PropDefault;
import com.facebook.components.annotations.FromBoundsDefined;
import com.facebook.components.annotations.Prop;
import com.facebook.components.annotations.FromMeasure;
import com.facebook.components.annotations.FromPrepare;
import com.facebook.components.annotations.MountSpec;
import com.facebook.components.annotations.OnBoundsDefined;
import com.facebook.components.annotations.OnMeasure;
import com.facebook.components.annotations.OnMount;
import com.facebook.components.annotations.OnPrepare;
import com.facebook.components.annotations.OnUnmount;
import com.facebook.components.annotations.ResType;
import com.facebook.components.Size;

import static com.facebook.components.SizeSpec.EXACTLY;
import static com.facebook.components.SizeSpec.UNSPECIFIED;

/**
 * A component that wraps another component and allow it to be horizontally scrollable. It's
 * analogous to a {@link android.widget.HorizontalScrollView}.
 */
@MountSpec(canMountIncrementally = true)
class HorizontalScrollSpec {

  @PropDefault static final boolean scrollbarEnabled = true;

  private static final SynchronizedPool<Size> sSizePool =
      new SynchronizedPool<>(2);

  @OnLoadStyle
  static void onLoadStyle(
      ComponentContext c,
      Output<Boolean> scrollbarEnabled) {

    final TypedArray a = c.obtainStyledAttributes(
        R.styleable.HorizontalScroll,
        0);

    for (int i = 0, size = a.getIndexCount(); i < size; i++) {
      final int attr = a.getIndex(i);

      if (attr == R.styleable.HorizontalScroll_android_scrollbars) {
        scrollbarEnabled.set(a.getInt(attr, 0) != 0);
      }
    }

    a.recycle();
  }

  @OnPrepare
  static void onPrepare(
      ComponentContext context,
      @Prop Component<?> contentProps,
      Output<ComponentTree> contentComponent) {
    contentComponent.set(
        ComponentTree.create(context, contentProps)
            .incrementalMount(true)
            .build());
  }

  @OnMeasure
  static void onMeasure(
      ComponentContext context,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      @FromPrepare ComponentTree contentComponent,
      Output<Integer> measuredComponentWidth,
      Output<Integer> measuredComponentHeight) {

    final int measuredWidth;
    final int measuredHeight;

    Size contentSize = acquireSize();

    // Measure the component with undefined width spec, as the contents of the
    // hscroll have unlimited horizontal space.
    contentComponent.setSizeSpec(
        SizeSpec.makeSizeSpec(0, UNSPECIFIED),
        heightSpec,
        contentSize);

    measuredWidth = contentSize.width;
    measuredHeight = contentSize.height;

    releaseSize(contentSize);
    contentSize = null;

    measuredComponentWidth.set(measuredWidth);
    measuredComponentHeight.set(measuredHeight);

    // If size constraints were not explicitly defined, just fallback to the
    // component dimensions instead.
    size.width = SizeSpec.getMode(widthSpec) == UNSPECIFIED
        ? measuredWidth
        : SizeSpec.getSize(widthSpec);
    size.height = measuredHeight;
  }

  @OnBoundsDefined
  static void onBoundsDefined(
      ComponentContext context,
      ComponentLayout layout,
      @FromPrepare ComponentTree contentComponent,
      @FromMeasure Integer measuredComponentWidth,
      @FromMeasure Integer measuredComponentHeight,
      Output<Integer> componentWidth,
      Output<Integer> componentHeight) {

    // If onMeasure() has been called, this means the content component already
    // has a defined size, no need to calculate it again.
    if (measuredComponentWidth != null && measuredComponentHeight != null) {
      componentWidth.set(measuredComponentWidth);
      componentHeight.set(measuredComponentHeight);
    } else {
      final int measuredWidth;
      final int measuredHeight;

      Size contentSize = acquireSize();
      contentComponent.setSizeSpec(
          SizeSpec.makeSizeSpec(0, UNSPECIFIED),
          SizeSpec.makeSizeSpec(layout.getHeight(), EXACTLY),
          contentSize);

      measuredWidth = contentSize.width;
      measuredHeight = contentSize.height;

      releaseSize(contentSize);
      contentSize = null;

      componentWidth.set(measuredWidth);
      componentHeight.set(measuredHeight);
    }
  }

  @OnCreateMountContent
  static HorizontalScrollComponentView onCreateMountContent(ComponentContext c) {
    return new HorizontalScrollComponentView(c);
  }

  @OnMount
  static void onMount(
      ComponentContext context,
      HorizontalScrollComponentView horizontalScrollComponentView,
      @Prop(optional = true, resType = ResType.BOOL) boolean scrollbarEnabled,
      @FromPrepare ComponentTree contentComponent,
      @FromBoundsDefined int componentWidth,
      @FromBoundsDefined int componentHeight) {

    horizontalScrollComponentView.setHorizontalScrollBarEnabled(scrollbarEnabled);
    horizontalScrollComponentView.mount(contentComponent, componentWidth, componentHeight);
  }

  @OnUnmount
  static void onUnmount(
      ComponentContext context,
      HorizontalScrollComponentView mountedView) {
    mountedView.unmount();
  }

  static class HorizontalScrollComponentView extends HorizontalScrollView {
    private final ComponentView mComponentView;

    private int mComponentWidth;
    private int mComponentHeight;

    public HorizontalScrollComponentView(Context context) {
      super(context);
      mComponentView = new ComponentView(context);
      addView(mComponentView);
    }

    @Override
    protected void onScrollChanged(int left, int top, int oldLeft, int oldTop) {
      super.onScrollChanged(left, top, oldLeft, oldTop);

      // Visible area changed, perform incremental mount.
      incrementalMount();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
      // The hosting component view always matches the component size. This will
      // ensure that there will never be a size-mismatch between the view and the
      // component-based content, which would trigger a layout pass in the
      // UI thread.
      mComponentView.measure(
          MeasureSpec.makeMeasureSpec(mComponentWidth, MeasureSpec.EXACTLY),
          MeasureSpec.makeMeasureSpec(mComponentHeight, MeasureSpec.EXACTLY));

      // The mounted view always gets exact dimensions from the framework.
      setMeasuredDimension(
          MeasureSpec.getSize(widthMeasureSpec),
          MeasureSpec.getSize(heightMeasureSpec));
    }

    void mount(ComponentTree component, int width, int height) {
      mComponentView.setComponent(component);
      mComponentWidth = width;
      mComponentHeight = height;
    }

    void incrementalMount() {
      mComponentView.performIncrementalMount();
    }

    void unmount() {
      // Clear all component-related state from the view.
      mComponentView.setComponent(null);
      mComponentWidth = 0;
      mComponentHeight = 0;
    }
  }

  private static Size acquireSize() {
    Size size = sSizePool.acquire();
    if (size == null) {
      size = new Size();
    }

    return size;
  }

  private static void releaseSize(Size size) {
    sSizePool.release(size);
  }
}
