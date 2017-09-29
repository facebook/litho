/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.treeprop;

import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnCreateChildren;

/**
 * Used in TreePropSectionTest.
 */
@GroupSectionSpec
public class TreePropSectionTestLeafGroupSpec {

  public static class Result {
    public Object mProp;
  }

  @OnCreateChildren
  static Children onCreateChildren(
      final SectionContext c,
      @TreeProp TreePropNumberType propA,
      @TreeProp TreePropStringType propB,
      @Prop(optional = true) Result resultPropA,
      @Prop Result resultPropB) {
    if (resultPropA != null) {
      resultPropA.mProp = propA;
    }
    resultPropB.mProp = propB;

    return Children.create().build();
  }
}
