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

package com.facebook.samples.litho.kotlin.canvas

import android.animation.ValueAnimator
import android.graphics.Color
import android.view.animation.LinearInterpolator
import com.facebook.litho.ClickEvent
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.widthPercent
import com.facebook.litho.useRef
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.litho.widget.canvas.CanvasComponent
import com.facebook.litho.widget.canvas.Shading
import com.facebook.litho.widget.canvas.Shape
import com.facebook.litho.widget.canvas.Transform
import com.facebook.primitive.utils.types.Point
import com.facebook.primitive.utils.types.Size
import com.facebook.primitive.utils.types.center
import com.facebook.rendercore.px

// start_example
class DrawAnimatedSquareCanvasKComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val rotation = useState { 0f }
    val animator = useRef<ValueAnimator?> { null }

    val startAnimator: (ClickEvent) -> Unit = {
      animator.value?.cancel()
      animator.value =
          ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 2000
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { rotation.update(it.animatedValue as Float) }
          }
      animator.value?.start()
    }

    return Column {
      child(
          CanvasComponent(
              style = Style.widthPercent(100f).height(100.px).onClick(action = startAnimator)) {
                group(
                    transform =
                        Transform {
                          translate(dx = size.center.x, dy = size.center.y)
                          rotate(rotation.value, pivot = size.center)
                        }) {
                      // draw square
                      fill(
                          shape = Shape.rect(topLeft = Point(-30f, -30f), size = Size(60f, 60f)),
                          shading = Shading.solidColor(Color.RED))
                    }
              })
    }
  }
}
// end_example
