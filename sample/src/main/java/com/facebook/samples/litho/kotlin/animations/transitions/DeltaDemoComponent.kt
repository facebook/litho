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
import com.facebook.litho.Transition
import com.facebook.litho.animated.useBinding
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.alignSelf
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.transition.transitionKey
import com.facebook.litho.transition.useTransition
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

class DeltaDemoComponent : KComponent() {
  companion object {
    private const val MIN = -999
    private const val MAX = +999
  }

  @OptIn(ExperimentalLithoApi::class)
  // start_example
  override fun ComponentScope.render(): Component {
    val number = useState { 0 }
    val delta = useBinding(0)
    useTransition(number.value) {
      val diff = diffOf(number.value)
      val previous = diff.previous
      val next = diff.next
      val d = if (previous == null || next == null) next ?: 0 else next - previous
      delta.set(d)
      Transition.create(Transition.TransitionKeyType.GLOBAL, "bubble")
          .animate(AnimatedProperties.SCALE)
          .animator(Transition.springWithConfig(250.0, 10.0))
    }
    val text = buildString {
      val d = delta.get()
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
                .transitionKey(context, "bubble", Transition.TransitionKeyType.GLOBAL)
                .margin(all = 10.dp)
                .scale(lerp((number.value - MIN) / (MAX - MIN).toFloat(), 0.5f, 1.5f))
                .background(RoundedRect(0xff6ab071, 8.dp))
                .onClick { number.update(Random.nextInt(MIN, MAX)) })
  }

  // end_example

  private fun lerp(fraction: Float, from: Float, to: Float): Float = from + fraction * (to - from)
}
