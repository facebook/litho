/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.treeprop;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.TreeProp;

/**
 * Used in {@link TreePropTest}.
 */
@LayoutSpec
public class TreePropTestMiddleSpec {

  @OnCreateTreeProp
  static TreePropStringType onCreateTreePropB(
      ComponentContext c,
      @TreeProp TreePropStringType propB) {
    return new TreePropStringType(propB.getValue() + "_changed");
  }

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop TreePropTestResult resultPropALeaf1,
      @Prop TreePropTestResult resultPropBLeaf1) {
    return TreePropTestLeaf.create(c)
        .resultPropA(resultPropALeaf1)
        .resultPropB(resultPropBLeaf1)
        .buildWithLayout();
  }
}
