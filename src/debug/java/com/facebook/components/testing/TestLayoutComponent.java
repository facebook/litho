/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing;

import com.facebook.yoga.YogaAlign;

import com.facebook.yoga.YogaFlexDirection;

import java.util.ArrayList;
import java.util.List;

import android.support.annotation.AttrRes;
import android.support.annotation.StyleRes;
import android.support.v4.util.Pools;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.ComponentLifecycle;
import com.facebook.litho.Container;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Layout;

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
    final Component<?> mountSpecComponent =
        TestDrawableComponent.create(c, false, true, true, false, false).build();

    if (mIsDelegate) {
      return Layout.create(c, mountSpecComponent).flexShrink(0).build();
    }

    ComponentLayout.ContainerBuilder containerBuilder = Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START);

    if (mHasMountSpecChild) {
      containerBuilder.child(mountSpecComponent);
    }

    return containerBuilder.build();
  }

  @Override
  public MountType getMountType() {
    return MountType.NONE;
  }

  public static Builder create(
      ComponentContext context,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes) {
    return create(context, defStyleAttr, defStyleRes, true, true, false, false);
  }

  public static Builder create(
      ComponentContext context,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes,
      boolean callsShouldUpdateOnMount,
      boolean isPureRender,
      boolean hasMountSpecChild,
      boolean isDelegate) {
    return newBuilder(
        context,
        defStyleAttr,
        defStyleRes,
        new State(callsShouldUpdateOnMount, isPureRender, hasMountSpecChild, isDelegate));
  }

  public static Builder create(ComponentContext context) {
    return create(context, 0, 0, true, true, false, false);
  }

  public static Builder create(
      ComponentContext context,
      boolean callsShouldUpdateOnMount,
      boolean isPureRender) {
    return create(context, 0, 0, callsShouldUpdateOnMount, isPureRender, false, false);
  }

  private static Builder newBuilder(
      ComponentContext context,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes,
      State state) {
    Builder builder = mBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }
    builder.init(context, defStyleAttr, defStyleRes, state);
    return builder;
  }

  public static class State extends TestComponent<TestLayoutComponent> implements Cloneable {

    private State(
        boolean callsShouldUpdateOnMount,
        boolean isPureRender,
        boolean hasMountSpecChild,
        boolean isDelegate) {
      super(get(callsShouldUpdateOnMount, isPureRender, hasMountSpecChild, isDelegate));
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }
      if (!super.equals(o)) {
        return false;
      }
      if (o instanceof State) {
        return true;
      }
      return false;
    }
  }

  public static class Builder
      extends com.facebook.litho.Component.Builder<TestLayoutComponent> {
    State mState;

    private void init(
        ComponentContext context,
        @AttrRes int defStyleAttr,
        @StyleRes int defStyleRes,
        State state) {
      super.init(context, defStyleAttr, defStyleRes, state);
      mState = state;
    }

    public Builder unique() {
      mState.mIsUnique = true;
      return this;
    }

    @Override
    public Builder key(String key) {
      super.setKey(key);
      return this;
    }

    @Override
    public TestComponent<TestLayoutComponent> build() {
      State state = mState;
      release();
      return state;
    }

    @Override
    protected void release() {
      super.release();
      mState = null;
      mBuilderPool.release(this);
    }
  }
}
