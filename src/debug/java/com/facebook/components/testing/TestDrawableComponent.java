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
