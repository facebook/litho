/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.animated.Animated
import com.facebook.litho.animated.backgroundColor
import com.facebook.litho.animated.translationX
import com.facebook.litho.animated.useBinding
import com.facebook.litho.dp
import com.facebook.litho.flexbox.height
import com.facebook.litho.flexbox.margin
import com.facebook.litho.flexbox.width
import com.facebook.litho.view.onClick

private const val TAG = "TransitionComponent"

class TransitionComponent : KComponent() {

  override fun ComponentScope.render(): Component? {
    val xProgress = useBinding(-2f)
    val colorProgress = useBinding(0f)
    val xValue = useBinding(xProgress) { progress -> progress * progress * 100 }
    val bgColor =
        useBinding(colorProgress) { progress ->
          Color.HSVToColor(floatArrayOf(100f * progress, 100f, 255f))
        }

    return Column(
        style =
            Style.onClick {
              Animated.loop(
                      Animated.sequence(
                          Animated.timing(target = xProgress, to = 2f, duration = 3000),
                          Animated.timing(target = xProgress, to = 2f, duration = 3000)),
                      2)
                  .start()
              Animated.loop(
                      Animated.parallel(
                          Animated.timing(target = colorProgress, to = 2f, duration = 3000),
                          Animated.timing(target = colorProgress, to = 2f, duration = 3000)),
                      2)
                  .start()
            },
        children =
            listOf(
                Row(
                    style =
                        Style.width(20.dp)
                            .height(20.dp)
                            .margin(all = 20.dp)
                            .translationX(xValue)
                            .backgroundColor(bgColor)),
            ))
  }
}
