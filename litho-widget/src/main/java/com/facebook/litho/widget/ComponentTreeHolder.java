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

import android.support.annotation.IntDef;
import android.support.v4.util.Pools;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.ComponentTree.MeasureListener;
import com.facebook.litho.LayoutHandler;
import com.facebook.litho.Size;
import com.facebook.litho.StateHandler;
import com.facebook.litho.TreeProps;
import java.util.concurrent.atomic.AtomicBoolean;
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

  private static final Pools.SynchronizedPool<ComponentTreeHolder> sComponentTreeHoldersPool =
      new Pools.SynchronizedPool<>(8);
  private @Nullable ComponentTreeMeasureListenerFactory mComponentTreeMeasureListenerFactory;

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

  private final AtomicBoolean mIsReleased = new AtomicBoolean(false);

  @IntDef({RENDER_UNINITIALIZED, RENDER_ADDED, RENDER_DRAWN})
  public @interface RenderState {}

  static final int RENDER_UNINITIALIZED = 0;
  static final int RENDER_ADDED = 1;
  static final int RENDER_DRAWN = 2;

  private final AtomicInteger mRenderState = new AtomicInteger(RENDER_UNINITIALIZED);

  private int mId;
  private boolean mIsTreeValid;
  private @Nullable LayoutHandler mLayoutHandler;
  private LayoutHandler mPreallocateMountContentHandler;
  private boolean mCanPreallocateOnDefaultHandler;
  private boolean mShouldPreallocatePerMountSpec;
  private boolean mUseSharedLayoutStateFuture;
  private String mSplitLayoutTag;
  private boolean mIsInserted = true;
  private boolean mHasMounted = false;

  interface ComponentTreeMeasureListenerFactory {
    @Nullable
    MeasureListener create(ComponentTreeHolder holder);
  }

  public static Builder create() {
    return new Builder();
  }

  public static class Builder {
    private RenderInfo renderInfo;
    private LayoutHandler layoutHandler;
    private boolean canPrefetchDisplayLists;
    private boolean canCacheDrawingDisplayLists;
    private ComponentTreeMeasureListenerFactory componentTreeMeasureListenerFactory;
    private String splitLayoutTag;
    private @Nullable LayoutHandler preallocateMountContentHandler;
    private boolean canPreallocateOnDefaultHandler;
    private boolean shouldPreallocatePerMountSpec;
    private boolean useSharedLayoutStateFuture;

    private Builder() {}

    public Builder renderInfo(RenderInfo renderInfo) {
      this.renderInfo = renderInfo == null ? ComponentRenderInfo.createEmpty() : renderInfo;
      return this;
    }

    public Builder layoutHandler(LayoutHandler layoutHandler) {
      this.layoutHandler = layoutHandler;
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

    public Builder componentTreeMeasureListenerFactory(
        @Nullable ComponentTreeMeasureListenerFactory componentTreeMeasureListenerFactory) {
      this.componentTreeMeasureListenerFactory = componentTreeMeasureListenerFactory;
      return this;
    }

    public Builder splitLayoutTag(String splitLayoutTag) {
      this.splitLayoutTag = splitLayoutTag;
      return this;
    }

    public Builder preallocateMountContentHandler(
        @Nullable LayoutHandler preallocateMountContentHandler) {
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

    public Builder useSharedLayoutStateFuture(boolean useSharedLayoutStateFuture) {
      this.useSharedLayoutStateFuture = useSharedLayoutStateFuture;
      return this;
    }

    public ComponentTreeHolder build() {
      ensureMandatoryParams();

      ComponentTreeHolder componentTreeHolder = sComponentTreeHoldersPool.acquire();
      if (componentTreeHolder == null) {
        componentTreeHolder = new ComponentTreeHolder();
      }

      componentTreeHolder.mRenderInfo = renderInfo;
      componentTreeHolder.mLayoutHandler = layoutHandler;
      componentTreeHolder.mPreallocateMountContentHandler = preallocateMountContentHandler;
      componentTreeHolder.mCanPreallocateOnDefaultHandler = canPreallocateOnDefaultHandler;
      componentTreeHolder.mShouldPreallocatePerMountSpec = shouldPreallocatePerMountSpec;
      componentTreeHolder.mUseSharedLayoutStateFuture = useSharedLayoutStateFuture;
      componentTreeHolder.mComponentTreeMeasureListenerFactory =
          componentTreeMeasureListenerFactory;
      componentTreeHolder.mSplitLayoutTag = splitLayoutTag;
      componentTreeHolder.acquireId();
      componentTreeHolder.mIsReleased.set(false);

      return componentTreeHolder;
    }

    private void ensureMandatoryParams() {
      if (renderInfo == null) {
        throw new IllegalArgumentException(
            "A RenderInfo must be specified to create a ComponentTreeHolder");
      }
    }
  }

  private void acquireId() {
    mId = sIdGenerator.getAndIncrement();
  }

  public synchronized void acquireStateAndReleaseTree() {
    acquireStateHandler();
    acquireAnimationState();
    releaseTree();
  }

  synchronized void invalidateTree() {
    mIsTreeValid = false;
  }

  synchronized void clearStateHandler() {
    mStateHandler = null;
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

  public synchronized void updateLayoutHandler(@Nullable LayoutHandler layoutHandler) {
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

  public boolean isReleased() {
    return mIsReleased.get();
  }

  public synchronized void release() {
    releaseTree();
    clearStateHandler();
    mRenderInfo = null;
    mLayoutHandler = null;
    mPreallocateMountContentHandler = null;
    mShouldPreallocatePerMountSpec = false;
    mCanPreallocateOnDefaultHandler = false;
    mUseSharedLayoutStateFuture = false;
    mPendingNewLayoutListener = null;
    mLastRequestedWidthSpec = UNINITIALIZED;
    mLastRequestedHeightSpec = UNINITIALIZED;
    mIsInserted = true;
    mHasMounted = false;
    mRenderState.set(RENDER_UNINITIALIZED);
    if (mIsReleased.getAndSet(true)) {
      throw new RuntimeException("Releasing already released ComponentTreeHolder!");
    }
    sComponentTreeHoldersPool.release(this);
  }

  @GuardedBy("this")
  private void ensureComponentTree(ComponentContext context) {
    if (mComponentTree == null) {
      final Object layoutDiffingEnabledAttr =
          mRenderInfo.getCustomAttribute(ComponentRenderInfo.LAYOUT_DIFFING_ENABLED);
      final ComponentTree.Builder builder =
          ComponentTree.create(context, mRenderInfo.getComponent());
      // If no custom attribute is set, defer default value to the builder.
      if (layoutDiffingEnabledAttr != null) {
        builder.layoutDiffing((boolean) layoutDiffingEnabledAttr);
      }
      mComponentTree =
          builder
              .layoutThreadHandler(mLayoutHandler)
              .stateHandler(mStateHandler)
              .preAllocateMountContentHandler(mPreallocateMountContentHandler)
              .preallocateOnDefaultHandler(mCanPreallocateOnDefaultHandler)
              .shouldPreallocateMountContentPerMountSpec(mShouldPreallocatePerMountSpec)
              .useSharedLayoutStateFuture(mUseSharedLayoutStateFuture)
              .measureListener(
                  mComponentTreeMeasureListenerFactory == null
                      ? null
                      : mComponentTreeMeasureListenerFactory.create(this))
              .splitLayoutTag(mSplitLayoutTag)
              .hasMounted(mHasMounted)
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
