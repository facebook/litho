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

package com.facebook.litho.widget;

import static com.facebook.litho.LithoLifecycleProvider.LithoLifecycle.DESTROYED;
import static com.facebook.litho.ThreadUtils.assertMainThread;

import androidx.annotation.IntDef;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.ComponentTree.MeasureListener;
import com.facebook.litho.ComponentTree.WorkingRangeAndPositionHolder;
import com.facebook.litho.ComponentTree.WorkingRangeHolder;
import com.facebook.litho.ErrorEventHandler;
import com.facebook.litho.LithoLifecycleListener;
import com.facebook.litho.LithoLifecycleProvider;
import com.facebook.litho.LithoLifecycleProviderDelegate;
import com.facebook.litho.Size;
import com.facebook.litho.TreeProps;
import com.facebook.litho.TreeState;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.rendercore.RunnableHandler;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/**
 * A class used to store the data backing a {@link RecyclerBinder}. For each item the
 * ComponentTreeHolder keeps the {@link RenderInfo} which contains the original {@link Component}
 * and either the {@link ComponentTree} or the {@link TreeState} depending upon whether the item is
 * within the current working range or not.
 */
@ThreadSafe
public class ComponentTreeHolder {
  private static final int UNINITIALIZED = -1;
  private static final AtomicInteger sIdGenerator = new AtomicInteger(1);
  private final boolean mCanInterruptAndMoveLayoutsBetweenThreads;
  private final boolean mIsReconciliationEnabled;
  private final boolean mIsLayoutDiffingEnabled;
  public static final String PREVENT_RELEASE_TAG = "prevent_release";
  public static final String ACQUIRE_STATE_HANDLER_ON_RELEASE = "acquire_state_handler";
  private final @Nullable LithoLifecycleProvider mParentLifecycle;
  private @Nullable ComponentTreeHolderLifecycleProvider mComponentTreeHolderLifecycleProvider;
  private final @Nullable ErrorEventHandler mErrorEventHandler;
  private final ComponentsConfiguration mComponentsConfiguration;

  @IntDef({RENDER_UNINITIALIZED, RENDER_ADDED, RENDER_DRAWN})
  public @interface RenderState {}

  static final int RENDER_UNINITIALIZED = 0;
  static final int RENDER_ADDED = 1;
  static final int RENDER_DRAWN = 2;

  interface ComponentTreeMeasureListenerFactory {
    @Nullable
    MeasureListener create(ComponentTreeHolder holder);
  }

  private @Nullable final ComponentTreeMeasureListenerFactory mComponentTreeMeasureListenerFactory;
  private final AtomicInteger mRenderState = new AtomicInteger(RENDER_UNINITIALIZED);
  private final int mId;
  private final RunnableHandler mPreallocateMountContentHandler;
  private final boolean mShouldPreallocatePerMountSpec;
  private final boolean mIncrementalMount;
  private final boolean mVisibilityProcessingEnabled;

  @GuardedBy("this")
  private boolean mIsTreeValid;

  @GuardedBy("this")
  private @Nullable RunnableHandler mResolveHandler;

  @GuardedBy("this")
  private @Nullable RunnableHandler mLayoutHandler;

  @GuardedBy("this")
  private boolean mIsInserted = true;

  @GuardedBy("this")
  private boolean mHasMounted = false;

  @GuardedBy("this")
  private int mLastMeasuredHeight;

  @GuardedBy("this")
  private @Nullable ComponentTree mComponentTree;

  @GuardedBy("this")
  private TreeState mTreeState;

  @GuardedBy("this")
  private RenderInfo mRenderInfo;

  @GuardedBy("this")
  private @Nullable ComponentTree.NewLayoutStateReadyListener mPendingNewLayoutListener;

  @GuardedBy("this")
  private int mLastRequestedWidthSpec = UNINITIALIZED;

  @GuardedBy("this")
  private int mLastRequestedHeightSpec = UNINITIALIZED;

  @GuardedBy("this")
  private WorkingRangeAndPositionHolder mPendingWorkingRangeAndPosition = null;

  public static Builder create() {
    return new Builder();
  }

  public static class Builder {

