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

package com.facebook.litho;

import static android.os.Process.THREAD_PRIORITY_DEFAULT;
import static com.facebook.litho.FrameworkLogEvents.EVENT_LAYOUT_CALCULATE;
import static com.facebook.litho.FrameworkLogEvents.EVENT_PRE_ALLOCATE_MOUNT_CONTENT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_ATTRIBUTION;
import static com.facebook.litho.FrameworkLogEvents.PARAM_IS_BACKGROUND_LAYOUT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_ROOT_COMPONENT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_TREE_DIFF_ENABLED;
import static com.facebook.litho.HandlerInstrumenter.instrumentLithoHandler;
import static com.facebook.litho.LayoutState.CalculateLayoutSource;
import static com.facebook.litho.StateContainer.StateUpdate;
import static com.facebook.litho.ThreadUtils.assertHoldsLock;
import static com.facebook.litho.ThreadUtils.assertMainThread;
import static com.facebook.litho.ThreadUtils.isMainThread;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.CheckReturnValue;
import javax.annotation.concurrent.GuardedBy;

/**
 * Represents a tree of components and controls their life cycle. ComponentTree takes in a single
 * root component and recursively invokes its OnCreateLayout to create a tree of components.
 * ComponentTree is responsible for refreshing the mounted state of a component with new props.
 *
 * The usual use case for {@link ComponentTree} is:
 * <code>
 * ComponentTree component = ComponentTree.create(context, MyComponent.create());
 * myHostView.setRoot(component);
 * <code/>
 */
@ThreadSafe
public class ComponentTree {

  public static final int INVALID_ID = -1;
  private static final String TAG = ComponentTree.class.getSimpleName();
  private static final int SIZE_UNINITIALIZED = -1;
  private static final String DEFAULT_LAYOUT_THREAD_NAME = "ComponentLayoutThread";
  private static final String DEFAULT_PMC_THREAD_NAME = "PreallocateMountContentThread";
  private static final String EMPTY_STRING = "";

  private static final int SCHEDULE_NONE = 0;
  private static final int SCHEDULE_LAYOUT_ASYNC = 1;
  private static final int SCHEDULE_LAYOUT_SYNC = 2;
  private static boolean sBoostPerfLayoutStateFuture = false;
  private boolean mReleased;
  private String mReleasedComponent;

  @IntDef({SCHEDULE_NONE, SCHEDULE_LAYOUT_ASYNC, SCHEDULE_LAYOUT_SYNC})
  @Retention(RetentionPolicy.SOURCE)
  private @interface PendingLayoutCalculation {}

  public interface MeasureListener {
    void onSetRootAndSizeSpec(int width, int height);
  }

  @GuardedBy("this")
  private @Nullable MeasureListener mMeasureListener;

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
  private final boolean mNestedTreeResolutionExperimentEnabled;

  @Nullable private LithoHandler mPreAllocateMountContentHandler;

  // These variables are only accessed from the main thread.
  @ThreadConfined(ThreadConfined.UI)
  private boolean mIsMounting;
  @ThreadConfined(ThreadConfined.UI)
  private final boolean mIncrementalMountEnabled;

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

  // TODO(6606683): Enable recycling of mComponent.
  // We will need to ensure there are no background threads referencing mComponent. We'll need
  // to keep a reference count or something. :-/
  @Nullable
  @GuardedBy("this")
  private Component mRoot;

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
  @Nullable
  private LayoutState mMainThreadLayoutState;

  // The semantics here are tricky. Whenever you transfer mBackgroundLayoutState to a local that
  // will be accessed outside of the lock, you must set mBackgroundLayoutState to null to ensure
  // that the current thread alone has access to the LayoutState, which is single-threaded.
  @GuardedBy("this")
  @Nullable
  private LayoutState mBackgroundLayoutState;

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

  private final boolean mCreateInitialStateOncePerThread;

  public static Builder create(ComponentContext context, Component.Builder<?> root) {
    return create(context, root.build());
  }

  public static Builder create(ComponentContext context, @NonNull Component root) {
    if (root == null) {
      throw new NullPointerException("Creating a ComponentTree with a null root is not allowed!");
    }
    return new ComponentTree.Builder(context, root);
  }

