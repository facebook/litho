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
import com.facebook.litho.animation.AnimatedContent
import com.facebook.litho.animation.fadeIn
import com.facebook.litho.animation.fadeOut
import com.facebook.litho.animation.slideInHorizontally
import com.facebook.litho.animation.slideOutHorizontally
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
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

/**
 * Keyed content-swap demo: step a counter up or down and the new value cross-fades and slides in
 * while the previous value slides out, with direction following the step.
 *
 * Demonstrates [AnimatedContent] with a direction-aware `transitionSpec`. The container retains the
 * outgoing content until its exit finishes, and sizes itself to the target content.
 */
@OptIn(ExperimentalLithoApi::class)
class AnimatedContentCounterComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val count = useState { 0 }

    return Column(style = Style.padding(all = 16.dp)) {
      child(
          Row {
            child(
                Text(
                    text = "−",
                    textSize = 28.sp,
                    style = Style.padding(all = 12.dp).onClick { count.update { it - 1 } },
                ),
            )
            child(
                Text(
                    text = "+",
                    textSize = 28.sp,
                    style = Style.padding(all = 12.dp).onClick { count.update { it + 1 } },
                ),
            )
          },
      )
      child(
          AnimatedContent(
              targetState = count.value,
              transitionSpec = {
                if (targetState >= initialState) {
                  enter = fadeIn() + slideInHorizontally { it }
                  exit = fadeOut() + slideOutHorizontally { -it }
                } else {
                  enter = fadeIn() + slideInHorizontally { -it }
                  exit = fadeOut() + slideOutHorizontally { it }
                }
              },
          ) { value ->
            Column(
                style =
                    Style.height(120.dp)
                        .width(200.dp)
                        .backgroundColor("#4285F4".toColorInt())
                        .padding(all = 16.dp),
            ) {
              child(Text(text = value.toString(), textSize = 48.sp, textColor = Color.WHITE))
            }
          },
      )
    }
  }
}
