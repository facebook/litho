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

import static com.facebook.litho.LithoVisibilityEventsController.LithoVisibilityState.DESTROYED;
import static com.facebook.litho.ThreadUtils.assertMainThread;

import android.view.View;
import androidx.annotation.IntDef;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LifecycleOwner;
import com.facebook.litho.AOSPLifecycleOwnerProvider;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.ComponentTree.MeasureListener;
import com.facebook.litho.LithoVisibilityEventsController;
import com.facebook.litho.LithoVisibilityEventsControllerDelegate;
import com.facebook.litho.LithoVisibilityEventsListener;
import com.facebook.litho.Size;
import com.facebook.litho.TreePropContainer;
import com.facebook.litho.TreeState;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.rendercore.RunnableHandler;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

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
  public static final String PREVENT_RELEASE_TAG = "prevent_release";
  public static final String ACQUIRE_STATE_HANDLER_ON_RELEASE = "acquire_state_handler";
  private final @Nullable LithoVisibilityEventsController mParentLifecycle;
  private @Nullable ComponentTreeHolderVisibilityEventsController
      mComponentTreeHolderLifecycleProvider;
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

  @GuardedBy("this")
  private boolean mIsTreeValid;

  @GuardedBy("this")
  private @Nullable RunnableHandler mLayoutHandler;

  @GuardedBy("this")
  private boolean mIsInserted = true;

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

  public static Builder create(ComponentsConfiguration configuration) {
    return new Builder(configuration);
  }

  public static class Builder {

    private RenderInfo renderInfo;
    private final ComponentsConfiguration componentsConfiguration;
    private RunnableHandler layoutHandler;
    private ComponentTreeMeasureListenerFactory componentTreeMeasureListenerFactory;
    private @Nullable LithoVisibilityEventsController parentLifecycle;

    private Builder(ComponentsConfiguration configuration) {
      componentsConfiguration = configuration;
    }

    public Builder renderInfo(RenderInfo renderInfo) {
      this.renderInfo = renderInfo == null ? ComponentRenderInfo.createEmpty() : renderInfo;
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

    public Builder parentLifecycleProvider(LithoVisibilityEventsController parentLifecycle) {
      this.parentLifecycle = parentLifecycle;
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
    mComponentTreeMeasureListenerFactory = builder.componentTreeMeasureListenerFactory;
    mId = sIdGenerator.getAndIncrement();
    mParentLifecycle = builder.parentLifecycle;
    mComponentsConfiguration = builder.componentsConfiguration;
  }

  @VisibleForTesting
  @UiThread
  public synchronized void acquireStateAndReleaseTree(boolean acquireTreeStateOnRelease) {
    if (acquireTreeStateOnRelease || shouldAcquireTreeStateOnRelease()) {
      acquireTreeState();
    }

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
      ComponentContext context, int widthSpec, int heightSpec, @Nullable Size size) {

    final ComponentTree componentTree;
    final Component component;
    final TreePropContainer treePropContainer;

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
      treePropContainer =
          mRenderInfo instanceof TreePropsWrappedRenderInfo
              ? ((TreePropsWrappedRenderInfo) mRenderInfo).getTreePropContainer()
              : null;
    }

    componentTree.setRootAndSizeSpecSync(component, widthSpec, heightSpec, size, treePropContainer);

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
    final TreePropContainer treePropContainer;

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

      treePropContainer =
          mRenderInfo instanceof TreePropsWrappedRenderInfo
              ? ((TreePropsWrappedRenderInfo) mRenderInfo).getTreePropContainer()
              : null;
    }

    if (measureListener != null) {
      componentTree.addMeasureListener(measureListener);
    }

    componentTree.setRootAndSizeSpecAsync(component, widthSpec, heightSpec, treePropContainer);

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

  /**
   * @return whether this ComponentTreeHolder has been inserted into the adapter yet.
   */
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
      ComponentTree.Builder builder;
      if (mParentLifecycle != null) {
        mComponentTreeHolderLifecycleProvider = new ComponentTreeHolderVisibilityEventsController();
      }
      builder =
          ComponentTree.create(
              context, mRenderInfo.getComponent(), mComponentTreeHolderLifecycleProvider);

      String renderInfoLogTag = mRenderInfo.getLogTag();

      ComponentsConfiguration.Builder treeComponentsConfigurationBuilder =
          ComponentsConfiguration.create(mComponentsConfiguration);

      if (mRenderInfo.getLogTag() != null) {
        treeComponentsConfigurationBuilder.logTag(renderInfoLogTag);
      }

      if (mRenderInfo.getComponentsLogger() != null) {
        treeComponentsConfigurationBuilder.componentsLogger(mRenderInfo.getComponentsLogger());
      }

      builder
          .componentsConfiguration(treeComponentsConfigurationBuilder.build())
          .layoutThreadHandler(mLayoutHandler)
          .treeState(mTreeState)
          .measureListener(
              mComponentTreeMeasureListenerFactory == null
                  ? null
                  : mComponentTreeMeasureListenerFactory.create(this));

      mComponentTree = builder.build();

      if (mPendingNewLayoutListener != null) {
        mComponentTree.setNewLayoutStateReadyListener(mPendingNewLayoutListener);
      }
    }
  }

  /**
   * We may need to wait until the corresponding view is detached before releasing the tree as the
   * view might need to run an animation
   */
  @UiThread
  public synchronized void releaseTreeImmediatelyOrOnViewDetached() {
    if (mComponentTree != null) {
      if (mComponentTree.getLithoView() != null
          && mComponentTree.getLithoView().isAttachedToWindow()) {
        mComponentTree
            .getLithoView()
            .addOnAttachStateChangeListener(
                new View.OnAttachStateChangeListener() {
                  @Override
                  public void onViewAttachedToWindow(View view) {}

                  @Override
                  public void onViewDetachedFromWindow(View view) {
                    releaseTree();
                    view.removeOnAttachStateChangeListener(this);
                  }
                });
      } else {
        releaseTree();
      }
    }
  }

  @UiThread
  public synchronized void releaseTree() {
    if (mComponentTree != null) {

      if (mComponentTreeHolderLifecycleProvider != null) {
        mComponentTreeHolderLifecycleProvider.moveToVisibilityState(DESTROYED);

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

  /** Lifecycle controlled by a ComponentTreeHolder. */
  private class ComponentTreeHolderVisibilityEventsController
      implements LithoVisibilityEventsController,
          LithoVisibilityEventsListener,
          AOSPLifecycleOwnerProvider {

    public LithoVisibilityEventsControllerDelegate mLithoVisibilityEventsControllerDelegate;

    public ComponentTreeHolderVisibilityEventsController() {
      mParentLifecycle.addListener(this);
      mLithoVisibilityEventsControllerDelegate = new LithoVisibilityEventsControllerDelegate();
    }

    @Override
    public LithoVisibilityState getVisibilityState() {
      return mLithoVisibilityEventsControllerDelegate.getVisibilityState();
    }

    @Override
    public void onMovedToState(LithoVisibilityState state) {
      switch (state) {
        case HINT_VISIBLE:
          moveToVisibilityState(LithoVisibilityState.HINT_VISIBLE);
          return;
        case HINT_INVISIBLE:
          moveToVisibilityState(LithoVisibilityState.HINT_INVISIBLE);
          return;
        case DESTROYED:
          moveToVisibilityState(DESTROYED);
          return;
        default:
          throw new IllegalStateException("Illegal state: " + state);
      }
    }

    @Override
    @UiThread
    public void moveToVisibilityState(LithoVisibilityState lithoLifecycle) {
      assertMainThread();
      mLithoVisibilityEventsControllerDelegate.moveToVisibilityState(lithoLifecycle);
      if (lithoLifecycle == DESTROYED) {
        mParentLifecycle.removeListener(this);
        mComponentTree = null;
        mIsTreeValid = false;
      }
    }

    @Override
    public synchronized void addListener(LithoVisibilityEventsListener listener) {
      mLithoVisibilityEventsControllerDelegate.addListener(listener);
    }

    @Override
    public synchronized void removeListener(LithoVisibilityEventsListener listener) {
      mLithoVisibilityEventsControllerDelegate.removeListener(listener);
    }

    @Override
    @Nullable
    public LifecycleOwner getLifecycleOwner() {
      if (mParentLifecycle != null && mParentLifecycle instanceof AOSPLifecycleOwnerProvider) {
        return ((AOSPLifecycleOwnerProvider) mParentLifecycle).getLifecycleOwner();
      }

      return null;
    }
  }
}
