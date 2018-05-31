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

import static com.facebook.litho.SizeSpec.AT_MOST;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;

import android.content.Context;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.Diff;
import com.facebook.litho.LithoView;
import com.facebook.litho.Output;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.FromBind;
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
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.annotations.State;

/**
 * Component that wraps another component, allowing it to be vertically scrollable. It's analogous
 * to {@link android.widget.ScrollView}.
 *
 * <p>See also: {@link com.facebook.litho.widget.HorizontalScroll} for horizontal scrollability.
 *
 * @uidocs https://fburl.com/VerticalScroll:3f2b
 * @prop scrollbarEnabled whether the vertical scrollbar should be drawn
 * @prop scrollbarFadingEnabled whether the scrollbar should fade out when the view is not scrolling
 * @props initialScrollOffsetPixels initial vertical scroll offset, in pixels
 */
@MountSpec(canMountIncrementally = true, isPureRender = true)
public class VerticalScrollSpec {

  @PropDefault static final boolean scrollbarEnabled = true;
  @PropDefault static final boolean scrollbarFadingEnabled = true;

  @OnMeasure
  static void onMeasure(
      ComponentContext context,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      @State ComponentTree childComponentTree) {
    measureVerticalScroll(widthSpec, heightSpec, size, childComponentTree);
  }

  @OnBoundsDefined
  static void onBoundsDefined(
      ComponentContext c, ComponentLayout layout, @State ComponentTree childComponentTree) {
    measureVerticalScroll(
        SizeSpec.makeSizeSpec(layout.getWidth(), EXACTLY),
        SizeSpec.makeSizeSpec(layout.getHeight(), EXACTLY),
        null,
        childComponentTree);
  }

  static void measureVerticalScroll(
      int widthSpec, int heightSpec, Size size, ComponentTree childComponentTree) {
    childComponentTree.setSizeSpec(widthSpec, SizeSpec.makeSizeSpec(0, UNSPECIFIED), size);

    // If we were measuring the component now we want to compute the appropriate size depending on
    // the heightSpec
    if (size != null) {
      switch (SizeSpec.getMode(heightSpec)) {
          // If this Vertical scroll is being measured with a fixed height we don't care about
          // the size of the content and just use that instead
        case EXACTLY:
          size.height = SizeSpec.getSize(heightSpec);
          break;
          // For at most we want the VerticalScroll to be as big as its content up to the maximum
          // height specified in the heightSpec
        case AT_MOST:
          size.height = Math.min(SizeSpec.getSize(heightSpec), size.height);
      }
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
      StateValue<ComponentTree> childComponentTree,
      @Prop(optional = true) Integer initialScrollOffsetPixels,
      @Prop Component childComponent) {
    ScrollPosition initialScrollPosition = new ScrollPosition();
    initialScrollPosition.y = initialScrollOffsetPixels == null ? 0 : initialScrollOffsetPixels;
    scrollPosition.set(initialScrollPosition);

    childComponentTree.set(
        ComponentTree.create(new ComponentContext(context.getBaseContext()), childComponent)
            .incrementalMount(false)
            .build());
  }

  @OnMount
  static void onMount(
      ComponentContext context,
      final LithoScrollView lithoScrollView,
      @Prop(optional = true) boolean scrollbarEnabled,
      @Prop(optional = true) boolean scrollbarFadingEnabled,
      @Prop Component childComponent,
      @State ComponentTree childComponentTree,
      @State final ScrollPosition scrollPosition) {
    lithoScrollView.mount(childComponentTree, childComponent);
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

  @ShouldUpdate(onMount = true)
  static boolean shouldUpdate(
      @Prop Diff<Component> childComponent,
      @Prop(optional = true) Diff<Boolean> scrollbarEnabled,
      @Prop(optional = true) Diff<Boolean> scrollbarFadingEnabled) {
    return childComponent.getPrevious().isEquivalentTo(childComponent.getNext())
        && scrollbarEnabled.getPrevious().equals(scrollbarEnabled.getNext())
        && scrollbarFadingEnabled.getPrevious().equals(scrollbarFadingEnabled.getNext());
  }

  static class LithoScrollView extends ScrollView {

    private final LithoView mLithoView;

    LithoScrollView(Context context) {
      super(context);
      mLithoView = new LithoView(context);
      addView(mLithoView);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void mount(ComponentTree contentComponentTree, Component component) {
      if (mLithoView.getComponentTree() == null) {
        mLithoView.setComponentTree(contentComponentTree);
      } else {
        mLithoView.setComponent(component);
      }
    }

    private void unmount() {
      mLithoView.setComponentTree(null);
    }
  }

  static class ScrollPosition {
    int y = 0;
  }
}