  protected ComponentTree(Builder builder) {
    mContext = ComponentContext.withComponentTree(builder.context, this);
    mRoot = wrapRootInErrorBoundary(builder.root);

    // Incremental mount will not work if this ComponentTree is nested in a parent with it turned
    // off, so always disable it in that case
    mIncrementalMountEnabled =
        builder.incrementalMountEnabled && !mContext.isParentIncrementalMountDisabled();
    mIsLayoutDiffingEnabled = builder.isLayoutDiffingEnabled;
    mLayoutThreadHandler = builder.layoutThreadHandler;
    mShouldPreallocatePerMountSpec = builder.shouldPreallocatePerMountSpec;
    mPreAllocateMountContentHandler = builder.preAllocateMountContentHandler;

    mIsAsyncUpdateStateEnabled = builder.asyncStateUpdates;
    mHasMounted = builder.hasMounted;
    mMeasureListener = builder.mMeasureListener;
    mNestedTreeResolutionExperimentEnabled = builder.nestedTreeResolutionExperimentEnabled;
    mUseCancelableLayoutFutures = builder.useCancelableLayoutFutures;
    mMoveLayoutsBetweenThreads = builder.canInterruptAndMoveLayoutsBetweenThreads;
    isReconciliationEnabled = builder.isReconciliationEnabled;
    mCreateInitialStateOncePerThread =
        ComponentsConfiguration.createInitialStateOncePerThread || mUseCancelableLayoutFutures;

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

    if (ComponentsConfiguration.IS_INTERNAL_BUILD) {
      HotswapManager.addComponentTree(this);
    }

    // Instrument LithoHandlers.
    mMainThreadHandler = instrumentLithoHandler(mMainThreadHandler);
    mLayoutThreadHandler = ensureAndInstrumentLayoutThreadHandler(mLayoutThreadHandler);
    if (mPreAllocateMountContentHandler != null) {
      mPreAllocateMountContentHandler = instrumentLithoHandler(mPreAllocateMountContentHandler);
    }
  }

