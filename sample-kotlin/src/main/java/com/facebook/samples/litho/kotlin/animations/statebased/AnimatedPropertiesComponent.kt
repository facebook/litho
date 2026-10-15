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
import com.facebook.litho.animation.alpha
import com.facebook.litho.animation.elevation
import com.facebook.litho.animation.height
import com.facebook.litho.animation.rotation
import com.facebook.litho.animation.scale
import com.facebook.litho.animation.translationX
import com.facebook.litho.animation.translationY
import com.facebook.litho.animation.useAnimationGroup
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.useState
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp
import com.facebook.yoga.YogaAlign

/**
 * Demonstrates all animatable Style properties in one place.
 *
 * A card toggles between two states, animating every supported property via [useAnimationGroup]:
 * alpha, rotation, scale, translationX, translationY, elevation, and height.
 */
@OptIn(ExperimentalLithoApi::class)
class AnimatedPropertiesComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val toggled = useState { false }
    val transition = useAnimationGroup(toggled.value, label = "properties")

    val alpha = transition.animateFloat(label = "alpha") { if (it) 0.5f else 1f }
    val rotation = transition.animateFloat(label = "rotation") { if (it) 15f else 0f }
    val scale = transition.animateFloat(label = "scale") { if (it) 0.8f else 1f }
    val translationX = transition.animateFloat(label = "translationX") { if (it) 100f else 0f }
    val translationY = transition.animateFloat(label = "translationY") { if (it) 50f else 0f }
    val elevation = transition.animateFloat(label = "elevation") { if (it) 16f else 0f }
    val height = transition.animateDimen(label = "height") { if (it) 200.dp else 150.dp }

    return Column(style = Style.padding(all = 16.dp)) {
      child(
          Text(
              text = "Tap card to toggle all properties",
              textSize = 16.sp,
              textColor = Color.GRAY,
              style = Style.margin(bottom = 16.dp),
          ),
      )
      child(
          Column(
              alignItems = YogaAlign.CENTER,
              style =
                  Style.width(200.dp)
                      .height(height)
                      .backgroundColor("#4285F4".toColorInt())
                      .padding(all = 16.dp)
                      .alpha(alpha)
                      .rotation(rotation)
                      .scale(scale)
                      .translationX(translationX)
                      .translationY(translationY)
                      .elevation(elevation)
                      .onClick { toggled.update { !it } },
          ) {
            child(propertyLabel("alpha", if (toggled.value) "0.5" else "1.0"))
            child(propertyLabel("rotation", if (toggled.value) "15°" else "0°"))
            child(propertyLabel("scale", if (toggled.value) "0.8" else "1.0"))
            child(propertyLabel("translationX", if (toggled.value) "100" else "0"))
            child(propertyLabel("translationY", if (toggled.value) "50" else "0"))
            child(propertyLabel("elevation", if (toggled.value) "16" else "0"))
            child(propertyLabel("height", if (toggled.value) "200dp" else "150dp"))
          },
      )
    }
  }
}

private fun ComponentScope.propertyLabel(name: String, value: String): Component {
  return Text(
      text = "$name: $value",
      textSize = 11.sp,
      textColor = Color.WHITE,
  )
}
