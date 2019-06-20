/*
 * Copyright 2019-present Facebook, Inc.
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

package com.facebook.litho.codelab.events

import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Row
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Prop
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaWrap

@Suppress("MagicNumber")
@LayoutSpec
object ColorBoxCollectionSpec {

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext, @Prop items: IntArray): Component {
    val rowBuilder = Row.create(c).wrap(YogaWrap.WRAP)
    items.forEach {
      rowBuilder.child(
          Row.create(c)
              .marginDip(YogaEdge.ALL, 4f)
              .widthDip(48f)
              .heightDip(48f)
              .backgroundColor(it))
    }
    return rowBuilder.build()
  }
}
