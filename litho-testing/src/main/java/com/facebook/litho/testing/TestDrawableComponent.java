/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.AttrRes;
import android.support.annotation.StyleRes;
import android.support.v4.util.Pools;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.ComponentLifecycle;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;

public class TestDrawableComponent extends ComponentLifecycle {
  private static final List<TestDrawableComponent> sInstances = new ArrayList<>();
  private static final Pools.SynchronizedPool<Builder> mBuilderPool =
      new Pools.SynchronizedPool<>(2);
  
  private enum Property {
    CALLS_SHOULD_UPDATE_ON_MOUNT, IS_PURE_REDNER, CAN_MEASURE,
    USES_DISPLAY_LIST, IMPLEMENTS_ACCESSIBILITY, IS_MOUNT_SIZE_DEPENDENT
  }

  private EnumSet<Property>  mProperties;

  private synchronized static TestDrawableComponent get(Set<Property> properties) {
    for (TestDrawableComponent lifecycle : sInstances) {
        if (lifecycle.mProperties.equals(properties)) {
            return lifecycle;
        }
    }

    final TestDrawableComponent lifecycle = new TestDrawableComponent(properties);
    sInstances.add(lifecycle);

    return lifecycle;
  }

  private TestDrawableComponent(Set<Property> properties) {
      super();
      mProperties = EnumSet.copyOf(properties);
  }

  @Override
  public boolean shouldUpdate(Component previous, Component next) {
    return !next.equals(previous);
  }

  @Override
  protected boolean callsShouldUpdateOnMount() {
    return mProperties.contains(Property.CALLS_SHOULD_UPDATE_ON_MOUNT);
  }

  @Override
  protected boolean isPureRender() {
    return mProperties.contains(Property.IS_PURE_REDNER);
  }

  @Override
  protected boolean implementsAccessibility() {
    return mProperties.contains(Property.IMPLEMENTS_ACCESSIBILITY);
  }

  @Override
  protected boolean shouldUseDisplayList() {
    return mProperties.contains(Property.USES_DISPLAY_LIST);
  }

  @Override
  public boolean isMountSizeDependent() {
    return mProperties.contains(Property.IS_MOUNT_SIZE_DEPENDENT);
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

    EnumSet<Property> properties = EnumSet.noneOf(Property.class);

    if (callsShouldUpdateOnMount) {
      properties.add(Property.CALLS_SHOULD_UPDATE_ON_MOUNT);
    }
    if (implementsAccessibility) {
      properties.add(Property.IMPLEMENTS_ACCESSIBILITY);
    }
    if (isMountSizeDependent) {
      properties.add(Property.IS_MOUNT_SIZE_DEPENDENT);
    }
    if (usesDisplayList) {
      properties.add(Property.USES_DISPLAY_LIST);
    }
    if (isPureRender) {
      properties.add(Property.IS_PURE_REDNER);
    }
    if (canMeasure) {
      properties.add(Property.CAN_MEASURE);
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

    private State(Set<Property> properties) {
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
      mBuilderPool.release(this);
    }

    public Builder key(String key) {
      super.setKey(key);
      return this;
    }
  }
}
