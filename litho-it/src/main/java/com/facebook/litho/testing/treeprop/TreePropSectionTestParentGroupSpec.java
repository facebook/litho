/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.treeprop;

import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.testing.treeprop.TreePropSectionTestLeafGroupSpec.Result;

/**
 * Used in TreePropSectionTest.
 */
@GroupSectionSpec
public class TreePropSectionTestParentGroupSpec {

  @OnCreateTreeProp
  static TreePropNumberType onCreateTreePropA(
      SectionContext c,
      @Prop TreePropNumberType propA) {
    return propA;
  }

  @OnCreateTreeProp
  static TreePropStringType onCreateTreePropB(
      SectionContext c,
      @Prop TreePropStringType propB) {
    return propB;
  }

  @OnCreateChildren
  static Children onCreateChildren(
      final SectionContext c,
      @Prop Result resultPropALeaf1,
      @Prop Result resultPropBLeaf1,
      @Prop Result resultPropBLeaf2) {

    return Children.create()
        .child(TreePropSectionTestMiddleGroup.create(c)
            .resultPropALeaf1(resultPropALeaf1)
            .resultPropBLeaf1(resultPropBLeaf1))
        .child(TreePropSectionTestLeafGroup.create(c)
            .resultPropB(resultPropBLeaf2))
        .build();
  }
}
