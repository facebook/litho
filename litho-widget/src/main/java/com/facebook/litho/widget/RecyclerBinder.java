/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.view.ViewGroup;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentInfo;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.ComponentView;
import com.facebook.litho.MeasureComparisonUtils;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.ThreadUtils;
import com.facebook.litho.utils.IncrementalMountUtils;

import static android.support.v7.widget.OrientationHelper.HORIZONTAL;
import static android.support.v7.widget.OrientationHelper.VERTICAL;
import static com.facebook.litho.MeasureComparisonUtils.isMeasureSpecCompatible;

/**
 * This binder class is used to asynchronously layout Components given a list of {@link Component}
 * and attaching them to a {@link RecyclerSpec}.
 */
@ThreadSafe
public class RecyclerBinder implements Binder<RecyclerView> {

  private static final int UNINITIALIZED = -1;
  private static final Size sDummySize = new Size();

  @GuardedBy("this")
  private final List<ComponentTreeHolder> mComponentTreeHolders;
  private final LayoutInfo mLayoutInfo;
  private final RecyclerView.Adapter mInternalAdapter;
  private final ComponentContext mComponentContext;
  private final RangeScrollListener mRangeScrollListener = new RangeScrollListener();
  private final LayoutHandlerFactory mLayoutHandlerFactory;
  private final GridSpanSizeLookup mGridSpanSizeLookup = new GridSpanSizeLookup();

  // Data structure to be used to hold Components and ComponentTreeHolders before adding them to
  // the RecyclerView. This happens in the case of inserting something inside the current working
  // range.
  //TODO t15827349
  private final List<ComponentTreeHolder> mPendingComponentTreeHolders;
  private final float mRangeRatio;
  private final AtomicBoolean mIsMeasured = new AtomicBoolean(false);

  private int mLastWidthSpec = UNINITIALIZED;
  private int mLastHeightSpec = UNINITIALIZED;
  private int mChildrenWidthSpec = UNINITIALIZED;
  private int mChildrenHeightSpec = UNINITIALIZED;
  private Size mMeasuredSize;
  private RecyclerView mMountedView;
  private int mCurrentFirstVisiblePosition;
  private int mCurrentLastVisiblePosition;
  private RangeCalculationResult mRange;

  public RecyclerBinder(
      ComponentContext componentContext,
      float rangeRatio,
      LayoutInfo layoutInfo) {
    this(componentContext, rangeRatio, layoutInfo, null);
  }

  /**
   * @param componentContext The {@link ComponentContext} this RecyclerBinder will use.
   * @param rangeRatio specifies how big a range this binder should try to compute. The range is
   * computed as number of items in the viewport (when the binder is measured) multiplied by the
   * range ratio. The ratio is to be intended in both directions. For example a ratio of 1 means
   * that if there are currently N components on screen, the binder should try to compute the layout
   * for the N components before the first component on screen and for the N components after the
   * last component on screen.
   * @param layoutInfo an implementation of {@link LayoutInfo} that will expose information about
   * the {@link LayoutManager} this RecyclerBinder will use.
   * @param layoutHandlerFactory the RecyclerBinder will use this layoutHandlerFactory when creating
   * {@link ComponentTree}s in order to specify on which thread layout calculation should happen.
   * Pass null to use the default components layout thread.
   */
  public RecyclerBinder(
      ComponentContext componentContext,
      float rangeRatio,
      LayoutInfo layoutInfo,
      @Nullable LayoutHandlerFactory layoutHandlerFactory) {
    mComponentContext = componentContext;
    mComponentTreeHolders = new ArrayList<>();
    mPendingComponentTreeHolders = new ArrayList<>();
    mInternalAdapter = new InternalAdapter();

    mRangeRatio = rangeRatio;
    mLayoutInfo = layoutInfo;
    mLayoutHandlerFactory = layoutHandlerFactory;
    mCurrentFirstVisiblePosition = mCurrentLastVisiblePosition = 0;
  }

  public RecyclerBinder(ComponentContext c) {
    this(c, 4f, new LinearLayoutInfo(c, VERTICAL, false), null);
  }

  public RecyclerBinder(ComponentContext c, LayoutInfo layoutInfo) {
    this(c, 4f, layoutInfo, null);
  }

  /**
   * Update the item at index position. The {@link RecyclerView} will only be notified of the item
   * being updated after a layout calculation has been completed for the new {@link Component}.
   */
  @UiThread
  public final void updateItemAtAsync(int position, ComponentInfo componentInfo) {
    ThreadUtils.assertMainThread();

    // If the binder has not been measured yet we simply fall back on the sync implementation as
    // nothing will really happen until we compute the first range.
    if (!mIsMeasured.get()) {
      updateItemAt(position, componentInfo);
      return;
    }

    //TODO t15827349 implement async operations in RecyclerBinder.
  }

  /**
   * Inserts an item at position. The {@link RecyclerView} will only be notified of the item being
   * inserted after a layout calculation has been completed for the new {@link Component}.
   */
  @UiThread
  public final void insertItemAtAsync(int position, ComponentInfo componentInfo) {
