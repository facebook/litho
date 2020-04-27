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

package com.facebook.litho;

import static android.os.Process.THREAD_PRIORITY_DEFAULT;
import static com.facebook.litho.FrameworkLogEvents.EVENT_LAYOUT_STATE_FUTURE_GET_WAIT;
import static com.facebook.litho.FrameworkLogEvents.EVENT_PRE_ALLOCATE_MOUNT_CONTENT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_IS_MAIN_THREAD;
import static com.facebook.litho.FrameworkLogEvents.PARAM_LAYOUT_FUTURE_WAIT_FOR_RESULT;
import static com.facebook.litho.HandlerInstrumenter.instrumentLithoHandler;
import static com.facebook.litho.LayoutState.CalculateLayoutSource;
import static com.facebook.litho.StateContainer.StateUpdate;
import static com.facebook.litho.ThreadUtils.assertHoldsLock;
import static com.facebook.litho.ThreadUtils.assertMainThread;
import static com.facebook.litho.ThreadUtils.isMainThread;
import static com.facebook.litho.WorkContinuationInstrumenter.markFailure;
import static com.facebook.litho.WorkContinuationInstrumenter.onBeginWorkContinuation;
import static com.facebook.litho.WorkContinuationInstrumenter.onEndWorkContinuation;
import static com.facebook.litho.WorkContinuationInstrumenter.onOfferWorkForContinuation;
import static com.facebook.litho.config.ComponentsConfiguration.DEFAULT_BACKGROUND_THREAD_PRIORITY;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import com.facebook.infer.annotation.ReturnsOwnership;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.litho.LithoHandler.DefaultLithoHandler;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.animation.AnimatedProperty;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.perfboost.LithoPerfBooster;
import com.facebook.litho.stats.LithoStats;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.CheckReturnValue;
import javax.annotation.concurrent.GuardedBy;

/**
 * Represents a tree of components and controls their life cycle. ComponentTree takes in a single
 * root component and recursively invokes its OnCreateLayout to create a tree of components.
 * ComponentTree is responsible for refreshing the mounted state of a component with new props.
 *
 * <p>The usual use case for {@link ComponentTree} is: <code>
 * ComponentTree component = ComponentTree.create(context, MyComponent.create());
 * myHostView.setRoot(component);
 * </code>
 */
@ThreadSafe
public class ComponentTree {

  public static final int INVALID_ID = -1;
  private static final String INVALID_KEY = "LithoTooltipController:InvalidKey";
  private static final String INVALID_HANDLE = "LithoTooltipController:InvalidHandle";
  private static final String TAG = ComponentTree.class.getSimpleName();
  private static final int SIZE_UNINITIALIZED = -1;
  private static final String DEFAULT_LAYOUT_THREAD_NAME = "ComponentLayoutThread";
  private static final String DEFAULT_PMC_THREAD_NAME = "PreallocateMountContentThread";
  private static final String EMPTY_STRING = "";
  private static final String REENTRANT_MOUNTS_EXCEED_MAX_ATTEMPTS =
      "ComponentTree:ReentrantMountsExceedMaxAttempts";
  private static final int REENTRANT_MOUNTS_MAX_ATTEMPTS = 25;
  private static final String CT_CONTEXT_IS_DIFFERENT_FROM_ROOT_BUILDER_CONTEXT =
      "ComponentTree:CTContextIsDifferentFromRootBuilderContext";

  private static final int SCHEDULE_NONE = 0;
  private static final int SCHEDULE_LAYOUT_ASYNC = 1;
  private static final int SCHEDULE_LAYOUT_SYNC = 2;
  public static final int STATE_UPDATES_IN_LOOP_THRESHOLD = 50;
  private static final String STATE_UPDATES_IN_LOOP_EXCEED_THRESHOLD =
      "ComponentTree:StateUpdatesWhenLayoutInProgressExceedsThreshold";
  private static boolean sBoostPerfLayoutStateFuture = false;
  private final boolean mAreTransitionsEnabled;
  private boolean mReleased;
  private String mReleasedComponent;
  private @Nullable volatile AttachDetachHandler mAttachDetachHandler;
  private @Nullable Deque<ReentrantMount> mReentrantMounts;

  @GuardedBy("this")
  private int mStateUpdatesFromCreateLayoutCount;

  private final boolean mIncrementalVisibility;
  private final @RecyclingMode int mRecyclingMode;

  private final InitialStateContainer mInitialStateContainer = new InitialStateContainer();

  @IntDef({SCHEDULE_NONE, SCHEDULE_LAYOUT_ASYNC, SCHEDULE_LAYOUT_SYNC})
  @Retention(RetentionPolicy.SOURCE)
  private @interface PendingLayoutCalculation {}

  public interface MeasureListener {

    /**
     * This callback gets called every time a ComponentTree commits a new layout computation. The
     * call is executed on the same thread that computed the newly committed layout but outside the
     * commit lock. This means that in practice the calls are not guaranteed to be ordered. A layout
     * X committed before a layout Y could end up executing its MeasureListener's callback after the
     * callback of layout Y. Clients that need guarantee over the ordering can rely on the
     * layoutVersion parameter that is guaranteed to be increasing for successive commits (in the
     * example layout X callback will receive a layoutVersion that is lower than the layoutVersion
     * for layout Y)
     *
     * @param layoutVersion the layout version associated with the layout that triggered this
     *     callback
     * @param width the resulting width from the committed layout computation
     * @param height the resulting height from the committed layout computation
     * @param stateUpdate whether this layout computation was triggered by a state update.
     */
    void onSetRootAndSizeSpec(int layoutVersion, int width, int height, boolean stateUpdate);
  }

  @GuardedBy("this")
  private @Nullable List<MeasureListener> mMeasureListeners;

  /**
   * Listener that will be notified when a new LayoutState is computed and ready to be committed to
   * this ComponentTree.
   */
  public interface NewLayoutStateReadyListener {
    void onNewLayoutStateReady(ComponentTree componentTree);
  }

  private static final AtomicInteger sIdGenerator = new AtomicInteger(0);
  // Do not access sDefaultLayoutThreadLooper directly, use getDefaultLayoutThreadLooper().
  @GuardedBy("ComponentTree.class")
  private static volatile Looper sDefaultLayoutThreadLooper;

  @GuardedBy("ComponentTree.class")
  private static volatile Looper sDefaultPreallocateMountContentThreadLooper;

  private static final ThreadLocal<WeakReference<LithoHandler>> sSyncStateUpdatesHandler =
      new ThreadLocal<>();

  @Nullable private final IncrementalMountHelper mIncrementalMountHelper;
  private final boolean mShouldPreallocatePerMountSpec;
  private final Runnable mPreAllocateMountContentRunnable =
      new Runnable() {
        @Override
        public void run() {
          preAllocateMountContent(mShouldPreallocatePerMountSpec);
        }
      };

  private final Object mUpdateStateSyncRunnableLock = new Object();

  @GuardedBy("mUpdateStateSyncRunnableLock")
  private @Nullable UpdateStateSyncRunnable mUpdateStateSyncRunnable;

  private final ComponentContext mContext;

  @Nullable private LithoHandler mPreAllocateMountContentHandler;

  // These variables are only accessed from the main thread.
  @ThreadConfined(ThreadConfined.UI)
  private boolean mIsMounting;

  @ThreadConfined(ThreadConfined.UI)
  private final boolean mIncrementalMountEnabled;

  @ThreadConfined(ThreadConfined.UI)
  private final boolean mVisibilityProcessingEnabled;

  @ThreadConfined(ThreadConfined.UI)
  private final boolean mIsLayoutDiffingEnabled;

  @ThreadConfined(ThreadConfined.UI)
  private boolean mIsAttached;

  @ThreadConfined(ThreadConfined.UI)
  private final boolean mIsAsyncUpdateStateEnabled;

  @ThreadConfined(ThreadConfined.UI)
  private LithoView mLithoView;

  @ThreadConfined(ThreadConfined.UI)
  private LithoHandler mLayoutThreadHandler;

  private LithoHandler mMainThreadHandler = new DefaultLithoHandler(Looper.getMainLooper());
  private final Runnable mBackgroundLayoutStateUpdateRunnable =
      new Runnable() {
        @Override
        public void run() {
          backgroundLayoutStateUpdated();
        }
      };
  private volatile NewLayoutStateReadyListener mNewLayoutStateReadyListener;

  private final Object mCurrentCalculateLayoutRunnableLock = new Object();

  @GuardedBy("mCurrentCalculateLayoutRunnableLock")
  private @Nullable CalculateLayoutRunnable mCurrentCalculateLayoutRunnable;

  private final Object mLayoutStateFutureLock = new Object();
  private final boolean mUseCancelableLayoutFutures;

  @GuardedBy("mLayoutStateFutureLock")
  private final List<LayoutStateFuture> mLayoutStateFutures = new ArrayList<>();

  private volatile boolean mHasMounted;

  /** Transition that animates width of root component (LithoView). */
  @ThreadConfined(ThreadConfined.UI)
  @Nullable
  Transition.RootBoundsTransition mRootWidthAnimation;

  /** Transition that animates height of root component (LithoView). */
  @ThreadConfined(ThreadConfined.UI)
  @Nullable
  Transition.RootBoundsTransition mRootHeightAnimation;

  @Nullable
  @GuardedBy("this")
  private Component mRoot;

  @GuardedBy("this")
  private int mExternalRootVersion = -1;

  // Versioning that gets incremented every time we start a new layout computation. This can
  // be useful for stateful objects shared across layouts that need to check whether for example
  // a measure/onCreateLayout call is being executed in the context of an old layout calculation.
  @GuardedBy("this")
  private int mLayoutVersion;

  @Nullable
  @GuardedBy("this")
  private TreeProps mRootTreeProps;

  @GuardedBy("this")
  private int mWidthSpec = SIZE_UNINITIALIZED;

  @GuardedBy("this")
  private int mHeightSpec = SIZE_UNINITIALIZED;

  @GuardedBy("this")
  private int mPendingLayoutWidthSpec = SIZE_UNINITIALIZED;

  @GuardedBy("this")
  private int mPendingLayoutHeightSpec = SIZE_UNINITIALIZED;

  // This is written to only by the main thread with the lock held, read from the main thread with
  // no lock held, or read from any other thread with the lock held.
  @Nullable private LayoutState mMainThreadLayoutState;

  // The semantics here are tricky. Whenever you transfer mBackgroundLayoutState to a local that
  // will be accessed outside of the lock, you must set mBackgroundLayoutState to null to ensure
  // that the current thread alone has access to the LayoutState, which is single-threaded.
  @GuardedBy("this")
  @Nullable
  private LayoutState mBackgroundLayoutState;

  // TODO(t64511317): Merge mLatestLayoutState and mBackgroundLayoutState
  @GuardedBy("this")
  @Nullable
  private LayoutState mLatestLayoutState;

  @GuardedBy("this")
  private StateHandler mStateHandler;

  @ThreadConfined(ThreadConfined.UI)
  private RenderState mPreviousRenderState;

  @ThreadConfined(ThreadConfined.UI)
  private boolean mPreviousRenderStateSetFromBuilder = false;

  protected final int mId;

  @GuardedBy("this")
  private boolean mIsMeasuring;

  @PendingLayoutCalculation
  @GuardedBy("this")
  private int mScheduleLayoutAfterMeasure;

  private final EventHandlersController mEventHandlersController = new EventHandlersController();

  private final EventTriggersContainer mEventTriggersContainer = new EventTriggersContainer();

  @GuardedBy("this")
  private final WorkingRangeStatusHandler mWorkingRangeStatusHandler =
      new WorkingRangeStatusHandler();

  private boolean mForceLayout;

  private final boolean isReconciliationEnabled;

  private final boolean mMoveLayoutsBetweenThreads;

  private final boolean mForceAsyncStateUpdate;

  private final @Nullable String mLogTag;

  private final @Nullable ComponentsLogger mLogger;

  public static Builder create(ComponentContext context) {
    return new ComponentTree.Builder(context);
  }

  public static Builder create(ComponentContext context, Component.Builder<?> root) {
    return create(context, root.build());
  }

  public static Builder create(ComponentContext context, Component root) {
    return new ComponentTree.Builder(context).withRoot(root);
  }

