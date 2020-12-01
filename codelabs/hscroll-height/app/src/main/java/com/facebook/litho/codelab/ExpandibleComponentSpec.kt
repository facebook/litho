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

package com.facebook.litho.codelab

import com.facebook.litho.ClickEvent
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.Param
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State
import com.facebook.litho.widget.SolidColor

/**
 * Component which toggles between a maximum and minimum height when clicked, starting from an
 * initial height.
 */
@Suppress("MagicNumber")
@LayoutSpec
object ExpandibleComponentSpec {

  @OnCreateInitialState
  fun onCreateInitialState(
      c: ComponentContext,
      @Prop initialHeight: Float,
      currentHeight: StateValue<Float>
  ) {
    currentHeight.set(initialHeight)
  }

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop color: Int,
      @State currentHeight: Float
  ): Component {
    return SolidColor.create(c)
        .color(color)
        .heightDip(currentHeight)
        .widthDip(100f)
        .clickHandler(ExpandibleComponent.onClick(c))
        .build()
  }

  @OnUpdateState
  fun onUpdateState(
      currentHeight: StateValue<Float>,
      @Param collapse: Float,
      @Param expand: Float
  ) {
    val currentState = currentHeight.get()
    if (currentState != null) {
      if (currentState < expand) {
        currentHeight.set(expand)
      } else {
        currentHeight.set(collapse)
      }
    }
  }

  @OnEvent(ClickEvent::class)
  fun onClick(c: ComponentContext, @Prop collapseHeight: Float, @Prop expandHeight: Float) {
    ExpandibleComponent.onUpdateState(c, collapseHeight, expandHeight)
  }
}