    private RenderInfo renderInfo;
    private ComponentsConfiguration componentsConfiguration =
        ComponentsConfiguration.getDefaultComponentsConfiguration();
    private @Nullable RunnableHandler resolveHandler;
    private RunnableHandler layoutHandler;
    private ComponentTreeMeasureListenerFactory componentTreeMeasureListenerFactory;
    private @Nullable RunnableHandler preallocateMountContentHandler;
    private boolean shouldPreallocatePerMountSpec;
    private boolean incrementalMount = true;
    private boolean canInterruptAndMoveLayoutsBetweenThreads;
    private boolean isReconciliationEnabled = ComponentsConfiguration.isReconciliationEnabled;
    private boolean isLayoutDiffingEnabled = ComponentsConfiguration.isLayoutDiffingEnabled;
    private int recyclingMode;
    private boolean visibilityProcessingEnabled = true;
    private @Nullable LithoLifecycleProvider parentLifecycle;
    private @Nullable ErrorEventHandler errorEventHandler;

    private Builder() {}

    public Builder renderInfo(RenderInfo renderInfo) {
      this.renderInfo = renderInfo == null ? ComponentRenderInfo.createEmpty() : renderInfo;
      return this;
    }

    public Builder componentsConfiguration(ComponentsConfiguration componentsConfiguration) {
      this.componentsConfiguration = componentsConfiguration;
      return this;
    }

    public Builder resolveHandler(@Nullable RunnableHandler resolveHandler) {
      this.resolveHandler = resolveHandler;
      return this;
    }

    public Builder layoutHandler(@Nullable RunnableHandler layoutHandler) {
      this.layoutHandler = layoutHandler;
      return this;
    }

    public Builder componentTreeMeasureListenerFactory(
        @Nullable ComponentTreeMeasureListenerFactory componentTreeMeasureListenerFactory) {
      this.componentTreeMeasureListenerFactory = componentTreeMeasureListenerFactory;
      return this;
    }

    public Builder preallocateMountContentHandler(
        @Nullable RunnableHandler preallocateMountContentHandler) {
      this.preallocateMountContentHandler = preallocateMountContentHandler;
      return this;
    }

    public Builder shouldPreallocatePerMountSpec(boolean shouldPreallocatePerMountSpec) {
      this.shouldPreallocatePerMountSpec = shouldPreallocatePerMountSpec;
      return this;
    }

    public Builder incrementalMount(boolean incrementalMount) {
      this.incrementalMount = incrementalMount;
      return this;
    }

    public Builder visibilityProcessingEnabled(boolean visibilityProcessingEnabled) {
      this.visibilityProcessingEnabled = visibilityProcessingEnabled;
      return this;
    }

    public Builder canInterruptAndMoveLayoutsBetweenThreads(boolean isEnabled) {
      this.canInterruptAndMoveLayoutsBetweenThreads = isEnabled;
      return this;
    }

    public Builder isReconciliationEnabled(boolean isEnabled) {
      isReconciliationEnabled = isEnabled;
      return this;
    }

    public Builder recyclingMode(int recyclingMode) {
      this.recyclingMode = recyclingMode;
      return this;
    }

    public Builder isLayoutDiffingEnabled(boolean isEnabled) {
      isLayoutDiffingEnabled = isEnabled;
      return this;
    }

    public Builder parentLifecycleProvider(LithoLifecycleProvider parentLifecycle) {
      this.parentLifecycle = parentLifecycle;
      return this;
    }

    public Builder errorEventHandler(ErrorEventHandler errorEventHandler) {
      this.errorEventHandler = errorEventHandler;
      return this;
    }

    public ComponentTreeHolder build() {
      ensureMandatoryParams();
      return new ComponentTreeHolder(this);
    }

    private void ensureMandatoryParams() {
      if (renderInfo == null) {
        throw new IllegalArgumentException(
            "A RenderInfo must be specified to create a ComponentTreeHolder");
      }
    }
  }

  @VisibleForTesting
  ComponentTreeHolder(Builder builder) {
    mRenderInfo = builder.renderInfo;
    mResolveHandler = builder.resolveHandler;
    mLayoutHandler = builder.layoutHandler;
    mPreallocateMountContentHandler = builder.preallocateMountContentHandler;
    mShouldPreallocatePerMountSpec = builder.shouldPreallocatePerMountSpec;
    mComponentTreeMeasureListenerFactory = builder.componentTreeMeasureListenerFactory;
    mCanInterruptAndMoveLayoutsBetweenThreads = builder.canInterruptAndMoveLayoutsBetweenThreads;
    mId = sIdGenerator.getAndIncrement();
    mIncrementalMount = builder.incrementalMount;
    mVisibilityProcessingEnabled = builder.visibilityProcessingEnabled;
    mIsReconciliationEnabled = builder.isReconciliationEnabled;
    mIsLayoutDiffingEnabled = builder.isLayoutDiffingEnabled;
    mParentLifecycle = builder.parentLifecycle;
    mErrorEventHandler = builder.errorEventHandler;
    mComponentsConfiguration = builder.componentsConfiguration;
  }

