/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing;

import java.util.ArrayList;
import java.util.List;

import android.support.annotation.AttrRes;
import android.support.annotation.StyleRes;
import android.support.v4.util.Pools;
import android.view.View;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.ComponentLifecycle;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;

public class TestViewComponent extends ComponentLifecycle {
  private static final List<TestViewComponent> sInstances = new ArrayList<>();
  private static final Pools.SynchronizedPool<Builder> mBuilderPool =
      new Pools.SynchronizedPool<>(2);

  private final boolean mCallsShouldUpdateOnMount;
  private final boolean mIsPureRender;
  private final boolean mCanMeasure;
  private final boolean mCanMountIncrementally;

  private synchronized static TestViewComponent get(
      boolean callsShouldUpdateOnMount,
      boolean isPureRender,
      boolean canMeasure,
      boolean canMountIncrementally) {
    for (TestViewComponent lifecycle : sInstances) {
      if (lifecycle.mCallsShouldUpdateOnMount == callsShouldUpdateOnMount &&
          lifecycle.mIsPureRender == isPureRender &&
          lifecycle.mCanMeasure == canMeasure &&
          lifecycle.mCanMountIncrementally == canMountIncrementally) {
        return lifecycle;
      }
    }

    final TestViewComponent lifecycle = new TestViewComponent(
        callsShouldUpdateOnMount,
        isPureRender,
        canMeasure,
        canMountIncrementally);

    sInstances.add(lifecycle);

    return lifecycle;
  }

  private TestViewComponent(
      boolean callsShouldUpdateOnMount,
      boolean isPureRender,
      boolean canMeasure,
      boolean canMountIncrementally) {
    super();

    mCallsShouldUpdateOnMount = callsShouldUpdateOnMount;
    mIsPureRender = isPureRender;
    mCanMeasure = canMeasure;
    mCanMountIncrementally = canMountIncrementally;
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
  protected boolean canMountIncrementally() {
    return mCanMountIncrementally;
  }

  @Override
  protected Object onCreateMountContent(ComponentContext c) {
    if (c instanceof TestComponentContextWithView) {
      return ((TestComponentContextWithView) c).getTestView();
    }
    return new View(c);
  }

  @Override
  protected void onMount(ComponentContext c, Object convertView, Component _stateObject) {
