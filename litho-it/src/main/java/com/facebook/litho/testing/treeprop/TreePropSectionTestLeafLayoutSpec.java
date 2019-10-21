/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.testing.treeprop;

import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.TreeProp;

/**
 * Used in {@link TreePropSectionTestLeafGroupSpec} to mutate the treeprops. Currently without any
 * effect as treeprops aren't propagated from Sections. See T29504095.
 */
@LayoutSpec
public class TreePropSectionTestLeafLayoutSpec {
  @OnCreateLayout
  static Component onCreateLayout(
      final ComponentContext c,
      @TreeProp TreePropNumberType propA,
      @TreeProp TreePropStringType propB,
      @Prop(optional = true) TreePropSectionTestLeafGroupSpec.Result resultPropA,
      @Prop TreePropSectionTestLeafGroupSpec.Result resultPropB) {

    if (resultPropA != null && propA != null) {
      resultPropA.mProp = new TreePropNumberType(propA.getValue() + 1);
    }

    if (resultPropB != null && propB != null) {
      resultPropB.mProp = new TreePropStringType(propB.getValue() + "_changed_again");
    }

    return Column.create(c).build();
  }
}
