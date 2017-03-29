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
import android.view.View;

import com.facebook.components.Component;
import com.facebook.components.ComponentLayout;
import com.facebook.components.ComponentLifecycle;
import com.facebook.components.ComponentContext;
import com.facebook.components.Size;
import com.facebook.components.SizeSpec;

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
    State state = (State) _stateObject;
    state.onMountCalled();
  }

  @Override
  protected void onUnmount(ComponentContext c, Object mountedContent, Component<?> component) {
    State state = (State) component;
    state.onUnmountCalled();
  }

  @Override
  protected boolean canMeasure() {
    return mCanMeasure;
  }

  @Override
  protected void onMeasure(
      ComponentContext c,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      Component<?> component) {
    int width = SizeSpec.getSize(widthSpec);
    int height = SizeSpec.getSize(heightSpec);

    size.height = height;
    size.width = width;

    State state = (State) component;
    state.onMeasureCalled();
  }

  @Override
  protected void onBoundsDefined(
      ComponentContext c,
      ComponentLayout layout,
      Component<?> component) {
    State state = (State) component;
    state.onDefineBoundsCalled();
  }

  @Override
  protected void onBind(ComponentContext c, Object mountedContent, Component<?> component) {
    State state = (State) component;
    state.onBindCalled();
  }

  @Override
  protected void onUnbind(ComponentContext c, Object mountedContent, Component<?> component) {
    State state = (State) component;
    state.onUnbindCalled();
  }

  @Override
  public MountType getMountType() {
    return MountType.VIEW;
  }

