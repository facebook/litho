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

package com.facebook.litho;

import static com.facebook.litho.FrameworkLogEvents.EVENT_CALCULATE_LAYOUT_STATE;
import static com.facebook.litho.FrameworkLogEvents.EVENT_LAYOUT_STATE_FUTURE_GET_WAIT;
import static com.facebook.litho.FrameworkLogEvents.EVENT_PRE_ALLOCATE_MOUNT_CONTENT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_ATTRIBUTION;
import static com.facebook.litho.FrameworkLogEvents.PARAM_COMPONENT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_IS_BACKGROUND_LAYOUT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_IS_MAIN_THREAD;
import static com.facebook.litho.FrameworkLogEvents.PARAM_LAYOUT_FUTURE_WAIT_FOR_RESULT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_LAYOUT_STATE_SOURCE;
import static com.facebook.litho.FrameworkLogEvents.PARAM_LAYOUT_VERSION;
import static com.facebook.litho.FrameworkLogEvents.PARAM_TREE_DIFF_ENABLED;
import static com.facebook.litho.LayoutState.CalculateLayoutSource;
import static com.facebook.litho.LayoutState.isFromSyncLayout;
import static com.facebook.litho.LayoutState.layoutSourceToString;
import static com.facebook.litho.LithoLifecycleProvider.LithoLifecycle.HINT_INVISIBLE;
import static com.facebook.litho.LithoLifecycleProvider.LithoLifecycle.HINT_VISIBLE;
import static com.facebook.litho.StateContainer.StateUpdate;
import static com.facebook.litho.ThreadUtils.assertHoldsLock;
import static com.facebook.litho.ThreadUtils.assertMainThread;
import static com.facebook.litho.ThreadUtils.isMainThread;
import static com.facebook.litho.config.ComponentsConfiguration.DEFAULT_BACKGROUND_THREAD_PRIORITY;
import static com.facebook.rendercore.instrumentation.HandlerInstrumenter.instrumentHandler;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.view.Choreographer;
import android.view.View;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.litho.LithoLifecycleProvider.LithoLifecycle;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.animation.AnimatedProperty;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.perfboost.LithoPerfBooster;
import com.facebook.litho.stats.LithoStats;
import com.facebook.rendercore.RunnableHandler;
import com.facebook.rendercore.RunnableHandler.DefaultHandler;
import com.facebook.rendercore.Systracer;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
public class ComponentTree implements LithoLifecycleListener {

  private static final boolean DEBUG_LOGS = false;

  public static final int INVALID_LAYOUT_VERSION = -1;
  public static final int INVALID_ID = -1;
  private static final String INVALID_KEY = "LithoTooltipController:InvalidKey";
  private static final String INVALID_HANDLE = "LithoTooltipController:InvalidHandle";
  private static final String TAG = ComponentTree.class.getSimpleName();
  private static final int SIZE_UNINITIALIZED = -1;
  private static final String DEFAULT_RESOLVE_THREAD_NAME = "ComponentResolveThread";
  private static final String DEFAULT_LAYOUT_THREAD_NAME = "ComponentLayoutThread";
  private static final String EMPTY_STRING = "";
  private static final String REENTRANT_MOUNTS_EXCEED_MAX_ATTEMPTS =
      "ComponentTree:ReentrantMountsExceedMaxAttempts";
  private static final int REENTRANT_MOUNTS_MAX_ATTEMPTS = 25;
  private static final String CT_CONTEXT_IS_DIFFERENT_FROM_ROOT_BUILDER_CONTEXT =
      "ComponentTree:CTContextIsDifferentFromRootBuilderContext";
  private static final String M_LITHO_VIEW_IS_NULL =
      "ComponentTree:mountComponentInternal_mLithoView_Null";

  public static final int STATE_UPDATES_IN_LOOP_THRESHOLD = 50;
  private static final String STATE_UPDATES_IN_LOOP_EXCEED_THRESHOLD =
      "ComponentTree:StateUpdatesWhenLayoutInProgressExceedsThreshold:";
  private static boolean sBoostPerfLayoutStateFuture = false;
  @Nullable LithoLifecycleProvider mLifecycleProvider;
  private final boolean mAreTransitionsEnabled;
  private final ComponentsConfiguration mComponentsConfiguration;

  @GuardedBy("this")
  private boolean mReleased;

  interface OnReleaseListener {

    /** Called when this ComponentTree is released. */
    void onReleased();
  }

  @ThreadConfined(ThreadConfined.UI)
  private @Nullable List<OnReleaseListener> mOnReleaseListeners;

  private String mReleasedComponent;
  private @Nullable volatile AttachDetachHandler mAttachDetachHandler;
  private @Nullable Deque<ReentrantMount> mReentrantMounts;

  private @Nullable DebugComponentTimeMachine.TreeRevisions mTimeline;

  @GuardedBy("this")
  private int mStateUpdatesFromCreateLayoutCount;

  private final RenderUnitIdGenerator mRenderUnitIdGenerator;
  private boolean mInAttach = false;

  // Used to lazily store a CoroutineScope, if coroutine helper methods are used.
  final AtomicReference<Object> mInternalScopeRef = new AtomicReference<>();

  @Override
  public void onMovedToState(LithoLifecycle state) {
    switch (state) {
      case HINT_VISIBLE:
        onMoveToStateHintVisible();
        return;
      case HINT_INVISIBLE:
        onMoveToStateHintInvisible();
        return;
      case DESTROYED:
        onMoveToStateDestroy();
        return;
      default:
        throw new IllegalStateException("Illegal state: " + state);
    }
  }

  private void onMoveToStateHintVisible() {
    if (mLithoView != null) {
      mLithoView.setVisibilityHintNonRecursive(true);
    }
  }

  private void onMoveToStateHintInvisible() {
    if (mLithoView != null) {
      mLithoView.setVisibilityHintNonRecursive(false);
    }
  }

  private void onMoveToStateDestroy() {
    // This will call setComponentTree(null) on the LithoView if any.
    release();
    if (mLifecycleProvider != null) {
      mLifecycleProvider.removeListener(this);
      mLifecycleProvider = null;
    }
  }

  public synchronized void subscribeToLifecycleProvider(LithoLifecycleProvider lifecycleProvider) {
    if (mLifecycleProvider != null) {
      throw new IllegalStateException("Already subscribed");
    }
    mLifecycleProvider = lifecycleProvider;
    mLifecycleProvider.addListener(this);
  }

  public synchronized boolean isSubscribedToLifecycleProvider() {
    return mLifecycleProvider != null;
  }

  RenderUnitIdGenerator getRenderUnitIdGenerator() {
    return mRenderUnitIdGenerator;
  }

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
  private static volatile Looper sDefaultResolveThreadLooper;

  @GuardedBy("ComponentTree.class")
  private static volatile Looper sDefaultPreallocateMountContentThreadLooper;

  private static final ThreadLocal<WeakReference<RunnableHandler>> sSyncStateUpdatesHandler =
      new ThreadLocal<>();

  private final ThreadLocal<CalculationStateContext> mCalculationStateContextThreadLocal =
      new ThreadLocal<CalculationStateContext>();

  private final @Nullable IncrementalMountHelper mIncrementalMountHelper;
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

  private @Nullable RunnableHandler mPreAllocateMountContentHandler;

  // These variables are only accessed from the main thread.
  @ThreadConfined(ThreadConfined.UI)
  private boolean mIsMounting;

  @ThreadConfined(ThreadConfined.UI)
  private boolean mIsMeasuring;

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
  private @Nullable LithoView mLithoView;

  @ThreadConfined(ThreadConfined.UI)
  private RunnableHandler mLayoutThreadHandler;

  @ThreadConfined(ThreadConfined.UI)
  private RunnableHandler mResolveThreadHandler;

  private RunnableHandler mMainThreadHandler = new DefaultHandler(Looper.getMainLooper());
  private final Runnable mBackgroundLayoutStateUpdateRunnable =
      new Runnable() {
        @Override
        public void run() {
          backgroundLayoutStateUpdated();
        }
      };
  private volatile @Nullable NewLayoutStateReadyListener mNewLayoutStateReadyListener;

  private final Object mCurrentCalculateLayoutRunnableLock = new Object();

  private final Object mCurrentCalculateLayoutFutureRunnableLock = new Object();

  private final Object mResolveRunnableLock = new Object();

  @GuardedBy("mCurrentCalculateLayoutRunnableLock")
  private @Nullable CalculateLayoutRunnable mCurrentCalculateLayoutRunnable;

  @GuardedBy("mCurrentCalculateLayoutFutureRunnableLock")
  private @Nullable CalculateLayoutFutureRunnable mCurrentCalculateLayoutFutureRunnable;

  @GuardedBy("mResolveRunnableLock")
  private @Nullable CalculateResolutionRunnable mResolveRunnable;

  private final Object mLayoutStateFutureLock = new Object();

  private final Object mResolvedResultFutureLock = new Object();

  @GuardedBy("mLayoutStateFutureLock")
  private final List<LayoutStateFuture> mLayoutStateFutures = new ArrayList<>();

  @GuardedBy("mResolvedResultFutureLock")
  private final List<RenderTreeFuture> mResolvedResultFutures = new ArrayList<>();

  @GuardedBy("mLayoutStateFutureLock")
  private final List<LayoutTreeFuture> mLayoutTreeFutures = new ArrayList<>();

  private volatile boolean mHasMounted;
  private volatile boolean mIsFirstMount;

  /** Transition that animates width of root component (LithoView). */
  @ThreadConfined(ThreadConfined.UI)
  @Nullable
  Transition.RootBoundsTransition mRootWidthAnimation;

  /** Transition that animates height of root component (LithoView). */
  @ThreadConfined(ThreadConfined.UI)
  @Nullable
  Transition.RootBoundsTransition mRootHeightAnimation;

  @GuardedBy("this")
  private @Nullable Component mRoot;

  @GuardedBy("this")
  private int mExternalRootVersion = INVALID_LAYOUT_VERSION;

  @GuardedBy("this")
  private int mNextResolveVersion;

  // Versioning that gets incremented every time we start a new layout computation. This can
  // be useful for stateful objects shared across layouts that need to check whether for example
  // a measure/onCreateLayout call is being executed in the context of an old layout calculation.
  @GuardedBy("this")
  private int mNextLayoutVersion;

  @GuardedBy("this")
  private int mCommittedLayoutVersion = INVALID_LAYOUT_VERSION;

  @GuardedBy("this")
  private @Nullable TreeProps mRootTreeProps;

  @GuardedBy("this")
  private int mWidthSpec = SIZE_UNINITIALIZED;

  @GuardedBy("this")
  private int mHeightSpec = SIZE_UNINITIALIZED;

  @GuardedBy("this")
  private @CalculateLayoutSource int mLastLayoutSource = CalculateLayoutSource.NONE;

  // This is written to only by the main thread with the lock held, read from the main thread with
  // no lock held, or read from any other thread with the lock held.
  private @Nullable LayoutState mMainThreadLayoutState;

  @GuardedBy("this")
  private @Nullable LayoutState mCommittedLayoutState;

  @GuardedBy("this")
  private @Nullable LithoResolutionResult mCommittedResolutionResult;

  @GuardedBy("this")
  private @Nullable TreeState mTreeState;

  @ThreadConfined(ThreadConfined.UI)
  private @Nullable RenderState mPreviousRenderState;

  protected final int mId;

  private final ErrorEventHandler mErrorEventHandler;

  private final EventHandlersController mEventHandlersController = new EventHandlersController();

  private final EventTriggersContainer mEventTriggersContainer = new EventTriggersContainer();

  @GuardedBy("this")
  private final WorkingRangeStatusHandler mWorkingRangeStatusHandler =
      new WorkingRangeStatusHandler();

  private final boolean isReconciliationEnabled;

  private final boolean isSplitStateHandlersEnabled;

  private final boolean isResolveAndLayoutFuturesSplitEnabled;

  private final boolean useSeparateThreadHandlersForResolveAndLayout;

  private final boolean isReuseLastMeasuredNodeInComponentMeasureEnabled;

  private final boolean mMoveLayoutsBetweenThreads;

  private final @Nullable String mLogTag;

  private final @Nullable ComponentsLogger mLogger;

  private final @Nullable BatchedStateUpdatesStrategy mBatchedStateUpdatesStrategy;

  public static Builder create(ComponentContext context) {
    return new ComponentTree.Builder(context);
  }

  public static Builder create(ComponentContext context, Component.Builder<?> root) {
    return create(context, root.build());
  }

  public static Builder create(ComponentContext context, Component root) {
    return create(context, root, null);
  }

  public static Builder create(
      ComponentContext context,
      Component root,
      @Nullable LithoLifecycleProvider lifecycleProvider) {
    // TODO T88511125: Enforce non-null lithoLifecycleOwner here.
    return new ComponentTree.Builder(context)
        .withRoot(root)
        .withLithoLifecycleProvider(lifecycleProvider);
  }

