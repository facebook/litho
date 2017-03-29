/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import android.support.v4.util.Pools;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentInfo;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.LayoutHandler;
import com.facebook.litho.Size;
import com.facebook.litho.StateHandler;

/**
 * A class used to store the data backing a {@link RecyclerBinder}. For each item the
 * ComponentTreeHolder keeps the {@link ComponentInfo} which contains the original {@link Component}
 * and either the {@link ComponentTree} or the {@link StateHandler} depending upon whether
 * the item is within the current working range or not.
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
  private ComponentInfo mComponentInfo;
  private boolean mIsTreeValid;
  private LayoutHandler mLayoutHandler;

  static ComponentTreeHolder acquire(ComponentInfo componentInfo, LayoutHandler layoutHandler) {
    ComponentTreeHolder componentTreeHolder = sComponentTreeHoldersPool.acquire();
    if (componentTreeHolder == null) {
      componentTreeHolder = new ComponentTreeHolder();
    }
    componentTreeHolder.mComponentInfo = componentInfo;
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
