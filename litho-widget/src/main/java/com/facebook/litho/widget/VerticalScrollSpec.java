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
import android.support.v4.util.Pools;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.LithoView;
import com.facebook.litho.Output;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.FromBind;
import com.facebook.litho.annotations.FromBoundsDefined;
import com.facebook.litho.annotations.FromMeasure;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnUnbind;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.State;

/**
 * Component that wraps another component, allowing it to be vertically scrollable. It's analogous
 * to {@link android.widget.ScrollView}.
 *
 * <p>See also: {@link com.facebook.litho.widget.HorizontalScroll} for horizontal scrollability.
 *
 * @uidocs https://fburl.com/VerticalScroll:android
 * @prop scrollbarEnabled whether the vertical scrollbar should be drawn
 * @prop scrollbarFadingEnabled whether the scrollbar should fade out when the view is not scrolling
 * @props initialScrollOffsetPixels initial vertical scroll offset, in pixels
 */
@MountSpec(canMountIncrementally = true)
public class VerticalScrollSpec {

  @PropDefault static final boolean scrollbarEnabled = true;
  @PropDefault static final boolean scrollbarFadingEnabled = true;

  private static final Pools.SynchronizedPool<Size> sSizePool = new Pools.SynchronizedPool<>(2);

  @OnMeasure
  static void onMeasure(
      ComponentContext context,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      @Prop Component childComponent,
      Output<Integer> measuredComponentWidth,
      Output<Integer> measuredComponentHeight) {
    final int measuredWidth;
    final int measuredHeight;

    final Size measuredSize = acquireSize();

    childComponent.measure(context, widthSpec, SizeSpec.makeSizeSpec(0, UNSPECIFIED), measuredSize);

    measuredWidth = measuredSize.width;
    measuredHeight = measuredSize.height;

    releaseSize(measuredSize);

    measuredComponentWidth.set(measuredWidth);
    measuredComponentHeight.set(measuredHeight);

    // If size constraints were not explicitly defined, just fallback to the
    // component dimensions instead.
    size.height =
        SizeSpec.getMode(heightSpec) == UNSPECIFIED ? measuredHeight : SizeSpec.getSize(heightSpec);
    size.width = measuredWidth;
  }

  @OnBoundsDefined
  static void onBoundsDefined(
      ComponentContext c,
      ComponentLayout layout,
      @Prop Component childComponent,
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
      childComponent.measure(
          c,
          SizeSpec.makeSizeSpec(layout.getWidth(), EXACTLY),
          SizeSpec.makeSizeSpec(0, UNSPECIFIED),
          contentSize);

      measuredWidth = contentSize.width;
      measuredHeight = contentSize.height;

      releaseSize(contentSize);

      componentWidth.set(measuredWidth);
      componentHeight.set(measuredHeight);
    }
  }

  @OnCreateMountContent
  static LithoScrollView onCreateMountContent(ComponentContext context) {
    return new LithoScrollView(context);
  }

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext context,
      StateValue<ScrollPosition> scrollPosition,
      @Prop Component childComponent,
      @Prop(optional = true) Integer initialScrollOffsetPixels) {
    ScrollPosition initialScrollPosition = new ScrollPosition();
    initialScrollPosition.y = initialScrollOffsetPixels == null ? 0 : initialScrollOffsetPixels;
    scrollPosition.set(initialScrollPosition);
  }

  @OnMount
  static void onMount(
      ComponentContext context,
      final LithoScrollView lithoScrollView,
      @Prop(optional = true) boolean scrollbarEnabled,
      @Prop(optional = true) boolean scrollbarFadingEnabled,
      @Prop Component childComponent,
      @State final ScrollPosition scrollPosition,
      @FromBoundsDefined int componentWidth,
      @FromBoundsDefined int componentHeight) {
    lithoScrollView.mount(childComponent, componentWidth, componentHeight);
    lithoScrollView.setVerticalScrollBarEnabled(scrollbarEnabled);
    lithoScrollView.setScrollbarFadingEnabled(scrollbarFadingEnabled);
  }

  @OnBind
  protected static void onBind(
      ComponentContext context,
      final LithoScrollView lithoScrollView,
      @State final ScrollPosition scrollPosition,
      Output<ViewTreeObserver.OnPreDrawListener> onPreDrawListener,
      Output<ViewTreeObserver.OnScrollChangedListener> onScrollChangedListener) {
    ViewTreeObserver viewTreeObserver = lithoScrollView.getViewTreeObserver();

    ViewTreeObserver.OnPreDrawListener preDrawListener =
        new ViewTreeObserver.OnPreDrawListener() {
          @Override
          public boolean onPreDraw() {
            lithoScrollView.setScrollY(scrollPosition.y);
            ViewTreeObserver currentViewTreeObserver = lithoScrollView.getViewTreeObserver();
            if (currentViewTreeObserver.isAlive()) {
              currentViewTreeObserver.removeOnPreDrawListener(this);
            }
            return true;
          }
        };
    viewTreeObserver.addOnPreDrawListener(preDrawListener);
    onPreDrawListener.set(preDrawListener);

    ViewTreeObserver.OnScrollChangedListener scrollChangedListener =
        new ViewTreeObserver.OnScrollChangedListener() {
          @Override
          public void onScrollChanged() {
            scrollPosition.y = lithoScrollView.getScrollY();
          }
        };
    viewTreeObserver.addOnScrollChangedListener(scrollChangedListener);
    onScrollChangedListener.set(scrollChangedListener);
  }

  @OnUnbind
  protected static void onUnbind(
      ComponentContext context,
      LithoScrollView lithoScrollView,
      @FromBind ViewTreeObserver.OnPreDrawListener onPreDrawListener,
      @FromBind ViewTreeObserver.OnScrollChangedListener onScrollChangedListener) {
    ViewTreeObserver viewTreeObserver = lithoScrollView.getViewTreeObserver();
    viewTreeObserver.removeOnPreDrawListener(onPreDrawListener);
    viewTreeObserver.removeOnScrollChangedListener(onScrollChangedListener);
  }

  @OnUnmount
  static void onUnmount(ComponentContext context, LithoScrollView lithoScrollView) {
    lithoScrollView.unmount();
  }

  static class LithoScrollView extends ScrollView {

    private final LithoView mLithoView;
    private int mComponentWidth;
    private int mComponentHeight;

    LithoScrollView(Context context) {
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
          MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
      super.onScrollChanged(l, t, oldl, oldt);
    }

    private void mount(Component component, int componentWidth, int componentHeight) {
      if (mLithoView.getComponentTree() == null) {
        mLithoView.setComponentTree(
            ComponentTree.create(mLithoView.getComponentContext(), component)
                .incrementalMount(false)
                .build());
      } else {
        mLithoView.setComponent(component);
      }

      mComponentWidth = componentWidth;
      mComponentHeight = componentHeight;
    }

    private void unmount() {
      mLithoView.unbind();
      mComponentWidth = 0;
      mComponentHeight = 0;
    }
  }

  static class ScrollPosition {
    int y = 0;
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
