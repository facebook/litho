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

package com.fblitho.lithoktsample.lithography.components

import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Row
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.yoga.YogaEdge.RIGHT
import com.facebook.yoga.YogaEdge.TOP
import com.facebook.yoga.YogaEdge.ALL
import com.facebook.yoga.YogaPositionType.ABSOLUTE

@LayoutSpec
object ActionsComponentSpec {

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext): Component =
      Row.create(c)
          .backgroundColor(0xDDFFFFFF.toInt())
          .positionType(ABSOLUTE)
          .positionDip(RIGHT, 4f)
          .positionDip(TOP, 4f)
          .paddingDip(ALL, 2f)
          .child(FavouriteButton.create(c))
          .build()
}
