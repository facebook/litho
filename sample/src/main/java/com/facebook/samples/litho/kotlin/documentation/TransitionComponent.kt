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

package com.facebook.samples.litho.kotlin.documentation

import android.graphics.Color
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.Transition
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.dp
import com.facebook.litho.transition.transitionKey
import com.facebook.litho.transition.useTransition
import com.facebook.litho.useState
import com.facebook.litho.view.alpha
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.onClick

// start_example
class TransitionComponent : KComponent() {
  override fun ComponentScope.render(): Component? {
    val isHalfAlpha = useState { false }
    useTransition(Transition.create("square").animate(AnimatedProperties.ALPHA))

    return Column(style = Style.onClick { isHalfAlpha.update(!isHalfAlpha.value) }) {
      child(
          Row(
              style =
                  Style.backgroundColor(Color.YELLOW)
                      .width(80.dp)
                      .height(80.dp)
                      .alpha(
                          if (isHalfAlpha.value) {
                            0.5f
                          } else {
                            1.0f
                          })
                      .transitionKey(context, "square")))
    }
  }
}
// end_example
