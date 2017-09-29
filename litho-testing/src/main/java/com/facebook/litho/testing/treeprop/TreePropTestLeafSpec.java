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
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.TreeProp;

/**
 * Used in {@link TreePropTest}.
 */
@LayoutSpec
public class TreePropTestLeafSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @TreeProp TreePropNumberType propA,
      @TreeProp TreePropStringType propB,
      @Prop(optional = true) TreePropTestResult resultPropA,
      @Prop TreePropTestResult resultPropB) {
    if (resultPropA != null) {
      resultPropA.mProp = propA;
    }
    resultPropB.mProp = propB;
    return null;
  }
}
