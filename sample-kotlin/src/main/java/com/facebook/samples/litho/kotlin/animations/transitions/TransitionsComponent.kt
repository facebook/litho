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
import com.facebook.litho.animation.spring
import com.facebook.litho.animation.transition
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.alignSelf
import com.facebook.litho.kotlin.widget.VerticalScroll
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.useState
import com.facebook.litho.view.alpha
import com.facebook.litho.view.background
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp
import com.facebook.samples.litho.kotlin.drawable.RoundedRect
import com.facebook.yoga.YogaAlign

/**
 * Animates layout changes with [Style.transition]: one box slides across with `x`, and another
 * grows with `size(spec, reflow = true)`, so the components below follow its size every frame. Tap
 * to toggle both. Below it, [ContainersComponent] shows enter, exit and content swaps,
 * [TriStateComponent] picks a spec from the direction of a change, and [DeltaDemoComponent]
 * animates a scale.
 */
@OptIn(ExperimentalLithoApi::class)
class TransitionsComponent : KComponent() {

  override fun ComponentScope.render(): Component? {
    val alphaValue = useState { 1f }
    val shouldExpand = useState { false }
    val toRight = useState { true }
    return VerticalScroll {
      Column(
          style =
              Style.width(200.dp).alpha(alphaValue.value).onClick {
                toRight.update(!toRight.value)
                alphaValue.update { prevValue -> if (prevValue == 1f) 0.5f else 1f }
                shouldExpand.update(!shouldExpand.value)
              },
      ) {
        child(
            Column(
                style =
                    Style.width(50.dp)
                        .height(50.dp)
                        .margin(all = 5.dp)
                        .alignSelf(if (toRight.value) YogaAlign.FLEX_END else YogaAlign.FLEX_START)
                        .background(RoundedRect(0xff666699, 8.dp))
                        .transition { x = spring() },
            ),
        )
        child(
            Column(
                style =
                    Style.width(if (shouldExpand.value) 75.dp else 50.dp)
                        .height(if (shouldExpand.value) 75.dp else 50.dp)
                        .margin(all = 5.dp)
                        .background(RoundedRect(0xffba7bb5, 8.dp))
                        .transition { size(spring(), reflow = true) },
            ),
        )
        child(ContainersComponent())
        child(TriStateComponent())
        child(DeltaDemoComponent())
      }
    }
  }
}
