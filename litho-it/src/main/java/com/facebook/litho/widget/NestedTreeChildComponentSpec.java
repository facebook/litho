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

package com.facebook.litho.widget;

import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.widget.NestedTreeComponentSpec.C;
import com.facebook.litho.widget.NestedTreeParentComponentSpec.B;
import com.facebook.litho.widget.RootComponentWithTreePropsSpec.A;

@LayoutSpec
public class NestedTreeChildComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext context, @TreeProp A a, @TreeProp B b, @TreeProp C c) {
    if (a == null) {
      throw new IllegalStateException("A is null");
    }
    if (b == null) {
      throw new IllegalStateException("B is null");
    }
    if (context == null) {
      throw new IllegalStateException("C is null");
    }
    return Column.create(context)
        .key("Column")
        .child(CardClip.create(context).key("CardClip").widthDip(10).heightDip(10))
        .build();
  }
}
