/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;

import android.support.v7.widget.RecyclerView.LayoutManager;

/**
 * An implementation of this interface will provide the {@link RecyclerBinder} with all the
 * information about the {@link RecyclerView} layout.
 */
public interface LayoutInfo {

  /**
   * This is the main scrolling direction that the {@link LayoutManager} passed to this binder will
   * use.
   *
   * @return either {@link OrientationHelper#HORIZONTAL} or {@link OrientationHelper#VERTICAL}.
   */
  int getScrollDirection();

  /**
   * @return The first visible position in the current {@link RecyclerView} layout.
   */
  int findFirstVisiblePosition();

  /**
   * @return The last visible position in the current {@link RecyclerView} layout.
   */
  int findLastVisiblePosition();

  /**
   * @return The {@link LayoutManager} to be used with the {@link RecyclerView}.
   */
  LayoutManager getLayoutManager();

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
   * @return the widthSpec to be used to measure the size of the components within this
   * {@link RecyclerBinder}.
   */
  int getChildWidthSpec(int widthSpec);

  /**
   * @param heightSpec the heightSpec used to measure the parent {@link RecyclerSpec}.
   * @return the heightSpec to be used to measure the size of the components within this
   * {@link RecyclerBinder}.
   */
  int getChildHeightSpec(int heightSpec);