  protected ComponentTree(Builder builder) {
    mComponentsConfiguration = builder.componentsConfiguration;

    mContext = ComponentContext.withComponentTree(builder.context, this);
    mRoot = builder.root;
    if (builder.mLifecycleProvider != null) {
      subscribeToLifecycleProvider(builder.mLifecycleProvider);
    }

    if (ComponentsConfiguration.enableStateUpdatesBatching) {
      mBatchedStateUpdatesStrategy = new PostStateUpdateToChoreographerCallback();
    } else {
      mBatchedStateUpdatesStrategy = null;
    }

    mIncrementalMountEnabled =
        builder.incrementalMountEnabled && !incrementalMountGloballyDisabled();
    mVisibilityProcessingEnabled = builder.visibilityProcessingEnabled;
    if (ComponentsConfiguration.overrideLayoutDiffing != null) {
      mIsLayoutDiffingEnabled = ComponentsConfiguration.overrideLayoutDiffing;
    } else {
      mIsLayoutDiffingEnabled = builder.isLayoutDiffingEnabled;
    }
    mLayoutThreadHandler = builder.layoutThreadHandler;
    mShouldPreallocatePerMountSpec = builder.shouldPreallocatePerMountSpec;
    mPreAllocateMountContentHandler = builder.preAllocateMountContentHandler;
    mIsAsyncUpdateStateEnabled = builder.asyncStateUpdates;
    mHasMounted = builder.hasMounted;
    mIsFirstMount = builder.isFirstMount;
    addMeasureListener(builder.mMeasureListener);
    mMoveLayoutsBetweenThreads = builder.canInterruptAndMoveLayoutsBetweenThreads;
    if (ComponentsConfiguration.overrideReconciliation != null) {
      isReconciliationEnabled = ComponentsConfiguration.overrideReconciliation;
    } else {
      isReconciliationEnabled = builder.isReconciliationEnabled;
    }
    isReuseLastMeasuredNodeInComponentMeasureEnabled =
        ComponentsConfiguration.reuseLastMeasuredNodeInComponentMeasure;
    isResolveAndLayoutFuturesSplitEnabled =
        ComponentsConfiguration.isResolveAndLayoutFuturesSplitEnabled;
    useSeparateThreadHandlersForResolveAndLayout =
        isResolveAndLayoutFuturesSplitEnabled
            && ComponentsConfiguration.useSeparateThreadHandlersForResolveAndLayout;
    isSplitStateHandlersEnabled =
        ComponentsConfiguration.isSplitStateHandlersEnabled
            || isResolveAndLayoutFuturesSplitEnabled;
    mErrorEventHandler = builder.errorEventHandler;

    mTreeState = builder.treeState == null ? new TreeState() : builder.treeState;

    if (builder.previousRenderState != null) {
      mPreviousRenderState = builder.previousRenderState;
    }

    if (builder.overrideComponentTreeId != -1) {
      mId = builder.overrideComponentTreeId;
    } else {
      mId = generateComponentTreeId();
    }

    if (builder.mRenderUnitIdGenerator != null) {
      mRenderUnitIdGenerator = builder.mRenderUnitIdGenerator;
      if (mId != mRenderUnitIdGenerator.getComponentTreeId()) {
        throw new IllegalStateException(
            "Copying RenderUnitIdGenerator is only allowed if the ComponentTree IDs match");
      }
    } else {
      mRenderUnitIdGenerator = new RenderUnitIdGenerator(mId);
    }

    mIncrementalMountHelper =
        ComponentsConfiguration.USE_INCREMENTAL_MOUNT_HELPER
            ? new IncrementalMountHelper(this)
            : null;

    // Instrument LithoHandlers.
    mMainThreadHandler = instrumentHandler(mMainThreadHandler);
    mLayoutThreadHandler = ensureAndInstrumentLayoutThreadHandler(mLayoutThreadHandler);
    if (mPreAllocateMountContentHandler != null) {
      mPreAllocateMountContentHandler = instrumentHandler(mPreAllocateMountContentHandler);
    }

    if (useSeparateThreadHandlersForResolveAndLayout) {
      mResolveThreadHandler =
          builder.resolveThreadHandler != null
              ? instrumentHandler(builder.resolveThreadHandler)
              : instrumentHandler(new DefaultHandler(getDefaultResolveThreadLooper()));
    }

    mLogger = builder.logger;
    mLogTag = builder.logTag;
    mAreTransitionsEnabled = AnimationsDebug.areTransitionsEnabled(mContext.getAndroidContext());
  }

  private Object getResolveThreadHandlerLock() {
    return useSeparateThreadHandlersForResolveAndLayout
        ? mResolveRunnableLock
        : mCurrentCalculateLayoutFutureRunnableLock;
  }

  private RunnableHandler getResolveThreadHandler() {
    return useSeparateThreadHandlersForResolveAndLayout
        ? mResolveThreadHandler
        : mLayoutThreadHandler;
  }

