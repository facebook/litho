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

package com.facebook.litho.sections.widget;

import com.facebook.litho.widget.LayoutHandlerFactory;
import com.facebook.litho.widget.RecyclerBinder;

/**
 * Configuration setting for {@link RecyclerBinder}.
 */
public class RecyclerBinderConfiguration {
  private static final double DEFAULT_RANGE = 5.0;
  private final double mRangeRatio;
  private final LayoutHandlerFactory mLayoutHandlerFactory;
  private final boolean mCanPrefetchDisplayLists;
  private final boolean mIsCircular;
  private boolean mHasDynamicItemHeight;
  private boolean mInsertPostAsyncLayoutEnabled;
  private boolean mIsWrapContent;

  public RecyclerBinderConfiguration(double rangeRatio) {
    this(rangeRatio, null, false);
  }

  public RecyclerBinderConfiguration(double rangeRatio, LayoutHandlerFactory idleExecutor) {
    this(rangeRatio, idleExecutor, false);
  }

  public RecyclerBinderConfiguration(
      double rangeRatio, LayoutHandlerFactory idleExecutor, boolean canPrefetchDisplayLists) {
    this(rangeRatio, idleExecutor, canPrefetchDisplayLists, false);
  }

  public RecyclerBinderConfiguration(
      double rangeRatio,
      LayoutHandlerFactory idleExecutor,
      boolean canPrefetchDisplayLists,
      boolean isCircular) {
    this(rangeRatio, idleExecutor, canPrefetchDisplayLists, isCircular, false);
  }

  /**
   * @param rangeRatio Ratio to determine the number of components before and after the {@link
   *     android.support.v7.widget.RecyclerView}'s total number of currently visible items to have
   *     their Component layout computed ahead of time.
   *     <p>e.g total number of visible items = 5 rangeRatio = 10 total number of items before the
   *     1st visible item to be computed = 5 * 10 = 50 total number of items after the last visible
   *     item to be computed = 5 * 10 = 50
   * @param idleExecutor This determines the thread on which the Component layout calculation will
   *     be processed in. Null means that the computation will be done in the background thread.
   * @param canPrefetchDisplayLists If this is true, displaylists for the Android Views and
   *     Drawables that are not collected during the ComponentTree layout computation will be
   *     collected whenever the UI thread is free.
   * @param isCircular If true, the underlying RecyclerBinder will have a circular behaviour. Note:
   *     circular lists DO NOT support any operation that changes the size of items like insert,
   *     remove, insert range, remove range
   * @param isWrapContent If true, the underlying RecyclerBinder will measure the parent height by
   *     the height of children if the orientation is vertical, or measure the parent width by the
   *     width of children if the orientation is horizontal.
   */
  public RecyclerBinderConfiguration(
      double rangeRatio,
      LayoutHandlerFactory idleExecutor,
      boolean canPrefetchDisplayLists,
      boolean isCircular,
      boolean isWrapContent) {
    mRangeRatio = rangeRatio > 0 ? rangeRatio : DEFAULT_RANGE;
    mLayoutHandlerFactory = idleExecutor;
    mCanPrefetchDisplayLists = canPrefetchDisplayLists;
    mIsCircular = isCircular;
    mIsWrapContent = isWrapContent;
  }

  /**
   * TODO T23919104 mihaelao Do not enable this. This is an experimental feature and your Section
   * surface will take a perf hit if you use it. Talk to the Litho team if you think you need this.
   */
  public void setHasDynamicItemHeight(boolean hasDynamicItemHeight) {
    mHasDynamicItemHeight = hasDynamicItemHeight;
  }

  /**
   * TODO (T26795745): remove this once the experiment is finished.
   *
   * <p>Do not enable this. This is an experimental feature and your Section surface will take a
   * perf hit if you use it.
   */
  public void setInsertPostAsyncLayoutEnabled(boolean insertPostAsyncLayoutEnabled) {
    mInsertPostAsyncLayoutEnabled = insertPostAsyncLayoutEnabled;
  }

  public double getRangeRatio() {
    return mRangeRatio;
  }

  public LayoutHandlerFactory getLayoutHandlerFactory() {
    return mLayoutHandlerFactory;
  }

  public boolean canPrefetchDisplayLists() {
    return mCanPrefetchDisplayLists;
  }

  public boolean isCircular() {
    return mIsCircular;
  }

  public boolean isWrapContent() {
    return mIsWrapContent;
  }

  boolean hasDynamicItemHeight() {
    return mHasDynamicItemHeight;
  }

  boolean insertPostAsyncLayoutEnabled() {
    return mInsertPostAsyncLayoutEnabled;
  }
}
