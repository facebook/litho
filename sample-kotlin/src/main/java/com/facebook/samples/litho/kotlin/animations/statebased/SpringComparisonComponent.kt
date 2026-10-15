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

import android.graphics.Color
import androidx.core.graphics.toColorInt
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.Value
import com.facebook.litho.animation.DampingRatio
import com.facebook.litho.animation.Stiffness
import com.facebook.litho.animation.spring
import com.facebook.litho.animation.translationX
import com.facebook.litho.animation.useAnimatedFloat
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.useState
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp

/**
 * Visual comparison of spring Stiffness and DampingRatio presets.
 *
 * Six colored squares, each with a different spring configuration, all toggle simultaneously on
 * tap. Shows how stiffness controls speed and damping ratio controls overshoot.
 */
@OptIn(ExperimentalLithoApi::class)
class SpringComparisonComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val toggled = useState { false }
    val targetX = if (toggled.value) 200f else 0f

    return Column(
        style = Style.padding(all = 16.dp).onClick { toggled.update { !it } },
    ) {
      child(
          Text(
              text = "Tap anywhere to toggle",
              textSize = 16.sp,
              textColor = Color.GRAY,
              style = Style.margin(bottom = 16.dp),
          ),
      )
      child(
          springRow(
              "High / No Bounce",
              "#EA4335".toColorInt(),
              useAnimatedFloat(
                  targetX,
                  spring(stiffness = Stiffness.High, dampingRatio = DampingRatio.NoBouncy),
              ),
          ),
      )
      child(
          springRow(
              "Medium / No Bounce (default)",
              "#FBBC04".toColorInt(),
              useAnimatedFloat(
                  targetX,
                  spring(stiffness = Stiffness.Medium, dampingRatio = DampingRatio.NoBouncy),
              ),
          ),
      )
      child(
          springRow(
              "Low / No Bounce",
              "#34A853".toColorInt(),
              useAnimatedFloat(
                  targetX,
                  spring(stiffness = Stiffness.Low, dampingRatio = DampingRatio.NoBouncy),
              ),
          ),
      )
      child(
          springRow(
              "Low / Low Bounce",
              "#4285F4".toColorInt(),
              useAnimatedFloat(
                  targetX,
                  spring(stiffness = Stiffness.Low, dampingRatio = DampingRatio.LowBouncy),
              ),
          ),
      )
      child(
          springRow(
              "Low / Medium Bounce",
              "#FF6D01".toColorInt(),
              useAnimatedFloat(
                  targetX,
                  spring(stiffness = Stiffness.Low, dampingRatio = DampingRatio.MediumBouncy),
              ),
          ),
      )
      child(
          springRow(
              "Low / High Bounce",
              "#46BDC6".toColorInt(),
              useAnimatedFloat(
                  targetX,
                  spring(stiffness = Stiffness.Low, dampingRatio = DampingRatio.HighBouncy),
              ),
          ),
      )
    }
  }
}

@OptIn(ExperimentalLithoApi::class)
private fun ComponentScope.springRow(
    label: String,
    color: Int,
    translationX: Value<Float>,
): Component {
  return Column(style = Style.margin(bottom = 8.dp)) {
    child(
        Row {
          child(
              Column(
                  style =
                      Style.width(40.dp)
                          .height(40.dp)
                          .backgroundColor(color)
                          .translationX(translationX),
              ),
          )
        },
    )
    child(
        Text(
            text = label,
            textSize = 12.sp,
            textColor = Color.DKGRAY,
        ),
    )
  }
}
