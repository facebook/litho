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

package com.facebook.samples.litho.kotlin.animations.animatedcounter

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.animated.Animated
import com.facebook.litho.animated.AnimatedAnimation
import com.facebook.litho.animated.translationY
import com.facebook.litho.animated.useBinding
import com.facebook.litho.core.padding
import com.facebook.litho.dp
import com.facebook.litho.flexbox.alignSelf
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.onCleanup
import com.facebook.litho.sp
import com.facebook.litho.useEffect
import com.facebook.litho.useRef
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify

class AnimatingCounterRootComponent : KComponent() {
  override fun ComponentScope.render(): Component? {
    val count = useState { 0 }
    return Column(alignContent = YogaAlign.CENTER) {
      child(
          Row(
              style = Style.alignSelf(YogaAlign.CENTER).flex(grow = 1f),
              justifyContent = YogaJustify.CENTER,
              alignItems = YogaAlign.CENTER) {
            child(
                Text(
                    style = Style.padding(all = 8.dp).onClick { count.update { c -> c + 1 } },
                    text = "+"))
            child(AnimatingCounter(count = count.value))
            child(
                Text(
                    style = Style.padding(all = 8.dp).onClick { count.update { c -> c - 1 } },
                    text = "-"))
          })
    }
  }
}

// start_example
class AnimatingCounter(private val count: Int) : KComponent() {
  override fun ComponentScope.render(): Component? {
    val animation = useRef<AnimatedAnimation?> { null }
    val translationY = useBinding(0f)

    useEffect(count) {
      // Animate the text to a Y-offset based on count
      val newAnimation = Animated.spring(translationY, to = count * 10.dp.toPixels().toFloat())
      newAnimation.start()
      animation.value = newAnimation

      onCleanup { animation.value?.cancel() }
    }

    return Text(style = Style.translationY(translationY), text = "$count", textSize = 24.sp)
  }
}
// end_example
