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
import com.facebook.primitive.utils.types.Point
import com.facebook.rendercore.px

// start_example
class DrawShapeCanvasKComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    return Column {
      child(
          CanvasComponent(style = Style.widthPercent(100f).height(100.px)) {
            // left circle
            fill(
                shape = Shape.circle(center = Point(40f, 40f), radius = 40f),
                shading = Shading.solidColor(Color.RED))
            // right circle
            fill(
                shape = Shape.circle(center = Point(140f, 40f), radius = 40f),
                shading =
                    Shading.linearGradient(
                        gradient = Gradient(Color.RED, Color.GREEN, Color.BLUE),
                        startPoint = Point.Zero,
                        endPoint = Point(180f, 0f)))
          })
    }
  }
}
// end_example
