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
import com.facebook.litho.animation.scale
import com.facebook.litho.animation.spring
import com.facebook.litho.animation.translationX
import com.facebook.litho.animation.tween
import com.facebook.litho.animation.useAnimationGroup
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.alignSelf
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.onCleanup
import com.facebook.litho.useEffect
import com.facebook.litho.useState
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp
import com.facebook.rendercore.ms
import com.facebook.rendercore.sp
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify

/**
 * Runs code when an animation finishes, using `onSettled` on a [useAnimationGroup].
 *
 * One group drives the block's position and scale. `onSettled` fires once, after every property in
 * the group has settled. Here it increments a counter. With loop on, it also flips the target, so
 * the group starts again in the other direction.
 */
@OptIn(ExperimentalLithoApi::class)
class AnimationCallbackComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val atEnd = useState { false }
    val animating = useState { false }
    val settledCount = useState { 0 }
    val looping = useState { false }
    val isLooping = looping.value

    val travelX = (TRACK_WIDTH.toPixels() - BLOCK_SIZE.toPixels()).toFloat()

    // start_example
    val motion = useAnimationGroup(atEnd.value, label = "callback")
    val x =
        motion.animateFloat(
            transitionSpec = { tween(MOVE_DURATION, easing = Easing.FastOutSlowIn) },
            label = "x",
        ) { end ->
          if (end) travelX else 0f
        }
    val scale =
        motion.animateFloat(
            transitionSpec = {
              spring(stiffness = Stiffness.Low, dampingRatio = DampingRatio.MediumBouncy)
            },
            label = "scale",
        ) { end ->
          if (end) 1.4f else 1f
        }

    useEffect(isLooping) {
      motion.onSettled = {
        settledCount.update { it + 1 }
        if (isLooping) {
          atEnd.update { !it }
        } else {
          animating.update(false)
        }
      }
      onCleanup { motion.onSettled = null }
    }
    // end_example

    val count = settledCount.value

    return Column(style = Style.padding(all = 16.dp)) {
      child(
          Text(
              text = "Tap the track to animate",
              textSize = 16.sp,
              textColor = Color.GRAY,
              style = Style.margin(bottom = 16.dp),
          ),
      )
      child(
          Column(
              justifyContent = YogaJustify.CENTER,
              style =
                  Style.width(TRACK_WIDTH)
                      .height(TRACK_HEIGHT)
                      .backgroundColor("#EEF1F5".toColorInt())
                      .onClick {
                        animating.update(true)
                        atEnd.update { !it }
                      },
          ) {
            child(
                Column(
                    style =
                        Style.width(BLOCK_SIZE)
                            .height(BLOCK_SIZE)
                            .backgroundColor("#EA4335".toColorInt())
                            .translationX(x)
                            .scale(scale),
                ),
            )
          },
      )
      child(
          Text(
              text = if (animating.value) "Animating…" else "Idle",
              textSize = 18.sp,
              textColor = "#333333".toColorInt(),
              style = Style.margin(top = 16.dp),
          ),
      )
      child(
          Text(
              text = "Settled $count ${if (count == 1) "time" else "times"}",
              textSize = 18.sp,
              textColor = "#333333".toColorInt(),
              style = Style.margin(vertical = 8.dp),
          ),
      )
      child(
          Text(
              text = if (isLooping) "Loop: on" else "Loop: off",
              textSize = 16.sp,
              textColor = Color.WHITE,
              style =
                  Style.alignSelf(YogaAlign.FLEX_START)
                      .backgroundColor(if (isLooping) "#34A853".toColorInt() else Color.GRAY)
                      .padding(horizontal = 16.dp, vertical = 8.dp)
                      .onClick {
                        val enableLoop = !isLooping
                        looping.update(enableLoop)
                        if (enableLoop && !animating.value) {
                          animating.update(true)
                          atEnd.update { !it }
                        }
                      },
          ),
      )
    }
  }
}

private val MOVE_DURATION = 600.ms
private val TRACK_WIDTH = 320.dp
private val TRACK_HEIGHT = 120.dp
private val BLOCK_SIZE = 56.dp
