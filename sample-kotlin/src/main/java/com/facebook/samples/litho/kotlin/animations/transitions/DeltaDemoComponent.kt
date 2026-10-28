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

package com.facebook.samples.litho.kotlin.animations.transitions

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.animation.DampingRatio
import com.facebook.litho.animation.spring
import com.facebook.litho.animation.transition
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.alignSelf
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useState
import com.facebook.litho.view.background
import com.facebook.litho.view.onClick
import com.facebook.litho.view.scale
import com.facebook.litho.widget.TextAlignment
import com.facebook.litho.widget.VerticalGravity
import com.facebook.rendercore.dp
import com.facebook.samples.litho.kotlin.drawable.RoundedRect
import com.facebook.yoga.YogaAlign
import kotlin.math.absoluteValue
import kotlin.random.Random

/**
 * Scales a bubble with [Style.transition] and shows how much its number changed. The click handler
 * computes the change from the previous number, so nothing needs the previous render.
 */
@OptIn(ExperimentalLithoApi::class)
class DeltaDemoComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    val number = useState { 0 }
    val delta = useState { 0 }
    val text = buildString {
      val d = delta.value
      if (d == 0) append("Tap me") else append(if (d > 0) "+" else "-").append(d.absoluteValue)
    }
    return Text(
        text,
        alignment = TextAlignment.CENTER,
        verticalGravity = VerticalGravity.CENTER,
        style =
            Style.width(100.dp)
                .height(100.dp)
                .alignSelf(YogaAlign.CENTER)
                .margin(all = 10.dp)
                .scale(lerp((number.value - MIN) / (MAX - MIN).toFloat(), 0.5f, 1.5f))
                .background(RoundedRect(0xff6ab071, 8.dp))
                .transition { scale = spring(dampingRatio = DampingRatio.HighBouncy) }
                .onClick {
                  val next = Random.nextInt(MIN, MAX)
                  delta.update(next - number.value)
                  number.update(next)
                },
    )
  }

  private fun lerp(fraction: Float, from: Float, to: Float): Float = from + fraction * (to - from)

  private companion object {
    const val MIN = -999
    const val MAX = +999
  }
}
