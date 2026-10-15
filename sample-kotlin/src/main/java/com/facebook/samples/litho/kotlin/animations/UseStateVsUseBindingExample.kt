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

package com.facebook.samples.litho.kotlin.animations

import android.animation.ValueAnimator
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.animated.alpha
import com.facebook.litho.animated.useBinding
import com.facebook.litho.core.height
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.onCleanup
import com.facebook.litho.useEffect
import com.facebook.litho.useRef
import com.facebook.litho.view.backgroundColor
import com.facebook.rendercore.dp
import com.facebook.yoga.YogaAlign

/**
 * Demonstrates the correct pattern for animating values in Litho using useBinding + useRef.
 *
 * ## Anti-pattern (DO NOT DO THIS):
 * Using useState in a ValueAnimator.addUpdateListener causes a re-render on EVERY animation frame
 * (60 times per second). This is extremely expensive and will cause jank:
 * ```kotlin
 * // ❌ WRONG - causes 60 re-renders per second!
 * var alpha by useState { 1f }
 * val animator = ValueAnimator.ofFloat(1f, 0f).apply {
 *     addUpdateListener { alpha = it.animatedValue as Float }  // Triggers re-render every frame!
 * }
 * ```
 *
 * ## Correct pattern:
 * useBinding provides a DynamicValue that can be updated without triggering re-renders. useRef
 * stores the animator instance across renders without causing re-renders when set. useEffect with
 * onCleanup ensures proper lifecycle management - the animator is created on the main thread and
 * cleaned up when the component unmounts.
 */
class UseStateVsUseBindingExample : KComponent() {
  override fun ComponentScope.render(): Component {
    val alphaBinding = useBinding(1f)
    val animatorRef = useRef<ValueAnimator?> { null }

    useEffect(Unit) {
      animatorRef.value =
          ValueAnimator.ofFloat(1f, 0.3f).apply {
            duration = 2000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation -> alphaBinding.set(animation.animatedValue as Float) }
          }
      animatorRef.value?.start()
      onCleanup { animatorRef.value?.cancel() }
    }

    return Column(alignItems = YogaAlign.CENTER, style = Style.padding(all = 20.dp)) {
      child(
          Text(
              style =
                  Style.width(200.dp)
                      .height(100.dp)
                      .backgroundColor(0xFF6200EE.toInt())
                      .alpha(alphaBinding),
              text = "I fade in and out smoothly without re-rendering!",
          ),
      )
    }
  }
}
