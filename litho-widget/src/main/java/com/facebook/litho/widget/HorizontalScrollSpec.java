/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.widget;

import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.HorizontalScrollView;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.LithoView;
import com.facebook.litho.Output;
import com.facebook.litho.R;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.FromBoundsDefined;
import com.facebook.litho.annotations.FromMeasure;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnLoadStyle;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.State;
import com.facebook.yoga.YogaDirection;
import javax.annotation.Nullable;

/**
 * A component that wraps another component and allow it to be horizontally scrollable. It's
 * analogous to a {@link android.widget.HorizontalScrollView}.
 *
 * @uidocs https://fburl.com/HorizontalScroll:e378
 */
@MountSpec
class HorizontalScrollSpec {

  private static final int LAST_SCROLL_POSITION_UNSET = -1;

  @PropDefault static final boolean scrollbarEnabled = true;

  /** Scroll change listener invoked when the scroll position changes. */
  public interface OnScrollChangeListener {
    void onScrollChange(View v, int scrollX, int oldScrollX);
  }

  @OnLoadStyle
  static void onLoadStyle(ComponentContext c, Output<Boolean> scrollbarEnabled) {

    final TypedArray a = c.obtainStyledAttributes(R.styleable.HorizontalScroll, 0);

    for (int i = 0, size = a.getIndexCount(); i < size; i++) {
      final int attr = a.getIndex(i);

      if (attr == R.styleable.HorizontalScroll_android_scrollbars) {
        scrollbarEnabled.set(a.getInt(attr, 0) != 0);
      }
    }

    a.recycle();
  }

  @OnMeasure
  static void onMeasure(
      ComponentContext context,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      @Prop Component contentProps,
      @State ComponentTree childComponentTree,
      Output<Integer> measuredComponentWidth,
      Output<Integer> measuredComponentHeight) {

    final int measuredWidth;
    final int measuredHeight;

    final Size contentSize = new Size();

    // Measure the component with undefined width spec, as the contents of the
    // hscroll have unlimited horizontal space.
    childComponentTree.setRootAndSizeSpec(
        contentProps, SizeSpec.makeSizeSpec(0, UNSPECIFIED), heightSpec, contentSize);
    contentProps.measure(context, SizeSpec.makeSizeSpec(0, UNSPECIFIED), heightSpec, contentSize);

    measuredWidth = contentSize.width;
    measuredHeight = contentSize.height;

    measuredComponentWidth.set(measuredWidth);
    measuredComponentHeight.set(measuredHeight);

    // If size constraints were not explicitly defined, just fallback to the
    // component dimensions instead.
    size.width =
        SizeSpec.getMode(widthSpec) == UNSPECIFIED ? measuredWidth : SizeSpec.getSize(widthSpec);
    size.height = measuredHeight;
  }

  @OnBoundsDefined
  static void onBoundsDefined(
      ComponentContext context,
      ComponentLayout layout,
      @Prop Component contentProps,
      @Prop(optional = true) boolean fillViewport,
      @State ComponentTree childComponentTree,
      @FromMeasure Integer measuredComponentWidth,
      @FromMeasure Integer measuredComponentHeight,
      Output<Integer> componentWidth,
      Output<Integer> componentHeight,
      Output<YogaDirection> layoutDirection) {

    final int layoutWidth = layout.getWidth() - layout.getPaddingLeft() - layout.getPaddingRight();

    // If onMeasure() has been called, this means the content component already
    // has a defined size, no need to calculate it again.
    if (measuredComponentWidth != null && measuredComponentHeight != null) {
      componentWidth.set(Math.max(measuredComponentWidth, fillViewport ? layoutWidth : 0));
      componentHeight.set(measuredComponentHeight);
    } else {
      final int measuredWidth;
      final int measuredHeight;

      Size contentSize = new Size();
      childComponentTree.setRootAndSizeSpec(
          contentProps,
          SizeSpec.makeSizeSpec(0, UNSPECIFIED),
          SizeSpec.makeSizeSpec(layout.getHeight(), EXACTLY),
          contentSize);

      measuredWidth = Math.max(contentSize.width, fillViewport ? layoutWidth : 0);
      measuredHeight = contentSize.height;

      componentWidth.set(measuredWidth);
      componentHeight.set(measuredHeight);
    }

    layoutDirection.set(layout.getResolvedLayoutDirection());
  }

