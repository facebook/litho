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
import static com.facebook.litho.LayoutState.CalculateLayoutSource;
import static com.facebook.litho.LayoutState.layoutSourceToString;
import static com.facebook.litho.LithoLifecycleProvider.LithoLifecycle.HINT_INVISIBLE;
import static com.facebook.litho.LithoLifecycleProvider.LithoLifecycle.HINT_VISIBLE;
import static com.facebook.litho.StateContainer.StateUpdate;
import static com.facebook.litho.ThreadUtils.assertHoldsLock;
import static com.facebook.litho.ThreadUtils.assertMainThread;
import static com.facebook.litho.ThreadUtils.isMainThread;
import static com.facebook.litho.WorkContinuationInstrumenter.markFailure;
import static com.facebook.litho.WorkContinuationInstrumenter.onBeginWorkContinuation;
import static com.facebook.litho.WorkContinuationInstrumenter.onEndWorkContinuation;
import static com.facebook.litho.WorkContinuationInstrumenter.onOfferWorkForContinuation;
import static com.facebook.litho.config.ComponentsConfiguration.DEFAULT_BACKGROUND_THREAD_PRIORITY;
import static com.facebook.rendercore.instrumentation.HandlerInstrumenter.instrumentHandler;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.view.View;
import androidx.annotation.IntDef;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
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
import com.facebook.rendercore.instrumentation.FutureInstrumenter;
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
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicInteger;
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

  public static final int INVALID_ID = -1;
  private static final String INVALID_KEY = "LithoTooltipController:InvalidKey";
  private static final String INVALID_HANDLE = "LithoTooltipController:InvalidHandle";
  private static final String TAG = ComponentTree.class.getSimpleName();
  private static final int SIZE_UNINITIALIZED = -1;
  private static final String DEFAULT_LAYOUT_THREAD_NAME = "ComponentLayoutThread";
  private static final String EMPTY_STRING = "";
  private static final String REENTRANT_MOUNTS_EXCEED_MAX_ATTEMPTS =
      "ComponentTree:ReentrantMountsExceedMaxAttempts";
  private static final int REENTRANT_MOUNTS_MAX_ATTEMPTS = 25;
  private static final String CT_CONTEXT_IS_DIFFERENT_FROM_ROOT_BUILDER_CONTEXT =
      "ComponentTree:CTContextIsDifferentFromRootBuilderContext";

  public static final int STATE_UPDATES_IN_LOOP_THRESHOLD = 50;
  private static final String STATE_UPDATES_IN_LOOP_EXCEED_THRESHOLD =
      "ComponentTree:StateUpdatesWhenLayoutInProgressExceedsThreshold";
  private static boolean sBoostPerfLayoutStateFuture = false;
  @Nullable LithoLifecycleProvider mLifecycleProvider;
  private final boolean mAreTransitionsEnabled;
  private final boolean mReuseInternalNodes;
  private final boolean mIsLayoutCachingEnabled;
  private final boolean mUseInputOnlyInternalNodes;
  private final boolean mIgnoreNullLayoutStateError;

  @GuardedBy("this")
  private boolean mReleased;

  private String mReleasedComponent;
  private @Nullable volatile AttachDetachHandler mAttachDetachHandler;
  private @Nullable Deque<ReentrantMount> mReentrantMounts;

  private @Nullable DebugComponentTimeMachine.TreeRevisions mTimeline;

  @GuardedBy("this")
  private int mStateUpdatesFromCreateLayoutCount;

  private final @RecyclingMode int mRecyclingMode;

  private final InitialStateContainer mInitialStateContainer = new InitialStateContainer();

  private @Nullable LithoRenderUnitFactory mLithoRenderUnitFactory;

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

  public boolean isInputOnlyInternalNodeEnabled() {
    return mUseInputOnlyInternalNodes;
  }

  public boolean isInternalNodeReuseEnabled() {
    return mReuseInternalNodes;
  }

  public boolean isLayoutCachingEnabled() {
    return mIsLayoutCachingEnabled;
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
  private static volatile Looper sDefaultPreallocateMountContentThreadLooper;

  private static final ThreadLocal<WeakReference<RunnableHandler>> sSyncStateUpdatesHandler =
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

  @Nullable private RunnableHandler mPreAllocateMountContentHandler;

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
  private LithoView mLithoView;

  @ThreadConfined(ThreadConfined.UI)
  private RunnableHandler mLayoutThreadHandler;

  private RunnableHandler mMainThreadHandler = new DefaultHandler(Looper.getMainLooper());
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
  private volatile boolean mIsFirstMount;

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
  private int mNextLayoutVersion;

  @GuardedBy("this")
  private int mCommittedLayoutVersion = -1;

  @Nullable
  @GuardedBy("this")
  private TreeProps mRootTreeProps;

  @GuardedBy("this")
  private int mWidthSpec = SIZE_UNINITIALIZED;

  @GuardedBy("this")
  private int mHeightSpec = SIZE_UNINITIALIZED;

  @GuardedBy("this")
  private @CalculateLayoutSource int mLastLayoutSource = CalculateLayoutSource.NONE;

  // This is written to only by the main thread with the lock held, read from the main thread with
  // no lock held, or read from any other thread with the lock held.
  @Nullable private LayoutState mMainThreadLayoutState;

  @GuardedBy("this")
  @Nullable
  private LayoutState mCommittedLayoutState;

  @GuardedBy("this")
  private StateHandler mStateHandler;

  @ThreadConfined(ThreadConfined.UI)
  private RenderState mPreviousRenderState;

  protected final int mId;

  private final ErrorEventHandler mErrorEventHandler;

  private final EventHandlersController mEventHandlersController = new EventHandlersController();

  private final EventTriggersContainer mEventTriggersContainer = new EventTriggersContainer();

  @GuardedBy("this")
  private final WorkingRangeStatusHandler mWorkingRangeStatusHandler =
      new WorkingRangeStatusHandler();

  private final boolean useStatelessComponent;

  private final boolean shouldSkipShallowCopy;

  private final boolean isReconciliationEnabled;

  private final boolean shouldClearPrevContextWhenCommitted;

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
    mContext = ComponentContext.withComponentTree(builder.context, this);
    mRoot = builder.root;
    if (builder.mLifecycleProvider != null) {
      subscribeToLifecycleProvider(builder.mLifecycleProvider);
    }
    mIncrementalMountEnabled =
        builder.incrementalMountEnabled && !incrementalMountGloballyDisabled();
    mVisibilityProcessingEnabled = builder.visibilityProcessingEnabled;
    mIsLayoutDiffingEnabled = builder.isLayoutDiffingEnabled;
    mLayoutThreadHandler = builder.layoutThreadHandler;
    mShouldPreallocatePerMountSpec = builder.shouldPreallocatePerMountSpec;
    mPreAllocateMountContentHandler = builder.preAllocateMountContentHandler;
    mIsAsyncUpdateStateEnabled = builder.asyncStateUpdates;
    mHasMounted = builder.hasMounted;
    mIsFirstMount = builder.isFirstMount;
    addMeasureListener(builder.mMeasureListener);
    mUseCancelableLayoutFutures = builder.useCancelableLayoutFutures;
    mMoveLayoutsBetweenThreads = builder.canInterruptAndMoveLayoutsBetweenThreads;
    isReconciliationEnabled = builder.isReconciliationEnabled;
    mIgnoreNullLayoutStateError = builder.ignoreNullLayoutStateError;
    mForceAsyncStateUpdate = builder.shouldForceAsyncStateUpdate;
    mRecyclingMode = builder.recyclingMode;
    mErrorEventHandler = builder.errorEventHandler;
    mIsLayoutCachingEnabled = builder.isLayoutCachingEnabled;
    mReuseInternalNodes = mIsLayoutCachingEnabled || builder.reuseInternalNodes;
    mUseInputOnlyInternalNodes = mReuseInternalNodes || builder.useInputOnlyInternalNodes;
    useStatelessComponent = mReuseInternalNodes || builder.useStatelessComponent;
    shouldSkipShallowCopy = useStatelessComponent && builder.shouldSkipShallowCopy;
    shouldClearPrevContextWhenCommitted =
        useStatelessComponent && builder.shouldClearPrevContextWhenCommitted;

    final StateHandler builderStateHandler = builder.stateHandler;
    mStateHandler =
        builderStateHandler == null ? StateHandler.createNewInstance(null) : builderStateHandler;

    if (builder.previousRenderState != null) {
      mPreviousRenderState = builder.previousRenderState;
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
    mMainThreadHandler = instrumentHandler(mMainThreadHandler);
    mLayoutThreadHandler = ensureAndInstrumentLayoutThreadHandler(mLayoutThreadHandler);
    if (mPreAllocateMountContentHandler != null) {
      mPreAllocateMountContentHandler = instrumentHandler(mPreAllocateMountContentHandler);
    }
    mLogger = builder.logger;
    mLogTag = builder.logTag;
    mAreTransitionsEnabled = AnimationsDebug.areTransitionsEnabled(mContext.getAndroidContext());
  }

  private static boolean incrementalMountGloballyDisabled() {
    return ComponentsConfiguration.isIncrementalMountGloballyDisabled;
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
      /** Right now we don't care about testing this per surface, so we'll use the config value. */
      LithoPerfBooster booster = ComponentsConfiguration.perfBoosterFactory.acquireInstance();
      booster.markImportantThread(new Handler(sDefaultLayoutThreadLooper));
      sBoostPerfLayoutStateFuture = true;
    }
    return instrumentHandler(handler);
  }

  @Nullable
  @ThreadConfined(ThreadConfined.UI)
  LayoutState getMainThreadLayoutState() {
    return mMainThreadLayoutState;
  }

  @Nullable
  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  @GuardedBy("this")
  public LayoutState getCommittedLayoutState() {
    return mCommittedLayoutState;
  }

  @VisibleForTesting
  @Nullable
  public LayoutStateContext getLayoutStateContext() {
    final LayoutState layoutState = getCommittedLayoutState();
    return layoutState == null ? null : layoutState.getLayoutStateContext();
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

  public void setNewLayoutStateReadyListener(NewLayoutStateReadyListener listener) {
    mNewLayoutStateReadyListener = listener;
  }

  /**
   * Provide custom {@link RunnableHandler}. If null is provided default one will be used for
   * layouts.
   */
  @ThreadConfined(ThreadConfined.UI)
  public void updateLayoutThreadHandler(@Nullable RunnableHandler layoutThreadHandler) {
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
  public RunnableHandler getLayoutThreadHandler() {
    return (RunnableHandler) mLayoutThreadHandler;
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
        mMainThreadLayoutState.getWidth() != viewWidth
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

    if (mLithoView.getLocalVisibleRect(currentVisibleArea)
        // It might not be yet visible but animating from 0 height/width in which case we still
        // need
        // to mount them to trigger animation.
        || animatingRootBoundsFromZero(currentVisibleArea)) {
      mountComponent(currentVisibleArea, true);
    }
    // if false: no-op, doesn't have visible area, is not ready or not attached
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

    final boolean isDirtyMount = mLithoView.isMountStateDirty();

    mIsMounting = true;

    if (!mHasMounted) {
      mIsFirstMount = true;
      mHasMounted = true;
    }

    // currentVisibleArea null or empty => mount all
    try {
      mLithoView.mount(layoutState, currentVisibleArea, processVisibilityOutputs);
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
    final List<String> keys = layoutState.getComponentKeysNeedingPreviousRenderData();
    applyPreviousRenderData(components, keys);
  }

  void applyPreviousRenderData(
      @Nullable List<Component> components, @Nullable List<String> componentKeys) {
    if (components == null || components.isEmpty()) {
      return;
    }

    if (mPreviousRenderState == null) {
      return;
    }

    mPreviousRenderState.applyPreviousRenderData(components, componentKeys);
  }

  private void recordRenderData(LayoutState layoutState) {
    final List<Component> components = layoutState.getComponentsNeedingPreviousRenderData();
    final List<String> keys = layoutState.getComponentKeysNeedingPreviousRenderData();
    if (components == null || components.isEmpty()) {
      return;
    }

    if (mPreviousRenderState == null) {
      mPreviousRenderState = new RenderState();
    }

    mPreviousRenderState.recordRenderData(layoutState.getLayoutStateContext(), components, keys);
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

    mLithoView = view;
    mLithoRenderUnitFactory = view.getLithoRenderUnitFactory();
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

    mLithoView = null;
    mLithoRenderUnitFactory = null;
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
    final @Nullable List<Attachable> attachables = mMainThreadLayoutState.getAttachables();
    final LayoutStateContext layoutStateContext = mMainThreadLayoutState.getLayoutStateContext();
    if (mAttachDetachHandler != null) {
      mAttachDetachHandler.onAttached(layoutStateContext, attachables);
    } else if (attachables != null) {
      getOrCreateAttachDetachHandler().onAttached(layoutStateContext, attachables);
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
                      mMainThreadLayoutState, mRoot.getId(), widthSpec, heightSpec)
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
                mMainThreadLayoutState, mRoot.getId(), widthSpec, heightSpec);
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

  @Nullable
  LithoRenderUnitFactory getLithoRenderUnitFactory() {
    return mLithoRenderUnitFactory;
  }

  /** Returns the recycling mode. Please see {@link RecyclingMode for details of different modes} */
  public @RecyclingMode int getRecyclingMode() {
    return mRecyclingMode;
  }

  public boolean useStatelessComponent() {
    return useStatelessComponent;
  }

  public boolean shouldSkipShallowCopy() {
    return shouldSkipShallowCopy;
  }

  public boolean isReconciliationEnabled() {
    return isReconciliationEnabled;
  }

  public boolean shouldClearPrevContextWhenCommitted() {
    return shouldClearPrevContextWhenCommitted;
  }

  public ErrorEventHandler getErrorEventHandler() {
    return mErrorEventHandler;
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
      long selectedRevision, Component root, TreeProps props, StateHandler newState) {
    ThreadUtils.assertMainThread();
    mStateHandler = newState;
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
        -1,
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
      String rootGlobalKey,
      StateHandler stateHandler,
      TreeProps props,
      @LayoutState.CalculateLayoutSource int source,
      @Nullable String attribution) {
    assertHoldsLock(this);
    if (mTimeline == null) {
      mTimeline =
          new DebugComponentTimeMachine.TreeRevisions(
              root, rootGlobalKey, stateHandler, props, source, attribution);
    } else {
      mTimeline.setLatest(root, stateHandler, props, source, attribution);
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

    ensureSyncStateUpdateRunnable(attribution, isCreateLayoutInProgress);
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

  final void updateHookStateSync(
      String globalKey, HookUpdater updater, String attribution, boolean isCreateLayoutInProgress) {
    if (mForceAsyncStateUpdate && mIsAsyncUpdateStateEnabled) {
      updateHookStateAsync(globalKey, updater, attribution, isCreateLayoutInProgress);
      return;
    }

    synchronized (this) {
      if (mRoot == null) {
        return;
      }

      mStateHandler.queueHookStateUpdate(globalKey, updater);
    }

    ensureSyncStateUpdateRunnable(attribution, isCreateLayoutInProgress);
  }

  final void updateHookStateAsync(
      String globalKey, HookUpdater updater, String attribution, boolean isCreateLayoutInProgress) {
    synchronized (this) {
      if (mRoot == null) {
        return;
      }

      mStateHandler.queueHookStateUpdate(globalKey, updater);
    }

    LithoStats.incrementComponentStateUpdateAsyncCount();
    updateStateInternal(true, attribution, isCreateLayoutInProgress);
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
    if (ComponentsConfiguration.ignoreStateUpdatesForScreenshotTest) {
      return;
    }

    final Component root;
    final @Nullable TreeProps rootTreeProps;

    synchronized (this) {
      if (mRoot == null) {
        return;
      }

      root = shouldSkipShallowCopy() ? mRoot : mRoot.makeShallowCopy();
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
        isCreateLayoutInProgress,
        false);
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

  void recordEventHandler(ComponentContext scopedContext, EventHandler eventHandler) {
    mEventHandlersController.recordEventHandler(
        Component.getGlobalKey(scopedContext, scopedContext.getComponentScope()), eventHandler);
  }

  @GuardedBy("mEventTriggersContainer")
  private void bindTriggerHandler(ComponentContext scopedContext, Component component) {
    component.recordEventTrigger(scopedContext, mEventTriggersContainer);
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
        -1,
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
        -1,
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
        -1,
        null,
        null,
        false,
        false);
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

  /**
   * @return the {@link LithoView} associated with this ComponentTree if any. Since this is modified
   *     on the main thread, it is racy to get the current LithoView off the main thread.
   */
  @Keep
  @Nullable
  @UiThread
  public LithoView getLithoView() {
    return mLithoView;
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

  synchronized void consumeStateUpdateTransitions(
      List<Transition> outList, @Nullable String logContext) {
    if (mStateHandler != null) {
      mStateHandler.consumePendingStateUpdateTransitions(outList, logContext);
    }
  }

  synchronized @Nullable List<Transition> getStateUpdateTransitions() {
    final List<Transition> updateStateTransitions;
    if (mStateHandler != null && mStateHandler.getPendingStateUpdateTransitions() != null) {
      final Map<String, List<Transition>> pendingStateUpdateTransitions =
          mStateHandler.getPendingStateUpdateTransitions();
      updateStateTransitions = new ArrayList<>();
      for (List<Transition> pendingTransitions : pendingStateUpdateTransitions.values()) {
        updateStateTransitions.addAll(pendingTransitions);
      }
    } else {
      updateStateTransitions = null;
    }
    return updateStateTransitions;
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
   * rewrapping the component. TODO: (T81557408) Fix @Nullable issue
   */
  private void setRootAndSizeSpecAndWrapper(
      Component root,
      int widthSpec,
      int heightSpec,
      boolean isAsync,
      @Nullable Size output,
      @CalculateLayoutSource int source,
      int externalRootVersion,
      @Nullable String extraAttribution,
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
        false,
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
        false,
        false);
  }

  /* TODO: (T81557408) Fix @Nullable issue */
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
        if (mStateHandler.hasUncommittedUpdates()) {
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

      if (forceLayout) {
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
      root = shouldSkipShallowCopy() ? mRoot : mRoot.makeShallowCopy();
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
      if (!isReleased() && isFromSyncLayout(source) && !mUseCancelableLayoutFutures) {
        final String errorMessage =
            "LayoutState is null, but only async operations can return a null LayoutState. Source: "
                + layoutSourceToString(source)
                + ", current thread: "
                + Thread.currentThread().getName()
                + ". Root: "
                + (mRoot == null ? "null" : mRoot.getSimpleName())
                + ". Interruptible layouts: "
                + mMoveLayoutsBetweenThreads;

        if (mIgnoreNullLayoutStateError) {
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

    List<Component> components = null;
    List<String> componentKeys = null;
    @Nullable Map<String, Component> attachables = null;
    LayoutStateContext layoutStateContext = null;

    int rootWidth = 0;
    int rootHeight = 0;
    boolean committedNewLayout = false;
    synchronized (this) {
      // We don't want to compute, layout, or reduce trees while holding a lock. However this means
      // that another thread could compute a layout and commit it before we get to this point. To
      // handle this, we make sure that the committed setRootId is only ever increased, meaning
      // we only go "forward in time" and will eventually get to the latest layout.
      // TODO(t66287929): Remove isCommitted check by only allowing one LayoutStateFuture at a time
      if (localLayoutVersion > mCommittedLayoutVersion
          && !localLayoutState.isCommitted()
          && isCompatibleSpec(localLayoutState, mWidthSpec, mHeightSpec)) {
        mCommittedLayoutVersion = localLayoutVersion;
        mCommittedLayoutState = localLayoutState;
        localLayoutState.markCommitted();
        committedNewLayout = true;
      }

      if (DEBUG_LOGS) {
        logFinishLayout(source, extraAttribution, localLayoutState, committedNewLayout);
      }

      final StateHandler layoutStateStateHandler = localLayoutState.consumeStateHandler();
      if (committedNewLayout) {

        components = localLayoutState.consumeComponents();
        componentKeys = localLayoutState.consumeComponentKeys();

        if (layoutStateStateHandler != null) {
          final StateHandler stateHandler = mStateHandler;
          if (stateHandler != null) { // we could have been released
            if (ComponentsConfiguration.isTimelineEnabled) {
              final int indexOfRoot = components.indexOf(root);
              final String availableGlobalKey =
                  (indexOfRoot >= 0 && componentKeys != null)
                      ? componentKeys.get(indexOfRoot)
                      : null;
              final String globalKey = availableGlobalKey;
              DebugComponentTimeMachine.saveTimelineSnapshot(
                  this, root, globalKey, stateHandler, treeProps, source, extraAttribution);
            }
            stateHandler.commit(layoutStateStateHandler);
          }
        }

        if (mMeasureListeners != null) {
          rootWidth = localLayoutState.getWidth();
          rootHeight = localLayoutState.getHeight();
        }

        layoutStateContext = localLayoutState.getLayoutStateContext();
        layoutStateContext.prune();
        layoutStateContext.freeze();
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

    if (committedNewLayout) {
      final List<MeasureListener> measureListeners;
      synchronized (this) {
        measureListeners = mMeasureListeners == null ? null : new ArrayList<>(mMeasureListeners);
      }

      if (measureListeners != null) {
        for (MeasureListener measureListener : measureListeners) {
          measureListener.onSetRootAndSizeSpec(
              localLayoutVersion,
              rootWidth,
              rootHeight,
              source == CalculateLayoutSource.UPDATE_STATE_ASYNC
                  || source == CalculateLayoutSource.UPDATE_STATE_SYNC);
        }
      }
    }

    if (components != null) {
      bindEventAndTriggerHandlers(layoutStateContext, components, componentKeys, extraAttribution);
    }

    if (committedNewLayout) {
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

  private void bindEventAndTriggerHandlers(
      final LayoutStateContext layoutStateContext,
      final List<Component> components,
      final List<String> componentKeys,
      final @Nullable String extraAttribution) {

    if (!layoutStateContext.isFrozen()) {
      throw new IllegalStateException(
          "Using a LayoutStateContext before it was frozen.\n"
              + "extra-attribution="
              + extraAttribution);
    }

    synchronized (mEventTriggersContainer) {
      clearUnusedTriggerHandlers();

      for (int i = 0, size = components.size(); i < size; i++) {
        final Component component = components.get(i);
        final String globalKey = componentKeys.get(i);
        final ComponentContext scopedContext =
            component.getScopedContext(layoutStateContext, globalKey);
        mEventHandlersController.bindEventHandlers(scopedContext, component, globalKey);
        bindTriggerHandler(scopedContext, component);
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
      String extraAttribution,
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
      mStateHandler = null;
      mPreviousRenderState = null;
      mMeasureListeners = null;
    }

    if (mAttachDetachHandler != null) {
      // Execute detached callbacks if necessary.
      mAttachDetachHandler.onDetached();
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

  static synchronized Looper getDefaultLayoutThreadLooper() {
    if (sDefaultLayoutThreadLooper == null) {
      final HandlerThread defaultThread =
          new HandlerThread(DEFAULT_LAYOUT_THREAD_NAME, DEFAULT_BACKGROUND_THREAD_PRIORITY);
      defaultThread.start();
      sDefaultLayoutThreadLooper = defaultThread.getLooper();
    }

    return sDefaultLayoutThreadLooper;
  }

  private static boolean isCompatibleSpec(LayoutState layoutState, int widthSpec, int heightSpec) {
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
  @Nullable
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
    if (mAttachDetachHandler == null) {
      mAttachDetachHandler = new AttachDetachHandler();
    }
    return mAttachDetachHandler;
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
    private final RunnableFuture<LayoutState> futureTask;
    private final AtomicInteger refCount = new AtomicInteger(0);
    private final boolean isFromSyncLayout;
    private final int layoutVersion;
    private volatile boolean interruptRequested;
    @CalculateLayoutSource private final int source;
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
          FutureInstrumenter.instrument(
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
                  }),
              "LayoutStateFuture_calculateLayout");
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

        previousLayoutState = mCommittedLayoutState;
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
    LayoutState runAndGet(@CalculateLayoutSource final int source) {
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

  private static boolean isFromSyncLayout(@CalculateLayoutSource int source) {
    switch (source) {
      case CalculateLayoutSource.MEASURE_SET_SIZE_SPEC:
      case CalculateLayoutSource.SET_ROOT_SYNC:
      case CalculateLayoutSource.UPDATE_STATE_SYNC:
      case CalculateLayoutSource.SET_SIZE_SPEC_SYNC:
      case CalculateLayoutSource.RELOAD_PREVIOUS_STATE:
        return true;
      default:
        return false;
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
    private RunnableHandler layoutThreadHandler;
    private RunnableHandler preAllocateMountContentHandler;
    private StateHandler stateHandler;
    private RenderState previousRenderState;
    private boolean asyncStateUpdates = true;
    private int overrideComponentTreeId = -1;
    private boolean hasMounted = false;
    private boolean isFirstMount = false;
    private @Nullable MeasureListener mMeasureListener;
    private boolean shouldPreallocatePerMountSpec;
    private boolean isReconciliationEnabled =
        ComponentsConfiguration.keepReconciliationEnabledWhenStateless
            && ComponentsConfiguration.isReconciliationEnabled;
    private ErrorEventHandler errorEventHandler = DefaultErrorEventHandler.INSTANCE;
    private boolean canInterruptAndMoveLayoutsBetweenThreads =
        ComponentsConfiguration.canInterruptAndMoveLayoutsBetweenThreads;
    private boolean useCancelableLayoutFutures = ComponentsConfiguration.useCancelableLayoutFutures;
    private @Nullable String logTag;
    private @Nullable ComponentsLogger logger;
    private boolean shouldForceAsyncStateUpdate =
        ComponentsConfiguration.shouldForceAsyncStateUpdate;
    private boolean ignoreNullLayoutStateError = ComponentsConfiguration.ignoreNullLayoutStateError;
    private @Nullable LithoLifecycleProvider mLifecycleProvider;

    private boolean useStatelessComponent = ComponentsConfiguration.useStatelessComponent;
    private boolean shouldSkipShallowCopy = ComponentsConfiguration.shouldSkipShallowCopy;
    private boolean useInputOnlyInternalNodes = ComponentsConfiguration.useInputOnlyInternalNodes;
    private boolean reuseInternalNodes = ComponentsConfiguration.reuseInternalNodes;
    private boolean isLayoutCachingEnabled = ComponentsConfiguration.enableLayoutCaching;
    private boolean shouldClearPrevContextWhenCommitted =
        ComponentsConfiguration.shouldClearPrevContextWhenCommitted;

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
    public Builder layoutThreadHandler(@Nullable RunnableHandler handler) {
      layoutThreadHandler = handler;
      return this;
    }

    /**
     * Specify an initial state handler object that the ComponentTree can use to set the current
     * values for states.
     */
    public Builder stateHandler(@Nullable StateHandler stateHandler) {
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

    public Builder ignoreNullLayoutStateError(boolean ignoreNullLayoutStateError) {
      this.ignoreNullLayoutStateError = ignoreNullLayoutStateError;
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

    public void overrideStatelessConfigs(
        boolean useStateLessComponent,
        boolean shouldSkipShallowCopy,
        boolean inputOnlyInternalNode,
        boolean internalNodeReuseEnabled,
        boolean isLayoutCachingEnabled) {
      this.useStatelessComponent = useStateLessComponent;
      this.shouldSkipShallowCopy = shouldSkipShallowCopy;
      this.useInputOnlyInternalNodes = inputOnlyInternalNode;
      this.reuseInternalNodes = internalNodeReuseEnabled;
      this.isLayoutCachingEnabled = isLayoutCachingEnabled;
      this.shouldClearPrevContextWhenCommitted = shouldClearPrevContextWhenCommitted;
    }
  }
}
