/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.util.Pools.SynchronizedPool;
import android.widget.HorizontalScrollView;

import com.facebook.R;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.ComponentView;
import com.facebook.litho.Output;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnLoadStyle;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.FromBoundsDefined;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.FromMeasure;
import com.facebook.litho.annotations.FromPrepare;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.Size;

import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;

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
        ComponentTree.create(context, contentProps).build());
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
