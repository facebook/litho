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
import androidx.core.graphics.drawable.toDrawable
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.alignSelf
import com.facebook.litho.kotlin.widget.Card
import com.facebook.litho.kotlin.widget.Image
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.useCached
import com.facebook.litho.view.Shape
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.clip
import com.facebook.rendercore.dp
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify

/**
 * Demonstrates different approaches to rounded corners and circular clipping in Litho.
 *
 * This example shows three approaches:
 * 1. **Style.clip(Shape.RoundedRectangle())** - Recommended for rounded corners (hardware clipping)
 * 2. **Style.clip(Shape.Circle)** - Recommended for circular elements like avatars (hardware
 *    clipping)
 * 3. **Card** - Software overlay clipping (requires clippingColor to match parent background)
 */
class RoundedCornerExampleComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val cornerRadius = 12.dp
    val roundedShape = useCached(cornerRadius) { Shape.RoundedRectangle(cornerRadius) }
    val imageStyle = Style.height(120.dp).width(120.dp)
    return Column(style = Style.backgroundColor(Color.CYAN), justifyContent = YogaJustify.CENTER) {
      // Approach 1: Style.clip(Shape.RoundedRectangle()) - recommended for rounded corners
      child(
          Text(
              "Style.clip(Shape.RoundedRectangle()) - recommended approach",
              style = Style.padding(all = 12.dp),
          ),
      )
      child(
          Image(
              drawable = Color.MAGENTA.toDrawable(),
              style = imageStyle.alignSelf(YogaAlign.CENTER).clip(roundedShape),
          ),
      )

      // Approach 2: Style.clip(Shape.Circle) - recommended for circular elements
      child(
          Text(
              "Style.clip(Shape.Circle) - for circular avatars, FABs, icons",
              style = Style.padding(all = 12.dp),
          ),
      )
      child(
          Image(
              drawable = Color.GREEN.toDrawable(),
              style = imageStyle.alignSelf(YogaAlign.CENTER).clip(Shape.Circle),
          ),
      )

      // Approach 3: Card with software clipping (legacy)
      child(
          Text(
              "Card - software clipping (requires clippingColor to match parent)",
              style = Style.padding(all = 12.dp),
          ),
      )
      child(
          Card(
              cornerRadius = cornerRadius,
              clippingColor = Color.CYAN,
              style = Style.alignSelf(YogaAlign.CENTER),
          ) {
            Image(
                drawable = Color.BLUE.toDrawable(),
                style = imageStyle,
            )
          },
      )
    }
  }
}