  @VisibleForTesting
  @UiThread
  public synchronized void acquireStateAndReleaseTree(boolean acquireTreeStateOnRelease) {
    if (acquireTreeStateOnRelease || shouldAcquireTreeStateOnRelease()) {
      acquireTreeState();
    }

    acquireAnimationState();
    releaseTree();
  }

  synchronized void invalidateTree() {
    mIsTreeValid = false;
  }

  synchronized void setNewLayoutReadyListener(
      @Nullable ComponentTree.NewLayoutStateReadyListener listener) {
    if (mComponentTree != null) {
      mComponentTree.setNewLayoutStateReadyListener(listener);
    } else {
      mPendingNewLayoutListener = listener;
    }
  }

  int getId() {
    return mId;
  }

  public void computeLayoutSync(
      ComponentContext context, int widthSpec, int heightSpec, Size size) {

    final ComponentTree componentTree;
    final Component component;
    final TreeProps treeProps;

    synchronized (this) {
      if (mRenderInfo.rendersView()) {
        // Nothing to do for views.
        return;
      }

      mLastRequestedWidthSpec = widthSpec;
      mLastRequestedHeightSpec = heightSpec;

      ensureComponentTree(context);

      componentTree = mComponentTree;
      component = mRenderInfo.getComponent();
      treeProps =
          mRenderInfo instanceof TreePropsWrappedRenderInfo
              ? ((TreePropsWrappedRenderInfo) mRenderInfo).getTreeProps()
              : null;
    }

    componentTree.setRootAndSizeSpecSync(component, widthSpec, heightSpec, size, treeProps);

    synchronized (this) {
      if (componentTree == mComponentTree && component == mRenderInfo.getComponent()) {
        mIsTreeValid = true;
        if (size != null) {
          mLastMeasuredHeight = size.height;
        }
      }
    }
  }

  public void computeLayoutAsync(ComponentContext context, int widthSpec, int heightSpec) {
    computeLayoutAsync(context, widthSpec, heightSpec, null);
  }

  public void computeLayoutAsync(
      ComponentContext context,
      int widthSpec,
      int heightSpec,
      @Nullable MeasureListener measureListener) {

    final ComponentTree componentTree;
    final Component component;
    final TreeProps treeProps;

    synchronized (this) {
      if (mRenderInfo.rendersView()) {
        // Nothing to do for views.
        return;
      }

      mLastRequestedWidthSpec = widthSpec;
      mLastRequestedHeightSpec = heightSpec;

      ensureComponentTree(context);

      componentTree = mComponentTree;
      component = mRenderInfo.getComponent();

      treeProps =
          mRenderInfo instanceof TreePropsWrappedRenderInfo
              ? ((TreePropsWrappedRenderInfo) mRenderInfo).getTreeProps()
              : null;
    }

    if (measureListener != null) {
      componentTree.addMeasureListener(measureListener);
    }

    componentTree.setRootAndSizeSpecAsync(component, widthSpec, heightSpec, treeProps);

    synchronized (this) {
      if (mComponentTree == componentTree && component == mRenderInfo.getComponent()) {
        mIsTreeValid = true;
      }
    }
  }

  public synchronized void addMeasureListener(@Nullable MeasureListener measureListener) {
    if (mComponentTree != null) {
      mComponentTree.addMeasureListener(measureListener);
    }
  }

  public synchronized void clearMeasureListener(@Nullable MeasureListener measureListener) {
    if (mComponentTree != null) {
      mComponentTree.clearMeasureListener(measureListener);
    }
  }

  public synchronized RenderInfo getRenderInfo() {
    return mRenderInfo;
  }

  public synchronized boolean isTreeValid() {
    return mIsTreeValid;
  }

  public synchronized boolean isTreeValidForSizeSpecs(int widthSpec, int heightSpec) {
    return isTreeValid()
        && mLastRequestedWidthSpec == widthSpec
        && mLastRequestedHeightSpec == heightSpec;
  }

  public synchronized @Nullable ComponentTree getComponentTree() {
    return mComponentTree;
  }

  @VisibleForTesting
  @Nullable
  TreeState getTreeState() {
    return mTreeState;
  }

