/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing;

import android.support.v4.util.Pools;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.yoga.YogaEdge;

public class TestSizeDependentComponent extends Component {

  private static final Pools.SynchronizedPool<Builder> sBuilderPool =
      new Pools.SynchronizedPool<>(2);

  private TestSizeDependentComponent() {}

  @Override
  protected ComponentLayout onCreateLayoutWithSizeSpec(
      ComponentContext c,
      int widthSpec,
      int heightSpec,
      Component _stateObject) {
    final TestSizeDependentComponent state = ((TestSizeDependentComponent) _stateObject);

    final Component.Builder builder1 =
        TestDrawableComponent.create(c, false, true, true, false, false)
            .flexShrink(0)
            .backgroundColor(0xFFFF0000);
    final Component.Builder builder2 = TestViewComponent.create(c, false, true, true, false)
        .flexShrink(0)
        .marginPx(YogaEdge.ALL, 3);

    if (state.hasFixedSizes) {
      builder1
          .widthPx(50)
          .heightPx(50);
      builder2
          .heightPx(20);
    }

    if (state.isDelegate) {
      return builder1.buildWithLayout();
    }

    return Column.create(c)
        .paddingPx(YogaEdge.ALL, 5)
        .child(builder1)
        .child(builder2)
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

  public static Builder create(ComponentContext context) {
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }
    builder.init(context, new TestSizeDependentComponent());

    return builder;
  }

  boolean hasFixedSizes;
  boolean isDelegate;

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
    TestSizeDependentComponent state = (TestSizeDependentComponent) other;
    if (this.getId() == state.getId()) {
      return true;
    }

    return true;
  }

  public static class Builder
      extends com.facebook.litho.Component.Builder<TestSizeDependentComponent, Builder> {

    TestSizeDependentComponent mTestSizeDependentComponent;

    private void init(ComponentContext context, TestSizeDependentComponent state) {
      super.init(context, 0, 0, state);
      mTestSizeDependentComponent = state;
    }

    public Builder setFixSizes(boolean hasFixSizes) {
      mTestSizeDependentComponent.hasFixedSizes = hasFixSizes;
      return this;
    }

    public Builder setDelegate(boolean isDelegate) {
      mTestSizeDependentComponent.isDelegate = isDelegate;
      return this;
    }

    @Override
    public Builder getThis() {
      return this;
    }

    @Override
    public Component build() {
      TestSizeDependentComponent testSizeDependentComponent = mTestSizeDependentComponent;
      release();
      return testSizeDependentComponent;
    }

    @Override
    protected void release() {
      super.release();
      mTestSizeDependentComponent = null;
      sBuilderPool.release(this);
    }
  }
}