  private static LithoHandler ensureAndInstrumentLayoutThreadHandler(
      @Nullable LithoHandler handler) {
    if (handler == null) {
      handler =
          ComponentsConfiguration.threadPoolForBackgroundThreadsConfig == null
              ? new DefaultLithoHandler(getDefaultLayoutThreadLooper())
              : new ThreadPoolLayoutHandler(
                  ComponentsConfiguration.threadPoolForBackgroundThreadsConfig);
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
            mMainThreadLayoutState,
            componentRootId,
            viewWidth,
            viewHeight);

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

    final MountState mountState = mLithoView.getMountState();
    if (mountState.isDirty()) {
      mountState.collectAllTransitions(layoutState, this);
    }
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
            mMainThreadLayoutState,
            componentRootId,
            viewWidth,
            viewHeight);

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
        mountComponent(null, true);
      }

      return true;
    }

    return false;
  }

  void incrementalMountComponent() {
    assertMainThread();

    if (!mIncrementalMountEnabled) {
      throw new IllegalStateException("Calling incrementalMountComponent() but incremental mount" +
          " is not enabled");
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

  void processVisibilityOutputs() {
    assertMainThread();

    if (!mIncrementalMountEnabled) {
      throw new IllegalStateException(
          "Calling processVisibilityOutputs() but incremental mount is not enabled");
    }

    if (mLithoView == null) {
      return;
    }

    if (mMainThreadLayoutState == null) {
      Log.w(TAG, "Main Thread Layout state is not found");
      return;
    }

    final Rect currentVisibleArea = new Rect();
    if (mLithoView.getLocalVisibleRect(currentVisibleArea)) {
      mLithoView.processVisibilityOutputs(mMainThreadLayoutState, currentVisibleArea);
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
        || isCompatibleSpec(mBackgroundLayoutState, widthSpec, heightSpec);
  }

  void mountComponent(Rect currentVisibleArea, boolean processVisibilityOutputs) {
    assertMainThread();

    if (mMainThreadLayoutState == null) {
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
      mLithoView.getMountState().setIsFirstMountOfComponentTree();
      mHasMounted = true;
    }

    // currentVisibleArea null or empty => mount all
    mLithoView.mount(mMainThreadLayoutState, currentVisibleArea, processVisibilityOutputs);

    if (isDirtyMount) {
      recordRenderData(mMainThreadLayoutState);
    }

    mIsMounting = false;
    mRootHeightAnimation = null;
    mRootWidthAnimation = null;

    if (isDirtyMount) {
      mLithoView.onDirtyMountComplete();
    }
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
   * Set a new LithoView to this ComponentTree checking that they have the same context and
   * clear the ComponentTree reference from the previous LithoView if any.
   * Be sure this ComponentTree is detach first.
   */
  void setLithoView(@NonNull LithoView view) {
    assertMainThread();

    // It's possible that the view associated with this ComponentTree was recycled but was
    // never detached. In all cases we have to make sure that the old references between
    // lithoView and componentTree are reset.
    if (mIsAttached) {
      if (mLithoView != null) {
        mLithoView.setComponentTree(null);
      } else {
        detach();
      }
    } else if (mLithoView != null) {
      // Remove the ComponentTree reference from a previous view if any.
      mLithoView.clearComponentTree();
    }

    if (!hasSameRootContext(view.getContext(), mContext.getAndroidContext())) {
      // This would indicate bad things happening, like leaking a context.
      throw new IllegalArgumentException(
          "Base view context differs, view context is: " + view.getContext() +
              ", ComponentTree context is: " + mContext);
    }

    mLithoView = view;
  }

  void clearLithoView() {
    assertMainThread();

    // Crash if the ComponentTree is mounted to a view.
    if (mIsAttached) {
      throw new IllegalStateException(
          "Clearing the LithoView while the ComponentTree is attached");
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
              mIsLayoutDiffingEnabled,
              null,
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
        components = new ArrayList<>(localLayoutState.getComponents());
        attachables = localLayoutState.consumeAttachables();
        if (layoutStateStateHandler != null) {
          mStateHandler.commit(layoutStateStateHandler, mNestedTreeResolutionExperimentEnabled);
        }

        localLayoutState.clearComponents();
        mMainThreadLayoutState = localLayoutState;
      }

      final AttachDetachHandler attachDetachHandler = mContext.getAttachDetachHandler();
      if (attachDetachHandler != null) {
        attachDetachHandler.onAttached(attachables);
      } else if (attachables != null) {
        mContext.getOrCreateAttachDetachHandler().onAttached(attachables);
      }

      bindEventAndTriggerHandlers(components);

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
          null,
          rootTreeProps);
    }
  }

  /**
   * Returns {@code true} if the layout call mounted the component.
   */
  boolean layout() {
    assertMainThread();

    return mountComponentIfNeeded();
  }

  /**
   * Returns whether incremental mount is enabled or not in this component.
   */
  public boolean isIncrementalMountEnabled() {
    return mIncrementalMountEnabled;
  }

  /** Whether the refactored implementation of nested tree resolution should be used. */
  public boolean isNestedTreeResolutionExperimentEnabled() {
    return mNestedTreeResolutionExperimentEnabled;
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
  public void setRoot(Component rootComponent) {
    if (rootComponent == null) {
      throw new IllegalArgumentException("Root component can't be null");
    }

    setRootAndSizeSpecAndWrapper(
        rootComponent,
        SIZE_UNINITIALIZED,
        SIZE_UNINITIALIZED,
        false /* isAsync */,
        null /* output */,
        CalculateLayoutSource.SET_ROOT_SYNC,
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
    final ComponentsLogger logger = mContext.getLogger();
    final PerfEvent event =
        logger != null
            ? LogTreePopulator.populatePerfEventFromLogger(
                mContext,
                logger,
                logger.newPerformanceEvent(mContext, EVENT_PRE_ALLOCATE_MOUNT_CONTENT))
            : null;

    toPrePopulate.preAllocateMountContent(shouldPreallocatePerMountSpec);

    if (logger != null) {
      logger.logPerfEvent(event);
    }
  }

  public void setRootAsync(Component rootComponent) {
    if (rootComponent == null) {
      throw new IllegalArgumentException("Root component can't be null");
    }

    setRootAndSizeSpecAndWrapper(
        rootComponent,
        SIZE_UNINITIALIZED,
        SIZE_UNINITIALIZED,
        true /* isAsync */,
        null /* output */,
        CalculateLayoutSource.SET_ROOT_ASYNC,
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

    LithoStats.incStateUpdateLazy(1);
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

  void updateStateSync(String componentKey, StateUpdate stateUpdate, String attribution) {

    synchronized (this) {
      if (mRoot == null) {
        return;
      }

      mStateHandler.queueStateUpdate(componentKey, stateUpdate, false);
    }

    LithoStats.incStateUpdateSync(1);
    final Looper looper = Looper.myLooper();

    if (looper == null) {
      Log.w(
          TAG,
          "You cannot update state synchronously from a thread without a looper, " +
              "using the default background layout thread instead");
      synchronized (mUpdateStateSyncRunnableLock) {
        if (mUpdateStateSyncRunnable != null) {
          mLayoutThreadHandler.remove(mUpdateStateSyncRunnable);
        }
        mUpdateStateSyncRunnable = new UpdateStateSyncRunnable(attribution);

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
      mUpdateStateSyncRunnable = new UpdateStateSyncRunnable(attribution);

      String tag = EMPTY_STRING;
      if (handler.isTracing()) {
        tag = "updateStateSync " + attribution;
      }
      handler.post(mUpdateStateSyncRunnable, tag);
    }
  }

  void updateStateAsync(String componentKey, StateUpdate stateUpdate, String attribution) {
    if (!mIsAsyncUpdateStateEnabled) {
      throw new RuntimeException("Triggering async state updates on this component tree is " +
          "disabled, use sync state updates.");
    }

    synchronized (this) {
      if (mRoot == null) {
        return;
      }

      mStateHandler.queueStateUpdate(componentKey, stateUpdate, false);
    }

    updateStateInternal(true, attribution);
  }

  void updateStateInternal(boolean isAsync, String attribution) {

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
        attribution,
        rootTreeProps);
  }

  StateHandler getStateHandler() {
    return mStateHandler;
  }

  boolean shouldCreateInitialStateOncePerThread() {
    return mCreateInitialStateOncePerThread;
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
   * Same as {@link #setSizeSpec(int, int)} but fetches the resulting width/height
   * in the given {@link Size}.
   */
  public void setSizeSpec(int widthSpec, int heightSpec, Size output) {
    setRootAndSizeSpecInternal(
        null,
        widthSpec,
        heightSpec,
        false /* isAsync */,
        output /* output */,
        CalculateLayoutSource.SET_SIZE_SPEC_SYNC,
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
        null,
        null);
  }

  /**
   * Compute asynchronously a new layout with the given component root and sizes
   */
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
        null,
        treeProps);
  }

  /**
   * Compute a new layout with the given component root and sizes
   */
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
        null,
        treeProps);
  }

  /**
   * @return the {@link LithoView} associated with this ComponentTree if any.
   */
  @Keep
  @Nullable
  public LithoView getLithoView() {
    assertMainThread();
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
      componentKeysToBounds =
          mMainThreadLayoutState.getComponentKeyToBounds();
    }

    if (!componentKeysToBounds.containsKey(anchorGlobalKey)) {
      throw new IllegalArgumentException(
          "Cannot find a component with key " + anchorGlobalKey + " to use as anchor.");
    }

    final Rect anchorBounds = componentKeysToBounds.get(anchorGlobalKey);
    LithoTooltipController.showOnAnchor(
        tooltip,
        anchorBounds,
        mLithoView,
        tooltipPosition,
        xOffset,
        yOffset);
  }

  void showTooltip(LithoTooltip lithoTooltip, String anchorGlobalKey, int xOffset, int yOffset) {
    assertMainThread();

    final Map<String, Rect> componentKeysToBounds;
    synchronized (this) {
      componentKeysToBounds = mMainThreadLayoutState.getComponentKeyToBounds();
    }

    if (!componentKeysToBounds.containsKey(anchorGlobalKey)) {
      throw new IllegalArgumentException(
          "Cannot find a component with key " + anchorGlobalKey + " to use as anchor.");
    }

    final Rect anchorBounds = componentKeysToBounds.get(anchorGlobalKey);
    lithoTooltip.showLithoTooltip(mLithoView, anchorBounds, xOffset, yOffset);
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
      String extraAttribution,
      @Nullable TreeProps treeProps) {

    setRootAndSizeSpecInternal(
        wrapRootInErrorBoundary(root),
        widthSpec,
        heightSpec,
        isAsync,
        output,
        source,
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
      String extraAttribution,
      @Nullable TreeProps treeProps) {
    synchronized (this) {
      if (mReleased) {
        // If this is coming from a background thread, we may have been released from the main
        // thread. In that case, do nothing.
        //
        // NB: This is only safe because we don't re-use released ComponentTrees.
        return;
      }

      final Map<String, List<StateUpdate>> pendingStateUpdates =
          mStateHandler == null ? null : mStateHandler.getPendingStateUpdates();
      if (pendingStateUpdates != null && pendingStateUpdates.size() > 0 && root != null) {
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
          widthSpecInitialized &&
              heightSpecInitialized &&
              mWidthSpec != SIZE_UNINITIALIZED &&
              mHeightSpec != SIZE_UNINITIALIZED;
      final boolean sizeSpecsAreCompatible =
          sizeSpecDidntChange ||
              (allSpecsWereInitialized &&
                  mostRecentLayoutState != null &&
                  LayoutState.hasCompatibleSizeSpec(
                      mWidthSpec,
                      mHeightSpec,
                      widthSpec,
                      heightSpec,
                      mostRecentLayoutState.getWidth(),
                      mostRecentLayoutState.getHeight()));
      final boolean rootDidntChange = !rootInitialized || root.getId() == mRoot.getId();

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
      throw new IllegalArgumentException("The layout can't be calculated asynchronously if" +
          " we need the Size back");
    }

    final LayoutStateFuture layoutStateFuture =
        mUseCancelableLayoutFutures
            ? createLayoutStateFutureAndCancelRunning(
                mContext, mIsLayoutDiffingEnabled, treeProps, source, extraAttribution)
            : null;

    if (isAsync) {
      synchronized (mCurrentCalculateLayoutRunnableLock) {
        if (mCurrentCalculateLayoutRunnable != null) {
          mLayoutThreadHandler.remove(mCurrentCalculateLayoutRunnable);
        }
        mCurrentCalculateLayoutRunnable =
            new CalculateLayoutRunnable(source, treeProps, extraAttribution, layoutStateFuture);

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
      calculateLayout(output, source, extraAttribution, treeProps, layoutStateFuture);
    }
  }

  /**
   * Calculates the layout.
   *
   * @param output a destination where the size information should be saved
   * @param treeProps Saved TreeProps to be used as parent input
   */
  private void calculateLayout(
      @Nullable Size output,
      @CalculateLayoutSource int source,
      @Nullable String extraAttribution,
      @Nullable TreeProps treeProps,
      @Nullable LayoutStateFuture layoutStateFuture) {
    final int widthSpec;
    final int heightSpec;
    final Component root;
    LayoutState previousLayoutState = null;

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

      if (mMainThreadLayoutState != null) {
        previousLayoutState = mMainThreadLayoutState;
      }
    }

    final ComponentsLogger logger = mContext.getLogger();
    final PerfEvent layoutEvent =
        logger != null
            ? LogTreePopulator.populatePerfEventFromLogger(
                mContext, logger, logger.newPerformanceEvent(mContext, EVENT_LAYOUT_CALCULATE))
            : null;

    if (layoutEvent != null) {
      layoutEvent.markerAnnotate(PARAM_ROOT_COMPONENT, root.getSimpleName());
      layoutEvent.markerAnnotate(PARAM_IS_BACKGROUND_LAYOUT, !ThreadUtils.isMainThread());
      layoutEvent.markerAnnotate(PARAM_TREE_DIFF_ENABLED, mIsLayoutDiffingEnabled);
      layoutEvent.markerAnnotate(PARAM_ATTRIBUTION, extraAttribution);
    }

    LayoutState localLayoutState =
        layoutStateFuture == null
            ? calculateLayoutState(
                mContext,
                root,
                widthSpec,
                heightSpec,
                mIsLayoutDiffingEnabled,
                previousLayoutState,
                treeProps,
                source,
                extraAttribution)
            : calculateLayoutState(layoutStateFuture);

    if (localLayoutState == null) {
      if (output != null) {
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
      if (noCompatibleComponent) {
        final StateHandler layoutStateStateHandler = localLayoutState.consumeStateHandler();
        if (layoutStateStateHandler != null) {
          if (mStateHandler != null) { // we could have been released
            mStateHandler.commit(layoutStateStateHandler, mNestedTreeResolutionExperimentEnabled);
          }
        }

        if (mMeasureListener != null) {
          rootWidth = localLayoutState.getWidth();
          rootHeight = localLayoutState.getHeight();
        }

        components = new ArrayList<>(localLayoutState.getComponents());
        localLayoutState.clearComponents();

        attachables = localLayoutState.consumeAttachables();

        // Set the new layout state.
        mBackgroundLayoutState = localLayoutState;
        layoutStateUpdated = true;
      }
    }

    if (noCompatibleComponent) {
      if (mMeasureListener != null) {
        mMeasureListener.onSetRootAndSizeSpec(rootWidth, rootHeight);
      }
      final AttachDetachHandler attachDetachHandler = mContext.getAttachDetachHandler();
      if (attachDetachHandler != null) {
        attachDetachHandler.onAttached(attachables);
      } else if (attachables != null) {
        mContext.getOrCreateAttachDetachHandler().onAttached(attachables);
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

    if (layoutEvent != null) {
      logger.logPerfEvent(layoutEvent);
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
   * Transfer mBackgroundLayoutState to mMainThreadLayoutState. This will proxy
   * to the main thread if necessary. If the component/size-spec changes in the
   * meantime, then the transfer will be aborted.
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
   * The contract is that in order to release a ComponentTree, you must do so from the main
   * thread, or guarantee that it will never be accessed from the main thread again. Usually
   * HostView will handle releasing, but if you never attach to a host view, then you should call
   * release yourself.
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
      mReleasedComponent = mRoot.getSimpleName();
      if (mLithoView != null) {
        mLithoView.setComponentTree(null);
      }
      mRoot = null;

      // Clear mWorkingRangeStatusHandler before releasing LayoutState because we need them to help
      // dispatch OnExitRange events.
      clearWorkingRangeStatusHandler();

      mMainThreadLayoutState = null;
      mBackgroundLayoutState = null;
      mStateHandler = null;
      mPreviousRenderState = null;
      mPreviousRenderStateSetFromBuilder = false;
    }

    synchronized (mEventTriggersContainer) {
      clearUnusedTriggerHandlers();
    }

    final AttachDetachHandler attachDetachHandler = mContext.getAttachDetachHandler();
    if (attachDetachHandler != null) {
      // Execute detached callbacks if necessary.
      attachDetachHandler.onDetached();
    }
  }

  @GuardedBy("this")
  private boolean isCompatibleComponentAndSpec(LayoutState layoutState) {
    assertHoldsLock(this);

    return mRoot != null && isCompatibleComponentAndSpec(
        layoutState, mRoot.getId(), mWidthSpec, mHeightSpec);
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

    return mWidthSpec != SIZE_UNINITIALIZED
        && mHeightSpec != SIZE_UNINITIALIZED;
  }

  @Nullable
  public synchronized String getSimpleName() {
    return mRoot == null ? null : mRoot.getSimpleName();
  }

  @Nullable
  Object getCachedValue(Object cachedValueInputs) {
    if (isReleased()) {
      return null;
    }
    return mStateHandler.getCachedValue(cachedValueInputs);
  }

  void putCachedValue(Object cachedValueInputs, Object cachedValue) {
    if (isReleased()) {
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

  private static boolean isCompatibleSpec(
      LayoutState layoutState, int widthSpec, int heightSpec) {
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

  /**
   * If there is another compatible layout running, wait on it. Otherwise, release all
   * LayoutStateFutures that are currently running, because the layout that is about to be scheduled
   * will make their result redundant.
   *
   * @return the LayoutStateFuture which will return the result for the current layout.
   */
  private LayoutStateFuture createLayoutStateFutureAndCancelRunning(
      ComponentContext context,
      boolean diffingEnabled,
      @Nullable TreeProps treeProps,
      @CalculateLayoutSource int source,
      @Nullable String extraAttribution) {

    final int widthSpec;
    final int heightSpec;
    final Component root;
    LayoutState previousLayoutState = null;

    synchronized (this) {
      // Can't compute a layout if specs or root are missing
      if (!hasSizeSpec() || mRoot == null) {
        return null;
      }

      widthSpec = mWidthSpec;
      heightSpec = mHeightSpec;
      mPendingLayoutWidthSpec = widthSpec;
      mPendingLayoutHeightSpec = heightSpec;
      root = mRoot.makeShallowCopy();

      if (mMainThreadLayoutState != null) {
        previousLayoutState = mMainThreadLayoutState;
      }
    }

    LayoutStateFuture localLayoutStateFuture =
        new LayoutStateFuture(
            context,
            root,
            widthSpec,
            heightSpec,
            diffingEnabled,
            previousLayoutState,
            treeProps,
            source,
            extraAttribution);
    final boolean waitingFromSyncLayout = localLayoutStateFuture.isFromSyncLayout;

    synchronized (mLayoutStateFutureLock) {
      boolean canReuse = false;
      for (int i = 0; i < mLayoutStateFutures.size(); i++) {
        final LayoutStateFuture runningFuture = mLayoutStateFutures.get(i);
        if (!runningFuture.isReleased() && runningFuture.equals(localLayoutStateFuture)) {
          // Use the latest LayoutState calculation if it's the same.
          localLayoutStateFuture = runningFuture;
          canReuse = true;
        } else {
          // This means the local layout state future will be run to get the result.
          // We can cancel all other LayoutStateFutures which are currently running if they are not
          // needed to get a sync result.
          if (runningFuture.canBeCancelled()) {
            runningFuture.release();
          }
        }
      }

      localLayoutStateFuture.registerForResponse(waitingFromSyncLayout);

      if (!canReuse) {
        mLayoutStateFutures.add(localLayoutStateFuture);
      }
    }

    return localLayoutStateFuture;
  }

  /** Calculates a LayoutState for the given LayoutStateFuture on the thread that calls this. */
  private @Nullable LayoutState calculateLayoutState(LayoutStateFuture layoutStateFuture) {
    final LayoutState layoutState = layoutStateFuture.runAndGet();

    synchronized (mLayoutStateFutureLock) {
      layoutStateFuture.unregisterForResponse();

      // This future has finished executing, if no other threads were waiting for the response we
      // can remove it.
      if (layoutStateFuture.getWaitingCount() == 0) {
        layoutStateFuture.release();
        mLayoutStateFutures.remove(layoutStateFuture);
      }
    }

    return layoutState;
  }

  protected @Nullable LayoutState calculateLayoutState(
      ComponentContext context,
      Component root,
      int widthSpec,
      int heightSpec,
      boolean diffingEnabled,
      @Nullable LayoutState previousLayoutState,
      @Nullable TreeProps treeProps,
      @CalculateLayoutSource int source,
      @Nullable String extraAttribution) {

      LayoutStateFuture localLayoutStateFuture =
          new LayoutStateFuture(
              context,
              root,
              widthSpec,
              heightSpec,
              diffingEnabled,
              previousLayoutState,
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

      final LayoutState layoutState = localLayoutStateFuture.runAndGet();

      synchronized (mLayoutStateFutureLock) {
        localLayoutStateFuture.unregisterForResponse();

        // This future has finished executing, if no other threads were waiting for the response we
        // can remove it.
        if (localLayoutStateFuture.getWaitingCount() == 0) {
          localLayoutStateFuture.release();
          mLayoutStateFutures.remove(localLayoutStateFuture);
        }
      }

      return layoutState;
  }

  private LayoutState calculateLayoutStateInternal(
      ComponentContext context,
      Component root,
      int widthSpec,
      int heightSpec,
      boolean diffingEnabled,
      @Nullable LayoutState previousLayoutState,
      @Nullable TreeProps treeProps,
      @CalculateLayoutSource int source,
      @Nullable String extraAttribution,
      @Nullable LayoutStateFuture layoutStateFuture) {
    final ComponentContext contextWithStateHandler =
        getNewContextForLayout(context, treeProps, layoutStateFuture);

    return LayoutState.calculate(
        contextWithStateHandler,
        root,
        mId,
        widthSpec,
        heightSpec,
        diffingEnabled,
        previousLayoutState,
        source,
        extraAttribution);
  }

  private ComponentContext getNewContextForLayout(
      ComponentContext context,
      @Nullable TreeProps treeProps,
      @Nullable LayoutStateFuture layoutStateFuture) {
    final ComponentContext contextWithStateHandler;

    synchronized (this) {
      final KeyHandler keyHandler =
          (ComponentsConfiguration.useGlobalKeys || ComponentsConfiguration.isDebugModeEnabled)
              ? new KeyHandler(mContext.getLogger())
              : null;

      contextWithStateHandler =
          new ComponentContext(
              context,
              StateHandler.createNewInstance(mStateHandler),
              keyHandler,
              treeProps,
              layoutStateFuture);
    }

    return contextWithStateHandler;
  }

  @VisibleForTesting
  List<LayoutStateFuture> getLayoutStateFutures() {
    return mLayoutStateFutures;
  }

  /** Wraps a {@link FutureTask} to deduplicate calculating the same LayoutState across threads. */
  @VisibleForTesting
  class LayoutStateFuture {

    private final AtomicInteger runningThreadId = new AtomicInteger(-1);
    private final ComponentContext context;
    private final Component root;
    private final int widthSpec;
    private final int heightSpec;
    private final boolean diffingEnabled;
    @Nullable private final LayoutState previousLayoutState;
    @Nullable private final TreeProps treeProps;
    private final FutureTask<LayoutState> futureTask;
    private final AtomicInteger refCount = new AtomicInteger(0);
    private final boolean isFromSyncLayout;
    private final AtomicBoolean interrupted = new AtomicBoolean(false);
    private final int source;
    private final String extraAttribution;

    @GuardedBy("LayoutStateFuture.this")
    private volatile boolean released = false;

    @GuardedBy("LayoutStateFuture.this")
    @Nullable
    private volatile LayoutState layoutState = null;

    private boolean isBlockingSyncLayout;

    private LayoutStateFuture(
        final ComponentContext context,
        final Component root,
        final int widthSpec,
        final int heightSpec,
        final boolean diffingEnabled,
        @Nullable final LayoutState previousLayoutState,
        @Nullable final TreeProps treeProps,
        @CalculateLayoutSource final int source,
        @Nullable final String extraAttribution) {
      this.context = context;
      this.root = root;
      this.widthSpec = widthSpec;
      this.heightSpec = heightSpec;
      this.diffingEnabled = diffingEnabled;
      this.previousLayoutState = previousLayoutState;
      this.treeProps = treeProps;
      this.isFromSyncLayout = isFromSyncLayout(source);
      this.source = source;
      this.extraAttribution = extraAttribution;

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
                  final LayoutState result =
                      calculateLayoutStateInternal(
                          context,
                          root,
                          widthSpec,
                          heightSpec,
                          diffingEnabled,
                          previousLayoutState,
                          treeProps,
                          source,
                          extraAttribution,
                          ComponentTree.this.mMoveLayoutsBetweenThreads
                                  || ComponentTree.this.mUseCancelableLayoutFutures
                              ? LayoutStateFuture.this
                              : null);
                  synchronized (LayoutStateFuture.this) {
                    if (released) {
                      return null;
                    } else {
                      layoutState = result;
                      return result;
                    }
                  }
                }
              });
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
      layoutState = null;
      released = true;
    }

    boolean isReleased() {
      return released;
    }

    boolean isInterrupted() {
      return !ThreadUtils.isMainThread() && interrupted.get();
    }

    void interrupt() {
      interrupted.set(true);
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
    LayoutState runAndGet() {
      if (mMoveLayoutsBetweenThreads) {
        return runAndGetSwitchThreadsWhenUIBlocked();
      }

      return runAndGetIncreasePriority();
    }

    @VisibleForTesting
    @Nullable
    LayoutState runAndGetIncreasePriority() {
      if (runningThreadId.compareAndSet(-1, Process.myTid())) {
        futureTask.run();
      }

      final int runningThreadId = this.runningThreadId.get();
      final int originalThreadPriority;
      final boolean didRaiseThreadPriority;
      final boolean notRunningOnMyThread = runningThreadId != Process.myTid();

      if (isMainThread() && !futureTask.isDone() && notRunningOnMyThread) {
        // Main thread is about to be blocked, raise the running thread priority.
        originalThreadPriority =
            ComponentsConfiguration.inheritPriorityFromUiThread
                ? ThreadUtils.tryInheritThreadPriorityFromCurrentThread(runningThreadId)
                : ThreadUtils.tryRaiseThreadPriority(
                    runningThreadId, Process.THREAD_PRIORITY_DISPLAY);
        didRaiseThreadPriority = true;

      } else {
        originalThreadPriority = THREAD_PRIORITY_DEFAULT;
        didRaiseThreadPriority = false;
      }

      final LayoutState result;
      try {
        final boolean shouldTrace = notRunningOnMyThread && ComponentsSystrace.isTracing();
        if (shouldTrace) {
          ComponentsSystrace.beginSectionWithArgs("LayoutStateFuture.get")
              .arg("root", root.getSimpleName())
              .arg("runningThreadId", runningThreadId)
              .flush();
        }
        result = futureTask.get();
        if (shouldTrace) {
          ComponentsSystrace.endSection();
        }
      } catch (ExecutionException e) {
        final Throwable cause = e.getCause();
        if (cause instanceof RuntimeException) {
          throw (RuntimeException) cause;
        } else {
          throw new RuntimeException(e.getMessage(), e);
        }
      } catch (InterruptedException | CancellationException e) {
        throw new RuntimeException(e.getMessage(), e);
      } finally {
        if (didRaiseThreadPriority) {
          // Reset the running thread's priority after we're unblocked.
          try {
            Process.setThreadPriority(runningThreadId, originalThreadPriority);
          } catch (IllegalArgumentException | SecurityException ignored) {
          }
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

    @VisibleForTesting
    @Nullable
    LayoutState runAndGetSwitchThreadsWhenUIBlocked() {
      if (runningThreadId.compareAndSet(-1, Process.myTid())) {
        futureTask.run();
      }

      final int runningThreadId = this.runningThreadId.get();

      if (isMainThread() && !futureTask.isDone() && runningThreadId != Process.myTid()) {
        // This means the UI thread is about to be blocked by the bg thread. Instead of waiting,
        // the bg task is interrupted.
        if (!isFromSyncLayout) {
          interrupt();
        }
      }

      LayoutState result;
      try {
        result = futureTask.get();
        if (interrupted.get() && result.isPartialLayoutState()) {
          if (ThreadUtils.isMainThread()) {
            // This means that the bg task was interrupted and it returned a partially resolved
            // InternalNode. We need to finish computing this LayoutState.

            result = resolvePartialInternalNodeAndCalculateLayout(result);
          } else {
            // This means that the bg task was interrupted and the UI thread will pick up the rest
            // of
            // the work. No need to return a LayoutState.
            result = null;
          }
        }

      } catch (ExecutionException | InterruptedException | CancellationException e) {
        final Throwable cause = e.getCause();
        if (cause instanceof RuntimeException) {
          throw (RuntimeException) cause;
        } else {
          throw new RuntimeException(e.getMessage(), e);
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
          LayoutState.resumeCalculate(
              getNewContextForLayout(context, treeProps, null),
              source,
              extraAttribution,
              partialLayoutState);

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
    private final @Nullable LayoutStateFuture mLayoutStateFuture;

    public CalculateLayoutRunnable(
        @CalculateLayoutSource int source,
        @Nullable TreeProps treeProps,
        String attribution,
        @Nullable LayoutStateFuture layoutStateFuture) {
      mSource = source;
      mTreeProps = treeProps;
      mAttribution = attribution;
      mLayoutStateFuture = layoutStateFuture;
    }

    @Override
    public void tracedRun(Throwable tracedThrowable) {
      calculateLayout(null, mSource, mAttribution, mTreeProps, mLayoutStateFuture);
    }
  }

  private final class UpdateStateSyncRunnable extends ThreadTracingRunnable {

    private final String mAttribution;

    public UpdateStateSyncRunnable(String attribution) {
      mAttribution = attribution;
    }

    @Override
    public void tracedRun(Throwable tracedThrowable) {
      updateStateInternal(false, mAttribution);
    }
  }

  public synchronized void updateMeasureListener(@Nullable MeasureListener measureListener) {
    mMeasureListener = measureListener;
  }

  /**
   * A builder class that can be used to create a {@link ComponentTree}.
   */
  public static class Builder {

    // required
    private final ComponentContext context;
    private final Component root;

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
    private boolean nestedTreeResolutionExperimentEnabled =
        ComponentsConfiguration.isNestedTreeResolutionExperimentEnabled;
    private boolean isReconciliationEnabled = ComponentsConfiguration.isReconciliationEnabled;
    private boolean canInterruptAndMoveLayoutsBetweenThreads =
        ComponentsConfiguration.canInterruptAndMoveLayoutsBetweenThreads;
    private boolean splitLayoutForMeasureAndRangeEstimation =
        ComponentsConfiguration.splitLayoutForMeasureAndRangeEstimation;
    private boolean useCancelableLayoutFutures = ComponentsConfiguration.useCancelableLayoutFutures;

    protected Builder(ComponentContext context, Component root) {
      this.context = context;
      this.root = root;
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

    /**
     * Whether or not to enable layout tree diffing. This will reduce the cost of
     * updates at the expense of using extra memory. True by default.
     *
     * @Deprecated We will remove this option soon, please consider turning it on (which is on by
     * default)
     */
    public Builder layoutDiffing(boolean enabled) {
      isLayoutDiffingEnabled = enabled;
      return this;
    }

    /**
     * Specify the looper to use for running layouts on. Note that in rare cases
     * layout must run on the UI thread. For example, if you rotate the screen,
     * we must measure on the UI thread. If you don't specify a Looper here, the
     * Components default Looper will be used.
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

    /**
     * Specify whether the ComponentTree allows async state updates. This is enabled by default.
     */
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

    /**
     * Whether the refactored implementation of nested tree resolution should be used. This
     * implementation fixes the following issue during nested tree resolution:
     *
     * <ul>
     *   <li>disallows overriding global keys
     *   <li>fixes incorrect global key generation
     *   <li>applies state updates only once
     * </ul>
     */
    public Builder enableNestedTreeResolutionExperiment(boolean isEnabled) {
      this.nestedTreeResolutionExperimentEnabled = isEnabled;
      return this;
    }


    /** Sets if is reconciliation is enabled */
    public Builder isReconciliationEnabled(boolean isEnabled) {
      this.isReconciliationEnabled = isEnabled;
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

    /** Builds a {@link ComponentTree} using the parameters specified in this builder. */
    public ComponentTree build() {
      return new ComponentTree(this);
    }
  }
}
