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
import com.facebook.litho.animation.Easing
import com.facebook.litho.animation.keyframes
import com.facebook.litho.animation.translationX
import com.facebook.litho.animation.translationY
import com.facebook.litho.animation.useAnimationGroup
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.useState
import com.facebook.litho.view.Shape
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.clip
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp
import com.facebook.rendercore.ms
import com.facebook.rendercore.sp

/**
 * Moves a dot along a curved path with [keyframes].
 *
 * One [useAnimationGroup] animates `translationX` and `translationY`, using keyframes at the same
 * timestamps for both. X moves at a constant rate. Y eases up to a peak above both endpoints and
 * then eases down, so the dot traces an arc. Tap to send it to the other corner and back.
 */
@OptIn(ExperimentalLithoApi::class)
class PathMotionComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val atEnd = useState { false }

    val travelX = (STAGE_WIDTH.toPixels() - DOT_SIZE_DP.dp.toPixels()).toFloat()
    val drop = DROP_DP.dp.toPixels().toFloat()
    val lift = LIFT_DP.dp.toPixels().toFloat()

    // start_example
    val motion = useAnimationGroup(atEnd.value, label = "path")
    val x =
        motion.animateFloat(
            transitionSpec = {
              val endX = if (targetState) travelX else 0f
              keyframes<Float>(DURATION) {
                (travelX / 2f) at HALF_DURATION
                endX at DURATION
              }
            },
            label = "x",
        ) { end ->
          if (end) travelX else 0f
        }
    val y =
        motion.animateFloat(
            transitionSpec = {
              val endY = if (targetState) 0f else drop
              keyframes<Float>(DURATION) {
                (-lift) at HALF_DURATION using Easing.EaseOut
                endY at DURATION using Easing.EaseIn
              }
            },
            label = "y",
        ) { end ->
          if (end) 0f else drop
        }
    // end_example

    return Column(style = Style.padding(all = 16.dp).onClick { atEnd.update { !it } }) {
      child(
          Text(
              text = "Tap anywhere to send the dot along the arc",
              textSize = 16.sp,
              textColor = Color.GRAY,
              style = Style.margin(bottom = 16.dp),
          ),
      )
      child(
          Column(
              style =
                  Style.width(STAGE_WIDTH)
                      .height((LIFT_DP + DROP_DP + DOT_SIZE_DP).dp)
                      .backgroundColor("#EEF1F5".toColorInt()),
          ) {
            child(
                Column(
                    style =
                        Style.width(DOT_SIZE_DP.dp)
                            .height(DOT_SIZE_DP.dp)
                            .margin(top = LIFT_DP.dp)
                            .backgroundColor("#4285F4".toColorInt())
                            .clip(Shape.Circle)
                            .translationX(x)
                            .translationY(y),
                ),
            )
          },
      )
    }
  }
}

private val DURATION = 1200.ms
private val HALF_DURATION = 600.ms
private val STAGE_WIDTH = 320.dp
private const val DOT_SIZE_DP = 32
private const val LIFT_DP = 80
private const val DROP_DP = 160
