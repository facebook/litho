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

package com.facebook.samples.litho.kotlin.animations.statebased

import android.graphics.Color as AndroidColor
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.animation.backgroundColor
import com.facebook.litho.animation.useAnimatedColor
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp
import com.facebook.rendercore.graphics.Color
import com.facebook.rendercore.sp
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify

/**
 * Demonstrates [useAnimatedColor] for smooth ARGB color interpolation.
 *
 * A square cycles through colors on each tap. The color transitions smoothly via per-channel
 * interpolation with spring physics.
 */
@OptIn(ExperimentalLithoApi::class)
class ColorAnimationComponent : KComponent() {

  companion object {
    private val COLORS = arrayOf(
        Color.fromInt(0xFFEA4335.toInt()), // Red
        Color.fromInt(0xFF4285F4.toInt()), // Blue
        Color.fromInt(0xFF34A853.toInt()), // Green
        Color.fromInt(0xFF9C27B0.toInt()), // Purple
    )

    private val COLOR_NAMES = arrayOf("Red", "Blue", "Green", "Purple")
  }

  override fun ComponentScope.render(): Component {
    val colorIndex = useState { 0 }
    val backgroundColor = useAnimatedColor(COLORS[colorIndex.value])

    return Column(
        style = Style.padding(all = 16.dp),
    ) {
      child(
          Text(
              text = "Tap the square to cycle colors",
              textSize = 16.sp,
              textColor = AndroidColor.GRAY,
              style = Style.margin(bottom = 16.dp),
          ),
      )
      child(
          Column(
              alignItems = YogaAlign.CENTER,
              justifyContent = YogaJustify.CENTER,
              style =
                  Style.width(150.dp).height(150.dp).backgroundColor(backgroundColor).onClick {
                    colorIndex.update { (it + 1) % COLORS.size }
                  },
          ) {
            child(
                Text(
                    text = COLOR_NAMES[colorIndex.value],
                    textSize = 20.sp,
                    textColor = AndroidColor.WHITE,
                ),
            )
          },
      )
    }
  }
}
