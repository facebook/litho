/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import static android.view.View.OVER_SCROLL_IF_CONTENT_SCROLLS;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.ViewTreeObserver;
import android.widget.HorizontalScrollView;
import androidx.annotation.Nullable;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.ComponentTree;
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

/**
 * A component that wraps another component and allow it to be horizontally scrollable. It's
 * analogous to a {@link android.widget.HorizontalScrollView}.
 *
 * @uidocs
 */
@MountSpec(hasChildLithoViews = true)
class HorizontalScrollSpec {

  private static final int LAST_SCROLL_POSITION_UNSET = -1;

  @PropDefault static final boolean scrollbarEnabled = true;
  @PropDefault static final int initialScrollPosition = LAST_SCROLL_POSITION_UNSET;
  @PropDefault static final boolean incrementalMountEnabled = false;
  @PropDefault static final int overScrollMode = OVER_SCROLL_IF_CONTENT_SCROLLS;

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
    childComponentTree.setRootAndSizeSpecSync(
        contentProps, SizeSpec.makeSizeSpec(0, UNSPECIFIED), heightSpec, contentSize);

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
      @Nullable @FromMeasure Integer measuredComponentWidth,
      @Nullable @FromMeasure Integer measuredComponentHeight,
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
      childComponentTree.setRootAndSizeSpecSync(
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
      @Prop(optional = true) @Nullable HorizontalScrollEventsController eventsController,
      @Prop(optional = true) @Nullable
          HorizontalScrollLithoView.OnScrollChangeListener onScrollChangeListener,
      @Prop(optional = true) @Nullable final ScrollStateListener scrollStateListener,
      @Prop(optional = true) boolean incrementalMountEnabled,
      @Prop(optional = true) int overScrollMode,
      @Prop(optional = true) boolean horizontalFadingEdgeEnabled,
      @Prop(optional = true) int fadingEdgeLength,
      @State final HorizontalScrollLithoView.ScrollPosition lastScrollPosition,
      @State ComponentTree childComponentTree,
      @Nullable @FromBoundsDefined Integer componentWidth,
      @Nullable @FromBoundsDefined Integer componentHeight,
      @FromBoundsDefined final YogaDirection layoutDirection) {

    horizontalScrollLithoView.setHorizontalScrollBarEnabled(scrollbarEnabled);
    horizontalScrollLithoView.setOverScrollMode(overScrollMode);
    horizontalScrollLithoView.setHorizontalFadingEdgeEnabled(horizontalFadingEdgeEnabled);
    horizontalScrollLithoView.setFadingEdgeLength(fadingEdgeLength);
    horizontalScrollLithoView.mount(
        childComponentTree,
        lastScrollPosition,
        componentWidth != null ? componentWidth : 0,
        componentHeight != null ? componentHeight : 0,
        onScrollChangeListener,
        scrollStateListener);
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
      @Prop(optional = true) @Nullable HorizontalScrollEventsController eventsController) {

    mountedView.unmount();

    if (eventsController != null) {
      eventsController.setScrollableView(null);
    }
  }

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext c,
      StateValue<HorizontalScrollLithoView.ScrollPosition> lastScrollPosition,
      StateValue<ComponentTree> childComponentTree,
      @Prop Component contentProps,
      @Prop(optional = true) int initialScrollPosition,
      @Prop(optional = true) boolean incrementalMountEnabled) {

    lastScrollPosition.set(new HorizontalScrollLithoView.ScrollPosition(initialScrollPosition));
    childComponentTree.set(
        ComponentTree.createNestedComponentTree(c, contentProps)
            .incrementalMount(incrementalMountEnabled)
            .build());
  }
}
