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

package com.facebook.samples.litho.kotlin.lithography.components

import android.R.drawable.star_off
import android.R.drawable.star_on
import com.facebook.litho.ClickEvent
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Row
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.State

@LayoutSpec
object FavouriteButtonSpec {

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @State favourited: Boolean
  ): Component =
      Row.create(c)
          .backgroundRes(if (favourited) star_on else star_off)
          .widthDip(32f)
          .heightDip(32f)
          .clickHandler(FavouriteButton.onClick(c))
          .build()

  @OnUpdateState
  fun toggleFavourited(favourited: StateValue<Boolean>) = favourited.set(!(favourited.get()!!))

  @OnEvent(ClickEvent::class)
  fun onClick(c: ComponentContext): Unit = FavouriteButton.toggleFavourited(c)
}