  protected ComponentTree(Builder builder) {
    mContext = ComponentContext.withComponentTree(builder.context, this);
    mRoot = wrapRootInErrorBoundary(builder.root);
    mIncrementalMountEnabled = builder.incrementalMountEnabled;
    mVisibilityProcessingEnabled = builder.visibilityProcessingEnabled;
    mIsLayoutDiffingEnabled = builder.isLayoutDiffingEnabled;
    mLayoutThreadHandler = builder.layoutThreadHandler;
    mShouldPreallocatePerMountSpec = builder.shouldPreallocatePerMountSpec;
    mPreAllocateMountContentHandler = builder.preAllocateMountContentHandler;
    mIsAsyncUpdateStateEnabled = builder.asyncStateUpdates;
    mHasMounted = builder.hasMounted;
    addMeasureListener(builder.mMeasureListener);
    mUseCancelableLayoutFutures = builder.useCancelableLayoutFutures;
    mMoveLayoutsBetweenThreads = builder.canInterruptAndMoveLayoutsBetweenThreads;
    isReconciliationEnabled = builder.isReconciliationEnabled;
    mForceAsyncStateUpdate = builder.shouldForceAsyncStateUpdate;
    mRecyclingMode = builder.recyclingMode;

    if (mPreAllocateMountContentHandler == null && builder.canPreallocateOnDefaultHandler) {
      mPreAllocateMountContentHandler =
          new DefaultLithoHandler(getDefaultPreallocateMountContentThreadLooper());
    }

    final StateHandler builderStateHandler = builder.stateHandler;
    mStateHandler =
        builderStateHandler == null ? StateHandler.createNewInstance(null) : builderStateHandler;

    if (builder.previousRenderState != null) {
      mPreviousRenderState = builder.previousRenderState;
      mPreviousRenderStateSetFromBuilder = true;
    }

    if (builder.overrideComponentTreeId != -1) {
      mId = builder.overrideComponentTreeId;
    } else {
      mId = generateComponentTreeId();
    }

    mIncrementalMountHelper =
        ComponentsConfiguration.USE_INCREMENTAL_MOUNT_HELPER
            ? new IncrementalMountHelper(this)
            : null;

    // Instrument LithoHandlers.
    mMainThreadHandler = instrumentLithoHandler(mMainThreadHandler);
    mLayoutThreadHandler = ensureAndInstrumentLayoutThreadHandler(mLayoutThreadHandler);
    if (mPreAllocateMountContentHandler != null) {
      mPreAllocateMountContentHandler = instrumentLithoHandler(mPreAllocateMountContentHandler);
    }
    mLogger = builder.logger;
    mLogTag = builder.logTag;
    mAreTransitionsEnabled = TransitionUtils.areTransitionsEnabled(mContext.getAndroidContext());
    mIncrementalVisibility = builder.incrementalVisibility;
  }

  /**
   * The provided measureListener will be called when a valid layout is commited.
   *
   * @param measureListener
   */
  public void addMeasureListener(@Nullable MeasureListener measureListener) {
    if (measureListener == null) {
      return;
    }

    synchronized (this) {
      if (mMeasureListeners == null) {
        mMeasureListeners = new ArrayList<>();
      }

      mMeasureListeners.add(measureListener);
    }
  }

  public void clearMeasureListener(MeasureListener measureListener) {
    if (measureListener == null) {
      return;
    }

    synchronized (this) {
      if (mMeasureListeners != null) {
        mMeasureListeners.remove(measureListener);
      }
    }
  }

  boolean areTransitionsEnabled() {
    return mAreTransitionsEnabled;
  }

  private static LithoHandler ensureAndInstrumentLayoutThreadHandler(
      @Nullable LithoHandler handler) {
    if (handler == null) {
      handler =
          ComponentsConfiguration.threadPoolForBackgroundThreadsConfig == null
              ? new DefaultLithoHandler(getDefaultLayoutThreadLooper())
              : ThreadPoolLayoutHandler.getDefaultInstance();
    } else {
      if (sDefaultLayoutThreadLooper != null
          && sBoostPerfLayoutStateFuture == false
          && ComponentsConfiguration.boostPerfLayoutStateFuture == true
          && ComponentsConfiguration.perfBoosterFactory != null) {
        /**
         * Right now we don't care about testing this per surface, so we'll use the config value.
         */
        LithoPerfBooster booster = ComponentsConfiguration.perfBoosterFactory.acquireInstance();
        booster.markImportantThread(new Handler(sDefaultLayoutThreadLooper));
        sBoostPerfLayoutStateFuture = true;
      }
    }
    return instrumentLithoHandler(handler);
  }

  @Nullable
  @ThreadConfined(ThreadConfined.UI)
  LayoutState getMainThreadLayoutState() {
    return mMainThreadLayoutState;
  }

  @Nullable
  @VisibleForTesting
  @GuardedBy("this")
  protected LayoutState getBackgroundLayoutState() {
    return mBackgroundLayoutState;
  }

  @VisibleForTesting
  @Nullable
  public LayoutState getLatestLayoutState() {
    return mLatestLayoutState;
  }

  /**
   * Picks the best LayoutState and sets it in mMainThreadLayoutState. The return value is a
   * LayoutState that must be released (after the lock is released). This awkward contract is
   * necessary to ensure thread-safety.
   */
  @CheckReturnValue
  @ReturnsOwnership
  @ThreadConfined(ThreadConfined.UI)
  @GuardedBy("this")
  private LayoutState setBestMainThreadLayoutAndReturnOldLayout() {
    assertHoldsLock(this);

    final boolean isMainThreadLayoutBest = isBestMainThreadLayout();

    if (isMainThreadLayoutBest) {
      // We don't want to hold onto mBackgroundLayoutState since it's unlikely
      // to ever be used again. We return mBackgroundLayoutState to indicate it
      // should be released after exiting the lock.
      final LayoutState toRelease = mBackgroundLayoutState;
      mBackgroundLayoutState = null;
      return toRelease;
    } else {
      // Since we are changing layout states we'll need to remount.
      if (mLithoView != null) {
        mLithoView.setMountStateDirty();
      }

      final LayoutState toRelease = mMainThreadLayoutState;
      mMainThreadLayoutState = mBackgroundLayoutState;
      mBackgroundLayoutState = null;

      return toRelease;
    }
  }

  @CheckReturnValue
  @ReturnsOwnership
  @ThreadConfined(ThreadConfined.UI)
  @GuardedBy("this")
  private boolean isBestMainThreadLayout() {
    assertHoldsLock(this);

    // If everything matches perfectly then we prefer mMainThreadLayoutState
    // because that means we don't need to remount.
    if (isCompatibleComponentAndSpec(mMainThreadLayoutState)) {
      return true;
    } else if (isCompatibleSpec(mBackgroundLayoutState, mWidthSpec, mHeightSpec)
        || !isCompatibleSpec(mMainThreadLayoutState, mWidthSpec, mHeightSpec)) {
      // If mMainThreadLayoutState isn't a perfect match, we'll prefer
      // mBackgroundLayoutState since it will have the more recent create.
      return false;
    } else {
      // If the main thread layout is still compatible size-wise, and the
      // background one is not, then we'll do nothing. We want to keep the same
      // main thread layout so that we don't force main thread re-layout.
      return true;
    }
  }

  /** Whether this ComponentTree has been mounted at least once. */
  public boolean hasMounted() {
    return mHasMounted;
  }

  public void setNewLayoutStateReadyListener(NewLayoutStateReadyListener listener) {
    mNewLayoutStateReadyListener = listener;
  }

  /**
   * Provide custom {@link LithoHandler}. If null is provided default one will be used for layouts.
   */
  @ThreadConfined(ThreadConfined.UI)
  public void updateLayoutThreadHandler(@Nullable LithoHandler layoutThreadHandler) {
    synchronized (mUpdateStateSyncRunnableLock) {
      if (mUpdateStateSyncRunnable != null) {
        mLayoutThreadHandler.remove(mUpdateStateSyncRunnable);
      }
    }
    synchronized (mCurrentCalculateLayoutRunnableLock) {
      if (mCurrentCalculateLayoutRunnable != null) {
        mLayoutThreadHandler.remove(mCurrentCalculateLayoutRunnable);
      }
    }
    mLayoutThreadHandler = ensureAndInstrumentLayoutThreadHandler(layoutThreadHandler);
  }

  @VisibleForTesting
  public NewLayoutStateReadyListener getNewLayoutStateReadyListener() {
    return mNewLayoutStateReadyListener;
  }

  @ThreadConfined(ThreadConfined.UI)
  private void dispatchNewLayoutStateReady() {
    final NewLayoutStateReadyListener listener = mNewLayoutStateReadyListener;
    if (listener != null) {
      listener.onNewLayoutStateReady(this);
    }
  }

  private void backgroundLayoutStateUpdated() {
    assertMainThread();

    // If we aren't attached, then we have nothing to do. We'll handle
    // everything in onAttach.
    if (!mIsAttached) {
      dispatchNewLayoutStateReady();
      return;
    }

    final boolean layoutStateUpdated;
    final int componentRootId;
    synchronized (this) {
      if (mRoot == null) {
        // We have been released. Abort.
        return;
      }

      final LayoutState oldMainThreadLayoutState = mMainThreadLayoutState;
      setBestMainThreadLayoutAndReturnOldLayout();
      layoutStateUpdated = (mMainThreadLayoutState != oldMainThreadLayoutState);
      componentRootId = mRoot.getId();
    }

    if (!layoutStateUpdated) {
      return;
    }

    dispatchNewLayoutStateReady();

    // Dispatching the listener may have detached us -- verify that's not the case
    if (!mIsAttached) {
      return;
    }

    // We defer until measure if we don't yet have a width/height
    final int viewWidth = mLithoView.getMeasuredWidth();
    final int viewHeight = mLithoView.getMeasuredHeight();
    if (viewWidth == 0 && viewHeight == 0) {
      // The host view has not been measured yet.
      return;
    }

    final boolean needsAndroidLayout =
        !isCompatibleComponentAndSize(
            mMainThreadLayoutState, componentRootId, viewWidth, viewHeight);

    if (needsAndroidLayout) {
      mLithoView.requestLayout();
    } else {
      mountComponentIfNeeded();
    }
  }

  /**
   * If we have transition key on root component we might run bounds animation on LithoView which
   * requires to know animating value in {@link LithoView#onMeasure(int, int)}. In such case we need
   * to collect all transitions before mount happens but after layout computation is finalized.
   */
  void maybeCollectTransitions() {
    assertMainThread();

    final LayoutState layoutState = mMainThreadLayoutState;
    if (layoutState == null || layoutState.getRootTransitionId() == null) {
      return;
    }

    mLithoView.maybeCollectAllTransitions(layoutState, this);
  }

  void attach() {
    assertMainThread();

    if (mLithoView == null) {
      throw new IllegalStateException("Trying to attach a ComponentTree without a set View");
    }

    if (mIncrementalMountHelper != null) {
      mIncrementalMountHelper.onAttach(mLithoView);
    }

    final int componentRootId;
    synchronized (this) {
      // We need to track that we are attached regardless...
      mIsAttached = true;

      // ... and then we do state transfer
      setBestMainThreadLayoutAndReturnOldLayout();

      if (mRoot == null) {
        throw new IllegalStateException(
            "Trying to attach a ComponentTree with a null root. Is released: "
                + mReleased
                + ", Released Component name is: "
                + mReleasedComponent);
      }

      componentRootId = mRoot.getId();
    }

    // We defer until measure if we don't yet have a width/height
    final int viewWidth = mLithoView.getMeasuredWidth();
    final int viewHeight = mLithoView.getMeasuredHeight();
    if (viewWidth == 0 && viewHeight == 0) {
      // The host view has not been measured yet.
      return;
    }

    final boolean needsAndroidLayout =
        !isCompatibleComponentAndSize(
            mMainThreadLayoutState, componentRootId, viewWidth, viewHeight);

    if (needsAndroidLayout || mLithoView.isMountStateDirty()) {
      mLithoView.requestLayout();
    } else {
      mLithoView.rebind();
    }
  }

  private static boolean hasSameRootContext(Context context1, Context context2) {
    return ContextUtils.getRootContext(context1) == ContextUtils.getRootContext(context2);
  }

  @ThreadConfined(ThreadConfined.UI)
  boolean isMounting() {
    return mIsMounting;
  }

  private boolean mountComponentIfNeeded() {
    if (mLithoView.isMountStateDirty() || mLithoView.mountStateNeedsRemount()) {
      if (mIncrementalMountEnabled) {
        incrementalMountComponent();
      } else {
        final Rect visibleRect = new Rect();
        mLithoView.getLocalVisibleRect(visibleRect);
        mountComponent(visibleRect, true);
      }

      return true;
    }

    return false;
  }

  @UiThread
  void incrementalMountComponent() {
    assertMainThread();

    if (!mIncrementalMountEnabled) {
      throw new IllegalStateException(
          "Calling incrementalMountComponent() but incremental mount is not enabled");
    }

    if (mLithoView == null) {
      return;
    }

    // Per ComponentTree visible area. Because LithoViews can be nested and mounted
    // not in "depth order", this variable cannot be static.
    final Rect currentVisibleArea = new Rect();

    if (ComponentsConfiguration.incrementalMountWhenNotVisible) {
      boolean isVisible = mIsAttached && mLithoView.getLocalVisibleRect(currentVisibleArea);

      if (!isVisible) {
        // We just do this so that every mount call when the LithoView is not visible is done with
        // the same rect so that we can return early if possible.
        currentVisibleArea.setEmpty();
      }

      mountComponent(currentVisibleArea, true);
    } else {
      if (mLithoView.getLocalVisibleRect(currentVisibleArea)
          // It might not be yet visible but animating from 0 height/width in which case we still
          // need
          // to mount them to trigger animation.
          || animatingRootBoundsFromZero(currentVisibleArea)) {
        mountComponent(currentVisibleArea, true);
      }
      // if false: no-op, doesn't have visible area, is not ready or not attached
    }
  }

