/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import static android.support.v7.widget.OrientationHelper.HORIZONTAL;
import static android.support.v7.widget.OrientationHelper.VERTICAL;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.facebook.litho.MeasureComparisonUtils.isMeasureSpecCompatible;
import static com.facebook.litho.widget.RenderInfoViewCreatorController.COMPONENT_VIEW_TYPE;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.EventHandler;
import com.facebook.litho.LayoutHandler;
import com.facebook.litho.LithoView;
import com.facebook.litho.MeasureComparisonUtils;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.ThreadUtils;
import com.facebook.litho.utils.DisplayListUtils;
import com.facebook.litho.viewcompat.ViewCreator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * This binder class is used to asynchronously layout Components given a list of {@link Component}
 * and attaching them to a {@link RecyclerSpec}.
 */
@ThreadSafe
public class RecyclerBinder
    implements Binder<RecyclerView>, LayoutInfo.RenderInfoCollection, HasStickyHeader {

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
  private final @Nullable LithoViewFactory mLithoViewFactory;
  private final ComponentTreeHolderFactory mComponentTreeHolderFactory;
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
  private final Runnable mNotifyDatasetChangedRunnable = new Runnable() {
    @Override
    public void run() {
      mInternalAdapter.notifyDataSetChanged();
    }
  };

  private int mLastWidthSpec = UNINITIALIZED;
  private int mLastHeightSpec = UNINITIALIZED;
  private Size mMeasuredSize;
  private RecyclerView mMountedView;
  private int mCurrentFirstVisiblePosition = RecyclerView.NO_POSITION;
  private int mCurrentLastVisiblePosition = RecyclerView.NO_POSITION;
  private int mCurrentOffset;
  private @Nullable RangeCalculationResult mRange;
  private StickyHeaderController mStickyHeaderController;
  private final boolean mCanPrefetchDisplayLists;
  private final boolean mCanCacheDrawingDisplayLists;
  private EventHandler<ReMeasureEvent> mReMeasureEventEventHandler;

  private final ViewportManager mViewportManager;
  private final ViewportChanged mViewportChangedListener =
      new ViewportChanged() {
        @Override
        public void viewportChanged(
            int firstVisibleIndex,
            int lastVisibleIndex,
            int firstFullyVisibleIndex,
            int lastFullyVisibleIndex,
            boolean dataInRangeIsChanged) {
          onNewVisibleRange(firstVisibleIndex, lastVisibleIndex);
        }

        @Override
        public void lastItemAttached() {
          // no-opt
        }
      };

  @VisibleForTesting
  final RenderInfoViewCreatorController mRenderInfoViewCreatorController =
      new RenderInfoViewCreatorController();

  interface ComponentTreeHolderFactory {
    ComponentTreeHolder create(
        RenderInfo renderInfo,
        LayoutHandler layoutHandler,
        boolean canPrefetchDisplayLists,
        boolean canCacheDrawingDisplayLists);
  }

  static final ComponentTreeHolderFactory DEFAULT_COMPONENT_TREE_HOLDER_FACTORY =
          new ComponentTreeHolderFactory() {
    @Override
    public ComponentTreeHolder create(
        RenderInfo renderInfo,
        LayoutHandler layoutHandler,
        boolean canPrefetchDisplayLists,
        boolean canCacheDrawingDisplayLists) {
      return ComponentTreeHolder.acquire(
          renderInfo,
          layoutHandler,
          canPrefetchDisplayLists,
          canCacheDrawingDisplayLists);
    }
  };

  public static class Builder {

    private float rangeRatio = 4f;
    private LayoutInfo layoutInfo;
    private @Nullable LayoutHandlerFactory layoutHandlerFactory;
    private boolean canPrefetchDisplayLists;
    private boolean canCacheDrawingDisplayLists;
    private ComponentTreeHolderFactory componentTreeHolderFactory =
        DEFAULT_COMPONENT_TREE_HOLDER_FACTORY;
    private ComponentContext componentContext;
    private LithoViewFactory lithoViewFactory;

    /**
     * @param rangeRatio specifies how big a range this binder should try to compute. The range is
     * computed as number of items in the viewport (when the binder is measured) multiplied by the
     * range ratio. The ratio is to be intended in both directions. For example a ratio of 1 means
     * that if there are currently N components on screen, the binder should try to compute the
     * layout for the N components before the first component on screen and for the N components
     * after the last component on screen. If not set, defaults to 4f.
     */
    public Builder rangeRatio(float rangeRatio) {
      this.rangeRatio = rangeRatio;
      return this;
    }

    /**
     * @param layoutInfo an implementation of {@link LayoutInfo} that will expose information about
     * the {@link LayoutManager} this RecyclerBinder will use. If not set, it will default to a
     * vertical list.
     */
    public Builder layoutInfo(LayoutInfo layoutInfo) {
      this.layoutInfo = layoutInfo;
      return this;
    }

    /**
     *
     * @param layoutHandlerFactory the RecyclerBinder will use this layoutHandlerFactory when
     * creating {@link ComponentTree}s in order to specify on which thread layout calculation
     * should happen.
     */
    public Builder layoutHandlerFactory(LayoutHandlerFactory layoutHandlerFactory) {
      this.layoutHandlerFactory = layoutHandlerFactory;
      return this;
    }

    public Builder lithoViewFactory(LithoViewFactory lithoViewFactory) {
      this.lithoViewFactory = lithoViewFactory;
      return this;
    }

    public Builder canPrefetchDisplayLists(boolean canPrefetchDisplayLists) {
      this.canPrefetchDisplayLists = canPrefetchDisplayLists;
      return this;
    }

    public Builder canCacheDrawingDisplayLists(boolean canCacheDrawingDisplayLists) {
      this.canCacheDrawingDisplayLists = canCacheDrawingDisplayLists;
      return this;
    }

    /**
     *
     * @param componentTreeHolderFactory Factory to acquire a new ComponentTreeHolder. Defaults to
     * {@link #DEFAULT_COMPONENT_TREE_HOLDER_FACTORY}.
     */
    public Builder componentTreeHolderFactory(
        ComponentTreeHolderFactory componentTreeHolderFactory) {
      this.componentTreeHolderFactory = componentTreeHolderFactory;
      return this;
    }

    /**
     * @param c The {@link ComponentContext} the RecyclerBinder will use.
     */
    public RecyclerBinder build(ComponentContext c) {
      componentContext = c;

      if (layoutInfo == null) {
        layoutInfo = new LinearLayoutInfo(c, VERTICAL, false);
      }

      return new RecyclerBinder(this);
    }
  }

  private RecyclerBinder(Builder builder) {
    mComponentContext = builder.componentContext;
    mComponentTreeHolderFactory = builder.componentTreeHolderFactory;
    mComponentTreeHolders = new ArrayList<>();
    mPendingComponentTreeHolders = new ArrayList<>();
    mInternalAdapter = new InternalAdapter();

    mRangeRatio = builder.rangeRatio;
    mLayoutInfo = builder.layoutInfo;
    mLayoutHandlerFactory = builder.layoutHandlerFactory;
    mLithoViewFactory = builder.lithoViewFactory;
    mCanPrefetchDisplayLists = builder.canPrefetchDisplayLists;
    mCanCacheDrawingDisplayLists = builder.canCacheDrawingDisplayLists;

    mViewportManager = new ViewportManager(
        mCurrentFirstVisiblePosition,
        mCurrentLastVisiblePosition,
        builder.layoutInfo,
        mMainThreadHandler,
        RecyclerView.SCROLL_STATE_IDLE);
  }

  /**
   * Update the item at index position. The {@link RecyclerView} will only be notified of the item
   * being updated after a layout calculation has been completed for the new {@link Component}.
   */
  @UiThread
  public final void updateItemAtAsync(int position, RenderInfo renderInfo) {
    ThreadUtils.assertMainThread();

    // If the binder has not been measured yet we simply fall back on the sync implementation as
    // nothing will really happen until we compute the first range.
    if (!mIsMeasured.get()) {
      updateItemAt(position, renderInfo);
      return;
    }

    //TODO t15827349 implement async operations in RecyclerBinder.
  }

  /**
   * Inserts an item at position. The {@link RecyclerView} will only be notified of the item being
   * inserted after a layout calculation has been completed for the new {@link Component}.
   */
  @UiThread
  public final void insertItemAtAsync(int position, RenderInfo renderInfo) {
    ThreadUtils.assertMainThread();

    // If the binder has not been measured yet we simply fall back on the sync implementation as
    // nothing will really happen until we compute the first range.
    if (!mIsMeasured.get()) {
      insertItemAt(position, renderInfo);
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
   * See {@link RecyclerBinder#appendItem(RenderInfo)}.
   */
  @UiThread
  public final void appendItem(Component component) {
    insertItemAt(getItemCount(), component);
  }

  /**
   * Inserts a new item at tail. The {@link RecyclerView} gets notified immediately about the
   * new item being inserted. If the item's position falls within the currently visible range, the
   * layout is immediately computed on the] UiThread.
   * The RenderInfo contains the component that will be inserted in the Binder and extra info
   * like isSticky or spanCount.
   */
  @UiThread
  public final void appendItem(RenderInfo renderInfo) {
    insertItemAt(getItemCount(), renderInfo);
  }

  /**
   * See {@link RecyclerBinder#insertItemAt(int, RenderInfo)}.
   */
  @UiThread
  public final void insertItemAt(int position, Component component) {
    insertItemAt(position, ComponentRenderInfo.create().component(component).build());
  }

  /**
   * Inserts a new item at position. The {@link RecyclerView} gets notified immediately about the
   * new item being inserted. If the item's position falls within the currently visible range, the
   * layout is immediately computed on the] UiThread.
   * The RenderInfo contains the component that will be inserted in the Binder and extra info
   * like isSticky or spanCount.
   */
  @UiThread
  public final void insertItemAt(int position, RenderInfo renderInfo) {
    ThreadUtils.assertMainThread();

    final ComponentTreeHolder holder = mComponentTreeHolderFactory.create(
        renderInfo,
        mLayoutHandlerFactory != null ?
            mLayoutHandlerFactory.createLayoutCalculationHandler(renderInfo) :
            null,
        mCanPrefetchDisplayLists,
        mCanCacheDrawingDisplayLists);
    final boolean computeLayout;
    final int childrenWidthSpec, childrenHeightSpec;
    synchronized (this) {
      mComponentTreeHolders.add(position, holder);

      mRenderInfoViewCreatorController.maybeUpdateViewCreatorMappingsOnItemInsert(renderInfo);

      childrenWidthSpec = getActualChildrenWidthSpec(holder);
      childrenHeightSpec = getActualChildrenHeightSpec(holder);

      if (mIsMeasured.get() && holder.getRenderInfo().rendersComponent()) {
        if (mRange == null && !mRequiresRemeasure.get()) {
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
          final int firstVisiblePosition = Math.max(mCurrentFirstVisiblePosition, 0);
          computeLayout = position >= firstVisiblePosition &&
              position < firstVisiblePosition + mRange.estimatedViewportCount;
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

    mViewportManager.setDataChangedIsVisible(
        mViewportManager.isInsertInVisibleRange(
            position, 1, mRange != null ? mRange.estimatedViewportCount : -1));
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
   * The RenderInfo contains the component that will be inserted in the Binder and extra info
   * like isSticky or spanCount.
   */
  @UiThread
  public final void insertRangeAt(int position, List<RenderInfo> renderInfos) {
    ThreadUtils.assertMainThread();

    for (int i = 0, size = renderInfos.size(); i < size; i++) {

      synchronized (this) {
        final RenderInfo renderInfo = renderInfos.get(i);
        final ComponentTreeHolder holder = mComponentTreeHolderFactory.create(
            renderInfo,
            mLayoutHandlerFactory != null ?
                mLayoutHandlerFactory.createLayoutCalculationHandler(renderInfo) :
                null,
            mCanPrefetchDisplayLists,
            mCanCacheDrawingDisplayLists);

        mComponentTreeHolders.add(position + i, holder);
        mRenderInfoViewCreatorController.maybeUpdateViewCreatorMappingsOnItemInsert(renderInfo);

        if (mIsMeasured.get() && holder.getRenderInfo().rendersComponent()) {
          if (mRange == null && !mRequiresRemeasure.get()) {
            initRange(
                mMeasuredSize.width,
                mMeasuredSize.height,
                position + i,
                getActualChildrenWidthSpec(holder),
                getActualChildrenHeightSpec(holder),
                mLayoutInfo.getScrollDirection());
          } else if (mRequiresRemeasure.get()) {
            requestUpdate();
          }
        }
      }
    }
    mInternalAdapter.notifyItemRangeInserted(position, renderInfos.size());

    computeRange(mCurrentFirstVisiblePosition, mCurrentLastVisiblePosition);

    mViewportManager.setDataChangedIsVisible(
        mViewportManager.isInsertInVisibleRange(
            position, renderInfos.size(), mRange != null ? mRange.estimatedViewportCount : -1));
  }

  /**
   * See {@link RecyclerBinder#updateItemAt(int, Component)}.
   */
  @UiThread
  public final void updateItemAt(int position, Component component) {
    updateItemAt(position, ComponentRenderInfo.create().component(component).build());
  }

  /**
   * Updates the item at position. The {@link RecyclerView} gets notified immediately about the item
   * being updated. If the item's position falls within the currently visible range, the layout is
   * immediately computed on the UiThread.
   */
  @UiThread
  public final void updateItemAt(int position, RenderInfo renderInfo) {
    ThreadUtils.assertMainThread();

    final ComponentTreeHolder holder;
    final boolean shouldComputeLayout;
    final int childrenWidthSpec, childrenHeightSpec;
    synchronized (this) {
      holder = mComponentTreeHolders.get(position);
      shouldComputeLayout = mRange != null && position >= mCurrentFirstVisiblePosition &&
          position < mCurrentFirstVisiblePosition + mRange.estimatedViewportCount;

      final RenderInfo previousRenderInfo = holder.getRenderInfo();
      mRenderInfoViewCreatorController.maybeUpdateViewCreatorMappingsOnItemUpdate(
          previousRenderInfo, renderInfo);
      holder.setRenderInfo(renderInfo);

      if (mRange == null && mIsMeasured.get() && renderInfo.rendersComponent()) {
        // Range might not have been initialized if all previous items were views and we update
        // one of them to be a component.
        initRange(
            mMeasuredSize.width,
            mMeasuredSize.height,
            position,
            getActualChildrenWidthSpec(holder),
            getActualChildrenHeightSpec(holder),
            mLayoutInfo.getScrollDirection());
      }

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

    mViewportManager.setDataChangedIsVisible(mViewportManager.isUpdateInVisibleRange(position, 1));
  }

  /**
   * Updates the range of items starting at position. The {@link RecyclerView} gets notified
   * immediately about the item being updated.
   */
  @UiThread
  public final void updateRangeAt(int position, List<RenderInfo> renderInfos) {
    ThreadUtils.assertMainThread();

    for (int i = 0, size = renderInfos.size(); i < size; i++) {

      synchronized (this) {
        final ComponentTreeHolder holder = mComponentTreeHolders.get(position + i);

        final RenderInfo previousRenderInfo = holder.getRenderInfo();
        final RenderInfo newRenderInfo = renderInfos.get(i);

        mRenderInfoViewCreatorController.maybeUpdateViewCreatorMappingsOnItemUpdate(
            previousRenderInfo, newRenderInfo);

        holder.setRenderInfo(newRenderInfo);

        if (mRange == null && mIsMeasured.get() && newRenderInfo.rendersComponent()) {
          // Range might not have been initialized if all previous items were views and we update
          // one of them to be a component.
          initRange(
              mMeasuredSize.width,
              mMeasuredSize.height,
              position + i,
              getActualChildrenWidthSpec(holder),
              getActualChildrenHeightSpec(holder),
              mLayoutInfo.getScrollDirection());
        }
      }
    }
    mInternalAdapter.notifyItemRangeChanged(position, renderInfos.size());

    computeRange(mCurrentFirstVisiblePosition, mCurrentLastVisiblePosition);

    mViewportManager.setDataChangedIsVisible(
        mViewportManager.isUpdateInVisibleRange(position, renderInfos.size()));
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
    final int mRangeSize = mRange != null ? mRange.estimatedViewportCount : -1;
    synchronized (this) {
      holder = mComponentTreeHolders.remove(fromPosition);
      mComponentTreeHolders.add(toPosition, holder);

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

    mViewportManager.setDataChangedIsVisible(
        mViewportManager.isMoveInVisibleRange(fromPosition, toPosition, mRangeSize));
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
      mRenderInfoViewCreatorController.maybeUpdateViewCreatorMappingsOnItemRemove(
          holder.getRenderInfo());
    }
    mInternalAdapter.notifyItemRemoved(position);

    holder.release();
    computeRange(mCurrentFirstVisiblePosition, mCurrentLastVisiblePosition);

    mViewportManager.setDataChangedIsVisible(mViewportManager.isRemoveInVisibleRange(position, 1));
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
        mRenderInfoViewCreatorController.maybeUpdateViewCreatorMappingsOnItemRemove(
            holder.getRenderInfo());
        holder.release();
      }
    }
    mInternalAdapter.notifyItemRangeRemoved(position, count);

    computeRange(mCurrentFirstVisiblePosition, mCurrentLastVisiblePosition);

    mViewportManager.setDataChangedIsVisible(
        mViewportManager.isRemoveInVisibleRange(position, count));
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
  public final synchronized RenderInfo getRenderInfoAt(int position) {
    return mComponentTreeHolders.get(position).getRenderInfo();
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
    final boolean shouldInitRange =
        mRange == null
            && !mComponentTreeHolders.isEmpty()
            && mCurrentFirstVisiblePosition < mComponentTreeHolders.size();

    final int positionToComputeLayout = findFirstComponentPosition();
    if (shouldInitRange && positionToComputeLayout >= 0) {
      initRange(
          SizeSpec.getSize(widthSpec),
          SizeSpec.getSize(heightSpec),
          positionToComputeLayout,
          getActualChildrenWidthSpec(mComponentTreeHolders.get(positionToComputeLayout)),
          getActualChildrenHeightSpec(mComponentTreeHolders.get(positionToComputeLayout)),
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

  private int findFirstComponentPosition() {
    for (int i = 0, size = mComponentTreeHolders.size(); i < size; i++) {
      if (mComponentTreeHolders.get(i).getRenderInfo().rendersComponent()) {
        return i;
      }
    }

    return -1;
  }

  /**
   * Gets the number of items in this binder.
   */
  @Override
  public int getItemCount() {
    return mInternalAdapter.getItemCount();
  }

  @GuardedBy("this")
  private void invalidateLayoutData() {
    mRange = null;
    for (int i = 0, size = mComponentTreeHolders.size(); i < size; i++) {
      mComponentTreeHolders.get(i).invalidateTree();
    }

    // We need to call this as we want to make sure everything is re-bound since we need new sizes
    // on all rows.
    mMainThreadHandler.removeCallbacks(mNotifyDatasetChangedRunnable);
    mMainThreadHandler.post(mNotifyDatasetChangedRunnable);
  }

  @GuardedBy("this")
  private void initRange(
      int width,
      int height,
      int positionToComputeLayout,
      int childrenWidthSpec,
      int childrenHeightSpec,
      int scrollDirection) {
    if (positionToComputeLayout >= mComponentTreeHolders.size()) {
      return;
    }

    final Size size = new Size();
    final ComponentTreeHolder holder = mComponentTreeHolders.get(positionToComputeLayout);
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

    final LayoutManager layoutManager = mLayoutInfo.getLayoutManager();

    view.setLayoutManager(layoutManager);
    view.setAdapter(mInternalAdapter);
    view.addOnScrollListener(mRangeScrollListener);
    view.addOnScrollListener(mViewportManager.getScrollListener());

    mLayoutInfo.setRenderInfoCollection(this);

    mViewportManager.addViewportChangedListener(mViewportChangedListener);

    if (mCurrentFirstVisiblePosition != RecyclerView.NO_POSITION &&
        mCurrentFirstVisiblePosition >= 0) {
      if (layoutManager instanceof LinearLayoutManager) {
        ((LinearLayoutManager) layoutManager)
            .scrollToPositionWithOffset(mCurrentFirstVisiblePosition, mCurrentOffset);
      } else {
        view.scrollToPosition(mCurrentFirstVisiblePosition);
      }
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

    final View firstView = view.getChildAt(0);

    if (firstView != null) {
      mCurrentOffset = mLayoutInfo.getScrollDirection() == HORIZONTAL
          ? mLayoutInfo.getLayoutManager().getDecoratedLeft(firstView)
          : mLayoutInfo.getLayoutManager().getDecoratedTop(firstView);
    } else {
      mCurrentOffset = 0;
    }

    view.removeOnScrollListener(mRangeScrollListener);
    view.removeOnScrollListener(mViewportManager.getScrollListener());
    view.setAdapter(null);
    view.setLayoutManager(null);

    mViewportManager.removeViewportChangedListener(mViewportChangedListener);

    // We might have already unmounted this view when calling mount with a different view. In this
    // case we can just return here.
    if (mMountedView != view) {
      return;
    }

    mMountedView = null;
    if (mStickyHeaderController != null) {
      mStickyHeaderController.reset();
    }

    mLayoutInfo.setRenderInfoCollection(null);
  }

  @UiThread
  public void scrollToPosition(int position) {
    if (mMountedView == null) {
      mCurrentFirstVisiblePosition = position;
      return;
    }

    mMountedView.scrollToPosition(position);
  }

  @UiThread
  public void scrollToPositionWithOffset(int position, int offset) {
    if (mMountedView == null || !(mMountedView.getLayoutManager() instanceof LinearLayoutManager)) {
      mCurrentFirstVisiblePosition = position;
      mCurrentOffset = offset;
      return;
    }

    ((LinearLayoutManager) mMountedView.getLayoutManager()).scrollToPositionWithOffset(
        position,
        offset);
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
    return mLayoutInfo.findFirstVisibleItemPosition();
  }

  @Override
  public int findFirstFullyVisibleItemPosition() {
    return mLayoutInfo.findFirstFullyVisibleItemPosition();
  }

  @Override
  public int findLastVisibleItemPosition() {
    return mLayoutInfo.findLastVisibleItemPosition();
  }

  @Override
  public int findLastFullyVisibleItemPosition() {
    return mLayoutInfo.findLastFullyVisibleItemPosition();
  }

  @Override
  @UiThread
  @GuardedBy("this")
  public boolean isSticky(int position) {
    return mComponentTreeHolders.get(position).getRenderInfo().isSticky();
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

  @Override
  @UiThread
  public void setViewportChangedListener(@Nullable ViewportChanged viewportChangedListener) {
    mViewportManager.addViewportChangedListener(viewportChangedListener);
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

      if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) {
        firstVisible = lastVisible = 0;
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

        if (holder.getRenderInfo().rendersView()) {
          continue;
        }

        childrenWidthSpec = getActualChildrenWidthSpec(holder);
        childrenHeightSpec = getActualChildrenHeightSpec(holder);
      }

      if (i >= rangeStart && i <= rangeEnd) {
        if (!holder.isTreeValid()) {
          holder.computeLayoutAsync(mComponentContext, childrenWidthSpec, childrenHeightSpec);
        }
      } else if (holder.isTreeValid() && !holder.getRenderInfo().isSticky()) {
        holder.acquireStateHandlerAndReleaseTree();
      }
    }
  }

  @VisibleForTesting
  @Nullable
  RangeCalculationResult getRangeCalculationResult() {
    return mRange;
  }

  @GuardedBy("this")
  private int getActualChildrenWidthSpec(final ComponentTreeHolder treeHolder) {
    if (mIsMeasured.get() && !mRequiresRemeasure.get()) {
      return mLayoutInfo.getChildWidthSpec(
          SizeSpec.makeSizeSpec(mMeasuredSize.width, SizeSpec.EXACTLY),
          treeHolder.getRenderInfo());
    }

    return mLayoutInfo.getChildWidthSpec(mLastWidthSpec, treeHolder.getRenderInfo());
  }

  @GuardedBy("this")
  private int getActualChildrenHeightSpec(final ComponentTreeHolder treeHolder) {
    if (mIsMeasured.get() && !mRequiresRemeasure.get()) {
      return mLayoutInfo.getChildHeightSpec(
          SizeSpec.makeSizeSpec(mMeasuredSize.height, SizeSpec.EXACTLY),
          treeHolder.getRenderInfo());
    }

    return mLayoutInfo.getChildHeightSpec(mLastHeightSpec, treeHolder.getRenderInfo());
  }

  private class RangeScrollListener extends RecyclerView.OnScrollListener {

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
      if (mCanPrefetchDisplayLists) {
        DisplayListUtils.prefetchDisplayLists(recyclerView);
      }
    }
  }

  private class BaseViewHolder extends RecyclerView.ViewHolder {

    private final boolean isLithoViewType;

    public BaseViewHolder(View view, boolean isLithoViewType) {
      super(view);
      this.isLithoViewType = isLithoViewType;
    }
  }

  private class InternalAdapter extends RecyclerView.Adapter<BaseViewHolder> {

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      final ViewCreator viewCreator = mRenderInfoViewCreatorController.getViewCreator(viewType);

      if (viewCreator != null) {
        return new BaseViewHolder(viewCreator.createView(mComponentContext), false);
      } else {
        final LithoView lithoView =
            mLithoViewFactory == null
                ? new LithoView(mComponentContext, null)
                : mLithoViewFactory.createLithoView(mComponentContext);

        return new BaseViewHolder(lithoView, true);
      }
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
      // We can ignore the synchronization here. We'll only add to this from the UiThread.
      // This read only happens on the UiThread as well and we are never writing this here.
      final ComponentTreeHolder componentTreeHolder = mComponentTreeHolders.get(position);

      final RenderInfo renderInfo = componentTreeHolder.getRenderInfo();
      if (renderInfo.rendersComponent()) {
        final LithoView lithoView = (LithoView) holder.itemView;
        final int childrenWidthSpec = getActualChildrenWidthSpec(componentTreeHolder);
        final int childrenHeightSpec = getActualChildrenHeightSpec(componentTreeHolder);
        if (!componentTreeHolder.isTreeValid()) {
          componentTreeHolder.computeLayoutSync(
              mComponentContext, childrenWidthSpec, childrenHeightSpec, null);
        }

        int width, height;
        switch (mLayoutInfo.getScrollDirection()) {
          case OrientationHelper.VERTICAL:
            if (SizeSpec.getMode(childrenWidthSpec) == SizeSpec.EXACTLY) {
              width = SizeSpec.getSize(childrenWidthSpec);
            } else {
              width = ViewGroup.LayoutParams.MATCH_PARENT;
            }
            if (SizeSpec.getMode(childrenHeightSpec) == SizeSpec.EXACTLY) {
              height = SizeSpec.getSize(childrenHeightSpec);
            } else {
              height = WRAP_CONTENT;
            }

            lithoView.setLayoutParams(new RecyclerView.LayoutParams(width, height));
            break;
          default:
            if (SizeSpec.getMode(childrenWidthSpec) == SizeSpec.EXACTLY) {
              width = SizeSpec.getSize(childrenWidthSpec);
            } else {
              width = ViewGroup.LayoutParams.WRAP_CONTENT;
            }
            if (SizeSpec.getMode(childrenHeightSpec) == SizeSpec.EXACTLY) {
              height = SizeSpec.getSize(childrenHeightSpec);
            } else {
              height = MATCH_PARENT;
            }

            lithoView.setLayoutParams(new RecyclerView.LayoutParams(width, height));
        }

        lithoView.setComponentTree(componentTreeHolder.getComponentTree());
      } else {
        renderInfo.getViewBinder().bind(holder.itemView);
      }
    }

    @Override
    public int getItemViewType(int position) {
      final RenderInfo renderInfo = mComponentTreeHolders.get(position).getRenderInfo();
      if (renderInfo.rendersComponent()) {
        // Special value for LithoViews
        return COMPONENT_VIEW_TYPE;
      } else {
        return renderInfo.getViewType();
      }
    }

    @Override
    public void onViewAttachedToWindow(BaseViewHolder holder) {
      final int position = holder.getLayoutPosition();
      mViewportManager.onViewportchangedAfterViewAdded(position);
    }

    @Override
    public void onViewDetachedFromWindow(BaseViewHolder holder) {
      // LayoutPosition of the detached ViewHolder is always 1 position less than
      // the actual on screen.
      mViewportManager.onViewportChangedAfterViewRemoval(holder.getLayoutPosition() + 1);
    }

    @Override
    public int getItemCount() {
      // We can ignore the synchronization here. We'll only add to this from the UiThread.
      // This read only happens on the UiThread as well and we are never writing this here.
      return mComponentTreeHolders.size();
    }

    @Override
    public void onViewRecycled(BaseViewHolder holder) {
      if (holder.isLithoViewType) {
        final LithoView lithoView = (LithoView) holder.itemView;
        lithoView.setComponentTree(null);
      }
    }
  }
}
