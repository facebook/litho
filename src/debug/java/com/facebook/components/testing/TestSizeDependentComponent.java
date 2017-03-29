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
