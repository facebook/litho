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
import com.facebook.litho.animation.DampingRatio
import com.facebook.litho.animation.Easing
import com.facebook.litho.animation.Stiffness
import com.facebook.litho.animation.keyframes
import com.facebook.litho.animation.snap
import com.facebook.litho.animation.spring
import com.facebook.litho.animation.translationX
import com.facebook.litho.animation.tween
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
import com.facebook.rendercore.ms
import com.facebook.rendercore.sp

/**
 * Side-by-side comparison of all animation spec types: spring, tween, keyframes, and snap.
 *
 * Four colored squares animate translationX simultaneously on tap. Demonstrates the visual
 * difference between each spec type.
 */
@OptIn(ExperimentalLithoApi::class)
class SpecComparisonComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val toggled = useState { false }
    val targetX = if (toggled.value) 200f else 0f

    val springX = useAnimatedFloat(
        targetX,
        spring(stiffness = Stiffness.Medium, dampingRatio = DampingRatio.LowBouncy),
    )
    val tweenX = useAnimatedFloat(targetX, tween(500.ms, easing = Easing.FastOutSlowIn))
    val keyframesX = useAnimatedFloat(
        targetX,
        keyframes(500.ms) {
          0f at 0.ms
          (targetX * 1.2f) at 300.ms using Easing.FastOutSlowIn
          targetX at 500.ms using Easing.EaseOut
        },
    )
    val snapX = useAnimatedFloat(targetX, snap())

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
      child(specRow("Spring", "#EA4335".toColorInt(), springX))
      child(specRow("Tween", "#34A853".toColorInt(), tweenX))
      child(specRow("Keyframes", "#4285F4".toColorInt(), keyframesX))
      child(specRow("Snap", "#FBBC04".toColorInt(), snapX))
    }
  }
}

@OptIn(ExperimentalLithoApi::class)
private fun ComponentScope.specRow(
    label: String,
    color: Int,
    translationX: com.facebook.litho.Value<Float>,
): Component {
  return Column(style = Style.margin(bottom = 12.dp)) {
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
