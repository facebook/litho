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

package com.facebook.litho.widget

import androidx.recyclerview.widget.RecyclerView

/**
 * An implementation of this interface will provide the [RecyclerBinder] with all the information
 * about the [RecyclerView] layout.
 */
interface LayoutInfo : ViewportInfo {
  /**
   * This is the main scrolling direction that the [LayoutManager] passed to this binder will use.
   *
   * @return either [OrientationHelper.HORIZONTAL] or [OrientationHelper.VERTICAL].
   */
  fun getScrollDirection(): Int

  /** @return The [RecyclerView.LayoutManager] to be used with the [RecyclerView]. */
  fun getLayoutManager(): RecyclerView.LayoutManager?

  /** @param renderInfoCollection */
  fun setRenderInfoCollection(renderInfoCollection: RenderInfoCollection?)

  /**
   * RecyclerBinder delegates scrolling responsibilities to the LayoutInfo, as the varied
   * LayoutManagers wrapped by a LayoutInfo lack a common scrolling interface. Typical
   * implementations should forward the call to the underlying LayoutManager's
   * scrollToPositionWithOffset() or an equivalent.
   *
   * @param position Index of the item in the adapter
   * @param offset Additional adjustment to control the precise position the scroll moves to.
   */
  fun scrollToPositionWithOffset(position: Int, offset: Int)

  /**
   * This is called when the [RecyclerBinder] needs to calculate a range size. The returned value
   * should be an approximate range size based on the size of the first measured item.
   *
   * @param firstMeasuredItemWidth The width of the first item measured while computing the range.
   * @param firstMeasuredItemHeight The height of the first item measured while computing the range.
   * @param recyclerMeasuredWidth The measured width of the RecyclerView. If the RecyclerView
   *   scrolls vertically this might be not significant.
   * @param recyclerMeasuredHeight The measured height of the RecyclerView. If the RecyclerView
   *   scrolls horizontally this might be not significant.
   * @return The estimated number of items that are needed to fill one viewport of the RecyclerView.
   */
  fun approximateRangeSize(
      firstMeasuredItemWidth: Int,
      firstMeasuredItemHeight: Int,
      recyclerMeasuredWidth: Int,
      recyclerMeasuredHeight: Int
  ): Int

  /**
   * @param widthSpec the widthSpec used to measure the parent [Recycler].
   * @param renderInfo retrieve SpanSize of the component if it is a [GridLayoutInfo]
   * @return the widthSpec to be used to measure the size of the components within this
   *   [RecyclerBinder].
   */
  fun getChildWidthSpec(widthSpec: Int, renderInfo: RenderInfo): Int

  /**
   * @param heightSpec the heightSpec used to measure the parent [Recycler].
   * @param renderInfo retrieve SpanSize of the component if it is a [GridLayoutInfo]
   * @return the heightSpec to be used to measure the size of the components within this
   *   [RecyclerBinder].
   */
  fun getChildHeightSpec(heightSpec: Int, renderInfo: RenderInfo): Int

  /**
   * @param measuredWidth the width of the RecyclerView
   * @param measuredHeight the height of the RecyclerView
   * @return a [ViewportFiller] to fill the RecyclerView viewport with views, or `null` to not
   *   pre-fill the RecyclerView.
   */
  fun createViewportFiller(measuredWidth: Int, measuredHeight: Int): ViewportFiller?

  /**
   * @param maxHeight the max height of the parent [Recycler].
   * @param componentTreeHolders the list of [ComponentTreeHolder] in this [RecyclerBinder].
   * @return the measured height of this [RecyclerBinder].
   */
  fun computeWrappedHeight(
      maxHeight: Int,
      componentTreeHolders: MutableList<ComponentTreeHolder>
  ): Int

  interface RenderInfoCollection {
    fun getRenderInfoAt(position: Int): RenderInfo
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
    fun wantsMore(): Boolean

    /**
     * This will be called to inform implementations that the next layout has been computed.
     * Implementations should use the width/height to determine whether they still need more views
     * to fill their initial viewport (which should be reflected in the next call to [wantsMore]
     */
    fun add(renderInfo: RenderInfo, width: Int, height: Int)

    /**
     * Return the fill along the main axis (i.e. height for `VERTICAL` and width for `HORIZONTAL`),
     * this method is available after [ViewportFiller.add] is called.
     */
    fun getFill(): Int
  }
}
