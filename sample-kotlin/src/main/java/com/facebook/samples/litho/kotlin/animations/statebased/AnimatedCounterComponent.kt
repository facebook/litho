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
import android.graphics.Typeface
import androidx.core.graphics.toColorInt
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.animation.DampingRatio
import com.facebook.litho.animation.Stiffness
import com.facebook.litho.animation.spring
import com.facebook.litho.animation.translationY
import com.facebook.litho.animation.useAnimatedFloat
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
import com.facebook.rendercore.sp
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify

/**
 * Counter with spring-animated translation.
 *
 * Demonstrates [useAnimatedFloat] reacting to arbitrary state changes (not just boolean toggles).
 * The count text bounces with a spring whenever the count changes.
 */
@OptIn(ExperimentalLithoApi::class)
class AnimatedCounterComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val count = useState { 0 }
    val translationY = useAnimatedFloat(
        count.value * 40f,
        spring(stiffness = Stiffness.Low, dampingRatio = DampingRatio.LowBouncy),
    )

    return Column(style = Style.padding(all = 16.dp)) {
      child(
          Text(
              text = "Count: ${count.value}",
              textSize = 48.sp,
              textColor = "#333333".toColorInt(),
              typeface = Typeface.DEFAULT_BOLD,
              style = Style.translationY(translationY).margin(bottom = 24.dp),
          ),
      )
      child(
          Row(alignItems = YogaAlign.CENTER, justifyContent = YogaJustify.CENTER) {
            child(
                Column(
                    alignItems = YogaAlign.CENTER,
                    justifyContent = YogaJustify.CENTER,
                    style =
                        Style.width(64.dp)
                            .height(64.dp)
                            .margin(end = 16.dp)
                            .backgroundColor("#EA4335".toColorInt())
                            .onClick { count.update { it - 1 } },
                ) {
                  child(
                      Text(
                          text = "−",
                          textSize = 24.sp,
                          textColor = Color.WHITE,
                          typeface = Typeface.DEFAULT_BOLD,
                      ),
                  )
                },
            )
            child(
                Column(
                    alignItems = YogaAlign.CENTER,
                    justifyContent = YogaJustify.CENTER,
                    style =
                        Style.width(64.dp)
                            .height(64.dp)
                            .backgroundColor("#34A853".toColorInt())
                            .onClick { count.update { it + 1 } },
                ) {
                  child(
                      Text(
                          text = "+",
                          textSize = 24.sp,
                          textColor = Color.WHITE,
                          typeface = Typeface.DEFAULT_BOLD,
                      ),
                  )
                },
            )
          },
      )
    }
  }
}
