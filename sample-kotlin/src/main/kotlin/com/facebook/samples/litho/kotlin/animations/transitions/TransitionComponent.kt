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
import android.util.Log
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.animated.Animated
import com.facebook.litho.animated.SpringConfig
import com.facebook.litho.animated.backgroundColor
import com.facebook.litho.animated.translationX
import com.facebook.litho.animated.useBinding
import com.facebook.litho.dp
import com.facebook.litho.flexbox.height
import com.facebook.litho.flexbox.margin
import com.facebook.litho.flexbox.width
import com.facebook.litho.useState

private const val TAG = "TransitionComponent"

class TransitionComponent : KComponent() {

  override fun ComponentScope.render(): Component? {
    val isColorTransitionComplete = useState { false }
    val isXTransitionComplete = useState { false }
    val xProgress = useBinding(500f)
    val x1Progress = useBinding(500f)
    val x2Progress = useBinding(500f)
    val colorProgress = useBinding(0f)
    val rotation = useBinding(0f)
    val bgColor =
        useBinding(colorProgress) { progress ->
          Color.HSVToColor(floatArrayOf(100f * progress, 100f, 255f))
        }

    return Column(
        style =
            Style.onClick {
              Animated.parallel(
                      Animated.spring(
                          xProgress,
                          to = if (isXTransitionComplete.value) 100f else 50f,
                          SpringConfig(stiffness = 50f, dampingRatio = 0.2f)),
                      Animated.spring(
                          x1Progress,
                          to = if (isXTransitionComplete.value) 100f else 50f,
                          SpringConfig(stiffness = 50f, dampingRatio = 0.2f)),
                      Animated.spring(
                          x2Progress,
                          to = if (isXTransitionComplete.value) 100f else 50f,
                          SpringConfig(stiffness = 50f, dampingRatio = 0.2f)),
                      Animated.timing(
                          target = colorProgress,
                          to = if (isColorTransitionComplete.value) 0f else 1f,
                          duration = 1000,
                          onUpdate = { Log.d(TAG, "onUpdate: $it") },
                          onFinish = {
                            isColorTransitionComplete.update(!isColorTransitionComplete.value)
                          }))
                  .start()
            },
        children =
            listOf(
                Row(
                    style =
                        Style.width(100.dp).height(100.dp).backgroundColor(bgColor).onClick {
                          Animated.timing(
                                  target = colorProgress,
                                  to = if (isColorTransitionComplete.value) 0f else 1f,
                                  duration = 1000,
                                  onUpdate = { Log.d(TAG, "onUpdate: $it") },
                                  onFinish = {
                                    isColorTransitionComplete.update(
                                        !isColorTransitionComplete.value)
                                  })
                              .start()
                        }),
                Row(
                    style =
                        Style.width(20.dp)
                            .height(20.dp)
                            .margin(all = 20.dp)
                            .translationX(xProgress)
                            .backgroundColor(bgColor)),
                Row(
                    style =
                        Style.width(20.dp)
                            .height(20.dp)
                            .margin(all = 20.dp)
                            .translationX(x1Progress)
                            .backgroundColor(bgColor)),
                Row(
                    style =
                        Style.width(20.dp)
                            .height(20.dp)
                            .margin(all = 20.dp)
                            .translationX(x2Progress)
                            .backgroundColor(bgColor)),
            ))
  }
}
