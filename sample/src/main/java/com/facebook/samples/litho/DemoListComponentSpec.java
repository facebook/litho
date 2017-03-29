/**
 * Copyright 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE-examples file in the root directory of this source tree.
 */

package com.facebook.samples.litho;

import com.facebook.yoga.YogaAlign;

import com.facebook.yoga.YogaFlexDirection;

import android.support.v7.widget.OrientationHelper;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.widget.LinearLayoutInfo;
import com.facebook.litho.widget.Recycler;
import com.facebook.litho.widget.RecyclerBinder;

@LayoutSpec
public class DemoListComponentSpec {

  private static final String MAIN_SCREEN = "main_screen";

  @OnCreateLayout
  static ComponentLayout onCreateLayout(ComponentContext c) {
    final RecyclerBinder recyclerBinder = new RecyclerBinder(
        c,
        4.0f,
        new LinearLayoutInfo(c, OrientationHelper.VERTICAL, false));

    Demos.addAllToBinder(recyclerBinder, c);

    return Recycler.create(c)
        .binder(recyclerBinder)
        .withLayout().flexShrink(0)