  private boolean animatingRootBoundsFromZero(Rect currentVisibleArea) {
    return !mHasMounted
        && ((mRootHeightAnimation != null && currentVisibleArea.height() == 0)
            || (mRootWidthAnimation != null && currentVisibleArea.width() == 0));
  }

  /**
   * @return the width value that LithoView should be animating from. If this returns non-negative
   *     value, we will override the measured width with this value so that initial animated value
   *     is correctly applied.
   */
  @ThreadConfined(ThreadConfined.UI)
  int getInitialAnimatedLithoViewWidth(int currentAnimatedWidth, boolean hasNewComponentTree) {
    return getInitialAnimatedLithoViewDimension(
        currentAnimatedWidth, hasNewComponentTree, mRootWidthAnimation, AnimatedProperties.WIDTH);
  }

  /**
   * @return the height value that LithoView should be animating from. If this returns non-negative
   *     value, we will override the measured height with this value so that initial animated value
   *     is correctly applied.
   */
  @ThreadConfined(ThreadConfined.UI)
  int getInitialAnimatedLithoViewHeight(int currentAnimatedHeight, boolean hasNewComponentTree) {
    return getInitialAnimatedLithoViewDimension(
        currentAnimatedHeight,
        hasNewComponentTree,
        mRootHeightAnimation,
        AnimatedProperties.HEIGHT);
  }

  private int getInitialAnimatedLithoViewDimension(
      int currentAnimatedDimension,
      boolean hasNewComponentTree,
      @Nullable Transition.RootBoundsTransition rootBoundsTransition,
      AnimatedProperty property) {
    if (rootBoundsTransition == null) {
      return -1;
    }

    if (!mHasMounted && rootBoundsTransition.appearTransition != null) {
      return (int)
          Transition.getRootAppearFromValue(
              rootBoundsTransition.appearTransition, mMainThreadLayoutState, property);
    }

    if (mHasMounted && !hasNewComponentTree) {
      return currentAnimatedDimension;
    }

    return -1;
  }

  @ThreadConfined(ThreadConfined.UI)
  void setRootWidthAnimation(@Nullable Transition.RootBoundsTransition rootWidthAnimation) {
    mRootWidthAnimation = rootWidthAnimation;
  }

  @ThreadConfined(ThreadConfined.UI)
  void setRootHeightAnimation(@Nullable Transition.RootBoundsTransition rootHeightAnimation) {
    mRootHeightAnimation = rootHeightAnimation;
  }

  /**
   * @return whether this ComponentTree has a computed layout that will work for the given measure
   *     specs.
   */
  public synchronized boolean hasCompatibleLayout(int widthSpec, int heightSpec) {
    return isCompatibleSpec(mMainThreadLayoutState, widthSpec, heightSpec)
        || isCompatibleSpec(mBackgroundLayoutState, widthSpec, heightSpec);
  }

  @UiThread
  void mountComponent(@Nullable Rect currentVisibleArea, boolean processVisibilityOutputs) {
    assertMainThread();

    if (mIsMounting) {
      collectReentrantMount(new ReentrantMount(currentVisibleArea, processVisibilityOutputs));
      return;
    }

    mountComponentInternal(currentVisibleArea, processVisibilityOutputs);

    consumeReentrantMounts();
  }

  private void mountComponentInternal(
      @Nullable Rect currentVisibleArea, boolean processVisibilityOutputs) {
    final LayoutState layoutState = mMainThreadLayoutState;
    if (layoutState == null) {
      Log.w(TAG, "Main Thread Layout state is not found");
      return;
    }

    final boolean isDirtyMount = mLithoView.isMountStateDirty();

    if (!isDirtyMount
        && mHasMounted
        && ComponentsConfiguration.incrementalMountWhenNotVisible
        && currentVisibleArea != null
        && currentVisibleArea.equals(mLithoView.getPreviousMountBounds())) {
      return;
    }

    mIsMounting = true;

    if (!mHasMounted) {
      mLithoView.setIsFirstMountOfComponentTree();
      mHasMounted = true;
    }

    // currentVisibleArea null or empty => mount all
    try {
      mLithoView.mount(layoutState, currentVisibleArea, processVisibilityOutputs);
      if (isDirtyMount) {
        recordRenderData(layoutState);
      }
    } finally {
      mIsMounting = false;
      mRootHeightAnimation = null;
      mRootWidthAnimation = null;

      if (isDirtyMount) {
        mLithoView.onDirtyMountComplete();
      }
    }
  }

  private void collectReentrantMount(ReentrantMount reentrantMount) {
    if (mReentrantMounts == null) {
      mReentrantMounts = new ArrayDeque<>();
    } else if (mReentrantMounts.size() > REENTRANT_MOUNTS_MAX_ATTEMPTS) {
      logReentrantMountsExceedMaxAttempts();
      mReentrantMounts.clear();
      return;
    }
    mReentrantMounts.add(reentrantMount);
  }

  private void consumeReentrantMounts() {
    if (mReentrantMounts != null) {
      final Deque<ReentrantMount> reentrantMounts = new ArrayDeque<>(mReentrantMounts);
      mReentrantMounts.clear();

      while (!reentrantMounts.isEmpty()) {
        final ReentrantMount reentrantMount = reentrantMounts.pollFirst();
        mLithoView.setMountStateDirty();
        mountComponentInternal(
            reentrantMount.currentVisibleArea, reentrantMount.processVisibilityOutputs);
      }
    }
  }

  private void logReentrantMountsExceedMaxAttempts() {
    final String message =
        "Reentrant mounts exceed max attempts"
            + ", view="
            + (mLithoView != null ? LithoViewTestHelper.toDebugString(mLithoView) : null)
            + ", component="
            + (mRoot != null ? mRoot : getSimpleName());
    ComponentsReporter.emitMessage(
        ComponentsReporter.LogLevel.FATAL, REENTRANT_MOUNTS_EXCEED_MAX_ATTEMPTS, message);
  }

  void applyPreviousRenderData(LayoutState layoutState) {
    final List<Component> components = layoutState.getComponentsNeedingPreviousRenderData();
    if (components == null || components.isEmpty()) {
      return;
    }

    if (mPreviousRenderState == null) {
      return;
    }

    mPreviousRenderState.applyPreviousRenderData(components);
  }

  private void recordRenderData(LayoutState layoutState) {
    final List<Component> components = layoutState.getComponentsNeedingPreviousRenderData();
    if (components == null || components.isEmpty()) {
      return;
    }

    if (mPreviousRenderState == null) {
      mPreviousRenderState = new RenderState();
    }

    mPreviousRenderState.recordRenderData(components);
  }

  void detach() {
    assertMainThread();

    if (mIncrementalMountHelper != null) {
      mIncrementalMountHelper.onDetach(mLithoView);
    }

    synchronized (this) {
      mIsAttached = false;
    }
  }

  /**
   * Set a new LithoView to this ComponentTree checking that they have the same context and clear
   * the ComponentTree reference from the previous LithoView if any. Be sure this ComponentTree is
   * detach first.
   */
  void setLithoView(@NonNull LithoView view) {
    assertMainThread();

    if (mLithoView == view) {
      return;
    }

    if (mLithoView != null) {
      mLithoView.setComponentTree(null);
    } else if (mIsAttached) {
      // It's possible that the view associated with this ComponentTree was recycled but was
      // never detached. In all cases we have to make sure that the old references between
      // lithoView and componentTree are reset.
      detach();
    }

    // TODO t58734935 revert this.
    if (mContext.getAndroidContext() != mContext.getApplicationContext()
        && !hasSameRootContext(view.getContext(), mContext.getAndroidContext())) {
      // This would indicate bad things happening, like leaking a context.
      throw new IllegalArgumentException(
          "Base view context differs, view context is: "
              + view.getContext()
              + ", ComponentTree context is: "
              + mContext.getAndroidContext());
    }

    mLithoView = view;
  }

  void clearLithoView() {
    assertMainThread();

    // Crash if the ComponentTree is mounted to a view.
    if (mIsAttached) {
      throw new IllegalStateException("Clearing the LithoView while the ComponentTree is attached");
    }

    mLithoView = null;
  }

  void forceMainThreadLayout() {
    assertMainThread();

    LithoView lithoView = mLithoView;
    if (lithoView != null) {
      // If we are attached to a LithoView, then force a relayout immediately. Otherwise, we'll
      // relayout next time we are measured.
      lithoView.forceRelayout();
    } else {
      mForceLayout = true;
    }
  }

  @GuardedBy("this")
  private boolean isPendingLayoutCompatible() {
    synchronized (mCurrentCalculateLayoutRunnableLock) {
      if (mCurrentCalculateLayoutRunnable != null) {
        // if we have a pending runnable, then it will capture the correct root and size specs when
        // it runs, so it is inherently compatible.
        return true;
      }
    }

    if (mPendingLayoutWidthSpec == SIZE_UNINITIALIZED
        || mPendingLayoutHeightSpec == SIZE_UNINITIALIZED) {
      // In this case, there's no pending layout at all
      return false;
    }

    // Otherwise, we need to check for whether the pending (async) layout that is using the correct
    // size specs
    return MeasureComparisonUtils.areMeasureSpecsEquivalent(mWidthSpec, mPendingLayoutWidthSpec)
        && MeasureComparisonUtils.areMeasureSpecsEquivalent(mHeightSpec, mPendingLayoutHeightSpec);
  }

  void measure(int widthSpec, int heightSpec, int[] measureOutput, boolean forceLayout) {
    assertMainThread();

    Component component = null;
    TreeProps treeProps = null;
    int layoutVersion = -1;

    synchronized (this) {
      mIsMeasuring = true;

      // This widthSpec/heightSpec is fixed until the view gets detached.
      mWidthSpec = widthSpec;
      mHeightSpec = heightSpec;

      setBestMainThreadLayoutAndReturnOldLayout();

      // We don't check if mRoot is compatible here since if it doesn't match mMainThreadLayout,
      // that means we're computing an async layout with a new root which can just be applied when
      // it finishes, assuming it has compatible width/height specs
      final boolean shouldCalculateNewLayout =
          !isCompatibleSpec(mMainThreadLayoutState, mWidthSpec, mHeightSpec)
              || (!mMainThreadLayoutState.isForComponentId(mRoot.getId())
                  && !isPendingLayoutCompatible());

      if (mForceLayout || forceLayout || shouldCalculateNewLayout) {
        // Neither layout was compatible and we have to perform a layout.
        // Since outputs get set on the same object during the lifecycle calls,
        // we need to copy it in order to use it concurrently.
        component = mRoot.makeShallowCopy();
        layoutVersion = ++mLayoutVersion;
        treeProps = TreeProps.copy(mRootTreeProps);
        mForceLayout = false;
      }
    }

    if (component != null) {
      // TODO: We should re-use the existing CSSNodeDEPRECATED tree instead of re-creating it.
      if (mMainThreadLayoutState != null) {
        // It's beneficial to delete the old layout state before we start creating a new one since
        // we'll be able to re-use some of the layout nodes.
        synchronized (this) {
          mMainThreadLayoutState = null;
        }
      }

      // We have no layout that matches the given spec, so we need to compute it on the main thread.
      LayoutState localLayoutState =
          calculateLayoutState(
              mContext,
              component,
              widthSpec,
              heightSpec,
              layoutVersion,
              mIsLayoutDiffingEnabled,
              treeProps,
              CalculateLayoutSource.MEASURE,
              null);
      if (localLayoutState == null) {
        throw new IllegalStateException(
            "LayoutState cannot be null for measure, this means a LayoutStateFuture was released incorrectly.");
      }

      final List<Component> components;
      @Nullable final Map<String, Component> attachables;
      synchronized (this) {
        final StateHandler layoutStateStateHandler = localLayoutState.consumeStateHandler();

        attachables = localLayoutState.consumeAttachables();
        if (layoutStateStateHandler != null) {
          mStateHandler.commit(layoutStateStateHandler);
          mInitialStateContainer.unregisterStateHandler(layoutStateStateHandler);
        }

        components = localLayoutState.consumeComponents();
        mMainThreadLayoutState = localLayoutState;
        mLatestLayoutState = localLayoutState;
      }

      if (attachables != null) {
        getOrCreateAttachDetachHandler().onAttached(attachables);
      }

      if (components != null) {
        bindEventAndTriggerHandlers(components);
      }

      // We need to force remount on layout
      mLithoView.setMountStateDirty();

      dispatchNewLayoutStateReady();
    }

    measureOutput[0] = mMainThreadLayoutState.getWidth();
    measureOutput[1] = mMainThreadLayoutState.getHeight();

    int layoutScheduleType = SCHEDULE_NONE;
    Component root = null;
    TreeProps rootTreeProps = null;

    synchronized (this) {
      mIsMeasuring = false;

      if (mScheduleLayoutAfterMeasure != SCHEDULE_NONE) {
        layoutScheduleType = mScheduleLayoutAfterMeasure;
        mScheduleLayoutAfterMeasure = SCHEDULE_NONE;
        root = mRoot.makeShallowCopy();
        rootTreeProps = TreeProps.copy(mRootTreeProps);
      }
    }

    if (layoutScheduleType != SCHEDULE_NONE) {
      setRootAndSizeSpecInternal(
          root,
          SIZE_UNINITIALIZED,
          SIZE_UNINITIALIZED,
          layoutScheduleType == SCHEDULE_LAYOUT_ASYNC,
          null /*output */,
          CalculateLayoutSource.MEASURE,
          -1,
          null,
          rootTreeProps);
    }
  }

