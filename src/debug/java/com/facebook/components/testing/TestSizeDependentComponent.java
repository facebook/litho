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

import android.support.v4.util.Pools;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.ComponentLifecycle;
import com.facebook.litho.Container;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.litho.ComponentContext;
import com.facebook.yoga.YogaEdge;

public class TestSizeDependentComponent extends ComponentLifecycle {
  public static TestSizeDependentComponent sInstance = null;
  private static final Pools.SynchronizedPool<Builder> sBuilderPool =
      new Pools.SynchronizedPool<>(2);

  private TestSizeDependentComponent() {}

  @Override
  protected ComponentLayout onCreateLayoutWithSizeSpec(
      ComponentContext c,
      int widthSpec,
      int heightSpec,
      Component _stateObject) {
    final State state = ((State) _stateObject);

    final ComponentLayout.Builder builder1 =
        TestDrawableComponent.create(c, false, true, true, false, false)
            .withLayout().flexShrink(0)
            .backgroundColor(0xFFFF0000);
    final ComponentLayout.Builder builder2 = TestViewComponent.create(c, false, true, true, false)
        .withLayout().flexShrink(0)
        .marginPx(YogaEdge.ALL, 3);

    if (state.hasFixedSizes) {
      builder1
          .widthPx(50)
          .heightPx(50);
      builder2
          .heightPx(20);
    }

    if (state.isDelegate) {
      return builder1.build();
    }

    return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
        .flexDirection(YogaFlexDirection.COLUMN)
        .paddingPx(YogaEdge.ALL, 5)
        .child(
            builder1)
        .child(
            builder2)
        .build();
  }

  @Override
  protected boolean canMeasure() {
    return true;
  }

  @Override
  public MountType getMountType() {
    return MountType.NONE;
  }

  public static synchronized TestSizeDependentComponent get() {
    if (sInstance == null) {
      sInstance = new TestSizeDependentComponent();
    }

    return sInstance;
  }

  public static Builder create(ComponentContext context) {
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }
    builder.init(context, new State());

    return builder;
  }

  public static class State extends Component<TestSizeDependentComponent> implements Cloneable {

    boolean hasFixedSizes;
    boolean isDelegate;

    private State() {
      super(get());
    }

    @Override
    public String getSimpleName() {
      return "TestSizeDependentComponent";
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (other == null || getClass() != other.getClass()) {
        return false;
      }
      State state = (State) other;
      if (this.getId() == state.getId()) {
        return true;
      }

      return true;
    }
  }

  public static class Builder
      extends com.facebook.litho.Component.Builder<TestSizeDependentComponent> {

    State mState;

    private void init(ComponentContext context, State state) {
