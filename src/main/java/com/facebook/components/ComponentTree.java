/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import javax.annotation.CheckReturnValue;
import javax.annotation.concurrent.GuardedBy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.IntDef;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.view.ViewParent;

import com.facebook.infer.annotation.ReturnsOwnership;

import static com.facebook.litho.ComponentLifecycle.StateUpdate;
import static com.facebook.litho.ComponentsLogger.ACTION_SUCCESS;
import static com.facebook.litho.ComponentsLogger.EVENT_LAYOUT_CALCULATE;
import static com.facebook.litho.ComponentsLogger.EVENT_PRE_ALLOCATE_MOUNT_CONTENT;
import static com.facebook.litho.ComponentsLogger.PARAM_IS_BACKGROUND_LAYOUT;
import static com.facebook.litho.ComponentsLogger.PARAM_LOG_TAG;
import static com.facebook.litho.ComponentsLogger.PARAM_TREE_DIFF_ENABLED;
import static com.facebook.litho.ThreadUtils.assertHoldsLock;
import static com.facebook.litho.ThreadUtils.assertMainThread;
import static com.facebook.litho.ThreadUtils.isMainThread;

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
public class ComponentTree {

  private static final String TAG = ComponentTree.class.getSimpleName();
  private static final int SIZE_UNINITIALIZED = -1;
  // MainThread Looper messages:
  private static final int MESSAGE_WHAT_BACKGROUND_LAYOUT_STATE_UPDATED = 1;
  private static final String DEFAULT_LAYOUT_THREAD_NAME = "ComponentLayoutThread";
  private static final int DEFAULT_LAYOUT_THREAD_PRIORITY = Process.THREAD_PRIORITY_BACKGROUND;

  private static final int SCHEDULE_NONE = 0;
  private static final int SCHEDULE_LAYOUT_ASYNC = 1;
  private static final int SCHEDULE_LAYOUT_SYNC = 2;
  private ComponentsStethoManager mStethoManager;

  @IntDef({SCHEDULE_NONE, SCHEDULE_LAYOUT_ASYNC, SCHEDULE_LAYOUT_SYNC})
  @Retention(RetentionPolicy.SOURCE)
  private @interface PendingLayoutCalculation {}

  private static final AtomicInteger sIdGenerator = new AtomicInteger(0);
  private static final Handler sMainThreadHandler = new ComponentMainThreadHandler();
  // Do not access sDefaultLayoutThreadLooper directly, use getDefaultLayoutThreadLooper().
  @GuardedBy("ComponentTree.class")
  private static volatile Looper sDefaultLayoutThreadLooper;

  // Helpers to track view visibility when we are incrementally
  // mounting and partially invalidating
  private static final int[] sCurrentLocation = new int[2];
  private static final int[] sParentLocation = new int[2];
  private static final Rect sParentBounds = new Rect();

  private final Runnable mCalculateLayoutRunnable = new Runnable() {
    @Override
    public void run() {
      calculateLayout(null);
    }
  };

  private final ComponentContext mContext;

  // These variables are only accessed from the main thread.
  private boolean mIsMounting;
  private boolean mIncrementalMountEnabled;
  private boolean mIsLayoutDiffingEnabled;
  private boolean mIsAttached;
  private boolean mIsAsyncUpdateStateEnabled;
  private ComponentView mComponentView;
  private LayoutHandler mLayoutThreadHandler;

  @GuardedBy("this")
  private boolean mHasViewMeasureSpec;

  // TODO(6606683): Enable recycling of mComponent.
  // We will need to ensure there are no background threads referencing mComponent. We'll need
  // to keep a reference count or something. :-/
  @GuardedBy("this")
  private Component<?> mRoot;

  @GuardedBy("this")
  private int mWidthSpec = SIZE_UNINITIALIZED;

  @GuardedBy("this")
  private int mHeightSpec = SIZE_UNINITIALIZED;

  // This is written to only by the main thread with the lock held, read from the main thread with
  // no lock held, or read from any other thread with the lock held.
  private LayoutState mMainThreadLayoutState;

  // The semantics here are tricky. Whenever you transfer mBackgroundLayoutState to a local that
  // will be accessed outside of the lock, you must set mBackgroundLayoutState to null to ensure
  // that the current thread alone has access to the LayoutState, which is single-threaded.
  @GuardedBy("this")
  private LayoutState mBackgroundLayoutState;

  @GuardedBy("this")
  private StateHandler mStateHandler;

  private Object mLayoutLock;

  protected final int mId = sIdGenerator.getAndIncrement();