  /** Returns {@code true} if the layout call mounted the component. */
  boolean layout() {
    assertMainThread();

    return mountComponentIfNeeded();
  }

  /** Returns whether incremental mount is enabled or not in this component. */
  public boolean isIncrementalMountEnabled() {
    return mIncrementalMountEnabled;
  }

  boolean isVisibilityProcessingEnabled() {
    return mVisibilityProcessingEnabled;
  }

  /** Returns the recycling mode. Please see {@link RecyclingMode for details of different modes} */
  public @RecyclingMode int getRecyclingMode() {
    return mRecyclingMode;
  }

  public boolean isReconciliationEnabled() {
    return isReconciliationEnabled;
  }

  synchronized Component getRoot() {
    return mRoot;
  }

  /**
   * Update the root component. This can happen in both attached and detached states. In each case
   * we will run a layout and then proxy a message to the main thread to cause a
   * relayout/invalidate.
   */
  public void setRoot(Component root) {
    if (root == null) {
      throw new IllegalArgumentException("Root component can't be null");
    }

    setRootAndSizeSpecAndWrapper(
        root,
        SIZE_UNINITIALIZED,
        SIZE_UNINITIALIZED,
        false /* isAsync */,
        null /* output */,
        CalculateLayoutSource.SET_ROOT_SYNC,
        -1,
        null,
        null);
  }

  /**
   * Pre-allocate the mount content of all MountSpec in this tree. Must be called after layout is
   * created.
   */
  @ThreadSafe(enableChecks = false)
  private void preAllocateMountContent(boolean shouldPreallocatePerMountSpec) {
    final LayoutState toPrePopulate;

    synchronized (this) {
      if (mMainThreadLayoutState != null) {
        toPrePopulate = mMainThreadLayoutState;
      } else if (mBackgroundLayoutState != null) {
        toPrePopulate = mBackgroundLayoutState;
      } else {
        return;
      }
    }
    final ComponentsLogger logger = getContextLogger();
    final PerfEvent event =
        logger != null
            ? LogTreePopulator.populatePerfEventFromLogger(
                mContext,
                logger,
                logger.newPerformanceEvent(mContext, EVENT_PRE_ALLOCATE_MOUNT_CONTENT))
            : null;

    toPrePopulate.preAllocateMountContent(shouldPreallocatePerMountSpec, mRecyclingMode);

    if (event != null) {
      logger.logPerfEvent(event);
    }
  }

  public void setRootAsync(Component root) {
    if (root == null) {
      throw new IllegalArgumentException("Root component can't be null");
    }

    setRootAndSizeSpecAndWrapper(
        root,
        SIZE_UNINITIALIZED,
        SIZE_UNINITIALIZED,
        true /* isAsync */,
        null /* output */,
        CalculateLayoutSource.SET_ROOT_ASYNC,
        -1,
        null,
        null);
  }

  void updateStateLazy(String componentKey, StateUpdate stateUpdate) {
    synchronized (this) {
      if (mRoot == null) {
        return;
      }

      mStateHandler.queueStateUpdate(componentKey, stateUpdate, true);
    }
  }

  void applyLazyStateUpdatesForContainer(String componentKey, StateContainer container) {
    StateHandler stateHandler;
    synchronized (this) {
      if (mRoot == null) {
        return;
      }

      stateHandler = StateHandler.createShallowCopyForLazyStateUpdates(mStateHandler);
    }

    stateHandler.applyLazyStateUpdatesForContainer(componentKey, container);
  }

  void updateStateSync(
      String componentKey,
      StateUpdate stateUpdate,
      String attribution,
      boolean isCreateLayoutInProgress) {

    if (mForceAsyncStateUpdate && mIsAsyncUpdateStateEnabled) {
      updateStateAsync(componentKey, stateUpdate, attribution, isCreateLayoutInProgress);
      return;
    }

    synchronized (this) {
      if (mRoot == null) {
        return;
      }

      mStateHandler.queueStateUpdate(componentKey, stateUpdate, false);
    }

    LithoStats.incrementComponentStateUpdateSyncCount();
    final Looper looper = Looper.myLooper();

    if (looper == null) {
      Log.w(
          TAG,
          "You cannot update state synchronously from a thread without a looper, "
              + "using the default background layout thread instead");
      synchronized (mUpdateStateSyncRunnableLock) {
        if (mUpdateStateSyncRunnable != null) {
          mLayoutThreadHandler.remove(mUpdateStateSyncRunnable);
        }
        mUpdateStateSyncRunnable =
            new UpdateStateSyncRunnable(attribution, isCreateLayoutInProgress);

        String tag = EMPTY_STRING;
        if (mLayoutThreadHandler.isTracing()) {
          tag = "updateStateSyncNoLooper " + attribution;
        }
        mLayoutThreadHandler.post(mUpdateStateSyncRunnable, tag);
      }
      return;
    }

    final WeakReference<LithoHandler> handlerWr = sSyncStateUpdatesHandler.get();
    LithoHandler handler = handlerWr != null ? handlerWr.get() : null;

    if (handler == null) {
      handler = new DefaultLithoHandler(looper);
      sSyncStateUpdatesHandler.set(new WeakReference<>(handler));
    }

    synchronized (mUpdateStateSyncRunnableLock) {
      if (mUpdateStateSyncRunnable != null) {
        handler.remove(mUpdateStateSyncRunnable);
      }
      mUpdateStateSyncRunnable = new UpdateStateSyncRunnable(attribution, isCreateLayoutInProgress);

      String tag = EMPTY_STRING;
      if (handler.isTracing()) {
        tag = "updateStateSync " + attribution;
      }
      handler.post(mUpdateStateSyncRunnable, tag);
    }
  }

  void updateStateAsync(
      String componentKey,
      StateUpdate stateUpdate,
      String attribution,
      boolean isCreateLayoutInProgress) {
    if (!mIsAsyncUpdateStateEnabled) {
      throw new RuntimeException(
          "Triggering async state updates on this component tree is "
              + "disabled, use sync state updates.");
    }

    synchronized (this) {
      if (mRoot == null) {
        return;
      }

      mStateHandler.queueStateUpdate(componentKey, stateUpdate, false);
    }

    LithoStats.incrementComponentStateUpdateAsyncCount();
    updateStateInternal(true, attribution, isCreateLayoutInProgress);
  }

  void updateHookStateAsync(
      HookUpdater updater, String attribution, boolean isCreateLayoutInProgress) {
    synchronized (this) {
      if (mRoot == null) {
        return;
      }

      mStateHandler.queueHookStateUpdate(updater);
    }

    LithoStats.incrementComponentStateUpdateAsyncCount();
    updateStateInternal(true, attribution, isCreateLayoutInProgress);
  }

  void updateStateInternal(boolean isAsync, String attribution, boolean isCreateLayoutInProgress) {
    if (ComponentsConfiguration.ignoreStateUpdatesForScreenshotTest) {
      return;
    }

    final Component root;
    final @Nullable TreeProps rootTreeProps;

    synchronized (this) {
      if (mRoot == null) {
        return;
      }

      if (mIsMeasuring) {
        // If the layout calculation was already scheduled to happen synchronously let's just go
        // with a sync layout calculation.
        if (mScheduleLayoutAfterMeasure == SCHEDULE_LAYOUT_SYNC) {
          return;
        }

        mScheduleLayoutAfterMeasure = isAsync ? SCHEDULE_LAYOUT_ASYNC : SCHEDULE_LAYOUT_SYNC;
        return;
      }

      root = mRoot.makeShallowCopy();
      rootTreeProps = TreeProps.copy(mRootTreeProps);

      if (isCreateLayoutInProgress) {
        logStateUpdatesFromCreateLayout();
      }
    }

    setRootAndSizeSpecInternal(
        root,
        SIZE_UNINITIALIZED,
        SIZE_UNINITIALIZED,
        isAsync,
        null /*output */,
        isAsync
            ? CalculateLayoutSource.UPDATE_STATE_ASYNC
            : CalculateLayoutSource.UPDATE_STATE_SYNC,
        -1,
        attribution,
        rootTreeProps,
        isCreateLayoutInProgress);
  }

