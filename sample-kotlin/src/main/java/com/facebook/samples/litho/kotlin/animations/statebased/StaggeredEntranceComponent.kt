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
import com.facebook.litho.animation.CubicBezierEasing
import com.facebook.litho.animation.FiniteAnimationSpec
import com.facebook.litho.animation.Segment
import com.facebook.litho.animation.alpha
import com.facebook.litho.animation.translationY
import com.facebook.litho.animation.tween
import com.facebook.litho.animation.useAnimationGroup
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
 * Staggered entrance with a completion callback, via [useAnimationGroup].
 *
 * - **Stagger**: each box's `tween` starts [STAGGER_MS] after the previous one. On exit the order
 *   reverses, chosen per direction from the `Segment` receiver.
 * - **Custom easing**: a [CubicBezierEasing] that decelerates hard into place.
 * - **Completion**: the group's `onSettled` updates the status line once every box has settled.
 */
@OptIn(ExperimentalLithoApi::class)
class StaggeredEntranceComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val shown = useState { false }
    val settled = useState { true }
    val entrance =
        useAnimationGroup(shown.value, label = "entrance", onSettled = { settled.update(true) })
    val offsetPx = 24.dp.toPixels().toFloat()

    val boxes =
        BOX_COLORS.indices.map { index ->
          val spec: Segment<Boolean>.() -> FiniteAnimationSpec<Float> = {
            val order = if (false.isTransitioningTo(true)) index else BOX_COLORS.lastIndex - index
            tween(DURATION_MS.ms, easing = ENTRANCE_EASING, delay = (order * STAGGER_MS).ms)
          }
          val alpha = entrance.animateFloat(spec, label = "alpha$index") { if (it) 1f else 0f }
          val offset =
              entrance.animateFloat(spec, label = "offset$index") { if (it) 0f else offsetPx }
          alpha to offset
        }

    return Column(style = Style.padding(all = 16.dp)) {
      child(
          Text(
              text = if (shown.value) "Hide" else "Show",
              textSize = 18.sp,
              textColor = "#1A73E8".toColorInt(),
              style =
                  Style.padding(vertical = 8.dp).onClick {
                    shown.update { !it }
                    settled.update(false)
                  },
          ),
      )
      child(
          Row(style = Style.margin(vertical = 16.dp)) {
            boxes.forEachIndexed { index, (alpha, offset) ->
              child(
                  Column(
                      style =
                          Style.width(64.dp)
                              .height(64.dp)
                              .margin(end = 12.dp)
                              .backgroundColor(BOX_COLORS[index])
                              .alpha(alpha)
                              .translationY(offset),
                  ),
              )
            }
          },
      )
      child(
          Text(
              text = if (settled.value) "Settled" else "Animating…",
              textSize = 14.sp,
              textColor = Color.GRAY,
          ),
      )
    }
  }

  private companion object {
    const val DURATION_MS = 400
    const val STAGGER_MS = 120
    val ENTRANCE_EASING = CubicBezierEasing(0.2f, 0.9f, 0.1f, 1f)
    val BOX_COLORS = listOf("#EA4335".toColorInt(), "#FBBC04".toColorInt(), "#34A853".toColorInt())
  }
}
