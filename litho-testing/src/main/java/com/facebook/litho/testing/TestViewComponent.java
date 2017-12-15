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
import android.view.View;
import com.facebook.litho.ActualComponentLayout;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;

public class TestViewComponent extends TestComponent {
  private static final Pools.SynchronizedPool<Builder> sBuilderPool =
      new Pools.SynchronizedPool<>(2);

  private final boolean mCallsShouldUpdateOnMount;
  private final boolean mIsPureRender;
  private final boolean mCanMeasure;
  private final boolean mCanMountIncrementally;

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
  public String getSimpleName() {
    return "TestViewComponent";
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
  protected void onMount(ComponentContext c, Object convertView) {
    onMountCalled();
  }

  @Override
  protected void onUnmount(ComponentContext c, Object mountedContent) {
    onUnmountCalled();
  }

  @Override
  protected boolean canMeasure() {
    return mCanMeasure;
  }

  @Override
  protected void onMeasure(
      ComponentContext c,
      ActualComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size) {
    int width = SizeSpec.getSize(widthSpec);
    int height = SizeSpec.getSize(heightSpec);

    size.height = height;
    size.width = width;

    onMeasureCalled();
  }

  @Override
  protected void onBoundsDefined(
      ComponentContext c,
    ActualComponentLayout layout) {
    onDefineBoundsCalled();
  }

  @Override
  protected void onBind(ComponentContext c, Object mountedContent) {
    onBindCalled();
  }

  @Override
  protected void onUnbind(ComponentContext c, Object mountedContent) {
    onUnbindCalled();
  }

  @Override
  public MountType getMountType() {
    return MountType.VIEW;
  }

  public static Builder create(
      ComponentContext context,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes) {
    return create(context, defStyleAttr, defStyleRes, true, true, true, true);
  }

  public static Builder create(
      ComponentContext context,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes,
      boolean callsShouldUpdateOnMount,
      boolean isPureRender,
      boolean canMeasure,
      boolean canMountIncrementally) {
    return newBuilder(
        context,
        defStyleAttr,
        defStyleRes,
        new TestViewComponent(
            callsShouldUpdateOnMount, isPureRender, canMeasure, canMountIncrementally));
  }

  public static Builder create(ComponentContext context) {
    return create(context, 0, 0, true, true, true, true);
  }

  public static Builder create(
      ComponentContext context,
      boolean callsShouldUpdateOnMount,
      boolean isPureRender,
      boolean canMeasure,
      boolean canMountIncrementally) {
    return create(
        context,
        0,
        0,
        callsShouldUpdateOnMount,
        isPureRender,
        canMeasure,
        canMountIncrementally);
  }

  private static Builder newBuilder(
      ComponentContext context,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes,
      TestViewComponent state) {
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
    if (o instanceof TestViewComponent) {
      return true;
    }
    return false;
  }

  public static class Builder extends com.facebook.litho.Component.Builder<Builder> {
    TestViewComponent mState;

    private void init(
        ComponentContext context,
        @AttrRes int defStyleAttr,
        @StyleRes int defStyleRes,
        TestViewComponent state) {
      super.init(context, defStyleAttr, defStyleRes, state);
      mState = state;
    }

    public Builder unique() {
      mState.mIsUnique = true;
      return this;
    }

    @Override
    public TestViewComponent build() {
      TestViewComponent state = mState;
      release();
      return state;
    }

    @Override
    protected void release() {
      super.release();
      mState = null;
      sBuilderPool.release(this);
    }

    @Override
    public Builder getThis() {
      return this;
    }
  }
}