  @GuardedBy("this")
  private boolean mIsMeasuring;
  @GuardedBy("this")
  private @PendingLayoutCalculation int mScheduleLayoutAfterMeasure;

  public static Builder create(ComponentContext context, Component.Builder<?> root) {
    return create(context, root.build());
  }

  public static Builder create(ComponentContext context, Component<?> root) {
    return ComponentsPools.acquireComponentTreeBuilder(context, root);
  }

  protected ComponentTree(Builder builder) {
    mContext = ComponentContext.withComponentTree(builder.context, this);
    mRoot = builder.root;

    mIncrementalMountEnabled = builder.incrementalMountEnabled;
    mIsLayoutDiffingEnabled = builder.isLayoutDiffingEnabled;
    mLayoutThreadHandler = builder.layoutThreadHandler;
    mLayoutLock = builder.layoutLock;
    mIsAsyncUpdateStateEnabled = builder.asyncStateUpdates;

    if (mLayoutThreadHandler == null) {
      mLayoutThreadHandler = new DefaultLayoutHandler(getDefaultLayoutThreadLooper());
    }

    final StateHandler builderStateHandler = builder.stateHandler;
    mStateHandler = builderStateHandler == null
        ? StateHandler.acquireNewInstance(null)
        : builderStateHandler;
  }

  LayoutState getMainThreadLayoutState() {
    return mMainThreadLayoutState;
  }

  @VisibleForTesting
  protected LayoutState getBackgroundLayoutState() {
    return mBackgroundLayoutState;
  }

  /**
   * Picks the best LayoutState and sets it in mMainThreadLayoutState. The return value
   * is a LayoutState that must be released (after the lock is released). This
   * awkward contract is necessary to ensure thread-safety.
   */
  @CheckReturnValue
  @ReturnsOwnership
  private LayoutState setBestMainThreadLayoutAndReturnOldLayout() {
    assertHoldsLock(this);

    // If everything matches perfectly then we prefer mMainThreadLayoutState
    // because that means we don't need to remount.
    boolean isMainThreadLayoutBest;
    if (isCompatibleComponentAndSpec(mMainThreadLayoutState)) {
      isMainThreadLayoutBest = true;
    } else if (isCompatibleSpec(mBackgroundLayoutState, mWidthSpec, mHeightSpec)
        || !isCompatibleSpec(mMainThreadLayoutState, mWidthSpec, mHeightSpec)) {
      // If mMainThreadLayoutState isn't a perfect match, we'll prefer
      // mBackgroundLayoutState since it will have the more recent create.
      isMainThreadLayoutBest = false;
    } else {
      // If the main thread layout is still compatible size-wise, and the
      // background one is not, then we'll do nothing. We want to keep the same
      // main thread layout so that we don't force main thread re-layout.
      isMainThreadLayoutBest = true;
    }

    if (isMainThreadLayoutBest) {
      // We don't want to hold onto mBackgroundLayoutState since it's unlikely
      // to ever be used again. We return mBackgroundLayoutState to indicate it
      // should be released after exiting the lock.
      LayoutState toRelease = mBackgroundLayoutState;
      mBackgroundLayoutState = null;
      return toRelease;
    } else {
      // Since we are changing layout states we'll need to remount.
      if (mComponentView != null) {
        mComponentView.setMountStateDirty();
      }

      LayoutState toRelease = mMainThreadLayoutState;
      mMainThreadLayoutState = mBackgroundLayoutState;
      mBackgroundLayoutState = null;

      return toRelease;
    }
  }

