/**
 * Copyright (c) 2017-present, Facebook, Inc.
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

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.util.Log;
import android.view.ViewGroup;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentInfo;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.EventHandler;
import com.facebook.litho.LithoView;
import com.facebook.litho.MeasureComparisonUtils;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.ThreadUtils;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.utils.DisplayListPrefetcherUtils;
import com.facebook.litho.utils.IncrementalMountUtils;

import static android.support.v7.widget.OrientationHelper.HORIZONTAL;
import static android.support.v7.widget.OrientationHelper.VERTICAL;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.facebook.litho.MeasureComparisonUtils.isMeasureSpecCompatible;

/**
 * This binder class is used to asynchronously layout Components given a list of {@link Component}
 * and attaching them to a {@link RecyclerSpec}.
 */
@ThreadSafe
public class RecyclerBinder implements
    Binder<RecyclerView>,
    LayoutInfo.ComponentInfoCollection,
    HasStickyHeader {
  public static final float DEFAULT_RANGE_RATIO = 4f;

  private static final int UNINITIALIZED = -1;
  private static final Size sDummySize = new Size();
  private static final String TAG = RecyclerBinder.class.getSimpleName();

  @GuardedBy("this")
  private final List<ComponentTreeHolder> mComponentTreeHolders;
  private final LayoutInfo mLayoutInfo;
  private final RecyclerView.Adapter mInternalAdapter;
  private final ComponentContext mComponentContext;
  private final RangeScrollListener mRangeScrollListener = new RangeScrollListener();
  private final LayoutHandlerFactory mLayoutHandlerFactory;
  private final boolean mUseNewIncrementalMount;
  private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());

  // Data structure to be used to hold Components and ComponentTreeHolders before adding them to
  // the RecyclerView. This happens in the case of inserting something inside the current working
  // range.
  //TODO t15827349
  private final List<ComponentTreeHolder> mPendingComponentTreeHolders;
  private final float mRangeRatio;
  private final AtomicBoolean mIsMeasured = new AtomicBoolean(false);
  private final AtomicBoolean mRequiresRemeasure = new AtomicBoolean(false);
  private final Runnable mRemeasureRunnable = new Runnable() {
    @Override
    public void run() {
      if (mReMeasureEventEventHandler != null) {
        mReMeasureEventEventHandler.dispatchEvent(new ReMeasureEvent());
      }
    }
  };

  private int mLastWidthSpec = UNINITIALIZED;
  private int mLastHeightSpec = UNINITIALIZED;
  private Size mMeasuredSize;
  private RecyclerView mMountedView;
  private int mCurrentFirstVisiblePosition;
  private int mCurrentLastVisiblePosition;
  private RangeCalculationResult mRange;
  private StickyHeaderController mStickyHeaderController;
  private boolean mCanPrefetchDisplayLists;
  private EventHandler<ReMeasureEvent> mReMeasureEventEventHandler;

  public RecyclerBinder(
      ComponentContext componentContext,
      float rangeRatio,
      LayoutInfo layoutInfo) {
    this(componentContext, rangeRatio, layoutInfo, null, false, false);
  }

  public RecyclerBinder(
      ComponentContext componentContext,
      float rangeRatio,
      LayoutInfo layoutInfo,
      @Nullable LayoutHandlerFactory layoutHandlerFactory) {
    this(componentContext, rangeRatio, layoutInfo, layoutHandlerFactory, false, false);
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
   * @param useNewIncrementalMount
   * @param canPrefetchDisplayLists
   */
  public RecyclerBinder(
      ComponentContext componentContext,
      float rangeRatio,
      LayoutInfo layoutInfo,
      @Nullable LayoutHandlerFactory layoutHandlerFactory,
      boolean useNewIncrementalMount,
      boolean canPrefetchDisplayLists) {
    mComponentContext = componentContext;
    mUseNewIncrementalMount = useNewIncrementalMount;
    mComponentTreeHolders = new ArrayList<>();
    mPendingComponentTreeHolders = new ArrayList<>();
    mInternalAdapter = new InternalAdapter();

    mRangeRatio = rangeRatio;
    mLayoutInfo = layoutInfo;
    mLayoutHandlerFactory = layoutHandlerFactory;
    mCurrentFirstVisiblePosition = mCurrentLastVisiblePosition = 0;
    mCanPrefetchDisplayLists = canPrefetchDisplayLists;
  }

  public RecyclerBinder(ComponentContext c) {
    this(c, DEFAULT_RANGE_RATIO, new LinearLayoutInfo(c, VERTICAL, false), null, false, false);
  }

  public RecyclerBinder(ComponentContext c, LayoutInfo layoutInfo) {
    this(c, DEFAULT_RANGE_RATIO, layoutInfo, null, false, false);
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
            null,
        mCanPrefetchDisplayLists);
    final boolean computeLayout;
    final int childrenWidthSpec, childrenHeightSpec;
    synchronized (this) {
      mComponentTreeHolders.add(position, holder);

      childrenWidthSpec = getActualChildrenWidthSpec(holder);
      childrenHeightSpec = getActualChildrenHeightSpec(holder);

      if (mIsMeasured.get()) {
        if (mRange == null && ! mRequiresRemeasure.get()) {
          initRange(
              mMeasuredSize.width,
              mMeasuredSize.height,
              position,
              childrenWidthSpec,
              childrenHeightSpec,
              mLayoutInfo.getScrollDirection());

          computeLayout = false;
        } else if (mRequiresRemeasure.get()) {
          requestUpdate();
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

  private void requestUpdate() {
    if (mMountedView != null) {
      mMainThreadHandler.removeCallbacks(mRemeasureRunnable);
      mMountedView.removeCallbacks(mRemeasureRunnable);
      ViewCompat.postOnAnimation(mMountedView, mRemeasureRunnable);
    } else {
      // We are not mounted but we still need to post this. Just post on the main thread.
      mMainThreadHandler.removeCallbacks(mRemeasureRunnable);
      mMainThreadHandler.post(mRemeasureRunnable);
    }
  }

  /**
   * Inserts the new items starting from position. The {@link RecyclerView} gets notified
   * immediately about the new item being inserted.
   * The ComponentInfo contains the component that will be inserted in the Binder and extra info
   * like isSticky or spanCount.
   */
  @UiThread
  public final void insertRangeAt(int position, List<ComponentInfo> componentInfos) {
    ThreadUtils.assertMainThread();

    for (int i = 0, size = componentInfos.size(); i < size; i++) {

      synchronized (this) {
        final ComponentInfo componentInfo = componentInfos.get(i);
        final ComponentTreeHolder holder = ComponentTreeHolder.acquire(
            componentInfo,
            mLayoutHandlerFactory != null ?
                mLayoutHandlerFactory.createLayoutCalculationHandler(componentInfo) :
                null,
            mCanPrefetchDisplayLists);

        mComponentTreeHolders.add(position + i, holder);

        if (mRange == null && mIsMeasured.get()) {
          initRange(
              mMeasuredSize.width,
              mMeasuredSize.height,
              position,
              getActualChildrenWidthSpec(holder),
              getActualChildrenHeightSpec(holder),
              mLayoutInfo.getScrollDirection());
        }
      }
    }
    mInternalAdapter.notifyItemRangeInserted(position, componentInfos.size());

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
   * Updates the range of items starting at position. The {@link RecyclerView} gets notified
   * immediately about the item being updated.
   */
  @UiThread
  public final void updateRangeAt(int position, List<ComponentInfo> componentInfos) {
    ThreadUtils.assertMainThread();

    for (int i = 0, size = componentInfos.size(); i < size; i++) {

      synchronized (this) {
        mComponentTreeHolders.get(position + i).setComponentInfo(componentInfos.get(i));
      }
    }
    mInternalAdapter.notifyItemRangeChanged(position, componentInfos.size());

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
   * Removes count items starting from position.
   */
  @UiThread
  public final void removeRangeAt(int position, int count) {
    ThreadUtils.assertMainThread();
    synchronized (this) {
      for (int i = 0; i < count; i++) {
        final ComponentTreeHolder holder = mComponentTreeHolders.remove(position);
        holder.release();
      }
    }
    mInternalAdapter.notifyItemRangeRemoved(position, count);

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
  public final synchronized ComponentInfo getComponentInfoAt(int position) {
    return mComponentTreeHolders.get(position).getComponentInfo();
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
   * @param reMeasureEventHandler the EventHandler to invoke in order to trigger a re-measure.
   */
  @Override
  public synchronized void measure(
      Size outSize,
      int widthSpec,
      int heightSpec,
      EventHandler<ReMeasureEvent> reMeasureEventHandler) {
    final int scrollDirection = mLayoutInfo.getScrollDirection();

    switch (scrollDirection) {
      case HORIZONTAL:
        if (SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED) {
          throw new IllegalStateException(
              "Width mode has to be EXACTLY OR AT MOST for an horizontal scrolling RecyclerView");
        }
        break;

      case VERTICAL:
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

    if (mLastWidthSpec != UNINITIALIZED && !mRequiresRemeasure.get()) {
      switch (scrollDirection) {
        case VERTICAL:
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
    final boolean canMeasure = reMeasureEventHandler != null;

    switch (scrollDirection) {
      case OrientationHelper.VERTICAL:
        if (!canMeasure && SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED) {
          throw new IllegalStateException("Can't use Unspecified width on a vertical scrolling " +
              "Recycler if dynamic measurement is not allowed");
        }

        outSize.height = SizeSpec.getSize(heightSpec);

        if (SizeSpec.getMode(widthSpec) == SizeSpec.EXACTLY || !canMeasure) {
          outSize.width = SizeSpec.getSize(widthSpec);
          mReMeasureEventEventHandler = null;
          mRequiresRemeasure.set(false);
        } else if (mRange != null
            && (SizeSpec.getMode(widthSpec) == SizeSpec.AT_MOST
            || SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED)) {
          outSize.width = mRange.measuredSize;
          mReMeasureEventEventHandler = null;
          mRequiresRemeasure.set(false);
        } else {
          outSize.width = 0;
          mRequiresRemeasure.set(true);
          mReMeasureEventEventHandler = reMeasureEventHandler;
        }

        break;
      case OrientationHelper.HORIZONTAL:
        if (!canMeasure && SizeSpec.getMode(heightSpec) == SizeSpec.UNSPECIFIED) {
          throw new IllegalStateException("Can't use Unspecified height on an horizontal " +
              "scrolling Recycler if dynamic measurement is not allowed");
        }

        outSize.width = SizeSpec.getSize(widthSpec);

        if (SizeSpec.getMode(heightSpec) == SizeSpec.EXACTLY || !canMeasure) {
          outSize.height = SizeSpec.getSize(heightSpec);
          mReMeasureEventEventHandler = null;
          mRequiresRemeasure.set(false);
        } else if (mRange != null
            && (SizeSpec.getMode(heightSpec) == SizeSpec.AT_MOST
            || SizeSpec.getMode(heightSpec) == SizeSpec.UNSPECIFIED)) {
          outSize.height = mRange.measuredSize;
          mReMeasureEventEventHandler = null;
          mRequiresRemeasure.set(false);
        } else {
          outSize.height = 0;
          mRequiresRemeasure.set(true);
          mReMeasureEventEventHandler = reMeasureEventHandler;
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
    mRange.measuredSize = scrollDirection == HORIZONTAL ? size.height : size.width;
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
          SizeSpec.makeSizeSpec(height, SizeSpec.EXACTLY),
          mReMeasureEventEventHandler);
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

    mLayoutInfo.setComponentInfoCollection(this);

    if (mCurrentFirstVisiblePosition != RecyclerView.NO_POSITION &&
        mCurrentFirstVisiblePosition > 0) {
      view.scrollToPosition(mCurrentFirstVisiblePosition);
    }

    enableStickyHeader(mMountedView);
  }

  private void enableStickyHeader(RecyclerView recyclerView) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      // Sticky header needs view translation APIs which are not available in Gingerbread and below.
      Log.w(TAG, "Sticky header is supported only on ICS (API14) and above");
      return;
    }
    if (recyclerView == null) {
      return;
    }
    RecyclerViewWrapper recyclerViewWrapper = RecyclerViewWrapper.getParentWrapper(recyclerView);
    if (recyclerViewWrapper == null) {
      return;
    }
    if (mStickyHeaderController == null) {
      mStickyHeaderController = new StickyHeaderController(this);
    }

    mStickyHeaderController.init(recyclerViewWrapper);
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
    if (mStickyHeaderController != null) {
      mStickyHeaderController.reset();
    }
    view.removeOnScrollListener(mRangeScrollListener);
    view.setAdapter(null);
    view.setLayoutManager(null);
    mLayoutInfo.setComponentInfoCollection(null);
  }

  @UiThread
  public void scrollToPosition(int position) {
    if (mMountedView == null) {
      mCurrentFirstVisiblePosition = position;
      return;
    }

    mMountedView.scrollToPosition(position);
  }

  @GuardedBy("this")
  private boolean isCompatibleSize(int widthSpec, int heightSpec) {
    final int scrollDirection = mLayoutInfo.getScrollDirection();

    if (mLastWidthSpec != UNINITIALIZED) {

      switch (scrollDirection) {
        case HORIZONTAL:
          return isMeasureSpecCompatible(
              mLastHeightSpec,
              heightSpec,
              mMeasuredSize.height);
        case VERTICAL:
          return isMeasureSpecCompatible(
              mLastWidthSpec,
              widthSpec,
              mMeasuredSize.width);
      }
    }

    return false;
  }

  @Override
  public int findFirstVisibleItemPosition() {
    return mLayoutInfo.findFirstVisiblePosition();
  }

  @Override
  public int findLastVisibleItemPosition() {
    return mLayoutInfo.findLastVisiblePosition();
  }

  @Override
  @UiThread
  @GuardedBy("this")
  public boolean isSticky(int position) {
    return mComponentTreeHolders.get(position).getComponentInfo().isSticky();
  }

  @Override
  @UiThread
  @GuardedBy("this")
  public boolean isValidPosition(int position) {
    return position >= 0 && position < mComponentTreeHolders.size();
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
      final int childrenWidthSpec, childrenHeightSpec;

      synchronized (this) {
        // Someone modified the ComponentsTreeHolders while we were computing this range. We
        // can just bail as another range will be computed.
        if (treeHoldersSize != mComponentTreeHolders.size()) {
          return;
        }

        holder = mComponentTreeHolders.get(i);
        childrenWidthSpec = getActualChildrenWidthSpec(holder);
        childrenHeightSpec = getActualChildrenHeightSpec(holder);
      }

      if (i >= rangeStart && i <= rangeEnd) {
        if (!holder.isTreeValid()) {
          holder.computeLayoutAsync(mComponentContext, childrenWidthSpec, childrenHeightSpec);
        }
      } else if (holder.isTreeValid() && !holder.getComponentInfo().isSticky()) {
        holder.acquireStateHandlerAndReleaseTree();
      }
    }
  }

  @GuardedBy("this")
  private int getActualChildrenWidthSpec(final ComponentTreeHolder treeHolder) {
    if (mIsMeasured.get() && !mRequiresRemeasure.get()) {
      return mLayoutInfo.getChildWidthSpec(
          SizeSpec.makeSizeSpec(mMeasuredSize.width, SizeSpec.EXACTLY),
          treeHolder.getComponentInfo());
    }

    return mLayoutInfo.getChildWidthSpec(mLastWidthSpec, treeHolder.getComponentInfo());
  }

  @GuardedBy("this")
  private int getActualChildrenHeightSpec(final ComponentTreeHolder treeHolder) {
    if (mIsMeasured.get() && !mRequiresRemeasure.get()) {
      return mLayoutInfo.getChildHeightSpec(
          SizeSpec.makeSizeSpec(mMeasuredSize.height, SizeSpec.EXACTLY),
          treeHolder.getComponentInfo());
    }

    return mLayoutInfo.getChildHeightSpec(mLastHeightSpec, treeHolder.getComponentInfo());
  }

  private class RangeScrollListener extends RecyclerView.OnScrollListener {

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

      if (!mUseNewIncrementalMount) {
        IncrementalMountUtils.performIncrementalMount(recyclerView);
      }

      final int firstVisiblePosition = mLayoutInfo.findFirstVisiblePosition();
      final int lastVisiblePosition = mLayoutInfo.findLastVisiblePosition();

      if (firstVisiblePosition < 0 || lastVisiblePosition < 0) {
        return;
      }

      if (firstVisiblePosition != mCurrentFirstVisiblePosition
          || lastVisiblePosition != mCurrentLastVisiblePosition) {
        onNewVisibleRange(firstVisiblePosition, lastVisiblePosition);
      }

      if (mCanPrefetchDisplayLists) {
        DisplayListPrefetcherUtils.prefetchDisplayLists(recyclerView);
      }
    }
  }

  private class LithoViewHolder extends RecyclerView.ViewHolder {

    public LithoViewHolder(LithoView lithoView) {
      super(lithoView);

      switch (mLayoutInfo.getScrollDirection()) {
        case OrientationHelper.VERTICAL:
          lithoView.setLayoutParams(
              new RecyclerView.LayoutParams(
                  ViewGroup.LayoutParams.MATCH_PARENT,
                  WRAP_CONTENT));
          break;
        default:
          lithoView.setLayoutParams(
              new RecyclerView.LayoutParams(
                  WRAP_CONTENT,
                  ViewGroup.LayoutParams.MATCH_PARENT));
      }
    }
  }

  private class InternalAdapter extends RecyclerView.Adapter {

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      return new LithoViewHolder(
          new LithoView(mComponentContext, null, mUseNewIncrementalMount));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
      final LithoView lithoView = (LithoView) holder.itemView;
      // We can ignore the synchronization here. We'll only add to this from the UiThread.
      // This read only happens on the UiThread as well and we are never writing this here.
      final ComponentTreeHolder componentTreeHolder = mComponentTreeHolders.get(position);
      final int childrenWidthSpec = getActualChildrenWidthSpec(componentTreeHolder);
      final int childrenHeightSpec = getActualChildrenHeightSpec(componentTreeHolder);
      if (!componentTreeHolder.isTreeValid()) {
        componentTreeHolder
            .computeLayoutSync(mComponentContext, childrenWidthSpec, childrenHeightSpec, null);
      }

      lithoView.setComponentTree(componentTreeHolder.getComponentTree());
    }

    @Override
    public int getItemCount() {
      // We can ignore the synchronization here. We'll only add to this from the UiThread.
      // This read only happens on the UiThread as well and we are never writing this here.
      return mComponentTreeHolders.size();
    }
  }
}
