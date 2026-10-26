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

package com.facebook.samples.litho.kotlin.commonprops

import android.graphics.Color
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.alignSelf
import com.facebook.litho.flexbox.position
import com.facebook.litho.flexbox.positionType
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.view.Shape
import com.facebook.litho.view.background
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.clip
import com.facebook.rendercore.dp
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaPositionType

/**
 * Demonstrates `Style.background(color, shape)`, which paints a filled rounded rectangle or circle
 * without clipping children, compared with `Style.clip(shape)`, which clips children to the shape.
 */
class ShapeBackgroundExampleComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val labelStyle = Style.padding(all = 12.dp)
    return Column(style = Style.padding(all = 16.dp)) {
      child(Text("Style.background(color, Shape.RoundedRectangle(16.dp))", style = labelStyle))
      child(Column(style = Style.width(200.dp).height(80.dp).background(Color.BLUE, ROUNDED_SHAPE)))

      child(Text("Style.background(color, Shape.Circle)", style = labelStyle))
      child(Column(style = Style.width(80.dp).height(80.dp).background(Color.RED, Shape.Circle)))

      child(Text("Shape.Circle on a 200x80 box stays a centered circle", style = labelStyle))
      child(
          Column(style = Style.backgroundColor(Color.LTGRAY).alignSelf(YogaAlign.FLEX_START)) {
            child(
                Column(
                    style = Style.width(200.dp).height(80.dp).background(Color.RED, Shape.Circle),
                ),
            )
          },
      )

      child(
          Text(
              "Overflowing badge: background(color, shape) keeps it (left), clip(shape) cuts it (right)",
              style = labelStyle,
          ),
      )
      child(
          Row(style = Style.padding(all = 12.dp)) {
            child(BadgedBox(Style.background(Color.BLUE, ROUNDED_SHAPE)))
            child(
                BadgedBox(
                    Style.backgroundColor(Color.BLUE).clip(ROUNDED_SHAPE).margin(start = 32.dp),
                ),
            )
          },
      )
    }
  }
}

private val ROUNDED_SHAPE = Shape.RoundedRectangle(16.dp)

private fun ComponentScope.BadgedBox(boxStyle: Style): Component =
    Column(style = Style.width(100.dp).height(100.dp) + boxStyle) {
      child(
          Column(
              style =
                  Style.width(32.dp)
                      .height(32.dp)
                      .positionType(YogaPositionType.ABSOLUTE)
                      .position(top = (-12).dp, end = (-12).dp)
                      .background(Color.GREEN, Shape.Circle),
          ),
      )
    }
