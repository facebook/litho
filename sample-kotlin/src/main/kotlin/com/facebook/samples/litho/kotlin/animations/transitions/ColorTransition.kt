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
import com.facebook.litho.animated.backgroundColor
import com.facebook.litho.animated.useBinding
import com.facebook.litho.dp
import com.facebook.litho.flexbox.height
import com.facebook.litho.flexbox.padding
import com.facebook.litho.flexbox.width
import com.facebook.litho.useState

private const val TAG = "ColorTransition"

class ColorTransition : KComponent() {

  override fun ComponentScope.render(): Component? {
    val isComplete = useState { false }
    val colorProgress = useBinding(0f)
    val bgColor =
        useBinding(colorProgress) { progress ->
          Color.HSVToColor(floatArrayOf(100f * progress, 100f, 255f))
        }
    return Column(
        style =
            Style.padding(all = 20.dp).onClick {
              Animated.timing(
                      target = colorProgress,
                      to = if (isComplete.value) 0f else 1f,
                      duration = 1000,
                      onUpdate = { Log.d(TAG, "onUpdate: $it") },
                      onFinish = { isComplete.update(!isComplete.value) })
                  .start()
            },
        children =
            listOf(
                Row(style = Style.width(100.dp).height(100.dp).backgroundColor(bgColor)),
            ))
  }
}
