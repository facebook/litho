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

import android.graphics.Color
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.animation.DampingRatio
import com.facebook.litho.animation.FiniteAnimationSpec
import com.facebook.litho.animation.snap
import com.facebook.litho.animation.spring
import com.facebook.litho.animation.transition
import com.facebook.litho.animation.tween
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.width
import com.facebook.litho.core.widthPercent
import com.facebook.litho.flexbox.alignSelf
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.useState
import com.facebook.litho.view.alpha
import com.facebook.litho.view.background
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp
import com.facebook.rendercore.ms
import com.facebook.samples.litho.kotlin.drawable.RoundedRect
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify

/**
 * Picks a [Style.transition] spec from the direction of a state change. The click handler records
 * the state being left, so the spec can depend on both the previous and the next state.
 */
@OptIn(ExperimentalLithoApi::class)
// start_example
class TriStateComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    val change = useState { TriState.HEIGHT to TriState.HEIGHT }
    val (previous, current) = change.value
    return Column {
      child(
          Row(
              style = Style.widthPercent(100f).margin(top = 10.dp),
              justifyContent = YogaJustify.SPACE_BETWEEN,
          ) {
            for (entry in TriState.entries) {
              child(
                  Column(
                      style =
                          Style.width(50.dp)
                              .height(50.dp)
                              .margin(all = 5.dp)
                              .alpha(if (current == entry) 1f else 0.3f)
                              .background(RoundedRect(entry.color, 8.dp))
                              .onClick {
                                if (entry != current) change.update(current to entry)
                              },
                  ),
              )
            }
          },
      )
      child(
          Column(
              style =
                  Style.width(if (current == TriState.WIDTH) 150.dp else 50.dp)
                      .height(if (current == TriState.HEIGHT) 150.dp else 50.dp)
                      .alignSelf(
                          if (current == TriState.ALIGN) YogaAlign.FLEX_END
                          else YogaAlign.FLEX_START,
                      )
                      .margin(all = 5.dp)
                      .background(RoundedRect(Color.RED, 8.dp))
                      .transition { bounds(specFor(previous, current), reflow = true) },
          ),
      )
    }
  }
}

private fun specFor(previous: TriState, current: TriState): FiniteAnimationSpec<Float> =
    when (setOf(previous, current)) {
      setOf(TriState.HEIGHT, TriState.WIDTH) -> spring()
      setOf(TriState.HEIGHT, TriState.ALIGN) -> tween(1_000.ms)
      setOf(TriState.WIDTH, TriState.ALIGN) -> spring(dampingRatio = DampingRatio.HighBouncy)
      else -> snap()
    }

// end_example

private enum class TriState(val color: Long) {
  HEIGHT(0xffbf678d),
  WIDTH(0xff6ab071),
  ALIGN(0xff678db0),
}
