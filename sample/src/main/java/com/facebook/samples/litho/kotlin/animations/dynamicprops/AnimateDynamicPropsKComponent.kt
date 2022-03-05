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

package com.facebook.samples.litho.kotlin.animations.dynamicprops

import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import com.facebook.litho.ClickEvent
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.animated.useBinding
import com.facebook.litho.core.padding
import com.facebook.litho.dp
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useRef
import com.facebook.litho.view.onClick
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import java.util.concurrent.TimeUnit

// start_example
class AnimateDynamicPropsKComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    val time = useBinding(0L)
    val animator = useRef<ValueAnimator?> { null }

    val startAnimator: (ClickEvent) -> Unit = {
      animator.value?.cancel()
      animator.value =
          ValueAnimator.ofInt(0, TimeUnit.HOURS.toMillis(12).toInt()).apply {
            duration = 2000
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { time.set((it.animatedValue as Int).toLong()) }
          }
      animator.value?.start()
    }

    return Column(alignItems = YogaAlign.CENTER, style = Style.padding(all = 20.dp)) {
      child(Text("Click to Start Animation", style = Style.onClick(startAnimator)))
      child(
          ClockFace.create(context)
              .time(time)
              .widthDip(200f)
              .heightDip(200f)
              .marginDip(YogaEdge.TOP, 20f)
              .build())
    }
  }
}
// end_example