  @OnCreateMountContent
  static HorizontalScrollLithoView onCreateMountContent(Context c) {
    return new HorizontalScrollLithoView(c);
  }

  @OnMount
  static void onMount(
      final ComponentContext context,
      final HorizontalScrollLithoView horizontalScrollLithoView,
      @Prop(optional = true, resType = ResType.BOOL) boolean scrollbarEnabled,
      @Prop(optional = true) HorizontalScrollEventsController eventsController,
      @Prop(optional = true) final OnScrollChangeListener onScrollChangeListener,
      @State final ScrollPosition lastScrollPosition,
      @State ComponentTree childComponentTree,
      @FromBoundsDefined int componentWidth,
      @FromBoundsDefined int componentHeight,
      @FromBoundsDefined final YogaDirection layoutDirection) {

    horizontalScrollLithoView.setHorizontalScrollBarEnabled(scrollbarEnabled);
    horizontalScrollLithoView.mount(
        childComponentTree,
        lastScrollPosition,
        onScrollChangeListener,
        componentWidth,
        componentHeight);
    final ViewTreeObserver viewTreeObserver = horizontalScrollLithoView.getViewTreeObserver();
    viewTreeObserver.addOnPreDrawListener(
        new ViewTreeObserver.OnPreDrawListener() {
          @Override
          public boolean onPreDraw() {
            horizontalScrollLithoView.getViewTreeObserver().removeOnPreDrawListener(this);

            if (lastScrollPosition.x == LAST_SCROLL_POSITION_UNSET) {
              if (layoutDirection == YogaDirection.RTL) {
                horizontalScrollLithoView.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
              }
              lastScrollPosition.x = horizontalScrollLithoView.getScrollX();
            } else {
              horizontalScrollLithoView.setScrollX(lastScrollPosition.x);
            }

            return true;
          }
        });

    if (eventsController != null) {
      eventsController.setScrollableView(horizontalScrollLithoView);
    }
  }

  @OnUnmount
  static void onUnmount(
      ComponentContext context,
      HorizontalScrollLithoView mountedView,
      @Prop(optional = true) HorizontalScrollEventsController eventsController) {

    mountedView.unmount();

    if (eventsController != null) {
      eventsController.setScrollableView(null);
    }
  }

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext c,
      StateValue<ScrollPosition> lastScrollPosition,
      StateValue<ComponentTree> childComponentTree,
      @Prop Component contentProps,
      @Prop(optional = true) Integer initialScrollPosition) {

    lastScrollPosition.set(
        new ScrollPosition(
            initialScrollPosition == null ? LAST_SCROLL_POSITION_UNSET : initialScrollPosition));
    childComponentTree.set(
        ComponentTree.create(
                new ComponentContext(
                    c.getAndroidContext(), c.getLogTag(), c.getLogger(), c.getTreePropsCopy()),
                contentProps)
            .incrementalMount(false)
            .build());
  }

  static class HorizontalScrollLithoView extends HorizontalScrollView {
    private final LithoView mLithoView;

    private int mComponentWidth;
    private int mComponentHeight;

    @Nullable private ScrollPosition mScrollPosition;
    @Nullable private HorizontalScrollSpec.OnScrollChangeListener mOnScrollChangeListener;

    public HorizontalScrollLithoView(Context context) {
      super(context);
      mLithoView = new LithoView(context);
      addView(mLithoView);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
      super.onScrollChanged(l, t, oldl, oldt);

      if (mScrollPosition != null) {
        if (mOnScrollChangeListener != null) {
          mOnScrollChangeListener.onScrollChange(this, getScrollX(), mScrollPosition.x);
        }
        mScrollPosition.x = getScrollX();
      }
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

    void mount(
        ComponentTree componentTree,
        final ScrollPosition scrollPosition,
        HorizontalScrollSpec.OnScrollChangeListener onScrollChangeListener,
        int width,
        int height) {
      mLithoView.setComponentTree(componentTree);
      mScrollPosition = scrollPosition;
      mOnScrollChangeListener = onScrollChangeListener;
      mComponentWidth = width;
      mComponentHeight = height;
    }

    void unmount() {
      // Clear all component-related state from the view.
      mLithoView.unbind();
      mComponentWidth = 0;
      mComponentHeight = 0;
      mScrollPosition = null;
      mOnScrollChangeListener = null;
    }
  }

  static class ScrollPosition {
    int x;

    ScrollPosition(int initialX) {
      this.x = initialX;
    }
  }
}
