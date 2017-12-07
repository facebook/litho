/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.util.Pools.SynchronizedPool;
import android.view.ViewTreeObserver;
import android.widget.HorizontalScrollView;
import com.facebook.litho.ActualComponentLayout;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.LithoView;
import com.facebook.litho.Output;
import com.facebook.litho.R;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.FromBoundsDefined;
import com.facebook.litho.annotations.FromMeasure;
import com.facebook.litho.annotations.FromPrepare;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnLoadStyle;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.State;

/**
 * A component that wraps another component and allow it to be horizontally scrollable. It's
 * analogous to a {@link android.widget.HorizontalScrollView}.
 *
 * @uidocs
 */
@MountSpec
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
      @Prop Component contentProps,
      Output<ComponentTree> contentComponent) {
    contentComponent.set(
        ComponentTree.create(context, contentProps)
            .incrementalMount(false)
            .build());
  }

  @OnMeasure
  static void onMeasure(
      ComponentContext context,
      ActualComponentLayout layout,
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
      ActualComponentLayout layout,
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
  static HorizontalScrollLithoView onCreateMountContent(ComponentContext c) {
    return new HorizontalScrollLithoView(c);
  }

  @OnMount
  static void onMount(
      ComponentContext context,
      final HorizontalScrollLithoView horizontalScrollLithoView,
      @Prop(optional = true, resType = ResType.BOOL) boolean scrollbarEnabled,
      @State final ScrollPosition lastScrollPosition,
      @FromPrepare ComponentTree contentComponent,
      @FromBoundsDefined int componentWidth,
      @FromBoundsDefined int componentHeight) {

    horizontalScrollLithoView.setHorizontalScrollBarEnabled(scrollbarEnabled);
    horizontalScrollLithoView.mount(contentComponent, componentWidth, componentHeight);
    final ViewTreeObserver viewTreeObserver = horizontalScrollLithoView.getViewTreeObserver();
    viewTreeObserver.addOnPreDrawListener(
        new ViewTreeObserver.OnPreDrawListener() {
          @Override
          public boolean onPreDraw() {
            horizontalScrollLithoView.getViewTreeObserver().removeOnPreDrawListener(this);
            horizontalScrollLithoView.setScrollX(lastScrollPosition.x);
            return true;
          }
        });
    viewTreeObserver.addOnScrollChangedListener(
        new ViewTreeObserver.OnScrollChangedListener() {
          @Override
          public void onScrollChanged() {
            lastScrollPosition.x = horizontalScrollLithoView.getScrollX();
          }
        });
  }

  @OnUnmount
  static void onUnmount(
      ComponentContext context,
      HorizontalScrollLithoView mountedView) {
    mountedView.unmount();
  }

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext c,
      StateValue<ScrollPosition> lastScrollPosition) {
    lastScrollPosition.set(new ScrollPosition());
  }

  static class HorizontalScrollLithoView extends HorizontalScrollView {
    private final LithoView mLithoView;

    private int mComponentWidth;
    private int mComponentHeight;

    public HorizontalScrollLithoView(Context context) {
      super(context);
      mLithoView = new LithoView(context);
      addView(mLithoView);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
      // The hosting component view always matches the component size. This will
      // ensure that there will never be a size-mismatch between the view and the
      // component-based content, which would trigger a layout pass in the
      // UI thread.
      mLithoView.measure(
          MeasureSpec.makeMeasureSpec(mComponentWidth, MeasureSpec.EXACTLY),
          MeasureSpec.makeMeasureSpec(mComponentHeight, MeasureSpec.EXACTLY));

      // The mounted view always gets exact dimensions from the framework.
      setMeasuredDimension(
          MeasureSpec.getSize(widthMeasureSpec),
          MeasureSpec.getSize(heightMeasureSpec));
    }

    void mount(ComponentTree component, int width, int height) {
      mLithoView.setComponentTree(component);
      mComponentWidth = width;
      mComponentHeight = height;
    }

    void unmount() {
      // Clear all component-related state from the view.
      mLithoView.setComponentTree(null);
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

  static class ScrollPosition {
    int x = 0;
  }
}