  public synchronized void setRenderInfo(RenderInfo renderInfo) {
    invalidateTree();
    mRenderInfo = renderInfo;
  }

  public synchronized void updateResolveHandler(@Nullable RunnableHandler resolveHandler) {
    mResolveHandler = resolveHandler;
    if (mComponentTree != null) {
      mComponentTree.updateResolveThreadHandler(resolveHandler);
    }
  }

  public synchronized void updateLayoutHandler(@Nullable RunnableHandler layoutHandler) {
    mLayoutHandler = layoutHandler;
    if (mComponentTree != null) {
      mComponentTree.updateLayoutThreadHandler(layoutHandler);
    }
  }

  synchronized int getMeasuredHeight() {
    return mLastMeasuredHeight;
  }

  synchronized void setMeasuredHeight(int height) {
    mLastMeasuredHeight = height;
  }

  void checkWorkingRangeAndDispatch(
      int position,
      int firstVisibleIndex,
      int lastVisibleIndex,
      int firstFullyVisibleIndex,
      int lastFullyVisibleIndex) {
    checkWorkingRangeAndDispatch(
        new WorkingRangeAndPositionHolder(
            position,
            new WorkingRangeHolder(
                firstVisibleIndex,
                lastVisibleIndex,
                firstFullyVisibleIndex,
                lastFullyVisibleIndex)));
  }

  synchronized void checkWorkingRangeAndDispatch(
      WorkingRangeAndPositionHolder workingRangeAndPosition) {
    if (mComponentTree == null) {
      if (ComponentsConfiguration.useReliableWorkingRange) {
        mPendingWorkingRangeAndPosition = workingRangeAndPosition;
      }
    } else {
      mComponentTree.checkWorkingRangeAndDispatch(workingRangeAndPosition);
    }
  }

  int getRenderState() {
    return mRenderState.get();
  }

  void setRenderState(@RenderState int renderState) {
    mRenderState.set(renderState);
  }

  public synchronized boolean hasCompletedLatestLayout() {
    return mRenderInfo.rendersView()
        || (mComponentTree != null
            && mComponentTree.hasCompatibleLayout(
                mLastRequestedWidthSpec, mLastRequestedHeightSpec));
  }

  /** @return whether this ComponentTreeHolder has been inserted into the adapter yet. */
  public synchronized boolean isInserted() {
    return mIsInserted;
  }

  /** Set whether this ComponentTreeHolder has been inserted into the adapter. */
  public synchronized void setInserted(boolean inserted) {
    mIsInserted = inserted;
  }

  @GuardedBy("this")
  private void ensureComponentTree(ComponentContext context) {
    if (mComponentTree == null) {
      if (mParentLifecycle != null) {
        mComponentTreeHolderLifecycleProvider = new ComponentTreeHolderLifecycleProvider();
      }
      final ComponentTree.Builder builder =
          ComponentTree.create(
              context, mRenderInfo.getComponent(), mComponentTreeHolderLifecycleProvider);

      // if custom attributes are provided on RenderInfo, they will be preferred over builder values
      applyCustomAttributesIfProvided(builder);

      builder
          .resolveThreadHandler(mResolveHandler)
          .layoutThreadHandler(mLayoutHandler)
          .treeState(mTreeState)
          .preAllocateMountContentHandler(mPreallocateMountContentHandler)
          .shouldPreallocateMountContentPerMountSpec(mShouldPreallocatePerMountSpec)
          .measureListener(
              mComponentTreeMeasureListenerFactory == null
                  ? null
                  : mComponentTreeMeasureListenerFactory.create(this))
          .hasMounted(mHasMounted)
          .incrementalMount(mIncrementalMount)
          .visibilityProcessing(mVisibilityProcessingEnabled)
          .canInterruptAndMoveLayoutsBetweenThreads(mCanInterruptAndMoveLayoutsBetweenThreads)
          .logger(mRenderInfo.getComponentsLogger(), mRenderInfo.getLogTag())
          .componentsConfiguration(mComponentsConfiguration)
          .build();

      mComponentTree = builder.build();

      if (mPendingNewLayoutListener != null) {
        mComponentTree.setNewLayoutStateReadyListener(mPendingNewLayoutListener);
      }
      if (mPendingWorkingRangeAndPosition != null) {
        mComponentTree.checkWorkingRangeAndDispatch(mPendingWorkingRangeAndPosition);
        mPendingWorkingRangeAndPosition = null;
      }
    }
  }

