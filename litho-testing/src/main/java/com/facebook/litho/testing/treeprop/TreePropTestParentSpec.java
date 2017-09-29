/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.treeprop;

import com.facebook.litho.Column;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.Prop;
import com.facebook.yoga.YogaAlign;

/**
 * Used in {@link TreePropTest}.
 */
@LayoutSpec
public class TreePropTestParentSpec {

  @OnCreateTreeProp
  static TreePropNumberType onCreateTreePropA(
      ComponentContext c,
      @Prop TreePropNumberType propA) {
    return propA;
  }

  @OnCreateTreeProp
  static TreePropStringType onCreateTreePropB(
      ComponentContext c,
      @Prop TreePropStringType propB) {
    return propB;
  }

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop TreePropTestResult resultPropALeaf1,
      @Prop TreePropTestResult resultPropBLeaf1,
      @Prop TreePropTestResult resultPropBLeaf2,
      @Prop TreePropTestResult resultPropAMount) {
    return Column.create(c)
        .flexShrink(0)
        .alignContent(YogaAlign.FLEX_START)
        .child(TreePropTestMiddle.create(c)
            .resultPropALeaf1(resultPropALeaf1)
            .resultPropBLeaf1(resultPropBLeaf1))
        .child(TreePropTestMount.create(c)
            .resultPropA(resultPropAMount))
        .child(TreePropTestLeaf.create(c)
            .resultPropB(resultPropBLeaf2))
        .build();
  }
}
