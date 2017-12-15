/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing;

import android.support.annotation.AttrRes;
import android.support.annotation.StyleRes;
import android.support.v4.util.Pools;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Wrapper;

public class TestLayoutComponent extends TestComponent {
  private static final Pools.SynchronizedPool<Builder> sBuilderPool =
      new Pools.SynchronizedPool<>(2);

  private final boolean mCallsShouldUpdateOnMount;
  private final boolean mIsPureRender;
  private final boolean mHasMountSpecChild;
  private final boolean mIsDelegate;

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
  protected ComponentLayout onCreateLayout(ComponentContext c) {
    super.onCreateLayout(c);
    final Component mountSpecComponent =
        TestDrawableComponent.create(c, false, true, true, false, false).build();

    if (mIsDelegate) {
      return Wrapper.create(c).delegate(mountSpecComponent).build();
    }

    ComponentLayout.ContainerBuilder containerBuilder = Column.create(c);

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
        new TestLayoutComponent(
            callsShouldUpdateOnMount, isPureRender, hasMountSpecChild, isDelegate));
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
      TestLayoutComponent state) {
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }
    builder.init(context, defStyleAttr, defStyleRes, state);
    return builder;
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
    if (o instanceof TestLayoutComponent) {
      return true;
    }
    return false;
  }

  public static class Builder extends com.facebook.litho.Component.Builder<Builder> {
    TestLayoutComponent mState;

    private void init(
        ComponentContext context,
        @AttrRes int defStyleAttr,
        @StyleRes int defStyleRes,
        TestLayoutComponent state) {
      super.init(context, defStyleAttr, defStyleRes, state);
      mState = state;
    }

    public Builder unique() {
      mState.mIsUnique = true;
      return this;
    }

    @Override
    public Builder getThis() {
      return this;
    }

    @Override
    public TestComponent build() {
      TestLayoutComponent state = mState;
      release();
      return state;
    }

    @Override
    protected void release() {
      super.release();
      mState = null;
      sBuilderPool.release(this);
    }
  }
}
