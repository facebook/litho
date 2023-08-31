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

import android.graphics.Color
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.widthPercent
import com.facebook.litho.widget.canvas.CanvasComponent
import com.facebook.litho.widget.canvas.Gradient
import com.facebook.litho.widget.canvas.Shading
import com.facebook.litho.widget.canvas.Shape
import com.facebook.litho.widget.canvas.Transform
import com.facebook.primitive.utils.types.BlendingMode
import com.facebook.primitive.utils.types.Point
import com.facebook.rendercore.px

// start_example
class DrawTransparentHoleCanvasKComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    return Column {
      child(
          CanvasComponent(style = Style.widthPercent(100f).height(100.px)) {
            // gradient background
            fill(
                // this.size returns the size of the current drawing scope, in this case it'll be
                // the size of the canvas
                shape = Shape.rect(topLeft = Point.Zero, size = size),
                shading =
                    Shading.linearGradient(
                        gradient = Gradient(Color.RED, Color.GREEN, Color.BLUE),
                        startPoint = Point.Zero,
                        endPoint = Point(size.width, 0f)))
            layer(
                transform =
                    Transform { translate(dx = size.width * 0.1f, dy = size.height * 0.1f) },
                size = size * 0.8f // 80% of the canvas size
                ) {
                  // layer background
                  fill(
                      shape = Shape.rect(topLeft = Point.Zero, size = size),
                      shading = Shading.solidColor(Color.CYAN))
                  // ellipse with xor blending mode
                  fill(
                      shape =
                          Shape.ellipse(
                              topLeft = Point(size.width * 0.1f, size.height * 0.1f),
                              size = size * 0.8f),
                      shading = Shading.solidColor(Color.BLACK),
                      blendingMode = BlendingMode.Xor)
                }
          })
    }
  }
}
// end_example
