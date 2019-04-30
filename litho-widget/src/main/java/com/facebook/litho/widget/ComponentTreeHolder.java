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

import androidx.annotation.IntDef;
import androidx.annotation.VisibleForTesting;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.ComponentTree.MeasureListener;
import com.facebook.litho.LithoHandler;
import com.facebook.litho.Size;
import com.facebook.litho.StateHandler;
import com.facebook.litho.TreeProps;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A class used to store the data backing a {@link RecyclerBinder}. For each item the
 * ComponentTreeHolder keeps the {@link RenderInfo} which contains the original {@link Component}
 * and either the {@link ComponentTree} or the {@link StateHandler} depending upon whether the item
 * is within the current working range or not.
 */
@ThreadSafe
public class ComponentTreeHolder {
  private static final int UNINITIALIZED = -1;
  private static final AtomicInteger sIdGenerator = new AtomicInteger(1);

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
  private final LithoHandler mPreallocateMountContentHandler;
  private final boolean mCanPreallocateOnDefaultHandler;
  private final boolean mShouldPreallocatePerMountSpec;
  private final boolean mIncrementalMount;

  @GuardedBy("this")
  private boolean mIsTreeValid;

  @GuardedBy("this")
  private @Nullable LithoHandler mLayoutHandler;

  @GuardedBy("this")
  private boolean mIsInserted = true;

  @GuardedBy("this")
  private boolean mHasMounted = false;

  @GuardedBy("this")
  private int mLastMeasuredHeight;

  @GuardedBy("this")
  private @Nullable ComponentTree mComponentTree;

  @GuardedBy("this")
  private StateHandler mStateHandler;

  @GuardedBy("this")
  private RenderInfo mRenderInfo;

  @GuardedBy("this")
  private @Nullable ComponentTree.NewLayoutStateReadyListener mPendingNewLayoutListener;

  @GuardedBy("this")
  private int mLastRequestedWidthSpec = UNINITIALIZED;

  @GuardedBy("this")
  private int mLastRequestedHeightSpec = UNINITIALIZED;

  public static Builder create() {
    return new Builder();
  }

  public static class Builder {
    private RenderInfo renderInfo;
    private LithoHandler layoutHandler;
    private ComponentTreeMeasureListenerFactory componentTreeMeasureListenerFactory;
    private @Nullable LithoHandler preallocateMountContentHandler;
    private boolean canPreallocateOnDefaultHandler;
    private boolean shouldPreallocatePerMountSpec;
    private boolean incrementalMount = true;

    private Builder() {}

    public Builder renderInfo(RenderInfo renderInfo) {
      this.renderInfo = renderInfo == null ? ComponentRenderInfo.createEmpty() : renderInfo;
      return this;
    }

    public Builder layoutHandler(LithoHandler layoutHandler) {
      this.layoutHandler = layoutHandler;
      return this;
    }

    public Builder componentTreeMeasureListenerFactory(
        @Nullable ComponentTreeMeasureListenerFactory componentTreeMeasureListenerFactory) {
      this.componentTreeMeasureListenerFactory = componentTreeMeasureListenerFactory;
      return this;
    }

    public Builder preallocateMountContentHandler(
        @Nullable LithoHandler preallocateMountContentHandler) {
      this.preallocateMountContentHandler = preallocateMountContentHandler;
      return this;
    }

    public Builder canPreallocateOnDefaultHandler(boolean canPreallocateOnDefaultHandler) {
      this.canPreallocateOnDefaultHandler = canPreallocateOnDefaultHandler;
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
    mLayoutHandler = builder.layoutHandler;
    mPreallocateMountContentHandler = builder.preallocateMountContentHandler;
    mCanPreallocateOnDefaultHandler = builder.canPreallocateOnDefaultHandler;
    mShouldPreallocatePerMountSpec = builder.shouldPreallocatePerMountSpec;
    mComponentTreeMeasureListenerFactory = builder.componentTreeMeasureListenerFactory;
    mId = sIdGenerator.getAndIncrement();
    mIncrementalMount = builder.incrementalMount;
  }

