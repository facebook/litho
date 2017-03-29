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

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.AttrRes;
import android.support.annotation.StyleRes;
import android.support.v4.util.Pools;

import com.facebook.components.Component;
import com.facebook.components.ComponentLayout;
import com.facebook.components.ComponentLifecycle;
import com.facebook.components.ComponentContext;
import com.facebook.components.Size;
import com.facebook.components.SizeSpec;

public class TestDrawableComponent extends ComponentLifecycle {
  private static final List<TestDrawableComponent> sInstances = new ArrayList<>();
  private static final Pools.SynchronizedPool<Builder> mBuilderPool =
      new Pools.SynchronizedPool<>(2);

  private final boolean mCallsShouldUpdateOnMount;
  private final boolean mIsPureRender;
  private final boolean mCanMeasure;
  private final boolean mUsesDisplayList;
  private final boolean mImplementsAccessibility;
  private final boolean mIsMountSizeDependent;

  private synchronized static TestDrawableComponent get(
      boolean callsShouldUpdateOnMount,
      boolean isPureRender,
      boolean canMeasure,
      boolean implementsAccessibility,
      boolean usesDisplayList,
      boolean isMountSizeDependent) {
    for (TestDrawableComponent lifecycle : sInstances) {
      if (lifecycle.mCallsShouldUpdateOnMount == callsShouldUpdateOnMount &&
          lifecycle.mIsPureRender == isPureRender &&
          lifecycle.mCanMeasure == canMeasure &&
          lifecycle.mImplementsAccessibility == implementsAccessibility &&
          lifecycle.mUsesDisplayList == usesDisplayList &&
          lifecycle.mIsMountSizeDependent == isMountSizeDependent) {
        return lifecycle;
      }
    }

    final TestDrawableComponent lifecycle = new TestDrawableComponent(
        callsShouldUpdateOnMount,
        isPureRender,
        canMeasure,
        implementsAccessibility,
        usesDisplayList,
        isMountSizeDependent);

    sInstances.add(lifecycle);

    return lifecycle;
  }

  private TestDrawableComponent(
      boolean callsShouldUpdateOnMount,
      boolean isPureRender,
      boolean canMeasure,
      boolean implementsAccessibility,
      boolean usesDisplayList,
      boolean isMountSizeDependent) {
    super();

    mCallsShouldUpdateOnMount = callsShouldUpdateOnMount;
    mIsPureRender = isPureRender;
    mCanMeasure = canMeasure;
    mImplementsAccessibility = implementsAccessibility;
    mUsesDisplayList = usesDisplayList;
    mIsMountSizeDependent = isMountSizeDependent;
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
  protected boolean implementsAccessibility() {
    return mImplementsAccessibility;
  }

  @Override
  protected boolean shouldUseDisplayList() {
    return mUsesDisplayList;
  }

  @Override
  public boolean isMountSizeDependent() {
    return mIsMountSizeDependent;
  }

  @Override
  protected Object onCreateMountContent(ComponentContext c) {
    return new ColorDrawable();
  }

  @Override
  protected void onMount(ComponentContext c, Object convertDrawable, Component _stateObject) {
    State state = (State) _stateObject;
    ((ColorDrawable) convertDrawable).setColor(state.color);

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
    State state = (State) component;
    state.onMeasureCalled();

    size.width = (state.measuredWidth != -1)
        ? SizeSpec.resolveSize(widthSpec, state.measuredWidth)
        : width;
    size.height = (state.measuredHeight != -1)
        ? SizeSpec.resolveSize(heightSpec, state.measuredHeight)
        : height;
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
    return MountType.DRAWABLE;
  }

  public static Builder create(
      ComponentContext context,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes) {
    return create(context, defStyleAttr, defStyleRes, true, true, true, false, false);
  }

  public static Builder create(
      ComponentContext context,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes,
      boolean callsShouldUpdateOnMount,
      boolean isPureRender,
      boolean canMeasure,
      boolean implementsAccessibility,
      boolean usesDisplayList) {
    return create(
        context,
        defStyleAttr,
        defStyleRes,
        callsShouldUpdateOnMount,
        isPureRender,
        canMeasure,
        implementsAccessibility,
        usesDisplayList,
        false);
  }

  public static Builder create(
      ComponentContext context,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes,
      boolean callsShouldUpdateOnMount,
      boolean isPureRender,
      boolean canMeasure,
      boolean implementsAccessibility,
      boolean usesDisplayList,
      boolean isMountSizeDependent) {
    return newBuilder(
        context,
        defStyleAttr,
        defStyleRes,
        new State(
            callsShouldUpdateOnMount,
            isPureRender,
            canMeasure,
            implementsAccessibility,
            usesDisplayList,
            isMountSizeDependent));
  }

  public static Builder create(ComponentContext context) {
    return create(context, 0, 0, true, true, true, false, false, false);
  }

  public static Builder create(
      ComponentContext context,
      boolean callsShouldUpdateOnMount,
      boolean isPureRender,
      boolean canMeasure,
      boolean implementsAccessibility,
      boolean usesDisplayList) {
    return create(
        context,
        0,
        0,
        callsShouldUpdateOnMount,
        isPureRender,
        canMeasure,
        implementsAccessibility,
        usesDisplayList);
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

  public static class State extends TestComponent<TestDrawableComponent> implements Cloneable {
    int color = Color.BLACK;
    int measuredWidth = -1;
    int measuredHeight = -1;

    private State(
        boolean callsShouldUpdateOnMount,
        boolean isPureRender,
        boolean canMeasure,
        boolean implementsAccessibility,
        boolean usesDisplayList,
        boolean isMountSizeDependent) {
      super(get(
          callsShouldUpdateOnMount,
          isPureRender,
          canMeasure,
          implementsAccessibility,
          usesDisplayList,
          isMountSizeDependent));
    }

    @Override
    public int hashCode() {
      return super.hashCode() + color;
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
        State s = (State) o;
        return color == s.color;
      }
      return false;
    }
  }

  public static class Builder
      extends com.facebook.components.Component.Builder<TestDrawableComponent> {
    State mState;

    private void init(
        ComponentContext context,
        @AttrRes int defStyleAttr,
        @StyleRes int defStyleRes,
        State state) {
      super.init(context, defStyleAttr, defStyleRes, state);
      mState = state;
    }

    public Builder measuredWidth(int width) {
      mState.measuredWidth = width;
      return this;
    }

    public Builder measuredHeight(int height) {
      mState.measuredHeight = height;
      return this;
    }

    public Builder color(int color) {
      mState.color = color;
      return this;
    }

    public Builder unique() {
      mState.mIsUnique = true;
      return this;
    }

    @Override
    public TestComponent<TestDrawableComponent> build() {
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
