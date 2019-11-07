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

import static com.facebook.litho.SizeSpec.AT_MOST;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;

import android.content.Context;
import android.os.Build;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
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
import com.facebook.litho.Wrapper;
import com.facebook.litho.annotations.FromMeasure;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.annotations.State;
import javax.annotation.Nullable;

/**
 * Component that wraps another component, allowing it to be vertically scrollable. It's analogous
 * to {@link android.widget.ScrollView}. TreeProps will only be set during @OnCreateInitialState
 * once, so updating TreeProps on the parent will not reflect on the VerticalScroll.
 *
 * <p>See also: {@link com.facebook.litho.widget.HorizontalScroll} for horizontal scrollability.
 *
 * @uidocs https://fburl.com/VerticalScroll:3f2b
 * @prop scrollbarEnabled whether the vertical scrollbar should be drawn
 * @prop scrollbarFadingEnabled whether the scrollbar should fade out when the view is not scrolling
 * @props initialScrollOffsetPixels initial vertical scroll offset, in pixels
 * @props verticalFadingEdgeEnabled whether the vertical edges should be faded when scrolled
 * @prop fadingEdgeLength size of the faded edge used to indicate that more content is available
 * @prop onInterceptTouchListener NOT THE SAME AS LITHO'S interceptTouchHandler COMMON PROP. this is
 *     a listener that handles the underlying ScrollView's onInterceptTouchEvent first, whereas the
 *     Litho prop wraps the component into another view and intercepts there.
 */
@MountSpec(hasChildLithoViews = true, isPureRender = true)
public class VerticalScrollSpec {

