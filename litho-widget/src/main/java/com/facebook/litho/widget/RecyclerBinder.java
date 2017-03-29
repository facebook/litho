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
    ThreadUtils.assertMainThread();

    // If the binder has not been measured yet we simply fall back on the sync implementation as
    // nothing will really happen until we compute the first range.
    if (!mIsMeasured.get()) {
      insertItemAt(position, componentInfo);
      return;
    }

    //TODO t15827349 implement async operations in RecyclerBinder.
  }

  /**
   * Moves an item from fromPosition to toPostion. If there are other pending operations on this
   * binder this will only be executed when all the operations have been completed (to ensure index
   * consistency).
   */
  @UiThread
  public final void moveItemAsync(int fromPosition, int toPosition) {
    ThreadUtils.assertMainThread();

    // If the binder has not been measured yet we simply fall back on the sync implementation as
    // nothing will really happen until we compute the first range.
    if (!mIsMeasured.get()) {
      moveItem(fromPosition, toPosition);
      return;
    }

    //TODO t15827349 implement async operations in RecyclerBinder.
  }

  /**
   * Removes an item from position. If there are other pending operations on this binder this will
   * only be executed when all the operations have been completed (to ensure index consistency).
   */
  @UiThread
  public final void removeItemAtAsync(int position) {
    ThreadUtils.assertMainThread();

    // If the binder has not been measured yet we simply fall back on the sync implementation as
    // nothing will really happen until we compute the first range.
    if (!mIsMeasured.get()) {
      removeItemAt(position);
      return;
    }

    //TODO t15827349 implement async operations in RecyclerBinder.
  }

  /**
   * See {@link RecyclerBinder#insertItemAt(int, ComponentInfo)}.
   */
  @UiThread
  public final void insertItemAt(int position, Component component) {
    insertItemAt(position, ComponentInfo.create().component(component).build());
  }

  /**
   * Inserts a new item at position. The {@link RecyclerView} gets notified immediately about the
   * new item being inserted. If the item's position falls within the currently visible range, the
   * layout is immediately computed on the] UiThread.
   * The ComponentInfo contains the component that will be inserted in the Binder and extra info
   * like isSticky or spanCount.
   */
  @UiThread
  public final void insertItemAt(int position, ComponentInfo componentInfo) {
    ThreadUtils.assertMainThread();

    final ComponentTreeHolder holder = ComponentTreeHolder.acquire(
        componentInfo,
        mLayoutHandlerFactory != null ?
            mLayoutHandlerFactory.createLayoutCalculationHandler(componentInfo) :
            null);
    final boolean computeLayout;
    final int childrenWidthSpec, childrenHeightSpec;
    synchronized (this) {
      mComponentTreeHolders.add(position, holder);

      childrenWidthSpec = getActualChildrenWidthSpec(holder);
      childrenHeightSpec = getActualChildrenHeightSpec(holder);

      if (mIsMeasured.get()) {
        if (mRange == null) {
          initRange(
              mMeasuredSize.width,
              mMeasuredSize.height,
              position,
              childrenWidthSpec,
              childrenHeightSpec,
              mLayoutInfo.getScrollDirection());

          computeLayout = false;
        } else {
          computeLayout = position >= mCurrentFirstVisiblePosition &&
              position < mCurrentFirstVisiblePosition + mRange.estimatedViewportCount;
        }
      } else {
        computeLayout = false;
      }
    }

    if (computeLayout) {
      holder.computeLayoutSync(mComponentContext, childrenWidthSpec, childrenHeightSpec, null);
    }
    mInternalAdapter.notifyItemInserted(position);

    computeRange(mCurrentFirstVisiblePosition, mCurrentLastVisiblePosition);
  }

  /**
   * See {@link RecyclerBinder#updateItemAt(int, Component)}.
   */
  @UiThread
  public final void updateItemAt(int position, Component component) {
    updateItemAt(position, ComponentInfo.create().component(component).build());
  }

  /**
   * Updates the item at position. The {@link RecyclerView} gets notified immediately about the item
   * being updated. If the item's position falls within the currently visible range, the layout is
   * immediately computed on the UiThread.
   */
  @UiThread
  public final void updateItemAt(int position, ComponentInfo componentInfo) {
    ThreadUtils.assertMainThread();

    final ComponentTreeHolder holder;
    final boolean shouldComputeLayout;
    final int childrenWidthSpec, childrenHeightSpec;
    synchronized (this) {
      holder = mComponentTreeHolders.get(position);
      shouldComputeLayout = mRange != null && position >= mCurrentFirstVisiblePosition &&
          position < mCurrentFirstVisiblePosition + mRange.estimatedViewportCount;

      holder.setComponentInfo(componentInfo);

      childrenWidthSpec = getActualChildrenWidthSpec(holder);
      childrenHeightSpec = getActualChildrenHeightSpec(holder);
    }

    // If we are updating an item that is currently visible we need to calculate a layout
    // synchronously.
    if (shouldComputeLayout) {
      holder.computeLayoutSync(mComponentContext, childrenWidthSpec, childrenHeightSpec, null);
    }

    mInternalAdapter.notifyItemChanged(position);
    computeRange(mCurrentFirstVisiblePosition, mCurrentLastVisiblePosition);
  }

  /**
   * Moves an item from fromPosition to toPosition. If the new position of the item is within the
   * currently visible range, a layout is calculated immediately on the UI Thread.
   */
  @UiThread
  public final void moveItem(int fromPosition, int toPosition) {
    ThreadUtils.assertMainThread();

    final ComponentTreeHolder holder;
    final boolean isNewPositionInRange, isNewPositionInVisibleRange;
    final int childrenWidthSpec, childrenHeightSpec;
    synchronized (this) {
      holder = mComponentTreeHolders.remove(fromPosition);
      mComponentTreeHolders.add(toPosition, holder);
      final int mRangeSize = mRange != null ? mRange.estimatedViewportCount : -1;

      isNewPositionInRange = mRangeSize > 0 &&
          toPosition >= mCurrentFirstVisiblePosition - (mRangeSize * mRangeRatio) &&
          toPosition <= mCurrentFirstVisiblePosition + mRangeSize + (mRangeSize * mRangeRatio);

      isNewPositionInVisibleRange = mRangeSize > 0 &&
          toPosition >= mCurrentFirstVisiblePosition &&
          toPosition <= mCurrentFirstVisiblePosition + mRangeSize;

      childrenWidthSpec = getActualChildrenWidthSpec(holder);
      childrenHeightSpec = getActualChildrenHeightSpec(holder);
    }
    final boolean isTreeValid = holder.isTreeValid();

    if (isTreeValid && !isNewPositionInRange) {
      holder.acquireStateHandlerAndReleaseTree();
    } else if (isNewPositionInVisibleRange && !isTreeValid) {
      holder.computeLayoutSync(mComponentContext, childrenWidthSpec, childrenHeightSpec, null);
    }

    mInternalAdapter.notifyItemMoved(fromPosition, toPosition);
    computeRange(mCurrentFirstVisiblePosition, mCurrentLastVisiblePosition);
  }

  /**
   * Removes an item from index position.
   */
  @UiThread
  public final void removeItemAt(int position) {
    ThreadUtils.assertMainThread();
    final ComponentTreeHolder holder;
    synchronized (this) {
      holder = mComponentTreeHolders.remove(position);
    }
    mInternalAdapter.notifyItemRemoved(position);
    holder.release();
    computeRange(mCurrentFirstVisiblePosition, mCurrentLastVisiblePosition);
  }

  /**
   * Returns the {@link ComponentTree} for the item at index position. TODO 16212132 remove
   * getComponentAt from binder
   */
  @Nullable
  @Override
  public final synchronized ComponentTree getComponentAt(int position) {
    return mComponentTreeHolders.get(position).getComponentTree();
  }

  @Override
  public void bind(RecyclerView view) {
    // Nothing to do here.
  }

  @Override
  public void unbind(RecyclerView view) {
    // Nothing to do here.
  }

  /**
   * A component mounting a RecyclerView can use this method to determine its size. A Recycler that
   * scrolls horizontally will leave the width unconstrained and will measure its children with a
   * sizeSpec for the height matching the heightSpec passed to this method.
   *
   * If padding is defined on the parent component it should be subtracted from the parent size
   * specs before passing them to this method.
   *
   * Currently we can't support the equivalent of MATCH_PARENT on the scrollDirection (so for
   * example we don't support MATCH_PARENT on width in an horizontal RecyclerView). This is mainly
   * because we don't have the equivalent of LayoutParams in components. We can extend the api of
   * the binder in the future to provide some more layout hints in order to support this.
   *
   * @param outSize will be populated with the measured dimensions for this Binder.
   * @param widthSpec the widthSpec to be used to measure the RecyclerView.
   * @param heightSpec the heightSpec to be used to measure the RecyclerView.
   */
  @Override
  public synchronized void measure(Size outSize, int widthSpec, int heightSpec) {
    final int scrollDirection = mLayoutInfo.getScrollDirection();

    switch (scrollDirection) {
      case OrientationHelper.HORIZONTAL:
        if (SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED) {
          throw new IllegalStateException(
              "Width mode has to be EXACTLY OR AT MOST for an horizontal scrolling RecyclerView");
        }
        break;

      case OrientationHelper.VERTICAL:
        if (SizeSpec.getMode(heightSpec) == SizeSpec.UNSPECIFIED) {
          throw new IllegalStateException(
              "Height mode has to be EXACTLY OR AT MOST for a vertical scrolling RecyclerView");
        }
        break;

      default:
        throw new UnsupportedOperationException(
            "The orientation defined by LayoutInfo should be" +
                " either OrientationHelper.HORIZONTAL or OrientationHelper.VERTICAL");
    }

    if (mLastWidthSpec != UNINITIALIZED) {
      switch (scrollDirection) {
        case OrientationHelper.VERTICAL:
          if (MeasureComparisonUtils.isMeasureSpecCompatible(
              mLastWidthSpec,
              widthSpec,
              mMeasuredSize.width)) {
            outSize.width = mMeasuredSize.width;
            outSize.height = SizeSpec.getSize(heightSpec);

            return;
          }
          break;
        default:
          if (MeasureComparisonUtils.isMeasureSpecCompatible(
              mLastHeightSpec,
              heightSpec,
              mMeasuredSize.height)) {
            outSize.width = SizeSpec.getSize(widthSpec);
            outSize.height = mMeasuredSize.height;

            return;
          }
      }

      mIsMeasured.set(false);
      invalidateLayoutData();
    }

    // We have never measured before or the measures are not valid so we need to measure now.
    mLastWidthSpec = widthSpec;
    mLastHeightSpec = heightSpec;

    // Width and Height specs here are all for span size 1
    mChildrenWidthSpec = mLayoutInfo.getChildWidthSpec(widthSpec);
    mChildrenHeightSpec = mLayoutInfo.getChildHeightSpec(heightSpec);

    // We now need to compute the size of the non scrolling side. We try to do this by using the
    // calculated range (if we have one) or computing one.
    if (mRange == null && mCurrentFirstVisiblePosition < mComponentTreeHolders.size()) {
      initRange(
          SizeSpec.getSize(widthSpec),
          SizeSpec.getSize(heightSpec),
          mCurrentFirstVisiblePosition,
          getActualChildrenWidthSpec(mComponentTreeHolders.get(mCurrentFirstVisiblePosition)),
          getActualChildrenHeightSpec(mComponentTreeHolders.get(mCurrentFirstVisiblePosition)),
          scrollDirection);
    }

    // At this point we might still not have a range. In this situation we should return the best
    // size we can detect from the size spec and update it when the first item comes in.
    // TODO 16207395.
    switch (scrollDirection) {
      case OrientationHelper.VERTICAL:
        if (mRange != null
            && (SizeSpec.getMode(widthSpec) == SizeSpec.AT_MOST
            || SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED)) {
          outSize.width = mRange.measuredSize;
        } else {
          outSize.width = SizeSpec.getSize(widthSpec);
        }
        outSize.height = SizeSpec.getSize(heightSpec);
        break;
      case OrientationHelper.HORIZONTAL:
        outSize.width = SizeSpec.getSize(widthSpec);
        if (mRange != null
            && (SizeSpec.getMode(heightSpec) == SizeSpec.AT_MOST
            || SizeSpec.getMode(heightSpec) == SizeSpec.UNSPECIFIED)) {
          outSize.height = mRange.measuredSize;
        } else {
          outSize.height = SizeSpec.getSize(heightSpec);
        }
        break;
    }

    mMeasuredSize = new Size(outSize.width, outSize.height);
    mIsMeasured.set(true);

    if (mRange != null) {
      computeRange(mCurrentFirstVisiblePosition, mCurrentLastVisiblePosition);
    }
  }

  /**
   * Gets the number of items in this binder.
   */
  public int getItemCount() {
    return mInternalAdapter.getItemCount();
  }

  @GuardedBy("this")
  private void invalidateLayoutData() {
    mRange = null;
    for (int i = 0, size = mComponentTreeHolders.size(); i < size; i++) {
      mComponentTreeHolders.get(i).invalidateTree();
    }
  }

  @GuardedBy("this")
  private void initRange(
      int width,
      int height,
      int rangeStart,
      int childrenWidthSpec,
      int childrenHeightSpec,
      int scrollDirection) {
    int nextIndexToPrepare = rangeStart;

    if (nextIndexToPrepare >= mComponentTreeHolders.size()) {
      return;
    }

    final Size size = new Size();
    final ComponentTreeHolder holder = mComponentTreeHolders.get(nextIndexToPrepare);
    holder.computeLayoutSync(mComponentContext, childrenWidthSpec, childrenHeightSpec, size);

    final int rangeSize = Math.max(
        mLayoutInfo.approximateRangeSize(
            size.width,
            size.height,
            width,
            height),
        1);

    mRange = new RangeCalculationResult();
    mRange.measuredSize = scrollDirection == OrientationHelper.HORIZONTAL
        ? size.height
        : size.width;
    mRange.estimatedViewportCount = rangeSize;
  }

  /**
   * This should be called when the owner {@link Component}'s onBoundsDefined is called. It will
   * inform the binder of the final measured size. The binder might decide to re-compute its
   * children layouts if the measures provided here are not compatible with the ones receive in
   * onMeasure.
   */
  @Override
  public synchronized void setSize(int width, int height) {
    if (mLastWidthSpec == UNINITIALIZED || !isCompatibleSize(
        SizeSpec.makeSizeSpec(width, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(height, SizeSpec.EXACTLY))) {
      measure(
          sDummySize,
          SizeSpec.makeSizeSpec(width, SizeSpec.EXACTLY),
          SizeSpec.makeSizeSpec(height, SizeSpec.EXACTLY));
    }
  }

  /**
   * Call from the owning {@link Component}'s onMount. This is where the adapter is assigned to the
   * {@link RecyclerView}.
   *
   * @param view the {@link RecyclerView} being mounted.
   */
  @UiThread
  @Override
  public void mount(RecyclerView view) {
    ThreadUtils.assertMainThread();

    if (mMountedView == view) {
      return;
    }

    if (mMountedView != null) {
      unmount(mMountedView);
    }

    mMountedView = view;

    view.setLayoutManager(mLayoutInfo.getLayoutManager());
    view.setAdapter(mInternalAdapter);
    view.addOnScrollListener(mRangeScrollListener);
    if (mLayoutInfo.getSpanCount() > 1) {
      ((GridLayoutManager) mLayoutInfo.getLayoutManager()).setSpanSizeLookup(mGridSpanSizeLookup);
    }
    if (mCurrentFirstVisiblePosition != RecyclerView.NO_POSITION &&
        mCurrentFirstVisiblePosition > 0) {
      view.scrollToPosition(mCurrentFirstVisiblePosition);
    }
  }

  /**
   * Call from the owning {@link Component}'s onUnmount. This is where the adapter is removed from
   * the {@link RecyclerView}.
   *
   * @param view the {@link RecyclerView} being unmounted.
   */
  @UiThread
  @Override
  public void unmount(RecyclerView view) {
    ThreadUtils.assertMainThread();

    // We might have already unmounted this view when calling mount with a different view. In this
    // case we can just return here.
    if (mMountedView != view) {
      return;
    }

    mMountedView = null;
    view.removeOnScrollListener(mRangeScrollListener);
    view.setAdapter(null);
    view.setLayoutManager(null);
    if (mLayoutInfo.getSpanCount() > 1) {
      ((GridLayoutManager) mLayoutInfo.getLayoutManager()).setSpanSizeLookup(null);
    }
  }

  @GuardedBy("this")
  private boolean isCompatibleSize(int widthSpec, int heightSpec) {
    final int scrollDirection = mLayoutInfo.getScrollDirection();

    if (mLastWidthSpec != UNINITIALIZED) {

      switch (scrollDirection) {
        case OrientationHelper.HORIZONTAL:
          return isMeasureSpecCompatible(
              mLastHeightSpec,
              heightSpec,
              mMeasuredSize.height);
        case OrientationHelper.VERTICAL:
          return isMeasureSpecCompatible(
              mLastWidthSpec,
              widthSpec,
              mMeasuredSize.width);
      }
    }

    return false;
  }

  private static class RangeCalculationResult {

    // The estimated number of items needed to fill the viewport.
    private int estimatedViewportCount;
    // The size computed for the first Component.
    private int measuredSize;
  }

  @VisibleForTesting
  void onNewVisibleRange(int firstVisiblePosition, int lastVisiblePosition) {
    mCurrentFirstVisiblePosition = firstVisiblePosition;
    mCurrentLastVisiblePosition = lastVisiblePosition;
    computeRange(firstVisiblePosition, lastVisiblePosition);
  }

  private void computeRange(int firstVisible, int lastVisible) {
    final int rangeSize;
    final int rangeStart;
    final int rangeEnd;
    final int treeHoldersSize;

    synchronized (this) {
      if (!mIsMeasured.get() || mRange == null) {
        return;
      }

      rangeSize = Math.max(mRange.estimatedViewportCount, lastVisible - firstVisible);
      rangeStart = firstVisible - (int) (rangeSize * mRangeRatio);
      rangeEnd = firstVisible + rangeSize + (int) (rangeSize * mRangeRatio);
      treeHoldersSize = mComponentTreeHolders.size();
    }

    // TODO 16212153 optimize computeRange loop.
    for (int i = 0; i < treeHoldersSize; i++) {
      final ComponentTreeHolder holder;