  /**
   * State updates can be triggered when layout creation is still in progress which causes an
   * infinite loop because state updates again create the layout. To prevent this we keep a track of
   * how many times consequently state updates was invoked from within layout creation. If this
   * crosses the threshold a runtime exception is thrown.
   */
  @GuardedBy("this")
  private void logStateUpdatesFromCreateLayout() {
    if (++mStateUpdatesFromCreateLayoutCount == STATE_UPDATES_IN_LOOP_THRESHOLD) {
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.FATAL,
          STATE_UPDATES_IN_LOOP_EXCEED_THRESHOLD,
          "State Updates when create layout in progress exceeds threshold");
    }
  }

  StateHandler getStateHandler() {
    return mStateHandler;
  }

  void recordEventHandler(Component component, EventHandler eventHandler) {
    mEventHandlersController.recordEventHandler(component.getGlobalKey(), eventHandler);
  }

  private void bindTriggerHandler(Component component) {
    synchronized (mEventTriggersContainer) {
      component.recordEventTrigger(mEventTriggersContainer);
    }
  }

  private void clearUnusedTriggerHandlers() {
    mEventTriggersContainer.clear();
  }

  @Nullable
  EventTrigger getEventTrigger(String triggerKey) {
    synchronized (mEventTriggersContainer) {
      return mEventTriggersContainer.getEventTrigger(triggerKey);
    }
  }

  @Nullable
  EventTrigger getEventTrigger(Handle handle, int methodId) {
    synchronized (mEventTriggersContainer) {
      return mEventTriggersContainer.getEventTrigger(handle, methodId);
    }
  }

  /**
   * Check if the any child components stored in {@link LayoutState} have entered/exited the working
   * range, and dispatch the event to trigger the corresponding registered methods.
   */
  public synchronized void checkWorkingRangeAndDispatch(
      int position,
      int firstVisibleIndex,
      int lastVisibleIndex,
      int firstFullyVisibleIndex,
      int lastFullyVisibleIndex) {

    LayoutState layoutState =
        isBestMainThreadLayout() ? mMainThreadLayoutState : mBackgroundLayoutState;

    if (layoutState != null) {
      layoutState.checkWorkingRangeAndDispatch(
          position,
          firstVisibleIndex,
          lastVisibleIndex,
          firstFullyVisibleIndex,
          lastFullyVisibleIndex,
          mWorkingRangeStatusHandler);
    }
  }

  /**
   * Dispatch OnExitedRange event to component which is still in the range, then clear the handler.
   */
  private synchronized void clearWorkingRangeStatusHandler() {
    final LayoutState layoutState =
        isBestMainThreadLayout() ? mMainThreadLayoutState : mBackgroundLayoutState;

    if (layoutState != null) {
      layoutState.dispatchOnExitRangeIfNeeded(mWorkingRangeStatusHandler);
    }

    mWorkingRangeStatusHandler.clear();
  }

  /**
   * Update the width/height spec. This is useful if you are currently detached and are responding
   * to a configuration change. If you are currently attached then the HostView is the source of
   * truth for width/height, so this call will be ignored.
   */
  public void setSizeSpec(int widthSpec, int heightSpec) {
    setSizeSpec(widthSpec, heightSpec, null);
  }

  /**
   * Same as {@link #setSizeSpec(int, int)} but fetches the resulting width/height in the given
   * {@link Size}.
   */
  public void setSizeSpec(int widthSpec, int heightSpec, Size output) {
    setRootAndSizeSpecInternal(
        null,
        widthSpec,
        heightSpec,
        false /* isAsync */,
        output /* output */,
        CalculateLayoutSource.SET_SIZE_SPEC_SYNC,
        -1,
        null,
        null);
  }

  public void setSizeSpecAsync(int widthSpec, int heightSpec) {
    setRootAndSizeSpecInternal(
        null,
        widthSpec,
        heightSpec,
        true /* isAsync */,
        null /* output */,
        CalculateLayoutSource.SET_SIZE_SPEC_ASYNC,
        -1,
        null,
        null);
  }

  /** Compute asynchronously a new layout with the given component root and sizes */
  public void setRootAndSizeSpecAsync(Component root, int widthSpec, int heightSpec) {
    if (root == null) {
      throw new IllegalArgumentException("Root component can't be null");
    }

    setRootAndSizeSpecAndWrapper(
        root,
        widthSpec,
        heightSpec,
        true /* isAsync */,
        null /* output */,
        CalculateLayoutSource.SET_ROOT_ASYNC,
        -1,
        null,
        null);
  }

  /**
   * Compute asynchronously a new layout with the given component root, sizes and stored TreeProps.
   */
  public void setRootAndSizeSpecAsync(
      Component root, int widthSpec, int heightSpec, @Nullable TreeProps treeProps) {
    if (root == null) {
      throw new IllegalArgumentException("Root component can't be null");
    }

    setRootAndSizeSpecAndWrapper(
        root,
        widthSpec,
        heightSpec,
        true /* isAsync */,
        null /* output */,
        CalculateLayoutSource.SET_ROOT_ASYNC,
        -1,
        null,
        treeProps);
  }

  /** Compute a new layout with the given component root and sizes */
  public void setRootAndSizeSpec(Component root, int widthSpec, int heightSpec) {
    if (root == null) {
      throw new IllegalArgumentException("Root component can't be null");
    }

    setRootAndSizeSpecAndWrapper(
        root,
        widthSpec,
        heightSpec,
        false /* isAsync */,
        null /* output */,
        CalculateLayoutSource.SET_ROOT_SYNC,
        -1,
        null,
        null);
  }

  public void setRootAndSizeSpec(Component root, int widthSpec, int heightSpec, Size output) {
    if (root == null) {
      throw new IllegalArgumentException("Root component can't be null");
    }

    setRootAndSizeSpecAndWrapper(
        root,
        widthSpec,
        heightSpec,
        false /* isAsync */,
        output,
        CalculateLayoutSource.SET_ROOT_SYNC,
        -1,
        null,
        null);
  }

  public void setRootAndSizeSpec(
      Component root, int widthSpec, int heightSpec, Size output, @Nullable TreeProps treeProps) {
    if (root == null) {
      throw new IllegalArgumentException("Root component can't be null");
    }

    setRootAndSizeSpecAndWrapper(
        root,
        widthSpec,
        heightSpec,
        false /* isAsync */,
        output,
        CalculateLayoutSource.SET_ROOT_SYNC,
        -1,
        null,
        treeProps);
  }

  public void setVersionedRootAndSizeSpec(
      Component root,
      int widthSpec,
      int heightSpec,
      Size output,
      @Nullable TreeProps treeProps,
      int externalRootVersion) {
    if (root == null) {
      throw new IllegalArgumentException("Root component can't be null");
    }

    setRootAndSizeSpecAndWrapper(
        root,
        widthSpec,
        heightSpec,
        false /* isAsync */,
        output,
        CalculateLayoutSource.SET_ROOT_SYNC,
        externalRootVersion,
        null,
        treeProps);
  }

  /** @return the {@link LithoView} associated with this ComponentTree if any. */
  @Keep
  @Nullable
  public LithoView getLithoView() {
    assertMainThread();
    return mLithoView;
  }

  boolean hasIncrementalVisibility() {
    return mIncrementalVisibility;
  }

  /**
   * Provides a new instance from the StateHandler pool that is initialized with the information
   * from the StateHandler currently held by the ComponentTree. Once the state updates have been
   * applied and we are back in the main thread the state handler gets released to the pool.
   *
   * @return a copy of the state handler instance held by ComponentTree.
   */
  public synchronized StateHandler acquireStateHandler() {
    return StateHandler.createNewInstance(mStateHandler);
  }

  synchronized @Nullable void consumeStateUpdateTransitions(
      List<Transition> outList, @Nullable String logContext) {
    if (mStateHandler != null) {
      mStateHandler.consumePendingStateUpdateTransitions(outList, logContext);
    }
  }

  /**
   * Takes ownership of the {@link RenderState} object from this ComponentTree - this allows the
   * RenderState to be persisted somewhere and then set back on another ComponentTree using the
   * {@link Builder}. See {@link RenderState} for more information on the purpose of this object.
   */
  @ThreadConfined(ThreadConfined.UI)
  public RenderState consumePreviousRenderState() {
    final RenderState previousRenderState = mPreviousRenderState;

    mPreviousRenderState = null;
    mPreviousRenderStateSetFromBuilder = false;
    return previousRenderState;
  }

  /**
   * @deprecated
   * @see #showTooltip(LithoTooltip, String, int, int)
   */
  @Deprecated
  void showTooltip(
      DeprecatedLithoTooltip tooltip,
      String anchorGlobalKey,
      TooltipPosition tooltipPosition,
      int xOffset,
      int yOffset) {
    assertMainThread();

    final Map<String, Rect> componentKeysToBounds;
    synchronized (this) {
      componentKeysToBounds = mMainThreadLayoutState.getComponentKeyToBounds();
    }

    if (!componentKeysToBounds.containsKey(anchorGlobalKey)) {
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.ERROR,
          INVALID_KEY,
          "Cannot find a component with key " + anchorGlobalKey + " to use as anchor.");
      return;
    }

    final Rect anchorBounds = componentKeysToBounds.get(anchorGlobalKey);
    LithoTooltipController.showOnAnchor(
        tooltip, anchorBounds, mLithoView, tooltipPosition, xOffset, yOffset);
  }

  void showTooltipOnHandle(
      ComponentContext componentContext,
      LithoTooltip lithoTooltip,
      Handle handle,
      int xOffset,
      int yOffset) {
    assertMainThread();

    final Map<Handle, Rect> componentHandleToBounds;
    synchronized (this) {
      componentHandleToBounds = mMainThreadLayoutState.getComponentHandleToBounds();
    }

    final Rect anchorBounds = componentHandleToBounds.get(handle);

    if (handle == null || anchorBounds == null) {
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.ERROR,
          INVALID_HANDLE,
          "Cannot find a component with handle "
              + handle
              + " to use as anchor.\nComponent: "
              + componentContext.getComponentScope().getSimpleName());
      return;
    }

    lithoTooltip.showLithoTooltip(mLithoView, anchorBounds, xOffset, yOffset);
  }

  void showTooltip(LithoTooltip lithoTooltip, String anchorGlobalKey, int xOffset, int yOffset) {
    assertMainThread();

    final Map<String, Rect> componentKeysToBounds;
    synchronized (this) {
      componentKeysToBounds = mMainThreadLayoutState.getComponentKeyToBounds();
    }

    if (!componentKeysToBounds.containsKey(anchorGlobalKey)) {
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.ERROR,
          INVALID_KEY,
          "Cannot find a component with key " + anchorGlobalKey + " to use as anchor.");
      return;
    }

    final Rect anchorBounds = componentKeysToBounds.get(anchorGlobalKey);
    lithoTooltip.showLithoTooltip(mLithoView, anchorBounds, xOffset, yOffset);
  }

  /**
   * @return the InitialStateContainer associated with this tree. This is basically a look-aside
   *     table for initial states so that we can guarantee that onCreateInitialState doesn't get
   *     called multiple times for the same Component.
   */
  InitialStateContainer getInitialStateContainer() {
    return mInitialStateContainer;
  }

  /**
   * This internal version of {@link #setRootAndSizeSpecInternal(Component, int, int, boolean, Size,
   * int, String, TreeProps)} wraps the provided root in a wrapper component first. Ensure to only
   * call this for entry calls to setRoot, i.e. non-recurring calls as you will otherwise continue
   * rewrapping the component.
   */
  private void setRootAndSizeSpecAndWrapper(
      Component root,
      int widthSpec,
      int heightSpec,
      boolean isAsync,
      Size output,
      @CalculateLayoutSource int source,
      int externalRootVersion,
      String extraAttribution,
      @Nullable TreeProps treeProps) {

    setRootAndSizeSpecInternal(
        wrapRootInErrorBoundary(root),
        widthSpec,
        heightSpec,
        isAsync,
        output,
        source,
        externalRootVersion,
        extraAttribution,
        treeProps);
  }

  private Component wrapRootInErrorBoundary(Component originalRoot) {
    // If a rootWrapperComponentFactory is provided, we use it to create a new root
    // component.
    final RootWrapperComponentFactory rootWrapperComponentFactory =
        ErrorBoundariesConfiguration.rootWrapperComponentFactory;
    return rootWrapperComponentFactory == null
        ? originalRoot
        : rootWrapperComponentFactory.createWrapper(mContext, originalRoot);
  }

  private void setRootAndSizeSpecInternal(
      Component root,
      int widthSpec,
      int heightSpec,
      boolean isAsync,
      @Nullable Size output,
      @CalculateLayoutSource int source,
      int externalRootVersion,
      String extraAttribution,
      @Nullable TreeProps treeProps) {
    setRootAndSizeSpecInternal(
        root,
        widthSpec,
        heightSpec,
        isAsync,
        output,
        source,
        externalRootVersion,
        extraAttribution,
        treeProps,
        false);
  }

  private void setRootAndSizeSpecInternal(
      Component root,
      int widthSpec,
      int heightSpec,
      boolean isAsync,
      @Nullable Size output,
      @CalculateLayoutSource int source,
      int externalRootVersion,
      String extraAttribution,
      @Nullable TreeProps treeProps,
      boolean isCreateLayoutInProgress) {
    synchronized (this) {
      if (mReleased) {
        // If this is coming from a background thread, we may have been released from the main
        // thread. In that case, do nothing.
        //
        // NB: This is only safe because we don't re-use released ComponentTrees.
        return;
      }

      // If this is coming from a setRoot
      if (source == CalculateLayoutSource.SET_ROOT_SYNC
          || source == CalculateLayoutSource.SET_ROOT_ASYNC) {
        if (mExternalRootVersion >= 0 && externalRootVersion < 0) {
          throw new IllegalStateException(
              "Setting an unversioned root after calling setVersionedRootAndSizeSpec is not "
                  + "supported. If this ComponentTree takes its version from a parent tree make "
                  + "sure to always call setVersionedRootAndSizeSpec");
        }

        if (mExternalRootVersion > externalRootVersion) {
          // Since this layout is not really valid we don't need to set a Size.
          return;
        }

        mExternalRootVersion = externalRootVersion;
      }

      if (mStateHandler.hasPendingUpdates() && root != null) {
        root = root.makeShallowCopyWithNewId();
      }

      final boolean rootInitialized = root != null;
      final boolean treePropsInitialized = treeProps != null;
      final boolean widthSpecInitialized = widthSpec != SIZE_UNINITIALIZED;
      final boolean heightSpecInitialized = heightSpec != SIZE_UNINITIALIZED;

      final boolean widthSpecDidntChange = !widthSpecInitialized || widthSpec == mWidthSpec;
      final boolean heightSpecDidntChange = !heightSpecInitialized || heightSpec == mHeightSpec;
      final boolean sizeSpecDidntChange = widthSpecDidntChange && heightSpecDidntChange;
      final LayoutState mostRecentLayoutState =
          mBackgroundLayoutState != null ? mBackgroundLayoutState : mMainThreadLayoutState;
      final boolean allSpecsWereInitialized =
          widthSpecInitialized
              && heightSpecInitialized
              && mWidthSpec != SIZE_UNINITIALIZED
              && mHeightSpec != SIZE_UNINITIALIZED;
      final boolean sizeSpecsAreCompatible =
          sizeSpecDidntChange
              || (allSpecsWereInitialized
                  && mostRecentLayoutState != null
                  && Layout.hasCompatibleSizeSpec(
                      mWidthSpec,
                      mHeightSpec,
                      widthSpec,
                      heightSpec,
                      mostRecentLayoutState.getWidth(),
                      mostRecentLayoutState.getHeight()));
      final boolean rootDidntChange =
          !rootInitialized
              || (mostRecentLayoutState != null
                  && root.getId() == mostRecentLayoutState.getRootComponent().getId());

      if (rootDidntChange && sizeSpecsAreCompatible) {
        // The spec and the root haven't changed. Either we have a layout already, or we're
        // currently computing one on another thread.
        if (output == null) {
          return;
        }

        // Set the output if we have a LayoutState, otherwise we need to compute one synchronously
        // below to get the correct output.
        if (mostRecentLayoutState != null) {
          output.height = mostRecentLayoutState.getHeight();
          output.width = mostRecentLayoutState.getWidth();
          return;
        }
      }

      if (widthSpecInitialized) {
        mWidthSpec = widthSpec;
      }

      if (heightSpecInitialized) {
        mHeightSpec = heightSpec;
      }

      if (rootInitialized) {
        mRoot = root;
      }

      if (treePropsInitialized) {
        mRootTreeProps = treeProps;
      }
    }

    if (isAsync && output != null) {
      throw new IllegalArgumentException(
          "The layout can't be calculated asynchronously if we need the Size back");
    }

    if (isAsync) {
      synchronized (mCurrentCalculateLayoutRunnableLock) {
        if (mCurrentCalculateLayoutRunnable != null) {
          mLayoutThreadHandler.remove(mCurrentCalculateLayoutRunnable);
        }
        mCurrentCalculateLayoutRunnable =
            new CalculateLayoutRunnable(
                source, treeProps, extraAttribution, isCreateLayoutInProgress);

        String tag = EMPTY_STRING;
        if (mLayoutThreadHandler.isTracing()) {
          tag = "calculateLayout ";
          if (root != null) {
            tag = tag + root.getSimpleName();
          }
        }
        mLayoutThreadHandler.post(mCurrentCalculateLayoutRunnable, tag);
      }
    } else {
      calculateLayout(output, source, extraAttribution, treeProps, isCreateLayoutInProgress);
    }
  }

  /**
   * Calculates the layout.
   *
   * @param output a destination where the size information should be saved
   * @param treeProps Saved TreeProps to be used as parent input
   * @param isCreateLayoutInProgress This indicates state update has been invoked from within layout
   *     create.
   */
  private void calculateLayout(
      @Nullable Size output,
      @CalculateLayoutSource int source,
      @Nullable String extraAttribution,
      @Nullable TreeProps treeProps,
      boolean isCreateLayoutInProgress) {
    final int widthSpec;
    final int heightSpec;
    final Component root;
    final int layoutVersion;

    // Cancel any scheduled layout requests we might have in the background queue
    // since we are starting a new layout computation.
    synchronized (mCurrentCalculateLayoutRunnableLock) {
      if (mCurrentCalculateLayoutRunnable != null) {
        mLayoutThreadHandler.remove(mCurrentCalculateLayoutRunnable);
        mCurrentCalculateLayoutRunnable = null;
      }
    }

    synchronized (this) {
      // Can't compute a layout if specs or root are missing
      if (!hasSizeSpec() || mRoot == null) {
        return;
      }

      // Check if we already have a compatible layout.
      if (hasCompatibleComponentAndSpec()) {
        if (output != null) {
          final LayoutState mostRecentLayoutState =
              mBackgroundLayoutState != null ? mBackgroundLayoutState : mMainThreadLayoutState;
          output.width = mostRecentLayoutState.getWidth();
          output.height = mostRecentLayoutState.getHeight();
        }
        return;
      }

      widthSpec = mWidthSpec;
      heightSpec = mHeightSpec;
      mPendingLayoutWidthSpec = widthSpec;
      mPendingLayoutHeightSpec = heightSpec;
      root = mRoot.makeShallowCopy();
      layoutVersion = ++mLayoutVersion;
    }

    final LayoutState localLayoutState =
        calculateLayoutState(
            mContext,
            root,
            widthSpec,
            heightSpec,
            layoutVersion,
            mIsLayoutDiffingEnabled,
            treeProps,
            source,
            extraAttribution);

    if (localLayoutState == null) {
      if (!mReleased && output != null) {
        throw new IllegalStateException(
            "LayoutState is null, but only async operations can return a null LayoutState");
      }

      return;
    }

    if (output != null) {
      output.width = localLayoutState.getWidth();
      output.height = localLayoutState.getHeight();
    }

    List<Component> components = null;
    @Nullable Map<String, Component> attachables = null;

    final boolean noCompatibleComponent;
    int rootWidth = 0;
    int rootHeight = 0;
    boolean layoutStateUpdated = false;
    synchronized (this) {
      mPendingLayoutWidthSpec = SIZE_UNINITIALIZED;
      mPendingLayoutHeightSpec = SIZE_UNINITIALIZED;

      // Make sure some other thread hasn't computed a compatible layout in the meantime.
      noCompatibleComponent =
          !hasCompatibleComponentAndSpec()
              && isCompatibleSpec(localLayoutState, mWidthSpec, mHeightSpec);
      final StateHandler layoutStateStateHandler = localLayoutState.consumeStateHandler();
      if (noCompatibleComponent) {
        if (layoutStateStateHandler != null) {
          if (mStateHandler != null) { // we could have been released
            mStateHandler.commit(layoutStateStateHandler);
          }
        }

        if (mMeasureListeners != null) {
          rootWidth = localLayoutState.getWidth();
          rootHeight = localLayoutState.getHeight();
        }

        components = localLayoutState.consumeComponents();
        attachables = localLayoutState.consumeAttachables();

        // Set the new layout state.
        mBackgroundLayoutState = localLayoutState;
        mLatestLayoutState = localLayoutState;
        layoutStateUpdated = true;
      }

      if (layoutStateStateHandler != null) {
        mInitialStateContainer.unregisterStateHandler(layoutStateStateHandler);
      }
      // Resetting the count after layout calculation is complete and it was triggered from within
      // layout creation
      if (!isCreateLayoutInProgress) {
        mStateUpdatesFromCreateLayoutCount = 0;
      }
    }

    if (noCompatibleComponent) {
      final List<MeasureListener> measureListeners;
      synchronized (this) {
        measureListeners = mMeasureListeners == null ? null : new ArrayList<>(mMeasureListeners);
      }

      if (measureListeners != null) {
        for (MeasureListener measureListener : measureListeners) {
          measureListener.onSetRootAndSizeSpec(
              layoutVersion,
              rootWidth,
              rootHeight,
              source == CalculateLayoutSource.UPDATE_STATE_ASYNC
                  || source == CalculateLayoutSource.UPDATE_STATE_SYNC);
        }
      }

      if (mAttachDetachHandler != null) {
        mAttachDetachHandler.onAttached(attachables);
      } else if (attachables != null) {
        getOrCreateAttachDetachHandler().onAttached(attachables);
      }
    }

    if (components != null) {
      bindEventAndTriggerHandlers(components);
    }

    if (layoutStateUpdated) {
      postBackgroundLayoutStateUpdated();
    }

    if (mPreAllocateMountContentHandler != null) {
      mPreAllocateMountContentHandler.remove(mPreAllocateMountContentRunnable);

      String tag = EMPTY_STRING;
      if (mPreAllocateMountContentHandler.isTracing()) {
        tag = "preallocateLayout ";
        if (root != null) {
          tag = tag + root.getSimpleName();
        }
      }
      mPreAllocateMountContentHandler.post(mPreAllocateMountContentRunnable, tag);
    }
  }

  private void bindEventAndTriggerHandlers(List<Component> components) {
    clearUnusedTriggerHandlers();

    for (final Component component : components) {
      mEventHandlersController.bindEventHandlers(
          component.getScopedContext(), component, component.getGlobalKey());
      bindTriggerHandler(component);
    }

    mEventHandlersController.clearUnusedEventHandlers();
  }

  /**
   * Transfer mBackgroundLayoutState to mMainThreadLayoutState. This will proxy to the main thread
   * if necessary. If the component/size-spec changes in the meantime, then the transfer will be
   * aborted.
   */
  private void postBackgroundLayoutStateUpdated() {
    if (isMainThread()) {
      // We need to possibly update mMainThreadLayoutState. This call will
      // cause the host view to be invalidated and re-laid out, if necessary.
      backgroundLayoutStateUpdated();
    } else {
      // If we aren't on the main thread, we send a message to the main thread
      // to invoke backgroundLayoutStateUpdated.
      String tag = EMPTY_STRING;
      if (mMainThreadHandler.isTracing()) {
        tag = "postBackgroundLayoutStateUpdated";
      }
      mMainThreadHandler.post(mBackgroundLayoutStateUpdateRunnable, tag);
    }
  }

  /**
   * The contract is that in order to release a ComponentTree, you must do so from the main thread,
   * or guarantee that it will never be accessed from the main thread again. Usually HostView will
   * handle releasing, but if you never attach to a host view, then you should call release
   * yourself.
   */
  public void release() {
    if (mIsMounting) {
      throw new IllegalStateException("Releasing a ComponentTree that is currently being mounted");
    }

    synchronized (this) {
      mMainThreadHandler.remove(mBackgroundLayoutStateUpdateRunnable);

      synchronized (mCurrentCalculateLayoutRunnableLock) {
        if (mCurrentCalculateLayoutRunnable != null) {
          mLayoutThreadHandler.remove(mCurrentCalculateLayoutRunnable);
          mCurrentCalculateLayoutRunnable = null;
        }
      }
      synchronized (mUpdateStateSyncRunnableLock) {
        if (mUpdateStateSyncRunnable != null) {
          mLayoutThreadHandler.remove(mUpdateStateSyncRunnable);
          mUpdateStateSyncRunnable = null;
        }
      }

      synchronized (mLayoutStateFutureLock) {
        for (int i = 0; i < mLayoutStateFutures.size(); i++) {
          mLayoutStateFutures.get(i).release();
        }

        mLayoutStateFutures.clear();
      }

      if (mPreAllocateMountContentHandler != null) {
        mPreAllocateMountContentHandler.remove(mPreAllocateMountContentRunnable);
      }

      mReleased = true;
      if (mRoot != null) {
        mReleasedComponent = mRoot.getSimpleName();
      }
      if (mLithoView != null) {
        mLithoView.setComponentTree(null);
      }
      mRoot = null;

      // Clear mWorkingRangeStatusHandler before releasing LayoutState because we need them to help
      // dispatch OnExitRange events.
      clearWorkingRangeStatusHandler();

      mMainThreadLayoutState = null;
      mBackgroundLayoutState = null;
      mLatestLayoutState = null;
      mStateHandler = null;
      mPreviousRenderState = null;
      mPreviousRenderStateSetFromBuilder = false;
      mMeasureListeners = null;
    }

    synchronized (mEventTriggersContainer) {
      clearUnusedTriggerHandlers();
    }

    if (mAttachDetachHandler != null) {
      // Execute detached callbacks if necessary.
      mAttachDetachHandler.onDetached();
    }
  }

  @GuardedBy("this")
  private boolean isCompatibleComponentAndSpec(LayoutState layoutState) {
    assertHoldsLock(this);

    return mRoot != null
        && isCompatibleComponentAndSpec(layoutState, mRoot.getId(), mWidthSpec, mHeightSpec);
  }

  // Either the MainThreadLayout or the BackgroundThreadLayout is compatible with the current state.
  @GuardedBy("this")
  private boolean hasCompatibleComponentAndSpec() {
    assertHoldsLock(this);

    return isCompatibleComponentAndSpec(mMainThreadLayoutState)
        || isCompatibleComponentAndSpec(mBackgroundLayoutState);
  }

  @GuardedBy("this")
  private boolean hasSizeSpec() {
    assertHoldsLock(this);

    return mWidthSpec != SIZE_UNINITIALIZED && mHeightSpec != SIZE_UNINITIALIZED;
  }

  @Nullable
  public synchronized String getSimpleName() {
    return mRoot == null ? null : mRoot.getSimpleName();
  }

  @Nullable
  synchronized Object getCachedValue(Object cachedValueInputs) {
    if (mReleased) {
      return null;
    }
    return mStateHandler.getCachedValue(cachedValueInputs);
  }

  @VisibleForTesting
  @Nullable
  AttachDetachHandler getAttachDetachHandler() {
    return mAttachDetachHandler;
  }

  synchronized void putCachedValue(Object cachedValueInputs, Object cachedValue) {
    if (mReleased) {
      return;
    }
    mStateHandler.putCachedValue(cachedValueInputs, cachedValue);
  }

  private static synchronized Looper getDefaultLayoutThreadLooper() {
    if (sDefaultLayoutThreadLooper == null) {
      final HandlerThread defaultThread =
          new HandlerThread(DEFAULT_LAYOUT_THREAD_NAME, DEFAULT_BACKGROUND_THREAD_PRIORITY);
      defaultThread.start();
      sDefaultLayoutThreadLooper = defaultThread.getLooper();
    }

    return sDefaultLayoutThreadLooper;
  }

  private static synchronized Looper getDefaultPreallocateMountContentThreadLooper() {
    if (sDefaultPreallocateMountContentThreadLooper == null) {
      final HandlerThread defaultThread = new HandlerThread(DEFAULT_PMC_THREAD_NAME);
      defaultThread.start();
      sDefaultPreallocateMountContentThreadLooper = defaultThread.getLooper();
    }

    return sDefaultPreallocateMountContentThreadLooper;
  }

  private static boolean isCompatibleSpec(LayoutState layoutState, int widthSpec, int heightSpec) {
    return layoutState != null
        && layoutState.isCompatibleSpec(widthSpec, heightSpec)
        && layoutState.isCompatibleAccessibility();
  }

  private static boolean isCompatibleComponentAndSpec(
      LayoutState layoutState, int componentId, int widthSpec, int heightSpec) {
    return layoutState != null
        && layoutState.isCompatibleComponentAndSpec(componentId, widthSpec, heightSpec)
        && layoutState.isCompatibleAccessibility();
  }

  private static boolean isCompatibleComponentAndSize(
      LayoutState layoutState, int componentId, int width, int height) {
    return layoutState != null
        && layoutState.isForComponentId(componentId)
        && layoutState.isCompatibleSize(width, height)
        && layoutState.isCompatibleAccessibility();
  }

  public synchronized boolean isReleased() {
    return mReleased;
  }

  synchronized String getReleasedComponent() {
    return mReleasedComponent;
  }

  public ComponentContext getContext() {
    return mContext;
  }

  // TODO: T48569046 remove this method and use mLogger
  private ComponentsLogger getContextLogger() {
    return mLogger == null ? mContext.getLogger() : mLogger;
  }

  public @Nullable ComponentsLogger getLogger() {
    return mLogger;
  }

  public @Nullable String getLogTag() {
    return mLogTag;
  }

  /*
   * The layouts which this ComponentTree was currently calculating will be terminated before
   * a valid result is computed. It's not safe to try to compute any layouts for this ComponentTree
   * after that because it's in an incomplete state, so it needs to be released.
   */
  public void cancelLayoutAndReleaseTree() {
    if (!mUseCancelableLayoutFutures) {
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.ERROR,
          TAG,
          "Cancelling layouts for a ComponentTree with useCancelableLayoutFutures set to false is a no-op.");
      return;
    }

    synchronized (mLayoutStateFutureLock) {
      for (int i = 0, size = mLayoutStateFutures.size(); i < size; i++) {
        mLayoutStateFutures.get(i).release();
      }
    }

    release();
  }

  private @Nullable LayoutState calculateLayoutState(
      ComponentContext context,
      Component root,
      int widthSpec,
      int heightSpec,
      int layoutVersion,
      boolean diffingEnabled,
      @Nullable TreeProps treeProps,
      @CalculateLayoutSource int source,
      @Nullable String extraAttribution) {

    LayoutStateFuture localLayoutStateFuture =
        new LayoutStateFuture(
            context,
            root,
            widthSpec,
            heightSpec,
            layoutVersion,
            diffingEnabled,
            treeProps,
            source,
            extraAttribution);
    final boolean waitingFromSyncLayout = localLayoutStateFuture.isFromSyncLayout;

    synchronized (mLayoutStateFutureLock) {
      boolean canReuse = false;
      for (int i = 0; i < mLayoutStateFutures.size(); i++) {
        final LayoutStateFuture runningLsf = mLayoutStateFutures.get(i);
        if (!runningLsf.isReleased() && runningLsf.equals(localLayoutStateFuture)) {
          // Use the latest LayoutState calculation if it's the same.
          localLayoutStateFuture = runningLsf;
          canReuse = true;
          break;
        }
      }
      if (!canReuse) {
        mLayoutStateFutures.add(localLayoutStateFuture);
      }

      localLayoutStateFuture.registerForResponse(waitingFromSyncLayout);
    }

    final LayoutState layoutState = localLayoutStateFuture.runAndGet(source);

    synchronized (mLayoutStateFutureLock) {
      localLayoutStateFuture.unregisterForResponse();

      // This future has finished executing, if no other threads were waiting for the response we
      // can remove it.
      if (localLayoutStateFuture.getWaitingCount() == 0) {
        localLayoutStateFuture.release();
        mLayoutStateFutures.remove(localLayoutStateFuture);
      }
    }

    if (context.getAndroidContext() != root.getBuilderContext()) {
      final String message =
          "ComponentTree context is different from root builder context"
              + ", ComponentTree context="
              + context.getAndroidContext()
              + ", root builder context="
              + root.getBuilderContext()
              + ", root="
              + root.getSimpleName()
              + ", ContextTree="
              + ComponentTreeDumpingHelper.dumpContextTree(context);
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.ERROR,
          CT_CONTEXT_IS_DIFFERENT_FROM_ROOT_BUILDER_CONTEXT,
          message);
    }

    return layoutState;
  }

  @VisibleForTesting
  List<LayoutStateFuture> getLayoutStateFutures() {
    return mLayoutStateFutures;
  }

  private AttachDetachHandler getOrCreateAttachDetachHandler() {
    AttachDetachHandler localAttachDetachHandler = mAttachDetachHandler;
    if (localAttachDetachHandler == null) {
      synchronized (this) {
        localAttachDetachHandler = mAttachDetachHandler;
        if (localAttachDetachHandler == null) {
          mAttachDetachHandler = localAttachDetachHandler = new AttachDetachHandler();
        }
      }
    }
    return localAttachDetachHandler;
  }

  @IntDef({
    RecyclingMode.DEFAULT,
    RecyclingMode.NO_VIEW_REUSE,
    RecyclingMode.NO_VIEW_RECYCLING,
    RecyclingMode.NO_UNMOUNTING
  })
  public @interface RecyclingMode {
    /** Default recycling mode. */
    int DEFAULT = 0;
    /**
     * Keep calling unmount and returning Views to the recycle pool, but do not actually reuse them.
     * Take a view out of the pool but throw it away. Create a new view instead of using it. This is
     * to test the hypothesis that the crashes are related to the actual re-use of views (i.e. the
     * view is in a bad state and we re-attach it to the tree and cause a crash)
     */
    int NO_VIEW_REUSE = 1;
    /**
     * Keep calling unmount, but do not put Views into the recycle pool. This is to test the
     * hypothesis that the crashes are more related to holding onto Views longer rather than the
     * actual re-use of them. If we see crashes decrease in this variant but not in the
     * NO_VIEW_REUSE variant, this would be indicative of just holding onto views being the problem.
     */
    int NO_VIEW_RECYCLING = 2;
    /**
     * Do not call Component.unmount, do not put Views into the recycle pool. This is to test the
     * hypothesis that the crashes are more related to unmount calls (which also execute product
     * logic). If we do not see crashes improving in the first two variants but improving in this
     * one, this would be indicative of one of the unmount implementations being involved in the
     * crash (e.g. unmount racing with onCreateLayout on another thread).
     */
    int NO_UNMOUNTING = 3;
  }

  /** Wraps a {@link FutureTask} to deduplicate calculating the same LayoutState across threads. */
  class LayoutStateFuture {

    private final AtomicInteger runningThreadId = new AtomicInteger(-1);
    private final ComponentContext context;
    private final Component root;
    private final int widthSpec;
    private final int heightSpec;
    private final boolean diffingEnabled;
    @Nullable private final TreeProps treeProps;
    private final FutureTask<LayoutState> futureTask;
    private final AtomicInteger refCount = new AtomicInteger(0);
    private final boolean isFromSyncLayout;
    private final int layoutVersion;
    private volatile boolean interruptRequested;
    private final int source;
    private final String extraAttribution;

    @Nullable private volatile Object interruptToken;
    @Nullable private volatile Object continuationToken;

    @GuardedBy("LayoutStateFuture.this")
    private volatile boolean released = false;

    private boolean isBlockingSyncLayout;

    private LayoutStateFuture(
        final ComponentContext context,
        final Component root,
        final int widthSpec,
        final int heightSpec,
        int layoutVersion,
        final boolean diffingEnabled,
        @Nullable final TreeProps treeProps,
        @CalculateLayoutSource final int source,
        @Nullable final String extraAttribution) {
      this.context = context;
      this.root = root;
      this.widthSpec = widthSpec;
      this.heightSpec = heightSpec;
      this.diffingEnabled = diffingEnabled;
      this.treeProps = treeProps;
      this.isFromSyncLayout = isFromSyncLayout(source);
      this.source = source;
      this.extraAttribution = extraAttribution;
      this.layoutVersion = layoutVersion;

      this.futureTask =
          new FutureTask<>(
              new Callable<LayoutState>() {
                @Override
                public @Nullable LayoutState call() {
                  synchronized (LayoutStateFuture.this) {
                    if (released) {
                      return null;
                    }
                  }
                  final LayoutState result = calculateLayoutStateInternal();
                  synchronized (LayoutStateFuture.this) {
                    if (released) {
                      return null;
                    } else {
                      return result;
                    }
                  }
                }
              });
    }

    private LayoutState calculateLayoutStateInternal() {
      @Nullable
      LayoutStateFuture layoutStateFuture =
          ComponentTree.this.mMoveLayoutsBetweenThreads
                  || ComponentTree.this.mUseCancelableLayoutFutures
              ? LayoutStateFuture.this
              : null;
      final ComponentContext contextWithStateHandler;
      final LayoutState previousLayoutState;

      synchronized (ComponentTree.this) {
        final StateHandler stateHandler =
            StateHandler.createNewInstance(ComponentTree.this.mStateHandler);
        previousLayoutState = mLatestLayoutState;
        contextWithStateHandler = new ComponentContext(context, stateHandler, treeProps, null);
        mInitialStateContainer.registerStateHandler(stateHandler);
      }

      return LayoutState.calculate(
          contextWithStateHandler,
          root,
          layoutStateFuture,
          ComponentTree.this.mId,
          widthSpec,
          heightSpec,
          layoutVersion,
          diffingEnabled,
          previousLayoutState,
          source,
          extraAttribution);
    }

    private boolean isFromSyncLayout(@CalculateLayoutSource int source) {
      switch (source) {
        case CalculateLayoutSource.MEASURE:
        case CalculateLayoutSource.SET_ROOT_SYNC:
        case CalculateLayoutSource.UPDATE_STATE_SYNC:
        case CalculateLayoutSource.SET_SIZE_SPEC_SYNC:
          return true;
        default:
          return false;
      }
    }

    @VisibleForTesting
    synchronized void release() {
      if (released) {
        return;
      }
      interruptToken = continuationToken = null;
      released = true;
    }

    boolean isReleased() {
      return released;
    }

    boolean isInterruptRequested() {
      return !ThreadUtils.isMainThread() && interruptRequested;
    }

    private void interrupt() {
      interruptRequested = true;
    }

    void unregisterForResponse() {
      final int newRefCount = refCount.decrementAndGet();

      if (newRefCount < 0) {
        throw new IllegalStateException("LayoutStateFuture ref count is below 0");
      }
    }

    void registerForResponse(boolean waitingFromSyncLayout) {
      refCount.incrementAndGet();
      if (waitingFromSyncLayout) {
        this.isBlockingSyncLayout = true;
      }
    }

    public int getWaitingCount() {
      return refCount.get();
    }

    boolean canBeCancelled() {
      return !isBlockingSyncLayout && !isFromSyncLayout;
    }

    @VisibleForTesting
    @Nullable
    LayoutState runAndGet(int source) {
      if (runningThreadId.compareAndSet(-1, Process.myTid())) {
        futureTask.run();
      }

      final int runningThreadId = this.runningThreadId.get();
      final boolean notRunningOnMyThread = runningThreadId != Process.myTid();
      final int originalThreadPriority;
      final boolean didRaiseThreadPriority;

      final boolean shouldWaitForResult = !futureTask.isDone() && notRunningOnMyThread;

      if (shouldWaitForResult && !isMainThread() && !isFromSyncLayout(source)) {
        return null;
      }

      if (isMainThread() && shouldWaitForResult) {
        // This means the UI thread is about to be blocked by the bg thread. Instead of waiting,
        // the bg task is interrupted.
        if (mMoveLayoutsBetweenThreads && !isFromSyncLayout) {
          interrupt();
          interruptToken =
              WorkContinuationInstrumenter.onAskForWorkToContinue("interruptCalculateLayout");
        }

        originalThreadPriority =
            ThreadUtils.tryRaiseThreadPriority(runningThreadId, Process.THREAD_PRIORITY_DISPLAY);
        didRaiseThreadPriority = true;
      } else {
        originalThreadPriority = THREAD_PRIORITY_DEFAULT;
        didRaiseThreadPriority = false;
      }

      LayoutState result;
      PerfEvent logFutureTaskGetWaiting = null;
      final ComponentsLogger logger = getContextLogger();
      final boolean shouldTrace = notRunningOnMyThread && ComponentsSystrace.isTracing();
      try {
        if (shouldTrace) {
          ComponentsSystrace.beginSectionWithArgs("LayoutStateFuture.get")
              .arg("treeId", ComponentTree.this.mId)
              .arg("root", root.getSimpleName())
              .arg("runningThreadId", runningThreadId)
              .flush();

          ComponentsSystrace.beginSectionWithArgs("LayoutStateFuture.wait")
              .arg("treeId", ComponentTree.this.mId)
              .arg("root", root.getSimpleName())
              .arg("runningThreadId", runningThreadId)
              .flush();
        }

        logFutureTaskGetWaiting =
            logger != null
                ? LogTreePopulator.populatePerfEventFromLogger(
                    mContext,
                    logger,
                    logger.newPerformanceEvent(mContext, EVENT_LAYOUT_STATE_FUTURE_GET_WAIT))
                : null;
        result = futureTask.get();

        if (shouldTrace) {
          ComponentsSystrace.endSection();
        }

        if (logFutureTaskGetWaiting != null) {
          logFutureTaskGetWaiting.markerPoint("FUTURE_TASK_END");
        }

        if (didRaiseThreadPriority) {
          // Reset the running thread's priority after we're unblocked.
          try {
            Process.setThreadPriority(runningThreadId, originalThreadPriority);
          } catch (IllegalArgumentException | SecurityException ignored) {
          }
        }

        if (interruptRequested && result.isPartialLayoutState()) {
          if (ThreadUtils.isMainThread()) {
            // This means that the bg task was interrupted and it returned a partially resolved
            // InternalNode. We need to finish computing this LayoutState.
            final Object token =
                onBeginWorkContinuation("continuePartialLayoutState", continuationToken);
            continuationToken = null;
            try {
              result = resolvePartialInternalNodeAndCalculateLayout(result);
            } catch (Throwable th) {
              markFailure(token, th);
              throw th;
            } finally {
              onEndWorkContinuation(token);
            }
          } else {
            // This means that the bg task was interrupted and the UI thread will pick up the rest
            // of
            // the work. No need to return a LayoutState.
            result = null;
            continuationToken =
                onOfferWorkForContinuation("offerPartialLayoutState", interruptToken);
            interruptToken = null;
          }
        }
      } catch (ExecutionException | InterruptedException | CancellationException e) {

        if (shouldTrace) {
          ComponentsSystrace.endSection();
        }

        final Throwable cause = e.getCause();
        if (cause instanceof RuntimeException) {
          throw (RuntimeException) cause;
        } else {
          throw new RuntimeException(e.getMessage(), e);
        }
      } finally {
        if (shouldTrace) {
          ComponentsSystrace.endSection();
        }
        if (logFutureTaskGetWaiting != null) {
          logFutureTaskGetWaiting.markerAnnotate(
              PARAM_LAYOUT_FUTURE_WAIT_FOR_RESULT, shouldWaitForResult);
          logFutureTaskGetWaiting.markerAnnotate(PARAM_IS_MAIN_THREAD, isMainThread());
          logger.logPerfEvent(logFutureTaskGetWaiting);
        }
      }

      if (result == null) {
        return null;
      }
      synchronized (LayoutStateFuture.this) {
        if (released) {
          return null;
        }
        return result;
      }
    }

    private LayoutState resolvePartialInternalNodeAndCalculateLayout(
        final LayoutState partialLayoutState) {
      if (released) {
        return null;
      }
      final LayoutState result =
          LayoutState.resumeCalculate(source, extraAttribution, partialLayoutState);

      synchronized (LayoutStateFuture.this) {
        return released ? null : result;
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      LayoutStateFuture that = (LayoutStateFuture) o;

      if (widthSpec != that.widthSpec) {
        return false;
      }
      if (heightSpec != that.heightSpec) {
        return false;
      }
      if (!context.equals(that.context)) {
        return false;
      }
      if (root.getId() != that.root.getId()) {
        // We only care that the root id is the same since the root is shallow copied before
        // it's passed to us and will never be the same object.
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int result = context.hashCode();
      result = 31 * result + root.getId();
      result = 31 * result + widthSpec;
      result = 31 * result + heightSpec;
      return result;
    }
  }

  public static int generateComponentTreeId() {
    return sIdGenerator.getAndIncrement();
  }

  @VisibleForTesting
  EventHandlersController getEventHandlersController() {
    return mEventHandlersController;
  }

  private class CalculateLayoutRunnable extends ThreadTracingRunnable {

    private final @CalculateLayoutSource int mSource;
    @Nullable private final TreeProps mTreeProps;
    private final String mAttribution;
    private final boolean mIsCreateLayoutInProgress;

    public CalculateLayoutRunnable(
        @CalculateLayoutSource int source,
        @Nullable TreeProps treeProps,
        String attribution,
        boolean isCreateLayoutInProgress) {
      mSource = source;
      mTreeProps = treeProps;
      mAttribution = attribution;
      mIsCreateLayoutInProgress = isCreateLayoutInProgress;
    }

    @Override
    public void tracedRun(ThreadTracingRunnable prevTracingRunnable) {
      calculateLayout(null, mSource, mAttribution, mTreeProps, mIsCreateLayoutInProgress);
    }
  }

  private final class UpdateStateSyncRunnable extends ThreadTracingRunnable {

    private final String mAttribution;
    private final boolean mIsCreateLayoutInProgress;

    public UpdateStateSyncRunnable(String attribution, boolean isCreateLayoutInProgress) {
      mAttribution = attribution;
      mIsCreateLayoutInProgress = isCreateLayoutInProgress;
    }

    @Override
    public void tracedRun(ThreadTracingRunnable prevTracingRunnable) {
      updateStateInternal(false, mAttribution, mIsCreateLayoutInProgress);
    }
  }

  /**
   * An encapsulation of currentVisibleArea and processVisibilityOutputs for each re-entrant mount.
   */
  private static final class ReentrantMount {
    @Nullable final Rect currentVisibleArea;
    final boolean processVisibilityOutputs;

    private ReentrantMount(@Nullable Rect currentVisibleArea, boolean processVisibilityOutputs) {
      this.currentVisibleArea = currentVisibleArea;
      this.processVisibilityOutputs = processVisibilityOutputs;
    }
  }

  /** A builder class that can be used to create a {@link ComponentTree}. */
  public static class Builder {

    // required
    private final ComponentContext context;
    private boolean visibilityProcessingEnabled = true;
    private @RecyclingMode int recyclingMode = RecyclingMode.DEFAULT;
    private Component root;

    // optional
    private boolean incrementalMountEnabled = true;
    private boolean isLayoutDiffingEnabled = true;
    private LithoHandler layoutThreadHandler;
    private LithoHandler preAllocateMountContentHandler;
    private StateHandler stateHandler;
    private RenderState previousRenderState;
    private boolean asyncStateUpdates = true;
    private int overrideComponentTreeId = -1;
    private boolean hasMounted = false;
    private @Nullable MeasureListener mMeasureListener;
    private boolean shouldPreallocatePerMountSpec;
    private boolean canPreallocateOnDefaultHandler;
    private boolean isReconciliationEnabled = ComponentsConfiguration.isReconciliationEnabled;
    private boolean canInterruptAndMoveLayoutsBetweenThreads =
        ComponentsConfiguration.canInterruptAndMoveLayoutsBetweenThreads;
    private boolean useCancelableLayoutFutures = ComponentsConfiguration.useCancelableLayoutFutures;
    private @Nullable String logTag;
    private @Nullable ComponentsLogger logger;
    private boolean incrementalVisibility = ComponentsConfiguration.incrementalVisibilityHandling;
    private boolean shouldForceAsyncStateUpdate =
        ComponentsConfiguration.shouldForceAsyncStateUpdate;

    protected Builder(ComponentContext context) {
      this.context = context;
    }

    /**
     * Specify root for the component tree
     *
     * <p>IMPORTANT: If you do not set this, a default root will be set and you can reset root after
     * build and attach of the component tree
     */
    public Builder withRoot(Component root) {
      if (root == null) {
        throw new NullPointerException("Creating a ComponentTree with a null root is not allowed!");
      }

      this.root = root;
      return this;
    }

    /**
     * Whether or not to enable the incremental mount optimization. True by default.
     *
     * <p>IMPORTANT: if you set this to false, visibility events will NOT FIRE. Please don't use
     * this unless you really need to.
     */
    public Builder incrementalMount(boolean isEnabled) {
      incrementalMountEnabled = isEnabled;
      return this;
    }

    public Builder visibilityProcessing(boolean isEnabled) {
      visibilityProcessingEnabled = isEnabled;
      return this;
    }

    /**
     * Whether or not to enable layout tree diffing. This will reduce the cost of updates at the
     * expense of using extra memory. True by default.
     *
     * <p>We will remove this option soon, please consider turning it on (which is on by default)
     */
    @Deprecated
    public Builder layoutDiffing(boolean enabled) {
      isLayoutDiffingEnabled = enabled;
      return this;
    }

    /**
     * Specify the looper to use for running layouts on. Note that in rare cases layout must run on
     * the UI thread. For example, if you rotate the screen, we must measure on the UI thread. If
     * you don't specify a Looper here, the Components default Looper will be used.
     */
    public Builder layoutThreadLooper(Looper looper) {
      if (looper != null) {
        layoutThreadHandler = new DefaultLithoHandler(looper);
      }

      return this;
    }

    /** Specify the handler for to preAllocateMountContent */
    public Builder preAllocateMountContentHandler(LithoHandler handler) {
      preAllocateMountContentHandler = handler;
      return this;
    }

    /**
     * If true, this ComponentTree will only preallocate mount specs that are enabled for
     * preallocation with {@link MountSpec#canPreallocate()}. If false, it preallocates all mount
     * content.
     */
    public Builder shouldPreallocateMountContentPerMountSpec(boolean preallocatePerMountSpec) {
      shouldPreallocatePerMountSpec = preallocatePerMountSpec;
      return this;
    }

    /**
     * If true, mount content preallocation will use a default layout handler to preallocate mount
     * content on a background thread if no other layout handler is provided through {@link
     * ComponentTree.Builder#preAllocateMountContentHandler(LithoHandler)}.
     */
    public Builder preallocateOnDefaultHandler(boolean preallocateOnDefaultHandler) {
      canPreallocateOnDefaultHandler = preallocateOnDefaultHandler;
      return this;
    }

    /**
     * Specify the looper to use for running layouts on. Note that in rare cases layout must run on
     * the UI thread. For example, if you rotate the screen, we must measure on the UI thread. If
     * you don't specify a Looper here, the Components default Looper will be used.
     */
    public Builder layoutThreadHandler(LithoHandler handler) {
      layoutThreadHandler = handler;
      return this;
    }

    /**
     * Specify an initial state handler object that the ComponentTree can use to set the current
     * values for states.
     */
    public Builder stateHandler(StateHandler stateHandler) {
      this.stateHandler = stateHandler;
      return this;
    }

    /**
     * Specify an existing previous render state that the ComponentTree can use to set the current
     * values for providing previous versions of @Prop/@State variables.
     */
    public Builder previousRenderState(RenderState previousRenderState) {
      this.previousRenderState = previousRenderState;
      return this;
    }

    /** Specify whether the ComponentTree allows async state updates. This is enabled by default. */
    public Builder asyncStateUpdates(boolean enabled) {
      this.asyncStateUpdates = enabled;
      return this;
    }

    /**
     * Gives the ability to override the auto-generated ComponentTree id: this is generally not
     * useful in the majority of circumstances, so don't use it unless you really know what you're
     * doing.
     */
    public Builder overrideComponentTreeId(int overrideComponentTreeId) {
      this.overrideComponentTreeId = overrideComponentTreeId;
      return this;
    }

    /**
     * Sets whether the 'hasMounted' flag should be set on this ComponentTree (for use with appear
     * animations).
     */
    public Builder hasMounted(boolean hasMounted) {
      this.hasMounted = hasMounted;
      return this;
    }

    public Builder measureListener(MeasureListener measureListener) {
      this.mMeasureListener = measureListener;
      return this;
    }

    /** Sets if reconciliation is enabled */
    public Builder isReconciliationEnabled(boolean isEnabled) {
      this.isReconciliationEnabled = isEnabled;
      return this;
    }

    /** Experimental, do not use!! If used recycling for components may not happen. */
    public Builder recyclingMode(@RecyclingMode int recyclingMode) {
      this.recyclingMode = recyclingMode;
      return this;
    }

    /**
     * Experimental, do not use! If enabled, cancel a layout calculation before it finishes if
     * there's another layout pending with a newer state of the ComponentTree.
     */
    public Builder useCancelableLayoutFutures(boolean isEnabled) {
      this.useCancelableLayoutFutures = isEnabled;
      return this;
    }

    public Builder incrementalVisibility(boolean isEnabled) {
      this.incrementalVisibility = isEnabled;
      return this;
    }

    /**
     * Experimental, do not use! If enabled, a layout computation can be interrupted on a bg thread
     * and resumed on the UI thread if it's needed immediately.
     */
    public Builder canInterruptAndMoveLayoutsBetweenThreads(boolean isEnabled) {
      this.canInterruptAndMoveLayoutsBetweenThreads = isEnabled;
      return this;
    }

    // TODO: T48569046 verify the usage, if this should be split up
    public Builder logger(@Nullable ComponentsLogger logger, @Nullable String logTag) {
      this.logger = logger;
      this.logTag = logTag;
      return this;
    }

    /** Builds a {@link ComponentTree} using the parameters specified in this builder. */
    public ComponentTree build() {

      // Setting root to default to allow users to initialise without a root.
      if (root == null) {
        root = Row.create(context).build();
      }
      // TODO: T48569046 verify logTag when it will be set on CT directly
      if (logger != null && logTag == null) {
        logTag = root.getSimpleName();
      }

      return new ComponentTree(this);
    }
  }
}