  @PropDefault static final boolean scrollbarEnabled = true;
  @PropDefault static final boolean scrollbarFadingEnabled = true;

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext context,
      StateValue<ScrollPosition> scrollPosition,
      StateValue<ComponentTree> childComponentTree,
      @Prop(optional = true) Integer initialScrollOffsetPixels,
      @Prop(optional = true) boolean incrementalMountEnabled,
      @Prop Component childComponent) {
    ScrollPosition initialScrollPosition = new ScrollPosition();
    initialScrollPosition.y = initialScrollOffsetPixels == null ? 0 : initialScrollOffsetPixels;
    scrollPosition.set(initialScrollPosition);

    childComponentTree.set(
        ComponentTree.create(
                new ComponentContext(
                    context.getAndroidContext(),
                    context.getLogTag(),
                    context.getLogger(),
                    context.getTreePropsCopy()),
                childComponent)
            .incrementalMount(incrementalMountEnabled)
            .isReconciliationEnabled(false)
            .build());
  }

  @OnMeasure
  static void onMeasure(
      ComponentContext c,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      @Prop Component childComponent,
      @Prop(optional = true) boolean fillViewport,
      @State ComponentTree childComponentTree,
      Output<Integer> measuredWidth,
      Output<Integer> measuredHeight) {
    measureVerticalScroll(
        c, widthSpec, heightSpec, size, childComponentTree, childComponent, fillViewport);
    measuredWidth.set(size.width);
    measuredHeight.set(size.height);
  }

  @OnBoundsDefined
  static void onBoundsDefined(
      ComponentContext c,
      ComponentLayout layout,
      @Prop Component childComponent,
      @Prop(optional = true) boolean fillViewport,
      @State ComponentTree childComponentTree,
      @FromMeasure Integer measuredWidth,
      @FromMeasure Integer measuredHeight) {

    final int layoutWidth = layout.getWidth() - layout.getPaddingLeft() - layout.getPaddingRight();
    final int layoutHeight =
        layout.getHeight() - layout.getPaddingTop() - layout.getPaddingBottom();

    if (measuredWidth != null
        && measuredWidth == layoutWidth
        && (!fillViewport || (measuredHeight != null && measuredHeight == layoutHeight))) {
      // If we're not filling the viewport, then we always measure the height with unspecified, so
      // we just need to check that the width matches.
      return;
    }

    measureVerticalScroll(
        c,
        SizeSpec.makeSizeSpec(layout.getWidth(), EXACTLY),
        SizeSpec.makeSizeSpec(layout.getHeight(), EXACTLY),
        new Size(),
        childComponentTree,
        childComponent,
        fillViewport);
  }

  static void measureVerticalScroll(
      ComponentContext c,
      int widthSpec,
      int heightSpec,
      Size size,
      ComponentTree childComponentTree,
      Component childComponent,
      boolean fillViewport) {
    // If fillViewport is true, then set a minimum height to ensure that the viewport is filled.
    if (fillViewport) {
      childComponent =
          Wrapper.create(c)
              .delegate(childComponent)
              .minHeightPx(SizeSpec.getSize(heightSpec))
              .build();
    }

    childComponentTree.setRootAndSizeSpec(
        childComponent, widthSpec, SizeSpec.makeSizeSpec(0, UNSPECIFIED), size);

    // Compute the appropriate size depending on the heightSpec
    switch (SizeSpec.getMode(heightSpec)) {
      case EXACTLY:
        // If this Vertical scroll is being measured with a fixed height we don't care about
        // the size of the content and just use that instead
        size.height = SizeSpec.getSize(heightSpec);
        break;

      case AT_MOST:
        // For at most we want the VerticalScroll to be as big as its content up to the maximum
        // height specified in the heightSpec
        size.height = Math.min(SizeSpec.getSize(heightSpec), size.height);
        break;
    }
  }

  @OnCreateMountContent
  static LithoScrollView onCreateMountContent(Context context) {
    return new LithoScrollView(context);
  }

  @OnMount
  static void onMount(
      ComponentContext context,
      final LithoScrollView lithoScrollView,
      @Prop(optional = true) boolean scrollbarEnabled,
      @Prop(optional = true) boolean scrollbarFadingEnabled,
      @Prop(optional = true) boolean nestedScrollingEnabled,
      @Prop(optional = true) boolean incrementalMountEnabled,
      @Prop(optional = true) boolean verticalFadingEdgeEnabled,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) int fadingEdgeLength,
      @Prop(optional = true) NestedScrollView.OnScrollChangeListener onScrollChangeListener,
      // NOT THE SAME AS LITHO'S interceptTouchHandler COMMON PROP, see class javadocs
      @Prop(optional = true) OnInterceptTouchListener onInterceptTouchListener,
      @State ComponentTree childComponentTree,
      @State final ScrollPosition scrollPosition) {
    lithoScrollView.mount(childComponentTree, scrollPosition, incrementalMountEnabled);
    lithoScrollView.setScrollbarFadingEnabled(scrollbarFadingEnabled);
    lithoScrollView.setNestedScrollingEnabled(nestedScrollingEnabled);
    lithoScrollView.setVerticalFadingEdgeEnabled(verticalFadingEdgeEnabled);
    lithoScrollView.setFadingEdgeLength(fadingEdgeLength);

    // On older versions we need to disable the vertical scroll bar as otherwise we run into an NPE
    // that was only fixed in Lollipop - see
    // https://github.com/aosp-mirror/platform_frameworks_base/commit/6c8fef7fb866d244486a962dd82f4a6f26505f16#diff-7c8b4c8147fbbbf69293775bca384f31.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      lithoScrollView.setVerticalScrollBarEnabled(false);
    } else {
      lithoScrollView.setVerticalScrollBarEnabled(scrollbarEnabled);
    }
    lithoScrollView.setOnScrollChangeListener(onScrollChangeListener);
    lithoScrollView.setOnInterceptTouchListener(onInterceptTouchListener);
  }

  @OnUnmount
  static void onUnmount(ComponentContext context, LithoScrollView lithoScrollView) {
    lithoScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) null);
    lithoScrollView.setOnInterceptTouchListener(null);
    lithoScrollView.unmount();
  }

  @ShouldUpdate(onMount = true)
  static boolean shouldUpdate(
      @Prop Diff<Component> childComponent,
      @Prop(optional = true) Diff<Boolean> scrollbarEnabled,
      @Prop(optional = true) Diff<Boolean> scrollbarFadingEnabled,
      @Prop(optional = true) Diff<Boolean> fillViewport,
      @Prop(optional = true) Diff<Boolean> nestedScrollingEnabled,
      @Prop(optional = true) Diff<Boolean> incrementalMountEnabled) {
    return !childComponent.getPrevious().isEquivalentTo(childComponent.getNext())
        || !scrollbarEnabled.getPrevious().equals(scrollbarEnabled.getNext())
        || !scrollbarFadingEnabled.getPrevious().equals(scrollbarFadingEnabled.getNext())
        || !fillViewport.getPrevious().equals(fillViewport.getNext())
        || !nestedScrollingEnabled.getPrevious().equals(nestedScrollingEnabled.getNext())
        || !incrementalMountEnabled.getPrevious().equals(incrementalMountEnabled.getNext());
  }

  static class LithoScrollView extends NestedScrollView {
    private final LithoView mLithoView;

    @Nullable private ScrollPosition mScrollPosition;
    @Nullable private ViewTreeObserver.OnPreDrawListener mOnPreDrawListener;
    private boolean mIsIncrementalMountEnabled;
    private OnInterceptTouchListener mOnInterceptTouchListener;

    LithoScrollView(Context context) {
      super(context);
      mLithoView = new LithoView(context);

      addView(mLithoView);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
      boolean result = false;
      if (mOnInterceptTouchListener != null) {
        result = mOnInterceptTouchListener.onInterceptTouch(this, ev);
      }
      if (!result && super.onInterceptTouchEvent(ev)) {
        result = true;
      }
      return result;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
      super.onScrollChanged(l, t, oldl, oldt);

      if (mIsIncrementalMountEnabled) {
        mLithoView.performIncrementalMount();
      }

      if (mScrollPosition != null) {
        mScrollPosition.y = getScrollY();
      }
    }

    /**
     * NestedScrollView does not automatically consume the fling event. However, RecyclerView
     * consumes this event if it's either vertically or horizontally scrolling. {@link
     * RecyclerView#fling} Since this view is specifically made for vertically scrolling components,
     * we always consume the nested fling event just like recycler view.
     */
    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
      return super.dispatchNestedFling(velocityX, velocityY, true);
    }

    public void setOnInterceptTouchListener(OnInterceptTouchListener onInterceptTouchListener) {
      mOnInterceptTouchListener = onInterceptTouchListener;
    }

    private void mount(
        ComponentTree contentComponentTree,
        final ScrollPosition scrollPosition,
        boolean isIncrementalMountEnabled) {
      mLithoView.setComponentTree(contentComponentTree);

      mIsIncrementalMountEnabled = isIncrementalMountEnabled;
      mScrollPosition = scrollPosition;
      final ViewTreeObserver.OnPreDrawListener onPreDrawListener =
          new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
              setScrollY(scrollPosition.y);
              ViewTreeObserver currentViewTreeObserver = getViewTreeObserver();
              if (currentViewTreeObserver.isAlive()) {
                currentViewTreeObserver.removeOnPreDrawListener(this);
              }
              return true;
            }
          };
      getViewTreeObserver().addOnPreDrawListener(onPreDrawListener);

      mOnPreDrawListener = onPreDrawListener;
    }

    private void unmount() {
      mLithoView.setComponentTree(null);

      mScrollPosition = null;
      getViewTreeObserver().removeOnPreDrawListener(mOnPreDrawListener);
      mOnPreDrawListener = null;
    }
  }

  static class ScrollPosition {
    int y = 0;
  }

  public interface OnInterceptTouchListener {
    boolean onInterceptTouch(NestedScrollView nestedScrollView, MotionEvent event);
  }
}