  private static boolean incrementalMountGloballyDisabled() {
    return ComponentsConfiguration.isIncrementalMountGloballyDisabled;
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public void setCalculationStateContext(@Nullable CalculationStateContext stateContext) {
    mCalculationStateContextThreadLocal.set(stateContext);
  }

  @Nullable
  public CalculationStateContext getCalculationStateContext() {
    return mCalculationStateContextThreadLocal.get();
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

  private static RunnableHandler ensureAndInstrumentLayoutThreadHandler(
      @Nullable RunnableHandler handler) {
    if (handler == null) {
      handler = new DefaultHandler(getDefaultLayoutThreadLooper());
    } else if (sDefaultLayoutThreadLooper != null
        && sBoostPerfLayoutStateFuture == false
        && ComponentsConfiguration.boostPerfLayoutStateFuture == true
        && ComponentsConfiguration.perfBoosterFactory != null) {
      /* Right now we don't care about testing this per surface, so we'll use the config value. */
      LithoPerfBooster booster = ComponentsConfiguration.perfBoosterFactory.acquireInstance();
      booster.markImportantThread(new Handler(sDefaultLayoutThreadLooper));
      sBoostPerfLayoutStateFuture = true;
    }
    return instrumentHandler(handler);
  }

  @ThreadConfined(ThreadConfined.UI)
  @Nullable
  LayoutState getMainThreadLayoutState() {
    return mMainThreadLayoutState;
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  @GuardedBy("this")
  public @Nullable LayoutState getCommittedLayoutState() {
    return mCommittedLayoutState;
  }

  /** Whether this ComponentTree has been mounted at least once. */
  public boolean hasMounted() {
    return mHasMounted;
  }

  public boolean isFirstMount() {
    return mIsFirstMount;
  }

  public void setIsFirstMount(boolean isFirstMount) {
    mIsFirstMount = isFirstMount;
  }

  public void setNewLayoutStateReadyListener(@Nullable NewLayoutStateReadyListener listener) {
    mNewLayoutStateReadyListener = listener;
  }

  /**
   * Provide custom {@link RunnableHandler}. If null is provided default one will be used for
   * layouts.
   */
  @ThreadConfined(ThreadConfined.UI)
  public void updateLayoutThreadHandler(@Nullable RunnableHandler layoutThreadHandler) {
    if (!useSeparateThreadHandlersForResolveAndLayout) {
      synchronized (mUpdateStateSyncRunnableLock) {
        if (mUpdateStateSyncRunnable != null) {
          mLayoutThreadHandler.remove(mUpdateStateSyncRunnable);
        }
      }
    }

    synchronized (mCurrentCalculateLayoutRunnableLock) {
      if (mCurrentCalculateLayoutRunnable != null) {
        mLayoutThreadHandler.remove(mCurrentCalculateLayoutRunnable);
      }
    }
    mLayoutThreadHandler = ensureAndInstrumentLayoutThreadHandler(layoutThreadHandler);
  }

  @ThreadConfined(ThreadConfined.UI)
  public void updateResolveThreadHandler(@Nullable RunnableHandler resolveThreadHandler) {
    if (!useSeparateThreadHandlersForResolveAndLayout) {
      return;
    }

    synchronized (getResolveThreadHandlerLock()) {
      if (mResolveRunnable != null) {
        mResolveThreadHandler.remove(mResolveRunnable);
      }
    }

    synchronized (mUpdateStateSyncRunnableLock) {
      if (mUpdateStateSyncRunnable != null) {
        mResolveThreadHandler.remove(mUpdateStateSyncRunnable);
      }
    }

    if (resolveThreadHandler == null) {
      resolveThreadHandler = new DefaultHandler(getDefaultResolveThreadLooper());
    }

    mResolveThreadHandler = instrumentHandler(resolveThreadHandler);
  }

  @VisibleForTesting
  public RunnableHandler getLayoutThreadHandler() {
    return mLayoutThreadHandler;
  }

  @VisibleForTesting
  public @Nullable NewLayoutStateReadyListener getNewLayoutStateReadyListener() {
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

    final boolean layoutStateUpdated;
    synchronized (this) {
      if (mRoot == null) {
        // We have been released. Abort.
        return;
      }
      if (mCommittedLayoutState == null) {
        throw new RuntimeException("Unexpected null mCommittedLayoutState");
      }

      if (mMainThreadLayoutState != mCommittedLayoutState) {
        promoteCommittedLayoutStateToUI();
        layoutStateUpdated = true;
      } else {
        layoutStateUpdated = false;
      }
    }

    if (!layoutStateUpdated) {
      if (DEBUG_LOGS) {
        debugLog("backgroundLayoutStateUpdated", "Abort: LayoutState was not updated");
      }
      return;
    }

    dispatchNewLayoutStateReady();

    // If we are in measure, we will let mounting happen from the layout call
    if (!mIsAttached || mIsMeasuring) {
      if (DEBUG_LOGS) {
        debugLog(
            "backgroundLayoutStateUpdated",
            "Abort: will wait for attach/measure (mIsAttached: "
                + mIsAttached
                + ", mIsMeasuring: "
                + mIsMeasuring
                + ")");
      }
      return;
    }

    // We defer until measure if we don't yet have a width/height
    final int viewWidth = mLithoView.getMeasuredWidth();
    final int viewHeight = mLithoView.getMeasuredHeight();
    if (viewWidth == 0 && viewHeight == 0) {
      if (DEBUG_LOGS) {
        debugLog("backgroundLayoutStateUpdated", "Abort: Host view was not measured yet");
      }
      // The host view has not been measured yet.
      return;
    }

    final boolean needsAndroidLayout =
        mMainThreadLayoutState == null
            || mMainThreadLayoutState.getWidth() != viewWidth
            || mMainThreadLayoutState.getHeight() != viewHeight;

    if (needsAndroidLayout) {
      mLithoView.requestLayout();
    } else {
      mountComponentIfNeeded();
    }

    if (DEBUG_LOGS) {
      debugLog(
          "backgroundLayoutStateUpdated",
          "Updated - viewWidth: "
              + viewWidth
              + ", viewHeight: "
              + viewHeight
              + ", needsAndroidLayout: "
              + needsAndroidLayout
              + ", layoutRequested: "
              + mLithoView.isLayoutRequested());
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

    if (mLithoView != null) {
      mLithoView.maybeCollectAllTransitions(layoutState, this);
    }
  }

  void attach() {
    assertMainThread();

    if (mLithoView == null) {
      throw new IllegalStateException("Trying to attach a ComponentTree without a set View");
    }
    mInAttach = true;
    try {
      if (mIncrementalMountHelper != null && !mLithoView.skipNotifyVisibleBoundsChangedCalls()) {
        mIncrementalMountHelper.onAttach(mLithoView);
      }

      synchronized (this) {
        // We need to track that we are attached regardless...
        mIsAttached = true;

        if (mCommittedLayoutState != null && mMainThreadLayoutState != mCommittedLayoutState) {
          promoteCommittedLayoutStateToUI();
        }

        if (mRoot == null) {
          throw new IllegalStateException(
              "Trying to attach a ComponentTree with a null root. Is released: "
                  + mReleased
                  + ", Released Component name is: "
                  + mReleasedComponent);
        }
      }

      // We defer until measure if we don't yet have a width/height
      final int viewWidth = mLithoView.getMeasuredWidth();
      final int viewHeight = mLithoView.getMeasuredHeight();
      if (viewWidth == 0 && viewHeight == 0) {
        // The host view has not been measured yet.
        return;
      }

      final boolean needsAndroidLayout =
          mMainThreadLayoutState == null
              || mMainThreadLayoutState.getWidth() != viewWidth
              || mMainThreadLayoutState.getHeight() != viewHeight;

      if (needsAndroidLayout || mLithoView.isMountStateDirty()) {
        mLithoView.requestLayout();
      } else {
        mLithoView.rebind();
      }
    } finally {
      mInAttach = false;
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
        mLithoView.getCorrectedLocalVisibleRect(visibleRect);
        mountComponent(visibleRect, true);
      }

      return true;
    }

    return false;
  }

  @UiThread
  void incrementalMountComponent() {
    if (!mIncrementalMountEnabled) {
      throw new IllegalStateException(
          "Calling incrementalMountComponent() but incremental mount is not enabled");
    }
    notifyVisibleBoundsChanged();
  }

  @UiThread
  void notifyVisibleBoundsChanged() {
    assertMainThread();

    if (mLithoView == null) {
      return;
    }

    // Per ComponentTree visible area. Because LithoViews can be nested and mounted
    // not in "depth order", this variable cannot be static.
    final Rect currentVisibleArea = new Rect();
    final boolean hasNonEmptyVisibleRect =
        mLithoView.getCorrectedLocalVisibleRect(currentVisibleArea);

    if (ComponentsConfiguration.shouldContinueIncrementalMountWhenVisibileRectIsEmpty
        && !hasNonEmptyVisibleRect) {
      // Set to pure empty to allow for easy comparisons.
      currentVisibleArea.setEmpty();
    }

    if (ComponentsConfiguration.shouldContinueIncrementalMountWhenVisibileRectIsEmpty
        || hasNonEmptyVisibleRect
        || hasComponentsExcludedFromIncrementalMount(mMainThreadLayoutState)
        // It might not be yet visible but animating from 0 height/width in which case we still
        // need to mount them to trigger animation.
        || animatingRootBoundsFromZero(currentVisibleArea)) {
      mountComponent(currentVisibleArea, true);
    }
  }

  // Check if we should ignore the result of visible rect checking and continue doing
  // IncrementalMount.
  private static boolean hasComponentsExcludedFromIncrementalMount(
      @Nullable LayoutState layoutState) {
    return layoutState != null && layoutState.hasComponentsExcludedFromIncrementalMount();
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
        || isCompatibleSpec(mCommittedLayoutState, widthSpec, heightSpec);
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

    // We are investigating a crash where mLithoView becomes null during the execution of this
    // function. Use a local copy.
    final LithoView lithoViewRef = mLithoView;
    if (lithoViewRef == null) {
      return;
    }
    final boolean isDirtyMount = lithoViewRef.isMountStateDirty();

    mIsMounting = true;

    if (!mHasMounted) {
      mIsFirstMount = true;
      mHasMounted = true;
    }

    // currentVisibleArea null or empty => mount all
    try {
      lithoViewRef.mount(layoutState, currentVisibleArea, processVisibilityOutputs);
      if (isDirtyMount) {
        recordRenderData(layoutState);
      }
    } catch (Exception e) {
      throw ComponentUtils.wrapWithMetadata(this, e);
    } finally {
      mIsMounting = false;
      mRootHeightAnimation = null;
      mRootWidthAnimation = null;

      if (isDirtyMount) {
        lithoViewRef.onDirtyMountComplete();
        if (mLithoView == null) {
          ComponentsReporter.emitMessage(
              ComponentsReporter.LogLevel.WARNING,
              M_LITHO_VIEW_IS_NULL,
              "mLithoView is unexpectedly null");
        }
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
    final List<ScopedComponentInfo> scopedComponentInfos =
        layoutState.getScopedComponentInfosNeedingPreviousRenderData();
    applyPreviousRenderData(scopedComponentInfos);
  }

  void applyPreviousRenderData(@Nullable List<ScopedComponentInfo> scopedComponentInfos) {
    if (scopedComponentInfos == null || scopedComponentInfos.isEmpty()) {
      return;
    }

    if (mPreviousRenderState == null) {
      return;
    }

    mPreviousRenderState.applyPreviousRenderData(scopedComponentInfos);
  }

  private void recordRenderData(LayoutState layoutState) {
    final List<ScopedComponentInfo> scopedComponentInfos =
        layoutState.getScopedComponentInfosNeedingPreviousRenderData();
    if (scopedComponentInfos == null || scopedComponentInfos.isEmpty()) {
      return;
    }

    if (mPreviousRenderState == null) {
      mPreviousRenderState = new RenderState();
    }

    mPreviousRenderState.recordRenderData(scopedComponentInfos);
  }

  void detach() {
    assertMainThread();

    if (mIncrementalMountHelper != null && !mLithoView.skipNotifyVisibleBoundsChangedCalls()) {
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

    if (mLifecycleProvider != null && view != null) {
      final LithoLifecycle currentStatus = mLifecycleProvider.getLifecycleStatus();
      if (currentStatus == HINT_VISIBLE) {
        view.setVisibilityHintNonRecursive(true);
      }

      if (currentStatus == HINT_INVISIBLE) {
        view.setVisibilityHintNonRecursive(false);
      }
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

    if (view == null && mInAttach) {
      throw new RuntimeException("setting null LithoView while in attach");
    }

    mLithoView = view;
  }

  void clearLithoView() {
    assertMainThread();

    // Crash if the ComponentTree is mounted to a view.
    if (mIsAttached) {
      throw new IllegalStateException("Clearing the LithoView while the ComponentTree is attached");
    }

    if (mLifecycleProvider != null) {
      mLithoView.resetVisibilityHint();
    }

    if (mInAttach) {
      throw new RuntimeException("clearing LithoView while in attach");
    }

    mLithoView = null;
  }

  @UiThread
  @GuardedBy("this")
  private void promoteCommittedLayoutStateToUI() {
    if (mCommittedLayoutState == null) {
      throw new RuntimeException("Cannot promote null LayoutState!");
    }
    if (mCommittedLayoutState == mMainThreadLayoutState) {
      return;
    }
    mMainThreadLayoutState = mCommittedLayoutState;
    dispatchOnAttached();

    if (mLithoView != null) {
      mLithoView.setMountStateDirty();
    }
  }

  @UiThread
  @GuardedBy("this")
  private void dispatchOnAttached() {
    final @Nullable List<Attachable> attachables =
        mMainThreadLayoutState != null ? mMainThreadLayoutState.getAttachables() : null;
    if (mAttachDetachHandler != null) {
      mAttachDetachHandler.onAttached(attachables);
    } else if (attachables != null) {
      getOrCreateAttachDetachHandler().onAttached(attachables);
    }
  }

  void measure(int widthSpec, int heightSpec, int[] measureOutput, boolean forceLayout) {
    assertMainThread();

    mIsMeasuring = true;
    try {
      final boolean needsSyncLayout;
      synchronized (this) {
        if (DEBUG_LOGS) {
          debugLog(
              "StartMeasure",
              "WidthSpec: "
                  + View.MeasureSpec.toString(widthSpec)
                  + ", HeightSpec: "
                  + View.MeasureSpec.toString(heightSpec)
                  + ", isCompatibleWithCommittedLayout: "
                  + isCompatibleSpec(mCommittedLayoutState, widthSpec, heightSpec)
                  + ", isCompatibleWithMainThreadLayout: "
                  + isCompatibleComponentAndSpec(
                      mMainThreadLayoutState,
                      mRoot != null ? mRoot.getId() : INVALID_ID,
                      widthSpec,
                      heightSpec)
                  + ", hasSameSpecs: "
                  + (mMainThreadLayoutState != null
                      && mMainThreadLayoutState.getWidthSpec() == widthSpec
                      && mMainThreadLayoutState.getHeightSpec() == heightSpec));
        }

        if (mCommittedLayoutState != null
            && mCommittedLayoutState != mMainThreadLayoutState
            && isCompatibleSpec(mCommittedLayoutState, widthSpec, heightSpec)) {
          promoteCommittedLayoutStateToUI();
        }

        final boolean hasExactSameSpecs =
            mMainThreadLayoutState != null
                && mMainThreadLayoutState.getWidthSpec() == widthSpec
                && mMainThreadLayoutState.getHeightSpec() == heightSpec;
        final boolean hasSameRootAndEquivalentSpecs =
            isCompatibleComponentAndSpec(
                mMainThreadLayoutState,
                mRoot != null ? mRoot.getId() : INVALID_ID,
                widthSpec,
                heightSpec);
        if (hasExactSameSpecs || hasSameRootAndEquivalentSpecs) {
          measureOutput[0] = mMainThreadLayoutState.getWidth();
          measureOutput[1] = mMainThreadLayoutState.getHeight();
          needsSyncLayout = false;
        } else {
          needsSyncLayout = true;
        }
      }

      if (needsSyncLayout || forceLayout) {
        final Size output = new Size();
        setSizeSpecForMeasure(widthSpec, heightSpec, output, forceLayout);

        // It's possible we don't commit a layout or block on a future on another thread (which will
        // not immediately promote the committed layout state since that needs to happen on the main
        // thread). Ensure we have the latest LayoutState before exiting.
        synchronized (this) {
          if (mReleased) {
            throw new RuntimeException("Tree is released during measure!");
          }
          if (mCommittedLayoutState != mMainThreadLayoutState) {
            promoteCommittedLayoutStateToUI();
          }

          if (mMainThreadLayoutState != null) {
            measureOutput[0] = mMainThreadLayoutState.getWidth();
            measureOutput[1] = mMainThreadLayoutState.getHeight();
          } else {
            measureOutput[0] = output.width;
            measureOutput[1] = output.height;

            ComponentsReporter.emitMessage(
                ComponentsReporter.LogLevel.ERROR,
                "NullLayoutStateInMeasure",
                "Measure Specs: ["
                    + View.MeasureSpec.toString(widthSpec)
                    + ", "
                    + View.MeasureSpec.toString(heightSpec)
                    + "], Current Specs: ["
                    + View.MeasureSpec.toString(mWidthSpec)
                    + ", "
                    + View.MeasureSpec.toString(mHeightSpec)
                    + "], Output [W: "
                    + output.width
                    + ", H:"
                    + output.height
                    + "], Last Layout Source: "
                    + LayoutState.layoutSourceToString(mLastLayoutSource));
          }
        }
      } else {
        setSizeSpecForMeasureAsync(widthSpec, heightSpec);
      }
    } finally {
      mIsMeasuring = false;
    }

    if (DEBUG_LOGS) {
      debugLog(
          "FinishMeasure",
          "WidthSpec: "
              + View.MeasureSpec.toString(widthSpec)
              + ", HeightSpec: "
              + View.MeasureSpec.toString(heightSpec)
              + ", OutWidth: "
              + measureOutput[0]
              + ", OutHeight: "
              + measureOutput[1]);
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

  @Nullable
  RunnableHandler getMountContentPreallocationHandler() {
    return mPreAllocateMountContentHandler;
  }

  public boolean shouldReuseOutputs() {
    return mComponentsConfiguration.shouldReuseOutputs();
  }

  public boolean isReconciliationEnabled() {
    return isReconciliationEnabled;
  }

  public boolean isSplitStateHandlersEnabled() {
    return isSplitStateHandlersEnabled;
  }

  public boolean isReuseLastMeasuredNodeInComponentMeasureEnabled() {
    return isReuseLastMeasuredNodeInComponentMeasureEnabled;
  }

  public boolean shouldKeepLithoNodeAndLayoutResultTreeWithReconciliation() {
    return mComponentsConfiguration.keepLithoNodeAndLayoutResultTreeWithReconciliation();
  }

  public ErrorEventHandler getErrorEventHandler() {
    return mErrorEventHandler;
  }

  synchronized @Nullable Component getRoot() {
    return mRoot;
  }

  /**
   * Update the root component. This can happen in both attached and detached states. In each case
   * we will run a layout and then proxy a message to the main thread to cause a
   * relayout/invalidate.
   */
  public void setRoot(@Nullable Component root) {
    setRootAndSizeSpecAndWrapper(
        root,
        SIZE_UNINITIALIZED,
        SIZE_UNINITIALIZED,
        false /* isAsync */,
        null /* output */,
        CalculateLayoutSource.SET_ROOT_SYNC,
        INVALID_LAYOUT_VERSION,
        null,
        null);
  }

  /**
   * Similar to setRoot. This method allows setting a new root with cached TreeProps and
   * StateHandler. It is used to enable time-traveling through external editors such as Flipper
   *
   * @param selectedRevision the revision of the tree we're resetting to
   * @param root component to set the newState for
   * @param props the props of the tree
   * @param newState the cached state
   * @see DebugComponentTimeMachine
   * @see ComponentTree#getTimeline()
   */
  @UiThread
  synchronized void resetState(
      long selectedRevision, Component root, TreeProps props, TreeState newTreeState) {
    ThreadUtils.assertMainThread();
    mTreeState = newTreeState;
    mRootTreeProps = props;
    final DebugComponentTimeMachine.TreeRevisions timeline = mTimeline;
    if (timeline != null) {
      timeline.setSelected(selectedRevision);
    }

    setRootAndSizeSpecInternal(
        root,
        SIZE_UNINITIALIZED,
        SIZE_UNINITIALIZED,
        false /* isAsync */,
        null /* output */,
        CalculateLayoutSource.RELOAD_PREVIOUS_STATE,
        INVALID_LAYOUT_VERSION,
        null,
        null,
        false,
        true);
  }

  /**
   * The business logic for this method resides in DebugComponentTimeMachine. ComponentTree only
   * stores its own timeline
   *
   * @see DebugComponentTimeMachine
   */
  @Nullable
  DebugComponentTimeMachine.TreeRevisions getTimeline() {
    final DebugComponentTimeMachine.TreeRevisions timeline = mTimeline;
    return timeline != null ? timeline.shallowCopy() : null;
  }

  /**
   * The business logic for this method resides in DebugComponentTimeMachine. ComponentTree only
   * stores its own timeline
   *
   * @see DebugComponentTimeMachine
   */
  @GuardedBy("this")
  void appendTimeline(
      Component root,
      TreeState treeState,
      TreeProps props,
      @LayoutState.CalculateLayoutSource int source,
      @Nullable String attribution) {
    assertHoldsLock(this);
    if (mTimeline == null) {
      mTimeline =
          new DebugComponentTimeMachine.TreeRevisions(root, treeState, props, source, attribution);
    } else {
      mTimeline.setLatest(root, treeState, props, source, attribution);
    }
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
      } else if (mCommittedLayoutState != null) {
        toPrePopulate = mCommittedLayoutState;
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

    toPrePopulate.preAllocateMountContent(shouldPreallocatePerMountSpec);

    if (event != null) {
      logger.logPerfEvent(event);
    }
  }

  public void setRootAsync(@Nullable Component root) {
    setRootAndSizeSpecAndWrapper(
        root,
        SIZE_UNINITIALIZED,
        SIZE_UNINITIALIZED,
        true /* isAsync */,
        null /* output */,
        CalculateLayoutSource.SET_ROOT_ASYNC,
        INVALID_LAYOUT_VERSION,
        null,
        null);
  }

  @VisibleForTesting
  synchronized void updateStateLazy(String componentKey, StateUpdate stateUpdate) {
    updateStateLazy(componentKey, stateUpdate, false);
  }

  synchronized void updateStateLazy(
      String componentKey, StateUpdate stateUpdate, boolean isNestedTree) {
    if (mRoot == null) {
      return;
    }

    if (mTreeState != null) {
      mTreeState.queueStateUpdate(componentKey, stateUpdate, true, isNestedTree);
    }
  }

  /**
   * @return a StateContainer with lazy state updates applied. This may be the same container passed
   *     in if there were no updates to apply. This method won't mutate the passed container.
   */
  synchronized StateContainer applyLazyStateUpdatesForContainer(
      String componentKey, StateContainer container, boolean isNestedTree) {

    if (mRoot == null || mTreeState == null) {
      return container;
    }

    return mTreeState.applyLazyStateUpdatesForContainer(componentKey, container, isNestedTree);
  }

  @VisibleForTesting
  void updateStateSync(
      String componentKey,
      StateUpdate stateUpdate,
      String attribution,
      boolean isCreateLayoutInProgress) {
    updateStateSync(componentKey, stateUpdate, attribution, isCreateLayoutInProgress, false);
  }

  void updateStateSync(
      String componentKey,
      StateUpdate stateUpdate,
      String attribution,
      boolean isCreateLayoutInProgress,
      boolean isNestedTree) {
    synchronized (this) {
      if (mRoot == null) {
        return;
      }

      if (mTreeState != null) {
        mTreeState.queueStateUpdate(componentKey, stateUpdate, false, isNestedTree);
      }
    }

    ensureSyncStateUpdateRunnable(attribution, isCreateLayoutInProgress);
  }

  @VisibleForTesting
  void updateStateAsync(
      String componentKey,
      StateUpdate stateUpdate,
      String attribution,
      boolean isCreateLayoutInProgress) {
    updateStateAsync(componentKey, stateUpdate, attribution, isCreateLayoutInProgress, false);
  }

  void updateStateAsync(
      String componentKey,
      StateUpdate stateUpdate,
      String attribution,
      boolean isCreateLayoutInProgress,
      boolean isNestedTree) {
    if (!mIsAsyncUpdateStateEnabled) {
      throw new RuntimeException(
          "Triggering async state updates on this component tree is "
              + "disabled, use sync state updates.");
    }

    synchronized (this) {
      if (mRoot == null) {
        return;
      }

      if (mTreeState != null) {
        mTreeState.queueStateUpdate(componentKey, stateUpdate, false, isNestedTree);
      }
    }

    LithoStats.incrementComponentStateUpdateAsyncCount();
    onAsyncStateUpdateEnqueued(attribution, isCreateLayoutInProgress);
  }

  final void updateHookStateSync(
      String globalKey,
      HookUpdater updater,
      String attribution,
      boolean isCreateLayoutInProgress,
      boolean isNestedTree) {
    synchronized (this) {
      if (mRoot == null) {
        return;
      }

      if (mTreeState != null) {
        mTreeState.queueHookStateUpdate(globalKey, updater, isNestedTree);
      }
    }

    ensureSyncStateUpdateRunnable(attribution, isCreateLayoutInProgress);
  }

  final void updateHookStateAsync(
      String globalKey,
      HookUpdater updater,
      String attribution,
      boolean isCreateLayoutInProgress,
      boolean isNestedTree) {
    synchronized (this) {
      if (mRoot == null) {
        return;
      }

      if (mTreeState != null) {
        mTreeState.queueHookStateUpdate(globalKey, updater, isNestedTree);
      }
    }

    LithoStats.incrementComponentStateUpdateAsyncCount();
    onAsyncStateUpdateEnqueued(attribution, isCreateLayoutInProgress);
  }

  private void onAsyncStateUpdateEnqueued(String attribution, boolean isCreateLayoutInProgress) {
    if (mBatchedStateUpdatesStrategy == null
        || !mBatchedStateUpdatesStrategy.onAsyncStateUpdateEnqueued(
            attribution, isCreateLayoutInProgress)) {
      updateStateInternal(true, attribution, isCreateLayoutInProgress);
    }
  }

  private void ensureSyncStateUpdateRunnable(String attribution, boolean isCreateLayoutInProgress) {
    LithoStats.incrementComponentStateUpdateSyncCount();
    final Looper looper = Looper.myLooper();

    if (looper == null) {
      Log.w(
          TAG,
          "You cannot update state synchronously from a thread without a looper, "
              + "using the default background layout thread instead");
      synchronized (mUpdateStateSyncRunnableLock) {
        if (mUpdateStateSyncRunnable != null) {
          getResolveThreadHandler().remove(mUpdateStateSyncRunnable);
        }
        mUpdateStateSyncRunnable =
            new UpdateStateSyncRunnable(attribution, isCreateLayoutInProgress);

        String tag = EMPTY_STRING;
        if (mLayoutThreadHandler.isTracing()) {
          tag = "updateStateSyncNoLooper " + attribution;
        }
        getResolveThreadHandler().post(mUpdateStateSyncRunnable, tag);
      }
      return;
    }

    final WeakReference<RunnableHandler> handlerWr = sSyncStateUpdatesHandler.get();
    RunnableHandler handler = handlerWr != null ? handlerWr.get() : null;

    if (handler == null) {
      handler = new DefaultHandler(looper);
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

  void updateStateInternal(boolean isAsync, String attribution, boolean isCreateLayoutInProgress) {
    final @Nullable TreeProps rootTreeProps;

    synchronized (this) {
      if (mRoot == null) {
        return;
      }

      rootTreeProps = TreeProps.copy(mRootTreeProps);

      if (isCreateLayoutInProgress) {
        logStateUpdatesFromCreateLayout(attribution);
      }

      if (mBatchedStateUpdatesStrategy != null) {
        mBatchedStateUpdatesStrategy.onInternalStateUpdateStart();
      }
    }

    setRootAndSizeSpecInternal(
        mRoot,
        SIZE_UNINITIALIZED,
        SIZE_UNINITIALIZED,
        isAsync,
        null /*output */,
        isAsync
            ? CalculateLayoutSource.UPDATE_STATE_ASYNC
            : CalculateLayoutSource.UPDATE_STATE_SYNC,
        INVALID_LAYOUT_VERSION,
        attribution,
        rootTreeProps,
        isCreateLayoutInProgress,
        false);
  }

  /**
   * State updates can be triggered when layout creation is still in progress which causes an
   * infinite loop because state updates again create the layout. To prevent this we keep a track of
   * how many times consequently state updates was invoked from within layout creation. If this
   * crosses the threshold a runtime exception is thrown.
   *
   * @param attribution
   */
  @GuardedBy("this")
  private void logStateUpdatesFromCreateLayout(@Nullable String attribution) {
    if (++mStateUpdatesFromCreateLayoutCount == STATE_UPDATES_IN_LOOP_THRESHOLD) {
      String message =
          "State update loop during layout detected. Most recent attribution: "
              + attribution
              + ".\n"
              + "State updates were dispatched over 50 times during the current layout. "
              + "This happens most commonly when state updates are dispatched unconditionally from "
              + "the render method.";
      if (ComponentsConfiguration.isDebugModeEnabled
          || ComponentsConfiguration.crashIfExceedingStateUpdateThreshold) {
        throw new RuntimeException(message);
      } else {
        ComponentsReporter.emitMessage(
            ComponentsReporter.LogLevel.FATAL,
            STATE_UPDATES_IN_LOOP_EXCEED_THRESHOLD + attribution,
            message);
      }
    }
  }

  @Nullable
  synchronized TreeState getTreeState() {
    return mTreeState;
  }

  void recordEventHandler(ComponentContext scopedContext, EventHandler eventHandler) {
    mEventHandlersController.recordEventHandler(scopedContext.getGlobalKey(), eventHandler);
  }

  @GuardedBy("mEventTriggersContainer")
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
    if (mCommittedLayoutState != null) {
      mCommittedLayoutState.checkWorkingRangeAndDispatch(
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
    if (mCommittedLayoutState != null) {
      mCommittedLayoutState.dispatchOnExitRangeIfNeeded(mWorkingRangeStatusHandler);
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
  public void setSizeSpec(int widthSpec, int heightSpec, @Nullable Size output) {
    setRootAndSizeSpecInternal(
        null,
        widthSpec,
        heightSpec,
        false /* isAsync */,
        output /* output */,
        CalculateLayoutSource.SET_SIZE_SPEC_SYNC,
        INVALID_LAYOUT_VERSION,
        null,
        null,
        false,
        false);
  }

  public void setSizeSpecAsync(int widthSpec, int heightSpec) {
    setRootAndSizeSpecInternal(
        null,
        widthSpec,
        heightSpec,
        true /* isAsync */,
        null /* output */,
        CalculateLayoutSource.SET_SIZE_SPEC_ASYNC,
        INVALID_LAYOUT_VERSION,
        null,
        null,
        false,
        false);
  }

  private void setSizeSpecForMeasure(
      int widthSpec, int heightSpec, Size output, boolean forceLayout) {
    setRootAndSizeSpecInternal(
        null,
        widthSpec,
        heightSpec,
        false /* isAsync */,
        output /* output */,
        CalculateLayoutSource.MEASURE_SET_SIZE_SPEC,
        INVALID_LAYOUT_VERSION,
        null,
        null,
        false,
        forceLayout);
  }

  private void setSizeSpecForMeasureAsync(int widthSpec, int heightSpec) {
    setRootAndSizeSpecInternal(
        null,
        widthSpec,
        heightSpec,
        true /* isAsync */,
        null /* output */,
        CalculateLayoutSource.MEASURE_SET_SIZE_SPEC_ASYNC,
        INVALID_LAYOUT_VERSION,
        null,
        null,
        false,
        false);
  }

  /** Compute asynchronously a new layout with the given component root and sizes */
  public void setRootAndSizeSpecAsync(@Nullable Component root, int widthSpec, int heightSpec) {
    setRootAndSizeSpecAndWrapper(
        root,
        widthSpec,
        heightSpec,
        true /* isAsync */,
        null /* output */,
        CalculateLayoutSource.SET_ROOT_ASYNC,
        INVALID_LAYOUT_VERSION,
        null,
        null);
  }

  /**
   * Compute asynchronously a new layout with the given component root, sizes and stored TreeProps.
   */
  public void setRootAndSizeSpecAsync(
      @Nullable Component root, int widthSpec, int heightSpec, @Nullable TreeProps treeProps) {
    setRootAndSizeSpecAndWrapper(
        root,
        widthSpec,
        heightSpec,
        true /* isAsync */,
        null /* output */,
        CalculateLayoutSource.SET_ROOT_ASYNC,
        INVALID_LAYOUT_VERSION,
        null,
        treeProps);
  }

  /** Compute a new layout with the given component root and sizes */
  public void setRootAndSizeSpecSync(@Nullable Component root, int widthSpec, int heightSpec) {
    setRootAndSizeSpecAndWrapper(
        root,
        widthSpec,
        heightSpec,
        false /* isAsync */,
        null /* output */,
        CalculateLayoutSource.SET_ROOT_SYNC,
        INVALID_LAYOUT_VERSION,
        null,
        null);
  }

  public void setRootAndSizeSpecSync(
      @Nullable Component root, int widthSpec, int heightSpec, Size output) {
    setRootAndSizeSpecAndWrapper(
        root,
        widthSpec,
        heightSpec,
        false /* isAsync */,
        output,
        CalculateLayoutSource.SET_ROOT_SYNC,
        INVALID_LAYOUT_VERSION,
        null,
        null);
  }

  public void setRootAndSizeSpecSync(
      @Nullable Component root,
      int widthSpec,
      int heightSpec,
      Size output,
      @Nullable TreeProps treeProps) {
    setRootAndSizeSpecAndWrapper(
        root,
        widthSpec,
        heightSpec,
        false /* isAsync */,
        output,
        CalculateLayoutSource.SET_ROOT_SYNC,
        INVALID_LAYOUT_VERSION,
        null,
        treeProps);
  }

  public void setVersionedRootAndSizeSpec(
      @Nullable Component root,
      int widthSpec,
      int heightSpec,
      Size output,
      @Nullable TreeProps treeProps,
      int externalRootVersion) {
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

  public void setVersionedRootAndSizeSpecAsync(
      @Nullable Component root,
      int widthSpec,
      int heightSpec,
      Size output,
      @Nullable TreeProps treeProps,
      int externalRootVersion) {
    setRootAndSizeSpecAndWrapper(
        root,
        widthSpec,
        heightSpec,
        true /* isAsync */,
        output,
        CalculateLayoutSource.SET_ROOT_ASYNC,
        externalRootVersion,
        null,
        treeProps);
  }

  /**
   * @return the {@link LithoView} associated with this ComponentTree if any. Since this is modified
   *     on the main thread, it is racy to get the current LithoView off the main thread.
   */
  @Keep
  @UiThread
  public @Nullable LithoView getLithoView() {
    return mLithoView;
  }

  /**
   * Provides a new instance of the {@link TreeState} that is initialized with the TreeState held by
   * the ComponentTree.
   *
   * @return a copy of tree state instance help by ComponentTree
   */
  public synchronized TreeState acquireTreeState() {
    return mTreeState == null ? new TreeState() : new TreeState(mTreeState);
  }

  public static @Nullable LithoLifecycleProvider getLifecycleProvider(ComponentContext context) {
    return context.getComponentTree() == null
        ? null
        : context.getComponentTree().mLifecycleProvider;
  }

  public @Nullable LithoLifecycleProvider getLifecycleProvider() {
    return mLifecycleProvider;
  }

  /**
   * Creates a ComponentTree nested inside the ComponentTree of the provided parentContext. If the
   * parent ComponentTree is subscribed to a LithoLifecycleProvider, the nested ComponentTree will
   * also subscribe to a {@link SimpleNestedTreeLifecycleProvider} hooked with the parent's
   * lifecycle provider.
   *
   * @param parentContext context associated with the parent ComponentTree.
   * @param component root of the new nested ComponentTree.
   * @return builder for a nested ComponentTree.
   */
  public static ComponentTree.Builder createNestedComponentTree(
      final ComponentContext parentContext, Component component) {
    final ComponentTree parentComponentTree = parentContext.getComponentTree();
    if (parentComponentTree == null) {
      throw new IllegalStateException(
          "Cannot create a nested ComponentTree with a null parent ComponentTree.");
    }

    final SimpleNestedTreeLifecycleProvider lifecycleProvider =
        parentComponentTree.mLifecycleProvider == null
            ? null
            : new SimpleNestedTreeLifecycleProvider(parentComponentTree);

    return ComponentTree.create(
        ComponentContext.makeCopyForNestedTree(parentContext), component, lifecycleProvider);
  }

  synchronized @Nullable List<Transition> getStateUpdateTransitions() {
    return mTreeState == null ? null : mTreeState.getPendingStateUpdateTransitions();
  }

  /**
   * Takes ownership of the {@link RenderState} object from this ComponentTree - this allows the
   * RenderState to be persisted somewhere and then set back on another ComponentTree using the
   * {@link Builder}. See {@link RenderState} for more information on the purpose of this object.
   */
  @ThreadConfined(ThreadConfined.UI)
  public @Nullable RenderState consumePreviousRenderState() {
    final RenderState previousRenderState = mPreviousRenderState;

    mPreviousRenderState = null;
    return previousRenderState;
  }

  /**
   * @see #showTooltip(LithoTooltip, String, int, int)
   * @deprecated
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
    componentKeysToBounds = mMainThreadLayoutState.getComponentKeyToBounds();

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
   * Common internal entry-point for calls which are updating the root. If the provided root is
   * null, an EmptyComponent is used instead.
   */
  private void setRootAndSizeSpecAndWrapper(
      @Nullable Component root,
      int widthSpec,
      int heightSpec,
      boolean isAsync,
      @Nullable Size output,
      @CalculateLayoutSource int source,
      int externalRootVersion,
      @Nullable String extraAttribution,
      @Nullable TreeProps treeProps) {
    if (root == null) {
      root = new EmptyComponent();
    }

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
        false,
        false);
  }

  private void setRootAndSizeSpecInternal(
      @Nullable Component root,
      int widthSpec,
      int heightSpec,
      boolean isAsync,
      @Nullable Size output,
      @CalculateLayoutSource int source,
      int externalRootVersion,
      @Nullable String extraAttribution,
      @Nullable TreeProps treeProps,
      boolean isCreateLayoutInProgress,
      boolean forceLayout) {
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

      if (root != null) {
        if ((mTreeState != null && mTreeState.hasUncommittedUpdates())) {
          root = root.makeShallowCopyWithNewId();
        }
      }

      final boolean rootInitialized = root != null;
      final boolean treePropsInitialized = treeProps != null;
      final boolean widthSpecInitialized = widthSpec != SIZE_UNINITIALIZED;
      final boolean heightSpecInitialized = heightSpec != SIZE_UNINITIALIZED;
      final Component resolvedRoot = root != null ? root : mRoot;
      final int resolvedWidthSpec = widthSpecInitialized ? widthSpec : mWidthSpec;
      final int resolvedHeightSpec = heightSpecInitialized ? heightSpec : mHeightSpec;
      final LayoutState mostRecentLayoutState = mCommittedLayoutState;

      if (!forceLayout
          && resolvedRoot != null
          && mostRecentLayoutState != null
          && mostRecentLayoutState.isCompatibleComponentAndSpec(
              resolvedRoot.getId(), resolvedWidthSpec, resolvedHeightSpec)) {
        // The spec and the root haven't changed and we have a compatible LayoutState already
        // committed
        if (output != null) {
          output.height = mostRecentLayoutState.getHeight();
          output.width = mostRecentLayoutState.getWidth();
        }

        if (DEBUG_LOGS) {
          debugLog(
              "StartLayout",
              "Layout was compatible, not calculating a new one - Source: "
                  + layoutSourceToString(source)
                  + ", Extra: "
                  + extraAttribution
                  + ", WidthSpec: "
                  + View.MeasureSpec.toString(resolvedWidthSpec)
                  + ", HeightSpec: "
                  + View.MeasureSpec.toString(resolvedHeightSpec));
        }

        return;
      }

      if (DEBUG_LOGS) {
        debugLog(
            "StartLayout",
            "Calculating new layout - Source: "
                + layoutSourceToString(source)
                + ", Extra: "
                + extraAttribution
                + ", WidthSpec: "
                + View.MeasureSpec.toString(resolvedWidthSpec)
                + ", HeightSpec: "
                + View.MeasureSpec.toString(resolvedHeightSpec));
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

      if (forceLayout && mRoot != null) {
        mRoot = mRoot.makeShallowCopyWithNewId();
      }

      if (treePropsInitialized) {
        mRootTreeProps = treeProps;
      } else {
        treeProps = mRootTreeProps;
      }

      mLastLayoutSource = source;
    }

    if (isAsync && output != null) {
      throw new IllegalArgumentException(
          "The layout can't be calculated asynchronously if we need the Size back");
    }

    if (isResolveAndLayoutFuturesSplitEnabled) {
      startCalculationWithSplitFutures(
          isAsync, output, source, extraAttribution, isCreateLayoutInProgress);
      return;
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

  private void startCalculationWithSplitFutures(
      boolean isAsync,
      @Nullable Size output,
      @CalculateLayoutSource int source,
      @Nullable String extraAttribution,
      boolean isCreateLayoutInProgress) {
    final LithoResolutionResult resolutionResult;
    final Component root;
    final TreeProps treeProps;
    synchronized (this) {
      resolutionResult = mCommittedResolutionResult;
      root = mRoot;
      treeProps = mRootTreeProps;
    }

    // The current root and tree-props are the same as the committed resolution result. Therefore,
    // there is no need to calculate the resolution result again and we can proceed straight to
    // layout.
    if (resolutionResult != null
        && resolutionResult.component == root
        && resolutionResult.context.getTreeProps() == treeProps) {
      startLayoutCalculationWithSplitFutures(
          output, source, extraAttribution, isCreateLayoutInProgress, false);
      return;
    }

    if (isAsync) {
      synchronized (getResolveThreadHandlerLock()) {
        if (mResolveRunnable != null) {
          getResolveThreadHandler().remove(mResolveRunnable);
        }
        mResolveRunnable =
            new CalculateResolutionRunnable(source, extraAttribution, isCreateLayoutInProgress);

        String tag = EMPTY_STRING;
        if (getResolveThreadHandler().isTracing()) {
          tag = "doResolve ";
          if (mRoot != null) {
            tag = tag + mRoot.getSimpleName();
          }
        }
        getResolveThreadHandler().post(mResolveRunnable, tag);
      }
    } else {
      doResolve(output, source, extraAttribution, isCreateLayoutInProgress);
    }
  }

  private void doResolve(
      @Nullable Size output,
      @CalculateLayoutSource int source,
      @Nullable String extraAttribution,
      boolean isCreateLayoutInProgress) {

    synchronized (getResolveThreadHandlerLock()) {
      if (mResolveRunnable != null) {
        getResolveThreadHandler().remove(mResolveRunnable);
        mResolveRunnable = null;
      }
    }

    final int localResolveVersion;
    final Component root;
    final TreeState treeState;
    final ComponentContext context;
    final @Nullable LithoNode currentNode;
    final @Nullable TreeProps treeProps;

    synchronized (this) {
      if (mRoot == null) {
        return;
      }

      localResolveVersion = mNextResolveVersion++;
      root = mRoot;
      treeState = acquireTreeState();
      currentNode = mCommittedResolutionResult != null ? mCommittedResolutionResult.node : null;
      treeProps = mRootTreeProps;
      context = new ComponentContext(mContext, treeProps);
    }

    treeState.registerRenderState();

    final @Nullable LithoResolutionResult resolutionResult =
        calculateResolutionResult(
            context, root, treeState, localResolveVersion, currentNode, source);

    if (resolutionResult == null) {
      if (!isReleased() && isFromSyncLayout(source)) {
        final String errorMessage =
            "ResolutionResult is null, but only async operations can return a null ResolutionResult. Source: "
                + layoutSourceToString(source)
                + ", current thread: "
                + Thread.currentThread().getName()
                + ". Root: "
                + (root == null ? "null" : root.getSimpleName());
        throw new IllegalStateException(errorMessage);
      }

      return;
    }

    commitResolutionResult(resolutionResult, isCreateLayoutInProgress);

    startLayoutCalculationWithSplitFutures(
        output,
        source,
        extraAttribution,
        isCreateLayoutInProgress,
        // Don't post when using the same thread handler, as it could cause heavy delays
        !useSeparateThreadHandlersForResolveAndLayout);
  }

  @Nullable
  private LithoResolutionResult calculateResolutionResult(
      final ComponentContext context,
      final Component root,
      final TreeState treeState,
      final int resolveVersion,
      final @Nullable LithoNode currentNode,
      final @CalculateLayoutSource int source) {

    // TODO (T134774377) Implement perf event for RenderTreeFuture
    RenderTreeFuture localResolvedResultFuture =
        new RenderTreeFuture(context, root, treeState, currentNode, null, resolveVersion);

    final boolean waitingFromSyncLayout = isFromSyncLayout(source);

    // TODO (T136079256): Combine into common mechanism
    synchronized (mResolvedResultFutureLock) {
      boolean canReuse = false;
      for (int i = 0; i < mResolvedResultFutures.size(); i++) {
        final RenderTreeFuture runningRtf = mResolvedResultFutures.get(i);
        if (!runningRtf.isReleased()
            && runningRtf.isEquivalentTo(localResolvedResultFuture)
            && runningRtf.tryRegisterForResponse(waitingFromSyncLayout)) {
          // Use the latest LithoResolutionResult calculation if it's the same.
          localResolvedResultFuture = runningRtf;
          canReuse = true;
          break;
        }
      }
      if (!canReuse) {
        if (!localResolvedResultFuture.tryRegisterForResponse(waitingFromSyncLayout)) {
          throw new RuntimeException("Failed to register to local ResolvedResultFuture");
        }
        mResolvedResultFutures.add(localResolvedResultFuture);
      }
    }

    final LithoResolutionResult resolutionResult = localResolvedResultFuture.runAndGet(source);

    synchronized (mResolvedResultFutureLock) {
      localResolvedResultFuture.unregisterForResponse();

      // This future has finished executing, if no other threads were waiting for the response we
      // can remove it.
      if (localResolvedResultFuture.getWaitingCount() == 0) {
        localResolvedResultFuture.release();
        mResolvedResultFutures.remove(localResolvedResultFuture);
      }
    }

    if (root.getBuilderContext() != null
        && root.getBuilderContext() != mContext.getAndroidContext()) {
      final String message =
          "ComponentTree context is different from root builder context"
              + ", ComponentTree context="
              + mContext.getAndroidContext()
              + ", root builder context="
              + root.getBuilderContext()
              + ", root="
              + root.getSimpleName()
              + ", ContextTree="
              + ComponentTreeDumpingHelper.dumpContextTree(mContext);
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.ERROR,
          CT_CONTEXT_IS_DIFFERENT_FROM_ROOT_BUILDER_CONTEXT,
          message);
    }

    return resolutionResult;
  }

  private synchronized void commitResolutionResult(
      final LithoResolutionResult resolutionResult, final boolean isCreateLayoutInProgress) {
    if (mCommittedResolutionResult == null
        || mCommittedResolutionResult.version < resolutionResult.version) {
      mCommittedResolutionResult = resolutionResult;

      if (mTreeState != null) {
        mTreeState.commitRenderState(resolutionResult.treeState);
      }

      // Resetting the count after resolve calculation is complete and it was triggered during a
      // calculation
      if (!isCreateLayoutInProgress) {
        mStateUpdatesFromCreateLayoutCount = 0;
      }
    }

    if (mTreeState != null) {
      mTreeState.unregisterRenderState(resolutionResult.treeState);
    }
  }

  private void startLayoutCalculationWithSplitFutures(
      @Nullable Size output,
      @CalculateLayoutSource int source,
      @Nullable String extraAttribution,
      boolean isCreateLayoutInProgress,
      boolean forceSyncCalculation) {

    final boolean isAsync = !isFromSyncLayout(source);

    if (isAsync && output != null) {
      throw new IllegalStateException(
          "Cannot generate output for async layout calculation (source = " + source + ")");
    }

    if (isAsync && !forceSyncCalculation) {
      synchronized (mCurrentCalculateLayoutFutureRunnableLock) {
        if (mCurrentCalculateLayoutFutureRunnable != null) {
          mLayoutThreadHandler.remove(mCurrentCalculateLayoutFutureRunnable);
        }
        mCurrentCalculateLayoutFutureRunnable =
            new CalculateLayoutFutureRunnable(source, extraAttribution, isCreateLayoutInProgress);

        String tag = EMPTY_STRING;
        if (mLayoutThreadHandler.isTracing()) {
          tag = "doLayout ";
          if (mRoot != null) {
            tag = tag + mRoot.getSimpleName();
          }
        }
        mLayoutThreadHandler.post(mCurrentCalculateLayoutFutureRunnable, tag);
      }
    } else {
      doLayout(output, source, extraAttribution, isCreateLayoutInProgress);
    }
  }

  private void doLayout(
      @Nullable Size output,
      @CalculateLayoutSource int source,
      @Nullable String extraAttribution,
      boolean isCreateLayoutInProgress) {

    final @Nullable LithoResolutionResult resolutionResult;
    final int widthSpec;
    final int heightSpec;
    final int layoutVersion;
    final @Nullable LayoutState currentLayoutState;
    final @Nullable DiffNode currentDiffNode;
    final @Nullable TreeProps treeProps;

    synchronized (this) {
      resolutionResult = mCommittedResolutionResult;
      widthSpec = mWidthSpec;
      heightSpec = mHeightSpec;
      currentLayoutState = mCommittedLayoutState;
      currentDiffNode = currentLayoutState != null ? currentLayoutState.getDiffTree() : null;
      layoutVersion = mNextLayoutVersion++;
      treeProps = resolutionResult != null ? resolutionResult.context.getTreeProps() : null;
    }

    // No resolution result, do not proceed with layout.
    // If a resolution future is not already running, run it now.
    if (resolutionResult == null) {
      final boolean noCurrentResolutionsInProgress;

      synchronized (mResolvedResultFutureLock) {
        noCurrentResolutionsInProgress = mResolvedResultFutures.isEmpty();
      }

      if (noCurrentResolutionsInProgress) {
        startCalculationWithSplitFutures(
            !isFromSyncLayout(source), output, source, extraAttribution, isCreateLayoutInProgress);
      }

      return;
    }

    // No width / height spec, no point proceeding.
    if (widthSpec == SIZE_UNINITIALIZED && heightSpec == SIZE_UNINITIALIZED) {
      return;
    }

    resolutionResult.treeState.registerLayoutState();

    final LayoutState layoutState =
        runLayoutTreeFuture(
            source,
            resolutionResult,
            widthSpec,
            heightSpec,
            layoutVersion,
            currentLayoutState,
            currentDiffNode);

    if (layoutState == null) {
      if (!isReleased() && isFromSyncLayout(source)) {
        final String errorMessage =
            "LayoutState is null, but only async operations can return a null LayoutState. Source: "
                + layoutSourceToString(source)
                + ", current thread: "
                + Thread.currentThread().getName()
                + ". Root: "
                + (resolutionResult.component.getSimpleName());

        throw new IllegalStateException(errorMessage);
      }

      return;
    }

    if (output != null) {
      output.width = layoutState.getWidth();
      output.height = layoutState.getHeight();
    }

    commitLayoutState(
        layoutState,
        layoutVersion,
        source,
        extraAttribution,
        isCreateLayoutInProgress,
        treeProps,
        resolutionResult.component);

    LithoStats.incrementComponentCalculateLayoutCount();
    if (ThreadUtils.isMainThread()) {
      LithoStats.incrementComponentCalculateLayoutOnUICount();
    }
  }

  @Nullable
  private LayoutState runLayoutTreeFuture(
      final @CalculateLayoutSource int source,
      final LithoResolutionResult resolutionResult,
      int widthSpec,
      int heightSpec,
      int version,
      @Nullable LayoutState currentLayoutState,
      @Nullable DiffNode diffNode) {

    synchronized (mCurrentCalculateLayoutFutureRunnableLock) {
      if (mCurrentCalculateLayoutFutureRunnable != null) {
        mLayoutThreadHandler.remove(mCurrentCalculateLayoutFutureRunnable);
        mCurrentCalculateLayoutFutureRunnable = null;
      }
    }

    final boolean isSync = isFromSyncLayout(source);

    // TODO (T134774377) Implement perf event for LayoutTreeFuture
    LayoutTreeFuture layoutTreeFuture =
        new LayoutTreeFuture(
            resolutionResult,
            currentLayoutState,
            diffNode,
            null,
            widthSpec,
            heightSpec,
            mId,
            version);

    // TODO (T136079256): Combine into common mechanism
    synchronized (mLayoutStateFutureLock) {
      boolean isReusingFuture = false;

      for (LayoutTreeFuture runningLtf : mLayoutTreeFutures) {
        if (!runningLtf.isReleased() && runningLtf.isEquivalentTo(layoutTreeFuture)) {
          if (isSync) {
            // We are running sync now. Grab and wait on the current running one.
            layoutTreeFuture = runningLtf;
            isReusingFuture = true;
            break;
          } else {
            // An LTF is already running with the exact same params, no need to calculate async.
            return null;
          }
        }
      }

      if (!isReusingFuture) {
        mLayoutTreeFutures.add(layoutTreeFuture);
      }

      layoutTreeFuture.tryRegisterForResponse(isSync);
    }

    final LayoutState layoutState = layoutTreeFuture.runAndGet(source);

    synchronized (mLayoutStateFutureLock) {
      layoutTreeFuture.unregisterForResponse();

      if (layoutTreeFuture.getWaitingCount() == 0) {
        layoutTreeFuture.release();
        mLayoutTreeFutures.remove(layoutTreeFuture);
      }
    }

    return layoutState;
  }

  private void commitLayoutState(
      final LayoutState layoutState,
      final int layoutVersion,
      final @CalculateLayoutSource int source,
      final @Nullable String extraAttribution,
      final boolean isCreateLayoutInProgress,
      final @Nullable TreeProps treeProps,
      final Component rootComponent) {
    List<ScopedComponentInfo> scopedComponentInfos = null;

    int rootWidth = 0;
    int rootHeight = 0;
    boolean committedNewLayout = false;
    synchronized (this) {
      // We don't want to compute, layout, or reduce trees while holding a lock. However this means
      // that another thread could compute a layout and commit it before we get to this point. To
      // handle this, we make sure that the committed setRootId is only ever increased, meaning
      // we only go "forward in time" and will eventually get to the latest layout.
      // TODO(t66287929): Remove isCommitted check by only allowing one LayoutStateFuture at a time
      if (layoutVersion > mCommittedLayoutVersion
          && !layoutState.isCommitted()
          && isCompatibleSpec(layoutState, mWidthSpec, mHeightSpec)) {
        mCommittedLayoutVersion = layoutVersion;
        mCommittedLayoutState = layoutState;
        layoutState.markCommitted();
        committedNewLayout = true;
      }

      if (DEBUG_LOGS) {
        logFinishLayout(source, extraAttribution, layoutState, committedNewLayout);
      }

      final TreeState localTreeState = layoutState.consumeTreeState();
      if (committedNewLayout) {
        scopedComponentInfos = layoutState.consumeScopedComponentInfos();
        if (localTreeState != null) {
          final TreeState treeState = mTreeState;
          if (treeState != null) { // we could have been released
            if (ComponentsConfiguration.isTimelineEnabled) {
              DebugComponentTimeMachine.saveTimelineSnapshot(
                  this, rootComponent, treeState, treeProps, source, extraAttribution);
            }

            if (isResolveAndLayoutFuturesSplitEnabled) {
              // Render state was committed during resolve
              treeState.commitLayoutState(localTreeState);
            } else {
              treeState.commitRenderState(localTreeState);
              if (isSplitStateHandlersEnabled()) {
                treeState.commitLayoutState(localTreeState);
              }
            }
          }
        }

        if (mMeasureListeners != null) {
          rootWidth = layoutState.getWidth();
          rootHeight = layoutState.getHeight();
        }

        bindHandlesToComponentTree(this, layoutState);
      }

      if (mTreeState != null && localTreeState != null) {
        if (isResolveAndLayoutFuturesSplitEnabled) {
          // Render state was unregistered after resolve
          mTreeState.unregisterLayoutState(localTreeState);
        } else {
          mTreeState.unregisterRenderState(localTreeState);
          if (isSplitStateHandlersEnabled()) {
            mTreeState.unregisterLayoutState(localTreeState);
          }
        }
      }

      // Resetting the count after layout calculation is complete and it was triggered from within
      // layout creation
      if (!isCreateLayoutInProgress) {
        mStateUpdatesFromCreateLayoutCount = 0;
      }
    }

    if (committedNewLayout) {
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
    }

    if (scopedComponentInfos != null) {
      bindEventAndTriggerHandlers(scopedComponentInfos);
    }

    if (committedNewLayout) {
      postBackgroundLayoutStateUpdated();
    }

    if (mPreAllocateMountContentHandler != null) {
      mPreAllocateMountContentHandler.remove(mPreAllocateMountContentRunnable);

      String tag = EMPTY_STRING;
      if (mPreAllocateMountContentHandler.isTracing()) {
        tag = "preallocateLayout ";
        if (rootComponent != null) {
          tag = tag + rootComponent.getSimpleName();
        }
      }
      mPreAllocateMountContentHandler.post(mPreAllocateMountContentRunnable, tag);
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
    final int localLayoutVersion;
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
      if (isCompatibleComponentAndSpec(mCommittedLayoutState)) {
        if (output != null) {
          output.width = mCommittedLayoutState.getWidth();
          output.height = mCommittedLayoutState.getHeight();
        }
        return;
      }

      widthSpec = mWidthSpec;
      heightSpec = mHeightSpec;
      root = mRoot;
      localLayoutVersion = mNextLayoutVersion++;
    }

    final LayoutState localLayoutState =
        calculateLayoutState(
            mContext,
            root,
            widthSpec,
            heightSpec,
            localLayoutVersion,
            mIsLayoutDiffingEnabled,
            treeProps,
            source,
            extraAttribution);

    if (localLayoutState == null) {
      if (!isReleased()
          && isFromSyncLayout(source)
          && !mComponentsConfiguration.getUseCancelableLayoutFutures()) {
        final String errorMessage =
            "LayoutState is null, but only async operations can return a null LayoutState. Source: "
                + layoutSourceToString(source)
                + ", current thread: "
                + Thread.currentThread().getName()
                + ". Root: "
                + (mRoot == null ? "null" : mRoot.getSimpleName())
                + ". Interruptible layouts: "
                + mMoveLayoutsBetweenThreads;

        if (mComponentsConfiguration.getIgnoreNullLayoutStateError()) {
          ComponentsReporter.emitMessage(
              ComponentsReporter.LogLevel.ERROR, "ComponentTree:LayoutStateNull", errorMessage);
        } else {
          throw new IllegalStateException(errorMessage);
        }
      }

      return;
    }

    if (output != null) {
      output.width = localLayoutState.getWidth();
      output.height = localLayoutState.getHeight();
    }

    commitLayoutState(
        localLayoutState,
        localLayoutVersion,
        source,
        extraAttribution,
        isCreateLayoutInProgress,
        treeProps,
        root);
  }

  private void bindEventAndTriggerHandlers(final List<ScopedComponentInfo> scopedComponentInfos) {

    synchronized (mEventTriggersContainer) {
      clearUnusedTriggerHandlers();
      for (ScopedComponentInfo scopedComponentInfo : scopedComponentInfos) {
        final ComponentContext scopedContext = scopedComponentInfo.getContext();
        final Component component = scopedComponentInfo.getComponent();
        if (component instanceof SpecGeneratedComponent) {
          mEventHandlersController.bindEventHandlers(
              scopedContext, component, scopedContext.getGlobalKey());
          ((SpecGeneratedComponent) component)
              .recordEventTrigger(scopedContext, mEventTriggersContainer);
        }
      }
    }

    mEventHandlersController.clearUnusedEventHandlers();
  }

  /**
   * Transfers mCommittedLayoutState to mMainThreadLayoutState. This will proxy to the main thread
   * if necessary.
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

  private void logFinishLayout(
      int source,
      @Nullable String extraAttribution,
      LayoutState localLayoutState,
      boolean committedNewLayout) {
    final String message = committedNewLayout ? "Committed layout" : "Did NOT commit layout";
    debugLog(
        "FinishLayout",
        message
            + " - Source: "
            + layoutSourceToString(source)
            + ", Extra: "
            + extraAttribution
            + ", WidthSpec: "
            + View.MeasureSpec.toString(localLayoutState.getWidthSpec())
            + ", HeightSpec: "
            + View.MeasureSpec.toString(localLayoutState.getHeightSpec())
            + ", Width: "
            + localLayoutState.getWidth()
            + ", Height: "
            + localLayoutState.getHeight());
  }

  /**
   * The contract is that in order to release a ComponentTree, you must do so from the main thread.
   * Usually HostView will handle releasing, but if you never attach to a host view, then you should
   * call release yourself.
   */
  public void release() {
    assertMainThread();
    if (mIsMounting) {
      throw new IllegalStateException("Releasing a ComponentTree that is currently being mounted");
    }

    synchronized (this) {
      if (mBatchedStateUpdatesStrategy != null) {
        mBatchedStateUpdatesStrategy.release();
      }

      mMainThreadHandler.remove(mBackgroundLayoutStateUpdateRunnable);

      if (isResolveAndLayoutFuturesSplitEnabled) {
        synchronized (getResolveThreadHandlerLock()) {
          if (mResolveRunnable != null) {
            getResolveThreadHandler().remove(mResolveRunnable);
            mResolveRunnable = null;
          }
        }

        synchronized (mCurrentCalculateLayoutFutureRunnableLock) {
          if (mCurrentCalculateLayoutFutureRunnable != null) {
            mLayoutThreadHandler.remove(mCurrentCalculateLayoutFutureRunnable);
            mCurrentCalculateLayoutFutureRunnable = null;
          }
        }
      } else {
        synchronized (mCurrentCalculateLayoutRunnableLock) {
          if (mCurrentCalculateLayoutRunnable != null) {
            mLayoutThreadHandler.remove(mCurrentCalculateLayoutRunnable);
            mCurrentCalculateLayoutRunnable = null;
          }
        }
      }

      synchronized (mUpdateStateSyncRunnableLock) {
        if (mUpdateStateSyncRunnable != null) {
          getResolveThreadHandler().remove(mUpdateStateSyncRunnable);
          mUpdateStateSyncRunnable = null;
        }
      }

      if (isResolveAndLayoutFuturesSplitEnabled) {
        synchronized (mResolvedResultFutureLock) {
          for (RenderTreeFuture rtf : mResolvedResultFutures) {
            rtf.release();
          }

          mResolvedResultFutures.clear();
        }

        synchronized (mLayoutStateFutureLock) {
          for (LayoutTreeFuture ltf : mLayoutTreeFutures) {
            ltf.release();
          }

          mLayoutTreeFutures.clear();
        }
      } else {
        synchronized (mLayoutStateFutureLock) {
          for (int i = 0; i < mLayoutStateFutures.size(); i++) {
            mLayoutStateFutures.get(i).release();
          }

          mLayoutStateFutures.clear();
        }
      }

      if (mPreAllocateMountContentHandler != null) {
        mPreAllocateMountContentHandler.remove(mPreAllocateMountContentRunnable);
      }

      if (mRoot != null) {
        mReleasedComponent = mRoot.getSimpleName();
      }
      if (mLithoView != null) {
        mLithoView.setComponentTree(null);
      }
      mReleased = true;
      mRoot = null;

      // Clear mWorkingRangeStatusHandler before releasing LayoutState because we need them to help
      // dispatch OnExitRange events.
      clearWorkingRangeStatusHandler();

      mMainThreadLayoutState = null;
      mCommittedLayoutState = null;
      mTreeState = null;
      mPreviousRenderState = null;
      mMeasureListeners = null;
      mCommittedResolutionResult = null;
    }

    if (mAttachDetachHandler != null) {
      // Execute detached callbacks if necessary.
      mAttachDetachHandler.onDetached();
    }

    if (mOnReleaseListeners != null) {
      for (OnReleaseListener listener : mOnReleaseListeners) {
        listener.onReleased();
      }
    }

    synchronized (mEventTriggersContainer) {
      clearUnusedTriggerHandlers();
    }
  }

  @GuardedBy("this")
  private boolean isCompatibleComponentAndSpec(@Nullable LayoutState layoutState) {
    assertHoldsLock(this);

    return mRoot != null
        && isCompatibleComponentAndSpec(layoutState, mRoot.getId(), mWidthSpec, mHeightSpec);
  }

  @GuardedBy("this")
  private boolean hasSizeSpec() {
    assertHoldsLock(this);

    return mWidthSpec != SIZE_UNINITIALIZED && mHeightSpec != SIZE_UNINITIALIZED;
  }

  public synchronized @Nullable String getSimpleName() {
    return mRoot == null ? null : mRoot.getSimpleName();
  }

  synchronized @Nullable Object getCachedValue(Object cachedValueInputs, boolean isNestedTree) {
    if (mReleased || mTreeState == null) {
      return null;
    }
    return mTreeState.getCachedValue(cachedValueInputs, isNestedTree);
  }

  @VisibleForTesting
  @Nullable
  AttachDetachHandler getAttachDetachHandler() {
    return mAttachDetachHandler;
  }

  synchronized void putCachedValue(
      Object cachedValueInputs, Object cachedValue, boolean isNestedTree) {
    if (mReleased || mTreeState == null) {
      return;
    }
    mTreeState.putCachedValue(cachedValueInputs, cachedValue, isNestedTree);
  }

  public static synchronized Looper getDefaultLayoutThreadLooper() {
    if (sDefaultLayoutThreadLooper == null) {
      final HandlerThread defaultThread =
          new HandlerThread(DEFAULT_LAYOUT_THREAD_NAME, DEFAULT_BACKGROUND_THREAD_PRIORITY);
      defaultThread.start();
      sDefaultLayoutThreadLooper = defaultThread.getLooper();
    }

    return sDefaultLayoutThreadLooper;
  }

  private static synchronized Looper getDefaultResolveThreadLooper() {
    if (sDefaultResolveThreadLooper == null) {
      final HandlerThread defaultThread =
          new HandlerThread(DEFAULT_RESOLVE_THREAD_NAME, DEFAULT_BACKGROUND_THREAD_PRIORITY);
      defaultThread.start();
      sDefaultResolveThreadLooper = defaultThread.getLooper();
    }

    return sDefaultResolveThreadLooper;
  }

  private static boolean isCompatibleSpec(
      @Nullable LayoutState layoutState, int widthSpec, int heightSpec) {
    return layoutState != null
        && layoutState.isCompatibleSpec(widthSpec, heightSpec)
        && layoutState.isCompatibleAccessibility();
  }

  private static boolean isCompatibleComponentAndSpec(
      @Nullable LayoutState layoutState, int componentId, int widthSpec, int heightSpec) {
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
  private @Nullable ComponentsLogger getContextLogger() {
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
    if (!mComponentsConfiguration.getUseCancelableLayoutFutures()) {
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.ERROR,
          TAG,
          "Cancelling layouts for a ComponentTree with useCancelableLayoutFutures set to false is a no-op.");
      return;
    }

    if (isResolveAndLayoutFuturesSplitEnabled) {
      synchronized (mResolvedResultFutureLock) {
        for (RenderTreeFuture rtf : mResolvedResultFutures) {
          rtf.release();
        }
      }

      synchronized (mLayoutStateFutureLock) {
        for (LayoutTreeFuture ltf : mLayoutTreeFutures) {
          ltf.release();
        }
      }
    } else {
      synchronized (mLayoutStateFutureLock) {
        for (int i = 0, size = mLayoutStateFutures.size(); i < size; i++) {
          mLayoutStateFutures.get(i).release();
        }
      }
    }

    if (ThreadUtils.isMainThread()) {
      release();
    } else {
      mMainThreadHandler.post(
          new Runnable() {
            @Override
            public void run() {
              release();
            }
          },
          "Release");
    }
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
    final boolean waitingFromSyncLayout = isFromSyncLayout(source);

    // TODO (T136079256): Combine into common mechanism
    synchronized (mLayoutStateFutureLock) {
      boolean canReuse = false;
      for (int i = 0; i < mLayoutStateFutures.size(); i++) {
        final LayoutStateFuture runningLsf = mLayoutStateFutures.get(i);
        if (!runningLsf.isReleased()
            && runningLsf.isEquivalentTo(localLayoutStateFuture)
            && runningLsf.tryRegisterForResponse(waitingFromSyncLayout)) {
          // Use the latest LayoutState calculation if it's the same.
          localLayoutStateFuture = runningLsf;
          canReuse = true;
          break;
        }
      }
      if (!canReuse) {
        if (!localLayoutStateFuture.tryRegisterForResponse(waitingFromSyncLayout)) {
          throw new RuntimeException("Failed to register to localLayoutState");
        }
        mLayoutStateFutures.add(localLayoutStateFuture);
      }
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

    if (root.getBuilderContext() != null
        && root.getBuilderContext() != context.getAndroidContext()) {
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

  @UiThread
  private AttachDetachHandler getOrCreateAttachDetachHandler() {
    AttachDetachHandler handler = mAttachDetachHandler;
    if (handler == null) {
      handler = new AttachDetachHandler();
      mAttachDetachHandler = handler;
    }
    return handler;
  }

  void addOnReleaseListener(OnReleaseListener onReleaseListener) {
    assertMainThread();
    if (mOnReleaseListeners == null) {
      mOnReleaseListeners = new ArrayList<>();
    }
    mOnReleaseListeners.add(onReleaseListener);
  }

  void removeOnReleaseListener(OnReleaseListener onReleaseListener) {
    assertMainThread();
    if (mOnReleaseListeners != null) {
      mOnReleaseListeners.remove(onReleaseListener);
    }
  }

  private void debugLog(String eventName, String info) {
    if (DEBUG_LOGS) {
      android.util.Log.d(
          "ComponentTreeDebug",
          "("
              + hashCode()
              + ") ["
              + eventName
              + " - Root: "
              + (mRoot != null ? mRoot.getSimpleName() : null)
              + "] "
              + info);
    }
  }

  private static void bindHandlesToComponentTree(
      ComponentTree componentTree, LayoutState layoutState) {
    for (Handle handle : layoutState.getComponentHandles()) {
      handle.setComponentTree(componentTree);
    }
  }

  class LayoutStateFuture extends TreeFuture<LayoutState> {
    private final Object loggerMutex = new Object();
    private final ComponentContext context;
    private final Component root;
    private final int widthSpec;
    private final int heightSpec;
    private final boolean diffingEnabled;
    private final @Nullable TreeProps treeProps;
    private final int layoutVersion;
    @CalculateLayoutSource private final int source;
    private final String extraAttribution;
    @Nullable PerfEvent logFutureTaskGetWaiting = null;

    private LayoutStateFuture(
        final ComponentContext context,
        final Component root,
        final int widthSpec,
        final int heightSpec,
        int layoutVersion,
        final boolean diffingEnabled,
        final @Nullable TreeProps treeProps,
        @CalculateLayoutSource final int source,
        final @Nullable String extraAttribution) {
      super(
          ComponentTree.this.mMoveLayoutsBetweenThreads
              || ComponentTree.this.mComponentsConfiguration.getUseCancelableLayoutFutures());

      this.context = context;
      this.root = root;
      this.widthSpec = widthSpec;
      this.heightSpec = heightSpec;
      this.diffingEnabled = diffingEnabled;
      this.treeProps = treeProps;
      this.source = source;
      this.extraAttribution = extraAttribution;
      this.layoutVersion = layoutVersion;
    }

    @Override
    protected LayoutState calculate() {
      @Nullable
      LayoutStateFuture layoutStateFuture =
          mMoveOperationsBetweenThreads ? LayoutStateFuture.this : null;

      final ComponentContext contextWithStateHandler;
      final LayoutState previousLayoutState;

      final TreeState treeState;
      synchronized (ComponentTree.this) {
        treeState =
            ComponentTree.this.mTreeState == null
                ? new TreeState()
                : new TreeState(ComponentTree.this.mTreeState);

        previousLayoutState = mCommittedLayoutState;
        contextWithStateHandler = new ComponentContext(context, treeProps);

        treeState.registerRenderState();
        if (isSplitStateHandlersEnabled()) {
          treeState.registerLayoutState();
        }
      }

      final boolean isTracing = ComponentsSystrace.isTracing();
      if (isTracing) {
        if (extraAttribution != null) {
          ComponentsSystrace.beginSection("extra:" + extraAttribution);
        }
        ComponentsSystrace.beginSectionWithArgs(
                new StringBuilder("LayoutState.calculate_")
                    .append(root.getSimpleName())
                    .append("_")
                    .append(layoutSourceToString(source))
                    .toString())
            .arg("treeId", ComponentTree.this.mId)
            .arg("rootId", root.getId())
            .arg("widthSpec", SizeSpec.toString(widthSpec))
            .arg("heightSpec", SizeSpec.toString(heightSpec))
            .flush();
      }

      try {
        final ComponentsLogger logger = contextWithStateHandler.getLogger();
        final PerfEvent logLayoutState =
            logger != null
                ? LogTreePopulator.populatePerfEventFromLogger(
                    contextWithStateHandler,
                    logger,
                    logger.newPerformanceEvent(
                        contextWithStateHandler, EVENT_CALCULATE_LAYOUT_STATE))
                : null;
        if (logLayoutState != null) {
          logLayoutState.markerAnnotate(PARAM_COMPONENT, root.getSimpleName());
          logLayoutState.markerAnnotate(PARAM_LAYOUT_STATE_SOURCE, layoutSourceToString(source));
          logLayoutState.markerAnnotate(PARAM_IS_BACKGROUND_LAYOUT, !ThreadUtils.isMainThread());
          logLayoutState.markerAnnotate(
              PARAM_TREE_DIFF_ENABLED,
              previousLayoutState != null && previousLayoutState.getDiffTree() != null);
          logLayoutState.markerAnnotate(PARAM_ATTRIBUTION, extraAttribution);
          logLayoutState.markerAnnotate(PARAM_LAYOUT_VERSION, layoutVersion);
        }

        final LayoutState layoutState =
            LayoutState.calculate(
                contextWithStateHandler,
                root,
                layoutStateFuture,
                treeState,
                ComponentTree.this.mId,
                widthSpec,
                heightSpec,
                layoutVersion,
                diffingEnabled,
                previousLayoutState,
                logLayoutState);

        if (logLayoutState != null) {
          Preconditions.checkNotNull(logger).logPerfEvent(logLayoutState);
        }

        return layoutState;
      } finally {
        if (isTracing) {
          ComponentsSystrace.endSection();
          if (extraAttribution != null) {
            ComponentsSystrace.endSection();
          }
        }
      }
    }

    @Override
    protected Systracer.ArgsBuilder addSystraceArgs(Systracer.ArgsBuilder argsBuilder) {
      return argsBuilder.arg("treeId", ComponentTree.this.mId).arg("root", root.getSimpleName());
    }

    @Override
    protected void onWaitStart(boolean isTracing) {
      super.onWaitStart(isTracing);

      final ComponentsLogger logger = getContextLogger();

      synchronized (loggerMutex) {
        logFutureTaskGetWaiting =
            logger != null
                ? LogTreePopulator.populatePerfEventFromLogger(
                    mContext,
                    logger,
                    logger.newPerformanceEvent(mContext, EVENT_LAYOUT_STATE_FUTURE_GET_WAIT))
                : null;
      }
    }

    @Override
    protected void onWaitEnd(boolean isTracing, boolean errorOccurred) {
      super.onWaitEnd(isTracing, errorOccurred);

      synchronized (loggerMutex) {
        if (!errorOccurred && logFutureTaskGetWaiting != null) {
          logFutureTaskGetWaiting.markerPoint("FUTURE_TASK_END");
        }
      }
    }

    @Override
    protected void onGetEnd(boolean isTracing) {
      super.onGetEnd(isTracing);

      synchronized (loggerMutex) {
        if (logFutureTaskGetWaiting != null) {
          final boolean notRunningOnMyThread = mRunningThreadId.get() != Process.myTid();
          final boolean shouldWaitForResult = !mFutureTask.isDone() && notRunningOnMyThread;

          logFutureTaskGetWaiting.markerAnnotate(
              PARAM_LAYOUT_FUTURE_WAIT_FOR_RESULT, shouldWaitForResult);
          logFutureTaskGetWaiting.markerAnnotate(PARAM_IS_MAIN_THREAD, isMainThread());
          final ComponentsLogger logger = getContextLogger();

          if (logger != null) {
            logger.logPerfEvent(logFutureTaskGetWaiting);
          }
        }
      }
    }

    @Nullable
    @Override
    protected LayoutState resumeCalculation(LayoutState partialResult) {
      if (mReleased) {
        return null;
      }
      final LayoutState result =
          LayoutState.resumeCalculate(source, extraAttribution, partialResult);

      synchronized (LayoutStateFuture.this) {
        return mReleased ? null : result;
      }
    }

    @Override
    public boolean isEquivalentTo(TreeFuture that) {
      if (!(that instanceof LayoutStateFuture)) {
        return false;
      }

      if (this == that) {
        return true;
      }

      final LayoutStateFuture thatLsf = (LayoutStateFuture) that;

      if (widthSpec != thatLsf.widthSpec) {
        return false;
      }
      if (heightSpec != thatLsf.heightSpec) {
        return false;
      }
      if (!context.equals(thatLsf.context)) {
        return false;
      }
      if (root.getId() != thatLsf.root.getId()) {
        // We only care that the root id is the same since the root is shallow copied before
        // it's passed to us and will never be the same object.
        return false;
      }

      return true;
    }
  }

  public static int generateComponentTreeId() {
    return sIdGenerator.getAndIncrement();
  }

  @VisibleForTesting
  EventHandlersController getEventHandlersController() {
    return mEventHandlersController;
  }

  private class CalculateLayoutFutureRunnable extends ThreadTracingRunnable {
    private final @CalculateLayoutSource int mSource;
    private final @Nullable String mAttribution;
    private final boolean mIsCreateLayoutInProgress;

    public CalculateLayoutFutureRunnable(
        @CalculateLayoutSource int source,
        @Nullable String attribution,
        boolean isCreateLayoutInProgress) {
      mSource = source;
      mAttribution = attribution;
      mIsCreateLayoutInProgress = isCreateLayoutInProgress;
    }

    @Override
    public void tracedRun(ThreadTracingRunnable prevTracingRunnable) {
      doLayout(null, mSource, mAttribution, mIsCreateLayoutInProgress);
    }
  }

  private class CalculateResolutionRunnable extends ThreadTracingRunnable {

    private final @CalculateLayoutSource int mSource;
    private final @Nullable String mAttribution;
    private final boolean mIsCreateLayoutInProgress;

    public CalculateResolutionRunnable(
        @CalculateLayoutSource int source,
        @Nullable String attribution,
        boolean isCreateLayoutInProgress) {
      mSource = source;
      mAttribution = attribution;
      mIsCreateLayoutInProgress = isCreateLayoutInProgress;
    }

    @Override
    public void tracedRun(ThreadTracingRunnable prevTracingRunnable) {
      doResolve(null, mSource, mAttribution, mIsCreateLayoutInProgress);
    }
  }

  private class CalculateLayoutRunnable extends ThreadTracingRunnable {

    private final @CalculateLayoutSource int mSource;
    private final @Nullable TreeProps mTreeProps;
    private final @Nullable String mAttribution;
    private final boolean mIsCreateLayoutInProgress;

    public CalculateLayoutRunnable(
        @CalculateLayoutSource int source,
        @Nullable TreeProps treeProps,
        @Nullable String attribution,
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

    final @Nullable Rect currentVisibleArea;
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
    private Component root;

    // optional
    private ComponentsConfiguration componentsConfiguration =
        ComponentsConfiguration.getDefaultComponentsConfiguration();
    private boolean incrementalMountEnabled = true;
    private boolean isLayoutDiffingEnabled = true;
    private @Nullable RunnableHandler resolveThreadHandler;
    private RunnableHandler layoutThreadHandler;
    private @Nullable RunnableHandler preAllocateMountContentHandler;
    private @Nullable TreeState treeState;
    private RenderState previousRenderState;
    private boolean asyncStateUpdates = true;
    private int overrideComponentTreeId = -1;
    private boolean hasMounted = false;
    private boolean isFirstMount = false;
    private @Nullable MeasureListener mMeasureListener;
    private boolean shouldPreallocatePerMountSpec;
    private boolean isReconciliationEnabled = ComponentsConfiguration.isReconciliationEnabled;
    private ErrorEventHandler errorEventHandler = DefaultErrorEventHandler.INSTANCE;
    private boolean canInterruptAndMoveLayoutsBetweenThreads =
        ComponentsConfiguration.canInterruptAndMoveLayoutsBetweenThreads;

    private @Nullable String logTag;
    private @Nullable ComponentsLogger logger;
    private @Nullable LithoLifecycleProvider mLifecycleProvider;

    private @Nullable RenderUnitIdGenerator mRenderUnitIdGenerator;

    protected Builder(ComponentContext context) {
      this.context = context;
    }

    public Builder componentsConfiguration(ComponentsConfiguration componentsConfiguration) {
      if (componentsConfiguration != null) {
        this.componentsConfiguration = componentsConfiguration;
      }
      return this;
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

    public Builder withLithoLifecycleProvider(LithoLifecycleProvider lifecycleProvider) {
      this.mLifecycleProvider = lifecycleProvider;
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
        layoutThreadHandler = new DefaultHandler(looper);
      }

      return this;
    }

    public Builder resolveThreadLooper(Looper looper) {
      if (looper != null) {
        resolveThreadHandler = new DefaultHandler(looper);
      }

      return this;
    }

    /** Specify the handler for to preAllocateMountContent */
    public Builder preAllocateMountContentHandler(@Nullable RunnableHandler handler) {
      preAllocateMountContentHandler = handler;
      return this;
    }

    /** Enable Mount Content preallocation using the same thread we use to compute layouts */
    public Builder useDefaultHandlerForContentPreallocation() {
      preAllocateMountContentHandler = new DefaultHandler(getDefaultLayoutThreadLooper());
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
     * Specify the looper to use for running layouts on. Note that in rare cases layout must run on
     * the UI thread. For example, if you rotate the screen, we must measure on the UI thread. If
     * you don't specify a Looper here, the Components default Looper will be used.
     */
    public Builder layoutThreadHandler(RunnableHandler handler) {
      layoutThreadHandler = handler;
      return this;
    }

    public Builder resolveThreadHandler(RunnableHandler handler) {
      resolveThreadHandler = handler;
      return this;
    }

    /**
     * Specify an initial tree state object that the ComponentTree can use to set the current values
     * for states.
     */
    public Builder treeState(@Nullable TreeState treeState) {
      this.treeState = treeState;
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
     * This should not be used in majority of cases and should only be used when overriding previous
     * component tree id {@link ComponentTree.Builder#overrideComponentTreeId}
     *
     * @param prevComponentTree Previous ComponentTree to override the render unit id map
     */
    public Builder overrideRenderUnitIdMap(ComponentTree prevComponentTree) {
      this.mRenderUnitIdGenerator = prevComponentTree.getRenderUnitIdGenerator();
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

    public Builder isFirstMount(boolean isFirstMount) {
      this.isFirstMount = isFirstMount;
      return this;
    }

    public Builder measureListener(@Nullable MeasureListener measureListener) {
      this.mMeasureListener = measureListener;
      return this;
    }

    /** Sets if reconciliation is enabled */
    public Builder isReconciliationEnabled(boolean isEnabled) {
      this.isReconciliationEnabled = isEnabled;
      return this;
    }

    /**
     * Sets the custom ErrorEventHandler. Ignores null values to never overwrite Litho's
     * DefaultErrorEventHandler.
     */
    public Builder errorHandler(ErrorEventHandler errorEventHandler) {
      if (errorEventHandler != null) {
        this.errorEventHandler = errorEventHandler;
      }
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
        root = new EmptyComponent();
      }
      // TODO: T48569046 verify logTag when it will be set on CT directly
      if (logger != null && logTag == null) {
        logTag = root.getSimpleName();
      }

      return new ComponentTree(this);
    }
  }

  /**
   * In this approach we are attempting to start the layout calculation using the Choreographer
   * frame callbacks system.
   *
   * <p>Whenever the Choreographer receives a VSYNC signal, it starts a cycle to prepare the next
   * Frame. In this cycle is goes through 3 main phases: input handling, animation and traversals
   * (which layouts, measures and draws).
   *
   * <p>Fortunately, the Choreographer API provides a way to execute code during the next animation
   * phase of the processing.
   *
   * <p>With this knowledge, this new variant to batch state updates will schedule the layout
   * calculation start on the Choregrapher's animation phase. This way we can guarantee that all
   * states generated by input handling are properly enqueued before we start the layout
   * calculation.
   */
  class PostStateUpdateToChoreographerCallback implements BatchedStateUpdatesStrategy {

    private final AtomicReference<Choreographer> mMainChoreographer = new AtomicReference<>();

    private final AtomicInteger mEnqueuedUpdatesCount = new AtomicInteger(0);
    private final AtomicReference<String> mAttribution = new AtomicReference<>("");

    private final Choreographer.FrameCallback mFrameCallback =
        new Choreographer.FrameCallback() {
          @Override
          public void doFrame(long l) {
            // We retrieve the attribution before we reset the enqueuedUpdatesCount. The order here
            // matters, otherwise there could be an inconsistency in the attribution.
            String attribution = mAttribution.getAndSet("");

            if (mEnqueuedUpdatesCount.getAndSet(0) > 0) {
              updateStateInternal(
                  true,
                  attribution != null
                      ? attribution
                      : "<cls>" + getContext().getComponentScope().getClass().getName() + "</cls>",
                  mContext.isCreateLayoutInProgress());
            }
          }
        };

    private final Runnable mCreateMainChoreographerRunnable =
        () -> {
          mMainChoreographer.set(Choreographer.getInstance());

          /* in the case that we have to asynchronously initialize the choreographer, then we
          verify if we have enqueued state updates. If so, then we post a callback, because it
          is impossible that one has been set, even though we should be processing these updates.
          This is the case that the `ComponentTree` was created in a non Main Thread, and state updates were scheduled
          without the Choreographer being initialized yet. */
          if (mEnqueuedUpdatesCount.get() > 0) {
            mMainChoreographer.get().postFrameCallback(mFrameCallback);
          }
        };

    PostStateUpdateToChoreographerCallback() {
      initializeMainChoreographer();
    }
    /**
     * This method will guarantee that we will create a {@link Choreographer} instance linked to the
     * Main Thread {@link Looper}.
     *
     * <p>If the thread that is calling this method is the Main Thread, then it will initialize
     * immediately the {@link Choreographer}. Otherwise, it will schedule a initializion runnable,
     * that will execute in the main thread (via {@link #mCreateMainChoreographerRunnable})
     *
     * @return {@code true} when the main choreographer was initialized already, or {@code false}
     *     when the process was started, but the initialization has not occured yet.
     */
    private void initializeMainChoreographer() {
      if (mMainChoreographer.get() != null) return;

      if (Looper.myLooper() == Looper.getMainLooper()) {
        mMainChoreographer.set(Choreographer.getInstance());
      } else {
        scheduleChoreographerCreation();
      }
    }

    @Override
    public boolean onAsyncStateUpdateEnqueued(
        String attribution, boolean isCreateLayoutInProgress) {
      if (mEnqueuedUpdatesCount.getAndIncrement() == 0 && mMainChoreographer.get() != null) {
        mAttribution.set(attribution);
        mMainChoreographer.get().postFrameCallback(mFrameCallback);
      }

      return true;
    }

    @Override
    public void onInternalStateUpdateStart() {
      resetEnqueuedUpdates();
      removeFrameCallback();
    }

    @Override
    public void release() {
      resetEnqueuedUpdates();
      removeChoreographerCreation();
      removeFrameCallback();
    }

    private void resetEnqueuedUpdates() {
      mEnqueuedUpdatesCount.set(0);
    }

    private void removeChoreographerCreation() {
      mMainThreadHandler.remove(mCreateMainChoreographerRunnable);
    }

    private void removeFrameCallback() {
      if (mMainChoreographer.get() != null) {
        mMainChoreographer.get().removeFrameCallback(mFrameCallback);
      }
    }

    private void scheduleChoreographerCreation() {
      mMainThreadHandler.postAtFront(mCreateMainChoreographerRunnable, "Create Main Choreographer");
    }
  }
}
