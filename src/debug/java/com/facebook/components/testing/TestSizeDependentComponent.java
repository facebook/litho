/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components.testing;

import android.support.v4.util.Pools;

import com.facebook.components.Component;
import com.facebook.components.ComponentLayout;
import com.facebook.components.ComponentLifecycle;
import com.facebook.components.Container;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.components.ComponentContext;
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
            .withLayout()
            .backgroundColor(0xFFFF0000);
    final ComponentLayout.Builder builder2 = TestViewComponent.create(c, false, true, true, false)
        .withLayout()
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

    return Container.create(c)