  private void backgroundLayoutStateUpdated() {
    assertMainThread();

    // If we aren't attached, then we have nothing to do. We'll handle
    // everything in onAttach.
    if (!mIsAttached) {
      return;
    }

    LayoutState toRelease;
    boolean layoutStateUpdated;
    int componentRootId;
    synchronized (this) {
      if (mRoot == null) {
        // We have been released. Abort.
        return;
      }

      LayoutState oldMainThreadLayoutState = mMainThreadLayoutState;
      toRelease = setBestMainThreadLayoutAndReturnOldLayout();
      layoutStateUpdated = (mMainThreadLayoutState != oldMainThreadLayoutState);
      componentRootId = mRoot.getId();
    }

    if (toRelease != null) {
      toRelease.releaseRef();
      toRelease = null;
    }

    if (!layoutStateUpdated) {
      return;
    }

    // We defer until measure if we don't yet have a width/height
    int viewWidth = mComponentView.getMeasuredWidth();
    int viewHeight = mComponentView.getMeasuredHeight();
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
      mComponentView.requestLayout();
    } else {
      mountComponentIfDirty();
    }
  }

  void attach() {
    assertMainThread();

    if (mComponentView == null) {
      throw new IllegalStateException("Trying to attach a ComponentTree without a set View");
    }

    LayoutState toRelease;
    int componentRootId;
    synchronized (this) {
      // We need to track that we are attached regardless...
      mIsAttached = true;

      // ... and then we do state transfer
      toRelease = setBestMainThreadLayoutAndReturnOldLayout();
      componentRootId = mRoot.getId();
    }

    if (toRelease != null) {
      toRelease.releaseRef();
      toRelease = null;
    }

    // We defer until measure if we don't yet have a width/height
    int viewWidth = mComponentView.getMeasuredWidth();
    int viewHeight = mComponentView.getMeasuredHeight();
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

    if (needsAndroidLayout || mComponentView.isMountStateDirty()) {
      mComponentView.requestLayout();
    } else {
      mComponentView.rebind();
    }
  }

  private static boolean hasSameBaseContext(Context context1, Context context2) {
    return getBaseContext(context1) == getBaseContext(context2);
  }

  private static Context getBaseContext(Context context) {
    Context baseContext = context;
    while (baseContext instanceof ContextWrapper) {
      baseContext = ((ContextWrapper) baseContext).getBaseContext();
    }

    return baseContext;
  }

  boolean isMounting() {
    return mIsMounting;
  }

  private boolean mountComponentIfDirty() {
    if (mComponentView.isMountStateDirty()) {
      if (mIncrementalMountEnabled) {
        incrementalMountComponent();
      } else {
        mountComponent(null);
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

    // Per ComponentTree visible area. Because ComponentViews can be nested and mounted
    // not in "depth order", this variable cannot be static.
    final Rect currentVisibleArea = ComponentsPools.acquireRect();

    if (getVisibleRect(currentVisibleArea)) {
      mountComponent(currentVisibleArea);
    }
    // if false: no-op, doesn't have visible area, is not ready or not attached
    ComponentsPools.release(currentVisibleArea);
  }

  private boolean getVisibleRect(Rect visibleBounds) {
    assertMainThread();

    getLocationAndBoundsOnScreen(mComponentView, sCurrentLocation, visibleBounds);

    final ViewParent viewParent = mComponentView.getParent();
    if (viewParent instanceof View) {
      View parent = (View) viewParent;
      getLocationAndBoundsOnScreen(parent, sParentLocation, sParentBounds);
      if (!visibleBounds.setIntersect(visibleBounds, sParentBounds)) {
        return false;
      }
    }

    visibleBounds.offset(-sCurrentLocation[0], -sCurrentLocation[1]);

    return true;
  }

  private static void getLocationAndBoundsOnScreen(View view, int[] location, Rect bounds) {
    assertMainThread();

    view.getLocationOnScreen(location);
    bounds.set(
        location[0],
        location[1],
        location[0] + view.getWidth(),
        location[1] + view.getHeight());
  }

  void mountComponent(Rect currentVisibleArea) {
    assertMainThread();
    mIsMounting = true;
    // currentVisibleArea null or empty => mount all
    mComponentView.mount(mMainThreadLayoutState, currentVisibleArea);

    mIsMounting = false;
  }

  void detach() {
    assertMainThread();

    synchronized (this) {
      mIsAttached = false;
      mHasViewMeasureSpec = false;
    }
  }

  /**
   * Set a new ComponentView to this ComponentTree checking that they have the same context and
   * clear the ComponentTree reference from the previous ComponentView if any.
   * Be sure this ComponentTree is detach first.
   */
  void setComponentView(@NonNull ComponentView view) {
    assertMainThread();

    // It's possible that the view associated with this ComponentTree was recycled but was
    // never detached. In all cases we have to make sure that the old references between
    // componentView and componentTree are reset.
    if (mIsAttached) {
      if (mComponentView != null) {
        mComponentView.setComponent(null);
      } else {
        detach();
      }
    } else if (mComponentView != null) {
      // Remove the ComponentTree reference from a previous view if any.
      mComponentView.clearComponentTree();
    }

    if (!hasSameBaseContext(view.getContext(), mContext)) {
      // This would indicate bad things happening, like leaking a context.
      throw new IllegalArgumentException(
          "Base view context differs, view context is: " + view.getContext() +
              ", ComponentTree context is: " + mContext);
    }

    mComponentView = view;
  }

  void clearComponentView() {
    assertMainThread();

    // Crash if the ComponentTree is mounted to a view.
    if (mIsAttached) {
      throw new IllegalStateException(
          "Clearing the ComponentView while the ComponentTree is attached");
    }

    mComponentView = null;
  }

  void measure(int widthSpec, int heightSpec, int[] measureOutput, boolean forceLayout) {
    assertMainThread();

    Component component = null;
    LayoutState toRelease;
    synchronized (this) {
      mIsMeasuring = true;

      // This widthSpec/heightSpec is fixed until the view gets detached.
      mWidthSpec = widthSpec;
      mHeightSpec = heightSpec;
      mHasViewMeasureSpec = true;

      toRelease = setBestMainThreadLayoutAndReturnOldLayout();

      if (forceLayout || !isCompatibleComponentAndSpec(mMainThreadLayoutState)) {
        // Neither layout was compatible and we have to perform a layout.
        // Since outputs get set on the same object during the lifecycle calls,
        // we need to copy it in order to use it concurrently.
        component = mRoot.makeShallowCopy();
      }
    }

    if (toRelease != null) {
      toRelease.releaseRef();
      toRelease = null;
    }

    if (component != null) {
      // TODO: We should re-use the existing CSSNodeDEPRECATED tree instead of re-creating it.
      if (mMainThreadLayoutState != null) {
        // It's beneficial to delete the old layout state before we start creating a new one since
        // we'll be able to re-use some of the layout nodes.
        LayoutState localLayoutState;
        synchronized (this) {
          localLayoutState = mMainThreadLayoutState;
          mMainThreadLayoutState = null;
        }
        localLayoutState.releaseRef();
      }

      // We have no layout that matches the given spec, so we need to compute it on the main thread.
      LayoutState localLayoutState = calculateLayoutState(
          mLayoutLock,
          mContext,
          component,
          widthSpec,
          heightSpec,
          mIsLayoutDiffingEnabled,
          null);

      final StateHandler layoutStateStateHandler =
          localLayoutState.consumeStateHandler();
      synchronized (this) {
        if (layoutStateStateHandler != null) {
          mStateHandler.commit(layoutStateStateHandler);
          ComponentsPools.release(layoutStateStateHandler);
        }

        mMainThreadLayoutState = localLayoutState;
        localLayoutState = null;
      }

      // We need to force remount on layout
      mComponentView.setMountStateDirty();
    }

    measureOutput[0] = mMainThreadLayoutState.getWidth();
    measureOutput[1] = mMainThreadLayoutState.getHeight();

    int layoutScheduleType = SCHEDULE_NONE;
    Component root = null;

    synchronized (this) {
      mIsMeasuring = false;

      if (mScheduleLayoutAfterMeasure != SCHEDULE_NONE) {
        layoutScheduleType = mScheduleLayoutAfterMeasure;
        mScheduleLayoutAfterMeasure = SCHEDULE_NONE;
        root = mRoot.makeShallowCopy();
      }
    }

    if (layoutScheduleType != SCHEDULE_NONE) {
      setRootAndSizeSpecInternal(
          root,
          SIZE_UNINITIALIZED,
          SIZE_UNINITIALIZED,
          layoutScheduleType == SCHEDULE_LAYOUT_ASYNC,
          null /*output */);
    }
  }

  /**
   * Returns {@code true} if the layout call mounted the component.
   */
  boolean layout() {
    assertMainThread();

    return mountComponentIfDirty();
  }

  /**
   * Returns whether incremental mount is enabled or not in this component.
   */
  public boolean isIncrementalMountEnabled() {
    return mIncrementalMountEnabled;
  }

  synchronized Component getRoot() {
    return mRoot;
  }

  /**
   * Update the root component. This can happen in both attached and detached states. In each case
   * we will run a layout and then proxy a message to the main thread to cause a
   * relayout/invalidate.
   */
  public void setRoot(Component<?> rootComponent) {
    if (rootComponent == null) {
      throw new IllegalArgumentException("Root component can't be null");
    }

    setRootAndSizeSpecInternal(
        rootComponent,
        SIZE_UNINITIALIZED,
        SIZE_UNINITIALIZED,
        false /* isAsync */,
        null /* output */);
  }

  public void preAllocateMountContent() {
    assertMainThread();

    final LayoutState toPrePopulate;

    if (mMainThreadLayoutState != null) {
      toPrePopulate = mMainThreadLayoutState.acquireRef();
    } else {

      synchronized (this) {
        toPrePopulate = mBackgroundLayoutState;
        if (toPrePopulate == null) {
          return;
        }
        toPrePopulate.acquireRef();
      }
    }

    logPreAllocationStart();
    toPrePopulate.preAllocateMountContent();
    logPreAllocationFinish();

    toPrePopulate.releaseRef();
  }

  public void setRootAsync(Component<?> rootComponent) {
    if (rootComponent == null) {
      throw new IllegalArgumentException("Root component can't be null");
    }

    setRootAndSizeSpecInternal(
        rootComponent,
        SIZE_UNINITIALIZED,
        SIZE_UNINITIALIZED,
        true /* isAsync */,
        null /* output */);
  }

  synchronized void updateStateLazy(String componentKey, StateUpdate stateUpdate) {
    if (mRoot == null) {
      return;
    }

    mStateHandler.queueStateUpdate(componentKey, stateUpdate);
  }

  void updateState(String componentKey, StateUpdate stateUpdate) {
    updateStateInternal(componentKey, stateUpdate, false);
  }

  void updateStateAsync(String componentKey, StateUpdate stateUpdate) {
    if (!mIsAsyncUpdateStateEnabled) {
        throw new RuntimeException("Triggering async state updates on this component tree is " +
            "disabled, use sync state updates.");
    }
    updateStateInternal(componentKey, stateUpdate, true);
  }

  void updateStateInternal(String key, StateUpdate stateUpdate, boolean isAsync) {

    final Component<?> root;

    synchronized (this) {
      if (mRoot == null) {
        return;
      }

      mStateHandler.queueStateUpdate(key, stateUpdate);

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
    }

    setRootAndSizeSpecInternal(
        root,
        SIZE_UNINITIALIZED,
        SIZE_UNINITIALIZED,
        isAsync,
        null /*output */);
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
        output /* output */);
  }

  public void setSizeSpecAsync(int widthSpec, int heightSpec) {
    setRootAndSizeSpecInternal(
        null,
        widthSpec,
        heightSpec,
        true /* isAsync */,
        null /* output */);
  }

  /**
   * Compute asynchronously a new layout with the given component root and sizes
   */
  public void setRootAndSizeSpecAsync(Component<?> root, int widthSpec, int heightSpec) {
    if (root == null) {
      throw new IllegalArgumentException("Root component can't be null");
    }

    setRootAndSizeSpecInternal(
        root,
        widthSpec,
        heightSpec,
        true /* isAsync */,
        null /* output */);
  }

  /**
   * Compute a new layout with the given component root and sizes
   */
  public void setRootAndSizeSpec(Component<?> root, int widthSpec, int heightSpec) {
    if (root == null) {
      throw new IllegalArgumentException("Root component can't be null");
    }

    setRootAndSizeSpecInternal(
        root,
        widthSpec,
        heightSpec,
        false /* isAsync */,
        null /* output */);
  }

  public void setRootAndSizeSpec(Component<?> root, int widthSpec, int heightSpec, Size output) {
    if (root == null) {
      throw new IllegalArgumentException("Root component can't be null");
    }

    setRootAndSizeSpecInternal(
        root,
        widthSpec,
        heightSpec,
        false /* isAsync */,
        output);
  }

  /**
   * @return the {@link ComponentView} associated with this ComponentTree if any.
   */
  @Keep
  @Nullable
  public ComponentView getComponentView() {
    assertMainThread();
    return mComponentView;
  }

  /**
   * Provides a new instance from the StateHandler pool that is initialized with the information
   * from the StateHandler currently held by the ComponentTree. Once the state updates have been
   * applied and we are back in the main thread the state handler gets released to the pool.
   * @return a copy of the state handler instance held by ComponentTree.
   */
  public synchronized StateHandler getStateHandler() {
    return StateHandler.acquireNewInstance(mStateHandler);
  }

  private void setRootAndSizeSpecInternal(
      Component<?> root,
      int widthSpec,
      int heightSpec,
      boolean isAsync,
      Size output) {

    synchronized (this) {

      final Map<String, List<StateUpdate>> pendingStateUpdates =
          mStateHandler.getPendingStateUpdates();
      if (pendingStateUpdates != null && pendingStateUpdates.size() > 0 && root != null) {
        root = root.makeShallowCopyWithNewId();
      }
      final boolean rootInitialized = root != null;
      final boolean widthSpecInitialized = widthSpec != SIZE_UNINITIALIZED;
      final boolean heightSpecInitialized = heightSpec != SIZE_UNINITIALIZED;

      if (mHasViewMeasureSpec && !rootInitialized) {
        // It doesn't make sense to specify the width/height while the HostView is attached and it
        // has been measured. We do not throw an Exception only because there can be race conditions
        // that can cause this to happen. In such race conditions, ignoring the setSizeSpec call is
        // the right thing to do.
        return;
      }

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
        if (output != null) {
          output.height = mostRecentLayoutState.getHeight();
          output.width = mostRecentLayoutState.getWidth();
        }
        return;
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
