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
import com.facebook.litho.animation.Segment
import com.facebook.litho.animation.Stiffness
import com.facebook.litho.animation.alpha
import com.facebook.litho.animation.height
import com.facebook.litho.animation.rotation
import com.facebook.litho.animation.spring
import com.facebook.litho.animation.tween
import com.facebook.litho.animation.useAnimationGroup
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.useState
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp
import com.facebook.rendercore.ms
import com.facebook.rendercore.sp
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify

/**
 * Expandable card with coordinated animations via [useAnimationGroup].
 *
 * This is the signature demo for the state-based animation system. Three coordinated animations:
 * - **Height**: bouncy spring for expand, quick tween for collapse (directional via [Segment])
 * - **Content alpha**: fade in/out with different timings per direction
 * - **Chevron rotation**: 0° ↔ 180° with default spring
 */
@OptIn(ExperimentalLithoApi::class)
class ExpandableCardComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val expanded = useState { false }
    val transition = useAnimationGroup(expanded.value, label = "expand")

    val height =
        transition.animateDimen(
            label = "height",
            transitionSpec = {
              when {
                false.isTransitioningTo(true) ->
                    spring(stiffness = Stiffness.Low, dampingRatio = DampingRatio.LowBouncy)
                else -> tween(250.ms, easing = Easing.FastOutSlowIn)
              }
            },
        ) { state ->
          if (state) 200.dp else 56.dp
        }

    val contentAlpha =
        transition.animateFloat(
            label = "contentAlpha",
            transitionSpec = {
              when {
                false.isTransitioningTo(true) -> tween(200.ms, easing = Easing.EaseIn)
                else -> tween(150.ms, easing = Easing.EaseOut)
              }
            },
        ) { state ->
          if (state) 1f else 0f
        }

    val chevronRotation =
        transition.animateFloat(
            label = "chevronRotation",
        ) { state ->
          if (state) 180f else 0f
        }

    return Column(
        style =
            Style.height(height).backgroundColor(Color.WHITE).padding(horizontal = 16.dp).onClick {
              expanded.update { !it }
            },
    ) {
      child(
          Row(
              alignItems = YogaAlign.CENTER,
              justifyContent = YogaJustify.SPACE_BETWEEN,
              style = Style.height(56.dp),
          ) {
            child(
                Text(
                    text = "Expandable Card",
                    textSize = 18.sp,
                    textColor = "#333333".toColorInt(),
                    style = Style.flex(grow = 1f),
                ),
            )
            child(
                Text(
                    text = "▼",
                    textSize = 16.sp,
                    textColor = Color.GRAY,
                    style = Style.width(24.dp).height(24.dp).rotation(chevronRotation),
                ),
            )
          },
      )
      child(
          Text(
              text =
                  "This content animates in with a fade when expanding " +
                      "and fades out when collapsing. The height uses a bouncy spring " +
                      "for expand and a quick tween for collapse.",
              textSize = 14.sp,
              textColor = "#666666".toColorInt(),
              style = Style.alpha(contentAlpha).margin(top = 8.dp),
          ),
      )
    }
  }
}
