/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.content.Context;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.LithoView;
import com.facebook.litho.Output;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.FromBind;
import com.facebook.litho.annotations.FromPrepare;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.OnUnbind;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.State;

/**
 * Component that wraps another component, allowing it to be vertically scrollable.
 * It's analogous to {@link android.widget.ScrollView}.
 *
 * See also:
 * {@link com.facebook.litho.widget.HorizontalScroll} for horizontal scrollability.
 *
 * @uidocs https://fburl.com/VerticalScroll:android
 *
 * @prop scrollbarEnabled whether the vertical scrollbar should be drawn
 * @prop scrollbarFadingEnabled whether the scrollbar should fade out when the view is not
 *       scrolling
 * @props initialScrollOffsetPixels initial vertical scroll offset, in pixels
 */
@MountSpec(canMountIncrementally = true)
public class VerticalScrollSpec {

  @PropDefault static final boolean scrollbarEnabled = true;
  @PropDefault static final boolean scrollbarFadingEnabled = true;

  @OnPrepare
  static void onPrepare(
      ComponentContext context,
      @Prop Component<?> childComponent,
      Output<ComponentTree> childComponentTree) {
      childComponentTree.set(
          ComponentTree.create(context,childComponent).build());

  }

  @OnCreateMountContent
  static LithoScrollView onCreateMountContent(ComponentContext context) {
    return new LithoScrollView(context);
  }

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext context,
      StateValue<ScrollPosition> scrollPosition,
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
      @FromPrepare ComponentTree childComponentTree,
      @State final ScrollPosition scrollPosition) {
      lithoScrollView.mount(childComponentTree);
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

    LithoScrollView(Context context) {
        super(context);
        mLithoView = new LithoView(context);
        addView(mLithoView);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
      super.onScrollChanged(l, t, oldl, oldt);

      // Perform incremental mount since visible region has changed.
      mLithoView.performIncrementalMount();
    }

    private void mount(ComponentTree contentComponentTree) {
      mLithoView.setComponentTree(contentComponentTree);
    }

    private void unmount() {
      mLithoView.setComponentTree(null);
    }
  }

  static class ScrollPosition {
    int y = 0;
  }
}
