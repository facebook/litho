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

import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import java.util.List;

/**
 * An implementation of this interface will provide the {@link RecyclerBinder} with all the
 * information about the {@link RecyclerView} layout.
 */
public interface LayoutInfo extends ViewportInfo {

  /**
   * This is the main scrolling direction that the {@link LayoutManager} passed to this binder will
   * use.
   *
   * @return either {@link OrientationHelper#HORIZONTAL} or {@link OrientationHelper#VERTICAL}.
   */
  int getScrollDirection();

  /**
   * @return The {@link LayoutManager} to be used with the {@link RecyclerView}.
   */
  LayoutManager getLayoutManager();

  /** @param renderInfoCollection */
  void setRenderInfoCollection(RenderInfoCollection renderInfoCollection);

  /**
   * This is called when the {@link RecyclerBinder} needs to calculate a range size.
   * The returned value should be an approximate range size based on the size of the first measured
   * item.
   *
   * @param firstMeasuredItemWidth The width of the first item measured while computing the range.
   * @param firstMeasuredItemHeight The height of the first item measured while computing the range.
   * @param recyclerMeasuredWidth The measured width of the RecyclerView. If the RecyclerView
   * scrolls vertically this might be not significant.
   * @param recyclerMeasuredHeight The measured height of the RecyclerView. If the RecyclerView
   * scrolls horizontally this might be not significant.
   * @return The estimated number of items that are needed to fill one viewport of the RecyclerView.
   */
  int approximateRangeSize(
      int firstMeasuredItemWidth,
      int firstMeasuredItemHeight,
      int recyclerMeasuredWidth,
      int recyclerMeasuredHeight);

  /**
   * @param widthSpec the widthSpec used to measure the parent {@link RecyclerSpec}.
   * @param renderInfo retrieve SpanSize of the component if it is a {@link GridLayoutInfo}
   * @return the widthSpec to be used to measure the size of the components within this
   * {@link RecyclerBinder}.
   */
  int getChildWidthSpec(int widthSpec, RenderInfo renderInfo);

  /**
   * @param heightSpec the heightSpec used to measure the parent {@link RecyclerSpec}.
   * @param renderInfo retrieve SpanSize of the component if it is a {@link GridLayoutInfo}
   * @return the heightSpec to be used to measure the size of the components within this
   * {@link RecyclerBinder}.
   */
  int getChildHeightSpec(int heightSpec, RenderInfo renderInfo);

  /**
   * @param measuredWidth the width of the RecyclerView
   * @param measuredHeight the height of the RecyclerView
   * @return a {@link ViewportFiller} to fill the RecyclerView viewport with views, or null to not
   *     pre-fill the RecyclerView.
   */
  ViewportFiller createViewportFiller(int measuredWidth, int measuredHeight);

  /**
   * @param maxHeight the max height of the parent {@link RecyclerSpec}.
   * @param componentTreeHolders the list of {@link ComponentTreeHolder} in this {@link
   *     RecyclerBinder}.
   * @return the measured height of this {@link RecyclerBinder}.
   */
  int computeWrappedHeight(int maxHeight, List<ComponentTreeHolder> componentTreeHolders);

  interface RenderInfoCollection {
    RenderInfo getRenderInfoAt(int position);
  }

  /**
   * Interface that is responsible for filling the viewport of the list with initial layouts
   * according to the LayoutManager. The goal here is to have the layouts that the RecyclerView will
   * ask for when it comes onto the screen already computed, e.g. in the background, so that we
   * don't drop frames on the main thread. NB: This class should try to respect the layout of views
   * as they will appear in the RecyclerView.
   */
  interface ViewportFiller {

    /**
     * Implementations should return true if they need more views to be computed in order to fill
     * the screen.
     */
    boolean wantsMore();

    /**
     * This will be called to inform implementations that the next layout has been computed.
     * Implementations should use the width/height to determine whether they still need more views
     * to fill their initial viewport (which should be reflected in the next call to {@link
     * #wantsMore()}
     */
    void add(RenderInfo renderInfo, int width, int height);

    /**
     * Return the fill along the main axis (i.e. height for VERTICAL and width for HORIZONTAL), this
     * method is available after {@link ViewportFiller#add(RenderInfo, int, int)} is called.
     */
    int getFill();
  }
}
