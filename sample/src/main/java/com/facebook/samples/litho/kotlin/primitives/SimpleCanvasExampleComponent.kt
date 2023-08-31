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

package com.facebook.samples.litho.kotlin.primitives

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.graphics.Color
import android.view.animation.LinearInterpolator
import com.facebook.litho.ClickEvent
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.useCached
import com.facebook.litho.useRef
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.litho.widget.canvas.CanvasComponent
import com.facebook.litho.widget.canvas.CanvasScope
import com.facebook.litho.widget.canvas.Gradient
import com.facebook.litho.widget.canvas.Path
import com.facebook.litho.widget.canvas.Shading
import com.facebook.litho.widget.canvas.Shape
import com.facebook.litho.widget.canvas.Transform
import com.facebook.litho.widget.canvas.at
import com.facebook.primitive.utils.types.BlendingMode
import com.facebook.primitive.utils.types.Point
import com.facebook.primitive.utils.types.Size
import com.facebook.primitive.utils.types.center

class SimpleCanvasExampleComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    val dashPhase = useState { 0f }
    val ringsRotation = useState { 0f }
    val heartScale = useState { 1f }

    val animators = useRef<AnimatorSet?> { null }

    val startAnimatorOnClick: (ClickEvent) -> Unit = {
      animators.value?.cancel()
      animators.value = AnimatorSet()

      animators.value?.playTogether(
          ValueAnimator.ofFloat(0f, 40f).apply {
            duration = 600
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { dashPhase.update(it.animatedValue as Float) }
          },
          ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 2000
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { ringsRotation.update(it.animatedValue as Float) }
          },
          ValueAnimator.ofFloat(1f, 1.5f).apply {
            duration = 400
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { heartScale.update(it.animatedValue as Float) }
          })

      animators.value?.start()
    }

    val circleRadius = 40f
    val circlePath = useCached { Path { circle(Point(circleRadius, circleRadius), circleRadius) } }

    // create 3 rings shape
    val ringsSize = Size(130f, 130f)
    val ringsPath = useCached {
      Path {
        add(
            Path {
              add(circlePath)
              add(circlePath, Transform { translate(dx = 50f) })
              add(circlePath, Transform { translate(dx = 30f, dy = 50f) })
            },
            // translate to avoid stroke clipping
            Transform { translate(dx = 4f, dy = 4f) })
      }
    }

    // A heart shape, the path data taken from some random svg found online
    val heartSize = Size(110f, 95f)
    val heartPath = useCached {
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

    val squareSize = Size(80f, 80f)

    return Column {
      child(
          CanvasComponent(style = Style.onClick(action = startAnimatorOnClick)) {
            // fill 4 circles in 4 canvas corners
            drawTopLeftCircle(circleRadius)
            drawTopRightCircle(circleRadius)
            drawBottomLeftCircle(circleRadius)
            drawBottomRightCircle(circleRadius)

            // draw filled group clipped to round rect shape with a rotating, gradient stroked three
            // rings shape inside
            drawRotatingRingsInsideGrayShape(ringsPath, ringsSize, ringsRotation.value)

            //  draw rect with a "marching ants" effect https://en.wikipedia.org/wiki/Marching_ants
            drawMarchingAntsRect(dashPhase.value)

            // draw a circle shape filled with a radial gradient that will be used as a background
            // for a layer
            drawGradientCircle()

            // use a layer to draw a rectangle with an animated heart shaped hole on top of the
            // gradient circle
            drawLayerWithHeartShapedHole(heartPath, heartSize, heartScale.value)

            // draw two rotated intersected squares with multiply blending mode
            drawLeftRect(squareSize)
            drawRightRect(squareSize)
          })
    }
  }

  private fun CanvasScope.drawTopLeftCircle(circleRadius: Float) {
    fill(
        Shape.circle(Point(circleRadius, circleRadius), circleRadius),
        Shading.solidColor(Color.RED))
  }

  private fun CanvasScope.drawTopRightCircle(circleRadius: Float) {
    group(
        Transform {
          translate(dx = size.width)
          translate(dx = -circleRadius)
        }) {
          fill(Shape.circle(Point(0f, circleRadius), circleRadius), Shading.solidColor(Color.CYAN))
        }
  }

  private fun CanvasScope.drawBottomLeftCircle(circleRadius: Float) {
    group(Transform { translate(dy = size.height) }) {
      fill(
          Shape.circle(Point(circleRadius, -circleRadius), circleRadius),
          Shading.solidColor(Color.BLUE))
    }
  }

  private fun CanvasScope.drawBottomRightCircle(circleRadius: Float) {
    group(Transform { translate(dx = size.width, dy = size.height) }) {
      fill(
          Shape.circle(Point(-circleRadius, -circleRadius), circleRadius),
          Shading.solidColor(Color.GREEN))
    }
  }

  private fun CanvasScope.drawRotatingRingsInsideGrayShape(
      ringsPath: Path,
      ringsSize: Size,
      ringsRotation: Float
  ) {
    val roundRectSize = Size(size.width * 0.75f, size.height * 0.33f)
    val roundedRectPath = Path {
      rect(
          topLeft = Point.Zero,
          size = roundRectSize,
          // radius will be clipped to at most half of the width of shorter border
          cornerRadius = 5000f)
    }
    group(
        transform = Transform { translate(dx = size.width * 0.125f, dy = 100f) },
        size = roundRectSize,
        clip = roundedRectPath) {
          // fill rect that is the same size as current group, it'll be clipped to the group clip
          // path
          fill(Shape.rect(Point.Zero, size), Shading.solidColor(Color.GRAY))

          // stroke the rings using a linear gradient in the center of the current group
          drawRotatingRings(ringsPath, ringsSize, ringsRotation)
        }
  }

  private fun CanvasScope.drawRotatingRings(
      ringsPath: Path,
      ringsSize: Size,
      ringsRotation: Float
  ) {
    group(
        Transform {
          translate(
              dx = (center - ringsSize.center).x - 4f, dy = (center - ringsSize.center).y - 4f)
          rotate(ringsRotation, center)
        }) {
          stroke(
              Shape.path(ringsPath),
              Shading.linearGradient(
                  Gradient(Color.RED, Color.GREEN, Color.BLUE),
                  Point.Zero,
                  Point(ringsSize.width, ringsSize.height)),
              lineWidth = 4f,
              dashLengths = floatArrayOf(8f, 4f))
        }
  }

  private fun CanvasScope.drawMarchingAntsRect(dashPhase: Float) {
    val antsRectSize = Size(400f, 80f)
    group(
        transform =
            Transform {
              translate(
                  dx = (center - antsRectSize.center).x - 2f,
                  dy = (center - antsRectSize.center).y - 2f)
            }) {
          stroke(
              shape = Shape.rect(topLeft = Point(2f, 2f), size = antsRectSize),
              shading = Shading.solidColor(Color.BLACK),
              lineWidth = 2f,
              dashLengths = floatArrayOf(4f, 6f),
              dashPhase = -dashPhase)
        }
  }

  private fun CanvasScope.drawGradientCircle() {
    val bigCircleRadius = 330f
    val bigCircleSize = Size(bigCircleRadius * 2f, bigCircleRadius * 2f)
    group(
        transform =
            Transform {
              translate(
                  dx = center.x - bigCircleRadius,
                  dy = size.height * 0.83f - bigCircleRadius - 100f)
            },
        size = bigCircleSize) {
          fill(
              Shape.rect(topLeft = Point.Zero, size = size, cornerRadius = bigCircleRadius),
              Shading.radialGradient(
                  gradient =
                      Gradient(Color.CYAN at 0f, Color.MAGENTA at 0.5f, Color.YELLOW at 1.0f),
                  center = center,
                  radius = bigCircleRadius))
        }
  }

  private fun CanvasScope.drawLayerWithHeartShapedHole(
      heartPath: Path,
      heartSize: Size,
      heartScale: Float
  ) {
    val layerSize = heartSize * 2f
    layer(
        Transform { translate(dx = center.x - heartSize.width, dy = size.height * 0.66f - 100f) },
        size = layerSize) {
          // fill rect that is the same size as the layer, it'll be clipped to the layer
          // bounds
          fill(Shape.rect(Point.Zero, size), Shading.solidColor(Color.GREEN))

          // cut the hole in the middle of the layer using heart shape
          drawAnimatedHeartShapedHole(heartPath, heartSize, heartScale)
        }
  }

  private fun CanvasScope.drawAnimatedHeartShapedHole(
      heartPath: Path,
      heartSize: Size,
      heartScale: Float
  ) {
    group(
        Transform {
          scale(heartScale, heartScale, heartSize.center)
          translate((size.center - heartSize.center).x, (size.center - heartSize.center).y)
        }) {
          fill(Shape.path(heartPath), Shading.solidColor(Color.BLUE), BlendingMode.Xor)
        }
  }

  private fun CanvasScope.drawLeftRect(squareSize: Size) {
    group(
        Transform {
          rotate(45f, squareSize.center)
          translate(dx = size.center.x - squareSize.width, dy = size.height * 0.83f)
        }) {
          fill(
              Shape.rect(Point.Zero, squareSize),
              Shading.solidColor(Color.BLUE),
              blendingMode = BlendingMode.Multiply)
        }
  }

  private fun CanvasScope.drawRightRect(squareSize: Size) {
    group(
        Transform {
          rotate(45f, squareSize.center)
          translate(dx = size.center.x - 10f, dy = size.height * 0.83f)
        }) {
          fill(
              Shape.rect(Point.Zero, squareSize),
              Shading.solidColor(Color.GREEN),
              blendingMode = BlendingMode.Multiply)
        }
  }
}
