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

package com.facebook.rendercore.graphics.drawscope

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.MaskFilter
import android.graphics.Paint
import android.graphics.Paint.Cap
import android.graphics.Path
import android.graphics.PathEffect
import android.graphics.RectF
import android.graphics.Shader
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.IntSize
import com.facebook.rendercore.geometry.Point
import com.facebook.rendercore.graphics.Color

class CanvasDrawScope(private val drawContext: DrawContext, private val canvas: Canvas) :
    DrawScope {

  override val center: Point = Point((canvas.width / 2).toFloat(), (canvas.height / 2).toFloat())
  override val size: IntSize = IntSize(canvas.width, canvas.height)

  /**
   * Applies multiple transformations and executes the given drawing block. The canvas state is
   * saved and restored to isolate the transformations.
   *
   * @param transformations Block for configuring transformations to apply.
   * @param block The drawing operations to execute with the transformations applied.
   */
  override fun transform(
      transformations: TransformationScope.() -> Unit,
      block: DrawScope.() -> Unit,
  ) {
    val saveCount = canvas.save()
    try {
      val transformScope = TransformationScope(canvas)
      transformScope.transformations()
      this.block()
    } finally {
      canvas.restoreToCount(saveCount)
    }
  }

  /**
   * Draws an arc scaled to fit inside the specified oval.
   *
   * @param oval The bounds of the oval used to define the shape and size of the arc.
   * @param startAngle Starting angle (in degrees) where the arc begins.
   * @param sweepAngle Sweep angle (in degrees) measured clockwise.
   * @param useCenter If true, include the center of the oval in the arc, and close it if it is
   *   being stroked.
   * @param color The color to use for drawing the arc.
   * @param drawStyle The style to use for drawing the arc (e.g., Fill or Stroke).
   * @param shader Optional shader to apply to the paint.
   * @param pathEffect Optional path effect to apply to the paint.
   * @param maskFilter Optional mask filter to apply to the paint.
   * @param colorFilter Optional color filter to apply to the paint.
   */
  override fun drawArc(
      oval: RectF,
      startAngle: Float,
      sweepAngle: Float,
      useCenter: Boolean,
      color: Color,
      drawStyle: DrawStyle,
      shader: Shader?,
      pathEffect: PathEffect?,
      maskFilter: MaskFilter?,
      colorFilter: ColorFilter?,
  ) {
    canvas.drawArc(
        oval,
        startAngle,
        sweepAngle,
        useCenter,
        drawContext.configurePaint(
            drawStyle,
            color,
            shader,
            pathEffect,
            maskFilter,
            colorFilter,
        ),
    )
  }

  /**
   * Draws an arc scaled to fit inside the specified rectangle.
   *
   * @param left The left side of the rectangle to be drawn.
   * @param top The top side of the rectangle to be drawn.
   * @param right The right side of the rectangle to be drawn.
   * @param bottom The bottom side of the rectangle to be drawn.
   * @param startAngle Starting angle (in degrees) where the arc begins.
   * @param sweepAngle Sweep angle (in degrees) measured clockwise.
   * @param useCenter If true, include the center of the oval in the arc, and close it if it is
   *   being stroked.
   * @param color The color to use for drawing the arc.
   * @param drawStyle The style to use for drawing the arc (e.g., Fill or Stroke).
   * @param shader Optional shader to apply to the paint.
   * @param pathEffect Optional path effect to apply to the paint.
   * @param maskFilter Optional mask filter to apply to the paint.
   * @param colorFilter Optional color filter to apply to the paint.
   */
  override fun drawArc(
      left: Float,
      top: Float,
      right: Float,
      bottom: Float,
      startAngle: Float,
      sweepAngle: Float,
      useCenter: Boolean,
      color: Color,
      drawStyle: DrawStyle,
      shader: Shader?,
      pathEffect: PathEffect?,
      maskFilter: MaskFilter?,
      colorFilter: ColorFilter?,
  ) {
    canvas.drawArc(
        left,
        top,
        right,
        bottom,
        startAngle,
        sweepAngle,
        useCenter,
        drawContext.configurePaint(
            drawStyle,
            color,
            shader,
            pathEffect,
            maskFilter,
            colorFilter,
        ),
    )
  }

  /**
   * Draws the specified bitmap at the given position using the provided paint properties.
   *
   * The bitmap's top-left corner will be placed at the (left, top) coordinates, and it will be
   * rendered unscaled. The paint is configured with the specified color and optional effects.
   *
   * @param bitmap The bitmap to be drawn.
   * @param left The x-coordinate of the left side where the bitmap will be drawn.
   * @param top The y-coordinate of the top side where the bitmap will be drawn.
   * @param color The color to use for drawing the bitmap (applied as a color filter).
   * @param drawStyle The style to use for drawing (e.g., Fill or Stroke).
   * @param shader Optional shader to apply to the paint.
   * @param pathEffect Optional path effect to apply to the paint.
   * @param maskFilter Optional mask filter to apply to the paint.
   * @param colorFilter Optional color filter to apply to the paint.
   */
  override fun drawBitmap(
      bitmap: Bitmap,
      left: Float,
      top: Float,
      color: Color,
      drawStyle: DrawStyle,
      shader: Shader?,
      pathEffect: PathEffect?,
      maskFilter: MaskFilter?,
      colorFilter: ColorFilter?,
  ) {
    canvas.drawBitmap(
        bitmap,
        left,
        top,
        drawContext.configurePaint(
            drawStyle,
            color,
            shader,
            pathEffect,
            maskFilter,
            colorFilter,
        ),
    )
  }

  /**
   * Draws a circle at the specified center coordinate with the given radius and paint properties.
   *
   * @param radius The radius of the circle to draw.
   * @param center The center point of the circle to draw.
   * @param color The color to use for drawing the circle.
   * @param drawStyle The style to use for drawing the circle (e.g., Fill or Stroke).
   * @param alpha The alpha (transparency) value for the circle, ranging from 0.0 (fully
   *   transparent) to 1.0 (fully opaque).
   * @param shader Optional shader to apply to the paint.
   * @param pathEffect Optional path effect to apply to the paint.
   * @param maskFilter Optional mask filter to apply to the paint.
   * @param colorFilter Optional color filter to apply to the paint.
   */
  override fun drawCircle(
      radius: Dimen,
      center: Point,
      color: Color,
      drawStyle: DrawStyle,
      shader: Shader?,
      pathEffect: PathEffect?,
      maskFilter: MaskFilter?,
      colorFilter: ColorFilter?,
  ) {
    canvas.drawCircle(
        center.x,
        center.y,
        radius.toFloatPixels(drawContext.resourceResolver),
        drawContext.configurePaint(
            drawStyle,
            color,
            shader,
            pathEffect,
            maskFilter,
            colorFilter,
        ),
    )
  }

  /**
   * Draws a line between the specified start and end points with the given paint properties.
   *
   * @param start The starting point of the line to draw.
   * @param end The ending point of the line to draw.
   * @param color The color to use for drawing the line.
   * @param strokeWidth The width of the line stroke in px.
   * @param alpha The alpha (transparency) value for the line, ranging from 0.0 (fully transparent)
   *   to 1.0 (fully opaque).
   * @param strokeCap The stroke cap style to use for the ends of the line (e.g., Butt, Round,
   *   Square). Default value is Butt.
   * @param shader Optional shader to apply to the paint.
   * @param pathEffect Optional path effect to apply to the paint.
   * @param maskFilter Optional mask filter to apply to the paint.
   * @param colorFilter Optional color filter to apply to the paint.
   */
  override fun drawLine(
      start: Point,
      end: Point,
      color: Color,
      strokeWidth: Dimen,
      strokeCap: Cap?,
      shader: Shader?,
      pathEffect: PathEffect?,
      maskFilter: MaskFilter?,
      colorFilter: ColorFilter?,
  ) {
    val paint =
        drawContext.configurePaint(
            style = Stroke(strokeWidth),
            color = color,
            shader = shader,
            pathEffect = pathEffect,
            maskFilter = maskFilter,
            strokeCap = strokeCap,
            colorFilter = colorFilter,
        )

    canvas.drawLine(start.x, start.y, end.x, end.y, paint)
  }

  /**
   * Draws the specified oval using the provided paint properties.
   *
   * The oval will be filled or stroked based on the specified [drawStyle]. The [oval] parameter
   * defines the rectangular bounds of the oval to be drawn.
   *
   * @param oval The rectangle bounds of the oval to be drawn.
   * @param color The color to use for drawing the oval.
   * @param drawStyle The style to use for drawing the oval (e.g., Fill or Stroke).
   * @param shader Optional shader to apply to the paint.
   * @param pathEffect Optional path effect to apply to the paint.
   * @param maskFilter Optional mask filter to apply to the paint.
   * @param colorFilter Optional color filter to apply to the paint.
   */
  override fun drawOval(
      oval: RectF,
      color: Color,
      drawStyle: DrawStyle,
      shader: Shader?,
      pathEffect: PathEffect?,
      maskFilter: MaskFilter?,
      colorFilter: ColorFilter?,
  ) {
    canvas.drawOval(
        oval,
        drawContext.configurePaint(
            drawStyle,
            color,
            shader,
            pathEffect,
            maskFilter,
            colorFilter,
        ),
    )
  }

  /**
   * Draws the specified oval using the provided paint properties.
   *
   * The oval will be filled or stroked based on the specified [drawStyle]. The oval is defined by
   * the rectangle with the given [left], [top], [right], and [bottom] coordinates.
   *
   * @param left The left side of the rectangle bounds of the oval to be drawn.
   * @param top The top side of the rectangle bounds of the oval to be drawn.
   * @param right The right side of the rectangle bounds of the oval to be drawn.
   * @param bottom The bottom side of the rectangle bounds of the oval to be drawn.
   * @param color The color to use for drawing the oval.
   * @param drawStyle The style to use for drawing the oval (e.g., Fill or Stroke).
   * @param shader Optional shader to apply to the paint.
   * @param pathEffect Optional path effect to apply to the paint.
   * @param maskFilter Optional mask filter to apply to the paint.
   * @param colorFilter Optional color filter to apply to the paint.
   */
  override fun drawOval(
      left: Float,
      top: Float,
      right: Float,
      bottom: Float,
      color: Color,
      drawStyle: DrawStyle,
      shader: Shader?,
      pathEffect: PathEffect?,
      maskFilter: MaskFilter?,
      colorFilter: ColorFilter?,
  ) {
    canvas.drawOval(
        left,
        top,
        right,
        bottom,
        drawContext.configurePaint(
            drawStyle,
            color,
            shader,
            pathEffect,
            maskFilter,
            colorFilter,
        ),
    )
  }

  /**
   * Draws the specified path using the provided paint properties.
   *
   * The path will be filled or stroked based on the specified [drawStyle].
   *
   * @param path The path to be drawn.
   * @param color The color to use for drawing the path.
   * @param drawStyle The style to use for drawing the path (e.g., Fill or Stroke).
   * @param shader Optional shader to apply to the paint.
   * @param pathEffect Optional path effect to apply to the paint.
   * @param maskFilter Optional mask filter to apply to the paint.
   * @param colorFilter Optional color filter to apply to the paint.
   */
  override fun drawPath(
      path: Path,
      color: Color,
      drawStyle: DrawStyle,
      shader: Shader?,
      pathEffect: PathEffect?,
      maskFilter: MaskFilter?,
      colorFilter: ColorFilter?,
  ) {
    canvas.drawPath(
        path,
        drawContext.configurePaint(
            drawStyle,
            color,
            shader,
            pathEffect,
            maskFilter,
            colorFilter,
        ),
    )
  }

  /**
   * Draws a series of points using the provided paint properties.
   *
   * Each pair of values in [pts] represents the x and y coordinates of a point to be drawn. The
   * points will be rendered using the specified [drawStyle].
   *
   * @param pts Array of coordinates (x0, y0, x1, y1, ...) representing the points to draw.
   * @param color The color to use for drawing the points.
   * @param strokeWidth The width of the point strokes in dp.
   * @param shader Optional shader to apply to the paint.
   * @param pathEffect Optional path effect to apply to the paint.
   * @param maskFilter Optional mask filter to apply to the paint.
   * @param colorFilter Optional color filter to apply to the paint.
   */
  override fun drawPoints(
      pts: FloatArray,
      color: Color,
      strokeWidth: Dimen,
      shader: Shader?,
      pathEffect: PathEffect?,
      maskFilter: MaskFilter?,
      colorFilter: ColorFilter?,
  ) {
    canvas.drawPoints(
        pts,
        drawContext.configurePaint(
            Stroke(strokeWidth),
            color,
            shader,
            pathEffect,
            maskFilter,
            colorFilter,
        ),
    )
  }

  /**
   * Draw the specified Rect using the specified Paint. The rectangle will be filled or framed based
   * on the Style in the paint.
   *
   * @param rect The rectangle to be drawn.
   * @param color The color to use for drawing the rectangle.
   * @param drawStyle The style to use for drawing the rectangle (e.g., Fill or Stroke).
   * @param alpha The alpha (transparency) value for the rectangle, ranging from 0.0 (fully
   *   transparent) to 1.0 (fully opaque).
   * @param shader Optional shader to apply to the paint.
   * @param pathEffect Optional path effect to apply to the paint.
   * @param maskFilter Optional mask filter to apply to the paint.
   * @param colorFilter Optional color filter to apply to the paint.
   */
  override fun drawRect(
      rect: RectF,
      color: Color,
      drawStyle: DrawStyle,
      shader: Shader?,
      pathEffect: PathEffect?,
      maskFilter: MaskFilter?,
      colorFilter: ColorFilter?,
  ) {
    canvas.drawRect(
        rect,
        drawContext.configurePaint(
            drawStyle,
            color,
            shader,
            pathEffect,
            maskFilter,
            colorFilter,
        ),
    )
  }

  /**
   * Draw the specified Rect using the specified Paint. The rectangle will be filled or framed based
   * on the Style in the paint.
   *
   * @param left The left side of the rectangle to be drawn
   * @param top The top side of the rectangle to be drawn
   * @param right The right side of the rectangle to be drawn
   * @param bottom The bottom side of the rectangle to be drawn
   * @param paint The paint used to draw the rect
   * @param color The color to use for drawing the rectangle.
   * @param drawStyle The style to use for drawing the rectangle (e.g., Fill or Stroke).
   * @param alpha The alpha (transparency) value for the rectangle, ranging from 0.0 (fully
   *   transparent) to 1.0 (fully opaque).
   * @param shader Optional shader to apply to the paint.
   * @param pathEffect Optional path effect to apply to the paint.
   * @param maskFilter Optional mask filter to apply to the paint.
   * @param colorFilter Optional color filter to apply to the paint.
   */
  override fun drawRect(
      left: Float,
      top: Float,
      right: Float,
      bottom: Float,
      color: Color,
      drawStyle: DrawStyle,
      shader: Shader?,
      pathEffect: PathEffect?,
      maskFilter: MaskFilter?,
      colorFilter: ColorFilter?,
  ) {
    canvas.drawRect(
        left,
        top,
        right,
        bottom,
        drawContext.configurePaint(drawStyle, color, shader, pathEffect, maskFilter, colorFilter),
    )
  }

  /**
   * Draw the specified round-rect using the specified Paint. The roundrect will be filled or framed
   * based on the Style in the paint.
   *
   * @param rect The rectangular bounds of the roundRect to be drawn.
   * @param color The color to use for drawing the rectangle.
   * @param rx The x-radius of the oval used to round the corners.
   * @param ry The y-radius of the oval used to round the corners.
   * @param drawStyle The style to use for drawing the rectangle (e.g., Fill or Stroke).
   * @param alpha The alpha (transparency) value for the rectangle, ranging from 0.0 (fully
   *   transparent) to 1.0 (fully opaque).
   * @param shader Optional shader to apply to the paint.
   * @param pathEffect Optional path effect to apply to the paint.
   * @param maskFilter Optional mask filter to apply to the paint.
   * @param colorFilter Optional color filter to apply to the paint.
   */
  override fun drawRoundRect(
      rect: RectF,
      color: Color,
      rx: Float,
      ry: Float,
      drawStyle: DrawStyle,
      shader: Shader?,
      pathEffect: PathEffect?,
      maskFilter: MaskFilter?,
      colorFilter: ColorFilter?,
  ) {
    canvas.drawRoundRect(
        rect,
        rx,
        ry,
        drawContext.configurePaint(
            drawStyle,
            color,
            shader,
            pathEffect,
            maskFilter,
            colorFilter,
        ),
    )
  }

  /**
   * Draw the specified round-rect using the specified Paint. The roundrect will be filled or framed
   * based on the Style in the paint.
   *
   * @param left The left side of the rectangle to be drawn.
   * @param top The top side of the rectangle to be drawn.
   * @param right The right side of the rectangle to be drawn.
   * @param bottom The bottom side of the rectangle to be drawn.
   * @param color The color to use for drawing the rectangle.
   * @param rx The x-radius of the oval used to round the corners.
   * @param ry The y-radius of the oval used to round the corners.
   * @param drawStyle The style to use for drawing the rectangle (e.g., Fill or Stroke).
   * @param alpha The alpha (transparency) value for the rectangle, ranging from 0.0 (fully
   *   transparent) to 1.0 (fully opaque).
   * @param shader Optional shader to apply to the paint.
   * @param pathEffect Optional path effect to apply to the paint.
   * @param maskFilter Optional mask filter to apply to the paint.
   * @param colorFilter Optional color filter to apply to the paint.
   */
  override fun drawRoundRect(
      left: Float,
      top: Float,
      right: Float,
      bottom: Float,
      color: Color,
      rx: Float,
      ry: Float,
      drawStyle: DrawStyle,
      shader: Shader?,
      pathEffect: PathEffect?,
      maskFilter: MaskFilter?,
      colorFilter: ColorFilter?,
  ) {
    canvas.drawRoundRect(
        left,
        top,
        right,
        bottom,
        rx,
        ry,
        drawContext.configurePaint(
            drawStyle,
            color,
            shader,
            pathEffect,
            maskFilter,
            colorFilter,
        ),
    )
  }

  override fun drawText(
      text: String,
      x: Float,
      y: Float,
      color: Color,
      drawStyle: DrawStyle,
      shader: Shader?,
      pathEffect: PathEffect?,
      maskFilter: MaskFilter?,
      colorFilter: ColorFilter?,
      textAlign: Paint.Align,
      textScaleX: Float,
      textSize: Float?,
      textSkewX: Float,
  ) {
    canvas.drawText(
        text,
        x,
        y,
        drawContext.configureTextPaint(
            drawStyle,
            color,
            shader,
            pathEffect,
            maskFilter,
            colorFilter,
            textAlign,
            textScaleX,
            textSize,
            textSkewX,
        ),
    )
  }
}
