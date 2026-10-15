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
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.toDrawable
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.flex
import com.facebook.litho.flexbox.layoutDirection
import com.facebook.litho.kotlin.widget.Image
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.kotlin.widget.VerticalScroll
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.view.Shape
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.clip
import com.facebook.litho.view.elevation
import com.facebook.rendercore.dp
import com.facebook.yoga.YogaDirection

/**
 * Demonstrates per-corner [Shape.RoundedRectangle]. Each label states the expected result, which
 * differs before API 33 because Android can only clip to a single-radius rounded rectangle there.
 */
class PerCornerRoundedRectangleExampleComponent : KComponent() {
  override fun ComponentScope.render(): Component =
      VerticalScroll(style = Style.flex(grow = 1f)) {
        Column(style = Style.backgroundColor(Color.CYAN).padding(all = 12.dp)) {
          child(
              label("1. RoundedRectangle(16.dp) vs four 16.dp radii: identical on all API levels"),
          )
          child(
              Row(gap = 12.dp) {
                child(tile(UNIFORM))
                child(tile(ALL_FOUR))
              },
          )
          child(label("2. Top only, 24.dp: exact on all API levels"))
          child(tile(TOP_ONLY))
          child(label("3. Start only, 24.dp, LTR then RTL: the rounded side flips"))
          child(
              Row(gap = 12.dp) {
                child(tile(START_ONLY))
                child(tile(START_ONLY, Style.layoutDirection(YogaDirection.RTL)))
              },
          )
          child(label("4. Mixed 16/16/4/4: exact on API 33+; below 33, 4.dp on all corners"))
          child(tile(MIXED))
          child(label("5. Diagonal 24.dp: exact on API 33+; below 33, 24.dp on all corners"))
          child(tile(DIAGONAL))
          child(label("6. Top only + elevation: below API 33 the shadow extends past the bottom"))
          child(
              Column(style = Style.padding(all = 16.dp)) {
                child(tile(TOP_ONLY, Style.elevation(8.dp), Color.WHITE))
              },
          )
        }
      }
}

private val UNIFORM = Shape.RoundedRectangle(16.dp)
private val ALL_FOUR = Shape.RoundedRectangle(16.dp, 16.dp, 16.dp, 16.dp)
private val TOP_ONLY = Shape.RoundedRectangle(topStart = 24.dp, topEnd = 24.dp)
private val START_ONLY = Shape.RoundedRectangle(topStart = 24.dp, bottomStart = 24.dp)
private val MIXED = Shape.RoundedRectangle(16.dp, 16.dp, 4.dp, 4.dp)
private val DIAGONAL = Shape.RoundedRectangle(topStart = 24.dp, bottomEnd = 24.dp)

private fun ResourcesScope.label(text: String): Component =
    Text(text, style = Style.padding(top = 16.dp, bottom = 8.dp))

private fun ResourcesScope.tile(
    shape: Shape,
    style: Style = Style,
    @ColorInt color: Int = Color.BLUE,
): Component = Image(
    drawable = color.toDrawable(),
    style = Style.width(120.dp).height(80.dp).clip(shape) + style,
)
