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

import com.facebook.yoga.YogaFlexDirection;

import java.util.ArrayList;
import java.util.List;

import android.support.annotation.AttrRes;
import android.support.annotation.StyleRes;
import android.support.v4.util.Pools;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.ComponentLifecycle;
import com.facebook.litho.Container;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Layout;

public class TestLayoutComponent extends ComponentLifecycle {
  private static final List<TestLayoutComponent> sInstances = new ArrayList<>();
  private static final Pools.SynchronizedPool<Builder> mBuilderPool =
      new Pools.SynchronizedPool<>(2);

  private final boolean mCallsShouldUpdateOnMount;
  private final boolean mIsPureRender;
  private final boolean mHasMountSpecChild;
  private final boolean mIsDelegate;

  private synchronized static TestLayoutComponent get(
      boolean callsShouldUpdateOnMount,
      boolean isPureRender,
      boolean hasMountSpecChild,
      boolean isDelegate) {
    for (TestLayoutComponent lifecycle : sInstances) {
