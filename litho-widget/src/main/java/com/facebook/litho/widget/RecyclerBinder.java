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

import static android.support.v7.widget.OrientationHelper.HORIZONTAL;
import static android.support.v7.widget.OrientationHelper.VERTICAL;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.facebook.litho.FrameworkLogEvents.EVENT_ERROR;
import static com.facebook.litho.FrameworkLogEvents.PARAM_MESSAGE;
import static com.facebook.litho.MeasureComparisonUtils.isMeasureSpecCompatible;
import static com.facebook.litho.widget.RenderInfoViewCreatorController.DEFAULT_COMPONENT_VIEW_TYPE;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IntDef;
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
import com.facebook.litho.ComponentTree.MeasureListener;
import com.facebook.litho.ComponentsLogger;
import com.facebook.litho.ComponentsSystrace;
import com.facebook.litho.EventHandler;
import com.facebook.litho.LayoutHandler;
import com.facebook.litho.LithoView;
import com.facebook.litho.LogEvent;
import com.facebook.litho.MeasureComparisonUtils;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.ThreadUtils;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.utils.DisplayListUtils;
import com.facebook.litho.viewcompat.ViewBinder;
import com.facebook.litho.viewcompat.ViewCreator;
import com.facebook.litho.widget.ComponentTreeHolder.ComponentTreeMeasureListenerFactory;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
  private final List<ComponentTreeHolder> mComponentTreeHolders = new ArrayList<>();

  @GuardedBy("this")
  private final List<ComponentTreeHolder> mAsyncComponentTreeHolders = new ArrayList<>();

  private final LayoutInfo mLayoutInfo;
  private final RecyclerView.Adapter mInternalAdapter;
  private final ComponentContext mComponentContext;
  private final RangeScrollListener mRangeScrollListener = new RangeScrollListener();
  private final LayoutHandlerFactory mLayoutHandlerFactory;
  private final @Nullable LithoViewFactory mLithoViewFactory;
  private final ComponentTreeHolderFactory mComponentTreeHolderFactory;
  private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
  private final boolean mAllowFillViewportOnMainForTest;
  private final float mRangeRatio;
  private final AtomicBoolean mIsMeasured = new AtomicBoolean(false);
  private final AtomicBoolean mRequiresRemeasure = new AtomicBoolean(false);

  @GuardedBy("this")
  private final ArrayList<AsyncInsertOperation> mInsertsWaitingForInitialMeasure =
      new ArrayList<>();

  @GuardedBy("this")
  private final Deque<AsyncBatch> mAsyncBatches = new ArrayDeque<>();

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

  private final ComponentTreeMeasureListenerFactory mComponentTreeMeasureListenerFactory =
      new ComponentTreeMeasureListenerFactory() {
        @Override
        public MeasureListener create(final ComponentTreeHolder holder) {
          return getMeasureListener(holder);
        }
      };
  private String mSplitLayoutTag;

  private MeasureListener getMeasureListener(final ComponentTreeHolder holder) {
    return new MeasureListener() {
      @Override
      public void onSetRootAndSizeSpec(int width, int height) {
        if (holder.getMeasuredHeight() == height) {
          return;
        }

        holder.setMeasuredHeight(height);

        final RangeCalculationResult range = RecyclerBinder.this.mRange;

        if (range != null
            && holder.getMeasuredHeight() <= RecyclerBinder.this.mRange.measuredSize) {
          return;
        }

        synchronized (RecyclerBinder.this) {
          resetMeasuredSize(width);
        }

        requestRemeasure();
      }
    };
  }

  private final ComponentTree.NewLayoutStateReadyListener mAsyncLayoutReadyListener =
      new ComponentTree.NewLayoutStateReadyListener() {
        @Override
        public void onNewLayoutStateReady(ComponentTree componentTree) {
          applyReadyBatches();
        }
      };

  private final boolean mIsCircular;
  private final boolean mHasDynamicItemHeight;
  private final boolean mWrapContent;
  private final boolean mInsertPostAsyncLayoutEnabled;
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
  private volatile boolean mHasAsyncOperations = false;
  private volatile boolean mAsyncInsertsShouldWaitForMeasure = true;

  @GuardedBy("this")
  private @Nullable AsyncBatch mCurrentBatch = null;

  private final ViewportManager mViewportManager;
  private final ViewportChanged mViewportChangedListener =
      new ViewportChanged() {
        @Override
        public void viewportChanged(
            int firstVisibleIndex,
            int lastVisibleIndex,
            int firstFullyVisibleIndex,
            int lastFullyVisibleIndex,
            int state) {
          onNewVisibleRange(firstVisibleIndex, lastVisibleIndex);
          onNewWorkingRange(
              firstVisibleIndex, lastVisibleIndex, firstFullyVisibleIndex, lastFullyVisibleIndex);
        }
      };
  private int mCountPostComputeRangeRunnableOnAnimation;

  @VisibleForTesting final RenderInfoViewCreatorController mRenderInfoViewCreatorController;

  private Runnable mComputeRangeRunnable =
      new Runnable() {
        @Override
        public void run() {
          // If mount hasn't happened or we don't have any pending updates, we're ready to compute
          // range.
          if (mMountedView == null || !mMountedView.hasPendingAdapterUpdates()) {
            mCountPostComputeRangeRunnableOnAnimation = 0;
            computeRange(mCurrentFirstVisiblePosition, mCurrentLastVisiblePosition);
            return;
          }

          // If the view gets detached, we can still have pending updates.
          // If the view's visibility is GONE, layout won't happen until it becomes visible. We have
          // to exit here, otherwise we keep posting this runnable to the next frame until it
          // becomes visible.
          if (!mMountedView.isAttachedToWindow() || mMountedView.getVisibility() == View.GONE) {
            return;
          }

          if (mCountPostComputeRangeRunnableOnAnimation >= 3) {
            mCountPostComputeRangeRunnableOnAnimation = 0;
            logTooManyPostingAttempts();
            computeRange(mCurrentFirstVisiblePosition, mCurrentLastVisiblePosition);

            return;
          }

          // If we have pending updates, wait until the sync operations are finished and try again
          // in the next frame.
          mCountPostComputeRangeRunnableOnAnimation++;
          ViewCompat.postOnAnimation(mMountedView, mComputeRangeRunnable);
        }
      };

  interface ComponentTreeHolderFactory {
    ComponentTreeHolder create(
        RenderInfo renderInfo,
        LayoutHandler layoutHandler,
        boolean canPrefetchDisplayLists,
        boolean canCacheDrawingDisplayLists,
        ComponentTreeMeasureListenerFactory measureListenerFactory,
        String splitLayoutTag);
  }

  static final ComponentTreeHolderFactory DEFAULT_COMPONENT_TREE_HOLDER_FACTORY =
      new ComponentTreeHolderFactory() {
        @Override
        public ComponentTreeHolder create(
            RenderInfo renderInfo,
            LayoutHandler layoutHandler,
            boolean canPrefetchDisplayLists,
            boolean canCacheDrawingDisplayLists,
            ComponentTreeMeasureListenerFactory measureListenerFactory,
            String splitLayoutTag) {
          return ComponentTreeHolder.acquire(
              renderInfo,
              layoutHandler,
              canPrefetchDisplayLists,
              canCacheDrawingDisplayLists,
              measureListenerFactory,
              splitLayoutTag);
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
    private boolean isCircular;
    private boolean hasDynamicItemHeight;
    private boolean wrapContent;
    private boolean insertPostAsyncLayoutEnabled;
    private boolean customViewTypeEnabled;
    private int componentViewType;
    private @Nullable RecyclerView.Adapter overrideInternalAdapter;
    private boolean allowFillViewportOnMainForTest;
    private String splitLayoutTag;

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
     * Whether the underlying RecyclerBinder will have a circular behaviour. Defaults to false.
     * Note: circular lists DO NOT support any operation that changes the size of items like insert,
     * remove, insert range, remove range
     */
    public Builder isCircular(boolean isCircular) {
      this.isCircular = isCircular;
      return this;
    }

    /**
     * If true, the underlying RecyclerBinder will measure the parent height by the height of
     * children if the orientation is vertical, or measure the parent width by the width of children
     * if the orientation is horizontal.
     */
    public Builder wrapContent(boolean wrapContent) {
      this.wrapContent = wrapContent;
      return this;
    }

    /**
     * @param componentTreeHolderFactory Factory to acquire a new ComponentTreeHolder. Defaults to
     *     {@link #DEFAULT_COMPONENT_TREE_HOLDER_FACTORY}.
     */
    public Builder componentTreeHolderFactory(
        ComponentTreeHolderFactory componentTreeHolderFactory) {
      this.componentTreeHolderFactory = componentTreeHolderFactory;
      return this;
    }

    /**
     * Do not enable this. This is an experimental feature and your Section surface will take a perf
     * hit if you use it.
     *
     * <p>Whether the items of this RecyclerBinder can change height after the initial measure. Only
     * applicable to horizontally scrolling RecyclerBinders. If true, the children of this h-scroll
     * are all measured with unspecified height. When the ComponentTree of a child is remeasured,
     * this will cause the RecyclerBinder to remeasure in case the height of the child changed and
     * the RecyclerView needs to have a different height to account for it. This only supports
     * changing the height of the item that triggered the remeasuring, not the height of all items
     * in the h-scroll.
     */
    public Builder hasDynamicItemHeight(boolean hasDynamicItemHeight) {
      this.hasDynamicItemHeight = hasDynamicItemHeight;
      return this;
    }

    /**
     * TODO (T26795745): remove this once the experiment is finished.
     *
     * <p>Do not enable this. This is an experimental feature and your Section surface will take a
     * perf hit if you use it.
     *
     * <p>If true, insert operations won't start async layout calculations for the items in range,
     * instead these layout calculations will be posted to the next frame.
     */
    public Builder insertPostAsyncLayoutEnabled(boolean insertPostAsyncLayoutEnabled) {
      this.insertPostAsyncLayoutEnabled = insertPostAsyncLayoutEnabled;
      return this;
    }

    /**
     * Enable setting custom viewTypes on {@link ViewRenderInfo}s.
     *
     * <p>After this is set, all {@link ViewRenderInfo}s must be built with a custom viewType
     * through {@link ViewRenderInfo.Builder#customViewType(int)}, otherwise exception will be
     * thrown.
     *
     * @param componentViewType the viewType to be used for Component types, provided through {@link
     *     ComponentRenderInfo}. Set this to a value that won't conflict with your custom viewTypes.
     */
    public Builder enableCustomViewType(int componentViewType) {
      this.customViewTypeEnabled = true;
      this.componentViewType = componentViewType;
      return this;
    }

    /**
     * Method for tests to allow mocking of the InternalAdapter to verify interaction with the
     * RecyclerView.
     */
    @VisibleForTesting
    Builder overrideInternalAdapter(RecyclerView.Adapter overrideInternalAdapter) {
      this.overrideInternalAdapter = overrideInternalAdapter;
      return this;
    }

    /** Method for tests to allow fillListViewport to be called on the main thread. */
    @VisibleForTesting
    Builder allowFillViewportOnMainForTest(boolean allowFillViewportOnMainForTest) {
      this.allowFillViewportOnMainForTest = allowFillViewportOnMainForTest;
      return this;
    }

    public Builder splitLayoutTag(String splitLayoutTag) {
      this.splitLayoutTag = splitLayoutTag;
      return this;
    }

    /** @param c The {@link ComponentContext} the RecyclerBinder will use. */
    public RecyclerBinder build(ComponentContext c) {
      componentContext = c;

      if (layoutInfo == null) {
        layoutInfo = new LinearLayoutInfo(c, VERTICAL, false);
      }

      return new RecyclerBinder(this);
    }
  }

  @Override
  public boolean isWrapContent() {
    return mWrapContent;
  }

  private RecyclerBinder(Builder builder) {
    mComponentContext = builder.componentContext;
    mComponentTreeHolderFactory = builder.componentTreeHolderFactory;
    mInternalAdapter =
        builder.overrideInternalAdapter != null
            ? builder.overrideInternalAdapter
            : new InternalAdapter();

    mRangeRatio = builder.rangeRatio;
    mLayoutInfo = builder.layoutInfo;
    mLayoutHandlerFactory = builder.layoutHandlerFactory;
    mLithoViewFactory = builder.lithoViewFactory;
    mCanPrefetchDisplayLists = builder.canPrefetchDisplayLists;
    mCanCacheDrawingDisplayLists = builder.canCacheDrawingDisplayLists;
    mRenderInfoViewCreatorController =
        new RenderInfoViewCreatorController(
            builder.customViewTypeEnabled,
            builder.customViewTypeEnabled
                ? builder.componentViewType
                : DEFAULT_COMPONENT_VIEW_TYPE);

    mIsCircular = builder.isCircular;
    mHasDynamicItemHeight =
        mLayoutInfo.getScrollDirection() == HORIZONTAL ? builder.hasDynamicItemHeight : false;
    mInsertPostAsyncLayoutEnabled = builder.insertPostAsyncLayoutEnabled;
    mAllowFillViewportOnMainForTest = builder.allowFillViewportOnMainForTest;
    mWrapContent = builder.wrapContent;

    mViewportManager =
        new ViewportManager(
            mCurrentFirstVisiblePosition,
            mCurrentLastVisiblePosition,
            builder.layoutInfo,
            mMainThreadHandler);

    mSplitLayoutTag = builder.splitLayoutTag;
  }

  /**
   * Update the item at index position. The {@link RecyclerView} will only be notified of the item
   * being updated after a layout calculation has been completed for the new {@link Component}.
   */
  @UiThread
  public final void updateItemAtAsync(int position, RenderInfo renderInfo) {
    ThreadUtils.assertMainThread();

    if (SectionsDebug.ENABLED) {
      Log.d(SectionsDebug.TAG, "(" + hashCode() + ") updateItemAtAsync " + position);
    }

    updateItemAtAsyncInner(position, renderInfo);
  }

  /**
   * Update the items starting from the given index position. The {@link RecyclerView} will only be
   * notified of the item being updated after a layout calculation has been completed for the new
   * {@link Component}.
   */
  @UiThread
  public final void updateRangeAtAsync(int position, List<RenderInfo> renderInfos) {
    ThreadUtils.assertMainThread();

    if (SectionsDebug.ENABLED) {
      Log.d(
          SectionsDebug.TAG,
          "(" + hashCode() + ") updateRangeAtAsync " + position + ", count: " + renderInfos.size());
    }

    for (int i = 0, size = renderInfos.size(); i < size; i++) {
      updateItemAtAsyncInner(position + i, renderInfos.get(i));
    }
  }

  @UiThread
  private void updateItemAtAsyncInner(int position, RenderInfo renderInfo) {
    final ComponentTreeHolder holder;
    final boolean renderInfoWasView;
    final int indexInComponentTreeHolders;
    synchronized (this) {
      holder = mAsyncComponentTreeHolders.get(position);
      renderInfoWasView = holder.getRenderInfo().rendersView();

      mRenderInfoViewCreatorController.maybeTrackViewCreator(renderInfo);
      holder.setRenderInfo(renderInfo);

      if (holder.isInserted()) {
        // If it's inserted, we can just count on the normal range computation re-computing this
        indexInComponentTreeHolders = mComponentTreeHolders.indexOf(holder);
        mViewportManager.setDataChangedIsVisible(
            mViewportManager.isUpdateInVisibleRange(indexInComponentTreeHolders, 1));
      } else {
        indexInComponentTreeHolders = -1;

        // TODO(T28668712): Handle updates outside of range
        computeLayoutAsync(holder);
      }
    }

    if (indexInComponentTreeHolders >= 0
        && (renderInfoWasView || holder.getRenderInfo().rendersView())) {
      mInternalAdapter.notifyItemChanged(indexInComponentTreeHolders);
    }
  }

  /**
   * Inserts an item at position. The {@link RecyclerView} will only be notified of the item being
   * inserted after a layout calculation has been completed for the new {@link Component}.
   */
  @UiThread
  public final void insertItemAtAsync(int position, RenderInfo renderInfo) {
    ThreadUtils.assertMainThread();

    assertNoInsertOperationIfCircular();

    final AsyncInsertOperation operation = createAsyncInsertOperation(position, renderInfo);

    synchronized (this) {
      mHasAsyncOperations = true;

      mAsyncComponentTreeHolders.add(position, operation.mHolder);

      // If the binder has not been measured yet, we will compute an initial page of content based
      // on the measured size of this RecyclerBinder synchronously during measure, and the rest will
      // be kicked off as async inserts.
      if (mAsyncInsertsShouldWaitForMeasure) {
        mInsertsWaitingForInitialMeasure.add(operation);
        return;
      }

      registerAsyncInsert(operation);
    }
  }

  /**
   * Inserts the new items starting from position. The {@link RecyclerView} will only be notified of
   * the items being inserted after a layout calculation has been completed for the new {@link
   * Component}s. There is not a guarantee that the {@link RecyclerView} will be notified about all
   * the items in the range at the same time.
   */
  @UiThread
  public final void insertRangeAtAsync(int position, List<RenderInfo> renderInfos) {
    ThreadUtils.assertMainThread();

    assertNoInsertOperationIfCircular();

    synchronized (this) {
      mHasAsyncOperations = true;

      for (int i = 0, size = renderInfos.size(); i < size; i++) {
        final AsyncInsertOperation operation =
            createAsyncInsertOperation(position + i, renderInfos.get(i));

        mAsyncComponentTreeHolders.add(position + i, operation.mHolder);

        // If the binder has not been measured yet, we will compute an initial page of content based
        // on the measured size of this RecyclerBinder synchronously during measure, and the rest
        // will be kicked off as async inserts.
        if (mAsyncInsertsShouldWaitForMeasure) {
          mInsertsWaitingForInitialMeasure.add(operation);
        } else {
          registerAsyncInsert(operation);
        }
      }
    }
  }

  @UiThread
  private void applyReadyBatches() {
    ThreadUtils.assertMainThread();

    synchronized (this) {
      boolean appliedBatch = false;
      while (!mAsyncBatches.isEmpty()) {
        final AsyncBatch batch = mAsyncBatches.peekFirst();
        if (!isBatchReady(batch)) {
          break;
        }

        mAsyncBatches.pollFirst();
        applyBatch(batch);

        appliedBatch = true;
      }

      if (appliedBatch) {
        maybeUpdateRangeOrRemeasureForMutation();
      }
    }
  }

  private static boolean isBatchReady(AsyncBatch batch) {
    for (int i = 0, size = batch.mOperations.size(); i < size; i++) {
      final AsyncOperation operation = batch.mOperations.get(i);
      if (operation instanceof AsyncInsertOperation
          && !((AsyncInsertOperation) operation).mHolder.hasCompletedLatestLayout()) {
        return false;
      }
    }
    return true;
  }

  @GuardedBy("this")
  @UiThread
  private void applyBatch(AsyncBatch batch) {
    for (int i = 0, size = batch.mOperations.size(); i < size; i++) {
      final AsyncOperation operation = batch.mOperations.get(i);

      switch (operation.mOperation) {
        case Operation.INSERT:
          applyAsyncInsert((AsyncInsertOperation) operation);
          break;
        case Operation.REMOVE:
          removeItemAt(((AsyncRemoveOperation) operation).mPosition);
          break;
        case Operation.REMOVE_RANGE:
          final AsyncRemoveRangeOperation removeRangeOperation =
              (AsyncRemoveRangeOperation) operation;
          removeRangeAt(removeRangeOperation.mPosition, removeRangeOperation.mCount);
          break;
        case Operation.MOVE:
          final AsyncMoveOperation moveOperation = (AsyncMoveOperation) operation;
          moveItem(moveOperation.mFromPosition, moveOperation.mToPosition);
          break;
        default:
          throw new RuntimeException("Unhandled operation type: " + operation.mOperation);
      }
    }
  }

  @GuardedBy("this")
  @UiThread
  private void applyAsyncInsert(AsyncInsertOperation operation) {
    mComponentTreeHolders.add(operation.mPosition, operation.mHolder);
    operation.mHolder.setInserted(true);
    mInternalAdapter.notifyItemInserted(operation.mPosition);
    mViewportManager.isInsertInVisibleRange(
        operation.mPosition, 1, mRange != null ? mRange.estimatedViewportCount : -1);
  }

  @GuardedBy("this")
  private void registerAsyncInsert(AsyncInsertOperation operation) {
    addToCurrentBatch(operation);

    final ComponentTreeHolder holder = operation.mHolder;
    holder.setNewLayoutReadyListener(mAsyncLayoutReadyListener);
    holder.computeLayoutAsync(
        mComponentContext, getActualChildrenWidthSpec(holder), getActualChildrenHeightSpec(holder));
  }

  /**
   * Moves an item from fromPosition to toPostion. If there are other pending operations on this
   * binder this will only be executed when all the operations have been completed (to ensure index
   * consistency).
   */
  @UiThread
  public final void moveItemAsync(int fromPosition, int toPosition) {
    ThreadUtils.assertMainThread();

    if (SectionsDebug.ENABLED) {
      Log.d(
          SectionsDebug.TAG,
          "(" + hashCode() + ") moveItemAsync " + fromPosition + " to " + toPosition);
    }

    final AsyncMoveOperation operation = new AsyncMoveOperation(fromPosition, toPosition);
    synchronized (this) {
      mAsyncComponentTreeHolders.add(toPosition, mAsyncComponentTreeHolders.remove(fromPosition));

      // TODO(t28619782): When moving a CT into range, do an async prepare
      addToCurrentBatch(operation);
    }
  }

  /**
   * Removes an item from position. If there are other pending operations on this binder this will
   * only be executed when all the operations have been completed (to ensure index consistency).
   */
  @UiThread
  public final void removeItemAtAsync(int position) {
    ThreadUtils.assertMainThread();

    if (SectionsDebug.ENABLED) {
      Log.d(SectionsDebug.TAG, "(" + hashCode() + ") removeItemAtAsync " + position);
    }

    final AsyncRemoveOperation asyncRemoveOperation = new AsyncRemoveOperation(position);
    synchronized (this) {
      mAsyncComponentTreeHolders.remove(position);
      addToCurrentBatch(asyncRemoveOperation);
    }
  }

  /**
   * Removes count items starting from position. If there are other pending operations on this
   * binder this will only be executed when all the operations have been completed (to ensure index
   * consistency).
   */
  @UiThread
  public final void removeRangeAtAsync(int position, int count) {
    ThreadUtils.assertMainThread();

    assertNoRemoveOperationIfCircular(count);

    if (SectionsDebug.ENABLED) {
      Log.d(
          SectionsDebug.TAG,
          "(" + hashCode() + ") removeRangeAtAsync " + position + ", size: " + count);
    }

    final AsyncRemoveRangeOperation operation = new AsyncRemoveRangeOperation(position, count);
    synchronized (this) {
      for (int i = 0; i < count; i++) {
        // TODO(t28712163): Cancel pending layouts for async inserts
        mAsyncComponentTreeHolders.remove(position);
      }
      addToCurrentBatch(operation);
    }
  }

  @GuardedBy("this")
  private void addToCurrentBatch(AsyncOperation operation) {
    if (mCurrentBatch == null) {
      mCurrentBatch = new AsyncBatch();
    }
    mCurrentBatch.mOperations.add(operation);
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
   * layout is immediately computed on the] UiThread. The RenderInfo contains the component that
   * will be inserted in the Binder and extra info like isSticky or spanCount.
   */
  @UiThread
  public final void insertItemAt(int position, RenderInfo renderInfo) {
    ThreadUtils.assertMainThread();

    assertNoInsertOperationIfCircular();

    if (SectionsDebug.ENABLED) {
      Log.d(SectionsDebug.TAG, "(" + hashCode() + ") insertItemAt " + position);
    }

    final ComponentTreeHolder holder = createComponentTreeHolder(renderInfo);
    synchronized (this) {
      if (mHasAsyncOperations) {
        throw new RuntimeException("Trying to do a sync insert when using asynchronous mutations!");
      }
      mComponentTreeHolders.add(position, holder);
      mRenderInfoViewCreatorController.maybeTrackViewCreator(renderInfo);
    }

    mInternalAdapter.notifyItemInserted(position);

    mViewportManager.setDataChangedIsVisible(
        mViewportManager.isInsertInVisibleRange(
            position, 1, mRange != null ? mRange.estimatedViewportCount : -1));
  }

  private void requestRemeasure() {
    if (SectionsDebug.ENABLED) {
      Log.d(SectionsDebug.TAG, "(" + hashCode() + ") requestRemeasure");
    }

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
   * immediately about the new item being inserted. The RenderInfo contains the component that will
   * be inserted in the Binder and extra info like isSticky or spanCount.
   */
  @UiThread
  public final void insertRangeAt(int position, List<RenderInfo> renderInfos) {
    ThreadUtils.assertMainThread();

    assertNoInsertOperationIfCircular();

    if (SectionsDebug.ENABLED) {
      Log.d(
          SectionsDebug.TAG,
          "(" + hashCode() + ") insertRangeAt " + position + ", size: " + renderInfos.size());
    }

    for (int i = 0, size = renderInfos.size(); i < size; i++) {

      synchronized (this) {
        final RenderInfo renderInfo = renderInfos.get(i);
        final ComponentTreeHolder holder = createComponentTreeHolder(renderInfo);
        if (mHasAsyncOperations) {
          throw new RuntimeException(
              "Trying to do a sync insert when using asynchronous mutations!");
        }
        mComponentTreeHolders.add(position + i, holder);
        mRenderInfoViewCreatorController.maybeTrackViewCreator(renderInfo);
      }
    }

    mInternalAdapter.notifyItemRangeInserted(position, renderInfos.size());

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

    if (SectionsDebug.ENABLED) {
      Log.d(SectionsDebug.TAG, "(" + hashCode() + ") updateItemAt " + position);
    }

    final ComponentTreeHolder holder;
    final boolean renderInfoWasView;
    synchronized (this) {
      holder = mComponentTreeHolders.get(position);
      renderInfoWasView = holder.getRenderInfo().rendersView();

      mRenderInfoViewCreatorController.maybeTrackViewCreator(renderInfo);
      holder.setRenderInfo(renderInfo);
    }

    // If this item is rendered with a view (or was rendered with a view before now) we need to
    // notify the RecyclerView's adapter that something changed.
    if (renderInfoWasView || renderInfo.rendersView()) {
      mInternalAdapter.notifyItemChanged(position);
    }

    mViewportManager.setDataChangedIsVisible(mViewportManager.isUpdateInVisibleRange(position, 1));
  }

  /**
   * Updates the range of items starting at position. The {@link RecyclerView} gets notified
   * immediately about the item being updated.
   */
  @UiThread
  public final void updateRangeAt(int position, List<RenderInfo> renderInfos) {
    ThreadUtils.assertMainThread();

    if (SectionsDebug.ENABLED) {
      Log.d(
          SectionsDebug.TAG,
          "(" + hashCode() + ") updateRangeAt " + position + ", size: " + renderInfos.size());
    }

    for (int i = 0, size = renderInfos.size(); i < size; i++) {

      synchronized (this) {
        final ComponentTreeHolder holder = mComponentTreeHolders.get(position + i);
        final RenderInfo newRenderInfo = renderInfos.get(i);

        // If this item is rendered with a view (or was rendered with a view before now) we still
        // need to notify the RecyclerView's adapter that something changed.
        if (newRenderInfo.rendersView() || holder.getRenderInfo().rendersView()) {
          mInternalAdapter.notifyItemChanged(position + i);
        }

        mRenderInfoViewCreatorController.maybeTrackViewCreator(newRenderInfo);
        holder.setRenderInfo(newRenderInfo);
      }
    }

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

    if (SectionsDebug.ENABLED) {
      Log.d(
          SectionsDebug.TAG, "(" + hashCode() + ") moveItem " + fromPosition + " to " + toPosition);
    }

    final ComponentTreeHolder holder;
    final boolean isNewPositionInRange;
    final int mRangeSize = mRange != null ? mRange.estimatedViewportCount : -1;
    synchronized (this) {
      holder = mComponentTreeHolders.remove(fromPosition);
      mComponentTreeHolders.add(toPosition, holder);

      isNewPositionInRange = mRangeSize > 0 &&
          toPosition >= mCurrentFirstVisiblePosition - (mRangeSize * mRangeRatio) &&
          toPosition <= mCurrentFirstVisiblePosition + mRangeSize + (mRangeSize * mRangeRatio);
    }
    final boolean isTreeValid = holder.isTreeValid();

    if (isTreeValid && !isNewPositionInRange) {
      holder.acquireStateHandlerAndReleaseTree();
    }
    mInternalAdapter.notifyItemMoved(fromPosition, toPosition);

    mViewportManager.setDataChangedIsVisible(
        mViewportManager.isMoveInVisibleRange(fromPosition, toPosition, mRangeSize));
  }

  /**
   * Removes an item from index position.
   */
  @UiThread
  public final void removeItemAt(int position) {
    ThreadUtils.assertMainThread();

    assertNoRemoveOperationIfCircular(1);

    if (SectionsDebug.ENABLED) {
      Log.d(SectionsDebug.TAG, "(" + hashCode() + ") removeItemAt " + position);
    }

    final ComponentTreeHolder holder;
    synchronized (this) {
      holder = mComponentTreeHolders.remove(position);
    }
    mInternalAdapter.notifyItemRemoved(position);

    holder.release();

    mViewportManager.setDataChangedIsVisible(mViewportManager.isRemoveInVisibleRange(position, 1));
  }

  /**
   * Removes count items starting from position.
   */
  @UiThread
  public final void removeRangeAt(int position, int count) {
    ThreadUtils.assertMainThread();

    assertNoRemoveOperationIfCircular(count);

    if (SectionsDebug.ENABLED) {
      Log.d(
          SectionsDebug.TAG, "(" + hashCode() + ") removeRangeAt " + position + ", size: " + count);
    }

    synchronized (this) {
      for (int i = 0; i < count; i++) {
        final ComponentTreeHolder holder = mComponentTreeHolders.remove(position);
        holder.release();
      }
    }
    mInternalAdapter.notifyItemRangeRemoved(position, count);

    mViewportManager.setDataChangedIsVisible(
        mViewportManager.isRemoveInVisibleRange(position, count));
  }

  /**
   * Called after all the change set operations (inserts, removes, etc.) in a batch have completed.
   */
  @UiThread
  public void notifyChangeSetComplete() {
    ThreadUtils.assertMainThread();

    if (SectionsDebug.ENABLED) {
      Log.d(SectionsDebug.TAG, "(" + hashCode() + ") notifyChangeSetComplete");
    }

    closeCurrentBatch();
    applyReadyBatches();
    maybeUpdateRangeOrRemeasureForMutation();
  }

  private synchronized void closeCurrentBatch() {
    if (mCurrentBatch != null) {
      mAsyncBatches.addLast(mCurrentBatch);
      mCurrentBatch = null;
    }
  }

  private synchronized void maybeUpdateRangeOrRemeasureForMutation() {
    synchronized (this) {
      if (!mIsMeasured.get()) {
        return;
      }

      if (mRequiresRemeasure.get()) {
        requestRemeasure();
        return;
      }

      if (mRange == null) {
        int firstComponent = findFirstComponentPosition(mComponentTreeHolders);
        if (firstComponent >= 0) {
          final ComponentTreeHolder holder = mComponentTreeHolders.get(firstComponent);
          initRange(
              mMeasuredSize.width,
              mMeasuredSize.height,
              holder,
              getActualChildrenWidthSpec(holder),
              getActualChildrenHeightSpec(holder),
              mLayoutInfo.getScrollDirection());

          if (SectionsDebug.ENABLED) {
            Log.d(
                SectionsDebug.TAG,
                "(" + hashCode() + ") initializing range on main thread, will not fill viewport");
          }
        }
      }
    }

    maybePostComputeRange();
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
  public final synchronized ComponentTree getComponentForStickyHeaderAt(int position) {
    final ComponentTreeHolder holder = mComponentTreeHolders.get(position);
    if (holder.isTreeValid()) {
      return holder.getComponentTree();
    }

    // This could happen when RecyclerView is populated with new data, and first position is not 0.
    // It is possible that sticky header is above the first visible position and also it is outside
    // calculated range and its layout has not been calculated yet.
    final int childrenWidthSpec = getActualChildrenWidthSpec(holder);
    final int childrenHeightSpec = getActualChildrenHeightSpec(holder);
    holder.computeLayoutSync(mComponentContext, childrenWidthSpec, childrenHeightSpec, null);

    return holder.getComponentTree();
  }

  @Override
  public final synchronized RenderInfo getRenderInfoAt(int position) {
    return mComponentTreeHolders.get(position).getRenderInfo();
  }

  @VisibleForTesting
  final synchronized ComponentTreeHolder getComponentTreeHolderAt(int position) {
    return mComponentTreeHolders.get(position);
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
      @Nullable EventHandler<ReMeasureEvent> reMeasureEventHandler) {
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

    if (!mInsertsWaitingForInitialMeasure.isEmpty() && !mComponentTreeHolders.isEmpty()) {
      throw new IllegalStateException(
          "One of InsertsWaitingForInitialMeasure or ComponentTreeHolders should be empty!");
    }

    // We now need to compute the size of the non scrolling side. We try to do this by using the
    // calculated range (if we have one) or computing one.
    boolean doFillViewportAfterFinishingMeasure = false;
    if (mRange == null) {
      ComponentTreeHolder holderForRange = null;
      if (!mComponentTreeHolders.isEmpty()) {
        final int positionToComputeLayout = findFirstComponentPosition(mComponentTreeHolders);
        if (mCurrentFirstVisiblePosition < mComponentTreeHolders.size()
            && positionToComputeLayout >= 0) {
          holderForRange = mComponentTreeHolders.get(positionToComputeLayout);
        }
      } else if (!mInsertsWaitingForInitialMeasure.isEmpty()) {
        final int positionToComputeLayout =
            findFirstAsyncComponentInsert(mInsertsWaitingForInitialMeasure);
        if (positionToComputeLayout >= 0) {
          holderForRange = mInsertsWaitingForInitialMeasure.get(positionToComputeLayout).mHolder;
        }
      }

      if (holderForRange != null) {
        initRange(
            SizeSpec.getSize(widthSpec),
            SizeSpec.getSize(heightSpec),
            holderForRange,
            getActualChildrenWidthSpec(holderForRange),
            getActualChildrenHeightSpec(holderForRange),
            scrollDirection);
        doFillViewportAfterFinishingMeasure = true;
      }
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
          mAsyncInsertsShouldWaitForMeasure = false;
        } else if (mRange != null) {
          outSize.width = mRange.measuredSize;
          mReMeasureEventEventHandler = null;
          mRequiresRemeasure.set(false);
          mAsyncInsertsShouldWaitForMeasure = false;
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
          mReMeasureEventEventHandler = mHasDynamicItemHeight ? reMeasureEventHandler : null;
          mRequiresRemeasure.set(mHasDynamicItemHeight);
          mAsyncInsertsShouldWaitForMeasure = false;
        } else if (mRange != null) {
          outSize.height = mRange.measuredSize;
          mReMeasureEventEventHandler = mHasDynamicItemHeight ? reMeasureEventHandler : null;
          mRequiresRemeasure.set(mHasDynamicItemHeight);
          mAsyncInsertsShouldWaitForMeasure = false;
        } else {
          outSize.height = 0;
          mRequiresRemeasure.set(true);
          mReMeasureEventEventHandler = reMeasureEventHandler;
        }
        break;
    }

    mMeasuredSize = new Size(outSize.width, outSize.height);
    mIsMeasured.set(true);

    // If we were in a position to recompute range, we are also in a position to re-fill the
    // viewport
    if (doFillViewportAfterFinishingMeasure && !mInsertsWaitingForInitialMeasure.isEmpty()) {
      fillAdapterWithInitialLayouts();
    } else if (doFillViewportAfterFinishingMeasure
        && shouldFillListViewport()
        && (mAllowFillViewportOnMainForTest || !ThreadUtils.isMainThread())) {
      fillListViewport();
    } else if (SectionsDebug.ENABLED) {
      Log.d(
          SectionsDebug.TAG,
          "("
              + hashCode()
              + ") Not filling viewport from measure - requested: "
              + doFillViewportAfterFinishingMeasure
              + ", supported: "
              + shouldFillListViewport()
              + ", isMainThread: "
              + ThreadUtils.isMainThread());
    }

    updateAsyncInsertOperations();

    if (mRange != null) {
      computeRange(mCurrentFirstVisiblePosition, mCurrentLastVisiblePosition);
    }
  }

  private boolean shouldFillListViewport() {
    return ComponentsConfiguration.fillListViewport
        || (ComponentsConfiguration.fillListViewportHScrollOnly
            && mLayoutInfo.getScrollDirection() == OrientationHelper.HORIZONTAL);
  }

  @GuardedBy("this")
  private void fillListViewport() {
    ComponentsSystrace.beginSection("fillListViewport");
    final int firstVisiblePosition = mLayoutInfo.findFirstVisibleItemPosition();

    // NB: This does not handle 1) partially visible items 2) item decorations
    final int startIndex =
        firstVisiblePosition != RecyclerView.NO_POSITION ? firstVisiblePosition : 0;

    computeLayoutsToFillListViewport(mComponentTreeHolders, startIndex);

    ComponentsSystrace.endSection();
  }

  @GuardedBy("this")
  private void fillAdapterWithInitialLayouts() {
    ComponentsSystrace.beginSection("fillAdapterWithInitialLayouts");
    if (mComponentTreeHolders.size() > 0) {
      throw new RuntimeException(
          "Should only fill adapter with initial layouts when there are no existing items in adapter!");
    }

    if (mAsyncInsertsShouldWaitForMeasure) {
      throw new IllegalStateException(
          "mAsyncInsertsShouldWaitForMeasure must be false to ensure no other inserts are added to those waiting for measure.");
    }

    final int numInserts = mInsertsWaitingForInitialMeasure.size();
    final ArrayList<ComponentTreeHolder> holdersForInsert = new ArrayList<>(numInserts);
    for (int i = 0; i < numInserts; i++) {
      final AsyncInsertOperation operation = mInsertsWaitingForInitialMeasure.get(i);

      if (i != operation.mPosition) {
        throw new RuntimeException(
            "For initial insert, expected insert position to match order of insertion! ("
                + i
                + " != "
                + operation.mPosition);
      }

      holdersForInsert.add(operation.mHolder);
    }

    final int numComputed = computeLayoutsToFillListViewport(holdersForInsert, 0);
    if (numComputed > 0) {
      maybePostInsertInitialLayoutsIntoAdapter(holdersForInsert.subList(0, numComputed));
    }

    for (int i = numComputed; i < numInserts; i++) {
      registerAsyncInsert(mInsertsWaitingForInitialMeasure.get(i));
    }
    closeCurrentBatch();

    mInsertsWaitingForInitialMeasure.clear();

    ComponentsSystrace.endSection();
  }

  @GuardedBy("this")
  private int computeLayoutsToFillListViewport(List<ComponentTreeHolder> holders, int offset) {
    final int width = mMeasuredSize.width;
    final int height = mMeasuredSize.height;
    final LayoutInfo.ViewportFiller filler = mLayoutInfo.createViewportFiller(width, height);
    if (filler == null) {
      return 0;
    }

    ComponentsSystrace.beginSection("computeLayoutsToFillListViewport");

    final int widthSpec = SizeSpec.makeSizeSpec(width, SizeSpec.EXACTLY);
    final int heightSpec = SizeSpec.makeSizeSpec(height, SizeSpec.EXACTLY);
    final Size outSize = new Size();

    int numInserted = 0;
    int index = offset;
    while (filler.wantsMore() && index < holders.size()) {
      final ComponentTreeHolder holder = holders.get(index);
      final RenderInfo renderInfo = holder.getRenderInfo();

      // Bail as soon as we see a View since we can't tell what height it is and don't want to
      // layout too much :(
      if (renderInfo.rendersView()) {
        break;
      }

      holder.computeLayoutSync(
          mComponentContext,
          mLayoutInfo.getChildWidthSpec(widthSpec, renderInfo),
          mLayoutInfo.getChildHeightSpec(heightSpec, renderInfo),
          outSize);

      filler.add(renderInfo, outSize.width, outSize.height);

      index++;
      numInserted++;
    }

    ComponentsSystrace.endSection();

    if (SectionsDebug.ENABLED) {
      Log.d(
          SectionsDebug.TAG,
          "("
              + hashCode()
              + ") filled viewport with "
              + numInserted
              + " items (holder.size() = "
              + holders.size()
              + ")");
    }

    return numInserted;
  }

  private void maybePostInsertInitialLayoutsIntoAdapter(final List<ComponentTreeHolder> toInsert) {
    if (ThreadUtils.isMainThread()) {
      insertInitialLayoutsIntoAdapter(toInsert);
    } else {
      mMainThreadHandler.post(
          new Runnable() {
            @Override
            public void run() {
              insertInitialLayoutsIntoAdapter(toInsert);
            }
          });
    }
  }

  private void insertInitialLayoutsIntoAdapter(List<ComponentTreeHolder> toInsert) {
    ThreadUtils.assertMainThread();

    synchronized (this) {
      for (int i = 0, size = toInsert.size(); i < size; i++) {
        final ComponentTreeHolder holder = toInsert.get(i);
        holder.setInserted(true);
        mComponentTreeHolders.add(i, holder);
        mInternalAdapter.notifyItemInserted(i);
      }
    }
  }

  @GuardedBy("this")
  private void updateAsyncInsertOperations() {
    for (AsyncBatch batch : mAsyncBatches) {
      updateBatch(batch);
    }
    if (mCurrentBatch != null) {
      updateBatch(mCurrentBatch);
    }
  }

  @GuardedBy("this")
  private void updateBatch(AsyncBatch batch) {
    for (AsyncOperation operation : batch.mOperations) {
      if (!(operation instanceof AsyncInsertOperation)) {
        continue;
      }

      final ComponentTreeHolder holder = ((AsyncInsertOperation) operation).mHolder;
      computeLayoutAsync(holder);
    }
  }

  @GuardedBy("this")
  private void computeLayoutAsync(ComponentTreeHolder holder) {
    // If there's an existing async layout that's compatible, this is a no-op. Otherwise, that
    // computation will be canceled (if it hasn't started) and this new one will run.
    final int widthSpec = getActualChildrenWidthSpec(holder);
    final int heightSpec = getActualChildrenHeightSpec(holder);
    holder.computeLayoutAsync(mComponentContext, widthSpec, heightSpec);
  }

  private static int findFirstComponentPosition(List<ComponentTreeHolder> holders) {
    for (int i = 0, size = holders.size(); i < size; i++) {
      if (holders.get(i).getRenderInfo().rendersComponent()) {
        return i;
      }
    }

    return -1;
  }

  private static int findFirstAsyncComponentInsert(List<AsyncInsertOperation> operations) {
    for (int i = 0, size = operations.size(); i < size; i++) {
      final AsyncInsertOperation operation = operations.get(i);
      if (operation.mHolder.getRenderInfo().rendersComponent()) {
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

  /**
   * Insert operation is not supported in case of circular recycler unless it is initial insert
   * because the indexes universe gets messed.
   */
  private void assertNoInsertOperationIfCircular() {
    if (mIsCircular && !mComponentTreeHolders.isEmpty()) {
      // Initialization of a list happens using insertRangeAt() or insertAt() operations,
      // so skip this check when mComponentTreeHolders was not populated yet
      throw new UnsupportedOperationException("Circular lists do not support insert operation");
    }
  }

  /**
   * Remove operation is not supported in case of circular recycler unless it's a removal if all
   * items because indexes universe gets messed.
   */
  @GuardedBy("this")
  private void assertNoRemoveOperationIfCircular(int removeCount) {
    if (mIsCircular
        && !mComponentTreeHolders.isEmpty()
        && mComponentTreeHolders.size() != removeCount) {
      // Allow only removal of all elements in case on notifyDataSetChanged() call
      throw new UnsupportedOperationException("Circular lists do not support insert operation");
    }
  }

  @GuardedBy("this")
  private void invalidateLayoutData() {
    mRange = null;
    for (int i = 0, size = mComponentTreeHolders.size(); i < size; i++) {
      mComponentTreeHolders.get(i).invalidateTree();
    }

    // We need to call this as we want to make sure everything is re-bound since we need new sizes
    // on all rows.
    if (Looper.myLooper() == Looper.getMainLooper()) {
      mInternalAdapter.notifyDataSetChanged();
    } else {
      mMainThreadHandler.removeCallbacks(mNotifyDatasetChangedRunnable);
      mMainThreadHandler.post(mNotifyDatasetChangedRunnable);
    }
  }

  @GuardedBy("this")
  private void initRange(
      int width,
      int height,
      ComponentTreeHolder holder,
      int childrenWidthSpec,
      int childrenHeightSpec,
      int scrollDirection) {
    ComponentsSystrace.beginSection("initRange");
    try {
      final Size size = new Size();
      holder.computeLayoutSync(mComponentContext, childrenWidthSpec, childrenHeightSpec, size);

      final int rangeSize =
          Math.max(mLayoutInfo.approximateRangeSize(size.width, size.height, width, height), 1);

      mRange = new RangeCalculationResult();
      mRange.measuredSize = scrollDirection == HORIZONTAL ? size.height : size.width;
      mRange.estimatedViewportCount = rangeSize;
    } finally {
      ComponentsSystrace.endSection();
    }
  }

  @GuardedBy("this")
  private void resetMeasuredSize(int width) {
    // we will set a range anyway if it's null, no need to do this now.
    if (mRange == null) {
      return;
    }
    int maxHeight = 0;

    for (int i = 0, size = mComponentTreeHolders.size(); i < size; i++) {
      final ComponentTreeHolder holder = mComponentTreeHolders.get(i);
      final int measuredItemHeight = holder.getMeasuredHeight();
      if (measuredItemHeight > maxHeight) {
        maxHeight = measuredItemHeight;
      }
    }

    if (maxHeight == mRange.measuredSize) {
      return;
    }

    final int rangeSize =
        Math.max(
            mLayoutInfo.approximateRangeSize(
                SizeSpec.getSize(mLastWidthSpec),
                SizeSpec.getSize(mLastHeightSpec),
                width,
                maxHeight),
            1);

    mRange.measuredSize = maxHeight;
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
    if (ComponentsConfiguration.enableSwapAdapter) {
      view.swapAdapter(mInternalAdapter, false);
    } else {
      view.setAdapter(mInternalAdapter);
    }
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
    } else if (mIsCircular) {
      // Initialize circular RecyclerView position
      final int jumpToMiddle = Integer.MAX_VALUE / 2;
      final int offsetFirstItem =
          mComponentTreeHolders.isEmpty() ? 0 : jumpToMiddle % mComponentTreeHolders.size();
      view.scrollToPosition(jumpToMiddle - offsetFirstItem);
    }

    enableStickyHeader(mMountedView);
  }

  private void enableStickyHeader(RecyclerView recyclerView) {
    if (mIsCircular) {
      Log.w(TAG, "Sticky header is not supported for circular RecyclerViews");
      return;
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      // Sticky header needs view translation APIs which are not available in Gingerbread and below.
      Log.w(TAG, "Sticky header is supported only on ICS (API14) and above");
      return;
    }
    if (recyclerView == null) {
      return;
    }
    SectionsRecyclerView sectionsRecycler = SectionsRecyclerView.getParentRecycler(recyclerView);
    if (sectionsRecycler == null) {
      return;
    }
    if (mStickyHeaderController == null) {
      mStickyHeaderController = new StickyHeaderController(this);
    }

    mStickyHeaderController.init(sectionsRecycler);
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
    if (ComponentsConfiguration.enableSwapAdapter) {
      view.swapAdapter(null, false);
    } else {
      view.setAdapter(null);
    }
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
  public void scrollToPosition(int position, boolean smoothScroll) {
    if (mHasAsyncOperations) {
      throw new RuntimeException(
          "TODO(T28718173): requestFocus not permitted with async operations yet.");
    }

    if (mMountedView == null) {
      mCurrentFirstVisiblePosition = position;
      return;
    }

    if (smoothScroll) {
      mMountedView.smoothScrollToPosition(position);
    } else {
      mMountedView.scrollToPosition(position);
    }
  }

  @UiThread
  public void scrollToPositionWithOffset(int position, int offset) {
    if (mHasAsyncOperations) {
      throw new RuntimeException(
          "TODO(T28718173): requestFocus not permitted with async operations yet.");
    }

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
    maybePostComputeRange();
  }

  @VisibleForTesting
  void onNewWorkingRange(
      int firstVisibleIndex,
      int lastVisibleIndex,
      int firstFullyVisibleIndex,
      int lastFullyVisibleIndex) {
    if (mRange == null
        || firstVisibleIndex == RecyclerView.NO_POSITION
        || lastVisibleIndex == RecyclerView.NO_POSITION) {
      return;
    }

    final int rangeSize =
        Math.max(mRange.estimatedViewportCount, lastVisibleIndex - firstVisibleIndex);
    final int layoutRangeSize = (int) (rangeSize * mRangeRatio);
    final int rangeStart = Math.max(0, firstVisibleIndex - layoutRangeSize);
    final int rangeEnd =
        Math.min(firstVisibleIndex + rangeSize + layoutRangeSize, mComponentTreeHolders.size() - 1);

    for (int position = rangeStart; position <= rangeEnd; position++) {
      final ComponentTreeHolder holder = mComponentTreeHolders.get(position);
      holder.checkWorkingRangeAndDispatch(
          position,
          firstVisibleIndex,
          lastVisibleIndex,
          firstFullyVisibleIndex,
          lastFullyVisibleIndex);
    }
  }

  private void maybePostComputeRange() {
    if ((ComponentsConfiguration.insertPostAsyncLayout || mInsertPostAsyncLayoutEnabled)
        && mMountedView != null) {
      mMountedView.removeCallbacks(mComputeRangeRunnable);
      ViewCompat.postOnAnimation(mMountedView, mComputeRangeRunnable);
    } else {
      computeRange(mCurrentFirstVisiblePosition, mCurrentLastVisiblePosition);
    }
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

    computeRangeLayout(treeHoldersSize, rangeStart, rangeEnd, mIsCircular);
  }

  private void computeRangeLayout(
      int treeHoldersSize, int rangeStart, int rangeEnd, boolean ignoreRange) {
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

      if (ignoreRange) {
        if (!holder.isTreeValid()) {
          holder.computeLayoutAsync(mComponentContext, childrenWidthSpec, childrenHeightSpec);
        }
      } else {
        if (i >= rangeStart && i <= rangeEnd) {
          if (!holder.isTreeValid()) {
            holder.computeLayoutAsync(mComponentContext, childrenWidthSpec, childrenHeightSpec);
          }
        } else if (holder.isTreeValid() && !holder.getRenderInfo().isSticky()) {
          holder.acquireStateHandlerAndReleaseTree();
        }
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
    if (mHasDynamicItemHeight) {
      return SizeSpec.UNSPECIFIED;
    }

    if (mIsMeasured.get() && !mRequiresRemeasure.get()) {
      return mLayoutInfo.getChildHeightSpec(
          SizeSpec.makeSizeSpec(mMeasuredSize.height, SizeSpec.EXACTLY),
          treeHolder.getRenderInfo());
    }

    return mLayoutInfo.getChildHeightSpec(mLastHeightSpec, treeHolder.getRenderInfo());
  }

  private AsyncInsertOperation createAsyncInsertOperation(int position, RenderInfo renderInfo) {
    final ComponentTreeHolder holder = createComponentTreeHolder(renderInfo);
    holder.setInserted(false);
    return new AsyncInsertOperation(position, holder);
  }

  /** Async operation types. */
  @IntDef({Operation.INSERT, Operation.REMOVE, Operation.REMOVE_RANGE, Operation.MOVE})
  @Retention(RetentionPolicy.SOURCE)
  private @interface Operation {
    int INSERT = 0;
    int REMOVE = 1;
    int REMOVE_RANGE = 2;
    int MOVE = 3;
  }

  /** An operation received from one of the *Async methods, pending execution. */
  private abstract static class AsyncOperation {

    private final int mOperation;

    public AsyncOperation(int operation) {
      mOperation = operation;
    }
  }

  private static final class AsyncInsertOperation extends AsyncOperation {

    private final int mPosition;
    private final ComponentTreeHolder mHolder;

    public AsyncInsertOperation(int position, ComponentTreeHolder holder) {
      super(Operation.INSERT);
      mPosition = position;
      mHolder = holder;
    }
  }

  private static final class AsyncRemoveOperation extends AsyncOperation {

    private final int mPosition;

    public AsyncRemoveOperation(int position) {
      super(Operation.REMOVE);
      mPosition = position;
    }
  }

  private static final class AsyncRemoveRangeOperation extends AsyncOperation {

    private final int mPosition;
    private final int mCount;

    public AsyncRemoveRangeOperation(int position, int count) {
      super(Operation.REMOVE_RANGE);
      mPosition = position;
      mCount = count;
    }
  }

  private static final class AsyncMoveOperation extends AsyncOperation {

    private final int mFromPosition;
    private final int mToPosition;

    public AsyncMoveOperation(int fromPosition, int toPosition) {
      super(Operation.MOVE);
      mFromPosition = fromPosition;
      mToPosition = toPosition;
    }
  }

  /** A batch of {@link AsyncOperation}s that should be applied all at once. */
  private static final class AsyncBatch {

    private final ArrayList<AsyncOperation> mOperations = new ArrayList<>();
  }

  private class RangeScrollListener extends RecyclerView.OnScrollListener {

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
      if (mCanPrefetchDisplayLists) {
        DisplayListUtils.prefetchDisplayLists(recyclerView);
      }
    }
  }

  private static class BaseViewHolder extends RecyclerView.ViewHolder {

    private final boolean isLithoViewType;
    @Nullable private ViewBinder viewBinder;

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
        final View view = viewCreator.createView(mComponentContext, parent);
        return new BaseViewHolder(view, false);
      } else {
        final LithoView lithoView =
            mLithoViewFactory == null
                ? new LithoView(mComponentContext, null)
                : mLithoViewFactory.createLithoView(mComponentContext);

        return new BaseViewHolder(lithoView, true);
      }
    }

    @Override
    @GuardedBy("RecyclerBinder.this")
    public void onBindViewHolder(BaseViewHolder holder, int position) {
      position = getNormalizedPosition(position);

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
        final boolean isOrientationVertical =
            mLayoutInfo.getScrollDirection() == OrientationHelper.VERTICAL;

        final int width;
        final int height;
        if (SizeSpec.getMode(childrenWidthSpec) == SizeSpec.EXACTLY) {
          width = SizeSpec.getSize(childrenWidthSpec);
        } else if (isOrientationVertical) {
          width = MATCH_PARENT;
        } else {
          width = WRAP_CONTENT;
        }

        if (SizeSpec.getMode(childrenHeightSpec) == SizeSpec.EXACTLY) {
          height = SizeSpec.getSize(childrenHeightSpec);
        } else if (isOrientationVertical) {
          height = WRAP_CONTENT;
        } else {
          height = MATCH_PARENT;
        }

        final RecyclerViewLayoutManagerOverrideParams layoutParams =
            new RecyclerViewLayoutManagerOverrideParams(
                width, height, childrenWidthSpec, childrenHeightSpec, renderInfo.isFullSpan());

        lithoView.setLayoutParams(layoutParams);
        lithoView.setComponentTree(componentTreeHolder.getComponentTree());
      } else {
        final ViewBinder viewBinder = renderInfo.getViewBinder();
        holder.viewBinder = viewBinder;
        viewBinder.bind(holder.itemView);
      }
    }

    @Override
    @GuardedBy("RecyclerBinder.this")
    public int getItemViewType(int position) {
      final RenderInfo renderInfo =
          mComponentTreeHolders.get(getNormalizedPosition(position)).getRenderInfo();
      if (renderInfo.rendersComponent()) {
        // Special value for LithoViews
        return mRenderInfoViewCreatorController.getComponentViewType();
      } else {
        return renderInfo.getViewType();
      }
    }

    @Override
    @GuardedBy("RecyclerBinder.this")
    public int getItemCount() {
      // We can ignore the synchronization here. We'll only add to this from the UiThread.
      // This read only happens on the UiThread as well and we are never writing this here.

      // If the recycler is circular, we have to simulate having an infinite number of items in the
      // adapter by returning Integer.MAX_VALUE.
      return mIsCircular ? Integer.MAX_VALUE : mComponentTreeHolders.size();
    }

    @Override
    public void onViewRecycled(BaseViewHolder holder) {
      if (holder.isLithoViewType) {
        final LithoView lithoView = (LithoView) holder.itemView;
        lithoView.unmountAllItems();
        lithoView.setComponentTree(null);
      } else {
        final ViewBinder viewBinder = holder.viewBinder;
        if (viewBinder != null) {
          viewBinder.unbind(holder.itemView);
          holder.viewBinder = null;
        }
      }
    }
  }

  /**
   * If the recycler is circular, returns the position of the {@link ComponentTreeHolder} that is
   * used to render the item at given position. Otherwise, it returns the position passed as
   * parameter, which is the same as the index of the {@link ComponentTreeHolder}.
   */
  @GuardedBy("this")
  private int getNormalizedPosition(int position) {
    return mIsCircular ? position % mComponentTreeHolders.size() : position;
  }

  public static class RecyclerViewLayoutManagerOverrideParams extends RecyclerView.LayoutParams
      implements LithoView.LayoutManagerOverrideParams {
    private final int mWidthMeasureSpec;
    private final int mHeightMeasureSpec;
    private final boolean mIsFullSpan;

    private RecyclerViewLayoutManagerOverrideParams(
        int width,
        int height,
        int overrideWidthMeasureSpec,
        int overrideHeightMeasureSpec,
        boolean isFullSpan) {
      super(width, height);
      mWidthMeasureSpec = overrideWidthMeasureSpec;
      mHeightMeasureSpec = overrideHeightMeasureSpec;
      mIsFullSpan = isFullSpan;
    }

    @Override
    public int getWidthMeasureSpec() {
      return mWidthMeasureSpec;
    }

    @Override
    public int getHeightMeasureSpec() {
      return mHeightMeasureSpec;
    }

    public boolean isFullSpan() {
      return mIsFullSpan;
    }
  }

  private ComponentTreeHolder createComponentTreeHolder(RenderInfo renderInfo) {
    return mComponentTreeHolderFactory.create(
        renderInfo,
        mLayoutHandlerFactory != null
            ? mLayoutHandlerFactory.createLayoutCalculationHandler(renderInfo)
            : null,
        mCanPrefetchDisplayLists,
        mCanCacheDrawingDisplayLists,
        mHasDynamicItemHeight ? mComponentTreeMeasureListenerFactory : null,
        mSplitLayoutTag);
  }

  private void logTooManyPostingAttempts() {
    final ComponentsLogger logger = mComponentContext.getLogger();
    if (logger != null) {
      final LogEvent logEvent = logger.newEvent(EVENT_ERROR);
      if (logger.isTracing(logEvent)) {
        final StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("Tried posting too many times! ");
        messageBuilder.append("mMountedView is attached: ");
        messageBuilder.append(mMountedView.isAttachedToWindow());
        messageBuilder.append("; ");
        messageBuilder.append("mMountedView visibility: ");
        messageBuilder.append(mMountedView.getVisibility());
        messageBuilder.append("; ");
        messageBuilder.append("adapter has updates: ");
        messageBuilder.append(mMountedView.hasPendingAdapterUpdates());
        messageBuilder.append("; ");
        messageBuilder.append("mMountedView requested layout: ");
        messageBuilder.append(mMountedView.isLayoutRequested());
        messageBuilder.append("; ");

        for (int i = mCurrentFirstVisiblePosition; i <= mCurrentLastVisiblePosition; i++) {
          final RenderInfo renderInfo = mComponentTreeHolders.get(i).getRenderInfo();
          if (renderInfo.rendersComponent()) {
            messageBuilder.append(renderInfo.getComponent().getSimpleName());
          } else {
            messageBuilder.append(renderInfo.getViewCreator().getClass());
          }

          messageBuilder.append(" ");
        }

        logEvent.addParam(PARAM_MESSAGE, messageBuilder.toString());
        logger.log(logEvent);
      }
    }
  }
}
