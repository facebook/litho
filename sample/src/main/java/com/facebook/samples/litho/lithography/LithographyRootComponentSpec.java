/**
 * Copyright 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE-examples file in the root directory of this source tree.
 */

package com.facebook.samples.litho.lithography;

import com.facebook.yoga.YogaAlign;

import com.facebook.yoga.YogaFlexDirection;

import com.facebook.litho.ComponentLayout;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Recycler;
import com.facebook.litho.widget.RecyclerBinder;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
public class LithographyRootComponentSpec {

  private static final String MAIN_SCREEN = "main_screen";

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop final RecyclerBinder recyclerBinder) {

    return Recycler.create(c)
        .binder(recyclerBinder)
        .withLayout().flexShrink(0)
        .paddingDip(YogaEdge.TOP, 8)
        .testKey(MAIN_SCREEN)
        .build();