  public synchronized void acquireStateAndReleaseTree() {
    acquireStateHandler();
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

    componentTree.setRootAndSizeSpec(component, widthSpec, heightSpec, size, treeProps);

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
      componentTree.updateMeasureListener(measureListener);
    }

    componentTree.setRootAndSizeSpecAsync(component, widthSpec, heightSpec, treeProps);

    synchronized (this) {
      if (mComponentTree == componentTree && component == mRenderInfo.getComponent()) {
        mIsTreeValid = true;
      }
    }
  }

  public synchronized void updateMeasureListener(@Nullable MeasureListener measureListener) {
    if (mComponentTree != null) {
      mComponentTree.updateMeasureListener(measureListener);
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

  public synchronized void setRenderInfo(RenderInfo renderInfo) {
    invalidateTree();
    mRenderInfo = renderInfo;
  }

  public synchronized void updateLayoutHandler(@Nullable LithoHandler layoutHandler) {
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

  synchronized void checkWorkingRangeAndDispatch(
      int position,
      int firstVisibleIndex,
      int lastVisibleIndex,
      int firstFullyVisibleIndex,
      int lastFullyVisibleIndex) {
    if (mComponentTree != null) {
      mComponentTree.checkWorkingRangeAndDispatch(
          position,
          firstVisibleIndex,
          lastVisibleIndex,
          firstFullyVisibleIndex,
          lastFullyVisibleIndex);
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
      final Object isReconciliationEnabled =
          mRenderInfo.getCustomAttribute(ComponentRenderInfo.RECONCILIATION_ENABLED);
      final Object layoutDiffingEnabledAttr =
          mRenderInfo.getCustomAttribute(ComponentRenderInfo.LAYOUT_DIFFING_ENABLED);
      final ComponentTree.Builder builder =
          ComponentTree.create(context, mRenderInfo.getComponent());
      // If no custom attribute is set, defer to the default value of the builder.
      if (isReconciliationEnabled != null) {
        builder.layoutDiffing(!(boolean) isReconciliationEnabled);
        builder.isReconciliationEnabled((boolean) isReconciliationEnabled);
        builder.enableNestedTreeResolutionExperiment((boolean) isReconciliationEnabled);
      } else if (layoutDiffingEnabledAttr != null) {
        builder.layoutDiffing((boolean) layoutDiffingEnabledAttr);
      }
      mComponentTree =
          builder
              .layoutThreadHandler(mLayoutHandler)
              .stateHandler(mStateHandler)
              .preAllocateMountContentHandler(mPreallocateMountContentHandler)
              .preallocateOnDefaultHandler(mCanPreallocateOnDefaultHandler)
              .shouldPreallocateMountContentPerMountSpec(mShouldPreallocatePerMountSpec)
              .useSharedLayoutStateFuture(true)
              .measureListener(
                  mComponentTreeMeasureListenerFactory == null
                      ? null
                      : mComponentTreeMeasureListenerFactory.create(this))
              .hasMounted(mHasMounted)
              .incrementalMount(mIncrementalMount)
              .build();
      if (mPendingNewLayoutListener != null) {
        mComponentTree.setNewLayoutStateReadyListener(mPendingNewLayoutListener);
      }
    }
  }

  @GuardedBy("this")
  private void releaseTree() {
    if (mComponentTree != null) {
      mComponentTree.release();
      mComponentTree = null;
    }

    mIsTreeValid = false;
  }

  @GuardedBy("this")
  private void acquireStateHandler() {
    if (mComponentTree == null) {
      return;
    }

    mStateHandler = mComponentTree.acquireStateHandler();
  }

  @GuardedBy("this")
  private void acquireAnimationState() {
    if (mComponentTree == null) {
      return;
    }

    mHasMounted = mComponentTree.hasMounted();
  }
}
