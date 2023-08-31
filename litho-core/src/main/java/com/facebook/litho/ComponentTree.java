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

import static android.content.Context.ACCESSIBILITY_SERVICE;
import static com.facebook.litho.FrameworkLogEvents.EVENT_PRE_ALLOCATE_MOUNT_CONTENT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_ATTRIBUTION;
import static com.facebook.litho.FrameworkLogEvents.PARAM_COMPONENT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_IS_BACKGROUND_LAYOUT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_SOURCE;
import static com.facebook.litho.FrameworkLogEvents.PARAM_VERSION;
import static com.facebook.litho.LayoutState.isFromSyncLayout;
import static com.facebook.litho.LayoutState.layoutSourceToString;
import static com.facebook.litho.LithoLifecycleProvider.LithoLifecycle.HINT_INVISIBLE;
import static com.facebook.litho.LithoLifecycleProvider.LithoLifecycle.HINT_VISIBLE;
import static com.facebook.litho.RenderSourceUtils.getExecutionMode;
import static com.facebook.litho.RenderSourceUtils.getSource;
import static com.facebook.litho.StateContainer.StateUpdate;
import static com.facebook.litho.ThreadUtils.assertMainThread;
import static com.facebook.litho.ThreadUtils.isMainThread;
import static com.facebook.litho.config.ComponentsConfiguration.DEFAULT_BACKGROUND_THREAD_PRIORITY;
import static com.facebook.litho.debug.LithoDebugEventAttributes.Breadcrumb;
import static com.facebook.litho.debug.LithoDebugEventAttributes.HasMainThreadLayoutState;
import static com.facebook.litho.debug.LithoDebugEventAttributes.IdMatch;
import static com.facebook.litho.debug.LithoDebugEventAttributes.MainThreadLayoutStateHeightSpec;
import static com.facebook.litho.debug.LithoDebugEventAttributes.MainThreadLayoutStatePrettySizeSpecs;
import static com.facebook.litho.debug.LithoDebugEventAttributes.MainThreadLayoutStateRootId;
import static com.facebook.litho.debug.LithoDebugEventAttributes.MainThreadLayoutStateWidthSpec;
import static com.facebook.litho.debug.LithoDebugEventAttributes.MeasureHeightSpec;
import static com.facebook.litho.debug.LithoDebugEventAttributes.MeasurePrettySizeSpecs;
import static com.facebook.litho.debug.LithoDebugEventAttributes.MeasureWidthSpec;
import static com.facebook.litho.debug.LithoDebugEventAttributes.Root;
import static com.facebook.litho.debug.LithoDebugEventAttributes.RootId;
import static com.facebook.litho.debug.LithoDebugEventAttributes.SizeSpecsMatch;
import static com.facebook.rendercore.debug.DebugEventDispatcher.beginTrace;
import static com.facebook.rendercore.debug.DebugEventDispatcher.endTrace;
import static com.facebook.rendercore.debug.DebugEventDispatcher.generateTraceIdentifier;
import static com.facebook.rendercore.instrumentation.HandlerInstrumenter.instrumentHandler;
import static com.facebook.rendercore.utils.MeasureSpecUtils.getMeasureSpecDescription;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.view.Choreographer;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.arch.core.util.Function;
import androidx.lifecycle.LifecycleOwner;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.litho.LithoLifecycleProvider.LithoLifecycle;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.debug.AttributionUtils;
import com.facebook.litho.debug.DebugOverlay;
import com.facebook.litho.debug.LithoDebugEvent;
import com.facebook.litho.debug.LithoDebugEventAttributes;
import com.facebook.litho.perfboost.LithoPerfBooster;
import com.facebook.litho.stats.LithoStats;
import com.facebook.rendercore.LogLevel;
import com.facebook.rendercore.RunnableHandler;
import com.facebook.rendercore.RunnableHandler.DefaultHandler;
import com.facebook.rendercore.debug.DebugEventAttribute;
import com.facebook.rendercore.debug.DebugEventBus;
import com.facebook.rendercore.debug.DebugEventDispatcher;
import com.facebook.rendercore.visibility.VisibilityBoundsTransformer;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.concurrent.GuardedBy;
import kotlin.Unit;

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
public class ComponentTree
    implements LithoLifecycleListener,
        StateUpdater,
        MountedViewReference,
        ErrorComponentReceiver,
        LithoTreeLifecycleProvider {

  private static final boolean DEBUG_LOGS = false;

  public static final int INVALID_LAYOUT_VERSION = -1;
  public static final int INVALID_ID = -1;
  private static final String TAG = ComponentTree.class.getSimpleName();
  public static final int SIZE_UNINITIALIZED = -1;
  private static final String DEFAULT_RESOLVE_THREAD_NAME = "ComponentResolveThread";
  /* package-private */ static final String DEFAULT_LAYOUT_THREAD_NAME = "ComponentLayoutThread";
  private static final String EMPTY_STRING = "";
  private static final String CT_CONTEXT_IS_DIFFERENT_FROM_ROOT_BUILDER_CONTEXT =
      "ComponentTree:CTContextIsDifferentFromRootBuilderContext";
  private static final String M_LITHO_VIEW_IS_NULL =
      "ComponentTree:mountComponentInternal_mLithoView_Null";

  public static final int STATE_UPDATES_IN_LOOP_THRESHOLD = 50;
  private static final String STATE_UPDATES_IN_LOOP_EXCEED_THRESHOLD =
      "ComponentTree:StateUpdatesWhenLayoutInProgressExceedsThreshold:";
  private static boolean sBoostPerfLayoutStateFuture = false;
  @Nullable LithoLifecycleProvider mLifecycleProvider;

  @GuardedBy("this")
  private boolean mReleased;

  @ThreadConfined(ThreadConfined.UI)
  private @Nullable List<OnReleaseListener> mOnReleaseListeners;

  private String mReleasedComponent;
  private @Nullable volatile AttachDetachHandler mAttachDetachHandler;

  @GuardedBy("this")
  private int mStateUpdatesFromCreateLayoutCount;

  private @Nullable final AccessibilityManager mAccessibilityManager;

  private boolean mInAttach = false;

  @Nullable private final ComponentTreeTimeMachine mTimeMachine;
  @Nullable private final ComponentTreeDebugEventsSubscriber mDebugEventsSubscriber;

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

    LifecycleOwner lifecycleOwner = null;
    if (lifecycleProvider instanceof AOSPLifecycleOwnerProvider) {
      lifecycleOwner = ((AOSPLifecycleOwnerProvider) lifecycleProvider).getLifecycleOwner();
    }

    setInternalTreeProp(LifecycleOwner.class, lifecycleOwner);
  }

  public synchronized boolean isSubscribedToLifecycleProvider() {
    return mLifecycleProvider != null;
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

  private static final ThreadLocal<WeakReference<RunnableHandler>> sSyncStateUpdatesHandler =
      new ThreadLocal<>();

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
  private boolean mIsMeasuring;

  @ThreadConfined(ThreadConfined.UI)
  private final boolean mIsLayoutDiffingEnabled;

  @ThreadConfined(ThreadConfined.UI)
  private boolean mIsAttached;

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

  private final Object mCurrentDoLayoutRunnableLock = new Object();

  private final Object mCurrentDoResolveRunnableLock = new Object();

  @GuardedBy("mCurrentDoLayoutRunnableLock")
  private @Nullable DoLayoutRunnable mCurrentDoLayoutRunnable;

  @GuardedBy("mCurrentDoResolveRunnableLock")
  private @Nullable DoResolveRunnable mCurrentDoResolveRunnable;

  private final Object mLayoutStateFutureLock = new Object();

  private final Object mResolveResultFutureLock = new Object();

  @GuardedBy("mResolveResultFutureLock")
  private final List<ResolveTreeFuture> mResolveResultFutures = new ArrayList<>();

  @GuardedBy("mLayoutStateFutureLock")
  private final List<LayoutTreeFuture> mLayoutTreeFutures = new ArrayList<>();

  private @Nullable TreeFuture.FutureExecutionListener mFutureExecutionListener;

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
  private @RenderSource int mLastLayoutSource = RenderSource.NONE;

  // This is written to only by the main thread with the lock held, read from the main thread with
  // no lock held, or read from any other thread with the lock held.
  private @Nullable LayoutState mMainThreadLayoutState;

  @GuardedBy("this")
  private @Nullable LayoutState mCommittedLayoutState;

  @GuardedBy("this")
  private @Nullable ResolveResult mCommittedResolveResult;

  @GuardedBy("this")
  private @Nullable TreeState mTreeState;

  protected final int mId;

  @GuardedBy("this")
  private final WorkingRangeStatusHandler mWorkingRangeStatusHandler =
      new WorkingRangeStatusHandler();

  private final boolean useSeparateThreadHandlersForResolveAndLayout;

  /**
   * This is a breadcrumb that can be associated with the logs produced by {@link
   * ComponentTree#debugLog(String, String)}
   *
   * <p>Use {@link ComponentTree#setDebugLogsBreadcrumb(String)} to set the breadcrumb.
   */
  @Nullable private String mDebugLogBreadcrumb;

  /**
   * This method associates this {@link ComponentTree} debug logs with the given <code>String</code>
   * This allows you to create an association of the {@link ComponentTree} with any identifier or
   * sorts that enables you to trace the logs of this {@link ComponentTree} with the identifier.
   *
   * <p>This is particularly useful if you have multiple {@link ComponentTree} and you want to
   * filter the logs according to the given breadcrumb.
   */
  public void setDebugLogsBreadcrumb(@Nullable String breadcrumb) {
    mDebugLogBreadcrumb = breadcrumb;
  }

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
      @Nullable Component root,
      @Nullable LithoLifecycleProvider lifecycleProvider) {
    // TODO T88511125: Enforce non-null lithoLifecycleOwner here.
    final Builder builder = new ComponentTree.Builder(context);
    if (root != null) {
      builder.withRoot(root);
    }
    builder.withLithoLifecycleProvider(lifecycleProvider);
    return builder;
  }

  protected ComponentTree(Builder builder) {
    mRoot = builder.root;
    if (builder.overrideComponentTreeId != INVALID_ID) {
      mId = builder.overrideComponentTreeId;
    } else {
      mId = generateComponentTreeId();
    }
    final RenderUnitIdGenerator renderUnitIdGenerator;
    if (builder.mRenderUnitIdGenerator != null) {
      renderUnitIdGenerator = builder.mRenderUnitIdGenerator;
      if (mId != renderUnitIdGenerator.getComponentTreeId()) {
        throw new IllegalStateException(
            "Copying RenderUnitIdGenerator is only allowed if the ComponentTree IDs match");
      }
    } else {
      renderUnitIdGenerator = new RenderUnitIdGenerator(mId);
    }

    final String logTag;
    if (builder.logTag != null) {
      logTag = builder.logTag;
    } else if (builder.context.getLogTag() != null) {
      logTag = builder.context.getLogTag();
    } else {
      logTag = mRoot.getSimpleName();
    }

    final ComponentsLogger logger;
    if (builder.logger != null) {
      logger = builder.logger;
    } else {
      logger = builder.context.getLogger();
    }

    if (ComponentsConfiguration.enableStateUpdatesBatching) {
      mBatchedStateUpdatesStrategy = new PostStateUpdateToChoreographerCallback();
    } else {
      mBatchedStateUpdatesStrategy = null;
    }

    if (ComponentsConfiguration.overrideLayoutDiffing != null) {
      mIsLayoutDiffingEnabled = ComponentsConfiguration.overrideLayoutDiffing;
    } else {
      mIsLayoutDiffingEnabled = builder.isLayoutDiffingEnabled;
    }

    addMeasureListener(builder.mMeasureListener);

    useSeparateThreadHandlersForResolveAndLayout =
        ComponentsConfiguration.useSeparateThreadHandlersForResolveAndLayout;

    mTreeState = builder.treeState == null ? new TreeState() : builder.treeState;

    mIncrementalMountHelper =
        ComponentsConfiguration.USE_INCREMENTAL_MOUNT_HELPER
            ? new IncrementalMountHelper(this)
            : null;

    // Instrument LithoHandlers.
    mLayoutThreadHandler = builder.layoutThreadHandler;
    mMainThreadHandler = instrumentHandler(mMainThreadHandler);
    mLayoutThreadHandler = ensureAndInstrumentLayoutThreadHandler(mLayoutThreadHandler);

    mShouldPreallocatePerMountSpec = builder.shouldPreallocatePerMountSpec;
    mPreAllocateMountContentHandler = builder.preAllocateMountContentHandler;
    if (mPreAllocateMountContentHandler != null) {
      mPreAllocateMountContentHandler = instrumentHandler(mPreAllocateMountContentHandler);
    }

    if (useSeparateThreadHandlersForResolveAndLayout) {
      mResolveThreadHandler =
          builder.resolveThreadHandler != null
              ? instrumentHandler(builder.resolveThreadHandler)
              : instrumentHandler(new DefaultHandler(getDefaultResolveThreadLooper()));
    }

    final LithoConfiguration config =
        new LithoConfiguration(
            builder.config,
            AnimationsDebug.areTransitionsEnabled(builder.context.getAndroidContext()),
            ComponentsConfiguration.overrideReconciliation != null
                ? ComponentsConfiguration.overrideReconciliation
                : builder.isReconciliationEnabled,
            builder.visibilityProcessingEnabled,
            mShouldPreallocatePerMountSpec,
            mPreAllocateMountContentHandler,
            builder.incrementalMountEnabled && !incrementalMountGloballyDisabled(),
            builder.errorEventHandler,
            logTag,
            logger,
            renderUnitIdGenerator,
            builder.visibilityBoundsTransformer,
            builder.componentTreeDebugEventListener);

    mContext = ComponentContextUtils.withComponentTree(builder.context, config, this);

    if (ComponentsConfiguration.isTimelineEnabled) {
      mTimeMachine = new DebugComponentTreeTimeMachine(this);
    } else {
      mTimeMachine = null;
    }

    if (builder.mLifecycleProvider != null) {
      subscribeToLifecycleProvider(builder.mLifecycleProvider);
    }

    if (config.debugEventListener != null) {
      mDebugEventsSubscriber =
          new ComponentTreeDebugEventsSubscriber(
              mId,
              config.debugEventListener.getEvents(),
              debugEvent -> {
                config.debugEventListener.onEvent(debugEvent);
                return Unit.INSTANCE;
              });
      DebugEventBus.subscribe(mDebugEventsSubscriber);
    } else {
      mDebugEventsSubscriber = null;
    }

    mAccessibilityManager =
        (AccessibilityManager) mContext.getAndroidContext().getSystemService(ACCESSIBILITY_SERVICE);
  }

  private Object getResolveThreadHandlerLock() {
    return useSeparateThreadHandlersForResolveAndLayout
        ? mCurrentDoResolveRunnableLock
        : mCurrentDoLayoutRunnableLock;
  }

  private RunnableHandler getResolveThreadHandler() {
    return useSeparateThreadHandlersForResolveAndLayout
        ? mResolveThreadHandler
        : mLayoutThreadHandler;
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
    final TreeState treeState = getTreeState();
    if (treeState == null) {
      return false;
    }
    return treeState.getMountInfo().mHasMounted;
  }

  @Override
  public boolean isFirstMount() {
    final TreeState treeState = getTreeState();
    if (treeState == null) {
      return false;
    }
    return treeState.getMountInfo().mIsFirstMount;
  }

  @Override
  public <T> boolean canSkipStateUpdate(
      final String globalKey,
      final int hookStateIndex,
      final @Nullable T newValue,
      final boolean isNestedTree) {
    final TreeState treeState = getTreeState();
    if (treeState == null) {
      return false;
    }

    return treeState.canSkipStateUpdate(globalKey, hookStateIndex, newValue, isNestedTree);
  }

  @Override
  public <T> boolean canSkipStateUpdate(
      final Function<T, T> newValueFunction,
      final String globalKey,
      final int hookStateIndex,
      final boolean isNestedTree) {
    final TreeState treeState = getTreeState();
    if (treeState == null) {
      return false;
    }

    return treeState.canSkipStateUpdate(newValueFunction, globalKey, hookStateIndex, isNestedTree);
  }

  @Override
  public void setFirstMount(boolean isFirstMount) {
    final TreeState treeState = getTreeState();
    if (treeState == null) {
      return;
    }
    treeState.getMountInfo().mIsFirstMount = isFirstMount;
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

    mLayoutThreadHandler = ensureAndInstrumentLayoutThreadHandler(layoutThreadHandler);
  }

  @ThreadConfined(ThreadConfined.UI)
  public void updateResolveThreadHandler(@Nullable RunnableHandler resolveThreadHandler) {
    if (!useSeparateThreadHandlersForResolveAndLayout) {
      return;
    }

    synchronized (getResolveThreadHandlerLock()) {
      if (mCurrentDoResolveRunnable != null) {
        mResolveThreadHandler.remove(mCurrentDoResolveRunnable);
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
      mLithoView.mountComponentIfNeeded();
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

  void attach() {
    assertMainThread();

    if (mLithoView == null) {
      throw new IllegalStateException("Trying to attach a ComponentTree without a set View");
    }
    mInAttach = true;
    try {
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

    } finally {
      mInAttach = false;
    }
  }

  private static boolean hasSameRootContext(Context context1, Context context2) {
    return ContextUtils.getRootContext(context1) == ContextUtils.getRootContext(context2);
  }

  /**
   * @return whether this ComponentTree has a computed layout that will work for the given measure
   *     specs.
   */
  public synchronized boolean hasCompatibleLayout(int widthSpec, int heightSpec) {
    return isCompatibleSpec(mMainThreadLayoutState, widthSpec, heightSpec)
        || isCompatibleSpec(mCommittedLayoutState, widthSpec, heightSpec);
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

    if (view == null && mInAttach) {
      throw new RuntimeException("setting null LithoView while in attach");
    }

    mLithoView = view;
  }

  public void clearLithoView() {
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

    BaseMountingView.clearDebugOverlay(mLithoView);

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

          LayoutState state = mMainThreadLayoutState;

          DebugEventDispatcher.dispatch(
              LithoDebugEvent.RenderOnMainThreadStarted,
              () -> String.valueOf(mId),
              attributes -> {
                attributes.put(Root, mRoot != null ? mRoot.getSimpleName() : "");
                attributes.put(Breadcrumb, mDebugLogBreadcrumb);
                attributes.put(HasMainThreadLayoutState, state != null);
                attributes.put(SizeSpecsMatch, true);
                attributes.put(IdMatch, true);

                if (state != null) {
                  int id = mRoot != null ? mRoot.getId() : INVALID_ID;
                  int mainThreadLayoutStateRootId = state.getRootComponent().getId();
                  boolean doesSpecMatch = state.isCompatibleSpec(widthSpec, heightSpec);
                  boolean doesIdsMatch = mainThreadLayoutStateRootId != id && id != INVALID_ID;

                  if (!doesSpecMatch) {
                    attributes.put(SizeSpecsMatch, false);
                    attributes.put(MainThreadLayoutStateWidthSpec, state.getWidthSpec());
                    attributes.put(MainThreadLayoutStateHeightSpec, state.getHeightSpec());
                    attributes.put(
                        MainThreadLayoutStatePrettySizeSpecs,
                        specsToString(state.getWidthSpec(), state.getHeightSpec()));

                    attributes.put(MeasureWidthSpec, widthSpec);
                    attributes.put(MeasureHeightSpec, heightSpec);
                    attributes.put(MeasurePrettySizeSpecs, specsToString(widthSpec, heightSpec));
                  }

                  if (!doesIdsMatch) {
                    attributes.put(IdMatch, false);
                    attributes.put(RootId, id);
                    attributes.put(MainThreadLayoutStateRootId, mainThreadLayoutStateRootId);
                  }
                }
                return Unit.INSTANCE;
              });

          // TODO: Remove this after plugging the logs subscriber by default.
          if (DEBUG_LOGS) {
            if (mMainThreadLayoutState != null) {

              int id = mRoot != null ? mRoot.getId() : INVALID_ID;
              boolean doesSpecMatch = state.isCompatibleSpec(widthSpec, heightSpec);
              final boolean doesIdMatch = state.getRootComponent().getId() == id;

              String idMatchError = "";
              if (!doesIdMatch && id != INVALID_ID) {
                idMatchError = "\nid-matched: " + doesIdMatch;
              }

              String measurementError = "";
              if (!doesSpecMatch) {
                String measuredSize = "w: " + state.getWidth() + " h: " + state.getHeight();
                String measuredSpec = specsToString(state.getWidthSpec(), state.getHeightSpec());
                String requiredSpec = specsToString(widthSpec, heightSpec);
                measurementError =
                    "\nrequired-spec: "
                        + requiredSpec
                        + "\nmeasured-spec: "
                        + measuredSpec
                        + "\nmeasured-size: "
                        + measuredSize;
              }

              debugLog("measure:", "needsSyncLayout: " + measurementError + idMatchError);
            } else {
              debugLog("measure:", "needsSyncLayout: no MainThreadLayout available");
            }
          }
        }
      }

      if (needsSyncLayout || forceLayout) {

        if (DebugOverlay.isEnabled && ThreadUtils.isMainThread() && mLithoView != null) {
          flash(mLithoView);
        }

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

  private static String specsToString(int widthSpec, int heightSpec) {
    return "w: "
        + SizeSpec.toSimpleString(widthSpec)
        + ", h: "
        + SizeSpec.toSimpleString(heightSpec);
  }

  LithoConfiguration getLithoConfiguration() {
    return mContext.mLithoConfiguration;
  }

  /** Returns whether incremental mount is enabled or not in this component. */
  public boolean isIncrementalMountEnabled() {
    return mContext.mLithoConfiguration.incrementalMountEnabled;
  }

  boolean isVisibilityProcessingEnabled() {
    return mContext.mLithoConfiguration.isVisibilityProcessingEnabled;
  }

  public boolean shouldReuseOutputs() {
    return mContext.mLithoConfiguration.mComponentsConfiguration.shouldReuseOutputs();
  }

  public boolean isReconciliationEnabled() {
    return mContext.mLithoConfiguration.isReconciliationEnabled;
  }

  public synchronized @Nullable Component getRoot() {
    return mRoot;
  }

  synchronized int getWidthSpec() {
    return mWidthSpec;
  }

  synchronized int getHeightSpec() {
    return mHeightSpec;
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
        RenderSource.SET_ROOT_SYNC,
        INVALID_LAYOUT_VERSION,
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
                mContext, logger, logger.newPerformanceEvent(EVENT_PRE_ALLOCATE_MOUNT_CONTENT))
            : null;

    Integer traceIdentifier =
        generateTraceIdentifier(LithoDebugEvent.ComponentTreeMountContentPreallocated);
    if (traceIdentifier != null) {
      beginTrace(
          traceIdentifier,
          LithoDebugEvent.ComponentTreeMountContentPreallocated,
          String.valueOf(mId),
          new HashMap<>());
    }

    toPrePopulate.preAllocateMountContent(shouldPreallocatePerMountSpec);

    if (traceIdentifier != null) {
      endTrace(traceIdentifier);
    }
    if (event != null) {
      logger.logPerfEvent(event);
    }
  }

  public void setRootSync(@Nullable Component root) {
    setRootAndSizeSpecAndWrapper(
        root,
        SIZE_UNINITIALIZED,
        SIZE_UNINITIALIZED,
        false /* isAsync */,
        null /* output */,
        RenderSource.SET_ROOT_SYNC,
        INVALID_LAYOUT_VERSION,
        null,
        null);
  }

  public void setRootAsync(@Nullable Component root) {
    setRootAndSizeSpecAndWrapper(
        root,
        SIZE_UNINITIALIZED,
        SIZE_UNINITIALIZED,
        true /* isAsync */,
        null /* output */,
        RenderSource.SET_ROOT_ASYNC,
        INVALID_LAYOUT_VERSION,
        null,
        null);
  }

  @VisibleForTesting
  synchronized void updateStateLazy(String componentKey, StateUpdate stateUpdate) {
    updateStateLazy(componentKey, stateUpdate, false);
  }

  @Override
  public synchronized void updateStateLazy(
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
  @Override
  public synchronized StateContainer applyLazyStateUpdatesForContainer(
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

  @Override
  public void updateStateSync(
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
  public void updateStateAsync(
      String componentKey,
      StateUpdate stateUpdate,
      String attribution,
      boolean isCreateLayoutInProgress) {
    updateStateAsync(componentKey, stateUpdate, attribution, isCreateLayoutInProgress, false);
  }

  @Override
  public void updateStateAsync(
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

    LithoStats.incrementComponentStateUpdateAsyncCount();
    onAsyncStateUpdateEnqueued(attribution, isCreateLayoutInProgress);
  }

  @Override
  public final void updateHookStateSync(
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

  @Override
  public final void updateHookStateAsync(
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
    dispatchStateUpdateEnqueuedEvent(attribution, false);

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

    dispatchStateUpdateEnqueuedEvent(attribution, true);

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

  private void dispatchStateUpdateEnqueuedEvent(String attribution, boolean isSynchronous) {
    DebugEventDispatcher.dispatch(
        LithoDebugEvent.StateUpdateEnqueued,
        () -> String.valueOf(mId),
        attributes -> {
          attributes.put(
              LithoDebugEventAttributes.Root, mRoot != null ? mRoot.getSimpleName() : "");
          attributes.put(LithoDebugEventAttributes.Attribution, attribution);
          attributes.put(
              LithoDebugEventAttributes.StateUpdateType, isSynchronous ? "sync" : "async");
          return Unit.INSTANCE;
        });
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
        isAsync ? RenderSource.UPDATE_STATE_ASYNC : RenderSource.UPDATE_STATE_SYNC,
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

  /**
   * Stores a tree property to be used in the context of this {@link ComponentTree}, where it will
   * associate the given key to its value.
   *
   * <p>It will make sure that the tree properties are properly cloned and stored.
   */
  private void setInternalTreeProp(Class key, @Nullable Object value) {
    if (!mContext.isParentTreePropsCloned()) {
      mContext.setTreeProps(TreeProps.acquire(mContext.getTreeProps()));
      mContext.setParentTreePropsCloned(true);
    }

    TreeProps treeProps = mContext.getTreeProps();
    if (treeProps != null) {
      treeProps.put(key, value);
    }
  }

  @Nullable
  @VisibleForTesting
  public synchronized TreeState getTreeState() {
    return mTreeState;
  }

  @Nullable
  @Override
  public EventTrigger getEventTrigger(String triggerKey) {
    return mTreeState == null ? null : mTreeState.getEventTrigger(triggerKey);
  }

  @Nullable
  @Override
  public EventTrigger getEventTrigger(Handle handle, int methodId) {
    return mTreeState == null ? null : mTreeState.getEventTrigger(handle, methodId);
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
        RenderSource.SET_SIZE_SPEC_SYNC,
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
        RenderSource.SET_SIZE_SPEC_ASYNC,
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
        RenderSource.MEASURE_SET_SIZE_SPEC,
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
        RenderSource.MEASURE_SET_SIZE_SPEC_ASYNC,
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
        RenderSource.SET_ROOT_ASYNC,
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
        RenderSource.SET_ROOT_ASYNC,
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
        RenderSource.SET_ROOT_SYNC,
        INVALID_LAYOUT_VERSION,
        null,
        null);
  }

  public void setRootAndSizeSpecSync(
      @Nullable Component root, int widthSpec, int heightSpec, @Nullable Size output) {
    setRootAndSizeSpecAndWrapper(
        root,
        widthSpec,
        heightSpec,
        false /* isAsync */,
        output,
        RenderSource.SET_ROOT_SYNC,
        INVALID_LAYOUT_VERSION,
        null,
        null);
  }

  public void setRootAndSizeSpecSync(
      @Nullable Component root,
      int widthSpec,
      int heightSpec,
      @Nullable Size output,
      @Nullable TreeProps treeProps) {
    setRootAndSizeSpecAndWrapper(
        root,
        widthSpec,
        heightSpec,
        false /* isAsync */,
        output,
        RenderSource.SET_ROOT_SYNC,
        INVALID_LAYOUT_VERSION,
        null,
        treeProps);
  }

  public void setVersionedRootAndSizeSpec(
      @Nullable Component root,
      int widthSpec,
      int heightSpec,
      @Nullable Size output,
      @Nullable TreeProps treeProps,
      int externalRootVersion) {
    setRootAndSizeSpecAndWrapper(
        root,
        widthSpec,
        heightSpec,
        false /* isAsync */,
        output,
        RenderSource.SET_ROOT_SYNC,
        externalRootVersion,
        null,
        treeProps);
  }

  public void setVersionedRootAndSizeSpecAsync(
      @Nullable Component root,
      int widthSpec,
      int heightSpec,
      @Nullable Size output,
      @Nullable TreeProps treeProps,
      int externalRootVersion) {
    setRootAndSizeSpecAndWrapper(
        root,
        widthSpec,
        heightSpec,
        true /* isAsync */,
        output,
        RenderSource.SET_ROOT_ASYNC,
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
    return context.getLifecycleProvider();
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
      final ComponentContext parentContext, @Nullable Component component) {

    final SimpleNestedTreeLifecycleProvider lifecycleProvider =
        parentContext.getLifecycleProvider() == null
            ? null
            : new SimpleNestedTreeLifecycleProvider(parentContext.getLifecycleProvider());

    return ComponentTree.create(
        ComponentContext.makeCopyForNestedTree(parentContext), component, lifecycleProvider);
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
      @RenderSource int source,
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
      @RenderSource int source,
      int externalRootVersion,
      @Nullable String extraAttribution,
      @Nullable TreeProps treeProps,
      boolean isCreateLayoutInProgress,
      boolean forceLayout) {

    final int requestedWidthSpec;
    final int requestedHeightSpec;
    final Component requestedRoot;
    final TreeProps requestedTreeProps;

    synchronized (this) {

      // logs the render that was requested
      logRenderRequest(root, source, widthSpec, heightSpec, extraAttribution, forceLayout);

      if (mReleased) {
        // If this is coming from a background thread, we may have been released from the main
        // thread. In that case, do nothing.
        //
        // NB: This is only safe because we don't re-use released ComponentTrees.
        return;
      }

      // If this is coming from a setRoot
      if (source == RenderSource.SET_ROOT_SYNC || source == RenderSource.SET_ROOT_ASYNC) {
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

      requestedWidthSpec = mWidthSpec;
      requestedHeightSpec = mHeightSpec;
      requestedRoot = mRoot;
      requestedTreeProps = mRootTreeProps;

      mLastLayoutSource = source;
    }

    if (isAsync && output != null) {
      throw new IllegalArgumentException(
          "The layout can't be calculated asynchronously if we need the Size back");
    }

    requestRenderWithSplitFutures(
        isAsync,
        output,
        source,
        extraAttribution,
        isCreateLayoutInProgress,
        requestedWidthSpec,
        requestedHeightSpec,
        requestedRoot,
        requestedTreeProps);
  }

  private void logRenderRequest(
      final @Nullable Component root,
      final @RenderSource int source,
      final int widthSpec,
      final int heightSpec,
      final @Nullable String extraAttribution,
      final boolean wasForced) {
    DebugEventDispatcher.dispatch(
        LithoDebugEvent.RenderRequest,
        () -> String.valueOf(mId),
        LogLevel.VERBOSE,
        attrs -> {
          final String attribution = AttributionUtils.getAttribution(extraAttribution);
          attrs.put(LithoDebugEventAttributes.RenderSource, getSource(source));
          attrs.put(LithoDebugEventAttributes.RenderExecutionMode, getExecutionMode(source));
          attrs.put(LithoDebugEventAttributes.Attribution, attribution);
          attrs.put(LithoDebugEventAttributes.Root, root != null ? root.getSimpleName() : "null");
          attrs.put(LithoDebugEventAttributes.Forced, wasForced);
          if (widthSpec != SIZE_UNINITIALIZED || heightSpec != SIZE_UNINITIALIZED) {
            attrs.put(DebugEventAttribute.widthSpec, getMeasureSpecDescription(widthSpec));
            attrs.put(DebugEventAttribute.heightSpec, getMeasureSpecDescription(heightSpec));
          }
          return Unit.INSTANCE;
        });
  }

  private void requestRenderWithSplitFutures(
      boolean isAsync,
      @Nullable Size output,
      @RenderSource int source,
      @Nullable String extraAttribution,
      boolean isCreateLayoutInProgress,
      final int widthSpec,
      final int heightSpec,
      final Component root,
      final TreeProps treeProps) {
    final ResolveResult currentResolveResult;
    synchronized (this) {
      currentResolveResult = mCommittedResolveResult;
    }

    // If we're only setting root, and there are no size specs, move the operation to async
    // to avoid hanging on the main thread
    if (source == RenderSource.SET_ROOT_SYNC
        && widthSpec == SIZE_UNINITIALIZED
        && heightSpec == SIZE_UNINITIALIZED) {
      isAsync = true;
      source = RenderSource.SET_ROOT_ASYNC;
    }

    // The current root and tree-props are the same as the committed resolved result. Therefore,
    // there is no need to calculate the resolved result again and we can proceed straight to
    // layout.
    if (currentResolveResult != null) {
      boolean canLayoutWithoutResolve =
          (ComponentsConfiguration.isSkipRootCheckingEnabled
                  // we can skip resolve phase if the render source is size changing
                  && (source == RenderSource.SET_SIZE_SPEC_SYNC
                      || source == RenderSource.SET_SIZE_SPEC_ASYNC
                      || source == RenderSource.MEASURE_SET_SIZE_SPEC
                      || source == RenderSource.MEASURE_SET_SIZE_SPEC_ASYNC)
                  // we cannot skip resolve phase if there's async resolve task running
                  && (mCurrentDoResolveRunnable == null))
              || (currentResolveResult.component == root
                  && currentResolveResult.context.getTreeProps() == treeProps);
      if (canLayoutWithoutResolve) {
        requestLayoutWithSplitFutures(
            currentResolveResult,
            output,
            source,
            extraAttribution,
            isCreateLayoutInProgress,
            false,
            widthSpec,
            heightSpec);
        return;
      }
    }

    if (isAsync) {
      synchronized (getResolveThreadHandlerLock()) {
        if (mCurrentDoResolveRunnable != null) {
          getResolveThreadHandler().remove(mCurrentDoResolveRunnable);
        }
        mCurrentDoResolveRunnable =
            new DoResolveRunnable(
                source,
                root,
                treeProps,
                widthSpec,
                heightSpec,
                extraAttribution,
                isCreateLayoutInProgress);

        String tag = EMPTY_STRING;
        if (getResolveThreadHandler().isTracing()) {
          tag = "doResolve ";
          if (mRoot != null) {
            tag = tag + mRoot.getSimpleName();
          }
        }
        getResolveThreadHandler().post(mCurrentDoResolveRunnable, tag);
      }
    } else {
      doResolve(
          output,
          source,
          extraAttribution,
          isCreateLayoutInProgress,
          root,
          treeProps,
          widthSpec,
          heightSpec);
    }
  }

  private void doResolve(
      @Nullable Size output,
      @RenderSource int source,
      @Nullable String extraAttribution,
      boolean isCreateLayoutInProgress,
      final Component root,
      final @Nullable TreeProps treeProps,
      final int widthSpec,
      final int heightSpec) {

    synchronized (getResolveThreadHandlerLock()) {
      if (mCurrentDoResolveRunnable != null) {
        getResolveThreadHandler().remove(mCurrentDoResolveRunnable);
        mCurrentDoResolveRunnable = null;
      }
    }

    final int localResolveVersion;
    final TreeState treeState;
    final ComponentContext context;
    final @Nullable LithoNode currentNode;

    synchronized (this) {
      if (root == null) {
        return;
      }

      localResolveVersion = mNextResolveVersion++;
      treeState = acquireTreeState();
      currentNode = mCommittedResolveResult != null ? mCommittedResolveResult.node : null;
      context = new ComponentContext(mContext, treeProps);
    }

    PerfEvent resolvePerfEvent =
        createEventForPipeline(
            FrameworkLogEvents.EVENT_CALCULATE_RESOLVE,
            source,
            extraAttribution,
            root,
            localResolveVersion);

    if (root.getBuilderContextName() != null
        && !Component.getBuilderContextName(mContext.getAndroidContext())
            .equals(root.getBuilderContextName())) {
      final String message =
          "ComponentTree context is different from root builder context"
              + ", ComponentTree context="
              + Component.getBuilderContextName(mContext.getAndroidContext())
              + ", root builder context="
              + root.getBuilderContextName()
              + ", root="
              + root.getSimpleName()
              + ", ContextTree="
              + ComponentTreeDumpingHelper.dumpContextTree(this);
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.ERROR,
          CT_CONTEXT_IS_DIFFERENT_FROM_ROOT_BUILDER_CONTEXT,
          message);
    }

    // If sync operations are interruptible and happen on background thread, which could end
    // up being moved to UI thread with null result and causing crash later. To prevent that
    // we mark it as Non-Interruptible.
    final boolean isInterruptible =
        !LayoutState.isFromSyncLayout(source)
            && (mContext.mLithoConfiguration.mComponentsConfiguration
                    .getUseInterruptibleResolution()
                || mContext.mLithoConfiguration.mComponentsConfiguration
                    .getUseCancelableLayoutFutures());

    ResolveTreeFuture treeFuture =
        new ResolveTreeFuture(
            context,
            root,
            treeState,
            currentNode,
            null,
            localResolveVersion,
            isInterruptible,
            widthSpec,
            heightSpec,
            mId,
            extraAttribution,
            source);

    final TreeFuture.TreeFutureResult<ResolveResult> resolveResultHolder =
        TreeFuture.trackAndRunTreeFuture(
            treeFuture,
            mResolveResultFutures,
            source,
            mResolveResultFutureLock,
            mFutureExecutionListener);

    if (resolveResultHolder == null) {
      return;
    }

    final @Nullable ResolveResult resolveResult = resolveResultHolder.result;

    if (resolveResult == null) {
      if (!isReleased()
          && isFromSyncLayout(source)
          && !mContext.mLithoConfiguration.mComponentsConfiguration
              .getUseCancelableLayoutFutures()) {
        final String errorMessage =
            "ResolveResult is null, but only async operations can return a null ResolveResult. Source: "
                + layoutSourceToString(source)
                + ", message: "
                + resolveResultHolder.message
                + ", current thread: "
                + Thread.currentThread().getName()
                + ". Root: "
                + (root == null ? "null" : root.getSimpleName());
        throw new IllegalStateException(errorMessage);
      }

      return;
    }

    commitResolveResult(resolveResult, isCreateLayoutInProgress);

    ComponentsLogger logger = getLogger();
    if (logger != null && resolvePerfEvent != null) {
      logger.logPerfEvent(resolvePerfEvent);
    }

    requestLayoutWithSplitFutures(
        resolveResult,
        output,
        source,
        extraAttribution,
        isCreateLayoutInProgress,
        // Don't post when using the same thread handler, as it could cause heavy delays
        !useSeparateThreadHandlersForResolveAndLayout,
        widthSpec,
        heightSpec);
  }

  /**
   * Creates a {@link PerfEvent} for the given {@param eventId}. If the used {@link
   * ComponentsLogger} is not interested in that event, it will return <code>null</code>.
   */
  @Nullable
  private PerfEvent createEventForPipeline(
      @FrameworkLogEvents.LogEventId int eventId,
      @RenderSource int source,
      @Nullable String extraAttribution,
      Component root,
      int pipelineVersion) {
    ComponentsLogger logger = getContextLogger();
    PerfEvent resolvePerfEvent = null;
    if (logger != null) {
      resolvePerfEvent = logger.newPerformanceEvent(eventId);
      if (resolvePerfEvent != null) {
        resolvePerfEvent.markerAnnotate(PARAM_COMPONENT, root.getSimpleName());
        resolvePerfEvent.markerAnnotate(PARAM_SOURCE, layoutSourceToString(source));
        resolvePerfEvent.markerAnnotate(PARAM_IS_BACKGROUND_LAYOUT, !ThreadUtils.isMainThread());
        resolvePerfEvent.markerAnnotate(PARAM_ATTRIBUTION, extraAttribution);
        resolvePerfEvent.markerAnnotate(PARAM_VERSION, pipelineVersion);
      }
    }
    return resolvePerfEvent;
  }

  private synchronized void commitResolveResult(
      final ResolveResult resolveResult, final boolean isCreateLayoutInProgress) {
    if (mCommittedResolveResult == null
        || mCommittedResolveResult.version < resolveResult.version) {
      mCommittedResolveResult = resolveResult;

      if (mTreeState != null) {
        mTreeState.commitResolveState(resolveResult.treeState);
      }

      // Resetting the count after resolve calculation is complete and it was triggered during a
      // calculation
      if (!isCreateLayoutInProgress) {
        mStateUpdatesFromCreateLayoutCount = 0;
      }
    }
  }

  private void requestLayoutWithSplitFutures(
      final ResolveResult resolveResult,
      @Nullable Size output,
      @RenderSource int source,
      @Nullable String extraAttribution,
      boolean isCreateLayoutInProgress,
      boolean forceSyncCalculation,
      final int widthSpec,
      final int heightSpec) {

    final boolean isAsync = !isFromSyncLayout(source);

    if (isAsync && output != null) {
      throw new IllegalStateException(
          "Cannot generate output for async layout calculation (source = " + source + ")");
    }

    if (isAsync && !forceSyncCalculation) {
      synchronized (mCurrentDoLayoutRunnableLock) {
        if (mCurrentDoLayoutRunnable != null) {
          mLayoutThreadHandler.remove(mCurrentDoLayoutRunnable);
        }
        mCurrentDoLayoutRunnable =
            new DoLayoutRunnable(
                resolveResult,
                source,
                widthSpec,
                heightSpec,
                extraAttribution,
                isCreateLayoutInProgress);

        String tag = EMPTY_STRING;
        if (mLayoutThreadHandler.isTracing()) {
          tag = "doLayout ";
          if (mRoot != null) {
            tag = tag + mRoot.getSimpleName();
          }
        }
        mLayoutThreadHandler.post(mCurrentDoLayoutRunnable, tag);
      }
    } else {
      doLayout(
          resolveResult,
          output,
          source,
          extraAttribution,
          isCreateLayoutInProgress,
          widthSpec,
          heightSpec);
    }
  }

  private void doLayout(
      final ResolveResult resolveResult,
      @Nullable Size output,
      @RenderSource int source,
      @Nullable String extraAttribution,
      boolean isCreateLayoutInProgress,
      final int widthSpec,
      final int heightSpec) {
    synchronized (mCurrentDoLayoutRunnableLock) {
      if (mCurrentDoLayoutRunnable != null) {
        mLayoutThreadHandler.remove(mCurrentDoLayoutRunnable);
        mCurrentDoLayoutRunnable = null;
      }
    }

    final int layoutVersion;
    final @Nullable LayoutState currentLayoutState;
    final @Nullable DiffNode currentDiffNode;
    final @Nullable TreeProps treeProps;

    final boolean isSync = isFromSyncLayout(source);

    synchronized (this) {
      currentLayoutState = mCommittedLayoutState;
      currentDiffNode = currentLayoutState != null ? currentLayoutState.getDiffTree() : null;
      layoutVersion = mNextLayoutVersion++;
      treeProps = resolveResult != null ? resolveResult.context.getTreeProps() : null;
    }

    // No width / height spec, no point proceeding.
    if (widthSpec == SIZE_UNINITIALIZED && heightSpec == SIZE_UNINITIALIZED) {
      return;
    }

    resolveResult.treeState.registerLayoutState();

    final LayoutTreeFuture layoutTreeFuture =
        new LayoutTreeFuture(
            resolveResult,
            currentLayoutState,
            currentDiffNode,
            null,
            widthSpec,
            heightSpec,
            mId,
            layoutVersion,
            mIsLayoutDiffingEnabled,
            source);

    final TreeFuture.TreeFutureResult<LayoutState> layoutStateHolder =
        TreeFuture.trackAndRunTreeFuture(
            layoutTreeFuture,
            mLayoutTreeFutures,
            source,
            mLayoutStateFutureLock,
            mFutureExecutionListener);

    if (layoutStateHolder == null) {
      return;
    }

    final @Nullable LayoutState layoutState = layoutStateHolder.result;

    if (layoutState == null) {
      if (!isReleased()
          && isSync
          && !mContext.mLithoConfiguration.mComponentsConfiguration
              .getUseCancelableLayoutFutures()) {
        final String errorMessage =
            "LayoutState is null, but only async operations can return a null LayoutState. Source: "
                + layoutSourceToString(source)
                + ", message: "
                + layoutStateHolder.message
                + ", current thread: "
                + Thread.currentThread().getName()
                + ". Root: "
                + (resolveResult.component.getSimpleName());

        throw new IllegalStateException(errorMessage);
      }

      return;
    }

    if (output != null) {
      output.width = layoutState.getWidth();
      output.height = layoutState.getHeight();
    }

    // Don't commit LayoutState if it doesn't match the committed resolved result
    if (resolveResult != mCommittedResolveResult) {
      return;
    }

    commitLayoutState(
        layoutState,
        layoutVersion,
        source,
        extraAttribution,
        isCreateLayoutInProgress,
        treeProps,
        resolveResult.component);
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  void setFutureExecutionListener(
      final @Nullable TreeFuture.FutureExecutionListener futureExecutionListener) {
    mFutureExecutionListener = futureExecutionListener;
  }

  private void commitLayoutState(
      final LayoutState layoutState,
      final int layoutVersion,
      final @RenderSource int source,
      final @Nullable String extraAttribution,
      final boolean isCreateLayoutInProgress,
      final @Nullable TreeProps treeProps,
      final Component rootComponent) {
    List<ScopedComponentInfo> scopedSpecComponentInfos = null;
    List<MeasureListener> measureListeners = null;
    List<Pair<String, EventHandler<?>>> createdEventHandlers = null;

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
        DebugEventDispatcher.dispatch(
            LithoDebugEvent.LayoutCommitted,
            () -> String.valueOf(mId),
            attributes -> {
              attributes.put(DebugEventAttribute.version, layoutVersion);
              attributes.put(DebugEventAttribute.source, layoutSourceToString(source));
              attributes.put(DebugEventAttribute.width, layoutState.getWidth());
              attributes.put(DebugEventAttribute.height, layoutState.getHeight());
              return Unit.INSTANCE;
            });
      }

      if (DEBUG_LOGS) {
        logFinishLayout(source, extraAttribution, layoutState, committedNewLayout);
      }

      final TreeState localTreeState = layoutState.getTreeState();
      if (committedNewLayout) {
        scopedSpecComponentInfos = layoutState.consumeScopedSpecComponentInfos();
        createdEventHandlers = layoutState.consumeCreatedEventHandlers();
        if (localTreeState != null) {
          final TreeState treeState = mTreeState;
          if (treeState != null) { // we could have been released
            saveRevision(rootComponent, treeState, treeProps, source, extraAttribution);
            treeState.commitLayoutState(localTreeState);
            treeState.bindEventAndTriggerHandlers(createdEventHandlers, scopedSpecComponentInfos);
          }
        }

        if (mMeasureListeners != null) {
          rootWidth = layoutState.getWidth();
          rootHeight = layoutState.getHeight();
        }

        bindHandlesToComponentTree(this, layoutState);

        measureListeners = mMeasureListeners == null ? null : new ArrayList<>(mMeasureListeners);
      }

      // Resetting the count after layout calculation is complete and it was triggered from within
      // layout creation
      if (!isCreateLayoutInProgress) {
        mStateUpdatesFromCreateLayoutCount = 0;
      }
    }

    if (committedNewLayout) {
      if (measureListeners != null) {
        for (MeasureListener measureListener : measureListeners) {
          measureListener.onSetRootAndSizeSpec(
              layoutVersion,
              rootWidth,
              rootHeight,
              source == RenderSource.UPDATE_STATE_ASYNC
                  || source == RenderSource.UPDATE_STATE_SYNC);
        }
      }

      postBackgroundLayoutStateUpdated();

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
    if (mLithoView != null && mLithoView.isMounting()) {
      throw new IllegalStateException("Releasing a ComponentTree that is currently being mounted");
    }

    synchronized (this) {
      if (mDebugEventsSubscriber != null) {
        DebugEventBus.unsubscribe(mDebugEventsSubscriber);
      }

      if (mBatchedStateUpdatesStrategy != null) {
        mBatchedStateUpdatesStrategy.release();
      }

      mMainThreadHandler.remove(mBackgroundLayoutStateUpdateRunnable);

      synchronized (getResolveThreadHandlerLock()) {
        if (mCurrentDoResolveRunnable != null) {
          getResolveThreadHandler().remove(mCurrentDoResolveRunnable);
          mCurrentDoResolveRunnable = null;
        }
      }

      synchronized (mCurrentDoLayoutRunnableLock) {
        if (mCurrentDoLayoutRunnable != null) {
          mLayoutThreadHandler.remove(mCurrentDoLayoutRunnable);
          mCurrentDoLayoutRunnable = null;
        }
      }

      synchronized (mUpdateStateSyncRunnableLock) {
        if (mUpdateStateSyncRunnable != null) {
          getResolveThreadHandler().remove(mUpdateStateSyncRunnable);
          mUpdateStateSyncRunnable = null;
        }
      }

      synchronized (mResolveResultFutureLock) {
        for (TreeFuture rtf : mResolveResultFutures) {
          rtf.release();
        }

        mResolveResultFutures.clear();
      }

      synchronized (mLayoutStateFutureLock) {
        for (TreeFuture ltf : mLayoutTreeFutures) {
          ltf.release();
        }

        mLayoutTreeFutures.clear();
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
      mMeasureListeners = null;
      mCommittedResolveResult = null;
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
    if (mTreeState != null) {
      mTreeState.clearUnusedTriggerHandlers();
    }
  }

  public synchronized @Nullable String getSimpleName() {
    return mRoot == null ? null : mRoot.getSimpleName();
  }

  @Override
  public synchronized @Nullable Object getCachedValue(
      Object cachedValueInputs, boolean isNestedTree) {
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

  @Override
  public synchronized void putCachedValue(
      Object cachedValueInputs, Object cachedValue, boolean isNestedTree) {
    if (mReleased || mTreeState == null) {
      return;
    }
    mTreeState.putCachedValue(cachedValueInputs, cachedValue, isNestedTree);
  }

  @Override
  public void removePendingStateUpdate(String key, boolean nestedTreeContext) {
    if (mReleased || mTreeState == null) {
      return;
    }
    mTreeState.removePendingStateUpdate(key, nestedTreeContext);
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

  private boolean isCompatibleSpec(
      final @Nullable LayoutState layoutState, final int widthSpec, final int heightSpec) {
    return layoutState != null
        && layoutState.isCompatibleSpec(widthSpec, heightSpec)
        && AccessibilityUtils.isAccessibilityEnabled(mAccessibilityManager)
            == layoutState.isAccessibilityEnabled();
  }

  private boolean isCompatibleComponentAndSpec(
      @Nullable LayoutState layoutState, int componentId, int widthSpec, int heightSpec) {
    return layoutState != null
        && layoutState.isCompatibleComponentAndSpec(componentId, widthSpec, heightSpec)
        && AccessibilityUtils.isAccessibilityEnabled(mAccessibilityManager)
            == layoutState.isAccessibilityEnabled();
  }

  @Override
  public synchronized boolean isReleased() {
    return mReleased;
  }

  synchronized String getReleasedComponent() {
    return mReleasedComponent;
  }

  public ComponentContext getContext() {
    return mContext;
  }

  @Nullable
  @Override
  public View getMountedView() {
    return mLithoView;
  }

  @Override
  public void onErrorComponent(Component component) {
    setRoot(component);
  }

  private @Nullable ComponentsLogger getContextLogger() {
    return mContext.mLithoConfiguration.logger;
  }

  public @Nullable ComponentsLogger getLogger() {
    return mContext.mLithoConfiguration.logger;
  }

  public @Nullable String getLogTag() {
    return mContext.mLithoConfiguration.logTag;
  }

  /*
   * The layouts which this ComponentTree was currently calculating will be terminated before
   * a valid result is computed. It's not safe to try to compute any layouts for this ComponentTree
   * after that because it's in an incomplete state, so it needs to be released.
   */
  public void cancelLayoutAndReleaseTree() {
    if (!mContext.mLithoConfiguration.mComponentsConfiguration.getUseCancelableLayoutFutures()) {
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.ERROR,
          TAG,
          "Cancelling layouts for a ComponentTree with useCancelableLayoutFutures set to false is a no-op.");
      return;
    }

    if (ThreadUtils.isMainThread()) {
      release();
    } else {
      mMainThreadHandler.post(this::release, "Release");
    }
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

  @Override
  public void addOnReleaseListener(OnReleaseListener onReleaseListener) {
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
              + (mDebugLogBreadcrumb != null ? ("/" + mDebugLogBreadcrumb) : "")
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
      handle.setStateUpdaterAndRootViewReference(componentTree, componentTree);
    }
  }

  @VisibleForTesting
  EventHandlersController getEventHandlersController() {
    return mTreeState.getEventHandlersController();
  }

  private static void flash(View view) {
    Drawable d = new PaintDrawable(Color.RED);
    d.setAlpha(128);
    view.post(
        () -> {
          d.setBounds(0, 0, view.getWidth(), view.getHeight());
          view.getOverlay().add(d);
          view.postDelayed(
              () -> {
                view.getOverlay().remove(d);
              },
              500);
        });
  }

  public int getId() {
    return mId;
  }

  public static int generateComponentTreeId() {
    return sIdGenerator.getAndIncrement();
  }

  private class DoLayoutRunnable extends ThreadTracingRunnable {
    private final ResolveResult mResolveResult;
    private final @RenderSource int mSource;
    private final int mWidthSpec;
    private final int mHeightSpec;
    private final @Nullable String mAttribution;
    private final boolean mIsCreateLayoutInProgress;

    public DoLayoutRunnable(
        final ResolveResult resolveResult,
        @RenderSource int source,
        int widthSpec,
        int heightSpec,
        @Nullable String attribution,
        boolean isCreateLayoutInProgress) {
      mResolveResult = resolveResult;
      mSource = source;
      mWidthSpec = widthSpec;
      mHeightSpec = heightSpec;
      mAttribution = attribution;
      mIsCreateLayoutInProgress = isCreateLayoutInProgress;
    }

    @Override
    public void tracedRun() {
      doLayout(
          mResolveResult,
          null,
          mSource,
          mAttribution,
          mIsCreateLayoutInProgress,
          mWidthSpec,
          mHeightSpec);
    }
  }

  private class DoResolveRunnable extends ThreadTracingRunnable {
    private final @RenderSource int mSource;
    private final Component mRoot;
    private final TreeProps mTreeProps;
    private final int mWidthSpec;
    private final int mHeightSpec;
    private final @Nullable String mAttribution;
    private final boolean mIsCreateLayoutInProgress;

    public DoResolveRunnable(
        @RenderSource int source,
        Component root,
        TreeProps treeProps,
        int widthSpec,
        int heightSpec,
        @Nullable String attribution,
        boolean isCreateLayoutInProgress) {
      mSource = source;
      mRoot = root;
      mTreeProps = treeProps;
      mWidthSpec = widthSpec;
      mHeightSpec = heightSpec;
      mAttribution = attribution;
      mIsCreateLayoutInProgress = isCreateLayoutInProgress;
    }

    @Override
    public void tracedRun() {
      doResolve(
          null,
          mSource,
          mAttribution,
          mIsCreateLayoutInProgress,
          mRoot,
          mTreeProps,
          mWidthSpec,
          mHeightSpec);
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
    public void tracedRun() {
      updateStateInternal(false, mAttribution, mIsCreateLayoutInProgress);
    }
  }

  public static final class LithoConfiguration {
    public ComponentsConfiguration mComponentsConfiguration;
    final boolean areTransitionsEnabled;
    public final boolean isReconciliationEnabled;
    final boolean isVisibilityProcessingEnabled;
    @Nullable final RunnableHandler mountContentPreallocationHandler;
    final boolean incrementalMountEnabled;
    final ErrorEventHandler errorEventHandler;
    final String logTag;
    @Nullable final ComponentsLogger logger;
    final RenderUnitIdGenerator renderUnitIdGenerator;
    @Nullable final VisibilityBoundsTransformer visibilityBoundsTransformer;
    @Nullable final ComponentTreeDebugEventListener debugEventListener;

    public final boolean preallocationPerMountContentEnabled;

    public LithoConfiguration(
        final ComponentsConfiguration config,
        boolean areTransitionsEnabled,
        boolean isReconciliationEnabled,
        boolean isVisibilityProcessingEnabled,
        boolean preallocationPerMountContentEnabled,
        @Nullable RunnableHandler mountContentPreallocationHandler,
        boolean incrementalMountEnabled,
        @Nullable ErrorEventHandler errorEventHandler,
        String logTag,
        @Nullable ComponentsLogger logger,
        RenderUnitIdGenerator renderUnitIdGenerator,
        @Nullable VisibilityBoundsTransformer visibilityBoundsTransformer,
        @Nullable ComponentTreeDebugEventListener debugEventListener) {
      this.mComponentsConfiguration = config;
      this.areTransitionsEnabled = areTransitionsEnabled;
      this.isReconciliationEnabled = isReconciliationEnabled;
      this.isVisibilityProcessingEnabled = isVisibilityProcessingEnabled;
      this.preallocationPerMountContentEnabled = preallocationPerMountContentEnabled;
      this.mountContentPreallocationHandler = mountContentPreallocationHandler;
      this.incrementalMountEnabled = incrementalMountEnabled;
      this.errorEventHandler =
          errorEventHandler == null ? DefaultErrorEventHandler.INSTANCE : errorEventHandler;
      this.logTag = logTag;
      this.logger = logger;
      this.renderUnitIdGenerator = renderUnitIdGenerator;
      this.visibilityBoundsTransformer = visibilityBoundsTransformer;
      this.debugEventListener = debugEventListener;
    }
  }

  /** A builder class that can be used to create a {@link ComponentTree}. */
  public static class Builder {

    // required
    private final ComponentContext context;
    private boolean visibilityProcessingEnabled = true;
    private Component root;

    // optional
    private @Nullable ComponentsConfiguration config;
    private boolean incrementalMountEnabled = true;
    private boolean isLayoutDiffingEnabled = true;
    private @Nullable RunnableHandler resolveThreadHandler;
    private RunnableHandler layoutThreadHandler;
    private @Nullable RunnableHandler preAllocateMountContentHandler;
    private @Nullable TreeState treeState;
    private RenderState previousRenderState;
    private int overrideComponentTreeId = INVALID_ID;
    private @Nullable MeasureListener mMeasureListener;
    private boolean shouldPreallocatePerMountSpec;
    private boolean isReconciliationEnabled = ComponentsConfiguration.isReconciliationEnabled;
    private ErrorEventHandler errorEventHandler = DefaultErrorEventHandler.INSTANCE;

    private @Nullable String logTag;
    private @Nullable ComponentsLogger logger;
    private @Nullable LithoLifecycleProvider mLifecycleProvider;

    private @Nullable RenderUnitIdGenerator mRenderUnitIdGenerator;
    private @Nullable VisibilityBoundsTransformer visibilityBoundsTransformer;
    private @Nullable ComponentTreeDebugEventListener componentTreeDebugEventListener;

    protected Builder(ComponentContext context) {
      this.context = context;
    }

    public Builder componentsConfiguration(@Nullable ComponentsConfiguration config) {
      this.config = config;
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

    public Builder withComponentTreeDebugEventListener(
        ComponentTreeDebugEventListener componentTreeDebugEventListener) {
      this.componentTreeDebugEventListener = componentTreeDebugEventListener;
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
      this.mRenderUnitIdGenerator = prevComponentTree.getLithoConfiguration().renderUnitIdGenerator;
      return this;
    }

    /**
     * This should not be used in majority of cases and should only be used when overriding previous
     * component tree id {@link ComponentTree.Builder#overrideComponentTreeId}
     *
     * @param renderUnitIdGenerator to override the {@link ComponentTree} renderUnitIgGenerator.
     */
    public Builder overrideRenderUnitIdMap(RenderUnitIdGenerator renderUnitIdGenerator) {
      this.mRenderUnitIdGenerator = renderUnitIdGenerator;
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

    // TODO: T48569046 verify the usage, if this should be split up
    public Builder logger(@Nullable ComponentsLogger logger, @Nullable String logTag) {
      this.logger = logger;
      this.logTag = logTag;
      return this;
    }

    public Builder visibilityBoundsTransformer(@Nullable VisibilityBoundsTransformer transformer) {
      visibilityBoundsTransformer = transformer;
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

      if (config == null) {
        config = context.mLithoConfiguration.mComponentsConfiguration;
      }

      if (visibilityBoundsTransformer == null) {
        visibilityBoundsTransformer = context.mLithoConfiguration.visibilityBoundsTransformer;
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

  /** This should only be used by Flipper. */
  @Nullable
  ComponentTreeTimeMachine getTimeMachine() {
    return mTimeMachine;
  }

  private void saveRevision(
      Component root,
      TreeState treeState,
      @Nullable TreeProps treeProps,
      @RenderSource int source,
      @Nullable String attribution) {
    if (mTimeMachine != null) {
      final TreeState frozenTreeState = new TreeState(treeState);
      mTimeMachine.storeRevision(root, frozenTreeState, treeProps, source, attribution);
    }
  }

  /**
   * Similar to {@link ComponentTree#setRoot(Component)}. This method allows setting a new root with
   * cached {@link TreeProps} and {@link StateHandler}.
   *
   * <p>It is used to enable time-travelling through external editors such as Flipper.
   */
  @UiThread
  protected synchronized void applyRevision(ComponentTreeTimeMachine.Revision revision) {
    ThreadUtils.assertMainThread();

    mTreeState = revision.getTreeState();
    mRootTreeProps = revision.getTreeProps();

    setRootAndSizeSpecInternal(
        revision.getRoot(),
        SIZE_UNINITIALIZED,
        SIZE_UNINITIALIZED,
        false /* isAsync */,
        null /* output */,
        RenderSource.RELOAD_PREVIOUS_STATE,
        INVALID_LAYOUT_VERSION,
        null,
        null,
        false,
        true);
  }
}
