/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.testing.treeprop;

import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.testing.treeprop.TreePropSectionTestLeafGroupSpec.Result;

/** Used in TreePropSectionTest. */
@GroupSectionSpec
public class TreePropSectionTestMiddleGroupSpec {

  @OnCreateTreeProp
  static TreePropStringType onCreateTreePropB(
      SectionContext c, @TreeProp TreePropStringType propB) {
    return new TreePropStringType(propB.getValue() + "_changed");
  }

  @OnCreateChildren
  static Children onCreateChildren(
      final SectionContext c, @Prop Result resultPropALeaf1, @Prop Result resultPropBLeaf1) {
    return Children.create()
        .child(
            TreePropSectionTestLeafGroup.create(c)
                .resultPropA(resultPropALeaf1)
                .resultPropB(resultPropBLeaf1))
        .build();
  }
}