  private void applyCustomAttributesIfProvided(ComponentTree.Builder builder) {
    final Object isReconciliationEnabledAttr =
        mRenderInfo.getCustomAttribute(ComponentRenderInfo.RECONCILIATION_ENABLED);
    final Object layoutDiffingEnabledAttr =
        mRenderInfo.getCustomAttribute(ComponentRenderInfo.LAYOUT_DIFFING_ENABLED);
    final Object errorEventHandlerAttr =
        mRenderInfo.getCustomAttribute(ComponentRenderInfo.ERROR_EVENT_HANDLER);

    // If the custom attribute is NOT set, defer to the value from the builder.
    if (isReconciliationEnabledAttr != null) {
      builder.isReconciliationEnabled((boolean) isReconciliationEnabledAttr);
    } else {
      builder.isReconciliationEnabled(mIsReconciliationEnabled);
    }

    if (layoutDiffingEnabledAttr != null) {
      builder.layoutDiffing((boolean) layoutDiffingEnabledAttr);
    } else {
      builder.layoutDiffing(mIsLayoutDiffingEnabled);
    }

    if (errorEventHandlerAttr instanceof ErrorEventHandler) {
      builder.errorHandler((ErrorEventHandler) errorEventHandlerAttr);
    } else if (mErrorEventHandler != null) {
      builder.errorHandler(mErrorEventHandler);
    }
  }

  @UiThread
  public synchronized void releaseTree() {
    if (mComponentTree != null) {
      if (mComponentTreeHolderLifecycleProvider != null) {
        mComponentTreeHolderLifecycleProvider.moveToLifecycle(DESTROYED);

        return;
      }

      mComponentTree.release();
      mComponentTree = null;
    }

    mIsTreeValid = false;
  }

  boolean shouldPreventRelease() {
    final Object preventRelease = mRenderInfo.getCustomAttribute(PREVENT_RELEASE_TAG);
    if (preventRelease instanceof Boolean) {
      return (Boolean) preventRelease;
    }

    return false;
  }

  private boolean shouldAcquireTreeStateOnRelease() {
    final Object acquireTreeState =
        mRenderInfo.getCustomAttribute(ACQUIRE_STATE_HANDLER_ON_RELEASE);
    if (acquireTreeState instanceof Boolean) {
      return (Boolean) acquireTreeState;
    }

    return false;
  }

  @GuardedBy("this")
  private void acquireTreeState() {
    if (mComponentTree == null) {
      return;
    }

    mTreeState = mComponentTree.acquireTreeState();
  }

  @GuardedBy("this")
  private void acquireAnimationState() {
    if (mComponentTree == null) {
      return;
    }

    mHasMounted = mComponentTree.hasMounted();
  }

  /** Lifecycle controlled by a ComponentTreeHolder. */
  private class ComponentTreeHolderLifecycleProvider
      implements LithoLifecycleProvider, LithoLifecycleListener {
    public LithoLifecycleProviderDelegate mLithoLifecycleProviderDelegate;

    public ComponentTreeHolderLifecycleProvider() {
      mParentLifecycle.addListener(this);
      mLithoLifecycleProviderDelegate = new LithoLifecycleProviderDelegate();
    }

    @Override
    public LithoLifecycle getLifecycleStatus() {
      return mLithoLifecycleProviderDelegate.getLifecycleStatus();
    }

    @Override
    public void onMovedToState(LithoLifecycle state) {
      switch (state) {
        case HINT_VISIBLE:
          moveToLifecycle(LithoLifecycle.HINT_VISIBLE);
          return;
        case HINT_INVISIBLE:
          moveToLifecycle(LithoLifecycle.HINT_INVISIBLE);
          return;
        case DESTROYED:
          moveToLifecycle(DESTROYED);
          return;
        default:
          throw new IllegalStateException("Illegal state: " + state);
      }
    }

    @Override
    @UiThread
    public void moveToLifecycle(LithoLifecycle lithoLifecycle) {
      assertMainThread();
      mLithoLifecycleProviderDelegate.moveToLifecycle(lithoLifecycle);
      if (lithoLifecycle == DESTROYED) {
        mParentLifecycle.removeListener(this);
        mComponentTree = null;
        mIsTreeValid = false;
      }
    }

    @Override
    public synchronized void addListener(LithoLifecycleListener listener) {
      mLithoLifecycleProviderDelegate.addListener(listener);
    }

    @Override
    public synchronized void removeListener(LithoLifecycleListener listener) {
      mLithoLifecycleProviderDelegate.removeListener(listener);
    }
  }
}
