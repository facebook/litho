// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.widget;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import android.support.v4.util.Pools;

import com.facebook.components.Component;
import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentTree;
import com.facebook.components.LayoutHandler;
import com.facebook.components.Size;
import com.facebook.components.StateHandler;

/**
 * A class used to store the data backing a {@link RecyclerBinder}. For each item the
 * ComponentTreeHolder keeps the original {@link Component} and either the {@link ComponentTree} or
 * the {@link StateHandler} depending upon whether the item is within the current working range or
 * not.
 */
@ThreadSafe
public class ComponentTreeHolder {
  private static final Pools.SynchronizedPool<ComponentTreeHolder> sComponentTreeHoldersPool =
      new Pools.SynchronizedPool<>(8);

  @GuardedBy("this")
  private ComponentTree mComponentTree;
  @GuardedBy("this")
  private StateHandler mStateHandler;
  @GuardedBy("this")
  private Component mComponent;
  private boolean mIsTreeValid;
  private LayoutHandler mLayoutHandler;

  static ComponentTreeHolder acquire(Component component, LayoutHandler layoutHandler) {
    ComponentTreeHolder componentTreeHolder = sComponentTreeHoldersPool.acquire();
    if (componentTreeHolder == null) {
      componentTreeHolder = new ComponentTreeHolder();
    }
    componentTreeHolder.mComponent = component;
    componentTreeHolder.mLayoutHandler = layoutHandler;
    return componentTreeHolder;
  }

  synchronized void acquireStateHandlerAndReleaseTree() {
    acquireStateHandler();
    releaseTree();
  }

  synchronized void invalidateTree() {
    mIsTreeValid = false;
  }

  synchronized void clearStateHandler() {
    mStateHandler = null;
  }

  void computeLayoutSync(
      ComponentContext context,
      int widthSpec,
      int heightSpec,
      Size size) {
    final ComponentTree componentTree;
    final Component component;

    synchronized (this) {
      ensureComponentTree(context);

      componentTree = mComponentTree;
      component = mComponent;
    }

    componentTree.setRootAndSizeSpec(component, widthSpec, heightSpec, size);

    synchronized (this) {
      if (componentTree == mComponentTree && component == mComponent) {
        mIsTreeValid = true;
      }
    }
  }

  void computeLayoutAsync(
      ComponentContext context,
      int widthSpec,
      int heightSpec) {
    final ComponentTree componentTree;
    final Component component;

    synchronized (this) {
      ensureComponentTree(context);

      componentTree = mComponentTree;
      component = mComponent;
    }

    componentTree.setRootAndSizeSpecAsync(component, widthSpec, heightSpec);

    synchronized (this) {
      if (mComponent == component && mComponentTree == componentTree) {
        mIsTreeValid = true;
      }
    }
  }

  synchronized boolean isTreeValid() {
    return mIsTreeValid;
  }

  synchronized ComponentTree getComponentTree() {
    return mComponentTree;
  }

  synchronized void setComponent(Component<?> component) {
    invalidateTree();
    mComponent = component;
  }

  synchronized void release() {
    releaseTree();
    clearStateHandler();
    mComponent = null;
    mLayoutHandler = null;
    sComponentTreeHoldersPool.release(this);
  }

  @GuardedBy("this")
  private void ensureComponentTree(ComponentContext context) {
    if (mComponentTree == null) {
      mComponentTree = ComponentTree.create(context, mComponent)
          .layoutDiffing(true)
          .layoutThreadHandler(mLayoutHandler)
          .stateHandler(mStateHandler)
          .incrementalMount(true)
          .build();
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

    mStateHandler = mComponentTree.getStateHandler();
  }
}
