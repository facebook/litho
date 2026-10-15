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

package com.facebook.litho.widget

import com.facebook.litho.ClickEvent
import com.facebook.litho.Column as ColumnBuilder
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.LongClickEvent
import com.facebook.litho.Style
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.PropDefault
import com.facebook.litho.view.onClick
import com.facebook.litho.view.onLongClick

@LayoutSpec
object LayoutWithInnerClickableChildTesterSpec {

  @PropDefault const val shouldSetClickHandler = false

  @PropDefault const val shouldSetLongClickHandler = false

  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop(optional = true) shouldSetClickHandler: Boolean,
      @Prop(optional = true) shouldSetLongClickHandler: Boolean,
  ): Component {
    var style = Style(null, null)
    if (shouldSetClickHandler) {
      style =
          style.plus(
              Style.onClick {
                LayoutWithInnerClickableChildTester.onClickEvent(c).dispatchEvent(it)
              },
          )
    }
    if (shouldSetLongClickHandler) {
      style =
          style.plus(
              Style.onLongClick {
                LayoutWithInnerClickableChildTester.onLongClickEvent(c).dispatchEvent(it) as Boolean
              },
          )
    }
    return ColumnBuilder.create(c).child(SimpleMountSpecTester(style = style)).build()
  }

  @JvmStatic @OnEvent(ClickEvent::class) fun onClickEvent(c: ComponentContext) = Unit

  @JvmStatic
  @OnEvent(LongClickEvent::class)
  fun onLongClickEvent(c: ComponentContext): Boolean = false
}
