/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho.testing.treeprop

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnCreateTreeProp
import com.facebook.litho.annotations.Prop
import com.facebook.yoga.YogaAlign

/** Used in [TreePropTest]. */
@LayoutSpec
object TreePropTestParentSpec {

  @JvmStatic
  @OnCreateTreeProp
  fun onCreateTreePropA(c: ComponentContext, @Prop propA: TreePropNumberType): TreePropNumberType =
      propA

  @JvmStatic
  @OnCreateTreeProp
  fun onCreateTreePropB(c: ComponentContext, @Prop propB: TreePropStringType): TreePropStringType =
      propB

  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop resultPropALeaf1: TreePropTestResult,
      @Prop resultPropBLeaf1: TreePropTestResult,
      @Prop resultPropBLeaf2: TreePropTestResult,
      @Prop resultPropAMount: TreePropTestResult
  ): Component =
      Column.create(c)
          .flexShrink(0f)
          .alignContent(YogaAlign.FLEX_START)
          .child(
              TreePropTestMiddle.create(c)
                  .resultPropALeaf1(resultPropALeaf1)
                  .resultPropBLeaf1(resultPropBLeaf1))
          .child(TreePropTestMount.create(c).resultPropA(resultPropAMount))
          .child(TreePropTestLeaf.create(c).resultPropB(resultPropBLeaf2))
          .build()
}
