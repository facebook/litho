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

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Prop
import com.facebook.litho.widget.Card

import com.facebook.yoga.YogaEdge.HORIZONTAL
import com.facebook.yoga.YogaEdge.VERTICAL
import com.fblitho.lithoktsample.lithography.data.Artist

@LayoutSpec
object FeedItemCardSpec {

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop artist: Artist): Component =
      Column.create(c)
          .paddingDip(VERTICAL, 8f)
          .paddingDip(HORIZONTAL, 16f)
          .child(
              Card.create(c)
                  .content(
                      FeedItemComponent.create(c)
                          .artist(artist)))
          .build()
}
