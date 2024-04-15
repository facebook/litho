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
import com.facebook.litho.useCached
import com.facebook.litho.widget.canvas.CanvasComponent
import com.facebook.litho.widget.canvas.Gradient
import com.facebook.litho.widget.canvas.Path
import com.facebook.litho.widget.canvas.Shading
import com.facebook.litho.widget.canvas.Shape
import com.facebook.litho.widget.canvas.Transform
import com.facebook.primitive.utils.types.Point
import com.facebook.rendercore.px

// start_example
class DrawPathCanvasKComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    // Using useCached to avoid recreating Path object on state updates
    val heartPath = useCached {
      // A heart shape, the path data taken from some random svg found online
      Path {
        add(
            Path {
              moveTo(Point(75f, 40f))
              cubicTo(Point(75f, 37f), Point(70f, 25f), Point(50f, 25f))
              cubicTo(Point(20f, 25f), Point(20f, 62.5f), Point(20f, 62.5f))
              cubicTo(Point(20f, 80f), Point(40f, 102f), Point(75f, 120f))
              cubicTo(Point(110f, 102f), Point(130f, 80f), Point(130f, 62.5f))
              cubicTo(Point(130f, 62.5f), Point(130f, 25f), Point(100f, 25f))
              cubicTo(Point(85f, 25f), Point(75f, 37f), Point(75f, 40f))
            },
            // the heart path starts at 20,25 so translate it to make it start at 0,0 in order to
            // make
            // positioning easier
            Transform { translate(dx = -20f, dy = -25f) })
      }
    }

    return Column {
      child(
          CanvasComponent(style = Style.widthPercent(100f).height(100.px)) {
            // left heart
            fill(shape = Shape.path(heartPath), shading = Shading.solidColor(Color.RED))
            // right heart translated using a group
            group(transform = Transform { translate(dx = 120f) }) {
              fill(
                  shape = Shape.path(heartPath),
                  shading =
                      Shading.linearGradient(
                          gradient = Gradient(Color.RED, Color.GREEN, Color.BLUE),
                          startPoint = Point.Zero,
                          endPoint = Point(110f, 0f)))
            }
          })
    }
  }
}
// end_example
