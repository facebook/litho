/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components.testing;

import java.util.ArrayList;
import java.util.List;

import android.support.annotation.AttrRes;
import android.support.annotation.StyleRes;
import android.support.v4.util.Pools;

import com.facebook.components.Component;
import com.facebook.components.ComponentLayout;
import com.facebook.components.ComponentLifecycle;
import com.facebook.components.Container;
import com.facebook.components.ComponentContext;
import com.facebook.components.Layout;

public class TestLayoutComponent extends ComponentLifecycle {
  private static final List<TestLayoutComponent> sInstances = new ArrayList<>();
  private static final Pools.SynchronizedPool<Builder> mBuilderPool =
      new Pools.SynchronizedPool<>(2);

  private final boolean mCallsShouldUpdateOnMount;
  private final boolean mIsPureRender;
  private final boolean mHasMountSpecChild;
  private final boolean mIsDelegate;

  private synchronized static TestLayoutComponent get(
      boolean callsShouldUpdateOnMount,
      boolean isPureRender,
      boolean hasMountSpecChild,
      boolean isDelegate) {
    for (TestLayoutComponent lifecycle : sInstances) {
      if (lifecycle.mCallsShouldUpdateOnMount == callsShouldUpdateOnMount
          && lifecycle.mIsPureRender == isPureRender
          && lifecycle.mHasMountSpecChild == hasMountSpecChild
          && lifecycle.mIsDelegate == isDelegate) {
        return lifecycle;
      }
    }

    final TestLayoutComponent lifecycle = new TestLayoutComponent(
        callsShouldUpdateOnMount,
        isPureRender,
        hasMountSpecChild,
        isDelegate);

    sInstances.add(lifecycle);

    return lifecycle;
  }

  private TestLayoutComponent(
      boolean callsShouldUpdateOnMount,
      boolean isPureRender,
      boolean hasMountSpecChild,
      boolean isDelegate) {
    super();

    mCallsShouldUpdateOnMount = callsShouldUpdateOnMount;
    mIsPureRender = isPureRender;
    mHasMountSpecChild = hasMountSpecChild;
    mIsDelegate = isDelegate;
  }

  @Override
  public boolean shouldUpdate(Component previous, Component next) {
    return !next.equals(previous);
  }

  @Override
  protected boolean callsShouldUpdateOnMount() {
    return mCallsShouldUpdateOnMount;
  }

  @Override
  protected boolean isPureRender() {
    return mIsPureRender;
  }

  @Override
  protected ComponentLayout onCreateLayout(ComponentContext c, Component _stateObject) {
    super.onCreateLayout(c, _stateObject);
