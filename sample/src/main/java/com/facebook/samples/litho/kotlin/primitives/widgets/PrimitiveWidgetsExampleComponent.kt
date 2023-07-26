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

package com.facebook.samples.litho.kotlin.primitives.widgets

import android.graphics.Color
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.FlexboxContainerScope
import com.facebook.litho.KComponent
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.view.background
import com.facebook.litho.view.backgroundColor
import com.facebook.rendercore.dp
import com.facebook.rendercore.drawableRes
import com.facebook.samples.litho.R.drawable.ic_launcher
import com.facebook.samples.litho.kotlin.drawable.RoundedRect

class PrimitiveWidgetsExampleComponent : KComponent() {
  override fun ComponentScope.render(): Component {

    return Column {
      child(Text("CardClip"))
      child(
          CardClip(
              clippingColor = Color.WHITE,
              cornerRadius = 20f,
              style = Style.width(60.dp).height(60.dp).backgroundColor(Color.GRAY)))
      child(Text("CardShadow"))
      child(
          CardShadow(
              shadowEndColor = Color.GRAY,
              shadowSize = 100f,
              cornerRadius = 20f,
              style = Style.width(60.dp).height(60.dp)))
      child(Text("Image"))
      child(Image(drawable = drawableRes(ic_launcher), style = Style.width(100.dp).height(100.dp)))
      child(Text("Progress"))
      child(Progress(style = Style.width(100.dp).height(100.dp)))
      child(Text("Horizontal Scroll"))
      child(HorizontalScroll { Row { getComponents(this) } })
      child(Text("Vertical Scroll"))
      child(VerticalScroll(style = Style.height(100.dp)) { Column { getComponents(this) } })
    }
  }

  private fun ResourcesScope.getComponents(flexboxContainerScope: FlexboxContainerScope) {
    for (i in 1..10) {
      flexboxContainerScope.child(
          Column(
              style =
                  Style.width(50.dp)
                      .height(50.dp)
                      .margin(all = 5.dp)
                      .background(RoundedRect(if (i % 2 == 0) 0xff666699 else 0xffd9d9d9, 8.dp))))
    }
  }
}
