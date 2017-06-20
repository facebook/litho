/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.AttrRes;
import android.support.annotation.StyleRes;
import android.support.v4.util.Pools;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.ComponentLifecycle;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;

public class TestDrawableComponent extends ComponentLifecycle {
  private static final List<TestDrawableComponent> sInstances = new ArrayList<>();
  private static final Pools.SynchronizedPool<Builder> sBuilderPool =
      new Pools.SynchronizedPool<>(2);

  private static final long CALLS_SHOULD_UPDATE_ON_MOUNT = 1L << 0;
  private static final long IS_PURE_RENDER = 1L << 1;
  private static final long CAN_MEASURE = 1L << 2;
  private static final long USES_DISPLAY_LIST = 1L << 3;
  private static final long IMPLEMENTS_ACCESSIBILITY = 1L << 4;
  private static final long IS_MOUNT_SIZE_DEPENDENT = 1L << 5;

  private final long mProperties;

  private static synchronized TestDrawableComponent get(long properties) {
    for (TestDrawableComponent lifecycle : sInstances) {
      if (lifecycle.mProperties == properties) {
        return lifecycle;
      }
    }

    final TestDrawableComponent lifecycle = new TestDrawableComponent(properties);

    sInstances.add(lifecycle);

    return lifecycle;
  }

  private TestDrawableComponent(long properties) {
    super();
    mProperties = properties;
  }

  @Override
  public boolean shouldUpdate(Component previous, Component next) {
    return !next.equals(previous);
  }

  @Override
  protected boolean callsShouldUpdateOnMount() {
    return (mProperties & CALLS_SHOULD_UPDATE_ON_MOUNT) != 0;
  }

  @Override
  protected boolean isPureRender() {
    return (mProperties & IS_PURE_RENDER) != 0;
  }

  @Override
  protected boolean implementsAccessibility() {
    return (mProperties & IMPLEMENTS_ACCESSIBILITY) != 0;
  }

  @Override
  protected boolean shouldUseDisplayList() {
    return (mProperties & USES_DISPLAY_LIST) != 0;
  }

  @Override
  public boolean isMountSizeDependent() {
    return (mProperties & IS_MOUNT_SIZE_DEPENDENT) != 0;
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
    return (mProperties & CAN_MEASURE) != 0;
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

    long properties = 0;
    
    if (callsShouldUpdateOnMount) { 
      properties |= CALLS_SHOULD_UPDATE_ON_MOUNT;
    }
    if (isPureRender) {
      properties |= IS_PURE_RENDER;
    }
    if (canMeasure) {
      properties |= CAN_MEASURE;
    }
    if (implementsAccessibility) {
      properties |= IMPLEMENTS_ACCESSIBILITY;
    }
    if (usesDisplayList) {
      properties |= USES_DISPLAY_LIST;
    }
    if (isMountSizeDependent) {
      properties |= IS_MOUNT_SIZE_DEPENDENT;
    }

    return newBuilder(context, defStyleAttr, defStyleRes, new State(properties));
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
    Builder builder = sBuilderPool.acquire();
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

    private State(long properties) {
      super(get(properties));
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
      extends com.facebook.litho.Component.Builder<TestDrawableComponent> {
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
      sBuilderPool.release(this);
    }

    public Builder key(String key) {
      super.setKey(key);
      return this;
    }
  }
}
